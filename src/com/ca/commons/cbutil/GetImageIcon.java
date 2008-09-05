package com.ca.commons.cbutil;

import java.io.*;
import javax.swing.*;
import java.util.jar.*;

public class GetImageIcon{
	 
	/** 
	* @param fileName the full qualified name of the file 
	* from the root of your application. use a "/" before the 
	* fileName, eg. /images/warning.gif 
	*/ 
	    public ImageIcon getImageIcon(String fileName, String jarName) { 
	        int c, i = 0;
	        byte buffer[];
	        JarFile  jfile;
	        JarEntry jentry;
	        InputStream in;
	        ImageIcon m_image;
	 
	        try{
	                // Create Jar-File object from JarFile
	                jfile = new JarFile(jarName);
	        }
	        catch(Exception ex){
	                System.out.println("JarFile-Problem " + "\n" + ex);
	                return null;
	        }
	 
	        try{
	                // create Jar-Entry object from File-Name in JarFile
	                jentry = jfile.getJarEntry(fileName);
	        }
	        catch(Exception ex){
	                System.out.println("JarEntry-Problem " + "\n" + ex);
	                return null;
	        }
	        
	        try{
	                // create InputStream from JarEntry
	                in = jfile.getInputStream(jentry);
	        }
	        catch(Exception ex){
	                System.out.println("JarEntry-Problem " + "\n" + ex);
	                return null;
	        }
	 
	        try{
	                //get uncompressed size of entry data in Jar-File and create byte-array with this size
	                buffer = new byte[(int)jentry.getSize()];
	                
	                //write int-value 'c' (casted to byte) with read() and while-loop in byte-Array
	                while((c = in.read()) != -1){
	                        buffer[i] = (byte)c;
	                        i++;
	                }
	        }
	        catch(Exception ex){
	                System.out.println("in.read()-Problem " + "\n" + ex);
	                return null;
	        }
	 
	        try{
	                //create ImageIcon with byte-Array 'buffer'
	                m_image = new ImageIcon(buffer);
	        }
	        catch(Exception ex){
	                System.out.println("ImageIcon-Problem " + "\n" + ex);
	                return null;
	        }
	        
	        // return the created ImageIcon
	        return m_image;
	    }        
	}