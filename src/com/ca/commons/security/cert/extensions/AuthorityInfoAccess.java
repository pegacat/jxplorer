
package com.ca.commons.security.cert.extensions;

import com.ca.commons.security.asn1.*;

import java.util.Hashtable;

/**
 * <pre>
 * AuthorityInfoAccessSyntax  ::=
 *          SEQUENCE SIZE (1..MAX) OF AccessDescription
 *
 * AccessDescription  ::=  SEQUENCE {
 *          accessMethod          OBJECT IDENTIFIER,
 *          accessLocation        GeneralName  }
 *
 * On-line Certificate Status Protocol (1 3 6 1 5 5 7 48 1)
 * </pre>
 *
 * @author vbui
 */
public class AuthorityInfoAccess implements V3Extension
{
    String value = null;

    static Hashtable acc = new Hashtable();

    static
    {
    	acc.put(ASN1OID.authInfoAccessOCSP, "On-line Certificate Status Protocol");
    }

    public void init(ASN1Object asn1object) throws Exception
    {
		if (!asn1object.isASN1Type(ASN1Type.SEQUENCE))
			throw new Exception("Wrong ASN.1 type for AuthorityInfoAccess");

		for (int i = 0; i < asn1object.size(); i++)
		{
			if (!(asn1object.getComponent(i) instanceof Sequence))
				throw new Exception("AuthorityInfoAccess component is not sequence");

			Sequence seq = (Sequence) asn1object.getComponent(i);
			for (int j = 0; j < seq.size(); j++)
			{
				ASN1Object nextComp = seq.getComponent(j);

				if (j == 0)
				{
					String accMethod = (String)acc.get(nextComp.getValue());
					if (accMethod == null) accMethod = "Unidentified";

					if (value == null)
						value = "Access Method: " + accMethod;
					else
						value = value + "\n" + "Access Method: " + accMethod;
				}
				else if (j == 1)
				{
					if (value == null)
						value = IssuerAltName.getGNameString(nextComp);
					else
						value = value + "\n" + IssuerAltName.getGNameString(nextComp);
				}
			}
		}
    }

	public String toString()
	{
		return value;
	}
}

