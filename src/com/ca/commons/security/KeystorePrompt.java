/*
 * KeystorePrompt.java
 *
 * Author: Santthosh Babu Selvadurai
 * Email: sbselvad@ncsu.edu
 * Created on July 2, 2007, 10:39 AM
 *
 * Copyright (c) 2007. All rights reserved.
 * You are granted a limited use, non exclusive, non transferable license to use this code and associated algorithm only inside
 * EMC research or with written permission from EMC copyright authority, provided that credit is acknowledged, by citation of this
 * software in any publications resulting from or incorporating data processed by this algorithm.
 *
 */

package com.ca.commons.security;

import com.ca.commons.cbutil.CBDialog;
import com.ca.commons.cbutil.CBIntText;
import com.ca.commons.cbutil.Theme;

import javax.swing.*;
import java.awt.event.*;

public class KeystorePrompt extends CBDialog implements ActionListener
{
    private javax.swing.JButton browseButton;
    //private javax.swing.JButton okButton;
    //private javax.swing.JButton cancelButton;
    private javax.swing.JLabel keystorePathLabel;
    private javax.swing.JLabel keystorePasswordLabel;
    private javax.swing.JPanel rootPanel;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JTextField keystorePathField;

    private String keystorePath = null;
    private String keystorePassword = null;

    /** Creates a new instance of KeystorePrompt */
    public KeystorePrompt(javax.swing.JFrame frame)
    {
        super(frame, CBIntText.get("Please select keystore file"), null);
        try
        {
            frame.setIconImage(new javax.swing.ImageIcon(this.getClass().getResource(Theme.getInstance().getDirImages()+"logo_16.gif")).getImage());
        }
        catch (Exception e) {} // we don't care if this stuff up - it's just a nice to have...

        initComponents();
        setLocationRelativeTo(null);
        setModal(true);
        setResizable(true);
        setSize(300,160);
        setVisible(true);

    }

     private void initComponents()
     {
        rootPanel = new javax.swing.JPanel();
        keystorePathLabel = new javax.swing.JLabel(CBIntText.get("Keystore File")+":");
        keystorePathField = new javax.swing.JTextField();
        keystorePasswordLabel = new javax.swing.JLabel(CBIntText.get("Password")+":");
        passwordField = new javax.swing.JPasswordField();
        browseButton = new javax.swing.JButton();

        setDefaultCloseOperation(this.DO_NOTHING_ON_CLOSE);


        browseButton.setText(CBIntText.get("Browse"));
        browseButton.addActionListener(this);

        //okButton.setText("OK");
        OK.addActionListener(this);

        //cancelButton.setText("Cancel");
        Cancel.addActionListener(this);

        display.add(keystorePasswordLabel);
        display.addGreedyWide(passwordField, 3);
         display.newLine();
          display.add(keystorePathLabel);
         display.addGreedyWide(keystorePathField, 2);
         display.add(browseButton);

  
    }

     public String getKeystorePath()
     {
         return keystorePath;
     }

     public String getKeystorePassword()
     {
         return keystorePassword;
     }

    public void actionPerformed(ActionEvent e)
    {
        if(OK == e.getSource())
        {
            setVisible(false);
            keystorePath = keystorePathField.getText();
            keystorePassword = passwordField.getPassword().toString();
        }
        else if(Cancel == e.getSource())
        {
            setVisible(false);
            System.exit(0);
        }
        else if(browseButton == e.getSource())
        {
            javax.swing.JFileChooser fc = new javax.swing.JFileChooser(CBIntText.get("Please select keystore file"));
            fc.setDialogTitle(CBIntText.get("Smart Keytool") + ":" + CBIntText.get("Please select keystore file"));
            int returnVal = fc.showOpenDialog(null);

            if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION)
            {
                keystorePathField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        }
    }

}
