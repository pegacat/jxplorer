package com.ca.commons.security;

/**
 * JXSSLSocketFactory.java
 */

import java.io.*;
import java.net.*;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import java.security.*;
import java.awt.*;
import java.util.ArrayList;

import javax.net.ssl.*;    // jdk 1.4 includes ssl in standard distro


/**
 * <p>Socket factory for SSL jndi links that returns an SSL socket.
 * It incorporates a keystore, which must contain the certs used
 * to authenticate the client.</p>
 *
 * <p>This code is based on sample code made freely available by author
 * Spencer W. Thomas on his web site http://hubris.engin.umich.edu/java/
 * On Wed 24 May, 2000.</p>
 *
 * <p><b>Warning</b></p>
 *
 * <p>This class relies heavily on an internal, single, static SSLSocketFactory.
 * multiple objects of this type in fact will use the same internal SSLSocketFactory.
 * (This is why a single static init() method sets up everything for the entire
 * class.)  The reason for this structure is that JndiSocketFactory is dynamically
 * invoked by the jndi connection, and we have no other chance to initialise the
 * object.</p>
 */

public class JXSSLSocketFactory extends SSLSocketFactory
{

    /**
     * This is the (static) factory internally shared between
     * all JndiSocketFactory objects.
     */

    private static SSLSocketFactory factory = null;

    /**
     * A single default object of this class.  It is
     * initialised when first called for, and then reused
     * whenever called again.
     */

    private static JXSSLSocketFactory default_factory = null;


    private static KeyStore clientKeystore;

    /**
     * The sun 'JKS' keystore is the simplest and most commonly
     * available keystore type.
     */

    private static final String DEFAULT_KEYSTORE_TYPE = "JKS";

    /**
     *  the class loader to use for loading security providers
     *  'n stuff.  Defaults to the system loader.
     */

    private static ClassLoader myClassLoader = null;


    /**
     *    Register a custom class loader to be used by
     *    the class when getting security providers.
     */



    public static void setClassLoader(ClassLoader newLoader)
    {
        myClassLoader = newLoader;
    }

    /**
     *  checks that myClassLoader is initialised, uses the System
     *  default loader if it isn't, and returns the guaranteed
     *  initialised loader.
     */

    private static ClassLoader getClassLoader()
    {
        if (myClassLoader == null)
            myClassLoader = ClassLoader.getSystemClassLoader();

        return myClassLoader;
    }

    static boolean debug = false;

    /**
     *  <p>Enable debugging...</p>
     *  <P>WARNING - this doesn't seem to be working...??</p>
     */

    public static void setDebug(boolean status)
    {
        /*
        all         turn on all debugging
        ssl         turn on ssl debugging

        The following can be used with ssl:

            record      enable per-record tracing
            handshake   print each handshake message
            keygen      print key generation data
            session     print session activity

            handshake debugging can be widened with:
            data        hex dump of each handshake message
            verbose     verbose handshake message printing

            record debugging can be widened with:
            plaintext   hex dump of record plaintext
        */

        debug = true;

        if (status == true)
            System.setProperty("javax.net.debug", "ssl,handshake,verbose");
        else
        {
            System.setProperty("javax.net.debug", " ");
        }
    }

