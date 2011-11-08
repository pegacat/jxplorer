package com.ca.commons.naming;

import junit.framework.*;

import javax.naming.InvalidNameException;



/**
 *  A class to exercise and test the RDN class
 */

public class RDNTest extends TestCase
{

    public RDNTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(RDNTest.class);
    }

    public static void main (String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    static final String SMALL_RDN = "l=a";
    static final String TRIPLE_RDN = "cn=fred+l=a+sn=x";
    static final String NASTY_RDN = "cn=x\\=y\\+z+l=a\\ +sn=x";
    static final String SMALL_MULTIVALUED_RDN = "o=o+l=l";


    public void testSmallRDN()
    {
        RDN stringRDN = new RDN(SMALL_RDN);
        assertEquals(stringRDN.toString(), SMALL_RDN);
        assertEquals(stringRDN.getAttID(), "l");
        assertEquals(stringRDN.getRawVal(), "a");

        System.out.println("RDN " + stringRDN.toString() + " : " + stringRDN.getAttID() + " " + stringRDN.getRawVal() );
    }

    public void testTripleRDN()
    {
        RDN stringRDN = new RDN(TRIPLE_RDN);
        assertEquals(stringRDN.toString(), TRIPLE_RDN);
        assertEquals(stringRDN.getAttID(0), "cn");
        assertEquals(stringRDN.getRawVal(0), "fred");
        assertEquals(stringRDN.getAttID(1), "l");
        assertEquals(stringRDN.getRawVal(1), "a");
        assertEquals(stringRDN.getAttID(2), "sn");
        assertEquals(stringRDN.getRawVal(2), "x");
    }

    public void testNastyRDN()
    {
        RDN stringRDN = new RDN(NASTY_RDN);
        assertEquals(stringRDN.toString(), NASTY_RDN);
        assertEquals(stringRDN.getAttID(0), "cn");
        assertEquals(stringRDN.getRawVal(0), "x=y+z");
        assertEquals(stringRDN.getAttID(1), "l");
        assertEquals(stringRDN.getRawVal(1), "a ");
        assertEquals(stringRDN.getAttID(2), "sn");
        assertEquals(stringRDN.getRawVal(2), "x");
    }

    public void testAddEscaped()
        throws InvalidNameException
    {
        RDN rdn = new RDN("o=o");
        rdn.addEscaped("l=l");
        assertEquals(rdn.toString(), SMALL_MULTIVALUED_RDN);
    }

    public void testAddEscapedFail1()
    {
        try
        {
            RDN rdn = new RDN("o=o");
            rdn.addEscaped("l=");

            fail("Should have raised an InvalidNameException");
        }
        catch (InvalidNameException success)
        {
            //TE: this is what we expect to happen i.e. this indicates a test pass.
        }
    }

    public void testAddEscapedFail2()
    {
        try
        {
            RDN rdn = new RDN("o=o");
            rdn.addEscaped("=l");

            fail("Should have raised an InvalidNameException");
        }
        catch (InvalidNameException success)
        {
            //TE: this is what we expect to happen i.e. this indicates a test pass.
        }
    }


}
