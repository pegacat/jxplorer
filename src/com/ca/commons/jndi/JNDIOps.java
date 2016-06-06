package com.ca.commons.jndi;




import com.ca.commons.naming.DXAttribute;
import com.ca.commons.naming.DXAttributes;
import com.ca.commons.naming.DXNamingEnumeration;


import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.Subject;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * <p>JNDI Ops is a bare-bones utility class that takes up some
 * of the over-head of making jndi calls.  It is used by
 * BasicOps which adds validation, error handling and logging.<p>
 * <p/>
 * <p>This utility class assumes you will be using ldap v3 with
 * the environment defaults:
 * "java.naming.ldap.deleteRDN" = "false"
 * Context.REFERRAL (java.naming.referral), "ignore");
 * java.naming.ldap.attributes.binary", "photo jpegphoto jpegPhoto");
 * java.naming.ldap.derefAliases", "finding");
 */

public class JNDIOps
{
    // WARNING: Paging set globally.  If needed per-connection, UI and config will need to be adjusted to allow per-connection configuration.
    public static final int NO_PAGING = -1;
    private static int pageSize = NO_PAGING;  // default page size for paged results.

    private static final String DEFAULT_CTX = "com.sun.jndi.ldap.LdapCtxFactory";


    /**
     * To speed up existance checks, we use a single static constraints object that
     * never changes.
     */
    private SearchControls existanceConstraints;


    /**
     * How to handle ldap referrals if unspecified
     */
    public static final String DEFAULT_REFERRAL_HANDLING = "ignore";

    /**
     * How to handle ldap aliases if unspecified
     */
    public static final String DEFAULT_ALIAS_HANDLING = "finding";


    // we may wish to extend this class to opening DSML connections in future.
    //private static final String DEFAULT_DSML_CTX = "com.sun.jndi.dsmlv2.soap.DsmlSoapCtxFactory";

    private NameParser nameParser;

    //AJR: converted protected member variable into private, added getContext / setContext accessors.
    //NOTE: used Context rather than Ctx to match existing getContext() in BasicOps
    private LdapContext ctx = null;

    private static Logger log = Logger.getLogger(JNDIOps.class.getName());  // ...It's round it's heavy it's wood... It's better than bad, it's good...

    // initialise a reusable static constraints object for fast existance searching in 'exists()' methods
    {
        existanceConstraints = new SearchControls();
        existanceConstraints.setSearchScope(SearchControls.OBJECT_SCOPE);
        existanceConstraints.setCountLimit(0);
        existanceConstraints.setTimeLimit(0);
        existanceConstraints.setReturningAttributes(new String[]{"1.1"});  // just the names Madam
    }

    /**
     * The page size for paged results.  Defaults to '-1' (not used).
     * A positive number flags the use of paged results.
     * @param newSize
     */
    public static void setPageSize(int newSize) {pageSize = newSize;}

    /**
     * Initialise a Basic Operation object with a context.
     */

    public JNDIOps(LdapContext c)
    {
        setContext(c);
    }

    /**
     * This creates a jndi connection with a particular set of environment properties.
     * Often this will be obtained by using one of the set...Properties methods to
     * create a base list of properties and then modifing it before calling this
     * constructor.
     */

    /**
     * This creates a jndi connection with a particular set of environment properties.
     * Often this will be obtained by using one of the set...Properties methods to
     * create a base list of properties and then modifing it before calling this
     * constructor.
     */

    public JNDIOps(Hashtable env) throws NamingException
    {

        // GSSAPI/Kerberos code from Vadim Tarassov
        // TODO: consider refactoring to make this handling
        // similar to other setup... methods?  Maybe in CBOpenConWin?
        if ((env.get(Context.SECURITY_AUTHENTICATION)).equals("GSSAPI"))
        {
            setupKerberosContext(env);
        }
        else
        {
            setContext(openContext(env));                // create the connection!
        }
    }

    /**
     * This creates an ldap context using GSSAPI/Kerberos for
     * security.  It uses cached kerberos credentials, or
     * calls JXCallbackHandler() to obtain credentials if none
     * are already available.
     * @param env
     * @throws NamingException
     */
    protected void setupKerberosContext(Hashtable env)
            throws NamingException
    {
        // debug
        log.finest("dumping kerberos environment keys");
        Enumeration keys = env.keys();
        while (keys.hasMoreElements())
        {
            String key = (String)keys.nextElement();
            log.finest(key + " : " + env.get(key));
        }


        // Create LoginContext

        LoginContext lc = null;
        try
        {
            lc = new LoginContext(JNDIOps.class.getName(),
                    new JXCallbackHandler());
            lc.login();                       
        }
        catch (LoginException ex)
        {
            ex.printStackTrace();
            throw new NamingException("login problem: " + ex);
        }

        LdapContext newCtx = (LdapContext) Subject.doAs(lc.getSubject(), new JndiAction(env));


        if (newCtx == null)
        {
            throw new NamingException("a problem with GSSAPI occurred - couldn't create a GSSAPI directory context");
        } // Vadim: other then GSSAPI

        setContext(newCtx);


        if (ctx == null)
        {
            throw new NamingException("another problem with GSSAPI occurred");   // usually caught by the first error
        } // Vadim: other then GSSAPI
    }

    /**
     * This creates a simple, unauthenticated jndi connection to an ldap url.
     * Note that this ftn may take some time to return...
     *
     * @param url a url of the form ldap://hostname:portnumber.
     */

    public JNDIOps(String url)
            throws NamingException
    {
        Hashtable env = new Hashtable();  // an environment for jndi context parameters

        setupBasicProperties(env, url);       // set up the bare minimum parameters

        setContext(openContext(env));                         // create the connection!
    }

    /**
     * <p>This creates a JNDIOps object using simple username + password authentication.</p>
     * <p/>
     * <p>This constructor opens an initial context.  Note that this ftn may take some
     * time to return...</p>
     *
     * @param url    a url of the form ldap://hostname:portnumber.
     * @param userDN the Manager User's distinguished name (optionally null if not used).
     * @param pwd    the Manager User's password - (is null if user is not manager).
     */

    public JNDIOps(String url, String userDN, char[] pwd)
            throws NamingException
    {

        Hashtable env = new Hashtable();              // an environment for jndi context parameters

        setupBasicProperties(env, url);                   // set up the bare minimum parameters

        setupSimpleSecurityProperties(env, userDN, pwd);  // add the username + password parameters

        setContext(openContext(env));             // create the connection !
    }

    /**
     * This creates a JNDIOps object with an SSL or SASL Connection.
     * <p/>
     * If only SSL is desired, the clientcerts, clientKeystorePwd and clientKeystoreType
     * variables may be set to null.
     *
     * @param url                a url of the form ldap://hostname:portnumber.
     * @param cacerts            the file containing the trusted server certificates (no keys).
     * @param clientcerts        the file containing client certificates.
     * @param caKeystorePwd      the password to the ca's keystore (may be null for non-client authenticated ssl).
     * @param clientKeystorePwd  the password to the client's keystore (may be null for non-client authenticated ssl).
     * @param caKeystoreType     the type of keystore file; e.g. 'JKS', or 'PKCS12'.
     * @param clientKeystoreType the type of keystore file; e.g. 'JKS', or 'PKCS12'.
     * @param tracing            whether to set BER tracing on or not.
     * @param sslTracing         whether to set SSL tracing on or not.
     */

