package com.ca.commons.jndi;

import com.ca.commons.naming.*;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;
import java.io.*;


/**
 * This is a general test bed interface to test the various jndi calls
 * used by JXplorer.  It used ldif change file format requests, and can
 * be run directly from the command line, or given an ldif change file
 * to process.<p>
 * <p/>
 * Requests are either standard ldif change file requests, such as:
 * <pre>
 * <p/>
 *    version: 1
 *    # Add a new entry
 *     dn: cn=Fiona Jensen, ou=Marketing, dc=airius, dc=com
 *     changetype: add
 *     objectclass: top
 *     objectclass: person
 *     objectclass: organizationalPerson
 *     cn: Fiona Jensen
 *     sn: Jensen
 *     uid: fiona
 *     telephonenumber: +1 408 555 1212
 *     jpegphoto:< file:///usr/local/directory/photos/fiona.jpg
 *    <pre>
 *    where valid 'changetype' values are [add|delete|modrdn|modify]
 *    (see draft-good-ldap-ldif-04.html) <p>
 *    This is extended by jnditest to include extra (non-standard) changetype
 *    commands, such as [connect|disconnect|list|search|searchOneLevel|read], to allow
 *    general directory access and testing.
 *    The 'connect' command takes the attributes; 'url', and the optional \n"+
 *    attributes: 'user','pwd','tracing','ldapVersion','referral' and 'useSSL' \n" +
 * <p/>
 *    XXX coming soon - [copyTree|deleteTree|moveTree]
 *
 *    @author Chris, Trudi
 */

public class TestJNDIOps
{
    /**
     * Open the appropriate input stream, and create a jdbctest object.
     */
    //  static String version = "1.0(build"+BuildNumber.value +")";

    LdifStreamReader in;

    PrintStream out;

    DXOps myOps = null;
    //AdvancedOps myAdvOps = null;
    
    LdifUtility ldifutil = new LdifUtility();    // ldif utility used to read / write ldif files

    boolean debug = true;
    boolean terminating = false; 	// whether the program should fail, returning -1, on error
    boolean printstack = false;		//TE: whether to print the stack trace.

    public static int OK = 0;
    public static int ERROR = -1;
    public static int FINISHED = 1;

    public static void main(String args[])
    {

        String fileName = null;
        String url = null;
        String user = null;
        String pwd = null;
        String version = "3";
        String referral = "follow";
        boolean useSSL = false;
        boolean tracing = false;
        boolean debugFlag = true;
        boolean terminateFlag = false;
        boolean printstackFlag = false;

        int i = 0;

        try
        {
            while (i < args.length)
            {
                String arg = (args[i].charAt(0) != '-') ? args[i] : args[i].substring(1);
                switch (arg.charAt(0))
                {
                    case '?':
                    case 'H':
                    case 'h':
                        if (args.length > i + 1)
                            printHelp(args[i + 1]);
                        else
                            printHelp(null);
                        return;  // print and exit program

                    case 'C':
                    case 'c':
                        url = args[++i];
                        break;

                    case 'D':
                    case 'd':
                        debugFlag = true;
                        break;

                    case 'E':
                    case 'e':
                        terminateFlag = true;
                        break;

                    case 'F':
                    case 'f':
                        fileName = args[++i];
                        break;

                    case 'P':
                    case 'p':
                        pwd = args[++i];
                        break;

                    case 'R':
                    case 'r':
                        referral = args[++i];
                        break;

                    case 'S':
                    case 's':
                        useSSL = true;
                        break;

                    case 'T':
                    case 't':
                        tracing = true;
                        break;

                    case 'U':
                    case 'u':
                        user = args[++i];
                        break;

                    case 'V':
                    case 'v':
                        version = args[++i];
                        break;

                    case 'X':
                    case 'x':
                        printstackFlag = true;
                        break;

                    default :
                        System.out.println("\n\nInvalid command line argument: -" + arg);
                        printHelp(null);
                        return;  // print and exit program
                }
                i++;
            }
        }
        catch (Exception e)
        {
            System.out.println("Error reading command line arguments.");
            printHelp("");
            System.exit(-1);
        }
        
        // create jnditest object
        TestJNDIOps tester = new TestJNDIOps(fileName, url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag);
        
        // use jnditest object to process data until input finished.
        tester.processInput();

        tester.out.println("\nnormal finish\n");
    }


    public static void printHelp(String subject)
    {
        if ("full".equalsIgnoreCase(subject))
        {
            printFullHelp();
            return;
        }
        if ("changetype".equalsIgnoreCase(subject))
        {
            printChangeTypeHelp();
            return;
        }

        System.out.println("" +
                "\n\njndiTest is a small utility/test program designed to check the ldap/jndi\n" +
                "functionality of JXplorer.  It reads input or files in ldif change file format\n" +
                "with some extra commands added to test searching and attribute reading.\n\n" +
                " usage: java jnditest [<options>]\n\n" +
                " options:\n" +
                " -c            an optional connection url of the form ldap:\\host:port\n" +
                " -d            debug (verbose) mode\n" +
                " -e            exit on error, returning -1\n" +
                " -f filename   name of an ldif changes input file\n" +
                " -h [subject]  this help message [full|changetype]\n" +
                " -p password   an option password\n" +
                " -r referral   the jndi referral type [follow|ignore|throw]\n" +
                " -t            set BER tracing on\n" +
                " -u userdn     an optional user dn\n" +
                " -v            set ldap version (default 3)\n\n" +
                " -x            print stack trace\n\n" +
                "in addition to normal ldif changes commands (cf draft-good-ldap-ldif-04.html) a:\n" +
                "new keyword 'actiontype' with values [list|search|read|connect|disconnect]\n" +
                "is defined, with a usage similar to 'changetype'");
    }

