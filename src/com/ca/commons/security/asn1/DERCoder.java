
package com.ca.commons.security.asn1;

import java.math.BigInteger;
import java.util.StringTokenizer;

/**
 * This class provides the facilities of encoding and decoding ASN1Objects
 * using Distinguished Encoding Rules (DER). The class is only used in this
 * package.
 */
public class DERCoder implements java.io.Serializable
{

    /**
     * Default constructor.
     */
    public DERCoder()
    {}
    
    /********************************/
    /*		DER Encoding			*/
    /********************************/
    
    /**
     * DER encoding -- encodes an ASN1Object to a byte array
     * @param	input the ASN1Object to be encoded
     * @return	the encoded byte array
     * @exception iss.security.asn1.ASN1Exception if any error
     * occurred in the encoding process
     */
    public byte [] encode(ASN1Object input) throws ASN1Exception
    {
        int length;
        byte [] buffer = new byte[8192];
        try
        {
            length = encode(input, buffer, 0);
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            throw new ASN1Exception("ASN.1 Object too big");
        }
        byte [] result = new byte[length];
        for (int i = 0; i < length; i++)
        {
            result[length-i-1] = buffer[i];
        }
        return result;
    }
    
    /**
     * DER encoding -- encodes an ASN1Object to a byte array
     * @param	input the ASN1Object to be encoded
     * @param	buffer the buffer to hold the encoded byte array
     * @param	offset the starting index of the buffer
     * @return	the length of the encoded bytes
     * @exception iss.security.asn1.ASN1Exception if any error
     * occurred in the encoding process
     */
    public int encode(ASN1Object input, byte [] buffer, int offset)
    throws ASN1Exception
    {
        /* call different method for different ASN.1 type */
        int tag = input.getASN1Type().getTag();
        switch (tag)
        {
        case 1:
            return encodeBOOLEAN(input, buffer, offset);
        case 2:
            return encodeINTEGER(input, buffer, offset);
        case 3:
            return encodeBITSTRING(input, buffer, offset);
        case 4:
            return encodeOCTETSTRING(input, buffer, offset);
        case 5:
            return encodeNULL(input, buffer, offset);
        case 6:
            return encodeOBJECTID(input, buffer, offset);
        case 10:
            return encodeENUMERATED(input,buffer,offset);
        case 19:
            return encodePrintableString(input, buffer, offset);
        case 20:
            //		case 20: return encodeT61String(input, buffer, offset);
        case 22:
            return encodeIA5String(input, buffer, offset);
        case 23:
            return encodeUTCTime(input, buffer, offset);
        case 24:
            return encodeGeneralizedTime(input, buffer, offset);
        case 28:
            return encodeUniversalString(input, buffer, offset);
        case 30:
            return encodeBMPSTRING(input, buffer, offset);
        case 48:
        case 49:
            return encodeSEQUENCE((Sequence)input, buffer, offset);
            //		case 49: return encodeSET(input, buffer, offset);
        case 128:
            return encodeCONTEXT(input, buffer, offset);
        }
        throw new ASN1Exception(input.getASN1Type().toString() +
                                " -- Unknown type");
    }
    
    /**
     * Encodes the length.
     */
    private int encodeLength(int length, byte [] stream, int offset)
    {
        if (length >= 0 && length <= 127)
        {
            stream[offset++] = (byte) length;
        }
        else
        {
            int count = 0;
            while (length != 0)
            {
                stream[offset++] = (byte) length;
                length >>>= 8;
                count++;
            }
            stream[offset++] = (byte) ((count)|0x80);
        }
        return offset;
    }
    
    /**
     * Encodes BOOLEAN.
     */
    private int encodeBOOLEAN(ASN1Object o, byte [] stream, int offset)
    {
        boolean v = ((Boolean) o.getValue()).booleanValue();
        stream[offset++] = (byte) (v?1:0);
        stream[offset++] = 1;
        stream[offset++] = (byte) o.getASN1Type().getTag();
        return offset;
    }
    
