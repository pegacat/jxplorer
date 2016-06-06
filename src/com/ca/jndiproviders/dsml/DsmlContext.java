package com.ca.jndiproviders.dsml;

import com.ca.commons.naming.DXNamingEnumeration;
import com.ca.commons.naming.DN;
import com.ca.commons.cbutil.CBBase64;
import com.ca.commons.cbutil.CBBase64EncodingException;

import javax.naming.directory.*;
import javax.naming.*;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.LdapContext;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

/**
 * <p>This is a DSML jndiproviders context, that provides support for all the basic DSML operations.</p>
 * <p/>
 * <p>However, it deliberately does *not* support:</p>
 * <ul>
 * <li>binding java objects - the bind methods are not implemented.  Users will need to do their
 * own serialisation if they want this functionality.
 * <li> referrals aren't implemented yet.
 * <li> ldap v3 extensions.
 * <li> composite names spanning multiple namespaces - all names are DSML names only.  (Composite
 * names are a bloody stupid idea that come close to making jndiproviders unusable, IMNSHO - CB)
 * </ul>
 * <p/>
 * <p>If you need the above features, you may be better off using the Sun DSML provider which is
 * far more ambitious in scope (but which has some licencing problems, and is a bit rocky in parts).</p>
 */

/*
 *   DSML SERVER ERROR NOTES
 *
 *   Server is incorrectly parsing escaped dns - e.g. dn="cn=Vivienne \"LEVER\", ... ".
 *   Server is returning HTTP 500, which prevents parsing of returned soap error
 *
 */


//TODO: schema support??

//TODO: we ended up using strings rather than names, so adjust all methods to chain to the string methods instead of the name methods.

//TODO: makes heavy use of static stuff - may not be thread safe.

public class DsmlContext implements LdapContext
{

    // Formatting
    private static int TABLEN = 4;
    private static String TAB = "    ";
    private static String TAB2 = TAB + TAB;
    private static String TAB3 = TAB2 + TAB;
    private static String TAB4 = TAB3 + TAB;
    private static String TAB5 = TAB4 + TAB;
    private static String TAB6 = TAB5 + TAB;

    private static String SOAPHEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            TAB + "<soap-env:Envelope xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            TAB2 + "<soap-env:Body>\n";

    private static String SOAPFOOTER = TAB2 + "</soap-env:Body>\n" +
            TAB + "</soap-env:Envelope>";

    private static String DSMLHEADER = TAB3 + "<dsml:batchRequest xmlns:dsml=\"urn:oasis:names:tc:DSML:2:0:core\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n";

    private static String DSMLFOOTER = TAB3 + "</dsml:batchRequest>\n";

    private static String SEARCHFOOTER = TAB3 + "</dsml:searchRequest>\n";

    private static String STANDARDHEADER = SOAPHEADER + DSMLHEADER;

    private static String STANDARDFOOTER = DSMLFOOTER + SOAPFOOTER;

    // SEARCH SCOPE CONSTANTS
    private static String BASEOBJECT = "baseObject";
    private static String SINGLELEVEL = "singleLevel";
    private static String WHOLESUBTREE = "wholeSubtree";
    private static String[] SCOPEOPTIONS = new String[]{BASEOBJECT, SINGLELEVEL, WHOLESUBTREE};

    // SEARCH ALIAS DEREF OPTIONS
    private static String NEVER = "neverDerefAliases";
    private static String SEARCHING = "derefInSearching";
    private static String FINDING = "derefFindingBaseObj";
    private static String ALWAYS = "derefAlways";
    private static String[] ALIASOPTIONS = new String[]{NEVER, SEARCHING, FINDING, ALWAYS};

    // Default Search Controls
    private static SearchControls DEFAULT_SEARCH_CONTROLS = new SearchControls();


    protected Hashtable environment;

    // the name of the context (as set by 'createSubcontext' methods
    private String contextName = "";

    private static Logger log = Logger.getLogger(DsmlContext.class.getName());

    // Debug
    {
        log.setLevel(Level.FINEST);
    }

    /**
     * <p>This constructs the XML tag and tag attributes for a DSML search operation start tag.  (filter and attributes elements
     * must be added separately, the end tag is a defined constant.)
     * <p/>
     * <p>e.g.
     * <pre>
     * &lt;dsml:searchRequest requestID="1" dn="cn=schema" scope="baseObject" derefAliases="derefInSearching"&gt;
     * </pre>
     * <p/>
     * something like
     *
     * @param searchHeader an existing string buffer to append the search header information in.
     * @param dn           the dn to search from
     * @param scope        the search scope, must be one of 'baseLevel', 'singleLevel' or 'wholeSubtree' (see constants)
     * @param derefAliases how to handle aliases.  must be on of 'neverDerefAliases', 'derefInSearching', 'derefFindingBaseObj', 'derefAlways' (see constants)
     * @param sizeLimit    the LDAP search size limit - default is 0 (unlimited by client)
     * @param timeLimit    the LDAP search time limit - default is 0 (unlimited by client)
     * @param typesOnly    - no idea, but it's in the spec.
     * @return a string buffer containing the raw xml for a search operation.
     * @throws NamingException
     */
    private static StringBuffer getSearchRequestHeader(StringBuffer searchHeader, String dn, String scope, String derefAliases, long sizeLimit, int timeLimit, boolean typesOnly)
            throws NamingException
    {
        // sanity check arguments
        if (!checkValidity(SCOPEOPTIONS, scope))
            throw new NamingException("search scope argument '" + scope + "' is invalid");

        if (!checkValidity(ALIASOPTIONS, derefAliases))
        {
            if (derefAliases != null) log.info("bad alias option passed '" + derefAliases + "'");
            derefAliases = SEARCHING;
        }

        if (searchHeader == null)
            searchHeader = new StringBuffer();

        searchHeader.append(TAB4 + "<dsml:searchRequest dn=\"");     // TODO: requestID is optional - let's drop it in the final version - CB
        searchHeader.append(escapeName(dn));
        searchHeader.append("\" scope=\"").append(scope);
        searchHeader.append("\" derefAliases=\"").append(derefAliases);

        if (sizeLimit > 0)
            searchHeader.append("\" sizeLimit=\"").append(sizeLimit);

        if (timeLimit > 0)
            searchHeader.append("\" timeLimit=\"").append(timeLimit);

        // never used?
        if (typesOnly == true)
            searchHeader.append("\" typesOnly=\"").append(typesOnly);

        searchHeader.append("\">\n");

        log.finest("created search header: " + searchHeader);

        return searchHeader;
    }


    /**
     * <p>This constructs the XML tag and elements for a DSML search attribute list
     * <p/>
     * e.g.
     * <pre>
     * &gt;dsml:attributes&lt;
     * &gt;dsml:attribute name="attributeTypes"/&lt;
     * &gt;dsml:attribute name="objectClasses"/&lt;
     * &gt;dsml:attribute name="matchingRules"/&lt;
     * &gt;dsml:attribute name="ldapSyntaxes"/&lt;
     * &gt;dsml:attribute name="*"/&lt;
     * &gt;/dsml:attributes&lt;
     * </pre>
     *
     * @param searchAttributes an existing string buffer to append the search attribute list information to
     * @param attributes       a string array of attribute names
     * 
     * @return the search request attributes
     */
    private static StringBuffer getSearchRequestAttributes(StringBuffer searchAttributes, String[] attributes)
    {
        if (attributes == null || attributes.length == 0)
        {
            return searchAttributes;  // do nothing if there are no specific attributes (means return all)
        }

        if (searchAttributes == null)
            searchAttributes = new StringBuffer(40 + 80 * attributes.length);

        searchAttributes.append(TAB5 + "<dsml:attributes>\n");

        int len = attributes.length;
        for (int i = 0; i < len; i++)
        {
            searchAttributes.append(TAB6 + "<dsml:attribute name=\"").append(attributes[i]).append("\"/>\n");
        }

        searchAttributes.append(TAB5 + "</dsml:attributes>\n");

        log.finest("created search attribute list: " + searchAttributes);

        return searchAttributes;
    }


    /**
     * Small utility method to check that a string is one of a number of valid options.
     *
     * @param options an array of valid options.
     * @param checkme a string to compare against the previous array.
     * @return true if checkme is one of the options, false if it is null, or is not one of the options.
     */
    private static boolean checkValidity(String[] options, String checkme)
    {
        if (checkme != null)
        {
            for (int i = 0; i < options.length; i++)
                if (options[i].equals(checkme))
                    return true;
        }
        return false;
    }

    /**
     * A little test method to produce dummy enumerations during development
     *
     * @param name
     * @param num
     * 
     * @return the test enumeration
     */
    private NamingEnumeration getTestEnumeration(Name name, int num)
    {
        log.finest("generating " + num + " test names from '" + name.toString() + "'");

        String itemBase = "c=AU";
        if (name.size() == 1)
            itemBase = "o=beta";
        else if (name.size() == 2)
            itemBase = "ou=gamma";
        else if (name.size() == 3)
            itemBase = "ou=delta";

        DXNamingEnumeration testEnumeration = new DXNamingEnumeration();
        for (int i = 0; i < num; i++)
        {
            String itemName = itemBase + i;
            testEnumeration.add(new SearchResult(itemName, null, getTestAttributes(itemName)));
        }

        return testEnumeration;
    }

    /**
     * A little test method to produce dummy attributes objects during development.
     *
     * @param name
     * 
     * @return the test attributes
     */
    private BasicAttributes getTestAttributes(String name)
    {
        log.finest("generating test data from name '" + name + "'");
        BasicAttributes testAttributes = new BasicAttributes("cn", name);
        testAttributes.put(new BasicAttribute("objectClass", "person"));
        testAttributes.put(new BasicAttribute("sn", "Test"));
        return testAttributes;
    }

    private DsmlContext()
    {
    }  // don't do this :-).


    private DsmlContext(String baseDN, Hashtable env)
    {
        contextName = baseDN==null?"":baseDN;

        environment = env;     // Don't see any reason to clone environment for every entry?
    }

    /**
     * Called by DsmlCtxFactory / also used for testing
     *
     * @param env the environment to use for this connection...
     */
    DsmlContext(Hashtable env)
    {
        environment = (env == null) ? new Hashtable() : env;
        log.fine("Created DsmlContext");
    }

    /**
     * Retrieves all of the attributes associated with a named object.
     * See {@link #getAttributes(javax.naming.Name)} for details.
     *
     * @param name the name of the object from which to retrieve attributes
     * @return	the set of attributes associated with <code>name</code>
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(String name) throws NamingException
    {
        return getAttributes(name, null);
    }

    /**
     * Modifies the attributes associated with a named object.
     * See {@link #modifyAttributes(javax.naming.Name, int, javax.naming.directory.Attributes)} for details.
     *
     * @param name   the name of the object whose attributes will be updated
     * @param mod_op the modification operation, one of:
     *               <code>ADD_ATTRIBUTE</code>,
     *               <code>REPLACE_ATTRIBUTE</code>,
     *               <code>REMOVE_ATTRIBUTE</code>.
     * @param attrs  the attributes to be used for the modification; map not be null
     * @throws	javax.naming.directory.AttributeModificationException if the modification cannot
     * be completed successfully
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException
    {
        ModificationItem[] mods = new ModificationItem[attrs.size()];
        Enumeration attObjects = attrs.getAll();
        int i = 0;
        while (attObjects.hasMoreElements())
        {
            mods[i++] = new ModificationItem(mod_op, (Attribute) attObjects.nextElement());
        }

        modifyAttributes(name, mods);

    }

    /**
     * Retrieves all of the attributes associated with a named object.
     * See the class description regarding attribute models, attribute
     * type names, and operational attributes.
     *
     * @param name the name of the object from which to retrieve attributes
     * @return	the set of attributes associated with <code>name</code>.
     * Returns an empty attribute set if name has no attributes;
     * never null.
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #getAttributes(String)
     * @see #getAttributes(javax.naming.Name, String[])
     */
    public Attributes getAttributes(Name name) throws NamingException
    {
        return getAttributes(name, null);
    }

