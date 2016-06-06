package com.ca.directory.jxplorer.search;

import java.util.logging.Logger;

import com.ca.directory.jxplorer.tree.*;
import com.ca.directory.jxplorer.broker.*;
import com.ca.commons.naming.*;

/**
*	This executes a search request from a particular base DN, using
* 	a particular filter, returning a particular set of attributes.
*/

public class SearchExecute
{
    private static Logger log = Logger.getLogger(SearchExecute.class.getName());

    private SearchExecute() {} // class should not be instantiated
    
   /**
	*	This executes a search request from a particular base DN, using
	* 	a particular filter, returning a particular set of attributes.
	*	@param searchTree the search tree.  
	*	@param baseDN the DN that the search will begin from.
	*	@param filter the LDAP search filter to be used.  Reset to '(objectClass=*)' if passed in as null.
	*	@param attribs a list of attributes the search should return (only these attributes, not the whole entry). //TE: TO DO...returning attributes... (should be a String[]).
	*	@param searchLevel either 0 = Search Base Object, 1 = Search Next Level or 2 = Search Full Subtree. 
	*	@param searchBroker the broker that will fire off the search.
	*	.
	*/
    
	public static void run(SmartTree searchTree, DN baseDN, String filter, String[] attribs, int searchLevel, JNDIDataBroker searchBroker)
    {
        if (filter==null)
            filter = "(objectClass=*)";

		switch (searchLevel)	//TE: info messages...
		{
			case 0: { log.info( "search: [ " + baseDN + "] [" + filter + "] [baseObject] "); break; }
			case 1: { log.info( "search: [ " + baseDN + "] [" + filter + "] [singleLevel] "); break; }
			case 2: { log.info( "search: [ " + baseDN + "] [" + filter + "] [wholeSubtree] "); break; }
		}
		
 		searchTree.entry=null;	//TE: so that the last entry is cleared from attribute editor pane.
        searchTree.clearTree();
            
        if ((baseDN.isEmpty()==false) && (baseDN.getRootRDN().equals("")))
            baseDN.remove(0);
                       
        searchBroker.search(baseDN, filter, searchLevel, attribs); // searchBroker is registered to searchTree as its data source.  When search broker gets results, searchTree is automatically updated.  Yes, this is clunky and confusing - CB.
    }
} 