    /**
     * Encodes INTEGER.
     */
    private int encodeINTEGER(ASN1Object o, byte [] stream, int offset)
    {
        BigInteger v = (BigInteger) (o.getValue());
        byte [] content = v.toByteArray();
        for (int i = content.length - 1; i >= 0; i--)
        {
            stream[offset++] = content[i];
        }
        offset = encodeLength(content.length, stream, offset);
        stream[offset++] = (byte) o.getASN1Type().getTag();
        return offset;
    }
    
    /**
    * Encodes ENUMERATED
    */
    private int encodeENUMERATED(ASN1Object o, byte [] stream, int offset)
    {
        BigInteger v = (BigInteger) (o.getValue());
        byte [] content = v.toByteArray();
        for (int i = content.length - 1; i >= 0; i--)
        {
            stream[offset++] = content[i];
        }
        offset = encodeLength(content.length, stream, offset);
        stream[offset++] = (byte) o.getASN1Type().getTag();
        return offset;
    }
    
    /**
     * Encodes SEQUENCE.
     */
    private int encodeSEQUENCE(ASN1Object o, byte [] stream, int offset)
    throws ASN1Exception
    {
        int start = offset;
        for (int i = o.size() - 1; i >= 0; i--)
        {
            offset = encode(o.getComponent(i), stream, offset);
        }
        offset = encodeLength(offset - start, stream, offset);
        stream[offset++] = (byte) (o.getASN1Type().getTag());
        return offset;
    }
    
    /**
     * Encodes BITSTRING.
     */
    private int encodeBITSTRING(ASN1Object o, byte [] stream, int offset)
    {
        byte [] content = (byte []) (o.getValue());
        for (int i = content.length - 1; i >= 0; i--)
        {
            stream[offset++] = content[i];
        }
        stream[offset++] = 0;
        offset = encodeLength(content.length + 1, stream, offset);
        stream[offset++] = (byte) (o.getASN1Type().getTag());
        return offset;
    }
    
    /**
     * Encodes OCTETSTRING.
     */
    private int encodeOCTETSTRING(ASN1Object o, byte [] stream, int offset)
    {
        byte [] content = (byte []) (o.getValue());
        for (int i = content.length - 1; i >= 0;i--)
        {
            stream[offset++] = content[i];
        }
        offset = encodeLength(content.length, stream, offset);
        stream[offset++] = (byte) (o.getASN1Type().getTag());
        return offset;
    }
    
    /**
     * Encodes UniversalString.
     */
    private int encodeUniversalString(ASN1Object o, byte [] stream, int offset)
    {
        byte [] content = (byte []) (o.getValue());
        for (int i = content.length - 1; i >= 0;i--)
        {
            stream[offset++] = content[i];
        }
        offset = encodeLength(content.length, stream, offset);
        stream[offset++] = (byte) (o.getASN1Type().getTag());
        return offset;
    }
    
    /**
     * Encodes BMPSTRING.
     */
    private int encodeBMPSTRING(ASN1Object o, byte [] stream, int offset)
    {
        byte [] content = (byte []) (o.getValue());
        for (int i = content.length - 1; i >= 0;i--)
        {
            stream[offset++] = content[i];
        }
        offset = encodeLength(content.length, stream, offset);
        stream[offset++] = (byte) (o.getASN1Type().getTag());
        return offset;
    }
    
    /**
     * Encodes OBJECT IDENTIFIER. OID is represented as string of fields
     * seperated by space.
     */
    private int encodeOBJECTID(ASN1Object o, byte [] stream, int offset)
    {
        String id = (String) (o.getValue());
        StringTokenizer st = new StringTokenizer(id, " ");
        String [] subid = new String[st.countTokens()];
        int start = offset;
        
        for (int i = 0; i < subid.length; i++)
        {
            subid[i] = st.nextToken();
        }
        for (int i = subid.length - 1; i > 1; i--)
        {
            long s = Long.parseLong(subid[i]);
            stream[offset++] = (byte) (s & 0x7f);
            s >>>= 7;
            while (s != 0)
            {
                stream[offset++] = (byte) (s | 0x80);
                s >>>= 7;
            }
        }
        
        long l = Long.parseLong(subid[0]) * 40 + Long.parseLong(subid[1]);
        stream[offset++] = (byte) l;
        offset = encodeLength(offset - start, stream, offset);
        stream[offset++] = (byte) o.getASN1Type().getTag();
        return offset;
    }
    
