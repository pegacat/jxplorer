
package com.ca.commons.security.cert.extensions;


import com.ca.commons.cbutil.CBParse;
import com.ca.commons.security.asn1.*;

import java.util.Hashtable;

/**
 * <pre>
 * CertificatePolicies ::= SEQUENCE SIZE (1..MAX) OF PolicyInformation
 *
 * PolicyInformation ::= SEQUENCE {
 *      policyIdentifier   CertPolicyId,
 *      policyQualifiers   SEQUENCE SIZE (1..MAX) OF
 *              PolicyQualifierInfo OPTIONAL }
 *
 * CertPolicyId ::= OBJECT IDENTIFIER
 *
 * PolicyQualifierInfo ::= SEQUENCE {
 *        policyQualifierId  PolicyQualifierId,
 *        qualifier        ANY DEFINED BY policyQualifierId }
 *
 * -- Implementations that recognize additional policy qualifiers shall
 * -- augment the following definition for PolicyQualifierId
 *
 * PolicyQualifierId ::=
 *     OBJECT IDENTIFIER ( id-qt-cps | id-qt-unotice )
 *
 * -- CPS pointer qualifier
 *
 *
 * CPSuri ::= IA5String
 *
 * -- user notice qualifier
 *
 * UserNotice ::= SEQUENCE {
 *      noticeRef        NoticeReference OPTIONAL,
 *      explicitText     DisplayText OPTIONAL}
 *
 * NoticeReference ::= SEQUENCE {
 *      organization     DisplayText,
 *      noticeNumbers    SEQUENCE OF INTEGER }
 *
 * DisplayText ::= CHOICE {
 *      visibleString    VisibleString  (SIZE (1..200)),
 *      bmpString        BMPString      (SIZE (1..200)),
 *      utf8String       UTF8String     (SIZE (1..200)) }
 * </pre>
 *
 * @author vbui
 */
public class CertificatePolicies implements V3Extension
{
	String value = null;

    static Hashtable qualifierIDName = new Hashtable();

    static
    {
    	qualifierIDName.put(ASN1OID.cpsOID, "CPS");
    }

    public void init(ASN1Object asn1object) throws Exception
    {
		if (!asn1object.isASN1Type(ASN1Type.SEQUENCE))
			throw new Exception("Wrong ASN.1 type for CertificatePolicies");

		for (int i = 0; i < asn1object.size(); i++)
		{
			if (!(asn1object.getComponent(i) instanceof Sequence))
				throw new Exception("CertificatePolicies component is not sequence");

			Sequence seq = (Sequence) asn1object.getComponent(i);
			for (int j = 0; j < seq.size(); j++)
			{
				ASN1Object nextComp = seq.getComponent(j);

				if (j == 0)
				{
					String certPolicyID = nextComp.getValue().toString();

					if (value == null)
						value = "Certificate Policy ID: " + certPolicyID;
					else
						value = value + "\n" + "Certificate Policy ID: " + certPolicyID;
				}
				else if (j == 1)
				{
					value = value + "\n" + " Qualifier Info: ";

					if (!(nextComp instanceof Sequence))
						throw new Exception("CertificatePolicies component.policyQualifierInfo is not sequence");

					nextComp = nextComp.getComponent(0);

					String qualifierID = nextComp.getComponent(0).getValue().toString();
					if (qualifierIDName.get(qualifierID) != null)
					{
						qualifierID = (String) qualifierIDName.get(qualifierID);
					}
					value = value + "\n" + "  Qualifier ID: " + qualifierID;

					ASN1Object qualifier = nextComp.getComponent(1);
					if (qualifier.getValue() != null)
					{
						value = value + "\n" + "  Qualifier: " + qualifier.getValue();
					}
					else
					{
						value = value + "\n" + "  Qualifier: " + CBParse.bytes2HexSplit(qualifier.getByteArray(), 4, 36);
					}
				}
			}
		}
    }

	public String toString()
	{
		return value;
	}
}

