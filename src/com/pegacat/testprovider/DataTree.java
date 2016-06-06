package com.pegacat.testprovider;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.ldap.LdapName;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * A simple tree data structure containing entries to allow a
 * light weight 'fake' directory to be used for standalone unit tests.
 * NB: always has a "" root node...
 */


public class DataTree extends HashMap <String, TreeEntry>
{

    public DataTree()
    {
        super();
    }

    /**
     * over ride base HashMap method to provide type safety.
     * @param key
     * @return the unique tree entry described by the key.
     */
    public TreeEntry get(String key)
    {
        return (TreeEntry) super.get(key);
    }
    /**
     * Adds an entry to the tree.  Requires entry to be added to the parent,
     * or a new 'place holder' parent to be created.
     *
     * Some extra hoops are jumped through in case the nodes are not added in
     * 'shortest first' order, or there are missing parent nodes.  This can
     * lead to the creation of fake 'place holder' parent nodes.
     *
     *
     * @param newEntry the new entry to add to the TestTree data object
     */

    // XXX need to modify to cope with adding existing entries better (e.g. when overwriting during backup)
    public void addEntry(TreeEntry newEntry)
    {
        Name entryName = newEntry.getName();

        Name parentName = null;
        String parentNameS = null;
        if (entryName.size()>0)  // the root node has no parent...
        {
            parentName = entryName.getPrefix(entryName.size()-1);
            parentNameS = parentName.toString();
        }
        String stringEntryName = entryName.toString();

        if (this.containsKey(stringEntryName))  // are we replacing a (possibly place holder) node? If so, grab the children of that placeholder
        {
            TreeEntry placeHolder = (TreeEntry)this.get(stringEntryName);
            newEntry.children = (ArrayList<TreeEntry>)placeHolder.children.clone();
            TreeEntry parent = ((TreeEntry)this.get(parentNameS));
            if (parent != null)  // i.e. make sure we are not the root node..
                parent.replaceChild(placeHolder, newEntry);  // add self to parent, replacing predecessor...
        }
        else if (parentName != null) // mess around sorting out the parent entry child list
        {
            if (this.containsKey(parentNameS)) // parent entry already exists
            {
                ((TreeEntry)this.get(parentNameS)).addChild(newEntry);  // add self to parent...
            }
            else    // no parent entry; create place holder
            {
                TreeEntry placeHolder = new TreeEntry(parentName, new String[] {"objectclass", "placeHolder"});
                placeHolder.addChild(newEntry);
                addEntry(placeHolder);
            }
        }

        this.put(entryName.toString(), newEntry);
    }

    /**
     * removes an entry from the test tree, removes child entries,
     * and removes the parent reference.
     * @param name
     */
    public void deleteEntry(String name)
    {
        TreeEntry deleteMe = (TreeEntry)this.get(name);
        deleteEntry(deleteMe);
    }

    public void deleteEntry(TreeEntry deleteMe)
    {
        if (deleteMe != null)
        {
            // go through in reverse, since deleting questions modifies the questions ArrayList
            // (they delete the parent reference as they go...)
            int size = deleteMe.children.size();
            for (int i=size; i>0; i--)
                deleteEntry(deleteMe.children.get(i-1));

            // delete the reference pointer in the parent to the deleteMe node
            if (deleteMe.getName().isEmpty() == false)  // the root node has no parent, so skip it
            {
                TreeEntry parent = (TreeEntry)this.get(deleteMe.getParent().toString());
                parent.removeChild(deleteMe);
            }

            this.remove(deleteMe.getName().toString());
        }
    }

    /**
     * WARNING.
     * this does not modify the entry.
     * this does not rename questions. (yet).
     * It simply changes the hash key to the newName, leaving all parent/child relationships as is.
     * HENCE IT IS HIGHLY DANGEROUS, AND MUST ONLY BE USED FOR LEAF NODE RENAMES!
     * @param oldName
     * @param newName
     */
    public void renameEntry(String oldName, String newName)
        throws NamingException
    {
        TreeEntry entry = get(oldName);

        entry.setName(new LdapName(newName));
        this.remove(oldName);
        this.put(newName, entry);


    }

    /**
     * debug print - dump the tree to standard out.
     */
    public void dump()
    {
        dumpEntry((TreeEntry)get(""), 0);
    }

    public void dumpEntry(TreeEntry entry, int indent)
    {
        for (int i=0; i<indent*4; i++)
            System.out.print(" ");

        if (entry.getStringName().length() == 0)
            System.out.println("<root>");
        else
            System.out.println(entry.getStringName());

        indent++;

        for (TreeEntry child : entry.getChildren())
            dumpEntry(child, indent);
    }

}
