package com.pegacat.testprovider;

import javax.naming.ldap.*;

import javax.naming.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This code lovingly written by Chris.
 */
public class TestProviderParser implements NameParser
{
    
    private static Logger log = Logger.getLogger(TestProviderParser.class.getName());
//    private static Logger log = Logger.getLogger("com.pegacat.testprovider");

    /**
     * Default DSML parser for general use
     */
    public static final TestProviderParser parser = new TestProviderParser();

    // Debug
    {
        log.setLevel(Level.FINE);
    }

    /**
     * Parses a name into its components.  DSML names are
     * simply LDAP names, and this parser returns a javax.naming.ldap.LdapName object.
     *
     * @param name The non-null string name to parse.
     * @return A jndi name object (LdapName)
     * @throws javax.naming.InvalidNameException
     *                                      If name does not conform to the LDAP syntax
     * @throws javax.naming.NamingException If a naming exception was encountered.
     */

    public Name parse(String name) throws NamingException
    {
        return new LdapName(name);
    }

    /**
     * Empty constructor.  Use if you don't want to use the static 'parser' object (e.g.
     * for thread safety reasons).
     */
    public TestProviderParser()
    {
        log.log(Level.FINE, "creating DsmlParser");
    }

}
