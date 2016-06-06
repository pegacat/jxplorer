
package com.ca.commons.naming;

import com.ca.commons.cbutil.*;
import com.ca.commons.jndi.SchemaOps;

import javax.naming.*;
import javax.naming.directory.*;

import java.text.Collator;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *    This class is a container for a collection of
 *    Attribute objects.  It is schema aware, and will
 *    search schema to find a complete set of attributes
 *    for its component objectClass(s).<p>
 *
 *    The class is built around a hashtable of id/attribute
 *    pairs.
 *
 *
 * nb this class must work with both oid's (2.3.1.2.3.1.2.41.23.1.pi.2.e.34.phi)
 * and ldap names ('c').  attribute names with multiple string representations
 * such as 'cn' / 'commonName' are handled by accepting any form on input, and
 * returning an arbitrarily chosen standard (e.g. *just* 'cn') on return, (in order
 * to cope with rare errors interfacing with some servers). 
 */

// XXX a lot of effort is gone to to trim out ';binary' from the names of
// XXX ldap attributes.  THis is a grubby hack, and handling should be improved
// XXX somehow.

public class DXAttributes implements Attributes, Iterable<DXAttribute>
{

    static Hashtable attributeNames = new Hashtable(100);  // a global hashset of all attribute oid-s and corresponding ldap descr names known to the program...

    static int ID = 0;

    int id;                             // unique id for debugging.

    HashMap<String, DXAttribute> atts;  // a hashmap of attribute id/ attribute object values, keyed by lowercase id.

    HashSet must;                       // a hashset of attribute ldap ids that *must* be entered - set in expandAllAttributes()

    boolean ignoreCase = true;          // whether attribute IDs are case sensitive

    boolean schemaChecked = false;      // indicates a schema search has been made, and a full list of objectclass(s) attribute obtained

    DXAttribute allObjectClasses;         // a list of allObjectClasses, including parents.

    Vector orderedSOCs = new Vector();  // structural object classes, in deepest-first order

    static SchemaOps schema;            // the schema context

    static Hashtable knownParents = new Hashtable(30);      // hash of DXAttributes containing the known object class parents for a particular object class subset (e.g. 'inetorgperson' -> 'orgperson','person','top')
    static Hashtable knownSubSets = new Hashtable(30);      // hash of known object class subsets
    static Hashtable objectClassDepths = new Hashtable(30); // hash of known object class 'depths' in object class inheritance tree

    public static final String STRUCTURAL_NODE = "structuralTreeNode";  // a synthetic node used only for tree display

    private final static Logger log = Logger.getLogger(DXAttributes.class.getName());

    static
    {
        objectClassDepths.put("top", new Integer(0));

        // pre set 'synthetic' schema attributes
        objectClassDepths.put("schema", new Integer(1));
        objectClassDepths.put("AttributeDefinition", new Integer(2));
        objectClassDepths.put("ClassDefinition", new Integer(2));
        objectClassDepths.put("SyntaxDefinition", new Integer(2));

    }

    // common code run by all the more basic constructors.
    void basicInit()
    {
        id = ID++;

        atts = new HashMap<String, DXAttribute>();
        must = new HashSet();
    }

    /**
     *    Initialises an empty set of attributes with no schema
     */
    public DXAttributes()
    {
        basicInit();
    }

    /**
     *    Initialises a set of attributes with a single attribute
     */

    public DXAttributes(Attribute a)
    {
        basicInit();
        put(a);
    }

    /**
     *    Copies an existing Attributes object into a DXAttributes
     *    object.
     */

    public DXAttributes(Attributes a)
    {

        if (a==null)
        {
            atts = new HashMap<String, DXAttribute>();
            must = new HashSet();
        }
        else
        {
            atts = new HashMap<String, DXAttribute>(a.size()+10);
            must = new HashSet(a.size());   // overkill, but what the heck...

            Enumeration e = a.getAll();
            while (e.hasMoreElements())
            {
                DXAttribute newAtt = new DXAttribute((Attribute)e.nextElement());
                put (newAtt);
            }
        }
    }

    /**
     *    Initialises a set of DXattributes using
     *    a Hashtable of existing id/attribute pairs...
     *    @param newAtts hashtable of id/attribute pairs to
     *           initialise the new DXAttributes object
     */

    public DXAttributes(HashMap<String, DXAttribute> newAtts)
    {
        atts = (HashMap<String, DXAttribute>) newAtts.clone();
    }


    /**
     *    Initialises a set of DXattributes using
     *    a NamingEnumeration of existing attribute(s).
     *    @param newAtts namingenumeration of attributes to
     *           initialise the new DXAttributes object
     */

    public DXAttributes(NamingEnumeration newAtts)
    {
        atts = new HashMap<String, DXAttribute>();
        while (newAtts.hasMoreElements())
        {
            Attribute current = (Attribute) newAtts.nextElement();
            if (current instanceof DXAttribute)
                atts.put(current.getID().toLowerCase(), (DXAttribute)current);
            else
                atts.put(current.getID().toLowerCase(), new DXAttribute(current));
        }
    }

     @Override
    public Iterator<DXAttribute> iterator() {
        return atts.values().iterator();
     }


