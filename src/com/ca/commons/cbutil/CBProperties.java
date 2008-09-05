package com.ca.commons.cbutil;

import java.io.*;
import java.util.*;

/**
 * <p>A hack class for formatting.  The java Properties class is astonishingly broken in too many ways to
 * enumerate.  This is the beginnings of an attempt to migrate to something less laughable.  It is based
 * on the sun code, which thoughtfully makes every useful method in the base class private.  What a joke.
 * <p/>
 * <p>For a start, this class:
 * <ul><li>a) puts everything in alphabetical order
 * <li>b) translates any 'property' ending in '.comment' to a properties file comment '#...' eliding the comment.
 * </ul>
 */
public class CBProperties extends Properties
{
    private static final String specialSaveChars = "=: \t\r\n\f#!";

    public CBProperties(Properties props)
    {
        Enumeration keys = props.keys();
        while (keys.hasMoreElements())
        {
            Object key = keys.nextElement();
            Object val = props.get(key);
            put(key, val);
        }
    }

    public Enumeration keys()
    {
        Vector sortedKeys = new Vector();
        Enumeration baseKeys = super.keys();
        while (baseKeys.hasMoreElements())
        {
            String key = (String) baseKeys.nextElement();
            sortedKeys.addElement(key);
        }
        Collections.sort(sortedKeys);

        /*   The sortedkeys file should now contain (some) ordered pairs of properties
         *   in the form:
         *   my.property = 76
         *   my.property.comment = a very nice comment
         *
         *   ... this swaps those around, replacing the 'my.property.comment' with '#my.property ='
         #  for pretty commenting.
         */
        for (int i = 1; i < sortedKeys.size(); i++)
        {
            String key = (String) sortedKeys.get(i);
            if (key.endsWith(".comment"))
            {
                String newkey = "#" + key.substring(0, key.length() - 8);
                String value = super.getProperty(key);
                super.remove(key);
                super.setProperty(newkey, value);
                String previousKey = (String) sortedKeys.get(i - 1);
                if (key.startsWith(previousKey))
                {
                    sortedKeys.set(i - 1, newkey);
                    sortedKeys.set(i, previousKey);
                }
                else
                    sortedKeys.set(i, newkey);  // change name, but not position.  Allows for 'free floating' comments.
            }
        }

        return sortedKeys.elements();
    }

    public synchronized void store(OutputStream out, String header)
            throws IOException
    {
        BufferedWriter awriter;
        //TODO: Use UTF-8 !!!
        awriter = new BufferedWriter(new OutputStreamWriter(out, "8859_1"));
        if (header != null)
            writeln(awriter, "#" + header);
        writeln(awriter, "#" + new Date().toString());
        for (Enumeration e = keys(); e.hasMoreElements();)
        {
            String key = (String) e.nextElement();
            String val = (String) get(key);
            key = saveConvert(key, true);

            val = saveConvert(val, false);

            if (key.charAt(0) == '#') // write out comments nicely
            {
                writeln(awriter, "");
                writeln(awriter, key + "  " + val);
            }
            else
                writeln(awriter, key + "=" + val);
        }
        awriter.flush();
    }

    /*
     * Converts unicodes to encoded &#92;uxxxx
     * and writes out any of the characters in specialSaveChars
     * with a preceding slash
     */
    private String saveConvert(String theString, boolean escapeSpace)
    {
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len * 2);

        for (int x = 0; x < len; x++)
        {
            char aChar = theString.charAt(x);

            switch (aChar)
            {
                case ' ':
                    if (x == 0 || escapeSpace)
                        outBuffer.append('\\');

                    outBuffer.append(' ');
                    break;
                case '#':  // allow comments to be passed through!
                    if (x != 0)
                        outBuffer.append('\\');
                    outBuffer.append('#');
                    break;
                case '\\':
                    outBuffer.append('\\');
                    outBuffer.append('\\');
                    break;
                case '\t':
                    outBuffer.append('\\');
                    outBuffer.append('t');
                    break;
                case '\n':
                    outBuffer.append('\\');
                    outBuffer.append('n');
                    break;
                case '\r':
                    outBuffer.append('\\');
                    outBuffer.append('r');
                    break;
                case '\f':
                    outBuffer.append('\\');
                    outBuffer.append('f');
                    break;
                default:
                    if ((aChar < 0x0020) || (aChar > 0x007e))
                    {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(toHex((aChar >> 12) & 0xF));
                        outBuffer.append(toHex((aChar >> 8) & 0xF));
                        outBuffer.append(toHex((aChar >> 4) & 0xF));
                        outBuffer.append(toHex(aChar & 0xF));
                    }
                    else
                    {
                        if (specialSaveChars.indexOf(aChar) != -1)
                            outBuffer.append('\\');
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }

    private static void writeln(BufferedWriter bw, String s) throws IOException
    {
        bw.write(s);
        bw.newLine();
    }


    /**
     * Convert a nibble to a hex character
     *
     * @param	nibble	the nibble to convert.
     */
    private static char toHex(int nibble)
    {
        return hexDigit[(nibble & 0xF)];
    }

    /**
     * A table of hex digits
     */
    private static final char[] hexDigit = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
}
