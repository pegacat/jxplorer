package com.ca.commons.naming;


import com.ca.commons.cbutil.*;
import com.ca.commons.jndi.AdvancedOps;
import com.ca.commons.jndi.ConnectionData;

import javax.naming.*;
import javax.naming.directory.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper for BasicOps that converts the jndi primative names
 * into com.ca.commons.naming objects...
 */

public class DXOps extends AdvancedOps
{

    private final static Logger log = Logger.getLogger(DXOps.class.getName());

    /**
     * Initialise with the directory context.
     */


    public DXOps(DirContext ctx)
            throws NamingException
    {
        super(ctx);
    }

    public DXOps(ConnectionData cData)
            throws NamingException
    {
        super(cData);
    }


    /**
     * This preparses a name, preparitory to passing to the jndi operation.
     * Usefull to over-ride if a Name needs to be escaped or re-formatted.
     *
     * @param name the pre jndi operation name.
     * @return the version used by the operation.
     */

    public Name preParse(Name name)
    {
        //(assuming jndi doesn't mess with the names it's given, we don't need this...)
        //DN newName = (name instanceof DN)?new DN((DN)name):new DN(name);
        return name;
    }

    /**
     * This postparses a name, after it has been returned from the jndi operation.
     * Usefull to over-ride if the name needs to be unescaped or reformatted.
     *
     * @param name the post jndi operation name.
     * @return the re-formatted version used by the application.
     */

    public Name postParse(Name name)
    {
        return postParse(name.toString());
    }

    /**
     * This postparses a name, after it has been returned from the jndi operation.
     * It assumes that it has got a jndi <i>CompositeName</i> that needs to be
     * converted to a legal ldap dn (i.e. an ldap <i>CompoundName</i>).  If this
     * is *not* the case, there will be trouble...
     *
     * @param name the post jndi operation name.
     * @return the re-formatted version used by the application, as a DN object.
     */
//TODO: figure out if this is needed still.
//TODO: can we avoid the horrible, horrible, horrible JNDI Composite name disaster?
// (Ans: I don't think so; jndi gives us back strings that are composite names, even though
// we are never using federated name spaces.  So we have to warp the whole program to cope with
// them)

    public Name postParse(String name)
    {
        /* EMERGENCY HACK
         * (JNDI apparently does not handle terminating spaces correctly - it
         * retains the escape characters, but trims the actual space, resulting
         * in an illegal ldap dn)
         */
        if (name.charAt(name.length() - 1) == '\\')
        {
            name = NameUtility.checkEndSpaces(name);
        }


        try
        {
            Name cn = new CompositeName(name);
            if (cn.size() == 0)                     // if the name is empty ...
                return new DN();                  // ... just return an empty DN

            return new DN(cn.get(cn.size() - 1));  // get the last element of the composite name, which will be the ldap compound name, and init the DN with that.
        }
        catch (NamingException e)     // should never happen :-) (ROTFL)
        {
            log.log(Level.WARNING, "unexpected error: bad name back from jndi ftn in CBOps.postParse(" + name + ")?", e);
            e.printStackTrace();
            //System.exit(-1);
            return new DN(name);  // bad server response?  return (possibly) corrupt name anyway...
        }
    }

    /**
     * This postparses a name, after it has been returned from the jndi operation.
     * It assumes that it has got a jndi <i>CompositeName</i> that needs to be
     * converted to a legal ldap dn (i.e. an ldap <i>CompoundName</i>).  If this
     * is *not* the case, there will be trouble...
     *
     * @param name the post jndi operation name.
     * @return the re-formatted version used by the application as an ldap String.
     */
    public String postParseString(String name)
    {
        /* EMERGENCY HACK
         * (JNDI apparently does not handle terminating spaces correctly - it
         * retains the escape characters, but trims the actual space, resulting
         * in an illegal ldap dn)
         */

        if (name.length() == 0)
            return name;

        if (name.charAt(name.length() - 1) == '\\')
        {
            name = NameUtility.checkEndSpaces(name);
        }
        if (name.startsWith("ldap://"))
        {
            try
            {
                name = URLDecoder.decode(name, "UTF8");
            }
            catch (UnsupportedEncodingException e)
            {
                log.severe("unexpected error: couldn't URL decode in CBOps.postParseString(" + name + ")?\n" + e.toString());
                e.printStackTrace();
                return name;
            }
        }

        //if (name.indexOf('\\')>0 || name.indexOf('/')>0)
        //    System.out.println("PRE  postParseName: " + name);


        try
        {
            Name cn = new CompositeName(name);    // If it isn't a composite name, this will fail horribly.  But since it's JNDI, and therefore not object oriented, we have no way of telling...
            if (cn.size() == 0)                     // if the name is empty ...
                return "";                  // ... just return an empty DN

            name = cn.get(cn.size() - 1);
            //      if (name.indexOf('\\')>0 || name.indexOf('/')>0)
            //          System.out.println("POST postParseName: " + name);

            return name;  // get the last element of the composite name, which will be the ldap compound name, and init the DN with that.
        }
        catch (NamingException e)     // should never happen :-) (ROTFL)
        {
            log.log(Level.WARNING, "unexpected error: bad name back from jndi ftn in CBOps.postParseString(" + name + ")?", e);
            e.printStackTrace();
            return name;  // bad server response?  return (possibly) corrupt name anyway...
        }

    }


