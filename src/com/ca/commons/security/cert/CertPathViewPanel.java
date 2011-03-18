
package com.ca.commons.security.cert;

import com.ca.commons.cbutil.CBToolBarButton;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;

import java.security.cert.*;

/**
 * Third tab: Display certification path of the certificate.
 * Certification path as Tree, &View Certificate... button.
 * Certificate status box.
 *
 * @author vbui
 */
public class CertPathViewPanel extends JPanel
{
	private X509Certificate cert = null;

	private JPanel certPathPanel = new JPanel();
	private JTree certpath = new JTree();
	private CBToolBarButton viewCertButton = new CBToolBarButton("&View Certificate...", null);
	private JLabel certStatusLabel = new JLabel("Certificate status:");
	private JTextArea certStatusText = new JTextArea(2, 20);

	/**
	 * Constructor.
	 */
	public CertPathViewPanel(X509Certificate cert)
	{
		this.cert = cert;

		// set cert path tree icon to certIcon
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setLeafIcon(CertViewer.certIcon);
		renderer.setOpenIcon(CertViewer.certIcon);
		renderer.setClosedIcon(CertViewer.certIcon);
		certpath.setCellRenderer(renderer);

		// prepare visual elements
		Border etchedBorder = BorderFactory.createEtchedBorder();

		Border certPathBorder = BorderFactory.createTitledBorder(
				etchedBorder, "Certification Path");
		certPathPanel.setBorder(certPathBorder);

		viewCertButton.setWidthHeight(110, 23);
		viewCertButton.setEnabled(false);

		if (cert != null)
		{
			String ssubject = CertViewer.getMostSignificantName(cert.getSubjectX500Principal().getName());
			String iissuer = CertViewer.getMostSignificantName(cert.getIssuerX500Principal().getName());

			DefaultMutableTreeNode issuer =
					new DefaultMutableTreeNode(iissuer);
			DefaultMutableTreeNode subject =
					new DefaultMutableTreeNode(ssubject);

			if (!iissuer.equals(ssubject))
				issuer.add(subject);

			certpath.setModel(new DefaultTreeModel(issuer));

			try
			{
				cert.checkValidity();
				certStatusText.setText("This certificate is OK.");
			}
			catch (CertificateExpiredException ex)
			{
				certStatusText.setText("This certificate is expired.");
			}
			catch (CertificateNotYetValidException ex)
			{
				certStatusText.setText("This certificate is not yet valid.");
			}
		}

		// layout
		certPathPanel.setLayout(new GridBagLayout());
		certPathPanel.add(new JScrollPane(certpath), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(3, 6, 6, 6), 0, 0));

		/*
		certPathPanel.add(viewCertButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 6, 7, 6), 0, 0));
		*/

		setLayout(new GridBagLayout());
		add(certPathPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(10, 10, 10, 10), 0, 0));
		add(certStatusLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 13, 2, 11), 0, 0));
		add(new JScrollPane(certStatusText), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(0, 12, 10, 11), 0, 0));

		// add listeners
		viewCertButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				viewCertButton_actionPerformed(e);
			}
		});
	}

	/**
	 * View cert action.
	 */
	private void viewCertButton_actionPerformed(ActionEvent e)
	{
		System.out.println("View a certificate in certification path");
	}
}
