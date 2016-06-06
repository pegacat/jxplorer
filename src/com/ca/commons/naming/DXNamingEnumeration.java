package com.ca.commons.naming;

import java.util.*;
import java.text.Collator;

import javax.naming.*;

/**
 *    A simple class that implements NamingEnumeration, used to 
 *    bundle up search results 'n stuff.  Used by not-really-jndi
 *    ftns to wrap things nicely for higher level ftns that are
 *    expecting NamingEnumerations.
 *
 *    as usual, the enumeration can be enumerated using the model:
 *    <pre>
 *     while (myEnumeration.hasMoreElements())
 *          doSomethingWith(myEnumeration.nextElement();
 *    </pre>
 *    in addition, it supports the NamingEnumeration interface 
 *    equivalent, which (<b>in the future</b>) may also throw a NamingException.
 *    <pre>
 *     try
 *     {
 *         while (myEnumeration.hasMore())
 *             doSomethingWith(myEnumeration.next();
 *     }
 *     catch (NamingException e) { }
 *    </pre>
 *
 *     As a spot of convenience evil, it implements a sort() ftn as well...
 *     (Since it's not a 'dynamic' enumeration as a normal naming enumeration
 *     would be, but uses a vector base object), and also allows the enumeration
 *     to be dumped out as a vector or a string array :-) .
 */
 
public class DXNamingEnumeration implements NamingEnumeration
{
    private int pointer = 0;
    private ArrayList data;

        // get a single platform specific language collator for use in sorting.
    private static Collator myCollator = Collator.getInstance();

    /**
     *    <p>A quickie class for ordering stuff using their intrinsic toString() methods...
     *    ... not necessarily the fastest because it requires many, many 'toString()'
     *    calls.  Preferable is to implement 'Comparable' interface in the object and
     *    use the 'fastSort()' method.</p>
     */

    private class SimpleComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            return myCollator.compare(o1.toString(), o2.toString());
        }
    }
    /**
     *    The constructor does nothing except initialise class variables.
     */
    public DXNamingEnumeration() { data = new ArrayList(); }

    /**
     *    A quicky to wrap normal enumerations with as well...
     */
    public DXNamingEnumeration(Enumeration ne) 
    { 
        data = new ArrayList();
        while (ne.hasMoreElements())
            add(ne.nextElement());
    }


    /**
     *    The constructor takes a collection and uses
     *    it to initialise with.
     */

    public DXNamingEnumeration(Collection <?> c)
    {
        data = new ArrayList();
        data.addAll(c);
    }


    /**
     *    The constructor takes another NamingEnumeration and uses
     *    it to initialise with.     
     */        
     
    public DXNamingEnumeration(NamingEnumeration ne) 
    { 
        data = new ArrayList();
        if (ne!=null)
            while (ne.hasMoreElements())
                add(ne.nextElement());
    }

    /**
     *    A convenience constructor to wrap an existing ArrayList.
     *    Note that since the enumeration is supposed to be read-only,
     *    the initialising ArrayList is <i>not</i> cloned, but used as is.
     */
    /* replaced by Collection based constructor above for simplicity...
    public DXNamingEnumeration(ArrayList listData)
    {
        data = listData;
    }
    */
    /**
     *    Adds an object to the enumeration.
     *    @param o object to be added.
     */        
    public void add(Object o)        { data.add(o); }

    /**
     *    Removes an object from the enumeration.
     *    @param o the object to be removed.
     */        
    public void remove(Object o)        { data.remove(o); }
    
    
    /** 
     *    Enumerations can't usually be re-used.  Sometimes it would be
     *    nice though... this resets the enumeration so you can reread it,
     *    which is useful for debugging (i.e. you can print it before use...)
     */
    public void reset()              { pointer = 0; }
    
    /**
     *    Not really necessary, this returns the number of elements in the
     *    enumeration.
     *    @return number of objects in enumeration
     */
    public int size()                { return data.size(); }
    
    /* 
     *    identical in ftn to hasMoreElements().  In future, this may
     *    throw NamingEnumerationExceptions.
     *    @return true if more elements are available.
     */
    public boolean hasMore()         { return (pointer < data.size()); }
    
    /*
     *    standard enumeration ftn.
     *    @return true if more elements are available
     */
    public boolean hasMoreElements() { return hasMore(); }
        
    /*
     *    identical in ftn to nextElement().
     *
     *    @return returns the next element in the enumeration.
     */        
    public Object next() throws NoSuchElementException
    {
        try
        { return data.get(pointer++); }
        catch (ArrayIndexOutOfBoundsException e)
        { throw new NoSuchElementException(); }
    }    
    
    /*
     *    standard enumeration ftn.
     *
     *    @return returns the next element in the enumeration.
     */        
    public Object nextElement() throws NoSuchElementException { return next(); }
    
    
    /**
     *    <p>This method attempts to order the components of the
     *    SimpleEnumeration using their intrinsic 'toString()'
     *    methods sorted by a language sensitive collator
     *   (this may be meaningless for some components).</p>
     */
     
    public DXNamingEnumeration sort()
    {
        Collections.sort(data, new SimpleComparator());

        return this;
    }

    /**
     * This sorts a DXNamingEnumeration that contains an
     * enumeration of objects that implement the Comparable interface
     * (such as DXAttribute).
     * 
     * @return itself only sorted
     */

    public DXNamingEnumeration fastSort()
    {
        Collections.sort(data);

        return this;
    }

    /**
     *    A simple existance test against the core ArrayList list of
     *    objects.
     */   
      
    public boolean contains(Object test)
    {
        return data.contains(test);
    }
    
    /** 
    *    Included for Naming Enumeration compatibility... does nothing,
    *    'cause DXNamingEnumeration isn't really an enumeration, and
    *    has already slurped all the data... :-)
    */
    public void close() {;} 
    
    public String toString()  // mainly used for debugging
    {
        StringBuffer ret = new StringBuffer();
        for (int i=0; i<data.size(); i++)
        {
            Object o = data.get(i);
            ret.append((o==null)?"null":o.toString() + "\n");
        }    
        return ret.toString();   
    }
    
    public Object[] toArray()
    {
        return data.toArray();
    } 
     
    
    public String[] toStringArray()
    {
        String[] ret = new String[data.size()];
        for (int i=0; i<data.size(); i++)
        {
            Object o = data.get(i);
            ret[i] = ((o==null)?null:o.toString());
        }   
        return ret; 
    } 
 
    public ArrayList getArrayList() { return data; }
}