    protected static void printFullHelp()
    {
        printChangeTypeHelp();
    }

    protected static void printChangeTypeHelp()
    {
        System.out.println("" +
                " *    This is a general test bed interface to test the various jndi calls \n" +
                " *    used by JXplorer.  It used ldif change file format requests, and can \n" +
                " *    be run directly from the command line, or given an ldif change file\n" +
                " *    to process.<p>\n" +
                " *\n" +
                " *    Requests are either standard ldif change file requests, such as:\n" +
                " *\n" +
                " *    version: 1\n" +
                " *    # Add a new entry\n" +
                " *     changetype: add\n" +
                " *     objectclass: top\n" +
                " *     objectclass: person\n" +
                " *     objectclass: organizationalPerson\n" +
                " *     cn: Fiona Jensen\n" +
                " *     sn: Jensen\n" +
                " *     uid: fiona\n" +
                " *     telephonenumber: +1 408 555 1212\n" +
                " *     jpegphoto:< file:///usr/local/directory/photos/fiona.jpg\n" +
                " *    <pre>\n" +
                " *    where valid 'changetype' values are [add|delete|modrdn|modify] \n" +
                " *    This is extended by jnditest to include extra (non-standard) changetype\n" +
                " *    commands, such as [connect|disconnect|list|search|searchOneLevel|read], to allow\n" +
                " *    general directory access and testing.\n" +
                " *    The 'connect' command takes the attributes; 'url', and the optional \n" +
                " *    attributes: 'user','pwd','tracing','ldapVersion','referral' and 'useSSL' \n" +
                " *\n" +
                " *    XXX coming soon - [copyTree|deleteTree|moveTree]\n" +
                " *");
    }

    /**
     * Constructs a jnditest object, opening a connection (if non-null input is
     * passed for connection opening) and opening an input file (if a non-null
     * file name is passed).<p>
     * <p/>
     * All the following parameters can be null.
     *
     * @param fileName       the name of an LDIF changes input file.
     * @param url            a url of the form ldap://hostname:portnumber.
     * @param user           a user to bind to the directory as.
     * @param pwd            the user's password.
     * @param tracing        whether to set BER tracing on or not.
     * @param version        the LDAP Version (2 or 3) being used.
     * @param debugFlag      echo all system statement.
     * @param terminateFlag  exit on error, returning -1.
     * @param referral       the jndi referral type [follow|ignore|throw].
     * @param useSSL         to use SSL.
     * @param printstackFlag whether to print a stack trace.
     */

    public TestJNDIOps(String fileName, String url, String user, String pwd, boolean tracing, String version, boolean debugFlag, boolean terminateFlag, String referral, boolean useSSL, boolean printstackFlag)
    {
        out = System.out;  // may want to make this configurable in future :-)

        debug = debugFlag;
        terminating = terminateFlag;
        printstack = printstackFlag;

        // open a connection (if we've been given one)
        if (url != null)
            openConnection(url, user, pwd, tracing, version, referral, useSSL);

        // open an input stream - file if we have one, console if not...
        if (fileName != null)
            in = openFile(fileName);
        else
        {
            in = null;
            try
            {
                in = new LdifStreamReader(new InputStreamReader(System.in));
            }
            catch (Exception e)
            {
                error("unable to open system input stream reader", e);
                System.exit(-1);
            }
        }
    }

    /**
     * Open an ldap connection using jndi, via the standard JXplorer classes
     * (DXOps.java, which wraps BasicOps.java).
     *
     * @param url          a url of the form ldap://hostname:portnumber
     * @param user         a user to bind to the directory as.
     * @param pwd          the user's password.
     * @param tracing      whether to set BER tracing on or not
     * @param version      the LDAP Version (2 or 3) being used.
     * @param referralType the jndi referral type [follow|ignore|throw].
     * @param useSSL       to use SSL.
     */

    public void openConnection(String url, String user, String pwd, boolean tracing, String version, String referralType, boolean useSSL)
    {
        if (referralType == null) referralType = "follow";
        if ("ignorefollowthrow".indexOf(referralType) == -1)
        {
            error("unknown referraltype " + referralType, null);
            referralType = "follow";
        }

        if (url.toLowerCase().startsWith("ldap://") == false)
            url = "ldap://" + url;

        try
        {
            if (debug) out.println("opening connection with :\n url: " + url + "\n user: " + user + "\n pwd: " + pwd + "\n tracing: " + tracing + "\n version: " + version + "\n referral: " + referralType);

            if ((url == null) || (url.length() == 0))
            {
                error("unable to open connection - no url supplied", null);
                return;
            }

            if ((version == null) || (version.length() == 0)) version = "3"; // default to ldap version 3

            char[] password = (pwd == null || pwd.length() == 0) ? null : pwd.toCharArray();
            int versn = Integer.parseInt(version);

            ConnectionData cData = new ConnectionData(versn, url, user, password, tracing, null, null);
            LdapContext ctx = BasicOps.openContext(cData);
            //LdapContext ctx = BasicOps.openContext(versn, url, user, password, tracing, null, null);
            if (ctx != null)
                myOps = new DXOps(ctx);

            if (myOps == null)
                error("unable to open connection " + url, null);
            else if (debug) out.println("connection " + url + " open. ");
        }
        catch (Exception e)
        {
            error("error opening connection " + url + "\n  ", e);

        }
    }

    /**
     * Opens a text file as a Buffered Reader.
     *
     * @param fileName the name of the file to open.
     * @return the Input Stream.
     */

