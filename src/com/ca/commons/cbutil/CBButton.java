package com.ca.commons.cbutil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * Simple class that extends JButton to give the button the affact of a rollover.
 * On mouse entered, the text changes colour to blue.
 *
 * @author Trudi.
 */

public class CBButton extends JButton
{


    /**
     * Constructor that calls CBButton(String text, String tooltip, Icon icon) with a null icon.
     *
     * @param text    the button label.
     * @param tooltip the button tooltip.
     */

    public CBButton(String text, String tooltip)
    {
        this(text, tooltip, null);
    }


    /**
     * Constructor that calls CBButton(String text, String tooltip, Icon icon) with a null icon.
     *
     * @param text the button label.
     * @param icon the icon to put on the button.
     */

    public CBButton(String text, Icon icon)
    {
        this(text, "", icon);
    }


    /**
     * Constructor that makes the button.  It adds the tooltip and a mouse listener
     * to create the rollover affect.
     *
     * @param text    the button label.
     * @param tooltip the button tooltip.
     * @param icon    the icon to put on the button.
     */

    public CBButton(String text, String tooltip, Icon icon)
    {
        super(text, icon);

        if (tooltip != null)
            setToolTipText(tooltip);

        addMouseListener(new MouseAdapter()
        {
            public void mouseEntered(MouseEvent e)
            {
                setForeground(Color.blue);
            }

            public void mouseExited(MouseEvent e)
            {
                setForeground(Color.black);
            }
        });
    }
}
