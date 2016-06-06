package com.ca.commons.naming;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * This is a wrapper for a DXEntry which provides methods to help in reading and writing LDIF entries.
 *
 *
 * (c) Chris Betts; Pegacat Software (http://pegacat.com)
 */
public class LdifEntry extends DXEntry
{
    LdifEntryType changeType = LdifEntryType.normal;

    private static Logger log = Logger.getLogger(LdifEntryType.class.getName());

    public static final String DELETEOLDRDN = "deleteoldrdn"; // RFC 2849 attributes for moddn/modrdn operations
    public static final String NEWSUPERIOR = "newsuperior";
    public static final String NEWRDN = "newrdn";
    public static final String LINENO = "linenumber"; // used in preview mode to hold the line of the LDIF entry for reporting...

    public static final String CHANGE_TYPE_ATTRIBUTE = "changetype"; // 'fake' changetype attribute used for LDIF entry display...

    public static final String ERROR = "error";     // synthetic attributes for error reporting in UI
    //public static final String WARNING ="warning";

    public LdifEntry()
    {
        super();
        commonInit();
    }

    public LdifEntry(DN dn)
    {
        super(dn);
        commonInit();
    }

    /**
     * Mainly used for testing...
     * @param stringName
     * @param atts
     */
    public LdifEntry(String stringName, Attribute[] atts)
    {
        super(makeAtts(atts), new DN(stringName));
        commonInit();
    }

    /**
     * This wraps the passed attributes object.
     *
     * It does <b>NOT</b> copy the underlying data, it
     * references it directly.
     *
     * @param baseAttributes
     */
    public LdifEntry(DXAttributes baseAttributes)
    {
        basicCopy(baseAttributes);
        commonInit();
    }

    public LdifEntry(DXAttributes baseAttributes, DN dn)
    {
        basicCopy(baseAttributes);
        this.dn = dn;
        commonInit();
    }

    public LdifEntry(DXEntry baseEntry)
    {
        basicCopy(baseEntry);
        this.dn = baseEntry.dn;
        commonInit();
    }


    // common code run by all the more basic constructors.
    void basicCopy(DXAttributes baseAttributes)
    {
        id = ID++;
        atts = baseAttributes.atts;
        must = baseAttributes.must;

        if (atts.containsKey(CHANGE_TYPE_ATTRIBUTE)) // special handling for LDIF Change Type records...
        {
            try
            {
                String val = atts.get(CHANGE_TYPE_ATTRIBUTE).get().toString();
                for (LdifEntryType entryType: LdifEntryType.values())
                    if (val.equals(entryType.toString()))
                    {
                        setChangeType(entryType);
                        System.out.println("SET TO: " + entryType );
                        return;
                    }
            }
            catch (NamingException e)
            {
                log.warning("Unexpected internal error setting LDIF change entry type " + e.getMessage());
            }


        }

    }

    private void commonInit()
    {
        must.add(CHANGE_TYPE_ATTRIBUTE);
    }

    public void setChangeType(LdifEntryType type)
    {
        changeType = type;
        if (type != LdifEntryType.normal)
            this.put(new BasicAttribute(CHANGE_TYPE_ATTRIBUTE, type.toString()));
    }

    public LdifEntryType getChangeType()
    {
        return changeType;
    }

    public String toString()
    {
        try
        {
            StringBuffer ldifData = new StringBuffer(1024);
            ldifData.append("dn" + LdifUtility.ldifEncode(getStringName(), 2) + "\n");

            if (changeType != LdifEntryType.normal)
                ldifData.append("changetype: ").append(changeType.toString()).append("\n");

            switch (changeType) {

                case normal:
                case add:

                    // write out the object class
                    ArrayList<String> objectClasses = getOrderedOCs();
                    if (objectClasses == null)
                        return "objectclass:  error - no object class found";

                    for (String ocValue: objectClasses )
                        ldifData.append(LdifUtility.ldifEncode(this.getAllObjectClasses().getID(), ocValue));

                    // write out the other attributes
                    for (DXAttribute att: getAttArrayList())
                    {
                        if (!att.isObjectClass())     // skip the object class
                            if (!att.getID().equals(CHANGE_TYPE_ATTRIBUTE)) // skip the change type attribute (handled implicitly above)
                                for (Object value: att.getValues())  // empty attributes will be skipped
                                    ldifData.append(LdifUtility.ldifEncode(att.getID(), value));
                    }

                    break;

                case delete:   // actually nothing to do; all delete needs is the dn and the changetype.
                    break;

                case modify:

                    // write out replaces first
                    for (DXAttribute att: getAttArrayList())
                    {
                        if (!att.getID().equals(CHANGE_TYPE_ATTRIBUTE))
                        {
                            LdifModifyAttribute ldapAtt = (LdifModifyAttribute) att;
                            if (ldapAtt.modifyType == LdifModifyType.replace)
                                ldifData.append(ldapAtt.toString());
                        }
                    }

                    // then write out adds
                    for (DXAttribute att: getAttArrayList())
                    {
                        if (!att.getID().equals(CHANGE_TYPE_ATTRIBUTE))
                        {
                            LdifModifyAttribute ldapAtt = (LdifModifyAttribute) att;
                            if (ldapAtt.modifyType == LdifModifyType.add)
                                ldifData.append(ldapAtt.toString());
                        }
                    }

                    // then deletes
                    for (DXAttribute att: getAttArrayList())
                    {
                        if (!att.getID().equals(CHANGE_TYPE_ATTRIBUTE))
                        {
                            LdifModifyAttribute ldapAtt = (LdifModifyAttribute) att;
                            if (ldapAtt.modifyType == LdifModifyType.delete)
                                ldifData.append(ldapAtt.toString());
                        }
                    }

                    break;

                case moddn:
                case modrdn:

                    DXAttribute rdn = get(NEWRDN);
                    DXAttribute deleteOldRdn = get(DELETEOLDRDN);
                    DXAttribute newSuperior = get(NEWSUPERIOR);

                    if (rdn == null)
                        ldifData.append("error: no 'newrdn' attribute found");
                    else if (deleteOldRdn == null)
                        ldifData.append("error: no 'deleteoldrdn' attribute found");
                    else
                    {
                        // write out rdn, and the optional deleteoldrdn and newsuperior atts
                        ldifData.append(LdifUtility.ldifEncode(rdn.getID(), rdn.get()));
                        ldifData.append(LdifUtility.ldifEncode(deleteOldRdn.getID(), deleteOldRdn.get()));
                        if (newSuperior != null)
                            ldifData.append(LdifUtility.ldifEncode(newSuperior.getID(), newSuperior.get()));
                    }
                    break;
            }
            ldifData.append("\n");       // all entries get a clear line break after them...
            return ldifData.toString();
        }
        catch (ClassCastException e)
        {
            log.warning("error trying to write out LDIF entry '" + dn + "'; LDIF change attribute not found when expected " + e.getMessage());
            return "";
        }
        catch (NamingException e)
        {
            log.warning("unexpected naming exception trying to write out LDIF entry '" + dn +  "' " + e.getMessage());
            return "";
        }
    }
}
