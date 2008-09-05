
package com.ca.commons.security.cert.extensions;

import com.ca.commons.security.asn1.*;

/**
 * <pre>
 * cRLDistributionPoints ::= {
 *       CRLDistPointsSyntax }
 *
 * CRLDistPointsSyntax ::= SEQUENCE SIZE (1..MAX) OF DistributionPoint
 *
 * DistributionPoint ::= SEQUENCE {
 *       distributionPoint       [0]     DistributionPointName OPTIONAL,
 *       reasons                 [1]     ReasonFlags OPTIONAL,
 *       cRLIssuer               [2]     GeneralNames OPTIONAL }
 *
 * DistributionPointName ::= CHOICE {
 *       fullName                [0]     GeneralNames,
 *       nameRelativeToCRLIssuer [1]     RelativeDistinguishedName }
 *
 * ReasonFlags ::= BIT STRING {
 *       unused                  (0),
 *       keyCompromise           (1),
 *       cACompromise            (2),
 *       affiliationChanged      (3),
 *       superseded              (4),
 *       cessationOfOperation    (5),
 *       certificateHold         (6) }
 *
 * </pre>
 *
 * @author vbui
 */
public class CRLDistributionPoints implements V3Extension
{
    String value = null;

    public void init(ASN1Object asn1object) throws Exception
    {
    	if (!asn1object.isASN1Type(ASN1Type.SEQUENCE))
    		throw new Exception("Wrong ASN.1 type for CRLDistributionPoints");

    	for (int i = 0; i < asn1object.size(); i++)
    	{
    		ASN1Object distributionPoint = asn1object.getComponent(i);

    		if (!distributionPoint.isASN1Type(ASN1Type.SEQUENCE))
    			throw new Exception("Wrong ASN.1 type for CRLDistributionPoints.distributionPoint");

    		for (int j = 0; j < distributionPoint.size(); j++)
    		{
	    		ASN1Object nextComp = distributionPoint.getComponent(j);

				if (j == 0)
				{
					ASN1Object fullName = (ASN1Object)((ASN1Object) nextComp.getValue()).getValue();

					if (value == null)
						value = IssuerAltName.getGNameString(fullName);
					else
						value = value + "\n" + IssuerAltName.getGNameString(fullName);

				}
				else if (j == 1)
				{
					System.out.println("Not reading CRLDistributionPoints.distributionPoint.reasons");
				}
				else if (j == 2)
				{
					System.out.println("Not reading CRLDistributionPoints.distributionPoint.cRLIssuer");
				}
    		}
    	}
    }

    public String toString()
    {
    	return value;
    }
}

