package com.ca.directory.jxplorer.broker;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;


import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.*;

import com.ca.directory.jxplorer.*;
import com.ca.commons.naming.*;
import com.ca.commons.cbutil.*;
import com.ca.commons.jndi.*;




/**
*	This utility class handles all the JNDIDataBroker LDAP calls, returning objects
*  	to calling classes and managing the connection.<p>
*
*  	Before examining this class make sure to examing the base DataBroker class thoroughly.
*  	The base DataBroker class takes user requests, and creates DataQuery objects.  A
*  	separate thread takes these DataQuery objects and uses the methods of derived
*  	classes (such as this one) to do the actual grunt work.
*/

public class JNDIDataBroker extends DataBroker
{
    private static final int    SEARCHLIMIT = 0;
    private static final int    SEARCHTIMEOUT = 0;

    /**
     *   Used as a parameter to unthreadedSearch, this specifies to only search
     *   the base object.
     */

    public static final int SEARCH_BASE_OBJECT = 0;

    /**
     *   Used as a parameter to unthreadedSearch, this specifies to only search
     *   the next level from the current DN.
     */

    public static final int SEARCH_ONE_LEVEL = 1;

    /**
     *   Used as a parameter to unthreadedSearch, this specifies to search
     *   the entire subtree from the current DN.
     */

    public static final int SEARCH_SUB_TREE = 2;

    /**
     *    Magic value for search filters that returns only the DN of an entry
     */

    public static final String[] RETURN_ONLY_DN = new String[] {"1.1"};

    /**
     *    Magic value for search filters that returns all entries
     */
    public static final String RETURN_ALL_ENTRIES = "(objectClass=*)";

  	private LdapContext ctx;

    private boolean tracing = false;
    private boolean connectionError = true;


    private Hashtable attributeNames;

    private boolean quietMode = false;     			// suppress gui ops (esp. error msgs.)
    private boolean errorWhileQuietFlag = false; 	// used when broker is in 'quiet gui mode'.

  	int limit   = SEARCHLIMIT;             			// default number of results returned.
    int timeout = SEARCHTIMEOUT;           			// default timeout.
    boolean pagedResults = false;                   // whether to us LDAP paged results handling for large data sets

    static int threadID = 1;               			// debug identifier for thread tracking
    static final boolean DEBUGTHREADS = false; 		// debug flag for threadiness

    private CBGraphicsOps dirOps = null;   			// the low level directory operations class.
    private SchemaOps schemaOps;                    // the low level schemaOps class

    private HashSet specialObjectClasses;  			// OS390 hack

    private final static Logger log = Logger.getLogger(JNDIDataBroker.class.getName());

    protected static boolean lockReadOnly = false;  // whether the entire browser should be 'locked' into read only mode.
    protected boolean readOnly = false;             // whether to allow write access to the directory - note, this is for convenience, not security!  It prevents accidental modification only.


   /**
	*  	Helper class for DataBroker, this encapsulates an ldap-like connection
	*  	request that is placed on the DataBroker queue for eventual resolution.
	*  	The Connection Request object is intended to be used once, and then
	*  	discarded.
	*/

    public class DataConnectionQuery extends DataQuery
    {
		public final ConnectionData conData;

	   /**
		*   Defines a request to open a connection to an LDAP (only) server.
		*	@param cData a data object containing connection information.
		*/

		public DataConnectionQuery(ConnectionData cData)
		{
            super(DataQuery.EXTENDED);

			conData = cData;

            setExtendedData("version", String.valueOf(conData.version));
            setExtendedData("url", conData.getURL());

            readOnly = lockReadOnly?true:conData.readOnly;
		}

	   /**
		*    Utility name translation method
		*/

        public String getTypeString()
        {
            return super.getTypeString() + " Connection Request";
        }
    }

    /**
     * Forces ALL jndi brokers to be read only, regardless of what is set in the connection data.
     */
    public static void lockToReadOnlyMode()
    {
        lockReadOnly = true;
    }

    /**
     * Returns whether this data broker only allows 'read' operations, and has disabled all directory 'write' applications.
     * @return
     */
    public boolean isReadOnly()
    {
        return readOnly;
    }



   /**
    *	Constructor does nothing except create an env object ( 'connect()'
    *   is used to open a connection)
    */

    public JNDIDataBroker()
    {
        initSpecialObjectClasses();
    }



   /**
	*  	Clones a JNDIDataBroker, using the same underlying directory connection,
	*  	but clearing the data listener list, and having its own debug flags.
	*  	Any StopMonitors however will need to be re-registered with the new
	*  	DataBroker.
	*	@param cloneMe
	*/

    public JNDIDataBroker(JNDIDataBroker cloneMe)
    {
        registerDirectoryConnection(cloneMe);
    }



   /**
	* 	Resets a JNDIDataBroker, using the same underlying directory connection
	*  	as the passed broker, but clearing the data listener list, and resetting debug flags.
	*  	Any StopMonitors however will need to be re-registered.
	*	@param cloneMe
	*/

