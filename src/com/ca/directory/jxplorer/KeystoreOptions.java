package com.ca.directory.jxplorer;


import com.ca.commons.cbutil.*;
import java.awt.Frame;
import java.util.*;
import javax.swing.*;
import java.awt.Dimension;
import javax.swing.border.TitledBorder;

import java.security.Provider;
import java.security.Security;

/**
 *    The Keystore Options dialog allows the user to set which keystore files
 *    are being used, and what type they are.  This is then used to set the
 *    value of the JX properties (kept in the dxconfig.txt file)
 *<PRE>
 *        keystoreType.clientcerts    - client cert keystore type (e.g. "JKS")
 *        keystoreType.cacerts        - trusted server keystore type (e.g. "JKS")
 *        option.ssl.clientcerts      - client cert location, e.g. D\:\\JavaProjects\\MyBrowser\\security\\clientcerts.jks
 *        option.ssl.cacerts          - trusted server keystore location, e.g. D\:\\JavaProjects\\MyBrowser\\security\\cacerts.jks
 *</PRE>
 */

public class KeystoreOptions extends CBDialog
{
     
    /**
     *    Keystore file locations (e.g. D\:\\JavaProjects\\MyBrowser\\security\\clientcerts.jks)
     */
      
    private String caStore, clientStore;
    
    /**
     *    The file choosers used to select the keystore paths.
     */
     
    CBFileChooserButton caChooser, clientChooser;

    /**
     *    The text fields used to enter the keystore paths (through the CBFileChooserButton).
     */
    
    JTextField caKeystoreLocationText, clientKeystoreLocationText;


    /**
     *    The text fields used to enter the keystore types. (e.g. "PKCS12" or "JKS")
     */
    
    CBJComboBox caTypeCombo, clientTypeCombo;
    
    private JTextField clientKeystore, trustedServerKeystore;   
    
    
    private boolean debug = false;
    
    /**
     *    A pointer to the properties file to modify with the user's changes
     */
    
    private Properties properties;
    
    /**
     *    A vector of (String) key types ('PKCS12' or 'JKS' etc.)
     */
    
    private Vector keyTypes;
    
    /**
     *    Initialise the dialog with a parent graphics frame and a list of
     *    properties to modify.
     *    @param owner the parent GUI frame to center on
     *    @param properties the property list to modify with the values described above.
     */
     
    KeystoreOptions(Frame owner, Properties properties)
    {
        super(owner, CBIntText.get("Keystore Options"), HelpIDs.SSL_CHANGE_KEYSTORE);  

        this.properties = properties;

        keyTypes = new Vector(10);

        /*
         *    Read the existing values from the properties list
         */
            
        String caType = (String) properties.get(JXConfig.CA_TYPE_PROPERTY);
        String clientType = (String) properties.get(JXConfig.CLIENT_TYPE_PROPERTY);
        caStore = (String) properties.get(JXConfig.CA_PATH_PROPERTY);
        clientStore = (String) properties.get(JXConfig.CLIENT_PATH_PROPERTY);

        keyTypes.add(caType);
        if (keyTypes.contains(clientType) == false)
            keyTypes.add(clientType);
        
        keyTypes = setupKeyList(keyTypes);  
        
        caTypeCombo = new CBJComboBox(keyTypes);
        caTypeCombo.setSelectedItem(caType);
        
        clientTypeCombo = new CBJComboBox(keyTypes);
        clientTypeCombo.setSelectedItem(clientType);
        
        /*
         *    Setup the gui using the current values.
         */

        makeWide();

        //  --- first line ---
		
		CBPanel caPanel = new CBPanel();
        caPanel.setBorder(new TitledBorder(CBIntText.get("Setup the Trusted CA / Server Keystore")));
        
        //  --- second line ---
        
        caPanel.add(new JLabel(CBIntText.get("CA/Server Keystore")+":"));
        
        caKeystoreLocationText = new JTextField(caStore);
        caPanel.addGreedyWide(caKeystoreLocationText, 2);
        caPanel.makeLight();
        
        caChooser = new CBFileChooserButton(caKeystoreLocationText, this, CBIntText.get("Load"), CBIntText.get("Open the file chooser."));
		caChooser.setPreferredSize(new Dimension(65,21));
        caChooser.setStartingDirectory(caStore);
        caPanel.addln(caChooser);
        
        //  --- third line ---

        caPanel.add(new JLabel(CBIntText.get("Set CA/Server Keystore Type")+":"));
		caTypeCombo.setPreferredSize(new Dimension(100,21));
		caPanel.add(caTypeCombo); 
		caPanel.add(new JLabel("  "));
		caPanel.addln(new JLabel(" "));
		    
        display.addln(new JLabel(" "));
		display.addln(caPanel);
        
        //  --- fourth ---
		
        display.addln(new JLabel(" "));
		CBPanel clientPanel = new CBPanel();
		clientPanel.setBorder(new TitledBorder(CBIntText.get("Setup the Client's Private Keystore")));
        
        //  --- fifth line ---
        
        clientPanel.add(new JLabel(CBIntText.get("Client Keystore")+":"));
        
        clientKeystoreLocationText = new JTextField(clientStore);
        clientPanel.addGreedyWide(clientKeystoreLocationText, 2);
        clientPanel.makeLight();
        
        clientChooser = new CBFileChooserButton(clientKeystoreLocationText, this, CBIntText.get("Load"), CBIntText.get("Open the file chooser."));
		clientChooser.setPreferredSize(new Dimension(65,21));
        clientChooser.setStartingDirectory(caStore);
        clientPanel.addln(clientChooser);
        
        //  --- sixth line ---

        clientPanel.add(new JLabel(CBIntText.get("Set Client Keystore Type") + ": "));
		clientTypeCombo.setPreferredSize(new Dimension(100,21));
        clientPanel.add(clientTypeCombo);       
		clientPanel.add(new JLabel("  "));
        clientPanel.addln(new JLabel("  "));
		display.addln(clientPanel);
    }

    public Vector setupKeyList(Vector keyList)
    {
        if (debug)
        {
            Provider[] providers = Security.getProviders();
            for (int i=0; i<providers.length; i++)
            {       
                Set keys = providers[i].keySet();
                Iterator iterator = keys.iterator();
                while (iterator.hasNext())
                {
                    String key = (String) iterator.next();                
                }
            }
        }
        
        Provider[] providers = Security.getProviders();
        for (int i=0; i<providers.length; i++)
        {
            Set keys = providers[i].keySet();
            Iterator iterator = keys.iterator();
            while (iterator.hasNext())
            {
                String key = (String) iterator.next();
        
                if (key.startsWith("KeyStore"))
                {
                    if (key.endsWith("ImplementedIn") == false)
                    {
                        String keyStoreName = key.substring(9);
                           
                        if (keyList.contains(keyStoreName) == false)
                            keyList.add(keyStoreName);
                    }
                }
            }
        }

        return keyList;
    }
    
    /**
     *    Called by the parent class when the user presses the 'OK' button.
     *    loads the properties object up with the new user entered values,
     *    as read from the text components.
     */
     
    public void doOK()
    {
        properties.setProperty(JXConfig.CA_TYPE_PROPERTY, (String)caTypeCombo.getSelectedItem());
        properties.setProperty(JXConfig.CLIENT_TYPE_PROPERTY, (String)clientTypeCombo.getSelectedItem());
        properties.setProperty(JXConfig.CA_PATH_PROPERTY, caKeystoreLocationText.getText());
        properties.setProperty(JXConfig.CLIENT_PATH_PROPERTY, clientKeystoreLocationText.getText());
    
        super.doOK();
    }    
    
}