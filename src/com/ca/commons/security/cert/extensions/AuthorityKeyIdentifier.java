
package com.ca.commons.security.cert.extensions;

import com.ca.commons.security.asn1.*;
import com.ca.commons.cbutil.CBParse;

import java.math.BigInteger;

/**
 * <pre>
 * AuthorityKeyIdentifier ::= SEQUENCE {
 *       keyIdentifier             [0] KeyIdentifier            OPTIONAL,
 *       authorityCertIssuer       [1] GeneralNames             OPTIONAL,
 *       authorityCertSerialNumber [2] CertificateSerialNumber  OPTIONAL }
 *     -- authorityCertIssuer and authorityCertSerialNumber shall both
 *     -- be present or both be absent
 *
 * KeyIdentifier ::= OCTET STRING
 * </pre>
 *
 * @author vbui
 */
public class AuthorityKeyIdentifier implements V3Extension
{
    String value = null;
    BigInteger serialNumber;
	byte [] keyId;

    public BigInteger getSerialNumber() { return serialNumber; }

    public byte [] getKeyId() { return keyId; }

    public void init(ASN1Object asn1object) throws Exception
    {
	    if (!asn1object.isASN1Type(ASN1Type.SEQUENCE))
	    	throw new Exception("Wrong ASN.1 type for AuthorityKeyIdentifier");

		for (int i = 0; i < asn1object.size(); i++)
		{
			ASN1Object compBig = asn1object.getComponent(i);
			ASN1Object nextComp = (ASN1Object) compBig.getValue();

			if (nextComp.isASN1Type(ASN1Type.OCTET_STRING) && i == 0)
			{
				keyId = (byte[]) nextComp.getValue();
				value = "KeyID: " + CBParse.bytes2HexSplit(keyId, 4);
			}
			else if (nextComp.isASN1Type(ASN1Type.OCTET_STRING))
			{
				if (value == null)
					value = "Authority Cert SerialNumber: " + CBParse.bytes2HexSplit((byte[]) nextComp.getValue(), 4);
				else
					value = value + "\nAuthority Cert SerialNumber: " + CBParse.bytes2HexSplit((byte[]) nextComp.getValue(), 4);
				serialNumber = new BigInteger((byte[]) nextComp.getValue());
			}
			else
			{
				ASN1Object gnames = (ASN1Object) nextComp.getValue();

				IssuerAltName ian = new IssuerAltName();
				ian.init(gnames);

				String gnamesString = ian.toString();
				if (gnamesString.startsWith("Unrecognised"))
					gnamesString = "Directory Address: " + IssuerAltName.cleanName(new Name(gnames).toString());

				if (value == null)
					value = gnamesString;
				else
					value = value + "\n" + gnamesString;
			}

		}
    }

	public String toString()
	{
		return value;
	}
}

