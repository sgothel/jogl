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

import java.nio.IntBuffer;

import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.VisualIDHolder;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import jogamp.nativewindow.WrappedSurface;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLContextShareSet;

import com.jogamp.common.nio.Buffers;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;

public class X11ExternalGLXContext extends X11GLXContext {

  private X11ExternalGLXContext(Drawable drawable, long ctx) {
    super(drawable, null);
    this.contextHandle = ctx;
    GLContextShareSet.contextCreated(this);
    setGLFunctionAvailability(false, 0, 0, CTX_PROFILE_COMPAT, false /* strictMatch */, false /* withinGLVersionsMapping */); // use GL_VERSION
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
    IntBuffer val = Buffers.newDirectIntBuffer(1);

    int w, h;
    GLX.glXQueryDrawable(display, drawable, GLX.GLX_WIDTH, val);
    w=val.get(0);
    GLX.glXQueryDrawable(display, drawable, GLX.GLX_HEIGHT, val);
    h=val.get(0);

    GLX.glXQueryContext(display, ctx, GLX.GLX_SCREEN, val);
    X11GraphicsScreen x11Screen = (X11GraphicsScreen) X11GraphicsScreen.createScreenDevice(display, val.get(0), false);

    GLX.glXQueryContext(display, ctx, GLX.GLX_FBCONFIG_ID, val);
    X11GLXGraphicsConfiguration cfg = null;
    // sometimes glXQueryContext on an external context gives us a framebuffer config ID
    // of 0, which doesn't work in a subsequent call to glXChooseFBConfig; if this happens,
    // create and use a default config (this has been observed when running on CentOS 5.5 inside
    // of VMWare Server 2.0 with the Mesa 6.5.1 drivers)
    if( VisualIDHolder.VID_UNDEFINED == val.get(0) || !X11GLXGraphicsConfiguration.GLXFBConfigIDValid(display, x11Screen.getIndex(), val.get(0)) ) {
        GLCapabilities glcapsDefault = new GLCapabilities(GLProfile.getDefault());
        cfg = X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(glcapsDefault, glcapsDefault, null, x11Screen, VisualIDHolder.VID_UNDEFINED);
        if(DEBUG) {
            System.err.println("X11ExternalGLXContext invalid FBCONFIG_ID "+val.get(0)+", using default cfg: " + cfg);
        }
    } else {
        cfg = X11GLXGraphicsConfiguration.create(glp, x11Screen, val.get(0));
    }

    final WrappedSurface ns = new WrappedSurface(cfg, drawable, w, h, true);
    return new X11ExternalGLXContext(new Drawable(factory, ns), ctx);
  }

  @Override
  protected boolean createImpl(GLContextImpl shareWith) {
      return true;
  }

  @Override
  protected void makeCurrentImpl() throws GLException {
  }

  @Override
  protected void releaseImpl() throws GLException {
  }

  @Override
  protected void destroyImpl() throws GLException {
  }

  // Need to provide the display connection to extension querying APIs
  static class Drawable extends X11GLXDrawable {
    Drawable(GLDrawableFactory factory, NativeSurface comp) {
      super(factory, comp, true);
    }

    @Override
    public GLContext createContext(GLContext shareWith) {
      throw new GLException("Should not call this");
    }

    @Override
    public int getWidth() {
      throw new GLException("Should not call this");
    }

    @Override
    public int getHeight() {
      throw new GLException("Should not call this");
    }

    public void setSize(int width, int height) {
      throw new GLException("Should not call this");
    }
  }
}
