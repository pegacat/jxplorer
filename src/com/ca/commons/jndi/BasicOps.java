package com.ca.commons.jndi;

import java.util.Properties;
import java.util.Hashtable;
import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.logging.*;


/**
 * <p>The BasicOps class contains methods for performing basic
 * directory operations.  Errors are generally caught and handled locally,
 * although return codes usually indicate the general success status of
 * operations.  </p>
 * <p/>
 * <p>Two methods, error() and log() are defined.  These are intended to be
 * over-ridden by programs wishing application specific handling of these
 * (i.e. for more sensible user output than System.out.println()...).</p
 */

public class BasicOps extends JNDIOps
{
    private final static Logger log = Logger.getLogger(BasicOps.class.getName());

    protected int ldapVersion = -1;  // ldap version of the current connection (-1 = not connected)

    String errorMsg;                     //TE: a record of the last error msg.
    Exception errorException = null;     //TE: a record of the last exception.


    /**
     * Initialise a Basic Operation object with a context.
     */

    public BasicOps(LdapContext c)
            throws NamingException
    {
        super(c);
        setLdapVersion(c.getEnvironment());
    }

    /**
     * Create a new BasicOps object, initialised
     * with an ldap context created from the connectionData,
     * and maintaining a reference to that connectionData.
     *
     * @param cData contains all the connection details.  (Effectively
     *              a structured form of the jndi environment object).
     */
    public BasicOps(ConnectionData cData)
            throws NamingException
    {
        super(cData.getJNDIEnvironment());
    }

    /**
     * Factory Method to create BasicOps objects, initialised
     * with an ldap context created from the connectionData,
     * and maintaining a reference to that connectionData.
     *
     * @param cData the details of the directory to connect to
     * @return a BasicOps object.
     */

    public static BasicOps getInstance(ConnectionData cData)
            throws NamingException
    {
        BasicOps newObject = new BasicOps(cData);
        return newObject;
    }

    /**
     * Open an initial context.
     * Will open an initial context which can then be used to construct a
     * BasicOps object. Note that this method may take some time to return.
     *
     * @param connectionData a data object contain all the connection details.
     */

    public static LdapContext openContext(ConnectionData connectionData)
            throws NamingException
    {
        return openContext(connectionData.getJNDIEnvironment());
    }


    /**
     * This static ftn. can be used to open an initial context (which can then
     * be used to construct a BasicOps object).  Note that this ftn may take some
     * time to return...
     *
     * @param version       the LDAP Version (2 or 3) being used.
     * @param host          the LDAP server name.
     * @param port          the LDAP server port (default 389) being used.
     * @param user          the Manager User's DN - (is null if user is not manager)
     * @param pwd           the Manager User's password - (is null if user is not manager)
     * @param tracing       whether to set BER tracing on or not
     * @param referralType  the jndi ldap referral type: [follow:ignore:throw]
     * @param aliasHandling how aliases should be handled in searches ('always'|'never'|'find'|'search')
     * @return The created context.
     * @deprecated use getInstance() instead
     */

    public static LdapContext openContext(int version, String host, int port, String user, char[] pwd,
                                         boolean tracing, String referralType, String aliasHandling)
            throws NamingException
    {
        if (host == null)
            throw new NamingException("Host not specified in openContext()!");

        if (port == 0) port = 389;

        return openContext(version, ("ldap://" + host + ":" + port), user, pwd, tracing, referralType, aliasHandling);
    }


    /**
     * Opens a simple default initial context, with no authentication, using version 3 ldap.
     *
     * @deprecated use getInstance() instead.
     */

    public static LdapContext openContext(String url)
            throws NamingException
    {
        ConnectionData myData = new ConnectionData();
        myData.url = url;

        return openContext(3,
                url,
                "",
                null,
                false,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null);
    }


