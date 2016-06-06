package com.ca.commons.cbutil;

import java.io.File;
import java.io.FileFilter;

/**
 * Quick Hack to cover up yet another
 * Swing inadequacy.
 *
 * (superseded by javax.swing.filechooser.FileNameExtensionFilter as of java 1.6?)
 */

public class CBFileFilter extends javax.swing.filechooser.FileFilter
        implements FileFilter
{
    protected String[] extensions; // make it possible to extend this class
    String description;

    public CBFileFilter(String[] exts)
    {
        this(exts, "no description given");
    }

    public CBFileFilter(String[] exts, String desc)
    {
        extensions = new String[exts.length];
        for (int i = 0; i < exts.length; i++)
        {
            extensions[i] = exts[i].toLowerCase();
        }

        description = desc;
    }

    public boolean accept(File f)
    {
        if (f.isDirectory()) return true;

        for (int i = 0; i < extensions.length; i++)
            if (f.getName().toLowerCase().endsWith(extensions[i]))
                return true;

        return false;
    }

    public String getDescription()
    {
        return description;
    }

}