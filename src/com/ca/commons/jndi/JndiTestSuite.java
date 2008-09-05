/**
 * Created by IntelliJ IDEA.
 * User: betch01
 * Date: Dec 3, 2002
 * Time: 12:03:50 PM
 * To change this template use Options | File Templates.
 */
package com.ca.commons.jndi;

import junit.framework.*;

public class JndiTestSuite extends TestCase {

    public JndiTestSuite(String name)
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
        suite.addTest(BasicOpsTest.suite());
        suite.addTest(SchemaOpsTest.suite());
        suite.addTest(ConnectionDataTest.suite());
        suite.addTest(JNDIOpsTest.suite());
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




