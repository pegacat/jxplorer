package com.ca.directory.jxplorer.broker;

import com.ca.commons.cbutil.*;
import com.ca.commons.naming.*;

import javax.naming.*;
import javax.naming.directory.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *    This static class contains special code for Mitchell Rozonkiewiecz 
 *    of the OS390 security group, who requires special handling of ldap
 *    modify requests.  It is only activated if a file 'specialocs.txt' is
 *    present in the browser home directory.
 */

public class Special 
{
    private static Logger log = Logger.getLogger(Special.class.getName());

    private Special() {} // class can't be instantiated.

    /**
     *    Returns the set of attributes that are in the newSet, but
     *    not in the old set, excluding the rdnclass.
     *    IN ADDITION: it returns the set of all attribute values that
     *    have been modified or added as a add records (In the case of
     *    modified attributes, this requires a separate delete op to
     *    work properly)
     *    @param newRDN the RDN of the newer entry.
     *           (usually this is the same as the RDN of the original entry). 
     *           May be null if it is not to be checked.
     *           <b>Not currently used</b>
     *    @param oldSet the set of already existing attributes
     *    @param newSet the set of new attributes to test
     *    @return attributes that must be added to the object.
     */
     
    public static DXAttributes getAdditionSet(RDN newRDN, Attributes oldSet, Attributes newSet) throws NamingException
    {
        DXAttributes changes = new DXAttributes();
        NamingEnumeration testAtts = newSet.getAll();

        // no need to check for rdn as it must exist in the old set already :-)
        
        while (testAtts.hasMore())
        {
            Attribute testVal = (Attribute)testAtts.next();
            Attribute oldVal = oldSet.get(testVal.getID());
            if (! emptyAtt(testVal))            // don't add empty atts!
            {
                if (oldVal == null)             // if the old attribute was null
                    changes.put(testVal);       // ...add it 
                else if (emptyAtt(oldVal))      // or if it had only null values add it. (It was probably created by the browser,
                    changes.put(testVal);       // and doesn't exist in the database)
                else if (attributesEqual(oldVal, testVal) == false)
                {
                    Attribute adds = getDiff(oldVal, testVal);
                    if (adds != null) changes.put(adds);                
                }    
            }                    
        }    
        return changes;
    }
    
    /**
     *    Returns an attribute containing all values that are
     *    contained in changeAtt, but not in baseAtt.
     *    @param baseAtt the basic attribute set to compare changeAtt against
     *    @param changeAtt an attribute set containing possibly changed values.
     */
     
