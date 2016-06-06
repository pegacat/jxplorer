package com.ca.directory.jxplorer.search;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.*;
import javax.naming.NamingException;
import java.io.*;

import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.HelpIDs;
import com.ca.directory.jxplorer.JXplorer;
import com.ca.directory.jxplorer.JXplorerBrowser;
import com.ca.directory.jxplorer.broker.JNDIDataBroker;

/**
*	This class acts as a item selector.  It sets up a dialog that lets you select items
*	from one list and add them to another list.  It also allows you to remove selected
*	items from the list.
 *
 *  Much of the class is static; there might be difficulties running two of these simultaneously...
*/
public class ReturnAttributesDialog extends CBDialog
{
    private static Logger log = Logger.getLogger(ReturnAttributesDialog.class.getName());

	private JList 			availableList, selectedList;
	private CBButton		btnAdd, btnRemove, btnSave, btnLoad, btnDelete;
	private JTextField		nameField;
    private JCheckBox       includeDNCheckBox;
	private ArrayList		arrayList = new ArrayList();
	private String			localDir="";
	private JXplorerBrowser browser;

    /**
     * Flag for save prompt.
     */
    private boolean hasSaved = false;

    /**
     * The property file which stores the return attributes list.
     */
    public static final	String FILENAME = "return_attributes.txt";

    /**
     * Flag that indicates that the DN should be included in the results window.
     */
    public static final String INCLUDE_DN = "[DN]";   

    /**
     * Used to indicate no return attributes.
     */
    public static final String DEFAULT_RETURN_ATTRS = "None";


    /*
     *  The locatio of the stored property file
     */
    static String configSavePath;

    /*
     * The common properties object used to save return attribute lists.
     */
    static Properties returnAttProperties;
    /**
    *	Static method that should be used rather than creating an object directly if
	*	you wish to get the user input after the user has finished making selections and
	*	has closed the window.
	*	<p>
	*	This method creates a CBListSelector object and calls its 'show' method.  When the
	*	user is finished with the dialog this method returns the user's selections.
	*	@param jx the parent frame (JXplorer).
	*	@return a list of user selections.
	*/
	public static ArrayList getSelectedValues(JXplorerBrowser jx)
	{
		ReturnAttributesDialog listSelector = new ReturnAttributesDialog(jx);

		listSelector.setVisible(true);

		return listSelector.getSelectedValues();
	}

   /**
    *	Returns the names of all the saved return-attributes lists from the property file
	*	'return_attributes.txt'.
	*	@param name the name of the return attribute list.
	*	@return an array containing all the names of the lists.
	*/
	public static String[] getReturnAttributes(String name)
	{
        ArrayList list = new ArrayList(0);

        if(returnAttProperties == null)
            return new String[] {"objectClass"};  // as a default, get the object class rather than nothing ("1.1")

        String value = returnAttProperties.getProperty(name);

        if(value == null)
            return new String[] {"objectClass"};  // as a default, get the object class rather than nothing ("1.1")

        getReturnAttributes(value, list);

        return (String[]) list.toArray(new String[list.size()]);
	}

   /**
    *	Returns the names of all the saved return-attributes lists from the property file
	*	'return_attributes.txt'.
	*	@param value the list of return attributes.
	*	@param list a list of return attributes that are obtained from the value.
	*/
	public static void getReturnAttributes(String value, ArrayList list)
	{
		if ((value.indexOf(";")>-1)==true)
		{
			list.add(value.substring(0, value.indexOf(";")));
			getReturnAttributes(value.substring(value.indexOf(";")+1), list);
		}
	}

