package com.ca.directory.jxplorer.search;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.*;
import com.ca.directory.jxplorer.broker.DataBrokerQueryInterface;

/**
*	This class saves filters to and reads filters from the property file "search_filters.txt".
*	A user specified title is appended to "JXFilter." to form the name the filter.  For example 
*	"JXFilter.myFilter".  Filters can be stored in this file as raw filters, for example 
*	"JXFilter.myFilter=(sn=f*)", or filters can be stored as groups of filters, for example 
*	"JXFilter.myNewFilter=!&JXFilter.myFilterJXFilter.myOtherFilter".  In the last example you 
*	may notice the "!&" symbols at the beinging of the value.  These tell us what joins the filters
*	and if the filter has a 'not'.  Possible combinations are: '!&', '!|', '&', '|' or nothing if there
*	is only one filter.
*	<p>
*	This class also allows the saving of text filters (ones that the user types or pastes into the text
*	filter tab).  These differ from the prior two in that they may not follow the same syntax rules and
*	therefore may not be able to be loaded into the build and/or join tabs.  These types of filters are
*	saved with the 'JXTextFilter' prefix.
*/
public class SearchModel
{
	public static 	ReturnAttributesDisplay rat =null;	

	protected		Properties 	properties; 	
	protected		String		localDir;							//TE: the local directory where the property file is located.	
	protected		ArrayList  	dependantFilters = new ArrayList();
	protected		ArrayList  	tempList = new ArrayList();
    protected       String      searchFilterConfig;                 //CB: the name of the property file to save to.

	public static final	String 	SEARCH_FILTER_FILENAME = "search_filters.txt";	//TE: the property file.
	static final 	String 		NAME = "JXFilter.";					//TE: the filter name prefix e.g. 'JXFilter.name'.\
	static final 	String 		TEXTNAME = "JXTextFilter.";			//TE: the filter name prefix e.g. 'JXTextFilter.name'.


   /**
	*  	A flag used to indicate build filters for example '(cn=f*)'.
	*/
	public static final int BUILDFILTER = 1;
	
   /**
	*  	A flag used to indicate all filters, not including 'JX' prefix.
	*/
	public static final int ALLFILTERS = 2;
	
   /**
	*  	A flag used to indicate join filters for example 
	*	'JXFilter.myFilter=!|JXFilter.aFilter1JXFilter.aFilter2'.
	*/
	public static final int JOINFILTER = 3;
	
   /**
	*  	A flag used to indicate text filters for example 
	*	'JXTextFilter.myFilter=(cn=f*)'.
	*/
	public static final int TEXTFILTER = 4;
	
   /**
	*  	A flag used to indicate all filters, including 'JX' prefix.
	*/
	public static final int FULLNAMES = 5;

   /**
	*  	A flag used to indicate build and join filters only.
	*/
	public static final int BUILDJOIN = 6;
		
   /**
	*  	Name used to save the base DN in the property file.
	*/
	public static final String BASEDN = "baseDN";
	
   /**
	*  	Name used to save the return attribute list name in the property file.
	*/
	public static final String RETATTRS = "retAttrs";
	
   /**
	*  	Name used to save the search level in the property file.
	*/
	public static final String SEARCHLEVEL = "searchLevel";
	
   /**
	*  	Name used to save the state of the find alias check box in the property file.
	*/
	public static final String FIND = "find";
	
   /**
	*  	Name used to save the state of the find alias check box in the property file.
	*/
	public static final String SEARCH = "search";

    private static Logger log = Logger.getLogger(SearchModel.class.getName());


   /**
    *	Constructor that sets up the property file.
	*/
	public SearchModel()
	{
		properties = new Properties();

        searchFilterConfig = CBUtility.getPropertyConfigPath(JXplorer.APPLICATION_NAME, SEARCH_FILTER_FILENAME);

        if (new File(searchFilterConfig).exists()==false) { log.info("no search filter config file found at: " + searchFilterConfig); return;}

        properties = CBUtility.readPropertyFile(searchFilterConfig);

        if (properties.size()==0) { log.info("Initialising config file: " + searchFilterConfig); return;}
	}

