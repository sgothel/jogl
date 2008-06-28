/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package javax.media.opengl;

import java.lang.reflect.*;
import java.security.*;
import com.sun.opengl.impl.*;

public class NativeWindowFactory {
  private static Constructor awtFactory = null;

  /** Initializes the sole NativeWindowFactory instance . */
  private static void initializeAWTFactory() throws GLException {
    if (awtFactory == null) {
        // Use the desktop OpenGL as the fallback always
        try {
          String osName = System.getProperty("os.name");
          String osNameLowerCase = osName.toLowerCase();
          String factoryClassName = null;

          // Because there are some complications with generating all
          // platforms' Java glue code on all platforms (among them that we
          // would have to include jawt.h and jawt_md.h in the jogl
          // sources, which we currently don't have to do) we break the only
          // static dependencies with platform-specific code here using reflection.

          if (osNameLowerCase.startsWith("wind")) {
            factoryClassName = "com.sun.opengl.impl.jawt.windows.WindowsJAWTWindow";
          } else if (osNameLowerCase.startsWith("mac os x")) {
            factoryClassName = "com.sun.opengl.impl.jawt.macosx.MacOSXJAWTWindow";
          } else {
            // Assume Linux, Solaris, etc. Should probably test for these explicitly.
            factoryClassName = "com.sun.opengl.impl.jawt.x11.X11JAWTWindow";
          }

          if (factoryClassName == null) {
            throw new GLException("OS " + osName + " not yet supported");
          }

          awtFactory = GLReflection.getConstructor(factoryClassName, new Class[] { Object.class });
        } catch (Exception e) {
          throw new GLException(e);
        }
    }
  }

  /**
   * Returns a NativeWindow.
   *
   * This method digest a window object 'winObj'.
   * This can be either itself a NativeWindow, 
   * or any other Java-level window toolkit window object.
   *
   * In case 'winObj' is a terminal NativeWindow, where
   * {@link NativeWindow#isTerminalObject()} returns true,
   * it is passed through directly.
   *
   * Otherwise either the non NativeWindow object,
   * or the wrapped window object within the proxy NativeWindow
   * will be used to factor a terminal NativeWindow.
   *
   * @throws IllegalArgumentException if the passed winObj is null
   * @throws NativeWindowException if the passed winObj's is a proxy NativeWindow
   *                               and does not hold a supported wrapped window object,
   *                               or it is not a supported window object.
   * @throws GLException if any window system-specific errors caused
   *         the creation of the GLDrawable to fail.
   */
  public static NativeWindow getNativeWindow(Object winObj) 
    throws IllegalArgumentException, GLException, NativeWindowException
  {
    if(null==winObj) {
        throw new IllegalArgumentException("winObj is null");
    }
    if(winObj instanceof NativeWindow) {
        NativeWindow nw = (NativeWindow) winObj;
        if(nw.isTerminalObject()) {
            return nw; // use the terminal NativeWindow object directly
        }
        Object  wrappedWindow = nw.getWrappedWindow();
        if(null==wrappedWindow) {
            throw new NativeWindowException("Proxy NativeWindow holds no wrapped window: "+nw);
        }
        winObj = wrappedWindow;
    }

    if (GLReflection.isAWTComponent(winObj)) {
      initializeAWTFactory();
      if(awtFactory == null) {
          throw new GLException("Could not determine an AWT-NativeWindow constructor");
      }
      try {
          return (NativeWindow) awtFactory.newInstance(new Object[] { winObj });
      } catch (Exception ie) {
          ie.printStackTrace();
      }
    }
    throw new NativeWindowException("Target type is unsupported. Currently supported: \n"+
                                    "\tjavax.media.opengl.NativeWindow\n"+
                                    "\tjava.awt.Component\n");
  }
}

