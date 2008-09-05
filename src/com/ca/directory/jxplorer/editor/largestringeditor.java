package com.ca.directory.jxplorer.editor;

import com.ca.commons.cbutil.*;
import javax.swing.*;
import java.awt.*;

/**
 *   Creates a simple text editor for displaying very
 *   large text attributes. 
 *   @author Chris.
 */
public class largestringeditor extends CBDialog
    implements abstractstringeditor
{
    protected JTextArea          area;    
    protected JScrollPane        scroll; 
    protected editablestring editableText;

    /**
     * Construct the GUI, but do not initialise it with data (yet).
     */
    public largestringeditor(Frame owner, editablestring text)
    { 
        super(owner, CBIntText.get("Simple Text Editor"), null); 
        setModal(true); 
        
        area = new JTextArea();
        area.setLineWrap(true);		//TE: wrap the lines.
		
        scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);		//TE: no horizontal scroll.
        scroll.setViewport(new CBViewport());
        scroll.getViewport().setView(area);
        scroll.setPreferredSize(new Dimension(310,60));
		
        makeHeavy();
        addln(scroll);
        
		setSize(500, 400);
        
        setStringValue(text);
    } 									   

   /**
    * Sets the attribute value in the text area of the attribute editor.
    * @param originalText
    */
    public void setStringValue(editablestring originalText)
    {       
        editableText = originalText;
		area.setText(editableText.getStringValue()); 
        JViewport port = scroll.getViewport();
        if (port != null)
            port.setViewPosition(new Point(0,0));
        else
            System.out.println("NULL VIEWPORT");    
    }     

   /**   
    * Sets the changed attribute value from the attribute editor in the table.
    */  
    public void doOK()
    {   
        String newAttribute = area.getText();
        
        //sets the attribute value to reflect the changes 
        //made in the attribute editor.
        
        editableText.setStringValue(newAttribute);        

        super.doOK();
    }
}