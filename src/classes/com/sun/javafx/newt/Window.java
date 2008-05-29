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

public abstract class Window {
    /** OpenKODE window type */
    public static final String KD = "KD";

    /** Microsoft Windows window type */
    public static final String WINDOWS = "Windows";

    /** X11 window type */
    public static final String X11 = "X11";

    /** Mac OS X window type */
    public static final String MACOSX = "MacOSX";

    /** Creates a Window of the default type for the current operating system. */
    public static Window create() {
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
      return create(windowType);
    }


    public static Window create(String type) {
        try {
            Class windowClass = null;
            if (KD.equals(type)) {
                windowClass = Class.forName("com.sun.javafx.newt.kd.KDWindow");
            } else if (WINDOWS.equals(type)) {
                windowClass = Class.forName("com.sun.javafx.newt.windows.WindowsWindow");
            } else if (X11.equals(type)) {
                windowClass = Class.forName("com.sun.javafx.newt.x11.X11Window");
            } else if (MACOSX.equals(type)) {
                windowClass = Class.forName("com.sun.javafx.newt.macosx.MacOSXWindow");
            } else {
                throw new RuntimeException("Unknown window type \"" + type + "\"");
            }
            return (Window) windowClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Window() {
    }

    public abstract void    setSize(int width, int height);
    public abstract int     getWidth();
    public abstract int     getHeight();
    public abstract boolean setFullscreen(boolean fullscreen);
    public abstract long    getWindowHandle();
    public abstract void    pumpMessages();
}
