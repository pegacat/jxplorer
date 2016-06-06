package com.ca.directory.jxplorer;

import java.awt.*;
import java.util.*;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.*;

import com.ca.commons.naming.*;
import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.search.SearchModel;

/**
 * This class and it's inner classes handle adding, editing and deleting of bookmarks.
 * All the common operations are in this class whereas all the GUI setup and specific
 * operations are in the inner classes: AddDialog, EditDialog and DeleteDialog.
 * It uses a property file called 'bookmarks.txt' to store the bookmarks in the
 * following format:
 * <p>
 *  dn.name=something e.g: dn.DEMOCORP=o=DEMOCORP,c=AU<br>
 *  desc.name=something e.g: desc.DEMOCORP=A sample directory.
 * <p>
 * The name part (e.g: DEMOCORP) is used as the title of the bookmark in the combo box.
 * @author Trudi.
 */
public class BookMarks
{
    /**
     *  The parent frame that the dialogs should centre on.
     */
	JXplorerBrowser browser = null;

    /**
     * The bookmark property file used for reading, writing and deleting
     * bookmarks from and to.
     */
	static Properties propertyList = null; // try to share bookmarks across all windows // TODO: tighten this up...

    /**
     * The name of the property file.
     */
    final String FILE_NAME = "bookmarks.txt";

    /*
     *   The path to the property file.
     */

    String bookmarkPath;

    /**
     * The dialog used for deleting a bookmark.
     */
    DeleteDialog deleteDialog = null;

    /**
     * The dialog used for editing a bookmark.
     */
    EditDialog editDialog = null;

    /**
     * The dialog used for adding a bookmark.
     */
    AddDialog addDialog = null;

    private static Logger log = Logger.getLogger(BookMarks.class.getName());

    /**
    * Sets up the property list.
	* @param jx jxplorer (parent component).
	*/
	public BookMarks(JXplorerBrowser jx)
	{
		browser = jx;
        bookmarkPath = CBUtility.getPropertyConfigPath(JXplorer.APPLICATION_NAME, FILE_NAME);
		propertyList = CBUtility.readPropertyFile(bookmarkPath);
	}

    /**
     * Checks if the Name contains only spaces.  A Name is considered invalid if this is the
     * case therefore this method will return false.  Otherwise true is returned.  This method
     * can be expanded to do other checking in the future.
     * @param name the name of the bookmark that we want to validate.
     * @return true if the Name doesn't contains only spaces, false otherwise.
     */
	protected boolean isValidName(String name)
	{
		try
		{
			if(name.trim().length()<=0)
				return false;
		}
		catch(Exception e)
		{
			log.warning("Name '" + name + "' not specified in Bookmarks");
			return false;
		}

		return true;
	}

    /**
     * Checks if the DN equals 'cn=no entries' or contains only spaces.
     * A DN is considered invalid if this is the
     * case therefore this method will return false.
     * Otherwise true is returned.  This method
     * can be expanded to do other checking in the future.
     * @param dn the DN that we want to validate.
     * @return true if the DN doesn't equal 'cn=no entries'
     * or doesn't contains only spaces, false otherwise.
     */
	protected boolean isValidDN(String dn)
	{
		if(dn.equalsIgnoreCase("cn=no entries"))
			return false;
		else if(dn.trim().length()<=0)
			return false;
		else
			return true;
	}

    /**
     * Takes a DN and returns it's current lowest RDN.
     * @param dn the DN which we will get the RDN from.
     * @return the RDN of the DN.
     */
	protected String getCurrentRDN(String dn)
	{
		DN bloop = new DN(dn);
	    return bloop.getLowestRDN().toString();
	}

    /**
     * Returns true or false depending on if a bookmark exists
     * in the property file 'bookmarks.txt'.
     * @param name the name of the book mark which is part of
     * the key (i.e. dn.name).
     * @return true if the bookmark exists, false if not.
     */
	protected boolean checkIfBookmarkExists(String name)
	{
		propertyList = CBUtility.readPropertyFile(bookmarkPath);
		return propertyList.containsKey("dn."+name);
	}

    /**
     * Creates a new DeleteDialog object if deleteDialog is
     * null.
     * @return the DeleteDialog object.
     */
    public DeleteDialog getDeleteDialog()
    {
        if(deleteDialog == null)
            return new DeleteDialog();
        return deleteDialog;
    }

