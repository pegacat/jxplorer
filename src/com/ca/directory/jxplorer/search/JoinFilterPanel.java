package com.ca.directory.jxplorer.search;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.*;

import com.ca.commons.cbutil.*;



/**
*	Creates a scrollable panel that can add and remove rows that are used to join raw filters or filters
*	that are a combination of filter.  Each row consists of a filter combo box that contains the names of
*	all possible filters that can be joined (obtained from search_filter.txt) and an edit button which is used
*	to display the filter of a particular row.  If there are more than one row, an And/Or combo is added to the second
*	row and and And/Or labels to all rows there after.  The And/Or labels always show the value of the And/Or combo box.
*	<p>
*	This idea behind this class is that a user can join existing filters. The filters can be raw filters or filters
*	consisting of already joined filters.  This enables the user to construct complex LDAP filters.
*	<p> 
*	This panel is controlled by the SearchGUI class.  I.e. the SearchGUI class tells this class to add or remove a row or
* 	to edit or load a certain filter.  It also feeds back to the SearchGUI the values of it's components when required.
*	@author Trudi.
*/

public class JoinFilterPanel extends CBPanel
{
	CBPanel 				panel = new CBPanel();
	JCheckBox				notCheckBox;
	static final String[]	andOrArray = new String[] {CBIntText.get("And"), CBIntText.get("Or")};
	static final int 		AND=0, OR=1;
	JLabel[]				andOrLabel = new JLabel[50];
	CBJComboBox[]				filterCombo = new CBJComboBox[50];
	CBJComboBox				andOrCombo;
	CBButton[]				btnEdit = new CBButton[50];
	int						rowCount=0, labelCount=0;
	SearchModel				searchModel = new SearchModel();
	
    private static Logger log = Logger.getLogger(JoinFilterPanel.class.getName());

	
   /**
    *	Constructor that sets up the Join tab with a not check box, and the first row e.g a filter combo and an edit button.
	*	@param btnEdit the edit button that is added to each row.
	*	.
	*/
		
	public JoinFilterPanel(CBButton btnEdit)
	{	
		notCheckBox = new JCheckBox(CBIntText.get("Not"));
		notCheckBox.setToolTipText(CBIntText.get("Not this filter"));
		
		makeLight();
		add(notCheckBox);

		newLine();
	
		makeHeavy();
				
		add(new JScrollPane(panel)); 				
		makeLight(); 
		
		addFilterRow(btnEdit);		
	}	
	
	
	
   /**
    *	<p>Adds a row or line to the Join tab.  A line consists of a filter combo and an edit button.  If the second
	*	row is added, an And/Or combo is added, if there are any more rows that are added after the second an And/Or
	*	label is added with the value of the And/Or combo.  If the And/Or combo is changed by the user the And/Or
	*	labels are updated to reflect the change via a listener on the And/Or combo.</p>
	*
	*	<p>The components in a row are added according to row number for example if the second row is being added, a 
	*	filter combo is created and stored in the filter combo array at that row position.</p>
	*	@param btnEdit the edit button to be added to the row.
	*	.
	*/
		
