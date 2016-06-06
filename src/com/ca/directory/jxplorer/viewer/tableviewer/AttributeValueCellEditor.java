package com.ca.directory.jxplorer.viewer.tableviewer;

import com.ca.commons.cbutil.CBIntText;
import com.ca.commons.cbutil.CBJComboBox;
import com.ca.commons.cbutil.CBUtility;
import com.ca.commons.jndi.SchemaOps;
import com.ca.commons.naming.DN;
import com.ca.commons.security.cert.CertViewer;
import com.ca.directory.jxplorer.broker.DataBrokerQueryInterface;
import com.ca.directory.jxplorer.editor.*;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.EventObject;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *     The cell editor that brings up the dialog.
 *     We inherit from AbstractCellEditor,
 *     even though it means we have to create a dummy
 *     check box.  Attribute Value Editor uses schema
 *     info to validate the user's input before submission...
 */
public class AttributeValueCellEditor extends AbstractCellEditor
{
    Frame owner;

    JTextField textField = new JTextField();
    JLabel label = new JLabel("");

    CBJComboBox combobox = new CBJComboBox();

    JComponent editorComponent = textField;

    abstractbinaryeditor abstractEditor = null;    // this is the display editor for binary data - e.g. the audio player, or photo viewer

    Object value;
    boolean binaryEditFlag = false;            // handle binary editing separately
    boolean specialStringEditor = false;       // handle special string stuff like postal address
    protected ClassLoader myLoader = null;     // optional extended class loader
    public DataBrokerQueryInterface datasource = null;       //TE: The syntax of the attribute.
    public DN currentDN = null;                //TE: The dn of the entry being modified.

    int lastSelectedRow = 0;                    //TE: The last selected row - which is used to set the height back to normal (16).

    public boolean enabled = true;

    public static final String BINARY_SYNTAX =              "1.3.6.1.4.1.1466.115.121.1.5";
    public static final String BOOLEAN_SYNTAX =             "1.3.6.1.4.1.1466.115.121.1.7";
    public static final String CERTIFICATE_SYNTAX =         "1.3.6.1.4.1.1466.115.121.1.8";
    public static final String GENERALIZED_TIME_SYNTAX =    "1.3.6.1.4.1.1466.115.121.1.24";
    public static final String POSTAL_ADDRESS_SYNTAX =      "1.3.6.1.4.1.1466.115.121.1.41";

    private static Logger log = Logger.getLogger(AttributeValueCellEditor.class.getName());

   /**
    *    A basic constructor, which does little except add a
    *    mouse listener to the default text field, setting the
    *    click count for 'cancel editing' to two.
    */
    public AttributeValueCellEditor(Frame parent)
    {
        owner = parent;

	//	textField.setFont(new Font("Tahoma", Font.PLAIN,11)); //TE: makes the textField same as the label - bug 3013.

/*
		editorComponent.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
//TE: I've commented this out because a double click makes the value in the cell disappear?? Bug - 3007.
//                if (e.getClickCount() == 2)
//                    cancelCellEditing();
            }
        });
*/


    }

    //
    //  Implementing the CellEditor Interface
    //

    // implements javax.swing.table.TableCellEditor

