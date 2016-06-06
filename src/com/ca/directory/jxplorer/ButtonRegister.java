package com.ca.directory.jxplorer;

import java.awt.*;
import java.util.*;
import java.util.List;


/**
 * This class should be used to register the common buttons and menu items in JXplorer.
 * JXplorer has three sets of the same thing; edit menu items, tool bar buttons and the
 * tree's popup menu items.  Once registered with this class, a group of components can be
 * enabled/disabled in one operation.<br><br>
 * <b>The way it works</b><br>
 * A component (button, menu item etc) is registered via registerItem using a key.  A set of standard
 * keys for base components: (CONNECT, DISCONNECT...etc) are provided. The key is added to a lookup register
 * of buttons ('buttonRegister') which returns an ArrayList of the component that is registered.
 *
 * If more than one component
 * is registered against the key (which is the point of this class), that component is just
 * added to the key's ArrayList of components rather than making a new entry in the
 * ButtonContainer.<br><br>
 *
 * To set a component's state use setItemEnabled.  Pass in the key and whether you want it enabled
 * or not.  This will set the state over all the components registered against the given key.
 *
 * Alternatively, the button will automatically be set to enabled if the browser is connected, or
 * disabled when the browser is offline.  (Some special handling is done for 'standard' buttons)
 * enumerated below.
 *
 * @author Trudi.
 */

public class ButtonRegister
{
    ButtonContainer buttonRegister = null;

    //TE: keys...
    public static final String CONNECT         = "CONNECT";
    public static final String DISCONNECT      = "DISCONNECT";
    public static final String PRINT           = "PRINT";
    public static final String CUT             = "CUT";
    public static final String COPY            = "COPY";
    public static final String COPY_DN         = "COPY_DN";
    public static final String PASTE           = "PASTE";
    public static final String PASTE_ALIAS     = "PASTE_ALIAS";
    public static final String DELETE          = "DELETE";
    public static final String NEW             = "NEW";
    public static final String NEW_WINDOW      = "NEW_WINDOW";
    public static final String RENAME          = "RENAME";
    public static final String REFRESH         = "REFRESH";
    public static final String REFRESH_TREE    = "REFRESH_TREE";
    public static final String BOOKMARKS       = "BOOKMARKS";
    public static final String SEARCH          = "SEARCH";
    public static final String STOP            = "STOP";
    public static final String LDIF            = "LDIF";



    /*
     *  A list of all the buttons that are used (sometimes in both the menu bar and pop up tools).
     */

/*    private final List<String> components = Arrays.asList(CONNECT, DISCONNECT, PRINT, CUT,COPY,COPY_DN,PASTE,
                                                       PASTE_ALIAS, DELETE, NEW, NEW_WINDOW,RENAME,REFRESH,
                                                       REFRESH_TREE,BOOKMARKS,SEARCH,STOP,LDIF); */

    // getting odd errors when using old config templates; apparently we try to add new components in some circumstances?  Hence switch to resizeable arrays...

    private final ArrayList<String> components = new ArrayList<String>(Arrays.asList(CONNECT, DISCONNECT, PRINT, CUT,COPY,COPY_DN,PASTE,
                                                       PASTE_ALIAS, DELETE, NEW, NEW_WINDOW,RENAME,REFRESH,
                                                       REFRESH_TREE,BOOKMARKS,SEARCH,STOP,LDIF));



    /*
     *  A list of buttons that come 'on' when we're able to edit (including offline editing of ldif files).
     *  NOTE: this was historically used to toggle between connected/disconnected states
     */
/*    private List<String> editingButtons = Arrays.asList(COPY, COPY_DN, CUT, DELETE, NEW, RENAME, LDIF); */

    private ArrayList<String> editingButtons = new ArrayList<String>(Arrays.asList(COPY, COPY_DN, CUT, DELETE, NEW, RENAME, LDIF));


    /*
     *  A list of buttons that are used when we have a directory that we can search
     */
    private List<String> searchButtons = Arrays.asList(BOOKMARKS, SEARCH);



    /**
     * Empty constructor.
     */
    public ButtonRegister()
    {
    }

