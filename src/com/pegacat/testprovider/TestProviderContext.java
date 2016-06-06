package com.pegacat.testprovider;

import com.ca.commons.naming.DXEntry;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>This is a test jndiprovider context to be used for junit testing etc.
 * <p/>
 * <p>It provides support for the basic JNDI operations used for a GUI client.
 * <p/>
 * <p>It does *not* support:</p>
 * <ul>
 * <li>binding java objects - the logon methods are not implemented.  Users will need to do their
 * own serialisation if they want this functionality.
 * <li> no referrals
 * <li> no ldap v3 extensions.
 * <li> no composite names spanning multiple namespaces - all names are LDAP names only.  (Composite
 * names are a bloody stupid idea that come close to making jndiproviders unusable, IMNSHO - CB)
 * <li> thread safety; it is intended to be run single threaded during testing, and was not built
 * with thread safety in mind, so you're on your own there...
 * </ul>
 * <p/>
 * <p>If you need the above features, you should probably plan to use system tests and a real provider.</p>
 * <p>In Progress:</p>
 * <p>The following are yet to be implemented</p>
 * <ul>
 * <li>alias search scope
 * </ul>
 */

public class TestProviderContext implements LdapContext
{
    /**
     * This is the core internal data structure where entries are stored
     */
    private DataTree entries;

    // SEARCH ALIAS DEREF OPTIONS
    private static String NEVER = "never";
    private static String SEARCHING = "searching";
    private static String FINDING = "finding";
    private static String ALWAYS = "always";
    private static String[] ALIASOPTIONS = new String[]{NEVER, SEARCHING, FINDING, ALWAYS};

    // Default Search Controls
    private static SearchControls DEFAULT_SEARCH_CONTROLS = new SearchControls();


    protected Hashtable environment;

    // the name of the context (as set by 'createSubcontext' methods
    private String contextName = "";

    private boolean secureLogon = false;  // whether we've securely logged on.

    private static Logger log = Logger.getLogger(TestProviderContext.class.getName());

