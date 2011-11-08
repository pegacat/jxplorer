package com.ca.directory.jxplorer.broker;

import java.util.Vector;
import java.util.logging.Logger;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.ca.directory.jxplorer.*;
import com.ca.commons.cbutil.*;

/**
 *    The Stop Monitor interacts with the Brokers to cancel unwanted 
 *    data queries.  Since these data queries may well hang on 
 *    dud connections etc., they are set to self-destruct whenever
 *    they 'complete' a cancelled request.  Because of this the Stop Monitor starts
 *    up a replacement thread immediately after cancelling a particular
 *    request.
 */
 
public class StopMonitor
{
    /**
     *    The Stop Monitor starts with a list of 
     *    Brokers to keep track off. 
     */
    
    Vector watchList;  // A vector of Brokers to monitor
    Frame parent;      // a link to the 'owning' gui, to be passed to the local gui

    Vector watchingComponents; // A vector of components (e.g. gui 'stop buttons') linked to the StopMonitor state
    
    StopMonitorGUI monitor;

    static int threadID = 0;  // a unique ID used for thread debugging.
    
    private static Logger log = Logger.getLogger(StopMonitor.class.getName());

    // An internal use data holder - we need to keep associations
    // of threads and brokers, and threads/brokers/queries
    
    protected class QueryBroker
    {
        public DataBroker broker;
        public DataQuery query;
        public int id;
        
        QueryBroker(DataBroker b, DataQuery q)
        {
            broker = b;
            query  = q;
            id = q.id;
        }
    }

    /**
     *    Creates a stop monitor to look after a list of brokers and the (single)
     *    thread that runs each broker.  
     */
          
    public StopMonitor(DataBroker[] brokerList, Frame parent)
    {
        this.parent = parent;
    
        watchList = new Vector(brokerList.length);
    
        watchingComponents = new Vector(4);
    
        for (int i=0; i<brokerList.length; i++)
            add(brokerList[i]);    
    }

    /**
     *    Add a new broker to the stop monitor.
     */
     
    public void add(DataBroker b)
    {
        watchList.add(b);
        b.registerStopMonitor(this);
    }

    /**
     *    Get a list of all the queries outstanding, and which brokers they
     *    belong to.
     *    @return a vector of QueryBroker objects.
     */
     
    protected Vector getQueryBrokers()
    {
        int i, noBrokers = watchList.size();
        
        Vector doublets = new Vector(noBrokers*2);  // guess initial size
        
        // first get the queries currently being executed
        for (i=0; i<noBrokers; i++)
        {
            DataBroker broker = (DataBroker)watchList.get(i);
            DataQuery query = broker.getCurrent();
            if (query != null && !query.isCancelled())
                doublets.add(new QueryBroker(broker, query));
        }
        
        // now get all the queries waiting in queues.
        for (i=0; i<noBrokers; i++)
        {
            DataBroker broker = (DataBroker)watchList.get(i);
            Vector queries = broker.getRequestQueue();
            for (int j=0; j<queries.size(); j++)
                doublets.add(new QueryBroker(broker, (DataQuery)queries.get(j)));
        }
        
        return doublets;
    }

    /** 
    *    See if any of the monitored brokers has any outstanding requests.
    */
    public boolean queriesPending()
    {
        // now get all the queries waiting in queues.
        for (int i=0; i<watchList.size(); i++)
        {
            if (  ((DataBroker)watchList.get(i)).hasRequests() )
                return true;
        }            
        
        return false;
    }

    /**
     *    This registers a graphics component (such as a button) that
     *    should be enabled when there are queries pending, and disabled
     *    when there aren't.
     */
    public void addWatcher(Component c)
    {
        watchingComponents.add(c);
    }

    /**
     *    This works through all the registered 'watcher' components,
     *    and enables or disables them depending on whether there
     *    are queries waiting to be cancelled or not.
     */
     
    public void updateWatchers()
    {
        boolean enableStatus = queriesPending();
        
        for (int i=0; i<watchingComponents.size(); i++)
        {
            Component c = (Component)watchingComponents.get(i);
            if (c.isEnabled() != enableStatus) 
            {
                c.setEnabled(enableStatus);
                c.repaint();   
            }                
        }    
        
    }


     // PROG NOTE - this is intended to be run from the newly 
     //             created StopMonitorPanel thread.
     
    /**
     *    This cancels a particular query in a particular broker's queue.
     *    If the query is already in progress it marks the query as cancelled
     *    which will eventually lead to the death of the thread executing that
     *    query, and starts another thread to take over...
     */
      