    public static DXAttribute getDiff(Attribute baseAtt, Attribute changeAtt)
    {
        try
        {
            Enumeration changeVals = changeAtt.getAll();
            DXNamingEnumeration baseVals = new DXNamingEnumeration(baseAtt.getAll());
            DXAttribute mods = new DXAttribute(baseAtt.getID());
            while (changeVals.hasMoreElements())
            {
                Object o = changeVals.nextElement();
                if (baseVals.contains(o) == false)
                    mods.add(o);
            }
            return (mods.size() > 0)?mods:null;
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "Unexpected Error in Special.java: ", e);
            return null;
        }    
    }    
    
    /**
     *    Returns all attributes that have not changed.
     *    
     *    @param newRDN the RDN of the newer entry.
     *           (usually this is the same as the RDN of the original entry). 
     *           May be null if it is not to be checked.
     *    @param oldSet the set of already existing attributes
     *    @param newSet the set of new attributes to test
     *    @return attributes that require updating
     */
         
    public static DXAttributes getReplacementSet(RDN newRDN, Attributes oldSet, Attributes newSet) 
        throws NamingException
    {
    
        DXAttributes changes = new DXAttributes();
        NamingEnumeration testAtts = newSet.getAll();
        
        while (testAtts.hasMore())                                // find changed attributes
        {
            Attribute testAtt = (Attribute)testAtts.next();
        
            String ID = testAtt.getID();
            
            Attribute oldAtt = oldSet.get(ID);

            /*
             *    Check if we're dealing with a naming attribute - if we are, make
             *    sure the naming value doesn't get included in our handling...
             */
            
            if ((newRDN!=null) && (newRDN.contains(ID)))          // need to tread carefully, as 
            {                                                     // we're dealing with a naming att...  
                String namingValue = newRDN.getRawVal(ID);
            
                if (oldAtt != null)                               // Would this be an Error???
                {
                    testAtt = new DXAttribute(testAtt);           // make local copies,
                    oldAtt  = new DXAttribute(oldAtt);            // (so we can remove the naming value,
                    testAtt.remove(namingValue);                  // without affecting the passed in data)
                    oldAtt.remove(namingValue);
                    
System.out.println("and finally we end up with: \n new: " + testAtt.toString() + "\n old: " + oldAtt.toString());
                }
            }
            
            
            if (attributesEqual(oldAtt, testAtt))
            {
                if (emptyAtt(testAtt) == false)
                    changes.put(testAtt);
            }
            
        } 
        
        return changes;
    }

        
    
    /**
     *    Returns the set of attributes that are in the oldSet, but
     *    not in the new set, and thus must be deleted. (excluding the rdnclass).
     *
     *    @param newRDN the RDN of the newer entry.
     *           (usually this is the same as the RDN of the original entry). 
     *           May be null if it is not to be checked.
     *    @param oldSet the set of already existing attributes
     *    @param newSet the set of new attributes to test
     */
    
    public static DXAttributes getDeletionSet(RDN newRDN, Attributes oldSet, Attributes newSet) throws NamingException
    {
        DXAttributes changes = new DXAttributes();
        NamingEnumeration oldAtts = oldSet.getAll();


        // look for old attributes that no longer exist...    
        
        while (oldAtts.hasMore())
        {
            Attribute oldAtt = (Attribute)oldAtts.next();
            Object val = oldAtt.get();
            
            if ("".equals(val)) 
            {
                val = null; //XXX zero length string hack - don't automatically delete zero length strings.
            }
            
            if ((val != null)) // if the old value was real (i.e. not null or blank string...)
            {
                String ID = oldAtt.getID();

                if (newRDN == null || newRDN.contains(ID)==false) // (can't delete the rdn class...)
                {
                    Attribute newAtt = (Attribute)newSet.get(ID);

                    if (newAtt != null)    // check whether the new att exists, and has
                    {                      // a real value - if not, delete it.
                        if (emptyAtt(newAtt))
                            newAtt = null;
                    }
                    
                    if (newAtt == null)  // .. but it no longer exists...
                    {
                        changes.put(new DXAttribute(oldAtt.getID()));  // ... delete it.
                    }   
                    else if (attributesEqual(newAtt, oldAtt) == false)  
                    {
                        changes.put(getDiff(newAtt, oldAtt));  // delete changed/removed values
                    }
                }    
            }    
        }
        
        return changes;
    }
/*
    static private NamingEnumeration getMissingValues(NamingEnumeration A, NamingEnumeration B) 
        throws NamingException
    {
        //if (A==null||B==null) return new DXNamingEnumeration();
        
        DXNamingEnumeration a = new DXNamingEnumeration(A);
        DXNamingEnumeration b = new DXNamingEnumeration(B);
        if (a==null) System.out.println("a = null!"); else System.out.println("a = " + a.toString());
        if (b==null) System.out.println("b = null!"); else System.out.println("b = " + b.toString());

        DXNamingEnumeration ret = new DXNamingEnumeration(b);
        while (a.hasMore())
        {
            ret.remove(a.next());
        }
        return ret;
    } 
*/

    /**
     *    Utility ftn: checks that an attribute is not null and has at least
     *    one non-null value.
     */
    public static boolean emptyAtt(Attribute att)
    {
         return DXAttribute.isEmpty(att); 
    }

    
    private static boolean attributesEqual(Attribute a, Attribute b) 
        throws NamingException
    {
        // sanity checks...
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() == 0 && b.size() == 0) return true;
        if (a.size() == 0 || b.size() == 0) return false;
        if (a.get() == null && b.get() == null) return true;
        if (a.get() == null || b.get() == null) return false;
        if (a.getID().equals(b.getID())==false) return false;
        
        try
        {
            Object[] A = CBArray.enumerationToArray(a.getAll());
            Object[] B = CBArray.enumerationToArray(b.getAll());
            return CBArray.isUnorderedEqual(A,B);
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "Naming Exception testing attributes " + a.getID() + " & " + b.getID() + " in DXAttributes:attributesEqual()", e);
        }    
        return false; // only here if error occurred.
    }
    
}