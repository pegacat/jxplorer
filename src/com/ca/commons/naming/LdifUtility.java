package com.ca.commons.naming;

import com.ca.commons.cbutil.CBBase64;
import com.ca.commons.cbutil.CBParse;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * collection of static utility ftns. for
 * writing and reading ldif files.  Currently does not
 * handle URLs properly, but will do base64 encoding
 * quite happily given half a chance...
 */

//TODO: add 'version 2' handling to ldif file when using xml / ldif, and setup config parameter.
//TODO: would be nice to be able to save text in human readible form...

public class LdifUtility
{
    private static boolean debug = false;

    private static boolean handleXML = false;

    private Hashtable params = null;    // list of expandable strings for the ldif file, used during file parsing
    // e.g. KEY: <base_dn>, KEY VALUE: "o=eTrust, cn=Users"
    private String filedir = null;

    //public static String cr = System.getProperty("line.separator", "\n");
    public static String cr = "\n";

    private final static Logger log = Logger.getLogger(LdifUtility.class.getName());

    private int lineNumber = 0;  // used for file debugging.

    private String fileName = ""; // used for file debugging

    public LdifUtility()
    {
    }

    /**
     * sets up error information to allow debugging of ldif files
     */
    public void resetErrorReportingInformation(String ldifFileName)
    {
        lineNumber = 0;
        fileName = ldifFileName;
    }

    /**
     * Sets whether to support the draft rfc for special handling of XML data in LDIF files.
     *
     * @param state
     */
    public static void setSupportXML_LDIF_RFC(boolean state)
    {
        handleXML = state;
    }

    /**
     * Constructor
     *
     * @param params  - hashtable with the list of strings that will have to be substituted in the ldif file
     * @param filedir - ldif file directory, used to find the input files specified in the ldif stream
     */
    public LdifUtility(Hashtable params, String filedir)
    {
        this.params = params;
        this.filedir = filedir;
    }

    /**
     * Utility method for debugging
     *
     * @return the last read line of the ldif file.
     */
    public int getCurrentLineNumber()
    { return lineNumber;}


    /**
     * Set the ldif filepath - used to find input files
     *
     * @param filedir file path
     */
    public void setFileDir(String filedir)
    {
        this.filedir = filedir + "\\";
    }

    /**
     * Set the ldif file parameters
     *
     * @param params list of parameters
     */
    public void setParams(Hashtable params)
    {
        this.params = params;
    }

    /**
     * This writes a value as an ldif base64 encoded string.
     *
     * @param attributeValue the object to be ldif encoded
     * @return the ldif encoding (possibly base64) with appropriate colons.
     */
    public String ldifEncodeAsBinary(String attributeID, Object attributeValue)
    {
        StringBuffer ldifData = new StringBuffer(attributeID).append(":: ");
        int offset = attributeID.length() + 3;
        if (attributeValue.getClass().isArray())
        {
            try
            {
                byte b[] = (byte[]) attributeValue;


                ldifData.append(CBBase64.binaryToString(b, offset)).append("\n");
                return ldifData.toString();
            }
            catch (ClassCastException e)
            {
                log.warning("unable to cast array to byte array.");
            }
        }

        // it's not a byte array; force it to a string, read as bytes,
        // and code those.  This will work in most cases, but will 
        // fail badly for isNonString data that has not been encoded properly
        // already; e.g. a gif file should (probably) be translated to a 
        // byte array before being passed to this ftn.    

        ldifData.append(CBBase64.binaryToString(attributeValue.toString().getBytes(), offset)).append("\n");
        return ldifData.toString();
    }

