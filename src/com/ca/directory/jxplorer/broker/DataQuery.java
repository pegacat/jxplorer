package com.ca.directory.jxplorer.broker;

import com.ca.directory.jxplorer.DataListener;
import com.ca.directory.jxplorer.broker.DataBroker;
import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.broker.DataBrokerQueryInterface;

import java.util.Hashtable;
import java.util.Vector;
import java.util.ArrayList;
import javax.naming.NamingException;

/**
 *    Helper class for DataBroker, this encapsulates an ldap-like data
 *    request that is placed on the DataBroker queue for eventual resolution.
 *    DataBroker Request objects are intended to be used once, and then
 *    discarded.
 */

public class DataQuery
{
    /**
     *    The following gives the 'type' of the query.
     */
    // could use OO for this, but seems unnecessary; would only create class sprawl for
    // so many similar classes. - CB

    // note - use of bit mask-like patterns implies you can have multiple of these at the same time, but you can't... not sure
            // why we chose to do this; change to using an enum?
    public static final int UNKNOWN = 0;     /** an unknown type **/
    public static final int EXISTS = 1;      /** an existance check query **/
    public static final int READENTRY = 2;   /** a read of a single entry **/
    public static final int LIST = 4;        /** list the immediate children of an entry **/
    public static final int SEARCH = 8;      /** a search query **/
    public static final int MODIFY = 16;     /** a modify request **/
    public static final int COPY = 32;       /** a copy request **/
    public static final int XWINCOPY = 64;   /** copy across windows **/
    public static final int GETRECOC = 128;  /** get recommended object classes request **/
    public static final int EXTENDED = 256;  /** an extended data query: i.e. a user inherited extension class **/

    protected Vector listeners = new Vector();

    protected int type = 0;

    static int noRequests = 0;

    public int id;  // debugging

    protected boolean myStatus = false;

    protected ArrayList myArrayList = null;

    protected DXEntry myEntry = null;

    protected DXNamingEnumeration myEnum = null;

    protected Exception myEx = null;



    // The following data variables are set initially in the
    // constructor, and never again modified.

    protected DN requestDN = null;
    protected DN oldDN = null;

    protected DXEntry oldEntry = null;
    protected DXEntry newEntry = null;

    protected String filter = null;
	
	protected String[] returnAttrs = null;

    protected int searchLevel = 2;	//TE: 0 = search base object, 1 = search next level, 2 = seach full subtree.

    protected Hashtable extendedData = null;  // convenience object for extended queries


    // These booleans relate to which stage of the query life cycle the
    // query is at - at the start, they are all false.  When the query
    // is being executed 'working' is true.  Finally 'ready' is true.
    // If the user cancels the query, the broker should pick this up
    // and ignore the query.

    protected boolean ready = false;        // whether the query if completed

    protected boolean cancelled = false;  // whether the user has cancelled this query.

    protected boolean working = false;    // whether this request is currently being run by a broker thread

    protected boolean squelched = false;    // whether this request has been consumed by a selfish listener

    // For cross-window operations only; a source directory to use for copy / move operations across windos
    protected DataBrokerUnthreadedInterface externalDataSource = null;

    // For cross-window operations only; whether to overwrite existing entries when doing a copy / move operation across windows
    protected boolean overwriteExistingData = true;
    
    // XXX Break into sub-classes?

    public DataQuery()
    {
        id = noRequests++;
    }

    /**
     *    Constructor for Get all Object Class requests
     */

    public DataQuery(int type)
    {
        this.type = type;

        if (type == EXTENDED)
            extendedData = new Hashtable();

        id = noRequests++;
    }

    public DN getRequestDN()
    {
        return requestDN;
    }

    /**
     *    Constructor for List, Read Entry, Exists, and get Recommended Object Class requests.
     */

    public DataQuery(int type, DN dn)
    {
        this(type);
        requestDN = dn;
        if (type != DataQuery.LIST &&  type != DataQuery.READENTRY && type != DataQuery.EXISTS )// && type != DataQuery.GETRECOC)
            setException(new Exception("Bad Constructor call (ii) for DataQuery of Type " + type));
    }

