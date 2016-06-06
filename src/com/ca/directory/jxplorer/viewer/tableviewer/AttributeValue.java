package com.ca.directory.jxplorer.viewer.tableviewer;

import com.ca.commons.naming.DXAttribute;
import com.ca.directory.jxplorer.editor.*;
import com.ca.commons.cbutil.CBIntText;

import java.util.logging.Logger;


/**
 *    TableAttributeEditor utility class; encapsulates the idea of
 *    an editable (String) attribute value, as displayed in a table.
 *    It also checks to see if an attribute is Binary, and sets a
 *    'binary' flag as a warning to cell editors to use special 
 *    handling.
 */

// there is confusion here with handling non-string attributes, and some unnecessary double handling.  I'm
// refactoring the class to use an internal DXAttribute object to simplify the logic, and take advantage
// of the DXAttribute built in handling of non-strings.

public class AttributeValue implements editablebinary, editablestring
{
    /*
     *  The base attribute the value comes from.  Gives us a way of consistently knowing the ID and the schema
     */
    DXAttribute baseAttribute;
    public Object value;
    Object backup;
    //boolean binary;
    String[] options = null;

    boolean editable = true;

    private final static Logger log = Logger.getLogger(AttributeValue.class.getName());

    /**
     *    Whether this is a naming value.
     */
     
    boolean naming;

    /**
     * initialise with a DXAttribute object, take a backup, and set changed to false
     *
     * @param att the root attribute (from where the value originally came = but this is not rechecked...)
     * @param v the value to display; may be null
     */
    public AttributeValue(DXAttribute att, Object v)
    {
        baseAttribute = att;
        value = v;
        backup = v;
        naming=false;

        if (att.hasOptions())
            setOptions(att.getOptions());
    }

    /**
     * Initialise with a DXAttribute object as above, but also set editability.
     * (Mainly intended to set operational attribute values to being non-editable) 
     *
     * @param att
     * @param v
     * @param editable
     */
    public AttributeValue(DXAttribute att, Object v, boolean editable)
    {
        this(att, v);
        this.editable = editable;
    }

    public boolean isEditable() { return editable; }

    public DXAttribute getBaseAttribute() {return baseAttribute;}


    /**
     *  sets whether this value is a naming value (e.g. the 'Smith' in cn=Smith).
     */
     
    public void setNamingStatus(boolean state) 
    {
        naming = state;
    }

    /**
     *    Returns whether this value is a naming value.
     */
     
    public boolean isNaming()
    {
        return naming;
    } 

    /**
     *    Whether the Attribute Value has been given a set of
     *    suggested values.
     */  
        
    public boolean hasOptions() { return (options!=null); } 
    
    /**
     *    adds a list of suggested options to the Attribute Value...
     */
     
    public void setOptions(String[] ops) 
    { 
        options = ops; 
    } 

    /**
     *    adds a list of suggested options to the Attribute Value...
     */
     
    public String[] getOptions() { return options; } 

    
    /**
     * update object 
     */
    public void update(Object data)
    {
        value = data;
        if (baseAttribute.isString())
            value = getStringValue(); // special space handling...
    }
    
    /**
     *    synonym for update, used for EditableBinary interface...
     */
    public void setValue(byte[] b)
    {
        update(b);
    }
    
    
    /**
     *    synonym for update, used for EditableString interface...
     *
     */
    public void setStringValue(String b)
    {
        update(b);
    }
    
    
    public byte[] getValue()
    {
        if (isNonStringData())
        {
            if (value instanceof String)  // may happen if it's been initialised with an empty string.
            {
                log.warning("warning - Attribute Value " + value + " mis represented as byte array data");
                return null;		//TE this was commented out, but it seems to fix the problem of clicking reset which disables the binary fields until submit is clicked.
            }    
            else    
                return ((byte[]) value);
        }            
        else
            return null; // XXX I wonder if this should return the string as a byte array? - CB'11
    }

     
    /** 
     * returns a displayable string; or an empty string for a null or eroneous value
     */

    public String getStringValue()
    {
        return toString();
        /*
        if (isNonStringData())
            return "";        // was null
        else
        {
            String val = ((String) value);
            if (naming) // special handling for spaces - bug 4886
            {
                int len = val.length();
                val = val.trim();
                if (val.length() == 0 && len>0)
                    val = " ";
            }
            return val;
        }
        */
    }



    /**
     * reset the value back to what it was initialised with.
     */
    public void reset()
    {
        value = backup;
    }
    
    public boolean isEmpty()
    { 
        if (value==null) return true;
        if (isNonStringData())
        {
            if (value instanceof byte[])
                if (((byte[])value).length == 0)
                    return true;    // XXX don't believe this code ever runs...

            return false;
        }    
        
        return (value.toString().length()==0);
    }

    /**
     *    Returns value as string.  NB. - returns null values and empty
     *    Strings as 1 character blanks, in order to get buggy Swing 
     *    printing junk to work.
     */
    //TODO   : remove getStringValue()?
     
