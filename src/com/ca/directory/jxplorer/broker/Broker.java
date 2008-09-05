
package com.ca.directory.jxplorer.broker;

import com.ca.directory.jxplorer.*;

import com.ca.commons.naming.DN;
import com.ca.commons.naming.DXEntry;
import com.ca.commons.naming.DXNamingEnumeration;
import com.ca.commons.jndi.SchemaOps;

import javax.naming.directory.DirContext;
import javax.naming.NamingException;
import java.util.Vector;
import java.util.ArrayList;

public abstract class Broker implements Runnable, DataSource
{
    protected Vector requestQueue = new Vector(10);  // the core list of outstanding queries.
                                                     // also the object that everything synchronizes on...
    protected Vector listeners = new Vector();

    private static int noBrokers = 0;  // for debugging, assign a unique sequential ID to each object created

    public int id;

    protected DataQuery current = null;

    StopMonitor stopMonitor = null;

    public Broker() { id = (noBrokers++); }

    private static boolean debug = false;

    /**
     *    Registers a stop monitor that is used by the gui to allow user
     *    cancellation of in-progress broker actions.
     */

    public void registerStopMonitor(StopMonitor monitor) { stopMonitor = monitor; }

    /**
     *    Adds a DataQuery to the request Queue.
     *    Primarily used by the DataSource methods to
     *    register requests.
     */

    public DataQuery push(DataQuery request)
    {
        for (int i=0; i<listeners.size(); i++)
            request.addDataListener((DataListener)listeners.get(i));

        synchronized(requestQueue)
        {
            requestQueue.add(request);
        }

        if (stopMonitor != null) stopMonitor.updateWatchers();

        synchronized(requestQueue)
        {
            requestQueue.notifyAll();
        }


        return request;  // returns the same request for easy chaining.
    }

    /**
     *    Gets the next DataQuery from the request Queue,
     *    removing it from the queue as it does so.
     */

    public DataQuery pop()
    {
        DataQuery request = null;

        synchronized(requestQueue)
        {
            if (requestQueue.isEmpty()) return null;
            request = (DataQuery)requestQueue.firstElement();
            requestQueue.removeElementAt(0);
            request.setRunning();                // set the running flag (for use by StopMonitor)
        }

        return request;
    }

    /**
     *    Removes a particular query from the pending query list...
     */

     public void removeQuery(DataQuery query)
     {
         synchronized(requestQueue)
         {
             requestQueue.remove(query);
         }
     }

    /**
     *    Returns whether there are more DataQuerys pending.
     */

    public boolean hasRequests()
    {
        synchronized(requestQueue)
        {
            return !requestQueue.isEmpty();
        }
    }

    /**
     *    Wait until notified that something (presumably
     *    an addition to the queue) has occured.  When woken,
     *    process all queue requests, and then go back to
     *    waiting.
     */

    public void run()
    {
        while (true)
        {
if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " processing Queue of length: " +  requestQueue.size() + " in broker " + id);
            if (processQueue()==false)
            {
if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " ending." +  requestQueue.size());
                return;
            }

