package com.ca.commons.cbutil;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class attempts to launch a program (depending on its file extension) and attempts
 * to open the specified file.  For example: launch MS word (winword.exe) with file 'a.doc'.
 *
 * ... I think this only works on Windows - CB?  (Could try to do something using 'afplay' for audio on OSX?
 *
 * @author Trudi.
 */

public class CBLauncher
{


    /**
     * This class attempts to launch a program (depending on its file extension) and attempts
     * to open the specified file.  For example: launch MS word (winword.exe) with file 'a.doc'.
     *
     * @param extension the file extension that determines what program to use (e.g. '.mp3').
     * @param fileName  the name of the file that needs to be opened (e.g. 'a.mp3').
     */

    public static void launchProgram(String extension, String fileName)
    {
        System.out.println("launching: " + extension + " - " + fileName);

        String command = null;
        StringBuffer fileType = new StringBuffer(20);
        StringBuffer program = new StringBuffer(50);
        Process p;
        InputStream stdOut = null;
        boolean collecting;
        int b;

        if (command == null)
        {
            /*
             * Find out the file type associated with .xls files
             *
             * assoc determines the association between a file extention
             * and the 'file type'.  It will return something like:
             *
             * .xls=Excel.Sheet.8
             *
             * We strip off everything upto (and including) the equals
             */

            try
            {
                p = Runtime.getRuntime().exec("cmd /c assoc " + extension);
                stdOut = p.getInputStream();
                collecting = false;

                while ((b = stdOut.read()) != -1)
                {
                    char c = (char) b;
                    if (c == '\r' || c == '\n')
                    {
                        //Do nothing
                    }
                    else if (collecting)
                    {
                        fileType.append(c);
                    }
                    else if (c == '=')
                    {
                        collecting = true;
                    }
                }
            }
            catch (IOException e)
            {
                CBUtility.error(CBIntText.get("Error trying to associate file extension: {0} with program", new String[]{extension}) + "\n" + e);
            }

            /*
             * Find the program associated with the file type
             *
             * ftype determines the command used to open files of a
             * given type.  It will return something like:
             *
             * Excel.Sheet.8="h:\MSOffice.97\Office\excel.exe" /e
             *
             * We strip off everything upto (and including) the equals
             */

            try
            {
                p = Runtime.getRuntime().exec("cmd /c ftype " + fileType.toString());
                stdOut = p.getInputStream();
                collecting = false;

                while ((b = stdOut.read()) != -1)
                {
                    char c = (char) b;

                    if (c == '\r' || c == '\n')
                    {
                        //Do nothing
                    }
                    else if (collecting)
                    {
                        program.append(c);
                    }
                    else if (c == '=')
                    {
                        collecting = true;
                    }
                }
            }
            catch (IOException e)
            {
                CBUtility.error(CBIntText.get("Error trying to associate file extension: {0} with program", new String[]{extension}) + "\n" + e);
            }
            command = program.toString();
        }

        String fullProgramName = program.toString();
        String runProgramName;

        System.out.println("Running program: " + fullProgramName);

//        if (program.toString().endsWith("%1"))	//TE: for some reason, to get mplayer2 & realplayer working, remove the %1 from the end of the name (they don't write it like the others i.e. "%1").
            if (program.toString().contains("%1"))	//TE: for some reason, to get mplayer2 & realplayer working, remove the %1 from the end of the name (they don't write it like the others i.e. "%1").
            runProgramName = fullProgramName.substring(0, fullProgramName.lastIndexOf("%1") - 1);
        else if (program.toString().endsWith("\"%L\""))	//TE: Midi format.
            runProgramName = fullProgramName.substring(0, fullProgramName.lastIndexOf("\"%L\"") - 1);
        else
            runProgramName = program.toString();

        Runtime r = Runtime.getRuntime();

        try
        {
            //System.out.println("Attempting to run command line: " + runProgramName + " \"" + fileName + "\"");

            r.exec(runProgramName + " \"" + fileName + "\"");
        }
        catch (IOException e)
        {
            CBUtility.error(CBIntText.get("Error occured when trying to launch program: {0} with the file {1}. Either the program found does not support the file type, or the file name was incorrect.",
                    new String[]{runProgramName, fileName}) + "\n\n\n" + e);
        }
    }
}