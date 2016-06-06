package com.ca.directory.jxplorer;

/**
 * An interface for different batch jobs, to be passed an array of strings
 * (usually from the JXplorer main method) and a reference to the
 * core JXplorer object.
 *
 * (c) Chris Betts: JXplorer Project
 */
public interface BatchJob
{
    public void execute(JXplorer jx, String[] args);
}