    /**
     * Opens an initial context with (optional) authentication and configurable ldap version.
     *
     * @param version       the LDAP Version (2 or 3) being used.
     * @param url           a url of the form ldap://hostname:portnumber
     * @param managerUserDN the Manager User's distinguished name (optionally null if not used)
     * @param pwd           the Manager User's password - (is null if user is not manager)
     */
/*
   public static LdapContext openContext(int version, String url, String userDN,
                                        char[] pwd, boolean tracing,
                                        String referralType, String aliasType,
                                        boolean useSSL, String cacerts, String clientcerts,
                                        char[] caKeystorePwd, char[] clientKeystorePwd,
                                        String caKeystoreType, String clientKeystoreType )
*/

    public static LdapContext openContext(int version,
                                         String url,
                                         String managerUserDN,
                                         char[] pwd)
            throws NamingException
    {
        return openContext(version,
                url,
                managerUserDN,
                pwd,
                false,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null);
    }


    /**
     * This static ftn. can be used to open an initial context (which can then
     * be used to construct a BasicOps object).  Note that this ftn may take some
     * time to return...
     *
     * @param version       the LDAP Version (2 or 3) being used.
     * @param url           a url of the form ldap://hostname:portnumber
     * @param userDN        the Manager User's distinguished name (optionally null if not used)
     * @param pwd           the Manager User's password - (is null if user is not manager)
     * @param tracing       whether to set BER tracing on or not
     * @param referralType  the jndi ldap referral type: [follow:ignore:throw] (may be null - defaults to 'follow')
     * @param aliasHandling
     * @return The created context.
     * @deprecated use getInstance() instead
     */

    public static LdapContext openContext(int version, String url, String userDN, char[] pwd, boolean tracing, String referralType, String aliasHandling)
            throws NamingException
    {
        return openContext(version,
                url,
                userDN,
                pwd,
                tracing,
                referralType,
                aliasHandling,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null);
    }
     /**
     * This static ftn. can be used to open an initial context (which can then
     * be used to construct a BasicOps object).  Note that this ftn may take some
     * time to return...
     *
     * @param version            the LDAP Version (2 or 3) being used.
     * @param url                a url of the form ldap://hostname:portnumber.
     * @param userDN             the Manager User's distinguished name (optionally null if not used).
     * @param pwd                the Manager User's password - (is null if user is not manager).
     * @param tracing            whether to set BER tracing on or not.
     * @param referralType       the jndi ldap referral type: [follow:ignore:throw] (may be null - defaults to 'follow').
     * @param aliasType          how aliases should be handled in searches ('always'|'never'|'find'|'search').
     * @param useSSL             whether to use SSL (either simple or client-authenticated).
     * @param cacerts            the file containing the trusted server certificates (no keys).
     * @param clientcerts        the file containing client certificates.
     * @param caKeystorePwd      the password to the ca's keystore (may be null for non-client authenticated ssl).
     * @param clientKeystorePwd  the password to the client's keystore (may be null for non-client authenticated ssl).
     * @param caKeystoreType     the type of keystore file; e.g. 'JKS', or 'PKCS12'.
     * @param clientKeystoreType the type of keystore file; e.g. 'JKS', or 'PKCS12'.
     * @return The created context.
      * @deprecated use longer version with 'useGSSAPI' option instead.
     */

    public static LdapContext openContext(int version,
                                         String url,
                                         String userDN,
                                         char[] pwd,
                                         boolean tracing,
                                         String referralType,
                                         String aliasType,
                                         boolean useSSL,
                                         String cacerts,
                                         String clientcerts,
                                         char[] caKeystorePwd,
                                         char[] clientKeystorePwd,
                                         String caKeystoreType,
                                         String clientKeystoreType)
            throws NamingException
    {
     return openContext(version,
                url,
                userDN,
                pwd,
                tracing,
                referralType,
                aliasType,
                useSSL,
                cacerts,
                clientcerts,
                caKeystorePwd,
                clientKeystorePwd,
                caKeystoreType,
                clientKeystoreType,
                false,
                null);
    }

