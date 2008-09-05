package com.ca.directory.jxplorer.viewer.tableviewer;

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
 
public class AttributeValue implements editablebinary, editablestring
{
    public Object value;
    Object backup;
    boolean changed;
    boolean binary;
    String id;
    String[] options = null;

    private final static Logger log = Logger.getLogger(AttributeValue.class.getName());

    /**
     *    Whether this is a naming value.
     */
     
    boolean naming;
    
    /**
     * initialise with an object, take a backup, and set changed to false   
     */
    public AttributeValue(String ID, Object v) 
    { 
        id = ID;
        value = v; 
        backup = v; 
        changed=false; 
        naming=false;
        testBinary();
    }

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
        testBinary();
        if (binary==false)
        {
            value = getStringValue(); // special space handling...
        }
        changed = (value != backup);  // if value not the same as original, it's changed!

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

     
    /** 
     *
     */

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



    /**
     * reset the value back to what it was initialised with.
     */
    public void reset()
    {
        value = backup;
        changed = false;
        testBinary();
    }
    
    /** 
     *  sets the binary-ness of the attribute (this may be known externally
     *  from schema).
     */
    public void setBinary(boolean bin)
    {
        binary = bin;
        if (bin==true && value instanceof String)
            value = null;
    }
    
    /**
     *    Checks whether the object is binary (actually, it checks if
     *    the object <b>isn't</b> a String...)
     */
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

    /**
     *    Returns value as string.  NB. - returns null values and empty
     *    Strings as 1 character blanks, in order to get buggy Swing 
     *    printing junk to work.
     */
     
    public String toString() 
    { 
        if (value == null) return " ";            		// XXX Hack to get Broken swing printing working
		
		//TE: moved this line of code to before the following one...seems to fix the displaying of 
		//	  '(binary Value)' to empty attribute values when the Reset button is clicked (bug 2319).
		if (value.toString().length() == 0) return " ";	// XXX Hack to get Broken swing printing working
	
        if (binary) return CBIntText.get("(non string data)");

        return value.toString(); 
    }
    
    public String  getID()    { return id; }
    public boolean changed()  { return changed; }
    public Object  value()    { return value; }
    public Object  backup()   { return backup; } 
    public boolean isBinary() { return binary; }
}