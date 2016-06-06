package com.ca.directory.jxplorer;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import com.ca.commons.cbutil.CBAction;
import com.ca.commons.cbutil.CBButton;
import com.ca.commons.cbutil.CBHelpSystem;
import com.ca.commons.cbutil.CBIntText;
import com.ca.commons.cbutil.CBPanel;
import com.ca.commons.cbutil.CBUtility;
import com.ca.commons.cbutil.Theme;
import com.ca.directory.jxplorer.search.SearchGUI;
import com.ca.directory.jxplorer.tree.SmartTree;

/**
 * Sets up an advanced options dialog box that is accessable through the Options drop down menu.
 * It has five tabbed panes: the first is for the look and feel, the second is for log levels,
 * the third is for the log method, the fourth is for LDAP limit & timeout and the last is for the URL handling.
 * When the user clicks the 'Apply' button all of the changes are saved in the property file and reflected in JX.
 *
 * @author Trudi.
 */
public class AdvancedOptions extends JDialog
{
    private CBButton btnApply, btnReset, btnCancel, btnHelp;
    private JTextField ldapLimit, ldapTimeout, pageSize;
    private JCheckBox pagedResults;
    private JComboBox       urlCombo, logLevelCombo, logMethodCombo, cachePwdCombo;
    private CBPanel display;
    private JTabbedPane tabbedPane;
    private JRadioButton[] lookAndFeel;
    private String[] lookAndFeelVal;

//    private final String[] logLevelVal = new String[]{CBIntText.get("Errors Only"), CBIntText.get("Basic"), CBIntText.get("Tree Operations"), CBIntText.get("Extensive"), CBIntText.get("All"), CBIntText.get("All + BER Trace")};
    private final String[] logLevelVal = new String[]{CBIntText.get("Severe"), CBIntText.get("Warning"), CBIntText.get("Info"), CBIntText.get("Fine"), CBIntText.get("Finest"), CBIntText.get("All + BER Trace")};

    // XXX WARNING - functionality is keyed on the order of elements in the following string array
    private final String[] logMethodVal = new String[]{CBIntText.get("None"), CBIntText.get("Console"), CBIntText.get("File"), CBIntText.get("Console & File")};

    private MainMenu mainMenu;
    private final JXplorerBrowser browser;

    // Utility constants for look and feel stuff..
    private static final int WINDOWS = 0;
    private static final int JAVA = 1;
    protected static final int MOTIF = 2;
    protected static final int GTK = 3;
    protected static final int MAC = 4;

    private static Logger log = Logger.getLogger(AdvancedOptions.class.getName());

    // Look and feels...
    public static final String WINDOWS_LF = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
    public static final String JAVA_LF = "javax.swing.plaf.metal.MetalLookAndFeel";
    public static final String MOTIF_LF = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
    public static final String GTK_LF = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
    //public static final String MAC_LF = "com.sun.java.swing.plaf.mac.MacLookAndFeel";
    public static final String MAC_LF = "apple.laf.AquaLookAndFeel";

