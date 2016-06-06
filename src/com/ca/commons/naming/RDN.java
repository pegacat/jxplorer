package com.ca.commons.naming;

//import java.util.Vector;

//import com.ca.commons.cbutil.*;

import javax.naming.InvalidNameException;

/**
 *    An RDN element.  May be multi-valued (but most aren't)<p>
 *
 *    Some quick definitions: <ul>
 *    <li>'raw' means an rdn with unescaped special
 *    characters in it, suitable for display to a user - cn=fred+nurk.
 *    <li>'escaped' means with special characters escaped in a form suitable
 *    for jndi transmission (leading '\' characters, and quad slashes '\\\\' for
 *    an escaped slash.
 *    <li>'jndireturn' means an incorrectly escaped string from a jndi query
 *    which requires special handling due to bugs (?) in jndi (as of java 1.3)
 *    </ul>
 *    Values are entered and stored in the RDN <i>escaped</i>, in an internal
 *    string ldapEscapedRDN.  Utility ftns
 *    are provided to translate rdns between these different forms, and functions
 *    creating/changing rdns should make sure always to pass the final (no escape
 *    character) form when using RDNs.<p>
 *
 *    While parts of an rdn (particular attributes and values) may be manipulated
 *    as raw, unescaped strings, entire rdns are always escaped when represented
 *    as strings (e.g. by the 'toString()' method).<p>
 *
 *    An added complication is unicode.  While strings may be entered as escaped
 *    utf8, they are always converted to unicode asap, and never returned as utf8.
 *    (they are automatically translated to utf8 by jndi when transmitted to the
 *    server, or manually by JXplorer when saving ldif files as a final step).
 *
 *    <p>The class is optimised for single valued RDNs, as this represents the
 *    majority of examples seen by the author...</p>
 *
 *    <p>This class uses delayed evaluation of RDN strings.  Hence invalid RDNs
 *    can be instantiated, and will only throw exceptions when used.</p>
 *
 *    @author Chris Betts
 */

// TODO: refactoring opportunity: javax.naming now includes Rdn class - evaluate as replacement for this class.
// TODO: suggest refactoring this class to extend Rdn, then consider if this class is still required.

public class RDN
{
    /**
     *    <p>This gives the positions of seperators ('+') or '1 past the end of string'
     *    for att-value sub elements in a multi-valued rdn.   The first element is
     *    always -1, the last is the length of the string.
     *    Usually length 2, since most RDNs are
     *    not multivalued.</p>
     *
     *    <pre>
     *     cn=Sue\,Grabbit\+Run+sn=Law         (27 characters)
     *    ^                    ^      ^
     *    |                    |      |
     *    -1                   20     27
     *    [0]                  [1]    [2]
     *    </pre>
     *
     *    Any rdn sub string (i) is thus element[i]+1 to element[i+1]
     */

    private int[] elements = null;

    /**
     *    The escaped ldap RDN (e.g. cn=Sue\,Grabbit\+Run+sn=Law)
     */

    private String ldapEscapedRDN;


    /**
     *    status constants of the RDN - whether it has been tested,
     *    and if it has, whether it is single valued or multi valued.
     */

    private int UNTESTED = 0, SINGLEVALUED = 1, MULTIVALUED = 2;

    /**
     *   the status of the RDN (one of UNTESTED|SINGLEVALUED|MULTIVALUED)
     */

    private int status = UNTESTED;

    /**
     *  Default number of allowable elements (before a manual array resize
     *  must be done).
     */

    private static int MAXELEMENTS = 16;  // maximum number of multi-valued attributes...

    /**
     *    Empty constructor - creates an RDN with no values.
     */

    public RDN() { ldapEscapedRDN = "";}


    private boolean debug = false;

    /**
     *    Standard constructor - creates an RDN using an ldap escaped utf8
     *    rdn string, which may be multi-valued.
     *    @param rdn the string rdn to be parsed
     */



