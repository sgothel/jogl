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

import java.nio.*;
import java.util.*;
import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.util.*;
import com.jogamp.opengl.impl.*;
import com.jogamp.nativewindow.impl.ProxySurface;

public class WindowsWGLDrawableFactory extends GLDrawableFactoryImpl {
  private static final boolean VERBOSE = Debug.verbose();

  private static final DesktopGLDynamicLookupHelper windowsWGLDynamicLookupHelper;

  static {
    DesktopGLDynamicLookupHelper tmp = null;
    try {
        tmp = new DesktopGLDynamicLookupHelper(new WindowsWGLDynamicLibraryBundleInfo());
    } catch (GLException gle) {
        if(DEBUG) {
            gle.printStackTrace();
        }
    }
    windowsWGLDynamicLookupHelper = tmp;
    if(null!=windowsWGLDynamicLookupHelper) {
        WGL.getWGLProcAddressTable().reset(windowsWGLDynamicLookupHelper);
    }
  }

  public GLDynamicLookupHelper getGLDynamicLookupHelper(int profile) {
      return windowsWGLDynamicLookupHelper;
  }

  public WindowsWGLDrawableFactory() {
    super();

    // Register our GraphicsConfigurationFactory implementations
    // The act of constructing them causes them to be registered
    new WindowsWGLGraphicsConfigurationFactory();
    try {
      ReflectionUtil.createInstance("com.jogamp.opengl.impl.windows.wgl.awt.WindowsAWTWGLGraphicsConfigurationFactory", 
                                    null, getClass().getClassLoader());
    } catch (JogampRuntimeException jre) { /* n/a .. */ }

    NativeWindowFactory.getDefaultToolkitLock().lock(); // OK
    try {
        sharedDrawable = new WindowsDummyWGLDrawable(this, null);
        WindowsWGLContext ctx  = (WindowsWGLContext) sharedDrawable.createContext(null);
        ctx.makeCurrent();
        canCreateGLPbuffer = ctx.getGL().isExtensionAvailable("GL_ARB_pbuffer");
        ctx.release();
        sharedContext = ctx;
    } catch (Throwable t) {
        throw new GLException("WindowsWGLDrawableFactory - Could not initialize shared resources", t);
    } finally {
        NativeWindowFactory.getDefaultToolkitLock().unlock(); // OK
    }
    if(null==sharedContext) {
        throw new GLException("WindowsWGLDrawableFactory - Shared Context is null");
    }
    if (DEBUG) {
      System.err.println("!!! SharedContext: "+sharedContext+", pbuffer supported "+canCreateGLPbuffer);
    }
  }

  WindowsDummyWGLDrawable sharedDrawable=null;
  WindowsWGLContext sharedContext=null;
  boolean canCreateGLPbuffer = false;

  protected final GLDrawableImpl getSharedDrawable() {
    return sharedDrawable; 
  }

  protected final GLContextImpl getSharedContext() {
    return sharedContext; 
  }

  protected void shutdownInstance() {
     if (DEBUG) {
          System.err.println("!!! Shutdown Shared:");
          System.err.println("!!!          CTX     : "+sharedContext);
          System.err.println("!!!          Drawable: "+sharedDrawable);
          Exception e = new Exception("Debug");
          e.printStackTrace();
     }
    // don't free native resources from this point on,
    // since we might be in a critical shutdown hook sequence
     if(null!=sharedContext) {
        // may cause deadlock: sharedContext.destroy(); // implies release, if current
        sharedContext=null;
     }
     if(null!=sharedDrawable) {
        // may cause deadlock: sharedDrawable.destroy();
        sharedDrawable=null;
     }
  }

  protected GLDrawableImpl createOnscreenDrawableImpl(NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new WindowsOnscreenWGLDrawable(this, target);
  }

  protected GLDrawableImpl createOffscreenDrawableImpl(NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new WindowsOffscreenWGLDrawable(this, target);
  }

  public boolean canCreateGLPbuffer(AbstractGraphicsDevice device) {
    return canCreateGLPbuffer;
  }

