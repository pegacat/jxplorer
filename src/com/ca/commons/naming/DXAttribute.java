package com.ca.commons.naming;

import javax.naming.*;
import javax.naming.directory.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.CollationKey;
import java.text.Collator;

import com.ca.commons.cbutil.*;
import com.ca.commons.jndi.SchemaOps;

/**
 * <p>This is a schema-aware wrapper to BasicAttribute<p>
 *
 * <p>It goes to a lot of effort to figure out whether the attribute has
 * string values, or contains 'byte array' values.  If it contains byte array values,
 * it also tries to figure out whether they are ASN1 values.  This is important, as we
 * need to make sure that byte array values are passed correctly to JNDI, and in addition
 * we need to make sure that ASN1 values are passed using ';binary'.</p>
 */

//There was Confusion here between non-string syntaxes, and 'Binary' syntaxes which in ldap means ASN.1. encoded.
//We have modified the class to use 'non string' (for byte arrays) and 'ASN1' (for ASN1 structures).  All ASN1
//structures are 'non string', but attributes like jpeg photos and so on are byte arrays, but not ASN1 structures...


public class DXAttribute extends BasicAttribute implements Comparable
{
    //protected boolean isNonString = false;    // whether this attribute is isNonString data (e.g. ASN1 or a jpeg, an audio file etc.)
    protected boolean isString = true;    // whether this attribute is an ordinary string, or something else (e.g. ASN1 or a jpeg, an audio file etc.).  Default is true (most attributes are strings).
    protected boolean isASN1 = false;

    static boolean appendBinaryOption = false; // whether to add ';binary' to the end of non String attribute names.
    static boolean appendRFC4523BinaryOption = true; // whether to add ';binary' to the end of PKI attributes, as per rfc 4453

    String name;               // the name of the attribute (usually identical to the ID, unless ';binary' has been added to the ID)
    String syntaxOID;          // the OID of the Syntax (i.e. "1.3.6.1.4.1.1466.115.121.1.5" for binary)
    String syntaxDesc;         // the same thing as a human readable description
    String description;        // the free form description of the attribute that may exist in schema

    private static final String STRING = "string";
    private static final String BYTE_ARRAY = "bytes";
    private static final String ASN1 = "asn1";
    static Hashtable knownAttributeTypes;       // a list of known 'attributes, matched against the constants above

    static SchemaOps schema;  // the schema of the most recently connected to directory


    private CollationKey collationKey;                              // used for language-sensitive sorting.
    private static Collator myCollator = Collator.getInstance();    // used for language-sensitive sorting.

    private final static Logger log = Logger.getLogger(DXAttribute.class.getName());  // ...It's round it's heavy it's wood... It's better than bad, it's good...

    static
    {
        knownAttributeTypes = new Hashtable(100);
    }

    //TODO: Consider moving some of the 'attribute is string/ASN1/byte array' stuff into SchemaOps?

    /**
     * Normal constructor for an Attribute with no (current) value.
     * @param ID the name of the attribute (e.g. "userCertificate")
     */

    public DXAttribute(String ID)
    {
        super(ID);
        init();
    }

    /**
     * Normal constructor an Attribute with a single value.
     * @param ID the name of the attribute (e.g. "commonName")
     * @param value the value of the attribute (String or byte array).  e.g. "Fred"
     */

    public DXAttribute(String ID, Object value)
    {
        super(ID, value);
        init();
    }

    /**
     * Make a copy of a normal, pre-existing attribute.
     * @param att the attribute (e.g. BasicAttribute) to wrap.
     */

    public DXAttribute(Attribute att)
    {
        super(att.getID());
        try
        {
            addValues(att.getAll());
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "error reading attribute values for attribute " + getID() + "\n", e);
        }

        setName(att.getID());  // ?? necessary??

