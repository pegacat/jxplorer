package com.ca.directory.jxplorer.tree;

import com.ca.commons.cbutil.CBResourceLoader;
import com.ca.commons.cbutil.CBUtility;
import com.ca.commons.cbutil.Theme;
import com.ca.commons.naming.*;
import com.ca.directory.jxplorer.JXConfig;

import javax.naming.NamingEnumeration;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.text.CollationKey;
import java.text.Collator;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 *    SmartNode is a utility class for storing class/value/icon combinations, as
 *    well as the standard MutableTreeNode information from the swing.tree package. 
 *    combinations.  Takes arguments of form 'cn=Fred+sn=Nurk: ...'
 *    and parses them to:
 *       distinguishedValue = 'Fred+Nurk'
 *
 *    The class also tries to work out an appropriate icon for displaying the node.
 *    It uses the objectClass attributes, when these are known, or a default icon
 *    keyed to the ldap RDN attribute (i.e. 'c','o','cn' etc.)
 *
 *    WARNING: At present, this only works for <b>single</b> valued RDNs!
 */
 
 // Mmmm... wonder if it's worth trying to include an explicit DN for every node,
 // rather than generating it from the tree?
 // ans. Yes it would be DAMN useful, but there might be some stress keeping them
 // all updated when parent nodes changed... better to have them able to work it 
 // out dynamically...

 
public class SmartNode extends DefaultMutableTreeNode implements  Transferable, Comparable
{
    private static RDN emptyRDN = new RDN(); 

    public RDN rdn = emptyRDN;
    
    /*
     *  A misnomer - this is actually the naming attribute of the RDN.
     */
    String nodeObjectClass = null;

    boolean dummy = false;
    boolean root = false;
    boolean alwaysRefresh = false;
    boolean blankRoot = false;                       // if root is blank, use ROOTNAME or blank depending on context
    //boolean objectClassSet = false;                  // whether a true object class is known.
    boolean structural = false;                      // whether this is just to fill the tree out, but doesn't represent an entry... (i.e. in a search tree response)


    static Hashtable icons = new Hashtable(16);      // the icons displayed in the tree
    static boolean useIcons;

    public static final String ROOTNAME = "World";
    public static final String DUMMYMESSAGE = "reading...";

    ImageIcon icon = null;

    JPopupMenu menu = null;  // an optional popup menu.

	private static boolean initialised = false;

  	final public static DataFlavor UNICODETEXT = DataFlavor.getTextPlainUnicodeFlavor();

	DataFlavor[] flavours = { UNICODETEXT };

    // get a single platform specific language collator for use in sorting.
    private static Collator myCollator = Collator.getInstance();

    // the collation key of the node, used for sorting.
    private CollationKey collationKey;

    private static Logger log = Logger.getLogger(SmartNode.class.getName());

/**
 *   Pre load the image icons, we'll be using them a lot.
 *
 *   Image icons are stored in the 'icons' hashtable, keyed on their
 *   file name stem. (i.e. person.gif is stored with key 'person')
 */
 
 
// XXX Rewrite to use resource loader as well...!
// XXX (and possibly only create icons when needed?)
 
    static public void initIcons(CBResourceLoader resourceLoader)
    {
    	if (initialised) return;	// we only need to run this method once, but
    	initialised = true;			// it's not an error to call it multiple times.

        if (resourceLoader==null)
            return;

        String[] extensions = {"jpg","gif","jpeg"};
        
        String[] iconFiles = CBUtility.readFilteredDirectory(Theme.getInstance().getDirIcons(), extensions);
        
        /*
         *    Emergency BackUp : If the icon directory is bad, try to
         *    find a working one, and if successfull, save it as new 
         *    default value.
         */
        if (iconFiles == null)
        {
            log.warning("can't find icon directory " + Theme.getInstance().getDirIcons() + " trying to find /icons directory");
            iconFiles = CBUtility.readFilteredDirectory(Theme.getInstance().getDirIcons(), extensions);
            if (iconFiles == null)
            {
                log.warning("Can't find icon directory; check 'dir.icons=' line in dxconfig.txt.");
                return;
            }
            log.warning("Recovered!  - iconPath reset to " + Theme.getInstance().getDirIcons());
        }
         
        for (int i=0; i<iconFiles.length; i++)
        {
            String stem = iconFiles[i].substring(0,iconFiles[i].lastIndexOf('.'));
            // save icon names *in lower case*
            icons.put(stem.toLowerCase(), new ImageIcon(Theme.getInstance().getDirIcons() + iconFiles[i]));
        }
        
        // get any extra icons available in resource files...
        
        try
        {

        	//TODO: Still check this case for theme-izing
	        String[] extraIcons = resourceLoader.getPrefixedResources("icons/");
	        for (int i=0; i<extraIcons.length; i++)
	        {
	        	String iconName = extraIcons[i];
	        	String stem = iconName.substring(6);
	        	int endpos = stem.lastIndexOf('.');
				if (stem.length() > 0 && endpos != -1)
				{
            // save icon names *in lower case*
					stem = stem.substring(0,endpos).toLowerCase();
		        	byte[] b = resourceLoader.getResource(iconName);
		        	icons.put(stem, new ImageIcon(b));
	        	}
	        }
        }
		catch (Exception e)
		{     
			log.warning("Error trying to load icons from resource files: " + e);
		}
        useIcons = icons.containsKey("default"); // only use icons if we have a fallback default

    }
    
