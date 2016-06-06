package com.ca.commons.cbutil;

import javax.swing.*;
import java.util.Vector;


/**
 * A JComboBox that sets the KeySelectionManager in the constructors.
 *
 * @author Trudi
 */

public class CBJComboBox extends JComboBox
{

    /**
     * Creates a JComboBox with a default data model.
     * Sets a key selection manager.
     */

    public CBJComboBox()
    {
        super();
        setKeySelectionManager(new CBKeySelectionManager(this));
    }


    /**
     * Creates a JComboBox that takes it's items from an existing ComboBoxModel.
     * Sets a key selection manager.
     */

    public CBJComboBox(ComboBoxModel aModel)
    {
        super(aModel);
        setKeySelectionManager(new CBKeySelectionManager(this));
    }


    /**
     * Creates a JComboBox that contains the elements in the specified array.
     * Sets a key selection manager.
     */

    public CBJComboBox(Object[] items)
    {
        super(items);
        setKeySelectionManager(new CBKeySelectionManager(this));
    }


    /**
     * Creates a JComboBox that contains the elements in the specified Vector.
     * Sets a key selection manager.
     */

    public CBJComboBox(Vector items)
    {
        super(items);
        setKeySelectionManager(new CBKeySelectionManager(this));
    }


    /**
     * Changes the default key selection manager to allow multi character identification
     * of items within the combo box.  For example if the user presses two keys within two
     * seconds this class checks for a word beginning with the two characters that the keys
     * represent (e.g. c+r = creatorsName).  Normally the key selection manager would jump
     * to the first word in the list that begins with a 'c' then the first word in the list
     * that begins with an 'r'.  This behaviour will still occur if the user presses keys that
     * have a duration more than two seconds apart, for example, if a 'c' is pressed then three
     * seconds later an 'r' the list would not jump to 'creatorsName' it would have jumped to
     * the first word in the list beginning with an 'r'.
     */

    public class CBKeySelectionManager implements KeySelectionManager
    {
        /**
         * The keystrokes the user entered before the current one.
         */

        String oldKeys = null;

        /**
         * The position in combo box that the selection is to be moved to.
         */

        int position = -1;

        /**
         * The time in milliseconds of the last keystroke.
         */

        long oldTime = -1;

        /**
         * The combo box that this key-selection-manager is added to.
         */

        CBJComboBox combo;


        /**
         * Constructor that does nothing more than registers the combo box that this
         * key selection manager belongs to.
         *
         * @param combo the CBJComboBox that this key selection manager is added to.
         */

        public CBKeySelectionManager(CBJComboBox combo)
        {
            super();
            this.combo = combo;
        }


        /**
         * A wrapper to selectionForKey(char aKey, ComboBoxModel aModel, long time)...just
         * adds the time so that the manager can determine if two seconds has passed since
         * the last key stroke.
         *
         * @param aKey   a char value, usually indicating a keyboard key that was pressed.
         * @param aModel the component's data model, containing the list of selectable items.
         * @return an int equal to the selected row, where 0 is the first item and -1 is none.
         */

        public int selectionForKey(char aKey, ComboBoxModel aModel)
        {
            return selectionForKey(aKey, aModel, System.currentTimeMillis());
        }


        /**
         * A wrapper to selectionForKey(char aKey, ComboBoxModel aModel, long time)...just
         * adds the time so that the manager can determine if two seconds has passed since
         * the last key stroke.
         *
         * @param aKey   a char value, usually indicating a keyboard key that was pressed.
         * @param aModel the component's data model, containing the list of selectable items.
         * @param time   the time the key stroke was pressed (actually this is not the exact time
         *               the key was pressed but the time the selectionForKey(char aKey, ComboBoxModel aModel)
         *               was called...'time' is just used to determine if two seconds has lapsed and if so
         *               indicating that the 'oldKey' should be cleared.
         * @return an int equal to the selected row, where 0 is the first item and -1 is none.
         */

        public int selectionForKey(char aKey, ComboBoxModel aModel, long time)
        {
            String key = null;		//TE: the beginning of the list item to be searched for within the list.

            if (oldTime > -1)
            {
                //TE: if two seconds have passed since the last keystroke...don't
                //TE: keep the old key.
                if ((time - oldTime) > 2000)
                    oldKeys = null;
            }


            if (oldKeys != null)						//TE: if the old key has not previously been
                key = oldKeys + String.valueOf(aKey);	//TE: assigned make the key only equal the keystroke key.
            else									//TE: if the old key has previously been assigned make
                key = String.valueOf(aKey);			//TE: the key equal the old key + the keystroke key.

            key = key.toLowerCase();

            int count = combo.getItemCount();

            oldKeys = (oldKeys==null)?String.valueOf(aKey):oldKeys + String.valueOf(aKey);			//TE: append the current key to list of old key strokes.
            oldTime = time;							//TE: remember the current time.

            for (int i = 0; i < count; i++)
            {
                //TE: iterate thru the list checking for the first list item that begins with the key.

                Object temp = combo.getItemAt(i);

                if ((((String) temp).toLowerCase()).startsWith(key))	//TE: if there is a match...
                {

                    position = i;
//                    oldKeys = String.valueOf(aKey);	//TE: remember the current key.
//                    oldTime = time;					//TE: remember the current time.

                    return position;
                }
            }

//            oldKeys = (oldKeys==null)?String.valueOf(aKey):oldKeys + String.valueOf(aKey);			//TE: append the current key to list of old key strokes.
//            oldTime = time;							//TE: remember the current time.

            return position;
        }
    }
}