    /**
     * Encodes UTCTime.
     */
    private int encodeUTCTime(ASN1Object o, byte [] stream, int offset)
    {
        String time = (String) (o.getValue());
        byte [] t = time.getBytes();
        for (int i = t.length - 1; i >= 0; i--)
        {
            stream[offset++] = t[i];
        }
        offset = encodeLength(t.length, stream, offset);
        stream[offset++] = (byte) o.getASN1Type().getTag();
        return offset;
    }
    
    /**
     * Encodes GeneralizedTime.
     */
    private int encodeGeneralizedTime(ASN1Object o, byte[] stream, int offset)
    {
    
        String time = (String) (o.getValue());
        byte [] t = time.getBytes();
        for (int i = t.length - 1; i >= 0; i--)
        {
            stream[offset++] = t[i];
        }
        offset = encodeLength(t.length, stream, offset);
        stream[offset++] = (byte) o.getASN1Type().getTag();
        return offset;
    }
    
    /**
     * Encodes PrintableString.
     */
    private int encodePrintableString(ASN1Object o, byte [] stream,
                                      int offset)
    {
        String s = (String) (o.getValue());
        byte [] content = s.getBytes();
        for (int i = content.length - 1; i >= 0; i--)
        {
            stream[offset++] = content[i];
        }
        offset = encodeLength(content.length, stream, offset);
        stream[offset++] = (byte) o.getASN1Type().getTag();
        return offset;
    }
    
    /**
     * Encodes IA5String.
     */
    private int encodeIA5String(ASN1Object o, byte [] stream,
                                int offset)
    {
        String s = (String) (o.getValue());
        byte [] content = s.getBytes();
        for (int i = content.length - 1; i >= 0; i--)
        {
            stream[offset++] = content[i];
        }
        offset = encodeLength(content.length, stream, offset);
        stream[offset++] = (byte) o.getASN1Type().getTag();
        return offset;
    }
    
    /**
     * Encodes NULL.
     */
    private int encodeNULL(ASN1Object o, byte [] stream, int offset)
    {
        stream[offset++] = 0;
        stream[offset++] = (byte) o.getASN1Type().getTag();
        return offset;
    }
    
