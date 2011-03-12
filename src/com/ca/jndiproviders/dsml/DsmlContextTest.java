package com.ca.jndiproviders.dsml;

import junit.framework.*;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.*;
import java.io.*;

import com.ca.commons.naming.DXNamingEnumeration;

/**
 * This code lovingly written by Chris.
 */
public class DsmlContextTest extends TestCase
{
    /**
     * @param name
     */
    public DsmlContextTest(String name)
    {
        super(name);
    }

    /**
     * @return the test suite
     */
    public static Test suite()
    {
        return new TestSuite(DsmlContextTest.class);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Calls the DSML server directly.  Does not go over the wire.
     */
    public void testModifyRequest()
            throws Exception
    {
        //assertEquals(modifyResponse, executeInternalDSMLRequest(modifyRequest1));
    }

    /**
     * Calls the DSML server directly.  Does not go over the wire.
     */
    public void testSearchTreeRequest()
            throws Exception
    {
        StringBuffer treeSearch = DsmlContext.constructSearchRequest("c=AU", "wholeSubtree", "neverDerefAliases", 0, 0, false, "(objectClass=*)", new String[]{"objectClass", "cn"});
        assertXMLEquals(searchRequestSubTree, treeSearch.toString());

        //System.out.println("------\n");
        //System.out.println(treeSearch);
        //System.out.println("\n------\n");
    }

    /**
     * Calls the DSML server directly.  Does not go over the wire.
     */
    public void testSearchLevelRequest()
            throws Exception
    {
        StringBuffer levelSearch = DsmlContext.constructSearchRequest("c=AU", "singleLevel", "derefInSearching", 0, 0, false, "(objectClass=*)", new String[]{"objectClass", "cn"});
        //assertEquals(searchRequestSubTree, levelSearch.toString());

        //System.out.println("---SINGLE LEVEL---\n");
        //System.out.println(levelSearch);
        //System.out.println("\n------\n");
    }

    /**
     * Calls the DSML server directly.  Does not go over the wire.
     */
    public void testSearchEntry()
            throws Exception
    {
        StringBuffer entrySearch = DsmlContext.constructSearchRequest("c=AU", "baseObject", "derefAlways", 0, 0, false, "(objectClass=*)", null);
        //assertEquals(searchRequestSubTree, entrySearch.toString());

        //  System.out.println("--BASE OBJECT----\n");
        //  System.out.println(entrySearch);
        //  System.out.println("\n------\n");
    }


    private static String searchResponse1 = "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "   <soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> \n" +
            "      <soapenv:Body>\n" +
            "         <batchResponse xmlns=\"urn:oasis:names:tc:DSML:2:0:core\">\n" +
            "            <searchResponse>\n" +
            "               <searchResultEntry dn=\"ou=Corporate,o=DEMOCORP,c=AU\">\n" +
            "                  <attr name=\"ou\">\n" +
            "                     <value>Corporate</value>\n" +
            "                  </attr>\n" +
            "                  <attr name=\"objectClass\">\n" +
            "                     <value>organizationalUnit</value>\n" +
            "                     <value>top</value>\n" +
            "                     <value>inetOrganizationalUnit</value>\n" +
            "                  </attr>\n" +
            "                  <attr name=\"description\">\n" +
            "                     <value>a very nice org unit</value>\n" +
            "                     <value>not at all like sales</value>\n" +
            "                     <value>no charges were ever proved</value>\n" +
            "                     <value>Clerical &lt;/value&gt; fred</value>\n" +
            "                  </attr>\n" +
            "                  <attr name=\"password\">\n" +
            "                     <value xsi:type=\"xsd:base64Binary\">YQBh</value>    " +
            "                  </attr>\n" +
            "               </searchResultEntry>\n" +
            "               <searchResultEntry dn=\"ou=Customer,o=DEMOCORP,c=AU\">\n" +
            "                  <attr name=\"ou\">\n" +
            "                     <value>Customer</value>\n" +
            "                  </attr>\n" +
            "                  <attr name=\"objectClass\">\n" +
            "                     <value>organizationalUnit</value>\n" +
            "                  </attr>\n" +
            "               </searchResultEntry>\n" +
            "               <searchResultEntry dn=\"ou=Engineering,o=DEMOCORP,c=AU\">\n" +
            "                  <attr name=\"ou\">\n" +
            "                     <value>Engineering</value>\n" +
            "                  </attr>\n" +
            "                  <attr name=\"objectClass\">\n" +
            "                     <value>organizationalUnit</value>\n" +
            "                  </attr>\n" +
            "               </searchResultEntry>\n" +
            "               <searchResultEntry dn=\"ou=Human Resources,o=DEMOCORP,c=AU\">\n" +
            "                  <attr name=\"ou\">\n" +
            "                     <value>Human Resources</value>\n" +
            "                  </attr>\n" +
            "                  <attr name=\"objectClass\">\n" +
            "                     <value>organizationalUnit</value>\n" +
            "                  </attr>\n" +
            "               </searchResultEntry>\n" +
            "               <searchResultEntry dn=\"ou=Manufacturing,o=DEMOCORP,c=AU\">\n" +
            "                  <attr name=\"ou\">\n" +
            "                     <value>Manufacturing</value>\n" +
            "                  </attr>\n" +
            "                  <attr name=\"objectClass\">\n" +
            "                     <value>organizationalUnit</value>\n" +
            "                  </attr>\n" +
            "               </searchResultEntry>\n" +
            "               <searchResultEntry dn=\"ou=Marketing,o=DEMOCORP,c=AU\">\n" +
            "                  <attr name=\"ou\">\n" +
            "                     <value>Marketing</value>\n" +
            "                  </attr>\n" +
            "                  <attr name=\"objectClass\">\n" +
            "                     <value>organizationalUnit</value>\n" +
            "                  </attr>\n" +
            "               </searchResultEntry>\n" +
            "               <searchResultEntry dn=\"ou=Operations,o=DEMOCORP,c=AU\">\n" +
            "                  <attr name=\"ou\">\n" +
            "                     <value>Operations</value>\n" +
            "                  </attr>\n" +
            "                  <attr name=\"objectClass\">\n" +
            "                     <value>organizationalUnit</value>\n" +
            "                     <value>top</value>\n" +
            "                     <value>inetOrganizationalUnit</value>\n" +
            "                  </attr>\n" +
            "               </searchResultEntry>\n" +
            "               <searchResultEntry dn=\"ou=Planning,o=DEMOCORP,c=AU\">\n" +
            "                  <attr name=\"ou\">\n" +
            "                     <value>Planning</value>\n" +
            "                  </attr>\n" +
            "                  <attr name=\"objectClass\">\n" +
            "                     <value>organizationalUnit</value>\n" +
            "                  </attr>\n" +
            "               </searchResultEntry>\n" +
            "               <searchResultEntry dn=\"ou=Projects,o=DEMOCORP,c=AU\">\n" +
            "                  <attr name=\"ou\">\n" +
            "                     <value>Projects</value>\n" +
            "                  </attr>\n" +
            "                  <attr name=\"objectClass\">\n" +
            "                     <value>organizationalUnit</value>\n" +
            "                  </attr>\n" +
            "               </searchResultEntry>\n" +
            "               <searchResultEntry dn=\"ou=Services,o=DEMOCORP,c=AU\">\n" +
            "                  <attr name=\"ou\">\n" +
            "                     <value>Services</value>\n" +
            "                  </attr>\n" +
            "                  <attr name=\"objectClass\">\n" +
            "                     <value>organizationalUnit</value>\n" +
            "                  </attr>\n" +
            "               </searchResultEntry>\n" +
            "               <searchResultEntry dn=\"ou=\\\"Support\\\",o=DEMOCORP,c=AU\">\n" +
            "                  <attr name=\"ou\">\n" +
            "                     <value>Support</value>\n" +
            "                  </attr>\n" +
            "                  <attr name=\"objectClass\">\n" +
            "                     <value>organizationalUnit</value>\n" +
            "                  </attr>\n" +
            "               </searchResultEntry>\n" +
            "               <searchResultDone>\n" +
            "                  <resultCode code=\"0\"/>\n" +
            "               </searchResultDone>\n" +
            "            </searchResponse>\n" +
            "         </batchResponse> \n" +
            "      </soapenv:Body>\n" +
            "   </soapenv:Envelope>";


    private static String searchResponse2 = "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "   <soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> \n" +
            "      <mydsml:soapenv:Body>\n" +
            "         <mydsml:batchResponse xmlns:mydsml=\"urn:oasis:names:tc:DSML:2:0:core\">\n" +
            "            <mydsml:searchResponse>\n" +
            "               <mydsml:searchResultEntry dn=\"ou=Corporate,o=DEMOCORP,c=AU\">\n" +
            "                  <mydsml:attr name=\"ou\">\n" +
            "                     <mydsml:value>Corporate</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "                  <mydsml:attr name=\"objectClass\">\n" +
            "                     <mydsml:value>organizationalUnit</mydsml:value>\n" +
            "                     <mydsml:value>top</mydsml:value>\n" +
            "                     <mydsml:value>inetOrganizationalUnit</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "                  <mydsml:attr name=\"description\">\n" +
            "                     <mydsml:value>a very nice org unit</mydsml:value>\n" +
            "                     <mydsml:value>not at all like sales</mydsml:value>\n" +
            "                     <mydsml:value>no charges were ever proved</mydsml:value>\n" +
            "                     <mydsml:value>Clerical &lt;/value&gt; fred</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "                  <mydsml:attr name=\"password\">\n" +
            "                     <mydsml:value xsi:type=\"xsd:base64Binary\">YQBh</mydsml:value>    " +
            "                  </mydsml:attr>\n" +
            "               </mydsml:searchResultEntry>\n" +
            "               <mydsml:searchResultEntry dn=\"ou=Customer,o=DEMOCORP,c=AU\">\n" +
            "                  <mydsml:attr name=\"ou\">\n" +
            "                     <mydsml:value>Customer</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "                  <mydsml:attr name=\"objectClass\">\n" +
            "                     <mydsml:value>organizationalUnit</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "               </mydsml:searchResultEntry>\n" +
            "               <mydsml:searchResultEntry dn=\"ou=Engineering,o=DEMOCORP,c=AU\">\n" +
            "                  <mydsml:attr name=\"ou\">\n" +
            "                     <mydsml:value>Engineering</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "                  <mydsml:attr name=\"objectClass\">\n" +
            "                     <mydsml:value>organizationalUnit</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "               </mydsml:searchResultEntry>\n" +
            "               <mydsml:searchResultEntry dn=\"ou=Human Resources,o=DEMOCORP,c=AU\">\n" +
            "                  <mydsml:attr name=\"ou\">\n" +
            "                     <mydsml:value>Human Resources</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "                  <mydsml:attr name=\"objectClass\">\n" +
            "                     <mydsml:value>organizationalUnit</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "               </mydsml:searchResultEntry>\n" +
            "               <mydsml:searchResultEntry dn=\"ou=Manufacturing,o=DEMOCORP,c=AU\">\n" +
            "                  <mydsml:attr name=\"ou\">\n" +
            "                     <mydsml:value>Manufacturing</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "                  <mydsml:attr name=\"objectClass\">\n" +
            "                     <mydsml:value>organizationalUnit</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "               </mydsml:searchResultEntry>\n" +
            "               <mydsml:searchResultEntry dn=\"ou=Marketing,o=DEMOCORP,c=AU\">\n" +
            "                  <mydsml:attr name=\"ou\">\n" +
            "                     <mydsml:value>Marketing</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "                  <mydsml:attr name=\"objectClass\">\n" +
            "                     <mydsml:value>organizationalUnit</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "               </mydsml:searchResultEntry>\n" +
            "               <mydsml:searchResultEntry dn=\"ou=Operations,o=DEMOCORP,c=AU\">\n" +
            "                  <mydsml:attr name=\"ou\">\n" +
            "                     <mydsml:value>Operations</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "                  <mydsml:attr name=\"objectClass\">\n" +
            "                     <mydsml:value>organizationalUnit</mydsml:value>\n" +
            "                     <mydsml:value>top</mydsml:value>\n" +
            "                     <mydsml:value>inetOrganizationalUnit</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "               </mydsml:searchResultEntry>\n" +
            "               <mydsml:searchResultEntry dn=\"ou=Planning,o=DEMOCORP,c=AU\">\n" +
            "                  <mydsml:attr name=\"ou\">\n" +
            "                     <mydsml:value>Planning</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "                  <mydsml:attr name=\"objectClass\">\n" +
            "                     <mydsml:value>organizationalUnit</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "               </mydsml:searchResultEntry>\n" +
            "               <mydsml:searchResultEntry dn=\"ou=Projects,o=DEMOCORP,c=AU\">\n" +
            "                  <mydsml:attr name=\"ou\">\n" +
            "                     <mydsml:value>Projects</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "                  <mydsml:attr name=\"objectClass\">\n" +
            "                     <mydsml:value>organizationalUnit</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "               </mydsml:searchResultEntry>\n" +
            "               <mydsml:searchResultEntry dn=\"ou=Services,o=DEMOCORP,c=AU\">\n" +
            "                  <mydsml:attr name=\"ou\">\n" +
            "                     <mydsml:value>Services</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "                  <mydsml:attr name=\"objectClass\">\n" +
            "                     <mydsml:value>organizationalUnit</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "               </mydsml:searchResultEntry>\n" +
            "               <mydsml:searchResultEntry dn=\"ou=\\\"Support\\\",o=DEMOCORP,c=AU\">\n" +
            "                  <mydsml:attr name=\"ou\">\n" +
            "                     <mydsml:value>Support</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "                  <mydsml:attr name=\"objectClass\">\n" +
            "                     <mydsml:value>organizationalUnit</mydsml:value>\n" +
            "                  </mydsml:attr>\n" +
            "               </mydsml:searchResultEntry>\n" +
            "               <mydsml:searchResultDone requestID=\"1\">\n" +
            "                  <mydsml:resultCode code=\"0\"/>\n" +
            "               </mydsml:searchResultDone>\n" +
            "            </mydsml:searchResponse>\n" +
            "         </mydsml:batchResponse> \n" +
            "      </soapenv:Body>\n" +
            "   </soapenv:Envelope>";


    public void testSearchResultParse()
            throws NamingException
    {
        NamingEnumeration results1 = DsmlContext.parseSearchResponse(searchResponse1, "c=AU");

        // check out the first entry

        SearchResult result = (SearchResult) results1.next();

        String dn = result.getName();

        assertEquals(dn, "ou=Corporate,o=DEMOCORP,c=AU");

        OrderedAttributes atts = new OrderedAttributes(result.getAttributes());

        Enumeration attEnum = atts.getAll();

        Attribute att1 = (Attribute) attEnum.nextElement();

        assertEquals(att1.getID(), "description");

        Enumeration values = att1.getAll();

        assertEquals(values.nextElement(), "a very nice org unit");
        assertEquals(values.nextElement(), "not at all like sales");
        assertEquals(values.nextElement(), "no charges were ever proved");
        assertEquals(values.nextElement(), "Clerical </value> fred");


    }


    private static String searchRequestSubTree = "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "   <soap-env:Envelope xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "      <soap-env:Body>\n" +
            "         <dsml:batchRequest xmlns:dsml=\"urn:oasis:names:tc:DSML:2:0:core\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "            <dsml:searchRequest dn=\"c=AU\" scope=\"wholeSubtree\" derefAliases=\"neverDerefAliases\">\n" +
            "               <dsml:filter>\n" +
            "                  <dsml:present name=\"objectClass\"/>\n" +
            "               </dsml:filter>\n" +
            "               <dsml:attributes>\n" +
            "                  <dsml:attribute name=\"objectClass\"/>\n" +
            "                  <dsml:attribute name=\"cn\"/>\n" +
            "               </dsml:attributes>\n" +
            "            </dsml:searchRequest>\n" +
            "         </dsml:batchRequest>\n" +
            "      </soap-env:Body>\n" +
            "   </soap-env:Envelope>";

    /**
     * Search request (cn=Trudi)
     */
    private static String searchRequest1 =
            "<dsml:batchRequest xmlns:dsml=\"urn:oasis:names:tc:DSML:2:0:core\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
            "   <dsml:searchRequest dn=\"c=AU\" scope=\"wholeSubtree\" derefAliases=\"neverDerefAliases\">" +
            "      <dsml:filter>" +
            "         <dsml:equalityMatch name=\"cn\">" +
            "            <dsml:value>Trudi</dsml:value>" +
            "         </dsml:equalityMatch>" +
            "      </dsml:filter>" +
            "      <dsml:attributes>" +
            "         <dsml:attribute name=\"objectClass\"/>" +
            "      </dsml:attributes>" +
            "   </dsml:searchRequest>" +
            "</dsml:batchRequest>";

    /**
     * Search request (&(|(sn=z*)(sn=w*))(|(sn=e*)(sn=r*)))
     */
    private static String searchRequest2 =
            "<dsml:batchRequest xmlns:dsml=\"urn:oasis:names:tc:DSML:2:0:core\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
            "   <dsml:searchRequest requestID=\"101\" dn=\"c=AU\" scope=\"wholeSubtree\" derefAliases=\"neverDerefAliases\">" +
            "       <dsml:filter>" +
            "           <dsml:and>" +
            "               <dsml:or>" +
            "                   <dsml:substrings name=\"sn\">" +
            "                       <dsml:initial>z</dsml:initial>" +
            "                   </dsml:substrings>" +
            "                   <dsml:substrings name=\"sn\">" +
            "                       <dsml:initial>w</dsml:initial>" +
            "                   </dsml:substrings>" +
            "               </dsml:or>" +
            "               <dsml:or>" +
            "                   <dsml:substrings name=\"sn\">" +
            "                       <dsml:initial>e</dsml:initial>" +
            "                   </dsml:substrings>" +
            "                   <dsml:substrings name=\"sn\">" +
            "                       <dsml:initial>r</dsml:initial>" +
            "                   </dsml:substrings>" +
            "               </dsml:or>" +
            "           </dsml:and>" +
            "       </dsml:filter>" +
            "       <dsml:attributes>" +
            "           <dsml:attribute name=\"objectClass\"/>" +
            "       </dsml:attributes>" +
            "   </dsml:searchRequest>" +
            "</dsml:batchRequest>";

    /**
     * Test that various ldap filters are correctly translated into the corresponding DSML (ignoring white space differences)
     *
     * @throws NamingException
     */
    public void testLdapFilter()
            throws NamingException
    {
        assertXMLEquals(filter1, DsmlContext.getSearchRequestFilter(new StringBuffer(), "(cn=fred)", "    "));

        assertXMLEquals(filter2, DsmlContext.getSearchRequestFilter(new StringBuffer(), "(&(!(sn<=Le*))(cn~=Crai))", "    "));

        assertXMLEquals(filter3, DsmlContext.getSearchRequestFilter(new StringBuffer(), "(!(|(&(!(|(sn=*)(cn=a*)(sn=b)))(!(&(sn=*c)(!(cn=d*))(sn=*e*))))(|(!(sn=f))(cn=g)(cn=*a*b*c*)(!(sn=*h)))))", "        "));
    }

    /**
     * Test that various ldap 'attribute match' search filters are correctly constructed.
     */
    public void testCreateAttributeLdapFilter()
    {
        OrderedAttributes testAtts = new OrderedAttributes("person", "chris");

        assertEquals("(person=*)", DsmlContext.getAttributeMatchFilter(testAtts));

        testAtts.put(new BasicAttribute("favoriteDrink"));

        assertEquals("(&(favoriteDrink=*)(person=*))", DsmlContext.getAttributeMatchFilter(testAtts));

        testAtts.put(new BasicAttribute("postCode"));

        assertEquals("(&(favoriteDrink=*)(person=*)(postCode=*))", DsmlContext.getAttributeMatchFilter(testAtts));

        testAtts.put(new BasicAttribute("address"));
        testAtts.put(new BasicAttribute("uid"));

        assertEquals("(&(address=*)(favoriteDrink=*)(person=*)(postCode=*)(uid=*))", DsmlContext.getAttributeMatchFilter(testAtts));

        assertEquals("(objectClass=*)", DsmlContext.getAttributeMatchFilter(new BasicAttributes()));

        assertEquals("(objectClass=*)", DsmlContext.getAttributeMatchFilter(null));
    }

    private static String filter1 =
            "<dsml:filter>\n" +
            "    <dsml:equalityMatch name=\"cn\">\n" +
            "        <dsml:value>fred</dsml:value>\n" +
            "    </dsml:equalityMatch>\n" +
            "</dsml:filter>";

    private static String filter2 =
            "              <dsml:filter>\n" +
            "                  <dsml:and>\n" +
            "                     <dsml:not>\n" +
            "                        <dsml:lessOrEqual name=\"sn\">\n" +
            "                           <dsml:value>Le*</dsml:value>\n" +
            "                        </dsml:lessOrEqual>\n" +
            "                     </dsml:not>\n" +
            "                     <dsml:approxMatch name=\"cn\">\n" +
            "                        <dsml:value>Crai</dsml:value>\n" +
            "                     </dsml:approxMatch>\n" +
            "                  </dsml:and>\n" +
            "               </dsml:filter>";
    /**
     * Search request (!(|(&(!(|(sn=*)(cn=a*)(sn=b)))(!(&(sn=*c)(!(cn=d*))(sn=*e*))))(|(!(sn=f))(cn=g)(cn=*a*b*c*)(!(sn=*h)))))
     */
    private static String filter3 =
            "       <dsml:filter>" +
            "          <dsml:not>" +
            "             <dsml:or>" +
            "                <dsml:and>" +
            "                   <dsml:not>" +
            "                      <dsml:or>" +
            "                         <dsml:present name=\"sn\"/>" +
            "                         <dsml:substrings name=\"cn\">" +
            "                            <dsml:initial>a</dsml:initial>" +
            "                         </dsml:substrings>" +
            "                         <dsml:equalityMatch name=\"sn\">" +
            "                            <dsml:value>b</dsml:value>" +
            "                         </dsml:equalityMatch>" +
            "                      </dsml:or>" +
            "                   </dsml:not>" +
            "                   <dsml:not>" +
            "                      <dsml:and>" +
            "                         <dsml:substrings name=\"sn\">" +
            "                            <dsml:final>c</dsml:final>" +
            "                         </dsml:substrings>" +
            "                         <dsml:not>" +
            "                            <dsml:substrings name=\"cn\">" +
            "                               <dsml:initial>d</dsml:initial>" +
            "                            </dsml:substrings>" +
            "                         </dsml:not>" +
            "                         <dsml:substrings name=\"sn\">" +
            "                            <dsml:any>e</dsml:any>" +
            "                         </dsml:substrings>" +
            "                      </dsml:and>" +
            "                   </dsml:not>" +
            "                </dsml:and>" +
            "                <dsml:or>" +
            "                   <dsml:not>" +
            "                      <dsml:equalityMatch name=\"sn\">" +
            "                         <dsml:value>f</dsml:value>" +
            "                      </dsml:equalityMatch>" +
            "                   </dsml:not>" +
            "                   <dsml:equalityMatch name=\"cn\">" +
            "                      <dsml:value>g</dsml:value>" +
            "                   </dsml:equalityMatch>" +
            "                   <dsml:substrings name=\"cn\">" +
            "                      <dsml:any>a</dsml:any>" +
            "                      <dsml:any>b</dsml:any>" +
            "                      <dsml:any>c</dsml:any>" +
            "                   </dsml:substrings>" +
            "                   <dsml:not>" +
            "                      <dsml:substrings name=\"sn\">" +
            "                         <dsml:final>h</dsml:final>" +
            "                      </dsml:substrings>" +
            "                   </dsml:not>" +
            "                </dsml:or>" +
            "             </dsml:or>" +
            "          </dsml:not>" +
            "       </dsml:filter>";

    /**
     * Delete cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU
     */
    private static String deleteRequest1 =
            "   <dsml:delRequest dn=\"cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU\"/>";

    /**
     * Test that various ldap filters are correctly translated into the corresponding DSML (ignoring white space differences)
     *
     * @throws NamingException
     */
    public void testDeleteRequest()
            throws NamingException
    {
        StringBuffer buffy = new StringBuffer();
        DsmlContext.getDeleteRequestElement(buffy, "cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU");
        assertXMLEquals(deleteRequest1, buffy.toString());
    }

    private static String escapeString = "&lt;&gt;&amp;&apos;&quot;fred";
    private static String rawString = "<>&'\"fred";
    private static String yuckyEscapeString = "&lt;&gt;&amp;&apos;&quot;&#66;&#72;&#65;&#64;";

    public void testEscape()
    {
        assertEquals(escapeString, DsmlContext.escape(rawString));
    }

    public void testUnescape()
    {
        assertEquals(rawString, DsmlContext.unescape(escapeString));
        assertEquals(rawString, DsmlContext.unescape(yuckyEscapeString));
    }

    private static String stringElement = "<dsml:value>organizationalPerson</dsml:value>";
    private static String binaryElement = "<dsml:value xsi:type=\"xsd:base64Binary\">c2VjcmV0IHBhc3N3b3Jk</dsml:value>";
    private static byte[] password = new byte[]{115, 101, 99, 114, 101, 116, 32, 112, 97, 115, 115, 119, 111, 114, 100};  // the password 'secret password'
    private static String address = "21 Jump Street$New York$90210";

    public void testCreateDsmlElement()
            throws NamingException
    {
        StringBuffer buffy1 = new StringBuffer();
        DsmlContext.createDsmlValueElement("organizationalPerson", buffy1);
        assertEquals(stringElement, buffy1.toString().trim());

        StringBuffer buffy2 = new StringBuffer();
        DsmlContext.createDsmlValueElement(password, buffy2);
        assertEquals(binaryElement, buffy2.toString().trim());
    }

    /**
     * Add cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU
     */
    private static String addRequest1 =
            "   <dsml:addRequest dn=\"cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU\">" +
            "      <dsml:attr name=\"cn\">" +
            "         <dsml:value>Alana SHORE</dsml:value>" +
            "      </dsml:attr>" +
            "      <dsml:attr name=\"objectClass\">" +
            "         <dsml:value>inetOrgPerson</dsml:value>" +
            "         <dsml:value>organizationalPerson</dsml:value>" +
            "         <dsml:value>person</dsml:value>" +
            "         <dsml:value>top</dsml:value>" +
            "      </dsml:attr>" +
            "      <dsml:attr name=\"sn\">" +
            "         <dsml:value>SHORE</dsml:value>" +
            "      </dsml:attr>" +
            "      <dsml:attr name=\"userPassword\">" +
            "         <dsml:value xsi:type=\"xsd:base64Binary\">c2VjcmV0IHBhc3N3b3Jk</dsml:value>" +
            "      </dsml:attr>" +
            "   </dsml:addRequest>";


    public void testAddRequest()
            throws NamingException
    {
        OrderedAttributes testAtts = new OrderedAttributes();
        testAtts.put(new BasicAttribute("cn", "Alana SHORE"));
        OrderedAttribute oc = new OrderedAttribute("objectClass");
        oc.add("inetOrgPerson");
        oc.add("organizationalPerson");
        oc.add("person");
        oc.add("top");
        testAtts.put(oc);
        testAtts.put(new BasicAttribute("userPassword", password));
        testAtts.put(new BasicAttribute("sn", "SHORE"));

        String dn = "cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU";

        StringBuffer addRequest = new StringBuffer();
        DsmlContext.getAddRequestElement(addRequest, dn, testAtts);

        //System.out.println(addRequest);

        assertXMLEquals(addRequest1, addRequest.toString());
    }


    /**
     * Modify (add a jpegPhoto) to cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU
     */
    private static String modifyRequest1 =
            "<dsml:batchRequest xmlns:dsml=\"urn:oasis:names:tc:DSML:2:0:core\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
            "   <dsml:modifyRequest dn=\"cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU\">" +
            "      <dsml:modification name=\"favoriteDrink\" operation=\"add\">" +
            "         <dsml:value>japanese slipper</dsml:value>" +
            "      </dsml:modification>" +
            "      <dsml:modification name=\"sn\" operation=\"delete\"/>" +
            "      <dsml:modification name=\"userPassword\" operation=\"replace\">" +
            "         <dsml:value xsi:type=\"xsd:base64Binary\">c2VjcmV0IHBhc3N3b3Jk</dsml:value>" +
            "      </dsml:modification>" +
            "   </dsml:modifyRequest>" +
            "</dsml:batchRequest>";

    /**
     * Modify (add a jpegPhoto) to cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU
     */
    private static String modifyRequest2 =
            "<dsml:batchRequest xmlns:dsml=\"urn:oasis:names:tc:DSML:2:0:core\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
            "   <dsml:modifyRequest dn=\"cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU\">" +
            "      <dsml:modification name=\"jpegPhoto\" operation=\"add\">" +
            "         <dsml:value xsi:type=\"xsd:base64Binary\">/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAUDBAQEAwUEBAQFBQUGBwwIBwcHBw8LCwkMEQ8SEhEPERETFhwXExQaFRERGCEYGh0dHx8fExciJCIeJBweHx7/2wBDAQUFBQcGBw4ICA4eFBEUHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh7/wAARCAA3ACkDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD7KqK5mitreS4nkWOKJC8jscBVAySamxzXk37WviZ/DHwK16aGXy7m/RbCAjs0p2n/AMd3VpSh7Saj3E3ZXMT4a/tLeB/F/inU9DuXbSfJnIsbi4b93dRjjJPRGyOh7V7XaXtndrutbqGcYz+7cN/KvgbwTonijT/BFrquh+EZtW0N5/ssrW1uskrSfxORjOPfpXq3wG0fxHZfGiXXbJli8NRacYL1Gl2FXOCqeX3II69smvpcwybC0aU5xqe/HeKTaTeyv09ddexwUsVUlUUXHR7M+q6MUKQwDDoaWvlj0AyPWvlL/gojrhi8LeG/DscgDXV61zIuOdsa4H6tX0F431W90WeC+s1EoEbCSFj8rge/Y18a/tXeONY8fXemSz6XaadpGizncPN3ztI2AWP+yMdB7mpy7H0J4t0W7STtr5rf8QrUpRpqXc1fgF8cp/Bvw/Tw/cabLclGdrSWMgAM3VXB9D3rpfg/qBuvFmnajqt5IiT3pa7EbYV95JUEf3Q+2vnrSJ4XvLwQMHiWRfmXpkjnFdfpHiS4sIjFZlDMFJUyvtTI5AJ/Kv1yhgaE6NS1r1FZvva6v+uh87Vqz5o26H6IDGOMUlcx8KZNcm+HWhTeJJ45tWls0kuHQcEtyPrwQM966j86/KKsPZ1JQvezsfQRd0mZXiXT1v8ATyhAJXJ59O9eDeI/2d9O8eS3OpS6xc6dAwLWiQoCssg6SOD1T/ZGM+tfRzKCCCMg01VWOMKihVUYAHQV5tLBRpY/67B2lZadLrqbSqOVP2b2PzR1rRL3wzqF3pWoWf2S6trhoJU243MP4sehHINQWyCaAWzgss8iocDJClhux+Ga+h/Ffwe8R/ETRNa8VW999o8QJq9wlukxCRXUCMVAH91hjAPTiuo/Zu+BN34YvD4m8bRW0mpBDHaWIIkS3B+87HoznGBjgDPrX6v/AKxYNYVVYNXstFpq0nt0VzwvqdR1LdD2D4d6pY6xoFld6Q92bCK3S3U3EJQuVUAMM/zrp9vuaSNFjQIihUAwFAwBT6/Nmk22la57KukFQ3YkNvIsLBZGXCn0J6GiiiSugItIsLfS9Nt9PtQRDAgRc9T6k+5PNW6KKIpJWQBRRRTA/9k=</dsml:value>" +
            "      </dsml:modification>" +
            "   </dsml:modifyRequest>" +
            "</dsml:batchRequest>";

    private static String modifyRequest3 =
            "       <dsml:modifyRequest dn=\"cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU\">\n" +
            "            <dsml:modification name=\"userPassword\" operation=\"add\">\n" +
            "                <dsml:value xsi:type=\"xsd:base64Binary\">c2VjcmV0IHBhc3N3b3Jk</dsml:value>\n" +
            "            </dsml:modification>\n" +
            "            <dsml:modification name=\"address\" operation=\"delete\">\n" +
            "                <dsml:value>21 Jump Street$New York$90210</dsml:value>\n" +
            "            </dsml:modification>\n" +
            "            <dsml:modification name=\"objectClass\" operation=\"replace\">\n" +
            "                <dsml:value>inetOrgPerson</dsml:value>\n" +
            "                <dsml:value>organizationalPerson</dsml:value>\n" +
            "                <dsml:value>person</dsml:value>\n" +
            "                <dsml:value>top</dsml:value>\n" +
            "            </dsml:modification>\n" +
            "        </dsml:modifyRequest>";
    
    public void testModRequest()
            throws NamingException
    {
        OrderedAttributes testAtts = new OrderedAttributes();
        testAtts.put(new BasicAttribute("cn", "Alana SHORE"));
        OrderedAttribute oc = new OrderedAttribute("objectClass");
        oc.add("inetOrgPerson");
        oc.add("organizationalPerson");
        oc.add("person");
        oc.add("top");

        String dn = "cn=Alana SHORE,ou=Infrastructure,ou=Support,o=DEMOCORP,c=AU";

        StringBuffer modRequest = new StringBuffer();

        ModificationItem[] mods = new ModificationItem[3];
        mods[0] = new ModificationItem(DsmlContext.ADD_ATTRIBUTE, new BasicAttribute("userPassword", password));
        mods[1] = new ModificationItem(DsmlContext.REMOVE_ATTRIBUTE, new BasicAttribute("address", address));
        mods[2] = new ModificationItem(DsmlContext.REPLACE_ATTRIBUTE, oc);

        DsmlContext.getModRequestElement(modRequest, dn, mods);
        //System.out.println(modRequest);

        assertXMLEquals(modifyRequest3, modRequest.toString());
    }

    private static String escapedName1 = "cn=Juliet &quot;&lt;&amp;&gt;&apos; LEVY";
    private static String unescapedName1= "cn=Juliet \"<&>' LEVY";

    public void testNameEscaping()
    {
        assertEquals(escapedName1, DsmlContext.escapeName(unescapedName1));
    }

    private static String modDNRequest =
            "           <dsml:modDNRequest dn=\"CN=Alice Johnson,DC=Example,DC=COM\"\n" +
            "                         newrdn=\"CN=Alice Weiss\"\n" +
            "                         deleteoldrdn=\"false\"/>";

    private static String modDNRequest2 =
            "           <dsml:modDNRequest dn=\"CN=Alice Johnson,DC=Example,DC=COM\"\n" +
            "                         newrdn=\"CN=Alice Weiss\"\n" +
            "                         deleteoldrdn=\"true\"/>";

    private static String modDNRequest3 =
            "           <dsml:modDNRequest dn=\"CN=Alice Johnson,DC=Example,DC=COM\"\n" +
            "                         newrdn=\"CN=Alice Weiss\"\n" +
            "                         deleteoldrdn=\"true\"/>";

// DSML
// cn=\\&quot;Craig \\\nLink\\&quot;,ou=Administration,ou=Corporate,o=DEMOCORP,c=AU
// java+ldap
// "cn=\\\"Craig \\\\nLink\\\",ou=Administration,ou=Corporate,o= DEMOCORP,c=AU"
// ldap
// cn=\"Craig \\nLink\",ou=Administration,ou=Corporate,o=DEMOCORP,c=AU

// no good - needs quotes escaped...
// newrdn="cn=\"Fred \\Nurk\""
// this seems to work...
// newrdn="cn=\&quot;Fred \\Nurk\&quot;"

    private static String modDNRequest4 =
            "           <dsml:modDNRequest dn=\"CN=Alice Johnson,DC=Example,DC=COM\"\n" +
            "                         newrdn=\"CN=Alice Weiss\"\n" +
            "                         deleteoldrdn=\"true\"/>";



    public void testModDNRequest()
            throws NamingException
    {
        Hashtable env = new Hashtable();
        env.put("java.naming.ldap.deleteRDN", "false");
        DsmlContext testCtx = new DsmlContext(env);

        StringBuffer buffy = new StringBuffer();
        testCtx.getModDNRequestElement(buffy, "CN=Alice Johnson,DC=Example,DC=COM", "CN=Alice Weiss");
        assertXMLEquals(modDNRequest, buffy.toString());

        testCtx.addToEnvironment("java.naming.ldap.deleteRDN", "true");
        buffy = new StringBuffer();
        testCtx.getModDNRequestElement(buffy, "CN=Alice Johnson,DC=Example,DC=COM", "CN=Alice Weiss");
        assertXMLEquals(modDNRequest2, buffy.toString());

        testCtx.addToEnvironment("java.naming.ldap.deleteRDN", "true");
        buffy = new StringBuffer();
        testCtx.getModDNRequestElement(buffy, "CN=Alice Johnson,DC=Example,DC=COM", "CN=Alice Weiss");
        assertXMLEquals(modDNRequest3, buffy.toString());

        testCtx.addToEnvironment("java.naming.ldap.deleteRDN", "true");
        buffy = new StringBuffer();
        testCtx.getModDNRequestElement(buffy, "CN=Alice Johnson,DC=Example,DC=COM", "CN=Alice Weiss");
        assertXMLEquals(modDNRequest4, buffy.toString());

    }

    /**
     * Search request for Alana SHORE.
     */
    private static String searchRequest4 =
            "<dsml:batchRequest xmlns:dsml=\"urn:oasis:names:tc:DSML:2:0:core\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
            "   <dsml:searchRequest dn=\"c=AU\" scope=\"wholeSubtree\" derefAliases=\"derefInSearching\">" +
            "      <dsml:filter>" +
            "         <dsml:equalityMatch name=\"cn\">" +
            "            <dsml:value>Alana SHORE</dsml:value>" +
            "         </dsml:equalityMatch>" +
            "      </dsml:filter>" +
            "      <dsml:attributes>" +
            "         <dsml:attribute name=\"objectClass\"/>" +
            "      </dsml:attributes>" +
            "   </dsml:searchRequest>" +
            "</dsml:batchRequest>";

    /**
     * The expected add response xml.
     */
    private static String addResponse =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<batchResponse xmlns=\"urn:oasis:names:tc:DSML:2:0:core\">" +
            "<addResponse>" +
            "<resultCode code=\"0\"/>" +
            "</addResponse>" +
            "</batchResponse>\n\n";

    /**
     * The expected delete response xml.
     */
    private static String delResponse =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<batchResponse xmlns=\"urn:oasis:names:tc:DSML:2:0:core\">" +
            "<delResponse>" +
            "<resultCode code=\"0\"/>" +
            "</delResponse>" +
            "</batchResponse>\n\n";

    // Example from the DSML v2.0 spec
    private static String delResponse2 =
            "<delResponse matchedDN=\"OU=HR,DC=Example,DC=COM\">\n" +
            "   <resultCode code=\"32\" descr=\"noSuchObject\"/>\n" +
            "   <errorMessage>DSDEL::230234</errorMessage>\n" +
            "</delResponse>\n";

    // Example from the DSML v2.0 spec
    private static String modResponse2 =
            "<modifyResponse>\n" +
            "   <resultCode code=\"53\" descr=\"unwillingToPerform\"/>\n" +
            "   <errorMessage>System Attribute may not be modified</errorMessage>\n" +
            "</modifyResponse>\n";

    private static String errorResponse =
            "        <errorResponse type=\"malformedRequest\">\n" +
            "              <message>Unknown element �bogusRequest�  line 87 column 4</message>\n" +
            "           </errorResponse>";

    // if modifying error messages, use 'print strings' to confirm result for assertEquals test
    public void testErrorResponse()
    {
        try
        {
            DsmlContext.checkForError(delResponse2);
            fail("no exception thrown when parsing error responses");
        }
        catch (NamingException e)
        {
            // expected case!
            //System.out.println(e.getMessage());
            assertEquals(e.getMessage(), "noSuchObject Exception (LDAP 32)\nDSDEL::230234");
        }

        try
        {
            DsmlContext.checkForError(modResponse2);
            fail("no exception thrown when parsing error responses");
        }
        catch (NamingException e)
        {
            // expected case!
            //System.out.println(e.getMessage());
            assertEquals(e.getMessage(), "unwillingToPerform Exception (LDAP 53)\nSystem Attribute may not be modified");
        }

        try
        {
            DsmlContext.checkForError(errorResponse);
            fail("no exception thrown when parsing error responses");
        }
        catch (NamingException e)
        {
            // expected case!
            //System.out.println(e.getMessage());
            assertEquals(e.getMessage(), "Error Processing DSML Request: malformedRequest\nUnknown element �bogusRequest�  line 87 column 4");
        }
    }

    /**
     * The expected modify response xml.
     */
    private static String modifyResponse =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<batchResponse xmlns=\"urn:oasis:names:tc:DSML:2:0:core\">" +
            "<modifyResponse>" +
            "<resultCode code=\"0\"/>" +
            "</modifyResponse>" +
            "</batchResponse>\n\n";

    /**
     * The expected modify DN response xml.
     */
    private static String modifyDNResponse =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<batchResponse xmlns=\"urn:oasis:names:tc:DSML:2:0:core\">" +
            "<modDNResponse>" +
            "<resultCode code=\"0\"/>" +
            "</modDNResponse>" +
            "</batchResponse>\n\n";

    //
    //  TEST UTILITIES
    //
    //  These are some methods and classes to allow for easy testing of
    //  XML and JNDI objects.  They allow for comparison between strings
    //  that have different white space, and solve the ordering problem.
    //



    /**
     * Compare two xml strings (in a fairly brutal manner)
     * ignoring whitespace.
     *
     * @param xml1
     * @param xml2
     */
    public static void assertXMLEquals(String xml1, String xml2)
    {
        StringTokenizer firstString = new StringTokenizer(xml1, " \r\n\t\f");
        StringTokenizer secondString = new StringTokenizer(xml2, " \r\n\t\f");

        StringBuffer firstXML = new StringBuffer(xml1.length());
        StringBuffer secondXML = new StringBuffer(xml2.length());

        while (firstString.hasMoreTokens())
        {
            firstXML.append(firstString.nextToken());
        }

        while (secondString.hasMoreTokens())
        {
            secondXML.append(secondString.nextToken());
        }
        assertEquals(firstXML.toString(), secondXML.toString());
    }

    /**
     * Utility class that returns attributes sorted in alphabetical order by ID
     */
    class OrderedAttributes extends BasicAttributes
    {
        public OrderedAttributes()
        {
            super();
        }

        public OrderedAttributes(String id, Object val)
        {
            super(id, val);
        }

        /**
         * Copy the attribute values of an existing element
         *
         * @param atts
         */

        public OrderedAttributes(Attributes atts)
        {
            Enumeration en = atts.getAll();
            while (en.hasMoreElements())
            {
                this.put((Attribute) en.nextElement());
            }
        }

        public NamingEnumeration getAll()
        {
            DXNamingEnumeration sortedByID = new DXNamingEnumeration();
            NamingEnumeration IDs = getIDs();
            while (IDs.hasMoreElements())
            {
                Attribute att = this.get((String) IDs.nextElement());
                sortedByID.add(att);
            }
            return sortedByID;
        }

        public NamingEnumeration getIDs()
        {
            Enumeration original = super.getIDs();
            DXNamingEnumeration sortedEnum = new DXNamingEnumeration(original);
            sortedEnum.sort();
            return sortedEnum;

        }
    }

    /**
     * Utility class that returns attributes sorted by order of addition
     */

    class OrderedAttribute extends BasicAttribute
    {
        public OrderedAttribute(String id)
        {
            super(id);
        }

        public NamingEnumeration getAll()
                throws NamingException
        {
            DXNamingEnumeration sortedByID = new DXNamingEnumeration();

            for (int i = 0; i < size(); i++)
            {
                Object val = this.get(i);
                sortedByID.add(val);
            }
            return sortedByID;
        }

    }

    public void testSchemaResultParse()
            throws Exception
    {
        StringBuffer bigSchema = new StringBuffer(400000);
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader("DsmlContextTestData.txt"));
            for (String s = br.readLine(); s != null; s = br.readLine())
            {
                bigSchema.append(s);
            }
        }
        catch (FileNotFoundException e)
        {
            System.out.println("SKIPPING testSchemaResultParse - can't find DsmlContextTestData.txt");
            return;
        }
        finally
        {
            if (br != null) br.close();
        }


