package com.ca.directory.jxplorer.search;

import com.ca.commons.cbutil.*;
import com.ca.commons.naming.DN;
import com.ca.directory.jxplorer.HelpIDs;
import com.ca.directory.jxplorer.JXConfig;
import com.ca.directory.jxplorer.JXplorerBrowser;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * This class creates a dialog that has currently three tabs on it.  The first one is for
 * creating or building filters, the second on is for joining already created filters and the third
 * one allows the user to enter or paste in a text filter. This class acts as a
 * a controller between the view (build, join & text tabs) and the model (SearchModel) as well as being a part of
 * the view itself.
 * <p/>
 * This class, in brief, sets up a search dialog that can be used to build, load, view, edit, join or save filters aswell
 * as search an LDAP directory with the filter that the user has created.
 *
 * @author Trudi.
 */
public class SearchGUI extends CBDialog
{
    final JXplorerBrowser browser;
    protected JTabbedPane tabbedPane;
    JCheckBox aliasSearchCheckBox, aliasFindingCheckBox;
    JTextField baseDNTextField, filterNameTextField;
    CBJComboBox andOrCombo, searchLevelCombo, returnAttributesCombo;
    static final String[] andOrArray = new String[]{CBIntText.get("And"), CBIntText.get("Or")};
    static final String[] searchLevelArray = new String[]{CBIntText.get("Search Base Object"), CBIntText.get("Search Next Level"), CBIntText.get("Search Full Subtree"),};
    static final int BASEOBJECTSEARCH = 0, ONELEVELSEARCH = 1, FULLSUBTREESEARCH = 2;
    CBButton btnSave, btnLoad, btnMore, btnLess, btnView;
    BuildFilterPanel build;
    JoinFilterPanel join;
    TextFilterPanel text;
    SearchModel searchModel;
    String buildName = "Untitled", joinName = "Untitled", textName = "Untitled";
    CBButton[] btnEdit = new CBButton[50];
    int buttonCounter = 0;                //TE: a counter that keeps track of the number of created 'edit' buttons and is used when recalling buttons from the button array..
    String[] returnAttrs = null;

    enum FilterType
    {
        UI, JOIN, RAW
    }



    private static Logger log = Logger.getLogger(SearchGUI.class.getName());

    //private static ReturnAttributesDisplay rat = null;

    /**
     * Contructor that sets up the display and initiates the main search objects: SearchModel,
     * BuildFilterPanel, JoinFilterPanel and TextFilterPanel.
     *
     * @param browser JXplorer.
     * @param    baseDN the DN of the currently selected entry (i.e. unless changed is where the search will be conducted from).
     */
    public SearchGUI(DN baseDN, JXplorerBrowser browser)
    {
        super(browser, CBIntText.get("Search"), HelpIDs.SEARCH);
        this.browser = browser;

        build = new BuildFilterPanel(browser);
        join = new JoinFilterPanel(getEditButton());
        text = new TextFilterPanel();

        buttonCounter++;
        searchModel = new SearchModel();

        CBPanel panel = getMainPanel(baseDN);

        tabbedPane = new JTabbedPane();

        tabbedPane.addTab(CBIntText.get("Build Filter"), new ImageIcon(Theme.getInstance().getDirImages() + "build.gif"), build, CBIntText.get("Build a filter from scratch."));
        tabbedPane.addTab(CBIntText.get("Join Filters"), new ImageIcon(Theme.getInstance().getDirImages() + "join.gif"), join, CBIntText.get("Join filters that have been made in the Build tab."));
        tabbedPane.addTab(CBIntText.get("Text Filter"), new ImageIcon(Theme.getInstance().getDirImages() + "text.gif"), text, CBIntText.get("Type or paste a filter into the field in plain text."));

        OK.setText(CBIntText.get("Search"));

        display.makeHeavy();
        display.addln(panel);
        display.add(tabbedPane);
        display.makeLight();
        display.add(getButtonPanel());

        /*
        CBButton btnAttrs = new CBButton(CB IntText.get("Return Attributes"), CB IntText.get("Select Returning Attributes."));
        btnAttrs.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ArrayList list = CBListSelector.getSelectedValues(SearchGUI.this.browser, build.getAttributes(), CBIntText.get("Select Returning Attributes"), HelpIDs.SEARCH);
                if (list != null)
                    returnAttrs = (String[]) list.toArray(new String[list.size()]);
            }
        });
       */
        
        setSize(550, 400);
        CBUtility.center(this, this.browser);

        /**
         *  This change listener is intended to listen for tab changes.
         *	Updates the filter name as the user flicks between tabs &
         *	enables or disables the controller buttons depending on which
         *	tab is visible.
         */
        tabbedPane.addChangeListener(new ChangeListener()
        {
            private int oldIndex = -1;

            public void stateChanged(ChangeEvent e)
            {
                int index = tabbedPane.getSelectedIndex();

                if (index == 0)
                {
                    filterNameTextField.setText(buildName);
                    setButtons(true);
                }
                else if (index == 1)
                {
                    filterNameTextField.setText(joinName);
                    setButtons(true);
                }
                else if (index == 2)
                {
                    if (oldIndex == 0)
                    {
                        text.displayFilter(getLDAPFilter(FilterType.UI));
                    }
                    else if (oldIndex == 1)
                    {
                        text.displayFilter(getLDAPFilter(FilterType.JOIN));
                    }

                    filterNameTextField.setText(textName);
                    setButtons(false);
                }
                oldIndex = index;
            }
        });
        /*
        if (tabbedPane.getSelectedIndex()==0)
			{
				buildName = name; 						//TE: update the name of the build filter so the tab change listener knows the correct name of the filter.
				searchModel.saveFilter(name, getLDAPFilter());
			}
			else if (tabbedPane.getSelectedIndex()==1)
			{
				String filter = join.getFilter();

				if(recursiveFilterCheck(name, filter, "Save"))	//TE: stop the user from constructing a filter that is recursive.
					return;

				joinName = name;						//TE: update the name of the join filter so the tab change listener knows the correct name of the filter.
				searchModel.saveFilter(name, filter);
			}
         */

    }

