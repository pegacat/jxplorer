
package com.ca.commons.security.cert.extensions;

import com.ca.commons.security.asn1.*;
import com.ca.commons.cbutil.CBParse;

/**
 * <pre>
 * SubjectKeyIdentifier ::= KeyIdentifier
 * KeyIdentifier ::= OCTET STRING
 * </pre>
 *
 * @author vbui
 */
public class SubjectKeyIdentifier implements V3Extension
{
	String value = null;
	byte [] keyId;

    public byte [] getKeyId() { return keyId; }

    public void init(ASN1Object asn1object) throws Exception
    {
		if (!asn1object.isASN1Type(ASN1Type.OCTET_STRING))
			throw new Exception("Wrong ASN.1 type for SubjectKeyIdentifier");

		keyId = (byte[]) asn1object.getValue();
		value = CBParse.bytes2HexSplit(keyId, 4);
    }

	public String toString()
	{
		return value;
	}
}