    public RDN(String rdn)
    {
        if (rdn == null)
        {
            rdn = "";
        }
        else
        {
            // trim any unnecessary white space off the end...
            int len = rdn.length();

            if ((rdn.indexOf('\\') > -1) && (len >=2 && rdn.charAt(len-2) == '\\'  && rdn.charAt(len-1) == ' '))
            {
                    rdn = specialSpaceHandling(rdn); // pathalogical case
            }
            else
                rdn = rdn.trim();
        }

        ldapEscapedRDN = rdn;

        if (debug) System.out.println(" % NEW RDN: " + rdn);
    }

    /**
     *   clones an RDN.
     *   @param copyMe the RDN to copy.
     */

    public RDN(RDN copyMe)
    {
        this(copyMe.ldapEscapedRDN);
    }

    /**
     *  This RDN may have a special escaped space on the end...
     *  this method handles this (rare) case and cleans up the rdn
     *  in a process that takes a bit longer than normal.
     */

    // XXX this is kinda messy, and only works for single valued RDNs..  Can we come up with a nicer algorithm?

    private String specialSpaceHandling(String rdn)
    {
        // count the slashes...

        int finalPos = rdn.length() - 2;
        int pos = finalPos;

        // work backwards from the second last position, deleting slashes

        while (rdn.charAt(pos) == '\\')  // remember '\\' is a *single* slash!
        {
            pos--;
        }

        int numSlashesDeleted = finalPos - pos;

        int valuePos = rdn.indexOf('=')+1;
        String att = rdn.substring(0, valuePos);
        String val = rdn.substring(valuePos);

        if (numSlashesDeleted%2 == 0) // o.k. - we can trim that pesky space
        {
            val = val.trim();
        }                      // (otherwise leave it alone, it's
        else                       // escaped and meant to be there) - so
        {                       // just get rid of leading spaces...
            val = val.trim() + " ";
        }

        rdn = att + val;

        return rdn;
    }

    /**
     *    Whether the rdn is empty (i.e. is an empty string)
     */

    public boolean isEmpty()
    {
        return ("".equals(ldapEscapedRDN));
    }

    /**
     *    adds an ldap escaped utf8 Name element (i.e. the portion of an rdn separated by a '+' sign)
     *    @param rdnfragment an attribute = value pair.
     */

    public void addEscaped(String rdnfragment)
                 throws InvalidNameException
    {
        validate();  // throws InvalidNameException

        int equalpos = NameUtility.next(rdnfragment, 0, '=');
        // check rdn has at least one non null attribute and one non null value

        if (equalpos <= 0 || equalpos == rdnfragment.length()-1)
            throw new InvalidNameException("RDN.add(): invalid rdn fragment '" + ((rdnfragment==null)?"<null>":rdnfragment) + "' (can't find equal sign)");

        if (ldapEscapedRDN.length()>0)
            ldapEscapedRDN += "+" + rdnfragment;
        else
            ldapEscapedRDN = rdnfragment;

    }

    /**
     *    adds an unescaped unicode Name element (i.e. one of the parts seperated by a '+' sign).
     *    This will fail on multi-part elements (e.g. cn="fred" will work, cn="fred"+sn="erick" won't).
     *    XXX - this escapes the equals sign?
     *    @param rdnfragment an attribute = value pair.
     */

    public void addRaw(String rdnfragment)
                 throws InvalidNameException
    {
        int equalpos = NameUtility.next(rdnfragment, 0, '=');
        // check rdn has at least one non null attribute and one non null value

        if (equalpos <= 0 || equalpos == rdnfragment.length()-1)
            throw new InvalidNameException("RDN.addRaw(): invalid rdn fragment '" + ((rdnfragment==null)?"<null>":rdnfragment) + "' (can't find equal sign)");

        String attribute = rdnfragment.substring(0, equalpos);
        String value = rdnfragment.substring(equalpos+1);

        addEscaped(attribute + "=" + NameUtility.escape(value));
    }


