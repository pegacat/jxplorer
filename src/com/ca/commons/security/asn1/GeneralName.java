
package com.ca.commons.security.asn1;

import java.util.Hashtable;

public class GeneralName
{

    private int generalNameID     = -1;
    private String rfc822Name     = null;		    // GeneralName[1]
    private String dNSName        = null;			// GeneralName[2]
    private Name   directoryName  = null;			// GeneralName[4]
    private String uRID           = null;		    // GeneralName[6]
    private String ipAddress     = null;		    // GeneralName[7]
    private String registeredOID   = null;		    // GeneralName[8]
    
    private static Hashtable idName = new Hashtable();
    
    static
    {
        idName.put(new Integer(1), "rfc822Name");
        idName.put(new Integer(2), "dNSName");
        idName.put(new Integer(4), "directoryName");
        idName.put(new Integer(6), "uRID");
        idName.put(new Integer(7), "ipAddress");
        idName.put(new Integer(8), "registeredOID");
    }
    
    public static String lookUpName(int id)
    {
        return (String) idName.get(new Integer(id));
    }
    
    public GeneralName( Object obj, int type) throws ASN1Exception
    {
    
        if ( (type < 0) || ( type > 8))
            throw new ASN1Exception("Wrong type");
            
        if ( (type == 1) && ( obj instanceof String))
        {
            rfc822Name = (String) obj;
            generalNameID = 1;
        }
        else if ( (type ==2) && ( obj instanceof String))
        {
            dNSName  = (String) obj;
            generalNameID = 2;
        }
        else if ( (type ==4) && ( obj instanceof Name))
        {
            directoryName = ( Name) obj;
            generalNameID = 4;
        }
        else if ( (type == 6) && ( obj instanceof String))
        {
            uRID = (String) obj;
            generalNameID = 6;
        }
        else if ( (type == 7) && ( obj instanceof String))
        {
            ipAddress = (String) obj;
            generalNameID = 7;
        }
        else if ( (type == 8) && ( obj instanceof String))
        {
            registeredOID = (String) obj;
            generalNameID = 8;
        }
        else
        {
            throw new ASN1Exception("Wrong Type");
        }
    }
    
    /**
     * Creates an GeneralName from DER bytes.
     */
    public GeneralName(byte [] data)
    {
        ASN1Object o = ASN1Object.fromBytes(data);
        //	System.out.println(o.toString());
        init(o);
    }
    
    /**
     * Creates an GeneralName from an ASN1object.
     */
    public GeneralName(ASN1Object o)
    {
        init(o);
    }
    
    private void init(ASN1Object ss)
    {
        if (!ss.isASN1Type(ASN1Type.ContextSpecific))
        {
            //	&& ss.implicit()) {
            //throw Exception();
            return;
        }
        
        ASN1Object mm = (ASN1Object) ss.getValue();
        int tag = ss.getTag();
        
        if (tag >= 0 && tag <= 8)
            generalNameID = tag;
        else
            generalNameID = tag - 32; // For constructed Types
            
        if (tag == 0)
        {
            System.out.println("not Implemented");
            
        }
        else if (tag == 0x01)
        {
        
            ASN1Object tmp1 = (ASN1Object)ss.getValue();
            if (tmp1.isASN1Type(ASN1Type.OCTET_STRING) == false)
                return;
                
            byte[] bytearray= (byte[])tmp1.getValue();
            rfc822Name =  new String(bytearray);
            
        }
        else if (tag == 0x2)
        {
        
            ASN1Object tmp1 = (ASN1Object)ss.getValue();
            if (tmp1.isASN1Type(ASN1Type.OCTET_STRING) == false)
                return;
                
            byte[] bytearray= (byte[])tmp1.getValue();
            dNSName = new String(bytearray);
            
        }
        else if (tag == 0x3)
        {
            System.out.println("not Implemented");
            
        }
        else if (tag == ( 0x04 + 0x20))
        { // Constructed Type!!
            directoryName = new Name( (ASN1Object) ss.getValue());
            
        }
        else if (tag == 0x5)
        {
            System.out.println("not Implemented");
            
        }
        else if (tag == 0x6)
        {
        
            ASN1Object tmp1 = (ASN1Object)ss.getValue();
            if (tmp1.isASN1Type(ASN1Type.OCTET_STRING) == false)
                return;
                
            byte[] bytearray= (byte[])tmp1.getValue();
            uRID = (String) new String(bytearray);
            
        }
        else if (tag == 0x7)
        {
        
            ASN1Object tmp1 = (ASN1Object)ss.getValue();
            if (tmp1.isASN1Type(ASN1Type.OCTET_STRING) == false)
                return;
                
            byte[] bytearray= (byte[])tmp1.getValue();
            ipAddress =  new String(bytearray);
            
            
        }
        else if (tag == 0x08)
        {
        
            ASN1Object tmp1 = (ASN1Object)ss.getValue();
            //System.out.println(" OBJECT TYPE:" + tmp1.getASN1Type());
            
            if (tmp1.isASN1Type(ASN1Type.OCTET_STRING) == false)
                return;
                
            try
            {
                byte[] data = (byte[] ) tmp1.getValue();
                ASN1Object obj    = ASN1Object.create(ASN1Type.OBJECT_ID);
                decodeOBJECTID( obj, data, 0, data.length);
                registeredOID     = (String) obj.getValue();
            }
            catch ( Exception e)
            {
                System.out.println(e);
                registeredOID = null;
            }
            
        }
        else
        {
            //throw ASN1Exception("Wrong Type");
            return;
        }
    }
    
