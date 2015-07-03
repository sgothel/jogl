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

package jogamp.opengl.x11.glx;

import java.nio.IntBuffer;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.WrappedSurface;

import com.jogamp.common.nio.Buffers;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;


public class X11ExternalGLXDrawable extends X11GLXDrawable {

  private X11ExternalGLXDrawable(final GLDrawableFactory factory, final NativeSurface surface) {
    super(factory, surface, true);
  }

  protected static X11ExternalGLXDrawable create(final GLDrawableFactory factory, final GLProfile glp) {
    final long context = GLX.glXGetCurrentContext();
    if (context == 0) {
      throw new GLException("Error: current context null");
    }
    final long display = GLX.glXGetCurrentDisplay();
    if (display == 0) {
      throw new GLException("Error: current display null");
    }
    final long drawable = GLX.glXGetCurrentDrawable();
    if (drawable == 0) {
      throw new GLException("Error: attempted to make an external GLDrawable without a drawable current");
    }
    final IntBuffer val = Buffers.newDirectIntBuffer(1);

    GLX.glXQueryContext(display, context, GLX.GLX_SCREEN, val);
    final X11GraphicsScreen x11Screen = (X11GraphicsScreen) X11GraphicsScreen.createScreenDevice(display, val.get(0), false);

    GLX.glXQueryContext(display, context, GLX.GLX_FBCONFIG_ID, val);
    final X11GLXGraphicsConfiguration cfg = X11GLXGraphicsConfiguration.create(glp, x11Screen, val.get(0));

    int w, h;
    GLX.glXQueryDrawable(display, drawable, GLX.GLX_WIDTH, val);
    w=val.get(0);
    GLX.glXQueryDrawable(display, drawable, GLX.GLX_HEIGHT, val);
    h=val.get(0);

    GLX.glXQueryContext(display, context, GLX.GLX_RENDER_TYPE, val);
    if ((val.get(0) & GLX.GLX_RGBA_TYPE) == 0) {
      if (DEBUG) {
        System.err.println("X11ExternalGLXDrawable: WARNING: forcing GLX_RGBA_TYPE for newly created contexts (current 0x"+Integer.toHexString(val.get(0))+")");
      }
    }
    return new X11ExternalGLXDrawable(factory, new WrappedSurface(cfg, drawable, w, h, true));
  }

  @Override
  public GLContext createContext(final GLContext shareWith) {
    return new Context(this, shareWith);
  }

  public void setSize(final int newWidth, final int newHeight) {
    throw new GLException("Should not call this");
  }

  static class Context extends X11GLXContext {
    Context(final X11GLXDrawable drawable, final GLContext shareWith) {
      super(drawable, shareWith);
    }
  }
}
