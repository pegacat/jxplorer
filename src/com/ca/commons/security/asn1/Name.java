
package com.ca.commons.security.asn1;

import java.util.StringTokenizer;
import java.util.Vector;

/**
 * The definition of a X.500 directory name.
 * For more information, please refer to :
 * CCITT. Recommendation X.500: The Directory - Overview of Concepts,
 * Models and Services. 1988.
 *
 * @author who
 */
public class Name
{
    private String country = null;
    private String state = null;
    private String locality = null;
    private String org = null;
    // support multiple OU
    private Vector orgUnit = new Vector();
    private String commonName = null;
    private String emailAdd = null;
    private String uniqueID = null;
    private String addressIP = null;
    
    /* ASN1Object representation of the Name */
    private ASN1Object name = null;
    
    /**
     * Constructs an empty Name.
     */
    public Name()
    {}
    
    /**
     * Constructs a name given a string with the following format:
     * "C=country, S=state, L=locality, ..."
     */
    public Name(String input)
    {
        StringTokenizer tok  = new StringTokenizer(input, ",");
        while (tok.hasMoreTokens())
        {
            String nextToken = tok.nextToken().trim();
            StringTokenizer fieldTok = new StringTokenizer(nextToken, "=");
            if (fieldTok.countTokens() == 2)
            {
                String name = fieldTok.nextToken().trim();
                String value = fieldTok.nextToken().trim();
                
                if (name.equalsIgnoreCase("C"))
                {
                    if (!value.equalsIgnoreCase("null"))
                        country = value;
                }
                else if (name.equalsIgnoreCase("S"))
                {
                    if (!value.equalsIgnoreCase("null"))
                        state = value;
                }
                else if (name.equalsIgnoreCase("L"))
                {
                    if (!value.equalsIgnoreCase("null"))
                        locality = value;
                }
                else if (name.equalsIgnoreCase("O"))
                {
                    if (!value.equalsIgnoreCase("null"))
                        org = value;
                }
                else if (name.equalsIgnoreCase("OU"))
                {
                    if (!value.equalsIgnoreCase("null"))
                        orgUnit.add(value);
                }
                else if (name.equalsIgnoreCase("CN"))
                {
                    if (!value.equalsIgnoreCase("null"))
                        commonName = value;
                }
                else if (name.equalsIgnoreCase("E"))
                {
                    if (!value.equalsIgnoreCase("null"))
                        emailAdd = value;
                }
                else if (name.equalsIgnoreCase("EM"))
                {
                    if (!value.equalsIgnoreCase("null"))
                        emailAdd = value;
                }
                else if (name.equalsIgnoreCase("Email"))
                {
                    if (!value.equalsIgnoreCase("null"))
                        emailAdd = value;
                }
                else if (name.equalsIgnoreCase("EmailAddress"))
                {
                    if (!value.equalsIgnoreCase("null"))
                        emailAdd = value;
                }
            }
        }
        
        if (!valid())
        {
            throw new IllegalArgumentException("invalid parameters " +
                                               "for Name object.");
        }
    }
    
    /**
     * Constructs a Name with the given attributes.
     * @param	c country code
     * @param	s state or province
     * @param	l locality
     * @param	o organization
     * @param	ou organizational unit
     * @param	cn common name
     * @param	em email address
     */
    public Name(String c, String s, String l, String o, String ou,
                String cn, String em)
    {
        country = c;
        state = s;
        locality = l;
        org = o;
        orgUnit.add(ou);
        commonName = cn;
        emailAdd = em;
        
        if (!valid())
        {
            throw new IllegalArgumentException("invalid parameters " +
                                               "for Name object.");
        }
    }
    
    /**
     * Constructs a Name from an ASN1Object.
     */
    public Name(ASN1Object o)
    {
        name = o;
        init();
        if (name == null)
        {
            throw new IllegalArgumentException("cannot construct a " +
                                               "Name from input ASN1Object");
        }
    }
    
    /**
     * Constructs a Name from a byte array.
     */
    public Name(byte [] input)
    {
        name = ASN1Object.fromBytes(input);
        init();
        if (name == null)
        {
            throw new IllegalArgumentException("cannot construct a " +
                                               "Name from input byte array");
        }
    }
    
