package com.ca.directory.jxplorer;

import com.ca.commons.cbutil.*;
import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.broker.*;
import com.ca.directory.jxplorer.broker.DataQuery;
import com.ca.directory.jxplorer.tree.SmartTree;

import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.logging.Logger;

public class LdifImport //extends JDialog implements ActionListener
{
    DataBrokerQueryInterface dataSource;
    File readFile;
    SmartTree tree;
    Frame owner;
    SchemaDataBroker schema;
    boolean offline = true;
    LdifUtility ldifutil = new LdifUtility();
    
    static final boolean debug = true;



    //TODO: refactor offline file load handling - it's all pretty confusing...


    /**
     *    Constructor for the LdifImport object; sets up the data source, the visual tree and the owning
     * Frame (used as the parent for UI windows that are created when needed).
     *
     * @param dataSource the base DN to work from
     *    @param mrTree the tree to display the read ldif entries in
     * @param owner a parent frame to display the progress bar in
     * @param schema schema, if available, to check whether attributes are binary or not
     */

    public LdifImport(DataBrokerQueryInterface dataSource, SmartTree mrTree, Frame owner, SchemaDataBroker schema)
    {
        this.owner = owner;

        this.schema = schema;

        tree = mrTree;
        DN base = tree.getCurrentDN();

        offline = tree.isEmpty();

        this.dataSource = dataSource;

        //importFile(null)  // call separately to make constructor and action obvious.
    }

    /**
     *    Read a subtree from an ldif file, adding entries as
     *    they are read.
     *
     *    THere's an open question here as to how strict we should be reading an ldif file -
     *    the approach taken is to throw an error and stop if the ldif file is obviously corrupt,
     *    but to allow minor errors through.
     *
     *    This is the major switchboard of the LdifImport class; from there test and 'real' imports
     *    are done for both normal and changetype LDIF files.
     *
     *    @param textStream The stream being read (i.e. the ldif file)
     *    @param broker the broker to send the read data to
     *    @param query the DataQuery doing the import
     *    @param preview whether we're just doing a preliminary test run that doesn't modify the directory
     *    @return after running in 'preview' mode, whether to run again and import the file for real.
     * @throws NamingException if there is an error reading the ldif file
     */

