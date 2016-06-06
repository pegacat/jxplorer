package com.ca.directory.jxplorer.tree;

import com.ca.commons.cbutil.*;
import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.*;
import com.ca.directory.jxplorer.broker.*;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *   This class allows a user to create the details of a new entry.
 *   These details are then passed to the table editor (usually) 
 *   where the user can make further modifications, before finally
 *   submitting the result to the directory.
 */

// XXX
// XXX    This class is being pressed into service to do two similar
// XXX    things - create new entries, and change the object classes of
// XXX    Existing entries.  It gets a bit ugly, and should probably be
// XXX    Be rewritten.  - Chris
// XXX    


public class NewEntryWin extends CBDialog implements ActionListener, DataListener
{
    JTextField rdnField,parentDNField;
    
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
     
    DN parentDN;
    DN childDN;
    
    /*
     *    Only used in 'change classes' mode.
     */
     
    DN entryDN = null;
    DXAttributes defaultAttributes = null;
    
    protected CBHelpSystem helpBroker;     
        
    String objectClassName = null; // is it 'objectclass' or 'objectClass'

    private static Logger log = Logger.getLogger(NewEntryWin.class.getName());

    /**
     *    this sets whether the window needs to worry about the entry name.
     *    IF we're modifying an existing entry, we don't.
     */
     
    protected boolean simpleMode;  

    /*
     *    Whether the child/parent name fields are disabled (implying that 
     *    they must have been set externally!)
     */
     
    protected boolean disableName = false;
    
	
	boolean virtualEntry = false;		//TE: a flag representing a virtual entry.
	
  	public BasicAttribute newObjectClasses = null;
	
      /**
     *    This creates the GUI, using the passed parameters
     *    to set up the default name and default object classes
     *    (if possible), and the destination of the prepared
     *    data.
     *
     *    @param pDN the parent DN.  This is the DN under which the new entry will
     *               be created.
     *    @param cDN an arbitrary child DN of a sibling of the to-be-created entry.
     *               This may be null, but if present is used to 'guess' what object
     *               classes may be wanted for the new entry.
     *    @param dSource a link to the directory, which is queried to discover
     *               what object classes are available for the new entry.
     *    @param attDisplay where the resulting half-finished entry is sent for the 
     *               user to modify, and finally submit to the directory.
     *    @param parent the usual parent GUI for swing look and feel propogation etc.
     */