    /**
     * Encodes ContextSpecific.
     */
    private int encodeCONTEXT(ASN1Object o, byte [] stream, int offset)
    throws ASN1Exception
    {
    
        ASN1Object v = (ASN1Object) (o.getValue());
        int start = offset;
        
        //System.out.println("TYPE:" + v.getASN1Type().getName());
        
        if (o.implicit())
        {
        
            if ((o.getTag() & 0x20) == 0)
            {
            
                byte[] data = null;
                
                if ( v.getASN1Type().equals(ASN1Type.BOOLEAN))
                {
                
                    boolean b = ((Boolean) v.getValue()).booleanValue();
                    stream[offset++] = (byte) (b?1:0);
                    stream[offset++] = 1;
                }
                
                if ( v.getASN1Type().equals(ASN1Type.OCTET_STRING))
                {
                
                    data = (byte []) (v.getValue());
                    
                    for (int i = data.length - 1; i >= 0; i--)
                    {
                        stream[offset++] = data[i];
                    }
                    offset = encodeLength(data.length, stream, offset);
                }
                
                if ( v.getASN1Type().equals(ASN1Type.IA5String))
                {
                
                    String s = (String) (v.getValue());
                    data = s.getBytes();
                    for (int i = data.length - 1; i >= 0; i--)
                    {
                        stream[offset++] = data[i];
                    }
                    offset = encodeLength(data.length, stream, offset);
                }
                
                if ( v.getASN1Type().equals(ASN1Type.INTEGER))
                {
                    BigInteger tmp = (BigInteger) v.getValue();
                    data =  tmp.toByteArray();
                    
                    for (int i = data.length - 1; i >= 0; i--)
                    {
                        stream[offset++] = data[i];
                    }
                    offset = encodeLength(data.length, stream, offset);
                }
                
                if (v.getASN1Type().equals(ASN1Type.UTCTime))
                {
                
                    String time = (String) (v.getValue());
                    //System.out.println("time:" + time);
                    
                    byte [] t = time.getBytes();
                    for (int i = t.length - 1; i >= 0; i--)
                    {
                        stream[offset++] = t[i];
                    }
                    
                    offset = encodeLength(t.length, stream, offset);
                } // End of IF ... UTCTime
                
                if ( v.getASN1Type().equals(ASN1Type.GENERALIZEDTIME))
                {
                
                    String time = (String) (v.getValue());
                    //System.out.println("time:" + time);
                    
                    byte [] t = time.getBytes();
                    for (int i = t.length - 1; i >= 0; i--)
                    {
                        stream[offset++] = t[i];
                    }
                    
                    offset = encodeLength(t.length, stream, offset);
                }
                
                if ( v.getASN1Type().equals(ASN1Type.BIT_STRING))
                {
                
                    byte [] content = (byte []) (v.getValue());
                    for (int i = content.length - 1; i >= 0; i--)
                    {
                        stream[offset++] = content[i];
                    }
                    stream[offset++] = 0;
                    offset = encodeLength(content.length + 1, stream, offset);
                }
                
                if ( v.getASN1Type().equals(ASN1Type.ENUMERATED))
                {
                    BigInteger tmp = (BigInteger) v.getValue();
                    data =  tmp.toByteArray();
                    
                    for (int i = data.length - 1; i >= 0; i--)
                    {
                        stream[offset++] = data[i];
                    }
                    offset = encodeLength(data.length, stream, offset);
                }
                
                if ( v.getASN1Type().equals(ASN1Type.OBJECT_ID))
                {
                
                    String id = (String) (v.getValue());
                    StringTokenizer st = new StringTokenizer(id, " ");
                    String [] subid = new String[st.countTokens()];
                    //start = offset;
                    
                    for (int i = 0; i < subid.length; i++)
                    {
                        subid[i] = st.nextToken();
                    }
                    for (int i = subid.length - 1; i > 1; i--)
                    {
                        long s = Long.parseLong(subid[i]);
                        stream[offset++] = (byte) (s & 0x7f);
                        s >>>= 7;
                        
                        while (s != 0)
                        {
                            stream[offset++] = (byte) (s | 0x80);
                            s >>>= 7;
                        }
                    }
                    
                    long l = Long.parseLong(subid[0]) * 40 + Long.parseLong(subid[1]);
                    stream[offset++] = (byte) l;
                    offset = encodeLength(offset - start, stream, offset);
                    //stream[offset++] = (byte) o.getASN1Type().getTag();
                    //return offset;
                }
                
                if ( (v.getASN1Type().equals(ASN1Type.SEQUENCE)== true) ||
                        ( v.getASN1Type().equals(ASN1Type.SET) == true) )
                {
                
                    start = offset;
                    //System.out.println("SIZE:" + v.size());
                    for (int i = v.size() - 1; i >= 0; i--)
                    {
                        offset = encode(v.getComponent(i), stream, offset);
                    }
                    
                    offset = encodeLength(offset - start, stream, offset);
                    stream[offset++] = (byte) (0xa0 | o.getTag());
                    return offset;
                } // End of IF ... sequence
                
                stream[offset++] = (byte) (0x80 | o.getTag()); // For all other types
                
            }
            else
            {
            
                offset = encode(v, stream, offset);
                stream[offset - 1] = (byte) (0xa0 | o.getTag());
            }
        }
        else
        {
        
            offset = encode(v, stream, offset);
            offset = encodeLength(offset - start, stream, offset);
            stream[offset++] = (byte) (0xa0 | o.getTag());
        }
        return offset;
    }
    
    
    /********************************/
    /*		DER Decoding			*/
    /********************************/
    
    /**
     * DER decoding -- decodes a byte array to an ASN1Object
     * @param	buffer the byte array to be decoded
     * @return	the decoded ASN1Object
     * @exception iss.security.asn1.ASN1Exception if any error
     * occurred in the decoding process
     */
    public ASN1Object decode(byte [] buffer)
    throws ASN1Exception
    {
        int [] offset = new int [1];
        offset[0] = 0;
        return decode(buffer, offset);
    }
    
