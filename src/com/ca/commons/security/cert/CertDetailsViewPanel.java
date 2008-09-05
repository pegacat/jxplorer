
package com.ca.commons.security.cert;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.util.*;
import java.io.*;
import java.lang.String;

import java.security.cert.*;
import java.security.*;

import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;

import com.ca.commons.security.asn1.*;
import com.ca.commons.security.cert.extensions.*;
import com.ca.commons.cbutil.*;

/**
 * Second tab: Display details information about the certificate.
 * Show combo <All>, Version 1 Fields Only, Extensions Only, Critical
 * Extensions Only, Properties Only. Field-Value table, Field View text area,
 * &Edit Properties... button disabled, &Copy to File... button.
 *
 * @author vbui
 */
public class CertDetailsViewPanel extends JPanel
{
	private X509Certificate cert = null;

	// visual elements
	private CBToolBarButton editPropertiesButton = new CBToolBarButton("&Edit Properties...", "Edit Properties...");
	private CBToolBarButton copyToFileButton = new CBToolBarButton("&Copy to File...", "Copy to File...");

	private JLabel showLabel = new JLabel("Show:");

	private String[] showOptions =
			{
					"<All>", "Version 1 Fields Only",
					"Extensions Only", "Critical Extensions Only",
					"Properties Only"
			};
	private JComboBox showCombo = new JComboBox(showOptions);

	// table to display cert details with sample data
	private JTable certDetailsTable = new JTable();
	private JTextArea fieldText = new JTextArea(5, 20);

	/**
	 * Constructor.
	 */
	public CertDetailsViewPanel(X509Certificate cert)
	{
		this.cert = cert;

		// certDetailsTable cell renderer
		certDetailsTable.setDefaultRenderer(JLabel.class,
                                 new LabelRenderer(true));

		// increase row height to fit labels with graphics
		certDetailsTable.setRowHeight(19);

		certDetailsTable.setShowGrid(false);

		// prepare visual elements
		editPropertiesButton.setWidthHeight(110, 23);
		copyToFileButton.setWidthHeight(110, 23);

		editPropertiesButton.setEnabled(false);

		// fieldText.setFont(new Font("Monospaced", Font.PLAIN, 11));

		showCombo.setPreferredSize(new Dimension(200, 23));
		showCombo.setMinimumSize(new Dimension(200, 23));

		certDetailsTable.setModel(new CertDetailsTableModel(cert, 0));
		certDetailsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		certDetailsTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				certDetailsTable_selectionChanged(e);
			}
		});

		// layout
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		// buttonsPanel.add(editPropertiesButton);

		buttonsPanel.add(copyToFileButton);

		setLayout(new GridBagLayout());
		add(showLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(10, 10, 10, 10), 0, 0));
		add(showCombo, new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(10, 0, 10, 10), 0, 0));
		add(new JScrollPane(certDetailsTable), new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 10, 10, 10), 0, 0));
		add(new JScrollPane(fieldText), new GridBagConstraints(0, 2, 3, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(5, 10, 6, 10), 0, 0));
		add(buttonsPanel, new GridBagConstraints(0, 3, 3, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(0, 10, 6, 5), 0, 0));

		// add listeners
		editPropertiesButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				editPropertiesButton_actionPerformed(e);
			}
		});

		copyToFileButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				copyToFileButton_actionPerformed(e);
			}
		});

		showCombo.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				showCombo_itemStateChanged(e);
			}
		});
	}

	/**
	 * Edit properties action.
	 */
	private void editPropertiesButton_actionPerformed(ActionEvent e)
	{
		System.out.println("Edit the selected property ");
	}

	/**
	 * Copy to file action.
	 */
	private void copyToFileButton_actionPerformed(ActionEvent e)
	{
		String filename = CBUtility.chooseFileToSave(this, "Please specify a location to save this certificate",
				new String[] {"der"}, "Certificate file (*.der)");
		if (filename == null) return;

		if (!filename.toLowerCase().endsWith(".der"))
			filename = filename + ".der";

		if (!CBUtility.okToWriteFile(CBUtility.getParentFrame(this), filename))
		{
			return;
		}

		try
		{
			FileOutputStream fos = new FileOutputStream(filename);
			byte[] derout = cert.getEncoded();
			fos.write(derout);
			fos.close();
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this,
					ex.getMessage(),
					"Error!", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Selection on the cert details table changed.
	 */
	private void certDetailsTable_selectionChanged(ListSelectionEvent e)
	{
		int selectedRow = certDetailsTable.getSelectedRow();
		if (selectedRow >= 0)
		{
			String selectedValue=certDetailsTable.getValueAt(selectedRow, 1).toString();
			CertDetailsTableModel model = (CertDetailsTableModel) certDetailsTable.getModel();

			Object details = model.getDetails(selectedValue);

			if (details != null)
				fieldText.setText(details.toString());
			else
				fieldText.setText(selectedValue);
		}
	}

	/**
	 * Selection on the show combo box changed.
	 */
	private void showCombo_itemStateChanged(ItemEvent e)
	{
		((CertDetailsTableModel) certDetailsTable.getModel()).
				setViewMode(showCombo.getSelectedIndex());
		fieldText.setText("");
	}
}

class LabelRenderer extends JLabel implements TableCellRenderer
{
	Border unselectedBorder = null;
	Border selectedBorder = null;
	boolean isBordered = true;

	public LabelRenderer(boolean isBordered)
	{
		super();
		this.isBordered = isBordered;
		setOpaque(true); //MUST do this for background to show up.
	}

	public Component getTableCellRendererComponent(
							JTable table, Object label,
							boolean isSelected, boolean hasFocus,
							int row, int column)
	{
		setIcon(((JLabel) label).getIcon());
		setText(((JLabel) label).getText());
		setBackground(Color.white);

		if (isBordered)
		{
			if (isSelected)
			{
				if (selectedBorder == null)
				{
					selectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
											  table.getSelectionBackground());
				}
				setBorder(selectedBorder);
			}
			else
			{
				if (unselectedBorder == null)
				{
					unselectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
											  table.getBackground());
				}
				setBorder(unselectedBorder);
			}
		}
		return this;
	}
}

