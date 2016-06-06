package com.ca.commons.cbutil;

import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java's PropertyResourceBundle class is tragically
 * bad.  Why a class intended for i18n use deliberately
 * restricts itself to 8859-1 characters, while using
 * a bizarre unicode escaping format rather than utf8,
 * is beyond me (and certainly beyond any translators I need to send
 * stuff to.)<p>
 * <p/>
 * This class is a reimplementation that automatically
 * selects whether a file is 16bit unicode, utf-8, or
 * local character encoding, and loads it accordingly.
 * Otherwise it is intended to be functionally very
 * similar to PropertiesResourceBundle.<p>
 * <p/>
 * Note that this class does <i>not</i> extend ResourceBundle,
 * as ResourceBundle is difficult to
 * extend  - all the functionality is hidden away
 * in private methods.  So even though ResourceBundle
 * does some neat things, we won't be doing anything but a
 * bare bones rewrite here.
 * <p/>
 * <B>Important:</B> This uses a simplified form of the properties
 * file - keys and phrases are separated by an '=' sign, and while,
 * for backward compatibility, keys <i>can</i> have escaped characters,
 * except for '=', they don't have to.  A further restriction is that
 * the '=' sign <i>may not</i> be immediately preceeded by an escaped
 * escape character (i.e. '\\=' is illegal) - the '=' sign must be
 * preceeded by a space character in this case.  Any space characters
 * at the start and end of a key/description are trimmed.<p>
 * <p/>
 * e.g.   [values in square brackets represent byte values within the file]
 * <p/>
 * potato = kartoffel  // normal ascii (german)
 * help = [30 d8   30 eb   30d7] // 16 bit unicode (japanese)
 * file = [e6 96 87    e6 a1 a3] // utf-8 (chinese)
 */

public class CBResourceBundle
{
    Hashtable translations = new Hashtable();

    private static Logger log = Logger.getLogger(CBResourceBundle.class.getName());

    /**
     * This creates a Resource bundle using only the name of the
     * the resource bundle (e.g. "language.JX").  It then uses
     * the default locale and resource loader to track down the
     * appropriate translation file.
     *
     * @param baseName the name of the translation file to look up.
     *                 this name is extended using the standard locality rules
     *                 to try to find localised files (e.g. "language.JX" becomes
     *                 language/JX_fr_CA in french-speaking canada).
     */

    public CBResourceBundle(String baseName)
    {
        loadBundle(baseName, Locale.getDefault(), ClassLoader.getSystemClassLoader());
    }

    /**
     * This creates a Resource bundle the name of the
     * the resource bundle (e.g. "language.JX").  It then uses
     * the specified locale to track down the
     * appropriate translation file.
     *
     * @param baseName the name of the translation file to look up.
     *                 this name is extended using the standard locality rules
     *                 to try to find localised files (e.g. "language.JX" becomes
     *                 language/JX_fr_CA in french-speaking canada).
     * @param locale   a specific locale to use in place of the default
     *                 system locale.
     */

    public CBResourceBundle(String baseName, Locale locale)
    {
        loadBundle(baseName, locale, ClassLoader.getSystemClassLoader());
    }


    /**
     * This creates a Resource bundle the name of the
     * the resource bundle (e.g. "language.JX").  It then uses
     * the specified locale to track down the
     * appropriate translation file, and teh specified class loader
     * to retrieve the file.
     *
     * @param baseName the name of the translation file to look up.
     *                 this name is extended using the standard locality rules
     *                 to try to find localised files (e.g. "language.JX" becomes
     *                 language/JX_fr_CA in french-speaking canada).
     * @param locale   a specific locale to use in place of the default
     *                 system locale.
     * @param loader   a custom class loader (such as CBClassLoader) used to
     *                 retrieve the translation file.
     */

    public CBResourceBundle(String baseName, Locale locale, ClassLoader loader)
    {
        loadBundle(baseName, locale, loader);
    }

    /**
     * This method searches through all the valid permutations of the base
     * bundle name (modified for locale - e.g. JX_fr_CA.properties, JX_fr.properties,
     * and JX.properties...). <p>
     * If successful, it loads the data (the translation strings) into a local
     * data store (Hashtable).
     *
     * @param baseName the name of the translation file to look up.
     *                 this name is extended using the standard locality rules
     *                 to try to find localised files (e.g. "language.JX" becomes
     *                 language/JX_fr_CA in french-speaking canada).
     * @param locale   a specific locale to use in place of the default
     *                 system locale.
     * @param loader   a custom class loader (such as CBClassLoader) used to
     *                 retrieve the translation file.
     */
    protected void loadBundle(String baseName, Locale locale, ClassLoader loader)
    {
        Vector names = getBundleNames(baseName, locale);
        for (int i = names.size() - 1; i >= 0; i--)
        {
            URL url = loader.getResource(names.get(i).toString());   // XXX why getResource, not findResource???

            if (loadData(url) == true)
                return;                  // once a single file has been loaded, we're done.
        }
        
        // couldn't succesfully load anything...
        log.warning("unable to load resource bundle '" + baseName + "'");
    }

    /**
     * Calculate the bundles along the search path from the base bundle to the
     * bundle specified by baseName and locale.
     *
     * @param baseName the base bundle name
     * @param locale   the locale
     *                 the search path.
     */

