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

package com.sun.opengl.impl.x11.glx;

import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.nativewindow.impl.NullWindow;
import com.sun.nativewindow.impl.x11.*;

public class X11OffscreenGLXDrawable extends X11GLXDrawable {
  private long pixmap;
  private boolean isDoubleBuffered;

  protected X11OffscreenGLXDrawable(GLDrawableFactory factory, AbstractGraphicsScreen screen,
                                    GLCapabilities caps,
                                    GLCapabilitiesChooser chooser,
                                    int width,
                                    int height) {
    super(factory, new NullWindow(X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(caps, chooser, screen, false)), true);
    ((NullWindow) getNativeWindow()).setSize(width, height);
    create();
  }

  public GLContext createContext(GLContext shareWith) {
    return new X11OffscreenGLXContext(this, shareWith);
  }
  
  private void create() {
    NullWindow nw = (NullWindow) getNativeWindow();
    X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration) nw.getGraphicsConfiguration().getNativeGraphicsConfiguration();
    XVisualInfo vis = config.getXVisualInfo();
    int bitsPerPixel = vis.depth();
    AbstractGraphicsScreen aScreen = config.getScreen();
    AbstractGraphicsDevice aDevice = aScreen.getDevice();
    long dpy = aDevice.getHandle();
    int screen = aScreen.getIndex();

    getFactoryImpl().lockToolkit();
    try {
      pixmap = X11Lib.XCreatePixmap(dpy, (int) X11Lib.RootWindow(dpy, screen), 
                                    component.getWidth(), component.getHeight(), bitsPerPixel);
      if (pixmap == 0) {
        throw new GLException("XCreatePixmap failed");
      }
      long drawable = GLX.glXCreateGLXPixmap(dpy, vis, pixmap);
      if (drawable == 0) {
        X11Lib.XFreePixmap(dpy, pixmap);
        pixmap = 0;
        throw new GLException("glXCreateGLXPixmap failed");
      }
      nw.setSurfaceHandle(drawable);
      isDoubleBuffered = (X11GLXGraphicsConfiguration.glXGetConfig(dpy, vis, GLX.GLX_DOUBLEBUFFER, new int[1], 0) != 0);
      if (DEBUG) {
        System.err.println("Created pixmap " + toHexString(pixmap) +
                           ", GLXPixmap " + toHexString(drawable) +
                           ", display " + toHexString(dpy));
      }
    } finally {
      getFactoryImpl().unlockToolkit();
    }
  }

  public void destroy() {
    if (pixmap == 0) return;
    try {
      NativeWindow nw = getNativeWindow();
      long display = nw.getDisplayHandle();
      long drawable = nw.getSurfaceHandle();
      if (DEBUG) {
        System.err.println("Destroying pixmap " + toHexString(pixmap) +
                           ", GLXPixmap " + toHexString(drawable) +
                           ", display " + toHexString(display));
      }

      // Must destroy pixmap and GLXPixmap
      getFactoryImpl().lockToolkit();

      if (DEBUG) {
        long cur = GLX.glXGetCurrentContext();
        if (cur != 0) {
          System.err.println("WARNING: found context " + toHexString(cur) + " current during pixmap destruction");
        }
      }

      // FIXME: workaround for crashes on NVidia hardware when
      // destroying pixmap (no context is current at the point of the
      // crash, at least from the point of view of
      // glXGetCurrentContext)
      GLX.glXMakeCurrent(display, 0, 0);

      GLX.glXDestroyGLXPixmap(display, drawable);
      X11Lib.XFreePixmap(display, pixmap);
      drawable = 0;
      pixmap = 0;
      display = 0;
    } finally {
      getFactoryImpl().unlockToolkit();
    }
  }

  public boolean isDoubleBuffered() {
    return isDoubleBuffered;
  }
}
