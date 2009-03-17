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

import javax.media.nativewindow.NWCapabilities;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class NewtFactory {
    /** OpenKODE window type */
    public static final String KD = "KD";

    /** Microsoft Windows window type */
    public static final String WINDOWS = "Windows";

    /** X11 window type */
    public static final String X11 = "X11";

    /** Mac OS X window type */
    public static final String MACOSX = "MacOSX";

    /** Generic AWT wrapped window type, if available */
    public static final String AWT = "AWT";

    // Work-around for initialization order problems on Mac OS X
    // between native Newt and (apparently) Fmod
    static {
        Window.init(getWindowType());
    }

    static int getPropertyIntValue(String propname) {
        int i=0;
        String s = System.getProperty(propname);
        if(null!=s) {
            try {
                i = Integer.valueOf(s).intValue();
            } catch (NumberFormatException nfe) {}
        }
        return i;
    }

    /** Creates a Window of the default type for the current operating system. */
    public static String getWindowType() {
      String osName = System.getProperty("newt.ws.name");
      if(null==osName||osName.length()==0) {
          osName = System.getProperty("os.name");
      }
      String osNameLowerCase = osName.toLowerCase();
      String windowType;
      if (osNameLowerCase.startsWith("kd")) {
          windowType = KD;
      } else if (osNameLowerCase.startsWith("wind")) {
          windowType = WINDOWS;
      } else if (osNameLowerCase.startsWith("mac os x") ||
                 osNameLowerCase.startsWith("darwin")) {
          windowType = MACOSX;
      } else if (osNameLowerCase.equals("awt")) {
          windowType = AWT;
      } else {
          windowType = X11;
      }
      return windowType;
    }

    /**
     * Create a Display entity, incl native creation
     */
    public static Display createDisplay(String name) {
      return Display.create(getWindowType(), name);
    }

    /**
     * Create a Display entity using the given implementation type, incl native creation
     */
    public static Display createDisplay(String type, String name) {
      return Display.create(type, name);
    }

    /**
     * Create a Screen entity, incl native creation
     */
    public static Screen createScreen(Display display, int index) {
      return Screen.create(getWindowType(), display, index);
    }

    /**
     * Create a Screen entity using the given implementation type, incl native creation
     */
    public static Screen createScreen(String type, Display display, int index) {
      return Screen.create(type, display, index);
    }

    /**
     * Create a Window entity, incl native creation
     */
    public static Window createWindow(Screen screen, NWCapabilities caps) {
      return Window.create(getWindowType(), screen, caps);
    }

    public static Window createWindow(Screen screen, NWCapabilities caps, boolean undecorated) {
        return Window.create(getWindowType(), screen, caps, undecorated);
    }

    /**
     * Create a Window entity using the given implementation type, incl native creation
     */
    public static Window createWindow(String type, Screen screen, NWCapabilities caps) {
      return Window.create(type, screen, caps);
    }

    /**
     * Instantiate a Display entity using the native handle.
     */
    public static Display wrapDisplay(String name, long handle) {
      return Display.wrapHandle(getWindowType(), name, handle);
    }

    /**
     * Instantiate a Screen entity using the native handle.
     */
    public static Screen wrapScreen(Display display, int index, long handle) {
      return Screen.wrapHandle(getWindowType(), display, index, handle);
    }

    /**
     * Instantiate a Window entity using the native handle.
     */
    public static Window wrapWindow(Screen screen, NWCapabilities caps, long visualID,
                                    long windowHandle, boolean fullscreen, boolean visible, 
                                    int x, int y, int width, int height) {
      return Window.wrapHandle(getWindowType(), screen, caps, visualID, 
                               windowHandle, fullscreen, visible, x, y, width, height);
    }

    private static final boolean instanceOf(Object obj, String clazzName) {
        Class clazz = obj.getClass();
        do {
            if(clazz.getName().equals(clazzName)) {
                return true;
            }
            clazz = clazz.getSuperclass();
        } while (clazz!=null);
        return false;
    }

}

