/**
 * This suite runs all the tests for
 * the different TableViewer classes.
 * Author: Chris Betts
 * Date: 15/05/2002 / 16:35:24
 */

package com.ca.directory.jxplorer.viewer.tableviewer;

import junit.framework.*;
import com.ca.commons.naming.*;
import junit.framework.*;



public class TableViewerTestSuite extends TestCase
{
    public TableViewerTestSuite(String name)
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
        suite.addTest(AttributeTableModelTest.suite());

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


