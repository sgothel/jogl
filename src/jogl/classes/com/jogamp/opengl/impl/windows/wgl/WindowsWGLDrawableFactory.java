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
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.windows.WindowsGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.impl.ProxySurface;
import com.jogamp.nativewindow.impl.windows.GDI;
import com.jogamp.nativewindow.impl.windows.RegisteredClassFactory;
import com.jogamp.opengl.impl.DesktopGLDynamicLookupHelper;
import com.jogamp.opengl.impl.GLContextImpl;
import com.jogamp.opengl.impl.GLDrawableFactoryImpl;
import com.jogamp.opengl.impl.GLDrawableImpl;
import com.jogamp.opengl.impl.GLDynamicLookupHelper;
import com.jogamp.opengl.impl.SharedResourceRunner;

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

    // Init shared resources off thread
    // Will be released via ShutdownHook
    sharedResourceImpl = new SharedResourceImplementation();
    sharedResourceRunner = new SharedResourceRunner(sharedResourceImpl);
    sharedResourceThread = new Thread(sharedResourceRunner, Thread.currentThread().getName()+"-SharedResourceRunner");
    sharedResourceThread.setDaemon(true); // Allow JVM to exit, even if this one is running
    sharedResourceThread.start();
  }

  WindowsGraphicsDevice defaultDevice;
  SharedResourceImplementation sharedResourceImpl;
  SharedResourceRunner sharedResourceRunner;
  Thread sharedResourceThread;
  HashMap/*<connection, SharedResource>*/ sharedMap = new HashMap();

  long processAffinityChanges = 0;
  PointerBuffer procMask = PointerBuffer.allocateDirect(1);
  PointerBuffer sysMask = PointerBuffer.allocateDirect(1);

  protected void enterThreadCriticalZone() {
    synchronized (sysMask) {
        if( 0 == processAffinityChanges) {
            long pid = GDI.GetCurrentProcess();
            if ( GDI.GetProcessAffinityMask(pid, procMask, sysMask) ) {
                if(DEBUG) {
                    System.err.println("WindowsWGLDrawableFactory.enterThreadCriticalZone() - 0x" + Long.toHexString(pid) + " - " + Thread.currentThread().getName());
                    Thread.dumpStack();
                }
                processAffinityChanges = pid;
                GDI.SetProcessAffinityMask(pid, 1);
            }
        }
    }
  }

  protected void leaveThreadCriticalZone() {
    synchronized (sysMask) {
        if( 0 != processAffinityChanges) {
            long pid = GDI.GetCurrentProcess();
            if( pid != processAffinityChanges) {
                throw new GLException("PID doesn't match: set PID 0x" + Long.toHexString(processAffinityChanges) +
                                                       " this PID 0x" + Long.toHexString(pid) );
            }
            if(DEBUG) {
                System.err.println("WindowsWGLDrawableFactory.leaveThreadCriticalZone() - 0x" + Long.toHexString(pid) + " - " + Thread.currentThread().getName());
            }
            GDI.SetProcessAffinityMask(pid, sysMask.get(0));
        }
    }
  }

  static class SharedResource implements SharedResourceRunner.Resource {
      private WindowsGraphicsDevice device;
      private AbstractGraphicsScreen screen;
      private WindowsDummyWGLDrawable drawable;
      private WindowsWGLContext context;
      private boolean canCreateGLPbuffer;
      private boolean readDrawableAvailable;

      SharedResource(WindowsGraphicsDevice dev, AbstractGraphicsScreen scrn, WindowsDummyWGLDrawable draw, WindowsWGLContext ctx,
                     boolean readBufferAvail, boolean canPbuffer) {
          device = dev;
          screen = scrn;
          drawable = draw;
          context = ctx;
          canCreateGLPbuffer = canPbuffer;
          readDrawableAvailable = readBufferAvail;
      }
      final public AbstractGraphicsDevice getDevice() { return device; }
      final public AbstractGraphicsScreen getScreen() { return screen; }
      final public GLDrawableImpl getDrawable() { return drawable; }
      final public GLContextImpl getContext() { return context; }

      final boolean canCreateGLPbuffer() { return canCreateGLPbuffer; }
      final boolean isReadDrawableAvailable() { return readDrawableAvailable; }
  }

  class SharedResourceImplementation implements SharedResourceRunner.Implementation {
        public void clear() {
            synchronized(sharedMap) {
                sharedMap.clear();
            }
        }
        public SharedResourceRunner.Resource mapPut(String connection, SharedResourceRunner.Resource resource) {
            synchronized(sharedMap) {
                return (SharedResourceRunner.Resource) sharedMap.put(connection, resource);
            }
        }
        public SharedResourceRunner.Resource mapGet(String connection) {
            synchronized(sharedMap) {
                return (SharedResourceRunner.Resource) sharedMap.get(connection);
            }
        }
        public Collection/*<Resource>*/ mapValues() {
            synchronized(sharedMap) {
                return sharedMap.values();
            }
        }

        public SharedResourceRunner.Resource createSharedResource(String connection) {
            WindowsGraphicsDevice sharedDevice = new WindowsGraphicsDevice(connection, AbstractGraphicsDevice.DEFAULT_UNIT);
            sharedDevice.lock();
            try {
                AbstractGraphicsScreen absScreen = new DefaultGraphicsScreen(sharedDevice, 0);
                if (null == absScreen) {
                    throw new GLException("Couldn't create shared screen for device: "+sharedDevice+", idx 0");
                }
                GLProfile glp = GLProfile.getDefault(sharedDevice);
                if (null == glp) {
                    throw new GLException("Couldn't get default GLProfile for device: "+sharedDevice);
                }
                WindowsDummyWGLDrawable sharedDrawable = WindowsDummyWGLDrawable.create(WindowsWGLDrawableFactory.this, glp, absScreen);
                if (null == sharedDrawable) {
                    throw new GLException("Couldn't create shared drawable for screen: "+absScreen+", "+glp);
                }
                WindowsWGLContext sharedContext  = (WindowsWGLContext) sharedDrawable.createContext(null);
                if (null == sharedContext) {
                    throw new GLException("Couldn't create shared context for drawable: "+sharedDrawable);
                }
                sharedContext.setSynchronized(true);
                boolean canCreateGLPbuffer;
                boolean readDrawableAvailable;
                sharedContext.makeCurrent();
                try {
                    canCreateGLPbuffer = sharedContext.getGL().isExtensionAvailable(GL_ARB_pbuffer);
                    readDrawableAvailable = sharedContext.isExtensionAvailable(WGL_ARB_make_current_read) &&
                                            sharedContext.isFunctionAvailable(wglMakeContextCurrent);
                } finally {
                    sharedContext.release();
                }
                if (DEBUG) {
                    System.err.println("!!! SharedDevice:  " + sharedDevice);
                    System.err.println("!!! SharedScreen:  " + absScreen);
                    System.err.println("!!! SharedContext: " + sharedContext);
                    System.err.println("!!! pbuffer avail: " + canCreateGLPbuffer);
                    System.err.println("!!! readDrawable:  " + readDrawableAvailable);
                }
                return new SharedResource(sharedDevice, absScreen, sharedDrawable, sharedContext, readDrawableAvailable, canCreateGLPbuffer);
            } catch (Throwable t) {
                throw new GLException("WindowsWGLDrawableFactory - Could not initialize shared resources for "+connection, t);
            } finally {
                sharedDevice.unlock();
            }
        }

        public void releaseSharedResource(SharedResourceRunner.Resource shared) {
            SharedResource sr = (SharedResource) shared;
            if (DEBUG) {
              System.err.println("!!! Shutdown Shared:");
              System.err.println("!!!   Device  : " + sr.device);
              System.err.println("!!!   Screen  : " + sr.screen);
              System.err.println("!!!   Drawable: " + sr.drawable);
              System.err.println("!!!   CTX     : " + sr.context);
            }

            if (null != sr.context) {
                // may cause JVM SIGSEGV: sharedContext.destroy();
                sr.context = null;
            }

            if (null != sr.drawable) {
                sr.drawable.destroy();
                sr.drawable = null;
            }

            if (null != sr.screen) {
                sr.screen = null;
            }

            if (null != sr.device) {
                sr.device.close();
                sr.device = null;
            }
        }
  }

  public final AbstractGraphicsDevice getDefaultDevice() {
      return defaultDevice;
  }

  public final boolean getIsDeviceCompatible(AbstractGraphicsDevice device) {
      if(device instanceof WindowsGraphicsDevice) {
          return true;
      }
      return false;
  }

  final static String GL_ARB_pbuffer = "GL_ARB_pbuffer";
  final static String WGL_ARB_make_current_read = "WGL_ARB_make_current_read";
  final static String wglMakeContextCurrent = "wglMakeContextCurrent";

  protected final GLContext getSharedContextImpl(AbstractGraphicsDevice device) {
    SharedResourceRunner.Resource sr = sharedResourceRunner.getShared(device);
    if(null!=sr) {
      return sr.getContext();
    }
    return null;
  }

  protected final boolean hasSharedContextImpl(AbstractGraphicsDevice device) {
      return null != getSharedContextImpl(device);
  }

  protected final GLContext getOrCreateSharedContextImpl(AbstractGraphicsDevice device) {
    SharedResourceRunner.Resource sr = sharedResourceRunner.getOrCreateShared(device);
    if(null!=sr) {
      return sr.getContext();
    }
    return null;
  }

  protected AbstractGraphicsDevice getOrCreateSharedDeviceImpl(AbstractGraphicsDevice device) {
    SharedResourceRunner.Resource sr = sharedResourceRunner.getOrCreateShared(device);
    if(null!=sr) {
        return sr.getDevice();
    }
    return null;
  }

  protected WindowsWGLDrawable getOrCreateSharedDrawable(AbstractGraphicsDevice device) {
    SharedResourceRunner.Resource sr = sharedResourceRunner.getOrCreateShared(device);
    if(null!=sr) {
        return (WindowsWGLDrawable) sr.getDrawable();
    }
    return null;
  }

  SharedResource getOrCreateSharedResource(AbstractGraphicsDevice device) {
    return (SharedResource) sharedResourceRunner.getOrCreateShared(device);
  }

  protected final void shutdownInstance() {
    sharedResourceRunner.releaseAndWait();
    RegisteredClassFactory.shutdownSharedClasses();
  }

  protected List/*GLCapabilitiesImmutable*/ getAvailableCapabilitiesImpl(AbstractGraphicsDevice device) {
    return WindowsWGLGraphicsConfigurationFactory.getAvailableCapabilities(this, device);
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

    final SharedResource sr = (SharedResource) sharedResourceRunner.getOrCreateShared(device);
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
      };
    maybeDoSingleThreadedWorkaround(r);
    return (GLDrawableImpl) returnList.get(0);
  }

  /**
   * @return 1 if read drawable extension is available, 0 if not
   *           and -1 if undefined yet, ie no shared device exist at this point.
   */
  public final int isReadDrawableAvailable(AbstractGraphicsDevice device) {
    SharedResource sr = (SharedResource) sharedResourceRunner.getOrCreateShared((null!=device)?device:defaultDevice);
    if(null!=sr) {
        return sr.isReadDrawableAvailable() ? 1 : 0 ;
    }
    return -1; // undefined
  }

  public final boolean canCreateGLPbuffer(AbstractGraphicsDevice device) {
    SharedResource sr = (SharedResource) sharedResourceRunner.getOrCreateShared((null!=device)?device:defaultDevice);
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
