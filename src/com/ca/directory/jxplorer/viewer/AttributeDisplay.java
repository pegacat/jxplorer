package com.ca.directory.jxplorer.viewer;

import javax.swing.*;
import java.lang.reflect.*;

import java.awt.*;
import java.awt.print.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.ca.directory.jxplorer.*;
import com.ca.commons.cbutil.*;
import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.broker.DataBrokerQueryInterface;

import javax.naming.directory.*;
import javax.naming.NamingException;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

/**
 *    <p>AttributeDisplay holds a variety of display and editing
 *    classes, as well as a simple toolbar to allow switching
 *    between these.  The main one is an HTML template displaying
 *    class, but there are (will be) also attribute editing and
 *    template editing classes...</p>
 *
 *    <p>Note that this class is *not* thread safe.</p>
 */

public class AttributeDisplay extends JTabbedPane
    implements DataSink, Printable
{
    /**
     *    The HTML editor is always in position 1 (if present)
     */
     
    public static final int HTMLRENDERER = 1;
    
    /**
     *    The Table editor is always in position 2 (if present)
     */
    
    public static final int EDITOR = 2;

    /**
     *   The scroll pane that holds the main view object
     */
      
    protected JScrollPane  viewHolder;
    
    /**
     *  The main view panel.  This is what holds the various editors.
     */
     
    protected JPanel       view;
    
    
    //protected JToolBar     toolBar;

    /**
     *    the source of directory data for all editors
     */
     
    protected DataBrokerQueryInterface dataSource;
    
    /**
     *    the currently viewed entry (may be null)
     */
     
    protected DXEntry              entry = null;                   

    /**
     *    The editor currently being displayed to the user.
     *    This is updated every time the user tabs to a new
     *    editor, or a different editor is loaded.
     */
     
    protected PluggableEditor      currentEditor;

    /**
     *    The default html editor, that is usually available, and which
     *    displays html template files
     */
     
    protected HTMLTemplateDisplay  templateDisplay;                
    
    /**
     *    The default table editor, that is usually available, and displays
     *    entry data as a grid of strings.  Binary data may be edited using
     *    special 'attribute editors'.
     */
     
    protected TableAttributeEditor tableDisplay;                   

    /**
     *    The printing preferences used to print the currently visible entry editor.
     */

    private Properties printprefs = new Properties();    

    /**
     *    A hashtable of all the editors that have ever been used to display an
     *    entry in the current JX session.  When an entry is loaded, these are 
     *    checked first to determine whether an editor already exists, and if it
     *    does, it is re-used.
     */
     
    protected Hashtable editors = new Hashtable();
    
    /**
     *    a list containing the currently displayed editors
     */

    protected Vector activeEditors = new Vector(8);  

    /**
     *    the parent frame displaying this object
     */
     
    protected JFrame owner;                          


    protected String oldOCSig = null;                // a concatenation of the last used object classes.

    /** A local copy of the JX component, to pass on to pluggable editors */

    private JMenuBar registerMenu = null;

    /** A local copy of the JX component, to pass on to pluggable editors */

    private JToolBar registerButtons = null;

    /** A local copy of the JX component, to pass on to pluggable editors */

    private JTree registerTree = null;

    /** A local copy of the JX component, to pass on to pluggable editors */

    private JPopupMenu registerTreeMenu = null;

    /** A local copy of the JX component, to pass on to pluggable editors */

    private JFrame registerJX = null;

    /**
     *  A local copy of the special class loader used to load the plugable
     *  editors.
     */
     
    protected ClassLoader myLoader = null;            // the class loader used for reading pluggable editors from zip/jar files.

    /**
     *  A local copy of the special resource loader used to load the plugable
     *  editors and their associated resource files.
     */
     
    protected CBResourceLoader resourceLoader = null; // the resource loader used for reading HTML templates from zip/jar files.

    /**
     *    All Plugins must have this package prefix.  Usually it is com.ca.directory.jxplorer.viewer,
     *    but it <i>can</i> be reset in the configuration.
     */
     
    public static String PACKAGEPREFIX = "com.ca.directory.jxplorer.viewer.";

    public static void setPackagePrefix(String prefix) { PACKAGEPREFIX = prefix; }

    public static String getPackagePrefix() { return PACKAGEPREFIX; }

    // Do we need an alternative 'BASESPREFIX' variable for when PACKAGEPREFIX is changed?
    // public static String BASEPREFIX = "com.ca.directory.jxplorer.viewer.";

    /**
     *     A utility constant - this is used in the editor hashtable to indicate
     *     that no pluggable editor has been found, and that there is no need
     *     to use the resource loader to look again.
     */
     
    protected static final String NONE = "none";

    /**
     *    A temporary copy of a component that is to be printed, used by the
     *    print thread mechanism to pass an editor image around.
     */
     
    private Component printComponent = null;

    /**
     *    Every time a tab is added or deleted, or a new tab is selected, a
     *    change event is triggered.  Sometimes we'd like to ignore these
     *    while we're getting our tabs sorted out (i.e. when we're adding
     *    and deleting large numbers of tabs) and this flags that such
     *    changes should be ignored.
     *
     */
     
    protected boolean ignoreChangeEvents = false;

    private static Logger log = Logger.getLogger(AttributeDisplay.class.getName());

    /**
     *    The constructor for AttributeDisplay requires information
     *    about default file directories and urls.  These are passed
     *    in via a Properties object, that should contain values for
     *    'dir.templates', 'dir.htmldocs', and 'dir.local'.
     *    @param owner the parent frame, used for gui sanity and L&F propogation
     *    @param resourceLoader the resource loader used to load HTML templates from zip/jar files
     */

    public AttributeDisplay(JFrame owner, CBResourceLoader resourceLoader) 
    {
        super();


        //myProperties = props;

        if (JXConfig.getProperty("plugins.package") != null)
        {
            setPackagePrefix(JXConfig.getProperty("plugins.package"));
            log.fine("SETTING PLUGIN PREFIX TO: " + PACKAGEPREFIX);
        }
        else
            log.fine("PLUGIN PREFIX UNCHANGED: " + PACKAGEPREFIX);

        this.resourceLoader = resourceLoader;

        this.owner = owner;

            initHTMLEditor();
            initTableEditor();
            addEditor(templateDisplay);

            /**
             *  This change listener is *intended* to listen for user initiated tab
             *  changes, rather than programmatic changes to the editor tabs (which
             *  are expected to look after themselves)
             */

            addChangeListener( new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    if (ignoreChangeEvents)   // sometimes when we're messing around we can fire heaps of
                        return;          // change events - and there's no point paying attention to 'em.

                    int index = getSelectedIndex();

                    if (index >= 0 && activeEditors.size() > index && activeEditors.get(index) != null)
                    {
                        setCurrentEditor((PluggableEditor)activeEditors.get(index));
                    }
                    else    // should never happen (ROTFL)
                    {
                        log.warning("internal error - unable to find editor # " + index + " in Attribute Display");
                    }
                }
            });
    }

    /**
     *    This specifies the class loader used to load plugin editors.
     *    (This may not be known when an AttributeDisplay object is first 
     *    created, at which time only the default system class loader is used).
     */
     
    public void registerClassLoader(ClassLoader loader)
    {
        myLoader = loader;
        if (tableDisplay == null)
        	initTableEditor();
        	
        tableDisplay.registerClassLoader(loader);
    }

    /**
     * delay initialising the html editor until we need it, or it is loaded by
     * a background thread.
     */

    protected synchronized void initHTMLEditor()
    {
        if (templateDisplay != null) return;

        templateDisplay = new HTMLTemplateDisplay(this, resourceLoader);
        
        /*
         *    HTML editor is placed in the hashtable associated with the object
         *    class "top".  Since *all* directory entries must have object class
         *    "top", this is the equivalent of saying the editor may be used for
         *    all object classes.
         */
         
        //editors.put("top", templateDisplay);        
        
        // Set the html editor to be the current default editor.
        
        currentEditor = templateDisplay;
    }

    /**
     * delay initialising the table editor until we need it, or it is loaded by
     * a background thread.
     */

    protected synchronized void initTableEditor()
    {
        if (tableDisplay != null) return;

        tableDisplay = new TableAttributeEditor(owner);
        
        /*
         *    HTML editor is placed in the hashtable associated with the object
         *    class "top".  Since *all* directory entries must have object class
         *    "top", this is the equivalent of saying the editor may be used for
         *    all object classes.
         */
         
        //editors.put("top", tableDisplay);           // can be used for all entries
    }

   /**
    * The method @print@ must be implemented for @Printable@ interface.
    * Parameters are supplied by system.
    *
    */
    //(Magic Irina Spell...!)
    
    public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException
    {
        //Component printMe = getPrintComponent();

        Graphics2D g2 = (Graphics2D)g;
        g2.setColor(Color.black);    //set default foreground color to black

        //for faster printing, turn off double buffering
        //RepaintManager.currentManager(this).setDoubleBufferingEnabled(false);

        Dimension d = printComponent.getSize();    //get size of document
        double panelWidth  = d.width;    //width in pixels
        double panelHeight = d.height;   //height in pixels
        double pageHeight = pf.getImageableHeight();   //height of printer page
        double pageWidth  = pf.getImageableWidth();    //width of printer page
        double scale = pageWidth/panelWidth;
        int totalNumPages = (int)Math.ceil(scale * panelHeight / pageHeight);

        //make sure we don't print empty pages
        if(pageIndex >= totalNumPages)
        {
            return Printable.NO_SUCH_PAGE;
        }

        //shift Graphic to line up with beginning of print-imageable region
        g2.translate(pf.getImageableX(), pf.getImageableY());
        
        //shift Graphic to line up with beginning of next page to print
        g2.translate(0f, -pageIndex*pageHeight);
        
        //scale the page so the width fits...
        g2.scale(scale, scale);
        
        // PRINT IT!
        printComponent.paint(g2);
        
        return Printable.PAGE_EXISTS;
    }

    /**
     *  Starts the print operation.  This is quite expensive, and is kicked off
     *  in a separate thread.
     */
     
    public void print()
    {
        final PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        
        // make a local copy of the object to be printed, for use by the print thread
        // below...
        
        final Component printMe = getPrintComponent();

        Thread worker = new Thread()
        {
            public void run()
            {
                if (job.printDialog())
                {
                    try 
                    { 
                        // need to make sure that no other thread tries to 
                        // run a print job and set printCompnent at the same
                        // time...
                        synchronized(this)
                        { 
                            printComponent = printMe;
                            job.print(); 
                            printComponent = null;
                        }    
                    }
                    catch (Exception ex) 
                        { log.warning("error printing: " + ex);  }
                }
            }
        };
        worker.start();

    }


    /**
     *    Used to force Attribute Display to show a particular web page,
     *    rather than an attribute display.
     *    This forces it to change view to an HTMLTemplateDisplay
     *    editor, and then show the desired URL thrugh this display.
     *
     *    @param docURL the web page url to be displayed
     */
     
    public void openDocumentURL(String docURL)
    {
        if (templateDisplay == null)
            initHTMLEditor();

        templateDisplay.openDocumentURL(docURL);
        setCurrentEditor(templateDisplay);
    }


    /**
     *   This is a 'get out of jail free' method that allows an external class
     *   to force the display of a particular editor.  (e.g. for a pluggable
     *   editor system that wants to be started on an empty directory).
     *   @param entry the entry to display (may be null)
     *   @param ds the data source to use for directory info.
     *   @param editorName the name to display in the editor tab.
     */
     
	public void displaySpecialEntry(DXEntry entry, DataBrokerQueryInterface ds, String editorName)
	{

        PluggableEditor ed = getEditor(editorName);
        
        
        if (ed != null)
        {
            if (ed.isUnique() == false)
            {
                setCurrentEditor(ed);
            }        
            else
            {
                if (currentEditor != ed)
                    addUniqueEditor(ed);
    
                refreshEditors(entry, ds);
                oldOCSig = null;
            }
        }

	}

    /**
     *    <p>Displays data that can be modified by the user.</p>
     *
     *    <p>This method also organises which editors are used to
     *    display the current entry, searching for pluggable editors
     *    and so on.</p>
     *
     *    @param dxentry the entry to be displayed by all the editors
     *    @param ds the datasource the editors may use for more info
     */