    /**
     *    <p>Initialize the socket factory with a particular key store(s) and
     *    security provider.  The minimum requirement is for a keystore
     *    containing trusted directory servers (the 'castore', or trusted
     *    certificate authority store, since the servers are usually signed
     *    by a common CA, whose cert would be held in this file).</p>
     *
     *    <p>Further options include a private key store (the 'clientstore')
     *    that allows for client-authenticated ssl and SASL).</p>
     *
     *    <p>Finally, it is possible to configure a non-standard keystore
     *    type and security provider.  The keystore type defaults to Sun's
     *    JKS (at time of writting, the only keystore type that the default
     *    Sun security provider will handle).</p>
     *
     *    <p>Nb. - it is possible to set a custom class loader (using
     *    'registerClassLoader()' ) in which case this loader can be used
     *    to load the security provider.</p>
     *
     *    @param caKeystoreFile      A keystore file name of public certificates (trusted CA signs)
     *    @param clientKeystoreFile  A keystore file name of the client's certificates, containing private keys.
     *                               (may be null if only simple, 'server authenticated' ssl is being used).
     *    @param caPassphrase        A password for the caKeystoreFile certificate.
     *                               (may be null if only simple, 'server authenticated' ssl is being used, and keystore type is 'JKS').
     *                               <b>Calling Program must manually clear passphrase after init() call.</b>
     *    @param clientPassphrase    A password for the clientKeystoreFile certificate.
     *                               (may be null if only simple, 'server authenticated' ssl is being used).
     *                               <b>Calling Program must manually clear passphrase after init() call.</b>
     *    @param caKeystoreType      The type of cakeystore file. (null => 'JKS')
     *    @param clientKeystoreType  The type of clientkeystore file. (null => 'JKS')
     *    @param owner               The owning GUI frame used for possible user interaction.
     */



    //
    //  Implementation note: this may be called repeatedly, with different info,
    //  as new connections are made.  This is unsatisfactory, and dangerous if it
    //  is being used from multiple threads, but is unavoidable due to the difficulty
    //  of setting jndi to use a specific SSL socket class (we have to pass it a
    //  factory).  Hence we need to be sure that different calls don't interfere,
    //  even at the cost of recreating objects unnecessarily...
    //
    public static void init(String caKeystoreFile, String clientKeystoreFile,
                            char[] caPassphrase, char[] clientPassphrase,
                            String caKeystoreType, String clientKeystoreType, Frame owner)
        //throws Exception
        throws GeneralSecurityException, IOException
    {
        boolean usingSASL = false;   // whether we are using client-authenticated SSL

        checkFileSanity(caKeystoreFile, clientKeystoreFile, clientPassphrase);


        if ((clientPassphrase!=null) && (clientPassphrase.length>0) && (clientKeystoreFile != null))
            usingSASL = true;

        // use the client store if there is no caKeystoreFile.
        if (caKeystoreFile == null && clientKeystoreFile != null)
            caKeystoreFile = clientKeystoreFile;

        // set cert authority keystore type to default if required.
        if (caKeystoreType == null)
            caKeystoreType = DEFAULT_KEYSTORE_TYPE;

        // set the ssl protocol - usually "TLS" unless over-ridden
        SSLContext sslctx = setSSLContextProtocol();

        /*
         *    The KeyManagerFactory manages the clients certificates *and* private keys,
         *    which allow the client to authenticate itself to others.  It is not required
         *    for 'simple' SSL, only for SASL.
         */

        KeyManagerFactory clientKeyManagerFactory = null;
        TrustManagerFactory caTrustManagerFactory = null;
        KeyStore caKeystore = null;
        KeyManager[] clientKeyManagers = null;


        /*
         * 'traditional' server-authenticated ssl only requires the use of a trusted server
         * keystore.  Stronger client-authenticated ssl that requires both parties to authenticate
         * (as used in LDAP SASL/External) requires a client keystore with a client private key.
         */
        if (usingSASL)
        {
            /*
             *    Create a keystore to hold the client certificates and private keys.
             */

            if (clientKeystoreType == null)
                clientKeystoreType = DEFAULT_KEYSTORE_TYPE;

            clientKeystore = KeyStore.getInstance(clientKeystoreType); // key manager key store

            /*
             *    Load the keystore from the client keystore file using the client
             *    keystore password.
             */

            if (clientKeystoreFile != null)
                clientKeystore.load(new FileInputStream(clientKeystoreFile), clientPassphrase);

            /*
             *    Create a key manager using the default sun X509 key manager
             */

            clientKeyManagerFactory = KeyManagerFactory.getInstance("SunX509");

            /*
             *    Initialise the client keystore manager with the just loaded keystore,
             *    and the keystore password.
             */

            clientKeyManagerFactory.init(clientKeystore, clientPassphrase);

            /*
             * Initialise the list of key managers
             */

            clientKeyManagers = clientKeyManagerFactory.getKeyManagers();
        }
        else
        {
            clientKeystore = null;
        }

        /*
         *   Initialise the trusted server certificate keystore.
         */


        caKeystore = KeyStore.getInstance(caKeystoreType);

        /*
         *    Load the keys from the 'certificate authority' keystore (the trusted server keystore) file.
         */

        if (caKeystoreFile != null)
        {
            // caPassword may be null for some keystores (e.g. a 'JKS' keystore), and it is not an error.
            caKeystore.load(new FileInputStream(caKeystoreFile), caPassphrase);
        }

        /**
         * Create a trust manager using the default algorithm
         * (can be set using 'ssl.TrustManagerFactory.algorithm=...' in java.security file - default is usually 'SunX509')
         * - code suggestion from Vadim Tarassov
         */
        String defaultTrustAlgorithm = TrustManagerFactory.getDefaultAlgorithm();

        caTrustManagerFactory = TrustManagerFactory.getInstance(defaultTrustAlgorithm);

        /*
         *    Create a trust manager factory using the default java X509 certificate based trust manager.
         */
//        caTrustManagerFactory = TrustManagerFactory.getInstance("SunX509");

        /*
         *    Initialise the trust manager with the keystore containing the trusted server certs.
         */

        caTrustManagerFactory.init(caKeystore);

        /*
         *    Get the list of trust managers from the trust manager factory, to initialise the
         *    ssl context with.
         */

         TrustManager[] caTrustManagers = caTrustManagerFactory.getTrustManagers();

        caTrustManagers = JXTrustManager.convert(caTrustManagers, caKeystore, caKeystoreFile, caPassphrase, caKeystoreType, owner);

        TrustManager[] trustedServerAndCAManagers = caTrustManagers;

        sslctx.init(clientKeyManagers, trustedServerAndCAManagers, null);

        factory = sslctx.getSocketFactory();

        // we need to set/reset the default factory to take account of the new initialisation data received
        // (this method may be called multiple times in the course of JXplorer's use.

        synchronized(JXSSLSocketFactory.class)
        {
            default_factory = new JXSSLSocketFactory();
        }
    }


