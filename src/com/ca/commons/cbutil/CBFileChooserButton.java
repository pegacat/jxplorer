package com.ca.commons.cbutil;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * A small utility file picker button.  It works in conjunction with
 * a JTextComponent.  It brings up a 'file' button that the user can
 * use to select a file, the name + path of which is then set in the
 * associated text component.
 */

public class CBFileChooserButton extends CBButton implements ActionListener
{
    static String globalLastDirectory = "";  // the last directory accessed
    String localLastDirectory = "";  // possibly the user wants just this button to remember the last directory used.
    boolean useLocal = false;   // which 'last directory' to use.

    JTextComponent myText;
    Component parent;

    FileFilter filter = null;
    /*
     *  mode selector for internal JFileChooser object; allowable values are FILES_ONLY, DIRECTORIES_ONLY, and FILES_AND_DIRECTORIES;
     */
    int fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES;


    /**
     * Constructor takes the JTextComponent it is to be
     * associated with.  The constructor creates a named button
     * (usually 'File'), and tries to open an image icon
     * using the file 'open.gif'.  (If it's not there, the
     * button is unadorned.)  These defaults can obviously
     * be reset using setText() and setIcon().
     *
     * @param text      the JTextComponent to be filled out with
     *                  the selected file name after selection.
     * @param GUIparent the GUI parent to use for the file Chooser
     *                  dialog.
     */

    public CBFileChooserButton(JTextComponent text, Component GUIparent)
    {
        this(text, GUIparent, "File");
    }

    /**
     * Constructor takes the JTextComponent it is to be
     * associated with.  The constructor creates a named button
     * (usually 'File'), and tries to open an image icon
     * using the file 'open.gif'.  (If it's not there, the
     * button is unadorned.)  These defaults can obviously
     * be reset using setText() and setIcon().
     *
     * @param text       the JTextComponent to be filled out with
     *                   the selected file name after selection.
     * @param GUIparent  the GUI parent to use for the file Chooser
     *                   dialog.
     * @param buttonName the name of the button the user pressed to launch
     *                   the file chooser.
     */

    public CBFileChooserButton(JTextComponent text, Component GUIparent, String buttonName)
    {
        this(text, GUIparent, buttonName, "");
    }

    /**
     * Constructor takes the JTextComponent it is to be
     * associated with.  The constructor creates a named button
     * (usually 'File'), and tries to open an image icon
     * using the file 'open.gif'.  (If it's not there, the
     * button is unadorned.)  These defaults can obviously
     * be reset using setText() and setIcon().
     *
     * @param text       the JTextComponent to be filled out with
     *                   the selected file name after selection.
     * @param GUIparent  the GUI parent to use for the file Chooser
     *                   dialog.
     * @param buttonName the name of the button the user pressed to launch
     *                   the file chooser.
     * @param tooltip    the tooltip to be added to the button.
     */

    public CBFileChooserButton(JTextComponent text, Component GUIparent, String buttonName, String tooltip)
    {
    	//TODO: Check out for theme-izing
        super(buttonName, tooltip, new ImageIcon("open.gif"));
        myText = text;
        parent = GUIparent;
        addActionListener(this);
    }


    public void setLocalDirectoryUse(boolean state)
    {
        useLocal = state;
    }

    public boolean getLocalDirectoryUse()
    {
        return useLocal;
    }

    public static void setGlobalDirectory(String dirString)
    {
        globalLastDirectory = dirString;
    }

    public void setLocalDirectory(String dirString)
    {
        localLastDirectory = dirString;
        useLocal = true;
    }


    public void setStartingDirectory(String dirString)
    {
        if (useLocal)
            localLastDirectory = dirString;
        else
            setGlobalDirectory(dirString);
    }

    public String getStartingDirectory()
    {
        return (useLocal ? localLastDirectory : globalLastDirectory);
    }


    /**
     * Sets a file filter (e.g. to select files with certain .xxx suffixes) such as CBFileFilter.
     * @param filter
     */
    public void addChoosableFileFilter(FileFilter filter)
    {
        this.filter = filter;
    }

    /**
     * Sets the file selection mode as per JFileChooser selection modes:
     *
    <ul>
     <li>JFileChooser.FILES_ONLY
    <li>JFileChooser.DIRECTORIES_ONLY
    <li>JFileChooser.FILES_AND_DIRECTORIES
    </ul>
     * @param mode
     */
    public void setFileSelectionMode(int mode)
    {
        fileSelectionMode = mode;
    }

    public void actionPerformed(ActionEvent e)
    {
        String lastDirectory = (useLocal) ? localLastDirectory : globalLastDirectory;

        /*
         *	Check that the directory exists - if it doesn't the Sun JFileChooser (circa java 1.4)
         *  can throw an uncaught exception and the GUI fails...
         */

        try
        {
            if (new File(lastDirectory).exists() == false)
            {
                lastDirectory = null;
            }
        }
        catch (Exception ex)  // something wierd. null is nice and safe (unusually)
        {
            lastDirectory = null;
        }

        JFileChooser chooser = new JFileChooser(lastDirectory);

        chooser.setFileSelectionMode(fileSelectionMode);
        
        if (filter != null)
            chooser.addChoosableFileFilter(filter);

        int option = chooser.showOpenDialog(parent);

        if (option == JFileChooser.APPROVE_OPTION) // only do something if user chose 'ok'
        {
            setStartingDirectory(chooser.getSelectedFile().getParent());
            File readFile = chooser.getSelectedFile();
            myText.setText(readFile.toString());
        }
    }
}