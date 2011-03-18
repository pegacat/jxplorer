package com.ca.commons.naming;

/**
 * This code lovingly written by:
 * User: betch01
 * Date: 22/07/2003
 */

import junit.framework.*;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Arrays;

public class DXAttributeTest extends TestCase
{

    public DXAttributeTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(DXAttributeTest.class);
    }

    public static void main (String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public void testNonStringCheck()
    {
        assertTrue( DXAttribute.isStringSyntax(null));
        assertFalse( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.5"));
        assertFalse( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.28"));
        assertFalse( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.40"));
        assertFalse( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.4"));
        assertFalse( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.8"));
        assertFalse( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.9"));
        assertFalse( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.10"));

        assertFalse( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.4{112}"));
        assertFalse( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.8{88}"));
        assertFalse( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.9{0}"));

        assertFalse( DXAttribute.isStringSyntax("SYNTAX 1.3.6.1.4.1.1466.115.121.1.8"));
        assertFalse( DXAttribute.isStringSyntax("SYNTAX 1.3.6.1.4.1.1466.115.121.1.40"));
        assertFalse( DXAttribute.isStringSyntax("SYNTAX 1.3.6.1.4.1.1466.115.121.1.4{777}"));

        assertTrue( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.41") );
        assertTrue( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.42") );
        assertTrue( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.48") );
        assertTrue( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.49") );

        assertTrue( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.41{99}") );
        assertTrue( DXAttribute.isStringSyntax("1.3.6.1.4.1.1466.115.121.1.42{512}") );

        assertTrue( DXAttribute.isStringSyntax("SYNTAX 1.3.6.1.4.1.1466.115.121.1.49") );
        assertTrue( DXAttribute.isStringSyntax("SYNTAX 1.3.6.1.4.1.1466.115.121.1.41{99}") );
        assertTrue( DXAttribute.isStringSyntax("SYNTAX 1.3.6.1.4.1.1466.115.121.1.42{512}") );
    }

    public void testASN1Check()
    {
        assertTrue( DXAttribute.isASN1Syntax(null) == false);
        assertTrue( DXAttribute.isASN1Syntax("1.3.6.1.4.1.1466.115.121.1.5"));
        assertTrue( DXAttribute.isASN1Syntax("1.3.6.1.4.1.1466.115.121.1.8"));
        assertTrue( DXAttribute.isASN1Syntax("1.3.6.1.4.1.1466.115.121.1.9"));
        assertTrue( DXAttribute.isASN1Syntax("1.3.6.1.4.1.1466.115.121.1.10"));

        assertTrue( DXAttribute.isASN1Syntax("1.3.6.1.4.1.1466.115.121.1.5{112}"));
        assertTrue( DXAttribute.isASN1Syntax("1.3.6.1.4.1.1466.115.121.1.8{88}"));
        assertTrue( DXAttribute.isASN1Syntax("1.3.6.1.4.1.1466.115.121.1.9{0}"));

        assertTrue( DXAttribute.isASN1Syntax("SYNTAX 1.3.6.1.4.1.1466.115.121.1.5"));
        assertTrue( DXAttribute.isASN1Syntax("SYNTAX 1.3.6.1.4.1.1466.115.121.1.8"));
        assertTrue( DXAttribute.isASN1Syntax("SYNTAX 1.3.6.1.4.1.1466.115.121.1.9{777}"));

        assertTrue( DXAttribute.isASN1Syntax("1.3.6.1.4.1.1466.115.121.1.28") == false);
        assertTrue( DXAttribute.isASN1Syntax("1.3.6.1.4.1.1466.115.121.1.40") == false);
        assertTrue( DXAttribute.isASN1Syntax("1.3.6.1.4.1.1466.115.121.1.48") == false);
        assertTrue( DXAttribute.isASN1Syntax("1.3.6.1.4.1.1466.115.121.1.49") == false);

        assertTrue( DXAttribute.isASN1Syntax("1.3.6.1.4.1.1466.115.121.1.28{99}") == false);
        assertTrue( DXAttribute.isASN1Syntax("1.3.6.1.4.1.1466.115.121.1.40{512}") == false);

        assertTrue( DXAttribute.isASN1Syntax("SYNTAX 1.3.6.1.4.1.1466.115.121.1.49") == false);
        assertTrue( DXAttribute.isASN1Syntax("SYNTAX 1.3.6.1.4.1.1466.115.121.1.40{99}") == false);
        assertTrue( DXAttribute.isASN1Syntax("SYNTAX 1.3.6.1.4.1.1466.115.121.1.28{512}") == false);

    }

    public void testBasicAttributeFunctionality() throws NamingException
    {
        DXAttribute newAtt = new DXAttribute("fakeName", new DXNamingEnumeration(new ArrayList(Arrays.asList(new String[] {"a", "b", "c", "d"}))));

        assertTrue( "a".equals(newAtt.get()));
        assertTrue( "d".equals(newAtt.get(3)));
    }
}