    public LdifStreamReader openFile(String fileName)
    {
        try
        {
            File myFile = new File(fileName);
            ldifutil.setFileDir(myFile.getParent());
            return new LdifStreamReader(new InputStreamReader(new FileInputStream(myFile)));
        }
        catch (Exception e)
        {
            error("unable to open file : " + fileName + "\n  ", e);
            System.exit(-1);
        }
        return null;
    }

    /**
     * Reads input as an ldif entry at a time
     * (i.e. as a series of colon seperated att:value pairs, until
     * a blank line or eof is read).
     */

    public void processInput()
    {
        // start the infinite loop...
        try
        {
            DXEntry commandSet;
            while ((commandSet = ldifutil.readLdifEntry(in)) != null)
            {
                if (commandSet == null)
                    error("internal error in jnditest - parsed ldif command set null", null);

                if (processCommand(commandSet) == FINISHED)
                    return;
            }
        }
        catch (Exception e)
        {
            error("error reading input - program stopped." + "\n  ", e);
            System.exit(-1);
        }
    }

    /**
     * This checks that an entry is valid, and
     * then passes it to the appropriate command handler.
     *
     * @param entry the DXEntry object containing a dn attribute,
     *              a changetype attribute, and any data attributes required
     *              by the ldif command.
     * @return code - 0 = normal, 1 = error, -1 = finished.
     */

    public int processCommand(DXEntry entry)
    {
        // do some sanity checking
        if (entry.size() == 0)
        {
            if (debug) out.println("\n\nEnding on double blank line");
            return FINISHED;
        }

        if (entry.get("version") != null)
            entry.remove("version");         // strip out version number.

        if (entry.getDN().size() == 0)
        {
            DXAttribute temp = (DXAttribute) entry.get("changetype");
            String test = "";
            try
            {
                test = temp.get().toString();
            }
            catch (Exception e)
            {
            } // never happen
            // two commands do not require a dn...
            if ((test.equalsIgnoreCase("connect") == false) &&
                    (test.equalsIgnoreCase("disconnect") == false))
            {
                error("error reading input - no dn attribute in entry!\n*******\n" + entry + "******\n", null);
                return ERROR;
            }
        }

        DXAttribute command = (DXAttribute) entry.get("changetype");
        if (command == null || command.size() == 0)
        {
            error("error reading input - no 'changetype' attribute in entry.\n******\n" + entry + "******\n", null);
            return ERROR;
        }

        String commandString = "";

        try
        {
            commandString = command.get().toString();
        }
        catch (NamingException e)
        {
            error("internal error in processCommand()\n  ", e);
            return ERROR;
        }    // never happen :-)

        if (debug) System.out.println("\n\nCOMMAND= " + commandString);

        entry.remove("changetype");

        if ((myOps == null) && !(commandString.equalsIgnoreCase("connect") || commandString.equalsIgnoreCase("disconnect")))
            error("Attempting operation " + commandString + " without an open connection!", null);

        // branch to appropriate handler
        try
        {
            if (commandString.equalsIgnoreCase("add"))
                addEntry(entry, returnType(entry));
            else if (commandString.equalsIgnoreCase("delete"))
                deleteEntry(entry, returnType(entry));
            else if (commandString.equalsIgnoreCase("modrdn"))
                modrdnEntry(entry, modRDN(entry));
            else if (commandString.equalsIgnoreCase("modify"))
                modifyEntry(entry, returnType(entry));
            else if (commandString.equalsIgnoreCase("list"))
                listEntry(entry, list(entry));
            else if (commandString.equalsIgnoreCase("search"))
                searchEntry(entry, search(entry));
            else if (commandString.equalsIgnoreCase("searchOneLevel"))
                searchOneLevelEntry(entry, search(entry));
            else if (commandString.equalsIgnoreCase("read"))
                readEntry(entry, read(entry));
            else if (commandString.equalsIgnoreCase("connect"))
                connect(entry);
            else if (commandString.equalsIgnoreCase("disconnect"))
                disconnect(entry);
            else if (commandString.equalsIgnoreCase("copy"))
                copy(entry, copy(entry, command));
            else if (commandString.equalsIgnoreCase("cut"))
                cut(entry, cut(entry, command));
        }
        catch (NamingException e)
        {
            error("Naming Exception in " + commandString + "\n  ", e);
            return ERROR;
        }

        return OK;
    }


    /**
     * Gets the 'returntype' value from the test ldif file and parses it into a string.
     * 'returntype' is a boolean value pertaining to the add, delete or modify 'changetype'
     * functions in the ldif file. i.e. it signifies if an entry should actually be
     * added or deleted. Changes the value to boolean. Removes the 'returntype' attribute.
     *
     * @param entry the DXEntry object containing a dn attribute, a changetype attribute, and any data attributes required.
     * @return expectedVal a boolean value that represents the success or failure of the modification.
     */

    public boolean returnType(DXEntry entry)
    {

        DXAttribute returnType = (DXAttribute) entry.get("returntype");

        String myType = "";
        try
        {
            myType = returnType.get().toString();
        }
        catch (Exception e)
        {
        }

        boolean expectedVal = "true".equalsIgnoreCase(myType);

        if (returnType != null)
        {
            if (debug) System.out.println("\n\nparsed returntype: " + myType + "\n as: " + expectedVal + "\n");
            entry.remove("returntype");
        }
        return expectedVal;
    }


    /**
     * Add an entry to a directory.  Compares the 'returntype' value in the ldif file (if
     * present) to the result of the add operation.  If the two don't match the program exits.
     *
     * @param entry         contains the att/val pairs to be added, and the dn to add them to.
     * @param expectedValue a flag that indicates the modification success.
     */

