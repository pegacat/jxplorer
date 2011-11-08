package com.ca.directory.jxplorer.search;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Logger;
import javax.swing.*;

import com.ca.directory.jxplorer.*;
import com.ca.commons.cbutil.*;

/**  
*	This class sets up a dialog that displays a list of saved filters (from search_filters.txt).   	
*	It allows the user to delete one filter at a time.  For each filter being deleted it checks if it
*	is a subfilter of any other filter.  If so it checks if that filter is a subfilter and so on.
*	(A filter is unusable if a subfilter is deleted).  After all of the dependant filters are determined,
*	a prompt is displayed asking if they really want to delete this filter and all its dependant filters.
*	@author Trudi.
*/
public class DeleteFilterGUI extends CBDialog
{
	SearchModel     searchModel = new SearchModel();
	JList 		    list;
	MainMenu 	    mainMenu;
	ArrayList 	    filterNames;
    JXplorerBrowser browser = null;

    private static Logger log = Logger.getLogger(DeleteFilterGUI.class.getName());

    /**
    *	Constructor that sets up a dialog with a scrollable list which is used to display
	*	all of the filters that can be deleted.
	*	@param jxplorer JXplorer.	
	*/
	public DeleteFilterGUI(JXplorerBrowser jxplorer)
	{
		super(jxplorer, CBIntText.get("Delete Search Filter"), HelpIDs.SEARCH_DELETE_FILTER);
		this.browser = jxplorer;
        mainMenu = jxplorer.getMainMenu();
		
		filterNames = searchModel.getFilterNames(SearchModel.ALLFILTERS);
				
		display.addln(new JLabel(CBIntText.get("Select a Search Filter to Delete")+":"));

		display.makeWide();
		display.add(getScrollList(filterNames.toArray(), filterNames.toArray()));

		CBButton btnDelete = new CBButton(CBIntText.get("Delete"), CBIntText.get("Delete the selected filters."));
		btnDelete.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					delete();
		}});

        // Clear out the buttons...
        buttonPanel.removeAll();

        // Add the buttons we need...
		buttonPanel.add(btnDelete);
		buttonPanel.add(Cancel);
		buttonPanel.add(Help);

        btnDelete.setToolTipText(CBIntText.get("Click here to delete the selected filter."));
        Cancel.setToolTipText(CBIntText.get("Click here to exit."));
        Help.setToolTipText(CBIntText.get("Click here for Help."));

		setSize(300, 200);

		CBUtility.center(this, jxplorer);
	}

   /**
    *	Sets up a scrollable list with given items and tooltips.
	*	@param items an array of things (usually strings that are added to the list).
	*	@param toolTips and array of strings used as the tooltips for the items (i.e. tooltip[x] goes with item[x]).
	*	@return the scroll pane with the list and listeners added.
	*/
	protected JScrollPane getScrollList(Object[] items, Object[] toolTips)
	{
		final Object[] names = items;
		final Object[] toolTps = toolTips;
		
		list = new JList(names) 
		{
			public String getToolTipText(MouseEvent e)  	//TE: add a tool tip!
			{
				int index = locationToIndex(e.getPoint());	
				if (-1 < index) 
					return toolTps[index].toString();
				else 
					return null;
			}
		};
		
		list.setSelectionMode(0);
        list.setSelectionModel(new CBSingleSelectionModel(list));   //TE: tries to ensure that the selected item is visible.
		list.setToolTipText("");
				
		JScrollPane sp = new JScrollPane(list);
		sp.setPreferredSize(new Dimension(250, 100)); 
		sp.setMinimumSize(new Dimension(250, 100)); 
		sp.setAlignmentX(LEFT_ALIGNMENT);		

		return sp;	
	}	

   /**
    * 	Gets the filter that the user has chosen to delete and displays a list of dependant filters.
	*	These dependant filters will have to be deleted as well or they will not work.  It displays a 
	*	list of these filters (via a JOptionPane message) so the user can decide if they really want
	*	to delete the filters.  If they do the method goes ahead and deletes them and updates the 
	*	search menu before exiting.
	*/
	public void delete()
	{
		if(filterNames.size()<=0)
			return;

		if(list.getSelectedIndex() ==-1)
		{
			int response = JOptionPane.showConfirmDialog(this, CBIntText.get("No filter selected.  Try again?"), CBIntText.get("No Filter Selected"), JOptionPane.YES_NO_OPTION);

			if (response == JOptionPane.NO_OPTION)						//TE: the user hasn't made a selection do they want to try again or exit the dialog?
			{
				setVisible(false);	
				dispose();
				return;	  
			}
			else
			{
				return;
			}
		} 
			
		Object value = list.getSelectedValue(); 
		
		int response;		 
		
		boolean text = false;
		
		if (searchModel.isTextFilter(value.toString()))
			text = true;

		if (!text)
		{				
			ArrayList dependantFilters = searchModel.getDependants(value);	//TE: get all of the filters that depend on the filter that the user wants to blow away.		
			
			String[] names = (String[])dependantFilters.toArray(new String[dependantFilters.size()]);
			StringBuffer buffy = new StringBuffer();
			
			for(int i=1;i<names.length;i++)									//TE: remove the 'JXFilter.' from the filter names.
			{
				buffy.append(names[i].substring(names[i].indexOf(".")+1));
				buffy.append("\n"); 
			}			
			
			if (names.length==1)											//TE: if there are no dependant filters...
			{
				response = JOptionPane.showConfirmDialog(this, CBIntText.get("Are you sure you want to delete: ''{0}'' ?",
                                new String[] {value.toString()}),
								CBIntText.get("Delete Information"), JOptionPane.YES_NO_OPTION);
			}
			else															//TE: if there are dependand filters display them and wait for user confirmation before deleting them...
			{
				response = JOptionPane.showConfirmDialog(this, CBIntText.get("Deleting ''{0}'' will also delete these dependant filters: ''{1}''\nDo you want to continue?\n\n",
                                new String[] {value.toString(), buffy.toString()}),
								CBIntText.get("Delete Information"), JOptionPane.YES_NO_OPTION);
			}
		
			if (response == JOptionPane.YES_OPTION)
			{
				searchModel.removeFilters(names);
			
				mainMenu.updateSearchMenu();								//TE: update the search menu.
				
				try
				{
					for(int i=0; i<names.length; i++)
					{
						filterNames.remove(names[i].substring(names[i].indexOf(".")+1));
						log.info("Deleted search filter: " + names[i].substring(names[i].indexOf(".")+1));
					}

					list.setListData(filterNames.toArray());
				}
				catch(Exception e)
				{
					log.warning("No selection to remove.");
				}				
			}
		}
		else
		{
			response = JOptionPane.showConfirmDialog(this, CBIntText.get("Are you sure you want to delete: ''{0}'' ?", new String[] {value.toString()}),
							CBIntText.get("Delete Information"), JOptionPane.YES_NO_OPTION);			
					
			if (response == JOptionPane.YES_OPTION)
			{
				searchModel.removeFilter("JXTextFilter."+value.toString());
			
				mainMenu.updateSearchMenu();								//TE: update the search menu.
				
				try
				{
					filterNames.remove(value.toString().substring(value.toString().indexOf(".")+1));
					log.info("Deleted search filter: " + value.toString().substring(value.toString().indexOf(".")+1));

					list.setListData(filterNames.toArray());
				}
				catch(Exception e)
				{
					log.warning("No selection to remove.");
				}				
			}							
		}

        // Set the search GUI to null so that it is forced to re-read it's config so it gets updated filter list...
		browser.getTree().setSearchGUI(null);
		browser.getSearchTree().setSearchGUI(null);
		browser.getSchemaTree().setSearchGUI(null);
	}
}
