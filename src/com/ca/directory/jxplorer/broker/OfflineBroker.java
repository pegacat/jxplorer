
package com.ca.directory.jxplorer.broker;

import javax.naming.*;
import javax.naming.directory.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.ca.directory.jxplorer.*;
import com.ca.commons.naming.*;
import com.ca.commons.jndi.SchemaOps;

import java.awt.Component;

/** 
 * 	    This sets up a 'virtual broker' that reads info in, and allows
 *      the user to operate on it, without any server being involved.  The
 *      data is usually (always?) read from an ldif file.
 */

public class OfflineBroker extends Broker
{
    Hashtable nodes;  // stored node dn keys (as strings), and nodes
    Component display;

    private static Logger log = Logger.getLogger(OfflineBroker.class.getName());

    /**
     *    Data node class.  Tuned for 10ish children.
     *
     */
    
    class Node
    {
        DXEntry entry;
        
        //DXAttributes myAtts;
        Vector children;
        DN dn;
        NameClassPair namePair;

        /** 
         *    constructor for a node with a dn and a set of attribute
         */        
        public Node(DXEntry entry) 
        {
            this.entry = entry;
            dn = entry.getDN();
            children = new Vector(10);
            namePair = new NameClassPair(dn.getLowestRDN().toString(), dn.getLowestRDN().getAtt());    
        }
        
        public void addChild(Node n) { children.add(n); }
        
        public void removeChild(Node n) { children.remove(n); }
        
        public void updateAttributes(DXAttributes a) 
        { 
            try
            {
                NamingEnumeration attset = a.getAll();
                while (attset.hasMore())
                    entry.put((Attribute)attset.next()); 
            }   
            catch (NamingException e)
            {
                log.log(Level.WARNING, "unusual error in OfflineBroker::updateAttributes", e);
            }
        }
        
        /** 
         *    Returns enumeration of Children as NameClassPairs
         */
         
        public DXNamingEnumeration getChildren()
        {
            DXNamingEnumeration result = new DXNamingEnumeration();
            for (int i=0; i<children.size(); i++)
                result.add(((Node)children.elementAt(i)).getNameClassPair());
            return result;                            
        }
     
        /** 
         *    Returns enumeration of children as Nodes
         */
         
        public NamingEnumeration getChildNodes()
        {
            DXNamingEnumeration result = new DXNamingEnumeration();
            for (int i=0; i<children.size(); i++)
                result.add(children.elementAt(i));
            return result;                            
        }

        /** 
         *    Gets a NameClassPair for the present node, i.e.
         *    something like NameClassPair(cn=Fred Bloggs, cn)
         */     
        public NameClassPair getNameClassPair() { return namePair; }         
        
        /**
         *    Get Attributes for current Node.
         */
        public DXEntry getEntry() { return entry; }
        
        /** 
         *    Gets the full dn for the current node.
         */
        public DN getDN() { return dn; }
        
        /**
         *    Looks up the parent in the big nodes Hashtable.
         *    @return Returns null if the parent isn't found,
         *            returns parent node otherwise.
         */
         
        public Node getParent()
        {
            DN parentDN = dn.parentDN();
            if (parentDN == null) return null;
            Node Parent = (Node)nodes.get(parentDN.toString());
            return Parent;
        }
        
        public String toString() { return dn.toString(); }
    }

    /**
     *    Constructor for Offline Broker does nothing except 
     *    initialise the big hashtable that is at the core
     *    of the class.
     */
    public OfflineBroker(Component graphicsDisplay)
    {
        display = graphicsDisplay;
        nodes = new Hashtable(1000);
    }

    /**
     *    Empties the core Hashtable in preparation for a new
     *    ldif file to be read in.
     */
     
     public void clear()
     {
         nodes.clear();   
     }

    /**
     *    gets the children of a particular DN as an enumeration
     *    @param nodeDN the DN to retrieve children for
     *
     *    @return an enumeration of NameClassPair-s to be the
     *     sub-nodes of the given node.  returns null if nodeDN
     *     not found at all.
     */
     
     public DXNamingEnumeration children(DN nodeDN)
     {
         Node N = (Node)nodes.get(nodeDN.toString());
         return (N==null)? null : N.getChildren() ;
     }
    

    /**
     *    whether the data source is currently on-line.
     *    @return on-line status
     */  
     
     public boolean isActive() { return true; }

    /**
     * Whether there are any nodes in our offline broker (effectively, if the ldif file read was successful)
     * @return
     */
     public boolean hasData() { return !nodes.isEmpty(); }  

    /**
     *    Gets a list of all known schemaOps object classes.
     *    @return all known object classes, as .  This will be
     *            null if this data source does not support
     *            this feature.
     */
    public Vector objectClasses() { return null; }
    
    /**
     *    Gets a list of the object classes most likely
     *    to be used for the next Level of the DN...
     *    @param dn the dn of the parent to determine likely
     *              child object classes for
     *    @return list of recommended object classes...             
     */
     
     public Vector recommendedObjectClasses(DN dn) { return null; }
     
