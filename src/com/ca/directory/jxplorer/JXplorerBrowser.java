package com.ca.directory.jxplorer;

import com.ca.commons.cbutil.*;
import com.ca.commons.naming.DN;
import com.ca.commons.naming.DXAttribute;
import com.ca.commons.naming.DXAttributes;
import com.ca.directory.jxplorer.broker.*;
import com.ca.directory.jxplorer.event.JXplorerEvent;
import com.ca.directory.jxplorer.event.JXplorerEventGenerator;
import com.ca.directory.jxplorer.event.JXplorerListener;
import com.ca.directory.jxplorer.search.SearchBar;
import com.ca.directory.jxplorer.tree.SmartModel;
import com.ca.directory.jxplorer.tree.SmartNode;
import com.ca.directory.jxplorer.tree.SmartTree;
import com.ca.directory.jxplorer.viewer.AttributeDisplay;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * (c) Chris Betts: Groupmind Project
 */
public class JXplorerBrowser extends JFrame                     // Applet
        implements JXplorerEventGenerator
{
    JXplorer parent;

    private JFrame rootFrame;                     // convenience variables to avoid
    Container mainPane;                                  // calling get methods all the time.

    transient JXplorerListener jxplorerListener;

    EventListenerList eventListeners = new EventListenerList();

    JSplitPane splitPane;                       // the main page, containing treeTabPane on the left and the results on the right

    JScrollPane explorePanel;                   // contains mr tree
    JScrollPane resultsPanel;                   // contains search tree
    JScrollPane schemaPanel;                    // contains schema tree

    JTabbedPane treeTabPane;

    JPanel userViewPanel;

    CBPanel topPanel;                            // the top panel, containing the tool bars.
    JToolBar searchBar;                          // the quick search toolbar.
    ButtonBar buttonBar;                         // the graphic button bar.

    protected Stack statusStack = new Stack();


    //public static JFrame jx;

    private JNDIDataBroker jndiBroker = null;      // the JNDIDataBroker intermediary class through which requests pass
    JNDIDataBroker searchBroker = null;            // another JNDIDataBroker used for searching, and the search tree.
    public OfflineDataBroker offlineBroker = null;        // provides a gateway to ldif files.
    SchemaDataBroker schemaBroker = null;          // provides access to an artificaial 'schema tree'


    // use 'getActiveTree()' to find the currently shown tree.
    public SmartTree mrTree = null;              // the 'main' browse tree
    SmartTree searchTree = null;          // the search results tree
    SmartTree schemaTree = null;          // the schema display tree

    AttributeDisplay mainViewer;                // the main display panel

    CBPanel statusDisplay;
    JLabel displayLabel;

    protected MainMenu mainMenu;

    protected ButtonRegister buttonRegister = null;      //TE: Object that is used by JXplorer to register all its buttons and menu items.

    protected StopMonitor stopMonitor;

    public Thread jndiThread, schemaThread, searchThread, offlineThread;

    public String url = "Disconnected";         //TE: an anchor for the LDAP/DSML url.

    private static Logger log = Logger.getLogger(JXplorerBrowser.class.getName());

    //TODO: tighten this up - it sometimes gets out of synch with whether we're actually connected to a directory
    boolean connected = false;  //TE: a vague flag that is set to true if the user hits the connect button, false if user hits disconnect button.  This is for changing the state of the buttons when flicking between tabs.

    //TODO: wrap this up in the 'connected' variable above?
    public boolean workOffline = false;

    public enum DisplayMode
    {BROWSE, SEARCH, SCHEMA}

    public JXplorerBrowser()
    {

    }

    public void init(JXplorer parent)
    {
        this.parent = parent;

        DataBrokerQueryInterface datamodifier;

        rootFrame = this;
        mainPane = rootFrame.getContentPane();
        mrTree = null;

        initJNDIBroker();

        initSearchBroker();

        initSchemaBroker();

        initOfflineBroker();

        initStopMonitor();

        buttonRegister = new ButtonRegister();

        setupGUI();

        setStatus(CBIntText.get("Not Connected"));

        setBackground(Color.white);

        datamodifier = (DataBrokerQueryInterface) this.offlineBroker;

        setInitialOfflineTreeBroker();

        setVisible(true);
    }


    public void setInitialOfflineTreeBroker()
    {
        mrTree.registerDataSource(this.offlineBroker);
    }


    public JXplorer getJXplorer()
    {
        return parent;    
    }

    public JNDIDataBroker getJndiBroker()
    {
        return jndiBroker;
    }

    /**
     * allows other components to set the display message.
     * TODO: probably shouldn't allow direct access - *shrug* - do we care? 
     * @return
     */
    public JLabel getDisplayLabel() { return displayLabel;}

    /**
     * Activate any special actions linked to
     * changes in logging levels (currently BER and SSL tracing)
     */
    public void checkSpecialLoggingActions()
    {
        if (CBUtility.getTrueLogLevel(log) == Level.ALL)
            jndiBroker.setTracing(true);
        else
            jndiBroker.setTracing(false);
    }


    public void initJNDIBroker()
    {
        jndiBroker = new JNDIDataBroker();
        if (CBUtility.getTrueLogLevel(log) == Level.ALL)
            jndiBroker.setTracing(true);  // set BER tracing on.

        jndiBroker.setTimeout(JXConfig.getIntProperty("option.ldap.timeout", 0));
        jndiBroker.setLimit(JXConfig.getIntProperty("option.ldap.limit", 0));
        jndiBroker.setPaging(Boolean.parseBoolean(JXConfig.getProperty("option.ldap.pagedResults", "false")), JXConfig.getIntProperty("option.ldap.pageSize", 1000));

        jndiThread = new Thread(jndiBroker, "jndiBroker Thread");
        jndiThread.start();
    }

    public void initSearchBroker()
    {
        searchBroker = new JNDIDataBroker(jndiBroker);
        searchThread = new Thread(searchBroker, "searchBroker Thread");
        searchThread.start();
    }


    public void initSchemaBroker()
    {
        schemaBroker = new SchemaDataBroker(jndiBroker);

        schemaThread = new Thread(schemaBroker, "schemaBroker Thread");
        schemaThread.start();
    }

    /**
     * initialise the offline broker, used for viewing ldif & csv
     * files independantly of a working directory.
     */
    public void initOfflineBroker()
    {
        offlineBroker = new OfflineDataBroker();

        offlineThread = new Thread(offlineBroker, "offlineBroker Thread");
        offlineThread.start();
    }

    public void initStopMonitor()
    {
        DataBroker[] brokerList = {jndiBroker, searchBroker, schemaBroker, offlineBroker};
        stopMonitor = new StopMonitor(brokerList, this);
    }

    /**
     * returns the current stop monitor object.
     */

    public StopMonitor getStopMonitor()
    {
        return stopMonitor;
    }

    /**
     * Starts the GUI off, calling subroutines to initialise the
     * overall window, the menu and the main panel.
     */

    protected void setupGUI()
    {
        setupLookAndFeel();

        setupWindowButtons();    // set response to window button clicks


        setupMenu();             // setup the menu items

        setupMainPanel();        // set up the main viewing panel

        setupStatusDisplay();    // set up the status panel

        setupFrills();           // do funny icons and logos 'n stuff

        //positionBrowser();       // set the size and location of the main browser window.
    }


    /**
     * Set the position and size of the browser, if one hasn't already been set.
     */
    /* LOGIC MOVED TO JXplorer.createNewWindow()
    protected void positionBrowser()
    {
        Rectangle bounds = getBounds();

        if (bounds == null || bounds.width < 100 || bounds.height < 100)
        {
            int width, height, xpos, ypos;

            try  // try to use the default of whatever the 'root' windows was last 
            {
                width = Integer.parseInt(JXConfig.getProperty("width"));
                height = Integer.parseInt(JXConfig.getProperty("height"));
            }
            catch (Exception e)
            {
                width = 1000;
                height = 800;  // emergency fallbacks
            }

            // In this unusual case, the centering will be slightly off - but we probably don't care.
            if (width < 100) width = 100;
            if (height < 100) height = 100;

            try
            {
                xpos = Integer.parseInt(JXConfig.getProperty("xpos"));
                ypos = Integer.parseInt(JXConfig.getProperty("ypos"));
            }
            catch (Exception e)
            {
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                xpos = (screen.width - width) / 2;
                ypos = (screen.height - height) / 2;
            }


            setBounds(xpos, ypos, width, height);
            setSize(width, height);
        }
    }
    */
    /**
     * This sets the initial look and feel to the local system
     * look and feel, if possible; otherwise uses the java default.
     * (Note that user can change this using the 'view' menu item,
     * set up below.)
     */

    protected void setupLookAndFeel()
    {
        try
        {
            UIManager.setLookAndFeel(JXConfig.getProperty("gui.lookandfeel"));        //TE: gets the look and feel from the property file.
        }
        catch (Exception exc)
        {
            log.warning("WARNING: Can't load Look and Feel: " + exc);
            try
            {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                JXConfig.setProperty("gui.lookandfeel", UIManager.getCrossPlatformLookAndFeelClassName());
            }
            catch (Exception exc2)
            {
                log.warning("ERRROR: Can't load default Look and Feel either! : " + exc2);
            }
        }
    }

    /**
     * Sets up the window behaviour to close this window, (and possibly the application if it is the final window) on
     * a window close.
     */

    protected void setupWindowButtons()
    {
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent ev)
            {
                closeWindow();
            }
        });
    }

    /**
     * Close the current window
     */
    protected void closeWindow()
    {
        parent.browserClosing(this);
        super.dispose();
    }

    /**
     * Call the parent to shutdown JXplorer.
     */
    protected void shutdown()
    {
        //parent.browserClosing(this);
        parent.shutdown();
    }


    /**
     * Sets up the menu items, along with appropriate listeners
     */

    protected void setupMenu()
    {
        if (JXConfig.getProperty("gui.menu", "true").equals("true"))
            mainMenu = new MainMenu(this);
    }


    /**
     * Returns Main Menu.
     *
     * @return the main menue.
     */

    public MainMenu getMainMenu()
    {
        return mainMenu;
    }


    /**
     * Returns the JNDIDataBroker used for searching, and the search tree.
     *
     * @return the search broker.
     */

    public JNDIDataBroker getSearchBroker()
    {
        return searchBroker;
    }


    /**
     * Returns the tree's tabbed pane.
     *
     * @return the tabbed pane.
     */

    public JTabbedPane getTreeTabPane()
    {
        return treeTabPane;
    }


    /**
     * Returns the Explore panel.
     *
     * @return the explore panel.
     */

    public JScrollPane getExplorePanel()
    {
        return explorePanel;
    }


    /**
     * Returns the panel for displaying the search results.
     *
     * @return the results panel.
     */

    public JScrollPane getResultsPanel()
    {
        return resultsPanel;
    }


    /**
     * Returns the search display tree.
     *
     * @return the search (or results) tree.
     */

    public SmartTree getSearchTree()
    {
        return searchTree;
    }


    /**
     * Returns the display tree.
     *
     * @return the explore tree.
     */

    public SmartTree getTree()
    {
        return mrTree;
    }


    /**
     * Returns the schema display tree.
     *
     * @return the schema tree.
     */

    public SmartTree getSchemaTree()
    {
        return schemaTree;
    }


    /**
     * Returns the JXplorer frame.
     *
     * @return the root frame.
     */

    public JFrame getRootFrame()
    {
        return rootFrame;
    }


    /**
     * Returns the Attribute Display.
     *
     * @return the main viewer.
     */

    public AttributeDisplay getAttributeDisplay()
    {
        return mainViewer;
    }


    /**
     * Sets up the main panel, and places the directory viewing
     * panels in it.
     */

    protected void setupMainPanel()
    {
        setupToolBars();

        setupActiveComponents();   // initialise the tree, result panel etc.
        // prior to adding to the tabbed panes below
        setupMainWorkArea();       // set up the split screen and tabbed panels.

        mainPane.setBackground(Color.lightGray);

        mainViewer.registerClassLoader(parent.getClassLoader());

        validate();
    }

    protected void setupToolBars()
    {

        topPanel = new CBPanel();
        searchBar = new SearchBar(this);  // set up the tool bar with quick search
        buttonBar = new ButtonBar(this);  // sets up the tool bar with the graphics icons

        topPanel.makeWide();

        topPanel.add(buttonBar);
        topPanel.addln(searchBar);
        mainPane.add(topPanel, BorderLayout.NORTH);

        mainPane.setBackground(Color.white);

        if (JXConfig.getProperty("gui.buttonbar", "true").equals("false"))
            buttonBar.setVisible(false);
        if (JXConfig.getProperty("gui.searchbar", "true").equals("false"))
            searchBar.setVisible(false);
    }

    /**
     * this is where smart objects such as the tree
     * viewer, the results viewer (from the search class)
     * and the attribute viewers get added to the panes.
     */
    protected void setupActiveComponents()
    {
        mainViewer = new AttributeDisplay(this, parent.getResourceLoader());

        mrTree = new SmartTree(this, CBIntText.get("Explore"), parent.getResourceLoader());
        mrTree.setBackground(new Color(0xF7F9FF));
        initialiseTree(mrTree, mainViewer, this);

        searchTree = new SmartTree(this, CBIntText.get("Results"), parent.getResourceLoader());
        searchTree.setBackground(new Color(0xEEFFFF));
        initialiseTree(searchTree, mainViewer, this);

        schemaTree = new SmartTree(this, CBIntText.get("Schema"), parent.getResourceLoader());
        schemaTree.setBackground(new Color(0xEEFFEE));
        schemaTree.getTree().setEditable(false);
        initialiseTree(schemaTree, mainViewer, this);

        mainViewer.registerComponents(mainMenu, buttonBar, mrTree.getTree(), mrTree.getPopupTool(), this);
    }

    public void initialiseTree(SmartTree tree, DataSink viewer, JXplorerEventGenerator gen)
    {
        if (viewer != null) tree.registerDataSink(viewer);
        if (gen != null) tree.registerEventPublisher(gen);
    }

    /**
     * The Status panel is the small (one line) panel at the bottom
     * of the browser that reports to users what is happening with
     * the browser (e.g. 'connecting', 'disconnected' etc.)
     */
    protected void setupStatusDisplay()
    {
        statusDisplay = new CBPanel();
        statusDisplay.makeHeavy();
        displayLabel = new JLabel(CBIntText.get("initialising..."));
        statusDisplay.addln(displayLabel);
        mainPane.add(statusDisplay, BorderLayout.SOUTH);
    }

    public String getStatus()
    {
        return displayLabel.getText();
    }

    /**
     * Sets a status message that is displayed on the bottom of
     * the screen.
     */

    public void setStatus(String s)
    {
        displayLabel.setText(s);
        displayLabel.repaint();          // XXX paintology
    }

    /**
     * saves the old Status message on the status stack
     * for later use, and sets status to a new message.
     *
     * @param newMessage the new status message to set
     *                   (note - this new Message is *not* saved on the stack!)
     */

    public void pushStatus(String newMessage)
    {
        statusStack.push(displayLabel.getText());
        setStatus(newMessage);
    }

    /**
     * recalls a status message saved via @pushStatus,
     * as well as setting it using @setStatus.
     *
     * @return the saved status message, in case anyone cares
     */

    public String popStatus()
    {
        String status;
        if (statusStack.empty())
            status = "";   // sanity check
        else
            status = (String) statusStack.pop();

        setStatus(status);
        return status;    // in case someone is interested...
    }

    /**
     * Gets the current display mode.
     * On error returns 'BROWSE'.
     * @return
     */
    public DisplayMode getDisplayMode()
    {
        if (treeTabPane == null) return DisplayMode.BROWSE;
        
        Component treePane = treeTabPane.getSelectedComponent();
        if (treePane == explorePanel)                // Explore.
            return DisplayMode.BROWSE;
        else if (treePane == resultsPanel)             // Search.
            return DisplayMode.SEARCH;
        else if (treePane == schemaPanel)           // Schema.
            return DisplayMode.SCHEMA;
        else
            return DisplayMode.BROWSE;
    }

    /**
     * Sets up the main work area, below the tool bar,
     * which displays the tree/browser panel, and the
     * results panel...
     */

    public JSplitPane getSplitPane() { return splitPane; }

    protected void setupMainWorkArea()
    {
        // make sure stuff has been done already is correct...


        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
        mainPane.add(splitPane, BorderLayout.CENTER);

        treeTabPane = new JTabbedPane();
        treeTabPane.setMinimumSize(new Dimension(100, 100));

        treeTabPane.setPreferredSize(new Dimension(320, 100));   //CB: screens have got bigger in the last ten years...

        /*
        if (CBUtility.isLinux())
            treeTabPane.setPreferredSize(new Dimension(265, 100));      //TE: bug 2538.
        else
            treeTabPane.setPreferredSize(new Dimension(300, 100));   //TE: was 220x100 but increased size to 250 to fit icons. // CB: made 300 to fit all tabs on OSX
        */
        /*
         *    Initialise the work area scroll panes.  Our user defined
         *    classes will be added to these, and will become magically
         *    scrollable.
         */

        explorePanel = new JScrollPane(mrTree);
        resultsPanel = new JScrollPane(searchTree);
        schemaPanel = new JScrollPane(schemaTree);

        explorePanel.getVerticalScrollBar().setUnitIncrement(16); // ScrollPane's aren't respecting scrollable tree component's getScrollableUnitIncrement() methods; who knows why.
        resultsPanel.getVerticalScrollBar().setUnitIncrement(16);
        schemaPanel.getVerticalScrollBar().setUnitIncrement(16);

        splitPane.add(treeTabPane, JSplitPane.LEFT, 0);

        if (JXConfig.getProperty("gui.viewPanel", "true").equals("true"))
        {
            userViewPanel = new JPanel(new BorderLayout());
            userViewPanel.add(mainViewer, BorderLayout.CENTER);
            splitPane.add(userViewPanel, JSplitPane.RIGHT, 1);
        }

        if (mrTree != null)
            treeTabPane.addTab(mrTree.getName(), new ImageIcon(Theme.getInstance().getDirImages() + "explore.gif"), explorePanel, "Displays the directory tree, and allows the user to graphically browse the directory.");     //TE: sets the tabs up with name, icon, component and tool tip.
        if (searchTree != null)
            treeTabPane.addTab(searchTree.getName(), new ImageIcon(Theme.getInstance().getDirImages() + "find.gif"), resultsPanel, "Displays the search results, and allows the user to graphically browse these results.");
        if (schemaTree != null)
            treeTabPane.addTab(schemaTree.getName(), new ImageIcon(Theme.getInstance().getDirIcons() + "schema.gif"), schemaPanel, "Displays the directory schema, and allows the user to graphically browse the schema.");


        // nb. Don't add Tab for Admin, this only appears if the user
        // successfully establishes at least one admin connection...

        /**
         *	This change listener is intended to listen for tab changes.
         *	It makes sure the entry is updated in the editor pane so that
         *	when changing between for example schema and explore, the last
         *	schema data is not displayed...instead the entry that is selected
         *	in the explore tab is displayed.  (Bug 2243).
         */

        treeTabPane.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                DisplayMode mode = getDisplayMode();

                ButtonRegister br = getButtonRegister();
                if (mode == DisplayMode.BROWSE)
                {
                    setStatus(CBIntText.get("Connected To ''{0}''", new String[]{url}));
                    if (br != null)
                        br.setEditingButtons(isConnected() && mrTree.isModifiable());    // CB: set editing button state
                    mrTree.refreshEditorPane();
                }
                else if (mode == DisplayMode.SEARCH)
                {
                    setStatus("Number of search results: " + String.valueOf(searchTree.getNumOfResults()));
                    if (br != null)
                        br.setEditingButtons(isConnected()&&searchTree.isModifiable());    // CB: set editing button state
                    searchTree.refreshEditorPane();
                }
                else if (mode == DisplayMode.SCHEMA)
                {
                    setStatus(CBIntText.get("Connected To ''{0}''", new String[]{url}));
                    if (br != null)                          //TE: disable buttons for schema display.
                        br.setEditingButtons(false);

                    schemaTree.refreshEditorPane();
                }
            }
        });
    }

    /**
     * A vague flag that is set to true if the user hits the connect button,
     * false if user hits disconnect button.  This is for changing the state
     * of the buttons when flicking between tabs.
     *
     * @return value of connected.
     */
    public boolean isConnected()
    {
        return connected;
    }

    /**
     * A vague flag that is set to true if the user hits the connect button,
     * false if user hits disconnect button.  This is for changing the state
     * of the buttons when flicking between tabs.
     *
     * @param connected state to set connected to.
     */
    public void setConnected(boolean connected)
    {
        this.connected = connected;
    }

    /**
     * Returns the tree that is currently being directly
     * displayed to the user.
     */

    public SmartTree getActiveTree()
    {

        switch (getDisplayMode())
        {
            case BROWSE: return mrTree;
            case SEARCH: return searchTree;
            case SCHEMA: return schemaTree;
        }

        // should have returned by now... this line should never be reached!
        log.warning("ERROR: Unable to establish active tree - panel");
        return null;

        /*
        int paneNumber = treeTabPane.getSelectedIndex();
        if (paneNumber == treeTabPane.indexOfTab(CBIntText.get("Explore")))
            return mrTree;
        else if (paneNumber == treeTabPane.indexOfTab(CBIntText.get("Results")))
            return searchTree;
        else if (paneNumber == treeTabPane.indexOfTab(CBIntText.get("Schema")))
            return schemaTree;
        */
    }

    /**
     * Make minor additions; the top-right window icon and the
     * title bar text.
     */

    protected void setupFrills()
    {
        //this.setIconImage(new ImageIcon(Theme.getInstance().getDirImages() + "ODlogo.gif").getImage());
        //this.setIconImage(getImageIcon(Theme.getInstance().getDirImages() + "ODlogo.gif").getImage());
        this.setTitle("JXplorer");
        this.setIconImage(JXplorer.getImageIcon("JX32.png").getImage());

        /* java 6 specific code - sigh - not yet
        ArrayList<Image> icons = new ArrayList<Image>();
        icons.add( getImageIcon( "JX16.png" ).getImage() );
        icons.add( getImageIcon( "JX32.png" ).getImage() );
        icons.add( getImageIcon( "JX48.png" ).getImage() );
        icons.add( getImageIcon( "JX128.png" ).getImage() );
        this.setIconImages( icons );
        */
    }



    /**
     * Before a new connection is made, the old display trees should be cleared.
     */
    public void preConnectionSetup()
    {
        if (mrTree == null) return;
        mrTree.clearTree();
        mrTree.setRootDN(SmartTree.NODATA_DN);

        treeTabPane.setSelectedIndex(0);

        if (searchTree == null) return;
        searchTree.clearTree();
        searchTree.setRootDN(SmartTree.NODATA_DN);

        if (schemaTree == null) return;
        schemaTree.clearTree();
        schemaTree.setRootDN(SmartTree.NODATA_DN);

        // TODO: maybe restart jndibroker thread somehow?
    }

    /**
     * Perform necessary setup after a connection has been established.
     * This will link up the trees with their respective data
     * brokers.<p>
     * <p/>
     * This does quite a bit of work, including trying to find the default
     * base naming context(s), and registering schema.
     */

    public boolean postConnectionSetup(JNDIDataBroker.DataConnectionQuery request)
    {
        searchTree.clearTree();
        if (workOffline == true)
        {
            workOffline = false;
            offlineBroker.clear();
            //mrTree.registerDataSource(jxplorer.jndiBroker, new DN(SmartTree.NODATA));
        }
        else
        {
            if (request.conData.getTemplateName() != null)
                setTitle(CBIntText.get("JXplorer") + " - " + request.conData.getTemplateName());
            else
                setTitle(CBIntText.get("JXplorer") + " - " + request.conData.getURL());
        }
        String baseDN = request.conData.baseDN;
        DN base = new DN(baseDN);
        DN[] namingContexts = null;

        int ldapV = request.conData.version;

        try
        {
            if (base == null || base.size() == 0 || jndiBroker.unthreadedExists(base) == false)
            {
                if (ldapV == 2)
                {
//                    if (jndiBroker.getDirOp().exists(base) == false) // bail out if we can't find the base DN for ldap v2
                        if (jndiBroker.unthreadedExists(base) == false) // bail out if we can't find the base DN for ldap v2
                    {
                        CBUtility.error("Error opening ldap v2 connection - bad base DN '" + ((base == null) ? "*null*" : base.toString()) + "' ");
                        disconnect();
                        return false;
                    }
                }
                else // for ldap v3, try to find a valid base.
                {
                    if (base != null && base.size() > 0)
                        log.warning("The Base DN '" + base + "' cannot be found.");

                    base = null;  // set base to a known state.

                    namingContexts = jndiBroker.readFallbackRoot();  // may return null, but that's o.k.

                    if (baseDN.trim().length() > 0)
                    {
// Change from user error message to log message CBErrorWin errWin = new CBErrorWin(this, CBIntText.get("The DN you are trying to access cannot be found or does not exist.  The fall back DN is ") + namingContexts[0].toString(), "DN Not Found"); 	//TE: user info.
                        if (namingContexts != null && namingContexts[0] != null)
                            log.warning("Cannot find the user-specified Base DN - Using the fall back DN '" + namingContexts[0].toString() + "'");
                        else
                            log.warning("WARNING: Cannot find the user-specified Base DN, and cannot read alternative from directory.  Leaving unset for the present.");
                    }

                    if (namingContexts != null && namingContexts.length == 1) // if we only have one context...
                        base = namingContexts[0];                             // ... make it our base
                }
            }

            mrTree.clearTree();
            mrTree.registerDataSource(jndiBroker);
        }
        catch (Exception ex)    // wierd things can go wrong here; especially if ldap v2
        {
            if (ldapV != 2)
            {
                CBUtility.error("Possible errors occurred while opening connection.", ex);  // if not ldap v2, try to carry on anyway
            }
            else  // if ldap v2, just bail out.
            {
                CBUtility.error("Error opening ldap v2 connection (possibly bad base DN?) ", ex);
                disconnect();
                return false;
            }
        }

        //
        //    Set up the initial state of the tree, either with the base DN given, or with
        //    a set of Naming Contexts read from 'jndiBroker.readFallbackRoot()', or by
        //    trying to expand a blank DN (i.e. by doing a list on "").
        //

        if (base != null)    // We've got a single base DN - use it to set the tree root...
        {
            mrTree.setRootDN(base);
            mrTree.expandRootDN();

            if (base.size() == 0)
            {
                mrTree.expandRoot();
                mrTree.getRootNode().setAlwaysRefresh(true);
            }
            else
            {
                mrTree.expandDN(base);
                makeDNAutoRefreshing(base);
            }

        }
        else if (namingContexts != null)  // We've got multiple naming contexts - add them all.
        {
//            mrTree.setRoot("");
            mrTree.setRootDN(new DN());
            mrTree.expandRootDN();
            
            for (int i = 0; i < namingContexts.length; i++)         // for each context
            {
                DN namingContext = namingContexts[i];           // get the 'base' DN
                SmartNode node = mrTree.addNode(namingContext); // add that to the tree as a node

                // *Amazing* but harmless Hack for Mr John Q. Birrell
                if (node.getChildCount() == 0)                  // if nothing is there already (might be if server mis-configured)
                    node.add(new SmartNode());                  // make that node expandable
            }

            for (int i = 0; i < namingContexts.length; i++)         // for each context
            {
                mrTree.expandDN(namingContexts[i]);                 // and make the node visible.
                makeDNAutoRefreshing(namingContexts[i]);

            }
        }
        else    // no information; try to expand an empty dn and see what we get!
        {
            mrTree.expandRoot();
            mrTree.getRootNode().setAlwaysRefresh(true);
        }

        searchTree.clearTree();
        searchBroker.registerDirectoryConnection(jndiBroker);
        searchTree.registerDataSource(searchBroker);
        searchTree.setRootDN(new DN());   // empty root

        schemaTree.clearTree();

        if (Integer.toString(ldapV) != null && ldapV > 2)
        {
            schemaBroker.registerDirectoryConnection(jndiBroker);
            schemaTree.registerDataSource(schemaBroker);
            schemaTree.setRootDN(new DN("cn=schema"));

            DXAttribute.setDefaultSchema(jndiBroker.getSchemaOps());
            DXAttributes.setDefaultSchema(jndiBroker.getSchemaOps());
        }
        else
        {
            DXAttribute.setDefaultSchema(null);
            DXAttributes.setDefaultSchema(null);
        }

        if (base != null)
        {
            jndiBroker.getEntry(base);   // read first entry.
            JXConfig.setDefaultProperty("baseDN", base.toString());

            url = request.conData.url;
            setStatus(CBIntText.get("Connected To ''{0}''", new String[]{url}));
        }

        boolean readOnlyStatus = jndiBroker.isReadOnly();
        getButtonRegister().setConnectedState(readOnlyStatus);

        mainMenu.setConnected(true);
        setConnected(true);

        return true;
    }

    protected void makeDNAutoRefreshing(DN dn)
    {
        try
        {
            TreePath path = ((SmartModel) mrTree.getModel()).getPathForDN(dn);
            if (path == null) throw new Exception("null path returned");
            Object[] nodes = path.getPath();
            for (int j = 0; j < nodes.length - 1; j++)
            {
                ((SmartNode) nodes[j]).setAlwaysRefresh(true);  // XXX hack for x500/ldap server compatibility - force refreshing if certain magic nodes are expanded.
            }

        }
        catch (Exception e)
        {
            log.info("INFO: messed up setting auto-expanding nodes for context '" + dn + "'");
        }
    }


    /**
     * Disables/enables the menu and button bar items to reflect a disconnected state.
     * Clears the Explore, Results and Schema trees and also sets the status message
     * to disconnected.
     */

    public void disconnect()
    {
        jndiBroker.disconnect();

        mrTree.goOffline(offlineBroker);
        schemaTree.goOffline(null);
        searchTree.goOffline(null);

        searchTree.setNumOfResults(0);

        mrTree.clearTree();
        schemaTree.clearTree();
        searchTree.clearTree();



        getButtonRegister().setDisconnectState();

        mainMenu.setDisconnected();
        setConnected(false);

        setStatus(CBIntText.get("Disconnected"));
        setTitle(CBIntText.get("JXplorer"));
    }


    /**
     * Disables/enables the menu and button bar items to reflect a disconnected state.
     * Also sets the status message to disconnected.
     */

    public void setDisconnectView()
    {
        mainMenu.setDisconnected();
        getButtonRegister().setDisconnectState();

        setStatus(CBIntText.get("Disconnected"));
    }


    //
    //    Make JXplorer into an event generating object, that can
    //    register ActionListeners and trigger actionEvents.
    //

    /**
     * Add the specified JXplorer listener to receive JXplorer events from
     * JXplorer. Currently,the only JXplorer event occurs when a user selects a DN.
     * If l is null, no exception is thrown and no JXplorer is performed.
     *
     * @param l the JXplorer listener
     * @see #removeJXplorerListener
     */

    public synchronized void addJXplorerListener(JXplorerListener l)
    {
        if (l != null)
            eventListeners.add(JXplorerListener.class, l);
    }

    /**
     * Remove the specified JXplorer listener so that it no longer
     * receives JXplorer events from this button. JXplorer events occur
     * when a user presses or releases the mouse over this button.
     * If l is null, no exception is thrown and no JXplorer is performed.
     *
     * @param l the JXplorer listener
     * @see #addJXplorerListener
     */
    public synchronized void removeJXplorerListener(JXplorerListener l)
    {
        if (l != null)
            eventListeners.remove(JXplorerListener.class, l);
    }


    /**
     * Creates JXplorer events
     * by dispatching them to any registered
     * <code>JXplorerListener</code> objects.
     * (Implements the JXplorerEventGenerator interface)
     * <p/>
     *
     * @param e the JXplorer event.
     * @see com.ca.directory.jxplorer.event.JXplorerEventGenerator
     * @see com.ca.directory.jxplorer.event.JXplorerListener
     */
    public void fireJXplorerEvent(JXplorerEvent e)
    {
        Object[] list = eventListeners.getListenerList();
        for (int index = list.length - 2; index >= 0; index -= 2)
        {
            if (list[index] == JXplorerListener.class)
            {
                ((JXplorerListener) list[index + 1]).JXplorerDNSelected(e);
            }
        }
    }


    /**
     * Returns the ButtonRegister object.
     *
     * @return the ButtonRegister object that is used by JXplorer to
     *         register all its buttons and menu items.
     * @see #buttonRegister
     */

    public ButtonRegister getButtonRegister()
    {
        return buttonRegister;
    }


    /**
     * Returns the (singleton) root JXplorer object
     * @return
     */
    public JXplorer getRootJXplorer()
    {
        return parent;
    }

    /**
     * Convenience method to get all current browsers (including calling one)
     * @return
     */
    public ArrayList<JXplorerBrowser> getAllBrowsers()
    {
        return parent.getBrowsers();
    }

    /**  ... unnecessary - set 'stateChanged()'
      * sets up the mouse listener to monitor mouse clicks.  At
      * the moment, the sole use of this is to check whether there
      * is an unsaved editor change.
      */
     /*
     protected void setBrowserMouseListener()
     {
         MouseListener ml = new MouseAdapter()
         {
             public void mousePressed(MouseEvent e)
             {
                 if (treeTabPane.contains(e.getPoint()))
                 {
                    mainViewer.checkForUnsavedChanges();
                 }
             }
         };

         addMouseListener(ml);

     }
     */
}
