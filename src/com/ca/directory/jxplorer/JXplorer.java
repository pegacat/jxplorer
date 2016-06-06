package com.ca.directory.jxplorer;

import com.ca.commons.cbutil.*;
import com.ca.commons.jndi.JndiSocketFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Does the main setup for JXplorer.
 */
public class JXplorer
{
    private ArrayList<JXplorerBrowser> browsers;


    CBResourceLoader resourceLoader;              // loads files 'n stuff from zip/jar archives.
    CBClassLoader classLoader;                    // loads classes from zip/jar archives.
    protected CBHelpSystem helpSystem;            // use a single help system for all browser windows
    public static String APPLICATION_NAME = "jxplorer";

    private static Logger log = Logger.getLogger(JXplorer.class.getName());  // ...It's round it's heavy it's wood... It's better than bad, it's good...

    private boolean batchMode = false;            // whether to run without a UI.


    /**
     * The main function, from whence all else proceeds.
     *
     * @param args used in batch mode only
     */

    public static void main(String[] args)
    {
        printTime("main start");

        log.fine("running JXplorer version " + JXConfig.version);

        if (checkJavaEnvironment() == false)
            System.exit(-1);

        boolean batch = false;
        if (args.length > 0)
        {
            runBatchJob(args);
        }
        else
            new JXplorer(false);

        printTime("main end");
    }

    /**
     * Constructor for the JXplorer object, which is in fact the whole browser.
     */

    // PROG NOTE: Many of these methods are order dependant - don't change the
    //            order they are called without checking!
    public JXplorer(boolean runInBatchMode)
    {
        super();
        batchMode = runInBatchMode;
        if (batchMode)
            JXplorerInitResources();
        else
            JXplorerFullInit();
    }

    /**
     * Initialises all the program resources, including the user interface GUI stuff.
     */
    protected void JXplorerFullInit()
    {
        JWindow splash = new JWindow();
        showSplashScreen(splash);

        JXplorerInitResources();

        setupHelp();             // set up the JavaHelp system

        browsers = new ArrayList<JXplorerBrowser>();

        createInitialWindows();

        initUtilityFtns();  //TODO: This may cause problems when the first browser window is closed - need to use system frame?

        splash.dispose();
    }

    /**
     * This initialises the config values, the logging system, resources (e.g. graphics) and language files.
     * It *does not* initialise anything requiring a GUI, and so can be used by the batch system.
     */
    protected void JXplorerInitResources()
    {
        copyDefaultFiles();

        JXConfig.setupProperties();

        JXConfig.setupLogger();  // requires properties to be loaded first

        setupResourceFiles();

        setupLanguage();

        if (checkFileEnvironment() == false) return;
    }

    protected void setupLanguage()
    {
        // defaults to current platform if newLocale == null
        String localeString = JXConfig.getProperty(JXConfig.LANGUAGE_OVERRIDE);

        if (localeString == null || localeString.equals(JXConfig.LANGUAGE_OVERRIDE_DEFAULT))
        {
            CBIntText.init("language.JX", classLoader);   // i18n support, auto-detecting current platform
        }
        else
        {
            String[] elements = localeString.split("\\.");
            Locale newLocale = null;

            switch (elements.length)
            {
                case 1:
                    newLocale = new Locale(elements[0]);
                    break;
                case 2:
                    newLocale = new Locale(elements[0], elements[1]);
                    break;
                case 3:
                    newLocale = new Locale(elements[0], elements[1], elements[2]);
                    break;
                default:
                    log.severe("unable to parse locale element " + localeString);
            }
            CBIntText.init("language.JX", classLoader, newLocale);
        }
    }

    public static void printTime(String msg)
    {
        long time = System.currentTimeMillis();
        log.info(msg + "\nTIME: " + new Date().toString() + "  (" + (time % 1000) + ")\n");
    }


