/**
 * Created by IntelliJ IDEA.
 * User: betch01
 * Date: Dec 3, 2002
 * Time: 12:09:51 PM
 * To change this template use Options | File Templates.
 */
package com.ca.commons.jndi;

import junit.framework.*;

import javax.naming.directory.*;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import java.util.*;

public class SchemaOpsTest extends TestCase
{
    private static boolean testWithDirectory = false;

    private static SchemaOps virtualOps = null;
    private static SchemaOps directorySchemaOps = null;

    public void testGetOID()
    {
        assertEquals("0.9.2342.19200300.100.4.4", virtualOps.getOID(syntaxValue1));
        assertEquals("2.5.4.0", virtualOps.getOID(attributeTypes[0]));
        assertEquals("1.3.6.1.4.1.1466.115.121.1.31", virtualOps.getOID("( 1.3.6.1.4.1.1466.115.121.1.31 ) "));
        assertEquals("1.3.6.1.4.1.1466.115.121.1.32", virtualOps.getOID("(1.3.6.1.4.1.1466.115.121.1.32  "));
        assertEquals("1.3.6.1.4.1.1466.115.121.1.33", virtualOps.getOID("(1.3.6.1.4.1.1466.115.121.1.33)"));
        assertEquals("1.3.6.1.4.1.1466.115.121.1.34", virtualOps.getOID("1.3.6.1.4.1.1466.115.121.1.34"));
    }

    private static String syntaxValue1 = "( 0.9.2342.19200300.100.4.4 NAME 'newPilotPerson' SUP ( person ) " +
            "STRUCTURAL MAY ( uid $ mail $ drink $ roomNumber $ userClass $ homePhone $ homePostalAddress " +
            " $ secretary $ personalTitle $ preferredDeliveryMethod $ businessCategory $ janetMailbox " +
            " $ otherMailbox $ mobile $ pager $ organizationalStatus $ mailPreferenceOption $ personalSignature ) ) ";

    private static String syntaxValue2 = "( 0.9.2342.19200300.100.4.4 NAME ( 'newPilotPerson' 'fred' 'neuerFleigerMensh' ) SUP ( person ) " +
            "STRUCTURAL MAY ( uid $ mail $ drink $ roomNumber $ userClass $ homePhone $ homePostalAddress " +
            " $ secretary $ personalTitle $ preferredDeliveryMethod $ businessCategory $ janetMailbox " +
            " $ otherMailbox $ mobile $ pager $ organizationalStatus $ mailPreferenceOption $ personalSignature ) ) ";

    /*
     *  Test case where there are no spaces between things...
     */
    private static String syntaxValue3 = "(9.9.9 NAME 'test' SUP (top) STRUCTURAL MAY " +
            "(uid$mail$drink$roomNumber$userClass$homePhone$ " +
            "homePostalAddress $ secretary $ personalTitle $ preferredDeliveryMethod $ " +
            "businessCategory $ janetMailbox $ otherMailbox $ mobile $ pager $ " +
            "organizationalStatus $ mailPreferenceOption $ personalSignature) )";

    private static String[] syntaxValue3Mays =
            {"uid", "mail", "drink", "roomNumber", "userClass", "homePhone",
            "homePostalAddress", "secretary", "personalTitle", "preferredDeliveryMethod",
            "businessCategory", "janetMailbox", "otherMailbox", "mobile", "pager",
            "organizationalStatus", "mailPreferenceOption", "personalSignature"};