// XXX hack alert - entry == null && ds != null is a special flag to load the
// XXX 'null entry editor' (for pki group).  This is highly volitile while
// XXX requirements are worked out; if they stabilise we'll want to revisit this
// XXX code and neaten it up (and the calling code from smart tree).

    public void displayEntry(DXEntry dxentry, DataBrokerQueryInterface ds)
    {
        // Set the local data variables.
        
        dataSource = ds;
        entry = dxentry;
    
        // check that the default editors have been initialised.
    
        if (tableDisplay == null)   
            initTableEditor();
        if (templateDisplay == null )
            initHTMLEditor();

        // check for a 'no data' display - if there is no data, revert
        // to the default html 'no data' template.

        if (entry == null) // || entry.size() == 0) //TE: This is commented out to allow virtual nodes to be edited.
        {
        	if (oldOCSig != null)
        	{
	            clearPluggableEditors();

	            if (activeEditors.size() == 0)
	            {
	                addEditor(templateDisplay);
	            }
	            oldOCSig = null;
	        }
	        refreshEditors(null, ds);
        }
        else
        {
            dataSource = ds;                             // may be null, or read only...
            ArrayList<String> ocs = entry.getOrderedOCs();
    
            // try to create a unique 'signature' for a group of object classes
            // This relies on them being delivered in the same order though.  (Less 
            // efficient if they aren't, but otherwise shouldn't be a big problem).
            String newOCSig = new String();            
            
            if (ocs != null)
                for (int i=0; i<ocs.size(); i++)           
                {
                    Object ocSig = ocs.get(i);
                    if (ocSig != null)
                        newOCSig += ocSig.toString();
                }

            // Check if signiture hasn't changed.  If it *has* changed, 
            // reset the editors using 'setEditors', and update
            // the 'old object class signiture' variable.
            if (newOCSig.equals(oldOCSig) == false) 
            {
                setEditors(entry, ds, ocs);
                oldOCSig = newOCSig;
            }

            // Some quick sanity checks...
            if (entry.getStatus() == DXEntry.NEW)  // check for new entries (but *not* NEW_WRITTEN)...
            {
                // don't allow editors to appear that can't edit a new entry
                trimNonNewEntryEditors();
                suggestPluggableEditor();
            }    
            else
            {
                // make sure that the html template display is around...
                // XXX (a bit of a hack - this should really check that *all*
                // XXX editor that can't handle new entries have been added back...
                // XXX (maybe set flag?)
				//TE: added '&& !currentEditor.isUnique()' b/c this code always made sure the HTML
				//TE: editor is visible, whereas with a unique plugin we only want that one visible...
				//TE: unless of course I am totally confused!  See bug 674.
                if (activeEditors.contains(templateDisplay) == false && !currentEditor.isUnique())
                {
                    add((PluggableEditor)templateDisplay, 0);
                    if (currentEditor != null)                //XXX hack hack. 
                        setCurrentEditor(currentEditor);
                }    
            }

            if (activeEditors.contains(currentEditor) == false)  
            {
                suggestPluggableEditor();
            }   
            
             

            // now that the editor set we're using has been sorted out,
            // actually go and update the editors!  (Nb. this triggers an usaved changes check)
            refreshEditors(entry, ds);
        }
    }

    /**
     *    Not all editors are capable of displaying new entries.  This whips through
     *    and removes all editors that can't.
     */
     
    private void trimNonNewEntryEditors()
    {
        int size = activeEditors.size();
        for (int i=size-1; i>=0; i--)
        {
            if (((DataSink)activeEditors.get(i)).canCreateEntry() == false)
            {
                remove(i);
            }   
        } 
        suggestTableEditor();  // use table editor as default for new entries...
    }


    /**
     *    If a purpose written pluggable editor is available, switch to that,
     */

    public boolean suggestPluggableEditor()
    {
        for (int i=activeEditors.size()-1; i>=0; i--)
        {
            // try to set to the first 'user written' pluggable editor
            // that can be found...
            PluggableEditor ed = (PluggableEditor)activeEditors.get(i);
            if ( (ed != templateDisplay) && (ed != tableDisplay) )
            {
                setCurrentEditor(ed);
                return true;
            }    
        }
        return false;
    }
    
    /*
     *    If the table editor is available (i.e. has not been replaced
     *    by a pluggable editor) this switches the display to the table editor.
     */
     
    public boolean suggestTableEditor()
    {
        // if we can't find one of those, relapse to the table editor.
    
        if (activeEditors.contains(tableDisplay) == true)
        {
            setCurrentEditor(tableDisplay);
            return true;
        }    
        return false;
    }

    /*
     *    If the html editor is available (i.e. has not been replaced
     *    by a pluggable editor) this switches the display to the table editor.
     */
     
    public boolean suggestHTMLEditor()
    {
        // if we can't find one of those, relapse to the table editor.
    
        if (activeEditors.contains(templateDisplay) == true)
        {
            setCurrentEditor(templateDisplay);
            return true;
        }    
        return false;
    }


    /**
     *    Sets the current editor to the specified editor, 
     *    loads the current entry,
     *    and makes sure that it is visible
     *    to the user.
     *    @param makeCurrent the editor to select.
     */

    protected void setCurrentEditor(PluggableEditor makeCurrent)
    {
        currentEditor = makeCurrent;
        if (currentEditor != null && currentEditor.getDataSink()!= null)
            currentEditor.getDataSink().displayEntry(entry, dataSource);
            
        int index = activeEditors.indexOf(makeCurrent);
        
        if (index == -1)
        {
            clearPluggableEditors();
            addEditor(makeCurrent);
            
            setSelectedIndex(activeEditors.indexOf(makeCurrent));
            
        }
        else if (index != getSelectedIndex())
        {
            setSelectedIndex(index);
        }
    }

    /**
     *    Returns the current editor.
     */
     
    protected PluggableEditor getCurrentEditor()
    {
        return currentEditor;
    }

    /**
     *    Clear out all the old editors, and get new editors corresponding
     *    to the new object classes.
     *    @param entry the entry to be displayed by all the editors
     *    @param ds the datasource the editors may use for more info
     *    @param ocs the object classes (in order) to find editors for.
     */

    protected void setEditors(DXEntry entry, DataBrokerQueryInterface ds, ArrayList<String> ocs)
    {
    
        try
        {
            clearPluggableEditors();              // clear all extra editors
    
            // search for unique structural editors...
    
            if ("false".equalsIgnoreCase(JXConfig.getProperty("plugins.ignoreUniqueness")))
            {
                if(ocs==null)		//TE: may happen if virtual entry.
					return;
					
				int size = ocs.size();

                for (int i=0; i<size; i++)
                {
                    Object objectClass = ocs.get(i);
                    if (objectClass != null)
                    {
                        PluggableEditor ed = getEditor(objectClass.toString());
                        if (ed != null && ed.isUnique() == true)    // found a unique editor
                        {                                           // so clear old ones,
                            addUniqueEditor(ed);                    // and use the unique one
                            refreshEditors(entry, ds);              // to display the data
                            setCurrentEditor(ed);
                            return;                                 // ... and exit.
                        }
                    }
                }
            }
            else
                log.warning("skipping uniqueness test for pluggable editors");
    
            boolean newEdSet = false;
    
            // search for non-unique structural editors
            for (int i=0; i<ocs.size(); i++)
            {
                Object objectClass = ocs.get(i);
                if (objectClass != null)
                {
                    PluggableEditor ed = getEditor(objectClass.toString());  
                    if (ed != null)
                    {
                        addEditor(ed);
                        
                        // Force the displayed editor to be the first pluggable one...
                        if (newEdSet == false)
                        {
                            setCurrentEditor(ed);
                            newEdSet = true;
                        }
                    }    
                }
            }
    
            // search for non-structural editors
            try
            {
                Attribute allOCs = entry.getAllObjectClasses();
                if (allOCs != null)
                {
                    Enumeration vals = allOCs.getAll();
                    while (vals.hasMoreElements())
                    {
                        Object oc = vals.nextElement();
                        if (oc != null)
                        {
                            String ocName = oc.toString();
                            if (ocs.contains(ocName) == false) // don't bother with struct objectclasses dealt with above
                            {
                                PluggableEditor ed = getEditor(ocName);
    
                                if (ed != null)
                                {
                                    addEditor(ed);
    
                                    if (ed.isUnique())           // a special service to users...
                                        log.warning("WARNING: Illegal unique editor defined for oc: " + ocName + " not allowed - (oc not in primary structural inheritance chain)");
                                }
                            }
                        }
                    }
                }
            }
            catch (NamingException e) 
            {
                log.log(Level.WARNING, "WARNING: non-fatal exception getting object classes for plugin editors. ", e);
            }       
    
            addEditor(templateDisplay);        // and always add old faithfulls...
//XXX            
            if (entry.getStatus() != DXEntry.NEW)   // well, almost always...
                addEditor(tableDisplay);
        }            
        catch (Exception e)
        {
            log.warning("Unexpected Exception in AttributeDisplay\n" + e);
            e.printStackTrace();
        }
    }

    /**
     *    Removes all extra editors, leaves only the default html editor...
     */

