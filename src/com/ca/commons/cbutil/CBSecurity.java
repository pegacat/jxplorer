package com.ca.commons.cbutil;

/**
 * <p>This is a grab bag of useful static functions
 * related to security - mainly doing conversions
 * between PEM and DER.</p>
 * <p/>
 * (nb: PEM = 'Privacy Enhanced Mail' format,<br>
 * while DER = 'Destinguished Encoding Rules' ASN1 data.<br>
 * - PEM is usually base64 encoded DER data, with some minor frills.)
 */


public class CBSecurity
{

// XXX WARNING !!! XXX
// Many of the following constants have derived from observation, rather than looking
// At standards.  ('cause I can't find the standards :-( ).  - CB

    /**
     * Standard header for the base 64 encoded info block of a pem file.
     */

    public static final byte[] PEM_BEGIN = (new String("-----BEGIN")).getBytes();

    /**
     * Standard footer for the base 64 encoded info block of a pem file.
     */
    public static final byte[] PEM_END = (new String("-----END")).getBytes();

    /**
     * Standard header for a pem encoded certificate block
     */

    public static final byte[] PEM_CERT_HEADER = new String("-----BEGIN CERTIFICATE-----").getBytes();

    /**
     * Standard footer for a pem encoded certificate block
     */

    public static final byte[] PEM_CERT_FOOTER = new String("-----END CERTIFICATE-----").getBytes();


    /**
     * Standard header for a pem encoded encrypted private key block
     */

    public static final byte[] PEM_ENC_KEY_HEADER = (new String("-----BEGIN ENCRYPTED PRIVATE KEY-----")).getBytes();

    /**
     * Standard header for a pem unencoded encrypted private key block
     */

    public static final byte[] PEM_KEY_HEADER = (new String("-----BEGIN PRIVATE KEY-----")).getBytes();

    /**
     * Standard footer for a pem encoded encrypted private key block
     */

    public static final byte[] PEM_ENC_KEY_FOOTER = (new String("-----END ENCRYPTED PRIVATE KEY-----")).getBytes();

    /**
     * Standard footer for a pem unencoded encrypted private key block
     */

    public static final byte[] PEM_KEY_FOOTER = (new String("-----END PRIVATE KEY-----")).getBytes();

    /**
     * Standard header for a pem encoded RSA private key block
     */

    public static final byte[] PEM_RSA_KEY_HEADER = new String("-----BEGIN RSA PRIVATE KEY-----").getBytes();

    /**
     * Standard header for a pem encoded RSA private key block
     */

    public static final byte[] PEM_RSA_KEY_FOOTER = new String("-----END RSA PRIVATE KEY-----").getBytes();

    /**
     * Returns the position that a searchByte first appears in
     * a byte array.
     *
     * @param mainArray  the byte array to search within
     * @param searchByte the byte to look for
     */

    public static int indexOf(byte[] mainArray, byte searchByte)
    {
        return indexOf(mainArray, searchByte, 0);
    }

    /**
     * Returns the first position, greater than a given index,
     * that a searchByte first appears at within an array.
     *
     * @param mainArray  the byte array to search within
     * @param searchByte the byte to look for
     */

    public static int indexOf(byte[] mainArray, byte searchByte, int fromIndex)
    {
        int len = mainArray.length;

        // Some sanity checks...

        if (fromIndex < 0)
        {
            fromIndex = 0;
        }
        else if (fromIndex >= len)
        {
            return -1;
        }

        // find the byte!

        for (int i = fromIndex; i < len; i++)
            if (mainArray[i] == searchByte) return i;

        return -1;                     // didn't find anything...
    }

    /**
     * <p>Tries to match a byte sequence within a larger
     * byte array.</p>
     * <p/>
     * <p>Students of Sun's java.lang.String class may
     * recognise some of this code :-). </p>
     *
     * @param mainArray      the base array to search within.
     * @param searchSequence the short sequence to find the position of
     *                       within the main array.
     * @return the index of the searchSequence within the main Array,
     *         or -1 if not found.
     */

    public static int indexOf(byte[] mainArray, byte[] searchSequence)
    {
        return indexOf(mainArray, searchSequence, 0);
    }

