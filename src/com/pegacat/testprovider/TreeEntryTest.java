package com.pegacat.testprovider;

import junit.framework.*;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.ldap.LdapName;

/**
 * Sanity test the basic TreeEntry class.
 * Tests are pretty micky mouse, as the class is a simple data container that doesn't do very much
 * more than extend the BasicAttributes class it is based on...
 */
public class TreeEntryTest extends TestCase
{
    String nameString[] = {"", // 0
                           "c=au", // 1
                           "o=pegacat,c=au", // 2
                           "ou=research,o=pegacat,c=au", // 3
                           "cn=Fred,ou=research,o=pegacat,c=au", // 4
                           "cn=Eric,ou=research,o=pegacat,c=au", // 5
                           "cn=john,ou=research,o=apache,c=us"};     // 6

    LdapName name[] = new LdapName[nameString.length];
    TreeEntry entry[] = new TreeEntry[nameString.length];

    public TreeEntryTest(String name)
    {
        super(name);
    }

    /**
     * @return
     */
    public static Test suite()
    {
        return new TestSuite(TreeEntryTest.class);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public void setUp()
            throws NamingException
    {
        for (int i = 0; i < nameString.length; i++)
            name[i] = new LdapName(nameString[i]);
    }

    public void testClone()
            throws NamingException
    {
        TreeEntry entry = new TreeEntry(name[5], new String[]{"objectClass", "person", "cn", "Fred", "favouriteDrink", "vodka"});
        TreeEntry clone = (TreeEntry) entry.clone();

        assertTrue(clone.size() == 3);

        entry.remove("favouriteDrink");

        assertTrue(entry.size() == 2);
        assertTrue(clone.size() == 3);

        assertTrue(clone.getName().equals(entry.getName()));

        entry.name = new LdapName("cn=blah");

        assertFalse(clone.getName().equals(entry.getName()));
    }

    public void testNameRegistration()
    {
        TreeEntry entry;

        for (int i = 0; i < nameString.length; i++)
        {
            entry = new TreeEntry(name[i]);
            assertEquals(entry.getName(), name[i]);
            assertEquals(entry.getName().toString(), nameString[i]);
            assertEquals(entry.getStringName(), nameString[i]);
        }
    }

    public void testAttributeRegistration()
            throws NamingException
    {
        TreeEntry entry = new TreeEntry(name[5], new String[]{"objectClass", "person", "cn", "Fred", "favouriteDrink", "vodka"});

        assertEquals(entry.get("objectClass").get(), "person");
        assertEquals(entry.get("cn").get(), "Fred");
        assertEquals(entry.get("favouriteDrink").get(), "vodka");
    }

    public void testEquality()
            throws NamingException
    {
        for (int i = 0; i < nameString.length; i++)
        {
            name[i] = new LdapName(nameString[i]);
            entry[i] = new TreeEntry(name[i]);
        }

        TreeEntry test = new TreeEntry(new LdapName(nameString[3]));

        assertTrue(test.equals(entry[3]));
        assertFalse(test.equals(entry[4]));
    }

    public void testChildRegistration()
            throws InvalidNameException
    {
        for (int i = 0; i < nameString.length; i++)
        {
            name[i] = new LdapName(nameString[i]);
            entry[i] = new TreeEntry(name[i]);
        }

        entry[3].addChild(entry[4]);
        entry[3].addChild(entry[5]);

        assertTrue(entry[3].getChildren().size() == 2);

        entry[3].removeChild(entry[4]);
        assertTrue(entry[3].getChildren().size() == 1);

        entry[3].removeChild(entry[2]); // doesn't exist

        assertTrue(entry[3].getChildren().size() == 1);

        entry[3].removeChild(entry[5]);
        assertTrue(entry[3].getChildren().size() == 0);
    }
}
