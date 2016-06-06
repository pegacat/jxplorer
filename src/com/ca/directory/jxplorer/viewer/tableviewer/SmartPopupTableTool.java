package com.ca.directory.jxplorer.viewer.tableviewer;

import com.ca.commons.cbutil.*;
import com.ca.commons.naming.DN;
import com.ca.commons.naming.RDN;
import com.ca.directory.jxplorer.JXConfig;
import com.ca.directory.jxplorer.JXplorerBrowser;
import com.ca.directory.jxplorer.search.SearchExecute;

import javax.naming.NamingException;
import javax.naming.ldap.LdapName;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This is the small popup menu that appears when a manager right-clicks (or system-dependant-whatever-s) on the
 * attribute editing table, allowing them to cut/copy/paste/delete/rename tree elements
 */
public class SmartPopupTableTool extends JPopupMenu
        implements ActionListener
{

    JMenuItem delete, newValue, findDN, makeNaming, removeNaming;  // displayable menu options for user input

    JXplorerBrowser browser;

    JTable table;                      // the table displaying the data - NOT CURRENTLY USED

    AttributeTableModel model;         // the data model - used to insert values into

    String attributeName = null;       // the currently selected attribute class name

    int currentRow;                    // the currently selected row.

    AttributeValue currentValue;       // type of currently selected table row

    AttributeNameAndType currentType;         // value of currently selected table row

    DN currentDN = null;               // used by the cache system?
//RDN currentRDN = null;             // used for naming attribute magic.

    AttributeValueCellEditor cellEditor = null;     //TE: to stop cell editing.

    private static Logger log = Logger.getLogger(SmartPopupTableTool.class.getName());

    boolean readOnly = false;
    /**
     * Constructor initialises the drop down menu and menu items, and registers 'this' component as being the listener
     * for all the menu items.
     */
    public SmartPopupTableTool(JTable t, AttributeTableModel m, JXplorerBrowser jxplorer)
    {
        browser = jxplorer;
        table = t;
        model = m;

        add(newValue = new JMenuItem(CBIntText.get("Add Another Value")));
        add(delete = new JMenuItem(CBIntText.get("Delete Value")));
        add(makeNaming = new JMenuItem(CBIntText.get("Make Naming Value")));
        add(removeNaming = new JMenuItem(CBIntText.get("Remove Naming Value")));
        add(new JSeparator());
        add(findDN = new JMenuItem(CBIntText.get("Find DN")));

        removeNaming.setVisible(false);

        findDN.addActionListener(this);
        newValue.addActionListener(this);
        delete.addActionListener(this);
        makeNaming.addActionListener(this);
        removeNaming.addActionListener(this);

        setVisible(false);
    }

    /**
     * Set the name of the attribute being operated with. That is, for new Value creation.
     */
    public void registerCurrentRow(AttributeNameAndType type, AttributeValue value, int row, RDN currentRDN)
    {
        // TODO: it would be nice to track mandatory single valued attributes, and prevent them being deleted.

        currentType = type;
        currentValue = value;
        currentRow = row;

        // only show 'follow dn' if it is, actually, a dn...

        try {
            LdapName test = new LdapName(value.toString());
            findDN.setVisible(true);
        }
        catch (NamingException e)
        {
            findDN.setVisible(false);
        }


        // if this is read only, disable everything except 'follow dn' and bail out.
        if (readOnly)
        {
            newValue.setVisible(false);
            delete.setVisible(false);
            removeNaming.setVisible(false);
            makeNaming.setVisible(false);
            return;
        }

        if (currentType.toString().equalsIgnoreCase("objectclass"))
        {
            newValue.setEnabled(false);
            delete.setEnabled(false);
        }
        else if (readOnly)
        {
            newValue.setVisible(false);
            delete.setVisible(false);
        }
        else
        {
            newValue.setEnabled(true);
            delete.setEnabled(true);
        }

        if (value.isNaming())
        {
            removeNaming.setVisible(true);
            makeNaming.setVisible(false);
        }
        else if (currentType.isMandatory())  // is it true that naming attributes *have* to be mandatory?  I guess no one has complained for the last ten years...
        {
            removeNaming.setVisible(false);
            makeNaming.setVisible(true);
        }
        else
        {
            removeNaming.setVisible(false);
            makeNaming.setVisible(false);
        }

        /*
        // a bunch of logical assumptions were coded below which may not be valid.

        if (value.isNaming())
        {

            if (currentRDN != null)  // which it never should
            {
                if (currentRDN.size() > 1)
                    removeNaming.setVisible(true);
                else
                    removeNaming.setVisible(false);
            }
            makeNaming.setVisible(false);
        }
        else
        {
            if (currentRDN != null)
            {
                if (currentRDN.toString().indexOf(type.toString() + "=") > -1) // i.e. if we already have a naming att of this type...
                    makeNaming.setVisible(false);   // don't let the user add another one.
                else if (currentType.isMandatory())
                    makeNaming.setVisible(true);
                else
                    makeNaming.setVisible(false);
            }
            removeNaming.setVisible(false);
        }
        */
    }

    public void setReadWrite(boolean readWrite)
    {
        this.readOnly = !readWrite;
    }


    /**
     * This handles the menu item actions.  They rely on the attributeName String being set prior to this method being
     * called (usually by setAttributeName() above).  Most of the action handling is simply tossing arguments to
     * JTable,
     * @param ev the active event, i.e. the menu item selected
     */
    public void actionPerformed(ActionEvent ev)
    {
        setVisible(false);

        Object eventSource = ev.getSource();
        if (eventSource == newValue)
        {
            cellEditor.stopCellEditing();   //TE: bug fix 3107
            newValue();
        }
        else if (eventSource == delete)
        {
            delete();
        }
        else if (eventSource == removeNaming)
        {
            removeRDNComponent();
        }
        else if (eventSource == makeNaming)
        {
            addRDNComponent();
        }
        else if (eventSource == findDN)
        {
            findDNComponent();
        }
        else  // should never happen...
        {
            log.log(Level.WARNING, "Unknown event in popup menu:\n", ev);
        }

        repaint();
    }

    /**
     * Performs a search on the attribute value.  If the value is a DN, the search result is displayed
     * in the Search Results tab.
     */
    public void findDNComponent()
    {
        if ("".equals(currentValue.getStringValue()))
        {
            browser.getSearchTree().clearTree();
            browser.getTreeTabPane().setSelectedComponent(browser.getResultsPanel());
            return;
        }

        String filter = "(objectclass=*)";
        DN dn = new DN(currentValue.getStringValue());

        String aliasOption = "always";
        log.info("Setting search alias option to: [" + aliasOption + "]");
        JXConfig.setProperty("option.ldap.searchAliasBehaviour", aliasOption);

        browser.getSearchBroker().setGUIQuiet(true);
        SearchExecute.run(browser.getSearchTree(), dn, filter, new String[]{"objectClass"}, 0, browser.getSearchBroker());

        browser.getTreeTabPane().setSelectedComponent(browser.getResultsPanel());
    }

    /**
     *
     */
    public void newValue()
    {
        int type = currentType.isMandatory() ? AttributeNameAndType.MANDATORY : AttributeNameAndType.NORMAL;
        String attName = currentType.getName();
        AttributeValue newVal = new AttributeValue(currentValue.getBaseAttribute(), null);
        

        /*
        if (currentValue.isBinary())
        {
            newVal = new AttributeValue(new DXAttribute(attName), null);
            newVal.setBinary(true);
        }
        else
            newVal = new AttributeValue(new DXAttribute(attName), null);
        */
        model.addAttribute(newVal, type, currentRow + 1);
        model.fireChange();
    }

    /**
     *
     */
    public void delete()
    {
        model.deleteAttribute(currentType.getName(), currentRow);
        if (currentValue.isNonStringData())
            currentValue.setValue(null);
        model.fireChange();

        if ((currentType.getName()).equalsIgnoreCase("jpegPhoto"))    //TE: deletes the temporary files associated with the current entry.
            CBCache.cleanCache(currentDN.toString());
    }

    /**
     *
     */
    public void removeRDNComponent()
    {
        if (model.getRDNSize() == 1)
            CBUtility.error(CBIntText.get("Cannot remove the last naming component!"));
        else
            model.removeNamingComponent(currentType, currentValue);
    }

    /**
     *
     */
    public void addRDNComponent()
    {
        if (currentValue.isNonStringData())
            CBUtility.error(CBIntText.get("Binary naming components are not supported."));
        else if (currentValue.isEmpty())
            CBUtility.error(CBIntText.get("A Naming Component must have an actual value."));
        else
            model.addNamingComponent(currentType, currentValue);
    }

    /**
     *
     * @param dn
     */
    public void setDN(DN dn)
    {
        currentDN = dn;
        //currentRDN = dn.getLowestRDN();
    }

    /**
     * registers the cell editor.  TE: for bug fix 3107.
     * @param myEditor the cell editor.
     */
    public void registerCellEditor(AttributeValueCellEditor myEditor)
    {
        cellEditor = myEditor;
    }
}