    /**
     *    Returns the RDN as an ldap escaped ldap utf8 string.
     *    (This is a very inexpensive operation - it simply
     *    returns the pre-existing string.)
     *    @return the internal representation of the RDN as an ldap escaped string.
     */

    public String toString()
    {
        return ldapEscapedRDN;
    }

    /**
     *    Debug prints the raw, unescaped form of the elements.
     */

    public void dump()
    {
        if (status == UNTESTED)
            checkForMultiValued();

        System.out.println("DEBUG DUMP - RDN: " + ldapEscapedRDN + ((status==MULTIVALUED)?" MULTI VALUED":" SINGLE VALUED"));

        if (status == MULTIVALUED)
        {
            for (int i=0; i<(elements.length - 1); i++)
            {
                System.out.println("element-m (" + (elements[i]+1) + ") -> (" + elements[i+1] + ") " + i + ": " + getElement(i));
            }
        }
        else
        {
            System.out.println("element-s 0: " + ldapEscapedRDN);
        }

        Thread.currentThread().dumpStack();

    }

    /**
     *    Returns the Ith att-val pair in escaped ldap form.
     *    @param i the element index to get (counting from 0)
     *    @return the attribute value pair.
     */

    public String getElement(int i)
    {
        if (status == UNTESTED)
            checkForMultiValued();

        if (status == SINGLEVALUED && i==0)
            return ldapEscapedRDN;

        if (i<0 || elements == null || elements.length <= i+1)
            return "error VII";

        return ldapEscapedRDN.substring(elements[i]+1, elements[i+1]);
    }

    /**
     *    Returns all elements as a string array, in escaped ldap form.
     */

    public String[] getElements()
    {
        if (status == UNTESTED)
            checkForMultiValued();

        if (status == SINGLEVALUED)
            return new String[] {ldapEscapedRDN};

        if (elements == null)
            return new String[] {"error VIIB"};

        String[] elementArray = new String[elements.length-1];

        for (int i=0; i<(elements.length-1); i++)
            elementArray[i] = ldapEscapedRDN.substring(elements[i]+1, elements[i+1]);

        return elementArray;
    }

    /**
     *    Sets the Ith att-val pair in escaped ldap form.
     *    @param i the element index to get (counting from 0)
     *    @param ldapEscapedElement the element to replace (if the
     *           rdn is single valued, this would be the whole rdn.)
     */

    public void setElement(int i, String ldapEscapedElement)
        throws InvalidNameException
    {
        validate();

        if (status == SINGLEVALUED)
        {
            if (i==0)
                ldapEscapedRDN = ldapEscapedElement;
            else
                throw new InvalidNameException("cannot set non zero element of single valued rdn.");
        }
        else
        {
            if (i < 0 || i >= size())
                throw new InvalidNameException("attempt to set element " + i + " of rdn: '" + ldapEscapedRDN + "' (size = " + size() + ")");

            ldapEscapedRDN = ldapEscapedRDN.substring(0, elements[i]+1) +
                             ldapEscapedElement +
                             ldapEscapedRDN.substring(elements[i+1]);

            parseMultiValued();

        }
    }



    /**
     *    Gets the first attribute name.
     */

    public String getAttID()
    {
        return getAttID(0);
    }

    /**
     *    gets the attribute name from a particular indexed rdn element.
     */

    public String getAttID(int i)
    {
        if (status == UNTESTED)
            checkForMultiValued();

        if (status == SINGLEVALUED && i!=0)
            return "rdn error VIII";

        String element = getElement(i);

        int pos = element.indexOf('='); // no need for escape check, since att must be unescaped always.

        if (pos == -1) return "rdn error IX";

if (debug)
{
    System.out.println("Debug = " + debug);
    Thread.currentThread().dumpStack();
    System.out.println(" % RDN -> found attribute as '" + element.substring(0,pos) + "'");
}

        return element.substring(0, pos);
    }