    /**
     * Creates a new EditDialog object if editDialog is
     * null.
     * @return the EditDialog object.
     */
    public EditDialog getEditDialog()
    {
        if(editDialog == null)
            return new EditDialog();
        return editDialog;
    }

    /**
     * Creates a new AddDialog object if editDialog is
     * null.<br>
     * <b>NOTE:</b> see 'name' and 'edit'.
     * @param name if the user wants to edit an existing bookmark,
     * 'edit' should be true and 'name' should be the name of that existing
     * bookmark.  If the user just wants to add a new bookmark, then 'name'
     * should be the DN of that bookmark and 'edit' should be false.
     * @param edit true if the user wants to edit an existing
     * bookmark, false if the user wants to add a new bookmark.
     * @return the AddDialog object.
     */
    public AddDialog getAddDialog(String name, boolean edit)
    {
        if(addDialog == null)
            addDialog = new AddDialog(name, edit);

        if(edit == true)
            addDialog.setHelpLink(HelpIDs.BOOKMARK_EDIT);
        else
            addDialog.setHelpLink(HelpIDs.BOOKMARK_ADD);

        addDialog.Help.setToolTipText(CBIntText.get("Click here for Help."));

        return addDialog;
    }

    /**
     * Deletes a bookmark from the property file.
     * Updates the bookmark menu also.
     * @param name the name of the bookmark to delete.
     */
    public void deleteBookmark(String name)
    {
        propertyList.remove("dn."+name);
        propertyList.remove("desc."+name);

        CBUtility.writePropertyFile(bookmarkPath, propertyList, null);

        updateAllBookmarks();
    }

     public void updateAllBookmarks()
        {
            // Updates the Bookmark menu items...
            ArrayList<JXplorerBrowser> browsers = browser.getAllBrowsers();
            for (JXplorerBrowser b:browsers)
            {
                b.getMainMenu().updateBookmarkMenu();  // remembering that propertyList is global...
            }

        }

    /**
     * Gets the names of all the saved bookmarks
     * from the property file.
     * @return a list of saved bookmark names sorted alphabetically.
     */
    public Object[] getSavedBookmarkNames()
    {
        Enumeration keys = propertyList.keys();
        ArrayList list = new ArrayList();
        while (keys.hasMoreElements())
        {
            String key = keys.nextElement().toString();

            if(key.toLowerCase().startsWith("dn"))
            {
                String name = key.substring(key.indexOf(".")+1);
                list.add(name);
            }
        }

        Object listOb[] = list.toArray();

        // Sort the list alphabetically...
        Arrays.sort(listOb, new SearchModel.StringComparator());

        return listOb;
    }

    /**
     * Makes a new CBJComboBox with the values supplied.
     * @param values a list of values to go into the combo box.
     * @return a new CBJComboBox object populated with the values supplied.
     */
    public CBJComboBox makeComboBox(Object[] values)
    {
        CBJComboBox combo = new CBJComboBox(values);
        combo.setRenderer(new CBBasicComboBoxRenderer(values));
        combo.setPreferredSize(new Dimension(140, 20));
        return combo;
    }

    /**
     * Makes a new dialog that allows the user to add or edit a bookmark.
     * @author Trudi.
     */
    public class AddDialog extends CBDialog
    {
        /**
         * The text field for the name of the bookmark.
         */
        JTextField nameField = new JTextField();

        /**
         * The text field for the DN of the bookmark.
         */
        JTextField dnField = new JTextField();

        /**
         * The text field for the description of the bookmark.
         */
        JTextField descField = new JTextField();

        /**
         * The name of the bookmark that is being edited.  This is used
         * as a store in the case of a user renaming the edited bookmark.
         */
        String editName = null;

        /**
         * A flag that represents the bookmark is being edited rather than
         * added.
         */
        boolean edit = false;

