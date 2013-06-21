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

import java.nio.Buffer;

import java.nio.ShortBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import jogamp.nativewindow.WrappedSurface;
import jogamp.nativewindow.windows.GDI;
import jogamp.nativewindow.windows.GDIDummyUpstreamSurfaceHook;
import jogamp.nativewindow.windows.GDISurface;
import jogamp.nativewindow.windows.RegisteredClassFactory;
import jogamp.opengl.DesktopGLDynamicLookupHelper;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.GLGraphicsConfigurationUtil;
import jogamp.opengl.SharedResourceRunner;

import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.windows.WindowsGraphicsDevice;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.GLRendererQuirks;

public class WindowsWGLDrawableFactory extends GLDrawableFactoryImpl {
  private static DesktopGLDynamicLookupHelper windowsWGLDynamicLookupHelper = null;

  public WindowsWGLDrawableFactory() {
    super();

    synchronized(WindowsWGLDrawableFactory.class) {
        if( null == windowsWGLDynamicLookupHelper ) {
            windowsWGLDynamicLookupHelper = AccessController.doPrivileged(new PrivilegedAction<DesktopGLDynamicLookupHelper>() {
                public DesktopGLDynamicLookupHelper run() {
                    DesktopGLDynamicLookupHelper tmp;
                    try {
                        tmp = new DesktopGLDynamicLookupHelper(new WindowsWGLDynamicLibraryBundleInfo());
                        if(null!=tmp && tmp.isLibComplete()) {
                            WGL.getWGLProcAddressTable().reset(tmp);
                        }
                    } catch (Exception ex) {
                        tmp = null;
                        if(DEBUG) {
                            ex.printStackTrace();
                        }
                    }
                    return tmp;
                }
            } );
        }
    }

    defaultDevice = new WindowsGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);