    public void registerDirectoryConnection(JNDIDataBroker cloneMe)
    {
        ctx = cloneMe.ctx;
//      schemaOps = cloneMe.schemaOps;
        tracing = cloneMe.tracing;
        connectionError = cloneMe.connectionError;
        attributeNames = cloneMe.attributeNames;

        limit   = cloneMe.limit;
        timeout = cloneMe.timeout;
        pagedResults = cloneMe.pagedResults;

        dirOps = cloneMe.dirOps;
        schemaOps = cloneMe.schemaOps;

        readOnly = cloneMe.readOnly;

        specialObjectClasses = cloneMe.specialObjectClasses;  // OS390 hack
    }



   /**
	*  	Mitch/OS390 hack
	*/

    protected void initSpecialObjectClasses()
    {
        String fileName = System.getProperty("user.dir") + File.separator + "specialocs.txt";
        if (new File(fileName).exists())
        {
            try
            {
                String text = CBUtility.readTextFile(new File(fileName));
                StringTokenizer st = new StringTokenizer(text);
                specialObjectClasses = new HashSet(10);
                while (st.hasMoreTokens())
                {
                    String oc = st.nextToken();
                    specialObjectClasses.add(oc);
                }
            }
            catch (Exception e)
            {
                log.info("unable to obtain special object classes list:\n  " + e.toString());
                specialObjectClasses = null;
            }
        }
    }



   /**
	*  	Suppresses user notification of errors via GUI dialogs,
	*  	and logs them instead.  Necessary for large batch ops. like
	*  	importing an ldif file.
	*	@param status
	*/

    public void setGUIQuiet(boolean status)
    {
        quietMode = status;
        dirOps.setQuietMode(status);

        if (quietMode == false)
            setQuietError(false);  // clear quiet error flag.
    }



   /**
	*  	Sets the quiet error flag status.
	*	@param status
	*/

    public void setQuietError(boolean status)
    {
        errorWhileQuietFlag = status;
    }



   /**
	*  	This returns whether one or more errors occured while the
	*  	program was in 'quiet gui' (i.e. no error dialogs) mode.
	*  	It does not return the actual error, since frequently there
	*  	were many: the user should consult the log file.
    * 
	*	@return whether one or more errors occured while the
	*  	program was in 'quiet gui' (i.e. no error dialogs) mode
	*/

    public boolean getQuietError()
    {
        return (errorWhileQuietFlag || dirOps.errorWhileQuietFlag);
    }


   /**
	*  	Sets ber tracing status.  Set to true this generates a huge
	*  	amount of comms. tracing info, <i>when the next connection is opened</i>.
	*  	It doesn't seem possible to set it for an already open connection, so
	*  	we no longer even try.
	*	@param traceStatus
	*/

    public void setTracing(boolean traceStatus)
    {
        tracing = traceStatus;
    }

    /**
     *  	Returns ber tracing status.  When true this generates a huge
     *  	amount of comms. tracing info, <i>when the next connection is opened</i>.
     *  	It doesn't seem possible to set it for an already open connection, so
     *  	we no longer even try.
     *	    @return the traceStatus
     */

    public boolean getTracing() { return tracing; }

   /**
	*   <p>Queues a request to open a connection to an LDAP (only) server.</p>
	*
	*   <p>Note that some rarely modified connection status variables are set externally - e.g.
	*   BER tracing status (derived from the log level, set by setTracing() ),
	*   and the security keystore type and external security provider (if any)
	*   which are set in the config file).</p>
	*
	*   @param baseDN          		the base DN from which to browse.
	*   @param version         		the LDAP Version (2 or 3) being used.
	*   @param host            		the LDAP server url.
	*   @param port            		the LDAP server port (default 389) being used.
	*   @param userDN		   		the Manager User's DN - (is null if user is not manager)
	*   @param pwd             		the Manager User's password - (is null if user is not manager)
	*   @param referralType    		the jndi ldap referral type: [follow:ignore:throw]
	*   @param aliasType			how aliases are handled: 'always'|'never'|'finding'|'searching'
	*   @param useSSL          		whether to use SSL for encryption and/or authentication (dependant on other parameters)
	*   @param cacerts		      	path to a store of trusted server certificates or CA certificates - required for Server-auth ssl
	*   @param clientcerts		  	path to client certificates - if available, will use for client authentication
	*	@param caKeystorePwd		the password to the client's keystore (may be null for non-client authenticated ssl).
	*   @param clientKeystorePwd	the password to the client certificates - required to use client certs for authentication
	*   @deprecated            		use connect(ConnectionData) instead.
	*   @return                		returns the thread that is used to make the connection
	*/

    // nb capitalisation of 'cacerts' and 'clientcerts' wierd to match actual default file names.

  	public DataQuery connect(String baseDN, int version, String host,
          int port, String userDN, char[] pwd,
          String referralType, String aliasType, boolean useSSL,
          String cacerts, String clientcerts,
          char[] caKeystorePwd, char[] clientKeystorePwd)
  	{
		ConnectionData cData = new ConnectionData();
		cData.setURL(host,port);

		cData.baseDN 				= baseDN;
		cData.version				= version;
		cData.setURL(host, port);
		cData.userDN				= userDN;
		cData.pwd					= pwd;
		cData.referralType 			= referralType;
		cData.aliasType				= aliasType;
		cData.useSSL				= useSSL;
		cData.cacerts				= cacerts;
		cData.clientcerts 			= clientcerts;
		cData.caKeystorePwd			= caKeystorePwd;
		cData.clientKeystorePwd		= clientKeystorePwd;
        cData.tracing               = getTracing();
        return connect(cData);
    }



