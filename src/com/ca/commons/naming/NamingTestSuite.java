/**
 * This suite runs all the tests for
 * the different Naming classes.
 * Author: Chris Betts
 * Date: 15/05/2002 / 16:35:24
 */

package com.ca.commons.naming;
import junit.framework.*;

public class NamingTestSuite extends TestCase {

    public NamingTestSuite(String name)
	{
        super(name);
    }


    /**
     * Assembles and returns a test suite
     * containing all known tests.
     *
     * New tests should be added here!
     *
     * @return A non-null test suite.
     */

    public static Test suite()
    {
        TestSuite suite = new TestSuite();

		// The tests...
        suite.addTest(DNTest.suite());
        suite.addTest(RDNTest.suite());
        suite.addTest(DXAttributesTest.suite());
        suite.addTest(DXAttributeTest.suite());
		suite.addTest(NameUtilityTest.suite());
        suite.addTest(LdifModifyAttributeTest.suite());
        suite.addTest(LdifEntryTest.suite());
        suite.addTest(LdifUtilityTest.suite());
        suite.addTest(LdifStreamReaderTest.suite());

        return suite;
    }

    /**
     * Runs the test suite.
     */

    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }
}




