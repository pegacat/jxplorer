package com.ca.commons.naming;

import com.ca.commons.cbutil.CBParse;
import javax.naming.InvalidNameException;

/** 
 *    Some specialised String utilities for parsing ldap names.
 *
 */
 
public class NameUtility
{
    /** return position of next non-escaped character 'c' in a DN.
     * (i.e. character 'c' without a preceeding '\' character, or 
     *  not in double quotes) (unless the char is a quote, in which
     * case it isn't. um.).  Note that by definition '\' cannot exist, 
     * unescaped, as a first class character and therefore this function 
     * will not find '\'s.
     */

    public static int next(String searchMe, int startpos, char c)
    {
        if (c=='\\') return -1; // can't have an unescaped slash.
        
        if (startpos < 0 || startpos > searchMe.length()) return -1; // can't search outside string.
        
        int escape=-1, quotes=-1, nextC=-1;
        
        while (true)
        {
            if (escape<startpos) escape = searchMe.indexOf('\\', startpos);
            if (quotes<startpos) quotes = searchMe.indexOf('"', startpos);
            if (nextC<startpos)  nextC  = searchMe.indexOf(c, startpos);

            // check for trivial case - no escaped characters...
            if (escape==-1 && quotes==-1)  
                return nextC;
    
            // second trivial case - char occurs before any possible escaping
            // NOTE - if the search char is a quote, this will return the first quote
            //        (rather than treat it as an escape character).
            if (   ((escape == -1) || (nextC < escape)) && ((quotes == -1) || (nextC <= quotes)) )
                return nextC;
    
            // if a slash escape is the next thing, then move past it...
    
            if (quotes == -1 || (escape != -1 && escape<quotes))  
            {
                startpos = escape+2;    // flip past the next escape 
            }
            else // handle quoted text (above code implicitly assures us that the leading quote is unescaped)
            {
                boolean escaped = true;  // flag indicating that a particular quote character is 'escaped' (i.e. \")
            
                while (escaped)
                {
                    quotes = searchMe.indexOf('"', quotes+1);  // find next quote
                    if (quotes == -1) return -1;  // ERROR;
        
                    int backcheck = quotes-1;                    // make sure we only
                    while (searchMe.charAt(backcheck--) == '\\') // count unescaped quotes
                        escaped = !escaped;
                        
                    escaped = !escaped;                    
                }   
                
                startpos = quotes+1;
            }
        }            
    }


    /**
     *    A commutative twin to escape(), this removes the leading
     *    slashes from a string.
     */
    
    public static String unescape(String string)
        throws InvalidNameException
    {
        return unescape(string, false);
    }        

    /* 
     *    takes a string that is (possible) in ldap dn escaped utf8 format.
     *    (e.g. something like cn=\E5\B0\8F\E7\AC\A0 etc.)
     */
	 
