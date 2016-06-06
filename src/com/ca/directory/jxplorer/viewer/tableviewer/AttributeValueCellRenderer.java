package com.ca.directory.jxplorer.viewer.tableviewer;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.*;
import java.awt.*;

import com.ca.commons.cbutil.CBIntText;

    /*
     *    A minimal extension of DefaultTableCellRenderer allowing
     *    us to set some rows to bold font and others to normal font.
     *
     */
                                               
public class AttributeValueCellRenderer extends DefaultTableCellRenderer

{
    Font normalFont;
    Font boldFont;
    Font boldBlueFont;
    
    /**
     *    constructor calls the super class constructor, and
     *    initialises the fonts.
     */
     
    public AttributeValueCellRenderer() 
    {
        super();
        normalFont = this.getFont();
        boldFont = normalFont.deriveFont(java.awt.Font.BOLD);
        boldBlueFont = normalFont.deriveFont(java.awt.Font.BOLD);
    }

    // is this really needed?
    public String truncateLongString(String truncateMe, int len)
    {
        //return truncateMe;
        return truncateMe.substring(0, len) + "...";
    }        

    /**
     *    intercepts the super classes returned component, and sets the
     *    font on it before returning.
     */    
     
    public Component getTableCellRendererComponent(JTable table, Object value, 
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        if (value instanceof AttributeValue)
        {
            AttributeValue attVal = (AttributeValue)value;

            //String id = attVal.getID();
            //if (id.equalsIgnoreCase("telephoneNumber"))
            //    System.out.println("ph: " + attVal.getStringValue());

            String attString = attVal.toString();
            if (attString.length() > 256)
                attString = truncateLongString(attString, 256);
            
            Component c = super.getTableCellRendererComponent(table, attString, isSelected, hasFocus, row, column);
            
            if (attVal.isNaming())
            {
                c.setForeground(((isSelected)?Color.white:Color.blue));
                c.setFont(boldFont);
                if (!isSelected)
                {
                    if ((row%2)==1)
                        c.setBackground(new Color(249,249,249));
                    else
                        c.setBackground(Color.white);
                }
            }
            else if (attVal.isEditable()==false)
            {
                c.setFont(boldFont);
                c.setForeground(new Color(64,96,64));
                //c.setForeground(Color.red);

                if ((row%2)==1)
                    c.setBackground(new Color(249,249,249));
                else
                    c.setBackground(Color.white);
            }
            else
            {
                c.setForeground(((isSelected)?Color.white:Color.black));
                c.setFont(normalFont);
                if (!isSelected)
                {
                    if ((row%2)==1)
                        c.setBackground(new Color(249,249,249));
                    else
                        c.setBackground(Color.white);
                }
            }
            return c;                
            
        }
        else    
            return super.getTableCellRendererComponent(table, new String("error"), isSelected, hasFocus, row, column);
    }    
}