	protected void addFilterRow(CBButton btnEdit)
	{
		if (rowCount < 50)
		{
			panel.newLine();
			panel.makeLight();
			
			/*
			 *    The first row has an 'invisible' combo box so the layout stays regular
			 */
			 
			if (rowCount==0)
			{
				panel.add(andOrCombo = new CBJComboBox(andOrArray));	//TE: combo box ('and', 'or').
				andOrCombo.setPreferredSize(new Dimension(50, 20));
				andOrCombo.setVisible(false);
			}
			
			/*
			 *    The second row has the active combo box that the user can change.
			 */
			 
			else if (rowCount==1)
			{
				panel.add(andOrCombo= new CBJComboBox(andOrArray));	//TE: combo box ('and', 'or').
				
				andOrCombo.addActionListener(new ActionListener(){	//TE: add an action listener that updates the And/Or labels.
					public void actionPerformed(ActionEvent e){				
						try
						{
							for(int i=0; i<labelCount;i++)
								andOrLabel[i].setText(andOrCombo.getSelectedItem().toString());			
						}
						catch(Exception ee)
						{
							log.log(Level.WARNING, "Problem updating the 'And/Or' labels in the Join tab of the search dialog:", ee);
						}
				}});				
				andOrCombo.setPreferredSize(new Dimension(50, 20));
			}
			
			/*
			 *    Any subsequent rows simply have labels that 'echo' the status of the combo box on the second row.
			 */
			 
			else if (rowCount >1)
			{
				andOrLabel[labelCount] = new JLabel();				//TE the and/or label.
				try
				{
					andOrLabel[labelCount].setText(andOrCombo.getSelectedItem().toString());
				}
				catch(Exception e)
				{
					andOrLabel[labelCount].setText(" ");
					log.log(Level.WARNING, "Problem updating the 'And/Or' labels in the Join tab of the search dialog:", e);
				}
				panel.add(andOrLabel[labelCount]);
				labelCount++;										//TE: keep the label counter up to date.
			}

			panel.makeWide();
				
			ArrayList list = searchModel.getFilterNames(SearchModel.BUILDJOIN);
			
			Object listOb[] = list.toArray();
			Arrays.sort(listOb, new SearchModel.StringComparator());		//TE: sort the list alphabetically.
		
			panel.add(filterCombo[rowCount] = new CBJComboBox(listOb));		//TE: the attribute combo ('cn', 'sn' etc).
			filterCombo[rowCount].setPreferredSize(new Dimension(140, 20));
			filterCombo[rowCount].setRenderer(new CBBasicComboBoxRenderer(listOb));
			
			panel.makeLight();
			
			panel.add(btnEdit);

			rowCount++;														//TE: keep the row counter up to date.
			panel.revalidate();			
		}		
	}
	


   /**
    *	Updates the all filter combo's and their renders with any new filters that have usually been added 
	*	in the Build tab.
	*	@param item the name of the filter to add to the combo boxes.
	*	.
	*/

	protected void updateFilterCombo(String item)
	{ 
		ArrayList list = searchModel.getFilterNames(SearchModel.ALLFILTERS);
		list.add(item);	
		
		for(int i=0; i<rowCount; i++)
		{
			filterCombo[i].setRenderer(new CBBasicComboBoxRenderer(list.toArray()));
			filterCombo[i].addItem(item);	
		}
	}


	
   /**	
    *	Removes the last line from the join filter tab e.g the filter combo, edit button and andOr combo/label.
	*	@param btnEdit the edit button for the line or row that is being removed.
	*	.
	*/
		
	protected void removeFilterRow(CBButton btnEdit)
	{ 									   
		if (rowCount > 1)
		{  
			rowCount--;			//TE: keep the row counter up to date.
			
			if(rowCount==1)				
				panel.remove(andOrCombo);
			
			panel.remove(filterCombo[rowCount]);	
			panel.remove(btnEdit); 	
			
			if (rowCount>1)
			{	
				labelCount--;	//TE: keep the label counter up to date.
				panel.remove(andOrLabel[labelCount]);
			}
			
			panel.repaint();	   	
		}

		panel.revalidate();		
	}
	
	
	
   /**	
    *	Checks if the filter is valid.  A filter is not valid if any attribute or function
	*	combo contains an empty string or a null.
	*	returns true if all combo contain a value, false otherwise.
	*	.
	*/
		
	protected boolean isFilterValid()
	{	
		try
		{
			for(int i=0; i<rowCount; i++)
			{
				String name = filterCombo[i].getSelectedItem().toString();
				
				if(name.trim().length()<=0)
					return false;				//TE: check if the attribute combo has a value.
				
				if (rowCount>=2)	   
				{
					String andOr = andOrCombo.getSelectedItem().toString();
					
					if(andOr.trim().length()<=0) 		//TE: check if the and/or combo has a value.
						return false;	
				}
			}
		}
		catch(Exception e)
		{
			return false;
		}
		return true;
	}
		


