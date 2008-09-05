package com.ca.commons.security.asn1;

import java.util.Hashtable;

/**
 * A collection of object identifiers used in this package and other
 * packages. They are defined by different standards like PKCS, X.509,
 * X.500, etc.
 */
public class ASN1OID implements java.io.Serializable
{

    private static Hashtable id2name = new Hashtable(50);
    private static Hashtable name2id = new Hashtable(50);
    
    /* RSA Data Security Inc. */
    public static String rsadsi		= "1 2 840 113549";
    public static String x9_57		= "1 2 840 10040";
    public static String ansi_x942	= "1 2 840 10046";
    
    public static String digestAlgorithm = rsadsi + " 2";
    public static String md2 = digestAlgorithm + " 2";
    public static String md4 = digestAlgorithm + " 4";
    public static String md5 = digestAlgorithm + " 5";
    
    public static String encryptionAlgorithm = rsadsi + " 3";
    public static String rc2_cbc = encryptionAlgorithm + " 2";
    public static String des_ede3_cbc = encryptionAlgorithm + " 7";
    
    /* PKCS defined by RSA Data Security Inc. */
    public static String pkcs = rsadsi + " 1";
    
    /* PKCS #1 */
    public static String pkcs_1 = pkcs + " 1";
    public static String rsaEncryption = pkcs_1 + " 1";
    public static String md2WithRSAEncryption = pkcs_1 + " 2";
    public static String md4WithRSAEncryption = pkcs_1 + " 3";
    public static String md5WithRSAEncryption = pkcs_1 + " 4";
    public static String sha1WithRSAEncryption = pkcs_1 + " 5";
    
    /* PKCS #5 */
    public static String pkcs_5 = pkcs + " 5";
    public static String pbeWithMD2AndDES_CBC = pkcs_5 + " 1";
    public static String pbeWithMD5AndDES_CBC = pkcs_5 + " 3";
    public static String pbeWithSHA1AndDES_CBC = pkcs_5 + " 10";
    public static String pbeWithSHA1AndRC2_CBC = pkcs_5 + " 11";
    public static String pbeWithSHA1AndRC4 = pkcs_5 + " 12";
    
    
    
    /* PKCS #9 */
    public static String pkcs_9 = pkcs + " 9";
    public static String emailAddress = pkcs_9 + " 1";
    public static String unstructuredName = pkcs_9 + " 2";
    public static String contentType = pkcs_9 + " 3";
    public static String messageDigest = pkcs_9 + " 4";
    public static String signingTime = pkcs_9 + " 5";
    public static String countersignature = pkcs_9 + " 6";
    public static String challengePassword = pkcs_9 + " 7";
    public static String unstructuredAddress = pkcs_9 + " 8";
    public static String extendedCertificateAttributes = pkcs_9 + " 9";
    public static String friendlyName = pkcs_9 + " 20";
    public static String localKeyID = pkcs_9 + " 21";
    public static String certTypes = pkcs_9 + " 22";
    public static String x509Certificate = certTypes + " 1";
    public static String sdsiCertificate = certTypes + " 2";
    public static String crlTypes = pkcs_9 + " 23";
    public static String x509Crl = crlTypes + " 1";
    
    /* PKCS #7 */
    public static String pkcs_7 = pkcs + " 7";
    public static String data = pkcs_7 + " 1";
    public static String signedData = pkcs_7 + " 2";
    public static String envelopedData = pkcs_7 + " 3";
    public static String signedAndEnvelopedData = pkcs_7 + " 4";
    public static String digestedData = pkcs_7 + " 5";
    public static String encryptedData = pkcs_7 + " 6";
    /* S/MIME */
    public static String ct_authData = pkcs_9 + " 16 1 2";
    public static String macValue = pkcs_9 + " 16 2 8";
    
    /* pkcs12 */
    public static String pkcs_12 = pkcs + " 12";
    public static String pkcs_12PbeIds = pkcs_12 + " 1";
    public static String pbeWithSHA1And128BitRC4 = pkcs_12PbeIds + " 1";
    public static String pbeWithSHA1And40BitRC4 = pkcs_12PbeIds + " 2";
    public static String pbeWithSHA1And3DES = pkcs_12PbeIds + " 3";
    public static String pbeWithSHA1And2DES = pkcs_12PbeIds + " 4";
    public static String pbeWithSHA1And128BitRC2 = pkcs_12PbeIds + " 5";
    public static String pbeWithSHA1And40BitRC2 = pkcs_12PbeIds + " 6";
    