    /**
     * This encodes a single attribute value in an ldif file.  It will be called
     * repeatedly for the same attribute if the att is multi-valued.
     * <p/>
     * E.g. objectclass = {top, person, orgPerson} would be called three times
     * returning each of:
     * objectclass: top
     * objectclass: person
     * objectclass: orgPerson
     *
     * @param attributeID
     * @param attributeValue
     * @return encoded att val pair (may be multi-line for base64 or long text) - includes '\n' for valid text - returns empty string for invalid values.
     */
    public static String ldifEncode(String attributeID, Object attributeValue)
    {
        StringBuffer ldifData = new StringBuffer();
        String val = ldifEncode(attributeValue, attributeID.length());
        if (val == null || val.length() == 0)
            return "";
        ldifData.append(attributeID).append(val).append("\n");
        return ldifData.toString();
    }

    /**
     * This is used to write a value that is *probably* normal
     * string encoded, but *may* need to be base64 encoded or xml encoded.  It
     * checks the string against the requirements of draft-good-ldap-ldif-04
     * (initial character sane, subsequent characters not null, CR or LF),
     * and returns the appropriate string, with appropriate ': ' or ':: '
     * prefix.  It also supports the draft 'Extended LDAP Data Interchange Foramt'
     * handling of xml text that is identified by an initial '&lt;?xml ' string.
     *
     * @param o      the object to be ldif encoded
     * @param offset The first line of the string may be offset by
     *               by this many characters for nice formatting (e.g. in
     *               an ldif file, the first line may include 'att value = ...'
     *               at the beginning of the base 64 encoded block).
     * @return the ldif encoding (possibly base64) with appropriate colons.
     */


    public static String ldifEncode(Object o, int offset)
    {
        boolean base64Encode = false;

        boolean xmlEncode = false;

        if ((o instanceof String) == false)
        {
            if (o.getClass().isArray())     // treat as byte array
            {
                try
                {
                    byte b[] = (byte[]) o;
                    String ret = ":: " + CBBase64.binaryToString(b, offset + 3);
                    return ret;
                }
                catch (ClassCastException e)
                {
                    log.warning("unable to cast array to byte array.");
                    return null;
                }
            }
            // treat as a string anyway
        }
        String s = o.toString();
        int len = s.length();

        if (len == 0)
            return ": ";  // this shouldn't really happen; null attributes should be culled before we get here...


        // run the rfc tests to see if this is a good and virtuous string
        char startChar = s.charAt(0);
        if ("\n\r :".indexOf(startChar) != -1)    // check for safe start char
            base64Encode = true;
        else if (startChar == '<')
        {
            if (handleXML == true && s.startsWith("<?xml "))  // we have xml text, and will cope with weird characters differently.
                xmlEncode = true;
            else
                base64Encode = true;
        }
        else
        {
            char test[] = new char[len];
            s.getChars(0, len, test, 0);
            for (int i = 0; i < len; i++)
            {
                //System.out.println("checking: " + i + ": " + test[i] + " = " + CBUtility.charToHex(test[i]));
                if (test[i] > 126 || test[i] < 32)    // check for sane intermediate chars
                {
                    base64Encode = true;          // (may be unicode international string)
                    break;
                }
            }
        }

        if (s.charAt(s.length() - 1) == ' ')        // end space considered harmful
            base64Encode = true;

        if (base64Encode)
            return ":: " + translateToLdifBase64(s, offset);
        else if (xmlEncode)
            return ": " + translateToLdifXML(s);
        else
            return ": " + s;                        // return unmodified string.

    }

    /**
     * translates a string to base 64, supressing redundant errors.
     *
     * @param s
     * @param offset
     * @return base64 value of string.
     */

    private static String translateToLdifBase64(String s, int offset)
    {
        try
        {
            s = CBBase64.binaryToString(s.getBytes("UTF8"), offset + 3);
        }
        catch (UnsupportedEncodingException e) // why would we get this when utf8 is mandatory across all java platforms?
        {
            log.log(Level.WARNING, "error utf8 encoding strings...", e);
            s = CBBase64.binaryToString(s.getBytes(), offset + 3);
        }
        return s;
    }