            try
            {
if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " waiting in run() loop");
                synchronized(requestQueue)
                {
                    requestQueue.wait();
                }
if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " notified in run() loop");
            }
            catch (Exception e)
            {
if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " interrupted in run() loop \n    " + e);
            }
        }
    }

    /**
     *    process all queue requests
     */

    protected boolean processQueue()
    {
        while (hasRequests())
        {
            current = pop();            // keep track of the current query for reporting

            if (current == null) return true;  // sanity check: fantastically small (?) chance of thread magic causing a null return?

            processRequest(current);

            if (stopMonitor != null) stopMonitor.updateWatchers();

            if (current != null && current.isCancelled())    // if the request was cancelled by the user, then
            {                             // it had probably hung, and another broker thread
                return false;             // will have been started - so kill this thread off.
            }
            else
            {
                current = null;
            }
        }

        return true;                      // completed queue without any cancellations
    }

    /**
     *    Process a specific request.
     */

    protected void processRequest(DataQuery request)
    {
if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " process request " + request.id );

        if (request.isCancelled() == true)
        {
            request.finish();
            return;
        }

        try
        {
            if (!isActive())
                request.setException(new Exception("No Data Connection Enabled"));

            if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " process request " + request.id + " of type " + request.getTypeString());

            switch(request.getType())
            {
                case DataQuery.EXISTS:    doExistsQuery(request); break;

                case DataQuery.READENTRY: doEntryQuery(request);  break;

                case DataQuery.LIST:      doListQuery(request);   break;

                case DataQuery.SEARCH:    doSearchQuery(request);  break;

                case DataQuery.MODIFY:    doModifyQuery(request);  break;

                case DataQuery.COPY:      doCopyQuery(request);    break;

//                    case DataQuery.GETALLOC:  doGetAllOCsQuery(request); break;

                case DataQuery.GETRECOC:  doGetRecOCsQuery(request); break;

                case DataQuery.EXTENDED:  doExtendedQuery(request); break;

                case DataQuery.UNKNOWN:

                default:                  throw new NamingException("JX Internal Error: Unknown Data Broker Request type: " + request.getType());
            }
        }
        catch (Exception e)
        {
            request.setException(e);
        }

        // request *should* already be finished by this stage, but just in case...
        request.finish();

    }


    //
    //    DATA SOURCE INTERFACE
    //
    //    (Constructs appropriate DataQuerys and queues them)


    public DataQuery getChildren(DN nodeDN)
    {
        return push(new DataQuery(DataQuery.LIST, nodeDN));
    }

    public DataQuery getEntry(DN nodeDN)
    {
        return push(new DataQuery(DataQuery.READENTRY, nodeDN));
    }

    public DataQuery exists(DN nodeDN)
    {
        return push(new DataQuery(DataQuery.EXISTS, nodeDN));
    }
/*
    public DataQuery getObjectClasses()
    {
        return push(new DataQuery(DataQuery.GETALLOC));
    }
*/
    public DataQuery getRecommendedObjectClasses(DN dn)
    {
        return push(new DataQuery(DataQuery.GETRECOC, dn));
    }

    public DataQuery modifyEntry(DXEntry oldEntry, DXEntry newEntry)
    {
        return push(new DataQuery(DataQuery.MODIFY, oldEntry, newEntry));
    }

    public DataQuery copyTree(DN oldNodeDN, DN newNodeDN)
    {
        return push(new DataQuery(DataQuery.COPY, oldNodeDN, newNodeDN));
    }

    public DataQuery search(DN nodeDN, String filter, int searchLevel, String[] returnAttributes)
    {
        return push(new DataQuery(DataQuery.SEARCH, nodeDN, filter, searchLevel, returnAttributes));
    }

    public DataQuery extendedRequest(DataQuery query)
    {
        return push(query);
    }

    /**
     *  Adds a data listener to every DataQuery generated by this broker.
     *  For threading simplicity, no data listeners are added to DataQuerys
     *  <i>allready</i> in the request queue.<p>
     *  While it is not an error to register a listener multiple times,
     *  a listener will still only be notified once.
     */
    public void addDataListener(DataListener l)
    {
        if (listeners.contains(l) == false)
        {	
            listeners.add(l);
        }
    }
	
	
	
   /**
	*  	Removes a data listener from every DataQuery generated by this broker.
	*   @param l the listener to be notified when the data is ready.	
	*/
	 	
	public void removeDataListener(DataListener l)
	{		
		if (listeners.contains(l)==true)
		{
			listeners.remove(l);
		}
	}



    // Abstract DataSource methods - must be extended

    public abstract boolean isModifiable();

    public abstract DirContext getDirContext();

    public abstract boolean isActive();

    public abstract SchemaOps getSchemaOps();

    /**
     *    Sets the finish flag of a request and returns
     *    the query.  Often overloaded by derived broker classes.
     */

    protected DataQuery finish(DataQuery request)
    {
		if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " request " + request.id + " finished ");
        request.finish();
        return request;
    }




    // Methods for the stop monitor - return a list of outstanding tasks

    /**
     *    Returns the DataQuery currently being processed (if any).
     *    @return the current DataQuery (may be null if there is none).
     */
    public DataQuery getCurrent()   { return current; }

    /**
     *    Returns the vector of outstanding queries.
     *    @return a vector of (DataQuery).
     */
    public synchronized Vector getRequestQueue()
    {
        return requestQueue;
    }

    // this one doesn't need to be abstract...

    protected DataQuery doExtendedQuery(DataQuery request)
            throws NamingException
    {
        request.doExtendedRequest(this);
        return finish(request);
    }


    /**
     *    Method for the Broker interface - chains to
     *    dirOp.exists().
     */

    protected DataQuery doExistsQuery(DataQuery request)
        throws NamingException
    {
        unthreadedExists(request.requestDN());
        request.setStatus(true);
        return finish(request);
    }

    /**
     *    Method for the Broker interface - chains to
     *    list().
     */

    protected DataQuery doListQuery(DataQuery request)
            throws NamingException
    {
        request.setEnum(unthreadedList(request.requestDN()));
        return finish(request);
    }

    /**
     *    Method for the Broker interface - chains to
     *    dirOp.read().
     */

    protected DataQuery doEntryQuery(DataQuery request)
            throws NamingException
    {
        request.setEntry(unthreadedReadEntry(request.requestDN(), null));
        return finish(request);
    }

    /**
     *    Method for the Broker interface - chains to
     *    search().
     */

    protected DataQuery doSearchQuery(DataQuery request)
            throws NamingException
    {
        DXNamingEnumeration en = unthreadedSearch(request.requestDN(), request.filter(), request.searchLevel(), request.returnAttributes());
        request.setEnum(en);
        return finish(request);
    }

    /**
     *    Method for the Broker interface - chains to
     *    modifyEntry().
     */

    protected DataQuery doModifyQuery(DataQuery request)
            throws NamingException
    {
        unthreadedModify(request.oldEntry(), request.newEntry());
        request.setStatus(true);
        return finish(request);
    }

    /**
     *    Method for the Broker interface - chains to
     *    copyTree().
     */

    protected DataQuery doCopyQuery(DataQuery request)
            throws NamingException
    {
        unthreadedCopy(request.oldDN(), request.requestDN());
        request.setStatus(true);
        return finish(request);
    }

    /**
     *    Method for the Broker interface - chains to
     *    unthreadedGetRecOCs.
     */

    protected DataQuery doGetRecOCsQuery(DataQuery request)
            throws NamingException
    {
        request.setArrayList(unthreadedGetRecOCs(request.requestDN()));
        return finish(request);
    }

    /**
     *    Method for the Broker interface - chains to
     *    getObjectClasses().
     */
