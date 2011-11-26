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
 
package jogamp.opengl.x11.glx;

import javax.media.opengl.*;

import javax.media.nativewindow.x11.*;
import jogamp.nativewindow.*;
import jogamp.nativewindow.x11.*;

public class X11DummyGLXDrawable extends X11OnscreenGLXDrawable {
  private static final int f_dim = 64;
  private long dummyWindow = 0;

  /** 
   * Due to the ATI Bug https://bugzilla.mozilla.org/show_bug.cgi?id=486277,
   * we cannot switch the Display as we please, 
   * hence we reuse the target's screen configuration. 
   */
  public X11DummyGLXDrawable(X11GraphicsScreen screen, GLDrawableFactory factory, GLCapabilitiesImmutable caps) {
    super(factory, 
          new WrappedSurface(X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(
            caps, caps, null, screen)));
    this.realized = true;

    WrappedSurface ns = (WrappedSurface) getNativeSurface();
    X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)ns.getGraphicsConfiguration();

    X11GraphicsDevice device = (X11GraphicsDevice) screen.getDevice();
    long dpy = device.getHandle();
    int scrn = screen.getIndex();
    long visualID = config.getVisualID();

    dummyWindow = X11Lib.CreateDummyWindow(dpy, scrn, visualID, f_dim, f_dim);
    ns.setSurfaceHandle( dummyWindow );
    ns.surfaceSizeChanged(f_dim, f_dim);

    updateHandle();
  }

  public static X11DummyGLXDrawable create(X11GraphicsScreen screen, GLDrawableFactory factory, GLProfile glp) {
      GLCapabilities caps = new GLCapabilities(glp);
      return new X11DummyGLXDrawable(screen, factory, caps);
  }

  public void setSize(int width, int height) {
  }

  public int getWidth() {
    return 1;
  }

  public int getHeight() {
    return 1;
  }

  protected void destroyImpl() {
    if(0!=dummyWindow) {
        destroyHandle();
        X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)getNativeSurface().getGraphicsConfiguration();
        X11Lib.DestroyDummyWindow(config.getScreen().getDevice().getHandle(), dummyWindow);
    }
  }
}
