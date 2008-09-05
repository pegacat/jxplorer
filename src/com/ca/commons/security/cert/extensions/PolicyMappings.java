
package com.ca.commons.security.cert.extensions;

import com.ca.commons.security.asn1.*;

/**
 * <pre>
 * policyMappings EXTENSION ::= {
 *         SYNTAX  PolicyMappingsSyntax
 *         IDENTIFIED BY id-ce-policyMappings }
 *
 * PolicyMappingsSyntax ::= SEQUENCE SIZE (1..MAX) OF SEQUENCE {
 *         issuerDomainPolicy           CertPolicyId,
 *         subjectDomainPolicy          CertPolicyId }
 * </pre>
 *
 * @author vbui
 */
public class PolicyMappings implements V3Extension
{
	String value = null;

    public void init(ASN1Object asn1object) throws Exception
    {
		if (!asn1object.isASN1Type(ASN1Type.SEQUENCE))
			throw new Exception("Wrong ASN.1 type for PolicyMappings");

		for (int i = 0; i < asn1object.size(); i++)
		{
			Sequence seq = (Sequence) asn1object.getComponent(i);
			for (int j = 0; j < seq.size(); j++)
			{
				ASN1Object nextComp = seq.getComponent(j);
				String certPolicyID = nextComp.getValue().toString();

				if (j == 0)
				{
					if (value == null)
						value = "Issuer Domain Policy: " + certPolicyID;
					else
						value = value + "\n" + "Issuer Domain Policy: " + certPolicyID;
				}
				else if (j == 1)
				{
					value = value + "\n" + "Subject Domain Policy: " + certPolicyID;
				}
			}
		}
    }

	public String toString()
	{
		return value;
	}
}

