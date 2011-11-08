package com.ca.directory.jxplorer.editor;

import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.JXConfig;
import com.ca.directory.jxplorer.JXplorer;
import com.ca.directory.jxplorer.HelpIDs;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import javax.swing.ImageIcon;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *    JPEG photo viewer.
 *    Allows users to view a JPEG photo and save or load to/from a file.
 *    @author Trudi.
 */
public class jpegphotoeditor extends basicbinaryeditor
{
    protected ImageIcon iconBytes;
    protected JLabel label;
    protected int imageHeight, imageWidth, screenHeight, screenWidth;    //TE: used for the height and width of the image and screen size.
    protected JScrollPane scrollPaneLabel;

    private final static Logger log = Logger.getLogger(jpegphotoeditor.class.getName());

    /**
     *    Constructor.
     *    @param owner handle to the application frame, used to display dialog boxes.
     */
    public jpegphotoeditor(Frame owner)
    {
        this(owner, false);
    }

    /**
     *    Sets up the frame with one panel, one label which displays the JPEG image and five buttons.
     *    @param owner handle to the application frame, used to display dialog boxes.
     *    @param viewable specifies if there is a viewer for the binary data, if true, the "view" button is added to the panel.
     */
    public jpegphotoeditor(Frame owner, boolean viewable)
    {
        super(owner);
        setTitle(CBIntText.get("jpegPhoto"));
        btnEdit.setVisible(false);
    }

    /**
     *    Adds the label (which is used for displaying the image) to the panel.
     */
    public void addMainViewComponent()
    {
        label = new JLabel();
        scrollPaneLabel = new JScrollPane(label);
        display.makeHeavy();
        display.addln(scrollPaneLabel);
        display.makeLight();
    }

    /**
     *    Sets up the help button (which is used to open the java help at the appropriate location).
     *    @return btnHelp the button to be added.
     */
    public CBButton addHelp()
    {
        btnHelp = new CBButton(CBIntText.get("Help"), CBIntText.get("Click here for Help."));    //TE: creates a new help button with a listener that will open JX help at appropriate location.
        CBHelpSystem.useDefaultHelp(btnHelp, HelpIDs.ATTR_JPEGPHOTO);

        return btnHelp;
    }

    /**
     *    Sets the size of the panel according to the size of the image.
     *    Reduces the size of the image if bigger than screen size.
     *    Increases the size of the panel if too small.
     */
    public void preferredSize(int newWidth, int newHeight)
    {

        newHeight = newHeight + 80;                      //TE: image height plus button panel height.
        if (newWidth < 345)
        {
            newWidth = 345;
        }            //TE: minimum width to show all buttons.

        Toolkit toolKit = Toolkit.getDefaultToolkit();

        screenWidth = toolKit.getScreenSize().width;
        screenHeight = toolKit.getScreenSize().height;

        if (newWidth >= screenWidth)
        {
            newWidth = screenWidth - 50;
        }        //TE: we don't want the window to be wider than the screen.
        if (newHeight >= screenHeight)
        {
            newHeight = screenHeight - 50;
        }    //TE: we don't want the window to be higher than the screen.

        setBounds((screenWidth - newWidth) / 2, (screenHeight - newHeight) / 2, newWidth, newHeight);    //TE: sets the windows position on the screen and its width & height.
    }

    /**
     *    Sets the image to display in the editor.
     */
    public void setValue(editablebinary editMe)
    {
        this.editMe = editMe;

        bytes = editMe.getValue();

        oldBytes = bytes;

        if (bytes != null)
        {
            setButtons(true);         //TE: enables the ok & save buttons (only needed if value is available).
            iconBytes = new ImageIcon(bytes);
            imageHeight = iconBytes.getIconHeight();
            imageWidth = iconBytes.getIconWidth();
            preferredSize(imageWidth, imageHeight);
            label.setIcon(iconBytes);
        }
        else
        {
            preferredSize(400, 300);
            setButtons(false);        //TE: disables the ok & save buttons (only needed if value is available).
        }
    }

