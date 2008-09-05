
package com.ca.commons.security.cert.extensions;

import com.ca.commons.security.asn1.*;

/**
 * <pre>
 * NetscapeCertType ::= BIT STRING
 * {
 * 		bit-0 SSL client - this cert is certified for SSL client authentication use
 * 		bit-1 SSL server - this cert is certified for SSL server authentication use
 * 		bit-2 S/MIME - this cert is certified for use by clients (New in PR3)
 * 		bit-3 Object Signing - this cert is certified for signing objects such as Java applets and plugins(New in PR3)
 * 		bit-4 Reserved - this bit is reserved for future use
 * 		bit-5 SSL CA - this cert is certified for issuing certs for SSL use
 * 		bit-6 S/MIME CA - this cert is certified for issuing certs for S/MIME use (New in PR3)
 * 		bit-7 Object Signing CA - this cert is certified for issuing certs for Object Signing (New in PR3)
 * }
 * </pre>
 *
 * @author vbui
 */
public class NetscapeCertType implements V3Extension
{
	String value = null;

    public void init(ASN1Object asn1object) throws Exception
    {
		if (!asn1object.isASN1Type(ASN1Type.BIT_STRING))
			throw new Exception("Wrong ASN.1 type for NetscapeCertType");

		byte[] o = (byte[]) asn1object.getValue();

		StringBuffer buff = new StringBuffer();

		if ((o[1] & (byte)0x80) != 0)
			buff.append("SSL client, ");
		if ((o[1] & (byte)0x40) != 0)
			buff.append("SSL server, ");
		if ((o[1] & (byte)0x20) != 0)
			buff.append("S/MIME, ");
		if ((o[1] & (byte)0x10) != 0)
			buff.append("Object Signing, ");
		if ((o[1] & (byte)0x08) != 0)
			buff.append("Reserved, ");
		if ((o[1] & (byte)0x04) != 0)
			buff.append("SSL CA, ");
		if ((o[1] & (byte)0x02) != 0)
			buff.append("S/MIME CA, ");
		if ((o[1] & (byte)0x01) != 0)
			buff.append("Object Signing CA, ");

		value = buff.toString();
		if (value.endsWith(", "))
			value = value.substring(0, value.length() - 2);
    }

	public String toString()
	{
		return value;
	}
}

