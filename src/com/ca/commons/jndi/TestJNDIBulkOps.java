package com.ca.commons.jndi;


import java.util.*;
import java.util.logging.*;

import javax.naming.NamingException;

import com.ca.commons.cbutil.*;
import com.ca.commons.naming.DXEntry;



/**
*	Opens and reads the property file @see FILENAME.  Creates a BulkJndiTest
*	object and passes args[] and the property file to it.
*	Determines the options supplied by the user and sets these as global variables.
*	Reads the property file (BulkJndiTest.txt) to determine:
*	<p>
*			The computer name.
*			The port number.
*			The number of Threads to be used.
*			The number of times each Thread should run the jnditests on the LDIF data.
*			The names of the LDIF files to be used by each Thread.
*	</p>
*	Creates a new Test object for each Thread then starts the Thread.
*	@author Trudi.
*/

public class TestJNDIBulkOps
{
   /**
	*	The name of the property file that lists the LDIF files and which Threads they pertain to.
	*/
	
	final static String FILENAME = "BulkJndiTest.txt";
		
    private final static Logger log = Logger.getLogger(TestJNDIBulkOps.class.getName());

		
   /**
    *	Opens and reads the property file @see FILENAME.  Creates a BulkJndiTest
	*	object and passes args[] and the property file to it.
	*	@param args any options supplied by the user.  Can be
	*	<p>
	*       -d            debug (verbose) mode.										<br>
	*       -e            exit on error, returning -1.								<br>
	*       -h [subject]  this help message [full|changetype].						<br>
	*       -p password   an option password.										<br>
	*       -r referral   the jndi referral type [follow|ignore|throw].				<br>
	*       -t            set BER tracing on.										<br>
	*       -u userdn     an optional user dn.										<br>
	*       -v            set ldap version (default 3).								<br>
	*       -x            print stack trace. 										<br>	
	*	</p>
	*/
			
    public static void main (String args[]) 
    {
        log.addHandler(new ConsoleHandler());
        log.setLevel(Level.FINE);

		Properties properties = new Properties();

        properties = CBUtility.readPropertyFile(FILENAME);
        if (properties.size()==0) { log.warning("Can't find: " + FILENAME); return;}
	    	
		new TestJNDIBulkOps(args, properties);
    }
	
	
	
   /**
    *	Determines the options supplied by the user and sets these as global variables.
	*	Reads the property file (BulkJndiTest.txt) to determine:
	*	<p>
	*			The number of Threads to be used.
	*			The number of times each Thread should run the jnditests on the LDIF data.
	*			The names of the LDIF files to be used by each Thread.
	*	</p>
	*	Creates a new Test object for each Thread then starts the Thread.
	*	@param args any options supplied by the user.  Can be
	*	<p>
	*       -d            debug (verbose) mode.										<br>
	*       -e            exit on error, returning -1.								<br>
	*       -h [subject]  this help message [full|changetype].						<br>
	*       -p password   an option password.										<br>
	*       -r referral   the jndi referral type [follow|ignore|throw].				<br>
	*       -t            set BER tracing on.										<br>
	*       -u userdn     an optional user dn.										<br>
	*       -v            set ldap version (default 3).								<br>
	*       -x            print stack trace. 										<br>	
	*	</p>	
	*	@param properties the property file (BulkJndiTest.txt).
	*/
		
