package com.ca.commons.cbutil;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class creates a cache directory called 'temp' where temporary files are stored.
 * Currently it works for audio, jpegPhoto and odDocumentDOC attributes.  The audio
 * are stored in a sub dir called 'audio'.  It also contains methods
 * for decreasing the cache size, cleaning (or emptying the cache and sorting the
 * temporary files according to their last modified date.
 *
 * @author Trudi.
 */

public class CBCache
{
    private static int counter = 0;               //TE: a counter that is used to but a unique number on the temp files.
    private static int CACHEMAX = 100;            //TE: the maximum size the cache should ever reach before decreasing its size.
    private static int CACHEMIN = 50;             //TE: the size the cache gets minimized to when decreaseing its size.
    private static File fileDir = null;            //TE: the directory that the temp files are stored.
    private static File audioFileDir = null;       //TE: the directory that the audio temp files are stored.
    private static String allFiles[];                //TE: an array to store the files within the temp directory.
    private static String allAudioFiles[];           //TE: an array to store the files within the audio temp directory.
    private static String extension = ".jpg";        //TE: the extension of the temporary file.
    private static String dirPath = "temp";

    private static Logger log = Logger.getLogger(CBCache.class.getName());


    /**
     * Creates a cache directory named 'temp' and adds temporary files to it currently naming them (for example)
     * with the hash of their 'DN + a unique number + .jpg'.
     *
     * @param currentDN the dn of the entry being modified (will be used as part of the name of the temp file i.e: cn=Al,o=DEMOCORP,c=AU).
     * @param entry     the actual entry that is being displayed.
     * @param type      the attribute type for example: audio or jpegPhoto.
     * @param size      the size of the number of specific attribute values this entry contains (i.e. 3 jpegPhoto attributes).
     */

    public static void createCache(String currentDN, Attributes entry, String type, int size)
    {
        if (type.equalsIgnoreCase("audio"))
        {
            createAudioCache(currentDN, entry, type, size);
            return;
        }

        fileDir = makeDir();
        allFiles = fileDir.list();

        currentDN = Integer.toString(currentDN.hashCode());

        for (int i = 0; i < allFiles.length; i++)    				//TE: don't create temporary files if they already exist for the entry.
        {
            if (allFiles[i].startsWith(currentDN) && type.equalsIgnoreCase("jpegPhoto") && allFiles[i].endsWith(".jpg"))
                return;
            else if (allFiles[i].startsWith(currentDN) && type.equalsIgnoreCase("odDocumentDOC") && allFiles[i].endsWith(".doc"))
                return;
            else if (allFiles[i].startsWith(currentDN) && type.equalsIgnoreCase("odSpreadSheetXLS") && allFiles[i].endsWith(".xls"))
                return;
            else if (allFiles[i].startsWith(currentDN) && type.equalsIgnoreCase("odMovieAVI") && allFiles[i].endsWith(".avi"))
                return;
            else if (allFiles[i].startsWith(currentDN) && type.equalsIgnoreCase("odSoundWAV") && allFiles[i].endsWith(".wav"))
                return;
            else if (allFiles[i].startsWith(currentDN) && type.equalsIgnoreCase("odMusicMID") && allFiles[i].endsWith(".mid"))
                return;
        }

        for (int i = 0; i < size; i++)
        {
            byte[] bytes = getAttByteValue(type, entry, i);	//TE: gets the byte[] of the jpegPhoto/audio attribute.

            if (type.equalsIgnoreCase("odDocumentDOC"))
            {
                doNormalCache(currentDN, bytes, ".doc");
            }
            else if (type.equalsIgnoreCase("odSpreadSheetXLS"))
            {
                doNormalCache(currentDN, bytes, ".xls");
            }
            else if (type.equalsIgnoreCase("odMovieAVI"))
            {
                doNormalCache(currentDN, bytes, ".avi");
            }
            else if (type.equalsIgnoreCase("odMusicMID"))
            {
                doNormalCache(currentDN, bytes, ".mid");
            }
            else if (type.equalsIgnoreCase("odSoundWAV"))
            {
                doNormalCache(currentDN, bytes, ".wav");
            }
            else if (type.equalsIgnoreCase("jpegPhoto"))
            {
                doNormalCache(currentDN, bytes, ".jpg");
            }
        }
        if (allFiles.length > CACHEMAX)    	//TE: decrease the size of the cache if the cache contains 100 or more temporary files.
            decreaseCacheSize();
    }


    /**
     * Creates an audio cache directory named 'temp/audio' and adds temporary files to it currently naming
     * them (for example) with the hash of their 'DN + a unique number + .wav'.
     *
     * @param currentDN the dn of the entry being modified (will be used as part of the name of the temp file i.e: cn=Al,o=DEMOCORP,c=AU).
     * @param entry     the actual entry that is being displayed.
     * @param type      the attribute type for example: audio or jpegPhoto.
     * @param size      the size of the number of specific attribute values this entry contains (i.e. 3 audio attributes).
     */

    public static void createAudioCache(String currentDN, Attributes entry, String type, int size)
    {
        if (!type.equalsIgnoreCase("audio"))
        {
            log.warning("Error - trying to create a audio temporary cache with incorrect data in 'CBCache.createAudioCache'.");
            return;
        }

        audioFileDir = makeAudioDir();
        allAudioFiles = audioFileDir.list();

        currentDN = Integer.toString(currentDN.hashCode());

        for (int i = 0; i < allAudioFiles.length; i++)    				//TE: don't create temporary files if they already exist for the entry.
        {
            if (allAudioFiles[i].startsWith(currentDN))
                return;
        }

        for (int i = 0; i < size; i++)
        {
            byte[] bytes = getAttByteValue(type, entry, i);	//TE: gets the byte[] of the jpegPhoto/audio attribute.

            if (type.equalsIgnoreCase("audio"))
            {
                doAudioCache(currentDN, bytes);
            }
        }
        if (allAudioFiles.length > CACHEMAX)    	//TE: decrease the size of the cache if the cache contains 100 or more temporary files.
            decreaseAudioCacheSize();
    }


    /**
     * Does the actual writing of the cache files.
     *
     * @param currentDN the dn of the entry being modified (will be used as part of the name of the temp file i.e: cn=Al,o=DEMOCORP,c=AU).
     * @param bytes     the byte[] of the audio attribute.
     * @param extension the extension of the cache file (currently .doc, .jpg).
     */

    public static void doNormalCache(String currentDN, byte[] bytes, String extension)
    {
        String name = currentDN + counter + extension;
        File file = new File(fileDir, name);

        counter++;

        try
        {
            file.deleteOnExit();    //TE: deletes the temporary files when JX is shut down.

            FileOutputStream output = new FileOutputStream(file);
            output.write(bytes);
            output.close();
        }
        catch (Exception e)
        {
            CBUtility.error(CBIntText.get("Problem writing the temporary file: ") + file.toString() + "\n" + e);
        }
    }


    /**
     * Creates the audio temporary files.  First, the type of audio file needs to be determined.
     * This is done by converting the first 100 bytes of the audio file to hex then searching it for an
     * identifier of the audio file (for example in a .wav file the header should contain the word 'WAVE'
     * which in hex is '574156').  If the audio file type is determined the temporary file is created with
     * the current DN + a unique number + the extension for example:
     * <p/>
     * cn=Al,o=DEMOCORP,c=AU9.wav
     * <p/>
     * This method can determine the following audio file types:
     * <p/>
     * .wav
     * .mp3
     * .rmi
     * .ram
     * .aiff
     * .mid
     * .au
     * .stm
     * .voc
     * .xm
     * .s3m
     * .it
     * <p/>
     * if an audio file type is unknown the temporary file is still created but with no extension.
     *
     * @param currentDN the dn of the entry being modified (will be used as part of the name of the temp file i.e: cn=Al,o=DEMOCORP,c=AU).
     * @param bytes     the byte[] of the audio attribute.
     */

    public static void doAudioCache(String currentDN, byte[] bytes)
    {
        byte[] testBytes = new byte[100];
        if (bytes.length > 100)
            System.arraycopy(bytes, 0, testBytes, 0, 100);
        else
            testBytes = bytes;

        String hexSub = CBParse.bytes2Hex(testBytes);

        //TE: look at the header of the sound files and determine the extension.
        if (hexSub.indexOf("574156") > -1)
            extension = ".wav";
        else if (hexSub.indexOf("494433") > -1)
            extension = ".mp3";
        else if ((hexSub.toLowerCase()).indexOf("fffb9044") > -1)
            extension = ".mp3";
        else if ((hexSub.toLowerCase()).indexOf("fffb300c") > -1)
            extension = ".mp3";
        else if ((hexSub.toLowerCase()).indexOf("fff330c0") > -1)
            extension = ".mp3";
        else if (hexSub.indexOf("524946") > -1)
            extension = ".rmi";
        else if ((hexSub.toLowerCase()).indexOf("524d46") > -1)
            extension = ".ram";
        else if (hexSub.indexOf("41494646") > -1)
            extension = ".aiff";
        else if ((hexSub.toLowerCase()).indexOf("4d546864") > -1)
            extension = ".mid";
        else if ((hexSub.toLowerCase()).indexOf("2e736e64") > -1)
            extension = ".au";
        else if ((hexSub.toLowerCase()).indexOf("636f6f6c") > -1)
            extension = ".stm";		//TE: screamTracker format.
        else if ((hexSub.toLowerCase()).indexOf("437265617469766520566f6963652046696c65") > -1)
            extension = ".voc";		//TE: Creative Voice File.
        else if ((hexSub.toLowerCase()).indexOf("457874656e646564204d6f64756c65") > -1)
            extension = ".xm"; 		//TE: Extended Module format.
        else if ((hexSub.toLowerCase()).indexOf("5343524d") > -1)
            extension = ".s3m";
        else if ((hexSub.toLowerCase()).indexOf("494d50") > -1)
            extension = ".it";
        else
            extension = ".xxx";

        File file = new File(audioFileDir, currentDN + counter + extension);
        counter++;

        file.deleteOnExit();    //TE: deletes the temporary files when JX is shut down.
        try
        {
            FileOutputStream output = new FileOutputStream(file);
            output.write(bytes);
            output.close();
        }
        catch (IOException e)
        {
            CBUtility.error(CBIntText.get("Problem writing the audio temporary file: ") + file.toString() + "\n" + e);
        }
    }


    /**
     * This Comparator compares two Files by their lastModified() date.
     */

    public static class FileComparator implements Comparator
    {
        /**
         * This Comparator compares two Files by their lastModified() date.
         *
         * @param o1 one of the two items to be compared.
         * @param o2 the other of the items to be compared.
         */

        public int compare(Object o1, Object o2)
        {
            long a = ((File) o1).lastModified();
            long b = ((File) o2).lastModified();

            return (a == b) ? 0 : ((a < b) ? -1 : 1);
        }
    }


    /**
     * Sorts an array of files in order of lastModified (oldest to newest).
     *
     * @param files the array of files to be sorted.
     * @return the sorted file array.
     */

    public static File[] sortFiles(File[] files)
    {
        Arrays.sort(files, new FileComparator());
        return files;
    }


    /**
     * Returns the directory that is being used for the caching.
     *
     * @return the directory.
     */

    public static File getCacheDirectory()
    {
        return fileDir;
    }


    /**
     * Returns the directory that is being used for the audio caching.
     *
     * @return the directory.
     */

    public static File getAudioCacheDirectory()
    {
        return audioFileDir;
    }


    /**
     * Decreases the size of the cache (usually if the cache contains 100 or more temporary files),
     * by deleting the oldest files until there is only 50 temporary files left in the cache.
     * It also checks that it deletes all the files pertaining to one entry i.e. if an entry has
     * four temporary files, this method will try to delete all four even if the minimum level of 50 is
     * reached (this ensures that the templates refresh correctly).
     */

    public static void decreaseCacheSize()
    {
        File[] fileArray = fileDir.listFiles();

        fileArray = sortFiles(fileArray);    					//TE: sort the files according to there last modified date (oldest to newest).

        for (int i = 0; i < (fileArray.length - CACHEMIN); i++)    	//TE: do the following until there is only 50 temp files left.
        {
            String fileName = fileArray[i].getName();

            for (int x = 0; x < allFiles.length; x++)    			//TE: make sure that when deleting a temp file, that all the temp files pertaining to the same entry are deleted also.
            {
                if (allFiles[x].startsWith(fileName.substring(0, ((fileArray[i].toString()).length()) - 15)))
                {
                    //TE: if the temp file in the directory at position 'x' starts with the the same name as the temp file listed at
                    //    position i of the sorted array (sorted by last modified), minus the last 15 characters (to ensure that the
                    //    'digit+.extension' is cut off the temp name), then delete it!...phew

                    File tempFile = new File(makeDir(), allFiles[x].toString());
                    tempFile.delete();
                }
            }
        }
        decreaseAudioCacheSize();
    }


    /**
     * Decreases the size of the audio cache (usually if the cache contains 100 or more temporary files),
     * by deleting the oldest files until there is only 50 temporary files left in the cache.
     * It also checks that it deletes all the files pertaining to one entry i.e. if an entry has
     * four temporary files, this method will try to delete all four even if the minimum level of 50 is
     * reached (this ensures that the templates refresh correctly).
     */

    public static void decreaseAudioCacheSize()
    {
        File[] fileArray = audioFileDir.listFiles();

        fileArray = sortFiles(fileArray);    					//TE: sort the files according to there last modified date (oldest to newest).

        for (int i = 0; i < (fileArray.length - CACHEMIN); i++)    	//TE: do the following until there is only 50 temp files left.
        {
            String fileName = fileArray[i].getName();

            for (int x = 0; x < allAudioFiles.length; x++)    		//TE: make sure that when deleting a temp file, that all the temp files pertaining to the same entry are deleted also.
            {
                if (allAudioFiles[x].startsWith(fileName.substring(0, ((fileArray[i].toString()).length()) - 15)))
                {
                    //TE: if the temp file in the directory at position 'x' starts with the the same name as the temp file listed at
                    //    position i of the sorted array (sorted by last modified), minus the last 15 characters (to ensure that the
                    //    'digit+.extension' is cut off the temp name), then delete it!...phew

                    File tempFile = new File(makeAudioDir(), allAudioFiles[x].toString());
                    tempFile.delete();
                }
            }
        }
    }


    /**
     * Creates the temporary directory, calling it 'temp'.
     *
     * @return the directory.
     */

    private static File makeDir()
    {
        File dir = new File(dirPath);
        dir.mkdir();
        dir.deleteOnExit();

        return dir;
    }


    /**
     * Creates the temporary audio directory, calling it 'temp/audio'.
     *
     * @return the directory.
     */

    private static File makeAudioDir()
    {
        File dir = new File(dirPath + File.separator + "audio");
        dir.mkdir();
        dir.deleteOnExit();

        return dir;
    }


    /**
     * Returns the absolute path of the temp directory.
     *
     * @return the path of the temp directroy.
     */

    public static String getDirPath()
    {
        return makeDir().getAbsolutePath();
    }


    /**
     * Sets the path of the temp directory.
     *
     * @param path the path of where to make the temp directory
     */

    public static String setDirPath(String path)
    {
        return dirPath = path;
    }


    /**
     * Returns the absolute path of the audio temp directory.
     *
     * @return the path of the temp directroy.
     */

    public static String getAudioDirPath()
    {
        return makeAudioDir().getAbsolutePath();
    }


    /**
     * Gets the byte[] value of a specified attribute value.
     *
     * @param name    the name of the attribute value e.g. 'jpegPhoto'.
     * @param entry   the entry that is to be displayed.
     * @param entries the number of attributes values (e.g. 'jpegPhoto') pertaining to the entry.
     * @return the byte array of the attribute value.
     */

    private static byte[] getAttByteValue(String name, Attributes entry, int entries)
    {
        try
        {
            Attribute a = entry.get(name);
            if (a == null) return null;    // no pre-existing value, so nothing to do.
            if (a.size() == 0 || a.get() == null) return null;    // no pre-existing value, so nothing to do.

            Object o = a.get(entries);    	//TE: gets the attribute value at a certain position i.e. if 3 attribute values it will get the one at the position stored in entires.

            if (o instanceof byte[])
                return (byte[]) o;

            return null;
        }
        catch (NamingException e)
        {
            log.log(Level.WARNING, "Form Value Error getting value for " + name + " value :\n ", e);    // not a terminal error.
            return null;
        }
    }


    /**
     * Cleans the cache of entries that start with the dn of the entry being modified.
     *
     * @param currentDN the dn of the entry being modified.
     */

    public static void cleanCache(String currentDN)
    {
        currentDN = Integer.toString(currentDN.hashCode());

        allFiles = makeDir().list();

        for (int i = 0; i < allFiles.length; i++)
        {
            if (allFiles[i].startsWith(currentDN.toString())) //&& allFiles[i].endsWith(extension))    //TE: check that the temp files are of the entry being modified.
            {
                File tempFile = new File(fileDir, allFiles[i].toString());
                tempFile.delete();
            }
        }

        allAudioFiles = makeAudioDir().list();

        for (int i = 0; i < allAudioFiles.length; i++)
        {
            if (allAudioFiles[i].startsWith(currentDN.toString())) //&& allFiles[i].endsWith(extension))    //TE: check that the temp files are of the entry being modified.
            {
                File tempFile = new File(audioFileDir, allAudioFiles[i].toString());
                tempFile.delete();
            }
        }
    }


    /**
     * Cleans the cache of all temporary entries by deleting them.
     */

    public static void cleanCache()
    {
        allFiles = makeDir().list();

        if (allFiles == null)
            return;

        for (int i = 0; i < allFiles.length; i++)
        {
            File tempFile = new File(makeDir(), allFiles[i].toString());
            tempFile.delete();
        }

        allAudioFiles = makeAudioDir().list();

        for (int i = 0; i < allAudioFiles.length; i++)
        {
            File tempFile = new File(makeAudioDir(), allAudioFiles[i].toString());
            tempFile.delete();
        }
    }


    /**
     * Sets the maximum cache size @CACHEMAX@
     *
     * @param size the maximum cache size.
     */

    public static void setMaxCacheSize(int size)
    {
        CACHEMAX = size;
    }


    /**
     * Sets the minimum cache size @CACHEMAX@
     *
     * @param size the minimum cache size.
     */

    public static void setMinCacheSize(int size)
    {
        CACHEMIN = size;
    }

}