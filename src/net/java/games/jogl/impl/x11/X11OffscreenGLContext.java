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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

package net.java.games.jogl.impl.x11;

import java.awt.image.BufferedImage;
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class X11OffscreenGLContext extends X11GLContext {
  private int pixmap;
  private boolean isDoubleBuffered;
  // Width and height of the underlying bitmap
  private int width;
  private int height;

  public X11OffscreenGLContext(GLCapabilities capabilities,
                               GLCapabilitiesChooser chooser,
                               GLContext shareWith) {
    super(null, capabilities, chooser, shareWith);
  }

  protected GL createGL()
  {
    return new X11GLImpl(this);
  }

  protected boolean isOffscreen() {
    return true;
  }

  public int getOffscreenContextBufferedImageType() {
    if (capabilities.getAlphaBits() > 0) {
      return BufferedImage.TYPE_INT_ARGB;
    } else {
      return BufferedImage.TYPE_INT_RGB;
    }
  }

  public int getOffscreenContextWidth() {
      return width;
  }

  public int getOffscreenContextHeight() {
      return height;
  }

  public int getOffscreenContextPixelDataType() {
      return GL.GL_UNSIGNED_BYTE;
  }
  
  public int getOffscreenContextReadBuffer() {
    if (isDoubleBuffered) {
      return GL.GL_BACK;
    }
    return GL.GL_FRONT;
  }

  public boolean offscreenImageNeedsVerticalFlip() {
    // There doesn't seem to be a way to do this in the construction
    // of the Pixmap or GLXPixmap
    return true;
  }

  public boolean canCreatePbufferContext() {
    // For now say no
    return false;
  }

  public synchronized GLContext createPbufferContext(GLCapabilities capabilities,
                                                     int initialWidth,
                                                     int initialHeight) {
    throw new GLException("Not supported");
  }

  public void bindPbufferToTexture() {
    throw new GLException("Should not call this");
  }

  public void releasePbufferFromTexture() {
    throw new GLException("Should not call this");
  }

  protected synchronized boolean makeCurrent(Runnable initAction) throws GLException {
    display = X11GLContextFactory.getDisplayConnection();
    if (pendingOffscreenResize) {
      if (pendingOffscreenWidth != width || pendingOffscreenHeight != height) {
        if (context != 0) {
          destroy();
        }
        width  = pendingOffscreenWidth;
        height = pendingOffscreenHeight;
        pendingOffscreenResize = false;
      }
    }
    mostRecentDisplay = display;
    return super.makeCurrent(initAction);
  }

  public synchronized void swapBuffers() throws GLException {
  }

  protected synchronized void free() throws GLException {
    try {
      super.free();
    } finally {
      display = 0;
    }
  }

  protected void create() {
    XVisualInfo vis = chooseVisual();
    int bitsPerPixel = vis.depth();

    if (display == 0) {
      throw new GLException("No active display");
    }
    int screen = GLX.DefaultScreen(display);
    pixmap = GLX.XCreatePixmap(display, (int) GLX.RootWindow(display, screen), width, height, bitsPerPixel);
    if (pixmap == 0) {
      throw new GLException("XCreatePixmap failed");
    }
    drawable = GLX.glXCreateGLXPixmap(display, vis, pixmap);
    if (drawable == 0) {
      throw new GLException("glXCreateGLXPixmap failed");
    }
    context = createContext(vis, false);
    if (context == 0) {
      throw new GLException("Unable to create OpenGL context");
    }
    isDoubleBuffered = (X11GLContextFactory.glXGetConfig(display, vis, GLX.GLX_DOUBLEBUFFER, new int[1]) != 0);
  }

  protected void destroyImpl() {
    if (context != 0) {
      super.destroyImpl();
      // Must destroy OpenGL context, pixmap and GLXPixmap
      GLX.glXDestroyContext(display, context);
      GLX.glXDestroyGLXPixmap(display, (int) drawable);
      GLX.XFreePixmap(display, pixmap);
      context = 0;
      drawable = 0;
      pixmap = 0;
      GLContextShareSet.contextDestroyed(this);
    }
  }
}