    /**
     *  evil undocumented feature - can change SSL protocol on command line
     *   (needed for mainframe TOPSECRET folks who have want to use SSLv3).
     * ... normally it just returns "TLS".
     * @return
     * @throws NoSuchAlgorithmException
     */
    private static SSLContext setSSLContextProtocol() throws NoSuchAlgorithmException
    {
    	SSLContext sslctx;

    	String protocol = System.getProperty("sslversion", "TLS"); // default to TLS
        if (!"TLS".equals(protocol))
        {
            System.out.println("SECURITY WARNING: Using non-standard ssl version: '" + protocol + "'\n (P.S. You can set the exact ssl protocol used with options.ssl.protocol; see jxconfig.txt)");
        }
        sslctx = SSLContext.getInstance(protocol);
        return sslctx;
    }

    /**
     *    Checks that the files containing the keystores really exist.
     *    Throws an exception (that can be bubbled through to the gui)
     *    if they don't.  This is much clearer than relying on the
     *    Sun ssl stuff to meaningfully report back the error :-).
     *
     *    Also insist that we have at least one viable keystore to work with.
     */

    private static void checkFileSanity(String caKeystoreFile, String clientKeystoreFile, char[] clientPassphrase)
        throws SSLException
    {
        if (clientKeystoreFile == null && caKeystoreFile == null)
            throw new SSLException("SSL Initialisation error: No valid keystore files available.");

        if (caKeystoreFile != null)
            if (new File(caKeystoreFile).exists() == false)
                throw new SSLException("SSL Initialisation error: file '" + caKeystoreFile + "' does not exist.");

        if (clientKeystoreFile != null && clientPassphrase != null)
            if (new File(clientKeystoreFile).exists() == false)
                throw new SSLException("SSL Initialisation error: file '" + clientKeystoreFile + "' does not exist.");
    }