    public void addEntry(DXEntry entry, boolean expectedValue)
    {
        if (debug) System.out.println("\nADD: " + entry);

        boolean ret = false;

        try
        {
            myOps.addEntry(entry.getDN(), entry);
            ret = true;
        }
        catch (NamingException e)
        {
        }

        //TE: flag to test if the modification succeeded or not.

        if (ret != expectedValue)                   //TE: tests if the modification status is what we expected.
        {
            if (debug) System.out.println("\nret equals expectedValue: " + (ret == expectedValue) + "\n");
            if (debug) System.out.println("\n&&&& RET: " + ret + "  &&&& EXPECTEDVALUE: " + expectedValue);
            error("\nadd operation failed for: " + entry + "\n", null);
        }
    }


    /**
     * Delete an entry from a directory. Compares the 'returntype' value in the ldif file (if
     * present) to the result of the delete operation.  If the two don't match the program exits.
     *
     * @param entry         contains dn to delete.
     * @param expectedValue exptectedValue a flag that indicates the modification success.
     */

    public void deleteEntry(DXEntry entry, boolean expectedValue)
    {
        if (debug) System.out.println("\nDELETE: " + entry);

        boolean ret = false;

        try
        {
            myOps.deleteEntry(entry.getDN());
            ret = true;
        }
        catch (NamingException e)
        {
        }

        //TE: flag to test if the modification succeeded or not.

        if (ret != expectedValue)                   //TE: tests if the modification status is what we expected.
        {
            if (debug) System.out.println("\n\nRET  " + ret + "  EXPECTEDVALUE  " + expectedValue);
            if (debug) System.out.println("\nret equals expectedValue: " + (ret == expectedValue) + "\n");
            error("\ndelete operation failed for: " + entry + "\n", null);
        }
    }


    /**
     * Gets the 'modrdnresult' value from the test ldif file and parses into a string.
     * 'modrdnresult' is the 'cn' attribute of the entry after the rename.
     * Removes the 'modrdnresult' attribute. Note: 'modrdnresult' can be configured to
     * represent any string attribute.
     *
     * @param entry contains the dn to start searching from, and a 'filter' attribute with a search filter.
     * @return myModRdnCn
     */

    public String modRDN(DXEntry entry)
    {

        DXAttribute modRdnResult = (DXAttribute) entry.get("modrdnresult");

        String myModRdnCn = "";
        try
        {
            myModRdnCn = modRdnResult.get().toString();
        }
        catch (Exception e)
        {
        }

        if (modRdnResult != null)
        {
            if (debug) System.out.println("\n\nparsed modrdnresult (DXAttribute): " + modRdnResult + "\n to mymodrdnCn(String): " + myModRdnCn);
            entry.remove("modrdnresult");
        }
        return myModRdnCn;
    }


    /**
     * Modifies the rdn of a directory.  Compares the 'modrdnresult' value in the ldif file (if
     * present) to the rdn of the entry.  If the two don't match the program exits.
     *
     * @param entry          contains dn to be modified.
     * @param expectedModrdn a flag that indicates the value that the modify is expected to return.
     */

    public void modrdnEntry(DXEntry entry, String expectedModrdn)
    {
        String newDNString = entry.getString("newrdn");
        DN newDN = new DN(newDNString);
        if (debug) System.out.println("modrdn: " + entry.getDN() + "\n  to: " + newDN);

        boolean ret = false;

        try
        {
            myOps.renameEntry(entry.getDN(), newDN);
            ret = true;
        }
        catch (NamingException e)
        {
        }

        if (ret == false)
            error("modrdn operation failed for: " + entry, null);

        int compare = -2;                           //TE: the flag to compare the expected DN with the actual DN.

        compare = expectedModrdn.compareTo(newDNString);    //TE: the compare of the expected DN with the actual DN.

        if (compare != 0)                            //TE: if 0, the two results are the same, therefore the modify performed as expected.
        {
            if (debug) System.out.println("\n\nnewDN CN String: " + newDNString);
            if (debug) System.out.println("EXPECTEDVALUE  : " + expectedModrdn);
            error("\nmodrdn operation failed for: " + entry + "\nExpected read result for cn: " + expectedModrdn + "\nActual result for cn: " + newDNString, null);
        }
    }


    /**
     * Modifies the attributes of an entry in a directory.  Compares the 'returntype' value in the ldif file (if
     * present) to the result of the modify operation.  If the two don't match the program exits.
     *
     * @param entry          contains the entry to be modified, and the list of att/val modification pairs (see ldif spec.).
     * @param expectedValue a flag that indicates the modification success.
     */

