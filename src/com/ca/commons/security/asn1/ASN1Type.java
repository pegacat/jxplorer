
package com.ca.commons.security.asn1;

import java.util.Hashtable;

/**
 * This class is the collection of all necessary ASN.1 types that are
 * used in this package and other packages.
 */
public class ASN1Type implements java.io.Serializable
{

    private int tag;
    private String name;
    
    private static Hashtable asn1Types = new Hashtable(27);
    
    /* UNIVERSAL class types */
    
    public static final ASN1Type BOOLEAN = new ASN1Type(1,
                                           "BOOLEAN");
    public static final ASN1Type INTEGER = new ASN1Type(2,
                                           "INTEGER");
    public static final ASN1Type BIT_STRING = new ASN1Type(3,
            "BIT STRING");
    public static final ASN1Type OCTET_STRING = new ASN1Type(4,
            "OCTET STRING");
    public static final ASN1Type NULL = new ASN1Type(5,
                                        "NULL");
    public static final ASN1Type OBJECT_ID = new ASN1Type(6,
            "OBJECT IDENTIFIER");
    public static final ASN1Type ENUMERATED = new ASN1Type(10,
            "ENUMERATED");
    public static final ASN1Type PrintableString = new ASN1Type(19,
            "PrintableString");
    public static final ASN1Type T61String = new ASN1Type(20,
            "T61String");
    public static final ASN1Type IA5String = new ASN1Type(22,
            "IA5String");
    public static final ASN1Type UTCTime = new ASN1Type(23,
                                           "UTCTime");
    public static final ASN1Type GENERALIZEDTIME = new ASN1Type(24,
            "GeneralizedTime");
    public static final ASN1Type UniversalString = new ASN1Type(28,
            "UniversalString");
    public static final ASN1Type BMPString = new ASN1Type(30,
            "BMPString");
    public static final ASN1Type SEQUENCE = new ASN1Type(48,
                                            "SEQUENCE");
    public static final ASN1Type SET = new ASN1Type(49,
                                       "SET");
    public static final ASN1Type ContextSpecific = new ASN1Type(128,
            "ContextSpecific");
            
    /**
     * Creates and registers a new ASN1Type.
     * @param	t the tag of the ASN1Type
     * @param	n the name of the ASN1Type
     */
    public ASN1Type(int t, String n)
    {
        tag = t;
        name = n;
        asn1Types.put(new Integer(tag), name);
    }
    
    /**
     * Creates an ASN1Type instance with the given tag 't'.
     */
    public ASN1Type(int t)
    {
        tag = t;
        name = asn1Name(t);
    }
    
    /**
     * Gets the name of the ASN1Type.
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Gets the tag of the ASN1Type.
     */
    public int getTag()
    {
        return tag;
    }
    
    /**
     * Gets the hashcode of the ASN1Type.
     */
    public int hashCode()
    {
        return tag;
    }
    
    /**
     * Tests whether the given object represents the same ASN.1 type
     * as this ASN1Type.
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof ASN1Type))
        {
            return false;
        }
        ASN1Type a = (ASN1Type) o;
        if (a.tag == this.tag)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /**
     * Gets the string representation of the ASN1Type.
     */
    public String toString()
    {
        return "ASN.1 type " + name + " (" + asn1Name(tag) + ") [" +
               tag + "] ";
    }
    
    /**
     * Gets the ASN1Type name corresponding to the given tag.
     */
    public static String asn1Name(int t)
    {
        String n = (String) asn1Types.get(new Integer(t));
        if (n == null)
        {
            return "Unknown";
        }
        else
        {
            return n;
        }
    }
}
