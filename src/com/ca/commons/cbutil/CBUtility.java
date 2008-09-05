package com.ca.commons.cbutil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a grab bag of useful classes and static functions that are
 * not important enough to merit being top level entities.  Most of them
 * are concerned with string handling, file handling, and i18n issues.
 */

public class CBUtility
{

    private static Cursor savedCursor;
    private static Frame displayFrame = null;

    private static Logger log = Logger.getLogger(CBUtility.class.getName());

    private CBUtility()
    {
    }

    /**
     * A utility ftn used to make a closing window shut down
     * the current application.  Useful for small test progs.
     */
    public static class BasicWindowMonitor extends WindowAdapter
    {
        public void windowClosing(WindowEvent e)
        {
            Window w = e.getWindow();
            w.setVisible(false);
            w.dispose();
            //System.exit(0);
        }
    }

    /**
     * Returns the raw text (i.e. with tags as "\<...\>" strings) of a web page
     *
     * @param url the url of the web age to read as plain text.
     * @return a StringBuffer containing the raw html text
     */

    public static StringBuffer readURLText(URL url)
    {
        return readURLText(url, new StringBuffer("error: can't read URL " + url.toString()));
    }

    /**
     * Returns the raw text (i.e. with tags as "\<...\>" strings) of a web page
     *
     * @param url       the url of the web age to read as plain text.
     * @param errorText a custom message to return if something goes wrong.
     * @return a StringBuffer containing the raw html text
     */

    public static StringBuffer readURLText(URL url, StringBuffer errorText)
    {
        StringBuffer page = new StringBuffer("");
        String thisLine;
        try
        {
            BufferedReader source = new BufferedReader(new InputStreamReader(url.openStream()));

            while ((thisLine = source.readLine()) != null)
            {
                page.append(thisLine + "\n");
            }
            return page;
        }
        catch (Exception e)
        {
            return errorText;
        }
    }

    /**
     * Reads an input stream into a byte array.
     */

    public static byte[] readStream(InputStream is) throws IOException
    {
        byte[] data = null;
        byte[] buffer = new byte[16384];
        int blockSize = 0;
        int size = 0;

        while ((blockSize = is.read(buffer)) != -1)        // kinda clumsy, reallocating
        {                                                  // memory like this I guess,
            byte[] temp = new byte[size + blockSize];      // but since we don't know
            if (size != 0)                                 // how big the stream is, what
                System.arraycopy(data, 0, temp, 0, size);  // else can we do? (?)

            System.arraycopy(buffer, 0, temp, size, blockSize);
            data = temp;
            size += blockSize;
        }
        return data;
    }


    /**
     * Reads a text file, and returns the result as a String.  Not
     * Recommended for large (say > 100k) files.
     *
     * @param file the ascii file to read from.
     */

    public static String readTextFile(File file)
            throws IOException
    {
        // special handling for file reading in non-english locales...

        if (Locale.getDefault().getLanguage().equals("en") == false)
            return readI18NFile(file);

        // Read File into String Buffer
        FileReader in = new FileReader(file);
        int size = (int) file.length();
        char[] data = new char[size];
        int chars_read = 0;
        while (chars_read < size)
            chars_read += in.read(data, chars_read, size - chars_read);

        return new String(data);  // use default locale encoding...
    }

    /**
     * Reads a text file, and returns the result as a StringBuffer.  Not
     * Recommended for large (say > 100k) files.<p>
     * <p/>
     * This function attempts to automatically determine the encoding
     * of the file it is to read, as either UTF-16, UTF-8, or default
     * locale encoding, based on 1) whether the first two bytes are
     * Unicode byte-ordering markers ('FFFE' or 'FEFF'), UTF-8 (based
     * on whether the file is a valid UTF8 string) or,
     * failing this, the default locale encoding.
     *
     * @param file the local encoding/unicode/utf8 file to read from.
     */

    public static String readI18NFile(File file)
            throws IOException
    {
        // Read File into String Buffer

        FileInputStream in = new FileInputStream(file);
        int size = (int) file.length();
        byte[] data = new byte[size];
        int bytes_read = 0;

        while (bytes_read < size)
            bytes_read += in.read(data, bytes_read, size - bytes_read);

        return readI18NByteArray(data);
    }

