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

    long dpy = config.getScreen().getDevice().getHandle();
    int scrn = config.getScreen().getIndex();
    // System.out.println("X11DummyGLXDrawable: dpy "+toHexString(dpy)+", scrn "+scrn);
    X11Lib.XLockDisplay(dpy);
    try{
        nw.setSurfaceHandle( X11Lib.RootWindow(dpy, scrn) );
    } finally {
        X11Lib.XUnlockDisplay(dpy);
    }
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
    // nothing to do, but allowed
  }
}