        DXNamingEnumeration results1 = new DXNamingEnumeration(DsmlContext.parseSearchResponse(bigSchema.toString(), ""));

        assertEquals(results1.size(), 1);

        SearchResult result = (SearchResult) results1.next();

        String dn = result.getName();

        assertEquals(dn, "cn=schema");

        OrderedAttributes atts = new OrderedAttributes(result.getAttributes());

        DXNamingEnumeration attEnum = new DXNamingEnumeration(atts.getAll());

        attEnum.sort();

        assertEquals(attEnum.size(), 7);

        Object[] attributeArray = attEnum.toArray();

        assertEquals(((Attribute)attributeArray[0]).getID(), "attributeTypes");

        assertEquals(((Attribute)attributeArray[6]).getID(), "objectClasses");

        DXNamingEnumeration objectClassesValues = new DXNamingEnumeration(((Attribute)attributeArray[6]).getAll());

        objectClassesValues.sort();

        String[] values = objectClassesValues.toStringArray();

        int len = values.length;

        assertEquals(263, len);

        // check first
        assertEquals("( 0.9.2342.19200300.100.4.13 NAME 'domain' SUP ( top ) STRUCTURAL MUST ( dc ) MAY ( associatedName $ o $ description $ l $ st $ street $ physicalDeliveryOfficeName $ postalAddress $ postalCode $ postOfficeBox $ street $ facsimileTelephoneNumber $ internationaliSDNNumber $ telephoneNumber $ teletexTerminalIdentifier $ telexNumber $ preferredDeliveryMethod $ destinationIndicator $ registeredAddress $ x121Address $ businessCategory $ seeAlso $ searchGuide $ userPassword ) )", values[0]);

        // check last
        assertEquals("( 2.6.5.1.4 NAME 'mhsUserAgent' SUP ( applicationEntity ) STRUCTURAL MAY ( mhsDeliverableContentLength $ mhsDeliverableContentTypes $ mhsDeliverableEits $ mhs-or-addresses $ owner ) )", values[len-1]);
    }

}