    public static String readI18NByteArray(byte[] data)
    {
        // Try to work out whether this is unicode double bytes (utf-16),
        // unicode (or *cough* 7 bit ascii) in utf-8 format, or local
        // encoding...
        try
        {
            if (CBParse.isUnicode(data))
            {
                log.finer("reading unicode 16 bit text");
                String text = new String(data, "UTF-16");  // return as 16 bit unicode
                if (text.length() > 0) return text;
                return new String(data); // something went wrong - try again with default encoding...
            }
            else
            {
                byte[] test = new byte[250];  // grab the start of the file to test...

                if (data.length < 250)
                    test = data;
                else
                    System.arraycopy(data, 0, test, 0, 250);

                if (CBParse.isNonAsciiUTF8(test))
                {
                    log.finer("reading utf8 text");
                    String text = new String(data, "UTF-8");   // return as UTF-8
                    if (text.length() > 0) return text;
                    return (new String(data));  // something went wrong - try again with default encoding
                }
                else
                {
                    log.finer("reading local encoding text");

                    String newString = new String(data);
                    if (newString.indexOf("\\u") == -1)
                    {
                        return newString;    // no need for special processing.
                    }

                    // MANUALLY (!) decode \ u java unicode escape strings...
                    // (Why?  Because someone may be in a foreign locale, but
                    // still using broken java unicode escape syntax from standard
                    // property files.)

                    StringBuffer buffer = new StringBuffer(newString);

                    int pos = 0;
                    while (pos + 6 < buffer.length())
                    {
                        if (buffer.charAt(pos) != '\\')
                            pos++;
                        else if (buffer.charAt(pos + 1) != 'u')
                            pos += 2;
                        else
                        {
                            String unicode = buffer.substring(pos + 2, pos + 6);
                            int uni = Integer.parseInt(unicode, 16);
                            buffer = buffer.delete(pos, pos + 6);
                            buffer = buffer.insert(pos, (char) uni);
                            pos++;
                        }

                    }

                    return buffer.toString();  // return as default locale encoding
                }
            }
        }

                /*   If anything goes wrong (UnsupportedEncodingException, or hopefully if
                 *   the utf-8 string turns out not to be) fall back on using the
                 *   default encoding.
                 */

        catch (Exception e)
        {
            log.warning("Confused Reading File: " + e.toString() + "\n -> reverting to default encoding");
            return new String(data);  // return as default locale encoding
        }
    }


    /**
     * Reads an array of strings from a file
     * (via a property file, 'cause I'm lazy).
     *
     * @param fileName the file to read from
     */
    public static String[] readStringArrayFile(String fileName)
    {
        Properties props = readPropertyFile(fileName);
        String[] values = new String[props.size()];
        Enumeration en = props.elements();
        int count = 0;
        while (en.hasMoreElements())
        {
            values[count++] = en.nextElement().toString();
        }
        return values;
    }

    /**
     * Reads a java Properties list from a file.
     *
     * @param fileName the full path and file name of the properties file
     *                 to read in.
     */

