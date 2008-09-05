
package com.ca.commons.security.asn1;

/**
 * This class represents ASN.1 constructed types. It is only used in this
 * package. It represents a sequence of components, where each component
 * is an ASN1Object. Components can be added to, removed from, and retrieved
 * from the Sequence object.
 *
 * @author who
 */
public class Sequence extends ASN1Object implements java.io.Serializable
{

    private ASN1Object [] component;
    private int count;
    private static int initSize = 2;
    
    /**
     * Default constructor.
     */
    public Sequence()
    {}
    
    /**
     * Initializes the Sequence object.
     */
    public void init(ASN1Type type)
    {
        asn1Type = type;
        byteArray = null;
        count = 0;
        component = new ASN1Object[initSize];
    }
    
    /**
     * Adds a component to the Sequence object at the end.
     */
    public void addComponent(ASN1Object comp)
    {
        addComponent(comp, count);
    }
    
    /**
     * Adds a component to the Sequence object at a given index.
     */
    public void addComponent(ASN1Object comp, int index)
    {
        if (index > count)
        {
            throw new IllegalArgumentException("illegal component index");
        }
        if (component.length <= count)
        {
        
            /* need to expand the component array */
            ASN1Object [] oldComponent = component;
            component = new ASN1Object[component.length+2];
            System.arraycopy(oldComponent, 0, component, 0,
                             oldComponent.length);
        }
        if (index < count)
        {
            System.arraycopy(component, index, component,
                             index + 1, count - index);
        }
        component[index] = comp;
        count++;
    }
    
    /**
     * Removes a component at a given index from the Sequence object.
     */
    public void removeComponent(ASN1Object comp, int index)
    {
        /* not implemented */
    }
    
    /**
     * Gets a component at a given index from the Sequence object.
     */
    public ASN1Object getComponent(int index)
    {
        if (index > count)
        {
            throw new IllegalArgumentException("illegal component index");
        }
        return component[index];
    }
    
    /**
     * Sets a component at a given index in the Sequence object.
     */
    public void setComponent(ASN1Object comp, int index)
    {
        if (index > count)
        {
            throw new IllegalArgumentException("illegal component index");
        }
        component[index] = comp;
    }
    
    /**
     * Returns the number of components in the Sequence object.
     */
    public int size()
    {
        return count;
    }
    
    /* *
     * Returns whether two objects are the same.
    public equals(Object o) {
    	if (!(o instanceof iss.security.asn1.Sequence)) {
    		return false;
    	}
    	if (!this.asn1Type.equals(o.asn1Type)) {
    		return false;
    	}
    	if (this.count != o.count) {
    		return false;
    	}
    
    	for (int i = 0; i < count; i++) {
    		if (!this.component[i].equals(o.component[i])) {
    			return false;
    		}
    	}
    	return true;
}
     */
    
    /**
     * A string representation of the Sequence object.
     */
    public String toString()
    {
        String result = super.toString();
        for (int i = 0; i < count; i++)
        {
            result += "\n\t[" + i + "]" + component[i].toString();
        }
        return result;
    }
}
