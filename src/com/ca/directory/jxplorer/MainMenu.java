package com.ca.directory.jxplorer;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Logger;

import com.ca.directory.jxplorer.search.*;
import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.tree.*;
import com.ca.commons.cbutil.*;
import com.ca.commons.security.KeystoreGUI;

import java.io.File;
import java.io.IOException;


public class MainMenu extends JMenuBar
{

     //XXX in a fantasy alternate universe this could be rewritten as Action object
     //thingumies so that enabling/disabling stuff could be echoed to the button bar. or not.
     //XXX in the same fantasy universe instantiation of menuitems might be delayed until
     // the user actually selected the main menu item, thus shaving a few milliseconds
     // from the load lag time.

    private static Logger log = Logger.getLogger(MainMenu.class.getName());

    JXplorer jxplorer;
    JXOpenConWin getConnection = null;  // the connection window

    JMenu fileMenu;
    JMenuItem connect, disconnect, print, refreshTree, exit;

    JMenu editMenu;
    JMenuItem cut, copy, paste, delete, rename, copyDN, newEntry, pasteAlias;

    JMenu ldifMenu;
    JMenuItem fullExport, subExport, importFile, viewOffline;

    JMenu searchMenu;
    JMenuItem search, deleteFilter, attrList;

    JMenu bookmarkMenu;
    JMenuItem editBookmark, addBookmark, deleteBookmark;

    JMenu lookAndFeelMenu;
	JMenuItem refresh;

    JMenu optionsMenu, toolsMenu;
    JMenuItem advancedOptions;

    JMenu sslMenu;
    JMenuItem simpleSSL, authSSL, keystoreOptions;

    JMenu helpMenu;
    JMenuItem helpContents, helpSearch, helpAbout;

	SmartTree tree;

	String dirIcons;
	String dirImages;
	String dirTemplates;

    public static Properties myProperties;   // global variables for the browser, read from...
    public static String propertyFile;       // ...a user configurable file storing default properties.
    public static String localDir;           // local directory the browser is being run from...
    public static String fileURLPrefix;      // a prefix that converts local files into a url (e.g. file:///)

    public MainMenu(JXplorer jxplorer)
    {
        super();
        this.jxplorer = jxplorer;

		setImageDirs();	//TE: gets the image and icon dirs.

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

        jxplorer.setJMenuBar(this);

        setDisconnected();
    }

/*    protected void processEvent(AWTEvent e)
    {
System.out.println(CBIntText.get("snaffled event ") + e.toString());
        super.processEvent(e);

    }
*/

