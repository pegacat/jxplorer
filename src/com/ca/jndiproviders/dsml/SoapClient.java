package com.ca.jndiproviders.dsml;


/**
 * <p>SoapClient allows for a 'raw' soap message to be constructed and
 * retrieved.</p>
 *
 * <p>It takes the soap message as text, constructs the appropriate
 * http envelope, and hoofs it off to the server.  It then takes
 * the response, and returns it as raw text.</p>
 *
 * <p>The original functionality here is inspired by the ibm dev works
 * article: http://www-128.ibm.com/developerworks/xml/library/x-soapcl/
 * on a 'zero overhead' soap client by Bod duCharme and Michael Brennan.
 *
 */

import com.ca.commons.cbutil.CBBase64;

import java.io.*;
import java.net.*;
import java.util.logging.Logger;

public class SoapClient
{
    private static Logger log = Logger.getLogger(SoapClient.class.getName());

    /**
     *  Original main method for stand alone operation...
     *
     * @param args
     * @throws Exception
     */
    /*
    public static void main(String[] args) throws Exception
    {
        if (args.length < 2)
        {
            System.err.println("Usage:  java com.ca.jndiproviders.dsml.SoapClient " +
                    "http://soapURL soapEnvelopefile.xml" +
                    " [SOAPAction]");
            System.err.println("SOAPAction is optional.");
            System.exit(1);
        }

        String SOAPUrl = args[0];
        String xmlFile2Send = args[1];

        String SOAPAction = "";
        if (args.length > 2)
            SOAPAction = args[2];

        // Open the input file. After we copy it to a byte array, we can see
        // how big it is so that we can set the HTTP Content-Length
        // property.

        FileInputStream fin = new FileInputStream(xmlFile2Send);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        // Copy the SOAP file to the open connection.
        copy(fin, bout);
        fin.close();

        byte[] b = bout.toByteArray();

        sendSoapMsg(SOAPUrl, b, SOAPAction);

    }
    */
    /**
     * This takes a byte array and hoofs off the contents to the target URL, adding
     * a bunch of http headers, including an optional 'SOAPaction:' header.  It returns
     * the raw contents of the reply, sans any http headers.
     *
     * @param SOAPUrl
     * @param b
     * @param SOAPAction
     * @param username optional username for HTTPAuth Authorization: header
     * @param pwd optional password for HTTPAuth Authorization: header
     * @return the response data
     * @throws IOException
     */
    public static String sendSoapMsg(String SOAPUrl, byte[] b, String SOAPAction, String username, String pwd)
            throws IOException
    {
        log.finest("HTTP REQUEST SIZE " + b.length );

        // bracket Soap Action with quotes. *shrug*.
        if (SOAPAction.startsWith("\"") == false)
            SOAPAction = "\"" + SOAPAction + "\"";

        //TODO: reuse connection...

        //TODO: time out connection using ldap time out...
        // Create the connection where we're going to send the file.
        URL url = new URL(SOAPUrl);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

        // Set the appropriate HTTP parameters.
        httpConn.setRequestProperty("SOAPAction", SOAPAction);
        httpConn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        httpConn.setRequestProperty("Content-Length",
                String.valueOf(b.length));
        httpConn.setRequestProperty("Cache-Control", "no-cache");
        httpConn.setRequestProperty("Pragma", "no-cache");

        // if we have a username password, add an HTTP basic authorization header
        if (username != null && pwd != null)
        {
            String authval = username + ":" + pwd;
            String base64val = CBBase64.binaryToString(authval.getBytes("UTF-8"));
            httpConn.setRequestProperty("Authorization", "Basic " + base64val);
        }

        httpConn.setRequestMethod("POST");
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);

        // Everything's set up; send the XML that was read in to b.
        OutputStream out = httpConn.getOutputStream();
        out.write(b);
        out.close();

        // Read the response

        InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
        BufferedReader in = new BufferedReader(isr);

        StringBuffer response = new StringBuffer(1024);

        String inputLine;
        while ((inputLine = in.readLine()) != null)
            response.append(inputLine);

        in.close();

        log.finest("HTTP RESPONSE SIZE: " + response.length());

        return response.toString();
    }

    /*
    // copy method from From E.R. Harold's book "Java I/O"
    public static void copy(InputStream in, OutputStream out)
            throws IOException
    {
        // do not allow other threads to read from the
        // input or write to the output while copying is
        // taking place

        synchronized (in)
        {
            synchronized (out)
            {

                byte[] buffer = new byte[256];
                while (true)
                {
                    int bytesRead = in.read(buffer);
                    if (bytesRead == -1) break;
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }
    */
}