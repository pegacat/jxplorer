package com.ca.directory.jxplorer;

import com.ca.commons.cbutil.CBHelpSystem;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.JHelp;

/**
 * (c) Chris Betts: JXplorer
 *
 * Basic test suite to try to make sure that help ids are actually in the help system, and we don't have orphaned
 * links in the project.
 *
 * Help System needs a major overhaul (2012) as it's pretty old and clunky; this is a first step :-)
 *
 * Main problem is consistency checking; needing to make sure that the index and toc targets are correctly linked to the
 * map.xml targets, that the code references (the HelpIDs) correctly link to the map.xml targets,
 * and that the map.xml targets correctly link to real files.
 *
 * This class helps us check that the code references targets in map.xml, but we're still doing the other checking
 * manually :-(.  (Consider rewritting codestripper perl script to do matching between index, toc and map - unless
 * there's something on the web already?)
 */

public class HelpIDsTest extends TestCase
{
    
    public HelpIDsTest(String name)
    {
        super(name);
    }


    public static Test suite()
    {
        return new TestSuite(HelpIDsTest.class);
    }



    protected void setUp()
    {
    }



    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }


    public void testIDs()
    {
        String[] idArray = {HelpIDs.ABORT, 
         HelpIDs.ATTR_AUDIO
        , HelpIDs.ATTR_BINARY
        , HelpIDs.ATTR_BOOLEAN
        , HelpIDs.ATTR_JPEGPHOTO
        , HelpIDs.ATTR_PASSWORD
        , HelpIDs.ATTR_POSTAL
        , HelpIDs.ATTR_TIME
        , HelpIDs.CLASS_CHANGE
        , HelpIDs.CONFIG_ADVANCED
        , HelpIDs.BOOKMARK_ADD
        , HelpIDs.BOOKMARK_EDIT
        , HelpIDs.BOOKMARK_DELETE
        , HelpIDs.CONNECT
//        , HelpIDs.CONTACT_SUPPORT
        , HelpIDs.ENTRY_NEW
        , HelpIDs.LDIF_EXPORT_SUBTREE
        , HelpIDs.LDIF_EXPORT_TREE
        , HelpIDs.SEARCH
        , HelpIDs.SEARCH_DELETE_FILTER
        , HelpIDs.SEARCH_RESULTS
        , HelpIDs.SEARCH_RETURN_ATTRIBUTES
        , HelpIDs.SSL_CERTS
        , HelpIDs.SSL_CHANGE_KEYSTORE
        , HelpIDs.SSL_KEYSTORE_PASSWORD
        , HelpIDs.SSL_VIEW
        , HelpIDs.JXWORKBENCH_SEARCH_REPLACE
        , HelpIDs.JXWORKBENCH_PASSWORD_VAULT
        , HelpIDs.JXWORKBENCH_CSV
        };

        CBHelpSystem helpSystem = new CBHelpSystem("JXplorerHelp.hs");
        //HelpBroker broker = helpSystem.getHelpBroker();

        JHelp helpViewer = new JHelp(helpSystem.getHelpSet());

        String missingIDs = "";

        for (String id:idArray)
        {
            try
            {
                helpViewer.setCurrentID(id);
            }
            catch (Exception e)
            {
                System.out.println("missing: " + id);
                missingIDs += "'" + id + "' ";
            }

        }

        assertEquals("The following IDs are not enabled: ", "", missingIDs);
    }    

}
