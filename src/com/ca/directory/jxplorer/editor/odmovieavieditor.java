package com.ca.directory.jxplorer.editor;

import com.ca.commons.cbutil.*;

import java.awt.*;



/**
 * Class that extends BaseODMediaEditor to basically customise the display with
 * the title 'odMovieAVI' and to add a launch button that when clicked creates
 * a temp file and tries to launch the file in the default player.
 * @author Trudi.
 */
 
public class odmovieavieditor extends baseodmediaeditor
{

   /**
    * Calls the BaseODMediaEditor constructor, sets the dialog title, the file
    * name and the extension.
    * @param owner handle to the application frame, used to display dialog boxes.
    */

    public odmovieavieditor(Frame owner)
    {
        super(owner);
        super.setDialogTitle(CBIntText.get("odMovieAVI"));
        super.setFileName("odMovieAVI");
        super.setExtension(".avi");
    }
}