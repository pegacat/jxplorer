package com.ca.commons.naming;

import com.ca.commons.cbutil.CBBase64;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import java.io.BufferedReader;
import java.io.StringReader;


public class LdifUtilityTest extends TestCase
{
    public LdifUtility utility;

    public LdifUtilityTest(String name)
    {
        super(name);
    }


    public static Test suite()
    {
        return new TestSuite(LdifUtilityTest.class);
    }



    protected void setUp()
    {
        utility = new LdifUtility();

    }



    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(LdifUtilityTest.suite());
    }

    public void testBase64Decoding()
            throws Exception
    {
        String base64Text = "" +
                "VGhlIERhdGEgQ3J5c3RhbHMgb2YgdGhlIG9yaWdpbmFsIGNvcmUgYXJlIGJlaW" +
                " 5nIHNsb3dseSBhc3NpbWlsYXRlZC4gIFRoZSBbW0NvbnNlcnZhdGl2ZXNdXSBhcmUgcmVzaXN0a" +
                " W5nIHRob3VnaCwgZmlnaHRpbmcgdGhlIFtbTWVtb3J5IFdhcnNdXSB0byBwcmV2ZW50IG5ldyBk" +
                " YXRhIGJlaW5nIGFjY2Vzc2VkLCBhbmQgdHJ5aW5nIHRvIHNsb3cgdGhlIHBhY2Ugb2YgYXNzaW1" +
                " pbGF0aW9u";

        String expectedOutput = "The Data Crystals of the original core are being slowly assimilated.  The [[Conservatives]] are resisting though, fighting the [[Memory Wars]] to prevent new data being accessed, and trying to slow the pace of assimilation";


        byte[] base64Data = CBBase64.decode(base64Text.getBytes("US-ASCII"));
        String output = new String(base64Data, "US-ASCII");

        System.out.println("got:\n" + output);

        assertEquals(expectedOutput, output);
    }



    public void testBase64Decoding2()
            throws Exception
    {

//        String base64Text = "CgpBIE51cnNlcnkgYXJlYSB3aGVyZSB5b3VuZywgcHJlLXNlbnRpZW50IHBvZGpbmdzIGFyZSByYWlzZWQuICBJdCBpcyBvZmYgbGltaXRzIHRvIG9sZGVyIHBvZGxpbmdzLCBidQgdGhlcmUgaXMgbm8gcmVhc29uIHdoeSB5b3Ugd291bGQgd2FudCB0byBnbyB0aGVyZSBhbnl3XkuCgpsZXRzIHNlZSBpZiBwYXJhZ3JhcGhzIGFyZSB3b3JraW5nIG5vdyEKCkhhcHB5IFBhcmFncmFwaC4K";

          //String base64Text = "CgpBIE51cnNlcnkgYXJlYSB3aGVyZSB5b3VuZywgcHJlLXNlbnRpZW50IHBvZGxpbmdzIGFyZSByYWlzZWQuICBJdCBpcyBvZmYgbGltaXRzIHRvIG9sZGVyIHBvZGxpbmdzLCBidXQgdGhlcmUgaXMgbm8gcmVhc29uIHdoeSB5b3Ugd291bGQgd2FudCB0byBnbyB0aGVyZSBhbnl3YXkuCgpsZXRzIHNlZSBpZiBwYXJhZ3JhcGhzIGFyZSB3b3JraW5nIG5vdyEKCkhhcHB5IFBhcmFncmFwaC4K";
        String base64Text = "" +
                "CgpBIE51cnNlcnkgYXJlYSB3aGVyZSB5b3VuZywgcHJlLXNlbnRpZW50IHBvZG\n" +
                " xpbmdzIGFyZSByYWlzZWQuICBJdCBpcyBvZmYgbGltaXRzIHRvIG9sZGVyIHBvZGxpbmdzLCBid\n" +
                " XQgdGhlcmUgaXMgbm8gcmVhc29uIHdoeSB5b3Ugd291bGQgd2FudCB0byBnbyB0aGVyZSBhbnl3\n" +
                " YXkuCgpsZXRzIHNlZSBpZiBwYXJhZ3JhcGhzIGFyZSB3b3JraW5nIG5vdyEKCkhhcHB5IFBhcmFncmFwaC4K";

        String expectedOutput = "bloop";

        System.out.println("base64text length = " + base64Text.length());

        byte[] base64Data = CBBase64.decode(base64Text.getBytes("US-ASCII"));
        String output = new String(base64Data, "US-ASCII");

        System.out.println("got:\n" + output);
    }


    public void testLdifEncoding()
    {
        String attVal1 = "fnord";
        String attVal2 = "bloopy\nbloopy\nbloopy\n";
        String attVal3 = " snort ";


        String ldifVal1 = utility.ldifEncode("bloop", attVal1);
        assertEquals("bloop: " + attVal1 + "\n", ldifVal1);

        // note use of ldif hack
        String ldifVal2 = utility.ldifEncode("bloop", attVal2);
        assertEquals("bloop:: Ymxvb3B5CmJsb29weQpibG9vcHkK\n", ldifVal2);

        String ldifVal3 = utility.ldifEncode("bloop", attVal3);
        assertEquals("bloop:: IHNub3J0IA==\n", ldifVal3);


    }

    public void testLdifDecoding()
            throws NamingException
    {
        String attVal1 = "description: fnord";
        String attVal2 = "description: bloopy\nbloopy\nbloopy\n";

        Object[] attValPair = utility.ldifDecodeAttribute(attVal1);
        assertEquals("description", attValPair[0]);
        assertEquals("fnord", attValPair[1]);

        attValPair = utility.ldifDecodeAttribute(attVal2);
        assertEquals("description", attValPair[0]);
        assertEquals("bloopy\nbloopy\nbloopy\n", attValPair[1]);

    }

    public void testLdifEncodingAsBinary()
    {
        String binaryLdif = utility.ldifEncodeAsBinary("bloop", " snort ");
        assertEquals("bloop:: IHNub3J0IA==\n", binaryLdif);
    }

    public void testLdifDecodingAsBinary()
            throws NamingException
    {
        Object[] attValPair = utility.ldifDecodeAttribute("bloop:: IHNub3J0IA==\n");
        assertEquals("bloop", attValPair[0]);
        assertEquals(" snort ", attValPair[1]);
          String text = "\n== Welcome to Sentience ==\n" +
                "\n" +
                "time to leave behind the dark dreams you had before you woke up...\n" +
                "\n" +
                "\n" +
                "... no feeling of absence from the pool; rather instead of 1st voice whispers, you can see the magically glowing sigils of 5 wide hexagraphs...\n" +
                "\n" +
                "... now that you have woken, you can go anywhere in the hive... except back to the nursery ...\n" +
                "\n" +
                "\n" +
                "=== Questions ===\n" +
                "\n" +
                "questions about the nursery are discouraged... but if pressed, will usually be answered.\n" +
                " \n" +
                "questions about ancient history are met with the [[Standard History]]; mention of the Evolution wars etc. are met with amusement, and suggestions to ignore the dreams within a dreams of the time before you woke.\n" +
                "\n";

        String base64text = "description:: Cj09IFdlbGNvbWUgdG8gU2VudGllbmNlID09Cgp0aW1lIHRvIGxlYXZlIGJlaG\n" +
                " luZCB0aGUgZGFyayBkcmVhbXMgeW91IGhhZCBiZWZvcmUgeW91IHdva2UgdXAuLi4KCgouLi4gb\n" +
                " m8gZmVlbGluZyBvZiBhYnNlbmNlIGZyb20gdGhlIHBvb2w7IHJhdGhlciBpbnN0ZWFkIG9mIDFz\n" +
                " dCB2b2ljZSB3aGlzcGVycywgeW91IGNhbiBzZWUgdGhlIG1hZ2ljYWxseSBnbG93aW5nIHNpZ2l\n" +
                " scyBvZiA1IHdpZGUgaGV4YWdyYXBocy4uLgoKLi4uIG5vdyB0aGF0IHlvdSBoYXZlIHdva2VuLC\n" +
                " B5b3UgY2FuIGdvIGFueXdoZXJlIGluIHRoZSBoaXZlLi4uIGV4Y2VwdCBiYWNrIHRvIHRoZSBud\n" +
                " XJzZXJ5IC4uLgoKCj09PSBRdWVzdGlvbnMgPT09CgpxdWVzdGlvbnMgYWJvdXQgdGhlIG51cnNl\n" +
                " cnkgYXJlIGRpc2NvdXJhZ2VkLi4uIGJ1dCBpZiBwcmVzc2VkLCB3aWxsIHVzdWFsbHkgYmUgYW5\n" +
                " zd2VyZWQuCiAKcXVlc3Rpb25zIGFib3V0IGFuY2llbnQgaGlzdG9yeSBhcmUgbWV0IHdpdGggdG\n" +
                " hlIFtbU3RhbmRhcmQgSGlzdG9yeV1dOyBtZW50aW9uIG9mIHRoZSBFdm9sdXRpb24gd2FycyBld\n" +
                " GMuIGFyZSBtZXQgd2l0aCBhbXVzZW1lbnQsIGFuZCBzdWdnZXN0aW9ucyB0byBpZ25vcmUgdGhl\n" +
                " IGRyZWFtcyB3aXRoaW4gYSBkcmVhbXMgb2YgdGhlIHRpbWUgYmVmb3JlIHlvdSB3b2tlLgoK\n";

        String encodedText = utility.ldifEncode("description", text);

        assertEquals(base64text, encodedText);

        attValPair = utility.ldifDecodeAttribute(base64text);
        assertEquals("description", attValPair[0]);

        String actual = (String)attValPair[1];

        actual = actual.replaceAll("\r","" );

        //assertEquals(text, actual);

    }
      public void testLdifEncodeEntry2()
            throws Exception
    {
        DXEntry bloop = new DXEntry(new DN("cn=Admin,ou=users,o=groupmind,c=au"),
                        new Attribute[]{ new DXAttribute("oc", new String[] {"top", "person", "organisationalPerson"}),
                                         new DXAttribute("groupmindGroup", new String[] {"cn=test2,cn=topics,o=groupmind,c=au","cn=test1,cn=topics,o=groupmind,c=au"})}
                        );

        String expectedLdif = "" +
                "dn: cn=Admin,ou=users,o=groupmind,c=au\n" +
                "oc: top\n" +
                "oc: person\n" +
                "oc: organisationalPerson\n" +
                "groupmindGroup: cn=test2,cn=topics,o=groupmind,c=au\n" +
                "groupmindGroup: cn=test1,cn=topics,o=groupmind,c=au\n\n";


        String ldifEntry = new LdifEntry(bloop).toString();

        assertEquals(expectedLdif, ldifEntry);
    }

    public void testLdifEncodeEntry()
            throws Exception
    {
        DXEntry bloop = new DXEntry(new DN("cn=Admin,ou=users,o=groupmind,c=au"),
                        new Attribute[]{ new DXAttribute("oc", new String[] {"top", "person", "organisationalPerson"}),
                                         new BasicAttribute("cn", "Admin"),
                                         new BasicAttribute("userPassword", "{SHA}5en6G6MezRroT3XKqkdPOmY/BfQ="),
                                         new BasicAttribute("userAddress", "10 Ramage road\nMount Eliza\nQueensland\n4000"),
                                         new BasicAttribute("groupmindRole", "admin"),
                                         new BasicAttribute("bloop", " snort "),
                                         new BasicAttribute("uid", "97"),
                                             new BasicAttribute("mail", "Admin@nowhere.com"),
                                         new DXAttribute("groupmindGroup", new String[] {"cn=test2,cn=topics,o=groupmind,c=au",
                                                                                            "cn=test1,cn=topics,o=groupmind,c=au"}),
                                         new BasicAttribute("sn", "Mr Admin")}
                );

        String expectedLdif = "" +
                "dn: cn=Admin,ou=users,o=groupmind,c=au\n" +
                "oc: top\n" +
                "oc: person\n" +
                "oc: organisationalPerson\n" +
                "bloop:: IHNub3J0IA==\n" +
                "cn: Admin\n" +
                "groupmindGroup: cn=test2,cn=topics,o=groupmind,c=au\n" +
                "groupmindGroup: cn=test1,cn=topics,o=groupmind,c=au\n" +
                "groupmindRole: admin\n" +
                "mail: Admin@nowhere.com\n" +
                "sn: Mr Admin\n" +
                "uid: 97\n" +
                "userAddress:: MTAgUmFtYWdlIHJvYWQKTW91bnQgRWxpemEKUXVlZW5zbGFuZAo0MDAw\n" +
                "userPassword: {SHA}5en6G6MezRroT3XKqkdPOmY/BfQ=\n" +
                "\n";


        String ldifEntry = new LdifEntry(bloop).toString();

        assertEquals(expectedLdif, ldifEntry);
    }

    public void testLdifDecodeEntry()
            throws Exception
    {
         String ldifData = "" +
                "DN: cn=Admin,ou=users,o=groupmind,c=au\n" +
                "objectClass: top\n" +
                "objectClass: person\n" +
                "objectClass: organisationalPerson\n" +
                "groupmindGroup: cn=test2,cn=topics,o=groupmind,c=au\n" +
                "groupmindGroup: cn=test1,cn=topics,o=groupmind,c=au\n" +
                "oc: top\n" +
                "oc: person\n" +
                "oc: organisationalPerson\n" +
                "sn: Mr Admin\n" +
                "userPassword: {SHA}5en6G6MezRroT3XKqkdPOmY/BfQ=\n" +
                "mail: Admin@nowhere.com\n" +
                "uid: 97\n" +
                "userAddress: 10 Ramage road\n" +
                " >Mount Eliza\n" +
                " >Queensland\n" +
                " >4000\n" +
                "cn: Admin\n" +
                "groupmindRole: admin\n" +
                "bloop:: IHNub3J0IA==\n" +
                 "\n" +
                "DN: cn=Fred,ou=users,o=groupmind,c=au\n" +
                "objectClass: top\n" +
                "objectClass: person\n" +
                "objectClass: organisationalPerson\n" +
                "groupmindGroup: cn=test2,cn=topics,o=groupmind,c=au\n" +
                "groupmindGroup: cn=test1,cn=topics,o=groupmind,c=au\n" +
                "oc: top\n" +
                "oc: person\n" +
                "oc: organisationalPerson\n" +
                "sn: Mr Fred\n" +
                "userPassword: {SHA}5en6G6MezRroT3XKqkdPOmY/BfQ=\n" +
                "mail: Fred@nowhere.com\n" +
                "uid: 98\n" +
                "userAddress: 12 Ramage road\n" +
                " >Mount Bloopy\n" +
                " >Tasmania\n" +
                " >7015\n" +
                "cn: Admin\n" +
                "groupmindRole: user\n" +
                "description:: CgpBIE51cnNlcnkgYXJlYSB3aGVyZSB5b3VuZywgcHJlLXNlbnRpZW50IHBvZG\n" +
                " xpbmdzIGFyZSByYWlzZWQuICBJdCBpcyBvZmYgbGltaXRzIHRvIG9sZGVyIHBvZGxpbmdzLCBid\n" +
                " XQgdGhlcmUgaXMgbm8gcmVhc29uIHdoeSB5b3Ugd291bGQgd2FudCB0byBnbyB0aGVyZSBhbnl3\n" +
                " YXkuCgpsZXRzIHNlZSBpZiBwYXJhZ3JhcGhzIGFyZSB3b3JraW5nIG5vdyEKCkhhcHB5IFBhcmFncmFwaC4K\n\n\n";

       StringReader stringReader = new StringReader(ldifData);
       LdifStreamReader reader = new LdifStreamReader(stringReader);

       DXEntry entry1 = utility.readLdifEntry(reader);

        assertEquals("cn=Admin,ou=users,o=groupmind,c=au", entry1.getDN().toString());
        assertEquals("97", entry1.getString("uid"));
        assertEquals("admin", entry1.getString("groupmindRole"));
        assertEquals("10 Ramage road\nMount Eliza\nQueensland\n4000", entry1.getString("userAddress"));

       DXEntry entry2 = utility.readLdifEntry(reader);

        assertEquals("cn=Fred,ou=users,o=groupmind,c=au", entry2.getDN().toString());
        assertEquals("98", entry2.getString("uid"));
        assertEquals("user", entry2.getString("groupmindRole"));
    }

    /**
     * This test the 'readable line feed hack' where we use the '>' character to make multi line text readable in
     * an LDIF file.  (This is not standard LDIF!)
     * @throws Exception
     */
        public void testLdifDecodeEntry2()
            throws Exception
    {
         String ldifData = "" +
                 "DN: cn=Test Area,cn=blog,cn=Chris,cn=topics,o=groupmind,c=au\n" +
                 "objectClass: top\n" +
                 "objectClass: groupmindEntry\n" +
                 "groupmindCounter: 2\n" +
                 "groupmindType: topic\n" +
                 "groupmindAccess: write:cn=Chris,ou=users,o=groupmind,c=au\n" +
                 "groupmindAccess: write:cn=Chris,cn=Chris,ou=users,o=groupmind,c=au\n" +
                 "groupmindAccess: read:cn=public\n" +
                 "groupmindAccess: write:cn=public\n" +
                 "groupmindAccess: read:cn=Chris,cn=Chris,ou=users,o=groupmind,c=au\n" +
                 "groupmindAccess: read:cn=Chris,ou=users,o=groupmind,c=au\n" +
                 "groupmindRating: 10\n" +
                 "groupmindAuthor: cn=Chris,ou=users,o=groupmind,c=au\n" +
                 "uid: 3as\n" +
                 "title: Test Area\n" +
                 "cn: Test Area\n" +
                 "description: This blog is useful for testing formatting.\n" +
                 " > >\n" +
                 " >\n" +
                 " >alpha\n" +
                 "groupmindTimestamp: 1215037384717";

       StringReader stringReader = new StringReader(ldifData);
       LdifStreamReader reader = new LdifStreamReader(stringReader);

       DXEntry entry1 = utility.readLdifEntry(reader);

       String result =  "This blog is useful for testing formatting.\n" +
                 " >\n" +
                 "\n" +
                 "alpha";

       assertEquals("cn=Test Area,cn=blog,cn=Chris,cn=topics,o=groupmind,c=au", entry1.getDN().toString());
       assertEquals(result, entry1.getString("description"));
    }
}

