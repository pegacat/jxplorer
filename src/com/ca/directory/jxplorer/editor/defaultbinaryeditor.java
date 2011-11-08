package com.ca.directory.jxplorer.editor;

import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.JXConfig;
import com.ca.directory.jxplorer.JXplorer;
import com.ca.directory.jxplorer.HelpIDs;
import com.ca.commons.naming.DN;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This class extends JFileChooser so that we can add an accessory
 * object to it.  This accessory is a panel with two radio buttons on it:
 * save and load.  They are basically flags that tell JX if the user wants
 * to load a new file in or save the current one to an external file.<br>
 * This accessory panel also had a Help button.<br>
 * This class can be extended if you want to add another button for example.
 * This is done by over writing the addComponent method.
 * @author Trudi.
 */
public class defaultbinaryeditor extends JFileChooser
        implements abstractbinaryeditor
{
    /**
     * The DN of the current entry.
     */
    protected DN currentDN = null;

    /**
     * The data from the directory.
     */
    protected editablebinary editMe = null;

    /**
     * Global variable for the entry that is being either loaded or saved
     * it is also used as the source for setting the value in the table
     * editor if the user is not in edit mode (this would mean the value is unchanged).
     */
    protected byte[] bytes;

    /**
     * The accessory object that is added to this JFileChooser.
     */
    protected JFileChooserAccessory accessory = new JFileChooserAccessory();

    /**
     * The handle to the application frame, used to dsiplay dialog boxes.
     */
    protected Frame owner;

    private static Logger log = Logger.getLogger(defaultbinaryeditor.class.getName());

    /**
     * Constructor that calls the parent constructor with the file directory to open.
     * It also sets the accessory, the approve button tool tip and the dialog title.
     * @param owner handle to the application frame, used to display dialog boxes.
     */
    public defaultbinaryeditor(Frame owner)
    {
        super(JXConfig.getProperty("binary.homeDir"));
        this.owner = owner;

        setAccessory(accessory);

        setApproveButtonToolTipText(CBIntText.get("Click here to either load or save the file depending on the option selected."));
        setDialogTitle(CBIntText.get("Binary Data"));
    }

    /**
     * Opens the dialog.  If a value exists the save radio button
     * is enabled and selected otherwise it is disabled and the load
     * radio button is selected.<br><br>
     * When the dialog is closed, this method determines if the cancel
     * button was clicked where by doing nothing.  Otherwise if the OK
     * button was clicked, it calls the save method if the save radio
     * button is checked, or the load method if the load radio button is
     * checked.
     */
    public void showDialog()
    {
        if (bytes == null || bytes.length == 0)
        {
            accessory.setSaveEnabled(false);
            accessory.setSaveRadioSelected(false);
        }
        else
        {
            accessory.setSaveEnabled(true);
            accessory.setSaveRadioSelected(true);
        }

        if (showDialog(owner, CBIntText.get("OK")) != JFileChooser.APPROVE_OPTION)
            return;

        if (accessory.isSaveSelected())
            save();
        else if (accessory.isLoadSelected())
            load();

        quit();
    }

    /**
     * Save binary data to the file.
     */
    public void save()
    {
        File file = getSelectedFile();
        JXConfig.setProperty("binary.homeDir", getSelectedFile().getParent());

        try
        {
            FileOutputStream output = new FileOutputStream(file);
            output.write(bytes);

            output.close();
        }
        catch (IOException e)
        {
            log.log(Level.WARNING, "Error writing to the file!", e);
            return;
        }

        JOptionPane.showMessageDialog(owner, CBIntText.get("File ''{0}'' was successfully saved.", new String[] {file.getName()}), CBIntText.get("File Saved"), JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Loads binary data from file.
     */
    protected void load()
    {
        CBCache.cleanCache(currentDN.toString());    //TE: delete any temporary files associates with this entry.

        File file = getSelectedFile();
        JXConfig.setProperty("binary.homeDir", getSelectedFile().getParent());

        try
        {
            FileInputStream input = new FileInputStream(file);

            int length = (int) file.length();
            if (length > 0)
            {

                bytes = new byte[length];
                int read = input.read(bytes);
                editMe.setValue(bytes);
            }
            input.close();
        }
        catch (IOException e)
        {
            log.log(Level.WARNING,"Error opening the file!", e);
            return;
        }

        JOptionPane.showMessageDialog(owner, CBIntText.get("File ''{0}'' was successfully loaded.  Don't forget to click Submit in the Table Editor to save the data to the DSA.", new String[] {file.getName()}), CBIntText.get("File Loaded"), JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Does nothing.  Can be over written.
     */
    public void quit()
    {
        //TE: Extend this...odDocumentDOCEditor & odSpreadSheetXLSEditor.
    }

    /**
     * Sets the value to display in the editor.  If there is no value or if it is
     * null, "No data available" is displayed in the editor.  Otherwise the value
     * is shortened (if needed) to 1000 characters then converted to a hex string and
     * displayed in the text area.
     * @param editMe a value from the dsa that is to be displayed in the editor.
     */
    public void setValue(editablebinary editMe)
    {
        this.editMe = editMe;
        bytes = editMe.getValue();
    }

    /**
     * Sets the dn of the entry being modified.
     * @param dn the DN of the entry being modified.
     */

    public void setDN(DN dn)
    {
        currentDN = dn;
    }

    /**
     * Returns null.  Can be over written.
     * @return null.
     */
    public JComponent addComponent()
    {
        return null;
    }

    /**
     * Creats a panel with two radio buttons and a help button.
     * Calls the addComponent method incase this class has been
     * extended and the extending class wants to add an extra component.
     * @author Trudi.
     */
    public class JFileChooserAccessory extends JPanel
    {
        protected JLabel label;
        protected CBButton helpButton, btnCustom = null;

        protected JRadioButton saveRadio = new JRadioButton(CBIntText.get("Save"));
        protected JRadioButton loadRadio = new JRadioButton(CBIntText.get("Load"));

        /**
         * Sets up the panel.
         */
        public JFileChooserAccessory()
        {
            CBPanel mainPanel = new CBPanel();

            helpButton = new CBButton(CBIntText.get("Help"), CBIntText.get("Click here for Help."));
            CBHelpSystem.useDefaultHelp(helpButton, HelpIDs.ATTR_BINARY);

            saveRadio.setToolTipText(CBIntText.get("To save to an external file, select this option then click OK."));
            loadRadio.setToolTipText(CBIntText.get("To load from an external file, select this option then click OK."));
            loadRadio.setSelected(true);

            ButtonGroup radioGroup = new ButtonGroup();
            radioGroup.add(loadRadio);
            radioGroup.add(saveRadio);

            CBPanel radioPanel = new CBPanel();
            radioPanel.setBorder(new TitledBorder(CBIntText.get("Options")));
            radioPanel.addln(saveRadio);
            radioPanel.addln(loadRadio);

            mainPanel.addln(radioPanel);

            JComponent c = addComponent();
            if (c != null)
                mainPanel.addln(c);

            mainPanel.addln(helpButton);
            add(mainPanel, BorderLayout.CENTER);
        }

        /**
         * Sets the enabled state of the saveRadio button.
         * @param b the enabled state.
         */
        public void setSaveEnabled(boolean b)
        {
            saveRadio.setEnabled(b);
        }

        /**
         * Sets whether the saveRadio is selected.
         * @param b true to select the saveRadio, false otherwise.
         */
        public void setSaveRadioSelected(boolean b)
        {
            saveRadio.setSelected(b);
        }

        /**
         * @return true if loadRadio is selected.
         */
        public boolean isLoadSelected()
        {
            return loadRadio.isSelected();
        }

        /**
         * @return true if saveRadio is selected.
         */
        public boolean isSaveSelected()
        {
            return saveRadio.isSelected();
        }
    }
}