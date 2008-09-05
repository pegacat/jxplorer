package com.ca.directory.jxplorer.tree;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 *    SmartTreeCellRenderer replaces the DefaultTreeCellRenderer.  The display is
 *    modified so that the tree elements are displayed similarly to a windows
 *    env. throughout all UIs (the default Motif display was (April '99) 
 *    way broken; black on dark grey display was difficult to read and looked 
 *    horrible).  This class also handles the objectClass sensitive display
 *    icons. <p>
 *    
 *    As with most swing components, the tree display is horribly broken.
 *    Despite the use of a custom renderer, icons larger than 16x16 appear
 *    to be truncated in any look and feel apart from 'java'.  Surprise?  I'm
 *    about to fall over and die from not-Surprise.
 *
 */
 
public class SmartTreeCellRenderer extends DefaultTreeCellRenderer
{
    boolean useIcons = true;
 
    public JLabel displayLabel = new JLabel("label");
 
    private static final Color BLUE = new Color(0x000077);     
    private static final Color WHITE = Color.white;
    private static final Color BLACK = Color.black;
    
    private Color currentBackground = null;
    private Color currentForeground = null;
    private ImageIcon currentIcon = null;
    
    /**
     *    Default constructor.  All the intelligence is added by overloading
     *    the methods below.
     */
    public SmartTreeCellRenderer() 
    {  
        super(); 
    }

    /** 
     *
     *    Constructor setting whether to use icons when rendering cells
     *    (otherwise defaults to true).
     *
     *    @param usingIcons whether icons are active.
     */
            
    public SmartTreeCellRenderer(boolean usingIcons) 
    {      
        this();  
        useIcons = usingIcons; 
    }

    /**
     *    Overloaded renderer is used to a) get the foreground and background
     *    working sanely on all platforms (was screwed under motif), and
     *    to set up our 'node-type-dependant' icon images, rather than the
     *    default 'open' or 'close' icon images.<p>
     *
     *    <i>If</i> the passed object implements a 'getIcon()' method returning a
     *    javax.swing.Icon, and useIcons
     *    is true, the getIcon method will be used to display a graphic
     *    (implemented via reflection in order to conform with treeCellRenderer interface... :-) ).
     *
     *    @param tree     the tree to display this component within
     *    @param value    the object to display
     *    @param selected has the object been selected by the user?
     *    @param expanded is the object tree node displaying children?
     *    @param leaf     is the object without child nodes?
     *    @param row      the display row of the object.
     *    @param hasFocus whether the object has GUI focus (which is different from 'selected',
     *                    but here is displayed the same, since we don't allow multiple selections)
     */
     
    public Component getTreeCellRendererComponent(JTree tree, Object value,
        boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
    {       
        Object icon = null;
	
		if (value instanceof SmartNode)
		{
			SmartNode node = (SmartNode) value;	
		
	        if (selected) 
	        /* only one entry should ever be selected, so we know we
	         * can't reuse the label without changing the colors...
	         */ 
	        {			
				currentForeground = WHITE;
				currentBackground = BLUE;
				displayLabel.setForeground(currentForeground);			 
				displayLabel.setBackground(currentBackground);
				displayLabel.setOpaque(true);              			
	        }
	        else
	        /*  This is the most common option - so we can usually
	         *  reuse the label as is.
	         */         
			{
	            if (currentBackground != WHITE)  
	            /*
	             *    We've just come from a selected option - reset
	             *    the colours.
	             */
	            {
					currentForeground = BLACK;
					currentBackground = WHITE;
					displayLabel.setForeground(currentForeground);			 
					displayLabel.setBackground(currentBackground);				
					displayLabel.setOpaque(false); 
	            }
	        }			


			//TE: what kind of node is it & how to handle the label/text...
	        if (node.isMultiValued() == true)  				//TE: multivalued RDN.
			{	
	            StringBuffer buffy = new StringBuffer();
                buffy.append(node.rdn.getRawVal(0));
                int size = node.rdn.size();
	            for (int i=1; i<size; i++)
	            {
	                buffy.append(" + ");	//TE: this + symbol should be red to distinguish it from being part of the naming attribute.
                    buffy.append(node.rdn.getRawVal(i));
	            }
		
				displayLabel.setText(buffy.toString());
			}
	        else if (node.isDummy())
	        {
	            displayLabel.setText(node.getDummyMessage());
	        }
	        else if (node.isBlankRoot())
	        {	
	            displayLabel.setText(node.getBlankRootName());
	        }
	        else   											//TE: normal node.	
	        {
                displayLabel.setText(node.rdn.getRawVal(0));
	        }				
			
			
			//TE: set the icon...
	        try
	        {
	            /*
	             *    Only reset the icon if it has actually changed...
	             */
	             
	            if (node.getIcon() != currentIcon)
	            {
	                currentIcon = node.getIcon();
	                displayLabel.setIcon(currentIcon);
	            }                            
	        }
	        catch (Exception e) // If this fails, we'll just lose the icon
	        {
				//TE: ignore.
	        } 			


	        return displayLabel;
		}
		else
		{
			displayLabel.setText(value.toString());  //TE: this happens (for some reason) on Linux & Solaris if the Java L&F is set!
			return displayLabel;
		}
    }
}