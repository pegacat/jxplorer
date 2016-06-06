package com.ca.commons.security.cert;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Properties;

import java.io.*;

import java.security.cert.*;

import com.ca.commons.security.cert.extensions.*;
import com.ca.commons.security.asn1.*;
import com.ca.commons.security.util.CertUtil;

import com.ca.commons.cbutil.*;

import java.util.StringTokenizer;

/**
 * <p>A certificate viewer which is similar to the Microsoft's,
 * written in Java, therefore it can be run in all operating systems
 * with a Java virtual machine installed.
 * General tab, Details tab, Certification Path tab, OK button.</p>
 *
 * <p>Note that the static 'setProperties()' method must be called
 * before use if graphics are to be displayed.</p>
 *
 * @author vbui, cbetts
 */

// XXX this class has gathered a fair bit of lint - there are a bunch of generic security methods
// XXX that should be moved into some sort of security util class... - CB

// TODO: logging, and bubble exception properly from CertUtil

public class CertViewer extends CBDialog
{
    /**
     *    Utility method for passing around a cert and accompanying
     *    filename
     */

    public static class CertAndFileName
    {
        public X509Certificate cert;
        public String fileName;
    }

    /**
     *    The actual cert currently being displayed.
     */

	private X509Certificate cert = null;

    /**
     *    The file name the cert was loaded from (if any).
     */

    private String fileName = null;

	// visual elements
	private JTabbedPane tabs = new JTabbedPane();
	private CertGeneralViewPanel generalView = null;
	private CertDetailsViewPanel detailsView = null;
	private CertPathViewPanel pathView = null;
	private CBButton okButton, saveButton, loadButton;


    /**
     *    When the mode is set to this the cert can only be viewed
     */

    public static final int VIEW_ONLY = 0;

    /**
     *    When the mode is set to this the cert can be viewed and saved to file
     */

    public static final int VIEW_SAVE = 1;

    /**
     *    When the mode is set to this the cert can be loaded from file and viewed
     */

    public static final int VIEW_LOAD = 2;

    /**
     *    When the mode is set to this the cert can be viewed, saved and loaded
     */

    public static final int VIEW_SAVE_LOAD = 3;

    /**
     *    Synonym for VIEW_SAVE_LOAD
     */

    public static final int VIEW_LOAD_SAVE = 3;

    protected int mode = VIEW_SAVE;

	//public static String imagePath = null;

	public static ImageIcon certLargeIcon = null;
	public static ImageIcon certIcon = null;
	public static ImageIcon attributeIcon = null;
	public static ImageIcon extensionIcon = null;
	public static ImageIcon criticalExtensionIcon = null;
	public static ImageIcon thumbprintIcon = null;
	public static Image frameIcon = null;

    /**
     * used to store image directory and default browsing directory properties,
     * under "dir.images" and "cert.homeDir".
     */

    public static Properties properties = null;

	public static String helpLink = null;			//TE: help link, might not be initialised, if it is a help button should be added.

    /**
     *  sets the graphic icons for the various buttons and cert display screens,
     * using the image directory set via the setProperties() method.
     */
	protected static void setupGraphics()
	{
		try
		{
			certLargeIcon         = getImageIcon("certificate_large.gif");
			certIcon              = getImageIcon("certificate.gif");
			attributeIcon         = getImageIcon("attribute.gif");
			extensionIcon         = getImageIcon("extension.gif");
			criticalExtensionIcon = getImageIcon("criticalExtension.gif");
			thumbprintIcon        = getImageIcon("thumbprint.gif");
			frameIcon             = getImageIcon("pki_icon.gif").getImage();
		}
		catch (Exception ex)
		{
			System.out.println(ex.getMessage());
		}
	}

    /**
     *    Local copy of standard JXplorer function to allow stand-alone use.
     */

    public static ImageIcon getImageIcon(String name)
    {
        if (properties == null) return null;

        ImageIcon newIcon = new ImageIcon(Theme.getInstance().getDirImages() + name);
        return newIcon;
    }

   /**
    *	Sets up the help link for the help button.
	*/

	public static void setupHelpLink(String link)
	{
		helpLink = link;
	}

    /**
     *    Used to set the global properties used by CertViewer to load images,
     *    and to determine the default browseing directory (which can be
     *    maintained between sessions for added usability).
     *    @param props a properties object containing a "dir.images" property
     *     (setting the image directory path) and a "cert.homeDir" property
     *     describing the directory the browser should start viewing in.
     */

