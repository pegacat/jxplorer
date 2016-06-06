package com.ca.directory.jxplorer.tree;

import com.ca.commons.cbutil.CBIntText;
import com.ca.commons.cbutil.CBUtility;
import com.ca.commons.cbutil.Theme;
import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.ButtonRegister;
import com.ca.directory.jxplorer.JXConfig;
import com.ca.directory.jxplorer.JXplorerBrowser;
import com.ca.directory.jxplorer.event.JXplorerEvent;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;


/**
 * This is the small popup menu that appears when a manager left-clicks
 * (or system-dependant-whatever-s) on the display tree, allowing them
 * to cut/copy/paste/delete/rename tree elements
 */

public class SmartPopupTool extends JPopupMenu
        implements ActionListener
{
    DN cutDN;                   // DN of node that was *previously* selected to be cut
    DN copyDN;                  // DN of node that was *previously* selected to copy
    DN selectDN;                // DN of node grabbed by 'copy DN'
    boolean newEnabled = true;    // whether the 'new' option is on or not

    JMenuItem cut, copy, paste, pasteAlias, delete, rename, search, newEntry, refresh, copydn, bookmark;

    ButtonRegister br = null;
    SmartTree tree;

    private static Logger log = Logger.getLogger(SmartPopupTool.class.getName());

    public static GlobalCopyClipboard globalClipboard = new GlobalCopyClipboard();

    /**
     * Constructor initialises the drop down menu and menu items,
     * and registers 'this'
     * component as being the listener for all the menu items.
     *
     * @param owningTree
     */
    public SmartPopupTool(SmartTree owningTree, JXplorerBrowser browser)
    {

        tree = owningTree;

        add(bookmark = new JMenuItem(CBIntText.get("Add to Bookmarks"), new ImageIcon(Theme.getInstance().getDirImages() + "plus.gif")));
        add(search = new JMenuItem(CBIntText.get("Search"), new ImageIcon(Theme.getInstance().getDirImages() + "find.gif")));
        add(new JPopupMenu.Separator());
        add(newEntry = new JMenuItem(CBIntText.get("New"), new ImageIcon(Theme.getInstance().getDirImages() + "new.gif")));
        add(copydn = new JMenuItem(CBIntText.get("Copy DN"), new ImageIcon(Theme.getInstance().getDirImages() + "copy_dn.gif")));
        add(new JPopupMenu.Separator());
        add(cut = new JMenuItem(CBIntText.get("Cut Branch"), new ImageIcon(Theme.getInstance().getDirImages() + "cut.gif")));
        add(copy = new JMenuItem(CBIntText.get("Copy Branch"), new ImageIcon(Theme.getInstance().getDirImages() + "copy.gif")));
        add(paste = new JMenuItem(CBIntText.get("Paste Branch"), new ImageIcon(Theme.getInstance().getDirImages() + "paste.gif")));
        add(pasteAlias = new JMenuItem(CBIntText.get("Paste Alias"), new ImageIcon(Theme.getInstance().getDirIcons() + "alias.gif")));
        add(new JPopupMenu.Separator());
        add(delete = new JMenuItem(CBIntText.get("Delete"), new ImageIcon(Theme.getInstance().getDirImages() + "delete.gif")));
        add(rename = new JMenuItem(CBIntText.get("Rename"), new ImageIcon(Theme.getInstance().getDirImages() + "rename.gif")));
        add(refresh = new JMenuItem(CBIntText.get("Refresh"), new ImageIcon(Theme.getInstance().getDirImages() + "refresh.gif")));

        bookmark.setToolTipText(CBIntText.get("Bookmark an entry"));
        bookmark.setAccelerator(KeyStroke.getKeyStroke("B".charAt(0), java.awt.Event.CTRL_MASK, false));

        search.setAccelerator(KeyStroke.getKeyStroke("F".charAt(0), java.awt.Event.CTRL_MASK, false));
        search.setToolTipText(CBIntText.get("Search for an entry in the directory."));

        cut.setAccelerator(KeyStroke.getKeyStroke("U".charAt(0), java.awt.Event.CTRL_MASK, false));
        cut.setToolTipText(CBIntText.get("Select a subtree to move."));

        copy.setAccelerator(KeyStroke.getKeyStroke("O".charAt(0), java.awt.Event.CTRL_MASK, false));
        copy.setToolTipText(CBIntText.get("Select a subtree to copy."));

        paste.setAccelerator(KeyStroke.getKeyStroke("P".charAt(0), java.awt.Event.CTRL_MASK, false));
        paste.setToolTipText(CBIntText.get("Paste a previously selected subtree."));

        delete.setAccelerator(KeyStroke.getKeyStroke("D".charAt(0), java.awt.Event.CTRL_MASK, false));
        delete.setToolTipText(CBIntText.get("Delete an entry."));

        rename.setAccelerator(KeyStroke.getKeyStroke("M".charAt(0), java.awt.Event.CTRL_MASK, false));
        rename.setToolTipText(CBIntText.get("Rename an entry."));

        copydn.setAccelerator(KeyStroke.getKeyStroke("Y".charAt(0), java.awt.Event.CTRL_MASK, false));
        copydn.setToolTipText(CBIntText.get("Copy the Distinguished Name of an entry to the clipboard."));

        newEntry.setAccelerator(KeyStroke.getKeyStroke("N".charAt(0), java.awt.Event.CTRL_MASK, false));
        newEntry.setToolTipText(CBIntText.get("Create a new entry."));

        refresh.setAccelerator(KeyStroke.getKeyStroke("R".charAt(0), java.awt.Event.CTRL_MASK, false));
        refresh.setToolTipText(CBIntText.get("Refresh an entry."));

        for (int i = 0; i < getComponentCount(); i++)
            if (getComponent(i) instanceof JMenuItem)
                ((JMenuItem) getComponent(i)).addActionListener(this);

        setVisible(false);
        cutDN = null;
        copyDN = null; //activeDN = null;

        br = browser.getButtonRegister();

        br.registerItem(br.PASTE, paste);
        br.registerItem(br.PASTE_ALIAS, pasteAlias);
        br.registerItem(br.COPY, copy);
        br.registerItem(br.COPY_DN, copydn);
        br.registerItem(br.CUT, cut);
        br.registerItem(br.DELETE, delete);
        br.registerItem(br.NEW, newEntry);
        br.registerItem(br.RENAME, rename);
        br.registerItem(br.REFRESH, refresh);
        br.registerItem(br.BOOKMARKS, bookmark);
        br.registerItem(br.SEARCH, search);

        br.setItemEnabled(br.PASTE, false);
        br.setItemEnabled(br.PASTE_ALIAS, false);
    }


    /**
     * This displays the popup tool at the right spot.  Some special
     * magic here allows us to set the state of the popup tool if we want.
     *
     * @param invoker
     * @param x
     * @param y
     */

    public void show(Component invoker, int x, int y)
    {
        SmartTree tree = (SmartTree) invoker;
        SmartNode node = tree.getSelectedNode();

        boolean modifiable = true;

        if (node == null) modifiable = false;


        if (node.isStructural()) modifiable = false;   // would be nice to allow this for offline (LDIF) work...
        if (tree.getName().equalsIgnoreCase("Schema")) modifiable = false;
        if (tree.isModifiable() == false) modifiable = false;


        br.setItemEnabled(br.RENAME, modifiable);

        super.show(invoker, x, y);
    }


    /**
     * Returns the last set active path (e.g. the TreePath corresponding to
     * the last highlighted tree node).
     */

    public TreePath getActivePath()
    {
        return tree.getSelectionPath();
    }


    /**
     * Returns the most recently selected tree node.
     */

    public SmartNode getActiveNode()
    {
        TreePath path = getActivePath();
        return (path == null) ? null : (SmartNode) path.getLastPathComponent();
    }


    /**
     * Gets the active DN.
     *
     * @return the active DN
     */

    public DN getActiveDN()
    {
        return tree.getTreeModel().getDNForPath(getActivePath());
    }


    /**
     *  This is called externally to popupTreeTool.  It highlights
     *  the path, and records the highlighted node's DN for use
     *  by the menu items.  This ftn is called from the mouse
     *  listener that kicks off the popupTreeTool, or by @SmartTree.valueChanged.
     *
     *  @param ActivePath the set of nodes to select.
     */
/*
    public void setActivePath(TreePath ActivePath)
    {
        activePath = ActivePath;
        activeDN = tree.getTreeModel().getDNForPath(activePath);
    }
*/


    /**
     * This sets whether the popup tool will show functions that will modify the directory.
     *
     * @param canModify
     */

    public void setModifiable(boolean canModify)
    {
        br.setEditingButtons(canModify);

        /*
        if (canModify == false)  // e.g. schema...   
        {
            br.setItemEnabled(br.CUT, false);
            br.setItemEnabled(br.COPY, false);
            br.setItemEnabled(br.COPY_DN, false);
            br.setItemEnabled(br.DELETE, false);
            br.setItemEnabled(br.RENAME, false);
            br.setItemEnabled(br.NEW, false);
            br.setItemEnabled(br.SEARCH, false);
            br.setItemEnabled(br.BOOKMARKS, false);
            br.setItemEnabled(br.PASTE, false);
            br.setItemEnabled(br.PASTE_ALIAS, false);
        }
        else
        {
            br.setItemEnabled(br.CUT, true);
            br.setItemEnabled(br.COPY, true);
            br.setItemEnabled(br.COPY_DN, true);
            br.setItemEnabled(br.DELETE, true);
            br.setItemEnabled(br.RENAME, true);
            br.setItemEnabled(br.NEW, true);
            br.setItemEnabled(br.SEARCH, true);
            br.setItemEnabled(br.BOOKMARKS, true);
        }
        */
    }

    /**
     * This handles the menu item actions.  They rely on
     * 'activeDN' being set prior to this method being called
     * (usually by setActivePath() above).  Most of the action
     * handling is simply tossing arguments to the treeDataSource,
     * and any required tree methods to reflect the changes made.
     *
     * @param ev the active event, i.e. the menu item selected
     */

    public void actionPerformed(ActionEvent ev)
    {
        Object event = ev.getSource();
        setVisible(false);
        repaint();
        if (event == cut)
            cut();
        else if (event == copy)
            copy();
        else if (event == paste)
            paste();
        else if (event == pasteAlias)
            pasteAlias();
        else if (event == delete)
            delete();
        else if (event == rename)
            rename();
        else if (event == newEntry)
            newEntry();
        else if (event == refresh)
            refresh();
        else if (event == copydn)
            copyDN();
        else if (event == bookmark)
            tree.openAddBookmarkDialog(getActiveDN());    //TE: open the bookmark dialog via SmartTree.
        else if (event == search)
            tree.openSearch(getActiveDN());    //TE: open the search dialog via SmartTree.
        else  // should never happen...
            log.warning("Unknown event in popup menu:\n" + ev.toString());

//        repaint();
    }

// XXX All this code below needs to be hived off into a separate 'SmartTreeOperations'
// XXX class of something...


    /**
     * 'Cuts' a subtree by registering the DN of the cut branch,
     * which is accessed by the paste ftn. - the @paste ftn then
     * has responsibility for moving the subtree.
     */

    public void cut()
    {
        DN activeDN = getActiveDN();

        log.fine("Cut " + activeDN);
        cutDN = activeDN;
        copyDN = null;
        selectDN = null;
        br.setItemEnabled(br.PASTE, true);
        br.setItemEnabled(br.PASTE_ALIAS, false);

        globalClipboard.arm(cutDN, tree, true);
    }


    /**
     * 'Copies' a subtree by registering the DN of the copied
     * branch, which is accessed by the paste ftn.  - the @paste
     * ftn then has responsibility for copying the subtree.
     */

    public void copy()
    {
        DN activeDN = getActiveDN();
        log.fine("Copy " + activeDN);
        copyDN = activeDN;
        cutDN = null;
        selectDN = null;
        br.setItemEnabled(br.PASTE, true);
        br.setItemEnabled(br.PASTE_ALIAS, true);

        globalClipboard.arm(copyDN, tree, false);
    }

    /**
     * if user has made custion options they are returned as ints otherwise
     * the selected option is returned
     * credit : http://www.java2s.com/Tutorial/Java/0240__Swing/JOptionPaneUtilityClassGetselectionfromJOptionPane.htm
     *
     * @param optionPane The option pane being checked
     */
    protected static int getSelection(JOptionPane optionPane)
    {
        int returnValue = JOptionPane.CLOSED_OPTION;

        Object selectedValue = optionPane.getValue();
        if (selectedValue != null)
        {
            Object options[] = optionPane.getOptions();
            if (options == null)
            {
                if (selectedValue instanceof Integer)
                {
                    returnValue = ((Integer) selectedValue).intValue();
                }
            }
            else
            {
                for (int i = 0, n = options.length; i < n; i++)
                {
                    if (options[i].equals(selectedValue))
                    {
                        returnValue = i;
                        break; // out of for loop
                    }
                }
            }
        }
        return returnValue;
    }

    /**
     * This checks to see if the 'confirm tree operations' option is on.  If it is,
     * it sticks up an annoying dialog box to ask the user whether they actually want
     * to do what they just asked to do.
     *
     * @param operationType the type of operation being performed e.g. delete, copy etc.
     */

    protected boolean checkAction(String operationType)
    {
        String prop = JXConfig.getProperty("option.confirmTreeOperations");
        if ("false".equalsIgnoreCase(prop))    // the user has configured JX not to ask for confirmation
            return true;

        JOptionPane pane = null;
        Object[] options = {"OK", "CANCEL"};
        int selection = -1;
        String msg = CBIntText.get("The {0} operation will modify the directory - continue?", new String[]{operationType});
        if (operationType.equalsIgnoreCase("delete"))
        {
            selection = JOptionPane.showOptionDialog(this, msg, CBIntText.get("Confirm Tree Operation"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, options, options[1]);
        } else
        {
            selection = JOptionPane.showOptionDialog(this, msg, CBIntText.get("Confirm Tree Operation"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, options, options[0]);
        }


        return (selection == JOptionPane.OK_OPTION);
    }


    /**
     * The paste ftn either copies or moves a pre-selected subtree
     * depending
     */

    public void paste()
    {
        DN activeDN = getActiveDN();

        if (activeDN == null) // should never happen...
            return;                          // so ignore it.

        if (tree != globalClipboard.sourceTree)
        {
            System.out.println("WHOO HOO!  INTER-TREE COPY from: " + globalClipboard.sourceDN + " TO: " + activeDN);
            interDirectoryPaste(activeDN);
            tree.setSelectionPath(tree.getTreeModel().getPathForDN(activeDN));
        } else
        {
            // make sure we're not pasting something into itself...

            if (cutDN == null && copyDN == null) // should never happen...
                return;                          // so ignore it.

            String from = (copyDN == null) ? cutDN.toString() : copyDN.toString();
            String to = activeDN.toString();
            log.fine("pasting: \n" + from + "\n" + to);

            if (to.endsWith(from))
            {
                CBUtility.error(this, CBIntText.get("Unable to paste an object into itself!"));
                return;
            }
            if (copyDN != null)
            {
                copy(copyDN, activeDN);
            } else if (cutDN != null)
            {
                move(cutDN, activeDN);
            }
        }
    }

    public void interDirectoryPaste(DN activeDN)
    {
        // make sure we're not pasting something into itself...

        if (globalClipboard.sourceDN == null) // should never happen...
            return;                          // so ignore it.

        String from = globalClipboard.sourceDN.toString();
        String to = activeDN.toString();

        log.fine("x-dir pasting: \n" + from + "\n" + to);

        if (globalClipboard.isCut)
        {
            interDirectoryMove(globalClipboard.sourceDN, activeDN, globalClipboard.sourceTree);
        } else // ??? if (cutDN != null) ???
        {
            interDirectoryCopy(globalClipboard.sourceDN, activeDN, globalClipboard.sourceTree);
        }

    }

    public void interDirectoryCopy(DN fromDN, DN toDN, SmartTree fromTree)
    {
        if (checkAction("paste") == false)
            return;

        tree.copyTreeFromExternalDirectory(fromDN, toDN, fromTree);
    }

    ;

    //TODO: unfinished...
    public void interDirectoryMove(DN fromDN, DN toDN, SmartTree fromTree)
    {
        if (checkAction("cut") == false)
            return;

        try
        {
            toDN.addChildRDN(fromDN.getLowestRDN().toString());
        } catch (javax.naming.InvalidNameException e)
        {
            CBUtility.error(tree, CBIntText.get("Unable to add {0} to {1} due to bad name", new String[]{fromDN.toString(), toDN.toString()}), e);
            return;
        }

        /*
        tree.modifyEntry(new DXEntry(moveFrom), new DXEntry(moveTo));

        cutDN = null;
        br.setItemEnabled(br.PASTE, false);
        br.setItemEnabled(br.PASTE_ALIAS, false);


        tree.copyTreeFromExternalDirectory(fromDN, toDN, fromTree);
        fromTree.deleteTreeNode(new DXEntry(fromDN));
        */
    }

    ;


    /**
     * Move the node to another location (performs a cut operation).
     * Currently this is being used by the drag functionality - the difference
     * between this method and the move(DN moveFrom, DN moveTo) method is purely
     * that this method does NOT show a confirmation message even if that option is
     * set to true by the user.
     *
     * @param moveFrom the DN of the entry to be moved (or cut).
     * @param moveTo   the new DN of the entry i.e. where it will be cut to.
     *                 .
     */
    //TE: XXXXXXX A TEMP FIX FOR BUG 2742 - WE STILL NEED A CONFIRMATION DIALOG...THIS IS
    //		JUST A WORK AROUND FOR JX CRASHING WHEN DISPLAYING THE CONFIRMATION DIALOG.
    public void dragMove(DN moveFrom, DN moveTo)
    {
        try
        {
            moveTo.addChildRDN(moveFrom.getLowestRDN().toString());
        } catch (javax.naming.InvalidNameException e)
        {
            CBUtility.error(tree, CBIntText.get("Unable to add {0} to {1} due to bad name", new String[]{moveFrom.toString(), moveTo.toString()}), e);
            return;
        }

        tree.modifyEntry(new DXEntry(moveFrom), new DXEntry(moveTo));

        cutDN = null;
        br.setItemEnabled(br.PASTE, false);
        br.setItemEnabled(br.PASTE_ALIAS, false);
    }


    /**
     * Move the node to another location (performs a cut operation).
     *
     * @param moveFrom the DN of the entry to be moved (or cut).
     * @param moveTo   the new DN of the entry i.e. where it will be cut to.
     */

    public void move(DN moveFrom, DN moveTo)
    {
        // 'move'ing a tree means placing the old tree *under*
        // the newly selected tree... hence deepen 'activeDN' by
        // one new level.  (i.e. ou=eng,o=uni,c=au moved to o=biz,c=au
        // requires activeDN extended to ou=eng,o=biz,c=au before move

        if (checkAction("cut") == false)
            return;

        try
        {
            moveTo.addChildRDN(moveFrom.getLowestRDN().toString());
        } catch (javax.naming.InvalidNameException e)
        {
            CBUtility.error(tree, CBIntText.get("Unable to add {0} to {1} due to bad name", new String[]{moveFrom.toString(), moveTo.toString()}), e);
            return;
        }

        tree.modifyEntry(new DXEntry(moveFrom), new DXEntry(moveTo));

        cutDN = null;
        br.setItemEnabled(br.PASTE, false);
        br.setItemEnabled(br.PASTE_ALIAS, false);
    }


    /**
     * Copys (in the directory) the selected DN to the new
     * destination.  The copied entry/subtree is placed *under*
     * the destination node.
     * Currently this is being used by the drag functionality - the difference
     * between this method and the copy(DN copyFrom, DN copyTo) method is purely
     * that this method does NOT show a confirmation message even if that option is
     * set to true by the user.
     *
     * @param copyFrom the DN of an entry or subtree apex to copy entries from
     * @param copyTo   the location to copy to.
     *                 .
     */
    //TE: XXXXXXX A TEMP FIX FOR BUG 2742 - WE STILL NEED A CONFIRMATION DIALOG...THIS IS
    //		JUST A WORK AROUND FOR JX CRASHING WHEN DISPLAYING THE CONFIRMATION DIALOG.
    public void dragCopy(DN copyFrom, DN copyTo)
    {
        tree.copyTree(copyFrom, copyTo);
    }


    /**
     * Copies (in the directory) the selected Dn to the new
     * destination.  The copied entry/subtree is placed *under*
     * the destination node.
     *
     * @param copyFrom the DN of an entry or subtree apex to copy entries from
     * @param copyTo   the location to copy to.
     */

    public void copy(DN copyFrom, DN copyTo)
    {
        if (checkAction("paste") == false)
            return;

        tree.copyTree(copyFrom, copyTo);
    }


    /**
     * Deletes the currently <i>selected</i> entry.
     */
    public void delete()
    {
        DN activeDN = getActiveDN();

        // If a delete is performed with a null DN, the whole DIT is deleted.  Check for a null DN...
        if (activeDN == null || activeDN.isEmpty())
        {
            log.warning("An invalid DN was requested to be deleted: " + activeDN);

            JOptionPane.showMessageDialog(this, CBIntText.get("Please select a valid entry to delete."),
                    CBIntText.get("No Entry Selected"), JOptionPane.WARNING_MESSAGE);

            return;
        }

        if (checkAction("delete") == false)
            return;

        log.fine("deleting " + activeDN);

        tree.modifyEntry(new DXEntry(activeDN), null);
        br.setItemEnabled(br.PASTE_ALIAS, false);

        tree.clearEntry();    //TE: display a null entry.
    }


    /**
     * Starts the tree cell editor on the currently <i>selected</i> entry.
     */

    public void rename()
    {
        if (checkAction("rename") == false) return;

        tree.getTree().startEditingAtPath(getActivePath());  // renameSubTree called by the TreeCellListener
        br.setItemEnabled(br.PASTE_ALIAS, false);
    }


    /**
     * Uses the tree to start the process of creating a new entry.
     */

    public void newEntry()
    {
        if (newEnabled == false)
        {
            CBUtility.warning(this, CBIntText.get("Browser unable to add new entries using LDAP 2 connection"), CBIntText.get("reduced funcitonality in LDAP 2"));
        } else
        {
            setVisible(false);
            repaint();
            tree.makeNewEntry(getActiveDN());
        }
    }

    /**
     * Creates an alias of the currently copied or copyDN-ed entry
     */

/*
    YIPE - here be dragons.  Try to stuff around to support RFC 4512
    (No idea how this used to work at all?  eTrust Directory trickiness??)


    See part of RFC 4512:
2.6.  Alias Entries

  As adapted from [X.501]:

     An alias, or an alias name, for an object is an alternative name
     for an object or object entry which is provided by the use of
     alias entries.

     Each alias entry contains, within the 'aliasedObjectName'
     attribute (known as the 'aliasedEntryName' attribute in X.500), a
     name of some object.  The distinguished name of the alias entry is
     thus also a name for this object.

         NOTE - The name within the 'aliasedObjectName' is said to be
                pointed to by the alias.  It does not have to be the
                distinguished name of any entry.

     The conversion of an alias name to an object name is termed
     (alias) dereferencing and comprises the systematic replacement of
     alias names, where found within a purported name, by the value of
     the corresponding 'aliasedObjectName' attribute.  The process may
     require the examination of more than one alias entry.

     Any particular entry in the DIT may have zero or more alias names.
     It therefore follows that several alias entries may point to the
     same entry.  An alias entry may point to an entry that is not a
     leaf entry and may point to another alias entry.

     An alias entry shall have no subordinates, so that an alias entry
     is always a leaf entry.

     Every alias entry shall belong to the 'alias' object class.

  An entry with the 'alias' object class must also belong to an object
  class (or classes), or be governed by a DIT content rule, which
  allows suitable naming attributes to be present.

  Example:

     dn: cn=bar,dc=example,dc=com
     objectClass: top
     objectClass: alias
     objectClass: extensibleObject
     cn: bar
     aliasedObjectName: cn=foo,dc=example,dc=com

     */
    public void pasteAlias()
    {
        if (checkAction("paste alias") == false)
            return;

        DN aliasedObject = selectDN;
        if (aliasedObject == null)
            aliasedObject = copyDN;
        if (aliasedObject == null)
        {
            log.warning("no DN selected for aliasing.");
            br.setItemEnabled(br.PASTE_ALIAS, false);
            return;
        }

        DN newAlias = new DN(getActiveDN());
        RDN newAliasName = aliasedObject.getLowestRDN();
        newAlias.add(newAliasName);

        DXEntry alias = new DXEntry(newAlias);
        DXAttribute oc = new DXAttribute("objectClass", "top");
        oc.add("alias");
        oc.add("extensibleObject");
        alias.put(oc);
        alias.put(new DXAttribute("aliasedObjectName", aliasedObject.toString()));
        alias.put(new DXAttribute(newAliasName.getAttID(), newAliasName.getRawVal()));

        tree.modifyEntry(null, alias);
    }

    /**
     * Reads the directory to refresh the currently selected entry's data
     * and it's immediate children.
     */

    public void refresh()
    {
        tree.refresh(getActiveDN());
    }

    /**
     * Copies the DN of the current entry to the clipboard...
     */

    public void copyDN()
    {
        selectDN = new DN(getActiveDN());
        StringSelection ss = new StringSelection(selectDN.toString());
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        clip.setContents(ss, ss);
        tree.fireJXplorerEvent(new JXplorerEvent(this, JXplorerEvent.EventType.DNSELECTED, selectDN.toString()));

        if (!tree.getName().equalsIgnoreCase("Schema"))
            br.setItemEnabled(br.PASTE_ALIAS, true);
        br.setItemEnabled(br.PASTE, false);
    }

    /**
     * This is a clipboard object to allow us to copy data between browser windows.  It is a singleton object
     * shared between all popup tool menus, that is armed whenever 'cut' or 'copy' is pressed, and disarmed
     * as soon as a paste or move is completed.
     * <p/>
     * The idea is to trigger an inter-window copy *only* if a user immediately moves to a new window to
     * complete that action...
     */
    static class GlobalCopyClipboard
    {
        public SmartTree sourceTree;
        public DN sourceDN;
        public boolean armed = false;
        public boolean isCut = false; // whether is cut or copy... should use ENUM...

        public GlobalCopyClipboard()
        {
        }

        ;

        public void arm(DN sourceDN, SmartTree sourceTree, boolean isCut)
        {
            this.sourceDN = sourceDN;
            this.sourceTree = sourceTree;
            armed = true;
            this.isCut = isCut;

            sourceTree.browser.getJXplorer().setPopupToolPasteOptions(true);
        }

        public void disarm()
        {
            sourceDN = null;
            sourceTree = null;
            armed = false;
            isCut = false;

            sourceTree.browser.getJXplorer().setPopupToolPasteOptions(false);

        }

    }

}