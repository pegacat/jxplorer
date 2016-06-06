package com.ca.directory.jxplorer.search;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.naming.*;
import javax.naming.directory.*;

import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.*;
import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.broker.*;
import com.ca.directory.jxplorer.tree.SmartTree;

/**
*	This class is currently called from the Search GUI when the user has requested that certain
*	attributes are returned in the search.  
*	<p>
*	The way it is <i>intended</i> to work is the <code>SearchGUI</code> creates a <code>ReturnAttributesDisplay</code> 
*	object by calling the constructor.  The constructor does nothing more than registers two global variables.  
*	The creator of the object is expected to register a DataBrokerQueryInterface via the <code>registerDataSource</code> method
*	so that this object is added to it as a <code>DataListener</code>. 
*	<p>
*	When the search returns, all the <code>DataListener</code> objects are notified with the results of the search 
*	(hense this class) via the <code>dataReady</code> method.  The <code>dataReady</code> method calls the 
*	<code>displaySearchResult</code> method which extracts the attribute values from the search results.
*	It is this method that creates a static <code>ReturnAttributesGUI</code> which displays the results in a 
*	table.  Only one copy of <code>ReturnAttributesGUI</code> is initiated.  If one is already open when a
*	search result is received we just reset the data in it's table.
*/
public class ReturnAttributesDisplay 
	implements DataListener
{
    private static Logger log = Logger.getLogger(ReturnAttributesDisplay.class.getName());

    /**
     * The owning frame.
     */
	private JXplorerBrowser browser;

    /**
     * Where we get the search results from.
     */
	private DataBrokerQueryInterface dataSource = null;

    /**
     * Returned attribute values (populates the table).
     */
	private Object[][] tableData = null;

    /**
     * Return attributes (is used for table header).
     */
	private String[] tableHeader = null;

   /**
	*  	Because this isn't modal, the user might 'lose' it behind the 
	*  	main gui or something.  Since we only ever want one of these,
	*  	we'll simply reuse a single object, and make sure it's visible
	*  	when we need it. 
	*/
	private static 	ReturnAttributesGUI gui = null;

    /**
    *	Constructor that does nothing more than register the params as global 
	*	variables.
	*	@param jx the owing frame.
	*	@param tableHeader contains a list of attributes that the user wants returned (is used as the table header).
	*/
	public ReturnAttributesDisplay(JXplorerBrowser jx, String[] tableHeader)
	{			
		this.tableHeader = tableHeader;		  
		this.browser = jx;
	}

   /**
    *	Registers a given DataBrokerQueryInterface and notifies it that this class is
	*	a DataListener.
	*	@param ds the DataBrokerQueryInterface to be registered.
	*/
	public void registerDataSource(DataBrokerQueryInterface ds)
	{
		dataSource = ds;
		dataSource.addDataListener(this);
	}

   /**
	*  	This is the data listener interface - this method is called when a (Search) data query is finished
	*  	by a DataBroker.
	*	@param result the search result.
	*/
    public void dataReady(com.ca.directory.jxplorer.broker.DataQuery result)
    {
        int type = result.getType();

        if (result.hasException())
        {
            CBUtility.error("Unable to perform " + result.getTypeString() + " operation.", result.getException());
            return;
        }
        else
        {
            // Make sure we are dealing with a search result...
            if (type == com.ca.directory.jxplorer.broker.DataQuery.SEARCH)
				displaySearchResult(result); 
        }
    }	
		
   /**
   	*	This method basically takes the search result and extracts the attribute
	*	values then populates a two-dimensional array with these values.  The
	*	array is used to initiate the JTable that is used in the gui.  
	*	@param result the search result.
	*/
    protected void displaySearchResult(com.ca.directory.jxplorer.broker.DataQuery result)
    {	
		HashMap map = new HashMap(0);		
		
        try		
        {	
			DXNamingEnumeration myResults = result.getEnumeration();
			
			Object[] searchResults = myResults.toArray();

			int rows = searchResults.length;
			int cols = tableHeader.length;

			if (rows == 0)
			{
                // Nothing returned in the search so init the array with no values...
				tableData = new Object[0][0];
			}
			else
			{
                // Add the attribute value to the array - if no value exists add an empty string...
				tableData = new Object[rows][cols];

				String dn = "";
                Attribute att = null;
				String header = "";

                // Keep track of the position of the [DN] header.  It needs to be replaced with something more useful...
                int includeDNPos = -1;

				for (int i = 0; i < rows; i++)
				{
					for (int j = 0; j < cols; j++)
					{
						Attributes atts = ((SearchResult) searchResults[i]).getAttributes();

                        // Get the DN...
						dn = ((SearchResult) searchResults[i]).getName();

                        // Get the attribute from the results...
                        att = atts.get(tableHeader[j]);

                        // An attribute should look something like 'cn: Fred' so we need to substring
                        // it to just 'Fred' in the process of adding it to the array.  However, we first
                        // need to check that the header item isn't the include DN flage: [DN].  If it is
                        // the value will be the DN.
                        if(tableHeader[j].equalsIgnoreCase(ReturnAttributesDialog.INCLUDE_DN))
                        {
                            includeDNPos = j;
                            tableData[i][j] = dn;
                        }
                        else if(att == null)
                        {
                            tableData[i][j] = "";
                        }
                        else
                        {
                            header = att.toString();
                            tableData[i][j] = header.substring(header.indexOf(":") + 2);
                        }
					}

                    // Add the row number and the DN of the entry it represents...
					map.put(String.valueOf(i), dn);
				}

                // Only after we have finished processing the list, can we change the name of the DN header if needed...
                if(includeDNPos > -1)
                    tableHeader[includeDNPos] = "DN";
			}
			
			if (tableData==null || tableHeader==null)
			{
				log.warning("Problem retrieving the search results for the Return Attributes display");
				return;				
			}

			if (gui == null)			
			{
                // Only create one gui...
				gui = new ReturnAttributesGUI(tableData, tableHeader, rows, map);
			}
			else
			{
                // If one exists just set the data...
				gui.setTableData(tableData, tableHeader, rows, map);
			    gui.setVisible(true); 		
			}		
	    }
	    catch (NamingException e)
	    {
	        result.setException(e);  	// XXX set the exception on the result object, let someone else handle it.
	    }
	    catch (ClassCastException ee)
	    {
	    	log.log(Level.WARNING, "Casting problem in return attribute display ", ee);
	    }
		
		// Because we make a new ReturnAttributeDisplay each time a search is done - but only keep
		// one GUI floating around...make sure the data listener is removed after the search
		// result has been processed - otherwise the gui will try to display all previous search
		// results and everything falls over in a heap.  In otherwords - DON'T remove this...
		dataSource.removeDataListener(this);
    }	

   /**
    *	Class that sets up a dialog that displays the given data in a table.  The dialog
	*	has a print button that prints this table.
	*	@author Trudi	
	*/
	class ReturnAttributesGUI extends JDialog implements Printable
	{
		JTable 					table;
		DefaultTableModel		model;
		CBTableSorter 			sorter;
		CBButton					btnPrint, btnClose, btnHelp, btnSave;
		CBPanel					bottomPanel;
		CBPanel					topPanel;
		CBPanel					display;
		JScrollPane 			scrollPane;
		JFileChooser 			chooser;
		HashMap					map;
		
	   /*
		*    A temporary copy of a component that is to be printed, used by the
		*    print thread mechanism to pass an editor image around.
		*/
	    private Component printComponent = null;
			
			
	   /**
	    *	Sets up a dialog that displays the given data in a table.  The dialog
		*	has a print button that prints this table.
		*	@param tableData the data that is to be displayed in the tabel i.e. Object[rows][columns].
		*	@param tableHeader an array holding the attribute names that are used for the header of 
		*		each column (cn, sn etc).
		*	@param num the number of search results returned (this is just used in the title bar).
		*	@param map a store for the DN of a particular row number.		
		*/
		public ReturnAttributesGUI(Object[][] tableData, String[] tableHeader, int num, HashMap map)
		{
			super(browser, CBIntText.get(String.valueOf(num) + " " + CBIntText.get("Search Results")), false);
					
			this.map = map;
					
			model = new DefaultTableModel(tableData, tableHeader);

            // For sorting via clicking on table headers...
			sorter = new CBTableSorter(model);

            // Requires a click/shift + click...
			table = new JTable(sorter);
			sorter.addMouseListenerToHeaderInTable(table);

            // Adds a mouse event listener to the table...
			addMouseListenerToTable();
			
	        // Create the scroll pane and add the table to it...
	        scrollPane = new JScrollPane(table);		
						
			// Main display (control & table holder)...
			display = new CBPanel();

			// Top panel (table holder)...
			topPanel = new CBPanel();
			topPanel.makeHeavy();
			topPanel.addln(scrollPane);
			
			display.makeHeavy();
			display.addln(topPanel);
								
	        // Bottom panel (control area)...
	        bottomPanel = new CBPanel();
		
			btnPrint = new CBButton(CBIntText.get("Print"), CBIntText.get("Print this page."));
			btnPrint.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e){
						print();
			}});
			
			btnSave = new CBButton(CBIntText.get("Save"), CBIntText.get("Save this page."));
			btnSave.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e){
						save();
			}});			
						
			btnClose = new CBButton(CBIntText.get("Close"), CBIntText.get("Close this window."));
			btnClose.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e){
						close();
			}});
			
			btnHelp = new CBButton(CBIntText.get("Help"), CBIntText.get("Help for this window."));
			CBHelpSystem.useDefaultHelp(btnHelp, HelpIDs.SEARCH_RESULTS);
					
			bottomPanel.makeWide();
			bottomPanel.add(new JLabel("  "));
			bottomPanel.makeLight();		
			bottomPanel.add(btnPrint);
			bottomPanel.add(btnSave);
			bottomPanel.add(btnClose);
			bottomPanel.add(btnHelp);
			
			display.makeLight();
			display.addln(bottomPanel);		
			
			// Get the Container & add the display components...
	        Container pane = getContentPane();
	        pane.setLayout(new BorderLayout());	
			pane.add(display);	
			
			setSize(500, 300);
			CBUtility.center(this, browser);
			setVisible(true);		
		}	
		
   	   /**
	    *	To correctly set the data in the table, this method removes the scroll pane
		*	and therefore the table, then recreates the DefaultTableModel, the CBTableSorter,
		*	the JTable itself and the JScrollPane.  Adds the scroll pane back onto the top panel
		*	then repaints it all.  Phew...to not do this some weird painting occurs!
		*	<p>
		*	The title of the dialog is also set with the number of search results.
		*	@param tableData the data that is to be displayed in the tabel i.e. Object[rows][columns].
		*	@param tableHeader an array holding the attribute names that are used for the header of 
		*		each column (cn, sn etc).
		*	@param num the number of search results returned (this is just used in the title bar).
		*	@param map a store for the DN of a particular row number.
		*/
		public void setTableData(Object[][] tableData, String[] tableHeader, int num, HashMap map)
		{	 
			this.map = map;
			setTitle(CBIntText.get(String.valueOf(num) + " " + CBIntText.get("Search Results")));

            // Get rid of the scroll pane that holds the table...
			topPanel.remove(scrollPane);

			model = new DefaultTableModel(tableData, tableHeader);

            // For sorting via clicking on table headers...
            sorter = new CBTableSorter(model);

            // Requires a click/shift + click...
            table = new JTable(sorter);
			addMouseListenerToTable();
			sorter.addMouseListenerToHeaderInTable(table);
			scrollPane = new JScrollPane(table);	
			
			topPanel.add(scrollPane);	

			repaint();
		}

	   /**
	    *	Adds a mouse listener to the table.  It is only listening for right
		*	mouse click events.  Kicks off the setUp for the popup menu with the 
		*	co-ordinates of the mouse at the time of the click.
		*/
		public void addMouseListenerToTable()
		{
			table.addMouseListener(new MouseAdapter()
			{
                public void mousePressed(MouseEvent e) { if (!doPopupStuff(e)) super.mousePressed(e); }

                public void mouseReleased(MouseEvent e) { if (!doPopupStuff(e)) super.mouseReleased(e); }

				public boolean doPopupStuff(MouseEvent e)
				{
					if(!e.isPopupTrigger())
                        return false;

                    setUpPopup(e.getX(), e.getY());

                    return true;
				}
			});		
		}

	   /**
	    *	Sets up a popup menu with one menu item 'Go to enty...'.  Because
		*	this is triggered by a right click - this method grabs the row at the
		*	location of the mouse event and ensures that that row is selected (the 
		*	table doesn't do this automatically).  Adds a listener to the menu item
		*	that kicks off the 'goToEntry' work if selected.
		*	@param x the vertical co-ordinate of the mouse click.
		*	@param y the horizontal co-ordinate of the mouse click.
		*/
		public void setUpPopup(int x, int y)
		{
			JPopupMenu pop = new JPopupMenu("Go to");
			final JMenuItem menuItem = new JMenuItem(CBIntText.get("Go to entry..."),
                    new ImageIcon(Theme.getInstance().getDirImages()+"goto.gif"));
			pop.add(menuItem);

            // If a right click has been performed we can't be sure that row is selected.
			final int selectedRow = table.rowAtPoint(new Point(x, y));

			table.changeSelection(selectedRow, table.getSelectedColumn(), false, false);
						
	        menuItem.addActionListener(new ActionListener()
	        {
	            public void actionPerformed(ActionEvent e)
	            {
	                JMenuItem src = ((JMenuItem)e.getSource());
						
	                if (src == menuItem)
	                    goToEntry(selectedRow);	
	            }
	        });		
			
			pop.show(table, x, y);
		}
		
	   /**
	   	*	Gets the Explore tree, gets the DN from the HashMap (by looking up the true row number via
		*	the table sorter).  Asks the tree to read and expand the DN and flips to the Explore tab.
		*	@param selectedRow the row number that is selected.  This is the look up key for the HashMap.
		*		Prior to looking up the DN in the HashMap - a check is done that the row number is the true
		*		row number and not the row number after a sort.
		*/
		public void goToEntry(int selectedRow)
		{
            // Get the Explore tab tree...
			SmartTree tree = browser.getTree();

            // Get the DN from the HashMap store (get the true row number from the sorter)...
			Object temp = map.get(String.valueOf(sorter.getTrueIndex(selectedRow)));
			tree.collapse();
			tree.readAndExpandDN(new DN(temp.toString()));

            // Flip to the Explore tab...
			browser.getTreeTabPane().setSelectedComponent(browser.getExplorePanel());
		}

	   /**
		* 	The method @print@ must be implemented for @Printable@ interface.
		* 	Parameters are supplied by system.
		*/
		public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException
		{
			Graphics2D g2 = (Graphics2D)g;

            //set default foreground color to black...
			g2.setColor(Color.black);

			//for faster printing, turn off double buffering
			//RepaintManager.currentManager(this).setDoubleBufferingEnabled(false);

			Dimension d = printComponent.getSize();    			//get size of document
			double panelWidth  = d.width;    					//width in pixels
			double panelHeight = d.height;   					//height in pixels
			double pageHeight = pf.getImageableHeight();   		//height of printer page
			double pageWidth  = pf.getImageableWidth();    		//width of printer page
			double scale = pageWidth/panelWidth;
			int totalNumPages = (int)Math.ceil(scale * panelHeight / pageHeight);

			//make sure we don't print empty pages
			if(pageIndex >= totalNumPages)
			{
				return Printable.NO_SUCH_PAGE;
			}

			//shift Graphic to line up with beginning of print-imageable region
			g2.translate(pf.getImageableX(), pf.getImageableY());

			//shift Graphic to line up with beginning of next page to print
			g2.translate(0f, -pageIndex*pageHeight);

			//scale the page so the width fits...
			g2.scale(scale, scale);

			// PRINT IT!
			printComponent.paint(g2);

			return Printable.PAGE_EXISTS;
		}

	   /**
		*  	Starts the print operation.  This is quite expensive, and is kicked off
		*  	in a separate thread.
		*/
		public void print()
		{
			final PrinterJob job = PrinterJob.getPrinterJob();
			job.setPrintable(this);

			// make a local copy of the object to be printed, for use by the print thread
			// below...

			final Component printMe = table;

			Thread worker = new Thread()
			{
				public void run()
				{
					if (job.printDialog())
					{
						try 
						{ 
							// need to make sure that no other thread tries to 
							// run a print job and set printCompnent at the same
							// time...
							synchronized(this)
							{ 
								printComponent = printMe;
								job.print(); 
								printComponent = null;
							}    
						}
						catch (Exception ex) 
						{ 
							log.warning("error printing: " + ex);
						}
					}
				}
			};
			worker.start();	
		}	

	   /**
	    *	Disposes of the dialog.
		*/
		public void close()
		{
			this.setVisible(false);
			this.dispose();
		}

	   /**
	    *	Opens a file selector and saves the contents of the JTable in commar-separated-form
		*	(.csv) to the location the user has selected.
		*/
		public void save()
		{
	        chooser = new JFileChooser(JXConfig.getProperty("csv.homeDir"));
	        
	        chooser.addChoosableFileFilter(new CBFileFilter(new String[] {"csv"},"CSV Files (*.csv)"));
	        
	        int option = chooser.showSaveDialog(this);

            // only do something if user chose 'ok'...
	        if (option == JFileChooser.APPROVE_OPTION)
	        {
	            File readFile = chooser.getSelectedFile();
	            
				if (readFile == null)
				{
	                CBUtility.error(CBIntText.get("Please select a file"));
				}
	            else
	            {
                    // Make sure the files extension is '.csv'...
					readFile = adjustFileName(readFile);
					
					int response = -1;

                    // Ask the user if they want to overwrite an existing file...
					if (readFile.exists())
					{
						response = JOptionPane.showConfirmDialog(this,
                                CBIntText.get("The File ''{0}'' already exists. Do you want to replace it?", new String[] {readFile.toString()}),
                                CBIntText.get("Overwrite Confirmation"), JOptionPane.YES_NO_OPTION );

						if (response != JOptionPane.YES_OPTION)
							save();
					}

                    // Save the user specified path to dxconfig.txt...
	                JXConfig.setProperty("csv.homeDir", readFile.getParent());
	                doFileWrite(readFile);
	            }                    
	        }						
		}

	   /** 
		*   A quick spot of mucking around to add '.csv' to naked files.
		*	@param file the file to add the extension to.
		*/
	    protected File adjustFileName(File file)
	    {
            // sanity check.
	        if (file == null)
                return null;

            // don't do anything if file already exists...
	        if (file.exists())
                return file;
	        
	        String name = file.getName();

            // ... or if it already has an extension.
	        if (name.indexOf('.') != -1)
                return file;
	        
	        name = name + ".csv";
	        
	        return new File(file.getParentFile(), name);
	    }		

	   /** 
		*   Gets the text from the table, escapes any commars (by placing quotes around text),  then
		*	writes the CSV file to the user specified location.  Closes the file when complete.
		*	@param file the file to save the data to.
		*/
	    protected void doFileWrite(File file)
	    {                           
	        if (file == null)
	            CBUtility.error(CBIntText.get("Unable to write to empty file"), null);

			FileWriter fileWriter = null;
			
            try
            {
                fileWriter = new FileWriter(file);

                // Temp storage for data that is to be written to file...
				StringBuffer buffy = new StringBuffer(0);

                // How many columns in the table...
				int cols = model.getColumnCount();

                // How many rows in the table...
				int rows = model.getRowCount();
				String temp = "";
				
				for (int i = 0; i < rows; i++)
				{
                    // Grabs the text from the table model...
					for (int j = 0; j < cols; j++)
					{
						temp = (model.getValueAt(i, j)).toString();

                        // Escape the value for CSV, then add it to the list...
						buffy.append(escapeForCSV(temp));

                        // Don't add a commar at the end of the row, instead put in a carrage return....
						if(!((j == cols - 1) == true))
							buffy.append(",");
						else
							buffy.append("\n");
						
						temp = "";
					}
				}						

				fileWriter.write(buffy.toString());

				fileWriter.close();		            
				log.warning("Closed CSV file");
				
				chooser.setVisible(false); 
            }
            catch (Exception e)
            {
                log.warning("Error writing CSV file from Return Attributes dialog "+e);
            }
	    }  			
	}

    /**
     * Escapes a value for CSV:
     * 1) any " is replaced with ""
     * 2) trims white space from start and end.
     * 3) the string is surrounded in double quotes.
     * For example (note white space at end),<br>
     * John, "Da Man" Doe <br>
     * becomes<br>
     * "John, ""Da Man"" Doe"
     * @param str the string value to escape.
     * @return the string value escaped.
     */
    private String escapeForCSV(String str)
    {
        if(str == null)
            return str;

        str = str.trim();

        str = str.replaceAll("\"", "\"\"");
        return "\"" + str + "\"";
    }
}