    /**
     * DER decoding -- decodes a byte array to an ASN1Object
     * @param	buffer the byte array to be decoded
     * @param	offset the starting index of the buffer
     * @return	the decoded ASN1Object
     * @exception iss.security.asn1.ASN1Exception if any error
     * occurred in the decoding process
     */
    public ASN1Object decode(byte [] buffer, int [] offset)
    throws ASN1Exception
    {
        int start = offset[0];
        int tag = buffer[offset[0]++];
        int oldTag = 0;
        if ((tag & 0x80) != 0 && (tag & 0x40) == 0)
        {// context-specific tag
            oldTag = tag;
            tag = 0x80;
        }
        
        ASN1Type type = new ASN1Type(tag);
        ASN1Object object = ASN1Object.create(type);
        int length = decodeLength(buffer, offset);
        if (length < 0 && length != -1)
        {
            throw new ASN1Exception(
                "Invalid object length (" + length + ")");
        }
        switch (tag)
        {
        case 1:
            decodeBOOLEAN(object, buffer, offset, length);
            break;
        case 2:
            decodeINTEGER(object, buffer, offset, length);
            break;
        case 3:
            decodeBITSTRING(object, buffer, offset, length);
            break;
        case 4:
            decodeOCTETSTRING(object, buffer, offset, length);
            break;
        case 5:
            decodeNULL(object, buffer, offset, length);
            break;
        case 6:
            decodeOBJECTID(object, buffer, offset, length);
            break;
        case 10:
            decodeENUMERATED(object, buffer, offset, length);
            break;
        case 19:
            decodePrintableString(object, buffer, offset, length);
            break;
        case 20:
            //		case 20: decodeT61String(object, buffer, offset, length);
            //				break;
        case 22:
            decodeIA5String(object, buffer, offset, length);
            break;
        case 23:
            decodeUTCTime(object, buffer, offset, length);
            break;
        case 24:
            decodeGeneralizedTime(object, buffer, offset, length);
            break;
        case 28:
            decodeUniversalString(object, buffer, offset, length);
            break;
        case 30:
            decodeBMPSTRING(object, buffer, offset, length);
            break;
        case 48:
        case 49:
            decodeSEQUENCE(object, buffer, offset, length);
            break;
            //		case 49: decodeSET(object, buffer, offset, length);
            //				break;
        case 128:
            decodeCONTEXT(object, buffer, offset, length, oldTag);
            break;
        default:
            throw new ASN1Exception("Unknow ASN.1 tag --" + tag);
        }
        
        //	offset[0] = offset[0] + length;
        byte [] der = new byte[offset[0]-start];
        System.arraycopy(buffer, start, der, 0, offset[0] - start);
        object.setByteArray(der);
        return object;
    }
    
    /**
     * Decodes the length.
     */
    private int decodeLength(byte [] stream, int [] offset)
    throws ASN1Exception
    {
        int length = stream[offset[0]++] & 0xff;
        if ((length & 0x80) != 0)
        {
            int count = length & 0x7f;
            if (count > 4)
            {
                throw new ASN1Exception("ASN.1 Object too large");
            }
            if (count == 0)
            { // indefinite length
                return -1;
            }
            length = stream[offset[0]++] & 0xff;
            for (int i = 1; i < count; i++)
            {
                length <<= 8;
                length |= (stream[offset[0]++] & 0xff);
            }
        }
        return length;
    }
    
    /**
     * Decodes BOOLEAN. Definite-length only.
     */
    private void decodeBOOLEAN(ASN1Object o, byte [] stream, int [] offset,
                               int length) throws ASN1Exception
    {
        if (length != 1)
        {
            throw new ASN1Exception("Wrong data (BOOLEAN) length");
        }
        boolean v = (stream[offset[0]] == 0) ? false : true;
        o.setValue(new Boolean(v));
        offset[0]++;
    }
    
