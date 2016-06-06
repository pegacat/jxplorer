/**
 *
 *
 * Author: Chris Betts
 * Date: 28/11/2002 / 17:02:19
 */
package com.ca.commons.jndi;

import javax.naming.directory.*;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.ldap.LdapContext;
import java.util.*;

import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * <p>The Schema Ops provides a number of convenience methods for accessing schema information.</p>
 * <p/>
 * <p>In addition, it allows the schema to be accessed even if a particular jndi provider
 * does not provide a getSchemaOps() call, by attempting to access a subschema subentry call directly.
 * (e.g. for a DSML directory).</p>
 */
public class SchemaOps
{
    protected LdapContext ctx = null;            // the root jndi context used for all directory queries.
    Attributes rawSchemaAttributes = null;      // what is read from the directory subschema subentry
    private String schemaRoot = null;           // the entry in the directory that holds the schema attribute set - usually cn=schema
    HashMap oids = new HashMap(1000);           // a fast lookup list of oids to descriptive names.

    final static String subschemaAttributeName = "subschemaSubentry";

    private final static Logger log = Logger.getLogger(SchemaOps.class.getName());

    public static final String SCHEMA_FAKE_OBJECT_CLASS_NAME = "synthetic_JXplorer_schema_object";
    private static final BasicAttribute schemaObjectClassAttribute = new BasicAttribute("objectClass");

    private ArrayList fullObjectClassArray = null; // cache the complete list of object classes; it gets used a bit.

    private ArrayList fullAttributeNameArray = null; // cache the complete list of attribute names; it gets used a bit.

    static
    {
        schemaObjectClassAttribute.add("top");
        schemaObjectClassAttribute.add(SCHEMA_FAKE_OBJECT_CLASS_NAME);
    }

    /**
     * <p>Initialise the SchemaOps object, and read the
     * full schema from the directory.  Note that this is a very
     * expensive operation, that can involve downloading 10-100k from
     * the directory!</p>
     */
    public SchemaOps(LdapContext context)
            throws NamingException
    {
        ctx = context;
        if (ctx == null)
        {
            setSchemaRoot("cn=schema"); // default - but doesn't do anything since ctx==null
            loadOIDs(); // load static OIDs - for testing, and future off-line schema functionality

            return;
        }

        log.finest("Reading Schema info from directory context");

        setSchemaRoot(getSchemaRoot());
        rawSchemaAttributes = getRawSchema();

//printRawSchema();
//System.out.println("\n\n\n\n");

        loadOIDs();
    }

    /**
     * <p>Initialise the SchemaOps object from an Attributes list.
     * This constructor is intended for testing.
     * </p>
     */
    // package visibility for testing

    public SchemaOps(Attributes rawSchemaAtts)
            // throws NamingException
    {
        ctx = null;
        rawSchemaAttributes = rawSchemaAtts;

        setSchemaRoot("cn=schema");
        loadOIDs();
        log.finest("SCHEMA ROOTX:" + getSchemaRoot());
    }

    /**
     * This attempts to translate an OID into a descriptive name.  If it cannot
     * find a translation, it will return the oid unchanged.
     *
     * @param oid the 'dot form' OID, e.g. "2.5.6.2" or whatever
     * @return the human readable string form of the OID (e.g. "country")
     */
    public String translateOID(String oid)
    {
        if (oids.containsKey(oid))
            return (String) oids.get(oid);
        else
        {
            return oid;
        }
    }

    /**
     * Returns whether the passed OID or ldap string name is already known to JXplorer.
     * @param oid
     * @return true if the oid or string name is known to JX.
     */
    public boolean knownOID(String oid)
    {
        return oids.containsKey(oid);
    }

    /**
     * setup the global list of oids vs readable strings, by loading the schema and using the rfc defaults
     */
    protected void loadOIDs()
    {
        loadOIDsFromSchema();

        loadStaticOIDs();
    }

