/*
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 *
 * User: betch01
 * Date: 15/05/2002 / 15:45:25
 */
package com.ca.commons.naming;

import junit.framework.*;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collections;


/**
 *  A class to exercise and test the DN class
 *  @author Chris Betts
 */
public class DNTest extends TestCase
{
    public static final String RDN3Att = "cn";
    public static final String RDN2Att = "ou";
    public static final String RDN1Att = "o";
    public static final String RDN0Att = "c";

    public static final String RDN3Val = "fred";
    public static final String RDN2Val = "\\+research";
    public static final String RDN1Val = "x\\=y\\+\\\"z\\\"";
    public static final String RDN0Val = "af";

    public static final String RDN3 = RDN3Att + '=' + RDN3Val + "+sn=bloggs";
    public static final String RDN2 = RDN2Att + '=' + RDN2Val;
    public static final String RDN1 = RDN1Att + '=' + RDN1Val;
    public static final String RDN0 = RDN0Att + '=' + RDN0Val;

    public static final String NODE = "cn=foo\\'tle+sn=snort";

    // should come out as: "cn=fred+sn=bloggs,ou=\\+research,o=x\\=y\\+\\'z\\',c=af";
    public static String bigComplicatedDN = RDN3 + ',' + RDN2 + ',' + RDN1 + ',' + RDN0;
    public static String anotherComplicatedDN = NODE + ',' + RDN2 + ',' + RDN1 + ',' + RDN0;


    public static final String DERDN5Att = "cn";
    public static final String DERDN4Att = "ou";
    public static final String DERDN3Att = "ou";
    public static final String DERDN2Att = "ou";
    public static final String DERDN1Att = "o";
    public static final String DERDN0Att = "c";

    public static final String DERDN5Val = "por-p201";
    public static final String DERDN4Val = "P 2.01 Koordinierung\\, Controlling\\, Strategische Personalplanung (P 2.01)";
    public static final String DERDN3Val = "P 2 Personalbetreuung\\, Stellenwirtschaft";
    public static final String DERDN2Val = "Personal- und Organisationsreferat";
    public static final String DERDN1Val = "Landeshauptstadt Munchen";
    public static final String DERDN0Val = "DE";

    public static final String DERDN5 = DERDN5Att + '=' + DERDN5Val;
    public static final String DERDN4 = DERDN4Att + '=' + DERDN4Val;
    public static final String DERDN3 = DERDN3Att + '=' + DERDN3Val;
    public static final String DERDN2 = DERDN2Att + '=' + DERDN2Val;
    public static final String DERDN1 = DERDN1Att + '=' + DERDN1Val;
    public static final String DERDN0 = DERDN0Att + '=' + DERDN0Val;

    public static String DEbigComplicatedDN = DERDN5 + ',' + DERDN4 + ',' + DERDN3 + ',' + DERDN2 + ',' + DERDN1 + ',' + DERDN0;

    public static final String strangeDN = "cn=\\\"Craig \\\\nLink\\\",ou=Administration,ou=Corporate,o= DEMOCORP,c=AU";
    public static final String strangeRDN1 = "cn=\"Craig \\nLink\"";
    public static final String strangeRDN2 = "ou=Administration";
    public static final String strangeRDN3 = "ou=Corporate";
    public static final String strangeRDN4 = "o= DEMOCORP";
    public static final String strangeRDN5 = "c=AU";

