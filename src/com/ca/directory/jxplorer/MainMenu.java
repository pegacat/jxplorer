package com.ca.directory.jxplorer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.ca.commons.cbutil.CBAbout;
import com.ca.commons.cbutil.CBCache;
import com.ca.commons.cbutil.CBIntText;
import com.ca.commons.cbutil.CBUtility;
import com.ca.commons.cbutil.Theme;
import com.ca.commons.naming.DN;
import com.ca.commons.naming.DXNamingEnumeration;
import com.ca.commons.security.KeystoreGUI;
import com.ca.directory.jxplorer.broker.DataBrokerQueryInterface;
import com.ca.directory.jxplorer.search.DeleteFilterGUI;
import com.ca.directory.jxplorer.search.ReturnAttributesDialog;
import com.ca.directory.jxplorer.search.SearchExecute;
import com.ca.directory.jxplorer.search.SearchModel;
import com.ca.directory.jxplorer.tree.SmartTree;


public class MainMenu extends JMenuBar
{

     //XXX in a fantasy alternate universe this could be rewritten as Action object
     //thingumies so that enabling/disabling stuff could be echoed to the button bar. or not.
     //XXX in the same fantasy universe instantiation of menuitems might be delayed until
     // the user actually selected the main menu item, thus shaving a few milliseconds
     // from the load lag time.

    private static Logger log = Logger.getLogger(MainMenu.class.getName());

    protected JXplorerBrowser browser;
    protected JXOpenConWin getConnection = null;  // the connection window

    protected JMenu fileMenu;
    protected JMenuItem connect, disconnect, print, refreshTree, newWindow, closeWindow, exit;

    protected JMenu editMenu;
    protected JMenuItem cut, copy, paste, delete, rename, copyDN, newEntry, pasteAlias;

    protected JMenu ldifMenu;
    protected JMenuItem fullExport, subExport, importFile, viewOffline;

    protected JMenu searchMenu;
    protected JMenuItem search, deleteFilter, attrList;

    protected JMenu bookmarkMenu;
    protected JMenuItem editBookmark, addBookmark, deleteBookmark;

    protected JMenu lookAndFeelMenu;
	protected JMenuItem refresh;

    protected JMenu optionsMenu, toolsMenu;
    protected JMenuItem advancedOptions;

    protected JMenu sslMenu;
    protected JMenuItem simpleSSL, authSSL, keystoreOptions;

    protected JMenu helpMenu;
    protected JMenuItem helpContents, helpSearch, helpAbout;

	protected SmartTree tree;

    public static Properties myProperties;   // global variables for the browser, read from...
    public static String propertyFile;       // ...a user configurable file storing default properties.
    public static String localDir;           // local directory the browser is being run from...
    public static String fileURLPrefix;      // a prefix that converts local files into a url (e.g. file:///)

    public MainMenu(JXplorerBrowser jxplorerBrowser)
    {
        super();
        this.browser = jxplorerBrowser;

        /*
         *    This is a long ftn that sets up lots and lots
         *    of menu items.
         */

        // String constants for menu items

        // Set up the file menu item
        fileMenu = new JMenu(CBIntText.get("File"));
        fileMenu.setMnemonic('F');
        setupFileMenu(fileMenu);


        editMenu = new JMenu(CBIntText.get("Edit"));
        editMenu.setMnemonic('E');
        setupEditMenu(editMenu);

        // Set up the look and feel view menu
        lookAndFeelMenu = new JMenu(CBIntText.get("View"));
        lookAndFeelMenu.setMnemonic('V');
        setupLookAndFeelMenu(lookAndFeelMenu);

        // Set up the ldif menu item
        ldifMenu = new JMenu(CBIntText.get("LDIF"));
        ldifMenu.setMnemonic('L');
        setupLdifMenu(ldifMenu);

        // Set up the search menu item
        searchMenu = new JMenu(CBIntText.get("Search"));
        searchMenu.setMnemonic('S');
        setupSearchMenu(searchMenu);

		 // Set up the bookmark menu item
        bookmarkMenu = new JMenu(CBIntText.get("Bookmark"));
        bookmarkMenu.setMnemonic('B');
        setupBookmarkMenu(bookmarkMenu);

        // Set up the options menu item
        optionsMenu = new JMenu(CBIntText.get("Options"));
        optionsMenu.setMnemonic('O');
        setupOptionsMenu(optionsMenu);

        // Set up the tools menu item
        toolsMenu = new JMenu(CBIntText.get("Tools"));
        toolsMenu.setMnemonic('T');
        setupToolsMenu(toolsMenu);

        // Set up the options menu item
        sslMenu = new JMenu(CBIntText.get("Security"));
        sslMenu.setMnemonic('i');
        setupSSLMenu(sslMenu);

        // Set up the help menu item
        helpMenu = new JMenu(CBIntText.get("Help"));
        helpMenu.setMnemonic('H');
        setupHelpMenu(helpMenu);

        // Set the overall Menu Bar
        this.add(fileMenu);
        this.add(editMenu);
        this.add(lookAndFeelMenu);
		this.add(bookmarkMenu);
        this.add(searchMenu);
        this.add(ldifMenu);
        this.add(optionsMenu);
        this.add(toolsMenu);
        this.add(sslMenu);
        this.add(helpMenu);

        jxplorerBrowser.setJMenuBar(this);

        setDisconnected();
    }

