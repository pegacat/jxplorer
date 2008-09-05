/**
 *
 */

 
package com.ca.commons.naming;

import junit.framework.*;
import javax.naming.NamingException;

public class NameUtilityTest extends TestCase
{

	private static final String myLdapDN = "\\e6\\90\\ad\\e9\\85\\8d\\e5\\90\\b8\\e6\\94\\b6\\e5\\a4\\96\\e8\\b5\\84";
	private static final String myUnicode = "\u642d\u914d\u5438\u6536\u5916\u8d44";	

	private static final String finalSpaceTest = "bloop\\ ";
	private static final String finalSpaceTestOutcome = "bloop ";
	private static final String badFinalSpace = "bloop\\";
	private static final String badFinalSpaceOutcome = "bloop ";
	
	private static final String specialCharRDNVal = "jon\\,fred\\+erick (\\\"\\<http:\\\\\\\\www.blarg.com\\>\\\")" ;

    public NameUtilityTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(NameUtilityTest.class);
    }

    public static void main (String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

	
	public void testCodec() throws NamingException
	{
		String unicode1 = NameUtility.unescape(myLdapDN);
		assertEquals("testing escaped UTF8 to unicode conversion", unicode1, myUnicode);		
		
		String temp = NameUtility.unescape(specialCharRDNVal);
		String backAgain = NameUtility.escape(temp);
		
		assertEquals("testing escaping/unescaping of special rfc 2253 characters", specialCharRDNVal, backAgain);
	}
	
	public void testFinalSpace() throws NamingException
	{
		String temp = NameUtility.unescape(finalSpaceTest);
		assertEquals("Test that a final escaped space is decoded o.k.", finalSpaceTestOutcome, temp);

		temp = NameUtility.escape(temp);
		assertEquals("Test that a final escaped space is encoded o.k.", finalSpaceTest, temp);
		
		temp = NameUtility.unescape(badFinalSpace);
		assertEquals("Test that a malformed final slash is encoded to a slash and a space", badFinalSpaceOutcome, temp);
		
	}
}
