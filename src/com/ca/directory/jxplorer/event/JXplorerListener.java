package com.ca.directory.jxplorer.event;

import java.util.EventListener;

/**
 * The listener interface for receiving JXplorer events. 
 * The class that is interested in processing an JXplorer event
 * implements this interface, and the object created with that
 * class is registered with a component, using the component's
 * <code>addJXplorerListener</code> method. When the JXplorer event
 * occurs, that object's <code>JXplorerDNSelected</code> method is
 * invoked.
 *
 * @see JXplorerEvent
 *
 * @author Chris Betts
 */
 
public interface JXplorerListener extends EventListener {

    /**
     * Invoked when an JXplorer occurs.
     */
    public void JXplorerDNSelected(JXplorerEvent e);

}