    /**
     * Decodes INTEGER. Definite-length only.
     */
    private void decodeINTEGER(ASN1Object o, byte [] stream, int [] offset,
                               int length) throws ASN1Exception
    {
        if (length < 1)
        {
            throw new ASN1Exception("Wrong data (INTEGER) length");
        }
        byte [] content = new byte[length];
        System.arraycopy(stream, offset[0], content, 0, length);
        BigInteger v = new BigInteger(content);
        o.setValue(v);
        offset[0] += length;
    }
    
    /**
     * Decodes ENUMERATED. Definite-length only.
     */
    private void decodeENUMERATED(ASN1Object o, byte [] stream, int [] offset,
                                  int length) throws ASN1Exception
    {
        if (length < 1)
        {
            throw new ASN1Exception("Wrong data (INTEGER) length");
        }
        byte [] content = new byte[length];
        System.arraycopy(stream, offset[0], content, 0, length);
        BigInteger v = new BigInteger(content);
        o.setValue(v);
        offset[0] += length;
    }
    
    /**
     * Decodes SEQUENCE.
     */
    private void decodeSEQUENCE(ASN1Object o, byte [] stream, int [] offset,
                                int length) throws ASN1Exception
    {
        int start = offset[0];
        if (length == -1)
        { // indefinite length
            while (true)
            {
                if (stream[offset[0]] == 0
                        && stream[offset[0]+1] == 0)
                {
                    offset[0] += 2;
                    break;
                }
                o.addComponent(decode(stream, offset));
            }
        }
        else
        {
            while (offset[0] < start + length)
            {
                o.addComponent(decode(stream, offset));
            }
            if (offset[0] != start + length)
            {
                throw new ASN1Exception("Wrong data (SEQUENCE) length");
            }
        }
    }
    
    /**
     * Decodes BITSTRING. Assume definite-length only.
     */
    private void decodeBITSTRING(ASN1Object o, byte [] stream, int [] offset,
                                 int length) throws ASN1Exception
    {
        if (length == -1)
        {
            // not implemented
        }
        else
        {
            if (length < 1)
            {
                throw new ASN1Exception("Wrong data (BIT STRING) length");
            }
            byte [] content = new byte[length];
            System.arraycopy(stream, offset[0], content, 0, length);
            o.setValue(content);
            offset[0] += length;
        }
    }
    
    /**
     * Decodes OCTETSTRING. Assume definite-length only.
     */
    private void decodeOCTETSTRING(ASN1Object o, byte [] stream,
                                   int [] offset, int length)
    {
        if (length == -1)
        {
            // not implemented
        }
        else
        {
            byte [] content = new byte[length];
            System.arraycopy(stream, offset[0], content, 0, length);
            o.setValue(content);
            offset[0] += length;
        }
    }
    
    /**
    * Decodes UniversalSTRING. Assume definite-length only.
    */
    private void decodeUniversalString(ASN1Object o, byte [] stream,
                                       int [] offset, int length)
    {
        if (length == -1)
        {
            // not implemented
        }
        else
        {
            byte [] content = new byte[length];
            System.arraycopy(stream, offset[0], content, 0, length);
            o.setValue(content);
            offset[0] += length;
        }
    }
    
    /**
    * Decodes BMPSTRING. Assume definite-length only.
    */
    private void decodeBMPSTRING(ASN1Object o, byte [] stream,
                                 int [] offset, int length)
    {
        if (length == -1)
        {
            // not implemented
        }
        else
        {
            byte [] content = new byte[length];
            System.arraycopy(stream, offset[0], content, 0, length);
            o.setValue(content);
            offset[0] += length;
        }
    }
    
    /**
     * Decodes OBJECT IDENTIFIER. Definite-length only.
     */
    private void decodeOBJECTID(ASN1Object o, byte [] stream, int [] offset,
                                int length) throws ASN1Exception
    {
        if (length < 1)
        {	// at least two components
            throw new ASN1Exception("Wrong data (OBJECT ID) length");
        }
        int end = offset[0] + length;
        int v = stream[offset[0]++];
        String content = Integer.toString(v/40) + " ";
        content += Integer.toString(v%40) + " ";
        
        while (offset[0] < end)
        {
            long l =  0;
            while ((stream[offset[0]] & 0x80) != 0)
            {
                l |= 0x7f & stream[offset[0]++];
                l <<= 7;
            }
            l |= 0x7f & stream[offset[0]++];
            content += Long.toString(l) + " ";
        }
        if (offset[0] != end)
        {
            throw new ASN1Exception("Wrong data (OBJECT ID) length");
        }
        o.setValue(content.trim());
    }
    