    public static String pkcs_12Version1 = pkcs_12 + " 10";
    public static String pkcs_12BagIds = pkcs_12Version1 + " 1";
    public static String keyBag = pkcs_12BagIds + " 1";
    public static String pkcs_8ShroudedKeyBag = pkcs_12BagIds + " 2";
    public static String certBag = pkcs_12BagIds + " 3";
    public static String crlBag = pkcs_12BagIds + " 4";
    public static String secretBag = pkcs_12BagIds + " 5";
    public static String safeContentsBag = pkcs_12BagIds + " 6";
    
    public static String pkcs_12CertBagIds = pkcs_12 + " 4";
    public static String x509CertCRLBagId = pkcs_12CertBagIds + " 1";
    public static String SDSICertBagId = pkcs_12CertBagIds + " 2";
    
    
    /* X.500 defined object identifiers */
    public static String X500 = "2 5";
    public static String X509 = X500 + " 4";
    public static String commonName = X509 + " 3";
    public static String country = X509 + " 6";
    public static String locality = X509 + " 7";
    public static String stateOrProvince = X509 + " 8";
    public static String organization = X509 + " 10";
    public static String organizationalUnit = X509 + " 11";
    public static String surname = X509 + " 4";
    public static String serialNumber = X509 + " 5";
    public static String title = X509 + " 12";
    public static String description = X509 + " 13";
    public static String givenName = X509 + " 42";
    public static String initials = X509 + " 43";
    public static String uniqueIdentifier = X509 + " 45";
    
    /* X.509 v3 extension */
    public static String ld_ce		 = "2.5.29";
    public static String subjectKeyIdentifier	= ld_ce + ".14";
    public static String keyUsage				= ld_ce + ".15";
    public static String privateKeyUsagePeriod  = ld_ce + ".16";
    public static String subjectAltName 		= ld_ce + ".17";
    public static String issuerAltName			= ld_ce + ".18";
    public static String basicConstraints 		= ld_ce + ".19";
    public static String crlNumber				= ld_ce + ".20";
    
    public static String nameConstraints        = ld_ce + ".30";
    public static String crlDistributionPoints  = ld_ce + ".31";
    public static String certificatePolicies 	= ld_ce + ".32";
    public static String policyMappings         = ld_ce + ".33";
    public static String authorityKeyIdentifier = ld_ce + ".35";
    public static String policyConstraints      = ld_ce + ".36";
    public static String extendedKeyUsage  		= ld_ce + ".37";
    
    // AuthorityInfoAccess
    public static String id_pkix_dot = "1.3.6.1.5.5.7";
    public static String id_pe_dot	 = id_pkix_dot + ".1";
    public static String id_pe_authorityInfoAccess = id_pe_dot + ".1";
    
    // CRL Related Constants
    public static String CRLReason              = ld_ce + " 21";
    public static String CRLNumber              = ld_ce + " 20";
    public static String invalidityDate         = ld_ce + " 24";
    public static String deltaCRLIndicator      = ld_ce + " 27";
    public static String certIssuer             = ld_ce + " 29";
    public static String issuingDPoint          = ld_ce + " 28";
    
    public static String id_pkix = "1 3 6 1 5 5 7";
    
    // OCSP Related Constants
    public static String id_pe	 = id_pkix + " 1";
    public static String id_ad	 = id_pkix + " 48";
    public static String authInfoAccessOCSP      = id_ad + " 1";
    public static String authInfoAccesscaIssuers = id_ad + " 2";
    
    // Cert Policy OIDs
    public static String cpsOID			= id_pkix + " 2 1";
    public static String userNoticeOID	= id_pkix + " 2 2";
    
    // Extended Key Usage
    public static String id_kp = id_pkix + " 3";
    public static String id_kp_serverAuth		= id_kp + " 1";
    public static String id_kp_clientAuth		= id_kp + " 2";
    public static String id_kp_codeSigning  	= id_kp + " 3";
    public static String id_kp_emailProtection	= id_kp + " 4";
    public static String id_kp_ipsecEndSystem 	= id_kp + " 5";
    public static String id_kp_ipsecTunnel      = id_kp + " 6";
    public static String id_kp_ipsecUser        = id_kp + " 7";
    public static String id_kp_timeStamping     = id_kp + " 8";
    public static String id_kp_OCSPSigning      = id_kp + " 9";
    
    
    /* public key identifiers */
    public static String id_dsa			= x9_57 + "4 1";
    public static String dhpublicnumber = ansi_x942	+ "2 1";
    
    /* algorithm identifiers */
    public static String algorithm = "1 3 14 3 2";
    public static String sha = algorithm + " 18";
    public static String sha1 = algorithm + " 26";
    public static String shaWithRSAEncryption = algorithm + " 15";
    