    /**
     * Initializes the Name from the ASN1Object. If error, the ASN1Object
     * is reset to null.
     */
    private void init()
    {
        ASN1Object o = name;
        ASN1Object rdn, ava;
        String type, value;
        
        if (o == null || !o.isASN1Type(ASN1Type.SEQUENCE))
        {
            name = null;
            return;
        }
        
        for (int i = 0; i < o.size(); i++)
        {
            rdn = o.getComponent(i);
            if (!rdn.isASN1Type(ASN1Type.SET))
            {
                name = null;
                return;
            }
            
            /* accepted attributes */
            for (int j = 0; j < rdn.size(); j++)
            {
                ava = rdn.getComponent(j);
                type = (String) (ava.getComponent(0).getValue());
                value = (String) (ava.getComponent(1).getValue());
                if (type.equals(ASN1OID.commonName))
                {
                    commonName = value;
                }
                else if (type.equals(ASN1OID.country))
                {
                    country = value;
                }
                else if (type.equals(ASN1OID.stateOrProvince))
                {
                    state = value;
                }
                else if (type.equals(ASN1OID.locality))
                {
                    locality = value;
                }
                else if (type.equals(ASN1OID.organization))
                {
                    org = value;
                }
                else if (type.equals(ASN1OID.organizationalUnit))
                {
                    orgUnit.add(value);
                }
                else if (type.equals(ASN1OID.emailAddress))
                {
                    emailAdd = value;
                }
                else if (type.equals(ASN1OID.unstructuredName))
                {
                    uniqueID = value;
                }
                else if (type.equals(ASN1OID.unstructuredAddress))
                {
                    addressIP = value;
                }
                else
                {			// ignore other attribute
                    //	System.out.println("Unknown attribute");
                    //	System.out.println("type " + type + " value " + value);
                }
            }
        }
        
        /* the essential attributes cannot all be null */
        if (!valid())
        {
            name = null;
        }
    }
    
    /**
     * Creates the ASN1Object with respective attibutes. If error,
     * the name object is set to null.
     */
    private void createName()
    {
        if (!valid())
        {
            name = null;
            return;
        }
        
        try
        {
            name = ASN1Object.create(ASN1Type.SEQUENCE);
            if (country != null)
            {
                name.addComponent(createRDN(ASN1OID.country, country));
            }
            if (state != null)
            {
                name.addComponent(createRDN(ASN1OID.stateOrProvince, state));
            }
            if (locality != null)
            {
                name.addComponent(createRDN(ASN1OID.locality, locality));
            }
            if (org != null)
            {
                name.addComponent(createRDN(ASN1OID.organization, org));
            }
            for (int i = 0; i < orgUnit.size(); i++)
            {
                String myou = (String) orgUnit.elementAt(i);
                if (myou != null && myou.trim().length() > 0)
                    name.addComponent(createRDN(
                                          ASN1OID.organizationalUnit, myou));
            }
            if (emailAdd != null)
            {
                name.addComponent(createEmail(ASN1OID.emailAddress, emailAdd));
            }
            if (commonName != null)
            {
                name.addComponent(createRDN(ASN1OID.commonName, commonName));
            }
            if (uniqueID != null)
            {
                name.addComponent(createEmail(
                                      ASN1OID.unstructuredName, uniqueID));
            }
            if (addressIP != null)
            {
                name.addComponent(createRDN(
                                      ASN1OID.unstructuredAddress, addressIP));
            }
        }
        catch(ASN1Exception asn1e)
        {
            name = null;
            return;
        }
        name.initByteArray();
    }
    
    /**
     * Creates a relative distinguished name while the value is an
     * ASN.1 PrintableString.
     */
    private ASN1Object createRDN(String t, String v)
    throws ASN1Exception
    {
        ASN1Object rdn, ava, type, value;
        
        rdn = ASN1Object.create(ASN1Type.SET);
        ava = ASN1Object.create(ASN1Type.SEQUENCE);
        type = ASN1Object.create(ASN1Type.OBJECT_ID, t);
        value = ASN1Object.create(ASN1Type.PrintableString, v);
        ava.addComponent(type);
        ava.addComponent(value);
        rdn.addComponent(ava);
        return rdn;
    }
    
    /**
     * Creates a relative distinguished name while the value is an
     * ASN.1 IA5String.
     */
    private ASN1Object createEmail(String t, String v)
    throws ASN1Exception
    {
        ASN1Object rdn, ava, type, value;
        
        rdn = ASN1Object.create(ASN1Type.SET);
        ava = ASN1Object.create(ASN1Type.SEQUENCE);
        type = ASN1Object.create(ASN1Type.OBJECT_ID, t);
        value = ASN1Object.create(ASN1Type.IA5String, v);
        ava.addComponent(type);
        ava.addComponent(value);
        rdn.addComponent(ava);
        return rdn;
    }
    