    /**
     * This static ftn. can be used to open an initial context (which can then
     * be used to construct a BasicOps object).  Note that this ftn may take some
     * time to return...
     *
     * @param version            the LDAP Version (2 or 3) being used.
     * @param url                a url of the form ldap://hostname:portnumber.
     * @param userDN             the Manager User's distinguished name (optionally null if not used).
     * @param pwd                the Manager User's password - (is null if user is not manager).
     * @param tracing            whether to set BER tracing on or not.
     * @param referralType       the jndi ldap referral type: [follow:ignore:throw] (may be null - defaults to 'follow').
     * @param aliasType          how aliases should be handled in searches ('always'|'never'|'find'|'search').
     * @param useSSL             whether to use SSL (either simple or client-authenticated).
     * @param cacerts            the file containing the trusted server certificates (no keys).
     * @param clientcerts        the file containing client certificates.
     * @param caKeystorePwd      the password to the ca's keystore (may be null for non-client authenticated ssl).
     * @param clientKeystorePwd  the password to the client's keystore (may be null for non-client authenticated ssl).
     * @param caKeystoreType     the type of keystore file; e.g. 'JKS', or 'PKCS12'.
     * @param clientKeystoreType the type of keystore file; e.g. 'JKS', or 'PKCS12'.
     * @param useGSSAPI          use GSSAPI (usually with kerberos)
     * @param templateName       optional user friendly name for this connection set, used by GUI windows
     * @return The created context.
     */

    public static LdapContext openContext(int version,
                                         String url,
                                         String userDN,
                                         char[] pwd,
                                         boolean tracing,
                                         String referralType,
                                         String aliasType,
                                         boolean useSSL,
                                         String cacerts,
                                         String clientcerts,
                                         char[] caKeystorePwd,
                                         char[] clientKeystorePwd,
                                         String caKeystoreType,
                                         String clientKeystoreType,
                                         boolean useGSSAPI,
                                         String templateName)
            throws NamingException
    {
        ConnectionData connectionData =
                new ConnectionData(version,
                        url,
                        userDN,
                        pwd,
                        tracing,
                        referralType,
                        aliasType,
                        useSSL,
                        cacerts,
                        clientcerts,
                        caKeystorePwd,
                        clientKeystorePwd,
                        caKeystoreType,
                        clientKeystoreType,
                        useGSSAPI, templateName, null);

        return JNDIOps.openContext(connectionData.getJNDIEnvironment());
    }


    /**
     * This is a raw interface to javax.naming.directory.InitialLdapContext, that allows
     * an arbitrary environment string to be passed through.  Often the other version
     * of openContext() above will prove more convenient.
     *
     * @param env a list of environment variables for the context
     * @return a newly created LdapContext.
     */

    public static LdapContext openContext(Properties env)
            throws NamingException
    {
        log.fine("opening Directory Context to " + env.get(Context.PROVIDER_URL) + "\n using: " + env.get(Context.INITIAL_CONTEXT_FACTORY));

        LdapContext ctx = new InitialLdapContext(env, null);

        log.fine("context successfully opened " + (ctx != null));

        if (ctx != null)
        {

        }
        return ctx;
    }

    private void setLdapVersion(Hashtable env) throws NamingException
    {
        try
        {
            ldapVersion = Integer.parseInt(env.get("java.naming.ldap.version").toString());
        }
        catch (Exception e)
        {
            throw new NamingException("BasicOps.openContext(): unable to determine ldap version of connection.");
        }
    }


    /**
     * A simple wrapper for a ctx.getSchema("") call.
     *
     * @deprecated - jndi's 'getSchema' may not always be available (e.g. not implemented in dsml).
     *             use 'getSchemaAttributes()' instead
     */
/*
    public DirContext getSchema() throws NamingException
    {
        if (getContext() == null)
            throw new NamingException("No context open to retrieve Schema from");

        log.finest("getSchema() call");

        return getContext().getSchema("");
    }
*/
    /**
     * basically a wrapper for context.rename... changes the
     * distinguished name of an object, checks for error.
     *
     * @param OldDN current distinguished name of an object.
     * @param NewDN the name it is to be changed to.
     * @deprecated use renameEntry instead
     */

//XXXX - this will fail for single valued manditory attributes.
//XXXX - since using 'deleteRDN = false' - 30 May 2002.