     /**
      *    Returns the context for the schemaOps.  This
      *    may be null if the directory does not support
      *    schemaOps.
      *    @return null - not implemented.
      */
      
     public SchemaOps getSchemaOps() { return null; }
     
   /**
    *    Make a new entry with the provided DN.
    *    @param entry the DN and attributes of the new object.
    *    @return the operation's success status
    */
    
    protected boolean addNode(DXEntry entry)
    {
        DN nodeDN = entry.getDN();
        
        log.fine("adding node " + nodeDN);
        
        Node N = new Node(entry);
        nodes.put(nodeDN.toString(), N);
        Node P = N.getParent();
        
        if (P==null)
        {
            if (nodeDN.size()>1)  // so there *should* be a parent!
            {
                addNode(new DXEntry(new DXAttributes(new DXAttribute("structuralTreeNode", "true")), nodeDN.parentDN()));  // add a 'fake' node to pad the tree out...
                P = (Node)nodes.get(nodeDN.parentDN().toString());    
            }   
        }
        log.fine("parent = " + ((P==null)?"null":P.toString()));
        if (P != null) P.addChild(N);
        return true;      // not very discriminating
    }
    
   /**
    *    Update an entry with the designated DN.
    *    @param oldSet the old set of attributes of the object.
    *    @param newSet the replacement set of attributes..
    *    @return the operation's success status
    */
   
    public boolean updateNode(DXEntry oldSet, DXEntry newSet)
    {
        log.fine("offline cache updating " + oldSet.getDN().toString());
        Node N = (Node)nodes.get(oldSet.getDN().toString());
        if (N==null) return false;
        N.updateAttributes(newSet);
        return true;    
    }
    
   /**
    *    deletes an entry with the given DN.  If the entry has subordinates,
    *    they are also deleted.
    *
    *    @param nodeDN the DN of the tree root to delete (may be a single entry).
    *    @return the operation's success status
    */
    
    public boolean deleteTree(DN nodeDN)                      // may be a single node.
    {
        log.fine("offline cache deleting " + nodeDN.toString());
        Node N = (Node)nodes.get(nodeDN.toString());
        
        if (N==null) return false;
        
        Enumeration children = N.getChildNodes();
        while (children.hasMoreElements())
            deleteTree(((Node)children.nextElement()).getDN());
            
        nodes.remove(nodeDN.toString());
        Node parent = N.getParent();                 // this node tells its parent to remove this        
        if (parent != null) parent.removeChild(N);  // node from the parent's child list...
        return true;    
    }
    
   /**
    *    Moves a DN to a new DN, including all subordinate entries.
    *    (nb it is up to the implementer how this is done; e.g. if it is an
    *     ldap broker, it may choose rename, or copy-and-delete, as appropriate)
    *     
    *    @param oldNodeDN the original DN of the sub tree root (may be a single
    *           entry).
    *    @param newNodeDN the target DN for the tree to be moved to.
    */
    
    public void move(DN oldNodeDN, DN newNodeDN)       // may be a single node.
        throws NamingException
    {
        unthreadedCopy(oldNodeDN, newNodeDN);            // brutal...
        deleteTree(oldNodeDN);                           // ... but it works.
    }
    

    
    
   /**
    *    Checks whether the current data source is modifiable.  (Nb., 
    *    a directory may have different access controls defined on 
    *    different parts of the directory: if this is the case, the
    *    directory may return true to isModifiable(), however a 
    *    particular modify attempt may still fail.
    *
    *    @return whether the directory is modifiable
    */
    
    public boolean isModifiable() { return true; }
    


    /**
     *    We don't actually have an underlying DirContext, so return null...
     */
     
     public DirContext getDirContext() { return null; }
     
     
     
    /**
     *    Method for the Broker interface - chains to 
     *    children().
     */
     
    public DataQuery doListQuery(DataQuery request)
    {
        request.setEnum(children(request.requestDN()));
        return request;
    }
                
    /**
     *    Method for the Broker interface - chains to 
     *    search().
     */
     
    public DataQuery doSearchQuery(DataQuery request)
    {
        request.setException(new Exception("offline searches not allowed"));
        return request;
    }
                
                
    /**
     *    Method for the Broker interface - chains to 
     *    getObjectClasses().
     */
     
    public DataQuery doGetAllOCsQuery(DataQuery request)
    {
        request.setException(new Exception("offline object class list not implemented"));
        return request;
    }
                
    /**
     *    Method for the Broker interface - chains to 
     *    getRecommendedObjectClasses.
     */
     
    public DataQuery doGetRecOCsQuery(DataQuery request)
    {
        request.setException(new Exception("offline object class list not implemented"));
        return request;
    }  
    
    /**
     *   returns the next level of a directory tree, returning
     *   a Enumeration of the results
     *
     *   @param searchbase the node in the tree to expand
     *   @return list of results (NameClassPair); the next layer of the tree... 
     */
     
    public  DXNamingEnumeration unthreadedList(DN searchbase)
    {
        return children(searchbase);
    }
        
