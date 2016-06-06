/*
 * HelpSystem.java
 * (modified from rescoHelpBroker, courtesy Irina)
 * Copyright (c) 1999 by Computer Associates, Inc. All Rights Reserved.
 * @author Chris Betts
 */

package com.ca.commons.cbutil;

import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper around the JavaHelp system.
 */
public class CBHelpSystem
{
    private String helpSetName;			// name of the help set
    private HelpSet helpSet = null;		// manages data
    private HelpBroker helpBroker;		// manages help viewer

    private ClassLoader myClassLoader;

    boolean setup = false;

    private static CBHelpSystem defaultHelpSystem;      // a 'global' help set, usually the first constructed.

    private static Point defaultLocation = new Point(100,100);

    private static Logger log = Logger.getLogger(CBHelpSystem.class.getName());

    /**
     * fires up the Help DataBroker, using the passed help file name (which
     * is checked for localisation).
     *
     * @param helpset_Name name of the help set (and path)
     */

    public CBHelpSystem(String helpset_Name)
    {
        this(helpset_Name, null, null);
    }

    public CBHelpSystem(String helpset_Name, Dimension initialSize)
    {

        this(helpset_Name, null, initialSize);
    }

    /**
     * fires up the Help DataBroker, using the passed help file name (which
     * is checked for localisation).
     *
     * @param helpset_Name name of the help set (and path)
     * @param cl           the class loader used to load the help set url.
     */
//TE:  XXXXXXXXXX why does this get run twice on start up?
    public CBHelpSystem(String helpset_Name, ClassLoader cl, Dimension initialSize)
    {
        if (cl != null) myClassLoader = cl;
        if (helpset_Name != null) helpSetName = helpset_Name;

        if (setup == true) return;  // only run once.

        if (myClassLoader == null)
            myClassLoader = getClass().getClassLoader();

        helpSet = getHelpSet(helpSetName, myClassLoader);

        if (helpSet == null)
        {
            log.warning("Error creating help set!  Can't find: " + helpSetName);
            return;  // leaving setup==false in case we want to try again...
        }
        else
        {
            helpBroker = helpSet.createHelpBroker();
            if (initialSize != null)
                helpBroker.setSize(initialSize);
        }

        log.info("Initial HelpSet created: " + helpSet.toString());

        setup = true;  // we've now constructed the help object!        if (defaultHelpSystem == null)

        defaultHelpSystem = this;
    }

    public boolean ready()
    {
        return setup;
    }



    public synchronized HelpSet getHelpSet()
    {
        return helpSet;
    }

    public synchronized HelpBroker getHelpBroker()
    {
        return helpBroker;
    }

    /**
     * Uses the specified class loader (or the default, if the class loader is
     * null) to look up the named help set, and return it.
     *
     * @param newHelpSetName the class loader to use for looking up the help set - or null
     *                       if the default class loader is to be used.
     * @param loader         the base name of the help set (without any
     *                       localisation extensions.)
     */

    public static HelpSet getHelpSet(String newHelpSetName, ClassLoader loader)
    {
        HelpSet newHelpSet = null;
        try
        {
            URL url = HelpSet.findHelpSet(loader, newHelpSetName);
            if (url != null)
                newHelpSet = new HelpSet(loader, url);
        }
        catch (NoClassDefFoundError ec)
        {
            log.log(Level.WARNING, "Help DataBroker initialization error: couldn't find help set ", ec);
        }
        catch (ExceptionInInitializerError ex)
        {
            log.log(Level.WARNING, "Help DataBroker initialization error: ", ex);
            ex.getException().printStackTrace();  // very rare error - keep for debugging...
        }
        catch (Exception ee)
        {
            log.log(Level.WARNING, "Help Set '" + newHelpSetName + "' not found");
        }

        return newHelpSet;
    }

    /**
     * Appends a new help set to the main help set - useful for plugin help sets.
     *
     * @param loader        - the class loader used to find the plugin help set
     * @param pluginHelpSet - the
     */

