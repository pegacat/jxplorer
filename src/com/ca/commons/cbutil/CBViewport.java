package com.ca.commons.cbutil;

import javax.swing.*;
import java.awt.*;


/**
 * JViewport appears to be more than usually stuffed...
 * this class inherits from it, and lobotimises 'scrollRectToVisible'
 * to prevent the bizarre automatically-scroll-to-the-bottom
 * behaviour that was screwing up the browser...
 *
 * @author Chris Betts
 * @version 1.0 22/8/99
 * @see JViewport
 */
public class CBViewport extends JViewport
{

    /**
     * Labotimised so that the bloody thing works.  Something
     * was calling this function and forcing the window to always
     * be at the bottom; otherwise it doesn't seem to be needed :-)
     * ... hopefully Sun will fix stuff up and we can go back to
     * using JViewport some time...
     *
     * @param contentRect the Rectangle to display
     */

    public void scrollRectToVisible(Rectangle contentRect)
    {
        // code removed to protect the innocent
    }
}