    /**
     * Iterates through the schema finding OIDs and their corresponding descriptions.
     */
    protected void loadOIDsFromSchema()
    {
        if (rawSchemaAttributes == null)
            return;

        try
        {
            NamingEnumeration rawSchemaAtts = rawSchemaAttributes.getAll();
            while (rawSchemaAtts.hasMoreElements())
            {
                Attribute rawSchemaAtt = (Attribute) rawSchemaAtts.nextElement();
                NamingEnumeration values = rawSchemaAtt.getAll();

                while (values.hasMoreElements())
                {
                    String value = (String) values.nextElement();
                    if (value.indexOf('(') == -1)
                        log.finest("skipping non schema attribute: " + rawSchemaAtt.getID() + ":" + value);
                    else
                        oids.put(getOID(value), getFirstName(value));
                }
            }
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "Unable to read schema oids: ", e);
        }
    }


    protected void loadStaticOIDs()
    {
        // a quick pick of common syntaxes for Active Directory support
        // (and other servers that don't publish syntax descriptions)
        // taken from rfc 2252

        oids.put("1.3.6.1.4.1.1466.115.121.1.1", "ACI Item");
        oids.put("1.3.6.1.4.1.1466.115.121.1.2", "Access Point");
        oids.put("1.3.6.1.4.1.1466.115.121.1.3", "Attribute Type Description");
        oids.put("1.3.6.1.4.1.1466.115.121.1.4", "Audio");
        oids.put("1.3.6.1.4.1.1466.115.121.1.5", "Binary");
        oids.put("1.3.6.1.4.1.1466.115.121.1.6", "Bit String");
        oids.put("1.3.6.1.4.1.1466.115.121.1.7", "Boolean");
        oids.put("1.3.6.1.4.1.1466.115.121.1.8", "Certificate");
        oids.put("1.3.6.1.4.1.1466.115.121.1.9", "Certificate List");
        oids.put("1.3.6.1.4.1.1466.115.121.1.10", "Certificate Pair");
        oids.put("1.3.6.1.4.1.1466.115.121.1.11", "Country String");
        oids.put("1.3.6.1.4.1.1466.115.121.1.12", "DN");
        oids.put("1.3.6.1.4.1.1466.115.121.1.13", "Data Quality Syntax");
        oids.put("1.3.6.1.4.1.1466.115.121.1.14", "Delivery Method");
        oids.put("1.3.6.1.4.1.1466.115.121.1.15", "Directory String");
        oids.put("1.3.6.1.4.1.1466.115.121.1.16", "DIT Content Rule Description");
        oids.put("1.3.6.1.4.1.1466.115.121.1.17", "DIT Structure Rule Description");
        oids.put("1.3.6.1.4.1.1466.115.121.1.18", "DL Submit Permission");
        oids.put("1.3.6.1.4.1.1466.115.121.1.19", "DSA Quality Syntax");
        oids.put("1.3.6.1.4.1.1466.115.121.1.20", "DSE Type");
        oids.put("1.3.6.1.4.1.1466.115.121.1.21", "Enhanced Guide");
        oids.put("1.3.6.1.4.1.1466.115.121.1.22", "Facsimile Telephone Number");
        oids.put("1.3.6.1.4.1.1466.115.121.1.23", "Fax");
        oids.put("1.3.6.1.4.1.1466.115.121.1.24", "Generalized Time");
        oids.put("1.3.6.1.4.1.1466.115.121.1.25", "Guide");
        oids.put("1.3.6.1.4.1.1466.115.121.1.26", "IA5 String");
        oids.put("1.3.6.1.4.1.1466.115.121.1.27", "INTEGER");
        oids.put("1.3.6.1.4.1.1466.115.121.1.28", "JPEG");
        oids.put("1.3.6.1.4.1.1466.115.121.1.54", "LDAP Syntax Description");
        oids.put("1.3.6.1.4.1.1466.115.121.1.56", "LDAP Schema Definition");
        oids.put("1.3.6.1.4.1.1466.115.121.1.57", "LDAP Schema Description");
        oids.put("1.3.6.1.4.1.1466.115.121.1.29", "Master And Shadow Access Points");
        oids.put("1.3.6.1.4.1.1466.115.121.1.30", "Matching Rule Description");
        oids.put("1.3.6.1.4.1.1466.115.121.1.31", "Matching Rule Use Description");
        oids.put("1.3.6.1.4.1.1466.115.121.1.32", "Mail Preference");
        oids.put("1.3.6.1.4.1.1466.115.121.1.33", "MHS OR Address");
        oids.put("1.3.6.1.4.1.1466.115.121.1.55", "Modify Rights");
        oids.put("1.3.6.1.4.1.1466.115.121.1.34", "Name And Optional UID");
        oids.put("1.3.6.1.4.1.1466.115.121.1.35", "Name Form Description");
        oids.put("1.3.6.1.4.1.1466.115.121.1.36", "Numeric String");
        oids.put("1.3.6.1.4.1.1466.115.121.1.37", "Object Class Description");
        oids.put("1.3.6.1.4.1.1466.115.121.1.40", "Octet String");
        oids.put("1.3.6.1.4.1.1466.115.121.1.38", "OID");
        oids.put("1.3.6.1.4.1.1466.115.121.1.39", "Other Mailbox");
        oids.put("1.3.6.1.4.1.1466.115.121.1.41", "Postal Address");
        oids.put("1.3.6.1.4.1.1466.115.121.1.42", "Protocol Information");
        oids.put("1.3.6.1.4.1.1466.115.121.1.43", "Presentation Address");
        oids.put("1.3.6.1.4.1.1466.115.121.1.44", "Printable String");
        oids.put("1.3.6.1.4.1.1466.115.121.1.58", "Substring Assertion");
        oids.put("1.3.6.1.4.1.1466.115.121.1.45", "Subtree Specification");
        oids.put("1.3.6.1.4.1.1466.115.121.1.46", "Supplier Information");
        oids.put("1.3.6.1.4.1.1466.115.121.1.47", "Supplier Or Consumer");
        oids.put("1.3.6.1.4.1.1466.115.121.1.48", "Supplier And Consumer");
        oids.put("1.3.6.1.4.1.1466.115.121.1.49", "Supported Algorithm");
        oids.put("1.3.6.1.4.1.1466.115.121.1.50", "Telephone Number");
        oids.put("1.3.6.1.4.1.1466.115.121.1.51", "Teletex Terminal Identifier");
        oids.put("1.3.6.1.4.1.1466.115.121.1.52", "Telex Number");
        oids.put("1.3.6.1.4.1.1466.115.121.1.53", "UTC Time");
    }

    /**
     * Utility method to print a syntax subtree...
     *
     * @param syntaxRoot the root of the syntax tree to print out, e.g. "", "objectClasses"
     * @throws NamingException
     */
    protected void debugPrint(String syntaxRoot)
            throws NamingException
    {
        System.out.println("---DEBUG PRINT---");
        System.out.println("schema root: " + getSchemaRoot());
        if (syntaxRoot.length() > 0 && syntaxRoot.startsWith("schema=") == false)
            syntaxRoot = "schema=" + syntaxRoot;

        tabbedDebugPrint(syntaxRoot, "");
        System.out.println("-----------------");
    }

    protected void tabbedDebugPrint(String syntaxElement, String indent)
            throws NamingException
    {
        System.out.println(indent + syntaxElement);
        Attributes entry = getAttributes(syntaxElement);
        System.out.println(indent + "--==< " + syntaxElement + ">==--");
        if (entry == null)
            System.out.println(indent + " ** NULL ENTRY **");
        else
        {
            NamingEnumeration atts = entry.getAll();
            while (atts.hasMoreElements())
            {
                Attribute att = (Attribute) atts.nextElement();
                System.out.println(indent + "att " + att.getID());
                NamingEnumeration values = att.getAll();
                while (values.hasMoreElements())
                    System.out.println(indent + "   " + values.nextElement().toString());
            }
        }
        System.out.println(indent + "-");

        ArrayList list = listEntryNames(syntaxElement);
        if (list == null)
        {
            return;
        }
        for (int i = 0; i < list.size(); i++)
        {
            String nextLevel = syntaxElement;
            if (nextLevel.length() > 0)
                nextLevel = "," + nextLevel;
            nextLevel = "schema=" + list.get(i) + nextLevel;
            tabbedDebugPrint(nextLevel, "\t" + indent);
        }
    }

    public void printRawSchema()
    {
        if (rawSchemaAttributes == null)
        {
            System.out.println("NO SCHEMA READ!");
            return;
        }
        try
        {
            System.out.println("---RAW SCHEMA---");
            Enumeration attEnum = rawSchemaAttributes.getAll();
            while (attEnum.hasMoreElements())
            {
                Attribute att = (Attribute) attEnum.nextElement();
                String ID = att.getID();
                Enumeration vals = att.getAll();
                while (vals.hasMoreElements())
                    System.out.println(ID + " : " + vals.nextElement());
            }
        }
        catch (NamingException e)
        {
            System.out.println("error printing raw schema:" + e);
        }
    }

    /**
     * <p>This returns the raw schema straight from the
     * directory.  Only use this if you <i>really</i>
     * know what you're doing - see getAttributes() for
     * an explanation.</p>
     */

    // ...Your schema caching code here...
    public Attributes getRawSchema()
            throws NamingException
    {
        String rawSchemaRoot = getSchemaRoot();
        log.finest("reading raw schema from " + rawSchemaRoot);
        // need to explicitly list operational attributes... (although eTrust Directory doesn't need this, others do)
        Attributes rawSchema = null;

        if (ctx != null)
            rawSchema = ctx.getAttributes(rawSchemaRoot, new String[]{"attributeTypes", "objectClasses", "matchingRules", "ldapSyntaxes", "*"});

        if (rawSchema == null)
        {
            log.warning("null schema read - returning empty schema list.");
            rawSchema = new BasicAttributes();  // return empty atts rather than null, to cut down on 'check for null' code...
        }
        else
        {
            if (rawSchema.size() == 0) // may be a type of directory that requires explicit listing of schema objects...
            {
                log.warning("Unable to read schema details from directory.");
                rawSchema = new BasicAttributes();  // give up - set to empty :-(
                return rawSchema;
            }

            log.finest("some schema read...");


            rawSchema.remove("objectClass");    // Paranoid.  Yes.
            rawSchema.remove("oc");             // But very thorough.
            rawSchema.remove("objectclass");
            String nameAttribute = rawSchemaRoot.substring(0, rawSchemaRoot.indexOf('='));
            rawSchema.remove(nameAttribute);
        }

        return rawSchema;
    }

    private void setSchemaRoot(String schema)
    {
        schemaRoot = schema;
    }

    /**
     * returns the root DN of the schema subentry as a string.
     *
     * @return the schema subentry (i.e. something like 'cn=schema')
     */

    public String getSchemaRoot()
    {
        if (schemaRoot != null)
            return schemaRoot;

        if (ctx != null)
        {
            try
            {
                log.finest("start get schema root call");
                Attributes SSSE;
                SSSE = ctx.getAttributes("", new String[]{subschemaAttributeName});
                if (SSSE != null && SSSE.get(subschemaAttributeName) != null)
                    schemaRoot = (String) SSSE.get(subschemaAttributeName).get();

                log.finest("schema root read as being: '" + String.valueOf(schemaRoot) + "'");
            }
            catch (NamingException e)
            {
                // revert to using good old 'cn=schema' ...
            }
        }

        if (schemaRoot == null)
        {
            log.finest("forcing value of schema root to 'cn=schema', since can't read subschema attribute name");
            schemaRoot = "cn=schema";  // default: this is what it usually is anyway... :-)
        }
        return schemaRoot;
    }

    /**
     * <p>HERE BE DRAGONS</p>
     * <p/>
     * <p>Similarly to jndi, we impose a structure on the raw schema entry.  In the
     * directory, there is a single schema entry, with a number of multi-valued attributes, e.g.
     * one attribute for 'objectClasses', one for 'ldapSyntaxes'.  Each attribute value has
     * a wretched format, e.g. an objectClasses value might be:
     * " ( 0.9.2342.19200300.100.4.4 NAME 'newPilotPerson' SUP ( person )
     * STRUCTURAL MAY ( uid $ mail $ drink $ roomNumber $ userClass $ homePhone $ homePostalAddress
     * $ secretary $ personalTitle $ preferredDeliveryMethod $ businessCategory $ janetMailbox
     * $ otherMailbox $ mobile $ pager $ organizationalStatus $ mailPreferenceOption $ personalSignature ) ) "</p>
     * <p/>
     * <p>We break this up by adding an extra layer of virtual attributes, turning the above attribute
     * value into an attributes object (with a 'MAY' attribute, a 'NAME' attribute etc. ...
     * (If you need the real deal, use getRawSchema()... but be sure
     * you know what you're doing :-). </p>
     *
     * @param entryName the name of an entry, e.g. schema=cn,schema=attributeTypes
     */
    public Attributes getAttributes(String entryName)
            throws NamingException
    {
        entryName = mangleEntryName(entryName);

        BasicAttributes schemaAttributes = new BasicAttributes(); // add fake object class to keep some DXattributes routines happy...
        schemaAttributes.put(schemaObjectClassAttribute);

        if (entryName == null || entryName.length() == 0)                    // return synthetic entry for syntax root
        {
            schemaAttributes.put(subschemaAttributeName, schemaRoot);
        }
        else if (entryName.indexOf(',') == -1 && entryName.indexOf('/') == -1) // return synthetic entry for syntax type headings
        {
            String schemaType = entryName.substring(entryName.indexOf('=') + 1);
            schemaAttributes.put("schemaType", schemaType);
        }
        else
        {
            schemaAttributes = getAttributesFromSchemaName(entryName);
        }
        return schemaAttributes;
    }

    /**
     * <p>This method does three things; firstly, it trims the schema root from
     * the name if present (e.g. trims ",cn=schema" in most cases).
     * Secondly, it trims any ';binary' from the end of the string.
     * Finally, it translates the pseudo-schema names jndi imposes on top of the
     * standard ldap/X500 syntax names from rfc 2252/2256:</p>
     * <ul>
     * <li>AttributeDefinition =>  attributeTypes
     * <li>ClassDefinition => objectClasses
     * <li>SyntaxDefinition => ldapSyntaxes
     * </ul>
     *
     * @param entryName : schema=ClassDefinition,cn=schema or schema=cn,schema=attributeTypes
     * @return mangled name
     */
    protected String mangleEntryName(String entryName)
    {
        if (entryName.indexOf("ClassDefinition") > -1)
            entryName = entryName.replaceAll("(ClassDefinition)", "objectClasses");
        if (entryName.indexOf("SyntaxDefinition") > -1)
            entryName = entryName.replaceAll("(SyntaxDefinition)", "ldapSyntaxes");
        if (entryName.indexOf("AttributeDefinition") > -1)
            entryName = entryName.replaceAll("(AttributeDefinition)", "attributeTypes");

        // if it is an ldap name, restructure it to the schema=..., schema=... used in JX.
        if (entryName.indexOf('/') > 0)
        {
            // trim ;binary for prettiness...
            // TODO: Is this such a good idea?; it may mess up some directories such as slapd...
            int pos = entryName.indexOf(";binary");
            if (pos > -1)
                entryName = entryName.substring(0, pos);


            int slashpos = entryName.indexOf('/');
            String topLevelName = entryName.substring(0, slashpos);
            String specificName = entryName.substring(++slashpos);
            return "schema=" + specificName + ",schema=" + topLevelName;
        }

        // otherwise it is already a JX style name, so we clean it up a bit to get it in a standard form

        // trim the schema root off the end, since we're only interested in the next level (objectclasses etc.)
        int pos = entryName.indexOf(schemaRoot);
        if (pos > 0)                                        // not '-1' due to need to trim preceeding comma
            entryName = entryName.substring(0, pos - 1);

        // a little naughtily, we often use 'cn=schema' as shorthand for the schema root... get rid of that instead if it is different from the schema root (usually it isn't)
        pos = entryName.indexOf("cn=schema");
        if (pos > 0)                                        // not '-1' due to need to trim preceeding comma
            entryName = entryName.substring(0, pos - 1);


        return entryName;
    }

    /**
     * returns the specific schema entry name - eg 'cn' in 'schema=cn,schema=attributeTypes'
     *
     * @param entryName the schema entry DN in JX format - 'schema=cn,schema=attributeTypes'
     * @return the specific schema name - e.g. 'cn'
     * @throws NamingException
     */
    protected String getSpecificName(String entryName)
            throws NamingException
    {
        int equalpos = entryName.indexOf('=') + 1;
        int commapos = entryName.indexOf(',');
        if (equalpos <= 0 || commapos == -1 || equalpos > commapos)
            throw new NamingException("error parsing schema dn '" + entryName + "' ");

        return entryName.substring(equalpos, commapos);
    }

    /**
     * returns the specific schema entry name - eg 'cn' in 'schema=cn,schema=attributeTypes'
     *
     * @param entryName the schema entry DN in JX format - 'schema=cn,schema=attributeTypes'
     * @return the specific schema name - e.g. 'cn'
     * @throws NamingException
     */

    protected String getTypeName(String entryName)
            throws NamingException
    {
        if (entryName.endsWith(",cn=schema"))
            entryName = entryName.substring(0, entryName.length() - 10);

        int equalpos = entryName.lastIndexOf('=') + 1;
        return entryName.substring(equalpos);
    }


    /**
     * This looks up the raw 'Attribute' corresponding to the full entryName.  It then takes the value
     * of that attribute, and creates a new Attributes object by parsing that value.
     *
     * @param entryName
     * @throws NamingException
     */
    // package visibility for testing
    protected BasicAttributes getAttributesFromSchemaName(String entryName) throws NamingException
    {
        if (rawSchemaAttributes == null)
            return null;

        entryName = mangleEntryName(entryName);

        // do all this the slow way for now...
        String schemaTypeName = getTypeName(entryName);
        String specificName = getSpecificName(entryName);

        Attribute schemaGroup = rawSchemaAttributes.get(schemaTypeName);

        if (schemaGroup == null)
        {
            // some wierdo directories manage to get their cases muddled.  This is a last-gasp attempt
            // to read them by using an all-lower case version of the name.
            schemaGroup = rawSchemaAttributes.get(schemaTypeName.toLowerCase());
            throw new NamingException("Unable to find schema entry for schema type '" + schemaTypeName + "'");
        }

        NamingEnumeration schemaValues = schemaGroup.getAll();
        String schemaValue;
        while (schemaValues.hasMore())
        {
            schemaValue = (String) schemaValues.next();
            String[] names = getNames(schemaValue);
            for (int i = 0; i < names.length; i++)
            {
                // use case-insensitive match to cope with weirdo directories that muddle case
                if (specificName.equalsIgnoreCase(names[i]))
                {
                    return getAttributesFromSchemaValue(schemaValue);
                }
            }
        }
        return null;
    }

    /**
     *  <p>This Parses the schema values from the syntaxValue.  The parser is very simple,
     *  and this might cause trouble in future years.  The assumptions are:</p>
     *  <ul>
     *  <li> syntax keywords are all in upper case (e.g. NAME, DESC etc.).
     *  <li> keywords are all followed by a list of values in mixed case.
     *  <li> all values are more than one character long.
     *  <li>
     *  </ul>
     */
    // package visibility for testing

    /**
     * A quicky parse routine to spin through a list ( 'fred' 0.2.3.4 'nigel' 'horse heads' ) fnord fnord fnord ( 'more fnords' )
     * adding elements to the passed attribute until it hits the first ')'
     *
     * @param schemaAttribute attribute to add the bracketed strings to.
     * @param st              a string tokeniser to read values from.
     */
    private void addBracketedValues(Attribute schemaAttribute, StringTokenizer st)
    {
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            if (token.endsWith(")"))  // that's all for this list...
            {
                if (token.length() > 1)
                    schemaAttribute.add(getQuotedTokens(token.substring(0, token.length() - 1), st));

                return;
            }

            schemaAttribute.add(getQuotedTokens(token, st));
        }
    }

    /**
     * If the token has an opening ' character, this will read either it or a sequence until
     * it gets to the closing ' - otherwise it just returns the token.
     *
     * @param token
     * @return token without quotes
     */
    private String getQuotedTokens(String token, StringTokenizer st)
    {
        if (token.charAt(0) != '\'')
            return token;

        if (token.length() < 2)  // ??? Wierd - this should never happen.
            return token;

        if (token.charAt(0) == '\'' && token.charAt(token.length() - 1) == '\'')
            return token.substring(1, token.length() - 1);

        // string of quoted text... this would be so much easier in perl.  sigh.
        StringBuffer returnText = new StringBuffer(token.substring(1));
        while (st.hasMoreTokens())
        {
            token = st.nextToken();
            if (token.endsWith("'"))
                return (returnText.append(" ").append(token.substring(0, token.length() - 1)).toString());
            else
                returnText.append(" ").append(token);
        }

        return returnText.toString();  // someone forgot the closing quote I guess...
    }


    /**
     * This parses an attribute schema string such as: "( 2.5.6.2 NAME 'country' SUP ( top ) STRUCTURAL MUST ( c ) MAY ( description $ searchGuide ) )"
     * and breaks it up into a pseudo attribute with elements such as 'NAME'->'country' and 'MAY' -> 'description', 'searchGuide'
     *
     * @param syntaxValue the string of syntax details as per rfc 2252
     * @return the pseudo attribute, suitable for display.
     */
    protected BasicAttributes getAttributesFromSchemaValue(String syntaxValue)
    {
        BasicAttributes schemaValues = new BasicAttributes(); // add fake object class to keep some DXattributes routines happy...
        schemaValues.put(schemaObjectClassAttribute);

        StringTokenizer st = new StringTokenizer(syntaxValue, " \t\n\r\f$");

        // Special Handling for first OID case
        if (st.hasMoreTokens())
        {
            String oid = st.nextToken();
            if (oid.startsWith("("))  // can be a stray opening '('.
            {
                if (oid.length() == 1)
                    oid = st.nextToken();
                else
                    oid = oid.substring(1);     // handle case where there is no space between ( and Oid.
            }
            schemaValues.put(new BasicAttribute("OID", oid));
        }

        while (st.hasMoreTokens())
        {
            String attributeID = st.nextToken();
            if (attributeID.endsWith(")") == false)   // stray closing ')' is possible (see above)
            {
                addAttribute(schemaValues, attributeID, st);
            }
            else
            {
                if (attributeID.length() > 1)
                    addAttribute(schemaValues, attributeID.substring(1), st);
            }
        }

        try
        {
            Attribute syntaxOID;
            if ((syntaxOID = schemaValues.get("SYNTAX")) != null)
            {
                String syntaxOIDString = (String)syntaxOID.get();
                String syntaxDescription = translateOID(syntaxOIDString);
                schemaValues.put(new BasicAttribute("SYNTAX Description", syntaxDescription));
            }
        }
        catch (Exception e)
        {
            log.log(Level.INFO, "unable to translate syntax oid ", e);
        }
        return schemaValues;
    }

    /**
     * Parses the current attribute from the schema string.  NB - this method will recurse if two
     * attribute keyword tokens are discovered next to each other.
     *
     * @param schemaValues  - the Attributes object to add the schema Attribut<b>e</b> objects to.
     * @param attributeName - the name of the new Attribut<b>e</b> object to construct
     * @param st            the token list to read the attribute data from
     */

    private void addAttribute(Attributes schemaValues, String attributeName, StringTokenizer st)
    {
        BasicAttribute schemaAttribute = new BasicAttribute(attributeName);
        schemaValues.put(schemaAttribute);

        if (st.hasMoreTokens())
        {
            String token = st.nextToken();
            if (token.startsWith("("))
            {
                if (token.length() > 1)
                {
                    if (token.endsWith(")") == true)  // pathalogical case: "(VALUE)"
                    {
                        token = token.substring(0, token.length() - 1);
                        schemaAttribute.add(token.substring(1));
                    }
                    else
                    {
                        schemaAttribute.add(token.substring(1));
                        addBracketedValues(schemaAttribute, st);
                    }
                }
                else
                    addBracketedValues(schemaAttribute, st);
            }
            else if (token.endsWith(")"))
            { // do nothing - this should be the very end of the string tokenizer list, and this the left over bit at the end.
                // (note this is *not* the match to the "{" case above...!)
            }
            else if (isSyntaxKeyword(token) == true)
            {
                addAttribute(schemaValues, token, st);
            }
            else
            {
                token = getQuotedTokens(token, st);
                schemaAttribute.add(token);
            }
        }
    }


    /**
     * Read the next block of text in a single quoted block, e.g. DESC 'some stuff here',
     * (assuming that the first quoted string has already been read before the string
     * tokenizer has been passed in, e.g. this should get "stuff here'", and return
     * "stuff here".
     *
     * @param st a string tokenizer with a series of tokens one of which ends in a single quote
     * @return the concatenated string of all tokens up to and including the one suffixed with a single quote.
     */
    private static String readQuoteBlock(StringTokenizer st)
    {
        StringBuffer returnBuffer = new StringBuffer();

        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            returnBuffer.append(" ");
            returnBuffer.append(token);
            if (token.endsWith("'"))
            {
                returnBuffer.deleteCharAt(returnBuffer.length() - 1);  // remove final quote
                return returnBuffer.toString();
            }
        }
        log.finest("unexpected end of schema text in single quoted block");
        return returnBuffer.toString();
    }

    /**
     * This <i>should</i> probably have a list of keywords extracted from rfc 2252... but I can't be
     * bothered, so I'll simply test that the token is entirely uppercase, alphabetic characters...
     *
     * @param token the token to test for being an rfc 2252 syntax keyword.
     * @return tue if the token is a special ldap keyword
     */
    private static boolean isSyntaxKeyword(String token)
    {
        String[] reservedKeywords = {
            "ABSTRACT",
            "APPLIES",
            "AUXILIARY",
            "COLLECTIVE",
            "DESC",
            "EQUALITY",
            "MAY",
            "MUST",
            "NAME",
            "NO-USER-MODIFICATION",
            "OBSOLETE",
            "ORDERING",
            "SINGLE-VALUE",
            "STRUCTURAL",
            "SUBSTR",
            "SUP",
            "SYNTAX",
            "USAGE"};

        int size = reservedKeywords.length;
        for (int i = 0; i < size; i++)
            if (reservedKeywords[i].equals(token))
                return true;

        if (token.startsWith("X-"))
            return true;

        return false;  // probably isn't - but they might change the standard I suppose.  Oh well...
    }
