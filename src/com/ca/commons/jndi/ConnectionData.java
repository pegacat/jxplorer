package com.ca.commons.jndi;

import javax.naming.NamingException;
import javax.naming.Context;
import java.util.*;

/**
 * The ConnectionData inner class is used to pass
 * connection data around.  Not all fields are
 * guaranteed to be valid values.
 */

public class ConnectionData
{
    /**
     * The base to start browsing from, e.g.'o=Democorp,c=au'.
     * (This is often reset to what the directory says the base
     * is in practice).
     */

    public String baseDN = "";

    /**
     * The LDAP Version (2 or 3) being used.
     */

    public int version = 3;  // default to 3...

    /**
     * Which protocol to use (currently "ldap", "dsml")
     */

    public static final String LDAP = "ldap";
    public static final String DSML = "dsml";
    public String protocol = LDAP;  // default is always to use LDAP

    /**
     * A URL of the form ldap://hostname:portnumber.
     */

    public String url;

    /**
     * The Manager User's distinguished name (optionally null if not used).
     */

    public String userDN;

    /**
     * The Manager User's password - (is null if user is not manager).
     */

    public char[] pwd;

    /**
     * The jndi ldap referral type: [follow:ignore:throw] (may be null - defaults to 'follow').
     */

    public String referralType = "follow";

    /**
     * How aliases should be handled in searches ('always'|'never'|'find'|'search').
     */

    public String aliasType = "searching";

    /**
     * Whether to use SSL (either simple or client-authenticated).
     */

    public boolean useSSL;
    
    /**
     * The file containing the trusted server certificates (no keys).
     */

    // XXX we may want to expand this later to 'SSL type'
    public String cacerts;

    /**
     * The file containing client certificates and private key(s).
     */

    public String clientcerts;

    /**
     * The password to the ca's keystore (may be null for non-client authenticated ssl).
     */

    public char[] caKeystorePwd;

    /**
     * The password to the client's keystore (may be null for non-client authenticated ssl).
     */

    public char[] clientKeystorePwd;

    /**
     * The type of ca keystore file; e.g. 'JKS', or 'PKCS12'.
     */

    public String caKeystoreType;

    /**
     * The type of client keystore file; e.g. 'JKS', or 'PKCS12'.
     */

    public String clientKeystoreType;

    /**
     * The SSL connection socket factory.  This defaults to com.ca.commons.jndi.JndiSocketFactory
     */

    public String sslSocketFactory = "com.ca.commons.jndi.JndiSocketFactory";

    /**
     * Whether to set BER tracing on or not.  (This is a very verbose
     * dump of all the raw ldap data as it streams past).
     */

    public boolean tracing;

    /**
     * Whether to set SSL tracing on or not.  (This is a very verbose
     * dump of all the SSL data as it streams past).
     */

    public boolean sslTracing;


    private static final String DEFAULT_CTX = "com.sun.jndi.ldap.LdapCtxFactory";

//    private static final String DEFAULT_DSML_CTX = "com.sun.jndi.dsmlv2.soap.DsmlSoapCtxFactory";
    private static final String DEFAULT_DSML_CTX = "com.ca.jndiproviders.dsml.DsmlCtxFactory";


    // Vadim: GSSAPI

        /**
         * Whether to use GSSAPI
         */

    public boolean useGSSAPI;

    /**
     *  Any extra environment magic required; e.g. to make GSSAPI work
     */
    public Properties extraProperties;

    /**
     *  Optional 'user friendly' name for this set of connection data details, used to give the
     * browser window a unique name.
     */

    public String templateName;

    /**
     * Allows for a 'read only' connection to be specified on connection.  The read only
     * connection disables any directory changes.
     */

    public boolean readOnly = false;

    /**
     * Empty constructor - data fields are intended
     * to be set directly.
     */



    public ConnectionData()
    {

    }

