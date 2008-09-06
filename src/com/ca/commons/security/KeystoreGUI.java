package com.ca.commons.security;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.*;

import java.security.*;
import java.security.cert.*;
import java.security.spec.*;
import java.net.URL;

//use Van Bui's Certificate Viewer
import com.ca.commons.cbutil.*;
import com.ca.commons.security.cert.CertViewer;

public class KeystoreGUI extends CBDialog implements ActionListener
{
    public boolean standAlone = false;

    public static final String ERRORCERT = "<unable to read>";
    public static final String DELETEDCERT = "<deleted>";

    CBButton viewCert, addCert, deleteCert, passwordButton,
    importKeyButton, exportKeyButton;

    CBButton[] commandButtons;

    protected KeyStore keystore = null;

    final JList certList;                // final is for ease of use in mouse listener
    DefaultListModel certListModel;

    public static ImageIcon smallCert;
    public static ImageIcon smallKeyCert;

    Properties properties;

    protected CBHelpSystem helpBroker;

    char[] password = null;

    protected String keystoreFile;

    protected String keystoreType;

    private static Logger log = Logger.getLogger(KeystoreGUI.class.getName());

    /**
     *   Whether to cripple the GUI because we're displaying a losing
     *   key format (e.g. KSE) which doesn't support a bunch of operations...
     */

    private boolean crippled = false;

    /**
     *    Whether to additionally cripple the set password because we're
     *    displaying a key format which doesn't support 'set password'
     */

    private boolean cripplePassword = false;

    // whether the keystore has been modified and must be written back to disk.
    private boolean changed = false;

    /**
     *   This creates the Keystore config window to manage a particular keystore.
     *   @param owner the parent frame (used for internal GUI stuff)
     *   @param props the JX property list (used to get and set default keystore directories)
     *   @param keyStoreLocation the location of the java keystore to manage.
     *   @param keyStorePassword the password of the encrypted keystore - may be null,
     *                           in which case the user will be prompted.
     *   @param keyStoreType the java abreviation of the keystore type (typically 'jks' for
     *          'java keystore' - the default java file based keystore).
     *   @param title a meaningfull (to the user) name for the keystore
     *   @param handlePrivateKeys whether the keystore manager will allow the
     *          user to associate a private key with a particular certificate.
     *   @param helpTopic the link into the default java help system (if used).  See
     *          @see com.ca.commons.cbutil.CBHelpSystem
     */

