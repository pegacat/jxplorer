package com.ca.directory.jxplorer;

import com.ca.commons.cbutil.CBIntText;
import com.ca.commons.cbutil.CBUtility;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;



/**
 *    This sets up the button tool bar.  It acts as a quick GUI
 *    to allow common, simple requests to be entered, and calls
 *    tree.SmartPopupTool to do the real work.
 */

public class ButtonBar extends JToolBar
        implements ActionListener
{
    JXplorer jxplorer;
    MainMenu menu;
    JButton connect,disconnect,print,cut,copy,paste,del,newEntry,rename,refresh,stop,pasteAlias,copyDN;

    ButtonRegister br = null;

    private static Logger log = Logger.getLogger(ButtonBar.class.getName());


    /**
     * Sets up the button bar and registers these buttons in the ButtonRegister.
     * @param jxplorer
     */

    public ButtonBar(JXplorer jxplorer)
    {
        super();
        this.jxplorer = jxplorer;
        setFloatable(false);

        menu = jxplorer.mainMenu;   // the toolbar routes most commands through menu

        setSize(750, 20);

        String dir = JXplorer.getProperty("dir.images");
        String dirIcons = JXplorer.getProperty("dir.icons");
        add(connect = new JButton(new ImageIcon(dir + "connect.gif")));
        add(disconnect = new JButton(new ImageIcon(dir + "disconnect.gif")));
        add(print = new JButton(new ImageIcon(dir + "print.gif")));
        addSeparator();
        add(cut = new JButton(new ImageIcon(dir + "cut.gif")));
        add(copyDN = new JButton(new ImageIcon(dir + "copy_dn.gif")));
        add(copy = new JButton(new ImageIcon(dir + "copy.gif")));
        add(paste = new JButton(new ImageIcon(dir + "paste.gif")));
        add(pasteAlias = new JButton(new ImageIcon(dirIcons + "alias.gif")));
        addSeparator();
        add(del = new JButton(new ImageIcon(dir + "delete.gif")));
        addSeparator();
        add(newEntry = new JButton(new ImageIcon(dir + "new.gif")));
        add(rename = new JButton(new ImageIcon(dir + "rename.gif")));
        addSeparator();
        add(refresh = new JButton(new ImageIcon(dir + "refresh.gif")));
        addSeparator();
        add(stop = new JButton(new ImageIcon(dir + "stop.gif")));

        connect.setRolloverIcon(new ImageIcon(dir + "connect_rollover.gif"));
        disconnect.setRolloverIcon(new ImageIcon(dir + "disconnect_rollover.gif"));
        print.setRolloverIcon(new ImageIcon(dir + "print_rollover.gif"));
        cut.setRolloverIcon(new ImageIcon(dir + "cut_rollover.gif"));
        copy.setRolloverIcon(new ImageIcon(dir + "copy_rollover.gif"));
        copyDN.setRolloverIcon(new ImageIcon(dir + "copy_dn_rollover.gif"));
        paste.setRolloverIcon(new ImageIcon(dir + "paste_rollover.gif"));
        pasteAlias.setRolloverIcon(new ImageIcon(dir + "alias_rollover.gif"));
        del.setRolloverIcon(new ImageIcon(dir + "delete_rollover.gif"));
        newEntry.setRolloverIcon(new ImageIcon(dir + "new_rollover.gif"));
        rename.setRolloverIcon(new ImageIcon(dir + "rename_rollover.gif"));
        refresh.setRolloverIcon(new ImageIcon(dir + "refresh_rollover.gif"));
        stop.setRolloverIcon(new ImageIcon(dir + "stop_rollover.gif"));

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

        br = JXplorer.getButtonRegister();

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

        jxplorer.getStopMonitor().addWatcher(stop);    // stopMonitor will adjust the enabled status of the stop button.

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
            jxplorer.getActiveTree().getPopupTool().cut();
        else if (e.getSource() == copy)
            jxplorer.getActiveTree().getPopupTool().copy();
        else if (e.getSource() == copyDN)
            jxplorer.getActiveTree().getPopupTool().copyDN();
        else if (e.getSource() == paste)
            jxplorer.getActiveTree().getPopupTool().paste();
        else if (e.getSource() == pasteAlias)
            jxplorer.getActiveTree().getPopupTool().pasteAlias();
        else if (e.getSource() == del)
            jxplorer.getActiveTree().getPopupTool().delete();
        else if (e.getSource() == newEntry)
            jxplorer.getActiveTree().getPopupTool().newEntry();
        else if (e.getSource() == rename)
            jxplorer.getActiveTree().getPopupTool().rename();
        else if (e.getSource() == refresh)
            jxplorer.getActiveTree().getPopupTool().refresh();
        else if (e.getSource() == stop)
            jxplorer.getStopMonitor().show();
        else
            log.warning("error - unknown option in ButtonBar: " + e.getSource().toString());

    }



    /**
     * Calls the setConnectedState method in ButtonRegister.
     * @see ButtonRegister
     */
    public void setConnected()
    {
        br.setConnectedState();
    }



    /**
     * Calls the setDisconnectState method in ButtonRegister.
     * @see ButtonRegister
     */
    public void setDisconnected()
    {
        br.setDisconnectState();
    }
}