
package com.ca.commons.security.cert.extensions;

import com.ca.commons.security.asn1.*;

import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * <pre>
 * PrivateKeyUsagePeriod ::= SEQUENCE {
 *      notBefore       [0]     GeneralizedTime OPTIONAL,
 *      notAfter        [1]     GeneralizedTime OPTIONAL }
 * </pre>
 *
 * @author vbui
 */
public class PrivateKeyUsagePeriod implements V3Extension
{
    String value = null;

    public void init(ASN1Object asn1object) throws Exception
    {
		if (!asn1object.isASN1Type(ASN1Type.SEQUENCE))
			throw new Exception("Wrong ASN.1 type for PrivateKeyUsagePeriod");

		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		SimpleDateFormat printer = new SimpleDateFormat("MMM d, yyyy    h:mm:ss a");

		String dateString = null;

		for (int i = 0; i < asn1object.size(); i++)
		{
			ASN1Object nextComp = (ASN1Object) asn1object.getComponent(i).getValue();

			if (nextComp.isASN1Type(ASN1Type.OCTET_STRING) && i == 0)
			{
				String notBefore = new String((byte[])nextComp.getValue());
				try
				{
					Date hotdate = formatter.parse(notBefore);
					value = "Not Before: " + printer.format(hotdate);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					value = "Not Before: invalid date";
				}
			}
			else if (nextComp.isASN1Type(ASN1Type.OCTET_STRING) && i == 1)
			{
				String notAfter = new String((byte[])nextComp.getValue());
				try
				{
					Date hotdate = formatter.parse(notAfter);
					value = value + "\nNot After: " + printer.format(hotdate);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					value = value + "\nNot After: invalid date";
				}
			}
		}
    }

	public String toString()
	{
		return value;
	}
}

