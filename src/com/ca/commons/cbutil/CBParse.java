package com.ca.commons.cbutil;

/**
 * Methods being moved out of CBUtility for stand-alone-ishness
 */
public class CBParse
{
       /**
    *   Converts a string to hex then to a byte array.
    *   @param forConversion value to be converted.
    *   @return bytesConverted byte representation of the hex.
    */

    public static byte[] hex2bytes(String forConversion)
               throws NumberFormatException
    {
        char charForConversion[] = forConversion.toCharArray();

        byte[] bytesConverted = new byte[charForConversion.length/2];

        int a = 0;      //TE: loop counter.

        try
        {
            for (int i=0;i<charForConversion.length; i=i+2)     //TE: increments by two b/c two chars need to be parsed at a time.
            {
                bytesConverted[a] = CBParse.hex2Byte(charForConversion[i], charForConversion[i+1]);
                a++;
            }
        }
        catch (NumberFormatException e)
        {
            throw e;  // rethrow
        }
        catch (Exception e)     // what would these be?
        {
            CBUtility.error("Problem parsing hex string '" + forConversion + "' to bytes: " + e);
            throw new NumberFormatException("error parsing hex string '" + forConversion + "' " + e.getMessage());
        }
        return bytesConverted;
    }

    public static String bytes2Hex(byte[] bytes)
    {
        StringBuffer ret = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++)
        {
            ret.append(byte2Hex(bytes[i]));
        }
        return ret.toString();
    }


    public static String string2Hex(String orig)
    {
        StringBuffer ret = new StringBuffer(orig.length() * 2);
        char[] c = orig.toCharArray();
        for (int i = 0; i < c.length; i++)
        {
            ret.append(char2Hex(c[i]));
        }
        return ret.toString();
    }

    static public String byte2Hex(byte b)
    {
        // Returns hex String representation of byte b
        final char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] array = {hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f]};
        return new String(array);
    }

    static public String char2Hex(char c)
    {
        // Returns hex String representation of char c
        byte hi = (byte) (c >>> 8);
        byte lo = (byte) (c & 0xff);
        return byte2Hex(hi) + byte2Hex(lo);
    }

    static public byte hex2Byte(char hex1, char hex2)
            throws NumberFormatException
    {
        byte a = hexChar2Byte(hex1);
        byte b = hexChar2Byte(hex2);
        return (byte) ((a << 4) + b);
    }



    /**
     * Convert a single character to a byte...
     */

    static public byte hexChar2Byte(char hex)
            throws NumberFormatException
    {
        if (hex < '0')
            throw new NumberFormatException("invalid character for hex conversion: '" + hex + "'");
        else if (hex <= '9')
            return ((byte) (hex - 48)); // ('0' -> '9')
        else if (hex <= 'F')
            return ((byte) (hex - 55)); // ('A' -> 'F')
        else if (hex < 'a')
            throw new NumberFormatException("invalid character for hex conversion: '" + hex + "'");
        else if (hex <='f')
            return ((byte) (hex - 87)); // ('a' -> 'f')
        else
            throw new NumberFormatException("invalid character for hex conversion: '" + hex + "'");
    }

    /**
     * From Van Bui - prints out a hex string formatted with
     * spaces between each hex word of length wordlength.
     *
     * @param in         input array of bytes to convert
     * @param wordlength the length of hex words to print otu.
     */
    public static String bytes2HexSplit(byte[] in, int wordlength)
    {
        String hex = bytes2Hex(in);
        StringBuffer buff = new StringBuffer();

        for (int i = 0; i < hex.length(); i++)
        {
            buff.append(hex.charAt(i));
            if ((i + 1) % wordlength == 0)
                buff.append(" ");
        }

        return buff.toString();
    }

    /**
     * From Van Bui - prints out a hex string formatted with
     * spaces between each hex word of length wordlength, and
     * new lines every linelength.
     *
     * @param in         input array of bytes to convert
     * @param wordlength the length of hex words to print otu.
     * @param linelength the length of a line to print before inserting
     *                   a line feed.
     */

    public static String bytes2HexSplit(byte[] in, int wordlength, int linelength)
    {
        String hex = bytes2Hex(in);
        StringBuffer buff = new StringBuffer();

        for (int i = 0; i < hex.length(); i++)
        {
            buff.append(hex.charAt(i));
            if ((i + 1) % wordlength == 0)
                buff.append(" ");
            if ((i + 1) % linelength == 0)
                buff.append("\n");
        }

        return buff.toString();
    }

    /**
     * Determines whether a given byte sequence is a valid utf-8
     * encoding.  While this does not mean that the byte *is* a
     * utf-8 encoded string, the chance of a random byte sequence
     * happening to be utf8 is roughly (1/2 ** (byte array length)).<p>
     * Note that '7 bit ascii' is *always* a valid utf-8 string...<p>
     * see rfc 2279
     */
    public static boolean isUTF8(byte[] sequence)
    {
        boolean debug = false;
        if (debug) System.out.println("\n\n Starting UTF8 Check\n\n");
        int numberBytesInChar;

        for (int i = 0; i < sequence.length; i++)
        {
            byte b = sequence[i];
            if (debug) System.out.println("testing byte: " + byte2Hex(b));
            if (((b >> 6) & 0x03) == 2)
            {
                if (debug) System.out.println("start byte is invalid utf8 - has 10... start");
                return false;
            }
            byte test = b;
            numberBytesInChar = 0;
            while ((test & 0x80) > 0)
            {
                test <<= 1;
                numberBytesInChar++;
            }

            if (numberBytesInChar > 1)  // check that extended bytes are also good...
            {
                for (int j = 1; j < numberBytesInChar; j++)
                {
                    if (i + j >= sequence.length)
                    {
                        if (debug) System.out.println("following byte length is invalid - overruns end... ");
                        return false;           // not a character encoding - probably random bytes
                    }
                    if (debug) System.out.println("testing byte: " + byte2Hex(sequence[i + j]));
                    if (((sequence[i + j] >> 6) & 0x03) != 2)
                    {
                        if (debug) System.out.println("following byte is invalid utf8 - does *not* have 10... start");
                        return false;
                    }
                }
                i += numberBytesInChar - 1;  // increment i to the next utf8 character start position.
            }
        }

        return true;
    }

    /**
     * Determines whether a given byte sequence is a valid utf-8
     * encoding, encoding (at least in part) something *other* than
     * normal Ascii (i.e.
     * it is utf-8 encoding something that is not just 7-bit ascii,
     * which in utf-8 is indistinguishable from the original text).<p>
     * <p/>
     * While this does not mean that the bytes *are* a
     * utf-8 encoded string, the chance of a random byte sequence
     * (containing bytes with the high-bit set)
     * happening to be utf8 is roughly (1/2 ** (byte array length)).<p>
     * see rfc 2279
     */

    public static boolean isNonAsciiUTF8(byte[] sequence)
    {
        boolean nonAsciiDetected = false;

        int numberBytesInChar;
        for (int i = 0; i < sequence.length - 3; i++)
        {
            byte b = sequence[i];
            if (((b >> 6) & 0x03) == 2) return false;
            byte test = b;
            numberBytesInChar = 0;
            while ((test & 0x80) > 0)
            {
                test <<= 1;
                numberBytesInChar++;
            }

            // check if multi-byte utf8 sequence found
            if (numberBytesInChar > 1)  // check that extended bytes are also good...
            {
                nonAsciiDetected = true;
                for (int j = 1; j < numberBytesInChar; j++)
                {
                    if (((sequence[i + j] >> 6) & 0x03) != 2)
                        return false;
                }
                i += numberBytesInChar - 1;  // increment i to the next utf8 character start position.
            }
        }

        return nonAsciiDetected;
    }


    /**
     * This uses the implicit 'unicode marker' at the start of a
     * Unicode file to determine whether a file is a unicode file.
     * At the beginning of every unicode file is a two byte code
     * indicating the endien-ness of the file (either FFFE or FEFF).
     * If either of these sequences is found, this function returns
     * true, otherwise it returns false.  <i>Technically</i> this isn't
     * a sure test, since a) something else could have this signiture,
     * and b) unicode files are not absolutely required to have this
     * signiture (but most do).
     */

    public static boolean isUnicode(byte[] sequence)
    {
        if (sequence.length >= 2)
        {
            if (sequence[0] == (byte) 0xFF && sequence[1] == (byte) 0xFE) return true;
            if (sequence[0] == (byte) 0xFE && sequence[1] == (byte) 0xFF) return true;
        }
        return false;
    }

    /**
     * Turns a string into HTML displayable text by escaping
     * special characters ('<','&' etc...).
     * <p/>
     * ... add new ones as required; or see if an existing ftn somewhere
     * does this already...
     */

    public static String toHTML(String rawText)
    {
        String test;
        if (rawText.length() > 14)
            test = rawText.substring(0, 14).toLowerCase();
        else
            test = rawText.toLowerCase();

        if (test.startsWith("<html>") || test.startsWith("<!doctype html>"))
        {

// XXX this was commented out, but it seems to be necessaary/desirable?
            if (test.startsWith("<html>"))
                rawText = rawText.substring(6);
            else if (test.startsWith("<!doctype html>"))
                rawText = rawText.substring(15);

            if (rawText.toLowerCase().endsWith("</html>"))
            {
                rawText = rawText.substring(0, rawText.length() - 7);
            }

// END XXX

            return rawText;
        }
        char C;
        StringBuffer temp = new StringBuffer(rawText);

        for (int pos = 0; pos < temp.length(); pos++)
        {
            C = temp.charAt(pos);

            switch (C)
            {
                case '<':
                    replaceChar(temp, pos, "&lt;");
                    break;
                case '>':
                    replaceChar(temp, pos, "&gt;");
                    break;
                case '&':
                    replaceChar(temp, pos, "&amp;");
                    break;
                case '\"':
                    replaceChar(temp, pos, "&quot;");
                    break;
                case '#':
                    replaceChar(temp, pos, "&#35;");
                    pos++;
                    break;
            }
        }
        return temp.toString();
    }

    /**
     * Deletes a character in <i>text</i> at position <i>pos<i> and replaces
     * it with the string <i>replacement</i>.
     *
     * @param text        the text to be modified
     * @param pos         the position of the character to be deleted
     * @param replacement the string the character is to be replaced with.
     */

    public static int replaceChar(StringBuffer text, int pos, String replacement)
    {
        text.deleteCharAt(pos);
        text.insert(pos, replacement);
        return (pos + replacement.length());
    }

    /**
     * Deletes all characters <i>c</i> in <i>text</i> replaces
     * it with the string <i>replacement</i>.
     *
     * @param text        the text to be modified
     * @param replacement the string the character is to be replaced with.
     */

    public static String replaceAllChar(StringBuffer text, char c, String replacement)
    {
        return replaceAllBufferChar(text, c, replacement).toString();
    }

    /**
     * Deletes all characters <i>c</i> in <i>text</i> replaces
     * it with the string <i>replacement</i>.
     *
     * @param text        the text to be modified
     * @param replacement the string the character is to be replaced with.
     */

    public static StringBuffer replaceAllBufferChar(StringBuffer text, char c, String replacement)
    {
        int pos = 0;
        while (pos != -1)
        {
            pos = text.toString().indexOf(c, pos);
            if (pos != -1)
                pos = replaceChar(text, pos, replacement);
        }
        return text;
    }

    /**
     * Deletes a substring in <i>text</i> at position <i>pos<i>, of length <i>len</i> and replaces
     * it with the string <i>replacement</i>.
     *
     * @param text        the text to be modified
     * @param pos         the position of the character to be deleted
     * @param replacement the string the character is to be replaced with.
     */

    public static int replaceString(StringBuffer text, int pos, int len, String replacement)
    {
        text.replace(pos, pos + len, replacement);
        //text.delete(pos, pos+len);
        //text.insert(pos, replacement);
        return (pos + replacement.length());
    }

    /**
     * Deletes all characters <i>orig</i> in <i>text</i> and replaces
     * it with the string <i>replacement</i>.
     *
     * @param text        the text to be modified
     * @param orig        the original text substring to be changed
     * @param replacement the string the original substring is to be replaced with.
     */

    public static String replaceAllString(StringBuffer text, String orig, String replacement)
    {
        return replaceAllBufferString(text, orig, replacement).toString();
    }

    /**
     * Deletes all characters <i>orig</i> in <i>text</i> replaces
     * it with the string <i>replacement</i>.
     *
     * @param text        the text to be modified
     * @param orig        the original text substring to be changed
     * @param replacement the string the original substring is to be replaced with.
     */

    public static StringBuffer replaceAllBufferString(StringBuffer text, String orig, String replacement)
    {
        int pos = 0;
        while (pos != -1)
        {
            pos = text.toString().indexOf(orig, pos);
            if (pos != -1)
                pos = replaceString(text, pos, orig.length(), replacement);
        }
        return text;
    }


    /**
     * Utility for micro-parser.  Gets the next character pos in a string
     * after an initial offset that either matches, or does not match, <i>any</i>
     * of a set of comparison characters.
     *
     * @param pos      the position to start searching from
     * @param searchMe the string to search
     * @param compare  a string containing characters to compare against
     * @param match    whether the match is for characters in the compare string (true)
     *                 or <i>not</i> in the compare string (false)
     * @return the position found, or -1 if no position is found.
     */

    public static int nextCharIn(int pos, String searchMe, String compare, boolean match)
    {
        char test;
        int length = searchMe.length();
        while (pos < length)
        {
            test = searchMe.charAt(pos);
            if ((compare.indexOf(test) != -1) == match)
                return pos;
            pos++;
        }
        return -1;
    }

    public static boolean isPrintableAscii(String str)
    {
        if (str == null)
            return false;

        for (byte b:str.getBytes())
            if (b < 32 && b > 126)
                return false;

        return true;
    }

}