    /**
     * This replaces new lines (of various sorts) with a 'new line' + '>' character, as per 'xml in ldif' draft rfc.
     *
     * @param s the string to translate
     * @return the translated string
     */
    private static String translateToLdifXML(String s)
    {
        StringBuffer xml = new StringBuffer(";transfer-rxer>:").append(cr).append(s);

        // carriage return madness :-(.  Runs fastest on systems that use '\n' only.  Seems about right.

        if (s.indexOf("\r") != -1)  // mac (I think)
        {
            CBParse.replaceAllBufferString(xml, "\r", "\r>");
        }
        if (s.indexOf("\n") != -1)  // catches both '\r\n' and '\n'
        {
            CBParse.replaceAllBufferString(xml, "\n", "\n>");
        }

        return xml.toString();
    }


    /**
     *    Writes a single ldif entry...
     *
     */
    /**
     * retrieves a single entry from the directory and writes it
     * out to an ldif file.  Note that ldif header 'version 1' must
     * have already been written elsewhere...
     *
     * @param entry             the entry being written
     * @param saveFile          the file to write the entry to
     * @param originalPrefix    an optional portion of the dn to update
     * @param replacementPrefix an optional replacement for a portion of the dn
     */

    public void writeLdifEntry(DXEntry entry, FileWriter saveFile, String originalPrefix, String replacementPrefix)
            throws NamingException, IOException
    {

        /**
         *    Prefix replacement magic.  If we are moving the tree during
         *    the save, and a different prefix has been given (i.e. the
         *    originalPrefix and replacementPrefix variables aren't zero)
         *    we switch the relavent portion of the saved DN, substituting
         *    the portion of the DN that contains the original prefix with
         *    the replacement.
         *    e.g. cn=Fredo,o=FrogFarm,c=au, with original prefix o=FrogFarm,c=au
         *         and replacement o=FreeFrogs,c=au, becomes cn=Fredo,o=FreeFrogs,c=au
         */

        // see if we actually need to replace anything...
        String dn = entry.getStringName();
        if ((originalPrefix != null) && (dn.endsWith(originalPrefix)) && (originalPrefix.equals(replacementPrefix)==false))
        {
            if (debug) System.out.println("original DN = '" + dn + "'");
            dn = dn.substring(0, dn.length() - originalPrefix.length()) + replacementPrefix;
            if (debug) System.out.println("after replacement DN = '" + dn + "'");
            entry.setDN(new DN(dn));
        }


        String ldifData = new LdifEntry(entry).toString();


        if (debug)
            System.out.println(ldifData);
        else
        {
            saveFile.write(ldifData);
            saveFile.flush();
        }
    }

    /**
     * Parse an attribute: value line of an ldif file, and place
     * the attribute value pair in an DXDXEntry object.
     *
     * @param attributeText a complete ldif text line (unwrapped) to parse
     * @param newEntry           the partially created DXDXEntry, which is modified by this
     *                           method.  - May have a null name on error
     * @throws NamingException if there is an error reading the ldif text
     */


