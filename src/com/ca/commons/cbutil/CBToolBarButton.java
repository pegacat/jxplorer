/**
 *
 *
 * Author: Van Bui
 * Date: 15/09/2002 / 16:15:18
 */
package com.ca.commons.cbutil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

/**
 * A button with some pre-defined behaviours. The text, tool tip, icon,
 * and mnemonic could be set in the constructor. The button has an optional
 * icon on top and the text on bottom, center-aligned. The text is normally
 * displayed in black. When the mouse is moved over the button the text turns
 * blue. If alwaysPaintBorder is set to <b>false</b>, the border of the button
 * is only highlighted when the mouse enters the button.
 * <br>
 *
 * @author vbui
 */
public class CBToolBarButton extends JButton
{

    static Logger logger = Logger.getLogger(CBToolBarButton.class.getPackage().getName());

    private boolean alwaysPaintBorder = true;
    private int width = 75;
    private int height = 40;

    /**
     * Constructor.
     *
     * @param label   the text on the button, for example, if this parameter
     *                is set to "E&xit" the button will display the text "Exit" and will
     *                have "x" as its mnemonic.
     * @param tooltip the tool tip text for the button.
     */
    public CBToolBarButton(String label, String tooltip)
    {
        super();
        init(label, tooltip);
    }

    /**
     * Constructor.
     *
     * @param label        the text on the button, for example, if this parameter
     *                     is set to "E&xit" the button will display the text "Exit" and will
     *                     have "x" as its mnemonic.
     * @param iconFileName specify where to find the icon for the button.
     * @param tooltip      the tool tip text for the button.
     */
    public CBToolBarButton(String label, String iconFileName, String tooltip)
    {
        this(label, iconFileName, tooltip, CENTER, BOTTOM);
    }

    /**
     * Constructor.
     *
     * @param label        the text on the button, for example, if this parameter
     *                     is set to "E&xit" the button will display the text "Exit" and will
     *                     have "x" as its mnemonic.
     * @param iconFileName specify where to find the icon for the button.
     * @param tooltip      - the tool tip text for the button.
     * @param horizPos     - the horizontal position for the button text.
     * @param vertPos      - the vertical position for the button text.
     */
    //TODO: Check this out for theme-izing
    public CBToolBarButton(String label, String iconFileName, String tooltip,
                           int horizPos, int vertPos)
    {
        super();
        init(label, tooltip);

        ImageIcon buttonIcon = new ImageIcon(iconFileName);
        setIcon(buttonIcon);
        setHorizontalTextPosition(horizPos);
        setVerticalTextPosition(vertPos);
    }

    /**
     * Leave some space between the icon/text and the edges of the button.
     */
    public Insets getInsets()
    {
        return new Insets(1, 1, 1, 1);
    }

    /**
     * To help the layout manager.
     */
    public Dimension getPreferredSize()
    {
        return new Dimension(width, height);
    }

    /**
     * Initialization.
     */
    public void init(String label, String tooltip)
    {
        setForeground(Color.black);
        if (!alwaysPaintBorder) setBorderPainted(false);

        setText(label);

        setToolTipText(tooltip);

        addMouseListener(new MouseAdapter()
        {
            public void mouseEntered(MouseEvent e)
            {
                setForeground(Color.blue);
                if (!alwaysPaintBorder && isEnabled())
                    setBorderPainted(true);
            }

            public void mouseExited(MouseEvent e)
            {
                setForeground(Color.black);
                if (!alwaysPaintBorder && isEnabled())
                    setBorderPainted(false);
            }
        });

        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
    }

    /**
     * Sets the displayed text on a button.  An optional ampersand
     * character &amp; shows the location of the mnemonic (if any).
     *
     * @param label the button label, optionally including an
     *              ampersanded-mnemonic character
     */

    public void setText(String label)
    {
        if (label != null)
        {
            int pos = label.indexOf('&');
            if (pos >= 0 && pos <= label.length() - 2)
            {
                super.setText(label.substring(0, pos) + label.substring(pos + 1));
                setMnemonic(getText().charAt(pos));
            }
            else
            {
                super.setText(label);
            }
        }
    }

    /**
     * Set the parameter alwaysPaintBorder.
     */
    public void setAlwaysPaintBorder(boolean alwaysPaintBorder)
    {
        this.alwaysPaintBorder = alwaysPaintBorder;
        if (alwaysPaintBorder)
            setBorderPainted(true);
        else
            setBorderPainted(false);
    }

    /**
     * Set preferredSize of the button.
     *
     * @param width  preferred width of the button.
     * @param height preferred height of the button.
     */
    public void setWidthHeight(int width, int height)
    {
        this.width = width;
        this.height = height;
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
    }
}
