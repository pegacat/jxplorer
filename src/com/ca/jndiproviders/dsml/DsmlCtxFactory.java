package com.ca.jndiproviders.dsml;

import javax.naming.spi.InitialContextFactory;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.logging.*;

/**
 * This code lovingly written by Chris.
 */
public class DsmlCtxFactory   implements InitialContextFactory
{

    private static Logger log = Logger.getLogger(DsmlContext.class.getName());
//    private static Logger log = Logger.getLogger("com.ca.jndiproviders.dsml");

    /*// Debug
    {
        log.setLevel(Level.FINE);
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.FINEST);
        log.addHandler(ch);
        log.info("com.ca.jndiproviders.dsml log setup");
    }
    */
    
    /**
     * Creates an Initial Context for beginning name resolution.
     * Special requirements of this context are supplied
     * using <code>environment</code>.
     * <p/>
     * The environment parameter is owned by the caller.
     * The implementation will not modify the object or keep a reference
     * to it, although it may keep a reference to a clone or copy.
     *
     * @param environment The possibly null environment
     *                    specifying information to be used in the creation
     *                    of the initial context.
     * @return A non-null initial context object that implements the Context
     *         interface.
     * @throws javax.naming.NamingException If cannot create an initial context.
     */
    public Context getInitialContext(Hashtable environment) throws NamingException
    {
        log.fine("CREATING NEW CA DSML CONTEXT...");

        // sort out any pre creation setup (e.g. setting up SOAP connection etc?)

        DsmlContext newContext = new DsmlContext((Hashtable)environment.clone());

        log.fine( "...CREATED NEW CA DSML CONTEXT");

        return newContext;
    }
}