    /**
     * Registers a component against a key in the ButtonContainer.
     * @param key the key of the component (this.CONNECT, this.DISCONNECT...etc).
     * @param comp the component (button/menu item) to register.
     */
    public void registerItem(String key, Component comp)
    {
        if(buttonRegister == null)
            buttonRegister = new ButtonContainer();

        if (!components.contains(key))
            components.add(key);

        buttonRegister.put(key, comp);
    }

    /**
     * Sets a component's state.  This will set the state over all the components
     * registered against the given key.
     * @param key the key of the component (this.CONNECT, this.DISCONNECT...etc).
     * @param enabled the state to set the component to (true = enabled, false = disabled).
     */
    public void setItemEnabled(String key, boolean enabled)
    {
        if(buttonRegister != null)
        {
            ArrayList temp = buttonRegister.get(key);
            if(temp != null)
                for (int i = 0; i < temp.size(); i++)
                    ((Component)temp.get(i)).setEnabled(enabled);
        }
    }



    /**
     * Extension point, that allows us to add a new 'editing button' to JX
     * (e.g. if we want to make 'save as CSV' available when we're working offlin on an LDIF
     * file, we can add it here...)
     * @param key
     */
    public void setAsEditingButton(String key)
    {
        editingButtons.add(key);
    }

    /**
     * Sets the state of buttons used for editing the tree -
     * e.g.common commonents (COPY, CUT, DELETE, NEW,
     * RENAME, PASTE_ALIAS (only if state is false), PASTE (only if state is false)).
     *
     * Note that non 'standard' buttons are unaffected by this.
     * @param state true to enable, false to disable.
     */
    public void setEditingButtons(boolean state)
    {

        for (String editingButton:editingButtons)
        {
            // only reset paste and paste_alias if we're turning things *off*.  Don't turn them *on* when we
            // turn other buttons on, as we probably don't have anything to actually paste!
            if (editingButton.equals(PASTE) || editingButton.equals(PASTE_ALIAS))
            {
                if (state==false)
                    setItemEnabled(editingButton, false);
            }
            setItemEnabled(editingButton, state);
        }
    }

    /**
     * Sets all the components to disabled except for CONNECT.
     */
    public void setDisconnectState()
    {

        for (String component:components)
        {
            if (component.equals(CONNECT))
                setItemEnabled(component, true);
            else
                setItemEnabled(component, false);
        }
    }

    /**
     * Sets all the components to enabled except STOP, PASTE and PASTE_ALIAS.
     */
    public void setConnectedState(boolean readOnly)
    {

        for (String component:components)
        {
            if (component.equals(STOP) || component.equals(PASTE) || component.equals(PASTE_ALIAS))
                setItemEnabled(component, false);
            else if (readOnly && editingButtons.contains(component))
                setItemEnabled(component, false);
            else
                setItemEnabled(component, true);    // should pick up 'search' and 'bookmark' implicitly
        }
    }

    /**
     * Makes a new kind of hash table: one that allows
     * multiple objects to be stored against one key.
     * It does this by using String=ArrayList.
     */
    class ButtonContainer extends Hashtable<String, ArrayList>
    {
        /**
         * Empty constructor.
         */
        public ButtonContainer() {}

        /**
         * Registers a component against a key in the ButtonContainer.  It creates a
         * new ArrayList if one does exist for the key, otherwise it uses the existing one.
         * @param key the key of the component (this.CONNECT, this.DISCONNECT...etc).
         * @param comp the component (button/menu item) to register.
         */
        public void put(String key, Component comp)
        {
            ArrayList temp = super.get(key);
            if(temp == null)
                temp = new ArrayList();

            if(!temp.contains(comp))
                temp.add(comp);

            super.put(key, temp);
        }

        /**
         * Returns the ArrayList of components that is stored against
         * the key.
         * @param key the key of the component (this.CONNECT, this.DISCONNECT...etc).
         * @return the ArrayList of components that is stored against
         * the key.
         */
        public ArrayList get(String key)
        {
            return super.get(key);
        }
    }
}