   /**
    *	Opens the Return Attributes Dialog.
	*	@param jx JXplorer (main frame).
	*	@param attrNames the names of the return attributes.
	*	@param ds the DataBrokerQueryInterface being used.
	*/
	public void openRetAttrDisplay(JXplorerBrowser jx, String[] attrNames, DataBrokerQueryInterface ds)
	{
		rat = new ReturnAttributesDisplay(jx, attrNames);			
		rat.registerDataSource(ds);
	}

   /**
    *	Returns all of the filters in the property file "search_filters.txt".
	*	@return all of the filters in the property file "search_filters.txt".
	*/
	protected Enumeration getAllFilters()
	{
		properties = CBUtility.readPropertyFile(searchFilterConfig);
		return properties.propertyNames();
	}

   /**
    *	Returns an array list of the names from the property file (search_filters.txt) depending on the integer that is
	*	supplied as a parameter.  
	*	@param type one of JOINFILTER, ALLFILTERS, BUILDFILTER, TEXTFILTER, FULLNAMES.
	*	@return the list of filter names.
	*/
	public ArrayList<String> getFilterNames(int type)
	{	
		Enumeration list = getAllFilters();
		ArrayList loadList = new ArrayList();
		
		while (list.hasMoreElements())
		{
			String temp = list.nextElement().toString();
			
			switch (type)
			{
				case BUILDFILTER : {	if (properties.get(temp).toString().startsWith("(") && temp.startsWith(NAME)) loadList.add(temp.substring(temp.indexOf(NAME)+9));  break; }		//TE: get raw filter names.
				case ALLFILTERS  : {	if (temp.startsWith(NAME)) loadList.add(temp.substring(temp.indexOf(NAME)+9)); else if (temp.startsWith(TEXTNAME)) loadList.add(temp.substring(temp.indexOf(NAME)+14)); break; }	//TE: get all filter names, not including prefix.
				case JOINFILTER  : { 	if (!properties.get(temp).toString().startsWith("(") && temp.startsWith(NAME)) loadList.add(temp.substring(temp.indexOf(NAME)+9));  break; }	//TE: get joined filter names.
				case TEXTFILTER  : {  	if (temp.startsWith(TEXTNAME)) loadList.add(temp.substring(temp.indexOf(NAME)+14)); break; }
				case FULLNAMES   : {	if(temp.startsWith(NAME) || temp.startsWith(TEXTNAME)) loadList.add(temp); break; }		//TE: get all filter names, including prefix.
				case BUILDJOIN	 : {	if (temp.startsWith(NAME)) 	loadList.add(temp.substring(temp.indexOf(NAME)+9));  break; }		//TE: get all build and join filters.
				default: loadList.add("");
			}
		}
		return loadList;
	}

   /**
    *	Returns the value from the property file of a given key.  If the filter name prefix 'JXFilter.' isn't supplied with
	*	the name of the filter, it is inserted.
	*	@param name the key of the value that is being returned (can be either 'JXFilter.myFilter' or 'myFilter').
	*	@return the value of the key i.e. the filter.
	*/
	public String getFilter(String name)
	{
		if(name.startsWith("JXFilter."))
			return properties.getProperty(name);		//TE: filter name prefix has been supplied.
		else	
			return properties.getProperty(NAME+name);	//TE: inserts the filter name prefix 'JXFilter'.
	}

   /**
    *	Returns the value from the property file of a given text filter.
	*	@param name the key of the value that is being returned (e.g. 'myFilter').
	*	@return the value of the key i.e. the filter.
	*/
	   	public String getTextFilter(String name)
	{	
		return properties.getProperty(TEXTNAME+name);	//TE: inserts the filter name prefix 'JXTextFilter'.
	}	