    public static void setProperties(Properties props) { properties = props; }

    /**
     *    A utility method - this allows just the image directory to be
     *    set.  It is usually preferable to use setProperties() instead,
     *    which allows the cert.homeDir property to be maintained between
     *    sessions.
     */

    public static void setImageDirectory(String imagePath)
    {
        if (properties == null)
            properties = new Properties();
        properties.put("dir.images", imagePath);
    }


    public static CertAndFileName loadCertificate(Frame owner)
    {
        CertViewer viewer = new CertViewer(owner, null, VIEW_SAVE_LOAD);

        // by this stage, the viewer has already prompted the user to
        // select a certificate - if it is null, it means the user
        // hit 'cancel' on the file chooser, so bail out...

        if (viewer.getCertificate() == null)
            return null;

        // show the certificate viewer, and allow the user to decide
        // whether to use the current cert, or select another.

        viewer.setVisible(true);

        CertAndFileName returnInfo = new CertAndFileName();
        returnInfo.cert = viewer.getCertificate();
        returnInfo.fileName = viewer.getFileName();

        return returnInfo;
    }

    public static CertAndFileName editCertificate(Frame owner, byte[] certData)
    {
        if (certData == null) return loadCertificate(owner);

        X509Certificate cert = CertUtil.loadX509Certificate (certData);

        CertViewer viewer = new CertViewer(owner, cert, VIEW_SAVE_LOAD);

        // by this stage, the viewer has already prompted the user to
        // select a certificate - if it is null, it means the user
        // hit 'cancel' on the file chooser, so bail out...

        if (viewer.getCertificate() == null)
            return null;

        // show the certificate viewer, and allow the user to decide
        // whether to use the current cert, or select another.

        viewer.setVisible(true);

        CertAndFileName returnInfo = new CertAndFileName();
        returnInfo.cert = viewer.getCertificate();
        returnInfo.fileName = viewer.getFileName();

        return returnInfo;
    }

	/**
 	 * Frameless Constructor.  This should only be used if no
     * parent frame can be found, and will cause odd behaviour
     * in some circumstances.
     * @deprecated
     * @param cert the X509 certificate to view
 	 */
 	public CertViewer(X509Certificate cert)
 	{
        super(null, CBIntText.get("Certificate"), null);
		init(cert, VIEW_SAVE);
 	}

	/**
	 * Constructor.
     * @param owner the parent Frame (used for centering and look and feel propogation)
     * @param cert the X509 certificate to view
	 */

	public CertViewer(Frame owner, X509Certificate cert)
	{
		super(owner, CBIntText.get("Certificate"), helpLink);		//TE: if there is no help link supplied helpLink will be null.
		init(cert, VIEW_SAVE);
	}

    /**
     * Constructor.
     * @param owner the parent Frame (used for centering and look and feel propogation)
     * @param cert the X509 certificate to view
     * @param mode one of VIEW, VIEW_SAVE, VIEW_LOAD or VIEW_SAVE_LOAD, describing
     *        what options should be available to the user.
     */

    public CertViewer(Frame owner, X509Certificate cert, int mode)
    {
        super(owner, CBIntText.get("Certificate"), helpLink);		//TE: if there is no help link supplied helpLink will be null.
        init(cert, mode);
    }

