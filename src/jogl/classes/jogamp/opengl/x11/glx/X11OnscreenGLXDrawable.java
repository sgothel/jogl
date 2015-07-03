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

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;

public class X11OnscreenGLXDrawable extends X11GLXDrawable {
  /** GLXWindow can't be made current on AWT with NVidia driver, hence disabled for now */
  public static final boolean USE_GLXWINDOW = false;
  long glXWindow; // GLXWindow, a GLXDrawable representation
  boolean useGLXWindow;

  protected X11OnscreenGLXDrawable(final GLDrawableFactory factory, final NativeSurface component, final boolean realized) {
    super(factory, component, realized);
    glXWindow=0;
    useGLXWindow=false;
    if(realized) {
        createHandle();
    }
  }

  @Override
  public long getHandle() {
    if(USE_GLXWINDOW) {
        if(useGLXWindow) {
            return glXWindow;
        }
    }
    return super.getHandle();
  }

  @Override
  protected void destroyHandle() {
    if(USE_GLXWINDOW) {
        if(0!=glXWindow) {
            GLX.glXDestroyWindow(getNativeSurface().getDisplayHandle(), glXWindow);
            glXWindow = 0;
            useGLXWindow=false;
        }
    }
  }

  @Override
  protected final void createHandle() {
    if(USE_GLXWINDOW) {
        final X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)getNativeSurface().getGraphicsConfiguration();
        if(config.getFBConfig()>=0) {
            useGLXWindow=true;
            final long dpy = getNativeSurface().getDisplayHandle();
            if(0!=glXWindow) {
                GLX.glXDestroyWindow(dpy, glXWindow);
            }
            glXWindow = GLX.glXCreateWindow(dpy, config.getFBConfig(), getNativeSurface().getSurfaceHandle(), null);
            if (DEBUG) {
              System.err.println("X11OnscreenGLXDrawable.setRealized(true): glXWindow: "+toHexString(getNativeSurface().getSurfaceHandle())+" -> "+toHexString(glXWindow));
            }
            if(0==glXWindow) {
                throw new GLException("X11OnscreenGLXDrawable.setRealized(true): GLX.glXCreateWindow() failed: "+this);
            }
        }
    }
  }

  @Override
  public GLContext createContext(final GLContext shareWith) {
    return new X11GLXContext(this, shareWith);
  }
}