    protected void cancelQuery(QueryBroker pair)
    {
        if (pair == null) return;
    
        DataQuery query = pair.query;
        DataBroker broker = pair.broker;
        
        //PROG NOTE - danger of contention here.  Remember that a broker thread
        //            may briefly have a lock on the requestQuery queue
        
        synchronized(this)    
        {        
            if (query.isRunning())  // badness - maybe a connection has hung or something.  We have
            {                       // kill the query and start a new thread...
                query.cancel();
                
                new Thread(broker, "restarted thread for: " + (threadID++) + " " + broker.getClass()).start();
            }
            else    // nice 'n simple! - dump it from the array of queries and our work is done!
            {
                broker.removeQuery(query);
            }
        }            
    }

    /**
     *    This prompts the stop monitor to create a GUI showing the user the possible
     *    queries available to be stopped.
     */
     
    public void show()
    {
        Vector QBs = getQueryBrokers();

        if (QBs.size() == 0)
        {
             JOptionPane.showMessageDialog(parent,
             CBIntText.get("There are no outstanding directory queries to cancel."),
             CBIntText.get("Nothing to do."), JOptionPane.INFORMATION_MESSAGE);
             return;   
        }
    
        if (monitor!=null) 
        {
            monitor.update();
            if (monitor.singleDelete() == false)            
                monitor.setVisible(true);
        }
        else
        {        
            monitor = new StopMonitorGUI(this, QBs);
            monitor.setSize(300, 400);
            CBUtility.center(monitor, parent);
    
            // We need to stick the monitor in a separate thread, because the action
            // of deleting a query may itself hang...        
            Thread buttonMonitor = new Thread(monitor, "Stop Monitor Thread");
            buttonMonitor.start();        
            
            if (monitor.singleDelete() == false)            
                monitor.setVisible(true);
        }            
    }



    //
    //    START StopMonitorGUI class
    //

    /**
     *    This class is used to display a gui list of cancel-able queries to the user.
     *    The main StopMonitor class does the hard work.
     *    The gui implements a runnable thread that is notified when the user 
     *    hits a button, and calls (from the separate thread) the main StopMonitor
     *    class to delete queries.  The thread is ended when the gui is closed.
     */
     
    class StopMonitorGUI extends JDialog implements Runnable
    {
        JList list;
        DefaultListModel model;
        StopMonitor myMonitor;
        boolean     finished = false;     // whether the stop monitor has been closed by the user
        boolean     selected = false;     // whether a selection has been made by the user
        QueryBroker selectedQuery = null; // which data query has been selected.
        