    public DNTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(DNTest.class);
    }

    public static void main (String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public void testEmptyConstructor()
    {
        DN bloop = new DN();
        assertEquals(bloop.size(), 0);
    }

    public void testCopyConstructor()
    {

        DN copyMe = new DN(bigComplicatedDN);
        DN copy = new DN(copyMe);
        DN bloop = new DN(copy);
        assertEquals("dn equality", copyMe, bloop);
    }


    public void testStringConstructor()
    {
        DN stringDN = new DN(bigComplicatedDN);
        assertEquals(stringDN.toString(), bigComplicatedDN);
    }

    public void testDEStringConstructor()
    {
        DN stringDN = new DN(DEbigComplicatedDN);
        assertEquals(stringDN.toString(), DEbigComplicatedDN);
        System.out.println(stringDN);
    }


    // this is actually a fair bit of work to test, since it
    // requires creating a JNDI ldap Name, which is a bitch.
/*    public void testNameConstructor()
    {
        assertTrue(true); // placeholder
        //DN(Name name)
    }
*/

    // Kaff - pretty much the same test as the string constructor.
    public void testToString()
    {
        DN stringDN = new DN(bigComplicatedDN);
        assertEquals(stringDN.toString(), bigComplicatedDN);
    }

    public void testGetDN()
    {
        testToString();  // deprecated method chains to toString()
    }

    public void testMakeStrangeDN()
        throws NamingException
    {
        DN testDN = new DN();
        testDN.add(strangeRDN5);
        testDN.add(strangeRDN4);
        testDN.add(strangeRDN3);
        testDN.add(strangeRDN2);

        RDN testRDN = new RDN();
        testRDN.addRaw(strangeRDN1);
        testDN.add(testRDN);
        assertEquals(testDN.toString(), strangeDN);
    }

    public void testDEGetRDNAttribute()
    {
        DN testDN = new DN(DEbigComplicatedDN);
        assertEquals(DERDN0Att, testDN.getRDNAttribute(0));
        assertEquals(DERDN1Att, testDN.getRDNAttribute(1));
        assertEquals(DERDN2Att, testDN.getRDNAttribute(2));
        assertEquals(DERDN3Att, testDN.getRDNAttribute(3));
        assertEquals(DERDN4Att, testDN.getRDNAttribute(4));
        assertEquals(DERDN5Att, testDN.getRDNAttribute(5));
        assertEquals("", testDN.getRDNAttribute(6));
        assertEquals("", testDN.getRDNAttribute(-1));
    }


    public void testGetRDNAttribute()
    {
        DN testDN = new DN(bigComplicatedDN);
        assertEquals(RDN0Att, testDN.getRDNAttribute(0));
        assertEquals(RDN1Att, testDN.getRDNAttribute(1));
        assertEquals(RDN2Att, testDN.getRDNAttribute(2));
        assertEquals(RDN3Att, testDN.getRDNAttribute(3));
        assertEquals("", testDN.getRDNAttribute(4));
        assertEquals("", testDN.getRDNAttribute(-1));
    }

    public void testGetRDNValue()
    {
        DN testDN = new DN(bigComplicatedDN);

        try
        {
            assertEquals(NameUtility.unescape(RDN0Val), testDN.getRDNValue(0));
            assertEquals(NameUtility.unescape(RDN1Val), testDN.getRDNValue(1));
            assertEquals(NameUtility.unescape(RDN2Val), testDN.getRDNValue(2));
            assertEquals(NameUtility.unescape(RDN3Val), testDN.getRDNValue(3));
        }
        catch (InvalidNameException e)
        {
            assertTrue(false);
        }

        assertEquals("", testDN.getRDNValue(4));
        assertEquals("", testDN.getRDNValue(-1));
    }

    public void testSetRDN()
    {
        DN test = new DN("x=x,x=x,x=x,x=x");
        test.setRDN(new RDN(RDN0), 0);
        test.setRDN(new RDN(RDN1), 1);
        test.setRDN(new RDN(RDN2), 2);
        test.setRDN(new RDN(RDN3), 3);
        assertEquals(new DN(bigComplicatedDN), test);
    }

    public void testGetRDN()
    {
        DN test = new DN(bigComplicatedDN);
        assertEquals(test.getRDN(0), new RDN(RDN0));
        assertEquals(test.getRDN(1), new RDN(RDN1));
        assertEquals(test.getRDN(2), new RDN(RDN2));
        assertEquals(test.getRDN(3), new RDN(RDN3));
    }

    public void testGetRootRDN()
    {
        DN test = new DN(bigComplicatedDN);
        assertEquals(test.getRootRDN(), new RDN(RDN0));
        assertEquals(test.getRootRDN(), test.getRDN(0));
    }

    public void testGetLowestRDN()
    {
        DN test = new DN(bigComplicatedDN);
        assertEquals(test.getLowestRDN(), new RDN(RDN3));
        assertEquals(test.getLowestRDN(), test.getRDN(3));
    }

    public void testStartsWith()
    {
        DN big = new DN(bigComplicatedDN);
        DN prefix = new DN(RDN0);

        assertTrue(big.startsWith(prefix));

        prefix.add(new RDN(RDN1));
        assertTrue(big.startsWith(prefix));

        prefix.add(new RDN(RDN2));
        assertTrue(big.startsWith(prefix));
    }

    public void testSharesParent()
    {
        DN big = new DN(bigComplicatedDN);
        DN sibling = new DN(anotherComplicatedDN);

        assertTrue(big.sharesParent(sibling));
    }

    public void testParentDN()
    {
        DN veryLongDN = new DN("cn=new level,"+bigComplicatedDN);
        assertEquals(veryLongDN.getParent(), new DN(bigComplicatedDN));
    }

    public void testReverse()
    {
        String bigComplicatedDN = RDN3 + ',' + RDN2 + ',' + RDN1 + ',' + RDN0;
        String reverseDN = RDN0 + ',' + RDN1 + ',' + RDN2 + ',' + RDN3;

        DN testReversal = new DN(bigComplicatedDN);
        testReversal.reverse();
        assertEquals(testReversal.toString(), reverseDN); // placeholder
    }

    public void testClear()
    {
        DN big = new DN(bigComplicatedDN);
        big.clear();
        assertTrue(big.size()==0); // placeholder
    }


    public void testSorting()
    {
        ArrayList<DN> list = new ArrayList<DN>();

        list.add(new DN("cn=fred,ou=legal,o=pegacat,c=au"));
        list.add(new DN("o=pegacat,c=au"));
        list.add(new DN("cn=nigel,ou=legal,o=pegacat,c=au"));
        list.add(new DN("cn=fred,ou=HR,o=pegacat,c=au"));
        list.add(new DN("cn=greg,ou=legal,o=ibm,c=us"));
        list.add(new DN("c=au"));
        list.add(new DN("c=us"));
        


        Collections.sort(list);

        for(DN dn: list)
            System.out.println("DN: " + dn.toString());

        assertEquals("first element should be c=au", list.get(0).toString(), "c=au");
        assertEquals("fifth element should be c=us", list.get(5).toString(), "c=us");
    }


}
