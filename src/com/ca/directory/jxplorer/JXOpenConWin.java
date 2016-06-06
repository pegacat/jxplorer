package com.ca.directory.jxplorer;

import com.ca.commons.cbutil.*;
import com.ca.commons.jndi.ConnectionData;
import com.ca.commons.naming.CBOpenConWin;
import com.ca.directory.jxplorer.broker.*;
import com.ca.directory.jxplorer.broker.DataQuery;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JXOpenConWin allows the user to open an ldap connection.  The jndiBroker
 * that the connection is opened through is attached to a tree display, and
 * a separate schema tree display.  An attribute viewer is linked with the
 * main tree display.  A JLabel allows the connection window to communicate
 * status to the user.  These parameters are all obtained directly from the
 * main JXplorer object
 *
 * @author Trudi
 */
public class JXOpenConWin extends CBOpenConWin implements DataListener
{
    private final static Logger log = Logger.getLogger(JXOpenConWin.class.getName());

    // a list of passwords used this session, to allow for reconnection and to save users re-entering them.
    private final static HashMap cachedps = new HashMap(20);

    protected JXplorerBrowser browser;
    protected JNDIDataBroker jndiBroker;
    protected JTextField dsmlService;
    /**
     * Constant used to add 'DSML' option to combo box.
     */
    protected static final String DSMLV2 = CBIntText.get("DSML v2");

    /**
     * <p>JXOpenConWin allows the user to open an ldap connection.  The jndiBroker
     * that the connection is opened through is attached to a tree display, and
     * a separate schema tree display.  An attribute viewer is linked with the
     * main tree display.  A JLabel allows the connection window to communicate
     * status to the user.  These parameters are all obtained directly from the
     * main JXplorer object</p>
     * <p/>
     *
     * @param jx            a JXplorer object to obtain trees, data brokers and gui hooks from.
     * @param statusDisplay a label used to give status feedback to the user.
     * @param clientcerts   the client certificate keystore (optional if 'simple ssl' is used).
     * @param cacerts       the trusted server certificate keystore (required for ssl)
     * @param referral      the jndi referral handling method ("follow" is default).
     * @param aliasType     the jndi alias handling - whether aliases are searched or not.
     *                      (default is "searching");
     */
    public JXOpenConWin(JXplorerBrowser jx, JLabel statusDisplay, String clientcerts, String cacerts,
                        String referral, String aliasType)
    {
        super(jx, JXplorer.APPLICATION_NAME, statusDisplay, clientcerts, cacerts, referral, aliasType, HelpIDs.CONNECT);

        browser = jx;
        if (browser != null && browser.getJndiBroker() != null && newCon != null) // possibly null in batch mode
            newCon.tracing = browser.getJndiBroker().getTracing();

        addPasswordHandlingListener();
    }