    /**
     * This postparses a namingEnumeration of NameClassPairs, after it has been returned from the jndi operation.
     * It returns a DXNamingEnumeration, and sets all the names to be the complete, full dn, rather than simply
     * the dn relative to the base.
     *
     * @param names the post jndi operation namingEnumeration.
     * @param base  the 'base' dn from which the names in the enumeration (may) be relative.
     *              If the Names in
     *              the enumeration are suffixed by the searchBase, they are unaltered, otherwise the searchBase
     *              is added to the names to give the full DN in the namespace.
     * @return the re-formatted version used by the application.
     */
    public NamingEnumeration postParseNameClassPairs(NamingEnumeration names, Name base)
    {
        log.finer("parsing with base :" + base.toString());
        DXNamingEnumeration dxe = new DXNamingEnumeration();

        String baseString = null;

        if (base != null && base.isEmpty() == false)
            baseString = base.toString();

        try
        {
            while (names.hasMore())
            {
                NameClassPair ncp = (NameClassPair) names.next();

                String rawName = postParseString(ncp.getName()).toString();

                // IMPORTANT!
                // This appends the 'base' DN to the enumerated DNs in order to get absolute DNs...

                if (ncp.isRelative() && baseString != null)
                {
                    if (rawName.length() != 0)
                        rawName = rawName + "," + baseString;
                    else
                        rawName = baseString;
                }

                log.finer("ended up with: '" + rawName + "'");
                ncp.setName(rawName);
                dxe.add(ncp);
            }
        }
        catch (NamingException ex)
        {
            CBUtility.error(CBIntText.get("Search partially failed! - only {0} entries returned", new Integer[]{new Integer(dxe.size())}), ex);
        }

        return dxe;
    }


    /*
     * <p>Takes a NamingEnumeration and converts any names from being
     * relative to the base to being full (i.e. including the base).</p>
     * @param
     * /

    protected NamingEnumeration setFullDNs(NamingEnumeration ne, Name base)
        throws NamingException
    {
System.out.println("setting full dns for " + base);

        DXNamingEnumeration dxne = new DXNamingEnumeration();

        while (ne.hasMore())
        {
            NameClassPair ncp = (NameClassPair)ne.next();


            // IMPORTANT!
            // This appends the 'base' DN to the enumerated DNs in order to get absolute DNs...

            if (ncp.isRelative() && base != null && base.size() > 0)
            {
                String rawName = postParseString(ncp.getName());

System.out.println("modifying: " + rawName + " + " + base);

                if (rawName.length() != 0)
                    rawName = rawName + "," + base.toString();
                else
                    rawName = base.toString();

System.out.println("ended up with: '" + rawName + "'");

                ncp.setName(rawName);  // I *think* this only needs to be done if we're changing stuff...
            }

            dxne.add(ncp);
        }

        return dxne;
    }
*/

    /**
     * Overload the corresponding method in JNDIOps so that we can play silly buggers with parsing
     * the stupid composite names that jndi inflicts on us.
     *
     * @param searchbase the search base
     * @param filter the search filter
     * @param limit size limit of the search
     * @param timeout time limit of the search
     * @param returnAttributes the attributes to return in the search
     * 
     * @return the search result
     * 
     * @throws NamingException if unable to perform the search
     */
    protected NamingEnumeration rawSearchBaseEntry(Name searchbase, String filter, int limit,
                                                   int timeout, String[] returnAttributes)
            throws NamingException
    {
        return postParseNameClassPairs(super.rawSearchBaseEntry(searchbase, filter, limit, timeout, returnAttributes), searchbase);
    }

