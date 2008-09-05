
package com.ca.commons.security.asn1;

import com.ca.commons.cbutil.CBBase64;

import java.io.*;

/**
 * This class provides some basic methods to convert ASN1Objects
 * from/to byte arrays and files. The byte arrays can be base64 .
 * encoded. The files can be in Privacy Enhanced Mail (PEM) format.
 * They are all assumed not to be encrypted.
 *
 * @author who
 */
public class ASN1Util implements java.io.Serializable
{

    private static DERCoder derCoder = new DERCoder();

    /**
     * Creates an ASN1Object from a byte array. The byte array can
     * be base64 encoded or not.
     * @exception	iss.security.asn1.ASN1Exception if creating the
     * ASN1Obect from the byte array throws this exception.
     */
    public static ASN1Object fromByteArray(byte [] content)
    throws ASN1Exception
    {
        if (content == null || content.length == 0)
        {
            return null;
        }

        ASN1Object object = null;
        byte [] byteArray;

        /* test whether base64 encoded */
        byte [] base64Decoded = CBBase64.decode(content);
        if (base64Decoded != null)
        {
            byteArray = base64Decoded;
        }
        else
        {
            byteArray = content;
        }

        object = derCoder.decode(byteArray);
        //	object.setByteArray(byteArray);
        return object;
    }

    /**
     * Creates an ASN1Object from a file, while the file may be in binary
     * form or in PEM format.
     * @exception	iss.security.asn1.ASN1Exception if creating the
     * ASN1Object from the file throws this exception.
     */
    public static ASN1Object fromFile(File f) throws IOException, ASN1Exception
    {
        BufferedReader reader = new BufferedReader(new FileReader(f));
        byte [] buffer = new byte[(int) (f.length())];

        /* checks the PEM file beginning identifier */
        String s = null;
        while ((s = reader.readLine()) != null
                && !(s.startsWith("-----BEGIN")))
            ;

        /* file is in PEM format, need to do base64 decoding */
        if (s != null)
        {
            s = reader.readLine();
            int offset = 0;
            byte [] temp;

            while (s != null)
            {
                temp = s.getBytes();
                System.arraycopy(temp, 0, buffer, offset, temp.length);
                offset += temp.length;
                s = reader.readLine();
                if (s == null)
                {
                    reader.close();
                    return null;
                }

                /* checks the PEM file ending identifier */
                if (s.startsWith("-----END"))
                {
                    s = null;
                }
            }
            reader.close();
            byte [] result = new byte[offset];
            System.arraycopy(buffer, 0, result, 0, offset);
            return fromByteArray(result);
        }

        reader.close();

        /* file is in binary format, need not to do base64 decoding */
        FileInputStream in = new FileInputStream(f);
        in.read(buffer);
        in.close();
        return fromByteArray(buffer);
    }

    /**
     * Gets the DER encoded byte array of the given ASN1Object.
     * @exception	iss.security.asn1.ASN1Exception if the DER encoding
     * process throws the exception.
     */
    public static byte [] toByteArrayDER(ASN1Object o)
    throws ASN1Exception
    {
        if (o.getByteArray() == null)
        {
            byte [] data = derCoder.encode(o);
            o.setByteArray(data);
            return data;
        }
        else
        {
            return o.getByteArray();
        }
    }

    /**
     * Gets the PEM (DER and base64 encoded) byte array of the given
     * ASN1Object.
     * @exception	iss.security.asn1.ASN1Exception if the DER encoding
     * process throws the exception.
     */
    public static byte [] toByteArrayPEM(ASN1Object o)
    throws ASN1Exception
    {
        if (o.getByteArray() == null)
        {
            byte [] data = derCoder.encode(o);
            o.setByteArray(data);
            return CBBase64.encode(data);
        }
        else
        {
            return CBBase64.encode(o.getByteArray());
        }
    }

    /**
     * Gets the DER encoded byte array of the given ASN1Object, and
     * saves the byte array to a file.
     * @exception	iss.security.asn1.ASN1Exception if the DER encoding
     * process throws this exception.
     */
    public static void saveDER(ASN1Object o, File file)
    throws IOException, ASN1Exception
    {
        byte [] data = toByteArrayDER(o);
        FileOutputStream out = new FileOutputStream(file);
        out.write(data);
        out.close();
    }

    /**
     * Saves the given ASN1Object to a file in PEM format.
     * @param	name the name/type of the ASN1Object.
     * @exception	iss.security.asn1.ASN1Exception if the DER encoding
     * process throws this exception.
     */
    public static void savePEM(ASN1Object o, File file, String name)
    throws IOException, ASN1Exception
    {

        byte [] inPEM = toByteArrayPEM(o);
        savePEM(inPEM, file, name);
    }

    /**
     * Saves the given Object ASN.1 encoding to a file in PEM format.
     * @param	name - the name/type of the ASN1Object.
     */
    public static void savePEM(byte [] inPEM, File file, String name)
    throws IOException
    {
        PrintWriter writer = new PrintWriter(new FileWriter(file));

        /* write beginning line */
        writer.print("-----BEGIN ");
        writer.print(name);
        writer.println("-----");

        /* write data */
        int i;
        for (i = 0; i < inPEM.length / 64; i++)
        {
            String temp = new String(inPEM, i*64, 64);
            writer.println(temp);
        }
        if (inPEM.length != i * 64)
        {
            String temp = new String(inPEM, i*64, inPEM.length-i*64);
            writer.println(temp);
        }

        /* write ending line */
        writer.print("-----END ");
        writer.print(name);
        writer.println("-----");
        writer.close();
    }

    /**
     * Base64 encoding of the input byte array.
    public static byte [] der2pem(byte [] data) {
    	return Base64Coder.encode(data);
}
     */

    /**
     * Base64 decoding of the input byte array.
    public static byte [] pem2der(byte [] data) {
    	return Base64Coder.decode(data);
}
     */
}