    protected void setupFileMenu(JMenu fileMenu)
    {
        // setup common menu listener

        ActionListener fileListener =  new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JMenuItem src = ((JMenuItem)e.getSource());

				tree = jxplorer.getActiveTree();

                if (src == connect)
                    connect();
                else if (src == disconnect)
                    disconnect();
                else if (src == print)
                    print();
                else if (src == refreshTree)
                    jxplorer.mrTree.collapse();
                else if (src == exit)
                    jxplorer.shutdown();

                jxplorer.repaint();
            }
        };

        // initialise menu items

        connect = setMenuItem(fileMenu, fileListener,
            new String[] {CBIntText.get("Connect"), "C", CBIntText.get("Connect to a directory server."), "E", dirImages+"connect.gif" } );

        disconnect = setMenuItem(fileMenu, fileListener,
            new String[] {CBIntText.get("Disconnect"), "D", CBIntText.get("Disconnect from a directory server."), "E", dirImages+"disconnect.gif" } );

        print = setMenuItem(fileMenu, fileListener,
            new String[] {CBIntText.get("Print"),   "P", CBIntText.get("Print out the current entry."), "E", dirImages+"print.gif" } );

        setMenuItem(fileMenu, fileListener, new String[] {"-", "",  "", "" } );

        refreshTree = setMenuItem(fileMenu, fileListener,
            new String[] {CBIntText.get("Refresh Tree"), "R", CBIntText.get("Forces the tree to be reloaded from the directory."), "E", dirImages+"refresh_all.gif" } );

        setMenuItem(fileMenu, fileListener, new String[] {"-", "",  "", "" } );

        exit = setMenuItem(fileMenu, fileListener,
            new String[] {CBIntText.get("Exit"),    "x", CBIntText.get("Quit the JXplorer application."), "E", dirImages+"blank.gif" } );

        ButtonRegister br = JXplorer.getButtonRegister();
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
            getConnection = new JXOpenConWin(jxplorer, jxplorer.displayLabel,
												JXplorer.getProperty("option.ssl.clientcerts"),
												JXplorer.getProperty("option.ssl.cacerts"),
												JXplorer.getProperty("option.ldap.referral"),
												JXplorer.getProperty("option.ldap.searchAliasBehaviour"));
            getConnection.setSize(450,340);
            CBUtility.center(getConnection, jxplorer);
        }
        else
        {
            getConnection.reinit( JXplorer.getProperty("option.ssl.clientcerts"),
                                  JXplorer.getProperty("option.ssl.cacerts"),
                                  JXplorer.getProperty("option.ldap.referral"),
                                  JXplorer.getProperty("option.ldap.searchAliasBehaviour"));

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
        jxplorer.disconnect();
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
        jxplorer.pushStatus("Printing...");
        jxplorer.mainViewer.print();
        jxplorer.popStatus();
    }

    public void setupEditMenu(JMenu editMenu)
    {
        // set up a common listener for this menu

        ActionListener editListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JMenuItem item = (JMenuItem)e.getSource();

				tree = jxplorer.getActiveTree();

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
            new String[] {CBIntText.get("New"), "Ctrl+N", CBIntText.get("Create a new entry."), "E", dirImages+"new.gif"} );

        copyDN = setMenuItem(editMenu, editListener,
            new String[] {CBIntText.get("Copy DN"), "Ctrl+Y", CBIntText.get("Copy the Distinguished Name of an entry to the clipboard."), "E", dirImages+"copy_dn.gif"} );

	    setMenuItem(editMenu, editListener, new String[] {"-", "", "", ""} );

        cut = setMenuItem(editMenu, editListener,
            new String[] {CBIntText.get("Cut Branch"), "Ctrl+U", CBIntText.get("Select a subtree to move."), "E", dirImages+"cut.gif"} );

        copy = setMenuItem(editMenu, editListener,
            new String[]  {CBIntText.get("Copy Branch"), "Ctrl+O", CBIntText.get("Select a subtree to copy."), "E", dirImages+"copy.gif"});

        paste = setMenuItem(editMenu, editListener,
            new String[] {CBIntText.get("Paste Branch"), "Ctrl+P", CBIntText.get("Paste a previously selected subtree."), "E", dirImages+"paste.gif"} );

        pasteAlias = setMenuItem(editMenu, editListener,
            new String[] {CBIntText.get("Paste Alias"), "", CBIntText.get("Paste an Alias."), "E", dirIcons+"alias.gif"} );

        setMenuItem(editMenu, editListener, new String[] {"-", "", "", ""} );

        delete = setMenuItem(editMenu, editListener,
            new String[] {CBIntText.get("Delete"), "Ctrl+D", CBIntText.get("Delete an entry."), "E", dirImages+"delete.gif"} );

        rename = setMenuItem(editMenu, editListener,
            new String[] {CBIntText.get("Rename"), "Ctrl+M", CBIntText.get("Rename an entry."), "E", dirImages+"rename.gif"} );

        ButtonRegister br = JXplorer.getButtonRegister();
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

                SmartTree activeTree = jxplorer.getActiveTree();
                boolean usingSearch = (activeTree == jxplorer.searchTree);

                if (item == fullExport)
                    ldifFullExport(activeTree, usingSearch);
                else if (item == subExport)
                    ldifSubExport(activeTree, usingSearch);
                else if (item == importFile)
                    importFile();
                else if (item == viewOffline)
                    viewOffline();

                jxplorer.repaint();
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

        ButtonRegister br = JXplorer.getButtonRegister();
        br.registerItem(br.LDIF, fullExport);
        br.registerItem(br.LDIF, subExport);
        br.registerItem(br.LDIF, importFile);
    }

    public void ldifFullExport(SmartTree activeTree, boolean usingSearch)
    {
        DN base = activeTree.getRootDN();
        DataSource datasource = activeTree.getDataSource();
        LdifExport export = new LdifExport(base, datasource, jxplorer.searchTree, usingSearch, jxplorer, HelpIDs.LDIF_EXPORT_TREE);
        export.setSize(360,120);
        export.setTitle(CBIntText.get("Export Full Tree"));
        CBUtility.center(export, jxplorer);
        export.setVisible(true);
    }

    public void ldifSubExport(SmartTree activeTree, boolean usingSearch)
    {
        DN base = activeTree.getCurrentDN();
        DataSource datasource = activeTree.getDataSource();
        LdifExport export = new LdifExport(base, datasource, jxplorer.searchTree, usingSearch, jxplorer, HelpIDs.LDIF_EXPORT_SUBTREE);
        export.setSize(360,120);
        export.setTitle(CBIntText.get("Export Subtree"));
        CBUtility.center(export, jxplorer);
        export.setVisible(true);
    }

    public void importFile()
    {
        DataSource datamodifier;
        if (jxplorer.workOffline)
            datamodifier = (DataSource)jxplorer.offlineBroker;
        else
        {
            if (jxplorer.jndiBroker.isActive())
                datamodifier = (DataSource)jxplorer.jndiBroker;
            else
            {
                CBUtility.error(jxplorer, "Error: Not Connected! (Did You Want to 'View Offline'?)");
                return;
            }
        }
        LdifImport imp = new LdifImport(datamodifier, jxplorer.mrTree, jxplorer, null);
    }

    public void viewOffline()
    {
        disconnect();
        jxplorer.setStatus("Working Offline");
        jxplorer.workOffline = true ;
        jxplorer.offlineBroker.clear();
        jxplorer.mrTree.registerDataSource(jxplorer.offlineBroker);
        jxplorer.mrTree.setRoot(new DN(SmartTree.NODATA));
        LdifImport imp = new LdifImport(jxplorer.offlineBroker, jxplorer.mrTree, jxplorer, null);

        // XXX This activates the rest of the LDIF menu, *but* it will leave the LDIF menu activated
        // XXX even if the view offline load fails for some reason :-(.  Not sure how to fix this
        // XXX problem... - CB

        ButtonRegister br = JXplorer.getButtonRegister();
        br.setItemEnabled(br.LDIF, true);

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
					tree = jxplorer.getActiveTree();
					tree.openSearch();											//TE: open the search window (via Smart Tree).
				}
				else if (item == deleteFilter)
				{
					if (sm.getFilterNames(SearchModel.ALLFILTERS).size()==0)
					{
						JOptionPane.showMessageDialog(jxplorer, CBIntText.get("There are no filters available to delete."), CBIntText.get("Nothing to Delete"), JOptionPane.INFORMATION_MESSAGE );
						return;
					}
					else
					{
						DeleteFilterGUI dfg = new DeleteFilterGUI(jxplorer);	//TE: open the delete filter dialog.
						dfg.setVisible(true);
					}
				}
				else if (item == attrList)
				{
					 ReturnAttributesDialog rad = new ReturnAttributesDialog(jxplorer);
					 rad.setVisible(true);
				}
				else
				{
					doSearch(item.getText(), ((myJMenuItem)item).getToolTipText());
				}

                jxplorer.repaint();
            }
        };

        search = setMenuItem(searchMenu, searchListener,
            new String[] {CBIntText.get("Search Dialog"),  "Ctrl+F", CBIntText.get("Search the directory."), "E", dirImages+"find.gif"} );

        deleteFilter = setMenuItem(searchMenu, searchListener,
            new String[] {CBIntText.get("Delete Filter"),  "", CBIntText.get("Delete an existing filter."), "E", dirImages+"delete.gif"} );

        attrList = setMenuItem(searchMenu, searchListener,
            new String[] {CBIntText.get("Return Attribute Lists"),  "", CBIntText.get("Opens a dialog that lets you manage the attributes that you wish to be returned in a search."), "E", dirImages+"return_attrs.gif"} );

        if(names.length > 0)
		    setMenuItem(searchMenu, searchListener, new String[] {"-", "", "", "", ""} );

        ButtonRegister br = JXplorer.getButtonRegister();
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
		DN dn = (baseDN==null) ? jxplorer.getActiveTree().getCurrentDN() : new DN(baseDN);

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
			sm.openRetAttrDisplay(jxplorer, retAttrsList, (jxplorer.getSearchTree()).getDataSource());
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
        JXplorer.setProperty("option.ldap.searchAliasBehaviour", aliasOption);

		//TE: run the search...
		SearchExecute.run(jxplorer.getSearchTree(),
							dn,
							filter,
							retAttrsList,
							searchLevel,
							jxplorer.getSearchBroker());

		//TE: go to results tab...
		jxplorer.getTreeTabPane().setSelectedComponent(jxplorer.getResultsPanel());
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
                        jxplorer.getTreeTabPane().setSelectedIndex(0);
						goToBookmark(toolTps[list.getSelectedIndex()].toString(), jxplorer.getActiveTree());
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

                jxplorer.getTreeTabPane().setSelectedIndex(0);

				tree = jxplorer.getActiveTree();

				if (item == editBookmark)
					tree.openEditBookmarkDialog();
				else if (item == addBookmark)
					tree.openAddBookmarkDialog(jxplorer.mrTree.getCurrentDN());
				else if (item == deleteBookmark)
					tree.openDeleteBookmarkDialog();
				else
					goToBookmark(((myJMenuItem)item).name, tree);

				jxplorer.repaint();
			}
		};

		addBookmark = setMenuItem(bookmarkMenu, bookmarkListener,
		new String[] {CBIntText.get("Add Bookmark"),  "Ctrl+B", CBIntText.get("Add a bookmark from the current DN."), "E", dirImages+"plus.gif"} );

        deleteBookmark = setMenuItem(bookmarkMenu, bookmarkListener,
        new String[] {CBIntText.get("Delete Bookmark"),"d", CBIntText.get("Delete a bookmark."), "E", dirImages+"delete.gif"} );

		editBookmark = setMenuItem(bookmarkMenu, bookmarkListener,
		new String[] {CBIntText.get("Edit Bookmark"),"i", CBIntText.get("Edit your bookmarks."), "E", dirImages+"edit.gif"} );


        ButtonRegister br = JXplorer.getButtonRegister();
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
       String status = ("false".equals(JXplorer.getProperty("gui.buttonbar")))?"U":"C";
       setCheckBoxMenu(lookAndFeelMenu, new String[][] {{CBIntText.get("Show Button Bar"),"B", CBIntText.get("Display the shortcut button toolbar."),   "E", status, dirImages+"blank.gif"}},
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    boolean selected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
                    jxplorer.buttonBar.setVisible(selected);
                    jxplorer.repaint();
                    JXplorer.setProperty("gui.buttonbar", String.valueOf(selected));
                }
            }
        );


        status = ("false".equals(JXplorer.getProperty("gui.searchbar")))?"U":"C";

        setCheckBoxMenu(lookAndFeelMenu, new String[][] {{CBIntText.get("Show Search Bar"),"w", CBIntText.get("Show the quick search tool bar."), "E", status, dirImages+"blank.gif"}},
           	new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    boolean selected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
                    jxplorer.searchBar.setVisible(selected);
                    jxplorer.repaint();
                    JXplorer.setProperty("gui.searchbar", String.valueOf(selected));
                }
            }
        );

		ActionListener viewListener = new ActionListener()
		{
		    public void actionPerformed(ActionEvent e)
		    {
		        JMenuItem item = (JMenuItem)e.getSource();

				tree = jxplorer.getActiveTree();

		        if (item == refresh)
 					tree.getPopupTool().refresh();

		        jxplorer.repaint();
		    }
		};

		setMenuItem(lookAndFeelMenu, viewListener, new String[] {"-", "", "", "", ""} );

		refresh = setMenuItem(lookAndFeelMenu, viewListener,
			new String[] {CBIntText.get("Refresh"), "Ctrl+R",CBIntText.get("Refreshes an Entry."), "E", dirImages+"refresh.gif"} );

        ButtonRegister br = JXplorer.getButtonRegister();
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
                        JXplorer.myProperties.setProperty(propName, String.valueOf(getState()));
                        jxplorer.repaint();
                    }
                });
            }

            // property may be externally changed: recheck every paint!
            public void paint(Graphics g)
            {    // weird !("false"... syntax to force default case to be true...
                boolean state = !("false".equalsIgnoreCase(JXplorer.myProperties.getProperty(propName)));
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
        browserSearchAliases.setState("finding".equals(JXplorer.getProperty("option.ldap.browseAliasBehaviour")));

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
                jxplorer.repaint();
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
                        JXplorer.setProperty("option.ldap.browseAliasBehaviour", "finding");
                        if (jxplorer.jndiBroker.getDirContext() != null)
                        {
                            jxplorer.jndiBroker.getDirContext().addToEnvironment("java.naming.ldap.derefAliases", "finding");
                            //System.out.println("set to: " + jxplorer.jndiBroker.getDirContext().getEnvironment().get("java.naming.ldap.derefAliases"));
                        }
                    }
                    else
                    {
                        JXplorer.setProperty("option.ldap.browseAliasBehaviour", "never");
                        if (jxplorer.jndiBroker.getDirContext() != null)
                        {
                            jxplorer.jndiBroker.getDirContext().addToEnvironment("java.naming.ldap.derefAliases", "never");
                            //System.out.println("set to: " + jxplorer.jndiBroker.getDirContext().getEnvironment().get("java.naming.ldap.derefAliases"));
                        }
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
        AdvancedOptions adOpt = new AdvancedOptions(jxplorer, this);
        CBUtility.center(adOpt, jxplorer);
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
                    KeystoreGUI win = new KeystoreGUI(jxplorer, JXplorer.getMyProperties(),
                                                     JXplorer.getProperty(JXplorer.CA_PATH_PROPERTY),
                                                     null,
                                                     JXplorer.getProperty(JXplorer.CA_TYPE_PROPERTY),
                                                     CBIntText.get("Manage Your Trusted Server Certificates."),
                                                     false, HelpIDs.SSL_CERTS, false);
                    win.setSize(450,320);
                    CBUtility.center(win, jxplorer);
                    win.setVisible(true);  // may modify jxplorer properties
                }
                else if (item == authSSL)
                {
                    KeystoreGUI win = new KeystoreGUI(jxplorer, JXplorer.getMyProperties(),
                                                     JXplorer.getProperty(JXplorer.CLIENT_PATH_PROPERTY),
                                                     null,
                                                     JXplorer.getProperty(JXplorer.CLIENT_TYPE_PROPERTY),
                                                     CBIntText.get("Manage Your Own Private Keys and Certificates."),
                                                     true, HelpIDs.SSL_CERTS, false);
                    win.setSize(450,440);
                    CBUtility.center(win, jxplorer);
                    win.setVisible(true);  // may modify jxplorer properties
                }

                /*
                 *     allow the user to select which keystore files and keystore types to use.
                 */

                else if (item == keystoreOptions)
                {
                    KeystoreOptions options = new KeystoreOptions(jxplorer, JXplorer.getMyProperties());
                    options.setSize(530,260);
                    CBUtility.center(options, jxplorer);
                    options.setVisible(true);
                }
                jxplorer.repaint();
            }
        };

        simpleSSL = setMenuItem(sslMenu, sslListener,
            new String[] {CBIntText.get("Trusted Servers and CAs"),"u",
                          CBIntText.get("Setup which servers you trust for SSL."),
                          "E", dirImages+"sslcert.gif"} );	//TE: changed mnemonic from 'S' to 'u'.

        authSSL = setMenuItem(sslMenu, sslListener,
            new String[] {CBIntText.get("Client Certificates"),"C",
                          CBIntText.get("Setup client authentication (if available)."),
                          ("none".equals(JXplorer.getProperty("authprovider"))?"D":"E"), dirImages+"sslkeycert.gif"} );

        keystoreOptions = setMenuItem(sslMenu, sslListener,
            new String[] {CBIntText.get("Advanced Keystore Options"),"K",
                          CBIntText.get("Select your keystore locations and the type of keystore to use."),
                          "E", dirImages+"blankRec.gif"} );

    }


    protected void setupToolsMenu(JMenu toolsMenu)
    {
        String [][] toolsMenuItems =
                { {CBIntText.get("Stop Action"),"A", CBIntText.get("Stop the currently executing browser action."), "E", dirImages+"stop.gif"} };

        setMenu(toolsMenu, toolsMenuItems, new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
				String cmd = ((JMenuItem)e.getSource()).getName();

                if (cmd.equals(CBIntText.get("Stop Action")))
                {
                    jxplorer.getStopMonitor().show();
                }
                jxplorer.repaint();
            }
        });

		//TE: only enables the stop menu item if there is a query to stop.  If the stop menu
		//	  item is repositioned up date the getItem(0) method to the appropriate index.
		jxplorer.getStopMonitor().addWatcher(toolsMenu.getItem(0));
    }


    protected void setupHelpMenu(JMenu helpMenu)
    {
        ActionListener helpListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JMenuItem item = (JMenuItem)e.getSource();

                if (jxplorer.helpSystem == null && item != helpAbout)               // reality check - the help system
                {                                                                   // seems a bit flaky
                    CBUtility.error(jxplorer, "Unable to open Help System", null);
                    return;
                }

                if (item == helpContents)
                    jxplorer.helpSystem.openTab(HelpIDs.TAB_TOC);     //TE: opens the help with the table of contents tab visible.
                else if (item == helpSearch)                //TE: opens the help with the search tab visible.
                    jxplorer.helpSystem.openTab(HelpIDs.TAB_SEARCH);
                else if (item == helpAbout)
                    showAboutMessage();

                jxplorer.repaint();
            }
        };

        helpContents = setMenuItem(helpMenu, helpListener,
            new String[] {CBIntText.get("Contents"), "C", CBIntText.get("Display the help index."), "E", dirImages+"content.gif" } );

        helpSearch = setMenuItem(helpMenu, helpListener,
            new String[] {CBIntText.get("Search"),   "r", CBIntText.get("Search help for a keyword."), "E", dirImages+"search.gif" } );

        setMenuItem(helpMenu, helpListener, new String[] {"-", "", "", "", ""  } );  // separator

        helpAbout = setMenuItem(helpMenu, helpListener,
            new String[] {CBIntText.get("About"),    "A", CBIntText.get("General information about JXplorer."), "E", dirImages+"about.gif" } );



    }

   /**
    * Displays an about dialog in the center of JXplorer.
    * Switch display depending on whether it is the open source version or the commercial version...
	*/
    public void showAboutMessage()
    {
        // Open source license...
        File licence = new File(System.getProperty("user.dir") + File.separator + "licence.txt");
        if (licence.exists())
        {
            String textBody = JXplorer.version+"\n\nCopyright \u00a9 2005 CA. All rights reserved.";

            try
            {
                textBody = CBUtility.readTextFile(licence);
                textBody = JXplorer.version + "\n" +
                           "\nWritten by: Chris Betts" +
                           "\n            Trudi Ersvaer\n" +
                           "\nThanks to:  Jason Paul" +
                           "\n            Lachlan O'Dea" +
                           "\n            Van Bui\n\n\n" +
                           textBody;

            }
            catch (IOException e) {} // should still be set to original CA text.

            CBAbout about = new CBAbout(jxplorer, textBody, new ImageIcon(dirTemplates + "JXAboutBottom.gif"),
                        new ImageIcon(dirTemplates + "JXAboutTop.gif"), CBIntText.get("OK"), CBIntText.get("Close this window"), CBIntText.get("About JXplorer"));

            about.setSize(477, 350);
            about.setResizable(true);
            CBUtility.center(about, jxplorer);
            about.setVisible(true);
        }
        else
        {
            /* I don't think we can ship with the CA tau code?? - CB
            // CA license...
            File ca_licence = new File(System.getProperty("user.dir") + File.separator + "ca_license.txt");
            String textBody = null;
            try
            {
                textBody = CBUtility.readTextFile(ca_licence);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            AboutBox about = new AboutBox(jxplorer, "eTrust\u2122 Directory JXplorer", JXplorer.version, "", "", textBody, HelpIDs.CONTACT_SUPPORT);
            about.setVisible(true);
            */
        }
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



   /**
    *	Sets the image & icon paths.
	*/

	public void setImageDirs()
	{
		dirImages = JXplorer.getProperty("dir.images");
	 	dirIcons = JXplorer.getProperty("dir.icons");
		dirTemplates = JXplorer.getProperty("dir.templates");
	}
}