  // DEBUG PRINT CODE - don't remove, can be quite usefull...
/*
        KeyManager[] myKM = new KeyManager[keyManagers.length];
        for (int i=0; i<keyManagers.length; i++)
        {
            myKM[i] = new MyX509KeyManager((X509KeyManager)keyManagers[i]);
        }

        TrustManager[] myTM = new TrustManager[trustManagers.length];
        for (int i=0; i<trustManagers.length; i++)
        {
            myTM[i] = new MyX509TrustManager((X509TrustManager)trustManagers[i]);
        }

        System.out.println("Number of Keymanagers = " + myKM.length);
        if (myKM.length >=1)
        {
            KeyManager bloop = myKM[0];
            if (bloop == null) System.out.println("Bloop is Null???!");
            System.out.println("bloop is a " + bloop.getClass());
            if (bloop instanceof X509KeyManager)
            {
                System.out.println("bloop is X509KeyManager!");
                String[] clients = ((X509KeyManager)bloop).getClientAliases("SunX509", null);
                System.out.println("Num clients = " + clients.length);
                for (int i=0; i<clients.length; i++)
                    System.out.println("client: " + i + " = " + clients[i]);
            }
        }


        System.out.println("Number of Trustmanagers = " + myTM.length);
        if (myTM.length >=1)
        {
            TrustManager bloop = myTM[0];
            if (bloop == null) System.out.println("Bloop is Null???!");
            System.out.println("bloop is a " + bloop.getClass());
            if (bloop instanceof X509TrustManager)
            {
                System.out.println("bloop is X509TrustManager!");
               ((X509TrustManager)bloop).getAcceptedIssuers();
            }
        }
*/

    /**
     * Constructor
     */
    public JXSSLSocketFactory()
    {
    }

    /**
     * <p>Return an instance of this class.</p>
     *
     * <p>Each call to 'init()' should reset the default factory.</p>
     *
     *
     * @return		An instance of JndiSocketFactory.
     */

    public static SocketFactory getDefault()
    {
        synchronized(JXSSLSocketFactory.class)
        {
            if (default_factory == null)
	            default_factory = new JXSSLSocketFactory();
        }

        return default_factory;
    }


	public static KeyStore getClientKeyStore() {
		return clientKeystore;
	}

    /**
     * This forces the socket to use only a specific protocol (e.g. 'TLSv1'), if available.
     *
     * By default, a range of protocols are enabled (SSLv3, TLSv1 etc.).  This
     * method checks if a particular protocol (passed as a variable via System property 'option.ssl.protocol')
     * is available, and then sets it to be the only
     * enabled protocol
     *
     * @param newSocket
     */
	private static void optionallySetSSLSocketProtocol(SSLSocket newSocket)
	{
        if (debug)
        {
            String[] enabledProtocols = newSocket.getEnabledProtocols();
            for (int i = 0; i < enabledProtocols.length; i++)
            {
                System.out.println("Available SSL Protocol: " + i + " = " + enabledProtocols[i]);
            }
        }

        String protocol = System.getProperty("option.ssl.protocol");  // may be 'TLS', 'TLSv1', 'TLSv1,TLSv1.1', 'any' etc.
        if (protocol.equalsIgnoreCase("any"))
             return;  // nothing to do

        // reads supported protocols; e.g. one of {SSLv2Hello(2), SSLv3, TLSv1, TLSv1.1, TLSv1.2}
        String[] availableProtocols = newSocket.getSupportedProtocols();
        ArrayList<String> protocolsToSet = new ArrayList<String>();

        for (int i=0; i<availableProtocols.length; i++)  //iterates through available protocols, e.g. 'SSLv2Hello(1)', 'SSLv3' etc.
        {
            String anAvailableProtocol = availableProtocols[i];
            if (protocol.contains(anAvailableProtocol) || anAvailableProtocol.startsWith(protocol))  // do exact match, or begins with
            {
                protocolsToSet.add(anAvailableProtocol);
                if (debug) System.out.println("SSL: enabling protocol '" + anAvailableProtocol + "'");
            }
        }

        if (protocolsToSet.size()==0)
        {
            System.out.println("WARNING: Unable to set SSL to use '" + protocol + "'.  Available Protocols are:");
            for (int i=0; i<availableProtocols.length; i++)
                System.out.println(availableProtocols[i]);

        }
        else
        {
            String[] protocols = protocolsToSet.toArray(new String[]{});

            newSocket.setEnabledProtocols(protocols);
        }
	}


