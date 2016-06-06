package com.ca.directory.jxplorer.viewer;

import com.ca.commons.cbutil.*;
import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.*;
import com.ca.directory.jxplorer.broker.DataBrokerQueryInterface;
import com.ca.directory.jxplorer.tree.SmartTree;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.ZipException;

/**
 *    Display Template handles the insertion of attribute values into
 *    an HTML template, in order to display filtered information to
 *    the user in an attractive manner.<p>
 *
 *    It also implements a simple web browser for viewing help links
 *    etc.<p>
 *
 *    Form submission is done in the @MyHTMLEditorKit class.
 */


/*  IMPLEMENTATION NOTES....
 *
 *  This is a large class, and should probably be broken up - an MVC model maybe, but
 *  at least separate the functionality families.
 *
 *  It is currently doing three separate things.
 *  A) Operating the Template Display GUI
 *  B) Keeping track of the html templates on disk, and indexing them by object class
 *  C) Parsing the html text and doing tricks (many of which may not be necessary, since they were often
 *     work arounds for buggy sun code in the good 'ol days).
 */

public class HTMLTemplateDisplay extends JPanel
        implements DataSink, PluggableEditor
{
    JScrollPane scrollDisplay;

    DataBrokerQueryInterface currentDataSource = null;

    JEditorPane editor;
    JViewport viewport; // was CBViewport
    JTextArea bloop;

    String baseText;        // the original text, formatted for html but without attribute values
    StringBuffer htmlText;  // the display text, including attributes...
//    String baseURL;			// the base file URL of the template directory location
//    String localURL;		//TE: the path to the users working dir.  Used to store the temporary templates extracted from a zip/jar.
//    URL base = null;		// the same base file URL, as a url.

    Component display;      // a display panel used for user error messages.

    public static final String DEFAULTTEXT = "<html><head><title>Default Template</title></head><body> <dxtemplate:get-all-attributes> <br> </body></html>";
    public static final String ATTRIBTAG = "<dxtemplate:";
    public static String NOVALUEFOUND;
    public static final String JPEGEXTENSION = ".jpg";    	//TE: the extenion of the temporary jpegPhotos stored locally.
    public static final String DOCEXTENSION = ".doc";	    //TE: the extenion of the temporary documents stored locally.
    public static final String XLSEXTENSION = ".xls";	    //TE: the extenion of the temporary spreadsheets stored locally.
    public static final String WAVEXTENSION = ".wav";	    //TE: the extenion of the temporary wav files stored locally.
    public static final String AVIEXTENSION = ".avi";	    //TE: the extenion of the temporary avi files stored locally.
    public static final String MIDEXTENSION = ".mid";	    //TE: the extenion of the temporary mid files stored locally.

    public static final String startFile = "start";    		// the initially displayed html page

    public static final int MAX_LEGAL_VALUE_LENGTH = 500;
    public static final String ILLEGAL_VALUE = "[ATTRIBUTE TOO LARGE TO DISPLAY]";

    public static final String DEFAULT = "defaulttemplate";         // name for default templates...

    JToolBar toolBar;      	// the toolbar containing the list of allowed templates

    /*
     *   A bunch of variables are needed to keep track of all the available html templates,
     *   and the users state when viewing a particular template combination.
     */

    // The combo box the user uses to select a display template
    CBJComboBox viewTemplates;

    // The position in the combo box for a particular 'object class set signature' (i.e. what the user last
    // viewed for a particular object class set).  Keyed by object class signature.
    Hashtable viewTemplatesPos = new Hashtable();   	// hashtable of most-recently-used templates

    // All available templates (as File objects, keyed by lower case object class.  Defaults keyed by empty string "").
    Hashtable templates = new Hashtable(100);

    // the 'object class signature' (the object classes for a particular entry concatenated to a unique key)
    String oldObjectClassesSignature = "";

    // the base directory for all the html templates
    File baseTemplateDir;

    // the base directory for any plugin html templates
    File pluginTemplateDirectory = null;

//    String currentLDAPObjectClass = "";  		// the current object class being viewed
    DXEntry currentEntry = null;          		// the current entry being displayed.
    // (we need to remember this, because the display template changes)
    static String currentTemplateName = "";        	// the name of the currently selected template...

    public static String NODATA;  					// no data html page - not static, 'cause of internationalisation requirements...

    public boolean showHTMLErrors = true; 			// for debugging html forms.

    protected MyHyperlinkListener hyperlinkListener;
    protected MyHTMLEditorKit htmlEditorKit;

    protected boolean settingUpTemplates = false;  	// utility variable to disable combo box action listener during combo box setup.

    Vector currentBinaryAttributes = new Vector(0); // used by MyHTMLEditorKit to flag special data...

    CBResourceLoader resourceLoader = null;        	// used to load HTML templates from zip/jar files.

    protected String currentDN;    					//TE: part of the name of the temporary files that are created for jpegPhoto & audio attributes (i.e. the dn of the entry).

    SmartTree smartTree = null;

    private static Logger log = Logger.getLogger(HTMLTemplateDisplay.class.getName());

    /**
     *    Default Constructor for HTMLTemplateDisplay
     */

    public HTMLTemplateDisplay(Component owner, CBResourceLoader resourceLoader)
    {
        commonConstructorCode(owner, resourceLoader);
        setToDefault();
    }

    protected void setToDefault()
    {
        htmlText = new StringBuffer(DEFAULTTEXT);
        baseText = new String(DEFAULTTEXT);
    }

    /**
     * <p>A bunch of vaguely hacky code to add hyperlink-like functionality to the html display</p>
     */
    class MyHyperlinkListener implements HyperlinkListener
    {
        public void hyperlinkUpdate(final HyperlinkEvent e)
        {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        String desc = e.getDescription();

                        if (desc.toLowerCase().startsWith("dn:")) // el hack especial
                        {
                            String dn = desc.substring(desc.indexOf(":") + 1);

                            if (smartTree != null)
                            {
                                smartTree.readAndExpandDN(new DN(dn));
                            }
                        }

//Ooookay... we've switched to using the java temp directory, so these ../.. style matches don't work any more.  We should be able to just accept the url and launch it?

//                        else if (desc.toLowerCase().startsWith(".." + File.separator + "temp" + File.separator + "audio" + File.separator) || desc.toLowerCase().startsWith(".." + File.separator + ".." + File.separator + "temp" + File.separator + "audio" + File.separator))	//TE: audio handler.
                        else if (desc.startsWith(CBCache.getAudioDirPath()))
                        {//TE: launches audio files...

                            //if (!System.getProperty("os.name").equalsIgnoreCase("SunOS"))	//TE: Check if the operating system is Sun if so don't attempt to play the audio file.
                            if (CBUtility.isWindows())  // CB: actually, I think this stuff only works on windows?
                            {
                                //String path = CBCache.getAudioDirPath();
                                //String audioFilePath = desc.substring(desc.indexOf("temp", desc.lastIndexOf("\"")));
                                //String audioFileName = audioFilePath.substring(audioFilePath.lastIndexOf("\\"));
                                //String extension = audioFileName.substring(audioFileName.indexOf("."));
                                String extension = desc.substring(desc.lastIndexOf("."));

                                if (extension.equalsIgnoreCase(".xxx"))
                                    CBUtility.error(CBIntText.get("Unable to play unknown audio file type"));
                                else
                                //    CBLauncher.launchProgram(extension, path + audioFileName);
                                {
                                    System.out.println("LAUNCHING: " + desc);
                                    CBLauncher.launchProgram(extension, desc);
                                }
                            }
                            else
                                CBUtility.error(CBIntText.get("content only opens under windows"));
                        }
                        //else if (desc.toLowerCase().startsWith(".." + File.separator + "temp" + File.separator) || desc.toLowerCase().startsWith(".." + File.separator + ".." + File.separator + "temp" + File.separator))	//TE: audio handler.
                        else if (desc.startsWith(CBCache.getDirPath()))
                        {//TE: currently launches odDocumentDOC/odSpreadSheetXLS/odMusicMID/odSoundWAV/odMovieAVI files with .doc extension...
                            //if (!System.getProperty("os.name").equalsIgnoreCase("SunOS"))	//TE: Check if the operating system is Sun if so don't attempt to launch the document.
                            if (CBUtility.isWindows())  // CB: actually, I think this stuff only works on windows?
                            {
                                //String path = CBCache.getDirPath();
                                //String filePath = desc.substring(desc.indexOf("temp", desc.lastIndexOf("\"")));
                                //String fileName = filePath.substring(filePath.indexOf("\\"));
                                //String extension = fileName.substring(fileName.indexOf("."));
                                //CBLauncher.launchProgram(extension, path + fileName);

                                String extension = desc.substring(desc.lastIndexOf("."));
                                CBLauncher.launchProgram(extension, desc);

                            }
                            else
                                CBUtility.error(CBIntText.get("content only opens under windows"));

                        }
                        else if (!System.getProperty("os.name").equalsIgnoreCase("SunOS") && desc.toLowerCase().startsWith("mailto"))	//TE: spawn default mail client (windows only).
                        {//TE: launches email client...
                            launchClient(desc);
                        }
                        else
                        {
                            URL url = e.getURL();

// XXX
//      Have had no end of trouble trying to do string matching of paths (shame there sin't a decent OO way of doing it with File
//      objects!).  As a result have switched everything to using 'canonical paths'... hopefully that will work :-). - CB
// XXX


//CB - base no longer used?                            if (base != null && url.getProtocol().equals("file"))  // possibly this is another template...
                            if (url.getProtocol().equals("file"))  // possibly this is another template...
                            {

                                String fullFileName = "";
                                String baseTemplateDirPath = "";
                                String pluginTemplateDirectoryPath = "";
                                try
                                {
                                // Assume this is a template, and attempt to obtain template name from url...
                                    fullFileName = new File(URLDecoder.decode(url.getFile(), "UTF-8")).getCanonicalPath(); // wierd processing to get standard file separators ('/' or '\') (needed for matching below)
                                    baseTemplateDirPath = baseTemplateDir.getCanonicalPath();
                                    if (pluginTemplateDirectory != null)
                                        pluginTemplateDirectoryPath = pluginTemplateDirectory.getCanonicalPath();
                                }
                                catch (IOException e) // not expected...
                                {
                                    log.log(Level.WARNING, "Exception trying to access HTML file urls " + url.getFile().toString(), e);
                                }
                                String fileName = "";

                                if (fullFileName.startsWith(baseTemplateDirPath))
                                {
                                    fileName = fullFileName.substring(baseTemplateDirPath.length());
                                }
//TODO: this needs to be tested -> do urls between plugin templates work...
                                else if (pluginTemplateDirectory!=null && fullFileName.startsWith(pluginTemplateDirectoryPath))
                                {
                                    fileName = fullFileName.substring(pluginTemplateDirectoryPath.length());
                                }

                                if (fileName.startsWith(File.separator))
                                    fileName = fileName.substring(1);

                                fileName = (new File(fileName)).toString();

                                if (templateExists(fileName))
                                {
                                    setNewTemplate(fileName);
                                }
                                else
                                {
                                    openPage(url);
                                }
                            }
                            else
                            {

                                if (Desktop.isDesktopSupported()) {
                                    try {
                                        Desktop.getDesktop().browse(e.getURL().toURI());
                                    } catch (IOException e1) {
                                        // TODO Auto-generated catch block
                                        e1.printStackTrace();
                                    } catch (URISyntaxException e1) {
                                        // TODO Auto-generated catch block
                                        e1.printStackTrace();
                                    }
                                }

                             /*   I don't think this code is used?  And the internal java html display is so bad I don't think we should even try... - CB August '13


                             //TE: if the user has set (via advanced options) to launch URLs in a browser...
                                if ((JXConfig.getProperty("option.url.handling")).equalsIgnoreCase("Launch"))
                                    launchClient(desc);
//TE: otherwise open it in JXplorer...
                                else
                                    openPage(url);
                             */
                            }

                        }
                    }
                });
            }
        }
    };



    /**
     *	Recreates the editor, and sets the text.
     *	@param htmlText the Text (possibly html) to display
     */

    public void setEditorText(String htmlText)
    {
        //System.out.println("-----------\n" + htmlText + "\n----------------\n");

        Dimension current = null;
        if (editor != null)  // reuse editor to avoid memory leak.
                             // TODO: figure out what the JEditorPane doco means about recreating the document
        {
            try
            {
                editor.setText(htmlText);
            }
            catch (Exception e) // can Sun write code?  No, no I don't think they can.  If the editor screws up internally, make it again.
            {
                recreateEditor(current, htmlText);
            }
        }
        else
        {
            recreateEditor(current, htmlText);
        }
        editor.validate();

        // TODO: figure out a way of being able to reuse the viewport without getting exceptions.
        // In Java 1.5 exceptions are thrown more than not when reusing the viewport.
        // Now we will recreated it each time :-S
        try
        {
            viewport.setView(editor);
        }
//        catch (ArrayIndexOutOfBoundsException e) // XXX sun stuff sometimes throws an exception, claiming the viewport has no existing child??
        catch (Exception e) // XXX sun stuff sometimes throws an exception, claiming the viewport has no existing child?? Or things stuff up in the flow layout strategy?
        {
            viewport = new JViewport();
            viewport.setView(editor);
            scrollDisplay.setViewport(viewport);
        }
    }

    /**
     * The Swing editor is a fragile beast, and must be frequently executed because deep inside it is as
     * buggy a piece of crap as Sun ever wrote.
     * @param current
     * @param htmlText
     */
    private void recreateEditor(Dimension current, String htmlText)
    {
        editor = getNewEditor();
        if (current == null) // this occasionally happens.  *shrug*
        {
            current = new Dimension (400,400);
        }
        editor.setSize(current);
        editor.setText(htmlText);
    }


    /**
     *	As advised in the doco, it is apparently better to re-create an
     *  editor pane every time, rather than reuse the old one.  Phenomenal.
     *
     *  Sept '04 - this method causes a memory leak, so calling repeatedly
     * is not advised.  The doco actually advises recreating the document
     * object, however for our purposes we can probably accept the performance
     * hit of tearing down the old document and rebuilding it.  Seems to fix
     * the memory leak anyway :-).
     */

    protected JEditorPane getNewEditor()
    {
        // this fixes the memory leak, but removes the point of the method -
        // better not to call it at all.
        //if (editor != null) { return editor; }

        JEditorPane newEditor = new JEditorPane();

        newEditor.setEditorKitForContentType("text/html", htmlEditorKit);
        newEditor.addHyperlinkListener(hyperlinkListener);
        newEditor.setEditable(false);
        newEditor.setMinimumSize(new Dimension(400, 400));
        newEditor.setContentType("text/html");

        return newEditor;

    }

    /**
     * <p>This code is run by both constructors.  It sets up the GUI and the template lists, and other
     * housekeeping tasks.</p>
     * @param owner
     * @param resourceLoader
     */
    private void commonConstructorCode(Component owner, CBResourceLoader resourceLoader)
    {
        this.resourceLoader = resourceLoader;

        setupTemplateLists();

        initGUI(owner);

        setStartPage();
    }

    /**
     * <p>This searches the template directories at start up, and builds up a list
     * of available templates, keyed by lowercase object class.</p>
     */
    private void setupTemplateLists()
    {
        try
        {
            // unpack any plugin templates (sets 'pluginTemplateDirectory')
            initZipTemplates();

            // Find the root of the /template directory tree
            baseTemplateDir = getBaseTemplateDirectory();
            String baseTemplate = baseTemplateDir.getCanonicalPath();

            // get the list of default html templates that can be used for all object classes
            String[] defaultTemplates = CBUtility.readFilteredDirectory(baseTemplate, new String[]{"html", "htm"});

            addToGlobalTemplateList(defaultTemplates, DEFAULT);

            // do the same for the plugin templates (if any)
            if (pluginTemplateDirectory != null)
            {

                defaultTemplates = CBUtility.readFilteredDirectory(pluginTemplateDirectory.getCanonicalPath(), new String[]{"html", "htm"});
                addToGlobalTemplateList(defaultTemplates, "");
            }

            if (defaultTemplates == null || defaultTemplates.length == 0)
                log.warning("Warning - can't find any default html templates in " + baseTemplate);

            // get the list of object class directories (e.g. /person, /organizationalUnit etc.)
            String[] objectClassFolders = getAllTemplateSubDirectories(baseTemplateDir);
            addIndividualObjectClassTemplates(objectClassFolders, baseTemplate);

            // do the same for the plugin templates (if any)
            if (pluginTemplateDirectory != null)
            {
                objectClassFolders = getAllTemplateSubDirectories(pluginTemplateDirectory);
                addIndividualObjectClassTemplates(objectClassFolders, pluginTemplateDirectory.toString());
            }

            //printTemplates();
        }
        catch (FileNotFoundException e)
        {
            log.warning("Error initialising HTML Template: " + e.toString());
        }
        catch (IOException e2)
        {
            log.warning("Error initialising HTML Template file paths: " + e2.toString());
        }
    }
