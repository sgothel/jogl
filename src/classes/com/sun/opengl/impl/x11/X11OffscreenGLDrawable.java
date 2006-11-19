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

package com.sun.opengl.impl.x11;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public class X11OffscreenGLDrawable extends X11GLDrawable {
  private long pixmap;
  private boolean isDoubleBuffered;
  // Width and height of the underlying bitmap
  private int width;
  private int height;

  public X11OffscreenGLDrawable(GLCapabilities capabilities,
                                GLCapabilitiesChooser chooser) {
    super(capabilities, chooser);
  }

  public GLContext createContext(GLContext shareWith) {
    return new X11OffscreenGLContext(this, shareWith);
  }
  
  public void setSize(int newWidth, int newHeight) {
    width = newWidth;
    height = newHeight;
    if (pixmap != 0) {
      destroy();
    }
    create();
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }
  
  private void create() {
    display = X11GLDrawableFactory.getDisplayConnection();
    XVisualInfo vis = chooseVisual(false);
    int bitsPerPixel = vis.depth();

    lockToolkit();
    try {
      int screen = GLX.DefaultScreen(display);
      pixmap = GLX.XCreatePixmap(display, (int) GLX.RootWindow(display, screen), width, height, bitsPerPixel);
      if (pixmap == 0) {
        throw new GLException("XCreatePixmap failed");
      }
      drawable = GLX.glXCreateGLXPixmap(display, vis, pixmap);
      if (drawable == 0) {
        GLX.XFreePixmap(display, pixmap);
        pixmap = 0;
        throw new GLException("glXCreateGLXPixmap failed");
      }
      isDoubleBuffered = (X11GLDrawableFactory.glXGetConfig(display, vis, GLX.GLX_DOUBLEBUFFER, new int[1], 0) != 0);
      if (DEBUG) {
        System.err.println("Created pixmap " + toHexString(pixmap) +
                           ", GLXPixmap " + toHexString(drawable) +
                           ", display " + toHexString(display));
      }
      setChosenGLCapabilities(X11GLDrawableFactory.xvi2GLCapabilities(display, vis));
    } finally {
      unlockToolkit();
    }
  }

  public void destroy() {
    if (pixmap != 0) {
      if (DEBUG) {
        System.err.println("Destroying pixmap " + toHexString(pixmap) +
                           ", GLXPixmap " + toHexString(drawable) +
                           ", display " + toHexString(display));
      }

      // Must destroy pixmap and GLXPixmap
      lockToolkit();

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
      GLX.XFreePixmap(display, pixmap);
      unlockToolkit();
      drawable = 0;
      pixmap = 0;
      display = 0;
      setChosenGLCapabilities(null);
    }
  }

  public boolean isDoubleBuffered() {
    return isDoubleBuffered;
  }
}
