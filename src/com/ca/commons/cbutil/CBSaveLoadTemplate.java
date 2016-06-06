package com.ca.commons.cbutil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An object of this cunning class can be added as is
 * to any container containing user-input components
 * such as text fields, check boxes and so forth.  When
 * it is asked to 'save' a template, it scans through
 * its parent container, identifies user-input
 * components, and saves their data to a properties file
 * with the name 'template name' + 'component number'.These
 * can be automatically read back later.<p>
 * <p/>
 * At the moment it only handles the *swing* components: <br>
 * JTextField, CBJComboBox, JToggleButton (JCheckBox, JRadioButton), but it could easily be modified
 * (would need to change JContainer checks to Container).<p>
 * <p/>
 * It will iterate through sub-components, but since there is no
 * "JContainer" class, all sub-components must be explicitly listed
 * in the code; at the moment it only handles JPanel.<p>
 * The fact that it
 * does not handle JPasswordField is considered a feature.<p>
 * <p/>
 * <b>Warning</b> This class is unlikely to correctly handle
 * non-string data; i.e. combo boxes with icons etc.
 */

public class CBSaveLoadTemplate extends JPanel
{
    protected CBButton save, delete, makeDefault;
    protected CBJComboBox loadops;

    public Properties templates;   // the properties object, read from the file
    public String configFile;             // the config file where we load and save stuff...

    int numTemplates;       // the number of options saved in the properties file.

    private boolean saveFlag = false;	//TE: flag to show when the user has saved...used to stop the auto update of fields.

    //Vector illegalComponents = new Vector(); // components that are specifically not to be messed with.

    static final String NUMTEMPLATES = "number_of_templates";
    static final String TEMPLATENAME = "template_name";
    static final String DEFAULT = "default";

    private static Logger log = Logger.getLogger(CBSaveLoadTemplate.class.getName());

    /**
     * Each CBSaveLoadTemplate object must have a file name to
     * read and load data from...
     * @param applicationName the name of the application (used to locate the config directory on some systems)
     * @param fileName the name of the template
     */

