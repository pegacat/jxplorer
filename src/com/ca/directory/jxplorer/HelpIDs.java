package com.ca.directory.jxplorer;

/**
 * Data class that holds the help system IDs.
 * These are used when for linking a help
 * topic to a help button on a dialog.
 * @author Trudi.
 */
public class HelpIDs
{
    /**
     * Stop monitor dialog.
     */
    public static final String ABORT = "utilities.abort";

    /**
     * Audio editor dialog.
     */
    public static final String ATTR_AUDIO = "edit.audio";

    /**
     * Default editor dialog.
     */
    public static final String ATTR_BINARY = "edit.binary";

    /**
     * Boolean editor dialog.
     */
    public static final String ATTR_BOOLEAN = "adding_an_attribute_or_value_htm";  // XXX don't really have a specific help topic for this...

    /**
     * JPEGPhoto editor dialog.
     */
    public static final String ATTR_JPEGPHOTO = "edit.jpegphoto";

    /**
     * Password editor dialog.
     */
    public static final String ATTR_PASSWORD = "edit.password";

    /**
     * Address editor dialog.
     */
    public static final String ATTR_POSTAL = "edit.attributeeditor"; // need to rename

    /**
     * Generalized time editor dialog.
     */
    public static final String ATTR_TIME = "edit.time";

    /**
     * Change object class dialog.
     */
    public static final String CLASS_CHANGE = "edit.new";   // check

    /**
     * Advanced options dialog.
     */
    public static final String CONFIG_ADVANCED = "config.advanced";

    /**
     * Add bookmark dialog.
     */
    public static final String BOOKMARK_ADD = "adding_a_bookmark_htm";

    /**
     * Edit bookmark dialog.
     */
    public static final String BOOKMARK_EDIT = "editing_a_bookmark_htm";

    /**
     * Delete bookmark dialog.
     */
    public static final String BOOKMARK_DELETE = "deleting_a_bookmark_htm";

    /**
     * Connection dialog.
     */
    public static final String CONNECT = "browse.connection";

    /**
     * Customer support link.
     */
    //public static final String CONTACT_SUPPORT = "contact.support";

    /**
     * New entry dialog.
     */
    public static final String ENTRY_NEW = "edit.new";

    /**
     * LDIF export dialog (full subtree).
     */
    public static final String LDIF_EXPORT_SUBTREE = "ldif.fileio";

    /**
     * LDIF export dialog (full tree).
     */
    public static final String LDIF_EXPORT_TREE = "ldif.fileio";

    /**
     * Search dialog.
     */
    public static final String SEARCH = "browse.searching";

    /**
     * Delete search filter dialog.
     */
    public static final String SEARCH_DELETE_FILTER = "browse.searching";

    /**
     * Return attributes search results dialog.
     */
    public static final String SEARCH_RESULTS = "search.results";

    /**
     * Return attributes dialog.
     */
    public static final String SEARCH_RETURN_ATTRIBUTES = "return.attributes";

    /**
     * Manage certs dialog.
     */
    public static final String SSL_CERTS = "ssl.certs";

    /**
     * Keystore options dialog.
     */
    public static final String SSL_CHANGE_KEYSTORE = "ssl.changekeystore"; 

    /**
     * Change the keystore password dialog.
     */
    public static final String SSL_KEYSTORE_PASSWORD = "ssl.changekeystore";

    /**
     * Certificate dialog.
     */
    public static final String SSL_VIEW = "ssl.view";

    /**
     * Table of contents tab.
     */
    public static final String TAB_TOC = "TOC";

    /**
     * Search tab.
     */
    public static final String TAB_SEARCH = "Search";

    /**
     * JXWorkBench find and replace page.
     */
    public static final String JXWORKBENCH_SEARCH_REPLACE = "workbench.replace";

    /**
     * JXWorkbench password vault.
     */
    public static final String JXWORKBENCH_PASSWORD_VAULT = "workbench.vault";

    /**
     * JXWorkbench import / export
     */
    public static final String JXWORKBENCH_CSV = "workbench.io";


    // XXX remember to add any new IDs to the list in HelpIDsTest, which tests that they appear in the help system map!
}