    /**
     *    gets the attribute type names as a String array.
     *    @return an array of attribute types as a string; e.g. {'cn', 'uid'}
     */

    public String[] getAttIDs()
    {
        if (status == UNTESTED)
            checkForMultiValued();

        String[] atts = getElements();

        for (int i=0; i<atts.length; i++)
        {
            int pos = atts[i].indexOf('='); // no need for escape check, since att must be unescaped always.

            if (pos == -1) return new String[] {"rdn error IXB"};

            atts[i] = atts[i].substring(0, pos);
        }

        return atts;
    }




    /**
     *    Utility function - returns true if the passed attribute value
     *    is contained within the RDN.  This is case insensitive.
     *
     *    @param attributeType the Attribute name (e.g. "cn") to search for.
     *    @return true if it exists in the RDN, false if not.
     */

    public boolean contains(String attributeType)
    {
        if (status == UNTESTED)
            checkForMultiValued();

        if (attributeType == null || attributeType.length()==0)
            return false;

        for (int i=0; i<size(); i++)
            if (attributeType.equalsIgnoreCase(getAttID(i)))
                return true;

        return false;
    }

    /**
     *    Utility function - returns a raw attribute value looked up
     *    by name.  This search is case insensitive.  Note that this
     *    class is not optimized for this function .
     *
     *    @param attributeType the attribute type to find the corresponding
     *                         value for (e.g. "cn").
     *    @return String the corresponding value (e.g. "Fred"), or null
     *                   if there is no such value.
     */

    public String getRawVal(String attributeType)
    {
        if (status == UNTESTED)
            checkForMultiValued();

        if (attributeType == null || attributeType.length()==0)
            return null;

        for (int i=0; i<size(); i++)
            if (attributeType.equalsIgnoreCase(getAttID(i)))
                return getRawVal(i);

        return null;

    }

    /**
     *    Gets the first raw, unescaped, attribute value.
     */

    public String getRawVal()
    {
        return getRawVal(0);
    }

    /**
     *    gets  the raw, unescaped, attribute value from a particular indexed rdn element.
     */

    public String getRawVal(int i)
    {
        if (status == UNTESTED)
            checkForMultiValued();

        if (status == SINGLEVALUED && i!=0)
            return "rdn error X";

        String element = getElement(i);

        int pos = element.indexOf('='); // no need for escape check, since att must be unescaped always.

        if (pos == -1)
        {
            return "rdn error XI";
        }

        String raw = element.substring(pos+1);

        // since the value may be escaped, try to unescape it...
        try
        {
            return NameUtility.unescape(raw);
        }
        catch (Exception e)
        {
            return "rdn error XII";
        }
    }

    /**
     *    gets the attribute values as a String array.
     *    @return an array of attribute types as a string; e.g. {'cn', 'uid'}
     */

    public String[] getRawVals()
    {
        if (status == UNTESTED)
            checkForMultiValued();

        String[] vals = getElements();

        for (int i=0; i<vals.length; i++)
        {
            vals[i] = getRawVal(i);
        }

        return vals;
    }



    /**
     *    Sets the raw, unescaped value of uni-valued rdn.
     */

    public void setRawVal(String v)
        throws InvalidNameException
    {
        setRawVal(v, 0);
    }

    /**
     *    sets a raw, unescaped, value at a particular index position.
     */

    public void setRawVal(String v, int i)
        throws InvalidNameException
    {
        validate();

        String attval = getElement(i);
        String att = attval.substring(0, attval.indexOf('='));
        if (att == null || att.length()==0)
            throw new InvalidNameException("can't parse old RDN '" + ldapEscapedRDN);

        String newElement = att + "=" + NameUtility.escape(v);
        setElement(i, newElement);
    }

    /**
     *    returns the number of sub-elements in this rdn. (usually one!)
     */