    public static String removeEscapedUTF(String utfString)
        throws InvalidNameException
    {
        try
        {
            boolean foundUTF = false;
            int safeLen = utfString.length()-1;
            int pos = utfString.indexOf('\\');
            
            while ( pos > -1 && pos < safeLen)
            {  
                char c = utfString.charAt(pos+1);
                if ("01234567890ABCDEFabcdef".indexOf(c) > 0)
                {
                    foundUTF = true;
                    char c2 = utfString.charAt(pos+2); 
                    if ("01234567890ABCDEFabcdef".indexOf(c2) == -1)
                        throw new InvalidNameException("second char of escaped hex couplet wasn't hex; was '" + c2 + "'");
                        
                    char utf8 = (char)Integer.parseInt("" + c + c2, 16);
                    utfString = utfString.substring(0,pos) + utf8 + utfString.substring(pos+3); 
                    pos = utfString.indexOf('\\',pos+1);
                }
                else
                {
                    pos = utfString.indexOf('\\',pos+1); // skip normally escaped ldap character (e.g. \+ or \= )
                }
            }
            
            if (foundUTF)  // read the string as ascii (8859-1) bytes, and then interpret 
            {    		   // those bytes as utf8 to make a java unicode internal string...
                utfString = new String(utfString.getBytes("ISO-8859-1"), "UTF8");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new InvalidNameException("unable to parse rdn val: '" + utfString + "' - raw error was: " + e.toString());
        }
        
        return utfString;
    }


    /**
     *    Removes the escaping.  If jndiHack is false, this is roughly the
	 *    opposite of escape (however multiple valid formats resolve to the
	 *    same final unicode string).
     *    If jndiHack is true, it does special handling to cope with both
     *    version 2 ldap escapes, and the wonky return values for slashes
     *    given by jndi.<p>
     *     
     *    If used for DNs, The string argument should be the smallest
     *    possible unit of an rdn value.<p>
     *
     *    If the argument has beginning and end quotes, they are removed, leaving    
     *    text otherwise untouched (ldap v2 escaping).<br>
     *
     *    Otherwise, remove all leading '\' characters, giving special handling to
     *    escaped slashes.
     *    <p>
     *    <i>Note that in the second form this ftn is <b>not</b> commutative with escape</i>.
     *
     *    @param string the string to remove escape characters from
     *    @param jndiHack if true, indicates special handling for ldap v2 escaping, and
     *           wierd jndi return values.
     */

     
    public static String unescape(String string, boolean jndiHack)
        throws InvalidNameException
    {
        int len = string.length();
        if (len == 0) return string;
    
        if (string.charAt(0)=='\"')
        {
            if (string.charAt(string.length()-1)!='\"')  // whole string *must* be quoted
                throw new InvalidNameException("RDN.unescape(): invalid rdn fragment '" + ((string==null)?"<null>":string) + "'");
                
            string = string.substring(1,string.length()-1);
        }
        else
        {       
            string = handleEscapedCharacters(string);
        }
        return string;
    }


	/**
	 *   handle ldap escaped characters as per rfc 2253
	 *   In short - ',', '+', '=', '<', '>', '#', ';', '"' are escaped with
	 *   a backslash, and utf8 can be escaped as a hexpair backslash
	 *
	 */
	 
    private static String handleEscapedCharacters(String string)
		throws InvalidNameException
    {
		if (string.indexOf('\\') == -1)
			return string;

		boolean hasUTF8 = false; // whether a utf8 string has been found...
		int pos;  // position of most recently found slash
			
		StringBuffer buffy = new StringBuffer(string);			
			
	    try
	    {
	        pos = string.indexOf("\\");
	        while ( pos > -1)
	        {  
		        if (pos == buffy.length()-1) // XXX trailing escaped ' ' bug!
		        {
		            buffy.setCharAt(pos, ' '); //put the space back 
		        }
		        else
				{
					
			        char c = buffy.charAt(pos+1);

			        if (("\",=+<>#;\\ ".indexOf(c)) >= 0)   
			        {
						buffy.deleteCharAt(pos);   // remove leading slashes
				    }
					else if (("0123456789abcdefABCDEF").indexOf(c) >= 0)
					{
						hasUTF8 = true; // we'll cope with this seperately below...
						pos += 2; // skip past the two hex digits and keep going...					
					}
					else
					{
						throw new InvalidNameException("illegal escaped character '" + c + "' in name: '" + string + "' (NameUtility:handleEscapedCharacters() ).");
					}
				}					
				pos = buffy.toString().indexOf("\\",pos+1);  // try to find next slash to work on.
	        }
	    }
	    catch (StringIndexOutOfBoundsException e)
	    {
	        throw new InvalidNameException("unparsable string '" + string + "' in NameUtility");
	    }                
			
		if (hasUTF8)
			return removeEscapedUTF(buffy.toString());
		else			
    		return buffy.toString();
    }


    
    // there *SHOULD* only be two quotes: at the beginning and the end.  Trim 'em off
    // and hope (only here for backwards compatibility with RFC 1779 anyway!)
    /**
     *  Not currently used?
     */
    public static String trimQuotes(String string)
    {
        int pos = string.indexOf('\"');
        int pos2 = string.lastIndexOf('\"');
        if (pos == -1 || pos == pos2)
            System.out.println("RDN.trimQuotes(): rare error parsing rdn fragment:   " + string); // return string unchanged.
        else    
            string = string.substring(0,pos) + string.substring(pos+1,pos2) + string.substring(pos2+1);
        return string;
    }

    /*  (Obsolete - kept as backup.  Now using new CompositeName(..) followed
    *    by get(0), which returns something pretty close to ldap encoding).
     *
     *    Extra special handling for amazing jndi behaviour.
     *    The rule is - each slash was translated to three slashes,
     *    Unless behind a special character (including a slash), in
     *    which case it is worth four... This funtion translates them
     *    *back* to correct ldap format (one original slash = two slashes)
     *    So that the simple parser in unescape will work on them correctly.<p>
     *
     *    If jndi ever started working correctly, this will become redundant.
     */
    protected static String cleanupSlashes(String string)
    {
        int pos = 0;
        int next3group;
        int next4group;
        while (pos > -1)
        {
            next4group = string.indexOf("\\\\\\\\",pos);
            next3group = string.indexOf("\\\\\\",pos);
            
            if (next3group == -1)  //end of loop (3 group is subset of 4 group, so no more groups!)
            {
                pos = -1;
            }    
            else if ((next4group==-1) || (next3group<next4group))    // replace 3 group with a double slash (removed be standard parser later)
            {
                string = string.substring(0,next3group) + string.substring(next3group+1);
                pos = next3group + 2;                
            }
            else // translate the next four group.
            {
                string = string.substring(0,next4group) + string.substring(next4group+2);
                pos = next4group + 2;       
            }
        }
        return string;
    }
    
    /** 
     *    escapes special characters using a backslash, as per RFC 2253.
     *    IN ADDITION: escapes forward slash '/' characters for jndi
     *    @param string the string to convert to escaped form.
     */
     
    public static String escape(String string)
    {
		if (string == null || string.length() == 0)
				return string;
				
        StringBuffer buffy = new StringBuffer(string);

        buffy = CBParse.replaceAllBufferChar(buffy, '\\',"\\\\");
        buffy = CBParse.replaceAllBufferChar(buffy, ',',"\\,");
        buffy = CBParse.replaceAllBufferChar(buffy, '=',"\\=");
        buffy = CBParse.replaceAllBufferChar(buffy, '+',"\\+");
        buffy = CBParse.replaceAllBufferChar(buffy, '<',"\\<");
        buffy = CBParse.replaceAllBufferChar(buffy, '>',"\\>");
        buffy = CBParse.replaceAllBufferChar(buffy, '#',"\\#");
        buffy = CBParse.replaceAllBufferChar(buffy, ';',"\\;");
        buffy = CBParse.replaceAllBufferChar(buffy, '\"',"\\\"");
		
		if (buffy.charAt(buffy.length()-1) == ' ') // final space check
		{
			buffy.setCharAt(buffy.length()-1, '\\');
			buffy.append(' ');
		}	
  //      buffy = CBUtility.replaceAllBufferChar(buffy, '/',"\\/");
        string = buffy.toString();
        
        return buffy.toString();
    }

  
    /**
     *    Apparently jndi does not handle end spaces correctly.
     *    This checks for the condition that the DN is illegal,
     *    with a dangling slash on the end, representing a DN
     *    with an end space mashed by jndi. 
     *    @param ldapDNString the potentially illegal DN to rescue
     *    @return the corrected string (unchanged if no correcting required)
     */
     
    public static String checkEndSpaces(String ldapDNString)
    {
        // Check how many slashes there are on the end.
        // We are looking for an ending double slash
        // (a normal escape at this stage would look like
        // a double slash followed by an escape character,
        // e.g. "\\," or "\\+".  just "\\" is bad...);
        // The fuss below is to cope with a DN that ends with
        // escaped slashes, *followed* by a space (a pathalogical
        // case, but we must be carefull!)
    
        int finalPos = ldapDNString.length() - 1;
        int pos = finalPos;
        
        while (ldapDNString.charAt(pos) == '\\')  // remember '\\' is a *single* slash!
        {
            pos--;        
        }    

        int numSlashes = finalPos - pos;

        if (numSlashes%4 == 2)
        {
            return ldapDNString + " ";
        }    
            
        return ldapDNString;            
    }


} 