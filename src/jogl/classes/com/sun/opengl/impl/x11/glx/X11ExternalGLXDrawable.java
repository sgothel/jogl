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

import java.nio.LongBuffer;

public class X11ExternalGLXDrawable extends X11GLXDrawable {
  private int fbConfigID;
  private int renderType;
  private long readDrawable;

  private X11ExternalGLXDrawable(GLDrawableFactory factory, NativeWindow component) {
    super(factory, component, true);

    readDrawable = GLX.glXGetCurrentReadDrawable();

    // Need GLXFBConfig ID in order to properly create new contexts
    // on this drawable
    long display = getNativeWindow().getDisplayHandle();
    long context = GLX.glXGetCurrentContext();
    int[] val = new int[1];
    GLX.glXQueryContext(display, context, GLX.GLX_FBCONFIG_ID, val, 0);
    fbConfigID = val[0];
    renderType = GLX.GLX_RGBA_TYPE;
    GLX.glXQueryContext(display, context, GLX.GLX_RENDER_TYPE, val, 0);
    if ((val[0] & GLX.GLX_RGBA_BIT) == 0) {
        if (DEBUG) {
          System.err.println("X11ExternalGLXDrawable: WARNING: forcing GLX_RGBA_TYPE for newly created contexts");
        }
    }
  }

  protected static X11ExternalGLXDrawable create(GLDrawableFactory factory, AbstractGraphicsScreen aScreen) {
    ((GLDrawableFactoryImpl) factory).lockToolkit();
    try {
      long display = GLX.glXGetCurrentDisplay();
      long context = GLX.glXGetCurrentContext();
      int[] val = new int[1];
      GLX.glXQueryContext(display, context, GLX.GLX_SCREEN, val, 0);
      int screen = val[0];
      long drawable = GLX.glXGetCurrentDrawable();
      if (drawable == 0) {
        throw new GLException("Error: attempted to make an external GLDrawable without a drawable/context current");
      }

      if(screen!=aScreen.getIndex()) {
        throw new GLException("Error: Passed AbstractGraphicsScreen's index is not current: "+aScreen+", GLX-screen "+screen);
      }
      if(display!=aScreen.getDevice().getHandle()) {
        throw new GLException("Error: Passed AbstractGraphicsScreen's display is not current: "+aScreen+", GLX-display 0x"+Long.toHexString(display));
      }

      NullWindow nw = new NullWindow(X11GLXGraphicsConfigurationFactory.createDefaultGraphicsConfiguration(aScreen, false));
      nw.setSurfaceHandle(drawable);
      return new X11ExternalGLXDrawable(factory, nw);
    } finally {
      ((GLDrawableFactoryImpl) factory).unlockToolkit();
    }
  }

  public GLContext createContext(GLContext shareWith) {
    return new Context(this, shareWith);
  }

  public void setSize(int newWidth, int newHeight) {
    throw new GLException("Should not call this");
  }

  public int getWidth() {
    throw new GLException("Should not call this");
  }  

  public int getHeight() {
    throw new GLException("Should not call this");
  }  

  class Context extends X11GLXContext {
    Context(X11GLXDrawable drawable, GLContext shareWith) {
      super(drawable, shareWith);
    }

    protected int makeCurrentImpl() throws GLException {
      if (drawable.getNativeWindow().getSurfaceHandle() == 0) {
        // parent drawable not properly initialized
        // FIXME: signal error?
        if (DEBUG) {
          System.err.println("parent drawable not properly initialized");
        }
        return CONTEXT_NOT_CURRENT;
      }

      // Note that we have to completely override makeCurrentImpl
      // because the underlying makeCurrent call differs from the norm
      getFactoryImpl().lockToolkit();
      try {
        boolean created = false;
        if (context == 0) {
          create();
          if (DEBUG) {
            System.err.println(getThreadName() + ": !!! Created GL context for " + getClass().getName());
          }
          created = true;
        }

        if (!GLX.glXMakeContextCurrent(drawable.getNativeWindow().getDisplayHandle(),
                                       drawable.getNativeWindow().getSurfaceHandle(),
                                       readDrawable,
                                       context)) {
          throw new GLException("Error making context current");
        } else {
          if (DEBUG && VERBOSE) {
            System.err.println(getThreadName() + ": glXMakeCurrent(display " + toHexString(drawable.getNativeWindow().getDisplayHandle()) +
                               ", drawable " + toHexString(drawable.getNativeWindow().getSurfaceHandle()) +
                               ", context " + toHexString(context) + ") succeeded");
          }
        }

        if (created) {
          setGLFunctionAvailability(false);
          return CONTEXT_CURRENT_NEW;
        }
        return CONTEXT_CURRENT;
      } finally {
        getFactoryImpl().unlockToolkit();
      }
    }

    protected void releaseImpl() throws GLException {
      getFactoryImpl().lockToolkit();
      try {
        if (!GLX.glXMakeContextCurrent(drawable.getNativeWindow().getDisplayHandle(), 0, 0, 0)) {
          throw new GLException("Error freeing OpenGL context");
        }
      } finally {
        getFactoryImpl().unlockToolkit();
      }
    }

    protected void create() {
      long display = getNativeWindow().getDisplayHandle();
      int screen = getNativeWindow().getScreenIndex();
      // We already have the GLXFBConfig ID for the context. All we
      // need to do is use it to choose the GLXFBConfig and then
      // create a context with it.
      int[]   iattributes = new int[] {
        GLX.GLX_FBCONFIG_ID,
        fbConfigID,
        0,
        0
      };
      float[] fattributes = new float[0];
      int[] nelementsTmp = new int[1];
      LongBuffer fbConfigs = GLX.glXChooseFBConfigCopied(display, screen, iattributes, 0, nelementsTmp, 0);
      int nelements = nelementsTmp[0];
      if (nelements <= 0) {
        throw new GLException("context creation error: couldn't find a suitable frame buffer configuration");
      }
      if (nelements != 1) {
        throw new GLException("context creation error: shouldn't get more than one GLXFBConfig");
      }
      // Note that we currently don't allow selection of anything but
      // the first GLXFBConfig in the returned list (there should be only one)
      long fbConfig = fbConfigs.get(0);
      // Create a gl context for the drawable
      X11GLXContext other = (X11GLXContext) GLContextShareSet.getShareContext(this);
      long share = 0;
      if (other != null) {
        share = other.getContext();
        if (share == 0) {
          throw new GLException("GLContextShareSet returned an invalid OpenGL context");
        }
      }
      // FIXME: how to determine "direct" bit?
      context = GLX.glXCreateNewContext(display, fbConfig, renderType, share, true);
      if (context == 0) {
        String detail = "  display=" + toHexString(display) +
          " fbconfig=" + fbConfig +
          " fbconfigID=" + toHexString(fbConfigID) +
          " renderType=" + toHexString(renderType) +
          " share=" + toHexString(share);
        throw new GLException("context creation error: glXCreateNewContext() failed: " + detail);
      }
      GLContextShareSet.contextCreated(this);

      if (DEBUG) {
        System.err.println("Created context " + toHexString(context) +
                           " for GLXDrawable " + toHexString(drawable.getNativeWindow().getSurfaceHandle()));
      }
    }
  }
}
