package com.ca.commons.cbutil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/**
 * <p>This utility class forms the basis of a generic 'pop up'
 * dialog, that can either be extended, or used as is.  It is
 * basically a JDialog with built in 'OK', 'Cancel' and 'Help'
 * buttons (the help pre-wired into CBHelpSystem), and a 'main
 * display panel' that is a CBPanel (i.e. has a built in
 * grid bag layout manager and utility methods).</p>
 * <p/>
 * <p>If the class is being extended, 'ok' and 'cancel' behaviour
 * can be modified by extending the 'doOK()' and 'doCancel()'
 * methods, and extra components may be added in the extended
 * constructor.</p>
 * <p/>
 * <p>If the class is used as is, a new action listener may
 * be added to the 'OK' button after removing the old action
 * listener, and new components may be added directly to the
 * internal CBPanel 'display' object.</p>
 */

public class CBDialog extends JDialog
{
    boolean cancelled = false;

    public CBButton OK, Cancel, Help;     // Initialise the bottom buttons that actually do stuff

    /**
     * This is the main display component.  The display panel and
     * the buttonPane are the two top level components shown.
     */

    public CBPanel display;

    /**
     * This is the parent Frame of the dialog
     */

    //TODO: refactor so we can use something other than a frame as an owner?
    protected Frame owner;

    /**
     * This pane contains the 'ok', 'cancel' and 'help' buttons.
     */

    protected JPanel buttonPanel;

    public CBDialog(Frame owner, String title, String helpLink)
    {
        super(owner); // create modal dialog ...

        this.owner = owner;

        setModal(true);

        setTitle(title);

        display = new CBPanel();

        Container pane = getContentPane();

        pane.setLayout(new BorderLayout());

        buttonPanel = new JPanel();
        buttonPanel.add(OK = new CBButton(CBIntText.get("OK"), CBIntText.get("Click here to make the changes.")));
        buttonPanel.add(Cancel = new CBButton(CBIntText.get("Cancel"), CBIntText.get("Click here to exit.")));

        setHelpLink(helpLink);

        pane.add(display);
        pane.add(buttonPanel, BorderLayout.SOUTH);

        //TE: better way to implement keystroke listening...
        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "enter");
        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
        display.getActionMap().put("enter", new MyAction(CBAction.ENTER));
        display.getActionMap().put("escape", new MyAction(CBAction.ESCAPE));

        Cancel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                doCancel();
            }
        });

        OK.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                doOK();
            }
        });

        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                doCancel();
            }
        });
    }

    /**
     * Apparently it is better to use key bindings rather than adding a KeyListener...
     * "for reacting in a special way to particular keys, you usually should use key
     * bindings instead of a key listener".
     * This class lets the user set the key as an int.  If a key is pressed and it
     * matches the assigned int, a check is done for if it is an escape or enter key.
     * (27 or 10).  If escape, the doCancel method is called.  If enter, the doClick
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
         * doCancel is called if the Esc key pressed,
         * OK.doClick is called if Enter key is pressed.
         *
         * @param e never used.
         */
        public void actionPerformed(ActionEvent e)
        {
            if (getKey() == ESCAPE)
                doCancel();
            else if (getKey() == ENTER)
                OK.doClick();
        }
    }

    /**
     * @param helpLink
     */
    public void setHelpLink(String helpLink)
    {
        if (helpLink != null)
        {
            buttonPanel.add(Help = new CBButton(CBIntText.get("Help"), CBIntText.get("Click here for Help.")));
            CBHelpSystem.useDefaultHelp(Help, helpLink);
        }
    }

    /**
     * Gets the display panel.
     *
     * @return the display panel
     */
    public CBPanel getDisplayPanel()
    {
        return display;
    }

    /**
     * Chaining method to internal CBPanel display object.
     * (@see com.ca.directory.util.CBPanel)
     */

    public Component add(Component comp)
    {
        display.add(comp);
        return comp;
    }

    /**
     * Chaining method to internal CBPanel display object.
     * (@see com.ca.directory.util.CBPanel)
     */

    public void addln(Component comp)
    {
        display.addln(comp);
    }

    /**
     * Chaining method to internal CBPanel display object.
     * (@see com.ca.directory.util.CBPanel)
     */

    public void makeHeavy()
    {
        display.makeHeavy();
    }

    /**
     * Chaining method to internal CBPanel display object.
     * (@see com.ca.directory.util.CBPanel)
     */

    public void makeLight()
    {
        display.makeLight();
    }

    /**
     * Chaining method to internal CBPanel display object.
     * (@see com.ca.directory.util.CBPanel)
     */

    public void makeWide()
    {
        display.makeWide();
    }


    /**
     * When the user hits 'cancel', the window is shut down.
     */

    public void doCancel()
    {
        cancelled = true;
        quit();
    }

    /**
     * Returns whether the dialog was cancelled.
     * (Useful after a 'setVisible(true)' to discover if the
     * user left in a huff.)
     */

    public boolean wasCancelled()
    {
        return cancelled;
    }

    /**
     * Default behaviour is the same as cancel.  Over-ride to
     * give appropriate 'OK' behaviour.
     */

    public void doOK()
    {
        quit();
    }

    /**
     * Closes the window and disposes the gui.
     * Called by doOK() and doCancel().
     */

    public void quit()
    {
        setVisible(false);
        dispose();
    }
}