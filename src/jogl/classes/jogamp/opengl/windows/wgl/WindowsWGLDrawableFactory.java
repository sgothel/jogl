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
import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.DefaultGraphicsScreen;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.WrappedSurface;
import jogamp.nativewindow.windows.GDI;
import jogamp.nativewindow.windows.GDIDummyUpstreamSurfaceHook;
import jogamp.nativewindow.windows.GDISurface;
import jogamp.nativewindow.windows.RegisteredClassFactory;
import jogamp.opengl.Debug;
import jogamp.opengl.DesktopGLDynamicLookupHelper;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.GLGraphicsConfigurationUtil;
import jogamp.opengl.SharedResourceRunner;

import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.GenericUpstreamSurfacelessHook;
import com.jogamp.nativewindow.windows.WindowsGraphicsDevice;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.GLRendererQuirks;

public class WindowsWGLDrawableFactory extends GLDrawableFactoryImpl {
  private static final boolean DEBUG_SHAREDCTX = DEBUG  || GLContext.DEBUG;

  /**
   * Bug 1036: NVidia Windows Driver 'Threaded optimization' workaround.
   * <p>
   * https://jogamp.org/bugzilla/show_bug.cgi?id=1036
   * </p>
   * <p>
   * Since NV driver 260.99 from 2010-12-11 a 'Threaded optimization' feature has been introduced.
   * The driver spawns off a dedicated thread to off-load certain OpenGL tasks from the calling thread
   * to perform them async and off-thread.
   * </p>
   * <p>
   * If 'Threaded optimization' is manually enabled 'on', the driver may crash with JOGL's consistent
   * multi-threaded usage - this is a driver bug.
   * </p>
   * <p>
   * If 'Threaded optimization' is manually disabled 'off', the driver always works correctly.
   * </p>
   * <p>
   * 'Threaded optimization' default setting is 'auto' and the driver may crash without this workaround.
   * </p>
   * <p>
   * If setting the process affinity to '1' (1st CPU) while initialization and launching
   * the  {@link SharedResourceRunner}, the driver does not crash anymore in 'auto' mode.
   * This might be either because the driver does not enable 'Threaded optimization'
   * or because the driver's worker thread is bound to the same CPU.
   * </p>
   * <p>
   * Property integer value <code>jogl.windows.cpu_affinity_mode</code>:
   * <ul>
   *   <li>0 - none (no affinity, may cause driver crash with 'Threaded optimization' = ['auto', 'on'])</li>
   *   <li>1 - process affinity (default, workaround for driver crash for 'Threaded optimization' = 'auto', still crashes if set to 'on')</li>
   * </ul>
   * </p>
   * <p>
   * Test case reproducing the crash reliable is: com.jogamp.opengl.test.junit.jogl.caps.TestTranslucencyNEWT<br>
   * (don't ask why ..)
   * </p>
   */
  private static final int CPU_AFFINITY_MODE;

  static {
      Debug.initSingleton();
      CPU_AFFINITY_MODE = PropertyAccess.getIntProperty("jogl.windows.cpu_affinity_mode", true, 1);
  }

  private static DesktopGLDynamicLookupHelper windowsWGLDynamicLookupHelper = null;

  private final CPUAffinity cpuAffinity;

