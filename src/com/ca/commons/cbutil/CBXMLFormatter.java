package com.ca.commons.cbutil;

import org.w3c.dom.*;

import java.io.*;

/**
 * <p>Takes a DOM tree and prints it out
 * as nicely formatted XML</p>
 */
public class CBXMLFormatter
{

    /**
     * use four spaces as default
     */
    private String tab = "    ";

    /**
     * Line feed to use... could use
     * different one for mac but can't be bothered :-)
     */
    private String newLine = System.getProperty("line.separator");

    /**
     * reusable serializer for static methods
     */
    private static CBXMLFormatter printer;

    /**
     * reusable writer for static methods
     */
    private static Writer writer;

    public static void print(Node element)
    {
        if (printer == null)
            printer = new CBXMLFormatter();

        if (writer == null)
            writer = new OutputStreamWriter(System.out);

        try
        {
            printer.serializeNode(element, writer, "    ");
            writer.flush();
        }
        catch (IOException e)
        {
            System.err.println("IO exception writing to stdout: " + e);
        }
    }

    public static String dumpToString(Node element)
    {
        if (printer == null)
            printer = new CBXMLFormatter();

        if (writer == null)
            writer = new OutputStreamWriter(System.out);

        try
        {
            return printer.serializeNode(element, new StringBuffer(), "    ").toString();
        }
        catch (IOException e)
        {
            System.err.println("IO exception writing to stdout: " + e);
        }

        return "<error/>";
    }

    /**
     * empty constructor
     */
    public CBXMLFormatter()
    {
    }

    /**
     * <p> This sets the tab size to use. </p>
     *
     * @param tab the tab size as an example string (e.g. '    ').
     */
    public void setTab(String tab)
    {
        this.tab = tab;
    }

    /**
     * <p> This sets the carriage return character(s) to use.
     * (default is '\n')
     * </p>
     *
     * @param newLine the CR to use.
     */
    public void setNewLine(String newLine)
    {
        this.newLine = newLine;
    }

    /**
     * <p> This serializes the DOM and prints
     * it to standard out.</p>
     *
     * @param domTree DOM tree to printXML.
     */

    public void printXML(Document domTree)
            throws IOException
    {

        Writer writer = new OutputStreamWriter(System.out);
        printXML(domTree, writer);
    }

    /**
     * <p> This serializes the DOM and sends it
     * to the suplied output stream</p>
     *
     * @param domTree DOM tree to printXML.
     * @param out     <code>OutputStream</code> to write to.
     */

    public void printXML(Document domTree, OutputStream out)
            throws IOException
    {

        Writer writer = new OutputStreamWriter(out);
        printXML(domTree, writer);
    }

    /**
     * <p> This serializes a DOM tree and sends it to
     * the provided writer.</p>
     *
     * @param domTree DOM tree to printXML.
     * @param writer  a Writer object to output to.
     */

    public void printXML(Document domTree, Writer writer)
            throws IOException
    {

        // Start serialization recursion with no indenting
        serializeNode(domTree, writer, "");
        writer.flush();
    }