    /**
     *    <p>This sets the standard schema to use while this connection is open.
     *    (It may be possible in future releases to set schema on a per-Attribute
     *    basis - it is not clear yet whether this would be useful.)</p>
     *
     *    <p>Note: currently this also sets the default schema for the DXAttribute
     *    Class.</p>
     */
    public static void setDefaultSchema(SchemaOps defaultSchema)
    {
        schema = defaultSchema;

        DXAttribute.setDefaultSchema(defaultSchema);
    }


    public int getID() { return id; }

    /**
     *    creates a new DXAttributes, copying each Attribute object.
     *    @return a new DXAttributes object
     */

    public Object clone()
    {
        return new DXAttributes(atts);
    }

    /**
     * Convenience wrapper for fast checking and sorting of att IDs.
     * @param attrID
     * @return
     */
    public boolean contains(String attrID)
    {
        return atts.containsKey(attrID);
    }

    /**
     *    gets an Attribute specified by its ID
     *    @param attrID the ID of the attribute to retrieve
     *    @return the specified attribute (actually a DXAttribute)
     *            - null if it can't be found.
     */

    public DXAttribute get(java.lang.String attrID)
    {
        DXAttribute ret = atts.get(attrID.toLowerCase());
        if (ret==null)
        {
            ret = atts.get(attrID.toLowerCase() + ";binary");
        }

        return ret;
    }

    /**
     *    returns all the Attribute(s) in this DXAttributes object.
     *    - the NamingEnumeration is (evily) pre-sorted alphabetically...
     *    @return enumeration of all stored Attribute objects
     */
    public NamingEnumeration getAll()
    {
        return (new DXNamingEnumeration (atts.values())).sort();
    }



    /**
     *     convenience method that returns the attributes as a collection...
     */

    public ArrayList<DXAttribute> getAttArrayList()
    {
        ArrayList<DXAttribute> vals = new ArrayList<DXAttribute> (atts.values());
        Collections.sort(vals);
        return vals;
    }

    /**
     *    For convenience and display, DXAttributes objects have a complete
     *    list of all attribute(s) that the objectclass(s) represented might
     *    possibly contain.  However sometimes we need an <b>Attributes</b>
     *    object that has no null-valued attributes (i.e. when adding it to
     *    the directory).
     *    @return an Attributes object containing no null valued attribute(s).
     */
    public Attributes getAsNonNullAttributes()
    {
        return new DXAttributes(getAllNonNull());
    }

    /**
     *    returns all the Attribute(s) that have non-null values
     *    in this DXAttributes object.
     *    - the NamingEnumeration is (evily) pre-sorted alphabetically...<p>
     *
     *    Warning: whether an attribute is added is undefined if the
     *    attribute is multi-valued and contains one or more null values in
     *    addition to other non-null values.
     *
     *    @return enumeration of all stored Attribute objects with non-null values
     */

    public NamingEnumeration getAllNonNull()
    {
        DXNamingEnumeration returnEnumeration = new DXNamingEnumeration ();
        Enumeration allatts = getAll();

        while (allatts.hasMoreElements())
        {
            Attribute fnord = (Attribute) allatts.nextElement();
            if (fnord != null) // should never happen...
            {
                try
                {
                    if (fnord.get() != null)            // if there is at least one non-null value...
                        returnEnumeration.add(fnord);   // add it to the list
                }
                catch (NoSuchElementException e)        // 'expected' exception (love that jndi)
                {

                }
                catch (NamingException e2)
                {
                    log.log(Level.WARNING, "whoops: Naming Exception reading " + fnord.getID(), e2);
                }
            }
        }

        returnEnumeration.sort();

        return returnEnumeration;
    }

    /**
     *    returns all the mandatory 'MUST' have Attribute(s) in this DXAttributes object.
     *    - the NamingEnumeration is (evily) pre-sorted alphabetically...
     *    @return enumeration of all stored Attribute objects
     */
    public NamingEnumeration getMandatory()
    {
        DXNamingEnumeration returnEnumeration = new DXNamingEnumeration ();

        if (must==null) return returnEnumeration;  // return empty enumeration if not initialised...

        Iterator musts = must.iterator();
        while (musts.hasNext())
        {
            String s = (String)musts.next();
            returnEnumeration.add(get(s));
        }
        returnEnumeration.sort();

        return returnEnumeration;
    }



   /**
    *   Returns the list of mandatory attribute IDs (HashSet:must).
    *   @return the list of mandatory attribute IDs (e.g. objectClass, cn, sn).
    */

    public HashSet getMandatoryIDs()
    {
        return must;
    }



    /**
     *    returns all the optional 'MAY' Attribute(s) in this DXAttributes object.
     *    - the NamingEnumeration is (evily) pre-sorted alphabetically...
     *    @return enumeration of all stored Attribute objects
     */
    public NamingEnumeration getOptional()
    {
        DXNamingEnumeration returnEnumeration = new DXNamingEnumeration ();
        Collection<DXAttribute> allAtts = atts.values();
        for (Attribute att: allAtts)
        {
            String id = (String) att.getID();
            if (must.contains(id)==false)    // if it's *not* mandatory
                returnEnumeration.add(get(id)); // add it to the optional list
        }
        returnEnumeration.sort();

        return returnEnumeration;
    }



    /**
     *    returns all the attribute IDs held in this DXAttributes object.
     *    @return all attribute IDs
     */

