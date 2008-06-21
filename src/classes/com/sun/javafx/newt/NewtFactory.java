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

    /** Creates a Window of the default type for the current operating system. */
    public static String getWindowType() {
      String osName = System.getProperty("os.name");
      String osNameLowerCase = osName.toLowerCase();
      String windowType;
      if (osNameLowerCase.startsWith("wind")) {
          windowType = WINDOWS;
      } else if (osNameLowerCase.startsWith("mac os x")) {
          windowType = MACOSX;
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
     * Create a Screen entity, incl native creation
     */
    public static Screen createScreen(Display display, int index) {
      return Screen.create(getWindowType(), display, index);
    }

    /**
     * Create a Window entity, incl native creation
     */
    public static Window createWindow(Screen screen, long visualID) {
      return Window.create(getWindowType(), screen, visualID);
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
    public static Window wrapWindow(Screen screen, long visualID,
                                    long windowHandle, boolean fullscreen, boolean visible, 
                                    int x, int y, int width, int height) {
      return Window.wrapHandle(getWindowType(), screen, visualID, 
                               windowHandle, fullscreen, visible, x, y, width, height);
    }
}