   /**
	* 	<p>Queues a request to open a connection to an LDAP (only) server.</p>
	*
	*   <p>Note that some rarely modified connection status variables are set externally - e.g.
	*   BER tracing status (derived from the log level, set by setTracing() ),
	*   and the security keystore type and external security provider (if any)
	*   which are set in the config file).</p>
	* 	@param cData data object containing all the connection information.
	*   @return returns the thread that is used to make the connection
	*/

  	public DataQuery connect(ConnectionData cData)
  	{
        cData.caKeystoreType     = JXConfig.getProperty("keystoreType.cacerts", "JKS");
        cData.clientKeystoreType = JXConfig.getProperty("keystoreType.clientcerts", "JKS");

        DataQuery openCon = new DataConnectionQuery(cData);

        return push(openCon);
    }



   /**
	*  	Extends the base class processRequest method to handle DataConnectionRequest objects.
	*	@param request the connection data query.
	*/

    protected void processRequest(DataQuery request)
    {
        try
        {
            if (request instanceof DataConnectionQuery)
                openConnection((DataConnectionQuery) request);
            else
                super.processRequest(request);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            request.setException(e);
        }
    }



   /**
	*   Does the actual grunt work of opening a new connection.
	*	@param request a DataQuery object that contains the connection details.
	*	@return the data query object.
	*/

    protected DataQuery openConnection(DataConnectionQuery request)
    {
        disconnect(); // clear out any existing cobwebs...

		ConnectionData cData = request.conData;
        try
        {
            openConnection(cData);
        }
        // can throw NamingException, GeneralSecurityException, IOException
        catch (Exception ne)        // An error has occurred.  Exit connection routine
        {                                 // taking no further action.
            log.warning("initial receipt of exception by jndi broker " + ne.getMessage());
            ne.printStackTrace();
            request.setException(ne);
            request.setStatus(false);
            request.finish();
            return request;
        }


        request.setStatus(true); 		// success!
        request.finish();
        return request;
 	}

    /**
     * This partially initialises the JNDI Broker by directly applying a context.
     * (to be used for internal test cases only).
     * @param testCtx
     * @throws Exception
     */
    public void openTestConnection(LdapContext testCtx)
            throws Exception
    {
        ctx = testCtx;
        dirOps = new CBGraphicsOps(ctx);
//        schemaOps = new SchemaOps(ctx);

    }

    /**
     * This initialises the JNDIDataBroker with an active LDAP connection.
     * Within 'normal JXplorer' this is usually called as the result of the evaluation of a
     * DataConnectionQuery being picked up from the query queue and being executed by
     * 'openConnection(DataConnectionQuery)', but it can also be executed directly by
     * unthreaded code that wants to create a JNDIDataBroker directly (note however that this
     * method may block for a minute or more if the LDAP server is unresponsive.)
     * @param cData a connection data object.
     * @throws Exception
     */
    public void openConnection(ConnectionData cData)
              throws Exception
    {
        String url = cData.url;

        connectionError = false;

        ctx = null;    // null the current directory context (can't be used again).

        //  Try to get a directory context using above info.

        dirOps = new CBGraphicsOps(cData);        	// this wraps up ctx for basic operations
        ctx = dirOps.getContext();

        if (ctx == null)
            throw new NamingException("unable to open connection: unknown condition, no error returned.");

        // make a bogus, fast, directory request to trigger some activity on the context.  Without this the
        // context may *appear* to be open since jndi sometimes won't actually try to use it until a request is made
        // (e.g. with DSML, SSL connections etc.)

        String base = (cData.baseDN==null)?"":cData.baseDN;

        //XXX bogus request failing - why??? (ans SASL error in jdk 1.4.0 - 1.4.1)
        //ctx.search(base, "objectClass=*", new SearchControls(SearchControls.OBJECT_SCOPE, 0, 0, new String[]{}, false, false));
        if (dirOps.exists(base) == false)
            cData.baseDN = getActualDN(cData.baseDN);	//TE: (for bug 2363) - try to solve case sensitive DN problem.

        // At this stage we should have a valid ldap context

        try
        {
            schemaOps = new SchemaOps(ctx);
        }        		// this wraps up ctx for basic operations
        catch (NamingException e)
        {
            log.log(Level.WARNING, "unable to init schemaOps Ops ", e);
            schemaOps = null;
        }

        if (schemaOps != null && (cData.protocol == ConnectionData.DSML || cData.version > 2))    					// if ldap 3 or better try to open a schemaOps context
        {
            try
            {
                String binaries = schemaOps.getNewBinaryAttributes();
                if (binaries.trim() != "")
                    ctx.addToEnvironment("java.naming.ldap.attributes.binary", binaries);
                initAttributeNamesHash();
            }
            catch (NamingException e)    // annoying, but not fatal, error.
            {
                log.log(Level.WARNING, "Failed to connect to schemaOps: " + url, e);
            }
        }
        else
        {
            log.info("skipped schemaOps stuff : " + cData.protocol);
        }

        if (cData.protocol.equalsIgnoreCase("dsml"))    //TODO: some day we may support a different version of DSML. If so the version variable in ConnectionData should be called ldapVersion and a new variable called dsmlVersion added.
            log.info("Successfully connected to " + url + " using " + cData.protocol + " version 2");
        else
            log.info("Successfully connected to " + url + " using " + cData.protocol + " version " + cData.version);
}
	