	public void init(X509Certificate cert, int mode)
	{
        if (certLargeIcon == null)
            setupGraphics();

        this.mode = mode;

        displayCert(cert);  // setup tab panes.

        saveButton = new CBButton(CBIntText.get("Copy to File"), CBIntText.get("Copy to File"));
        loadButton = new CBButton(CBIntText.get("Read from File"), CBIntText.get("Read from File"));

        // Since this inherits from CBDialog, it already has 'ok' and
        // 'cancel'buttons; depending on what mode we're in, we will
        // add others, and possibly remove the cancel.

        if (mode == VIEW_ONLY || mode == VIEW_SAVE)
        {                                // if we can't load, there's no need for cancel
            buttonPanel.remove(Cancel);  // remove default CBDialog cancel button
        }

        if (mode == VIEW_SAVE || mode == VIEW_SAVE_LOAD)
        {
            buttonPanel.add(saveButton, 0);
        }

        if (mode == VIEW_LOAD || mode == VIEW_SAVE_LOAD)
        {
            buttonPanel.add(loadButton, 0);
            //OK.setText(CBIntText.get("Submit"));
        }

        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        setSize(440, 477);
        CBUtility.center(this, owner);

        saveButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e) { saveCert(); }
        });

        loadButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e) { loadCert(); }
        });

        // if we've started up with a null cert, fire up the load
        // cert window immediately.

        if (cert == null && (mode & VIEW_LOAD)>0)
            loadCert();
    }

    /**
     *    This is a bit heavyhanded - when the cert changes,
     *    tear down everything and recreate all the gui objects
     *    in the tab pane.  Better would be to charge off and
     *    update all the individual components, but right now
     *    I'm in a bit of a hurry :-)  - CB
     *    @param displayCert the new cert to display.
     */

    public void displayCert(X509Certificate displayCert)
    {
        cert = displayCert;

        tabs.removeAll();
        generalView = new CertGeneralViewPanel(displayCert);
        detailsView = new CertDetailsViewPanel(displayCert);
        pathView = new CertPathViewPanel(displayCert);
        tabs.add("General", generalView);
        tabs.add("Details", detailsView);
        tabs.add("Certification Path", pathView);

        display.removeAll();
        makeHeavy();
        display.add(tabs);
    }


    /**
     *  When the user hits 'cancel', the window is shut down,
     *  and the cert is set to null (for the benefit of anyone
     *  using this as a cert loader).
     */

    public void doCancel()
    {
        cert = null;
        super.doCancel();
    }

    /**
     *    Returns the certificate currently being viewed by the CertViewer.
     *    @return the current certificate (may be null).
     */

    public X509Certificate getCertificate() { return cert; }

    /**
     *    Returns the file name (if any) associated with the currently viewed
     *    cert.
     *    @return the full file name
     */

    public String getFileName() { return fileName; }

    /**
     *    This method prompts the user to select a file name to save the cert to.
     */

    protected void saveCert()
    {
        JFileChooser chooser = new JFileChooser(properties.getProperty("cert.homeDir", System.getProperty("user.dir")));

        chooser.addChoosableFileFilter(new CBFileFilter(new String[] {"der", "pem"}, CBIntText.get("Certificate File") + " (*.der) (*.pem)"));

        int option = chooser.showSaveDialog(this);

        File readFile = chooser.getSelectedFile();
        if (option != JFileChooser.APPROVE_OPTION || readFile == null) // only do something if user chose 'ok'
        {
            return;  // user picked 'cancel' or didn't select a file.
        }

        if (properties != null)
            properties.setProperty("cert.homeDir", readFile.getParent());

        fileName = readFile.toString();

        try
        {
            byte[] derout = cert.getEncoded();

            if (fileName.toLowerCase().endsWith(".pem")) // special handling for PEM files
            {
//System.out.println("saving pem cert!!!");
                derout = CBSecurity.convertToPEMCertificate(derout);
            }
            else if (fileName.toLowerCase().endsWith(".der") == false)
            {
                fileName = fileName + ".der";
                readFile = new File(fileName);
            }

            if (saveFileCheck(readFile) == false) return;  // something wrong with the file name

            FileOutputStream fos = new FileOutputStream(readFile);
//System.out.println("writeing: " + derout.length + " bytes");
            fos.write(derout);
            fos.close();
        }
        catch (Exception ex)
        {
            CBUtility.error(CBIntText.get("Unable to save Certificate."), ex);
        }
    }

    /**
     *    Checks a file before saving it.
     */

    public boolean saveFileCheck(File checkMe)
    {
        if (checkMe.isDirectory())
        {
            CBUtility.error(checkMe.toString() + " is a directory.", null);
            return false;
        }
        else if (checkMe.exists())
        {
            int saveAnswer = JOptionPane.showConfirmDialog(owner,
                (checkMe.toString() + "\n " + CBIntText.get("This file already exists.\nDo you want to overwrite this file?")),
                "Question", JOptionPane.OK_CANCEL_OPTION);

            return (saveAnswer == JOptionPane.OK_OPTION);
        }

        return true;
    }


    protected void loadCert()
    {
        String browseDir = System.getProperty("user.dir");
        if (properties != null)
        {
            if (properties.getProperty("cert.homeDir") != null)
                browseDir = properties.getProperty("cert.homeDir");
        }
        JFileChooser chooser = new JFileChooser(browseDir);

        chooser.addChoosableFileFilter(new CBFileFilter(new String[] {"der", "pem"}, CBIntText.get("Certificate File") + " (*.der), (*.pem)"));

        int option = chooser.showOpenDialog(this);

        File readFile = chooser.getSelectedFile();

        if (option != JFileChooser.APPROVE_OPTION || readFile == null) // only do something if user chose 'ok'
        {    // user picked 'cancel' or didn't select a file.
            if (cert == null)
                doCancel();  // quit out - we have nothing to display.
            return;
        }

        try
        {
            if (properties != null)
                properties.setProperty("cert.homeDir", readFile.getParent());

//Obsolete - use Sun code
//XXX - don't use Sun code - it doesn't handle plain text prefix in pem file correctly.
            byte[] data = getDERCertDataFromFile(readFile);

//Obsolete - use Sun code
//XXX - don't use Sun code - it doesn't handle plain text prefix in pem file correctly.
            X509Certificate newCert = CertUtil.loadX509Certificate(data);

/* Sun code
System.out.println("using new code");
            X509Certificate newCert = CertUtil.loadX509Certificate(readFile);
*/
            displayCert(newCert);
            fileName = readFile.getName();
        }
        catch (Exception ex)
        {
            CBUtility.error(CBIntText.get("Unable to load Certificate."), ex);
        }

    }



	/**
	 * Attempt to retrieve the most significant name in a distinguish name
	 * ????? (cn = somename)
	 */
	public static String getMostSignificantName(String dnstring)
	{
		String leftmostname = null;
		StringTokenizer stok = new StringTokenizer(dnstring, ",");
		if (stok.hasMoreTokens()) leftmostname = stok.nextToken();
		return leftmostname;
	}


    /**
     *    This takes a file, and converts the certificate data within it into
     *    a byte array.  While this is straightforward if it is DER encoded,
     *    some parsing is required if it is PEM to identify the certificate
     *    portion of the text, and convert it into raw DER bytes.
     *
     *    @param file the file containing an X509 certificate in DER or PEM form
     *    @return the raw (DER) byte data.
     */

    public static byte[] getDERCertDataFromFile(File file)
        throws CertificateParsingException, FileNotFoundException, IOException
    {
         /* Read the file data into a byte array */
        FileInputStream in = new FileInputStream(file);
        byte [] buffer = new byte[(int) (file.length())];
        in.read(buffer);
        in.close();

        /* check if this is pem base64 encoded data - if it is, translate it */
        if (CBSecurity.isPEM(buffer))
        {
            //XXX <your code to handle unencrypted private keys here> XXX//

//System.out.println("\nparsing PEM data!!\n");

            byte[] pemData = CBSecurity.convertFromPEMCertificate(buffer);
            if (pemData == null)
                throw new CertificateParsingException("Unable to parse PEM encoded cert - invalid PEM encoding.");

            buffer = pemData;
        }

        return buffer;
    }


    /**
     * Main method, the starting point of this program.
     */
    public static void main(String[] args)
    {
        JFrame parent = new JFrame();
        //parent.setIconImage(frameIcon);

        X509Certificate cert = null;

        try
        {
            byte[] data = getDERCertDataFromFile(new File(args[0]));

            if ( (cert = CertUtil.loadX509Certificate(data)) == null )
            {
                System.out.println("Problem opening certfile \"" + args[0] + "\"");
                System.exit(1);
            }


            String localDir = System.getProperty("user.dir") + File.separator;

            Properties props = new Properties();

            /*
             *    This sets the directory that the file browsers start in.
             *    This can be saved/read from file to allow the user to start
             *    loading/saving from the same place.
             */

            props.setProperty("cert.homeDir", localDir + "certs" + File.separator);

            /*
             *    This simply sets the directory where the GUI will try to load
             *    its button images from.
             */

            props.setProperty("dir.images", Theme.getInstance().getDirImages());

            CertViewer.setProperties(props);

            CertViewer me = new CertViewer(parent, cert);

            me.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    System.exit(0);
                }
            });

            me.setVisible(true);
        }
        catch (Exception e)
        {
            System.err.println("ERROR OCCURRED");
            e.printStackTrace();
            System.exit(-1);
        }
        // look and feel

    }

}