/*
 * Provides a call back handler for the GSSAPI/Kerberos code.
 */
package com.ca.commons.jndi;

import java.io.IOException;
import java.awt.*;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.swing.*;

/**
 * Provides a call back handler for the GSSAPI/Kerberos code.  It allows
 * the user to enter username / password details if the kerberos repository
 * isn't already set up.
  * @author vadim, chris betts
 */
public class JXCallbackHandler implements CallbackHandler
{
    // nb - this may appear to be a security problem, since storing password
    // strings is generally unwelcome; however in fact we are simply copying
    // the string from jndi which already has it - so we're no worse off.
    // (we could just read them straight from the jndi environment I guess)
    private static Frame owner = null;
//    private static String userNamePrompt;
//    private static String pwdPrompt;
    private static String promptHeader = "Require Kerberos Credentials";

    /**
     * We optionally pass these things externally so we can smoothly fit into the calling
     * application... otherwise english only defaults are used.
     * @param ownerFrame  - the base gui we will use to show dialogs
     * @param userNamePromptString - the (translated) text to show the user for a user name prompt - NOT CURRENTLY USED
     * @param pwdPromptString - the (translated) text to show the user for a pwd prompt - NOT CURRENTLY USED
     * @param promptHeaderString - the (translated) text to adorn the prompt dialogue window.
     */
    public static void setupGUI(Frame ownerFrame, String userNamePromptString, String pwdPromptString, String promptHeaderString)
    {
        owner = ownerFrame;
//        userNamePrompt = userNamePromptString;
//        pwdPrompt = pwdPromptString;
        promptHeader = promptHeaderString;
    }

    public JXCallbackHandler()
    {
    }

    /**
     * This method passes user credentials back in the event that they have not been
     * set up in a global kerberos cache.
     * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
     */
    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException
    {
        for (int i = 0; i < callbacks.length; i++)
        {

            if (callbacks[i] instanceof NameCallback)
            {

                NameCallback cb = (NameCallback) callbacks[i];
                cb.setName(getUserName(cb.getPrompt()));
            }
            else if (callbacks[i] instanceof PasswordCallback)
            {
                PasswordCallback cb = (PasswordCallback) callbacks[i];
                cb.setPassword(getPassword(cb.getPrompt()));
            }
            else
            {
                throw new UnsupportedCallbackException(callbacks[i]);
            }
        }
    }

    /**
     * This should raise a swing dialogue prompt and get a response from the user.
     * It relies on Callback.getPrompt() to produce a sensible text message to prompt the user with.
     * @param prompt
     * @return the user name
     */
    private String getUserName(String prompt)
    {
        String result = (String)JOptionPane.showInputDialog(owner, prompt, promptHeader, JOptionPane.QUESTION_MESSAGE);
        return result;
    }


    /**
     * This should raise a swing dialogue prompt and get a response from the user.
     * It relies on Callback.getPrompt() to produce a sensible text message to prompt the user with.
     * @param prompt
     * @return the password
     */
    private char[] getPassword(String prompt)
    {
        JPasswordField pwd = new JPasswordField();
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(prompt), BorderLayout.NORTH);
        panel.add(pwd);

        JOptionPane.showMessageDialog(owner, panel, promptHeader, JOptionPane.QUESTION_MESSAGE);
        return pwd.getPassword();
    }

    /**
     * A quicky test method to make sure the gui dialogues are working o.k. ...
     * @param args
     */
    public static void main(String [] args)
    {
        JXCallbackHandler test = new JXCallbackHandler();
        System.out.println("user name = " + test.getUserName("a nice shiny user name"));
        System.out.println("pwd = " + new String(test.getPassword("a nice shiny password")));
        System.exit(0);
    }
}