   /**
    *	Returns the LDAP filter for a given filter name.  It expects the filter name prefix 'JXFilter.' or
	*	'JXTextFilter' is included in the name of the filter.
	*	@param name the key of the value that is being returned (can be either 'JXFilter.myFilter' or 'JXTextFilter').
	*	@return the LDAP filter.
	*/
	public String getLDAPFilter(String name)
	{	
//		if(name.startsWith("!"))
//			name = name.substring(1);
//		if(name.startsWith("&"))	
//			name = name.substring(1);
//		else if(name.startsWith("|"))
//			name = name.substring(1);	//TE: ???? why did I need this???
		
		ArrayList list = getFilterNames(BUILDFILTER);

		if (list.contains(name.substring(9)))
			return properties.getProperty(name);				//TE: is a raw filter.
		
		list.clear();
		
		list = getFilterNames(JOINFILTER);

		if (list.contains(name.substring(9)))
			return getJoinFilter(properties.getProperty(name));	//TE: is a join filter.		

		list.clear();
		
		list = getFilterNames(TEXTFILTER);
							
		return properties.getProperty(name);   					//TE: is a text filter.
	}	

   /**
    *	Counts the number of times a substring occurs within a string.
	*	@param string the string that is getting checked for the occurence of a substring.
	*	@param substring the substring that is being checked for.
	*	@return the number of times the substring occurs within the string.
	*/
	protected int getOccurences(String string, String substring)
	{
		int pos = -1;
		int count = 0;
		
		while ((pos = string.indexOf(substring, pos+1))!=-1)
			count++;
 
		return count;			
	}	
	
   /**
    *	Returns an array list of the names of the filters that make up the supplied filter
	*	for example, 'aFilter1' & 'aFilter2' from '!|JXFilter.aFilter1JXFilter.aFilter2'.
	*	@param filter the filter value that we want to extract the subfilter names from for example, 
	*	'!|JXFilter.aFilter1JXFilter.aFilter2'.
	*	@return the list of the subfilter names.
	*/
	protected ArrayList getJoinFilterNames(String filter)
	{
		ArrayList list = new ArrayList();
		String names;

		int num = getOccurences(filter, "JXFilter");
		
		for(int i=0; i<num; i++)
		{
			try
			{
				names = filter.substring(filter.indexOf(NAME)+9);	//TE: make a substring of the filter from after the first '.' to the end of the filter for example 'aFilter1JXFilter.aFilter2'.
				names = names.substring(0, names.indexOf(NAME));	//TE: make a substring of the name up to the first occurance of 'JXFilter.' for example 'aFilter1'.
			}			
				
			catch(Exception e)
			{
				names = filter.substring(filter.indexOf(NAME)+9);	//TE: XXXXXX arr... there must have been a reason for this??
			}
			filter = filter.substring(filter.indexOf(names)+ names.length());	//TE: make a substring of the filter from the end of the last name.

			list.add(names);		
		}
	
		return list;
	}	

   /**
    *	Returns the raw filter of a filter that is created using the Join method.
	*	For example (!(&(cn=f*)(sn=f*)(|(cn=f*)(sn=f*)))).
	*	@param filter the filter value for example: '&JXFilter.myFilter1JXFilter.myFilter2'.
	*	@return the raw filter as a string for example: (!(&(cn=f*)(sn=f*)(|(cn=f*)(sn=f*)))).
	*/
	protected String getJoinFilter(String filter)
	{
		StringBuffer buffy = new StringBuffer();
		
		getOperator(buffy, filter);

		return buffy.toString();
	}
	