   /**
    *   Warning: ETrust Directory Specific Code!  Currently unused.
    *
	*	This is a bit of a hack to verify the base DN.  It basically takes a DN and does a base object
	*	search and returns the DN of the results.  This method is needed as a bit of a hack for when the
	*	the user puts in a base DN that is of a different case to what is stored in the dsa.  If this happens
	*	the tree remembers the wrong DN and throws some errors when the user tries to modify the base DN
	*	entry (bug 2363).  However, currently (20 June 02) the dsa gives the DN back the way it was put in.  I've
	*	assigned the bug to them to fix...  
	*	@param dn the DN to verify (or do the base object search on).
	*	@return the DN that the search returns (one day this will be what is actually stored in the dsa).  If
	*		there is an exception thrown, the DN will be returned unchanged.
	*	.
	*/
	//TE: NOTE: WAITING IN DSA CHANGE BEFORE THIS CAN BE USED...BUG 2363
	public String getActualDN(String dn)
	{
		if(true)
			return dn;
			
		try
		{
            NamingEnumeration en = dirOps.searchBaseEntry(new DN(dn), "objectClass=*",0,0, null);
			String temp = "";
			while (en.hasMoreElements())
			{
				temp = (en.nextElement()).toString();
				temp = temp.substring(0, temp.indexOf(":"));				
			}
			return temp;
		}
		catch(Exception e)
		{
			return dn;
		}		
	}



   /**
	*  	The attributeNames hash provides a quick look up mapping
	*  	between numericoids and attribute names.  This initialises
	*  	that hashtable.
	*/

    // XXX probably neater ways of doing this now jndi 1.2 is out...
    // TODO: ... and why is it being done here, and not in schema ops?
    protected void initAttributeNamesHash()
    {
        attributeNames = new Hashtable(100);

        /*
         *   XXX This tries to parse efficiently the syntax line ... not very happy with it though, it seems a bit ad-hoc.
         *   we may want to rewrite this more generically some time...
         */
        try
        {
            Attribute attDefs = schemaOps.getAttributeTypes();
            if (attDefs==null)
            {
                log.warning("unable to read schema attributes in JNDIDataBroker:initAttributeNamesHash");
                return;
            }
            StringTokenizer tokenizer;
            for (int i=0; i<attDefs.size(); i++)
            {
                String parseMe = attDefs.get(i).toString();  // response something like: ( 2.5.4.4 NAME ( 'sn' 'surname' ) SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
                int namePos = parseMe.indexOf("NAME");
                if (namePos == -1)
                    throw new NamingException("unable to parse ldap syntax '" + parseMe + "'");
                String oid = parseMe.substring(1, namePos).trim();
                String names = "";
                if (parseMe.indexOf("SYNTAX")>-1)
                    names = parseMe.substring(parseMe.indexOf("'", namePos), parseMe.indexOf("SYNTAX")-2); // trim off final bracket, if it exists...

                tokenizer = new StringTokenizer(names, "'");
                while (tokenizer.hasMoreElements())
                {
                    String name = tokenizer.nextToken().trim();
                    if (name.length() > 0)
                    {
                        attributeNames.put(oid, name);
                    }
                }

            }
        }
        catch (NamingException e)
        {
            log.warning("unable to parse schemaOps at JndiBroker:initAttributeNamesHash " + e + "\n");
        }
        catch (Exception e2)
        {
            log.warning("Unexpected exception parsing schemaOps at JndiBroker:initAttributeNamesHash " + e2 + "\n");
            e2.printStackTrace();
        }

    }



   /**
	*	@param attributeoid the attributes OID.
	*	@return the attribute description.  If attributeNames is null "(schemaOps not correctly read)"
	*		is returned, if attributeNames are still null after trying to get the description
	*		"(attribute not listed in schemaOps)" is returned.
	*/

    public String getAttributeDescription(String attributeoid)
    {
        if (attributeNames == null) return "(schemaOps not correctly read)";
        String attributeName = (String)attributeNames.get(attributeoid);
        if (attributeName == null) attributeName = "(attribute not listed in schemaOps)";
        return attributeName;
    }


   /**
	*  	This returns the version of ldap currently in use.
	*  	@return current ldap version ('2' or '3')
	*/

    public int getVersion()
    {
        return dirOps.getLdapVersion();
    }



   /**
	*  	Used for debugging; prints out the first level of a dn from
	*  	a particular context, headlined with
	*  	with the supplied message.
	*  	@param C the context to print out info for.
	*  	@param dn the DN to print the children of.
	*  	@param message text to print out along with the context data.
	*/

    public void printContextList(Context C, DN dn, String message)
    {
        System.out.println("******* " + message + " ******\nPrinting context '" + dn + "'");
        try
        {
            NamingEnumeration debug = C.list(dn);
            while (debug.hasMore())
            {
                System.out.println(((NameClassPair)(debug.next())).getName());
            }
        }
        catch (NamingException e)
        {
            System.out.println("error printing context " + dn + "( " + message + " )" +e);
        }
    }



   /**
	* 	Disconnects from the current context, freeing both context and
	*  	context environment parameters list.
	*/

