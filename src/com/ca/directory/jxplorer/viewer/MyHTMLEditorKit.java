package com.ca.directory.jxplorer.viewer;


import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.naming.directory.*;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URLDecoder;
import com.ca.commons.naming.*;
import com.ca.commons.cbutil.*;

/**
 *    A collection of evil hacks to get inside the (*^^(&* HMTL Editor Kit
 *    Model, and disable the automated HTML form submission code, allowing
 *    us to intercept form submissions.
 */
class MyFormView extends FormView
{
    /**
     * Name of the Submit button.  This must be removed on form submit, so
     * that it doesn't get added to the entry.
     */
    public static final String SUBMIT = "Submit";

    HTMLTemplateDisplay templateDisplay;
	
    private static Logger log = Logger.getLogger(MyHTMLEditorKit.class.getName());

//	int  numberNamingValues;			//TE: these four are used in setRDN...
//	AttributeValue [] namingValues;

    public MyFormView(Element elem, HTMLTemplateDisplay display) 
    {
        super(elem);

        templateDisplay = display;
    }

    /**
     *    This method is what all the fuss in this whole class is about.  It over-rides the
     *    method in the standard class which would try to submit this to a web server, and
     *    instead we parse the form data and try to submit it to the directory.  Pretty neat
     *    ay?	
     */
     
    protected void submitData(String data) 
    {
		DXEntry oldEntry = templateDisplay.currentEntry;     	//TE: get the old entry (i.e its attributes & values).

        log.finest("Entry before changes: " + oldEntry);

        DN  dn  = oldEntry.getDN();                             // Get the original DN
        RDN rdn = new RDN(oldEntry.getRDN());          			//TE: get the rdn of the old entry.

        String [] namingTypes = rdn.getAttIDs();                  // Read the list of RDN attribute types
        String [] namingRawValues = rdn.getRawVals();           // and values
        boolean nameChanged = false;
    
		DXEntry newEntry = parseData(data, oldEntry);	    	//TE: make an entry with any changes that the user may have done (this entry doesn't have a dn yet).

        // Must remove the Submit attr so that it doesn't get added...
        newEntry.remove(SUBMIT);

        log.finest("Entry after changes: " + newEntry);

		if (newEntry == null) return; // Error!
        
		try
		{ 
            String newName;                                     // temporary variable.
			for(int i=0;i<namingTypes.length;i++)				//TE: make the rdn (the rdn might be multivalued...).
			{
                // if we have an entry for one of the naming values...
                Attribute namingAtt = newEntry.get(namingTypes[i]);

                if(namingAtt != null)
                {
                    newName = (String)namingAtt.get();  // XXX - does this handle multi valued atts correctly?

                    if (newName!=null)       // no deleting of names in html templates!
                    {
                        // CB: bug 5054 - handle pesky white space.
                        // ... trim off any white space, unless it is all white space in which case leave one
                        // singleton space, and update the entry object with the newly trimmed name.
                        if (newName.trim().equalsIgnoreCase(newName) == false)
                        {
                            newName = newName.trim();
                            if (newName.length() == 0) newName = " ";
                            namingAtt.remove(0);
                            namingAtt.add(0, newName);
                        }

                        if (newName.equals(namingRawValues) == false)
                        {
                            nameChanged = true;
                            rdn.setRawVal(newName, i);
                        }
                    }
                }
			}
		} 
		catch (Exception e) // shouldn't happen; old entry should have already been validated by this stage.
        {
            log.log(Level.WARNING, "error parsing entry name in HTMLEditorKit ", e);
        } 
		
        if (nameChanged)
        {
    		DN newDN = new DN(dn); 					// get the dn of the old entry so the new rdn can be added to it.
	    	newDN.setRDN(rdn, newDN.size()-1);		// set the rdn in the old dn.
            newEntry.setDN(newDN);					// give the new entry the new DN....PHEW	   	   
        }
        else
        {
            newEntry.setDN(dn);                     // Name hasn't changed; reuse old one
        }

        Enumeration allOldAtts = oldEntry.getAll();
        
        // merge the old and the new, with the new getting any atts that appear in old
        // but not yet in new.  The idea here is to allow transparent passing through of
        // 'hidden' attributes, that are not affected by the form.
        
        while (allOldAtts.hasMoreElements())
        {
            Attribute att = (Attribute)allOldAtts.nextElement();
            if (att.size() != 0)
                if (newEntry.get(att.getID()) == null)
                {
                    newEntry.put(att);
                }    
        }

        // make the modification request!


        templateDisplay.currentDataSource.modifyEntry(oldEntry, newEntry);
    }

    /**
     *    Parses the form query data string, which is in standard html escaped
     *    format, and load the results into a new DXEntry.
     *
     *    @param data the html form to parse
     *    @param oldEntry the original entry, required for long string handling.
     *    @return the newly parse entry, suitable for sending to the directory.
     */    
     