   /**
    *	Takes a filter value for example '&JXFilter.myFilter1JXFilter.myFilter2' and appends the operator
	* 	to the string buffer.  In this example '&'.  Other possible values are '!&', '!|'. '|' or nothing.
	*	Removes these values from the filter string then appends the value or raw filter for example '(cn=f*)'
	*	of each subfilter to the buffer.
	*	<br>The only filters that should use this method are filters that are NOT raw i.e filters that are a 
	*	conbination of filters.<br>
	*	@param buffy the string buffer which is used to append the filter parts.
	*	@param filter e.g. '&JXFilter.myFilter1JXFilter.myFilter2'.
	*/
    public void getOperator(StringBuffer buffy, String filter)
    {
        if (filter==null) { log.warning("Unexpected error in processing a search filter: no filter supplied."); return;}
     
	 	int count = 0;   					//TE: keeps count of the number of ')' to be appended at the end.
		if (filter.startsWith("!"))
		{
			buffy.append("(!");				//TE: append '(!'.
			count++;
			filter = filter.substring(1);	//TE: remove ! from beginning of filter.
		}

		if (filter.startsWith("&"))
		{
			buffy.append("(&");				//TE: append '(&'.
			count++;
			filter = filter.substring(1);	//TE: remove & from beginning of filter.
		}
		else if (filter.startsWith("|"))
		{
			buffy.append("(|");				//TE: append '(|'.
			count++;
			filter = filter.substring(1);	//TE: remove | from beginning of filter.
		}		

		ArrayList list = getJoinFilterNames(filter);						//TE: get the filter names in this filter.

		String[] names = (String[])list.toArray(new String[list.size()]);	//TE: convert the list array to a string array.

        for (int i=0; i<names.length; i++)
        {	 
			String name = names[i];
			getValue(buffy, name);			//TE: get the filter value for each subfilter e.g, (cn=f*).
        }

		for(int i=0; i<count; i++)
			buffy.append(")");				//TE: append ')'.
    }

   /**
    *	If a filter value is raw e.g. (cn=f*) this method appends the value to the string buffer other wise
	*	it calls the getOperator() method again in a recursive manner until the raw filter is reached.
	*	@param buffy the string buffer which is used to append the filter parts.
	*	@param filter e.g. 'JXFilter.myFilter1'.
	*/
    protected void getValue(StringBuffer buffy, String filter)
    {     
		ArrayList list = getFilterNames(BUILDFILTER);
	  
        if (list.contains(filter))   //TE: if the filter is raw e.g. (cn=f*) append it to the buffer otherwise iterate the process.
        {
            buffy.append(getFilter(filter));
        }
		else 
		{			
			getOperator(buffy, getFilter(filter)); 			
		}				
    }	

   /**
    *	Returns true if the property file (search_filter.txt) contains the supplied
	*	filter.
	*	@param name the name of the filter for example, 'myfilter'.  The method provides the filter
	*	name prefix 'JXFilter.'.
	*	@return true if the property file contains the filter, false otherwise.
	*/
	protected boolean exists(String name)
	{
		if(properties.containsKey(NAME+name))	//TE: check if the filter (JXFilter.blah) name already exists, if so return true.
			return true;
		else if(properties.containsKey(TEXTNAME+name))	//TE: check if the filter (JXTextFilter.blah) name already exists, if so return true.
			return true;					
		
		return false;
	}

   /**
    *	Returns true if the given filter is a text filter by checking the property
	*	file for 'JXTextFilter+name'.  
	*	@param name the name of the filter without the 'JXTextFilter' prefix.
	*	@return true if the filter is a text filter, false otherwise.
	*/
	protected boolean isTextFilter(String name)
	{
		if(properties.containsKey(TEXTNAME+name))
			return true;
			
		return false;		
	}

   /**
    *	Saves a filter to the property file 'search_filter.txt'.  The name of the filter is added to the filter name prefix
	*	'JXFilter.' e.g. 'JXFilter.myFilter1'.  Saves either a raw filter e.g. 'JXFilter.myFilter1=(cn=f*)' or a combination of
	*	filters e.g.'JXFilter.myFilter1=&JXFilter.myFilter2JXFilter.myFilter3'
	*	@param name the name of the filter e.g. 'myFilter'.  The filter name prefix is added to this before saving.
	*		The saved name should look like: 'JXFilter.myFilter".
	*	@param filter the value that is being saved e.g. (cn=f*) or '&JXFilter.myFilter2JXFilter.myFilter3'.
	*/
	protected void saveFilter(String name, String filter)
	{			
		properties.setProperty(NAME+name, filter);
		CBUtility.writePropertyFile(searchFilterConfig, properties, "");
	}

