package com.ca.directory.jxplorer;

import com.ca.commons.cbutil.*;
import com.ca.commons.jndi.JndiSocketFactory;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.security.Provider;
import java.security.Security;
import java.util.*;
import java.util.logging.*;

/**
 * Does the main setup for JXplorer.
 */
public class JXplorer
{
    private ArrayList<JXplorerBrowser> browsers;


    CBResourceLoader resourceLoader;              // loads files 'n stuff from zip/jar archives.
    CBClassLoader classLoader;                    // loads classes from zip/jar archives.
    protected CBHelpSystem helpSystem;            // use a single help system for all browser windows


    private static Logger log = Logger.getLogger(JXplorer.class.getName());  // ...It's round it's heavy it's wood... It's better than bad, it's good...

    /**
     * Constructor for the JXplorer object, which is in fact the whole browser.
     */

    // PROG NOTE: Many of these methods are order dependant - don't change the
    //            order they are called without checking!

    public JXplorer()
    {
        super();
        JXplorerInit(null);
    }

    public JXplorer(String initLdifName)
    {
        super();
        JXplorerInit(initLdifName);
    }


    protected void JXplorerInit(String initLdifName)
    {
        JWindow splash = new JWindow();
        showSplashScreen(splash);

        JXConfig.loadProperties();

        JXConfig.setupLogger();  // requires properties to be loaded first

        setupResourceFiles();

        CBIntText.init("language.JX", classLoader);   // i18n support.

        if (checkFileEnvironment() == false) return;

        setupHelp();             // set up the JavaHelp system

        browsers = new ArrayList<JXplorerBrowser>();

        createNewWindow();

        initUtilityFtns();  //TODO: This may cause problems when the first browser window is closed - need to use system frame?

        /* let's not complicate things for the time being
        if (initLdifName != null)
        {
            this.setStatus("Working Offline");
            this.workOffline = true;
            this.offlineBroker.clear();
            this.mrTree.registerDataSource(this.offlineBroker);
            this.mrTree.setRoot(new DN(SmartTree.NODATA));

            LdifImport imp = new LdifImport(datamodifier, this.mrTree, this, null, initLdifName);
        }
        */

        splash.dispose();
    }


    public static void printTime(String msg)
    {
        long time = System.currentTimeMillis();
        log.info(msg + "\nTIME: " + new Date().toString() + "  (" + (time % 1000) + ")\n");
    }

    /**
     * The main class, from whence all else proceeds.
     *
     * @param args Not currently used - intention to setup as CLI LDIF processor at some stage...
     */

    public static void main(String[] args)
    {
        printTime("main start");

        log.fine("running JXplorer version " + JXConfig.version);

        String initLdifName;

        if (checkJavaEnvironment() == false)
            System.exit(-1);

        if (args.length > 0)
        {
            System.out.println("LDIF FILE READ ON CLI DISABLED DURING DEVELOPMENT OF MULTI-WINDOW JX VERSION");
            System.exit(-1);
            log.info("trying to open ldif file " + args[0]);
            initLdifName = args[0];
        }
        else
        {
            initLdifName = null;
        }

        new JXplorer(initLdifName);

        printTime("main end");
    }


    /**
     * Set up some common utility ftns for logging and error reporting.
     */
    public void initUtilityFtns()
    {
        if (browsers.size()>0)
            CBUtility.initDefaultDisplay(browsers.get(0));
        //otherwise we should be shutting down...
    }



    /**
     * Checks that the java and system environment is
     * adequate to run in.
     */

