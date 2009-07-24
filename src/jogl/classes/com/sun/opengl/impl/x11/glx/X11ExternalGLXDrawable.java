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
import javax.media.nativewindow.x11.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.nativewindow.impl.NullWindow;
import com.sun.nativewindow.impl.x11.*;

import com.sun.gluegen.runtime.PointerBuffer;

public class X11ExternalGLXDrawable extends X11GLXDrawable {
  private int fbConfigID;
  private int renderType;

  private X11ExternalGLXDrawable(GLDrawableFactory factory, NativeWindow component, int renderType) {
    super(factory, component, true);

    this.renderType = renderType;

    // Need GLXFBConfig ID in order to properly create new contexts
    // on this drawable
    X11GLXGraphicsConfiguration cfg = (X11GLXGraphicsConfiguration) component.getGraphicsConfiguration();
    fbConfigID = cfg.getFBConfigID();
  }

  protected static X11ExternalGLXDrawable create(GLDrawableFactory factory, GLProfile glp) {
    ((GLDrawableFactoryImpl)factory).lockToolkit();
    try {
        long context = GLX.glXGetCurrentContext();
        if (context == 0) {
          throw new GLException("Error: current context null");
        }
        long display = GLX.glXGetCurrentDisplay();
        if (display == 0) {
          throw new GLException("Error: current display null");
        }
        long drawable = GLX.glXGetCurrentDrawable();
        if (drawable == 0) {
          throw new GLException("Error: attempted to make an external GLDrawable without a drawable current");
        }
        int[] val = new int[1];
        GLX.glXQueryContext(display, context, GLX.GLX_SCREEN, val, 0);
        X11GraphicsScreen x11Screen = (X11GraphicsScreen) X11GraphicsScreen.createScreenDevice(display, val[0]);

        GLX.glXQueryContext(display, context, GLX.GLX_FBCONFIG_ID, val, 0);
        X11GLXGraphicsConfiguration cfg = X11GLXGraphicsConfiguration.create(glp, x11Screen, val[0]);

        int w, h;
        GLX.glXQueryDrawable(display, drawable, GLX.GLX_WIDTH, val, 0);
        w=val[0];
        GLX.glXQueryDrawable(display, drawable, GLX.GLX_HEIGHT, val, 0);
        h=val[0];

        GLX.glXQueryContext(display, context, GLX.GLX_RENDER_TYPE, val, 0);
        if ((val[0] & GLX.GLX_RGBA_TYPE) == 0) {
          if (DEBUG) {
            System.err.println("X11ExternalGLXDrawable: WARNING: forcing GLX_RGBA_TYPE for newly created contexts (current 0x"+Integer.toHexString(val[0])+")");
          }
        }
        NullWindow nw = new NullWindow(cfg);
        nw.setSurfaceHandle(drawable);
        nw.setSize(w, h);
        return new X11ExternalGLXDrawable(factory, nw, GLX.GLX_RGBA_TYPE);
    } finally {
        ((GLDrawableFactoryImpl)factory).unlockToolkit();
    }
  }

  public GLContext createContext(GLContext shareWith) {
    return new Context(this, shareWith);
  }

  public void setSize(int newWidth, int newHeight) {
    throw new GLException("Should not call this");
  }

  public int getWidth() {
    return getNativeWindow().getWidth();
  }  

  public int getHeight() {
    return getNativeWindow().getHeight();
  }  

  class Context extends X11GLXContext {
    Context(X11GLXDrawable drawable, GLContext shareWith) {
      super(drawable, shareWith);
    }

    protected void create() {
      createContext(true);
    }
  }
}