 	public void disconnect()
 	{
        attributeNames = null;
        ctx = null;
        schemaOps = null;

        if (dirOps == null)
            return;   // no context open (not even a BasicOps object in existance!).
         try
         {
             dirOps.close();  // closes ctx...
         }
         catch (NamingException e)
         {
             e.printStackTrace();  //To change body of catch statement use Options | File Templates.
         }
         dirOps = null;

        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
 	}



   /**
	*   Sets the maximum number of objects returned by a search
	*   @param maxResponses the maximum returned objects
	*/

    public void setLimit(int maxResponses) { limit = maxResponses; }



   /**
	*   Sets the timeout period before the connection gives up trying
	*   to fetch a given data request.
	*   @param maxTime the maximum time allowed for a query.
	*/

    public void setTimeout(int maxTime) { timeout = maxTime; }

    public void setPaging(boolean usePaging, int pageSize)
    {
        pagedResults = usePaging;
        if (usePaging)
            JNDIOps.setPageSize(pageSize);
        else
            JNDIOps.setPageSize(JNDIOps.NO_PAGING); // if we're not using paging, ignore page size
    }


   /**
	*   returns the next level of a directory tree, returning
	*   a Enumeration of the results
	*   @param searchbase the node in the tree to expand
	*   @return list of results (NameClassPair); the next layer of the tree...
	*/

    public DXNamingEnumeration unthreadedList(DN searchbase)
    {
        SetContextToBrowsingAliases();

        try
        {
            return new DXNamingEnumeration(dirOps.list(searchbase));
        }
        catch (NamingException e)
        {
            error("unable to list " + searchbase, e);
            return new DXNamingEnumeration();  // return empty list.
        }
    }



   /**
	*   Performs a directory search.
	*   @param dn the DN (relative to initial context in ldap) to seach from.
	*   @param filter the non-null filter to use for the search
	*   @param search_level whether to search the base object, the next level or the whole subtree.
	*   @param returnAttributes - a list of attributes to return.  If set to null,
	*          only the objectClass is returned.
	*   @return list of results ('SearchResult's); the next layer of the tree...
	*/

    public DXNamingEnumeration unthreadedSearch(DN dn, String filter, int search_level, String[] returnAttributes)
    {
        SetContextToSearchingAliases();

        DXNamingEnumeration ret;

        // CB - use constants for search levels.

        try
        {
            if (search_level == SEARCH_BASE_OBJECT)
                ret = new DXNamingEnumeration(dirOps.searchBaseEntry(dn, filter, limit, timeout, returnAttributes));
            else if (search_level == SEARCH_ONE_LEVEL)
                ret = new DXNamingEnumeration(dirOps.searchOneLevel(dn, filter, limit, timeout, returnAttributes));
            else if (search_level == SEARCH_SUB_TREE)
                ret = new DXNamingEnumeration(dirOps.searchSubTree(dn, filter, limit, timeout, returnAttributes));
            else
                return null;
        }
        catch (NamingException e)
        {
            error("unable to search " + dn, e);
            return new DXNamingEnumeration();
        }

        SetContextToBrowsingAliases();

        return ret;
    }



   /**
	* 	Reads all the attribute type and values for the given entry.
	* 	Converts utf-8 to unicode if necessary.
	* 	@param dn the ldap string distinguished name of entry to be read
	* 	@return an 'Attributes' object containing a list of all Attribute
	*   	objects.
	*/

    public synchronized Attributes read(DN dn)
    {
        Attributes atts = null;
        try
        {
            atts = dirOps.read(dn);
        }
        catch (NamingException e)
        {
            error("unable to read " + dn, e);

        }
        return new DXAttributes(atts);
    }



   /**
	*	Deletes a subtree by recursively deleting sub-sub trees from the given DN.
	*	@param nodeDN the DN of the node where to do the recursive delete.
	*/

    public void deleteTree(DN nodeDN)
        throws NamingException
    {
        if (readOnly)
            throw new NamingException(CBIntText.get("JXplorer is in read only mode; no directory modifications allowed"));

         dirOps.deleteTree(nodeDN);
    }



   /**
    *  	Moves a DN to a new DN, including all subordinate entries.
    *  	(nb it is up to the implementer how this is done; e.g. if it is an
    *  	ldap broker, it may choose rename, or copy-and-delete, as appropriate)
    * 	@param oldNodeDN the original DN of the sub tree root (may be a single
    *  		entry).
    *  	@param newNodeDN the target DN for the tree to be moved to.
    */

    public void moveTree(DN oldNodeDN, DN newNodeDN)       // may be a single node.
        throws NamingException
    {
        if (readOnly)
            throw new NamingException(CBIntText.get("JXplorer is in read only mode; no directory modifications allowed"));

        dirOps.moveTree(oldNodeDN, newNodeDN);
    }



   /**
    *  	Copies a DN representing a subtree to a new subtree, including
    *  	copying all subordinate entries.
    *  	@param oldNodeDN the original DN of the sub tree root
    *  		to be copied (may be a single entry).
    *  	@param newNodeDN the target DN for the tree to be moved to.
    */

    public void unthreadedCopy(DN oldNodeDN, DN newNodeDN)       // may be a single node.
        throws NamingException
    {
        if (readOnly)
            throw new NamingException(CBIntText.get("JXplorer is in read only mode; no directory modifications allowed"));
        dirOps.copyTree(oldNodeDN, newNodeDN, true);  // TODO: check if we need more sophisticated handling of DN renaming...
    }



