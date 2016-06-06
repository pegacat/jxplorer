package com.ca.commons.cbutil;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.zip.ZipException;

/**
 * Title:        test
 * Description:  See if we can get this crappy IDE to work properly just once.
 * Copyright:    Copyright (c) 2001
 * Company:
 *
 * @author Chris Betts
 * @version 1.0
 */

public class CBClassLoader extends ClassLoader
{

    /**
     * a cached list of classes to speed the return of twice loaded classes.
     */

    protected Hashtable classes = new Hashtable();
    protected Hashtable lowerCaseClasses = new Hashtable();

    /**
     * The resource loader provides the interface to a group of zip files.
     */

    protected CBResourceLoader resourceLoader;


    private static Logger log = Logger.getLogger(CBClassLoader.class.getName());

    /**
     * Constructor - note that that the class is useless until at least one resource file has been
     * registered with it using the addResource() method.
     */

    public CBClassLoader(CBResourceLoader loader)
    {
        log.fine("Started CBClassLoader");

        resourceLoader = loader;
    }

    /**
     * Translates the '.' seperators of Class package names into the \ seperators needed for
     * the internal directory structure of the zip file.
     */

    protected String translateClassName(String name)
    {
        if (name.endsWith(".class"))
            name = name.replace('.', '/');
        else
            name = name.replace('.', '/') + ".class";

        log.finer("looking for class: " + name);

        return name;
    }

    /**
     * This sample function for reading class implementations reads
     * them from the local file system
     */

    private byte[] getClassFromResourceFiles(String className)
            throws ZipException
    {
        className = translateClassName(className);
        return resourceLoader.getResource(className);
    }

    /**
     * This is a simple version for external clients since they
     * will always want the class resolved before it is returned
     * to them.
     */
    public Class findClass(String className) throws ClassNotFoundException
    {
        return (findClass(className, true));
    }

    void addClass(Class c)
    {
        log.finer("adding class " + c.toString());
        // super.addClass(c);
    }

    /**
     * This is the required version of findClass which is called
     * both from findClass above and from the internal function
     * loadClass of the parent.
     */

    public synchronized Class findClass(String className, boolean resolveIt)
            throws ClassNotFoundException
    {
        Class result;
        byte classData[];

        log.finer("        >>>>>> Load class : " + className);

        /* Check our local cache of classes */
        Object local = classes.get(className);
        if (local != null)
        {
            if (local instanceof String && "".equals((String) local))
            {
                log.finer("        >>>>>> ignoring '" + className + "' (failed to load previously).");
                throw new ClassNotFoundException("ignoring class '" + className + "' (failed to load previously).");
            }
            log.finer("        >>>>>> returning cached result.");
            return (Class) local;
        }

        /* Check with the primordial class loader */
        try
        {
            result = super.findSystemClass(className);
            log.finer("        >>>>>> returning system class (in CLASSPATH).");
            return result;
        }
        catch (ClassNotFoundException e)
        {
            log.finer("        >>>>>> Not a system class - looking in zip files.");
        }

        /* Try to load it from our repository */
        try
        {
            classData = getClassFromResourceFiles(className);
        }
        catch (ZipException e)
        {
            classes.put(className, "");   // stick a dummy entry in as a warning to others...
            lowerCaseClasses.put(className.toLowerCase(), "");
            throw new ClassNotFoundException("Error getting className: '" + className + "' : " + e);
        }

        if (classData == null)
        {
            classes.put(className, "");   // stick a dummy entry in as a warning to others...
            lowerCaseClasses.put(className.toLowerCase(), "");
            throw new ClassNotFoundException();
        }

        /* Define it (parse the class file) */
        result = defineClass(className, classData, 0, classData.length);
        if (result == null)
        {
            classes.put(className, "");   // stick a dummy entry in as a warning to others...
            lowerCaseClasses.put(className.toLowerCase(), "");
            throw new ClassFormatError();
        }

        if (resolveIt)
        {
            resolveClass(result);
        }

        classes.put(className, result);
        lowerCaseClasses.put(className.toLowerCase(), result);
        log.finer("        >>>>>> Returning newly loaded zipped class. " + className);
        return result;
    }

    public URL getResource(String name)
    {
        URL bloop = super.getResource(name);
        return bloop;
    }

    /**
     * Returns a 'jar url' to the specified resource.
     *
     * @param name the name of the resource to look for (e.g. 'HelpSet.hs')
     * @return the url of the resource, (e.g. 'jar:file:myjarfile.jar!/HelpSet.hs'.
     *         - this will be null if the resource cannot be found in the known
     *         jar file.
     */

    protected URL findResource(String name)
    {
        CBJarResource container = resourceLoader.getJarContainingResource(name);
        if (container == null)
        {
            return findFileResource(name); // try for an unpacked file.
        }

        String zipFile = container.getZipFileName();
        String url = "jar:file:" + zipFile + "!/" + name;

        try
        {
            return new URL(url);
        }
        catch (MalformedURLException e)
        {
            log.warning("Unable to construct url: " + url + "\n -> due to " + e);
            return null;
        }
    }

    /**
     * ... currently I'm struggling to understand why JX sometimes finds the language/JX_xx_XX.properties files and
     * sometimes doesn't.  This is an attempt to double check for resources *not* in a jar file, but in a JX sub-directory.
     * @param name
     * @return
     */
    protected URL findFileResource(String name)
    {
        try
        {
            File candidate = new File(name);
            if (candidate.exists())
            {
                return candidate.toURI().toURL();
            }
        }
        catch (Exception e) {}
        return null;
    }

    public String toString()
    {
        return "CBClassLoader";
    }

    public InputStream getResourceAsStream(String name)
    {
        return super.getResourceAsStream(name);
    }

}