    public void modifyEntry(DXEntry entry, boolean expectedValue)
    {
        if (debug) System.out.println("modify: " + entry);

        Name myDN = entry.getDN();


        /*TE:   The three operations to modify an entry are:
                                add
                                delete
                                replace
                A test is done to see which operation is to be carried out.

                Note: within one ldif entry at this stage we can't do multiple operations
                on the same attribute or the same operations on multiple attributes
                because of the way the parser works,
                for example:
                                dn: cn=T,ou=Applications,ou=Customer,o=Democorp,c=AU
                                changetype: modify
                                add: drink
                                drink: red wine
                                drink: white wine
                                -
                                delete: drink
                                drink: red wine

                would result in both the 'drink' attributes being deleted.  To delete just the
                'red wine' attribute, write another ldif entry,
                for example:
                                dn: cn=T,ou=Applications,ou=Customer,o=Democorp,c=AU
                                changetype: modify
                                delete: drink
                                drink: red wine

                however, deleting another attribute not used by the add operation should work fine
                for example:
                                dn: cn=T,ou=Applications,ou=Customer,o=Democorp,c=AU
                                changetype: modify
                                add: drink
                                drink: red wine
                                drink: white wine
                                -
                                delete: cn
                                cn: TEST
        */

        DXAttribute add = null;
        DXAttribute delete = null;
        DXAttribute replace = null;

        add = (DXAttribute) entry.get("add");
        delete = (DXAttribute) entry.get("delete");
        replace = (DXAttribute) entry.get("replace");


        //TE:   Adds new attribute values to the specified attribute in the directory,
        //      for example:
        //                      dn: cn=T,ou=Applications,ou=Customer,o=Democorp,c=AU
        //                      changetype: modify
        //                      add: drink
        //                      drink: red wine
        //                      drink: white wine
        //                      drink: water
        //
        //      will add three new 'drink' attributes to the entry.

        if (add != null)                             //TE: tests if the modify operation is 'add'.
        {
            String attrString = "";
            try
            {
                attrString = add.get().toString();
            }
            catch (Exception e)
            {
            } // never happen

            DXAttribute attr = (DXAttribute) entry.get(attrString);

            boolean ret = false;

            try
            {
                myOps.addAttribute(myDN, attr);
                ret = true;
            }
            catch (NamingException e1)
            {
            }

            //TE: flag to test if the modification succeeded or not.

            if (ret != expectedValue)               //TE: tests if the modification status is what we expected.
            {
                if (debug) System.out.println("\n\nRET  " + ret + "  EXPECTEDVALUE  " + expectedValue);
                if (debug) System.out.println("\nret equals expectedValue: " + (ret == expectedValue) + "\n");
                error("\nmodify-add operation failed for: " + entry + "\n", null);
            }
        }


        //TE:   Deletes attribute values of a specified attribute within a specified entry,
        //      for example:
        //                      dn: cn=T,ou=Applications,ou=Customer,o=Democorp,c=AU
        //                      changetype: modify
        //                      delete: drink
        //                      drink: red wine
        //                      drink: white wine
        //                      drink: water
        //
        //      will delete these three 'drink' attributes from the entry.

        if (delete != null)                          //TE: tests if the modify operation is 'delete'.
        {
            String attrString = "";
            try
            {
                attrString = delete.get().toString();
            }
            catch (Exception e)
            {
            } // never happen

            DXAttribute attr = (DXAttribute) entry.get(attrString);

            boolean ret = false;

            try
            {
                myOps.deleteAttribute(myDN, attr);
                ret = true;
            }
            catch (NamingException e1)
            {
            }

            //TE: flag to test if the modification succeeded or not.

            if (ret != expectedValue)               //TE: tests if the modification status is what we expected.
            {
                if (debug) System.out.println("\n\nRET  " + ret + "  EXPECTEDVALUE  " + expectedValue);
                if (debug) System.out.println("\nret equals expectedValue: " + (ret == expectedValue) + "\n");
                error("\nmodify-delete operation failed for: " + entry + "\n", null);
            }
        }


        //TE:   Replaces all of the values of a specified attribute in the specified entry,
        //      for example:
        //                      dn: cn=T,ou=Applications,ou=Customer,o=Democorp,c=AU
        //                      changetype: modify
        //                      replace: drink
        //                      drink: beer
        //
        //      will replace all of the current values of attribute 'drink' with value 'beer'.
        //      In other words, if there are currently multiple 'drink' attributes they all
        //      will be removed and replaced with this one new value.

        if (replace != null)                         //TE: tests if the modify operation is 'replace'.
        {
            String attrString = "";
            try
            {
                attrString = replace.get().toString();
            }
            catch (Exception e)
            {
            } // never happen

            DXAttribute attr = (DXAttribute) entry.get(attrString);

            boolean ret = false;

            try
            {
                myOps.updateAttribute(myDN, attr);
                ret = true;
            }
            catch (NamingException e1)
            {
            }

            //TE: flag to test if the modification succeeded or not.

            if (ret != expectedValue)               //TE: tests if the modification status is what we expected.
            {
                if (debug) System.out.println("\n\nRET  " + ret + "  EXPECTEDVALUE  " + expectedValue);
                if (debug) System.out.println("\nret equals expectedValue: " + (ret == expectedValue) + "\n");
                error("\nmodify-replace operation failed for: " + entry + "\n", null);
            }
        }
    }


    /**
     * Gets the 'listresult' from the test ldif file and parses it into a string.
     * 'searchresult' is an integer value pertaining to the amount of list results
     * expected to be returned from a search. Converts to int. Removes the 'listresult' attribute.
     *
     * @param entry the DXEntry object containing a dn attribute, a changetype attribute, and any data attributes required.
     * @return list an integer value representing the amount of entries that are expected to be returned by the list.
     */

    public int list(DXEntry entry)
    {

        DXAttribute listResult = (DXAttribute) entry.get("listresult");

        String myListResult = "";
        try
        {
            myListResult = listResult.get().toString();
        }
        catch (Exception e)
        {
        }

        int list = -1;
        try
        {
            list = Integer.parseInt(myListResult);
        }
        catch (Exception e)
        {
        }

        if (listResult != null)
        {
            if (debug) System.out.println("\n\nparsed listresult(DXAttribute): " + listResult + "\n to myListResult(String): " + myListResult + ", to list(int): " + list);
            entry.remove("listresult");
        }
        return list;
    }


