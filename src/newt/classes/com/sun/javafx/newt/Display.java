/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.javafx.newt;

import javax.media.nativewindow.*;

public abstract class Display {

    private static Class getDisplayClass(String type) 
        throws ClassNotFoundException 
    {
        Class displayClass = null;
        if (NativeWindowFactory.TYPE_EGL.equals(type)) {
            displayClass = Class.forName("com.sun.javafx.newt.opengl.kd.KDDisplay");
        } else if (NativeWindowFactory.TYPE_WINDOWS.equals(type)) {
            displayClass = Class.forName("com.sun.javafx.newt.windows.WindowsDisplay");
        } else if (NativeWindowFactory.TYPE_MACOSX.equals(type)) {
            displayClass = Class.forName("com.sun.javafx.newt.macosx.MacDisplay");
        } else if (NativeWindowFactory.TYPE_X11.equals(type)) {
            displayClass = Class.forName("com.sun.javafx.newt.x11.X11Display");
        } else if (NativeWindowFactory.TYPE_AWT.equals(type)) {
            displayClass = Class.forName("com.sun.javafx.newt.awt.AWTDisplay");
        } else {
            throw new RuntimeException("Unknown display type \"" + type + "\"");
        }
        return displayClass;
    }

    protected static Display create(String type, String name) {
        try {
            Class displayClass = getDisplayClass(type);
            Display display = (Display) displayClass.newInstance();
            display.name=name;
            display.createNative();
            if(null==display.aDevice) {
                throw new RuntimeException("Display.createNative() failed to instanciate an AbstractGraphicsDevice");
            }
            return display;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void destroy() {
        closeNative();
    }

    protected static Display wrapHandle(String type, String name, AbstractGraphicsDevice aDevice) {
        try {
            Class displayClass = getDisplayClass(type);
            Display display = (Display) displayClass.newInstance();
            display.name=name;
            display.aDevice=aDevice;
            return display;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void createNative();
    protected abstract void closeNative();

    public String getName() {
        return name;
    }

    public long getHandle() {
        return aDevice.getHandle();
    }

    public AbstractGraphicsDevice getGraphicsDevice() {
        return aDevice;
    }

    protected String name;
    protected AbstractGraphicsDevice aDevice;
}