        /**
         * Makes a new dialog that allows the user to add or edit a bookmark.
         * <b>NOTE:</b> see 'name' and 'edit'.
         * @param name if the user wants to edit an existing bookmark,
         * 'edit' should be true and 'name' should be the name of that existing
         * bookmark.  If the user just wants to add a new bookmark, then 'name'
         * should be the DN of that bookmark and 'edit' should be false.
         * @param edit true if the user wants to edit an existing
         * bookmark, false if the user wants to add a new bookmark.
         */
        public AddDialog(String name, boolean edit)
        {
            super(browser, CBIntText.get("Add Bookmark"), null);

            this.edit = edit;

            if(edit)
            {
                // Should be the name of a previously saved bookmark...
                displayBookmarkDetails(name);
                editName = name;
                setTitle(CBIntText.get("Edit Bookmark"));
            }
            else
            {
                // Should be the DN of the entry that the bookmark will represent...
                displayNewBookmarkDetails(name);
            }

            CBPanel namePanel = new CBPanel();
            namePanel.add(new JLabel(CBIntText.get("Bookmark Name") + ": "));
            namePanel.makeWide();
            namePanel.add(nameField);

            namePanel.makeLight();

            OK.setToolTipText(CBIntText.get("Click here to exit when finished."));
            OK.setText(CBIntText.get("Save"));
            Cancel.setToolTipText(CBIntText.get("Click here to exit."));
            // NOTE: the Help button tooltip is set elsewhere (b/c it will be null here).

            CBPanel detailsPanel = new CBPanel();
            detailsPanel.add(new JLabel(CBIntText.get("DN") + ": "));
            detailsPanel.makeWide();
            detailsPanel.addln(dnField);

            detailsPanel.makeLight();

            detailsPanel.add(new JLabel(CBIntText.get("Description") + ": "));
            detailsPanel.makeWide();

            detailsPanel.addln(descField);
            detailsPanel.setBorder(new TitledBorder(CBIntText.get("Bookmark Properties")));

            display.makeWide();
            display.addln(namePanel);
            display.addln(detailsPanel);

            setSize(480, 200);
            CBUtility.center(this, browser);
        }

        public JButton getHelpButton()
        {
            return Help;
        }

       /**
        * Reads the details of the bookmark from the property file then
        * displays the details such as it's DN and it's description
        * in their appropriate text fields.
        * @param name the name of the bookmark.
        */
        protected void displayBookmarkDetails(String name)
        {
            try
            {
                nameField.setText(name);
                dnField.setText(propertyList.getProperty("dn."+name));
                descField.setText(propertyList.getProperty("desc."+name));
            }
            catch(Exception e)
            {
                CBUtility.error("Error loading '" + name + "' bookmark.  The bookmark cannot be found.", e);
            }
        }

        /**
         * Set the text fields in this dialog.<br><br>
         * The name text field is set with the RDN of the DN supplied.<br>
         * The dn text field is set with the DN supplied.<br>
         * The description text field is set to blank.<br>
         * @param dn the dn of the bookmark.
         */
        public void displayNewBookmarkDetails(String dn)
        {
            nameField.setText(getCurrentRDN(dn));
            dnField.setText(dn);
            descField.setText("");
        }

        /**
         * Saves the bookmark to the property file after doing some
         * basic checks.<br>
         * The checks are if the name is valid, if the bookmark exists and
         * if the dn is valid.<br>
         * If the user is editing a bookmark and they change the name
         * this method asks them if they want to delete the old bookmark.<br>
         * The bookmark menu is updated also.
         */
        public void doOK()
        {
            String name = nameField.getText();
            String desc = descField.getText();
            String dn = dnField.getText();

			try
			{
                // Check if it only contains spaces...
				if (!isValidName(name))
				{
					CBUtility.error(CBIntText.get("The bookmark you are trying to save " +
                            "contains an invalid Name.  Please check the Name then try again."));
					return;
				}

                if(checkIfBookmarkExists(name))
                {
                    int response = JOptionPane.showConfirmDialog(this,
                            CBIntText.get("Do you want to replace it?"),
                            CBIntText.get("Bookmark Exists"), JOptionPane.OK_CANCEL_OPTION);

                    if (response != JOptionPane.OK_OPTION)
                        return;
                }

                // Check if the bookmark being added equals 'cn=no entries' or if it
                // only contains spaces...
				if (!isValidDN(dn))
				{
					CBUtility.error(CBIntText.get("The bookmark you are trying to save " +
                            "contains an invalid DN.  Please check the DN then try again."));
					return;
				}

                if(edit)
                {
                    if(!name.equals(editName))
                    {
                        int response = JOptionPane.showConfirmDialog(this,
                                CBIntText.get("You have renamed ''{0}'' to ''{1}''.  Do you want to delete ''{0}''?",
                                        new String[] {editName,name}),
                                CBIntText.get("Delete Bookmark?"), JOptionPane.YES_NO_OPTION);

                        if (response == JOptionPane.YES_OPTION)
                            deleteBookmark(editName);
                    }
                }

				propertyList.setProperty("dn."  +name, dn);
				propertyList.setProperty("desc."+name, desc);

                CBUtility.writePropertyFile(bookmarkPath, propertyList, null);
			}
			catch(Exception e)
			{
				CBUtility.error("Cannot add an empty bookmark.");
                return;
			}

            // Updates the Bookmark menu items...
            //browser.getMainMenu().updateBookmarkMenu();
            updateAllBookmarks();

            JOptionPane.showMessageDialog(browser,
                    CBIntText.get("The bookmark ''{0}'' was successfully saved.",
                            new String[] {name}), CBIntText.get("Save Succeeded"),
                    JOptionPane.INFORMATION_MESSAGE );

            super.doOK();
        }