    private static String[] attributeTypes = {
            "( 2.5.4.0 NAME ( 'objectClass' 'oc' 'objectClass' ) SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 )",
            "( 2.5.4.1 NAME ( 'aliasedObjectName' 'aliasedObjectName' ) SYNTAX 1.3.6.1.4.1.1466.115.121.1.12 )",
            "( 2.5.4.2 NAME 'knowledgeInformation' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )",
            "( 2.5.4.3 NAME ( 'cn' 'commonName' ) SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )",
            "( 2.5.4.4 NAME ( 'sn' 'surname' ) SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )",
            "( 2.5.4.5 NAME 'serialNumber' SYNTAX 1.3.6.1.4.1.1466.115.121.1.44 )",
            "( 2.5.4.6 NAME ( 'c' 'countryName' ) SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )",
            "( 2.5.4.7 NAME ( 'l' 'localityName' ) SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )",
            "( 2.5.4.8 NAME ( 'st' 'stateOrProvinceName' ) SYNTAX",
            "( 1.3.6.1.4.1.453.7.3.2.4 NAME 'mhsBadAddressSearchPoint' SYNTAX 1.3.6.1.4.1.1466.115.121.1.12 )",
            "( 1.3.6.1.4.1.453.7.3.2.5 NAME 'mhsBadAddressSearchAttributes' SYNTAX 1.3.6.1.4.1.1466.115.121.1.5 )",
            "( 1.3.6.1.4.1.453.7.3.2.6 NAME 'mhsBodyPartConversionService' SYNTAX 1.3.6.1.4.1.1466.115.121.1.5 )",
            "( 1.3.6.1.4.1.3327.6.1000.1.13 NAME 'cert_authCertSerialNumber' SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )",
            "( 1.3.6.1.4.1.3327.77.4.1.2 NAME 'uNSPSCTitle' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SINGLE-VALUE )" };

    // note sorted order, no redundancies
    private static String[] sortedAttributeNames = {"aliasedObjectName", "c", "cert_authCertSerialNumber",
                                                    "cn","commonName","countryName","knowledgeInformation",
                                                    "l","localityName","mhsBadAddressSearchAttributes","mhsBadAddressSearchPoint",
                                                    "mhsBodyPartConversionService","objectClass","oc",
                                                    "serialNumber","sn","st",
                                                    "stateOrProvinceName","surname", "uNSPSCTitle"};
/*
    private static String[] sortedAttributeNames = {"aliasedObjectName", "aliasedObjectName", "c", "cert_authCertSerialNumber",
                                                    "cn","commonName","countryName","knowledgeInformation",
                                                    "l","localityName","mhsBadAddressSearchAttributes","mhsBadAddressSearchPoint",
                                                    "mhsBodyPartConversionService","objectClass","objectClass","oc",
                                                    "serialNumber","sn","st",
                                                    "stateOrProvinceName","surname"};
*/


    private static String binaryStrings = "mhsBadAddressSearchAttributes mhsBodyPartConversionService cert_authCertSerialNumber ";

    private static String[] ldapSyntaxes = {
            "( 1.3.6.1.4.1.1466.115.121.1.4 DESC 'Audio' )",
            "( 1.3.6.1.4.1.1466.115.121.1.5 DESC 'Binary' )",
            "( 1.3.6.1.4.1.1466.115.121.1.7 DESC 'Boolean' )",
            "( 1.3.6.1.4.1.1466.115.121.1.8 DESC 'Certificate' )",
            "( 1.3.6.1.4.1.1466.115.121.1.9 DESC 'Certificate List' )" };

    private static String[] nameForms = {
            "( 1.3.6.1.4.1.3327.7.1 NAME 'country-top-NF' OC country MUST ( c ) )",
            "( 1.3.6.1.4.1.3327.7.2 NAME 'o-top-NF' OC organization MUST ( o ) )",
            "( 1.3.6.1.4.1.3327.7.3 NAME 'o-country-NF' OC organization MUST ( o ) MAY ( dnQualifier ) )" };

    private static String[] objectClasses = {
            "( 2.5.6.0 NAME 'top' ABSTRACT MUST ( objectClass ) )",
            "( 2.5.6.1 NAME 'alias' SUP ( top ) STRUCTURAL MUST ( aliasedObjectName ) )",
            "( 2.5.6.2 NAME 'country' SUP ( top ) STRUCTURAL MUST ( c ) MAY ( description $ searchGuide ) )",
            "( 2.5.6.3 NAME 'locality' SUP ( top ) STRUCTURAL MAY ( description $ searchGuide $ l $ st $ street $ seeAlso ) )",
            "( 1.1.1.1.1.1 NAME 'xxxPerson' DESC 'Person im EEA GDS-System' AUXILIARY MAY ( eeaBadgeNumber $ eeaPersonalHash ) X-NDS_NOT_CONTAINER '1' )"};



    private static String[] objectClassesNames = {"top","alias","country","locality", "xxxPerson" }; // note unsorted order
    private static String[] sortedObjectClassesNames = {"alias","country","locality", "top", "xxxPerson"}; // note sorted order

