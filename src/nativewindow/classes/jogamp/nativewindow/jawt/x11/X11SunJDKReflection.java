/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package jogamp.nativewindow.jawt.x11;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;

import com.jogamp.nativewindow.awt.AWTGraphicsConfiguration;


/** This class encapsulates the reflection routines necessary to peek
    inside a few data structures in the AWT implementation on X11 for
    the purposes of correctly enumerating the available visuals. */

public class X11SunJDKReflection {
  private static Class<?> x11GraphicsDeviceClass;
  private static Method   x11GraphicsDeviceGetDisplayMethod;
  private static Class<?> x11GraphicsConfigClass;
  private static Method   x11GraphicsConfigGetVisualMethod;
  private static boolean initialized;

  static {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
        @Override
        public Object run() {
          try {
            x11GraphicsDeviceClass = Class.forName("sun.awt.X11GraphicsDevice");
            x11GraphicsDeviceGetDisplayMethod = x11GraphicsDeviceClass.getDeclaredMethod("getDisplay", new Class[] {});
            x11GraphicsDeviceGetDisplayMethod.setAccessible(true);

            x11GraphicsConfigClass = Class.forName("sun.awt.X11GraphicsConfig");
            x11GraphicsConfigGetVisualMethod = x11GraphicsConfigClass.getDeclaredMethod("getVisual", new Class[] {});
            x11GraphicsConfigGetVisualMethod.setAccessible(true);
            initialized = true;
          } catch (final Exception e) {
            // Either not a Sun JDK or the interfaces have changed since 1.4.2 / 1.5
          }
          return null;
        }
      });
  }

  public static long graphicsDeviceGetDisplay(final GraphicsDevice device) {
    if (!initialized) {
      return 0;
    }

    try {
      return ((Long) x11GraphicsDeviceGetDisplayMethod.invoke(device, (Object[])null)).longValue();
    } catch (final Exception e) {
      return 0;
    }
  }

  public static int graphicsConfigurationGetVisualID(final AbstractGraphicsConfiguration config) {
      try {
          if (config instanceof AWTGraphicsConfiguration) {
              return graphicsConfigurationGetVisualID(((AWTGraphicsConfiguration) config).getAWTGraphicsConfiguration());
          }
          return 0;
      } catch (final Exception e) {
          return 0;
      }
  }

  public static int graphicsConfigurationGetVisualID(final GraphicsConfiguration config) {
    if (!initialized) {
      return 0;
    }

    try {
      return ((Integer) x11GraphicsConfigGetVisualMethod.invoke(config, (Object[])null)).intValue();
    } catch (final Exception e) {
      return 0;
    }
  }
}
