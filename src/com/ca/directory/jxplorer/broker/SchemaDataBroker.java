package com.ca.directory.jxplorer.broker;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;

//import com.ca.directory.jxplorer.tree.*;
import com.ca.commons.naming.*;
import com.ca.commons.jndi.SchemaOps;

import java.util.*;
import java.util.logging.Logger;

/**
 *    This wraps the schemaOps searching functionality of a
 *    JNDIDataBroker, and makes it work as a DataBrokerQueryInterface
 *    so that a SmartTree object can use it to browse the 
 *    schemaOps.
 */
 
public class SchemaDataBroker extends DataBroker
{
    JNDIDataBroker jndiBroker;
    
    SchemaOps schemaOps;

    private final static Logger log = Logger.getLogger(SchemaDataBroker.class.getName());

    public SchemaDataBroker(JNDIDataBroker jb)
    {
        registerDirectoryConnection(jb);
    }
    
    public void registerDirectoryConnection(JNDIDataBroker broker)
    {
        jndiBroker = broker;
        schemaOps = jndiBroker.getSchemaOps();
    }

    /**
     *    Takes a DXAttributes set representing class schemaOps defs,
     *    and translates the oids into human friendly strings...
     */
/*
    protected Attributes addClassInfo(Attributes classdefs)
    {
        if ((classdefs instanceof DXAttributes) && ((DXAttributes)classdefs).hasOIDs() == false) // there are no OIDs requiring translation - we must be using ldap strings instead
        {
            return classdefs;        
        }
        
        try
        {
            Attribute must = classdefs.get("MUST");            // get syntax attribute
            if (must != null)
            {
                DXNamingEnumeration musts = new DXNamingEnumeration(must.getAll());   
                BasicAttribute mustDesc = new BasicAttribute("(MUST-DESC)");
                while (musts.hasMore())
                    mustDesc.add(getAttributeDescription(musts.next().toString()));    
                classdefs.put(mustDesc);
            }
            
            Attribute may  = classdefs.get("MAY");
            if (may != null)
            {
                DXNamingEnumeration mays  = new DXNamingEnumeration(may.getAll());   
                BasicAttribute mayDesc = new BasicAttribute("(MAY-DESC)");
                while (mays.hasMore())
                    mayDesc.add(getAttributeDescription(mays.next().toString()));    
                classdefs.put(mayDesc);
            }

            return classdefs;
        }
        catch (NamingException e)      { return classdefs; }
        catch (NullPointerException e) { return classdefs; }
    }
*/
/*
    public DataQuery getAttributeDefinition(String attributeoid)
    {
        return getEntry(new DN("schemaOps="+attributeoid + ",schemaOps=AttributeDefinition"));
    }
*/
     /**
      *    Convenience Class; makes a request for the 
      *    passed value, appending 'AttributeDefinition' to the dn.
      *    @param attributeoid the name of the attribute to get info for
      *    @return an Attributes object containing the (server
      *            dependant) available data...
      */
/*
    public String getAttributeDescription(String attributeoid)
    {
         
        return jndiBroker.getAttributeDescription(attributeoid) ; //+ " (" + attributeoid + ")";
    }
*/
     /**
      *    Convenience Class; makes a request for the 
      *    passed syntax definition.
      *    @param syntaxName the name of the syntax to get info for
      *    @return an Attributes object containing the (server
      *            dependant) available data...
      */
/*
    public DataQuery getSyntaxDefinition (String syntaxName)
    {
        if (syntaxName.indexOf('{') > -1)
            syntaxName = syntaxName.substring(0, syntaxName.indexOf('{'));

        return getEntry(new DN("schemaOps="+syntaxName + ",schemaOps=SyntaxDefinition,cn=schemaOps"));
    }
*/
     /**
      *    Convenience Class; makes a request for the 
      *    passed value, appending 'SyntaxDefinition' to the dn.  
      *    Looks for a 'DESC' field in the reTranslates the resulting
      *    @param syntaxName the name of the syntax to get info for
      *    @return an Attributes object containing the (server
      *            dependant) available data...
      */
/*
    public String getSyntaxDescription (String syntaxName)
    {
        try
        {
            //System.out.println("looking up " + "schemaOps="+syntaxName + ",schemaOps=SyntaxDefinition,cn=schemaOps");
            
            // try hard coded list first, from RFC .
             
            String oidName = null;
             
             oidName = (String)syntaxHash.get(syntaxName);  // try quick look up
             if (oidName == null)                           // ... fail to a directory query.
             {
                 Attributes syntaxdefs = getEntry(new DN("schemaOps="+syntaxName + ",schemaOps=SyntaxDefinition,cn=schemaOps")).getEntry();
                 oidName = syntaxdefs.get("DESC").get().toString();


                 if (oidName != null)
                     syntaxHash.put(syntaxName, oidName);   // and add it to the hash for later... :-)
             }    
             return oidName;
         }
         catch (NullPointerException e) { return null; } 
         catch (NamingException e2)     { return null; }
     }
*/

