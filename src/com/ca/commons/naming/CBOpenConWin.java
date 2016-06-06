package com.ca.commons.naming;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URISyntaxException;
import java.util.logging.Logger;


import javax.swing.*;
import javax.swing.border.TitledBorder;

import com.ca.commons.jndi.ConnectionData;
import com.ca.commons.cbutil.*;

/**
 * CBOpenConWin displays connection info to the user, allowing them
 * to open a connection to an ldap server.
 */


// Programming note: this class is a bit of an interloper; but it requires classes from both com.ca.commons.cbutil and
// com.ca.commons.jndi, so it get stuck in naming...

public abstract class CBOpenConWin extends CBDialog
{
    /**
     * stores the users connection data for passing to the 'connect()' method.
     */

    protected ConnectionData newCon; //ConnectionData newCon;

    protected JTextField hostName, port, baseDN, managerDN;
    protected CBJComboBox version, level;
    protected JCheckBox readOnly;                // whether to open a read only connection.
    protected JPasswordField password;
    protected JLabel hostLabel, portLabel, userMessage, readOnlyLabel;
    protected CBSaveLoadTemplate myTemplater;

    protected CBPanel securityPanel;

    static int threadID = 1;             // naming variable for thread debugging
    static final boolean DEBUGTHREADS = false;

    // Security Levels ...
    static final int NONE = 0;
    static final int USER_AUTH = 1;
    static final int SSL_NO_AUTH = 2;
    static final int SSL_USER_AUTH = 3;
    static final int SASL = 4;
    static final int GSSAPI = 5;	// Vadim: GSSAPI

    // Protocols + Version strings for combo selection box
    protected static final String LDAPV2 = CBIntText.get("LDAP v2");
    protected static final String LDAPV3 = CBIntText.get("LDAP v3");


    protected SecurityActionListener securityListener;

    private final static Logger log = Logger.getLogger(CBOpenConWin.class.getName());

    protected String[] securityOptions = {
            CBIntText.get("Anonymous"),
            CBIntText.get("User + Password"),
            CBIntText.get("SSL + Anonymous"),
            CBIntText.get("SSL + User + Password"),
            CBIntText.get("SSL + SASL + Keystore Password"),
            CBIntText.get("GSSAPI") // Vadim: GSSAPI
        };

    /**
     * <p>CBOpenConWin allows the user to open an ldap connection.  The jndiBroker
     * that the connection is opened through is attached to a tree display, and
     * a separate schema tree display.  An attribute viewer is linked with the
     * main tree display.  A JLabel allows the connection window to communicate
     * status to the user.  These parameters are all obtained directly from the
     * main JXplorer object</p>
     * <p/>
     *
     * @param owner         the owning frame (used for look and feel propagation etc.)
     * @param applicationName the name of the application (used to find the config file on some systems)
     * @param statusDisplay a label used to give status feedback to the user.
     * @param clientcerts   the client certificate keystore (optional if 'simple ssl' is used).
     * @param cacerts       the trusted server certificate keystore (required for ssl)
     * @param referral      the jndi referral handling method ("follow" is default).
     * @param aliasType     the jndi alias handling - whether aliases are searched or not.
     *                      (default is "searching");
     * @deprecated use constructor that takes Help ID.
     */

    public CBOpenConWin(Frame owner, String applicationName, JLabel statusDisplay, String clientcerts, String cacerts,
                        String referral, String aliasType)
    {
        this(owner, applicationName, statusDisplay, clientcerts, cacerts, referral, aliasType, null);
    }


