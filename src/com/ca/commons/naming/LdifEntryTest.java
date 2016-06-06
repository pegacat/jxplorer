package com.ca.commons.naming;

import com.ca.commons.cbutil.CBBase64;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * (c) Chris Betts; Pegacat Software (http://pegacat.com)
 */
public class LdifEntryTest extends TestCase
{

    public LdifEntryTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(LdifEntryTest.class);
    }

    public static void main (String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * <pre>
     * example 3 from RFC 2849
     *
        dn: cn=Gern Jensen, ou=Product Testing, dc=airius, dc=com
        objectclass: top
        objectclass: person
        objectclass: organizationalPerson
        cn: Gern Jensen
        cn: Gern O Jensen
        sn: Jensen
        uid: gernj
        telephonenumber: +1 408 555 1212
        description:: V2hhdCBhIGNhcmVmdWwgcmVhZGVyIHlvdSBhcmUhICBUaGlzIHZhbHVl
        IGlzIGJhc2UtNjQtZW5jb2RlZCBiZWNhdXNlIGl0IGhhcyBhIGNvbnRyb2wgY2hhcmFjdG
        VyIGluIGl0IChhIENSKS4NICBCeSB0aGUgd2F5LCB5b3Ugc2hvdWxkIHJlYWxseSBnZXQg
        b3V0IG1vcmUu
     * </pre>
     */
    public void testNormal()
            throws Exception
    {
        LdifEntry entry = new LdifEntry("cn=Gern Jensen, ou=Product Testing, dc=airius, dc=com",
                   new Attribute[]{ new DXAttribute("objectclass", new String[] {"top", "person", "organizationalPerson"}),
                                    new DXAttribute("cn", new String[] {"Gern Jensen", "Gern O Jensen"}),
                                    new BasicAttribute("sn", "Jensen"),
                                    new BasicAttribute("uid", "gernj"),
                                    new BasicAttribute("telephonenumber", "+1 408 555 1212"),
                                    new BasicAttribute("description", "What a careful reader you are!  This value is base-64-encoded because it has a control character in it (a CR).\n" +
                                            "  By the way, you should really get out more.")});

        // note example reordered to be alphabetical (for testing consistency), and spaces removed from DN
        String rfcText = "" +
                "dn: cn=Gern Jensen,ou=Product Testing,dc=airius,dc=com\n" +
                "objectclass: top\n" +
                "objectclass: person\n" +
                "objectclass: organizationalPerson\n" +
                "cn: Gern Jensen\n" +
                "cn: Gern O Jensen\n" +
                "description:: V2hhdCBhIGNhcmVmdWwgcmVhZGVyIHlvdSBhcmUhICBUaGlzIHZhbHVlIGlzIG\n" +
                " Jhc2UtNjQtZW5jb2RlZCBiZWNhdXNlIGl0IGhhcyBhIGNvbnRyb2wgY2hhcmFjdGVyIGluIGl0I\n" +
                " ChhIENSKS4KICBCeSB0aGUgd2F5LCB5b3Ugc2hvdWxkIHJlYWxseSBnZXQgb3V0IG1vcmUu\n" +
                "sn: Jensen\n" +
                "telephonenumber: +1 408 555 1212\n" +
                "uid: gernj\n\n";

        assertEquals("check normal ldif equals rfc text", rfcText, entry.toString());

    }

    /**
     * <pre>
     * example 6: from RFC 2849
     *
     * # Add a new entry
        dn: cn=Fiona Jensen, ou=Marketing, dc=airius, dc=com
        changetype: add
        objectclass: top
        objectclass: person
        objectclass: organizationalPerson
        cn: Fiona Jensen
        sn: Jensen
        uid: fiona
        telephonenumber: +1 408 555 1212
        jpegphoto:< file:///usr/local/directory/photos/fiona.jpg

     ... but we'll lose the file import as it's not supported... and may never be (it looks horrible security-wise, and no one's asked for it in ten years?)
     </pre>
     
     * @throws Exception
     */
    public void testAdd()
            throws Exception
    {
        LdifEntry entry = new LdifEntry("cn=Fiona Jensen,ou=Marketing,dc=airius,dc=com",
                   new Attribute[]{ new DXAttribute("objectclass", new String[] {"top", "person", "organizationalPerson"}),
                                    new DXAttribute("cn", "Fiona Jensen"),
                                    new BasicAttribute("sn", "Jensen"),
                                    new BasicAttribute("telephonenumber", "+1 408 555 1212"),
                                    new BasicAttribute("uid", "fiona")});

        entry.setChangeType(LdifEntryType.add);

        // note example reordered to be alphabetical (for testing consistency), and spaces removed from DN
        String rfcText = "" +
                "dn: cn=Fiona Jensen,ou=Marketing,dc=airius,dc=com\n" +
                "changetype: add\n" +
                "objectclass: top\n" +
                "objectclass: person\n" +
                "objectclass: organizationalPerson\n" +
                "cn: Fiona Jensen\n" +
                "sn: Jensen\n" +
                "telephonenumber: +1 408 555 1212\n" +
                "uid: fiona\n\n";

        assertEquals("check add ldif equals rfc text", rfcText, entry.toString());
    }

    /**
     * also from example 6:
     * <pre>
     * # Delete an existing entry
dn: cn=Robert Jensen, ou=Marketing, dc=airius, dc=com
changetype: delete
     </pre>
     * @throws Exception
     */
    public void testDelete()
            throws Exception
    {
        LdifEntry entry = new LdifEntry("cn=Robert Jensen,ou=Marketing,dc=airius,dc=com",
                   new Attribute[]{ new DXAttribute("objectclass", new String[] {"top", "person", "organizationalPerson"}),
                                    new DXAttribute("cn", "Robert Jensen"),
                                    new BasicAttribute("sn", "Jensen"),
                                    new BasicAttribute("telephonenumber", "+1 408 555 1212"),
                                    new BasicAttribute("uid", "robby")});

        entry.setChangeType(LdifEntryType.delete);

        // note spaces removed from DN
        String rfcText = "" +
                "dn: cn=Robert Jensen,ou=Marketing,dc=airius,dc=com\n" +
                 "changetype: delete\n\n";

        assertEquals("check add ldif equals rfc text", rfcText, entry.toString());
    }

    /**
     * more from example 6:
     *
     * <pre>
        # Modify an entry: add an additional value to the postaladdress
        # attribute, completely delete the description attribute, replace
        # the telephonenumber attribute with two values, and delete a specific
        # value from the facsimiletelephonenumber attribute
        dn: cn=Paula Jensen, ou=Product Development, dc=airius, dc=com
        changetype: modify
        add: postaladdress
        postaladdress: 123 Anystreet $ Sunnyvale, CA $ 94086
        -
        delete: description
        -
        replace: telephonenumber
        telephonenumber: +1 408 555 1234
        telephonenumber: +1 408 555 5678
        -
        delete: facsimiletelephonenumber
        facsimiletelephonenumber: +1 408 555 9876
        -

     * </pre>
     * @throws Exception
     */
    public void testModify()
            throws Exception
    {
        LdifEntry entry = new LdifEntry(new DN("cn=Paula Jensen,ou=Product Development,dc=airius,dc=com"));

        entry.setChangeType(LdifEntryType.modify);

        entry.put(new LdifModifyAttribute(new DXAttribute("postaladdress", "123 Anystreet $ Sunnyvale, CA $ 94086"), LdifModifyType.add));

        entry.put(new LdifModifyAttribute(new DXAttribute("description"), LdifModifyType.delete));

        entry.put(new LdifModifyAttribute(new DXAttribute("telephonenumber", new String[] {"+1 408 555 1234", "+1 408 555 5678"}), LdifModifyType.replace));

        entry.put(new LdifModifyAttribute(new DXAttribute("facsimiletelephonenumber", "+1 408 555 9876"), LdifModifyType.delete));

        // note spaces removed from DN, and order re-arranged to 'replace', 'add' and then 'delete.
        String rfcText = "" +
                "dn: cn=Paula Jensen,ou=Product Development,dc=airius,dc=com\n" +
                "changetype: modify\n" +
                "replace: telephonenumber\n" +
                "telephonenumber: +1 408 555 1234\n" +
                "telephonenumber: +1 408 555 5678\n" +
                "-\n" +
                "add: postaladdress\n" +
                "postaladdress: 123 Anystreet $ Sunnyvale, CA $ 94086\n" +
                "-\n" +
                "delete: description\n" +
                "-\n" +
                "delete: facsimiletelephonenumber\n" +
                "facsimiletelephonenumber: +1 408 555 9876\n" +
                "-\n\n";

        assertEquals("check add ldif equals rfc text", rfcText, entry.toString());
    }

    /**
     *
     * Example 6 continues...
     *
     * <pre>
     # Modify an entry's relative distinguished name
dn: cn=Paul Jensen, ou=Product Development, dc=airius, dc=com
changetype: modrdn
newrdn: cn=Paula Jensen
deleteoldrdn: 1

# Rename an entry and move all of its children to a new location in
# the directory tree (only implemented by LDAPv3 servers).
dn: ou=PD Accountants, ou=Product Development, dc=airius, dc=com
changetype: modrdn
newrdn: ou=Product Development Accountants
deleteoldrdn: 0
newsuperior: ou=Accounting, dc=airius, dc=com
     * </pre>
     * @throws Exception
     */
    public void testModDN()
            throws Exception
    {
        LdifEntry entry = new LdifEntry("cn=Paul Jensen,ou=Product Development,dc=airius,dc=com",
                   new Attribute[]{ new DXAttribute(LdifEntry.NEWRDN, "cn=Paula Jensen"),
                                    new DXAttribute(LdifEntry.DELETEOLDRDN, "1")});

        entry.setChangeType(LdifEntryType.modrdn);

        // note spaces removed from DN
        String rfcText = "" +
                "dn: cn=Paul Jensen,ou=Product Development,dc=airius,dc=com\n" +
                "changetype: modrdn\n" +
                "newrdn: cn=Paula Jensen\n" +
                "deleteoldrdn: 1\n\n";

        assertEquals("check add ldif equals rfc text", rfcText, entry.toString());


        entry = new LdifEntry("ou=PD Accountants,ou=Product Development,dc=airius,dc=com",
                     new Attribute[]{ new DXAttribute(LdifEntry.NEWRDN, "ou=Product Development Accountants"),
                                      new DXAttribute(LdifEntry.NEWSUPERIOR, "ou=Accounting, dc=airius, dc=com"),
                                      new DXAttribute(LdifEntry.DELETEOLDRDN, "0")});

          entry.setChangeType(LdifEntryType.modrdn);

          // note spaces removed from DN
          rfcText = "" +
                  "dn: ou=PD Accountants,ou=Product Development,dc=airius,dc=com\n" +
                  "changetype: modrdn\n" +
                  "newrdn: ou=Product Development Accountants\n" +
                  "deleteoldrdn: 0\n" +
                  "newsuperior: ou=Accounting, dc=airius, dc=com\n\n";

          assertEquals("check add ldif equals rfc text", rfcText, entry.toString());




    }
}