    public static Properties readPropertyFile(String fileName)
    {
        Properties propertyList = new Properties();

        try
        {
            File propertyFile = new File(fileName);
            if (propertyFile == null || propertyFile.exists() == false)
            {
                log.warning("No property list:\n" + fileName);
                return propertyList; // return empty properties list
            }

            FileInputStream in = new FileInputStream(propertyFile);
            propertyList.load(in);
            return propertyList;
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "Can't read property list:\n" + fileName + "\n", e);
            return propertyList;
        }
    }

    /**
     * Writes an array of strings into a file
     * (via a property file, 'cause I'm lazy).
     * (XXX Warning - will only write unique values; doubles will be lost).
     *
     * @param fileName the file to read to
     * @param strings  the array of strings
     */
    public static void writeStringArrayFile(String fileName, String[] strings)
    {
        Properties props = new Properties();
        for (int i = 0; i < strings.length; i++)
            props.put(strings[i], strings[i]);     // so it's redundant.  sue me.

        writePropertyFile(fileName, props, "generated string array list");
    }


    /**
     * Writes a java Properties list to a file.
     *
     * @param fileName the full path and file name of the properties file
     *                 to read in.
     */

    public static void writePropertyFile(String fileName, Properties propertyList, String comments)
    {
        // do hack to get propertyList to print out in alphabetical order...

        CBProperties orderedPropertyList = new CBProperties(propertyList);

        try
        {
            File propertyFile = new File(fileName);

            FileOutputStream out = new FileOutputStream(propertyFile);

            orderedPropertyList.store(out, "Generated Property List " + fileName + "\n" + ((comments != null) ? comments : ""));
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "Can't write property list:\n" + fileName + "\n", e);
        }
    }

    /**
     * Turns a string into HTML displayable text by escaping
     * special characters ('<','&' etc...).
     * <p/>
     * ... add new ones as required; or see if an existing ftn somewhere
     * does this already...
     *
     * @deprecated use CBParse method instead
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
                    CBParse.replaceChar(temp, pos, "&lt;");
                    break;
                case '>':
                    CBParse.replaceChar(temp, pos, "&gt;");
                    break;
                case '&':
                    CBParse.replaceChar(temp, pos, "&amp;");
                    break;
                case '\"':
                    CBParse.replaceChar(temp, pos, "&quot;");
                    break;
                case '#':
                    CBParse.replaceChar(temp, pos, "&#35;");
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
     * @deprecated use CBParse method instead
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
     * @deprecated use CBParse method instead
     */

    public static String replaceAllChar(StringBuffer text, char c, String replacement)
    {
        return CBParse.replaceAllBufferChar(text, c, replacement).toString();
    }

    /**
     * Deletes all characters <i>c</i> in <i>text</i> replaces
     * it with the string <i>replacement</i>.
     *
     * @param text        the text to be modified
     * @param replacement the string the character is to be replaced with.
     * @deprecated use CBParse method instead
     */

    public static StringBuffer replaceAllBufferChar(StringBuffer text, char c, String replacement)
    {
        int pos = 0;
        while (pos != -1)
        {
            pos = text.toString().indexOf(c, pos);
            if (pos != -1)
                pos = CBParse.replaceChar(text, pos, replacement);
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
     * @deprecated use CBParse method instead
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
     * @deprecated use CBParse method instead
     */

    public static String replaceAllString(StringBuffer text, String orig, String replacement)
    {
        return CBParse.replaceAllBufferString(text, orig, replacement).toString();
    }

    /**
     * Deletes all characters <i>orig</i> in <i>text</i> replaces
     * it with the string <i>replacement</i>.
     *
     * @param text        the text to be modified
     * @param orig        the original text substring to be changed
     * @param replacement the string the original substring is to be replaced with.
     * @deprecated use CBParse method instead
     */

    public static StringBuffer replaceAllBufferString(StringBuffer text, String orig, String replacement)
    {
        int pos = 0;
        while (pos != -1)
        {
            pos = text.toString().indexOf(orig, pos);
            if (pos != -1)
                pos = CBParse.replaceString(text, pos, orig.length(), replacement);
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
     * @deprecated use CBParse method instead
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

    /**
     * Reads a directory, returning all file names of the given extension.
     *
     * @param dirPath   directory to read
     * @param extension the file extension to filter files with.
     * @return list of full file names
     */

    public static String[] readFilteredDirectory(String dirPath, String extension)
    {
        String[] extensions = new String[1];
        extensions[0] = extension;

        return readFilteredDirectory(dirPath, extensions);
    }

    /**
     * Reads a directory, returning all file names of the given extensions
     *
     * @param dirPath        directory to read
     * @param fileExtensions extension a list of file extensions to filter files with.
     * @return list of full file names
     */

    public static String[] readFilteredDirectory(String dirPath, String[] fileExtensions)
    {
        final String[] extensions = fileExtensions;
        File dir = new File(dirPath);
//XXX Could use CBFileFilter here?
        String[] templates = dir.list(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                for (int i = 0; i < extensions.length; i++)
                {
                    if (name.endsWith(extensions[i]))
                        return true;
                }
                return false;
            }
        });

        return templates;
    }


    /**
     * Sets the cursor to the wait cursor.
     *
     * @param C the owning component.
     */

    public static void setWaitCursor(Component C)
    {
        C.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }


    /**
     * Sets the cursor to the normal cursor.
     *
     * @param C the owning component.
     */

    public static void setNormalCursor(Component C)
    {
        C.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }


    /**
     * Sets the cursor to the hand cursor.
     *
     * @param C the owning component.
     */

    public static void setHandCursor(Component C)
    {
        C.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }


    /**
     * Saves a cursor.  One cursor.  That's all. Just one.  Try to
     * save another one, it'll overwrite this one.  So don't.
     *
     * @param C the owning component.
     */

    public static void saveCursor(Component C)
    {
        savedCursor = C.getCursor();
    }


    /**
     * Gets the cursor back that you just saved.  Probably better
     * make sure it's the same component you saved it from.  Wouldn't
     * care to guess what happens if it isn't...
     *
     * @param C the owning component.
     */
    public static void restoreCursor(Component C)
    {
        if (savedCursor != null)
            C.setCursor(savedCursor);
        else
            log.info("graphics error: can't restore cursor; no cursor saved...");
    }

    /**
     *    Sets the level of logging on a scale of 0 (none) to 10 (everything).
     *    @param L the log level.
     */

    //public static void setLogDebugLevel(int L) { debugLevel = L; }

    /**
     *    Returns the global debug level.
     */

    //public static int getLogDebugLevel() { return debugLevel; }



    /**
     *    Sets the type of logging, using the strings 'none', 'console' or 'file'.
     *    @param logType the type of logging to use.
     */
    //public static void setLogType(String logType) {setLogType(logType, null);}

    /**
     *    Sets the type of logging, using the strings 'none', 'console' or 'file'.
     *    @param logType the type of logging to use.
     *    @param fileName the name of the log file to use, (unused if logType != 'file')
     */
/*
    public static void setLogType(String logType, String fileName)
    {
        if (logType.equalsIgnoreCase("none")) loggingStyle = NOLOG;
        else if (logType.equalsIgnoreCase("console")) loggingStyle = CONSOLE;
        else if (logType.equalsIgnoreCase("file") || logType.equalsIgnoreCase("both"))
        {
            String logFileName = (fileName==null)?"jxplorer.log":fileName;
            try
            {
                logfile = new FileWriter(logFileName);
                if (logType.equalsIgnoreCase("both"))
                    loggingStyle=CONSOLEANDFILE;
                else
                    loggingStyle = FILE;
            }
            catch (Exception e)
            {
                CBUtility.log("unable to open log file " + logFileName + "\nreverting to console logging");
                loggingStyle = CONSOLE;
            }
        }
        else loggingStyle = CONSOLE;  // console is default...

        log("Logging Initialised to " + logType, 1);
    }
*/
    /**
     *    Closes the log file.
     */
/*
    public static void closeLog()
    {
        try
        {
            if (logfile != null) logfile.close();
        }
        catch (Exception e)
        {
            CBUtility.log("error shutting log file " + e.toString());
        }
    }
*/
    /**
     *    logs if the global debug level equal to or greater than the
     *    passed int value.<p>
     *
     *    <b>Log Levels</b><br>
     *    <ul>
     *    <li>0 - error logging only
     *    <li>1
     *    <li>2
     *    <li>3
     *    <li>4 - entry level logging of all delete/copy/move operations
     *    <li>5
     *    <li>6
     *    <li>7
     *    <li>8
     *    <li>9 - full BER logging
     *    </ul>
     *
     *
     *    @param S the string to log
     *    @param level the debug level at which the string starts
     *            being printed
     */
/*
    public static void log(String S, int level)
    {
        if (debugLevel >= level) log(S);
    }
*/
    /**
     * Simple logging utility.  Writes log data to a file or console,
     * or ignores it, depending on the value of the logging and logfile
     * property (defaults set in JXplorer.java, user sets in dxconfig.txt)
     */

/*
     public static void log(String S)
     {
        S = (new Date(System.currentTimeMillis())).toString() + ": " + S;
        switch (loggingStyle)
        {
            case NOLOG: break;            // do nothing

            case CONSOLEANDFILE:         // log file and console...
            case FILE: try               // log file only
                    {
                        logfile.write(S + "\n");
                        logfile.flush();
                    }
                    catch (Exception e)
                    {
                        CBUtility.log("unable to write to log file\nreverting to console\n" + e + "\n"+S);
                        loggingStyle = CONSOLE;
                    }
                    if (loggingStyle == FILE) break;

            case CONSOLE:                     // console

            default:  System.out.println(S);  break;  //  echo to console
         }
    }
*/
    public static void initDefaultDisplay(Frame owner)
    {
        displayFrame = owner;
    }

    public static Frame getDefaultDisplay()
    {
        return displayFrame;
    }


    /**
     * utility ftn; prints error message to user, and echos to the log ftn.
     *
     * @return returns false for easy chaining.
     */

    public static boolean error(Component owner, String Msg)
    {
        return error(getParentFrame(owner), Msg, null);
    }

    /**
     * utility ftn; prints error message to user, and echos to the log ftn.
     *
     * @return returns false for easy chaining.
     */

    public static boolean error(Frame owner, String Msg)
    {
        if (displayFrame == null) // no default display registered
        {
            log.warning("graphics error: error display not initialised! (root error was: " + Msg + ")");
            return false;
        }
        return error(owner, Msg, null);
    }

    /**
     * utility ftn; prints error message to user, and echos to the log ftn.
     *
     * @return returns false for easy chaining.
     */

    public static boolean error(String Msg)
    {
        if (displayFrame == null) // no default display registered
        {
            log.warning("graphics error: error display not initialised! (error was: " + Msg + ")");
            return false;
        }
        return error(displayFrame, Msg, null);
    }

    /**
     * wrapper for the JFrame version of error.
     *
     * @param Msg a short one line message to display to the user
     * @param e   the exception to log
     * @return returns false for easy chaining.
     */


    public static boolean error(String Msg, Exception e)
    {
        return error(displayFrame, Msg, e);
    }


    /**
     * wrapper for the JFrame version of error.
     *
     * @param owner the component (from which the parent Frame will be derived)
     * @param Msg   a short one line message to display to the user
     * @param e     the exception to log
     * @return returns false for easy chaining.
     */


    public static boolean error(Component owner, String Msg, Exception e)
    {
        return error(getParentFrame(owner), Msg, e);
    }

    /**
     * utility ftn; prints error message and the error to the user,
     * and echos to the log ftn.
     *
     * @param owner the parent Frame (required for dialog box drawing)
     * @param Msg   a short one line message to display to the user
     * @param e     the exception to log
     * @return returns false for easy chaining.
     */


    public static boolean error(Frame owner, String Msg, Exception e)
    {
        if (owner == null) //TE: added this check basically so that I can centre the error window...i.e if there is no owner - there is nothing to centre upon!
        {
            if (displayFrame == null) // no default display registered
            {
                log.warning("error display not initialised! (error was: " + Msg + ")");
                return false;
            }
            else
            {
                owner = displayFrame;
            }
        }

        CBErrorWin errWin = new CBErrorWin(owner, Msg, e);

        log.log(Level.WARNING, "error displayed to user: " + Msg, e);

        return false;
    }


    /**
     * Utility function. Opens a dialog with a confirmation message.
     *
     * @param Msg the confirmation message to be displayed.
     */

    public static void confirm(String Msg)
    {
        if (displayFrame == null) // no default display registered
        {
            log.warning("error display not initialised! (error was: " + Msg + ")");
            return;
        }

        new CBErrorWin(displayFrame, Msg, "Confirmation Message");
    }


    /**
     * utility ftn; prints warning dialog message to the user,
     * *without* echoing to the log ftn.  Basically wrapper to JOptionPane
     *
     * @param caller the GUI component calling (required for dialog box drawing)
     * @param Msg    a short one line message to display to the user
     * @return returns false for easy chaining.
     */

    public static boolean warning(Component caller, String Msg, String Title)
    {
        JOptionPane.showMessageDialog(caller, Msg,
                Title, JOptionPane.WARNING_MESSAGE);
        return false;  // for chaining
    }

    /**
     * Short version of warning method - uses default frame, and has the
     * title 'Warning'.
     *
     * @param Msg the warning message to display.
     */
    public static boolean warning(String Msg)
    {
        if (displayFrame == null) // no default display registered
        {
            log.warning("warning display not initialised! (error was: " + Msg + ")");
            return false;
        }
        return warning(displayFrame, Msg, "Warning");
    }

    /**
     * prints an enumeration...
     */

    public static void printEnumeration(Enumeration e)
    {
        while (e.hasMoreElements())
        {
            Object raw = e.nextElement();
            String value = (raw == null) ? "*null*" : raw.toString();
            System.out.println("    " + value);
        }
    }

    /**
     * Iterates through a components parents until it finds the
     * root frame.  Useful for initing JDialogs etc. that require
     * a root frame to work properly.
     */
    public static Frame getParentFrame(Component c)
    {
        if (c == null) return null;

        Component parent = c.getParent();
        while (!(parent instanceof Frame) && (parent != null))
            parent = parent.getParent();

        return (parent == null) ? null : (Frame) parent;
    }

    /**
     * Converts a 'dos' style file path to a unix style file path
     * by exchanging '\' characters for for '/' characters.
     */

    public static String convertPathToUnix(String dosPath)
    {
        String ret = dosPath.replace('\\', '/');
        return ret;
    }


    /**
     * This positions a component to the center of another component.
     * If both components are showing on the sceen, it uses absolute
     * screen co-ordinates, otherwise if only the positioner component
     * is showing, it uses relative co-ordinates (since it is unable to
     * obtain screen co-ords).  If the components share a reference
     * frame, these two actions are equivalent (i.e. if they both have
     * the same parent).  If nothing is showing, the component is unchanged.
     * NOTE: if the X & Y coordinates are off the screen, the component to
     * center will be centered in the middle of the screen.
     *
     * @param centerMe   the component to center
     * @param positioner the component used as the reference center.  If null,
     *                   the component will be centered on the screen
     */

    public static void center(Component centerMe, Component positioner)
    {
        if (centerMe == null) return;

        if (positioner != null && positioner.isShowing())
        {
            Rectangle pos = positioner.getBounds();    // relative info.
            Point absPos = positioner.getLocationOnScreen();  // absolute info.
            int centerX = absPos.x + (pos.width / 2);    // center x pos, in screen co-ords
            int centerY = absPos.y + (pos.height / 2);   // center y pos, in screen co-ords
            pos = centerMe.getBounds();                 // relative info;

            int x = 0;
            int y = 0;

            if (centerMe.isShowing())  // if centerMe is showing, center it using screen co-ords (no possibility of error)
            {
                absPos = centerMe.getLocationOnScreen();    // absolute info;
                int currentX = absPos.x + (pos.width / 2);    // center of centerMe x pos, in screen co-ords
                int currentY = absPos.y + (pos.height / 2);   // center of centerMe y pos, in screen co-ords

                int deltaX = centerX - currentX;            // amount to move X
                int deltaY = centerY - currentY;            // amount to move Y

                x = pos.x + deltaX;
                y = pos.y + deltaY;
            }
            else  // centerMe isn't showing - can't use screen co-ords, so *assume* both positioner and centerMe have same reference frame
            {     // (i.e. components share a common parent...)
                x = centerX - (pos.width / 2);
                y = centerY - (pos.height / 2);
            }

            Toolkit toolKit = Toolkit.getDefaultToolkit();

            if ((x - 100) < 0 || (x + 100) > toolKit.getScreenSize().width || (y - 100) < 0 || (y + 100) > toolKit.getScreenSize().height)       //TE: if off screen (add some padding/a safety margin)...
                centerOnScreen(centerMe);                                                               //TE: center in middle of screen (bug 2926).
            else
                centerMe.setLocation(x, y);  // move, using local co-ordinates.
        }
        else
        {
            centerOnScreen(centerMe);
        }
    }


    /**
     * Centers a component on the middle of the screen.
     *
     * @param centerMe the component to center.
     */

    private static void centerOnScreen(Component centerMe)
    {
        Dimension screen = centerMe.getToolkit().getScreenSize();
        Dimension object = centerMe.getSize();
        centerMe.setLocation((int) (screen.getWidth() - object.getWidth()) / 2, (int) (screen.getHeight() - object.getHeight()) / 2);
    }


    /**
     * @deprecated use CBParse method instead
     */
    public static String bytes2Hex(byte[] bytes)
    {
        StringBuffer ret = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++)
        {
            ret.append(CBParse.byte2Hex(bytes[i]));
        }
        return ret.toString();
    }


    /**
     * @deprecated use CBParse method instead
     */
    public static String string2Hex(String orig)
    {
        StringBuffer ret = new StringBuffer(orig.length() * 2);
        char[] c = orig.toCharArray();
        for (int i = 0; i < c.length; i++)
        {
            ret.append(CBParse.char2Hex(c[i]));
        }
        return ret.toString();
    }

    /**
     * @deprecated use CBParse method instead
     */
    static public String byte2Hex(byte b)
    {
        // Returns hex String representation of byte b
        final char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] array = {hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f]};
        return new String(array);
    }

    /**
     * @deprecated use CBParse method instead
     */
    static public String char2Hex(char c)
    {
        // Returns hex String representation of char c
        byte hi = (byte) (c >>> 8);
        byte lo = (byte) (c & 0xff);
        return CBParse.byte2Hex(hi) + CBParse.byte2Hex(lo);
    }

    /**
     * @deprecated use CBParse method instead
     */
    static public byte hex2Byte(char hex1, char hex2)
    {
        byte a = CBParse.hexChar2Byte(hex1);
        byte b = CBParse.hexChar2Byte(hex2);
        return (byte) ((a << 4) + b);
    }

    /**
     * Convert a single character to a byte...
     *
     * @deprecated use CBParse method instead
     */

    static public byte hexChar2Byte(char hex)
    {
        if (hex <= '9')
            return ((byte) (hex - 48)); // ('0' -> '9')
        else if (hex <= 'F')
            return ((byte) (hex - 55)); // ('A' -> 'F')
        else
            return ((byte) (hex - 87)); // ('a' -> 'f')
    }

    /**
     * From Van Bui - prints out a hex string formatted with
     * spaces between each hex word of length wordlength.
     *
     * @param in         input array of bytes to convert
     * @param wordlength the length of hex words to print otu.
     * @deprecated use CBParse method instead
     */
    public static String bytes2HexSplit(byte[] in, int wordlength)
    {
        String hex = CBParse.bytes2Hex(in);
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
     * @deprecated use CBParse method instead
     */

    public static String bytes2HexSplit(byte[] in, int wordlength, int linelength)
    {
        String hex = CBParse.bytes2Hex(in);
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
     *
     * @deprecated use CBParse method instead
     */
    public static boolean isUTF8(byte[] sequence)
    {
        boolean debug = false;
        if (debug) log.warning("\n\n Starting UTF8 Check\n\n");
        int numberBytesInChar;

        for (int i = 0; i < sequence.length; i++)
        {
            byte b = sequence[i];
            if (debug) System.out.println("testing byte: " + CBParse.byte2Hex(b));
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
                    if (debug) System.out.println("testing byte: " + CBParse.byte2Hex(sequence[i + j]));
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
     *
     * @deprecated use CBParse method instead
     */

    public static boolean isNonAsciiUTF8(byte[] sequence)
    {
        log.finest("testing sequence for utf8: " + CBParse.bytes2Hex(sequence));
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
     *
     * @deprecated use CBParse method instead
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

    /*
     *    Some refugees from com.ca.pki.util.StaticUtil
     */

    /**
     * Show file chooser to get a file location for saving data.
     */
    public static String chooseFileToSave(Component parent, String title, String[] filter, String fileType)
    {
        JFileChooser chooser = new JFileChooser(System.getProperty("PKIHOME"));
        chooser.setToolTipText(title);
        chooser.setDialogTitle(title);
        if (filter != null && fileType != null)
        {
            CBFileFilter filt = new CBFileFilter(filter, fileType);
            chooser.setFileFilter(filt);
        }
        int returnVal = chooser.showSaveDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            if (chooser.getSelectedFile() != null)
                return chooser.getSelectedFile().toString();
        }
        return null;
    }

    public static boolean okToWriteFile(Frame parent, String fileName)
    {
        File f = new File(fileName);
        if (f.isDirectory())
        {
            JOptionPane.showMessageDialog(parent, fileName + " is a directory.", "Error!", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        else if (f.exists())
        {
            int saveAnswer = JOptionPane.showConfirmDialog(parent,
                    "File " + fileName + " already exists.\nDo you want to overwrite?",
                    "Question", JOptionPane.OK_CANCEL_OPTION);
            return (saveAnswer == JOptionPane.OK_OPTION);
        }
        return true;
    }


    /**
     * This Comparator compares two strings, ignoring case.
     *
     * @author Trudi.
     */

    public static class IgnoreCaseStringComparator implements Comparator
    {

        /**
         * This Comparator compares two strings, ignoring case.
         *
         * @param o1 one of the two items to be compared.
         * @param o2 the other of the items to be compared.
         * @return the result of the compare (0 if o1 & o2 are equal, -1 if o1 < o2, 1 if o1 > o2).
         *         NOTE: if o1 is null and o2 is not null, 1 is returned. If o2 is null and o1 is not null, -1 is returned.
         *         If both o1 and o2 are null, 0 is returned. If an error occurs trying to cast either o1 or o2, 0 is returned.
         */

        public int compare(Object o1, Object o2)
        {
            if (o1 == null && o2 != null)
                return 1;
            else if (o2 == null && o1 != null)
                return -1;
            else if (o1 == null && o2 == null)
                return 0;

            try
            {
                return (o1.toString().toLowerCase()).compareTo(o2.toString().toLowerCase());
            }
            catch (ClassCastException e)
            {
                System.out.println("Error sorting values - invalid string in sort." + e);
                return 0;
            }
        }
    }

    /**
     * Searches for a config file.  First checks if a property directory has been explicitly set
     * with the System property 'jxplorer.config' (which can take either a path, or the
     * value 'user.home' to set it to use the user home directory).  Then checks the user
     * home directory to see if the specific config file already exists there.  Then reverts to the default
     * location in the user.dir directory the program is run from.
     *
     * @param configFileName the name of the actual file - e.g. "bookmarks.txt".
     * @return
     */
    public static String getPropertyConfigPath(String configFileName)
    {
        String pathToConfigFile;

        // the potential path to a config file in the user home directory
        String userHomeConfigDir = System.getProperty("user.home") + File.separator + "jxplorer";
        String userHomeConfigFilePath = userHomeConfigDir + File.separator + configFileName;

        // the default config location is to the directory JXplorer is run from.
        String defaultConfigFilePath = System.getProperty("user.dir") + File.separator + configFileName;

        // see if a config file has been explicitly set
        if (System.getProperty("jxplorer.config") != null)
        {
            pathToConfigFile = System.getProperty("jxplorer.config");

            // check if we are being forced to use the user.home directory (we will create the file if it
            // doesn't exist)
            if (pathToConfigFile.equalsIgnoreCase("home") || pathToConfigFile.equalsIgnoreCase("user.home"))
            {
                // quick sanity check that there is a 'jxplorer' config directory under user home for us to save to
                File configDir = new File(userHomeConfigDir);
                if (configDir.exists() == false)
                {
                    configDir.mkdir(); // - if not, we'll create it.
                }

                return userHomeConfigFilePath;
            }
            // otherwise return the parameter - again we will create it if it doesn't exist
            if (pathToConfigFile.endsWith(File.separator))
                return pathToConfigFile + configFileName;
            else
                return pathToConfigFile + File.separator + configFileName;
        }

        // try the user home directory next... if the user home directory config file exists read the config from there
        if (new File(userHomeConfigFilePath).exists())
        {
            return userHomeConfigFilePath;
        }

        // if there is no config file in the user.home directory, and we haven't been told otherwise, default to
        // the directory JXplorer is run from.
        return defaultConfigFilePath;
    }

    /**
     * This gets the working log level of a particular logger.  You would think Sun would already
     * have a method to do this.  You would be wrong.
     *
     * @param log
     * @return the active working log level - e.g. whether a particular log call will be called because this,
     * or a parent, logger is set to a particular log level.
     */
    public static Level getTrueLogLevel(Logger log)
    {
        if (log.getLevel() != null)
        {
            return log.getLevel();
        }

        if (log.getParent() != null)
            return getTrueLogLevel(log.getParent());

        // should never get here...
        log.severe("no active log level initialised");
        System.err.println("no active log level initialised");

        return Level.ALL;
    }
}