
package com.ca.commons.security.cert.extensions;

import java.util.*;

import com.ca.commons.security.asn1.*;

/**
 * <pre>
 *  IssuerAltName ::= GeneralNames
 *
 *  GeneralNames ::= SEQUENCE SIZE (1..MAX) OF GeneralName
 *
 *  GeneralName ::= CHOICE {
 *       otherName                       [0]     OtherName,
 *       rfc822Name                      [1]     IA5String,
 *       dNSName                         [2]     IA5String,
 *       x400Address                     [3]     ORAddress,
 *       directoryName                   [4]     Name,
 *       ediPartyName                    [5]     EDIPartyName,
 *       uniformResourceIdentifier       [6]     IA5String,
 *       iPAddress                       [7]     OCTET STRING,
 *       registeredID                    [8]     OBJECT IDENTIFIER}
 *
 *  OtherName ::= SEQUENCE {
 *       type-id    OBJECT IDENTIFIER,
 *       value      [0] EXPLICIT ANY DEFINED BY type-id }
 *
 *  EDIPartyName ::= SEQUENCE {
 *       nameAssigner            [0]     DirectoryString OPTIONAL,
 *       partyName               [1]     DirectoryString }
 * </pre>
 *
 * @author vbui
 */
public class IssuerAltName implements V3Extension
{
	String value = null;

    public void init(ASN1Object asn1object) throws Exception
    {
		if (!asn1object.isASN1Type(ASN1Type.SEQUENCE))
			throw new Exception("Wrong ASN.1 type for *AltName");

		for (int i = 0; i < asn1object.size(); i++)
		{
			ASN1Object nextName = (ASN1Object) asn1object.getComponent(i);

			String nextEntry = getGNameString(nextName);

			if (value == null)
				value = nextEntry;
			else
				value = value + "\n" + nextEntry;
		}
    }

	// clean up a DN
	public static String cleanName(String dn)
	{
		StringTokenizer tok = new StringTokenizer(dn, ",");
		Vector toks = new Vector();
		while (tok.hasMoreTokens())
		{
			String nextToken = tok.nextToken();
			if (!nextToken.endsWith(" = null"))
				toks.addElement(nextToken);
		}

		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < toks.size(); i ++)
		{
			if (buff.toString().length() == 0)
				buff.append((String) toks.elementAt(i));
			else
				buff.append(", " + toks.elementAt(i));
		}

		return buff.toString();
	}

	// convert a 4 bytes IP address to a more presentable format
	private static String getIP(String ip)
	{
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < ip.length(); i++)
		{
			buff.append((int) ip.charAt(i) + ".");
		}
		if (buff.toString().endsWith("."))
			buff.deleteCharAt(buff.length() - 1);

		return buff.toString();
	}

	public static String getGNameString(ASN1Object  nextName)
	{
		GeneralName gname = new GeneralName(nextName);
		int type = gname.getType();
		Object valuee = gname.getValue(type);

		String nextEntry = null;
		if (type >= 0 && valuee != null)
		{
			String value = null;
			if (type == 7)
			{ // ipAddress
				value = getIP(valuee.toString());
			}
			else
			{
				value = cleanName(valuee.toString());
			}
			nextEntry = GeneralName.lookUpName(type) + ": " + value;
		}
		else
		{
			nextEntry = "Unrecognised GeneralName type: " + type;
		}

		return nextEntry;
	}

	public String toString()
	{
		return value;
	}
}