    public JNDIOps(String url,
                   String cacerts, String clientcerts,
                   char[] caKeystorePwd, char[] clientKeystorePwd,
                   String caKeystoreType, String clientKeystoreType,
                   boolean tracing, boolean sslTracing, String sslSocketFactory)
            throws NamingException
    {

        Hashtable env = new Hashtable();  // an environment for jndi context parameters


        setupBasicProperties(env, url, tracing, DEFAULT_REFERRAL_HANDLING, DEFAULT_ALIAS_HANDLING);       // set up the bare minimum parameters

        // add the SSL ('ca...') and possible SASL ('client...') parameters
        setupSSLProperties(env, cacerts, clientcerts,
                caKeystorePwd, clientKeystorePwd,
                caKeystoreType, clientKeystoreType,
                sslTracing, sslSocketFactory);

        setContext(openContext(env));            // create the connection !
    }

    /**
     * @param env
     * @param url
     * @throws NamingException
     */
    public static void setupBasicProperties(Hashtable env, String url)
            throws NamingException
    {
        setupBasicProperties(env, url, false, DEFAULT_REFERRAL_HANDLING, DEFAULT_ALIAS_HANDLING);
    }


    /**
     * This method combines a serverURL (e.g. ldap://localhost:19389) with a base DN to
     * start searching from (e.g. o=democorp,c=au) to create a full ldap URL
     * (e.g. ldap://localhost:19389/o=democorp,c=au.  It does some of the required
     * escaping to the baseDN, but does not formally check either the DN or the
     * serverURL for correctness.
     *
     * see RFC 2252 for more details.
     * @param serverURL
     * @param baseDN
     * @return
     */

/* from RFC 1738 2.2
     Unsafe:

   Characters can be unsafe for a number of reasons.  The space
   character is unsafe because significant spaces may disappear and
   insignificant spaces may be introduced when URLs are transcribed or
   typeset or subjected to the treatment of word-processing programs.
   The characters "<" and ">" are unsafe because they are used as the
   delimiters around URLs in free text; the quote mark (""") is used to
   delimit URLs in some systems.  The character "#" is unsafe and should
   always be encoded because it is used in World Wide Web and in other
   systems to delimit a URL from a fragment/anchor identifier that might
   follow it.  The character "%" is unsafe because it is used for
   encodings of other characters.  Other characters are unsafe because
   gateways and other transport agents are known to sometimes modify
   such characters. These characters are "{", "}", "|", "\", "^", "~",
   "[", "]", and "`".
*/
    /**
     * Make the server URL from the specified server URL and base DN.
     * 
     * @param serverURL the URL of the server
     * @param baseDN the base distinguished name of the directory server
     * 
     * @return the server URL
     */
    public static String makeServerURL(String serverURL, String baseDN)
    {
        if (baseDN != null && baseDN.length() > 0)
        {
            // trim any extra '/' on the end of the server URL (we add it back below)
            if (serverURL.length()>7 && serverURL.endsWith("/"))
                serverURL = serverURL.substring(0, serverURL.length()-1);

            // XXX really important that this one happens first!!
            baseDN = baseDN.replaceAll("[%]", "%25");

            baseDN = baseDN.replaceAll(" ", "%20");
            baseDN = baseDN.replaceAll("[<]", "%3c");
            baseDN = baseDN.replaceAll("[>]", "%3e");
            baseDN = baseDN.replaceAll("[\"]", "%3f");
            baseDN = baseDN.replaceAll("[#]", "%23");
            baseDN = baseDN.replaceAll("[{]", "%7b");
            baseDN = baseDN.replaceAll("[}]", "%7d");
            baseDN = baseDN.replaceAll("[|]", "%7c");
            baseDN = baseDN.replaceAll("[\\\\]", "%5c");         // double check this one :-)
            baseDN = baseDN.replaceAll("[\\^]", "%5e");
            baseDN = baseDN.replaceAll("[~]", "%7e");
            baseDN = baseDN.replaceAll("[\\[]", "%5b");
            baseDN = baseDN.replaceAll("[\\]]", "%5d");
            baseDN = baseDN.replaceAll("[']", "%27");

            baseDN = baseDN.replaceAll("[?]", "%3f");

            serverURL = serverURL + "/" + baseDN;
        }

        return serverURL;
    }

    /**
     * This sets the basic environment properties needed for a simple,
     * unauthenticated jndi connection.  It is used by openBasicContext().
     * <p/>
     * This method is provided as a convenience for people wishing to append
     * or modify the jndi environment, without setting it up entirely from
     * scratch.
     *
     * @param url
     * @param env
     * @throws NamingException
     */


    public static void setupBasicProperties(Hashtable env, String url, boolean tracing, String referralType, String aliasType)
            throws NamingException
    {
        // sanity check
        if (url == null)
            throw new NamingException("URL not specified in openContext()!");

        // set the tracing level now, since (wierdly) it can't be set once the connection is open.
        if (tracing)
            env.put("com.sun.jndi.ldap.trace.ber", System.err);

        env.put("java.naming.ldap.version", "3");               // always use ldap v3

        if (env.get(Context.INITIAL_CONTEXT_FACTORY) == null)
            env.put(Context.INITIAL_CONTEXT_FACTORY, DEFAULT_CTX);  // use jndi provider

        env.put("java.naming.ldap.deleteRDN", "false");         // usually what we want

        env.put(Context.REFERRAL, referralType);                    //could be: follow, ignore, throw

        env.put("java.naming.ldap.attributes.binary", "photo jpegphoto jpegPhoto");  // special hack to handle non-standard binary atts

        env.put("java.naming.ldap.derefAliases", aliasType);    // could be: finding, searching, etc.

        env.put(Context.SECURITY_AUTHENTICATION, "none");       // no authentication (may be modified by other code)

        env.put(Context.PROVIDER_URL, url);                     // the ldap url to connect to; e.g. "ldap://ca.com:389"
    }


    /**
     * This sets the environment properties needed for a simple username +
     * password authenticated jndi connection.  It is used by openSimpleSecurityContext().
     * <p/>
     * This method is provided as a convenience for people wishing to append
     * or modify the jndi environment, without setting it up entirely from
     * scratch.
     *
     * @param env
     * @param userDN
     * @param pwd
     */
    public static void setupSimpleSecurityProperties(Hashtable env, String userDN, char[] pwd)
    {
        env.put(Context.SECURITY_AUTHENTICATION, "simple");         // 'simple' = username + password

        env.put(Context.SECURITY_PRINCIPAL, userDN);                // add the full user dn

        env.put(Context.SECURITY_CREDENTIALS, new String(pwd));     // stupid jndi requires us to cast this to a string-
        // this opens a security weakness with swapped memory etc.
    }

    /**
     * @param env
     * @param cacerts
     * @param clientcerts
     * @param caKeystorePwd
     * @param clientKeystorePwd
     * @param caKeystoreType
     * @param clientKeystoreType
     * @param tracing
     * @param sslTracing
     * @param sslSocketFactory
     * @throws NamingException
     * @deprecated - use the version without the tracing flag (set in setupBasicProperties).
     */
    public static void setupSSLProperties(Hashtable env,
                                          String cacerts, String clientcerts,
                                          char[] caKeystorePwd, char[] clientKeystorePwd,
                                          String caKeystoreType, String clientKeystoreType,
                                          boolean tracing, boolean sslTracing,
                                          String sslSocketFactory)
            throws NamingException
    {
        setupSSLProperties(env, cacerts, clientcerts, caKeystorePwd, clientKeystorePwd, caKeystoreType, clientKeystoreType, sslTracing, sslSocketFactory);
    }