/*
    private void printTemplates()
    {
            System.out.println("***PRINTING TEMPLATES VARIABLE***");
            Enumeration bloop = templates.keys();
            while (bloop.hasMoreElements())
            {
                String key = (String)bloop.nextElement();
                System.out.println("hashtable: " + key);
                File[] files = (File[]) templates.get(key);
                if (files == null)
                    System.out.println("  NULL !");
                else
                    for (int i=0; i<files.length; i++)
                        System.out.println("  -> " + files[i].toString());
            }
    }
*/
    private void addIndividualObjectClassTemplates(String[] objectClassFolders, String baseTemplate)
    {
        // search the discovered object class directories, adding their html templates to the global hash.
        for (int i = 0; i < objectClassFolders.length; i++)
        {
            String folderName = baseTemplate + File.separator + objectClassFolders[i];
            String objectClass = objectClassFolders[i];
            String[] ocTemplates = CBUtility.readFilteredDirectory(folderName, new String[]{"html", "htm"});
            addToGlobalTemplateList(ocTemplates, objectClass);
        }
    }

    /**
     * <p>Adds a directory of html files (with paths relative to baseTemplateDir) to the global hash, keyed by lower case object class.</p>
     * @param fileNames the individual file names
     * @param folderName the name of the folder, in mixed case, which is also the object class to key by.
     */
    private void addToGlobalTemplateList(String[] fileNames, String folderName)
    {
        if (fileNames == null)  // there may not be any templates at all...
            return;

        String objectClass = folderName.toLowerCase();

        File[] fileList = new File[fileNames.length];
        for (int i = 0; i < fileNames.length; i++)
        {
            if (DEFAULT.equals(folderName))
                fileList[i] = new File(fileNames[i]);
            else
                fileList[i] = new File(folderName, fileNames[i]);
        }

        // add the array of files to the hashtable, keyed by lowercase object class.
        templates.put(objectClass, fileList);
    }

    /**
     * <p>CONSTRUCTOR METHOD: returns all sub directories under the template directory</p>
     * <p>These correspond to the various object classes.</p>
     * @param baseTemplateDir 
     * @return returns all sub directories under the template directory
     */
    private String[] getAllTemplateSubDirectories(File baseTemplateDir)
    {
        String[] childDirectories = baseTemplateDir.list(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                File candidate = new File(dir, name);
                return candidate.isDirectory();
            }
        });

        return childDirectories;
    }

    /**
     * <p>CONSTRUCTOR METHOD: This finds the root of the /templates directory tree, where the
     * html file templates are stored.  (It is also where any templates
     * in .zip resource files are kept.)</p>
     * @return the file location of /templates (usually [jxplorer home]/templates )
     * @throws FileNotFoundException
     */
    private File getBaseTemplateDirectory()
            throws FileNotFoundException
    {
        File baseTemplateDir = new File(Theme.getInstance().getDirTemplates());
        if (!baseTemplateDir.exists())
        {
            log.warning("can't find html template directory " + baseTemplateDir + " - trying to find /templates directory");
            baseTemplateDir = new File(JXConfig.localDir + "templates" + File.separator);
            if (!baseTemplateDir.exists())
            {
                throw new FileNotFoundException("ERROR - Cannot find backup /template directory in " + baseTemplateDir);
            }
        }
        return baseTemplateDir;
    }

    /**
     *	<p>Gets any resourses from a zip/jar file depending on the object class of the entry.
     *	For example, if the oc is person, this checks the zip/jar file for resources in
     *	templates/oc/.  It then extracts these files and puts them in a temporary dir of the
     *	same path within the plugins dir.  For example, jxplorer/plugins/templates/oc/Main.html .
     *
     * <p>This is Trudi code, that has been kidnapped by Chris and mungified...
     * It now unpacks permanently, and only does the writing if needed (checks the
     * date stamps).</p>
     */
    private void initZipTemplates()
    {
        String[] zipTemplates = null;		//TE: stores the names of any resourses found in the zip/jar file.

        File pluginDirectory = new File(JXConfig.getProperty("dir.plugins"));
        if (!pluginDirectory.exists())
            pluginDirectory.mkdirs();

        //zipTemplates = resourceLoader.getPrefixedResources("templates/"+className+"/");//+File.separator);// doesn't wk w file.sep??????

        // read all plugin files with the prefix 'templates/'

        zipTemplates = resourceLoader.getPrefixedResources("templates/");

        if (zipTemplates.length == 0)
            return; // nothing to do!

        pluginTemplateDirectory = new File(pluginDirectory, "templates/");

        // unpack them to the plugins directory.

        int prefixSize = "templates/".length();

        for (int i = 0; i < zipTemplates.length; i++)
        {
            // get the template name as a string and as a (potential) file.
            String templateName = zipTemplates[i];

            if (templateName.length() > prefixSize)
            {
                String templateRelativeFileName = CBParse.replaceAllChar(new StringBuffer(templateName), '/', File.separator);

                File pluginTemplateFile = new File(pluginDirectory + File.separator + templateRelativeFileName);

                try
                {
                    // See if we need to unpack this template file because it doesn't already exist, or is newer than
                    // the cached version.
                    if ((pluginTemplateFile.exists() == false) ||
                            (pluginTemplateFile.lastModified() < resourceLoader.getLastModified(templateName)))
                    {
                        if (templateRelativeFileName.endsWith(File.separator)) // it's a directory - just make it!
                        {
                            pluginTemplateFile.mkdirs();
                        }
                        else
                        {
                            if (!pluginTemplateFile.getParentFile().exists())
                                pluginTemplateFile.getParentFile().mkdirs();

                            byte[] b = resourceLoader.getResource(templateName);	//TE: read the resource into a byte array.

                            try
                            {
                                FileOutputStream output = new FileOutputStream(pluginTemplateFile);
                                output.write(b);
                                output.close();
                            }
                            catch (java.io.IOException e)
                            {
                                CBUtility.error("Problem writing unpacked plugin template file: " + pluginTemplateFile + " to disk \n  ", e);
                            }
                        }
                    }
                }
                catch (ZipException e)
                {
                    CBUtility.error("Problem accessing plugin zip file: " + e);
                }
            }
        }
/*XXX NEEDED???
        if (zipTemplates!=null)
        {	//TE: add any templates to the combo box...
            for(int j=0;j<zipTemplates.length;j++)
            {
                if (zipTemplates[j].endsWith(".html") || zipTemplates[j].endsWith(".htm"))
                {
                    templateNames.add(zipTemplates[j].substring(10));	//TE: cut the prefix 'templates/' off the name of the HTML template for normal oc display handling.
                }
            }
        }
*/
    }

    /**
     * CONSTRUCTOR METHOD: Finds and sets up the start page displayed when JX first loads.  This can be localised.
     * XXX
     * XXX URL MAGIC: This will fail on pathalogical urls, since Sun's file URL Loader can't cope with special
     * XXX            characters in file names (e.g. '#', '@' etc).  We go to insane lengths to handle this in the
     * XXX            template stuff by loading the html text manually via a file reader, and then massaging the
     * XXX            html - but I can't be bothered doing it for this one special case.
     * XXX
     */
    private void setStartPage()
    {

        if (editor == null) editor = getNewEditor();


        String htmldocs = Theme.getInstance().getDirHtmlDocs();
        try
        {
            final String locale = Locale.getDefault().toString();;
            File localeSpecificStartFile = new File(htmldocs + startFile + "_" + locale + ".html");
            if (localeSpecificStartFile.exists() == false)
            {
                localeSpecificStartFile = new File(htmldocs + startFile + ".html");
                if (localeSpecificStartFile.exists() == false)
                {
                    log.info("unable to find locale specific start file: " + localeSpecificStartFile);
                    editor.setText("<html><head><title>JXplorer Start Screen</title></head>" +
                                   "<body><h2><font face=\"arial\">Welcome to JXplorer...</font></h2><p>" +
                                   "<font face=\"arial\">This panel will display the results of your directory browsing and searches.</font></p>" +
                                   "<p><font face=\"arial\">If you need any assistance, use the help option on the menu bar above.</font></p>" +
                                   "</body></html>");
                    validate(); // necessary?
                    return;
                }
            }
            openPage(localeSpecificStartFile.toURL());
            validate();  // necessary?
        }
        catch (IOException e)
        {
            log.warning("unable to open welcome page.  " + e);
        }
    }

    /**
     * <p>CONSTRUCTOR METHOD: Sets up the GUI components; the combo box and the display pane.</p>
     * @param owner the parent GUI to initialise from.
     */
    private void initGUI(Component owner)
    {
        setLayout(new BorderLayout());
        NODATA = "<html><head><title>" + CBIntText.get("No Data") + "</title></head><body><h2><font face=\"arial\">" + CBIntText.get("Select an entry to view data") + "</font></h2></body></html>";
        NOVALUEFOUND = "<i>" + CBIntText.get("No Value Found") + "</i>";
        viewport = new JViewport(); // was CBViewport
        scrollDisplay = new JScrollPane();
        initToolBar();
        add(toolBar, BorderLayout.NORTH);
        add(scrollDisplay);
        htmlEditorKit = new MyHTMLEditorKit(this);
        hyperlinkListener = new MyHyperlinkListener();
        editor = getNewEditor();
        viewport.setView(editor);
        scrollDisplay.setViewport(viewport);
        display = owner;
    }

    /**
     *	Checks whether a particular candidate string corresponds to the name of a template
     *  in the viewTemplates combo box.
     *  @return whether it exists.
     */

    protected boolean templateExists(String candidate)
    {
/*
    	if (candidate.endsWith(".htm"))									// trim .htm?
    		candidate = candidate.substring(0, candidate.length()-4);   // extension to match
    	else if(candidate.endsWith(".html"))						    // with template names
    		candidate = candidate.substring(0, candidate.length()-5);
*/


        for (int i = 0; i < viewTemplates.getItemCount(); i++)
        {
            String name = (String) viewTemplates.getItemAt(i);
            if (name.equalsIgnoreCase(candidate)) return true;          // CASE SENSITIVE CODE
        }
        return false;
    }

    /**
     *    Sets up the initial tool bar.  The toolbar should have different numbers of
     *    components visible, depending on the users privileges and whether
     *    or not they're actively editing the page...
     */

    public void initToolBar()
    {
        toolBar = new JToolBar();
        String[] errorMessage = {CBIntText.get("no templates found")};
        String[] templates = readTemplateNames(new String[]{});
        if (templates == null || templates.length == 0)
            templates = errorMessage;

        viewTemplates = new CBJComboBox(templates);
        viewTemplates.setEditable(false);
        viewTemplates.setAlignmentY(Component.TOP_ALIGNMENT);

        viewTemplates.setToolTipText(CBIntText.get("Select a template to view attributes with"));

        toolBar.add(viewTemplates);

        /*
         *    Add Action Listeners to the different Tool Bar Components
         */
        viewTemplates.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (settingUpTemplates == true) return;              // ignore 'changes' that are simply adding options to combo box.

                if ((e == null) || (e.getSource() == null)) return;     // sanity check
                if (viewTemplates.getSelectedItem() == null) return; // sanity check

                String templateName = viewTemplates.getSelectedItem().toString();
                if (templateName.equalsIgnoreCase(currentTemplateName)) return;   // nothing *really* happened  // CASE SENSITIVE CODE

                setNewTemplate(templateName);

                repaint();
            }
        });

    }


    public void setNewTemplate(String templateName)
    {
        String current = (String) viewTemplates.getSelectedItem();
        if (templateName.equalsIgnoreCase(current) == false)                       // CASE SENSITIVE CODE
        {
            viewTemplates.setSelectedItem(templateName);
        }
        currentTemplateName = templateName;

        openNewTemplate(templateName);
        displayEntry(currentEntry, currentDataSource);         // same old data, different template
    }


    /**
     *   Adds an array of files to the templateNames list (which eventually turns
     *   into the displayed combo box).
     */

    private void addFileListToTemplateNames(ArrayList templateNames, File[] fileList)
    {
        if (fileList == null)
        {
            return;
        }

        for (int i = 0; i < fileList.length; i++)
        {
            templateNames.add(fileList[i].toString());
        }
    }

    /**
     *  Creates the set of html template names corresponding to a given set of objectClassNames.
     */

    public String[] readTemplateNames(String[] objectClassNames)
    {
        //printTemplates();

        // This is the list of available templates that will be displayed in the combo box.
        ArrayList templateNames = new ArrayList();

        // Always add in the defaults...
        addFileListToTemplateNames(templateNames, (File[]) templates.get(DEFAULT));

        // Now find the templates for each known object class, and store those.
        for (int classNo = 0; classNo < objectClassNames.length; classNo++)
        {
            String className = objectClassNames[classNo].toLowerCase();
            addFileListToTemplateNames(templateNames, (File[]) templates.get(className));
        }

        // dump the stored names into a string array
        String[] templates = (String[]) templateNames.toArray(new String[templateNames.size()]);

        Arrays.sort(templates);

        return templates;
    }


    /**
     *    Try to open a new template.  Try both ".html" and ".htm"
     *    extensions if necessary.  Start in subdirectory (for object
     *    class specific templates) and then try the default template
     *    directory for general templates.
     */

    public boolean openNewTemplate(String templateName)
    {
        // Try to open the template file in the normal templates directory
        File templateFile = new File(baseTemplateDir, templateName);
        if (templateFile.exists() == false)
        {
            //... if that fails, try to find it in the plugin directory...
            templateFile = new File(pluginTemplateDirectory, templateName);
            if (templateFile.exists() == false)
                return CBUtility.error(display, CBIntText.get("Can't find html template!") + " " + templateName);
        }

        try
        {
            htmlText = new StringBuffer(CBUtility.readTextFile(templateFile));

            // parse file and set paths correctly
            htmlText = parseNewTemplate(htmlText, templateFile.toURI().toURL());
            baseText = htmlText.toString();  // set the base template text.
        }
        catch (IOException e)
        {
            return CBUtility.error(display, CBIntText.get("Can't read html template!") + " " + templateFile.getAbsolutePath());
        }
        return true;

    }


    /*    Pre-parses the html file, doing hack workarounds to make images
     *    work under broken Java 1.2.0 swing.
     *
     *    All this could be done more aesthetically using the HTMLEditorKit...
     *    But we're on a deadline, and it doesn't seem to work properly; so
     *    we'll hack it by hand for the time being... Especially since the
     *    *(&^ swing HTMLDocument documentBase property doesn't seem to
     *    work for generated documents (i.e. you can't set it!) so we need
     *    to stick in fully qualified base refs to things like images etc...
     */

    public StringBuffer parseNewTemplate(StringBuffer templateText, URL url)
    {
        // XXX DANGER WILL ROBINSON
        // XXX INCREDIBLY SCREWED SUN URL HANDLING
        // XXX file:// HIGHLY UNRELIABLE
        //
        // bottom code is carefully hacked to produce a base url reference
        // that may work on solaris under jre 1.3 as well as normal NT.
        // This can be very easily messed up (for example by naively using
        // suns URL class as a constructor).  Be carefull.

        String baseTagURL = url.getPath();

        // note - force use of 'file://' prefix.  URL constructed version would only
        // have file: prefix, ommitting double slash.  While both versions may work,
        // it seems to be variable...

        //XXX should we be trying to do something clever with the File.toURL() method here?

        baseTagURL = "file://" + baseTagURL.substring(0, baseTagURL.lastIndexOf('/') + 1);

        int headPos = templateText.toString().indexOf("<head>");

        String baseTag = "\n<base href=\"" + baseTagURL + "\">\n";

        templateText.insert(headPos + 7, baseTag);	//TE: was templateText.insert(headPos+6, baseTag); ??? html = <html <base href...> >
        // System.out.println(templateText);
        return templateText;
    }

    /**
     *    Returns a template file given the root path and name of the file.
     *
     *    @param fileNameAndPath the name and path, but <b>not</b> the extension,
     *                           of the template file
     *
     *    @return the File object found.  This may be null, or may not exist; these
     *            conditions must be checked for.
     */

    public File getTemplateFile(String fileNameAndPath)
    {
        String fileName = fileNameAndPath + ".html";
        File templateFile = new File(fileName);
        if (templateFile.exists() == false)    // check file exists, try '.htm' ext. if not
            templateFile = new File(fileNameAndPath + ".htm");
        return templateFile;
    }


    public String[] getObjectClasses(DXAttributes atts)
    {
        try
        {
            Attribute a = atts.getAllObjectClasses();

            if (a == null) return new String[]{};

            DXNamingEnumeration en = new DXNamingEnumeration(a.getAll());
            en.sort();    // alphabetacize.

            String[] ret = en.toStringArray();

            return ret;
        }
        catch (NamingException e)
        {
            log.warning("unable to read object classes in AttributeDisplay: " + e.toString());
        }
        return null;
    }

    /**
     *    Concatenates a string array of alphabetically ordered object Classes into
     *    one long string, providing a unique String for this combination of classes.
     *
     */

    public String getObjectClassSignature(String[] objectClasses)
    {
        String ret = "";
        if (objectClasses == null) return "";
        for (int i = 0; i < objectClasses.length; i++)
            ret += objectClasses[i];
        return ret;
    }


    /**
     *    Checks if the list of object classes has changed.  If it has,
     *    reset the 'oldClassSignature' and return true, otherwise return
     *    false.
     */

    public boolean objectClassesChanged(String classesSignature)
    {
        return !((oldObjectClassesSignature != null) && (oldObjectClassesSignature.equals(classesSignature)));
    }


    public void displayEntry(DXEntry entry, DataBrokerQueryInterface formDataSource)
    {
        if (entry == null || entry.size() == 0)
        {
            setEditorText(NODATA);
            return;
        }

        currentDataSource = formDataSource;  // used by form submission handler in MyHTMLEditorKit.

        currentEntry = entry;

        String[] objectClasses = getObjectClasses(entry);
        if (objectClasses == null)
        {
            log.warning("unable to find any object classes for " + entry.getDN().toString());
            setEditorText(NODATA);
            return;
        }

        setupTemplates(objectClasses, entry);  // change templates if necessary...


        if (entry == null)
        {
            CBUtility.error(this, CBIntText.get("Error: No data for this node!"), null);
            setEditorText(NODATA);
            return;
        }
        displayData(entry);
    }

    /**
     *    Takes a list of object classes, and if the object classes to display have
     *    changed modifies the list of available templates to display only appropriate
     *    templates.
     */
    void setupTemplates(String[] objectClasses, DXEntry entry)
    {
        String objectClassesSignature = getObjectClassSignature(objectClasses);

        // change the template viewing combo box to a new set of templates, if objectClass has changed
        if (objectClassesChanged(objectClassesSignature))
        {
            // remember the current menu position for later
            viewTemplatesPos.put(oldObjectClassesSignature, new Integer(viewTemplates.getSelectedIndex()));
            oldObjectClassesSignature = objectClassesSignature;

            // disable combo box action listener using flag
            settingUpTemplates = true;

            // clear all existing combo box templates
            viewTemplates.removeAllItems();

            // read the list of new templates for the particular object classes this entry has.
            String[] templates = readTemplateNames(objectClasses);

            if ((templates == null) || (templates.length == 0))
            // make sure that we found some!  (We *should* find at least the default templates)
                log.warning("No templates found for objectClasses " + objectClassesSignature);
            else
            {
                // load the combo box with the listed templates, in the order we got them from readTemplateNames
                for (int i = 0; i < templates.length; i++)
                    viewTemplates.addItem(templates[i]);


                if (viewTemplatesPos.containsKey(objectClassesSignature))
                {
                    // If we've viewed this object class set before, try to use the same template as last time.
                    int indexPos = ((Integer) viewTemplatesPos.get(objectClassesSignature)).intValue();
                    if (indexPos < templates.length)
                        viewTemplates.setSelectedIndex(indexPos);
                }
                else
                {
                    // if we *haven't* viewed this object class set before...
                    // ... set the combo box to show the first option ('all') by default
                    viewTemplates.setSelectedIndex(0);

                    // ... and try to set the view to the most specific available template
                    attemptToSetOCSpecificTemplate(entry, templates);
                }
                String templateName = viewTemplates.getSelectedItem().toString();
                openNewTemplate(templateName);
            }
            settingUpTemplates = false;		// re-enable combo box action listener
        }
    }

    /**
     *    This attempts to match a list of template names with the deepest possible
     *    object class.
     */

    protected void attemptToSetOCSpecificTemplate(DXAttributes entry, String[] templates)
    {
        //String baseOC = ((DXAttributes)entry).getBaseObjectClass();
        ArrayList<String> ocs = (entry).getOrderedOCs();

        for (int i = 0; i < ocs.size(); i++)
        {
            String oc = ((String) ocs.get(i)).toLowerCase();
            for (int j = 0; j < templates.length; j++)
            {
                String template = templates[j].toLowerCase();
                if (template.startsWith(oc))
                {
                    settingUpTemplates = true;
                    viewTemplates.setSelectedIndex(j);
                    settingUpTemplates = false;
                    return;
                }
            }
        }
    }


    /**
     *	Checks if the HTML page has any custom tags such as...
     *	<p><dxtemplate:get-all-attributes style="list"/></p>
     *	<p><dxtemplate:get-attribute name="cn" style="list"/></p>
     *	if so it enters the data.  If the tag is 'get-all-attributes', all
     *	the attributes are included in the page.  If the tag is 'get-attribute', the
     *	specific attribute is displayed.
     *	This method also kicks off the handling of inserting form data.
     *	@param entry the entry that we are trying to display.
     */

    protected void displayData(DXEntry entry)
    {
        if (entry == null)
        {
            setEditorText(NODATA);
            return;
        }

        int tagstart = 0;        				// the start of the ATTRIBTAG html tag
        int tagend;            				// the end of the ATTRIBTAG html tag

        //int htmlTagLen = ATTRIBTAG.length();

        htmlText = new StringBuffer(baseText);

        // check if doco is XHTML, and try to convert to HTML 3.2 by replacing all end tags '/>' with '>'
        htmlText = parseXHTML(htmlText);


        mediaCheck(entry, "jpegPhoto");			//TE: checks for jpegPhoto and creates temporary file that can be later displayed in a template.
        mediaCheck(entry, "audio");				//TE: checks for audio and creates temporary file that can be later displayed in a template.
        mediaCheck(entry, "odDocumentDOC"); 	//TE: checks for odDocumentDOC and creates temporary file that can be later displayed in a template.
        mediaCheck(entry, "odSpreadSheetXLS"); 	//TE: checks for odSpreadSheetXLS and creates temporary file that can be later displayed in a template.
        mediaCheck(entry, "odMusicMID"); 		//TE: checks for odMusicMID and creates temporary file that can be later displayed in a template.
        mediaCheck(entry, "odSoundWAV"); 		//TE: checks for odSoundWAV and creates temporary file that can be later displayed in a template.
        mediaCheck(entry, "odMovieAVI"); 		//TE: checks for odMovieAVI and creates temporary file that can be later displayed in a template.


        tagstart = htmlText.indexOf(ATTRIBTAG, tagstart);
        while (tagstart >= 0)                              //TODO: this is all a bit ad-hoc and hacky.  If people start using these in earnest we should rewrite this section
        {
            tagend = htmlText.indexOf(">", tagstart);
            String tempTag = htmlText.substring(tagstart, tagend);
            try
            {
                if (tempTag.indexOf("name='")>-1)
                    tempTag = tempTag.replaceAll("'", "\"");

                String attName = "";
                if (tempTag.indexOf("get-all-attributes") > 0)		//TE: splat all attribute values onto the page.
                    attName = "all";
                else											//TE: get the name of the specific attribute to splat.  The next line is basically just getting the attribute name between the quotes e.g. 'cn' from name="cn".
                    attName = tempTag.substring(tempTag.indexOf("name=\"") + 6, tempTag.indexOf("\"", tempTag.indexOf("name=\"") + 6));

                String modifier = null;

                if (tempTag.indexOf("style=\"") > 0)				//TE: get the name of the specific attribute to splat.  The next line is basically just getting the style of display from between the quotes e.g. 'list' from style="list".
                    modifier = tempTag.substring(tempTag.indexOf("style=\"") + 7, tempTag.indexOf("\"", tempTag.indexOf("style=\"") + 7));

                htmlText.delete(tagstart, tagend + 1);			//TE: remove the tag, we don't need it anymore.
                if (attName.equalsIgnoreCase("all"))  			//TE: make new tags for all the attributes and splat them into where the old tag use to be.
                    htmlText.insert(tagstart, formattedAllAttributes(currentEntry, modifier));
                else											//TE: make a new tag for the attribute specified and splat it into where the old tag use to be.
                    htmlText.insert(tagstart, formattedAttribute(attName, currentEntry, modifier));	//TE: splat certain attributes.

                //TE: moves the position to the beginning of the next custom tag, if there is one and...
                //TE: breaks the loop if there are no more tags i.e. tagstart would =-1.
                tagstart = htmlText.indexOf(ATTRIBTAG, tagstart);
            }
            catch (Exception e)
            {
                htmlText.delete(tagstart, tagend + 1);
                String errorMsg = "error parsing {" + tempTag + "}";
                htmlText.insert(tagstart, errorMsg);
                tagstart = htmlText.indexOf(ATTRIBTAG, tagstart + errorMsg.length());
            }

        }
        if ((htmlText == null) || (htmlText.length() == 0))
        {
            log.warning("HTMLTemplateDisplay:displayNodeData - bad html String " + ((htmlText == null) ? " (is null!)" : ""));
            setEditorText(NODATA);
            return;
        }

        String htmlString = htmlText.toString().trim();

        htmlString = insertFormData(htmlString, entry);


        try
        {
            setEditorText(htmlString);
        }
        catch (EmptyStackException e)    // the amazingly buggy HTML editor has done it again...
        {
            log.warning("Another Bug in Sun HTML Component: " + e);
        }

        scrollDisplay.getVerticalScrollBar().setValue(0);   //TE: sets the scroll bar back to the top.
        viewport.setViewPosition(new Point(0, 0));           //TE: sets the view back to the top.
    }


    /**
     *    Checks if there is any jpegPhoto, audio or odDocumentDOC attributes in the entry if so creates temporary files for them
     *    only if there isn't any already.  Gets the byte[] of the jpegPhoto, audio or odDocumentDOC attributes of the entry being
     *    displayed.  Makes a temp directory called 'temp'. Lists all files in the directory, if there are
     *    any temp files for the jpegPhoto, audio or odDocumentDOC attributes in question it does not recreate them.  Otherwise, if
     *    there isn't any temp files for the entry, it goes ahead and makes them.  The temp files are named
     *    as followed: dn+unique number+.jpg.  The temp files and directory are removed when the JX exists.
     *    @param entry the entry that is to be displayed.
     *	 @param type the type of media being checked for (jpegPhoto, audio or odDocumentDOC).
     */

    protected void mediaCheck(DXEntry entry, String type)
    {
        DXAttribute attribute = null;
        attribute = (DXAttribute) entry.get(type);

        if (attribute == null)
            return;

        currentDN = entry.getDN().toString();    				//TE: the name of the dn becomes the main part of the temporary jpegPhoto or audio file.
        currentDN = Integer.toString(currentDN.hashCode());		//TE: make a hash code of it.

        int size = attribute.size();    						//TE: the amount of jpegPhoto or audio attributes in the entry.

        if (attribute != null)
        {
            CBCache.createCache(entry.getDN().toString(), entry, type, size);   //TE: creates a cache for the jpegPhoto or audio file.
        }
    }



    /**
     *	Changes XHTML into standard HTML for display.
     *  (Currently all this involves is changing any string '/&gt;' into '&gt;').
     */
    // (Currently all this involves is changing any string '/>' into '>').

    static public StringBuffer parseXHTML(StringBuffer html)
    {
        return CBParse.replaceAllBufferString(html, "/>", ">");
    }

    /**
     *    This method looks for form elements, and fills in the initial form values based on
     *    the entry data received.  It is fairly picky about white space - it prefers there to
     *    be minimal whitespace in the form html.  Also, there is minimal form validation; an
     *    incorrect form will cause all sorts of mess.<p>
     *    Note that if <i>anything</i> is wrong with the form, this will simply fail with
     *    an exception (probably a substring exception).
     *
     */

    // XXX This should be rewritten to correctly use the HTML Document object model, but I don't
    // XXX have time right now to work out how to do that :-)   - CB

    protected String insertFormData(String htmlString, DXEntry entry)
    {
        if (htmlString.indexOf("<form") < 0)
        {
            if (currentBinaryAttributes.size() > 0)
                currentBinaryAttributes.clear();
            return htmlString;  // no forms today.
        }

        try
        {
            htmlString = insertFormInputData(htmlString, entry);
            htmlString = insertFormSelectData(htmlString, entry);
            htmlString = insertFormTextAreaData(htmlString, entry);
            htmlString = insertFormImageData(htmlString, entry);    //TE: inserts jpegPhotos into the html template.
            htmlString = insertFormAudioData(htmlString, entry);	//TE: inserts a link (or links) to temporary audio files which should be played by the systems default player depending on the extension of the file.
        }
        catch (Exception e)  // usually simply that the value can't be found.
        {
            if (showHTMLErrors)
            {
                log.warning("Error parsing form html for value insertion in HTMLTemplateDisplay. \n  " + e);
                e.printStackTrace();
            }
        }

        return htmlString;
    }

    // XXX This should be rewritten to correctly use the HTML Document object model,
    protected String getTagValue(String tagName, String tag)
    {
        tag = tag.toLowerCase();
        tagName = tagName.toLowerCase();
        try
        {
            int start = tag.indexOf(tagName) + tagName.length();
            start = tag.indexOf("\"", start) + 1;
            if (start < 0) return null;
            int end = tag.indexOf("\"", start);
            if (end < 0) return null;
            String val = tag.substring(start, end);
            return val.trim();
        }
        catch (Exception e)
        {
            if (showHTMLErrors)
                log.warning("error parsing: " + tagName + "\n  " + e);
            return null;
        }
    }



    /**
     *    Goes through the html template, checking for any field tags of text type for example:
     *    <p>
     *        &#60tr&#62
     *            &#60th align="right" width="200"&#62Common Name:&#60/th&#62
     *            &#60td width="230"&#62&#60input type="text" name="cn" value=""/&#62&#60/td&#62
     *        &#60/tr&#62
     *    <p>
     *    If one is found, it checks the name and gets (in this example) the cn value from the entry and inserts
     *    it into the value part of the tag (for example value="Trudi").  If the attribute is multivalued, the
     *    input tag (&#60input type="text" name="cn" value=""/&#62) is copied and reused until all of the attribute
     *    values are inserted.  For layout, a &#60/br&#62 is added to the end of multivalued attribute tags.  The tag
     *    is then inserted back into the html template and returned.
     *    @param htmlString the actual html file that is to be displayed.
     *    @param entry the current entry that the html file is trying to display.
     *    @return the updated html file (updated with the values of the text fields that are to be displayed).
     */

    // XXX This should be rewritten to correctly use the HTML Document object model,
    protected String insertFormInputData(String htmlString, DXEntry entry)
    {
        int tagStart,tagEnd,pos = 0;
        StringBuffer multiValuedTag = new StringBuffer(0);

        while ((pos = htmlString.indexOf("<input", pos)) >= 0)
        {
            tagStart = pos;
            tagEnd = htmlString.indexOf(">", pos);

            String tag = htmlString.substring(tagStart, tagEnd + 1);

            String type = getTagValue("type", tag);
            String name = getTagValue("name", tag);

            DXAttribute attribute = null;
            attribute = (DXAttribute) entry.get(name);    //TE: get the attribute values that are being processed.

              int size = 0;

            if (attribute != null)
                size = attribute.size();    //TE: get the number of values the attribute has so we can tell if it is multivalued.

            if ("text".equalsIgnoreCase(type) && name != null && attribute != null)    //TE: if the input type is 'text', and the input name is not null, and the attribute is not null.
            {
                if (size == 1)	//TE: was attribute.size() -> throws a null pointer exception.
                    tag = insertFormValue(tag, entry, name);    //TE: if there is only one value process it normally.
                else if (size > 1)	//TE: multivalued.
                {
                    for (int i = 0; i < size; i++)
                    {   //TE: make a new tag for each attribute value (only used for multivalued attributes),
                        //    for example: <input type="text" name="telephoneNumber" value="9727 8941"/><br/>.
                        //    Then append the tag to a string buffer.
                        multiValuedTag.append(new String(insertFormValue(tag, entry, name, i) + "<br>"));
                    }
                    tag = multiValuedTag.toString();
                    multiValuedTag.setLength(0);
                }
            }
            else if ("hidden".equalsIgnoreCase(type) && name != null)
                tag = insertFormValue(tag, entry, name);
            else if ("password".equalsIgnoreCase(type) && name != null && attribute != null)
            {
                if (attribute.size() == 1)
                    tag = insertFormValue(tag, entry, name);
                else if (attribute.size() > 1)
                {
                    for (int i = 0; i < size; i++)
                    {   //TE: make a new tag for each password value (only used for multivalued attributes),
                        //    for example: <input type="text" name="userPassword" value="*****"/><br/>.
                        //    Then append the tag to a string buffer.
                        multiValuedTag.append(new String(insertFormValue(tag, entry, name, i) + "<br>"));
                    }
                    tag = multiValuedTag.toString();
                    multiValuedTag.setLength(0);
                }
            }
            pos = tagStart + tag.length(); // reset pos because tag may have grown larger and may now contains multiple html tags.
            htmlString = htmlString.substring(0, tagStart) + tag + htmlString.substring(tagEnd + 1);    //TE: insert the tag into the htmlString.
        }
        return htmlString;
    }

    // XXX This should be rewritten to correctly use the HTML Document object model,
    protected String insertFormSelectData(String htmlString, DXEntry entry)
    {
        int tagStart,tagEnd,pos = 0;
        while ((pos = htmlString.indexOf("<select", pos)) >= 0)
        {
            tagStart = pos;
            tagEnd = htmlString.indexOf("</select>", pos)+"</select>".length();

            String tag = htmlString.substring(tagStart, tagEnd);

            String name = getTagValue("name", tag);

            if (name != null)
            {
                try
                {
                    Attribute a = entry.get(name);
                    if (a == null || a.get() == null) return htmlString;  // no pre-existing value, so nothing to do.

                    String entryValue = ((String) a.get()).toLowerCase();

                    // Convert to lower case
                    String lowerCaseTag = tag.toLowerCase();
                    // Remove seleced attribute
                    String selectedRemovedTag = lowerCaseTag.replaceAll("[ \\t]*\\bselected\\b[ \\t]*", "");
                    // Add selected attribute to the pre-existing value
                    String resultTag = selectedRemovedTag.replaceAll("value[ \\t]*=[ \\t]*([\"']*)\\b"+entryValue+"\\b\\1[ \\t]*", "value=\""+entryValue+"\" selected");
                    if (selectedRemovedTag.length()!=resultTag.length()) {
                        htmlString = htmlString.substring(0, tagStart) + resultTag + htmlString.substring(tagEnd);
                        tagEnd=tagEnd-tag.length()+resultTag.length();
                    }
                }
                catch (Exception e)
                {
                    if (showHTMLErrors)
                    {
                        e.printStackTrace();
                        log.warning("Error getting value for " + name + " value :\n " + e);            // not a terminal error.
                    }
                }
            }

            pos = tagEnd;
        }
        return htmlString;
    }



    /**
     *    Goes through the html template, checking for any text area tags for example:
     *    <p>
     *        &#60tr&#62
     *          &#60th align="right" valign="top"  width="200"&#62Address:&#60/th&#62
     *          &#60td width="230"&#62&#60textarea name="postalAddress" rows="4"&#62&#60/textarea&#62&#60/td&#62
     *        &#60/tr&#62
     *    <p>
     *    If one is found, it checks the name and gets (in this example) the postalAddress value from the entry and inserts
     *    it into the value part of the tag (for example value="21 Jump Street").  If the attribute is multivalued, the
     *    input tag (&#60textarea name="postalAddress" rows="4"&#62&#60/textarea&#62) is copied and reused until all of the attribute
     *    values are inserted.  For layout, a &#60/br&#62 is added to the end of multivalued attribute tags.  The tag
     *    is then inserted back into the html template and returned.
     *    @param htmlString the actual html file that is to be displayed.
     *    @param entry the current entry that the html file is trying to display.
     *    @return the updated html file (updated with the values of the text fields that are to be displayed).
     */

    // XXX This should be rewritten to correctly use the HTML Document object model,
    protected String insertFormTextAreaData(String htmlString, DXEntry entry)
    {
        int tagStart,tagEnd,pos = 0;
        while ((pos = htmlString.indexOf("<textarea", pos)) >= 0)
        {
            tagStart = pos;
            tagEnd = htmlString.indexOf(">", pos);
            String tag = htmlString.substring(tagStart, tagEnd);

            String name = getTagValue("name", tag);

            DXAttribute attribute = null;
            attribute = (DXAttribute) entry.get(name);    //TE: get the attribute values that are being processed.

            int size = 0;

            if (attribute != null)
                size = attribute.size();    //TE: get the number of values the attribute has so we can tell if it is multivalued.

            int nextTag = htmlString.indexOf("</textarea>", tagEnd);

            if (nextTag > 0)
            {
                String betweenTagText = htmlString.substring(tagEnd + 1, nextTag);
                if (betweenTagText.trim().length() == 0) // i.e. nothing but white space
                {
                    if (size == 1)
                    {
                        String text = getAttValue(name, entry);
                        if (text != null && attribute != null) //&& name.toLowerCase().indexOf("address")>0	//TE: removed this to allow any attribute to use a text area.
                        {
                            //Only replace '$' in address fields..
                            if (name.toLowerCase().indexOf("address") > 0)
                                text = text.replace('$', '\n');

                            htmlString = htmlString.substring(0, tagEnd + 1) + text + htmlString.substring(nextTag);
                        }
                    }
                    else if (size > 1)	//TE: multivalued.
                    {
                        String text;
                        String textAreaStartTag = htmlString.substring(tagStart, tagEnd + 1);    //TE: <textarea name="postalAddress" rows="4">
                        String textAreaEndTag = htmlString.substring(tagEnd + 1, htmlString.indexOf(">", tagEnd + 1) + 1);    //TE: </textarea>

                        StringBuffer multiValuedTag = new StringBuffer();
                        for (int i = 0; i < size; i++)
                        {   	//TE: make a new tag for each attribute value (only used for multivalued attributes),
                            //    for example: <textarea name="postalAddress" rows="4">21 Jump Street \nJump Haven</textarea><br/>.
                            //    Then append the tag to a string buffer.
                            text = getAttValue(name, entry, i);
                            text = text.replace('$', '\n');
                            multiValuedTag.append(new String(textAreaStartTag + text + textAreaEndTag + "<br>"));    //TE: <textarea name="postalAddress" rows="4">21 Jump Street</textarea>
                        }

                        String multiTag = multiValuedTag.toString();
                        htmlString = htmlString.substring(0, tagStart) + multiTag + htmlString.substring(nextTag);
                    }
                }
            }
            pos = tagEnd;
        }
        return htmlString;
    }


    /**
     *    Inserts jpegPhoto values into the html file/template.  It searches the html file (which is the parameter:
     *    "htmlString") for the jpegPhoto tag (or flag): "Image N/A". If there is one or more audio values in the
     *	 entry it removes the whole tag (or flag) "Image N/A", and makes a string buffer containing the new tags of all of
     *    the jpegPhoto attributes that pertain to the current entry.
     *    <p>
     *        For example:
     *            &#60br/&#62&#60img src="..\..\temp\cn=Allison MOODY,o=DEMOCORP,c=AU0.jpg" border="1" /&#62
     *            &#60br/&#62&#60img src="..\..\temp\cn=Allison MOODY,o=DEMOCORP,c=AU1.jpg" border="1" /&#62
     *            &#60br/&#62&#60img src="..\..\temp\cn=Allison MOODY,o=DEMOCORP,c=AU2.jpg" border="1" /&#62
     *    <p>
     *    It gets the names of the files from searching the temp directory for names that begin with the DN of the current
     *    entry and end in '.jpg'.  The string buffer is inserted in the place of the removed image tag (the last "&#62" is removed), and then returned.
     *    <p>
     *    The html files/templates reside in jxplorer>templates>inetOrgPerson (inetOrgPerson being the only object class that
     *    contains the jpegPhoto attribute that we provide templates for) whereas the temp directory resides in the jxplorer
     *    directory.  This is why the image tag needs "..\..\temp\".
     *    @param htmlString the actual html file that is to be displayed & that the jpegPhoto image is to be set.
     *    @param entry the current entry that the html file is trying to display.
     *    @return the updated html file (updated with the names and locations of the images that are to be displayed).
     */

    protected String insertFormImageData(String htmlString, DXEntry entry)
    {
        int tagStart,tagEnd,pos = 0;

        DXAttribute attribute = null;
        attribute = (DXAttribute) entry.get("jpegPhoto");    					//TE: gets the jpegPhoto attributes of the current entry.

        while ((pos = htmlString.indexOf("Image N/A", pos)) >= 0)               //  Aaargh... This is pretty magical...! TODO: switch to using <dxattribute> syntax?
        {
            tagStart = pos;
            tagEnd = htmlString.indexOf("<", pos);

            String tag = htmlString.substring(tagStart, tagEnd);    			//TE: tag = e.g. Image N/A.

            if ("Image N/A".equalsIgnoreCase(tag) && tag != null && attribute != null)
            {
                if (attribute.size() != 0)
                {
                    int tagPos = htmlString.indexOf(tag);    					//TE: gets the position of the image tag.

                    String htmlCopyStart = htmlString.substring(0, tagPos);    	//TE: makes a substring of the beginning of the htmlString, up to where the image tag starts.
                    String htmlCopyEnd = htmlString.substring(htmlString.indexOf("<", tagPos));    	//TE: makes a substring of the end of the htmlString, from the end of the image tag to the end of the htmlString.

                    File fileDir = CBCache.getCacheDirectory();    				//TE: get the temp directory.

                    File[] imageFiles = fileDir.listFiles(new FilenameFilter()
                            {
                                public boolean accept(File dir, String name)
                                {
                                    return name.endsWith(JPEGEXTENSION);
                                }
                            });

                    StringBuffer imageTags = new StringBuffer();

                    for (File imageFile:imageFiles)
                    {
//                        imageTags.append(new String("<img src=\"" + imageFile.getAbsolutePath() + "\" border=\"1\"><br>"));    //TE: the new image tag.  // CB - note 'toURI()' prepends 'file:' to the path
                        String imageTag = "<img src=\"" + imageFile.toURI() + "\" border=\"1\"><br>";
                        imageTags.append(imageTag);    //TE: the new image tag.
                    }

                    htmlString = htmlCopyStart + imageTags.toString() + htmlCopyEnd;    				//TE: the htmlString with the image tags updated.


/*
                    String[] allFiles = fileDir.list();    					//TE: get the files in the temp directory.

                    String[] currentFiles = new String[allFiles.length];

                    StringBuffer imageTags = new StringBuffer();

                    int x = 0;    												//TE: used as a separate counter by the "currentFiles" array.

                    for (int i = 0; i < allFiles.length; i++)
                    {
                        //TE: checks all of the files in the temp directory.  If any begin with the DN of the current
                        //    entry and end with ".jpg", they are added to a new string buffer of image tags.
                        if (allFiles[i].startsWith(currentDN) && allFiles[i].endsWith(JPEGEXTENSION))
                        {
                            currentFiles[x] = allFiles[i].toString();

                            imageTags.append(new String("<img src=\".." + File.separator + ".." + File.separator + "temp" + File.separator + currentFiles[x] + "\" border=\"1\"><br>"));    //TE: the new image tag.
                            x++;
                        }
                    }
                    htmlString = htmlCopyStart + imageTags.toString() + htmlCopyEnd;    				//TE: the htmlString with the image tags updated.
*/
                }
            }
            pos = tagEnd;
        }
        return htmlString;
    }


    /**
     *    Inserts audio hyperlinks into the html file/template.  It searches the html file (which is the parameter:
     *    "htmlString") for the audio tag (or flag): "Audio N/A". If there is one or more audio values in the
     *	 entry it removes the whole tag (or flag) "Audio N/A", and makes a string buffer containing the new tags of all of
     *    the audio attributes that pertain to the current entry.
     *    <p>
     *        For example:
     *            &#60a href="..\..\temp\cn=Allison MOODY,o=DEMOCORP,c=AU0.wav"&#62Sound File&#60a/&#62&#60br/&#62
     *            &#60a href="..\..\temp\cn=Allison MOODY,o=DEMOCORP,c=AU1.wav"&#62Sound File&#60a/&#62&#60br/&#62
     *            &#60a href="..\..\temp\cn=Allison MOODY,o=DEMOCORP,c=AU2.wav"&#62Sound File&#60a/&#62&#60br/&#62
     *    <p>
     *    It gets the names of the files from searching the temp directory for names that begin with the DN of the current
     *    entry & that don't end in '.jpg'.  The string buffer is inserted in the place of the removed audio tag , and then returned.
     *    <p>
     *    The html files/templates reside in jxplorer>templates>inetOrgPerson (inetOrgPerson being the only object class that
     *    contains the audio attribute that we provide templates for) whereas the temp directory resides in the jxplorer
     *    directory.  This is why the image tag needs "..\..\temp\".
     *    @param htmlString the actual html file that the audio hyperlinks need to be inserted into.
     *    @param entry the current entry that the html file is trying to display.
     *    @return the updated html file (updated with the names and locations of the audio files that are to be hyperlinked to).
     */

    protected String insertFormAudioData(String htmlString, DXEntry entry)
    {
        int tagStart,tagEnd,pos = 0;

        DXAttribute attribute = null;
        attribute = (DXAttribute) entry.get("audio");    			//TE: gets the audio attributes of the current entry.

        while ((pos = htmlString.indexOf("Audio N/A", pos)) >= 0)
        {
            tagStart = pos;
            tagEnd = htmlString.indexOf("<", pos);

            String tag = htmlString.substring(tagStart, tagEnd);    //TE: tag = e.g. 'Audio N/A'.

            if ("Audio N/A".equalsIgnoreCase(tag) && tag != null && attribute != null)
            {
                if (attribute.size() != 0)
                {
                    int tagPos = htmlString.indexOf(tag);    		//TE: gets the position of the audio tag.

                    String htmlCopyStart = htmlString.substring(0, tagPos);    							//TE: makes a substring of the beginning of the htmlString, up to where the audio tag starts.
                    String htmlCopyEnd = htmlString.substring(htmlString.indexOf("<", tagPos));    	//TE: makes a substring of the end of the htmlString, from the end of the audio tag to the end of the htmlString.

                    File fileDir = CBCache.getAudioCacheDirectory();    	//TE: get the temp directory.
                    String[] allFiles = fileDir.list();    				//TE: get the files in the temp directory.

                    String[] currentFiles = new String[allFiles.length];

                    StringBuffer audioTags = new StringBuffer();

                    int x = 0;    									//TE: used as a separate counter by the "currentFiles" array.

                    for (int i = 0; i < allFiles.length; i++)
                    {
                        //TE: checks all of the files in the temp directory.  If any begin with the DN of the current
                        //    entry and don't end with ".jpg", they are added to a new string buffer of audio tags.
                        if (allFiles[i].startsWith(currentDN) && !allFiles[i].endsWith(JPEGEXTENSION))
                        {
                            currentFiles[x] = allFiles[i].toString();
                            String audiotag = "<a href=\"" + CBCache.getAudioDirPath() + currentFiles[x] + "\">Sound File</a><br>";

                            //System.out.println("AUDIO TAG: " + audiotag);
                            
                            audioTags.append(audiotag);    //TE: the new image tag.
                            x++;
                        }
                    }
                    htmlString = htmlCopyStart + audioTags.toString() + htmlCopyEnd;    				//TE: the htmlString with the audio tags updated.
                }
            }
            pos = tagEnd;
        }
        return htmlString;
    }


    /**
     *    Inserts the attribute value into the tag.
     *    @param tag the html tag that the value needs to be inserted into e.g. &#60input type="text" name="cn" value=""/&#62.
     *    @param entry the current entry (or the entry that needs to be displayed).
     *    @param name the name of the attribute (e.g. 'cn').
     *    @return the tag with the value inserted.
     */

    protected String insertFormValue(String tag, DXEntry entry, String name)
    {
        String newTag = insertFormValue(tag, entry, name, 0);    //TE: not a multivalued attribute therefore get attribute at position 0.
        return newTag;
    }


    /**
     *    Inserts the attribute value into the tag.
     *    @param tag the html tag that the value needs to be inserted into e.g. &#60input type="text" name="cn" value=""/&#62.
     *    @param entry the current entry (or the entry that needs to be displayed).
     *    @param name the name of the attribute (e.g. 'cn').
     *    @param position the position of the value to be inserted (an attribute may be multivalued).
     *    @return the tag with the value inserted.
     */

    // XXX This should be rewritten to correctly use the HTML Document object model,
    protected String insertFormValue(String tag, DXEntry entry, String name, int position)
    {
        String val = getTagValue(" value", tag);

        if (val == null) return tag;  // no value tag - nothing to do.
        if (val.trim().length() > 0) return tag;  // value preset - don't change it!

        // o.k., we have an empty value.  Let's fill it in.

        int pos = tag.indexOf(" value");
        pos = tag.indexOf("\"", pos) + 1;
        int end = tag.indexOf("\"", pos);

        String entryValue = getAttValue(name, entry, position);    //TE: get the attribute's value at the specified position.
        if (entryValue != null)
            tag = tag.substring(0, pos) + entryValue + tag.substring(end);

        return tag;
    }


    /**
     *    Get the value of an attribute corresponding to a particular name.
     *    @param name the name of the attribute(e.g. 'cn').
     *    @param entry the current entry (or the entry that needs to be displayed).
     *    @return the attribute value (e.g. 'frank').
     */

    protected String getAttValue(String name, DXEntry entry)
    {
        String newTag = getAttValue(name, entry, 0);    //TE: not a multivalued attribute therefore get attribute at position 0.
        return checkLength(newTag);
    }

    /**
     *    Returns ILLEGAL_VALUE if MAX_LEGAL_VALUE_LENGTH is exceeded.
     *    @param checkString a string which may be excessively long
     *    @return the checkString, or an error string if it was too long.
     */

    private String checkLength(String checkString)
    {
        return (checkString.length() > MAX_LEGAL_VALUE_LENGTH) ? ILLEGAL_VALUE : checkString;
    }


    /**
     *    <p>Get the value of an attribute corresponding to a particular name.</p>
     *
     *    <p>For simplicity elsewhere in the code (!) we make sure that for naming
     *    attributes the first
     *    value (in position 0) is *always* the naming value.</p>
     *
     *    @param name the actual attribute name (e.g. 'cn').
     *    @param entry the current entry (or the entry that needs to be displayed).
     *    @param position the position of the value to be inserted (an attribute may be multivalued).
     *    @return the attribute value (e.g. 'frank').
     */

    protected String getAttValue(String name, DXEntry entry, int position)
    {
        int numberValues;             // how many values the attribute has.

        if (name == null) return "No name given in HTMLTemplateDisplay.getAttValue()";


        try
        {
            Attribute a = entry.get(name);
            if (a == null) return null;  // no pre-existing value, so nothing to do.
            numberValues = a.size();
            if (numberValues == 0 || a.get() == null) return null;  // no pre-existing value, so nothing to do.
            if (numberValues <= position) return null; // can't do that.

            Object attValue = a.get(position);    //TE: get the attribute value at the specified position.

            // XXX WARNING - HERE BE MAGIC
            /*
             *    We want to ensure that the naming value is always the first displayed.
             *    So we check if it is a naming attribute, and whether it is multi valued.
             *    If it is, we swap the naming value with whatever is in position 0.
             */

            if (numberValues > 1)
            {
                boolean namingMagic = false;  // whether we're playing silly buggers with multi-valued naming values

                String namingValue = null;

                /*
                 *    See if we've got a naming value to worry about.
                 */

                String[] namingTypes = entry.getRDN().getAttIDs();
                if (namingTypes != null)
                {
                    for (int i = 0; i < namingTypes.length; i++)
                        if (namingTypes[i].equalsIgnoreCase(name))
                        {
                            namingMagic = true;
                            namingValue = entry.getRDN().getRawVals()[i];
                        }
                }

                /*
                 *    If we do, swap 0 for the naming value, and visa versa.
                 */

                if (namingMagic)
                {
                    if (position == 0)
                    {
                        return checkLength(namingValue);
                    }
                    else if (attValue.equals(namingValue))
                    {
                        return checkLength((String) a.get(0));
                    }
                }

            }

            if (attValue instanceof String)
            {
                return checkLength((String) attValue);
            }
            else
            {
                if (attValue instanceof byte[])
                {
                    currentBinaryAttributes.add(name);

                    if (a.getID().equalsIgnoreCase("userpassword"))
                    {
                        return getPasswordText(attValue);
                    }
                    else
                        return CBBase64.binaryToString((byte[]) attValue);
                }
            }
            return "Unable to get Att Value for " + name + "!";

        }
        catch (NamingException e)
        {
            if (showHTMLErrors)
                log.warning("Form Value Error getting value for " + name + " value :\n " + e);            // not a terminal error.
            return "";
        }
    }

    String getPasswordText(Object val)
    {
        if (val == null) return "";

        if (!(val instanceof byte[]))
        {
            //??
            if (val instanceof String)
                return (String)val;
            else // WTF??
                return "error parsing pwd";

        }

        byte[] data = (byte[]) val;

        try
        {

            if (data == null || data.length == 0)
                return "";

            if (CBParse.isUTF8(data)) // if the password is already 'text' encoded (ASCII or UTF-8) return as is...
                return new String(data, "UTF-8");
        }
        catch (Exception e) {}

        return CBBase64.binaryToString((byte[]) data);

    }

    /**
     *    This takes a attribute type name and the complete attribute list, as
     *    well as an optional modifier, and returns the formatted html necessary
     *    to display the attribute in the html text.
     *
     *    @param attType the type of the attribute being formatted
     *    @param attributes a list of all available attributes
     *    @param modifier one of list|table|plain, or a null value
     *     (which is equivalent to list) that sets the html list display
     *     type
     *    @return formatted html text displaying the attribute
     */

    public String formattedAttribute(String attType, Attributes attributes, String modifier)
    {
        Attribute theAttribute = attributes.get(attType);
        //TE: Return null values as a blank string...was 'No Value Found' changed to satisfy bug 2712.
        if (theAttribute == null)
        {
            log.warning("can't find attribute: '" + attType + "'");
            return "";
        } // TEMP

        return formattedAttribute(theAttribute, modifier);
    }

    /**
     *    This takes a attribute type name and the complete attribute list, as
     *    well as an optional modifier, and returns the formatted html necessary
     *    to display the attribute in the html text.
     *
     *    @param theAttribute the attribute to format into an HTML fragment
     *    @param modifier one of list|table|options|plain, or a null value
     *     (which is equivalent to list) that sets the html list display
     *     type
     *    @return formatted html text displaying the attribute
     */

    public String formattedAttribute(Attribute theAttribute, String modifier)
    {
//TODO - Adjust for non-string Values...

        try
        {
            if (theAttribute == null) return NOVALUEFOUND;

            String syntaxOID = "";
            if (theAttribute instanceof DXAttribute)
                syntaxOID = ((DXAttribute) theAttribute).getSyntaxOID();

            // Return single values as raw strings...
            if (theAttribute.size() == 1)
            {
                Object o = theAttribute.get();
                if (o == null)
                    return NOVALUEFOUND;
                else if (o instanceof String)
                {
                    if ("1.3.6.1.4.1.1466.115.121.1.12".equals(syntaxOID))
                        return "<a href=\"dn:" + o.toString() + "\">" + o.toString() + "</a>";		//TE: dn hyperlink for text.html.
                    else if ("1.3.6.1.4.1.1466.115.121.1.26".equalsIgnoreCase(syntaxOID))
                        return "<a href=\"mailto:" + o.toString() + "\">" + o.toString() + "</a>";	//TE: email hyperlink for text.html
                    else if ((((String) o).toLowerCase()).startsWith("http://"))
                        return "<a href=\"" + o + "\">" + o + "</a>";								//TE: URL hyperlink
                    else
                        return CBParse.toHTML(syntaxParse(o.toString(), syntaxOID));
                }
//                else if (!"1.3.6.1.4.1.1466.115.121.1.4".equals(syntaxOID))		//TE: why did I put this in?
//				{
//                    return CBIntText.get("(Binary Value)");
//				}
            }
            NamingEnumeration values = theAttribute.getAll();
            if (values == null) return NOVALUEFOUND;

            // it's a list of values; use the list formatting method...

            return formattedListAttribute(values, modifier, syntaxOID, theAttribute.getID());
        }
        catch (NamingException e)
        {
            return "<i>" + CBIntText.get("Error: exception reading value") + "</i>";
        }
    }

    public String syntaxParse(String s, String syntaxOID)
    {
        if (!("".equals(syntaxOID)))
        {
            if ("1.3.6.1.4.1.1466.115.121.1.41".equals(syntaxOID)) 			// 'Postal Address'
                s = s.replace('$', '\n');
        }
        return s;
    }