    private static String[] topLevelNames;

    private static BasicAttributes virtualSchema;

    private static String[] syntaxNames2 = new String[]{"newPilotPerson", "fred", "neuerFleigerMensh"};

    public SchemaOpsTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(SchemaOpsTest.class);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    private BasicAttribute getAttribute(String[] values, String name)
    {
        BasicAttribute retAtt = new BasicAttribute(name);
        for (int i=0; i<values.length; i++)
            retAtt.add(values[i]);
        return retAtt;
    }

    protected void setUp()
    {
        // virtualSchema is used to run basic tests in the absence of a live directory
        System.out.println("running stand alone tests");
        virtualSchema = new BasicAttributes();
        topLevelNames = new String[] {"attributeTypes","ldapSyntaxes","nameForms","objectClasses"};
        virtualSchema.put(getAttribute(attributeTypes, topLevelNames[0]));
        virtualSchema.put(getAttribute(ldapSyntaxes, topLevelNames[1]));
        virtualSchema.put(getAttribute(nameForms, topLevelNames[2]));
        virtualSchema.put(getAttribute(objectClasses, topLevelNames[3]));
        virtualOps = new SchemaOps(virtualSchema);

    }

    private boolean initDirectory()
    {
        if (testWithDirectory)
        {
            System.out.println("running directory link tests");
            LdapContext ctx = null;
            try
            {
                ConnectionData cdata = new ConnectionData();
                cdata.setURL("ldap://betch01:19389");
                ctx = BasicOps.openContext(cdata);

                //ctx = BasicOps.openContext("ldap://betch01:19389");
            }
            catch (NamingException e)
            {
                fail("exception opening directory connection: " + e);
            }

            if (ctx == null)
            {
                fail("Unable to open directory connection to test Schema Ops");
            }

            try
            {
                directorySchemaOps = new SchemaOps(ctx);
            }
            catch (NamingException e)
            {
                fail("unable to initialise directory connection " + e);
            }

            return true;
        }
        else
            return false;
    }

    private boolean checkFixtures()
    {

        if (virtualOps == null)
        {
            System.out.println("skipping test - no schema ops object");
            return false;
        }

        return true;
    }

    public void testStuff() throws NamingException
    {
        if (!checkFixtures())
            return;

        //virtualOps.debugPrint("");
    }


    public void testValueParser()
        throws NamingException
    {
        BasicAttributes atts = virtualOps.getAttributesFromSchemaValue(syntaxValue1);
        assertNotNull(atts.get("OID"));
        assertNotNull(atts.get("NAME"));
        assertNotNull(atts.get("SUP"));
        assertNotNull(atts.get("STRUCTURAL"));
        assertNotNull(atts.get("MAY"));


        Attributes bloop = virtualOps.getAttributesFromSchemaValue(syntaxValue3);
        debugPrintAttribute("WIERD BRACKET THING", bloop);

        Enumeration mayValues = bloop.get("MAY").getAll();

        for (int i=0; mayValues.hasMoreElements(); i++)
            assertEquals(syntaxValue3Mays[i], mayValues.nextElement().toString());
    }

    public void testNameParser()
        throws NamingException
    {
        assertEquals("newPilotPerson", virtualOps.getNames(syntaxValue1)[0]);

        String[] names = virtualOps.getNames(syntaxValue2);
        assertEquals(names.length, syntaxNames2.length);
        for (int i = 0; i < syntaxNames2.length; i++)
        {
            assertEquals(names[i], syntaxNames2[i]);
        }

        //syntaxValue3Mays
        assertEquals("newPilotPerson", virtualOps.getFirstName(syntaxValue1));

        assertEquals("newPilotPerson", virtualOps.getFirstName(syntaxValue2));

    }

    public void testObjectClasses()
        throws NamingException
    {
        ArrayList list = virtualOps.getKnownObjectClasses();
        assertNotNull(list);
        assertTrue(list.size() == sortedObjectClassesNames.length);
        for (int i=0; i<list.size(); i++)
            assertTrue(" testing equality between ("+i+")" + list.get(i) + " and " + objectClassesNames[i],list.get(i).equals(sortedObjectClassesNames[i]));
    }

