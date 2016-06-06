package com.ca.directory.jxplorer.broker;

import com.ca.commons.cbutil.CBIntText;
import com.ca.commons.cbutil.CBUtility;
import com.ca.commons.cbutil.CBpbar;
import com.ca.commons.jndi.SchemaOps;
import com.ca.commons.naming.*;

import com.ca.directory.jxplorer.DataListener;
import com.ca.directory.jxplorer.SwingDataListener;

import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.ldap.LdapContext;
import java.util.ArrayList;
import java.util.Vector;


/*
    this seems to be gettimg a bit feral...


    a) smartPopupTool registers a request to do something
    b) calls the approprate SmartTree method
    c) calls the TreeDataSource method...
    d) ... which is then picked up by an implementing class such as JNDIDataBroker, which extends DataBroker which implements DataBrokerQueryInterface
    e) which creates a DataQuery object and tosses it into the DataQuery queue
    f) ... which is picked up by the base DataBroker class 'processQueue()' method, which delegates back to an 'unthreaded...' method
    g) run by the implementing class (e.g. JNDIDataBroker).

    TODO: Possibly simplify the above?  partially by renaming, partially by cutting out middle-men?

 */

public abstract class DataBroker implements Runnable, DataBrokerQueryInterface, DataBrokerUnthreadedInterface
{
    protected Vector requestQueue = new Vector(10);  // the core list of outstanding queries.
    // also the object that everything synchronizes on...
    protected Vector listeners = new Vector();

    private static int noBrokers = 0;  // for debugging, assign a unique sequential ID to each object created

    public int id;

    protected DataQuery current = null;

    StopMonitor stopMonitor = null;

    public DataBroker() { id = (noBrokers++); }

    private static boolean debug = false;

    /**
     * Registers a stop monitor that is used by the gui to allow user
     * cancellation of in-progress broker actions.
     */

    public void registerStopMonitor(StopMonitor monitor)
    { stopMonitor = monitor; }

    /**
     * Adds a DataQuery to the request Queue.
     * Primarily used by the DataBrokerQueryInterface methods to
     * register requests.
     */

    public DataQuery push(DataQuery request)
    {
//        if (request.type == DataQuery.MODIFY)
//            System.out.println("modify request: " + request.toString());
        if (debug)
        {
            System.out.println("Thread: " + Thread.currentThread().getName() + " pushing request: " + request.getTypeString() + ": " + request.toString() + " in broker " + this.id());
            try {
            Thread.sleep(100);
            Thread.dumpStack();
            Thread.sleep(100);
            } catch (Exception e) {};    
            System.out.println("----------");
        }

        for (int i = 0; i < listeners.size(); i++)
            request.addDataListener(new SwingDataListener((DataListener) listeners.get(i)));

        synchronized (requestQueue)
        {
            requestQueue.add(request);
        }

        if (stopMonitor != null) stopMonitor.updateWatchers();

        synchronized (requestQueue)
        {
            requestQueue.notifyAll();
        }


        return request;  // returns the same request for easy chaining.
    }

    /**
     * Gets the next DataQuery from the request Queue,
     * removing it from the queue as it does so.
     */

    public DataQuery pop()
    {

        DataQuery request = null;

        synchronized (requestQueue)
        {
            if (requestQueue.isEmpty()) return null;
            request = (DataQuery) requestQueue.firstElement();
            if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " popping request: " + request.getTypeString() + ": " + request.toString() + " in broker " + this.id());

            requestQueue.removeElementAt(0);
            request.setRunning();                // set the running flag (for use by StopMonitor)
        }

