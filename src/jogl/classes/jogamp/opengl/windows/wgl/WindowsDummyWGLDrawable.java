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

package jogamp.opengl.windows.wgl;

import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;

import javax.media.nativewindow.AbstractGraphicsScreen;
import jogamp.nativewindow.windows.GDI;
import jogamp.nativewindow.windows.GDIUtil;

import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLException;
import jogamp.nativewindow.windows.GDISurface;

public class WindowsDummyWGLDrawable extends WindowsWGLDrawable {
  private long hwnd;
  private boolean handleHwndLifecycle;

  private WindowsDummyWGLDrawable(GLDrawableFactory factory, GDISurface ns, boolean handleHwndLifecycle) {
    super(factory, ns, true);
    this.handleHwndLifecycle = handleHwndLifecycle;
    
    if(NativeSurface.LOCK_SURFACE_NOT_READY >= ns.lockSurface()) {
        throw new GLException("WindowsDummyWGLDrawable: surface not ready (lockSurface)");
    }
    try {
        WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration)ns.getGraphicsConfiguration();
        config.updateGraphicsConfiguration(factory, ns, null);
        if (DEBUG) {
          System.err.println("!!! WindowsDummyWGLDrawable: "+config);
        }
    } catch (Throwable t) {
        destroyImpl();
        throw new GLException(t);
    } finally {
        unlockSurface();
    }
  }

  public static WindowsDummyWGLDrawable create(GLDrawableFactory factory, GLProfile glp, AbstractGraphicsScreen absScreen,
                                               long windowHandle, int width, int height, boolean handleWindowLifecycle) {
    if(0 == windowHandle) {
        throw new GLException("Error windowHandle 0, werr: "+GDI.GetLastError());
    }
    GLCapabilities caps = new GLCapabilities(glp);
    WindowsWGLGraphicsConfiguration cfg = WindowsWGLGraphicsConfigurationFactory.createDefaultGraphicsConfiguration(caps, absScreen);
    GDISurface ns = new GDISurface(cfg, windowHandle);
    ns.surfaceSizeChanged(width, height);
    return new WindowsDummyWGLDrawable(factory, ns, handleWindowLifecycle);
  }

  public GLContext createContext(GLContext shareWith) {
    // FIXME: figure out how to hook back in the Java 2D / JOGL bridge
    return new WindowsWGLContext(this, shareWith);
  }

  protected void destroyImpl() {
    if (handleHwndLifecycle && hwnd != 0) {
      GDI.ShowWindow(hwnd, GDI.SW_HIDE);
      GDIUtil.DestroyDummyWindow(hwnd);
      hwnd = 0;
    }
  }
}
