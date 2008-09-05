package com.ca.directory.jxplorer.editor;

// AudioAccessory.java
// A simple accessory for JFileChooser that lets you play .au clips.
//
import com.ca.commons.cbutil.CBButton;
import com.ca.commons.cbutil.CBIntText;
import sun.applet.AppletAudioClip;

import javax.swing.*;
import java.applet.AudioClip;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;

public class audioaccessory extends JPanel implements PropertyChangeListener {

  protected		AudioClip     currentClip;
  protected		String        currentName="";
  protected		JLabel        fileLabel;
  protected		CBButton       playButton, stopButton, helpButton;
  protected		JOptionPane   infoBox;    // TE: information box.
  protected		boolean       unSupported = false;    //TE: used to check if an audio type is not supported (ie if it can be played).

  public audioaccessory() {
    // Set up the accessory.  The file chooser will give us a reasonable size.
    setLayout(new BorderLayout());
    add(fileLabel = new JLabel("  " + CBIntText.get("Select sound file")), BorderLayout.NORTH);
    JPanel p = new JPanel();
    playButton = new CBButton(CBIntText.get("Play"), CBIntText.get("Play this audio clip."));
    stopButton = new CBButton(CBIntText.get("Stop"), CBIntText.get("Stop playing the current audio clip."));
    playButton.setEnabled(false);
    stopButton.setEnabled(false);
    p.add(playButton);
    p.add(stopButton);
    add(p, BorderLayout.CENTER);  

    playButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (currentClip != null) {
          if (unSupported){
              infoBox = new JOptionPane();    //TE: information box added to inform user that certain types of audio can't be played.
              infoBox.showMessageDialog(null, CBIntText.get("Unable to play audio formats of type .mp3, .rmi or .ram"), CBIntText.get("Information Message"), JOptionPane.INFORMATION_MESSAGE);
              return;
          }
          currentClip.stop();
          currentClip.play();
        }
      }
    });
    stopButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (currentClip != null) {
          currentClip.stop();
        }
      }
    });
  }

	public void propertyChange(PropertyChangeEvent e) 
	{
		if (e.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) 
		{
			unSupported = false;
			// Ok, the user selected a file in the chooser
			File f = (File)e.getNewValue();

			if (f!=null)
			{
				// Make reasonably sure it's an audio file
				if (f.getName().toLowerCase().endsWith(".mid") || f.getName().toLowerCase().endsWith(".aiff") 
				  		|| f.getName().toLowerCase().endsWith(".wav") || f.getName().toLowerCase().endsWith(".au") 
				      	|| f.getName().toLowerCase().endsWith(".rmi") || f.getName().toLowerCase().endsWith(".ram") 
				       	|| f.getName().toLowerCase().endsWith(".mp3")) 
				{    // TE makes sure that it is an audio file...these are just the ones that I have tested, there are sure to be more therefore this will need adjusting.
					if (f.getName().toLowerCase().endsWith(".rmi") || f.getName().toLowerCase().endsWith(".ram") || f.getName().toLowerCase().endsWith(".mp3")) 
					{ 	//TE these are the audio types that are unsupported.
						unSupported = true;
					}        
					setCurrentClip(f);
				}
				else
				{
					setCurrentClip(null);
				}
			}
			else
			{
				setCurrentClip(null);
			}
		}
	}

  public void setCurrentClip(File f) {
    // Make sure we have a real file, otherwise, disable the buttons
    if ((f == null) || (f.getName() == null)) {
      fileLabel.setText("  " + CBIntText.get("Select sound file"));
      playButton.setEnabled(false);
      stopButton.setEnabled(false);
      return;
    }

    // Ok, seems the audio file is real, so load it and enable the buttons
    String name = f.getName();
    if (name.equals(currentName)) {
      return;
    }
    if (currentClip != null) { currentClip.stop(); }
    currentName = name;
    try {
      URL u = f.getAbsoluteFile().toURL();  // CB try to make URL handling more robust...
      //URL u = new URL("file:///" + f.getAbsolutePath());
      currentClip = new AppletAudioClip(u);
    }
    catch (Exception e) {
      e.printStackTrace();
      currentClip = null;
      fileLabel.setText(CBIntText.get("Error loading clip."));
    }
    fileLabel.setText("  " + name);
    playButton.setEnabled(true);
    stopButton.setEnabled(true);
  }
  
 /**
  *	Stops the current audio clip
  *
  */
  
  public void stopPlay()
  { 
	if(currentClip == null) {return;}
	currentClip.stop();
  }
}