    public String toString() 
    {

        if (value == null) return " ";            		// XXX Hack to get Broken swing printing working
		
		//TE: moved this line of code to before the following one...seems to fix the displaying of 
		//	  '(binary Value)' to empty attribute values when the Reset button is clicked (bug 2319).
		if (value.toString().length() == 0) return " ";	// XXX Hack to get Broken swing printing working
	
        if (isNonStringData())
            return CBIntText.get("(non string data)");

        return value.toString(); 
    }

    public String  getID()    { return baseAttribute.getID(); }

    public boolean changed()
    {
        boolean changed = false;

        if (value == null)
        {
            changed = !(backup == null || "".equals(backup)); // handle empty strings and nulls...
        }
        else if (isNonStringData()==false && value != null)
        {
            changed = (!value.equals(backup));  // do a string compare
        }
        else
            changed =  (value != backup);  // if value not the same as original, it's changed!
        
        //if (changed)
            //System.out.println("change in value: " + getID() + " from: " + backup + " to " + value);

        return changed;
    }

    public Object  value()    { return value; }

    public Object  backup()   { return backup; }

    public boolean isNonStringData() { return !baseAttribute.isString(); }
}


/*
public class AttributeValue implements editablebinary, editablestring
{
    public Object value;
    Object backup;
    boolean binary;
    String id;
    String[] options = null;

    private final static Logger log = Logger.getLogger(AttributeValue.class.getName());

    boolean naming;

    public AttributeValue(String ID, Object v)
    {
        if (ID.equalsIgnoreCase("userPassword"))
            System.out.println("setting it up...: ");
        id = ID;
        value = v;
        backup = v;
        naming=false;
        testBinary();
    }

    public AttributeValue(String ID)
    {
        if (ID.equalsIgnoreCase("userPassword"))
            System.out.println("setting it up...: ");

        id = ID;
        naming=false;
    }



    public void setNamingStatus(boolean state)
    {
        naming = state;
    }


    public boolean isNaming()
    {
        return naming;
    }

    public boolean hasOptions() { return (options!=null); }


    public void setOptions(String[] ops)
    {
        options = ops;
    }


    public String[] getOptions() { return options; }


    public void update(Object data)
    {
        if (id.equalsIgnoreCase("userPassword"))
            System.out.println("ello ello ello!");
        value = data;
        testBinary();
        if (binary==false)
        {
            value = getStringValue(); // special space handling...
        }
    }

    public void setValue(byte[] b)
    {
        update(b);
    }

    public void setStringValue(String b)
    {
        update(b);
    }


    public byte[] getValue()
    {
        if (binary)
        {
            if (value instanceof String)  // may happen if it's been initialised with an empty string.
            {
                log.warning("warning - Attribute Value " + value + " mis represented as byte array data");
                return null;		//TE this was commented out, but it seems to fix the problem of clicking reset which disables the binary fields until submit is clicked.
            }
            else
                return ((byte[]) value);
        }
        else
            return null;
    }



    public String getStringValue()
    {
        if (binary == false)
        {
            String val = ((String) value);
            if (naming) // special handling for spaces - bug 4886
            {
                int len = val.length();
                val = val.trim();
                if (val.length() == 0 && len>0)
                    val = " ";
            }
            return val;
        }
        else
            return null;
    }



    public void reset()
    {
        value = backup;
        testBinary();
    }


    public void setBinary(boolean bin)
    {
        binary = bin;
        //if (bin==true && value instanceof String)
        //    value = null;
    }


    public boolean testBinary()
    {
        if (binary==true) return true;  // once it's set to binary, it stays that way.

        binary = (value==null)?false:((value instanceof String)?false:true);
        return binary;
    }

    public boolean isEmpty()
    {
        if (value==null) return true;
        if (binary)
        {
            if (value instanceof byte[])
                if (((byte[])value).length == 0)
                    return true;    // XXX don't believe this code ever runs...

            return false;
        }

        return (value.toString().length()==0);
    }



    public String toString()
    {
        if (id.equalsIgnoreCase("userPassword"))
            System.out.println("printing out stuff...");

        if (value == null) return " ";            		// XXX Hack to get Broken swing printing working

		//TE: moved this line of code to before the following one...seems to fix the displaying of
		//	  '(binary Value)' to empty attribute values when the Reset button is clicked (bug 2319).
		if (value.toString().length() == 0) return " ";	// XXX Hack to get Broken swing printing working

        if (binary)
            return CBIntText.get("(non string data)");

        return value.toString();
    }

    public String  getID()    { return id; }
    public boolean changed()
    {
        boolean changed = false;

        if (value == null)
        {
            changed = !(backup == null || "".equals(backup)); // handle empty strings and nulls...
        }
        else if (binary==false && value != null)
        {
            changed = (!value.equals(backup));  // do a string compare
        }
        else
            changed =  (value != backup);  // if value not the same as original, it's changed!

        if (changed)
            System.out.println("change in value: " + getID() + " from: " + backup + " to " + value);

        return changed;
    }

    public Object  value()    { return value; }
    public Object  backup()   { return backup; }
    public boolean isBinary() { return binary; }
}
*/