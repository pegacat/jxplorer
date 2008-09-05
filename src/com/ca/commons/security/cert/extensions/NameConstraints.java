
package com.ca.commons.security.cert.extensions;

import com.ca.commons.security.asn1.*;

/**
 * <pre>
 * NameConstraints ::= SEQUENCE {
 *      permittedSubtrees       [0]     GeneralSubtrees OPTIONAL,
 *      excludedSubtrees        [1]     GeneralSubtrees OPTIONAL }
 *
 * GeneralSubtrees ::= SEQUENCE SIZE (1..MAX) OF GeneralSubtree
 *
 * GeneralSubtree ::= SEQUENCE {
 *      base                    GeneralName,
 *      minimum         [0]     BaseDistance DEFAULT 0,
 *      maximum         [1]     BaseDistance OPTIONAL }
 *
 * BaseDistance ::= INTEGER (0..MAX)
 * </pre>
 *
 * @author vbui
 */
public class NameConstraints implements V3Extension
{
    String value = null;

    public void init(ASN1Object asn1object) throws Exception
    {
    	if (!asn1object.isASN1Type(ASN1Type.SEQUENCE))
    		throw new Exception("Wrong ASN.1 type for NameConstraints");

    	if (asn1object.size() >= 1)
    	{
    		value = "permittedSubtrees: " + getSubtrees(asn1object.getComponent(0));
    	}

    	if (asn1object.size() >= 2)
    	{
    		value = value + "\nexcludedSubtrees: " + getSubtrees(asn1object.getComponent(0));
    	}
    }

	public String getSubtrees(ASN1Object obj)
	{
		StringBuffer buff = new StringBuffer();

		for (int i = 0; i < obj.size(); i++)
		{
			buff.append("\n  generalSubtree");
			ASN1Object gsubtree = obj.getComponent(i);
			if (gsubtree.size() >= 1)
			{
				buff.append("\n\tbase: " + IssuerAltName.getGNameString(gsubtree.getComponent(0)));
			}
			if (gsubtree.size() >= 2)
			{
				buff.append("\n\tminimum: " + gsubtree.getComponent(1).getValue());
			}
			if (gsubtree.size() >= 3)
			{
				buff.append("\n\tmaximum: " + gsubtree.getComponent(2).getValue());
			}
		}

		return buff.toString();
	}

    public String toString()
    {
    	return value;
    }
}