   /**
	*  	Checks the existance of a given entry.
	*	@param checkMe the DN of the entry to check for existance.
	*  	@return whether the entry could be found in the directory.
	*/

    public boolean unthreadedExists(DN checkMe)
           throws NamingException
    {
        return dirOps.exists(checkMe);
    }



   /**
    *   Process the queue.
    * 
    *	@return whether the queue was successfully processed
	*/

    public boolean processQueue()
    {
        if (dirOps != null) dirOps.setQuietMode(true);
        boolean ret = super.processQueue();


        // WARNING - THREAD PROBLEMS POSSIBLE
        // If this thread is cancelled, we run the risk of interfering
        // with dirOps quiet mode status.  However, since only jndi brokers
        // should be accessing it, it's probably o.k. to leave it in
        // quiet mode = true.  Setting quiet mode = false while something
        // else was using it could be very bad howerver, hence the
        // immediate bail out.
        if (ret == false) return false;

        if (dirOps != null) dirOps.setQuietMode(false);
        return true;
    }


   /**
	*    Utility method for extended queries - returns whether
	*    a 'masked' exception has occured.
	*/

    public Exception getException()
    {
        return dirOps.quietException;
    }



   /**
	*  	Utility method for extended queries - allows
	*  	a 'masked' exception to be cleared.
	*/

    public void clearException()
    {
        dirOps.quietException = null;
    }



   /**
    *	@param request
	*	@return the query.
	*/

    protected DataQuery finish(DataQuery request)
    {
        request.setException(dirOps.quietException);  // probably null
        request.finish();
        return request;
    }


   /**
	*    whether the data source is currently on-line.
	*    @return on-line status
	*/

    public boolean isActive() { return (ctx != null); }



   /**
	*    whether the data source is currently on-line.
	*    @return on-line status
	*/

    public boolean hasConnectionError() { return ((ctx==null)||(connectionError)); }



   /**
	*  	Reads an entry with all its attributes from the directory.
    *  	@param entryDN the DN of the entry.
	*  	@param returnAttributes a list of string names of attributes to return in the search.
	*          (null means 'return all entries', a zero length array means 'return no attributes'.)
	*	@return the entry.
	*/

    public DXEntry unthreadedReadEntry(DN entryDN, String[] returnAttributes)
           throws NamingException
    {
        DXAttributes atts = new DXAttributes(dirOps.read(entryDN, returnAttributes));
        return new DXEntry(atts, entryDN);
    }



   /**
    *  	Add the new entry to the directory.
    *  	@param newEntry the new entry containing the replacement set of attributes.
    */

    public void addEntry(DXEntry newEntry)
           throws NamingException
    {
        // sanity check
         if (newEntry.getDN() == null)
            throw new NamingException("Internal Error: Entry with null DN passed to JNDIDataBroker addEntry().  Modify Request Cancelled.");

        dirOps.addEntry(newEntry.getDN(), newEntry);
    }



   /**
	*  	Utility ftn for updateNode - takes a list of attributes to modify, and
	*  	the type of modification, and adds them to an array of modifications (starting
	*  	at a particular index).
	*
	*  	@param mods the array of modification items
	*  	@param atts an enumeration of attributes to add to the mod array
	*  	@param TYPE the type of modification (DELETE,REPLACE,ADD)
	*  	@param index the position in the modification array to start filling entries in
	*  	@return the final index position reached.
	*/

    protected int loadMods(ModificationItem[] mods, NamingEnumeration atts, int TYPE, int index)
        throws NamingException
    {
        while (atts.hasMore())
        {
          Attribute temp =   (Attribute)atts.next();
          mods[index++] = new ModificationItem(TYPE,temp);
        }
        return index;
    }



   /**
    *  	Update an entry with the designated DN.
    * 	@param oldEntry the old entry containing the old set of attributes.
    *  	@param newEntry the new entry containing the replacement set of attributes.
    */

    public void unthreadedModify(DXEntry oldEntry, DXEntry newEntry)
        throws NamingException
    {
        if (readOnly)
            throw new NamingException(CBIntText.get("JXplorer is in read only mode; no directory modifications allowed"));

        if (useSpecialWriteAllAttsMode()) // do magic for Mitch
            doSpecialWriteAllAttsHandling(oldEntry, newEntry);
        else
            dirOps.modifyEntry(oldEntry, newEntry);
    }