    // used by tree to create new entry
    public NewEntryWin(DN pDN, DN cDN, DataBrokerQueryInterface dSource,
                            DataSink attDisplay, Frame parent)
    {
        this(pDN, cDN, dSource, null, null, attDisplay, parent);
        disableName = false;
        rdnField.setEnabled(true);
        parentDNField.setEnabled(true);
    }

    
    /**
     *    This creates the GUI, using the passed parameters
     *    to set up the default name and default object classes
     *    (if possible), and the destination of the prepared
     *    data.
     *
     *    @param pDN the parent DN.  This is the DN under which the new entry will
     *               be created.
     *    @param cDN an arbitrary child DN of a sibling of the to-be-created entry.
     *               This may be null, but if present is used to 'guess' what object
     *               classes may be wanted for the new entry.
     *    @param dSource a link to the directory, which is queried to discover
     *               what object classes are available for the new entry.
     *    @param defaultValues - sometimes the 'new' entry is really just an update
     *               of an old one, with the object classes changing.  If this is non
     *               null, it is used to initialise the attributes of the new entry.
     *    @param rdn an optional default name for an entry (again, mainly used if 
     *               we're over-hauling an old entry, rather than creating a new one
     *               from scratch.
     *    @param attDisplay where the resulting half-finished entry is sent for the 
     *               user to modify, and finally submit to the directory.
     *    @param parent the usual parent GUI for swing look and feel propogation etc.
     */
    //TODO: combine with constructor above
    public NewEntryWin(DN pDN, DN cDN, 
            DataBrokerQueryInterface dSource, Attributes defaultValues,
            String rdn, DataSink attDisplay, Frame parent)
    {
        super(parent, CBIntText.get("Set Entry Object Classes"), HelpIDs.ENTRY_NEW);

        simpleMode = false;
        
        dataSource = dSource;
            
        entryEditor = attDisplay;            
            
        if (defaultValues!=null)  
        {
            if (defaultValues instanceof DXAttributes)
                defaultAttributes = (DXAttributes)defaultValues;
            else
                defaultAttributes = new DXAttributes(defaultAttributes);
        }
        
        // try to figure out what the capitalisation of 'objectClass' is today...
        try
        {
            objectClassName = dataSource.getSchemaOps().getNameOfObjectClassAttribute();
        }
        catch (Exception e)
        {
        } // do nothing...
    
        parentDN = pDN;
        childDN = cDN;
            
        getContentPane().add(display);
 
        autoSuggest = new JCheckBox(CBIntText.get("Suggest Classes?"), suggest);
        display.add(autoSuggest,3,0);
        display.newLine();
        
        display.add(new JLabel(CBIntText.get("Parent DN") + ": "));
        display.addLine(parentDNField = new JTextField(parentDN.toString(), 20));
        
        parentDNField.setEnabled(false);      // disable, (but this is over-ridden if we are called from the other constructor)
        
        display.add(new JLabel(CBIntText.get("Enter RDN") + ": "));
        display.addLine(rdnField = new JTextField("=", 20));

        // try to set some meaningful default values for the rdn field
        if (rdn != null)
        {
            rdnField.setText(rdn);
            rdnField.setEnabled(false);      // disable, (but this is over-ridden if we are called from the other constructor)
        }    
        else if ((suggest == true)&&(childDN != null))
		{
           rdnField.setText(childDN.getLowestRDN().getAttID() + "=");
		}
		
        setupObjectClassPanels(null);
        
    
        
        autoSuggest.addActionListener(this);

        registerMouseListeners();
    }

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
     */
    // called from change class
   //TODO: combine with constructor below
   /*
    public NewEntryWin(DataBrokerQueryInterface dSource, DN entryDN, Attributes defaultValues,
            DataSink attDisplay, Frame parent)
    {
        super(parent, CBIntText.get("Set Entry Object Classes"), HelpIDs.CLASS_CHANGE);

        simpleMode = true;
        
        this.entryDN = entryDN;
        
        dataSource = dSource;
            
        entryEditor = attDisplay;            
            
        if (defaultValues instanceof DXAttributes)
            defaultAttributes = (DXAttributes) defaultValues;
        else
            defaultAttributes = new DXAttributes(defaultAttributes);
        
        // try to figure out what the capitalisation of 'objectClass' is today...
        try
        {
            objectClassName = dataSource.getSchemaOps().getNameOfObjectClassAttribute();
        }
        catch (Exception e) {} // do nothing...
    
        getContentPane().add(display);
 
        setupObjectClassPanels(defaultAttributes);
        
        registerMouseListeners();
    }
	*/
	
	
   /**
	*   This creates the GUI, using the passed parameters
	*   to set up the default name and default object classes
	*   (if possible), and the destination of the prepared
	*   data.
	*
	*   @param dSource a link to the directory, which is queried to discover
	*           what object classes are available for the entry.
	*   @param entryDN the DN of the (existing) entry to be modified.
	*   @param attDisplay - where the data is to be displayed; usually the entry Editor.
	*   @param parent the usual parent GUI for swing look and feel propogation etc.
	*	@param virtualEntry flag representing if the entry is a virtual entry (should always
	*			be true b/c currently this constructor will only be called under that condition).
	*/
	// called from change class
   /*
    public NewEntryWin(DataBrokerQueryInterface dSource, DN entryDN, DataSink attDisplay, Frame parent, boolean virtualEntry)
    {
        super(parent, CBIntText.get("Set Entry Object Classes"), HelpIDs.CLASS_CHANGE);

		this.virtualEntry = virtualEntry;
		
		simpleMode = true;
        
        this.entryDN = entryDN;
		
		dataSource = dSource;
	
		entryEditor = attDisplay;
		getContentPane().add(display);
		
		setupObjectClassPanels(null);
		registerMouseListeners();		
    }
    */


    protected void setupObjectClassPanels(Attributes currentAtts)            
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

