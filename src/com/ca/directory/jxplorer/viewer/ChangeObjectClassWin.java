package com.ca.directory.jxplorer.viewer;

import com.ca.commons.cbutil.*;
import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.DataListener;
import com.ca.directory.jxplorer.DataSink;
import com.ca.directory.jxplorer.HelpIDs;
import com.ca.directory.jxplorer.broker.DataBrokerQueryInterface;
import com.ca.directory.jxplorer.broker.DataQuery;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Logger;

/**
 *   This class allows a user to change the object classes of
 *    Existing entries.  Warning: Handling of this can get hairy on the JNDI end, due to the
 *   way servers handle changes to the object class attribute.
 *
 *   Code History: this is forked off from tree.NewEntryWin.
 */


public class ChangeObjectClassWin extends CBDialog implements ActionListener, DataListener
{
    JScrollPane possiblePanel;     // holds the selection list
    JList possibleList;            // list of possible object classes to be selected

    JScrollPane selectedPanel;     // holds the results list
    JList selectedList;            // list of user-selected object classes

    DefaultListModel selDataModel; // dataModel for selected list
    DefaultListModel posDataModel; // dataModel for possible list

    DataSink entryEditor;          // the table display the larval entry is (finally) displayed within.

    JCheckBox autoSuggest;

    CBButton select, remove;

    static boolean suggest = true;

    Vector suggestedClasses = new Vector();

    DataBrokerQueryInterface dataSource;

    /*
     *    Only used in 'new entry' mode.
     */

    //DN parentDN;
    //DN childDN;

    /*
     *    Only used in 'change classes' mode.
     */

    DN entryDN = null;
    DXAttributes defaultAttributes = null;

    protected CBHelpSystem helpBroker;

    String objectClassName = null; // is it 'objectclass' or 'objectClass'

    private static Logger log = Logger.getLogger(ChangeObjectClassWin.class.getName());

	boolean virtualEntry = false;		//TE: a flag representing a virtual entry.

  	public BasicAttribute newObjectClasses = null;

   /**
     *    This creates the GUI, using the passed parameters
     *    to set up the default name and default object classes
     *    (if possible), and the destination of the prepared
     *    data.
     *
     *    @param dSource a link to the directory, which is queried to discover
     *               what object classes are available for the entry.
     *    @param entryDN the DN of the (existing) entry to be modified.
     *    @param defaultValues - sometimes the 'new' entry is really just an update
     *               of an old one, with the object classes changing.  If this is non
     *               null, it is used to initialise the attributes of the new entry.
     *    @param attDisplay where the resulting half-finished entry is sent for the
     *               user to modify, and finally submit to the directory.
     *    @param parent the usual parent GUI for swing look and feel propogation etc.
    *	@param virtualEntry flag representing if the entry is a virtual entry
     */
    public ChangeObjectClassWin(DataBrokerQueryInterface dSource, DN entryDN, Attributes defaultValues,
                                DataSink attDisplay, Frame parent, boolean virtualEntry)
    {
        super(parent, CBIntText.get("Set Entry Object Classes"), HelpIDs.CLASS_CHANGE);

        this.virtualEntry = virtualEntry;

        this.entryDN = entryDN;

        dataSource = dSource;

        entryEditor = attDisplay;

        // try to figure out what the capitalisation of 'objectClass' is today...
        try
        {
            objectClassName = dataSource.getSchemaOps().getNameOfObjectClassAttribute();
        }
        catch (Exception e) {} // do nothing...

        getContentPane().add(display);

        setDefaultAttributes(defaultValues);

        setupObjectClassPanels(); // may be null

        registerMouseListeners();
    }

    private void setDefaultAttributes(Attributes defaults)
    {
        if (defaults instanceof DXAttributes)
            defaultAttributes = (DXAttributes)defaults;
        else
            defaultAttributes = new DXAttributes(defaults);
    }


