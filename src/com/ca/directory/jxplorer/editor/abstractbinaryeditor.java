package com.ca.directory.jxplorer.editor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;



/**
 *    <p>This interface defines the basis for BinaryEditors.
 *    BinaryEditors implementing this interface, that 
 *    have the name [object class]editor.class, will be
 *    dynamically loaded, providing that it implements
 *    (in addition to this interface) a no-argument constructor.</p>
 *
 *    <p><b>IMPORTANT</b></p>
 *
 *    <p>Editors <b>MUST</b> have completely lower case class names, or they will not be able to be matched
 *    with the corresponding attribute name by JXplorer.  (This is because attribute names are in arbitrary
 *    case, and there is no way to find or load a class in a case insensitive way using the standard java
 *    class loader.)</p>
 */
public interface abstractbinaryeditor
{
    /**
     *    the main function; takes an EditableBinary object that
     *    can then be, well, edited.  
     *
     *    @param editMe the array of bytes to be modified.
     */
     
    public void setValue(editablebinary editMe);
    
    /**
     *    An optional utility ftn that returns the edited object...
     *    may not be used in normal operation since setValue(...)
     *    can update the EditableBinary object.
     */
     
//    public EditableBinary getValue();
}