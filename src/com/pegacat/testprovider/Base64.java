package com.pegacat.testprovider;

import java.io.UnsupportedEncodingException;

/**
 * Converts back and forth between binary and base 64 (rfc 1521). There are
 * almost certainly java classes that already do this, but it will
 * take longer to find them than to write this :-) <p>
 * <p/>
 * Not fully optimised for speed - might be made faster if necessary... <p>
 * <p/>
 * Maybe should rewrite sometime as a stream?
 *
 * @author Chris Betts
 */

public class Base64
{


/*  if speeding up by using array, maybe mess around with this array...

    static int binToChar[] = 
    {
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0x3e, 0xff, 0xff, 0xff, 0x3f,
        0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b,
        0x3c, 0x3d, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
        0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e,
        0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16,
        0x17, 0x18, 0x19, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20,
        0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28,
        0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x30,
        0x31, 0x32, 0x33, 0xff, 0xff, 0xff, 0xff, 0xff
    };
*/

    /**
     * Purely static class; hence no one should be able to instantiate it...
     */

    private Base64()
    {
    }

    /**
     * Takes a binary byte array and converts it to a formatted base64 mime
     * encoded data.
     *
     * @param byteArray an array of 8 bit bytes to be converted
     * @return the resultant encoded string
     */
    public static String binaryToString(byte[] byteArray)
    {
        return binaryToString(byteArray, 0);
    }

    /**
     * Takes a binary byte array and converts it to base64 mime
     * encoded data.
     *
     * @param byteArray an array of 8 bit bytes to be converted
     * @param offset    The first line of the string may be offset by
     *                  by this many characters for nice formatting (e.g. in
     *                  an ldif file, the first line may include 'att value = ...'
     *                  at the beginning of the base 64 encoded block).
     * @return the resultant encoded string
     */

    public static String binaryToString(byte[] byteArray, int offset)
    {
        if (byteArray == null) return null;  // XXX correct behaviour?

        int arraySize = byteArray.length;
        int thirdSize = arraySize / 3;

        byte[] base64Data = encode(byteArray);

        if (base64Data == null) return null;  // Exception occurred.

        return format(base64Data, offset);

    }

    /**
     * This returns a formatted string representing the base64 data.
     *
     * @param base64Data a byte array of base64 encoded data.
     * @param offset     the string is formatted to fit inside column 76.
     *                   The offset number provides for the first line to be
     *                   foreshortened (basically for pretty formatting in ldif
     *                   files).
     */

// XXX this is unusually sucky, even for me.  Better would be too format on the fly...

    public static String format(byte[] base64Data, int offset)
    {

        String data;
        try
        {
            data = new String(base64Data, "US-ASCII");
        }
        catch (Exception e)
        {
            data = new String(base64Data);  // should be equivalent to above
        }
        
        // some magic to make the columns line up nicely, and everythin,
        // including leading space, to fit under column 76...
        
        StringBuffer buffer = new StringBuffer(data);

        int i = 76 - offset;
        while (i < base64Data.length)
        {
            buffer.insert(i, "\r\n ");
            i += 78;
        }

        return buffer.toString();
    }

    /**
     * Encodes an arbitrary byte array into an array of base64 encoded bytes
     * (i.e. bytes that have values corresponding to the ascii values of base 64
     * data, and can be type cast to those chars).
     *
     * @param byteArray the raw data to encode
     * @return the base64 encoded bytes (the length of this is ~ 4/3 the length of
     *         the raw data).
     */

    public static byte[] encode(byte[] byteArray)
    {
        try
        {
            int arraySize = byteArray.length;

            int outputSize = (arraySize % 3 == 0) ? (arraySize / 3) * 4 : ((arraySize / 3) + 1) * 4;

            byte[] output = new byte[outputSize];
            
            // iterate through array, reading off byte triplets and converting to base64
    
            int bufferLength = 0;

            for (int i = 0; i <= (arraySize - 3); i += 3)
            {
                convertTriplet(byteArray[i], byteArray[i + 1], byteArray[i + 2], 3, output, bufferLength);
                bufferLength += 4;
            }

            switch (arraySize % 3)
            {
                case 0:
                    break;

                case 1:
                    convertTriplet(byteArray[arraySize - 1], (byte) 0, (byte) 0, 1, output, bufferLength);
                    break;

                case 2:
                    convertTriplet(byteArray[arraySize - 2], byteArray[arraySize - 1], (byte) 0, 2, output, bufferLength);
                    break;
            }

            return output;
        }
        catch (Base64EncodingException e)
        {
            return null;
        }
    }