    public void testAttributeNames()
        throws NamingException
    {
        ArrayList list = virtualOps.attributeNames();
        assertNotNull("read attribute list", list);
        assertTrue("read list size: " + list.size() + " should equal stored list: " + sortedAttributeNames.length , list.size() == sortedAttributeNames.length);
        for (int i=0; i<list.size(); i++)
            assertTrue(" testing equality between ("+i+")" + list.get(i) + " and " + sortedAttributeNames[i],list.get(i).equals(sortedAttributeNames[i]));
    }


    public void testGetNewBinaryAttributes()
    {
        String bloop = virtualOps.getNewBinaryAttributes();
        assertEquals(bloop, binaryStrings);
    }

    public void testGetAttributeSyntax()
    {
        assertEquals("1.3.6.1.4.1.1466.115.121.1.38", virtualOps.getAttributeSyntax("objectClass"));
        assertEquals("1.3.6.1.4.1.1466.115.121.1.38", virtualOps.getAttributeSyntax("oc"));
        assertEquals("1.3.6.1.4.1.1466.115.121.1.12", virtualOps.getAttributeSyntax("aliasedObjectName"));
        assertEquals("1.3.6.1.4.1.1466.115.121.1.15", virtualOps.getAttributeSyntax("knowledgeInformation"));
    }

    public void testDirGetAttributeSyntax()
    {
        if (initDirectory())
        {
            assertEquals("1.3.6.1.4.1.1466.115.121.1.38", directorySchemaOps.getAttributeSyntax("objectClass"));
            assertEquals("1.3.6.1.4.1.1466.115.121.1.38", directorySchemaOps.getAttributeSyntax("oc"));
            assertEquals("1.3.6.1.4.1.1466.115.121.1.12", directorySchemaOps.getAttributeSyntax("aliasedObjectName"));
            assertEquals("1.3.6.1.4.1.1466.115.121.1.15", directorySchemaOps.getAttributeSyntax("knowledgeInformation"));
        }
    }

    public void testDirSchemaLookup()
    {
        if (initDirectory())
        {
            assertEquals("c",directorySchemaOps.schemaLookup("schema=countryName,schema=attributeTypes", "NAME"));
            assertEquals("Certificate",directorySchemaOps.schemaLookup("schema=Certificate,schema=ldapSyntaxes", "DESC"));
        }
    }


    public void testSchemaLookup()
    {
        assertEquals("c",virtualOps.schemaLookup("schema=countryName,schema=attributeTypes", "NAME"));
        assertEquals("Certificate",virtualOps.schemaLookup("schema=Certificate,schema=ldapSyntaxes", "DESC"));
        assertEquals("objectClass", virtualOps.schemaLookup("AttributeDefinition/objectClass", "NAME"));
        assertEquals("objectClass", virtualOps.schemaLookup("attributeTypes/objectClass", "NAME"));
    }

//.schemaLookup("AttributeDefinition/objectClass", "NAME");

    public void testSchemaList()
        throws NamingException
    {
        ArrayList nextLevel = virtualOps.listEntryNames("schema=objectClasses");
        for (int i=0; i<nextLevel.size(); i++)
            assertEquals(objectClassesNames[i], nextLevel.get(i) );

        nextLevel = virtualOps.listEntryNames("");
        for (int i=0; i<topLevelNames.length; i++)
            nextLevel.contains(topLevelNames[i]);

        nextLevel = virtualOps.listEntryNames("cn=schema");
        for (int i=0; i<topLevelNames.length; i++)
            nextLevel.contains(topLevelNames[i]);
    }

    public void testGetNameOfObjectClassAttribute()
    {
        assertEquals("objectClass", virtualOps.getNameOfObjectClassAttribute());
    }

    public void testTranslateOID()
    {
        assertEquals("knowledgeInformation", virtualOps.translateOID("2.5.4.2"));
        assertEquals("mhsBadAddressSearchPoint", virtualOps.translateOID("1.3.6.1.4.1.453.7.3.2.4"));
        assertEquals("o-top-NF", virtualOps.translateOID("1.3.6.1.4.1.3327.7.2"));
    }