    private void doSpecialWriteAllAttsHandling(DXEntry oldEntry, DXEntry newEntry)
        throws NamingException
    {
        // check for cases where handling is the same as normal
        if ( (newEntry == null) || (oldEntry == null && newEntry.getStatus() == DXEntry.NEW) )
        {
            dirOps.modifyEntry(oldEntry, newEntry);
        }

        // do any rename required
        if (oldEntry.getDN().toString().equals(newEntry.getDN().toString()) == false)
        {
            moveTree(oldEntry.getDN(), newEntry.getDN());
        }

        if (DXAttributes.attributesEqual(oldEntry,newEntry))
        {
            return; // nothing to do.
        }

        DN nodeDN = newEntry.getDN();
        RDN newRDN = nodeDN.getLowestRDN();

        DXAttributes adds = null;    // use modify-add for new attribute values (inc entirely new attribute)
        DXAttributes reps = null;    // use modify-replace for changed attribute values
        DXAttributes dels = null;    // use modify-delete for deleted attribute values (inc deleting entire attribute)

        try
        {
            if (hasVerboseObjectClass(newEntry))  // ?? XXX is this right? Shouldn't oldSet be null or something?
                reps = Special.getReplacementSet(newRDN, oldEntry, newEntry);
            else
                reps = new DXAttributes();

            dels = Special.getDeletionSet(newRDN, oldEntry, newEntry);
            adds = Special.getAdditionSet(newRDN, oldEntry, newEntry);

            log.fine("updateNode: " + nodeDN);

            ModificationItem[] mods;

            mods = new ModificationItem[dels.size() + reps.size() + adds.size()];

            int modIndex = 0;
            modIndex = loadMods(mods, dels.getAll(), LdapContext.REMOVE_ATTRIBUTE, modIndex);
            modIndex = loadMods(mods, adds.getAll(), LdapContext.ADD_ATTRIBUTE, modIndex);
            modIndex = loadMods(mods, reps.getAll(), LdapContext.REPLACE_ATTRIBUTE, modIndex);

            dirOps.modifyAttributes(nodeDN, mods);
        }
        catch (Exception e)
        {
            NamingException os390 = new NamingException("SPECIAL OS390 MODE ERROR: Unexpected Error updating node " + oldEntry.getDN() + "! " + e.toString());
            os390.initCause(e);
            throw os390;
        }
    }




   /*
	*   Optional debug code.  Very useful. NEVER REMOVE!!!
	* 	@param oldSet old entry.
	* 	@param newSet new entry.
	*	@param adds list of attributes to add.
	*	@param reps list of attributes to replace.
	*	@param dels list of attributes to delete.
	*/
/*
    private void printDebug(DXEntry oldSet, DXEntry newSet, DXAttributes adds, DXAttributes reps, DXAttributes dels)
    {


        System.out.println("\n*** entries are ***\n\nold:\n" + oldSet.toString() + "\n\nnew:\n" + newSet.toString());
        System.out.println("\n-----------------\nreps:\n" + reps.toString());
        System.out.println("\n-----------------\ndels:\n" + dels.toString());
        System.out.println("\n-----------------\nadds:\n" + adds.toString());
        //Thread.currentThread().dumpStack();
    }
 */

    protected boolean useSpecialWriteAllAttsMode()
    {
        // XXX Warning - the 'special code for Mitch' works very differently
        // XXX from the normal DXAttributes.get... code in the normal operation
        // XXX section - be careful about changing it.

        return (specialObjectClasses != null);  // test for special hack for Mitch
    }

   /**
	*  	Checks if any of the objectClasses of this object are on the list
	*  	of special objects that require all attributes to be sent in a
	*  	'replace' list, whether or not they have been modified by the user.
	*  	(by request of Mitch Rozonkiewiecz and the OS390 security people...)
	*	@param atts the list of attributes.
	*	@return true if on the list, false other wise.
	*/

    protected boolean hasVerboseObjectClass(Attributes atts)
    {
        if (specialObjectClasses == null) return false;  // there are none.

        try
        {
            Attribute oc;
            if (atts instanceof DXAttributes == false)
                oc = atts.get("objectClass");  // even odds: this may screw up due to capitalization.
            else                               // (but this *should* always be a DXAttributes object...)
                oc = ((DXAttributes)atts).getAllObjectClasses();

            NamingEnumeration ocs = oc.getAll();
            while (ocs.hasMore())
            {
                Object test = ocs.next();
                if (specialObjectClasses.contains(test))
                {
                    return true;
                }
            }
            return false;
        }
        catch (NamingException e)
        {
            log.warning("error getting object classes in jndibroker " + e);
            return false;
        }
    }



   /**
    *    Checks whether the current data source is modifiable.  (Nb.,
    *    a directory may have different access controls defined on
    *    different parts of the directory: if this is the case, the
    *    directory may return true to isModifiable(), however a
    *    particular modify attempt may still fail.
    *
    *    @return whether the directory is modifiable
    */

    public boolean isModifiable()
    {
        return (!readOnly);
    }



    /***  DataBrokerQueryInterface-Like methods querying schemaOps.  These are used
          by 'SchemaDataBroker' to support querying of schemaOps info. ***/



   /**
	*  	returns the root DN of the schemaOps subentry as a string.
	*  	@return the schemaOps subentry (i.e. something like 'cn=schemaOps')
	*/

    public String getSchemaRoot()
    {
        if (ctx==null)
            return "";

        String fnord = "";
        //System.out.println("long shot...");
        try
        {
            fnord = ctx.getSchema("").toString();
            return fnord;
        }
        catch (NamingException e)
            { log.warning("error reading schemaOps\n" + e);}


        return "cn=schemaOps"; // default: often what is anyway... :-)
    }



   /**
	*    Gets a list of the object classes most likely
	*    to be used for the next Level of the DN...
	*    @param dn the dn of the parent to determine likely
	*              child object classes for
	*    @return list of recommended object classes...
	*/
    public ArrayList unthreadedGetRecOCs(DN dn)
    {
        return schemaOps.getRecommendedObjectClasses(dn.toString());
    }

