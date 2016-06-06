package com.pegacat.testprovider;

import junit.framework.*;

import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.LdapName;
import java.util.Hashtable;

import com.ca.commons.jndi.JndiTools;

/**
 * This code lovingly written by Chris.
 */
public class TestProviderContextTest extends TestCase
{
    TestProviderContext ctx;

    String nameString[] = {"", // 0
                           "c=au", // 1
                           "o=pegacat,c=au", // 2
                           "ou=research,o=pegacat,c=au", // 3
                           "cn=Fred,ou=research,o=pegacat,c=au", // 4
                           "cn=Eric,ou=research,o=pegacat,c=au", // 5
                           "cn=john,ou=research,o=apache,c=us"};     // 6

    LdapName name[] = new LdapName[nameString.length];
    TreeEntry entry[] = new TreeEntry[nameString.length];



    // A reusable constraints object for entry existance searching

    private SearchControls entryExistsSearchConstraints;

    {
        entryExistsSearchConstraints = new SearchControls();
        entryExistsSearchConstraints.setSearchScope(SearchControls.OBJECT_SCOPE);
        entryExistsSearchConstraints.setReturningAttributes(new String[]{"1.1"});  // don't return attributes
    }

    /**
     * test class constructor.  Contains common code to set up a TestProviderContext.
     *
     * @param name
     */
    public TestProviderContextTest(String name)
    {
        super(name);
    }


    /**
     *
     */
    public static Test suite()
    {
        return new TestSuite(TestProviderContextTest.class);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * creates a tame, fresh context for every test
     *
     * @throws NamingException
     */

    public void setUp()
            throws NamingException
    {
        Hashtable env = new Hashtable();
        ctx = (TestProviderContext) new TestProviderContextFactory().getInitialContext(env);

        for (int i = 0; i < nameString.length; i++)
        {
            name[i] = new LdapName(nameString[i]);
            entry[i] = new TreeEntry(name[i]);
        }
    }

    /**
     * Load up the context with a bunch of test entries
     *
     * @throws NamingException
     */
    public void loadContext()
            throws NamingException
    {
        for (int i = 0; i < entry.length; i++)
        {
            ctx.createSubcontext(name[i], entry[i]);
        }
    }

    /**
     * Example of how to set up TestProvider (slightly misleading, since 'setup()' is still
     * being called, although not used...)
     * @throws Exception
     */
    public void testSetUp()
            throws Exception
    {
        Hashtable env = new Hashtable();
        ctx = (TestProviderContext) new TestProviderContextFactory().getInitialContext(env);

        for (int i = 0; i < nameString.length; i++)
        {
            LdapName name = new LdapName(nameString[i]);
            TreeEntry entry = new TreeEntry(name);
            if (i>3)
            {
                entry.put("objectClass", "person");
                entry.put("cn", name.getRdn(name.size()-1).getValue());
                entry.put("sn", "Smith");
                entry.put("userPassword", JndiTools.shaEncode("fnord"));
            }
            ctx.createSubcontext(name, entry);
        }
        ctx.getEntries().dump();
    }

    /**
     *
     */
    //TODO:
    public void testModifyRequest()
            throws Exception
    {
    }

    public void testCheckUserNameAndPassword()
        throws Exception
    {
        loadContext();

        ctx.checkUserNameAndPassword("Chris", "secret");
        ctx.checkUserNameAndPassword("cn=Chris,ou=research,o=pegacat,c=au", "secret");

        entry[4].put("userPassword", "epiphenomenon");
        entry[5].put("userPassword", JndiTools.shaEncode("escatology"));

        loadContext();

        ctx.checkUserNameAndPassword(entry[4].getName().toString(), "epiphenomenon");
        
        ctx.checkUserNameAndPassword(entry[5].getName().toString(), "escatology");
        ctx.checkUserNameAndPassword(entry[5].getName().toString(), JndiTools.shaEncode("escatology"));

        try
        {
            ctx.checkUserNameAndPassword(entry[4].getName().toString(), "secret");
            fail("should not have authenticate " + entry[4].getName().toString());
        }
        catch (NamingException e)  // expected result
        {
        }

        try
        {
            ctx.checkUserNameAndPassword(entry[5].getName().toString(), JndiTools.shaEncode("secret"));
            fail("should not have authenticate " + entry[5].getName().toString());
        }
        catch (NamingException e)  // expected result
        {
        }
    }

    /**
     *
     */
    public void testSearchTreeRequest()
            throws Exception
    {
        entry[3].put("name", "Group Mind Research Department");

        entry[4].put("uid", "774");
        entry[4].put("favouriteDrink", "Japanese Slipper");

        entry[5].put("uid", "992");
        entry[5].put("postcode", "3767");

        loadContext();

        TestProviderEnumeration<SearchResult> results = (TestProviderEnumeration) ctx.search(name[3], "(ObjectClass=*)", new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, null, false, false));

        assertEquals(3, results.size());


        results = (TestProviderEnumeration) ctx.search(name[3], "(ObjectClass=*)", new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, new String[]{"uid", "postcode"}, false, false));