     /**
      *    Convenience Class; makes a request for the 
      *    passed value, appending 'ClassDefinition' to the dn.
      *    @param className the name of the class to get info for
      *    @return an Attributes object containing the (server
      *            dependant) available data...
      */
/*
     public DataQuery getClassDefinition (String className)
     {
         return getEntry(new DN("schemaOps="+className + ",schemaOps=ClassDefinition"));
     }
*/

/*
    protected void loadSyntaxHash()
    {
         // a quick pick of common syntaxes for Active Directory support
         // (and other servers that don't publish syntax descriptions)
         // taken from rfc 2252
        syntaxHash = new Hashtable(70);

        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.1","ACI Item");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.2","Access Point");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.3","Attribute Type Description");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.4","Audio");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.5","Binary");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.6","Bit String");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.7","Boolean");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.8","Certificate");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.9","Certificate List");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.10","Certificate Pair");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.11","Country String");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.12","DN");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.13","Data Quality Syntax");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.14","Delivery Method");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.15","Directory String");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.16","DIT Content Rule Description");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.17","DIT Structure Rule Description");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.18","DL Submit Permission");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.19","DSA Quality Syntax");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.20","DSE Type");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.21","Enhanced Guide");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.22","Facsimile Telephone Number");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.23","Fax");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.24","Generalized Time");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.25","Guide");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.26","IA5 String");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.27","INTEGER");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.28","JPEG");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.54","LDAP Syntax Description");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.56","LDAP Schema Definition");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.57","LDAP Schema Description");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.29","Master And Shadow Access Points");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.30","Matching Rule Description");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.31","Matching Rule Use Description");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.32","Mail Preference");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.33","MHS OR Address");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.55","Modify Rights");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.34","Name And Optional UID");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.35","Name Form Description");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.36","Numeric String");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.37","Object Class Description");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.40","Octet String");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.38","OID");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.39","Other Mailbox");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.41","Postal Address");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.42","Protocol Information");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.43","Presentation Address");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.44","Printable String");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.58","Substring Assertion");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.45","Subtree Specification");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.46","Supplier Information");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.47","Supplier Or Consumer");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.48","Supplier And Consumer");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.49","Supported Algorithm");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.50","Telephone Number");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.51","Teletex Terminal Identifier");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.52","Telex Number");
        syntaxHash.put("1.3.6.1.4.1.1466.115.121.1.53","UTC Time");
     }
*/
     //temp
/*
     public static boolean isAttributeBinary(String attID)
     {
         return false;
     }
*/
//     public DataBrokerQueryInterface getDataSource() { return null; } // can't modify schemaOps data...
     

     
    /**
     *  Operation is not allowed - sets an exception in the request 
     */
     
    public DataQuery doExistsQuery(DataQuery request)
    {
        return request.setException(new Exception("schemaOps exists not yet implemented")); //XXX
    }

     
    protected DataQuery doSearchQuery(DataQuery request)
    {
        return request.setException(new Exception("schemaOps search not allowed"));
    }
                
    /**
     *  Operation is not allowed - sets an exception in the request 
     */
     
    protected DataQuery doModifyQuery(DataQuery request)
    {
        return request.setException(new Exception("schemaOps modification not allowed"));
    }
                
    /**
     *  Operation is not allowed - sets an exception in the request 
     */
     
    protected DataQuery doCopyQuery(DataQuery request)
    {
        return request.setException(new Exception("schemaOps copy not allowed"));
    }
                
    /**
     *  Operation is not allowed - sets an exception in the request 
     */
     
    protected DataQuery doGetAllOCsQuery(DataQuery request)
    {
        return request.setException(new Exception("schemaOps object class list not allowed"));
    }
                
    /**
     *  Operation is not allowed - sets an exception in the request 
     */
     
    protected DataQuery doGetRecOCsQuery(DataQuery request)
    {
        return request.setException(new Exception("schemaOps rec. object class list not allowed"));
    }  
    
    
    /**
     *   returns the next level of a directory tree, returning
     *   a Enumeration of the results
     *
     *   @param searchbase the node in the tree to expand
     *   @return list of results (NameClassPair); the next layer of the tree... 
     */
     
    public DXNamingEnumeration unthreadedList(DN searchbase)
    {
        try
        {
            if (schemaOps == null) // not initialised.
                return null;

            ArrayList nextLevel = schemaOps.listEntryNames(searchbase.toString());
            int size = nextLevel.size();
            for (int i=0; i<size; i++)
            {
                nextLevel.set(i, new NameClassPair("schema="+ (String)nextLevel.get(i),"schema", false));
            }
            return new DXNamingEnumeration(nextLevel);
        }
        catch (NamingException e)
        {
            System.out.println("hurm.");
            return null;
        }
    }
    
