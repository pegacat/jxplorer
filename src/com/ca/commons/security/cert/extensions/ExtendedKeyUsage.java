
package com.ca.commons.security.cert.extensions;

import com.ca.commons.security.asn1.*;

/**
 * <pre>
 * ExtKeyUsageSyntax ::= SEQUENCE SIZE (1..MAX) OF KeyPurposeId
 *
 * KeyPurposeId ::= OBJECT IDENTIFIER
 *
 * serverAuth (1 3 6 1 5 5 7 3 1) -- TLS Web server authentication
 * clientAuth (1 3 6 1 5 5 7 3 2) -- TLS Web client authentication
 * codeSigning (1 3 6 1 5 5 7 3 3) -- Code signing
 * emailProtection (1 3 6 1 5 5 7 3 4) -- E-mail protection
 * ipsecEndSystem (1 3 6 1 5 5 7 3 5) -- IP security end system
 * ipsecTunnel (1 3 6 1 5 5 7 3 6) -- IP security tunnel termination
 * ipsecUser (1 3 6 1 5 5 7 3 7) -- IP security user
 * timeStamping (1 3 6 1 5 5 7 3 8) -- Timestamping
 * OCSP Signing (1 3 6 1 5 5 7 3 9) -- OCSP Signing
 * smartCard Logon (1 3 6 1 4 1 311 20 2 2) -- Smart Card Logon
 * </pre>
 *
 * @author vbui
 */
public class ExtendedKeyUsage implements V3Extension
{
    String value = null;

    public void init(ASN1Object asn1object) throws Exception
    {
	    if (!asn1object.isASN1Type(ASN1Type.SEQUENCE))
	    	throw new Exception("Wrong ASN.1 type for ExtendedKeyUsage");

	    for (int i = 0; i < asn1object.size(); i++)
	    {
			Object pur = asn1object.getComponent(i).getValue();
			if ( pur instanceof String ) {	// if not, it's not in table !
				String spur = (String) pur;
				spur = ASN1OID.getName(spur);
		    	if (spur != null) {
					if (value == null)
						value = spur;
					else
						value = value + "\n" + spur;
				}
			}
	    }
    }

	public String toString()
	{
		return value;
	}
}

