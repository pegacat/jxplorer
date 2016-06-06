package com.ca.commons.cbutil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;


/*
 * This class displays the CA standard about dialog. This class is to be used across all the java
 * projects.  This is why all the Strings for components are passed in the constructor.  Currently
 * JXplorer handles its strings differently for internationalisation.
 */

public class CBAbout extends JDialog
{

    private JTextArea information; //This is where the message information about copyright etc. goes
    private JLabel productLogoLabel, caLogoLabel;	//Two JLabels to display the images at the top and bottom left of the screen
    private JPanel buttonsPanel, bottomPanel;
    private CBButton okButton;
    private static final int width = 477;	// the standard CA about window width
    private static final int height = 288;	// the standard CA about window hight
    private JFrame owner;


    private static Logger log = Logger.getLogger(CBAbout.class.getName());

    /**
     * The url for the CA website.
     */
    private static final String URL = "http://www.ca.com";

    /**
     * This is a constructor.
     *
     * @param frame            the parent frame for the dialog box
     * @param message          the product details to be displayed in the center scrollpane
     * @param caLogoImage      the CA Logo ImageIcon to go down the bottom left of the dialog
     * @param productLogoImage the product specific banner to go up the top of the dialog
     * @param okButtonMessage  the String to go on the label to the OK button
     * @param okButtonTooltip  the tooltip to be displayed on the mouseover for the OK button
     * @param aboutDialogTitle the title of the AboutDlg
     */


    public CBAbout(JFrame frame, String message, ImageIcon caLogoImage, ImageIcon productLogoImage,
                   String okButtonMessage, String okButtonTooltip, String aboutDialogTitle)
    {
        super(frame, aboutDialogTitle, true);

        owner = frame;


        setBackground(Color.white);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screen.width - width) / 2, (screen.height - height) / 2, width, height);

        setSize(width, height);
        setResizable(false);

        /*
         * puts the product banner image up the top of the dialog
         */
        productLogoLabel = new JLabel(productLogoImage);
        productLogoLabel.setBackground(Color.white);
        getContentPane().add(productLogoLabel, BorderLayout.NORTH);

        /*
         * creates a scrollable text area in the center of the dialog and
         * inserts the String with your copyright information, versions etc.
         */
        JPanel infoPanel = new JPanel(new GridLayout(1, 0));
        infoPanel.setBackground(Color.white);
        information = new JTextArea(message);
        information.setBackground(Color.white);
        information.setOpaque(true);
        information.setEditable(false);

        JScrollPane textPane = new JScrollPane(information);
        textPane.setBackground(Color.white);
        textPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        infoPanel.add(textPane);
        infoPanel.setBorder(BorderFactory.createLineBorder(Color.white, 15));
        getContentPane().add(infoPanel, BorderLayout.CENTER);

        /*
         * Creates the bottom panel of the dialog which will hold the calogo image,
         * a blank spacing label and the button panel
         */
        bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBackground(Color.white);
        caLogoLabel = new JLabel(caLogoImage);
        caLogoLabel.setToolTipText("i am ca");

        caLogoLabel.addMouseListener(new MouseAdapter()
        {
            /**
             * Sets the cursor to the hand cursor when the mouse is over
             * the CA logo.
             */
            public void mouseEntered(MouseEvent e)
            {
                CBUtility.setHandCursor((Component) caLogoLabel);
            }

            /**
             * Sets the cursor back to the normal cursor when the mouse is over
             * the CA logo.
             */
            public void mouseExited(MouseEvent e)
            {
                CBUtility.setNormalCursor((Component) caLogoLabel);
            }

            /**
             * Kicks off the CA web site in the default browser when the CA logo is clicked.
             */
            public void mouseClicked(MouseEvent e)
            {
                iAmCa();
            }
        });
        bottomPanel.add(caLogoLabel);
        bottomPanel.add(new JLabel("                                                    "));

        /*
         *	Creates a panel that places the ok button down the bottom right of the Dialog
         *  and will hide the dialog when pressed
         */
        buttonsPanel = new JPanel(new FlowLayout());
        buttonsPanel.setBackground(Color.white);

        //blank labels to move the ok button down to the bottom of the screen
        buttonsPanel.add(new JLabel(""));
        buttonsPanel.add(new JLabel(""));

        okButton = new CBButton(okButtonMessage, okButtonTooltip);
        okButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
            }
        });
        buttonsPanel.add(okButton);
        bottomPanel.add(buttonsPanel);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
    }


    /**
     * Attempts to launch the CA website in the users default browser.
     * 
     */

    public void iAmCa()
    {
        try
        {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(URL));
        }
        catch (IOException e)
        {
            log.severe("unable to open browser: " + e.getMessage());
        }

        /*
        // this stuff hits security problems these days - use Desktop api instead

        if (System.getProperty("os.name").indexOf("Windows") >= 0)
            CBLauncher.launchProgram(".html", URL);
        else
        {

            String browserName;

            //Set up a FileDialog for the user to locate the browser to use.
            FileDialog fileDialog = new FileDialog(owner);
            fileDialog.setMode(FileDialog.LOAD);
            fileDialog.setTitle("Choose the browser to use:");
            fileDialog.setVisible(true);

            //Retrieve the path information from the dialog and verify it.
            String resultPath = fileDialog.getDirectory();
            String resultFile = fileDialog.getFile();
            if (resultPath != null && resultPath.length() != 0 && resultFile != null && resultFile.length() != 0)
            {
                File file = new File(resultPath + resultFile);
                if (file != null)
                {
                    browserName = file.getPath();

                    try
                    {
                        //Launch the browser and pass it the desired URL
                        // Runtime.getRuntime().exec(new String[]{browserName, URL});

                        String url = "http://www.google.com";
                        java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
                    }
                    catch (IOException exc)
                    {
                        exc.printStackTrace();
                    }
                }
            }
        } */
    }
}