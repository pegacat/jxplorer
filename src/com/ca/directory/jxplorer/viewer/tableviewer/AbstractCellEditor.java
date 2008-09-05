package com.ca.directory.jxplorer.viewer.tableviewer;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;

     
abstract public class AbstractCellEditor implements TableCellEditor
{
    protected EventListenerList listenerList =  new EventListenerList();
    protected Object value;
    protected ChangeEvent changeEvent = null;
    protected int clickCountToStart = 1;

//
//  Handle the event listener bookkeeping
//
    // implements javax.swing.CellEditor
    public void addCellEditorListener(CellEditorListener l) 
    {
        listenerList.add(CellEditorListener.class, l);
    }

    // implements javax.swing.CellEditor
    public void removeCellEditorListener(CellEditorListener l) 
    {
        listenerList.remove(CellEditorListener.class, l);
    }

    /*
     * Notify all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is lazily created using the parameters passed into 
     * the fire method.
     * @see EventListenerList
     */
    protected void fireEditingStopped() 
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) 
        {
            if (listeners[i]==CellEditorListener.class) 
            {
                // Lazily create the event:
                if (changeEvent == null)
                    changeEvent = new ChangeEvent(this);
                ((CellEditorListener)listeners[i+1]).editingStopped(changeEvent);
            }           
        }
    }

    /*
     * Notify all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is lazily created using the parameters passed into 
     * the fire method.
     * @see EventListenerList
     */
    protected void fireEditingCanceled() 
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) 
        {
            if (listeners[i]==CellEditorListener.class) 
            {
                // Lazily create the event:
                if (changeEvent == null)
                changeEvent = new ChangeEvent(this);
                ((CellEditorListener)listeners[i+1]).editingCanceled(changeEvent);
            }           
        }
    }


    public Object getCellEditorValue()
    {
        return value;
    }
    
    public void setCellEditorValue(Object value)
    {
        this.value = value;
    }    

 
    /**
     * Specifies the number of clicks needed to start editing.
     *
     * @param count  an int specifying the number of clicks needed to start editing
     * @see #getClickCountToStart
     */
    public void setClickCountToStart(int count) 
    {
        clickCountToStart = count;
    }

    /**
     *  ClickCountToStart controls the number of clicks required to start
     *  editing.
     */
    public int getClickCountToStart() 
    {
        return clickCountToStart;
    }
    
    // implements javax.swing.CellEditor
    public boolean isCellEditable(EventObject anEvent) 
    {
        if (anEvent instanceof MouseEvent) 
        { 
            return ((MouseEvent)anEvent).getClickCount() >= clickCountToStart;
        }
        return true;
    }
 
   
    // implements javax.swing.CellEditor
    public boolean stopCellEditing() 
    {
        fireEditingStopped();
        return true;
    }

    // implements javax.swing.CellEditor
    public void cancelCellEditing() 
    {
        fireEditingCanceled();
    }

   // implements javax.swing.CellEditor
    public boolean shouldSelectCell(EventObject anEvent) 
    {
        return true;
    }

}