    public DXEntry parseData(String data, DXEntry oldEntry)
    {    
        DXEntry newEntry = new DXEntry(); 
       
        HashSet forbiddenAttributes = null; // used to prevent very long strings breaking stuff.
       
        int start=0;
        int equalpos=0;
        int end=0;
        int length = data.length();

        try
        {
            while (start < length)
            {
                end = data.indexOf('&', start);
                if (end == -1) end = length;
                
                equalpos = data.indexOf('=', start);
    
                String attribute = data.substring(start, equalpos);

                //TODO - remove this deprecated method.  Use decode(str, enc) where enc is the page enc.
                //TODO - figure out how to get the page's advertised encoding.
                String value = URLDecoder.decode(data.substring(equalpos+1, end));//, "ISO-8859-1");
                Object val = value;

                String stringVal = null;

                // special handling for (XXX single valued) binary attributes
                if (templateDisplay.currentBinaryAttributes.contains(attribute.toLowerCase()))
                {
                	Object original = null;
                	String oldVal = null;
                	Attribute binAtt = templateDisplay.currentEntry.get(attribute);
                	if (binAtt != null && binAtt.size()>0)
                	{
            			original = binAtt.get();
            			oldVal = CBBase64.binaryToString((byte[])original);	
					}            			
					
            		if (value.equals(oldVal))
            		{
            			val = original;        // nothing has changed...
            		}	
            		else
            		{	
	                	// XXX Special handling for passwords: pass raw string through if password (only).
	                	if (attribute.toLowerCase().indexOf("password") > -1)
	                	{
                            try
                            {
	                		    val = value.getBytes("UTF-8");
                            }
                            catch (UnsupportedEncodingException e2)
                            {
                                CBUtility.error(templateDisplay, CBIntText.get("Unable to UTF-8 encode password"), e2);
                            }
	                	}
	                	else
	                	{ 
		            		val = CBBase64.stringToBinary(value);	// not password - back convert data.
		            	}
					}		            	
                }
                else if (attribute.toLowerCase().indexOf("address") > -1) 	// special handling for address fields...
                {
                	val = value.replace('\n','$');
                }
                
                if (val instanceof String)
                    stringVal = (String)val;
                    
                int dataLength = (stringVal != null)?stringVal.length():((byte[])val).length; // (*&^%*)( java array syntax.

                /* 
                 *  If the attribute was an illegally long string, replace it
                 *  now with the original oldEntry attribute (thus preventing it
                 *  from being modified in the directory, as there will be no
                 *  change.) - also add this attribute to the 'forbidden atts'
                 *  list.
                 */
                 
                if (stringVal != null && stringVal.equals(HTMLTemplateDisplay.ILLEGAL_VALUE))
                {
                    if (forbiddenAttributes == null)
                    {
                        forbiddenAttributes = new HashSet();
                    }    
                    Attribute att = oldEntry.get(attribute);
                    newEntry.put(att);
                    forbiddenAttributes.add(att);  // nb. it is possible to add the same att twice if it 
                                                   // has two long values.  We don't care.
                }
                else
                {
                    
                    if (newEntry.get(attribute) != null)  // append to existing attributes
                    {
                        if (dataLength != 0)          // don't add empty attribute values to existing attributes
                        {
                            Attribute att = newEntry.get(attribute);
                            
                            // check that this isn't an attribute containing a 
                            // very long string.
                            if (forbiddenAttributes == null || forbiddenAttributes.contains(att) == false)
                                att.add(val);       
                        }    
                    }    
                    else
                    {
                        // nb. no need to test forbidden Attributes here, as this is the first time
                        // we've met this particular attribute.
                        
                        if (dataLength != 0)
                            newEntry.put(new DXAttribute(attribute,val)); // create new attributes
                        else
                            newEntry.put(new DXAttribute(attribute));       // create new empty attribute
                    }
                }
                                    
                start = end + 1;    
            }
        }
        catch (Exception e)
        {
            CBUtility.error(templateDisplay, "Unable to submit form due\nto a problem with the query url.", new Exception("Parser error in MyHTMLEditorKit - badly formed query url: " + data + "\n  " + e.toString()));
            return null;  // exit
        }            

        return newEntry;  
    }    
}


public class MyHTMLEditorKit extends HTMLEditorKit
{
    private final ViewFactory newDefaultFactory;

    public MyHTMLEditorKit(HTMLTemplateDisplay display)
    {
        super();
        newDefaultFactory = new MyHTMLFactory(display);
    }

    public ViewFactory getViewFactory()
    {
        return newDefaultFactory; 
    }        

    public static class MyHTMLFactory extends HTMLEditorKit.HTMLFactory 
    {
        private final HTMLTemplateDisplay templateDisplay;
    
        public MyHTMLFactory(HTMLTemplateDisplay display)
        {
            super();
            templateDisplay = display;
        }
    
        /** overload the create method to serve up our own version of FormView
         *  and ImageView...
         */
        public View create(Element elem) 
        {
            View v = super.create(elem);
            if (v instanceof FormView)
                v = new MyFormView(elem, templateDisplay);        
            return v;
        }
    }
  
}