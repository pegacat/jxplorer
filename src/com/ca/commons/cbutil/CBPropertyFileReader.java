package com.ca.commons.cbutil;

import java.io.*;
import java.util.Properties;

/**
 * Reads a property file from a given location.
 *
 * @author erstr01
 */
public class CBPropertyFileReader
{
    /**
     * The path and file name of the file to read.
     */
    private String propFileName = null;

    /**
     * The object that the file is loaded into.
     */
    private Properties properties = null;

    /**
     * Reads a property file.
     *
     * @param propFileName the path and name of the file to read.
     */
    public CBPropertyFileReader(String propFileName)
            throws IOException
    {
        this.propFileName = propFileName;
        File propFile = new File(propFileName);
        loadPropertyFile(propFile);
    }

    /**
     * Reads a property file.
     *
     * @param propFile the property file.
     */
    public CBPropertyFileReader(File propFile)
            throws IOException
    {
        if (propFile == null || !propFile.exists())
            throw new IOException("File does not exist.");

        propFileName = propFile.getAbsolutePath();
        loadPropertyFile(propFile);
    }

    /**
     * Sets up the reader.
     *
     * @param propFile the property file.
     */
    private void loadPropertyFile(File propFile)
            throws IOException
    {
        // Sanity Check that file exists...
        if (propFile.exists() == false)
            throw new IOException("Cannot find the properties file: " + propFileName);

        properties = new Properties();
        properties.load(new FileInputStream(propFile));
    }

    /**
     * Wraps Property.getProperty().
     *
     * @param key the key in the property file.
     * @return the value pertaining to the supplied key.
     */
    public String getValue(String key)
    {
        return properties.getProperty(key);
    }

    /**
     * @return the property file name.
     */
    public String getPropFileName()
    {
        return propFileName;
    }

    /**
     * Sets the property file name.
     *
     * @param propFileName the property file name including it's path.
     */
    public void setPropFileName(String propFileName)
    {
        this.propFileName = propFileName;
    }

    /**
     * @return the object that the property file has been read in to.
     */
    public Properties getProperties()
    {
        return properties;
    }

    /**
     * Set the property object.
     *
     * @param properties the object that the property file has been read into.
     */
    public void setProperties(Properties properties)
    {
        this.properties = properties;
    }
}
