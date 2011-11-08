package com.ca.directory.jxplorer.viewer;

import java.awt.*;
import javax.swing.*;

import com.ca.directory.jxplorer.broker.DataBrokerQueryInterface;
import com.ca.directory.jxplorer.DataSink;
import com.ca.commons.naming.DXEntry;
import com.ca.commons.cbutil.CBIntText;
import com.ca.commons.cbutil.CBHelpSystem;
import com.ca.commons.cbutil.Theme;

import javax.swing.ImageIcon;



/**
 *    This is a Basic 'PluggableEditor' that it may be useful to inherit from.
 *    It is no more than a read-only text display of an entry, with no-op methods for
 *    for most of the PluggableEditor and DataSink interface methods.
 * @author Chris Betts
 *
 */
 

public class BasicPluggableEditor extends JPanel
    implements DataSink, PluggableEditor
{
    
    JEditorPane basicDisplay = null;

    /**
     *    A Simple constructor that creates the JEditorPane.
     */
    public BasicPluggableEditor() 
    {
        setVisible(true);
    }
    
    protected void initDefaultEditor(String bloop)
    {
        basicDisplay = new JEditorPane("text/plain",bloop);
        basicDisplay.setEditable(false);
        basicDisplay.setFont(new Font("Monospaced",Font.PLAIN,12));
        this.removeAll();
        this.setLayout(new BorderLayout());
        this.add(basicDisplay, BorderLayout.CENTER);
        this.repaint();
        this.invalidate();
        
    }
    
    /**
     *    Displays the data in an entry as a plain text list.
     */
     
    public void displayEntry(DXEntry entry, DataBrokerQueryInterface ds)
    {   
        String bloop = entry.toString();
        initDefaultEditor(bloop);                        
    }

    /**
     *    This returns the swing component being used to display
     *    the data, so that another component can set its visibility.<p>
     *    In many cases this may be the editor itself.
     *    @return the java.swing component being used to display the node data.
     */
     
    public JComponent getDisplayComponent()
    {
        return this;
    }
    
    /**
     *    This returns the component that should be used to print
     *    the data (e.g. a full table of data that spans several pages).<p>
     *    The default implementation simply returns getDisplayComponent().
     *    @return the component being used to print the node data.
     */
    public Component getPrintComponent()
    {
        return getDisplayComponent();
    }
    
     /**
      *    Returns the data sink that will be used to slurp data.<p>
      *    In many cases this may be the editor itself.
      *    @return a data sink to display or process data.
      */
      
     public DataSink getDataSink()
     {
     
         return this;
     }

    
    /**    
     *    This sets whether the component expects to be the <i>only</i>
     *    editor used for its object class, or whether it will co-exist
     *    with any other applicable editors, and the default (html/table)
     *    editor.
     */
     
    public boolean isUnique()
    {
        return false;
    }
    
    /** 
     *    Returns the title of the editor.  (This might be displayed on a 
     *    tab or similar).  If null, the object class name will be used
     *    instead.
     */
     
    public String getName()
    {
        return CBIntText.get("Basic Editor");
    }
     
    /**
     *   Returns the icon of the editor.  (This might be displayed on a tab or similar).
     *   (not set yet).
     *
     */   
           
    public ImageIcon getIcon() 
    { 
        return new ImageIcon(Theme.getInstance().getDirImages() + "blank.gif");
    }  
    
    /**
     *    Returns the Tool Tip of the editor.  (This might be displayed on a tab or similar).
     *    (not set yet).
     *    .
     */ 
         
    public String getToolTip() 
    {  
        return "";
    }
    
    /**
     *    Registers Swing components that the pluggable editor may manipulate.
     *    Any or all of these may be null if the editor is not permitted to
     *    manipulate the objects. <p>
     *
     *    Any changes made by the editor should be removed when the editor is
     *    unloaded.
     */ 
     
    public void registerComponents(JMenuBar menu, JToolBar buttons, JTree tree, JPopupMenu treeMenu, JFrame jxplorer)
    {
        // do nothing
    }
     
     
     /**
      *    This method is called when the editor is being unloaded by the browser,
      *    and allows for any required clean up activity.
      */
          
    public void unload() 
    {
        // do nothing
    } 

    /**
     *    Whether this editor can handle a partially created new entry.
     */
         
    public boolean canCreateEntry() 
    { 
        return false; 
    }
    
    /**
     *    Use the default tree icon system based on naming value
     *    or object class.
     */
     
    public ImageIcon getTreeIcon(String rdn) { return null; }
    
    /**
     *    Use the default popupmenu.
     */
     
    public JPopupMenu getPopupMenu(String rdn) { return null; }
    
    /**
     *    Don't hide sub entries.
     */
     
    public boolean hideSubEntries(String rdn) { return false; }
     
     
    /**
     *    <p>If you want to include language translation files in
     *    your plugin, simply zip 'em up, and use this method
     *    to register the base name of the translation files.</p>
     *    <p>For example, if your translation files are 'myPlugin.properties',
     *    'myPlugin_ja.properties', and 'myPlugin_fr_CA.properties',
     *    you can just zip them up in the same zip file as the
     *    rest of the plugins, and call this method with the
     *    string 'myPlugin' to have these language files 
     *    appended to the base JX language file set.)</p>
     *    @param name the base name of all the different language
     *    files.
     */ 
    public void addLanguageBundle(String name)
    { 
        CBIntText.addBundle(name, getClass().getClassLoader());
    } 
    
    /**
    *    <p>Optionally adds the name of a custom help set to
    *    JXplorer's default help set.  The help set will
    *    be loaded from the plugin's zip file (or equivalently
    *    the top level of the /plugins directory.</p>
    *
    *    <p>This method should return null if no help set needs
    *    to be registered.</p>
    *
    *    @param name the base name (without any localisation 
    *           extensions) of the help set to append. 
    */  
    
    public void addHelpSet(String name)
    {
        CBHelpSystem.addToDefaultHelpSystem(name, getClass().getClassLoader());
    }
    
    /**
     * Whether the editor has unsaved data changes.  This may be used by the GUI to prompt the user when
     * an editor pane is being navigated away from.
     * @return
     */

    public void checkForUnsavedChanges()
    {
    }


}