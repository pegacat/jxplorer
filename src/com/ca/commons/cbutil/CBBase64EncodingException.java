package com.ca.commons.cbutil;

/**
 * This code lovingly written by Chris.
 */
public class CBBase64EncodingException extends Exception
{

    public CBBase64EncodingException(String msg)
    {
        super(msg);
    }

    public CBBase64EncodingException(String msg, Exception e)
    {
        super(msg);
        initCause(e);
    }
}
