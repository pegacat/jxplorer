package com.ca.commons.naming;

import com.ca.commons.cbutil.CBBase64;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import java.util.logging.Logger;

/**
 * Utility extension to DXAttribute that adds an LDIF 'changetype: modify' type (see LdifModifyType add|delete|replace) to
 * a normal DXAttribute, and provides an RFC 2849 compliant LDIF print out of that attribute suitable for writing to
 * an LDIF change file.
 *
 * (c) Chris Betts; Pegacat Software (http://pegacat.com)
 */

public class LdifModifyAttribute extends DXAttribute
{
    public LdifModifyType modifyType;

    private final static Logger log = Logger.getLogger(LdifModifyAttribute.class.getName());  // ...It's round it's heavy it's wood... It's better than bad, it's good...

    public LdifModifyAttribute(String ID, Object attVal, LdifModifyType type)
    {
        super(ID, attVal);
        common(type);
    }

    public LdifModifyAttribute(String ID, LdifModifyType type)
    {
        super(ID);
        common(type);
    }
    
    public LdifModifyAttribute(Attribute att, LdifModifyType type)
    {
        super(att);
        common(type);
    }

    private void common(LdifModifyType type)
    {
        modifyType = type;
        this.add(0, modifyType.toString());
    }

    public boolean isOrdered() {return true;}

    /**
     * Returns a traditional attribute without the synthetic 'modify type' value at the beginning.
     * @return
     */
    public Attribute getCleanAttribute()
    {
        DXAttribute att = new DXAttribute(this.getID());
        try
        {
            for (int i=1; i<size(); i++)
                att.add(get(i));
        }
        catch (NamingException e)
        {
            log.severe("Unexpected exception during internal enumeration of existin gLdifModifyAttribute values " + e.getMessage());
        }
        return att;
    }
    /**
     * returns the text of a single LDIF modify attribute record; e.g.
     * <pre>
     * replace: manager
     * manager: cn=Chris Wilberforce Penderstock III,ou=People,dc=example,dc=com
     * -
     *
     * or
     * add: phone
     * phone: 999 769 332
     * phone: 999 769 337
     * -
     *
     * </pre>
     * @return
     */
    public String toString()
    {
        StringBuffer buffy = new StringBuffer(100);

        buffy.append(modifyType.toString()).append(": ").append(getID()).append("\n");

         try
         {
             NamingEnumeration vals = getAll();
             while (vals.hasMore())
             {
                 Object o = vals.next();

                 if (!modifyType.toString().equals(o))   // skip the 'synthetic' modify type value
                 {
                     if (!isString)
                     {
                         try
                         {
                             byte[] b = (byte[])o;
                             if (b!=null)
                                buffy.append(getID()).append(": ").append(CBBase64.binaryToString(b)).append("\n");
                         }
                         catch (ClassCastException cce)
                         {
                             buffy.append(getID()).append(": ").append(": unexpected error - not a byte array?");
                         }
                     }
                     else
                     {
                         if (o != null)
                         {
                            buffy.append(getID()).append(": ").append(o.toString()).append("\n");
                         }
                     }
                 }
             }

             return buffy.append("-\n").toString();
         }
         catch (NamingException e)
         {
             log.warning("error listing values for " + getID() +  ": " + e.getMessage() + " ... skipping");
             return "";
         }
    }
}