    /**
     * Modifies the attributes associated with a named object.
     * The order of the modifications is not specified.  Where
     * possible, the modifications are performed atomically.
     *
     * @param name   the name of the object whose attributes will be updated
     * @param mod_op the modification operation, one of:
     *               <code>ADD_ATTRIBUTE</code>,
     *               <code>REPLACE_ATTRIBUTE</code>,
     *               <code>REMOVE_ATTRIBUTE</code>.
     * @param attrs  the attributes to be used for the modification; may not be null
     * @throws	javax.naming.directory.AttributeModificationException if the modification cannot
     * be completed successfully
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #modifyAttributes(javax.naming.Name, javax.naming.directory.ModificationItem[])
     */
    public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException
    {
        modifyAttributes(name.toString(), mod_op, attrs);
    }

    /**
     * Retrieves the schema associated with the named object.
     * See {@link #getSchema(javax.naming.Name)} for details.
     *
     * @param name the name of the object whose schema is to be retrieved
     * @return	the schema associated with the context; never null
     * @throws	javax.naming.OperationNotSupportedException if schema not supported
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated not yet implemented
     */
    public LdapContext getSchema(String name) throws NamingException
    {
        throw new OperationNotSupportedException("DsmlContext does not support reading schema (yet)");
    }

    /**
     * Retrieves a context containing the schema objects of the
     * named object's class definitions.
     * See {@link #getSchemaClassDefinition(javax.naming.Name)} for details.
     *
     * @param name the name of the object whose object class
     *             definition is to be retrieved
     * @return	the <tt>LdapContext</tt> containing the named
     * object's class definitions; never null
     * @throws	javax.naming.OperationNotSupportedException if schema not supported
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated not yet implemented
     */
    public LdapContext getSchemaClassDefinition(String name) throws NamingException
    {
        throw new OperationNotSupportedException("DsmlContext does not support reading schema (yet)");
    }

    /**
     * Retrieves the schema associated with the named object.
     * The schema describes rules regarding the structure of the namespace
     * and the attributes stored within it.  The schema
     * specifies what types of objects can be added to the directory and where
     * they can be added; what mandatory and optional attributes an object
     * can have. The range of support for schemas is directory-specific.
     * <p/>
     * <p> This method returns the root of the schema information tree
     * that is applicable to the named object. Several named objects
     * (or even an entire directory) might share the same schema.
     * <p/>
     * <p> Issues such as structure and contents of the schema tree,
     * permission to modify to the contents of the schema
     * tree, and the effect of such modifications on the directory
     * are dependent on the underlying directory.
     *
     * @param name the name of the object whose schema is to be retrieved
     * @return	the schema associated with the context; never null
     * @throws	javax.naming.OperationNotSupportedException if schema not supported
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated not yet implemented
     */
    public LdapContext getSchema(Name name) throws NamingException
    {
        throw new OperationNotSupportedException("DsmlContext does not support reading schema (yet)");
    }

    /**
     * Retrieves a context containing the schema objects of the
     * named object's class definitions.
     * <p/>
     * One category of information found in directory schemas is
     * <em>class definitions</em>.  An "object class" definition
     * specifies the object's <em>type</em> and what attributes (mandatory
     * and optional) the object must/can have. Note that the term
     * "object class" being referred to here is in the directory sense
     * rather than in the Java sense.
     * For example, if the named object is a directory object of
     * "Person" class, <tt>getSchemaClassDefinition()</tt> would return a
     * <tt>LdapContext</tt> representing the (directory's) object class
     * definition of "Person".
     * <p/>
     * The information that can be retrieved from an object class definition
     * is directory-dependent.
     * <p/>
     * Prior to JNDI 1.2, this method
     * returned a single schema object representing the class definition of
     * the named object.
     * Since JNDI 1.2, this method returns a <tt>LdapContext</tt> containing
     * all of the named object's class definitions.
     *
     * @param name the name of the object whose object class
     *             definition is to be retrieved
     * @return	the <tt>LdapContext</tt> containing the named
     * object's class definitions; never null
     * @throws	javax.naming.OperationNotSupportedException if schema not supported
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated not yet implemented
     */
    public LdapContext getSchemaClassDefinition(Name name) throws NamingException
    {
        throw new OperationNotSupportedException("DsmlContext does not support reading schema (yet)");
    }

    /**
     * Modifies the attributes associated with a named object using
     * an ordered list of modifications.
     * See {@link #modifyAttributes(javax.naming.Name, javax.naming.directory.ModificationItem[])} for details.
     *
     * @param name the name of the object whose attributes will be updated
     * @param mods an ordered sequence of modifications to be performed;
     *             may not be null
     * @throws	javax.naming.directory.AttributeModificationException if the modifications
     * cannot be completed successfully
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException
    {
        log.finest("modify Atts of (" + name + ")");

        // construct XML

        StringBuffer modRequestBuffer = constructModRequest(name, mods);

        // send XML to server

        String response = sendDSMLRequest(modRequestBuffer);

        // parse response XML

        parseModResponse(response);

    }

    private StringBuffer constructModRequest(String name, ModificationItem[] mods)
            throws NamingException
    {
        StringBuffer message = new StringBuffer(200);
        message.append(STANDARDHEADER);

        getModRequestElement(message, name, mods);

        message.append(STANDARDFOOTER);

        return message;
    }

    /**
     * @param message
     * @param name
     * @param mods
     */
    /*
    "   <dsml:modifyRequest dn=\"cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU\">" +
    "      <dsml:modification name=\"favoriteDrink\" operation=\"add\">" +
    "         <dsml:value>japanese slipper</dsml:value>" +
    "      </dsml:modification>" +
    "      <dsml:modification name=\"address\" operation=\"delete\">\n" +
    "          <dsml:value>21 Jump Street$New York$90210</dsml:value>\n" +
    "      </dsml:modification>\n" +
    "      <dsml:modification name=\"userPassword\" operation=\"replace\">" +
    "         <dsml:value xsi:type=\"xsd:base64Binary\">c2VjcmV0IHBhc3N3b3Jk</dsml:value>" +
    "      </dsml:modification>" +
    "   </dsml:modifyRequest>" +
    */
    static void getModRequestElement(StringBuffer message, String name, ModificationItem[] mods)
            throws NamingException
    {
        message.append(TAB4).append("<dsml:modifyRequest dn=\"").append(escapeName(name)).append("\">\n");

        for (int i = 0; i < mods.length; i++)
        {
            Attribute att = mods[i].getAttribute();
            NamingEnumeration values = att.getAll();
            switch (mods[i].getModificationOp())
            {
                case ADD_ATTRIBUTE:

                    message.append(TAB5).append("<dsml:modification name=\"").append(att.getID()).append("\" operation=\"add\">\n");

                    while (values.hasMore())
                        createDsmlValueElement(values.next(), message);

                    message.append(TAB5).append("</dsml:modification>\n");

                    break;

                case REPLACE_ATTRIBUTE:

                    message.append(TAB5).append("<dsml:modification name=\"").append(att.getID()).append("\" operation=\"replace\">\n");

                    while (values.hasMore())
                        createDsmlValueElement(values.next(), message);

                    message.append(TAB5).append("</dsml:modification>\n");

                    break;

                case REMOVE_ATTRIBUTE:

                    message.append(TAB5).append("<dsml:modification name=\"").append(att.getID()).append("\" operation=\"delete\">\n");

                    while (values.hasMore())
                        createDsmlValueElement(values.next(), message);

                    message.append(TAB5).append("</dsml:modification>\n");

                    break;
            }

        }
        message.append(TAB4).append("</dsml:modifyRequest>\n");

    }

    void parseModResponse(String response)
            throws NamingException
    {
        // check for a dsml error
        checkForError(response);

        // quick check that it really was a modify response...
        if (response.indexOf("modifyResponse>") == -1)
            throw new NamingException("Unexpected DSML Response to Modify Request:\n " + response);
    }

    //private static Pattern searchResultEntry = Pattern.compile("<searchResultEntry.*?dn=\"(.*?)\">(.*?)</searchResultEntry>", Pattern.DOTALL);

    //TODO: tighten up...
    private static Pattern errorResult = Pattern.compile("errorResponse.*?type=\"(.*?)\"", Pattern.DOTALL);
    private static Pattern errorMessage = Pattern.compile("message>(.*?)</", Pattern.DOTALL);
    private static Pattern errorDetail = Pattern.compile("detail>(.*?)</", Pattern.DOTALL);

    private static Pattern ldapResultCode = Pattern.compile("code=\"(.*?)\"", Pattern.DOTALL);

    private static Pattern ldapResultDesc = Pattern.compile("resultCode.*?descr=\"(.*?)\"", Pattern.DOTALL);

    private static Pattern ldapResultMsg = Pattern.compile("errorMessage>(.*?)</", Pattern.DOTALL);

    static void checkForError(String response)
            throws NamingException
    {
        Matcher error = errorResult.matcher(response);
        if (error.find())
        {
            String errorMsg = "Error Processing DSML Request: " + error.group(1);

            Matcher message = errorMessage.matcher(response);
            if (message.find())
                errorMsg = errorMsg + "\n" + message.group(1);

            Matcher detail = errorDetail.matcher(response);
            if (detail.find())
                errorMsg = errorMsg + "\n" + detail.group(1);

            throw new NamingException(errorMsg);
        }
        else
        {
            try
            {
                Matcher resultMatcher = ldapResultCode.matcher(response);
                resultMatcher.find();
                String resultCode = resultMatcher.group(1);

                int i = Integer.parseInt(resultCode);

                if (i == 0)
                    return;  // all good here.

                Matcher descMatcher = ldapResultDesc.matcher(response);
                String desc = "";
                if (descMatcher.find())
                    desc = descMatcher.group(1);

                Matcher msgMatcher = ldapResultMsg.matcher(response);
                String msg = "";
                if (msgMatcher.find())
                    msg = msgMatcher.group(1);

                throw new NamingException(desc + " Exception (LDAP " + resultCode + ")\n" + msg);

            }
            catch (NumberFormatException e)
            {
                throw new NamingException("Unable to parse result code in DSML Response\n" + response);
            }
            catch (IllegalStateException e)
            {
                throw new NamingException("Unable to find result code in DSML Response\n" + response);
            }
        }
    }

    /**
     * Modifies the attributes associated with a named object using
     * an ordered list of modifications.
     * The modifications are performed
     * in the order specified.  Each modification specifies a
     * modification operation code and an attribute on which to
     * operate.  Where possible, the modifications are
     * performed atomically.
     *
     * @param name the name of the object whose attributes will be updated
     * @param mods an ordered sequence of modifications to be performed;
     *             may not be null
     * @throws	javax.naming.directory.AttributeModificationException if the modifications
     * cannot be completed successfully
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #modifyAttributes(javax.naming.Name, int, javax.naming.directory.Attributes)
     * @see javax.naming.directory.ModificationItem
     */
    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException
    {
        modifyAttributes(name.toString(), mods);
    }

    /**
     * Searches in a single context for objects that contain a
     * specified set of attributes.
     * See {@link #search(javax.naming.Name, javax.naming.directory.Attributes)} for details.
     *
     * @param name               the name of the context to search
     * @param matchingAttributes the attributes to search for
     * @return	an enumeration of <tt>SearchResult</tt> objects
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(String name, Attributes matchingAttributes) throws NamingException
    {
        return search(name, matchingAttributes, null);
    }

    /**
     * Searches in a single context for objects that contain a
     * specified set of attributes.
     * This method returns all the attributes of such objects.
     * It is equivalent to supplying null as
     * the <tt>atributesToReturn</tt> parameter to the method
     * <code>search(Name, Attributes, String[])</code>, i.e. search(name, matchingAttributes, null)
     * <br>
     * See {@link #search(javax.naming.Name, javax.naming.directory.Attributes, String[])} for a full description.
     *
     * @param name               the name of the context to search
     * @param matchingAttributes the attributes to search for
     * @return	an enumeration of <tt>SearchResult</tt> objects
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #search(javax.naming.Name, javax.naming.directory.Attributes, String[])
     */
    public NamingEnumeration search(Name name, Attributes matchingAttributes) throws NamingException
    {
        return search(name, matchingAttributes, null);
    }