//TODO: figure this out...
    public void clearEditors()
    {
        removeAll();
        activeEditors.removeAllElements();
        templateDisplay.setToDefault();
        addEditor(templateDisplay);
        tableDisplay.displayEntry(null, null);
        addEditor(tableDisplay);
    }


    /**
     *    Removes all transient editors, and ensures that the table editor
     *    and the html editor are available.
     */

    void clearPluggableEditors()
    {
        ignoreChangeEvents = true;
    
        for (int i=activeEditors.size()-1; i>=0; i--)
        {
            PluggableEditor ed = (PluggableEditor)activeEditors.get(i);
            if ( (ed != templateDisplay) && (ed != tableDisplay) )
            {
                remove(i);
            }                
        }

        if (activeEditors.contains(templateDisplay)==false)
        {
            addEditor(templateDisplay);
        }    

        if (activeEditors.contains(tableDisplay)==false)
        {
            addEditor(tableDisplay);
        }
        
        if (currentEditor != tableDisplay && currentEditor != templateDisplay)
        {
            suggestHTMLEditor();
        }                    



        ignoreChangeEvents = false;

    }


    /**
     *    This looks through a list of object classes in an attribute
     *    until it finds a unique editor corresponding to a particular
     *    value.  If no editor is found 'null' is returned.  Note that
     *    If multiple unique editors exist, which one is returned is
     *    undefined.<p>
     *
     *    @param oc the objectClass attribute; a list of object classes
     *    @return the unique pluggable editor corresponding to one particular
     *             object class value, or null if none is found.
     *
     */

    public PluggableEditor getUniqueEditor(Attribute oc)
    {
        try
        {
            Enumeration values = oc.getAll();
            while (values.hasMoreElements())
            {
                String objectClassName = (String)values.nextElement();

                PluggableEditor editor = getEditor(objectClassName);
                if (editor != null)
                {
                    if (editor.isUnique())
                        return editor;
                }
            }
            return null;
        }
        catch (Exception e)
        {
            log.log(Level.FINE, "Unable to find unique pluggable editor: ", e);
            return null;
        }
    }

    /**
     *    Gets a pluggable editor for a particular object class name.
     *    @param ocName the object class name to look up
     *    @return a corresponding editor, or null if none exists.
     */

    PluggableEditor getEditor(String ocName)
    {
        ocName = ocName.toLowerCase();

        Object editorFromHash = editors.get(PACKAGEPREFIX + ocName);

        if (editorFromHash != null)
            return castToPluggableEditor(editorFromHash, ocName);  // get it from storage

        return loadEditorFromDisk(PACKAGEPREFIX + ocName ); // may be null
    }

    /**
     *   Try to cast the object to a PluggableEditor, or return null if it is a placeholder.
     *  @param rawEditor the editor to cast (or a 'NONE' object placeholder)
     *  @param ocName the name of the editor (for error print out)
     */

    private PluggableEditor castToPluggableEditor(Object rawEditor, String ocName)
    {
        if (rawEditor == NONE)       // we have no editor for this entry, and we've already looked.
            return null;

        if (rawEditor instanceof PluggableEditor)   // already have an editor for that oc
        {
            return (PluggableEditor)rawEditor;
        }
        else
        {
            log.warning("Unexpected Class Cast Error loading plugin editor '"+PACKAGEPREFIX + ocName + "' from hashtable");
            return null;
        }
    }


    /**
     *    Look on disk for an editor with the class name 'ocName'.
     */

    PluggableEditor loadEditorFromDisk(String ocName)
    {
        // if here, we need to look on disk for a pluggable editor class...
        log.finer("looking for ocName: " + ocName);
        try
        {
            Class c = myLoader.loadClass(ocName);
            Constructor constructor = c.getConstructor(new Class[0]);
            
            
            // XXX If the pluggable editor has an error in the constructor, under some 
            // XXX circumstances it can fail so badly that this call never returns, and
            // XXX the thread hangs!  It doesn't even get to the exception handler below...
            // XXX but sometimes if the constructor fails everything works as expected.  Wierd.
            PluggableEditor editor = (PluggableEditor) constructor.newInstance(new Object[0]);
            
            editors.put(ocName, editor);         // add the new editor to our list
            return editor;
        }
        catch (Exception e)            // expected condition - just means no pluggable editor available
        {
			if (e instanceof InvocationTargetException) // rare exception - an error was encountered in the plugin's constructor.
			{
                log.warning("unable to load special editor for: '" + ocName + "' " + e);
                if (JXConfig.debugLevel >= 1)
                {
                    log.warning("Error loading plugin class: ");
				    ((InvocationTargetException)e).getTargetException().printStackTrace();
                }   
			}	
            
            log.log(Level.FINEST, "'Expected' Error loading " + ocName, e);
            editors.put(ocName, NONE);  // add a blank place holder - we can't load
                                          // an editor for this, and there's no point looking again. (change if want dynamic loading, i.e. look *every* time)
        }
        return null;  // only here if an error has occured.
    }


    /**
     *    This can be used to register Swing components that may be
     *    used by sub editors to affect the outside environment.
     */

    public void registerComponents(JMenuBar menu, JToolBar buttons, JTree tree, JPopupMenu treeMenu, JFrame jxplorer)
    {
        registerMenu     = menu;
        registerButtons  = buttons;
        registerTree     = tree;
        registerTreeMenu = treeMenu;
        registerJX       = jxplorer;
        
        // reset the sub editors as well.
        
        for (int i=0; i<activeEditors.size(); i++)
        {
            ((PluggableEditor)activeEditors.get(i)).registerComponents(menu, buttons, tree, treeMenu, jxplorer);
        }
    }


    /**
     *    Adds an editor to the current collection of active editors,
     *    and makes it a tab pane.
     */
     
    void addEditor(PluggableEditor ed)
    {
        if (activeEditors.contains(ed) == false)  // don't add editors twice!
        {
            add(ed);
            ed.registerComponents(registerMenu, registerButtons, registerTree, registerTreeMenu, registerJX);
        }
    }

    /**
     *    Adds a particular editor to the tab pane and collection of
     *    active editors, while removing all others.
     */

    void addUniqueEditor(PluggableEditor ed)
    {
        ignoreChangeEvents = true;   // don't bother the change listener until we've settled everything

        removeAll();
        addEditor(ed);
        setCurrentEditor(ed);

        ignoreChangeEvents = false;  // start the change listener listening again.
    }


	/**
	*   Refreshes the currently visible editor with new info.
	*	.
	*/
	 
	public void refreshEditors()
	{
		//TE: This method is in response to bug 367 re the fields in the
		//	HTML templates disappearing when the look and feel changes.
		//	Forcing a refresh of the page seems to solve the problem.
		//	Currently is is only being used by AdvancedOptions (yea...a bit
		//	of a lame fix).	

		if(dataSource != null)
			refreshEditors(entry, dataSource);
	}

    /**
     *    Refreshes the currently visible editor with new info.
     */

    public void refreshEditors(DXEntry entry, DataBrokerQueryInterface ds)
    {
        if (currentEditor != null)
        {
        	this.entry = entry;	//TE: make sure everything is in sink.
			dataSource = ds;
			
            currentEditor.getDataSink().displayEntry(entry, ds);  // checks for unsaved changes...

            // check that the editor hasn't changed display component
            JComponent display = currentEditor.getDisplayComponent();


            // XXX hack alert - some editor writers change their display component
            // XXX mid-flight, and expect JX to magically notice, and change the
            // XXX the display.  This code attempts to do this.
            
            if (indexOfComponent(display) == -1) // we have a problem - the display component has changed
            {
                String title = currentEditor.getName();
                ImageIcon icon = currentEditor.getIcon();
                String toolTip = currentEditor.getToolTip();

                int index = getSelectedIndex();  // find the index of the editor (XXX - this relies on the activeEditor vector tracking the inherent tab pane order)
                
                super.remove(index);
                super.insertTab(title, icon, display, toolTip, index);
                super.setSelectedIndex(index);
            }
        }
        else
            log.warning("internal error - no editor available in AttributeDisplay");

    }

     /**
      *    Return the thingumy that should be printed.
      */
    public Component getPrintComponent()
    {
        return currentEditor.getPrintComponent();
    }

    public boolean canCreateEntry() { return true; }

    public void add(PluggableEditor ed)
    {
        add(ed, getTabCount());
    }
    
    public void add(PluggableEditor ed, int index)
    {
        //add(ed.getName(), ed.getDisplayComponent()); // wierd array bounds error thrown here?
        insertTab(ed.getName(), ed.getIcon(), ed.getDisplayComponent(), ed.getToolTip(), index);
        activeEditors.add(index, ed);
    }

    public void remove(int index)
    {
        if (activeEditors.size() == 0) return;  // it can get a bit excitable about removing element 0 ...

        PluggableEditor ed = (PluggableEditor) activeEditors.remove(index);
        ed.unload();
        super.remove(index);
    }

    public void removeAll()
    {
        int size = activeEditors.size();
        for (int i=size-1; i>=0; i--)
            remove(i);

        super.removeAll();  // XXX this really shouldn't be necessary.
    }

    /* not necessary?  cf refreshEditors() handling
    public void checkForUnsavedChanges()
    {
            if (currentEditor != null)
            {
                currentEditor.checkForUnsavedChanges();
            }
     }
     */
}