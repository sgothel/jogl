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

package com.jogamp.newt;

import com.jogamp.newt.impl.*;

import javax.media.nativewindow.*;
import java.security.*;

public abstract class Screen {

    public static final boolean DEBUG = Debug.debug("Display");

    private static Class getScreenClass(String type) 
        throws ClassNotFoundException 
    {
        Class screenClass = NewtFactory.getCustomClass(type, "Screen");
        if(null==screenClass) {
            if (NativeWindowFactory.TYPE_EGL.equals(type)) {
                screenClass = Class.forName("com.jogamp.newt.impl.opengl.kd.KDScreen");
            } else if (NativeWindowFactory.TYPE_WINDOWS.equals(type)) {
                screenClass = Class.forName("com.jogamp.newt.impl.windows.WindowsScreen");
            } else if (NativeWindowFactory.TYPE_MACOSX.equals(type)) {
                screenClass = Class.forName("com.jogamp.newt.impl.macosx.MacScreen");
            } else if (NativeWindowFactory.TYPE_X11.equals(type)) {
                screenClass = Class.forName("com.jogamp.newt.impl.x11.X11Screen");
            } else if (NativeWindowFactory.TYPE_AWT.equals(type)) {
                screenClass = Class.forName("com.jogamp.newt.impl.awt.AWTScreen");
            } else {
                throw new RuntimeException("Unknown window type \"" + type + "\"");
            }
        }
        return screenClass;
    }

    protected static Screen create(String type, Display display, final int idx) {
        try {
            if(usrWidth<0 || usrHeight<0) {
                usrWidth  = Debug.getIntProperty("newt.ws.swidth", true, localACC);
                usrHeight = Debug.getIntProperty("newt.ws.sheight", true, localACC);
                if(usrWidth>0 || usrHeight>0) {
                    System.out.println("User screen size "+usrWidth+"x"+usrHeight);
                }
            }
            Class screenClass = getScreenClass(type);
            Screen screen  = (Screen) screenClass.newInstance();
            screen.display = display;
            screen.idx = idx;
            return screen;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected  synchronized final void createNative() {
        if(null == aScreen) {
            if(DEBUG) {
                System.out.println("Screen.createNative() START ("+Display.getThreadName()+", "+this+")");
            }
            display.addReference();
            createNativeImpl();
            if(null == aScreen) {
                throw new RuntimeException("Screen.createNative() failed to instanciate an AbstractGraphicsScreen");
            }
            if(DEBUG) {
                System.out.println("Screen.createNative() END ("+Display.getThreadName()+", "+this+")");
            }
        }
    }

    public synchronized final void destroy() {
        if ( null != aScreen ) {
            closeNativeImpl();
            display.removeReference();
            aScreen = null;
        }
    }

    protected synchronized final int addReference() {
        if(DEBUG) {
            System.out.println("Screen.addReference() ("+Display.getThreadName()+"): "+refCount+" -> "+(refCount+1));
        }
        if ( 0 == refCount ) {
            createNative();
        }
        if(null == aScreen) {
            throw new RuntimeException("Screen.addReference() (refCount "+refCount+") null AbstractGraphicsScreen");
        }
        return ++refCount;
    }

    protected synchronized final int removeReference() {
        if(DEBUG) {
            System.out.println("Screen.removeReference() ("+Display.getThreadName()+"): "+refCount+" -> "+(refCount-1));
        }
        refCount--;
        if(0==refCount && getDestroyWhenUnused()) {
            destroy();
        }
        return refCount;
    }

    /** 
     * @return number of references by Window
     */
    public synchronized final int getReferenceCount() {
        return refCount;
    }

    public final boolean getDestroyWhenUnused() { 
        return display.getDestroyWhenUnused(); 
    }
    public final void setDestroyWhenUnused(boolean v) { 
        display.setDestroyWhenUnused(v); 
    }

    protected abstract void createNativeImpl();
    protected abstract void closeNativeImpl();

    protected void setScreenSize(int w, int h) {
        System.out.println("Detected screen size "+w+"x"+h);
        width=w; height=h;
    }

    public final Display getDisplay() {
        return display;
    }

    public final int getIndex() {
        return idx;
    }

    public final AbstractGraphicsScreen getGraphicsScreen() {
        return aScreen;
    }

    public final boolean isNativeValid() {
        return null != aScreen;
    }

    /**
     * The actual implementation shall return the detected display value,
     * if not we return 800.
     * This can be overwritten with the user property 'newt.ws.swidth',
     */
    public final int getWidth() {
        return (usrWidth>0) ? usrWidth : (width>0) ? width : 480;
    }

    /**
     * The actual implementation shall return the detected display value,
     * if not we return 480.
     * This can be overwritten with the user property 'newt.ws.sheight',
     */
    public final int getHeight() {
        return (usrHeight>0) ? usrHeight : (height>0) ? height : 480;
    }

    protected Display display;
    protected int idx;
    protected AbstractGraphicsScreen aScreen;
    protected int refCount; // number of Screen references by Window
    protected int width=-1, height=-1; // detected values: set using setScreenSize
    protected static int usrWidth=-1, usrHeight=-1; // property values: newt.ws.swidth and newt.ws.sheight
    private static AccessControlContext localACC = AccessController.getContext();
}