    public void renameObject(Name OldDN, Name NewDN)
            throws NamingException
    {
        renameEntry(OldDN, NewDN);
    }


    /**
     * Copies an object to a new DN by the simple expedient of adding
     * an object with the new DN, and the attributes of the old object.
     *
     * @param FromDN the original object being copied
     * @param ToDN   the new object being created
     * @deprecated use copyEntry instead
     */

    public void copyObject(Name FromDN, Name ToDN)
            throws NamingException
    {
        copyEntry(FromDN, ToDN);
    }



    /**
     * creates a new object (subcontext) with the given
     * dn and attributes.
     *
     * @param Dn   the distinguished name of the new object
     * @param atts attributes for the new object
     * @deprecated use addEntry instead
     */

    public void addObject(Name Dn, Attributes atts)
            throws NamingException
    {
        addEntry(Dn, atts);
    }

    /**
     * deletes a leaf entry (subcontext).  It is
     * an error to attempt to delete an entry which is not a leaf
     * entry, i.e. which has children.
     *
     * @deprecated use deleteEntry instead
     */

    public void deleteObject(Name Dn)
            throws NamingException
    {
        deleteEntry(Dn);
    }

    /**
     * Updates an object with a new set of attributes
     *
     * @param Dn   distinguished name of object to update
     * @param atts the new attributes to update the object with.
     * @deprecated use updateEntry instead
     */

    public void updateObject(Name Dn, Attributes atts)
            throws NamingException
    {
        updateEntry(Dn, atts);
    }



    /**
     * Performs a base object search (i.e. just a search of the current entry, nothing below it),
     * returning no attributes (i.e. just DNs);
     *
     * @param Searchbase the domain name (relative to initial context in ldap) to seach from.
     * @param filter     the non-null filter to use for the search
     * @param limit      the maximum number of results to return
     * @param timeout    the maximum time to wait before abandoning the search
     * @return list of search results ('SearchResult's); entries matching the search filter.
     * @deprecated use searchBaseEntry instead
     */

    public NamingEnumeration searchBaseObject(Name Searchbase, String filter, int limit, int timeout)
            throws NamingException
    {
        return searchBaseEntry(Searchbase, filter, limit, timeout, new String[]{"objectClass"});
    }


    /**
     * Performs a base object search (i.e. just a search of the current entry, nothing below it).
     *
     * @param Searchbase       the domain name (relative to initial context in ldap) to seach from.
     * @param filter           the non-null filter to use for the search
     * @param limit            the maximum number of results to return
     * @param timeout          the maximum time to wait before abandoning the search
     * @param returnAttributes an array of strings containing the names of attributes to search. (null = all, empty array = none)
     * @return list of search results ('SearchResult's); entries matching the search filter.
     * @deprecated use searchBaseEntry instead
     */

    public NamingEnumeration searchBaseObject(Name Searchbase, String filter, int limit,
                                              int timeout, String[] returnAttributes)
            throws NamingException
    {
        return searchBaseEntry(Searchbase, filter, limit, timeout, returnAttributes);
    }


    /**
     * Shuts down the current context.<p>
     * nb. It is not an error to call this method multiple times.
     */

    public void close()
            throws NamingException
    {
        super.close();

        log.fine("closing context");

        ldapVersion = -1;
    }

    /**
     * This picks up the name parser used at the root level... if
     * the context only spans a single name space (i.e. for an ldap
     * directory) this will be the same as the one used throughout.
     */

    public NameParser getBaseNameParser()
            throws NamingException
    {
        log.finer("getting base name parser");

        if (getContext() == null)
            throw new NamingException("Null Directory Context\n  in BasicOps.searchSubTree()\n  (so can't do anything!)");

        return getContext().getNameParser("");
    }

    /**
     * Returns the ldap version of the current connection
     */

    public int getLdapVersion()
    {
        return ldapVersion;
    }

}