/*
   public String formattedAttribute( Object[] attlist, String modifier )
    {
        // Return null values as 'No Value Found' string
        if ((attlist==null ) || (attlist.length == 0)) return "<i>No Value Found</i>";

        // Return single values as raw strings...
        if (attlist.length == 1) return CBUtility.toHTML(attlist[0].toString());

        // it's a list of values; use the list formatting method...
        return formattedListAttribute( attlist, modifier);
    }
*/


    /**
     * 	Takes a list of attribute values, and formats them as an
     * 	html list, possibly of a type specified by the modifier.
     * 	@param attlist a list of objects to display.
     *  	@param modifier one of list|table|select|plain, or a null value
     *  		(which is equivalent to list) that sets the html list display
     *     	type.
     *	@param syntaxOID eg 1.3.6.1.4.1.1466.155.121.1.41 for postalAddress.
     *	@param syntaxID eg postalAddress.
     *  	@return the html text necessary to display th elist of values.
     */

    public String formattedListAttribute(NamingEnumeration attlist, String modifier, String syntaxOID, String syntaxID)
    {
        String listStart = "";
        String listEnd = "";
        String itemStart = "";
        String itemEnd = "";

        if ((modifier == null) || (modifier.equalsIgnoreCase("list")))
        {
            listStart = "<ul>\n";
            listEnd = "</ul>\n";
            itemStart = "<li>";
            itemEnd = "</li>";
        }
        else if (modifier.equalsIgnoreCase("table"))
        {
            listStart = "<table>\n";
            listEnd = "</table>\n";
            itemStart = "<tr><td valign=top>";
            itemEnd = "</td></tr>";
        }
        else if (modifier.equalsIgnoreCase("options"))
        {
            listStart = "";
            listEnd = "";
            itemStart = "<option";  // note missing closing brace; used in hack in formattedListWithModifiers to name options.  (If we ended up doing more of this, we should use a substitution mechanism instead; e.g. <option $1>)
            itemEnd = "</option>";
        }
        else if (modifier.equalsIgnoreCase("plain"))
        {
            itemEnd = "\n";
        }
        else
        {
        }

        return formattedListWithModifiers(attlist, listStart, listEnd, itemStart, itemEnd, syntaxOID, syntaxID);
    }


    /**
     * 	used by formattedListAttribute to format a list, using the
     * 	parameters as formatting text to produce html.
     * 	@param attlist a list of objects to display.
     *	@param listStart could be the unordered list or table tag.
     *	@param listEnd  could be the unordered list or table (end) tag.
     *	@param itemStart could be a list item or a row tag.
     *	@param itemEnd could be a list item tag or a row (end) tag
     *	@param syntaxOID eg 1.3.6.1.4.1.1466.155.121.1.41 for postalAddress.
     *	@param syntaxID eg postalAddress.
     *	@return the formatted list with the modifiers.
     */

    private String formattedListWithModifiers(NamingEnumeration attlist, String listStart, String listEnd, String itemStart, String itemEnd, String syntaxOID, String syntaxID)
    {
        if (attlist == null) return NOVALUEFOUND;

        if (syntaxOID != null)
        {
            if (syntaxOID.equalsIgnoreCase("1.3.6.1.4.1.1466.115.121.1.28"))    							//TE: special handling for jpegPhoto.
                return new String(getMediaTags(listStart, listEnd, itemStart, itemEnd, "jpegPhoto"));    	//TE: returns the all jpegPhoto tags (all included in the same string).
            else if (syntaxOID.equalsIgnoreCase("1.3.6.1.4.1.1466.115.121.1.4"))							//TE: special handling for audio.
                return new String(getMediaTags(listStart, listEnd, itemStart, itemEnd, "audio"));    		//TE: returns the all audio tags (all included in the same string).
        }

        if (syntaxID.equalsIgnoreCase("odDocumentDOC"))														//TE: special handling for odDocumentDOC.
            return new String(getMediaTags(listStart, listEnd, itemStart, itemEnd, "odDocumentDOC"));  	//TE: returns the all odDocumentDOC tags (all included in the same string).
        if (syntaxID.equalsIgnoreCase("odSpreadSheetXLS"))													//TE: special handling for odSpreadSheetXLS.
            return new String(getMediaTags(listStart, listEnd, itemStart, itemEnd, "odSpreadSheetXLS"));  	//TE: returns the all odSpreadSheetXLS tags (all included in the same string).
        if (syntaxID.equalsIgnoreCase("odMusicMID"))														//TE: special handling for odSpreadSheetXLS.
            return new String(getMediaTags(listStart, listEnd, itemStart, itemEnd, "odMusicMID"));  		//TE: returns the all odSpreadSheetXLS tags (all included in the same string).
        if (syntaxID.equalsIgnoreCase("odMovieAVI"))														//TE: special handling for odSpreadSheetXLS.
            return new String(getMediaTags(listStart, listEnd, itemStart, itemEnd, "odMovieAVI"));  		//TE: returns the all odSpreadSheetXLS tags (all included in the same string).
        if (syntaxID.equalsIgnoreCase("odSoundWAV"))														//TE: special handling for odSpreadSheetXLS.
            return new String(getMediaTags(listStart, listEnd, itemStart, itemEnd, "odSoundWAV"));  		//TE: returns the all odSpreadSheetXLS tags (all included in the same string).

        StringBuffer formattedList = new StringBuffer();
        while (attlist.hasMoreElements())
        {
            Object temp = attlist.nextElement();

            String value = "";
            if (temp != null)
                if (temp instanceof String)
                    value = temp.toString();
                else
                {
                    if (syntaxID.equals("userPassword"))
                        value = getPasswordText(temp);
                    else
                        value = "(Binary Value)";
                }
            /*
             *  Active Directory hack - sometimes AD doesn't publish schema, leading us to mis-identify
             *  large binary values as strings. It seems the second
             */
             if (value.length()>2 && (value.charAt(0) == 0 || value.charAt(1) == 0)) // real strings never have '0s' in them.
                  value = "(Binary Data in String)";

            if (syntaxOID != null && syntaxOID.equalsIgnoreCase("1.3.6.1.4.1.1466.115.121.1.12"))      		//TE: DN tag <a href="dn:...">dn...</a>.
                formattedList.append(itemStart + "<a href=\"dn:" + value + "\">" + value + "</a>" + itemEnd);
            else if (syntaxOID != null && syntaxOID.equalsIgnoreCase("1.3.6.1.4.1.1466.115.121.1.26"))
                formattedList.append(itemStart + "<a href=\"mailto:" + value + "\">" + value + "</a>" + itemEnd);		//TE: email tag <a href="mailto:whoever@whereever.com">whoever@whereever.com</a>.
            else if ((value.toLowerCase()).startsWith("http://"))
                formattedList.append(itemStart + "<a href=\"" + value + "\">" + value + "</a>" + itemEnd);				//TE: email tag <a href="url">url name</a>.
            else
            {
                // quick hack to allow properly named select 'options'
                String val = CBParse.toHTML(syntaxParse(value, syntaxOID));
                if (itemStart.equals("<option"))
                    formattedList.append(itemStart + " value=\""+ val + "\">" + val + itemEnd);
                else
                    formattedList.append(itemStart + val + itemEnd);
            }
        }
        return new String(listStart + formattedList.toString() + listEnd);
    }


    /**
     *    Returns a string representation of all HTML code need to display all of the jpegPhoto or audio
     *    attributes in the template.  Lists all the files in the temp directory.  Checks if any
     *    pertain to the entry that is to be displayed (checks that it starts with the dn of the entry
     *    and ends with '.jpg', or for audio checks that it doesn't end in ',jpg').  If so, appends
     *	 the html code to a string buffer which is returned as a string.
     *    @param listStart the start tag for the display type (bullet or table).
     *    @param listEnd the end tag for the display type.
     *    @param itemStart the start tag for the display inserts.
     *    @param itemEnd the end tag for the display inserts.
     *    @return the whole html code for displaying the jpeg photos within the display.
     */

    public String getMediaTags(String listStart, String listEnd, String itemStart, String itemEnd, String type)
    {
        StringBuffer htmlStringBuffer = new StringBuffer();

        File fileDir = type.equalsIgnoreCase("audio") ? CBCache.getAudioCacheDirectory() : CBCache.getCacheDirectory();    	//TE: get the temp directory.

        String[] allFiles = fileDir.list();    		//TE: get the files in the temp directory.
        String fileName;

        // TODO: do we need to check the types of things?

        for (String file:allFiles)
        {
            fileName = file.toString();
            if (file.startsWith(currentDN))
            {
                if (type.equalsIgnoreCase("audio") &&  !file.endsWith(JPEGEXTENSION))			//TE: if the files are temp audio files for the entry, create html for it...
                {   //TE: i.e <ul><li><a href="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.wav">audio</a></li></ul>
                    htmlStringBuffer.append(new String(listStart + itemStart + "<a href=\"" + CBCache.getAudioDirPath() + fileName + "\" + >audio</a>" + itemEnd + listEnd));
                }
                else if (type.equalsIgnoreCase("odMusicMID") && file.startsWith(currentDN) && file.endsWith(MIDEXTENSION))    //TE: if the files are temp odMusicMID files for the entry, create html for it...
                {   //TE: i.e <ul><li><a href="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.mid">audio</a></li></ul>
                    htmlStringBuffer.append(new String(listStart + itemStart + "<a href=\"" + CBCache.getDirPath() + fileName + "\" + >audio</a>" + itemEnd + listEnd));
                }
                else if (type.equalsIgnoreCase("odSoundWAV") && file.startsWith(currentDN) && file.endsWith(WAVEXTENSION))    //TE: if the files are temp odSoundWAV files for the entry, create html for it...
                {   //TE: i.e <ul><li><a href="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.wav">audio</a></li></ul>
                    htmlStringBuffer.append(new String(listStart + itemStart + "<a href=\"" + CBCache.getDirPath() + fileName + "\" + >audio</a>" + itemEnd + listEnd));
                }
                else if (type.equalsIgnoreCase("jpegPhoto") && file.startsWith(currentDN) && file.endsWith(JPEGEXTENSION))    //TE: if the files are temp jpeg files for the entry, create html for it...
                {   //TE: i.e <ul><li><img src="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.jpg" border="1"></li></ul>
                    htmlStringBuffer.append(new String(listStart + itemStart + "<img src=\"" + CBCache.getDirPath() + fileName + "\" " + "border=\"1\">" + itemEnd + listEnd));
                }
                else if (type.equalsIgnoreCase("odDocumentDOC") && file.startsWith(currentDN) && file.endsWith(DOCEXTENSION))    //TE: if the files are temp odDocumentDOC files for the entry, create html for it...
                {   //TE: i.e <ul><li><a href="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.doc">audio</a></li></ul>
                    htmlStringBuffer.append(new String(listStart + itemStart + "<a href=\"" + CBCache.getDirPath() + fileName + "\" + >document</a>" + itemEnd + listEnd));
                }
                else if (type.equalsIgnoreCase("odSpreadSheetXLS") && file.startsWith(currentDN) && file.endsWith(XLSEXTENSION))    //TE: if the files are temp odSpreadSheetXLS files for the entry, create html for it...
                {   //TE: i.e <ul><li><a href="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.doc">audio</a></li></ul>
                    htmlStringBuffer.append(new String(listStart + itemStart + "<a href=\"" + CBCache.getDirPath() + fileName + "\" + >spreadsheet</a>" + itemEnd + listEnd));
                }
                else if (type.equalsIgnoreCase("odMovieAVI") && file.startsWith(currentDN) && file.endsWith(AVIEXTENSION))    //TE: if the files are temp odMovieAVI files for the entry, create html for it...
                {   //TE: i.e <ul><li><a href="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.avi">movie</a></li></ul>
                    htmlStringBuffer.append(new String(listStart + itemStart + "<a href=\"" + CBCache.getDirPath() + fileName + "\" + >movie</a>" + itemEnd + listEnd));
                }
            }
        }
        return htmlStringBuffer.toString();
    }
            