  protected GLDrawableImpl createGLPbufferDrawableImpl(final NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    final List returnList = new ArrayList();
    Runnable r = new Runnable() {
        public void run() {
          GLContext lastContext = GLContext.getCurrent();
          if (lastContext != null) {
            lastContext.release();
          }
          synchronized(WindowsWGLDrawableFactory.this.sharedContext) {
              WindowsWGLDrawableFactory.this.sharedContext.makeCurrent();
              try {
                WGLExt wglExt = WindowsWGLDrawableFactory.this.sharedContext.getWGLExt();
                GLDrawableImpl pbufferDrawable = new WindowsPbufferWGLDrawable(WindowsWGLDrawableFactory.this, target,
                                                                               WindowsWGLDrawableFactory.this.sharedDrawable,
                                                                               wglExt);
                returnList.add(pbufferDrawable);
              } finally {
                WindowsWGLDrawableFactory.this.sharedContext.release();
                if (lastContext != null) {
                  lastContext.makeCurrent();
                }
              }
          }
        }
      };
    maybeDoSingleThreadedWorkaround(r);
    return (GLDrawableImpl) returnList.get(0);
  }

  protected NativeSurface createOffscreenSurfaceImpl(GLCapabilities capabilities, GLCapabilitiesChooser chooser, int width, int height) {
    AbstractGraphicsScreen screen = DefaultGraphicsScreen.createDefault();
    ProxySurface ns = new ProxySurface(WindowsWGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(
                                     capabilities, chooser, screen) );
    ns.setSize(width, height);
    return ns;
  }
 
  protected GLContext createExternalGLContextImpl() {
    return WindowsExternalWGLContext.create(this, null);
  }

  public boolean canCreateExternalGLDrawable(AbstractGraphicsDevice device) {
    return true;
  }

  protected GLDrawable createExternalGLDrawableImpl() {
    return WindowsExternalWGLDrawable.create(this, null);
  }

  static String wglGetLastError() {
    long err = GDI.GetLastError();
    String detail = null;
    switch ((int) err) {
      case GDI.ERROR_INVALID_PIXEL_FORMAT: detail = "ERROR_INVALID_PIXEL_FORMAT";       break;
      case GDI.ERROR_NO_SYSTEM_RESOURCES:  detail = "ERROR_NO_SYSTEM_RESOURCES";        break;
      case GDI.ERROR_INVALID_DATA:         detail = "ERROR_INVALID_DATA";               break;
      case GDI.ERROR_PROC_NOT_FOUND:       detail = "ERROR_PROC_NOT_FOUND";             break;
      case GDI.ERROR_INVALID_WINDOW_HANDLE:detail = "ERROR_INVALID_WINDOW_HANDLE";      break;
      default:                             detail = "(Unknown error code " + err + ")"; break;
    }
    return detail;
  }

  public boolean canCreateContextOnJava2DSurface(AbstractGraphicsDevice device) {
    return false;
  }

  public GLContext createContextOnJava2DSurface(Object graphics, GLContext shareWith)
    throws GLException {
    throw new GLException("Unimplemented on this platform");
  }

  //------------------------------------------------------
  // Gamma-related functionality
  //

  private static final int GAMMA_RAMP_LENGTH = 256;

  protected int getGammaRampLength() {
    return GAMMA_RAMP_LENGTH;
  }

  protected boolean setGammaRamp(float[] ramp) {
    short[] rampData = new short[3 * GAMMA_RAMP_LENGTH];
    for (int i = 0; i < GAMMA_RAMP_LENGTH; i++) {
      short scaledValue = (short) (ramp[i] * 65535);
      rampData[i] = scaledValue;
      rampData[i +     GAMMA_RAMP_LENGTH] = scaledValue;
      rampData[i + 2 * GAMMA_RAMP_LENGTH] = scaledValue;
    }

    long screenDC = GDI.GetDC(0);
    boolean res = GDI.SetDeviceGammaRamp(screenDC, ShortBuffer.wrap(rampData));
    GDI.ReleaseDC(0, screenDC);
    return res;
  }

  protected Buffer getGammaRamp() {
    ShortBuffer rampData = ShortBuffer.wrap(new short[3 * GAMMA_RAMP_LENGTH]);
    long screenDC = GDI.GetDC(0);
    boolean res = GDI.GetDeviceGammaRamp(screenDC, rampData);
    GDI.ReleaseDC(0, screenDC);
    if (!res) {
      return null;
    }
    return rampData;
  }

  protected void resetGammaRamp(Buffer originalGammaRamp) {
    if (originalGammaRamp == null) {
      // getGammaRamp failed earlier
      return;
    }
    long screenDC = GDI.GetDC(0);
    GDI.SetDeviceGammaRamp(screenDC, originalGammaRamp);
    GDI.ReleaseDC(0, screenDC);
  }
}