    /**
     * Lists an entry in a directory.  Compares the 'listresult' value in the ldif file to the
     * number of entries returned by the list.  If the two are not equal the program exists.
     *
     * @param entry        contains the dn to start searching from, and a 'filter' attribute with a search filter.
     * @param expectedList a flag that indicates the number of entries that are expected to be listed.
     */

    public void listEntry(DXEntry entry, int expectedList)
            throws NamingException
    {
        if (debug) System.out.println("\nlist: " + entry);
        NamingEnumeration names = myOps.list(entry.getDN());

        if (debug) out.println("\nlist of children:");

        int i = 0;                                  //TE: the flag to compare the expected number of list results with the actual number of list results.

        while (names.hasMore())                     //TE: Counts & lists entries.
        {
            i++;
            if (debug) out.println(((NameClassPair) names.next()).getName());
        }

        if (i != expectedList)
            error("\nList operation failed for: " + entry + "\nExpected list results: " + expectedList + "\nActual list results: " + i, null);
    }


    /**
     * Gets the 'searchresult' value from the test ldif file and parses it into a string.
     * 'searchresult' is an integer value pertaining to the amount of search results
     * expected to be returned from a search. Converts to int. Removes the 'searchresult' attribute.
     *
     * @param entry the DXEntry object containing a dn attribute, a changetype attribute, and any data attributes required.
     * @return search an integer value representing the amount of entries expected to be returned by the search.
     */

    public int search(DXEntry entry)
    {

        DXAttribute searchResult = (DXAttribute) entry.get("searchresult");

        String mySearch = "";
        try
        {
            mySearch = searchResult.get().toString();
        }
        catch (Exception e)
        {
        }

        int search = -1;
        try
        {
            search = Integer.parseInt(mySearch);
        }
        catch (Exception e)
        {
        }

        if (searchResult != null)
        {
            if (debug) System.out.println("\n\nparsed searchresult(DXAttribute): " + searchResult + "\n to mySearch(String): " + mySearch + ", to myResult(int): " + search);
            entry.remove("searchresult");
        }
        return search;
    }


    /**
     * Searches a directory.  Compares the 'searchresult' value in the ldif file to the
     * number of entries returned by the search.  If the two are not equal the program exists.
     *
     * @param entry          contains.
     * @param expectedSearch a flag that indicates the number of results returned by the search.
     *                       <ul>
     *                       <li> dn to start searching from
     *                       <li> a 'filter' attribute containing an ldap search filter
     *                       <li> an (optional) 'limit' attribute containing a number of returned entries limit
     *                       <li> an (optional) 'timeout' attribute containing a time limit (milliseconds).
     *                       </ul>
     */

    public void searchEntry(DXEntry entry, int expectedSearch)
            throws NamingException
    {
        String filter = entry.getString("filter");
        String limit = entry.getString("limit");
        String timeout = entry.getString("timeout");
        if (limit == null) limit = "0";
        if (timeout == null) timeout = "0";

        int lim = Integer.parseInt(limit);
        int time = Integer.parseInt(timeout);

        if (debug) System.out.println("\nSEARCH " + entry + "\n  filter: " + filter + "\n  limit: " + limit + "\n  timeout: " + timeout);


        NamingEnumeration names = myOps.searchSubTree(entry.getDN(), filter, lim, time);
        if (debug) out.println("\nNAMES:  " + names + "\n\nDN:  " + entry);

        if (debug) out.println("\nsubtree search results:");

        int i = 0;                                  //TE: the flag to compare the expected number of search results with the actual number of search results.

        while (names.hasMore())                     //TE: Counts & lists search results.
        {
            i++;
            if (debug) out.println(((SearchResult) names.next()).getName());
        }

        if (i != expectedSearch)
            error("\nSearch operation failed for: " + entry + "\nExpected search results: " + expectedSearch + "\nActual search results: " + i, null);
    }


    /**
     * Searches one level of a directory.  Compares the 'searchresult' value in the ldif file to the
     * number of entries returned by the search one level.  If the two are not equal the program exists.
     *
     * @param entry          contains.
     * @param expectedSearch a flag that indicates the number of results returned by the search.
     *                       <ul>
     *                       <li> dn to start searching from.
     *                       <li> a 'filter' attribute containing an ldap search filter.
     *                       <li> an (optional) 'limit' attribute containing a number of returned entries limit.
     *                       <li> an (optional) 'timeout' attribute containing a time limit (milliseconds).
     *                       </ul>
     */

    public void searchOneLevelEntry(DXEntry entry, int expectedSearch)
            throws NamingException
    {
        String filter = entry.getString("filter");
        String limit = entry.getString("limit");
        String timeout = entry.getString("timeout");
        if (limit == null) limit = "0";
        if (timeout == null) timeout = "0";

        if (debug) System.out.println("\n\nSEARCHONELEVEL: " + entry + "\n  filter: " + filter + "\n  limit: " + limit + "\n  timeout: " + timeout);

        int lim = Integer.parseInt(limit);
        int time = Integer.parseInt(timeout);

        NamingEnumeration names = myOps.searchOneLevel(entry.getDN(), filter, lim, time);
        if (debug) out.println("\n\none level search results:");

        int i = 0;                                  //TE: the flag to compare the expected number of search results with the actual number of search results.

        while (names.hasMore())                     //TE: Counts & lists search results.
        {
            i++;
            if (debug) out.println(((SearchResult) names.next()).getName());
        }

        if (i != expectedSearch)
            error("\n\nSearchOneLevel operation failed for: " + entry + "\n\nExpected search results: " + expectedSearch + "\nActual search results: " + i, null);
    }