    /*  This static ftn. sets the environment used to open an SSL or SASL context.
    *   It is used by openSSLContext.
    *
    *   If only SSL is desired, the clientcerts, clientKeystorePwd and clientKeystoreType
    * variables may be set to null.
    *
    * This method is provided as a convenience for people wishing to append
    * or modify the jndi environment, without setting it up entirely from
    * scratch.
    *
    *   @param url                a url of the form ldap://hostname:portnumber.
    *   @param tracing            whether to set BER tracing on or not.
    *   @param cacerts            the file containing the trusted server certificates (no keys).
    *   @param clientcerts        the file containing client certificates.
    *   @param caKeystorePwd      the password to the ca's keystore (may be null for non-client authenticated ssl).
    *   @param clientKeystorePwd  the password to the client's keystore (may be null for non-client authenticated ssl).
    *   @param caKeystoreType     the type of keystore file; e.g. 'JKS', or 'PKCS12'.
    *   @param clientKeystoreType the type of keystore file; e.g. 'JKS', or 'PKCS12'.
    *
    *   @return                   The created context.
    */

    public static void setupSSLProperties(Hashtable env,
                                          String cacerts, String clientcerts,
                                          char[] caKeystorePwd, char[] clientKeystorePwd,
                                          String caKeystoreType, String clientKeystoreType,
                                          boolean sslTracing,
                                          String sslSocketFactory)
            throws NamingException
    {

        // sanity check
        if (cacerts == null)
            throw new NamingException("Cannot use SSL without a trusted CA certificates JKS file.");

        // the exact protocol (e.g. "TLS") set in JndiSocketFactory
        env.put(Context.SECURITY_PROTOCOL, "ssl");

        // Initialise the SSL Socket Factory.  Due to architectural wierdnesses, this is
        // a separate, static method in our own separate SSL Factory class.

        if (sslSocketFactory.equals("com.ca.commons.jndi.JndiSocketFactory"))
        {
            JndiSocketFactory.init(cacerts, clientcerts,
                    caKeystorePwd, clientKeystorePwd,
                    caKeystoreType, clientKeystoreType);
        }

        // Tell JNDI to use our own, separate SSL Factory class with the keystores set as previously
        env.put("java.naming.ldap.factory.socket", sslSocketFactory);

        // try to use client authentication (SASL) if a clientcert keystore and pwd supplied
        if (clientcerts != null && (clientKeystorePwd != null && clientKeystorePwd.length > 0))
        {
            env.put(Context.SECURITY_AUTHENTICATION, "EXTERNAL");  // Use sasl external (i.e., certificate) auth
        }

        if (sslTracing)
        {
            // XXX doesn't seem to work?
            System.setProperty("javax.net.debug", "ssl handshake verbose");
        }
    }


    /**
     * This is a raw interface to javax.naming.directory.InitialLdapContext, that allows
     * an arbitrary environment string to be passed through.  Often it will be
     * convenient to create that environment list using a set...Properties call (or just
     * use one of the constructors to create a JNDIOps object.
     *
     * @param env a list of environment variables for the context
     * @return a newly created LdapContext.
     */

    public static LdapContext openContext(Hashtable env)
            throws NamingException
    {
        /* DEBUG code - do not remove
        System.out.println("-- listing properties --");
        for (Enumeration e = env.keys() ; e.hasMoreElements() ;)
        {
            String key = e.nextElement().toString();
            String val = env.get(key).toString();
            if (val.length() > 40) {
                    val = val.substring(0, 37) + "...";
            }
            System.out.println(key + "=" + val);
        }
        System.out.println("-- end list --");
        */


        LdapContext ctx = new InitialLdapContext(env, null);

        if (ctx == null)
            throw new NamingException("Internal Error with jndi connection: No Context was returned, however no exception was reported by jndi.");

        return ctx;
    }


    /**
     * <p>A wrapper for context.rename... changes the
     * distinguished name of an object, checks for error.
     * !! Only changes the final RDN.</p>
     * <p/>
     * <p>WARNING! this will fail for single valued manditory attributes.
     * since using 'deleteRDN = false' - use renameEntry(old, new, deleteOldRdn)
     * method instead - 30 May 2002.</p>
     *
     * @param oldDN current distinguished name of an object.
     * @param newDN the name it is to be changed to.
     */

    public void renameEntry(Name oldDN, Name newDN)
            throws NamingException
    {
        Name rdn = newDN.getSuffix(newDN.size() - 1);
        Name oldRdn = oldDN.getSuffix(oldDN.size() - 1);

        if (oldRdn.toString().equals(rdn.toString()) == false)
            ctx.rename(oldDN, rdn);                            // this should always work
        else
        {
            log.fine("EXPERIMENTAL: ATTEMPTING FULL RENAME: " + oldDN + " to " + newDN);
            ctx.rename(oldDN, newDN);   // this should fail on directories (openldap) unable to do internal node renames
                                        // (unless it's just the leaf node)
        }                               // - but will be caught and handled by AdvancedOps 'recMoveTree()'
    }


    /**
     * Copies an object to a new DN by the simple expedient of adding
     * an object with the new DN, and the attributes of the old object.
     *
     * @param fromDN the original object being copied
     * @param toDN   the new object being created
     */

    public void copyEntry(Name fromDN, Name toDN)
            throws NamingException
    {
        Attributes originalAtts = read(fromDN);

        // if we have different RDNs, stuff around to make sure we have the right attribute values...
        if (!fromDN.get(fromDN.size()-1).equals(toDN.get(toDN.size()-1)))
        {
            try
            {
                        // TODO: clean this up when we convert to LdapName throughout...?
                LdapName to = new LdapName(toDN.toString());
                Rdn rdn = to.getRdn(to.size()-1);
                DXAttributes rdnAtts = new DXAttributes(rdn.toAttributes());

                for (DXAttribute att:rdnAtts)
                {
                    for (Object value:att.getValues())
                    {
                        if (originalAtts.get(att.getID())!=null)
                        {
                            if (!originalAtts.get(att.getID()).contains(value))
                            {
                                Attribute original = originalAtts.get(att.getID());
                                original.add(value);
                            }
                            // else ... we're good, entry already contains rdn naming value
                        }
                        else
                        {
                            originalAtts.put(att);  // new att... toss it in (shouldn't ever happen though? Naming atts are mandatory?)
                        }
                    }
                }
            }
            catch (Exception e)
            {
                log.severe("unexpected error trying to add naming att/val to entry: " + e.getMessage() );
            }
        }

        addEntry(toDN, originalAtts);
    }


    /**
     * creates a new object (subcontext) with the given
     * dn and attributes.
     *
     * @param dn   the distinguished name of the new object
     * @param atts attributes for the new object
     */

    public void addEntry(Name dn, Attributes atts)
            throws NamingException
    {
        ctx.createSubcontext(dn, atts);
    }

    /**
     * deletes a leaf entry (subcontext).  It is
     * an error to attempt to delete an entry which is not a leaf
     * entry, i.e. which has children.
     */

