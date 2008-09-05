/**
 * Created by IntelliJ IDEA.
 * User: erstr01
 * Date: Dec 20, 2002
 * Time: 3:45:33 PM
 * To change this template use Options | File Templates.
 */
package com.ca.commons.jndi;

import junit.framework.*;



public class ConnectionDataTest extends TestCase
{

    public static ConnectionData testConnection;

    public ConnectionDataTest(String name)
    {
        super(name);
    }



    public static Test suite()
    {
        return new TestSuite(ConnectionDataTest.class);
    }

    protected void setUp()
    {
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public void testGetPort()
    {
        ConnectionData testConnection, dsmlURL;
        testConnection = new ConnectionData();
        testConnection.setURL("ldap://bloop:19389");
        assertEquals("port I 19389 " + testConnection.getPort(), 19389, testConnection.getPort());
        testConnection.setProtocol(ConnectionData.DSML);
        testConnection.setURL("dsml://bloop:19389/myserver/stuff");
        assertEquals("port II 19389 " + testConnection.getPort(), 19389, testConnection.getPort());
        testConnection.setURL("dsml://fnord.ca.com:19389/myserver/stuff");
        assertEquals("port III 19389 " + testConnection.getPort(), 19389, testConnection.getPort());
        testConnection.setURL("dsml://fnord.ca.com:66000/myserver/stuff");
        assertEquals("port IV 66000 (-1) " + testConnection.getPort(), -1, testConnection.getPort());
        testConnection.setURL("dsml://fnord.ca.com:-500/myserver/stuff");
        assertEquals("port V -500 (-1) " + testConnection.getPort(), -1, testConnection.getPort());
    }

    public void testGetHost()
    {
        ConnectionData testConnection, dsmlURL;
        testConnection = new ConnectionData();
        testConnection.setURL("ldap://bloop:19389");
        assertEquals("host bloop " + testConnection.getHost(), "bloop", testConnection.getHost());
        testConnection.setProtocol(ConnectionData.DSML);
        testConnection.setURL("http://bloop:19389/myserver/stuff");
        assertEquals("host bloop " + testConnection.getHost(), "bloop", testConnection.getHost());
        testConnection.setURL("http://fnord.ca.com:19389/myserver/stuff");
        assertEquals("host fnord.ca.com " + testConnection.getHost(), "fnord.ca.com", testConnection.getHost());
    }

}
