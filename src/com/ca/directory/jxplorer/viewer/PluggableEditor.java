package com.ca.directory.jxplorer.viewer;

import com.ca.directory.jxplorer.DataSink;

import java.awt.Component;
import javax.swing.*;


/**
 *    PluggableEditor defines the interface used by 'pluggable' objects that
 *    display data to the user in JXPlorer.  Objects that implement this
 *    interface, are placed in the package com.ca.directory.jxplorer.viewer, and
 *    are given the class name of a particular schema object class will be
 *    dynamically loaded when the JXPlorer user attempts to view that 
 *    object class.<p>
 *    For example, a class implementing this interface, called 
 *    'person.class', and placed in the com/cai/od/jxplorer/viewer directory,
 *    would be automatically loaded whenever the user accessed a 'person' entry,
 *    or a 'person' derived entry, in the directory.<p>
 *    Note that the class should be given the completely <b>lower case</b> name of the
 *    object class being modelled, and be aware that this breaks the java class naming
 *    convention.  (E.g. use 'organizationalunit.java' rather than either 'OrganizatonalUnit.java'
 *    or 'organizationalUnit.java').<p>
 * @author Chris Betts
 */
 
public interface PluggableEditor
{
    /**
     *    This returns the swing component being used to display
     *    the data, so that another component can set its visibility.<p>
     *    In many cases this may be the editor itself.
     *    @return the java.swing component being used to display the node data.
     */
     
    public JComponent getDisplayComponent();
    
    /**
     *    This returns the component that should be used to print
     *    the data (e.g. a full table of data that spans several pages).<p>
     *    A minimal implementation can simply return getDisplayComponent().
     *    @return the component being used to print the node data.
     */
    public Component getPrintComponent();
    
     /**
      *    Returns the data sink that will be used to slurp data.<p>
      *    In many cases this may be the editor itself.
      *    @return a data sink to display or process data.
      */
      
     public DataSink getDataSink();

    
    /**    
     *    This sets whether the component expects to be the <i>only</i>
     *    editor used for its object class, or whether it will co-exist
     *    with any other applicable editors, and the default (html/table)
     *    editor.
     */
     
    public boolean isUnique(); 
    
    /** 
     *    Returns the title of the editor.  (This might be displayed on a 
     *    tab or similar).  If null, the object class name will be used
     *    instead.
     */
     
     public String getName();
     
    /**
     *   Returns the icon of the editor.  (This might be displayed on a tab or similar).
     *   .
     */   
     
     public ImageIcon getIcon();  
     
    /**
     *    Returns the Tool Tip of the editor.  (This might be displayed on a tab or similar).
     *    .
     */    
         
     public String getToolTip();       
     
     
    /**
     *    Registers Swing components that the pluggable editor may manipulate.
     *    Any or all of these may be null if the editor is not permitted to
     *    manipulate the objects. <p>
     *
     *    Any changes made by the editor should be removed when the editor is
     *    unloaded.
     *
     *    @param menu the main menu that appears on the top of JXplorer
     *    @param buttons the JXplorer button bar
     *    @param tree the JXplorer tree
     *    @param treeMenu the popup menu that JXplorer uses for tree items
     *    @param jxplorer the root JXplorer object, from which the entire GUI tree descends.  (You need this for
     *           look & feel updates, and possibly for paintology)
     */ 
          
     public void registerComponents(JMenuBar menu, JToolBar buttons, JTree tree, JPopupMenu treeMenu, JFrame jxplorer);
     
     
     /**
      *    This method is called when the editor is being unloaded by the browser,
      *    and allows for any required clean up activity.  Note that this is <i>not</i>
      *    necessarily called when JXplorer shuts down.
      */
      
     public void unload(); 
     
     /**
      *    If a UNIQUE pluggable editor exists for an entry, the display tree will 
      *    call this method to see if a special icon is desired.  At this stage it
      *    is quite likely that the entry will not have been fully read, so that all
      *    that is known will be the entry name and object classes.<p>
      *
      *    Most pluggable editors will not bother implementing this class and will just
      *    return null.  Only pluggable editors with very special needs should consider
      *    implementing this: otherwise the normal naming attribute or object class
      *    based icons should be sufficient.  
      *
      *    @param rdn the name of the tree node being displayed (i.e. the rdn of the entry)
      *    @return an image icon to display - usually 'null' to revert to default behaviour.
      */
      
     public ImageIcon getTreeIcon(String rdn);
     
     
     /**
      *    If a UNIQUE pluggable editor exists for an entry, the display tree will
      *    call this method to see if a special pull down menu ('right click menu')
      *    is desired.  Most pluggable editors will use the default behaviour and
      *    will simply return null.
      *    @param rdn the name of the tree node being displayed (i.e. the rdn of the entry)
      *    @return a popup menu to use for this type of entry - usually 'null' to revert to default behaviour.
      */
      
     public JPopupMenu getPopupMenu(String rdn);
     
     
     /**
      *    If a UNIQUE pluggable editor exists for an entry, the display tree will
      *    call this method to see if it should hide the subtree below this entry
      *    because the pluggable editor is directly handling this data.<p>
      *
      *    Most pluggable editors will use the default behaviour and return 'false'
      *    to allow user browsing of sub entries.
      *
      *    @param rdn the name of the tree node being displayed (i.e. the rdn of the entry)
      *    @return whether to truncate the subtree (normally 'false').
      */
      
      public boolean hideSubEntries(String rdn);


    /**
     * Checks whether the editor has unsaved data changes.
     * This may be triggered by the UI when the user navigates away from an editor, and gives the
     * editor a chance to prompt the user to save those changes...
     * @return
     */
      public void checkForUnsavedChanges();

}