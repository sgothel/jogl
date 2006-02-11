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

public class X11ExternalGLDrawable extends X11GLDrawable {
  private int fbConfigID;
  private int renderType;
  private int screen;
  private long readDrawable;

  public X11ExternalGLDrawable() {
    super(null, null);
    lockToolkit();
    try {
      display = GLX.glXGetCurrentDisplay();
      drawable = GLX.glXGetCurrentDrawable();
      readDrawable = GLX.glXGetCurrentReadDrawable();
      if (drawable == 0) {
        throw new GLException("Error: attempted to make an external GLDrawable without a drawable/context current");
      }

      // Need GLXFBConfig ID in order to properly create new contexts
      // on this drawable
      long context = GLX.glXGetCurrentContext();
      int[] val = new int[1];
      GLX.glXQueryContext(display, context, GLX.GLX_FBCONFIG_ID, val, 0);
      fbConfigID = val[0];
      renderType = GLX.GLX_RGBA_TYPE;
      GLX.glXQueryContext(display, context, GLX.GLX_RENDER_TYPE, val, 0);
      if ((val[0] & GLX.GLX_RGBA_BIT) == 0) {
        if (DEBUG) {
          System.err.println("X11ExternalGLDrawable: WARNING: forcing GLX_RGBA_TYPE for newly created contexts");
        }
      }
      GLX.glXQueryContext(display, context, GLX.GLX_SCREEN, val, 0);
      screen = val[0];
    } finally {
      unlockToolkit();
    }
  }

  public GLContext createContext(GLContext shareWith) {
    return new Context(shareWith);
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

  public void destroy() {
  }

  class Context extends X11GLContext {
    Context(GLContext shareWith) {
      super(X11ExternalGLDrawable.this, shareWith);
      this.drawable = drawable;
    }

    protected int makeCurrentImpl() throws GLException {
      if (drawable.getDrawable() == 0) {
        // parent drawable not properly initialized
        // FIXME: signal error?
        if (DEBUG) {
          System.err.println("parent drawable not properly initialized");
        }
        return CONTEXT_NOT_CURRENT;
      }

      // Note that we have to completely override makeCurrentImpl
      // because the underlying makeCurrent call differs from the norm
      lockToolkit();
      try {
        boolean created = false;
        if (context == 0) {
          create();
          if (DEBUG) {
            System.err.println(getThreadName() + ": !!! Created GL context for " + getClass().getName());
          }
          created = true;
        }

        if (!GLX.glXMakeContextCurrent(drawable.getDisplay(),
                                       drawable.getDrawable(),
                                       readDrawable,
                                       context)) {
          throw new GLException("Error making context current");
        } else {
          mostRecentDisplay = drawable.getDisplay();
          if (DEBUG && VERBOSE) {
            System.err.println(getThreadName() + ": glXMakeCurrent(display " + toHexString(drawable.getDisplay()) +
                               ", drawable " + toHexString(drawable.getDrawable()) +
                               ", context " + toHexString(context) + ") succeeded");
          }
        }

        if (created) {
          resetGLFunctionAvailability();
          return CONTEXT_CURRENT_NEW;
        }
        return CONTEXT_CURRENT;
      } finally {
        unlockToolkit();
      }
    }

    protected void releaseImpl() throws GLException {
      lockToolkit();
      try {
        if (!GLX.glXMakeContextCurrent(drawable.getDisplay(), 0, 0, 0)) {
          throw new GLException("Error freeing OpenGL context");
        }
      } finally {
        unlockToolkit();
      }
    }

    protected void create() {
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
      GLXFBConfig[] fbConfigs = GLX.glXChooseFBConfig(display, screen, iattributes, 0, nelementsTmp, 0);
      int nelements = nelementsTmp[0];
      if (nelements <= 0) {
        throw new GLException("context creation error: couldn't find a suitable frame buffer configuration");
      }
      if (nelements != 1) {
        throw new GLException("context creation error: shouldn't get more than one GLXFBConfig");
      }
      // Note that we currently don't allow selection of anything but
      // the first GLXFBConfig in the returned list (there should be only one)
      GLXFBConfig fbConfig = fbConfigs[0];
      // Create a gl context for the drawable
      X11GLContext other = (X11GLContext) GLContextShareSet.getShareContext(this);
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
                           " for GLXDrawable " + toHexString(drawable.getDrawable()));
      }
    }
  }
}