    /**
     *    Constructor for dummy nodes; used to flag possibly expandable nodes 
     *    when their children status is unknown (due to not having been read 
     *    from the directory yet.)
     */
    
    public SmartNode() 
    { 
        super(); 
        log.finer("created null SmartNode (I)");
        dummy = true; 
        nodeObjectClass="default"; 
        //distinguishedValue = "null"; 
    }
    
    /**
     * Simple constructor, for when objectClass attributes are not known
     * @param rdnString the relative distinguished name, e.g. 'cn=fnord'
     */
     
    public SmartNode(String rdnString) 
    { 
        super(); 
        log.finer("created SmartNode (II) :" + rdnString);
        update(rdnString); 
    }
    
    /**
     * Simple constructor, for when objectClass attributes are not known
     * @param rdn the relative distinguished name, e.g. 'cn=fnord'
     */
     
    public SmartNode(RDN rdn) 
    { 
        super(); 
        log.finer("created SmartNode (IIb) :" + rdn);
        update(rdn); 
    }
    
    /**
     * Copy constructor, for when an RDN is the same, but the tree position
     * (and hence the full DN) is different.  <b>Does Not</b> makes copies of children:
     * use copyChildren() separately if you want this.
     *
     *  @param S the node to copy for initial values.
     */
     
     public SmartNode(SmartNode S) 
     { 
         super(); 
         log.finer("created SmartNode (III) :" + S.toString());
         //distinguishedValue = new String(S.distinguishedValue);
         nodeObjectClass = new String(S.nodeObjectClass);
         icon = S.icon;
         update(S.getRDN());
         dummy = S.dummy;
     }
    
    /** 
     *  When objectClass attributes are known, we try to be cleverer getting
     *  the icon for this node.
     * 
     *  @param RDN the RDN of the new node (e.g. 'cn=fnord')
     *  @param objectClasses a javax.naming.directory Attribute containing
     *         a list of the node's ldap objectClasses.
     */

    // XXX how to choose between conflicting object classes?  At the moment
    // XXX this picks up the first one that it can match an icon to...

    public SmartNode(String RDN, DXAttribute objectClasses) 
    {
        super();
        log.finer("created SmartNode (IV) :" + RDN);
        
        update(RDN);
        
        setTrueObjectClass(objectClasses);
        
    }        

    /**
     *    If available, it is best to use the object class of the 
     *    entry (to determine which icon to display 'n stuff).  This
     *    does a quick 'n dirty search if the object class attribute
     *    is known.  Better is to manually set the deepest object class,
     *    but this requires knowledge of the schema 'n stuff...
     */
             
    public void setTrueObjectClass(DXAttribute objectClasses)
    {        
        try
        { 
            NamingEnumeration obClasses = objectClasses.getAll();        
            while (obClasses.hasMoreElements())  // use non-error-checking form
            {
                String value = obClasses.nextElement().toString();
                if (setTrueObjectClass(value)) 
                    break;
            }                    
        }
        catch (javax.naming.NamingException e)
        {
            log.log(Level.WARNING, "Naming Exception parsing " + rdn +"\n", e);
        }
    }

    /** 
     *    This attempts to set the object class of the node to a particular
     *    value.  It returns true if an icon is available for that object
     *    class, false otherwise.
     */
     
