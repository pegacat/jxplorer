package com.ca.directory.jxplorer.search;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.*;
import javax.naming.NamingException;

import com.ca.directory.jxplorer.*;
import com.ca.commons.cbutil.*;
import com.ca.commons.jndi.SchemaOps;

import com.ca.directory.jxplorer.editor.generalizedtimeeditor;

/**
*	Creates a scrollable panel that can add and remove rows that are used to build raw filters.  Each row consists of 
*	an attribute combo box ('sn'), a function combo box ('Equal To'), and text field that the user can enter the filter value.  
*	If there are more than one row, an And/Or combo is added to the second row and and And/Or labels to all rows there after.  The And/Or labels always show the value of the And/Or combo box.
*	<p>
*	This idea behind this class is that a user can build simple raw filters (e.g. (&(cn=f*)(sn=f*)) with only one function ('|' or '&').
*	<p> 
*	This panel is controlled by the SearchGUI class.  I.e. the SearchGUI class tells this class to add or remove a row or
* 	to edit or load a certain filter.  It also feeds back to the SearchGUI the values of it's components when required.
*	@author Trudi.
*/
public class BuildFilterPanel extends CBPanel
{
	SchemaOps 				schema;
	int 					rowCount = 0, labelCount=0;
	CBJComboBox				andOrCombo;
	CBJComboBox[]			attributeCombo = new CBJComboBox[50], functionCombo = new CBJComboBox[50];
	JTextField[]			filterField = new JTextField[50];	
	JLabel[]				andOrLabel = new JLabel[50];
	static final String[]	andOrArray = new String[] {CBIntText.get("And"), CBIntText.get("Or")};
	static final String[]	functionArray = new String[] {CBIntText.get("Beginning With"),CBIntText.get("Not Beginning With"),CBIntText.get("Containing"),CBIntText.get("Not Containing"),CBIntText.get("Equal To"),CBIntText.get("Not Equal To"),CBIntText.get("Ending In"),CBIntText.get("Not Ending In"),CBIntText.get("Greater Than or Equal To"),CBIntText.get("Not Greater Than or Equal To"),CBIntText.get("Less Than or Equal To"),CBIntText.get("Not Less Than or Equal To"),CBIntText.get("Present"),CBIntText.get("Not Present"),CBIntText.get("Similar To"),CBIntText.get("Not Similar To") };
	static final int    	BEGIN=0, NOTBEGIN=1, CONTAINING=2, NOTCONTAINING=3, EQUALS=4, NOTEQUALS=5, ENDING=6, NOTENDING=7, GREATER=8, NOTGREATER=9, LESS=10, NOTLESS=11, PRESENT=12, NOTPRESENT=13, SIMILAR=14, NOTSIMILAR=15;	//TE: represent positions in the functionArray.	
	CBPanel 				panel = new CBPanel();
	JCheckBox				notCheckBox;
	String[] 				attrs;
	
	generalizedtimeeditor 	gte;
	JXplorerBrowser browser;

    private static Logger log = Logger.getLogger(BuildFilterPanel.class.getName());

    /**
    * Constructor that sets up the Build tab with a not check box, and the first row e.g an attribute combo,
	* a function combo and a text field.
	* @param jx JXplorer, used to get the search broker.
	*/
	public BuildFilterPanel(JXplorerBrowser jx)
	{   		
		schema = jx.getSearchBroker().getSchemaOps();
 		attrs = getAttributes();									//TE: get all attributes available.
		browser = jx;
		
		notCheckBox = new JCheckBox(CBIntText.get("Not"));
		notCheckBox.setToolTipText(CBIntText.get("Not this filter"));
		
		makeLight();
		add(notCheckBox);

		newLine();
	
		makeHeavy();
				
		add(new JScrollPane(panel)); 				
		makeLight(); 
		
		addFilterRow();		
	}