    /**
     * Encode an arbitrary byte array into an array of base64 encoded bytes.
     * That is, bytes that have values corresponding to the ascii values of base 64
     * data, and can be type cast to those chars.
     *
     * @param byteArray the raw data to encode
     * @param start     the number of padding spaces to have at the start (must be less than colSize)
     * @param colSize   the length of each line of text (i.e. the right hand margin).  Must
     *                  be a multiple of 4.
     * @return the base64 encoded bytes (the length of this is ~ 4/3 the length of
     *         the raw data).
     */

    public static byte[] encodeFormatted(byte[] byteArray, int start, int colSize)
            throws Base64EncodingException
    {
        try
        {
            if (colSize % 4 != 0)
            {
                throw new Base64EncodingException("error in encodeFormatted - colSize not a multiple of 4.");
            }

            if (start >= colSize)
            {
                throw new Base64EncodingException("error in encodeFormatted - start is not less than colSize.");
            }

            int arraySize = byteArray.length;

            int outputSize = start + ((arraySize % 3 == 0) ? (arraySize / 3) * 4 : ((arraySize / 3) + 1) * 4);

            outputSize += (outputSize / colSize) + 1;  // allow for new lines!

            byte[] output = new byte[outputSize];
            
            // iterate through array, reading off byte triplets and converting to base64
        
            for (int i = 0; i < start; i++)
                output[i] = (byte) ' ';   // pad to 'start' with spaces.

            int bufferLength = start;

            for (int i = 0; i <= (arraySize - 3); i += 3)
            {
                convertTriplet(byteArray[i], byteArray[i + 1], byteArray[i + 2], 3, output, bufferLength);
                bufferLength += 4;

                if (bufferLength % (colSize + 1) == colSize)  // check if we're at the end of a column
                {
                    output[bufferLength++] = (byte) '\n';
                }
            }

            switch (arraySize % 3)
            {
                case 0:
                    break;

                case 1:
                    convertTriplet(byteArray[arraySize - 1], (byte) 0, (byte) 0, 1, output, bufferLength);
                    bufferLength += 4;
                    break;

                case 2:
                    convertTriplet(byteArray[arraySize - 2], byteArray[arraySize - 1], (byte) 0, 2, output, bufferLength);
                    bufferLength += 4;
                    break;
            }
        
            // final '\n'
            if (bufferLength < outputSize) // it should be exactly one less!
            {
                output[bufferLength++] = (byte) '\n';
            }
            else
            {
                System.err.println("wierdness in formatted base 64 : bufferlength (" + bufferLength + ") != 1 + outputsize (" + outputSize + ")");
            }

            return output;
        }
        catch (Base64EncodingException e)
        {
            // XXX shouldn't we do something here?
            return null;
        }
        catch (Exception e2)
        {
            System.err.println("unexpected error in base 64 encoding");
            e2.printStackTrace();
            return null;
        }
    }


    /**
     * Takes a base 64 encoded string and converts it to a
     * binary byte array.
     *
     * @param chars a string of base64 encoded characters to be converted
     * @return the resultant binary array
     */

    public static byte[] stringToBinary(String chars)
    {
        if (chars == null) return null;

        byte charArray[];

        try
        {
            charArray = chars.getBytes("US-ASCII");
        }
        catch (UnsupportedEncodingException e)
        {
            charArray = chars.getBytes();
        }

        return decode(charArray);
    }


    /**
     * Decodes a byte array containing base64 encoded data.
     *
     * @param rawData a byte array, each byte of which is a seven-bit ASCII value.
     * @return the raw binary data, each byte of which may have any value from
     *         0 - 255.  This value will be null if a decoding error occurred.
     */