    protected static Vector getBundleNames(String baseName, Locale locale)
    {
        final Vector result = new Vector(8);
        final String language = locale.getLanguage();
        final int languageLength = language.length();
        final String country = locale.getCountry();
        final int countryLength = country.length();
        final String variant = locale.getVariant();
        final int variantLength = variant.length();

        if (baseName.toLowerCase().endsWith(".properties"))
        {
            baseName = baseName.substring(baseName.length() - 11);
        }

        baseName = baseName.replace('.', '/');   // note forward slash used, rather than File.separator, for jar access etc.
        final StringBuffer temp = new StringBuffer(baseName);

        result.addElement(temp.toString() + ".properties");
        result.addElement(temp.toString());

        if (languageLength + countryLength + variantLength == 0)
        {
            return result;          //The locale is "", "", "".
        }

        temp.append('_');
        temp.append(language);

        result.addElement(temp.toString() + ".properties");
        result.addElement(temp.toString());

        if (countryLength + variantLength == 0)
        {
            return result;
        }

        temp.append('_');
        temp.append(country);

        result.addElement(temp.toString() + ".properties");
        result.addElement(temp.toString());

        if (variantLength == 0)
        {
            return result;
        }

        temp.append('_');
        temp.append(variant);

        result.addElement(temp.toString() + ".properties");
        result.addElement(temp.toString());

        return result;
    }


    /**
     * This loads the data from a translation file, checking on the way
     * what file format it is in.  (UTF-8, 16bit unicode, or local encoding).
     *
     * @param url the URL to read the data InputStream from.
     * @return whether the load data operation was successfull, or whether
     *         it was interupted (for whatever reason; no file, error reading
     *         file, bad encoding, yadda yadda yadda).
     */

    protected boolean loadData(URL url)
    {
        if (url == null) return false;  // can't read from a null url!

        log.finer("Resource Bundle Reading data from " + ((url == null) ? "null url" : url.toString()));

        try
        {
            /*
             *    First, slurp all the data from the input stream into a byte array.
             */    
            byte[] data = CBUtility.readStream(url.openStream());
            
            /*
             *    Convert the byte array to a String using cunning auto-detecting
             *    encoding methods.
             */
             
//            String text = CBUtility.readI18NByteArray(data);
            
            String text = CBUtility.readUnicode(data);
            /*
             *    Load up the translations hashtable with the parsed data found
             *    in the string...
             */

            return parseData(text);
        }
        catch (Exception e)
        {
            log.log(Level.FINER, "Unable to read data from url: " + ((url == null) ? "(null url)" : url.toString()), e);
            return false;
        }
    }

    /**
     * parses the byte array as per a normal resource file
     * (i.e. looking for key/data pairs seperated by an
     * unescaped '=' sign) after first converting the byte
     * array into a String, using whichever language encoding
     * (unicode16, utf8, locale-specific) seems appropriate.
     */

    protected boolean parseData(String text)
    {
        int startSize = translations.size();

        int start = 0, end = 0;
        while ((end = text.indexOf('\n', start)) != -1)
        {
            String line = text.substring(start, end);


            line = line.trim();

            if (line.length() != 0 && line.charAt(0) != '#') // ignore blank lines and commented lines.
            {
                try
                {
                    int equalPos = 0;

                    do         // skip through all escaped equals characters until we find a non-escaped one.
                    {
                        equalPos = line.indexOf('=', equalPos + 1);
                    }
                    while (line.charAt(equalPos - 1) == '\\');

                    String key = unescape(line.substring(0, equalPos)).trim();
                    String trans = line.substring(equalPos + 1).trim();
                    translations.put(key, trans);

                }
                catch (Exception e)
                {
                    log.log(Level.FINER, "Exception parsing data line '" + line, e);
                } // prob. array ex. - ignore this line.
            }

            start = end + 1;
        }
        
        // check if we added any new translations - if we did, then this was
        // at least partially successfull.
        boolean success = (startSize < translations.size());
        if (success == false)
            log.finer("ParseData unsuccessfull - no new data found");
        return success;

    }

    /**
     * Removes all escapes ('\?' -> '?') from a string.
     * -> Not particularly efficient, but o.k. for short strings.
     */

    protected String unescape(String escapeMe)
    {
        int pos = 0;

        escapeMe = escapeMe.replace("\\n", "\n"); // manually unescape line breaks.

        while ((pos = escapeMe.indexOf('\\', pos)) >= 0)
            escapeMe = escapeMe.substring(0, pos) + escapeMe.substring(pos + 1);

        return escapeMe;
    }

    /**
     * returns the translation keys.
     *
     * @return an Enumeration of all the known keys (usually translatable
     *         strings).
     */

    public Enumeration keys()
    {
        return translations.keys();
    }

    /**
     * returns the translation keys.  Synonym for 'keys()', kept
     * for compatibility with ResourceBundle.
     *
     * @return an Enumeration of all the known keys (usually translatable
     *         strings).
     */

    public Enumeration getKeys()
    {
        return translations.keys();
    }

    /**
     * Returns the object corresponding to a given key.
     *
     * @param key the original text to translate/look up
     * @return the corresponding translation/object
     */

    public Object get(Object key)
    {
        return translations.get(key);
    }

    /**
     * Returns the object corresponding to a given key.  kept
     * for compatibility with ResourceBundle.
     *
     * @param key the original text to translate/look up
     * @return the corresponding translation/object
     */

    public Object getObject(Object key)
    {
        return translations.get(key);
    }


    /**
     * Convenience class returning a particular object
     * as a String.  If the object <i>was</i> a String
     * already it is passed back unchanged, otherwise
     * 'toString()' is called on the object before returning.
     * This class never throws a ClassCastException.
     *
     * @param key the original text to translate/look up
     * @return the corresponding translation/object as a String
     */

    public String getString(String key)
    {
        if (key == null) return "";
        Object o = translations.get(key);
        if (o == null) return "";

        return (o instanceof String) ? (String) o : o.toString();
    }


}