    /**
     * <p>CBOpenConWin allows the user to open an ldap connection.  The jndiBroker
     * that the connection is opened through is attached to a tree display, and
     * a separate schema tree display.  An attribute viewer is linked with the
     * main tree display.  A JLabel allows the connection window to communicate
     * status to the user.  These parameters are all obtained directly from the
     * main JXplorer object</p>
     * <p/>
     *
     * @param owner         the owning frame (used for look and feel propagation etc.)
     * @param applicationName the name of the application (used to find the config file on some systems)
     * @param statusDisplay a label used to give status feedback to the user.
     * @param clientcerts   the client certificate keystore (optional if 'simple ssl' is used).
     * @param cacerts       the trusted server certificate keystore (required for ssl)
     * @param referral      the jndi referral handling method ("follow" is default).
     * @param aliasType     the jndi alias handling - whether aliases are searched or not.
     *                      (default is "searching");
     *  @param helpID the help ID to attach to the Help button.
     */
    public CBOpenConWin(Frame owner, String applicationName, JLabel statusDisplay, String clientcerts, String cacerts,
                        String referral, String aliasType, String helpID)
    {
        this(owner, applicationName, statusDisplay, clientcerts, cacerts, helpID);

        newCon.referralType = referral;
        newCon.aliasType = aliasType;
    }


    /**
     * <p>CBOpenConWin allows the user to open an ldap connection.  The jndiBroker
     * that the connection is opened through is attached to a tree display, and
     * a separate schema tree display.  An attribute viewer is linked with the
     * main tree display.  A JLabel allows the connection window to communicate
     * status to the user.  These parameters are all obtained directly from the
     * main JXplorer object.</p>
     * <p/>
     * <p/>
     * <p>This constructor allows for setting up ssl connections, by
     * passing details of keystores through.</p>
     *
     * @param owner         the owning frame (used for look and feel propagation etc.)
     * @param applicationName the name of the application (used to find the config file on some systems)
     * @param statusDisplay a label used to give status feedback to the user.
     * @param clientcerts   the client certificate keystore (optional if 'simple ssl' is used).
     * @param cacerts       the trusted server certificate keystore (required for ssl)
     * @deprecated use constructor that takes Help ID.
     */

    public CBOpenConWin(Frame owner, String applicationName, JLabel statusDisplay, String clientcerts, String cacerts)
    {
        this(owner, applicationName, statusDisplay, clientcerts, cacerts, null);
    }

    /**
     * <p>CBOpenConWin allows the user to open an ldap connection.  The jndiBroker
     * that the connection is opened through is attached to a tree display, and
     * a separate schema tree display.  An attribute viewer is linked with the
     * main tree display.  A JLabel allows the connection window to communicate
     * status to the user.  These parameters are all obtained directly from the
     * main JXplorer object.</p>
     * <p/>
     * <p/>
     * <p>This constructor allows for setting up ssl connections, by
     * passing details of keystores through.</p>
     *
     * @param owner           the owning frame (used for look and feel propagation etc.)
     * @param applicationName the name of the application (used to find the config file on some systems)
     * @param statusDisplay   a label used to give status feedback to the user.
     * @param clientcerts     the client certificate keystore (optional if 'simple ssl' is used).
     * @param cacerts         the trusted server certificate keystore (required for ssl)
     * @param helpID          the help ID to attach to the Help button.
     */
    public CBOpenConWin(Frame owner, String applicationName, JLabel statusDisplay, String clientcerts, String cacerts, String helpID)
    {
        super(owner, CBIntText.get("Open LDAP connection"), helpID); // create modal dialog ...

        newCon = new ConnectionData();//ConnectionData();

        newCon.clientcerts = clientcerts;
        newCon.cacerts = cacerts;

        initGUI(applicationName, statusDisplay);
    }