        StopMonitorGUI(StopMonitor myMonitor, Vector queryBrokers)
        {
            super(myMonitor.parent);
            this.myMonitor = myMonitor;
            
            model = new DefaultListModel();
            list = new JList(model);
            for (int i=0; i<queryBrokers.size(); i++)
                model.addElement(queryBrokers.get(i));
                            
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setSelectionModel(new CBSingleSelectionModel(list));   //TE: tries to ensure that the selected item is visible.
            list.setCellRenderer(new StopMonitorListRenderer());
            list.setVisibleRowCount(6);
            list.setSelectedIndex(0);
            
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.getViewport().setView(list);            
            getContentPane().add("Center", scrollPane);
            
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            
            CBButton ok, cancel, help;
            buttonPanel.add(ok = new CBButton(CBIntText.get("Delete Query"), CBIntText.get("Delete the current query.")));
            buttonPanel.add(cancel = new CBButton(CBIntText.get("Exit"), CBIntText.get("Exit this window.")));
            buttonPanel.add(help = new CBButton(CBIntText.get("Help"), CBIntText.get("Open the help.")));
        	CBHelpSystem.useDefaultHelp(help, HelpIDs.ABORT);			//TE: direct link to the topic in the help.
            getContentPane().add("South", buttonPanel);
            
            ok.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    cancelSelectedQuery();    // get the selected query and cancel it.
                }
            });
            
            cancel.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    setVisible(false);
                }
            });

            list.addMouseListener(new MouseAdapter()
            {
                 public void mouseClicked(MouseEvent e) 
                 {
                     if (e.getClickCount() == 2) cancelSelectedQuery();  // get the selected query and cancel it.
                 }
            });                         
        }    
		
		

        /**
         *    Updates the list of queries/DataBroker pairs, maintaining the selection (if possible)
         */
        protected void update()
        {
            int selectedID = -1;
            
            Vector queryBrokers = myMonitor.getQueryBrokers();
            
            QueryBroker selectedQuery = (QueryBroker)list.getSelectedValue(); // cache currently selected query
            if (selectedQuery != null)
                selectedID = selectedQuery.id;
            
            model.removeAllElements();
            
            for (int i=0; i<queryBrokers.size(); i++)
            {
                QueryBroker newQuery = (QueryBroker)queryBrokers.get(i);
                model.addElement(newQuery);
                if (selectedID == newQuery.id)                
                    list.setSelectedValue(newQuery, true);      // re-assert selection
            }    
                
        }



        /**
         *    Called when the user has selected an item, and either double clicked
         *    or hit 'o.k.'.
         */
         
        protected void cancelSelectedQuery()
        {
            synchronized(this)
            {
                if (list.getSelectedIndex() == -1)
                {
                     JOptionPane.showMessageDialog(this.getContentPane(),
                     CBIntText.get("You must select a query to stop from the list") + "\n" +
                     CBIntText.get("otherwise you can just use the 'cancel' button."),
                     CBIntText.get("Stop Monitor Help"), JOptionPane.INFORMATION_MESSAGE);
                }
                else
                {
                    selectedQuery = (QueryBroker)list.getSelectedValue(); 
                    selected = true;
                    model.removeElement(selectedQuery);
                    notify();
                }                    
            }
        }
        
        public void setVisible(boolean status)
        {
            super.setVisible(status);
            synchronized(this)
            {
                if (status == true) notify(); // wake up status polling thread.           
            }    
        }

        /** 
        *    Checks if there is only a single query to delete.  If there is, it
        *    deletes it and returns true.  Otherwise it sets the selected index
        *    to zero, and returns false.
        *    @return whether there was only a single query, now deleted.
        */
        public boolean singleDelete()
        {
            if (model.size() == 0) return false;  // *huh*?  shouldn't ever happen.
            list.setSelectedIndex(0);        
            if (model.size() == 1)
            {
                cancelSelectedQuery();
                return true;
            }
            return false;   
        }
        
        /** 
         *    Do thread magic.  This loops, waiting until it is
         *    notified that selectedQuery has changed.  It will
         *    then attempt to delete that query.  While the monitor
         *    is visible it should also poll the queue status every
         *    250 ms for changes.
         */
         
        public void run()
        {
            while (!finished)
            {
                try
                {
                    synchronized(this)
                    {
                        if (selectedQuery != null)
                        {
                            myMonitor.cancelQuery(selectedQuery); // cancel query
                            selectedQuery = null;                 // and reset 'query to cancel' variable.
                        }
                        
                        if (isVisible())    // we want to keep polling the task list while active.
                            wait(250);
                        else                                
                            wait();
                            
                        update();                            
                    }                        
                }
                catch (InterruptedException e)
                {
                    CBUtility.error("Exception in stop monitor: ", e);
                }                    
            }
        }
    }



    //
    //    END StopMonitorGUI class
    //


    //
    //    START StopMonitorListRenderer class
    //


    /**
     *    A quicky cell renderer to allow us to display active and pending
     *    queries in different colours, and to display the text of a
     *    QueryBroker object. 
     */
     
    class StopMonitorListRenderer extends JLabel implements ListCellRenderer
    {
        Color highlight = new Color(0,0,128);
        Color active    = new Color(128,0,0);
        Color pending   = new Color(0,128,0);
        Color cancelled = new Color(64,64,64);
        
        StopMonitorListRenderer() 
        {
            setOpaque(true);
        }     
    
        public Component getListCellRendererComponent(JList list, Object value, int index, 
                                                        boolean isSelected, boolean cellHasFocus)
        {
            if (value instanceof QueryBroker == false)  // should never happen :-)
            {
                log.warning("Rendering error in StopMonitor"); setText("error"); return this;
            }
            
            if (index == -1)
            {
                int selected = list.getSelectedIndex();
                if (selected == -1)
                    return this;
                else
                    index = selected;
            }

            DataQuery item = ((QueryBroker)value).query;            
            
            if (item == null)    // shouldn't happen
            {
                setBackground(Color.white);
                setForeground(cancelled);
                setText("<deleted>"); 
                return this;
            }    
            
            setText(item.toString());
            Color back = Color.white;
            Color fore = Color.black; 
         
            if (item.isCancelled())    
                fore = cancelled;
            else if (item.isRunning())
                fore = active;
            else
                fore = pending;
                
            if (isSelected)
            {
                setBackground(highlight);
                setForeground(Color.white);
            }
            else
            {
                setBackground(back);
                setForeground(fore);
            }                
            return this;
        }
    }

    //
    //    STOP StopMonitorListRenderer class
    //
}    