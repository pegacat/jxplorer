package com.ca.directory.jxplorer.tree;

import com.ca.commons.cbutil.*;
import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.*;
import com.ca.directory.jxplorer.broker.*;
import com.ca.directory.jxplorer.event.JXplorerEvent;
import com.ca.directory.jxplorer.event.JXplorerEventGenerator;
import com.ca.directory.jxplorer.search.SearchGUI;
import com.ca.directory.jxplorer.viewer.AttributeDisplay;
import com.ca.directory.jxplorer.viewer.PluggableEditor;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.event.*;
import java.io.IOException;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * SmartTree displays the directory, using configurable
 * icons.  The SmartTree uses an internal 'SmartNode' class
 * which stores information about a visible Entry.  The
 * display information is separate from the underlying
 * directory; if the class is modified or extended, care
 * must be taken to always keep the two in sych.
 */

// The drag and drop handling in this code owes a lot to the java world tutorial:
// http://www.javaworld.com/javaworld/javatips/jw-javatip97.html

//TODO: 'root node' handling is dodgy - how do we handle multiple top level nodes? (e.g c=au and c=us at the same time?)

public class SmartTree extends JTree
        implements TreeSelectionListener, DataListener,
        TreeExpansionListener, JXplorerEventGenerator,
        DragGestureListener, DropTargetListener, DragSourceListener

