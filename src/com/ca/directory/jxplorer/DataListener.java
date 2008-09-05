package com.ca.directory.jxplorer;

import com.ca.commons.naming.*;

/**
 *    This interface handles 
 *
 *    There are two main ways to retrieve data from a DataQuery
 *    in a thread friendly manner.  The first is to poll the
 *    'ready()' method until it returns true.  The second method
 *    is to register a data listener that will be notified when
 *    the result is complete. 
 */

//XXX is this correct, or do we want a 'DataEvent' that wraps the
// DataQuery in some way, maybe providing a 'source'?

public interface DataListener
{
    public void dataReady(DataQuery result);
}