    public void deleteEntry(Name dn)
            throws NamingException
    {
        ctx.destroySubcontext(dn);
    }


    /**
     * Checks the existence of a particular DN, without (necessarily)
     * reading any attributes.
     *
     * @param nodeDN the DN to check
     * @return the existence of the nodeDN (or false if an error occurs).
     */
    //todo: merge 'exists' methods
    public boolean exists(Name nodeDN)
            throws NamingException
    {

        try
        {
            ctx.search(nodeDN, "(objectclass=*)", existanceConstraints);
            return true;
        }
        catch (NoSuchAttributeException e) // well, there has to be an entry for us not to find attributes on right?
        {                                  // so maybe it's an exchange server, or has weird visibility permissions...
            return true;
        }
        catch (NameNotFoundException e)  // ugly as sin, but there seems no other way of doing things
        {
            return false;
        }
                /*
                 *   This is what is known in the programming trade as 'a filthy hack'.  There is a bug in the Sun DSML provider
                 *   where a null pointer exception is thrown at com.sun.jndi.dsmlv2.soap.DsmlSoapCtx.c_lookup(DsmlSoapCtx.java:571)
                 *   (possibly when a referral is returned?).  For our purposes though it translates to 'not found' - so we intercept
                 *   the bug and return false instead...
                 */
        catch (NullPointerException e)
        {
            if ((ctx != null) && (ctx.getEnvironment().get(Context.INITIAL_CONTEXT_FACTORY).toString().indexOf("dsml") > 0))
                return false;
            else
                throw e;
        }
    }


    /**
     * Checks the existence of a particular DN, without (necessarily)
     * reading any attributes.
     *
     * @param nodeDN the DN to check
     * @return the existence of the nodeDN (or false if an error occurs).
     */

    public boolean exists(String nodeDN)
            throws NamingException
    {
        try
        {
            ctx.search(nodeDN, "(objectclass=*)", existanceConstraints);
            return true;
        }
        catch (NameNotFoundException e)  // ugly as sin, but there seems no other way of doing things
        {
            return false;
        }
                /*
                 *   This is what is known in the programming trade as 'a filthy hack'.  There is a bug in the Sun DSML provider
                 *   where a null pointer exception is thrown at com.sun.jndi.dsmlv2.soap.DsmlSoapCtx.c_lookup(DsmlSoapCtx.java:571)
                 *   (possibly when a referral is returned?).  For our purposes though it translates to 'not found' - so we intercept
                 *   the bug and return false instead...
                 */
        catch (NullPointerException e)
        {
            if ((ctx != null) && (ctx.getEnvironment().get(Context.INITIAL_CONTEXT_FACTORY).toString().indexOf("dsml") > 0))
                return false;
            else
                throw e;
        }
    }

    /**
     * Reads all the attribute type and values for the given entry.
     *
     * @param dn the ldap string distinguished name of entry to be read
     * @return an 'Attributes' object containing a list of all Attribute
     *         objects.
     */

    public synchronized Attributes read(Name dn)
            throws NamingException
    {
        return read(dn, null);
    }


    /**
     * Reads all the attribute type and values for the given entry.
     *
     * @param dn               the ldap string distinguished name of entry to be read
     * @param returnAttributes a list of specific attributes to return.
     * @return an 'Attributes' object containing a list of all Attribute
     *         objects.
     */

    public synchronized Attributes read(Name dn, String[] returnAttributes)
            throws NamingException
    {
        Attributes atts = ctx.getAttributes(dn, returnAttributes);

        return atts;
    }

    /**
     * Modifies an object's attributes, either adding, replacing or
     * deleting the passed attributes.
     *
     * @param dn       distinguished name of object to modify
     * @param mod_type the modification type to be performed; one of
     *                 LdapContext.REPLACE_ATTRIBUTE, LdapContext.DELETE_ATTRIBUTE, or
     *                 LdapContext.ADD_ATTRIBUTE.
     * @param attr     the new attributes to update the object with.
     */

    public void modifyAttributes(Name dn, int mod_type, Attributes attr)
            throws NamingException
    {
        ctx.modifyAttributes(dn, mod_type, attr);
    }


    /**
     * Modifies an object's attributes, either adding, replacing or
     * deleting the passed attributes.
     *
     * @param dn      distinguished name of object to modify
     * @param modList a list of ModificationItems
     */

    public void modifyAttributes(Name dn, ModificationItem[] modList)
            throws NamingException
    {
        ctx.modifyAttributes(dn, modList);
    }

    /**
     * Updates an object with a new set of attributes
     *
     * @param dn   distinguished name of object to update
     * @param atts the new attributes to update the object with.
     */

    public void updateEntry(Name dn, Attributes atts)
            throws NamingException
    {
        modifyAttributes(dn, LdapContext.REPLACE_ATTRIBUTE, atts);
    }


    /**
     * deletes an attribute from an object
     *
     * @param dn distinguished name of object
     * @param a  the attribute to delete
     */

    public void deleteAttribute(Name dn, Attribute a)
            throws NamingException
    {
        BasicAttributes atts = new BasicAttributes();
        atts.put(a);
        modifyAttributes(dn, LdapContext.REMOVE_ATTRIBUTE, atts);
    }

    /**
     * deletes a set of attribute-s from an object
     *
     * @param dn distinguished name of object
     * @param a  the Attributes object containing the
     *           list of attribute-s to delete
     */


    public void deleteAttributes(Name dn, Attributes a)
            throws NamingException
    {
        modifyAttributes(dn, LdapContext.REMOVE_ATTRIBUTE, a);
    }

    /**
     * updates an Attribute with a new value set
     *
     * @param dn distinguished name of object
     * @param a  the attribute to modify
     */

    public void updateAttribute(Name dn, Attribute a)
            throws NamingException
    {
        BasicAttributes atts = new BasicAttributes();
        atts.put(a);
        modifyAttributes(dn, LdapContext.REPLACE_ATTRIBUTE, atts);
    }

    /**
     * updates a set of Attribute-s.
     *
     * @param dn distinguished name of object
     * @param a  an Attributes object containing the attribute-s to modify
     */

    public void updateAttributes(Name dn, Attributes a)
            throws NamingException
    {
        modifyAttributes(dn, LdapContext.REPLACE_ATTRIBUTE, a);
    }

    /**
     * Adds a new attribute to a particular dn.
     *
     * @param dn distinguished name of object
     * @param a  the attribute to modify
     */

    public void addAttribute(Name dn, Attribute a)
            throws NamingException
    {
        BasicAttributes atts = new BasicAttributes();
        atts.put(a);
        modifyAttributes(dn, LdapContext.ADD_ATTRIBUTE, atts);
    }

    /**
     * Adds a set of attributes to a particular dn.
     *
     * @param dn distinguished name of object
     * @param a  the Attributes (set of attribute-s) to add
     */

    public void addAttributes(Name dn, Attributes a)
            throws NamingException
    {
        modifyAttributes(dn, LdapContext.ADD_ATTRIBUTE, a);
    }


    /**
     * returns the next level of a directory tree, returning
     * a Enumeration of the results, *relative* to the SearchBase (i.e. not as
     * absolute DNs), along with their object classes if possible.
     *
     * @param Searchbase the node in the tree to expand
     * @return list of results (NameClassPair); the next layer of the tree...
     */