        init();
    }

    /**
     * Creates an attribute with an enumeration of existing values.
     * @param ID the name of the attribute (e.g. "favoriteDrink")
     * @param values a set of values (e.g. "midori"... "cointreau"... "lemon juice"... "japanese slipper"
     */

    public DXAttribute(String ID, NamingEnumeration values)
    {
        super(ID);
        addValues(values);
        init();
    }

       /**
     *
     * @param id the name of attribute (e.g. 'objectClass')
     * @param values the values of the attribute (e.g. {'top', 'person', 'inetorgperson'}
     */
    public DXAttribute(String id, Object[] values)
    {
        super(id);

        for (Object value : values)
        {
            add(value);
        }
    }


        /**
     *
     * @param id the name of attribute (e.g. 'objectClass')
     * @param values the values of the attribute (e.g. {'top', 'person', 'inetorgperson'}
     */
    public DXAttribute(String id, List values)
    {
        super(id);

        for (Object value : values)
        {
            add(value);
        }
    }
    /**
     * Adds a series of values to the attribute.
     * @param values a bunch of new values to append to the attribute
     */
    public void addValues(NamingEnumeration values)
    {
        try
        {
            while (values.hasMore())
            {
                add(values.next());
            }
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "error adding values for attribute " + getID(), e);
        }
    }

    /**
     * Attempts to sort the values of the attribute alphabetically.  This is a
     * relatively expensive operation (but not as expensive as directory access!),
     * and should be used sparingly.  (e.g. at GUI display time)
     */
    public void sort()
    {
        if (values.size()<2)
            return; // nothing to do.

        try
        {
            // $%^# don't know how to write this to keep both java 1.4 & 1.5 happy - compiles under both, but 1.5 IDEs may flag this...
            Collections.sort((List)values);
        }
        catch (ClassCastException e)
        {
            // nope, can't do that...
        }
        catch (UnsupportedOperationException e)
        {
            // nope, can't do that either...
        }
    }

    /**
     * This sets the standard schema to use while this connection is open.
     * (It may be possible in future releases to set schema on a per-Attribute
     * basis - it is not clear yet whether this would be useful.)
     */
    public static void setDefaultSchema(SchemaOps defaultSchema)
    {
        schema = defaultSchema;
    }

    public static SchemaOps getDefaultSchema()
    {
        return schema;
    }

    /**
     * <p>Code common to all constructors, run at the *end* of each constructor.</p>
     */
    protected void init()
    {
        // figure out whether this is a byte array, and whether it is an ASN1 structure
        setAttributeType();

        // used for sorting.  XXX should this be delayed until we actually need to do any sorting??
        collationKey = myCollator.getCollationKey(getID());
    }

    /**
     * This method examines the schema to test whether the attribute is string, byte array or ASN1 data.
     */

    protected void setAttributeType()
    {
        // quickly handle schema atts.
        String ID = getID();


        // skip 'synthetic' schemas
        if ("SYNTAXNAMENUMERICOIDDESCEQUALITY".indexOf(ID) != -1) return;

        ID = ID.toLowerCase();

        // see if we've already cached details about this attribute ID
        if (knownAttributeTypes.get(ID) != null)
        {
            isString = STRING.equals(knownAttributeTypes.get(ID));
            isASN1 = ASN1.equals(knownAttributeTypes.get(ID));
        }
        // if the attribute ends with ;binary, then it must be both isNonString and ASN1
        else if (ID.endsWith(";binary"))
        {
            isString = false;
            isASN1 = true;
        }
        else if (schema != null)
        {
            setAttributeTypeFromSchema();  // sets isString and isASN1 variables
        }
        else // guessing time - we don't have a schema, but we'll try to figure it out from the value.
        {
            try
            {
                if (size() > 0)
                {
                    Object value = get();

                    isString = (value == null || value instanceof String);

                    if (!isString)
                        isASN1 = isKnownASN1Attribute(ID);  // more guessing...
                }
            }
            catch (NamingException e)
            {
                log.log(Level.WARNING, "Unexpected error reading value from attribute " + ID, e);
            }
        }

        updateTypesHashtable(ID);
    }

    private void updateTypesHashtable(String ID)
    {
        if (knownAttributeTypes.contains(ID) == false)
        {
            if (isString)
                knownAttributeTypes.put(ID, STRING);
            else if (isASN1)
                knownAttributeTypes.put(ID, ASN1);
            else
                knownAttributeTypes.put(ID, BYTE_ARRAY);
        }
    }

    /**
     * This static utility function will quickly return if the attribute is
     * known to be a string.  It will not read the schema directly however, and
     * relies on the attribute having been already read in some form from the directory
     * (e.g. during an entry load).  It is *not* reliable for attributes that have not
     * actually been read in the current session however.
     * @param attID
     * @return
     */
    public static boolean isString(String attID)
    {
        attID = attID.toLowerCase();
        if (knownAttributeTypes.containsKey(attID))
            return (knownAttributeTypes.get(attID).equals(STRING));
        else if (isKnownASN1Attribute(attID))
            return false;
        else if (attID.endsWith(";binary"))
            return false;
        else
            return true; //??


    }

    /**
     * Does this attribute have a valid value (i.e. not null, or empty string).
     */

    public static boolean isEmpty(Attribute att)
    {
        if (att == null || att.size() == 0)
        {
            return true;
        }
        else if (att.size() == 1)
        {
            Object val = null;
            try
            {
                val = att.get();
            }
            catch (NamingException e)
            {
                return true;  // assume naming exception means empty attribute... (?)
            }

            if (val == null || "".equals(val))
                return true;
        }

        return false;
    }
    /**
     * This returns whether the syntax is a non-string syntax that should be
     * passed via a byte array in JNDI.
     *
     * @param syntaxName the name of the syntax
     *
     * @return whether the syntax is a non-string syntax that should be
     * passed via a byte array in JNDI
     */
    public static boolean isStringSyntax(String syntaxName)
    {
        if (syntaxName == null)
            return true;  //don't know - default to 'yes'

        int pos = syntaxName.indexOf("1.3.6.1.4.1.1466.115.121.1.");
        if (pos == -1)
            return true; //don't know - default to 'yes'

        // some faffing around to get the final number of the OID.  Could probably be neatened up :-).
        String number = syntaxName.substring(pos + "1.3.6.1.4.1.1466.115.121.1.".length());

        if (number.length() > 2)
        {
            number = number.substring(0, 2);
            char c = number.charAt(1);
            if (Character.isDigit(c) == false)
                number = number.substring(0, 1);
        }

        try
        {
            int finalNumber = Integer.parseInt(number);

            switch (finalNumber)
            {
                case 4:
                    return false; // 1.3.6.1.4.1.1466.115.121.1.4 - audio
                case 5:
                    return false; // 1.3.6.1.4.1.1466.115.121.1.5 - ASN1 binary
                case 8:
                    return false; // 1.3.6.1.4.1.1466.115.121.1.8 - certificate
                case 9:
                    return false; // 1.3.6.1.4.1.1466.115.121.1.9 - certificate list
                case 10:
                    return false; // 1.3.6.1.4.1.1466.115.121.1.10 - certificate pair
                case 28:
                    return false; // 1.3.6.1.4.1.1466.115.121.1.28 - jpeg
                case 40:
                    return false; // 1.3.6.1.4.1.1466.115.121.1.40 - octet string
                default:
                    return true;
            }
        }
        catch (NumberFormatException e)
        {
            log.log(Level.WARNING, "Unexpected error parsing syntax: " + syntaxName, e);
            return true;
        }

    }

    /**
     * Gets whether the attribute is an ASN.1 structure (and thus a candidate 
     * for having ;binary tacked on to its name)
     *
     * @param syntaxName the name of the syntax (usually a thumping great OID)
     * 
     * @return whether the attribute is an ASN.1 structure
     */
    public static boolean isASN1Syntax(String syntaxName)
    {
        if (syntaxName == null)
            return false;

        int pos = syntaxName.indexOf("1.3.6.1.4.1.1466.115.121.1.");
        if (pos == -1)
            return false;

        String number = syntaxName.substring(pos + "1.3.6.1.4.1.1466.115.121.1.".length());

        if (number.length() > 2)
        {
            number = number.substring(0, 2);
            char c = number.charAt(1);
            if (Character.isDigit(c) == false)
                number = number.substring(0, 1);
        }

        try
        {
            int finalNumber = Integer.parseInt(number);

            switch (finalNumber)
            {
                case 5:
                    return true; // 1.3.6.1.4.1.1466.115.121.1.5 - ASN1 binary
                case 8:
                    return true; // 1.3.6.1.4.1.1466.115.121.1.8 - certificate
                case 9:
                    return true; // 1.3.6.1.4.1.1466.115.121.1.9 - certificate list
                case 10:
                    return true; // 1.3.6.1.4.1.1466.115.121.1.10 - certificate pair
                default:
                    return false;
            }
        }
        catch (NumberFormatException e)
        {
            log.log(Level.WARNING, "Unexpected error parsing syntax: " + syntaxName, e);
            return false;
        }

    }

    /**
     * <p>Look up the attribute ID in the schema, and see whether the
     * syntax found there is a String, or if it is a byte array whether it is also an ASN1 structure.</p>
     * <p>This method sets the following globals:</p>
     * syntaxOID<br>
     * isString<br>
     * isASN1<br>
     */
    public void setAttributeTypeFromSchema()
    {
        if (schema != null)
        {
            try
            {
                String attributeNameInSchema = "AttributeDefinition/" + getID();
                Attributes atts = schema.getAttributes(attributeNameInSchema);

                Attribute syntaxAttribute = null;
                while (atts!=null && (syntaxAttribute = atts.get("SUP")) != null)
                {
                    atts = schema.getAttributes("AttributeDefinition/" + syntaxAttribute.get().toString());
                }
                if (atts!=null)
                     syntaxAttribute = atts.get("SYNTAX");

                if (syntaxAttribute != null)
                {
                    syntaxOID = syntaxAttribute.toString();
                    isString = isStringSyntax(syntaxOID);
                    if (!isString)
                        isASN1 = isASN1Syntax(syntaxOID);
                }
                else
                {
                    log.info(" Can't find SYNTAX for... " + getID());
                }
            }
            // XXX does this actually work on any known ldap server??
            // XXX - if so, should modify above so that when atts == null the below is run I guess...
            catch (NamingException e)  // usually means the attribute wasn't found under that name.  Try again using ';binary' suffix.
            {
                try
                {
                    Attributes atts = schema.getAttributes("AttributeDefinition/" + getID() + ";binary");

                    // if we find anything at all, it must be an ASN1 ;binary attribute...
                    if (atts != null)
                    {
                        isString = false;
                        isASN1 = true;
                    }
                }
                catch (Exception e2) // give up
                {
                    log.log(Level.INFO, "can't find syntax for attribute " + getID(), e);
                }
            }
        }
        else
        {
            log.fine("no schema available");
        }
    }


    /**
     * Whether the attribute contains isNonString data; ideally found by checking Syntax, but
     * often set by inspection of the attribute value (whether it is a Byte array).
     */
    public boolean isString()
    {
        return isString;
    }

    /**
     * Sets the isNonString status of the attribute.  Shouldn't be required any more;
     * should be set by syntax checking.
     *
     * @deprecated use setString() instead
     */
    /*
    public void setBinary(boolean bin)
    {
        setString(!bin);
    }
    */

    /**
     * Sets the isNonString status of the attribute.  Shouldn't be required any more;
     * should be set by syntax checking.
     */
    public void setString(boolean stringStatus)
    {
        knownAttributeTypes.put(getID(), (stringStatus)?STRING:BYTE_ARRAY);
        isString = stringStatus;
    }

    /**
     * Utility Function: takes a schema Entry class such as 'AttributeDefinition/cn',     *
     * and returns the result of looking up a particular attribute within that
     * (e.g. 'DESC').
     *
     * @param schemaEntry     the full schema entry name to lookup, e.g. 'ClassDefinition/alias'
     * @param schemaAttribute the attribute to look up, e.g. 'MUST'
     * @return the value of the schema entry attribute (e.g. '2.5.4.1')
     */

    public String schemaLookup(String schemaEntry, String schemaAttribute)
    {
        if (schema == null)
            return null;
        else
            return schema.schemaLookup(schemaEntry, schemaAttribute);
    }

    /**
     * Gets whether this attribute has a description in the schema.
     *
     * @return whether this attribute has a description in the schema
     */
    public boolean hasOptions()
    {
        if (description == null)  // try to load a description if it exists
            getDescription();

        if (description == null || description == "" || description.indexOf("LIST:") < 0)
            return false;

        return true;
    }

    /**
     * IF a description field has been set in the schema, and IF that
     * description field LISTs a set of values, read 'em and return them
     * as a string array.
     */

    public String[] getOptions()
    {
        if (description == null)
            getDescription();
        if (description == null || description == "")
            return new String[0];

        // can't be fagged working out java generic parse stuff - do own quickie...
        int len = description.length();
        int pos = description.indexOf("LIST:");
        if (pos < 0) return new String[0];
        pos += 5;
        int next = pos;

        Vector resVect = new Vector();
        resVect.add("");
//        String option;
        while (pos < len && pos > 0)
        {
            next = description.indexOf(',', next + 1);
            if (next < 0)
            {
                resVect.add(description.substring(pos));
                pos = 0;
            }
            else if (description.charAt(next - 1) != '\\')
            {
                resVect.add(description.substring(pos, next));
                pos = next + 1;
            }
            else
            {
                next++;  // move past the escaped comma
            }
        }

        // dump vector into string array, unescaping as we go.
        String[] result = new String[resVect.size()];
        for (int i = 0; i < resVect.size(); i++)
            result[i] = unEscape((String) resVect.elementAt(i));

        return result;
    }

    public String unEscape(String escapeMe)
    {
        int slashpos = escapeMe.indexOf('\\');
        while (slashpos >= 0)
        {
            escapeMe = escapeMe.substring(0, slashpos) + escapeMe.substring(slashpos + 1);
            slashpos = escapeMe.indexOf('\\');
        }
        return escapeMe;
    }

    /**
     * Gets the OID of the schema entry corresponding to this particular entry.  Whoo hoo... something like
     * '1.3.6.1.4.1.1466.115.121.1.27'.
     *
     * @return the syntax OID
     */

    public String getSyntaxOID()
    {
        if (syntaxOID == null)
            setAttributeTypeFromSchema();  // sets syntaxOID

        if (syntaxOID == null)
            return "<unknown>";
        else
            return syntaxOID;
    }


    /**
     * Returns (and caches) the syntax description.
     */
    public String getSyntaxDesc()
    {
        if (syntaxDesc == null)
            syntaxDesc = schemaLookup("SyntaxDefinition/" + getSyntaxOID(), "DESC");
        return syntaxDesc;
    }


    public String getSyntaxName()
    {
        if (syntaxOID == null)
            setAttributeTypeFromSchema();  // sets syntaxOID

        if (syntaxOID == null)
            return "<unknown>";
        else
            return schema.translateOID(syntaxOID);
    }


    /**
     * Usefull escape to allow renaming of attributes.  Use with caution,
     * since an arbitrary name may not correspond to a valid schema name.
     */
    public void setName(String newName)
    {
        name = newName;
    }

    public String getName()
    {
        if (name == null)
            name = schemaLookup("AttributeDefinition/" + getID(), "NAME");
        if (name == null)
            name = getID();

        return name;
    }

    /**
     * Returns the attribute's 'DESC' field from the attribute schema
     * definition, if such exists (it's an extension).  Not to be
     * confused with the Syntax Description, which is something like "isNonString".
     */

    public String getDescription()
    {
        if (description == null)
        {
            description = schemaLookup("AttributeDefinition/" + getID(), "DESC");
            if (description == null)    // if description is still null, set it
                description = "";       // to an empty string, to avoid repeatedly looking it up.
        }
        return description;
    }

    /**
     * <p>A synonym for getID().  Use 'toDebugString()' for the complete printout.</p>
     *
     * @return a string representation of the object
     */
    public String toString()
    {
        return getID();
    }

    public boolean isObjectClass()
    {
        String id = getID();
        if (id.equalsIgnoreCase("objectclass") || id.equalsIgnoreCase("oc")) // 'oc' included for backward compatibility with eTrust directory
            return true;

        return false;
    }

    /**
     * General descriptive string: used mainly for debugging...
     */

    public String toDebugString()
    {
        int count = 1;
        try
        {
            StringBuffer result = new StringBuffer().append("att: ").append(getID()).append(" (size=").append(size()).append(") ");

            NamingEnumeration vals = getAll();
            if (!isString)
            {
                result.append(" (Byte Array) ");
                while (vals.hasMore())
                {
                    try
                    {
                        byte[] b = (byte[]) vals.next();
                        result.append("\n    ").append((count++)).append(":").append(((b == null) ? "null" : CBBase64.binaryToString(b)));
                    }
                    catch (ClassCastException cce)
                    {
                        result.append("\n    ").append((count++)).append(": <error - not a byte array>");
                    }
                }
            }
            else
            {
                while (vals.hasMore())
                {
                    Object o = vals.next();
                    result.append("\n    ").append((count++)).append(":").append(((o == null) ? "null" : o.toString()));
                }
            }

            return result.append("\n").toString();
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "error listing values for " + getID(), e);
            return (getID() + " (error listing values)");
        }


    }

    /**
     * Utility function; returns a simple formatted string for any attribute.  Will not print out nicely for binary att vals.
     * @param att
     * @return
     */
    public static String toFormattedString(Attribute att)
    {
        if (att == null)
            return "<empty>"; // should never happen... ho ho.

        try
        {
            StringBuffer buffy = new StringBuffer(att.getID());
            if (att.size()==0)
            {
                buffy.append(": - ");
            }
            else if (att.size()==1)
            {
                buffy.append(": ").append(att.get(0).toString());
            }
            else
            {
                buffy.append(": ").append(att.get(0).toString());
                int paddingSize = att.getID().length()+2;
                char[] padding = new char[paddingSize];
                for (int i=0; i<paddingSize; i++)
                    padding[i] = ' ';
                for (int attIndex =1; attIndex <att.size(); attIndex++)
                    buffy.append("\n").append(padding).append(att.get(attIndex).toString());
            }
            return buffy.toString();
        }
        catch (Exception e)
        {
            return att.getID() + ": (error: " + e.getMessage() + ")";
        }
    }

    // ugly, ugly hack to add ';binary' when writting data to dir, but
    // not otherwise.
    public static void setVerboseBinary(boolean status)
    {
        appendBinaryOption = status;
        log.fine("setting isNonString attribute status to " + status);
    }

    /**
     * hack to allow ';binary' to be added to a small number of PKI attributes, as per RFC 4453
     * @param status
     */
    public static void setRFC4523BinaryHandling(boolean status)
    {
        appendRFC4523BinaryOption = status;
    }

    /**
     * This returns the name of the attribute.
     * @return the attribute name; e.g. 'commonName'
     */

    //TODO:  should this use the OID instead?  Would solve problems with loonies who
    //TODO:  use different names for the same attribute in different place
    //TODO: ALSO - need to get this in synch with 'getName()' - inconsistent use of IDs...!
    public String getID()
    {
        String id = super.getID();
        if (appendBinaryOption && !(id.endsWith(";binary")))
        {
            //System.out.println("binaryness = " + isNonString);
            // TODO: this should only happen for ASN.1. syntaxes.
            if (isASN1)
            {
                id = id + ";binary";
                log.info("appending ;binary to attribute name "+id);
            }

        }
        // added support for RFC 4523; explicitly append ';binary' to PKI related attributes.
        else if (appendRFC4523BinaryOption && (!id.endsWith(";binary")))
        {
            if (isRFC4523Attribute(id))
                return id + ";binary";

            String lowerCaseId = id.toLowerCase();

        }
        return id;
    }

    /* values taken from RFC 4523 */
        static String[] rfc4523Vals = new String[] {
                "certificatelist",
                "certificatepair",
                "supportedalgorithm",
                "usercertificate",
                "cacertificate",
                "crosscertificatepair",
                "certificaterevocationlist",
                "authorityrevocationlist",
                "deltarevocationlist",
                "supportedalgorithms"};

    private static boolean isRFC4523Attribute(String attName)
    {
        String lowerCaseId = attName.toLowerCase();

        for (String val: rfc4523Vals)
            if (lowerCaseId.equals(val))
                return true;

        return false;
    }

    /**
     * A last resort hack to guess whether an otherwise unknown attribute is
     * an ASN1 structure.  This should only be used if the schema is unavailable.
     *
     * @param attribute the attribute to check
     *
     * @return whether an otherwise unknown attribute is an ASN1 structure
     */

    public static boolean isKnownASN1Attribute(String attribute)
    {
        final String search = attribute.toLowerCase();

        if (isRFC4523Attribute(search))
            return true;
        else if (search.indexOf("userpkcs12") >= 0)
            return true;

        return false;
    }


    /**
     * This method gets all the values of an attribute.  Useful only
     * for multivalued attributes (use get() otherwise).
     *
     * @return all the values of this attribute in a String Array.
     */

    public Object[] getValues()
    {
        Object[] values = new Object[size()];

        try
        {
            for (int i = 0; i < size(); i++)
                values[i] = get(i);
            return values;
        }
        catch (NamingException e)
        {
            return new String[]{};
        }
    }


    /**
     * Gets rid of null values and empty strings from the value set.
     */
    public void trim()
    {
        for (int i = size() - 1; i > 0; i--)
        {
            Object o = null;
            try
            {
                o = get(i);
                if (o == null || "".equals(o))
                {
                    remove(i);
                }
            }
            catch (NamingException e)  // shouldn't happen...
            {
                log.log(Level.WARNING, "Bad Attribute value in DXAttribute - removing ", e);
                remove(i);             // .. but remove offending entry if it does.

            }
        }
    }

    public void setOrdered(boolean state)
    {
        ordered = state;
    }


    /**
     * Returns true if this attribute is a SINGLE-VALUE attribute.
     *
     * @return true if this attribute is a SINGLE-VALUE attribute,
     *         false otherwise.
     */

    public boolean isSingleValued()
    {/* TE */
        return (schema==null?false:schema.isAttributeSingleValued(getName()));
    }

    /**
     * <p>The collation key is usually set by the constructor
     * based on getID(), but
     * may be over-ridden here if required.  Not sure that this
     * will ever be necessary; maybe if doing something insanely
     * clever across multiple platforms with different languages?</p>
     *
     * @param key
     */
    public void setCollationKey(CollationKey key)
    {
        collationKey = key;
    }

    /**
     * <p>This returns the collation key used for language sensitive
     * sorting.</p>
     *
     * @return the collaction key
     */
    public CollationKey getCollationKey()
    {
        return collationKey;
    }

    /**
     * <p>This is intended to compare two DXAttribute objects, and will
     * throw a class cast exception for anything else.  It sorts on
     * their internal collationkeys.</p>
     *
     * @param o the object to compare to
     * 
     * @return an integer value. Value is less than zero if this is less than
     * o, value is zero if this and o are equal and value is greater than zero
     * if this is greater than o.
     */
    public int compareTo(Object o)
    {
        return collationKey.compareTo(((DXAttribute) o).getCollationKey());
    }

}