    /**
     * Overload the corresponding method in JNDIOps so that we can play silly buggers with parsing
     * the stupid composite names that jndi inflicts on us.
     *
     * @param searchbase the search base
     * @param filter the search filter
     * @param limit size limit of the search
     * @param timeout time limit of the search
     * @param returnAttributes the attributes to return in the search
     * 
     * @return the search result
     * 
     * @throws NamingException if unable to perform the search
     */
    protected NamingEnumeration rawSearchOneLevel(Name searchbase, String filter, int limit,
                                                  int timeout, String[] returnAttributes) throws NamingException
    {
        return postParseNameClassPairs(super.rawSearchOneLevel(searchbase, filter, limit, timeout, returnAttributes), searchbase);
    }

    /**
     * overload the corresponding method in JNDIOps so that we can play silly buggers with parsing
     * the stupid composite names that jndi inflicts on us.
     *
     * @param searchbase the search base
     * @param filter the search filter
     * @param limit size limit of the search
     * @param timeout time limit of the search
     * @param returnAttributes the attributes to return in the search
     * 
     * @return the search result
     * 
     * @throws NamingException if unable to perform the search
     */
    protected NamingEnumeration rawSearchSubTree(Name searchbase, String filter, int limit,
                                                 int timeout, String[] returnAttributes) throws NamingException
    {
        return postParseNameClassPairs(super.rawSearchSubTree(searchbase, filter, limit, timeout, returnAttributes), searchbase);
    }

    /**
     * Update an entry with the designated DN.
     *
     * @param oldEntry the old entry containing the old set of attributes.
     * @param newEntry the new entry containing the replacement set of attributes.
     */

    public void modifyEntry(DXEntry oldEntry, DXEntry newEntry)
            throws NamingException
    {
        if (oldEntry != null) oldEntry.removeEmptyAttributes();
        if (newEntry != null) newEntry.removeEmptyAttributes();

        if (oldEntry == null && newEntry == null) // nothing to do.
        {
        }
        else if (oldEntry == null || (newEntry != null) && (newEntry.getStatus() == DXEntry.NEW)) // add
        {
            addEntryToDirectory(newEntry);
        }
        else if (newEntry == null) // delete
        {
            deleteTree(oldEntry.getDN());
        }
        else if (oldEntry.getDN() == null || newEntry.getDN() == null)
        {
            throw new NamingException("Internal Error: Entry with null DN passed to JNDIBroker unthreadedModify.  Modify Request Cancelled.");
        }
        else
        {
            // see if the name has changed, and modify it if it has
            handleAnyNameChange(oldEntry, newEntry);

            // check for change of attributes done in modify()
            updateEntry(oldEntry, newEntry);
        }
    }

    /**
     * Add the new entry to the directory & sets the status.
     *
     * @param newEntry the new entry containing the replacement set of attributes.
     */

    private void addEntryToDirectory(DXEntry newEntry)
            throws NamingException
    {
        addEntry(newEntry);

        newEntry.setStatus(DXEntry.NEW_WRITTEN); // once it's been added, it's no longer new...
    }

    /**
     * Add the new entry to the directory.
     *
     * @param newEntry the new entry containing the replacement set of attributes.
     */

    public void addEntry(DXEntry newEntry)
            throws NamingException
    {
        if (newEntry == null)
            throw new NamingException("Internal Error: null Entry passed to DXOps addEntry");

        if (newEntry.getDN() == null)
            throw new NamingException("Internal Error: Entry with null DN passed to DXOps addEntry");

        addEntry(newEntry.getDN(), newEntry);
    }


    /**
     * If the entry has changed its name, make the required calls to set up the
     * display tree and make the directory changes.
     *
     * @param oldEntry the old entry containing teh old set of attributes.
     * @param newEntry the new entry containing the replacement set of attributes.
     */

    private void handleAnyNameChange(DXEntry oldEntry, DXEntry newEntry)
            throws NamingException
    {
        // check for 'simple' rename from the tree, with no attributes involved.
        RDN oldRDN = oldEntry.getRDN();
        RDN newRDN = newEntry.getRDN();

        DN oldDN = oldEntry.getDN();
        DN newDN = newEntry.getDN();

        //System.out.println("oldDN & newDN " + oldDN.toString() + " :  " + newDN.toString());

        if (oldDN.equals(newDN))
            return;                     // nothing to see here, just move along.

        if (oldEntry.size() == 0 && newEntry.size() == 0) // a very simple rename, probably from the tree
        {
            moveTree(oldDN, newDN);
        }
        else if (oldRDN.isMultiValued() == false && newRDN.isMultiValued() == false)
        {
            //System.out.println("renaming: " + oldDN.toString() + " to " + newDN.toString());
            renameSingleValuedRDNS(oldEntry, newEntry);
        }
        else
        {
            renameComplexMultiValuedRDNs(oldEntry, newEntry);
        }
    }