    public NamingEnumeration list(Name Searchbase)
            throws NamingException
    {
        //    Attempt to read the names of the next level of subentries along with their object
        //    classes.  Failing that, try to just read their names.

// just do a real 'list', without object classes...
//        return rawSearchOneLevel(Searchbase, "(objectclass=*)", 0, 0, new String[]{"1.1"});

// a JXplorer 'list' returns object classes as well, so we can play silly buggers with GUI icons
        return rawSearchOneLevel(Searchbase, "(objectclass=*)", 0, 0, new String[]{"objectclass"});
    }


    /**
     * Performs a one-level directory search (i.e. a search of immediate children), returning
     * object classes if possible, otherwise just the names.
     *
     * @param searchbase the domain name (relative to initial context in ldap) to seach from.
     * @param filter     the non-null filter to use for the search
     * @param limit      the maximum number of results to return
     * @param timeout    the maximum time to wait before abandoning the search
     * @return list of search results ('SearchResult's); entries matching the search filter.
     */

    public NamingEnumeration searchOneLevel(String searchbase, String filter, int limit, int timeout)
            throws NamingException
    {
        return searchOneLevel(searchbase, filter, limit, timeout, new String[]{"1.1"});
    }

    /**
     * Performs a one-level directory search (i.e. a search of immediate children)
     *
     * @param searchbase       the domain name (relative to initial context in ldap) to seach from.
     * @param filter           the non-null filter to use for the search
     * @param limit            the maximum number of results to return
     * @param timeout          the maximum time to wait before abandoning the search
     * @param returnAttributes an array of strings containing the names of attributes to search. (null = all, empty array = none)
     * @return list of search results ('SearchResult's); entries matching the search filter.
     */


    public NamingEnumeration searchOneLevel(String searchbase, String filter, int limit,
                                            int timeout, String[] returnAttributes)
            throws NamingException
    {
        return rawSearchOneLevel(nameParser.parse(searchbase), filter, limit, timeout, returnAttributes);
    }


    /**
     * Performs a one-level directory search (i.e. a search of immediate children), returning
     * object classes if possible, otherwise just the names.
     *
     * @param searchbase the domain name (relative to initial context in ldap) to seach from.
     * @param filter     the non-null filter to use for the search
     * @param limit      the maximum number of results to return
     * @param timeout    the maximum time to wait before abandoning the search
     * @return list of search results ('SearchResult's); entries matching the search filter.
     */

    public NamingEnumeration searchOneLevel(Name searchbase, String filter, int limit, int timeout)
            throws NamingException
    {
        return rawSearchOneLevel(searchbase, filter, limit, timeout, new String[]{"1.1"});
    }

    /**
     * Performs a one-level directory search (i.e. a search of immediate children)
     *
     * @param searchbase       the domain name (relative to initial context in ldap) to seach from.
     * @param filter           the non-null filter to use for the search
     * @param limit            the maximum number of results to return
     * @param timeout          the maximum time to wait before abandoning the search
     * @param returnAttributes an array of strings containing the names of attributes to search. (null = all, empty array = none)
     * @return list of search results ('SearchResult's); entries matching the search filter.
     */


    public NamingEnumeration searchOneLevel(Name searchbase, String filter, int limit,
                                            int timeout, String[] returnAttributes)
            throws NamingException
    {
        return rawSearchOneLevel(searchbase, filter, limit, timeout, returnAttributes);
    }

    /**
     * Method that calls the actual search on the jndi context.
     *
     * @param searchbase       the domain name (relative to initial context in ldap) to seach from.
     * @param filter           the non-null filter to use for the search
     * @param limit            the maximum number of results to return
     * @param timeout          the maximum time to wait before abandoning the search
     * @param returnAttributes an array of strings containing the names of attributes to search. (null = all, empty array = none)
     * @return the search result
     * 
     * @throws NamingException if unable to search
     */
    protected NamingEnumeration rawSearchOneLevel(Name searchbase, String filter, int limit,
                                                  int timeout, String[] returnAttributes) throws NamingException
    {
        return rawSearch(searchbase, filter, limit, timeout, returnAttributes, SearchControls.ONELEVEL_SCOPE);
    }

    /**
     * Performs a directory sub tree search (i.e. of the next level and all subsequent levels below),
     * returning just dns);
     *
     * @param searchbase the domain name (relative to initial context in ldap) to seach from.
     * @param filter     the non-null filter to use for the search
     * @param limit      the maximum number of results to return
     * @param timeout    the maximum time to wait before abandoning the search
     * @return list of search results ('SearchResult's); entries matching the search filter. WARNING - these may be RELATIVE to the seachbase.
     */
    public NamingEnumeration searchSubTree(Name searchbase, String filter, int limit, int timeout)
            throws NamingException
    {
        return searchSubTree(searchbase, filter, limit, timeout, new String[]{"1.1"});
    }

    /**
     * Performs a directory sub tree search (i.e. of the next level and all subsequent levels below),
     * returning just dns);
     *
     * @param searchbase the domain name (relative to initial context in ldap) to seach from.
     * @param filter     the non-null filter to use for the search
     * @param limit      the maximum number of results to return
     * @param timeout    the maximum time to wait before abandoning the search
     * @return list of search results ('SearchResult's); entries matching the search filter. WARNING - these may be RELATIVE to the seachbase.
     */
    public NamingEnumeration searchSubTree(String searchbase, String filter, int limit, int timeout)
            throws NamingException
    {
        return searchSubTree((searchbase), filter, limit, timeout, new String[]{"1.1"});
    }

    /**
     * Performs a directory sub tree search (i.e. of the next level and all subsequent levels below).
     *
     * @param searchbase       the domain name (relative to initial context in ldap) to seach from.
     * @param filter           the non-null filter to use for the search
     * @param limit            the maximum number of results to return
     * @param timeout          the maximum time to wait before abandoning the search
     * @param returnAttributes an array of strings containing the names of attributes to search. (null = all, empty array = none)
     * @return list of search results ('SearchResult's); entries matching the search filter. WARNING - these may be RELATIVE to the seachbase.
     */

    public NamingEnumeration searchSubTree(String searchbase, String filter, int limit,
                                           int timeout, String[] returnAttributes)
            throws NamingException
    {
        return rawSearchSubTree(nameParser.parse(searchbase), filter, limit, timeout, returnAttributes);

//         SearchControls constraints = setSubTreeSearchControls(returnAttributes, limit, timeout);

//         return ctx.search(searchbase, filter, constraints);
    }

    /**
     * Performs a directory sub tree search (i.e. of the next level and all subsequent levels below).
     *
     * @param searchbase       the domain name (relative to initial context in ldap) to seach from.
     * @param filter           the non-null filter to use for the search
     * @param limit            the maximum number of results to return
     * @param timeout          the maximum time to wait before abandoning the search
     * @param returnAttributes an array of strings containing the names of attributes to search. (null = all, empty array = none)
     * @return list of search results ('SearchResult's); entries matching the search filter. WARNING - these may be RELATIVE to the seachbase.
     */

    public NamingEnumeration searchSubTree(Name searchbase, String filter, int limit,
                                           int timeout, String[] returnAttributes)
            throws NamingException
    {
        return rawSearchSubTree(searchbase, filter, limit, timeout, returnAttributes);
    }