    public boolean setTrueObjectClass(String value)
    {
        value = value.toLowerCase();
        if (icons.containsKey(value))
        {
            nodeObjectClass = value;
            icon = (ImageIcon) icons.get(nodeObjectClass);
            
            return true;
        }
        else
            return false;    
    }

    /**
     *    Takes an ldap RDN string such as 'ou=DemoCorp', and breaks it into
     *    a nodeObjectClass ('ou') and a distinguished value ('DemoCorp'),
     *    and replaces the existing values of these variables.
     *
     *    @param rdn the new RDN to replace the nodes current value.
     */

    public void update(String rdn)
    {
        try 
        { 
            update(new RDN(rdn)); 
        } 
        catch (Exception e) 
        {
            log.warning("unexpected error in SmartNode:update() " + e.toString());
            e.printStackTrace();
        } // should never throw an exception...
    }
    
    public void update(RDN newRDN)
    {

        if (newRDN==null) 
            setRdn(emptyRDN);
        else    
            setRdn(newRDN);

        if (rdn.isEmpty())            // probably a root entry
        {
            nodeObjectClass="default"; 
        }

        if (nodeObjectClass == null)  // in extremis, grab the ldap rdn attribute and use that for icons...
            nodeObjectClass = rdn.getAttID(0);  // use root 'cause DN has only one element...

        // create a collation key for fast language-sensitive sorting...
        // (note toLowerCase() for case insensitive sorting - remove for case sensitive...

        // XXX this is where features such as sorting by object class, or sorting by naming attribute, can be
        // XXX put...

        // 'false' is the default
        boolean sortByNamingAttribute = ("true".equals(JXConfig.getProperty("sort.by.naming.attribute")));

        if (rdn.isMultiValued())
        {
            StringBuffer key = new StringBuffer(rdn.toString().length());
            for (int i=0; i<rdn.size(); i++)
            {
                if (sortByNamingAttribute)
                    key.append(rdn.getRawVal(i)).append(rdn.getAttID(i));
                else
                    key.append(rdn.getAttID(i)).append(rdn.getRawVal(i));
            }
            collationKey = myCollator.getCollationKey(key.toString().toLowerCase());
        }
        else
        {
            if (sortByNamingAttribute)
                collationKey = myCollator.getCollationKey(nodeObjectClass + getDistinguishedValue().toLowerCase());
            else
                collationKey = myCollator.getCollationKey(getDistinguishedValue().toLowerCase() + nodeObjectClass);
        }
    }

    /**
     *    A utility ftn that makes copies of all the child nodes
     *    given to it, and adds the copies to the current node.
     *    @param children an enumeration of SmartNode(s), to copy
     *           and add.
     */
     
    public void copyChildren(Enumeration children)
    {
        while (children.hasMoreElements())
        {
            SmartNode A = new SmartNode((SmartNode)children.nextElement());
            add(A);
        }
    }
    
    /**
     *    Returns the RDN of the tree node as a String.
     *    @return the RDN as a string.
     */
     
    public RDN getRDN() {
        return (blankRoot==true)?emptyRDN:rdn; }
    
    /**
     *    Writes out the nodes RDN as a string, 
     *    @return the RDN, or a message string
     *    for special values (such as root, usually 'World', or dummy values, usually
     *    'reading...'.
     */
    
    public String toString() 
    {     
        if (blankRoot==true) return ROOTNAME;
        if (dummy) return DUMMYMESSAGE;
        //return distinguishedValue; 
        //return rdn.getRawVal(0);
        //return getDistinguishedValue();
        return rdn.toString();
    }
    
    /**
     *    returns the ldap class type of the node
     *    WARNING - Currently not properly implemented; Returns (first) attribute naming value instead...
     *    @return the ldap class type as a string.
     */
    
    public String getObjectClass() { return nodeObjectClass; }
    
    /**
     *    Writes out the nodes RDN as a string, displaying just the value of the RDN
     *    @return distinguishedValue the value part of the RDN
     */
    
    //XXX performance - this is used by comparator, but currently is slow.  
    //XXX maybe speed up?
    
    public String getDistinguishedValue() 
    {
        if (rdn.isMultiValued())
        {
            int size = rdn.size();
            StringBuffer val = new StringBuffer();
            for (int i=0; i<size; i++)
            {
                if (i>0) 
                    val.append("  +  ");
                val.append(rdn.getRawVal(i));
            }
            return val.toString();        
        }
        else
            return rdn.getRawVal(0);
    }