    /**
     * This sets up a full connection data object with the information needed to
     * create a jndi environment properties object.  Usually you won't need to use
     * the full method, and can use one of the shorter versions that sets empty
     * defaults for the unused bits.
     *
     * @param version
     * @param url
     * @param userDN
     * @param pwd
     * @param tracing
     * @param referralType
     * @param aliasType
     * @param useSSL
     * @param cacerts
     * @param clientcerts
     * @param caKeystorePwd
     * @param clientKeystorePwd
     * @param caKeystoreType
     * @param clientKeystoreType
     * @param useGSSAPI whether to use the GSSAPI protocol (e.g. for Kerberos support)
     * @param extraProperties a 'get out of jail free' for any bizarre properties that haven't
     * already been covered, or that are introduced in the future.  Using this isn't really
     * good coding practice, since we don't know what there properties are, but is sometimes
     * required...
     *
     */
    public ConnectionData(int version,
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
                          String templateName,
                          Properties extraProperties)
    {
        this.version = version;
        this.url = url;
        this.userDN = userDN;
        this.pwd = pwd;
        this.referralType = referralType;
        this.aliasType = aliasType;
        this.useSSL = useSSL;
        this.cacerts = cacerts;
        this.clientcerts = clientcerts;
        this.caKeystorePwd = caKeystorePwd;
        this.clientKeystorePwd = clientKeystorePwd;
        this.caKeystoreType = caKeystoreType;
        this.clientKeystoreType = clientKeystoreType;
        this.tracing = tracing;

        this.sslTracing = tracing;  // XXX for the time being, BER tracing and SSL Tracing are entwined :-).

        this.useGSSAPI = useGSSAPI;
        this.templateName = templateName;
        this.extraProperties = extraProperties;
    }

    /**
     * Utility method for test routines
     *
     * @param version
     * @param url
     * @param userDN
     * @param pwd
     * @param tracing
     * @param referralType
     * @param aliasType
     */ 
    public ConnectionData(int version,
                          String url,
                          String userDN,
                          char[] pwd,
                          boolean tracing,
                          String referralType,
                          String aliasType)
    {
        this.version = version;
        this.url = url;
        this.userDN = userDN;
        this.pwd = pwd;
        this.referralType = referralType;
        this.aliasType = aliasType;
        this.sslTracing = tracing;  // XXX for the time being, BER tracing and SSL Tracing are entwined :-).
    }


    public void setProtocol(String newProtocol)
    {
        if (newProtocol.equalsIgnoreCase(LDAP))
            protocol = LDAP;
        else if (newProtocol.equalsIgnoreCase(DSML))
            protocol = DSML;
        else
            System.err.println("Unknown Protocol " + newProtocol);
    }

    /**
     * This should be used to clear all the passwords
     * saved in this data object when they have been
     * used and are no longer needed... make sure however
     * that no references to the passwords remain to be
     * used by other parts of the program first :-)!
     * <p/>
     * (nb - since JNDI uses the passwords as Strings, they
     * can still be sniffed from JNDI :-( ).
     */

    public void clearPasswords()
    {
        if (pwd != null) for (int i = 0; i < pwd.length; i++) pwd[i] = ' ';  //TE: null is incompatible.
        if (caKeystorePwd != null) for (int i = 0; i < caKeystorePwd.length; i++) caKeystorePwd[i] = ' ';
        if (clientKeystorePwd != null) for (int i = 0; i < clientKeystorePwd.length; i++) clientKeystorePwd[i] = ' ';

        pwd = null;
        caKeystorePwd = null;
        clientKeystorePwd = null;
    }


    /**
     * Sets the url from the host & port, e.g. "ldap://" + host + ":" + port".
     * (NB: If the protocol is <i>NOT</i> LDAP, (e.g. DSML) this must be set first.
     *
     * @param host the host name to connect to, e.g. echidna or 168.10.5.122.
     * @param port the host port to connect to, e.g. 19389.
     */

    public void setURL(String host, int port)
    {
        if (protocol == LDAP)
            url = "ldap://" + host + ":" + port;
        else if (protocol == DSML)
            url = "http://" + host + ":" + port;

    }

    /**
     * Sets the url from the host & port, e.g. "ldap://" + host + ":" + port".
     * (NB: If the protocol is <i>NOT</i> LDAP, (e.g. DSML) this must be set first.
     *
     * @param URL The full URL to connect to
     */

