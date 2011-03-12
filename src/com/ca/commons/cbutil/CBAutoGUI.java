package com.ca.commons.cbutil;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Vector;

/**
 * This class creates a GUI interface to a command line tool.
 * It assumes that all the command line options are displayed as
 * is, or followed immediately by strings, and are presented in
 * the order they appear in a formatted file that describes them.<p>
 * <p/>
 * At the moment it will only wrap a single command line program.<p>
 * <p/>
 * The file format is: <br>
 * <pre>
 *    command string
 *    primary: {option name}                             (becomes a menu item)
 *    secondary: {option name} [nostring|ispassword]     (becomes a field)
 *    secondary: "your comment here"                     (becomes a helpfull label)
 *    secondary: ...
 *    ...
 *    primary:
 *    secondary:
 *    ...
 *    </pre>
 * <p/>
 * nb. this class tries to use an image: 'open.gif' to adorn the open file
 * file chooser, and expects this to be in the base directory.<p>
 * <p/>
 * Other options for secondary values include tip="..." for tooltips, and
 * default="..." for default values.
 *
 * @author Chris Betts
 */


public class CBAutoGUI extends JDialog implements ActionListener
{
    String commandName;     // the root OS utility name to run.
    Vector menuItems;       // primary options are menu items.
    Vector commonItems;     // some secondary options may be common to all Primary options

    final JEditorPane output = new JEditorPane();     // where the command output goes...
    JTabbedPane tabPane;    // the top level GUI component containing the option panes.
    CBPanel commonOptions;  // the top level GUI component containing shared options
    PrimaryOption common;   // a 'fake' primary option containing all shared secondary options

    static String lastDirectory = "";  // the last directory used by the user, used in the file chooser.

    boolean guiInitialised = false;

    boolean debug = true;

    static boolean standAlone = false;

    /**
     * An example of stand alone usage.
     */

    public static void main(String args[])
    {
        standAlone = true;

        String fileName = "keytool.txt";
        if (args.length >= 1)
            fileName = args[0];

        CBAutoGUI MrGui = new CBAutoGUI(null, fileName);
        MrGui.show(null);
    }

    /**
     * Constructs an AutoGUI showing a particular initial primary option.<p>
     * - requires a root GUI component
     * (may be null) and the fileName of a file containing a description
     * of the command line function to be called, and the parameters it
     * takes.
     *
     * @param owner    the root GUI (required for correct look and feel propagation
     *                 and repaint behaviour).
     * @param fileName the name of the file containing the command line function description.
     */

    public CBAutoGUI(Frame owner, String fileName)
    {
        super(owner);

        menuItems = new Vector(8);
        commonItems = new Vector(8);
        common = new PrimaryOption("common options");

        processFile(fileName);

        setSize(650, 550);
    }

    /**
     * Brute force search through vector for object by name.  yuck.
     * rewrite sometime to be prettier.  Kind of slow too - lots of string ops.
     * Oh well. CPU is cheap.
     */

    static Object getNamedObject(Vector v, String s)
    {
        s = s.trim();
        for (int i = 0; i < v.size(); i++)
        {
            if (v.get(i).toString().trim().equalsIgnoreCase(s))
                return v.get(i);
        }
        return null;
    }


    /**
     * Sets a particular primary option to display.
     *
     * @param optionName the primaryOption (and tab title) to set the tabbed display to.
     *                   a null or blank string sets the first option.
     */

