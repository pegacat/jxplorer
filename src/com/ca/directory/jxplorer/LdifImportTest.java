package com.ca.directory.jxplorer;

import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.broker.DataQuery;
import com.ca.directory.jxplorer.broker.JNDIDataBroker;
import com.ca.directory.jxplorer.tree.SmartTree;
import com.pegacat.testprovider.TestLdapContextData;
import com.pegacat.testprovider.TestProviderContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import java.io.StringBufferInputStream;
import java.util.Properties;

/**
 * (c) Chris Betts; Pegacat Software (http://pegacat.com)
 */
public class LdifImportTest extends TestCase
{
    protected TestProviderContext ctx;
    protected JNDIDataBroker broker;
    protected LdifImport ldifImport;

    protected void setUp()
            throws Exception
    {
        // initialise a fake directory with built in test data - a bit of stuffing around to fake enough part of
        // JXplorer to make this work...
        JXConfig.setupProperties(new Properties());
        ctx = new TestLdapContextData().setupTestContext();
        broker = new JNDIDataBroker();
        broker.openTestConnection(ctx);

        SmartTree dummyTree = new SmartTree(null, "test tree", null);
        dummyTree.registerDataSource(broker);

        // NOTE: LdifImport doesn't really like being loaded with no tree and no JXplorer browser, and complains a bit...
        ldifImport = new LdifImport(broker, dummyTree, null, null);
    }

    public LdifImportTest(String name)
    {
       super(name);
    }

    public static Test suite()
    {
       return new TestSuite(LdifImportTest.class);
    }

    public static void main(String[] args)
    {
       junit.textui.TestRunner.run(LdifImportTest.suite());
    }


    /**
     * Create a basic test entry for us to use to compare stuff to
     *
     * note that this entry actually already exists in the test context...
     * @return
     */
    private LdifEntry createTestEntry()
    {
                LdifEntry test = new LdifEntry("cn=Chris,ou=users,o=groupmind,c=au",
                        new Attribute[]{new DXAttribute("objectClass", new String[]{"top", "person", "orgperson", "groupmindPerson"}),
                                         new BasicAttribute("cn", "Chris"),
                                         new BasicAttribute("sn", "a surname"),
                                         new DXAttribute("favouriteDrink", new String[] {"Japanese Slipper", "Toblerone", "Midori Margharita"}),
                                         new BasicAttribute("userPassword", "secret"),
                                         new BasicAttribute("mail", "chris@pegacat.com")});
        return test;
    }




    /**
     * If we make a small change to an existing entry, it should come back as a modify/add
     * @throws Exception
     */
    public void testNormalLdifFile()
            throws Exception
    {

        /*
         // TODO: test checks on normal LDIF imports...

        LdifEntry changes = ldifImport.testNormalLdifImpact(test, broker, null);
        */
    }

    public void testImportLdifChangeModifyFile()
          throws Exception
      {
          String ldifText = "version: 1\n" +
                  "# test modifying an existing entry with add, replace and delete operators\n" +
                  "dn: cn=Chris,ou=users,o=groupmind,c=au\n" +
                  "changetype: modify\n" +
                  "add: postaladdress\n" +
                  "postaladdress: 123 Anystreet $ Sunnyvale, CA $ 94086\n" +
                  "-\n" +
                  "delete: userPassword\n" +
                  "-\n" +
                  "replace: mail\n" +
                  "mail: mrbloopy@fake.org\n" +
                  "mail: m.bloopy@fake.org\n" +
                  "-\n" +
                  "delete: favouriteDrink\n" +
                  "favouriteDrink: Toblerone\n" +
                  "-\n" +
                  "\n" +
                  "\n";


          StringBufferInputStream stringInputStream = new StringBufferInputStream(ldifText);

          // test initial directory state
          DXEntry original = broker.unthreadedReadEntry(new DN("cn=Chris,ou=users,o=groupmind,c=au"), null);
          assertTrue("pre test of staring tree state prior to ldif import", broker.unthreadedExists(new DN("cn=Chris,ou=users,o=groupmind,c=au")));
          assertTrue("pre test of ldif modify - delete", original.get("favouriteDrink").size() == 3);

          // make changes from ldif above
          ldifImport.readLdifTree(stringInputStream, broker, new DataQuery(), false);

          // confirm results

          assertTrue("confirm ldif modify - entry exists", broker.unthreadedExists(new DN("cn=Chris,ou=users,o=groupmind,c=au")));
          DXEntry entry = broker.unthreadedReadEntry(new DN("cn=Chris,ou=users,o=groupmind,c=au"), null);

          assertTrue("could read back modified entry", entry != null);

          assertTrue("confirm ldif modify - replace", entry.get("mail")!=null && entry.get("mail").size() == 2);
          assertTrue("confirm ldif modify - delete", entry.get("favouriteDrink").size() == 2);
          assertTrue("confirm ldif modify - delete", entry.getString("userPassword") == null);
          assertEquals("confirm ldif modify - add", "123 Anystreet $ Sunnyvale, CA $ 94086", entry.getString("postaladdress"));
      }

