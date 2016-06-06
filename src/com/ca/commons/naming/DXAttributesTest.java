/**
 *
 *
 * Author: Chris Betts
 * Date: 5/06/2002 / 16:59:37
 */
package com.ca.commons.naming;

import junit.framework.*;
import javax.naming.directory.BasicAttribute;
import javax.naming.ldap.LdapContext;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.NamingException;

import com.ca.commons.jndi.*;

public class DXAttributesTest extends TestCase
{
    private static final BasicAttribute attribute1 = new BasicAttribute("att1", "value1");
    private static final BasicAttribute attribute2 = new BasicAttribute("att2", "value2");
    private static final BasicAttribute attribute3 = new BasicAttribute("att3", "value3");
    private static final BasicAttribute attribute4 = new BasicAttribute("att4", "value4");
    private static final BasicAttribute attribute5 = new BasicAttribute("att5", "value5");
    //private static final BasicAttribute empty1 = new BasicAttribute("att5", null);
    //private static final BasicAttribute empty2 = new BasicAttribute("att5");
    //private static final BasicAttribute empty3 = new BasicAttribute("att5", "");

    public DXAttributesTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(DXAttributesTest.class);
    }

    public static void main (String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public void testEmptyConstructor()
    {
        DXAttributes bloop = new DXAttributes();
        assertEquals(bloop.size(), 0);
    }

    public void testEquality()
    {
        try
        {
            DXAttributes empty = new DXAttributes();

            DXAttributes atts1 = new DXAttributes();
            atts1.put(attribute1);
            atts1.put(attribute2);
            atts1.put(attribute3);
            atts1.put(attribute4);

            DXAttributes atts2 = new DXAttributes();
            atts2.put(attribute4);
            atts2.put(attribute3);
            atts2.put(attribute1);
            atts2.put(attribute2);

            assertEquals("chack that DXAttributes containing the same unordered list of attribute objects are equal",
                        atts1, atts2);

            atts2.remove("att2");
            assertTrue("check that DXAttributes with different sizes aren't equal",
                        !atts1.equals(atts2));

            atts2.put(attribute5);
            assertTrue("check that DXAttributes with same sizes but different att objects aren't equal",
                        !atts1.equals(atts2));

            assertTrue("check null not equal...",
                        !empty.equals(null));

            atts2.remove("att1");
            atts2.remove("att3");
            atts2.remove("att4");
            atts2.remove("att5");
            assertEquals("check empty atts always equal...",
                         empty, atts2);

            assertTrue(DXAttributes.attributesEqual(null, null));
            assertTrue(!DXAttributes.attributesEqual(null, new DXAttributes()));
            assertTrue(!DXAttributes.attributesEqual(new DXAttributes(), null));
        }
        catch (Exception e)
        {
            System.out.println("unexpected exception in DXAttributesTest: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Tests the expand all attributes.  DXAttribute
     * has a set default schema - this uses that
     * in the test b/c
     * @throws Exception should not throw an
     * exception.
     */
    //TODO - make this a system test.  Currently it wont be run - to run remove the DONT from the method name.
    public void DONTtestExpandAllAttributes1()
        throws Exception
    {
        ConnectionData cData = new ConnectionData();
        cData.setURL("ldap://betch01:19389");
        LdapContext ctx = BasicOps.openContext(cData);
        //LdapContext ctx = BasicOps.openContext("ldap://betch01:19389");
        DXAttributes a = new DXAttributes(ctx.getAttributes("ou=Manufacturing,o=DEMOCORP,c=AU"));
        SchemaOps schemaOps = new SchemaOps(ctx);
        ctx.addToEnvironment("java.naming.ldap.attributes.binary", schemaOps.getNewBinaryAttributes());
//        DXAttributes.setDefaultSchema(schemaOps);
//        DXAttribute.setDefaultSchema(schemaOps);
        a.expandAllAttributes();
    }

    /**
     * Tests the expand all attributes.  DXAttribute
     * has a set default schema - this uses that
     * in the test b/c
     * @throws Exception should not throw an
     * exception.
     */
    //TODO - make this a system test.  Currently it wont be run - to run remove the DONT from the method name.
    public void DONTtestExpandAllAttributes2()
        throws Exception
    {
        ConnectionData cData = new ConnectionData();
        cData.setURL("ldap://betch01:19389");
        LdapContext ctx = BasicOps.openContext(cData);
        //LdapContext ctx = BasicOps.openContext("ldap://betch01:19389");
        DXAttributes a = new DXAttributes(ctx.getAttributes("ou=Manufacturing,o=DEMOCORP,c=AU"));
        SchemaOps schemaOps = new SchemaOps(ctx);
        ctx.addToEnvironment("java.naming.ldap.attributes.binary", schemaOps.getNewBinaryAttributes());
        DXAttributes.setDefaultSchema(schemaOps);
//        DXAttribute.setDefaultSchema(schemaOps);
        a.expandAllAttributes();
    }
    /**
     * Tests the expand all attributes.  DXAttribute
     * has a set default schema - this uses that
     * in the test b/c
     * @throws Exception should not throw an
     * exception.
     */
    //TODO - make this a system test.  Currently it wont be run - to run remove the DONT from the method name.
    public void DONTtestExpandAllAttributes3()
        throws Exception
    {
        ConnectionData cData = new ConnectionData();
        cData.setURL("ldap://betch01:19389");
        LdapContext ctx = BasicOps.openContext(cData);

        //LdapContext ctx = BasicOps.openContext("ldap://betch01:19389");
        DXAttributes a = new DXAttributes(ctx.getAttributes("ou=Manufacturing,o=DEMOCORP,c=AU"));
        SchemaOps schemaOps = new SchemaOps(ctx);
        ctx.addToEnvironment("java.naming.ldap.attributes.binary", schemaOps.getNewBinaryAttributes());
        DXAttribute.setDefaultSchema(schemaOps);
        a.expandAllAttributes();
    }
    /**
     * Tests the expand all attributes.  DXAttribute
     * has a set default schema - this uses that
     * in the test b/c
     * @throws Exception should not throw an
     * exception.
     */
    //TODO - make this a system test.  Currently it wont be run - to run remove the DONT from the method name.
    public void DONTtestExpandAllAttributes4()
        throws Exception
    {
        ConnectionData cData = new ConnectionData();
        cData.setURL("ldap://betch01:19389");
        LdapContext ctx = BasicOps.openContext(cData);
        //LdapContext ctx = BasicOps.openContext("ldap://betch01:19389");
        DXAttributes a = new DXAttributes(ctx.getAttributes("ou=Manufacturing,o=DEMOCORP,c=AU"));
        SchemaOps schemaOps = new SchemaOps(ctx);
        ctx.addToEnvironment("java.naming.ldap.attributes.binary", schemaOps.getNewBinaryAttributes());
        DXAttributes.setDefaultSchema(schemaOps);
        DXAttribute.setDefaultSchema(schemaOps);
        a.expandAllAttributes();
    }



    public void testGetDeletionSet()
    {
        RDN newRDN = new RDN("cn=Test");
        Attributes oldSet = new DXAttributes();
        oldSet.put(new BasicAttribute("att1", "value1"));
        oldSet.put(new BasicAttribute("att2", "value2"));
        oldSet.put(new BasicAttribute("att3", "value3;binary"));
        oldSet.put(new BasicAttribute("att4", "value4"));
        oldSet.put(new BasicAttribute("att5", "value5"));

        Attributes newSet = new DXAttributes();
        newSet.put(new BasicAttribute("att1", "value1"));
        newSet.put(new BasicAttribute("att2", "value2"));
        newSet.put(new BasicAttribute("att3", "value3"));
        newSet.put(new BasicAttribute("att4", "value4"));

        try
        {
            DXAttributes deletionSet = DXAttributes.getDeletionSet(newRDN, oldSet, newSet);
            assertEquals("The size of the deletion set is wrong.  Was " + deletionSet.size() + ", should be 1.",
                    1, deletionSet.size());

            Attribute a = deletionSet.get("att5");
            assertNotNull("att5 should be in the deletion set", a);
        }
        catch (NamingException e)
        {
            e.printStackTrace();
            fail("Getting the deletion set shouldn't fail.");
        }
    }
}