    /**
     * Sets up a panel with the components which display the name of the filter, the base DN,
     * aliase prefs, search level prefs and the button panel.
     *
     * @param baseDN the Distinguished Name where the searching is done from.  This is added to a text field on this panel.
     * @return the panel with the components added.
     */
    public CBPanel getMainPanel(DN baseDN)
    {
        CBPanel panel = new CBPanel();

        //TE: adds a label & text field for the name of the filter...
        panel.add(new JLabel(CBIntText.get("Filter Name") + ": "));
        panel.makeWide();
        panel.add(filterNameTextField = new JTextField("Untitled"));
        panel.makeLight();
        panel.newLine();

        //TE: adds a label & text field for the name of the DN being searched from...
        panel.add(new JLabel(CBIntText.get("Start Searching From") + ": "));
        panel.makeWide();
        if (baseDN == null)
            panel.add(baseDNTextField = new JTextField(""));
        else
            panel.add(baseDNTextField = new JTextField(baseDN.toString()));
        panel.makeLight();
        panel.newLine();

        CBPanel optionsPanel = new CBPanel();    //TE: panel for adding the alias & search level panels to (for layout).

        //TE: alias check boxes...
        CBPanel aliasPanel = new CBPanel();
        aliasPanel.setBorder(new TitledBorder(CBIntText.get("Alias Options")));

        aliasPanel.makeWide();
        aliasPanel.addln(aliasSearchCheckBox = new JCheckBox(CBIntText.get("Resolve aliases while searching.")));
        aliasSearchCheckBox.setToolTipText(CBIntText.get("Resolve aliases while searching."));
        aliasPanel.addln(aliasFindingCheckBox = new JCheckBox(CBIntText.get("Resolve aliases when finding base object.")));
        aliasFindingCheckBox.setToolTipText(CBIntText.get("Resolve aliases when finding base object."));

        //TE: search level combo...
        CBPanel searchLevelPanel = new CBPanel();
        searchLevelPanel.setBorder(new TitledBorder(CBIntText.get("Search Level")));
        searchLevelPanel.addln(new JLabel(CBIntText.get("Select Search Level") + ": "));
        searchLevelPanel.makeWide();
        searchLevelPanel.addln(searchLevelCombo = new CBJComboBox(searchLevelArray));
        searchLevelCombo.setSelectedIndex(FULLSUBTREESEARCH);

        //TE: put the alias & search level panels on the options panel then add the options panel to the main panel...
        optionsPanel.add(aliasPanel);
        optionsPanel.makeWide();
        optionsPanel.addln(searchLevelPanel);

        panel.makeWide();
        panel.addln(optionsPanel);

        //TE: return attributes combo...
        CBPanel returnAttrsPanel = new CBPanel();
        returnAttributesCombo = new CBJComboBox(ReturnAttributesDialog.getSavedListNames());
        returnAttributesCombo.setSelectedItem(ReturnAttributesDialog.DEFAULT_RETURN_ATTRS);

        returnAttrsPanel.makeLight();
        returnAttrsPanel.add(new JLabel(CBIntText.get("Information to retrieve") + ": "));
        returnAttrsPanel.makeWide();
        returnAttrsPanel.addln(returnAttributesCombo);

        panel.addln(returnAttrsPanel);

        return panel;
    }

    /**
     * Sets the base DN in the base DN text field.
     *
     * @param baseDN the DN to search from.
     */
    public void setBaseDN(DN baseDN)
    {
        if (baseDN != null)
            baseDNTextField.setText(baseDN.toString());
    }