    /**
     * do complex multi-valued RDN rename
     * WARNING - this assumes that the size and the values of the RDNs will not
     * BOTH CHANGE AT THE SAME TIME!
     */
    private void renameComplexMultiValuedRDNs(DXEntry oldEntry, DXEntry newEntry)
            throws NamingException
    {
        String[] oldRDNs = oldEntry.getRDN().getElements();
        String[] newRDNs = newEntry.getRDN().getElements();

        DN oldDN = oldEntry.getDN();
        DN newDN = newEntry.getDN();

        /*
         *  Create a list of 'lost RDNs' -> one's that are in the old RDN set but not in the new one.
         */

        Object[] temp = CBArray.difference(oldRDNs, newRDNs);
        String[] lostRDNs = new String[temp.length];
        for (int i = 0; i < temp.length; i++)
            lostRDNs[i] = temp[i].toString();



        /*
         *  Cycle through the list of 'lost' RDNs, working out whether
         *  they are *all* missing from the new Entry (in which case we
         *  can do a rename with 'deleteOldRDNs=true'), or they are *all*
         *  in the new Entry (in which case we can do a rename with
         *  'deleteOldRDNs=false').  If some are and some aren't, throw
         *  a hopefully useful error message.
         */
        final int NOT_SET = 0;
        final int IN_NEW_ENTRY = 1;
        final int NOT_IN_NEW_ENTRY = -1;

        int deleteRDNState = NOT_SET;

        for (int i = 0; i < lostRDNs.length; i++)
        {
            String RDN = lostRDNs[i];  // get Attribute Value Assertions from lostRDNs
            String type = RDN.substring(0, RDN.indexOf('='));
            String value = RDN.substring(RDN.indexOf('=') + 1);

            if (newEntry.get(type).contains(value) == true)
            {
                if (deleteRDNState == NOT_SET)      // first time through
                    deleteRDNState = IN_NEW_ENTRY;
                if (deleteRDNState != IN_NEW_ENTRY)
                {
                    setWierdoRDNError(oldDN, newDN);
                }
            }
            else
            {
                if (deleteRDNState == NOT_SET)      // first time through
                    deleteRDNState = NOT_IN_NEW_ENTRY;


                if (deleteRDNState != NOT_IN_NEW_ENTRY)
                {
                    setWierdoRDNError(oldDN, newDN);
                }
            }
        }

        /*
         *  perform the actual rename operation, followed by any entry
         *  tweaking that is required to make everything consistant.
         */

        if (deleteRDNState == NOT_SET || deleteRDNState == IN_NEW_ENTRY)
        {
            renameEntry(oldDN, newDN, false);
        }
        else
        {
            renameEntry(oldDN, newDN, true);
            for (int i = 0; i < lostRDNs.length; i++)
            {
                String RDN = lostRDNs[i];  // get Attribute Value Assertions from lostRDNs
                String type = RDN.substring(0, RDN.indexOf('='));
                String value = RDN.substring(RDN.indexOf('=') + 1);
                oldEntry.get(type).remove(value);   // remove old value so it doesn't get double deleted...
            }
        }
    }


    private void renameSingleValuedRDNS(DXEntry oldEntry, DXEntry newEntry)
            throws NamingException
    {
        RDN oldRDN = oldEntry.getRDN();

        String type = oldRDN.getAtt();
        String value = oldRDN.getRawVal();

        Attribute oldNamingAttInNewEntry = newEntry.get(type);
        // if the old naming value does not exist in the new entry, drop it!
        if (!oldNamingAttInNewEntry.contains(value))
        {
            renameEntry(oldEntry.getDN(), newEntry.getDN(), true);
            oldEntry.get(type).remove(value);   // remove old value so it doesn't get double deleted...
        }
        // if it *does* exist in the new entry, keep it.
        else
        {
            renameEntry(oldEntry.getDN(), newEntry.getDN(), false);
        }
    }


    /**
     * This creates and throws a naming exception when the user has attempted a fiendishly complex
     * and stoopid request that requires a mixture of 'deleteOldRDN=false' and
     * 'deleteOldRDN=true'.  Since JX does not handle indeterminate quantum
     * states, we throw an error instead.
     *
     * @param oldDN
     * @param newDN
     * @throws NamingException a detailed error - this is ALWAYS thrown when the method is called.
     */