   /**
    *	Returns the current filter that is displayed in the Join tab by getting the values of 
	*	each component.  
	*	@return the filter value (not an LDAP filter, something more like: '&JXFilter.myFilter1JXFilter.myFilter2').
	*	.
	*/
	
	protected String getFilter()
	{
		StringBuffer buffy = new StringBuffer();

		for(int i=0;i<rowCount;i++)		//TE: append the value in each filter combo e.g. 'JXFilter.myFilter1JXFilter.myFilter2'.						
			buffy.append("JXFilter." + filterCombo[i].getSelectedItem().toString());			
			
		if (rowCount>=2)
		{
			switch (andOrCombo.getSelectedIndex())
			{
				case AND: { buffy.insert(0, "&"); break; }	//TE: insert '&' (AND).
				case OR : { buffy.insert(0, "|"); break; }	//TE: insert '|' (OR).
			}
		}		
		
		if (notCheckBox.isSelected())
			buffy.insert(0, "!");		//TE: insert '!'.
								
		return buffy.toString();
	}
		
	
	
   /**	
	*	Returns the value of the filter combo box at a specified row.
	*	@param row the row number of the combo box that the value is to be returned from.
	*	@return the value that is selected in the combo box.
	*	.
	*/
	
	protected String getFilterComboValue(int row)
	{
		try
		{
			return filterCombo[row].getSelectedItem().toString();	
		}
		catch(Exception e)
		{
			return null;
		}
	}
	

	
   /**
    *	Attempts to display a filter usually after someone has selected to load a filter via the load dialog
	*	or via the edit button in the search dialog.  This method needs to set values for the andOr combo and 
	*	labels, the not check box, and the filter combos.
	*	@param list a list of the names of the filters e.g.[myFilter1, myFilter2].
	*	@param value the value of the filter being displayed e.g. '&JXFilter.myFilter1JXFilter.myFilter2'.
	*	return true if the display was successful, false otherwise.
	*	.
	*/		
	
	protected boolean displayFilter(ArrayList list, String value)
	{
		boolean not = value.startsWith("!");					//TE: do we need to tick the not check box?
		int andOr=-1;
		if(value.indexOf("&")<2 && value.indexOf("&")>-1)		//TE: do we display AND in the and/or combo box or...
			andOr=AND;
		else if(value.indexOf("|")<2 && value.indexOf("|")>-1)	//TE: ...do we display OR?
			andOr=OR;
		else
			return false;
		
		setItems(not, andOr);									//TE: set the check box and the and/or combo box.
		
		String[] names = (String[])list.toArray(new String[list.size()]);		//TE: convert the list array to a string array.
		
		for(int i=0; i<names.length; i++)						//TE: set the filter combo boxes with the appropriate filter names.
			setItems(names[i], i);		
		
		return true;			
	}
		
	
	
   /**
    *	Sets the filter combo with the supplied string (or filter) at the supplied row.
	*	@param filter the value to displayed in the filter combo.
	*	@param row the row number of the combo box that the value is to be set.
	*	.
	*/
		
	protected void setItems(String filter, int row)
	{
		filterCombo[row].setSelectedItem(filter);
		panel.revalidate();
	}	
	
	
	
   /**
    *	Selects or de-selects the not check box depending on the value supplied and 
	*	sets the and/or combo to the index supplied.
	*	@param not the state of the not check box.
	*	@param index the index that the and/or combo is to be set to.
	*	.
	*/
	
	protected void setItems(boolean not, int index)
	{
		notCheckBox.setSelected(not);
		if(index==-1)
			index=0;	//TE: just in case something stuffed up, display AND.
		andOrCombo.setSelectedIndex(index);
		panel.revalidate();
	}
}