    /**
     *    Constructor for Copy requests
     */

    public DataQuery(int type, DN oldDN, DN newDN)
    {
        this(type);
        requestDN = newDN;
        this.oldDN = oldDN;
        if (type != DataQuery.COPY)
            setException(new Exception("Bad Constructor call (iii) for DataQuery of Type " + type));
    }




    /**
     *    Constructor for Modify requests
     */

    public DataQuery(int type, DXEntry oldEntry, DXEntry newEntry)
    {
        this(type);
        this.oldEntry = oldEntry;
        this.newEntry = newEntry;
        if (type != DataQuery.MODIFY)
            setException(new Exception("Bad Constructor call (iv) for DataQuery of Type " + type));
    }

    /**
     *    Constructor for Copy requests between windows
     *
     * @param type
     * @param sourceDN
     * @param newDN
     * @param externalDataSource
     * @param overwriteExistingData
     */
    public DataQuery(int type, DN sourceDN, DN newDN, DataBrokerUnthreadedInterface externalDataSource, boolean overwriteExistingData)
    {
        this(type);
        requestDN = newDN;
        this.oldDN = sourceDN;
        this.externalDataSource = externalDataSource;
        this.overwriteExistingData = overwriteExistingData;
        if (type != DataQuery.XWINCOPY)     
            setException(new Exception("Bad Constructor call (iii) for DataQuery of Type " + type));
    }
    
    /**
     *    Constructor for Delete requests between windows
     */

    public DataQuery(int type, DXEntry sourceEntry, DataBrokerUnthreadedInterface externalDataSource)
    {
        this(type);
        this.oldEntry = sourceEntry;
        this.externalDataSource = externalDataSource;
        if (type != DataQuery.MODIFY)
            setException(new Exception("Bad Constructor call (iv) for DataQuery of Type " + type));
    }



    /**
     *    Constructor for Search requests
     */	

    public DataQuery(int type, DN dn, String filter, int level, String[] returnAttrs)
    {
        this(type);
        requestDN = dn;
        this.filter = filter;
		this.returnAttrs = returnAttrs;
        searchLevel = level;
        if (type != DataQuery.SEARCH)
            setException(new Exception("Bad Constructor call (v) for DataQuery of Type " + type));
    }

    /**
     *    Prevents any other data listeners from using the data query.  Indicates that
     *    the current data listener has handled the data, and that no other data listener
     *    should bother.  This method nullifies any data (including exception information).
     */

    public void squelch()
    {
        squelched = true;

        myArrayList = null;
        myEntry = null;
        myEnum = null;
        myEx = null;
        requestDN = null;
        oldDN = null;
        oldEntry = null;
        newEntry = null;
        filter = null;
    }


    /**
    *    Flags whether a data listener has already consumed this data query ('event'), and
    *    the data has been 'used up'.
    */

    public boolean isSquelched() { return squelched; }

    /**
     *    Attempt to cancel this request by setting a cancelled
     *    flag and an exception.  There is no way to 'uncancel' a
     *    request.  The cancel flag should be picked up by the broker.
     */

    public void cancel()
    {
        cancelled = true;
        setException(new Exception("Request Cancelled"));
    }

    /**
     *    Returns whether this request has been cancelled.
     */

    public boolean isCancelled()
    {
        return cancelled;
    }

    /**
     *    Registers that this query is currently being operated on by a broker.
     *    This is cancelled when the query is 'finished()'.
     */

     public void setRunning()
     {
         working = true;
     }

     /**
      *    Returns whether the query is currently being operated on by a broker.
      */

     public boolean isRunning()
     {
         return working;
     }



    /**
     *    Called when the operation is complete.  While it
     *    is not an error to call this method multiple times,
     *    subsequent calls will have no effect.
     */

    public synchronized void finish()
    {
        if (ready == true) return; // only run this method once!

        ready = true;
        if (!cancelled)
        {
            notifyAll();
            fireDataEvent();
        }
        working = false;
    }

