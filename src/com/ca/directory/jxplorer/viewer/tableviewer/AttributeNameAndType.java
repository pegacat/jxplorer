package com.ca.directory.jxplorer.viewer.tableviewer;

/**
 *    TableAttributeEditor utility class; encapsulates the idea of
 *    a displayable object type name, that may or may not be mandatory
 *    (and hence displayed differently).
 */
 
public class AttributeNameAndType
{
    static final int NORMAL    = 0; 
    static final int MANDATORY = 1;         // whether attribute is required
    //static final int BINARY    = 2;         // future use
    static final int OPERATIONAL    = 3;         // display differently if it is an operational attribute


    private String value;
    private int type;
    
    /**
     * initialise with an object, take a backup, and set changed to false   
     */
    public AttributeNameAndType(String name, boolean isMandatory)
    {
        // trim ";binary" for display purposes... (Is this what people want I wonder?)
        if (name.endsWith(";binary"))
            name = name.substring(0, name.length()-7);

        value = name;
        type = isMandatory?1:0;
    }

    /**
     *
     * @param name the attribute ID
     * @param attributeType the type of the attribute
     */
    public AttributeNameAndType(String name, int attributeType)
    {
        value = name;
        type = attributeType;

    }

    public String getName() { return value; }
    
    public boolean isMandatory() { return type==MANDATORY; }

    public boolean isOperational() { return type==OPERATIONAL;}

    public String toString() { return value; }


}