        return request;
    }

    /**
     * Removes a particular query from the pending query list...
     */

    public void removeQuery(DataQuery query)
    {
        synchronized (requestQueue)
        {
            requestQueue.remove(query);
        }
    }

    /**
     * Returns whether there are more DataQuerys pending.
     */

    public boolean hasRequests()
    {
        synchronized (requestQueue)
        {
            return !requestQueue.isEmpty();
        }
    }

    /**
     * Wait until notified that something (presumably
     * an addition to the queue) has occured.  When woken,
     * process all queue requests, and then go back to
     * waiting.
     */

    public void run()
    {
        while (true)
        {
            if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " processing Queue of length: " + requestQueue.size() + " in broker " + id);

            if (processQueue() == false)
            {
                if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " ending." + requestQueue.size());
                return;
            }

            try
            {
                if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " waiting in run() loop");
                synchronized (requestQueue)
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
     * process all queue requests
     */

    protected boolean processQueue()
    {
        while (hasRequests())
        {
            current = pop();            // keep track of the current query for reporting

            if (current == null)
                return true;  // sanity check: fantastically small (?) chance of thread magic causing a null return?

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
     * Process a specific request.
     */

    protected void processRequest(DataQuery request)
    {
        if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " process request " + request.id);

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

            switch (request.getType())
            {
                case DataQuery.EXISTS:
                    doExistsQuery(request);
                    break;

                case DataQuery.READENTRY:
                    doEntryQuery(request);
                    break;

                case DataQuery.LIST:
                    doListQuery(request);
                    break;

                case DataQuery.SEARCH:
                    doSearchQuery(request);
                    break;

                case DataQuery.MODIFY:
                    doModifyQuery(request);
                    break;

                case DataQuery.COPY:
                    doCopyQuery(request);
                    break;

                case DataQuery.XWINCOPY:
                    doCopyBetweenWindowsQuery(request);
                    break;

                case DataQuery.GETRECOC:
                    doGetRecOCsQuery(request);
                    break;

                case DataQuery.EXTENDED:
                    doExtendedQuery(request);
                    break;

                case DataQuery.UNKNOWN:

                default:
                    throw new NamingException("JX Internal Error: Unknown Data DataBroker Request type: " + request.getType());
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

    public DataQuery copyTreeBetweenWindows(DN sourceNodeDN, DN newNodeDN, DataBrokerUnthreadedInterface sourceData, boolean overwriteExistingData)
    {
        return push(new DataQuery(DataQuery.XWINCOPY, sourceNodeDN, newNodeDN, sourceData, overwriteExistingData));
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
     * Adds a data listener to every DataQuery generated by this broker.
     * For threading simplicity, no data listeners are added to DataQuerys
     * <i>allready</i> in the request queue.<p>
     * While it is not an error to register a listener multiple times,
     * a listener will still only be notified once.
     */
    public void addDataListener(DataListener l)
    {
        if (listeners.contains(l) == false)
        {
            listeners.add(l);
        }
    }


    /**
     * Removes a data listener from every DataQuery generated by this broker.
     *
     * @param l the listener to be notified when the data is ready.
     */

    public void removeDataListener(DataListener l)
    {
        if (listeners.contains(l) == true)
        {
            listeners.remove(l);
        }
    }


    // Abstract DataBrokerQueryInterface methods - must be extended

    public abstract boolean isModifiable();

    public abstract LdapContext getLdapContext() throws NamingException;

    public abstract boolean isActive();

    public abstract SchemaOps getSchemaOps();

    /**
     * Sets the finish flag of a request and returns
     * the query.  Often overloaded by derived broker classes.
     */

    protected DataQuery finish(DataQuery request)
    {
        if (debug) System.out.println("Thread: " + Thread.currentThread().getName() + " request " + request.id + " finished ");
        request.finish();
        return request;
    }


    // Methods for the stop monitor - return a list of outstanding tasks

    /**
     * Returns the DataQuery currently being processed (if any).
     *
     * @return the current DataQuery (may be null if there is none).
     */
    public DataQuery getCurrent()
    { return current; }

    /**
     * Returns the vector of outstanding queries.
     *
     * @return a vector of (DataQuery).
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

    /*
     *
     *    DataBrokerQueryInterface methods
     *
     */


    /**
     * Method for the DataBroker interface - chains to
     * dirOp.exists().
     */

    protected DataQuery doExistsQuery(DataQuery request)
            throws NamingException
    {
        unthreadedExists(request.requestDN());
        request.setStatus(true);
        return finish(request);
    }

    /**
     * Method for the DataBroker interface - chains to
     * list().
     */

    protected DataQuery doListQuery(DataQuery request)
            throws NamingException
    {
        request.setEnumeration(unthreadedList(request.requestDN()));
        return finish(request);
    }

    /**
     * Method for the DataBroker interface - chains to
     * dirOp.read().
     */

    protected DataQuery doEntryQuery(DataQuery request)
            throws NamingException
    {
        request.setEntry(unthreadedReadEntry(request.requestDN(), null));
        return finish(request);
    }

    /**
     * Method for the DataBroker interface - chains to
     * search().
     */

    protected DataQuery doSearchQuery(DataQuery request)
            throws NamingException
    {
        DXNamingEnumeration en = unthreadedSearch(request.requestDN(), request.filter(), request.searchLevel(), request.returnAttributes());
        request.setEnumeration(en);
        return finish(request);
    }

    /**
     * Method for the DataBroker interface - chains to
     * modifyEntry().
     */

    protected DataQuery doModifyQuery(DataQuery request)
            throws NamingException
    {
        unthreadedModify(request.oldEntry(), request.newEntry());
        request.setStatus(true);
        return finish(request);
    }

    /**
     * Method for the DataBroker interface - chains to
     * unthreadedCopy().
     */

    protected DataQuery doCopyQuery(DataQuery request)
            throws NamingException
    {
        unthreadedCopy(request.oldDN(), request.requestDN());
        request.setStatus(true);
        return finish(request);
    }

    /**
     * Method for the DataBroker interface - chains to
     * unthreadedCopyBetweenWindows().
     */

    protected DataQuery doCopyBetweenWindowsQuery(DataQuery request)
            throws NamingException
    {
        unthreadedCopyBetweenWindows(request.oldDN(), request.getExternalDataSource(), request.requestDN(), request.overwriteExistingData());

        // grab the first level of results and stick them in an enumeration, so the display smart tree can show some new nodes...
        // DOESN'T WORK?  Do separate refresh() query instead? request.setEnumeration(unthreadedList(request.requestDN()));   // for  efficiency we could get this in the proceeding call, but this is easier.  I'm a hack...

        request.setStatus(true);
        return finish(request);
    }


    /**
     * Method for the DataBroker interface - chains to
     * unthreadedGetRecOCs.
     */

    protected DataQuery doGetRecOCsQuery(DataQuery request)
            throws NamingException
    {
        request.setArrayList(unthreadedGetRecOCs(request.requestDN()));
        return finish(request);
    }


    /**
     * Utility method for extended queries - returns whether
     * a 'masked' exception has occured.
     *
     * @return the exception, or null if there is none.
     */

    public Exception getException()
    {
        return null;
    }

    /**
     * Utility method for extended queries - allows
     * a 'masked' exception to be cleared.
     */

    public void clearException()
    {
    }

    /**
     * As a way to directly access the directory broker, a DataBrokerQueryInterface
     * MAY choose to publish the directory broker.
     *
     * @return the DataBroker - may be null.
     */

    public DataBroker getBroker()
    { return this; }

    /*
     *   Cross-Window copying stuff... unfortunately we can't easily whack this in one of the normal '...Ops'
     *   classes, as those packages are jndi based, and we need to deal with non-jndi based brokers (such as
     *   the offlineBroker used for LDIF).
     */

    /**
     * This is a tricky method that copies stuff between possibly different data sources... hence we write it
     * using the other 'unthreaded()' methods, and rely on the various implementing classes to do the actual work...
     *
     * @param oldNodeDN
     * @param externalDataSource
     * @param newNodeDN
     * @throws NamingException
     */
    public void unthreadedCopyBetweenWindows(DN oldNodeDN, DataBrokerUnthreadedInterface externalDataSource, DN newNodeDN, boolean overwriteExistingData)
            throws NamingException
    {
        if (oldNodeDN == null || newNodeDN == null || externalDataSource == null)
            return; // incorrectly initialised - bail out

        if (!externalDataSource.unthreadedExists(oldNodeDN))
            return;  // source node doesn't exist - bail out

        boolean newNodeExists = this.unthreadedExists(newNodeDN);

        CBpbar progressBar = new CBpbar(CBUtility.getDefaultDisplay(), CBIntText.get("Cross-Window Copy"), CBIntText.get("estimate"));

        // special root node processing; check if copy, replace or remove...
        if (newNodeExists && oldNodeDN.getLowestRDN().equals(newNodeDN.getLowestRDN()))
        {
            if (overwriteExistingData)
            {
                // replacing - clear away old tree, then copy new one over
                System.out.println("REPLACING");
                this.unthreadedModify(new DXEntry(newNodeDN), null);  // delete old tree
                unthreadedCopyBetweenWindowsLoop(oldNodeDN, externalDataSource, newNodeDN, progressBar);
            }
            else
            {
                // merging - work through the tree, creating new entries and merging with existing ones were necessary
                System.out.println("MERGING");
                unthreadedMergeBetweenWindowsLoop(oldNodeDN, externalDataSource, newNodeDN, progressBar);
            }
        }
        else
        {
            // copying to fresh branch - no old entries to worry about
            System.out.println("COPYING");
            unthreadedCopyBetweenWindowsLoop(oldNodeDN, externalDataSource, newNodeDN, progressBar);
        }


    }

    /**
     * This copies a tree from the external data source to a fresh tree in the current data source,
     * @param oldNodeDN
     * @param externalDataSource
     * @param targetDN
     * @param progressBar
     * @throws NamingException
     */
    protected void unthreadedCopyBetweenWindowsLoop(DN oldNodeDN, DataBrokerUnthreadedInterface externalDataSource, DN targetDN, CBpbar progressBar)
            throws NamingException
    {
        DXEntry newEntry = constructNewEntryData(oldNodeDN, externalDataSource, targetDN);
        this.unthreadedModify(null, newEntry); // blow any existing entry away

        // ... and then keep the loop going
        ArrayList<DN> children = getChildren(oldNodeDN, externalDataSource);
        for (DN childDN:children)
        {
            String newDNString = childDN.getLowestRDN().toString() + "," + targetDN.toString();
            DN targetChildDN = new DN(newDNString);
            unthreadedCopyBetweenWindowsLoop(childDN, externalDataSource, targetChildDN, progressBar);
        }
    }

    /**
     * This copies a tree from the external data source into an existing tree, adding new nodes and merging with existing
     * nodes when there are identical entries.  (In the merge new node data *wins*, but old attributes are retained if not
     * present in the new entry)
     * @param oldNodeDN
     * @param externalDataSource
     * @param targetDN
     * @param progressBar
     * @throws NamingException
     */
    protected void unthreadedMergeBetweenWindowsLoop(DN oldNodeDN, DataBrokerUnthreadedInterface externalDataSource, DN targetDN, CBpbar progressBar)
            throws NamingException
    {
        DXEntry newEntry = constructNewEntryData(oldNodeDN, externalDataSource, targetDN);
        DXEntry existingEntry = null;

        try
        {
            existingEntry = this.unthreadedReadEntry(targetDN, null);
        }
        catch (NameNotFoundException e)
        {
            // expected condition - no existing entry, so just copy new entry as normal
        }


        this.unthreadedModify(existingEntry, newEntry); // merge with any existing entry - or 'add' if existing entry is null

        // ... and then keep the loop going
        ArrayList<DN> children = getChildren(oldNodeDN, externalDataSource);
        for (DN childDN:children)
        {
            DN targetChildDN = new DN(childDN.getLowestRDN().toString() + "," + targetDN.toString());
            unthreadedMergeBetweenWindowsLoop(childDN, externalDataSource, targetChildDN, progressBar);
        }
    }



    /**
     * We create a new Entry, based closely on the original entry being copied from the external data source.
     * Some housekeeping may be required however around naming attributes.
     * @param oldNodeDN
     * @param externalDataSource
     * @param targetDN
     * @return
     * @throws NamingException
     */
    protected DXEntry constructNewEntryData(DN oldNodeDN, DataBrokerUnthreadedInterface externalDataSource, DN targetDN)
            throws NamingException
    {
        DXEntry newEntry = externalDataSource.unthreadedReadEntry(oldNodeDN, null); // newEntry is a copy of the old source Entry

        newEntry.setDN(targetDN);
        RDN newRDN = targetDN.getLowestRDN();
        RDN oldRDN = oldNodeDN.getLowestRDN();
        if (!oldRDN.equals(newRDN))  // TODO: which will only be the case (possibly) for the root node - consider moving out into top level unthreadedCopyBetweenWindows
        {
            String[] atts = newRDN.getAttIDs();
            for (String attributeName:atts)
            {
                String val = newRDN.getRawVal(attributeName);
                Attribute namingAtt = newEntry.get(attributeName);
                if (namingAtt == null || namingAtt.size()<=1)  // if it's a single valued (or missing) attribute, just replace it.
                    newEntry.put(new BasicAttribute(attributeName, val));
                else
                    newEntry.get(attributeName).add(val); // if it's multi valued, add a new value...
            }
        }
        return newEntry;

    }

    /**
     * (method derived from AdvancedOps namesake)
     * <p>This searches for all the children of the given named entry, and returns them as an
     * ArrayList.</p>
     *
     * @param base the base entry to search from
     * @return an ArrayList of [Name] objects representing children.
     */
    protected ArrayList<DN> getChildren(DN base, DataBrokerUnthreadedInterface externalDataSource)
            throws NamingException
    {
        ArrayList<DN> children = new ArrayList<DN>();

        NamingEnumeration rawList = externalDataSource.unthreadedList(base);

        while (rawList.hasMoreElements())
        {
            NameClassPair child = (NameClassPair) rawList.next();

            // XXX Because of apparent short comings in jndi (or maybe I'm using it wrong?) it seems impossible to
            // tell whether a DN is relative to the search base or not.  This is particularly bad when it comes
            // to dealing with aliases...!  So we check its size instead :-/
            //
            //Name childDN = parser.parse(child.getName());

            DN childDN = new DN(child.getName()); // they really didn't miss a trick when it came to making JNDI clumsy...

            if (childDN.size() == 1 && base.size() > 0)
            {
                childDN = new DN(child.getName() + "," + base.toString());
                System.out.println("combining base: " + base + " and child: " + child.getName() + " to get: " + childDN.toString());
            }

            children.add(childDN);
        }
        return children;
    }

    public String id() { return "DataBroker " + id;};
    
}