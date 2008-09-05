
package com.ca.commons.security.asn1;

import java.util.Hashtable;
import java.math.BigInteger;

/**
 * This class represents all ASN.1 primitive types. It is only
 * used in this package. For each ASN.1 primitive type, there is a
 * corresponding java class representation. For example, the BOOLEAN
 * ASN1Type is represented by java Boolean class. When creating a Primitive
 * object, a real object from its implementation class is actually assigned
 * to the Primitive object.
 */
public class Primitive extends ASN1Object implements java.io.Serializable
{

    private Class basicType;
    private Object value;
    
    private static Hashtable checkup = new Hashtable(20);
    
    static{
        register(ASN1Type.BOOLEAN, (new Boolean(true)).getClass());
        register(ASN1Type.INTEGER, (new BigInteger("0")).getClass());
        register(ASN1Type.OCTET_STRING, (new Object()).getClass());
        register(ASN1Type.NULL, (new Object()).getClass());
        register(ASN1Type.OBJECT_ID, (new String()).getClass());
        register(ASN1Type.BIT_STRING, (new Object()).getClass());
        register(ASN1Type.IA5String, (new String()).getClass());
        register(ASN1Type.T61String, (new String()).getClass());
        register(ASN1Type.PrintableString, (new String()).getClass());
        register(ASN1Type.UTCTime, (new String()).getClass());
        register(ASN1Type.GENERALIZEDTIME, (new String()).getClass());
        
        register(ASN1Type.UniversalString, (new Object()).getClass());
        register(ASN1Type.BMPString, (new Object()).getClass());
        register(ASN1Type.ENUMERATED, ( new BigInteger("0")).getClass());
    }
    
    /**
     * Registers the implementation class of the ASN1Type.
     */
    private static void register(ASN1Type type, Class r)
    {
        checkup.put(type, r);
    }
    
    /**
     * Constructor.
     */
    public Primitive()
    {}
    
    /**
     * Initializes the Primitive object.
     */
    public void init(ASN1Type type)
    {
        asn1Type = type;
        byteArray = null;
        basicType = (Class) checkup.get(type);
        value = null;
    }
    
    /**
     * Gets the implementation class of the Primitive object.
     */
    public Class getType()
    {
        return basicType;
    }
    
    /**
     * Gets the value of the Primitive object.
     */
    public Object getValue()
    {
        return value;
    }
    
    /**
     * Sets the value of the Primitive object. If the input object is not
     * from the implementation class of the Primitive object, an
     * IllegalArgumentException is thrown.
     */
    public void setValue(Object v)
    {
        if (ofType(v, basicType))
        {
            value = v;
        }
        else
        {
            throw new IllegalArgumentException("Incompatible type");
        }
    }
    
    /**
     * Returns whether two objects are the same.
    public boolean equals(Object o) {
    	if (!(o instanceof iss.security.asn1.Primitive)) {
    		return false;
    	}
    	if (!this.asn1Type.equals(o.asn1Type)) {
    		return false;
    	}
    	if (!this.basicType.getName().equals(o.basicType.getName())) {
    		return false;
    	}
    
    	if (this.value instanceof java.lang.Boolean
    	|| this.value instanceof java.lang.String
    	|| this.value instanceof java.math.BigInteger) {
    		if (this.value.equals(o.value) {
    			return true;
    		}
    	} else if (this.value instanceof Object) {
    		byte [] b1 = null;
    		byte [] b2 = null;
    		try {
    			b1 = (byte []) this.value;
    			b2 = (byte []) o.value;
    		} catch(ClassCastException cce) {
    			return false;
    		}
    		return Util.compByteArray(b1, b2);
    	}
    	return false;
}
     */
    
    /**
     * A string representation of the Primitive object.
     */
    public String toString()
    {
        String result = super.toString();
        result += "\n\t" + basicType.toString();
        result += "\tvalue " + value;
        return result;
    }
    
    /**
     * Checks whether the object o is in the class t.
     */
    private boolean ofType(Object o, Class t)
    {
        if (t.getName().equals("java.lang.Boolean")
                && (o instanceof Boolean))
        {
            return true;
        }
        if (t.getName().equals("java.lang.String")
                && (o instanceof String))
        {
            return true;
        }
        if (t.getName().equals("java.math.BigInteger")
                && (o instanceof BigInteger))
        {
            return true;
        }
        if (t.getName().equals("java.lang.Object")
                && (o instanceof Object))
        {
            return true;
        }
        return false;
    }
}
