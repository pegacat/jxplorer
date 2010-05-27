package com.ca.directory.jxplorer;

import com.ca.commons.cbutil.*;
import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.broker.Broker;
import com.ca.directory.jxplorer.broker.SchemaBroker;
import com.ca.directory.jxplorer.tree.SmartTree;

import javax.naming.NamingException;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Logger;

public class LdifImport //extends JDialog implements ActionListener
{
    DataSource dataSource;
    File readFile;
    SmartTree tree;
    Frame owner;
    SchemaBroker schema;
    boolean offline = true;
    LdifUtility ldifutil = new LdifUtility();
    
    static final boolean debug = true;
    
    static ProgressMonitorInputStream pmonitor;

    private static Logger log = Logger.getLogger(LdifImport.class.getName());

    /**
     *    Constructor for the LdifImport window.  Takes a DN, and
     *    a data modifier.<p>
     *
     *    The constructor sets up the GUI, defining buttons and fields
     *    and registering button listeners.
     *
     *    @param dataSource the base DN to work from
     *    @param mrTree the tree to display the read ldif entries in
     *    @param owner a parent frame to display the progress bar in
     *    @param schema schema, if available, to check whether attributes are binary or not
     *             (otherwise this may need to be inferred from the data)
     */
     
    public LdifImport(DataSource dataSource, SmartTree mrTree, Frame owner, SchemaBroker schema)
    {
        this.owner = owner;
    
        this.schema = schema;
        
        tree = mrTree;  
        DN base = tree.getCurrentDN();
		
        offline = ((tree.getRootDN() == null) || (tree.getRootDN().toString().equals(SmartTree.NODATA)));
        
        if ((base==null)||(tree.getRootDN()==null)||(offline))    // check that we *have* a base, or if we're working
            base = new DN();                                      // offline set it to blank anyway...

        this.dataSource = dataSource;

        openFile();
    }

    /**
     *    Constructor for the LdifImport window.  Takes a DN,
     *    a data modifier, and a ldif file name.<p>
     *
     *    The constructor sets up the GUI, defining buttons and fields
     *    and registering button listeners.
     *
     *    @param dataSource the base DN to work from
     *    @param mrTree the tree to display the read ldif entries in
     *    @param owner a parent frame to display the progress bar in
     *    @param schema schema, if available, to check whether attributes are binary or not
     *             (otherwise this may need to be inferred from the data)
     *    @param fileName ldif file name to open
     */
     
    public LdifImport(DataSource dataSource, SmartTree mrTree, Frame owner, SchemaBroker schema, String fileName)
    {
        this.owner = owner;
    
        this.schema = schema;
        
        tree = mrTree;  
        DN base = tree.getCurrentDN();
		
        offline = ((tree.getRootDN() == null) || (tree.getRootDN().toString().equals(SmartTree.NODATA)));
        
        if ((base==null)||(tree.getRootDN()==null)||(offline))    // check that we *have* a base, or if we're working
            base = new DN();                                      // offline set it to blank anyway...

        this.dataSource = dataSource;

        openFile(fileName);
    }


    public void openFile()
    {        
        JFileChooser chooser = new JFileChooser(JXplorer.getProperty("ldif.homeDir"));
        chooser.addChoosableFileFilter(new CBFileFilter(new String[] {"ldif", "ldi"},"Ldif Files (*.ldif, *.ldi)"));
        
        int option = chooser.showOpenDialog(owner);
        
        if (option == JFileChooser.APPROVE_OPTION) // only do something if user chose 'ok'
        {
            readFile = chooser.getSelectedFile();
            if (readFile == null)
                CBUtility.error(CBIntText.get("Please select a file"));
            else
            {    
                JXplorer.setProperty("ldif.homeDir", chooser.getSelectedFile().getParent());
                ldifutil.setFileDir(chooser.getSelectedFile().getParent());
                doFileRead(readFile);
            }

            if(owner instanceof JXplorer  && offline)
            {
                ((JXplorer)owner).getMainMenu().setConnected(false);
            }
        }
    }

    public void openFile(String fileName)
    {
        if (fileName == null) openFile();
	else
	{
	    readFile = new File(fileName);
            JXplorer.setProperty("ldif.homeDir", readFile.getParent());
            ldifutil.setFileDir(readFile.getParent());
            doFileRead(readFile);

            if(owner instanceof JXplorer  && offline)
            {
                ((JXplorer)owner).getMainMenu().setConnected(false);
            }
	}
    }

