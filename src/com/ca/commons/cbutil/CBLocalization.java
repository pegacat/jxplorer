package com.ca.commons.cbutil;

import java.io.File;
import java.io.FileInputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Reads a language translation file into a hashtable.
 * Figures out the name of the translation file by
 * the most specific description of the Locale.  For example, <br>
 * [language]_[country]_[variant].properties would be loaded before:<br>
 * [language]_[country].properties would be loaded before:<br>
 * [language].properties<br><br>
 * Use the 'get' method to return the tranlated text.
 *
 * @author erstr01
 */
public class CBLocalization
{
    private final static Logger log = Logger.getLogger(CBLocalization.class.getName());

    Locale locale = null;

    MessageFormat messageFormatter = null;

    Hashtable translations = null;

    private boolean errorGiven = false;  // This is a 'complain once' class - usually it's either working or it's not...

    /*
     * If the local language is english, don't print warning messages
     * about missing translation files (it confuses soooo many people)
     */
    private boolean english = true;

    /**
     * This initialises the international text class.
     *
     * @param locale the locale to use.
     * @param path   the path where the language files are stored.
     */
    public CBLocalization(Locale locale, String path)
    {
        this.locale = locale;

        translations = new Hashtable(500);

        loadLanguageFile(path, locale);

        if (!locale.getLanguage().equals("en"))
            english = false;

        messageFormatter = new MessageFormat("");
        messageFormatter.setLocale(locale);
    }

    /**
     * This attempts to get the localised version of a string.
     * If anything goes wrong, it attempts to return the key
     * string it was given - hence using a meaningfull key is
     * a good harm minimization strategy.
     */
    public String get(String key)
    {
        if (key == null)       // sanity check that a valid key has been passed.
        {
            return "null key";
        }

        if (translations == null || translations.size() == 0)   // we never opened a file...
        {
            if (errorGiven == false)  // only print error message once!  (otherwise we'd print an
            {                         // error for every string in the program!)
                if (!english) log.warning("Unable to translate (" + key + ") - can't find language file.");
                errorGiven = true;
            }
            return key;        // try to keep on trucking using the (english) key phrase
        }

        try
        {
            String val = (String) translations.get(key);  // return the translated word!
            if (val == null)  // this shouldn't happen, but can occur with an out-of-date (=incomplete) translation file.
            {
                if (!english) log.warning("Can't find translation for '" + key + "' - returning '" + key + "' unchanged.");
                return key;
            }
            return val;
        }
        catch (MissingResourceException e)
        {
            return key;        // couldn't find a translation, so return the keyword instead.
        }
    }

    /**
     * This attempts to get the localised version of a formatted string,
     * inserting arguments as appropriate (see MessageFormat class
     * for more details).
     * If anything goes wrong, it attempts to return the key
     * string it was given - hence using a meaningfull key is
     * a good harm minimization strategy.
     *
     * @param key  the format pattern; e.g. 'JXplorer has saved {0} ldif entries'.
     * @param args the list of parameters to insert into the format pattern.
     */
    public String get(String key, ArrayList args)
    {
        return get(key, args.toArray());
    }

    /**
     * This attempts to get the localised version of a formatted string,
     * inserting arguments as appropriate (see MessageFormat class
     * for more details).
     * If anything goes wrong, it attempts to return the key
     * string it was given - hence using a meaningfull key is
     * a good harm minimization strategy.
     *
     * @param key  the format pattern; e.g. 'JXplorer has saved {0} ldif entries'.
     * @param args the list of parameters to insert into the format pattern.
     */
    public String get(String key, Object[] args)
    {
        if (key == null)       // sanity check that a valid key has been passed.
        {
            return "null key";
        }
        String val = key;

        if (translations == null || translations.size() == 0)   // we never opened a file...
        {
            if (errorGiven == false)  // only print error message once!  (otherwise we'd print an
            {                         // error for every string in the program!)
                if (!english) log.warning("Unable to translate (" + key + ") - can't find language file.");
                errorGiven = true;
            }
        }
        else
        {
            try
            {
                val = (String) translations.get(key);  // return the translated word!
                if (val == null)  // this shouldn't happen, but can occur with an out-of-date (=incomplete) translation file.
                {
                    if (!english) log.warning("Can't find translation for (" + key + ") - returning unchanged.");
                    val = key; // revert to english
                }
            }
            catch (MissingResourceException e)
            {
                val = key;        // couldn't find a translation, so return the keyword instead.
            }
        }

        return MessageFormat.format(val, args);
    }

    /**
     * This method searches through all the valid permutations of the language
     * files that are found under the path.  It choses the file which is the
     * most specific for the given locale.  Then it loads the data (the translation
     * strings) into a local data store (Hashtable).
     *
     * @param path   the path where the language files are stored.
     * @param locale a specific locale to use.
     */
    private void loadLanguageFile(String path, Locale locale)
    {
        Vector names = getLanguageFileNames(path, locale);

        if (names == null)
        {
            log.warning("Names are null");
            return;
        }

        //TE: the most specific language file is added to the Vector last, so start at the end and work backwards...
        for (int i = names.size() - 1; i >= 0; i--)
        {
            // once a single file has been loaded, we're done...
            if (loadData(new File(names.get(i).toString())) == true)
                return;
        }

        // couldn't succesfully load anything...
        log.warning("Unable to load language file '" + path + "'");
        return;
    }