    public int size()
    {
        if (status == UNTESTED)
            checkForMultiValued();

        return (status==SINGLEVALUED)?1:elements.length-1;
    }



   /**
    *    Returns whether the rdn is multi-valued.
    */

    public boolean isMultiValued()
    {
       if (status == UNTESTED)
           checkForMultiValued();

       return (status == MULTIVALUED);
    }



   /**
    *   Test two RDNs for equivalence.  Takes a bit of a shortcut -
    *   checks for case insensitive equivalence, doesn't check schema
    *   to see whether the value is case sensitive or not...i.e. the importance
    *   is low because currently we set delete RDN to false by default therefore
    *   you can't store two attributes in the dir that are only different b/c of their
    *   case.  Having delete RDN set to false means that in changing the case we will
    *   have two copies of it in the entry - which can't happen if their differences are
    *   just in their case (upper/lower).
    *   @param test the RDN to test this RDN against for equality.
    *   @return true if the two RDNs are the same, false otherwise.
    */

    public boolean equals(RDN test)
    {
        if (test == null)
            return false;
        else if (test.size() != size()) return false;

        if (isMultiValued())
        {
            // XXX complex equality test for multi valued RDNs
            // should be made here - e.g. ordering of RDN subelements
            // shouldn't be important... in the meantime, we'll cheat.
            //TE: Eventually this will come up as a bug...to do: sort the arrays
            //TE: so that we avoid saying that two multivalued RDNs are different if in fact they are
            //TE: the same but just ordered differently. [e.g cn=A+sn=B == sn=B+cn=A but this method will
            //TE: return false].

            // el hack.

            String[] atts = getAttIDs();
            String[] vals = getRawVals();               // get unescaped unicode value
            String[] testAtts = test.getAttIDs();
            String[] testVals = test.getRawVals();      // get unescaped unicode value

            for (int i=0; i<size(); i++)
                if (!elementsEqual(atts[i], testAtts[i], vals[i], testVals[i]) )
                    return false;

            return true;
        }
        else
        {
            return elementsEqual(getAttID(), test.getAttID(), getRawVal(), test.getRawVal());  // use unescaped unicode value
        }
    }



   /**
    *   Check that the two attributes are the same, and that the values
    *   are the same.  The values must be passed in *unescaped* (we use
    *   the unescaped form to get around the problem of having different
    *   types of utf-8/unicode/whatever floating around).
    *   @param att1 the attribute type of the first RDN.
    *   @param att2 the attribute type of the second RDN.
    *   @param val1 the attribute value of the first RDN.
    *   @param val2 the attribute value of the second RDN.
    *   @return true if the the attribute type and value of the first and second RDN are the same (ignoring case).
    */

    private boolean elementsEqual(String att1, String att2, String val1, String val2)
    {
/*  Multiple trailing white spaces will be cut off val2 but not val1...therefore this method will return false &
    try to mod DN which fails b/c DN already exists?  I would expect this to succeed.  If I trim val1
    a mod DN is not done but a modify is done - and again fails because a modify on a DN is not allowed??
    --JX doesn't allow the user to enter spaces at the end of values.  But it should handle values that already
    has a space at the end.  I think this multiple trailing white space thing could be a dir bug - the white
    spaces appear not to be escaped. eg...  baseObject: ou=AAA    \ ,o=DEMOCORP,c=AU.  Seems to work fine with
    correctly escaped single white space eg...baseObject: ou=AAA\ ,o=DEMOCORP,c=AU.
System.out.println(">>"+val1+"<<");
System.out.println(">>"+val2+"<<");
        val1=val1.trim();
System.out.println(">>"+val1+"<<");
System.out.println(">>"+val2+"<<");
*/

            if (att1.equalsIgnoreCase(att2) == false)
                return false;

            // XXX THIS ASSUME CASE INSENSITIVE MATCH!  (Really should check schema...)
            if (val1.equalsIgnoreCase(val2) == false)
                return false;

            return true;
    }