    protected void setupObjectClassPanels()
    {

        display.addWide(new JLabel(CBIntText.get("Available Classes") + ": "),2);
        display.addWide(new JLabel(CBIntText.get("Selected Classes") + ": "),2);
        display.newLine();

        selDataModel = new DefaultListModel();
        selectedList = new JList(selDataModel);
        selectedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectedList.setSelectionModel(new CBSingleSelectionModel(selectedList));   //TE: tries to ensure that the selected item is visible.

        posDataModel = new DefaultListModel();
        possibleList = new JList(posDataModel);
        possibleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        possibleList.setSelectionModel(new CBSingleSelectionModel(possibleList));   //TE: tries to ensure that the selected item is visible.

        possiblePanel = new JScrollPane(possibleList);
        selectedPanel = new JScrollPane(selectedList);

		/*
		 *	If we've passed in an existing entry to get the 'recommended' object
		 *  classes from, then read them off immediately.
		 */

        if (defaultAttributes != null)
        {
            Attribute objectClasses = defaultAttributes.get(objectClassName);
            try
            {
                Enumeration recommendedObjectClasses = objectClasses.getAll();
                while (recommendedObjectClasses.hasMoreElements())
                {
                    String oc = (String)recommendedObjectClasses.nextElement();
                    selDataModel.addElement(oc);
                    suggestedClasses.add(oc);
                }
            }
            catch (Exception e) {}
        }

		/*
		 *  Post a query to get all the available object classes known to the
		 *  directory.
		 *
		 *  The results of this query are returned (eventually & asynchronously)
		 *  in 'dataReady()' below.
		 */
        try
        {
            ArrayList allObjectClasses = dataSource.getSchemaOps().getKnownObjectClasses();

            for (int i=0; i<allObjectClasses.size(); i++)
            {
                posDataModel.addElement(allObjectClasses.get(i));
            }
        }
        catch (NamingException e)
        {
            //TODO add a manual way for the user to manually add object classes
        }
        Enumeration existing = selDataModel.elements();
        while (existing.hasMoreElements())
        {
            posDataModel.removeElement(existing.nextElement());
        }

        display.makeHeavy();
        display.add(possiblePanel,0,4,2,2);
        display.add(selectedPanel,2,4,2,2);
        display.makeLight();
        display.newLine();

        select = new CBButton(CBIntText.get("Add"), CBIntText.get("Click to add the selection."));
        remove = new CBButton(CBIntText.get("Remove"), CBIntText.get("Click to remove the selection."));
        display.addWide(select, 2);
        display.addWide(remove, 2);
        display.newLine();

        validate();

        select.addActionListener(this);
        remove.addActionListener(this);
    }


    /**
     *    Callback from directory request to find possible object classes.
     */

    public void dataReady(DataQuery result)
    {
        try
        {

            if (result.getType() == DataQuery.GETRECOC)
            {
                if (suggestedClasses.size() < 1)  // only add suggestions if none have already been added!
                {
                    ArrayList recommendedObjectClasses = result.getObjectClasses();
                    for (int i=0; i<recommendedObjectClasses.size(); i++)
                        suggestedClasses.addElement(recommendedObjectClasses.get(i));
                }

                checkSuggestedList();
            }
        }
        catch (NamingException e) {CBUtility.error(CBIntText.get("threaded broker error") + ": ", e); } // XXXTHREAD
    }

    /**
     *
     */

    protected void checkSuggestedList()
    {
        if (suggest == true && suggestedClasses.size() > 0)
        {
            for (int i=0; i<suggestedClasses.size(); i++)
            {
                selectClass((String)suggestedClasses.get(i));
            }
        }
    }