   /**
    *	Returns the names of all the saved return-attributes lists from the property file
	*	'return_attributes.txt'.
	*	@return an array containing all the names of the lists.
	*/
	public static Object[] getSavedListNames()
	{
		Enumeration en = null;
		ArrayList list = new ArrayList(0);

		try
		{
			en = returnAttProperties.propertyNames();
		}
		catch(Exception e)
		{
			list.add(DEFAULT_RETURN_ATTRS);
			return list.toArray();
		}

		list.add(DEFAULT_RETURN_ATTRS);

		while (en.hasMoreElements())
		{
			list.add(en.nextElement().toString());
		}

		return list.toArray();
	}
   //XXPROP
   /**
    *	Sets up the property file called 'return_attributes.txt' in the user dir.
	*/
    /*
	public static Properties getProperties()
	{
		Properties myProperties = new Properties();

        String temp = System.getProperty("user.dir") + File.separator;
        if (temp==null) { log.warning("Unable to read user home directory."); return null;}

        myProperties = CBUtility.readPropertyFile(temp + FILENAME);
        if (myProperties.size()==0) { log.info("Initialising config file: " + temp + FILENAME); return null;}

		return myProperties;
	}
    */
   /**
    *	Sets up a dialog with two lists.  The list on the left displays all available values
	*	that the user can select from.  When the user selects a value it is displayed in the
	*	list on the right either by double clicking on the value or clicking the '>' button.
	*	The user can also remove a value from the selection list either by double clicking on
	*	it or clicking the '<' button.
	*	@param jx the parent frame (JXplorer).
	*/
	public ReturnAttributesDialog(JXplorerBrowser jx)
	{
		super(jx, CBIntText.get("Return Attributes"), HelpIDs.SEARCH_RETURN_ATTRIBUTES);

		this.browser = jx;

		//XXPROP setUpPropertyFile();
        if (returnAttProperties==null)
        {
            configSavePath = CBUtility.getPropertyConfigPath(JXplorer.APPLICATION_NAME, FILENAME);
            returnAttProperties = CBUtility.readPropertyFile(configSavePath);
        }

		CBPanel topPanel = new CBPanel();
		CBPanel leftPanel = new CBPanel();
		CBPanel middlePanel = new CBPanel();
		CBPanel rightPanel = new CBPanel();
		CBPanel bottomPanel = new CBPanel();

		// Top panel...
		topPanel.makeLight();
		topPanel.add(new JLabel(CBIntText.get("Name") + ": "));
		topPanel.makeWide();
		topPanel.add(nameField = new JTextField(CBIntText.get("Untitled")));
		topPanel.makeLight();
		topPanel.add(btnSave = new CBButton(CBIntText.get("Save"),
                CBIntText.get("Save your selected return attributes for use in the Search dialog.")));
		topPanel.add(btnLoad = new CBButton(CBIntText.get("Load"),
                CBIntText.get("Load an already saved list of return attributes.")));
		topPanel.add(btnDelete = new CBButton(CBIntText.get("Delete"),
                CBIntText.get("Delete a already saved list of return attributes.")));

		btnSave.setPreferredSize(new Dimension(62, 20));
		btnSave.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					save();
                    hasSaved = true;
		}});

		btnLoad.setPreferredSize(new Dimension(62, 20));
		btnLoad.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					load();
                    hasSaved = false;
		}});

		btnDelete.setPreferredSize(new Dimension(70, 20));
		btnDelete.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					delete();
                    hasSaved = true;
		}});

		// Left panel...
		leftPanel.addln(new JLabel(CBIntText.get("Available Attributes")+":"));
		leftPanel.makeHeavy();

		availableList = new JList(getAttributes());

        // Only one item can be selected at anyone time...
        availableList.setSelectionMode(0);

        // Tries to ensure that the selected item is visible...
		availableList.setSelectionModel(new CBSingleSelectionModel(availableList));

		leftPanel.addln(new JScrollPane(availableList));

		// Middle panel...
		btnAdd = new CBButton(CBIntText.get(">"),
                CBIntText.get("Add an attribute from the attribute list on the left to the selection list on the right."));
		btnAdd.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					add();
                    hasSaved = false;
		}});

		btnRemove = new CBButton(CBIntText.get("<"),
                CBIntText.get("Remove an attribute from the selection list on the right."));
		btnRemove.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					remove();
                    hasSaved = false;
		}});

		middlePanel.makeHeavy();
		middlePanel.addln(new JLabel("  "));
		middlePanel.makeLight();
		middlePanel.addln(btnAdd);
		middlePanel.addln(btnRemove);
		middlePanel.makeHeavy();
		middlePanel.addln(new JLabel("  "));

		// Right panel...
		rightPanel.addln(new JLabel(CBIntText.get("Selected Attributes")+":"));
		rightPanel.makeHeavy();

		selectedList = new JList();

        // Only one item can be selected at anyone time...
		selectedList.setSelectionMode(0);

        // Tries to ensure that the selected item is visible...
		selectedList.setSelectionModel(new CBSingleSelectionModel(selectedList));

		rightPanel.addln(new JScrollPane(selectedList));

        // Bottom panel...
        includeDNCheckBox = new JCheckBox(CBIntText.get("Include DN in search results."));
        includeDNCheckBox.setToolTipText(
                CBIntText.get("Click the checkbox if you want the DN of each result displayed in the results window."));

        bottomPanel.makeLight();
        bottomPanel.add(includeDNCheckBox);
		bottomPanel.makeHeavy();
		bottomPanel.addln(new JLabel("  "));

		// Main panel...
		display.addln(new JLabel("  "));
		display.makeWide();
		display.addln(topPanel);
		display.makeHeavy();
		display.add(leftPanel);
		display.makeLight();
		display.add(middlePanel);
		display.makeHeavy();
		display.add(rightPanel);
        display.newLine();
        display.makeLight();
		display.addln(bottomPanel);

		setSize(400, 350);
		CBUtility.center(this, owner);

		registerMouseListeners();
	}

   /**
    *	Gets a list of attributes that are available in the schema which can be used for
	*	searching.  These are used in the availableList part of the list.
	*	@return a string array of the available attributes to JX (null - if no schema publishing i.e. LDAP V2).
	*/
	protected String[] getAttributes()
	{
        try
        {
            JNDIDataBroker searchBroker = browser.getSearchBroker();
            //ArrayList en = searchBroker.getSchemaOps().listEntryNames("schema=AttributeDefinition,cn=schema");
            ArrayList en = searchBroker.getSchemaOps().getKnownAttributeNames();


            // Check for no schema publishing i.e. LDAP V2...
            if(en==null)
                return null;

            String[] temp = (String[]) en.toArray(new String[] {});
            Arrays.sort(temp, new CBUtility.IgnoreCaseStringComparator());

            return temp;
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "Error accessing attribute defs in ReturnAttributesDialog ", e);
            return null;
        }
	}


   /**
    *	Adds a double click mouse listener to both lists in this dialog.
	*	If the double click occurs in the list on the left, the 'add' method is
	*	called.  If the double click occurs in the list on the right, the
	*	remove method is called.
	*/
    protected void registerMouseListeners()
    {
		availableList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
			   	if (e.getClickCount() == 2)
					add();
                    hasSaved = false;
			}
		});

		selectedList.addMouseListener(new MouseAdapter()
		{
		    public void mouseClicked(MouseEvent e)
		    {
		       	if (e.getClickCount() == 2)
					remove();
                    hasSaved = false;
		    }
		});
    }

   /**
    *	Gets the selected item from the list on the left and adds it to
	*	the list on the right.  It uses a global array list to keep track
	*	of the selections in the right hand list.
	*/
	public void add()
	{
		try
		{
		    if(!arrayList.contains(availableList.getSelectedValue()))
				arrayList.add(availableList.getSelectedValue());

			selectedList.setListData(arrayList.toArray());
		}
		catch(Exception e)
		{
			log.warning("No selection to add.");
		}
	}

   /**
    *	Removes the selected item from the list on the left.  It uses a
	*	global array list to keep track of the selections in the right hand list.
	*/
	public void remove()
	{
		try
		{
			arrayList.remove(selectedList.getSelectedIndex());
			selectedList.setListData(arrayList.toArray());
		}
		catch(Exception e)
		{
			log.warning("No selection to remove.");
		}
	}

   /**
    * 	Saves a list to the property file called 'return_attributes.txt'.  Saves the list
	*	by 'name=value', where 'value' is the list of attributes to return separated by a ';'.
	*	For example myList=attr1;attr2;attr3;
	*	<p>
	*	Before saving some quick checks are done...
	*	<br>attributes have been selected,
	*	<br>the name is valid (not null, not empty and not 'Untitled),
	*	<br>the name doesn't already exist.
	*/
	public void save()
	{
		ArrayList list = getSelectedValues();

        // Any attributes selected?
		if (list==null)
		{
			JOptionPane.showMessageDialog(this,
                    CBIntText.get("Please select the return attributes that you want to save in your list"),
                    CBIntText.get("Nothing to Save"), JOptionPane.INFORMATION_MESSAGE );
			return;
		}

		String name = nameField.getText();

        // Valid name?
		if (name==null || name.trim().length() <= 0 || name.equalsIgnoreCase("Untitled"))
		{
			JOptionPane.showMessageDialog(this, CBIntText.get("Please enter a name for your list."),
                    CBIntText.get("Name Not Supplied"), JOptionPane.INFORMATION_MESSAGE );
			return;
		}

        // Name exists?
		if (exists(name))
		{
	        int response = JOptionPane.showConfirmDialog(this, CBIntText.get("Do you want to replace it?"),
                    CBIntText.get("List Exists"), JOptionPane.OK_CANCEL_OPTION);

	        if (response != JOptionPane.OK_OPTION)
	            return;
		}

		StringBuffer buffy = new StringBuffer(0);

        // Check if user wants to include the DN in the search results, if so add to list...
        if(includeDNCheckBox.isSelected())
            buffy.append(INCLUDE_DN + ";");

		for(int i=0; i<list.size();i++)
			buffy.append(list.get(i)+";");

		returnAttProperties.setProperty(name, buffy.toString());
		CBUtility.writePropertyFile(configSavePath, returnAttProperties, null);

		JOptionPane.showMessageDialog(this,
                CBIntText.get("Your return attributes list has been saved as ''{0}''.",new String[] {name}),
                CBIntText.get("Saved"), JOptionPane.INFORMATION_MESSAGE );

        // Set the search GUI to null so that it is forced to re-read it's config and pick up new lists...
		browser.getTree().setSearchGUI(null);
		browser.getSearchTree().setSearchGUI(null);
		browser.getSchemaTree().setSearchGUI(null);
	}

   /**
    *	Gets all the names of the saved list from the property file 'return_attributes.txt', then
	*	pops up a JOptionPane dialog that has a combo box displaying these names.  Gets the user selection
	*	then retreves the value from the property file.  Recall that in the property file the value is saved
	*	as a group of attribute names that are separated by a ';'.  This method calls the 'getListAttrs' method
	*	that recursively extracts the attribute names.  Once the names come back they are dropped into the
	*	selectedList (JList).
	*/
	public void load()
	{
		Enumeration en = returnAttProperties.propertyNames();

		ArrayList list = new ArrayList(0);

		while (en.hasMoreElements())
		{
            list.add((String)en.nextElement());
		}

		if (list.size()==0)
		{
			JOptionPane.showMessageDialog(this,
                    CBIntText.get("There are no filters available to load."),
                    CBIntText.get("Nothing to Load"), JOptionPane.INFORMATION_MESSAGE );
			return;
		}

		Object listOb[] = list.toArray();

		CBJComboBox loadCombo = new CBJComboBox(listOb);
		loadCombo.setRenderer(new CBBasicComboBoxRenderer(listOb));
		loadCombo.setPreferredSize(new Dimension(140, 20));
        int response = JOptionPane.showConfirmDialog(this, loadCombo,
                CBIntText.get("Select List"), JOptionPane.OK_CANCEL_OPTION);

        if (response != JOptionPane.OK_OPTION)
            return;

        // Default the check box to NOT checked...
        includeDNCheckBox.setSelected(false);

		String name = (loadCombo.getSelectedItem()).toString();
		String loadList = getList(name);

		nameField.setText(name);

        // Get rid of the old attributes that are floating around...
		arrayList.clear();
		getListAttrs(loadList, arrayList);

		selectedList.setListData(arrayList.toArray());
	}

   /**
    *	Recursively extracts the attributes from the saved return attributes list.
	*	@param loadList the list of file names separated by a ';'.
	*	@param list the list to store the file names in.
	*/
	public void getListAttrs(String loadList, ArrayList list)
	{
		if (loadList.indexOf(";") > -1)
		{
            String temp = loadList.substring(0, loadList.indexOf(";"));

            // Check if the attribute is the inlude DN flag, if it is just check the check box.
            // Otherwise add the attribute to the list of selected attributes.
            if(temp.equalsIgnoreCase(INCLUDE_DN))
                includeDNCheckBox.setSelected(true);
            else
			    list.add(temp);

            // Move along to the next index and call this method again...
			getListAttrs(loadList.substring(loadList.indexOf(";")+1), list);
		}
	}

   /**
    *	Returns true if the property file (return_attributes.txt) contains the supplied
	*	key name.
	*	@param name the name of the list for example, 'myList'.
	*	@return true if the property file contains the list, false otherwise.
	*/
	protected boolean exists(String name)
	{
        // Check if the list name already exists, if so return true...
		if(returnAttProperties.containsKey(name))
			return true;

		return false;
	}

   /**
    *	Returns the value from the property file of a given list name.
	*	@param name the key of the value that is being returned (e.g. 'myList').
	*	@return the value of the key i.e. the list.
	*/
	public String getList(String name)
	{
		return returnAttProperties.getProperty(name);
	}

   /**
    * 	Checks if the name of the list that the user wants to delete is valid, calls the remove method
	*	then clears the text in the name field.
	*/
	public void delete()
	{
		String toDelete = nameField.getText();

		if (toDelete==null || toDelete.trim().length() <= 0 || toDelete.equalsIgnoreCase("Untitled"))
		{
			JOptionPane.showMessageDialog(this,
                    CBIntText.get("Please enter the name of the list that you want to delete."),
                    CBIntText.get("Nothing to Delete"), JOptionPane.INFORMATION_MESSAGE );
			return;
		}
		else
		{
	        int response = JOptionPane.showConfirmDialog(this,
                    CBIntText.get("Are you sure you want to delete the list ''{0}''?", new String[] {toDelete}),
                    CBIntText.get("Delete List?"), JOptionPane.OK_CANCEL_OPTION);

	        if (response != JOptionPane.OK_OPTION)
	            return;
		}

		removeList(toDelete);

		nameField.setText("");

        // Get rid of the old attributes that are floating around...
		arrayList.clear();
		selectedList.setListData(arrayList.toArray());

        // Default the check box to NOT checked...
        includeDNCheckBox.setSelected(false);

        // Set the search GUI to null so that it is forced to re-read it's config and pick up new lists...
		browser.getTree().setSearchGUI(null);
		browser.getSearchTree().setSearchGUI(null);
		browser.getSchemaTree().setSearchGUI(null);
	}

   /**
    *	Removes a list from the properties file return_attributes.txt.
	*	@param name the list name (key) to be removed (deleted).
	*/
	protected void removeList(String name)
	{
		if(!returnAttProperties.containsKey(name))
			return;

		returnAttProperties.remove(name);
		CBUtility.writePropertyFile(configSavePath, returnAttProperties, null);
		removeFromSearch(name);
	}

   /**
    *	If a return attribute list is deleted check that the search file doesn't have
	*	a reference to it.  If it does delete it.
	*	@param name the name of the return attribute list that is being deleted.
	*/
	public void removeFromSearch(String name)
	{
		SearchModel sm = new SearchModel();
		sm.removeRetAttrs(name);
	}

   /**
    *	Returns a list of the values that the user has selected.
	*	@return a list of the values that the user has selected.
	*/
	public ArrayList getSelectedValues()
	{
		if(arrayList.isEmpty())
			return null;
		else
			return arrayList;
	}

   /**
    *   Overrides the doOK of CBDialog so that a prompt is displayed before the user exits.
    */
    public void doOK()
    {
        if(!hasSaved)
        {
            int response = JOptionPane.showConfirmDialog(this, CBIntText.get("Exit without saving the return attributes list?"),
                                                            CBIntText.get("Exit Without Saving"), JOptionPane.OK_CANCEL_OPTION);

            if (response == JOptionPane.OK_OPTION)
                super.doOK();
        }
        else
        {
            super.doOK();
        }
    }
}