    /**
     * This is a fairly brutal piece of code to allow us to use the cached passwords when
     * the user selects a new template.  We blow away the two existing Action Listeners
     * (one from CBSaveLoadTemplate, to load() the template, the other from CBOpenConWin,
     * to check the security level) and add our own, which does the just mentioned tasks
     * as well as setting the cached password (if any).  Unfortunately we can't just
     * append a third action listener to do this, as we don't seem to be able to
     * guarantee the order in which action listeners run.
     */
    private void addPasswordHandlingListener()
    {
        // this is the actual combo box that users select template names from
        CBJComboBox templateSelector = myTemplater.getLoadComboBox();

        // clear all existing action listeners (should be just two)
        ActionListener[] listeners = templateSelector.getActionListeners();
        for (int i = 0; i < listeners.length; i++)
            templateSelector.removeActionListener((ActionListener) listeners[i]);

        // add a new action listener to load template, check security level, and
        // insert cached password.
        templateSelector.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                myTemplater.load();
                checkSecurityLevel();
                loadPasswordHandling();
            }
        });

        // when the 'save' button is pressed, we have to save the password as well or
        // the post save process will clear it (?).  For whatever reason, in this case
        // we don't seem to need to clear the other action listeners...

        CBButton save = myTemplater.getSaveButton();

        save.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                savePasswordHandling();
            }
        });
    }

    /**
     * @param statusDisplay
     */
    protected void initGUI(String applicationName, JLabel statusDisplay)
    {
        super.initGUI(applicationName, statusDisplay);
        initSpecificGUI();
    }

    protected void initSpecificGUI()
    {
        // XXX I think we can just make DSML an option always can't we?  - CB '11
        // make 'dsml available' the default, but allow it to be turned off for light weight distributions...
        //if (JXConfig.getProperty("dsml") == null ||
        //        JXConfig.getProperty("dsml").equalsIgnoreCase("false") == false)
        version.addItem(DSMLV2);

        setReadOnlyHandling();

        myTemplater.loadDefault();  // needs to be redone after 'dsml' added to property list

        loadPasswordHandling();

        display.validate();
    }

    protected void setReadOnlyHandling()
    {
        if (JXConfig.getProperty("lock.read.only").equals("true"))
        {
            readOnly.setSelected(true);
            readOnly.setEnabled(false);
            readOnlyLabel.setEnabled(false);
        }
    }

    /**
     * During the time JXplorer is active, we cache the password for different connection setups. The
     * cache is lost when JX is shut down.
     */
    protected void loadPasswordHandling()
    {
        if (!JXConfig.getProperty("jxplorer.cache.passwords").equals("true"))
            return;

        // if we have a blank template, there's no point in proceeding
        if (hostName.getText().trim() == "")
            return;

        int key = makePwdKey();

        if (cachedps.containsKey(key))
        {
            String p = (String) cachedps.get(key);
            password.setText(p);
        }
    }

    /**
     * Cache the password used in case we want to reconnect later.  The cache is lost when JX is shut down.
     */
    protected void savePasswordHandling()
    {

        if (!JXConfig.getProperty("jxplorer.cache.passwords").equals("true"))
            return;

        int key = makePwdKey();
        cachedps.put(key, new String(password.getPassword()));
    }

    /**
     * We mash the unique connection details together into a unique key so we can recognise the connection
     * again and reproduce the required password.
     *
     * @return the password key
     */
    private int makePwdKey()
    {
        // try to get a unique signature for the connection box by concatenating everything except the password...
        String key = new StringBuffer(50).append(hostName.getText()).append(port.getText()).append(baseDN.getText()).append(managerDN.getText()).append(version.getSelectedItem().toString()).append(level.getSelectedIndex()).toString();
        return key.hashCode();
    }

    /**
     * This method overrides the CBOpenConWin.addExtraComponent method to insert
     * the DSML URL label and text field.
     */
    public void addExtraComponent()
    {
        JLabel urlLabel;
        display.makeLight();
        display.add(urlLabel = new JLabel("   " + CBIntText.get("DSML Service") + ": "), 0, 2, 1, 1);
        display.addWide(dsmlService = new JTextField("", 30), 4);
        urlLabel.setToolTipText(CBIntText.get("The DSML service; e.g.") + " 'dsml/services/DSML?ldapHost=localhost&ldapPort=19289'");

        //version.addItem(DSMLV2);
        VersionActionListener versionListener = new VersionActionListener();
        version.addActionListener(versionListener);
    }

    /**
     * Implements ActionListener to enable or disable the
     * DSML text field depending on protocol. If either LDAP V2 or V3
     * is selected...the text field will be disabled.  And the security levels -
     * if DSML is selected the security combo is disabled.
     */
    class VersionActionListener implements ActionListener
    {

        /**
         * Enables/disables the DSML text field depending on protocol.
         * And the security levels.
         */
        public void actionPerformed(ActionEvent event)
        {
            if (!version.getSelectedItem().equals(DSMLV2))
            {
                dsmlService.setEnabled(false);
                dsmlService.setText("");
                dsmlService.setBackground(Color.lightGray);

                // stuff around getting our normal options back
                if (level.getItemCount() < securityOptions.length)
                {
                    for (int i=level.getItemCount(); i<securityOptions.length; i++)
                        level.addItem(securityOptions[i]);
                }

                level.setEnabled(true);
                checkSecurityLevel();
            }
            else
            {
                dsmlService.setEnabled(true);
                dsmlService.setBackground(Color.white);

                // stuff around restricting security options to the ones we handle (currently anonymous and usr/pwd)
                if (level.getSelectedIndex()>1)
                    level.setSelectedIndex(1); // enabling security for DSML...

                for (int i=level.getItemCount()-1; i>1; i--)
                    level.removeItemAt(i);

                level.setEnabled(true);

                checkSecurityLevel();

            }
        }
    }

    /**
     * Over-ride base 'doOK()' method to allow for setting the new 'DSML' protocol if selected...
     */
    public void doOK()
    {
        if (version.getSelectedItem().equals(DSMLV2))
            newCon.protocol = ConnectionData.DSML;
        else
            newCon.protocol = ConnectionData.LDAP;

        addExtraEnvironmentProperties();

        savePasswordHandling();

        super.doOK();

    }

    private void addExtraEnvironmentProperties()
    {
        Properties props = JXConfig.getMyProperties();
        Enumeration keys = props.keys();
        while (keys.hasMoreElements())
        {
            String key = (String) keys.nextElement();
            // at the moment, we'll only add sun ldap and security env variables; there might be others we need later...
            if (key.startsWith("Context")
                    || key.startsWith("context")
                    || key.startsWith("com.sun.jndi.ldap") // more generic than com.sun.jndi.ldap.connect
                    || key.startsWith("java.security") // more generic than javax.security.sasl.qop
                    || key.startsWith("javax.security")) // more generic than javax.security.sasl.qop
            {
                newCon.putExtraProperty(key, props.getProperty(key));
            }
        }
    }

    /**
     * This resets some volitile parameters that might have changed
     * since the last time the user accessed the window.
     *
     * @param newclientcerts the client certificate keystore (optional if 'simple ssl' is used).
     * @param newcacerts     the trusted server certificate keystore (required for ssl)
     * @param newreferral    the jndi referral handling method ("follow" is default).
     * @param newaliasType   the jndi alias handling - whether aliases are searched or not.
     *                       (default is "searching");
     */
    public void reinit(String newclientcerts, String newcacerts,
                       String newreferral, String newaliasType)
    {
        newCon.clientcerts = newclientcerts;
        newCon.cacerts = newcacerts;
        newCon.referralType = newreferral;
        newCon.aliasType = newaliasType;

        if (jndiBroker == null)    //TE: bug 3222.
            newCon.tracing = browser.getJndiBroker().getTracing();
        else
            newCon.tracing = jndiBroker.getTracing();
    }

    /**
     * Sets title back to 'open ldap connection'/clears password...
     * but sets the title to 'Open LDAP/DSML Connection".
     */
    public void resetTitleAndPassword()
    {
        this.setTitle(CBIntText.get("Open LDAP/DSML Connection"));

        if (!JXConfig.getProperty("jxplorer.cache.passwords").equals("true"))
            password.setText("");
    }

    /**
     * This method is called when the user connection data
     * has been gathered and (roughly) checked.
     *
     * @param connectData the parsed connection data containing
     *                    host and port details, security info, etc.
     */
    public void connect(ConnectionData connectData)
    {
        // Queue the request on the broker's request list.  When completed, the request
        // will trigger the 'DataReady()' method below.

        try
        {
            jndiBroker = browser.getJndiBroker();

            // clear the GUI preparatory to the new data appearing.
            browser.preConnectionSetup();

            // create a new data query and put it on the query stack for the connection thread.
            com.ca.directory.jxplorer.broker.DataQuery query = jndiBroker.connect(connectData);

            query.addDataListener(new SwingDataListener(this));

        }
                // the code above just sets up the connection call, and doesn't really
                // do anything that should cause an exception - the connection is
                // done in a different thread, and any exceptions should be caught
                // by the 'DataReady()' code below.

        catch (Exception e)
        {
            log.log(Level.WARNING, "Unexpected exception in JXOpenConWin.connect", e);
            e.printStackTrace();
        }
    }

    /**
     * This method is called when a new connection attempt is finished.
     * If successfull, the main, search and schema trees are updated
     * with theire data sources, and a successfull connection message
     * displayed to the user.  If unsuccessful, an error message is displayed.
     *
     * @param request the connection request data object.
     */

    //XXX remember that this method is being run by the directory connection thread -
    //XXX so it can use unthreaded jndiBroker directory methods with impunity.
    public void dataReady(DataQuery request)
    {
        if (!(request instanceof JNDIDataBroker.DataConnectionQuery))
        {
            log.warning("Incorrect data for connection - cannot connect");
            return;
        }

        if (request.hasException() == false)  // apparently we have a valid connection to play with!
        {
            if (browser.postConnectionSetup((JNDIDataBroker.DataConnectionQuery) request))
            {
                setVisible(false);

                ((JNDIDataBroker.DataConnectionQuery) request).conData.clearPasswords();

                dispose();
            }
            else
            {
                browser.disconnect();
            }
        }
        else    // request registered an error in jdniBroker
        {
            // do we want a nice error box?  I think we do!
            new CBErrorWin(this, "Error opening connection:\n" + request.getException().getMessage(), request.getException()); // automatically visible one-shot.

            log.log(Level.WARNING, "Error opening connection ", request.getException());
            request.clearException();
            setTitle(CBIntText.get("Couldn't Connect: Try Again"));
            dispose();	//TE: don't remove this...for some reason if an incorrect port etc is entered on Linux - JX crashes!
            setVisible(true);
            userMessage.setText(CBIntText.get("Couldn't Open") + " " + request.getExtendedData("url"));
            browser.disconnect();
            request.squelch();  // we're done here.
        }
    }

    /**
     * overload the getURL method of the base class to use the new url field.
     *
     * @return the correct, parsed URL.
     */
    protected String getURL() throws NumberFormatException, URISyntaxException
    {
        String url = super.getURL();
        if (version.getSelectedItem().equals(DSMLV2)) // dsml may require an extra 'service' bit to add to the url...
        {
            String dsml = dsmlService.getText();
            if (dsml.startsWith("/"))               // trim any starting slash - we add it back just below.
                dsml = dsml.substring(1);

            if (url.startsWith("ldap://"))          // trim the ldap: protocol prefix - this is usually set by CBOpenConWin...
                url = url.substring(7);

            url = "http://" + url + "/" + dsml;     // construct the full dsml url...
        }
        log.fine("connecting with url: " + url);
        return url;
    }
}