    /**
     *    Notifies all interested listeners.  Note that the most
     *    recently added listener is notified first, giving it
     *    first option on handling any errors generated.
     */

    protected void fireDataEvent()
    {
        while(listeners.size() > 0 && !squelched)    // while there are still listeners, and this hasn't been squelched.
        {
            DataListener current = (DataListener)listeners.lastElement();
            listeners.removeElementAt(listeners.size()-1);
            current.dataReady(this);
        }
    }

    /**
     *    Utility translation method
     */

    public String getTypeString()
    {
        switch (type)
        {
            case UNKNOWN:   return "Unknown";
            case EXISTS:    return "Existance check";
            case READENTRY: return "Read entry";
            case LIST:      return "List";
            case SEARCH:    return "Search";
            case MODIFY:    return "Modify";
            case COPY:      return "Copy";
            case XWINCOPY:  return "Cross Windows Copy";
//            case GETALLOC:  return "Get all objectclasses from schema";
//            case GETRECOC:  return "Get recommended objectclasses from schema";
            case EXTENDED:  return "Extended request";
        }
        return "UNDEFINED";
    }

    /**
     *    Return a string representation of the DataBroker Request (usually for debugging).
     */

    public String toString()
    {
        String ret = "query ("+id+"): "  + getTypeString();

        if (type == SEARCH)
            ret += " filter: " + filter;
        else if (requestDN != null)
            ret += " on " + requestDN;

        if (ready() == false)
            return (ret + " : (request pending)");
        else
            return (ret + " : (completed)");

/*        {

            Object o = getResult();
            if (o == null)
                return (ret + " : (null)");
            else
                return (ret + " : " + getResult().toString());
        }
*/
    }

    /**
     *    Get the result of the request as a generic object (which must be cast
     *    correctly to be used).  (Usually for debugging).
     */

    public Object getResult()
    {
        try
        {
            switch (type)
            {
                case UNKNOWN:   return "UNKNOWN";
                case EXISTS:    return new Boolean(getStatus());
                case READENTRY: return getEntry();
                case LIST:      return getEnumeration();
                case SEARCH:    return getEnumeration();
                case MODIFY:    return new Boolean(getStatus());
                case COPY:      return new Boolean(getStatus());
//                case GETALLOC:  return getObjectClasses();
//                case GETRECOC:  return getObjectClasses();
                case EXTENDED:  return getAllExtendedData();
            }
            return "UNDEFINED";
        }
        catch (NamingException e)
        {
            return e;
        }
    }

    /** Unsynchronized Methods to set data variables **/

    public DataQuery setArrayList(ArrayList v) { myArrayList = v; return this;}

    public DataQuery setStatus(boolean b) { myStatus = b; return this;}

    public DataQuery setEntry(DXEntry d) { myEntry = d; return this;}

    public DataQuery setEnumeration(DXNamingEnumeration d)
    {
        if (type != LIST && type != SEARCH && type != XWINCOPY)
            setException(new NamingException("call of setEnum not allowed for a DataQuery of Type " + getTypeString()));

        myEnum = d;
        return this;
    }

    public DataQuery setException(Exception e) { myEx = e; return this;}

    //public void setType(int t) { type = t; }


    /** DataQuery Interface **/

    /**
     *    Used by some thread-friendly applications that wish to
     *    poll the state of the DataResult.  Returns false
     *    until the data request has been completed (which
     *    includes an error occurring).
     *    @return true if the request has been completed.
     */
    public boolean ready() { return ready; }

    /**
     *    Used by thread-friendly application to register a
     *    listener that will be called when the request has
     *    been completed (or an error thrown).<p>
     *
     *    Note that this listener is somewhat unusual, as it
     *    will only ever trigger a maximum of one time.<p>
     *
     *    if this method is called on a DataResult that has
     *    already been completed, it will be triggered immediately.
     *    This may cause listener code to be triggered <i>before</i>
     *    any code subsequent to the addDataListener() call.<p>
     *
     *    @param l the listener to be notified when the data
     *    operation has been completed.
     */

