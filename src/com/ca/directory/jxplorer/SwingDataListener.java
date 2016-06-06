package com.ca.directory.jxplorer;

import com.ca.directory.jxplorer.broker.DataQuery;

import java.awt.EventQueue;

/**
 * DataListener decorator.
 * Delivers dataReady events inside the AWT dispatcher thread
 * in case the listener intends to do any GUI stuff.
 * @author Robert Wruck
 */
public class SwingDataListener implements DataListener
{
	private final DataListener listener;
	
	/**
	 * Constructor
	 * @param listener The real listener
	 */
	public SwingDataListener(DataListener listener)
	{
		this.listener = listener;
	}
	
    private static final class DataReadyEvent implements Runnable
    {
    	private final DataListener listener;
    	private final DataQuery result;
    	
    	public DataReadyEvent(DataListener listener, DataQuery result)
    	{
    		this.listener = listener;
    		this.result = result;
    	}
    	
	    public void run()
	    {
	    	listener.dataReady(result);
	    }
    }
    
	//@Override
	public void dataReady(DataQuery result)
	{
       	EventQueue.invokeLater(new DataReadyEvent(listener, result));
	}

}
