package com.ca.directory.jxplorer.editor;

import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.JXConfig;
import com.ca.directory.jxplorer.JXplorer;
import com.ca.directory.jxplorer.HelpIDs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import sun.audio.*;

/**
 *    Simple audio editor that trys to play the audio clip.
 *    Allows the user to save and load audio clips from a file and
 *    set the audio clip in the table editor.
 *    @author Trudi.
 */
public class audioeditor extends basicbinaryeditor
{
    protected CBButton        btnPlay, btnStop;
    protected JLabel         label;
    protected File           file;
    protected AudioStream    audioStream;
    protected audioaccessory audioAccess;

    private static Logger log = Logger.getLogger(audioeditor.class.getName());

   /**
    * Constructor.
    * @param owner handle to the application frame, used to display dialog boxes.
    */
    public audioeditor(Frame owner)
    {
        this(owner, false);       
    }

   /**
    * Sets up the frame with one panel, one label with an image and seven buttons.
    * @param owner handle to the application frame, used to display dialog boxes.
    * @param viewable specifies if there is a viewer for the binary data,
    * if true, the "view" button is added to the panel.
    */
    public audioeditor(Frame owner, boolean viewable)
    {
        super(owner);
        setModal(true);
        setTitle(CBIntText.get("Audio"));

        display = new CBPanel();
   
        label = new JLabel(new ImageIcon(Theme.getInstance().getDirImages() + "audio.gif"));
        label.setOpaque(true); 
        label.setBackground(Color.white);
        
        btnPlay = new CBButton(CBIntText.get("Play"), CBIntText.get("Click here to play the audio clip."));
        btnPlay.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                       audioStop();
                       audioPlay();
           }});

        btnStop = new CBButton(CBIntText.get("Stop"), CBIntText.get("Click here to stop playing the audio clip."));
        btnStop.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                       audioStop();
           }});

        btnLoad = new CBButton(CBIntText.get("Load"), CBIntText.get("Click here to load an external audio file."));
        btnLoad.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                       audioStop();
                       load();
           }});

        btnSave = new CBButton(CBIntText.get("Save"), CBIntText.get("Click here to save the audio clip to an external file."));
        btnSave.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                       audioStop();
                       save();
           }});

        btnOK = new CBButton(CBIntText.get("OK"), CBIntText.get("Click here to make the changes (remember to click Submit in the table editor)."));
        btnOK.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                       audioStop();
                       setValue();
           }});

        btnCancel = new CBButton(CBIntText.get("Cancel"), CBIntText.get("Click here to exit."));
        btnCancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                       audioStop();
                       quit();
           }});

        btnHelp = new CBButton(CBIntText.get("Help"), CBIntText.get("Click here for Help."));    //TE: creates a new help button with a listener that will open JX help at appropriate location.
        CBHelpSystem.useDefaultHelp(btnHelp, HelpIDs.ATTR_AUDIO);

        display.makeHeavy();
        display.addln(label);
        display.makeLight();
        
        JPanel buttonPanelTop = new JPanel();
        JPanel buttonPanelBottom = new JPanel();
        
        buttonPanelTop.add(btnPlay);
        buttonPanelTop.add(btnStop);
        buttonPanelTop.add(btnHelp);
        
        buttonPanelBottom.add(btnLoad);
        buttonPanelBottom.add(btnSave);
        buttonPanelBottom.add(btnOK);
        buttonPanelBottom.add(btnCancel);
        
        display.addln(buttonPanelTop);
        display.addln(buttonPanelBottom);               
        
        getContentPane().add(display);        
        setSize(300,320); 
        CBUtility.center(this, owner); 
    } 

   /**
    * Creates an audio stream and plays it.
    */
    public void audioPlay()
    {
        try
        {          
            audioStream = new AudioStream(new ByteArrayInputStream(bytes));
        } 
        catch (IOException e) 
        {
            CBUtility.error(CBIntText.get("Error with audio file") + ": " + e);
        }
        catch (NullPointerException  ee)
        {
            log.log(Level.WARNING, CBIntText.get("No data available") + ": ", ee);
        }            
            
         AudioPlayer.player.start(audioStream);
    }

   /**
    * Stops the audio playing.
    */
    public void audioStop()
    {
         AudioPlayer.player.stop(audioStream);
    }

   /**
    * Sets the value to display in the editor.  If no data the stop, play, ok &
    * save buttons are disabled.
    */
    public void setValue(editablebinary editMe)
    {
        this.editMe = editMe; 

        bytes = editMe.getValue();

        // Backup copy of data...
        oldBytes = bytes;
        
        if(bytes == null)
            setButtons(false);            
    }

   /**
    * Load audio clip from a file.  Opens a file chooser that can play the clip.
    */
    protected void load()
    {
        // Delete any temporary files associates with this entry...
		if (currentDN != null)
			CBCache.cleanCache(currentDN.toString());
			
        JFileChooser chooser = new JFileChooser(JXConfig.getProperty("binary.homeDir"));

        // Sets up the 'play' and 'stop' feature in the JFileChooser...
        audioAccess = new audioaccessory();
        chooser.setAccessory(audioAccess);
        chooser.addPropertyChangeListener(audioAccess);
        
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
            // Stops the current auto clip if playing...
			audioAccess.stopPlay();
            return;
		}

        // Stops the current auto clips if playing...
		audioAccess.stopPlay();
		
        file = chooser.getSelectedFile();            
        
        JXConfig.setProperty("binary.homeDir", chooser.getSelectedFile().getParent());
      
        try
        {    
            FileInputStream input = new FileInputStream(file);
            int length = (int)file.length();
            if (length > 0)
            {
                bytes = new byte[length];
                int read = input.read(bytes);

                // Enables the buttons because there is data available...
                setButtons(true);
            }
            input.close();
        }
        catch(IOException e)
        {
            CBUtility.error(CBIntText.get("Error writing to the file") + ": " + e);
        }		
    }
    
   /**
    * Save binary data to the file.
    */
    protected void save()
    {
        JFileChooser chooser = new JFileChooser(); 
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION)
            return;

        File file = chooser.getSelectedFile();
        try
        {    
            FileOutputStream output = new FileOutputStream(file);
            output.write(bytes);
            output.close();
        } 
        catch(IOException e)
        {
            CBUtility.error(CBIntText.get("Error writing to the file") + ": " + e);
        }
    }

   /**
    * Enables/disables the components in the editor.
	* @param enabled flag to determine if the buttons should be enabled or not.
    */
    public void setButtons(boolean enabled)
    {
        btnStop.setEnabled(enabled);
        btnPlay.setEnabled(enabled);
        btnSave.setEnabled(enabled);
        btnOK.setEnabled(enabled);
    }

   /**
	* Returns a new value.
	* @return new value.
	*/
    public byte[] getNewValue()
    {
        if (bytes!=null && bytes.length!=0)
            return bytes;
        else 
            return null;
    }

   /**
    * Sets the value in the table editor.
    */
    public void setValue()
    {
        if (isChanged())        
            editMe.setValue(getNewValue());      
        quit();
    }                     
}