    public static byte[] decode(byte[] rawData)
    {
        try
        {

            int resultLength = (int) (rawData.length * .75); // set upper limit for binary array

            byte result[] = new byte[resultLength];

            int noBytesWritten = 0;
            int validCharacters = 0;

            byte c;
            byte quad[] = new byte[4]; // temp store for a quad of base64 byte characters
            int bytes;                // temp store for a triplet of bytes
            int numfound = 0;          // number of chars found for quad
             
             
            // iterate through, finding valid quads, and converting to byte triplets 
    
            for (int i = 0; i < rawData.length; i++)
            {
                c = rawData[i];
                if ((c >= (byte) 'A' && c <= (byte) 'Z') || (c >= (byte) 'a' && c <= (byte) 'z') || (c >= '0' && c <= '9') || (c == '+') || (c == (byte) '/') || (c == (byte) '='))
                {
                    quad[numfound++] = c;
                    validCharacters++;
                }
                else if (" \r\n\t\f".indexOf((char) c) == -1)
                {
                    //CBUtility.log("error... bad character (" + (char)c + ") read from base64 encoded string", 6);
                    return null;
                }

                // write the quad; paying special attention to possible 'filler'
                // characters '=' or '==' at the end of the string (see rfc).
                 
                if (numfound == 4)
                {
                    bytes = convertQuad(quad);
                    result[noBytesWritten++] = (byte) ((bytes & 0xFF0000) >> 16);
                    if (c != '=')
                    {
                        result[noBytesWritten++] = (byte) ((bytes & 0xFF00) >> 8);
                        result[noBytesWritten++] = (byte) (bytes & 0xFF);
                    }
                    else if (rawData[i - 1] != '=')
                    {
                        result[noBytesWritten++] = (byte) ((bytes & 0xFF00) >> 8);
                        if ((bytes & 0xFF) > 0)
                        {
                            //CBUtility.log("Warning: Corrupt base64 Encoded File - contains trailing bits after file end.", 6);
                            return null;
                        }
                    }
                    else if ((bytes & 0xFF00) > 0)
                    {
                        //CBUtility.log("Warning: Corrupt base64 Encoded File  - contains trailing bits after file end.", 6);
                        return null;
                    }

                    numfound = 0;
                }
            }
             
            // check that the number of real characters is correct - must be cleanly
            // divisible by 4...
             
            if (validCharacters % 4 != 0)
            {
                //CBUtility.log("Warning: Corrupt base64 Encoded File - Length (" + validCharacters + ") of valid characters not divisible by 4.", 6);
                return null;
            }


            byte finalResult[] = new byte[noBytesWritten];
            System.arraycopy(result, 0, finalResult, 0, noBytesWritten);
            return finalResult;
        }
        catch (Exception e)
        {
            //CBUtility.log("unable to create final decoded byte array from base64 bytes: " + e, 6);
            return null;
        }

    }

    /**
     * Decodes a byte array containing base64 encoded data.
     *
     * @param chars a String, each character of which is a seven-bit ASCII value.
     * @return the raw binary data, each byte of which may have any value from
     *         0 - 255.  This value will be null if a decoding error occurred.
     */

    public static byte[] decode(String chars)
            throws Base64EncodingException
    {

        if (chars == null) return null;

        byte rawData[];

        try
        {
            rawData = chars.getBytes("US-ASCII");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new Base64EncodingException("unable to convert base64 encoded data to bytes using US-ASCII encoding", e);
        }

        int resultLength = (int) (rawData.length * .75); // set upper limit for binary array

        byte result[] = new byte[resultLength];

        int noBytesWritten = 0;
        int validCharacters = 0;

        byte c;
        byte quad[] = new byte[4]; // temp store for a quad of base64 byte characters
        int bytes;                // temp store for a triplet of bytes
        int numfound = 0;          // number of chars found for quad


        // iterate through, finding valid quads, and converting to byte triplets

        for (int i = 0; i < rawData.length; i++)
        {
            c = rawData[i];
            if ((c >= (byte) 'A' && c <= (byte) 'Z') || (c >= (byte) 'a' && c <= (byte) 'z') || (c >= '0' && c <= '9') || (c == '+') || (c == (byte) '/') || (c == (byte) '='))
            {
                quad[numfound++] = c;
                validCharacters++;
            }
            else if (" \r\n\t\f".indexOf((char) c) == -1)
            {
                throw new Base64EncodingException("error... bad character (" + (char) c + ") read from base64 encoded string");
            }

            // write the quad; paying special attention to possible 'filler'
            // characters '=' or '==' at the end of the string (see rfc).

            if (numfound == 4)
            {
                bytes = convertQuad(quad);
                result[noBytesWritten++] = (byte) ((bytes & 0xFF0000) >> 16);
                if (c != '=')
                {
                    result[noBytesWritten++] = (byte) ((bytes & 0xFF00) >> 8);
                    result[noBytesWritten++] = (byte) (bytes & 0xFF);
                }
                else if (rawData[i - 1] != '=')
                {
                    result[noBytesWritten++] = (byte) ((bytes & 0xFF00) >> 8);
                    if ((bytes & 0xFF) > 0)
                    {
                        throw new Base64EncodingException("Warning: Corrupt base64 Encoded Data - contains trailing bits after end of base 64 data.");
                    }
                }
                else if ((bytes & 0xFF00) > 0)
                {
                    throw new Base64EncodingException("Warning: Corrupt base64 Encoded File  - contains trailing bits after end of base64 data.");
                }

                numfound = 0;
            }
        }

        // check that the number of real characters is correct - must be cleanly
        // divisible by 4...

        if (validCharacters % 4 != 0)
        {
            throw new Base64EncodingException("Warning: Corrupt base64 Encoded Data - Length (" + validCharacters + ") of valid characters not divisible by 4.");
        }


        byte finalResult[] = new byte[noBytesWritten];
        System.arraycopy(result, 0, finalResult, 0, noBytesWritten);
        return finalResult;

    }


