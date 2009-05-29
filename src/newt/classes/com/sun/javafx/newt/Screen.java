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

public abstract class Screen {

    private static Class getScreenClass(String type) 
        throws ClassNotFoundException 
    {
        Class screenClass = null;
        if (NativeWindowFactory.TYPE_EGL.equals(type)) {
            screenClass = Class.forName("com.sun.javafx.newt.opengl.kd.KDScreen");
        } else if (NativeWindowFactory.TYPE_WINDOWS.equals(type)) {
            screenClass = Class.forName("com.sun.javafx.newt.windows.WindowsScreen");
        } else if (NativeWindowFactory.TYPE_MACOSX.equals(type)) {
            screenClass = Class.forName("com.sun.javafx.newt.macosx.MacScreen");
        } else if (NativeWindowFactory.TYPE_X11.equals(type)) {
            screenClass = Class.forName("com.sun.javafx.newt.x11.X11Screen");
        } else if (NativeWindowFactory.TYPE_AWT.equals(type)) {
            screenClass = Class.forName("com.sun.javafx.newt.awt.AWTScreen");
        } else {
            throw new RuntimeException("Unknown window type \"" + type + "\"");
        }
        return screenClass;
    }

    protected static Screen create(String type, Display display, int idx) {
        try {
            if(usrWidth<0 || usrHeight<0) {
                usrWidth  = NewtFactory.getPropertyIntValue("newt.ws.swidth");
                usrHeight = NewtFactory.getPropertyIntValue("newt.ws.sheight");
                System.out.println("User screen size "+usrWidth+"x"+usrHeight);
            }
            Class screenClass = getScreenClass(type);
            Screen screen  = (Screen) screenClass.newInstance();
            screen.display = display;
            screen.createNative(idx);
            if(null==screen.aScreen) {
                throw new RuntimeException("Screen.createNative() failed to instanciate an AbstractGraphicsScreen");
            }
            return screen;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void destroy() {
        closeNative();
    }

    protected static Screen wrapHandle(String type, Display display, AbstractGraphicsScreen aScreen) {
        try {
            Class screenClass = getScreenClass(type);
            Screen screen  = (Screen) screenClass.newInstance();
            screen.display = display;
            screen.aScreen = aScreen;
            return screen;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void createNative(int index);
    protected abstract void closeNative();

    protected void setScreenSize(int w, int h) {
        System.out.println("Detected screen size "+w+"x"+h);
        width=w; height=h;
    }

    public Display getDisplay() {
        return display;
    }

    public int getIndex() {
        return aScreen.getIndex();
    }

    public AbstractGraphicsScreen getGraphicsScreen() {
        return aScreen;
    }

    /**
     * The actual implementation shall return the detected display value,
     * if not we return 800.
     * This can be overwritten with the user property 'newt.ws.swidth',
     */
    public int getWidth() {
        return (usrWidth>0) ? usrWidth : (width>0) ? width : 480;
    }

    /**
     * The actual implementation shall return the detected display value,
     * if not we return 480.
     * This can be overwritten with the user property 'newt.ws.sheight',
     */
    public int getHeight() {
        return (usrHeight>0) ? usrHeight : (height>0) ? height : 480;
    }

    protected Display display;
    protected AbstractGraphicsScreen aScreen;
    protected int width=-1, height=-1; // detected values: set using setScreenSize
    protected static int usrWidth=-1, usrHeight=-1; // property values: newt.ws.swidth and newt.ws.sheight
}

