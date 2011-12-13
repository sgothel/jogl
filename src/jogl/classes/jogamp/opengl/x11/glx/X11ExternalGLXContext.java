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

package jogamp.opengl.x11.glx;

import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import javax.media.opengl.*;
import jogamp.opengl.*;
import jogamp.nativewindow.WrappedSurface;

public class X11ExternalGLXContext extends X11GLXContext {
  private GLContext lastContext;

  private X11ExternalGLXContext(Drawable drawable, long ctx) {
    super(drawable, null);
    this.contextHandle = ctx;
    GLContextShareSet.contextCreated(this);
    setGLFunctionAvailability(false, 0, 0, CTX_PROFILE_COMPAT|CTX_OPTION_ANY);
    getGLStateTracker().setEnabled(false); // external context usage can't track state in Java
  }

  protected static X11ExternalGLXContext create(GLDrawableFactory factory, GLProfile glp) {
    long ctx = GLX.glXGetCurrentContext();
    if (ctx == 0) {
      throw new GLException("Error: current context null");
    }
    long display = GLX.glXGetCurrentDisplay();
    if (display == 0) {
      throw new GLException("Error: current display null");
    }
    long drawable = GLX.glXGetCurrentDrawable();
    if (drawable == 0) {
      throw new GLException("Error: attempted to make an external GLDrawable without a drawable/context current");
    }
    int[] val = new int[1];
    GLX.glXQueryContext(display, ctx, GLX.GLX_SCREEN, val, 0);
    X11GraphicsScreen x11Screen = (X11GraphicsScreen) X11GraphicsScreen.createScreenDevice(display, val[0], false);

    GLX.glXQueryContext(display, ctx, GLX.GLX_FBCONFIG_ID, val, 0);
    X11GLXGraphicsConfiguration cfg = null;
    // sometimes glXQueryContext on an external context gives us a framebuffer config ID
    // of 0, which doesn't work in a subsequent call to glXChooseFBConfig; if this happens,
    // create and use a default config (this has been observed when running on CentOS 5.5 inside
    // of VMWare Server 2.0 with the Mesa 6.5.1 drivers)
    if( X11GLXGraphicsConfiguration.GLXFBConfigIDValid(display, x11Screen.getIndex(), val[0]) ) {
        GLCapabilities glcapsDefault = new GLCapabilities(GLProfile.getDefault());
        cfg = X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(glcapsDefault, glcapsDefault, null, x11Screen);
        if(DEBUG) {
            System.err.println("X11ExternalGLXContext invalid FBCONFIG_ID "+val[0]+", using default cfg: " + cfg);
        }
    } else {
        cfg = X11GLXGraphicsConfiguration.create(glp, x11Screen, val[0]);
    }

    WrappedSurface ns = new WrappedSurface(cfg);
    ns.setSurfaceHandle(drawable);
    return new X11ExternalGLXContext(new Drawable(factory, ns), ctx);
  }

  protected boolean createImpl(GLContextImpl shareWith) {
      return true;
  }

  public int makeCurrent() throws GLException {
    // Save last context if necessary to allow external GLContexts to
    // talk to other GLContexts created by this library
    GLContext cur = getCurrent();
    if (cur != null && cur != this) {
      lastContext = cur;
      setCurrent(null);
    }
    return super.makeCurrent();
  }  

  public void release() throws GLException {
    super.release();
    setCurrent(lastContext);
    lastContext = null;
  }

  protected void makeCurrentImpl() throws GLException {
  }

  protected void releaseImpl() throws GLException {
  }

  protected void destroyImpl() throws GLException {
  }

  // Need to provide the display connection to extension querying APIs
  static class Drawable extends X11GLXDrawable {
    Drawable(GLDrawableFactory factory, NativeSurface comp) {
      super(factory, comp, true);
    }

    public GLContext createContext(GLContext shareWith) {
      throw new GLException("Should not call this");
    }

    public int getWidth() {
      throw new GLException("Should not call this");
    }

    public int getHeight() {
      throw new GLException("Should not call this");
    }

    public void setSize(int width, int height) {
      throw new GLException("Should not call this");
    }
  }
}
