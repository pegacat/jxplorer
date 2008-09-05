package com.ca.commons.cbutil;

import javax.swing.*;
import java.awt.*;

/**
 * Java layout managers are frustrating, badly written
 * pieces of garbage that are difficult to use, erratic,
 * throw strange errors, and are generally junk.  The
 * only one worth spitting on is GridBagLayout, but it's
 * clumsy and inconvenient to use.<p>
 * <p/>
 * This class wraps up JPanel with an intrinsic GridBagLayout
 * and constraints object, and provides a bunch of new
 * add() ftns to make stuff faster and easier.  For me, anyway :-).<p>
 * <p/>
 * This works nicely for adding single height objects of
 * different widths, using ftns such as add(), addline(),
 * newline(), or addwide().  High objects can be placed
 * explicitly using add(x,y,width,height).<p>
 * <p/>
 * The concept of a 'cursor' position is used, with cells
 * being added to the next 'cursor' position, and the cursor
 * being adjusted with 'newline()', or a fixed positional
 * placement.<p>
 * <p/>
 * <pre>
 *    e.g.
 * <p/>
 *    add(a); add(b); add(c); newLine();
 *    addLine(d);
 *    add(e); addLine(f);
 *    addWide(g,2); addLine(h);
 *    addBig(i,2);
 *    add(j,1,2,3,8)
 * <p/>
 *    should produce something like:
 * <p/>
 *    [ a ] [ b ] [ c ]
 *    [       d       ]
 *    [ e ] [    f    ]
 *    [    g    ] [ h ]
 *    [               ]
 *    [       i       ]
 *    ...    ...   ...
 *    ...    [   j    ]
 * <p/>
 *        (YMMV :-) )
 *    </pre>
 * - Chris
 */

public class CBPanel extends JPanel
{
    GridBagLayout gridbag;
    protected GridBagConstraints c;

    protected int xpos = 0;
    protected int ypos = 0;

    /**
     * Constructor calls the JPanel superclass constructor,
     * and initialises the GridBagLayout and GridBagConstraints
     * objects.
     */
    public CBPanel()
    {
        super(true); // set double buffering on.
        gridbag = new GridBagLayout();
        setLayout(gridbag);
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0;
        c.weighty = 0;
    }

    /**
     * Makes all components added <i>after</i> this call
     * expand to fill all available space.
     */
    public void makeHeavy()
    {
        c.weightx = 1;
        c.weighty = 1;
    }

    /**
     * Makes all components added <i>after</i> this call
     * expand to fill all available width (but not height).
     */
    public void makeWide()
    {
        c.weightx = 1;
        c.weighty = 0;
    }


    /*
     *    Makes all components added <i>after</i> this call
     *    stay their original size, and <i>not</i> fill up any spare space.
     */
    public void makeLight()
    {
        c.weightx = 0;
        c.weighty = 0;
    }


    /**
     * Makes all components added <i>after</i> this call
     * expand to fill all available height (but not width).
     */


    public void makeHigh()
    {
        c.weightx = 0;
        c.weighty = 1;
    }


    /**
     * Used to set the constraint objects x, y, width and
     * height objects.
     *
     * @param X      the X cell position
     * @param Y      the Y cell position
     * @param width  the number of cells spanned in width
     * @param height the number of cells of height
     */
    private void setXYWH(int X, int Y, int width, int height)
    {
        c.gridx = X;
        c.gridy = Y;
        c.gridwidth = width;
        c.gridheight = height;
    }

    /**
     * Add the next component in a straight line, automatically
     * incrementing the X position 'cursor'.
     *
     * @param comp the component to add
     */
    public Component add(Component comp)
    {
        return add(comp, xpos, ypos, 1, 1);
/*
        setXYWH(xpos, ypos, 1, 1);
        add(comp, c);
        xpos++;
        return comp;
*/
    }

    /**
     * Add a unit cell object to a specific x and y
     * position.  The 'cursor' is set to the adjacent
     * x position.
     *
     * @param comp the component to add
     */
    public Component add(Component comp, int x, int y)
    {
        return add(comp, x, y, 1, 1);
/*
        xpos = x;
        ypos = y;
        setXYWH(xpos,ypos,1,1);
        add(comp, c);
        xpos++;
        return comp;
*/
    }

    /**
     * Adds a component to the specified x,y cell position, with
     * a size spanning (width, height) cells.  Cursor resets to
     * the lowest y position (i.e. height), and the adjacent x
     * position (i.e. x+width);
     *
     * @param comp the component to add
     */

    public Component add(Component comp, int x, int y, int width, int height)
    {
//System.out.println("add constraints: " + c.weightx + ", " + c.weighty + " " + comp.toString());
        xpos = x;
        ypos = y;
        setXYWH(xpos, ypos, width, height);
        add(comp, c);
        ypos = y + height - 1;
        xpos = x + width;
        return comp;
    }

    /**
     * Adjusts the cursor to the
     * next line down.  (i.e. set the x cursor to zero,
     * and increments the y cursor).
     */


    public void newLine()
    {
        xpos = 0;
        ypos++;
    }

    /**
     * Adds a component taking up the remainder of the
     * line (using GridBagConstraints.REMAINDER), and
     * increments the cursor to the next line.
     *
     * @param comp the component to add
     */


    public void addLine(Component comp)
    {
        setXYWH(xpos, ypos, GridBagConstraints.REMAINDER, 1);
        add(comp, c);
        newLine();
    }

    /**
     * Adds a component taking up the remainder of the
     * line (using GridBagConstraints.REMAINDER), and
     * increments the cursor to the next line.
     *
     * @param comp the component to add
     */


    public void addln(Component comp)
    {
        addLine(comp);
    }

    /**
     * Adds a large multi - line component at the next cursor position,
     * increments the cursor to the start of the next line
     * immediately below the object.
     *
     * @param comp   the component to add
     * @param height the number of cells height the component is.
     */


    public void addLines(Component comp, int height)
    {
        setXYWH(xpos, ypos, GridBagConstraints.REMAINDER, height);
        add(comp, c);
        xpos = 0;
        ypos += height;
    }

    /**
     * Adds a wider-than-usual component, updating the
     * cursor as appropriate.
     */

    public void addWide(Component comp, int width)
    {
//System.out.println("add wide constraints: " + c.weightx + ", " + c.weighty + " comp: " + comp.toString());
        setXYWH(xpos, ypos, width, 1);
        add(comp, c);
        xpos += width;
    }

    public void addGreedyWide(Component comp)
    {
        addGreedyWide(comp, 1);
    }

    public void addGreedyWide(Component comp, int width)
    {
        double oldx = c.weightx;
        c.weightx = 1;
        addWide(comp, width);
        c.weightx = oldx;
    }

    /**
     * for debugging... gets current state...
     */

    public String toString()
    {
        return ("CBPanel x,y pos: " + xpos + "," + ypos + " constraint weight x,y " + c.weightx + "," + c.weighty);
    }
}