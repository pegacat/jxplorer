package com.ca.directory.jxplorer;

import com.ca.commons.cbutil.CBSystemProperties;
import com.ca.commons.cbutil.CBUtility;
import com.ca.commons.jndi.JNDIOps;
import com.ca.commons.naming.DXAttribute;
import com.ca.commons.naming.LdifUtility;
import com.ca.commons.security.cert.CertViewer;
import com.ca.directory.BuildNumber;
import com.ca.directory.jxplorer.broker.JNDIDataBroker;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.*;

/**
 * (c) Chris Betts: Groupmind Project
 */
public class JXConfig
{
    public static Properties myProperties;       // global variables for the browser, read from...
    public static String propertyFile;           // ...a user configurable file storing default properties.
    public static String localDir;               // local directory the browser is being run from...

    /*
     *    Constants that define which security property elements to use.
     */

    public static final String CLIENT_TYPE_PROPERTY = "keystoreType.clientcerts";
    public static final String CA_TYPE_PROPERTY = "keystoreType.cacerts";
    public static final String CLIENT_PATH_PROPERTY = "option.ssl.clientcerts";
    public static final String CA_PATH_PROPERTY = "option.ssl.cacerts";
    public static final String ALLOW_CONNECTION_CERT_IMPORT = "option.ssl.import.cert.during.connection";

    public static final String LANGUAGE_OVERRIDE="force.locale";
    public static final String LANGUAGE_OVERRIDE_DEFAULT="default";

    private static Logger log = Logger.getLogger(JXConfig.class.getName());
    public static String version = BuildNumber.value;
    public static boolean debug = false;
    public static int debugLevel = 0;
    public static boolean debugSSL = false;


    /**
     * Convenience function to access both the System
     * property list, or failing that the internal JXplorer
     * properties list.
     *
     * @param key property key; see the java.util.Properties class.
     */

    public static String getProperty(String key)
    {
        if (System.getProperty(key) != null)
            return System.getProperty(key);

        if (myProperties != null && myProperties.containsKey(key))
            return myProperties.getProperty(key);

        return null;
    }

    /**
     * Convenience function to return a positive integer config value.
     * Returns -1 if the value is not available or cannot be parsed

     *
     * @param key  the property key
     * @return the int value of the property, or -1 if missing or an error
     */
    public static int getIntProperty(String key)
    {
        return getIntProperty(key, -1);
    }

    /**
     * Convenience function to return a positive integer config value.
     * Returns -1 if the value is not available or cannot be parsed

     *
     * @param key  the property key
     * @return the int value of the property,
     * @param defaultValue the default value for the property if none is found pre-defined
     */
    public static int getIntProperty(String key, int defaultValue)
    {
        try
        {
            String val = getProperty(key);

            return (val==null)?defaultValue:Integer.parseInt(val);
        }
        catch (Exception e)
        {
            return defaultValue;
        }
    }

    /**
     * Convenience function to access the internal JXplorer properties list.
     *
     * @param key          property key; see the java.util.Properties class.
     * @param defaultValue the default value for the property if none is found pre-defined
     */

    public static String getProperty(String key, String defaultValue)
    {
        if (myProperties == null) return defaultValue;
        return myProperties.getProperty(key, defaultValue);
    }

    /**
     * Sometimes it is more convenient to simply pass the whole properties
     * hash to a subordinate class, rather than using the get...() methods
     */

    public static Properties getMyProperties()
    {
        return myProperties;
    }


    /**
     * sets a property in the dxserver property list.
     */
    public static void setProperty(String key, String value)
    {
        if (key != null)
            myProperties.setProperty(key, value);

    }

    /**
     * sets a property in the dxserver property list.
     */
    public static void setProperty(String key, String value, String comment)
    {
        if (key != null)
            myProperties.setProperty(key, value);

        if (comment != null && comment.length() > 0)
            myProperties.put(key + ".comment", comment);
    }

    /**
     * Sets the default values for properties.  This is overridden if the
     * property appears in the system properties list or config text file.
     * (The system properties list always takes priority.)<p>
     *
     * @param key   the unique key of the property.
     * @param value the actual value to set.
     * @return the final value of the property.
     */

    public static String setDefaultProperty(String key, String value)
    {
        if (System.getProperty(key) != null)
            return System.getProperty(key);

        if (myProperties.containsKey(key))
            return myProperties.getProperty(key);

        myProperties.setProperty(key, value);
        return value;
    }