    /**
     * Converts the Name to an ASN1Object.
     */
    public ASN1Object toASN1Object()
    {
        if (name == null)
        {
            createName();
        }
        return name;
    }
    
    /**
     * Gets the DER encoded byte array of the Name.
     */
    public byte [] toByteArrayDER()
    {
        if (name == null)
        {
            createName();
        }
        if (name == null)
        {
            return null;
        }
        return name.toDERBytes();
    }
    
    /* the following methods retrieve each attribute */
    
    /**
     * Gets the country code.
     */
    public String country()
    {
        return country;
    }
    
    /**
     * Gets the state or province name.
     */
    public String stateOrProvince()
    {
        return state;
    }
    
    /**
     * Gets the locality.
     */
    public String locality()
    {
        return locality;
    }
    
    /**
     * Gets the organization.
     */
    public String organization()
    {
        return org;
    }
    
    /**
     * Gets the organizational unit.
     */
    public String organizationalUnit()
    {
        if (orgUnit.size() >= 1)
            return (String) orgUnit.elementAt(0);
        else
            return null;
    }
    
    /**
     * Gets the organizational units.
     */
    public Vector organizationalUnits()
    {
        return orgUnit;
    }
    
    /**
     * Gets the common name.
     */
    public String commonName()
    {
        if (commonName != null && commonName.trim().length() != 0)
            return commonName;
        else
            return toString();
    }
    
    /**
     * Gets the email address.
     */
    public String emailAddress()
    {
        return emailAdd;
    }
    
    /**
     * Gets the unique identifier.
     */
    public String uniqueID()
    {
        return uniqueID;
    }
    
    /**
     * Gets the IP address.
     */
    public String addressIP()
    {
        return addressIP;
    }
    
    /**
     * Sets the name to be the same as 'o'.
     */
    public void set(Name o)
    {
        this.country = o.country;
        this.state = o.state;
        this.locality = o.locality;
        this.org = o.org;
        this.orgUnit = o.orgUnit;
        this.commonName = o.commonName;
        this.emailAdd = o.emailAdd;
        this.uniqueID = o.uniqueID;
        this.addressIP = o.addressIP;
        this.name = o.name;
    }
    
    /**
     * Tests whether this Name is the same as the given object.
     */
    public boolean equals(Object n)
    {
        if (!(n instanceof Name))
        {
            return false;
        }
        Name o = (Name) n;
        return this.toASN1Object().equals(o.toASN1Object());
    }
    
    /**
     * Checks the validity of the Name. The essential attributes cannot
     * be all nulls.
     */
    public boolean valid()
    {
        if ((country == null || country.length() == 0)
                && (state == null || state.length() == 0)
                && (locality == null || locality.length() == 0)
                && (org == null || org.length() == 0)
                && (orgUnit == null || orgUnit.size() == 0)
                && (commonName == null || commonName.length() == 0)
                && (emailAdd == null || emailAdd.length() == 0))
        {
            return false;
        }
        return true;
    }
    
    /**
     * A string description of the Name.
     */
    public String toString()
    {
        String result = null;
        
        if (country != null && !country.equals("null") && country.trim().length() > 0)
            result = "C = " + country;
        if (state != null && !state.equals("null") && state.trim().length() > 0)
            result += ", ST = " + state;
        if (locality != null && !locality.equals("null") && locality.trim().length() > 0)
            result += ", L = " + locality;
        if (org != null && !org.equals("null") && org.trim().length() > 0)
            result += ", O = " + org;
        for (int i = 0; i < orgUnit.size(); i++)
        {
            String anOrgUnit = (String) orgUnit.elementAt(i);
            if (anOrgUnit != null && !anOrgUnit.equals("null") && anOrgUnit.trim().length() > 0)
                result += ", OU = " + anOrgUnit;
        }
        if (commonName != null && !commonName.equals("null") && commonName.trim().length() > 0)
            result += ", CN = " + commonName;
        if (emailAdd != null && !emailAdd.equals("null") && emailAdd.trim().length() > 0)
            result += ", EM = " + emailAdd;
        if (uniqueID != null && !uniqueID.equals("null") && uniqueID.trim().length() > 0)
            result += ", unstructuredName = " + uniqueID;
        if (addressIP != null && !addressIP.equals("null") && addressIP.trim().length() > 0)
            result += ", unstructuredAddress = " + addressIP;
            
        return result;
    }
}
