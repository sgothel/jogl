/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
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
 * Neither the name Sven Gothel or the names of
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
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
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