    public void setURL(String URL)
    {
        if (protocol == LDAP)
        {
            if (URL.toLowerCase().startsWith("ldap://"))
                url = URL;
            else
                url = "ldap://" + URL;
        }
        else if (protocol == DSML)
        {
            if (URL.toLowerCase().startsWith("http://"))
                url = URL;
            else if (URL.toLowerCase().startsWith("dsml://"))
                url = "http://" + URL.substring(7);
            else
                url = "http://" + URL;
        }
        else    // not sure if this is necessary...
        {
            if (URL.toLowerCase().startsWith("ldap:"))
            {
                protocol = LDAP;
                url = URL;
            }
            else if (URL.toLowerCase().startsWith("http:"))
            {
                protocol = DSML;
                url = URL;
            }
            else if (URL.toLowerCase().startsWith("dsml:"))
            {
                protocol = DSML;
                url = "http:" + URL.substring(5);
            }
        }
    }

    public String getTemplateName()
    {
        return templateName;
    }

    public String getURL()
    {
        return url;
    }

    /**
     * Gets the host name from the url string.
     *
     * @return the host name for example: DEMOCORP.
     */

    // parse rules; the url is always of the form <protocol>://<hostname>:<port>[/server stuff (for dsml only)]

    public String getHost()
    {
        if (url == null)
            return null;

        int protocolSeparator = url.indexOf("://") + 3;
        int portSeparator = url.indexOf(":", protocolSeparator);
        return url.substring(protocolSeparator, portSeparator);
    }


    /**
     * Gets the port number from the url string.
     *
     * @return the port number for example: 19389.
     */

    public int getPort()
    {
        if (url == null)
            return -1;

        try
        {
            int protocolSeparator = url.indexOf("://") + 3;
            int portSeparator = url.indexOf(":", protocolSeparator) + 1;
            int serverDetails = url.indexOf("/", portSeparator);

            String port = (serverDetails == -1) ? url.substring(portSeparator) : url.substring(portSeparator, serverDetails);
            int portNumber = Integer.parseInt(port);
            if (portNumber > 65536 || portNumber <= 0)
                return -1;

            return portNumber;
        }
        catch (NumberFormatException nfe)
        {
            return -1;
        }
    }


    /**
     * Returns this data object as a string (doesn't include passwords)..
     *
     * @return the data object as a string.
     */

    public String toString()
    {
        return new String("baseDN: " + ((baseDN==null)?"null":baseDN) +
                "\nversion: " + Integer.toString(version) +
                "\nurl: " + ((url==null)?"null":url) +
                "\nuserDN: " + ((userDN==null)?"null":userDN) +
                "\npwd: " + ((pwd==null)?"null":"***") +
                "\nreferralType: " + ((referralType==null)?"null":referralType) +
                "\naliasType: " + ((aliasType==null)?"null":aliasType) +
                "\nuseSSL: " + String.valueOf(useSSL) +
                "\ncacerts: " + ((cacerts==null)?"null":cacerts) +
                "\nclientcerts: " + ((clientcerts==null)?"null":clientcerts) +
                "\ncaKeystoreType: " + ((caKeystoreType==null)?"null":caKeystoreType) +
                "\nclientKeystoreType: " + ((clientKeystoreType==null)?"null":clientKeystoreType) +
                "\ncaKeystorePwd; " + ((caKeystorePwd==null)?"null":new String(caKeystorePwd)) +
                "\nclientKeystorePwd: " + ((clientKeystorePwd==null)?"null":new String(clientKeystorePwd)) +
                "\ntracing: " + String.valueOf(tracing) +
                "\nprotocol: " + ((protocol==null)?"null":protocol) +
                "\nsslSocketFactory: " + ((sslSocketFactory==null)?"null":sslSocketFactory) +
                "\nuseGSSAPI: " + String.valueOf(useGSSAPI));
    }

