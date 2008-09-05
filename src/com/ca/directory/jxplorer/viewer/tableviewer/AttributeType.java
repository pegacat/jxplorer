package com.ca.directory.jxplorer.viewer.tableviewer;

/**
 *    TableAttributeEditor utility class; encapsulates the idea of
 *    a displayable object type name, that may or may not be mandatory
 *    (and hence displayed differently).
 */
 
public class AttributeType
{
    static final int NORMAL    = 0; 
    static final int MANDATORY = 1;         // whether attribute is required
    static final int BINARY    = 2;         // future use


    public String value;
    public boolean mandatory;
    
    
    /**
     * initialise with an object, take a backup, and set changed to false   
     */
    public AttributeType(String name, boolean isMandatory) 
    {
        // trim ";binary" for display purposes... (Is this what people want I wonder?)
        if (name.endsWith(";binary"))
            name = name.substring(0, name.length()-7);

        value = name;
        mandatory = isMandatory;
    }
    
    public String getValue() { return value; }
    
    public boolean isMandatory() { return mandatory; }

    public String toString() { return value; }
    
}