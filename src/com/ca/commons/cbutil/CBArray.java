package com.ca.commons.cbutil;

import java.util.Enumeration;
import java.util.Vector;


/**
 * Utility class for array operations.  Not particularly
 * fast - most operations implemented as linear searches.
 * XXX most of these may be redundant - see java 'util.Array' class...
 *
 * @author Chris Betts
 */


public class CBArray
{

    /**
     * Returns the list of elements that appear at least once
     * in one of the passed arrays.  An element appearing in
     * both arrays only appears once in the final array.
     * Behaviour is undefined if Elements are repeated within
     * an array; they may be trimmed, or not.
     *
     * @param a the first array to unionise
     * @param b the second array to unionise.
     * @return the resultant union.
     */

    public static Object[] union(Object[] a, Object[] b)
    {
//System.out.println("union starting; lengths " + a.length + " and " + b.length);    
        Object[] temp = new Object[a.length + b.length];
        int i = 0;
        for (i = 0; i < a.length; i++) temp[i] = a[i];
        for (int j = 0; j < b.length; j++)
            if (contains(a, b[j]) == false)   // no duplicates
                temp[i++] = b[j];
                
//for (int k=0; k<i; k++)
//    System.out.println("   " + k + " " + temp[k].toString());                
                
        if (a.length + b.length == i)
            return temp;
        else
        {
            Object[] ret = new Object[i];
            for (int k = 0; k < i; k++) ret[k] = temp[k];

            return ret;
        }
    }

    /**
     * Returns the list of elements that appear in a but
     * not in b.
     * Behaviour is undefined if Elements are repeated within
     * an array; they may be trimmed, or not.
     *
     * @param a the primary array
     * @param b the array to 'subtract'.
     * @return the resultant difference.
     */

    public static Object[] difference(Object[] a, Object[] b)
    {
//System.out.println("difference starting; lengths " + a.length + " and " + b.length);    
        if (a == null || b == null) return null;
        if (a.length == 0) return ((Object[]) a.clone());
        if (b.length == 0) return ((Object[]) a.clone());

        Object[] temp = new Object[a.length];
        int len = 0;
        for (int i = 0; i < a.length; i++)
            if (contains(b, a[i]) == false)
                temp[len++] = a[i];

        Object ret[] = new Object[len];
        for (int i = 0; i < len; i++) ret[i] = temp[i];
        
//for (int k=0; k<len; k++)
//    System.out.println("   " + k + " " + ret[k].toString() + " (" + ret.getClass().toString() + ")");         
        
        return ret;
    }

    /**
     * Returns the list of elements that appear in both
     * of the passed arrays.  An element appearing in
     * both arrays only appears once in the final array.
     * Behaviour is undefined if Elements are repeated within
     * an array; they may be trimmed, or not.
     *
     * @param a the first array to unionise
     * @param b the second array to unionise.
     * @return the resultant union.
     */

    public static Object[] intersection(Object[] a, Object[] b)
    {
//System.out.println("intersection starting; lengths " + a.length + " and " + b.length);    
        if (a == null || b == null) return null;
        if (a.length == 0) return ((Object[]) a.clone());
        if (b.length == 0) return ((Object[]) b.clone());

        if (a.length > b.length)            // make a the smallest array
        {
            Object[] c = a;
            a = b;
            b = c;
        }

        Object[] temp = new Object[a.length];

        int len = 0;
        for (int i = 0; i < a.length; i++)
            if (contains(b, a[i]) == true) temp[len++] = a[i];

        Object[] ret = new Object[len];
        for (int i = 0; i < len; i++) ret[i] = temp[i];
        return ret;
    }

    /**
     * Returns true if arrays have the same elements, in the same
     * order.
     */

    public static boolean isOrderedEqual(Object[] a, Object[] b)
    {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++)
            if (a[i].equals(b[i]) == false) return false;
        return true;
    }

    /**
     * Returns true if arrays have the same elements, in any
     * order.  Does *not* check for different numbers of
     * repeating elements - i.e. {A,A,B} == {A,B,B} is true.
     */
    public static boolean isUnorderedEqual(Object[] a, Object[] b)
    {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++)
            if (contains(b, a[i]) == false) return false;
        return true;
    }

    /**
     * Returns true if the passed object is not null,
     * and is equal to (at least one) element of the array.
     *
     * @param array the array that may contain the object.
     * @param o     the object to check the existence of.
     */
    public static boolean contains(Object array[], Object o)
    {
        if (o == null) return false;
        for (int i = 0; i < array.length; i++)
            if (o.equals(array[i])) return true;
        return false;
    }

    /**
     * Prints an array, using 'toString()' on each component.
     */

    public static void print(Object array[])
    {
        for (int i = 0; i < array.length; i++)
            System.out.println(i + ": " + array[i].toString());
    }


    public static Object[] enumerationToArray(Enumeration e)
    {
        Vector v = new Vector();
        while (e.hasMoreElements()) v.add(e.nextElement());
        return v.toArray();
    }

    public static Enumeration arrayToEnumeration(Object[] o)
    {
        Vector v = new Vector(o.length);
        for (int i = 0; i < o.length; i++) v.add(o[i]);
        return v.elements();
    }

    public static Object[] trimNulls(Object[] o)
    {
        Vector v = new Vector(o.length);
        for (int i = 0; i < o.length; i++)
            if (o[i] != null)
                v.add(o[i]);
        return v.toArray();
    }
}    