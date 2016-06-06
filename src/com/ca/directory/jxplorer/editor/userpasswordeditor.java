/**
 *    All pluggable editors must be in this package
 */

package com.ca.directory.jxplorer.editor;

import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.HelpIDs;
import com.ca.directory.jxplorer.JXConfig;

import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.MessageDigest;

import com.ca.commons.cbutil.CBBase64;
import com.ca.directory.jxplorer.jcrypt;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class userpasswordeditor extends JDialog
        implements abstractbinaryeditor
{
    /**
     * Test main() method to show dialog pane during development
     * @param args
     */
    public static boolean testMode = false;


/*

The Good Word is ==> helloworld

userPassword: {SHA}at+xg6SiyUovktq1redipHiJpaE=
userPassword: {MD5}/F4DjTilcDIIVEHn/nAQsA==
userPassword: {crypt}0pnSC65.QhkYc

CRYPT

$ htpasswd -nbd myName myPassword
myName:rqXexS6ZhobKA
 */


    /**
     * Screen for manual testing and general password tool...
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {
        testMode = true;
        userpasswordeditor editor = new userpasswordeditor(new JFrame());

        editablebinary val = new editablebinary()
        {
            byte[] internalVal;
            @Override
            public void setValue(byte[] bytes)
            {
                internalVal = bytes;
            }

            @Override
            public byte[] getValue()
            {
                return internalVal;
            }
        } ;

        // run through some quick warmups to make sure nothing it too broken

        test(editor, MD5, "helloworld", null, "/F4DjTilcDIIVEHn/nAQsA==");
        test(editor, SHA, "helloworld", null, "at+xg6SiyUovktq1redipHiJpaE=");
        test(editor, SHA256, "helloworld", null, "k2oYXKqiZrucvpgengXLeM1zKwsygOuURBK7b4+PB68=");
        test(editor, SHA512, "helloworld", null, "FZQkTVLy2MErFCu2H0e8Lq9QPW2cqEgMrp/PES9m5JZ9xej6mCheNtuK8bj/qLhMsV4PvPg2w964A8E/N2WaYA==");
        test(editor, SSHA, "helloworld", "salt".getBytes(), "1PnwSyeUJukjsnji/SovzCYMBds=");
        test(editor, SSHA256, "helloworld", "salt".getBytes(), "HG7GmnaWzuKF9gfz7XbDVCKQE2SGUN+YEIFcI1MtbAM=");
        test(editor, SSHA512, "helloworld", "salt".getBytes(), "0i9XGPFL8HeVsqJ8KKlMxemjfecq7SUqzsXeLfXYM6Y5DcMSDjynJwGWoZM6dqjsELDdVpF9bl2e4AWPK2/MYA==");
        test(editor, SSHA512, "mypwd", "randomString".getBytes(), "5QxZCiM/zcn0/upHX2uw6ICbgE+PLa9sJz/UpfMAMe1isyxuv+NeW4k4GjRDoTQHnB5QjCKCydJJjUQnT3DEEQ==");

        test(editor, CRYPT, "helloworld", "0p".getBytes(), "0pnSC65.QhkYc");
        test(editor, CRYPT, "myPassword", "rq".getBytes(), "rqXexS6ZhobKA");



        // SSHA-512 password: 'mypwd' salt: 'randomString'
        val.setValue(new String("{SSHA512}zesq+BgumcP/xx3mQ4YtAY15blOTGAo6tzjsyBdRnADNaN9Y+y7UkA4vBiacMB09fPMnuNYSb9eLlVTR1usNJFpEt4fCiyl10Ugj").getBytes("UTF-8"));
        editor.editMe = val;
        editor.showAdvancedUI(true);
        editor.setVisible(true);
    }

    private static void test(userpasswordeditor editor, String algorithm, String pwd, byte[] salt, String expectedResult)
    {
        try
        {
            byte[] hash = editor.generateRawPasswordHash(algorithm, pwd, salt);

            System.out.println("TESTING: " + algorithm);
            if (algorithm.equals(CRYPT))
            {
                if (!expectedResult.equals(new String(hash)))
                {
                    System.out.println("Test Error running " + algorithm + " algorithm; got: " + new String(hash) + " expected: " + expectedResult);
                    System.exit(-1);
                }
            }
            else
            {

                if (!expectedResult.equals(Base64.encode(hash)))
                {
                    System.out.println("Test Error running " + algorithm + " algorithm; got hash (b64): " + Base64.encode(hash) + " expected (b64): " + expectedResult);
                    System.exit(-1);
                }
            }
        }
        catch (NoSuchAlgorithmException e)
        {
            System.out.println("ERROR: UNSUPPORTED ALGORITHM: " + algorithm);
            System.exit(-1);
        }
    }

    /**
     *  DIALOG BOX OPTIONS - Supported Algorithms
     *
     *  To add a new algorithm
     *  1. add a constant 'name' below (must be the same as that used in the message digest *and* by LDAP value... will need
     *         extra handling if this is not aligned; fortunately MD5 and SHA appear to be aligned like this,
     *         HOWEVER SSHA-512 etc. are not - java uses a '-' in the name, while LDAP does not (e.g. 'SHA-512' = '{SHA512}')
     *  2. add it to the String[] of known algorithms
     *  3. Add the constant 'name' to the dialog box
     *  4. Add to 'getHashLengthInBytes()' method
     *  5. Add handling to password Verify
     *  6. Add handling to
     */
    
    /**
     * Verify existing password only
     */
    public static final String VERIFY = "Verify";

    /**
     * Don't encrypt - use plaintext
     */
    public static final String PLAINTEXT = "plain text";

    /**
     * CRYPT.
     */
    public static final String CRYPT = "CRYPT - Obsolete";


    /**
     * MD5.
     */
    public static final String MD5 = "MD5";

    /**
     * SHA.
     */
    public static final String SHA = "SHA";

    /**
     * SSHA.
     */
    public static final String SSHA = "SSHA";

    /**
     * SMD5.
     */
    public static final String SMD5 = "SMD5";

    /**
     * SHA-256.
     */
    public static final String SHA256 = "SHA-256";

    /**
     * SSHA-256.
     */
    public static final String SSHA256 = "SSHA-256";

    /**
     * SHA-512.
     */
    public static final String SHA512 = "SHA-512";

    /**
     * SSHA-512.
     */
    public static final String SSHA512 = "SSHA-512";

    /**
     * array of all supported algorithms.
     */

    public static final String[] supportedAlgorithms = new String[] {PLAINTEXT, CRYPT, MD5, SHA, SSHA, SMD5, SHA256, SSHA256, SHA512, SSHA512};

    protected CBPanel basicOptions, advancedOptions;

    protected JTextField pwdField, pwdConfirm, advanced_pwd, salt_ascii, salt_base64, base64result, ldapValueResult, ldifValueResult ;
    protected CBButton btnOK, btnCancel, btnHelp, clear, basic, advanced, test;
    protected editablebinary editMe = null;
    protected CBPanel display;
    protected JLabel pwdLabel, pwdConfirmLabel;
    protected CBJComboBox comboType, advancedAlgSelection;
    protected boolean firstClick = true;
    protected boolean hidingPasswords = true;


    protected static int default_encryption = 4;

    protected static boolean defaultToShowAdvancedUI = false;  // remember what the user used last time.
    
    private static Logger log = Logger.getLogger(userpasswordeditor.class.getName());

    /**
     * Constructor - sets up the gui.
     */
    public userpasswordeditor(Frame owner)
    {
        super(owner);

        setModal(true);
        setTitle(CBIntText.get("User Password"));

        display = new CBPanel();

        if (JXConfig.getProperty("mask.raw.passwords", "true").equalsIgnoreCase("false"))
        {
            pwdField = new JTextField();
            pwdConfirm = new JTextField();
            hidingPasswords = false;

        }
        else
        {
            pwdField = new JPasswordField();
            pwdConfirm = new JPasswordField();
        }

        pwdField.setBackground(Color.white); // For a better motif display
        pwdField.addMouseListener(new MouseListener()
        {
            public void mouseClicked(MouseEvent e) {}

            public void mouseEntered(MouseEvent e) {}

            public void mouseExited(MouseEvent e) {}

            public void mouseReleased(MouseEvent e) {}

            public void mousePressed(MouseEvent e)
            {
                if (firstClick)     // Only clear the field on the first click b/c if the pwd exists it will be encoded.
                {
                    if (hidingPasswords)
                        pwdField.setText("");
                    firstClick = false;
                }
            }
        });

        pwdConfirm.setBackground(Color.white); // For a better motif display

        pwdLabel = new JLabel(CBIntText.get("Enter Password")+":");
        pwdConfirmLabel = new JLabel(CBIntText.get("Re-enter Password")+":");

        btnOK = new CBButton(CBIntText.get("OK"), CBIntText.get("Click here to save the changes (remember to click Submit in the table editor)."));
        btnOK.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                load();
            }
        });

        btnCancel = new CBButton(CBIntText.get("Cancel"), CBIntText.get("Click here to exit."));
        btnCancel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                quit();
            }
        });

        comboType = new CBJComboBox();
        comboType.addItem(VERIFY);
        for (String algorithm: supportedAlgorithms)
        comboType.addItem(algorithm);

        comboType.setEditable(false);
        comboType.setSelectedIndex(default_encryption);
        comboType.addItemListener(new ItemListener()
        {
            // If "verify" is selected, the pwdConfirm Password-field will be disabled.
            public void itemStateChanged(ItemEvent e)
            {
                if (comboType.getSelectedItem().equals(VERIFY))
                    setNewPwdFieldEnabled(false);
                else
                    setNewPwdFieldEnabled(true);
            }
        });

        comboType.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                default_encryption = comboType.getSelectedIndex();
                //if (default_encryption>0)
                //    advancedAlgSelection.setSelectedIndex(default_encryption-1);
            }
        });


        btnHelp = new CBButton(CBIntText.get("Help"), CBIntText.get("Click here for Help."));
        CBHelpSystem.useDefaultHelp(btnHelp, HelpIDs.ATTR_PASSWORD);


        display.makeHeavy();

        basicOptions = new CBPanel();

        basicOptions.setBorder(new TitledBorder(CBIntText.get("Password Editor")));

        basicOptions.addln(pwdLabel);
        basicOptions.addln(pwdField);
        basicOptions.addln(pwdConfirmLabel);
        basicOptions.addln(pwdConfirm);
        basicOptions.add(comboType);

        basicOptions.add(advanced = new CBButton(CBIntText.get("Advanced Editor"), CBIntText.get("Go to the advanced password editor")));

        advanced.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                showAdvancedUI(true);
            }
        });

        advancedOptions = new CBPanel();
        advancedOptions.makeHeavy();
        advancedOptions.setBorder(new TitledBorder(CBIntText.get("Advanced Password Editor")));

        advancedOptions.addln(new JLabel(CBIntText.get("Enter Password") + ":"));
        advancedOptions.addln(advanced_pwd = new JTextField(100));
        advancedOptions.addln(new JLabel(CBIntText.get("salt (ascii)") + ":"));
        advancedOptions.addln(salt_ascii = new JTextField(100));
        advancedOptions.addln(new JLabel(CBIntText.get("salt (base64)") + ":"));
        advancedOptions.addln(salt_base64 = new JTextField(100));
        advancedOptions.addln(new JLabel(CBIntText.get("base64 hash value:") + ":"));
        advancedOptions.addln(base64result= new JTextField(200));
        advancedOptions.addln(new JLabel(CBIntText.get("ldap value") + ":"));
        advancedOptions.addln(ldapValueResult = new JTextField(""));
        advancedOptions.addln(new JLabel(CBIntText.get("ldif value") + ":"));
        advancedOptions.addln(ldifValueResult = new JTextField(200));

        advancedAlgSelection = new CBJComboBox();
        for (String algorithm: supportedAlgorithms)
            advancedAlgSelection.addItem(algorithm);

        advancedAlgSelection.setEditable(false);
        advancedAlgSelection.setSelectedIndex(default_encryption - 1);

        advancedAlgSelection.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                default_encryption = advancedAlgSelection.getSelectedIndex()+1;
                //comboType.setSelectedIndex(default_encryption);
            }
        });

        advancedOptions.add(advancedAlgSelection);
        advancedOptions.add(test = new CBButton(CBIntText.get("Test"), CBIntText.get("Test generation of password and optional salt before sending to the directory")));
        advancedOptions.add(clear = new CBButton(CBIntText.get("Clear"), CBIntText.get("Clear all fields")));
