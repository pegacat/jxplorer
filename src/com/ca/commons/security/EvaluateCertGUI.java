
package com.ca.commons.security;

import com.ca.commons.cbutil.*;
import com.ca.commons.security.cert.CertViewer;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.security.cert.X509Certificate;
import javax.swing.*;

/**
 * <p>This class provides a modal dialog that allows a user to examine an arbitrary certificate,
 * and then decide whether to accept or reject that certificate, or to add it to their keystore.</p>
 */
public class EvaluateCertGUI
{

    public static final int REJECT = 0;
    public static final int ACCEPT_ONCE = 1;
    public static final int ACCEPT_ALWAYS = 2;

    CBButton View, Reject, Accept_Once, Accept_Always;
    CBPanel display;
    CertViewer viewer;
    Frame owner;

    public EvaluateCertGUI(Frame rootFrame)
    {
        owner = rootFrame;
    }
    /**
     * <p>Creates a modal dialog that prompts the user to accept or reject a
     * certificate during an SSL connection.</p>
     *
     * <p>The function returns either EvaluateCertGUI.REJECT, EvaluateCertGUI.ACCEPT_ONCE or EvaluateCertGUI.ACCEPT_ALWAYS.</p>
     *
     * @param cert the (hitherto unknown) certificate to evaluate for use in ssl.
     * @return One of EvaluateCertGUI.REJECT, EvaluateCertGUI.ACCEPT_ONCE or EvaluateCertGUI.ACCEPT_ALWAYS.  If the user
     * simply closes the window, the response is taken to be REJECT.
     */
    public int isTrusted(X509Certificate cert)
    {
        final X509Certificate certificate = cert;  // make local, final copy.

        display = new CBPanel();

        display.addWide(new JLabel("The ldap server you are connecting to is using"), 3);
        display.newLine();

        display.addWide(new JLabel("an unknown security certificate."), 3);
        display.newLine();
        display.newLine();

        display.add(new JLabel("Subject: "));
        display.addWide(new JLabel(certificate.getSubjectDN().getName()), 2);
        display.newLine();

        display.add(new JLabel("Valid from: "));
        display.addWide(new JLabel(certificate.getNotBefore().toString()), 2);
        display.newLine();

        display.add(new JLabel("Valid to: "));
        display.addWide(new JLabel(certificate.getNotAfter().toString()), 2);
        display.newLine();
        display.add(new JLabel(""));
        display.newLine();

        display.addWide(new JLabel("Would you like to continue anyway?"), 3);
        display.newLine();

        display.add(View = new CBButton("View Certificate", "Examine the Certificate Details"));
        display.newLine();

        View.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                viewer = new CertViewer(owner, certificate, CertViewer.VIEW_ONLY);
                viewer.setVisible(true);
            }
        });

        /**
         *  Distilled evil to get JOptionPane to show custom buttons with tooltips.
         */

        Reject = new CBButton("End Connection", "Reject the certificate");
        Accept_Once = new CBButton("This Session Only", "Allow, but do not add to your trusted keystore.");
        Accept_Always = new CBButton("Always", "Add the server certificate to your trusted keystore");

        CBButton optionButtons[] = new CBButton[3];
        optionButtons[REJECT] = Reject;
        optionButtons[ACCEPT_ONCE] = Accept_Once;
        optionButtons[ACCEPT_ALWAYS] = Accept_Always;


        // ugly.  But effective.  This is added to the buttons below so that they trigger the
        // JOptionPane to return.
        ActionListener buttonListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent a)
            {
                Component sourceButton = (Component)a.getSource();
                ((JOptionPane)(sourceButton).getParent().getParent()).setValue(sourceButton);
            }
        };

        Reject.addActionListener(buttonListener);
        Accept_Once.addActionListener(buttonListener);
        Accept_Always.addActionListener(buttonListener);

        // the deep magic of JOptionPane continues to elude me (how *does* it create internal JButtons?) - however the
        // upshot is that it returns -1 (window closed) or the index of the button selected (see above)
        int v = JOptionPane.showOptionDialog(null, display, "Server CA Certificate missing", JOptionPane.DEFAULT_OPTION, JOptionPane.YES_NO_CANCEL_OPTION, null, optionButtons, optionButtons[0]);

        if (v == -1)
            v = REJECT;

        return v;
    }
}