/*  simply returning true for capitalised is too simplistic...
        int i, len = token.length();
        char c;
        for (i=0; i<len; i++)
        {
            c = token.charAt(i);
            if (c<'A' || c>'Z')
                return false;
        }
        return true;
*/

    /**
     * <p>Returns the next level in our virtual view of the schema.
     * The virtual view may either be in the jndi form (e.g. "objectClass/person")
     * of a 'dn like form' (e.g. "schema=objectClass, schema=person").  If it is
     * the latter, the 'fake attribute type' is ignored, e.g. "fnordle=objectClass,
     * snork=person" would resolve the same as "schema=objectClass, schema=person").</p>
     *
     * @param entryName the full schema name to get the next level of: e.g. "schema=objectClass"
     * @return the undecorated names of the next level (e.g. {'person', 'top', 'organisation'}
     * @throws NamingException if unable to read attributes from schema
     */
    public ArrayList<String> listEntryNames(String entryName)
            throws NamingException
    {
        if (rawSchemaAttributes == null)
            return new ArrayList<String>() ;

        entryName = mangleEntryName(entryName);

        ArrayList<String> schemaNames;

        if (entryName == null || entryName.length() == 0 || entryName.equals("cn=schema") || entryName.equals(schemaRoot))  // The 'root node', i.e. the top of the schema tree - returns things like
        {                                                 // 'objectClasses', 'ldapSyntaxes', 'attributeTypes'
            schemaNames = new ArrayList<String>(10);
            Enumeration schemaTopLevelNames = rawSchemaAttributes.getIDs();
            while (schemaTopLevelNames.hasMoreElements())
            {
                String name = (String) schemaTopLevelNames.nextElement();
                if (!schemaNames.contains(name))             //TE: don't add duplicates
                    schemaNames.add(name);
            }
        }
        else if (entryName.indexOf(',') == -1 && entryName.indexOf('/') == -1) // the first layer - returns things like
        {                                                                    // 'person', 'orgunit', 'newPilotPerson' etc...
            schemaNames = new ArrayList<String>(1000);
            if (entryName.indexOf('=') > 0)
                entryName = entryName.substring(entryName.indexOf('=') + 1);
            Attribute rawSyntaxAttribute = rawSchemaAttributes.get(entryName);   // entryName might be 'attributeTypes'
            if (rawSyntaxAttribute == null)
                throw new NamingException("unable to list syntaxes of type '" + entryName + "'");

            Enumeration values = rawSyntaxAttribute.getAll();
            String[] names;
            while (values.hasMoreElements())
            {
                names = getNames((String) values.nextElement());
                for (String name : names)
                {
                    if (!schemaNames.contains(name))     //TE: don't add duplicates
                        schemaNames.add(name);
                }
            }
        }
        else // double element, e.g. objectClass/person -> never has children.
        {
            schemaNames = new ArrayList<String>(0);
        }

        return schemaNames;
    }


    // note kind-a-sad attempt to make this method fastish.
    private int name_pos, bracket_pos, quote_pos, last_pos, pos;  // pointers for string parsing.

    /**
     * This strips the OID from a schema attribute description string.  The OID
     * is assumed to be the first element after an optional '(' character.
     *
     * @param ldapSchemaDescription
     * @return the OID string ('1.2.3.4' etc.) - or '0' if ldapSchemaDescription is null or unknown
     */
    // package visibility for testing

    public final String getOID(String ldapSchemaDescription)
    {
        if (ldapSchemaDescription == null)
            return "0";  // error.

        int start = 0;

        if (ldapSchemaDescription.charAt(0) == '(')
            start++;

        while (ldapSchemaDescription.charAt(start) == ' ')  // technically could be any whitespace, but practically it is only spaces... (I hope)
            start++;

        try
        {
            int endpos = ldapSchemaDescription.indexOf(' ', start);
            if (endpos == -1)
                endpos = ldapSchemaDescription.indexOf(')', start);
            if (endpos == -1)
                endpos = ldapSchemaDescription.length();

            String ret = ldapSchemaDescription.substring(start, endpos);
            return ret;
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "can't parse '" + ldapSchemaDescription + "'");
            e.printStackTrace();
            return "0";
        }
    }

    /**
     * parse strings that may be of the form either:
     * ????????????????/  NAME 'myname' ???????????
     * ????????????????/  NAME ('firstname', 'secondname', 'thirdname') ???????????
     *
     * @return the single name, or the first of the array of names, as a string.
     */

    // IMP note - for speed, this is implemented separately from getNames()

    public final String getFirstName(String ldapSchemaDescription)
    {
        name_pos = ldapSchemaDescription.indexOf("NAME");
        if (name_pos == -1)
            name_pos = ldapSchemaDescription.indexOf("DESC");  // for ldapSyntaxes entries
        if (name_pos == -1)  // fall back - should never happen; try to return OID
        {
            if (ldapSchemaDescription.startsWith("{"))
                ldapSchemaDescription = ldapSchemaDescription.substring(1).trim();
            pos = ldapSchemaDescription.indexOf(' ');
            if (pos == -1)
            {
                log.log(Level.WARNING, "unable to get name from " + ldapSchemaDescription);
                return "syntax_error";
            }
            return ldapSchemaDescription.substring(0, pos).trim();
        }
        quote_pos = ldapSchemaDescription.indexOf('\'', name_pos);
        quote_pos++;
        last_pos = ldapSchemaDescription.indexOf('\'', quote_pos);
        if (quote_pos != 0 && last_pos != -1)
            return ldapSchemaDescription.substring(quote_pos, last_pos);
        else
        {
            log.log(Level.WARNING, "unable to parse " + ldapSchemaDescription);
            return "syntax_error";
        }

    }

    /**
     * parse strings that may be of the form either:
     * ????????????????/  NAME 'myname' ???????????
     * ????????????????/  NAME ('firstname', 'secondname', 'thirdname') ???????????
     *
     * @return the Name or array of names, as an array of strings 1 or more elements long.
     */
    // package visibility for testing
    // TODO: rewrite using regexps...

    public final String[] getNames(String ldapSyntaxDescription)
    {
        try
        {
            name_pos = ldapSyntaxDescription.indexOf("NAME");
            if (name_pos == -1)
                name_pos = ldapSyntaxDescription.indexOf("DESC");  // for ldapSyntaxes entries
            if (name_pos == -1)  // fall back - should never happen; try to return OID
            {
                if (ldapSyntaxDescription.startsWith("{"))
                    ldapSyntaxDescription = ldapSyntaxDescription.substring(1).trim();
                return new String[]{ldapSyntaxDescription.substring(0, ldapSyntaxDescription.indexOf(' ')).trim()};
            }

            bracket_pos = ldapSyntaxDescription.indexOf('(', name_pos);
            quote_pos = ldapSyntaxDescription.indexOf('\'', name_pos);

            if (bracket_pos != -1 && bracket_pos < quote_pos)  // multiple names...
            {
                bracket_pos = ldapSyntaxDescription.indexOf(')', bracket_pos);  // get end bracket pos
                ArrayList newList = new ArrayList(5);
                while (quote_pos < bracket_pos && quote_pos != -1)  // iterate through grabbing 'quoted' substrings until we get to the end of the bracketed expression
                {
                    int start = ++quote_pos;
                    quote_pos = ldapSyntaxDescription.indexOf('\'', quote_pos);

                    String temp = ldapSyntaxDescription.substring(start, quote_pos);
                    newList.add(temp);

                    quote_pos++;
                    quote_pos = ldapSyntaxDescription.indexOf('\'', quote_pos);
                }
                return (String[]) newList.toArray(new String[]{});
            }
            else  // return the single name
            {
                quote_pos++;
                int next_quote = ldapSyntaxDescription.indexOf('\'', quote_pos);
                String temp = ldapSyntaxDescription.substring(quote_pos, next_quote);
                return new String[]{temp};
            }
        }
        catch (StringIndexOutOfBoundsException e)
        {
            log.log(Level.WARNING, "unable to parse line: " + ldapSyntaxDescription, e);
            return new String[]{"syntax_error"};
        }
    }


    /**
     *    Takes a DXAttributes set representing attribute schema defs,
     *    and translates the oids into human friendly strings...
     */