    /**
     * This returns the connection data as a jndi env object suitable for
     * use in opening a directory context.
     *
     * @return the JNDI environment
     *
     * @throws NamingException a large range of exceptions, ranging from invalid data through
     *                         to problems setting up the SSL connection.
     */
    public Hashtable getJNDIEnvironment()
            throws NamingException
    {
/*
   These are the ConnectionData variables to be placed into the hash table...

        version
        url
        userDN
        pwd
        referralType
        aliasType
        useSSL
        cacerts
        clientcerts
        caKeystorePwd
        clientKeystorePwd
        caKeystoreType
        clientKeystoreType
        tracing
        sslTracing
        useGSSAPI

        ... while 'extraProperties' is simply appended to the environment data
*/
        checkData();  // throws NamingException if data invalid.

        Hashtable env = new Hashtable();  // an environment for jndi context parameters

        if (protocol == DSML)       //TE: set the protocol to DSML or LDAP.
            env.put(Context.INITIAL_CONTEXT_FACTORY, DEFAULT_DSML_CTX);
        else if (protocol == LDAP)
            env.put(Context.INITIAL_CONTEXT_FACTORY, DEFAULT_CTX);

        JNDIOps.setupBasicProperties(env, url, tracing, referralType, aliasType);       // set up the bare minimum parameters


        // XXX should we pass the security type through to here to avoid this implicit check??
        // XXX evil warning - this sets Context.SECURITY_AUTHENTICATION to simple, it is over-ridden
        // XXX by other types below.
        if (pwd != null && userDN != null)
        {
            JNDIOps.setupSimpleSecurityProperties(env, userDN, pwd);
        }
        // add the SSL ('ca...') and possible SASL ('client...') parameters
        if (useSSL)
        {

            if (tracing)
                sslTracing = true;  // for the time being, bind tracing and sslTracing.

            JNDIOps.setupSSLProperties(env, cacerts, clientcerts,
                    caKeystorePwd, clientKeystorePwd,
                    caKeystoreType, clientKeystoreType,
                    sslTracing, sslSocketFactory);
        }

        // Vadim + DEE: GSSAPI

        if (useGSSAPI)
        {
            env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
            //Maybe include something like JNDIOps.setupKerberosProperties here??
            env.put("javax.security.sasl.qop","auth-conf");
            // Above says use confidentiality, i.e. encrypted packets
            // We do it here, so it only applies to the GSSAPI,
            // i.e. Kerberos, that can always do encryption.
            // If added to jxconfig.txt, it would apply to all
            // SASL connections, and not sure if password+SSL would
            // pass the QOP test.
            // DEE
        }

        // Add any 'extra' properties to the list.
        if (extraProperties!=null && extraProperties.size()>0)
        {
            Enumeration extraKeys = extraProperties.keys();
            while (extraKeys.hasMoreElements())
            {
                try
                {
                    String key = (String)extraKeys.nextElement();
                    String value = (String)extraProperties.getProperty(key);
                    if (value != null)
                        env.put(key, value);
                }
                catch (ClassCastException e) {} // do nothing, but skip non string properties (should never happen)
            }
        }

        return env;
    }

    /**
     * This method confirms that the data entered in the ConnectionData object is
     * consistent, complete and valid.
     *
     * @throws NamingException thrown if the data is inconsistent or incomplete.
     */
    public void checkData() throws NamingException
    {
        // sanity check
        if (url == null)
            throw new NamingException("URL not specified in openContext()!");

        if (version < 2 || version > 3)
            throw new NamingException("Incorrect ldap Version! (was " + version + ")");

        if (useSSL && (cacerts == null))
            throw new NamingException("Cannot use SSL without a trusted CA certificates JKS file.");

        if (referralType == null) referralType = "follow";  // not an error not to specify this.

        if (aliasType == null) aliasType = "finding"; // not an error not to specify this

        if ("followthrowignore".indexOf(referralType) == -1)
            throw new NamingException("unknown referral type: " + referralType + " (setting to 'follow')");
    }

    public void putExtraProperty(String key, String property)
    {
        if (extraProperties==null)
            extraProperties = new Properties();

        extraProperties.put(key, property);
    }
}