    /**
     * Converts three bytes to 4 base 64 values...
     * a half hearted attempt has been made to make it go fast...
     *
     * @param a       the first byte to convert
     * @param b       the second byte to convert
     * @param c       the third byte to convert
     * @param Num     the Number of 'real' bytes to convert - i.e. 1 (just a),
     *                2 (a and b), or 3 (a,b and c).
     * @param buff    the result buffer to put the final values in.
     * @param buffpos the position to start filling the result buffer from.
     */

    private static void convertTriplet(byte a, byte b, byte c, int Num, byte[] buff, int buffpos)
            throws Base64EncodingException
    {
        byte w, x, y, z;  // the four 6 bit values extracted.
        int trip = (a << 16) | ((b << 8) & 0xFF00) | (c & 0xFF);

        w = (byte) ((trip & 0xFC0000) >> 18);
        x = (byte) ((trip & 0x03F000) >> 12);
        y = (byte) ((trip & 0x000FC0) >> 6);
        z = (byte) (trip & 0x00003F);

        buff[buffpos] = convertFrom6Bit(w);
        buff[buffpos + 1] = convertFrom6Bit(x);

        if (Num == 1)
        {
            buff[buffpos + 2] = (byte) '=';
            buff[buffpos + 3] = (byte) '=';
        }
        else
        {
            buff[buffpos + 2] = convertFrom6Bit(y);

            if (Num == 2)
            {
                buff[buffpos + 3] = (byte) '=';
            }
            else
            {
                buff[buffpos + 3] = convertFrom6Bit(z);
            }
        }
    }

    /**
     * Use rfc 1521 specified character conversions
     * (I wonder if a preset array would be faster?)
     *
     * @param b the 6 significant bit byte to be converted
     * @return the converted base64 character
     */

    // this might be sped up using an array index as provided above...

    private static byte convertFrom6Bit(byte b)
            throws Base64EncodingException
    {
        byte c;

        if (b < 26)
            return (byte) ('A' + b);           // 'A' -> 'Z'
        else if (b < 52)
            return (byte) (('a' - 26) + b);  // 'a' -> 'z'
        else if (b < 62)
            return (byte) (('0' - 52) + b);  // '0' -> '9'
        else if (b == 62)
            return ((byte) '+');
        else if (b == 63)
            return ((byte) '/');
        else                // error - should never happen
        {
            throw new Base64EncodingException("erroroneous value " + (char) b + " passed in convertFrom6bit");
        }
    }

    /**
     * Use rfc 1521 specified character conversions
     * (I wonder if a preset array would be faster?)
     *
     * @param c a byte representing a single base64 encoded character
     * @return the corresponding raw 6 bit 'true' value.
     */

    private static byte convertTo6Bit(byte c)
            throws Base64EncodingException
    {
        if (c == (byte) '+')
            return 62;
        else if (c == (byte) '/')
            return 63;
        else if (c == (byte) '=')  // this result not actually used by calling program...
            return 0;
        else if (c <= (byte) '9')
            return (byte) (c - (byte) '0' + 52);
        else if (c <= (byte) 'Z')
            return (byte) (c - (byte) 'A');
        else if (c <= (byte) 'z')
            return (byte) (c - (byte) 'a' + 26);
        else                // error - should never happen
        {
            throw new Base64EncodingException("erroroneous value " + (char) c + " passed in convertTo6bit");
        }
    }

    /**
     * Takes a triplet of base64 encoded characters, and returns
     * a triplet of appropriate bytes
     *
     * @param quad four base 64 encoded characters
     * @return the corresponding 'true' 24 bit value, as an int.
     */

    private static int convertQuad(byte[] quad)
            throws Base64EncodingException
    {
        byte a = convertTo6Bit(quad[0]);
        byte b = convertTo6Bit(quad[1]);
        byte c = convertTo6Bit(quad[2]);
        byte d = convertTo6Bit(quad[3]);

        int ret = (a << 18) + (b << 12) + (c << 6) + d;

        return ret;
    }
    
    static class Base64EncodingException extends Exception
    {
    
        public Base64EncodingException(String msg)
        {
            super(msg);
        }
    
        public Base64EncodingException(String msg, Exception e)
        {
            super(msg);
            initCause(e);
        }
    }    
}