    if(null!=windowsWGLDynamicLookupHelper) {
        // Register our GraphicsConfigurationFactory implementations
        // The act of constructing them causes them to be registered
        WindowsWGLGraphicsConfigurationFactory.registerFactory();
        if(GLProfile.isAWTAvailable()) {
            try {
              ReflectionUtil.callStaticMethod("jogamp.opengl.windows.wgl.awt.WindowsAWTWGLGraphicsConfigurationFactory",
                                              "registerFactory", null, null, getClass().getClassLoader());
            } catch (Exception jre) { /* n/a .. */ }
        }

        sharedMap = new HashMap<String, SharedResourceRunner.Resource>();

        // Init shared resources off thread
        // Will be released via ShutdownHook
        sharedResourceRunner = new SharedResourceRunner(new SharedResourceImplementation());
        sharedResourceRunner.start();
    }
  }

  @Override
  protected final boolean isComplete() {
      return null != windowsWGLDynamicLookupHelper;
  }
  
  
  @Override
  protected final void destroy() {
    if(null != sharedResourceRunner) {
        sharedResourceRunner.stop();
        sharedResourceRunner = null;
    }
    if(null != sharedMap) {
        sharedMap.clear();
        sharedMap = null;
    }
    defaultDevice = null;
    /**
     * Pulling away the native library may cause havoc ..
     *
       windowsWGLDynamicLookupHelper.destroy();
     */
    windowsWGLDynamicLookupHelper = null;

    RegisteredClassFactory.shutdownSharedClasses();
  }

  @Override
  public GLDynamicLookupHelper getGLDynamicLookupHelper(int profile) {
      return windowsWGLDynamicLookupHelper;
  }

  private WindowsGraphicsDevice defaultDevice;
  private SharedResourceRunner sharedResourceRunner;
  private HashMap<String /*connection*/, SharedResourceRunner.Resource> sharedMap;

  private long processAffinityChanges = 0;
  private PointerBuffer procMask = PointerBuffer.allocateDirect(1);
  private PointerBuffer sysMask = PointerBuffer.allocateDirect(1);

  @Override
  protected void enterThreadCriticalZone() {
    synchronized (sysMask) {
        if( 0 == processAffinityChanges) {
            long pid = GDI.GetCurrentProcess();
            if ( GDI.GetProcessAffinityMask(pid, procMask, sysMask) ) {
                if(DEBUG) {
                    System.err.println("WindowsWGLDrawableFactory.enterThreadCriticalZone() - 0x" + Long.toHexString(pid) + " - " + getThreadName());
                    // Thread.dumpStack();
                }
                processAffinityChanges = pid;
                GDI.SetProcessAffinityMask(pid, 1);
            }
        }
    }
  }

  @Override
  protected void leaveThreadCriticalZone() {
    synchronized (sysMask) {
        if( 0 != processAffinityChanges) {
            long pid = GDI.GetCurrentProcess();
            if( pid != processAffinityChanges) {
                throw new GLException("PID doesn't match: set PID 0x" + Long.toHexString(processAffinityChanges) +
                                                       " this PID 0x" + Long.toHexString(pid) );
            }
            if(DEBUG) {
                System.err.println("WindowsWGLDrawableFactory.leaveThreadCriticalZone() - 0x" + Long.toHexString(pid) + " - " + getThreadName());
            }
            GDI.SetProcessAffinityMask(pid, sysMask.get(0));
        }
    }
  }

  static class SharedResource implements SharedResourceRunner.Resource {
      private final boolean hasARBPixelFormat;
      private final boolean hasARBMultisample;
      private final boolean hasARBPBuffer;
      private final boolean hasARBReadDrawable;
      private WindowsGraphicsDevice device;
      private AbstractGraphicsScreen screen;
      private GLDrawableImpl drawable;
      private GLContextImpl context;
      
      SharedResource(WindowsGraphicsDevice dev, AbstractGraphicsScreen scrn, GLDrawableImpl draw, GLContextImpl ctx,
                     boolean arbPixelFormat, boolean arbMultisample, boolean arbPBuffer, boolean arbReadDrawable) {
          device = dev;
          screen = scrn;
          drawable = draw;
          context = ctx;
          hasARBPixelFormat = arbPixelFormat;
          hasARBMultisample = arbMultisample;
          hasARBPBuffer = arbPBuffer;
          hasARBReadDrawable = arbReadDrawable;
      }

      @Override
      public final boolean isValid() {
          return null != context;
      }
      @Override
      final public AbstractGraphicsDevice getDevice() { return device; }
      @Override
      final public AbstractGraphicsScreen getScreen() { return screen; }
      @Override
      final public GLDrawableImpl getDrawable() { return drawable; }
      @Override
      final public GLContextImpl getContext() { return context; }
      @Override
      public GLRendererQuirks getRendererQuirks() {
          return null != context ? context.getRendererQuirks() : null;      
      }

      final boolean hasARBPixelFormat() { return hasARBPixelFormat; }
      final boolean hasARBMultisample() { return hasARBMultisample; }
      final boolean hasARBPBuffer() { return hasARBPBuffer; }
      final boolean hasReadDrawable() { return hasARBReadDrawable; }
  }

  class SharedResourceImplementation implements SharedResourceRunner.Implementation {
        @Override
        public void clear() {
            synchronized(sharedMap) {
                sharedMap.clear();
            }
        }
        @Override
        public SharedResourceRunner.Resource mapPut(String connection, SharedResourceRunner.Resource resource) {
            synchronized(sharedMap) {
                return sharedMap.put(connection, resource);
            }
        }
        @Override
        public SharedResourceRunner.Resource mapGet(String connection) {
            synchronized(sharedMap) {
                return sharedMap.get(connection);
            }
        }
        @Override
        public Collection<SharedResourceRunner.Resource> mapValues() {
            synchronized(sharedMap) {
                return sharedMap.values();
            }
        }

        @Override
        public boolean isDeviceSupported(String connection) {
            return true;
        }
        
        @Override
        public SharedResourceRunner.Resource createSharedResource(String connection) {
            final WindowsGraphicsDevice sharedDevice = new WindowsGraphicsDevice(connection, AbstractGraphicsDevice.DEFAULT_UNIT);
            sharedDevice.lock();
            try {
                final AbstractGraphicsScreen absScreen = new DefaultGraphicsScreen(sharedDevice, 0);
                final GLProfile glp = GLProfile.get(sharedDevice, GLProfile.GL_PROFILE_LIST_MIN_DESKTOP, false);
                if (null == glp) {
                    throw new GLException("Couldn't get default GLProfile for device: "+sharedDevice);
                }
                final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
                final GLDrawableImpl sharedDrawable = createOnscreenDrawableImpl(createDummySurfaceImpl(sharedDevice, false, caps, caps, null, 64, 64));
                sharedDrawable.setRealized(true);
                                
                final GLContextImpl sharedContext  = (GLContextImpl) sharedDrawable.createContext(null);
                if (null == sharedContext) {
                    throw new GLException("Couldn't create shared context for drawable: "+sharedDrawable);
                }
                boolean hasARBPixelFormat;
                boolean hasARBMultisample;
                boolean hasARBPBuffer;
                boolean hasARBReadDrawableAvailable;
                sharedContext.makeCurrent();
                try {
                    hasARBPixelFormat = sharedContext.isExtensionAvailable(WGL_ARB_pixel_format);
                    hasARBMultisample = sharedContext.isExtensionAvailable(WGL_ARB_multisample);
                    hasARBPBuffer = sharedContext.isExtensionAvailable(GLExtensions.ARB_pbuffer);
                    hasARBReadDrawableAvailable = sharedContext.isExtensionAvailable(WGL_ARB_make_current_read) &&
                                            sharedContext.isFunctionAvailable(wglMakeContextCurrent);
                } finally {
                    sharedContext.release();
                }
                if (DEBUG) {
                    System.err.println("SharedDevice:  " + sharedDevice);
                    System.err.println("SharedScreen:  " + absScreen);
                    System.err.println("SharedContext: " + sharedContext);
                    System.err.println("pixelformat:   " + hasARBPixelFormat);
                    System.err.println("multisample:   " + hasARBMultisample);
                    System.err.println("pbuffer:       " + hasARBPBuffer);
                    System.err.println("readDrawable:  " + hasARBReadDrawableAvailable);
                }
                return new SharedResource(sharedDevice, absScreen, sharedDrawable, sharedContext,
                                          hasARBPixelFormat, hasARBMultisample,
                                          hasARBPBuffer, hasARBReadDrawableAvailable);
            } catch (Throwable t) {
                throw new GLException("WindowsWGLDrawableFactory - Could not initialize shared resources for "+connection, t);
            } finally {
                sharedDevice.unlock();
            }
        }

        @Override
        public void releaseSharedResource(SharedResourceRunner.Resource shared) {
            SharedResource sr = (SharedResource) shared;
            if (DEBUG) {
              System.err.println("Shutdown Shared:");
              System.err.println("Device  : " + sr.device);
              System.err.println("Screen  : " + sr.screen);
              System.err.println("Drawable: " + sr.drawable);
              System.err.println("CTX     : " + sr.context);
            }

            if (null != sr.context) {
                // may cause JVM SIGSEGV: sharedContext.destroy();
                sr.context = null;
            }

            if (null != sr.drawable) {
                sr.drawable.setRealized(false);
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

  @Override
  public final AbstractGraphicsDevice getDefaultDevice() {
      return defaultDevice;
  }

  @Override
  public final boolean getIsDeviceCompatible(AbstractGraphicsDevice device) {
      if(null!=windowsWGLDynamicLookupHelper && device instanceof WindowsGraphicsDevice) {
          return true;
      }
      return false;
  }

  final static String WGL_ARB_pbuffer      = "WGL_ARB_pbuffer";
  final static String WGL_ARB_pixel_format = "WGL_ARB_pixel_format";
  final static String WGL_ARB_multisample = "WGL_ARB_multisample";
  final static String WGL_NV_float_buffer = "WGL_NV_float_buffer";
  final static String WGL_ARB_make_current_read = "WGL_ARB_make_current_read";
  final static String wglMakeContextCurrent = "wglMakeContextCurrent";

  @Override
  protected final Thread getSharedResourceThread() {
    return sharedResourceRunner.start();
  }

  @Override
  protected final SharedResource getOrCreateSharedResourceImpl(AbstractGraphicsDevice device) {
    return (SharedResource) sharedResourceRunner.getOrCreateShared(device);
  }

  protected final WindowsWGLDrawable getOrCreateSharedDrawable(AbstractGraphicsDevice device) {
    SharedResourceRunner.Resource sr = getOrCreateSharedResourceImpl(device);
    if(null!=sr) {
        return (WindowsWGLDrawable) sr.getDrawable();
    }
    return null;
  }

  @Override
  protected List<GLCapabilitiesImmutable> getAvailableCapabilitiesImpl(AbstractGraphicsDevice device) {
    return WindowsWGLGraphicsConfigurationFactory.getAvailableCapabilities(this, device);
  }

  @Override
  protected final GLDrawableImpl createOnscreenDrawableImpl(NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new WindowsOnscreenWGLDrawable(this, target);
  }

  @Override
  protected final GLDrawableImpl createOffscreenDrawableImpl(final NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    AbstractGraphicsConfiguration config = target.getGraphicsConfiguration();
    GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    if(!chosenCaps.isPBuffer()) {
        return WindowsBitmapWGLDrawable.create(this, target);
    }

    // PBuffer GLDrawable Creation
    GLDrawableImpl pbufferDrawable;
    final AbstractGraphicsDevice device = config.getScreen().getDevice();

    /**
     * Similar to ATI Bug https://bugzilla.mozilla.org/show_bug.cgi?id=486277,
     * we need to have a context current on the same Display to create a PBuffer.
     */
    final SharedResource sr = getOrCreateSharedResourceImpl(device);
    if(null!=sr) {
        GLContext lastContext = GLContext.getCurrent();
        if (lastContext != null) {
            lastContext.release();
        }
        sr.context.makeCurrent();
        try {
            pbufferDrawable = new WindowsPbufferWGLDrawable(WindowsWGLDrawableFactory.this, target);
        } finally {
            sr.context.release();
            if (lastContext != null) {
              lastContext.makeCurrent();
            }
        }
    } else {
        pbufferDrawable = new WindowsPbufferWGLDrawable(WindowsWGLDrawableFactory.this, target);
    }
    return pbufferDrawable;
  }

  /**
   * @return 1 if read drawable extension is available, 0 if not
   *           and -1 if undefined yet, ie no shared device exist at this point.
   */
  public final int isReadDrawableAvailable(AbstractGraphicsDevice device) {
    SharedResource sr = getOrCreateSharedResourceImpl( ( null != device ) ? device : defaultDevice );
    if(null!=sr) {
        return sr.hasReadDrawable() ? 1 : 0 ;
    }
    return -1; // undefined
  }

  @Override
  public final boolean canCreateGLPbuffer(AbstractGraphicsDevice device) {
    SharedResource sr = getOrCreateSharedResourceImpl( ( null != device ) ? device : defaultDevice );
    if(null!=sr) {
        return sr.hasARBPBuffer();
    }
    return false;
  }

  @Override
  protected final ProxySurface createMutableSurfaceImpl(AbstractGraphicsDevice deviceReq, boolean createNewDevice, 
                                                        GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested, 
                                                        GLCapabilitiesChooser chooser, UpstreamSurfaceHook upstreamHook) {
    final WindowsGraphicsDevice device;
    if(createNewDevice || !(deviceReq instanceof WindowsGraphicsDevice)) {
        device = new WindowsGraphicsDevice(deviceReq.getConnection(), deviceReq.getUnitID());
    } else {
        device = (WindowsGraphicsDevice)deviceReq;
    }
    final AbstractGraphicsScreen screen = new DefaultGraphicsScreen(device, 0);
    final WindowsWGLGraphicsConfiguration config = WindowsWGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, screen);
    if(null == config) {
        throw new GLException("Choosing GraphicsConfiguration failed w/ "+capsChosen+" on "+screen); 
    }    
    return new WrappedSurface(config, 0, upstreamHook, createNewDevice);
  }

  @Override
  public final ProxySurface createDummySurfaceImpl(AbstractGraphicsDevice deviceReq, boolean createNewDevice, 
                                                   GLCapabilitiesImmutable chosenCaps, GLCapabilitiesImmutable requestedCaps, GLCapabilitiesChooser chooser, int width, int height) {
    final WindowsGraphicsDevice device;
    if( createNewDevice || !(deviceReq instanceof WindowsGraphicsDevice) ) {
        device = new WindowsGraphicsDevice(deviceReq.getConnection(), deviceReq.getUnitID());
    } else {
        device = (WindowsGraphicsDevice)deviceReq;
    }
    final AbstractGraphicsScreen screen = new DefaultGraphicsScreen(device, 0);
    chosenCaps = GLGraphicsConfigurationUtil.fixOnscreenGLCapabilities(chosenCaps);
    final WindowsWGLGraphicsConfiguration config = WindowsWGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(chosenCaps, requestedCaps, chooser, screen);
    if(null == config) { 
        throw new GLException("Choosing GraphicsConfiguration failed w/ "+chosenCaps+" on "+screen);
    }    
    return new GDISurface(config, 0, new GDIDummyUpstreamSurfaceHook(width, height), createNewDevice);
  }    
  
  @Override
  protected final ProxySurface createProxySurfaceImpl(AbstractGraphicsDevice deviceReq, int screenIdx, long windowHandle, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser, UpstreamSurfaceHook upstream) {
    final WindowsGraphicsDevice device = new WindowsGraphicsDevice(deviceReq.getConnection(), deviceReq.getUnitID());
    final AbstractGraphicsScreen screen = new DefaultGraphicsScreen(device, screenIdx);
    final WindowsWGLGraphicsConfiguration cfg = WindowsWGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsRequested, capsRequested, chooser, screen);
    return new GDISurface(cfg, windowHandle, upstream, true);
  }

  @Override
  protected final GLContext createExternalGLContextImpl() {
    return WindowsExternalWGLContext.create(this, null);
  }

  @Override
  public final boolean canCreateExternalGLDrawable(AbstractGraphicsDevice device) {
    return true;
  }

  @Override
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

  //------------------------------------------------------
  // Gamma-related functionality
  //

  private static final int GAMMA_RAMP_LENGTH = 256;

  @Override
  protected final int getGammaRampLength() {
    return GAMMA_RAMP_LENGTH;
  }

  @Override
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

  @Override
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

  @Override
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