   /**
    *	Attempts to display a filter usually after someone has selected to load a filter via the load dialog
	*	or via the edit button in the search dialog.  This method needs to set values for the andOr combo and 
	*	labels, the not check box, the attribute combos and the function combos.
	*	@param filter the actual raw filter to be displayed e.g. '(!(|(!(sn=r*))(sn=e*)))'.
	*	@return true if the display was successful, false otherwise.
	*/
	protected boolean displayFilter(String filter)
	{
		while(rowCount>1)
			removeFilterRow();

		notCheckBox.setSelected(false);
					
		try
		{
			if (getOccurrences(filter, "(")>2)				//TE: filter contains more than one filter item therefore check for !, & or |.
			{
				if (filter.charAt(1) == '!')				//TE: set the check box if the filter starts with a not.
				{
					notCheckBox.setSelected(true);
					filter = trimFilter(filter);
				}

				if (filter.indexOf("&") >-1)				//TE: add a row and set the and/or combo box to 'AND'.
				{
					addFilterRow();
					andOrCombo.setSelectedIndex(0);	
					filter = trimFilter(filter);
				}
				else if (filter.indexOf("|") >-1)			//TE: add a row and set the and/or combo box to 'OR'.
				{
					addFilterRow();
					andOrCombo.setSelectedIndex(1);	
					filter = trimFilter(filter);
				}
				panel.revalidate();	
			}
			
			int items = (getOccurrences(filter, "(") - getOccurrences(filter, "!"));	//TE: by this stage the operators should have been trimmed off.
					
			int i=1;						
			
			while (i <= items)
			{
				String temp = (filter.indexOf("!") == 1) ? filter.substring(0, filter.indexOf("))")+2) : filter.substring(0, filter.indexOf(")")+1);
				
				if(i>2)
					addFilterRow();							//TE: two rows should have been already added.
					
				attributeCombo[i].setSelectedItem(getAttributeComboValue(temp));

				functionCombo[i].setSelectedIndex(getFunctionPosition(temp));
				
				filterField[i].setText(getFilterFieldText(temp));
				
				filter = filter.substring(temp.length());
				i++;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		
		return true;										//TE: display successful.
	}
	
   /**
    *	Returns the attribute value that is being search for.  For example, 'cn' in this filter:
	*	'(cn=Bob)'.  The way it does this is by removing the brackets (and '!' if present) from the beginning of
	*	the filter then cutting anything off the filter after any one of the following operators: '>=', '<=', '~=' or '='.  
	*	@param filter the search filter that the value (or text) is to be extracted from (e.g '(cn=Bob)').
	*	@return the attribute value, for example 'cn'.
	*/
	protected String getAttributeComboValue(String filter)
	{
		String value = "";
		
		try
		{
			if(filter.indexOf("!") >-1)			//TE: if (!(...))
				value = filter.substring(3);
			else								//TE: if (...)
				value = filter.substring(1);
				
			if(value.indexOf(">=")>-1)
				value = value.substring(0, value.indexOf(">="));
			else if(value.indexOf("<=")>-1)
				value = value.substring(0, value.indexOf("<="));
			else if(value.indexOf("~=")>-1)
				value = value.substring(0, value.indexOf("~="));
			else if(value.indexOf("=")>-1)
				value = value.substring(0, value.indexOf("="));	
		}
		catch (Exception e)
		{
			return "";
		}												

		return value;			
	}

   /**
    *	Returns the value that is being search for.  For example, 'Bob' in this filter:
	*	'(cn=Bob)'.  The way it does this is by checking the filter for one of the following 
	*	operators: '=*' or '='.  Then makes a substring from the end of the opeator
	*	to the end of the filter, for example, 'Bob)'.  To remove the end bracket(s), a check is done
	*	to see if the filter was  a 'not' filter ('(!(cn=Bob))').  If it was two brackets are removed,
	*	otherwise one.  The final check is to see if the filter ends with a '*' i.e containing or begins with.  
	*	If so it is removed also.
	*	@param filter the search filter that the value (or text) is to be extracted from (e.g '(cn=Bob)').
	*	@return the value, for example 'Bob'.
	*/
	protected String getFilterFieldText(String filter)
	{
		String text = "";

		try
		{		
			if(filter.indexOf("=*")>-1)
				text = filter.substring(filter.indexOf("=*")+2);	
			else if(filter.indexOf("=")>-1)
				text = filter.substring(filter.indexOf("=")+1);	
														
			if (filter.indexOf("!") >-1)						//TE: if (!(...))
				text = text.substring(0, text.length()-2);
			else												//TE: if (...)
				text = text.substring(0, text.length()-1);
			
			if(text.endsWith("*"))
				text = text.substring(0, text.length()-1);
				
			text = replace(text, "\\28", "(");		//TE: replaces any escaped characters.  WARNING: the '\\' replace should always go last.
			text = replace(text, "\\29", ")");
			text = replace(text, "\\2A", "*");
			text = replace(text, "\\5c", "\\");					
		}
		catch (Exception e)
		{
			return "";
		}

		return text;			
	}	

   /**
    *	Determines what the function of the filter is for example if it is a ~= or (!)~=.
	*	Returns the position or location in the function Combo of this function.  For example
	*	(!)~= is at position 15.
	*	@param filter the filter that we are to determine the function of.
	*	@return the position of this function in the functionCombo.
	*/
	protected int getFunctionPosition(String filter)
	{
		//TE: WARNING: messing with the order of these statements is BAD! 
		int asteriskCount = getOccurrences(filter, "*");
	
	    boolean containsNot = (filter.indexOf("!") >-1);
		boolean endsWithStar = (filter.endsWith("*)"));
		boolean endsWithStars = (filter.endsWith("*))"));

		if ((filter.indexOf("=*") >-1) && !containsNot && endsWithStar && (asteriskCount > 1)) 			//TE: (cn=*f*)		
			return CONTAINING;	
		else if ((filter.indexOf("=*") >-1) && containsNot && endsWithStars && (asteriskCount > 1))  	//TE: (!(cn=*f*))		
			return NOTCONTAINING;	
		else if ((filter.indexOf("=*)") >-1) && !containsNot)					//TE: (cn=*)		
			return PRESENT;	
		else if ((filter.indexOf("=*))") >-1) && containsNot)					//TE: (!(cn=*))		
			return NOTPRESENT;											
		else if ((filter.indexOf("=") >-1) && !containsNot && endsWithStar) 	//TE: (cn=f*)
			return BEGIN;
		else if ((filter.indexOf("=") >-1) && containsNot && endsWithStars)	 	//TE: (!(cn=f*))
			return NOTBEGIN;			
		else if ((filter.indexOf("=*") >-1) && !containsNot)					//TE: (cn=*f)
			return ENDING;											
		else if ((filter.indexOf("=*") >-1) && containsNot)						//TE: (!(cn=*f))
			return NOTENDING;	
		else if ((filter.indexOf(">=") >-1) && !containsNot)					//TE: (cn>=f)
			return GREATER;	
		else if ((filter.indexOf(">=") >-1) && containsNot)	   					//TE: (!(cn>=f))
			return NOTGREATER;	
		else if ((filter.indexOf("<=") >-1) && !containsNot)					//TE: (cn<=f)
			return LESS;	
		else if ((filter.indexOf("<=") >-1) && containsNot)	 					//TE: (!(cn<=f))
			return NOTLESS;		
		else if ((filter.indexOf("~=") >-1) && !containsNot)					//TE: (cn~=f)
			return SIMILAR;	
		else if ((filter.indexOf("~=") >-1) && containsNot)						//TE: (!(cn~=f))
			return NOTSIMILAR;			
		else if ((filter.indexOf("=") >-1) && !containsNot)   					//TE: (cn=f)
			return EQUALS;	
		else if ((filter.indexOf("=") >-1) && containsNot)	   					//TE: (!(cn=f))
			return NOTEQUALS;	
					
		return -1;	
	}

   /**
    *	Counts the number of times a substring occurs within a string.
	*	@param string the string that is getting checked for the occurence of a substring.
	*	@param substring the substring that is being checked for.
	*	@return the number of times the substring occurs within the string.
	*/
	protected int getOccurrences(String string, String substring)
	{
		int pos = -1;
		int count = 0;
		
		while ((pos = string.indexOf(substring, pos+1))!=-1)
			count++;
 
		return count;			
	}

   /**
    *	Trims a string by two places at the beginning of the string and one place at
	*	the end of the string.  Usually used to trim the operator from a filter eg 
	*	(&(cn=f*)(sn=f*))	=>	(cn=f*)(sn=f*).
	*	@param filter the string (e.g. (&(cn=f*)(sn=f*))) to trim.
	*	@return the trimmed string (e.g. (cn=f*)(sn=f*))
	*/
	protected String trimFilter(String filter)
	{
		return filter.substring(2, filter.length()-1);
	}
	
   /**
    *	Addes a row or line to the Build tab.  A line consists of an attribute combo, a function combo and text field.
	*	If the second row is added, an And/Or combo is added, if there are any more rows that are added after the 
	*	second an And/Or label is added with the value of the And/Or combo.  If the And/Or combo is changed by the user the And/Or
	*	labels are updated to reflect the change via a listener on the And/Or combo.
	*	The components in a row are added according to row number for example if the second row is being added, an 
	*	attriubte combo is created and stored in the attribute combo array at that row position.
	*/
	protected void addFilterRow()
	{
		if (rowCount < 49)
		{
			rowCount++;															//TE: keep the row counter up to date.
							
			panel.newLine();
			
			if (rowCount==1)
			{
				panel.add(andOrCombo = new CBJComboBox(andOrArray));				//TE: combo box ('and', 'or').
				andOrCombo.setPreferredSize(new Dimension(50, 20));
				andOrCombo.setVisible(false);
			}
			else if (rowCount==2)
			{
				panel.add(andOrCombo= new CBJComboBox(andOrArray));				//TE: combo box ('and', 'or').
				
				andOrCombo.addActionListener(new ActionListener(){				//TE: add an action listener that updates the And/Or labels.
					public void actionPerformed(ActionEvent e){				
						try
						{
							for(int i=0; i<labelCount;i++)
								andOrLabel[i].setText(andOrCombo.getSelectedItem().toString());			
						}
						catch(Exception ee)
						{
							log.log(Level.WARNING, "Error updating and/or labels in the search dialog: ", ee);
						}
				}});				
				andOrCombo.setPreferredSize(new Dimension(50, 20));
			}
			else if (rowCount >2)
			{
				andOrLabel[labelCount] = new JLabel();
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
				labelCount++;													    //TE: keep the label counter up to date.
			}			
		
			if (attrs ==null)
			{
				panel.add(attributeCombo[rowCount] = new CBJComboBox());			//TE: the attribute combo with no values ...probably b/c no schema publishing i.e. LDAP V2.
				attributeCombo[rowCount].setEditable(true);						    //TE: ...so allow the user to add some in.
			}
			else
			{
				panel.add(attributeCombo[rowCount] = new CBJComboBox(attrs));		//TE: the attribute combo ('cn', 'sn' etc).
				attributeCombo[rowCount].setRenderer(new CBBasicComboBoxRenderer(attrs));

                int pos = getPriorIndex(attributeCombo, rowCount);                  //TE: select the prior combox box selection if possible.
                if(pos>0)
                    attributeCombo[rowCount].setSelectedIndex(pos);
                else
                    attributeCombo[rowCount].setSelectedItem("sn");	 				//TE: display 'sn' as first choice.
			}
						
			attributeCombo[rowCount].setPreferredSize(new Dimension(140, 20));

			panel.makeLight();
				
			panel.add(functionCombo[rowCount] = new CBJComboBox(functionArray));	//TE: the function combo ('equals', 'beginning with' etc).

            int pos = getPriorIndex(functionCombo, rowCount);                       //TE: select the prior combox box selection if possible.
            if(pos>0)
                functionCombo[rowCount].setSelectedIndex(pos);
            else
                functionCombo[rowCount].setSelectedItem(functionArray[BEGIN]);

            functionCombo[rowCount].addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){								
					for(int i=1;i<=rowCount;i++)								    //TE: if a user selects present or !present, disable text field.
					{
                        checkTextFieldEnabled(i);
					}
			}});
			functionCombo[rowCount].setPreferredSize(new Dimension(140, 20));
			functionCombo[rowCount].setRenderer(new CBBasicComboBoxRenderer(functionArray));

			panel.makeWide();
			panel.add(filterField[rowCount] = new JTextField());					//TE: the filter criteria field (bob, fred etc).
	      	filterField[rowCount].setRequestFocusEnabled(true);
						
			final int row = rowCount;

            // Add a mouse listener so we know when filter field is clicked and so we can check for editor...
            addMouseListener(row);

            checkTextFieldEnabled(rowCount);

			panel.makeLight();

			panel.revalidate();				
		}		
	}

    /**
     * Adds a mouse listener to the filterField box.  The mouse
     * listener checks and opens an editor if any exist for the
     * attribute type.
     * @param row the index of the current row which is used
     * to get the attribute name from the attribute box.  This
     * name is used to determine the syntax of the attribute
     * which in turn determines which (if any) editor is loaded.
     */
    private void addMouseListener(final int row)
    {
        filterField[rowCount].addMouseListener(new MouseListener()              //TE: fix for bug 6466.
        {
            /**
             * Invoked when the mouse button has been clicked (pressed and released) on a component.
             */
            public void mouseClicked(MouseEvent e)
            {
                // Opens special editors...i.e. generalized time editor.
                checkForSpecialEditor(attributeCombo[row].getSelectedItem().toString(), filterField[row]);
            }

            /**
             * Invoked when the mouse enters a component.
             */
            public void mouseEntered(MouseEvent e) {}

            /**
             * Invoked when the mouse exits a component.
             */
            public void mouseExited(MouseEvent e) {}

            /**
             * Invoked when a mouse button has been pressed on a component.
             */
            public void mousePressed(MouseEvent e) {}

            /**
             * Invoked when a mouse button has been released on a component.
             */
            public void mouseReleased(MouseEvent e) {}
        });
    }

    /**
     * Checks if the PRESENT or NOTPRESENT function is selected.
     * If so, disables the text field.  If not enables it.
     * @param row the filter row that we are checking.
     */
    private void checkTextFieldEnabled(int row)
    {
        int item = functionCombo[row].getSelectedIndex();
        if (item == PRESENT || item == NOTPRESENT)
        {
            filterField[row].setEnabled(false);
            filterField[row].setBackground(Color.lightGray);
        }
        else
        {
            filterField[row].setEnabled(true);
            filterField[row].setBackground(Color.white);
        }
    }

    /**
     * Returns the selected item of the combo box indicated to by the value of pos.
     * @param combo an array of combo boxes that we want to find the selected item of
     * a specified element.
     * @param pos indicates which element in the combo array to get the selected
     * index of.
     * @return either the index selected item of the selected combo box, or pos.
     */
    public int getPriorIndex(JComboBox[] combo, int pos)
    {
        pos = (pos <= 0) ? pos : pos -1;
        if(pos > 0)
            return combo[pos].getSelectedIndex();
        else
            return pos;
    }

   /**
    *	Takes an attribute name and checks its syntax against that of the GeneralizedTime
	*	syntax (~121.1.24).  If they are the same the GeneralizedTime editor is opened & the
	*	Search dialog is forced to wait until the user has finished entering date and time info.
	*	This can be expanded to check for other editors - currently it just checks for one.
	*	@param attrName the attribute name - usually from the combo box in the Search dialog (Build
	*				tab).
	*	@param field the text field where the text will be set after the time dialog is closed.
	*/
	protected void checkForSpecialEditor(String attrName, JTextField field)
	{
		if(schema.getAttributeSyntax(attrName).indexOf("121.1.24")>-1)
		{
			gte = new generalizedtimeeditor(browser, field.getText(), false);
			CBUtility.center(gte, browser);    	//TE: centres the attribute editor.
			gte.setVisible(true);
			try
			{
				while (gte.isVisible())
					wait();
					
				field.setText(gte.getTime());
				field.setEditable(false);	 
				field.transferFocus();							
			}
			catch(Exception e)
			{
				log.log(Level.WARNING, "Problem with getting search filter information from editor: ", e);
				field.setText("");				
			}
		}	
		else
		{
			field.requestFocus();
			field.setEditable(true);			
		}				
	}

   /**	
    *	Removes lines (rows) from the filter constructor.  Starting from the last added.
	*/
	protected void removeFilterRow()
	{
		if (rowCount > 1)
		{	
			if(rowCount==2)				
				panel.remove(andOrCombo);
			
			panel.remove(attributeCombo[rowCount]);	
			panel.remove(functionCombo[rowCount]);
			panel.remove(filterField[rowCount]); 	
			
			if (rowCount>2)
			{	
				labelCount--;
				panel.remove(andOrLabel[labelCount]);
			}
			
			panel.repaint();	   
						
			rowCount--;	
		}

		panel.revalidate();
	}	

   /**
    *	Gets a list of attributes that are available in the schema which can be used for 
	*	searching.  These are used in the attributeCombo part of the filter.  
	*	@return a string array of the available attributes to JX (null - if no schema publishing i.e. LDAP V2).
	*/
	protected String[] getAttributes()
	{
        if(schema == null)
        {
            CBUtility.error("Unable to access schema in BuildFilterPanel - no schema available.");
            return new String[] {};
        }

        try
        {

            //ArrayList attributeNames = schema.listEntryNames("schema=AttributeDefinition,cn=schema");
            ArrayList attributeNames = schema.getKnownAttributeNames();
            if(attributeNames==null)		//TE: check for no schema publishing i.e. LDAP V2.
                return null;

            String[] temp = (String[]) attributeNames.toArray(new String[] {});
            Arrays.sort(temp, new CBUtility.IgnoreCaseStringComparator());

            return temp;

        }
        catch (NamingException e)
        {
            CBUtility.error("Unable to access schema in BuildFilterPanel " + e.toString());
            return null;
        }
	}

   /**
    *	Checks if the filter is valid.  A filter is not valid if any attribute or function
	*	combo contains an empty string or a null.
	*	returns true if all combo contain a value, false otherwise.
	*/
	protected boolean isFilterValid()
	{
		try
		{
			for(int i=1; i<=rowCount; i++)
			{
				String attr = attributeCombo[i].getSelectedItem().toString();
				
				if(attr.trim().length()<=0)
					return false;				//TE: check if the attribute combo has a value.
					
				String func = functionCombo[i].getSelectedItem().toString();
						
				if(attr.trim().length()<=0)		//TE: check if the function combo has a value.
					return false;
					
				if (rowCount>=2)
				{		
					String andOr = andOrCombo.getSelectedItem().toString();
			
					if(andOr.trim().length()<=0) 		//TE: check if the and/or combo has a value.
						return false;
				}					
			}
		}
		catch(Exception e)						//TE: incase the row count has been messed up.
		{
			return false;
		}
		
		return true;
	}					
			
   /**
    *	Constructs the filter by adding filter parts to a string buffer for each row in the filter constructor.
	*	@return the filter.
	*/
	protected String getFilter()
	{	
		StringBuffer buffy = new StringBuffer();

		for(int i=1;i<=rowCount;i++)				
			buffy.append(getFunctionPart(i));			

		if (rowCount>=2)
		{
			switch (andOrCombo.getSelectedIndex())
			{
				case 1: { buffy.insert(0, "(|"); break; }       //TE: 'Or' option.
				default: { buffy.insert(0, "(&"); break; }		//TE: 'And' option.
			}
			
			buffy.append(")");
		}		
		
		if (notCheckBox.isSelected())
		{
			buffy.insert(0, "(!");
			buffy.append(")");	
		}
								
		return buffy.toString();
	}

   /**
    *	Constructs the body ('(cn=w*)') of the filter by reading the attribute type selected (i.e. 'cn'),
	*	the function (i.e. 'Beginning With') and reading the actual filter (i.e. 'w') for a given row 
	*	number.  
	*	@param i the row number that is being processed.
	*   @return returns the function string which is then appended to a global string 
	*   	buffer that is used in the search.
	*/
	protected String getFunctionPart(int i)
	{
		String attr = attributeCombo[i].getSelectedItem().toString();
		String func = functionCombo[i].getSelectedItem().toString();
		String text = filterField[i].getText();
	
		int index = functionCombo[i].getSelectedIndex();
		
		text = replace(text, "\\", "\\5c");		//TE: escapes any of the following characters: *, (, ), \.  WARNING: the '\\' replace should always go first.
		text = replace(text, "(", "\\28");
		text = replace(text, ")", "\\29");
		text = replace(text, "*", "\\2A");
		
		switch (index)
		{
			case BEGIN:			return "("   + attr + "="  + text + "*)";	//TE: (cn=f*)
			case NOTBEGIN:		return "(!(" + attr + "="  + text + "*))";	//TE: (!(cn=f*))
			case CONTAINING:	return "("   + attr + "=*" + text + "*)";	//TE: (cn=*f*)
			case NOTCONTAINING:	return "(!(" + attr + "=*" + text + "*))";	//TE: (!(cn=*f*))
			case EQUALS:		return "("   + attr + "="  + text + ")";	//TE: (cn=f)
			case NOTEQUALS:		return "(!(" + attr + "="  + text + "))";	//TE: (!(cn=f))
			case ENDING:		return "("   + attr + "=*" + text + ")";	//TE: (cn=*f)
			case NOTENDING:		return "(!(" + attr + "=*" + text + "))";	//TE: (!(cn=*f))
			case GREATER:		return "("   + attr + ">=" + text + ")";	//TE: (cn>=f)
			case NOTGREATER:	return "(!(" + attr + ">=" + text + "))";	//TE: (!(cn>=f))
			case LESS:			return "("   + attr + "<=" + text + ")";	//TE: (cn<=f)
			case NOTLESS:		return "(!(" + attr + "<=" + text + "))";	//TE: (!(cn<=f))			
			case PRESENT:		return "("   + attr + "=*)";				//TE: (cn=*)
			case NOTPRESENT:	return "(!(" + attr + "=*))";				//TE: (!(cn=*))
			case SIMILAR:		return "("   + attr + "~=" + text + ")";	//TE: (cn~=f)
			case NOTSIMILAR:	return "(!(" + attr + "~=" + text + "))";	//TE: (!(cn~=f))
			default: 			return "";
		}
	}

   /**
    *	Takes a string and replaces any occurances of a given string with another given string.
	*	@param string the string that contains the text to be replaced.
	*	@param oldString the text to be replaced.
	*	@param newString the text to take the place of the old text.
	*	@return the string with the new text in place.
	*/
	protected String replace(String string, String oldString, String newString)
	{
		int pos = -1;
		
		try
		{
			while ((pos = string.indexOf(oldString, pos+1))!=-1)
			{
				string = string.substring(0,pos)+newString+string.substring(pos+oldString.length());
			}
			return string;
		}
		catch(Exception e)
		{
			return string;	
		}
	}
}

