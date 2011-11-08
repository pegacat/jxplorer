package com.ca.directory.jxplorer;

import com.ca.commons.naming.DXEntry;
import com.ca.directory.jxplorer.broker.DataBrokerQueryInterface;

/**
 *    DataSink defines the interface used by objects that
 *    are consumers of information from a tree, such as display
 *    panels that show information about a data node, or the off-line
 *    data store used to view ldif files off-line.
 */

public interface DataSink
{
    /** 
     *    Displays information about a given tree node,
     *    expressed as a DXEntry (an attribute set and associated distinguished
     *    name of a node).  The method also provides a data Source
     *    which may be used to update the node.  This data Source 
     *    may be null for non-editable data.
     *
     *    @param entry the directory entry to display.  If null, indicates that a 
     *           blank or 'empty' entry should be displayed.
     *    @param ds the datasource used for data modification/schema access etc.
     *     May be null, in which case no schema checking/prompting will be done,
     *     and no editing will be possible.
     */
     
    public void displayEntry(DXEntry entry, DataBrokerQueryInterface ds);

    /**
     *    Indicates whether the editor can create a new entry, given a 
     *    unique name.
     */    
     
    public boolean canCreateEntry(); 
}