    /**
     * <p> This will printXML the DOM subtree of
     * the provided node. </p>
     *
     * @param node   the document node to printXML.
     * @param writer the output Writer to output text to.
     * @param tabs   size text is inset.
     */
    public void serializeNode(Node node, Writer writer,
                              String tabs)
            throws IOException
    {
        // Determine action based on node type
        switch (node.getNodeType())
        {
            case Node.DOCUMENT_NODE:
                writer.write("<?xml version=\"1.0\"?>" + newLine);

                // recurse on each child
                NodeList nodes = node.getChildNodes();
                if (nodes != null)
                {
                    for (int nodeNo = 0; nodeNo < nodes.getLength(); nodeNo++)
                    {
                        serializeNode(nodes.item(nodeNo), writer, "");
                    }
                }
                break;

            case Node.ELEMENT_NODE:
                String name = node.getNodeName();
                writer.write(tabs + "<" + name);
                NamedNodeMap attributes = node.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++)
                {
                    Node current = attributes.item(i);
                    writer.write(" " + current.getNodeName() + "=\"" + current.getNodeValue() + "\"");
                }
                writer.write(">");

                // recurse on each child
                NodeList children = node.getChildNodes();
                if (children != null)
                {
                    if ((children.item(0) != null) &&
                            (children.item(0).getNodeType() ==
                            Node.ELEMENT_NODE))
                    {
                        writer.write(newLine);
                    }
                    for (int i = 0; i < children.getLength(); i++)
                    {
                        serializeNode(children.item(i), writer, tabs + tab);
                    }
                    if ((children.item(0) != null)
                            && (children.item(children.getLength() - 1).getNodeType() == Node.ELEMENT_NODE))
                    {
                        writer.write(tabs);
                    }
                }

                writer.write("</" + name + ">");
                writer.write(newLine);
                break;

            case Node.TEXT_NODE:
                writer.write(node.getNodeValue());
                break;

            case Node.CDATA_SECTION_NODE:
                writer.write("<![CDATA[" + node.getNodeValue() + "]]>");
                break;

            case Node.COMMENT_NODE:
                writer.write(tabs + "<!-- " + node.getNodeValue() + " -->");
                writer.write(newLine);
                break;

            case Node.PROCESSING_INSTRUCTION_NODE:
                writer.write("<?" + node.getNodeName() + " " + node.getNodeValue() + "?>");
                writer.write(newLine);
                break;

            case Node.ENTITY_REFERENCE_NODE:
                writer.write("&" + node.getNodeName() + ";");
                break;

            case Node.DOCUMENT_TYPE_NODE:
                DocumentType docType = (DocumentType) node;
                writer.write("<!DOCTYPE " + docType.getName());
                if (docType.getPublicId() != null)
                {
                    System.out.print(" PUBLIC \"" + docType.getPublicId() + "\" ");
                }
                else
                {
                    writer.write(" SYSTEM ");
                }
                writer.write("\"" + docType.getSystemId() + "\">");
                writer.write(newLine);
                break;
        }
    }

    /**
     * <p> This will printXML the DOM subtree of
     * the provided node.  Formatting it for HTML. </p>
     *
     * @param node the document node to printXML.
     * @param tabs size text is inset.
     */
    public StringBuffer serializeNode(Node node, StringBuffer buffy, String tabs)
            throws IOException
    {

        // Determine action based on node type
        switch (node.getNodeType())
        {
            case Node.DOCUMENT_NODE:
                buffy.append("<?xml version=\"1.0\"?>" + newLine);

                // recurse on each child
                NodeList nodes = node.getChildNodes();
                if (nodes != null)
                {
                    for (int nodeNo = 0; nodeNo < nodes.getLength(); nodeNo++)
                    {
                        serializeNode(nodes.item(nodeNo), buffy, "");
                    }
                }
                break;

            case Node.ELEMENT_NODE:
                String name = node.getNodeName();
                buffy.append(tabs + CBParse.toHTML("<" + name));
                NamedNodeMap attributes = node.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++)
                {
                    Node current = attributes.item(i);
                    buffy.append(CBParse.toHTML(" " + current.getNodeName() + "=\"" + current.getNodeValue() + "\""));
                }
                buffy.append(CBParse.toHTML(">"));

                // recurse on each child
                NodeList children = node.getChildNodes();
                if (children != null)
                {
                    if ((children.item(0) != null) &&
                            (children.item(0).getNodeType() ==
                            Node.ELEMENT_NODE))
                    {
                        buffy.append(newLine);
                    }
                    for (int i = 0; i < children.getLength(); i++)
                    {
                        serializeNode(children.item(i), buffy, tabs + tab);
                    }
                    if ((children.item(0) != null)
                            && (children.item(children.getLength() - 1).getNodeType() == Node.ELEMENT_NODE))
                    {
                        buffy.append(tabs);
                    }
                }

                buffy.append(CBParse.toHTML("</" + name + ">"));
                buffy.append(newLine);
                break;

            case Node.TEXT_NODE:
                buffy.append(CBParse.toHTML(node.getNodeValue()));
                break;

            case Node.CDATA_SECTION_NODE:
                buffy.append(CBParse.toHTML("<![CDATA[" + node.getNodeValue() + "]]>"));
                break;

            case Node.COMMENT_NODE:
                buffy.append(tabs + CBParse.toHTML("<!-- " + node.getNodeValue() + " -->"));
                buffy.append(newLine);
                break;

            case Node.PROCESSING_INSTRUCTION_NODE:
                buffy.append(CBParse.toHTML("<?" + node.getNodeName() + " " + node.getNodeValue() + "?>"));
                buffy.append(newLine);
                break;

            case Node.ENTITY_REFERENCE_NODE:
                buffy.append(CBParse.toHTML("&" + node.getNodeName() + ";"));
                break;

            case Node.DOCUMENT_TYPE_NODE:
                DocumentType docType = (DocumentType) node;
                buffy.append(CBParse.toHTML("<!DOCTYPE " + docType.getName()));
                if (docType.getPublicId() != null)
                {
                    System.out.print(" PUBLIC \"" + docType.getPublicId() + "\" ");
                }
                else
                {
                    buffy.append(CBParse.toHTML(" SYSTEM "));
                }
                buffy.append(CBParse.toHTML("\"" + docType.getSystemId() + "\">"));
                buffy.append(newLine);
                break;
        }

        return buffy;
    }
}