    public synchronized void addDataListener(DataListener l)
    {
        if (ready)
            l.dataReady(this);  // if data ready, fire immediately
        else
            listeners.add(l);   //  otherwise add to listener list.
    }


    synchronized void keepWaiting()
        throws NamingException
    {
        // XXX BIG UGLY HACK
        // We need to make sure that thread is not waiting for itself.  This can happen if a pluggable editor
        // (run from jndiThread) tries to get the result from a DataQuery immediately.  Currently this is done
        // by checking the thread name string (yuck).  Maybe it would be better to do away with the facility
        // altogether, and disallow DataQueries to return anything unless they are 'ready()'.

        String current = Thread.currentThread().getName();

        if ("jndiBroker Thread searchBroker Thread schemaBroker Thread".indexOf(current)>-1)
        {
            System.err.println("ERROR - Thread " + current + " possibly blocking on self");
            throw new NamingException("Thread Blockage (?) during Naming operation - attempt to force immediate data read from: " + current + " thread.  Consider using unthreaded DataBroker classes rather than DataQuery.");
        }

        try
        {
            notifyAll();
            wait();
        } catch (InterruptedException e)
        {
            System.out.println("Thread: " + Thread.currentThread().getName() + " interrupted in DataQuery:keepWaiting() loop \n    " + e);
        };  // block until data is cooked
    }

    /**
     *    Returns the status of an operation.  This will
     *    block until the transaction has finished, so thread
     *    friendly applications should wait until the
     *    DataResult is 'ready' before calling this method.<p>
     *
     *    This method should be used to read the response from
     *    DataBrokerQueryInterface methods modifyEntry() and copyTree(),
     *    and DataBrokerQueryInterface operations such as exists() and isActive().
     *
     *    Throws a NamingException if called inapropriately
     *    (i.e. if no result of this type would be possible).
     *
     *    @return the success status of the operation.
     */

    public boolean getStatus()
        throws NamingException
    {
        while (!ready()) keepWaiting();

        if (type == MODIFY || type == COPY || type == EXISTS)
            return myStatus;
        else
            throw new NamingException("Improper call of getStatus for a DataQuery of Type " + getTypeString());
    }
    /**
     *    A list of Name-Class Pairs from a 'getChildren()'
     *    DataBrokerQueryInterface operation.  This will
     *    block until the transaction has finished, so thread
     *    friendly applications should wait until the
     *    DataResult is 'ready' before calling this method.<p>
     *
     *    Throws a NamingException if called inapropriately
     *    (i.e. if no result of this type would be possible).
     *
     *    This method should be used to read the response from
     *    DataBrokerQueryInterface getChildren() method.
     *    @return the NameClass pairs representing the children
     *    of a given entry, or of SearchResults representing the
     *    result of a search request.
     */
    public DXNamingEnumeration getEnumeration()
        throws NamingException
    {
        try
        {
	        while (!ready()) keepWaiting();

	        if (type == LIST || type == SEARCH || type == XWINCOPY)
	            return myEnum;
	        else
	            throw new NamingException("Improper call of getNameClassPairs for a DataQuery of Type " + getTypeString());

        }
        catch (NamingException e)
        {
            throw new NamingException(e.toString());
        }
    }

    /**
     *    A DXEntry from a 'getEntry()'
     *    DataBrokerQueryInterface operation.  This will
     *    block until the transaction has finished, so thread
     *    friendly applications should wait until the
     *    DataResult is 'ready' before calling this method.<p>
     *
     *    Throws a NamingException if called inapropriately
     *    (i.e. if no result of this type would be possible).
     *
     *    This method should be used to read the response from
     *    DataBrokerQueryInterface getEntry() method.
     */
    public DXEntry getEntry()
        throws NamingException
    {
        while (!ready()) keepWaiting();

        if (type == READENTRY)
            return myEntry;
        else
            throw new NamingException("Improper call of getEntry for a DataQuery of Type " + getTypeString());
    }
    /**
     *    A vector of strings representing ObjectClasses from either a
     *    DataBrokerQueryInterface getObjectClasses() or a getRecommendedObjectClasses()
     *    operation.  This will
     *    block until the transaction has finished, so thread
     *    friendly applications should wait until the
     *    DataResult is 'ready' before calling this method.<p>
     *
     *    Throws a NamingException if called inapropriately
     *    (i.e. if no result of this type would be possible).
     *
     *    This method should be used to read the response from
     *    DataBrokerQueryInterface getObjectClasses() or a getRecommendedObjectClasses() method.
     */

