package com.ca.commons.naming;

import com.ca.commons.cbutil.CBFileFilter;
import com.ca.commons.cbutil.CBIntText;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This extends the standard JFileChooser class to import ldif files,
 * and also to allow for a 'test' button, and to include a new 'TEST_OPTION'
 * response to file open requests.
 *
 * (c) Chris Betts; Pegacat Software (http://pegacat.com)
 */

public class LdifFileChooser extends javax.swing.JFileChooser implements ActionListener
{
    public static int IMPORT_OPTION = 3;
    boolean buttonPressed = false;
    JDialog dialog;
    
    public LdifFileChooser(String homeDirectory)
    {
        super(homeDirectory);
        setApproveButtonText("Preview Changes");

        addChoosableFileFilter(new CBFileFilter(new String[] {"ldif", "ldi"},"Ldif Files (*.ldif, *.ldi)"));

    }

    protected JDialog createDialog(Component parent) throws HeadlessException {
        dialog = super.createDialog(parent);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton button = new JButton(CBIntText.get("Import"));
        button.setToolTipText(CBIntText.get("Tries to show what changes would be made without writing to the directory"));
        button.addActionListener(this);
        panel.add(button);
        //button.setSelected(true);

        // TODO: unselect import button....
        dialog.add(panel, java.awt.BorderLayout.SOUTH);
        dialog.pack();
        setVisible(true);

        return dialog;
    }

    public int showOpenDialog(Component parent)
               throws HeadlessException
    {
        int response = super.showOpenDialog(parent);
        return (buttonPressed)? IMPORT_OPTION :response;

    }
      public void actionPerformed(ActionEvent e) {
          buttonPressed = true;
          dialog.dispose();   // disposing of the dialog triggers showOpenDialog to end...
      }
}