    /**
     * Return an SSLSocket (upcast to Socket) given host and port.
     *
     * @param host	Name of the host to which the socket will be opened.
     * @param port	Port to connect to.
     * @return		An SSLSocket instance (as a Socket).
     * @throws	IOException	If the connection can't be established.
     * @throws	UnknownHostException	If the host is not known.
     */
    public Socket createSocket(String host, int port)
        throws IOException, UnknownHostException
    {
    	final SSLSocket newSocket = (SSLSocket)factory.createSocket(host, port);
    	optionallySetSSLSocketProtocol(newSocket);
    	return newSocket;
    }

    /**
     * Return an SSLSocket (upcast to Socket) given host and port.
     *
     * @param host	Address of the server host.
     * @param port	Port to connect to.
     * @return		An SSLSocket instance (as a Socket).
     * @throws	IOException	If the connection can't be established.
     * @throws	UnknownHostException	If the host is not known.
     */
    public Socket createSocket(InetAddress host, int port)
       throws IOException, UnknownHostException
    {
    	final SSLSocket newSocket = (SSLSocket)factory.createSocket(host, port);
    	optionallySetSSLSocketProtocol(newSocket);
    	return newSocket;
    }

    /**
     * Return an SSLSocket (upcast to Socket) given host and port.
     * The client is bound to the specified network address and port.
     *
     * @param host	Address of the server host.
     * @param port	Port to connect to.
     * @param client_host	Address of this (client) host.
     * @param port	Port to connect from.
     * @return		An SSLSocket instance (as a Socket).
     * @throws	IOException	If the connection can't be established.
     * @throws	UnknownHostException	If the host is not known.
     */
    public Socket createSocket(InetAddress host, int port,
			     InetAddress client_host, int client_port)
       throws IOException, UnknownHostException
    {
    	final SSLSocket newSocket = (SSLSocket)factory.createSocket(host, port, client_host, client_port);
    	optionallySetSSLSocketProtocol(newSocket);
    	return newSocket;
    }


    /**
     * Return an SSLSocket (upcast to Socket) given host and port.
     * The client is bound to the specified network address and port.
     *
     * @param host	Address of the server host.
     * @param port	Port to connect to.
     * @param client_host	Address of this (client) host.
     * @param port	Port to connect from.
     * @return		An SSLSocket instance (as a Socket).
     * @throws	IOException	If the connection can't be established.
     * @throws	UnknownHostException	If the host is not known.
     */
    public Socket createSocket(String host, int port,
			     InetAddress client_host, int client_port)
       throws IOException, UnknownHostException
    {
    	final SSLSocket newSocket = (SSLSocket)factory.createSocket(host, port, client_host, client_port);
    	optionallySetSSLSocketProtocol(newSocket);
    	return newSocket;
    }

    /**
     * Return an SSLSocket layered on top of the given Socket.
     */
    public Socket createSocket(Socket socket, String host, int port, boolean autoclose)
       throws IOException, UnknownHostException
    {
    	final SSLSocket newSocket = (SSLSocket)factory.createSocket(socket, host, port, autoclose);
    	optionallySetSSLSocketProtocol(newSocket);
    	return newSocket;
    }

    /**
     * Return default cipher suites.
     */
    public String[] getDefaultCipherSuites()
    {
	    return factory.getDefaultCipherSuites();
    }

    /**
     * Return supported cipher suites.
     */
    public String[] getSupportedCipherSuites()
    {
    	return factory.getSupportedCipherSuites();
    }
}