    /**
     *    returns the icon name (without the extension).  The Icon
     *    name is either one of the objectClass names (that an icon
     *    can be found for) or failing that the ldap class name.
     *    @return the string used to identify the icon.
     */
             
    public String getIconName() { return nodeObjectClass; }

    /**
     *    Sometimes a dummy node is used to signal that there may
     *    be child nodes below a given node, but we haven't actually
     *    read those nodes yet.
     *
     *    @return whether this node is a dummy node holding no real data.
     */

    public boolean isDummy() { return dummy; }

    /**
     *    Returns whether this node has a dummy node.
     *    (In a normal tree, the dummy node will be the only node)
     *    
     */
     
    public boolean hasDummy() 
    {
        if (getChildCount() == 0) return false;
        
        return ((SmartNode)getChildAt(0)).isDummy();
    }

    /**
     *    Returns the string to display when the node is a 'dummy'.
     *    @return dummy message, such as 'reading...'
     */
     
     public String getDummyMessage() 
     {
         return DUMMYMESSAGE;
     }

    /** 
     *    The root node is sometimes blank, but displayed to the user with a
     *    different name.  This simply returns whether the node is the
     *    root node - use @isBlankRoot to determine whether it is both
     *    a root node and blank...
     */
     
    public boolean isRoot() { return root; } 

    /** 
     *    The root node is sometimes blank.  This means it is 
     *    displayed as ROOTNAME (usually 'cn=World') in the tree,
     *    and written out as a zero length string in other contexts
     *    such as dumping ldif files.
     */
     
    public boolean isBlankRoot() { return blankRoot; } 
    
    /**
     *    Returns the name of the 'blank root' (i.e. "World" or similar).
     *    @return the blank root name, suitable for display to a user.
     */
     
    public String getBlankRootName() { return ROOTNAME; }
    
	/**
	 *	Experimental - returns true if the node should always
	 *  'refresh' when it is expanded by the tree...
	 */
	 
	public boolean isAlwaysRefresh() { return alwaysRefresh; }

	/**
	 *	Experimental - sets if the node should always
	 *  'refresh' (i.e. reload all children) when it is expanded by the tree...
	 */
	 
	public void setAlwaysRefresh(boolean state)
	{
		alwaysRefresh = state;
	}

    /** 
     *    This sets the root status of the node (default=false).
     *    If set to true, the root node will return ROOTSTRING as
     *    a name for display, rather than a blank.
     */
     
     public void setRoot(boolean state) 
     { 
         root = state; 
         blankRoot = false;
         if ((root == true) && ("".equals(rdn.toString())))
         {
             update("");
             blankRoot = true;
             nodeObjectClass = ROOTNAME.toLowerCase();
         }    
     }

    /**
     *    gets the icon associated with this node.  These are taken from a 
     *    central pool of icons - there is no local copy of the icon held in
     *    the node.
     *
     *    @return the associated icon.  This may be null, if not even the default 
     *            icon has been set.
     */
     
    public ImageIcon getIcon() 
    { 
        if (icon != null) return icon;
        
        icon = (ImageIcon) icons.get(nodeObjectClass.toLowerCase());
        
        if (icon == null)
            icon = (ImageIcon) icons.get("default"); // this may still be null if no icons found...
        
        return icon; 
    }
    
    /** 
     *    Rarely used method to force a particular icon to be used by this particular
     *    node.
     */
     
    public void setIcon(ImageIcon newIcon) { icon = newIcon; }
    
    /**
     *    Returns whether this node has the corresponding node as a child.
     *    @param n the child node.
     */
     
    public boolean hasChild(SmartNode n) { return hasChild(n.toString());}
    
    /**
     *    Returns whether this node has the corresponding node as a child.
     *    @param r the child node's RDN. (Test is case insensitive)
     */
     
    public boolean hasChild(RDN r) { return hasChild(r.toString());}
    
    
    /**
     *    Returns whether this node has the corresponding node as a child.
     *    @param testRDN the child node's RDN as a String.  (Test is case insensitive)
     */
     
    public boolean hasChild(String testRDN)
    {
        Enumeration children = children();
        while (children.hasMoreElements())
        {
            if (testRDN.equalsIgnoreCase(children.nextElement().toString()))
            {
                return true;
            }    
        }
        return false;
    }
    