    public static boolean checkJavaEnvironment()
    {
        log.info("running java from: " + System.getProperty("java.home"));
        String javaVersion = System.getProperty("java.version");
        log.info("running java version " + javaVersion);
        if (javaVersion.compareTo("1.5") < 0)
        {
            log.severe(CBIntText.get("TERMINATING: JXplorer requires Security Extensions and other features found only in java 1.5.0 or better."));
            JOptionPane.showMessageDialog(null, CBIntText.get("TERMINATING: JXplorer requires java 1.5.0 or better"), CBIntText.get("The Current Java Version is {0}", new String[]{javaVersion}), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    /**
     * Checks that the file directories are valid, and
     * contain any vital files.
     */

    public static boolean checkFileEnvironment()
    {
        return true;
    }



    /**
     * Initialise the JavaHelp system, pointing it at the right help files.
     */

    protected void setupHelp()
    {
        helpSystem = new CBHelpSystem("JXplorerHelp.hs"); // use default 'JXplorerHelp.hs' help set.
    }

    /**
     * This returns the help system used by JX.  Useful to get
     * if you need to append some more help stuff.
     *
     * @return the current JX HelpSystem.
     */

    public CBHelpSystem getHelpSystem()
    {
        return helpSystem;
    }


    /**
     * JXplorer utility ftn: load an image from the standard JX images directory.
     *
     * @param name the file name of the image file within the images directory
     * @return the loaded image.
     */

    public static ImageIcon getImageIcon(String name)
    {
        ImageIcon newIcon = new ImageIcon(Theme.getInstance().getDirImages() + name);
        return newIcon;
    }
    /**
     * Closes the application down, optionally printing out a message
     *
     * @param msg optional message to be printed out on closing.
     */

    public void shutdown(String msg)
    {
        JXConfig.setProperty("windows", String.valueOf(browsers.size()));

        for (int browserNo = 0; browserNo < browsers.size(); browserNo++)
        {
            JXplorerBrowser browser = browsers.get(browserNo);

            String suffix = getBrowserSuffix(browserNo);

            JXConfig.setProperty("width" + suffix, String.valueOf(((int) browser.getSize().getWidth())));
            JXConfig.setProperty("height"+ suffix, String.valueOf(((int) browser.getSize().getHeight())));

            JXConfig.setProperty("xpos"+ suffix, String.valueOf(browser.getX()));
            JXConfig.setProperty("ypos"+ suffix, String.valueOf(browser.getY()));

            JXConfig.setProperty("last.search.filter", "default");    //TE: sets the last filter property to 'default' (we don't really need to remember the filter after JX exists).
        }

        JXConfig.writePropertyFile();

        if (msg != null)
            log.severe("shutting down\n" + msg);
        else
            log.warning("shutting down");

        System.exit(0);
    }

    private static String getBrowserSuffix(int browserNo)
    {
        return (browserNo>0)?"": "-" + browserNo;
    }

    public String toString()
    {
        return "JXplorer version " + JXConfig.version;
    }



    /**
     * Displays a splash screen with a thin black border in the center of the screen.
     * Splash screen auto-sizes to be very slightly larger than the templates/JXsplash.png image.
     */

    public void showSplashScreen(JWindow splash)
    {
        ImageIcon splashIcon = new ImageIcon(Theme.getInstance().getDirTemplates() + "JXsplash.png");
        int width = splashIcon.getIconWidth();
        int height = splashIcon.getIconHeight();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        splash.setBounds((screen.width - width) / 2, (screen.height - height) / 2, width, height);
        JLabel pic = new JLabel(splashIcon);
        JPanel content = (JPanel) splash.getContentPane();
        content.add(pic);
        splash.setVisible(true);
    }


    /**
     * A variety of plugin possibilities exist in JXplorer.  To make distribution easier, it
     * is possible to package up class files, as well as image/text/etc. files, and use a
     * custom resource loader to access these zipped/jarred resources.  This sets it all up,
     * as well as adding any custom security providers...
     */

    public void setupResourceFiles()
    {
        resourceLoader = new CBResourceLoader();
        classLoader = new CBClassLoader(resourceLoader);

        String pluginPath = JXConfig.getProperty("dir.plugins");
        String[] pluginFiles = CBUtility.readFilteredDirectory(pluginPath, new String[]{"zip", "jar"});
        if (pluginFiles == null)
        {
            log.warning("Unable to access plugins directory: '" + pluginPath + "'");
            return;
        }

        for (int i = 0; i < pluginFiles.length; i++)
            resourceLoader.addResource(new CBJarResource(pluginPath + pluginFiles[i]));

        setupSecurityProviders();

        setupGSSAPIConfig();

    }

    /**
     * This sets up the inital GSSAPI config file if it does not already exist, and sets the
     * login config system property.
     */

    protected void setupGSSAPIConfig()
    {
        try
        {
            String sep = System.getProperty("line.separator");

            // the default gssapi.conf file (provided by Vadim Tarassov).
            String defaultFileText = "com.ca.commons.jndi.JNDIOps {" + sep +
                    "  com.sun.security.auth.module.Krb5LoginModule required client=TRUE" + sep +
                    "  \t\t\t\t\t\t\t\t\t\t\t\t\t\tuseTicketCache=TRUE;" + sep +
                    "};";

            String configFile = CBUtility.getPropertyConfigPath("gssapi.conf");
            File gssapi_conf = new File(configFile);

            // if it doesn't exist, write the default file above - if it does exist,
            // use whatever we're given...
            if (gssapi_conf.exists() == false)
            {
                FileWriter confWriter = new FileWriter(gssapi_conf);
                confWriter.write(defaultFileText);
                confWriter.close();
            }


            System.setProperty("java.security.auth.login.config", gssapi_conf.getCanonicalPath().toString());
        }
        catch (IOException e)
        {
            log.warning("ERROR: Unable to initialise GSSAPI config file " + e);
        }
    }

    protected void setupSecurityProviders()
    {
        // load providers in reverse order, since they are always
        // inserted at the beginning.

        String providerName = JXConfig.getProperty("securityProvider2", null);
        if (providerName != null)
            addSecurityProvider(providerName);

        providerName = JXConfig.getProperty("securityProvider1", null);
        if (providerName != null)
            addSecurityProvider(providerName);

        providerName = JXConfig.getProperty("securityProvider0", null);
        if (providerName != null)
            addSecurityProvider(providerName);

        providerName = JXConfig.getProperty("securityProvider", null);
        if (providerName != null)
            addSecurityProvider(providerName);

        // check to see if the users tried to use more than the above
        // (unlikely, we hope).

        providerName = JXConfig.getProperty("securityProvider3", null);
        if (providerName != null)
        {
            CBUtility.error(CBIntText.get("Too many security providers in config file."));
            printSecurityProviders();
        }
        // print provider list for debugging.
        else if (JXConfig.debugLevel >= 2)
            printSecurityProviders();

        // while we're here, register our custom class loader
        // with JndiSocketFactory, so it can access any custom
        // plugin security providers loaded above.

        JndiSocketFactory.setClassLoader(classLoader);
    }

    /**
     * Allows extra security providers to be manually added.
     *
     * @param providerName the class name of the security provider to
     *                     add (e.g. something like 'sun.security.provider.Sun')
     */

    protected void addSecurityProvider(String providerName)
    {

//XXX is there a fast way of checking if we already have this provider
//XXX (yes - could check for it in the global list of providers - probably not worth it...)

        try
        {

            Class providerClass = classLoader.loadClass(providerName);
            Object providerObject = providerClass.newInstance();

            Security.insertProviderAt((Provider) providerObject, 1);

//            Security.insertProviderAt(new com.sun.net.ssl.internal.ssl.Provider(), 1);
        }
        catch (Exception e)
        {
            System.err.println("\n*** unable to load new security provider: " + ((providerName == null) ? "null" : providerName));
            System.err.println(e);
        }

    }

    protected static void printSecurityProviders()
    {
        log.fine("\n***\n*** LIST OF CURRENT SECURITY PROVIDERS\n***");
        Provider[] current = Security.getProviders();
        {
            for (int i = 0; i < current.length; i++)
            {
                log.fine("provider: " + i + " = " + current[i].getName() + " " + current[i].getInfo());
                log.fine("   (" + current[i].getClass().toString() + ")\n");
            }
        }
        log.fine("\n***\n*** END LIST\n***\n");
    }

    /**
     * Test for solaris (usually to disable features that appear to be more than
     * usually broken on that platform - e.g. drag and drop).
     */

    public static boolean isSolaris()
    {
        String os = System.getProperty("os.name");
        if (os == null) return false;

        os = os.toLowerCase();
        if (os.indexOf("sun") > -1) return true;
        if (os.indexOf("solaris") > -1) return true;
        return false;
    }


    /**
     * Test for Linux (usually to disable features that appear to be more than
     * usually broken on that platform - e.g. JAVA L&F).
     *
     * @return true if Linux OS, false otherwise.
     */

    public static boolean isLinux()
    {
        String os = System.getProperty("os.name");

        if (os != null && os.toLowerCase().indexOf("linux") > -1)
            return true;

        return false;
    }

    /**
     * Test for Linux (usually to disable features that appear to be more than
     * usually broken on that platform - e.g. JAVA L&F).
     *
     * @return true if Linux OS, false otherwise.
     */

    public static boolean isMac()
    {
        String os = System.getProperty("mrj.version"); // mac specific call as per http://developer.apple.com/technotes/tn/tn2042.html
        return (os != null);
    }

    /**
     * Test for Windows (usually to disable features that appear to be more than
     * usually broken on that platform - e.g. JAVA L&F).
     *
     * @return true if Windows OS, false otherwise.
     */

    public static boolean isWindows()
    {
        String os = System.getProperty("os.name"); // mac specific call as per http://developer.apple.com/technotes/tn/tn2042.html

        if (os != null && os.toLowerCase().indexOf("windows") > -1)
            return true;

        return false;
    }

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    public CBResourceLoader getResourceLoader()
    {
        return resourceLoader;
    }

    public ArrayList<JXplorerBrowser> getBrowsers()
    {
        return browsers;
    }

    /*
     *   Handling for multiple Browser Windows...
     */

    /**
     * Creates a new independant browser window, to connect to a separate directory...
     */
    public void createNewWindow()
    {



        // look for JXWorkBenchBrowser 'add on' module...
        try
        {
            Class c = getClassLoader().loadClass("com.pegacat.jxworkbench.JXWorkBenchBrowser");
            Constructor constructor = c.getConstructor(new Class[0]);

            JXplorerBrowser browser = (JXplorerBrowser) constructor.newInstance(new Object[0]);
            browser.init(this);
            browsers.add(browser);
            log.warning("JXWorkBench loaded");
            return;
        }
        catch (Exception e)            // expected condition - just means JXWorkBench is not available
        {
            e.printStackTrace();
            log.warning("JXWorkBench not available - loading standard JXplorer version");
        }

        // only runs if no JXWorkBenchBrowser available...
        JXplorerBrowser browser = new JXplorerBrowser();
        browser.init(this);
        browsers.add(browser);

    }



    /**
      * Closes the application down
      */

     public void shutdown()
     {
         shutdown(null);
     }

     public void browserClosing(JXplorerBrowser closingBrowser)
     {
         if (browsers.size()>1)
         {

             System.out.println("Window " + browsers.size() + " Shutting Down");

             browsers.remove(closingBrowser);
             initUtilityFtns();   // bounce utility function pane on the offchance the closed window was the one being used for the utilty function 'root pane'
         }
         else
         {
             System.out.println("Last Window Shutting Down");
             shutdown();
         }
     }

    /**
     * This is used to trigger visibility changes on secondary browser popup tools, to allow the 'paste' option
     * to become active if a cut or copy selection is made in another browser window...
     * @param active
     */
     public void setPopupToolPasteOptions(boolean active)
     {
        if (browsers.size()>1)
        {
            for (JXplorerBrowser browser:browsers)
            {
                ButtonRegister br = browser.getButtonRegister();
                br.setItemEnabled(br.PASTE, active);
                br.setItemEnabled(br.PASTE_ALIAS, active);
            }
        }
     }


}