/*
    protected DataQuery doGetAllOCsQuery(DataQuery request)
    {
        request.setVector(unthreadedGetAllOCs());
        return finish(request);
    }
*/
    /**
     *   returns the next level of a directory tree, returning
     *   a Enumeration of the results
     *
     *   @param searchbase the node in the tree to expand
     *   @return list of results (NameClassPair); the next layer of the tree...
     */

    public abstract DXNamingEnumeration unthreadedList(DN searchbase) throws NamingException;

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

    public abstract DXNamingEnumeration unthreadedSearch(DN dn, String filter, int search_level, String[] returnAttributes) throws NamingException;

   /**
    *    Copies a DN representing a subtree to a new subtree, including
    *    copying all subordinate entries.
    *
    *    @param oldNodeDN the original DN of the sub tree root
    *           to be copied (may be a single entry).
    *    @param newNodeDN the target DN for the tree to be moved to.
    */
    public abstract void unthreadedCopy(DN oldNodeDN, DN newNodeDN) throws NamingException;

    /**
     *    Checks the existance of a given entry.
     */

    public abstract boolean unthreadedExists(DN checkMe) throws NamingException;

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

    public abstract DXEntry unthreadedReadEntry(DN entryDN, String[] returnAttributes) throws NamingException;


   /**
    *    Update an entry with the designated DN.
    *    @param oldEntry oldSet the old set of attributes of the object.
    *    @param newEntry newSet the replacement set of attributes..
    */

    public abstract void unthreadedModify(DXEntry oldEntry, DXEntry newEntry) throws NamingException;

    /**
     *    Gets a list of the object classes most likely
     *    to be used for the next Level of the DN...
     *    @param dn the dn of the parent to determine likely
     *              child object classes for
     *    @return list of recommended object classes...
     */

    public abstract ArrayList unthreadedGetRecOCs(DN dn) throws NamingException;



    /**
     *    Utility method for extended queries - returns whether
     *    a 'masked' exception has occured.
     *    @return the exception, or null if there is none.
     */

    public Exception getException()
    {
        return null;
    }

    /**
     *    Utility method for extended queries - allows
     *    a 'masked' exception to be cleared.
     */

    public void clearException()
    {
    }

        /**
     *    As a way to directly access the directory broker, a DataSource
     *    MAY choose to publish the directory broker.
     *    @return the Broker - may be null.
     */

    public Broker getBroker() { return this; }

}