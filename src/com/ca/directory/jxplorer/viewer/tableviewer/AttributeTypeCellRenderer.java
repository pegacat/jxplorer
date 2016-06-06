package com.ca.directory.jxplorer.viewer.tableviewer;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.*;
import java.awt.*;
    /*
     *    A minimal extension of DefaultTableCellRenderer allowing
     *    us to set some rows to bold font and others to normal font.
     *
     */
                                               
public class AttributeTypeCellRenderer extends DefaultTableCellRenderer

{
    Font normalFont;
    Font boldFont;
    Font boldBlueFont;
    /**
     *    constructor calls the super class constructor, and
     *    initialises the fonts.
     */
    public AttributeTypeCellRenderer() 
    {
        super();
        normalFont = this.getFont();
        boldFont = normalFont.deriveFont(java.awt.Font.BOLD);
        boldBlueFont = normalFont.deriveFont(java.awt.Font.BOLD);
    }

    /**
     *    intercepts the super classes returned component, and sets the
     *    font on it before returning.
     */    
    public Component getTableCellRendererComponent(JTable table, Object value, 
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        if (value instanceof AttributeNameAndType)
        {
            AttributeNameAndType attType = (AttributeNameAndType)value;
            
            Component c = super.getTableCellRendererComponent(table, attType.getName(), isSelected, hasFocus, row, column);
            
            if (attType.isMandatory())
            {
                c.setFont(boldFont);
                c.setForeground(Color.black);
            }
            else if (attType.isOperational())
            {
                c.setFont(boldFont);
                c.setForeground(new Color(64,96,64));
                //c.setForeground(Color.red);
            }
            else
            {
                c.setFont(normalFont);    
                c.setForeground(Color.black);
            }

            if ((row%2)==1)
                c.setBackground(new Color(249,249,249));
            else
                c.setBackground(Color.white);

            return c;                
            
        }
        else
        {
            System.out.println("WHY are we here?");
            return super.getTableCellRendererComponent(table, new String("error"), isSelected, hasFocus, row, column);
        }
    }    
}