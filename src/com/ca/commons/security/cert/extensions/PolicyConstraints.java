
package com.ca.commons.security.cert.extensions;

import com.ca.commons.security.asn1.*;

/**
 * <pre>
 * PolicyConstraints ::= SEQUENCE {
 *      requireExplicitPolicy           [0] SkipCerts OPTIONAL,
 *      inhibitPolicyMapping            [1] SkipCerts OPTIONAL }
 *
 * SkipCerts ::= INTEGER (0..MAX)
 * </pre>
 *
 * @author vbui
 */
public class PolicyConstraints implements V3Extension
{
    String value = null;

    public void init(ASN1Object asn1object) throws Exception
    {
    	if (!asn1object.isASN1Type(ASN1Type.SEQUENCE))
    		throw new Exception("Wrong ASN.1 type for PolicyConstraints");

    	if (asn1object.size() >= 1)
    	{
    		ASN1Object requireExplicitPolicy = asn1object.getComponent(0);
    		if (requireExplicitPolicy.getValue() == null)
    			throw new Exception("Wrong ASN.1 type for PolicyConstraints.requireExplicitPolicy");

    		value = "requireExplicitPolicy: " + requireExplicitPolicy.getValue();
    	}

    	if (asn1object.size() >= 2)
    	{
    		ASN1Object inhibitPolicyMapping = asn1object.getComponent(1);
    		if (inhibitPolicyMapping.getValue() == null)
    			throw new Exception("Wrong ASN.1 type for PolicyConstraints.inhibitPolicyMapping");

    		value = value + "\ninhibitPolicyMapping: " + inhibitPolicyMapping.getValue();
    	}
    }

    public String toString()
    {
    	return value;
    }
}

