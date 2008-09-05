package com.ca.commons.cbutil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Provides a small multi-line error reporting window, with an
 * option to pull down a detailed error message.
 */

public class CBErrorWin extends JDialog
{

    String msg;
    String error;
    String title;
    boolean haveErrorMsg;
    CBButton OK, Details;
    CBPanel display;
    Exception exception;

    /**
     * Constructor - take a parent frame, a message,
     * and a detailed error message (may be null).
     * <p/>
     * This creates a short multi-line text displayed, with a
     * 'details' button which expands the display to show the
     * full error message as well.
     *
     * @param owner the component (from which the parent Frame will be derived)
     * @param Msg   a short one line message to display to the user
     * @param e     the exception to log
     */

    public CBErrorWin(Dialog owner, String Msg, Exception e)
    {
        super(owner);
        title = CBIntText.get("Error Encountered");
        commonConstructor(Msg, e);
    }

    /**
     * Constructor - take a parent frame, a message,
     * and a detailed error message (may be null).
     * <p/>
     * This creates a short multi-line text displayed, with a
     * 'details' button which expands the display to show the
     * full error message as well.
     *
     * @param owner the component (from which the parent Frame will be derived)
     * @param Msg   a short one line message to display to the user
     * @param e     the exception to log
     */

    public CBErrorWin(Frame owner, String Msg, Exception e)
    {
        super(owner);
        title = CBIntText.get("Error Encountered");
        commonConstructor(Msg, e);
    }


    /**
     * Constructor - takes a parent frame, a confirmation message,
     * and creates a short multi-line text display.
     *
     * @param owner the component (from which the parent Frame will be derived)
     * @param Msg   a short one line confirmation message to display to the user.
     */

    public CBErrorWin(Frame owner, String Msg, String msgTitle)
    {
        super(owner);
        title = msgTitle;
        commonConstructor(Msg, null);
    }


    /**
     * This creates a short multi-line text displayed, with a
     * 'details' button which expands the display to show the
     * full error message as well.
     *
     * @param Msg a short one line message to display to the user.
     * @param e   the exception to log.
     */

    public void commonConstructor(String Msg, Exception e)
    {
        setModal(true);

        exception = e;

        setTitle(CBIntText.get(title));

        msg = (Msg == null) ? CBIntText.get("No Message Given") : Msg;

        haveErrorMsg = (e != null);
        error = (haveErrorMsg) ? e.toString() : CBIntText.get("No specific information");

        JScrollPane scrollPane = new JScrollPane(makeTextArea(msg));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);	//TE: no horizontal scroll bar.

        display = new CBPanel();
        display.makeHeavy();

        display.addLine(scrollPane);

        display.makeLight();
        JPanel buttons = new JPanel();

        buttons.add(OK = new CBButton(CBIntText.get("OK"), CBIntText.get("Click to close error window.")));

        if (haveErrorMsg != false) 	//TE: The 'Details' button is only added if there is a message to diplay
            buttons.add(Details = new CBButton(CBIntText.get("Details"), CBIntText.get("Click to display the full error message.")));

        display.add(buttons);

        OK.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
                dispose();
            }
        });

        if (haveErrorMsg != false)		//TE: Only add the listener if there is a message (otherwise the 'Details' button is not added).
        {
            Details.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    JScrollPane scrollPane2 = new JScrollPane(makeTextArea(msg));
                    final JScrollPane newPane = new JScrollPane(makeTextArea(error));

                    TitledBorder border = new TitledBorder(CBIntText.get("error details"));
                    newPane.setBorder(border);

                    CBErrorWin.this.getContentPane().remove(display);

                    display = new CBPanel();

                    display.makeHeavy();

                    display.addLine(scrollPane2);
                    display.addLine(newPane);

                    display.makeLight();

                    /*
                     *    Add a 'print stack' button
                     */

                    JPanel buttons2 = new JPanel();

                    if (exception != null)
                    {
                        JButton StackTrace = new JButton(CBIntText.get("Print Stack"));
                        StackTrace.setToolTipText(CBIntText.get("Prints a stack trace to the console (if active)"));
                        StackTrace.addActionListener(new ActionListener()
                        {
                            public void actionPerformed(ActionEvent e)
                            {
                                StringWriter sw = new StringWriter();
                                exception.printStackTrace(new PrintWriter(sw));
                                String trace = sw.toString();
                                newPane.setViewportView(makeTextArea(trace));
                                exception.printStackTrace(); // echo to console.
                            }
                        });
                        buttons2.add(StackTrace);
                    }

                    buttons2.add(OK);
                    display.add(buttons2);

                    CBErrorWin.this.setContentPane(display);
                    CBErrorWin.this.setSize(new Dimension(getWidth(), getHeight() + 100));
                    CBErrorWin.this.setVisible(true);
                }
            });
        }

        //TE: better way to implement keystroke listening...

        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "enter");
        display.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
        display.getActionMap().put("enter", new MyAction(CBAction.ENTER));
        display.getActionMap().put("escape", new MyAction(CBAction.ESCAPE));

        setContentPane(display);
        setSize(new Dimension(400, 150));
        CBUtility.center(this, getOwner());
        setVisible(true);
    }


    /**
     * Apparently it is better to use key bindings rather than adding a KeyListener...
     * "for reacting in a special way to particular keys, you usually should use key
     * bindings instead of a key listener".
     * This class lets the user set the key as an int.  If a key is pressed and it
     * matches the assigned int, a check is done for if it is an escape or enter key.
     * (27 or 10).  If escape or enter is pressed, the window is closed and the dialog
     * disposed.
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
         * Closes the window and disposes the dialog.
         *
         * @param e never used.
         */
        public void actionPerformed(ActionEvent e)
        {
            setVisible(false);
            dispose();
        }
    }


    /**
     * Constructs a text area with its initial text set.  Sets the back ground colour,
     * disables the text area, sets the text colour to black and allows line wrapping by word.
     *
     * @param text the text to be automatically set in the text field.
     * @return the JTextArea component.
     */

    protected JTextArea makeTextArea(String text)
    {
        JTextArea a = new JTextArea(text);

        a.setBackground(CBErrorWin.this.getBackground());
        a.setEnabled(true);	// CB: make 'true' to allow copy and paste!
        a.setDisabledTextColor(Color.black);	//TE: sets the disabled text colour to black.
        a.setLineWrap(true);     //TE: allows line wrapping.
        a.setWrapStyleWord(true);     //TE: sets line wrapping by word.


        /*
         *  Initially component was disabled, since we wanted hitting 'enter' to close the window (and we don't want
         *  the users editing the message).  However, we also want them to be able to copy and paste... so I've
         *  enabled the window but added a specific key listener to over-ride the default text area behaviour.
         */
        a.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "enter");  // nb - use specific 'WHEN_FOCUSED', rather than more general 'WHEN_IN_FOCUSED_WINDOW', otherwise Swing default focus behaviour will get priority.
        a.getActionMap().put("enter", new MyAction(CBAction.ENTER));

        return a;
    }
}         