class CertDetailsTableModel extends AbstractTableModel
{
	private String[] headers =
			{
				"Field", "Value"
			};

	private Vector data = new Vector();
	private X509Certificate cert = null;

	private Hashtable briefDetails = new Hashtable();

	public Object getDetails(String brief)
	{
		return briefDetails.get(brief);
	}

	/**
	 * Table Model Constructor
	 */
	public CertDetailsTableModel(X509Certificate cert, int viewMode)
	{
		this.cert = cert;
		setViewMode(viewMode);
	}

	/**
	 * Sets the view mode in CertDetailsViewPanel.  The viewMode determines
	 * which attributes and extensions are shown. <br>
	 * View Modes: <br>
	 * <item>
	 * <li> 0 All
	 * <li> 1 X509 V1 attributes only
	 * <li> 2 Extensions only
	 * <li> 3 Critical Extensions only
	 * <li> 4 Properties only
	 * </item>
	 */
	public void setViewMode(int viewMode)
	{
		data = new Vector();

		if (cert == null)
			return;

		if (viewMode == 0 || viewMode == 1)
		{
			Vector versionRow = new Vector();
			versionRow.add(new JLabel("Version", CertViewer.attributeIcon, SwingConstants.LEFT));
			versionRow.add("V" + cert.getVersion());
			data.add(versionRow);

			Vector serialNumberRow = new Vector();
			serialNumberRow.add(new JLabel("Serial Number", CertViewer.attributeIcon, SwingConstants.LEFT));
			serialNumberRow.add(CBParse.bytes2Hex(cert.getSerialNumber().toByteArray()));
			data.add(serialNumberRow);

			Vector sigAlgRow = new Vector();
			sigAlgRow.add(new JLabel("Signature Algorithm", CertViewer.attributeIcon, SwingConstants.LEFT));
			sigAlgRow.add(cert.getSigAlgName());
			data.add(sigAlgRow);

			Vector issuerRow = new Vector();
			issuerRow.add(new JLabel("Issuer", CertViewer.attributeIcon, SwingConstants.LEFT));
			issuerRow.add(cert.getIssuerX500Principal().getName());
			data.add(issuerRow);

			Vector fromRow = new Vector();
			fromRow.add(new JLabel("Valid From", CertViewer.attributeIcon, SwingConstants.LEFT));
			fromRow.add(cert.getNotBefore());
			data.add(fromRow);

			Vector toRow = new Vector();
			toRow.add(new JLabel("Valid To", CertViewer.attributeIcon, SwingConstants.LEFT));
			toRow.add(cert.getNotAfter());
			data.add(toRow);

			Vector subjectRow = new Vector();
			subjectRow.add(new JLabel("Subject", CertViewer.attributeIcon, SwingConstants.LEFT));


            // debugPrint();

            subjectRow.add(cert.getSubjectX500Principal().getName());
			data.add(subjectRow);

			Vector publicKeyRow = new Vector();
			publicKeyRow.add(new JLabel("Public Key", CertViewer.attributeIcon, SwingConstants.LEFT));
			PublicKey pubKey = cert.getPublicKey();
			String publicKeyString = pubKey.getAlgorithm();
			if (pubKey instanceof RSAPublicKey)
				publicKeyString = publicKeyString + " (" + ((RSAPublicKey) pubKey).getModulus().bitLength() + " Bits)";
			else if (pubKey instanceof DSAPublicKey)
				publicKeyString = publicKeyString + " (" + ((DSAPublicKey) pubKey).getY().bitLength() + " Bits)";
			publicKeyRow.add(publicKeyString);

			/*
			if (pubKey instanceof RSAPublicKey)
				briefDetails.put(publicKeyString, CBUtility.bytes2HexSplit(((RSAPublicKey)pubKey).getModulus().toByteArray(), 4, 36));
			else
				briefDetails.put(publicKeyString, StaticUtil.bytes2Hex(pubKey.getEncoded(), 4, 36));
			*/

			data.add(publicKeyRow);
		}

		if (viewMode == 0 || viewMode == 2)
		{
			Set nonCritSet = cert.getNonCriticalExtensionOIDs();
			if (nonCritSet != null && !nonCritSet.isEmpty())
			{
				for (Iterator i = nonCritSet.iterator(); i.hasNext();)
				{
					String oid = (String)i.next();
					Vector nonCritRow = new Vector();
					String extname = getNameFromOID(oid);
					nonCritRow.add(new JLabel(extname, CertViewer.extensionIcon, SwingConstants.LEFT));
					addExtDetails(nonCritRow,
							printext(extname, cert.getExtensionValue(oid)).toString());
					data.add(nonCritRow);
				}
			}
		}

		if (viewMode == 0 || viewMode == 2 || viewMode == 3)
		{
			Set critSet = cert.getCriticalExtensionOIDs();
			if (critSet != null && !critSet.isEmpty())
			{
				for (Iterator i = critSet.iterator(); i.hasNext();)
				{
					String oid = (String)i.next();
					Vector critRow = new Vector();
					String extname = getNameFromOID(oid);
					critRow.add(new JLabel(extname, CertViewer.criticalExtensionIcon, SwingConstants.LEFT));
					addExtDetails(critRow,
							printext(extname, cert.getExtensionValue(oid)).toString());
					data.add(critRow);
				}
			}
		}

		if (viewMode == 0 || viewMode == 4)
		{
			Vector thumbprintAlgorithmRow = new Vector();
			thumbprintAlgorithmRow.add(new JLabel("Thumbprint Algorithm", CertViewer.thumbprintIcon, SwingConstants.LEFT));
			thumbprintAlgorithmRow.add("sha1");
			data.add(thumbprintAlgorithmRow);

			try
			{
				Vector thumbprintRow = new Vector();
				thumbprintRow.add(new JLabel("Thumbprint", CertViewer.thumbprintIcon, SwingConstants.LEFT));
				MessageDigest md = MessageDigest.getInstance("SHA");
				byte[] hash = md.digest(cert.getEncoded());
				thumbprintRow.add(CBParse.bytes2HexSplit(hash, 4));
				data.add(thumbprintRow);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		fireTableStructureChanged();
	}

    private void debugPrint()
    {
        System.out.println("get dn: " + cert.getSubjectDN());
        System.out.println("get dn name: " + cert.getSubjectDN().getName());
        System.out.println("getName: " + cert.getSubjectX500Principal().getName());
        System.out.println("rfc2253: " + cert.getSubjectX500Principal().getName("RFC2253"));
        System.out.println("canonical: " +cert.getSubjectX500Principal().getName("CANONICAL"));
        System.out.println("toString: " + cert.getSubjectX500Principal().toString());
        System.out.print("der: ");
        byte[] bytes = cert.getSubjectX500Principal().getEncoded();
        for (int i=0; i<bytes.length; i++)
            System.out.print(" " + bytes[i]);
        String name = cert.getSubjectX500Principal().getName();
        System.out.println();
        System.out.println("default: ");

        bytes = name.getBytes();
        System.out.println("straight name: " + CBParse.bytes2Hex(bytes));
        try
        {
            System.out.println("unicode: " + CBParse.bytes2Hex(name.getBytes("UTF-16")));
            System.out.println("utf-8: " + CBParse.bytes2Hex(name.getBytes("UTF-8")));
        }
        catch (UnsupportedEncodingException e2)
        {
            e2.printStackTrace();
        }

        System.out.println();
    }

    private void addExtDetails(Vector row, String extDetails)
	{
		StringTokenizer tok = new StringTokenizer(extDetails, "\n");
		if (tok.countTokens() > 1)
		{
			String brief = tok.nextToken();
			row.addElement(brief);
			briefDetails.put(brief, extDetails);
		}
		else
		{
			row.addElement(extDetails);
		}
	}

	private String getNameFromOID(String oid)
	{
		return ASN1OID.getName(oid);
	}

	private Object printext(String extname, byte[] extvalue)
	{
		try
		{
			DERCoder derCoder = new DERCoder();
			ASN1Object ext = derCoder.decode(extvalue);
			ASN1Object rext = null;

			if (ext.isASN1Type(ASN1Type.OCTET_STRING))
				rext = derCoder.decode((byte[])ext.getValue());

			V3Extension v3e = null;
			boolean done = false;

			if (extname.equals("Authority Information Access"))
			{
				v3e = new AuthorityInfoAccess();
				done = true;
			}
			else if (extname.equals("Subject Key Identifier"))
			{
				v3e = new SubjectKeyIdentifier();
				done = true;
			}
			else if (extname.equals("Key Usage"))
			{
				v3e = new KeyUsage();
				done = true;
			}
			else if (extname.equals("Subject Alternative Name"))
			{
				v3e = new SubjectAltName();
				done = true;
			}
			else if (extname.equals("Issuer Alternative Name"))
			{
				v3e = new IssuerAltName();
				done = true;
			}
			else if (extname.equals("Basic Constraints"))
			{
				v3e = new BasicConstraints();
				done = true;
			}
			else if (extname.equals("Name Constraints"))
			{
				v3e = new NameConstraints();
			}
			else if (extname.equals("Certificate Policies"))
			{
				v3e = new CertificatePolicies();
				done = true;
			}
			else if (extname.equals("Policy Mappings"))
			{
				v3e = new PolicyMappings();
			}
			else if (extname.equals("Authority Key Identifier"))
			{
				v3e = new AuthorityKeyIdentifier();
				done = true;
			}
			else if (extname.equals("Policy Constraints"))
			{
				v3e = new PolicyConstraints();
			}
			else if (extname.equals("Extended Key Usage"))
			{
				v3e = new ExtendedKeyUsage();
				done = true;
			}
			else if (extname.equals("CRL Distribution Points"))
			{
				v3e = new CRLDistributionPoints();
				done = true;
			}
			else if (extname.equals("Private Key Usage Period"))
			{
				v3e = new PrivateKeyUsagePeriod();
				done = true;
			}
			else if (extname.equals("Netscape Cert Type"))
			{
				v3e = new NetscapeCertType();
				done = true;
			}
			else
			{
				// v3e = new UnknownExtension(new ObjectID(oid, extname));
			}

			if (!done)
			{
				System.out.println("extname: " + extname);
				System.out.println("exttype: " + rext);
				System.out.println("extvalu: " + rext.getValue());
				System.out.println("extbyte: " + CBParse.bytes2HexSplit((byte[])ext.getValue(), 4, 36));
			}

			if (v3e != null)
			{
				v3e.init(rext);
				if (v3e.toString() == null)
					throw new Exception("Could not read extension: " + extname);
				return v3e;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		return CBParse.bytes2HexSplit(extvalue, 4, 36);
	}

	public int getRowCount()
	{
		return data.size();
	}

	public int getColumnCount()
	{
		return 2;
	}

	public String getColumnName(int col)
	{
		return headers[col];
	}

	public Object getValueAt(int row, int col)
	{
		return ((Vector)data.elementAt(row)).elementAt(col);
	}

	public Class getColumnClass(int col)
	{
		return getValueAt(0, col).getClass();
	}
}
