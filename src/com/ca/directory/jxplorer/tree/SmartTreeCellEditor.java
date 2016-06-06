package com.ca.directory.jxplorer.tree;

import com.ca.commons.cbutil.*;
import com.ca.commons.naming.RDN;

import javax.naming.NamingException;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

/**
 *    This class provides a (potentially) multi-field editor for editing 
 *    multi-valued RDNs, and provides a number of sub-classes to allow
 *    it to be integrated into the JTree class.
 */
 
public class SmartTreeCellEditor extends DefaultTreeCellEditor
{
    Panel display;
    TextField standard;
    
    boolean useIcons = true;
 
    Object value = null;
 
    /**
     *    Default constructor.  All the intelligence is added by overloading
     *    the methods below.
     *    @param tree the tree the editor will be used in.
     *    @param renderer the existing renderer, used to obtain icons from.
     */
     
    public SmartTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) 
    {  
        super(tree, renderer); 
        display = new Panel();
        display.setBackground(Color.green);
        display.add(standard = new TextField("bloop"));
    }

    protected boolean shouldStartEditingTimer(EventObject event)
    {
        // supress editing timer when a popup operation is in progress...
        if (((SmartTree)tree).popupToolVisible() == true)
            return false;
        //else if (((SmartTree)tree).isModifiable() == false)  // unnecessary - simply setEditable(false) in SmartTree
        //    return false;
        else
            return super.shouldStartEditingTimer(event);
    }

    /**
     * overload the standard DefaultTreeCellEditor method that
     * creates the 'inner' editor used to display data.
     *
     * @return a custom tree cell editor, being a 'MyTreeCellEditor'
     *         object (as defined below).
     */
     
    protected TreeCellEditor createTreeCellEditor() 
    {

        MyTreeCellEditor editor = new MyTreeCellEditor();      
        // One click to edit.
        editor.setClickCountToStart(1);
        return editor;
        
    }

    /**
     *    Quick hack to disable editing during drag and drop operations.
     *    @param e the event to disable if dragging occuring.
     */
     
    public void actionPerformed(ActionEvent e)
    {
        if (((SmartTree)tree).dragging == true) 
            return;
            
        super.actionPerformed(e);
    }

    /**
     *    Hacked to use SmartNode icon, so we don't see the icon
     *    changing during editing.
     *
     */
     
    protected void determineOffset(JTree tree, Object value,
                   boolean isSelected, boolean expanded,
                   boolean leaf, int row) 
    {    
        if(value != null) 
        {
            editingIcon = ((SmartNode)value).getIcon();
            
            if (editingIcon != null)
                offset = 4 + editingIcon.getIconWidth();
            else
                offset = 4;
        }
        else 
        {
            editingIcon = null;
            offset = 0;
        }
    }
    
    /**
     *    Returns the value of the tree entry currently being edited.
     * 
     *    @return  the value of the tree entry currently being edited
     */
     
    public Object getCellEditorValue()
    {
        try
        {
            SmartNode node = (SmartNode)value;
            if (node.isMultiValued())
            {
                return value.toString();       
            }
        }
        catch (Exception e) {} // we don't care - even if it's not a SmartNode, just keep going.            

        return super.getCellEditorValue();
    }

    /**
     *    Extend the base method to nobble editing of 'structural' nodes.
     */
     
    public boolean isCellEditable(EventObject event)
    {
        if (tree == null)   // this should never happen.  But if there's no tree,
            return false;   // don't edit it!     

        if (((SmartTree)tree).isModifiable() == false)              // unnecessary?  setEditable(false) in SmartTree instead?
            return false;   // don't edit read-only trees!

        TreePath path = tree.getSelectionPath();
        
        if (path == null)   // no path == no editing!
            return false;
        
        SmartNode node =  (SmartNode)path.getLastPathComponent();

        if (node == null)  // if there's no smart node, don't edit that either!
            return false;

        
        /*
         *    The point of this method: don't allow structural, dummy or root
         *    nodes to ever be edited.
         */
                     
        if (node != null && node.isDummy() || node.isStructural() || node.isRoot())
            return false;            

        return super.isCellEditable(event);                
    }





    /**
     *    This is the actual GUI component shown to the user.  (It can be confusing to 
     *    work out that this is the object, because of the number of layers of intermediate
     *    components, DefaultCellEditors, EditorDelegates yada yada yada...
     */
     
    class CellEditorGUI extends CBPanel
    {
        ActionListener listener;   // bit of a hack - we only allow one listener
    
        boolean multi = false;     // whether currently displaying a multi-valued att.
    
        JTextField first;          // there is always at least one text field available - and this is it!
        
        Border thinBorder;         // We're very short of room when editing a tree cell, so we have the thinnest border possible.
        
        SmartNode node;            // A local pointer to the node being edited.

        RDN displayRDN;            // the original RDN to display.

        /**
         *    The constructor sets up our panel.  We extend CBPanel with its
         *    GridBagLayout to allow us to space the components with thin '+'
         *    signs between fat text fields in the multi-valued RDNs.  The
         *    constructor does little except set up the border, and construct
         *    the 'first' text field (which is always present).
         */
         
        CellEditorGUI()
        {
            makeHeavy();
            thinBorder = UIManager.getBorder("Tree.editorBorder");
            first = new JTextField();
            first.setBorder(thinBorder);
            first.setText("bloop!");
            add(first); 

        }

     
        /**
         *    Sets up the GUI to display a (possibly multi-valued)
         *    RDN (accessed via the passed SmartNode).  This
         *    creates the necessary text fields (and any 'plus sign'
         *    labels) to populate this object for display.
         *    @param node the node to edit, containing a potentially
         *           multi-valued rdn.
         */

        void setValue(SmartNode node)
        {
            this.node = node;

            RDN rdn = node.getRDN();

            int size = rdn.size();

            multi = node.isMultiValued();
        
            trimExcessComponents();           // just clearing the decks.
        
            Dimension prefSize = renderer.getPreferredSize();
        
            setSize(prefSize);

            first.setText(rdn.getRawVal(0));  // this is the last step for single valued RDNs
    
            /*
             *    Handle possible multi-valued components by
             *    alternately adding '+' sign labels and
             *    more text fields.
             */
             
            for (int i=1; i<size; i++)
            {
                makeLight();
                JLabel plus = new JLabel("+");
                plus.setForeground(Color.red);
                add(plus);
                makeHeavy();
                JTextField nextField = new JTextField(rdn.getRawVal(i));
                nextField.setBorder(thinBorder);
                nextField.addActionListener(listener);
                add(nextField);
            }    
            
        }
     
        /**
         *    Removes all fields, except the first text field.
         */
         
        protected void trimExcessComponents()
        {
            for (int i=getComponentCount()-1; i>0; i--)
            {
                remove(i);
            }
        }
         
     
        /**
         *    Constructs the RDN from the text fields the user
         *    has edited.
         *    @return the newly edited RDN.
         */
         
        RDN getValue()
        {
            RDN returnRDN = node.getRDN();
            
            try
            {
/*                
                if (multi==false)
                {
                    returnRDN.setRawVal(first.getText());
                    return returnRDN;
                }
                else
                {
*/                
                    int size = getComponentCount();
                    for (int i=0; i<size; i = i+2)
                    {
                        JTextField currentTextField = (JTextField) getComponent(i);
                        String text = currentTextField.getText();
                        text = text.trim();
                        if (text.length() == 0)
                            text = " ";
                        returnRDN.setRawVal(text, (i/2));
                    }
                    return returnRDN;
//                }
            }
            catch (NamingException e)
            {
                CBUtility.error(CBIntText.get("Invalid Name"), null);
//TODO:  this needs work, as it currently returns an invalid rdn below!


            }
            return returnRDN;              
        }
        
        /**
         *    <p>A bit of a hack occuring here - we only really allow for
         *    a single listener, and we add that to all the component text
         *    fields created for the multi-valued rdns.</p>
         *
         *    <p>If we ever needed to do this properly, we could eliminate 
         *    this method altogether, and read the complete listener list
         *    in 'setMultiValued(...)'.</p>
         *
         *    @param l the sole listener for this object.
         *
         */
         
        void addActionListener(ActionListener l)
        {
            listener = l;
            first.addActionListener(l); 
        }
        
       /**
         * Overrides <code>JTextField.getPreferredSize</code> to
         * return the preferred size based on current font, if set,
         * or else use renderer's font.
         * @return the preferred size for the cell renderer (as it appears in the tree).
         */
         
        public Dimension getPreferredSize() 
        {
            Dimension      size = super.getPreferredSize();
            
            // If not font has been set, prefer the renderers (the SmartTreeCellRenderers) height.
            if(renderer != null ) 
            {
                Dimension     rSize = renderer.getPreferredSize();
                size.height = rSize.height;
            }
            
            if (multi)
                size.width += 30;  // add a bit of elbow room for multi-valued rdn...
    
            return size;
        }
    }

    /**
     *    A custom CellEditor that extends DefaultCellEditor, and changes the
     *    internal editor delegate used to be the CellEditorGUI defined above.
     *    (why they didn't allow DefaultCellEditor to be created with an
     *    arbitrary editor component, who knows - the architecture is all there.).
     *
     */
     
    public class MyTreeCellEditor extends DefaultCellEditor
    {
    
        CellEditorGUI cellDisplay;
        
        /**
         *    Constructor automatically creates the editor with a 
         *    CellEditorGUI delegate.
         */
         
        public MyTreeCellEditor()
        {
            super(new JCheckBox()); // needed, because Sun screwed up writing DefaultCellEditor.
    
            cellDisplay = new CellEditorGUI();
            
            editorComponent = cellDisplay;
            
            this.clickCountToStart = 2;
            
            delegate = new EditorDelegate() 
            {
                public void setValue(Object value) 
                {
                    cellDisplay.setValue((SmartNode)value);
                }
    
                public Object getCellEditorValue() 
                {
                    return cellDisplay.getValue();
                }
            };
            
            cellDisplay.addActionListener(delegate);
        }
        
        /**
         *    Returns the editor component, which is the internal CellEditorGUI 
         *    object created in the constructor, after setting the value it is
         *    to display/edit.
         */
            
        public Component getTreeCellEditorComponent(JTree tree, Object value,
                            boolean isSelected,
                            boolean expanded,
                            boolean leaf, int row) 
        {
            cellDisplay.setBackground(tree.getBackground());
            delegate.setValue(value);
            return editorComponent;
        }
        


        
    }

}