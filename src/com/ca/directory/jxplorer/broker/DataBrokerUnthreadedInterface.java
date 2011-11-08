package com.ca.directory.jxplorer.broker;

import com.ca.commons.naming.DN;
import com.ca.commons.naming.DXEntry;
import com.ca.commons.naming.DXNamingEnumeration;

import javax.naming.NamingException;
import java.util.ArrayList;

/**
 * This defines an interface for 'immediate' queries.  These may block while the underlying data resource (e.g. a
 * JDNI directory) resolves.
 */
public interface DataBrokerUnthreadedInterface
{
    /**
        *   returns the next level of a directory tree, returning
        *   a Enumeration of the results
        *
        *   @param searchbase the node in the tree to expand
        *   @return list of results (NameClassPair); the next layer of the tree...
        */

       public DXNamingEnumeration unthreadedList(DN searchbase) throws NamingException;

       /**
        *   Performs a directory search.
        *
        *   @param dn the domain name (relative to initial context in ldap) to seach from.
        *   @param filter the non-null filter to use for the search
        *   @param search_level whether to search the base object, the next level or the whole subtree.
        *   @param returnAttributes a vector of string names of attributes to return in the search.
        *          (null means 'return all entries', a zero length array means 'return no attributes'.)
        *   @return list of results ('SearchResult's); the next layer of the tree...
        */

       public DXNamingEnumeration unthreadedSearch(DN dn, String filter, int search_level, String[] returnAttributes) throws NamingException;

      /**
       *    Copies a DN representing a subtree to a new subtree, including
       *    copying all subordinate entries.
       *
       *    @param oldNodeDN the original DN of the sub tree root
       *           to be copied (may be a single entry).
       *    @param newNodeDN the target DN for the tree to be moved to.
       */
       public void unthreadedCopy(DN oldNodeDN, DN newNodeDN) throws NamingException;

       /**
        * Copies a DN representing a subtree from a different sourceBroker to a new subtree within *this* broker, including
       *    copying all subordinate entries.
        *
        * @param oldNodeDN the original tree root to copy from
        * @param externalDataSource the original data broker to get data from
        * @param newNodeDN the new tree root to copy *underneath*
        * @param updateExistingEntries whether to replace or update (merge) existing entries that may be there. 
        * @throws NamingException
        */
       public void unthreadedCopyBetweenWindows(DN oldNodeDN, DataBrokerUnthreadedInterface externalDataSource, DN newNodeDN, boolean updateExistingEntries)
               throws NamingException;

       /**
        *    Checks the existance of a given entry.
        */

       public boolean unthreadedExists(DN checkMe) throws NamingException;

       /**
        *    Returns a complete list of all known object classes.
        */

       //public abstract Vector unthreadedGetAllOCs();

       /**
        *    Reads an entry with all its attributes from
        *    the directory.
        *    @param entryDN the DN of the object to read.
        *    @param returnAttributes a vector of string names of attributes to return in the search.
        *          (null means 'return all entries', a zero length array means 'return no attributes'.)
        */

       public DXEntry unthreadedReadEntry(DN entryDN, String[] returnAttributes) throws NamingException;


      /**
       *    Update an entry with the designated DN.
       *    @param oldEntry oldSet the old set of attributes of the object.
       *    @param newEntry newSet the replacement set of attributes..
       */

       public void unthreadedModify(DXEntry oldEntry, DXEntry newEntry) throws NamingException;

       /**
        *    Gets a list of the object classes most likely
        *    to be used for the next Level of the DN...
        *    @param dn the dn of the parent to determine likely
        *              child object classes for
        *    @return list of recommended object classes...
        */

       public ArrayList unthreadedGetRecOCs(DN dn) throws NamingException;


}
