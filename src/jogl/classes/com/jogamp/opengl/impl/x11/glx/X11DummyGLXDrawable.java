/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 
package com.jogamp.opengl.impl.x11.glx;

import javax.media.opengl.*;
import com.jogamp.opengl.impl.*;

import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import com.jogamp.nativewindow.impl.*;
import com.jogamp.nativewindow.impl.x11.*;

public class X11DummyGLXDrawable extends X11OnscreenGLXDrawable {

  private long dummyWindow = 0;

  /** 
   * Due to the ATI Bug https://bugzilla.mozilla.org/show_bug.cgi?id=486277,
   * we cannot switch the Display as we please, 
   * hence we reuse the target's screen configuration. 
   */
  public X11DummyGLXDrawable(X11GraphicsScreen screen, GLDrawableFactory factory, GLProfile glp) {
    super(factory, 
          new NullWindow(X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(
            new GLCapabilities(glp), null, screen)));
    this.realized = true;

    NullWindow nw = (NullWindow) getNativeWindow();
    X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)nw.getGraphicsConfiguration().getNativeGraphicsConfiguration();
    GLCapabilities caps = (GLCapabilities) config.getChosenCapabilities();

    X11GraphicsDevice device = (X11GraphicsDevice) screen.getDevice();
    long dpy = device.getHandle();
    int scrn = screen.getIndex();
    long visualID = config.getVisualID();

    dummyWindow = X11Lib.CreateDummyWindow(dpy, scrn, visualID);
    nw.setSurfaceHandle( dummyWindow );

    updateHandle();
  }

  public void setSize(int width, int height) {
  }

  public int getWidth() {
    return 1;
  }

  public int getHeight() {
    return 1;
  }

  public void destroy() {
    if(0!=dummyWindow) {
        destroyHandle();
        X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();
        X11Lib.DestroyDummyWindow(config.getScreen().getDevice().getHandle(), dummyWindow);
    }
  }
}
