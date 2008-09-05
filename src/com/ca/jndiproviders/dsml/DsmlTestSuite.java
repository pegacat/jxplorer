package com.ca.jndiproviders.dsml;

import junit.framework.*;

/**
 * <p>A micky mouse junit test suite that runs micky mouse junit tests on the
 * micky mouse example functionality included in the base SPML distribution.
 * <i>However</i> this is an excellent place to extend those tests when you
 * add real functionality.</p>
 * @author Trudi Ersvaer.
 */

public class DsmlTestSuite extends TestCase {

    public DsmlTestSuite(String name)
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
        suite.addTest(DsmlContextTest.suite());
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