    /**
     * A file might be named 'en_AU.properties'.  This method tries to figure out
     * from the locale what the language file names are.  The least specific file
     * name is added to the list first and the most specific last.  For example,
     * the least specific file name would be...<br><br>
     * [language].properties followed by<br>
     * [language]_[country].properties followed by<br>
     * [language]_[country]_[variant].properties<br><br>
     * This method adds the file names to the list with and without the extension '.properties'
     *
     * @param path   the path to the language files.
     * @param locale the locale.
     * @return a list of possible language file names.
     */
    private static Vector getLanguageFileNames(String path, Locale locale)
    {
        if (locale == null)
        {
            log.warning("Locale is null");
            return null;
        }

        final Vector result = new Vector(8);
        final String language = locale.getLanguage();
        final int languageLength = language.length();
        final String country = locale.getCountry();
        final int countryLength = country.length();
        final String variant = locale.getVariant();
        final int variantLength = variant.length();
        final StringBuffer temp = new StringBuffer(path);

        if (languageLength + countryLength + variantLength == 0)
        {
            return result;                                      //TE: The locale is "", "", "".
        }

        temp.append(language);

        result.addElement(temp.toString() + ".properties");     //TE: file name is [language].properties
        result.addElement(temp.toString());                     //TE: file name is [language]

        if (countryLength + variantLength == 0)
        {
            return result;                                      //TE: The locale is for example "en", "", "".
        }

        temp.append('_');
        temp.append(country);

        result.addElement(temp.toString() + ".properties");     //TE: file name is [language]_[country].properties
        result.addElement(temp.toString());                     //TE: file name is [language]_[country]

        if (variantLength == 0)
        {
            return result;                                      //TE: The locale is for example "en", "AU", "".
        }

        temp.append('_');
        temp.append(variant);

        result.addElement(temp.toString() + ".properties");     //TE: file name is [language]_[country]_[variant].properties
        result.addElement(temp.toString());                     //TE: file name is [language]_[country]_[variant]

        return result;                                          //TE: The locale is for example "en", "AU", "variant".
    }

    /**
     * This loads the data from a translation file, checking on the way
     * what file format it is in.  (UTF-8, 16bit unicode, or local encoding).
     *
     * @param file the File to read the data InputStream from.
     * @return whether the load data operation was successfull, or whether
     *         it was interupted (for whatever reason; no file, error reading
     *         file, bad encoding, yadda yadda yadda).
     */
    private boolean loadData(File file)
    {
        try
        {
            if (file == null || !file.exists())
                return false;

            log.info("Reading data from " + file.getAbsolutePath());

            // First, slurp all the data from the input stream into a byte array...
            byte[] data = CBUtility.readStream(new FileInputStream(file));

            // Convert the byte array to a String using cunning auto-detecting encoding methods...
//            StringBuffer buffy = new StringBuffer(CBUtility.readI18NByteArray(data));
            StringBuffer buffy = new StringBuffer(CBUtility.readUnicode(data));

            buffy.insert(buffy.length(), '\n');   //TE: make sure the last line has a '\n'!

            // Load up the translations hashtable with the parsed data found in the string...
            return parseData(buffy.toString());
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "Unable to read data from file '" + file.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * Parses the byte array as per a normal resource file
     * (i.e. looking for key/data pairs seperated by an
     * unescaped '=' sign) after first converting the byte
     * array into a String, using whichever language encoding
     * (unicode16, utf8, locale-specific) seems appropriate.
     *
     * @param text the language file as a string.
     * @return true if the language file has been loaded into the
     *         hashtable, false otherwise.
     */
    protected boolean parseData(String text)
    {
        int start = 0, end = 0;
        while ((end = text.indexOf('\n', start)) != -1)
        {
            String line = text.substring(start, end);

            line = line.trim();

            if (line.startsWith("="))
            {
                log.warning("Invalid entry in language file: '" + line + "'");
            }
            else if (line.length() != 0 && line.charAt(0) != '#') // ignore blank lines and commented lines.
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
                    log.warning("Exception parsing data line '" + line + "' -> " + e);
                } // prob. array ex. - ignore this line.
            }

            start = end + 1;
        }

        // check if we added any new translations - if we did, then this was
        // at least partially successfull.
        boolean success = (translations.size() > 0);
        if (success == false)
            log.warning("ParseData unsuccessfull - no new data found");
        return success;
    }

    /**
     * Removes all escapes ('\?' -> '?') from a string.
     * -> Not particularly efficient, but o.k. for short strings.
     */
    private String unescape(String escapeMe)
    {
        int pos = 0;
        while ((pos = escapeMe.indexOf('\\', pos)) >= 0)
            escapeMe = escapeMe.substring(0, pos) + escapeMe.substring(pos + 1);

        return escapeMe;
    }

    /**
     * Returns the translation keys.
     *
     * @return an Enumeration of all the known keys (usually translatable
     *         strings).
     */
    public Enumeration keys()
    {
        return translations.keys();
    }

    /**
     * Returns the translation keys.
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
     * Returns the object corresponding to a given key.
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

    /**
     * Checks if the country code is a valid one according to
     * Locale.getISOCountries().
     *
     * @param language the country code.
     * @return if the newly created Locale or null if the country
     *         code is not valid
     */
    public static Locale createLocale(String language)
    {
        if (language == null)
            return Locale.getDefault();

        String[] langs = Locale.getISOLanguages();
        for (int i = 0; i < langs.length; i++)
            if (language.equalsIgnoreCase(langs[i]))
                return new Locale(langs[i]);

        return Locale.getDefault();
    }
}