        assertEquals(3, results.size());

        assertEquals(null, results.get(0).getAttributes().get("name"));
        assertEquals(null, results.get(0).getAttributes().get("favouriteDrink"));
        assertEquals(null, results.get(0).getAttributes().get("objectClass"));

        assertEquals("774", results.get(1).getAttributes().get("uid").get());
        assertEquals(null, results.get(1).getAttributes().get("favouriteDrink"));
        assertEquals(null, results.get(1).getAttributes().get("objectClass"));

        assertEquals("992", results.get(2).getAttributes().get("uid").get());
        assertEquals("3767", results.get(2).getAttributes().get("postcode").get());
        assertEquals(null, results.get(2).getAttributes().get("objectClass"));

    }

    /**
     * We have a very primative and basic search filter capability for basic testing.
     * It handles single value matches of the sort (x=y) and (x=y*).  (objectClass=*) is
     * handled as a special 'always true' condition.  If you don't like it, go use a
     * real directory :-).
     *
     * @throws Exception
     */
     public void testSearchFilter()
            throws Exception
    {
        entry[3].put("name", "Group Mind Research Department");

        entry[4].put("uid", "774");
        entry[4].put("favouriteDrink", "Japanese Slipper");

        loadContext();

        assertTrue(ctx.filterMatches("(objectClass=*)", entry[3]));
        assertTrue(ctx.filterMatches("(name=*)", entry[3]));
        assertTrue(ctx.filterMatches("(name=Group Mind Research Department)", entry[3]));
        assertTrue(ctx.filterMatches("(name=Group Mind*)", entry[3]));
        assertTrue(ctx.filterMatches("(name=*Research Department)", entry[3]));
        assertFalse(ctx.filterMatches("(name>=Group)", entry[3])); // not implemented

        assertTrue(ctx.filterMatches("(objectClass=*)", entry[4]));
        assertTrue(ctx.filterMatches("(favouriteDrink=*)", entry[4]));
        assertTrue(ctx.filterMatches("(favouriteDrink=Japanese*)", entry[4]));
        assertTrue(ctx.filterMatches("(favouriteDrink=Japanese Slipper)", entry[4]));
        assertFalse(ctx.filterMatches("(name=Group Mind Research Department)", entry[4]));
        assertFalse(ctx.filterMatches("(name=*)", entry[4]));

        assertTrue(ctx.filterMatches("(favouriteDrink=Jap*)", entry[4]));
        assertTrue(ctx.filterMatches("(favouriteDrink=*Slipper)", entry[4]));
        assertTrue(ctx.filterMatches("(favouriteDrink=*Slip*)", entry[4]));

        assertFalse(ctx.filterMatches("(favouriteDrink=Japf*)", entry[4]));
        assertFalse(ctx.filterMatches("(favouriteDrink=*Shlapper)", entry[4]));
        assertFalse(ctx.filterMatches("(favouriteDrink=*SlipSlop*)", entry[4]));



    }

        /**
     * We now have a more complex search filter capability for basic testing.
     *
     *
     * @throws Exception
     */
     public void testComplexSearchFilter()
            throws Exception
    {
        entry[3].put("favouriteDrink", "Marguarita");
        entry[3].put("favouriteSheep", "Gerald");
        entry[3].put("objectClass", "Topic");

        entry[4].put("uid", "774");
        entry[4].put("favouriteDrink", "Japanese Slipper");
        entry[3].put("objectClass", "Post");

        loadContext();

        assertTrue(ctx.filterMatches("(|(objectClass=Topic)(objectClass=Post))", entry[3]));
        assertFalse(ctx.filterMatches("(&(objectClass=Topic)(objectClass=Post))", entry[3]));
        assertTrue(ctx.filterMatches("(&(favouriteSheep=Ger*)(|(objectClass=Topic)(objectClass=Post)))", entry[3]));
        assertFalse(ctx.filterMatches("(&(favouriteSheep=Nigel)(|(objectClass=Topic)(objectClass=Post)))", entry[3]));


        assertTrue(ctx.filterMatches("(favouriteDrink=Jap*)", entry[4]));
        assertTrue(ctx.filterMatches("(!(favouriteDrink=Vodka*))", entry[4]));
        assertTrue(ctx.filterMatches("(&(uid=774)(favouriteDrink=*Slipper))", entry[4]));




    }

        public void testSplitFilterComponents()
            throws Exception
    {
        Hashtable env = new Hashtable();
        env.put("java.naming.ldap.deleteRDN", "false");
        TestProviderContext testCtx = new TestProviderContext(env, ctx.getEntries());

        String[] component = testCtx.splitFilterComponents("(objectClass=Topic)(objectClass=Post)");
        assertEquals("(objectClass=Topic)", component[0]);
        assertEquals("(objectClass=Post)", component[1]);


        // good 'ol RFC
        component = testCtx.splitFilterComponents("(objectClass=Person)(|(sn=Jensen)(cn=Babs J*))");
        assertEquals("(objectClass=Person)", component[0]);
        assertEquals("(|(sn=Jensen)(cn=Babs J*))", component[1]);

        component = testCtx.splitFilterComponents("(!(|(cn=Fred)(cn=Nigel)))(objectClass=Topic)");
        assertEquals("(!(|(cn=Fred)(cn=Nigel)))", component[0]);
        assertEquals("(objectClass=Topic)", component[1]);
    }

    /**
     * tests a bunch of basic one level search variations
     */
    public void testSearchLevelRequest()
            throws Exception
    {
        entry[4].put("uid", "774");
        entry[4].put("favouriteDrink", "Japanese Slipper");

        entry[5].put("uid", "992");
        entry[5].put("postcode", "3767");

        loadContext();

        TestProviderEnumeration<SearchResult> results = (TestProviderEnumeration) ctx.list(name[3]);

        assertTrue(results.size() == 2);

        results = (TestProviderEnumeration) ctx.search(name[3], "(ObjectClass=*)", new SearchControls(SearchControls.ONELEVEL_SCOPE, 0, 0, null, false, false));

        assertTrue(results.size() == 2);


        results = (TestProviderEnumeration) ctx.search(name[3], "(ObjectClass=*)", new SearchControls(SearchControls.ONELEVEL_SCOPE, 0, 0, new String[]{"uid", "postcode"}, false, false));

        assertTrue(results.size() == 2);

        assertEquals("774", results.get(0).getAttributes().get("uid").get());
        assertEquals(null, results.get(0).getAttributes().get("favouriteDrink"));
        assertEquals(null, results.get(0).getAttributes().get("objectClass"));

        assertEquals("992", results.get(1).getAttributes().get("uid").get());
        assertEquals("3767", results.get(1).getAttributes().get("postcode").get());
        assertEquals(null, results.get(1).getAttributes().get("objectClass"));
    }

    /**
     * tests a bunch of basic entry search variations
     */
    public void testSearchEntry()
            throws Exception
    {
        entry[4].put("uid", "774");
        entry[4].put("favouriteDrink", "Japanese Slipper");

        loadContext();

        Attributes atts = ctx.getAttributes(name[4]);

        assertEquals(3, atts.size());

        TestProviderEnumeration<SearchResult> results = (TestProviderEnumeration) ctx.search(name[4], "(ObjectClass=*)", new SearchControls(SearchControls.OBJECT_SCOPE, 0, 0, null, false, false));

        assertEquals(1, results.size());
        assertEquals(3, results.get(0).getAttributes().size());


        results = (TestProviderEnumeration) ctx.search(name[4], "(ObjectClass=*)", new SearchControls(SearchControls.OBJECT_SCOPE, 0, 0, new String[]{"uid", "postcode"}, false, false));

        assertTrue(results.size() == 1);

        assertEquals("774", results.get(0).getAttributes().get("uid").get());
        assertEquals(null, results.get(0).getAttributes().get("favouriteDrink"));
        assertEquals(null, results.get(0).getAttributes().get("postcode"));
        assertEquals(null, results.get(0).getAttributes().get("objectClass"));
    }


    /**
     * Test that various ldap filters are correctly translated into the corresponding DSML (ignoring white space differences)
     *
     * @throws NamingException
     */
    public void testDeleteRequest()
            throws NamingException
    {
        loadContext();

        exists(nameString[6]);

        ctx.destroySubcontext(name[6]);

        try
        {
            exists(nameString[6]);
            fail("failed to delete entry " + name[6]);
        }
        catch (NamingException e)  // expected result
        {
        }
    }

    /**
     * Utility method - throws a naming exception if an entry doesn't exist
     *
     * @param name
     * @throws NamingException
     */
    protected void exists(String name)
            throws NamingException
    {
        ctx.search(name, "(objectclass=*)", entryExistsSearchConstraints); // throws exception if object doesn't exist
    }

    public void testAddRequest()
            throws NamingException
    {
        ctx = (TestProviderContext) new TestProviderContextFactory().getInitialContext(new Hashtable());

        BasicAttributes testAtts = new BasicAttributes();
        testAtts.put(new BasicAttribute("cn", "Alana SHORE"));

        BasicAttribute oc = new BasicAttribute("objectClass");
        oc.add("inetOrgPerson");
        oc.add("organizationalPerson");
        oc.add("person");
        oc.add("top");
        testAtts.put(oc);
        testAtts.put(new BasicAttribute("userPassword", "password"));
        testAtts.put(new BasicAttribute("sn", "SHORE"));

        String dn = "cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU";

        try
        {
            ctx.search(dn, "(objectclass=*)", entryExistsSearchConstraints); // throws exception if object doesn't exist
            fail("entry already exists! " + dn);
        }
        catch (NamingException e) {}

        ctx.createSubcontext(dn, testAtts);

        exists(dn);

        Attributes returnedAtts = ctx.getAttributes(dn);

        assertEquals("password", returnedAtts.get("userPassword").get());
        assertEquals("SHORE", returnedAtts.get("sn").get());

        Attribute returnedOC = returnedAtts.get("objectClass");
        assertTrue(returnedOC.contains("inetOrgPerson"));
        assertTrue(returnedOC.contains("top"));
        assertTrue(returnedOC.contains("person"));
        assertTrue(returnedOC.contains("organizationalPerson"));


    }

    public void testModRequest()
            throws NamingException
    {
        BasicAttributes testAtts = new BasicAttributes();
        testAtts.put(new BasicAttribute("objectClass", "placeHolder"));
        testAtts.put(new BasicAttribute("cn", "Alana SHORE"));
        testAtts.put(new BasicAttribute("address", "10 Ramage Road"));
        testAtts.put(new BasicAttribute("favouriteDrink", "Japanese Slipper"));


        BasicAttribute oc = new BasicAttribute("objectClass");
        oc.add("inetOrgPerson");
        oc.add("organizationalPerson");
        oc.add("person");
        oc.add("top");

        String dn = "cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU";

        ModificationItem[] mods = new ModificationItem[3];
        mods[0] = new ModificationItem(TestProviderContext.ADD_ATTRIBUTE, new BasicAttribute("userPassword", "my password"));
        mods[1] = new ModificationItem(TestProviderContext.REMOVE_ATTRIBUTE, new BasicAttribute("address", "10 Ramage Road"));
        mods[2] = new ModificationItem(TestProviderContext.REPLACE_ATTRIBUTE, oc);


        ctx.createSubcontext(dn, testAtts);

        Attributes returnedAtts = ctx.getAttributes(dn);
        assertEquals("placeHolder", returnedAtts.get("objectClass").get());
        assertEquals("Alana SHORE", returnedAtts.get("cn").get());
        assertEquals("10 Ramage Road", returnedAtts.get("address").get());

        ctx.modifyAttributes(dn, mods);

        returnedAtts = ctx.getAttributes(dn);
        assertEquals("Alana SHORE", returnedAtts.get("cn").get());
        assertEquals(4, returnedAtts.get("objectClass").size());
        assertEquals("my password", returnedAtts.get("userPassword").get());
        assertEquals(null, returnedAtts.get("address"));
        assertEquals(null, returnedAtts.get("nosuchattribute"));

        try
        {
            ModificationItem mod = new ModificationItem(TestProviderContext.ADD_ATTRIBUTE, new BasicAttribute("userPassword", "my password"));
            ctx.modifyAttributes(dn, new ModificationItem[] {mod});
            fail("attempt to re-add existing att value should throw an exception");
        }
        catch (Exception e) {}

        try
        {
            ModificationItem mod = new ModificationItem(TestProviderContext.REMOVE_ATTRIBUTE, new BasicAttribute("favouriteDrink", "Marguarita"));
            ctx.modifyAttributes(dn, new ModificationItem[] {mod});
            fail("attempt to delete non-existant attribute should throw an exception");
        }
        catch (Exception e) {}

         try
        {
            ModificationItem mod = new ModificationItem(TestProviderContext.REMOVE_ATTRIBUTE, new BasicAttribute("noSuchAtt", "Marguarita"));
            ctx.modifyAttributes(dn, new ModificationItem[] {mod});
            fail("attempt to operate on non-existant att value should throw an exception");
        }
        catch (Exception e) {}
    }

    //TODO:
    public void testModDNRequest()
            throws NamingException
    {
        Hashtable env = new Hashtable();
        env.put("java.naming.ldap.deleteRDN", "false");
        TestProviderContext testCtx = new TestProviderContext(env, ctx.getEntries());

        StringBuffer buffy = new StringBuffer();
//        testCtx.getModDNRequestElement(buffy, "CN=Alice Johnson,DC=Example,DC=COM", "CN=Alice Weiss");

        testCtx.addToEnvironment("java.naming.ldap.deleteRDN", "true");
        buffy = new StringBuffer();
//        testCtx.getModDNRequestElement(buffy, "CN=Alice Johnson,DC=Example,DC=COM", "CN=Alice Weiss");
//        assertXMLEquals(modDNRequest2, buffy.toString());

        testCtx.addToEnvironment("java.naming.ldap.deleteRDN", "true");
        buffy = new StringBuffer();
//        testCtx.getModDNRequestElement(buffy, "CN=Alice Johnson,DC=Example,DC=COM", "CN=Alice Weiss");
//        assertXMLEquals(modDNRequest3, buffy.toString());

        testCtx.addToEnvironment("java.naming.ldap.deleteRDN", "true");
        buffy = new StringBuffer();
//        testCtx.getModDNRequestElement(buffy, "CN=Alice Johnson,DC=Example,DC=COM", "CN=Alice Weiss");
//        assertXMLEquals(modDNRequest4, buffy.toString());

    }



}
