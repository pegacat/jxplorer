package com.ca.commons.cbutil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

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
public abstract class CBAction
        implements Action
{
    /**
     * Represents the escape key.
     */
    public static final int ESCAPE = 27;

    /**
     * Represents the enter key.
     */
    public static final int ENTER = 10;

    /**
     * User sets the int representation of the key they want to listen for.
     */
    private int key = 0;

    /**
     * Whether the listener is enabled.
     */
    private boolean enabled = true;

    /**
     * Empty constructor.
     */
    public CBAction()
    {
    }

    /**
     * Constructor that sets the key that is Action is associated with.
     *
     * @param key
     */
    public CBAction(int key)
    {
        this.key = key;
    }

    /**
     * Gets the key.
     *
     * @return the key
     */
    public int getKey()
    {
        return key;
    }

    /**
     * Sets the key.
     *
     * @param key an int representation of the keyboard key.
     */
    public void setKey(int key)
    {
        this.key = key;
    }

    /**
     * Subclasses to implement this method.
     *
     * @param e
     */
    public abstract void actionPerformed(ActionEvent e);

    //TE: implemented methods...
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
    }

    public Object getValue(String key)
    {
        return key;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void putValue(String key, Object value)
    {
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
    }

    public void setEnabled(boolean b)
    {
        enabled = b;
    }
}