    /**
     * Sets the default values for properties.  This is overridden if the
     * property appears in the system properties list or config text file.
     * (The system properties list always takes priority.)<p>
     * <p/>
     * Also adds a comment for the property file.
     *
     * @param key     the unique key of the property
     * @param value   the actual value to set
     * @param comment an optional comment
     * @return the final value of the property
     */
    public static String setDefaultProperty(String key, String value, String comment)
    {
        if (comment != null && comment.length() > 0)
            myProperties.put(key + ".comment", comment);

        return setDefaultProperty(key, value);
    }

    /**
     * Reads the initial config file, providing defaults if necessary
     */
    public static void setupProperties()
    {
        setupProperties(null);
    }

    public static String getLocalDirectory()
    {
        return System.getProperty("user.dir") + File.separator;
    }
    

    /**
     * Initialises the myProperties property list, and sets
     * default values for properties not in the config file.
     * <p/>
     * Note that file names use the File.separator character, (which
     * is system dependant) while URLs use '/' always.
     */

    public static String getConfigDirectory()
    {
        String configDir = CBUtility.getConfigDirectory(JXplorer.APPLICATION_NAME);

        log.info("JX using configDirectory: " + configDir);

        return configDir;
    }

    /**
     * Reads the initial config file, providing defaults if necessary.
     * @param suppliedProperties an initial set of default properties that takes precedence over any read file values or defaults
     */
    public static void setupProperties(Properties suppliedProperties)
    {
        localDir = getLocalDirectory();  // the application local directory, with a bunch of installed resource files

        String configDir = getConfigDirectory(); // the writeable configuration directory

        if (suppliedProperties == null)  // the usual case
        {
            String configFileName = "jxconfig.txt";

            //checks for custom config file location; either passed on the command line or forced on us by security permissions (e.g. Win 7)
            propertyFile = CBUtility.getPropertyConfigPath(JXplorer.APPLICATION_NAME, configFileName);

            myProperties = CBUtility.readPropertyFile(propertyFile);

        }
        else
        {
            myProperties = suppliedProperties;
        }

        // initialise the 'myProperties' variable (also used in setDefaultProperty())

        setDefaultProperty("url.defaultdirectory", "localhost", "default for empty connection screen GUI - rarely used");
        setDefaultProperty("url.defaultdirectory.port", "389", "default for empty connection screen GUI - rarely used");
        setDefaultProperty("url.defaultadmin", "localhost", "default value for a specific 3rd party plug in; rarely used");
        setDefaultProperty("url.defaultadminport", "3389", "default value for a specific 3rd party plug in; rarely used");
        setDefaultProperty("baseAdminDN", "cn=Management System", "default value for a specific 3rd party plug in; rarely used");


        /*
         *    File and URL defaults.  Many of these are the same thing in two forms;
         *    one for accessing the files directly, the other for accessing them
         *    as a URL.
         *
         *    The properties are of the form dir.* or url.*, with dir referring to
         *    a file directory and url referring to the directory as a URL access.
         *
         *    dir.local and url.local is the directory the app is currently running in.
         *    *.htmldocs is the directory with the doco in it; i.e. help etc.
         *    *.templates is the root directory for the html attribute template files.
         *
         *    XXX this is wierd.  Rewrite it all nicer.
         */

        setProperty("dir.comment", "this sets the directories that JXplorer reads its static resources from.");
        setDefaultLocationProperty("dir.local", localDir);
        setDefaultLocationProperty("dir.help", localDir + "help" + File.separator);
        setDefaultLocationProperty("dir.plugins", localDir + "plugins" + File.separator);

        setDefaultProperty("width", "1000", "set by client GUI - don't change");

        setDefaultProperty("height", "700", "set by client GUI - don't change");

        setDefaultProperty("baseDN", "c=au", "the default base DN for an empty connection - rarely used");

        setDefaultProperty("ldapversion", "3", "set by client GUI - don't change");

        // java log setup

        setDefaultProperty(".level", "WARNING", "(java loggin variable) - allowable values are 'OFF', 'SEVERE', 'WARNING', 'INFO', 'FINE', 'FINER', 'FINEST' and 'ALL'");

        setDefaultProperty("com.ca.level", "WARNING", " (java loggin variable) partial logging is also available.  Be warned that the Sun logging system is a very buggy partial reimplementation of log4j, and doesn't seem to do inheritance well.");

        setDefaultProperty(LANGUAGE_OVERRIDE, LANGUAGE_OVERRIDE_DEFAULT, "use this to set local language detection ('default') or force the use of a particular language. (e.g. 'en' / 'en.US' / 'de' / 'zh.CN' / 'zh.TW'");

        //setDefaultProperty("logging", "console");
        //setProperty("logging.comment", "allowable log modes: none | console | file | both");
        //setDefaultProperty("log4j.config", "log4j.xml");
        //setProperty("logging.comment", "this is not used for all logging - the logging system is still a bit primative, and in the process of migraing to java logging");

        setDefaultProperty("handlers", "java.util.logging.ConsoleHandler", "(java logging variable) This sets the log level for console reporting");

        setDefaultProperty("java.util.logging.ConsoleHandler.level", "ALL", "(java logging variable) This sets the log level for console reporting");

        setDefaultProperty("java.util.logging.ConsoleHandler.formatter", "java.util.logging.SimpleFormatter", "(java logging variable) This sets the built in formatter to use for console reporting");

        setDefaultProperty("java.util.logging.FileHandler.level", "ALL", "(java loggin variable) This sets the log level for log file reporting");

        setDefaultProperty("java.util.logging.FileHandler.pattern", "JX%u.log", "(java loggin variable) The name of the log file (see java.util.logging.FileHandler java doc)");

        setDefaultProperty("java.util.logging.FileHandler.formatter", "java.util.logging.SimpleFormatter", "(java loggin variable) This sets the built in formatter to use for file reporting");


        //setDefaultProperty("log.debuglevel", "warning", "allowable debug levels; severe, warning (default), info, fine, finer, finest (includes  BER dump) - usually set by client GUI.");

        setDefaultProperty("null.entry.editor", "defaulteditor", "the editor displayed for null entries is pluggable and can be set to a custom java class");

        setDefaultProperty("plugins.ignoreUniqueness", "false", "whether to allow multiple plugins for the same object class: 'true' or 'false");

        setDefaultProperty("option.ignoreSchemaOnSubmission", "false", "Skip client side schema checks; useful if JXplorer is getting confused or the schema is inconsistent");

        setDefaultProperty("option.ldap.timeout", "0", "the maximum time to allow a query to run before cancelling - '0' = 'as long as the server allows'");

        setDefaultProperty("option.ldap.limit", "0", "The maximum number of entries to return - '0' = 'all the server allows'");

        setDefaultProperty("option.ldap.referral", JNDIOps.DEFAULT_REFERRAL_HANDLING, "this is a jdni variable determining how referrals are handled: 'ignore','follow' or 'throw'");   // 'ignore'

        setDefaultProperty("option.ldap.pagedResults", "false", "set this to true to use paged results for large data sets, if your directory supports it (e.g AD)");

        setDefaultProperty("option.ldap.pageSize", "-1", "Sets the page size for paged results, if paged results are used.  (Generally 1000 is a good size)");

        setDefaultProperty("option.ldap.browseAliasBehaviour", JNDIOps.DEFAULT_ALIAS_HANDLING, "jndi variable setting how aliases are handled while browsing: 'always','never','finding','searching'");        // behaviour when browsing tree (= 'finding')

        setDefaultProperty("option.ldap.searchAliasBehaviour", "searching", "jndi variable setting how aliases are handled while searching: 'always','never','finding','searching'");      // behaviour when making search request

        setDefaultProperty("option.confirmTableEditorUpdates", "false", "whether the user is prompted before updates; usually set by GUI");          //TE: set false by default for dxadmin (bug 2848).

        setDefaultProperty("option.url.handling", "JXplorer", "override URL handling to launch JXplorer rather than default browser");                      //TE: set the URL handling to displaying JXplorer rather than launch into default browser.

        setDefaultProperty("option.ldap.sendVerboseBinarySuffix", "false", "some directories require ';binary' to be explicitly appended to binary attribute names: 'true' or 'false' (default)");

        setDefaultProperty("option.ldap.useRFC4523BinarySuffix", "true", "append ';binary' to certificate attributes as per RFC 4453: 'true' (default) or 'false'");

        setDefaultProperty("option.drag.and.drop", "true", "set to 'false' to disable drag and drop in the left hand tree view");

        setDefaultProperty("jxplorer.cache.passwords", "true", "whether JX should keep a (run time only) cache of passwords for reuse and reconnection");

        setDefaultProperty("mask.raw.passwords", "true", "whether to mask userPassword in the entry password editor");

        setDefaultProperty("sort.by.naming.attribute", "false", "if true, this sorts entries in the tree editor by naming attribute first, then by attribute value");

        if ("true".equals(getProperty("option.ldap.sendVerboseBinarySuffix")))
        {
            log.fine("using verbose binary suffix ';binary'");  // Warning: logger may not yet be initialised
            DXAttribute.setVerboseBinary(true);  // default if 'false'
        }

        if ("false".equals(getProperty("option.ldap.useRFC4523BinarySuffix")))
        {
            log.fine("using binary suffix ';binary'");  // Warning: logger may not yet be initialised
            DXAttribute.setRFC4523BinaryHandling(false);  // default is 'true'
        }


        /*
         *    Security defaults
         */

        setDefaultProperty(CA_PATH_PROPERTY, configDir + "security" + File.separator + "cacerts");
        setDefaultProperty(CLIENT_PATH_PROPERTY, configDir + "security" + File.separator + "clientcerts");
        setDefaultProperty(CLIENT_TYPE_PROPERTY, "JKS");
        setDefaultProperty(CA_TYPE_PROPERTY, "JKS");
        setDefaultProperty(ALLOW_CONNECTION_CERT_IMPORT, "true");
        // echo the above back as a system property so that independant trust stores can access it globally.  Ugly? Yes.
        System.setProperty(ALLOW_CONNECTION_CERT_IMPORT, getProperty(ALLOW_CONNECTION_CERT_IMPORT));

//        setDefaultProperty("securityProvider", "sun.security.provider.Sun");
//        set default security provider to match alljssl.jar
        setDefaultProperty("securityProvider", "com.sun.net.ssl.internal.ssl.Provider");
        setProperty("securityProvider.comment", "the security provider can be changed, and three more can be added by creating 'securityProperty0', 'securityProperty1' and 'securityProperty2'.");

        // SECURITY/SSL HANDLER
        setDefaultProperty("ldap.sslsocketfactory", "com.ca.commons.jndi.JndiSocketFactory");
        setProperty("ldap.sslsocketfactory.comment", "This is the built in ssl factory - it can be changed if required.");

        // special hack to allow forcing of TLS{x} or TLSv1 as the only allowed SSL protocol... option are 'TLSv1', 'SSLv3', 'any', 'TLSv1,TLSv1.1', or simply 'TLS' for all TLS protocols, or (not recommended) 'SSL' for only old SSL protocols
        setDefaultProperty("option.ssl.protocol", "any", "Force JXplorer to only use some specific SSL protocols, rather than negotiating a normal SSL connection. e.g. just 'TLS' or 'TLSv1' or 'TLSv1,TLSv1.1' (or 'any' to negotiate)");

        // special special hack to add to system properties, so it can be picked up by separate com.ca.security.commons.JXSSLSocketFactory
        String ssl = myProperties.getProperty("option.ssl.protocol");
        if (ssl!=null)
            System.setProperty("option.ssl.protocol", ssl);

        setDefaultProperty("option.ssl.debug", "true", "print out SSL debug messages");

        setDefaultProperty("gui.lookandfeel", UIManager.getSystemLookAndFeelClassName());    //TE: sets the default look and feel to the system default.
        setDefaultProperty("gui.lookandfeel.comment", "Can set to com.sun.java.swing.plaf.mac.MacLookAndFeel for OSX");    //TE: sets the default look and feel to the system default.

        setDefaultProperty("last.search.filter", "default");    //TE: sets the last filter property to 'default'.

        /*
         *    Check if we need to read system properties.
         */

        setDefaultProperty("getSystemEnvironment.comment", "Set this to true if you wish to add the system environment properties to the JX list (e.g. if you are setting JX properties via system variables)");
        setDefaultProperty("getSystemEnvironment", "false");

        if (getProperty("getSystemEnvironment").equalsIgnoreCase("true"))
        {
            CBSystemProperties.loadSystemProperties();
        }


        // XXX something of a hack - manually set these properties in CertViewer,
        // XXX simply because it is a 'sorta' pluggable editor, that may be called
        // XXX directly without an opportunity to pass them directly.

        CertViewer.setProperties(myProperties);

        //TE: sets up the help link in the cert viewer so that a help button is
        //TE: added to the dialog which links to the appropriate topic in the help.
        CertViewer.setupHelpLink(HelpIDs.SSL_VIEW);


        // optional support for xml in ldif files.
        setDefaultProperty("xml.ldif.rfc", "false");    //option to save XML text in ldif files
        setDefaultProperty("xml.ldif.rfc.comment", "Experimental support for saving XML in LDIF files in editable form (e.g. not base64 encoded)");
        if ("true".equals(getProperty("xml.ldif.rfc")))
            LdifUtility.setSupportXML_LDIF_RFC(true);


        // support for 'read only' browser creation
        setDefaultProperty("lock.read.only", "false", "if true, this locks the browser into read only mode and no directory modifications can be made");
        if ("true".equals(getProperty("lock.read.only")))
            JNDIDataBroker.lockToReadOnlyMode();

        // write out default property file if non exists...

        if (propertyFile != null && new File(propertyFile).exists() == false)
            writePropertyFile();
}