    public void ldifDecode(String attributeText, LdifEntry newEntry)
            throws NamingException
    {
        if (attributeText.startsWith("-"))  // this is the separator for changetype:modify attribute operations... I don't think we actually need to handle it though?
            return;

        Object[] attributeValuePair = ldifDecodeAttribute(attributeText);
        if (attributeValuePair == null || attributeValuePair[1]==null) // skip empty / erroneous values.
            return;

        String attributeID = (String) attributeValuePair[0];
        Object value = attributeValuePair[1];

        if ("dn".equalsIgnoreCase(attributeID))
        {
            DN dn = new DN(value.toString());
            newEntry.setDN(dn);

            if (dn.error())
            {
                log.severe("error reading ldif DXEntry: " + "(" + lineNumber + ") in LDIF file: + " + fileName + " value: " + value + " - skipping");
                newEntry.setDN(null);
            }
        }
        else if ("changetype".equalsIgnoreCase(attributeID))
        {
            String designatedChangeType = value.toString();
            for (LdifEntryType entryType: LdifEntryType.values())
                if (designatedChangeType.equals(entryType.toString()))
                {
                    newEntry.setChangeType(entryType);
                    return;
                }

            throw new NamingException("unknown change type in entry: " + newEntry.getStringName() + " changetype: " + designatedChangeType);
        }
        else if ("add".equalsIgnoreCase(attributeID))  // LDIF modify directive - this seeds the attribute, values will get filled in normally below
        {
            newEntry.put(new LdifModifyAttribute(value.toString(), LdifModifyType.add));
        }
        else if ("replace".equalsIgnoreCase(attributeID)) // LDIF modify directive - this seeds the attribute, values will get filled in normally below
        {
            newEntry.put(new LdifModifyAttribute(value.toString(), LdifModifyType.replace));
        }
        else if ("delete".equalsIgnoreCase(attributeID))  // LDIF modify directive - this seeds the attribute, optional values will get filled in normally below
        {
            newEntry.put(new LdifModifyAttribute(value.toString(), LdifModifyType.delete));
        }
        else if (attributeID != null)
        {
            Attribute existing = newEntry.get(attributeID);

            if (existing == null)
            {
                DXAttribute att = new DXAttribute(attributeID, value);
                newEntry.put(att);
            }
            else
            {
                existing.add(value);
                newEntry.put(existing);
            }
        }
    }


    public Object[] ldifDecodeAttribute(String attributeText)
            throws NamingException
    {
        int breakpos = attributeText.indexOf(':');
        if (breakpos < 0)
            throw new NamingException("Error - no ':' separator found in attribute text");

        String attributeID = attributeText.substring(0, breakpos);
        Object value = null;

        int attLen = attributeID.length();

        // auto-translateToDisplayTitle 'oc' to 'objectClass'
        if (attributeID.equals("oc")) attributeID = "objectClass";

        int startpos = 2;

        if (attributeText.length() <= breakpos + 1)  // empty value
        {
            value = "";
        }
        else if (attributeText.charAt(breakpos + 1) == ':')  // check for base64 encoded isNonString
        {
            value = getBase64Value(attributeText, attLen + 3);  // may return string or byte array!
        }
        else
        {
            if (attributeText.charAt(attLen + 1) != ' ') // again, may be a leading space, or may not...
                startpos = 1;
            value = attributeText.substring(attLen + startpos);

            // expand the value parameters, including the urls
            value = expandValueParams(value);

        }

        return new Object[]{attributeID, value};
    }

    /**
     * rewrite to kick off explicitly from the start of the base64 text.
     */
    private Object getBase64Value(String parseableLine, int startpos)
    {
        if (parseableLine == null)
            throw new NullPointerException("null 'parseableLine' passed to getBase64Value");

        byte[] rawBinaryData;

        String base64text = "";

        if (parseableLine.charAt(startpos) == ' ') // may be ::XXXX or :: XXXX -> so must adjust for possible space
            startpos++;

        base64text = parseableLine.substring(startpos);

        rawBinaryData = CBBase64.stringToBinary(base64text);

        if (rawBinaryData == null)
            //throw new NullPointerException("Unable to parse base64text:\n" + base64text);
            throw new NullPointerException("Null Pointer exception parsing line (" + lineNumber + ") in LDIF file: + " + fileName + "\n line: " + base64text);

        // a bit dodgy - we try to guess whether the isNonString data is UTF-8, or is really isNonString...
        // we should probably do some schema checking here, but instead we'll try to make an educated
        // guess...

        // Create a short array to test for utf-8 ishness... (we don't want to test all of large text files)
        byte[] testBytes;
        if (rawBinaryData.length > 256)
        {
            testBytes = new byte[256];
            System.arraycopy(rawBinaryData, 0, testBytes, 0, 256);
        }
        else
            testBytes = rawBinaryData;

        /*
        *    Make a (slightly ad-hoc) check to see if it is actually a utf-8 string *pretending* to by bytes...
        */

        if (CBParse.isUTF8(testBytes))
        {
            try
            {
                return new String(rawBinaryData, "UTF-8");
            }
            catch (Exception e)  // as per String constructor doco, behaviour is 'unspecified' if the above fails...
            {
                // drop through to return the raw isNonString data instead...
            }
        }
        return rawBinaryData;
        //}
        /*
        remove below?
        catch (NullPointerException e)
        {
            System.out.println("*** Exception parsing LDIF file");
            e.printStackTrace();
            System.out.println("***");

            throw new NullPointerException("Null Pointer exception parsing line (" + lineNumber +") in LDIF file: + " + fileName + "\n line:\n" + parseableLine + "\n(starpos:" + startpos + ")\n (was: " + e.getMessage() + ")");
           // throw new Exception("error parsing line in LDIF file: " + parseableLine, e);
        }
        */

    }



