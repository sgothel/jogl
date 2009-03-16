package com.sun.opengl.impl.io;

import javax.media.opengl.util.*;

import java.util.*;
import java.nio.*;
import java.io.*;
import java.net.*;

/** Utilities for dealing with streams. */

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