    /**
     * Returns a panel with four buttons on it: More, Less, Save & View.  The buttons are set up
     * with listeners and are placed one above the other.
     *
     * @return the panel with the buttons on it.
     */
    public CBPanel getButtonPanel()
    {
        CBPanel panel = new CBPanel();

        btnMore = new CBButton(CBIntText.get("More"), CBIntText.get("Add a Line to the search window."));
        btnMore.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (isFilterValid())
                {
                    if (tabbedPane.getSelectedIndex() == 0)    //TE: make sure that you are adding rows to the tab that is visible, not to both tabs!
                    {
                        build.addFilterRow();
                    }
                    else if (tabbedPane.getSelectedIndex() == 1 && buttonCounter < 50)
                    {
                        join.addFilterRow(getEditButton());
                        buttonCounter++;                    //TE: a counter that keeps track of the number of created 'edit' buttons.
                    }
                }
                else
                {
                    showMessage(CBIntText.get("There is an error in the filter; there appears to be missing information.\nPlease make sure you have entered all the information for the filter correctly,\nthen try to add more rows."), CBIntText.get("Missing Information"));
                }
            }
        });

        btnLess = new CBButton(CBIntText.get("Less"), CBIntText.get("Remove a Line from the search window."));
        btnLess.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (tabbedPane.getSelectedIndex() == 0)    //TE: make sure that you are removing rows from the tab that is visible, not from both tabs!
                {
                    build.removeFilterRow();
                }
                else if (tabbedPane.getSelectedIndex() == 1 && buttonCounter > 1)
                {
                    buttonCounter--;
                    join.removeFilterRow(btnEdit[buttonCounter]);
                }
            }
        });

        btnSave = new CBButton(CBIntText.get("Save"), CBIntText.get("Save this filter."));
        btnSave.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (isFilterValid())
                    save();
                else
                    showMessage(CBIntText.get("The filter cannot be constructed; there appears to be missing information.\nPlease make sure you have entered all the information for the filter correctly."), CBIntText.get("Missing Information"));
            }
        });

        btnLoad = new CBButton(CBIntText.get("Load"), CBIntText.get("Load a previously saved filter."));
        btnLoad.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (tabbedPane.getSelectedIndex() == 0)
                    loadBuild();
                else if (tabbedPane.getSelectedIndex() == 1)
                    loadJoin();
                else if (tabbedPane.getSelectedIndex() == 2)
                    loadText();
            }
        });

        btnView = new CBButton(CBIntText.get("View"), CBIntText.get("View this search filter as text."));
        btnView.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!isFilterValid())
                    showMessage(CBIntText.get("The filter cannot be constructed; there appears to be missing information.\nPlease make sure you have entered all the information for the filter correctly."), CBIntText.get("Missing Information"));
                else if (tabbedPane.getSelectedIndex() == 1 && recursiveFilterCheck(null, join.getFilter(), "View"))   // We don't care about the name of this filter b/c we are just viewing it.
                    return;
                else
                    showDialog(CBIntText.get("Current Filter"), getLDAPFilter());
            }
        });

        panel.makeHigh();                //TE: add a label that it takes up the any remaining space above the buttons, so that the buttons are at the bottom of the panel.
        panel.addln(new JLabel(" "));
        panel.makeLight();
        panel.addln(btnMore);
        panel.addln(btnLess);
        panel.addln(btnSave);
        panel.addln(btnLoad);
        panel.addln(btnView);

        return panel;
    }

    /**
     * Enables or disables the More, Less and View buttons.  We basically don't
     * want these buttons enabled if the user has selected the Text Filter tab.
     *
     * @param state the state of the buttons (enabled=true or disabled=false).
     */
    protected void setButtons(boolean state)
    {
        btnMore.setEnabled(state);
        btnLess.setEnabled(state);
        btnView.setEnabled(state);
    }

    /**
     * Displays a JOptionPane message with a text area containing the current filter.
     * The text area is set to do line wrapping and only vertical scrolling.
     *
     * @param title  the heading of the dialog that appears in the title bar.
     * @param filter the current LDAP filter (or any string to be displayed).
     */
    protected void showDialog(String title, String filter)
    {
        JTextArea area = new JTextArea(filter);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(300, 60));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JOptionPane.showMessageDialog(this, scrollPane, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Returns an edit button, i.e. a button labelled "Edit", has a dimension of 55x21, has a tooltip "Edit
     * this filter.", and has an action listener that calls the edit method with it's position (or row number).
     *
     * @return the configured button.
     */
    protected CBButton getEditButton()
    {
        btnEdit[buttonCounter] = new CBButton(CBIntText.get("Edit"), CBIntText.get("Edit this filter."));

        btnEdit[buttonCounter].setPreferredSize(new Dimension(55, 21));
        btnEdit[buttonCounter].addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                CBButton item = (CBButton) e.getSource();

                for (int i = 0; i < buttonCounter; i++)
                {
                    if (item == btnEdit[i])
                        edit(i);
                }
            }
        });

        return btnEdit[buttonCounter];
    }

    /**
     * Displays the filter(s) corresponding to the edit button that the user has selected.
     * The edit buttons live on the join panel.  If a user wants to edit a raw filter the raw filter
     * is displayed in the build panel otherwise if the user wants to edit a filter that is a combination
     * of filters, the filter is displayed in the join panel.
     *
     * @param row the row number that the filter name is to be taken from.
     */
    protected void edit(int row)
    {
        ArrayList list = searchModel.getFilterNames(SearchModel.BUILDFILTER);                        //TE: get all the raw filter names.
        String filter = join.getFilterComboValue(row);

        if (filter == null)                                                    //TE: will equal null if nothing to edit.
        {
            showMessage(CBIntText.get("There are no filters available to edit"), CBIntText.get("Nothing to Edit"));
            return;
        }
        else
        {
            try
            {
                if (list.contains(filter))                                    //TE: the filter is a raw filter which needs to be displayed in the build filter panel.
                {
                    buildName = filter;                                        //TE: set the global variable so that the correct name of the filter is displayed when tabs are changed.
                    tabbedPane.setSelectedIndex(0);                            //TE: change tabs.

                    build.displayFilter(searchModel.getFilter(filter));        //TE: send the raw filter off to get displayed.
                }
                else                                                        //TE: the filter is a combination of filters which need to be displayed in the join filter panel.
                {
                    String value = searchModel.getFilter(filter);            //TE: get the value of the filter.

                    setNumberOfRows(build.getOccurrences(searchModel.getFilter(filter), "JXFilter"));        //TE: ...set up that many number of rows.

                    ArrayList aList = searchModel.getJoinFilterNames(value);                                //TE: get the names of these filters.

                    if (!join.displayFilter(aList, value))
                    {
                        showMessage(CBIntText.get("Your filter cannot be edited."), CBIntText.get("Edit Unsuccessful"));
                    }
                    else
                    {
                        joinName = filter;
                        filterNameTextField.setText(joinName);                 //TE: set the global variable so that the correct name of the filter is displayed.
                    }
                }
            }
            catch (Exception e)                                //TE: the user has somehow selected nothing in either the attribute or the function combo.
            {
                showMessage(CBIntText.get("The filter cannot be constructed; there appears to be missing information.\nPlease make sure you have entered all the information for the filter correctly."), CBIntText.get("Missing Information"));
                return;
            }
        }
    }

    /**
     * Currently used by the loadJoin() and edit() methods to prompt the JoinFilterPanel class to either add or remove
     * rows.  If adding rows is required, the extra number of edit buttons are created before the hand-balling goes off
     * to the JoinFilterPanel class.
     *
     * @param rows the number of rows that need to be added.
     */
    protected void setNumberOfRows(int rows)
    {
        if (buttonCounter > rows)                    //TE: if there are more buttons than rows required...
        {
            while (buttonCounter > rows)            //TE: ...delete these buttons and any other components on that row until the correct number of rows are displayed.
            {
                buttonCounter--;                    //TE: make sure the button counter is updated.
                join.removeFilterRow(btnEdit[buttonCounter]);
            }
        }
        else if (buttonCounter < rows)                //TE: if there are less buttons than rows required...
        {
            while (buttonCounter < rows)            //TE: ...create and add these buttons and any other required components on that row until the correct number of rows are displayed.
            {
                join.addFilterRow(getEditButton());
                buttonCounter++;                     //TE: make sure the button counter is updated.
            }
        }
    }

    /**
     * Save the filter to the property file 'search_filter.txt".  Checks first to see if the filter exists.  If
     * it does a JOptionPane message asks the user if they want to replace it.  If the filter is saved successfully
     * a confirmation message is displayed again using JOptionPane.  This also updates the filter combo of the join
     * tab so that a user can see changes straight away.
     */
    protected void save()
    {
        String name = filterNameTextField.getText();    //TE: get the user defined name for the filter.
        boolean exists = false;                            //TE: true if the name exists, false otherwise.

        if (name == null || name.trim().length() <= 0 || name.equalsIgnoreCase("Untitled"))
        {
            showMessage(CBIntText.get("Please enter a name in the 'Filter Name' field for the filter that you are trying to save."), CBIntText.get("Name Required"));
            return;
        }
        else if (name.startsWith("JXFilter"))
        {
            showMessage(CBIntText.get("The name ''{0}'' is a reserved name.  Please enter a different name.", new String[]{name}), CBIntText.get("Naming Error"));
            return;
        }

        if (searchModel.exists(name))                    //TE: if the name exists ask the user if they want to replace it...
        {
            int response = JOptionPane.showConfirmDialog(this, CBIntText.get("The name ''{0}'' already exists.  Do you want to replace it?", new String[]{name}),
                    CBIntText.get("Select Filter"), JOptionPane.YES_NO_OPTION);

            if (response != JOptionPane.YES_OPTION)
                return;

            exists = true;
        }

        try
        {
            if (tabbedPane.getSelectedIndex() == 0)
            {
                buildName = name;                         //TE: update the name of the build filter so the tab change listener knows the correct name of the filter.
                searchModel.saveFilter(name, getLDAPFilter());
            }
            else if (tabbedPane.getSelectedIndex() == 1)
            {
                String filter = join.getFilter();

                if (recursiveFilterCheck(name, filter, "Save"))    //TE: stop the user from constructing a filter that is recursive.
                    return;

                joinName = name;                        //TE: update the name of the join filter so the tab change listener knows the correct name of the filter.
                searchModel.saveFilter(name, filter);
            }
            else if (tabbedPane.getSelectedIndex() == 2)
            {
                textName = name;                        //TE: update the name of the text filter so the tab change listener knows the correct name of the filter.
                searchModel.saveTextFilter(name, text.getFilter());
            }
        }
        catch (Exception e)                                //TE: the user has somehow selected nothing in either the attribute or the function combo.
        {
            showMessage(CBIntText.get("The filter cannot be constructed; there appears to be missing information.\nPlease make sure you have entered all the information for the filter correctly."), CBIntText.get("Missing Information"));
            return;
        }

        save(name);                                        //TE: save search levels, baseDN etc.

        if (!exists)                                        //TE: update the filter combo if the filter doesn't exist.  Otherwise you would need to close the search dialog to see any changes??
            join.updateFilterCombo(name);

        browser.getMainMenu().updateSearchMenu();            //TE: updates the Search menu items.

        showMessage(CBIntText.get("Your filter ''{0}'' was saved successfully.", new String[]{name}), CBIntText.get("Successful Save."));
    }

    /**
     * Saves the base DN, return attribute list name, the search level and the alias
     * state to the property file so they can be loaded similar to the filter.
     *
     * @param name the name of the filter, will be used in the key (e.g. name.baseDN=whatever).
     */
    protected void save(String name)
    {
        String baseDN = ((baseDNTextField.getText()).trim()).length() <= 0 ? ((browser.getTree()).getRootDN()).toString() : baseDNTextField.getText();
        searchModel.saveValue(name, SearchModel.BASEDN, baseDN);
        searchModel.saveValue(name, SearchModel.RETATTRS, (returnAttributesCombo.getSelectedItem()).toString());
        searchModel.saveSearchLevel(name, searchLevelCombo.getSelectedIndex());
        searchModel.saveAlias(name, SearchModel.FIND, aliasFindingCheckBox.isSelected());
        searchModel.saveAlias(name, SearchModel.SEARCH, aliasSearchCheckBox.isSelected());
    }

    /**
     * Displays a JOptionPane message which has a combo box containing all the possible filters that can be
     * loaded into the build display.  Gets the user selection and hand-balls the loading off to the BuildFilterPanel class.
     */
    protected void loadBuild()
    {
        ArrayList list = searchModel.getFilterNames(SearchModel.BUILDFILTER);    //TE: get the names of raw filters (i.e. (cn=f*)).

        if (list.size() == 0)
        {
            showMessage(CBIntText.get("There are no filters available to load."), CBIntText.get("Nothing to Load"));
            return;
        }

        Object listOb[] = list.toArray();
        Arrays.sort(listOb, new SearchModel.StringComparator());                //TE: sort the list alphabetically.

        CBJComboBox loadCombo = new CBJComboBox(listOb);
        loadCombo.setRenderer(new CBBasicComboBoxRenderer(listOb));
        loadCombo.setPreferredSize(new Dimension(140, 20));
        int response = JOptionPane.showConfirmDialog(this, loadCombo, CBIntText.get("Select Filter"), JOptionPane.OK_CANCEL_OPTION);

        if (response != JOptionPane.OK_OPTION)
            return;                                                                //TE: the user has probably decided not to load a filter i.e. has clicked 'cancel'.

        if (loadCombo.getSelectedItem() != null)
        {
            String filter = null;
            try
            {
                filter = searchModel.getFilter(loadCombo.getSelectedItem().toString());    //TE: gets the filter that the user selected.
            }
            catch (Exception e)                                //TE: the user has somehow selected nothing in either the attribute or the function combo.
            {
                showMessage(CBIntText.get("The filter cannot be constructed; there appears to be missing information.\nPlease make sure you have entered all the information for the filter correctly."), CBIntText.get("Missing Information"));
                return;
            }

            if (!build.displayFilter(filter))                                    //TE: display the filter.  If unsuccessful show message.
            {
                showMessage(CBIntText.get("Your filter cannot be displayed."), CBIntText.get("Load Unsuccessful"));
            }
            else
            {
                buildName = loadCombo.getSelectedItem().toString();                //TE: update the name of the join filter so the tab change listener knows the correct name of the filter.
                filterNameTextField.setText(buildName);                            //TE: set the name of the filter in the filter name field.
            }
        }
        else
            showMessage(CBIntText.get("Problem loading; there are no filters selected."), CBIntText.get("Nothing to Load"));

        load(loadCombo.getSelectedItem().toString());
    }

    /**
     * Displays a JOptionPane message which has a combo box containing all the possible filters that can be
     * loaded into the join display.  Gets the user selection and creates the edit buttons before hand-balling
     * the loading off to the JoinFilterPanel class.
     */
    protected void loadJoin()
    {
        ArrayList list = searchModel.getFilterNames(SearchModel.JOINFILTER);    //TE: get the names of filters that are made up of other filters (i.e. not raw filters).

        if (list.size() == 0)
        {
            showMessage(CBIntText.get("There are no filters available to load."), CBIntText.get("Nothing to Load"));
            return;
        }

        CBJComboBox loadCombo = new CBJComboBox(list.toArray());
        loadCombo.setRenderer(new CBBasicComboBoxRenderer(list.toArray()));
        loadCombo.setPreferredSize(new Dimension(140, 20));
        int response = JOptionPane.showConfirmDialog(this, loadCombo,
                CBIntText.get("Select Filter"), JOptionPane.OK_CANCEL_OPTION);

        if (response != JOptionPane.OK_OPTION)
            return;                                                                //TE: the user has probably decided not to load a filter i.e. has clicked 'cancel'.

        if (loadCombo.getSelectedItem() != null)
        {
            String filter = loadCombo.getSelectedItem().toString();                //TE: get the user selected filter from the combo box.

            try
            {
                setNumberOfRows(build.getOccurrences(searchModel.getFilter(filter), "JXFilter"));    //TE: add the right number of rows for displaying the filter.

                list = searchModel.getJoinFilterNames(searchModel.getFilter(filter));                //TE: get a list of the subfilter names.

                if (!join.displayFilter(list, searchModel.getFilter(filter)))    //TE: display the filter.  If unsuccessful show message.
                    showMessage(CBIntText.get("Your filter cannot be displayed."), CBIntText.get("Load Unsuccessful"));
                else
                {
                    joinName = loadCombo.getSelectedItem().toString();            //TE: update the name of the join filter so the tab change listener knows the correct name of the filter.
                    filterNameTextField.setText(joinName);                        //TE: set the name of the filter in the filter name field.
                }
            }
            catch (Exception e)                                //TE: the user has somehow selected nothing in either the attribute or the function combo.
            {
                showMessage(CBIntText.get("The filter cannot be constructed; there appears to be missing information.\nPlease make sure you have entered all the information for the filter correctly."), CBIntText.get("Missing Information"));
                return;
            }
        }
        else
            showMessage(CBIntText.get("Problem loading; there are no filters selected."), CBIntText.get("Nothing to Load"));

        load(loadCombo.getSelectedItem().toString());
    }

    /**
     * Displays a JOptionPane message which has a combo box containing all the possible filters
     * that can be loaded into the text display.  Gets the user selection and hand-balls
     * the loading off to the JoinFilterPanel class.
     */
    //TE: Perhaps allow the user to load any filter into the text area, not just text filters???
    protected void loadText()
    {
        ArrayList list = searchModel.getFilterNames(SearchModel.TEXTFILTER);    //TE: get the names of filters that are made up of other filters (i.e. not raw filters).

        if (list.size() == 0)
        {
            showMessage(CBIntText.get("There are no filters available to load."), CBIntText.get("Nothing to Load"));
            return;
        }

        CBJComboBox loadCombo = new CBJComboBox(list.toArray());
        loadCombo.setRenderer(new CBBasicComboBoxRenderer(list.toArray()));
        loadCombo.setPreferredSize(new Dimension(140, 20));
        int response = JOptionPane.showConfirmDialog(this, loadCombo, CBIntText.get("Select Filter"), JOptionPane.OK_CANCEL_OPTION);

        if (response != JOptionPane.OK_OPTION)
            return;

        if (loadCombo.getSelectedItem() != null)
        {
            textName = loadCombo.getSelectedItem().toString();                    //TE: get the user selected filter from the combo box.
            text.displayFilter(searchModel.getTextFilter(textName));
            filterNameTextField.setText(textName);                                //TE: set the name of the filter in the filter name field.
        }
        else
            showMessage(CBIntText.get("Problem loading; there are no filters selected."), CBIntText.get("Nothing to Load"));

        load(loadCombo.getSelectedItem().toString());
    }

    /**
     * Gets the values of the base dn, search level, return attributes and alias state from the
     * property file that pertain to the filter being loaded and sets these values in
     * the GUI.
     *
     * @param name the name of the filter that is being loaded.
     */
    protected void load(String name)
    {
        //TE: load search level...
        int searchLevel = 2;
        try
        {
            searchLevel = Integer.parseInt(searchModel.getValue(name + "." + SearchModel.SEARCHLEVEL));
            searchLevelCombo.setSelectedIndex(searchLevel);
        }
        catch (NumberFormatException e)
        {
            searchLevelCombo.setSelectedIndex(2);
        }


        //TE: load base dn...
        String dn = searchModel.getValue(name + "." + SearchModel.BASEDN);
        if (dn != null)
            baseDNTextField.setText(dn);

        //TE: get the name of the return attribute list.  Check that the list name exists then
        //		set it in the combo...
        String retAttrs = searchModel.getValue(name + "." + SearchModel.RETATTRS);
        if (retAttrs != null)
        {
            Object temp[] = ReturnAttributesDialog.getSavedListNames();

            for (int i = 0; i < temp.length; i++)
            {
                if (((String) temp[i]).equalsIgnoreCase(retAttrs))
                {
                    returnAttributesCombo.setSelectedItem(temp[i]);
                    break;
                }
            }
        }

        //TE: select finding alias checkboxes...
        String find = searchModel.getValue(name + "." + SearchModel.FIND);
        if (find != null)
        {
            if (find.equalsIgnoreCase("true"))
                aliasFindingCheckBox.setSelected(true);
            else
                aliasFindingCheckBox.setSelected(false);
        }

        //TE: select searching alias checkboxes...
        String search = searchModel.getValue(name + "." + SearchModel.SEARCH);
        if (search != null)
        {
            if (search.equalsIgnoreCase("true"))
                aliasSearchCheckBox.setSelected(true);
            else
                aliasSearchCheckBox.setSelected(false);
        }
    }

    /**
     * Does a check to see if the filter is valid i.e. it check if there is a value in
     * each of the combo boxes.
     *
     * @return false if one or more of the combo boxes does not contain a value, true otherwise.
     */
    protected boolean isFilterValid()
    {
        if (tabbedPane.getSelectedIndex() == 0)
            return build.isFilterValid();
        else if (tabbedPane.getSelectedIndex() == 1)
            return join.isFilterValid();
        else if (tabbedPane.getSelectedIndex() == 2)
            return text.isFilterValid();

        return false;    //TE: something weird with the tabbed panes.
    }

    /**
     * Returns the LDAP filter of the currently displayed tabbed pane.  This assumes that the build tab is at
     * position 0, the join tab is at position 1 and the text tab is at position 2.
     *
     * @return the LDAP filter for example (cn=f*)
     */
    protected String getLDAPFilter()
    {
        if (tabbedPane.getSelectedIndex() == 0)
            return getLDAPFilter(FilterType.UI);
        else if (tabbedPane.getSelectedIndex() == 1)
            return getLDAPFilter(FilterType.JOIN);
        else if (tabbedPane.getSelectedIndex() == 2)
            return getLDAPFilter(FilterType.RAW);

        return "";
    }

    protected String getLDAPFilter(FilterType type)
    {
        if (type == FilterType.UI)
            return build.getFilter();
        else if (type == FilterType.JOIN)
            return searchModel.getJoinFilter(join.getFilter());
        else if (type == FilterType.RAW)
            return text.getFilter();

        return "";
    }

    /**
     * Displays a information message dialog with the text that is supplied and titles the dialog with the title supplied.
     *
     * @param message the text to be displayed.
     * @param title   the title of this information message dialog.
     */
    protected void showMessage(String message, String title)
    {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Sets the search Alias Behaviour in the property file, gets the filter and executes the search & display
     * before closing the window.
     */
    public void doOK()
    {
        if (isFilterValid())
        {
            // Check if the filter is safe to use (we don't care about the name to begin with b/c we aren't saving)...
            if (tabbedPane.getSelectedIndex() == 1 && recursiveFilterCheck(null, join.getFilter(), "Search"))
                return;
            setAliasOptions();

            String returnAttrs = (returnAttributesCombo.getSelectedItem()).toString();
            if (!returnAttrs.equalsIgnoreCase(ReturnAttributesDialog.DEFAULT_RETURN_ATTRS))
            {
                closeSearchGUI();
                String[] attrNames = ReturnAttributesDialog.getReturnAttributes(returnAttrs);
                searchModel.openRetAttrDisplay(browser, attrNames, (browser.getSearchTree()).getDataSource());
                SearchExecute.run(browser.getSearchTree(), new DN(baseDNTextField.getText()), getLDAPFilter(), attrNames, searchLevelCombo.getSelectedIndex(), browser.getSearchBroker());    //TE: the search details.
            }
            else
            {
                closeSearchGUI();
                SearchExecute.run(browser.getSearchTree(), new DN(baseDNTextField.getText()), getLDAPFilter(),
                        new String[]{"objectClass"}, searchLevelCombo.getSelectedIndex(), browser.getSearchBroker());    //TE: the search details.			SearchExecute.run(jx.getSearchTree(), new DN(baseDNTextField.getText()), getLDAPFilter(), attrNames, searchLevelCombo.getSelectedIndex(), jx.getSearchBroker());	//TE: the search details.
            }
            browser.getTreeTabPane().setSelectedComponent(browser.getResultsPanel());
        }
        else
        {
            showMessage(CBIntText.get("The filter cannot be constructed; there appears to be missing information.\nPlease make sure you have entered all the information for the filter correctly."), CBIntText.get("Missing Information"));
        }
    }

    /**
     * Over writes the parent method to call the closeSearchGUI method.
     */
    public void doCancel()
    {
        closeSearchGUI();
    }

    /**
     * Sets the filter name text to 'Untitled' then
     * sets the visibility of the dialog to false.
     */
    public void closeSearchGUI()
    {
        filterNameTextField.setText("Untitled");        //TE: bug 4896.
        setVisible(false);
    }

    /**
     * Determines which search alias behaviour the user has selected in the search GUI.
     * The combinations are:
     * <p>
     * <b>always</b> - both alias check boxes are checked.<br>
     * <b>never</b> - neither alias check boxes are checked.<br>
     * <b>finding</b> - only the 'Resolve aliases when finding base object' check box is checked.<br>
     * <b>searching</b> - only the 'Resolve aliases while searching' check box is checked.
     * </p>
     * The search alias option is set in JXplorer's property file under 'option.ldap.searchAliasBehaviour'.
     */
    public void setAliasOptions()
    {
        String aliasOption = "always";

        if (!aliasSearchCheckBox.isSelected() && !aliasFindingCheckBox.isSelected())
            aliasOption = "never";
        else if (aliasSearchCheckBox.isSelected() && !aliasFindingCheckBox.isSelected())
            aliasOption = "searching";
        else if (!aliasSearchCheckBox.isSelected() && aliasFindingCheckBox.isSelected())
            aliasOption = "finding";

        log.fine("Setting search alias option to: [" + aliasOption + "]");
        JXConfig.setProperty("option.ldap.searchAliasBehaviour", aliasOption);
    }

    /**
     * Checks if the user is trying to save a copy of the filter into itself, for example
     * if a filter named t3 contains the filter t3 as part of a join.  This check applies only
     * for the join tab.  Constructs an info dialog if the filter is recursive.
     *
     * @param filterName the user specified name for the filter that is currently being saved.
     * @param filter     the value of the filter for example 'JXFilter.t1JXFilter.t1'.
     * @param type       the source of the filter being constructed for example: view, save or search (used in the info dialog).
     * @return true if the filter contains itself, false otherwise.
     */
    protected boolean recursiveFilterCheck(String filterName, String filter, String type)
    {
        if (recursiveFilterCheck(filterName, filter))    //TE: stop the user from constructing a filter that is recursive.
        {
            showMessage(CBIntText.get("The filter you are trying to {0} is not valid.  You may be trying to construct a filter within itself which will cause an infinite loop.", new String[]{type.toLowerCase()}), CBIntText.get(type) + " " + CBIntText.get("Error"));
            return true;
        }
        return false;
    }

    /**
     * Checks if the user is trying to save a copy of the filter into itself, for example
     * if a filter named t3 contains the filter t3 as part of a join.  This check applies only
     * for the join tab.
     *
     * @param filterName the user specified name for the filter that is currently being saved.
     * @param filter     the value of the filter for example 'JXFilter.t1JXFilter.t1'.
     * @return true if the filter contains itself, false otherwise.
     */
    protected boolean recursiveFilterCheck(String filterName, String filter)
    {
        boolean recursive = false;
        String temp;
        ArrayList list = searchModel.getJoinFilterNames(filter);

        for (int i = 0; i < list.size(); i++)
        {
            temp = list.get(i).toString();

            if (filterName != null && filterName.compareTo(temp) == 0)
            {
                recursive = true;
                break;
            }

            // Go down each level that check there is the same name in the filter to be saved and the children fiters...
            String value = searchModel.getFilter(temp);

            // check if it is joint filter or not...
            if (value != null && value.indexOf("JXFilter") != -1)
                recursive = recursiveFilterCheck(filterName, value);

            if (recursive)
				return true;
		}
					
		return recursive;     
    }
}


//TE:  ONE DAY..........

//	class TreeFilterPanel extends CBPanel
//	{
//		public TreeFilterPanel()	
//		{	
//			setBackground(new Color(235,255,255));
//		}		
//	}


	
