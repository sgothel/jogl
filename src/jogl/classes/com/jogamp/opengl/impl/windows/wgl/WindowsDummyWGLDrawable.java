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

package com.jogamp.opengl.impl.windows.wgl;

import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;

import javax.media.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.impl.ProxySurface;
import com.jogamp.nativewindow.impl.windows.GDI;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLException;

public class WindowsDummyWGLDrawable extends WindowsWGLDrawable {
  private static final int f_dim = 128;
  private long hwnd, hdc;

  protected WindowsDummyWGLDrawable(GLDrawableFactory factory, GLCapabilitiesImmutable caps, AbstractGraphicsScreen absScreen) {
    super(factory, new ProxySurface(WindowsWGLGraphicsConfigurationFactory.createDefaultGraphicsConfiguration(caps, absScreen)), true);
    hwnd = GDI.CreateDummyWindow(0, 0, f_dim, f_dim);
    hdc = GDI.GetDC(hwnd);
    ProxySurface ns = (ProxySurface) getNativeSurface();
    ns.setSurfaceHandle(hdc);
    ns.setSize(f_dim, f_dim);
    WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration)ns.getGraphicsConfiguration().getNativeGraphicsConfiguration();

    try {
        config.updateGraphicsConfiguration(factory, ns);
        if (DEBUG) {
          System.err.println("!!! WindowsDummyWGLDrawable: "+config);
        }
    } catch (Throwable t) {
        destroyImpl();
        throw new GLException(t);
    }
  }

  public static WindowsDummyWGLDrawable create(GLDrawableFactory factory, GLProfile glp, AbstractGraphicsScreen absScreen) {
      GLCapabilities caps = new GLCapabilities(glp);
      caps.setDepthBits(16);
      caps.setDoubleBuffered(true);
      caps.setOnscreen  (true);
      caps.setPBuffer   (true);
      return new WindowsDummyWGLDrawable(factory, caps, absScreen);
  }

  public void setSize(int width, int height) {
  }

  public int getWidth() {
    return 1;
  }

  public int getHeight() {
    return 1;
  }

  public GLContext createContext(GLContext shareWith) {
    if (hdc == 0) {
      // Construction failed
      return null;
    }
    // FIXME: figure out how to hook back in the Java 2D / JOGL bridge
    return new WindowsWGLContext(this, shareWith);
  }

  protected void destroyImpl() {
    if (hdc != 0) {
      GDI.ReleaseDC(hwnd, hdc);
      hdc = 0;
    }
    if (hwnd != 0) {
      GDI.ShowWindow(hwnd, GDI.SW_HIDE);
      GDI.DestroyWindow(hwnd);
      hwnd = 0;
    }
  }
}
