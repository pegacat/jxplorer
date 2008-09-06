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
            frame.setIconImage(new javax.swing.ImageIcon(this.getClass().getResource("/images/logo_16.gif")).getImage());
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
        keystorePathLabel = new javax.swing.JLabel(CBIntText.get("Keystore File:"));
        keystorePathField = new javax.swing.JTextField();
        keystorePasswordLabel = new javax.swing.JLabel(CBIntText.get("Password:"));
        passwordField = new javax.swing.JPasswordField();
        browseButton = new javax.swing.JButton();
        //okButton = new javax.swing.JButton();
        //cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(this.DO_NOTHING_ON_CLOSE);

        //keystorePathLabel.setText(CBIntText.get("Keystore File:"));

        //keystorePasswordLabel.setText(CBIntText.get("Password:"));

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

         /* original code from Santthosh Babu Selvadurai - changed to internal JX classes for backwards compatibility to jdk 1.4
        javax.swing. rootPanelLayout = new javax.swing.GroupLayout(rootPanel);
        rootPanel.setLayout(rootPanelLayout);
        rootPanelLayout.setHorizontalGroup(
            rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rootPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(rootPanelLayout.createSequentialGroup()
                            .addComponent(keystorePasswordLabel)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(passwordField, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(rootPanelLayout.createSequentialGroup()
                            .addComponent(keystorePathLabel)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(keystorePathField, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, rootPanelLayout.createSequentialGroup()
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)
                        .addGap(23, 23, 23)))
                .addGap(6, 6, 6)
                .addComponent(browseButton)
                .addContainerGap())
        );
        rootPanelLayout.setVerticalGroup(
            rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rootPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(keystorePathLabel)
                    .addComponent(keystorePathField, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(passwordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(keystorePasswordLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 17, Short.MAX_VALUE)
                .addGroup(rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okButton)
                    .addComponent(cancelButton)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(rootPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(rootPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        pack();
        */
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
            javax.swing.JFileChooser fc = new javax.swing.JFileChooser(CBIntText.get("Please select the keystore file!"));
            fc.setDialogTitle(CBIntText.get("Smart Keytool") + ":" + CBIntText.get("Please select the keystore file!"));
            int returnVal = fc.showOpenDialog(null);

            if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION)
            {
                keystorePathField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        }
    }

}
