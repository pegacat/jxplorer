package com.ca.directory.jxplorer.event;

import java.util.EventObject;

/**
 * The object that implements the <code>JXplorerListener</code> interface
 * gets this <code>JXplorerEvent</code> when the event occurs. 
 *  
 * @see JXplorerListener
 *
 * @author Chris Betts
 */
 
public class JXplorerEvent extends EventObject {

    
    /**
     *    The user selected DN being passed back in this event.
     *    @see #getDN()
     */
     
    private String DN;
    private EventType type;


    /**
     * Constructs a <code>JXplorerEvent</code> object.
     *
     * @param source  the object that originated the event
     * @param type    the event type
     * @param DN      the DN
     */
    public JXplorerEvent(Object source, EventType type, String DN) 
    {
        super(source);
        this.type = type;
        this.DN = DN;
    }


    /**
     * Returns the current DN.
     * 
     * @return the selected DN (or null if none selected)
     */
    public String getDN() {
        return DN;
    }


    /**
     * Gets the type of event.
     *
     * @return the type
     */
    public EventType getEventType() 
    {
        return type;
    }

   
    /**
     * Defines the ENTERED, EXITED, and ACTIVATED event types, along
     * with their string representations, returned by toString().
     */
    public static final class EventType 
    {
        private String typeString;

        private EventType(String s) 
        {
            typeString = s;
        }

        /**
         * Entered type.
         */
        public static final EventType DNSELECTED = new EventType("DNSELECTED");

        /**
         * Converts the type to a string.
         *
         * @return the string
         */
        public String toString() 
        {
            return typeString;
        }

    }


}