    /**
     * Returns an awt.Component that acts as a cell editor.
     * Checks, in the following order, for a Certificate, binary syntax,
     * postalAddress, GeneralizedTime, if there are options.  If none of these
     * syntax' match then the default string editor is set.
     * This method also increases the size of the row selected so that the value
     * is easier to read.
     * @param table
     * @param value
     * @param isSelected
     * @param row
     * @param column
     * @return an awt.Component that acts as a cell editor
     */
    public Component getTableCellEditorComponent(JTable table,
                         Object value, boolean isSelected,
                         int row, int column)
    {
        binaryEditFlag = false;
        specialStringEditor = false;

        table.setRowHeight(lastSelectedRow, 16);
        table.setRowHeight(row, 24);

        lastSelectedRow = row;

        if (value instanceof AttributeValue)
        {
            AttributeValue att = (AttributeValue) value;

            if (hasSyntax(att, CERTIFICATE_SYNTAX))			    //TE: a syntax check for Certificate.
                setCertificateEditor(att);
            else if (att.isNonStringData())						    //TE: binary check.
                setBinaryEditor(att);
            else if (hasSyntax(att, POSTAL_ADDRESS_SYNTAX))	    //TE: postalAddress syntax check.
                setPostalAddressEditor(att);
            else if (hasSyntax(att, GENERALIZED_TIME_SYNTAX))   //TE: generalizedTime syntax check.
                setGeneralizedTimeEditor(att);
            else if (hasSyntax(att, BOOLEAN_SYNTAX))	        //TE: boolean syntax check.
                setBooleanEditor(att);
            else if (att.hasOptions())      				    // there are suggested possible values
                setOptions(att);
            else
                setString(att);

            setCellEditorValue(att);
        }
        return editorComponent;
    }

   /**
    *	Checks if the attribute value's syntax matches the given syntax.
    *	@param att the attribute value for example, 'Fred' from 'cn=Fred'.
    *	@param syntax the syntax to check against for example, '1.3.6.1.4.1.1466.115.121.1.8'.
	*	@return true if the syntaxes match false otherwise.
	*/
	public boolean hasSyntax(AttributeValue att, String syntax)
	{
		String attSyntax = getAttributeSyntax(att);
	   	return (attSyntax != null && attSyntax.indexOf(syntax) > -1);
	}

   /**
    *   Sets the certificate editor in the table cell whose attribute is
    *   a certificate ["1.3.6.1.4.1.1466.115.121.1.8"].
    *   @param att the attribute value to be set in the editor.
    */
    private void setCertificateEditor(AttributeValue att)
    {
        CertViewer.CertAndFileName returnVal = CertViewer.editCertificate(owner, att.getValue());
        X509Certificate cert = returnVal.cert;
        if (cert != null)
        {
            try
            {
                byte[] newData = cert.getEncoded();
                if (Arrays.equals(newData, att.getValue()) == false)
                    att.setValue(newData);
            }
            catch(Exception e)
            {
                CBUtility.error(CBIntText.get("Error: unable to modify certificate."), e);
            }
        }

        binaryEditFlag = true;

        if (att.isEmpty())
        {
            label.setText(" ");
        }
        else
        {
            label.setText(CBIntText.get("(non string data)"));
        }

        editorComponent = label;
    }

   /**
    *   Sets the string in the table cell whose attribute is
    *   a string (does a check for the length of a string also and if
    *   the string is longer than 100 it sets the large string editor).
    *   @param att the attribute value to be set in the editor.
    */
    private void setString(AttributeValue att)
    {
        String textValue = att.toString();
        if (textValue.length() > 100) // arbitrary long display limit...
        {
            setLargeStringEditor(att);
        }
        else
        {
            textValue = textValue.trim();  // XXX trim off extra space that may be there for swing printing hack...

            textField.setText(textValue);

            editorComponent = textField;
        }
    }

   /**
    *   Sets the large string editor in the table cell whose attribute value is
    *   a string longer than 100 chars.
    *   @param att the attribute value to be set in the editor.
    */
    private void setLargeStringEditor(AttributeValue att)
    {
        largestringeditor lse = new largestringeditor(owner, att);
        specialStringEditor = true;
        CBUtility.center(lse, owner);
        lse.setVisible(true);
        label.setText(att.getStringValue().substring(0,100));
        editorComponent = label;
    }

   /**
    *   Sets a combo box in the table cell whose attribute could have
    *   suggested possible values.
    *   @param att the attribute value to be set in the editor.
    */
    private void setOptions(AttributeValue att)
    {
        combobox.removeAllItems();
        String[] ops = att.getOptions();
        for (int i=0; i<ops.length; i++)
            combobox.addItem(ops[i]);
        editorComponent = combobox;
    }