/*
    protected Attributes addAttributeInfo(Attributes attdefs)
    {
        try
        {
            Attribute syntax = attdefs.get("SYNTAX");        // get syntax attribute
            String oid = syntax.get().toString();            // convert oid value to string

            if (oid.indexOf('{') > -1)
                oid = oid.substring(0, oid.indexOf('{'));

            String syntaxdesc = (String) oids.get(oid);   // look up description for oid

            attdefs.put("(SYNTAX-DESC)", syntaxdesc);        // stick in synthetic attribute
            return attdefs;
        }
        catch (NamingException e)      { return attdefs; }
        catch (NullPointerException e) { return attdefs; }
    }
*/

    /**
     * This returns a sorted list of all known object classes, as read from the schema
     *
     * @return an array list of strings
     * @throws NamingException
     */
    public ArrayList getKnownObjectClasses()
            throws NamingException
    {
        if (fullObjectClassArray == null)
        {
            ArrayList temp = listEntryNames("schema=objectClasses,cn=schema");
            if (temp == null)
                throw new NamingException("unable to read list of object classes from schema");
            String[] OCs = (String[]) temp.toArray(new String[temp.size()]);

            //TODO: is there a cleaner way of sorting an arraylist than dumping it to an array, sorting, and reading it back?

            Arrays.sort(OCs, new Comparator()
            {
                public int compare(Object a, Object b)
                {
                    return ((String) a).compareToIgnoreCase((String) b);
                }

                public boolean equals(Object a, Object b)
                {
                    return ((String) a).equalsIgnoreCase((String) b);
                }
            });
            int size = OCs.length;
            fullObjectClassArray = new ArrayList(size);
            for (int i = 0; i < size; i++)
                fullObjectClassArray.add(i, OCs[i]);
        }

        return fullObjectClassArray;
    }

    public ArrayList<String> getKnownAttributeNames()
            throws NamingException
    {
            ArrayList attributeNames = listEntryNames("schema=AttributeDefinition,cn=schema");
            if(attributeNames==null)		//TE: check for no schema publishing i.e. LDAP V2.
                return null;

            String[] temp = (String[]) attributeNames.toArray(new String[] {});
            Arrays.sort(temp, new IgnoreCaseStringComparator());

            ArrayList<String> attList = new ArrayList(Arrays.asList(temp));

            return attList;
    }


    public ArrayList<String> getKnownLowerCaseAttributeNames()
            throws NamingException
    {
            ArrayList attributeNames = listEntryNames("schema=AttributeDefinition,cn=schema");
            if(attributeNames==null)		//TE: check for no schema publishing i.e. LDAP V2.
                return null;

            String[] temp = (String[]) attributeNames.toArray(new String[] {});
            Arrays.sort(temp, new IgnoreCaseStringComparator());

            ArrayList<String> attList = new ArrayList(temp.length);

            for (String att:temp)
                attList.add(att.toLowerCase());

            return attList;
    }

    /**
     * This returns a sorted list of all known attribute names, as read from the schema
     *
     * @return an array list of strings
     * @throws NamingException
     */
    public ArrayList attributeNames()
            throws NamingException
    {
        if (fullAttributeNameArray == null)
        {
            ArrayList temp = listEntryNames("schema=attributeTypes,cn=schema");
            if (temp == null)
                throw new NamingException("unable to read list of attribute types from schema");
            String[] ATs = (String[]) temp.toArray(new String[temp.size()]);
            Arrays.sort(ATs, new Comparator()
            {
                public int compare(Object a, Object b)
                {
                    return ((String) a).compareToIgnoreCase((String) b);
                }

                public boolean equals(Object a, Object b)
                {
                    return ((String) a).equalsIgnoreCase((String) b);
                }
            });
            int size = ATs.length;
            fullAttributeNameArray = new ArrayList(size);
            for (int i = 0; i < size; i++)
                fullAttributeNameArray.add(i, ATs[i]);
        }

        return fullAttributeNameArray;
    }

    /**
     * Gets a list of the object classes most likely
     * to be used for the next Level of the DN...
     *
     * @param dn the dn of the parent to determine likely
     *           child object classes for
     * @return list of recommended object classes...
     */

    public ArrayList getRecommendedObjectClasses(String dn)
    {
        try
        {
            if ((dn != null) && ctx != null)
            {
                Attributes atts = ctx.getAttributes(dn);

                if (atts == null)
                {
                    log.log(Level.WARNING, "error reading object classes for " + dn);
                }
                else
                {
                    Attribute objectClasses = atts.get("objectclass");
                    if (objectClasses == null) // hack.  Shouldn't have to do this.  Bad Server! Spank!
                        objectClasses = atts.get("objectClass");
                    if (objectClasses == null) // still!  - try 'oc'
                        objectClasses = atts.get("oc");

                    if (objectClasses == null) // aargh! Give up.
                    {
                        log.log(Level.WARNING, "unable to recognize object classes for " + dn);
                    }
                    else
                    {
                        NamingEnumeration names = objectClasses.getAll();
                        if (names == null)
                            log.log(Level.WARNING, "object class has no attributes!");

                        ArrayList returnArray = new ArrayList(10);
                        while (names.hasMore())
                            returnArray.add(names.next());

                        return returnArray;
                    }
                }
            }
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "error reading object classes for " + dn + "\n  internal error III:  ", e);
        }
        return null;
    }


    /**
     * Gets a list of all attribute definitions that have a 'binary' syntax
     * (currently defined as: SYNTAX = 1.3.6.1.4.1.1466.115.121.1.{4,5,8,9,10,28,40}
     *
     * Note the usual confusion between 'ASN.1' and 'byte array' - this is actually setting non-string 'byte array' atts.
     *
     * @return list of space separated attribute names as a string array - used to set ctx.addToEnvironment("java.naming.ldap.attributes.binary", -String- );
     */