    public void registerMouseListeners()
    {
        // 'add' list mouse listener
        possibleList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
               if (e.getClickCount() == 2)
               {
                   selectClass((String)possibleList.getSelectedValue());
               }
               else
                   super.mouseClicked(e);
            }
        });

        selectedList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
               if (e.getClickCount() == 2)
               {
                   removeClass((String)selectedList.getSelectedValue());
               }
               else
                   super.mouseClicked(e);
            }
        });
    }


    public void selectClass(String value)
    {
        if (selDataModel.contains(value)==false)
        {
            selDataModel.addElement(value);
            posDataModel.removeElement(value);
        }
    }

    public void removeClass(String value)
    {
        if (selDataModel.contains(value))
        {
            selDataModel.removeElement(value);
            if (posDataModel.contains(value) == false)
            {
                // add element to the correct alphabetial position...
                Object[] possibleValues = posDataModel.toArray();
                int len = possibleValues.length;
                for (int i=0; i<len; i++)
                {
                    if (value.compareTo(possibleValues[i].toString()) <= 0)
                    {
                        posDataModel.add(i, value);
                        break;
                    }
                }
            }
        }
    }


    /**
     *    <p>This creates the modified entry</p>
     *
     *    <p>This method is extended from the base class, and is called
     *    whenever the OK button is pressed.</p>
     */

    public void doOK()
    {
        String rdnText = null;


        Enumeration selectedObjectClassValues = selDataModel.elements();

        if (selectedObjectClassValues == null)  // error: force user to use cancel to exit
            {CBUtility.warning(this, CBIntText.get("At least one object class must be selected!"),CBIntText.get("Need ObjectClass(s)"));return;}


        BasicAttribute obclasses = new BasicAttribute((objectClassName==null)?"objectClass":objectClassName);
        while (selectedObjectClassValues.hasMoreElements())
        {
            String oc = selectedObjectClassValues.nextElement().toString();

            obclasses.add(oc);
        }
        if (obclasses.size()==0)
            {CBUtility.warning(this, CBIntText.get("At least one object class must be selected.")+ " ", CBIntText.get("Need ObjectClass(s)")); return;}// error: force user to use cancel to exit

		if (virtualEntry)
		{
			//TE: if it is a virtual entry we don't want to add a new entry at this stage.  We just want
			//	  to get the object class(es) that the user has selected.
			newObjectClasses = obclasses;
			doCancel();
		}

        if (createModifiedEntry(obclasses, rdnText, entryDN) == true)
            doCancel();
        // otherwise something went wrong, so keep dialog window around...
    }

    /**
     *    When Cancel is pressed (or the window is finished)
     *    this method shuts the window down, and prompts the
     *    parent to repaint (avoiding those nasty paint artifacts).
     */

    public void doCancel()
    {
        setVisible(false);
        dispose();
        this.getParent().repaint();
    }


    public void actionPerformed(ActionEvent e)
    {
        Object src = e.getSource();

        if (src == select)
        {
            if (possibleList.getSelectedValue() != null)
                selectClass(possibleList.getSelectedValue().toString());
        }
        else if (src == remove)
        {
            if (selectedList.getSelectedValue() != null)
                removeClass(selectedList.getSelectedValue().toString());
        }
        else if (src == autoSuggest)
        {
            suggest = autoSuggest.isSelected();
            if (suggest == true)
                checkSuggestedList();
            else
            {
                if (suggestedClasses.size() > 0)
                {
                    for (int i=0; i<suggestedClasses.size(); i++)
                        removeClass((String)suggestedClasses.get(i));
                }
            }
        }
    }


    /**
     *    create a new Entry, by constructing a DXAttributes object
     *    using the list of objectclasses, and the rdn text, and
     *    passing it to a TreeEntryCreator
     */

    public boolean createModifiedEntry(Attribute obclasses, String rdnText, DN newObjectDN)
    {
        // create a schema aware DXAttributes object, use it to get the list of all
        // parent object classes as an attribute, and add it to the DXAttributes

        DXAttributes attlist = new DXAttributes(obclasses);
        Attribute allObjectClasses = attlist.getAllObjectClasses();

        if (allObjectClasses == null) return false;  // should never happen

        attlist.put(allObjectClasses);
        attlist.expandAllAttributes();

        // copy appropriate default values (if any)
        if (defaultAttributes !=null)
        {
            DXNamingEnumeration defAtts = (DXNamingEnumeration)defaultAttributes.getAllNonNull();
            while (defAtts.hasMore())
            {
                Attribute defAtt = (Attribute)defAtts.next();
                String defAttName = defAtt.getID();
                if (defAttName.equalsIgnoreCase("objectclass")==false)
                {
                    if (attlist.get(defAttName)!=null)    // only use defaults for valid attribute
                        attlist.put(defAtt);
                }
            }
        }

        if (rdnText != null)
        {
            RDN rdn = new RDN(rdnText);

            if (rdn.size()==0 || "".equals(rdn.getRawVal(0)))
            {
                CBUtility.warning(this, CBIntText.get("Please fill in a valid name for the entry"), CBIntText.get("Can't read RDN"));
                return false;
            }

            String rdnAttribute = rdn.getAttID(0);
            String rdnValue = rdn.getRawVal(0);

            if (rdn.isMultiValued()==false)
            {
                BasicAttribute rdnAtt = new BasicAttribute(rdnAttribute, rdnValue);
                attlist.put(rdnAtt);
            }
            else    // multi valued rdn...
            {
                for (int i=0; i<rdn.size(); i++)
                {
                    BasicAttribute rdnAtt = new BasicAttribute(rdn.getAttID(i), rdn.getRawVal(i));
                    attlist.put(rdnAtt);
                }
            }
            if (rdn.validate() == false)
            {
                CBUtility.warning(this, CBIntText.get("Couldn't parse entry's name - please try again"), CBIntText.get("Can't read RDN"));
                return false;
            }
        }

        setVisible(false);
        this.getParent().repaint();

        if (dataSource == null)  // rare error (should be never)
        {
            CBUtility.error(CBIntText.get("unusual error") + ":\nno Data Source registered in NewEntryWin.", null);
            return false;
        }

        DXEntry newEntry = new DXEntry(attlist, newObjectDN);
        newEntry.setStatus(DXEntry.NORMAL);
        entryEditor.displayEntry(newEntry, dataSource);

        return true;
    }
}