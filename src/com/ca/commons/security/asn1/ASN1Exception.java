
package com.ca.commons.security.asn1;

/**
 * The exception will be thrown when some error occurs during
 * the creation/coding of the ASN1Objects.
 *
 * @author who
 */
public class ASN1Exception extends Exception implements java.io.Serializable
{

    /**
     * No description message.
     */
    public ASN1Exception()
    {
        super();
    }
    
    /**
     * Gives a description message to the exception.
     */
    public ASN1Exception(String msg)
    {
        super(msg);
    }
}