    /**
     * <p>Tries to match a byte sequence within a larger
     * byte array.</p>
     * <p/>
     * <p>Students of Sun's java.lang.String class may
     * recognise some of this code :-). </p>
     *
     * @param mainArray      the base array to search within.
     * @param searchSequence the short sequence to find the position of
     *                       within the main array.
     * @param fromIndex      the position to start searching from.
     * @return the index of the searchSequence within the main Array,
     *         or -1 if not found.
     */

    public static int indexOf(byte[] mainArray, byte[] searchSequence, int fromIndex)
    {
        byte v1[] = mainArray;
        byte v2[] = searchSequence;

        int max = mainArray.length;

        // Sanity Check (including empty arrays special condition)
        if (fromIndex >= max)
        {
            if (mainArray.length == 0 && fromIndex == 0 && searchSequence.length == 0)
            {
                /* There is an empty array at index 0 in an empty array. */
                return 0;
            }
            return -1;  // from index too large
        }


        // Sanity check: set negative fromIndexes to 0.
        if (fromIndex < 0)
        {
            fromIndex = 0;
        }

        // Empty search array is matched immediately
        if (searchSequence.length == 0)
        {
            return fromIndex;
        }

        byte first = v2[0];
        int i = fromIndex;

        startSearchForFirstChar:

        while (true)
        {

            /* Look for first character. */
            while (i < max && v1[i] != first)
            {
                i++;
            }

            if (i >= max)     // didn't find the sequence: return -1
            {
                return -1;
            }

            /* Found first character, now look at the rest of v2 */
            int j = i + 1;
            int end = j + searchSequence.length - 1;
            int k = 1;
            while (j < end)
            {
                if (v1[j++] != v2[k++])
                {
                    i++;
                    /* Look for str's first char again. */
                    continue startSearchForFirstChar;
                }
            }
            return i;    // Found whole string!!!
        }
    }

    /**
     * A simple check to see if a file is a PEM file, by
     * looking for PEM '------BEGIN...' and PEM '-----END'
     * tags.  Note that this is Not Conclusive!
     */

    public static boolean isPEM(byte[] test)
    {

        if (indexOf(test, PEM_BEGIN) == -1)
            return false;   // no PEM start string

        if (indexOf(test, PEM_END) == -1)
            return false;   // no PEM end string

        return true;  // has PEM begin and end tags - probably a PEM!
    }


    /**
     * This takes a byte array of PEM (originally rfc 1421-1424, but
     * has drifted a bit) encoded data, such as might be read as raw
     * bytes from a text file, and converts it to 'raw' DER binary
     * data (i.e. a byte array with values from 0x0 to 0xFF).
     *
     * @param pem the pem data to convert
     * @return the converted raw data
     */

    public static byte[] convertFromPEM(byte[] pem)
    {
        return convertFromPEM(pem, PEM_BEGIN, PEM_END);
    }

    /**
     * <p>This takes a byte array of PEM (originally rfc 1421-1424, but
     * has drifted a bit) encoded data, such as might be read as raw
     * bytes from a text file, and converts it to 'raw' DER binary
     * data (i.e. a byte array with values from 0x0 to 0xFF).</p>
     * <p/>
     * <p>In addition, this method allows the start of the PEM header
     * tag to be explicitly specified.  This is useful when
     * a single file contains multiple data blocks (e.g. a cert *and*
     * a private key).  Only the beginning of the stard header needs to
     * be specified; e.g. '-----BEGIN RSA PRIVATE' is sufficient, the
     * full header is not required.  (The footer is assumed to be the
     * first block starting with '-----END...')</p>
     *
     * @param pem the pem data to convert
     * @return the converted raw data
     */

    public static byte[] convertFromPEM(byte[] pem, byte[] header)
    {
        return convertFromPEM(pem, header, PEM_END);
    }

    /**
     * <p>This takes a byte array of PEM (originally rfc 1421-1424, but
     * has drifted a bit) encoded data representing an X509 certificate
     * and converts it to 'raw' DER binary
     * data (i.e. a byte array with values from 0x0 to 0xFF).</p>
     *
     * @param pem the pem data containing a certificate to convert
     * @return the converted raw data
     */

    public static byte[] convertFromPEMCertificate(byte[] pem)
    {
        return convertFromPEM(pem, PEM_CERT_HEADER, PEM_END);
    }


