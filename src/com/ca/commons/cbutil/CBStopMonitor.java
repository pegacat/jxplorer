package com.ca.commons.cbutil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A smart button that, when pressed, will interrupt a
 * (single) registered thread... (could conceivably be
 * expanded in future to handle lists of stoppable threads)
 */

public class CBStopMonitor extends JButton
{
    Thread thingToStop = null;
    boolean debugStopMon = false;
    CBpbar pbar = null;           // the progress bar of the stopped process (if any)


    /**
     * Initialise the stop monitor with the given text and icon (for display
     * in the button) and creates an action monitor to (on a button press)
     * stop any registered thread.
     *
     * @param text the button text
     * @param icon the (optional) display icon (may be null)
     */

    public CBStopMonitor(String text, Icon icon)
    {
        super(text, icon);

        addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (debugStopMon) System.out.println("StopMon button pressed; about to stop " + ((thingToStop == null) ? "null" : thingToStop.getName()));
                stop(thingToStop);
            }
        });
    }

    /**
     * Registers a thread to monitor and possibly stop.  An (optional)
     * progress bar can also be passed in, to be also closed if
     * the thread is stopped by the user.  Use of this register ftn.
     * <i>automatically stops any pre-existing thread</i>.
     *
     * @param stopMe  the thread to monitor, and possibly stop
     * @param newPbar a progress bar to close if the thread is stopped (may be null)
     */

    public void register(Thread stopMe, CBpbar newPbar)
    {
        if (debugStopMon) System.out.println("StopMon registering thread " + ((stopMe == null) ? "null" : stopMe.getName()));
        stop(thingToStop);             // stop any existing monitored thread...

        pbar = newPbar;
        thingToStop = stopMe;
        setEnabled(true);
    }

    /**
     * Stop monitoring a particular thread.  Since there can only be one
     * thread, the paramater thread must equal the montored thread, otherwise
     * it is ignored (thus the ftn. is safe to call, even if a thread has
     * already been freed and replaced with another).  Also closes the progress
     * bar (if any, and only if the parameter thread is indeed the same as the
     * monitored thread).
     *
     * @param ignoreMe the thread to de-register.
     */

    public void free(Thread ignoreMe)
    {
        if (debugStopMon) System.out.println("StopMon de-registering thread " + ((ignoreMe == null) ? "null" : ignoreMe.getName()));

        if (thingToStop == ignoreMe)
        {
            thingToStop = null;
            if (pbar != null) pbar.close();    // if we have a progress bar, close it.
        }
        setEnabled(false);
    }

    /**
     * Actually stop a particular thread (not just stop monitoring, actually
     * stop the thread).  Called automatically by the button action listener,
     * can also be called externally.  Also clears any associated progress bar.
     *
     * @param stopMe the thread to stop (must be the monitored thread for anything
     *               to happen...)
     */

    public void stop(Thread stopMe)
    {
        if (debugStopMon) System.out.println("StopMon asked to stop thread " + ((stopMe == null) ? "null" : stopMe.getName()));

        if (thingToStop == stopMe)
        {
            if (stopMe == null) return;
            if (pbar != null) pbar.close();      // if we have a progress bar, close it.
            thingToStop.interrupt();
            try
            {
                (Thread.currentThread()).sleep(10);
            }  // pause 10 ms
            catch (InterruptedException e)
            {
            }

            if (thingToStop.isInterrupted() == false)  // we tried to be nice but...
            {
                if (debugStopMon) System.out.println("attempting to force thread stoppage");
//                thingToStop.stop();
            }
            else
            {
                if (debugStopMon) System.out.println("StopMon interrupting thread " + ((thingToStop == null) ? "null" : thingToStop.getName() + " :" + thingToStop.isInterrupted()));
            }
            thingToStop = null;
            setEnabled(false);
        }
        else if (debugStopMon) System.out.println("unable to stop: " + stopMe.getName() + " - not registered");
    }

    /**
     * Returns whether the stop monitor is currently monitoring a thread.
     *
     * @return the status of the stop monitor
     */
    public boolean isBusy()
    {
        return (isEnabled());
    }

    /**
     * Ask user if they wish to abandon the current operation, if such exists.
     * Otherwise return true.
     *
     * @return whether to continue with threading operation
     */
    public boolean abandonAnyExistingOperation()
    {
        if (thingToStop != null)
        {
            int option = JOptionPane.showConfirmDialog(this,
                    "You are already doing an operation.  Cancel the old one and start a new operation?",
                    "Cancel Old Operation", JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.YES_OPTION)
            {
                stop(thingToStop);
                return true;
            }
            else
                return false;        // keep going with current operation!
        }
        return true;
    }

}