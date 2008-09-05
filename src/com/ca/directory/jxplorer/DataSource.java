package com.ca.directory.jxplorer;

import com.ca.commons.naming.*;
import com.ca.commons.jndi.SchemaOps;
import com.ca.directory.jxplorer.broker.Broker;

import javax.naming.directory.DirContext;

/**
 This interface defines data sources used by the the
 browser.  Usually this is an ldap data source, but may
 be any JNDI or program defined data source from which
 tree data can be read.

 @see com.ca.directory.jxplorer.broker.JNDIBroker
*/

public interface DataSource
{
    /**
     *    gets the children of a particular DN as an enumeration
     *    @param nodeDN the DN to retrieve children for
     *
     *    @return a DataQuery that may be queried using .getEnumeration() to obtain
     *     an enumeration of NameClassPair-s to be the
     *     sub-nodes of the given node.
     */
    public DataQuery getChildren(DN nodeDN);

    /**
     *    gets the attribute types and values for a particular DN
     *
     *    @param nodeDN the DN to retrieve attribute data for
     *    @return a DataQuery that may be queried using .getEntry() to obtain
     *          a DXEntry object containing attribute objects
     *            corresponding to the name/value(s) of the DN.
     */

    public DataQuery getEntry(DN nodeDN);

   /**
    *    Checks the existence of a particular entry by DN, without (necessarily)
    *    reading any attributes.
    *    @param nodeDN the DN to check the existance of.
    *    @return a DataQuery that may be queried using .getStatus() to obtain
    *        a boolean true if the node exists, or false if it don't.
    */

    public DataQuery exists(DN nodeDN);

    /**
     *    Gets a list of all known schema object classes.
     *    @return all known object classes, as .  This will be
     *            null if this data source does not support
     *            this feature.
     */
    //public DataQuery getObjectClasses();

    /**
     *    Gets a list of the object classes most likely
     *    to be used for the next Level of the DN...
     *    @param dn the dn of the parent to determine likely
     *              child object classes for
     *    @return list of recommended object classes...
     */

    public DataQuery getRecommendedObjectClasses(DN dn);

    /**
     *    Returns a schema context (for getting object classes etc.)
     */

    public SchemaOps getSchemaOps();

    /**
     *    whether the data source is currently on-line.
     *    @return on-line status
     */

    public boolean isActive();




    // OLD DATAMODIFIER INTERFACE

    /**
     *    This changes an old entry to a new entry.
     *    If the distinguished name has changed, the object is moved.
     *    If the oldEntry is null, the object is created, or overwritten.
     *    If the newEntry is null, the object is deleted, along with any
     *    subentries.
     *    @param oldEntry the original entry (may be null if adding a new entry).
     *    @param newEntry the new entry (may be null if deleting the entry).
     *    @return the success status of the operation.
     */

    public DataQuery modifyEntry(DXEntry oldEntry, DXEntry newEntry);


    /**
     *    Copies the entry, and any child entries, identified by
     *    DN oldNodeDn to newNodeDN.
     *    @param oldNodeDN the entry or subtree apexto copy from
     *    @param newNodeDN the entry/subtree apex to copy to
     */


    public DataQuery copyTree(DN oldNodeDN, DN newNodeDN);       // may be a single node.


    /**
     *    Executes a search request.
     *    @param nodeDN the root DN to start searching from
     *    @param filter the ldap string filter for the search
	 *    @param searchLevel whether to search the base object, the next level or the whole subtree.
	 *	  @param returnAttrs an array of attributes that the search should return.
     *           or the entire sub tree.
     */

    public DataQuery search(DN nodeDN, String filter, int searchLevel, String[] returnAttrs);


    /**
     *    Checks whether the current data source is modifiable.  (Nb.,
     *    a directory may have different access controls defined on
     *    different parts of the directory: if this is the case, the
     *    directory may return true to isModifiable(), however a
     *    particular modify attempt may still fail.
     *
     *    @return whether the directory is modifiable
     */

    public boolean isModifiable();

    /**
     *    As a way to directly access the raw jndi directory context, a DataSource
     *    MAY choose to publish the directory connection.
     *    @return the jndi directory context - may be null.
     */

    public DirContext getDirContext();

    /**
     *    As a way to directly access the directory broker, a DataSource
     *    MAY choose to publish the directory broker.
     *    @return the Broker - may be null.
     */

    public Broker getBroker();

    /**
     *    Used by thread-friendly application to register a
     *    listener that will be called whenever a DataQuery request has
     *    been completed (or equivalently, an error thrown).<p>
     *
     *    if this method is called on a DataQuery that has
     *    already been completed, it will be triggered immediately.
     *    This may cause listener code to be triggered <i>before</i>
     *    any code subsequent to the addDataListener() call.<p>
     *
     *    @param l the listener to be notified when the data
     *    operation has been completed.
     */

    public void addDataListener(DataListener l);
	
	
   /**
	*  	Removes a data listener from the DataQuery.
	*   @param l the listener to be notified when the data is ready.
	*/
	
   	public void removeDataListener(DataListener l);	

    /**
     *    General Bail out - this allows the passing of a generic
     *    DataQuery object.  The DataSource will run that object's
     *    'extendedRequest' method.
     *    @return returns the same query it was passed (for compatibility
     *    with similar methods)
     */

    public DataQuery extendedRequest(DataQuery query);
}