    public ArrayList getObjectClasses()
        throws NamingException
    {
        while (!ready()) keepWaiting();

//        if (type == GETALLOC || type == GETRECOC)
        if (type == GETRECOC)
            return myArrayList;
        else
            throw new NamingException("Improper call of getObjectClasses for a DataQuery of Type " + getTypeString());
    }

    /**
     *    Indicates that an error has occured.  This will
     *    block until the transaction has finished, so thread
     *    friendly applications should wait until the
     *    DataResult is 'ready' before calling this method.<p>
     *    It returns null if there is no exception.<p>
     *    @return the exception experienced by the directory
     *     operation, or null if no error found.
     */

    public Exception getException()
    {
        try
        {
            while (!ready()) keepWaiting();
        }
        catch (NamingException e) { return e; }

        return myEx;        // which is null, unless an exception occurred...
    }

    /**
     *    Returns whether an error has occured.  This will
     *    block until the transaction has finished, so thread
     *    friendly applications should wait until the
     *    DataResult is 'ready' before calling this method.<p>
     *    It returns false if there is no exception.<p>
     *    @return whether an exception occured in the execution of
     *    this query
     */

    public boolean hasException()
    {
        try
        {
            while (!ready()) keepWaiting();
        }
        catch (NamingException e)
        {
            myEx = e;
            return true;
        }

        return (myEx!=null);
    }


    /**
     *    This clears the exception status of a DataResult,
     *    indicating that the exception has been handled.
     *    It is not an error to call this method if no
     *    exception exists, and this method has no effect
     *    on exceptions that may occur later (e.g. if this
     *    method is called before the DataResult is ready()
     *    it may have no effect.)
     */
    public void clearException()
    {
        myEx = null;
    }



    public int getType() { return type; }



    // Read-only access methods to data variables.

    public DN requestDN() { return (requestDN==null)?null:new DN(requestDN); }

    public DN oldDN() { return (oldDN==null)?null:new DN(oldDN); }

    public DXEntry oldEntry() { return (oldEntry==null)?null:new DXEntry(oldEntry); }

    public DXEntry newEntry() { return (newEntry==null)?null:new DXEntry(newEntry); }

    public String filter() { return (filter==null)?null:filter; }
	
	public String[] returnAttributes() { return (returnAttrs==null)?null:returnAttrs; }

    public int searchLevel() { return searchLevel; }

    public DataBrokerUnthreadedInterface getExternalDataSource() { return externalDataSource; }

    public boolean overwriteExistingData() { return overwriteExistingData; }

    /**
     *    This provides a method of returning arbitrary information from
     *    an extended request.
     *    @return a hashtable of named objects (use addExtendedData to set these).
     */

    public Hashtable getAllExtendedData() { return extendedData; }

    /**
     *    Returns a named data object from the extended data queries object store.
     *    @param name case insensitive name of a previously added object.
     */

    public Object getExtendedData(String name) { return extendedData.get(name.toLowerCase()); }

    /**
     *    This provides a method for setting arbitrary information for
     *    the use of an extended request.
     */

    public void setExtendedData(String name, Object o) { if (extendedData != null) extendedData.put(name.toLowerCase(),o); }

    /**
     *    Users intending to make Extended requests should write (potentially anonymous)
     *    derived classes of DataQuery, extending this method.  Derived class writers
     *    should be carefull to only use the *unthreaded* methods of the DataBroker, in
     *    order to avoid possible thread problems.
     */

    public void doExtendedRequest(DataBroker b) { return; }

}


