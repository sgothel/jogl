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

import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.windows.WindowsGraphicsDevice;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.impl.ProxySurface;
import com.jogamp.nativewindow.impl.windows.GDI;
import com.jogamp.opengl.impl.DesktopGLDynamicLookupHelper;
import com.jogamp.opengl.impl.GLDrawableFactoryImpl;
import com.jogamp.opengl.impl.GLDrawableImpl;
import com.jogamp.opengl.impl.GLDynamicLookupHelper;
import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.opengl.GLCapabilitiesImmutable;

public class WindowsWGLDrawableFactory extends GLDrawableFactoryImpl {
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
    if(GLProfile.isAWTAvailable()) {
        try {
          ReflectionUtil.createInstance("com.jogamp.opengl.impl.windows.wgl.awt.WindowsAWTWGLGraphicsConfigurationFactory",
                                        null, getClass().getClassLoader());
        } catch (JogampRuntimeException jre) { /* n/a .. */ }
    }

    defaultDevice = new WindowsGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
  }

  static class SharedResource {
      private WindowsGraphicsDevice device;
      private WindowsDummyWGLDrawable drawable;
      private WindowsWGLContext context;
      private boolean canCreateGLPbuffer;
      private boolean readDrawableAvailable;

      SharedResource(WindowsGraphicsDevice dev, WindowsDummyWGLDrawable draw, WindowsWGLContext ctx,
                     boolean readBufferAvail, boolean canPbuffer) {
          device = dev;
          drawable = draw;
          context = ctx;
          canCreateGLPbuffer = canPbuffer;
          readDrawableAvailable = readBufferAvail;
      }
      WindowsGraphicsDevice getDevice() { return device; }
      WindowsWGLDrawable getDrawable() { return drawable; }
      WindowsWGLContext getContext() { return context; }
      boolean canCreateGLPbuffer() { return canCreateGLPbuffer; }
      boolean isReadDrawableAvailable() { return readDrawableAvailable; }

  }
  HashMap/*<connection, SharedResource>*/ sharedMap = new HashMap();
  WindowsGraphicsDevice defaultDevice;

  public final AbstractGraphicsDevice getDefaultDevice() {
      return defaultDevice;
  }

  public final boolean getIsDeviceCompatible(AbstractGraphicsDevice device) {
      if(device instanceof WindowsGraphicsDevice) {
          return true;
      }
      return false;
  }

  HashSet devicesTried = new HashSet();
  private final boolean getDeviceTried(String connection) {
      synchronized(devicesTried) {
          return devicesTried.contains(connection);
      }
  }
  private final void addDeviceTried(String connection) {
      synchronized(devicesTried) {
          devicesTried.add(connection);
      }
  }

  final static String GL_ARB_pbuffer = "GL_ARB_pbuffer";
  final static String WGL_ARB_make_current_read = "WGL_ARB_make_current_read";
  final static String wglMakeContextCurrent = "wglMakeContextCurrent";

  private SharedResource getOrCreateShared(AbstractGraphicsDevice device) {
    String connection = device.getConnection();
    SharedResource sr;
    synchronized(sharedMap) {
        sr = (SharedResource) sharedMap.get(connection);
    }
    if(null==sr && !getDeviceTried(connection)) {
        addDeviceTried(connection);
        NativeWindowFactory.getDefaultToolkitLock().lock(); // OK
        try {
            WindowsGraphicsDevice sharedDevice = new WindowsGraphicsDevice(connection, AbstractGraphicsDevice.DEFAULT_UNIT);
            GLProfile glp = GLProfile.getDefault(/*sharedDevice*/); // can't fetch device profile, which shared resource we create here
            AbstractGraphicsScreen absScreen = new DefaultGraphicsScreen(sharedDevice, 0);
            WindowsDummyWGLDrawable sharedDrawable = WindowsDummyWGLDrawable.create(this, glp, absScreen);
            WindowsWGLContext ctx  = (WindowsWGLContext) sharedDrawable.createContext(null);
            ctx.setSynchronized(true);
            ctx.makeCurrent();
            boolean canCreateGLPbuffer = ctx.getGL().isExtensionAvailable(GL_ARB_pbuffer);
            boolean readDrawableAvailable = ctx.isExtensionAvailable(WGL_ARB_make_current_read) &&
                                            ctx.isFunctionAvailable(wglMakeContextCurrent);
            if (DEBUG) {
              System.err.println("!!! SharedContext: "+ctx+", pbuffer supported "+canCreateGLPbuffer+
                                 ", readDrawable supported "+readDrawableAvailable);
            }
            ctx.release();
            sr = new SharedResource(sharedDevice, sharedDrawable, ctx, readDrawableAvailable, canCreateGLPbuffer);
            synchronized(sharedMap) {
                sharedMap.put(connection, sr);
            }
        } catch (Throwable t) {
            throw new GLException("WindowsWGLDrawableFactory - Could not initialize shared resources", t);
        } finally {
            NativeWindowFactory.getDefaultToolkitLock().unlock(); // OK
        }
    }
    return sr;
  }

  protected final GLContext getOrCreateSharedContextImpl(AbstractGraphicsDevice device) {
    SharedResource sr = getOrCreateShared(device);
    if(null!=sr) {
      return sr.getContext();
    }
    return null;
  }

  protected AbstractGraphicsDevice getOrCreateSharedDeviceImpl(AbstractGraphicsDevice device) {
    SharedResource sr = getOrCreateShared(device);
    if(null!=sr) {
        return sr.getDevice();
    }
    return null;
  }

  protected WindowsWGLDrawable getSharedDrawable(AbstractGraphicsDevice device) {
    SharedResource sr = getOrCreateShared(device);
    if(null!=sr) {
        return sr.getDrawable();
    }
    return null;
  }

  protected final void shutdownInstance() {
    if (DEBUG) {
        Exception e = new Exception("Debug");
        e.printStackTrace();
    }
    Collection/*<SharedResource>*/ sharedResources = sharedMap.values();
    for(Iterator iter=sharedResources.iterator(); iter.hasNext(); ) {
        SharedResource sr = (SharedResource) iter.next();

        if (DEBUG) {
          System.err.println("!!! Shutdown Shared:");
          System.err.println("!!!          Drawable: "+sr.drawable);
          System.err.println("!!!          CTX     : "+sr.context);
        }

        if (null != sr.context) {
          // may cause JVM SIGSEGV: sharedContext.destroy();
          sr.context = null;
        }

        if (null != sr.drawable) {
          // may cause JVM SIGSEGV: sharedDrawable.destroy();
          sr.drawable = null;
        }

    }
    sharedMap.clear();
    if (DEBUG) {
      System.err.println("!!! Shutdown Shared Finished");
    }
  }

  protected final GLDrawableImpl createOnscreenDrawableImpl(NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new WindowsOnscreenWGLDrawable(this, target);
  }

  protected final GLDrawableImpl createOffscreenDrawableImpl(final NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    AbstractGraphicsConfiguration config = target.getGraphicsConfiguration().getNativeGraphicsConfiguration();
    GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    if(!caps.isPBuffer()) {
        return new WindowsBitmapWGLDrawable(this, target);
    }

    // PBuffer GLDrawable Creation
    final AbstractGraphicsDevice device = config.getScreen().getDevice();

    final SharedResource sr = getOrCreateShared(device);
    if(null==sr) {
        throw new IllegalArgumentException("No shared resource for "+device);
    }
    final List returnList = new ArrayList();
    Runnable r = new Runnable() {
        public void run() {
          GLContext lastContext = GLContext.getCurrent();
          if (lastContext != null) {
            lastContext.release();
          }
          synchronized(sr.context) {
              sr.context.makeCurrent();
              try {
                GLDrawableImpl pbufferDrawable = new WindowsPbufferWGLDrawable(WindowsWGLDrawableFactory.this, target,
                                                                               sr.drawable,
                                                                               sr.context);
                returnList.add(pbufferDrawable);
              } finally {
                sr.context.release();
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

  /**
   * @return 1 if read drawable extension is available, 0 if not
   *           and -1 if undefined yet, ie no shared device exist at this point.
   */
  public final int isReadDrawableAvailable(AbstractGraphicsDevice device) {
    SharedResource sr = getOrCreateShared((null!=device)?device:defaultDevice);
    if(null!=sr) {
        return sr.isReadDrawableAvailable() ? 1 : 0 ;
    }
    return -1; // undefined
  }

  public final boolean canCreateGLPbuffer(AbstractGraphicsDevice device) {
    SharedResource sr = getOrCreateShared((null!=device)?device:defaultDevice);
    if(null!=sr) {
        return sr.canCreateGLPbuffer();
    }
    return false;
  }

  protected final NativeSurface createOffscreenSurfaceImpl(AbstractGraphicsDevice device,GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser, int width, int height) {
    AbstractGraphicsScreen screen = DefaultGraphicsScreen.createDefault(NativeWindowFactory.TYPE_WINDOWS);
    ProxySurface ns = new ProxySurface(WindowsWGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(
                                     capsChosen, capsRequested, chooser, screen) );
    ns.setSize(width, height);
    return ns;
  }
 
  protected final GLContext createExternalGLContextImpl() {
    return WindowsExternalWGLContext.create(this, null);
  }

  public final boolean canCreateExternalGLDrawable(AbstractGraphicsDevice device) {
    return true;
  }

  protected final GLDrawable createExternalGLDrawableImpl() {
    return WindowsExternalWGLDrawable.create(this, null);
  }

  static String wglGetLastError() {
    long err = GDI.GetLastError();
    String detail = null;
    switch ((int) err) {
      case GDI.ERROR_SUCCESS:              detail = "ERROR_SUCCESS";                    break;
      case GDI.ERROR_INVALID_PIXEL_FORMAT: detail = "ERROR_INVALID_PIXEL_FORMAT";       break;
      case GDI.ERROR_NO_SYSTEM_RESOURCES:  detail = "ERROR_NO_SYSTEM_RESOURCES";        break;
      case GDI.ERROR_INVALID_DATA:         detail = "ERROR_INVALID_DATA";               break;
      case GDI.ERROR_PROC_NOT_FOUND:       detail = "ERROR_PROC_NOT_FOUND";             break;
      case GDI.ERROR_INVALID_WINDOW_HANDLE:detail = "ERROR_INVALID_WINDOW_HANDLE";      break;
      default:                             detail = "(Unknown error code " + err + ")"; break;
    }
    return detail;
  }

  public final boolean canCreateContextOnJava2DSurface(AbstractGraphicsDevice device) {
    return false;
  }

  public final GLContext createContextOnJava2DSurface(Object graphics, GLContext shareWith)
    throws GLException {
    throw new GLException("Unimplemented on this platform");
  }

  //------------------------------------------------------
  // Gamma-related functionality
  //

  private static final int GAMMA_RAMP_LENGTH = 256;

  protected final int getGammaRampLength() {
    return GAMMA_RAMP_LENGTH;
  }

  protected final boolean setGammaRamp(float[] ramp) {
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

  protected final Buffer getGammaRamp() {
    ShortBuffer rampData = ShortBuffer.wrap(new short[3 * GAMMA_RAMP_LENGTH]);
    long screenDC = GDI.GetDC(0);
    boolean res = GDI.GetDeviceGammaRamp(screenDC, rampData);
    GDI.ReleaseDC(0, screenDC);
    if (!res) {
      return null;
    }
    return rampData;
  }

  protected final void resetGammaRamp(Buffer originalGammaRamp) {
    if (originalGammaRamp == null) {
      // getGammaRamp failed earlier
      return;
    }
    long screenDC = GDI.GetDC(0);
    GDI.SetDeviceGammaRamp(screenDC, originalGammaRamp);
    GDI.ReleaseDC(0, screenDC);
  }
}
