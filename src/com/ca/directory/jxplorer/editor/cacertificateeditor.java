/**
 *    All pluggable editors must be in this package
 */

package com.ca.directory.jxplorer.editor;

import com.ca.commons.security.cert.*;
import com.ca.commons.security.cert.CertViewer;
import com.ca.commons.cbutil.*;

import java.awt.Frame;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 *	Cerrtificate Editor.
 *	Allows user to enter a masked binary password.
 */

public class cacertificateeditor implements abstractbinaryeditor
{
    Frame owner;

    public cacertificateeditor(Frame owner)
    {
        this.owner = owner;
    }

    /**
     *    Uses the static editCertificate method in CertViewer to
     *    display (and potentially edit) the certificate.
     */

    public void setValue(editablebinary editMe)
    {
        byte[] data = editMe.getValue();

        CertViewer.CertAndFileName returnVal = null;

        try
        {
             returnVal = CertViewer.editCertificate(owner, data);
        }
        catch (Exception e)
        {
             CBUtility.error(CBIntText.get("Error reading certificate."), e);
        }

        X509Certificate cert = null;

        if(returnVal != null)       //TE: throws an exception if this is null....bug 3176.
            cert = returnVal.cert;

        if (cert != null)
        {
            try
            {
                byte[] newData = cert.getEncoded();
                if (Arrays.equals(newData, data) == false)
                    editMe.setValue(newData);
            }
            catch(Exception e)
            {
                CBUtility.error(CBIntText.get("Error: unable to modify certificate."), e);
            }
        }
    }
}