/*            
            if (type.equalsIgnoreCase("audio") && allFiles[i].startsWith(currentDN) && !allFiles[i].endsWith(JPEGEXTENSION))			//TE: if the files are temp audio files for the entry, create html for it...
            {   //TE: i.e <ul><li><a href="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.wav">audio</a></li></ul>
                fileName = allFiles[i].toString();
                htmlStringBuffer.append(new String(listStart + itemStart + "<a href=" + "\".." + File.separator + "temp" + File.separator + "audio" + File.separator + fileName + "\" + >audio</a>" + itemEnd + listEnd));
            }
            else if (type.equalsIgnoreCase("jpegPhoto") && allFiles[i].startsWith(currentDN) && allFiles[i].endsWith(JPEGEXTENSION))    //TE: if the files are temp jpeg files for the entry, create html for it...
            {   //TE: i.e <ul><li><img src="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.jpg" border="1"></li></ul>
                fileName = allFiles[i].toString();
                htmlStringBuffer.append(new String(listStart + itemStart + "<img src=" + "\".." + File.separator + "temp" + File.separator + fileName + "\" " + "border=\"1\">" + itemEnd + listEnd));
            }
            else if (type.equalsIgnoreCase("odDocumentDOC") && allFiles[i].startsWith(currentDN) && allFiles[i].endsWith(DOCEXTENSION))    //TE: if the files are temp odDocumentDOC files for the entry, create html for it...
            {   //TE: i.e <ul><li><a href="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.doc">audio</a></li></ul>
                fileName = allFiles[i].toString();
                htmlStringBuffer.append(new String(listStart + itemStart + "<a href=" + "\".." + File.separator + "temp" + File.separator + fileName + "\" + >document</a>" + itemEnd + listEnd));
            }
            else if (type.equalsIgnoreCase("odSpreadSheetXLS") && allFiles[i].startsWith(currentDN) && allFiles[i].endsWith(XLSEXTENSION))    //TE: if the files are temp odSpreadSheetXLS files for the entry, create html for it...
            {   //TE: i.e <ul><li><a href="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.doc">audio</a></li></ul>
                fileName = allFiles[i].toString();
                htmlStringBuffer.append(new String(listStart + itemStart + "<a href=" + "\".." + File.separator + "temp" + File.separator + fileName + "\" + >spreadsheet</a>" + itemEnd + listEnd));
            }
            else if (type.equalsIgnoreCase("odMusicMID") && allFiles[i].startsWith(currentDN) && allFiles[i].endsWith(MIDEXTENSION))    //TE: if the files are temp odMusicMID files for the entry, create html for it...
            {   //TE: i.e <ul><li><a href="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.mid">audio</a></li></ul>
                fileName = allFiles[i].toString();
                htmlStringBuffer.append(new String(listStart + itemStart + "<a href=" + "\".." + File.separator + "temp" + File.separator + fileName + "\" + >audio</a>" + itemEnd + listEnd));
            }
            else if (type.equalsIgnoreCase("odSoundWAV") && allFiles[i].startsWith(currentDN) && allFiles[i].endsWith(WAVEXTENSION))    //TE: if the files are temp odSoundWAV files for the entry, create html for it...
            {   //TE: i.e <ul><li><a href="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.wav">audio</a></li></ul>
                fileName = allFiles[i].toString();
                htmlStringBuffer.append(new String(listStart + itemStart + "<a href=" + "\".." + File.separator + "temp" + File.separator + fileName + "\" + >audio</a>" + itemEnd + listEnd));
            }
            else if (type.equalsIgnoreCase("odMovieAVI") && allFiles[i].startsWith(currentDN) && allFiles[i].endsWith(AVIEXTENSION))    //TE: if the files are temp odMovieAVI files for the entry, create html for it...
            {   //TE: i.e <ul><li><a href="..\temp\cn=turtle food,ou=Administration,ou=Corporate,o=DemoCorp,c=AU1.avi">movie</a></li></ul>
                fileName = allFiles[i].toString();
                htmlStringBuffer.append(new String(listStart + itemStart + "<a href=" + "\".." + File.separator + "temp" + File.separator + fileName + "\" + >movie</a>" + itemEnd + listEnd));
            }
*/            



    /*
     *    returns all non-null attribute values, formatted in a sane manner according to
     *    the optional modifier paramater
     *    @param attributes a list of all attributes to display
     *    @param modifier one of list|table|plain, or a null value
     *     (which is equivalent to list) that sets the html list display
     *     type
     *    @return the html text necessary to display the list of values.
     */

    public String formattedAllAttributes(DXEntry attributes, String modifier)
    {

//TODO modify code so that the NamingExceptions that may be handled by the
// NamingEnumeration are sensibly handled and displayed (currently they are
// ignored).

        if (modifier == null) modifier = "list";  // list is default option.

        NamingEnumeration attributeList;

        attributeList = attributes.getAllNonNull();

        StringBuffer list = new StringBuffer("");

        if (modifier.equalsIgnoreCase("list")) list.append("<table>");

        try
        {
            while (attributeList.hasMore())
            {
                Attribute theAttribute = (Attribute) attributeList.next();

                String syntaxOID = (theAttribute instanceof DXAttribute) ? ((DXAttribute) theAttribute).getSyntaxOID() : "";

                if (modifier.equalsIgnoreCase("list"))			//TE: Simple.html.
                {
                    list.append("<tr>" + theAttribute.getID() + "</td><td valign=top>");
                    list.append(formattedListAttribute(theAttribute.getAll(), modifier, syntaxOID, theAttribute.getID()) + "</tr>\n");
//                    list.append("</td></tr><tr valign=top colspan=\"2\"><hr></tr>\n");
                }
                else if (modifier.equalsIgnoreCase("table"))	//TE: Table
                {
                    list.append(formattedListAttribute(theAttribute.getAll(), modifier, syntaxOID, theAttribute.getID()));
                }
                else											//TE: Text.html
                {
                    list.append(theAttribute.getID() + ":\n" + formattedAttribute(theAttribute, "plain") + "\n\n");
                }
            }

            if (modifier.equalsIgnoreCase("list")) list.append("</table>");
        }
        catch (NamingException e)
        {
            log.warning("naming exception in formattedAllAttributes() " + e);
        }

        return list.toString();
    }


    /**
     *    Convenience wrapper for openPage, that prefixes parameter URL
     *    with the local URL directory path.
     *
     *    @param localURL the URL to convert to a fully defined path
     */