    public void setOption(String optionName)
    {
        if (optionName == null || optionName.length() == 0)
        {
            if (tabPane.getTabCount() > 0)
                optionName = tabPane.getTitleAt(0).trim();
            else
            {
                error("no tabs set! - nothing to display");
                return;
            }
        }

        optionName = optionName.trim();

        if (optionName.length() == 0) return;
        if (optionName.charAt(0) == '-')
            optionName = optionName.substring(1);
        if (optionName.length() == 0) return;

        for (int i = 0; i < tabPane.getTabCount(); i++)
        {
            if (optionName.equalsIgnoreCase(tabPane.getTitleAt(i).trim()))
            {
                tabPane.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * A bit of a hack - allows a program to set the default value
     * of an option at runtime.  Requires the primary and secondary
     * option names to be passed in (as they are in the file) as well
     * as the default value.
     *
     * @param primaryOptionName   the primary option command string, as in text file.
     * @param secondaryOptionName the secondary option command string, as in text file.
     * @param defaultValue        the new default value for the named secondary option.
     */

    public void setDefaultValue(String primaryOptionName, String secondaryOptionName, String defaultValue)
    {
        // XXX more of a hack - rig the names to be the same as the .toString() names of the Options...
        
        primaryOptionName = formatOptionName(primaryOptionName);
        secondaryOptionName = formatOptionName(secondaryOptionName);

        PrimaryOption primary = (PrimaryOption) getNamedObject(menuItems, primaryOptionName);
        if (primary == null)
            primary = common;

        SecondaryOption secondary = primary.get(secondaryOptionName);

        if (secondary != null)
            secondary.setDefaultValue(defaultValue);
    }

    /**
     * Sets the tab panel to the selected primary option,
     * and makes the component visible.
     *
     * @param option the command line primary option panel to make visible.
     */

    public void show(String option)
    {
        if (debug) System.out.println("showing GUI with option : " + ((option == null) ? " (no tab set) " : option));

        if (guiInitialised == false)
            initialiseGUI();
        else
            clearPasswords();

        setOption(option);
        setVisible(true);
    }

    /**
     * Create two panels.  In the top panel, place a tabbed set of panes
     * corresponding to each primary option, and populate that with all
     * the secondary options corresponding to the relevant primary option.
     * In the bottom panel, place the secondary options common to all primary options.
     */

    public void initialiseGUI()
    {
        if (debug) System.out.println("initialising GUI");

        setTitle("GUI interface to " + commandName);

        CBPanel display = new CBPanel();

        addTabbedPanes(display);

        addCommonOptions(display);


        JPanel buttons = new JPanel();
        JButton OK, Cancel, Help;

        display.makeWide();
        display.add(new JLabel(""));  // padding
        display.makeLight();
        display.add(OK = new JButton("Execute"));
        display.add(Cancel = new JButton("Cancel"));
        //display.add(Help = new JButton("Help"));
        display.makeWide();
        display.addLine(new JLabel(""));  // padding
        display.makeHeavy();

        OK.addActionListener(this);
        Cancel.addActionListener(this);
        //Help.addActionListener(this);

        //output = new JEditorPane();
        output.setBorder(new TitledBorder(new LineBorder(Color.black, 2), "output"));
        output.setPreferredSize(new Dimension(400, 150));

        display.addLine(new JScrollPane(output));

        getContentPane().add(display);
        setVisible(true);
        guiInitialised = true;
    }

    /**
     * This runs through all the groups of 'primary' options,
     * creating a new tabbed pane for each one, and populating
     * it with the associated secondary options.
     */

    public void addTabbedPanes(CBPanel display)
    {
        if (menuItems.size() == 0) return;  // don't add anything if there aren't any...

        tabPane = new JTabbedPane();

        for (int i = 0; i < menuItems.size(); i++)
        {
            PrimaryOption primary = (PrimaryOption) menuItems.get(i);
            CBPanel panel = new CBPanel();
            panel.makeLight();
            tabPane.addTab(primary.toString(), panel);

            boolean newLine = false;
            for (int j = 0; j < primary.size(); j++)
            {
                SecondaryOption secondary = primary.get(j);
                if (secondary.isLabel())
                {
                    addSecondaryToPanel(panel, secondary, newLine);
                    newLine = false;
                }
                else
                {
                    addSecondaryToPanel(panel, secondary, newLine);
                    newLine = !newLine;
                }
            }
            // Hack to get GridBagLayout to do what I want...
            
            panel.newLine();
            panel.add(new JLabel("                     ")); // spacer below...
            panel.makeHeavy();
            panel.addWide(new JLabel(" "), 5); // spacer below...
        }
        display.addLine(tabPane);
    }

    /**
     * Add common options to a separate, constant display area.
     * Sometimes commands are common to all options, for example a password
     * must be given before any action may be done. This method adds such
     * common options to a separate display area that remains constant
     * when the user moves between different tab panes.
     */

    public void addCommonOptions(CBPanel display)
    {
        if (commonItems.size() == 0) return;   // if there are no common options, don't do anything.

        commonOptions = new CBPanel();
        for (int i = 0; i < commonItems.size(); i++)
        {
            SecondaryOption secondary = (SecondaryOption) commonItems.get(i);
            addSecondaryToPanel(commonOptions, secondary, ((i % 2) != 0));
        }
        // Hack to get GridBagLayout to do what I want...
            
        commonOptions.newLine();
        commonOptions.add(new JLabel("                    ")); // spacer below...
        commonOptions.makeHeavy();
        commonOptions.addWide(new JLabel(" "), 5); // spacer below...

        display.addLine(commonOptions);
    }

    void clearPasswords()
    {
        for (int i = 0; i < tabPane.getTabCount(); i++)
        {
            Component c = tabPane.getComponent(i);
            clearPasswords(c);
        }
        clearPasswords(commonOptions);
    }

    void clearPasswords(Component c)
    {
        if (c instanceof Container)
        {
            Container con = (Container) c;
            for (int i = 0; i < con.getComponentCount(); i++)
            {
                Component comp = con.getComponent(i);
                if (comp instanceof JPasswordField)
                {
                    ((JPasswordField) comp).setText("");
                }
            }
        }
    }

    void addSecondaryToPanel(CBPanel panel, SecondaryOption secondary, boolean newLine)
    {
        if (secondary.isHidden()) return;  // don't add hidden fields.

        JComponent comp1 = null;
        JComponent comp2 = null;
        CBFileChooserButton fileChooser = null;

        String defaultValue = secondary.getDefaultValue();  // most don't have defaults...

        if (secondary.isLabel())
        {
            JLabel label = new JLabel(secondary.toString());
            Font current = label.getFont();
            label.setFont(new Font(current.getName(), Font.BOLD, current.getSize() + 2));
            panel.addLine(label);
/*            if (newLine)
                panel.addWide( label, 2);
            else
                panel.addWide( label, 4);    
*/                
            newLine = true;    // force new line...    
            return;
        }
        else if (secondary.isPassword())
        {
            panel.add(comp1 = new JLabel(secondary.toString()));
            panel.addGreedyWide(comp2 = new JPasswordField(), 2);
        }
        else if (secondary.isCheckBox())
        {
            panel.add(comp1 = new JLabel(secondary.toString()));
            panel.add(comp2 = new JCheckBox());
            if ("".equals(defaultValue) == false)
                ((JCheckBox) comp2).setSelected(true);
            panel.add(new JLabel(""));
        }
        else if (secondary.isFile())
        {
            panel.add(comp1 = new JLabel(secondary.toString()));
            panel.addGreedyWide(comp2 = new JTextField(defaultValue, 30));
            panel.add(fileChooser = new CBFileChooserButton((JTextField) comp2, this, "File"));
            if (defaultValue.length() > 0)
            {
                fileChooser.setLocalDirectoryUse(true);
                fileChooser.setStartingDirectory(defaultValue);
            }
        }
        else
        {
            panel.add(comp1 = new JLabel(secondary.toString()));
            panel.addGreedyWide(comp2 = new JTextField(defaultValue, 40), 2);
        }

        if (comp1 != null) comp1.setToolTipText(secondary.getTip());
        //if (comp2 != null) comp2.setToolTipText(secondary.getTip());
    
        if (newLine)
            panel.newLine();
    }


    void processFile(String fileName)
    {
        if (debug) System.out.println("processing file '" + fileName + "'");

        PrimaryOption currentItem = null;
        try
        {
            BufferedReader readText = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));

            String line = "";                                    // the first line read from the ldif file
            int count = 0;
            while ((line = readText.readLine()) != null)
            {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#')
                {
                    // do nothing - comment line.
                }
                else if (commandName == null) // get command line utility name (first real line in file)
                {
                    if (debug) System.out.println("read commandName as '" + line + "'");
                    commandName = line;
                }
                else if (line.toLowerCase().startsWith("primary: "))
                {
                    line = line.substring(9).trim();
                    if (debug) System.out.println("read primary option as '" + line + "'");
                    menuItems.add((currentItem = new PrimaryOption(line)));
                }
                else if (line.toLowerCase().startsWith("common: "))
                {
                    line = line.substring(8).trim();
                    if (debug) System.out.println("read common option as '" + line + "'");
                    SecondaryOption op = getSecondaryOption(line);
                    common.add(op);
                    commonItems.add(op);
                }
                else if (line.toLowerCase().startsWith("secondary: ") && currentItem != null)
                {
                    line = line.substring(11).trim();
                    if (debug) System.out.println("read secondary option as '" + line + "'");
                    currentItem.add(getSecondaryOption(line));
                }
                else
                    error("WARNING: ignoring line '" + line + "'");
            }
        }
        catch (Exception e)
        {
            error("unable to open file:\n" + e.toString());
            e.printStackTrace();
        }
    }

    SecondaryOption getSecondaryOption(String line)
    {
        int pos = line.indexOf(' ');
        if (pos < 0) pos = line.length();

        String name = line.substring(0, pos);

        boolean isPwd = false;
        boolean hasArg = true;
        boolean isLabel = false;
        boolean isHidden = false;
        boolean isFile = false;

        String tooltip = null;
        String defaultValue = null;

        if (line.charAt(0) == '\"')
        {
            isLabel = true;
            name = line.substring(1, line.length() - 1);   // the entire line is one comment.
        }
        else if (pos != line.length())
        {
            String lowC = line.substring(pos).toLowerCase();
            
            // This is damn ugly, but I'm in a hurry...
            if (lowC.indexOf("tip=") != -1)  // get tool tip
            {
                pos = lowC.indexOf("tip=");
                pos = line.indexOf("\"", pos);
                int pos2 = line.indexOf("\"", pos + 1);
                if (pos2 == -1) pos2 = line.length(); // no closing quote...
                tooltip = line.substring(pos + 1, pos2);
                lowC = line.substring(0, pos).toLowerCase() + line.substring(pos2);  // trim out comment so as not to confuse keyword search below.
            }
            if (lowC.indexOf("default=") != -1)  // get default value
            {
                pos = lowC.indexOf("default=");
                pos = line.indexOf("\"", pos);
                int pos2 = line.indexOf("\"", pos + 1);
                if (pos2 == -1) pos2 = line.length(); // no closing quote...
                defaultValue = line.substring(pos + 1, pos2);
                lowC = line.substring(0, pos).toLowerCase() + line.substring(pos2);  // trim out default values so as not to confuse keyword search below.
            }
            if (lowC.indexOf("nostring") != -1) hasArg = false;
            if (lowC.indexOf("ispassword") != -1) isPwd = true;
            if (lowC.indexOf("hidden") != -1) isHidden = true;
            if (lowC.indexOf("file") != -1) isFile = true;
        }

        return (new SecondaryOption(name, isPwd, hasArg, isLabel, isHidden, isFile, tooltip, defaultValue));
    }


    /**
     * Simple error printing routine - over-ride this for app specific functionality.
     */

    public void error(String msg)
    {
        System.err.println(msg);
    }

    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand().trim();
        if (cmd.equalsIgnoreCase("Execute"))
        {
            execute();
        }
        else if (cmd.equalsIgnoreCase("Cancel"))
        {
            cancel();
        }
        else if (cmd.equalsIgnoreCase("Help"))
        {
            help();
        }
        else
            error("Unknown command in OpenConWin\n" + cmd);
    }

