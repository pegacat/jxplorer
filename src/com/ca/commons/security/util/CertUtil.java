package com.ca.commons.security.util;

import com.ca.commons.security.cert.extensions.*;
import com.ca.commons.security.asn1.*;

import java.io.*;
import java.util.*;
import java.security.cert.*;
import java.security.*;

import java.math.BigInteger;

public class CertUtil
{

    /**
	 * Load an X509Certificate object from a specified file.  If this
	 * function is able to load the certificate from the file, return
	 * the object, otherwise, return <code>null</code>.
	 *
     * - <i>Warning: may not correctly handle pem files - CB</i>
     *
	 * @param fileName The name of the file containing the X.509 Certificate.
	 * @return An <code>X509Certificate</code> object containing the
	 * certificate, or <code>null</code>.
	 */

// TODO: logging, and bubble exception properly to a level that can display them.


	public static X509Certificate loadX509Certificate (String fileName)
	{
        return loadX509Certificate(new File(fileName));

    }
    public static X509Certificate loadX509Certificate (File file)
    {
		try
		{
			FileInputStream inputStream = new FileInputStream (file);

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate)cf.generateCertificate(inputStream);
			inputStream.close();

			return cert;
		}
		catch (Exception anyException)
		{
			anyException.printStackTrace ();
			return null;
		}
	}


	/**
	 * Load certificate from byte array (DER)
	 */
	public static X509Certificate loadX509Certificate (byte[] certdata)
	{
		try
		{
			ByteArrayInputStream inputStream = new ByteArrayInputStream (certdata);

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate)cf.generateCertificate(inputStream);
			inputStream.close();

			return cert;
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			return null;
		}
	}


	/**
	 * Saving a DER encoded certificate to a directory.
	 * Precondition: directory must be created before hand.
	 */
	public static void saveCertToDisk(byte[] certBytes, String outputDir) throws Exception
	{
		X509Certificate cert = loadX509Certificate(certBytes);
		FileOutputStream fos = null;
		String fileName = outputDir + "/" + cert.getSubjectDN().getName() + "_" + new Date().getTime() + ".der";
		try
		{
			fos = new FileOutputStream(fileName);
		}
		catch (Exception ex)
		{
			System.out.println("Could not open " + fileName + " for saving cert, now use " +
					(fileName = new Date().getTime() + ".der"));
			fos = new FileOutputStream(fileName);
		}

		fos.write(certBytes);
		fos.close();
	}


	public static AuthorityKeyIdentifier getAuthorityKeyIdentifier(X509Certificate theCert) {
		AuthorityKeyIdentifier v3e = null;
		byte [] aki = theCert.getExtensionValue(ASN1OID.authorityKeyIdentifier);
		ASN1Object rext = decodeExtension(aki);
		if ( rext != null ) {
			v3e = new AuthorityKeyIdentifier();
			try	{
				v3e.init(rext);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return v3e;
	}


	public static byte [] getIssuerKeyId(X509Certificate theCert) {
		AuthorityKeyIdentifier v3e = getAuthorityKeyIdentifier(theCert);
		if ( v3e == null ) return null;
		byte [] keyId = v3e.getKeyId();
		return keyId;
	}


	public static BigInteger getIssuerSerialNumber(X509Certificate theCert) {
		AuthorityKeyIdentifier v3e = getAuthorityKeyIdentifier(theCert);
		if ( v3e == null ) return null;
		BigInteger issuerSerialNumber = v3e.getSerialNumber();
		return issuerSerialNumber;
	}


	public static ASN1Object decodeExtension(byte[] extvalue) {
		ASN1Object rext = null;
		if ( extvalue != null ) {
			try	{
				DERCoder derCoder = new DERCoder();
				ASN1Object ext = derCoder.decode(extvalue);

				if (ext.isASN1Type(ASN1Type.OCTET_STRING))
					rext = derCoder.decode((byte[])ext.getValue());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return rext;
	}


	public static SubjectKeyIdentifier getSubjectKeyIdentifier(X509Certificate theCert) {
		SubjectKeyIdentifier v3e = null;
		byte [] ski = theCert.getExtensionValue(ASN1OID.subjectKeyIdentifier);
		ASN1Object rext = decodeExtension(ski);
		if ( rext != null ) {
			v3e = new SubjectKeyIdentifier();
			try	{
				v3e.init(rext);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return v3e;
	}


	public static byte [] getSubjectKeyId(X509Certificate theCert) {
		SubjectKeyIdentifier v3e = getSubjectKeyIdentifier(theCert);
		if ( v3e == null ) return null;
		byte [] keyId = v3e.getKeyId();
		return keyId;
	}


	/**
	 * Returns the path length constraint of a (root ca) certificate.
	 * For profile validation, if this is a (subordinate) CA certificate then:
	 * subordinate ca path-length constraint must be less than that of root ca
	 * return -1 if the certificate does not include the BasicConstraints field
	 * or if it has a NONE-path-length constraint.
	 */
    public static int getPathLenConstraint(X509Certificate cert)
	{
        // use constant instead String bsOID = "2.5.29.19"; // Basic Constraints;
		byte[] bs = cert.getExtensionValue(ASN1OID.basicConstraints);
		if (bs != null)
		{
			try
			{
				DERCoder derCoder = new DERCoder();
				ASN1Object ext = derCoder.decode(bs);
				ext = derCoder.decode((byte[])ext.getValue());

				BasicConstraints bsASN = new BasicConstraints();
				bsASN.init(ext);

				return bsASN.pathLenConstraint;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				// ignore
			}
		}

		return -1;
	}

}