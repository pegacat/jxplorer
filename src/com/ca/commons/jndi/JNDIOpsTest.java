package com.ca.commons.jndi;

import junit.framework.*;




/**
 * Test class for creating LDAP filters from JAXB objects.
 * It passes a DSML XML request to the LdapFilter class
 * and compares the result with the expected LDAP filter.
 * @author Chris.
 */

public class JNDIOpsTest extends TestCase
{

    public JNDIOpsTest(String name)
    {
        super(name);
    }


    public static Test suite()
    {
        return new TestSuite(JNDIOpsTest.class);
    }



    protected void setUp()
    {
    }



    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }



    /**
     * Test examples from RFC 2252
     */
    public void testURLEncoding()
    {
        assertEquals("ldap://localhost:19389", JNDIOps.makeServerURL("ldap://localhost:19389", null));
        assertEquals("ldap://localhost:19389", JNDIOps.makeServerURL("ldap://localhost:19389", ""));
        assertEquals("ldap:///o=University%20of%20Michigan,c=US", JNDIOps.makeServerURL("ldap://", "o=University of Michigan,c=US"));
        assertEquals("ldap:///o=University%20of%20Michigan,c=US", JNDIOps.makeServerURL("ldap:///","o=University of Michigan,c=US" ));
        assertEquals("ldap://ldap.itd.umich.edu/o=University%20of%20Michigan,c=US", JNDIOps.makeServerURL("ldap://ldap.itd.umich.edu", "o=University of Michigan,c=US"));
        assertEquals("ldap://ldap.itd.umich.edu/o=University%20of%20Michigan,c=US", JNDIOps.makeServerURL("ldap://ldap.itd.umich.edu/","o=University of Michigan,c=US" ));
        assertEquals("ldap://ldap.question.com/o=Question%3f,c=US", JNDIOps.makeServerURL("ldap://ldap.question.com", "o=Question?,c=US"));
        assertEquals("ldap:///o=%20%3c%3e%3f%23%25%7b%7d%7c%5c%5e%7e%5b%5d%27", JNDIOps.makeServerURL("ldap:///", "o= <>\"#%{}|\\^~[]'"));
    }

}