    public void execute()
    {
        execute(2000);  // two second default timeout
    }

    public void execute(int millisecTimeout)
    {
        final int timeout = millisecTimeout;

        CBPanel current = (CBPanel) tabPane.getSelectedComponent();
        Vector args = new Vector();
        String title = tabPane.getTitleAt(tabPane.getSelectedIndex());  // name of the primary option...

        PrimaryOption command = (PrimaryOption) getNamedObject(menuItems, title);
        args.add(command.toCommandString());

        Component[] comps = current.getComponents();

        setArguments(args, comps, command);

        comps = commonOptions.getComponents();

        setArguments(args, comps, common);

        String commandString = commandName;

        if (commandString.startsWith("%JAVA%"))
        {
            commandString = System.getProperty("java.home") + System.getProperty("file.separator") +
                    "bin" + System.getProperty("file.separator") + commandString.substring(6);
        }

        final String finalName = commandString + " " + command.toCommandString();

        for (int i = 0; i < args.size(); i++)
            commandString += " " + args.get(i).toString();

        final String finalCommand = commandString;

        output.setText("running command:\n  " + finalName);

        final Thread worker = new Thread("Execute " + finalName) // run command in a separate worker thread
        {
            public void run()
            {
                try
                {
                    Runtime runMe = Runtime.getRuntime();
                    Process goBoy = runMe.exec(finalCommand);
                    //BufferedReader readText = new BufferedReader(new InputStreamReader(goBoy.getInputStream()));
                    output.read(goBoy.getInputStream(), "text");
                    output.setText(output.getText() + "\n\nCommand " + finalName + " Executed.\n");
                }
                catch (IOException e)
                {
                    output.setText(output.getText() + "\n\nIOException reading command Output\n" + e.toString());
                }
            }
        };  
        
        // fire up another thread to time out the first, if necessary...
        
        Thread waiter = new Thread("Execute Watcher for " + finalCommand)
        {
            public void run()
            {
                try
                {
                    (Thread.currentThread()).sleep(timeout);

                    if (worker != null && worker.isAlive())
                    {
                        worker.interrupt();
                        try
                        {
                            (Thread.currentThread()).sleep(10);
                        }  // pause 10 ms
                        catch (InterruptedException e)
                        {
                        }
                    
                        /* apparently unnecessary - if code above failed, stop() wouldn't work anyway?
                        if (worker.isInterrupted()==false)  // we tried to be nice but...
                        {
                            System.out.println("attempting to force stop...");                        
                            worker.stop();
                        }
                        */
                        output.setText(output.getText() + "\n\nError - unable to complete command " + finalName + "\n   - request timed out in " + timeout + " milliseconds.\n");
                    }
                }
                catch (InterruptedException intEx)
                {
                }
            }
        };

        worker.start();
        waiter.start();
    }