    public void addHelpSet(String pluginHelpSet, ClassLoader loader)
    {
        if (helpSet != null)
            helpSet.add(getHelpSet(pluginHelpSet, loader));
        else // we don't have a help set - grab this one and use it.
        {
            helpSet = getHelpSet(pluginHelpSet, loader);
            if (helpSet != null)
                helpBroker = helpSet.createHelpBroker();
        }
    }

    public void addHelpSet(HelpSet newSet)
    {
        if (newSet == null) return;

        if (helpSet != null)
            helpSet.add(newSet);
        else // we don't have a help set - grab this one and use it.
        {
            helpSet = newSet;
            if (helpSet != null)
                helpBroker = helpSet.createHelpBroker();
        }
    }


    /**
     * Show the help window
     */

    public void open()
    {
        if (helpBroker != null)
        {
            helpBroker.setLocation(defaultLocation); // if we ever want to try to center the help browser location, we might start here...
            helpBroker.setDisplayed(true);
        }
    }


    /**
     * Show the help window with the specified tab opened.
     * If there is an error (IllegalArgumentException) open() is called.'
     *
     * @param tab the tab name...options should be either 'TOC', 'Index' or 'Search'.
     */

    public void openTab(String tab)
    {
        if (helpBroker != null)
        {
            try
            {
                helpBroker.setCurrentView(tab);
            }
            catch (IllegalArgumentException ia)
            {
                log.warning("Caught Illegal Argument Exception - an incorrect Help view was asked for: "
                        + ia + "\n\nOpening help with default view...");
                open();
                return;
            }
            helpBroker.setLocation(defaultLocation); // if we ever want to try to center the help browser location, we might start here...
            helpBroker.setDisplayed(true);
        }
    }


    /**
     * Shows the help window at a defined location (specifically set up for dialog help buttons).
     *
     * @param contentID the location (or topic ID) to be displayed when the help opens.
     */

    public void open(String contentID)
    {
        if (helpBroker != null)
        {
            helpBroker.setLocation(defaultLocation); // if we ever want to try to center the help browser location, we might start here...
            helpBroker.setDisplayed(true);
        }

        try
        {
            helpBroker.setCurrentID(contentID);
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "Caught exception: ", e);
        }
    }


    public static void setdefaultHelpSystem(CBHelpSystem set)
    {
        defaultHelpSystem = set;
    }

    public static CBHelpSystem getDefaultHelpSystem()
    {
        return defaultHelpSystem;
    }

    public static void addToDefaultHelpSystem(String newHelpSet, ClassLoader loader)
    {
        log.info("attempting to add help set: '" + newHelpSet + "' using loader: " + loader.toString());

        if (defaultHelpSystem == null)
        {
            CBHelpSystem newDefault = new CBHelpSystem(newHelpSet, loader, null);
            //HelpSet bloop = getHelpSet(newHelpSet, loader);
            log.info("trying to set default help system to: " + ((newDefault == null) ? "null" : newDefault.toString()));
            if (newDefault != null)
                setdefaultHelpSystem(newDefault);
        }
        else
        {
            HelpSet bloop = getHelpSet(newHelpSet, loader);
            log.info("found help set: " + ((bloop == null) ? " null " : bloop.toString()));
            defaultHelpSystem.addHelpSet(bloop);
        }
    }


    /**
     * Sets up a help button with a listener that when clicked will open the
     * at the defined location in java help.  Solves the modal problem too.
     *
     * @param button     the help button.
     * @param helpString the map ID (java help location that is to be displayed).
     */

    public static void useDefaultHelp(JButton button, String helpString)
    {
        // do we want to make a global default help broker...?
        try
        {

            if (helpString == null)
                return; // nothing to do.

            if (defaultHelpSystem == null)
                log.warning("No default HelpSystem.");

            if (defaultHelpSystem == null || defaultHelpSystem.ready() == false)
            {
                log.warning("using default help system with no name...");
                defaultHelpSystem = new CBHelpSystem(null);
            }

            HelpBroker helpBroker = defaultHelpSystem.getHelpSet().createHelpBroker();
            helpBroker.enableHelpOnButton(button, helpString, defaultHelpSystem.getHelpSet());
        }
        catch (Exception e)
        {
            //log.log(Level.WARNING, "No HelpSet available: ", e);
            log.log(Level.WARNING, "No HelpSet available: ");
        }
    }


}