    public NamingEnumeration getIDs()
    {
        // cannot simply return hash keys, as they are standardised to lower case.
        DXNamingEnumeration ret = new DXNamingEnumeration();
        NamingEnumeration allAtts = getAll();
        while (allAtts.hasMoreElements())
            ret.add(((Attribute)allAtts.nextElement()).getID());

        return ret;

        //return (NamingEnumeration)(new DXNamingEnumeration (atts.keys()));
    }

    /**
     *    returns whether attribute IDs are stored in a case
     *    sensitive manner; i.e. whether 'person' is different
     *    from 'Person'.  The default is <i>false</i>, implying
     *    case sensitivity.
     *    @return whether case is ignored for attribute IDs
     */

    public boolean isCaseIgnored()
    {
        return ignoreCase;
    }

    /**
     *    adds an attribute to the DXAttributes collection,
     *    using the attribute.getID() method to find a key.
     *    NB: doco seems unclear on whether this adds to,
     *    or replaces, any existing attribute with the same
     *    ID.  At the moment this <b>replaces</b> the values...
     *    @param attr the attribute to add
     *    @return the previous attribute (if any) with this key.
     */

    public Attribute put(Attribute attr)
    {
        if (attr == null) return null; // sanity check - can't add a null attribute...

        Attribute old = get(attr.getID().toLowerCase());
        schemaChecked = false;

        if (old!=null)
        {
            atts.remove(old.getID().toLowerCase()); // code for *replacing* existing attribute values
        }

        String ID = attr.getID().toLowerCase();
        if (attr instanceof DXAttribute)
            atts.put(ID, (DXAttribute)attr);
        else
            atts.put(ID, new DXAttribute(attr));

        return old;
    }

    /**
     *    creates an attribute with the specified values and adds it
     *    to the DXAttributes collection,
     *    using the attrID string as a key.
     *    NB: doco seems unclear on whether this adds to, or replaces,
     *    an existing attribute with the same ID.  This implementation
     *    <b>adds</b> the new object value...
     *
     *    @param attrID the String ID of the newly added attribute
     *    @param val the value to associate with the ID for the newly
     *               created attribute
     *    @return the previous attribute (if any) with this key.
     */

    public Attribute put(java.lang.String attrID, java.lang.Object val)
    {
        schemaChecked = false;
        return put(new DXAttribute(attrID.toLowerCase(), val));
    }

    /**
     *    Adds an Enumeration of Attribute(s) to a DXAttribute
     *
     *    @param attributeList the list of attributes to add.
     */

    public void put(Enumeration attributeList)
    {
        while (attributeList.hasMoreElements())
        {
            Attribute a = (Attribute)attributeList.nextElement();
            if (a instanceof DXAttribute)
                put(a);
            else
                put(new DXAttribute(a));
        }
    }

    /**
     *    removes the attribute containing this key (if any).
     *    @param attrID the ID of the attribute to remove
     *    @return the removed attribute (if any)
     */

    public Attribute remove(java.lang.String attrID)
    {
        schemaChecked = false;
        return (Attribute) atts.remove(attrID.toLowerCase());
    }

    /**
     *    returns the number of Attribute objects held.
     *
     *    @return number of attribute objects
     */
    public int size()
    {
        return atts.size();
    }




