package com.ca.commons.jndi;

import com.ca.commons.cbutil.CBBase64;

import javax.naming.directory.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * (c) GroupMind Project
 * - Dr Christopher Betts 2005
 *
 * THIS CLASS IS *NOT* PART OF THE ORIGINAL JXPLORER CLASSES IN THIS PACKAGE...
 */
public class JndiTools
{
   private static Logger log = Logger.getLogger(JndiTools.class.getName());

    /* replaced by EntryAttribute class
    public static BasicAttribute makeAttribute(String ID, String[] vals)
    {
        BasicAttribute att = new BasicAttribute(ID);
        for (String val : vals)
            att.add(val);
        return att;
    }
    */

    // candidate for replacement by an 'EntryAttributes' class...
    public static BasicAttributes makeAttributes(Attribute[] vals)
    {
        BasicAttributes atts = new BasicAttributes();
        for (Attribute val : vals)
            atts.put(val);
        return atts;
    }

    // Need this to do decent password encoding, once we've got binary atts working...
//    protected byte[] getLDAPValueBytes(String s, int type)
    public static String shaEncode(String s)
        throws IOException
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("sha");
            StringBuffer hexString = new StringBuffer("{sha}");

            md.reset();  // necessary?

            md.update(s.getBytes("UTF-8"));

            byte[] buff = md.digest();

            hexString.append(CBBase64.binaryToString(buff));
            return hexString.toString();
           // return hexString.toString().getBytes("UTF-8");
        }
        catch (java.security.NoSuchAlgorithmException e)
        {
            log.log(Level.WARNING, "Unexpected error encoding password ", e);
            e.printStackTrace();

            throw new IOException("internal error encrypting password " + e.getMessage());
        }
    }
}
