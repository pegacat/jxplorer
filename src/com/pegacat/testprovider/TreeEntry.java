package com.pegacat.testprovider;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * (c) GroupMind Project
 * - Dr Christopher Betts 2005
 */
class TreeEntry extends BasicAttributes
{
    public static String OBJECTCLASS = "objectClass";
    public static String PLACEHOLDER = "placeHolder";
    Name name;
    ArrayList<TreeEntry> children;

    /**
     * Creates an 'empty' place holder node
     * with the object class 'placeHolder'
     *
     * @param entryName
     */
    public TreeEntry(Name entryName)
    {
        this(entryName, new String[]{OBJECTCLASS, PLACEHOLDER});
    }

    /**
     * basic housekeeping code common to all constructors
     */

    private void setup(Name entryName)
    {
        name = entryName;
        children = new ArrayList<TreeEntry>();
    }

    /**
     * Initialise a Test Entry with a name and an array of attribute
     * name and (single) value pairs.  E.g. {"objectclass","person","cn","Fred"}
     *
     * @param entryName               the Name of the entry (usually an LdapName)
     * @param attributeNamesAndValues a list of single valued attribute name and values
     */
    public TreeEntry(Name entryName, String[] attributeNamesAndValues)
    {
        super(true);

        setup(entryName);

        for (int i = 0; i < attributeNamesAndValues.length; i = i + 2)
            this.put(new BasicAttribute(attributeNamesAndValues[i], attributeNamesAndValues[i + 1]));
    }


    /**
     * Initialise a Test Entry with a name and a set of attributes
     *
     * @param entryName the Name of the entry (usually an LdapName)
     * @param entryAtts the set of attributes
     */
    public TreeEntry(Name entryName, Attributes entryAtts)
    {
        super(true);
        
        setup(entryName);

        Enumeration newAtts = entryAtts.getAll();
        while (newAtts.hasMoreElements())
            this.put((Attribute) newAtts.nextElement());
    }

    public Object clone()
    {
        return new TreeEntry(this.getName(), (Attributes) super.clone());
    }

    public Attributes cloneAtts()
    {
        BasicAttributes newAtts = new BasicAttributes();
        NamingEnumeration atts = getAll();
        while (atts.hasMoreElements())
        {
            newAtts.put((BasicAttribute)((BasicAttribute)atts.nextElement()).clone());
        }
        return newAtts;
    }

    /**
     *
     * @param child
     */
    public void addChild(TreeEntry child)
    {
        children.add(child);
    }

    public void removeChild(TreeEntry child)
    {
        children.remove(child);
    }

    public void replaceChild(TreeEntry oldChild, TreeEntry newChild)
    {
        children.remove(oldChild);
        children.add(newChild);
    }

    public ArrayList<TreeEntry> getChildren()
    {
        return children;
    }

    public Name getParent()
    {
        if (name.size() == 0)
            return null;     // the root node has no parent
        else
            return name.getPrefix(name.size() - 1);
    }

    public void setName(Name newName)
    {
        name = newName;
    }

    public Name getName()
    {
        return name;
    }

    public String getStringName()
    {
        return name.toString();
    }

    public String toString()
    {
        try
        {
            StringBuffer text = new StringBuffer(getStringName()).append("\n");
            for (TreeEntry child : children)
                text.append("     -> ").append(child.getName()).append("\n");
            NamingEnumeration atts = getAll();
            while (atts.hasMoreElements())
            {
                Attribute att = (Attribute) atts.nextElement();
                text.append("    * ").append(att.getID());
                NamingEnumeration vals = att.getAll();
                while (vals.hasMore())
                    text.append(" : " + vals.next());
                text.append("\n");
            }
            return text.toString();
        }
        catch (NamingException e)
        {
            return "unexpected exception: " + e.toString();
        }
    }

    public boolean equals(Object obj)
    {
        try
        {
            return ((TreeEntry)obj).getName().equals(name);
        }
        catch (ClassCastException e)
        {
            return false;
        }
    }
}
