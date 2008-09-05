package com.ca.commons.cbutil;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.*;

/**
 * CBJarResources: CBJarResources maps all resources included in a
 * Zip or Jar file. Additionaly, it provides methods to extract
 * them as byte arrays, images, or input streams.<p>
 * This class makes no distinction between jar and zip files, and
 * makes no use of any jar manifest file, should it exist.
 */

public final class CBJarResource
{
    /**
     * The contents of the zip file are cached the first time they are used, to
     * speed any subsequent access of the same resource.
     */
    protected Hashtable contents = new Hashtable();

    /**
     * When the CBJarResource object is created, the referenced zip file's catalogue
     * is read.  Later, individual zipped files are read using this cache of catalogue entries.
     */
    protected Hashtable entries = new Hashtable();

    /**
     * The name of this object's jar/zip file
     */
    protected String zipFileName;

    /**
     * The actual zip or jar file
     */
    protected ZipFile zipFile;


    private static Logger log = Logger.getLogger(CBJarResource.class.getName());

    /*/ temp - debug
    {
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.FINEST);
        log.addHandler(handler);
        log.setLevel(Level.FINE);
        log.fine("CBJarResource log active");
    }
    //*/

    /**
     * creates a CBJarResources. It extracts all resources from a Jar
     * into an internal hashtable, keyed by resource names.
     * @param zipFileName a jar or zip file
     */

    /**
     * Returns the raw name of the zip/jar file.
     *
     * @return the name of the zip file, e.g. 'help/local/fnord.zip'
     */

    public String getZipFileName()
    {
        return zipFileName;
    }

    /**
     * Returns the last modified date of the zip file.
     */

    public long getLastModified()
    {
        File zip = new File(zipFileName);
        if (zip == null)
            return 0;
        else
            return zip.lastModified();
    }

    /**
     * creates a CBJarResources. It extracts all resources from a Jar
     * into an internal hashtable, keyed by resource names.
     *
     * @param zipFileName a jar or zip file
     */

    public CBJarResource(String zipFileName)
    {
        this.zipFileName = zipFileName;
        init();
        log.fine("INITIALIZED JAR " + zipFileName);
    }

    /**
     * Prints a simple identifying string for debugging purposes.
     */

    public String toString()
    {
        return "CBJarResource (" + zipFileName + ")";
    }

    /**
     * returns whether a particular resource
     * exists in the archive.  These names are preloaded into a hashtable, so this should be a lightweight operation.
     *
     * @param name the name of the resource (e.g. a class com/ca/od/util/CBJarResources, or an image file myimages/winona.gif)
     */

    public boolean hasResource(String name)
    {
        return entries.containsKey(name);
    }

    /**
     * Extracts a jar resource as a blob.  Note that this basic method throws no exceptions, but simply returns null on failure.
     *
     * @param name a resource name.
     * @return a resource, or null if no resource available for any reason.
     */

    public byte[] getResource(String name) throws ZipException
    {
        return getRawResource(name);
    }

    /**
     * Extracts a file from a jar or zip, returning it as a raw byte array.
     */

    protected byte[] getRawResource(String name) throws ZipException
    {
        try
        {
            ZipEntry entry = (ZipEntry) entries.get(name);
            InputStream in = zipFile.getInputStream(entry);

            if (entry == null)
            {
                throw new ZipException("Unable to find: " + name + " in zip file " + zipFileName);
            }

            return readZipEntryData(in, entry);
        }
        catch (IOException e)
        {
            throw new ZipException("Error reading zip file: " + e);
        }
    }


    /**
     * Returns an Image from a zip file.
     *
     * @param imageName    the name of the image (including path) as saved in the zip file
     * @param imageCreator a java.awt.Component used to get a Toolkit to construct the Image.
     * @return a usable java.awt.Image file.
     */

    public Image getImage(String imageName, Toolkit imageCreator)
            throws ZipException
    {
        if (hasResource(imageName) == false)
            throw new ZipException("resource: " + imageName + " not found in zip file: " + zipFileName);


        Image ret = imageCreator.createImage(getRawResource(imageName));
        return ret;
    }

    /**
     * Allows a zipped file to be accessed with a (uncompressed) input stream, not unlike
     * a normal FileInputStream.
     *
     * @param resourceName the fully qualified name of the object (i.e. with the path of the file as it exists in the zip file)
     */
    public InputStream getInputStream(String resourceName)
            throws ZipException
    {
        try
        {
            ZipEntry entry = (ZipEntry) entries.get(resourceName);
            InputStream in = zipFile.getInputStream(entry);
            return in;
        }
        catch (IOException e)
        {
            throw new ZipException("Error getting input stream: " + e.toString());
        }
    }

