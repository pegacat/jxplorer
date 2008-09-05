package com.ca.directory.jxplorer.editor;

import com.ca.commons.cbutil.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.logging.Logger;


/**
 *	Class that extends DefaultBinaryEditor to basically customise the display with
 *	a specified title and to add a launch button that when clicked creates
 *	a temp file and tries to launch the file in the default player.
 *	@author Trudi.
 */

public class baseodmediaeditor extends defaultbinaryeditor
{
    /**
     * The file that gets saved when the user clicks launch.
     */
    protected File file = null;

    /**
     * The title of the dialog.
     */
    protected String title = "BaseODMediaEditor";

    /**
     * The name of the file that gets saved.
     */
    protected String fileName = "BaseODMediaEditor";

    /**
     * The extension of the file that gets saved.
     */
    protected String extension = "";

    private static Logger log = Logger.getLogger(baseodmediaeditor.class.getName());

    /**
     *   Calls the parent constructor to do most the set up.  Sets the title & size.
     *   @param owner handle to the application frame, used to display dialog boxes.
     */

    public baseodmediaeditor(Frame owner)
    {
        super(owner);
        this.setDialogTitle(title);
    }

    /**
     * Sets the dialog title.
     * @param title the title the dialog should display.
     */
    public void setDialogTitle(String title)
    {
        this.title = title;
        super.setDialogTitle(CBIntText.get(title));
    }

    /**
     * Sets the file name.
     * @param fileName the name the file should be saved with.
     */
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    /**
     * Sets the extension of the file.
     * @param extension the extension the file should be saved with.
     */
    public void setExtension(String extension)
    {
        this.extension = extension;
        addChoosableFileFilter(new CBFileFilter(new String[]{extension}, fileName + " Files (*" + extension +")"));
    }

    /**
     * Over writes the super method to add the Launch button to the dialog.
     * @return the Launch button (a CBButton) that when clicked calls the
     * launch method.  However, null is returned for Sun systems b/c
     * the launcher in JX doesn't work for those platforms.
     */
    public JComponent addComponent()
    {
        CBButton btnView = new CBButton(CBIntText.get("Launch"), CBIntText.get(""));
        btnView.setToolTipText(CBIntText.get("Launch the saved file into it's default application."));
        btnView.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                launch();
            }
        });

        //todo: disable the Launch button if nothing to launch.
//        if (bytes == null || bytes.length == 0)
//            btnView.setEnabled(false);

        if (System.getProperty("os.name").equalsIgnoreCase("SunOS"))
            return null;
        else
            return btnView;
    }


    /**
     *	Makes a temporary file of the odMusicMID object (odMusicMID.mid) then
     *	tries to launch the document in the default player.
     */

    public void launch()
    {
        if (bytes == null)
        {
            log.warning("No file to launch via " + fileName + ".");
            return;
        }

        File dir = new File("temp");
        dir.mkdir();
        dir.deleteOnExit();

        file = new File(dir, fileName + extension);		//TE: save the data into a temporary file.
        file.deleteOnExit();

        try
        {
            FileOutputStream output = new FileOutputStream(file);
            output.write(bytes);
            output.close();
        }
        catch (IOException e)
        {
            CBUtility.error("Error writing to the file!" + e);
        }

        CBLauncher.launchProgram(extension, file.getPath());
    }


    /**
     *   Shuts the window and deletes the temp file.
     */

    public void quit()
    {
        if (file != null)
            file.delete();
    }
}