package com.ca.directory.jxplorer.editor;

import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.JXConfig;
import com.ca.directory.jxplorer.JXplorer;
import com.ca.directory.jxplorer.HelpIDs;
import com.ca.commons.naming.DN;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;



/**
*    Sets up a binary editor that displays the binary data in hex format.
*    The user can load, save and edit the data .  By default the editor displays
*    the first 1000 characters of the data.  The edit button allows the user
*    to view/edit the whole file.
*/

public class basicbinaryeditor extends JDialog
    implements abstractbinaryeditor
{
    protected editablebinary editMe = null;
	protected JTextArea      field;
	protected CBButton		 btnLoad, btnSave, btnView, btnOK, btnCancel, btnHelp, btnEdit;
	protected Frame		     frame;		    // application frame, used to display dialog boxes.
	protected CBPanel        display;       // the panel that components are displayed on.
    protected boolean        editHex;       //TE: hex edit flag that checks if all hex is displayed, (or if the 'edit' button has been clicked).
    protected StringBuffer   hex;           //TE: the converted bytes into a hex string.
    protected byte[]         bytes;         //TE: global variable for the entry that is being either loaded, save or edited it is also used as the source for setting the value in the table editor if the user is not in edit mode (this would mean the value is unchanged).
    protected byte[]         oldBytes;      //TE: copy of the data from the directory.
    protected DN             currentDN = null;    //TE: the dn of the current entry.


    private static final String NODATAMSG = "No data available";        //TE: constant variable that is displayed in text editor (usually) if no value is available.

    //private static String lastDirectory = null;


	// default to certificate viewer, need to add the filename at the end
	private String 		viewCommand = "rundll32.exe cryptext.dll,CryptExtOpenCER ";

    private final static Logger log = Logger.getLogger(basicbinaryeditor.class.getName());


   /**
    *    Constructor.
    *    @param owner handle to the application frame, used to display dialog boxes.
    */

    public basicbinaryeditor(Frame owner)
    {
        this(owner, false);
    }



   /**
    *    Sets up the frame with one panel, one scrollable text area and six buttons.
    *    @param owner handle to the application frame, used to display dialog boxes.
    *    @param viewable specifies if there is a viewer for the binary data, if true, the "view" button is added to the panel.
    */

	public basicbinaryeditor(Frame owner, boolean viewable)
	{
        super(owner);
        setModal(true);
        setTitle(CBIntText.get("Binary Data"));

        editHex = false;

        display = new CBPanel();

        addMainViewComponent();

		btnView = new CBButton(CBIntText.get("View"), CBIntText.get(""));
		btnView.addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent e) {
						view();
       	}});

		btnLoad = new CBButton(CBIntText.get("Load"), CBIntText.get("Click here to load an external file."));
		btnLoad.addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent e) {
			   		    load();
	  	}});

		btnSave = new CBButton(CBIntText.get("Save"), CBIntText.get("Click here to save the data to an external file."));
		btnSave.addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent e) {
			   		    save();
	   	}});

        btnOK = new CBButton(CBIntText.get("OK"), CBIntText.get("Click here to make the changes (remember to click Submit in the table editor)."));
        btnOK.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                       setValue();
           }});

        btnEdit = new CBButton(CBIntText.get("Edit"), CBIntText.get("Edit the file data in Hex."));
        btnEdit.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));    //TE: this could take time so set the wait cursor.
                        btnEdit.setEnabled(false);                //TE: disabled to show user that they are in edit mode.
                        field.setText(bytes2HexString(bytes));    //TE: the un-shortened value (needed to edit, save etc).
                        field.setEnabled(true);                   //TE: so that the user can edit.
                        editHex = true;                           //TE: flag to show that the user is in edit mode.
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)); //TE: sets the default cursor.
           }});

        btnCancel = new CBButton(CBIntText.get("Cancel"), CBIntText.get("Click here to exit."));
        btnCancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                       quit();
           }});

        JPanel buttonPanel = new JPanel();
		buttonPanel.add(btnLoad);
        buttonPanel.add(btnSave);

        if (viewable)
            buttonPanel.add(btnView);

        buttonPanel.add(btnEdit);
		buttonPanel.add(btnOK);
        buttonPanel.add(btnCancel);
        buttonPanel.add(addHelp());       //TE: help button for information window.
        display.add(buttonPanel);

        //TE: better way to implement keystroke listening...
        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "enter");
        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
        display.getActionMap().put("enter", new MyAction(CBAction.ENTER));
        display.getActionMap().put("escape", new MyAction(CBAction.ESCAPE));

        getContentPane().add(display);
        setSize(435,300);
	}


    /**
     * Apparently it is better to use key bindings rather than adding a KeyListener...
     * "for reacting in a special way to particular keys, you usually should use key
     * bindings instead of a key listener".
     * This class lets the user set the key as an int.  If a key is pressed and it
     * matches the assigned int, a check is done for if it is an escape or enter key.
     * (27 or 10).  If escape, the quit method is called.  If enter, the setValue
     * method is called.
     * Bug 4646.
     * @author Trudi.
     */
    private class MyAction extends CBAction
    {
        /**
         * Calls super constructor.
         * @param key
         */
        public MyAction(int key)
        {
            super(key);
        }

        /**
         * quit is called if the Esc key pressed,
         * setValue is called if Enter key is pressed.
         * @param e never used.
         */
        public void actionPerformed(ActionEvent e)
        {
            if (getKey() == ESCAPE)
                quit();
            else if (getKey() == ENTER)
                setValue();
        }
    }



   /**
    *    Adds the text area (which is used for displaying the hex) to the panel.
    */

    public void addMainViewComponent()
    {
        field = new JTextArea();
        field.setLineWrap(true);     //TE: allows line wrapping.
        field.setEnabled(false);     //TE: disables the text area.
        field.setDisabledTextColor(Color.black);    //TE: sets the disabled text colour to black.

        final JScrollPane scrollPane = new JScrollPane(field);
        scrollPane.setPreferredSize(new Dimension(310,60));;
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);    //TE: no horizontal scroll bar.

        display.makeHeavy();
        display.addln(scrollPane);
        display.makeLight();
    }




   /**
    *    Sets up the help button (which is used to open the java help at the appropriate location).
    *    @return btnHelp the button to be added.
    */

    public CBButton addHelp()
    {
        btnHelp = new CBButton(CBIntText.get("Help"), CBIntText.get("Click here for Help."));    //TE: creates a new help button with a listener that will open JX help at appropriate location.
        CBHelpSystem.useDefaultHelp(btnHelp, HelpIDs.ATTR_BINARY);
        return btnHelp;
    }




   /**
    *    Sets the value to display in the editor.  If there is no value or if it is
    *    null, "No data available" is displayed in the editor.  Otherwise the value
    *    is shortened (if needed) to 1000 characters then converted to a hex string and
    *    displayed in the text area.
    *    @param editMe a value from the dsa that is to be displayed in the editor.
    */

	public void setValue(editablebinary editMe)
	{
        editHex = false;        //TE: hex edit flag is set to false b/c by default we set the length of the data to 1000 characters.

        this.editMe = editMe;

        bytes = editMe.getValue();

        oldBytes = bytes;     //TE: backup copy of data.

        if(bytes == null || bytes.length == 0)
            field.setText(NODATAMSG);        //TE: sets the text area with "No data available".
        else
        {
            setButtons(true);

            byte[] shortBytes;
            if (bytes.length < 1000)
                shortBytes = bytes;
            else
            {
                shortBytes = new byte[1000];
                System.arraycopy(bytes,0,shortBytes,0,1000);
            }
            field.setText(bytes2HexString(shortBytes));     //TE: sets the value of the field to a 1000 hex character string.
        }
	}



   /**
    *    Loads binary data from file.  Shortens it to 1000 characters then displays
    *    it in the binary editor.
    */

	protected void load()
	{
        editHex = false;        //TE: hex edit flag is set to false b/c by default we set the length of the data to 1000 characters.

		CBCache.cleanCache(currentDN.toString());    //TE: delete any temporary files associates with this entry.

        JFileChooser chooser = new JFileChooser(JXConfig.getProperty("binary.homeDir"));
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION)
            return;
        File file = chooser.getSelectedFile();
        JXConfig.setProperty("binary.homeDir", chooser.getSelectedFile().getParent());

        try
        {
            FileInputStream input = new FileInputStream(file);

            int length = (int)file.length();
            if (length > 0)
            {
                setButtons(true);

                bytes = new byte[length];
                int read = input.read(bytes);

                byte[] shortBytes;
                if (bytes.length < 1000)
                    shortBytes = bytes;
                else
                {
                    shortBytes = new byte[1000];
                    System.arraycopy(bytes,0,shortBytes,0,1000);        //TE: copies first 1000 elements only.
                }

                if (read > 0)
                    field.setText(bytes2HexString(shortBytes));         //TE: converts the shortened array to a hex string then set it in the text area.
            }
            input.close();
        }
    	catch(IOException e)
	    {
			log.log(Level.WARNING, "Error opening the file!", e);
		}

	}



   /**
    *    Save binary data to the file.
    */

	protected void save()
	{
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION)
            return;

        File file = chooser.getSelectedFile();

        try
        {
            FileOutputStream output = new FileOutputStream(file);
            if(editHex)
                output.write(CBParse.hex2bytes(field.getText()));        //TE: if the user is in edit mode there may be changes to the data therefore get it from the text area.
            else
                output.write(bytes);        //TE: the user is not in edit mode therefore no changes have been made ...'bytes' can be used.

            output.close();
        }
        catch(IOException e)
		{
            log.log(Level.WARNING, "Error writing the file!", e);
		}
	}



   /**
    *   Method purely for extending, the intention is to launch the file in
	*	its default viewer using CBLauncher.launchProgram.
    */

	public void view()
	{
		//TE: Extend this...odDocumentDOCEditor & odSpreadSheetXLSEditor.
	}



   /**
    *    Set the command for the binary viewer.
    *    The string must have a space at the end, as the filename will be appended.
    */

	public void setViewCommand(String viewCommand)
	{
		this.viewCommand = viewCommand;
	}



   /**
    *    Enables or disables the ok, save & edit buttons depending on the boolean value.
    *    @param enabled if true buttons are enabled, if false buttons are disabled.
    */

    public void setButtons(boolean enabled)
    {
        btnOK.setEnabled(enabled);
        btnEdit.setEnabled(enabled);
        btnSave.setEnabled(enabled);
    }



    /**
     *    Checks if the value in the editor has changed.
     *    @return true if data has changed, false if not.
     */

    public boolean isChanged()
    {
       if(Arrays.equals(bytes, oldBytes)==false)
           return true;
       return false;
    }



   /**
    *    Returns true if the current value of the attribute is valid (i.e. not null).
    */

	public boolean isValid()
	{
        if (field == null)
            return false;

		String newValue = field.getText();
        return (newValue != null && newValue.length() > 0);
   	}



   /**
    *    Returns a new value.  If the whole file is displayed (i.e. if the user has
    *    clicked on 'edit', the new value will be retrieved from the text area.  If the
    *    editor is only displaying the first 1000 characters the new value is retrieved
    *    from its source (in this case the bytes array).  The idea behind this
    *    is that the only time a value can be edited/changed is when the user has clicked the
    *    edit button.  Therefore this is the only time that we need to get a new value.
    *    @return byte[] a value that is to be set in the table editor.
    */

	public byte[] getNewValue()
	{
        if(editHex)        //TE: hex edit mode flag.  If it is true the new value is taken from the text area (i.e. it may have been edited).
        {
            String newvalue = field.getText();

		    if (newvalue!=null && newvalue.length()!=0)
                return CBParse.hex2bytes(newvalue);
		    else
			    return null;
        }
        else
            return bytes;        //TE: the user is not in edit mode therefore no changes have been made to the bytes array.
	}



    /**
     *    This will return a structure containing the current values of the attribute.
     *    @return old value.
     */

    public byte[] getOldValue()
    {
        return oldBytes;
    }



   /**
    *    Sets the value in the table editor.
    */

    public void setValue()
    {
        if (isChanged())
            editMe.setValue(getNewValue());
        quit();
    }



   /**
    *   Converts a byte to hex then to a string.
    *   @param bytesForConversion value to be converted.
    *   @return string representation of hex.
    */

    public String bytes2HexString(byte[] bytesForConversion)
    {
        if (bytesForConversion!=null)
        {
            setEnabled(true);

            hex = new StringBuffer(bytesForConversion.length*2);

            try
            {
                for (int i=0; i<bytesForConversion.length; i++)
                {
                    hex.append(CBParse.byte2Hex(bytesForConversion[i]));
                }
                return hex.toString();
            }
            catch (Exception e)
            {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));        //TE: just in case it fails and the cursor is still set to wait.
                CBUtility.error("Problem parsing byte to hex: " + e);
            }
        }

        setButtons(false);         //TE: disables ok & edit buttons.

        return NODATAMSG;      //TE: set the text area to an empty string if there is no value in the directory.
    }







   /**
    *    Shuts the window.
    */

    public void quit()
    {
        setVisible(false);
        dispose();
    }



   /**
    *   Sets the dn of the entry being modified.
    *   @param dn the DN of the entry being modified.
    *
    */

    public void setDN(DN dn)
    {
        currentDN = dn;
    }
}