    /**
     * initializes internal hash tables with Jar file resources.
     */
    protected void init()
    {
        try
        {
            // extracts just sizes only.
            zipFile = new ZipFile(zipFileName);
            Enumeration e = zipFile.entries();
            while (e.hasMoreElements())
            {
                ZipEntry ze = (ZipEntry) e.nextElement();
                //if (debug)
                String name = ze.getName();
                log.fine("added zip entry: " + name);
                entries.put(name, ze);

                // add editor classes a second time, in canonical lower case form.  (This solves problems with loading classes by attribute / object class names
                // with directory dependant capitalization)

                if (name.endsWith("Editor") || name.endsWith("editor"))
                {
                    log.fine("double loading " + name.toLowerCase());
                    entries.put(name.toLowerCase(), ze);
                }
            }
        }
        catch (NullPointerException e)
        {
            log.warning("unable to init zip file " + zipFileName + " - no entries?");
        }
        catch (FileNotFoundException e)
        {
            log.log(Level.WARNING, "can't find zip file", e);
        }
        catch (IOException e)
        {
            log.log(Level.WARNING, "error reading zip file", e);
        }
    }

    /**
     * Reads a specific zip entry from the input stream...
     *
     * @param is the decrypted (important!) input stream to construct an object out of.  May be a ZipInputStream, but remember that
     *           ZipInputStream is *not* idempotent (i.e. don't call it on itself!).  For the record, ZipFile.getInputStream() is a decrypted stream.
     * @param ze the single zip entry to extract from the file.
     */

    protected byte[] readZipEntryData(InputStream is, ZipEntry ze)
            throws IOException
    {
        int size = (int) ze.getSize();

        if (size == -1)
        {
            log.warning("bizarre size error in zip entry reading = corrupt zip file?");
            return null;
        }

        byte[] b = new byte[(int) size];
        int rb = 0;
        int chunk = 0;
        while (((int) size - rb) > 0)
        {
            chunk = is.read(b, rb, (int) size - rb);
            if (chunk == -1)
            {
                break;
            }
            rb += chunk;
        }

        /* DEBUG
            System.out.println("successfully read: " + ze.getName()+"  rb="+rb+",size="+size+",csize="+ze.getCompressedSize());
            System.out.println("successfully read value:\n" + new String(b));
            for (int i=0; i<10; i++)
                System.out.print(CBUtility.byte2Hex(b[i]) + " ");
            System.out.println("\n");
        /* */

        return b;
    }

    /**
     * Dumps a zip entry into a string.
     *
     * @param ze a ZipEntry
     */
    protected String dumpZipEntry(ZipEntry ze)
    {
        StringBuffer sb = new StringBuffer();
        if (ze.isDirectory())
        {
            sb.append("dir:  ");
        }
        else
        {
            sb.append("file: ");
        }
        if (ze.getMethod() == ZipEntry.STORED)
        {
            sb.append("stored   ");
        }
        else
        {
            sb.append("deflated ");
        }
        sb.append(ze.getName());
        sb.append("\t\t");
        sb.append("" + ze.getSize());
        if (ze.getMethod() == ZipEntry.DEFLATED)
        {
            sb.append("/" + ze.getCompressedSize());
        }
        return (sb.toString());
    }

    /**
     * Is a test driver. Given a jar file and a resource name, it trys to
     * extract the resource and then tells us whether it could or not.
     * <p/>
     * <strong>Example</strong>
     * Let's say you have a JAR file which jarred up a bunch of gif image
     * files. Now, by using CBJarResources, you could extract, create, and display
     * those images on-the-fly.
     * <pre>
     *     ...
     *     CBJarResources JR=new CBJarResources("GifBundle.jar");
     *     Image image=Toolkit.createImage(JR.getResource("logo.gif");
     *     Image logo=Toolkit.getDefaultToolkit().createImage(
     *                   JR.getResources("logo.gif")
     *                   );
     *     ...
     * </pre>
     */

    public static void main(String[] args) throws IOException
    {
        if (args.length != 2)
        {
            System.out.println("usage: java CBJarResources <jar file name> <resource name>");
            System.exit(1);
        }
        CBJarResource jr = new CBJarResource(args[0]);
        byte[] buff = jr.getResource(args[1]);
        if (buff == null)
        {
            System.out.println("Could not find " + args[1] + ".");
        }
        else
        {
            System.out.println("Found " + args[1] + " (length=" + buff.length + ").");
        }
    }

    protected void finalize()
    {
        try
        {
            zipFile.close();
        }
        catch (Exception e)
        {
        }
    }

    /**
     * Returns the *names* only of any resources with a particular prefix (such as 'images/').
     * This is similar the wildcard search 'prefix*', but no actual '*' character
     * is used.  The search is case insensitive.
     */

    public String[] getPrefixedResources(String prefix)
    {
        prefix = prefix.toLowerCase();
        Vector results = new Vector();
        Enumeration keys = entries.keys();

        // cycle through all available entry names (keys) looking for prefixed entries
        while (keys.hasMoreElements())
        {
            String name = (String) keys.nextElement();
            if (name.toLowerCase().startsWith(prefix))
            {
                results.add(name);
            }
        }


        // laboriously cast to strings for returning.
        if (results.size() == 0)
        {
            return new String[]{};
        }
        else
        {
            // ???? return (String[]) (results.toArray);

            return (String[]) results.toArray(new String[results.size()]);
            
/*            
    		Object[] temp = results.toArray();
    		String[] ret = new String[temp.length];
    		for (int i=0; i<temp.length; i++)
    			ret[i] = (String) temp[i];
			return ret; 			
*/
        }
    }

}	// End of CBJarResources class.