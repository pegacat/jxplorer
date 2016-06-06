package com.ca.directory.jxplorer.broker;


import com.ca.commons.cbutil.*;
import com.ca.commons.jndi.*;
import com.ca.commons.naming.*;
import com.ca.commons.security.JXSSLSocketFactory;
import com.ca.directory.jxplorer.JXConfig;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *    A wrapper for BasicOps that overrides log() and error()
 *    in an application specific way.
 */
public class CBGraphicsOps extends DXOps
{
    public boolean quietMode = false;    // whether to supress gui error reporting
    public boolean errorWhileQuietFlag = false; // whether an error occured while quiet.
    public Exception quietException = null;   // what that quiet error was... (or the last one, if multiple errors occured)
    private CBpbar pbar = null;

    private static Logger log = Logger.getLogger(CBGraphicsOps.class.getName());
    
    /**
     *    Initialise with the directory context.
     */
     
    public CBGraphicsOps(LdapContext ctx)
        throws NamingException
    {
        super(ctx);
    }

    /**
     *    Initialise with a cData object.
     */

    public CBGraphicsOps(ConnectionData cData)
        throws NamingException
    {
        super(addSSLFactoryToConnectionData(cData));
    }

    /**
     *    Sets quiet error mode on or off, and clears 
     *    any stored 'quietException'. 
     */
     
    public void setQuietMode(boolean status)
    {
        quietMode = status;
        
        if (quietMode == true)
        {
            errorWhileQuietFlag = false;
            quietException = null;
        }    
    }
    
    /** 
     *  This override passes errors to CBUtility.error.
     *
     *  @param msg User friendly error message
     *  @param e The exception
     *  @return returns false (always) for easy chaining.
     */
     
    public boolean error(String msg, Exception e)
    {
        if (quietMode)
        {
            errorWhileQuietFlag = true;
            quietException = e;
            
            log.warning(msg+"\n  (details) " + ((e==null)?"no error details supplied":e.toString()));
        }    
        else
            CBUtility.error(msg, e);
            
        return false;
	}

   /**
    *	This override passes a confirmation message to CBError.confirm after a
	*	successful modify.
    *   @param dn  distinguished name of object to modify
    *   @param modList a list of ModificationItems
	*	.
	*/
	
    public void modifyAttributes(Name dn, ModificationItem[] modList)
        throws NamingException
    {
		super.modifyAttributes(dn, modList);
		
		if ("true".equalsIgnoreCase(JXConfig.getProperty("option.confirmTableEditorUpdates")))
        {
	        CBUtility.confirm(CBIntText.get("Entry: {0} was successfully updated", new String[] {dn.toString()}));	//TE: confirmation dialog.
        }
    }




    /**
     * overload this method for progress tracker.
     */

    public void startOperation(String heading, String operationName)
    {
        this.pbar = new CBpbar(CBUtility.getDefaultDisplay(), heading, operationName);
    }



    /**
     * overload this method for progress tracker.
     */

    public void stopOperation()
	{
        super.stopOperation();
        if (pbar ==null)
			return;

        pbar.close();
        this.pbar = null;
    }
	
	

    /**
     * overload this method for progress tracker.
     */

    public void pop() {
        if (pbar != null) pbar.pop();
    }

    /**
     * overload this method for progress tracker.  Note that elements
     * is passed to allow determination of the number of objects - but
     * the Enumeration must be returned without being reset, so be carefull
     * when using it...
     */

    public NamingEnumeration push(NamingEnumeration elements)
    {
        DXNamingEnumeration DXelements = new DXNamingEnumeration(elements);
        int size = DXelements.size();  // this *doesn't* use up the enumeration - it gets refreshed...
        if (pbar !=null) pbar.push(size);
        return DXelements;
    }

    /**
     * overload this method for progress tracker.  Note that elements
     * is passed to allow determination of the number of objects - but
     * the Enumeration must be returned without being reset, so be carefull
     * when using it...
     */

    public void push(ArrayList elements)
    {
        int size = elements.size();
        if (pbar !=null) pbar.push(size);
    }

    /**
     * overload this method for progress tracker.
     */

    public void inc() {
        if (pbar !=null) pbar.inc();
    }


    /**
    *   This sets the connctionData to use our advanced SSL Socket Factory, after initialising
    *  that socket factory.
    *
	*	@param connectionData a data object contain all the connection details.
	*/

	public static ConnectionData addSSLFactoryToConnectionData(ConnectionData connectionData)
        throws NamingException
    {
        //System.out.println("*** connection data *** ");
        //System.out.println(connectionData.toString());
        //System.out.println("*** end connection data *** ");

        if (connectionData.useSSL)
        {

// we could fall back to the old ssl socket factory for SASL, which must be set up right from the start, so there's
//            no point allowing on-the-fly addition of certificates...
//            if (connectionData.clientKeystorePwd != null)
//            {
//                connectionData.sslSocketFactory = "com.ca.commons.jndi.JndiSocketFactory";
//            }
//            else
//            {
                try
                {
                    JXSSLSocketFactory.init(connectionData.cacerts, connectionData.clientcerts,
                                            connectionData.caKeystorePwd, connectionData.clientKeystorePwd,
                                            connectionData.caKeystoreType, connectionData.clientKeystoreType, CBUtility.getDefaultDisplay());

                    JXSSLSocketFactory.setDebug(JXConfig.debugLevel >= 9 || JXConfig.debugSSL == true);

                    connectionData.sslSocketFactory = "com.ca.commons.security.JXSSLSocketFactory";
                }
                // this is a little hacky.  We have to throw NamingException to conform with the original
                // method signiture of the over-ridden method.
                catch (Exception e)
                {
                    NamingException ne = new NamingException("error pre-initialising SSL for JNDI connection: " + e.toString() + "\ncon: " + connectionData.toString());
                    ne.setRootCause(e);
                    throw ne;
                }
//            }
        }


        return connectionData;
    }

}