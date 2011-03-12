package com.ca.directory.jxplorer.editor;

/**
 *    This interface defines the functionality required of
 *    an object that can be edited by an AbstractEditableString.
 *    @author Trudi 
 */
 
public interface editablestring
{
    /**
     *    te: sets the value of the editable object.
     *    @param attString that may be edited later.
     *
     */
    public void setStringValue(String attString);
    
    /**
     *    te: gets the value of the edited object.
     *    @return the edited String.
     *
     */
    public String getStringValue();
}