  public WindowsWGLDrawableFactory() {
    super();

    switch( CPU_AFFINITY_MODE ) {
        case 0:
            cpuAffinity = new NopCPUAffinity();
            break;
        /**
         * Doesn't work !
        case 2:
            cpuAffinity = new WindowsThreadAffinity();
            break;
         */
        default:
            cpuAffinity = new WindowsProcessAffinity();
            break;
    }

    synchronized(WindowsWGLDrawableFactory.class) {
        if( null == windowsWGLDynamicLookupHelper ) {
            windowsWGLDynamicLookupHelper = AccessController.doPrivileged(new PrivilegedAction<DesktopGLDynamicLookupHelper>() {
                @Override
                public DesktopGLDynamicLookupHelper run() {
                    DesktopGLDynamicLookupHelper tmp;
                    try {
                        tmp = new DesktopGLDynamicLookupHelper(new WindowsWGLDynamicLibraryBundleInfo());
                        if(null!=tmp && tmp.isLibComplete()) {
                            WGL.getWGLProcAddressTable().reset(tmp);
                        }
                    } catch (final Exception ex) {
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
            } catch (final Exception jre) { /* n/a .. */ }
        }

        // Init shared resources off thread
        // Will be released via ShutdownHook
        sharedResourceImplementation = new SharedResourceImplementation();
        sharedResourceRunner = new SharedResourceRunner(sharedResourceImplementation);
        sharedResourceRunner.start();
    }
  }

  @Override
  protected final boolean isComplete() {
      return null != windowsWGLDynamicLookupHelper;
  }


  @Override
  protected final void shutdownImpl() {
    if( DEBUG ) {
        System.err.println("WindowsWGLDrawableFactory.shutdown");
    }
    if(null != sharedResourceRunner) {
        sharedResourceRunner.stop();
        sharedResourceRunner = null;
    }
    if(null != sharedResourceImplementation) {
        sharedResourceImplementation.clear();
        sharedResourceImplementation = null;
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
  public final GLDynamicLookupHelper getGLDynamicLookupHelper(final int majorVersion, final int contextOptions) {
      return windowsWGLDynamicLookupHelper;
  }

  /* pp */ static String toHexString(final long l) { return "0x"+Long.toHexString(l); }

  private WindowsGraphicsDevice defaultDevice;
  private SharedResourceImplementation sharedResourceImplementation;
  private SharedResourceRunner sharedResourceRunner;

  @Override
  protected void enterThreadCriticalZone() {
    synchronized (cpuAffinity) {
        cpuAffinity.set(1);
    }
  }

  @Override
  protected void leaveThreadCriticalZone() {
    synchronized (cpuAffinity) {
        cpuAffinity.reset();
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

      SharedResource(final WindowsGraphicsDevice dev, final AbstractGraphicsScreen scrn, final GLDrawableImpl draw, final GLContextImpl ctx,
                     final boolean arbPixelFormat, final boolean arbMultisample, final boolean arbPBuffer, final boolean arbReadDrawable) {
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
      public final boolean isAvailable() {
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
      public GLRendererQuirks getRendererQuirks(final GLProfile glp) {
          return null != context ? context.getRendererQuirks() : null;
      }

      final boolean hasARBPixelFormat() { return hasARBPixelFormat; }
      final boolean hasARBMultisample() { return hasARBMultisample; }
      final boolean hasARBPBuffer() { return hasARBPBuffer; }
      final boolean hasReadDrawable() { return hasARBReadDrawable; }
  }

  class SharedResourceImplementation extends SharedResourceRunner.AImplementation {
        @Override
        public boolean isDeviceSupported(final AbstractGraphicsDevice device) {
            return true;
        }

        @Override
        public SharedResourceRunner.Resource createSharedResource(final AbstractGraphicsDevice adevice) {
            final WindowsGraphicsDevice device = new WindowsGraphicsDevice(adevice.getConnection(), adevice.getUnitID());
            GLContextImpl context = null;
            boolean contextIsCurrent = false;
            device.lock();
            try {
                final AbstractGraphicsScreen absScreen = new DefaultGraphicsScreen(device, 0);
                final GLProfile glp = GLProfile.get(device, GLProfile.GL_PROFILE_LIST_MIN_DESKTOP, false);
                if (null == glp) {
                    throw new GLException("Couldn't get default GLProfile for device: "+device);
                }
                final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
                final GLDrawableImpl drawable = createOnscreenDrawableImpl(createDummySurfaceImpl(device, false, caps, caps, null, 64, 64));
                drawable.setRealized(true);

                context  = (GLContextImpl) drawable.createContext(null);
                if (null == context) {
                    throw new GLException("Couldn't create shared context for drawable: "+drawable);
                }
                contextIsCurrent = GLContext.CONTEXT_NOT_CURRENT != context.makeCurrent();

                final boolean allowsSurfacelessCtx;
                final boolean hasARBPixelFormat;
                final boolean hasARBMultisample;
                final boolean hasARBPBuffer;
                final boolean hasARBReadDrawableAvailable;
                if( contextIsCurrent ) {
                    if( context.getGLVersionNumber().compareTo(GLContext.Version3_0) >= 0 ) {
                        allowsSurfacelessCtx = probeSurfacelessCtx(context, true /* restoreDrawable */);
                    } else {
                        setNoSurfacelessCtxQuirk(context);
                        allowsSurfacelessCtx = false;
                    }
                    hasARBPixelFormat = context.isExtensionAvailable(WGL_ARB_pixel_format);
                    hasARBMultisample = context.isExtensionAvailable(WGL_ARB_multisample);
                    hasARBPBuffer = context.isExtensionAvailable(GLExtensions.ARB_pbuffer);
                    hasARBReadDrawableAvailable = context.isExtensionAvailable(WGL_ARB_make_current_read) &&
                                                  context.isFunctionAvailable(wglMakeContextCurrent);
                } else {
                    allowsSurfacelessCtx = false;
                    hasARBPixelFormat = false;
                    hasARBMultisample = false;
                    hasARBPBuffer = false;
                    hasARBReadDrawableAvailable = false;
                }
                if ( DEBUG_SHAREDCTX ) {
                    System.err.println("SharedDevice:  " + device);
                    System.err.println("SharedScreen:  " + absScreen);
                    System.err.println("SharedContext: " + context + ", madeCurrent " + contextIsCurrent);
                    System.err.println("  allowsSurfacelessCtx "+allowsSurfacelessCtx);
                    System.err.println("pixelformat:   " + hasARBPixelFormat);
                    System.err.println("multisample:   " + hasARBMultisample);
                    System.err.println("pbuffer:       " + hasARBPBuffer);
                    System.err.println("readDrawable:  " + hasARBReadDrawableAvailable);
                }
                return new SharedResource(device, absScreen, drawable, context,
                                          hasARBPixelFormat, hasARBMultisample,
                                          hasARBPBuffer, hasARBReadDrawableAvailable);
            } catch (final Throwable t) {
                throw new GLException("WindowsWGLDrawableFactory - Could not initialize shared resources for "+adevice, t);
            } finally {
                if ( contextIsCurrent ) {
                    context.release();
                }
                device.unlock();
            }
        }

        @Override
        public void releaseSharedResource(final SharedResourceRunner.Resource shared) {
            final SharedResource sr = (SharedResource) shared;
            if ( DEBUG_SHAREDCTX ) {
              System.err.println("Shutdown Shared:");
              System.err.println("Device  : " + sr.device);
              System.err.println("Screen  : " + sr.screen);
              System.err.println("Drawable: " + sr.drawable);
              System.err.println("CTX     : " + sr.context);
            }

            if (null != sr.context) {
                // may cause JVM SIGSEGV: sharedContext.destroy();
                sr.context.destroy(); // will also pull the dummy MutableSurface
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
  public final boolean getIsDeviceCompatible(final AbstractGraphicsDevice device) {
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
  protected final SharedResource getOrCreateSharedResourceImpl(final AbstractGraphicsDevice device) {
    return (SharedResource) sharedResourceRunner.getOrCreateShared(device);
  }

  protected final WindowsWGLDrawable getOrCreateSharedDrawable(final AbstractGraphicsDevice device) {
    final SharedResourceRunner.Resource sr = getOrCreateSharedResourceImpl(device);
    if(null!=sr) {
        return (WindowsWGLDrawable) sr.getDrawable();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * <p>
   * This factory always supports native desktop OpenGL profiles.
   * </p>
   */
  @Override
  public final boolean hasOpenGLDesktopSupport() { return true; }

  /**
   * {@inheritDoc}
   * <p>
   * This factory never supports native GLES profiles.
   * </p>
   */
  @Override
  public final boolean hasOpenGLESSupport() { return false; }

  /**
   * {@inheritDoc}
   * <p>
   * Always returns true.
   * </p>
   */
  @Override
  public final boolean hasMajorMinorCreateContextARB() { return true; }

  @Override
  protected List<GLCapabilitiesImmutable> getAvailableCapabilitiesImpl(final AbstractGraphicsDevice device) {
    return WindowsWGLGraphicsConfigurationFactory.getAvailableCapabilities(this, device);
  }

  @Override
  protected final GLDrawableImpl createOnscreenDrawableImpl(final NativeSurface target) {
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
    final AbstractGraphicsConfiguration config = target.getGraphicsConfiguration();
    final GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
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
        final GLContext lastContext = GLContext.getCurrent();
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
  public final int isReadDrawableAvailable(final AbstractGraphicsDevice device) {
    final SharedResource sr = getOrCreateSharedResourceImpl( ( null != device ) ? device : defaultDevice );
    if(null!=sr) {
        return sr.hasReadDrawable() ? 1 : 0 ;
    }
    return -1; // undefined
  }

  @Override
  public final boolean canCreateGLPbuffer(final AbstractGraphicsDevice device, final GLProfile glp) {
    final SharedResource sr = getOrCreateSharedResourceImpl( ( null != device ) ? device : defaultDevice );
    if(null!=sr) {
        return sr.hasARBPBuffer();
    }
    return false;
  }

  @Override
  protected final ProxySurface createMutableSurfaceImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                        final GLCapabilitiesImmutable capsChosen, final GLCapabilitiesImmutable capsRequested,
                                                        final GLCapabilitiesChooser chooser, final UpstreamSurfaceHook upstreamHook) {
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
  public final ProxySurface createDummySurfaceImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                   GLCapabilitiesImmutable chosenCaps, final GLCapabilitiesImmutable requestedCaps, final GLCapabilitiesChooser chooser, final int width, final int height) {
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
  public final ProxySurface createSurfacelessImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
          GLCapabilitiesImmutable chosenCaps, final GLCapabilitiesImmutable requestedCaps, final GLCapabilitiesChooser chooser, final int width, final int height) {
    chosenCaps = GLGraphicsConfigurationUtil.fixOnscreenGLCapabilities(chosenCaps);
    return createMutableSurfaceImpl(deviceReq, createNewDevice, chosenCaps, requestedCaps, chooser, new GenericUpstreamSurfacelessHook(width, height));
  }

  @Override
  protected final ProxySurface createProxySurfaceImpl(final AbstractGraphicsDevice deviceReq, final int screenIdx, final long windowHandle, final GLCapabilitiesImmutable capsRequested, final GLCapabilitiesChooser chooser, final UpstreamSurfaceHook upstream) {
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
  public final boolean canCreateExternalGLDrawable(final AbstractGraphicsDevice device) {
    return true;
  }

  @Override
  protected final GLDrawable createExternalGLDrawableImpl() {
    return WindowsExternalWGLDrawable.create(this, null);
  }

  static String wglGetLastError() {
    final long err = GDI.GetLastError();
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
  protected final int getGammaRampLength(final NativeSurface surface) {
    return GAMMA_RAMP_LENGTH;
  }

  @Override
  protected final boolean setGammaRamp(final NativeSurface surface, final float[] ramp) {
    final short[] rampData = new short[3 * GAMMA_RAMP_LENGTH];
    for (int i = 0; i < GAMMA_RAMP_LENGTH; i++) {
      final short scaledValue = (short) (ramp[i] * 65535);
      rampData[i] = scaledValue;
      rampData[i +     GAMMA_RAMP_LENGTH] = scaledValue;
      rampData[i + 2 * GAMMA_RAMP_LENGTH] = scaledValue;
    }

    final long hDC = surface.getSurfaceHandle();
    if( 0 == hDC ) {
        return false;
    }
    // final long screenDC = GDI.GetDC(0);
    final boolean res = GDI.SetDeviceGammaRamp(hDC, ShortBuffer.wrap(rampData));
    // GDI.ReleaseDC(0, screenDC);
    return res;
  }

  @Override
  protected final Buffer getGammaRamp(final NativeSurface surface) {
    final ShortBuffer rampData = ShortBuffer.wrap(new short[3 * GAMMA_RAMP_LENGTH]);
    final long hDC = surface.getSurfaceHandle();
    if( 0 == hDC ) {
        return null;
    }
    // final long screenDC = GDI.GetDC(0);
    final boolean res = GDI.GetDeviceGammaRamp(hDC, rampData);
    // GDI.ReleaseDC(0, screenDC);
    if (!res) {
      return null;
    }
    return rampData;
  }

  @Override
  protected final void resetGammaRamp(final NativeSurface surface, final Buffer originalGammaRamp) {
    if (originalGammaRamp == null) {
      // getGammaRamp failed earlier
      return;
    }
    final long hDC = surface.getSurfaceHandle();
    if( 0 == hDC ) {
        return;
    }
    // final long screenDC = GDI.GetDC(0);
    GDI.SetDeviceGammaRamp(hDC, originalGammaRamp);
    // GDI.ReleaseDC(0, hDC);
  }

  @Override
  protected final void resetGammaRamp(final DeviceScreenID deviceScreenID, final Buffer originalGammaRamp) {
    if (originalGammaRamp == null) {
      // getGammaRamp failed earlier
      return;
    }
    final long screenDC = GDI.GetDC(0);
    GDI.SetDeviceGammaRamp(screenDC, originalGammaRamp);
    GDI.ReleaseDC(0, screenDC);
  }


  static interface CPUAffinity {
      boolean set(final int newAffinity);
      boolean reset();
  }
  static final class WindowsThreadAffinity implements CPUAffinity {
      private long threadHandle;
      private long threadOrigAffinity;
      private long threadNewAffinity;
      public WindowsThreadAffinity() {
          threadHandle = 0;
          threadOrigAffinity = 0;
          threadNewAffinity = 0;
      }
      @Override
      public boolean set(final int newAffinity) {
          final long tid = GDI.GetCurrentThread();
          if( 0 != threadHandle ) {
              throw new IllegalStateException("Affinity already set");
          }
          final long threadLastAffinity = GDI.SetThreadAffinityMask(tid, newAffinity);
          final int werr = GDI.GetLastError();
          final boolean res;
          if( 0 != threadLastAffinity ) {
              res = true;
              this.threadHandle = tid;
              this.threadNewAffinity = newAffinity;
              this.threadOrigAffinity = threadLastAffinity;
          } else {
              res = false;
          }
          if(DEBUG) {
              System.err.println("WindowsThreadAffinity.set() - tid " + toHexString(tid) + " - " + getThreadName() +
                      ": OK "+res+" (werr "+werr+"), Affinity: "+toHexString(threadOrigAffinity) + " -> " + toHexString(newAffinity));
          }
          return res;
      }
      @Override
      public boolean reset() {
          if( 0 == threadHandle ) {
              return true;
          }
          final long tid = GDI.GetCurrentThread();
          if( tid != threadHandle) {
              throw new IllegalStateException("TID doesn't match: set TID " + toHexString(threadHandle) +
                                                               " this TID " + toHexString(tid) );
          }
          final long preThreadAffinity = GDI.SetThreadAffinityMask(threadHandle, threadOrigAffinity);
          final boolean res = 0 != preThreadAffinity;
          if(DEBUG) {
              System.err.println("WindowsThreadAffinity.reset() - tid " + toHexString(threadHandle) + " - " + getThreadName() +
                      ": OK "+res+" (werr "+GDI.GetLastError()+"), Affinity: "+toHexString(threadNewAffinity)+" -> orig "+ toHexString(threadOrigAffinity));
          }
          this.threadHandle = 0;
          this.threadNewAffinity = this.threadOrigAffinity;
          return res;
      }
  }
  static final class WindowsProcessAffinity implements CPUAffinity {
      private long processHandle;
      private long newAffinity;
      private final PointerBuffer procMask;
      private final PointerBuffer sysMask;

      public WindowsProcessAffinity() {
          processHandle = 0;
          newAffinity = 0;
          procMask = PointerBuffer.allocateDirect(1);
          sysMask = PointerBuffer.allocateDirect(1);
      }
      @Override
      public boolean set(final int newAffinity) {
          if( 0 != processHandle ) {
              throw new IllegalStateException("Affinity already set");
          }
          final long pid = GDI.GetCurrentProcess();
          final boolean res;
          if ( GDI.GetProcessAffinityMask(pid, procMask, sysMask) ) {
              if( GDI.SetProcessAffinityMask(pid, newAffinity) ) {
                  this.processHandle = pid;
                  this.newAffinity = newAffinity;
                  res = true;
              } else {
                  res = false;
              }
              if(DEBUG) {
                  System.err.println("WindowsProcessAffinity.set() - pid " + toHexString(pid) + " - " + getThreadName() +
                          ": OK "+res+" (werr "+GDI.GetLastError()+"), Affinity: procMask "+ toHexString(procMask.get(0)) + ", sysMask "+ toHexString(sysMask.get(0)) +
                          " -> "+toHexString(newAffinity));
              }
          } else {
              if(DEBUG) {
                  System.err.println("WindowsProcessAffinity.set() - pid " + toHexString(pid) + " - " + getThreadName() +
                          ": Error, could not GetProcessAffinityMask, werr "+GDI.GetLastError());
              }
              res = false;
          }
          return res;
      }
      @Override
      public boolean reset() {
          if( 0 == processHandle ) {
              return true;
          }
          final long pid = GDI.GetCurrentProcess();
          if( pid != processHandle) {
              throw new IllegalStateException("PID doesn't match: set PID " + toHexString(processHandle) +
                                                               " this PID " + toHexString(pid) );
          }
          final long origProcAffinity = procMask.get(0);
          final boolean res = GDI.SetProcessAffinityMask(processHandle, origProcAffinity);
          if(DEBUG) {
              final int werr = GDI.GetLastError();
              System.err.println("WindowsProcessAffinity.reset() - pid " + toHexString(processHandle) + " - " + getThreadName() +
                      ": OK "+res+" (werr "+werr+"), Affinity: "+toHexString(newAffinity)+" -> procMask "+ toHexString(origProcAffinity));
          }
          this.processHandle = 0;
          this.newAffinity = origProcAffinity;
          return res;
      }
  }
  static final class NopCPUAffinity implements CPUAffinity {
      public NopCPUAffinity() { }
      @Override
      public boolean set(final int newAffinity) {
          if(DEBUG) {
              System.err.println("NopCPUAffinity.set() - " + getThreadName());
          }
          return false;
      }
      @Override
      public boolean reset() {
          if(DEBUG) {
              System.err.println("NopCPUAffinity.reset() - " + getThreadName());
          }
          return false;
      }
  }
}
