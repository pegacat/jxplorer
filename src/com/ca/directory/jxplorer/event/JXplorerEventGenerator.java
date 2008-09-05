
package com.ca.directory.jxplorer.event;


/**
 *    This interface defines the functionality 
 *    required to fire a JXplorer event.  JXplorer events
 *    are used to communicate to programs that are using
 *    JXplorer as an embedded Swing component.
 *
 *    @see com.ca.directory.jxplorer.event.JXplorerEvent
 */
 
public interface JXplorerEventGenerator
{
    /**
     *    The method used to fire a JXplorerEvent
     */
     
    public void fireJXplorerEvent(JXplorerEvent e); 
}