    /**
     * Set up some common utility ftns for logging and error reporting.
     */
    public void initUtilityFtns()
    {
        if (browsers.size() > 0)
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
    //TODO: figure out what this was supposed to do...
    public static boolean checkFileEnvironment()
    {
        return true;
    }


    /**
     * Initialise the JavaHelp system, pointing it at the right help files.
     */

    protected void setupHelp()
    {
        helpSystem = new CBHelpSystem("JXplorerHelp.hs", new Dimension(1000, 600)); // use default 'JXplorerHelp.hs' help set.
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


    private static String getBrowserSuffix(int browserNo)
    {
        return (browserNo > 0) ? "-" + browserNo : "";
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

            String configFile = CBUtility.getPropertyConfigPath(APPLICATION_NAME, "gssapi.conf");
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
    public void createInitialWindows()
    {
        int numWindows = JXConfig.getIntProperty("windows", 1);
        for (int browserNo = 0; browserNo < numWindows; browserNo++)
        {
            String suffix = getBrowserSuffix(browserNo);

            int width = JXConfig.getIntProperty("width" + suffix);
            int height = JXConfig.getIntProperty("height" + suffix);

            int xpos = JXConfig.getIntProperty("xpos" + suffix);
            int ypos = JXConfig.getIntProperty("ypos" + suffix);

            JXplorerBrowser browser = createNewBrowser(xpos, ypos, width, height);

            int treeWidth = JXConfig.getIntProperty("treewidth" + suffix, 320);
            browser.getSplitPane().setDividerLocation(treeWidth);
        }


    }

    public JXplorerBrowser createNewBrowser()
    {
        int xpos, ypos, width, height;

        try  // try to use the default of whatever the 'root' windows was last
        {
            width = JXConfig.getIntProperty("width");
            height = JXConfig.getIntProperty("height");
        }
        catch (Exception e)
        {
            width = 1000;
            height = 700;  // emergency fallbacks
        }

        // In this unusual case, the centering will be slightly off - but we probably don't care.
        if (width < 100) width = 100;
        if (height < 100) height = 100;

        try
        {
            xpos = JXConfig.getIntProperty("xpos");
            ypos = JXConfig.getIntProperty("ypos");
        }
        catch (Exception e)
        {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            xpos = (screen.width - width) / 2;
            ypos = (screen.height - height) / 2;
        }

        return createNewBrowser(xpos, ypos, width, height);
    }

    public JXplorerBrowser createNewBrowser(int xpos, int ypos, int width, int height)
    {
        JXplorerBrowser browser;

        // look for JXWorkBenchBrowser 'add on' module...

        boolean usingJXW = true;
        if (usingJXW)
        {
            try
            {
                Class c = getClassLoader().loadClass("com.pegacat.jxworkbench.JXWBrowser");
                Constructor constructor = c.getConstructor(new Class[0]);
                browser = (JXplorerBrowser) constructor.newInstance(new Object[0]);
                log.info("JXWorkBench loaded");
            }
            catch (Exception e)            // expected condition - just means JXWorkBench is not available
            {
                e.printStackTrace();
                log.info("JXWorkBench not available - loading standard JXplorer version");
                browser = new JXplorerBrowser();
            }
        }
        else
        {
            browser = new JXplorerBrowser();
        }
        browser.setBounds(xpos, ypos, width, height);
        browser.setSize(width, height); // necessary?
        browser.init(this);

        browsers.add(browser);

        return browser;
    }

    /**
     * Closes the application down
     */

    public void shutdown()
    {
        shutdown(null);
    }

    /**
     * Closes the application down, optionally printing out a message
     *
     * @param msg optional message to be printed out on closing.
     */

    public void shutdown(String msg)
    {
        // this attempts to save the state (size and position) of the current windows, and recreate it
        // the next time JX is started up.

        JXConfig.setProperty("windows", String.valueOf(browsers.size()));

        for (int browserNo = 0; browserNo < browsers.size(); browserNo++)
        {
            JXplorerBrowser browser = browsers.get(browserNo);

            String suffix = getBrowserSuffix(browserNo);

            JXConfig.setProperty("width" + suffix, String.valueOf(((int) browser.getSize().getWidth())));
            JXConfig.setProperty("height" + suffix, String.valueOf(((int) browser.getSize().getHeight())));

            JXConfig.setProperty("xpos" + suffix, String.valueOf(browser.getX()));
            JXConfig.setProperty("ypos" + suffix, String.valueOf(browser.getY()));

            JXConfig.setProperty("treewidth" + suffix, String.valueOf(browser.getSplitPane().getDividerLocation()));

            JXConfig.setProperty("last.search.filter", "default");    //TE: sets the last filter property to 'default' (we don't really need to remember the filter after JX exists).
        }

        JXConfig.writePropertyFile();

        if (msg != null)
            log.severe("shutting down\n" + msg);
        else
            log.warning("shutting down");

        System.exit(0);
    }

    public void browserClosing(JXplorerBrowser closingBrowser)
    {
        if (browsers.size() > 1)
        {
            browsers.remove(closingBrowser);
            initUtilityFtns();   // bounce utility function pane on the offchance the closed window was the one being used for the utilty function 'root pane'
        }
        else
        {
            shutdown();
        }
    }

    /**
     * This is used to trigger visibility changes on secondary browser popup tools, to allow the 'paste' option
     * to become active if a cut or copy selection is made in another browser window...
     *
     * @param active
     */
    public void setPopupToolPasteOptions(boolean active)
    {
        if (browsers.size() > 1)
        {
            for (JXplorerBrowser browser : browsers)
            {
                ButtonRegister br = browser.getButtonRegister();
                br.setItemEnabled(br.PASTE, active);
                br.setItemEnabled(br.PASTE_ALIAS, active);
            }
        }
    }

    /**
     * When JX is first installed we set up some files using default files, if and only if they don't
     * already exist.  This is particularly important if JX is installed into a directory it can't write to (or the config.home directory is set),
     * we need to copy any 'default' templates to the destination directory. This only really needs to be done once,
     * on installation, but we make the check anyway in case files have been blown away for some reason.
     * <p/>
     * Basically we zip through a list of default files, copying them to the config directory, and trimming off any
     * ".default" tags that might be on the end.  If the file is a directory we copy the contents.  In all cases we
     * check for an existing file first, and don't overwrite.
     */
    private void copyDefaultFiles()
    {
        checkAndCopy("csvconfig.txt.default");
        checkAndCopy("security.default");
    }

    /**
     * Takes the source file in the 'local directory', removes any ".default" suffix,
     * and copies it to the 'destination config directory'
     * (which may be the same directory).
     * If it is a directory, recursively copies the contents.
     *
     * @param sourceFileName
     */
    private void checkAndCopy(String sourceFileName)
    {
        File sourceFile = new File(JXConfig.getLocalDirectory() + sourceFileName);

        String destinationFileName = sourceFileName;
        if (sourceFileName.endsWith(".default"))
            destinationFileName = sourceFileName.substring(0, sourceFileName.length() - 8);

        File destinationFile = new File(JXConfig.getConfigDirectory() + destinationFileName);

        if (!destinationFile.isDirectory() && destinationFile.exists())
            return;  // it's already there - nothing to do.

        if (!sourceFile.exists())
        {
            log.warning("Unable to find default file: " + sourceFile + " -> not copying to : " + destinationFile);
            return;
        }

        log.fine("COPYING resource file: " + sourceFile.getAbsolutePath() + " to " + destinationFile.getAbsolutePath());
        recursivelyCopyFile(sourceFile, destinationFile);

    }

    /**
     * Copies source to destination.  If source is a directory, recursively copies the contents.
     * If source is a file, will only copy that file if destination does not exist.
     *
     * @param sourceFile
     * @param destinationFile
     */
    private void recursivelyCopyFile(File sourceFile, File destinationFile)
    {

        try
        {
            if (sourceFile.isDirectory())
            {
                if (!destinationFile.exists())
                    if (!destinationFile.mkdirs())
                    {
                        log.severe("unable to set up destination directory for config files: " + destinationFile.getAbsolutePath());
                        return;
                    }

                String[] children = sourceFile.list();
                for (String child : children)
                {
                    if (!child.startsWith("."))  // skip system files.
                        recursivelyCopyFile(new File(sourceFile + File.separator + child), new File(destinationFile + File.separator + child));
                }
            }
            else
            {
                if (destinationFile.exists())  // do not overwrite existing files.
                    return;

                CBUtility.copyFile(sourceFile, destinationFile);
                System.out.println("copying: " + sourceFile + " -> to : " + destinationFile);
            }
        }
        catch (IOException e)
        {
            log.severe("unable to setup default files; couldn't copy " + sourceFile.getAbsolutePath() + " to " + destinationFile.getAbsolutePath() + "\n error was; " + e.getMessage());
        }
    }

    private static void runBatchJob(String[] args)
    {
        try
        {
            if (args[0].contains("report"))
            {
                JXplorer jx = new JXplorer(true);
                {
                    Class c = jx.getClassLoader().loadClass("com.pegacat.jxworkbench.reports.JXWBatchReport");
                    Constructor constructor = c.getConstructor(new Class[0]);
                    BatchJob reportEngine = (BatchJob) constructor.newInstance(new Object[0]);
                    reportEngine.execute(jx, args);
                }
            }
            else
            {
                printHelpAndExit();
            }    
        }
        catch (Exception e)
        {
            System.out.println("BATCH JOB FAILED DUE TO ERROR");
            e.printStackTrace();
        }
    }

    /**
     * prints a short message for people attempting to use batch mode incorrectly
     */
    private static void printHelpAndExit()
    {
        System.out.println("BATCH MODE USAGE\n" +
                "Currently only the -report option is supported for JXWorkBench batch reporting.\n" +
                "(For more information try -report -help");
    }

}