    /**
     * loads a property representing a file directory (XXX or url), and checks that
     * that it exists.  If either the property doesn't exist, or the actual
     * directory doesn't exist, then the default is used instead...
     */
    protected static void setDefaultLocationProperty(String propName, String defaultLocation)
    {
        setDefaultProperty(propName, defaultLocation);
        String newLocation = getProperty(propName);
        if (!newLocation.equals(defaultLocation))
        {
            File test = new File(newLocation);
            if (!test.exists())
            {
                log.warning("Uunable to find location '" + newLocation + "' -> reverting to '" + defaultLocation + "'");
                setProperty(propName, defaultLocation);
            }
        }
    }

    public static void writePropertyFile()
    {
        CBUtility.writePropertyFile(propertyFile, myProperties, new String("# The property file location defaults to where JXplorer is installed\n" +
                "# - this can be over-ridden with the system property 'jxplorer.config'\n" +
                "#   with a config directory location, or set to user home using the\n" +
                "#   flag 'user.home' (e.g. -Djxplorer.config='user.home' on the command line).\n"));
    }

        /**
     * Initialises the log manager using log configuration set in the properties file
     */
    protected static void setupLogger()
    {
        // loadProperties() should have been called.

        log.info("setting up logger");

        try
        {
            // Sun logging system weird.  Cascading log levels only work if you pre-create exact loggers. whatever.
            Logger.getLogger("com");
            Logger.getLogger("com.ca");
            Logger.getLogger("com.ca.directory");
            Logger.getLogger("com.ca.directory.jxplorer");

            // XXX Have to reinitialise 'log' here, because Sun logging system is too stupid to do the cascading trick
            // XXX unless the 'parent' loggers have been already created. (is this still correct?)
            log = Logger.getLogger(JXplorer.class.getName());

            LogManager logManager = LogManager.getLogManager();
            logManager.reset();

            logManager.readConfiguration(new FileInputStream(JXConfig.propertyFile));
            System.out.println("logging level set from config to: " + CBUtility.getTrueLogLevel(log) + " with " + log.getHandlers().length + " parents=" + log.getUseParentHandlers());

            log.info("Using configuration file: " + JXConfig.propertyFile);
            log.info("logging initialised to global level " + CBUtility.getTrueLogLevel(log));

            // Try to disable swing errors...

            Logger swinglog = Logger.getLogger("javax.swing");
            swinglog.setLevel(Level.SEVERE);

            debugSSL = getProperty("option.ssl.debug").equalsIgnoreCase("true");

            // DEBUG BLOCK
            /*
            log.severe("ENABLED");
            log.warning("ENABLED");
            log.info("ENABLED");
            log.fine("ENABLED");
            log.finer("ENABLED");
            log.finest("ENABLED");
             */
            if (false) throw new IOException();
        }
        catch (IOException e)
        {
            log.log(Level.SEVERE, "Unable to load log configuration from config file: " + JXConfig.propertyFile, e);
            System.err.println("Unable to load log configuration from config file: " + JXConfig.propertyFile);
            e.printStackTrace();
            setupBackupLogger();
        }

        int currentLogLevel = CBUtility.getTrueLogLevel(log).intValue();

        if (currentLogLevel <= Level.FINE.intValue())
        {
            Vector sortedKeys = new Vector();
            Enumeration baseKeys = myProperties.keys();
            while (baseKeys.hasMoreElements())
            {
                String key = (String) baseKeys.nextElement();
                sortedKeys.addElement(key);
            }
            Collections.sort(sortedKeys);

            Enumeration propNames = sortedKeys.elements();

            StringBuffer propertyData = new StringBuffer();
            String propName;

            while (propNames.hasMoreElements())
            {
                propName = (String) propNames.nextElement();
                propertyData.append("property: ").append(propName).append(" = ").append(myProperties.getProperty(propName)).append("\n");
            }

            log.fine("property:\n" + propertyData.toString());
        }
    }

    /**
     * Failback routine for if we can't find proper log parameters... setup a console logger
     */


    protected static void setupBackupLogger()
    {
        Logger mainLogger = LogManager.getLogManager().getLogger("com.ca");

        // property default value is 'WARNING'
        mainLogger.setLevel(Level.parse(JXConfig.getProperty("java.util.logging.ConsoleHandler.level")));
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        mainLogger.addHandler(handler);

        //TODO: figure out how to end verbose swing logs...
        Logger swinglog = Logger.getLogger("javax.swing");
        swinglog.setLevel(Level.SEVERE);

    }
}