    protected void initGUI(String applicationName, JLabel statusDisplay)
    {
        String oldConnection = "";
        int oldPortNo = 389;
        String oldBaseDN = "";
        //String oldUseSSL = CBIntText.get("false");

        userMessage = statusDisplay;


        /**
         *   Host / port and ldap version details
         */

        display.makeHeavy();
        JLabel temp;
        display.add(hostLabel = new JLabel("   " + CBIntText.get("Host") + ": "));
        display.addWide(hostName = new JTextField(oldConnection, 30), 2);
        hostLabel.setToolTipText(CBIntText.get("The url of the server; e.g.") + " 'www.cai.com'");

        display.add(portLabel = new JLabel("  " + CBIntText.get("Port") + ": "));
        display.addLine(port = new JTextField(String.valueOf(oldPortNo), 5));
        portLabel.setToolTipText(CBIntText.get("The port number of the LDAP server; often 389."));

        display.add(temp = new JLabel("   " + CBIntText.get("Protocol") + (": ")));
        display.add(version = new CBJComboBox(new String[]{LDAPV2, LDAPV3}));
        version.setSelectedIndex(1);
        temp.setToolTipText(CBIntText.get("For all but the oldest servers, this should be 'LDAP v3'."));
        version.setToolTipText(CBIntText.get("For all but the oldest servers, this should be 'LDAP v3'."));


        display.add(new JLabel(""));
        display.newLine();  //TE: hack to add space for an extra component to be added in...ie. the DSML URL field...DO NOT REMOVE (see addExtraComponent).

        /**
         *    Optional Panel for base DN.
         */

        CBPanel inset = new CBPanel();
        inset.setBorder(new TitledBorder(CBIntText.get("Optional Values")));

        inset.add(temp = new JLabel(CBIntText.get("Base DN") + ":         "));
        temp.setToolTipText(CBIntText.get("The base to start browsing from; e.g.") + " 'o=Democorp,c=au'");
        inset.makeWide();
        inset.addln(baseDN = new JTextField(String.valueOf(oldBaseDN), 30));
        inset.makeLight();
        inset.add(new JLabel(""));
        inset.addGreedyWide(new JLabel(""));
        inset.add(readOnlyLabel = new JLabel("  " + CBIntText.get("Read Only") + ": "));
        inset.addLine(readOnly = new JCheckBox()); // padding


        display.addLines(inset, 1);


        /*
         *    Security Panel for anonymous vs user/password vs SASL
         */

        securityPanel = new CBPanel();



        securityPanel.setBorder(new TitledBorder(CBIntText.get("Security")));

        securityPanel.makeLight();
        securityPanel.add(temp = new JLabel(CBIntText.get("Level") + ":"));
        temp.setToolTipText(CBIntText.get("The level of authentication."));

        level = new CBJComboBox(securityOptions);
        level.setToolTipText(CBIntText.get("Before using SSL, make sure you've set up your keystores in the 'Security' menu."));
        securityPanel.addln(level);

        securityPanel.add(temp = new JLabel(CBIntText.get("User DN") + ":  "));
        securityPanel.addln(managerDN = new JTextField(30));
        temp.setToolTipText(CBIntText.get("To log on as an authenticated user, enter your user dn here."));

        securityPanel.add(temp = new JLabel(CBIntText.get("Password") + ":   "));
        securityPanel.addLine(password = new JPasswordField(30));
        temp.setToolTipText(CBIntText.get("Set your user password (or SASL keystore password) here."));

        display.addLines(securityPanel, 3);

        OK.setToolTipText(CBIntText.get("Click here to connect using current settings."));

        /*
         * ca.commons.cbutil.CBSaveLoadTemplate is a fairly cunning component (see)
         * that allows the user to save and restore the state of edit fields in a dialog
         */

        myTemplater = getNewCBSaveLoadTemplate(applicationName, "connections.txt"); //TODO: make this a constant?

        display.addWide(myTemplater, 5);

        addExtraComponent();            //TE: allows the user to insert a component and not mess up the template handling.

        display.newLine();

        display.add(new JLabel(""));  // padding

        display.doLayout();

        getContentPane().add(display);

        doLayout();

        myTemplater.loadDefault();

        checkSecurityLevel();

        /*
         *    Add a listener that checks the security level (and hence
         *    which fields are greyed out) everytime something changes
         *    which might affect stuff.
         */

        securityListener = new SecurityActionListener();
        level.addActionListener(securityListener);
        //TE: get the combo box that has the names of the templates to load and add an action listener to it...
        (myTemplater.getLoadComboBox()).addActionListener(securityListener);
    }