	public TestJNDIBulkOps(String args[], Properties properties)
	{
	    String url = null;
	    String user = null;
	    String pwd  = null;
	    String version = "3";
	    String referral = "follow";
	    boolean useSSL = false;
	    boolean tracing = false;
	    boolean debugFlag = false;
	    boolean terminateFlag = false;	
		boolean printstackFlag = false;
		
	    try
	    {	 
			int i=0;
			
	        while (i<args.length)
	        {
	            String arg = (args[i].charAt(0) != '-')?args[i]:args[i].substring(1);
	            switch(arg.charAt(0))
	            {
	                case '?':
	                case 'H':
	                case 'h': if (args.length>i+1) 
	                                System.out.println(args[i+1]);
	                          else
	                                return;                           
	                case 'D':
	                case 'd': debugFlag = true; break;	                
	                case 'E':
	                case 'e': terminateFlag = true; break;	                
	                case 'P':
	                case 'p': pwd = args[++i]; break;	                
	                case 'R':
	                case 'r': referral = args[++i]; break;	                
	                case 'S':
	                case 's': useSSL = true; break;	                
	                case 'T':
	                case 't': tracing = true; break;	                
	                case 'U':
	                case 'u': user = args[++i]; break;	                
	                case 'V':
	                case 'v': version = args[++i]; break;
	            	case 'X':
	            	case 'x': printstackFlag = true; break;
	                
	                default : System.out.println("\n\nInvalid command line argument: -" + arg);
	                                return; 
	            }
	            i++;               
	        }
	    }
	    catch (Exception e)
	    {
	        System.out.println("Error reading command line arguments.");
	        System.exit(-1);
	    }

		
		int numberOfThreads = -1;	//TE: the number of threads that the user wants to use.
		int numberOfIterations = -1;	//TE: the number of times the ldif data is tested per thread.
		
		try
		{	
			//TE: find out how many threads the user wants to use...
			numberOfThreads = Integer.parseInt(properties.getProperty("NumberOfThreads").toString());
			
			//TE: find out how many iterations per thread the user wants...
			numberOfIterations = Integer.parseInt(properties.getProperty("NumberOfIterations").toString());			
			
			//TE: sanity check...
			if(numberOfThreads <=0 || numberOfThreads > 20)
			{
				System.out.println("Problem accessing the number of threads you wish to run." +
								   "  Check the property file 'BulkJndiTest.txt' and make sure the" +
								   " 'NumberOfThreads' entry contains a valid integer between 1 and 10.");
				System.exit(-1);
			}			
		}
		catch(NumberFormatException  e)
		{
			System.out.println("Problem accessing the number of threads you wish to run." +
							   "  Check the property file 'BulkJndiTest.txt' and make sure the" +
							   "  'NumberOfThreads' entry contains a valid integer between 1 and 10." +
							   "  Also check that the 'NumberOfIterations' contains a valid integer.");
			System.exit(-1);
		}
		
		url = properties.getProperty("ComputerName").toString()+":"+properties.getProperty("Port").toString();		
		
		if (url==null)
		{
			System.out.println("Problem accessing the computer name from the property file 'BulkJndiTest.txt'."+
								"Please check that a valid name is supplied in the 'ComputerName' entry.");
		 	System.exit(-1);
		}		

		Thread[] threads = new Thread[] {new Thread(new Test(properties.getProperty("ThreadOneLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread ONE"),
										 new Thread(new Test(properties.getProperty("ThreadTwoLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread TWO"),	
										 new Thread(new Test(properties.getProperty("ThreadThreeLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread THREE"),
										 new Thread(new Test(properties.getProperty("ThreadFourLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread FOUR"),
										 new Thread(new Test(properties.getProperty("ThreadFiveLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread FIVE"),
										 new Thread(new Test(properties.getProperty("ThreadSixLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread SIX"), 
										 new Thread(new Test(properties.getProperty("ThreadSevenLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread SEVEN"), 
										 new Thread(new Test(properties.getProperty("ThreadEightLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread EIGHT"),
										 new Thread(new Test(properties.getProperty("ThreadNineLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread NINE"),
										 new Thread(new Test(properties.getProperty("ThreadTenLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread TEN"),
										 new Thread(new Test(properties.getProperty("ThreadElevenLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread ELEVEN"),
										 new Thread(new Test(properties.getProperty("ThreadTwelveLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread TWELVE"),	
										 new Thread(new Test(properties.getProperty("ThreadThreeteenLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread THIRTEEN"),
										 new Thread(new Test(properties.getProperty("ThreadFourteenLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread FOURTEEN"),
										 new Thread(new Test(properties.getProperty("ThreadFifteenLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread FIFTTEEN"),
										 new Thread(new Test(properties.getProperty("ThreadSixteenLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread SIXTEEN"), 
										 new Thread(new Test(properties.getProperty("ThreadSeventeenLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread SEVENTEEN"), 
										 new Thread(new Test(properties.getProperty("ThreadEighteenLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread EIGHTEEN"),
										 new Thread(new Test(properties.getProperty("ThreadNineteenLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread NINETEEN"),
										 new Thread(new Test(properties.getProperty("ThreadTwentyLDIFFileName").toString(), 	url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag, numberOfIterations), "Test Thread TWENTY")};		

		for(int i=0; i<numberOfThreads; i++)
		{
			try
			{
				threads[i].start();
				System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n"+
						  		 "Starting Thread: " + threads[i].getName() +
						  		 "\n^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");			
			}
			catch(IllegalThreadStateException  e)
			{
				System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n"+
						  		   "Error Starting Thread: " + threads[i].getName() +
						  		   "\n^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n" +e);
			}			
		}				
    }
	


   /**
    *	Class basically kicks off the jnditests, but instead of only running
	*	through an LDIF file once, this allows the user to specify (via BulkJndiTest)
	*	how many iterations to do.
	*	@author Trudi.
	*/
		
	class Test implements Runnable 
	{		
	    String 		fileName = null;
	    String 		url = null;
	    String 		user = null;
	    String 		pwd  = null;
	    String 		version = "3";
	    String 		referral = "follow";
	    boolean 	useSSL = false;
	    boolean 	tracing = false;
	    boolean 	debugFlag = false;
	    boolean 	terminateFlag = false;	
		boolean		printstackFlag = false;
		int 		numberOfIterations = -1; 	
			
	   /**
	    *	Constructor that assigns the following global variables.
	    *   @param fileName        		the name of an LDIF changes input file.
	    *   @param url             		a url of the form ldap://hostname:portnumber.
	    *   @param user            		a user to bind to the directory as.
	    *   @param pwd             		the user's password. 
	    *   @param tracing         		whether to set BER tracing on or not.
	    *   @param version         		the LDAP Version (2 or 3) being used.
		*	@param debugFlag			echo all system statement.
		*	@param terminateFlag		exit on error, returning -1.
		*	@param referral				the jndi referral type [follow|ignore|throw].
		*	@param useSSL				to use SSL.
		*	@param printstackFlag		whether to print a stack trace.
		*	@param numberOfIterations	the number of times the user wants the tests to run through the LDIF file.				
		*/
				
		public Test(String fileName, String url, String user, String pwd, boolean tracing, String version, boolean debugFlag, boolean terminateFlag, String referral, boolean useSSL, boolean printstackFlag, int numberOfIterations)
		{
		    this.fileName = fileName;
		    this.url = url;
		    this.user = user;
		    this.pwd  = pwd;
		    this.version = version;
		    this.referral = referral;
		    this.useSSL = useSSL;
		    this.tracing = tracing;
		    this.debugFlag = debugFlag;
		    this.terminateFlag = terminateFlag;
			this.printstackFlag = printstackFlag;
			this.numberOfIterations	= numberOfIterations; 	
		}
		
		

	   /**
	    *	Creates a new jnditest object for each time the user wants the jnditests to 
		*	test an LDIF file.
		*/
				
		public void run()
		{
			for(int i=1;i<=numberOfIterations;i++)
			{	
				myJndiTest tester = new myJndiTest(fileName, url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag);										
	        	tester.processInput();
	        
		        tester.out.println("\n*****************************************************************************************" +
								   "\n* FINISHED TESTING OF THREAD: '"+Thread.currentThread().toString()+ "' ITERATION: "+i+ 
								   "\n*****************************************************************************************");	
			}
		}
		
	}		
	
	
	
   /**
    *	Class extend jnditest to allow for custom url to be used on connection i.e so that 
	*	instead of grabbing the url from the LDIF file this will set it from the url supplied
	*	in the constructor.
	*	@author Trudi.
	*/
		
	class myJndiTest extends TestJNDIOps
	{
		String myUrl;
		
		public myJndiTest(String fileName, String url, String user, String pwd, boolean tracing, String version, boolean debugFlag, boolean terminateFlag, String referral, boolean useSSL, boolean printstackFlag)
		{
			super(fileName, url, user, pwd, tracing, version, debugFlag, terminateFlag, referral, useSSL, printstackFlag);
			this.myUrl = url;
		}
		
    
	   /**
	    *   Opens a connection in the same manner as jnditest.connect but uses a url supplied by
		*	the constructor.
	    *   @param entry a 'fake' entry with no dn, but a bunch of attributes.
	    */
	    
	    public void connect(DXEntry entry)
              // throws NamingException
	    {
	        if (debug) System.out.println("connect: " + entry);
	        if (myOps != null)
	        {
	        	try
	        	{
					myOps.close();
				}
				catch (NamingException e) 
				{
					System.err.println("exception closing ops");
					e.printStackTrace();
				}
	        } 
	      
		  	String url = "";
			
		  	if(myUrl!=null)
	        	url = myUrl;
			else
				url = entry.getString("url");	
	        String user = entry.getString("user");
	        String pwd = entry.getString("pwd");
	        String tracing = entry.getString("tracing");
	        String version = entry.getString("ldapVersion");
	        String referral = entry.getString("referral");
	        String useSSL = entry.getString("useSSL");
	     
	        boolean trace = ((tracing != null) && (tracing.equalsIgnoreCase("true")));
	        boolean ssl = ((useSSL != null) && (useSSL.equalsIgnoreCase("true")));
	        openConnection(url, user, pwd, trace, version, referral, ssl);
	    }						
	}
}