    public KeystoreGUI( Frame owner, Properties props, String keyStoreLocation,
                        char[] keyStorePassword, String keyStoreType, String title,
                        boolean handlePrivateKeys, String helpTopic, boolean standAlone)
    {
        super(owner, title, helpTopic); // create modal dialog ...

        //if(title.compareToIgnoreCase("SmartKeytool 1.0")==0)
        if (standAlone)
        {
            this.standAlone=true;
            // converted this to make it backwardly compatible - CB
            try
            {
                this.owner.setIconImage(new ImageIcon("./images/logo_16.gif").getImage());
            }
            catch (Exception e) {} // we don't care if this stuff up - it's just a nice to have...

        }

        if ("KSE".equals(keyStoreType))
            crippled = true;

        properties = props;

        password = keyStorePassword;

        CertViewer.setProperties(properties);

        if (smallCert == null)
        {
            smallCert = getImageIcon("sslcert.gif");
        }

        if (smallKeyCert == null)
        {
            smallKeyCert = getImageIcon("sslkeycert.gif");
        }

        keystoreFile =  keyStoreLocation;

        keystoreType = keyStoreType;

        display.makeHeavy();

        JScrollPane scrollPane = new JScrollPane();

        certList = new JList();

        /*
         *    Problem here - some keystores require passwords to
         *    even look at them, while others don't.  Not sure how
         *    to handle this in general... in the meantime we have a
         *    a series of hacks...
         */

        if (password != null || "JKS".equalsIgnoreCase(keystoreType))
        {
            setupCertificateList();

        }
        else if ("KSE".equalsIgnoreCase(keystoreType) && keystoreFile!= null &&
                 keystoreFile.toLowerCase().endsWith(".der"))
        {
            setupCertificateList();
            cripplePassword = true;
        }
        else
        {
            if (setupPasswordAndKeystore(keystoreType, keystoreFile, this))          // no password, = no keystore
            {
                refreshView();                    // reset certListModel
                certList.setModel(certListModel); // set the display JList of certs..
            }
        }

        scrollPane.getViewport().setView(certList);

        display.add(scrollPane, 1, 1, 2, ((handlePrivateKeys)?7:5));

        display.makeLight();

        display.add(viewCert = new CBButton("  " + CBIntText.get("View Certificate"), CBIntText.get("View a certificate in detail."), getImageIcon("sslview.gif")), 3, 1);

        display.add(addCert = new CBButton("  " + CBIntText.get("Add Certificate"), CBIntText.get("Add a new trusted server certificate"), getImageIcon("ssladd.gif")), 3, 2);
        if (crippled)
        //addCert.disable();
        addCert.setEnabled(false);

        display.add(deleteCert = new CBButton("  " + CBIntText.get("Delete Certificate"), CBIntText.get("Delete an unwanted or out of date server certificate"), getImageIcon("ssldelete.gif")), 3, 3);

        display.add(passwordButton = new CBButton("  " + CBIntText.get("Set Password"), CBIntText.get("Change the certificate keystore password."), getImageIcon("sslpassword.gif")), 3, 4);

        importKeyButton = new CBButton("  " + CBIntText.get("Set Private Key"), CBIntText.get("Match a PKCS-8 private key with a certificate"), getImageIcon("sslprivatekey.gif"));

        exportKeyButton = new CBButton("  " + CBIntText.get("Export Private Key"), CBIntText.get("Export the PKCS-8 private key matching a certificate"), getImageIcon("sslexprivatekey.gif"));


        if (handlePrivateKeys)
        {
            display.add(importKeyButton, 3, 5);
            display.add(exportKeyButton, 3, 6);
        }


        commandButtons = new CBButton[] {viewCert, addCert, deleteCert, passwordButton, importKeyButton, exportKeyButton};

        for (int i=0; i<commandButtons.length; i++)
        {
            commandButtons[i].setHorizontalAlignment(SwingConstants.LEFT);
            commandButtons[i].addActionListener(this);
        }

        if (crippled)
        {
            JButton[] crippledButton = {addCert, deleteCert, exportKeyButton, importKeyButton};
            for (int i=0; i<4; i++)
            {
                //crippledButton[i].disable();
                crippledButton[i].setEnabled(false);
                crippledButton[i].removeActionListener(this);
                crippledButton[i].setToolTipText(CBIntText.get("Not available with this security provider"));
                crippledButton[i].setForeground(Color.gray);
            }
        }

        if (cripplePassword)
        {
             //passwordButton.disable();
             passwordButton.setEnabled(false);
             passwordButton.removeActionListener(this);
             passwordButton.setToolTipText(CBIntText.get("Not available with this security provider"));
             passwordButton.setForeground(Color.gray);
        }


        // special hack for double clicks

        MouseListener mouseListener = new MouseAdapter()
                                      {
                                          public void mouseClicked(MouseEvent e)
                                          {
                                              if (e.getClickCount() == 2)
                                              {
                                                  if (e.getModifiers() == MouseEvent.BUTTON1_MASK)
                                                  {
                                                      //int index = certList.locationToIndex(e.getPoint());
                                                      CertItem cert = (CertItem)certList.getSelectedValue();
                                                      viewCurrentCert(cert);
                                                  }
                                              }
                                          }
                                      };

        certList.addMouseListener(mouseListener);

        display.add(new JLabel("    "), 3, ((handlePrivateKeys)?7:5)); // padding...
    }


    /**
     *    checks actions on the various keystore affecting buttons.
     *    Note that the OK and Cancel button are handled by doOK() and
     *    doCancel() inherited from the base class.
     */

    public void actionPerformed(ActionEvent e)
    {

        JButton src = ((JButton)e.getSource());

        CertItem cert = (CertItem)certList.getSelectedValue();

        if (src == viewCert)
        {
            viewCurrentCert(cert);
        }
        else if (src == addCert)
        {
            addNewCert();
        }
        else if (src == deleteCert)
        {
            if(cert==null)
                CBUtility.error(CBIntText.get("Please select a certificate to delete."), null);
            else
                deleteCurrentCert(cert);
        }
        else if (src == passwordButton)
        {
            setupPasswords();
        }
        else if (src == importKeyButton)
        {
            importKey(cert);
        }
        else if (src == exportKeyButton)
        {
            exportKey(cert);
        }
    }


    /**
     *    If the user is satisfied with their changes, attempt to
     *    write the keystore.  Some checks may be required first,
     *    depending on the keystore type.
     */

    public void doOK()
    {
        if (changed)
        {
            /* check that the user has entered a valid passphrase */
            if (checkPassword() == false)
                return; // nothing to do.

            try
            {
                if (writeKeyStore(password, keystore, keystoreFile, keystoreType) == false)
                {
                    clearPassword(password);
                    password = null;
                    return;  // error given by writeKeyStore() method.
                }
            }
            catch (Exception e)
            {
                CBUtility.error(CBIntText.get("Error importing key file."), e);
                return;
            }
        }

        changed = false;

        // clean up the old password

        clearPassword(password);
        password = null;

        super.doOK();
//System.exit(0);       //XXX TEMP
    }

    public void doCancel()
    {
        if (changed)
        {
            String[] options = { CBIntText.get("Revise Changes"), CBIntText.get("Discard Changes") };

            int opt = JOptionPane.showOptionDialog(null, CBIntText.get("You have unsaved changes!"), "Warning",
                      JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                      null, options, options[0]);

            if (opt == 0) return;
        }

        super.doCancel();
//System.exit(0);  //XXX TEMP

    }