// Performance note - this might be sped up a tad if required...
    public String getNewBinaryAttributes()
    {
        if (rawSchemaAttributes == null)
            return "";

        // Compile and use regular expression
        String patternStr = "SYNTAX\\s+(.*?)\\s+";
        Pattern pattern = Pattern.compile(patternStr);

        try
        {
            StringBuffer binaryAttributeList = new StringBuffer(1000);
            Attribute rawSyntaxAttribute = getAttributeTypes();
            if (rawSyntaxAttribute == null)
                return "";

            NamingEnumeration values = rawSyntaxAttribute.getAll();
            while (values.hasMore())
            {
                //TODO - rewrite to make sure syntax matches are exact, and don't accidently pick up telephone syntaxes etc...

                String attributeDescription = (String) values.next();  // something like "( 1.3.6.1.4.1.453.7.6.2.1 NAME 'mhsX400Domain' SYNTAX 1.3.6.1.4.1.1466.115.121.1.5 )"

                Matcher matcher = pattern.matcher(attributeDescription);
                boolean matchFound = matcher.find();

                if (matchFound) 
                {
                    String attSyntax = matcher.group(1);

                    if (attSyntax.startsWith("1.3.6.1.4.1.1466.115.121.1."))
                    {
                        String attOIDSuffix = attSyntax.substring(27);

                        if ((attOIDSuffix.equals("4"))  ||  // audio
                            (attOIDSuffix.equals("5"))  ||  // ASN.1
                            (attOIDSuffix.equals("8"))  ||  // certificate
                            (attOIDSuffix.equals("9"))  ||  // certificate list
                            (attOIDSuffix.equals("10")) ||  // certificate pair
                            (attOIDSuffix.equals("28")) ||  // jpeg
                            (attOIDSuffix.equals("40")))    // octet string
                            {
                                String[] names = getNames(attributeDescription);
                                for (int i = 0; i < names.length; i++)
                                {
                                    binaryAttributeList.append(names[i]);
                                    binaryAttributeList.append(' ');
                                }
                            }
                    }
                }
            }
            return binaryAttributeList.toString();
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "unable to get binary attributes from schema", e);
            return "";
        }

    }


     /**
     * Convenience method to get the objectClasses Attribute, which represents the syntax of
     * attributeTypes.
     *
     * @return an Attribute with multiple values, each representing a particular attributeType
     *         represented as complex string of values that must be parsed: e.g. the surname value might be:
     *         2.5.4.4 NAME ( 'sn' 'surname' ) SYNTAX 1.3.6.1.4.1.1466.115.121.1.15
     */

    public Attribute getAttributeTypes()
    {
        return rawSchemaAttributes.get("attributeTypes");
    }

    /**
     * Finds the syntax of the corresponding attribute
     *
     * @param attID the undecorated attribute name - e.g. 'commonName'
     * @return returns the attribute syntax, or null if not found.
     */
    public String getAttributeSyntax(String attID)
    {
        if(attID == null)
            return null;

        if (attID.indexOf(';') > 0)
            attID = attID.substring(0, attID.indexOf(';'));		//TE: for example: userCertificate;binary.

        return schemaLookup("schema=" + attID + ",schema=attributeTypes", "SYNTAX");
    }

    /**
     * Looks up a particular value in a particular schema attribute
     *
     * @param entryName       the entry to lookup; e.g. 'schema=person, schema=objectClass'
     * @param schemaAttribute the actual field to look up, e.g. "DESC"
     * @return the looked up value (or the first one found, if multiple)
     */

    public String schemaLookup(String entryName, String schemaAttribute)
    {
        entryName = mangleEntryName(entryName);

        try
        {
            Attributes schemaAtts = getAttributes(entryName);
            Attribute schemaAtt = schemaAtts.get(schemaAttribute);
            String att = (String) schemaAtt.get();
            return att;

        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "unable to get value for " + entryName + " value: " + schemaAttribute, e);
        }
        catch (NullPointerException e2)
        {
            if ("DESC".equals(schemaAttribute) == false)  // ignore frequent error encountered searching for option 'DESC' schema paramater
                log.log(Level.WARNING, "unable to read any schema entry for " + entryName + "and attribute: " + schemaAttribute, e2);
        }
        catch (NoSuchElementException e)
        {
            if ("DESC".equals(schemaAttribute) == false)  // ignore frequent error encountered searching for option 'DESC' schema paramater
                log.log(Level.WARNING, "unable to read any schema entry for " + entryName + "and attribute: " + schemaAttribute, e);
        }
        return null;
    }

    public String getNameOfObjectClassAttribute()
    {
        return schemaLookup("schema=objectClass,schema=attributeTypes", "NAME");
    }


    /**
     * Tries to determine if the attribute is a SINGLE-VALUE attribute.
     * The rawSchemaAttributes Attributes object represents each attribute
     * similar to
     * <br><br>
     * ( 1.3.6.1.4.1.3327.77.4.1.2 NAME 'uNSPSCTitle' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SINGLE-VALUE )
     * <br><br>
     * This method gets each value (see above string) in the attribute, then each of their names in turn
     * until the name of the attribute we are seeking is found.  Then checks if 'SINGLE-VALUE'
     * is present in that value.
     *
     * @param name the name of the attribute, for example uNSPCSTitle.
     * @return true if the attribute is a SINGLE-VALUE attribute, false otherwise.
     */

    public boolean isAttributeSingleValued(String name)
    {/* TE */

        //TODO: there has got to be a more efficient way of doing this check... this is very long winded...
        if (rawSchemaAttributes == null)
            return false;

        try
        {
            Attribute attributeTypes = getAttributeTypes();
            NamingEnumeration en = attributeTypes.getAll();

            while (en.hasMore())
            {
                String attr = (String) en.next();
                String[] attrName = getNames(attr);

                for (int i = 0; i < attrName.length; i++)
                    if (attrName[i].equals(name) && (attr.indexOf("SINGLE-VALUE") >= 0))
                        return true;
            }
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "Unable to determine if attribute '" + name + "' is SINGLE-VALUE." + e);
        }
        return false;
    }

    /**
     * This Comparator compares two strings, ignoring case.
     *
     * @author Trudi.
     */

    public static class IgnoreCaseStringComparator implements Comparator
    {

        /**
         * This Comparator compares two strings, ignoring case.
         *
         * @param o1 one of the two items to be compared.
         * @param o2 the other of the items to be compared.
         * @return the result of the compare (0 if o1 & o2 are equal, -1 if o1 < o2, 1 if o1 > o2).
         *         NOTE: if o1 is null and o2 is not null, 1 is returned. If o2 is null and o1 is not null, -1 is returned.
         *         If both o1 and o2 are null, 1 is returned. If an error occurs trying to cast either o1 or o2, 0 is returned.
         */

        public int compare(Object o1, Object o2)
        {
            if (o1 == null || o2 == null) return (o1==null)?1:-1;

            return (o1.toString().toLowerCase()).compareTo(o2.toString().toLowerCase());
        }
    }

}
