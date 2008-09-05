/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Axis" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.axis.utils;


import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicButtonListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * @author Doug Davis (dug@us.ibm.com)
 * @author Chris Betts (chris.betts@ca.com) - added interceptor capability to TCPMon program; also cleaned some stuff up.
 */

public class TCPInterceptor extends JFrame
{
    private JTabbedPane notebook = null;

    private static int STATE_COLUMN = 0;
    private static int TIME_COLUMN = 1;
    private static int INHOST_COLUMN = 2;
    private static int OUTHOST_COLUMN = 3;
    private static int REQ_COLUMN = 4;

    class AdminPage extends JPanel
    {
        public JRadioButton listenerButton, proxyButton;
        public JLabel hostLabel, tportLabel;
        public JTextField port, host, tport;
        public JTabbedPane noteb;
        public JCheckBox HTTPProxyBox;
        public JTextField HTTPProxyHost, HTTPProxyPort;
        public JLabel HTTPProxyHostLabel, HTTPProxyPortLabel;

        public AdminPage(JTabbedPane notebook, String name)
        {
            JPanel mainPane = null;
            JButton addButton = null;

            this.setLayout(new BorderLayout());
            noteb = notebook;

            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();

            mainPane = new JPanel(layout);

            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            mainPane.add(new JLabel(getMessage("newTCP00", "Create a new TCP/IP Monitor...") + " "), c);

            // Add some blank space
            mainPane.add(Box.createRigidArea(new Dimension(1, 5)), c);

            // The listener info
            ///////////////////////////////////////////////////////////////////
            JPanel tmpPanel = new JPanel(new GridBagLayout());

            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = 1;
            tmpPanel.add(new JLabel(getMessage("listenPort00", "Listen Port #") + " "), c);

            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            tmpPanel.add(port = new JTextField(4), c);

            mainPane.add(tmpPanel, c);

            mainPane.add(Box.createRigidArea(new Dimension(1, 5)), c);

            // Group for the radio buttons
            ButtonGroup btns = new ButtonGroup();

            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            mainPane.add(new JLabel(getMessage("actAs00", "Act as a...")), c);

            // Target Host/Port section
            ///////////////////////////////////////////////////////////////////
            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;

            final String listener = getMessage("listener00", "Listener");

            mainPane.add(listenerButton = new JRadioButton(listener), c);
            btns.add(listenerButton);
            listenerButton.setSelected(true);

            listenerButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    if (listener.equals(event.getActionCommand()))
                    {
                        boolean state = listenerButton.isSelected();

                        tport.setEnabled(state);
                        host.setEnabled(state);
                        hostLabel.setForeground(state ? Color.black : Color.gray);
                        tportLabel.setForeground(state ? Color.black : Color.gray);
                    }
                }
            }
            );

            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = 1;
            mainPane.add(Box.createRigidArea(new Dimension(25, 0)));
            mainPane.add(hostLabel = new JLabel(getMessage("targetHostname00", "Target Hostname") + " "), c);

            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            mainPane.add(host = new JTextField(30), c);

            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = 1;
            mainPane.add(Box.createRigidArea(new Dimension(25, 0)));
            mainPane.add(tportLabel = new JLabel(getMessage("targetPort00", "Target Port #") + " "), c);

            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            mainPane.add(tport = new JTextField(4), c);

            // Act as proxy section
            ///////////////////////////////////////////////////////////////////
            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            final String proxy = getMessage("proxy00", "Proxy");

            mainPane.add(proxyButton = new JRadioButton(proxy), c);
            btns.add(proxyButton);

            proxyButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    if (proxy.equals(event.getActionCommand()))
                    {
                        boolean state = proxyButton.isSelected();

                        tport.setEnabled(!state);
                        host.setEnabled(!state);
                        hostLabel.setForeground(state ? Color.gray : Color.black);
                        tportLabel.setForeground(state ? Color.gray : Color.black);
                    }
                }
            }
            );

            // Spacer
            /////////////////////////////////////////////////////////////////
            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            mainPane.add(Box.createRigidArea(new Dimension(1, 10)), c);

            // Options section
            ///////////////////////////////////////////////////////////////////
            JPanel opts = new JPanel(new GridBagLayout());

            opts.setBorder(new TitledBorder(getMessage("options00", "Options")));
            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            mainPane.add(opts, c);

            // HTTP Proxy Support section
            ///////////////////////////////////////////////////////////////////
            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            final String proxySupport = getMessage("proxySupport00", "HTTP Proxy Support");

            opts.add(HTTPProxyBox = new JCheckBox(proxySupport), c);

            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = 1;
            opts.add(HTTPProxyHostLabel = new JLabel(getMessage("hostname00", "Hostname") + " "), c);
            HTTPProxyHostLabel.setForeground(Color.gray);

            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            opts.add(HTTPProxyHost = new JTextField(30), c);
            HTTPProxyHost.setEnabled(false);

            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = 1;
            opts.add(HTTPProxyPortLabel = new JLabel(getMessage("port00", "Port #") + " "), c);
            HTTPProxyPortLabel.setForeground(Color.gray);

            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            opts.add(HTTPProxyPort = new JTextField(4), c);
            HTTPProxyPort.setEnabled(false);

            HTTPProxyBox.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    if (proxySupport.equals(event.getActionCommand()))
                    {
                        boolean b = HTTPProxyBox.isSelected();
                        Color color = b ? Color.black : Color.gray;

                        HTTPProxyHost.setEnabled(b);
                        HTTPProxyPort.setEnabled(b);
                        HTTPProxyHostLabel.setForeground(color);
                        HTTPProxyPortLabel.setForeground(color);
                    }
                }
                ;
            }
            );

            // Set default proxy values...
            String tmp = System.getProperty("http.proxyHost");

            if (tmp != null && tmp.equals(""))
                tmp = null;

            HTTPProxyBox.setSelected(tmp != null);
            HTTPProxyHost.setEnabled(tmp != null);
            HTTPProxyPort.setEnabled(tmp != null);
            HTTPProxyHostLabel.setForeground(tmp != null ? Color.black : Color.gray);
            HTTPProxyPortLabel.setForeground(tmp != null ? Color.black : Color.gray);

            if (tmp != null)
            {
                HTTPProxyBox.setSelected(true);
                HTTPProxyHost.setText(tmp);
                tmp = System.getProperty("http.proxyPort");
                if (tmp != null && tmp.equals("")) tmp = null;
                if (tmp == null) tmp = "80";
                HTTPProxyPort.setText(tmp);
            }

            // Spacer
            //////////////////////////////////////////////////////////////////
            mainPane.add(Box.createRigidArea(new Dimension(1, 10)), c);

            // ADD Button
            ///////////////////////////////////////////////////////////////////
            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            final String add = getMessage("add00", "Add");

            mainPane.add(addButton = new JButton(add), c);


            this.add(new JScrollPane(mainPane), BorderLayout.CENTER);

            // addButton.setEnabled( false );
            addButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    if (add.equals(event.getActionCommand()))
                    {
                        String text;
                        Listener l = null;
                        int lPort = Integer.parseInt(port.getText());
                        String tHost = host.getText();
                        int tPort = 0;

                        text = tport.getText();
                        if (text != null && !text.equals(""))
                            tPort = Integer.parseInt(text);
                        l = new Listener(noteb, null, lPort, tHost, tPort,
                                proxyButton.isSelected());

                        // Pick-up the HTTP Proxy settings
                        ///////////////////////////////////////////////////
                        text = HTTPProxyHost.getText();
                        if ("".equals(text)) text = null;
                        l.HTTPProxyHost = text;
                        text = HTTPProxyPort.getText();
                        if ("".equals(text)) text = null;
                        if (text != null)
                            l.HTTPProxyPort = Integer.parseInt(text);

                        port.setText(null);
                        host.setText(null);
                        tport.setText(null);
                    }
                }
                ;
            }
            );

            notebook.addTab(name, this);
            notebook.repaint();
            notebook.setSelectedIndex(notebook.getTabCount() - 1);
        }
    }

    class SocketWaiter extends Thread
    {
        ServerSocket sSocket = null;
        Listener listener;
        int port;
        boolean pleaseStop = false;

        public SocketWaiter(Listener l, int p)
        {
            listener = l;
            port = p;
            start();
        }

        public void run()
        {
            try
            {
                listener.setLeft(new JLabel(getMessage("wait00", " Waiting for Connection...")));
                listener.repaint();
                sSocket = new ServerSocket(port);
                for (; ;)
                {
                    Socket inSocket = sSocket.accept();

                    if (pleaseStop) break;
                    new Connection(listener, inSocket);
                    inSocket = null;
                }
            }
            catch (Exception exp)
            {
                if (!"socket closed".equals(exp.getMessage()))
                {
                    JLabel tmp = new JLabel(exp.toString());

                    tmp.setForeground(Color.red);
                    listener.setLeft(tmp);
                    listener.setRight(new JLabel(""));
                    listener.stop();
                }
            }
        }

        public void halt()
        {
            try
            {
                pleaseStop = true;
                new Socket("127.0.0.1", port);
                if (sSocket != null) sSocket.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }


    class SocketRR extends Thread
    {
        Socket inSocket = null;
        Socket outSocket = null;
        JTextArea textArea;
        JButton sendXMLButton;
        InputStream in = null;
        OutputStream out = null;
        boolean xmlFormat;
        volatile boolean done = false;
        TableModel tableModel = null;
        int tableIndex = 0;
        String type = null;
        Connection myConnection = null;
        boolean holdXMLUntilEdited = false;

        /**
         *
         * @param c
         * @param inputSocket
         * @param inputStream
         * @param outputSocket
         * @param outputStream
         * @param _textArea
         * @param format
         * @param tModel
         * @param index
         * @param type
         * @param hold  Whether to hold on to the XML message until the user has
         *        edited it - if false, message is sent on immediately (as per
         *        original TcpMon behaviour).
         * @param send a button which allows the user to send the message on manually,
         * but is only active if 'hold' is true.
         */
        public SocketRR(Connection c, Socket inputSocket, InputStream inputStream,
                        Socket outputSocket, OutputStream outputStream,
                        JTextArea _textArea, boolean format,
                        TableModel tModel, int index, final String type,
                        boolean hold, JButton send)
        {
            inSocket = inputSocket;
            in = inputStream;
            outSocket = outputSocket;
            out = outputStream;
            textArea = _textArea;
            xmlFormat = format;
            tableModel = tModel;
            tableIndex = index;
            this.type = type;
            myConnection = c;
            holdXMLUntilEdited = hold;
            sendXMLButton = send;

            start();
        }

        public boolean isDone()
        {
            return (done);
        }

        public void run()
        {
            try
            {
                // fancy pants hold-and-forward stuff dies if these buffer lengths are exceeded for a
                // single http request or response... - CB
                byte[] buffer = new byte[32768];
                byte[] tmpbuffer = new byte[65536];
                int saved = 0;
                int len;
            //    int i1, i2;
                int i;
                int reqSaved = 0;
                int tabWidth = 3;
                boolean atMargin = true;


                //if ( inSocket  != null ) inSocket.setSoTimeout( 10 );
                //if ( outSocket != null ) outSocket.setSoTimeout( 10 );

                if (tableModel != null)
                {
                    String tmpStr = (String) tableModel.getValueAt(tableIndex,
                            REQ_COLUMN);

                    if (!"".equals(tmpStr))
                        reqSaved = tmpStr.length();
                }
               //XXX clear out old listeners?
                if (holdXMLUntilEdited)
                    sendXMLButton.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            try
                            {
                                sendXMLButton.setEnabled(false);
                                String text = textArea.getText();
                                text = fixContentLengthHeaders(text);
                                textArea.setText(text);
                                byte[] buffer = text.getBytes();  // XXX do we need to force "UTF-8" ?
                                out.write(buffer, 0, buffer.length);
                            }
                            catch (Exception e2)
                            {
                                e2.printStackTrace();
                            }
                            finally
                            {
                                finishUp();
                            }
                        }
                    });


                readWriteLoop(buffer, saved, reqSaved, tmpbuffer, tabWidth);


                // this.sleep(3);  // Let other threads have a chance to run
                // halt();
                // Only set the 'done' flag if we were reading from a
                // Socket - if we were reading from an input stream then
                // we'll let the other side control when we're done
                //      if ( inSocket != null ) done = true ;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (!holdXMLUntilEdited)
                    finishUp();
            }
        }

        private void readWriteLoop(byte[] buffer, int saved, int reqSaved, byte[] tmpbuffer, int tabWidth) throws IOException
        {
            int len;
            int i;



            readloop:
            for (; ;)
            {
                if (done) break;
                len = buffer.length;
                // Used to be 1, but if we block it doesn't matter
                // however 1 will break with some servers, including apache
                if (len == 0) len = buffer.length;
                if (saved + len > buffer.length) len = buffer.length - saved;
                int len1 = 0;

                while (len1 == 0)
                {
                    try
                    {
                        len1 = in.read(buffer, saved, len);
                    }
                    catch (Exception ex)
                    {
                        if (done && saved == 0) break readloop;
                        len1 = -1;
                        break;
                    }
                }
                len = len1;

                if (len == -1 && saved == 0) break;
                if (len == -1) done = true;

                // No matter how we may (or may not) format it, send it
                // on unformatted - we don't want to mess with how its
                // sent to the other side, just how its displayed
                if (!holdXMLUntilEdited && out != null && len > 0)
                {
                    out.write(buffer, saved, len);
                }

                if (tableModel != null && reqSaved < 50)
                {
                    String old = (String) tableModel.getValueAt(tableIndex,
                            REQ_COLUMN);

                    old = old + new String(buffer, saved, len);
                    if (old.length() > 50)
                        old = old.substring(0, 50);

                    reqSaved = old.length();

                    if ((i = old.indexOf('\n')) > 0)
                    {
                        old = old.substring(0, i - 1);
                        reqSaved = 50;
                    }

                    tableModel.setValueAt(old, tableIndex, REQ_COLUMN);
                }

                if (xmlFormat)
                {
                    saved = xmlFormatTextArea(saved, len, buffer, tmpbuffer, tabWidth);
                }
                else
                {
                    textArea.append(new String(buffer, 0, len));
                }

                if (holdXMLUntilEdited)
                {
                    sendXMLButton.setEnabled(true);
                }


                // this.sleep(3);  // Let other threads have a chance to run
            }
        }

        private int xmlFormatTextArea(int saved, int len, byte[] buffer, byte[] tmpbuffer, int tabWidth)
        {
            int i;
            boolean atMargin;
            // Do XML Formatting
            boolean inXML = false;
            int bufferLen = saved;

            if (len != -1) bufferLen += len;
            int i1 = 0;
            int i2 = 0;
            int thisIndent = -1, nextIndent = -1, previousIndent = -1;
            saved = 0;
            for (; i1 < bufferLen; i1++)
            {
                // Except when we're at EOF, saved last char
                if (len != -1 && i1 + 1 == bufferLen)
                {
                    if (buffer[i1] > 32) // CB - But ONLY do that if it isn't a character we want!!!
                    {
                        tmpbuffer[i2++] = buffer[i1];
                        saved = 0;
                    }
                    else
                        saved = 1;

                    break;
                }
                thisIndent = -1;
                if (buffer[i1] == '<' && buffer[i1 + 1] != '/')
                {
                    previousIndent = nextIndent++;
                    thisIndent = nextIndent;
                    inXML = true;
                }
                if (buffer[i1] == '<' && buffer[i1 + 1] == '/')
                {
                    if (previousIndent > nextIndent)
                        thisIndent = nextIndent;
                    previousIndent = nextIndent--;
                    inXML = true;
                }
                if (buffer[i1] == '/' && buffer[i1 + 1] == '>')
                {
                    previousIndent = nextIndent--;
                    inXML = true;
                }
                if (thisIndent != -1)
                {
                    if (thisIndent > 0) tmpbuffer[i2++] = (byte) '\n';
                    for (i = tabWidth * thisIndent; i > 0; i--)
                        tmpbuffer[i2++] = (byte) ' ';
                }
                atMargin = (buffer[i1] == '\n' || buffer[i1] == '\r');

                if (!inXML || !atMargin)
                {
                    tmpbuffer[i2++] = buffer[i1];
                }
            }

            String text = new String(tmpbuffer, 0, i2);
            textArea.append(text);

            // Shift saved bytes to the beginning
            for (i = 0; i < saved; i++)
                buffer[i] = buffer[bufferLen - saved + i];
            return saved;
        }

        private void finishUp()
        {
            done = true;
            try
            {
                if (out != null)
                {
                    out.flush();
                    if (null != outSocket)
                        outSocket.shutdownOutput();
                    else
                        out.close();
                    out = null;
                }
            }
            catch (Exception e)
            {
                ;
            }
            try
            {
                if (in != null)
                {
                    if (inSocket != null)
                        inSocket.shutdownInput();
                    else
                        in.close();
                    in = null;
                }
            }
            catch (Exception e)
            {
                ;
            }
            myConnection.wakeUp();
        }

        public void halt()
        {
            try
            {
                if (inSocket != null) inSocket.close();
                if (outSocket != null) outSocket.close();
                inSocket = null;
                outSocket = null;
                if (in != null) in.close();
                if (out != null) out.close();
                in = null;
                out = null;
                done = true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }


    class Connection extends Thread
    {
        Listener listener;
        boolean active;
        String fromHost;
        String time;
        JTextArea inputText = null;
        JScrollPane inputScroll = null;
        JTextArea outputText = null;
        JScrollPane outputScroll = null;
        Socket inSocket = null;
        Socket outSocket = null;
        Thread clientThread = null;
        Thread serverThread = null;
        SocketRR requestSocketRR = null;
        SocketRR ResponseSocketRR = null;
        InputStream inputStream = null;

        String HTTPProxyHost = null;
        int HTTPProxyPort = 80;

        public Connection(Listener l)
        {
            listener = l;
            HTTPProxyHost = l.HTTPProxyHost;
            HTTPProxyPort = l.HTTPProxyPort;
        }

        public Connection(Listener l, Socket s)
        {
            this(l);
            inSocket = s;
            start();
        }

        public Connection(Listener l, InputStream in)
        {
            this(l);
            inputStream = in;
            start();
        }

        public void run()
        {
            try
            {
                active = true;

                HTTPProxyHost = System.getProperty("http.proxyHost");
                if (HTTPProxyHost != null && HTTPProxyHost.equals(""))
                    HTTPProxyHost = null;

                if (HTTPProxyHost != null)
                {
                    String tmp = System.getProperty("http.proxyPort");

                    if (tmp != null && tmp.equals("")) tmp = null;
                    if (tmp == null)
                        HTTPProxyPort = 80;
                    else
                        HTTPProxyPort = Integer.parseInt(tmp);
                }

                if (inSocket != null)
                    fromHost = (inSocket.getInetAddress()).getHostName();
                else
                    fromHost = "resend";

                DateFormat df = new SimpleDateFormat("MM/dd/yy hh:mm:ss aa");

                time = df.format(new Date());

                int count = listener.connections.size();

                listener.tableModel.insertRow(count + 1, new Object[]{
                    getMessage("active00", "Active"),
                    time,
                    fromHost,
                    listener.hostField.getText(), ""
                }
                );
                listener.connections.add(this);
                inputText = new JTextArea(null, null, 20, 80);
                inputScroll = new JScrollPane(inputText);
                outputText = new JTextArea(null, null, 20, 80);
                outputScroll = new JScrollPane(outputText);

                ListSelectionModel lsm = listener.connectionTable.getSelectionModel();

                if (count == 0 || lsm.getLeadSelectionIndex() == 0)
                {
                    listener.outPane.setVisible(false);
                    int divLoc = listener.outPane.getDividerLocation();

                    listener.setLeft(inputScroll);
                    listener.setRight(outputScroll);

                    listener.removeButton.setEnabled(false);
                    listener.removeAllButton.setEnabled(true);
                    listener.saveButton.setEnabled(true);
                    listener.resendButton.setEnabled(true);
                    listener.outPane.setDividerLocation(divLoc);
                    listener.outPane.setVisible(true);
                }

                String targetHost = listener.hostField.getText();
                int targetPort = Integer.parseInt(listener.tPortField.getText());

                InputStream inSocketInputStream = inputStream;
                OutputStream inSocketOutputStream = null;

                InputStream tmpIn2 = null;
                OutputStream tmpOut2 = null;

                if (inSocketInputStream == null)
                    inSocketInputStream = inSocket.getInputStream();

                if (inSocket != null)
                    inSocketOutputStream = inSocket.getOutputStream();

                String bufferedData = null;
                StringBuffer buf = null;

                int index = listener.connections.indexOf(this);

                if (listener.isProxyBox.isSelected() || HTTPProxyHost != null)
                {
                    // Check if we're a proxy
                    byte[] b = new byte[1];

                    buf = new StringBuffer();
                    String s;

                    for (; ;)
                    {
                        int len;

                        len = inSocketInputStream.read(b, 0, 1);
                        if (len == -1) break;
                        s = new String(b);
                        buf.append(s);
                        if (b[0] != '\n') continue;
                        break;
                    }

                    bufferedData = buf.toString();
                    inputText.append(bufferedData);

                    if (bufferedData.startsWith("GET ") ||
                            bufferedData.startsWith("POST "))
                    {
                        int start, end;
                        URL url;

                        start = bufferedData.indexOf(' ') + 1;
                        while (bufferedData.charAt(start) == ' ') start++;
                        end = bufferedData.indexOf(' ', start);
                        String urlString = bufferedData.substring(start, end);

                        if (urlString.charAt(0) == '/') urlString = urlString.substring(1);
                        if (listener.isProxyBox.isSelected())
                        {
                            url = new URL(urlString);
                            targetHost = url.getHost();
                            targetPort = url.getPort();
                            if (targetPort == -1) targetPort = 80;

                            listener.tableModel.setValueAt(targetHost, index + 1,
                                    OUTHOST_COLUMN);
                            bufferedData = bufferedData.substring(0, start) +
                                    url.getFile() +
                                    bufferedData.substring(end);
                        }
                        else
                        {
                            url = new URL("http://" + targetHost + ":" +
                                    targetPort + "/" + urlString);

                            listener.tableModel.setValueAt(targetHost, index + 1,
                                    OUTHOST_COLUMN);
                            bufferedData = bufferedData.substring(0, start) +
                                    url.toExternalForm() +
                                    bufferedData.substring(end);

                            targetHost = HTTPProxyHost;
                            targetPort = HTTPProxyPort;
                        }

                    }
                }
                else
                /*
                 *     Change Host: header to point to correct host
                 *     keep reading a byte at a time until we find
                 *     a line (a data block ending in '\n') that starts with
                 *     'Host' - in which case we replace it with our own host.
                 */
                {
                    //
                    //
                    //
                    byte[] b1 = new byte[1];

                    buf = new StringBuffer();
                    String s1;
                    String lastLine = null;

                    for (; ;)
                    {
                        int len;
                        len = inSocketInputStream.read(b1, 0, 1);
                        if (len == -1)
                            break;
                        s1 = new String(b1);
                        buf.append(s1);
                        if (b1[0] != '\n')
                            continue;
                        // we have a complete line
                        String line = buf.toString();

                        buf.setLength(0);
                        // check to see if we have found Host: header
                        if (line.startsWith("Host: "))
                        {
                            // we need to update the hostname to target host
                            String newHost = "Host: " + targetHost + "\r\n";

                            bufferedData = bufferedData.concat(newHost);
                            break;
                        }
                        // add it to our headers so far
                        if (bufferedData == null)
                            bufferedData = line;
                        else
                            bufferedData = bufferedData.concat(line);

                        // failsafe
                        if (line.equals("\r\n")) break;
                        if ("\n".equals(lastLine) && line.equals("\n")) break;
                        lastLine = line;
                    }
                    if (bufferedData != null)
                    {
                        inputText.append(bufferedData);
                        int idx = bufferedData.length() < 50 ? bufferedData.length() : 50;
                        s1 = bufferedData.substring(0, idx);
                        int i = s1.indexOf('\n');

                        if (i > 0) s1 = s1.substring(0, i - 1);
                        s1 = s1 + "                           " +
                                "                       ";
                        s1 = s1.substring(0, 51);
                        listener.tableModel.setValueAt(s1, index + 1,
                                REQ_COLUMN);
                    }
                }

                if (targetPort == -1) targetPort = 80;
                outSocket = new Socket(targetHost, targetPort);

                tmpIn2 = outSocket.getInputStream();
                tmpOut2 = outSocket.getOutputStream();

                if (!listener.holdRequestBox.isSelected() && bufferedData != null)
                {
                    byte[] b = bufferedData.getBytes();
                    tmpOut2.write(b);
                }

                boolean format = listener.xmlFormatBox.isSelected();

                requestSocketRR = new SocketRR(this, inSocket, inSocketInputStream, outSocket,
                        tmpOut2, inputText, format,
                        listener.tableModel, index + 1, "request:",
                        listener.holdRequestBox.isSelected(), listener.sendButton);
                ResponseSocketRR = new SocketRR(this, outSocket, tmpIn2, inSocket,
                        inSocketOutputStream, outputText, format,
                        null, 0, "response:",
                        listener.holdResponseBox.isSelected(), listener.sendButton);

                while (requestSocketRR != null || ResponseSocketRR != null)
                {
                    // Only loop as long as the connection to the target
                    // machine is available - once that's gone we can stop.
                    // The old way, loop until both are closed, left us
                    // looping forever since no one closed the 1st one.
                    // while( !ResponseSocketRR.isDone() )
                    if (null != requestSocketRR && requestSocketRR.isDone())
                    {
                        if (index >= 0 && ResponseSocketRR != null)
                        {
                            listener.tableModel.setValueAt(getMessage("resp00", "Resp"),
                                    1 + index, STATE_COLUMN);
                        }
                        requestSocketRR = null;
                    }
                    if (null != ResponseSocketRR && ResponseSocketRR.isDone())
                    {
                        if (index >= 0 && requestSocketRR != null)
                        {
                            listener.tableModel.setValueAt(getMessage("req00", "Req"),
                                    1 + index, STATE_COLUMN);
                        }
                        ResponseSocketRR = null;
                    }

                    //  Thread.sleep( 10 );
                    synchronized (this)
                    {
                        this.wait(1000); //Safety just incase we're not told to wake up.
                    }
                }

                //  System.out.println("Done ");
                // requestSocketRR.halt();
                // ResponseSocketRR.halt();


                active = false;

                /*
                 if ( inSocket != null ) {
                 inSocket.close();
                 inSocket = null ;
                 }
                 outSocket.close();
                 outSocket = null ;
                 */

                if (index >= 0)
                {
                    listener.tableModel.setValueAt(getMessage("done00", "Done"),
                            1 + index, STATE_COLUMN);

                }
            }
            catch (Exception e)
            {
                StringWriter st = new StringWriter();
                PrintWriter wr = new PrintWriter(st);
                int index = listener.connections.indexOf(this);

                if (index >= 0)
                    listener.tableModel.setValueAt(getMessage("error00", "Error"), 1 + index, STATE_COLUMN);
                e.printStackTrace(wr);
                wr.close();
                outputText.append(st.toString());
                halt();
            }
        }

        synchronized void wakeUp()
        {
            this.notifyAll();
        }

        public void halt()
        {
            try
            {
                if (requestSocketRR != null) requestSocketRR.halt();
                if (ResponseSocketRR != null) ResponseSocketRR.halt();
                if (inSocket != null) inSocket.close();
                inSocket = null;
                if (outSocket != null) outSocket.close();
                outSocket = null;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        public void remove()
        {
            int index = -1;

            try
            {
                halt();
                index = listener.connections.indexOf(this);
                listener.tableModel.removeRow(index + 1);
                listener.connections.remove(index);
            }
            catch (Exception e)
            {
                System.err.println("index:=" + index + this);
                e.printStackTrace();
            }
        }
    }


    class Listener extends JPanel
    {
        public Socket inputSocket = null;
        public Socket outputSocket = null;
        public JTextField portField = null;
        public JTextField hostField = null;
        public JTextField tPortField = null;
        public JCheckBox isProxyBox = null;
        public JCheckBox holdRequestBox = null;
        public JCheckBox holdResponseBox = null;
        public JButton stopButton = null;
        public JButton removeButton = null;
        public JButton removeAllButton = null;
        public JCheckBox xmlFormatBox = null;
        public JButton saveButton = null;
        public JButton resendButton = null;
        public JButton sendButton = null;
        public JButton switchButton = null;
        public JButton closeButton = null;
        public JTable connectionTable = null;
        public DefaultTableModel tableModel = null;
        public JSplitPane outPane = null;
        public ServerSocket sSocket = null;
        public SocketWaiter sw = null;
        public JPanel leftPanel = null;
        public JPanel rightPanel = null;
        public JTabbedPane notebook = null;
        public String HTTPProxyHost = null;
        public int HTTPProxyPort = 80;

        final public Vector connections = new Vector();

        public Listener(JTabbedPane _notebook, String name,
                        int listenPort, String host, int targetPort,
                        boolean isProxy)
        {
            notebook = _notebook;
            if (name == null) name = getMessage("port01", "Port") + " " + listenPort;

            this.setLayout(new BorderLayout());

            // 1st component is just a row of labels and 1-line entry fields
            /////////////////////////////////////////////////////////////////////
            JPanel top = new JPanel();

            top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
            top.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            final String start = getMessage("start00", "Start");

            top.add(stopButton = new JButton(start));
            top.add(Box.createRigidArea(new Dimension(5, 0)));
            top.add(new JLabel("  " + getMessage("listenPort01", "Listen Port:") + " ", SwingConstants.RIGHT));
            top.add(portField = new JTextField("" + listenPort, 4));
            top.add(new JLabel("  " + getMessage("host00", "Host:"), SwingConstants.RIGHT));
            top.add(hostField = new JTextField(host, 30));
            top.add(new JLabel("  " + getMessage("port02", "Port:") + " ", SwingConstants.RIGHT));
            top.add(tPortField = new JTextField("" + targetPort, 4));
            top.add(Box.createRigidArea(new Dimension(5, 0)));
            top.add(isProxyBox = new JCheckBox(getMessage("proxy00", "Proxy")));
            top.add(holdRequestBox = new JCheckBox(getMessage("holdRequest", "Hold Request")));
            top.add(holdResponseBox = new JCheckBox(getMessage("holdResponse", "Hold Response")));

            isProxyBox.addChangeListener(new BasicButtonListener(isProxyBox)
            {
                public void stateChanged(ChangeEvent event)
                {
                    JCheckBox box = (JCheckBox) event.getSource();
                    boolean state = box.isSelected();

                    tPortField.setEnabled(!state);
                    hostField.setEnabled(!state);
                }
            }
            );

            isProxyBox.setSelected(isProxy);

            portField.setEditable(false);
            portField.setMaximumSize(new Dimension(50, Short.MAX_VALUE));
            hostField.setEditable(false);
            hostField.setMaximumSize(new Dimension(85, Short.MAX_VALUE));
            tPortField.setEditable(false);
            tPortField.setMaximumSize(new Dimension(50, Short.MAX_VALUE));

            stopButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    if (getMessage("stop00", "Stop").equals(event.getActionCommand())) stop();
                    if (start.equals(event.getActionCommand())) start();
                }
            }
            );

            this.add(top, BorderLayout.NORTH);

            // 2nd component is a split pane with a table on the top
            // and the request/response text areas on the bottom
            /////////////////////////////////////////////////////////////////////

            tableModel = new DefaultTableModel(new String[]{
                getMessage("state00", "State"),
                getMessage("time00", "Time"),
                getMessage("requestHost00", "Request Host"),
                getMessage("targetHost", "Target Host"),
                getMessage("request00", "Request...")
            }, 0);

            connectionTable = new JTable(1, 2);
            connectionTable.setModel(tableModel);
            connectionTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            // Reduce the STATE column and increase the REQ column
            TableColumn col;

            col = connectionTable.getColumnModel().getColumn(STATE_COLUMN);
            col.setMaxWidth(col.getPreferredWidth() / 2);
            col = connectionTable.getColumnModel().getColumn(REQ_COLUMN);
            col.setPreferredWidth(col.getPreferredWidth() * 2);


            ListSelectionModel sel = connectionTable.getSelectionModel();


            tableModel.addRow(new Object[]{
                "---", getMessage("mostRecent00", "Most Recent"), "---", "---", "---"
            }
            );

            JPanel tablePane = new JPanel();

            tablePane.setLayout(new BorderLayout());

            JScrollPane tableScrollPane = new JScrollPane(connectionTable);

            tablePane.add(tableScrollPane, BorderLayout.CENTER);
            JPanel buttons = new JPanel();

            buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
            buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            final String removeSelected = getMessage("removeSelected00", "Remove Selected");

            buttons.add(removeButton = new JButton(removeSelected));
            buttons.add(Box.createRigidArea(new Dimension(5, 0)));
            final String removeAll = getMessage("removeAll00", "Remove All");

            buttons.add(removeAllButton = new JButton(removeAll));
            tablePane.add(buttons, BorderLayout.SOUTH);

            removeButton.setEnabled(false);
            removeButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    if (removeSelected.equals(event.getActionCommand())) remove();
                }
            }
            );

            removeAllButton.setEnabled(false);
            removeAllButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    if (removeAll.equals(event.getActionCommand())) removeAll();
                }
            }
            );

            // Add Response Section
            /////////////////////////////////////////////////////////////////////
            JPanel pane2 = new JPanel();

            pane2.setLayout(new BorderLayout());

            leftPanel = new JPanel();
            leftPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
            leftPanel.add(new JLabel("  " + getMessage("request01", "Request")));
            leftPanel.add(new JLabel(" " + getMessage("wait01", "Waiting for connection")));

            rightPanel = new JPanel();
            rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
            rightPanel.add(new JLabel("  " + getMessage("response00", "Response")));
            rightPanel.add(new JLabel(""));

            outPane = new JSplitPane(0, leftPanel, rightPanel);
            outPane.setDividerSize(4);
            pane2.add(outPane, BorderLayout.CENTER);

            JPanel bottomButtons = new JPanel();

            bottomButtons.setLayout(new BoxLayout(bottomButtons, BoxLayout.X_AXIS));
            bottomButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            bottomButtons.add(xmlFormatBox = new JCheckBox(getMessage("xmlFormat00", "XML Format")));
            bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));

            final String save = getMessage("save00", "Save");
            bottomButtons.add(saveButton = new JButton(save));
            bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));

            final String resend = getMessage("resend00", "Resend");
            bottomButtons.add(resendButton = new JButton(resend));
            bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));

            final String sendStr = getMessage("send", "Send");
            bottomButtons.add(sendButton = new JButton(sendStr));
            bottomButtons.add(Box.createRigidArea(new Dimension(5, 0)));

            final String switchStr = getMessage("switch00", "Switch Layout");
            bottomButtons.add(switchButton = new JButton(switchStr));
            bottomButtons.add(Box.createHorizontalGlue());

            final String close = getMessage("close00", "Close");
            bottomButtons.add(closeButton = new JButton(close));
            pane2.add(bottomButtons, BorderLayout.SOUTH);

            saveButton.setEnabled(false);
            saveButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    if (save.equals(event.getActionCommand())) save();
                }
            }
            );

            resendButton.setEnabled(false);
            resendButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    if (resend.equals(event.getActionCommand())) resend();
                }
            }
            );

            switchButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    if (switchStr.equals(event.getActionCommand()))
                    {
                        int v = outPane.getOrientation();

                        if (v == 0)  // top/bottom
                            outPane.setOrientation(1);
                        else  // left/right
                            outPane.setOrientation(0);
                        outPane.setDividerLocation(0.5);
                    }
                }
            }
            );

            closeButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    if (close.equals(event.getActionCommand()))
                        close();
                }
            }
            );

            JSplitPane pane1 = new JSplitPane(0);

            pane1.setDividerSize(4);
            pane1.setTopComponent(tablePane);
            pane1.setBottomComponent(pane2);
            pane1.setDividerLocation(150);
            this.add(pane1, BorderLayout.CENTER);

            //
            ////////////////////////////////////////////////////////////////////
            sel.setSelectionInterval(0, 0);
            outPane.setDividerLocation(150);
            notebook.addTab(name, this);

              sel.addListSelectionListener(new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent event)
                {
                    if (event.getValueIsAdjusting()) return;
                    ListSelectionModel m = (ListSelectionModel) event.getSource();
                    int divLoc = outPane.getDividerLocation();

                    if (m.isSelectionEmpty())
                    {
                        setLeft(new JLabel(" " + getMessage("wait00", "Waiting for Connection...")));
                        setRight(new JLabel(""));
                        removeButton.setEnabled(false);
                        removeAllButton.setEnabled(false);
                        saveButton.setEnabled(false);
                        resendButton.setEnabled(false);
                        sendButton.setEnabled(false);
                    }
                    else
                    {
                        int row = m.getLeadSelectionIndex();

                        if (row == 0)
                        {
                            if (connections.size() == 0)
                            {
                                setLeft(new JLabel(" " + getMessage("wait00", "Waiting for connection...")));
                                setRight(new JLabel(""));
                                removeButton.setEnabled(false);
                                removeAllButton.setEnabled(false);
                                saveButton.setEnabled(false);
                                resendButton.setEnabled(false);
                                sendButton.setEnabled(false);
                            }
                            else
                            {
                                Connection conn = (Connection) connections.lastElement();

                                setLeft(conn.inputScroll);
                                setRight(conn.outputScroll);
                                removeButton.setEnabled(false);
                                removeAllButton.setEnabled(true);
                                saveButton.setEnabled(true);
                                resendButton.setEnabled(true);
                            }
                        }
                        else
                        {
                            Connection conn = (Connection) connections.get(row - 1);

                            setLeft(conn.inputScroll);
                            setRight(conn.outputScroll);
                            removeButton.setEnabled(true);
                            removeAllButton.setEnabled(true);
                            saveButton.setEnabled(true);
                            resendButton.setEnabled(true);
                        }
                    }
                    outPane.setDividerLocation(divLoc);
                }
            }
            );

            start();
        }

        public void setLeft(Component left)
        {
            leftPanel.removeAll();
            leftPanel.add(left);
        }

        public void setRight(Component right)
        {
            rightPanel.removeAll();
            rightPanel.add(right);
        }

        public void start()
        {
            int port = Integer.parseInt(portField.getText());

            portField.setText("" + port);
            int i = notebook.indexOfComponent(this);

            notebook.setTitleAt(i, getMessage("port01", "Port") + " " + port);

            int tmp = Integer.parseInt(tPortField.getText());

            tPortField.setText("" + tmp);

            sw = new SocketWaiter(this, port);
            stopButton.setText(getMessage("stop00", "Stop"));

            portField.setEditable(false);
            hostField.setEditable(false);
            tPortField.setEditable(false);
            isProxyBox.setEnabled(false);
        }

        public void close()
        {
            stop();
            notebook.remove(this);
        }

        public void stop()
        {
            try
            {
                for (int i = 0; i < connections.size(); i++)
                {
                    Connection conn = (Connection) connections.get(i);

                    conn.halt();
                }
                sw.halt();
                stopButton.setText(getMessage("start00", "Start"));
                portField.setEditable(true);
                hostField.setEditable(true);
                tPortField.setEditable(true);
                isProxyBox.setEnabled(true);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        public void remove()
        {
            ListSelectionModel lsm = connectionTable.getSelectionModel();
            int bot = lsm.getMinSelectionIndex();
            int top = lsm.getMaxSelectionIndex();

            for (int i = top; i >= bot; i--)
            {
                ((Connection) connections.get(i - 1)).remove();
            }
            if (bot > connections.size()) bot = connections.size();
            lsm.setSelectionInterval(bot, bot);
        }

        public void removeAll()
        {
            ListSelectionModel lsm = connectionTable.getSelectionModel();
            lsm.clearSelection();
            while (connections.size() > 0)
                ((Connection) connections.get(0)).remove();

            lsm.setSelectionInterval(0, 0);
        }

        public void save()
        {
            JFileChooser dialog = new JFileChooser(".");
            int rc = dialog.showSaveDialog(this);

            if (rc == JFileChooser.APPROVE_OPTION)
            {
                try
                {
                    File file = dialog.getSelectedFile();
                    FileOutputStream out = new FileOutputStream(file);

                    ListSelectionModel lsm = connectionTable.getSelectionModel();

                    rc = lsm.getLeadSelectionIndex();
                    if (rc == 0) rc = connections.size();
                    Connection conn = (Connection) connections.get(rc - 1);

                    rc = Integer.parseInt(portField.getText());
                    out.write((new String(getMessage("listenPort01", "Listen Port:") + " " + rc + "\n")).getBytes());
                    out.write((new String(getMessage("targetHost01", "Target Host:") + " " + hostField.getText() +
                            "\n")).getBytes());
                    rc = Integer.parseInt(tPortField.getText());
                    out.write((new String(getMessage("targetPort01", "Target Port:") + " " + rc + "\n")).getBytes());

                    out.write((new String("==== " + getMessage("request01", "Request") + " ====\n")).getBytes());
                    out.write(conn.inputText.getText().getBytes());

                    out.write((new String("==== " + getMessage("response00", "Response") + " ====\n")).getBytes());
                    out.write(conn.outputText.getText().getBytes());

                    out.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

        public void resend()
        {
            int rc;

            try
            {
                ListSelectionModel lsm = connectionTable.getSelectionModel();

                rc = lsm.getLeadSelectionIndex();
                if (rc == 0) rc = connections.size();
                Connection conn = (Connection) connections.get(rc - 1);

                if (rc > 0)
                {
                    lsm.clearSelection();
                    lsm.setSelectionInterval(0, 0);
                }

                InputStream in = null;
                String text = conn.inputText.getText();

                text = fixContentLengthHeaders(text);

                in = new ByteArrayInputStream(text.getBytes());
                new Connection(this, in);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

    }


    public TCPInterceptor(int listenPort, String targetHost, int targetPort)
    {
        super(getMessage("tcpint00", "TCPInterceptor"));

        notebook = new JTabbedPane();
        this.getContentPane().add(notebook);

        new AdminPage(notebook, getMessage("admin00", "Admin"));

        if (listenPort != 0)
        {
            Listener l = null;

            if (targetHost == null)
                l = new Listener(notebook, null, listenPort,
                        targetHost, targetPort, true);
            else
                l = new Listener(notebook, null, listenPort,
                        targetHost, targetPort, false);
            notebook.setSelectedIndex(1);

            l.HTTPProxyHost = System.getProperty("http.proxyHost");
            if (l.HTTPProxyHost != null && l.HTTPProxyHost.equals(""))
                l.HTTPProxyHost = null;

            if (l.HTTPProxyHost != null)
            {
                String tmp = System.getProperty("http.proxyPort");

                if (tmp != null && tmp.equals("")) tmp = null;
                if (tmp == null)
                    l.HTTPProxyPort = 80;
                else
                    l.HTTPProxyPort = Integer.parseInt(tmp);
            }
        }

        this.pack();
        this.setSize(800, 600);
        this.setVisible(true);
    }

    protected void processWindowEvent(WindowEvent event)
    {
        switch (event.getID())
        {
            case WindowEvent.WINDOW_CLOSING:
                exit();
                break;

            default:
                super.processWindowEvent(event);
                break;
        }
    }

    /**
     * A utility method that sorts out the correct content length headers
     * for a post/get request.
     * @param text the http text to correct (or simply confirm).
     * @return the text, modified with correct content length
     */
    private static String fixContentLengthHeaders(String text)
    {
        // Fix Content-Length HTTP headers
        if (text.startsWith("POST ") || text.startsWith("GET "))
        {
            int pos1, pos2, pos3;
            String body, headers, headers1, header2;

            pos3 = text.indexOf("\n\n");
            if (pos3 == -1)
            {
                pos3 = text.indexOf("\r\n\r\n");
                if (pos3 != -1) pos3 = pos3 + 4;
            }
            else
                pos3 += 2;

            headers = text.substring(0, pos3);

            pos1 = headers.indexOf("Content-Length:");
            if (pos1 != -1)
            {
                int newLen = text.length() - pos3;

                pos2 = headers.indexOf("\n", pos1);

                text = headers.substring(0, pos1) +
                        "Content-Length: " + newLen + "\n" +
                        headers.substring(pos2 + 1) +
                        text.substring(pos3);
            }
        }
        else
            System.err.println("Error - asked to fix content length for a bad request (no POST or GET)");
        return text;
    }


    private void exit()
    {
        System.exit(0);
    }

    public void setInputPort(int port)
    {
    }

    public void setOutputHostPort(char hostName, int port)
    {
    }

    public static void main(String[] args)
    {
        try
        {
            if (args.length == 3)
            {
                int p1 = Integer.parseInt(args[0]);
                int p2 = Integer.parseInt(args[2]);

                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new TCPInterceptor(p1, args[1], p2);
            }
            else if (args.length == 1)
            {
                int p1 = Integer.parseInt(args[0]);

                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new TCPInterceptor(p1, null, 0);
            }
            else if (args.length != 0)
            {
                System.err.println(getMessage("usage00", "Usage:") + " TCPInterceptor [listenPort targetHost targetPort]\n");
            }
            else
            {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new TCPInterceptor(8080, "localhost", 8888);
            }
        }
        catch (Throwable exp)
        {
            exp.printStackTrace();
        }
    }

    // Message resource bundle.
    private static ResourceBundle messages = null;

    /**
     * Get the message with the given key.  There are no arguments for this message.
     */
    public static String getMessage(String key, String defaultMsg)
    {
        try
        {
            if (messages == null)
            {
                initializeMessages();
            }
            return messages.getString(key);
        }
        catch (Throwable t)
        {
            // If there is any problem whatsoever getting the internationalized
            // message, return the default.
            return defaultMsg;
        }
    } // getMessage

    /**
     * Load the resource bundle messages from the properties file.  This is ONLY done when it is
     * needed.  If no messages are printed (for example, only Wsdl2java is being run in non-
     * verbose mode) then there is no need to read the properties file.
     */
    private static void initializeMessages()
    {
        messages = ResourceBundle.getBundle("org.apache.axis.utils.TCPInterceptor");
    } // initializeMessages

}
