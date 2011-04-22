/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

package com.jogamp.opengl.util;

import java.io.*;
import java.net.*;

/** Utilities for dealing with resources. */

public class Locator {
    private Locator() {}

    /**
     * Locates the resource using 'getResource(String path, ClassLoader cl)',
     * with this context ClassLoader and the path as is,
     * as well with the context's package name path plus the path.
     *
     * @see #getResource(String, ClassLoader)
     */
    public static URL getResource(Class context, String path) {
        if(null == path) {
            return null;
        }
        ClassLoader contextCL = (null!=context)?context.getClassLoader():null;
        URL url = getResource(path, contextCL);
        if (url == null && null!=context) {
            // Try again by scoping the path within the class's package
            String className = context.getName().replace('.', '/');
            int lastSlash = className.lastIndexOf('/');
            if (lastSlash >= 0) {
                String tmpPath = className.substring(0, lastSlash + 1) + path;
                url = getResource(tmpPath, contextCL);
            }
        }
        return url;
    }

    /**
     * Locates the resource using the ClassLoader's facility,
     * the absolute URL and absolute file.
     *
     * @see ClassLoader#getResource(String)
     * @see ClassLoader#getSystemResource(String)
     * @see URL#URL(String)
     * @see File#File(String)
     */
    public static URL getResource(String path, ClassLoader cl) {
        if(null == path) {
            return null;
        }
        URL url = null;
        if (cl != null) {
            url = cl.getResource(path);
            if(!urlExists(url)) {
                url = null;
            }            
        } 
        if(null == url) {
            url = ClassLoader.getSystemResource(path);
            if(!urlExists(url)) {
                url = null;
            }            
        }
        if(null == url) {
            try {
                url = new URL(path);
                if(!urlExists(url)) {
                    url = null;
                }
            } catch (MalformedURLException e) { }
        }
        if(null == url) {
            try {
                File file = new File(path);
                if(file.exists()) {
                    url = file.toURL();
                } else {
                }
            } catch (MalformedURLException e) {}
        }
        return url;
    }

    /**
     * Generates a path for the 'relativeFile' relative to the 'baseLocation'.
     * 
     * @param baseLocation denotes a directory
     * @param relativeFile denotes a relative file to the baseLocation
     */
    public static String getRelativeOf(File baseLocation, String relativeFile) {
        if(null == relativeFile) {
            return null;
        }
        
        while (baseLocation != null && relativeFile.startsWith("../")) {
            baseLocation = baseLocation.getParentFile();
            relativeFile = relativeFile.substring(3);
        }
        if (baseLocation != null) {
            final File file = new File(baseLocation, relativeFile);
            // Handle things on Windows
            return file.getPath().replace('\\', '/');
        }
        return null;
    }

    /**
     * Generates a path for the 'relativeFile' relative to the 'baseLocation'.
     * 
     * @param baseLocation denotes a URL to a file
     * @param relativeFile denotes a relative file to the baseLocation's parent directory
     */
    public static String getRelativeOf(URL baseLocation, String relativeFile) {    
        String urlPath = baseLocation.getPath();
        
        if ( baseLocation.toString().startsWith("jar") ) {
            JarURLConnection jarConnection;
            try {
                jarConnection = (JarURLConnection) baseLocation.openConnection();
                urlPath = jarConnection.getEntryName();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        
        // Try relative path first
        return getRelativeOf(new File(urlPath).getParentFile(), relativeFile);       
    }
    
    /**
     * Returns true, if the url exists,
     * trying to open a connection.
     */
    public static boolean urlExists(URL url) {
        boolean v = false;
        if(null!=url) {
            try {
                URLConnection uc = url.openConnection();
                v = true;
            } catch (IOException ioe) { }
        }
        return v;
    }

}

