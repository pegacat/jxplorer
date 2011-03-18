
package com.ca.commons.security.cert;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.text.SimpleDateFormat;

import java.security.cert.*;

import com.ca.commons.cbutil.CBToolBarButton;

/**
 * First tab: Display general information about the certificate.
 * Intended for, Issued to, Issued by, Valid from .. to ..,
 * &Install Certificate... button, Issuer &Statement... button disabled.
 *
 * @author vbui
 */
public class CertGeneralViewPanel extends JPanel
{
	private X509Certificate cert = null;

	// visual elements
	private CBToolBarButton installCertificateButton = new CBToolBarButton("&Install Certificate...", null);
	private CBToolBarButton issuerStatementButton = new CBToolBarButton("Issuer &Statement...", null);

	private JLabel topLabel = new JLabel("Certificate Information",
			CertViewer.certLargeIcon, SwingConstants.LEFT);

	private JLabel intendLabel = new JLabel("This certificate:");
	private JTextArea intend = new JTextArea(7, 20);
	private JLabel subjectLabel = new JLabel("Issued to:");
	private JLabel subject = new JLabel("subject");
	private JLabel issuerLabel = new JLabel("Issued by:");
	private JLabel issuer = new JLabel("issuer");
	private JLabel fromLabel = new JLabel("Valid from");
	private JLabel from = new JLabel("from");
	private JLabel toLabel = new JLabel("to");
	private JLabel to = new JLabel("to");

	/**
	 * Constructor.
	 */
	public CertGeneralViewPanel(X509Certificate cert)
	{
		this.cert = cert;

		// prepare visual elements
		installCertificateButton.setWidthHeight(110, 23);
		issuerStatementButton.setWidthHeight(110, 23);

		installCertificateButton.setEnabled(false);
		issuerStatementButton.setEnabled(false);

		intend.setPreferredSize(new Dimension(200, 150));
		intend.setMinimumSize(new Dimension(200, 150));
		intend.setMaximumSize(new Dimension(200, 150));
/*
XXX - I believe formal use of the line separator class is unnecessary - I
believe java seamlessly translates between unicode character '\n' and the
system dependant line separator... (leaving code here in case I'm wrong :-) ) - CB

		intend.setText(" Performs Windows System Component Verification" +
				Constants.ls +
				" Performs Windows Hardware Driver Verification" +
				Constants.ls +
				" Allows data on disk to be encrypted" +
				Constants.ls +
				" Allows secured communication on Internet" +
				Constants.ls +
				" ...");
*/
        intend.setText(" Performs Windows System Component Verification" +
				"\n Performs Windows Hardware Driver Verification" +
				"\n Allows data on disk to be encrypted" +
				"\n Allows secured communication on Internet" +
				"\n ...");

		intend.setEditable(false);

        // copied from com ca pki util Constants to decouple from the pki packages
        Font courierBoldFont = new Font("SansSerif", Font.BOLD, 11);

		topLabel.setFont(courierBoldFont);
		intendLabel.setFont(courierBoldFont);
		subjectLabel.setFont(courierBoldFont);
		issuerLabel.setFont(courierBoldFont);
		fromLabel.setFont(courierBoldFont);
		toLabel.setFont(courierBoldFont);

		JPanel generalPanel = new JPanel();
		generalPanel.setBackground(Color.white);

		JScrollPane intendScroll = new JScrollPane(intend);
		intendScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		if (cert != null)
		{
			SimpleDateFormat df = new SimpleDateFormat("EEE, MMM d, yyyy");
			from.setText(df.format(cert.getNotBefore()));
			to.setText(df.format(cert.getNotAfter()));

			subject.setText(CertViewer.getMostSignificantName(cert.getSubjectX500Principal().getName()));
			issuer.setText(CertViewer.getMostSignificantName(cert.getIssuerX500Principal().getName()));
		}

		// layout
		JPanel bottomPanel = new JPanel();
		bottomPanel.setBackground(Color.white);
		bottomPanel.setLayout(new GridBagLayout());
		bottomPanel.add(subjectLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 30, 6, 0), 0, 0));
		bottomPanel.add(subject, new GridBagConstraints(1, 0, 3, 1, 1.0, 1.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(1, 5, 6, 0), 0, 0));
		bottomPanel.add(issuerLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 30, 6, 0), 0, 0));
		bottomPanel.add(issuer, new GridBagConstraints(1, 1, 3, 1, 1.0, 1.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(1, 5, 6, 0), 0, 0));
		bottomPanel.add(fromLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 30, 6, 0), 0, 0));
		bottomPanel.add(from, new GridBagConstraints(1, 2, 1, 1, 0.0, 1.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 5, 6, 0), 0, 0));
		bottomPanel.add(toLabel, new GridBagConstraints(2, 2, 1, 1, 0.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 5, 6, 0), 0, 0));
		bottomPanel.add(to, new GridBagConstraints(3, 2, 1, 1, 1.0, 1.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(1, 5, 6, 0), 0, 0));

		generalPanel.setLayout(new GridBagLayout());
		generalPanel.add(topLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(10, 10, 6, 10), 0, 0));
		generalPanel.add(new JSeparator(), new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 10, 6, 10), 0, 0));
		generalPanel.add(intendLabel, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 10, 6, 10), 0, 0));
		generalPanel.add(intendScroll, new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 40, 6, 10), 0, 0));
		generalPanel.add(new JSeparator(), new GridBagConstraints(0, 4, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(6, 10, 6, 10), 0, 0));
		generalPanel.add(bottomPanel, new GridBagConstraints(0, 5, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 10, 6, 10), 0, 0));

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		//buttonsPanel.add(installCertificateButton);
		//buttonsPanel.add(issuerStatementButton);

		setLayout(new GridBagLayout());
		add(new JScrollPane(generalPanel), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(10, 10, 6, 10), 0, 0));
		add(buttonsPanel, new GridBagConstraints(0, 3, 3, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(0, 10, 6, 5), 0, 0));

		// add listeners
		installCertificateButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				installCertificateButton_actionPerformed(e);
			}
		});

		issuerStatementButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				issuerStatementButton_actionPerformed(e);
			}
		});
	}

	/**
	 * Install certificate action.
	 */
	private void installCertificateButton_actionPerformed(ActionEvent e)
	{
		System.out.println("Install the certificate to this machine");
	}

	/**
	 * Issuer statement action.
	 */
	private void issuerStatementButton_actionPerformed(ActionEvent e)
	{
		System.out.println("View issuer statement");
	}
}