//        advancedOptions.add(new JLabel(" "));
//        advancedOptions.add(new JLabel(" "));
        advancedOptions.addln(basic = new CBButton(CBIntText.get("Basic Editor"), CBIntText.get("Return to the basic password editor")));

        clear.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                clearAdvancedTextFields();
            }
        });

        basic.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                showAdvancedUI(false);
            }
        });

        test.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                testAdvancedUIForConsistentData();
            }
        });


//        advancedOptions.setVisible(false);
        display.addln(basicOptions);
        display.addln(advancedOptions);



        display.makeLight();
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnOK);
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnHelp);
        display.addln(buttonPanel);

        // Better way to implement keystroke listening...
        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "enter");
        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
        display.getActionMap().put("enter", new MyAction(CBAction.ENTER));
        display.getActionMap().put("escape", new MyAction(CBAction.ESCAPE));

//        setSize(300, 170);
        setSize(450, 600);
        CBUtility.center(this, owner);      // Centres the window.
        setTitle(CBIntText.get("User Password Data"));

        display.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        getContentPane().add(display);


        // If "verify" is selected, the pwdConfirm Password-field will be
        // disabled.
        if (comboType.getSelectedItem().equals(VERIFY))
            setNewPwdFieldEnabled(false);
        else
            setNewPwdFieldEnabled(true);

        showAdvancedUI(defaultToShowAdvancedUI);

    }

    protected void clearAdvancedTextFields()
    {
        advanced_pwd.setText("");
        salt_ascii.setText("");
        salt_base64.setText("");
        base64result.setText("");
        ldapValueResult.setText("");
        ldifValueResult .setText("");
    }

    /**
     * Enables or disables the pwdConfirm password field.
     * @param enabled True to enable the pwdConfirm password field and false to disable the pwdConfirm password field.
     */
    protected void setNewPwdFieldEnabled(boolean enabled)
    {
        pwdConfirm.setFocusable(enabled);
        pwdConfirm.setEnabled(enabled);
        pwdConfirmLabel.setEnabled(enabled);

        if (enabled)
            pwdConfirm.setBackground(Color.white); // For a better motif display
        else
            pwdConfirm.setBackground(Color.lightGray);
    }

    /**
     * Apparently it is better to use key bindings rather than adding a KeyListener... "for reacting in a special way to
     * particular keys, you usually should use key bindings instead of a key listener". This class lets the user set the
     * key as an int.  If a key is pressed and it matches the assigned int, a check is done for if it is an escape or
     * enter key. (27 or 10).  If escape, the quit method is called.  If enter, the apply method is called. Bug 4646.
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
         * quit is called if the Esc key pressed, load is called if Enter key is pressed.
         * @param e never used.
         */
        public void actionPerformed(ActionEvent e)
        {
            if (getKey() == ESCAPE)
                quit();
            else if (getKey() == ENTER)
                load();
        }
    }

    /**
     * This is the AbstractBinaryEditor interface method which is called when the user wants to edit the password
     */
    public void setValue(editablebinary editMe)
    {
        this.editMe = editMe;
        String pwdString = getStringFromBytes(editMe.getValue());
        pwdField.setText(pwdString);
        try
        {
            setAdvancedUIFromLdapValue(pwdString);
        }
        catch (NoSuchAlgorithmException e)
        {
            new CBErrorWin(this, CBIntText.get("unknown algorithm in pwd value:  " + pwdString + "\n" + e.getMessage()), e);
        }


    }





    /**
     * returns if the algorithm uses a salt (e.g. is SMD5 or one of the SSHA family)
     *
     * @param algorithm
     * @return
     */
    protected boolean usesSalt(String algorithm)
    {
        return (algorithm.startsWith("SSHA") || algorithm.equals("SMD5") || algorithm.equals(CRYPT));
    }

    /**
     * Calculates the pwd hash to be stored in the userPassword field, as generated from the basic UI.
     * @param algorithm The encryption scheme - the SHA and SSHA family + MD5, SMD5 and CRYPT
     * @param password The password in plaintext that should be hashed.
     * @param salt The salt that is to be used together with the schemes {SMD5} and {SSHA}. Should be between 8 and 16
     * Bytes. salt should be null for any other scheme.
     * @return The (usually, except for {CRYPT}) base64-encoded hashed pwd with the following format: - {MD5}base64(MD5-hash) for MD5 hashes -
     *          {CRYPT}salt+hash
     *         {SHA}base64(SHA-hash) for SHA hashes - {SMD5}base64(MD5-hash+salt bytes) for SMD5 hashes -
     *         {SSHA}base64(SHA-hash+salt bytes) for SSHA hashes Or null if unknown algorithm
     */
    protected byte[] getLDAPValueBytes(String algorithm, String password, byte[] salt)
    {
        if (algorithm.equals(VERIFY))
            return getBytesFromString(password);

        try
        {
            if ( usesSalt(algorithm) && (salt==null || salt.length == 0))
                throw (new InvalidParameterException("algorithm requires salt"));

            String ldapValue = getLdapValue(algorithm, password, salt);

            return ldapValue.getBytes("UTF-8");
        }
        catch (InvalidParameterException e)
        {
            new CBErrorWin(this, CBIntText.get("password salt required"), e);
            return new byte[0];
        }
        catch (UnsupportedEncodingException e)
        {
            new CBErrorWin(this, CBIntText.get("Unexpected error encoding password") +" :\n"+e.getMessage(), e);
            log.log(Level.WARNING, "Unexpected error encoding password ", e);
            e.printStackTrace();
            return new byte[0];
        }
        catch (java.security.NoSuchAlgorithmException e)
        {
            new CBErrorWin(this, CBIntText.get("Unsupported Alogrithm exception") + ":\n"+e.getMessage(), e);
            log.log(Level.WARNING, "Unsupported Alogrithm exception", e);
            e.printStackTrace();
            return new byte[0];
        }
    }

    protected String getLdapValue(String algorithm, String password, byte[] salt) throws NoSuchAlgorithmException
    {
        byte[] buff = generateRawPasswordHash(algorithm, password, salt);

        // buff now contains the hashed password...
        return constructLdapValue(algorithm, salt, buff);


    }

    private String constructLdapValue(String algorithm, byte[] salt, byte[] passwordHash)
    {
        // To be able to restore the salt it needs to be available at decoding time; the standard LDAP way is
        // to append it to the resulting hash; eg. {SMD5|SSHA}base64(h+salt)
        // So we now need to append the salt to the hashed pwd

        StringBuffer ldapValue = new StringBuffer();

        ldapValue.append(getLDAPAlgorithmName(algorithm));

        if (algorithm.equals(CRYPT))  // special - salt already in 'passwordHash'
        {
            ldapValue.append(new String(passwordHash));
        }
        else if (salt != null)
        {
            // copy the existing password hash into a buffer
            byte[] new_buf = new byte[passwordHash.length + salt.length];
            for (int x = 0; x < passwordHash.length; x++)
                new_buf[x] = passwordHash[x];

            // now copy the salt into the end of that buffer
            for (int x = passwordHash.length; x < new_buf.length; x++)
                new_buf[x] = salt[x - passwordHash.length];

            // New_buf now contains the 16(MD5), resp. 20(SHA1) hash
            // bytes and the salt - we can now base64 encode this and add it to the ldap value
            ldapValue.append(CBBase64.toBase64(new_buf));
        }
        else // the easy case; no salt!
        {
            ldapValue.append(CBBase64.binaryToString(passwordHash));
        }
        return ldapValue.toString(); // returns a string of the type {SHA}asfewSGBbs21cd...
    }

    protected byte[] generateRawPasswordHash(String algorithm, String password, byte[] salt) throws NoSuchAlgorithmException
    {
        // special handling for legacy and insecure 'crypt' algorithm - (still appears in some ancient directories)

        if (algorithm.equals(CRYPT))
        {
            return jcrypt.crypt(password, (salt==null)?null:new String(salt)).getBytes();
        }

        MessageDigest md;
        // the message digest doesn't explicitly do salting - we do that ourselves... so we need to translate our algorithm names
        // to trim off the leading 'S' for the salted versions... e.g. SMD5 -> MD5, SSHA-512 -> SHA-512.

        String messageDigestAlgorithmName = usesSalt(algorithm)?algorithm.substring(1):algorithm;

        md = MessageDigest.getInstance(messageDigestAlgorithmName);

        md.reset();
        md.update(getBytesFromString(password));


        if (salt != null)
        {
            // The way the salted hashes work is the following:
            // h=HASH(pwd+salt)
            //
            // So, lets append the salt to the pwd-buffer in md.
            md.update(salt);
        }

        // Calculate the hash-value of s+salt
        return md.digest();
    }


    /**
     * Converts a string to bytes using UTF-8 encoding.
     * ... just so we don't have exception handling everywhere
     *
     * @param s
     */
    protected byte[] getBytesFromString(String s)
    {
        try
        {
            return s.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e)  // this should never happen - things would have to be pretty messed up for this to trigger...
        {
            log.log(Level.WARNING, "Unexpected error encoding password ", e);
            e.printStackTrace();
            return new byte[0];
        }
    }
    /**
     * Converts between a byte array and text
     * ... just so we don't have exception handling everywhere
     */
    protected String getStringFromBytes(byte[] b)
    {
        if (b == null || b.length == 0)
        {
            return new String();
        }
        else
        {
            try
            {
                return new String(b, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                log.log(Level.WARNING, "Unexpected error decoding password ", e);
                e.printStackTrace();
                return new String(b);  // Fall back on the platform default... not sure this is the best thing to do - CB
            }
        }
    }

    protected void setAdvancedUIFromLdapValue(String ldapValue) throws NoSuchAlgorithmException
    {
        ldapValueResult.setText(ldapValue);
        ldifValueResult .setText("userPassword: " + CBBase64.toBase64(editMe.getValue()));

        String algorithm = getJavaAlgorithmName(ldapValue);
        byte[] hash = getPasswordHashFromLdapValue(algorithm, ldapValue);
        byte[] salt = getSalt(algorithm, ldapValue);

        salt_base64.setText(CBBase64.toBase64(salt));
        base64result.setText(CBBase64.toBase64(hash));

        if (isPrintableAscii(salt))
            salt_ascii.setText(new String(salt));

        advancedAlgSelection.setSelectedItem(algorithm);
    }
    /**
     * Allows the user to test their data before submitting it to the directory
     *
     * @return true is a valid, consistent data set has been entered, or false if there is inconsistent data
     */
    protected boolean testAdvancedUIForConsistentData()
    {
        String algorithm = advancedAlgSelection.getSelectedItem().toString();

        String newPassword = advanced_pwd.getText();

        byte[] salt = null;

        if (newPassword.trim().length()==0) // the user hasn't entered a password; see if they've entered another value
        {
            try
            {
                // the user has entered a raw ldap value
                if (ldapValueResult.getText().length() > 0)
                {
                    setAdvancedUIFromLdapValue(ldapValueResult.getText());
                }
                // the user has entered a password hash and (possibly) a salt
                else if (base64result.getText().length() > 0)
                {
                    byte[] pwdHash = CBBase64.decode(base64result.getText().trim());

                    if (pwdHash.length != getHashLengthInBytes(algorithm))
                        throw new NoSuchAlgorithmException("hash length (" + pwdHash.length + ") does not match length of " + algorithm + " algorithm (" + getHashLengthInBytes(algorithm) + ")");
                    if (usesSalt(algorithm))
                    {
                        salt = getSalt(algorithm);
                        if (salt==null)
                            throw new NoSuchAlgorithmException("algorithm " + algorithm + " requires a salt");
                    }

                    String ldapValue = constructLdapValue(algorithm, salt, pwdHash);
                    ldapValueResult.setText(ldapValue);
                    ldifValueResult.setText("userpassword: " + CBBase64.toBase64(ldapValue.getBytes("UTF-8")));
                }
                // the user has entered a raw ldif value
                else if (ldifValueResult.getText().length()>0)
                {
                    String ldifData = ldifValueResult.getText().substring(14);
                    String ldapValue = new String(CBBase64.decode(ldifData), "UTF-8");
                    setAdvancedUIFromLdapValue(ldapValue);
                }
            }
            catch (Exception e)
            {
                new CBErrorWin(this, CBIntText.get("error parsing directly entered values:\n" + e.getMessage()), e);
                return false;
            }
            return true;
        }

        try
        {

            if (usesSalt(algorithm))
            {
                salt = getSalt(algorithm);
                if (salt == null)       // generate random salt
                {
                    salt = getRandomSalt(algorithm);
                    salt_base64.setText(CBBase64.toBase64(salt));
                }
            }
            else
            {
                salt_ascii.setText("");
                salt_base64.setText("");
            }

            // special handling for plain text passwords
            if (algorithm.equals(PLAINTEXT))
            {
                advanced_pwd.setText(newPassword);
                ldapValueResult.setText(newPassword);
                base64result.setText("");
                ldifValueResult.setText("userpassword: " + CBBase64.toBase64(newPassword.getBytes("UTF-8")));
                return true;
            }

            byte[] pwdHash = generateRawPasswordHash(algorithm, newPassword, salt);

            base64result.setText(CBBase64.toBase64(pwdHash));

            String ldapValue = getLdapValue(algorithm, newPassword, salt);
            ldapValueResult.setText(ldapValue);

            ldifValueResult.setText("userpassword: " + CBBase64.toBase64(ldapValue.getBytes("UTF-8")));
            return true;
        }
        catch (UnsupportedEncodingException e)
        {
            //f**king java.
        }
        catch (CBBase64EncodingException e)
        {
            new CBErrorWin(this, CBIntText.get("error parsing base64 encoded salt:\n" + salt_base64.getText()), e);
        }
        catch (NoSuchAlgorithmException e)
        {
            new CBErrorWin(this, CBIntText.get("unexpected error working with algorithm type {" + algorithm + "} :\n" + salt_base64.getText()), e);
        }
        return false;
    }

    private byte[] getSalt(String algorithm) throws UnsupportedEncodingException, CBBase64EncodingException
    {
        byte[] salt = null;
        if (salt_ascii.getText().length() > 1)
        {
            salt = salt_ascii.getText().getBytes("UTF-8");
            salt_base64.setText(CBBase64.toBase64(salt));
        }
        else if (salt_base64.getText().length() > 1)
        {
            salt = CBBase64.decode(salt_base64.getText());
            if (isPrintableAscii(salt))
                salt_ascii.setText(new String(salt));
        }

        return salt;
    }

    public static boolean isPrintableAscii(byte[] salt)
    {
        if (salt == null)
            return false;

        for (byte b:salt)
            if (b < 32 && b > 126)
                return false;

        return true;
    }

    protected void displayAdvancedData()
    {
        if (editMe == null || editMe.getValue()==null)
            return;

        String oldPwdValue = getStringFromBytes(editMe.getValue());

        try
        {
            setAdvancedUIFromLdapValue(oldPwdValue);
        }
        catch (NoSuchAlgorithmException e)
        {
            log.warning("unsupported password algorithm encountered. " + e.getMessage());
        }

    }


    /**
     * Verifies the given pwd if "verify" is selected or sets the value of the EditableBinary object with whatever the
     * user has entered into the password text field.
     */
    protected void load()
    {
        if (testMode)
            System.exit(0);

        if (basicOptions.isVisible())
        {
            // "verify" selected, so we just verify the given pwd
            if (comboType.getSelectedItem().equals(VERIFY))
            {
                // newPasswordText is the plaintext pwd the user has entered and
                // oldPasswordLDAPValue is the hashed value of the userPassword field.
                String newPasswordText = pwdField.getText();
                String oldPasswordLDAPValue = getStringFromBytes(editMe.getValue());

                try
                {
                    if (passwordVerify(oldPasswordLDAPValue, newPasswordText))
                        JOptionPane.showMessageDialog(display, CBIntText.get("Password verified."), CBIntText.get("Password Verification."), JOptionPane.INFORMATION_MESSAGE);
                    else
                        JOptionPane.showMessageDialog(display, CBIntText.get("Password not verified."), CBIntText.get("Password Verification."), JOptionPane.INFORMATION_MESSAGE);
                }
                catch (NoSuchAlgorithmException e)
                {
                    new CBErrorWin(this, CBIntText.get("unknown password algorithm"), e);
                }
            }
            // Otherwise create the hashed userPassword
            else if (passwordConfirm())
            {
                byte[] salt = null;
                String algorithm = comboType.getSelectedItem().toString();

                if (usesSalt(algorithm))
                {
                    salt = getRandomSalt(algorithm);
                }

                // Set the new userPassword value and quit()
                editMe.setValue(getLDAPValueBytes(algorithm, pwdConfirm.getText(), salt));
                quit();
            }
        }
        else // use the Advanced Option UI instead
        {
            if (testAdvancedUIForConsistentData())  // this checks if the data is consistent, and informs the user if it is not
            {
                    editMe.setValue(getBytesFromString(ldapValueResult.getText()));
                    quit();
            }

        }
    }

    protected byte[] getRandomSalt(String algorithm)
    {
        byte[] salt = null;

        if (algorithm.equals(CRYPT))  // special handling for CRYPT... again...
        {
            return getBytesFromString(jcrypt.randomString(2));
        }

        try
        {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            salt = new byte[getHashLengthInBytes(algorithm)];
            random.nextBytes(salt);
        }
        catch (java.security.NoSuchAlgorithmException e)
        {
            log.log(Level.WARNING, "Unexpected error encoding password ", e);
            e.printStackTrace();
        }
        return salt;
    }

    /**
     * This method translates a java algorithm name (e.g. "SSHA-256") to the LDAP algorithm name used
     * in the LDAP value (e.g. "{SSHA256}".
     * @param algorithm
     * @return
     */
    protected String getLDAPAlgorithmName(String algorithm)
    {
        if (algorithm.equals(CRYPT)) // special handling for crypt
            return "{crypt}";

        return "{" + algorithm.replace("-", "") + "}";
    }

    /**
     * This method extracts the algorithm type from an ldap string of the type {algorithm}base64text
     * (e.g. {SSHA512}afc324D432... ) returns SSHA-512.
     *
     * Note: this translates between LDAP algorithm names and java algorithm names when necessary -
     * for example later SHA standard uses a '-' for the java algorithm name, but no dash in LDAP...
     *
     * Note that 'CRYPT' is not a standard java algorithm, and is handled separately with the 'jcrypt' class.
     *
     * @param userPasswordValue
     * @return
     */
    protected String getJavaAlgorithmName(String userPasswordValue) throws java.security.NoSuchAlgorithmException
    {
        if (!userPasswordValue.startsWith("{"))
            return PLAINTEXT;

        String algorithm = "unable to parse hash: " + userPasswordValue;

        try
        {
            algorithm = userPasswordValue.substring(1, userPasswordValue.indexOf('}'));

            if (algorithm.toLowerCase().equals("crypt")) // special handling for crypt
                return CRYPT;


            for (String supportedAlgorithm : supportedAlgorithms)
                if (algorithm.equalsIgnoreCase(supportedAlgorithm))
                    return supportedAlgorithm;

            // try again, inserting a '-' in the algorithm after any possible 'SHA' or 'SSHA' value

            algorithm = algorithm.replace("SHA", "SHA-");
            for (String supportedAlgorithm : supportedAlgorithms)
                if (algorithm.equalsIgnoreCase(supportedAlgorithm))
                    return supportedAlgorithm;

        }
        catch (Exception e)  // catches index exceptions etc...
        {}

        throw new NoSuchAlgorithmException("unsupported hash algorithm: " + algorithm);
    }

    /**
     * This gets the length of the encryption algorithm in *bytes*
     * @param algorithm
     * @return
     */
    protected int getHashLengthInBytes(String algorithm) throws NoSuchAlgorithmException
    {
        if (algorithm.equals(CRYPT))
            return 7;
        else if (algorithm.equals(MD5) || algorithm.equals(SMD5))
            return 16;
        else if (algorithm.equals(SHA) || algorithm.equals(SSHA))
            return 20;
        else if (algorithm.equals(SHA256) || algorithm.equals(SSHA256))
            return 32;
        else if (algorithm.equals(SHA512) || algorithm.equals(SSHA512))
            return 64;
        else throw new NoSuchAlgorithmException("algorithm {" + algorithm + "} is unsupported");


    }



    protected byte[] getPasswordHashFromLdapValue(String algorithm, String ldapPasswordValue) throws NoSuchAlgorithmException
    {
        if (ldapPasswordValue != null && ldapPasswordValue.length()>0)
        {
            try
            {
                String ldapAlgorithm = getLDAPAlgorithmName(algorithm);

                String encodedSection = ldapPasswordValue.substring(ldapAlgorithm.length()).trim();
                byte[] cryptData = CBBase64.stringToBinary(encodedSection);
                int hashLength = getHashLengthInBytes(algorithm);
                byte[] hash = new byte[hashLength];
                for (int i = 0; i < hashLength; i++)
                    hash[i] = cryptData[i];
                return hash;
            }
            catch (Exception e)
            {
                log.warning("unable to parse ldapPasswordValue: " + ldapPasswordValue + ": " + e.getMessage());
            }
        }
        return null;
    }

    protected byte[] getSalt(String algorithm, String ldapPasswordValue) throws NoSuchAlgorithmException
    {
        if (algorithm.equals(CRYPT))  // special handling for crypt - not base64 encoded, therefore can't use code below
        {
            String salt = ldapPasswordValue.substring(7, 9);  // grab first two bytes after "{crypt}" prefix
            return getBytesFromString(salt);
        }

        if (usesSalt(algorithm)== false)
            return null;

        /**
         * This set of algorithms (currently SMD5 and SSHA...) use a salt; the salt is assumed to be
         * appended to the password hash, so we will need to recover that before we can test the value.
         * To get the salt bytes first we need to base64-decode the pwd hash and store it in cryptData.
         * trim off the {...} piece to get 5QxZCiM/zcn0/upHX2uw6ICbgE+PLa9sJz/UpfMAMe1isyxuv+NeW4k4GjRDoTQHnB5QjCKCydJJjUQnT3DEEXJhbmRvbVN0cmluZw=="
         */
        String ldapAlgorithm = getLDAPAlgorithmName(algorithm);
        byte[] cryptData = CBBase64.stringToBinary(ldapPasswordValue.substring(ldapAlgorithm.length()).trim());

        int hashLength = getHashLengthInBytes(algorithm);

        // The length of the hash is 'hashLength'. To get
        // the size of the salt we need to subtract 'hashLength' Bytes from
        // the total length of the base64-decoded String.
        if (cryptData != null)
        {
            int len = cryptData.length - hashLength;
            if (len > 0)
            {
                // Get the salt Bytes. (E.g. an MD5-Hash takes 16 Bytes - the remaining Bytes will be the salt.)
                byte[] salt = new byte[len];
                for (int x = 0; x < len; x++)
                    salt[x] = cryptData[x + hashLength];
                return salt;
            }
        }
        log.warning("No Salt found for password value: " + ldapPasswordValue);
        return null;  // no salt found... this is often o.k.,
    }



    /**
     * Verifies a given password against the password stored in the userPassword-attribute.
     * <p/>
     * The userPassword-value should follow the following format: - {MD5}base64(MD5-hash) - {SHA}base64(SHA-hash) -
     * {SMD5}base64(MD5-hash+salt bytes) - {SSHA}base64(SHA-hash+salt bytes) - plaintext password
     * <p/>
     * If the userPassword value does not start with one of the prefixes {CRYPT}, {MD5}, {SMD5}, {SHA} or {SSHA} it will be
     * handled as a plaintext pwd.
     * <p/>
     * @param oldPasswordLDAPValue The original pwd as stored in the userPassword attribute.
     * @param newPassword The password in plaintext that should be verified against the hashed pwd stored in the userPassword
     * field.
     * @return True - if the given plaintext pwd matches with the hashed pwd in the userPassword field, otherwise false.
     *
     */
    protected boolean passwordVerify(String oldPasswordLDAPValue, String newPassword) throws NoSuchAlgorithmException
    {
        String algorithm = getJavaAlgorithmName(oldPasswordLDAPValue);

        byte[] salt = null;

        if (usesSalt(algorithm))
            salt = getSalt(algorithm, oldPasswordLDAPValue);

        //byte[] pwdHash = getLDAPValueBytes(algorithm, newPassword, salt);  // note - a 'null' salt is an allowable value for this method

        String newPwdLdapValue = getLdapValue(algorithm, newPassword, salt);

        // Compare the two hashed pwd-Strings and return true if they match, otherwise false.
        if (newPwdLdapValue.equals(oldPasswordLDAPValue))
            return (true);

        return (false);

/*
        if (oPwd.startsWith("{MD5}"))
        {
            // nPwd is the given pwd in cleartext, so
            // we create the MD5 hash for the given pwd
            // and store it again in nPwd.
            nPwd = new String(getLDAPValueBytes(MD5, nPwd, null));
        }
        else if (oPwd.startsWith("{SMD5}"))
        {
            // SMD5 means "Salted MD5". The "salt" is a
            // String of an arbitrary length appended to
            // the given MD5 hash in oPwd.

            // To get the salt bytes first we need to base64-decode
            // the pwd hash and store it in tmp.
            byte[] tmp = CBBase64.stringToBinary(oPwd.substring(6));

            // The length of an MD5-hash is always 16 Bytes. To get
            // the size of the salt we need to subtract 16 Bytes from
            // the total length of the base64-decoded String.
            if (tmp != null)
            {
                int len = tmp.length - 16;
                if (len > 0)
                {
                    // Get the salt Bytes. As the MD5-Hash takes 16 Bytes,
                    // the remaining Bytes will be the salt.
                    byte[] salt = new byte[len];
                    for (int x = 0; x < len; x++)
                        salt[x] = tmp[x + 16];

                    // nPwd is the given pwd in cleartext, so
                    // we create the hash for the given pwd
                    // and store it again in nPwd.
                    nPwd = new String(getLDAPValueBytes(SMD5, nPwd, salt));
                }
            }
        }
        else if (oPwd.startsWith("{SHA}"))
        {
            // nPwd is the given pwd in cleartext, so
            // we create the hash for the given pwd
            // and store it again in nPwd.
            nPwd = new String(getLDAPValueBytes(SHA, nPwd, null));
        }
        else if (oPwd.startsWith("{SSHA}"))
        {
            // SSHA means "Salted SHA-1". The "salt" is a
            // String of an arbitrary length appended to
            // the given SHA-1 hash in oPwd.

            // To get the salt bytes first we need to base64-decode
            // the pwd hash and store it in tmp.
            byte[] tmp = CBBase64.stringToBinary(oPwd.substring(6));

            // The length of an SHA-1-hash is always 20 Bytes. To get
            // the size of the salt we need to subtract 20 Bytes from
            // the total length of the base64-decoded String.
            if (tmp != null)
            {
                int len = tmp.length - 20;
                if (len > 0)
                {
                    // Get the salt Bytes. As the SHA-1-Hash takes 20 Bytes,
                    // the remaining Bytes will be the salt.
                    byte[] salt = new byte[len];
                    for (int x = 0; x < len; x++)
                        salt[x] = tmp[x + 20];

                    // nPwd is the given pwd in cleartext, so
                    // we create the hash for the given pwd
                    // and store it again in nPwd.
                    nPwd = new String(getLDAPValueBytes(SSHA, nPwd, salt));
                }
            }
        }
        else
        {
            // nPwd is already cleartext, no need for
            // hashing it.
            nPwd = new String(getBytesFromString(nPwd));
        }


        // Compare the two hashed pwd-Strings and return true if
        // they match, otherwise false.
        if (nPwd.equals(oPwd))
            return (true);

        return (false);
*/
    }

    /**
     * Does some checks on the password.
     * @return True - if the two password fields match. False - if the new password field is empty (an error message is
     *         displayed). False - if the password fields don't match (an error message is displayed).
     */
    protected boolean passwordConfirm()
    {
        if (pwdConfirm.getText().equals(pwdField.getText()))	// If the two password fields match carry on saving the password.
        {
            return true;
        }
        else if (pwdConfirm.getText().equals(""))	// If the new password field is empty display error message.
        {
            JOptionPane.showMessageDialog(display, CBIntText.get("Empty password field, please fill in both fields"), CBIntText.get("Warning message"), JOptionPane.INFORMATION_MESSAGE);
            pwdConfirm.setText("");
            return false;
        }
        else	// If the password fields don't match display error message.
        {
            JOptionPane.showMessageDialog(display, CBIntText.get("Password typed incorrectly, please try again"), CBIntText.get("Warning message"), JOptionPane.INFORMATION_MESSAGE);
            pwdConfirm.setText("");
            return false;
        }
    }

    /**
     * Shuts down the gui.
     */
    protected void quit()
    {
        setVisible(false);
        dispose();

        if (testMode) System.exit(0);
    }

    /**
     * Switches between advanced and basic password UI
     */
    protected void showAdvancedUI(boolean value)
    {
        defaultToShowAdvancedUI = value;
        advancedOptions.setVisible(value);
        basicOptions.setVisible(!value);

        if (value)
        {
            setSize(new Dimension(600, 450));
            displayAdvancedData();
        }
        else
        {
            setSize(new Dimension(320, 240));
        }

    }

}

