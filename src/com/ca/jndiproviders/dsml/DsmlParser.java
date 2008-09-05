package com.ca.jndiproviders.dsml;

import com.ca.commons.naming.DN;

import javax.naming.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This code lovingly written by Chris.
 */
public class DsmlParser implements NameParser
{
    
    private static Logger log = Logger.getLogger(DsmlParser.class.getName());
//    private static Logger log = Logger.getLogger("com.ca.jndiproviders.dsml");

    /**
     * Default DSML parser for general use
     */
    public static final DsmlParser parser = new DsmlParser();

    // Debug
    {
        log.setLevel(Level.FINE);
    }

    /**
     * Parses a name into its components.  DSML names are
     * simply LDAP names, and this parser returns a com.ca.commons.naming.DN object.
     * It does not support
     *
     * @param name The non-null string name to parse.
     * @return A non-null parsed form of the name using the naming convention
     *         of this parser.
     * @throws javax.naming.InvalidNameException
     *                                      If name does not conform to
     *                                      syntax defined for the namespace.
     * @throws javax.naming.NamingException If a naming exception was encountered.
     */

    public Name parse(String name) throws NamingException
    {
        DN newDN = new DN(name);
        if (newDN.error())
            throw newDN.getNamingException();
        return newDN;
    }

    /**
     * Empty constructor.  Use if you don't want to use the static 'parser' object (e.g.
     * for thread safety reasons).
     */
    public DsmlParser()
    {
        log.log(Level.FINE, "creating DsmlParser");
    }

}
