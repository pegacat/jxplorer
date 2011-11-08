package com.ca.directory.jxplorer;

import java.awt.*;
import java.util.*;



/**
 * This class should be used to register the common buttons and menu items in JXplorer.
 * JXplorer has three sets of the same thing; edit menu items, tool bar buttons and the
 * tree's popup menu items.  Once registered with this class, a group of components can be
 * enabled/disabled in one operation.<br><br>
 * <b>The way it works</b><br>
 * A component (button, menu item etc) is registered via registerItem using a key.  The
 * key is one of CONNECT, DISCONNECT...etc.  The key is added the ButtonContainer and its
 * value is an ArrayList of the component that is registered.  If more than one component
 * is registered against the key (which is the point of this class), that component is just
 * added to the key's ArrayList of components rather than making a new entry in the
 * ButtonContainer.<br><br>
 * To set a component's state use setItemEnabled.  Pass in the key and whether you want it enabled
 * or not.  This will set the state over all the components registered against the given key.
 * @author Trudi.
 */

public class ButtonRegister
{
    ButtonContainer bc = null;

    //TE: keys...
    public final String CONNECT         = "CONNECT";
    public final String DISCONNECT      = "DISCONNECT";
    public final String PRINT           = "PRINT";
    public final String CUT             = "CUT";
    public final String COPY            = "COPY";
    public final String COPY_DN         = "COPY_DN";
    public final String PASTE           = "PASTE";
    public final String PASTE_ALIAS     = "PASTE_ALIAS";
    public final String DELETE          = "DELETE";
    public final String NEW             = "NEW";
    public final String NEW_WINDOW      = "NEW_WINDOW";
    public final String RENAME          = "RENAME";
    public final String REFRESH         = "REFRESH";
    public final String REFRESH_TREE    = "REFRESH_TREE";
    public final String BOOKMARKS       = "BOOKMARKS";
    public final String SEARCH          = "SEARCH";
    public final String STOP            = "STOP";
    public final String LDIF            = "LDIF";

    private final String[] components = new String[]{CONNECT,
                                                   DISCONNECT,
                                                   PRINT,
                                                   CUT,
                                                   COPY,
                                                   COPY_DN,
                                                   PASTE,
                                                   PASTE_ALIAS,
                                                   DELETE,
                                                   NEW,
                                                   NEW_WINDOW,
                                                   RENAME,
                                                   REFRESH,
                                                   REFRESH_TREE,
                                                   BOOKMARKS,
                                                   SEARCH,
                                                   STOP,
                                                   LDIF};


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
        if(bc == null)
            bc = new ButtonContainer();

        bc.put(key, comp);
    }

    /**
     * Sets a component's state.  This will set the state over all the components
     * registered against the given key.
     * @param key the key of the component (this.CONNECT, this.DISCONNECT...etc).
     * @param enabled the state to set the component to (true = enabled, false = disabled).
     */
    public void setItemEnabled(String key, boolean enabled)
    {
        if (key.equals(COPY_DN))
        {
            //System.out.println("bloop");
        }

        if (key.equals(COPY))
        {
            //System.out.println("bloop");
        }

        if (key.equals(SEARCH))
        {
            //System.out.println("bloop " + enabled);
        }

        if(bc != null)
        {
            ArrayList temp = bc.get(key);
            if(temp != null)
                for (int i = 0; i < temp.size(); i++)
                    ((Component)temp.get(i)).setEnabled(enabled);
        }
    }

    /**
     * Sets the state of common commonents (COPY, CUT, DELETE, NEW,
     * RENAME, PASTE_ALIAS (only if state is false), PASTE (only if state is false)).
     * @param state true to enable, false to disable.
     */
    public void setCommonState(boolean state)
    {
        setItemEnabled(COPY, state);
        setItemEnabled(COPY_DN, state);
        setItemEnabled(CUT, state);
        setItemEnabled(DELETE, state);
        setItemEnabled(NEW, state);
        setItemEnabled(RENAME, state);
        setItemEnabled(BOOKMARKS, state);
        setItemEnabled(SEARCH, state);
        setItemEnabled(LDIF, state);

        if(state == false)
        {
            setItemEnabled(PASTE_ALIAS, state);
            setItemEnabled(PASTE, state);
        }
    }

    /**
     * Sets all the components to disabled except for CONNECT.
     */
    public void setDisconnectState()
    {
        for(int i=0;i<components.length; i++)
            if(i==0)
                setItemEnabled(components[i], true);    //TE: enable connection.
            else
                setItemEnabled(components[i], false);
    }

    /**
     * Sets all the components to enabled except STOP, PASTE and PASTE_ALIAS.
     */
    public void setConnectedState()
    {
        /*
        for(int i=0;i<components.length; i++)
        {
            if(i==6 || i==7 || i==15)
                setItemEnabled(components[i], false);   //TE: disable stop, paste & paste alias.
            else
                setItemEnabled(components[i], true);
        }
        */

        for (String component:components)
        {
            if (component.equals(STOP))
                setItemEnabled(component, false);
            else if (component.equals(PASTE))
                setItemEnabled(component, false);
            else if (component.equals(PASTE_ALIAS))
                setItemEnabled(component, false);
            else
                setItemEnabled(component, true);
        }
    }

    /**
     * Makes a new kind of hash table: one that allows
     * multiple objects to be stored against one key.
     * It does this by using String=ArrayList.
     */
    class ButtonContainer extends Hashtable
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
            ArrayList temp = (ArrayList)super.get(key);
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
            return (ArrayList)super.get(key);
        }
    }
}
