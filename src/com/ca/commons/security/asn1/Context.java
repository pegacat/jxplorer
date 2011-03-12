
package com.ca.commons.security.asn1;

/**
 * This class represents ASN.1 context specifically tagged types.
 */
public class Context extends ASN1Object implements java.io.Serializable
{

    private ASN1Object value;
    private boolean implicit;	//explicit or implicit tagging
    private int tag;			//one byte 00xxxxxx
    
    /**
     * Default constructor.
     */
    public Context()
    {}
    
    /**
     * Creates a Context object.
     * @param	t the tag.
     * @param	imp the flag indicating whether the Context object is using
     * implicit tagging or explicit tagging.
     * @param	o the underlying ASN1Object of the Context object. It can be
     * a Primitive or Sequence object but cannot be another Context object.
     * Otherwise an IllegalArgumentException is thrown.
     */
    public Context(int t, boolean imp, ASN1Object o)
    {
        asn1Type = ASN1Type.ContextSpecific;
        byteArray = null;
        tag = t;
        implicit = imp;
        
        if (o instanceof Primitive)
        {
            value = o;
        }
        else if (o instanceof Sequence)
        {
            value = o;
        }
        else if (o instanceof Context)
        {
            value = o;
        }
        else
        {
            throw new IllegalArgumentException("invalid ContextSpecific"+
                                               " underlying object");
        }
    }
    
    /**
     * Initializes the Context object.
     */
    public void init(ASN1Type type)
    {
        asn1Type = type;
        byteArray = null;
        implicit = false;
        tag = 0;
        value = null;
    }
    
    /**
     * Sets the value of the Cotext object as the given object. If the given
     * object is also a Context object, this method copies the content of
     * the given object to this object. If the given object is not suitable
     * for this operation, an IllegalArgumentException is thrown.
     */
    public void setValue(Object o)
    {
        if (o instanceof Context)
        {
            Context obj = (Context) o;
            tag = obj.getTag();
            value = (ASN1Object) (obj.getValue());
            implicit = obj.implicit();
        }
        else
        {
            throw new IllegalArgumentException("incompatible type");
        }
    }
    
    /**
     * Gets the value of the Cotext object.
     */
    public Object getValue()
    {
        return value;
    }
    
    /**
     * Gets the tag of the Cotext object.
     */
    public int getTag()
    {
        return tag;
    }
    
    /**
     * Gets the flag which indicates implicit/explicit tagging of the
     * Cotext object.
     */
    public boolean implicit()
    {
        return implicit;
    }
    
    /**
     * Returns whether two objects are the same.
    public boolean equals(Object o) {
    	if (!(o instanceof iss.security.asn1.Context)) {
    		return false;
    	}
    	if (!this.asn1Type.equals(o.asn1Type)) {
    		return false;
    	}
    	if (this.tag != o.tag) {
    		return false;
    	}
    	if (this.implicit != o.implicit) {
    		return false;
    	}
    	if (!this.value.equals(o.value)) {
    		return false;
    	}
    	return true;
}
     */
    
    /**
     * Gets the string description of the Cotext object.
     */
    public String toString()
    {
        String s = super.toString();
        s += "\n\tcontext specific tag [" + tag + "] implicit? " + implicit;
        s += "\n\tunderlying type: " + value.toString();
        return s;
    }
}