   /**
    *   Generic equality test allows for test against non-RDN objects
    *   via their 'toString()' and a case-insensitive match.
    */

    public boolean equals(Object o)
    {
        if (o == null)
            return false;
        if (o instanceof RDN)
            return equals((RDN)o);
        else
            return (ldapEscapedRDN.equalsIgnoreCase(o.toString()));
    }



    /**
     *    Test for multivaluedness by simply checking for unescaped
     *    plus symbols.  Does *not* validate the entire RDN, or do
     *    any syntax checking, but does break the rdn up into sub units.
     */

    private void checkForMultiValued()
    {
        if (status != UNTESTED) return;  // nothing to do

        if (NameUtility.next(ldapEscapedRDN, 0, '+') == -1)  // test for simplest case
        {
            status = SINGLEVALUED;
        }
        else  // things now get complicated and slow...
        {
            status = MULTIVALUED;
            parseMultiValued(MAXELEMENTS);
        }
    }

    /**
     *    Parse a multi valued RDN.
     */

    private void parseMultiValued()
    {
        parseMultiValued(MAXELEMENTS);
    }


    private void parseMultiValued(int max)
    {
        if (max > 512)
        {
            System.err.println("wierd error in RDN - attempt to parse RDN with more than 512 sub units???");
            return;
        }

        try
        {
            int[] temp = new int[max];

            temp[0] = -1;  // each element is one *before* the next attval pair, including the first

            int numElements = 0;
            int pos = 0;

if (debug) System.out.println("\n*** parsing multi valued rdn");
if (debug) System.out.println("parsing " + ldapEscapedRDN);
            while ((pos = NameUtility.next(ldapEscapedRDN, pos, '+'))>-1)
            {
                numElements++;
                temp[numElements] = pos;

if (debug) System.out.println("found " + numElements + " -th element at " + pos);

                int pos1, pos2;
                pos1 = temp[numElements-1] + 1;
                pos2 = temp[numElements];
if (debug) System.out.println(" = string " + pos1 + " -> " + pos2 + " = ");
if (debug) System.out.println(ldapEscapedRDN.substring(pos1, pos2));

                pos++;
            }

            numElements++;
            temp[numElements] = ldapEscapedRDN.length();

            int pos1, pos2;
            pos1 = temp[numElements-1] + 1;
            pos2 = temp[numElements];

if (debug) System.out.println("found " + numElements + " -th element at " + pos + " = string " +
             pos1 + " -> " + pos2 + " final len: " + ldapEscapedRDN.length());
if (debug) System.out.println(" = '" + ldapEscapedRDN.substring(pos1, pos2) + "'");



if (debug) System.out.println("found total of " + numElements + " elements...\n*****\n");

            elements = new int[numElements+1];
            System.arraycopy(temp, 0, elements, 0, numElements+1);
        }
        catch (IndexOutOfBoundsException e)
        {
if (debug) e.printStackTrace();
            System.err.println("huge number of multi-valued RDN units - increasing to: " + max*2);
            parseMultiValued(max*2);

        }
    }

    /**
     *    Checks whether the RDN is valid (i.e. has non null, correctly escaped elements).
     *    A (relatively) expensive operation.
     */

    public boolean validate()
    {
        try
        {
            if (status == UNTESTED)
                checkForMultiValued();

            if (isEmpty())        // *technically* an empty RDN isn't valid...
                return false;

            int noElements = size();
            for (int i=0; i<noElements; i++)
            {
                String att = getAttID(i);
                String val = getRawVal(i);

                if (att == null || att.length()==0)
                {
                    return false;
                }
                if (val == null || val.length()==0 || val.startsWith("error "))
                {
                    return false;
                }
            }
        }
        catch (Exception e)
        {
            return false;
        }

        return true; // XXX check whether the RDN is sane - i.e. parse it and confirm.
    }

}