    /**
     *    Return a list of object classes as a vector from deepest (at pos 0) to 'top' (at pos (size()-1) ).
     */
    public ArrayList<String> getOrderedOCs()
    {
        try
        {
            Attribute oc = getAllObjectClasses();
            if (oc == null)
                return null;

            ArrayList<String>ret = new ArrayList<String>(oc.size());
            NamingEnumeration vals = oc.getAll();
            while (vals.hasMore())
            {
                ret.add(vals.next().toString());
            }
            return ret;
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "Yet another rare naming exception - DXAttributes:getOrderedOCs ", e);
            return new ArrayList<String>();
        }
    }

    /**
     * Try to get the current object class; checks for an attribute called 'objectclass', 'objectClass' and 'oc'.
     * @return
     */

    public DXAttribute getAllObjectClasses()
    {
        if (allObjectClasses != null) // cache for reuse
            return allObjectClasses;

		DXAttribute att = get("objectclass");
        if (att == null)
            att = get("objectClass");  // try both forms for good luck.
        if (att == null)
            att = get("oc");  // backward compatibility for eTrust directory which uses 'oc' as a synonym.

        allObjectClasses = att;

	    return getAllObjectClasses(att); // returns null if att is null
    }

    /**
     *    Some directories don't include all of an entry's object classes,
     *    but only the lowest level ones.  This looks up all the parents
     *    the low level object classes are inherited from and, forms a
     *    complete ordered list of all object classes for this Attributes object.
     *
     *    @return an Attribute containing the names of all the object classes...
     */

    // XXX objecClass should no longer be case sensitive.  When happy this works,
    // XXX delete code below.

    public static DXAttribute getAllObjectClasses(DXAttribute oc)
    {
        if (oc==null) return null; // no object classes (may be virtual entry such as DSA prefix)

        if (knownSubSets.containsKey(oc))
            return(DXAttribute) knownSubSets.get(oc);

        try
        {
            DXAttribute orig = new DXAttribute(oc);

            Enumeration vals = oc.getAll();
            while (vals.hasMoreElements())
            {
                Attribute parents = getParentObjectClasses((String)vals.nextElement());
                if (parents != null)
                {
                    Enumeration parentVals = parents.getAll();
                    while (parentVals.hasMoreElements())
                    {
                        String parent = (String) parentVals.nextElement();
                        if (oc.contains(parent) == false)
                        {
                            oc.add(parent);
                        }
                    }
                }
            }

            DXAttribute fullOC = sortOCByDepth(oc);
            knownSubSets.put(orig, fullOC);
            return fullOC;
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "NamingException in getAllObjectClasses ", e);
            return oc;
        }
    }

    /**
     *    Takes a list of *all* object class values, and returns them
     *    sorted by depth.  This requires the objectClassDepths hashtable
     *    to be set (which is done by getParentObjectClasses() ).
     */

    protected static DXAttribute sortOCByDepth(Attribute oc)
    {
        DXAttribute ret = new DXAttribute("objectClass");
        ret.setOrdered(true);

        try
        {
            Enumeration vals = oc.getAll();
            while (vals.hasMoreElements())
            {
                String val = (String) vals.nextElement();
                Integer myInt = (Integer)objectClassDepths.get(val);

                if (myInt == null) // shouldn't happen (if schema is correct), but in case we missed one...
                {
                	getParentObjectClasses(val);  // try again to set the objectClassDepths hash for this value. (probably won't work).
                	myInt = (Integer)objectClassDepths.get(val); // and try to reget the value.
                	if (myInt == null) 			  // if still null, give up and set to zero.
                		myInt = new Integer(0);
                }
                int depth = myInt.intValue();
                int i;
                for (i=ret.size()-1; i>=0; i--)
                {
                    int existing = ( (Integer)objectClassDepths.get(ret.get(i)) ).intValue();
                    if (  existing >= depth)
                    {
                        ret.add(i+1, val);
                        break;
                    }
                }
                if (i == -1)
                    ret.add(0, val);
            }
            return ret;
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "Naming Exception in DXAttributes sortOCByDepth()", e);
            return new DXAttribute(oc);
        }
    }



    /**
     *    recursively builds up a complete ordered list of all the parents of a particular
     *    object class (including the child object class) from schema.
     *
     *    @param childOC the child Object Class to search for parents of
     *    @return an attribute containing the child class and all parents
     */

    public static DXAttribute getParentObjectClasses(String childOC)
        throws NamingException
    {
        if (schema == null) // in the absence of a schema, everything is at level '1', just below 'top'
        {
            objectClassDepths.put(childOC, new Integer(1));
            return null;
        }

        if ("schema attributedefinition classdefinition syntaxdefinition matchingrule".indexOf(childOC.toLowerCase()) != -1) return null;  // don't bother looking up synthetic object classes.

        if (knownParents.containsKey(childOC))
        {
            return (DXAttribute) knownParents.get(childOC);
        }

        DXAttribute parents = new DXAttribute("objectClass");  // this is the attribute returned

        String schemaParent = null;
        try
        {
            //schemaParents = schema.getAttributes("ClassDefinition/" + childOC, new String[] {"SUP"});
            Attributes schemaDef = schema.getAttributes("ClassDefinition/" + childOC);
            if (schemaDef!=null)
            {
                Attribute sup = schemaDef.get("SUP");
                if (sup!=null)
                    schemaParent = (String)sup.get();
            }
        }
        catch (NamingException e) // easily throws a name-not-found exception
        {
			log.warning("Possible Schema Error: class definition for " + childOC + " could not be found");
			objectClassDepths.put(childOC, new Integer(1));  // BAD SCHEMA! NO BISCUIT!  Set to one 'cause we don't know true depth (and top is always zero).

            return null;  // do nothing
        }

        // TODO: this is silly; why don't we just reuse the DXAttribute object returned?
        // XXX - no time to fix now; maybe later...

        if (schemaParent != null) // may not: e.g. 'top' has no parent
        {
            DXAttribute oc = getParentObjectClasses(schemaParent);                // recurse -> this should also set the depth in objectClassDepths

            if (oc != null)
            {
                Enumeration vals = oc.getAll();
                while (vals.hasMoreElements())                 // load up the return attribute
                {
                    parents.add(vals.nextElement());           // (o.k. to add the same value twice...)
                }
            }

            int depth = ((Integer)objectClassDepths.get(schemaParent)).intValue();
            if (objectClassDepths.containsKey(childOC) == false)
            {
                objectClassDepths.put(childOC, new Integer(depth+1));
            }
            else
            {
                int oldDepth = ((Integer)objectClassDepths.get(childOC)).intValue();
                if (oldDepth <= depth)
                    objectClassDepths.put(childOC, new Integer(depth+1));
            }
        }
        else  // no schemaParents
        {
			objectClassDepths.put(childOC, new Integer(1));  // BAD SCHEMA! NO BISCUIT!  Set to one 'cause we don't know true depth (and top is always zero).
            //schemaParents = null; // so store a blank place holder
        }

        parents.add(childOC);     // *** Note Cunning Recursive Magic - this is where base stuff actually gets added to parents... ***

        knownParents.put(childOC, parents);

        return parents;
    }

    /**
     *    Sets the vector of structural object classes
     *    in this attribute set.
     *    @param oc the object class to find (and add) the structural parents of.
     */

    void setOrderedSOCs(String oc)
        throws NamingException
    {
        orderedSOCs.add(oc);

        if (oc.equalsIgnoreCase("top")) return;  // recursive search finished.


        String parent = schema.schemaLookup("ClassDefinition/" + oc, "SUP");
        String struct = schema.schemaLookup("ClassDefinition/" + parent, "STRUCTURAL");
        // try to figure out if that was a structural object class...
        if ("true".equalsIgnoreCase(struct) )
        {
            setOrderedSOCs(parent);  // recurse to next set of parents.
            return;              // finished.
        }

    }

    /**
     *    This method trims all empty attributes (attributes without values) from
     *    the DXAttributes object.
     */

    public void removeEmptyAttributes()
    {
        Enumeration atts = getAll();
        while (atts.hasMoreElements())
        {
            Attribute att = (Attribute)atts.nextElement();
            if (att.size() == 0)
                remove(att.getID());
        }
    }




    /**
     *    <p>Sets the internal list of all attribute IDs needed for
     *    a given set of object classes, as well as noting which
     *    are mandatory and which optional.</p>
     *
     *    <p>As an added wrinkle, this must be able to cope with attribute
     *    ID's expressed either as ldap strings, or as numeric OIDs.  It
     *    does this by automatically detecting OIDs, and translating them
     *    to strings using schema lookups.</p>
     *
     *    <p>This method uses the schema to create empty valued attributes for
     *    attributes which don't currently exist, but which are allowed.</p>
     *
     */

    public void expandAllAttributes()
    {
        if (schema == null) return;

        if (get(STRUCTURAL_NODE)!=null)
            return;

        Attribute oc = getAllObjectClasses();

        try
        {
        // Quick Hack to eliminate 'fake attributes' used for top level of syntaxes...
        //XXX Might want to redo if efficiency is ever a concern :-)

            if (oc.contains(SchemaOps.SCHEMA_FAKE_OBJECT_CLASS_NAME) )
                return;  // ignore the synthetic 'schema' object classes...

            NamingEnumeration ocs = oc.getAll();

            // cycle through all object classes, finding attributes for each
            while (ocs.hasMore())
            {
                String objectClass = (String)ocs.next();
                Attributes ocAttrs = schema.getAttributes("ClassDefinition/" + objectClass);
//FIXSCHEMA                
                Attribute mustAtt = ocAttrs.get("MUST");  // get mandatory attribute IDs
                Attribute mayAtt  = ocAttrs.get("MAY");   // get optional attribute IDs

                if (mustAtt != null)
                {
                    NamingEnumeration musts = mustAtt.getAll();
                    while (musts.hasMore())
                    {
                        String attributeName = (String) musts.next();

                        //XXX ;binary hack.  Note attributeName is usually a string 'cn' but is sometimes an OID 1.4.0.88.whatever
                        if (attributeName.indexOf(";binary")>0) attributeName = attributeName.substring(0,attributeName.indexOf(";binary"));

                        String ldapName = getldapName(attributeName);

                        //registerOID(attributeName, ldapName);
                        if (get(ldapName)==null)                                  // if we don't already have this attribute...
                        {
                            put(new DXAttribute(ldapName));   // ... add it to the list
                            //CB empty atts now. put(new DXAttribute(getldapName(attributeName), null));   // ... add it to the list
                        }

                        if (must.contains(ldapName.toLowerCase())==false)            // and if it isn't already mandatory
                        {
                            must.add(ldapName.toLowerCase());                        // ... add it to the mandatory list as well
                        }
                    }
                }

                if (mayAtt != null)
                {
                    NamingEnumeration mays = mayAtt.getAll();
                    while (mays.hasMore())
                    {
                        String attOID = (String) mays.next();
                        //XXX isNonString hack
                        if (attOID.indexOf(";binary")>0) attOID = attOID.substring(0,attOID.indexOf(";binary"));

                        String ldapName = getldapName(attOID);
                        //registerOID(attOID, ldapName);

                        if (get(ldapName)==null)   // if we don't already have this one...
                        {
                            put(new DXAttribute(ldapName));   // ... add it to the list
                            //put(new DXAttribute(getldapName(attOID), null));   // ... add it to the list
                        }
                    }
                }
            }
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "unable to read attribute list for object classes: ", e);
            try
            {
                CBUtility.printEnumeration(oc.getAll());
            }
            catch (NamingException e2)
            {
                log.warning("...(further error: can't print object class list)...");
            }
            return;
        }
        catch (NullPointerException e)
        {
            log.log(Level.WARNING, "ERROR: unable to read list of object classes from schema - some functionality will not be available", e);
        }
    }

    /**
     * <p>If a "must" name has an alias, like "o" and "organization",
     * or "uid" and "userid", and schemas like "account" and
     * "posixAccount" use different aliases in in different Objects
     * and an entry has multiple ObjectTypes  we dont want to add
     * "uid" and "userid" as "must", as this will causes problems.
     * So we will check for the aliases, and use the first one.
     * If we can't find it, will just return the same name. DEE</p>
     *
     * For example, both ou and orgUnit might be 2.5.4.11 -> this makes
     * sure that regardless of what the schema refers to, whichever is
     * refered to 'first' will be used consistently.
     */

    public String getConsistentAttributeName(String attributeName)
    {
        if (attributeNames.containsKey(attributeName))
            return (String) attributeNames.get(attributeName);

        try
        {
            Attributes myldapEntry = schema.getAttributes("AttributeDefinition/" + attributeName);
            String testOID = myldapEntry.get("OID").get().toString();
            String newName = myldapEntry.get("NAME").get().toString();
            attributeNames.put(attributeName, newName);  // add an arbitrary 'standard' name to attributeNames hash.
            //System.out.println("DEE **** Adding original: " +  attributeName +  " ( oid = " + testOID + ")  =>  " + newName);
            return newName;
        }
        catch (Exception e)
        {
            log.info("getConsistentAttributeName unable to map alias for:" + attributeName +  e);
        }
        return attributeName;
    }
    /**
     *    This method does it's darndnest to return a consistent string ldap name.<p>
     *    First, it checks whether the string is <i>already</i> an ldap
     *    name; if it is, it figures out whether it is the (arbitrary) 'consistent' ldap name,
     *    and returns that.
     *
     *    Secondly, it tries to find the ldap text name for an oid
     *    (i.e. converts 2.5.4.0 to 'objectClass').<p>
     *
     *    Finally, if it <b>can't</b> find the name it returns the oid instead...
     *    (This shouldn't really happen, but means that the system may still work,
     *    although the raw OIDs aren't very user friendly)<p>
     *  @param attOID - an ldap attribute name or an ldap attribute OID
     * @return the 'canonical' standard ldap attribute name, if possible.
     */

    public String getldapName(String attOID)
    {
        if (schema == null)  // if we don't have a functioning 'schema' link, give up.
            return attOID;

        if (schema.knownOID(attOID)) // if this works, we have an old fashioned numeric OID to translate
            return schema.translateOID(attOID);

        return getConsistentAttributeName(attOID); // if we're here, make sure we return a 'consistent' attribute name
    }

    public String toString()
    {
        StringBuffer text = new StringBuffer("size (" + size() + ")\n");

        NamingEnumeration allatts = this.getAll();
        while (allatts.hasMoreElements())
        {
            Attribute fnord = (Attribute) allatts.nextElement();
            if (fnord == null)
                log.warning("bizarre null attribute in element list");
            else
            {
                if (must != null && must.contains(fnord.getID()))
                    text.append("must ");

                if (fnord instanceof DXAttribute)
                    text.append(" dx ").append(((DXAttribute)fnord).toDebugString()).append(" ");
                else
                {
                    String ID = fnord.getID();
                    text.append("\n    " + ID + " (not DXAttribute)" );
                    try
                    {
                        if (fnord.size() == 0)
                            text.append("        " + " (empty) ");
                        else
                        {
                            Enumeration vals = fnord.getAll();

                            while (vals.hasMoreElements())
                            {
                                Object val = vals.nextElement();
                                String fnordel = (val==null)?"*null*":val.toString();
                                text.append("        '" + fnordel + "'");
                            }
                        }
                    }
                    catch (NamingException e)
                    {
                        log.log(Level.WARNING, "whoops: Naming Exception reading " + ID, e);
                    }
                }
            }
        }
        return text.toString();
    }

    public void print() { print(null); }

    public void print(String msg)
    {
        if (msg!=null) System.out.println(msg);
        printAttributes(this);
    }

    public static void printAttributes(Attributes a)
    {
        if (a==null) System.out.println("null attributes set");
        NamingEnumeration allatts = a.getAll();

        printAttributeList(allatts);
    }

    public static void printAttributeList(NamingEnumeration en)
    {
        while (en.hasMoreElements())
        {
            Attribute fnord = (Attribute) en.nextElement();
            if (fnord == null)
                log.warning("bizarre null attribute in element list");
            else
            {
                String ID = fnord.getID();
                System.out.println("    " + ID);
                try
                {
                    Enumeration vals = fnord.getAll();

                    while (vals.hasMoreElements())
                    {
                        Object val = vals.nextElement();
                        String fnordel = (val==null)?"*null*":val.toString();
                        System.out.println("        " + fnordel);
                    }
                }
                catch (NamingException e)
                {
                    log.log(Level.WARNING, "whoops: Naming Exception reading " + ID, e);
                }
            }
        }
    }



    /**
     *    <p>Returns the set of attributes that are in the newSet, but
     *    not in the old set, excluding the rdnclass).</p>
     *
     *    <p>It also returns partial attributes that contain values that
     *    are in the newSet but not in the oldSet, when either a)
     *    either attribute has a size larger than one, or b) the attribute has
     *    a distinguished value.</p>
     *
     *    <p>The logic for case a) is that we can use adds and deletes on
     *    individual attributes to simulate a 'replace' operation, but we
     *    need to avoid doing this for single valued attributes (for which
     *    we *always* use replace).  So if Attribute A has values {1,2,3},
     *    and is changed to A' {1,3,5,6}, this method returns an 'add' {5,6}.</p>
     *
     *    <p>The logic for case b) is that we cannot use 'replace' on an
     *    attribute with a naming value in it, so we *must* use adds and
     *    deletes - and we'll have to take our chances that it is not
     *    single valued. (Possibly later on we can check this from schema).
     *    This method cannot change the distinguished value, but produces an
     *    'add' for any other changed.  So if, for entry cn=fred, Attribute cn
     *    has values {fred,erik}
     *    and cn' has values {fred, nigel], this method produces an 'add' {nigel}.</p>
     *
     *
     *    @param newRDN the new RDN of the entry that is being created
     *           (usually this is the same as the RDN of the original entry)
     *           May be null if it is not to be checked.
     *    @param oldSet the set of already existing attributes
     *    @param newSet the set of new attributes to test
     *    @return attributes that must be added to the object.
     */

    public static DXAttributes getAdditionSet(RDN newRDN, Attributes oldSet, Attributes newSet)
		throws NamingException
    {
        DXAttributes additionSet = new DXAttributes();
        NamingEnumeration listOfNewAttributes = newSet.getAll();

        while (listOfNewAttributes.hasMore())
        {
            Attribute newAtt = (Attribute)listOfNewAttributes.next();

            String attributeName = newAtt.getID();

            boolean isNamingAttribute = (newRDN != null) && newRDN.contains(attributeName);

            Attribute oldAtt = oldSet.get(attributeName);

            if (! emptyAtt(newAtt))            // don't add empty atts!
            {
                /*
                 *    Check for simple "whole attribute" adds
				 *    if the old Att is null, or only had null values add it.
				 *    (It was probably created by the browser,
				 *     and doesn't exist in the database)
                 */

                if ((isNamingAttribute == false) && (oldAtt == null || emptyAtt(oldAtt)))
				{
                    additionSet.put(newAtt);
				}

                /*
                 *   Check for attribute values that have been changed in attributes
                 *   that are larger than 1, or that are naming attributes
                 */
// TODO: - clean this up for DSML etc. ...

                else if (isNamingAttribute || (oldAtt.size() > 1 || newAtt.size() > 1 ))
                {
                    DXNamingEnumeration valuesToAdd = getMissingValues(oldAtt.getAll(), newAtt.getAll());

                    // check for distinguished value, and ignore it...
                    if (isNamingAttribute)
                    	removeAnyDistinguishedValues(newRDN, attributeName, valuesToAdd);

                    if (valuesToAdd.size()>0)
                        additionSet.put(new DXAttribute(attributeName, valuesToAdd));
                }
            }
        }
        return additionSet;
    }



    /**
     *    <p>Returns all single valued attributes whose values have changed -
     *    that is, exist in both the new set and the old set, but have different values.
     *    Note that this function ignores the naming attribute.</p>
     *
	 *    <p>We need this function to cope with mandatory single valued attributes
	 *       (otherwise we could just use add and delete).</p>
	 *
     *    <p>All other attribute combinations are handled by attribute value adds and
     *    deletes. (This is slightly more efficient, and is required to modify
     *    non-distinguished values of the naming attribute anyway).</p>
     *
     *    @param newRDN the RDN of the newer entry.
     *           (usually this is the same as the RDN of the original entry)
     *           May be null if it is not to be checked.
     *    @param oldSet the set of already existing attributes
     *    @param newSet the set of new attributes to test

     *    @return attributes that require updating
     */

	// LOGIC NOTE - only replace attributes that are single valued (old & new) and NOT naming values.

    public static DXAttributes getReplacementSet(RDN newRDN, Attributes oldSet, Attributes newSet)
        throws NamingException
    {
        DXAttributes replacementSet = new DXAttributes();

        NamingEnumeration listOfNewAttributes = newSet.getAll();

        while (listOfNewAttributes.hasMore())                                // find changed attributes
        {
            Attribute newAtt = (Attribute)listOfNewAttributes.next();

			if (newAtt != null && newAtt.size() == 1)  // only consider a single valued new attribute
			{
	            String attributeName = newAtt.getID();

	            if (newRDN == null || newRDN.contains(attributeName) == false) // skip any naming attributes
				{
		            Attribute oldAtt = oldSet.get(attributeName);

		            if (oldAtt != null && oldAtt.size() == 1)	// only look at changed single valued attributes.
		            {
		                // if  a single valued attribute has changed, make it a 'replace' op.
		                if (attributesEqual(newAtt, oldAtt)==false)
		                    replacementSet.put(newAtt);
		            }
	            }
			}
        }
        return replacementSet;
    }



    /**
     *    <p>Returns the set of attributes that are in the oldSet, but
     *    not in the new set, and thus must be deleted. </p>
     *
     *    <p>It also returns the set of attribute *values* that are in
     *    the old set, but not in the new set.  E.g. if attribute A
     *    has values {1,2,3,4}, and the new attribute A' has {1,4,6,7},
     *    this returns {2,3} for deletion.</p>
     *
	 *    <p>This method will ignore naming values, but will correctly
	 *    handle other values of the naming attribute.</p>
	 *
	 *    @param newRDN the RDN of the newer entry.
     *           (usually this is the same as the RDN of the original entry).
     *           May be null if it is not to be checked.
     *    @param oldSet the set of already existing attributes
     *    @param newSet the set of new attributes to test
     */

    public static DXAttributes getDeletionSet(RDN newRDN, Attributes oldSet, Attributes newSet)
		throws NamingException
    {
        DXAttributes deletionSet = new DXAttributes();

        NamingEnumeration listOfOldAttributes = oldSet.getAll();

        while (listOfOldAttributes.hasMore())
        {
            Attribute oldAtt = (Attribute)listOfOldAttributes.next();

            if (! emptyAtt(oldAtt))            // don't delete empty atts!
            {
	            String attributeName = oldAtt.getID();

                int pos = attributeName.indexOf(";binary");
                if (pos > -1)
                    attributeName = attributeName.substring(0, pos);
                
	            boolean isNamingAttribute = (newRDN != null) && newRDN.contains(attributeName);

	            Attribute newAtt = newSet.get(attributeName);

                if (newAtt == null)
               		newAtt = new DXAttribute(attributeName);

                /*
                 *    Check for simple "whole attribute" deletes
                 */

                if (emptyAtt(newAtt) && !isNamingAttribute)
                {
                    deletionSet.put(newAtt);
                }

                /*
                 *   Check for attribute values that have been dropped, in attributes
                 *   that are larger than 1
                 */

                else if (isNamingAttribute || oldAtt.size() > 1 || newAtt.size() > 1 )
                {
                    //bizarre special case; don't delete 'top' objectclass attribute ever... (some entries miss it, but trying to delete it makes directories cry)
                    if (attributeName.equalsIgnoreCase("objectclass"))
                        if (newAtt.contains("top")==false && oldAtt.contains("top")==true)
                            newAtt.add("top");


                    DXNamingEnumeration valuesToDelete = getMissingValues(newAtt.getAll(), oldAtt.getAll());
                    // check for distinguished value, and ignore it...
                    if (isNamingAttribute)
                    	removeAnyDistinguishedValues(newRDN, attributeName, valuesToDelete);

                    if (valuesToDelete.size()>0)
                    {
                        deletionSet.put(new DXAttribute(attributeName, valuesToDelete));
                    }
                }
            }
        }
        return deletionSet;
    }



	/**
	 *    Checks two 'Attribute' objects for equality.
     *    XXX - should this be moved to DXAttribute?
	 */

	private static boolean attributesEqual(Attribute a, Attribute b)
	    throws NamingException
	{
	    // sanity checks...
	    if (a == null && b == null) return true;
	    if (a == null || b == null) return false;
	    if (a.size() == 0 && b.size() == 0) return true;
	    if (a.size() != b.size()) return false;
	    if (a.get() == null && b.get() == null) return true;
	    if (a.get() == null || b.get() == null) return false;
	    if (a.getID().equalsIgnoreCase(b.getID())==false) return false;


	    try
	    {
	        Object[] A = CBArray.enumerationToArray(a.getAll());
	        Object[] B = CBArray.enumerationToArray(b.getAll());
	        return CBArray.isUnorderedEqual(A,B);
	    }
	    catch (NamingException e)
	    {
	        log.log(Level.WARNING, "Naming Exception testing attributes " + a.getID() + " & " + b.getID() + " in DXAttributes:attributesEqual()", e);
	    }
	    return false; // only here if error occurred.
	}

	/**
	 *    Checks whether two 'Attributes' objects are equivalent (including naming attributes, if any).
	 */

	public static boolean attributesEqual(Attributes a, Attributes b)
	{
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    public boolean equals(Object o)
    {
        if (o == null) return false;

        try
        {
            if (o instanceof Attributes)
                return this.equals((Attributes) o);
        }
        catch (NamingException e)
        {
            return false;  // suppress exception :-(...
        }

        return false;
    }

    public boolean equals(Attributes atts) throws NamingException
    {
        // some quick and simple equality checks
        if (atts == null) return false;
        if (size() == 0 && atts.size() == 0) return true;
        if (size() != atts.size()) return false;

        // at this stage, we have equality candidates - two equally sized attributes...

        NamingEnumeration testAtts = getAll();
        while (testAtts.hasMore())                                // find changed attributes
        {
            Attribute testAtt = (Attribute)testAtts.next();
            String ID = testAtt.getID();

            Attribute bAtt = atts.get(ID);

            if ( emptyAtt(bAtt) ^ emptyAtt(testAtt) ) return false;

            if (attributesEqual(testAtt, bAtt) == false) return false;
        }

        // if we're here, the attributes must be equal!

        return true;
    }

    /**
	 *	Checks the naming enumeration and removes any distinguished values that
	 *  occur in the RDN.
	 *  @param newRDN the rdn to check for values in
	 *  @param attributeName the name of the attribute (potentially) in the RDN
	 *  @param values the list of values to potentially remove the distinguished value from
	 */

    private static void removeAnyDistinguishedValues(RDN newRDN, String attributeName, DXNamingEnumeration values)
    {
        String distinguishedValue = newRDN.getRawVal(attributeName);
        values.remove(distinguishedValue);  // remove dist. val. (if it is there)
    }


    /**
     *    Returns all the values in B that are missing in A.  (i.e. return B minus (B union A)).
     *    @param A the list of values to exclude.
     *    @param B the list of values to include.
     *    @return all elements of B not found in A.
     */

    static private DXNamingEnumeration getMissingValues(NamingEnumeration A, NamingEnumeration B)
        throws NamingException
    {
        DXNamingEnumeration ret = new DXNamingEnumeration(B);

        if (A == null) return ret;

        while (A.hasMore())
        {
            ret.remove(A.next());
        }
        return ret;
    }

    public String[] toIDStringArray()
    {
        DXNamingEnumeration ret = new DXNamingEnumeration(getIDs());
        return ret.toStringArray();
    }

    /**
     *    Utility ftn: checks that an attribute is not null and has at least
     *    one non-null value.
     */
    
    public static boolean emptyAtt(Attribute att)
    {
         return DXAttribute.isEmpty(att);
    }


    /**
     * Utility method - useful for creating a multi valued attributes for the Entry constructor,
     * esp when chained with a bunch of 'makeAtt()' calls.
     * @param vals
     * @return a newly created set of attributes
     */

    public static BasicAttributes makeAtts(Attribute[] vals)
    {
        BasicAttributes atts = new BasicAttributes();
        for (Attribute val : vals)
            atts.put(val);
        return atts;
    }

}