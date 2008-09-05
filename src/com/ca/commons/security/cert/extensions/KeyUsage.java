
package com.ca.commons.security.cert.extensions;

import com.ca.commons.security.asn1.*;

/**
 * <pre>
 * KeyUsage ::= BIT STRING
 * {
 *     digitalSignature        (0),
 *     nonRepudiation          (1),
 *     keyEncipherment         (2),
 *     dataEncipherment        (3),
 *     keyAgreement            (4),
 *     keyCertSign             (5),
 *     cRLSign                 (6),
 *     encipherOnly            (7),
 *     decipherOnly            (8) }
 * </pre>
 *
 * @author vbui
 */
public class KeyUsage implements V3Extension
{
	String value = null;

    public void init(ASN1Object asn1object) throws Exception
    {
		if (!asn1object.isASN1Type(ASN1Type.BIT_STRING))
			throw new Exception("Wrong ASN.1 type for KeyUsage");

		byte[] o = (byte[]) asn1object.getValue();

		StringBuffer buff = new StringBuffer();

		if ((o[1] & (byte)0x80) != 0)
			buff.append("digitalSignature, ");
		if ((o[1] & (byte)0x40) != 0)
			buff.append("nonRepudiation, ");
		if ((o[1] & (byte)0x20) != 0)
			buff.append("keyEncipherment, ");
		if ((o[1] & (byte)0x10) != 0)
			buff.append("dataEncipherment, ");
		if ((o[1] & (byte)0x08) != 0)
			buff.append("keyAgreement, ");
		if ((o[1] & (byte)0x04) != 0)
			buff.append("keyCertSign, ");
		if ((o[1] & (byte)0x02) != 0)
			buff.append("cRLSign, ");
		if ((o[1] & (byte)0x01) != 0)
			buff.append("encipherOnly, ");
		if ((o[0] & (byte)0x80) != 0)			// ??
			buff.append("decipherOnly, ");

		value = buff.toString();
		if (value.endsWith(", "))
			value = value.substring(0, value.length() - 2);
    }

	public String toString()
	{
		return value;
	}
}