    /**
     * Decodes UTCTime. Assume definite-length only.
     */
    private void decodeUTCTime(ASN1Object o, byte [] stream, int [] offset,
                               int length) throws ASN1Exception
    {
        if (length == -1)
        {
            // not implemented
        }
        else
        {
            if (length < 11)
            {		// at least 11 ASCII characters
                throw new ASN1Exception("Wrong data (UTCTime) length");
            }
            o.setValue(new String(stream, offset[0], length));
            offset[0] += length;
        }
    }
    
    /**
     * Decodes GeneralizedTime. Assume definite-length only.
     */
    private void decodeGeneralizedTime(ASN1Object o, byte [] stream, int [] offset,
                                       int length) throws ASN1Exception
    {
        if (length == -1)
        {
            // not implemented
        }
        else
        {
            if (length < 11)
            {		// at least 13 ASCII characters
                throw new ASN1Exception("Wrong data (UTCTime) length");
            }
            o.setValue(new String(stream, offset[0], length));
            offset[0] += length;
        }
    }
    
    /**
     * Decodes PrintableString. Assume definite-length only.
     */
    private void decodePrintableString(ASN1Object o, byte [] stream,
                                       int [] offset, int length)
    {
        if (length == -1)
        {
            // not implemented
        }
        else
        {
            o.setValue(new String(stream, offset[0], length));
            offset[0] += length;
        }
    }
    
    /**
     * Decodes IA5String. Assume definite-length only.
     */
    private void decodeIA5String(ASN1Object o, byte [] stream,
                                 int [] offset, int length)
    {
        if (length == -1)
        {
            // not implemented
        }
        else
        {
            o.setValue(new String(stream, offset[0], length));
            offset[0] += length;
        }
    }
    
    /**
     * Decodes NULL. Definite-length only.
     */
    private void decodeNULL(ASN1Object o, byte [] stream, int [] offset,
                            int length) throws ASN1Exception
    {
        if (length != 0)
        {
            throw new ASN1Exception("Wrong data (NULL) length");
        }
        o.setValue("");
    }
    
    /**
     * Decodes ContextSpecific.
     */
    private void decodeCONTEXT(ASN1Object o, byte [] stream, int [] offset,
                               int length, int tag) throws ASN1Exception
    {
        Context obj = null;
        int start = offset[0];
        tag = tag & 0x3f;
        
        if ((tag & 0x20) == 0)
        { //primitive type
            if (length < 0)
            {
                throw new ASN1Exception("Wrong length(ContextSpecific)");
            }
            byte [] data = new byte[length];
            System.arraycopy(stream, offset[0], data, 0, length);
            obj = new Context(tag, true,
                              ASN1Object.create(ASN1Type.OCTET_STRING, data));
            offset[0] += length;
        }
        else
        {
            try
            {
                ASN1Object v = decode(stream, offset);
                if (length == -1)
                {
                    if (stream[offset[0]] == 0
                            && stream[offset[0]+1] == 0)
                    {
                        offset[0] += 2;
                    }
                    else
                    {
                        //	System.out.println(offset[0]);
                        throw new ASN1Exception(
                            "wrong indefinite-length decoding");
                    }
                }
                else
                {
                    if (offset[0] != start + length)
                    {
                        throw new ASN1Exception(
                            "wrong definite-length decoding");
                    }
                }
                obj = new Context(tag, false, v);
            }
            catch(ASN1Exception e)
            {
                //	e.printStackTrace(System.out);
                offset[0] = start;
                ASN1Object v = ASN1Object.create(ASN1Type.SEQUENCE);
                decodeSEQUENCE(v, stream, offset, length);
                obj = new Context(tag, true, v);
            }
        }
        o.setValue(obj);
    }
}