    protected CBSaveLoadTemplate getNewCBSaveLoadTemplate(String applicationName, String templateName)
    {
        return new CBSaveLoadTemplate(applicationName, templateName);
    }

    /**
     * Use this method to insert an extra component.  It is intended for
     * adding the DSML label and field under protocol version.  Hopefully
     * there has been a space provided (via display.newLine()) in the
     * initGUI method.  So use something like
     * display.add(urlLabel = new JLabel("   DSML: "), 0,2,1,1);
     * display.addWide(dsmlService = new JTextField("", 30),4);
     * in the overriding method.
     * NOTE: the reason for not just sticking this method call after adding
     * the protocol stuff is so that we don't stuff up the save/load template
     * coordinates that users may have previously saved.  I.e if we did it this
     * way, the rest of the components that are added after the DSML stuff
     * wont have the saved data loaded.  Using display.newLine() acts as
     * a place holder.
     */

    public void addExtraComponent()
    {
    }


    /**
     * Implements ActionListener to call the checkSecurityLevel method.
     *
     * @author Trudi.
     */

    class SecurityActionListener implements ActionListener
    {

        /**
         * Calls the checkSecurityLevel method.
         */

        public void actionPerformed(ActionEvent event)
        {
            checkSecurityLevel();
        }
    }


    /**
     * this simply checks the state of the security level combo box,
     * and grays out components accordingly.
     */

    protected int checkSecurityLevel()
    {

        int selected = level.getSelectedIndex();
        switch (selected)
        {
            case 0:  // anonymous

                setState(false, false);
                return NONE;

            case 1:  // user + password

                setState(true, true);
                return USER_AUTH;

            case 2:  // ssl + anonymous

                //XXX Big Dirty Hack - use password for non-JKS keystores...
                /*                                
                String caKeystoreType     = JXConfig.getProperty("keystoreType.cacerts", "JKS");  
                if ("JKS".equals(caKeystoreType) == false)
                    setState(false, true);                    // XXX HACK XXX
                else
                */
                
                setState(false, false);
                return SSL_NO_AUTH;

            case 3:  // ssl + user + password

                setState(true, true);
                return SSL_USER_AUTH;

            case 4:  // ssl + sasl + password

                setState(false, true);
                return SASL;

            case 5:  // Vadim: GSSAPI

                setState(false, false);
                return GSSAPI;
        }

        return NONE;
    }

    /**
     * Small utility ftn to handle graying out components at the same time
     * as disabling them.  (real graphics toolkits do this for you...)
     */

    private void setState(boolean user, boolean pwd)
    {
        managerDN.setEnabled(user);
        managerDN.setBackground(user ? Color.white : Color.lightGray);

        if (pwd == false)
            password.setText("");

        password.setEnabled(pwd);
        password.setBackground(pwd ? Color.white : Color.lightGray);
    }


    /**
     * Set's title back to 'open ldap connection'/clears password
     */

    public void resetTitleAndPassword()
    {
        this.setTitle(CBIntText.get("Open LDAP Connection"));
        password.setText("");
    }

    protected String getURL()
            throws NumberFormatException, URISyntaxException
    {
        String host = null;
        String portString = null;
        if (hostName != null)
            host = hostName.getText();
        if (port != null)
            portString = port.getText();

        if (host != null) host = host.trim();
        if (portString != null) portString = portString.trim();

        if (host == null || host.length() < 1)
        {
            throw new URISyntaxException("", CBIntText.get("A host name must be entered for JXplorer to connect to."));
        }

        if (portString == null || portString.length() < 1)
        {
            throw new URISyntaxException("", CBIntText.get("A port number must be entered for JXplorer to connect to."));
        }

        int port = Integer.parseInt(portString); // may throw exception

        if (port < 0) throw new NumberFormatException(CBIntText.get("Negative Port Number is illegal"));

        if (port > 65536) throw new NumberFormatException(CBIntText.get("Port Number {0} is illegal", new String[] {portString}));

        return "ldap://" + host + ":" + port;
    }

