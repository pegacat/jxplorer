package com.ca.commons.cbutil;

import javax.swing.*;
import java.awt.*;
import java.io.Closeable;

/**
 * A component so cunning you could stick a tail on it
 * and call it a weasel.<p>
 * <p/>
 * The basic idea is to create an easy interface to a
 * progress bar, while navigating a tree.  An object of
 * CBpbar class handles the pbar, expecting calls from
 * a separate thread, and enables
 * the programmer to update the pbar with a minimum of
 * fuss.  The class is heavily optimised towards tree
 * navigation; it will guess completion depending on what
 * part of a (partially navigated) tree it is in, as well
 * as keeping track of total objects written for label
 * display.<p>
 */

//    Programmer note: written with a ProgressMonitor object,
//    rather than extending ProgressMonitor, 'cause it
//    seemed easier to do the SwingUtility.invokeLater()
//    magic that way...

public class CBpbar implements Closeable
{
    /**
     * Constructor creates progress bar, and starts it up
     * in a separate thread...
     */

    ProgressMonitor pbar;

    int count;                         // number of nodes operated on

    int pcntg;                         // (estimated) percentage complete

    int level;                         // current depth in tree

    static int MAXLEVEL = 6;           // not interested in estimating percentage complete
                                        // below this level in the tree (percentage pts are integral)
    
    int fanout[] = new int[MAXLEVEL];   // total branches at each level - initialised to zero

    int visited[] = new int[MAXLEVEL];  // branches visited at each level - initialised to zero

    String notePrefix;                  // text prefixing the count number in the display

    /**
     * Constructor for progress bar.
     *
     * @param C          a graphical 'hook' so the Progress Monitior knows where to
     *                   display itself - usually a Swing Component of some sort...
     * @param title  a general desciption that appears in the box
     * @param prefix the active description, that prefixes the changing
     *                   'count' value that the progress bar reports.
     */

    public CBpbar(Component C, String title, String prefix)
    {
        pbar = new ProgressMonitor(C, title, prefix + " 0", 0, 100);
        notePrefix = prefix;
        level = 0;
        fanout[level] = 1;      // the root node is unitary.

        //System.out.println("starting pbar (" + pbar.getNote() + ") " + Thread.currentThread().toString());
    }

    public void setPrefix(String prefix)
    {
        notePrefix = prefix;
        pbar.setNote(prefix);
    }

    /**
     * Set the progress bar back to its starting state.
     * @param prefix the active description, that prefixes the changing
     */
    public void reset(String prefix)
    {
        setPrefix(prefix);
        level = 0;
        fanout[level] = 1;
    }

    /**
     * increments the displayed count by one,
     * and (depending on the depth we're at, and
     * the fanout set by @push) changes the percentage fill
     * of the bar.  Note that while every call of this makes
     * a request to SwingUtilities.invokeLater(), not every call
     * actually results in a visible update (Swing queues these
     * request in order to avoid starving worker threads).
     */

    public void inc()
    {
        count++;
        int oldpcntg = pcntg;
        if (level < MAXLEVEL && level >= 0)
        {

    // moderate mathematical cunning here.  Attempting (fairly brutaly)
    // to establish what proportion of the tree has been visited, by
    // assuming a perfectly 'balanced' tree, and toting up the bits
    // that have been visited.  This is computationally inefficient,
    // but we have CPU cycles to spare, right?

            visited[level]++;
            pcntg = 0;
            int spread = 100;
            for (int i = 0; i < level; i++)
            {
                pcntg += (spread * visited[i]) / fanout[i];
                spread = spread / fanout[i];
            }

            if (pcntg != oldpcntg)
            {
                pbar.setProgress(pcntg);
                pbar.setNote(notePrefix + " " + count);
            }
        }
    }

    /**
     * pop tells the progress bar that the tree process we are
     * tracking has gone up a level.
     */

    public void pop()
    {
        if (level > 0)
            level--;
    }

    /**
     * push tells the progress bar that the process we are
     * tracking has gone down a level, and that this level
     * has a certain number of entries (i.e. branches).  The
     * progress bar uses this info for estimating (based on
     * the assumption of a balanced tree) the proportion of
     * the tree processed.
     *
     * @param fanout the number of branches at this level.
     */
    public void push(int fanout)
    {
        level++;
        if (level < MAXLEVEL) this.fanout[level] = fanout;
    }

    /**
     * Closes the progress bar.
     */

    public void close()
    {
        //System.out.println("closing pbar (" + pbar.getNote() + ") " + Thread.currentThread().toString());

        pbar.close();
    }

    /**
     * returns whether the user has hit the 'cancel' button on the
     * progress bar.
     *
     * @return the canceled status of the bar.
     */

    public boolean isCanceled()
    {
        return pbar.isCanceled();
    }

    public ProgressMonitor getBaseMonitor() {return pbar;}

}