    /**
     * Sets up the panel, adds the tabbed pane and the buttons to it.
     *
     * @param jxplorer a JXplorer object to update changes with the log level, log method & LDAP values.
     * @param mainMenu a MainMenu object to update the gui when the L&F is changed.
     */
    public AdvancedOptions(JXplorerBrowser jxplorer, MainMenu mainMenu)
    {
        super(jxplorer);
        setModal(true);

        this.mainMenu = mainMenu;
        browser = jxplorer;

        setTitle(CBIntText.get("JXplorer Advanced Options"));

        display = new CBPanel();

        btnApply = new CBButton(CBIntText.get("Apply"), CBIntText.get("Click here to apply the changes"));
        btnApply.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                apply();
            }
        });

        btnReset = new CBButton(CBIntText.get("Reset"),
                CBIntText.get("Click here to reset the options"));
        btnReset.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                reset();
            }
        });

        btnCancel = new CBButton(CBIntText.get("Cancel"),
                CBIntText.get("Click here to exit Advanced Options"));
        btnCancel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                quit();
            }
        });

        // Creates a new help button with a listener that will open JX help at appropriate location...
        btnHelp = new CBButton(CBIntText.get("Help"), CBIntText.get("Click here for Help"));
        CBHelpSystem.useDefaultHelp(btnHelp, HelpIDs.CONFIG_ADVANCED);

        //TE: better way to implement keystroke listening...
        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "enter");
        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
        display.getActionMap().put("enter", new MyAction(CBAction.ENTER));
        display.getActionMap().put("escape", new MyAction(CBAction.ESCAPE));

        tabbedPane = new JTabbedPane();

        // Set up the ldap levels tab...
        ldapLevels();

        //Set up the L&F tab...
        lookAndFeelTab();

        // Set up the log level tab...
        logLevel();

        // Set up the log method tab...
        logMethod();

        // Set up the URL tab..
        urlTab();
        // Set up the password options tab...
        pwdTab();

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(btnApply);
        buttonPanel.add(btnReset);
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnHelp);
        display.addln(tabbedPane);
        display.addln(buttonPanel);

        setSize(360, 420);

        getContentPane().add(display);
    }

    /**
     * Apparently it is better to use key bindings rather than adding a KeyListener...
     * "for reacting in a special way to particular keys, you usually should use key
     * bindings instead of a key listener".
     * This class lets the user set the key as an int.  If a key is pressed and it
     * matches the assigned int, a check is done for if it is an escape or enter key.
     * (27 or 10).  If escape, the quit method is called.  If enter, the apply
     * method is called.
     * Bug 4646.
     *
     * @author Trudi.
     */
    private class MyAction extends CBAction
    {
        /**
         * Calls super constructor.
         *
         * @param key
         */
        public MyAction(int key)
        {
            super(key);
        }

        /**
         * quit is called if the Esc key pressed,
         * apply is called if Enter key is pressed.
         *
         * @param e never used.
         */
        public void actionPerformed(ActionEvent e)
        {
            if (getKey() == ESCAPE)
                quit();
            else if (getKey() == ENTER)
                apply();
        }
    }

    /**
     * Sets up the L&F radio buttons.
     */
    private void lookAndFeelTab()
    {
        lookAndFeel = new JRadioButton[]{
            new JRadioButton(CBIntText.get("Windows Look and Feel")),
            new JRadioButton(CBIntText.get("Java Look and Feel")),
            new JRadioButton(CBIntText.get("Motif Look and Feel")),
            new JRadioButton(CBIntText.get("GTK Look and Feel")),
            new JRadioButton(CBIntText.get("Mac Look and Feel"))};



        String[] toolTip = new String[]{
            CBIntText.get("Sets the look and feel to: Windows"),
            CBIntText.get("Sets the look and feel to: Java"),
            CBIntText.get("Sets the look and feel to: Motif"),
            CBIntText.get("Sets the look and feel to: GTK"),
            CBIntText.get("Sets the look and feel to: Apple Mac/OSX")};

        ButtonGroup lookAndFeelButtonGroup = new ButtonGroup();
        CBPanel lookAndFeelPanel = new CBPanel();

        lookAndFeelPanel.addln(new JLabel(CBIntText.get("Select a New Look & Feel")+": "));

        // White space...
        lookAndFeelPanel.addln(new JLabel(" "));

        if (CBUtility.isWindows())
            addLookAndFeelOption(lookAndFeelButtonGroup, WINDOWS, lookAndFeelPanel, toolTip);
        else
            lookAndFeel[WINDOWS].setSelected(false);

        addLookAndFeelOption(lookAndFeelButtonGroup, JAVA, lookAndFeelPanel, toolTip);
        addLookAndFeelOption(lookAndFeelButtonGroup, MOTIF, lookAndFeelPanel, toolTip);
        addLookAndFeelOption(lookAndFeelButtonGroup, GTK, lookAndFeelPanel, toolTip);

        if (CBUtility.isMac())
            addLookAndFeelOption(lookAndFeelButtonGroup, MAC, lookAndFeelPanel, toolTip);
        else
            lookAndFeel[MAC].setSelected(false);

        getLookAndFeel();

        tabbedPane.addTab(CBIntText.get("Look & Feel"), new ImageIcon(Theme.getInstance().getDirIcons() + "look_feel.gif"), lookAndFeelPanel, CBIntText.get("Change the 'look and feel' of JXplorer, that is, adopt a similar appearance to another application."));
    }

    /**
     * Adds a look and feel item to the panel.
     *
     * @param lookAndFeelButtonGroup the group to add it to.
     * @param i                      the position to add it.
     * @param lookAndFeelPanel       the panel to add it too.
     * @param toolTip                the tooltip for the item.
     */
    private void addLookAndFeelOption(ButtonGroup lookAndFeelButtonGroup, int i, CBPanel lookAndFeelPanel, String[] toolTip)
    {
        lookAndFeelButtonGroup.add(lookAndFeel[i]);
        lookAndFeelPanel.addln(lookAndFeel[i]);
        lookAndFeel[i].setToolTipText(toolTip[i]);
    }

    /**
     * Gets the L&F from the property file and set the appropriate combo box item.
     */
    private void getLookAndFeel()
    {
/*        lookAndFeelVal = new String[]{"com.sun.java.swing.plaf.windows.WindowsLookAndFeel",
                                      "javax.swing.plaf.metal.MetalLookAndFeel",
                                      "com.sun.java.swing.plaf.motif.MotifLookAndFeel",
                                      "com.sun.java.swing.plaf.mac.MacLookAndFeel"};
*/
        lookAndFeelVal = new String[] {WINDOWS_LF, JAVA_LF, MOTIF_LF, GTK_LF, MAC_LF};


        for (int i = 0; i < lookAndFeelVal.length; i++)
        {
            if (String.valueOf(lookAndFeelVal[i]).equalsIgnoreCase(JXConfig.getProperty("gui.lookandfeel")))
                lookAndFeel[i].setSelected(true);
        }
    }


    /**
     * Checks if the L&F has been changed.  If so updates
     * the property file and sets the L&F as requested.
     */
    private void checkLookAndFeel()
    {
        // Used for exception info...
        JRadioButton rb = null;    //TE: used for exception info.

        String currentLF = JXConfig.getProperty("gui.lookandfeel");

        try
        {
            if ((lookAndFeel[WINDOWS].isSelected()) == true)
            {
                // Don't change it unless we have to...
                if(currentLF.equalsIgnoreCase(WINDOWS_LF))
                    return;

                rb = lookAndFeel[WINDOWS];
                setNewLookAndFeel(WINDOWS_LF, WINDOWS);
            }
            else if ((lookAndFeel[JAVA].isSelected()) == true)
            {
                // Don't change it unless we have to...
                if(currentLF.equalsIgnoreCase(JAVA_LF))
                    return;

                rb = lookAndFeel[JAVA];
                setNewLookAndFeel(JAVA_LF, JAVA);
            }
            else if ((lookAndFeel[MOTIF].isSelected()) == true)
            {
                // Don't change it unless we have to...
                if(currentLF.equalsIgnoreCase(MOTIF_LF))
                    return;

                rb = lookAndFeel[MOTIF];
                setNewLookAndFeel(MOTIF_LF, MOTIF);
            }
            else if ((lookAndFeel[GTK].isSelected()) == true)
            {
                // Don't change it unless we have to...
                if(currentLF.equalsIgnoreCase(GTK_LF))
                    return;

                rb = lookAndFeel[GTK];
                setNewLookAndFeel(GTK_LF, GTK);
            }
            else if ((lookAndFeel[MAC].isSelected()) == true)
            {
                // Don't change it unless we have to...
                if(currentLF.equalsIgnoreCase(MAC_LF))
                    return;

                rb = lookAndFeel[MAC];
                setNewLookAndFeel(MAC_LF, MAC);
            }
        }
        catch (UnsupportedLookAndFeelException exc)  // It can throw an exception if you try to set windows on a non-windows box.
        {
            if (rb !=  null)
            {
                rb.setEnabled(false);
                log.warning("Unsupported LookAndFeel: " + rb.getText());
            }
            JOptionPane.showMessageDialog(browser, "That 'look and feel' isn't supported on your computer.","Unsupported Look and Feel",  JOptionPane.ERROR_MESSAGE);
            return;

        }
        catch (Exception exc)  // Shouldn't happen, but this is a pretty bizarre operation so just in case...
        {
            rb.setEnabled(false);
            exc.printStackTrace();
            log.warning("Could not load LookAndFeel: " + rb.getText());
            exc.printStackTrace();
        }

        // Sets the cursor to an hour-glass...
        getOwner().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        updateLookAndFeel();

        if (mainMenu.getConnection != null)
            SwingUtilities.updateComponentTreeUI(mainMenu.getConnection);

        // Sets the cursor back to normal...
        getOwner().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        // Force a refresh of the attribute display...so that the fields don't disappear (bug 367)...
		(browser.getAttributeDisplay()).refreshEditors();
    }

    /**
     * Sets the look and feel and registers it in the property file.
     * @param lf the look and feel (package name).
     * @param pos the position of the lf in the radio buttons.
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws UnsupportedLookAndFeelException
     */
    private void setNewLookAndFeel(String lf, int pos)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
    {
        // If this fails we know which L&F made it fail...
        // stuffs things up on error 'casue rb is then null... JRadioButton rb = lookAndFeel[pos];
        UIManager.setLookAndFeel(lf);

        // Updates the 'gui.lookandfeel' value in the dxconfig property file with the windows L&F...
        JXConfig.setProperty("gui.lookandfeel", lf);
        //return rb;
    }

    /**
     * There must be a better way of doing this but here it is anyway...
     * This method tries to make sure that the whole of JXplorer actually gets
     * the new Look and Feel.  It should work by just using...
     * <br>'SwingUtilities.updateComponentTreeUI(getOwner());',<br>
     * but some components are forgotten about - such as this window, the search
     * gui and the popup tree tool.  So this method manually tells them to update.
     * But thats not all - each tree has its own copy of the search gui & the popup
     * tree tool...so each tree needs to be told to update these components.
     * In all there are eight calls to the updateComponentTreeUI...phew.
     */
    private void updateLookAndFeel()
    {
        if ((getOwner() instanceof JXplorerBrowser) == false)
        {
            SwingUtilities.updateComponentTreeUI(getOwner());
            return;
        }
        else // multi window magic - need to get *all* browser windows and update them
        {
            JXplorerBrowser triggeringBrowser = (JXplorerBrowser) getOwner();
            JXplorer jxplorer = triggeringBrowser.getRootJXplorer();
            ArrayList<JXplorerBrowser> browsers = jxplorer.getBrowsers();
            for (JXplorerBrowser browser:browsers)
            {
                updateLookAndFeel(browser);
            }
        }


    }

    private void updateLookAndFeel(JXplorerBrowser browser)
    {

//TE XXXXXXXXXX this produces bug 2578..........
        // Update the bulk of JXplorer with the new L&F...
        SwingUtilities.updateComponentTreeUI(browser);

        // Make sure this window gets the new L&F too...
        SwingUtilities.updateComponentTreeUI(this);

        // Get the trees...
        SmartTree explore = browser.getTree();
        SmartTree search = browser.getSearchTree();
        SmartTree schema = browser.getSchemaTree();

        // To stop the truncation of tree nodes esp in Java L&F...what a joke....
        SwingUtilities.updateComponentTreeUI(explore);
        SwingUtilities.updateComponentTreeUI(search);
        SwingUtilities.updateComponentTreeUI(schema);

        // Make sure each tree's popup menu gets the new L&F...
        SwingUtilities.updateComponentTreeUI(explore.getPopupTool());
        SwingUtilities.updateComponentTreeUI(search.getPopupTool());
        SwingUtilities.updateComponentTreeUI(schema.getPopupTool());

        // Make sure each tree's search dialog gets the new L&F...
        SearchGUI sExplore = explore.getSearchGUI();
        SearchGUI sSearch = search.getSearchGUI();
        SearchGUI sSchema = schema.getSearchGUI();

        if (sExplore != null)
            SwingUtilities.updateComponentTreeUI(sExplore);
        if (sSearch != null)
            SwingUtilities.updateComponentTreeUI(sSearch);
        if (sSchema != null)
            SwingUtilities.updateComponentTreeUI(sSchema);
    }


    /**
     * Sets up the log method radio buttons.
     */
    private void logMethod()
    {
        CBPanel logMethodPanel = new CBPanel();

        logMethodPanel.addln(new JLabel(CBIntText.get("Select a New Log Method")+": "));

        // White space...
        logMethodPanel.addln(new JLabel(" "));

        logMethodCombo = new JComboBox(logMethodVal);
        logMethodCombo.setToolTipText(CBIntText.get("Set the log method in JXplorer."));

        logMethodPanel.addln(logMethodCombo);

        // White space...
        logMethodPanel.addln(new JLabel(" "));

        getLogMethod();

        tabbedPane.addTab(CBIntText.get("Log Method"), new ImageIcon(Theme.getInstance().getDirIcons() + "log_method.gif"), logMethodPanel, CBIntText.get("Set the method of logging you want, for example, to a file."));
    }

    /**
     * Gets the log method from the property file and sets the appropriate radio button.
     */
    private void getLogMethod()
    {
        String logHandlers = (JXConfig.getProperty("handlers")); // java logging standard property

        if (logHandlers.indexOf("ConsoleHandler") > 0 && logHandlers.indexOf("FileHandler") > 0)
            logMethodCombo.setSelectedItem(logMethodVal[3]);
        else if (logHandlers.indexOf("FileHandler") > 0)
            logMethodCombo.setSelectedItem(logMethodVal[2]);
        else if (logHandlers.indexOf("ConsoleHandler") > 0)
            logMethodCombo.setSelectedItem(logMethodVal[1]);
        else
            logMethodCombo.setSelectedItem(logMethodVal[0]);
    }

    /**
     * Sets up the log level radio buttons.
     */
    private void logLevel()
    {
        CBPanel logLevelPanel = new CBPanel();

        logLevelPanel.addln(new JLabel(CBIntText.get("Select a New Log Level")+": "));
        logLevelPanel.addln(new JLabel(" "));

        logLevelCombo = new JComboBox(logLevelVal);
        logLevelCombo.setToolTipText(CBIntText.get("Set the logging level in JXplorer."));
        logLevelPanel.addln(logLevelCombo);
        logLevelPanel.addln(new JLabel(" "));

        getLogLevel();

        tabbedPane.addTab(CBIntText.get("Log Level"),
                new ImageIcon(Theme.getInstance().getDirIcons() + "log_level.gif"), logLevelPanel,
                CBIntText.get("Set the level of logging you want, for example, errors only."));
    }

    /**
     * Gets the log level from the property file and sets the appropriate combo box item.
     */
    private void getLogLevel()
    {
        // Get the log level from the property file...
        Level logLevel;
        try
        {
            logLevel = Level.parse(JXConfig.getProperty(".level"));
        }
        catch (Exception e) // IllegalArgumentException, or possibly a null pointer exception
        {
            logLevel = Level.WARNING;  // default
        }

        if (logLevel.equals(Level.SEVERE))
            logLevelCombo.setSelectedItem(logLevelVal[0]);        // Errors Only option.
        else if (logLevel.equals(Level.WARNING))
            logLevelCombo.setSelectedItem(logLevelVal[1]);       // Basic option.
        else if (logLevel.equals(Level.INFO))
            logLevelCombo.setSelectedItem(logLevelVal[2]);       // Tree Operations option.
        else if (logLevel.equals(Level.FINE))
            logLevelCombo.setSelectedItem(logLevelVal[3]);       // Extensive option.
        else if (logLevel.equals(Level.FINEST))
            logLevelCombo.setSelectedItem(logLevelVal[4]);       // All option.
        else if (logLevel.equals(Level.ALL))
            logLevelCombo.setSelectedItem(logLevelVal[5]);       // All + BER Trace option.
        else
            logLevelCombo.setSelectedItem(logLevelVal[1]);// Default option.
    }

    /**
     * Sets up the LDAP Levels tab.  Adds the text fields & labels to the panel.
     */
    private void ldapLevels()
    {
        ldapLimit = new JTextField();
        ldapTimeout = new JTextField();
        pagedResults = new JCheckBox();
        pageSize = new JTextField();

        getLdapLevels();

        CBPanel ldapLevelsPanel = new CBPanel();

        ldapLevelsPanel.addln(new JLabel(CBIntText.get("Set LDAP Options") + ": "));
        ldapLevelsPanel.addln(new JLabel(" "));        //TE: white space.
        ldapLevelsPanel.add(new JLabel(CBIntText.get("LDAP Limit") + ": "));
        ldapLevelsPanel.add(ldapLimit);
        ldapLevelsPanel.newLine();
        ldapLevelsPanel.addln(new JLabel(" "));        //TE: white space.
        ldapLevelsPanel.add(new JLabel(CBIntText.get("LDAP Timeout")+": "));
        ldapLevelsPanel.add(ldapTimeout);
        ldapLevelsPanel.newLine();
        ldapLevelsPanel.addln(new JLabel(" "));        //TE: white space.
        ldapLevelsPanel.add(new JLabel(CBIntText.get("Use Paged Results")+": "));
        ldapLevelsPanel.add(pagedResults);
        ldapLevelsPanel.newLine();
        ldapLevelsPanel.addln(new JLabel(" "));        //TE: white space.
        ldapLevelsPanel.add(new JLabel(CBIntText.get("Page Size")+": "));
        ldapLevelsPanel.add(pageSize);

        pagedResults.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (pagedResults.isSelected())
                {
                    pageSize.setEnabled(true);
                    if ("-1".equals(pageSize.getText().trim()))
                        pageSize.setText("1000");
                }
                else
                {
                    pageSize.setText("-1");
                    pageSize.setEnabled(false);
                }
            }
        });

        tabbedPane.addTab(CBIntText.get("Search Limits"),
                new ImageIcon(Theme.getInstance().getDirIcons() + "find.gif"), ldapLevelsPanel,
                CBIntText.get("Set the search levels, that is, the number of entries returned from a search and the timeout."));
    }

    /**
     * Gets the ldap limit and timeout values from the property file and sets the text fields with these values.
     */
    private void getLdapLevels()
    {
        // Gets the values from the property file...
        String limit = JXConfig.getProperty("option.ldap.limit");
        String timeout = JXConfig.getProperty("option.ldap.timeout");
        String size = JXConfig.getProperty("option.ldap.pageSize");
        String usingPaging = JXConfig.getProperty("option.ldap.pagedResults");


        ldapLimit.setText(limit);
        ldapLimit.setColumns(5);
        ldapLimit.setToolTipText(CBIntText.get("Enter the new limit level."));
        ldapTimeout.setText(timeout);
        ldapTimeout.setColumns(5);
        ldapTimeout.setToolTipText(CBIntText.get("Enter the new timeout level."));
        pageSize.setText(size);
        pageSize.setToolTipText(CBIntText.get("Enter the page size for paged results."));
        pageSize.setColumns(5);

        pagedResults.setSelected("true".equalsIgnoreCase(usingPaging));
        pagedResults.setToolTipText(CBIntText.get("Whether to use paged results to return large data sets"));
        pageSize.setEnabled(pagedResults.isSelected());
    }

    /**
     * Sets up the URL tab with a combo box.
     */
    private void urlTab()
    {
        String[] url = new String[]{CBIntText.get("JXplorer"), CBIntText.get("Launch")};

        CBPanel urlPanel = new CBPanel();
        urlCombo = new JComboBox(url);

        getURLHandling();

        urlPanel.addln(new JLabel(CBIntText.get("Select URL handling") + ": "));
        urlPanel.addln(new JLabel(" "));
        urlPanel.addln(urlCombo);
        urlPanel.addln(new JLabel(" "));
        urlPanel.addln(new JLabel(CBIntText.get("Note: Launch is for Windows only.")));

        tabbedPane.addTab(CBIntText.get("URL"), new ImageIcon(Theme.getInstance().getDirIcons() + "url.gif"), urlPanel, CBIntText.get("Select how you would like the URLs handled in JXplorer."));
    }

    /**
     * Gets the url handling type (either JXplorer or Launch -
     * if something else, it defaults to JXplorer)
     * from the property file and sets the url combo box.
     */
    private void getURLHandling()
    {
        // Gets the value from the property file...
        String urlHandling = JXConfig.getProperty("option.url.handling");

        int index = 0;

        if (urlHandling != null)
            if (urlHandling.equalsIgnoreCase("Launch"))
                index = 1;

        urlCombo.setSelectedIndex(index);
    }

    /**
     * Sets up the Password option tab.
     */
    private void pwdTab()
    {
        String[] cache = new String[] {CBIntText.get("Yes"),CBIntText.get("No")};

        CBPanel urlPanel = new CBPanel();
        cachePwdCombo = new JComboBox(cache);

        getPasswordCachingOption();

        urlPanel.addln(new JLabel(CBIntText.get("Cache Passwords") + ": "));
        urlPanel.addln(new JLabel(" "));
        urlPanel.addln(cachePwdCombo);
        urlPanel.addln(new JLabel(" "));
        urlPanel.addln(new JLabel(CBIntText.get(" ")));

        tabbedPane.addTab(CBIntText.get("Cache Passwords"), new ImageIcon(Theme.getInstance().getDirIcons() + "cachePwds.gif"), urlPanel, CBIntText.get("Select Yes if you want passwords cached in JXplorer."));
    }

   /**
    * Gets the password caching option from the property
    * file and sets the cachePwd combo box.
    */
    private void getPasswordCachingOption()
    {
        // Gets the value from the property file...
        String pwdCaching = JXConfig.getProperty("jxplorer.cache.passwords");

        int index = 0;

        if(pwdCaching!=null)
            if(pwdCaching.equalsIgnoreCase("false"))
                index = 1;

        cachePwdCombo.setSelectedIndex(index);
    }

   /**
     * Calls the appropriate save methods then exits.
     */
    private void apply()
    {
        checkLookAndFeel();
        checkLogMethod();
        checkLogLevel();
        checkLdapLevels();
        checkUrlHandling();
        checkCachePwds();

        quit();
    }

    /**
     * Checks if the log method has been changed.
     * If so updates the property file and sets
     * the log method as requested.
     */
    private void checkLogMethod()
    {
        try
        {
            int logMethod = logMethodCombo.getSelectedIndex();

            String original = JXConfig.getProperty("handlers");

            switch (logMethod)
            {
                case 0:
                    JXConfig.setProperty("handlers", "");
                    break;
                case 1:
                    JXConfig.setProperty("handlers", "java.util.logging.ConsoleHandler");
                    break;
                case 2:
                    JXConfig.setProperty("handlers", "java.util.logging.FileHandler");
                    break;
                case 3:
                    JXConfig.setProperty("handlers", "java.util.logging.ConsoleHandler,java.util.logging.FileHandler");
                    break;
                default:
                    JXConfig.setProperty("handlers", "java.util.logging.ConsoleHandler,java.util.logging.FileHandler");
            }

            if (original.equals(JXConfig.getProperty("handlers")) == false)
            {
                JXConfig.writePropertyFile();
                JXConfig.setupLogger();
            }


            //TODO implement Full GUI for java logging of JX packages - may have to wait for Sun to fix the broken logging system :-)
        }
        catch (Exception e)
        {
            return;  // Should never happen. TODO: log properly
        }

    }

    /**
     * Checks if the log level has been changed.
     * If so updates the property file and sets
     * the log level as requested.
     */
    private void checkLogLevel()
    {
        String original = JXConfig.getProperty(".level");

        switch (logLevelCombo.getSelectedIndex())
        {
            case 0:
                {
                    JXConfig.setProperty(".level", "SEVERE");
                    JXConfig.setProperty("com.ca.level", "SEVERE");
                    break;
                }     //TE: Errors Only option.
            case 1:
                {
                    JXConfig.setProperty(".level", "WARNING");
                    JXConfig.setProperty("com.ca.level", "WARNING");
                    break;
                }    //TE: Basic option.
            case 2:
                {
                    JXConfig.setProperty(".level", "INFO");
                    JXConfig.setProperty("com.ca.level", "INFO");
                    break;
                }       //TE: Tree Operations option.
            case 3:
                {
                    JXConfig.setProperty(".level", "FINE");
                    JXConfig.setProperty("com.ca.level", "FINE");
                    break;
                }       //TE: Extensive option.
            case 4:
                {
                    JXConfig.setProperty(".level", "FINEST");
                    JXConfig.setProperty("com.ca.level", "FINEST");
                    break;
                }     //TE: All option.
            case 5:
                {
                    JXConfig.setProperty(".level", "ALL");
                    JXConfig.setProperty("com.ca.level", "ALL");
                    break;
                }        //TE: All + BER Trace option.
            default:
                {
                    JXConfig.setProperty(".level", "WARNING");
                    JXConfig.setProperty("com.ca.level", "WARNING");
                    break;
                }   //TE: Errors Only option.
        }

        if (original.equals(JXConfig.getProperty(".handlers")) == false)
        {
            JXConfig.writePropertyFile();
            JXConfig.setupLogger();
        }

        // Set the values for immediate use...
        browser.checkSpecialLoggingActions();
    }

    /**
     * Checks if the ldap levels have been changed by getting the values from the text areas.
     * Sets the changes in the property file and in searchBroker.
     */
    private void checkLdapLevels()
    {
        String limit = ldapLimit.getText();
        String timeout = ldapTimeout.getText();
        boolean usePaging = pagedResults.isSelected();
        String size = pageSize.getText();

        try
        {
            // Make sure the values are of integer type...
            Integer.valueOf(limit);
            Integer.valueOf(timeout);
            Integer.valueOf(size);
        }
        catch (NumberFormatException e)
        {
            CBUtility.error("Ldap limit, timeout and page size must be integers.\n" + e);
            getLdapLevels();
        }

        // Set the values in the property file...
        JXConfig.setProperty("option.ldap.limit", limit);
        JXConfig.setProperty("option.ldap.timeout", timeout);
        JXConfig.setProperty("option.ldap.pagedResults", Boolean.toString(usePaging));
        JXConfig.setProperty("option.ldap.pageSize", (usePaging)?size:"-1");

        // Sets the values in searchBroker for immediate use...
        browser.searchBroker.setTimeout(Integer.parseInt(timeout));
        browser.searchBroker.setLimit(Integer.parseInt(limit));
        browser.searchBroker.setPaging(usePaging, Integer.valueOf(size));
        browser.getJndiBroker().setPaging(usePaging, Integer.valueOf(size));
    }

    /**
     * Checks what the URL handling is and sets it in the property file.
     * Does this by getting the user selected index...if 0 then
     * 'JXplorer' if 1 then 'Launch'.
     */
    private void checkUrlHandling()
    {
        int index = urlCombo.getSelectedIndex();

        if (index == 1)
            JXConfig.setProperty("option.url.handling", "Launch");
        else
            JXConfig.setProperty("option.url.handling", "JXplorer");
    }

   /**
    * Checks what the URL handling is and sets it in the property file.
    * Does this by getting the user selected index...if 0 then
    * 'JXplorer' if 1 then 'Launch'.
    */
    private void checkCachePwds()
    {
        int index = cachePwdCombo.getSelectedIndex();

        if(index == 1)
            JXConfig.setProperty("jxplorer.cache.passwords", "false");
        else
            JXConfig.setProperty("jxplorer.cache.passwords", "true");
    }

   /**
     * Resets all values in the advanced options window back to the values in the property file.
     */
    private void reset()
    {
        getLookAndFeel();
        getLogLevel();
        getLogMethod();
        getLdapLevels();
        getURLHandling();
        getPasswordCachingOption();
    }

    /**
     * Shuts the advanced options dialog.
     */
    private void quit()
    {
        setVisible(false);
        dispose();
    }
}
