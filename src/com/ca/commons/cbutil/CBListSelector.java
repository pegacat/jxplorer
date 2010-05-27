package com.ca.commons.cbutil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class acts as a item selector.  It sets up a dialog that lets you select items
 * from one list and add them to another list.  It also allows you to remove selected
 * items from the list.
 *
 * @author Trudi.
 */

public class CBListSelector extends CBDialog
{
    JList availableList, selectedList;
    JScrollPane availableScrollPane, selectedScrollPane;
    CBButton btnAdd, btnRemove;
    JLabel availableLabel, selectedLabel;
    CBPanel leftPanel, middlePanel, rightPanel;
    ArrayList arrayList = new ArrayList();

    private static Logger log = Logger.getLogger(CBListSelector.class.getName());

    /**
     * Static method that should be used rather than creating an object directly if
     * you wish to get the user input after the user has finished making selections and
     * has closed the window.
     * <p/>
     * This method creates a CBListSelector object and calls its 'show' method.  When the
     * user is finished with the dialog this method returns the user's selections.
     *
     * @param owner  the parent frame (usally JXplorer).
     * @param list   the strings that are to appear in the list on the left of the dialog.
     * @param title  the words that will appear in the title bar of the dialog.
     * @param helpID the help ID to attach to the Help button.
     * @return a list of user selections.
     */

    public static ArrayList getSelectedValues(Frame owner, String[] list, String title, String helpID)
    {
        CBListSelector listSelector = new CBListSelector(owner, list, title, helpID);

        listSelector.setVisible(true);

        return listSelector.getSelectedValues();
    }


    /**
     * Sets up a dialog with two lists.  The list on the left displays all available values
     * that the user can select from.  When the user selects a value it is displayed in the
     * list on the right either by double clicking on the value or clicking the '>>' button.
     * The user can also remove a value from the selection list either by double clicking on
     * it or clicking the '<<' button.
     *
     * @param owner  the parent frame (usally JXplorer).
     * @param list   the strings that are to appear in the list on the left of the dialog.
     * @param title  the words that will appear in the title bar of the dialog.
     * @param helpID the help ID to attach to the Help button.
     */

    public CBListSelector(Frame owner, String[] list, String title, String helpID)
    {
        super(owner, CBIntText.get(title), helpID);

        leftPanel = new CBPanel();
        middlePanel = new CBPanel();
        rightPanel = new CBPanel();

        //TE: left panel...

        leftPanel.addln(availableLabel = new JLabel(CBIntText.get("Available Attributes")+":"));
        leftPanel.makeHeavy();

        availableList = new JList(list);
        availableList.setSelectionMode(0);		//TE: only one item can be selected at anyone time.
        availableList.setSelectionModel(new CBSingleSelectionModel(availableList));   //TE: tries to ensure that the selected item is visible.

        leftPanel.addln(availableScrollPane = new JScrollPane(availableList));

        //TE: middle panel...

        btnAdd = new CBButton(CBIntText.get(">>"), CBIntText.get("Add an attribute from the attribute list on the left to the selection list on the right."));
        btnAdd.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                add();
            }
        });

        btnRemove = new CBButton(CBIntText.get("<<"), CBIntText.get("Remove an attribute from the selection list on the right."));
        btnRemove.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                remove();
            }
        });

        middlePanel.makeHeavy();
        middlePanel.addln(new JLabel("  "));	//TE: top padding.
        middlePanel.makeLight();
        middlePanel.addln(btnAdd);
        middlePanel.addln(btnRemove);
        middlePanel.makeHeavy();
        middlePanel.addln(new JLabel("  "));	//TE: bottom padding.

        //TE: right panel...

        rightPanel.addln(selectedLabel = new JLabel(CBIntText.get("Selected Attributes")+":"));
        rightPanel.makeHeavy();

        selectedList = new JList();
        selectedList.setSelectionMode(0);		//TE: only one item can be selected at anyone time.
        selectedList.setSelectionModel(new CBSingleSelectionModel(selectedList));   //TE: tries to ensure that the selected item is visible.

        rightPanel.addln(selectedScrollPane = new JScrollPane(selectedList));

        //TE: main panel...

        display.addln(new JLabel("  "));		//TE: padding.
        display.makeHeavy();
        display.add(leftPanel);
        display.makeLight();
        display.add(middlePanel);
        display.makeHeavy();
        display.add(rightPanel);

        setSize(400, 300);
        CBUtility.center(this, owner);

        registerMouseListeners();
    }


    /**
     * Adds a double click mouse listener to both lists in this dialog.
     * If the double click occurs in the list on the left, the 'add' method is
     * called.  If the double click occurs in the list on the right, the
     * remove method is called.
     */

    protected void registerMouseListeners()
    {
        availableList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2)
                    add();
            }
        });

        selectedList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2)
                    remove();
            }
        });
    }


    /**
     * Gets the selected item from the list on the left and adds it to
     * the list on the right.  It uses a global array list to keep track
     * of the selections in the right hand list.
     */

    public void add()
    {
        try
        {
            if (!arrayList.contains(availableList.getSelectedValue()))
                arrayList.add(availableList.getSelectedValue());

            selectedList.setListData(arrayList.toArray());
        }
        catch (Exception e)
        {
            log.log(Level.FINER, "No selection to add.", e);
        }
    }


    /**
     * Removes the selected item from the list on the left.  It uses a
     * global array list to keep track of the selections in the right hand list.
     */

    public void remove()
    {
        try
        {
            arrayList.remove(selectedList.getSelectedIndex());
            selectedList.setListData(arrayList.toArray());
        }
        catch (Exception e)
        {
            log.log(Level.FINER, "No selection to remove.", e);
        }
    }


    /**
     * Returns a list of the values that the user has selected.
     *
     * @return a list of the values that the user has selected.
     */

    public ArrayList getSelectedValues()
    {
        if (arrayList.isEmpty())
            return null;
        else
            return arrayList;
    }
}