    /**
     *    Allows the user to match a private key with a particular certificate.
     *    (Currently limited to pkcs 8 - other may be possible depending on keystore
     *    implementation).
     *    @param certItem the certificate whose private key is to be imported.
     */

    protected void importKey(CertItem certItem)
    {
        try
        {
            /* Check that the user has selected a certificate to associate with the new key */

            if (certItem == null || certItem.getX509Cert() == null)
            {
                CBUtility.error(CBIntText.get("Please select a certificate to match with a key."), null);
                return;
            }

            /* Get the user to select a pkcs 8 private key file */

            File keyFile = getKeyFile(CBIntText.get("Select a pkcs8 private key file"));

            if (keyFile == null)
                return;  // nothing to do.

            /* Read the file data into a byte array */

            FileInputStream in = new FileInputStream(keyFile);
            byte [] buffer = new byte[(int) (keyFile.length())];
            in.read(buffer);
            in.close();

            /* check if this is pem base64 encoded data - if it is, translate it */
            if (CBSecurity.isPEM(buffer))
            {
                //TODO: XXX <your code to handle encrypted private keys here> XXX//

                byte[] pemData = CBSecurity.convertFromPEM(buffer, new String(CBSecurity.PEM_KEY_HEADER).getBytes());
                if (pemData != null)
                    buffer = pemData;
                else
                {
                    CBUtility.error(CBIntText.get("Unable to load key: does not begin with {0} ", new String[] {new String(CBSecurity.PEM_KEY_HEADER)}));
                    return;
                }
            }

            /* check that the user has entered a valid passphrase */
            if (checkPassword() == false)
                return; // nothing to do.

            /* import key */

            String alias = certItem.getAlias();

            java.security.cert.Certificate[] certChain = keystore.getCertificateChain(alias);

            //XXX <your code to handle unencrypted private keys here> XXX//

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);

            KeyFactory factory = KeyFactory.getInstance("RSA");

            PrivateKey key = factory.generatePrivate(keySpec);

            if (certChain == null || certChain.length == 0)  // ...which it often does, since cert
            {                                                // chains often aren't stored properly
                certChain = new java.security.cert.Certificate[1];              // in the keystore
                certChain[0] = certItem.getX509Cert();
            }

            keystore.setKeyEntry(alias, key, password, certChain);

            refreshView();
            changed = true;
        }
        catch (Exception e)
        {
            CBUtility.error("Error importing key file.", e);
            e.printStackTrace();
        }

    }

    /**
     *    Allows the user to export a private key with a particular certificate.
     *    (Currently limited to pkcs 8 - other may be possible depending on keystore
     *    implementation).
     *    @param certItem the certificate whose private key is to be exported.
     */

    protected void exportKey(CertItem certItem)
    {
        try
        {
            /* Check that the user has selected a certificate to associate with the new key */

            if (certItem == null || certItem.getX509Cert() == null)
            {
                CBUtility.error(CBIntText.get("Please select a certificate to match with a key."), null);
                return;
            }

            /* Get the user to select a pkcs 8 private key file */

            File keyFile = getKeyFile(CBIntText.get("Select a file to save the pkcs8 key to."));

            if (keyFile == null)
                return;  // nothing to do.

            /* check that the user has entered a valid passphrase */

            if (checkPassword() == false)
                return; // nothing to do.

            /* read key from keystore */

            Key myKey = keystore.getKey(certItem.getAlias(), password);

            if (myKey == null)
            {
                return;
            }
            byte[] data = myKey.getEncoded();

            if (data == null)
            {
                throw new Exception("Unable to access encoded private key data");
            }

            if (keyFile.toString().toLowerCase().endsWith(".pem"))
            {
                data = CBSecurity.convertToPEMPrivateKey(data);
            }

            FileOutputStream out = new FileOutputStream(keyFile);
            out.write(data);
            out.close();
        }
        catch (Exception e)
        {
            CBUtility.error("Error exporting key file.", e);
            e.printStackTrace();
        }

    }




    /**
     *    This prompts the user to select a pkcs8 file to import, and
     *    attach to an existing certificate.
     *    @return the File name of the selected pkcs8 file.
     */

    protected File getKeyFile(String title)
    {
        JFileChooser chooser = new JFileChooser(properties.getProperty("cert.homeDir"));
        chooser.addChoosableFileFilter(new CBFileFilter(new String[] {"der", "pem"},"Certificate Files (*.der, *.pem)"));
        chooser.setDialogTitle(title);

        int option = chooser.showOpenDialog(owner);

        while (true)
        {
            if (option == JFileChooser.APPROVE_OPTION) // only do something if user chose 'ok'
            {
                File keyFile = chooser.getSelectedFile();
                if (keyFile == null)
                    CBUtility.error(CBIntText.get("Please select a file"));
                else
                {
                    properties.setProperty("cert.homeDir", keyFile.getParent());
                    chooser = null;
                    return keyFile;
                }
            }
            else
            {
                chooser = null;
                return null;   // user selected cancel, or closed the window.
            }
        }
    }

    /**
     *    Uses the CertViewer to display the contents of the selected
     *    certificate.
     *    @param cert the certificate to display.
     */

    protected void viewCurrentCert(CertItem cert)
    {
        if (cert == null || cert.getX509Cert() == null)  // nothing to do.
        {
            CBUtility.error(CBIntText.get("Please select a certificate to view."), null);
            return;
        }

        CertViewer viewer = new CertViewer(owner, cert.getX509Cert());
        // converted this to make it backwardly compatible - CB
        if(standAlone)
        {
            try
            {
                this.owner.setIconImage(getImageIcon("logo_16.gif").getImage());
            }
            catch (Exception e) {} // we don't care if this stuff up - it's just a nice to have...
        }

        viewer.setVisible(true);
    }

    /**
     *    Checks the list to see which the currently selected certificate is,
     *    and then prompts the user to confirm the deletion.
     *    @param certItem the certificate to delete.
     */

    protected void deleteCurrentCert(CertItem certItem)
    {

        if (certItem == null)
            return;  // nothing to do.

        int delete = JOptionPane.showConfirmDialog(this, CBIntText.get("delete certificate: {0} ?", new String[] {certItem.getAlias()}),
                     CBIntText.get("Confirm Certificate Deletion"), JOptionPane.OK_CANCEL_OPTION);

        if (delete != JOptionPane.OK_OPTION)
            return; // nothing to do

        if (keystore == null) // ? Can't see how this would happen
        {
            CBUtility.error(CBIntText.get("Internal Error: unable to find Certificate Keystore"), null);
            return;
        }

        if (checkPassword() == false)
            return; // nothing to do.

        try
        {
            keystore.deleteEntry(certItem.getAlias());

            refreshView();
            changed = true;

            return;
/* DEFER
            if (writeKeyStore(password, keystore, keystoreFile) == true)
            {
                refreshView();
                return;        // SUCCESS!!!
            }
*/
        }
        catch (KeyStoreException e)
        {
            CBUtility.error(CBIntText.get("Error - unable to delete key: {0} from key store",  new String[] {certItem.getAlias()}), e);
        }

        // FAILURE!!!
        try    // try to reset entry in local keystore
        {
            keystore.setCertificateEntry(certItem.getAlias(), certItem.getX509Cert());
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "unable to recover key store.",e);
        }
    }


    /**
     *    checks that the user has entered a valid password.  If they haven't,
     *    it prompts for one.
     *    @return whether a valid password has been entered and checked against
     *            the keystore.
     */

    protected boolean checkPassword()
    {
        if (password != null)
            return true;  // we already have a password.

        return setupPasswordAndKeystore(keystoreType, keystoreFile, this);  // we don't, so try to get one...
    }

    /**
     *    <p>This allows the user to enter their password, which remains valid
     *    for the life of this component.  </p>
     *
     *    <p>This also sets up the keystore</p>
     *    @return whether the password successfully opened the keystore.
     */

    public boolean setupPasswordAndKeystore(String keystoreType, String keystoreFile, Component owner)
    {
        if ((password != null) && (keystore != null))  // no thanks, we already have one...
            return true;

        String message = CBIntText.get("Enter Key Store Password");
        while ((password = getPassword(owner, message)) != null)
        {
            keystore = readKeyStore(password, keystoreType, keystoreFile);

            if (keystore != null)
            {
                return true;    // we have a valid keystore!
            }
            // this message is only displayed if we go around the loop again.
            message = CBIntText.get("Password incorrect. Please try again.");
        }

        return false;   // user hasn't entered a password and has cancelled out.
    }

    public static char[] getPassword(Component owner, String message)
    {
        char[] password;
        JPasswordField passwordInput = new JPasswordField();
        int response = JOptionPane.showConfirmDialog(owner, passwordInput,
                       message, JOptionPane.OK_CANCEL_OPTION);

        if (response != JOptionPane.OK_OPTION)
            password = null;  // give up, go home
        else
            password = passwordInput.getPassword();
        return password;
    }

    /**
     *    Sets up the keystore variable, using the
     *    current password (may be null) and keystore file.
     */

    /*
    public static boolean setupKeyStore(char[] password, KeyStore keyStore, String keyStoreType, String keyStoreFile)
    {
        KeyStore newKeystore = readKeyStore(password, keyStoreType, keyStoreFile);
        if (newKeystore == null)
        {
            return false;
        }
        else
        {
            keyStore = newKeystore;
            return true;
        }
    }
    */
    /**
     *    Checks if the given alias name already exists in the
     *    Keystore.
     */

    private boolean listContains(String aliasName)
    {
        if (aliasName == null) return false;

        for (int i=0; i<certListModel.size(); i++)
            if (aliasName.equals(((CertItem)certListModel.get(i)).alias))
                return true;

        return false;
    }

    /**
     *    Allows the User to browse to a new Cert (on disk) and
     *    import it.
     */

    protected void addNewCert()
    {
        CertViewer.CertAndFileName info = CertViewer.loadCertificate(owner);
        if (info == null || info.cert == null)
        {
            return;  // no cert selected
        }

        String alias = null;

        if (info.fileName != null)
        {
            alias = new File(info.fileName).getName();
            if (alias != null && alias.indexOf('.')>0)
                alias = alias.substring(0, alias.indexOf('.')); // trim to get the stem
        }

        if (alias == null)
            alias = CBIntText.get("default");

        boolean nameAlreadyExists = false;
        do
        {
            alias = (String)JOptionPane.showInputDialog(this, CBIntText.get("Please enter a short unique name for this Certificate"),
                CBIntText.get("Enter Certificate Alias"), JOptionPane.QUESTION_MESSAGE, null, null, alias);

            nameAlreadyExists = listContains(alias);
            if (nameAlreadyExists)
            {
                JOptionPane.showMessageDialog(this, CBIntText.get("That name already exists."),
                CBIntText.get("Duplicate Alias"), JOptionPane.ERROR_MESSAGE);
            }
        }
        while (nameAlreadyExists);

        if (alias == null || alias.length() == 0)
            return; // nothing to do

        if (checkPassword() == false)
            return; // nothing to do.

        try
        {
            keystore.setCertificateEntry(alias, info.cert);

            refreshView();
            changed = true;
            return;
        }
        catch (KeyStoreException e)
        {
            CBUtility.error(CBIntText.get("Error - unable to add key: {0} from key store", new String[] {alias}), e);
        }
        // FAILURE!
        try
        {
            keystore.deleteEntry(alias);  // try to clean up.
        }
        catch (Exception e)
        {}
    }

    /**
     *    Reread the key store after an addition or deletion operation,
     *    and refresh certListModel.
     */

    protected void refreshView()
    {
        CertItem[] certs = getKeyStoreCerts(keystore);

        if (certListModel == null)
            setupCertificateListGUI();

        certListModel.removeAllElements();
        for (int i=0; i<certs.length; i++)
            certListModel.addElement(certs[i]);
    }

    /**
     *    Initialise empty list models, and associate the
     *    certificate list renderer with the cert list.
     *
     */

    protected void setupCertificateListGUI()
    {
        certListModel = new DefaultListModel();

        certList.setModel(certListModel);

        certList.setCellRenderer(new CertificateListRenderer());
    }

    /**
     *    Initialises a selection list of CertItems from the keystore.
     */

    protected void setupCertificateList()
    {
        // Initially read the keystore without a password, for
        // simple listing...

        keystore = readKeyStore(password, keystoreType, keystoreFile);

        setupCertificateListGUI();

        if (keystore == null)
            JOptionPane.showMessageDialog(this, CBIntText.get("Unable to find/open keystore: {0}", new String[] {keystoreFile}), CBIntText.get("Error: no Keystore"), JOptionPane.ERROR_MESSAGE);
        else
            refreshView();
    }


    /**
     *    The keystore has a particular password protecting its contents.
     *    This menu allows the user to change that password.
     */

    public class PasswordDialog extends CBDialog
    {
        public JPasswordField old, new1, new2;

        public PasswordDialog(Frame owner)
        {
            super(owner, CBIntText.get("Change the Key Store Password."), null);
            addln(new JLabel(getImageIcon("sslpassword.gif")));
            addln(new JLabel(CBIntText.get("This screen allows you to enter")));
            addln(new JLabel(CBIntText.get("a new key store password")));
            addln(new JLabel(" "));
            addln(new JLabel(CBIntText.get("Enter the old password")));
            addln(old = new JPasswordField());
            addln(new JLabel(CBIntText.get("The new Password") + ":"));
            addln(new1 = new JPasswordField());
            addln(new JLabel(CBIntText.get("Confirm the new Password") + ":"));
            addln(new2 = new JPasswordField());
            setSize(240, 320);
            CBUtility.center(this, owner);
        }

    }

    /**
     *   This allows the user to change the password used to protect
     *   the keystore.
     *
     */

    protected void setupPasswords()
    {
        PasswordDialog newPassword = new PasswordDialog(owner);

        // CB - not sure what's happening here;
        //if(standAlone)
        //    newPassword.setIconImage(new ImageIcon("./ODlogo.gif")).getImage());

        // Various things can go wrong here - keep showing the
        // user the password change window until they enter a
        // valid set of passwords, or get sick of it...

        while (newPassword.wasCancelled() == false)
        {
            newPassword.setVisible(true);

            if (newPassword.wasCancelled())
                return; // do nothing.

            char[] oldPass, newPass1, newPass2;
            oldPass = newPassword.old.getPassword();
            newPass1 = newPassword.new1.getPassword();
            newPass2 = newPassword.new2.getPassword();

            if (Arrays.equals(newPass1, newPass2) == true)
            {
                // this throws an error directly to the user if it fails
                KeyStore newKeystore = readKeyStore(oldPass, keystoreType, keystoreFile);
                if (newKeystore != null)
                {
                    if (writeKeyStore(newPass1, newKeystore, keystoreFile, keystoreType) == true)
                    {
                        keystore = newKeystore;
                        password = newPass1;

                        JOptionPane.showMessageDialog(this, CBIntText.get("Passwords successfully changed!"),
                                                      CBIntText.get("Success!"), JOptionPane.INFORMATION_MESSAGE);
                        return; // SUCCESS!
                    }
                }
                else
                    CBUtility.error(CBIntText.get("Unable to change password - incorrect password entered?"));

            }
            else
            {
                CBUtility.error(CBIntText.get("The new passwords were not identical!"), null);
            }
        }

    }

    protected void clearPassword(char[] c)
    {
        if (c != null)
            for (int i=0; i<c.length; i++)
                c[i] = 0;
    }

    /**
     *    This extracts an array of CertItem-s from a keystore,
     *    for display in the GUI.
     *    @param keystore the keystore to use.
     *    @return an array of CertItem-s representing the certificates and aliases
     *            stored in the keystore.
     */

    public static CertItem[] getKeyStoreCerts(KeyStore keystore)
    {
        try
        {
            Vector certVector = new Vector(10);  // vector of cert items...

            //PrivateKey privKey=null;

            Enumeration a = keystore.aliases();
            while ( a.hasMoreElements() )
            {
                String alias = (String) a.nextElement();
                CertItem item = new CertItem(alias);

                if ( keystore.isKeyEntry(alias) )
                {
                    X509Certificate userCert = (X509Certificate)keystore.getCertificate(alias);
                    item.addX509Cert(userCert);
                    item.setHasPrivateKey(true);
                }
                else
                {
                    X509Certificate userCert = (X509Certificate)keystore.getCertificate(alias);
                    item.addX509Cert(userCert);
                }
                certVector.add(item);
            }

            return (CertItem[]) certVector.toArray(new CertItem[0]);
        }
        catch (Exception e)
        {
            CBUtility.error(CBIntText.get("Error reading certificate from keystore."), e);
            return null;
        }


    }

    /**
     * initialises the keystore by reading the saved keystore file.
     * @param pass the password protecting the keystore.  If this is
     *        null, the keystore will be read-only, and no validation
     *        will be performed.
     * @param storeType - the type of the keystore.  Unless a custom
     *        security provider is being used, this will almost certainly
     *        be 'jks'.
     * @param keyFile the file name of the keystore.
     * @return the new keystore, or null if an error occurred.
     */

    public static KeyStore readKeyStore(char[] pass, String storeType, String keyFile)
    {
        //byte[] b=null;

        try
        {
            KeyStore keystore = KeyStore.getInstance( storeType );  // storeType is usually 'jks' for default java keystore

            FileInputStream fis = new FileInputStream(keyFile);
            keystore.load(fis, pass);

            fis.close();

            return keystore;
        }
        catch (Exception e)
        {

            CBUtility.error(CBIntText.get("Error opening certificate keystore {0}.  Probably an incorrect password", new String[] {keyFile}), e);

            return null;
        }
    }

    /**
     *    writes the keystore to a password protected file.
     *    @param password the password to use while saving it.
     *    @param keystore the certificate key store to save.
     *    @param keyFile the name of the file to save to.
     *    @param keystoreType the type of store - e.g. "JKS" or "KSE" or "PKCS12"
     *    @return the success status of the operation.
     */

    public static boolean writeKeyStore(char[] password, KeyStore keystore, String keyFile, String keystoreType)
    {
        if ("KSE".equalsIgnoreCase(keystoreType))
        {
            CertItem[] certs = getKeyStoreCerts(keystore);

            if (certs.length > 2)
               return givePKCS12ErrorMsg(CBIntText.get("This PKCS12 File can only have one certificate, one key, and one CA certificate"));

            if (certs.length == 2 && certs[0].hasPrivateKey && certs[1].hasPrivateKey)
               return givePKCS12ErrorMsg(CBIntText.get("This PKCS12 File can only have one certificate, one key, and one CA certificate"));

            // XXXcheck for if second cert if server certificate?
        }
        FileOutputStream fos = null;
        try
        {
            if (password == null)
                throw new KeyStoreException("null password not allowed");
            fos = new FileOutputStream(keyFile);
            keystore.store(fos, password);
            fos.close();
            return true;
        }
        catch (Exception e)  // IOException or KeyStoreException
        {
            CBUtility.error(CBIntText.get("Error saving certificate keystore.") +
                            "\n" + CBIntText.get("Probably an invalid password"), e);

            // try to clean up any mess.
            if (fos != null)
                try {fos.close();} catch(IOException e2) {}

            return false;
        }

    }

    /**
     *    Utility to reduce code duplication above
     */

    private static boolean givePKCS12ErrorMsg(String msg)
    {
        CBUtility.error(msg);
        return false;
    }


    /**
     *    A representation of a certificate that is displayed
     *    in the certificate list.
     */

    public static class CertItem
    {
        public String alias;

        public X509Certificate x509Cert = null;

        public boolean hasPrivateKey = false;

        /**
         *    Initialises a certitem with the alias name of a
         *    certificate only (the actual cert can be added
         *    seperately)
         *    @param certAlias the alias of the certificate, the arbitrary user assigned
         *           name the certificate is labelled by in the keystore.
         */

        public CertItem(String certAlias)
        {
            this(certAlias, null);
        }

        /**
         *    Initialises a certItem with the alias name of a
         *    certificate and the actual certificate data.
         *    @param certAlias the alias of the certificate, the arbitrary user assigned
         *           name the certificate is labelled by in the keystore.
         *    @param cert the actual X509 Certificate data.
         */

        public CertItem(String certAlias, X509Certificate cert)
        {
            alias = certAlias;
            x509Cert = cert;
        }


        /**
         *    Adds (or Replaces) the X509Cert data.
         *    @param x the actual X509 Certificate data.
         */

        public void addX509Cert(X509Certificate x)
        {
            x509Cert = x;
        }

        /**
         *    Returns a formatted string identifying the cert by the alias.
         *    @return the alias assigned to this cert.
         */

        public String toString()
        {
            if (hasPrivateKey)
                return "<html><b><font color=black>" + alias + "</font><br><font color=blue>(has private key)</font></b></html>";
            else
                return alias;
        }

        /**
         *    Returns a formatted string identifying the cert by the alias.
         *    @return the alias assigned to this cert.
         */

        public String getSelectedText()
        {
            if (hasPrivateKey)
                return "<html><b><font color=white>" + alias + "</font><br><font color=white>(has private key)</font></b></html>";
            else
                return alias;
        }

        /**
         *    returns the raw alias for this cert.
         *    @return the alias assigned to this cert.
         */

        public String getAlias()
        {
            return alias;
        }

        /**
         *    Returns an image representing this CertItem.
         */

        public ImageIcon getIcon()
        {
            if (hasPrivateKey)
                return smallKeyCert;
            else
                return smallCert;
        }

        /**
         *    Returns the X509 certificate data (may be null if this hasn't
         *    been set).
         *    @return the X509 certificate stored in this CertItem
         */

        public X509Certificate getX509Cert()
        {
            return x509Cert;
        }

        public void setHasPrivateKey(boolean state)
        {
            hasPrivateKey = state;
        }

        public boolean getHasPrivateKey()
        {
            return hasPrivateKey;
        }
    }


    /**
     *    A quicky cell renderer to allow us to display active and pending
     *    queries in different colours, and to display the text of a
     *    QueryBroker object.
     */

    class CertificateListRenderer extends JLabel implements ListCellRenderer
    {
        Color highlight = new Color(0,0,128);  // Colour of 'selected' text

        CertificateListRenderer()
        {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus)
        {
            if (value instanceof CertItem == false)  // should never happen :-)
            {
                System.err.println("Rendering error in KeystoreGUI");
                setText(ERRORCERT);
                return this;
            }

            if (index == -1)
            {
                index = list.getSelectedIndex();
                if (index == -1)
                {
                    setText("<error>");
                    return this;
                }
            }

            if (value == null)    // shouldn't happen
            {
                setBackground(Color.white);
                setForeground(Color.gray);
                setText("<deleted>");
                return this;
            }

            CertItem item = (CertItem)value;

            setIcon(item.getIcon());

            if (isSelected)
            {
                setText(item.getSelectedText());
                setBackground(highlight);
                setForeground(Color.white);
            }
            else
            {
                setText(item.toString());
                setBackground(Color.white);
                setForeground(Color.black);
            }
            return this;
        }
    }

    public ImageIcon getImageIcon(String name)
    {
        try
        {
            String path = properties.getProperty("dir.images") + name;
            File imageFile = new File(path);
            if (imageFile.exists())
            {
                ImageIcon newIcon = new ImageIcon(path);
                    return newIcon;
            }
         }
         catch (Exception e) {} // ignore; try load via class loader mechanism

        System.out.println("debug: trying to load jar image " + name + " from: " + this.getClass().getResource(name));

        try
        {
            return new ImageIcon(this.getClass().getResource("/" + name));
        }
        catch (Exception e)
        {
            System.out.println("Error loading images; " + name + " not found: " + e.getMessage());
        }
        return null;
    }


    private static void printUsageAndExit()
    {
        System.out.println("USAGE: java KeystoreGUI [keystore file|path] [keystore password] [keystore type] [provider]\n" +
                          "(defaults are 'security/clientcerts' and 'jks'");
        System.exit(0);
    }

    /**
     *    Main method for stand alone usage and provider testing.
     */

    public static void main(String[] argsv)
    {
        String keystoreType = "jks";

        String provider = null;

        String password = null;

        Frame rootFrame = new Frame();

        CBUtility.initDefaultDisplay(rootFrame);

        // stand alone demo...

        System.out.println("running KeystoreGUI 1.0 stand alone - Chris Betts 2002 / Santthosh Babu Selvadurai 2007\n");

        String localDir = System.getProperty("user.dir") + File.separator;

        Properties props = new Properties();

        /*
         *    This sets the directory that the file browsers start in.
         *    This can be saved/read from file to allow the user to start
         *    loading/saving from the same place.
         */

        props.setProperty("cert.homeDir", localDir + "certs" + File.separator);

        /*
         *    This simply sets the directory where the GUI will try to load
         *    its button images from.
         */

        props.setProperty("dir.images", localDir + "images" + File.separator);

        /*
         *   Set the location of the java keystore to manipulate.
         */

        String keystoreName = localDir + "security" + File.separator + "clientcerts";

        /*-SmartKeytool Utility- Commented for graphical packaging
        if (argsv.length < 1)
            printUsageAndExit();

        if (argsv[0].startsWith("-h"))
            printUsageAndExit();

        if (argsv[0].length() < 2)
        {
            keystoreName = argsv[0];
        }
        else if (argsv[0].charAt(1) == ':' || argsv[0].charAt(0) == '/')  // absolute path
        {
            keystoreName = argsv[0];
        }
        else    // assume relative path.
        {
            keystoreName = localDir + argsv[0];
        }

        /*
         *    Keystore Password


        if (argsv.length > 1)
        {
            password = argsv[1];
        }

        /*
         *    Read optional keystore type (e.g. "KSE")


        if (argsv.length > 2)
        {
            keystoreType = argsv[2];
        }

        /*
         *    Read optional provider (e.g. com.ca.pki.security.provider.KeyStoreEngine )

        *-SmartKeytool Utility- Commented for graphical packaging*/

        /*-SmartKeytool Utility- Commented for graphical packaging
         *
         * Standalone repackaging done by Santthosh Babu Selvadurai
         * sbselvad@ncsu.edu/santthosh@ieee.org
         *
         */
        javax.swing.UIManager ui = new javax.swing.UIManager();
        try
        {
            ui.setLookAndFeel(ui.getSystemLookAndFeelClassName());
            JFrame tempFrame = new JFrame();
            KeystorePrompt kp = new KeystorePrompt(tempFrame);
            kp.setSize(300,200);
            keystoreName = kp.getKeystorePath();
            password = kp.getKeystorePassword();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.exit(0);
        }

        if (argsv.length > 3)
        {
            provider = argsv[3];

            // register the new provider
            try
            {
                Class providerClass = Class.forName(provider);
                Provider providerObject = (Provider)providerClass.newInstance();
                Security.insertProviderAt(providerObject, 1);
                System.out.println("\nPROVIDER: " + providerObject.getName() + " v" + providerObject.getVersion() + " has been registered ");
            }
            catch (Exception e)
            {
                System.err.println("\n*** unable to load new security provider: " + ((provider==null)?"null":provider));
                System.err.println(e + "\n");
                printUsageAndExit();
            }
        }

        Provider[] current = Security.getProviders();
        for (int i=0; i<current.length; i++)
            System.out.println("registered security providers: " + i + " = " + current[i].getName() + " " + current[i].getInfo());

        // Extend KeystoreGUI to add 'exit on close' behaviour

        class StandaloneKeystore extends KeystoreGUI
        {
            public StandaloneKeystore( Frame owner, Properties props, String keyStoreLocation, char[] pwd, String keystoreType, String title, boolean handlePrivateKeys, String helpTopic)
            {
                super(owner, props, keyStoreLocation, pwd, keystoreType, title, handlePrivateKeys, helpTopic, true);
                this.setResizable(false);
                this.pack();
            }

            public void doOK()
            {
                super.doOK();
                System.exit(0);
            }

            public void doCancel()
            {
                super.doCancel();
                System.exit(0);
            }
        }

        char[] pwd = null; //((password==null)?null:password.toCharArray());

        StandaloneKeystore gui = new StandaloneKeystore(rootFrame, props, keystoreName, pwd, keystoreType, "SmartKeytool 1.0", true, null);

        gui.setSize(450,440);

        CBUtility.center(gui, null);

        gui.setVisible(true);
    }


}