    public CBSaveLoadTemplate(String applicationName, String fileName)
    {
        save = new CBButton(CBIntText.get("Save"), CBIntText.get("Click here to save current settings."));
        makeDefault = new CBButton(CBIntText.get("Default"), CBIntText.get("Click here to make the current setting the default."));
        delete = new CBButton(CBIntText.get("Delete"), CBIntText.get("Click here to delete a previously saved setting."));
        loadops = new CBJComboBox();
        templates = new Properties();

        configFile = parseConfigFile(applicationName, fileName);

        CBPanel main = new CBPanel();

        main.add(save);
        main.makeHeavy();
        main.add(loadops);
        main.makeLight();
        main.add(delete);
        main.add(makeDefault);

        setBorder(new TitledBorder(CBIntText.get("Use a Template")));
        setLayout(new BorderLayout());
        add(main);

        save.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                saveFlag = true;
                save();
            }
        });

        loadops.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!saveFlag)							//TE: don't update fields if user has just saved.
                    load();
                saveFlag = false;
            }
        });
        loadops.setToolTipText(CBIntText.get("Click here to load a previously saved setting."));

        delete.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                int response = showMessage();

                if (response != JOptionPane.YES_OPTION)	//TE: delete confirmation dialog.
                    return;

                delete();
            }
        });

        makeDefault.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                makeDefault();
            }
        });
    }


    /**
     * Expose the root combo box for the use of adding listeners 'n stuff.
     */

    public CBJComboBox getLoadComboBox()
    {
        return loadops;
    }

    public CBButton getSaveButton()
    {
        return save;
    }

    /**
     * Opens an dialog that asks if the user wants to delete the template.
     *
     * @return the user response as an integer (see JOptionPane)
     */

    public int showMessage()
    {
        return JOptionPane.showConfirmDialog(this, CBIntText.get("Are you sure you want to delete the template?"),
                CBIntText.get("Delete Confirmation"), JOptionPane.YES_NO_OPTION);
    }


    /**
     * Loads the dafault set up (if one has been saved).
     * - equivalent to manually loading the template
     * named 'default'.
     */
    public void loadDefault()
    {
        String defaultName = templates.getProperty(DEFAULT);
        if ((defaultName != null) && (defaultName.length() != 0))
            loadTemplateName(defaultName);
    }


    /*
     *    parses a file and loads the data into a Properties object,
     *    and then into the loadops combo box.
     *    Returns the full path of the config file.
     *
     * @param applicationName the name of the application (used to find the config file on some systems)
     * @param fileName the name of the actual config file
     *
     */

    protected String parseConfigFile(String applicationName, String fileName)
    {
        String configFile = CBUtility.getPropertyConfigPath(applicationName, fileName);
        if (configFile == null)
        {
            CBUtility.error(this, "Unable to read user home directory ", null);
            return fileName;
        }

        templates = CBUtility.readPropertyFile(configFile);
        if (templates.size() == 0)
        {
            log.info("Initialising config file: " + configFile);
            return configFile;
        }

        String temp = templates.getProperty(NUMTEMPLATES);
        if (temp == null)
        {
            CBUtility.error(this, "Unable to read number of templates parameter from: " + configFile, null);
            return configFile;
        }


        numTemplates = Integer.parseInt(temp);
        ArrayList names = new ArrayList(numTemplates);

        for (int i = 0; i < numTemplates; i++)
        {
            temp = templates.getProperty(TEMPLATENAME + i);
            names.add(temp);
        }

        Collections.sort(names);

        for (int i=0; i<numTemplates; i++)
        {
            loadops.addItem(names.get(i));
        }

        // make sure the default option is currently shown by the combo box.
        String defaultName = templates.getProperty(DEFAULT);
        loadops.setSelectedItem(defaultName);

        return configFile;
    }

    /**
     * Returns the name of the currently selected template; does not trigger any other activity.
     * @return the name of the current template, or an empty string if none.
     */
    public String getCurrentTemplateName()
    {
        String currentTemplateName = (String) loadops.getSelectedItem();
        if (currentTemplateName == null) return "";
        return currentTemplateName;
    }

    /**
     * Sets the current template name, without actually loading that template or triggering any other activity.
     * @param displayTemplateName the name of the template to display.
     */
    public void setCurrentTemplateName(String displayTemplateName)
    {
        if (displayTemplateName != null && !"".equals(displayTemplateName))
            loadops.setSelectedItem(displayTemplateName);
    }
    /**
     * saves a dialog windows state by working through all the
     * components held by the parent container, and if they are
     * (selected) User Input objects (TextFields etc.) saving
     * their contents to a properties list.
     */

    public void save()
    {
        String currentTemplateName = getCurrentTemplateName();
        String templateName = (String) JOptionPane.showInputDialog(this, CBIntText.get("Enter template name") + ":", CBIntText.get("Replace/Create Template"), JOptionPane.QUESTION_MESSAGE, null, null, currentTemplateName);
        if (templateName == null) return; // user cancelled.
        saveTemplateName(templateName);
        loadops.setSelectedItem(templateName);		//TE: make sure the saved template is the current selection in the combo box.
    }

    public void saveTemplateName(String templateName)
    {
        templateName = templateName.replace('.', '-');  // el hack.  We use dots to do magic with... so bar user from using them and hope no-one ever notices...
        if (templateName == null) return; // user selected 'cancel'
        Container parent = getParent();
        saveContainerInfo(parent, templateName);  // save parent container

        /*
         *    Use a brute-force search to discover if we already have
         *    a template with the newly entered name (i.e. user is
         *    updating an existing entry) by iterating through all
         *    template name properties until we find a matching one (or not).
         */
        boolean we_already_got_one = false;
        for (int i = 0; i < numTemplates; i++)
        {
            String test = (String) templates.getProperty(TEMPLATENAME + i);
            if ((test != null) && (test.equals(templateName)))
            {
                we_already_got_one = true;
                break;
            }
        }

        if (we_already_got_one == false)
        {
            templates.setProperty(TEMPLATENAME + numTemplates, templateName);
            numTemplates++;
            templates.setProperty(NUMTEMPLATES, Integer.toString(numTemplates));
            loadops.addItem(templateName);
        }

        saveToFile();

    }

    /**
     * iterates through a container, saving user-entry components as it goes.
     *
     * @param myContainer  container to save
     * @param templateName unique name for this template and component
     */

    public void saveContainerInfo(Container myContainer, String templateName)
    {
        if (myContainer == null)
        {
            log.warning("Unexpected error in CBSaveLoadTemplate.save() - no parent found");
            return;
        }    // should never happen
        Component[] components = myContainer.getComponents();
        for (int i = 0; i < components.length; i++)
        {
            Component c = components[i];

            saveComponent(c, i, templateName);

        }
    }

    /**
     * This method handles the saving of a particular component.  If
     * the class needs to be extended, extra handling for particular
     * components may need to be added, in which case this method
     * can be extended.
     *
     * @param c            the component to save
     * @param componentNo  the index of the component
     * @param templateName the name of the template being saved to.
     */

    protected void saveComponent(Component c, int componentNo, String templateName)
    {
        if ((c instanceof JPanel) && (c != this))  // if it's a container (and not this component), recurse and save those...
        {
            saveContainerInfo((Container) c, templateName + "." + componentNo);
        }
        else if ((c instanceof JScrollPane) && (c != this))
        {
            saveContainerInfo((Container) c, templateName + "." + componentNo);
        }
        else if ((c instanceof JViewport) && (c != this))
        {
            saveContainerInfo((Container) c, templateName + "." + componentNo);
        }
        else
        {
            if (c.getName()!=null)
            {
                saveComponentText(c, templateName + "." + c.getName());
            }
            else
                saveComponentText(c, templateName + "." + componentNo);

            /*
            String saveText = getComponentTextToSave(c);
            if (saveText != null)
                templates.setProperty(templateName + "." + componentNo, saveText);
            */
        }
    }


    /**
     * this ftn returns the text to save for a given component, and
     * is the sister ftn to 'loadComponentText'.  New components
     * (including awt components) can be added here, by
     * extracting their value as text, and making sure that loadComponentText
     * correctly translates that value when loading.
     *
     * @param c the component to retrieve a text value for.
     * @return the text value of the component.
     */
    protected void saveComponentText(Component c, String templateKey)
    {
        if (c == null) return;  // don't get null components

        //if (illegalComponents.contains(c)) return;  // don't get forbidden components.

        String saveText = null;

        try
        {
            if (c instanceof JPasswordField) // don't (usually) save passwords
                saveText = getPasswordDataToSave(new String(((JPasswordField)c).getPassword()));
            else if (c instanceof JTextField)
                saveText = ((JTextField) c).getText();
            else if (c instanceof JTextArea)
                saveText = ((JTextArea) c).getText();
            else if (c instanceof JToggleButton)
                saveText =  String.valueOf(((JToggleButton) c).isSelected());
            else if (c instanceof CBJComboBox)
                saveText =  ((CBJComboBox) c).getSelectedItem().toString();
            //else unknown component - ignore
            if (saveText != null)
            {
                templates.setProperty(templateKey, saveText);
            }



        }
        catch (Exception e)
        {
            // do nothing
        } // possibility of uninitialised objects above...may cause problems.
    }

    /**
     * By default, we do not save passwords.  Future extensions may want to do some sort of
     * crypto storeage thing...
     *
     * @param passwordToSave
     * @return
     */
    protected String getPasswordDataToSave(String passwordToSave)
    {
        return null;
    }

    protected String getPasswordDataFromLoad(String savedPasswordData)
    {
        return "";
    }

    /**
     * Takes a template name, and attempts to read all the (string)
     * values belonging to that template, and toss them in the
     * appropriate numbered component.
     */

    public void load()
    {
        String templateName = getCurrentTemplateName();

        if (templateName.length() == 0)
        {
            CBUtility.error(this, CBIntText.get("No template selected!"), null);
            return;
        }

        loadTemplateName(templateName);
    }

    public void loadTemplateName(String templateName)
    {
        Container parent = getParent();

        loadContainerInfo(parent, templateName);
    }

    public void loadContainerInfo(Container myContainer, String templateName)
    {
        if (myContainer == null)
        {
            log.warning("Unexpected error in CBSaveLoadTemplate.load() - no parent found");
            return;
        }    // should never happen
        
        Component[] components = myContainer.getComponents();

        for (int i = 0; i < components.length; i++)
        {
            Component c = components[i];
            loadComponent(c, i, templateName);
        }
    }


    /**
     * This method handles the loading of a particular component.  If
     * the class needs to be extended, extra handling for particular
     * components may need to be added, in which case this method
     * can be extended.
     *
     * @param c            the component to load
     * @param componentNo  the index of the component
     * @param templateName the name of the template being loaded from.
     */

    protected void loadComponent(Component c, int componentNo, String templateName)
    {
        if (c instanceof JPanel)   // recurse into container details.
        {
            loadContainerInfo((Container) c, templateName + "." + componentNo);
        }
        else if ((c instanceof JScrollPane))
        {
            loadContainerInfo((Container) c, templateName + "." + componentNo);
        }
        else if ((c instanceof JViewport))
        {
            loadContainerInfo((Container) c, templateName + "." + componentNo);
        }
        else
        {
            String text = (String) templates.get(templateName + "." + componentNo);  // often there won't be a value...

            if (text == null && c.getName()!=null) // adding support for saving named components in human readable config files...
            {
                text = (String) templates.get(templateName + "." + c.getName());  // often there won't be a value...
            }

            if (text != null)                               // ... if there is, load the data up!
            {
                loadComponentText(c, text);
            }
        }
    }

    public void loadComponentText(Component c, String text)
    {
        //if (illegalComponents.contains(c)) return;  // don't load forbidden components.

        if (c instanceof JTextField)
            ((JTextField) c).setText(text);
        if (c instanceof JPasswordField)
            ((JPasswordField) c).setText(getPasswordDataFromLoad(text));
        else if (c instanceof JTextArea)
            ((JTextArea) c).setText(text);
        else if (c instanceof TextField)
            ((TextField) c).setText(text);
        else if (c instanceof JToggleButton)
            ((JToggleButton) c).setSelected("true".equalsIgnoreCase(text));
        else if (c instanceof CBJComboBox)
            ((CBJComboBox) c).setSelectedItem(text);
    }

    public void delete()
    {
        String templateName = getCurrentTemplateName();
        if (templateName.length() == 0)
        {
            CBUtility.error(this, "No template selected!", null);
            return;
        }

        Container parent = getParent();
        if (parent == null)
        {
            log.warning("Unexpected error in CBSaveLoadTemplate.delete() - no parent found");
            return;
        }    // should never happen
        deleteComponentInfo(parent, templateName);

        for (int i = 0; i < numTemplates; i++)
        {
            if (templateName.equals((String) templates.get(TEMPLATENAME + i)))
            {
                templates.remove(TEMPLATENAME + i);
                numTemplates--;
                templates.put(NUMTEMPLATES, Integer.toString(numTemplates));
                loadops.removeItem(templateName);

                // and shuffle down all the values from above!
                // (you know, there's got to be a neater way of doing this...)
                for (int j = i + 1; j <= numTemplates; j++)
                {
                    templateName = (String) templates.get(TEMPLATENAME + j);
                    templates.put(TEMPLATENAME + (j - 1), templateName);
                }
                templates.remove(TEMPLATENAME + numTemplates);

                break;
            }
        }
        saveToFile();
    }

    public void deleteComponentInfo(Container myContainer, String templateName)
    {
        Component[] components = myContainer.getComponents();


        // Brute force delete - most of these don't exist, but attempting deletion does no harm...
        for (int i = 0; i < components.length; i++)
        {
            if (components[i] instanceof JPanel)
                deleteComponentInfo((Container) components[i], templateName + "." + i);
            else if ((components[i] instanceof JScrollPane))
                deleteComponentInfo((Container) components[i], templateName + "." + i);
            else if ((components[i] instanceof JViewport))
                deleteComponentInfo((Container) components[i], templateName + "." + i);
            else
                deleteComponentInfo(templateName + "." + i);//templates.remove(templateName + "." + i);
        }

        try
        {	//TE: if the default template is being deleted, also delete the 'defaul' entry in the property file.
            if (templates.getProperty("default") != null && templates.getProperty("default").equalsIgnoreCase(templateName))
                templates.remove("default");
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "No default template. ", e);
        }
    }


    /**
     * Deletes a template that is saved in the property file.  Can be extended
     * to account for individual component handling e.g. combo box visibility.
     *
     * @param templateName the name of the template that is to be deleted from the property file.
     */

    public void deleteComponentInfo(String templateName)
    {
        templates.remove(templateName);
    }

    public void makeDefault()
    {
        templates.setProperty(DEFAULT, getCurrentTemplateName());
        saveToFile();
    }

    public void saveToFile()
    {
        CBUtility.writePropertyFile(configFile, templates, "");
    }
    /*
    public void addIllegalComponent(Component c)
    {
        illegalComponents.add(c);
    }
    */

}