    public boolean readLdifTree(InputStream textStream, DataBroker broker, DataQuery query, boolean preview)
            throws NamingException
    {
        HashSet<String> addedDNs = new HashSet<String>();   // used for testing the ldif file for consistency in 'preview' mode
        HashSet<String> deletedDNs = new HashSet<String>(); // ditto


        DXEntry apex = null;               // the top of the ldif tree in this file
        int numEntriesRead = 0;
        LdifEntry newEntry;                                     // object to place newly read entry info in
        ArrayList<LdifEntry> testResults = new ArrayList<LdifEntry>();

        LdifStreamReader readText = getLdifStreamReader(textStream);

        try
        {
            ArrayList<DN> list = new ArrayList<DN>();		    //TE: stores the root DN('s ).

            String progressNote = CBIntText.get("reading entry #");

            if (preview)
            {
                addedDNs = new HashSet<String>();
                deletedDNs = new HashSet<String>();
            }

            try
            {
                while (((newEntry = ldifutil.readLdifEntry(readText))!=null))
                {
                    if (query.isCancelled()) return false;            // check whether the user has cancelled this query.

                    int size = newEntry.getDN().size();

                    if (size != 0)
                    {
                        DN newDN = newEntry.getDN();

                        if (apex == null || apex.getDN().size() > size)
                            apex = newEntry;                    // keep track of top entry...

                        if (offline)
                            translateToUnicode(newEntry);       // text magic for standalone offline viewing

                        if (preview)
                        {
                            int lineNumber = readText.getLineNumber();
                            if (newEntry.getChangeType() == LdifEntryType.normal)
                                testResults.add(testNormalLdifImpact(newEntry, broker, addedDNs, lineNumber));
                            else
                                testResults.add(testLdifChangeEntryImpact(newEntry, broker, addedDNs, deletedDNs, lineNumber));
                        }
                        else
                        {
                            if (newEntry.getChangeType() == LdifEntryType.normal)
                                addLdifNormalEntry(newEntry, broker);
                            else
                            {
                                if (broker instanceof OfflineDataBroker)
                                    addLdifChangeTypeEntry(newEntry, broker);  // special hack to allow 'viewing' of LDIF change files?
                                else
                                    executeLdifChangeTypeEntry(newEntry, broker);
                            }
                        }

                        if (!list.contains(getRoot(broker, newDN)))
                            list.add(getRoot(broker, newDN));
                    }
                    else
                    {
                        log.warning("skipping ldif data at line: " + ldifutil.getCurrentLineNumber());
                    }

                    if (pmonitor!=null)   // unit tests do not always set up the progress monitor
                        pmonitor.getProgressMonitor().setNote(progressNote + " " + (++numEntriesRead));  // XXX I'm not translating this to the MessageFormat version of CBIntText.get because I'm worried about performance - CB
                }
            }
            catch (InterruptedIOException e)    // almost certainly the user hitting 'cancel' on the progress bar
            {
                return false;
            }
            catch (NamingException e)
            {
                //TE: bug: 5153.  Not sure if this should be caught here and stop or should it try to process the rest of the LDIF file?
                CBUtility.error(CBIntText.get("An error occured while processing the LDIF file") + " line: " + readText.getLineNumber(), e);
                e.printStackTrace();
                return false;
            }

            if (preview)
            {
                return showTestResults(testResults, apex);
            }
            else
            {
                // quicky check to see if we're working off line, or need to set the
                // root for some other reason (can't actually think of any...)

                if (tree.isEmpty())
                {
                    //TE: if there are multiple root DNs this ensures that they are displayed (see bug 529),
                    //    e.g o=CA1 & o=CA2.
                    //	  However, I have a feeling this could make other things fall over...but have no idea
                    //    what at this stage because this only keeps account of the last root DN (e.g. o=CA2)
                    //	  whereas there may be several...
                    //CB: TODO: sort this out; the tree model doesn't really allow for multiple roots, so this is working by accident :-)
                    for(int i=0;i<list.size();i++)
                    {
                        DN root = (list.get(i));

                        tree.setRootDN(root);  // bit of a hack; pass the last known real DN
                        tree.expandRootDN();
                        tree.getRootNode().setStructural(true);
                    }
                }

                // give the tree a kick to make it refresh the apex parent, thus displaying
                // the newly imported nodes.
                if (apex != null && apex.getDN() != null && apex.getDN().size() > 1)
                    tree.refresh(apex.getDN().getParent());
            }
        }
        catch (Exception e2)
        {
            e2.printStackTrace();
            CBUtility.error(CBIntText.get("An error occured while processing the LDIF file") + " -" + readFile.toString() + ":" + readText.getLineNumber(), e2);
        }
        return false;
    }

    protected LdifStreamReader getLdifStreamReader(InputStream textStream) throws NamingException
    {
        LdifStreamReader readText;
        try
        {
            readText = new LdifStreamReader(new InputStreamReader(textStream, "UTF-8"));
        }
        catch (IOException e)  // shouldn't happen, but sig throws IOException and Unsupported
        {
            throw new NamingException(CBIntText.get("Unexpected problem - Unable to read the LDIF file") + ": " + e.getMessage());
        }
        return readText;
    }

    /**
     * Displays the test results.  If there are no errors, asks the user whether they would like to import the file immediately.
     * @param testResults
     * @param apex
     * @return whether to continue and import the file.
     */
    public boolean showTestResults(ArrayList<LdifEntry> testResults, DXEntry apex)
    {
        boolean error = false;
        JTextArea text = new JTextArea("The following LDIF errors were found.  The server is unlikely to accept the file.\n\n"); // only used when there's an error

        for (LdifEntry entry:testResults)
        {
            if (entry.contains(LdifEntry.ERROR))
            {
                text.append(" [" + entry.getString(LdifEntry.LINENO) + "] " + entry.getDN() + " -> " + entry.getString(LdifEntry.ERROR) + "\n");
                error = true;
            }
        }

        if (error)
        {
            JOptionPane.showMessageDialog(owner,text, CBIntText.get("Errors found!"), JOptionPane.ERROR_MESSAGE);
        }
        else if (apex == null)
        {

            JOptionPane.showMessageDialog(owner,CBIntText.get("No Ldif Entries were found.  File is empty or corrupt."), CBIntText.get("Errors found!"), JOptionPane.ERROR_MESSAGE);
        }
        else
        {
            int result = JOptionPane.showConfirmDialog(owner,CBIntText.get("No LDIF errors found (however the client can't completely test an LDIF file, and the server may still find errors).\nImport Now?"), CBIntText.get("No errors"), JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION)
                return true; // import the file for real!
        }
        return false;  // usually we do not immediately import the file...
    }

