package com.ca.commons.cbutil;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>This 'International Text' static class acts as a central source of all localised
 * strings.  If it cannot find a localised properties file, the
 * default (english) file is used.  If that file can not be found, the key used to
 * look up the translation is returned unchanged - hence it is useful if the keys
 * are meaningful in their own right (i.e. use 'File' rather than 'String 42' as
 * a key).<p>
 * <p/>
 * <p>Currently the class is static to provide easy access to i18n text by
 * all classes and plugins, and because it is difficult to see how
 * supporting multiple different-locale languages would be helpful (note
 * that multiple languages can be supported simply by using unicode).</p>
 *
 * <p>WARNING: the perl scripts used in the JXplorer project to extract key words and create
 * language files do not work well with strings that have escaped double quotes in them - use ''
 * instead</p>
 */

public class CBIntText
{
    static Locale locale = null;

    static MessageFormat messageFormatter = null;

    static Hashtable translations;

    private static boolean errorGiven = false;  // This is a 'complain once' class - usually it's either working or it's not...

    private static Logger log = Logger.getLogger(CBIntText.class.getName());
    /*
     * If the local language is english, don't print warning messages
     * about missing translation files (it confuses soooo many people)
     */
     
    private static boolean english = true;


    /**
     * This initialises the international text class.
     *
     * @param bundleLocation the name of the i18n bundle (e.g. "language/JX_ja.properties" = japanese).
     * @param customLoader a custom loader that does utf-8 translation magic.
     */
    public static void init(String bundleLocation, ClassLoader customLoader)
    {
        Locale def = Locale.getDefault();
        log.info("Default Locale is: " + def.getDisplayName());
        init(bundleLocation, customLoader, def);
    }

    /**
     * Sets up the language localisation with a specific locale.
     * @param bundleLocation
     * @param customLoader
     * @param newLocale
     */
    public static void init(String bundleLocation, ClassLoader customLoader, Locale newLocale)
    {
        locale = (newLocale!=null)?newLocale:Locale.getDefault();

        log.info("Using Locale: " + locale.getDisplayName() + " for " + locale.getDisplayCountry());
        log.info("Default language, localized for your locale is: " + locale.getDisplayLanguage(locale));
        log.info("Default country name, localized for your locale is: " + locale.getDisplayCountry(locale));

        translations = new Hashtable(500);

        addBundle(bundleLocation, customLoader);

        if (!locale.getLanguage().equals("en"))
            english = false;

        messageFormatter = new MessageFormat("");
        messageFormatter.setLocale(locale);
    }

    /**
     * Once init() has been called, this method can be used to add
     * multiple resource bundles to the universal 'CBIntText' list of
     * translations.  Note that clashes (i.e. translations of identical
     * terms) are resolved with the earliest registered bundle having
     * priority.  (i.e. if both JX and a plugin have a translation for
     * 'file', the JX one will be used).  If this is a problem, remember
     * that the translated string is arbitrary - e.g. a plugin could
     * translate the string 'pluginFile' instead.
     */

    public static void addBundle(String bundleLocation, ClassLoader customLoader)
    {

        log.fine("adding resource bundle: " + bundleLocation); // + " using loader: " + customLoader.toString());

        int startSize = translations.size();

        if (locale == null)
        {
            log.warning(" ERROR: - CBIntText.addBundle() has been called before CBIntText was initialised! - ignoring call.");
            return;
        }

        try
        {
            CBResourceBundle bundle = new CBResourceBundle( bundleLocation, locale, customLoader);
//            CBResourceBundle bundle = new CBResourceBundle(bundleLocation, locale);

            String name = bundle.getString("name");
            log.info(" added language localizaton set: " + ((name == null) ? "(not named)" : name));
            
            // Copy the new resource set into the hashtable, unless
            // a value already exists in the hashtable... (earlier values have precedence).
            
            Enumeration keys = bundle.getKeys();
            while (keys.hasMoreElements())
            {
                Object key = keys.nextElement();
                if (translations.containsKey(key) == false)
                {
                    log.fine("adding key: " + key + " trans: " + bundle.getString((String) key));

                    translations.put(key, bundle.getString((String) key));
                }
            }

        }
        catch (MissingResourceException e)
        {
            log.log(Level.WARNING, "unable to load resource bundle for " + locale.getDisplayLanguage(locale) + " in country " + locale.getDisplayCountry(locale), e);
        }
        finally
        {
            if (startSize < translations.size())  // i.e. we added stuff...
            {
                log.info(" locale language is " + locale.getDisplayLanguage(locale) + " in country " + locale.getDisplayCountry(locale));
            }
            else
            {
                log.info("Unable to load language resource bundle (couldn't even find default 'JX.properties' file)!");
            }
        }
    }

    /**
     * This attempts to get the localised version of a string.
     * If anything goes wrong, it attempts to return the key
     * string it was given - hence using a meaningfull key is
     * a good harm minimization strategy.
     */

    public static String get(String key)
    {
        if (key == null)       // sanity check that a valid key has been passed.
        {
            return "null key";
        }

        if (translations == null || translations.size() == 0)   // we never opened a bundle...
        {
            if (errorGiven == false)  // only print error message once!  (otherwise we'd print an
            {                         // error for every string in the program!)
                if (!english) log.info("Unable to translate (" + key + ") - can't find language resource bundle.");
                errorGiven = true;
            }
            return key;        // try to keep on trucking using the (english) key phrase
        }

        try
        {
            String val = (String) translations.get(key.trim());  // return the translated word!
            if (val == null)  // this shouldn't happen, but can occur with an out-of-date (=incomplete) translation file.
            {
                if (!english) log.fine("Can't find translation for (" + key + ") - returning unchanged.");
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

    public static String get(String key, Object[] args)
    {

        if (key == null)       // sanity check that a valid key has been passed.
        {
            return "null key";
        }
        String val = key;

        if (translations == null || translations.size() == 0)   // we never opened a bundle...
        {
            if (errorGiven == false)  // only print error message once!  (otherwise we'd print an
            {                         // error for every string in the program!)
                if (!english) log.info("Unable to translate (" + key + ") - can't find language resource bundle.");
                errorGiven = true;
            }
        }
        else
        {
            try
            {
                val = (String) translations.get(key.trim());  // return the translated word!
                if (val == null)  // this shouldn't happen, but can occur with an out-of-date (=incomplete) translation file.
                {
                    if (!english) log.fine("Can't find translation for (" + key + ") - returning unchanged.");
                    val = key; // revert to english
                }
            }
            catch (MissingResourceException e)
            {
                val = key;        // couldn't find a translation, so return the keyword instead.
            }
        }

        //XXX it may be better to reuse MessageFormat objects, but this will require some thought...

        return MessageFormat.format(val, args);        // try to keep on trucking using the (english) key phrase

    }
}