    public boolean isStructural() { return structural; }
    
    public void setStructural(boolean val) { structural = val; }
    

    /**
     *    Quicky comparator class to sort nodes by distinguished value, rather
     *    than (default) .toString()
     */
/*
    class NodeComparator implements Comparable
    {
        Object myObject;
        String compareString;
        
        public NodeComparator(Object o) 
        {
            myObject = (o==null)?"null":o;
            
            if (myObject instanceof SmartNode)
                compareString = ((SmartNode)myObject).getDistinguishedValue().toLowerCase();
            else
                compareString = myObject.toString().toLowerCase();    
        }
        
        public int compareTo(Object compObject) 
        { 
            return compareString.compareTo(compObject.toString()); 
        }
        
        public String toString() { return compareString; }
        
        public Object getObject() {return myObject; }
    }
*/

    /**
     * Used for sorting of Smart Nodes.
     * @param o the Smart Node to compare against.
     * @return a negative integer, zero, or a positive integer as this Collation Key is less
     * than, equal to, or greater than the given Object.
     * @throws ClassCastException Thrown if the passed object is not another SmartNode.
     */
    public int compareTo(Object o)
        throws ClassCastException
    {
        if (o == null)
            return -1;
        if (((SmartNode)o).collationKey == null)
            return -1;
        if (collationKey == null)
            return +1;       // bingo

        return collationKey.compareTo(((SmartNode)o).collationKey);
    }

    /**
     *  This sorts the children of the current node by the alphabetic 
     *  value of their naming attribute value; e.g. in cn=doofus, by 'doofus'.
     *
     */

    public void sort()
    {
        TreeSet sortedSet = new TreeSet();
        Enumeration kids = children();

        while (kids.hasMoreElements())
        {
            Object kid = kids.nextElement();
            //System.out.println("add node: '" + kid + "'");
            sortedSet.add(kid);
        }
        removeAllChildren();

        Iterator sortedKids = sortedSet.iterator();
        while (sortedKids.hasNext())
        {
            SmartNode newNode = (SmartNode)sortedKids.next();
            //System.out.println("node: " + newNode.toString() + "'");
            add(newNode);
        }
    }
 
 
    /**
     *    Check if a SmartNode has the same RDN as the passed RDN.
     *
     *    @param testRDN the RDN to test against
     *    @return true if the RDNs are equal
     */
     
    public boolean rdnEquals(RDN testRDN)
    {
        return (testRDN.equals(rdn));
    }  

    /**
     *    It is possible to register a special popup menu that is called when
     *    this node is 'right-clicked' on.  Usually you wouldn't bother.
     *    @param popupMenu the popupmenu to register.
     */
     
    public void setPopupMenu(JPopupMenu popupMenu) { menu = popupMenu; }
    
    /**
     *    It is possible to register a special popup menu that is called when
     *    this node is 'right-clicked' on.  Usually you wouldn't bother, but
     *    this returns that menu if you have bothered, and null if you haven't.
     *    @return the pre-registered popupmenu, or (more usually) null.
     */
     
    
    public JPopupMenu getPopupMenu() { return menu; } 
    

    
    /*
     *    Returns whether the rdn is multi-valued.
     */
    public boolean isMultiValued()
    {
        return rdn.isMultiValued();
    }

	//
	//	*** DRAG 'n DROP MAGIC ***
	//    
    
    public DN getDN()
    {
    	if (root) return new DN();
    	
    	DN ret = ((SmartNode)getParent()).getDN();
    	ret.add(getRDN());
    	return ret;
    }
    
	public Object getTransferData(DataFlavor df)
		throws UnsupportedFlavorException, IOException 
	{
		if (df.equals(flavours[0]) == false)  // currently only support one flavour.
			throw new UnsupportedFlavorException(df);
		String dn = getDN().toString();
		return dn;
	}

	//Returns an array of DataFlavor objects indicating the
	//flavors the data can be provided in.
	public DataFlavor[] getTransferDataFlavors() 
	{
		return flavours;
	}

	//Returns whether or not the specified data flavor is supported for this object.
	public boolean isDataFlavorSupported(DataFlavor flavour) 
	{
		 for (int i=0; i<flavours.length; i++)
		 	if ( flavours[i].equals(flavour) ) return true;
		 return false;
	}

    public void setRdn(RDN rdn)
    {
        this.rdn = rdn;
    }

}	
