package com.ca.directory.jxplorer.editor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 *    This interface defines the basis for EditableString.
 *    EditableString implementing this interface, that 
 *    have the name [object class]Editor.class, will be
 *    dynamically loaded, providing that it implements
 *    (in addition to this interface) a no-argument constructor..
 *
 *  *    <p><b>IMPORTANT</b></p>
 *
 *    <p>Editors <b>MUST</b> have completely lower case class names, or they will not be able to be matched
 *    with the corresponding attribute name by JXplorer.  (This is because attribute names are in arbitrary
 *    case, and there is no way to find or load a class in a case insensitive way using the standard java
 *    class loader.)</p>
 *
 *    @author Trudi  
 */
public interface abstractstringeditor
{
    /**
     *    The main function; takes an EditableString object that
     *    can then be edited.  
     *
     *    @param editMe to be modified.
     *
     */
     
    public void setStringValue(editablestring editMe);
}