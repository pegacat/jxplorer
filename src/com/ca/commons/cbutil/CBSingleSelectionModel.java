package com.ca.commons.cbutil;

import javax.swing.*;


/**
 * Create a SINGLE_SELECTION ListSelectionModel that calls a new
 * method, updateSingleSelection(), each time the selection
 * changes.  This can be a little bit more convienent than using the
 * ListModels ListSelectionListener, since ListSelectionListeners
 * are only given the range of indices that the change spans.
 */

public class CBSingleSelectionModel extends DefaultListSelectionModel
{
    JList list = null;

    public CBSingleSelectionModel(JList list)
    {
        this.list = list;
        setSelectionMode(SINGLE_SELECTION);
    }

    public void setSelectionInterval(int index0, int index1)
    {
        int oldIndex = getMinSelectionIndex();
        super.setSelectionInterval(index0, index1);
        int newIndex = getMinSelectionIndex();

        if (oldIndex != newIndex)
        {
            updateSingleSelection(oldIndex, newIndex);
        }
    }

    public void updateSingleSelection(int oldIndex, int newIndex)
    {
        list.ensureIndexIsVisible(newIndex);
    }
}