        if (currentAtts != null)
        {
            Attribute objectClasses = currentAtts.get(objectClassName);
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
		 *  If we haven't got a list of ocs, but we know the DN of a sibling
		 *  entry (confusing called 'childDN' here) then we try to copy
		 *  the object classes of that entry (on the grounds that similar
		 *  entries often occupy the same level of a directory.
		 *
		 *  The results of this query are returned (eventually & asynchronously)
		 *  in 'dataReady()' below.
		 */

        else if (childDN != null)
        {
            com.ca.directory.jxplorer.broker.DataQuery myQuery = dataSource.getRecommendedObjectClasses(childDN);
            myQuery.addDataListener(this);
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
     
    public void dataReady(com.ca.directory.jxplorer.broker.DataQuery result)
    {
        try
        {
/*
            if (result.getType() == DataQuery.GETALLOC)
            {
                Vector allObjectClasses = result.getObjectClasses(); 
                
                //XXX every performance mistake in the book is committed here.
                for (int i=0; i<allObjectClasses.size(); i++)
                {
                    posDataModel.addElement(allObjectClasses.get(i));
                }
                
                Enumeration existing = selDataModel.elements();
                while (existing.hasMoreElements())
                {
                    posDataModel.removeElement(existing.nextElement());
                }
                    
				//possibleList.setListData(allObjectClasses);            
            }
*/
            if (result.getType() == com.ca.directory.jxplorer.broker.DataQuery.GETRECOC)
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
     *    <p>This creates the new entry</p>
     *
     *    <p>This method is extended from the base class, and is called 
     *    whenever the OK button is pressed.</p>
     */
     
    public void doOK()
    {
        // XXX maybe improve so user doesn't need to spec rdn at this stage.
        
        String rdnText = null;
        
        if (simpleMode == false)
        {
        
            rdnText = rdnField.getText().trim();

            int equalpos = rdnText.indexOf('=');
            rdnText = rdnText.substring(0, equalpos+1) + NameUtility.escape(rdnText.substring(equalpos+1));  // fix for bug 5515

            try                             // do sanity check
            {
//        if (RDN==null || "".equals(RDN)) return;
                if (rdnText==null || "".equals(rdnText))	//TE: empty RDN.
                    throw new InvalidNameException(CBIntText.get("Empty RDN, please enter a valid RDN.") + " " + ((rdnText==null)?"<null>":CBIntText.get("The RDN value entered was: '")+rdnText) + "'");

                if (NameUtility.next(rdnText,0,'=')<0)	//TE: no '='.
                    throw new InvalidNameException(CBIntText.get("Invalid RDN, please enter a naming attribute followed by '=' followed by a name in the RDN field (for example, 'cn=Trudi). '") + ((rdnText==null)?"<null>":CBIntText.get("The RDN value entered was: '")+rdnText) + "'");
                else if (NameUtility.next(rdnText,0,'=')<1)	//TE: no naming attribute.
                    throw new InvalidNameException(CBIntText.get("Invalid RDN, please enter a naming attribute in the RDN field (for example, cn: 'cn=Trudi). '") + ((rdnText==null)?"<null>":CBIntText.get("The RDN value entered was: '")+rdnText) + "'");
            }
            catch (InvalidNameException ine) 
            {
                CBUtility.warning(this, CBIntText.get("Please Fill in the RDN field with a valid RDN."), CBIntText.get("Invalid RDN"));
                log.log(Level.WARNING, "Invalid RDN value in the New Entry dialog: ", ine);
                return;
            }

            String dn;
            if (parentDNField.getText().trim().length() > 0)
                dn = rdnText + "," + parentDNField.getText();
            else 
                dn = rdnText;                
                
            //DN newObjectDN = new DN(dn);
            entryDN = new DN(dn);
        }                                
                                
        Enumeration atts = selDataModel.elements();
        
        if (atts == null)  // error: force user to use cancel to exit
            {CBUtility.warning(this, CBIntText.get("At least one object class must be selected!"),CBIntText.get("Need ObjectClass(s)"));return;}
            
        
        BasicAttribute obclasses = new BasicAttribute((objectClassName==null)?"objectClass":objectClassName);
        while (atts.hasMoreElements())
        {
            String oc = atts.nextElement().toString();

            obclasses.add(oc);
        }
        if (obclasses.size()==0) 
            {CBUtility.warning(this, CBIntText.get("At least one object class must be selected.")+ " ", CBIntText.get("Need ObjectClass(s)")); return;}// error: force user to use cancel to exit
            
		if (virtualEntry)	
		{			  
			//TE: if it is a virtual entry we don't want to add a new entry it at this stage.  We just want
			//	  to get the object class(es) that the user has selected.
			newObjectClasses = obclasses;
			doCancel();
		}		
			
        if (createNewEntry(obclasses, rdnText, entryDN) == true)
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
     
    public boolean createNewEntry(Attribute obclasses, String rdnText, DN newObjectDN)
    {       
        // create a schema aware DXAttributes object, use it to get the list of all
        // parent object classes as an attribute, and add it to the DXAttributes
        
        DXAttributes attlist = new DXAttributes(obclasses);
        Attribute allObjectClasses = attlist.getAllObjectClasses();

        if (allObjectClasses == null) return false;  // should never happen

        //DXNamingEnumeration bloop = new DXNamingEnumeration(allObjectClasses.getAll());
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
        
//        CBUtility.setWaitCursor(this);
        log.fine("creating entry " + newObjectDN);
        DXEntry newEntry = new DXEntry(attlist, newObjectDN);
        
        newEntry.setStatus(DXEntry.NEW);
        entryEditor.displayEntry(newEntry, dataSource);
        
//        dataSource.modifyEntry(null, newEntry);
//        CBUtility.setNormalCursor(this);
        return true;
    }	
}