    /**
     * Read an DXEntry from LDIF text. Attribute/value pairs are read until
     * a blank line is encountered.
     *
     * @param textReader a buffered Reader and progress monitor to read lines of ldif text from...
     * @return the read DXEntry, as an DXEntry object, or null when we've finished the file - MAY HAVE A NULL DN ON ERROR
     * @throws InterruptedIOException if the user hits cancel on the progress bar
     */

    public LdifEntry readLdifEntry(LdifStreamReader textReader)
            throws IOException, NamingException
    {
        try
        {
            LdifEntry ldifEntry = new LdifEntry();

            String line;
            
            while ((line = textReader.readLine()) != null)
            {
                if (line.startsWith("version")) {}    // ignore
                else if (line.startsWith("#"))  {}    // comment - ignore
                else if (line.length()==0)            // end of current LDIF entry (there may be more)
                {
                    return ldifEntry;                 // usual exit point for function
                }
                else
                {
                    try
                    {
                        ldifDecode(line, ldifEntry);     // add the next attribute (or dn) to the entry
                    }
                    catch (NamingException e)
                    {
                        log.warning("error in LDIF file line: " + textReader.currentLine);
                    }
                }
            }

            if (line == null && ldifEntry.size()==0)  // return null when we've reached the end of the file and have no more entries.
                return null;

            return ldifEntry;  // finished reading file...
        }
        catch (IOException e)
        {
            throw new IOException("Error reading LDIF File '" + fileName + "' line: " + textReader.getLineNumber() + "\n" + e.getMessage(), e);
        }
    }

    


    /**
     * This method expands the strings inside the ldif file
     * that match the list of expandable strings in params list.
     *
     * @param value value to be expanded
     * @return expanded object
     */
    public Object expandValueParams(Object value)
    {
        if (params != null)
        {
            Enumeration keys = params.keys();
            while (keys.hasMoreElements())
            {
                String key = (String) keys.nextElement();
                String keyvalue = (String) params.get(key);

                // check for the key
                String oldValue = (String) value;
                int index = oldValue.indexOf(key);
                if (index > -1)
                {
                    String newValue = oldValue.substring(0, index) + keyvalue +
                            oldValue.substring(index + key.length(), oldValue.length());
                    value = newValue;
                }
            }
        }

        // load the file if the value is a url
        if (filedir != null)
        {
            // check if it is a file, i.e. look for "< file:"
            String oldValue = (String) value;
            String match = "< file://";

            int index = (oldValue.toLowerCase()).indexOf(match);

            if (index > -1)
            {
                String filename = filedir + oldValue.substring(index + 9, oldValue.length());
                File file = new File(filename);
                try
                {
                    FileInputStream input = new FileInputStream(file);

                    int length = (int) file.length();
                    if (length > 0)
                    {
                        byte[] bytes = new byte[length];
                        int read = input.read(bytes);
                        if (read > 0) value = bytes;
                    }
                    input.close();
                }
                catch (IOException e)
                {
                    log.warning("Error opening ldif included file!" + e);
                }
            }
        }
        return value;
    }
}