    void setArguments(Vector args, Component[] comps, PrimaryOption command)
    {
        Component oldC = comps[0];

        for (int i = 1; i < comps.length; i++)
        {
            Component newC = comps[i];
            if (oldC instanceof JLabel)
            {
                String newArg = null;

                if (newC instanceof JPasswordField)
                {
                    JPasswordField temp = (JPasswordField) newC;
                    char[] pass = temp.getPassword();
                    if (pass.length > 0)
                    {
                        String hack = new String(pass);  // destroy all that carefull security 'cause we can't be bothered trying 
                        if (hack.trim().length() > 0)   // to do magic when we echo the command line args...
                            newArg = hack.trim();
                    }
                }
                else if (newC instanceof JTextField)
                {
                    JTextField temp = (JTextField) newC;
                    if (temp.getText().trim().length() > 0)
                        newArg = temp.getText().trim();

                }
                else if (newC instanceof JCheckBox)
                {
                    JCheckBox temp = (JCheckBox) newC;
                    if (temp.isSelected())
                        newArg = "";      // el hack - set to blank, so that base arg gets included (but no argument text :-) )
                }

                if (newArg != null)
                {
                    String secondaryOptionName = ((JLabel) oldC).getText().trim();
                    SecondaryOption opt = command.get(secondaryOptionName);
                    if (opt == null)
                        error("Error - unknown option " + secondaryOptionName);
                    else
                    {
                        args.add(opt.toCommandString());
                        args.add(newArg);
                    }
                }
            }
            oldC = newC;
        }

        Vector hiddenOps = command.getHiddenOptions();
        for (int i = 0; i < hiddenOps.size(); i++)
        {
            SecondaryOption opt = (SecondaryOption) hiddenOps.get(i);
            args.add(opt.toCommandString());
        }
    }