/*
    public boolean openLocalURL(String localURL)
    {
        if (localURL.startsWith("http:")) // don't mess with existing defined URLs
            return openPage(localURL);
        else
            return openPage(JXplorer.fileURLPrefix + myProperties.getProperty("dir.local") + localURL);
    }
*/
    /**
     *    Convenience wrapper for openPage, that prefixes parameter URL
     *    with the document URL directory path.
     *    D:\abcdefghijklmnopqrstuvwxyz ` ; ' ^ $ # @ % & ( )\a test with lots of spaces\dist\htmldocs
     *    D:\abcdefghijklmnopqrstuvwxyz ` ; ' ^ $ # @ % & ( )\a test with lots of spaces\dist\htmldocs\start.html
     *    @param docURL the URL to convert to a fully defined path
     */

    public boolean openDocumentURL(String docURL)
    {
        if (docURL.startsWith("http:"))    // don't mess with urls that are already properly defined
            return openPage(docURL);
        else
        {
            File document = new File(Theme.getInstance().getDirHtmlDocs() + docURL);
            try
            {
                return openPage(document.toURL()); // TODO: refactor to .toURI().toURL() ?
            }
            catch (MalformedURLException e)
            {
                log.warning("Bad URL '" + document.toString() + "'\n" + e);
                return false;
            }
        }
            //return openPage(JXplorer.fileURLPrefix + myProperties.getProperty("dir.htmldocs") + docURL);
    }


    /**
     *	Tricky method that launches the users default mail client or Browser.  Does this
     *	by creating a bat file (which is deleted on JX exit) called 'temp.bat'.
     *	It then writes something like 'start mailto:whoever@whereever.com' or a url to this file
     *	It then uses the runtime class to run this bat file which should launch the default
     *	mail client (on windows only).
     * 	@param desc email address for example 'mailto:whoever@wherever.com'.
     */

    public void launchClient(String desc)
    {
        try
        {
            File file = new File("temp.bat"); 			//TE: make a bat file in the working directory.
            file.deleteOnExit();    					//TE: deletes the temporary file when JX is shut down.

            FileWriter output = new FileWriter(file);
            output.write("start " + desc);     			//TE: write 'start mailto:whoever@whereever.com" to the bat file.
            output.close();

            Runtime r = Runtime.getRuntime();
            r.exec("temp.bat");							//TE: run the bat file.
        }
        catch (Exception e)
        {
            CBUtility.error("Error launching default mail client " + e);
        }
    }


    /**
     *    identical in intent to openPage(URL) (see).  This
     *    version takes a string form of the URL instead.
     *
     *    @param urlString the string form of the url to be opened.
     *    @return whether the new page loading was successfull.
     */
    public boolean openPage(String urlString)
    {
        if (urlString == null) return false;

        if (urlString.toLowerCase().startsWith("dn:"))  // 'hyperlinked' dn: tags jump browser to appropriate entry when pressed.
        {
            String dn = urlString.substring(3);
            if (dn.startsWith("/")) dn = dn.substring(1);
            if (dn.startsWith("/")) dn = dn.substring(1);
            DN linkDN = new DN(dn);
            if (linkDN.size() > 0)
            {
                currentDataSource.getEntry(linkDN);
            }

            return true;
        }
        else
        {

            try
            {
                URL url = new URL(urlString);
                return openPage(url);
            }
            catch (MalformedURLException e)
            {
                log.warning("Bad URL '" + urlString + "'\n" + e);
                return false;
            }
        }
    }

    /**
     *    this opens a page for viewing.  It handles all errors internally.  If
     *    it is unable to open a page, it will attempt to restore the current
     *    page.
     *
     *
     *    @param url the location of the page (help file, template etc.) to be opened
     *    @return whether the new page loading was successfull.
     */
    public boolean openPage(URL url)
    {
        if (url == null) return false;
        URL original = editor.getPage();


        try
        {
            setEditor(url);

            return true;
        }
        catch (IOException e)    // catch normal 'can't find this' exceptions
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, CBIntText.get("Can't follow link") + " " + url.toString(),
                    CBIntText.get("Invalid URL"), JOptionPane.ERROR_MESSAGE);
            log.warning("unable to follow url " + url.toString() + "\n" + e);
            try
            {
                if (original != null)
                    editor.setPage(original);  // XXX this reuses the same editor, and hence might be error-prone.
                else
                    setToDefault();
            }            // Return to Original
            catch (Exception e3)
            {
                log.warning("fnord:  " + e3);
                setToDefault();
            }
            return false;
        }
        catch (Exception e2)    // some screwy stuff in swing html handling... try to catch that too.
        {
            log.warning("Wierd Java exception thrown on setPage in AttributeDisplay\n" + e2);
            try
            {
                if (original != null)
                    editor.setPage(original);	// XXX this reuses the same editor, and hence might be error-prone.
                else
                    setToDefault();
            }            // Return to Original
            catch (Exception e3)
            {
                setToDefault();
            }
            return false;
        }
    }

    /**
     *    Sets the editor to display the particular (html only, no DNs...) url.
     *    @param url the url of the local file/web page to display
     */

    protected void setEditor(URL url) throws IOException
    {
        if (editor == null) editor = getNewEditor();
        editor.setPage(url);  // XXX this reuses the same editor, and hence might be error-prone.
        validate();
    }


    public JComponent getDisplayComponent()
    {
        return this;
    }

    public String toString()
    {
        return (htmlText == null)?"":htmlText.toString();
    }

    public String getName()
    {
        return CBIntText.get("HTML View");
    }

    public ImageIcon getIcon()
    {
        return new ImageIcon(Theme.getInstance().getDirImages() + "html.gif");
    }    //TE: returns an icon.

    public String getToolTip()
    {
        return CBIntText.get("The HTML View is used to view and edit the data in purpose constructed HTML templates.");
    }    //TE: returns a tool tip.


    /**
     *    Return the thingumy that should be printed.
     */
    public Component getPrintComponent()
    {
        return editor;
    }


    public boolean isUnique()
    {
        return false;
    }


    public DataSink getDataSink()
    {
        return this;
    }

    public boolean canCreateEntry()
    {
        return false;
    }

    public void registerComponents(JMenuBar menu, JToolBar buttons, JTree tree, JPopupMenu treeMenu, JFrame jx)
    {
        smartTree = (SmartTree) tree;
    }

    public void unload()
    {
    }


    /**
     *    Use the default tree icon system based on naming value
     *    or object class.
     */

    public ImageIcon getTreeIcon(String rdn)
    {
        return null;
    }

    /**
     *    Use the default popupmenu.
     */

    public JPopupMenu getPopupMenu(String rdn)
    {
        return null;
    }

    /**
     *    Don't hide sub entries.
     */

    public boolean hideSubEntries(String rdn)
    {
        return false;
    }


    /**
     * Whether the editor has unsaved data changes.  This may be used by the GUI to prompt the user when
     * an editor pane is being navigated away from.
     * @return
     */

    public void checkForUnsavedChanges()
    {
    }

}