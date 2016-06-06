package com.pegacat.testprovider;

import junit.framework.*;

import javax.naming.ldap.LdapName;
import javax.naming.NamingException;

/**
 *  Runs through sanity tests for the fundamental
 * entry data object used in the tree model that backs
 * the test provider.
 */
public class DataTreeTest extends TestCase
{
    String nameString[] = {"",                                          // 0
                           "c=au",                                      // 1
                           "o=pegacat,c=au",                            // 2
                           "ou=research,o=pegacat,c=au",                // 3
                           "cn=Fred,ou=research,o=pegacat,c=au",        // 4
                           "cn=Eric,ou=research,o=pegacat,c=au",        // 5
                           "cn=john,ou=research,o=apache,c=us"};        // 6

    LdapName name[] = new LdapName[nameString.length];
    TreeEntry entry[] = new TreeEntry[nameString.length];

    public DataTreeTest(String name)
    {
        super(name);
    }

    /**
     */
    public static Test suite()
    {
        return new TestSuite(DataTreeTest.class);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    protected void setUp()
        throws NamingException
    {
        for (int i=0; i< nameString.length; i++)
        {
            name[i] = new LdapName(nameString[i]);
            entry[i] = new TreeEntry(name[i]);
        }
    }

    public void testTreeBuilding()
        throws NamingException
    {
        DataTree testTree = new DataTree();

        assertTrue(testTree.size()==0);

        testTree.addEntry(entry[0]);      // add root node
        assertTrue(testTree.size()==1);

        for (int i=1; i<6; i++)           // add next 5 *in order*, parents first
            testTree.addEntry(entry[i]);

        assertTrue(testTree.size()==6);

        testTree.addEntry(entry[6]);      // add parent-less entry...

        testTree.dump();

        assertTrue(testTree.size()==10);
    }

    public void testTreeRootDelete()
    {
        DataTree testTree = new DataTree();
        testTree.addEntry(entry[0]);
        testTree.deleteEntry("");
        assertTrue(testTree.size()==0);

    }

    public void testParentReferences()
    {
        DataTree testTree = new DataTree();
        for (int i=0; i<7; i++)           // add next 5 *in order*, parents first
            testTree.addEntry(entry[i]);

        assertTrue(entry[3].getChildren().size()==2);

        testTree.deleteEntry(nameString[4]);

        assertTrue(entry[3].getChildren().size()==1);

        testTree.deleteEntry(nameString[5]);

        assertTrue(entry[3].getChildren().size()==0);

        testTree.addEntry(entry[5]);

        assertTrue(entry[3].getChildren().size()==1);
    }

    public void testTreePruning()
    {
        DataTree testTree = new DataTree();
        for (int i=0; i<7; i++)           // add next 5 *in order*, parents first
            testTree.addEntry(entry[i]);

        assertTrue(testTree.size()==10);

        testTree.deleteEntry(nameString[5]);  // delete leaf node
        assertTrue(testTree.size()==9);

        testTree.deleteEntry(nameString[3]); // delete internal node
        assertTrue(testTree.size()==7);

        testTree.deleteEntry(nameString[6]); // delete leaf node
        assertTrue(testTree.size()==6);

        testTree.deleteEntry("c=us");       // delete synthetic place holder node with questions
        assertTrue(testTree.size()==3);

        testTree.deleteEntry("");           // delete root node
        assertTrue(testTree.size()==0);
    }

    public void testTreeAddAndDelete()
    {
    DataTree testTree = new DataTree();
        for (int i=0; i<7; i++)           // add next 5 *in order*, parents first
            testTree.addEntry(entry[i]);

        assertTrue(testTree.size()==10);

        testTree.deleteEntry(nameString[5]);  // delete leaf node
        assertTrue(testTree.size()==9);

        testTree.deleteEntry(nameString[3]); // delete internal node
        assertTrue(testTree.size()==7);

        testTree.deleteEntry(nameString[6]); // delete leaf node
        assertTrue(testTree.size()==6);

        testTree.deleteEntry("c=us");       // delete synthetic place holder node with questions
        assertTrue(testTree.size()==3);

        testTree.addEntry(entry[6]);      // add parent-less entry...
        assertTrue(testTree.size() == 7);

        testTree.addEntry(entry[5]);      // add parent-less entry...
        assertTrue(testTree.size() == 9);

        testTree.addEntry(entry[4]);      // add parent-less entry...
        assertTrue(testTree.size() == 10);

        testTree.deleteEntry("c=au");       // delete synthetic place holder node with questions
        assertTrue(testTree.size()==5);

    }

    public void testTreeReplace()
            throws NamingException
    {
    DataTree testTree = new DataTree();
        for (int i=0; i<7; i++)           // add next 5 *in order*, parents first
            testTree.addEntry(entry[i]);

        assertTrue(testTree.size()==10);

        String existingName = "ou=research,o=pegacat,c=au";
        TreeEntry testEntry = testTree.get(existingName);
        assertTrue("check children", testEntry.children.size()==2);


        TreeEntry replacement = new TreeEntry(new LdapName(existingName));
        replacement.put("tag", "marker");
        testTree.addEntry(replacement);

        assertTrue(testTree.size()==10);

        TreeEntry readItBack = testTree.get(existingName);

        assertTrue("check children", readItBack.children.size()==2);
        assertTrue("check tag", "marker".equals(readItBack.get("tag").get()));

    }
}
