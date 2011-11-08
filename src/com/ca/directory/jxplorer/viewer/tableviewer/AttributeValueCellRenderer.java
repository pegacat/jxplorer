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

    /**
     *  Intercepts byte array/binary attribute values, and substitutes the
     *  string '(non string data)' for display to the user...
     */
     
    protected void setValue(Object value)  // I wonder what the performance hit here is...
    {
        /* not actually used???
        if (value instanceof AttributeValue)
        {
            String id = ((AttributeValue)value).id; 
            if (id.equals("userPassword"))
                System.out.println("bloop");

            if (((AttributeValue)value).isBinary() && (((AttributeValue)value).isEmpty()==false))
                value = CBIntText.get("(non string data)");
            else
            {
                // truncate long strings for initial display
                String stringVal = value.toString();
                if (stringVal.length() > 100)
                    value = truncateLongString(stringVal);
                    
                if (stringVal.substring(0,6).toLowerCase().startsWith("<html>"))
                    value = " " + stringVal;
            }                    
        }
        */
        super.setValue(value);
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

            String id = attVal.getID();
            if (id.equalsIgnoreCase("userPassword"))
                System.out.println("pwd va: " + attVal.getStringValue());

            String attString = attVal.toString();
            if (attString.length() > 256)
                attString = truncateLongString(attString, 256);
            
            Component c = super.getTableCellRendererComponent(table, attString, isSelected, hasFocus, row, column);
            
            if (attVal.isNaming())
            {
                c.setForeground(((isSelected)?Color.white:Color.blue));
                c.setFont(boldFont);
            }    
            else
            {
                c.setForeground(((isSelected)?Color.white:Color.black));
                c.setFont(normalFont);    
            }    
            return c;                
            
        }
        else    
            return super.getTableCellRendererComponent(table, new String("error"), isSelected, hasFocus, row, column);
    }    
}