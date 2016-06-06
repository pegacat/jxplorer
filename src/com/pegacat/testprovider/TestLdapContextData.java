package com.pegacat.testprovider;

import com.ca.commons.naming.DXAttribute;
import com.ca.commons.naming.DXEntry;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.Attribute;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * This provides a pre-built fake directory for testing searching and reporting with.
 *
 * (c) JXWorkBench Project
 * - Dr Christopher Betts 2012
 */
public class TestLdapContextData
{
     // TODO - combine this with similar method from GroupMindServletTest?
    public static DXAttribute countryOCs = new DXAttribute("objectClass", new String[]{"top", "country"});
    public static DXAttribute orgOCs = new DXAttribute("objectClass", new String[]{"top", "organization"});
    public static DXAttribute orgUnitOCs = new DXAttribute("objectClass", new String[]{"top", "orgUnit"});
    public static DXAttribute personOCs = new DXAttribute("objectClass", new String[]{"top", "person", "orgperson", "groupmindPerson"});
    public static DXAttribute groupOCs = new DXAttribute("objectClass", new String[]{"top", "groupOfNames"});
    public static DXAttribute topicOCs = new DXAttribute("objectClass", new String[]{"top", "groupmindEntry"});

    /**
     * Sets up a test context for Groupmind tests pre-loaded with a bunch of standard groupmind data.
     * @return
     * @throws NamingException
     */
    public TestProviderContext setupTestContext()
            throws Exception
    {
        TestProviderContextFactory.setProviderContext(null);

        Hashtable environmentVars = new Hashtable();
        environmentVars.put("java.naming.ldap.version", "3");

        TestProviderContext ctx = (TestProviderContext) new TestProviderContextFactory().getInitialContext(environmentVars);

        // setup a small fake directory tree


        ctx.createSubcontext(new DXEntry("c=au", new Attribute[]{countryOCs, new BasicAttribute("c", "au")}));
        ctx.createSubcontext(new DXEntry("o=groupmind,c=au",
                        new Attribute[]{orgOCs,
                                        new BasicAttribute("o", "pegacat")}));
        ctx.createSubcontext(new DXEntry("ou=users,o=groupmind,c=au", new Attribute[]{orgUnitOCs, new BasicAttribute("ou", "research")}));


        ctx.createSubcontext(new DXEntry("cn=fred,ou=users,o=groupmind,c=au",
                        new Attribute[]{ personOCs,
                                         new BasicAttribute("cn", "fred"),
                                         new BasicAttribute("userPassword", "secret"),
                                         new BasicAttribute("uid", "97"),
                                         new BasicAttribute("mail", "admin@nowhere.com"),
                                         new DXAttribute("favouriteDrink", new String[] {"beer", "whisky"}),
                                         new DXAttribute("groupmindFavourites", new String[] {"cn=test2,cn=topics,o=groupmind,c=au",
                                                                                            "cn=test1,cn=topics,o=groupmind,c=au"}),
                                         new BasicAttribute("sn", "Mr Admin")}));


        ctx.createSubcontext(new DXEntry("cn=Fred,ou=users,o=groupmind,c=au",
                        new Attribute[]{ personOCs,
                                         new BasicAttribute("cn", "Fred"),
                                         new BasicAttribute("userPassword", "secret"),
                                         new DXAttribute("groupmindRole", new String[]{"normal"}),
                                         new BasicAttribute("uid", "aaa"),
                                         new DXAttribute("favouriteDrink", new String[] {"sav blanc"}),
                                         new BasicAttribute("mail", "Fred@nowhere.com"),
                                         new DXAttribute("groupmindFavourites", new String[] {"cn=test2,cn=topics,o=groupmind,c=au",
                                                                                            "cn=test1,cn=topics,o=groupmind,c=au"}),
                                         new BasicAttribute("sn", "Fred")}));

        ctx.createSubcontext(new DXEntry("cn=Eric,ou=users,o=groupmind,c=au",
                        new Attribute[]{personOCs,
                                         new BasicAttribute("cn", "Eric"),
                                         new BasicAttribute("sn", "a surname"),
                                         new BasicAttribute("userPassword", "secret"),
                                         new BasicAttribute("groupmindRole", "normal"),
                                         new DXAttribute("groupmindFavourites", new String[] {"cn=test2,cn=topics,o=groupmind,c=au"}),
                                         new DXAttribute("favouriteDrink", new String[] {"Japanese Slipper", "Mai Tai", "Midori Margharita"}),
                                         new BasicAttribute("uid", "98"),
                                         new BasicAttribute("mail", "Eric@nowhere.com"),
                                         new BasicAttribute("postcode", "3767")}));

        // Constants.ADMIN - role setting no longer used; single admin account currently used instead...

        ctx.createSubcontext(new DXEntry("cn=Chris,ou=users,o=groupmind,c=au",
                        new Attribute[]{personOCs,
                                         new BasicAttribute("cn", "Chris"),
                                         new BasicAttribute("sn", "a surname"),
                                         new DXAttribute("favouriteDrink", new String[] {"Japanese Slipper", "Toblerone", "Midori Margharita"}),
                                         new BasicAttribute("userPassword", "secret"),
                                         new BasicAttribute("mail", "chris@pegacat.com")}));

        ctx.createSubcontext(new DXEntry("cn=localgroup,cn=Chris,ou=users,o=groupmind,c=au",
                        new Attribute[]{groupOCs,
                                         new BasicAttribute("cn", "localgroup"),
                                         new DXAttribute("member",  new String[] {"cn=Chris,ou=users,o=groupmind,c=au",
                                                                                      "cn=Mike,ou=users,o=groupmind,c=au",
                                                                                      "cn=Fred,ou=users,o=groupmind,c=au"})}));

        ctx.createSubcontext(new DXEntry("cn=family,cn=Chris,ou=users,o=groupmind,c=au",
                        new Attribute[]{groupOCs,
                                         new BasicAttribute("cn", "family"),
                                new DXAttribute("member",  new String[] {"cn=Chris,ou=users,o=groupmind,c=au",
                                                                             "cn=Mike,ou=users,o=groupmind,c=au"})}));

        ctx.createSubcontext(new DXEntry("cn=sport,cn=Chris,ou=users,o=groupmind,c=au",
                        new Attribute[]{groupOCs,
                                         new BasicAttribute("cn", "sport"),
                                new DXAttribute("member",  new String[] {"cn=Chris,ou=users,o=groupmind,c=au",
                                                                             "cn=Fred,ou=users,o=groupmind,c=au"})}));


        ctx.createSubcontext(new DXEntry("cn=Mike,ou=users,o=groupmind,c=au",
                        new Attribute[]{personOCs,
                                         new BasicAttribute("cn", "Mike"),
                                         new BasicAttribute("sn", "surname"),
                                         new BasicAttribute("userPassword", "secret"),
                                         new BasicAttribute("groupmindRole", "normal"),
                                         new BasicAttribute("uid", "99"),
                                         new BasicAttribute("favouriteDrink", "Zulu Warrior")}));

        ctx.createSubcontext(new DXEntry("cn=test1,cn=topics,o=groupmind,c=au",
                        new Attribute[]{topicOCs,
                                         new BasicAttribute("cn", "test1"),
                                         new BasicAttribute("uid", "xx1"),
                                         new BasicAttribute("groupmindAuthor", "cn=Chris,ou=users,o=groupmind,c=au"),
                                         new BasicAttribute("description", "A very nice entry"),
                                         new BasicAttribute("title", "Sample DXEntry 1")}));

        ctx.createSubcontext(new DXEntry("cn=test2,cn=topics,o=groupmind,c=au",
                        new Attribute[]{topicOCs,
                                         new BasicAttribute("cn", "test2"),
                                         new BasicAttribute("uid", "xx2"),
                                         new BasicAttribute("groupmindAuthor", "cn=Chris,ou=users,o=groupmind,c=au"),
                                         new BasicAttribute("description", "Another very nice entry"),
                                         new BasicAttribute("title", "Sample DXEntry 2")}));

        return ctx;

    }

}