    /**
     * <p>This takes a byte array of PEM (originally rfc 1421-1424, but
     * has drifted a bit) encoded data, such as might be read as raw
     * bytes from a text file, and converts it to 'raw' DER binary
     * data (i.e. a byte array with values from 0x0 to 0xFF).</p>
     * <p/>
     * <p>In addition, this method allows the start of the PEM header
     * and footer tag to be explicitly specified.  This is useful when
     * a single file contains multiple data blocks (e.g. a cert *and*
     * a private key).  Only the beginning of the headers needs to
     * be specified; e.g. '-----BEGIN RSA PRIVATE' is sufficient, the
     * full header/footer is not required.</p>
     *
     * @param pem the pem data to convert
     * @return the converted raw data
     */


    public static byte[] convertFromPEM(byte[] pem, byte[] header, byte[] footer)
    {
        int start, end;

        start = indexOf(pem, header);

        end = indexOf(pem, footer);

        if (start == -1 || end == -1) return null;  // Something wrong - no headers!

        start = indexOf(pem, (byte) '\n', start) + 1;

        // skip past any more text, by avoiding all lines less than 64 characters long...

        int next;
        while ((next = indexOf(pem, (byte) '\n', start)) < start + 64)
        {
            if (next == -1)                         // really shouldn't ever happen...
                break;                              // - maybe for a very, very short file?

            start = next + 1;                       // keep looking for short lines...
        }

        if (start == -1)                            // something wrong - no end of line after '-----BEGIN...'
            return null;

        int len = end - start;

        byte[] data = new byte[len];

        System.arraycopy(pem, start, data, 0, len); // remove the PEM fluff from tbe base 64 data, stick in 'data'

        return CBBase64.decode(data);               // return the raw binary data
    }

    /**
     * This takes an array of raw data representing a DER encoded X509 certificate,
     * and base64 encodes it, adding PEM style -----BEGIN CERTIFICATE-----
     * and -----END CERTIFICATE----- tags.
     *
     * @param der the DER encoded data
     */

    public static byte[] convertToPEMCertificate(byte[] der)
    {
        return convertToPEM(der, PEM_CERT_HEADER, PEM_CERT_FOOTER);
    }

    /**
     * This takes an array of raw data representing a DER encoded RSA private key,
     * and base64 encodes it, adding PEM style -----BEGIN CERTIFICATE-----
     * and -----END CERTIFICATE----- tags.
     *
     * @param der the DER encoded data
     */

    public static byte[] convertToPEMRSAPrivateKey(byte[] der)
    {

        return convertToPEM(der, PEM_RSA_KEY_HEADER, PEM_RSA_KEY_FOOTER);
    }

    /**
     * This takes an array of raw data representing an
     * Encrypted DER encoded private key (probably pkcs 8),
     * and base64 encodes it, adding PEM style -----BEGIN CERTIFICATE-----
     * and -----END CERTIFICATE----- tags.
     *
     * @param der the DER encoded data
     */

    public static byte[] convertToPEMEncryptedPrivateKey(byte[] der)
    {
        return convertToPEM(der, PEM_ENC_KEY_HEADER, PEM_ENC_KEY_FOOTER);
    }

    public static byte[] convertToPEMPrivateKey(byte[] der)
    {
        return convertToPEM(der, PEM_KEY_HEADER, PEM_KEY_FOOTER);
    }

    protected static byte[] convertToPEM(byte[] der, byte[] header, byte[] footer)
    {
        try
        {
            byte[] base64Data = CBBase64.encodeFormatted(der, 0, 64); // theoretically may throw exception (should never happen)...

            int len = header.length + 1 + base64Data.length + footer.length + 1;  // '+1's for '\n' chars.

            byte[] pem = new byte[len];

            int pos = 0;
            System.arraycopy(header, 0, pem, 0, header.length);
            pos += header.length;

            pem[pos++] = (byte) '\n';

            System.arraycopy(base64Data, 0, pem, pos, base64Data.length);
            pos += base64Data.length;

            System.arraycopy(footer, 0, pem, pos, footer.length);
            pos += footer.length;

            pem[pos] = (byte) '\n';

            base64Data = null;
            der = null;

            return pem;
        }
        catch (Exception e)
        {
            System.err.println("error decoding pem file: " + e);
            return null;
        }
    }
}
        