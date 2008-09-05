/**
 * Created by IntelliJ IDEA.
 * User: betch01
 * Date: Dec 3, 2002
 * Time: 12:10:04 PM
 * To change this template use Options | File Templates.
 */
package com.ca.commons.jndi;

import junit.framework.*;

public class BasicOpsTest extends TestCase
{
    public BasicOpsTest(String name)
     {
         super(name);
     }

     public static Test suite()
     {
         return new TestSuite(BasicOpsTest.class);
     }

     public static void main (String[] args)
     {
         System.out.println("Running basic ops test");
         junit.textui.TestRunner.run(suite());
     }

/*
//    requires test ldap server to run
     public static void testSlash() throws NamingException
     {

         String ldapServerURL = "ldap://betch01:19389";

         System.out.println("Running slash test");

         ConnectionData con = new ConnectionData();
         con.setURL(ldapServerURL);
         BasicOps ops = new BasicOps(con);

         NameParser nameParser = ops.getContext().getNameParser("");
         NamingEnumeration bloop = ops.list("o=Democorp,c=au");
         while (bloop.hasMoreElements())
         {
             SearchResult result = (SearchResult)bloop.nextElement();
             String name = result.getName();
             System.out.println("\n1: " + name);
             System.out.println("2: " + new CompositeName(name).get(0));
             String ldapString = new CompositeName(name).get(0);
             Name realName = ops.getContext().getNameParser("").parse(ldapString);
             System.out.println("3: " + realName.toString());
         }
     }
*/

     public void testStuff()
     {
     }

}