    /**
     *   Not Implemented.
     *
     *   @param dn the domain name (relative to initial context in ldap) to seach from.
     *   @param filter the non-null filter to use for the search 
     *   @param search_level whether to search the base object, the next level or the whole subtree.
     *   @param returnAttributes a vector of string names of attributes to return in the search.  (Currently inoperative)
     *   @return list of results ('SearchResult's); the next layer of the tree... 
     */
     
    public DXNamingEnumeration unthreadedSearch(DN dn, String filter, int search_level, String[] returnAttributes) { return null; }
    
   /**
     *   Not Implemented.
    *     
    *    @param oldNodeDN the original DN of the sub tree root 
    *           to be copied (may be a single entry).
    *    @param newNodeDN the target DN for the tree to be moved to.
    */
    public void unthreadedCopy(DN oldNodeDN, DN newNodeDN)
        throws NamingException
   {
       throw new NamingException("unable to modify schema");
   }

    @Override
    public void unthreadedCopyBetweenWindows(DN oldNodeDN, DataBrokerUnthreadedInterface externalDataSource, DN newNodeDN, boolean updateExistingEntries)
            throws NamingException
    {
        throw new NamingException("unable to modify schema");
    }

    /**
     *   Not Implemented.
     *    @return whether the entry could be found in the directory.
     */
     
    public boolean unthreadedExists(DN checkMe) {return false;}
    
    /**
     *   Not Implemented.
     */

    public Vector unthreadedGetAllOCs() { return null; }

    /**
     *   Reads a 'virtual' schemaOps entry.  (i.e. reads a schemaOps attribute value, parsing and
     *   presenting the information as if it were an entry in its own right).
     *   @param entryDN the DN of the object to read.
     *   @param returnAttributes a vector of string names of attributes to return in the search.
     *          (null means 'return all entries', a zero length array means 'return no attributes'.)
     *   @return an entry returning a schemaOps group; null if unreadable.
     */
     
    public DXEntry unthreadedReadEntry(DN entryDN, String[] returnAttributes)
        throws NamingException
    {
            if (schemaOps == null) // not initialised.
                return null;

            DXEntry returnEntry = new DXEntry(entryDN);

            Attributes atts = schemaOps.getAttributes(entryDN.toString());

            if (atts == null || atts.size() == 0)
            {
                returnEntry = null;                 // no result
            }
            else if (returnAttributes == null)
            {
                returnEntry.put(atts.getAll());     // return the complete schemaOps entry
            }
            else if (returnAttributes.length == 0)
            {
                                                   // do nothing - just return the named, empty, DXEntry object
            }
            else                                    // XXX - may need to be implemented at some stage...
            {
                log.warning("return of partial schemaOps attributes not implemented - returning all");
                returnEntry.put(atts.getAll());
            }

            return returnEntry;
/*
        Attributes temp;

    	// add a synthetic schemaOps object class, so that it looks and acts like a normal
    	// directory entry.
        BasicAttribute oc = new BasicAttribute("objectClass", "top");
        oc.add("schemaOps");

        temp = jndiBroker.getSchemaData(entryDN, returnAttributes);

        if (temp == null)
        {
            System.err.println("unable to read schemaOps; null attributes returned");
            return null;
        }

        if (useRawData == false)
        {
            // Do some translation work to make things pretty for users.
            String type = entryDN.getRDNValue(1);
            oc.add(type);

            if (type.equals("AttributeDefinition"))
                temp = addAttributeInfo(temp);
            else if (type.equals("ClassDefinition"))
                temp = addClassInfo(temp);
    //      else if (type.equals("SyntaxDefinition"))
    //          temp = addSyntaxInfo(temp);
    		temp.put(oc);

        }

        return new DXEntry(temp, entryDN);
*/
    }

   /**
     *   Not Implemented.
    *    @param oldEntry the old set of attributes of the object.
    *    @param newEntry the replacement set of attributes..
    */
    
    public void unthreadedModify(DXEntry oldEntry, DXEntry newEntry)
           throws NamingException
      {
          throw new NamingException("unable to modify schema");
      }

    /**
     *   Not Implemented.
     *    @param dn the dn of the parent to determine likely
     *              child object classes for
     *    @return list of recommended object classes...             
     */
    
    public ArrayList unthreadedGetRecOCs(DN dn) { return null; }
    
    
    
    
    
    
    
    
    
    
    
    
    
    /** 
     *   Chains request to jndiBroker to satisfy broker interface
     */
     
    public LdapContext getLdapContext() throws NamingException { return jndiBroker.getLdapContext(); }
 
    /**
     *    Whether the schemaOps is modifiable - usually false.
     */
     
    public boolean isModifiable() { return false; }
    
    /**
     *    The schemaOps DataBroker is always active (even if there is no connection)
     *    because it will fall back on hard-coded default values if necessary.
     */
     
    public boolean isActive() { return true; }
    
    /** 
     *   Chains request to jndiBroker to satisfy broker interface
     */
     
    public SchemaOps getSchemaOps() { return schemaOps; }

    public String id() { return "SchemaDataBroker " + id;};

    
}