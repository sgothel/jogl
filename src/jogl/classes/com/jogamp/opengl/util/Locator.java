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
        URL url = null;
        if (cl != null) {
            url = cl.getResource(path);
        } else {
            url = ClassLoader.getSystemResource(path);
        }
        if(!urlExists(url)) {
            url = null;
            try {
                url = new URL(path);
            } catch (MalformedURLException e) { }
        }
        if(!urlExists(url)) {
            url = null;
            try {
                File file = new File(path);
                if(file.exists()) {
                    url = file.toURL();
                }
            } catch (MalformedURLException e) {}
        }
        return url;
    }

    /**
     * Generates a path for the 'relativeFile' relative to the 'absoluteFileLocation'
     */
    public static String getRelativeOf(String absoluteFileLocation, String relativeFile) {
        File file = new File(absoluteFileLocation);
        file = file.getParentFile();
        while (file != null && relativeFile.startsWith("../")) {
            file = file.getParentFile();
            relativeFile = relativeFile.substring(3);
        }
        if (file != null) {
            String res = new File(file, relativeFile).getPath();
	        // Handle things on Windows
            return res.replace('\\', '/');
        } else {
            return relativeFile;
        }
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