    protected NamingEnumeration rawSearchSubTree(Name searchbase, String filter, int limit,
                                                 int timeout, String[] returnAttributes) throws NamingException
    {
        if (returnAttributes != null && returnAttributes.length == 0)
            returnAttributes = new String[]{"objectClass"};

        return rawSearch(searchbase, filter, limit, timeout, returnAttributes, SearchControls.SUBTREE_SCOPE);

        /*
        // specify search constraints to search subtree
        SearchControls constraints1 = new SearchControls();

        constraints1.setSearchScope(SearchControls.SUBTREE_SCOPE);
        constraints1.setCountLimit(limit);
        constraints1.setTimeLimit(timeout);

        constraints1.setReturningAttributes(returnAttributes);
        SearchControls constraints = constraints1;

        return ctx.search(searchbase, filter, constraints);
        */
    }


    /**
     * Performs a base object search (i.e. just a search of the current entry, nothing below it),
     * returning no attributes (i.e. just DNs);
     *
     * @param searchbase the domain name (relative to initial context in ldap) to seach from.
     * @param filter     the non-null filter to use for the search
     * @param limit      the maximum number of results to return
     * @param timeout    the maximum time to wait before abandoning the search
     * @return list of search results ('SearchResult's); entries matching the search filter.
     */

    public NamingEnumeration searchBaseEntry(Name searchbase, String filter, int limit, int timeout)
            throws NamingException
    {
        return rawSearchBaseEntry(searchbase, filter, limit, timeout, new String[]{"objectClass"});
    }


    /**
     * Performs a base object search (i.e. just a search of the current entry, nothing below it).
     *
     * @param searchbase       the domain name (relative to initial context in ldap) to seach from.
     * @param filter           the non-null filter to use for the search
     * @param limit            the maximum number of results to return
     * @param timeout          the maximum time to wait before abandoning the search
     * @param returnAttributes an array of strings containing the names of attributes to search. (null = all, empty array = none)
     * @return list of search results ('SearchResult's); entries matching the search filter.
     */

    public NamingEnumeration searchBaseEntry(Name searchbase, String filter, int limit,
                                             int timeout, String[] returnAttributes)
            throws NamingException
    {
        return rawSearchBaseEntry(searchbase, filter, limit, timeout, returnAttributes);
    }

    /**
     * This is the core method for all base entry searches.
     *
     * @param searchbase       the domain name (relative to initial context in ldap) to seach from.
     * @param filter           the non-null filter to use for the search
     * @param limit            the maximum number of results to return
     * @param timeout          the maximum time to wait before abandoning the search
     * @param returnAttributes an array of strings containing the names of attributes to search. (null = all, empty array = none)
     * @return list of search results ('SearchResult's); entries matching the search filter.
     */

    protected NamingEnumeration rawSearchBaseEntry(Name searchbase, String filter, int limit,
                                                   int timeout, String[] returnAttributes)
            throws NamingException
    {
        //NamingEnumeration result = null;

        if (returnAttributes != null && returnAttributes.length == 0)
            returnAttributes = new String[]{"objectClass"};

        return rawSearch(searchbase, filter, limit, timeout, returnAttributes, SearchControls.OBJECT_SCOPE);
        /*
        // specify search constraints to search subtree
        SearchControls constraints = new SearchControls();

        constraints.setSearchScope(SearchControls.OBJECT_SCOPE);
        constraints.setCountLimit(limit);
        constraints.setTimeLimit(timeout);

        constraints.setReturningAttributes(returnAttributes);

        result = ctx.search(searchbase, filter, constraints);

        return result;
        */
    }

    /**
     * This is the underlying method for all searches.
     *
     * @param searchbase       the domain name (relative to initial context in ldap) to seach from.
     * @param filter           the non-null filter to use for the search
     * @param limit            the maximum number of results to return
     * @param timeout          the maximum time to wait before abandoning the search
     * @param returnAttributes an array of strings containing the names of attributes to search. (null = all, empty array = none)
     * @param scope
     * @return list of search results ('SearchResult's); entries matching the search filter.
     */
    protected NamingEnumeration rawSearch(Name searchbase, String filter, int limit, int timeout,
                                          String[] returnAttributes, int scope)
            throws NamingException
    {

        SearchControls constraints = new SearchControls();

        constraints.setSearchScope(scope);
        constraints.setCountLimit(limit);
        constraints.setTimeLimit(timeout);

        constraints.setReturningAttributes(returnAttributes);

        try
        {
            if (pageSize>=0) // do stuff...
            {
                byte[] cookie;
                try
                {
                    ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize, Control.NONCRITICAL)});
                    // do paged requests loop
                    DXNamingEnumeration result = new DXNamingEnumeration();
                    do
                    {   //TODO: consider adding type safety througout API; e.g. insert <SearchResult> here...
                        NamingEnumeration pageResult = ctx.search(searchbase, filter, constraints);

                        while (pageResult.hasMoreElements())
                        {
                            result.add(pageResult.next());
                        }

                        // Examine the paged results control response
                        cookie = getPagingCookie(ctx.getResponseControls());
                        // Re-activate paged results
                        ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize, cookie, Control.CRITICAL)});
                        log.fine ("*** PAGING TOTAL SO FAR: " + result.size());
                    }
                    while (cookie != null);

                    ctx.setRequestControls(null); // clear the context
                    return result;
                }
                catch (IOException e)
                {
                    ctx.setRequestControls(null); // clear the context
                    throw new NamingException("unexpected error creating page request controls: " + e.getMessage());
                }
            }
            else
            {
                NamingEnumeration result = ctx.search(searchbase, filter, constraints);
                return result;
            }
        }
        catch (NamingException e)
        {
            log.warning("error in rawSearch with filter: " + filter + " from base: " + searchbase + " => " + e.getMessage());
            throw e;
        }
    }


    /**
     * This checks to see if the response controls include a paged response control.  If
     * it does, we set a cookie to allow us to continue searching to the next page.
     *
     * @param controls
     * @return
     */
    private byte[] getPagingCookie(Control[] controls)
    {
        if (controls != null)
        {
            byte[] cookie;
            for (int i = 0; i < controls.length; i++)
            {
                if (controls[i] instanceof PagedResultsResponseControl)
                {
                    PagedResultsResponseControl pagedResponse = (PagedResultsResponseControl) controls[i];

                    //TODO Remove debug code
                    int total = pagedResponse.getResultSize();
                    if (total != 0)
                    {
                            log.fine("***************** END-OF-PAGE (read : " + total + ") *****************\n");
                    }
                    else
                    {
                            log.fine("***************** END-OF-PAGE (total: unknown) ***************\n");
                    }

                    cookie = pagedResponse.getCookie();
                    return cookie;
                }
            }
        }
        else
        {
            log.fine("No paged result control was sent from the server");
        }
        return null;
    }






    /**
     * Performs a base object search (i.e. just a search of the current entry, nothing below it),
     * returning no attributes (i.e. just DNs);
     *
     * @param searchbase the domain name (relative to initial context in ldap) to seach from.
     * @param filter     the non-null filter to use for the search
     * @param limit      the maximum number of results to return
     * @param timeout    the maximum time to wait before abandoning the search
     * @return list of search results ('SearchResult's); entries matching the search filter.
     */

    public NamingEnumeration searchBaseEntry(String searchbase, String filter, int limit, int timeout)
            throws NamingException
    {
        return rawSearchBaseEntry(nameParser.parse(searchbase), filter, limit, timeout, new String[]{"objectClass"});
    }


    /**
     * Performs a base object search (i.e. just a search of the current entry, nothing below it).
     *
     * @param searchbase       the domain name (relative to initial context in ldap) to seach from.
     * @param filter           the non-null filter to use for the search
     * @param limit            the maximum number of results to return
     * @param timeout          the maximum time to wait before abandoning the search
     * @param returnAttributes an array of strings containing the names of attributes to search. (null = all, empty array = none)
     * @return list of search results ('SearchResult's); entries matching the search filter.
     */

    public NamingEnumeration searchBaseEntry(String searchbase, String filter, int limit,
                                             int timeout, String[] returnAttributes)
            throws NamingException
    {
        return rawSearchBaseEntry(nameParser.parse(searchbase), filter, limit, timeout, returnAttributes);

    }


    /**
     * This method allows an object to be renamed, while also specifying
     * the exact fate of the old name.
     *
     * @param OldDN        the original name to be changed
     * @param NewDN        the new name
     * @param deleteOldRDN whether the rdn of the old name should be removed,
     *                     or retained as a second attribute value.
     */

    public void renameEntry(Name OldDN, Name NewDN, boolean deleteOldRDN)
            throws NamingException
    {
        String value = (deleteOldRDN) ? "true" : "false";
        try
        {
            ctx.addToEnvironment("java.naming.ldap.deleteRDN", value);

            renameEntry(OldDN, NewDN);

            ctx.addToEnvironment("java.naming.ldap.deleteRDN", "false");  // reset to default of 'false' afterwards.
        }
        catch (NamingException e)
        {
            ctx.addToEnvironment("java.naming.ldap.deleteRDN", "false");  // reset to default of 'false' afterwards.
            throw e;
        }
    }