    /**
     *    Create the gui chooser for the user to pick a photo with, i.e. load binary data from file.
     */
    protected void load()
    {
        CBCache.cleanCache(currentDN.toString());    //TE: delete any temporary files associates with this entry.

        JFileChooser chooser = new JFileChooser(JXConfig.getProperty("binary.homeDir"));
        chooser.addChoosableFileFilter(new CBFileFilter(new String[]{"jpeg", "jpg"}, "JPEG Files (*.jpeg, *.jpg)"));

        ImageAccessory ip = new ImageAccessory();    //TE: sets up the 'play' and 'stop' feature in the JFileChooser.
        chooser.setAccessory(ip);
        chooser.addPropertyChangeListener(ip);


        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File file = chooser.getSelectedFile();
        JXConfig.setProperty("binary.homeDir", chooser.getSelectedFile().getParent());

        try
        {
            FileInputStream input = new FileInputStream(file);

            int length = (int) file.length();
            if (length > 0)
            {
                bytes = new byte[length];
                int read = input.read(bytes);
                if (read > 0)
                {
                    iconBytes = new ImageIcon(bytes);        //TE: set the selected image in the editor.
                    imageHeight = iconBytes.getIconHeight();
                    imageWidth = iconBytes.getIconWidth();
                    label.setIcon(iconBytes);
                    preferredSize(imageWidth, imageHeight);
                    setButtons(true);         //TE: enables the ok & save buttons (only needed if a value is available).
                }
            }
            input.close();
        }
        catch (IOException e)
        {
            log.log(Level.WARNING,"Error opening the file!",e);
        }
        setVisible(false);                                  //TE: just to get the refresh!!!
        setVisible(true);                                   //TE: needs to go after the field is set to ensure the attribute value is captured.
    }

    /**
     *    Save binary data to the file.
     */
    protected void save()
    {

        JFileChooser chooser = new JFileChooser(JXConfig.getProperty("binary.homeDir"));

        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION)
            return;

        File file = chooser.getSelectedFile();
        try
        {
            FileOutputStream output = new FileOutputStream(file);
            output.write(bytes);
            output.close();
        }
        catch (IOException e)
        {
            log.log(Level.WARNING,"Error writing to the file! ", e);
        }
    }

    /**
     *    Enables/disables the ok & save buttons in the editor.
     */
    public void setButtons(boolean enabled)
    {
        btnOK.setEnabled(enabled);
        btnSave.setEnabled(enabled);
    }

    /**
     *    Returns a new value.
     *    @return new value.
     */
    public byte[] getNewValue()
    {
        if (bytes != null && bytes.length != 0)
            return bytes;
        else
            return null;
    }

    /**
     *    Sets the value in the table editor.
     */
    public void setValue()
    {
        if (isChanged())
            editMe.setValue(getNewValue());
        quit();
    }

    /**
     * Creates a thumbnail of the image that is clicked on
     * in the file chooser.
     */
    public class ImageAccessory extends JComponent
            implements PropertyChangeListener
    {
        ImageIcon image = null;
        File file = null;

        /**
         * Constructor that sets the image size.
         */
        public ImageAccessory()
        {
            setPreferredSize(new Dimension(100, 50));
        }

        /**
         * Formats the image.
         */
        public void loadImage()
        {
            if (file == null)
            {
                image = null;
                return;
            }

            ImageIcon tempImage = new ImageIcon(file.getPath());
            if (tempImage != null)
            {
                if (tempImage.getIconWidth() > 90)
                    image = new ImageIcon(tempImage.getImage().getScaledInstance(90, -1, Image.SCALE_DEFAULT));
                else
                    image = tempImage;
            }
        }

        /**
         * Listens for file selection changes.
         * @param e
         */
        public void propertyChange(PropertyChangeEvent e)
        {
            boolean update = false;
            String prop = e.getPropertyName();

            if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop))
            {
                file = null;
                update = true;
            }
            else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop))
            {
                file = (File) e.getNewValue();
                update = true;
            }

            if (update)
            {
                image = null;
                if (isShowing())
                {
                    loadImage();
                    repaint();
                }
            }
        }

        /**
         * Paints the image.
         * @param g
         */
        public void paintComponent(Graphics g)
        {
            if (image == null)
            {
                loadImage();
            }
            if (image != null)
            {
                int x = getWidth() / 2 - image.getIconWidth() / 2;
                int y = getHeight() / 2 - image.getIconHeight() / 2;

                if (y < 0)
                {
                    y = 0;
                }

                if (x < 5)
                {
                    x = 5;
                }
                image.paintIcon(this, g, x, y);
            }
        }
    }
}