    /**
     * Binds a name to an object, along with associated attributes.
     * See {@link #bind(javax.naming.Name, Object, javax.naming.directory.Attributes)} for details.
     *
     * @param name  the name to bind; may not be empty
     * @param obj   the object to bind; possibly null
     * @param attrs the attributes to associate with the binding
     * @throws	javax.naming.NameAlreadyBoundException if name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if some "mandatory" attributes
     * of the binding are not supplied
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated this method will not be implemented
     */
    public void bind(String name, Object obj, Attributes attrs) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Binds a name to an object, along with associated attributes,
     * overwriting any existing binding.
     * See {@link #rebind(javax.naming.Name, Object, javax.naming.directory.Attributes)} for details.
     *
     * @param name  the name to bind; may not be empty
     * @param obj   the object to bind; possibly null
     * @param attrs the attributes to associate with the binding
     * @throws	javax.naming.directory.InvalidAttributesException if some "mandatory" attributes
     * of the binding are not supplied
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated this method will not be implemented
     */
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Binds a name to an object, along with associated attributes.
     * If <tt>attrs</tt> is null, the resulting binding will have
     * the attributes associated with <tt>obj</tt> if <tt>obj</tt> is a
     * <tt>LdapContext</tt>, and no attributes otherwise.
     * If <tt>attrs</tt> is non-null, the resulting binding will have
     * <tt>attrs</tt> as its attributes; any attributes associated with
     * <tt>obj</tt> are ignored.
     *
     * @param name  the name to bind; may not be empty
     * @param obj   the object to bind; possibly null
     * @param attrs the attributes to associate with the binding
     * @throws	javax.naming.NameAlreadyBoundException if name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if some "mandatory" attributes
     * of the binding are not supplied
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see javax.naming.Context#bind(javax.naming.Name, Object)
     * @see #rebind(javax.naming.Name, Object, javax.naming.directory.Attributes)
     * @deprecated this method will not be implemented
     */

    public void bind(Name name, Object obj, Attributes attrs) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Binds a name to an object, along with associated attributes,
     * overwriting any existing binding.
     * If <tt>attrs</tt> is null and <tt>obj</tt> is a <tt>LdapContext</tt>,
     * the attributes from <tt>obj</tt> are used.
     * If <tt>attrs</tt> is null and <tt>obj</tt> is not a <tt>LdapContext</tt>,
     * any existing attributes associated with the object already bound
     * in the directory remain unchanged.
     * If <tt>attrs</tt> is non-null, any existing attributes associated with
     * the object already bound in the directory are removed and <tt>attrs</tt>
     * is associated with the named object.  If <tt>obj</tt> is a
     * <tt>LdapContext</tt> and <tt>attrs</tt> is non-null, the attributes
     * of <tt>obj</tt> are ignored.
     *
     * @param name  the name to bind; may not be empty
     * @param obj   the object to bind; possibly null
     * @param attrs the attributes to associate with the binding
     * @throws	javax.naming.directory.InvalidAttributesException if some "mandatory" attributes
     * of the binding are not supplied
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see javax.naming.Context#bind(javax.naming.Name, Object)
     * @see #bind(javax.naming.Name, Object, javax.naming.directory.Attributes)
     * @deprecated this method will not be implemented
     */
    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Retrieves selected attributes associated with a named object.
     * See {@link #getAttributes(javax.naming.Name, String[])} for details.
     *
     * @param name    The name of the object from which to retrieve attributes
     * @param attrIds the identifiers of the attributes to retrieve.
     *                null indicates that all attributes should be retrieved;
     *                an empty array indicates that none should be retrieved.
     * @return	the requested attributes; never null
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException
    {
        log.finest("getAttributes (" + name.toString() + ")");

        NamingEnumeration en = doDsmlSearch(name.toString(), BASEOBJECT, SEARCHING, 0, 0, false, "(objectClass=*)", attrIds);

        if (en.hasMoreElements() == false)
            return new BasicAttributes();  // return empty attributes object for 'virtual' nodes (e.g. router entries, base DN RDNs)

        SearchResult result = (SearchResult) en.next();

        return result.getAttributes();
//return getTestAttributes(name.toString());  //To change body of implemented methods use File | Settings | File Templates.

    }

    /**
     * Retrieves selected attributes associated with a named object.
     * See the class description regarding attribute models, attribute
     * type names, and operational attributes.
     * <p/>
     * <p> If the object does not have an attribute
     * specified, the directory will ignore the nonexistent attribute
     * and return those requested attributes that the object does have.
     * <p/>
     * <p> A directory might return more attributes than was requested
     * (see <strong>Attribute Type Names</strong> in the class description),
     * but is not allowed to return arbitrary, unrelated attributes.
     * <p/>
     * <p> See also <strong>Operational Attributes</strong> in the class
     * description.
     *
     * @param name    the name of the object from which to retrieve attributes
     * @param attrIds the identifiers of the attributes to retrieve.
     *                null indicates that all attributes should be retrieved;
     *                an empty array indicates that none should be retrieved.
     * @return	the requested attributes; never null
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException
    {
        return getAttributes(name.toString(), attrIds);
    }

    /**
     * Creates and binds a new context, along with associated attributes.
     * See {@link #createSubcontext(javax.naming.Name, javax.naming.directory.Attributes)} for details.
     *
     * @param name  the name of the context to create; may not be empty
     * @param attrs the attributes to associate with the newly created context
     * @return	the newly created context
     * @throws	javax.naming.NameAlreadyBoundException if the name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if <code>attrs</code> does not
     * contain all the mandatory attributes required for creation
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public LdapContext createSubcontext(String name, Attributes attrs) throws NamingException
    {
        log.finest("createSubcontext (" + name.toString() + ")");
// construct XML

        StringBuffer addRequestBuffer = constructAddRequest(name, attrs);

// send XML to server

        String response = sendDSMLRequest(addRequestBuffer);

// parse response XML

        parseAddResponse(response);

// return new context with new name?

        return new DsmlContext(name, environment);
    }

    void parseAddResponse(String response)
            throws NamingException
    {
        checkForError(response);
// quick check that it really was a modify response...
        if (response.indexOf("addResponse>") == -1)
            throw new NamingException("Unexpected DSML Response to Add Request:\n " + response);

    }

    /**
     * Creates and binds a new context, along with associated attributes.
     * This method creates a new subcontext with the given name, binds it in
     * the target context (that named by all but terminal atomic
     * component of the name), and associates the supplied attributes
     * with the newly created object.
     * All intermediate and target contexts must already exist.
     * If <tt>attrs</tt> is null, this method is equivalent to
     * <tt>Context.createSubcontext()</tt>.
     *
     * @param name  the name of the context to create; may not be empty
     * @param attrs the attributes to associate with the newly created context
     * @return	the newly created context
     * @throws	javax.naming.NameAlreadyBoundException if the name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if <code>attrs</code> does not
     * contain all the mandatory attributes required for creation
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public LdapContext createSubcontext(Name name, Attributes attrs) throws NamingException
    {
        return createSubcontext(name.toString(), attrs);
    }

    /**
     * Searches in a single context for objects that contain a
     * specified set of attributes, and retrieves selected attributes.
     * See {@link #search(javax.naming.Name, javax.naming.directory.Attributes, String[])} for details.
     *
     * @param name               the name of the context to search
     * @param matchingAttributes the attributes to search for
     * @param attributesToReturn the attributes to return
     * @return	a non-null enumeration of <tt>SearchResult</tt> objects
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException
    {
        // create a search filter based on the matchingAttributes list.

        String filter;
        filter = getAttributeMatchFilter(matchingAttributes);

// TODO search context crap: alias handling, time limits etc.

        return doDsmlSearch(name, WHOLESUBTREE, SEARCHING, 0, 0, false, filter, attributesToReturn);
    }

    /**
     * Searches in a single context for objects that contain a
     * specified set of attributes, and retrieves selected attributes.
     * The search is performed using the default
     * <code>SearchControls</code> settings.
     * <p/>
     * For an object to be selected, each attribute in
     * <code>matchingAttributes</code> must match some attribute of the
     * object.  If <code>matchingAttributes</code> is empty or
     * null, all objects in the target context are returned.
     * <p/>
     * An attribute <em>A</em><sub>1</sub> in
     * <code>matchingAttributes</code> is considered to match an
     * attribute <em>A</em><sub>2</sub> of an object if
     * <em>A</em><sub>1</sub> and <em>A</em><sub>2</sub> have the same
     * identifier, and each value of <em>A</em><sub>1</sub> is equal
     * to some value of <em>A</em><sub>2</sub>.  This implies that the
     * order of values is not significant, and that
     * <em>A</em><sub>2</sub> may contain "extra" values not found in
     * <em>A</em><sub>1</sub> without affecting the comparison.  It
     * also implies that if <em>A</em><sub>1</sub> has no values, then
     * testing for a match is equivalent to testing for the presence
     * of an attribute <em>A</em><sub>2</sub> with the same
     * identifier.
     * <p/>
     * The precise definition of "equality" used in comparing attribute values
     * is defined by the underlying directory service.  It might use the
     * <code>Object.equals</code> method, for example, or might use a schema
     * to specify a different equality operation.
     * For matching based on operations other than equality (such as
     * substring comparison) use the version of the <code>search</code>
     * method that takes a filter argument.
     * <p/>
     * When changes are made to this <tt>LdapContext</tt>,
     * the effect on enumerations returned by prior calls to this method
     * is undefined.
     * <p/>
     * If the object does not have the attribute
     * specified, the directory will ignore the nonexistent attribute
     * and return the requested attributes that the object does have.
     * <p/>
     * A directory might return more attributes than was requested
     * (see <strong>Attribute Type Names</strong> in the class description),
     * but is not allowed to return arbitrary, unrelated attributes.
     * <p/>
     * See also <strong>Operational Attributes</strong> in the class
     * description.
     *
     * @param name               the name of the context to search
     * @param matchingAttributes the attributes to search for.  If empty or null,
     *                           all objects in the target context are returned.
     * @param attributesToReturn the attributes to return.  null indicates that
     *                           all attributes are to be returned;
     *                           an empty array indicates that none are to be returned.
     * @return a non-null enumeration of <tt>SearchResult</tt> objects.
     *         Each <tt>SearchResult</tt> contains the attributes
     *         identified by <code>attributesToReturn</code>
     *         and the name of the corresponding object, named relative
     *         to the context named by <code>name</code>.
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see javax.naming.directory.SearchControls
     * @see javax.naming.directory.SearchResult
     * @see #search(javax.naming.Name, String, Object[], javax.naming.directory.SearchControls)
     */
    public NamingEnumeration search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException
    {
        return search(name.toString(), matchingAttributes, attributesToReturn);
    }

    /**
     * This takes a list of Attributes and produces a 'present' match filter based on them.
     *
     * @param matchingAttributes a list of attributes required to be present (e.g. 'person', 'favoriteDrink')
     * @return the ldap filter for matching the attributes; e.g. (&(person=*)(favoriteDrink=*))
     */
    static String getAttributeMatchFilter(Attributes matchingAttributes)
    {
        String filter;
        int numbAtts = (matchingAttributes == null) ? 0 : matchingAttributes.size();
        if (numbAtts == 0)
        {
            filter = "(objectClass=*)";
        }
        else
        {
            StringBuffer filterBuffer = new StringBuffer();
            if (numbAtts > 1)
                filterBuffer.append("(&");

            Enumeration atts = matchingAttributes.getIDs();  // ignore NamingException, as these atts come from the client, not the server
            while (atts.hasMoreElements())
            {
                String att = (String) atts.nextElement();
                filterBuffer.append("(").append(att).append("=*)");
            }

            if (numbAtts > 1)
                filterBuffer.append(")");
            filter = filterBuffer.toString();
        }
        return filter;
    }


    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter.  Performs the search as specified by
     * the search controls.
     * <p/>
     * See {@link #search(javax.naming.Name, String, javax.naming.directory.SearchControls)} for details.
     *
     * @param name   the name of the context or object to search
     * @param filter the filter expression to use for the search; may not be null
     * @param cons   the search controls that control the search.  If null,
     *               the default search controls are used (equivalent
     *               to <tt>(new SearchControls())</tt>).
     * @return	an enumeration of <tt>SearchResult</tt>s for
     * the objects that satisfy the filter.
     * @throws	javax.naming.directory.InvalidSearchFilterException if the search filter specified is
     * not supported or understood by the underlying directory
     * @throws	javax.naming.directory.InvalidSearchControlsException if the search controls
     * contain invalid settings
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(String name, String filter, SearchControls cons) throws NamingException
    {
        return search(name, filter, null, cons);  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter.  Performs the search as specified by
     * the search controls.
     * <p/>
     * The format and interpretation of <code>filter</code> follows RFC 2254
     * with the
     * following interpretations for <code>attr</code> and <code>value</code>
     * mentioned in the RFC.
     * <p/>
     * <code>attr</code> is the attribute's identifier.
     * <p/>
     * <code>value</code> is the string representation the attribute's value.
     * The translation of this string representation into the attribute's value
     * is directory-specific.
     * <p/>
     * For the assertion "someCount=127", for example, <code>attr</code>
     * is "someCount" and <code>value</code> is "127".
     * The provider determines, based on the attribute ID ("someCount")
     * (and possibly its schema), that the attribute's value is an integer.
     * It then parses the string "127" appropriately.
     * <p/>
     * Any non-ASCII characters in the filter string should be
     * represented by the appropriate Java (Unicode) characters, and
     * not encoded as UTF-8 octets.  Alternately, the
     * "backslash-hexcode" notation described in RFC 2254 may be used.
     * <p/>
     * If the directory does not support a string representation of
     * some or all of its attributes, the form of <code>search</code> that
     * accepts filter arguments in the form of Objects can be used instead.
     * The service provider for such a directory would then translate
     * the filter arguments to its service-specific representation
     * for filter evaluation.
     * See <code>search(Name, String, Object[], SearchControls)</code>.
     * <p/>
     * RFC 2254 defines certain operators for the filter, including substring
     * matches, equality, approximate match, greater than, less than.  These
     * operators are mapped to operators with corresponding semantics in the
     * underlying directory. For example, for the equals operator, suppose
     * the directory has a matching rule defining "equality" of the
     * attributes in the filter. This rule would be used for checking
     * equality of the attributes specified in the filter with the attributes
     * of objects in the directory. Similarly, if the directory has a
     * matching rule for ordering, this rule would be used for
     * making "greater than" and "less than" comparisons.
     * <p/>
     * Not all of the operators defined in RFC 2254 are applicable to all
     * attributes.  When an operator is not applicable, the exception
     * <code>InvalidSearchFilterException</code> is thrown.
     * <p/>
     * The result is returned in an enumeration of <tt>SearchResult</tt>s.
     * Each <tt>SearchResult</tt> contains the name of the object
     * and other information about the object (see SearchResult).
     * The name is either relative to the target context of the search
     * (which is named by the <code>name</code> parameter), or
     * it is a URL string. If the target context is included in
     * the enumeration (as is possible when
     * <code>cons</code> specifies a search scope of
     * <code>SearchControls.OBJECT_SCOPE</code> or
     * <code>SearchControls.SUBSTREE_SCOPE</code>), its name is the empty
     * string. The <tt>SearchResult</tt> may also contain attributes of the
     * matching object if the <tt>cons</tt> argument specified that attributes
     * be returned.
     * <p/>
     * If the object does not have a requested attribute, that
     * nonexistent attribute will be ignored.  Those requested
     * attributes that the object does have will be returned.
     * <p/>
     * A directory might return more attributes than were requested
     * (see <strong>Attribute Type Names</strong> in the class description)
     * but is not allowed to return arbitrary, unrelated attributes.
     * <p/>
     * See also <strong>Operational Attributes</strong> in the class
     * description.
     * <p/>
     * <p>Equivalent to search(name, filter, null, cons).
     *
     * @param name   the name of the context or object to search
     * @param filter the filter expression to use for the search; may not be null
     * @param cons   the search controls that control the search.  If null,
     *               the default search controls are used (equivalent
     *               to <tt>(new SearchControls())</tt>).
     * @return	an enumeration of <tt>SearchResult</tt>s of
     * the objects that satisfy the filter; never null
     * @throws	javax.naming.directory.InvalidSearchFilterException if the search filter specified is
     * not supported or understood by the underlying directory
     * @throws	javax.naming.directory.InvalidSearchControlsException if the search controls
     * contain invalid settings
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #search(javax.naming.Name, String, Object[], javax.naming.directory.SearchControls)
     * @see javax.naming.directory.SearchControls
     * @see javax.naming.directory.SearchResult
     */
    public NamingEnumeration search(Name name, String filter, SearchControls cons) throws NamingException
    {
        return search(name, filter, null, cons);
    }

    /**
     * <p>Searches in the named context or object for entries that satisfy the
     * given search filter.  Performs the search as specified by
     * the search controls.
     * <p/>
     * See {@link #search(javax.naming.Name, String, Object[], javax.naming.directory.SearchControls)} for details.
     *
     * @param name       the name of the context or object to search
     * @param filterExpr the filter expression to use for the search.
     *                   The expression may contain variables of the
     *                   form "<code>{i}</code>" where <code>i</code>
     *                   is a nonnegative integer.  May not be null.
     * @param filterArgs <b>NOT IMPLEMENTED!</b>
     *                   the array of arguments to substitute for the variables
     *                   in <code>filterExpr</code>.  The value of
     *                   <code>filterArgs[i]</code> will replace each
     *                   occurrence of "<code>{i}</code>".
     *                   If null, equivalent to an empty array.
     * @param cons       the search controls that control the search.  If null,
     *                   the default search controls are used (equivalent
     *                   to <tt>(new SearchControls())</tt>).
     * @return	an enumeration of <tt>SearchResult</tt>s of the objects
     * that satisfy the filter; never null
     * @throws	ArrayIndexOutOfBoundsException if <tt>filterExpr</tt> contains
     * <code>{i}</code> expressions where <code>i</code> is outside
     * the bounds of the array <code>filterArgs</code>
     * @throws	javax.naming.directory.InvalidSearchControlsException if <tt>cons</tt> contains
     * invalid settings
     * @throws	javax.naming.directory.InvalidSearchFilterException if <tt>filterExpr</tt> with
     * <tt>filterArgs</tt> represents an invalid search filter
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException
    {

        if (filterExpr == null)
            throw new NamingException("null ldap filter in DsmlContext search");

        if (filterArgs != null && filterArgs.length > 0)
            throw new NamingException("filter arguments not implemented in com.ca.jndiproviders.dsml provider");

        log.finest("search III (" + name.toString() + ") + filter: " + filterExpr);

        if (cons == null)
            cons = DEFAULT_SEARCH_CONTROLS;

// translate JNDI search scope into DSML speak
        String searchScope;
        switch (cons.getSearchScope())
        {
            case SearchControls.OBJECT_SCOPE:
                searchScope = BASEOBJECT;
                break;
            case SearchControls.ONELEVEL_SCOPE:
                searchScope = SINGLELEVEL;
                break;
            case SearchControls.SUBTREE_SCOPE:
                searchScope = WHOLESUBTREE;
                break;
            default:
                throw new NamingException("unexpected error; unknown search scope in SearchControl object: " + cons.getSearchScope()); // really, really, really should be impossible
        }


// translate JNDI alias handling into DSML speak
        String jndiAliasHandling = (String) environment.get("java.naming.ldap.derefAliases");

        if (jndiAliasHandling == null)
            jndiAliasHandling = "always";

        String aliasHandling = "derefAlways";

        if (jndiAliasHandling.equals("always"))
            aliasHandling = ALWAYS;
        else if (jndiAliasHandling.equals("never"))
            aliasHandling = NEVER;
        else if (jndiAliasHandling.equals("finding"))
            aliasHandling = FINDING;
        else if (jndiAliasHandling.equals("searching"))
            aliasHandling = SEARCHING;

// doesn't seem to be anything corresponding to the DSML 'types only' flag, unless possibly it is cons.getReturningObjFlag()??

// do the DSML search using the jndiproviders search controls data, environment data, and passed parameters
        NamingEnumeration returnEnum = doDsmlSearch(name, searchScope, aliasHandling, cons.getCountLimit(), cons.getTimeLimit(), false, filterExpr, cons.getReturningAttributes());

        return returnEnum;
    }

    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter.  Performs the search as specified by
     * the search controls.
     * <p/>
     * The interpretation of <code>filterExpr</code> is based on RFC
     * 2254.  It may additionally contain variables of the form
     * <code>{i}</code> -- where <code>i</code> is an integer -- that
     * refer to objects in the <code>filterArgs</code> array.  The
     * interpretation of <code>filterExpr</code> is otherwise
     * identical to that of the <code>filter</code> parameter of the
     * method <code>search(Name, String, SearchControls)</code>.
     * <p/>
     * When a variable <code>{i}</code> appears in a search filter, it
     * indicates that the filter argument <code>filterArgs[i]</code>
     * is to be used in that place.  Such variables may be used
     * wherever an <em>attr</em>, <em>value</em>, or
     * <em>matchingrule</em> production appears in the filter grammar
     * of RFC 2254, section 4.  When a string-valued filter argument
     * is substituted for a variable, the filter is interpreted as if
     * the string were given in place of the variable, with any
     * characters having special significance within filters (such as
     * <code>'*'</code>) having been escaped according to the rules of
     * RFC 2254.
     * <p/>
     * For directories that do not use a string representation for
     * some or all of their attributes, the filter argument
     * corresponding to an attribute value may be of a type other than
     * String.  Directories that support unstructured binary-valued
     * attributes, for example, should accept byte arrays as filter
     * arguments.  The interpretation (if any) of filter arguments of
     * any other type is determined by the service provider for that
     * directory, which maps the filter operations onto operations with
     * corresponding semantics in the underlying directory.
     * <p/>
     * This method returns an enumeration of the results.
     * Each element in the enumeration contains the name of the object
     * and other information about the object (see <code>SearchResult</code>).
     * The name is either relative to the target context of the search
     * (which is named by the <code>name</code> parameter), or
     * it is a URL string. If the target context is included in
     * the enumeration (as is possible when
     * <code>cons</code> specifies a search scope of
     * <code>SearchControls.OBJECT_SCOPE</code> or
     * <code>SearchControls.SUBSTREE_SCOPE</code>),
     * its name is the empty string.
     * <p/>
     * The <tt>SearchResult</tt> may also contain attributes of the matching
     * object if the <tt>cons</tt> argument specifies that attributes be
     * returned.
     * <p/>
     * If the object does not have a requested attribute, that
     * nonexistent attribute will be ignored.  Those requested
     * attributes that the object does have will be returned.
     * <p/>
     * A directory might return more attributes than were requested
     * (see <strong>Attribute Type Names</strong> in the class description)
     * but is not allowed to return arbitrary, unrelated attributes.
     * <p/>
     * If a search filter with invalid variable substitutions is provided
     * to this method, the result is undefined.
     * When changes are made to this LdapContext,
     * the effect on enumerations returned by prior calls to this method
     * is undefined.
     * <p/>
     * See also <strong>Operational Attributes</strong> in the class
     * description.
     *
     * @param name       the name of the context or object to search
     * @param filterExpr the filter expression to use for the search.
     *                   The expression may contain variables of the
     *                   form "<code>{i}</code>" where <code>i</code>
     *                   is a nonnegative integer.  May not be null.
     * @param filterArgs the array of arguments to substitute for the variables
     *                   in <code>filterExpr</code>.  The value of
     *                   <code>filterArgs[i]</code> will replace each
     *                   occurrence of "<code>{i}</code>".
     *                   If null, equivalent to an empty array.
     * @param cons       the search controls that control the search.  If null,
     *                   the default search controls are used (equivalent
     *                   to <tt>(new SearchControls())</tt>).
     * @return	an enumeration of <tt>SearchResult</tt>s of the objects
     * that satisfy the filter; never null
     * @throws	ArrayIndexOutOfBoundsException if <tt>filterExpr</tt> contains
     * <code>{i}</code> expressions where <code>i</code> is outside
     * the bounds of the array <code>filterArgs</code>
     * @throws	javax.naming.directory.InvalidSearchControlsException if <tt>cons</tt> contains
     * invalid settings
     * @throws	javax.naming.directory.InvalidSearchFilterException if <tt>filterExpr</tt> with
     * <tt>filterArgs</tt> represents an invalid search filter
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #search(javax.naming.Name, javax.naming.directory.Attributes, String[])
     * @see java.text.MessageFormat
     */
    public NamingEnumeration search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException
    {
        return search(name.toString(), filterExpr, filterArgs, cons);

    }

    /**
     * Closes this context.
     * This method releases this context's resources immediately, instead of
     * waiting for them to be released automatically by the garbage collector.
     * <p/>
     * <p> This method is idempotent:  invoking it on a context that has
     * already been closed has no effect.  Invoking any other method
     * on a closed context is not allowed, and results in undefined behaviour.
     *
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public void close() throws NamingException
    {
        log.finest("close()");
// TODO - any cleanup required here?
//To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Retrieves the full name of this context within its own namespace.
     * <p/>
     * <p> Many naming services have a notion of a "full name" for objects
     * in their respective namespaces.  For example, an LDAP entry has
     * a distinguished name, and a DNS record has a fully qualified name.
     * This method allows the client application to retrieve this name.
     * The string returned by this method is not a JNDI composite name
     * and should not be passed directly to context methods.
     * In naming systems for which the notion of full name does not
     * make sense, <tt>OperationNotSupportedException</tt> is thrown.
     *
     * @return	this context's name in its own namespace; never null
     * @throws	javax.naming.OperationNotSupportedException if the naming system does
     * not have the notion of a full name
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @since 1.3
     */
    public String getNameInNamespace() throws NamingException
    {
        log.finest("getNameInNamespace()");
        return contextName;
    }

    /**
     * Destroys the named context and removes it from the namespace.
     * See {@link #destroySubcontext(javax.naming.Name)} for details.
     *
     * @param name the name of the context to be destroyed; may not be empty
     * @throws	javax.naming.NameNotFoundException if an intermediate context does not exist
     * @throws	javax.naming.NotContextException if the name is bound but does not name a
     * context, or does not name a context of the appropriate type
     * @throws	javax.naming.ContextNotEmptyException if the named context is not empty
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */


    public void destroySubcontext(String name) throws NamingException
    {
        log.finest("destroySubcontext (" + name.toString() + ")");
// construct XML

        StringBuffer deleteRequestBuffer = constructDeleteRequest(name);

// send XML to server

        String response = sendDSMLRequest(deleteRequestBuffer);

// parse response XML

        parseDeleteResponse(response);
    }

    void parseDeleteResponse(String response)
            throws NamingException
    {
        checkForError(response);
// quick check that it really was a modify response...
        if (response.indexOf("delResponse>") == -1)
            throw new NamingException("Unexpected DSML Response to Delete Request:\n " + response);

    }

    /**
     * Unbinds the named object.
     * See {@link #unbind(javax.naming.Name)} for details.
     *
     * @param name the name to unbind; may not be empty
     * @throws	javax.naming.NameNotFoundException if an intermediate context does not exist
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated bind operations not supported
     */
    public void unbind(String name) throws NamingException
    {
        throw new OperationNotSupportedException("DsmlContext does not support storing java objects");

//To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Retrieves the environment in effect for this context.
     * See class description for more details on environment properties.
     * <p/>
     * <p> The caller should not make any changes to the object returned:
     * their effect on the context is undefined.
     * The environment of this context may be changed using
     * <tt>addToEnvironment()</tt> and <tt>removeFromEnvironment()</tt>.
     *
     * @return	the environment of this context; never null
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #addToEnvironment(String, Object)
     * @see #removeFromEnvironment(String)
     */
    public Hashtable getEnvironment() throws NamingException
    {
        return (Hashtable) environment.clone();  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Destroys the named context and removes it from the namespace.
     * Any attributes associated with the name are also removed.
     * Intermediate contexts are not destroyed.
     * <p/>
     * <p> This method is idempotent.
     * It succeeds even if the terminal atomic name
     * is not bound in the target context, but throws
     * <tt>NameNotFoundException</tt>
     * if any of the intermediate contexts do not exist.
     * <p/>
     * <p> In a federated naming system, a context from one naming system
     * may be bound to a name in another.  One can subsequently
     * look up and perform operations on the foreign context using a
     * composite name.  However, an attempt destroy the context using
     * this composite name will fail with
     * <tt>NotContextException</tt>, because the foreign context is not
     * a "subcontext" of the context in which it is bound.
     * Instead, use <tt>unbind()</tt> to remove the
     * binding of the foreign context.  Destroying the foreign context
     * requires that the <tt>destroySubcontext()</tt> be performed
     * on a context from the foreign context's "native" naming system.
     *
     * @param name the name of the context to be destroyed; may not be empty
     * @throws	javax.naming.NameNotFoundException if an intermediate context does not exist
     * @throws	javax.naming.NotContextException if the name is bound but does not name a
     * context, or does not name a context of the appropriate type
     * @throws	javax.naming.ContextNotEmptyException if the named context is not empty
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #destroySubcontext(String)
     */
    public void destroySubcontext(Name name) throws NamingException
    {
        destroySubcontext(name.toString());
//To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Unbinds the named object.
     * Removes the terminal atomic name in <code>name</code>
     * from the target context--that named by all but the terminal
     * atomic part of <code>name</code>.
     * <p/>
     * <p> This method is idempotent.
     * It succeeds even if the terminal atomic name
     * is not bound in the target context, but throws
     * <tt>NameNotFoundException</tt>
     * if any of the intermediate contexts do not exist.
     * <p/>
     * <p> Any attributes associated with the name are removed.
     * Intermediate contexts are not changed.
     *
     * @param name the name to unbind; may not be empty
     * @throws	javax.naming.NameNotFoundException if an intermediate context does not exist
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #unbind(String)
     * @deprecated bind/unbind operations not supported by DsmlContext
     */
    public void unbind(Name name) throws NamingException
    {
        throw new OperationNotSupportedException("DsmlContext does not support storing java objects");
    }

    /**
     * Retrieves the named object.
     * See {@link #lookup(javax.naming.Name)} for details.
     *
     * @param name the name of the object to look up
     * @return	the object bound to <tt>name</tt>
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated java Object specific methods such as bind/unbind/lookup are not supported by DsmlContext
     */
    public Object lookup(String name) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Retrieves the named object, following links except
     * for the terminal atomic component of the name.
     * See {@link #lookupLink(javax.naming.Name)} for details.
     *
     * @param name the name of the object to look up
     * @return	the object bound to <tt>name</tt>, not following the
     * terminal link (if any)
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated java Object specific methods such as bind/unbind/lookup are not supported by DsmlContext
     */
    public Object lookupLink(String name) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Removes an environment property from the environment of this
     * context.  See class description for more details on environment
     * properties.
     *
     * @param propName the name of the environment property to remove; may not be null
     * @return	the previous value of the property, or null if the property was
     * not in the environment
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #getEnvironment()
     * @see #addToEnvironment(String, Object)
     */
    public Object removeFromEnvironment(String propName) throws NamingException
    {
        return environment.remove(propName);
    }

    /**
     * Binds a name to an object.
     * See {@link #bind(javax.naming.Name, Object)} for details.
     *
     * @param name the name to bind; may not be empty
     * @param obj  the object to bind; possibly null
     * @throws	javax.naming.NameAlreadyBoundException if name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if object did not supply all mandatory attributes
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated java Object specific methods such as bind/unbind/lookup are not supported by DsmlContext
     */
    public void bind(String name, Object obj) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Binds a name to an object, overwriting any existing binding.
     * See {@link #rebind(javax.naming.Name, Object)} for details.
     *
     * @param name the name to bind; may not be empty
     * @param obj  the object to bind; possibly null
     * @throws	javax.naming.directory.InvalidAttributesException if object did not supply all mandatory attributes
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated java Object specific methods such as bind/unbind/lookup are not supported by DsmlContext
     */
    public void rebind(String name, Object obj) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Retrieves the named object.
     * If <tt>name</tt> is empty, returns a new instance of this context
     * (which represents the same naming context as this context, but its
     * environment may be modified independently and it may be accessed
     * concurrently).
     *
     * @param name the name of the object to look up
     * @return	the object bound to <tt>name</tt>
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #lookup(String)
     * @see #lookupLink(javax.naming.Name)
     * @deprecated java Object specific methods such as bind/unbind/lookup are not supported by DsmlContext
     */
    public Object lookup(Name name) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Retrieves the named object, following links except
     * for the terminal atomic component of the name.
     * If the object bound to <tt>name</tt> is not a link,
     * returns the object itself.
     *
     * @param name the name of the object to look up
     * @return	the object bound to <tt>name</tt>, not following the
     * terminal link (if any).
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #lookupLink(String)
     * @deprecated java Object specific methods such as bind/unbind/lookup are not supported by DsmlContext
     */
    public Object lookupLink(Name name) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Binds a name to an object.
     * All intermediate contexts and the target context (that named by all
     * but terminal atomic component of the name) must already exist.
     *
     * @param name the name to bind; may not be empty
     * @param obj  the object to bind; possibly null
     * @throws	javax.naming.NameAlreadyBoundException if name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if object did not supply all mandatory attributes
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #bind(String, Object)
     * @see #rebind(javax.naming.Name, Object)
     * @deprecated java Object specific methods such as bind/unbind/lookup are not supported by DsmlContext
     */
    public void bind(Name name, Object obj) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Binds a name to an object, overwriting any existing binding.
     * All intermediate contexts and the target context (that named by all
     * but terminal atomic component of the name) must already exist.
     * <p/>
     * <p> If the object is a <tt>LdapContext</tt>, any existing attributes
     * associated with the name are replaced with those of the object.
     * Otherwise, any existing attributes associated with the name remain
     * unchanged.
     *
     * @param name the name to bind; may not be empty
     * @param obj  the object to bind; possibly null
     * @throws	javax.naming.directory.InvalidAttributesException if object did not supply all mandatory attributes
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #rebind(String, Object)
     * @see #bind(javax.naming.Name, Object)
     * @deprecated java Object specific methods such as bind/unbind/lookup are not supported by DsmlContext
     */
    public void rebind(Name name, Object obj) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Binds a new name to the object bound to an old name, and unbinds
     * the old name.
     * See {@link #rename(javax.naming.Name, javax.naming.Name)} for details.
     *
     * @param oldName the name of the existing binding; may not be empty
     * @param newName the name of the new binding; may not be empty
     * @throws	javax.naming.NameAlreadyBoundException if <tt>newName</tt> is already bound
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public void rename(String oldName, String newName) throws NamingException
    {
        // note - this is the only time we use the 'name' version of the call,
        // because we have to do some processing rather than writing the string
        // out to DSML immediately.
        rename(new DN(oldName), new DN(newName));
    }

    StringBuffer constructModDNRequest(String oldName, String newName)
            throws NamingException
    {
        StringBuffer message = new StringBuffer(200);
        message.append(STANDARDHEADER);

        getModDNRequestElement(message, oldName, newName);

        message.append(STANDARDFOOTER);

        return message;
    }

    /**
     * Renames an entry.  Note that 'new Superior' is not supported; entries must
     * be copied and deleted instead.
     *
     * @param message
     * @param oldName the full DN of the old entry name
     * @param newRDN  the rdn of the new entry name
     */
    /*
    "                   <modDNRequest dn="CN=Alice Johnson,DC=Example,DC=COM"
                         newrdn="CN=Alice Weiss"
                         deleteoldrdn="true"
                         newSuperior="OU=Marketing,DC=Example,DC=COM"/>

    */
    void getModDNRequestElement(StringBuffer message, String oldName, String newRDN)
    {
        message.append(TAB4).append("<dsml:modDNRequest dn=\"").append(escapeName(oldName)).append("\" newrdn=\"").append(escapeName(newRDN)).append("\" ");

        if (environment.get("java.naming.ldap.deleteRDN").toString().equalsIgnoreCase("false"))
            message.append("deleteoldrdn=\"false\"/>");
        else
            message.append("deleteoldrdn=\"true\"/>");

    }

    void parseModDNResponse(String response)
            throws NamingException
    {
        // check for a dsml error
        checkForError(response);

        // quick check that it really was a modify response...
        if (response.indexOf("modDNResponse>") == -1)
            throw new NamingException("Unexpected DSML Response to Modify DN Request:\n " + response);
    }

    /**
     * Creates and binds a new context.
     * See {@link #createSubcontext(javax.naming.Name)} for details.
     *
     * @param name the name of the context to create; may not be empty
     * @return	the newly created context
     * @throws	javax.naming.NameAlreadyBoundException if name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if creation of the subcontext requires specification of
     * mandatory attributes
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated it is impossible to create a directory object without attributes  (RFC 2251 - sec 4.7)
     */
    public Context createSubcontext(String name) throws NamingException
    {
        throw new InvalidAttributesException("cannot create directory object without attributes");
    }

    /**
     * Creates and binds a new context.
     * Creates a new context with the given name and binds it in
     * the target context (that named by all but terminal atomic
     * component of the name).  All intermediate contexts and the
     * target context must already exist.
     *
     * @param name the name of the context to create; may not be empty
     * @return	the newly created context
     * @throws	javax.naming.NameAlreadyBoundException if name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if creation of the subcontext requires specification of
     * mandatory attributes
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #createSubcontext(String)
     * @deprecated it is impossible to create a directory object without attributes  (RFC 2251 - sec 4.7)
     */
    public Context createSubcontext(Name name) throws NamingException
    {
        throw new InvalidAttributesException("cannot create directory object without attributes");
    }

    /**
     * Binds a new name to the object bound to an old name, and unbinds
     * the old name.  Both names are relative to this context.
     * Any attributes associated with the old name become associated
     * with the new name.
     * Intermediate contexts of the old name are not changed.
     *
     * @param oldName the name of the existing binding; may not be empty
     * @param rdn the RDN of the new binding; may not be empty
     * @throws	javax.naming.NameAlreadyBoundException if <tt>newName</tt> is already bound
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #rename(String, String)
     * @see #bind(javax.naming.Name, Object)
     * @see #rebind(javax.naming.Name, Object)
     */
    public void rename(Name oldName, Name rdn) throws NamingException
    {
        log.finest("rename (" + oldName + " to " + rdn);

        String oldNameString = oldName.toString();

        if (rdn.size() != 1)
            throw new NamingException("cannot perform rename operation '" + rdn + "' is not an RDN \n" );

        String newRDN = rdn.toString();


        /*
        String newNameString = newName.toString();

        if (!oldNameString.endsWith(contextName))
            oldNameString = oldNameString + "," + contextName;

        if (!newNameString.endsWith(contextName))
        {
            newNameString = newNameString + "," + contextName;
            newName = new DN(newNameString);
        }

        if (newName.size() != 1 && newName.size() != oldName.size())
            throw new NamingException("cannot perform rename operation - DNs are at different levels:\n" + oldNameString + "\n" + newNameString);

        int nameSize = newName.size();

        if (!oldName.getPrefix(nameSize - 2).equals(newName.getPrefix(nameSize - 2)))
            throw new NamingException("cannot perform rename operation - RDNs have different parents:\n" + oldNameString + "\n" + newNameString);

        String newRDN = newName.get(newName.size() - 1).toString();

        */

        // construct XML

        StringBuffer modDNRequestBuffer = constructModDNRequest(oldNameString, newRDN);

        // send XML to server

        String response = sendDSMLRequest(modDNRequestBuffer);

        // parse response XML

        parseModDNResponse(response);


    }

    /**
     * Retrieves the parser associated with the named context.  Note that the
     * DsmlContext does not support composite names, and so DsmlParser.parser
     * is returned regardless of what name is passed.
     * See {@link #getNameParser(javax.naming.Name)} for details.
     *
     * @param name the name of the context from which to get the parser
     * @return	a name parser that can parse compound names into their atomic
     * components
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public NameParser getNameParser(String name) throws NamingException
    {
        return DsmlParser.parser;
    }

    /**
     * Retrieves the parser associated with the named context.  Note that the
     * DsmlContext does not support composite names, and so DsmlParser.parser
     * is returned regardless of what name is passed.
     *
     * @param name the name of the context from which to get the parser (not used)
     * @return	a name parser that can parse compound DSML/LDAP names into their atomic
     * components
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #getNameParser(String)
     * @see javax.naming.CompoundName
     */
    public NameParser getNameParser(Name name) throws NamingException
    {
        return DsmlParser.parser;
    }

    /**
     * Enumerates the names bound in the named context.
     * See {@link #list(javax.naming.Name)} for details.
     *
     * @param name the name of the context to list
     * @return	an enumeration of the names and class names of the
     * bindings in this context.  Each element of the
     * enumeration is of type <tt>NameClassPair</tt>.
     * @throws	javax.naming.NamingException if a naming exception is encountered
     */
    public NamingEnumeration list(String name) throws NamingException
    {
        log.finest("list (" + name.toString() + ")");

        return doDsmlSearch(name, SINGLELEVEL, SEARCHING, 0, 0, false, "(objectClass=*)", null);
    }

    /**
     * Enumerates the names bound in the named context, along with the
     * objects bound to them.
     * See {@link #listBindings(javax.naming.Name)} for details.
     *
     * @param name the name of the context to list
     * @return	an enumeration of the bindings in this context.
     * Each element of the enumeration is of type
     * <tt>Binding</tt>.
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated java Object specific methods such as bind/unbind/lookup are not supported by DsmlContext
     */
    public NamingEnumeration listBindings(String name) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Enumerates the names bound in the named context, along with the
     * class names of objects bound to them.
     * The contents of any subcontexts are not included.
     * <p/>
     * <p> If a binding is added to or removed from this context,
     * its effect on an enumeration previously returned is undefined.
     *
     * @param name the name of the context to list
     * @return	an enumeration of the names and class names of the
     * bindings in this context.  Each element of the
     * enumeration is of type <tt>NameClassPair</tt>.
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #list(String)
     * @see #listBindings(javax.naming.Name)
     * @see javax.naming.NameClassPair
     */
    public NamingEnumeration list(Name name) throws NamingException
    {
        return list(name.toString());

    }

    /**
     * Enumerates the names bound in the named context, along with the
     * objects bound to them.
     * The contents of any subcontexts are not included.
     * <p/>
     * <p> If a binding is added to or removed from this context,
     * its effect on an enumeration previously returned is undefined.
     *
     * @param name the name of the context to list
     * @return	an enumeration of the bindings in this context.
     * Each element of the enumeration is of type
     * <tt>Binding</tt>.
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #listBindings(String)
     * @see #list(javax.naming.Name)
     * @see javax.naming.Binding
     * @deprecated java Object specific methods such as bind/unbind/lookup are not supported by DsmlContext
     */
    public NamingEnumeration listBindings(Name name) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects is not supported by the DsmlContext");
    }

    /**
     * Adds a new environment property to the environment of this
     * context.  If the property already exists, its value is overwritten.
     * See class description for more details on environment properties.
     *
     * @param propName the name of the environment property to add; may not be null
     * @param propVal  the value of the property to add; may not be null
     * @return	the previous value of the property, or null if the property was
     * not in the environment before
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #getEnvironment()
     * @see #removeFromEnvironment(String)
     */
    public Object addToEnvironment(String propName, Object propVal) throws NamingException
    {
        log.finest("addToEnvironment (" + propName + ": " + propVal.toString());

        if (propName == null)
            return null;

        if (propVal == null)
            return environment.remove(propName);
        else
            return environment.put(propName, propVal);
    }

    /**
     * Not implemented - use DN methods instead if you want to create new Names.
     * DsmlContext does not support composite names (stupid idea anyway).
     *
     * @param name   a name relative to this context
     * @param prefix the name of this context relative to one of its ancestors
     * @return	the composition of <code>prefix</code> and <code>name</code>
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated
     */
    public String composeName(String name, String prefix) throws NamingException
    {
        throw new OperationNotSupportedException("DsmlContext does not support composing names - use DN methods directly");
    }

    /**
     * Not implemented - use DN methods instead if you want to create new Names.
     * DsmlContext does not support composite names (stupid idea anyway).
     *
     * @param name   a name relative to this context
     * @param prefix the name of this context relative to one of its ancestors
     * @return	the composition of <code>prefix</code> and <code>name</code>
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #composeName(String, String)
     * @deprecated
     */
    public Name composeName(Name name, Name prefix) throws NamingException
    {
        throw new OperationNotSupportedException("DsmlContext does not support composing names - use DN methods directly");
    }


/*
 *                              ---------- DSML Specific Code ------------
 */

    /**
     * This is the central coordinating search method.  It takes all the raw paramaters, and hands them off
     * to various sub methods do construct the search request, send it to the DSML server and get the result.
     *
     * @param name
     * @param scope
     * @param derefAliases
     * @param sizeLimit
     * @param timeLimit
     * @param typesOnly
     * @param filter
     * @param attributesToReturn
     * 
     * @return the search results
     * 
     * @throws NamingException
     */
    public NamingEnumeration doDsmlSearch(String name, String scope, String derefAliases, long sizeLimit, int timeLimit, boolean typesOnly, String filter, String[] attributesToReturn)
            throws NamingException
    {
        // construct XML

        StringBuffer searchRequestBuffer = constructSearchRequest(name, scope, derefAliases, sizeLimit, timeLimit, typesOnly, filter, attributesToReturn);

// send XML to server

        String response = sendDSMLRequest(searchRequestBuffer);

// parse response XML

        return parseSearchResponse(response, name);

    }

// static patterns for parsing search results.
//    private static Pattern searchResultEntry = Pattern.compile("<searchResultEntry (.*)</searchResultEntry>");

// nb.  Extreme caution required here to match DNs that may contain '"' and '>' characters.  Note that no legal dn
//      can contain the sequence '">', since the " and > both need escaping.  There *might* be a requestID in there
//      as well, but I've never seen this in the wild (what would the point be?)

// need to cope with option namespace prefixes...
//    private static Pattern searchResultEntry = Pattern.compile("<searchResultEntry.*?dn=\"(.*?)\">(.*?)</searchResultEntry>", Pattern.DOTALL);
//    private static Pattern searchResultAttribute = Pattern.compile("<attr name=\"(.*?)\">(.*?)</attr>", Pattern.DOTALL);
//    private static Pattern searchResultAttributeValues = Pattern.compile("<value(.*?)>(.*?)</value>", Pattern.DOTALL);

    private static Pattern searchResultEntry = Pattern.compile("searchResultEntry.*?dn=\"(.*?)\">(.*?)searchResultEntry>", Pattern.DOTALL);

    private static Pattern searchResultAttribute = Pattern.compile("attr name=\"(.*?)\">(.*?)attr>", Pattern.DOTALL);

    private static Pattern searchResultAttributeValues = Pattern.compile("value(.*?)>(.*?)<.*?value>", Pattern.DOTALL);

    private static Pattern searchTestType = Pattern.compile("type=\"(.*?)\"", Pattern.DOTALL);

    /**
     * This is one of the central methods of the provider.  It takes the raw XML returned and extracts the
     * DNs and attributes of the returned entries into a standard namingenumeration of SearchResult objects.
     *
     * @param response the DSML XML response
     * @param searchBase supply the search base so that this method can check if the DNs returned
     * in the search result are relative or not.  By default a search result's DN is set to relative.
     * If the DN returned in the search result includes the search base then it is not relative and
     * we need to set that so users of this provider don't re-add the search base after performing a
     * isRelative call on the SearchResult.
     * @return a naming enumeration of 'SearchResult' objects.
     * @throws NamingException if any error is found (including a returned error from the DSML server).
     */
    static NamingEnumeration parseSearchResponse(String response, String searchBase)
            throws NamingException
    {
        checkForError(response);

        DXNamingEnumeration en = new DXNamingEnumeration();

        Matcher responseMatcher = searchResultEntry.matcher(response);

// cycle through the available entries
        while (responseMatcher.find())
        {
            String dn = responseMatcher.group(1);

            BasicAttributes atts = new BasicAttributes();

            if (dn.indexOf("requestID") > -1) // what numb nut would put a requestID in a searchResultEntry??
            {
                int reqPos = dn.indexOf("requestID");
                int endPos = dn.lastIndexOf('"', reqPos);
                dn = dn.substring(0, endPos);
            }

            if (dn.indexOf("&") > -1)
                dn = unescape(dn);

            log.finest("Parsing DSML: read DN: " + dn);

            // turn pure pristine LDAP DN into un unbelievably screwed up JNDI Composite name, and then
            // store it as a string SO NO ONE CAN EVER REALLY TELL WHAT THE HELL IT IS.  God JNDI is
            // broken - what a (*&*^ joke.  Object Oriented?  Yeah, we've heard of that...

            dn = new CompositeName(dn).toString();

            log.finest("converted DN to: " + dn);

            String attribute = responseMatcher.group(2);

            Matcher attributeMatcher = searchResultAttribute.matcher(attribute);

            // cycle through individual attributes
            while (attributeMatcher.find())
            {
                String attributeName = attributeMatcher.group(1);

                BasicAttribute att = new BasicAttribute(attributeName);


                String attributeValues = attributeMatcher.group(2);

                log.finest("Parsing DSML: Attribute Name " + attributeName + " length: " + attributeValues.length());

                Matcher valueMatcher = searchResultAttributeValues.matcher(attributeValues);

                while (valueMatcher.find())
                {
                    String typeInfo = valueMatcher.group(1);

                    String type = "string"; // the default value is string

                    if (typeInfo != null && typeInfo.length() > 6)
                    {
// don't bother using regexp - faster to use string handling (if insanely clever might modify attributeValues regexp??)
                        int typeInfoStart = typeInfo.indexOf("type=\"");
                        int typeInfoEnd = typeInfo.indexOf('\"', typeInfoStart + 6);
                        if (typeInfoStart != -1 && typeInfoEnd > typeInfoStart)
                        {
                            type = typeInfo.substring(typeInfoStart + 6, typeInfoEnd);  // extract the type; e.g. xsd:base64Binary
                            if ((typeInfoStart = type.indexOf(':')) > -1)              // trim any namespace from the type (e.g. 'xsd:')
                                type = type.substring(typeInfoStart + 1);
                        }
                    }
                    String value = valueMatcher.group(2);

                    if (type.equals("string"))  // this is the usual case, and the default
                    {
                        if (value != null && value.indexOf('&') > -1)
                            value = unescape(value);
                        att.add(value);
                    }
                    else if (type.equals("anyURI"))
                        throw new NamingException("CA JNDI DSML Provider does not support 'anyURI' values");
                    else if (type.equals("base64Binary"))
                    {
                        try
                        {

                            System.out.println("PROCESSING BINARY VALUE " + attributeName);
                            byte[] data = CBBase64.decode(value);
                            System.out.println("RAW DATA: " + value.length() + " byte data " + data.length);
                            att.add(CBBase64.decode(value));
                        }
                        catch (CBBase64EncodingException e)
                        {
                            NamingException ne = new NamingException("unable to parse base64 value in entry: " + dn);
                            ne.setRootCause(e);
                            throw ne;
                        }
                    }
                }
                atts.put(att);
            }

            SearchResult currentResult = new SearchResult(dn, null, atts);

            // The search result sets 'is relative' to true by default.  Check if the
            // DN in the search result contains the search base, if so set 'is relative'
            // to false so that the search base is NOT added to the DN again.  In otherwords
            // check if the full DN has been returned in the search result...
            if(dn.endsWith(searchBase))
                currentResult.setRelative(false);

            en.add(currentResult);
        }

        log.finest("Parsing DSML: final enumeration - \n" + en.toString());

        return en;
    }

    /**
     * This converts the string buffer to raw bytes, and passes them to the SoapClient 'sendSoapMsg' method,
     * returning the response.
     *
     * @param searchRequestBuffer
     * 
     * @return the response from the request
     * 
     * @throws NamingException
     */
    private String sendDSMLRequest(StringBuffer searchRequestBuffer)
            throws NamingException
    {
        String response;

        try
        {
// note for performance junkies - assigning an unshared string buffer to a string is a cheap operation that shares the internal char array.
            String searchRequest = new String(searchRequestBuffer);

            byte[] rawBytes = searchRequest.getBytes("UTF-8");

            String URL = environment.get(PROVIDER_URL).toString();

            log.finest("----SENDING XML OVER WIRE-----\nURL: " + URL + "\nlength: " + rawBytes.length);

            String usr = null,pwd = null;

            if (environment.get(Context.SECURITY_AUTHENTICATION) == "simple")
            {
                usr = (String) environment.get(Context.SECURITY_PRINCIPAL);       
                pwd = (String) environment.get(Context.SECURITY_CREDENTIALS);
            }

            response = SoapClient.sendSoapMsg(URL, rawBytes, "#batchRequest", usr, pwd);

        }
        catch (UnsupportedEncodingException e) // should never happen.  really.
        {
            NamingException ne = new NamingException("unexpected exception encoding UTF-8 string for DSML message");
            ne.setRootCause(e);
            throw ne;
        }
        catch (IOException e)
        {
            NamingException ne = new NamingException("error contacting DSML Server");
            ne.setRootCause(e);
            throw ne;
        }
        return response;
    }

    /**
     * Constructs an add request.
     * 
     * @param name the name
     * @param atts the attributes
     * 
     * @return the add request
     */
    static StringBuffer constructAddRequest(String name, Attributes atts)
            throws NamingException
    {
        StringBuffer message = new StringBuffer(200);
        message.append(STANDARDHEADER);

        getAddRequestElement(message, name, atts);

        message.append(STANDARDFOOTER);

        return message;
    }

    /**
     * returns the actual DSML 'delRequest' element.<br>
     * e.g.:&lt;br&gt:
     * &lt;dsml:addRequest requestID="1" dn="cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU"&gt:
     * &lt;dsml:attr name="cn"&gt:
     * &lt;dsml:value&gt:Alana SHORE&lt;/dsml:value&gt:
     * &lt;/dsml:attr&gt:
     * &lt;dsml:attr name="objectClass"&gt:
     * &lt;dsml:value&gt:inetOrgPerson&lt;/dsml:value&gt:
     * &lt;dsml:value&gt:organizationalPerson&lt;/dsml:value&gt:
     * &lt;dsml:value&gt:person&lt;/dsml:value&gt:
     * &lt;dsml:value&gt:top&lt;/dsml:value&gt:
     * &lt;/dsml:attr&gt:
     * &lt;dsml:attr name="sn"&gt:
     * &lt;dsml:value&gt:SHORE&lt;/dsml:value&gt:
     * &lt;/dsml:attr&gt:
     * &lt;/dsml:addRequest&gt:
     *
     * @param message
     * @param name
     */

/*  "<dsml:batchRequest xmlns:dsml=\"urn:oasis:names:tc:DSML:2:0:core\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
    "   <dsml:addRequest dn=\"cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU\">" +
    "      <dsml:attr name=\"cn\">" +
    "         <dsml:value>Alana SHORE</dsml:value>" +
    "      </dsml:attr>" +
    "      <dsml:attr name=\"objectClass\">" +
    "         <dsml:value>inetOrgPerson</dsml:value>" +
    "         <dsml:value>organizationalPerson</dsml:value>" +
    "         <dsml:value>person</dsml:value>" +
    "         <dsml:value>top</dsml:value>" +
    "      </dsml:attr>" +
    "      <dsml:attr name=\"sn\">" +
    "         <dsml:value>SHORE</dsml:value>" +
    "      </dsml:attr>" +
    "   </dsml:addRequest>" +
    "</dsml:batchRequest>"
*/
    static void getAddRequestElement(StringBuffer message, String name, Attributes atts)
            throws NamingException // doesn't really...
    {
        message.append(TAB4).append("<dsml:addRequest dn=\"").append(escapeName(name)).append("\">\n");

        NamingEnumeration attEnum = atts.getAll();
        while (attEnum.hasMore())
        {
            Attribute att = (Attribute) attEnum.next();
            String attName = att.getID();
            message.append(TAB5).append("<dsml:attr name=\"").append(attName).append("\">\n");
            NamingEnumeration values = att.getAll();
            while (values.hasMore())
            {
                Object value = values.next();
                createDsmlValueElement(value, message);
            }
            message.append(TAB5).append("</dsml:attr>\n");
        }
        message.append(TAB4).append("</dsml:addRequest>\n");

    }

    /**
     * This returns a &lt;dsml:attr ... &gt; , coding the value either as a string
     * or as a base64Binary type value (including the type marker).  Note that urls references
     * are not supported.
     *
     * @param value
     * @param message
     * @throws NamingException
     */
    static void createDsmlValueElement(Object value, StringBuffer message)
            throws NamingException
    {
        if (value instanceof String)  // expensive check, but can't think of any way around it...
        {
            String stringVal = escape((String) value);
            message.append(TAB6).append("<dsml:value>").append(stringVal).append("</dsml:value>\n");
        }
        else
        {
            try
            {
                byte[] data = (byte[]) value;
                String base64Data = new String(CBBase64.encode(data), "US-ASCII");
System.out.println("SENDING BINARY DATA; byte length: " + data.length + " encoded: " + base64Data.length());
                message.append(TAB6).append("<dsml:value xsi:type=\"xsd:base64Binary\">").append(base64Data).append("</dsml:value>\n");
            }
            catch (UnsupportedEncodingException e)
            {
                NamingException ne = new NamingException("unexpected encoding exception adding attribute value of type " + value.getClass());
                ne.setRootCause(e);
                throw ne;
            }
            catch (ClassCastException e)
            {
                NamingException ne = new NamingException("unexpected error adding attribute value of type " + value.getClass());
                ne.setRootCause(e);
                throw ne;
            }
        }
    }

//    "<dsml:batchRequest xmlns:dsml=\"urn:oasis:names:tc:DSML:2:0:core\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
//    "   <dsml:delRequest dn=\"cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU\"/>" +
//    "</dsml:batchRequest>";

    /**
     * This returns a DSML batchrequest containing a single 'delRequest' element.
     *
     * @param name the name
     * 
     * @return a DSML batchrequest containing a single 'delRequest' element
     */
    static StringBuffer constructDeleteRequest(String name)
    {
        StringBuffer message = new StringBuffer(200);
        message.append(STANDARDHEADER);

        getDeleteRequestElement(message, name);

        message.append(STANDARDFOOTER);

        return message;
    }


    /**
     * returns the actual DSML 'delRequest' element.<br>
     * e.g.:<br>
     * &lt;dsml:delRequest requestID="1" dn="cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU"/&gt;
     *
     * @param message
     * @param name
     */
    static void getDeleteRequestElement(StringBuffer message, String name)
    {
        message.append(TAB4 + "<dsml:delRequest dn=\"").append(escapeName(name)).append("\"/>\n");
    }

    /**
     * This is a co-ordinating method that builds up the Search Request XML by calling various
     * component building methods.
     *
     * @param name
     * @param scope
     * @param derefAliases
     * @param sizeLimit
     * @param timeLimit
     * @param typesOnly
     * @param filter
     * @param attributesToReturn
     * 
     * @return the search request
     * 
     * @throws NamingException
     */
    static StringBuffer constructSearchRequest(String name, String scope, String derefAliases, long sizeLimit, int timeLimit, boolean typesOnly, String filter, String[] attributesToReturn)
            throws NamingException
    {
        StringBuffer message = new StringBuffer(500);
        message.append(STANDARDHEADER);

        getSearchRequestHeader(message, name, scope, derefAliases, sizeLimit, timeLimit, typesOnly);

        getSearchRequestFilter(message, filter, TAB4);

        getSearchRequestAttributes(message, attributesToReturn);

        message.append(SEARCHFOOTER).append(STANDARDFOOTER);

        return message;
    }

    private static Pattern escapeGTPattern = Pattern.compile("(&gt;)", Pattern.DOTALL);
    private static Pattern escapeLTPattern = Pattern.compile("(&lt;)", Pattern.DOTALL);
    private static Pattern escapeAMPPattern = Pattern.compile("(&amp;)", Pattern.DOTALL);
    private static Pattern escapeQUOTPattern = Pattern.compile("(&quot;)", Pattern.DOTALL);
    private static Pattern escapeAPOSPattern = Pattern.compile("(&apos;)", Pattern.DOTALL);
    private static Pattern escapeOCTOPattern = Pattern.compile("(&#.*?;)", Pattern.DOTALL);

    /**
     * Translates escaped XML characters as per Extensible Markup Language (XML) 1.0 (Second Edition)
     * section 4.6.
     *
     * @param value a string containing escaped character sequences such as '&amp;' or '&
     * 
     * @return the un-escaped value
     */

// I wonder what the efficiency hit of this is?  Hopefully it won't happen very often,
// but it could be quite ugly for large strings.  Redo via StringBuffer?

    static String unescape(String value)
    {
        if (value.indexOf('&') == -1)  // if we don't need to do anything...
            return value;            // ... then don't.

        value = escapeGTPattern.matcher(value).replaceAll(">");
        value = escapeLTPattern.matcher(value).replaceAll("<");
        value = escapeQUOTPattern.matcher(value).replaceAll("\"");
        value = escapeAPOSPattern.matcher(value).replaceAll("'");
        value = escapeAMPPattern.matcher(value).replaceAll("&");

        Matcher OCTOMatcher = escapeOCTOPattern.matcher(value);

// cycle through the available entries
        while (OCTOMatcher.find())
        {
            String escapeChar = OCTOMatcher.group(1);

// TODO: this does not correctly cope with multiple escape sequence pairs -
// note that wierd sequences like: &#38;#60; can occur - we only want the last pair
// (&#38; == '&').  Not sure how to cope with this though, since the pattern match
// only grabs the first?

            int len = escapeChar.length();
            char char1 = 0;
            char char2 = 0;

            if (len >= 5)
            {
                char1 = escapeChar.charAt(len - 3);
                char2 = escapeChar.charAt(len - 2);
            }
            if (Character.isDigit(char1) && Character.isDigit(char2))
            {
                int val = Character.digit(char1, 16) * 16 + Character.digit(char2, 16);
                try
                {
                    String replacement = new String(new byte[]{new Integer(val).byteValue()}, "US-ASCII");
                    value = OCTOMatcher.replaceFirst(replacement);
                }
                catch (UnsupportedEncodingException e) // should never happen.  It's flippin ASCII after all!
                {
                    log.log(Level.SEVERE, "Unexpected exception trying to decode escaped value " + OCTOMatcher.group(1), e);
                }
            }
            else
            {
                log.info("unable to decode escaped value: " + escapeChar);
                break; // give up (including on any subsequent escape sequences)
            }

            OCTOMatcher = escapeOCTOPattern.matcher(value);
        }

        return value;
    }

    /**
     * Handling to DSML escape names.  Currently this is done in
     * exactly the same way as values, but this method will allow
     * us to perform any future trickiness that may be required.
     * 
     * @param value
     * 
     * @return the escaped value
     */
    static String escapeName(String value)
    {
        return escape(value);
    }


    /**
     * As per the standard XML rules, we have to escape 'html' like
     * characters  &gt;, &lt;, &apos;, &quot;, and &amp;.
     * @param value
     * 
     * @return the escaped string
     */
    static String escape(String value)
    {
        int len = value.length();

// quickly check if value needs escaping.

        boolean needsEscaping = false;
        for (int i = 0; i < len; i++)
        {
            switch (value.charAt(i))
            {
                case '>':
                    needsEscaping = true;
                    break;
                case '<':
                    needsEscaping = true;
                    break;
                case '&':
                    needsEscaping = true;
                    break;
                case '\"':
                    needsEscaping = true;
                    break;
                case '\'':
                    needsEscaping = true;
                    break;
            }
            if (needsEscaping)
                i = len;
        }

        if (!needsEscaping)
            return value;

// it does need escaping; so start modifying the string...
        StringBuffer buffy = new StringBuffer(value);

        for (int i = len - 1; i >= 0; i--)
        {
            switch (value.charAt(i))
            {
                case '>':
                    buffy.replace(i, i + 1, "&gt;");
                    break;
                case '<':
                    buffy.replace(i, i + 1, "&lt;");
                    break;
                case '&':
                    buffy.replace(i, i + 1, "&amp;");
                    break;
                case '\"':
                    buffy.replace(i, i + 1, "&quot;");
                    break;
                case '\'':
                    buffy.replace(i, i + 1, "&apos;");
                    break;
            }
        }

        return buffy.toString();
    }

    /**
     * <p>This constructs the XML tag and elements for a DSML search filter
     * <p/>
     * <p>e.g.
     * <pre>
     * &gt;dsml:filter&lt;
     * &gt;dsml:and&lt;
     * &gt;dsml:substrings name="sn"&lt;
     * &gt;dsml:initial&lt;Li&gt;/dsml:initial&lt;
     * &gt;/dsml:substrings&lt;
     * &gt;dsml:substrings name="cn"&lt;
     * &gt;dsml:initial&lt;Cr&gt;/dsml:initial&lt;
     * &gt;/dsml:substrings&lt;
     * &gt;/dsml:and&lt;
     * &gt;/dsml:filter&lt;
     * </pre>
     *
     * @param message an existing string buffer to append the search filter information to
     * @param filter  an LDAP RFC 2254 filter to translate into verbose DSML woffle
     * @param indent  whitespace padding to indent the filter for a pleasent and harmonious viewing experience
     * 
     * @return the search request filter
     */

    static String getSearchRequestFilter(StringBuffer message, String filter, String indent)
            throws NamingException
    {
        if (filter == null)
            throw new NamingException("null filter expression in get SearchRequestFilter");

        filter = filter.trim();
        if  (filter.length() == 0)
            throw new NamingException("empty filter expression in get SearchRequestFilter");

        log.finest("translating ldap filter '" + filter + "'");
/*
 *  This is where we 'grow' the dsml version of the ldap filter
 */
//StringBuffer message = new StringBuffer();

/*
 *  This is formatting fluff - we keep a margin of whitespace to indent the text nicely
 */
        StringBuffer padding = new StringBuffer(indent);

/*
 *  We keep operator DSML elements on a stack, so that we can output the closing tags easily
 */
        Stack stack = new Stack();

/*
 *  This tokenizer breaks up a RFC 2254 ldap filter into its components
 */
        StringTokenizer filterElements = new StringTokenizer(filter, "()", true);

        message.append(padding).append("<dsml:filter>\n");

        while (filterElements.hasMoreTokens())
        {
            String filterToken = filterElements.nextToken();
            try
            {
                if (filterToken.charAt(0) == '(')
                {
                    filterToken = filterElements.nextToken();  // skip the bracket, and look at what's next.  In ldap, this *cannot* be another '(' or ')'.

                    padding.append(TAB);
                    message.append(padding);

                    if (filterToken.length() == 1)
                    {
                        switch (filterToken.charAt(0))
                        {
                            case '&':
                                message.append("<dsml:and>\n");
                                stack.push("</dsml:and>\n");
                                break;
                            case '|':
                                message.append("<dsml:or>\n");
                                stack.push("</dsml:or>\n");
                                break;
                            case '!':
                                message.append("<dsml:not>\n");
                                stack.push("</dsml:not>\n");
                                break;
                            default:
                                throw new NamingException("unexpected token '" + filterToken + "' in ldap filter: " + filter);
                        }
                    }
                    else
                    {
                        stack.push("");  // add an empty stack item.
                        translateFilterItem(filterToken, padding, message); // add the search filter item (e.g. 'sn>=fr', 'cn=*' or 'ou=*research*'
                        padding.setLength(padding.length() - TABLEN);
                    }
                }
                else if (filterToken.charAt(0) == ')')
                {
                    String closingElement = (String) stack.pop();
                    if (closingElement.length() > 0)    // ignore empty stack items.
                    {
                        message.append(padding).append(closingElement); // add closing tab
                        padding.setLength(padding.length() - TABLEN);
                    }
                }
                else
                {
                    throw new NamingException("unexpected token '" + filterToken + "' in search filter: " + filter);
                }
            }
            catch (EmptyStackException e)
            {
                throw new NamingException("unexpected end of ldap search filter (empty or non matching brackets?): " + filter);
            }
        }

        message.append(padding).append("</dsml:filter>\n");

        return message.toString();
    }

    static void translateFilterItem(String filterToken, StringBuffer padding, StringBuffer dsmlFilter)
            throws NamingException
    {
        int equalpos = filterToken.indexOf('=');
        if (equalpos < 1 || equalpos > filterToken.length() - 2)
            throw new NamingException("Unable to parse ldap filter element '" + filterToken + "'");

        String attribute = filterToken.substring(0, equalpos);    // e.g. 'cn'
        String expression = filterToken.substring(equalpos + 1);    // e.g. '*fred*'

        char endOfAttribute = attribute.charAt(equalpos - 1);       // check to see if it was ~=, >= or <=

// see if it is a 'simple' filter item
        if (endOfAttribute == '<' || endOfAttribute == '>' || endOfAttribute == '~')
        {
            attribute = attribute.substring(0, equalpos - 1); // trim attribute name of the ending </>/~ character
            String value = new StringBuffer(100).append(" name=\"").append(attribute).append("\">").append(padding).append(TAB).append("<dsml:value>").append(escape(expression)).append("</dsml:value>").append(padding).toString();

            switch (endOfAttribute)
            {
                case '<':
                    dsmlFilter.append("<dsml:lessOrEqual").append(value).append("</dsml:lessOrEqual>\n");
                    break;
                case '>':
                    dsmlFilter.append("<dsml:greaterOrEqual").append(value).append("</dsml:greaterOrEqual>\n");
                    break;
                case '~':
                    dsmlFilter.append("<dsml:approxMatch").append(value).append("</dsml:approxMatch>\n");
                    break;
            }
        }
// see if it is a 'simple' equality match filter item
        else if (expression.indexOf('*') == -1)
        {
            dsmlFilter.append("<dsml:equalityMatch name=\"").append(attribute).append("\">").append(padding).append("    ").append("<dsml:value>").append(escape(expression)).append("</dsml:value>").append(padding).append("</dsml:equalityMatch>\n");
        }
// see if it is a 'present' filter item
        else if (expression.equals("*"))
        {
            dsmlFilter.append("<dsml:present name=\"").append(attribute).append("\"/>\n");
        }
// yucky hard cases.
        else // the expression contains at least one '*', and is longer than 1 character.
        {
            dsmlFilter.append("<dsml:substrings name=\"").append(attribute).append("\">\n");
            padding.append(TAB);

// prepare to parse expressions like 'Fr*n*k*stein"
            StringTokenizer substringFilter = new StringTokenizer(expression, "*", true);
            String initial = substringFilter.nextToken();
            if (initial.equals("*") == false)
                dsmlFilter.append(padding).append("<dsml:initial>").append(escape(initial)).append("</dsml:initial>\n"); // were they drunk when they designed this verbose crud?

            while (substringFilter.hasMoreTokens())
            {
                String filterElement = substringFilter.nextToken();
                if (filterElement.equals("*") == false) // skip over the stars
                {
                    if (substringFilter.hasMoreTokens() == false) // then this is the last one!
                        dsmlFilter.append(padding).append("<dsml:final>").append(escape(filterElement)).append("</dsml:final>\n");
                    else
                        dsmlFilter.append(padding).append("<dsml:any>").append(escape(filterElement)).append("</dsml:any>\n");
                }
            }
            padding.setLength(padding.length() - TABLEN);
            dsmlFilter.append(padding).append("</dsml:substrings>\n");

        }
    }
// LDAP v3 Extensions not supported - your code here...
                     
    public ExtendedResponse extendedOperation(ExtendedRequest extendedRequest) throws NamingException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public LdapContext newInstance(Control[] controls) throws NamingException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void reconnect(Control[] controls) throws NamingException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Control[] getConnectControls() throws NamingException
    {
        return new Control[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setRequestControls(Control[] controls) throws NamingException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Control[] getRequestControls() throws NamingException
    {
        return new Control[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Control[] getResponseControls() throws NamingException
    {
        return new Control[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
