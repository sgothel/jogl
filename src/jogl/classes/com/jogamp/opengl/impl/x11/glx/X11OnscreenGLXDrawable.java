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

package com.jogamp.opengl.impl.x11.glx;

import javax.media.nativewindow.*;
import javax.media.opengl.*;

public class X11OnscreenGLXDrawable extends X11GLXDrawable {
  /** GLXWindow can't be made current on AWT with NVidia driver, hence disabled for now */
  public static final boolean USE_GLXWINDOW = false;
  long glXWindow; // GLXWindow, a GLXDrawable representation
  boolean useGLXWindow;

  protected X11OnscreenGLXDrawable(GLDrawableFactory factory, NativeWindow component) {
    super(factory, component, false);
    glXWindow=0;
    useGLXWindow=false;
  }

  public long getHandle() {
    if(useGLXWindow) {
        return glXWindow; 
    } 
    return getNativeWindow().getSurfaceHandle();
  }

  protected void destroyHandle() {
    if(0!=glXWindow) {
        GLX.glXDestroyWindow(getNativeWindow().getDisplayHandle(), glXWindow);
        glXWindow = 0;
        useGLXWindow=false;
    }
  }

  /** must be locked already */
  protected void updateHandle() {
    if(USE_GLXWINDOW) {
        X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();
        if(config.getFBConfig()>=0) {
            useGLXWindow=true;
            long dpy = getNativeWindow().getDisplayHandle();
            if(0!=glXWindow) {
                GLX.glXDestroyWindow(dpy, glXWindow);
            }
            glXWindow = GLX.glXCreateWindow(dpy, config.getFBConfig(), getNativeWindow().getSurfaceHandle(), null, 0);
            if (DEBUG) {
              System.err.println("!!! X11OnscreenGLXDrawable.setRealized(true): glXWindow: "+toHexString(getNativeWindow().getSurfaceHandle())+" -> "+toHexString(glXWindow));
            }
            if(0==glXWindow) {
                throw new GLException("X11OnscreenGLXDrawable.setRealized(true): GLX.glXCreateWindow() failed: "+this);
            }
        }
    }
  }

  public GLContext createContext(GLContext shareWith) {
    return new X11OnscreenGLXContext(this, shareWith);
  }

  public int getWidth() {
    return component.getWidth();
  }

  public int getHeight() {
    return component.getHeight();
  }
}