    // Debug
    {
        log.setLevel(Level.FINEST);
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


    private TestProviderContext()
    {
        // don't do this :-).
    }

    public boolean isSecure()
    {
        return secureLogon;
    }

    public DataTree getEntries()
    {
        return entries;
    }

    private TestProviderContext(String baseDN, Hashtable env, DataTree tree)
        throws NamingException
    {
        entries = tree;  // reuse base data tree.  In a real system this could create problems since
        // it has extra entries in it above the local context 'baseDN'... *shrug*

        checkSecurity(env);

        contextName = baseDN == null ? "" : baseDN;

        environment = env;     // Don't see any reason to clone environment for every entry?
    }

    private void checkSecurity(Hashtable env)
            throws AuthenticationException
    {
        // this is only for testing security - if no credentials are provided, full access is given :-).

        if (env != null && "simple".equals(env.get(Context.SECURITY_AUTHENTICATION)))         // 'simple' = username + password
        {
            String userName = (String)env.get(Context.SECURITY_PRINCIPAL);
            String userPwd = (String) env.get(Context.SECURITY_CREDENTIALS);

            checkUserNameAndPassword(userName, userPwd);
        }
        else
        {
            System.out.println("logged in anonymously");
        }
    }

    /**
     * There's magic in here to replicate the behaviour of directories that allow
     * in directory encryption of user passwords (e.g. with sha1) and then will
     * successfully match *either* the plain text password, or the encoded password.
     *
     * Not sure what ApacheDS does...
     *
     * @param userName
     * @param userPwd
     * @throws AuthenticationException
     */
    void checkUserNameAndPassword(String userName, String userPwd)
            throws AuthenticationException
    {

        secureLogon = false;

        if (userName != null)
        {
            // cause this is a test, we'll hardcode an always works...

            if (((userName.equals("Chris")||userName.equals("cn=Chris,ou=research,o=pegacat,c=au"))
                    && (userPwd.equals("secret"))))
            {
                secureLogon = true;
            }
            else
            {
                try
                {
                    Attributes atts = getAttributes(userName);

                    Attribute pwdAtt = atts.get("userPassword");
                    if (pwdAtt == null || pwdAtt.size() == 0)
                        throw new AuthenticationException("no password for user " + userName + " in directory");
                    String pwd = pwdAtt.get().toString();

                    if (pwd.startsWith("{sha}") || pwd.startsWith("{SHA}"))
                    {
                        if (!(userPwd.startsWith("{sha}") || userPwd.startsWith("{SHA}")))
                        {
                            userPwd = shaEncode(userPwd);
                        }
                        // compare suffixes...

                        if (pwd.substring(5).equals(userPwd.substring(5))) // skip '{sha}' prefixes...
                            secureLogon = true;
                    }
                    else if (pwd.equals(userPwd))
                    {
                        secureLogon = true;
                    }
                }
                catch (NamingException e)
                {
                    throw new AuthenticationException("no user '" + userName + "'");
                }
                catch (Exception e)
                {
                    throw new AuthenticationException("unable to authenticate user '" + userName + "' " + e.getMessage());
                }

            }

            if (secureLogon)
                System.out.println("logged in as " + userName + " : " + userPwd);
            else
                throw new AuthenticationException("unable to authenticate user '" + userName + "' " );
        }
    }

    // copied from com.ca.commons.jndi.JNDITools
    private static String shaEncode(String s)
        throws IOException
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("sha");
            StringBuffer hexString = new StringBuffer("{sha}");

            md.reset();  // necessary?

            md.update(s.getBytes("UTF-8"));

            byte[] buff = md.digest();

            //TODO: create local copy of this within testprovider package to make package stand alone?
            hexString.append(Base64.binaryToString(buff));
            return hexString.toString();
           // return hexString.toString().getBytes("UTF-8");
        }
        catch (java.security.NoSuchAlgorithmException e)
        {
            log.log(Level.WARNING, "Unexpected error encoding password ", e);
            e.printStackTrace();

            throw new IOException("internal error encrypting password " + e.getMessage());
        }
    }
    /**
     * Called by TestProviderFactory / also used for testing
     *
     * @param env the environment to use for this connection...
     */
    TestProviderContext(Hashtable env, DataTree data)
        throws AuthenticationException
    {
        entries = data;
        checkSecurity(env);
        environment = (env == null) ? new Hashtable() : env;
        log.fine("Created TestProviderContext");
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
    public DirContext getSchema(String name) throws NamingException
    {
        throw new OperationNotSupportedException("TestProviderContext does not support reading schema (yet)");
    }

    /**
     * Retrieves a context containing the schema objects of the
     * named object's class definitions.
     * See {@link #getSchemaClassDefinition(javax.naming.Name)} for details.
     *
     * @param name the name of the object whose object class
     *             definition is to be retrieved
     * @return	the <tt>DirContext</tt> containing the named
     * object's class definitions; never null
     * @throws	javax.naming.OperationNotSupportedException if schema not supported
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated not yet implemented
     */
    public DirContext getSchemaClassDefinition(String name) throws NamingException
    {
        throw new OperationNotSupportedException("TestProviderContext does not support reading schema (yet)");
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
    public DirContext getSchema(Name name) throws NamingException
    {
        throw new OperationNotSupportedException("TestProviderContext does not support reading schema (yet)");
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
     * <tt>DirContext</tt> representing the (directory's) object class
     * definition of "Person".
     * <p/>
     * The information that can be retrieved from an object class definition
     * is directory-dependent.
     * <p/>
     * Prior to JNDI 1.2, this method
     * returned a single schema object representing the class definition of
     * the named object.
     * Since JNDI 1.2, this method returns a <tt>DirContext</tt> containing
     * all of the named object's class definitions.
     *
     * @param name the name of the object whose object class
     *             definition is to be retrieved
     * @return	the <tt>DirContext</tt> containing the named
     * object's class definitions; never null
     * @throws	javax.naming.OperationNotSupportedException if schema not supported
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated not yet implemented
     */
    public DirContext getSchemaClassDefinition(Name name) throws NamingException
    {
        throw new OperationNotSupportedException("TestProviderContext does not support reading schema (yet)");
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
     * @param name  the name to logon; may not be empty
     * @param obj   the object to logon; possibly null
     * @param attrs the attributes to associate with the binding
     * @throws	javax.naming.NameAlreadyBoundException if name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if some "mandatory" attributes
     * of the binding are not supplied
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated this method will not be implemented
     */
    public void bind(String name, Object obj, Attributes attrs) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects not supported");
    }

    /**
     * Binds a name to an object, along with associated attributes,
     * overwriting any existing binding.
     * See {@link #rebind(javax.naming.Name, Object, javax.naming.directory.Attributes)} for details.
     *
     * @param name  the name to logon; may not be empty
     * @param obj   the object to logon; possibly null
     * @param attrs the attributes to associate with the binding
     * @throws	javax.naming.directory.InvalidAttributesException if some "mandatory" attributes
     * of the binding are not supplied
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated this method will not be implemented
     */
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects not supported");
    }

    /**
     * Binds a name to an object, along with associated attributes.
     * If <tt>attrs</tt> is null, the resulting binding will have
     * the attributes associated with <tt>obj</tt> if <tt>obj</tt> is a
     * <tt>DirContext</tt>, and no attributes otherwise.
     * If <tt>attrs</tt> is non-null, the resulting binding will have
     * <tt>attrs</tt> as its attributes; any attributes associated with
     * <tt>obj</tt> are ignored.
     *
     * @param name  the name to logon; may not be empty
     * @param obj   the object to logon; possibly null
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
        throw new OperationNotSupportedException("binding of java objects not supported");
    }

    /**
     * Binds a name to an object, along with associated attributes,
     * overwriting any existing binding.
     * If <tt>attrs</tt> is null and <tt>obj</tt> is a <tt>DirContext</tt>,
     * the attributes from <tt>obj</tt> are used.
     * If <tt>attrs</tt> is null and <tt>obj</tt> is not a <tt>DirContext</tt>,
     * any existing attributes associated with the object already bound
     * in the directory remain unchanged.
     * If <tt>attrs</tt> is non-null, any existing attributes associated with
     * the object already bound in the directory are removed and <tt>attrs</tt>
     * is associated with the named object.  If <tt>obj</tt> is a
     * <tt>DirContext</tt> and <tt>attrs</tt> is non-null, the attributes
     * of <tt>obj</tt> are ignored.
     *
     * @param name  the name to logon; may not be empty
     * @param obj   the object to logon; possibly null
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
        throw new OperationNotSupportedException("binding of java objects not supported");
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
        log.finest("getKeyValueList (" + name.toString() + ")");

        NamingEnumeration en = doSearch(name.toString(), SearchControls.OBJECT_SCOPE, SEARCHING, 0, 0, false, "(objectClass=*)", attrIds);

        if (en.hasMoreElements() == false)
            return new BasicAttributes();  // return empty attributes object for 'virtual' nodes (e.g. router entries, base DN RDNs)

        SearchResult result = (SearchResult) en.next();

        return result.getAttributes();
//return getTestAttributes(name.toString());

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
     * This method creates a new subcontext with the given name, binds it in
     * the target context (that named by all but terminal atomic
     * component of the name), and associates the supplied attributes
     * with the newly created object.
     * All intermediate and target contexts must already exist.
     * If <tt>attrs</tt> is null, this method is equivalent to
     * <tt>Context.createSubcontext()</tt>.
     *
     * @param entry  The DXEntry object, combining a name and a set of attributes
     * @return	the newly created context
     * @throws	javax.naming.NameAlreadyBoundException if the name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if <code>attrs</code> does not
     * contain all the mandatory attributes required for creation
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public DirContext createSubcontext(DXEntry entry) throws NamingException
    {
        return createSubcontext(entry.getStringName(), entry);
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
    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException
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
        filter = getAttributePresentFilter(matchingAttributes);

        return doSearch(name, SearchControls.SUBTREE_SCOPE, SEARCHING, 0, 0, false, filter, attributesToReturn);
    }


    /**
     * This takes an Attribute set and creates the corresponding ldap filter.
     *
     * @param matchingAttributes a list of required attributes(e.g. 'orgunit', 'person')
     * @return the matching ldap filter; e.g. (&(orgunit=*)(person=*))
     */

    static String getAttributePresentFilter(Attributes matchingAttributes)
    {
        int numbAtts = (matchingAttributes == null) ? 0 : matchingAttributes.size();
        if (numbAtts == 0)
            return "(objectClass=*)";

        if (numbAtts == 1)
            return "(" + (String) matchingAttributes.getIDs().nextElement() + "=*)";

        StringBuffer filterBuffer = new StringBuffer("(&");

        Enumeration atts = matchingAttributes.getIDs();  // ignore NamingException, as these atts come from the client, not the server
        while (atts.hasMoreElements())
            filterBuffer.append("(").append((String) atts.nextElement()).append("=*)");

        filterBuffer.append(")");

        return filterBuffer.toString();
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
     * When changes are made to this <tt>DirContext</tt>,
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
        return search(name, filter, null, cons);
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
     * The service provider for such a directory would then translateToDisplayTitle
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
            throw new NamingException("null ldap filter in TestProviderContext search");

        if (filterArgs != null && filterArgs.length > 0)
            throw new NamingException("filter arguments not implemented in testprovider");

        log.finest("search III (" + name + ") + filter: " + filterExpr);

        if (cons == null)
            cons = DEFAULT_SEARCH_CONTROLS;

        String aliasHandling = (String) environment.get("java.naming.ldap.derefAliases");

        if (aliasHandling == null)
            aliasHandling = "always";

        NamingEnumeration returnEnum = doSearch(name, cons.getSearchScope(), aliasHandling, cons.getCountLimit(), cons.getTimeLimit(), false, filterExpr, cons.getReturningAttributes());

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
     * When changes are made to this DirContext,
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
     * Unbinds the named object.
     * See {@link #unbind(javax.naming.Name)} for details.
     *
     * @param name the name to unbind; may not be empty
     * @throws	javax.naming.NameNotFoundException if an intermediate context does not exist
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated logon operations not supported
     */
    public void unbind(String name) throws NamingException
    {
        throw new OperationNotSupportedException("TestProviderContext does not support storing java objects");
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
        return (Hashtable) environment.clone();
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
     * @deprecated logon/unbind operations not supported by TestProviderContext
     */
    public void unbind(Name name) throws NamingException
    {
        throw new OperationNotSupportedException("Storing java objects not supported");
    }

    /**
     * Retrieves the named object.
     * See {@link #lookup(javax.naming.Name)} for details.
     *
     * @param name the name of the object to look up
     * @return	the object bound to <tt>name</tt>
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated java Object specific methods such as logon/unbind/lookup are not supported by TestProviderContext
     */
    public Object lookup(String name) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects not supported");
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
     * @deprecated java Object specific methods such as logon/unbind/lookup are not supported by TestProviderContext
     */
    public Object lookupLink(String name) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects not supported");
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
     * @param name the name to logon; may not be empty
     * @param obj  the object to logon; possibly null
     * @throws	javax.naming.NameAlreadyBoundException if name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if object did not supply all mandatory attributes
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated java Object specific methods such as logon/unbind/lookup are not supported by TestProviderContext
     */
    public void bind(String name, Object obj) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects not supported");
    }

    /**
     * Binds a name to an object, overwriting any existing binding.
     * See {@link #rebind(javax.naming.Name, Object)} for details.
     *
     * @param name the name to logon; may not be empty
     * @param obj  the object to logon; possibly null
     * @throws	javax.naming.directory.InvalidAttributesException if object did not supply all mandatory attributes
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated java Object specific methods such as logon/unbind/lookup are not supported by TestProviderContext
     */
    public void rebind(String name, Object obj) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects not supported");
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
     * @deprecated java Object specific methods such as logon/unbind/lookup are not supported by TestProviderContext
     */
    public Object lookup(Name name) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects not supported");
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
     * @deprecated java Object specific methods such as logon/unbind/lookup are not supported by TestProviderContext
     */
    public Object lookupLink(Name name) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects not supported");
    }

    /**
     * Binds a name to an object.
     * All intermediate contexts and the target context (that named by all
     * but terminal atomic component of the name) must already exist.
     *
     * @param name the name to logon; may not be empty
     * @param obj  the object to logon; possibly null
     * @throws	javax.naming.NameAlreadyBoundException if name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if object did not supply all mandatory attributes
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #bind(String, Object)
     * @see #rebind(javax.naming.Name, Object)
     * @see javax.naming.directory.DirContext#bind(javax.naming.Name, Object,
            *      javax.naming.directory.Attributes)
     * @deprecated java Object specific methods such as logon/unbind/lookup are not supported by TestProviderContext
     */
    public void bind(Name name, Object obj) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects not supported");
    }

    /**
     * Binds a name to an object, overwriting any existing binding.
     * All intermediate contexts and the target context (that named by all
     * but terminal atomic component of the name) must already exist.
     * <p/>
     * <p> If the object is a <tt>DirContext</tt>, any existing attributes
     * associated with the name are replaced with those of the object.
     * Otherwise, any existing attributes associated with the name remain
     * unchanged.
     *
     * @param name the name to logon; may not be empty
     * @param obj  the object to logon; possibly null
     * @throws	javax.naming.directory.InvalidAttributesException if object did not supply all mandatory attributes
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @see #rebind(String, Object)
     * @see #bind(javax.naming.Name, Object)
     * @see javax.naming.directory.DirContext#rebind(javax.naming.Name, Object,
            *      javax.naming.directory.Attributes)
     * @see javax.naming.directory.DirContext
     * @deprecated java Object specific methods such as logon/unbind/lookup are not supported by TestProviderContext
     */
    public void rebind(Name name, Object obj) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects not supported");
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
        rename(new LdapName(oldName), new LdapName(newName));
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
     * @see javax.naming.directory.DirContext#createSubcontext
     * @deprecated it is impossible to create a directory object without attributes  (RFC 2251 - sec 4.7)
     */
    public Context createSubcontext(Name name) throws NamingException
    {
        throw new InvalidAttributesException("cannot create directory object without attributes");
    }

    /**
     * Retrieves the parser associated with the named context.  Note that the
     * TestProviderContext does not support composite names, and so DsmlParser.parser
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
        return TestProviderParser.parser;
    }

    /**
     * Retrieves the parser associated with the named context.  Note that the
     * TestProviderContext does not support composite names, and so DsmlParser.parser
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
        return TestProviderParser.parser;
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

        return doSearch(name, SearchControls.ONELEVEL_SCOPE, SEARCHING, 0, 0, false, "(objectClass=*)", null);
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
     * @deprecated java Object specific methods such as logon/unbind/lookup are not supported by TestProviderContext
     */
    public NamingEnumeration listBindings(String name) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects not supported");
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
     * @deprecated java Object specific methods such as logon/unbind/lookup are not supported by TestProviderContext
     */
    public NamingEnumeration listBindings(Name name) throws NamingException
    {
        throw new OperationNotSupportedException("binding of java objects not supported");
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
     * TestProviderContext does not support composite names (stupid idea anyway).
     *
     * @param name   a name relative to this context
     * @param prefix the name of this context relative to one of its ancestors
     * @return	the composition of <code>prefix</code> and <code>name</code>
     * @throws	javax.naming.NamingException if a naming exception is encountered
     * @deprecated
     */
    public String composeName(String name, String prefix) throws NamingException
    {
        throw new OperationNotSupportedException("TestProviderContext does not support composing names - use LdapName directly");
    }

    /**
     * Not implemented - use DN methods instead if you want to create new Names.
     * TestProviderContext does not support composite names (stupid idea anyway).
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
        throw new OperationNotSupportedException("TestProviderContext does not support composing names - use LdapName directly");
    }





//
//             CORE IMPLEMENTATION METHODS HERE!
//             (other methods just chain to these ones)
//






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

        TreeEntry modifyMe = entries.get(name);

        if (modifyMe == null)
            throw new NameNotFoundException("unable to find entry '" + name + "'");

        for (ModificationItem mod : mods)
        {
            Attribute modificationAttribute = mod.getAttribute();
            String attributeID = modificationAttribute.getID();
            Attribute current = modifyMe.get(attributeID);

            switch (mod.getModificationOp())
            {
                /*
                 *  Add Attribute : this appends the new value(s) to the
                 *  existing attribute values.
                 */
                case DirContext.ADD_ATTRIBUTE:

                    if (current != null)
                    {
                        NamingEnumeration vals = modificationAttribute.getAll();
                        while (vals.hasMore())
                        {
                            Object newVal = vals.next();
                            if (current.contains(newVal)== false)
                                current.add(newVal);
                            else    // some directories (openldap) throw an exception when you try
                                    // to add an attribute that already exists => need to emulate worst case
                                throw new NamingException("Error adding Attribute " + attributeID + ".  Attribute already has value " + newVal);
                        }
                    }
                    else
                    {
                        current = modificationAttribute;  // if the attribute doesn't exist, create it.
                    }

                    modifyMe.put(current);
                    
                    break;

                /*
                 *  Replate Attribute : this completely replaces the old
                 *  values with the new values.
                 */
                case DirContext.REPLACE_ATTRIBUTE:

                    modifyMe.put(modificationAttribute);
                    break;

                /*
                 *  Deletes all values in the modification list, and removes
                 * the attribute entirely if nothing is left...
                 */

                case DirContext.REMOVE_ATTRIBUTE:

                    if (current != null)
                    {
                        NamingEnumeration vals = modificationAttribute.getAll();

                        if (vals.hasMore()==false)
                        {
                            // if there are no values to delete, this means to delete the entire attribute
                            modifyMe.remove(current.getID());
                            break;
                        }

                        // spin through list of values to remove, and delete them from the current attribute
                        while (vals.hasMore())
                        {
                            Object newVal = vals.next();
                            if (current.contains(newVal)== true)
                                current.remove(newVal);
                            else    // some directories (openldap) throw an exception if you try to delete a value
                                    //  that doesn't exist = need to emulate worst case
                                throw new NamingException("Error deleting Attribute " + attributeID + "does not have value " + newVal);
                        }
                        if (current.size()>0)
                            modifyMe.put(current);
                        else
                            modifyMe.remove(current.getID());  // remove the whole attribute if there are no values left.
                    }
                    else
                        throw new NamingException("Attribute " + attributeID);

                    break;
            }
        }


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
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException
    {
        log.finest("createSubcontext (" + name + ")");

        checkEntry(name, attrs);

        entries.addEntry(new TreeEntry(new LdapName(name), attrs));

        return new TestProviderContext(name, environment, entries);
    }

    /**
     * Hmm... this is a bit of a special purpose hack I guess, where I hard code a couple
     * of well known object classes so as to be able to throw mandatory attribute
     * exceptions.  One day it might be built out to be a bit more substantive I guess...
     * @param attrs
     * @throws NamingException
     */
    protected void checkEntry(String name, Attributes attrs) throws NamingException
    {
        Attribute oc = attrs.get("objectClass");
        if (oc == null)
            oc = attrs.get("objectclass");

        if (oc==null)
            throw new InvalidAttributesException("no object class found for entry: " + name);

        if (oc.contains("person")) // check contains 'cn' and 'sn'
        {
            if (attrs.get("cn") == null)
                throw new InvalidAttributesException("person entry does not contain 'cn' attribute");
            if (attrs.get("sn") == null)
                throw new InvalidAttributesException("person entry does not contain 'sn' attribute");
        }

        if (oc.contains("groupmindEntry")) // special check for a custom project of mine
        {
            if (attrs.get("uid") == null)
                throw new InvalidAttributesException("groupmindEntry entry does not contain 'uid' attribute");
            if (attrs.get("title") == null)
                throw new InvalidAttributesException("groupmindEntry entry does not contain 'title' attribute");

            if (attrs.get("cn") == null)
                throw new InvalidAttributesException("groupmindEntry entry does not contain 'cn' attribute");
            else
            {
                String cn = attrs.get("cn").get().toString();
                if (!name.startsWith("cn=" + cn))
                    throw new InvalidNameException("DAP: error code 64 - value of naming attribute 'cn' (" + cn + ") is not present in entry]; remaining name = " + name);
            }


        }
    }

    /**
     * Another create Subcontext method, taking an Entry object for initialisation.
     * @param newEntry
     * @return
     * @throws NamingException
     */
    public DirContext createSubcontext(TreeEntry newEntry) throws NamingException
    {
        Name newName = newEntry.getName();

        log.finest("createSubcontext (" + newName + ")");

        checkEntry(newEntry.getName().toString(), newEntry);

        entries.addEntry(new TreeEntry(newName, newEntry));

        return new TestProviderContext(newName.toString(), environment, entries);
    }



    /**
     * This is the central coordinating search method.  It takes all the raw paramaters, and hands them off
     * to various sub methods do construct the search request, send it to the DSML server and get the result.
     *
     * @param name
     * @param searchScope        - one of SearchControls.[OBJECT_SCOPE | ONELEVEL_SCOPE | SUBTREE_SCOPE]
     * @param aliasHandling      - one of 'always', 'never', 'finding', 'searching'
     * @param sizeLimit
     * @param timeLimit
     * @param typesOnly
     * @param filter
     * @param attributesToReturn
     * @return
     * @throws NamingException
     */
    public NamingEnumeration <SearchResult> doSearch(String name, int searchScope, String aliasHandling, long sizeLimit, int timeLimit, boolean typesOnly, String filter, String[] attributesToReturn)
            throws NamingException
    {
        TestProviderEnumeration <SearchResult> returnEnumeration = new TestProviderEnumeration <SearchResult> ();


        if (aliasHandling == null)
            aliasHandling = (String) environment.get("java.naming.ldap.derefAliases");

        // find the named entry the search scope is based on
        TreeEntry entry = entries.get(name);

        int baseNameLength = name.length(); // we have to trim the names to be relative to the searched name.  In a proper OO library this wouldn't be necessary, and LdapNames would be used throughout.  *shrug*
        if (baseNameLength>0)
            baseNameLength++;  // trim the comma...

        if (entry == null)
        {
            /*
            if (name.equals("") || contextName.startsWith(name)) // trying to allow for searches from higher levels than actually exist - dodgy...
            {
                entry = entries.get(contextName);
                if (entry == null)
                    throw new NameNotFoundException("internal test provider error - unable to find context name entry (e.g. base DN) " + contextName);
            }
            else
            */
                throw new NameNotFoundException(name);  // Mmm... using exceptions for a normally encountered condition... classy design of Context interface by jndi...
        }

        switch (searchScope)
        {
            case SearchControls.OBJECT_SCOPE:

                if (filterMatches(filter, entry))
                    returnEnumeration.add(new SearchResult("", null, (Attributes)entry.clone()));
                break;

            case SearchControls.ONELEVEL_SCOPE:

                ArrayList<TreeEntry> children = entry.getChildren();

                for (TreeEntry child : children)
                {
                    if (filterMatches(filter, child))
                    {
                        String childName = child.getStringName();
                        returnEnumeration.add(new SearchResult(childName.substring(0, childName.length() - baseNameLength), null, (Attributes)child.clone()));
                    }
                }

                break;

            case SearchControls.SUBTREE_SCOPE:

                getSubTree(entry, filter, returnEnumeration, baseNameLength);

                break;
        }

        // if we were worried about efficiency we might do this differently; but it's a test provider!
        if (attributesToReturn != null)  // null == 'return all attributes'
        {
            stripExcessAttributes(returnEnumeration, attributesToReturn);
            addOperationalAttributes(returnEnumeration, attributesToReturn);
        }


        return returnEnumeration;

    }

    /**
     * Strips all the non required attributes from the search results.  Not particularly efficient - *shrug*.
     * @param returnEnumeration
     * @param attributesToReturn
     */
    private void stripExcessAttributes(TestProviderEnumeration<SearchResult> returnEnumeration, String[] attributesToReturn)
    {
        for (SearchResult item : returnEnumeration )
        {
            if (attributesToReturn.length == 0 || attributesToReturn[0].equals("1.1")) // 'empty array' = 'return no attributes', as does the magic attribute "1.1"
                item.setAttributes(new BasicAttributes());
            else
            {
                Attributes existingAtts = item.getAttributes();
                BasicAttributes newAtts = new BasicAttributes();
                for (String returnAtt : attributesToReturn)
                {
                    if (returnAtt.equals("*"))  // special handling for wildcard
                    {
                        item.setAttributes(existingAtts);   // add all attributes
                        return;                             // and bail out                                                     
                    }

                    if (existingAtts.get(returnAtt)!= null)
                        newAtts.put(existingAtts.get(returnAtt));
                }
                item.setAttributes(newAtts);
            }
        }
    }

    /**
     * Adds fake operational attributes for testing...
     * @param returnEnumeration
     * @param attributesToReturn
     */
    private void addOperationalAttributes(TestProviderEnumeration<SearchResult> returnEnumeration, String[] attributesToReturn)
    {
        HashSet returnAtts = new HashSet(attributesToReturn.length);
        for (String att : attributesToReturn)
            returnAtts.add(att.toLowerCase());

        // this is where we could load up fake schema and stuff...

        if (returnAtts.contains("namingcontexts"))
        {
            BasicAttribute namingContext = new BasicAttribute("namingContexts");
            namingContext.add("");  // the naming context is always "" for this fake provider.  *shrug*.

            for (SearchResult item : returnEnumeration )
            {
                if (item.getName().length()==0) // only the root node gets this op attribute
                    item.getAttributes().put(namingContext);
            }
        }

        if (returnAtts.contains("subschemasubentry"))
        {
            BasicAttribute subschema = new BasicAttribute("subSchemaSubEntry", "cn=schema");

            for (SearchResult item : returnEnumeration )
            {
                if (item.getName().length()==0) // only the root node gets this op attribute
                    item.getAttributes().put(subschema);
            }
        }
    }



    /**
     * recursively walk the entry tree to return the entire sub tree of entries from the given entry, filtering
     * as we go.
     * @param entry
     * @param filter
     * @param returnEnumeration
     * @param baseNameLength
     */


    private void getSubTree(TreeEntry entry, String filter, TestProviderEnumeration<SearchResult> returnEnumeration, int baseNameLength)
    {
        if (filterMatches(filter, entry))
        {
            String name = entry.getStringName();
            name = (name.length() <= baseNameLength)?"":name.substring(0, name.length() - baseNameLength);
            returnEnumeration.add(new SearchResult(name, null, (Attributes)entry.clone()));
        }

        for (TreeEntry child : entry.getChildren())
            getSubTree(child, filter, returnEnumeration, baseNameLength);
    }



    /**
     * Parse more complex ldap search filter, e.g.
     *
     * (&(cn=Fred)(!objectclass=Topic))
     *
     * (&(!(|(cn=Fred)(cn=Nigel)))(objectClass=Topic))
     *
     * (&(objectClass=Person)(|(sn=Jensen)(cn=Babs J*)))

     *
     * @param filter
     * @param candidate
     * @return false, if the filter fails to match, or is too complex to parse with this dodgy parser...
     */
    public boolean filterMatches(String filter, TreeEntry candidate)
    {
        if (filter.equalsIgnoreCase("(objectclass=*)"))
            return true;
        else if (isSimpleFilter(filter))
            return evaluateAtomicFilterElement(filter, candidate);
        else
        {
            char operator = filter.charAt(1);

            String residualFilter = filter.substring(2, filter.length()-1);    // trim off enclosing brackets and operator...

            switch (operator)
            {
                case '&' :
                    String[] andComponents = splitFilterComponents(residualFilter);
                    return filterMatches(andComponents[0], candidate)&filterMatches(andComponents[1], candidate);

                case '|' :
                    String[] orComponents = splitFilterComponents(residualFilter);
                    return filterMatches(orComponents[0], candidate)|filterMatches(orComponents[1], candidate);

                case '!' : return !filterMatches(residualFilter, candidate);

                default: return evaluateAtomicFilterElement(residualFilter, candidate);
            }
        }
    }

    public String[] splitFilterComponents(String filter)
    {
        int depth = 0;
        for (int i=0; i<filter.length(); i++)
        {
             if (filter.charAt(i) == '(')
                 depth++;

            if (filter.charAt(i) == ')')
                depth--;

            if (depth ==0) // exit
            {
                if (i>=filter.length()-1) // oops - not splitable
                    return new String[] {filter, ""};
                else
                    return new String[]{filter.substring(0, i+1), filter.substring(i+1)};
            }
        }

        log.severe("Error parsing search filter: (can't split into components) " + filter);
        return new String[] {null, null};
    }

    /**
     * Figures out the result for a single filter elements e.g. (objectClass=Top*) or (cn=*Betts)
     * @param filter
     * @param candidate
     * @return
     */
    public boolean evaluateAtomicFilterElement(String filter, TreeEntry candidate)
    {
        if (filter.length()==0) // empty filter evaluates to true?  (necessary?)
            return true;

        int equalPos = filter.indexOf('=');
       String attributeName = filter.substring(1, equalPos);
       String searchValue = filter.substring(equalPos+1, filter.length()-1);
       Attribute testAtt = candidate.get(attributeName);
       if (testAtt == null)
           return false;

       if (searchValue.equals("*"))
           return true;

       if (testAtt.contains(searchValue))
           return true;

       // getting very dodgy here...  only handling single valued attributes - half hearted wild card search
       try
       {
           boolean endsWithStar = searchValue.endsWith("*");
           boolean startsWithStar = searchValue.startsWith("*");
           if (endsWithStar || startsWithStar)
           {
               String testVal = testAtt.get().toString();
               if (endsWithStar && searchValue.length()>1)
                   searchValue = searchValue.substring(0, searchValue.length()-1);
               if (startsWithStar && searchValue.length()>1)
                   searchValue = searchValue.substring(1);

               if (startsWithStar && endsWithStar)
               {
                   if (testVal.contains(searchValue))
                       return true;
               }
               else if (startsWithStar)
               {
                   if (testVal.endsWith(searchValue))
                       return true;
               }
               else
               {
                   if (testVal.startsWith(searchValue))
                       return true;
               }
           }
       }
       catch (NamingException e)
       {
           return false;
       }
       return false;
    }


    // we only handle (x=y) style filters in the test provider :-)
    private boolean isSimpleFilter(String filter)
    {
        // a clever person would do this with a regexp...
        if (filter.indexOf('&')>-1) return false;
        if (filter.indexOf('|')>-1) return false;
        if (filter.indexOf('!')>-1) return false;

        if (filter.indexOf('=')>0 && (filter.indexOf('=')==filter.lastIndexOf("=")))
            return true;

        return true;
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

        if (entries.containsKey(name) == false)
        {
            LdapName test = new LdapName(name);
            test.remove(test.size() - 1);
            if (entries.containsKey(test.toString()) == false)
                throw new NameNotFoundException("unable to find parent entry: " + test.toString());
        }

        entries.deleteEntry(name);
    }


    /**
     * Binds a new name to the object bound to an old name, and unbinds
     * the old name.  Both names are relative to this context.
     * Any attributes associated with the old name become associated
     * with the new name.
     * Intermediate contexts of the old name are not changed.
     *
     * @param oldName the name of the existing binding; may not be empty
     * @param rdn     the RDN of the new binding; may not be empty
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
            throw new NamingException("cannot perform rename operation '" + rdn + "' is not an RDN \n");

        if (oldName.size()<2)
            throw new NamingException("cannot rename root node in test provider");

        TreeEntry entry = entries.get(oldNameString);

        Rdn myRdn = new Rdn(rdn.get(0));

        entry.put(myRdn.getType(), myRdn.getValue().toString());

        if (entry.getChildren().size() != 0)
            throw new NamingException("renaming non-leaf entries not supported by test provider");

        String newRDN = rdn.toString();

        Name newName = oldName.getPrefix(oldName.size()-1).add(newRDN);

        entries.renameEntry(oldNameString, newName.toString());

        if (entries.get(newName.toString()) == null)
            throw new NamingException("unable to modify test provider directory"); // should never happen

    }

    /*
     *   Methods to uplift from DirContext to LdapContext... none of these are actually implemented
     * (currently class is being used to init a JNDIDataBroker for testing - CB Oct '12)
     */

    @Override
    public ExtendedResponse extendedOperation(ExtendedRequest extendedRequest) throws NamingException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LdapContext newInstance(Control[] controls) throws NamingException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void reconnect(Control[] controls) throws NamingException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Control[] getConnectControls() throws NamingException
    {
        return new Control[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setRequestControls(Control[] controls) throws NamingException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Control[] getRequestControls() throws NamingException
    {
        return new Control[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Control[] getResponseControls() throws NamingException
    {
        return new Control[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}



