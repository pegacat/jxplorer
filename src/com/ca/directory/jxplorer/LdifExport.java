package com.ca.directory.jxplorer;

import java.awt.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.*;
import javax.swing.*;

import javax.naming.*;
import javax.naming.directory.*;

import com.ca.directory.jxplorer.broker.DataQuery;
import com.ca.directory.jxplorer.tree.*;
import com.ca.directory.jxplorer.broker.*;
import com.ca.commons.naming.*;
import com.ca.commons.cbutil.*;

public class LdifExport extends CBDialog
{
    JTextArea rootDN, newRootDN;
    DataBrokerQueryInterface dataSource;
    SmartTree searchTree;
    SmartTree schemaTree;

    FileWriter saveFile;
    LdifUtility ldifutil = new LdifUtility();

    boolean usingSearch;

    CBpbar pbar;  // shows how far an operation has progressed for large ops.

    static String lastDirectory = null;

    private static Logger log = Logger.getLogger(LdifExport.class.getName());

    /**
     *    Constructor for the LdifExport window.  Takes a DN, and
     *    a jndi broker.  If it is exporting from a search result
     *    set, it takes the search tree as a parameter, and a boolean
     *    flag specifying that the tree should be used to list the DNs
     *    to be exported; otherwise it does a full dump from the provided
     *    DN.<p>
     *
     *    The constructor sets up the GUI, defining buttons and fields
     *    and registering button listeners.
     *
     *    @param D the base DN to work from
     *    @param broker the jndi broker to use to read entry attribtues from,
     *                  and to physically write the ldif file
     *    @param searchTree (possibly) a tree containing a list of search
     *                      results, to be used if the search flag is set
     *    @param usingSearch a boolean flag that forces the reading of the
     *                       list of DNs to save from the tree, rather than
     *                       directly from the directory...
     */
    public LdifExport(DN D, DataBrokerQueryInterface broker, SmartTree searchTree, boolean usingSearch, Frame owner)
    {
        this(D, broker, searchTree, usingSearch, owner, HelpIDs.LDIF_EXPORT_TREE);
    }

    /**
     *    Constructor for the LdifExport window.  Takes a DN, and
     *    a jndi broker.  If it is exporting from a search result
     *    set, it takes the search tree as a parameter, and a boolean
     *    flag specifying that the tree should be used to list the DNs
     *    to be exported; otherwise it does a full dump from the provided
     *    DN.<p>
     *
     *    The constructor sets up the GUI, defining buttons and fields
     *    and registering button listeners.
     *
     *    @param D the base DN to work from
     *    @param broker the jndi broker to use to read entry attribtues from,
     *                  and to physically write the ldif file
     *    @param searchTree (possibly) a tree containing a list of search
     *                      results, to be used if the search flag is set
     *    @param usingSearch a boolean flag that forces the reading of the
     *                       list of DNs to save from the tree, rather than
     *                       directly from the directory...
     *    @param helpID the ID of the help page to attach to the Help button.
     */
    public LdifExport(DN D, DataBrokerQueryInterface broker, SmartTree searchTree, boolean usingSearch, Frame owner, String helpID)
    {
        super(owner, CBIntText.get("LDIF Export"), helpID);

        OK.setToolTipText(CBIntText.get("Perform the LDIF export"));
        Cancel.setToolTipText(CBIntText.get("Cancel without performing an LDIF export"));
        Help.setToolTipText(CBIntText.get("Display help about LDIF exporting"));

        if (D==null) D = new DN();

        this.dataSource = broker;
        this.searchTree = searchTree;
        this.usingSearch = usingSearch;

        display.add(new JLabel(CBIntText.get("Root DN")),0,0);

        display.makeHeavy();

        rootDN = new JTextArea(D.toString());
		rootDN.setLineWrap(true);		//TE: allows line wrapping.
		display.add(new JScrollPane(rootDN, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),1,0);		//TE: scroll pane that scrolls vertically on need and never scrolls horizontally.

		display.makeLight();

		display.add(new JLabel(CBIntText.get("New root DN")),0,1);

		display.makeHeavy();
        newRootDN = new JTextArea(D.toString());
		newRootDN.setLineWrap(true);		//TE: allows line wrapping.
		display.add(new JScrollPane(newRootDN, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),1,1);		//TE: scroll pane that scrolls vertically on need and never scrolls horizontally.

        display.makeLight();
    }

    /**
    *    A quick spot of mucking around to add '.ldif' to naked files.
    *
    */

    protected File adjustFileName(File readFile)
    {
        if (readFile == null) return null;            // sanity check

        if (readFile.exists()) return readFile;       // don't do anything if file already exists...

        String name = readFile.getName();
        if (name.indexOf('.') != -1) return readFile; // ... or if it already has an extension.

        name = name + ".ldif";

        return new File(readFile.getParentFile(), name);
    }

