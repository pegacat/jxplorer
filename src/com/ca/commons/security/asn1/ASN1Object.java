
package com.ca.commons.security.asn1;


import com.ca.commons.cbutil.CBParse;

import java.util.Vector;
import java.util.Hashtable;
import java.io.*;

/**
 * This class is an abstract representation of ASN.1 types. It is
 * the superclass of all classes representing actual ASN.1 types.
 * It also defines necessary interfaces for its subclasses. For more
 * information about ASN.1, please refer to:
 * <pre>CCITT. Recommendation X.208: Specification of Abstract Syntax
 * Notation One (ASN.1). 1988</pre>
 * <P> ASN.1 types are encoded using Basic Encoding Rules (BER) and
 * Distinguished Encoding Rules (DER). For more information about
 * these rules, please refer to:
 * <pre>CCITT. Recommendation X.209: Specification of Basic Encoding
 * Rules for Abstract Notation One (ASN.1). 1988</pre>
 * <P> To create an ASN1Object of a specific ASN.1 type, the static
 * method create() is called which takes an ASN1Type object as the
 * parameter and returns a suitable subclass of ASN1Object.
 * <P> This class provides a set of methods which converts ASN1Objects
 * to/from DER encoded byte arrays, Privacy Enhanced Mail (PEM) formatted
 * files and character strings, etc. It also has a set of methods which
 * converts ASN1Object to/from encrypted form. For more information about
 * PEM format and encryption, please refer to com.ca.commons.security.pkcs5.Envelope.
 * Ref: http://www.rsa.com/rsalabs/pubs/PKCS/
 *
 * @author who
 */
public class ASN1Object implements java.io.Serializable
{

    /** The ASN.1 type of the ASN1Object. */
    protected ASN1Type asn1Type;

    /** DER encoded byte array representation of the ASN1Object. */
    protected byte [] byteArray;

    /** The mapping table of ASN.1 type and implementation class. */
    private static Hashtable asn1ToJava = new Hashtable(27);

    static{
        Class p = (new Primitive()).getClass();
        register(ASN1Type.BOOLEAN, p);
        register(ASN1Type.INTEGER, p);
        register(ASN1Type.OCTET_STRING, p);
        register(ASN1Type.NULL, p);
        register(ASN1Type.OBJECT_ID, p);
        register(ASN1Type.BIT_STRING, p);
        register(ASN1Type.IA5String, p);
        register(ASN1Type.T61String, p);
        register(ASN1Type.PrintableString, p);
        register(ASN1Type.UTCTime, p);

        register(ASN1Type.GENERALIZEDTIME,p);
        register(ASN1Type.ENUMERATED,p);
        register(ASN1Type.UniversalString, p);
        register(ASN1Type.BMPString, p);

        Class s = (new Sequence()).getClass();
        register(ASN1Type.SEQUENCE, s);
        register(ASN1Type.SET, s);

        Class c = new Context().getClass();
        register(ASN1Type.ContextSpecific, c);
    }

    /**
     * Constructs an empty ASN1Object.
     */
    public ASN1Object()
    {}

    /**
     * Initializes the ASN1Object with a specific ASN1Type.
     */
    protected void init(ASN1Type type)
    {
        asn1Type = type;
        byteArray = null;
    }

    /**
     * Gets the ASN1Type of the ASN1Object.
     */
    public ASN1Type getASN1Type()
    {
        return asn1Type;
    }

    /**
     * Gets the DER encoded byte array of the ASN1Object.
     */
    public byte [] getByteArray()
    {
        return byteArray;
    }

    /**
     * Sets the DER encoded byte array of the ASN1Object.
     * @param	b the DER encoded byte array.
     */
    void setByteArray(byte [] b)
    {
        byteArray = b;
    }

    /**
     * Checks the type of this ASN1Object against the given ASN1Type.
     */
    public boolean isASN1Type(ASN1Type type)
    {
        return asn1Type.equals(type);
    }