    /**
     * This assembles a connection data object from the UI fields filled in by the user.
     * In a pinch, this method can be used to read off the saved connection details
     * from a user template, without actually showing a UI.
     * @return
     * @throws Exception
     */
    public ConnectionData getConnectionData()
            throws Exception
    {
        return getConnectionData(null);
    }

    /**
     * Allows an external password to be supplied
     * @return
     * @throws Exception
     */
    public ConnectionData getConnectionData(char[] defaultPwd)
            throws Exception
    {

        // XXX TODO: this is kinda weird - this line blew away the setup of ssl info and broke SSL, why did I stick it in, and when?
        //ConnectionData connection = new ConnectionData();
        
        ConnectionData connection = newCon;


        log.fine("read values: " + hostName.getText() + ":" + port.getText());

        /*
         *    Read Host and Port
         */

        String url = getURL();  // throws exceptions if URL is bad.

        connection.setURL(url);

        if (userMessage != null) // possibly null in batch mode
            userMessage.setText(CBIntText.get("Opening Connection To") + " " + url);

        /*
         *   ldap version number
         */

        if (version.getSelectedItem() == LDAPV2)
            connection.version = 2;
        else // default for both ldap and dsml
            connection.version = 3;

        /*
         *    Security Magic
         */

        int securityLevel = checkSecurityLevel();

        connection.userDN = null;
        connection.clearPasswords();

        connection.useGSSAPI = false;

        if (securityLevel == USER_AUTH || securityLevel == SSL_USER_AUTH)
        {
            connection.userDN = managerDN.getText().trim();

            connection.pwd = (defaultPwd==null)?password.getPassword():defaultPwd;
            if ((connection.pwd.length) == 0)
            {					//TE: make sure the user has entered a password.
                throw new Exception(CBIntText.get("No Password Provided.  Please enter a password."));
            }
        }
        else if (securityLevel == SASL)
        {
            connection.clientKeystorePwd = (defaultPwd==null)?password.getPassword():defaultPwd;

            if ((connection.clientKeystorePwd.length) == 0)
            {				//TE: make sure the user has entered a password.
                throw new Exception(CBIntText.get("No Password Provided.  Please enter a password."));
            }
        }
        //Vadim: GSSAPI
        else if (securityLevel == GSSAPI)
        {
            // username & password are only used if an existing kerberos keystore cannot be found;
            // we'll prompt the user for them elsewhere if neccessary.

            connection.useGSSAPI = true;
        }

        setVisible(false);

        connection.useSSL = (securityLevel >= SSL_NO_AUTH && securityLevel != GSSAPI);

        connection.baseDN = baseDN.getText();

        connection.templateName = myTemplater.getCurrentTemplateName();

        connection.readOnly = readOnly.isSelected();

        return connection;
    }
        
    /**
     * Over-ride base class method that is called when the OK button is hit.
     */

    public void doOK()
    {
        try
        {
            newCon = getConnectionData();
        }
        catch (Exception err)
        {   // a bunch of things may throw exceptions; at this stage we haven't tried
            // to contact the directory, so just reset defaults and carry on...

            new CBErrorWin(this, "Error in data provided: "  + err.getMessage(), err); // automatically visible one-shot.

            err.printStackTrace();

            password.setText("");
            setVisible(true);

            this.setTitle(CBIntText.get("Couldn't Connect: Try Again"));
            log.warning("User error in openconwin: " + err);
            userMessage.setText(CBIntText.get("Error Opening Connection."));

            return;
        }

        // DO THE ACTUAL WORK!
        // Now the data has been read, send it off to the connect method to make the connection.

        connect(newCon);
    }

    /**
     * This method is called when the user connection data
     * has been gathered and (roughly) checked.
     *
     * @param connectData the parsed connection data containing
     *                    host and port details, security info, etc.
     */

    public abstract void connect(ConnectionData connectData);
}