   /**
    *	Saves a text filter to the property file 'search_filter.txt'.  The name of the filter is added to the filter name prefix
	*	'JXTextFilter.' e.g. 'JXFilter.myFilter1=(cn=f*)'.
	*	@param name the name of the filter e.g. 'myFilter'.  The filter name prefix is added to this before saving.
	*		The saved name should look like: 'JXTextFilter.myFilter".
	*	@param filter the value that is being saved e.g. (cn=f*).
	*/
	protected void saveTextFilter(String name, String filter)
	{
		properties.setProperty(TEXTNAME+name, filter);
		CBUtility.writePropertyFile(searchFilterConfig, properties, "");
	}

   /**
    *	Saves the given value to the property file with the name of the filter
	*	and the type of item as the key (e.g. name.baseDN=whatever).
	*	@param name the name of the filter (without the JXFilter prefix).
	*	@param type of value being saved (baseDN, retAttrs).
	*	@param value
	*/
	protected void saveValue(String name, String type, String value)
	{
		properties.setProperty(name+"."+type, value);
		CBUtility.writePropertyFile(searchFilterConfig, properties, "");
	}

   /**
    *	Saves the search level to the property file ('search_filters.txt') as name.searchLevel.
	*	@param name the name of the filter (without the JXFilter prefix).
	*	@param searchLevel the search level that the user wants saved with the filter.
	*/
	protected void saveSearchLevel(String name, int searchLevel)
	{
		properties.setProperty(name+"."+SEARCHLEVEL, Integer.toString(searchLevel));
		CBUtility.writePropertyFile(searchFilterConfig, properties, "");
	}
	
   /**
    *	Saves the search level to the property file ('search_filters.txt') as name.searchLevel.
	*	@param name the name of the filter (without the JXFilter prefix).
	*	@param aliasType the type of alias handling ('finding' etc.).
	*/
	protected void saveAlias(String name, String aliasType, boolean state)
	{
		if(state)
			properties.setProperty(name+"."+aliasType, "true");
		else
			properties.setProperty(name+"."+aliasType, "false");
				
		CBUtility.writePropertyFile(searchFilterConfig, properties, "");
	}	
	
   /**
    *	Returns the value associated with the key from the property file.
	*	This does a pure look up regardless of filter prefixes like JXFilter.
	*	@param key the key which we want the value returned (this is the actual
	*		key that we will use to look in the property file i.e. no modification
	*		is done to it.
	*	@return the value that the key stores (null if not present).
	*/
	public String getValue(String key)
	{
		if(properties.containsKey(key))
			return properties.getProperty(key);
		
		return null;	
	}

    /**
     *  inverse of 'saveValue()' - gets the value for a particular key and name
     */
    public String getValue(String name, String key)

    {
        return getValue(name + "." + key);
    }

   /**
    *	Checks if the property file contains the value.  If it does it gets the
	*	key for that value and checks if the key contains 'retAttrs'.  If it does
	*	the entry is removed from the property file.
	*	@param value the value that we are looking in the property file for in order to delete its entry.
	*/
	protected void removeRetAttrs(String value)
	{
		if(properties.containsValue(value))
		{
			Enumeration en = properties.propertyNames();
			while (en.hasMoreElements())
			{
				String temp = (en.nextElement()).toString();
				if (temp.indexOf(RETATTRS)>-1)		
				{
					if(((properties.get(temp)).toString()).equalsIgnoreCase(value))
						properties.remove(temp);
				}					
			}		

			CBUtility.writePropertyFile(searchFilterConfig, properties, "");
		}
	}

   /**	
    *	Removes an array of filters from the property file search_filters.txt.
	*	@param filterNames the array of filters names (keys) to be removed (deleted).
	*/
	protected void removeFilters(String[] filterNames)
	{
		for(int i=0;i<filterNames.length;i++)
		{
			removeFilter(filterNames[i]);			
		}	
	}

   /**	
    *	Removes a filter from the property file search_filters.txt.
	*	@param filterName the filter name (key) to be removed (deleted).  Expects the filter prefix.
	*/
	protected void removeFilter(String filterName)
	{
		properties.remove(filterName);

		String name = filterName.startsWith(NAME)? filterName.substring(9) : filterName.substring(13);

		Enumeration en = properties.propertyNames();
		while (en.hasMoreElements())
		{
			String temp = (en.nextElement()).toString();
			if(temp.startsWith(name))
				properties.remove(temp);
		}
		
		CBUtility.writePropertyFile(searchFilterConfig, properties, "");
	}