    /**
     * Compares the 'len' byte sequences of 2 arrays, x and y starting at
     * offsets 'xoff' and 'yoff' respectively.
     */
    public boolean compByteArray(byte [] x, int xoff, byte [] y,
                                 int yoff, int len)
    {
        if (len <= 0 || x == null || xoff < 0 || x.length < xoff + len
                || y == null || yoff < 0 || y.length < yoff + len)
        {
            return false;
        }
        for (int i = 0; i < len; i++)
        {
            if (x[xoff+i] != y[yoff+i])
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares byte sequences of 2 arrays, x and y.
     */
    public boolean compByteArray(byte [] x, byte [] y)
    {
        if (x == null || y == null || x.length != y.length)
        {
            return false;
        }
        return compByteArray(x, 0, y, 0, x.length);
    }

    /**
     * Returns whether two ASN1Objects are equal.
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof com.ca.commons.security.asn1.ASN1Object))
        {
            return false;
        }
        try
        {
            byte [] der = ASN1Util.toByteArrayDER(this);
            byte [] der1 = ASN1Util.toByteArrayDER((ASN1Object) o);
            return compByteArray(der, der1);
        }
        catch(ASN1Exception asn1e)
        {
            asn1e.printStackTrace(System.out);
            return false;
        }
    }

    /**
     * Returns a string representation of the ASN1Object.
     */
    public String toString()
    {
        return asn1Type.toString();
    }


    /* interface for primitive and context specific ASN.1 types */

    private Object o = null;

    /**
     * Returns the actual value of the ASN1Object. The ASN1Object cannot
     * be of a constructed ASN.1 type.
     * Subclasses should overwrite this method.
     */
    public Object getValue()
    {
        return o;
        // throw new IllegalArgumentException("method not supported");
    }

    /**
     * Sets the value of the ASN1Object. The ASN1Object cannot be of a
     * constructed ASN.1 type.
     * Subclass should overwrite this method.
     */
    public void setValue(Object o)
    {
        this.o = o;
        // throw new IllegalArgumentException("method not supported");
    }


    /* interface for composite ASN.1 classes */

    Vector comps = new Vector();

    /**
     * Adds a component to the ASN1Object. The ASN1Object must be of a
     * constructed ASN.1 type.
     * Subclass should overwrite this method.
     */
    public void addComponent(ASN1Object o)
    {
        comps.addElement(o);
        // throw new IllegalArgumentException("method not supported");
    }

    /**
     * Adds a component to the ASN1Object at a given index. The ASN1Object
     * must be of a constructed ASN.1 type.
     * Subclass should overwrite this method.
     */
    public void addComponent(ASN1Object o, int index)
    {
        throw new IllegalArgumentException("method not supported");
    }

    /**
     * Gets a component from the ASN1Object. The ASN1Object must be of a
     * constructed ASN.1 type to allow this operation.
     * Subclass should overwrite this method.
     */
    public ASN1Object getComponent()
    {
        throw new IllegalArgumentException("method not supported");
    }

    /**
     * Gets a component from the ASN1Object at the given index. The
     * ASN1Object must be of a constructed ASN.1 type.
     * Subclass should overwrite this method.
     */
    public ASN1Object getComponent(int index)
    {
        return (ASN1Object) comps.elementAt(index);
        // throw new IllegalArgumentException("method not supported");
    }

    /**
     * Returns the number of components in the ASN1Object. The ASN1Object
     * must be of a constructed ASN.1 type.
     * Subclass should overwrite this method.
     */
    public int size()
    {
        return comps.size();
        // throw new IllegalArgumentException("method not supported");
    }


    /* interface for context specific ASN.1 class */

    /**
     * Returns whether the ASN1Object is using implicit ASN.1 tagging.
     * The ASN1Object must be of the context specific ASN.1 type.
     * Subclass should overwrite this method.
     */
    public boolean implicit()
    {
        throw new IllegalArgumentException("method not supported");
    }

    /**
     * Returns the context specific tag of the ASN1Object. The ASN1Object
     * must be of the context specific ASN.1 type.
     * Subclass should overwrite this method.
     */
    public int getTag()
    {
        if (asn1Type != null)
            return asn1Type.getTag();
        else
            throw new IllegalArgumentException("Object not initialised, does not have a type");
    }


    /* class methods */

    /**
     * Registers the implementation class of the ASN1Type.
     */
    private static void register(ASN1Type type, Class c)
    {
        asn1ToJava.put(type, c);
    }

    /**
     * This is the main method of creating an ASN1Object of the given
     * ASN1Type, without assigning it any initial value.
     * @exception	com.ca.commons.security.asn1.ASN1Exception if no implementation
     * class of the ASN1Type is available or cannot create an instance of
     * the implementation class.
     */
    public static ASN1Object create(ASN1Type type)
    throws ASN1Exception
    {
        Class impl = null;
        try
        {
            ASN1Object o;
            impl  = (Class) asn1ToJava.get(type);
            if (impl == null)
            {
                throw new ASN1Exception(type.toString()+
                                        " : no implementation class available");
            }
            o = (ASN1Object) impl.newInstance();
            o.init(type);
            return o;
        }
        catch (InstantiationException e)
        {
            throw new ASN1Exception("Cannot create instance for " +
                                    type.toString() + "\n" + e.toString());
        }
        catch (IllegalAccessException e)
        {
            throw new ASN1Exception("Cannot create instance for " +
                                    type.toString() + "\n" + e.toString());
        }
    }

    /**
     * Same as the method create(ASN1Type), except that this method assigns
     * the created ASN1Object an initial value.
     * @param	v the object to be assigned to the ASN1Object as its value.
     */
    public static ASN1Object create(ASN1Type type, Object v)
    throws ASN1Exception
    {
        ASN1Object o = create(type);
        o.setValue(v);
        return o;
    }


    /*
     * Conversion methods:
     * The following section contains methods that convert ASN1Objects
     * to/from various forms, like byte array, character string, file,
     * encrypted form, etc.
     */

    /**
     * Initializes the DER encoded byte array of the ASN1Object.
     */
    public void initByteArray()
    {
        try
        {
            byte [] buf = ASN1Util.toByteArrayDER(this);
            setByteArray(buf);
        }
        catch(ASN1Exception asn1e)
        {
            asn1e.printStackTrace(System.out);
            setByteArray(null);
        }
    }

    /**
     * Converts to a DER byte array.
     */
    public byte [] toDERBytes()
    {
        try
        {
            return ASN1Util.toByteArrayDER(this);
        }
        catch(ASN1Exception asn1e)
        {
            asn1e.printStackTrace(System.out);
            return null;
        }
    }

    /*
     * Converts to an encrypted DER byte array.	-	*** Not used ***
     * @param	cipher the encryption algorithm.
     * @param	iv 8-byte array which will be filled up with IV bytes.
     * @param	pw the Password object to get an encrypion password.
     * @exception	com.ca.commons.security.pkcs5.PasswordException if error occured
     * when get a new password from 'pw'.
     *
    public byte [] toDERBytes(String cipher, byte [] iv, Password pw)
    throws PasswordException {
    	byte [] buf = toPEMBytes(cipher, iv, pw);
    	return Base64Coder.decode(buf);
}
    */

    /**
     * Converts to a DER file (in binary format).
     */
    public boolean toDERFile(File file)
    throws IOException
    {
        try
        {
            ASN1Util.saveDER(this, file);
            return true;
        }
        catch(ASN1Exception asn1e)
        {
            asn1e.printStackTrace(System.out);
            return false;
        }
    }

    /**
     * Converts to a base64 encoded byte array.
     */
    public byte [] toBase64()
    {
        try
        {
            return ASN1Util.toByteArrayPEM(this);
        }
        catch(ASN1Exception asn1e)
        {
            asn1e.printStackTrace(System.out);
            return null;
        }
    }

    /*
     * Converts to an encrypted PEM byte array.	-	*** Not used ***
     * @param	cipher the encryption algorithm.
     * @param	iv 8-byte array which will be filled up with IV bytes.
     * @param	pw the Password object to get an encrypion password.
     * @exception	com.ca.commons.security.pkcs5.PasswordException if error occured
     * when get a new password from 'pw'.
     *
    private byte [] toPEMBytes(String cipher, byte [] iv, Password pw)
    throws PasswordException {
    // cannot see any chance to use it directly at the moment
    	byte [] buf = toDERBytes();
    	byte [] tmp = new byte[8];
    	byte [] enc = Envelope.writeEncByteArray(buf, cipher, tmp, 0, pw);
    	if (iv != null && iv.length >= 8) {
    		System.arraycopy(tmp, 0, iv, 0, 8);
    	}
    	return enc;
}
    */

    /**
     * Converts to a PEM file.
     * @param	name the object type/name.
     */
    public boolean toPEMFile(File file, String name)
    throws IOException
    {
        try
        {
            ASN1Util.savePEM(this, file, name);
            return true;
        }
        catch(ASN1Exception asn1e)
        {
            asn1e.printStackTrace(System.out);
            return false;
        }
    }

    /*
     * Converts to an encrypted PEM file.	-	*** Not used ***
     * @param	name the object type/name.
     * @param	cipher the encryption algorithm.
     * @param	pw the Password object to get an encrypion password.
     * @exception	com.ca.commons.security.pkcs5.PasswordException if error occured
     * when get a new password from 'pw'.
     *
    public boolean toPEMFile(File file, String name,
    String cipher, Password pw) throws IOException, PasswordException {
    	byte [] buf = toDERBytes();
    	return Envelope.writeEncFile(file, buf, name, cipher, 0, pw);
}
    */

    /*
     * Converts to a PEM String.	-	*** Not used ***
     * @param	name the object type/name.
     *
    public String toPEMString(String name) {
    	try {
    		return toPEMString(name, null, null);
    	} catch(PasswordException pe) {
    		return null;
    	}
}

    /*
     * Converts to an encrypted PEM String.	-	*** Not used ***
     * @param	name the object type/name.
     * @param	cipher the encryption algorithm.
     * @param	pw the Password object to get an encrypion password.
     * @exception	com.ca.commons.security.pkcs5.PasswordException if error occured
     * when get a new password from 'pw'.
     *
    public String toPEMString(String name, String cipher,
    Password pw) throws PasswordException {
    	byte [] buf = toDERBytes();
    	try {
    		return Envelope.writeEncFile(buf, name, cipher, 0, pw);
    	} catch(IOException ioe) {
    		return null;
    	}
}
    */


    /*
     * from other objects to ASN1Object
     */

    /*					-	*** Not used ***
     * Converts from a byte array. The byte array can be either
     * base64 encoded or not, and can be encrypted or not.
     * @param	cipher the encryption algorithm.
     * @param	iv the Initial Vector.
     * @param	pw the Password object to get an encrypion password.
     * @exception	com.ca.commons.security.pkcs5.PasswordException if cannot get the
     * password or the password is wrong.
     *
    public static ASN1Object fromBytes(byte [] data, String cipher,
    byte [] iv, Password pw) throws PasswordException {
    	if (data == null)
    		throw new NullPointerException();
    	byte [] buf = Envelope.readEncByteArray(data, cipher, iv, pw);
    	try {
    		return ASN1Util.fromByteArray(buf);
    	} catch(ASN1Exception asn1e) {
    		asn1e.printStackTrace(System.out);
    		return null;
    	}
}
    */

    /**
     * Converts from a byte array. The byte array can be either
     * base64 encoded or not, but is assumed not encrypted.
     */
    public static ASN1Object fromBytes(byte [] data)
    {
        try
        {
            //			return fromBytes(data, null, null, null);
            //		} catch(PasswordException pe) {
            return ASN1Util.fromByteArray(data);
        }
        catch(ASN1Exception asn1e)
        {
            System.out.println(CBParse.bytes2Hex(data));
            asn1e.printStackTrace(System.out);
            return null;
        }
    }

    /**
     * Converts from a DER or PEM file which is not encrypted.
     */
    public static ASN1Object fromFile(File file)
    throws IOException
    {
        /*
              ASN1Object o = null;
        		try { 				-	*** Not used ***
        			o = fromFile(file, null);
        		} catch(PasswordException pe) {
        			if (pe.getCode() == PasswordException.PASSWORD_REQUIRED) {
        				throw new IllegalArgumentException("expect a passphrase");
        			}
        		}
        		if (o == null) {			*/
        try
        {
            return ASN1Util.fromFile(file);
        }
        catch(ASN1Exception asn1e)
        {
            asn1e.printStackTrace(System.out);
            return null;
        }
        //		}
        //		return o;
    }

    /*						-	*** Not used ***
     * Converts from a PEM file which is either encrypted or not.
     * @param	pw the Password object to get an encrypion password.
     * @exception	com.ca.commons.security.pkcs5.PasswordException if cannot get the
     * password or the password is wrong.
     *
    public static ASN1Object fromFile(File file, Password pw)
    throws IOException, PasswordException  {
    	byte [] buf = Envelope.readEncFile(file, pw);
    	try {
    		return ASN1Util.fromByteArray(buf);
    	} catch(ASN1Exception asn1e) {
    		asn1e.printStackTrace(System.out);
    		return null;
    	}
}
    */

    /*					-	*** Not used ***
     * Converts from a String which is actually a PEM file in String form.
     * The PEM file can be either encrypted or not.
     * @param	pw the Password object to get an encrypion password.
     * @exception	com.ca.commons.security.pkcs5.PasswordException if cannot get the
     * password or the password is wrong.
     *
    public static ASN1Object fromString(String s, Password pw)
    throws PasswordException {
    	byte [] buf = null;
    	try {
    		buf = Envelope.readEncFile(s, pw);
    	} catch(IOException ioe) {
    		return null;
    	}
    	try {
    		return ASN1Util.fromByteArray(buf);
    	} catch(ASN1Exception asn1e) {
    		asn1e.printStackTrace(System.out);
    		return null;
    	}
}

    /**				-	*** Not used ***
     * Converts from a String which is actually a PEM file in String form.
     * The PEM file is not encrypted.
     *
    public static ASN1Object fromString(String s) {
    	try {
    		return fromString(s, null);
    	} catch(PasswordException pe) {
    		if (pe.getCode() == PasswordException.PASSWORD_REQUIRED) {
    			throw new IllegalArgumentException("expect a passphrase");
    		}
    		return null;
    	}
}
    */
}