    protected void doFileRead(File readFile)
    {        
        if (readFile == null)
            CBUtility.error(CBIntText.get("unable to read null LDIF file"), null);

        final File myFile = readFile;

        dataSource.extendedRequest(new DataQuery(DataQuery.EXTENDED) 
        {
            public void doExtendedRequest(Broker b) 
            {
                try
                {
                    FileInputStream rawBytes = new FileInputStream(myFile);
                    pmonitor = new ProgressMonitorInputStream(owner, CBIntText.get("Reading LDIF file"), rawBytes);
                }
                catch (FileNotFoundException e)
                {
                    CBUtility.error(CBIntText.get("Unable to read the LDIF file ''{0}''.", new String[] {myFile.toString()}), e);
                    return;
                }

                readLdifTree("", pmonitor, "", "", b, this);

                if (b.getException() != null)
                {
                    CBUtility.error(CBIntText.get("There were one or more errors reading the LDIF file\n(See the log for more details)"), b.getException());
                    b.clearException();
                }
                closeDown();
            }
        });
        
    }    

    
    /**
     *    Read a subtree from an ldif file, adding entries as 
     *    they are read...   
     *
     *    @param treeApex the root node of the sub tree to be written out.
     *    @param textStream The stream being read (i.e. the ldif file)
     *    @param origPrefix the original DN prefix, that may be modified
     *                           on write to be replacementPrefix.  This may be
     *                           null if no action is to be taken.
     *    @param newPrefix another DN to replace the originalPrefix.
     *    @param b the broker to send the read data to
     *    @param query the DataQuery doing the import
     */
    