{
    boolean setup = false; // whether the delayed graphics constructor has been called.


    public static DN NODATA_DN = new DN(CBIntText.get("cn=no entries"));

    JXplorerEventGenerator eventPublisher; // if registered, this object is used to publish external events
    // (to programs that are using JXplorer as an embedded component)

    JXplorerBrowser browser;           // used for Swing/graphics L&F continuity

    SmartNode rootNode;            // the node representing the first RDN of the rootDN
    SmartNode rootDNBaseNode;      // the node representing the lowest RDN of the rootDN (may = top if only 1 rdn in rootDN)

    DN rootDN;          // the full root DN

    SmartModel treeModel;       // the tree model used to track tree data

    DN currentDN;       // the currently selected DN

    SmartPopupTool popupTreeTool;   // the right-mouse-click tree tools menu

    DefaultTreeCellEditor treeCellEditor;      // widget that allows user to edit node names

    SmartTreeCellRenderer treeCellRenderer;    // widget that display the nodes (text + icons)

    DataBroker treeDataSource;  // where the tree obtains its data

    ArrayList<DataSink> treeDataSinks = new ArrayList<DataSink>(); // a list of objects interested in tree changes

    public DXEntry entry; 			//TE: the current entry.

    String name;   // a unique name for the tree.

    AttributeDisplay pluggableEditorSource = null; // if initialised, this can find pluggable editors to set extended tree behaviour

    private SearchGUI searchGUI = null;
    static int treeNo = 0;


    public boolean dragging = false; // whether a drag 'n drop operation is in progress.

    /* Variables needed for DnD */
    private DragSource dragSource = null;
    private Point cursorLocation = null;

    /**
     * A holder for the number of results returned by a search.
     * This is used in the status bar for user info.
     */
    public int numOfResults = 0;

    private static Logger log = Logger.getLogger(SmartTree.class.getName());


    /**
     * Constructor for SmartTree.  The Tree starts off disconnected, and
     * must be later linked to a data source (using @RegisterDataSource)
     * to become active.
     *
     * @param Owner          the owning awt component - used for look and feel updates
     * @param name           the 'name' of the tree - used for debugging,
     * @param resourceLoader - a resource Loader used to get extra tree icons.  May be null.
     */

    public SmartTree(JXplorerBrowser Owner, String name, CBResourceLoader resourceLoader)
    {
        treeNo++;
        browser = Owner;

        this.name = name;

        setRootDN(NODATA_DN);

        setup = true;  // one way or another, only do this once!

        SmartNode.initIcons(resourceLoader);

        /*
         *    a custom tree cell renderer, shows multi valued attributes and icons.
         */

        treeCellRenderer = new SmartTreeCellRenderer();
        setCellRenderer(treeCellRenderer);

        /*
         *    custom editor, allows editing of multi-valued rdn.
         */

        treeCellEditor = new SmartTreeCellEditor(this, treeCellRenderer);

        setCellEditor(treeCellEditor);

        treeModel = new SmartModel(rootNode);
        setModel(treeModel);

        if (browser != null)  // browser is null during testing...
            registerPopupTool(new SmartPopupTool(this, browser));

        // disallow multiple selections...
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        addTreeSelectionListener(this);
        addTreeExpansionListener(this);

        // use the default editor, but nobble it to avoid editing when drag and drop
        // is operating, and stick special handling in for multi valued rdn editing...

        setTreeMouseListener();

        setTreeCellEditorListener();

        setEditable(true);                        // make sure the user can edit tree cells.

        setupDragAndDrop();
    }

    public String getName()
    {
        return name;
    }

    public String toString()
    {
        return name;
    }

    /**
     * Returns the currently selected Node.
     */
    protected SmartNode getSelectedNode()
    {
        if (getSelectionPath() == null) return null;
        return (SmartNode) getSelectionPath().getLastPathComponent();
    }

    /**
     * Set the popup tool that will be used by the tree.
     */

    public void registerPopupTool(SmartPopupTool tool)
    {
        popupTreeTool = tool;
    }

    /**
     * returns a vector of DNs being the all inclusive list of all DNs
     * in the subtree from the given apex DN.  Used by ldif save ftn.
     * Includes 'structural' nodes that are simply in there to fill out
     * a tree of search results (e.g. placeholder org units in a 'people' search).
     * If you want *just* the results of a search, use 'getAllSearchResultNodes()'
     *
     * @param start the node to start listing search result entries from.
     * @return a ArrayList of DNs
     */

    public ArrayList <DN> getAllNodes(DN start)
    {
        if (start == null) return new ArrayList<DN>(0);          // sanity check
        SmartNode apex = treeModel.getNodeForDN(start);          // need the smart node to find children
        if (apex == null) return new ArrayList<DN>(0);           // another sanity check

        try
        {
            ArrayList <DN> result = new ArrayList<DN>(10);

            result.add(start);                    //

            Enumeration children = apex.children();  // find children of 'from'
            while (children.hasMoreElements())
            {
                DN next = new DN(start);
                SmartNode child = (SmartNode) children.nextElement();
                next.addChildRDN(child.getRDN());
                result.addAll(getAllNodes(next));
            }
            return result;
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "error in SmartTree dump: ", e);
            return new ArrayList<DN>(0);
        }
    }

    /**
     * returns a vector of DNs being the all inclusive list of all DNs
     * in the subtree created from a search.  This *skips* structural nodes
     * that are created simply to 'fill out the tree with parent nodes (
     * e.g. org units created to display a 'person' search).
     *
     *
     * @param start the node to start listing search result entries from.
     * @return a ArrayList of DNs
     */

    public ArrayList <DN> getAllSearchResultNodes(DN start)
    {
        if (start == null) return new ArrayList<DN>(0);          // sanity check
        SmartNode startNode = treeModel.getNodeForDN(start);              // need the smart node to find children
        if (startNode == null) return new ArrayList<DN>(0);           // another sanity check

        try
        {
            ArrayList <DN> result = new ArrayList<DN>(10);

            if (startNode.isStructural() == false)
                result.add(start);                    // only add 'real' nodes from search results, skip structural nodes used to 'fill out' tree structure

            Enumeration children = startNode.children();  // find children of 'from'
            while (children.hasMoreElements())
            {
                DN next = new DN(start);
                SmartNode child = (SmartNode) children.nextElement();
                next.addChildRDN(child.getRDN());
                result.addAll(getAllSearchResultNodes(next));
            }
            return result;
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "error in SmartTree dump: ", e);
            return new ArrayList<DN>(0);
        }
    }


    /**
     * This sets the root of the tree, creating the nodes
     * necessary to display it.  A small wrinkle: the
     * 'rootDN' is the base DN as given by the user, e.g.
     * 'o=Democorp,c=us'.  This may contain multiple RDNs.
     * the root node, however, is the top level node in the
     * display tree; it has only a single RDN (i.e. 'c=us'
     * in the above example.)  The rootDN therefore may be
     * displayed as a number of nodes in the tree (maybe the
     * rootDN should be renamed the 'baseDN' or something...)
     *
     * Set this to null to indicate an empty tree with no data.
     *
     * @param newRootDN the DN of the root as a DN object - there is a big difference between 'null' (= no data / empty tree) and an empty DN (= a valid tree, with an empty root node)
     */

    public void setRootDN(DN newRootDN)
    {
        //rootSet = true;  instead of separate flag, simply check if rootDN == null or NODATA_DN
        if (newRootDN == null) newRootDN = NODATA_DN;  // equivalent to empty DN, 'cn=World'

        /*
         *    Special handling for 'No Data' trees...
         */

        if (newRootDN == NODATA_DN)
        {
            rootNode = new SmartNode(NODATA_DN.getLowestRDN());
            rootDNBaseNode = rootNode;
            this.rootDN = newRootDN;

            //rootSet = false;
            if (treeModel != null)
            {
                treeModel.setRoot(rootNode);
                treeModel.reload();
            }
            return;
        }

        this.rootDN = newRootDN;

        rootNode = new SmartNode("");      	// equivalent to new SmartNode("cn=World")
        rootNode.setRoot(true);
        rootNode.setStructural(true);

        treeModel.setRoot(rootNode); 		// reset the tree Model

        // set up the path to the lowest node of the DN, creating smart nodes for each RDN.

        SmartNode parent = rootNode;
        rootDNBaseNode = rootNode;

        for (int i = 0; i < newRootDN.size(); i++)
        {
            SmartNode child = new SmartNode(newRootDN.getRDN(i));
            child.setStructural(true);
            parent.add(child);
            parent = child;
        }
        rootDNBaseNode = parent;
        rootDNBaseNode.add(new SmartNode());  // stick in a dummy node so there's something to expand to...

        treeModel.reload();



    }

    public void expandRootDN()
    {
        if (rootDN.size() > 0)
            expandPath(treeModel.getPathForNode(rootDNBaseNode));
        else
            collapseRow(0);
    }


    public boolean isEmpty()
    {
        return (rootNode == null || rootDN == NODATA_DN);
    }

    /**
     * Forces a particular DN that is already in the tree to be displayed.
     */

    public void expandDN(DN dn)
    {
        TreePath path = treeModel.getPathForDN(dn);

        expandPath(path.getParentPath());  // without the 'getParentPath()' the next level children are shown as well.
    }

    /**
     * Forces the root node of the tree to expand.
     * (May trigger a list request)
     */

    public void expandRoot()
    {
        expandRow(0);
    }


    /**
     * fully expands all nodes of the tree.
     * (Mainly used for debugging; this could take an
     * unreasonable time if used on a large production
     * directory.)
     */

    public void expandAll()
    {
        int row = 0;
        while (row < getRowCount()) // i.e. tree is still expanding...
        {
            expandRow(row);
            row++;
        }

    }

        /**
     * fully expands all nodes of the tree.
     * (Mainly used for debugging; this could take an
     * unreasonable time if used on a large production
     * directory.)
     */

    public void expandAllExceptRoot()
    {
        int row = 1;
        while (row < getRowCount()) // i.e. tree is still expanding...
        {
            expandRow(row);
            row++;
        }

    }

    /**
     * Get the rootDN of the tree (often the same as the directory DSE
     * naming prefix, e.g. 'o=DemoCorp,c=au'.
     *
     * @return the root DN
     */

    public DN getRootDN()
    {

        //return (rootSet) ? rootDN : null;
        // do we need to return null if rootDN == NO_DATA (which was old behaviour?)

        return rootDN;
    }

    /**
     * Get the root node of tree.  Needed for root.setStructural() hack
     */
    public SmartNode getRootNode()
    {
        return rootNode;
    }

    /**
     * Get the lowest smart node of the baseDN.
     * For example, if base dn is ou=R&D,o=Democorp,c=au, this
     * returns the SmartNode corresponding to ou=R&D.
     */
    public SmartNode getLowestRootNode()
    {
        return rootDNBaseNode;
    }

    /**
     * Returns the tree model used by the Smart Tree to store node data in.
     */

    public SmartModel getTreeModel()
    {
        return treeModel;
    }

    /**
     * registers the data source the tree is to use to read its
     * data (e.g. a JNDIDataBroker).  There can be only one of these.
     *
     * @param s the source of data for initialising SmartNodes etc.
     */

    public void registerDataSource(DataBroker s)
    {
        if (s == null)
        {
            treeDataSource = null;
        }
        else
        {
            log.fine("registering data source for tree " + getName());
            treeDataSource = s;

            // Register ourselves as interested in *all* data events that pass through this data source...
            treeDataSource.addDataListener(this);

            setEditable(treeDataSource.isModifiable()); // disable editing if data source is read only...
        }
        //setRoot(rootDN);
    }

    /**
     * Returns the active data source that has been registered for the tree.
     *
     * @return the data source used by the tree.
     */

    public DataBroker getDataSource()
    {
        return treeDataSource;
    }

    /**
     * Register a <code>DataSink</code>.
     * DataSinks are entities that are interested in
     * displaying the data corresponding to a particular
     * SmartNode (that is, the attributes of the node).  There
     * may be a number of such sinks in operation simultaneously.
     * This registration method adds a new data sink to the list
     * of active data sinks.
     *
     * @param s a new data sink to add to the list of active sinks.
     */

    public void registerDataSink(DataSink s)
    {
        treeDataSinks.add(s);
        if (s instanceof AttributeDisplay)
            registerPluggableEditorSource((AttributeDisplay) s);
    }

    /**
     * Quick Hack to clear out a tree preparitory to loading a new
     * one.  May need revisiting to do a neater data clean up.
     */
    public void clearTree()
    {
        rootNode.removeAllChildren();
        setRootDN(NODATA_DN);
        treeModel.setRoot(rootNode);
        treeModel.reload();
        clearEntry();
        for (int i = 0; i < treeDataSinks.size(); i++)
            ((DataSink) treeDataSinks.get(i)).displayEntry(null, null);
    }

    public void goOffline(OfflineDataBroker broker)
    {
        clearTree();
        registerDataSource(broker);
    }


    /**
     * Takes a node, and adds a bunch of children.
     *
     * @param parent   the node to add the children too
     * @param children an enumeration of NameClassPair containing
     *                 the names of the child nodes
     */

    public void addCutting(SmartNode parent, NamingEnumeration children)
    {
        // sanity checks
        if (parent == null)
        {
            return;
        }
        if (children == null)
        {
            log.warning("null child list in addCutting...!");
            return;
        }

        if (children != null)
        {
            // Step 1: Clear any dummy nodes prior to adding real ones.
            if (parent.getChildCount() == 1 && ((SmartNode) parent.getChildAt(0)).isDummy())
            {
                parent.removeAllChildren();
            }
            // Step 2: Add new children (nb - there may be *zero* children to add, in which case parent is actually a leaf node
            while (children.hasMoreElements())
            {
                NameClassPair np = (NameClassPair) children.nextElement();

                SmartNode child;

                // &(*%& pointless bloody jndi; the one time it would be useful for them
                // to use 'Name' objects they use strings; in 'NameClassPair' no less. What a joke.

                DN temp = new DN(np.getName());
                child = new SmartNode(temp.getRDN(temp.size() - 1));

                if (parent.hasChild(child.toString()) == false)  // don't add a child twice!
                {
                    parent.add(child);
                    //treeModel.insertNodeInto(child, parent, parent.getChildCount());

                    // Step 3:  try to recover the object class list for the node to use.

                    if (np instanceof SearchResult)
                    {
                        doObjectClassSpecificHandling(child, ((SearchResult) np));
                    }

                }

                //XXX is it more efficient to work on nodes, then call nodeStructureChanged?
                //XXX i.e. parent.add(child);

                if (child.getAllowsChildren())
                //    treeModel.insertNodeInto(new SmartNode(), child, 0);
                    child.add(new SmartNode());

                //parent.add(child);
            }

            parent.sort();

            treeModel.nodeStructureChanged(parent);
        }
    }

    public void registerPluggableEditorSource(AttributeDisplay display)
    {
        pluggableEditorSource = display;
    }

    /**
     * HERE BE MAGIC
     * <p/>
     * In order for pluggable editors to truncate trees, special displays for aliases,
     * object class based icons, and object class based popup menus, we need special
     * handling for nodes where the object class is known.<p><
     * <p/>
     * This registers the object class with a node, and checks to see if any pluggable
     * editors are known corresponding to these object classes that are unique, and
     * have special requests (for icons/popup menus/etc.)<p>
     * <p/>
     * This is all optional stuff - the browser will work fine if these details are
     * not available, but more complex pluggable functionality will not be possible.
     *
     * @param child the smart node to register the object classes with
     * @param ocs   a search result containing the object class attribute (we hope).
     */

    protected void doObjectClassSpecificHandling(SmartNode child, SearchResult ocs)
    {
        if (ocs == null) return;                        // can't do anything.
        Attributes atts = ocs.getAttributes();

        if (atts == null || atts.size() == 0) return;   // still can't do anything.

        Attribute OC;

        try  // the usual fuss and bother to retrieve the object class attribute.  X500 does this so much better...
        {
            OC = atts.get("objectClass");
            if (OC == null)
                OC = atts.get("objectclass");

            if (OC == null) // there *may* only be one attribute, which *may* be a wierd capitalisation of object class...
            {
                Attribute test = (Attribute) atts.getAll().next();
                if ("objectclass".equals(test.getID().toLowerCase()))
                    OC = test;
            }

            if (OC == null)   // give up!  It doesn't have one.
                return;

            if ((OC instanceof DXAttribute) == false)
                OC = new DXAttribute(OC);
            OC = DXAttributes.getAllObjectClasses((DXAttribute) OC); // order it...

            doObjectClassSpecificHandling(child, OC);
        }
        catch (Exception e)
        {
            log.warning("Warning error doing object class specific handling for tree nodes: " + e);
        }
    }


    /**
     * HERE BE MAGIC
     * <p/>
     * In order for pluggable editors to truncate trees, special displays for aliases,
     * object class based icons, and object class based popup menus, we need special
     * handling for nodes where the object class is known.<p><
     * <p/>
     * This registers the object class with a node, and checks to see if any pluggable
     * editors are known corresponding to these object classes that are unique, and
     * have special requests (for icons/popup menus/etc.)<p>
     * <p/>
     * This is all optional stuff - the browser will work fine if these details are
     * not available, but more complex pluggable functionality will not be possible.
     *
     * @param child the smart node to register the object classes with
     * @param OC    objectclass attribute
     */
    protected void doObjectClassSpecificHandling(SmartNode child, Attribute OC)
    {
        if (OC instanceof DXAttribute == false)
            OC = new DXAttribute(OC);
        child.setTrueObjectClass((DXAttribute) OC);                   // and register it with smartnode

        // *** TRICKYNESS ***

        //
        // The code below looks for, and interogates, any pluggable editors
        // that are related to the node in order to determine any special
        // handling requirements such as custom popup menus, icons, or
        // subtree truncations.

        if (pluggableEditorSource != null)
        {
            PluggableEditor editor = pluggableEditorSource.getUniqueEditor(OC);

            if (editor != null)
            {

                if (editor.hideSubEntries(child.toString()))
                    child.setAllowsChildren(false);

                ImageIcon newIcon = editor.getTreeIcon(child.toString());
                if (newIcon != null)
                    child.setIcon(newIcon);

                if (editor.getPopupMenu(child.toString()) != null)
                    child.setPopupMenu(editor.getPopupMenu(child.toString()));
            }
        }
    }


    /**
     * Takes an entry, usually from a search result list, and adds it to
     * the tree, creating parent nodes if necessary.
     *
     * @param newDN the new DN to create a smart Node for, and add to the tree.
     */

    public SmartNode addNode(DN newDN)
    {
        if (newDN == null) return null;

        SmartNode parent, child = null;

        if (rootDN == null ||rootDN == NODATA_DN)
        {
            //setRootDN(NODATA_DN);  // is this necessary?  There are side effects from setRoot, but wouldn't they already have been set?
            // if we don't have a root DN, set it to the very first RDN...
            // TODO: I don't think this will cope with multiple top level branches??
            setRootDN(new DN(newDN.getRootRDN().toString()));
        }

        // Walk through the current newDN, creating new nodes
        // as necessary until we can add a new node corresponding to
        // the lowest RDN of the newDN.

        parent = rootNode;
        RDN rdn;

        for (int i = 0; i < newDN.size(); i++)
        {
            rdn = newDN.getRDN(i);
            Enumeration children = parent.children();

            child = null;
            while (children.hasMoreElements())
            {
                child = (SmartNode) children.nextElement();

                if (child.isDummy())                // strip any dummy nodes en passent
                    parent.remove(child);
                else if (child.rdnEquals(rdn))      // and check if the node already exists (before overwritting it below)
                    break;

                child = null;
            }

            if (child == null)               // if the node doesn't exist...
            {
                child = new SmartNode(rdn);  // ... create it
                if (i < newDN.size() - 1)
                {
                    child.setStructural(true);
                }
                parent.add(child);           // ... add it to the parent
                parent.sort();
                treeModel.nodeStructureChanged(parent);

                parent = child;
            }
            else
            {
                parent = child;              // reset parent pointer for next turn around
                if (i == newDN.size() - 1)
                    child.setStructural(false);
            }
        }
//		setSelectionPath(treeModel.getPathForDN(newDN));	//TE: make sure its selected.		
		
        return parent;
    }


    /**
     * Refresh the display of a given volatile dn or node.
     */

    public void refresh(DN dn)
    {
        if (dn != null)		//TE: make sure the dn is not null.
        {
            treeDataSource.getChildren(dn);
        }
    }


    /**
     * Forces a refresh of the Editor Pane.  Currently is used by the tab listener in
     * JXplorer for when the user changes tabs the correct entry display is updated
     * depending on the entry selected in the tree of the tab that is selected.
     * (see Bug 2243).
     * .
     */

    public void refreshEditorPane()
    {
        pluggableEditorSource.refreshEditors(entry, treeDataSource);
    }


    /**
     * Gets the DN of the currently selected tree node (or the root Node,
     * if no node is selected).
     *
     * @return DN the distinguished name of the current SmartNode.
     */

    public DN getCurrentDN()
    {
        return (currentDN == null) ? rootDN : currentDN;
    }


    /**
     * Displays a null entry in the table editor and the html view.
     * Intended to be used if an entry has been deleted.
     */

    public void clearEntry()
    {
        setEntry(null);
        pluggableEditorSource.displayEntry(null, treeDataSource);
    }


    /**
     * removes everything except the root DN node(s), and
     * sets things up as they were in the beginning, the
     * point being to force the tree to reload from the
     * data source, which has changed independantly of the
     * tree...
     */

    public void collapse()
    {

        if (isEmpty()) return;  // don't bother!

        try
        {
            NamingEnumeration en = treeDataSource.getChildren(rootDN).getEnumeration();

            if (en != null)
            {
                clearTree();
                setRootDN(rootDN);
                addCutting(rootDNBaseNode, en);
            }
        }
        catch (NamingException e)
        {
            CBUtility.error(CBIntText.get("threaded broker error")+ ": ", e);
        } // XXXTHREAD
    }


    /**
     * removes a SmartNode and its children from the tree.
     * Only affects the GUI tree - does nothing to the underlying data.
     *
     * @param apex the root of the subtree to be deleted (may be a leaf).
     */

    protected void deleteTreeNode(SmartNode apex)
    {
        treeModel.removeNodeFromParent(apex);
    }

    /**
     * changes the RDN of a tree node to the lowest level
     * RDN of the supplied DN.
     * (usually to mirror a change  in the underlying DIT)
     *
     * @param node  the node to modify
     * @param newDN the DN providing the new lowest level RDN
     *              to modify the node RDN to.
     */

    public void renameTreeNode(SmartNode node, DN newDN)
    {
        node.update(newDN.getLowestRDN());
    }

    /**
     * moves a subtree (may be a leaf) to a
     * new position.  This only modifies the
     * tree, it <i>does not</i> modify the
     * underlying directory.
     *
     * @param node the node to move.
     * @param to   the DN of the position to move it to.
     */

    public void moveTreeNode(SmartNode node, DN to)
    {
        DN from = treeModel.getDNForNode(node);
        if (from.sharesParent(to))    // we may only need to do a rename...
        {
            renameTreeNode(node, to);
        }
        else
        {
            // step 1; remove node from old position
            SmartNode parent = (SmartNode) node.getParent();
            treeModel.removeNodeFromParent(node);
            treeModel.nodeStructureChanged(parent);

            // step 2: modify the nodes RDN
            node.update(to.getLowestRDN());

            // step 3: add it to new position
            parent = treeModel.getNodeForDN(to.getParent());

            if (parent.getChildCount() == 1 && ((SmartNode) parent.getChildAt(0)).isDummy())
            {
                return;  // tree hasn't been read from the directory yet - wait until it is before adding anything
            }

            parent.add(node);
            parent.sort();
            treeModel.nodeStructureChanged(parent);
        }
    }

    /**
     * This copies a tree node and its children to a newly
     * created tree node with the given name, <i>without affecting
     * the underlying directory</i>.
     *
     * @param node the tree node to copy data from
     * @param to   the name of the new tree node to create and, if
     *             necessary, populate.
     */

    public void copyTreeNode(SmartNode node, DN to)
    {
        // find the parent corresponding to the target DN
        SmartNode parent = treeModel.getNodeForDN(to.getParent());

        // sanity check
        if (parent == null)
        {
            CBUtility.error(this, CBIntText.get("unable to copy node {0}.", new String[]{node.toString()}), null);
            return;
        }

        // copy the node (and children) and note the resulting node created
        SmartNode newCopy = copyTreeNodes(node, parent);

        // see if the newly created node has the right RDN
        // (it may not if it has been changed to avoid a 'copy'ing
        // name collision.


        if (newCopy.getRDN().equals(to.getLowestRDN()) == false)
        {
            // we must be doing a 'copy' with a naming problem, so
            // update the newCopy node with it's new, unique RDN.
            newCopy.update(to.getLowestRDN());
        }

        parent.sort();
        // signal a change to the tree for a display update...
        treeModel.nodeStructureChanged(parent);

    }

    /**
     * Copies the node 'from' (creating a new node to hold the copy),
     * adding it as a child to 'toParent',
     * and recursively copies all the children held by 'from' into
     * the newly created copy.
     * This only affects the tree, <i>not</i> the directory.
     *
     * @param from     the node to copy.
     * @param toParent the node which receives the from node as a child.
     * @return the copy of from that is added to toParent
     */

    public SmartNode copyTreeNodes(SmartNode from, SmartNode toParent)
    {
        SmartNode fromCopy = new SmartNode(from); // make copy of 'from' called 'fromCopy
        
        /*
         *    Time saver/Bug Fix - don't bother updating the tree
         *    if the parent hasn't had its children read yet.
         */
         
        if (toParent.hasDummy())
            return fromCopy;

        toParent.add(fromCopy);                   // add 'fromCopy' to 'toParent'

        Enumeration children = from.children();  // find children of 'from'
        while (children.hasMoreElements())
        {
            SmartNode child = (SmartNode) children.nextElement();
            copyTreeNodes(child, fromCopy);       // and add them recursively to 'fromCopy'
        }
        fromCopy.sort();
        return fromCopy;
    }


    /**
     * This makes the internal tree object available, in case
     * different cell editors/renderers and so on need to be
     * registered.
     *
     * @return the internal tree object
     */

    public JTree getTree()
    {
        return this;
    }

    /**
     * Starts the process of making a new entry.  As an aid to the user,
     * it tries to find any children of the current node, and if it can
     * find such, it uses them as a template for the new object.
     */

    public void makeNewEntry(DN parentDN)
    {
        if (treeDataSource.getSchemaOps() == null)
        {
            JOptionPane.showMessageDialog(browser, CBIntText.get("Because there is no schema currently published by the\ndirectory, adding a new entry is unavailable."), CBIntText.get("No Schema"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        else
        {
            // step 1: find a child to use as a template

            SmartNode parent = treeModel.getNodeForDN(parentDN);
            DN childDN = null;
            if (parent == null)
            {
                log.warning("unable to find " + parentDN + " in tree!");
                return;
            }

            if (parent.getChildCount() > 0)
            {
                SmartNode child = (SmartNode) parent.getChildAt(0);
                if ((child != null) && (child.isDummy() == false))
                {
                    RDN childRDN = child.getRDN();
                    childDN = new DN(parentDN);
                    try
                    {
                        childDN.addChildRDN(childRDN);
                    }
                    catch (InvalidNameException e)
                    {
                        log.log(Level.WARNING, "ERROR: makeNewEntry(DN parentDN) " + parentDN, e);
                    }
                }
                else  // children are not currently displayed - send off a query to get them...
                {
                    refresh(parentDN);
                }

            }

// step 2: find a datasink that can handle a partially created entry

            DataSink editor = null;
            for (int i = 0; i < treeDataSinks.size(); i++)
            {
                if (((DataSink) treeDataSinks.get(i)).canCreateEntry())
                    editor = (DataSink) treeDataSinks.get(i);
            }

            if (editor == null)
            {
                CBUtility.error("Unable to create a new entry!", new Exception("No available entry editors"));
                return;
            }

// step 3: open a NewEntryWin (see) and pass the found child to it.


            NewEntryWin userData = new NewEntryWin(parentDN, childDN, treeDataSource, editor, browser);

            userData.setSize(400, 300);
            CBUtility.center(userData, browser);    // TE: centres window.
            userData.setVisible(true);
        }
    }

/*
    public boolean exists(DN nodeDN)
    {
        if (nodeDN == null) return false;
        try
        {
            return treeDataSource.exists(nodeDN).getStatus();
        } catch (NamingException e) {CBUtility.error("threaded broker error: ", e); } // XXXTHREAD
        return false;
    }
*/

    /**
     * Returns the current popup tool.
     * This is used by the menu bar and others to trigger edits
     * etc, since all that functionality lives in SmartPopupTool.
     */

    public SmartPopupTool getPopupTool()
    {
        return popupTreeTool;
    }

    public boolean popupToolVisible()
    {
        return popupTreeTool.isVisible();
    }

    public void registerEventPublisher(JXplorerEventGenerator gen)
    {
        eventPublisher = gen;
    }

    public void fireJXplorerEvent(JXplorerEvent e)
    {
        if (eventPublisher != null)
            eventPublisher.fireJXplorerEvent(e);
    }


    public boolean isModifiable()
    {
        return treeDataSource.isModifiable();
    }
/*
    public LdapContext getLdapContext()
    {
        return (treeDataSource == null) ? null : treeDataSource.getLdapContext();

    }
 */
    /**
     * This files a request with the directory broker to modify (move / delete / add)
     * an entry.  If oldEntry is null this is an add, if newEntry is null it is a
     * delete, otherwise it is (when called by the tree) usually a rename.
     */

    public void modifyEntry(DXEntry oldEntry, DXEntry newEntry)
    {
        if (oldEntry == null && newEntry == null) return; // nothing to do.

        treeDataSource.modifyEntry(oldEntry, newEntry);  // queue directory request
    }


    /**
     * This files a request with the directory broker to copy a node or subtree.
     */

    //XXX This should be done in the thread that actually does the operation - it is
    //XXX possible for the user to 'beat' this method by quickly copying multiples of
    //XXX the same entry.  Also, it produces extra naming attributes.

    public void copyTree(DN oldNodeDN, DN newNodeDN)
    {

        // 'copy'ing a tree means placing the old tree *under*
        // the newly selected tree... hence deepen 'activeDN' by
        // one new level.  (i.e. copying ou=eng,o=uni,c=au moved
        // to o=biz,c=au requires activeDN extended to ou=eng,o=biz,c=au before move

        // * first check name is not already there; if it is, create a
        //   unique name of the form "copy [(n)] of ..." first.
        // * if adding this to the *display* tree fails display error message.
        // * if directory mod fails, clean up display tree...

        String uniqueRDN = treeModel.getUniqueCopyRDN(newNodeDN, oldNodeDN);

        // check not recursively pasting
        try
        {
            newNodeDN.addChildRDN(uniqueRDN);
        }
        catch (javax.naming.InvalidNameException e)
        {
            CBUtility.error(this, CBIntText.get("Unable to add {0} due to bad name", new String[]{newNodeDN.toString()}), e);
            return;
        }

        treeDataSource.copyTree(oldNodeDN, newNodeDN);  // queue directory request
    }

    public void copyTreeFromExternalDirectory(DN oldNodeDN, DN newNodeDN, SmartTree fromTree)
    {
        // 'copy'ing a tree means placing the old tree *under*
        // the newly selected tree... hence deepen 'activeDN' by
        // one new level.  (i.e. copying ou=eng,o=uni,c=au moved
        // to o=biz,c=au requires activeDN extended to ou=eng,o=biz,c=au before move

        // * first check name is not already there; if it is, create a
        //   unique name of the form "copy [(n)] of ..." first.
        // * if adding this to the *display* tree fails display error message.
        // * if directory mod fails, clean up display tree...

        DN testTargetDN = new DN(oldNodeDN.getLowestRDN().toString() + "," + newNodeDN.toString());

        boolean overwriteExistingData = true;

        try
        {
            // check for name clash, and ask user for what to do if there is one...
//            if (targetRDN.equals(oldRDN))    // see if we're trying to copy over a node with the same name...
            if (treeModel.exists(testTargetDN))    // see if we're trying to copy over a node with the same name...
            {
                // ask user whether to a) copy to new branch (copy of...), replace (delete + paste) or overwrite (merge and splat?)
                Object[] possibleValues = { CBIntText.get("Copy"), CBIntText.get("Replace"), CBIntText.get("Merge") };
                Object selectedValue = JOptionPane.showInputDialog(browser,
                        CBIntText.get("There is already a branch with that name.\nDo you want to:\n a) create a new copy,\nb) replace the existing tree,\nc) merge the new data with the old?"),
                        "Input", JOptionPane.INFORMATION_MESSAGE, null,possibleValues, possibleValues[0]);


                if (selectedValue.equals(CBIntText.get("Copy")))
                {
                    String uniqueRDN = treeModel.getUniqueCopyRDN(newNodeDN, oldNodeDN);
                    newNodeDN.addChildRDN(uniqueRDN);
                }
                else if (selectedValue.equals(CBIntText.get("Replace")))
                {
                    newNodeDN.addChildRDN(oldNodeDN.getLowestRDN());

                }
                else if (selectedValue.equals(CBIntText.get("Merge")))
                {
                    newNodeDN.addChildRDN(oldNodeDN.getLowestRDN());
                    overwriteExistingData = false; // we'll try to merge just the new values and entries over the top...
                }
                else
                {
                    log.warning("Unexpected Error determining value - reverting to 'Copy'");
                    String uniqueRDN = treeModel.getUniqueCopyRDN(newNodeDN, oldNodeDN);
                    newNodeDN.addChildRDN(uniqueRDN);
                }
            }
            else
            {
                // equivalent handling to replace, but we're not replacing anything...
                String uniqueRDN = treeModel.getUniqueCopyRDN(newNodeDN, oldNodeDN);
                newNodeDN.addChildRDN(uniqueRDN);
            }

        // check not recursively pasting
        }
        catch (javax.naming.InvalidNameException e)
        {
            CBUtility.error(this, CBIntText.get("Unable to add {0} due to bad name", new String[]{newNodeDN.toString()}), e);
            return;
        }

        treeDataSource.copyTreeBetweenWindows(oldNodeDN, newNodeDN, fromTree.treeDataSource, overwriteExistingData);  // queue directory request
        refresh(newNodeDN);

    }

    /**
     * Displays the result of a expand result, triggered from the data listener.
     * This (should) run from the directory connection thread.
     *
     * @param result the directory read result
     */

    protected void displayExpandedNodeResult(com.ca.directory.jxplorer.broker.DataQuery result)
    {
        // 1) Find node for result

        SmartNode node = treeModel.getNodeForDN(result.requestDN());

        if (node == null)
        {
            node = addNode(result.requestDN());
        }

        try
        {
            // XXX EXTREME HACKINESS AS WE TRY TO SIMULTANEOUSLY SATISFY RICK AND SCOTT'S CONFLICTING REQUIREMENTS
            // 1b) Special Hack for structural nodes to make work nicely for both x500 and ldap...
            /**
             *  What a debacle.  If null prefix router has knowledge of democorp (o=democorp,c=au) then a list of 'root' fails
             *  horribly, because no-one has the node 'c=au'.  So we hack it to not delete root nodes even if it gets zero
             *  results from a list.  Whoo hoo.  *But*, we still need to keep it all clear in the circumstance that it is
             *  just an empty dsa with a null root.  So we have to check IF the node is a 'base DN' type node (alwaysRefresh())
             *  AND we got no search results THEN if it is an empty DSA (only has a single dummy child) close things up,
             *  OTHERWISE keep the 'fake' node (e.g. c=AU).
             **/
            if (node.isAlwaysRefresh() && result.getEnumeration().size() == 0)
            {
                if (node.getChildCount() != 1 || ((SmartNode) node.getChildAt(0)).isDummy() == false)
                {
                    return;    // don't remove structural nodes children, even if server returns no entries.  Assume server is wrong. (for Scott)
                }
            }

            // 2) clear old data

            node.removeAllChildren();

            // 3) add new data

            addCutting(node, result.getEnumeration());

            // XXX (another) pki hack

            if (node == getLowestRootNode() && node.getChildCount() == 0)
            {
                pluggableEditorSource.displaySpecialEntry(null, treeDataSource, JXConfig.getProperty("null.entry.editor"));
            }


            // 4) Make sure the newly added data nodes are visible            
        
            expandPath(treeModel.getPathForDN(result.requestDN()));

            browser.setStatus("   " + node.getDN().toString() + ": (" + node.getChildCount() + ")");

        }
        catch (NamingException e)
        {
            result.setException(e);  // register error and trust someone else to handle it...
            node.removeAllChildren();
        }
    }


    /**
     * Takes a DN and reads and displays the corresponding entry.
     * Then reads all unexpanded parent nodes.  These calls are done
     * via the broker thread.
     * TE: only displays the entry if the supplied dn is below the root
     * DN (which actually is the baseDN) and if the prefix is the same.
     *
     * @param dn the end DN to display.
     */

    public void readAndExpandDN(DN dn)
    {
        if (currentDN.size() < rootDN.size())
        {	//TE: if the user has selected a node above the prefix (i.e. c AU instead of c AU o DEMOCORP
            //	in the case of a router being off line) - re select the prefix otherwise a read error may occur.
            refresh(rootDN);
            setSelectionPath(treeModel.getPathForDN(rootDN));
        }

        if (dn.size() < rootDN.size() || !dn.getPrefix(rootDN.size()).toString().equalsIgnoreCase(rootDN.toString()))
        {
            //TE: only display the entry if the dn is below the root DN (baseDN) and if the prefix is the same.

            JOptionPane.showMessageDialog(browser, CBIntText.get("The entry {0}\nwill not be displayed because it is either above the baseDN\n{1}\nthat you are connected with or it has a different prefix.", new String[]{dn.toString(), rootDN.toString()}),
                    CBIntText.get("Display Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        log.warning("Opening '" + dn + "' from root DN '" + getRootDN());	//TE: fixes bug 2540 - don't ask me how!

        // work down the DN, reading children as required...

        for (int level = 1; level <= dn.size(); level++)
        {
            DN ancestor = (DN) dn.getPrefix(level);

            if (ancestor.size() >= rootDN.size())
            {
                SmartNode node = treeModel.getNodeForDN(ancestor);

                /*
                 *   Check if node hasn't been read - if so, read it and all its sibling
                 *   nodes, by 'listing' all the children of its parent.
                 */
                if (node == null)
                {
                    treeDataSource.getChildren(ancestor.getParent());
                }
                /*
                 *  This check shouldn't be called, if the baseDN is skipped.
                 */
                else if (node.isStructural())
                {
                    treeDataSource.getChildren(ancestor);	//TE: for some unknown reason the structural nodes (root dn) need to be read for the correct behavor to occur.
                }
                /*
                 *	The node hasn't been read yet - read its sibling nodes by 'listing'
                 *  all the children of its parent.
                 */
                else if (node.isDummy())
                {
                    treeDataSource.getChildren(ancestor.getParent());
                }
                /*
                 *  The node already exists in the tree - make sure its visible.
                 */
                else
                {
                    //was: expandDN(ancestor);
                    treeDataSource.getChildren(ancestor.getParent());
                }
            }
        }

        treeDataSource.getEntry(dn);



    }


    /**
     * Displays the result of an entry read result, triggered from the data listener.
     * This (should) run from the directory connection thread.
     *
     * @param node the directory read result.  If null, indicates a 'no data' display should
     *             be shown.
     */

    public void displayReadNodeResult(com.ca.directory.jxplorer.broker.DataQuery node)
    {
        // sanity check
        if (treeDataSinks.size() == 0)
        {
            log.warning("no data sink in display Node");
            return;
        }
        setEntry(null);

        try
        {
            if (node != null)
            {
                setEntry(node.getEntry());
                currentDN = node.requestDN();
            }
            else
            {
                currentDN = null;
            }

            publishData(entry, treeDataSource);


        }
        catch (NamingException e)
        {
            CBUtility.error("unexpected naming error trying to \ndisplay: " + node, e);
        }


        if (node != null)
        {
            TreePath current = treeModel.getPathForDN(node.requestDN());

            if (current == null)
            {
                log.warning("Unable to find tree path for DN: " + node.requestDN());
                return;
            }

// 'User' demand seems to oscillate between wanting the next level displayed, and not
// wanting it displayed.  Comment out appropriate section below...

// just expand node
            if (isExpanded(current.getParentPath()) == false)
                expandPath(current.getParentPath());

// expand node and children
//            if (isExpanded(current) == false)
//                expandPath(current);

            if (current.equals(getSelectionPath()) == false)
            {
                setSelectionPath(current);
            }

            if (currentDN.size() <= rootDN.size())
            //TE: if the user has connected using a baseDN for example, and then moves
            //TE: up the tree, change the rootDN to the entry that the user has moved to.
                rootDN = currentDN;

            // scroll to display the new entry
            int row = this.getRowForPath(current);
            this.scrollRowToVisible(row);
        }
    }

    /**
     * sets the current entry highlighted by the tree.
     */
    public void setEntry(DXEntry newEntry)
    {
        entry = newEntry;
    }

    /**
     * This publishes entry data to all available data listeners,
     * along with a data source.
     * note that both entry and treeDataSource can legitimately be null here
     *
     * @param entry      the entry to publish.  Null indicates no data
     * @param dataSource the datasource to use for further info/operations.
     *                   Null indicates no available data source.
     */
    public void publishData(DXEntry entry, DataBrokerQueryInterface dataSource)
    {
        for (int i = 0; i < treeDataSinks.size(); i++)
        {
            treeDataSinks.get(i).displayEntry(entry, treeDataSource);
        }
    }


    /**
     * This is called when a modify request has been completed.
     */

    protected void displayModifyResult(com.ca.directory.jxplorer.broker.DataQuery result)
    {

        try
        {
            if (result.getStatus() == true)
            {
                DXEntry oldEntry = result.oldEntry();
                DXEntry newEntry = result.newEntry();

                // first, do the 'easy' pure adds and pure deletes.

                if (oldEntry == null || (newEntry != null) && (newEntry.isNewEntry())) // add
                {
                    SmartNode node = addNode(newEntry.getDN());
                    node.add(new SmartNode());  // stick in a dummy node so there's something to expand to...
                    doObjectClassSpecificHandling(node, newEntry.getAllObjectClasses());
                    
                    //XXX *** WARNING ***
                    //
                    // This code is a little naughty.  We reset the status of
                    // the entry, assuming that no other editor cares about it.
                    // - this *should* be done by the broker, but for some reason 
                    // isn't...
                    
                    newEntry.setStatus(DXEntry.NEW_WRITTEN);
                    publishData(newEntry, treeDataSource);
                }
                else if (newEntry == null) // delete
                {
                    DN parentDN = oldEntry.getDN().getParent();
                    deleteTreeNode(treeModel.getNodeForDN(oldEntry.getDN()));
                    if (parentDN != null && parentDN.size()>rootDN.size())
                        setSelectionPath(treeModel.getPathForDN(parentDN));
                }
                else if (oldEntry.getDN().equals(newEntry.getDN()) == false) // check for a change of name
                {
                    SmartNode node = treeModel.getNodeForDN(oldEntry.getDN());

                    /*
                     *    If the old node is not null, then move it to the right place.  If it
                     *    *is* null, it should be because it has been directly edited (and hence
                     *    the tree model is already up to date).
                     */
                     
                    if (node != null)
                    {
                        moveTreeNode(node, newEntry.getDN());
                        treeModel.nodeChanged(node);
                    }    
                    
                    /**
                     *    If the newEntry is empty (i.e. we're just doing a name change)
                     *    use the atts from the old entry...
                     */
                     
                    if (newEntry.size() == 0)
                    {
                        newEntry.put(oldEntry.getAll());
                    }
                    
                    // re-read node (to get attributes)
                    // XXX is this always necessary?
                    
                    treeDataSource.getEntry(newEntry.getDN());
                }
                else    // update editors
                {
                    // re-read node so as to force the browser to correctly display the current state
                    // (A bit heavy, but solves a bunch of consistancy problems)
                    
                    treeDataSource.getEntry(newEntry.getDN());

                }
                // don't need to worry about a change of attributes, since the tree doesn't use them...
            }
        }
        catch (NamingException e)
        {
            result.setException(e);  // XXX set the exception on the result object, let someone else handle it.
        }
        catch (Exception e)
        {
            e.printStackTrace();

        }
    }

    /**
     * Displays a copy result, triggered from the data listener.
     * This (should) run from the directory connection thread.
     *
     * @param result the directory copy result.
     *               be shown.
     */

    protected void displayCopyResult(com.ca.directory.jxplorer.broker.DataQuery result)
    {
        try
        {
            if (result.getStatus() == true)
            {
                copyTreeNode(treeModel.getNodeForDN(result.oldDN()), result.requestDN());
                this.setSelectionPath(treeModel.getPathForDN(result.requestDN()));
            }
        }
        catch (NamingException e)
        {
            result.setException(e);  // XXX set the exception on the result object, let someone else handle it.
        }
    }


    /**
     * Displays a copy result, triggered from the data listener.
     * This (should) run from the directory connection thread.
     *
     * @param result the directory copy result.
     *               be shown.
     */

    protected void displayXWinCopyResult(com.ca.directory.jxplorer.broker.DataQuery result)
    {
        setNumOfResults(0);

        try
        {
            NamingEnumeration results = result.getEnumeration();

            while (results!= null && results.hasMoreElements())
            {
                SearchResult sr = (SearchResult) results.nextElement();
                String search = sr.getName();
                if (search == null || search.length() == 0)
                {
                    addNode(NODATA_DN);
                }
                else
                {
                    DN searchDN = new DN(search);
                    addNode(searchDN);
                    numOfResults++;
                }
            }

            this.setSelectionPath(treeModel.getPathForDN(result.requestDN()));

            //expandAll();

        }
        catch (NamingException e)
        {
            result.setException(e);  // XXX set the exception on the result object, let someone else handle it.
        }
    }
    /**
     * Displays a search result, triggered from the data listener.
     * This (should) run from the directory connection thread.
     *
     * @param result the directory copy result.
     *               be shown.
     */

    protected void displaySearchResult(com.ca.directory.jxplorer.broker.DataQuery result)
    {
        setNumOfResults(0);
        clearTree();
        try
        {
            NamingEnumeration results = result.getEnumeration();

            if (results.hasMoreElements() == true)
            {
                setRootDN(result.getRequestDN());  // reset search tree to use search base as root DN?

                while (results.hasMoreElements())
                {
                    SearchResult sr = (SearchResult) results.nextElement();
                    String search = sr.getName();
                    if (search == null || search.length() == 0) // I think this is redundant? - CB
                    {
                        //addNode(NODATA_DN);
                        log.severe("Unexpected problem getting name of search result: " + sr);
                    }
                    else
                    {
                        DN searchDN = new DN(search);
                        addNode(searchDN);
                        numOfResults++;
                    }
                }
                //TE: task 4648...
                browser.setStatus("   " + result.getRequestDN().toString() + ":+ (" + String.valueOf(numOfResults) + ")");
    
                expandAllExceptRoot();
            }

//            this.setSelectionPath(treeModel.getPathForDN(searchRootDN));



//            DN searchRootDN = result.getRequestDN();
/*
            SwingUtilities.invokeLater(new Runnable()
            {
                  public void run()
                  {
                    expandAll();
//            this.setSelectionPath(treeModel.getPathForDN(searchRootDN));

                  }
            });
*/
        }
        catch (NamingException e)
        {
            result.setException(e);  // XXX set the exception on the result object, let someone else handle it.
        }
    }

    /**
     * @return the numOfResults.
     */

    public int getNumOfResults()
    {
        return numOfResults;
    }

    /**
     * Sets numOfResults.
     *
     * @param numOfResults a holder for the number of results returned by a search.
     */

    public void setNumOfResults(int numOfResults)
    {
        this.numOfResults = numOfResults;
    }

    /**
     *    By default the tree can handle dataQueries of type LIST, COPY, MODIFY, and READENTRY.
     *    This method allows these capabilities to be modified (for example to include SEARCH).
     *    @see com.ca.directory.jxplorer.broker.DataQuery .
     */
/*
    public void setCapabilities(int cap)
    {
        treeCapabilities = cap;
    }
*/
    //
    //
    //  Graphicsy overhead / user i/o functions...
    //
    //


    /**
     * Sets up a listener to monitor whether the user has
     * finished editing a tree cell...
     */

    protected void setTreeCellEditorListener()
    {

        // We are unable to distinguish between keyboard 'esc'
        // and mouse clicking outside the cell, we'll pretend
        // the user wants their changes to go through.
        // XXX may still need to work out some way to distinguish
        // XXX between different cancel modes...
        
        CellEditorListener cl = new CellEditorListener()
        {
            public void editingCanceled(ChangeEvent e)
            {
                changeDN();
            }

            public void editingStopped(ChangeEvent e)
            {
                changeDN();
            }

     

            /**
             * This method is called when the user has changed the name of an
             * entry directly, using a tree cell editor or the multi-valued
             * RDN editor.
             */

            protected void changeDN()
            {
                // o.k., we're not connected to anything...
                if (isActive() == false)
                    return;

                RDN rdn = (RDN) treeCellEditor.getCellEditorValue();

                DN newDN = new DN(currentDN);

                newDN.setRDN(rdn, newDN.size() - 1);

                // check if anything actually changed...
                if (currentDN.toString().equals(newDN.toString()))
                    return;

                //TE: Bug 3172 - if the name exists in the tree, don't attempt a rename...

                if (treeModel.checkForAnotherNodeWithSameRDN(newDN))
                {
                    new CBErrorWin(browser, "The name you are trying to use already exists - " +
                            "please choose another name or delete the original entry.",
                            "Name already exists");
                    refresh(currentDN.getParent());
                    return;
                }

                // modify entry will sort out all the yucky details for us...
                treeDataSource.modifyEntry(new DXEntry(currentDN), new DXEntry(newDN));
            }
        };

        treeCellEditor.addCellEditorListener(cl);
    }

    /**
     * sets up the mouse listener to monitor mouse clicks.  At
     * the moment, the sole use of this is to check whether the
     * popup menu has been triggered, and to supress 'normal' mouse tree
     * event handling if it has.
     */

    protected void setTreeMouseListener()
    {
        // hack our mouse listener to come first, so we can trigger the popup tool before the
        // tree cell editor timer...

        // grab a list of the old listeners (usually just one; the default tree UI listener)
        MouseListener[] listeners = getMouseListeners();

        // clean out all the old listeners
        for (MouseListener listener:listeners)
            removeMouseListener(listener);

        // create new popup listener, incorporating all other listeners
        PopupMouseListener mouseListener = new PopupMouseListener();

        // make that single listener the new 'uber listener'
        addMouseListener(mouseListener);

        // add back the old listeners
        for (MouseListener listener:listeners)
            addMouseListener(listener);
    }

    /**
     * This creates a 'priority' mouse listener, so that popup events
     * do not trigger tree cell editing.
     */
    class PopupMouseListener implements MouseListener
    {
            public void mouseClicked(MouseEvent e)
            {
            }

            public void mousePressed(MouseEvent e)
            {
                doPopupStuff(e);
            }

            public void mouseReleased(MouseEvent e)
            {
                doPopupStuff(e);
            }

            public void mouseEntered(MouseEvent e)
            {
            }

            public void mouseExited(MouseEvent e)
            {
            }

            public boolean doPopupStuff(MouseEvent e)
            {
                if (isActive() == false) return false; // o.k., we're not connected to anything...

                if (e.isPopupTrigger() == false) return false;

                TreePath path = getPathForLocation(e.getX(), e.getY());
                if (path == null)
                {
                    return false;
                }

                setSelectionPath(path); // make sure highlighting stays around
                
                // this probably isn't necessary, but just to make sure that currentDN is set

                DN thisDN = treeModel.getDNForPath(path);
                if (thisDN.equals(currentDN) == false)
                {
                    currentDN = thisDN;
                }

                if (treeDataSource != null)
                {
                    popupTreeTool.setModifiable(treeDataSource.isModifiable());  // whether the user can change anything...

                    // XXX el hack - check to see if entry has a *special* popup tool to use instead...
                    if (getSelectedNode().getPopupMenu() != null)
                        getSelectedNode().getPopupMenu().show(SmartTree.this, e.getX(), e.getY());
                    else
                    {	//TE: this should be improved...
                        Toolkit toolKit = Toolkit.getDefaultToolkit();

                        popupTreeTool.show(SmartTree.this, e.getX(), e.getY());	//TE: displays the popup menu.

                        if ((int) popupTreeTool.getLocationOnScreen().getY() > toolKit.getScreenSize().height - (popupTreeTool.getHeight() + 30))	//TE: if the popup menu extends off the bottom of the screen...
                        {
                            popupTreeTool.show(SmartTree.this, e.getX(), e.getY() - popupTreeTool.getHeight());	//TE: ...reposition it so that the menu ascends from the node rather than descends!
                        }
                    }
                }
                return true;
            }
        }


    /**
     * null implementation to satisfy @TreeExpansionListener interface
     *
     * @param e tree event, implicitly specifying the expanding node.
     */

    public void treeCollapsed(TreeExpansionEvent e)
    {
    }

    /**
     * The user has asked the tree to expand.  Check the node,
     * and if it is a null placeholder, read the node properly from the
     * directory before expanding and displaying.
     *
     * @param e tree event, implicitly specifying the expanding node.
     */

    public void treeExpanded(TreeExpansionEvent e)
    {
        if (isActive() == false) return; // o.k., we're not connected to anything...
        SmartNode current = (SmartNode) e.getPath().getLastPathComponent();


        try
        {
            if (((SmartNode) current.getFirstChild()).isDummy() == true)
            {
                treeDataSource.getChildren(treeModel.getDNForNode(current));
            }
            else if (current.isAlwaysRefresh())
            {
                treeDataSource.getChildren(treeModel.getDNForNode(current));
            }
        }
        catch (java.util.NoSuchElementException err)
        {
            System.out.println("unexpected exception expanding tree node " + err.getMessage());
        }  // why would it be trying to expand anyway?
    }

    /**
     * a node value has changed, so redisplay it...
     *
     * @param e tree event, implicitly specifying the changed node.
     */

    public void valueChanged(TreeSelectionEvent e)
    {
        if (isActive() == false) return; // o.k., we're not connected to anything...

        if (getSelectionPath() == null)
            return;

        if (e.isAddedPath() == false) // deletion occured
        {
            setSelectionPath(null);			  // clear the 'currently selected' data object in popupTreeTool
            displayReadNodeResult(null);          // clear the editor
        }
        else // addition occured
        {
            DN addedDN = treeModel.getDNForPath(getSelectionPath());

            if (addedDN.equals(currentDN) == false)
            {
                treeDataSource.getEntry(treeModel.getDNForNode(getSelectedNode()));
            }
        }
    }


    /**
     * Returns whether the tree is active - i.e. has a valid data source,
     * which is active, and the
     * tree has it's root set.
     */

    protected boolean isActive()
    {
        if (treeDataSource == null) return false;
        if (treeDataSource.isActive() == false) return false;

        //not sure about this one - deactivates search tree, is it useful? if (rootSet == false) return false;

        return true;
    }



    /**
     * This is the data listener interface - this method is called when a data query is finished
     * by a DataBroker.  The tree listens to these results, and adjusts itself to reflect successfull
     * directory operations.
     */

    public void dataReady(com.ca.directory.jxplorer.broker.DataQuery result)
    {
        int type = result.getType();

        if (result.hasException())
        {
            String exception = result.getException().toString();	//TE: quick solution to bug 561...if dsa is offine keep the tree but set everything else to disconnected mode.
            if (exception.indexOf("Socket closed") > -1)
                if (browser instanceof JXplorerBrowser)
                    ((JXplorerBrowser) browser).setDisconnectView();

            CBUtility.error("Unable to perform " + result.getTypeString() + " operation.", result.getException());

            if (type == com.ca.directory.jxplorer.broker.DataQuery.LIST)        // clean up failed list result...
            {
                SmartNode node = treeModel.getNodeForDN(result.requestDN());
                if (!node.isAlwaysRefresh()) // XXX Hack to avoid losing tree when get error reading non-existant base DN node.
                {
                    node.removeAllChildren();
                    treeModel.nodeStructureChanged(node);
                }
            }

            return;
        }
        else
        {
            switch (type)
            {
                case com.ca.directory.jxplorer.broker.DataQuery.LIST:
                    displayExpandedNodeResult(result);
                    break;

                case com.ca.directory.jxplorer.broker.DataQuery.COPY:
                    displayCopyResult(result);
                    break;

                case com.ca.directory.jxplorer.broker.DataQuery.XWINCOPY:
                    displayXWinCopyResult(result);
                    break;

                case com.ca.directory.jxplorer.broker.DataQuery.MODIFY:
                    displayModifyResult(result);
                    break;

                case com.ca.directory.jxplorer.broker.DataQuery.SEARCH:
                    displaySearchResult(result);
                    break;

                case com.ca.directory.jxplorer.broker.DataQuery.READENTRY:
                    displayReadNodeResult(result);
                    break;
            }

            if (result.hasException())
            {
                CBUtility.error("Exception occurred during tree display of " + result.getTypeString() + ".\n\n(Error caught by display tree)", result.getException());
                return;
            }
        }
    }


    public void validate()
    {
        super.validate();
    }

    //	********************
    //
    //  *** DRAG 'N DROP ***
    //
    //  ********************


    protected void setupDragAndDrop()
    {
// XXX Disable Drag and Drop on Solaris.  Doesn't work worth a damn, and
// XXX has some *VERY* strange behaviour

        if (CBUtility.isSolaris()) return;

        // Disable Drag and Drop if the user is in a locked read only mode...
        if ("true".equals(JXConfig.getProperty("lock.read.only")))
            return;

        // TODO: disable drag/drop when we have selected read only *for this particular connection*...


        // Disable Drag and Drop if the user has set the option to do so... ('true' is default though).
        if (!"true".equals(JXConfig.getProperty("option.drag.and.drop")))
            return;

        /*  Custom dragsource object: needed to handle DnD in a JTree.
          *  This is pretty ugly. I had to overide (labotimize) the updateCurrentCursor
         *  method to get the cursor to update properly.
         */

        dragSource = new DragSource()
        {
            protected DragSourceContext createDragSourceContext
                    (DragSourceContextPeer dscp, DragGestureEvent dgl, Cursor dragCursor,
                     Image dragImage, Point imageOffset, Transferable t,
                     DragSourceListener dsl)
            {
                return new DragSourceContext(dscp, dgl, dragCursor, dragImage, imageOffset, t, dsl)
                {
                    protected void updateCurrentCursor(int dropOp, int targetAct, int status)
                    {
                    }
                };
            }
        };


        DragGestureRecognizer dgr = dragSource.createDefaultDragGestureRecognizer(this,
                DnDConstants.ACTION_COPY_OR_MOVE, this);


        /*
* Eliminates right mouse clicks as valid actions - useful especially
          * if you implement a JPopupMenu for the JTree
         */
//?		dgr.setSourceActions(dgr.getSourceActions() & ~InputEvent.BUTTON3_MASK);

        dgr.setSourceActions(dgr.getSourceActions() + InputEvent.BUTTON1_MASK);

        /* First argument:  Component to associate the target with
         * Second argument: DropTargetListener
        */
        new DropTarget(this, this);

    }

    /**
     * DragGestureListener interface method
     */
    public void dragGestureRecognized(DragGestureEvent e)
    {
        //Get the selected node
        SmartNode dragNode = getSelectedNode();
        if (dragNode != null)
        {
            dragging = true;

            //Get the Transferable Object
            Transferable transferable = (Transferable) dragNode;

            //Select the appropriate cursor;
            Cursor cursor = DragSource.DefaultCopyDrop;
            int action = e.getDragAction();
            if (action == DnDConstants.ACTION_MOVE)
                cursor = DragSource.DefaultMoveDrop;

            //begin the drag
            dragSource.startDrag(e, cursor, transferable, this);
        }
    }

    /**
     * DragSourceListener interface method
     */
    public void dragDropEnd(DragSourceDropEvent dsde)
    {
        dragging = false;
    }

    /**
     * DragSourceListener interface method
     */
    public void dragEnter(DragSourceDragEvent dsde)
    {
        setCursor(dsde);
    }

    /**
     * DragSourceListener interface method
     */
    public void dragOver(DragSourceDragEvent dsde)
    {
        setCursor(dsde);
    }

    /**
     * DragSourceListener interface method
     */
    public void dropActionChanged(DragSourceDragEvent dsde)
    {
    }

    /**
     * DragSourceListener interface method
     */
    public void dragExit(DragSourceEvent dsde)
    {
    }

    /**
     * DragSourceListener interface method. This is a bit ugly.
     * This is where we set the cursor depending on whether
     * we are allowed to drop our Transferable object. However,
     * we need to have the location of the mouse and the object
     * that sets the cursor, which come from two different events.
     * Its not pretty but I set a global variable with the location
     * in the DropTargetListener dragOver method.
     */
    private void setCursor(DragSourceDragEvent dsde)
    {
        //if we dont know the cursor location, don't do anything.
        if (cursorLocation == null) return;

        TreePath destinationPath =
                getPathForLocation(cursorLocation.x, cursorLocation.y);

        //get the object that sets the cursor type
        DragSourceContext dsc = dsde.getDragSourceContext();

        //TE: if copy & if destination path is okay set cursor to allow drop...
        if (testDropTarget(destinationPath, getSelectionPath()) == null && dsde.getDropAction() == 1)
            dsc.setCursor(DragSource.DefaultCopyDrop);

        //TE: otherwise...if copy...set to drop not allowed
        else if (dsde.getDropAction() == 1)
            dsc.setCursor(DragSource.DefaultCopyNoDrop);

        //TE: if move & if destination path is okay set cursor to allow drop...
        else if (testDropTarget(destinationPath, getSelectionPath()) == null && dsde.getDropAction() == 2)
            dsc.setCursor(DragSource.DefaultMoveDrop);

        //TE: ...otherwise set to drop not allowed
        else
            dsc.setCursor(DragSource.DefaultMoveNoDrop);
    }


    /**
     * DropTargetListener interface method - What we do when drag is released
     */
    public void drop(DropTargetDropEvent e)
    {
        try
        {
            Transferable tr = e.getTransferable();

            //flavor not supported, reject drop
            if (!tr.isDataFlavorSupported(SmartNode.UNICODETEXT))
            {
                e.rejectDrop();
                return;
            }

            //cast into appropriate data type
            String bloop = tr.getTransferData(SmartNode.UNICODETEXT).toString();

            //get new parent node
            Point loc = e.getLocation();
            TreePath destinationPath = getPathForLocation(loc.x, loc.y);

            final String msg = testDropTarget(destinationPath, getSelectionPath());

            if (msg != null)
            {
                e.rejectDrop();

                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        //JOptionPane.showMessageDialog(Parent, msg, "Error Dialog", JOptionPane.ERROR_MESSAGE);
                        CBUtility.error(msg);
                    }
                });

                return;
            }

            SmartNode newParent = (SmartNode) destinationPath.getLastPathComponent();
            DN parentDN = treeModel.getDNForNode(newParent);

            SmartNode oldNode = (SmartNode) getSelectedNode();
            DN oldDN = treeModel.getDNForNode(oldNode);

            int action = e.getDropAction();
            boolean copyAction = (action == DnDConstants.ACTION_COPY);

            if (copyAction)
            {
                if (System.getProperty("java.version").startsWith("1.4.0"))
                    popupTreeTool.dragCopy(oldDN, parentDN);
                else
                    popupTreeTool.copy(oldDN, parentDN);	//TE: XXXXXXXX crashes!  CB: Not any more - Swing bug fixed in 1.4.1+
            }
            else
            {
                if (System.getProperty("java.version").startsWith("1.4.0"))
                    popupTreeTool.dragMove(oldDN, parentDN);
                else
                    popupTreeTool.move(oldDN, parentDN);	//TE: XXXXXXXX crashes!  CB: Not any more - Swing bug fixed in 1.4.1+
            }

            e.acceptDrop(action);
            e.getDropTargetContext().dropComplete(true);
        }
        catch (IOException io)
        {
            e.rejectDrop();
        }
        catch (UnsupportedFlavorException ufe)
        {
            e.rejectDrop();
        }
    } //end of method


    /**
     * DropTargetListener interface method
     */
    public void dragEnter(DropTargetDragEvent e)
    {
    }

    /**
     * DropTargetListener interface method
     */
    public void dragExit(DropTargetEvent e)
    {
    }

    /**
     * DropTargetListener interface method
     */
    public void dragOver(DropTargetDragEvent e)
    {
        //set global cursor location. Needed in setCursor method.  El Hack.
        cursorLocation = e.getLocation();
    }

    /**
     * DropTargetListener interface method
     */
    public void dropActionChanged(DropTargetDragEvent e)
    {
    }


    /**
     * Convenience method to test whether drop location is valid
     *
     * @param destination The destination path
     * @param dropper     The path for the node to be dropped
     * @return null if no problems, otherwise an explanation
     */

    private String testDropTarget(TreePath destination, TreePath dropper)
    {
        //Typical Tests for dropping

        //Test 1.
        boolean destinationPathIsNull = destination == null;
        if (destinationPathIsNull)
            return CBIntText.get("Invalid drop location.");

        //Test 2.
//    PersonNode node = (PersonNode) destination.getLastPathComponent();
//    if ( !node.getAllowsChildren() )
//      return "This node does not allow children";

        if (destination.equals(dropper))
            return CBIntText.get("Destination cannot be same as source");

        //Test 3.
        if (dropper.isDescendant(destination))
            return CBIntText.get("Destination node cannot be a descendant.");

        //Test 4.
        if (dropper.getParentPath().equals(destination))
            return CBIntText.get("Destination node cannot be a parent.");

        return null;
    }


    /**
     * Opens the delete bookmark dialog.
     */
    public void openDeleteBookmarkDialog()
    {
        BookMarks bm = new BookMarks((JXplorerBrowser) browser);
        bm.getDeleteDialog();
    }

    /**
     * Opens the edit bookmark dialog.
     */
    public void openEditBookmarkDialog()
    {
        BookMarks bm = new BookMarks((JXplorerBrowser) browser);
        bm.getEditDialog();
    }

    /**
     * Opens the add bookmark dialog.
     *
     * @param dn the DN of the bookmark to add.
     */
    public void openAddBookmarkDialog(DN dn)
    {
        BookMarks bm = new BookMarks((JXplorerBrowser) browser);
        BookMarks.AddDialog addDialog = bm.getAddDialog(dn.toString(), false);
        addDialog.setVisible(true);
    }

    /**
     * Opens the search dialog.
     */
    public void openSearch()
    {
        openSearch(currentDN);
    }

    /**
     * Opens the search dialog.
     *
     * @param dn the DN to search from.
     */
    public void openSearch(DN dn)
    {
        JXplorerBrowser jx = (JXplorerBrowser) browser;
        if (searchGUI == null)
            searchGUI = new SearchGUI(dn, jx);

        searchGUI.setBaseDN(dn);
        searchGUI.setVisible(true);
    }

    /**
     * @return the search GUI.
     */
    public SearchGUI getSearchGUI()
    {
        return searchGUI;
    }

    /**
     * Set the search GUI.
     *
     * @param searchGUI
     */
    public void setSearchGUI(SearchGUI searchGUI)
    {
        this.searchGUI = searchGUI;
    }

    /**
     * This utility method resets the tree from the top level, using the unthreaded methods of the current data broker.
     * It is primarily intended for use with offline data brokers that read a large quantity of information from a file,
     * and then need to reset the top level of the tree to reflect the new data set.
     * @param topNodes a list of 'top level' nodes to expand.  If null, a single apex node will be searched for
     * and used.
     */
    public void unthreadedRefresh(ArrayList<DN> topNodes)
    {
        // quicky check to see if we're working off line, or need to set the
        // root for some other reason (can't actually think of any...)

        if (isEmpty())
        {
            //TE: if there are multiple root DNs this ensures that they are displayed (see bug 529),
            //    e.g o=CA1 & o=CA2.
            //	  However, I have a feeling this could make other things fall over...but have no idea
            //    what at this stage because this only keeps account of the last root DN (e.g. o=CA2)
            //	  whereas there may be several...
            //CB: TODO: sort this out; the tree model doesn't really allow for multiple roots, so this is working by accident :-)
            for(DN root: topNodes)
            {
                setRootDN(root);  // bit of a hack; pass the last known real DN
                expandRootDN();   // this may kick off other read events?
                getRootNode().setStructural(true);
            }
        }

        // give the tree a kick to make it refresh the apex parent, thus displaying
        // the newly imported nodes.

        for (DN root: topNodes)
        {
            if (root.size()>1)
                refresh(root.getParent());
        }
    }
}