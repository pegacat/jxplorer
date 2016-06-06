package com.ca.directory.jxplorer;

import com.ca.commons.jndi.JndiTestSuite;
import com.ca.commons.naming.NamingTestSuite;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * (c) Chris Betts; Pegacat Software (http://pegacat.com)
 */
public class JXplorerTestSuite extends TestCase
{

    public JXplorerTestSuite(String name)
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

        // Tests for this package
        suite.addTest(LdifImportTest.suite());


		// Tests from other packages
        suite.addTest(NamingTestSuite.suite());
        suite.addTest(JndiTestSuite.suite());


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




