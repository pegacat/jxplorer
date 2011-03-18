package com.ca.directory.jxplorer.search;

import javax.swing.*;

import com.ca.commons.cbutil.*;	



/**
*	A basic panel that has a text area which the value can be set and 
*	retreived.  Currently used in the search dialog for the text input
*	of LDAP filters.
*	.
*/
	
class TextFilterPanel extends CBPanel
{
	JTextArea area;
	
	
	
   /**
    *	Constructs a panel with a text area that is currently used by the search dialog.
	*	.
	*/
			
	public TextFilterPanel()	
	{			
		addln(new JLabel("  "));	//TE: padding at top.
		makeHeavy();
		area = new JTextArea();	
        area.setLineWrap(true);		//TE: allows line wrapping.
        add(new JScrollPane(area, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),1,1);		//TE: scroll pane that scrolls vertically on need and never scrolls horizontally.	
	}
	
	
	
   /** 
    *	Returns the value from the text area.
	*	@return the string value (hopefully a valid filter) from the text area.
	*	.
	*/
		
	public String getFilter()
	{
		return area.getText();
	}
	
	
	
   /**
    * 	Displays the given string filter in the text area.
	*	@param filter the string filter to display in the text area.
	*	.
	*/
		
	public void displayFilter(String filter)
	{
		area.setText(filter);
	}
			
	
	
   /**
    *	This basically only checks if there is a value in the text area.  It doesn't 
	*	do a syntax check of the LDAP filter...we leave that up to the dsa.
	*	@return true if there is a value (if after a string trim the length is greater than zero),
	*		false otherwise.	
	*	.
	*/
	
	public boolean isFilterValid()
	{
		String filter = area.getText();
		
		if (filter.trim().length() <=0)
			return false;
	
		return true;
	}
}