    public void readLdifTree(String treeApex, InputStream textStream, String origPrefix, String newPrefix, Broker b, DataQuery query)
    {
        DN newDN = null;                   // a DN to be added to be read from the data source

        DXEntry apex = null;               // the top of the ldif tree in this file

        if (origPrefix == null) origPrefix = "";

        int numEntriesRead = 0;

        if (newPrefix==null) origPrefix = null;                 // sanity check
        if ((origPrefix!=null)&&(origPrefix.equals(newPrefix))) // sanity check
            {origPrefix = null; newPrefix = null; }

        //XXX hack alert: in fact DN comparision is a lot more restictive than what we do here.
        //XXX
        treeApex = treeApex.toLowerCase();

        BufferedReader readText = null;
        try
        {
             readText = new BufferedReader(new InputStreamReader(textStream, "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            CBUtility.error(CBIntText.get("Unexpected problem - Unable to read the LDIF file - no UTF-8 reader available") + ": ", e);
            return;
        }


        DXEntry newEntry = null;                            // object to place newly read entry info in
        String line ="";                                    // the first line read from the ldif file

        try
        {
            // start things rolling...
            readText.mark(256);          // prepare to read a 'version: X' header...
            line = readText.readLine();  // read the version header
            if (line.toLowerCase().startsWith("version") == false)  // version headers are kinda
            {                                                       // optionalish:  this might not be one.
                //log.warning("Warning: ldif file does not start with 'version ...'");
                readText.reset();
            }

			ArrayList list = new ArrayList();		//TE: stores the root DN('s ).

            try
            {
                while (((newEntry = ldifutil.readLdifEntry(readText))!=null))
                {
                    if (query.isCancelled()) return;  // check whether the user has cancelled this query.

                    int size = newEntry.getDN().size();

                    if (size != 0)
                    {
                        newDN = newEntry.getDN();

                        if (apex == null || apex.getDN().size() > size)
                        {
                            apex = newEntry;  // keep track of top entry...
                        }
                        translateToUnicode(newEntry);
                        b.unthreadedModify(null, newEntry);

                        if (!list.contains(getRoot(b, newDN)))
							list.add(getRoot(b, newDN));
                    }
                    else
                    {
                        log.warning("skipping ldif entry with no dn: ");
                    }

                    pmonitor.getProgressMonitor().setNote(CBIntText.get("reading entry #") + " " + (++numEntriesRead));  // XXX I'm not translating this to the MessageFormat version of CBIntText.get because I'm worried about performance - CB
                }
            }
            catch (InterruptedIOException e)    // almost certainly the user hitting 'cancel' on the progress bar
            {
                return;
            }
            catch (IOException e2)    // some other file reading error
            {
                CBUtility.error(CBIntText.get("unable to read LDIF file"), e2);
                return;
            }
            catch (NamingException e)
            {
                //TE: bug: 5153.  Not sure if this should be caught here and stop or should it try to process the rest of the LDIF file?
                CBUtility.error(CBIntText.get("An error occured while processing the LDIF file") + ": ", e);
                return;
            }


            // quicky check to see if we're working off line, or need to set the
            // root for some other reason (can't actually think of any...)

            if ((tree.getRootDN()==null)||(tree.getRootDN().toString().equals(SmartTree.NODATA)))
            {
				//TE: if there are multiple root DNs this ensures that they are displayed (see bug 529),
				//    e.g o=CA1 & o=CA2.
				//	  However, I have a feeling this could make other things fall over...but have no idea
				//    what at this stage because this only keeps account of the last root DN (e.g. o=CA2)
				//	  whereas there may be several...
                for(int i=0;i<list.size();i++)
                {
					String root = (list.get(i)).toString();

                	tree.setRoot(root);  // bit of a hack; pass the last known real DN
                	tree.getRootNode().setStructural(true);
                }
            }

            // give the tree a kick to make it refresh the apex parent, thus displaying
            // the newly imported nodes.
            if (apex != null && apex.getDN() != null && apex.getDN().size() > 1)
                tree.refresh(apex.getDN().parentDN());
        }
        catch (IOException e)
        {
            log.warning("Unable to read file " + readFile.toString());
        }

        catch (Exception e2)
        {
            log.warning("error parsing file " + readFile.toString());
            log.warning("Error occured reading line: " + ((line==null)?"*null line*":line) + "\n   (debug info: err was : " + e2 + "\n  ");
            e2.printStackTrace();
        }

    }

    private String getRoot(Broker b, DN lastKnownDN)
    {
        if (b==null)
        {
            log.warning("error: no data source available in ldif import/view");
            return null;
        }

        if (lastKnownDN == null)
        {
            log.warning("error: no DN available in ldif import/view");
            return null;
        }

        DN root = lastKnownDN;
        DN test = root;

        /*
         *    Go up through parents until we find a parent not in
         *    database; then the current value is the highest DN and
         *    hence root!  ( A bit hacky, but it works!)
         */
        try
        {
            while (root != null || (root.size()>0))
            {
                test = root.parentDN();
                if ((test==null)||(b.unthreadedExists(test)==false)||test.size()==0)
                    return root.toString();
                root = test;
            }
        }
        catch (NamingException e)
        {
            log.warning("Error testing root node " + test +  "\n" + e );
        }
        log.warning("Unable to determine root node from " + lastKnownDN);
        return null; // should never be reached
    }
    
    
    private void closeDown()
    {
        try
        {
            if (pmonitor != null) pmonitor.close();
        }
        catch (IOException e) {;}    
//        setVisible(false);
//        tree.collapse();
//        dispose();
    }
    
    /**
     *    If schema broker is active, use schema to check whether
     *    we have a utf-8 encoded unicode string.  If we do, decode
     *    it.  If schema is not active, use heuristic to check if 
     *    binary thingumy is utf-8, and if it is, translate it anyway.
     */
     
    private void translateToUnicode(DXEntry entry)
    {
        if (offline == false) return;  // we only need to do this when working offline -
                                       // otherwise we can pass utf8 straight through to
                                       // the directory and everything will still magically
                                       // work...
        try
        {
            Enumeration atts = entry.getAll();
            while (atts.hasMoreElements())
            {    
                DXAttribute att = (DXAttribute)atts.nextElement();
                for (int i=0; i<att.size(); i++)
                {
                    if (att.get(i) instanceof String == false)
                    {
                        byte[] seq = (byte[]) att.get(i);
                         
                        if (CBParse.isUTF8(seq))        // guess whether it is utf8...
                        { 
                            try
                            {
                                String s = new String(seq, "UTF8");
                                att.remove(i);
                                att.add(i, s);
                                att.setString(true); // is honest unicode string, not really nasty binary...
                            }
                            catch (Exception e)
                            {
                                log.warning("couldn't convert: " + att.getID() + "\n       " + e);
                            }
                        }                                
                    }
                }                    
            }
        }
        catch (NamingException e)
        {
            // ignore ubiquitous naming exception
        }    
    }
/*    
    boolean isStringInSchema()
    {
        return false;
    }
*/    
}
