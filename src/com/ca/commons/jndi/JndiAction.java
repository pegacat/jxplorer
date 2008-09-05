/*
 * Created on Aug 9, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.ca.commons.jndi;

import java.security.PrivilegedAction;
import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 * @author vadim
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class JndiAction implements PrivilegedAction {
	
	private Hashtable env;
	
	public JndiAction(Hashtable env){
		this.env = env;
	}

	/* (non-Javadoc)
	 * @see java.security.PrivilegedAction#run()
	 */
	public Object run() {
		
		DirContext result = null;
		
		try{
			result = new InitialDirContext(env);
		}catch(NamingException ex){
			ex.printStackTrace();
		}
		
		return result;
	}

}