    /**
     * Gets the 'readresultcn' value from the test ldif file and parses into a string.
     * 'readresultcn' is the 'cn' attribute of the entry, for example cn: Cora BALDWIN.
     * Removes the 'readresultcn' attribute. Note: 'readresultcn' can be configured to
     * represent any string attribute.
     *
     * @param entry the DXEntry object containing a dn attribute, a changetype attribute, and any data attributes required.
     * @return myReadCn a string 'cn' that is expected to be returned by the read.
     */

    public String read(DXEntry entry)
    {
        DXAttribute readResultCn = (DXAttribute) entry.get("readresultcn");

        String myReadCn = "";
        try
        {
            myReadCn = readResultCn.get().toString();
        }
        catch (Exception e)
        {
        }

        if (readResultCn != null)
        {
            if (debug) System.out.println("\n\nparsed readresultcn (DXAttribute): " + readResultCn + "\n to myReadCn(String): " + myReadCn);
            entry.remove("readresultcn");
        }
        return myReadCn;
    }


    /**
     * Reads an entry in a directory.  Currently compares the 'readresultcn' value in the ldif file
     * (can be configured to any value), to the 'cn' value of the result of the read.  If the two
     * values are not equal the program exits.
     *
     * @param entry        contains the dn to list the attributes of.
     * @param expectedRead a flag that indicates the value that the read is expected to return.
     */

    public void readEntry(DXEntry entry, String expectedRead)
            throws NamingException
    {
        if (debug) System.out.println("\nread: " + entry);

        Attributes atts = myOps.read(entry.getDN());
        if (atts == null)
        {
            throw new NamingException("\nUnable to read entry " + entry.getDN());
        }

        Attribute readCn = (Attribute) atts.get("cn");
        if (debug) out.println("\nREAD: " + readCn);

        String myReadCn = "";
        try
        {
            myReadCn = readCn.get().toString();
        }
        catch (Exception e)
        {
        }

        int compare = -2;                           //TE: the flag to compare the expected read with the actual read.

        compare = expectedRead.compareTo(myReadCn); //TE: the compare of the expected read with the actual read.

        if (compare != 0)                            //TE: if 0, the two results are the same, therefore the read performed as expected.
        {
            if (debug) out.println("\n\nREAD CN String: " + myReadCn);
            if (debug) out.println("EXPECTEDVALUE : " + expectedRead);
            error("\nRead operation failed for: " + entry + "\nExpected read result for cn: " + expectedRead + "\nActual read result for cn:   " + myReadCn, null);
        }

        DXEntry val = new DXEntry(atts);
        if (debug) out.println(val);
    }


    /**
     * Gets the 'copyTo' value from the test ldif file and parses into a string.
     * 'copyTo' is the 'DN' of the new entry to where the DN of the old entry gets copied to.
     * Removes the 'copyTo' attribute.
     *
     * @param entry   the DXEntry object containing a dn attribute, a changetype attribute, and any data attributes required.
     * @param command the type of test to be performed, i.e copy.
     * @return myCopyTo the DN of the where the old entry is to be copy (or moved) to.
     */

    public String copy(DXEntry entry, DXAttribute command)
    {
        DXAttribute copyTo = (DXAttribute) entry.get("copyTo");

        String myCopyTo = "";
        try
        {
            myCopyTo = copyTo.get().toString();
        }
        catch (Exception e)
        {
        }

        if (copyTo != null)
        {
            if (debug) System.out.println("copyTo: " + copyTo + "\ncommand: " + command + "\nmyCopyTo: " + myCopyTo);
            if (debug) System.out.println("\n\nparsed copyTo (DXAttribute): " + copyTo + "\n to myCopyTo(String): " + myCopyTo);
            entry.remove("copyTo");
        }
        return myCopyTo;
    }


    /**
     * Copies an entry to a new DN.
     *
     * @param oldEntry is what is being copied.
     * @param newEntry is the new DN of where the entry is copied to.
     */

    public void copy(DXEntry oldEntry, String newEntry)
    {


        Name newDN = myOps.postParse(newEntry);     //TE: Converts the new DN string (copyTo) from the LDIF file into Name.
        Name oldDN = oldEntry.getDN();

        if (debug) System.out.println("old DN: " + oldDN);
        if (debug) System.out.println("new DN: " + newDN);

        NamingEnumeration namesOld;

        try
        {
            namesOld = myOps.list(oldDN);
        }
        catch (NamingException e1)
        {
            System.err.println("failed getting old names");
            e1.printStackTrace();
            return;
        }

        if (debug) out.println("\nCopy of OLD  children:");

        int n = 0;                                  //TE: Counter for OLD entries.

        try
        {
            while (namesOld.hasMore())              //TE: Counts & lists OLD entries.
            {
                n++;
                if (debug) out.println("Old Entries: " + ((NameClassPair) namesOld.next()).getName());
            }
        }
        catch (Exception e)
        {
            if (debug) System.out.println("List for OLD entries failed during copy process");
        }

        try
        {
            myOps.copyTree(oldDN, newDN, true);
        }
        catch (NamingException e2)
        {
            System.err.println("error in copyTree()");
            e2.printStackTrace();
        }

        //TE: copies old entry to new entry.

        if (debug) System.out.println("Copy: " + oldEntry);
        NamingEnumeration namesNew;

        try
        {
            namesNew = myOps.list(newDN);
        }
        catch (NamingException e3)
        {
            System.err.println("error in getting new list");
            e3.printStackTrace();
            return;
        }


        if (debug) out.println("\nCopy of NEW children:");

        int i = 0;                                  //TE: Counter for NEW entries.

        try
        {
            while (namesNew.hasMore())              //TE: Counts & lists NEW entries.
            {
                i++;
                if (debug) out.println("New Entries: " + ((NameClassPair) namesNew.next()).getName());
            }
        }
        catch (Exception e)
        {
            if (debug) System.out.println("List for NEW entries failed during copy process");
        }

        if (i != n)                                  //TE: checks that the list contains the same number of entries.
            error("\nCopy operation failed for: " + oldEntry + "\nExpected number of copied entries: " + n + "\nActual number of copied entries: " + i, null);
    }


