package com.ca.directory.jxplorer.viewer;

import com.ca.commons.cbutil.*;
import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.*;
import com.ca.directory.jxplorer.broker.DataBrokerQueryInterface;
import com.ca.directory.jxplorer.viewer.tableviewer.*;
import com.ca.directory.jxplorer.viewer.tableviewer.AttributeValue;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;
import java.util.logging.Level;
/**
 * This class displays attributes in a table (currently string attributes only).  The user can modify the table values,
 * and submit the results, which are passed to the registered DataBrokerQueryInterface (obtained from the registered DataBrokerQueryInterface)...
 */


/*    PROGRAMMING NOTE:
 *
 *    Some rather unpleasent stuff happens with object class changing.  The state
 *    of the unmodified entry is maintained between displayEntry() calls using
 *    the classChangedOriginalEntry variable.
 */
public class TableAttributeEditor extends JPanel
        implements DataSink, PluggableEditor //, TreeEntryCreator
{
    private static Logger log = Logger.getLogger(TableAttributeEditor.class.getName());

    JTable attributeTable;
    AttributeTableModel tableData;
    JScrollPane tableScroller;
    CBButton submit, reset, changeClass, opAttrs; //, help;
    JFrame owner;
    JDialog virtualEntryDialog = null;

    /**
     * Flag for a virtual entry.
     */
    boolean virtualEntry = false;

    /**
     * Copy of the current entry.
     */
    DXEntry currentEntry = null;

    /**
     * Copy of the original entry.
     */
	DXEntry originalEntry = null;

    /**
     * Copy of the current DN.
     */
    DN currentDN = null;

    /*
     * Experimental - tracks the DN of a page with unsaved changes, as part of a check when the user moves
     * away by accident...
     */

    DN entryWithPendingChanges = null;

    /**
     * The data source directory data is read from.
     */
    public DataBrokerQueryInterface dataSource;

    /**
     * A rare operation is for the user to change the classes of an entry.  This backs up the original state of that
     * entry.
     */
    DXEntry classChangedOriginalEntry = null;

    SmartPopupTableTool popupTableTool;

    ClassLoader myLoader;

    final AttributeValueCellEditor myEditor;

    public String title = CBIntText.get("Table Editor");

    /**
     * Constructor initialises the table and a popup tool, as well as initialising the required GUI elements. It adds
     * action listeners for the three main buttons, which include basic user input validation checking.
     */
    public TableAttributeEditor(JFrame MyOwner)
    {
        // As usual, it is insanely hard to get the swing components to display
        // and work properly.  If JTable is not displayed in a scroll pane no headers are
        // displayed, and you have to do it manually.  (If you *do* display it
        // in a scrollbar, in this instance, it screws up sizing)
        // The broken header mis-feature is only mentioned in the tutorial,
        // not in the api doco - go figure.

        super();

        owner = MyOwner;

        // final JPanel mainPanel = (JPanel)this;

        tableData = new AttributeTableModel();

        attributeTable = new JTable(tableData);
        //attributeTable.setRowHeight(20);	// This may be needed, depends on how fussy people get about the bottom of letters like 'y' getting cut off when the cell is selected - bug 3013.

        popupTableTool = new SmartPopupTableTool(attributeTable, tableData, (JXplorerBrowser) owner);

        // Set the renderer for the attribute type...
        final AttributeTypeCellRenderer typeRenderer = new AttributeTypeCellRenderer();

        attributeTable.setDefaultRenderer(AttributeNameAndType.class, typeRenderer);

        // Set the renderer for the attribute value...
        final AttributeValueCellRenderer valueRenderer = new AttributeValueCellRenderer();

        attributeTable.setDefaultRenderer(AttributeValue.class, valueRenderer);

        // Set the editor for the attribute value...
        myEditor = new AttributeValueCellEditor(owner);

        attributeTable.setDefaultEditor(AttributeValue.class, myEditor);

        attributeTable.getTableHeader().setReorderingAllowed(false);

        currentDN = null;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(submit = new CBButton(CBIntText.get("Submit"), CBIntText.get("Submit your changes to the Directory.")));
        buttonPanel.add(reset = new CBButton(CBIntText.get("Reset"), CBIntText.get("Reset this entry i.e. cancels any changes.")));
        buttonPanel.add(changeClass = new CBButton(CBIntText.get("Change Classes"), CBIntText.get("Change the Object Class of this entry.")));
        buttonPanel.add(opAttrs = new CBButton(CBIntText.get("Properties"), CBIntText.get("View the Operational Attributes of this entry.")));

        // I don't really understand why we have to do this...
        // but without it these buttons over ride the default
        // button (Search Bar's search button), if they have
        // been clicked and the user hits the enter key?
        opAttrs.setDefaultCapable(false);
        submit.setDefaultCapable(false);
        reset.setDefaultCapable(false);
        changeClass.setDefaultCapable(false);

        setLayout(new BorderLayout(10, 10));

        tableScroller = new JScrollPane();
        attributeTable.setBackground(Color.white);
        tableScroller.setPreferredSize(new Dimension(300, 285));
        tableScroller.setViewportView(attributeTable);
        add(tableScroller, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        if ("true".equals(JXConfig.getProperty("lock.read.only")))
            title = CBIntText.get("Table Viewer");
        else
            title = CBIntText.get("Table Editor");


        setVisible(true);

        // triggers adding operational attributes of the current entry.
        opAttrs.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                displayOperationalAttributes();
            }
        });

        reset.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                myEditor.stopCellEditing();
                //tableData.reset();
                displayEntry(originalEntry, dataSource, false);

            }
        });

        submit.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                doSubmit();
            }
        });

        // This allows the user to change the objectclass attribute.
        // This is pretty tricky, because it changes what attributes are available.
        changeClass.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                changeClass();
            }
        });

        attributeTable.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                if (!doPopupStuff(e)) super.mousePressed(e);
            }

            public void mouseReleased(MouseEvent e)
            {
                if (!doPopupStuff(e)) super.mouseReleased(e);
            }

            //TODO need to have a way to call this from a keystroke...
            public boolean doPopupStuff(MouseEvent e)
            {
                if (e.isPopupTrigger() == false) return false;

                int row = attributeTable.rowAtPoint(new Point(e.getX(), e.getY()));

                attributeTable.clearSelection();
                attributeTable.addRowSelectionInterval(row, row);
                attributeTable.repaint();

                popupTableTool.registerCurrentRow((AttributeNameAndType) attributeTable.getValueAt(row, 0), (AttributeValue) attributeTable.getValueAt(row, 1), row, tableData.getRDN());         // active path also set by valueChanged
                popupTableTool.show(attributeTable, e.getX(), e.getY());
                popupTableTool.registerCellEditor(myEditor);    //TE: for bug fix 3107.
                return true;
            }
        });
    }

    /**
     * Opens the change class dialog.
     */
    public void changeClass()
    {	  //JPanel mainPanel
        /*
         *    MINOR MAGIC
         *
         *    This code reuses the 'new entry window'.  In order to make things
         *    sane, we prompt the user to save any serious changes before continuing.
         *    (Things can get really wierd if the user changes the name and then
         *    tries to change the objectclass - best to avoid the whole issue.)
         */
        myEditor.stopCellEditing();

        if (virtualEntry)
        {
            doVirtualEntryDisplay();
            return;
        }

        /*
         *    classChangedOriginalEntry saves the original state of the entry
         *    between visits to NewEntryWin.  (- I wonder if it would be neater
         *    to just reset the 'oldEntry' state of the table every time? ).
         *    Check it's not been set already (i.e. Pathological User is paying
         *    multiple visits to the NewEntryWin.)
         */
        if (classChangedOriginalEntry == null)
            classChangedOriginalEntry = tableData.getOldEntry();

        DXEntry newEntry = tableData.getNewEntry();
        DN newDN = newEntry.getDN();

        /*
         *    Pathalogical user has messed with the name, *and* wants to
         *    change the object classes...
         */
        if (newDN.equals(classChangedOriginalEntry.getDN()) == false)
        {
            checkForUnsavedChanges();
            /*
            if (promptForSave() == false)  // we may need to reset the 'newEntry' data
            {                                   // if the user discards their changes.

                tableData.reset();              // resets the table before going on.

                newEntry = tableData.getNewEntry();
                newDN = newEntry.getDN();
            }
            else // user has saved data - so now we need to reset the 'classChangedOriginalEntry'
            {
            */
                // to the changed (and hopefully saved!) data.
                // NB: If the directory write fails, then the change classes will also fail...
                classChangedOriginalEntry = tableData.getNewEntry();

        }

        /*
         *    Open NewEntryWin, allowing the user to reset the objectclass attribute.
         */

/*
        NewEntryWin userData = new NewEntryWin(newDN.parentDN(), newDN,
                                dataSource,
                                newEntry.getAsNonNullAttributes(),
                                newDN.getLowestRDN().toString(), TableAttributeEditor.this,
                                CBUtility.getParentFrame(mainPanel));
*/
        if (dataSource.getSchemaOps() == null)
        {
            JOptionPane.showMessageDialog(owner, CBIntText.get("Because there is no schema currently published by the\ndirectory, changing an entry's object class is unavailable."), CBIntText.get("No Schema"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        else
        {
            ChangeObjectClassWin userData = new ChangeObjectClassWin(dataSource, newDN, newEntry.getAsNonNullAttributes(), this, CBUtility.getParentFrame(this), false);
            userData.setSize(400, 250);
            CBUtility.center(userData, owner);    // TE: centres window.
            userData.setVisible(true);
        }
    }

    /**
     * Kicks off the entry modify/update & checks for manditory attributes.
     */
    public void doSubmit()
    {
        if (dataSource == null)
        {
            CBUtility.error("No dataSource available to write changes to in Table Attribute Editor");
            return;
        }

        myEditor.stopCellEditing();

        // If schema checking is on, make sure that all mandatory attributes are filled in.
        if ("false".equalsIgnoreCase(JXConfig.getProperty("option.ignoreSchemaOnSubmission"))
                && (tableData.checkMandatoryAttributesSet() == false))
        {
            CBUtility.error(TableAttributeEditor.this, CBIntText.get("All Mandatory Attributes must have values!"), null);
            return;
        }

        writeTableData();
    }

    private boolean showingOperationalAttributes = false;
    /**
     * Opens a dialog that displays the operational attributes of the current entry.
     */
    public void displayOperationalAttributes()
    {
        JXplorerBrowser jx = null;

        if (owner instanceof JXplorerBrowser)
            jx = (JXplorerBrowser) owner;
        else
            return;

        showingOperationalAttributes = !showingOperationalAttributes;

        // EJP 17 August 2010.
        // CB 14 August 2012 - some directories (looking at you Active Directory) don't support the '+' operator... so do it manually as well...
        String[] opAttrs = {"+","createTimeStamp", "creatorsName", "entryFlags", "federationBoundary", "localEntryID", "modifiersName", "modifyTimeStamp", "structuralObjectClass", "subordinateCount", "subschemaSubentry"};
        DXEntry entry = null;

        if (showingOperationalAttributes)
        {
            try
            {
                entry = (jx.getSearchBroker()).unthreadedReadEntry(currentDN, opAttrs);
                StringBuffer buffy = new StringBuffer("DN: " + currentDN.toString() + "\n\n");

                // Get the attribute values...
                // EJP 17 August 2010: use the actual attributes returned.
                NamingEnumeration ne = null;

                try
                {
                    ne = entry.getAll();
                    while (ne.hasMore())
                    {
                        DXAttribute att = (DXAttribute) ne.next();
                        buffy.append(att.getName() + ": " + att.get().toString() + "\n");

                        tableData.insertOperationalAttribute(att);
                    }
                } finally
                {
                    if (ne != null)
                        ne.close();
                }

                tableData.fireTableDataChanged();
            } catch (NamingException e)
            {
                CBUtility.error(TableAttributeEditor.this, CBIntText.get("Unable to read entry " + currentDN), e);
            }
        }
        else
        {
            tableData.removeOperationalAttributes();
            tableData.fireTableDataChanged();

        }
    }


    /**
     * Opens a dialog that asks the user if they want to make a virtual entry a non virtual entry.  If the user clicks
     * 'Yes' the 'change class' dialog opens.
     */
    public void doVirtualEntryDisplay()
    {
        virtualEntryDialog = new JDialog(owner, CBIntText.get("Virtual Entry"), true);

        CBButton btnYes = new CBButton(CBIntText.get("Yes"), CBIntText.get("Click yes to make a Virtual Entry."));
        CBButton btnNo = new CBButton(CBIntText.get("No"), CBIntText.get("Click no to cancel without making a Virtual Entry."));

        //TE: layout stuff...
        Container pane = virtualEntryDialog.getContentPane();
        pane.setLayout(new BorderLayout());
        CBPanel panel1 = new CBPanel();
        CBPanel panel2 = new CBPanel();
        CBPanel panel3 = new CBPanel();

        panel1.add(new JLabel(CBIntText.get("This entry is a Virtual Entry.  Are you sure you want to give this entry an object class?")));
        panel2.add(btnYes);
        panel2.add(btnNo);

        panel3.makeWide();
        panel3.addln(panel1);
        panel3.addln(panel2);

        pane.add(panel3);

        btnYes.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                processVirtualEntry();
            }
        });

        btnNo.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                shutVirtualEntryDialog();
            }
        });
        virtualEntryDialog.setSize(475, 125);
        CBUtility.center(virtualEntryDialog, owner);
        virtualEntryDialog.setVisible(true);
    }

    /**
     * Normally called by the 'Yes' button listener of the virtual entry dialog. This method opens the New Entry dialog
     * in simple mode (or Change Classes dialog). If the user selects one or more object classes they are added to the
     * entry and displayed in the table editor.
     */
    public void processVirtualEntry()
    {

        ChangeObjectClassWin userData = null;
        if (dataSource.getSchemaOps() == null)
        {
            JOptionPane.showMessageDialog(owner, CBIntText.get("Because there is no schema currently published by the\ndirectory, changing an entry's object class is unavailable."), CBIntText.get("No Schema"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        else
        {
            shutVirtualEntryDialog();				//TE: kill the prompt window.
            userData = new ChangeObjectClassWin(dataSource, currentEntry.getDN(), null, this, owner, true);
            userData.setSize(400, 250);
            CBUtility.center(userData, owner);    	//TE: centres window.
            userData.setVisible(true);

            while (userData.isVisible())			//TE: don't do anything until the New Entry window is closed.
            {
                try
                {
                    wait();
                }
                catch (Exception e)
                {
                    userData.dispose();
                }
            }
        }

        if (userData.newObjectClasses != null)		//TE: if the user has selected one or more object classes - add them to the entry in the directory.
        {
            try
            {
                DXOps dxOps = new DXOps(dataSource.getLdapContext());
                dxOps.addAttribute(currentEntry.getDN(), userData.newObjectClasses);
                dataSource.getEntry(currentEntry.getDN());			//TE: hack??  forces the entry to be read again - otherwise we don't display the naming value.
            }
            catch (NamingException e)
            {
                CBUtility.error(TableAttributeEditor.this, CBIntText.get("Unable to add new object classes to {0}.", new String[]{currentEntry.getDN().toString()}), e);
            }
        }
    }

    /**
     * Disposes of the virtual entry dialog that is opened as a prompt when the user may want to edit a virtual entry.
     */
    public void shutVirtualEntryDialog()
    {
        if (virtualEntryDialog != null)
        {
            virtualEntryDialog.setVisible(false);
            virtualEntryDialog.dispose();
        }
    }

    //
//    DN checkedDN;

    /**
     * <p>Displays data that can be modified by the user in a table.</p>
     *
     * Refactor: this is a complex method with many side effects, including checks for
     * previously unsaved changes...
     * @param entry the entry to be displayed by all the editors
     * @param ds the datasource the editors may use for more info
     */
     public void displayEntry(DXEntry entry, DataBrokerQueryInterface ds)
     {
		displayEntry(entry, ds, true);
	}

    private void displayEntry(DXEntry entry, DataBrokerQueryInterface ds, boolean storeOriginalEntry)
    {
        myEditor.stopCellEditing();


//        checkedDN = null; // hack - resets promptForSave.
               // Store original Entry for reset
        		if (entry != null && storeOriginalEntry && entry.getStatus() == DXEntry.NORMAL)
        			originalEntry = new DXEntry(entry);


        // Set the globals...
        currentEntry = entry;
        dataSource = ds;

        if (entry != null && entry.size() == 0)
        {
// If there is an entry and its size is zero - it's probably a virtual entry.
// We need to give the user the option of adding an object class to it i.e. so that
// it can be added to the directory as a real entry.
//
// Disable all the buttons except the 'Change Class' button - but rename this button
// to 'Add Class' so the user hopefully has a bit more of an idea about what is going on.

// Sets editor to a blank screen...
            tableData.clear();

// Disable all buttons except the 'Change Class' button - rename this one...
            submit.setEnabled(false);
            reset.setEnabled(false);
            changeClass.setText(CBIntText.get("Add Class"));
            changeClass.setEnabled(true);
            opAttrs.setEnabled(false);

            virtualEntry = true;

            return;
        }

        virtualEntry = false;

        // Isn't a virtual entry...
        if (entry != null)
            currentDN = entry.getDN();

        // May have been changed to 'Add Class'...
        changeClass.setText(CBIntText.get("Change Class"));

        // Some quick faffing around, to see if we're coming back from a
        // change classes operation.
        if (classChangedOriginalEntry != null)
        {
            // If they have the same name, then we're reviewing the same entry - otherwise we've moved on
            if (entry == null || entry.getDN().equals(classChangedOriginalEntry.getDN()) == false)
                classChangedOriginalEntry = null;
        }

        /*
         *    Check that we're not displaying a new entry, and leaving unsaved changes
         *    behind.
         *
         *    This turns out to be quite tricky, and involves a bunch 'o special cases.
         *
         *    First check whether the table data has changed (if not, do nothing)
         *    ->  if the new entry is null, prompt user to save
         *    ->  OR if the DN has changed, and it wasn't due to a rename, prompt user to save
         *
         */
        if (tableData.changedByUser())  // user made changes - were they saved?  (i.e., are we
        {                               // displaying the result of those changes?)
            boolean prompt = false;

            DXEntry oldEntry = tableData.getOldEntry();

            if (oldEntry != null)
            {
                /*
                 *    The code below is simply checking to see if the name of the
                 *    new entry is different from the old entry, and if it is,
                 *    whether that's due to the old entry being renamed.
                 */
                if (entry == null)
                {
                    prompt = true;
                }
                //TE: added the isEmpty check see bug: 3194.
                else if (!oldEntry.getDN().isEmpty() && entry.getDN().equals(oldEntry.getDN()) == false)
                {
                    DN oldParent = oldEntry.getDN().getParent();

                    DN newParent = entry.getDN().getParent();

                    if (oldParent.equals(newParent) == false)
                    {
                        prompt = true;
                    }
                    else
                    {
                        if (entry.getDN().getLowestRDN().equals(tableData.getRDN()) == false)
                        {
                            prompt = true;
                        }
                    }
                }

                if (prompt)    // yes, there is a risk of data loss - prompt the user.
                {
                    checkForUnsavedChanges();                // see if the user wants to save their data
                }
            }
        }

        myEditor.setDataSource(ds);    // Sets the DataBrokerQueryInterface in AttributeValueCellEditor used to get the syntax of attributes.

        // only enable buttons if DataBrokerQueryInterface
        // is valid *and* we can modify data...

        if (dataSource == null || entry == null || dataSource.isModifiable()==false)
        {
            setReadWrite(false, entry);
        }
        else
        {
            setReadWrite(true, entry);
        }

//        myEditor.stopCellEditing();

        if (entry != null)
        {
            entry.expandAllAttributes();
            currentDN = entry.getDN();

            tableData.insertAttributes(entry);
            popupTableTool.setDN(currentDN);    // Sets the DN in SmartPopupTableTool.
            myEditor.setDN(currentDN);          // Sets the DN in the attributeValueCellEditor which can be used to identify the entry that is being modified/
        }
        else
        {
            tableData.clear();					// Sets editor to a blank screen.
        }

        tableScroller.getVerticalScrollBar().setValue(0);   // Sets the scroll bar back to the top.
    }

    protected void setReadWrite(boolean writeable, DXEntry entry)
    {
        submit.setEnabled(writeable);
        reset.setEnabled(writeable);
        changeClass.setEnabled(writeable);
        opAttrs.setEnabled(writeable);
        myEditor.setEnabled(writeable);
        popupTableTool.setReadWrite(writeable);

        if (entry!=null && entry.get("objectclass") != null)  // only allow class changes if we can find
            changeClass.setEnabled(true);      // some to start with!
    }

    public JComponent getDisplayComponent()
    {
        validate();
        repaint();
        return this;
    }

    public String[] getAttributeValuesAsStringArray(Attribute a)
            throws NamingException
    {
        if (a == null) return new String[0];
        DXNamingEnumeration e = new DXNamingEnumeration(a.getAll());
        if (e == null) return new String[0];
        return e.toStringArray();
    }

    /**
     * Test whether the (unordered) object class lists of two attributes contain the same
     */
    public boolean objectClassesChanged(DXAttributes a, DXAttributes b)
    {
        boolean result = false;
        try
        {
            String[] A = getAttributeValuesAsStringArray(a.getAllObjectClasses());
            String[] B = getAttributeValuesAsStringArray(b.getAllObjectClasses());

            Object[] test = CBArray.difference(A, B);
            if (test.length > 0) result = true;
            test = CBArray.difference(B, A);
            if (test.length > 0) result = true;


            return result;
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "Error in TableAttributeEditor:objectClassesChanged ", e);
            return true;
        }
    }

    /**
     * Writes the data currently in the table editor to the directory.
     */
    public void writeTableData()
    {

        myEditor.stopCellEditing();

        if (dataSource == null) // if ds is null, data is not modifiable...
        {
            CBUtility.error("no datasource to write data to in writeTableData()");
            return;
        } // shouldn't happen

        DXEntry oldEntry = tableData.getOldEntry();

        DXEntry newEntry = tableData.getNewEntry();

        /*   Check to see if major surgery is needed - whether the user has been
         *   messing with the object class list. */

        if (classChangedOriginalEntry != null)
        {

            // use the saved state of the pre-class-changed entry as the 'old entry'
            // state.
            oldEntry = classChangedOriginalEntry;
            classChangedOriginalEntry = null;     // this is only used once! (either the object class change will
            // now succeed, or fail - either way, the entry state is reset to
            // match what's in the directory.)

            if (objectClassesChanged(oldEntry, newEntry))
            {
                oldEntry.removeEmptyAttributes();

                newEntry.setStatus(oldEntry.getStatus());

                Object[] delSet = CBArray.difference(oldEntry.toIDStringArray(), newEntry.toIDStringArray());

                /* if there *are* attributes that should no longer exist, delete them by adding them (blanked)
                 * to the complete 'newAtts' set of *all* known attributes. */

                if ((delSet != null) && (delSet.length > 0))
                {
                    for (int i = 0; i < delSet.length; i++)
                    {
                        newEntry.put(new DXAttribute(delSet[i].toString()));  // overwrite old values with an empty attribute
                    }
                }
            }
        }
        
        dataSource.modifyEntry(oldEntry, newEntry);
    }

    /**
     * Return the thingumy that should be printed.
     */
    public Component getPrintComponent()
    {
        return attributeTable;
    }

    /**
     * This editor is happy to be used in conjunction with other editors...
     */
    public boolean isUnique()
    {
        return false;
    }

    public String getName()
    {
        return title;
        //CBIntText.get("Table Editor");
    }

    public void setName(String title)
    {
        this.title = title;
    }

    public ImageIcon getIcon()
    {
        return new ImageIcon(Theme.getInstance().getDirImages() + "table.gif");
    }    //TE: returns an icon.

    public String getToolTip()
    {
        return CBIntText.get("The table editor is generally used for editing data, it also functions perfectly well as a simple, but robust, entry viewer.");
    }    //TE: returns a tool tip.

    public DataSink getDataSink()
    {
        return this;
    }

    public boolean canCreateEntry()
    {
        return true;
    }

    public void registerComponents(JMenuBar menu, JToolBar buttons, JTree tree, JPopupMenu treeMenu, JFrame jx)
    {
    }

    public void unload()
    {
    }

    /**
     * Use the default tree icon system based on naming value or object class.
     */
    public ImageIcon getTreeIcon(String rdn)
    {
        return null;
    }

    /**
     * Use the default popupmenu.
     */
    public JPopupMenu getPopupMenu(String rdn)
    {
        return null;
    }

    /**
     * Don't hide sub entries.
     */
    public boolean hideSubEntries(String rdn)
    {
        return false;
    }

    /**
     * Optionally register a new class loader for atribute value viewers to use.
     */
    public void registerClassLoader(ClassLoader loader)
    {
        myLoader = loader;
        myEditor.registerClassLoader(loader);
    }

    public void setVisible(boolean state)
    {
        super.setVisible(state);

        // has to be *after* previous call for SwingMagic reasons.
        if (state == false && tableData.changedByUser())  // user made changes - were they saved?  (i.e., are we
        {
            /*
             *    The setVisible() method may be called multiple time.  Only prompt
             *    the user the first time.
             */
            checkForUnsavedChanges();
        }
    }


    /**
     * Whether the editor has unsaved data changes.  This may be used by the GUI to prompt the user when
     * an editor pane is being navigated away from.
     * @return
     */
    /*
    public void checkForUnsavedChanges()
    {
        tableData.changedByUser();
    }
    */

    /**
     * This notifies the user that they are about to lose entered data (i.e. they've made changes and are about to a)
     * change classes or b) go to another entry), and allows them to save their data if they so choose...
     */
    public void checkForUnsavedChanges()
    {
        if (dataSource == null || dataSource.isActive() == false)
            return;  // no point prompting - nothing to save with!

        /*
         *    Only ever check the entry once (sometimes promptForSave can be called
         *    multiple time - remember that the 'save' function gets called by a
         *    separate thread).
         */

        if (tableData.changedByUser())
        {
            String save = CBIntText.get("Save");
            String discard = CBIntText.get("Discard");

            int result = JOptionPane.showOptionDialog(owner,
                                                 CBIntText.get("Submit changes to the Directory?"),
                                                 CBIntText.get("Save Data"), JOptionPane.DEFAULT_OPTION,
                                                 JOptionPane.QUESTION_MESSAGE, null,
                                                 new Object[] {save, discard}, save);
            if (result == 0)
            {
                writeTableData();  // nb - this queues a request to the directory
            }
        }
    }

}