    /**
     * Adds the entry to the directory
     * @param newEntry
     * @param broker
     * @throws NamingException
     */
    protected void addLdifNormalEntry(LdifEntry newEntry, DataBroker broker)
            throws NamingException
    {
        broker.unthreadedModify(null, newEntry);
    }

    /**
     * This adds the LDIF change entries in a form suitable for viewing; i.e. it doesn't
     * actually try to evaluate the changes, but simply displays them as raw data.  It's intended
     * to be used in the offline viewer.
     * @param newEntry
     * @param broker
     * @throws NamingException
     */
    protected void addLdifChangeTypeEntry (LdifEntry newEntry, DataBroker broker)
                   throws NamingException
     {
        // add some synthetic attributes to make the display pretty

         //newEntry.put(new BasicAttribute("changetype", newEntry.getChangeType().toString()));

         broker.unthreadedModify(null, newEntry);
     }
    /**
      * This executes the LDIF change entry modification list.
      *
      * @param newEntry
      * @param broker
      * @return an annotated entry with embedded  error and informational messages
      * @throws NamingException
      */
    protected void executeLdifChangeTypeEntry(LdifEntry newEntry, DataBroker broker)
               throws NamingException
     {
        LdapContext ctx;

         // strip out fake 'changetype' attribute
         newEntry.remove(LdifEntry.CHANGE_TYPE_ATTRIBUTE);

         switch (newEntry.getChangeType())
         {
             case add:
                 broker.unthreadedModify(null, newEntry);
                 break;

             case delete:
                 broker.unthreadedModify(newEntry, null);
                  break;

             // cf: http://docs.oracle.com/javase/tutorial/jndi/ldap/rename.html
             case moddn:
             case modrdn:

                  ctx = broker.getLdapContext();  // we have to go down to direct LDAP ops to make this work

                 String newSuperior = newEntry.getString(LdifEntry.NEWSUPERIOR);
                 String newRDN  = newEntry.getString(LdifEntry.NEWRDN);
                 String deleteOldRDN = Boolean.toString(newEntry.getString(LdifEntry.DELETEOLDRDN).equals("1"));

                 String newName = newRDN + "," + newSuperior;
                 String oldName = newEntry.getStringName();

                 ctx.addToEnvironment("java.naming.ldap.deleteRDN", deleteOldRDN);
                 ctx.rename(oldName, newName);

                 break;

             // check that modifications are valid (i.e. we're deleting attributes that exist, and adding ones that don't.
             case modify:

                 /*  Just a reminder  :-)
                  public ModificationItem(int mod_op, Attribute attr)
                  Creates a new instance of ModificationItem.
                  Parameters:
                  mod_op - Modification to apply. It must be one of: DirContext.ADD_ATTRIBUTE DirContext.REPLACE_ATTRIBUTE DirContext.REMOVE_ATTRIBUTE
                  attr - The non-null attribute to use for modification.
                 */

                 // SEE ALSO: DXOps.ModifyEntry()

                 ctx = broker.getLdapContext();  // we have to go down to direct LDAP mod ops to make this work, but it's pretty basic...

                 try
                 {
                     ArrayList<ModificationItem> mods = new ArrayList<ModificationItem>();

                     for (DXAttribute att: newEntry.getAttArrayList())
                     {
                         LdifModifyAttribute modifyAtt = (LdifModifyAttribute)att;

                         Attribute cleanAtt = modifyAtt.getCleanAttribute();

                         switch (modifyAtt.modifyType)
                         {
                             case add:

                                 mods.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, cleanAtt));
                                 break;

                             case replace:

                                 mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, cleanAtt));
                                 break;

                             case delete:

                                 mods.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, cleanAtt));
                                 break;
                         }

                     }
                     ctx.modifyAttributes(newEntry.getDN(), mods.toArray(new ModificationItem[mods.size()]));


                 }
                 catch (ClassCastException e)   // shouldn't happen
                 {
                     newEntry.put(LdifEntry.ERROR, CBIntText.get("unexpected internal error parsing modify attributes"));
                 }
         }
     }

    /**
     * Evaluates the difference between the changes listed in the newEntry and the
     * existing entry (if any)
     * and returns the result as an 'Ldif Entry', e.g. a DXEntry containing a
     * change type attribute and a list of zero or more LdifModifyAttributes.
     *
     * Unlike a normal LDIF entry however it is verbose and includes details of
     * deleted data - because we're using this to report to the user.
     *
     * Note: this entry is *not* intended to be written to the directory, and contains
     * synthetic attributes (e.g. for error reporting)
     *
     * @param newEntry
     * @param broker
     * @return an annotated entry with embedded  error and informational messages
     * @throws NamingException
     */
    protected LdifEntry testLdifChangeEntryImpact(LdifEntry newEntry, DataBroker broker, HashSet<String> addedDNs, HashSet<String>deletedDNs, int lineNumber)
              throws NamingException
    {
        // TODO: create added entry list and deleted entry list, to allow for deletes and add backs, and tests for children...

        DN dn = newEntry.getDN();
        LdifEntry existingEntry;
        boolean error = false;


        if (broker.unthreadedExists(dn))  // check that entry we're changing actually exists
        {

            switch (newEntry.getChangeType())
            {
                // check that we're adding a new entry
                case add:
                    if (deletedDNs.contains(dn)==false)   // it's just possible that we're adding back a deleted entry I guess...
                        newEntry.put(LdifEntry.ERROR, CBIntText.get("Entry already exists!  (Use changetype: modify)"));
                    else
                        deletedDNs.remove(dn);  // we're adding back a deleted DN...
                    break;

                // check that we're deleting entries / attribute values that actually exist, and that don't have children.
                case delete:
                    DXNamingEnumeration children = broker.unthreadedList(dn);
                    if (children.size()>0)
                        while (children.hasMore())
                        {
                            SearchResult result = (SearchResult)children.nextElement();
                            System.out.println("TESTING: " + result.getName());
                            if (deletedDNs.contains(new DN(result.getName())) == false);
                            {
                                error = true;
                                newEntry.put(LdifEntry.ERROR, CBIntText.get("Entry has children; delete will fail unless children are deleted first"));
                            }
                        }

                    if (error == false)
                        deletedDNs.add(dn.toString());
                    break;

                // check that we can change the name appropriately; e.g. doesn't already exist
                case moddn:
                case modrdn:

                    String newSuperior = newEntry.getString(LdifEntry.NEWSUPERIOR);
                    String newRDN  = newEntry.getString(LdifEntry.NEWRDN);
                    String deleteOldRDN = newEntry.getString(LdifEntry.DELETEOLDRDN);

                    if (newRDN == null)
                        newEntry.put(LdifEntry.ERROR, CBIntText.get("No 'newrdn' or 'newdn' value found in entry"));
                    else
                    {
                        if (newSuperior==null)
                            newSuperior = dn.getParent().toString();
                        DN newDN = new DN(newRDN + "," + newSuperior);
                        if (broker.unthreadedExists(newDN) || addedDNs.contains(newDN))
                            newEntry.put(LdifEntry.ERROR, CBIntText.get("new dn already exists: "));
                        else
                        {
                            addedDNs.add(newDN.toString());
                            deletedDNs.add(dn.toString());
                        }
                    }
                    break;

                // check that modifications are valid (i.e. we're deleting attributes that exist, and adding ones that don't.
                case modify:

                    existingEntry = new LdifEntry(broker.unthreadedReadEntry(dn, null));

                    for (DXAttribute att: newEntry.getAttArrayList())
                    {
                        if (!LdifEntry.CHANGE_TYPE_ATTRIBUTE.equals(att.getID()))
                        {
                            try
                            {
                                LdifModifyAttribute modifyAtt = (LdifModifyAttribute)att;

                                modifyAtt.remove(0); // clear away the fake 'delete', 'add' or 'replace' value...

                                DXAttribute oldAtt = existingEntry.get(modifyAtt.getID());

                                // TODO: test for terminal white space?  Which stuffs things up if there are accidental spaces or tabs at the end of a line...?
                                // TODO: (remove same for the real thing?)

                                switch (modifyAtt.modifyType)
                                {
                                    case add:
                                        if (oldAtt == null)
                                            break;   // adding a completely new attribute...

                                        if (oldAtt.isSingleValued())
                                            newEntry.put(LdifEntry.ERROR, CBIntText.get("unable to add to attribute {0} as it is single valued.  (Use 'replace')", new String[] {modifyAtt.getID()}) );
                                        else
                                            for (Object o: modifyAtt.getValues())
                                            {
                                                if (oldAtt.contains(o))
                                                    newEntry.put(LdifEntry.ERROR, CBIntText.get("unable to add value {0} from attribute {1} as it already exists", new String[] {o.toString(), modifyAtt.getID()}) );
                                            }
                                            break;

                                    case replace:
                                            // replace operations are pretty bullet proof... check for single valued shennanigans I guess...
                                        if (oldAtt != null && oldAtt.isSingleValued() && modifyAtt.size()>1)
                                            newEntry.put(LdifEntry.ERROR, CBIntText.get("unable to replace attribute {0} with multiple values as it is single valued.", new String[] {modifyAtt.getID()}) );

                                        break;

                                    case delete:
                                        if (oldAtt == null || oldAtt.size()==0)
                                            newEntry.put(LdifEntry.ERROR, CBIntText.get("unable to delete attribute {0} as it does not exist", new String[] {modifyAtt.getID()}) );
                                        else
                                        {
                                            for (Object o: modifyAtt.getValues())
                                            {
                                                if (oldAtt.contains(o)==false)
                                                    newEntry.put(LdifEntry.ERROR, CBIntText.get("unable to delete value {0} from attribute {1} as it does not exist", new String[] {o.toString(), modifyAtt.getID()}) );
                                            }

                                        }
                                        break;

                                    case test:  // ignore
                                        break;
                                }
                            }
                            catch (ClassCastException e)   // shouldn't happen
                            {
                                if (att instanceof DXAttribute)
                                    newEntry.put(LdifEntry.ERROR, CBIntText.get("attribute {0} does not have a change operation", new String[] {att.getID()}));
                                else
                                    newEntry.put(LdifEntry.ERROR, CBIntText.get("internal error parsing modify attributes for dn") + " (type: " + att.getClass().toString() + ")");
                            }
                        }
                    }
            }
        }
        else if (!broker.unthreadedExists(dn.getParent()))
        {
            if (addedDNs.contains(dn.getParent().toString())==false)
                if (!offline)
                    newEntry.put(LdifEntry.ERROR, CBIntText.get("Adding Entry without a parent. DN {0} does not exist, and has not yet appeared in LDIF file.", new String[] {dn.getParent().toString()}));
        }
        else // if it doesn't exist, make sure we're doing a changetype: add operation
        {
            if (newEntry.getChangeType()!=LdifEntryType.add) // check for errors; anything but 'add' should fail
                newEntry.put(CBIntText.get("error"), CBIntText.get("Entry does not exist!  (Use changetype: add)"));
            if (addedDNs.contains(dn))
                newEntry.put(LdifEntry.ERROR, CBIntText.get("new dn already exists"));

        }

        newEntry.put(new DXAttribute(LdifEntry.LINENO, lineNumber));

        return newEntry;
    }

    /**
     * Evaluates the difference between the newEntry and the existing entry (if any)
     * and returns the result as an 'Ldif Entry', e.g. a DXEntry containing a
     * change type attribute and a list of zero or more LdifModifyAttributes.
     *
     * Note this does not do moddn/modrdn operations; the method can't tell the difference
     * between an add and a moddn, and assumes an 'add' if entries have different names.
     *
     * If the entry returned is an 'add', then there was no existing entry and this is a
     * complete replacement.  If it is a 'modify', then it is overwriting an existing entry.
     *
     * @param newEntry
     * @param broker
     * @return
     * @throws NamingException
     */
    LdifEntry testNormalLdifImpact(DXEntry newEntry, DataBroker broker, HashSet<String> addedDNs, int lineNumber)   // package visibility for testing...
              throws NamingException
    {
        // check how the (normal) ldif entry would go merging with a normal directory...
        // is there an existing entry (which would fail)?  Is there a parent (without which it will fail)
        DN dn = newEntry.getDN();

        LdifEntry ldifEntry = new LdifEntry(dn);
        ldifEntry.put(new DXAttribute(LdifEntry.LINENO, lineNumber));

        if (broker.unthreadedExists(dn))
        {
            // problem - can't overwrite directory
            ldifEntry.put(LdifEntry.ERROR, CBIntText.get("Entry already exists (use changetype: modify)"));
        }
        else if (addedDNs != null && addedDNs.contains(dn))
        {
            ldifEntry.put(LdifEntry.ERROR, CBIntText.get("Entry exists twice in LDIF File"));
        }
        else if (!broker.unthreadedExists(dn.getParent()))
        {
            if (addedDNs.contains(dn.getParent().toString())==false)
                if (!offline)
                    ldifEntry.put(LdifEntry.ERROR, CBIntText.get("Adding Entry without a parent. DN {0} does not exist, and has not yet appeared in the LDIF file.", new String[] {dn.getParent().toString()}));
        }
        else
        {
            addedDNs.add(dn.toString());  // looks good... add it to the list of DNs added from this LDIF file
        }

        return ldifEntry;

    }

    /**
     * UNTESTED
     *
     * Gets the difference of two entries as an LDIF changetype entry.
     * @param newEntry
     * @param broker
     * @return
     * @throws NamingException
     */
    LdifEntry getDiff(DXEntry newEntry, DataBroker broker)
              throws NamingException
    {

        /*
         *  On a normal LDIF file import, we replace the complete entry.  If there *is*
         *  an existing entry, this procedure figures out the difference for reporting
         *  in the test result.
         */

        DN dn = newEntry.getDN();
        LdifEntry ldifEntry = new LdifEntry(dn);

        if (broker.unthreadedExists(dn))
        {
            // we're doing a modification of an existing entry
            ldifEntry.setChangeType(LdifEntryType.modify);

            DXEntry oldVals = broker.unthreadedReadEntry(dn, null);

            DXAttributes deletions = DXAttributes.getDeletionSet(null, oldVals, newEntry);
            DXAttributes additions = DXAttributes.getAdditionSet(null, oldVals, newEntry);
            // note: this *only* does 'single value' replacements - tweak below to handle multi-value replacements.
            DXAttributes replacements = DXAttributes.getReplacementSet(null, oldVals, newEntry);

            //
            // We now have a basic set of add, deletes and replacements, which is o.k. for an LDIF change file.
            // However, we now handle two variants to make the LDIF file 'neat':
            // a) where we have a deletion that removes all values, followed by an addition, convert to a 'replace' operation
            // b) where we have a deletion that removes all values, convert to a 'full delete' (e.g. without attribute values)
            //
            for (DXAttribute att: oldVals)
            {
                String attributeID = att.getID();
                if (deletions.contains(attributeID))
                {
                        DXAttribute deletionAttVals = deletions.get(attributeID);
                        if (deletionAttVals.size() == att.size())
                        {
                            // we're deleting all values and starting over...
                            if (additions.contains(attributeID))
                            {
                                // We're completely replacing the attribute; remove from deletion/addition list and add to replacement list
                                replacements.put(newEntry.get(attributeID));
                                deletions.remove(attributeID);
                                additions.remove(attributeID);
                            }
                            else
                            {
                                // We're completely deleting the attribute;  - remove values as we'll nuke the whole attribute
                                deletions.remove(attributeID);
                                deletions.put(new DXAttribute(attributeID)); // adding back with no value gives us a 'delete everything' LDIF command
                            }
                        }
                }
            }

            // load up our modify Ldif Entry
            for (DXAttribute addition: additions)
                ldifEntry.put(new LdifModifyAttribute(addition, LdifModifyType.add));

            for (DXAttribute deletion: deletions)
                ldifEntry.put(new LdifModifyAttribute(deletion, LdifModifyType.delete));

            for (DXAttribute replacement: replacements)
                ldifEntry.put(new LdifModifyAttribute(replacement, LdifModifyType.replace));
        }
        else
        {
            // we're doing an addition of an entirely new entry, so everything is a change!
            ldifEntry = new LdifEntry(newEntry);
            ldifEntry.setChangeType(LdifEntryType.add);
        }

        return ldifEntry;


    }

    static ProgressMonitorInputStream pmonitor;

    private static Logger log = Logger.getLogger(LdifImport.class.getName());

    public void selectAndImportFile()
    {
        importFile(null);
    }

    public void importFile(String fileName)
    {
        boolean testing = false;

        if (fileName == null)
        {
            LdifFileChooser chooser = new LdifFileChooser(JXConfig.getProperty("ldif.homeDir"));
            int option = chooser.showOpenDialog(owner);
            if (option == LdifFileChooser.CANCEL_OPTION) return;
            readFile = chooser.getSelectedFile();

            /*
            if (readFile == null) // not sure that this can happen...
            {
                CBUtility.error(CBIntText.get("Please select a file"));
                return;
            }
            */
            JXConfig.setProperty("ldif.homeDir", readFile.getParent());  // need to set the ldif file home directory for in-ldif file imports... this is *not* thread safe!!!

            if (option == LdifFileChooser.APPROVE_OPTION)
                testing = true;

            //readFile = chooser.getSelectedFile();
        }
        else
            readFile = new File(fileName);

        ldifutil.setFileDir(readFile.getParent());

        doFileRead(readFile, testing);

        if(owner instanceof JXplorerBrowser  && offline)
        {
            ((JXplorerBrowser)owner).getMainMenu().setConnected(false);
        }

    }



    public String getFileName()
    {
        return (readFile==null)?"<empty>":readFile.getName();
    }


    /**
     * Reads the file data into the directory
     * @param readFile
     */
    protected void doFileRead(File readFile, boolean test)
    {
        if (readFile == null)
            CBUtility.error(CBIntText.get("unable to read null LDIF file"), null);

        final File myFile = readFile;
        final boolean testStatus = test;

        DataQuery ldifQuery = new DataQuery(com.ca.directory.jxplorer.broker.DataQuery.EXTENDED)
        {
            public ProgressMonitorInputStream getInputStream(File file)
            {
                try
                {
                    //xxx
                    FileInputStream rawBytes = new FileInputStream(file);
                    pmonitor = new ProgressMonitorInputStream(owner, CBIntText.get("Reading LDIF file"), rawBytes);
                }
                catch (FileNotFoundException e)
                {
                    CBUtility.error(CBIntText.get("Unable to read the LDIF file ''{0}''.", new String[] {myFile.toString()}), e);
                    return null;
                }

                // hmmm.. how will this work in a multi-threaded environment?
                // ... might be better to pass a new ldif utility object into readLdifTree... however they shouldn't
                // be reading more than one ldif file at a time, and the only affect would be to stuff up ldif error reporting...
                ldifutil.resetErrorReportingInformation(myFile.toString());
                return pmonitor;
            }

            public void doExtendedRequest(DataBroker dataBroker)
            {
                pmonitor = getInputStream(myFile);

                try
                {
                    // future expansion point - allow offline broker knowledge of ldif file to allow saving on submit...
                    try
                    {
                        if (dataBroker instanceof OfflineDataBroker)
                            ((OfflineDataBroker)dataBroker).setLdifFile(myFile);
                    }
                    catch (ClassCastException e)
                    {
                        CBUtility.error("unexpected internal exception reading LDIF file " + e.getMessage());
                        //unlikely, but doesn't really matter}
                    }

                    if (readLdifTree(pmonitor, dataBroker, this, testStatus))
                    {
                        pmonitor = getInputStream(myFile); // renew the input stream
                        readLdifTree(pmonitor, dataBroker, this, false);  // import file for real
                    }
                }
                catch (NamingException e)
                {
                    CBUtility.error(CBIntText.get("There were one or more errors reading the LDIF file\n(See the log for more details)"), e);
                }
                catch (Exception e)
                {
                    CBUtility.error(CBIntText.get("There were one or more errors reading the LDIF file\n(See the log for more details)"), e);
                    System.out.println("=== ran CBUtility error code... ====");
                }
                closeDown();
            }
        };

        dataSource.extendedRequest(ldifQuery);
    }

    protected DN getRoot(DataBroker b, DN lastKnownDN)
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
                test = root.getParent();
                if ((test==null)||(b.unthreadedExists(test)==false)||test.size()==0)
                    return root;
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
        catch (IOException e) {}
    }
    
    /**
     *    If schema broker is active, use schema to check whether
     *    we have a utf-8 encoded unicode string.  If we do, decode
     *    it.  If schema is not active, use heuristic to check if 
     *    binary thingumy is utf-8, and if it is, translate it anyway.
     */
     
    protected void translateToUnicode(DXEntry entry)
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

}


