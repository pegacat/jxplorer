package com.ca.directory.jxplorer;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import com.ca.commons.cbutil.CBIntText;
import com.ca.commons.cbutil.Theme;



/**
 *    This sets up the button tool bar.  It acts as a quick GUI
 *    to allow common, simple requests to be entered, and calls
 *    tree.SmartPopupTool to do the real work.
 */

public class ButtonBar extends JToolBar
        implements ActionListener
{
    JXplorerBrowser browser;
    MainMenu menu;
    JButton connect,disconnect,print,cut,copy,paste,del,newEntry,rename,refresh,stop,pasteAlias,copyDN;

    ButtonRegister br = null;

    private static Logger log = Logger.getLogger(ButtonBar.class.getName());


    /**
     * Sets up the button bar and registers these buttons in the ButtonRegister.
     * @param browser
     */

    public ButtonBar(JXplorerBrowser browser)
    {
        super();
        this.browser = browser;
        setFloatable(false);

        menu = browser.mainMenu;   // the toolbar routes most commands through menu

        setSize(750, 20);

        add(connect = new JButton(new ImageIcon(Theme.getInstance().getDirImages() + "connect.gif")));
        add(disconnect = new JButton(new ImageIcon(Theme.getInstance().getDirImages() + "disconnect.gif")));
        add(print = new JButton(new ImageIcon(Theme.getInstance().getDirImages() + "print.gif")));
        addSeparator();
        add(cut = new JButton(new ImageIcon(Theme.getInstance().getDirImages() + "cut.gif")));
        add(copyDN = new JButton(new ImageIcon(Theme.getInstance().getDirImages() + "copy_dn.gif")));
        add(copy = new JButton(new ImageIcon(Theme.getInstance().getDirImages() + "copy.gif")));
        add(paste = new JButton(new ImageIcon(Theme.getInstance().getDirImages() + "paste.gif")));
        add(pasteAlias = new JButton(new ImageIcon(Theme.getInstance().getDirIcons() + "alias.gif")));
        addSeparator();
        add(del = new JButton(new ImageIcon(Theme.getInstance().getDirImages() + "delete.gif")));
        addSeparator();
        add(newEntry = new JButton(new ImageIcon(Theme.getInstance().getDirImages() + "new.gif")));
        add(rename = new JButton(new ImageIcon(Theme.getInstance().getDirImages() + "rename.gif")));
        addSeparator();
        add(refresh = new JButton(new ImageIcon(Theme.getInstance().getDirImages() + "refresh.gif")));
        addSeparator();
        add(stop = new JButton(new ImageIcon(Theme.getInstance().getDirImages() + "stop.gif")));

        connect.setRolloverIcon(new ImageIcon(Theme.getInstance().getDirImages() + "connect_rollover.gif"));
        disconnect.setRolloverIcon(new ImageIcon(Theme.getInstance().getDirImages() + "disconnect_rollover.gif"));
        print.setRolloverIcon(new ImageIcon(Theme.getInstance().getDirImages() + "print_rollover.gif"));
        cut.setRolloverIcon(new ImageIcon(Theme.getInstance().getDirImages() + "cut_rollover.gif"));
        copy.setRolloverIcon(new ImageIcon(Theme.getInstance().getDirImages() + "copy_rollover.gif"));
        copyDN.setRolloverIcon(new ImageIcon(Theme.getInstance().getDirImages() + "copy_dn_rollover.gif"));
        paste.setRolloverIcon(new ImageIcon(Theme.getInstance().getDirImages() + "paste_rollover.gif"));
        pasteAlias.setRolloverIcon(new ImageIcon(Theme.getInstance().getDirImages() + "alias_rollover.gif"));
        del.setRolloverIcon(new ImageIcon(Theme.getInstance().getDirImages() + "delete_rollover.gif"));
        newEntry.setRolloverIcon(new ImageIcon(Theme.getInstance().getDirImages() + "new_rollover.gif"));
        rename.setRolloverIcon(new ImageIcon(Theme.getInstance().getDirImages() + "rename_rollover.gif"));
        refresh.setRolloverIcon(new ImageIcon(Theme.getInstance().getDirImages() + "refresh_rollover.gif"));
        stop.setRolloverIcon(new ImageIcon(Theme.getInstance().getDirImages() + "stop_rollover.gif"));

        Component buttons[] = this.getComponents();
        for (int i = 0; i < buttons.length; i++)
            if (buttons[i] instanceof JButton)  // which it damn well should be...
            {
                JButton temp = ((JButton) buttons[i]);
                temp.addActionListener(this);
                temp.setRolloverEnabled(true);
                temp.setSize(16, 16);
            }

        connect.setToolTipText(CBIntText.get("Connect to a DSA."));
        disconnect.setToolTipText(CBIntText.get("Disconnect from a DSA."));
        print.setToolTipText(CBIntText.get("Print out the current entry."));
        cut.setToolTipText(CBIntText.get("Select a subtree to move."));
        copy.setToolTipText(CBIntText.get("Select a subtree to copy."));
        copyDN.setToolTipText(CBIntText.get("Copy the Distinguished Name of an entry to the clipboard."));
        paste.setToolTipText(CBIntText.get("Paste a previously selected subtree."));
        pasteAlias.setToolTipText(CBIntText.get("Paste an Alias."));
        del.setToolTipText(CBIntText.get("Delete an entry."));
        newEntry.setToolTipText(CBIntText.get("Create a new entry."));
        rename.setToolTipText(CBIntText.get("Rename an entry."));
        refresh.setToolTipText(CBIntText.get("Refresh an entry."));
        stop.setToolTipText(CBIntText.get("Cancel queries."));

        br = browser.getButtonRegister();

        br.registerItem(br.PASTE, paste);
        br.registerItem(br.PASTE_ALIAS, pasteAlias);
        br.registerItem(br.COPY, copy);
        br.registerItem(br.COPY_DN, copyDN);
        br.registerItem(br.CUT, cut);
        br.registerItem(br.CONNECT, connect);
        br.registerItem(br.DISCONNECT, disconnect);
        br.registerItem(br.PRINT, print);
        br.registerItem(br.DELETE, del);
        br.registerItem(br.NEW, newEntry);
        br.registerItem(br.RENAME, rename);
        br.registerItem(br.REFRESH, refresh);
        br.registerItem(br.STOP, stop);

        br.setItemEnabled(br.STOP, false);

        browser.getStopMonitor().addWatcher(stop);    // stopMonitor will adjust the enabled status of the stop button.

        setDisconnected();

        setMargin(new Insets(2, 0, 2, 0));

        doLayout();
    }



    /**
     * Calls the appropriate method in SmartPopupTool to handle
     * the request.
     * @param e
     */
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == connect)
            menu.connect();
        else if (e.getSource() == disconnect)
            menu.disconnect();
        else if (e.getSource() == print)
            menu.print();
        else if (e.getSource() == cut)
            browser.getActiveTree().getPopupTool().cut();
        else if (e.getSource() == copy)
            browser.getActiveTree().getPopupTool().copy();
        else if (e.getSource() == copyDN)
            browser.getActiveTree().getPopupTool().copyDN();
        else if (e.getSource() == paste)
            browser.getActiveTree().getPopupTool().paste();
        else if (e.getSource() == pasteAlias)
            browser.getActiveTree().getPopupTool().pasteAlias();
        else if (e.getSource() == del)
            browser.getActiveTree().getPopupTool().delete();
        else if (e.getSource() == newEntry)
            browser.getActiveTree().getPopupTool().newEntry();
        else if (e.getSource() == rename)
            browser.getActiveTree().getPopupTool().rename();
        else if (e.getSource() == refresh)
            browser.getActiveTree().getPopupTool().refresh();
        else if (e.getSource() == stop)
            browser.getStopMonitor().show();
        else
            log.warning("error - unknown option in ButtonBar: " + e.getSource().toString());

    }



    /**
     * Calls the setConnectedState method in ButtonRegister.
     * @see ButtonRegister
     */
    /*
    public void setConnected()
    {
        br.setConnectedState();
    }
    */


    /**
     * Calls the setDisconnectState method in ButtonRegister.
     * @see ButtonRegister
     */
    public void setDisconnected()
    {
        br.setDisconnectState();
    }
}