    public void cancel()
    {
        setVisible(false);
        dispose();
        if (standAlone) System.exit(0);
    }

    // your help code here (you'll need to add a 'Help' button called 'help'....)
    public void help()
    {

    }

    public static String formatOptionName(String name)
    {
        if (name == null || name.length() == 0)
            return "";
        else
            return ("  " + ((name.charAt(0) == '-') ? name.substring(1) : name));
    }

    /**
     * A generic Abstract Option (simply a name);
     */
    abstract class Option
    {
        String name;       // the name of the option, i.e. '-verbose'
        String tip = "";   // optional tool tip

        public Option(String optionString)
        {
            name = optionString;
        }
        
//        public String toString() { return name; }

        public String toCommandString()
        {
            return name;
        }

        public String getName()
        {
            return name;
        }

        public void setTip(String t)
        {
            if (t != null) tip = t;
        }

        public String getTip()
        {
            return tip;
        }

        public String toString()
        {
            return formatOptionName(name);
        }
    }
    
    /*
    *    A 'Primary Option' is the first argument to the command line.
    *    it never has an argument.
    */     
    
    class PrimaryOption extends Option
    {
        Vector options;

        public PrimaryOption(String optionName)
        {
            super(optionName);
            options = new Vector(8);
        }

        public void add(SecondaryOption option)
        {
            options.add(option);
        }

