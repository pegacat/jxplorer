package com.pegacat.testprovider;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.*;
import java.text.Collator;

/**
 * (c) GroupMind Project
 * - Dr Christopher Betts 2005
 *
 * This is a concrete implementation of the NamingEnumeration interface, backed by an ArrayList.
 */

public class TestProviderEnumeration <E> extends ArrayList <E> implements NamingEnumeration <E>
{
    private int currentElement = 0;

    /**
     * Allows us to create a test enumeration from an existing enumeration
     * @param initialiser
     */
    public TestProviderEnumeration(Enumeration <E> initialiser)
    {
        while (initialiser.hasMoreElements())
        {
            add(initialiser.nextElement());
        }
    }



    /**
     * Chain the ArrayList constructor
     */
    public TestProviderEnumeration()
    {
        super();
    }

    /**
     * Chain the ArrayList constructor
     */
    public TestProviderEnumeration(Collection c)
    {
        super(c);
    }

    public E next() throws NamingException
    {
        if (!hasMoreElements())
            throw new NamingException("no more elements");

        return get(currentElement++);
    }

    public boolean hasMore() throws NamingException
    {
        return hasMoreElements();
    }

    public void close() throws NamingException
    {
        clear();
        currentElement = 0;
    }

    public boolean hasMoreElements()
    {
        return (currentElement < size());
    }

    public E nextElement()
    {
         if (!hasMoreElements())
            throw new NoSuchElementException();

        return get(currentElement++);
    }

     public String[] toStringArray()
    {
        String[] ret = new String[size()];

        for (int i=0; i<size(); i++)
        {
            Object o = get(i);
            if (o==null) o="null";
            ret[i] = o.toString();
        }
        return ret;
    }

    /**
     *   Utility class to allow us to sort enumeration alphabetically for easy test comparision.  
     */

    private class AlphabeticComparator implements Comparator
    {
        Collator collator = Collator.getInstance();

        public int compare(Object a, Object b)
        {
            return collator.compare(a, b);
        }
    }

    /**
     *  Sorts the contents of the enumeration into alphabetical order for easy testing.
     */

    public void sort()
    {
        Collections.sort( this, new AlphabeticComparator());
    }

}
