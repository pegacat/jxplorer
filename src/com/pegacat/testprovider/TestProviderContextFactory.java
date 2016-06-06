package com.pegacat.testprovider;

import javax.naming.spi.InitialContextFactory;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.logging.*;

/**
 * This code lovingly written by Chris.
 */
public class TestProviderContextFactory implements InitialContextFactory
{

    private static Logger log = Logger.getLogger(TestProviderContext.class.getName());

    static TestProviderContext defaultCtx = null;  // can be set to a specific object for testing...

    /*// Debug
    {
        log.setLevel(Level.FINE);
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.FINEST);
        log.addHandler(ch);
        log.info("com.pegacat.testprovider log setup");
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
        log.fine("CREATING NEW TEST CONTEXT...");

        // XXX not 100% sure what to do here... trying creating a new context every time,
        // XXX unless they have object identical environments... um, there's billions of
        // XXX ways this could go wrong, but worse case you just get a new test context
        // XXX created every time...
        if (defaultCtx == null)
            return new TestProviderContext(environment, new DataTree());
        else if (defaultCtx.getEnvironment() == environment)
            return defaultCtx; // identical contexts
        else
            return new TestProviderContext(environment, defaultCtx.getEntries());
    }

    /**
     * This sets the 'default' context.  In effect, it bootstraps the system with a pre initilised
     * context that has all your test data in it.  As long as all subsequently created contexts
     * are equal or subordinate to this, they will also inherit the test data...
     * @param ctx
     */
    public static void setProviderContext(TestProviderContext ctx)
    {
        defaultCtx = ctx;
    }
}
