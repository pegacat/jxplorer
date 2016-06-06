package com.ca.commons.naming;

import java.io.*;

/**
 * Utility file reader for LDIF files.
 *
 * We overload buffered reader / line number reader to create a reader that returns pre-joined lines from
 * the LDIF file (e.g. splices together all those multi-line attribute values...).
 *
 * (c) Chris Betts; Pegacat Software (http://pegacat.com)
 */
public class LdifStreamReader extends LineNumberReader
{
    String nextLine;
    String currentLine = "";
    boolean firstLine = true;

    public LdifStreamReader(Reader reader)
            throws IOException
    {
        super(reader);
        nextLine = super.readLine();
    }

    /**
     * Returns  the next full attribute text of an LDIF file, concatenating any split lines as required
     * @return
     * @throws IOException
     */
    public String readLine()
                throws IOException
    {
        if (nextLine == null)  // stream ended
            return null;

        String current = "";

        do
        {
            current = current + nextLine;
            nextLine = super.readLine();

        }
        while (nextLineContinues()); // modifies nextLine

        return current;
    }

    /**
     * This checks whether the 'nextLine' instance variable is a line continuation (e.g. has a leading space as per RFC 2849)
     * @return
     */
    private boolean nextLineContinues()
    {
        if (nextLine == null)
            return false;

        int len = nextLine.length();
        if (len>0 && nextLine.charAt(0)==' ')
        {
            nextLine = nextLine.substring(1);

            //XXX A special (and inefficient) hack for a private project - allow an initial '>' to represent a line feed for readability of multi line text
            //XXX in an LDIF file.  Note - this is *not* standard LDIF!
            if (len>1 && nextLine.charAt(0) == '>')  // hack for user-readible line wraps... non-standard, but allowable in groupmind application.
                nextLine = "\n" + ((len>2)?nextLine.substring(1):"");

            return true;
        }
        else
            return false;
    }
}
