package com.ca.commons.cbutil;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;


/**
 * This class basically allows us to set the render of a JComboBox.  More to the point...
 * This is what we have to use to get text tool tip working on each menu item!
 *
 * @author Trudi.
 */

public class CBBasicComboBoxRenderer extends BasicComboBoxRenderer
{
    public Object[] text;	//TE: array of the values to be used in the combo box and to be used as their tooltips..

    /**
     * Constructor that basically assigns the text variable.
     *
     * @param txt the values that are to be added to the combo box and also used as tooltips respectfully.
     */

    public CBBasicComboBoxRenderer(Object[] txt)
    {
        super();
        text = txt;
    }

    /**
     * Defines the getListCellRendererComponent method of BasicComboBoxRender to add
     * tool tips and text to combo boxes in the super search dialog.
     * <p/>
     * Following copied from interface: javax.swing.ListCellRenderer...
     * Returns a component that has been configured to display the specified value.
     * That component's paint method is then called to "render" the cell. If it is
     * necessary to compute the dimensions of a list because the list cells do not
     * have a fixed size, this method is called to generate a component on which
     * getPreferredSize can be invoked.
     *
     * @param list         - The JList we're painting.
     * @param value        - The value returned by list.getModel().getElementAt(index).
     * @param index        - The cells index.
     * @param isSelected   - True if the specified cell was selected.
     * @param cellHasFocus - True if the specified cell has the focus.
     * @return a component whose paint() method will render the specified value.
     */

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        if (isSelected)
        {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());

            if (index > -1)
            {
                list.setToolTipText(value.toString());//(text[index].toString());
                setText(text[index].toString());	//TE: sets the tool tips and the text of each item in the combo.
            }
        }
        else
        {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        setFont(list.getFont());
        setText((value == null) ? "" : value.toString());
        return this;
    }
}