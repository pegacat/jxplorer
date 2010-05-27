package com.ca.directory.jxplorer.editor;

import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.viewer.tableviewer.*;
import com.ca.directory.jxplorer.HelpIDs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *   Creates a Boolean Editor.
 *   @author Trudi.
 */
public class booleaneditor extends JDialog
    implements abstractstringeditor
{
    public static final String TRUE = "TRUE";
    public static final String FALSE = "FALSE";

    protected CBButton btnOk = null, btnCancel = null, btnHelp = null;
    protected JComboBox combo = null;
    protected CBPanel panel = null;
    protected editablestring newEditableString = null;

   /**
    *   Creates a panel and adds 'Ok', 'Reset', and 'Cancel' buttons to it.
    *   Checks for attribute type and calls the appropriate method.
    *   @param owner the parent frame, usually JXplorer.
    *   @param att the value of the postalAddress attribute.
    */
    public booleaneditor(Frame owner, AttributeValue att)
    {
        super(owner);
        setModal(true);

        String attID = att.getID();

        setTitle(CBIntText.get(attID));

        panel = new CBPanel();

        btnOk = new CBButton(CBIntText.get("OK"), CBIntText.get("Click here to make the changes (remember to click Submit in the table editor)."));
        btnOk.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                       save();
           }});

        btnCancel = new CBButton(CBIntText.get("Cancel"), CBIntText.get("Click here to exit."));
        btnCancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                       quit();
           }});

        btnHelp = new CBButton(CBIntText.get("Help"), CBIntText.get("Click here for Help."));    //TE: creates a new help button with a listener that will open JX help at appropriate location.
        CBHelpSystem.useDefaultHelp(btnHelp, HelpIDs.ATTR_BOOLEAN);

        combo = new JComboBox();
        combo.addItem(TRUE);
        combo.addItem(FALSE);
        combo.setSize(20, 40);

        panel.makeLight();
        panel.add(combo);
        panel.addln(new JLabel(" "));

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(btnOk);
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnHelp); //TODO add help when and ID is supplied.
        panel.add(buttonPanel);

        //TE: better way to implement keystroke listening...
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "enter");
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
        panel.getActionMap().put("enter", new MyAction(CBAction.ENTER));
        panel.getActionMap().put("escape", new MyAction(CBAction.ESCAPE));

        getContentPane().add(panel);
		setSize(210,100);
    }

    /**
     * Apparently it is better to use key bindings rather than adding a KeyListener...
     * "for reacting in a special way to particular keys, you usually should use key
     * bindings instead of a key listener".
     * This class lets the user set the key as an int.  If a key is pressed and it
     * matches the assigned int, a check is done for if it is an escape or enter key.
     * (27 or 10).  If escape, the quit method is called.  If enter, the save
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
         * save is called if Enter key is pressed.
         * @param e never used.
         */
        public void actionPerformed(ActionEvent e)
        {
            if (getKey() == ESCAPE)
                quit();
            else if (getKey() == ENTER)
                save();
        }
    }

   /**
    * Sets the attribute value in the attribute editor.
    * @param oldEditableString the string value.
    */
    public void setStringValue(editablestring oldEditableString)
    {
        String attString = oldEditableString.getStringValue();

        if(attString == null || attString.equalsIgnoreCase(FALSE))
		    combo.setSelectedItem(FALSE);
        else
            combo.setSelectedItem(TRUE);

        newEditableString = oldEditableString;
    }

   /**
    * Sets the changed attribute value from the attribute editor in the table.
    */
    public void save()
    {
        Object obj = combo.getSelectedItem();

        if(obj != null && ((String)obj).equalsIgnoreCase(TRUE))
            newEditableString.setStringValue(TRUE);
        else if(obj != null && ((String)obj).equalsIgnoreCase(FALSE))
            newEditableString.setStringValue(FALSE);

        quit();
    }

   /**
    *    Shuts the panel.
    */
    public void quit()
    {
        setVisible(false);
        dispose();
    }
}