    public void testImportLdifChangeFile()
        throws Exception
    {
        String ldifText = "version: 1\n" +
                "# test a simple delete\n" +
                "dn: cn=Fred,ou=users,o=groupmind,c=au\n" +
                "changetype: delete\n" +
                "\n" +
                "# test adding a new entry\n" +
                "dn: cn=Michael,ou=users,o=groupmind,c=au\n" +
                "changetype: add\n" +
                "objectClass: inetOrgPerson\n" +
                "objectClass: organizationalPerson\n" +
                "objectClass: person\n" +
                "objectClass: top\n" +
                "businessCategory: ZnJ1ZA==\n" +
                "carLicense: AAA 333\n" +
                "carLicense: CCC 555\n" +
                "cn: Michael\n" +
                "description: a very nice person\n" +
                "displayName: Mike\n" +
                "sn: Smith\n" +
                "uid: 559\n" +
                "\n" +
//TODO: TEST PROVIDER DOESN"T SUPPORT RENAMES PROPERLY
//                "# test moving an entry up a level and changing its name\n" +
//                "dn: cn=Eric,ou=users,o=groupmind,c=au\n" +
//                "changetype: moddn\n" +
//                "deleteoldrdn: 0\n" +
//                "newsuperior: o=groupmind,c=au\n" +
//                "newrdn: cn=admin\n" +
//                "\n" +
                "# test modifying an existing entry with add, replace and delete operators\n" +
                "dn: cn=Chris,ou=users,o=groupmind,c=au\n" +
                "changetype: modify\n" +
                "add: postaladdress\n" +
                "postaladdress: 123 Anystreet $ Sunnyvale, CA $ 94086\n" +
                "-\n" +
                "delete: userPassword\n" +
                "-\n" +
                "replace: mail\n" +
                "mail: mrbloopy@fake.org\n" +
                "mail: m.bloopy@fake.org\n" +
                "-\n" +
                "delete: favouriteDrink\n" +
                "favouriteDrink: Toblerone\n" +
                "-\n" +
                "\n" +
                "\n";


        StringBufferInputStream stringInputStream = new StringBufferInputStream(ldifText);

        assertTrue("pre test of staring tree state prior to ldif import", broker.unthreadedExists(new DN("cn=Fred,ou=users,o=groupmind,c=au")));
        assertFalse("pre test of staring tree state prior to ldif import", broker.unthreadedExists(new DN("cn=Michael,ou=users,o=groupmind,c=au")));
        assertTrue("pre test of staring tree state prior to ldif import", broker.unthreadedExists(new DN("cn=Eric,ou=users,o=groupmind,c=au")));
        assertTrue("pre test of staring tree state prior to ldif import", broker.unthreadedExists(new DN("cn=Chris,ou=users,o=groupmind,c=au")));

        DXEntry original = broker.unthreadedReadEntry(new DN("cn=Chris,ou=users,o=groupmind,c=au"), null);

        assertTrue("pre test of ldif modify - delete", original.get("favouriteDrink").size() == 3);

        ldifImport.readLdifTree(stringInputStream, broker, new DataQuery(), false);

        assertFalse("confirm ldif delete", broker.unthreadedExists(new DN("cn=Fred,ou=users,o=groupmind,c=au")));
        assertTrue("confirm ldif Add", broker.unthreadedExists(new DN("cn=Michael,ou=users,o=groupmind,c=au")));

        assertTrue("confirm ldif modify - entry exists", broker.unthreadedExists(new DN("cn=Chris,ou=users,o=groupmind,c=au")));

        DXEntry entry = broker.unthreadedReadEntry(new DN("cn=Chris,ou=users,o=groupmind,c=au"), null);

        assertTrue("could read back modified entry", entry != null);

        assertEquals("confirm ldif modify - add", "123 Anystreet $ Sunnyvale, CA $ 94086", entry.getString("postaladdress"));
        assertTrue("confirm ldif modify - delete", entry.getString("delete") == null);
        assertTrue("confirm ldif modify - replace", entry.get("mail")!=null && entry.get("mail").size() == 2);
        assertTrue("confirm ldif modify - delete", entry.get("favouriteDrink").size() == 2);

// TODO: Test provider doesn't handle moves properly.
// TODO: a: rewrite test as simple rdn change
// TODO: b: rewrite provider to handle proper DN moves.
//        assertTrue("confirm ldif move", broker.unthreadedExists(new DN("cn=admin,o=groupmind,c=au")));
//        assertTrue("confirm ldif move - old entry gone", broker.unthreadedExists(new DN("cn=Eric,ou=users,o=groupmind,c=au")));





    }

}