    /* Netscape defined object identifiers */
    public static String netscape = "2 16 840 1 113730";
    public static String netscapeCertExt = netscape + " 1";
    public static String netscapeCertType = netscapeCertExt + " 1";
    public static String netscapeComment = netscapeCertExt + " 13";
    
    static {
    
        /* digest algorithms */
        register(md2, "md2");
        register(md4, "md4");
        register(md5, "md5");
        register(sha1, "sha1");
        
        /* encryption algorithms */
        register(rsaEncryption, "rsaEncryption");
        register(des_ede3_cbc, "des_ede3_cbc");
        register(rc2_cbc, "rc2_cbc");
        
        /* signature algorithms */
        register(md2WithRSAEncryption, "md2WithRSAEncryption");
        register(md4WithRSAEncryption, "md4WithRSAEncryption");
        register(md5WithRSAEncryption, "md5WithRSAEncryption");
        register(sha1WithRSAEncryption, "sha1WithRSAEncryption");
        
        /* X.501 attributes */
        register(country, "country");
        register(stateOrProvince, "stateOrProvince");
        register(locality, "locality");
        register(organization, "organization");
        register(organizationalUnit, "organizationalUnit");
        register(commonName, "commonName");
        register(surname, "surname");
        register(serialNumber, "serialNumber");
        register(title, "title");
        register(description, "description");
        register(givenName, "givenName");
        register(initials, "initials");
        register(uniqueIdentifier, "uniqueIdentifier");
        
        /* pkcs9 attributes */
        register(emailAddress, "emailAddress");
        register(unstructuredName, "unstructuredName");
        register(contentType, "contentType");
        register(messageDigest, "messageDigest");
        register(signingTime, "signingTime");
        register(countersignature, "countersignature");
        register(challengePassword, "challengePassword");
        register(unstructuredAddress, "unstructuredAddress");
        
        /* pkcs7 data types*/
        register(data, "data");
        register(signedData, "signedData");
        register(envelopedData, "envelopedData");
        register(signedAndEnvelopedData, "signedAndEnvelopedData");
        register(digestedData, "digestedData");
        register(encryptedData, "encryptedData");
        
        /* s/mime attributes */
        register(ct_authData, "ct_authData");
        register(macValue, "macValue");
        
        /* X.509 v3 extensions */
        register(subjectKeyIdentifier, "Subject Key Identifier");
        register(keyUsage, "Key Usage");
        register(privateKeyUsagePeriod, "Private Key Usage Period");
        register(subjectAltName, "Subject Alternative Name");
        register(issuerAltName, "Issuer Alternative Name");
        register(basicConstraints, "Basic Constraints");
        register(crlNumber, "CRL Number");
        
        register(nameConstraints, "Name Constraints");
        register(crlDistributionPoints, "CRL Distribution Points");
        register(certificatePolicies, "Certificate Policies");
        register(policyMappings, "Policy Mappings");
        register(authorityKeyIdentifier, "Authority Key Identifier");
        register(policyConstraints, "Policy Constraints");
        register(extendedKeyUsage, "Extended Key Usage");
        
        register(id_pe_authorityInfoAccess, "Authority Information Access");
        
        /* netscape attributes */
        register(netscapeCertType, "Netscape Cert Type");
        register(netscapeComment, "Netscape Comment");
        register(netscapeCertExt, "Netscape Certificate Extension");
        
        register(id_kp_serverAuth, "TLS Web server authentication");
        register(id_kp_clientAuth, "TLS Web client authentication");
        register(id_kp_codeSigning, "Code signing");
        register(id_kp_emailProtection, "E-mail protection");
        register(id_kp_ipsecEndSystem, "IP security end system");
        register(id_kp_ipsecTunnel, "IP security tunnel termination");
        register(id_kp_ipsecUser, "IP security user");
        register(id_kp_timeStamping, "Timestamping");
        register(id_kp_OCSPSigning, "OCSP Signing");
        register("1 3 6 1 4 1 311 20 2 2", "Smart Card Logon");
        
        /* public key identifiers */
        register(id_dsa, "id-dsa");
        register(dhpublicnumber, "dhpublicnumber");
    }
    
    /**
     * Gets the name of an object identifier.
     */
    public static String getName(String id)
    {
        String name = (String) id2name.get(id);
        if (name == null)
        {
            return id;
        }
        else
        {
            return name;
        }
    }
    
    /**
     * Gets an object identifier from its name.
     */
    public static String getID(String name)
    {
        String id = (String) name2id.get(name);
        if (id == null)
        {
            return name;
        }
        else
        {
            return id;
        }
    }
    
    /**
     * Register Object ID and its name.
     */
    private static void register(String id, String name)
    {
        id2name.put(id, name);
        name2id.put(name, id);
    }
}