    private void setWierdoRDNError(DN oldDN, DN newDN)
            throws NamingException
    {
        throw new NamingException(CBIntText.get("The rename operation is too complex to proceed.  Try to break it up into smaller stages.") +
                "\n    " + oldDN.toString() + "\n => " + newDN.toString());
    }



    /*
     *    See if the name has changed, and make required mods if it has.
     */

    //XXX - note case sensitive string compare to allow user to change capitalization...
    //TE: XXX can't change case in RDNs????  IF delete RDN = false changing case will fail b/c in the
    //TE: XXX dir the cn's are case insensitive so renaming from cn=A to cn=a is asking the dir to
    //TE: XXX have two cn's in the entry that are the same because the old cn will not be deleted i.e the
    //TE: XXX can't rename to an existing entry...
/*        if (oldEntry.getDN().equals(newEntry.getDN()) == false)
        {
            //Do the rename!

            moveTree(oldEntry.getDN(), newEntry.getDN());
        }
    }
*/

    /**
     * Update an entry with the designated DN.
     *
     * @param oldSet the old entry containing teh old set of attributes.
     * @param newSet the new entry containing the replacement set of attributes.
     * @throws NamingException if the operation fails.
     */

    public void updateEntry(DXEntry oldSet, DXEntry newSet)
            throws NamingException
    {

        if (DXAttributes.attributesEqual(oldSet, newSet))
            return; // nothing to do.

        DN nodeDN = newSet.getDN();
        RDN newRDN = nodeDN.getLowestRDN();

        DXAttributes adds = null;    // use modify-add for new attribute values (inc entirely new attribute)
        DXAttributes reps = null;    // use modify-replace for changed attribute values
        DXAttributes dels = null;    // use modify-delete for deleted attribute values (inc deleting entire attribute)

        reps = DXAttributes.getReplacementSet(newRDN, oldSet, newSet);
        dels = DXAttributes.getDeletionSet(newRDN, oldSet, newSet);
        adds = DXAttributes.getAdditionSet(newRDN, oldSet, newSet);

        if (false)
            printDebug(oldSet, newSet, adds, reps, dels);

        log.fine("updateNode: " + nodeDN);

        ModificationItem[] mods;

        mods = new ModificationItem[dels.size() + reps.size() + adds.size()];

        int modIndex = 0;
        modIndex = loadMods(mods, dels.getAll(), DirContext.REMOVE_ATTRIBUTE, modIndex);
        modIndex = loadMods(mods, adds.getAll(), DirContext.ADD_ATTRIBUTE, modIndex);
        modIndex = loadMods(mods, reps.getAll(), DirContext.REPLACE_ATTRIBUTE, modIndex);

        modifyAttributes(nodeDN, mods);          //TE: This may fail, returning false.
    }


    /**
     * Utility ftn for updateNode - takes a list of attributes to modify, and
     * the type of modification, and adds them to an array of modifications (starting
     * at a particular index).
     *
     * @param mods  the array of modification items
     * @param atts  an enumeration of attributes to add to the mod array
     * @param TYPE  the type of modification (DELETE,REPLACE,ADD)
     * @param index the position in the modification array to start filling entries in
     * @return return the final index position reached.
     */

    private int loadMods(ModificationItem[] mods, NamingEnumeration atts, int TYPE, int index)
            throws NamingException
    {
        while (atts.hasMore())
        {
            Attribute temp = (Attribute) atts.next();
            mods[index++] = new ModificationItem(TYPE, temp);
        }
        return index;
    }


    /**
     * Optional debug code.  Very useful. NEVER REMOVE!!!
     *
     * @param oldSet old entry.
     * @param newSet new entry.
     * @param adds   list of attributes to add.
     * @param reps   list of attributes to replace.
     * @param dels   list of attributes to delete.
     */

    private void printDebug(DXEntry oldSet, DXEntry newSet, DXAttributes adds, DXAttributes reps, DXAttributes dels)
    {


        System.out.println("\n*** entries are ***\n\nold:\n" + oldSet.toString() + "\n\nnew:\n" + newSet.toString());
        System.out.println("\n-----------------\nreps:\n" + reps.toString());
        System.out.println("\n-----------------\ndels:\n" + dels.toString());
        System.out.println("\n-----------------\nadds:\n" + adds.toString());
        //Thread.currentThread().dumpStack();
    }
}