    /**
     *   Not Implemented.
     *
     *   @param dn the distinguished name (relative to initial context in ldap) to seach from.
     *   @param filter the non-null filter to use for the search 
     *   @param search_level whether to search the base object, the next level or the whole subtree.
     *   @param returnAttributes a vector of string names of attributes to return in the search.  (Currently inoperative)
     *   @return list of results ('SearchResult's); the next layer of the tree... 
     */
     
    public  DXNamingEnumeration unthreadedSearch(DN dn, String filter, int search_level, String[] returnAttributes)
    {
        return null; 
    }
    
   /**
    *    Copies a DN representing a subtree to a new subtree, including 
    *    copying all subordinate entries.
    *     
    *    @param oldNodeDN the original DN of the sub tree root 
    *           to be copied (may be a single entry).
    *    @param newNodeDN the target DN for the tree to be moved to.
    */
    
    public  void unthreadedCopy(DN oldNodeDN, DN newNodeDN)
        throws NamingException
    {
        if (oldNodeDN == null)
            throw new NamingException("null old dn passed to unthreadedCopy() in OfflineBroker");                              // sanity check


        Node Old = (Node)nodes.get(oldNodeDN.toString());         // get the Node to copy
        if (Old==null)
            throw new NamingException("null old Node found to unthreadedCopy() in OfflineBroker");                              // sanity check

        DXEntry newEntry = new DXEntry(Old.getEntry(), newNodeDN);
        // TODO: stick in 'copy Entry' naming check, to make sure that the root node has the right naming attribute
        // TODO: (cf: AdvancedOps.copyEntryResettingNamingAttribute)
        addNode(newEntry);          // create a copy of the Old node...
        Node New = (Node)nodes.get(newNodeDN.toString());         // get the newly created copy  
        Enumeration children = Old.getChildNodes();               // get the old nodes children...
        while (children.hasMoreElements())                        // ...and for each child
        {
            Node child = (Node)children.nextElement();            // identify the child node
            DN NewChildDN = new DN(New.getDN());                  // get the 'New' nodes DN...
            NewChildDN.addChildRDN(child.getDN().getLowestRDN().toString());  // ... and add to it the child rdn that is being copied
            unthreadedCopy(child.getDN(),NewChildDN);                   // copy (and therefore create) the child node copy
        }
    }




   /**
    *    Checks the existence of a particular DN, without (necessarily)
    *    reading any attributes.
    *    @param nodeDN the DN to check.
    *    @return the existence of the nodeDN (or false if an error occurs).
    */
    
    public boolean unthreadedExists(DN nodeDN)   
    {   
        return nodes.containsKey(nodeDN.toString());
    }
    
    /**
     *    Not implemented.
     */

    public  Vector unthreadedGetAllOCs() { return null; }

    /**
     *    Reads an entry with all its attributes from
     *    the directory.
     *    @param entryDN the DN of the object to read.
     *    @param returnAttributes a vector of string names of attributes to return in the search.
     *          (null means 'return all entries', a zero length array means 'return no attributes'.)
     */
     
    public DXEntry unthreadedReadEntry(DN entryDN, String[] returnAttributes)
    {
        if (returnAttributes != null)
            log.info("warning: att list read entries not implemented in offline broker");
            
        Node N = (Node)nodes.get(entryDN.toString());
        return (N==null)?  new DXEntry(entryDN) : N.getEntry();
    }  
        
        
   /**
    *    Update an entry with the designated DN.
    *    @param oldEntry the old set of attributes of the object.
    *    @param newEntry the replacement set of attributes..
    */
    
    public  void unthreadedModify(DXEntry oldEntry, DXEntry newEntry)
        throws NamingException
    {
        if (oldEntry == null && newEntry == null)
        {
             // nothing to do.
        }
        else if (oldEntry == null) // add
        {
            addNode(newEntry);
        }
        else if (newEntry == null) // delete
        {
            deleteTree(oldEntry.getDN());
        }
        else
        {

            // remove naming attributes to avoid name/dn conflicts...
            // XXX work to do here supporting multi-valued naming atts...

            RDN singleValRDN = oldEntry.getDN().getLowestRDN();
            String namingAtt = singleValRDN.getAtt();
            String namingVal = singleValRDN.getRawVal();
            oldEntry.remove(namingAtt);
            newEntry.remove(namingAtt);
            newEntry.put(new DXAttribute(namingAtt, namingVal));

            if (oldEntry.getDN().equals(newEntry.getDN()) == false)
            {
                move(oldEntry.getDN(), newEntry.getDN());
                oldEntry.putDN(newEntry.getDN());
            }

            // check for change of attributes done in modify()

            updateNode(oldEntry, newEntry);
        }
    }

    /**
     *    Not implemented.
     *    @param dn the dn of the parent to determine likely
     *              child object classes for
     *    @return list of recommended object classes...             
     */
    
    public  ArrayList unthreadedGetRecOCs(DN dn) { return null; }
     
}