package com.ca.commons.security;

import com.ca.commons.cbutil.CBIntText;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.awt.*;

public class JXTrustManager
        implements X509TrustManager
{
    public static String CERTSTORE = "lbecacerts";
    public static String CERTSTORE_PASSWORD = "changeit";
    private X509TrustManager trustManager;
    private EvaluateCertGUI verifier;
    private Frame owner;
    private KeyStore caKeyStore;
    private String caKeyStorePath;
    private String caKeyStoreType;
    private char[] caPassphrase;

    /**
     * This takes an array of existing trust managers, and returns an array of
     * extended Trust Managers that include our own code to allow the user to
     * select new certificates at run time.
     * 
     * @param arrayOfTrustManagers must be of type X509TrustManager...
     * 
     * @return an array of JXTrustManagers incorporating the passed in managers.
     * 
     * @throws javax.net.ssl.SSLException
     */
    public static X509TrustManager[] convert(TrustManager[] arrayOfTrustManagers, KeyStore keystore,
                                             String caKeystorePath, char[] caPassphrase, String caKeystoreType, Frame rootFrame)
            throws SSLException
    {
        int numberOfManagers = arrayOfTrustManagers.length;
        X509TrustManager[] returnArray = new X509TrustManager[numberOfManagers];
        for (int i = 0; i < numberOfManagers; i++)
        {
            TrustManager old = arrayOfTrustManagers[i];
            if (old == null)
                throw new SSLException("unexpected SSL error - null trust manager found in trust array: element " + i + " of " + numberOfManagers);

            try
            {
                returnArray[i] = new JXTrustManager((X509TrustManager) old, keystore, caKeystorePath, caPassphrase, caKeystoreType, rootFrame);
            }
            catch (ClassCastException e)
            {
                throw new SSLException("unexpected SSL error - non X509 trust manager found in trust array: element " + i + " of " + numberOfManagers + " is of type " + old.getClass());
            }
        }
        return returnArray;
    }

    /**
     * <p>The local constructor for the TrustManager</p>
     * @param baseTrustManager
     * @param keystore
     * @param certAuthorityKeystorePath
     */
    private JXTrustManager(X509TrustManager baseTrustManager, KeyStore keystore,
                           String certAuthorityKeystorePath, char[] certAuthorityPassphrase, String certAuthorityKeystoreType,
                           Frame rootFrame)
    {
        trustManager = baseTrustManager;
        caKeyStore = keystore;
        caKeyStorePath = certAuthorityKeystorePath;
        caKeyStoreType = certAuthorityKeystoreType;
        caPassphrase = certAuthorityPassphrase;
        owner = rootFrame;
    }

    public X509Certificate[] getAcceptedIssuers()
    {
        return trustManager.getAcceptedIssuers();
    }

    private X509Certificate getCACert(X509Certificate chain[])
    {
        X509Certificate ca = chain[chain.length - 1];
        // check that root certificate is self-signed.
        if (ca.getSubjectDN().equals(ca.getIssuerDN()))
            return ca;
        else
            return null;
    }

    /**
     * Returns whether a particular certificate is one of the
     * accepted issuers.
     * 
     * @param caCert a CA certificate
     * 
     * @return whether caCert is a trusted authority
     */
    private boolean rootCertIsKnown(X509Certificate caCert)
    {
        X509Certificate certificates[] = getAcceptedIssuers();
        if (certificates == null)
            return false;
        for (int i = 0; i < certificates.length; i++)
            if (caCert.equals(certificates[i]))
                return true;

        return false;
    }

    public void checkClientTrusted(X509Certificate chain[], String authType)
            throws CertificateException
    {
        trustManager.checkClientTrusted(chain, authType);
    }

    /**
     * <p>This is the meat of the class, where we verify whether or not a particular
     * certificate is to be trusted.  If it is already in the keystore, then well and good,
     * otherwise we ask the user whether to allow it or not...</p>
     *
     * @param chain a list of certificates forming an certificate chain - these are the certs being checked.
     * @param authType the type of authentication being used; usually 'RSA' I believe...
     * @throws java.security.cert.CertificateException if the certificate is not valid.
     */
    public void checkServerTrusted(X509Certificate chain[], String authType)
            throws CertificateException
    {
        try
        {
            trustManager.checkServerTrusted(chain, authType);  // this will throw an exception if unsuccessfull
                                                               // ... otherwise the cert is o.k., so just continue...
        }
        /**
         * The certificate we've been given is unknown
         */
        catch (CertificateException e)
        {
            // SOME USERS MAY WISH TO DISALLOW THIS FEATURE.
            if ("false".equals(System.getProperty("option.ssl.import.cert.during.connection")))
                throw e;

            X509Certificate certificateAuthorityCert = getCACert(chain);
            if (certificateAuthorityCert == null)
                throw new CertificateException("Invalid Server Certificate: server certificate could not be verified, and the CA certificate is missing from the certificate chain. raw error: " + e);

            if (rootCertIsKnown(certificateAuthorityCert))
                throw new CertificateException("Invalid Server Certificate: The server certificate could not be verified, as it has a bad chain back to a known CA.  raw error: " + e);

            if (verifier == null)
                verifier = new EvaluateCertGUI(owner);

            switch (verifier.isTrusted(certificateAuthorityCert))
            {
                case EvaluateCertGUI.REJECT:
                    throw new CertificateException("user chose not to trust unknown certificate");

                case EvaluateCertGUI.ACCEPT_ONCE:
                    // do nothing, thus signifying acceptance within this method only??
                    return;

                case EvaluateCertGUI.ACCEPT_ALWAYS:
                    try
                    {
                        saveStore(certificateAuthorityCert);
                    }
                    catch (KeyStoreException e1)
                    {
                        throw new CertificateException("unable to save certificate in keystore! " + e1);
                    }
                    return;
            }
        }
    }

    /**
     * <p>This saves the certificate in the cert authority keystore.</p>
     * @param cert the new certificate to save.
     * @throws KeyStoreException
     */
    private void saveStore(X509Certificate cert)
        throws KeyStoreException
    {
        try
        {
            // SPECIAL HANDLING TO TRY DEFAULT PASSWORD FIRST.
            if (caPassphrase == null)
                caPassphrase = "changeit".toCharArray();  // neat feature or filthy hack?  ... you be the judge...

            try
            {
                caKeyStore.load(new FileInputStream(caKeyStorePath), caPassphrase); // if this works, we have a valid password.
            }
            catch (IOException e)
            {
                setupKeyStoreAndPassword();
            }

            if (caKeyStore == null)
                throw new KeyStoreException("unable to open keystore - no valid password or no valid file.");

            String alias = cert.getSubjectDN() + " (" + cert.getSerialNumber().toString() + ")";
            caKeyStore.setCertificateEntry(alias, cert);
            FileOutputStream fos = new FileOutputStream(caKeyStorePath);
            caKeyStore.store(fos, caPassphrase);
            fos.close();
        }
        catch (IOException e)
        {
            KeyStoreException kse = new KeyStoreException("unable to access keystore file");
            kse.initCause(e);
            throw kse;
        }
        catch (GeneralSecurityException e)
        {
            if (e instanceof KeyStoreException)
                throw (KeyStoreException)e;
            else
                throw new KeyStoreException("unable to save keystore " + caKeyStorePath + " error was: " + e);
        }
    }
    private boolean setupKeyStoreAndPassword()
    {
        String message = CBIntText.get("Enter Key Store Password");
        while ((caPassphrase = KeystoreGUI.getPassword(owner, message)) != null)
        {
            caKeyStore = KeystoreGUI.readKeyStore(caPassphrase, caKeyStoreType, caKeyStorePath);

            if (caKeyStore != null)
                return true;    // we have a valid keystore!

            // this message is only displayed if we go around the loop again.
            message = CBIntText.get("Password incorrect. Please try again.");
        }
        return false;
    }

}