    public Object getValue(int type)
    {
    
        if ( type < 0 || type > 8)
            return null;
            
        if ( type ==1)
            return rfc822Name;
        else if ( type ==2)
            return dNSName;
        else if ( type ==4)
            return directoryName;
        else if ( type ==6)
            return uRID;
        else if ( type ==7)
            return ipAddress;
        else if ( type ==8)
            return registeredOID;
            
        return null;
    }
    
    public int getType()
    {
    
        return generalNameID;
    }
    
    
    /**
     * Converts the GeneralName to an ASN1Object.
     */
    public ASN1Object toASN1Object()
    {
        //	try {
        
        if (generalNameID < 0)
        {
            return null;
        }
        
        ASN1Object mm = null;
        
        if (generalNameID == 1 && rfc822Name != null)
        {
        
            try
            {
                mm = new Context(0x01, true,
                                 ASN1Object.create( ASN1Type.IA5String, rfc822Name));
                mm.initByteArray();
            }
            catch ( Exception e)
            {
                System.out.println(e);
            }
            
            return mm;
        }
        else if ( generalNameID == 2 && dNSName != null)
        {
        
            try
            {
                mm = new Context(0x02, true,
                                 ASN1Object.create( ASN1Type.IA5String, dNSName));
                mm.initByteArray();
            }
            catch ( Exception e)
            {
                System.out.println(e);
            }
            return mm;
        }
        else if ( generalNameID == 4 && directoryName != null)
        {
        
            try
            {
                mm = new Context(0x04, false,
                                 directoryName.toASN1Object());
                mm.initByteArray();
            }
            catch ( Exception e)
            {
                System.out.println(e);
            }
            return mm;
        }
        else if ( generalNameID == 6 && uRID != null)
        {
        
            try
            {
                mm = new Context(0x06, true,
                                 ASN1Object.create( ASN1Type.IA5String, uRID));
                mm.initByteArray();
            }
            catch ( Exception e)
            {
                System.out.println(e);
            }
            return mm;
        }
        else if ( generalNameID == 7 && ipAddress != null)
        {
        
            try
            {
                mm = new Context(0x07, true,
                                 ASN1Object.create( ASN1Type.OCTET_STRING,
                                                    ipAddress.getBytes()));
                mm.initByteArray();
            }
            catch ( Exception e)
            {
                System.out.println(e);
            }
            return mm;
        }
        else if ( generalNameID == 8 && registeredOID != null)
        {
        
            try
            {
                mm = new Context(0x08, true,
                                 ASN1Object.create( ASN1Type.OBJECT_ID,
                                                    registeredOID));
                mm.initByteArray();
            }
            catch ( Exception e)
            {
                System.out.println(e);
            }
            return mm;
        }
        
        return null;
    }
    
    private static void decodeOBJECTID(ASN1Object o, byte [] stream, int offset,
                                       int length) throws ASN1Exception
    {
        if (length < 1)
        {	// at least two components
            throw new ASN1Exception("Wrong data (OBJECT ID) length");
        }
        int end = offset + length;
        int v = stream[offset++];
        String content = Integer.toString(v/40) + " ";
        content += Integer.toString(v%40) + " ";
        
        while (offset < end)
        {
            long l =  0;
            while ((stream[offset] & 0x80) != 0)
            {
                l |= 0x7f & stream[offset++];
                l <<= 7;
            }
            l |= 0x7f & stream[offset++];
            content += Long.toString(l) + " ";
        }
        if (offset != end)
        {
            throw new ASN1Exception("Wrong data (OBJECT ID) length");
        }
        o.setValue(content.trim());
    }
    
    
}