   /**
    *   Sets the binary editor in the table cell whose attribute is
    *   a binary.
    *   @param att the attribute value to be set in the editor.
    */
    private void setBinaryEditor(AttributeValue att)
    {
        startBinaryEditor(att); 					// runs modal dialog binary editor
        binaryEditFlag = true;

        if (att.isEmpty())
        {
            label.setText(" ");
        }
        else
        {
            label.setText(CBIntText.get("(non string data)"));
        }

        editorComponent = label;
    }

   /**
    *   Sets the generalized time editor in the table cell whose attribute is
    *   a generalizedTime ["1.3.6.1.4.1.1466.115.121.1.24"].
    *   @param att the attribute value to be set in the editor.
    */
    private void setGeneralizedTimeEditor(AttributeValue att)
    {
        generalizedtimeeditor timeEditor = null;

        if (att==null)
        {
            timeEditor = new generalizedtimeeditor(owner,"", true);
        }
        else
        {
            timeEditor = new generalizedtimeeditor(owner, att.toString(), true);
        }

        specialStringEditor = true;
        CBUtility.center(timeEditor, owner);    	//TE: centres the attribute editor.
        timeEditor.setStringValue(att);
        timeEditor.setVisible(true);

        if (att.isEmpty())
        {
            label.setText(" ");
        }
        else
        {
            label.setText(att.getStringValue());    //TE: sets the table label to reflect the changes.
        }

        editorComponent = label;
    }

   /**
    *   Sets the postal address editor in the table cell whose attribute is
    *   a postalAddress ["1.3.6.1.4.1.1466.115.121.1.41"].
    *   @param att the attribute value to be set in the editor.
    */
    private void setPostalAddressEditor(AttributeValue att)
    {
        postaladdresseditor postalEditor = new postaladdresseditor(owner, att);
        specialStringEditor = true;
        CBUtility.center(postalEditor, owner);    	//TE: centres the attribute editor.
        postalEditor.setStringValue(att);
        postalEditor.setVisible(true);

        if (att.isEmpty())
        {
            label.setText(" ");
        }
        else
        {
            label.setText(att.getStringValue());    //TE: sets the table label to reflect the changes.
        }

        editorComponent = label;
    }

   /**
    *   Sets the postal address editor in the table cell whose attribute is
    *   a postalAddress ["1.3.6.1.4.1.1466.115.121.1.7"].
    *   @param att the attribute value to be set in the editor.
    */
    private void setBooleanEditor(AttributeValue att)
    {                              
        booleaneditor booleanEditor = new booleaneditor(owner, att);
        specialStringEditor = true;
        CBUtility.center(booleanEditor, owner);    	//TE: centres the attribute editor.
        booleanEditor.setStringValue(att);
        booleanEditor.setVisible(true);

        if (att.isEmpty())
        {
            label.setText(" ");
        }
        else
        {
            label.setText(att.getStringValue());    //TE: sets the table label to reflect the changes.
        }

        editorComponent = label;
    }

   /**
    *	Returns the syntax of the attribute value that is supplied
    *	@param att the attribute value for example 'Fred' from 'cn=Fred'.
	*	@return the syntax of the attribute for example for postalAddress the return string would be:
	*		'SYNTAX: 1.3.6.1.4.1.1466.115.121.1.41'.  If it fails to find the schema, it returns
    *       '1.3.6.1.4.1.1466.115.121.1.15' (DirectoryString) as a default.
	*/
	public String getAttributeSyntax(AttributeValue att)
	{
		String attID = att.getID();
        return getAttributeSyntaxFromName(attID);
    }