    public void testMultipleNamedAttributes()
        throws NamingException
    {
        Attributes cn1 = virtualOps.getAttributesFromSchemaName("schema=commonName,schema=attributeTypes");
        Attributes cn2 = virtualOps.getAttributesFromSchemaName("schema=cn,schema=attributeTypes");

        assertNotNull(cn1);
        assertNotNull(cn2);
        assertEquals(cn1.get("OID"), cn2.get("OID"));

    }

    // test parsing of:
    //"( 1.1.1.1.1.1 NAME 'xxxPerson' DESC 'Person im EEA GDS-System' AUXILIARY MAY ( eeaBadgeNumber $ eeaPersonalHash ) X-NDS_NOT_CONTAINER '1' )"};
    //and
    //"( 2.5.6.2 NAME 'country' SUP ( top ) STRUCTURAL MUST ( c ) MAY ( description $ searchGuide ) )",

    public void testObjectClassAttributeParsing()
        throws NamingException
    {
        Attributes oc = virtualOps.getAttributes("schema=xxxPerson,schema=objectClasses");

        //debugPrintAttribute("XXX Person", oc);

        assertEquals(oc.get("NAME").get(), "xxxPerson");
        assertEquals(oc.get("DESC").get(), "Person im EEA GDS-System");

        oc = virtualOps.getAttributes("schema=country,schema=objectClasses");
        assertEquals(oc.get("NAME").get(), "country");
        assertEquals(oc.get("OID").get(), "2.5.6.2");


    }

    public void testObjectClassAttributeParsingWithMixedCases()
        throws NamingException
    {
        Attributes oc = virtualOps.getAttributes("schema=xxxperson,schema=objectClasses");

        //debugPrintAttribute("XXX Person", oc);
        assertNotNull("unable to read mixed case schema object", oc);
        assertNotNull("unable to read name of mixed case schema object", oc.get("NAME"));

        assertEquals(oc.get("NAME").get(), "xxxPerson");
        assertEquals(oc.get("DESC").get(), "Person im EEA GDS-System");

        oc = virtualOps.getAttributes("schema=country,schema=objectClasses");
        assertEquals(oc.get("NAME").get(), "country");
        assertEquals(oc.get("OID").get(), "2.5.6.2");


    }

    public void debugPrintAttribute(String msg, Attributes atts)
        throws NamingException
    {
        System.out.println(msg);
        Enumeration bloop = atts.getIDs();
        while (bloop.hasMoreElements())
        {
            String id = bloop.nextElement().toString();
            Enumeration vals = atts.get(id).getAll();
            while (vals.hasMoreElements())
                System.out.println("  " + id + " : " + vals.nextElement().toString());
        }

    }

    public void testGetTypeName()
        throws NamingException
    {
        assertEquals("objectClasses", virtualOps.getTypeName("schema=objectClasses,cn=schema"));
        assertEquals("attributeTypes", virtualOps.getTypeName("schema=cn,schema=attributeTypes,cn=schema"));
        assertEquals("attributeTypes", virtualOps.getTypeName("schema=cn,schema=attributeTypes"));
    }

    public void testGetSpecificName()
        throws NamingException
    {
        assertEquals("cn", virtualOps.getSpecificName("schema=cn,schema=attributeTypes,cn=schema"));
        assertEquals("cn", virtualOps.getSpecificName("schema=cn,schema=attributeTypes"));
        assertEquals("xxxPerson", virtualOps.getSpecificName("schema=xxxPerson,schema=objectClass"));
    }
    public void testMangleEntryName()
    {
        assertEquals("schema=objectClasses", virtualOps.mangleEntryName("schema=ClassDefinition,cn=schema"));
        assertEquals("schema=ldapSyntaxes", virtualOps.mangleEntryName("schema=SyntaxDefinition"));
        assertEquals("schema=cn,schema=attributeTypes", virtualOps.mangleEntryName("AttributeDefinition/cn;binary"));
    }                       //   ;binary,schema=AttributeDefinition>

    public void testIsAttributeSingleValued()
    {/* TE */
        assertTrue("Testing if uNSPSCTitle is a SINGLE-VALUE attribute.", virtualOps.isAttributeSingleValued("uNSPSCTitle"));
        assertTrue("Testing if countryName is a SINGLE-VALUE attribute.", (!virtualOps.isAttributeSingleValued("countryName")));
    }
}

