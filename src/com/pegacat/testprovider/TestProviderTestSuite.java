package com.pegacat.testprovider;

import junit.framework.*;

/**
 * <p>Runs tests on the test provider to ensure the provider is not corrupted (thus screwing
 * other tests further down the line - all very recursive!).</p>
 */

public class TestProviderTestSuite extends TestCase {

    public TestProviderTestSuite(String name)
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
        suite.addTest(TestProviderContextTest.suite());
        suite.addTest(TreeEntryTest.suite());
        suite.addTest(DataTreeTest.suite());
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