    /**
     *   This looks up the attribute syntax using the name of the attribute.
     * If none is found, it also checks to see if it is a 'SUP' of another
     * attribute, and tries to recursively get the schema from that.
     *  @param attID the name of the attribute to look up; e.g. 'cn'
     *  @return the syntax of the attribute (usually an OID, e.g. "1.3.6.1.4.1.1466.115.121.1.15")
     */
    private String getAttributeSyntaxFromName(String attID)
    {
        if (attID.indexOf(';') > 0)
            attID = attID.substring(0, attID.indexOf(';'));									//TE: for example: userCertificate;binary.

        try
        {
            if (datasource == null)
                throw new NamingException("No datasource!");

            SchemaOps schema = datasource.getSchemaOps();
            if (schema == null)
                throw new NamingException("No schema!");
                                               //TE: gets the schema.
            Attributes attSchema = schema.getAttributes("AttributeDefinition/" + attID);    //TE: gets the attribute definition.

            if (attSchema == null)
                throw new NamingException("No schema for AttributeDefinition/"+attID);

            Attribute attSyntax = attSchema.get("SYNTAX");                                  //TE: gets the Syntax of the attribute .

            if (attSyntax == null)                                              // maybe it's a 'SUP' entry...
            {
                Attribute supSchema = attSchema.get("SUP");
                if (supSchema == null)
                    throw new NamingException("Error processing attribute definition for " + attID + " no schema and no sup entry found");

                if (supSchema.get().toString().equals(attID))                   // 'SUP' is always single valued
                    throw new NamingException("recursive schema definition: " + attID + " sup of itself");

                return getAttributeSyntaxFromName(supSchema.get().toString());  // recursively find the syntax of the sup entry
            }
            else
            {
                String syntax = attSyntax.toString();
                if (syntax.startsWith("SYNTAX: "))
                    syntax = syntax.substring(8);
                return syntax;
            }
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "Problem processing attribute definition: ", e);
            return "1.3.6.1.4.1.1466.115.121.1.15";  // default to 'DirectoryString'
        }
    }

   /**
    *    Does a quick check to make the binary handling sane (if necessary)
    *    before calling the super class stopCellEditing.
    */
     // Enough casts to fill a hindu temple.
    public boolean stopCellEditing()
    {
        if (binaryEditFlag)
        {
            return super.stopCellEditing();
        }
        else if (specialStringEditor)
        {
            return super.stopCellEditing();
        }

        Object o = getCellEditorValue();
        if (o == null) return true;  // not actually editing - redundant call.

        if (o instanceof AttributeValue)
        {
            AttributeValue v = (AttributeValue)o;

            if (editorComponent instanceof JTextField)
            {
                String userData = ((JTextField)editorComponent).getText();
                // Bug 4891 - limit the user to entering only a single space.
                int len = userData.length();
                if (len > 0)
                {
                    userData.trim();
                    if (userData.length() == 0)
                        userData = " ";  // multiple spaces are shortened to a single space. Technically we should

                    if (userData.length() != len)
                        ((JTextField)editorComponent).setText(userData);
                }
                v.update(userData);
            }
            else if (editorComponent instanceof CBJComboBox)
                v.update(((CBJComboBox)editorComponent).getSelectedItem());
            else
                log.warning("unknown editorComponent = " + editorComponent.getClass());
        }
        else
            log.warning("(AttValCellEdit) Not an Att Val: is " + o.getClass());

        setCellEditorValue(o);

        return super.stopCellEditing();
    }

    public void setEnabled(boolean enable)
    {
        enabled = enable;
    }

   /**
    *    Checks if the user has clicked sufficient times to make the cell
    *    editable.
    */
    public boolean isCellEditable(EventObject e)
    {
        if (!enabled)
            return false;

        boolean editable = true;
        if (e instanceof MouseEvent)
        {
            editable = ((MouseEvent)e).getClickCount() >= clickCountToStart;
        }
        return editable;
    }

   /**
    *    Kicks of a separate binary editor by looking for a Java class with
    *    name of the attribute, plus the word 'Editor', and starting that
    *    up if it can be found.  (Otherwise it uses the default editor).
    */
    public void startBinaryEditor(AttributeValue att)
    {
        if (abstractEditor != null &&
            (abstractEditor instanceof Component) &&
            ((Component)abstractEditor).isVisible() )        //TE: checks if an editor is already open (i.e. if multiple clicks on a value in the table editor).
        {
            return;                                          //TE: do nothing...there is already an editor open.
        }

        if (att.isNonStringData()==false)  // should never happen!
            {log.warning("Error: non binary value passed to binary editor!"); return; }

        String attName = att.getID();
        int trim = attName.indexOf(";binary");
        if (trim>0)
            attName = attName.substring(0,trim);

        String className = "com.ca.directory.jxplorer.editor." + attName + "editor";

        try
        {
            Class c = null;
            if (myLoader == null)
            {
                c = Class.forName(className);
            }
            else
            {
                c = myLoader.loadClass(className.toLowerCase());
            }
            if (abstractbinaryeditor.class.isAssignableFrom(c))
            {
                Constructor constructor;
                if (owner instanceof Frame)
                {
                    constructor = c.getConstructor(new Class[] {java.awt.Frame.class});
                    abstractEditor = (abstractbinaryeditor) constructor.newInstance(new Object[] {owner});
                }
                else
                {
                    constructor = c.getConstructor(new Class[0]);
                    abstractEditor = (abstractbinaryeditor) constructor.newInstance(new Object[0]);
                }

                abstractEditor.setValue(att);
				if(abstractEditor instanceof basicbinaryeditor)		//TE: set the current DN in the editor.
					((basicbinaryeditor)abstractEditor).setDN(currentDN);
                else if(abstractEditor instanceof defaultbinaryeditor)		//TE: set the current DN in the editor.
                    ((defaultbinaryeditor)abstractEditor).setDN(currentDN);

                if (abstractEditor instanceof defaultbinaryeditor)
                {
                    ((defaultbinaryeditor)abstractEditor).showDialog(); //todo: TE:seems that the abstractEditor doesn't get set to null after it is closed...
                    abstractEditor = null;                              //todo: TE: so this is a hack to kill the dialog so that it opens the next time a user clicks on that attribute value.  It would be nice to fix this!
                }
                else if (abstractEditor instanceof Component)
                {
                    ((Component)abstractEditor).setVisible(true);
                }

                fireEditingStopped();  // everything is now being done by the separate binary editor window...
                return;  // ...and don't load default editor (below)
            }
            else
            {
                log.warning("error: can't load editor class " + className + " since it is not inherited from AbstractBinaryEditor\n");
            }

        }
        catch (NoSuchMethodException e)
        {
            log.log(Level.WARNING, "coding error in editor " + className+ " - using default binary editor instead.", e);
        }
        catch (ClassNotFoundException e)
        {
            log.info("(expected) can not find editor " + className + "\n" + e.toString()); // 'expected' error
            try
            {
                defaultbinaryeditor dbe = new defaultbinaryeditor(owner);  // kick off the default editor (creates an option dialog)
                dbe.setDN(currentDN);
                CBUtility.center(dbe, owner);    //TE: centers the window.
                dbe.setValue(att);
                dbe.showDialog();
            }
            catch (Exception e2)
            {
                log.log(Level.WARNING, "unable to start backup editor",  e2);
                e2.printStackTrace();
            }
        }
        catch (Exception e3)
        {
            log.log(Level.WARNING, "error loading editor " + className, e3);
            e3.printStackTrace();
        }
    }

   /**
    * Optionally register a new class loader for atribute value viewers to use.
    */
    public void registerClassLoader(ClassLoader loader)
    {
        myLoader = loader;
    }

   /**
    *   Returns the datasource.
    *   @param ds the datasource.
    */
    public void setDataSource(DataBrokerQueryInterface ds)
    {
        datasource = ds;
    }

   /**
    *   Sets the dn of the entry being modified.
    *   @param dn the DN of the entry being modified.
    */
    public void setDN(DN dn)
    {
        currentDN = dn;
    }

   /**
    *    Sets the the abstract editor (display editor for binary data - e.g. the audio player, or photo viewer), to null.
    */
    public void cleanupEditor()
    {
         abstractEditor = null;
    }
}