   /**
	*   Modifies the attributes associated with a named object using an an ordered list of modifications.
	*   @param dn  distinguished name of object to modify
	*   @param mods an ordered sequence of modifications to be performed; may not be null
	*/

    public void modifyAttributes(DN dn, ModificationItem[] mods)
        throws NamingException
    {
        if (ctx == null)
            throw new NamingException("no directory context to work with");

        if (readOnly)
            throw new NamingException(CBIntText.get("JXplorer is in read only mode; no directory modifications allowed"));

        ctx.modifyAttributes(dn, mods);
    }

   /**
	* 	Usually shells to CBUtility.error, but will log instead if quiet mode
	* 	is set.
	*	@param msg the error message to display.
	*	@param e the exception caused.
	*/

    public void error(String msg, Exception e)
    {
        if (quietMode == false)
        {
            CBUtility.error(msg, e);
        }
        else
        {
            errorWhileQuietFlag = true;
            log.warning(msg + "\n   (details) " + e.toString());
        }
    }



   /**
    *	@return the directory operations object.
	*/

    // public CBGraphicsOps getDirOp() { return dirOps; }



   /**
    *	@return the directory operations context.
	*/

    public LdapContext getLdapContext()
        throws NamingException
    {
        if (readOnly)
            throw new NamingException(CBIntText.get("JXplorer is in read only mode; no directory modifications allowed"));

        return (dirOps==null)?null:dirOps.getContext();
    }


    /**
     * Adds the provided LDAP environement values (e.g. 'java.naming.ldap.derefAliases=finding')
     * to the underlying LDAP context.
     *
     * @param key
     * @param value
     * @throws NamingException
     */
    public void addToEnvironment(String key, String value)
            throws NamingException
    {
        if (dirOps != null && dirOps.getContext() != null)
        {
            dirOps.getContext().addToEnvironment(key, value);
        }
        else
            throw new NamingException("Unable to set environement variables; no valid environment found"); // unexpected exception
    }


   /**
	*  	If the rootDN doesn't exist, we try to read the
	*  	the root DN from the server.  Not all servers
	*  	support this functionality, but some include a
	*  	<i>namingcontexts</i> attribute for their empty
	*  	entry...
	*  	@return the rootDN, or null if none was found. Under rare circumstances this may return multiple roots,
    *           however this will not always be handled gracefully!
	*/

    public DN[] readFallbackRoot()
    {
        log.finer("reading fallback root DN...");
        try
        {
            Attributes a = unthreadedReadEntry(new DN(""), new String[] {"namingContexts"});

            if (a==null) return null; // can't do anything, bail.

            log.finer("...Got root DSE data...");


//XXX namingContexts may need to be explicitly asked for, since it is an op att.

            Attribute rootDNC;
            rootDNC = a.get("namingcontexts");
            if (rootDNC == null)
                rootDNC = a.get("namingContexts");  // some servers do 'ave em...

            if (rootDNC == null || rootDNC.size()==0) return new DN[] {new DN()}; // can't do anything; bail with an empty DN.

            if (rootDNC.size() == 1)
            {
                String rootDNString = rootDNC.get().toString();

                log.info("read fallback root DN as: " + rootDNString);
                return new DN[] {new DN(rootDNString)};
            }

            //  Multiple Naming Contexts!!

            DN contexts[] = new DN[rootDNC.size()];

            int index=0;
            Enumeration roots = rootDNC.getAll();
            while (roots.hasMoreElements())
            {
                String dn = roots.nextElement().toString();
                contexts[index++] = new DN(dn);
            }
            return contexts;
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "Error reading fallback root: ",e);
            return null;
        }
    }



   /**
	*  	This sets the context to use either jndi 'searching' or 'never' alias
	*  	resolution, depending on the value of the jxplorer property
	*  	option.ldap.searchAliasBehaviour.
	*/

    public void SetContextToSearchingAliases()
    {

        try
        {
            if (ctx != null) ctx.addToEnvironment("java.naming.ldap.derefAliases", JXConfig.getProperty("option.ldap.searchAliasBehaviour"));
        }
        catch( Exception e)
        {
            log.warning("Unexpected Exception setting search alias handling behaviour in context to " + JXConfig.getProperty("option.ldap.searchAliasBehaviour") + "\n    " + e);
        }
    }



   /**
	*  	This sets the context to use either jndi 'browsing' or 'never' alias
	*  	resolution, depending on the value of the jxplorer property
	*  	option.ldap.browseAliasBehaviour.
	*/

    public void SetContextToBrowsingAliases()
    {
        try
        {
            if (ctx != null) ctx.addToEnvironment("java.naming.ldap.derefAliases", JXConfig.getProperty("option.ldap.browseAliasBehaviour"));
        }
        catch( Exception e)
        {
            log.warning("Unexpected exception setting browse alias handling behaviour in context to " + JXConfig.getProperty("option.ldap.browseAliasBehaviour") + "\n    " + e);
        }
    }

    /**
     *  Returns a schemaOps ops object capable of answering questions about schemaOps and syntax.
     * @return a schemaOps aware schemaOps object linked to the same directory context as the broker.
     */
    public SchemaOps getSchemaOps()
    {
        return schemaOps;
    }

    public String id() { return "JNDIDataBroker " + id;};

}