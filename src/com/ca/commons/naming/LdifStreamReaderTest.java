package com.ca.commons.naming;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.StringReader;

/**
 * (c) Chris Betts; Pegacat Software (http://pegacat.com)
 */
public class LdifStreamReaderTest extends TestCase
{

       public LdifStreamReaderTest(String name)
       {
           super(name);
       }


       public static Test suite()
       {
           return new TestSuite(LdifStreamReaderTest.class);
       }



       public static void main(String[] args)
       {
           junit.textui.TestRunner.run(LdifStreamReaderTest.suite());
       }

       public void testStreamReading()
               throws Exception
       {
           String testText = "" +
                   "data: VGhlI\n" +
                   " 5nIHNsb3ds\n" +
                   " W5nIHRob3V\n" +
                   " YXRhIGJlaW\n" +
                   " pbGF0aW9u\n";

           String expectedOutput = "data: VGhlI5nIHNsb3dsW5nIHRob3VYXRhIGJlaWpbGF0aW9u";

           LdifStreamReader reader = new LdifStreamReader(new StringReader(testText));

           String firstLine = reader.readLine();

           assertEquals(expectedOutput, firstLine);
       }

           public void testLdifFile()
               throws Exception
       {
           String testText = "version: 1\n" +
                   "dn:cn=Barbara Jensen, ou=Product Development, dc=airius, dc=com\n" +
                   "objectclass:top\n" +
                   "objectclass:person\n" +
                   "objectclass:organizationalPerson\n" +
                   "cn:Barbara Jensen\n" +
                   "cn:Barbara J Jensen\n" +
                   "cn:Babs Jensen\n" +
                   "sn:Jensen\n" +
                   "uid:bjensen\n" +
                   "telephonenumber:+1 408 555 1212\n" +
                   "description:Babs is a big sailing fan, and travels extensively in sea\n" +
                   " rch of perfect sailing conditions.\n" +
                   "title:Product Manager, Rod and Reel Division\n";

           String expectedOutput = "version: 1\n" +
                   "dn:cn=Barbara Jensen, ou=Product Development, dc=airius, dc=com\n" +
                   "objectclass:top\n" +
                   "objectclass:person\n" +
                   "objectclass:organizationalPerson\n" +
                   "cn:Barbara Jensen\n" +
                   "cn:Barbara J Jensen\n" +
                   "cn:Babs Jensen\n" +
                   "sn:Jensen\n" +
                   "uid:bjensen\n" +
                   "telephonenumber:+1 408 555 1212\n" +
                   "description:Babs is a big sailing fan, and travels extensively in search of perfect sailing conditions.\n" +
                   "title:Product Manager, Rod and Reel Division\n";

           LdifStreamReader reader = new LdifStreamReader(new StringReader(testText));

           StringBuffer fileData = new StringBuffer();

           String line;
           while ((line = reader.readLine())!=null)
            fileData.append(line).append("\n");

           assertEquals(expectedOutput, fileData.toString());
       }
}