    protected void setupFileMenu(JMenu fileMenu)
    {
        // setup common menu listener

        ActionListener fileListener =  new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JMenuItem src = ((JMenuItem)e.getSource());

				tree = browser.getActiveTree();

                if (src == connect)
                    connect();
                else if (src == disconnect)
                    disconnect();
                else if (src == print)
                    print();
                else if (src == refreshTree)
                    browser.mrTree.collapse();
                else if (src == newWindow)
                    browser.parent.createNewBrowser();
                else if (src == closeWindow)
                    browser.closeWindow();
                else if (src == exit)
                    browser.shutdown();

                browser.repaint();
            }
        };

        // initialise menu items

        connect = setMenuItem(fileMenu, fileListener,
            new String[] {CBIntText.get("Connect"), "C", CBIntText.get("Connect to a directory server."), "E", Theme.getInstance().getDirImages()+"connect.gif" } );

        disconnect = setMenuItem(fileMenu, fileListener,
            new String[] {CBIntText.get("Disconnect"), "D", CBIntText.get("Disconnect from a directory server."), "E", Theme.getInstance().getDirImages()+"disconnect.gif" } );

        print = setMenuItem(fileMenu, fileListener,
            new String[] {CBIntText.get("Print"),   "P", CBIntText.get("Print out the current entry."), "E", Theme.getInstance().getDirImages()+"print.gif" } );

        setMenuItem(fileMenu, fileListener, new String[] {"-", "",  "", "" } );

        refreshTree = setMenuItem(fileMenu, fileListener,
            new String[] {CBIntText.get("Refresh Tree"), "R", CBIntText.get("Forces the tree to be reloaded from the directory."), "E", Theme.getInstance().getDirImages()+"refresh_all.gif" } );

        setMenuItem(fileMenu, fileListener, new String[] {"-", "",  "", "" } );

        newWindow = setMenuItem(fileMenu, fileListener,
            new String[] {CBIntText.get("New Window"), "N", CBIntText.get("Opens a New browser window."), "E", Theme.getInstance().getDirImages()+"new_window.gif" } );

        closeWindow = setMenuItem(fileMenu, fileListener,
            new String[] {CBIntText.get("Close Window"), "W", CBIntText.get("Closes this browser window."), "E", Theme.getInstance().getDirImages()+"blank.gif" } );

        setMenuItem(fileMenu, fileListener, new String[] {"-", "",  "", "" } );

        exit = setMenuItem(fileMenu, fileListener,
            new String[] {CBIntText.get("Exit JXplorer"),    "x", CBIntText.get("Quit the JXplorer application."), "E", Theme.getInstance().getDirImages()+"blank.gif" } );

        ButtonRegister br = browser.getButtonRegister();
        br.registerItem(br.CONNECT, connect);
        br.registerItem(br.DISCONNECT, disconnect);
        br.registerItem(br.PRINT, print);
        br.registerItem(br.REFRESH_TREE, refreshTree);
    }

    public void connect()
    {
        CBCache.cleanCache();    //TE: deletes any temp files.

        if (getConnection == null)
        {
			//TE: the new connection class extends CBOpenConWin...
            getConnection = new JXOpenConWin(browser, browser.getDisplayLabel(),
												JXConfig.getProperty("option.ssl.clientcerts"),
												JXConfig.getProperty("option.ssl.cacerts"),
												JXConfig.getProperty("option.ldap.referral"),
												JXConfig.getProperty("option.ldap.searchAliasBehaviour"));
            getConnection.setSize(450,340);
            CBUtility.center(getConnection, browser);
        }
        else
        {
            getConnection.reinit( JXConfig.getProperty("option.ssl.clientcerts"),
                                  JXConfig.getProperty("option.ssl.cacerts"),
                                  JXConfig.getProperty("option.ldap.referral"),
                                  JXConfig.getProperty("option.ldap.searchAliasBehaviour"));

        }

        getConnection.resetTitleAndPassword();
        getConnection.setVisible(true);
    }

    /**
     *    Called by JX to turn appropriate buttons on.
     */

    public void setConnected(boolean searchActive)
    {
        editMenu.setEnabled(true);
        searchMenu.setEnabled(searchActive);
        toolsMenu.setEnabled(true);
		bookmarkMenu.setEnabled(true);
    }

    public void disconnect()
    {
        browser.disconnect();
    }

    public void setDisconnected()
    {
        editMenu.setEnabled(false);
        searchMenu.setEnabled(false);
        toolsMenu.setEnabled(false);
		bookmarkMenu.setEnabled(false);
    }

    public void print()
    {
        browser.pushStatus("Printing...");
        browser.mainViewer.print();
        browser.popStatus();
    }

    public void setupEditMenu(JMenu editMenu)
    {
        // set up a common listener for this menu

        ActionListener editListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JMenuItem item = (JMenuItem)e.getSource();

				tree = browser.getActiveTree();

                if (item == cut)
                    tree.getPopupTool().cut();
                else if (item == copy)
                    tree.getPopupTool().copy();
                else if (item == paste)
                    tree.getPopupTool().paste();
                else if (item == delete)
                    tree.getPopupTool().delete();
                else if (item == rename)
                    tree.getPopupTool().rename();
                else if (item == copyDN)
                    tree.getPopupTool().copyDN();
                else if (item == newEntry)
                    tree.getPopupTool().newEntry();
                else if (item == pasteAlias)
                    tree.getPopupTool().pasteAlias();
            }
        };

        // define the individual menu items

        newEntry = setMenuItem(editMenu, editListener,
            new String[] {CBIntText.get("New"), "Ctrl+N", CBIntText.get("Create a new entry."), "E", Theme.getInstance().getDirImages()+"new.gif"} );

        copyDN = setMenuItem(editMenu, editListener,
            new String[] {CBIntText.get("Copy DN"), "Ctrl+Y", CBIntText.get("Copy the Distinguished Name of an entry to the clipboard."), "E", Theme.getInstance().getDirImages()+"copy_dn.gif"} );

	    setMenuItem(editMenu, editListener, new String[] {"-", "", "", ""} );

        cut = setMenuItem(editMenu, editListener,
            new String[] {CBIntText.get("Cut Branch"), "Ctrl+U", CBIntText.get("Select a subtree to move."), "E", Theme.getInstance().getDirImages()+"cut.gif"} );

        copy = setMenuItem(editMenu, editListener,
            new String[]  {CBIntText.get("Copy Branch"), "Ctrl+O", CBIntText.get("Select a subtree to copy."), "E", Theme.getInstance().getDirImages()+"copy.gif"});

        paste = setMenuItem(editMenu, editListener,
            new String[] {CBIntText.get("Paste Branch"), "Ctrl+P", CBIntText.get("Paste a previously selected subtree."), "E", Theme.getInstance().getDirImages()+"paste.gif"} );

        pasteAlias = setMenuItem(editMenu, editListener,
            new String[] {CBIntText.get("Paste Alias"), "", CBIntText.get("Paste an Alias."), "E", Theme.getInstance().getDirIcons()+"alias.gif"} );

        setMenuItem(editMenu, editListener, new String[] {"-", "", "", ""} );

        delete = setMenuItem(editMenu, editListener,
            new String[] {CBIntText.get("Delete"), "Ctrl+D", CBIntText.get("Delete an entry."), "E", Theme.getInstance().getDirImages()+"delete.gif"} );

        rename = setMenuItem(editMenu, editListener,
            new String[] {CBIntText.get("Rename"), "Ctrl+M", CBIntText.get("Rename an entry."), "E", Theme.getInstance().getDirImages()+"rename.gif"} );

        ButtonRegister br = browser.getButtonRegister();
        br.registerItem(br.PASTE, paste);
        br.registerItem(br.PASTE_ALIAS, pasteAlias);
        br.registerItem(br.COPY, copy);
        br.registerItem(br.COPY_DN, copyDN);
        br.registerItem(br.CUT, cut);
        br.registerItem(br.DELETE, delete);
        br.registerItem(br.NEW, newEntry);
        br.registerItem(br.RENAME, rename);

        br.setItemEnabled(br.PASTE, false);
        br.setItemEnabled(br.PASTE_ALIAS, false);
    }

    public void setupLdifMenu(JMenu ldifMenu)
    {
        // define an listener for the ldif menu

        ActionListener ldifListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JMenuItem item = (JMenuItem)e.getSource();

                SmartTree activeTree = browser.getActiveTree();
                boolean usingSearch = (activeTree == browser.searchTree);

                if (item == fullExport)
                    ldifFullExport(activeTree, usingSearch);
                else if (item == subExport)
                    ldifSubExport(activeTree, usingSearch);
                else if (item == importFile)
                    importFile();
                else if (item == viewOffline)
                    viewOffline();
                //else if (item==ldifTest)
                //     testImport();

                browser.repaint();
            }
        };

        // define the individual options.

        fullExport = setMenuItem(ldifMenu, ldifListener,
            new String[] {CBIntText.get("Export Full Tree"), "x", CBIntText.get("Export an LDIF file of the entire tree."), "E", ""} );

        subExport = setMenuItem(ldifMenu, ldifListener,
            new String[] {CBIntText.get("Export Subtree"),   "p", CBIntText.get("Export an LDIF file of the current subtree."), "E", ""} );

        importFile = setMenuItem(ldifMenu, ldifListener,
            new String[] {CBIntText.get("Import File"),      "I", CBIntText.get("Import an LDIF file into the directory."), "E", ""} );

        viewOffline = setMenuItem(ldifMenu, ldifListener,
            new String[] {CBIntText.get("View Offline"),     "w", CBIntText.get("View an LDIF file off-Line, without adding to a directory."), "E", ""} );

        ButtonRegister br = browser.getButtonRegister();
        br.registerItem(br.LDIF, fullExport);
        br.registerItem(br.LDIF, subExport);
        br.registerItem(br.LDIF, importFile);
        //br.registerItem(br.LDIF, viewOffline); // don't register view offline - it is never disabled...
    }

    public void ldifFullExport(SmartTree activeTree, boolean usingSearch)
    {
        DN base = activeTree.getRootDN();
        DataBrokerQueryInterface datasource = activeTree.getDataSource();
        LdifExport export = new LdifExport(base, datasource, browser.searchTree, usingSearch, browser, HelpIDs.LDIF_EXPORT_TREE);
        export.setSize(360,120);
        export.setTitle(CBIntText.get("Export Full Tree"));
        CBUtility.center(export, browser);
        export.setVisible(true);
    }

    public void ldifSubExport(SmartTree activeTree, boolean usingSearch)
    {
        DN base = activeTree.getCurrentDN();
        DataBrokerQueryInterface datasource = activeTree.getDataSource();
        LdifExport export = new LdifExport(base, datasource, browser.searchTree, usingSearch, browser, HelpIDs.LDIF_EXPORT_SUBTREE);
        export.setSize(360,120);
        export.setTitle(CBIntText.get("Export Subtree"));
        CBUtility.center(export, browser);
        export.setVisible(true);
    }

    public void importFile()
    {
        DataBrokerQueryInterface datamodifier;

        if (browser.workOffline )
            datamodifier = browser.offlineBroker;
        else
        {
            if (browser.getJndiBroker().isActive())
                datamodifier = browser.getJndiBroker();
            else
            {
                CBUtility.error(browser, "Error: Not Connected! (Did You Want to 'View Offline'?)");
                return;
            }
        }
        LdifImport imp = new LdifImport(datamodifier, browser.mrTree, browser, null);

        imp.selectAndImportFile();  // prompts user to select a file
    }

    /*
    public void testImport()
    {
        DataBrokerQueryInterface datamodifier;

        if (browser.workOffline) // shouldn't be possible; should be disabled in UI when offline
        {
            datamodifier = browser.offlineBroker;
        }
        else
        {
            if (browser.getJndiBroker().isActive())
                datamodifier = browser.getJndiBroker();
            else
            {
                CBUtility.error(browser, "Error: Not Connected! (Did You Want to 'View Offline'?)");
                return;
            }
        }
        LdifImport imp = new LdifImport(datamodifier, browser.mrTree, browser, null);

        imp.selectAndTestFile();  // prompts user to select a file
    }
    */

    public void viewOffline()
    {
        disconnect();
        browser.setStatus("Working Offline");
        browser.workOffline = true ;
        browser.offlineBroker.clear();
        browser.mrTree.registerDataSource(browser.offlineBroker);
        browser.mrTree.setRootDN(SmartTree.NODATA_DN);

        LdifImport imp = new LdifImport(browser.offlineBroker, browser.mrTree, browser, null);

        imp.selectAndImportFile();  // prompts user to select a file

        browser.setTitle(CBIntText.get("JXplorer") +" - " + imp.getFileName());

        // XXX This activates the rest of the LDIF menu, *but* it will leave the LDIF menu activated
        // XXX even if the view offline load fails for some reason :-(.  Not sure how to fix this
        // XXX problem... - CB

        ButtonRegister br = browser.getButtonRegister();
        br.setItemEnabled(ButtonRegister.LDIF, true);
        br.setItemEnabled(ButtonRegister.CUT, true);
        br.setItemEnabled(ButtonRegister.COPY, true);
        br.setItemEnabled(ButtonRegister.COPY_DN, true);
        br.setItemEnabled(ButtonRegister.REFRESH, true);
    }



   /**
    *	Sets up the search menu with menu items and listeners.  Dynamically adds search filter names to the
	*	menu.  If there is more than 15, these names are added to a scrollable list.  A search is conducted
	*	if a user selects one of these names.
	*	@param searchMenu the actual search menu that needs to be set up.
	*/

    protected void setupSearchMenu(JMenu searchMenu)
    {
        final SearchModel sm = new SearchModel();

		searchMenu.removeAll();

		ArrayList list = sm.getFilterNames(SearchModel.FULLNAMES);						//TE: get the full names of the saved search filters.
		Object names[] = list.toArray();
		Arrays.sort(names, new SearchModel.StringComparator());					//TE: sort the list alphabetically.

        ActionListener searchListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JMenuItem item = (JMenuItem)e.getSource();

				if (item == search)
				{
					tree = browser.getActiveTree();
					tree.openSearch();											//TE: open the search window (via Smart Tree).
				}
				else if (item == deleteFilter)
				{
					if (sm.getFilterNames(SearchModel.ALLFILTERS).size()==0)
					{
						JOptionPane.showMessageDialog(browser, CBIntText.get("There are no filters available to delete."), CBIntText.get("Nothing to Delete"), JOptionPane.INFORMATION_MESSAGE );
						return;
					}
					else
					{
						DeleteFilterGUI dfg = new DeleteFilterGUI(browser);	//TE: open the delete filter dialog.
						dfg.setVisible(true);
					}
				}
				else if (item == attrList)
				{
					 ReturnAttributesDialog rad = new ReturnAttributesDialog(browser);
					 rad.setVisible(true);
				}
				else
				{
					doSearch(item.getText(), ((myJMenuItem)item).getToolTipText());
				}

                browser.repaint();
            }
        };

        search = setMenuItem(searchMenu, searchListener,
            new String[] {CBIntText.get("Search Dialog"),  "Ctrl+F", CBIntText.get("Search the directory."), "E", Theme.getInstance().getDirImages()+"find.gif"} );

        deleteFilter = setMenuItem(searchMenu, searchListener,
            new String[] {CBIntText.get("Delete Filter"),  "", CBIntText.get("Delete an existing filter."), "E", Theme.getInstance().getDirImages()+"delete.gif"} );

        attrList = setMenuItem(searchMenu, searchListener,
            new String[] {CBIntText.get("Return Attributes"),  "", CBIntText.get("Opens a dialog that lets you manage the attributes that you wish to be returned in a search."), "E", Theme.getInstance().getDirImages()+"return_attrs.gif"} );

        if(names.length > 0)
		    setMenuItem(searchMenu, searchListener, new String[] {"-", "", "", "", ""} );

        ButtonRegister br = browser.getButtonRegister();
        br.registerItem(br.SEARCH, search);

		String[] searchValues = new String[names.length];
		String[] searchNames = new String[names.length];

		for(int i=0; i<names.length;i++)
		{
			String key = names[i].toString();
			searchNames[i] = key;
			searchValues[i] = sm.getLDAPFilter(searchNames[i]);
			searchNames[i] = searchNames[i].startsWith("JXFilter") ? searchNames[i].substring(9) : searchNames[i].substring(13);	//TE: cut the prefix off (e.g: either 'JXFilter' or 'JXTextFilter').
		}

		if (names.length>15)	//TE: add a scrollable list rather than a blown out menu!
		{
			searchMenu.add(getScrollList(searchNames, searchValues, searchMenu));
		}
		else
		{
			for(int i=0;i<names.length;i++)
			{
				myJMenuItem searchMenuItem = new myJMenuItem(searchNames[i]);
				searchMenuItem.name = searchValues[i];
				setMenuItem(searchMenu, searchMenuItem, searchListener, new String[] {"", "", searchValues[i], "E", ""});
			}
		}
    }



   /**
    *	Runs the search.  Uses the saved search params from the property file if there
	*	is any.
	*	@param name the name of the saved filter.
	*	@param filter the LDAP filter.
	*/

	public void doSearch(String name, String filter)
	{
        SearchModel sm = new SearchModel();

		//TE: base DN...
		String baseDN = sm.getValue(name+"."+SearchModel.BASEDN);
		DN dn = (baseDN==null) ? browser.getActiveTree().getCurrentDN() : new DN(baseDN);

		//TE: search level (base object, one level, full subtree)...
		int searchLevel = 2;
		try
		{
			searchLevel = Integer.parseInt(sm.getValue(name+"."+SearchModel.SEARCHLEVEL));
		}
		catch(NumberFormatException e)
		{
			searchLevel = 2;
		}

		//TE: Return attributes...
		String retAttrs = sm.getValue(name+"."+SearchModel.RETATTRS);
		String[] retAttrsList = null;

		if (retAttrs == null)  // CB if it isn't specified, just return object class
		{
			retAttrsList = new String[] {"objectClass"};
		}
		else if (retAttrs.equalsIgnoreCase(ReturnAttributesDialog.DEFAULT_RETURN_ATTRS)) // CB to return all attributes, use the magic value 'null' (see jndi.search() methods)
		{
            retAttrs = null;
		}
        else  //TE: there is a list of return attributes so get it...
        {
            retAttrsList = ReturnAttributesDialog.getReturnAttributes(retAttrs);
			sm.openRetAttrDisplay(browser, retAttrsList, (browser.getSearchTree()).getDataSource());
        }

        // Get the alias options...
        String find = name+"."+SearchModel.FIND;
        String search = name+"."+SearchModel.SEARCH;

        String aliasOption = "always";

        if (search.equalsIgnoreCase("false") && find.equalsIgnoreCase("false"))
            aliasOption = "never";
        else if (search.equalsIgnoreCase("true") && find.equalsIgnoreCase("false"))
            aliasOption = "searching";
        else if (search.equalsIgnoreCase("false") && find.equalsIgnoreCase("true"))
            aliasOption = "finding";

        log.info("Setting search alias option to: ["+aliasOption+"]");
        JXConfig.setProperty("option.ldap.searchAliasBehaviour", aliasOption);

		//TE: run the search...
		SearchExecute.run(browser.getSearchTree(),
							dn,
							filter,
							retAttrsList,
							searchLevel,
							browser.getSearchBroker());

		//TE: go to results tab...
		browser.getTreeTabPane().setSelectedComponent(browser.getResultsPanel());
	}



   /**
    *	Enables or disables the search menu items (below the separator).
	*	@param state disabled if false, enabled if true.
	*/

	public void setSearchEnabled(boolean state)
	{
		if(searchMenu==null)
			return;

		int items = searchMenu.getItemCount();

		for(int i=0;i<items;i++)
		{
			JMenuItem temp = searchMenu.getItem(i);

			if(temp instanceof myJMenuItem)
				temp.setEnabled(state);
		}
		searchMenu.repaint();
	}



   /**
    *	Calls the set up for the search menu.  Used to dynamically update the filter names within the search menu.
	*/

	public void updateSearchMenu()
	{
		setupSearchMenu(searchMenu);
	}



   /**
    *	Sets up a scrollable list with given items and tooltips.  Adds a listener to each list item.
	*	@param items an array of things (usually strings that are added to the list.
	*	@param toolTips and array of strings used as the tooltips for the items (i.e. tooltip[x] goes with item[x]).
	*	@param menuType the menu that the listener is reponding to (either the search menu or the bookmark menu).
	*	@return the scroll pane with the list and listeners added.
	*/

	protected JScrollPane getScrollList(Object[] items, String[] toolTips, JMenu menuType)
	{
		final Object[] names = items;
		final String[] toolTps = toolTips;
		final JMenu type = menuType;

		final JList list = new JList(names)
		{
			public String getToolTipText(MouseEvent e)  						//TE: add a tool tip!
			{
				int index = locationToIndex(e.getPoint());
				if (-1 < index)
					return toolTps[index];
				else
					return null;
			}
		};
		list.setToolTipText("");
		list.setBackground(Color.lightGray);

		list.addListSelectionListener(new ListSelectionListener (){
			public void valueChanged(ListSelectionEvent e){
				if (e.getSource() == list && !e.getValueIsAdjusting())			//TE: listener registers mouse down and mouse release!  This is only to work on the mouse release!
				{
					if (type==searchMenu)										//TE: search menu action.
					{
						doSearch(names[list.getSelectedIndex()].toString(), toolTps[list.getSelectedIndex()]);
						//SearchExecute.run(jxplorer.getSearchTree(), jxplorer.getActiveTree().getCurrentDN(), toolTps[list.getSelectedIndex()], null, 2, jxplorer.getSearchBroker());
						//jxplorer.getTreeTabPane().setSelectedComponent(jxplorer.getResultsPanel());
						searchMenu.getPopupMenu().setVisible(false);			//TE: kill the menu (wont do it automatically with the list in it).
						searchMenu.setSelected(false);
					}
					else if (type==bookmarkMenu)								//TE: bookmark menu action.
					{
                        browser.getTreeTabPane().setSelectedIndex(0);
						goToBookmark(toolTps[list.getSelectedIndex()].toString(), browser.getActiveTree());
						bookmarkMenu.getPopupMenu().setVisible(false);			//TE: kill the menu (wont do it automatically with the list in it).
						bookmarkMenu.setSelected(false);
					}
				}
		}});

		JScrollPane sp = new JScrollPane(list);
		sp.setPreferredSize(new Dimension(100, 300));
		sp.setMinimumSize(new Dimension(100, 300));
		sp.setAlignmentX(LEFT_ALIGNMENT);

		return sp;
	}



   /**
   	*	Sets up the bookmark menu with menu items and listeners.  Dynamically adds bookmark names to the
	*	menu.  If there is more than 15, these names are added to a scrollable list.  If a user selects
	*	one of these names, the tree jumps to that entry.
	*	@param bookmarkMenu the bookmark menu.
	*/

	protected void setupBookmarkMenu(JMenu bookmarkMenu)
	{
		bookmarkMenu.removeAll();

		ActionListener bookmarkListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JMenuItem item = (JMenuItem)e.getSource();

                browser.getTreeTabPane().setSelectedIndex(0);

				tree = browser.getActiveTree();

				if (item == editBookmark)
					tree.openEditBookmarkDialog();
				else if (item == addBookmark)
					tree.openAddBookmarkDialog(browser.mrTree.getCurrentDN());
				else if (item == deleteBookmark)
					tree.openDeleteBookmarkDialog();
				else
					goToBookmark(((myJMenuItem)item).name, tree);

				browser.repaint();
			}
		};

		addBookmark = setMenuItem(bookmarkMenu, bookmarkListener,
		new String[] {CBIntText.get("Add Bookmark"),  "Ctrl+B", CBIntText.get("Add a bookmark from the current DN."), "E", Theme.getInstance().getDirImages()+"plus.gif"} );

        deleteBookmark = setMenuItem(bookmarkMenu, bookmarkListener,
        new String[] {CBIntText.get("Delete Bookmark"),"d", CBIntText.get("Delete a bookmark."), "E", Theme.getInstance().getDirImages()+"delete.gif"} );

		editBookmark = setMenuItem(bookmarkMenu, bookmarkListener,
		new String[] {CBIntText.get("Edit Bookmark"),"i", CBIntText.get("Edit your bookmarks."), "E", Theme.getInstance().getDirImages()+"edit.gif"} );


        ButtonRegister br = browser.getButtonRegister();
        br.registerItem(br.BOOKMARKS, addBookmark);

		Properties propertyList = CBUtility.readPropertyFile("bookmarks.txt");

		DXNamingEnumeration keys = new DXNamingEnumeration(propertyList.keys());

        if(keys.size() > 0)
		    setMenuItem(bookmarkMenu, bookmarkListener, new String[] {"-", "", "", "", ""} );

        Hashtable bookmarkTable = new Hashtable();

		while (keys.hasMoreElements())
		{
			String key = keys.nextElement().toString();
			if (key.toLowerCase().startsWith("dn"))
                bookmarkTable.put(key.substring(key.indexOf(".")+1), propertyList.getProperty(key));
		}

        DXNamingEnumeration en = new DXNamingEnumeration(bookmarkTable.keys());
        en.sort();

        int size = en.size();

        String[] bookmarkVals = new String[size];
        String[] bookmarkNams = new String[size];

        int j=0;
        while(en.hasMore())
        {
            bookmarkNams[j] = (String)en.next();
            bookmarkVals[j] = (String)bookmarkTable.get(bookmarkNams[j]);
            j++;
        }

		if (size>15)
		{
			bookmarkMenu.add(getScrollList(bookmarkNams, bookmarkVals, bookmarkMenu));
		}
		else
		{
			for(int i=0; i<bookmarkVals.length; i++)
			{
				myJMenuItem bookmarkMenuItem = new myJMenuItem(bookmarkNams[i]);
				bookmarkMenuItem.name = bookmarkVals[i];
				setMenuItem(bookmarkMenu, bookmarkMenuItem, bookmarkListener, new String[] {"", "", CBIntText.get("Go to") + ": " + bookmarkVals[i]+".", "E", ""});
			}
		}
	}



   /**
    *	Enables or disables the bookmark menu items (not the add or edit items - just the actual
	*	saved bookmarks).
	*	@param state disabled if false, enabled if true.
	*/

	public void setBookmarksEnabled(boolean state)
	{
		if(bookmarkMenu==null)
			return;

		int items = bookmarkMenu.getItemCount();

		for(int i=0;i<items;i++)
		{
			JMenuItem temp = bookmarkMenu.getItem(i);

			if(temp instanceof myJMenuItem)
				temp.setEnabled(state);
		}
		bookmarkMenu.repaint();
	}



   /**
	*	Extends JMenuItem to add a public string that can be used to store the
	*	name of the menu item.
	*/

	public class myJMenuItem extends JMenuItem
	{


		public String name;


   	   /**
		*	Constructor that creates a menuItem with text.
		*/

		public myJMenuItem(String s) { super(s); }
	}



   /**
    *	Calls the set up for the bookmark menu.  Used to dynamically update the bookmark names within the bookmark menu.
	*/

	public void updateBookmarkMenu()
	{
		setupBookmarkMenu(bookmarkMenu);
	}



   /**
    *	Tries to display the entry that the user asks for via a bookmark.  It tries to
	*	expand the children of all the parents instead of just displaying the
	*	direct parents of the entry in question.
	*	@param dn the dn of the entry that is to be opened.
	*	@param tree the tree that holds all the entries.
	*
	*/

	public void goToBookmark(String dn, SmartTree tree)
	{
        tree.readAndExpandDN(new DN(dn));
	}



    /**
     *    View menu setup.  Adds two check boxes; the first is for displaying the button bar
     *    and the second is for displaying the search bar in JX.
     */
    protected void setupLookAndFeelMenu(JMenu lookAndFeelMenu)
    {
       String status = ("false".equals(JXConfig.getProperty("gui.buttonbar")))?"U":"C";
       setCheckBoxMenu(lookAndFeelMenu, new String[][] {{CBIntText.get("Show Button Bar"),"B", CBIntText.get("Display the shortcut button toolbar."),   "E", status, Theme.getInstance().getDirImages()+"blank.gif"}},
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    boolean selected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
                    browser.buttonBar.setVisible(selected);
                    browser.repaint();
                    JXConfig.setProperty("gui.buttonbar", String.valueOf(selected));
                }
            }
        );


        status = ("false".equals(JXConfig.getProperty("gui.searchbar")))?"U":"C";

        setCheckBoxMenu(lookAndFeelMenu, new String[][] {{CBIntText.get("Show Search Bar"),"w", CBIntText.get("Show the quick search tool bar."), "E", status, Theme.getInstance().getDirImages()+"blank.gif"}},
           	new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    boolean selected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
                    browser.searchBar.setVisible(selected);
                    browser.repaint();
                    JXConfig.setProperty("gui.searchbar", String.valueOf(selected));
                }
            }
        );

		ActionListener viewListener = new ActionListener()
		{
		    public void actionPerformed(ActionEvent e)
		    {
		        JMenuItem item = (JMenuItem)e.getSource();

				tree = browser.getActiveTree();

		        if (item == refresh)
 					tree.getPopupTool().refresh();

		        browser.repaint();
		    }
		};

		setMenuItem(lookAndFeelMenu, viewListener, new String[] {"-", "", "", "", ""} );

		refresh = setMenuItem(lookAndFeelMenu, viewListener,
			new String[] {CBIntText.get("Refresh"), "Ctrl+R",CBIntText.get("Refreshes an Entry."), "E", Theme.getInstance().getDirImages()+"refresh.gif"} );

        ButtonRegister br = browser.getButtonRegister();
        br.registerItem(br.REFRESH, refresh);
    }



    /**
     *    This is a bit evil - it links various options, saved in the
     *    dxconfig.txt java properties file, with user menu items.  To
     *    do this requires a few quick utility classes derived from menu
     *    items, which automatically read and set various properties.
     */
    protected void setupOptionsMenu(JMenu optionsMenu)
    {
       // confirm tree operation check box.


        //boolean initStatus =  !("false".equalsIgnoreCase(jxplorer.myProperties.getProperty("option.confirmTreeOperations")));

        /*
         *    A quickie inner class, to give us a JCheckBoxMenuItem that will check
         *    and update from a particular property before repainting (so that if
         *    the property has been changed elsewhere, the menu bar still displays
         *    the correct value...)
         */

        class PropertyCheckBoxMenuItem extends JCheckBoxMenuItem
        {
            final String propName;

            public PropertyCheckBoxMenuItem(String s, String propertyName)
            {
                super(CBIntText.get(s), true);            // state reset on first paint (see below).
                propName = propertyName;

                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JXConfig.setProperty(propName, String.valueOf(getState()));
                        browser.repaint();
                    }
                });
            }

            // property may be externally changed: recheck every paint!
            public void paint(Graphics g)
            {    // weird !("false"... syntax to force default case to be true...
                boolean state = !("false".equalsIgnoreCase(JXConfig.getProperty(propName)));
                if (state != getState()) setState(state);  // nb - this triggers another paint...!
                super.paint(g);
            }
        }

        final PropertyCheckBoxMenuItem confirmOps;
        final PropertyCheckBoxMenuItem confirmTableEditorUpdates;
        final PropertyCheckBoxMenuItem checkSchema;
        final JCheckBoxMenuItem browserSearchAliases;

        confirmOps =
            new PropertyCheckBoxMenuItem(CBIntText.get("Confirm Tree Operations"),  "option.confirmTreeOperations");
        setMenuItemState(optionsMenu, confirmOps, "C", CBIntText.get("Prompt the user whenever the tree will be modified?"), true);

        confirmTableEditorUpdates =
            new PropertyCheckBoxMenuItem(CBIntText.get("Confirm Table Editor Updates"),  "option.confirmTableEditorUpdates");
        setMenuItemState(optionsMenu, confirmTableEditorUpdates, "T", CBIntText.get("Display message to confirm successful updates in Table Editor?"), true);

        checkSchema =
            new PropertyCheckBoxMenuItem(CBIntText.get("Ignore Schema Checking"),  "option.ignoreSchemaOnSubmission");
        setMenuItemState(optionsMenu, checkSchema, "g", CBIntText.get("Don't check entry consistency before submission."), true);

        browserSearchAliases =
            new JCheckBoxMenuItem(CBIntText.get("Resolve Aliases while Browsing"));
        setMenuItemState(optionsMenu, browserSearchAliases, "A", CBIntText.get("Whether to browse the referenced object, or the alias entry itself."), true);
        browserSearchAliases.setState("finding".equals(JXConfig.getProperty("option.ldap.browseAliasBehaviour")));

        //TE: defines a listener for the Advanced Options menu item of the Options menu.
        ActionListener optionsListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JMenuItem item = (JMenuItem)e.getSource();

                if (item == advancedOptions)
                {
                    setUpAdvancedOptions();    //TE: sets up the AdvancedOptions dialog when user clicks on this menu item.
                }
                browser.repaint();
            }
        };

        advancedOptions = setMenuItem(optionsMenu, optionsListener,
                    new String[] {CBIntText.get("Advanced Options"), "d", CBIntText.get("Open the Advanced Options dialog."), "E", ""} );

        // add *another* action listener for specific handling...
        browserSearchAliases.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try
                {
                    if (browserSearchAliases.isSelected())
                    {
                        JXConfig.setProperty("option.ldap.browseAliasBehaviour", "finding");
                        browser.getJndiBroker().addToEnvironment("java.naming.ldap.derefAliases", "finding");
                        /*
                        if (browser.getJndiBroker().getLdapContext() != null)
                        {
                            browser.getJndiBroker().getLdapContext().addToEnvironment("java.naming.ldap.derefAliases", "finding");
]                        }
                        */
                    }
                    else
                    {
                        JXConfig.setProperty("option.ldap.browseAliasBehaviour", "never");
                        browser.getJndiBroker().addToEnvironment("java.naming.ldap.derefAliases", "never");
                        /*
                        if (browser.getJndiBroker().getLdapContext() != null)
                        {
                            browser.getJndiBroker().getLdapContext().addToEnvironment("java.naming.ldap.derefAliases", "never");
                        }
                        */
                    }
                }
                catch (Exception e2) {}   // XXX probably never happen :-)
            }
        });
    }



   /**
    *    Sets up the advanced options dialog (written so that it is set up when the user clicks on the menu item
    *    instead of being set up when JX starts.  The problem with advanced options being set up when JX starts
    *    (or really when MainMenu is set up) is that it forces CBHelpSystem to over ride the Thread priority so that
    *    a Help Set is supplied.
    */

    protected void setUpAdvancedOptions()
    {
        AdvancedOptions adOpt = new AdvancedOptions(browser, this);
        CBUtility.center(adOpt, browser);
        adOpt.setVisible(true);
    }


    protected void setupSSLMenu(JMenu sslMenu)
    {
        ActionListener sslListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JMenuItem item = (JMenuItem)e.getSource();

                if (item == simpleSSL)
                {
                    KeystoreGUI win = new KeystoreGUI(browser, JXConfig.getMyProperties(),
                                                     JXConfig.getProperty(JXConfig.CA_PATH_PROPERTY),
                                                     null,
                                                     JXConfig.getProperty(JXConfig.CA_TYPE_PROPERTY),
                                                     CBIntText.get("Manage Your Trusted Server Certificates."),
                                                     false, HelpIDs.SSL_CERTS, false);
                    win.setSize(450,320);
                    CBUtility.center(win, browser);
                    win.setVisible(true);  // may modify jxplorer properties
                }
                else if (item == authSSL)
                {
                    KeystoreGUI win = new KeystoreGUI(browser, JXConfig.getMyProperties(),
                                                     JXConfig.getProperty(JXConfig.CLIENT_PATH_PROPERTY),
                                                     null,
                                                     JXConfig.getProperty(JXConfig.CLIENT_TYPE_PROPERTY),
                                                     CBIntText.get("Manage Your Own Private Keys and Certificates."),
                                                     true, HelpIDs.SSL_CERTS, false);
                    win.setSize(450,440);
                    CBUtility.center(win, browser);
                    win.setVisible(true);  // may modify jxplorer properties
                }

                /*
                 *     allow the user to select which keystore files and keystore types to use.
                 */

                else if (item == keystoreOptions)
                {
                    KeystoreOptions options = new KeystoreOptions(browser, JXConfig.getMyProperties());
                    options.setSize(530,260);
                    CBUtility.center(options, browser);
                    options.setVisible(true);
                }
                browser.repaint();
            }
        };

        simpleSSL = setMenuItem(sslMenu, sslListener,
            new String[] {CBIntText.get("Trusted Servers and CAs"),"u",
                          CBIntText.get("Setup which servers you trust for SSL."),
                          "E", Theme.getInstance().getDirImages()+"sslcert.gif"} );	//TE: changed mnemonic from 'S' to 'u'.

        authSSL = setMenuItem(sslMenu, sslListener,
            new String[] {CBIntText.get("Client Certificates"),"C",
                          CBIntText.get("Setup client authentication (if available)."),
                          ("none".equals(JXConfig.getProperty("authprovider"))?"D":"E"), Theme.getInstance().getDirImages()+"sslkeycert.gif"} );

        keystoreOptions = setMenuItem(sslMenu, sslListener,
            new String[] {CBIntText.get("Advanced Keystore Options"),"K",
                          CBIntText.get("Select your keystore locations and the type of keystore to use."),
                          "E", Theme.getInstance().getDirImages()+"blankRec.gif"} );

    }


    protected void setupToolsMenu(JMenu toolsMenu)
    {
        String [][] toolsMenuItems =
                { {CBIntText.get("Stop Action"),"A", CBIntText.get("Stop the currently executing browser action."), "E", Theme.getInstance().getDirImages()+"stop.gif"} };

        setMenu(toolsMenu, toolsMenuItems, new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
				String cmd = ((JMenuItem)e.getSource()).getName();

                if (cmd.equals(CBIntText.get("Stop Action")))
                {
                    browser.getStopMonitor().show();
                }
                browser.repaint();
            }
        });

		//TE: only enables the stop menu item if there is a query to stop.  If the stop menu
		//	  item is repositioned up date the getItem(0) method to the appropriate index.
		browser.getStopMonitor().addWatcher(toolsMenu.getItem(0));
    }


    protected void setupHelpMenu(JMenu helpMenu)
    {
        ActionListener helpListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JMenuItem item = (JMenuItem)e.getSource();

                if (browser.getRootJXplorer().helpSystem == null && item != helpAbout)               // reality check - the help system
                {                                                                   // seems a bit flaky
                    CBUtility.error(browser, "Unable to open Help System", null);
                    return;
                }

                if (item == helpContents)
                    browser.getRootJXplorer().helpSystem.openTab(HelpIDs.TAB_TOC);     //TE: opens the help with the table of contents tab visible.
                else if (item == helpSearch)                //TE: opens the help with the search tab visible.
                    browser.getRootJXplorer().helpSystem.openTab(HelpIDs.TAB_SEARCH);
                else if (item == helpAbout)
                    showAboutMessage();

                browser.repaint();
            }
        };

        helpContents = setMenuItem(helpMenu, helpListener,
            new String[] {CBIntText.get("Contents"), "C", CBIntText.get("Display the help index."), "E", Theme.getInstance().getDirImages()+"content.gif" } );

        helpSearch = setMenuItem(helpMenu, helpListener,
            new String[] {CBIntText.get("Search"),   "r", CBIntText.get("Search help for a keyword."), "E", Theme.getInstance().getDirImages()+"search.gif" } );

        setMenuItem(helpMenu, helpListener, new String[] {"-", "", "", "", ""  } );  // separator

        helpAbout = setMenuItem(helpMenu, helpListener,
            new String[] {CBIntText.get("About JXplorer"),    "A", CBIntText.get("General information about JXplorer."), "E", Theme.getInstance().getDirImages()+"about.gif" } );



    }

   /**
    * Displays an about dialog in the center of JXplorer.
    * Switch display depending on whether it is the open source version or the commercial version...
	*/
    public void showAboutMessage()
    {
        // Open source license...
        File licence = new File(System.getProperty("user.dir") + File.separator + "licence.txt");

            String textBody = JXConfig.version+"\n\n";

        try
        {
            if (licence.exists())
                textBody = CBUtility.readTextFile(licence);
        }
        catch (IOException e)
        {
            log.severe("unable to read licence file - check 'licence.txt' for CA Open Source licence details");            
        }
                textBody = JXConfig.version + "\n" +
                           "\nWritten by: Chris Betts" +
                           "\n            Trudi Ersvaer\n" +
                           "\nThanks to:  Jason Paul" +
                           "\n            Lachlan O'Dea" +
                           "\n            Van Bui\n\n\n" +
                           textBody;


            CBAbout about = new CBAbout(browser, textBody, new ImageIcon(Theme.getInstance().getDirTemplates() + "JXAboutBottom.gif"),
                        new ImageIcon(Theme.getInstance().getDirTemplates() + "JXAboutTop.gif"), CBIntText.get("OK"), CBIntText.get("Close this window"), CBIntText.get("About JXplorer"));

            about.setSize(477, 350);
            about.setResizable(true);
            CBUtility.center(about, browser);
            about.setVisible(true);

    }



   /**
    *    Utility function; sets up a menu with a bunch of menu items
    *    defined as strings, with tooltips and enabled/disabled state.<p>
    *
    *    ... assumes listeners will be created for them elsewhere, and registered
    *    here.
    *    @param menu the menu object to add this menu item to
    *    @param menuItems an array of string arrays defining the various menu items: <br>
    *           each string array should have 4 elements:
    *           {"item name", "mnemonic", "tool tip text", enabled/disabled state ("E"/"D")}
    *    @param listener - a thingumy to monitor the menu item, and do something if it's
    *           checked.
    */

    protected void setMenu(JMenu menu, String [][] menuItems, ActionListener listener)
    {
        for (int i=0;i<menuItems.length;i++)
        {
            JMenuItem item = setMenuItem(menu, listener, menuItems[i]);
        }
    }

    /**
     *  Sets up a menu item with a list of strings
     *  @param menu the menu to add this menu item to
     *  @param menuItems - an array of 4 strings; the name, the hot key, the tip, and a single letter 'E' or 'D' for enabled/disabled
     *  @param listener an action listener for this menu item.
     *  @return the created menu item
     */

    protected JMenuItem setMenuItem(JMenu menu, ActionListener listener, String[] menuItems)
    {
        if (menuItems[0].equals("-"))
        {
            menu.add(new JSeparator());
            return null;
        }
        else
        {
			JMenuItem item = new JMenuItem(CBIntText.get(menuItems[0]), new ImageIcon(menuItems[4]));
			item.setName(menuItems[0]);		//TE: gives the menu item a name...fixes bug 2447 re stop button (because stop button was not assigned with a name).
			return setMenuItem(menu, item, listener, menuItems);
        }
    }


   /**
	*  Sets up a menu item with a list of strings
	*  @param menu the menu to add this menu item to
	*  @param menuItems - an array of 4 strings; the name, the hot key, the tip, and a single letter 'E' or 'D' for enabled/disabled
	*  @param listener an action listener for this menu item.
	*  @return the created menu item
	*/

	protected JMenuItem setMenuItem(JMenu menu, JMenuItem item, ActionListener listener, String[] menuItems)
	{
        //item.setName(menuItems[0]);

        setMenuItemState(menu, item, menuItems[1], menuItems[2], (menuItems[3].charAt(0)=='E'));
        if (listener != null) item.addActionListener(listener);
        return item;
    }

   /**
    *    Similar to @setMenu, but for checkboxes.  Note arguments; the string array is an
    *    element longer (to allow setting of check box state).
    *    @param menu the menu object to add this menu item to
    *    @param menuItems an array of string arrays defining the various menu items: <br>
    *           each string array should have 5 elements:
    *           {"item name", "mnemonic", "tool tip text", enabled/disabled state ("E"/"D"), and
    *             checked/unchecked state ("C"/"U") }
    *    @param listener - a thingumy to monitor the menu item, and do something if it's
    *           checked.
    */

   protected void setCheckBoxMenu(JMenu menu, String [][] menuItems, ActionListener listener)
    {
        for (int i=0;i<menuItems.length;i++)
        {
            if (menuItems[i][0].equals("-"))
                menu.add(new JSeparator());
            else
            {
                JCheckBoxMenuItem tmp = new JCheckBoxMenuItem(menuItems[i][0], new ImageIcon(menuItems[i][5]), (menuItems[i][4].charAt(0)=='C'));
                setMenuItemState(menu, tmp, menuItems[i][1], menuItems[i][2], (menuItems[i][3].charAt(0)=='E'));
                if (listener != null) tmp.addActionListener(listener);
            }
        }
    }

    /**
     *    Takes a menu Item and sets up state information such as the mnemonic, the tool tip
     *    text and the enabled/disabled state.
     *    @param menu the parent menu.
     *    @param menuItem the menu item being modified
     *    @param mnemonic the hot key (alt-'key', or prefixed with "ctr"+'key')
     *    @param toolTipText text displayed when the mouse hovers over component
     *    @param enabled whether the menu item is active, or greyed out.
     */

    protected void setMenuItemState(JMenu menu, JMenuItem menuItem, String mnemonic, String toolTipText, boolean enabled)
    {
        if (mnemonic.length()==1)
        {
            menuItem.setMnemonic(mnemonic.charAt(0));
        }
        else if (mnemonic.startsWith("Cnt")||mnemonic.startsWith("cnt")||mnemonic.startsWith("Ctr")||mnemonic.startsWith("ctr"))
        {
            char C = mnemonic.charAt(mnemonic.length()-1);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(C, java.awt.Event.CTRL_MASK, false));
        }
        menuItem.setToolTipText(toolTipText);
        menuItem.setEnabled(enabled);
        menu.add(menuItem);
    }

}