    /**
     *    This method is called by the base class when the OK button is pressed.
     *    Handles actually writing the ldif file (relying heavily on LdifUtility for
     *    the grunt work).  Does the actual file writing in a separate thread.
     */

    public void doOK()
    {
        if(!checkRootDN())      //TE: bug 5057.
            return;

        setVisible(false);

        JFileChooser chooser = new JFileChooser(JXConfig.getProperty("ldif.homeDir"));

        chooser.addChoosableFileFilter(new CBFileFilter(new String[] {"ldif", "ldi"},"Ldif Files (*.ldif, *.ldi)"));

        int option = chooser.showSaveDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) // only do something if user chose 'ok'
        {
            File readFile = chooser.getSelectedFile();
            if (readFile == null)
            {
                CBUtility.error(CBIntText.get("Please select a file"));
            }
            else
            {
                readFile = adjustFileName(readFile);  // whack an '.ldif' on the end if necessary.

				int response = -1;
				if (readFile.exists())	//TE: ask the user if they want to overwrite an existing file.
				{
					response = JOptionPane.showConfirmDialog(this, CBIntText.get("File ''{0}'' already exists. Do you want to replace it?", new String[] {readFile.toString()}),
	        							CBIntText.get("Overwrite Confirmation"), JOptionPane.YES_NO_OPTION );

					if (response != JOptionPane.YES_OPTION)
					{
						setVisible(true);
						return;
					}
				}

                JXConfig.setProperty("ldif.homeDir", readFile.getParent());
                doFileWrite(readFile);
            }
        }
    }

    /**
     * Does three checks.<br><br>
     * 1) if there is a root DN and a new root DN.  If not a confirmation message appears
     * asking if the user wants to export the full subtree.<br>
     * 2) if there is no root DN.  In this case a dialog appears asking for it.
     * 3) if there is no new root DN.  In this case a dialog appears asking for it.
     * @return true only if all the checks succeed (or user approves the export full subtree).
     */
    public boolean checkRootDN()
    {
        String oldRoot = (rootDN.getText()).trim();    // the original DN
        String newRoot = (newRootDN.getText()).trim(); // the replacement DN (may be identical!)

        if((oldRoot == null || oldRoot.length() <= 0) && (newRoot == null || newRoot.length() <= 0))
        {
            int response = JOptionPane.showConfirmDialog(this, CBIntText.get("Without a 'Root DN' and a 'New Root DN', the full tree will be exported.  Do you want to continue?"),
                                CBIntText.get("Export Full Tree"), JOptionPane.YES_NO_OPTION );
            if (response != JOptionPane.YES_OPTION)
                return false;

            return true;
        }
        else if(oldRoot == null || oldRoot.length() <= 0)
        {
            JOptionPane.showMessageDialog(this, CBIntText.get("Please enter a 'Root DN'."),
                                            CBIntText.get("Root DN"), JOptionPane.INFORMATION_MESSAGE );
            return false;

        }
        else if(newRoot == null || newRoot.length() <= 0)
        {
            JOptionPane.showMessageDialog(this, CBIntText.get("Please enter a 'New Root DN'."),
                                            CBIntText.get("New Root DN"), JOptionPane.INFORMATION_MESSAGE );
            return false;
        }

        return true;
    }


    /**
     *    Launch a DataQuery that will write the ldif file.
     *
     */
    protected void doFileWrite(File saveFile)
    {
        if (saveFile == null)
            CBUtility.error(CBIntText.get("Unable to write to empty file"), null);

        final File myFile = saveFile;

        dataSource.extendedRequest(new DataQuery(DataQuery.EXTENDED)
        {
            public void doExtendedRequest(DataBroker b)
            {
                try
                {
                    FileWriter myFileWriter = new FileWriter(myFile);

                    pbar = new CBpbar(LdifExport.this, CBIntText.get("Saving LDIF file"), CBIntText.get("Saving Data"));

                    myFileWriter.write("version: 1\n");

                    DN oldRoot = new DN(rootDN.getText());    // the original DN
                    DN newRoot = new DN(newRootDN.getText()); // the replacement DN (may be identical!)

                    if (usingSearch)
                    {   // get the entries from the search tree directly; it will contain all the entries we want
                        ArrayList<DN> bloop = searchTree.getAllNodes(new DN(rootDN.getText()));
                        saveLdifList(bloop, myFileWriter, oldRoot.toString(), newRoot.toString(), b);
                    }
                    else
                    {   // read the entries from the directory - we don't know if the tree has done a complete read.
                        saveLdifTree(oldRoot, myFileWriter, oldRoot.toString(), newRoot.toString(), b);
                    }

					//TE: this seems to resolve the problem of no being able to access the ldif file until
					//TE: focus is moved from the current DN or the user exits JX.  See Bug numbers 669 & 533.
					//TE: NOTE: this fix could be the cause of an 'Extended' error...not sure!
					myFileWriter.close();
                }
                catch (Exception e)
                {
                    setException(e);
                }

				if(pbar.isCanceled())	//TE: delete the file if the user cancels the export.
					myFile.delete();

                closeDown();
                return;
            }
        });
    }



    /**
     *    Write a subtree to an ldif file by recursing through the
     *    tree, calling saveLdifEntry as it goes...
     *
     *    @param treeApex the root node of the sub tree to be written out.
     *    @param saveFile the file being written to...
     *    @param origPrefix the original DN prefix, that may be modified
     *                           on write to be replacementPrefix.  This may be
     *                           null if no action is to be taken.
     *    @param newPrefix another DN to replace the originalPrefix.
     *    @return number of entries written
     */

    public boolean saveLdifTree(DN treeApex, FileWriter saveFile, String origPrefix, String newPrefix, DataBroker broker)
    {
        // sanity checks...
        if (treeApex==null) return false;
        if (pbar == null) return false;

        if (newPrefix==null) origPrefix = null;                 // sanity check
        if ((origPrefix!=null)&&(origPrefix.equals(newPrefix))) // sanity check
            {origPrefix = null; newPrefix = null; }

        if (pbar.isCanceled()) return false;                    // user canceled

        DXEntry entry = null;

        try
        {
            if (treeApex.isEmpty() == false)
            {
                entry = broker.unthreadedReadEntry(treeApex, null);
            }

            if (entry != null)   // skip empty entries
            {    // skip fake tree node entries.
                if ((entry.contains(DXAttributes.STRUCTURAL_NODE)==false) && (entry.contains(DXAttributes.STRUCTURAL_NODE.toLowerCase())==false))
                {
                    DN escapeMe = new DN(treeApex);
                    ldifutil.writeLdifEntry(entry, saveFile, origPrefix, newPrefix);     // save the current dn...
                }
            }
            // need to get this as a DXNamingEnumeration to set progress bar...
            DXNamingEnumeration children = broker.unthreadedList(treeApex);

            pbar.push(children.size()); // might be zero...

            while (children != null && children.hasMore())
            {
                String subDNString = ((NameClassPair)children.next()).getName();
                DN child = new DN(treeApex);        // this could be done by string manip.,
                DN subDN = new DN(subDNString);
                child.addChildRDN(subDN.getLowestRDN());           // but then what happens if DN naming changes?

                if (saveLdifTree(child, saveFile, origPrefix, newPrefix, broker)==false)  // recursively traverse tree and write data
                    return false;
            }
        }
        catch (NamingException e)
        {
            CBUtility.error(this, CBIntText.get("Unable to read dn: {0} from directory", new String[] {treeApex.toString()}), e);
        }
        catch (Exception e)
        {
            CBUtility.error(this, CBIntText.get("General error reading dn: {0} from directory", new String[] {treeApex.toString()}), e);
            e.printStackTrace();
        }

        pbar.pop();
        pbar.inc();

        return true;
    }

    /**
     *    Writes a list of entries to an ldif file.
     *
     *    @param dns the list of the dns of objects to write out...
     *    @param saveFile the file being written to...
     *    @param originalPrefix the original DN prefix, that may be modified
     *                           on write to be replacementPrefix.  This may be
     *                           null if no action is to be taken.
     *    @param replacementPrefix another DN to replace the originalPrefix.
     */

    public void saveLdifList(ArrayList<DN> dns, FileWriter saveFile, String originalPrefix, String replacementPrefix, DataBroker broker)
    {
        if (replacementPrefix==null) originalPrefix = null;                        // sanity check.
        if ((originalPrefix!=null)&&(originalPrefix.equals(replacementPrefix)))   // sanity check.
        {
            originalPrefix = null;
            replacementPrefix = null;
        }
        int size = dns.size();
        pbar.push(size);

        for (int i=0; i<size; i++)
        {
            DN dn = dns.get(i);

            try
            {
                DXEntry entry = broker.unthreadedReadEntry(dn, null);
                ldifutil.writeLdifEntry(entry, saveFile, originalPrefix, replacementPrefix);
            }
            catch (NamingException e)
            {
                log.log(Level.WARNING, "Unable to read dn: '" + dn.toString() + "' from directory ", e);
            }
            catch (Exception e)
            {
                log.log(Level.WARNING, "General error reading: dn: '" + dn.toString() + "' from directory ", e);
            }

            if (pbar.isCanceled()) return;
            pbar.inc();
        }
        pbar.close();                // no need to pop out; we're done...
    }

    private void closeDown()
    {
        try
        {
            if (saveFile != null) saveFile.close();//TE: Always == null!
            log.warning("Closed LDIF file");
        }
        catch (IOException e) {;}

		if (pbar != null) pbar.close();
        setVisible(false);
        dispose();
    }
}