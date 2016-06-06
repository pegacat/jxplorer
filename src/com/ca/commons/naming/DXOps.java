package com.ca.commons.naming;


import com.ca.commons.cbutil.*;
import com.ca.commons.jndi.AdvancedOps;
import com.ca.commons.jndi.ConnectionData;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
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


    public DXOps(LdapContext ctx)
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
            String msg = CBIntText.get("Search partially failed! - only {0} entries returned.", new Integer[]{new Integer(dxe.size())});
            if (ex instanceof SizeLimitExceededException)
            {
                msg = msg + "\n" +  CBIntText.get("(Consider using paged results; see 'Advanced Options -> Ldap Limits.)");
            }
            CBUtility.error(msg, ex);
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
     * Update a new entry with the designated DN with the values of the old entry.
     * If the old entry is null, this becomes an 'add'; if the new entry is null, this becomes a 'delete',
     * otherwise it effectively copies.
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
            throw new NamingException("Internal Error: Entry with null DN passed to JNDIDataBroker unthreadedModify.  Modify Request Cancelled.");
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

    public void handleAnyNameChange(DXEntry oldEntry, DXEntry newEntry)
            throws NamingException
    {
        // check for 'simple' rename from the tree, with no attributes involved.
        RDN oldRDN = oldEntry.getRDN();

        DN oldDN = oldEntry.getDN();
        DN newDN = newEntry.getDN();

        if (oldDN.equals(newDN))
            return;                     // nothing to see here, just move along.

        if (exists(newDN))
        {
            throw new NamingException(CBIntText.get("The name: ''{0}'' already exists - please choose a different name", new String[] {newDN.getLowestRDN() .toString()}));
        }
        else
        {
            boolean replaceRDN = doWeReplaceTheRDN(oldEntry, newEntry);

            moveTree(oldDN, newDN, replaceRDN);

            for (int i=0; i<oldRDN.size(); i++)
            {
                // clean up the naming attribute so that general entry modify doesn't try to delete the changed naming attribute from the entry...
                 String type = oldRDN.getAttID(i);
                 String value = oldRDN.getRawVal(i);

                 Attribute oldNamingAttInNewEntry = newEntry.get(type);
                 // if the old naming value does not exist in the new entry, remove it from the old entry, so it doesn't get 'deleted' when the diff is done!
                 if (oldNamingAttInNewEntry!=null && (oldNamingAttInNewEntry.contains(value)==false))
                     oldEntry.get(type).remove(value);   // remove old value so it doesn't get double deleted...
            }
        }
    }

    /**
    // The horror.  Renaming an entry with a single valued attribute *requires* a rename that doesn't maintain
    // the old value.
    //
    // Renaming an entry between single valued attributes *requires* that the rename does
    // maintain the old value.
    //
    //  <br>(Case 0: one or other RDN has multiple AV pairs.  Give up; assume user knows what they're doing - return false)
    //  <br>Case 1: naming attribute is the same - internal node rename - return false
    //  <br>Case 2: different RDN attributes - don't need to do anything - return false
    //  <br>Case 3: same RDN attribute - not single valued - return false
    //  <br>Case 4: same RDN attribute - is single valued - return true...
    //  <br>Case 5: we have entry attributes, and can check that the new entry doesn't have the old attribute value, so we remove it...

     * @param oldEntry an 'entry' (only used for the RDN; usually empty)
     * @param newEntry an 'entry' (only used for the RDN; usually empty)
     * @return
     */

    protected boolean doWeReplaceTheRDN(DXEntry oldEntry, DXEntry newEntry)
    {
        RDN oldRdn = oldEntry.getRDN();
        RDN newRdn = newEntry.getRDN();

        if (oldRdn.size()>1 || newRdn.size()>1)
            return false;               // if the RDNs are multi-valued, assume the user knows what they're doing (ha ha)

        if (oldRdn.equals(newRdn))
            return false;               // internal tree rename; leaf name is the same so don't replace anything


        if (oldRdn.getAttID().equals(newRdn.getAttID())) // RDNs have the same ID.. possible replacement situation
        {
            DXAttribute oldNamingAtt = new DXAttribute(oldRdn.getAttID());

            if (oldNamingAtt.isSingleValued())
                return true;  // we have a single valued naming attribute; we have to replace the value...

            if (newEntry.size()>0)
            {
                DXAttribute newNamingAtt =  newEntry.get(oldRdn.getAttID());
                if (newNamingAtt!=null && newNamingAtt.contains(oldRdn.getRawVal(0)))   // the new entry contains the old attribute value...
                    return false;                                   // so don't replace it!
                else
                    return true;                                    // but if it doesn't, we *do* need to replace it...
            }

        }

        return false; // different attributes; don't replace nuffin' ...
    }


    /**
     * Update an entry with the designated DN.
     *
     * @param oldSet the old entry containing the old set of attributes.
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

        DXAttributes reps = null;    // use modify-replace for changed attribute values
        DXAttributes dels = null;    // use modify-delete for deleted attribute values (inc deleting entire attribute)
        DXAttributes adds = null;    // use modify-add for new attribute values (inc entirely new attribute)

        reps = DXAttributes.getReplacementSet(newRDN, oldSet, newSet);
        dels = DXAttributes.getDeletionSet(newRDN, oldSet, newSet);
        adds = DXAttributes.getAdditionSet(newRDN, oldSet, newSet);

        if (false)
            printDebug(oldSet, newSet, adds, reps, dels);

        log.fine("updateNode: " + nodeDN);

        ModificationItem[] mods;

        mods = new ModificationItem[dels.size() + reps.size() + adds.size()];

        int modIndex = 0;
        modIndex = loadMods(mods, dels.getAll(), LdapContext.REMOVE_ATTRIBUTE, modIndex);
        modIndex = loadMods(mods, adds.getAll(), LdapContext.ADD_ATTRIBUTE, modIndex);
        modIndex = loadMods(mods, reps.getAll(), LdapContext.REPLACE_ATTRIBUTE, modIndex);

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