// *********************************


    /**
     * <p>A wrapper for context.rename... changes the
     * distinguished name of an object.</p>
     * <p/>
     * <p>WARNING! this will fail for single valued manditory attributes.
     * since using 'deleteRDN = false' - use renameEntry(old, new, deleteOldRdn)
     * method instead - 30 May 2002.</p>
     *
     * @param oldDN current distinguished name of an object.
     * @param newDN the name it is to be changed to.
     */

    public void renameEntry(String oldDN, String newDN)
            throws NamingException
    {
        ctx.rename(oldDN, newDN);
    }


    /**
     * Copies an object to a new DN by the simple expedient of adding
     * an object with the new DN, and the attributes of the old object.
     *
     * @param fromDN the original object being copied
     * @param toDN   the new object being created
     */

    public void copyEntry(String fromDN, String toDN)
            throws NamingException
    {
        addEntry(toDN, read(fromDN));
    }


    /**
     * creates a new object (subcontext) with the given
     * dn and attributes.
     *
     * @param dn   the distinguished name of the new object
     * @param atts attributes for the new object
     */

    public void addEntry(String dn, Attributes atts)
            throws NamingException
    {
        ctx.createSubcontext(dn, atts);
    }

    /**
     * deletes a leaf entry (subcontext).  It is
     * an error to attempt to delete an entry which is not a leaf
     * entry, i.e. which has children.
     */

    public void deleteEntry(String dn)
            throws NamingException
    {
        ctx.destroySubcontext(dn);
    }


    /**
     * Reads all the attribute type and values for the given entry.
     *
     * @param dn the ldap string distinguished name of entry to be read
     * @return an 'Attributes' object containing a list of all Attribute
     *         objects.
     */

    public synchronized Attributes read(String dn)
            throws NamingException
    {
        return read(dn, null);
    }


    /**
     * Reads all the attribute type and values for the given entry.
     *
     * @param dn               the ldap string distinguished name of entry to be read
     * @param returnAttributes a list of specific attributes to return.
     * @return an 'Attributes' object containing a list of all Attribute
     *         objects.
     */

    public synchronized Attributes read(String dn, String[] returnAttributes)
            throws NamingException
    {
        return ctx.getAttributes(dn, returnAttributes);
    }

    /**
     * Modifies an object's attributes, either adding, replacing or
     * deleting the passed attributes.
     *
     * @param dn       distinguished name of object to modify
     * @param mod_type the modification type to be performed; one of
     *                 LdapContext.REPLACE_ATTRIBUTE, LdapContext.DELETE_ATTRIBUTE, or
     *                 LdapContext.ADD_ATTRIBUTE.
     * @param attr     the new attributes to update the object with.
     */

    public void modifyAttributes(String dn, int mod_type, Attributes attr)
            throws NamingException
    {
        ctx.modifyAttributes(dn, mod_type, attr);
    }


    /**
     * Modifies an object's attributes, either adding, replacing or
     * deleting the passed attributes.
     *
     * @param dn      distinguished name of object to modify
     * @param modList a list of ModificationItems
     */

    public void modifyAttributes(String dn, ModificationItem[] modList)
            throws NamingException
    {
        ctx.modifyAttributes(dn, modList);
    }

    /**
     * Updates an object with a new set of attributes
     *
     * @param dn   distinguished name of object to update
     * @param atts the new attributes to update the object with.
     */

    public void updateEntry(String dn, Attributes atts)
            throws NamingException
    {
        modifyAttributes(dn, LdapContext.REPLACE_ATTRIBUTE, atts);
    }


    /**
     * deletes an attribute from an object
     *
     * @param dn distinguished name of object
     * @param a  the attribute to delete
     */

    public void deleteAttribute(String dn, Attribute a)
            throws NamingException
    {
        BasicAttributes atts = new BasicAttributes();
        atts.put(a);
        modifyAttributes(dn, LdapContext.REMOVE_ATTRIBUTE, atts);
    }

    /**
     * deletes a set of attribute-s from an object
     *
     * @param dn distinguished name of object
     * @param a  the Attributes object containing the
     *           list of attribute-s to delete
     */


    public void deleteAttributes(String dn, Attributes a)
            throws NamingException
    {
        modifyAttributes(dn, LdapContext.REMOVE_ATTRIBUTE, a);
    }

    /**
     * updates an Attribute with a new value set
     *
     * @param dn distinguished name of object
     * @param a  the attribute to modify
     */

    public void updateAttribute(String dn, Attribute a)
            throws NamingException
    {
        BasicAttributes atts = new BasicAttributes();
        atts.put(a);
        modifyAttributes(dn, LdapContext.REPLACE_ATTRIBUTE, atts);
    }

    /**
     * updates a set of Attribute-s.
     *
     * @param dn distinguished name of object
     * @param a  an Attributes object containing the attribute-s to modify
     */

    public void updateAttributes(String dn, Attributes a)
            throws NamingException
    {
        modifyAttributes(dn, LdapContext.REPLACE_ATTRIBUTE, a);
    }

    /**
     * Adds a new attribute to a particular dn.
     *
     * @param dn distinguished name of object
     * @param a  the attribute to modify
     */

    public void addAttribute(String dn, Attribute a)
            throws NamingException
    {
        BasicAttributes atts = new BasicAttributes();
        atts.put(a);
        modifyAttributes(dn, LdapContext.ADD_ATTRIBUTE, atts);
    }

    /**
     * Adds a set of attributes to a particular dn.
     *
     * @param dn distinguished name of object
     * @param a  the Attributes (set of attribute-s) to add
     */

    public void addAttributes(String dn, Attributes a)
            throws NamingException
    {
        modifyAttributes(dn, LdapContext.ADD_ATTRIBUTE, a);
    }


    /**
     * returns the next level of a directory tree, returning
     * a Enumeration of the results, *relative* to the SearchBase (i.e. not as
     * absolute DNs), along with their object classes if possible.
     * <p/>
     * <p>WARNING - due to jndi wierdness, sometimes the entries are *not* relative, but are full DNs.</p>
     *
     * @param searchbase the node in the tree to expand
     * @return list of results (NameClassPair); the next layer of the tree...
     */

    public NamingEnumeration list(String searchbase)
            throws NamingException
    {
        //    Attempt to read the names of the next level of subentries along with their object
        //    classes.  Failing that, try to just read their names.

        return rawSearchOneLevel(nameParser.parse(searchbase), "(objectclass=*)", 0, 0, new String[]{"1.1"});
    }


    /**
     * Shuts down the current context.<p>
     * nb. It is not an error to call this method multiple times.
     */

    public void close()
            throws NamingException
    {
        if (ctx == null) return;  // it is not an error to multiply disconnect.
        nameParser = null;
        ctx.close();
        //TODO: decide if we should set ctx to null at this point
    }

    /**
     * This method allows an object to be renamed, while also specifying
     * the exact fate of the old name.
     *
     * @param OldDN        the original name to be changed
     * @param NewDN        the new name
     * @param deleteOldRDN whether the rdn of the old name should be removed,
     *                     or retained as a second attribute value.
     */

    public void renameEntry(String OldDN, String NewDN, boolean deleteOldRDN)
            throws NamingException
    {
        String value = (deleteOldRDN) ? "true" : "false";
        try
        {
            ctx.addToEnvironment("java.naming.ldap.deleteRDN", value);

            renameEntry(OldDN, NewDN);

            ctx.addToEnvironment("java.naming.ldap.deleteRDN", "false");  // reset to default of 'false' afterwards.
        }
        catch (NamingException e)
        {
            ctx.addToEnvironment("java.naming.ldap.deleteRDN", "false");  // reset to default of 'false' afterwards.
            throw e;  // rethrow exception...
        }
    }

    //	-----------------------------------------------------------------------
    //	Name related magic - put here for now, but probably should go elsewhere

    private static Properties nameParserSyntax = null;

    /**
     * setupLDAPSyntax
     * <p/>
     * Set up the syntax rules for parsing LDAP DNs when creating Name objects.
     */
    private static void setupLDAPSyntax()
    {
        nameParserSyntax = new Properties();

        nameParserSyntax.put("jndi.syntax.direction", "right_to_left");
        nameParserSyntax.put("jndi.syntax.separator", ",");
        nameParserSyntax.put("jndi.syntax.escape", "\\");

        // Not currently used, as the parser seems to preferentially quote rather than use escape chars.  May be an issue with LDAPv2
        // quoted RDNs.
        // 		nameParserSyntax.put("jndi.syntax.beginquote", "\"")
        // 		nameParserSyntax.put("jndi.syntax.endquote", "\"");

        nameParserSyntax.put("jndi.syntax.trimblanks", "true");
        nameParserSyntax.put("jndi.syntax.separator.typeval", "=");
    }

    /**
     * getNameFromString
     * <p/>
     * Convert DN String into JNDI Name,
     *
     * @param iDN DN in String.
     * @return	the resulting name
     */
    //TODO: decide if this method should be static or not - should use the syntax of the current connection, after all
    public static Name getNameFromString(String iDN)
            throws NamingException
    {
        // iDN is assumed to either:
        // a) contain an LDAP DN, without either server/port information or
        // namespace identifier ('ldap://').
        // or:
        // b) Contain a full URL ('ldap://server:port/o=...').

        // Parse it and return it.

        String DN = iDN;
        Name CompositeFormDN = null;
        CompoundName CompoundFormDN = null;

        if (iDN.indexOf("ldap://") != -1)
        {
            // iDN contains the string 'ldap://', and therefore has
            // at least 2 name spaces.  Instantiate a Composite name
            // object and strip off the name we want.
            CompositeFormDN = new CompositeName(iDN);
            if (CompositeFormDN.size() != 0)
                DN = CompositeFormDN.get(CompositeFormDN.size() - 1);
        }

        if (nameParserSyntax == null)
            setupLDAPSyntax();

        CompoundFormDN = new CompoundName(DN, nameParserSyntax);

        return CompoundFormDN;
    }

    /**
     * getNameFromSearchResult
     * <p/>
     * Given a SearchResult object and Base DN, work out the complete DN of the entry, parse it into a Name object and return it.
     *
     * @param iDirectoryEntry JNDI SearchResult object containing a Directory entry.
     * @param iBaseDN         Name object with the Base DN used for the search (may be empty).
     * @return Name object containing the complete DN of the entry.
     */
    //TODO: decide if this method should be static or not - should use the syntax of the current connection, after all
    public static Name getNameFromSearchResult(SearchResult iDirectoryEntry, Name iBaseDN)
            throws InvalidNameException, NamingException
    {
        // Get RDN from a string.  Parse it and if required add the base DN to it, and
        // then return it as a JNDI Name object.
        // Tim Bentley
        // 20010404

        // Take care of the JNDI trailing whitespace problem:
        String RDN = applyJNDIRDNBugWorkAround(iDirectoryEntry.getName());

        Name JNDIRDN = getNameFromString(RDN);

        if (JNDIRDN != null)
		{	// if the name is relative, insert the base DN
            if (iDirectoryEntry.isRelative())
                JNDIRDN.addAll(0, iBaseDN);
		}
		else
			JNDIRDN = (Name) iBaseDN.clone();				// if the RDN is null, use the base DN

        return JNDIRDN;
    }

    /**
     * applyJNDIRDNBugWorkAround
     * <p/>
     * Cope with escaping bug in JNDI RDN handling.
     *
     * @param iRDN String containing RDN to check escaping on.
     * @return String containing correctly escaped RDN.
     */
    private static String applyJNDIRDNBugWorkAround(String iRDN)
    {

        // Tim Bentley
        // 20010328
        // JNDI's SearchResult.getName() removes any trailing space character from the
        // RDN without also removing the LDAP escaping character ('\') - in fact it
        // then escapes the '\' character, resulting in '\\' at the end of the RDN.
        // Parse the passed in RDN and if the last two chars are '\'s, remove
        // them.

//		int 			SlashPos = iRDN.indexOf("\\");
        int SlashPos = iRDN.lastIndexOf("\\\\");	// AJR: need LAST occurrence, and need to escape backslashes
        String ReturnString;

        if (SlashPos == iRDN.length() - 2)
            ReturnString = iRDN.substring(0, SlashPos);
        else
            ReturnString = iRDN;

        return ReturnString;
    }


    public LdapContext getContext()
    {
        return ctx;
    }

    public void setContext(LdapContext ctx)
    {
        this.ctx = ctx;

        try
        {
            nameParser = ctx.getNameParser("");
        }
        catch (NamingException e)
        {
            // TODO: add logging to this class :-)
            System.out.println("Error initialising name parser " + e);
        }
    }


}