    /**
     * Gets the 'cutTo' value from the test ldif file and parses into a string.
     * 'cutTo' is the 'DN' of the new entry to where the DN of the old entry gets cut (or moved) to.
     * Removes the 'cutTo' attribute.
     *
     * @param entry   the DXEntry object containing a dn attribute, a changetype attribute, and any data attributes required.
     * @param command the type of test to be performed, i.e cut.
     * @return myCutTo the DN of the where the old entry is to be cut (or moved) to.
     */

    public String cut(DXEntry entry, DXAttribute command)
    {
        DXAttribute cutTo = (DXAttribute) entry.get("cutTo");

        String myCutTo = "";
        try
        {
            myCutTo = cutTo.get().toString();
        }
        catch (Exception e)
        {
        }

        if (cutTo != null)
        {
            if (debug) System.out.println("cutTo: " + cutTo + "\ncommand: " + command + "\nmyCutTo: " + myCutTo);
            if (debug) System.out.println("\n\nparsed cutTo (DXAttribute): " + cutTo + "\n to myCutTo(String): " + myCutTo);
            entry.remove("cutTo");
        }
        return myCutTo;
    }


    /**
     * Cuts an entry to a new DN.
     *
     * @param oldEntry is what is being cut.
     * @param newEntry is the new DN of where the entry is cut to.
     */

    public void cut(DXEntry oldEntry, String newEntry)
    {


        Name newDN = myOps.postParse(newEntry);     //TE: Converts the new DN string (copyTo) from the LDIF file into Name.
        Name oldDN = oldEntry.getDN();

        if (debug) System.out.println("old: " + oldDN);
        if (debug) System.out.println("new: " + newDN);

        NamingEnumeration namesOld;

        try
        {
            namesOld = myOps.list(oldDN);
        }
        catch (NamingException e1)
        {
            System.err.println("error getting namesOld");
            e1.printStackTrace();
            return;
        }

        if (debug) out.println("\nCut of OLD children:");

        int n = 0;                                  //TE: Counter for OLD entries.

        try
        {
            while (namesOld.hasMore())              //TE: Counts & lists OLD entries.
            {
                n++;
                if (debug) out.println("Old Entries: " + ((NameClassPair) namesOld.next()).getName());
            }
        }
        catch (Exception e)
        {
            if (debug) System.out.println("List for OLD entries failed during cut process");
        }

        try
        {
            myOps.moveTree(oldDN, newDN);
        }
        catch (NamingException e2)
        {
            System.err.println("error in moveTree()");
            e2.printStackTrace();
        }            //TE: cuts (or moves) old entry to new entry.

        if (debug) System.out.println("Copy: " + oldEntry);
        NamingEnumeration namesNew;

        try
        {
            namesNew = myOps.list(newDN);
        }
        catch (NamingException e3)
        {
            System.err.println("error getting namesNew");
            e3.printStackTrace();
            return;
        }


        if (debug) out.println("\nCut of NEW children:");

        int i = 0;                                  //TE: Counter for NEW entries.

        try
        {
            while (namesNew.hasMore())              //TE: Counts & lists NEW entries.
            {
                i++;
                if (debug) out.println("New Entries: " + ((NameClassPair) namesNew.next()).getName());
            }
        }
        catch (Exception e)
        {
            if (debug) System.out.println("List for NEW entries failed during cut process");
        }

        if (i != n)                                  //TE: checks that the list contains the same number of entries.
            error("\nCut operation failed for: " + oldEntry + "\nExpected number of cut entries: " + n + "\nActual number of cut entries: " + i, null);
    }


    /**
     * Opens a connection.
     *
     * @param entry a 'fake' entry with no dn, but a bunch of attributes.
     */

    public void connect(DXEntry entry)
    {
        if (debug) System.out.println("connect: " + entry);
        if (myOps != null)
            try
            {
                myOps.close();
            }
            catch (NamingException e)
            {
                System.err.println("error in myOps.close()");
                e.printStackTrace();
            }

        String url = entry.getString("url");
        String user = entry.getString("user");
        String pwd = entry.getString("pwd");
        String tracing = entry.getString("tracing");
        String version = entry.getString("ldapVersion");
        String referral = entry.getString("referral");
        String useSSL = entry.getString("useSSL");

        boolean trace = ((tracing != null) && (tracing.equalsIgnoreCase("true")));
        boolean ssl = ((useSSL != null) && (useSSL.equalsIgnoreCase("true")));
        openConnection(url, user, pwd, trace, version, referral, ssl);
    }


    /**
     * Disconnected from the directory.
     */

    public void disconnect(DXEntry entry)
    {
        if (debug) System.out.println("disconnected. ");
        try
        {
            myOps.close();
        }
        catch (NamingException e)
        {
            System.err.println("error in myOps.close()");
            e.printStackTrace();
        }
    }


    /**
     * Prints error message then terminates.
     */

    public void error(String msg, Exception e)
    {
        out.println(msg + "\n");

        if (e != null && printstack)
            e.printStackTrace();

        if (terminating)
            System.exit(-1);
    }
}