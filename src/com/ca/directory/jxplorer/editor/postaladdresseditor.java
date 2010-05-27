package com.ca.directory.jxplorer.editor;

import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.viewer.tableviewer.*;
import com.ca.directory.jxplorer.HelpIDs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *   Creates an attribute editor depending on which attribute is to be edited.
 *   Currently only creates a 'postalAddress' editor.
 *   @author Trudi.
 */
public class postaladdresseditor extends JDialog
    implements abstractstringeditor
{
    protected CBButton            btnOk, btnReset, btnCancel, btnHelp;
    protected JTextArea          area;
    protected CBPanel            display;
    protected editablestring     edStringNew;

    /**
     * Backup of attribute value before editing.
     */
    private final AttributeValue attBackup;

   /**
    *   Creates a panel and adds 'Ok', 'Reset', and 'Cancel' buttons to it.
    *   Checks for attribute type and calls the appropriate method.
    *   @param owner the parent frame, usually JXplorer.
    *   @param att the value of the postalAddress attribute.
    */
    public postaladdresseditor(Frame owner, AttributeValue att)
    {
        super(owner);
        setModal(true);

        attBackup = att;

        String attID = att.getID();

        setTitle(CBIntText.get(attID));

        display = new CBPanel();

        btnOk = new CBButton(CBIntText.get("OK"), CBIntText.get("Click here to make the changes (remember to click Submit in the table editor)."));
        btnOk.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                       save();
           }});

        btnReset = new CBButton(CBIntText.get("Reset"), CBIntText.get("Click here to reset your changes."));
        btnReset.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                       setStringValue(attBackup);
           }});

        btnCancel = new CBButton(CBIntText.get("Cancel"), CBIntText.get("Click here to exit."));
        btnCancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                       quit();
           }});

        btnHelp = new CBButton(CBIntText.get("Help"), CBIntText.get("Click here for Help."));    //TE: creates a new help button with a listener that will open JX help at appropriate location.
        CBHelpSystem.useDefaultHelp(btnHelp, HelpIDs.ATTR_POSTAL);

        area = new JTextArea();

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(310,60));

        display.makeHeavy();
        display.addln(scrollPane);
        display.makeLight();

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(btnOk);
        buttonPanel.add(btnReset);
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnHelp);
        display.add(buttonPanel);

        //TE: better way to implement keystroke listening...
        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "enter");
        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
        display.getActionMap().put("enter", new MyAction(CBAction.ENTER));
        display.getActionMap().put("escape", new MyAction(CBAction.ESCAPE));

        getContentPane().add(display);
		setSize(300,175);
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
    * Sets the attribute value in the text area of the attribute editor.
    * Replaces the '$' with '\n'.
    * @param edStringOld the string that needs the '$' replaced with the '\n'.
    */
    public void setStringValue(editablestring edStringOld)
    {
        String attString = edStringOld.getStringValue();
        String attRemoveDollar = attString.replace('$', '\n');
        attRemoveDollar = CBParse.replaceAllString(new StringBuffer(attRemoveDollar), "\\24", "$");  //TE: replaces '\24' with '$'.
        //attRemoveDollar = CBUtility.replaceAllString(new StringBuffer(attRemoveDollar), "\\", "");
		area.setText(attRemoveDollar);
        edStringNew = edStringOld;
    }

   /**
    * Sets the changed attribute value from the attribute editor in the table.
    */
    public void save()
    {
        String newAttribute = area.getText();

        // Replaces '$' with '\24'...
        String temp = CBParse.replaceAllString(new StringBuffer(newAttribute), "$", "\\24");

        // Places the '$' back...
        String editedAttribute = temp.replace('\n', '$');

        boolean check = true;

        // Un-comment this to do the 6 line/30character check!
        //check = addressChecker(editedAttribute);

        if(check)
        {
            // Sets the attribute value to reflect the changes made in the attribute editor...
            edStringNew.setStringValue(editedAttribute);
            quit();
        }
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