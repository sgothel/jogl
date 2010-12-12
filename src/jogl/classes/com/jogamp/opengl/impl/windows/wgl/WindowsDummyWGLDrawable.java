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
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLException;

public class WindowsDummyWGLDrawable extends WindowsWGLDrawable {
  private static final int f_dim = 64;
  private long hwnd, hdc;

  protected WindowsDummyWGLDrawable(GLDrawableFactory factory, GLCapabilitiesImmutable caps, AbstractGraphicsScreen absScreen) {
    super(factory, new ProxySurface(WindowsWGLGraphicsConfigurationFactory.createDefaultGraphicsConfiguration(caps, absScreen)), true);
    hwnd = GDI.CreateDummyWindow(0, 0, f_dim, f_dim);
    if(0 == hwnd) {
        throw new GLException("Error hwnd 0, werr: "+GDI.GetLastError());
    }
    ProxySurface ns = (ProxySurface) getNativeSurface();
    ns.setSize(f_dim, f_dim);
    
    if(NativeSurface.LOCK_SURFACE_NOT_READY >= lockSurface()) {
        throw new GLException("WindowsDummyWGLDrawable: surface not ready (lockSurface)");
    }
    try {
        WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration)ns.getGraphicsConfiguration().getNativeGraphicsConfiguration();
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

  public static WindowsDummyWGLDrawable create(GLDrawableFactory factory, GLProfile glp, AbstractGraphicsScreen absScreen) {
      GLCapabilities caps = new GLCapabilities(glp);
      caps.setDepthBits(16);
      caps.setDoubleBuffered(true);
      caps.setOnscreen  (true);
      caps.setPBuffer   (true);
      return new WindowsDummyWGLDrawable(factory, caps, absScreen);
  }

  public int lockSurface() throws GLException {
    int res = NativeSurface.LOCK_SURFACE_NOT_READY;
    ProxySurface ns = (ProxySurface) getNativeSurface();
    AbstractGraphicsDevice adevice = ns.getGraphicsConfiguration().getNativeGraphicsConfiguration().getScreen().getDevice();
    adevice.lock();
    try {
        res = ns.lockSurface();
        if(NativeSurface.LOCK_SUCCESS == res) {
            if(0 == hdc) {
                hdc = GDI.GetDC(hwnd);
                ns.setSurfaceHandle(hdc);
                if(0 == hdc) {
                    res = NativeSurface.LOCK_SURFACE_NOT_READY;
                    ns.unlockSurface();
                    throw new GLException("Error hdc 0, werr: "+GDI.GetLastError());
                    // finally will unlock adevice
                }
            }
        } else {
            Throwable t = new Throwable("Error lock failed - res "+res+", hwnd "+toHexString(hwnd)+", hdc "+toHexString(hdc));
            t.printStackTrace();
        }
    } finally {
        if( NativeSurface.LOCK_SURFACE_NOT_READY == res ) {
            adevice.unlock();
        }
    }
    return res;
  }

  public void unlockSurface() {
    ProxySurface ns = (ProxySurface) getNativeSurface();
    ns.validateSurfaceLocked();
    AbstractGraphicsDevice adevice = ns.getGraphicsConfiguration().getNativeGraphicsConfiguration().getScreen().getDevice();
    
    try {
        if ( 0 != hdc && 0 != hwnd && ns.getSurfaceRecursionCount() == 0) {
            GDI.ReleaseDC(hwnd, hdc);
            hdc=0;
            ns.setSurfaceHandle(hdc);
        }
        surface.unlockSurface();
    } finally {
        adevice.unlock();
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

  public GLContext createContext(GLContext shareWith) {
    // FIXME: figure out how to hook back in the Java 2D / JOGL bridge
    return new WindowsWGLContext(this, shareWith);
  }

  protected void destroyImpl() {
    if (hdc != 0) {
      GDI.ReleaseDC(hwnd, hdc);
      hdc = 0;
      ProxySurface ns = (ProxySurface) getNativeSurface();
      ns.setSurfaceHandle(hdc);
    }
    if (hwnd != 0) {
      GDI.ShowWindow(hwnd, GDI.SW_HIDE);
      GDI.DestroyDummyWindow(hwnd);
      hwnd = 0;
    }
  }
}
