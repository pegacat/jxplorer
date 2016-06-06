package com.ca.directory.jxplorer.search;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Logger;


import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.*;
import com.ca.directory.jxplorer.broker.StopMonitor;
import com.ca.commons.naming.DN;

public class SearchBar extends JToolBar
{

    /**
     * This sets up the quick search tool bar.  It acts as a quick GUI to allow common, simple searches to be entered,
     * and calls SearchExecute to do the real work.
     */
    JXplorerBrowser browser;
    StopMonitor stopMonitor;

    int lastQuickSearchSelection = 0;

    private static Logger log = Logger.getLogger(SearchBar.class.getName());

    public SearchBar(JXplorerBrowser browser)
    {
        super();
        this.browser = browser;

        setFloatable(false);

        final String attFile = "quicksearch.txt";

        setSize(750, 10);

        final CBButton search = new CBButton(CBIntText.get("Quick Search"), CBIntText.get("Click here to perform the search."));
        search.setPreferredSize(new Dimension(90, 20));

        ButtonRegister br = browser.getButtonRegister();
        br.registerItem(br.SEARCH, search);

        browser.getRootPane().setDefaultButton(search);	//TE: Sets the search button as the default - i.e for when the user hits the 'enter' key.

        final CBJComboBox searchAttribute;   // the attribute to search on
        final CBJComboBox searchFtn;         // the search function to use
        final JTextField searchFilter;     // the user's expression

        //XXX read these from a parameter file
        String[] selections = null;
        try
        {
            selections = CBUtility.readStringArrayFile(attFile);
        }
        catch (Exception e)
        {
            selections = null;
        }

        if ((selections == null) || (selections.length == 0))
            selections = new String[]{"cn", "sn", "description", "telephoneNumber", "postalCode", "address"};

        searchAttribute = new CBJComboBox(selections);
        searchAttribute.setEditable(true);
        searchAttribute.setPreferredSize(new Dimension(125, 20));

        searchAttribute.setToolTipText(CBIntText.get("Select a search attribute, or type in a new one (and press enter)."));
        add(searchAttribute);

        String[] ftns = new String[]{"=", "~=", ">=", "<=", "!(=)"};
        searchFtn = new CBJComboBox(ftns);
        searchFtn.setEditable(false);
        searchFtn.setPreferredSize(new Dimension(55, 20));

        searchFtn.setToolTipText(CBIntText.get("Specify the matching relationship for your search."));
        add(searchFtn);

        searchFilter = new JTextField();

        searchFilter.setToolTipText(CBIntText.get("Place the value to match here (you can use wildcards such as '*')."));
        add(searchFilter);

        search.setToolTipText(CBIntText.get("Search from your currently selected node using the searchBar fields."));

        add(search);

        final JXplorerBrowser jx = browser;

        search.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String ftn = searchFtn.getSelectedItem().toString();
                String filter = "(" + searchAttribute.getSelectedItem();

                if ("!(=)".equals(ftn))
                    filter = "(!" + filter + "=" + searchFilter.getText() + "))";
                else if ("rfc2254".equals(searchAttribute.getSelectedItem()))    // Allows users to enter complex search strings in the Search Bar.
                    filter = searchFilter.getText();
                else
                    filter += ftn + searchFilter.getText() + ")";

                DN base = jx.getTree().getCurrentDN();
                if (base == null)
                    base = jx.getTree().getRootDN();

                String aliasOption = "always";
                log.info("Setting search alias option to: [" + aliasOption + "]");
                JXConfig.setProperty("option.ldap.searchAliasBehaviour", aliasOption);

                SearchExecute.run(jx.getSearchTree(), base, filter, new String[]{"objectClass"}, 2, jx.getSearchBroker());
                jx.getTreeTabPane().setSelectedComponent(jx.getResultsPanel());
            }
        });

        searchAttribute.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean newItem = true;
                boolean itemDeleted = false;
                String selection = searchAttribute.getSelectedItem().toString();

                if (selection.length() == 0)
                {
                    int removePos = (lastQuickSearchSelection < searchAttribute.getItemCount()) ? lastQuickSearchSelection : -1;
                    if (removePos < 0) return;
                    searchAttribute.removeItemAt(removePos);
                }
                lastQuickSearchSelection = searchAttribute.getSelectedIndex();
                String[] values = new String[searchAttribute.getItemCount() + 1];
                int j = 0;
                for (int i = 0; i < searchAttribute.getItemCount(); i++)
                {
                    String searchAtt = searchAttribute.getItemAt(i).toString();
                    if (selection.equals(searchAtt))
                        newItem = false;

                    if ((searchAtt != null) && (searchAtt.length() > 0))
                    {
                        if (isAttributeValid(searchAtt))	//TE: check that it doesn't contain any spaces.
                            values[j++] = searchAtt;
                    }
                    else
                        itemDeleted = true;
                }

                if (newItem) // a new item has been added.
                {
                    if (isAttributeValid(selection))	//TE: check that it doesn't contain any spaces.
                        values[j++] = selection;
                }

                if (itemDeleted || newItem)  // write updated list to disk and update combo box
                {
                    for (int i = 0; i < values.length; i++)
                        if ((values[i] != null) && (values[i].toString().length() == 0))
                            values[i] = null;

                    Object[] trimmedArray = CBArray.trimNulls(values);
                    String[] trimmedStrings = new String[trimmedArray.length];
                    searchAttribute.removeAllItems();

                    for (int i = 0; i < trimmedArray.length; i++)
                        trimmedStrings[i] = trimmedArray[i].toString();

                    Arrays.sort(trimmedStrings);

                    for (int i = 0; i < trimmedArray.length; i++)
                        searchAttribute.addItem(trimmedStrings[i]);

                    CBUtility.writeStringArrayFile(attFile, trimmedStrings);

                    searchAttribute.setSelectedItem(selection);
                }
            }
        });

/*
        stopMonitorButton.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                stopMonitor.setVisible(true);
            }
        });
*/
    }

    /**
     * Checks if a given attribute is valid by searching it for a space i.e. a string 'hello colour' would return false,
     * whereas a string 'hello' would return true.  Logs a message if the attribute is not valid.
     * @param attr the string that is being check for a space.
     */
    public boolean isAttributeValid(String attr)
    {
        if (attr.indexOf(" ") > -1)
        {
            log.warning("The value '" + attr + "' in the Search Bar is not a valid attribute."
                    + "  An attribute cannot contain a space in it's name, therefore this value will not be saved in the 'quicksearch.txt' property file.");
            return false;
        }
        else
            return true;
    }
}