package com.ca.directory.jxplorer.editor;

/**
 *    This interface defines the functionality required of
 *    an object that can be edited by an AbstractBinaryEditor
 *    and its ilk.
 */
public interface editablebinary
{
    /**
     *    sets the value of the editable object.
     *    @param bytes an array of bytes that may be edited later.
     */
    public void setValue(byte[] bytes);
    
    /**
     *    gets the value of the edited object.
     *    @return the array of edited bytes.
     */
    public byte[] getValue();
}