        public int size()
        {
            return options.size();
        }

        public SecondaryOption get(int i)
        {
            return (SecondaryOption) options.get(i);
        }

        /**
         * looks for a named option.  Tries original string, if that files tries again
         * with a preceeding dash.
         */
        public SecondaryOption get(String s)
        {
            SecondaryOption op = (SecondaryOption) CBAutoGUI.getNamedObject(options, s);
            if (op == null && s.charAt(0) != '-')
                op = (SecondaryOption) CBAutoGUI.getNamedObject(options, "-" + s);
            return op;
        }

        public Vector getHiddenOptions()
        {
            Vector ret = new Vector();
            for (int i = 0; i < options.size(); i++)
            {
                SecondaryOption test = (SecondaryOption) options.get(i);
                if (test.isHidden())
                    ret.add(test);
            }
            return ret;
        }

        public String toCommandString()
        {
            return name;
        }

    }
    
    /*
    *    A secondary option appears after the primary option, may
    *    have an argument, and if it does that argument (which is
    *    entered using a text field) may be a hidden password field.
    */     
    
    class SecondaryOption extends Option
    {
        boolean password;
        boolean label;
        boolean hasArgument;
        boolean hidden;
        boolean file;
        String defaultValue = "";

        /**
         * Construct a new option that will appear on a tabbed pane.
         *
         * @param optionString the name of the option, as it appears on the command line, but without the dash
         * @param pwd          whether the option is a password field
         * @param arg          whether the option has an argument (if not, treat it as a check box)
         * @param lbl          whether the 'option' is in fact simply a label (a comment, or extra directions or something)
         * @param hide         whether the option is hidden from the user, and always present.
         */
        public SecondaryOption(String optionString, boolean pwd, boolean arg, boolean lbl, boolean hide, boolean isFile, String tooltip, String defVal)
        {
            super(optionString);
            password = pwd;
            hasArgument = arg;
            label = lbl;
            hidden = hide;
            file = isFile;
            if (tooltip != null)
                tip = tooltip;
            if (defVal != null)
                defaultValue = defVal;
        }

        // handle default values...        
        public void setDefaultValue(String s)
        {
            if (defaultValue != null) defaultValue = s;
        }

        public String getDefaultValue()
        {
            return (defaultValue == null) ? "" : defaultValue;
        }

        public String toCommandString()
        {
            return name;
        }

        public boolean isPassword()
        {
            return password;
        }

        public boolean isLabel()
        {
            return label;
        }

        public boolean isCheckBox()
        {
            return !hasArgument;
        }

        public boolean isHidden()
        {
            return hidden;
        }

        public boolean isFile()
        {
            return file;
        }

    }
}