        /**
         * When the user hits 'cancel', the window shuts and the bookmark menu is updated.
         */
        public void doCancel()
        {
            super.doCancel();
            // Updates the Bookmark menu items...
            //browser.getMainMenu().updateBookmarkMenu();
            updateAllBookmarks();

        }
    }

    /**
     * Opens a dialog that allows a user to select a bookmark
     * to edit.  Once a bookmark is selected this dialog closes
     * and the AddDialog dialog opens.
     * @author Trudi.
     */
    public class EditDialog
    {
        /**
         * Opens a dialog that allows a user to select a bookmark
         * to edit.  Once a bookmark is selected this dialog closes
         * and the AddDialog dialog opens.
         */
        public EditDialog()
        {
            Object bookmarks[] = getSavedBookmarkNames();
            CBJComboBox combo = makeComboBox(bookmarks);
            combo.setToolTipText(CBIntText.get("Select the bookmark name that you want to edit."));

            int response = JOptionPane.showConfirmDialog(browser, combo,
                    CBIntText.get("Edit Bookmark"), JOptionPane.OK_CANCEL_OPTION);

            if (response != JOptionPane.OK_OPTION)
                return;

            if (combo.getSelectedItem() != null)
            {
                String bookmark = combo.getSelectedItem().toString();
                AddDialog ad = getAddDialog(bookmark, true);
                ad.setVisible(true);
            }
        }
    }

    /**
     * Opens a dialog that allows a user to select a bookmark
     * to delete.  Deletes that bookmark.
     * @author Trudi.
     */
    public class DeleteDialog
    {
        /**
         * Opens a dialog that allows a user to select a bookmark
         * to delete.  Deletes that bookmark.
         */
        public DeleteDialog()
        {
            Object bookmarks[] = getSavedBookmarkNames();
            CBJComboBox combo = makeComboBox(bookmarks);
            combo.setToolTipText(CBIntText.get("Select the bookmark name that you want to delete."));

            int response = JOptionPane.showConfirmDialog(browser, combo,
                    CBIntText.get("Delete Bookmark"), JOptionPane.OK_CANCEL_OPTION);

            if (response != JOptionPane.OK_OPTION)
                return;

            if (combo.getSelectedItem()!=null)
            {
                String toDelete = combo.getSelectedItem().toString();
                int res = JOptionPane.showConfirmDialog(browser,
                        CBIntText.get("Are you sure you want to delete the bookmark called ''{0}''?", new String[] {toDelete}), CBIntText.get("Confirm Delete"), JOptionPane.OK_CANCEL_OPTION);

                if (res != JOptionPane.OK_OPTION)
                    return;

                deleteBookmark(toDelete);

                JOptionPane.showMessageDialog(browser,
                        CBIntText.get("The bookmark ''{0}'' was successfully deleted.",
                                new String[] {toDelete}), CBIntText.get("Delete Succeeded"),
                        JOptionPane.INFORMATION_MESSAGE );
            }
        }
    }
}
