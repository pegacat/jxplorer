
package com.ca.commons.security.cert.extensions;

import com.ca.commons.security.asn1.*;

/**
 * <pre>
 * BasicConstraints ::= SEQUENCE {
 *      cA                      BOOLEAN DEFAULT FALSE,
 *      pathLenConstraint       INTEGER (0..MAX) OPTIONAL }
 * </pre>
 *
 * @author vbui
 */
public class BasicConstraints implements V3Extension
{
    String value = null;

	public int pathLenConstraint = -1;

    public void init(ASN1Object asn1object) throws Exception
    {
	    if (!asn1object.isASN1Type(ASN1Type.SEQUENCE))
	    	throw new Exception("Wrong ASN.1 type for BasicConstraints");

	    if (asn1object.size() == 0)
	    {
	    	value = "CA: False\nPath Length Constraint: None";
			return;
	    }

		if (asn1object.size() >= 1)
		{
			Object ca = asn1object.getComponent(0).getValue();
			if (!(ca instanceof Boolean))
				throw new Exception("Wrong ASN.1 type for BasicConstraints.cA");

			value = "CA: " + ca;
		}

		if (asn1object.size() >= 2)
		{
			Object pathLen = asn1object.getComponent(1).getValue();
			value = value + "\nPath Length Constraint: " + pathLen;
			try
			{
				int len = Integer.parseInt(pathLen.toString());
				if (len >= 0) pathLenConstraint = len;
			}
			catch (NumberFormatException ex)
			{
				// ignore
			}
		}
		else
		{
			value = value + "\nPath Length Constraint: None";
		}
    }

	public String toString()
	{
		return value;
	}
}

