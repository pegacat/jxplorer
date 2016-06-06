package com.ca.commons.naming;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.naming.directory.BasicAttribute;

/**
 * Tests Ldif Modify Attributes print out correctly as per RFC 2849
 *
 * (e.g. the 'add|delete|replace' elements below)
 *
 *<pre>
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
 </pre>
 *
 * (c) Chris Betts; Pegacat Software (http://pegacat.com)
 */
public class LdifModifyAttributeTest extends TestCase
{

    public LdifModifyAttributeTest(String name)
    {
        super(name);
    }


    public static Test suite()
    {
        return new TestSuite(LdifModifyAttributeTest.class);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(LdifModifyAttributeTest.suite());
    }



    public void testAddAttribute()
    {
        LdifModifyAttribute add = new LdifModifyAttribute(new BasicAttribute("postaladdress", "123 Anystreet $ Sunnyvale, CA $ 94086"), LdifModifyType.add);

        assertEquals("check ldif add structure",
                "add: postaladdress\n" +
                "postaladdress: 123 Anystreet $ Sunnyvale, CA $ 94086\n" +
                "-\n",
                add.toString());
    }

    public void testDeleteAttribute()
    {
        LdifModifyAttribute delete = new LdifModifyAttribute(new BasicAttribute("facsimiletelephonenumber", "+1 408 555 9876"), LdifModifyType.delete);

        assertEquals("check ldif attribute value delete structure",
                "delete: facsimiletelephonenumber\n" +
                "facsimiletelephonenumber: +1 408 555 9876\n" +
                "-\n",
                delete.toString());

        delete = new LdifModifyAttribute(new BasicAttribute("description"), LdifModifyType.delete);

        assertEquals("check ldif full attribute delete structure",
                "delete: description\n" +
                "-\n",
                delete.toString());

    }

    public void testReplaceAttribute()
    {
        LdifModifyAttribute replace = new LdifModifyAttribute(new DXAttribute("telephonenumber", new String[] {"+1 408 555 1234", "+1 408 555 5678"}), LdifModifyType.replace);

        assertEquals("check ldif replace structure",
                "replace: telephonenumber\n" +
                "telephonenumber: +1 408 555 1234\n" +
                "telephonenumber: +1 408 555 5678\n" +
                "-\n",
                replace.toString());
    }

}