   /**
    *	Returns an Array List of filters that depend on the filter that the user is trying to delete,
	*	and any filters that depend on those filters and so on.
	*	@param filterName the name of the filter that the user wants to delete.
	*	@return the list of all dependand filters (filters that use this filter directly or indirectly).
	*/
	protected ArrayList getDependants(Object filterName)
	{
		dependantFilters.clear();							//TE: clear the global Array Lists.
		tempList.clear();
		
		if (!dependantFilters.contains(filterName))			//TE: make sure the filter being deleted is added to the list.
			dependantFilters.add(NAME+filterName);
		getDependantFilters(NAME+filterName);
		
		return dependantFilters;
	}
	
   /**
    *	This method tries to determine if there are any filters that use the filter that is supplied
	*	as a parameter.  The filters are constructed in a way that any one filter can be used by any
	*	other filters to make new filters.  Therefore deleting a filter can have a cascading effect of
	*	rendering other filters useless.  
	*	<p>
	*	This method searches all of the values from the property file 'search_filter.txt', to see if any
	*	contain the filter name.  If so the name of that filter (or the key) is saved in an Array List. 
	*	The process is repeated for each one of these saved keys.</p>
	*	<p>
	*	NOTE: Text filters don't have dependant filters.	
	*	@param filterName the name of the filter that is being checked for dependant filters.
	*/
	protected void getDependantFilters(Object filterName)
	{																	//TE: key=value.
		Collection col = properties.values();							//TE: get all the values from the property file.
		Object[] allValues = col.toArray();								//TE: make them usable.
		
		for(int i=0;i<allValues.length;i++)
		{
			if (allValues[i].toString().indexOf(filterName.toString())>-1)
			{
				String temp = getKeyForValue(allValues[i].toString());	//TE: get the key for the value.
				
				if (!temp.equalsIgnoreCase(""))
				{
					if (!dependantFilters.contains(temp))				//TE: add this to the list of filters that need to be deleted. && !dependantFilters.contains(temp))					//TE: add this to the list of filters that need to be deleted.
					{
						dependantFilters.add(temp);
						tempList.add(temp);
						getDependantFilters(temp); 						//TE: check this filter for any dependant filters.	
					}
				}							
			}
		}
	}
	
   /**
    *	Gets an enumeration of keys from the property list where the search filters are stored,
	*	then puts these keys into a string array.  It then extracts the value of each of these keys
	*	from the property file until it finds one that matches the supplied value.  In short, this
	*	method returns the key of the value supplied.
	*	@param value the value whose key we want returned.
	*	@return the key of the supplied value (returns and empty string if no key is found).
	*/
	protected String getKeyForValue(String value)
	{
		String[] allKeys = new String[properties.size()];
		Enumeration en = properties.propertyNames();
		int counter =0;
		
		while (en.hasMoreElements())
		{
			allKeys[counter]= (en.nextElement()).toString();	//TE: get all of the values from the enumeration and store them in the string array.
			counter++;
		}		
				
		for(int i=0;i<allKeys.length;i++)
		{
			if(properties.getProperty(allKeys[i]).equalsIgnoreCase(value) && !tempList.contains(allKeys[i]))	//TE: find which key has the supplied value and return it.	
				return allKeys[i];			
		}
			
		return "";			
	}

   /**
    *	This Comparator compares two case ignore Strings.
	*	@author Trudi.
    */
    public static class StringComparator implements Comparator
    {     
       /**
        *   This Comparator compares two case ignore Strings.
        *   @param o1 one of the two items to be compared.
        *   @param o2 the other of the items to be compared.
        *   @return the result of the compare (0 if the strings of o1 & o2 are equal, -1 if o1 < o2, 1 if o1 > o2).
		*
        */        
        public int compare(Object o1, Object o2) 
        {
            return ((String)o1).compareToIgnoreCase((String)o2);
        }
    }		
}