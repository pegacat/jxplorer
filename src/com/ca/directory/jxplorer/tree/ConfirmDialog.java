package com.ca.directory.jxplorer.tree;

import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.JXConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import java.util.logging.Logger;

/**
 *    A small GUI utility dialog window, that prompts the user for an
 *    ok/cancel answer, while also allowing them to set the value of
 *    a boolean property that determines whether the dialog is ever
 *    to be displayed.
 */
public class ConfirmDialog extends JDialog implements ActionListener
{

    CBPanel     display; 
    JCheckBox   showAgain;
    Properties  myProperties;
    String      binaryProperty = "";
    CBButton     OK, Cancel;
    boolean     userResponse = false;

    private static Logger log = Logger.getLogger(ConfirmDialog.class.getName());
    /**
     *    This should work similarly to an OptionDialog -
     *    ask the user something, and return a value.  As
     *    an added extra, it will take a properties list, and
     *    the name of a properties object (that should be storing
     *    'true' and 'false' values), and use it for a 'show this
     *    dialog again' message.  If the user selects no, the
     *    property will be modified to 'false' and the dialog
     *    will not operate until it has been set to 'true' by
     *    some external means.
     *    @param Msg the message to display to the user.
     *    @param binaryProperty the property name (key) that sets whether to show the
     *           dialog.  (may be null.)
     *    @param parent the gui parent to use when displaying the dialog
     */

    public ConfirmDialog(String Msg, String binaryProperty, Frame parent)
    {
        super(parent);
        this.myProperties = JXConfig.myProperties;
        this.binaryProperty = binaryProperty;
        //XXX should really check Msg string length...
        setSize(new Dimension(300,120));
        getContentPane().add(display = new CBPanel());
        
        display.addln(new JLabel(Msg));
        JPanel buttons = new JPanel();
        buttons.add(OK = new CBButton(CBIntText.get("OK"), CBIntText.get("Click here to confirm.")));
        buttons.add(Cancel = new CBButton(CBIntText.get("Cancel"), CBIntText.get("Click here to cancel.")));
        display.addln(buttons);
        
        OK.addActionListener(this);
        Cancel.addActionListener(this);
        
        if ((myProperties != null) && (binaryProperty != null) && (myProperties.getProperty(binaryProperty) != null))
        {
            showAgain = new JCheckBox(CBIntText.get("show this dialog every time"), true);
            display.addln(showAgain);
            showAgain.addActionListener(this);
        } 
        else
            showAgain = null;
            
        setVisible(false);    
    }

    /**
     *    the main method to call; this shows the dialog, makes it modal, 
     *    gets a response, unmodalifies the window, hides the dialog, and returns
     *    the value to the user.
     */
    public boolean getUserResponse()
    {
System.out.println("binary prop " + binaryProperty);    
System.out.println("myProperties " + !(myProperties==null));    

        if ("false".equals(myProperties.getProperty(binaryProperty)))
            return true;            // property says not to show this dialog.
        
        if (showAgain == null) return true;  // confirm not set up.
        
        showAgain.setSelected(true);
        
        setVisible(true);
        showAgain.setSelected(true);
        
        startModal();        // this blocks until the user has done stuff...
        showAgain.setSelected(true);

        //setVisible(false);   redundant
        return userResponse;
    }
    
    /**
     *    Traditional action loop.  Either of the buttons will cause the
     *    Modality to stop and the getValue() method to return.
     */
    public void actionPerformed(ActionEvent ev)
    {
        
        if (ev.getSource() == showAgain)
        {
            //System.out.println("setting button to: " + showAgain.isSelected());
            myProperties.setProperty(binaryProperty, String.valueOf(showAgain.isSelected()));
            showAgain.repaint();
        }            
        else if (ev.getSource() == OK)
        {
            //System.out.println("Ok selected");
            userResponse = true;
            stopModal();
            setVisible(false);
        }
        else if (ev.getSource() == Cancel)
        {
            //System.out.println("Cancel selected");
            userResponse = false;
            stopModal();
            setVisible(false);
        }
        else
        {
            //System.out.println("huh? \n" + ev.toString());
        }    
    }
    
    
    /*
     * Creates a new EventDispatchThread to dispatch events from. This
     * method returns when stopModal is invoked.
     * (Code stolen verbatim from JInternalFrame)
     */
    synchronized void startModal() {
    /* Since all input will be blocked until this dialog is dismissed,
     * make sure its parent containers are visible first (this component
     * is tested below).  This is necessary for JApplets, because
     * because an applet normally isn't made visible until after its
     * start() method returns -- if this method is called from start(),
     * the applet will appear to hang while an invisible modal frame
     * waits for input.
     */
    if (isVisible() && !isShowing()) {
        Container parent = this.getParent();
        while (parent != null) {
        if (parent.isVisible() == false) {
            parent.setVisible(true);
        }
        parent = parent.getParent();
        }
    }

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                EventQueue theQueue = getToolkit().getSystemEventQueue();
                while (isVisible()) {
                    // This is essentially the body of EventDispatchThread
                    AWTEvent event = theQueue.getNextEvent();
                    Object src = event.getSource();
                    if (src instanceof Component) {
                          ((Component) src).dispatchEvent(event);
                      } else if (src instanceof MenuComponent) {
                          ((MenuComponent) src).dispatchEvent(event);
                      } else {
                          log.warning("unable to dispatch event: " + event);
                      }
                }
            } else
                while (isVisible())
                    wait();
        } catch(InterruptedException e){}
    }
  
    /*
     * Stops the event dispatching loop created by a previous call to
     * <code>startModal</code>.
     */
    synchronized void stopModal() {
        notifyAll();
    }
  

}