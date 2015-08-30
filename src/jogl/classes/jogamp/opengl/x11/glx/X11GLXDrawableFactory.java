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
 */

package jogamp.opengl.x11.glx;

import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.WrappedSurface;
import jogamp.nativewindow.x11.X11DummyUpstreamSurfaceHook;
import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.X11Util;
import jogamp.opengl.DesktopGLDynamicLookupHelper;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.GLGraphicsConfigurationUtil;
import jogamp.opengl.SharedResourceRunner;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.nativewindow.GenericUpstreamSurfacelessHook;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;
import com.jogamp.opengl.GLRendererQuirks;

public class X11GLXDrawableFactory extends GLDrawableFactoryImpl {
  private static final boolean DEBUG_SHAREDCTX = DEBUG  || GLContext.DEBUG;

  public static final VersionNumber versionOneZero = new VersionNumber(1, 0, 0);
  public static final VersionNumber versionOneOne = new VersionNumber(1, 1, 0);
  public static final VersionNumber versionOneTwo = new VersionNumber(1, 2, 0);
  public static final VersionNumber versionOneThree = new VersionNumber(1, 3, 0);
  public static final VersionNumber versionOneFour = new VersionNumber(1, 4, 0);

  static final String GLX_SGIX_pbuffer = "GLX_SGIX_pbuffer";

  private static DesktopGLDynamicLookupHelper x11GLXDynamicLookupHelper = null;

  public X11GLXDrawableFactory() {
    super();

    synchronized(X11GLXDrawableFactory.class) {
        if( null == x11GLXDynamicLookupHelper ) {
            x11GLXDynamicLookupHelper = AccessController.doPrivileged(new PrivilegedAction<DesktopGLDynamicLookupHelper>() {
                @Override
                public DesktopGLDynamicLookupHelper run() {
                    DesktopGLDynamicLookupHelper tmp;
                    try {
                        tmp = new DesktopGLDynamicLookupHelper(new X11GLXDynamicLibraryBundleInfo());
                        if(null!=tmp && tmp.isLibComplete()) {
                            GLX.getGLXProcAddressTable().reset(tmp);
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

    defaultDevice = new X11GraphicsDevice(X11Util.getNullDisplayName(), AbstractGraphicsDevice.DEFAULT_UNIT);

    if(null!=x11GLXDynamicLookupHelper) {
        // Register our GraphicsConfigurationFactory implementations
        // The act of constructing them causes them to be registered
        X11GLXGraphicsConfigurationFactory.registerFactory();

        // Init shared resources off thread
        // Will be released via ShutdownHook
        sharedResourceImplementation = new SharedResourceImplementation();
        sharedResourceRunner = new SharedResourceRunner(sharedResourceImplementation);
        sharedResourceRunner.start();
    }
  }

  @Override
  protected final boolean isComplete() {
      return null != x11GLXDynamicLookupHelper;
  }

  @Override
  protected final void shutdownImpl() {
    if( DEBUG ) {
        System.err.println("X11GLXDrawableFactory.shutdown");
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
       x11GLXDynamicLookupHelper.destroy();
     */
    x11GLXDynamicLookupHelper = null;
  }

  @Override
  public final GLDynamicLookupHelper getGLDynamicLookupHelper(final int majorVersion, final int contextOptions) {
      return x11GLXDynamicLookupHelper;
  }

  private X11GraphicsDevice defaultDevice;
  private SharedResourceImplementation sharedResourceImplementation;
  private SharedResourceRunner sharedResourceRunner;

  static class SharedResource implements SharedResourceRunner.Resource {
      private final String glXServerVendorName;
      private final boolean isGLXServerVendorATI;
      private final boolean isGLXServerVendorNVIDIA;
      private final VersionNumber glXServerVersion;
      private final boolean glXServerVersionOneOneCapable;
      private final boolean glXServerVersionOneThreeCapable;
      private final boolean glXMultisampleAvailable;
      X11GraphicsDevice device;
      X11GraphicsScreen screen;
      GLDrawableImpl drawable;
      GLContextImpl context;

      SharedResource(final X11GraphicsDevice dev, final X11GraphicsScreen scrn,
                     final GLDrawableImpl draw, final GLContextImpl ctx,
                     final VersionNumber glXServerVer, final String glXServerVendor,
                     final boolean glXServerMultisampleAvail) {
          device = dev;
          screen = scrn;
          drawable = draw;
          context = ctx;
          glXServerVersion = glXServerVer;
          glXServerVersionOneOneCapable = glXServerVersion.compareTo(versionOneOne) >= 0 ;
          glXServerVersionOneThreeCapable = glXServerVersion.compareTo(versionOneThree) >= 0 ;
          glXServerVendorName = glXServerVendor;
          isGLXServerVendorATI = GLXUtil.isVendorATI(glXServerVendorName);
          isGLXServerVendorNVIDIA = GLXUtil.isVendorNVIDIA(glXServerVendorName);
          glXMultisampleAvailable = glXServerMultisampleAvail;
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

      final String getGLXVendorName() { return glXServerVendorName; }
      final boolean isGLXVendorATI() { return isGLXServerVendorATI; }
      final boolean isGLXVendorNVIDIA() { return isGLXServerVendorNVIDIA; }
      final VersionNumber getGLXVersion() { return glXServerVersion; }
      final boolean isGLXVersionGreaterEqualOneOne() { return glXServerVersionOneOneCapable; }
      final boolean isGLXVersionGreaterEqualOneThree() { return glXServerVersionOneThreeCapable; }
      final boolean isGLXMultisampleAvailable() { return glXMultisampleAvailable; }
  }

  class SharedResourceImplementation extends SharedResourceRunner.AImplementation {
        @Override
        public boolean isDeviceSupported(final AbstractGraphicsDevice device) {
            final boolean res;
            final X11GraphicsDevice x11Device = new X11GraphicsDevice(X11Util.openDisplay(device.getConnection()), device.getUnitID(), true /* owner */);
            x11Device.lock();
            try {
                res = GLXUtil.isGLXAvailableOnServer(x11Device);
            } finally {
                x11Device.unlock();
                x11Device.close();
            }
            if(DEBUG) {
                System.err.println("GLX "+(res ? "is" : "not")+" available on device/server: "+x11Device);
            }
            return res;
        }

        @Override
        public SharedResourceRunner.Resource createSharedResource(final AbstractGraphicsDevice adevice) {
            final X11GraphicsDevice device = new X11GraphicsDevice(X11Util.openDisplay(adevice.getConnection()), adevice.getUnitID(), true /* owner */);
            GLContextImpl context = null;
            boolean contextIsCurrent = false;
            device.lock();
            try {
                final X11GraphicsScreen screen = new X11GraphicsScreen(device, device.getDefaultScreen());

                GLXUtil.initGLXClientDataSingleton(device);
                final String glXServerVendorName = GLX.glXQueryServerString(device.getHandle(), 0, GLX.GLX_VENDOR);
                final boolean glXServerMultisampleAvailable = GLXUtil.isMultisampleAvailable(GLX.glXQueryServerString(device.getHandle(), 0, GLX.GLX_EXTENSIONS));

                final GLProfile glp = GLProfile.get(device, GLProfile.GL_PROFILE_LIST_MIN_DESKTOP, false);
                if (null == glp) {
                    throw new GLException("Couldn't get default GLProfile for device: "+device);
                }

                final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
                final GLDrawableImpl drawable = createOnscreenDrawableImpl(createDummySurfaceImpl(device, false, caps, caps, null, 64, 64));
                drawable.setRealized(true);
                final X11GLCapabilities chosenCaps =  (X11GLCapabilities) drawable.getChosenGLCapabilities();
                final boolean glxForcedOneOne = !chosenCaps.hasFBConfig();
                final VersionNumber glXServerVersion;
                if( glxForcedOneOne ) {
                    glXServerVersion = versionOneOne;
                } else {
                    glXServerVersion = GLXUtil.getGLXServerVersionNumber(device);
                }
                context = (GLContextImpl) drawable.createContext(null);
                if (null == context) {
                    throw new GLException("Couldn't create shared context for drawable: "+drawable);
                }
                contextIsCurrent = GLContext.CONTEXT_NOT_CURRENT != context.makeCurrent();

                final boolean allowsSurfacelessCtx;
                if( contextIsCurrent && context.getGLVersionNumber().compareTo(GLContext.Version3_0) >= 0 ) {
                    allowsSurfacelessCtx = probeSurfacelessCtx(context, true /* restoreDrawable */);
                } else {
                    setNoSurfacelessCtxQuirk(context);
                    allowsSurfacelessCtx = false;
                }

                if( context.hasRendererQuirk( GLRendererQuirks.DontCloseX11Display ) ) {
                    X11Util.markAllDisplaysUnclosable();
                }
                if ( DEBUG_SHAREDCTX ) {
                    System.err.println("SharedDevice:  " + device);
                    System.err.println("SharedScreen:  " + screen);
                    System.err.println("SharedContext: " + context + ", madeCurrent " + contextIsCurrent);
                    System.err.println("  allowsSurfacelessCtx "+allowsSurfacelessCtx);
                    System.err.println("GLX Server Vendor:      " + glXServerVendorName);
                    System.err.println("GLX Server Version:     " + glXServerVersion + ", forced "+glxForcedOneOne);
                    System.err.println("GLX Server Multisample: " + glXServerMultisampleAvailable);
                    System.err.println("GLX Client Vendor:      " + GLXUtil.getClientVendorName());
                    System.err.println("GLX Client Version:     " + GLXUtil.getClientVersionNumber());
                    System.err.println("GLX Client Multisample: " + GLXUtil.isClientMultisampleAvailable());
                }
                return new SharedResource(device, screen, drawable, context,
                                          glXServerVersion, glXServerVendorName,
                                          glXServerMultisampleAvailable && GLXUtil.isClientMultisampleAvailable());
            } catch (final Throwable t) {
                throw new GLException("X11GLXDrawableFactory - Could not initialize shared resources for "+adevice, t);
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
                ExceptionUtils.dumpStack(System.err);
            }

            if (null != sr.context) {
                // may cause JVM SIGSEGV, or freeze (ATI fglrx 3-6-beta2 32on64 shared ctx):
                sr.context.destroy(); // will also pull the dummy MutableSurface
                sr.context = null;
            }

            if (null != sr.drawable) {
                // may cause JVM SIGSEGV:
                sr.drawable.setRealized(false);
                sr.drawable = null;
            }

            if (null != sr.screen) {
                sr.screen = null;
            }

            if (null != sr.device) {
                // may cause JVM SIGSEGV:
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
      if(null != x11GLXDynamicLookupHelper && device instanceof X11GraphicsDevice) {
          return true;
      }
      return false;
  }

  @Override
  protected final Thread getSharedResourceThread() {
    return sharedResourceRunner.start();
  }

  @Override
  protected final SharedResource getOrCreateSharedResourceImpl(final AbstractGraphicsDevice device) {
    return (SharedResource) sharedResourceRunner.getOrCreateShared(device);
  }

  protected final long getOrCreateSharedDpy(final AbstractGraphicsDevice device) {
    final SharedResourceRunner.Resource sr = getOrCreateSharedResource( device );
    if(null!=sr) {
        return sr.getDevice().getHandle();
    }
    return 0;
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
    return X11GLXGraphicsConfigurationFactory.getAvailableCapabilities(this, device);
  }

  @Override
  protected final GLDrawableImpl createOnscreenDrawableImpl(final NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new X11OnscreenGLXDrawable(this, target, false);
  }

  @Override
  protected final GLDrawableImpl createOffscreenDrawableImpl(final NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    final AbstractGraphicsConfiguration config = target.getGraphicsConfiguration();
    final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    if(!caps.isPBuffer()) {
        return new X11PixmapGLXDrawable(this, target);
    }

    // PBuffer GLDrawable Creation
    GLDrawableImpl pbufferDrawable;
    final AbstractGraphicsDevice device = config.getScreen().getDevice();

    /**
     * Due to the ATI Bug https://bugzilla.mozilla.org/show_bug.cgi?id=486277,
     * we need to have a context current on the same Display to create a PBuffer.
     * The dummy context shall also use the same Display,
     * since switching Display in this regard is another ATI bug.
     */
    final SharedResource sr = (SharedResource) sharedResourceRunner.getOrCreateShared(device);
    if( null!=sr && sr.isGLXVendorATI() && null == GLContext.getCurrent() ) {
        sr.getContext().makeCurrent();
        try {
            pbufferDrawable = new X11PbufferGLXDrawable(this, target);
        } finally {
            sr.getContext().release();
        }
    } else {
        pbufferDrawable = new X11PbufferGLXDrawable(this, target);
    }
    return pbufferDrawable;
  }

  public final boolean isGLXMultisampleAvailable(final AbstractGraphicsDevice device) {
    if(null != device) {
        final SharedResource sr = (SharedResource) sharedResourceRunner.getOrCreateShared(device);
        if(null!=sr) {
          return sr.isGLXMultisampleAvailable();
        }
    }
    return false;
  }

  public final VersionNumber getGLXVersionNumber(final AbstractGraphicsDevice device) {
    if(null != device) {
        final SharedResource sr = (SharedResource) sharedResourceRunner.getOrCreateShared(device);
        if(null!=sr) {
          return sr.getGLXVersion();
        }
        if( device instanceof X11GraphicsDevice ) {
          return GLXUtil.getGLXServerVersionNumber((X11GraphicsDevice)device);
        }
    }
    return null;
  }

  public final boolean isGLXVersionGreaterEqualOneOne(final AbstractGraphicsDevice device) {
    if(null != device) {
        final SharedResource sr = (SharedResource) sharedResourceRunner.getOrCreateShared(device);
        if(null!=sr) {
          return sr.isGLXVersionGreaterEqualOneOne();
        }
        if( device instanceof X11GraphicsDevice ) {
          final VersionNumber glXServerVersion = GLXUtil.getGLXServerVersionNumber((X11GraphicsDevice)device);
          return glXServerVersion.compareTo(versionOneOne) >= 0;
        }
    }
    return false;
  }

  public final boolean isGLXVersionGreaterEqualOneThree(final AbstractGraphicsDevice device) {
    if(null != device) {
        final SharedResource sr = (SharedResource) sharedResourceRunner.getOrCreateShared(device);
        if(null!=sr) {
          return sr.isGLXVersionGreaterEqualOneThree();
        }
        if( device instanceof X11GraphicsDevice ) {
          final VersionNumber glXServerVersion = GLXUtil.getGLXServerVersionNumber((X11GraphicsDevice)device);
          return glXServerVersion.compareTo(versionOneThree) >= 0;
        }
    }
    return false;
  }

  @Override
  public final boolean canCreateGLPbuffer(AbstractGraphicsDevice device, final GLProfile glp) {
      if(null == device) {
        final SharedResourceRunner.Resource sr = sharedResourceRunner.getOrCreateShared(defaultDevice);
        if(null!=sr) {
            device = sr.getDevice();
        }
      }
      return isGLXVersionGreaterEqualOneThree(device);
  }

  @Override
  protected final ProxySurface createMutableSurfaceImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                        final GLCapabilitiesImmutable capsChosen,
                                                        final GLCapabilitiesImmutable capsRequested,
                                                        final GLCapabilitiesChooser chooser, final UpstreamSurfaceHook upstreamHook) {
    final X11GraphicsDevice device;
    if( createNewDevice || !(deviceReq instanceof X11GraphicsDevice) ) {
        device = new X11GraphicsDevice(X11Util.openDisplay(deviceReq.getConnection()), deviceReq.getUnitID(), true /* owner */);
    } else {
        device = (X11GraphicsDevice) deviceReq;
    }
    final X11GraphicsScreen screen = new X11GraphicsScreen(device, device.getDefaultScreen());
    final X11GLXGraphicsConfiguration config = X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, screen, VisualIDHolder.VID_UNDEFINED);
    if(null == config) {
        throw new GLException("Choosing GraphicsConfiguration failed w/ "+capsChosen+" on "+screen);
    }
    return new WrappedSurface(config, 0, upstreamHook, createNewDevice);
  }

  @Override
  public final ProxySurface createDummySurfaceImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                   GLCapabilitiesImmutable chosenCaps, final GLCapabilitiesImmutable requestedCaps, final GLCapabilitiesChooser chooser, final int width, final int height) {
    chosenCaps = GLGraphicsConfigurationUtil.fixOnscreenGLCapabilities(chosenCaps);
    return createMutableSurfaceImpl(deviceReq, createNewDevice, chosenCaps, requestedCaps, chooser, new X11DummyUpstreamSurfaceHook(width, height));
  }

  @Override
  public final ProxySurface createSurfacelessImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                  GLCapabilitiesImmutable chosenCaps, final GLCapabilitiesImmutable requestedCaps, final GLCapabilitiesChooser chooser, final int width, final int height) {
    chosenCaps = GLGraphicsConfigurationUtil.fixOnscreenGLCapabilities(chosenCaps);
    return createMutableSurfaceImpl(deviceReq, createNewDevice, chosenCaps, requestedCaps, chooser, new GenericUpstreamSurfacelessHook(width, height));
  }

  @Override
  protected final ProxySurface createProxySurfaceImpl(final AbstractGraphicsDevice deviceReq, final int screenIdx, final long windowHandle, final GLCapabilitiesImmutable capsRequested, final GLCapabilitiesChooser chooser, final UpstreamSurfaceHook upstream) {
    final X11GraphicsDevice device = new X11GraphicsDevice(X11Util.openDisplay(deviceReq.getConnection()), deviceReq.getUnitID(), true /* owner */);
    final X11GraphicsScreen screen = new X11GraphicsScreen(device, screenIdx);
    final int xvisualID = X11Lib.GetVisualIDFromWindow(device.getHandle(), windowHandle);
    if(VisualIDHolder.VID_UNDEFINED == xvisualID) {
        throw new GLException("Undefined VisualID of window 0x"+Long.toHexString(windowHandle)+", window probably invalid");
    }
    if(DEBUG) {
        System.err.println("X11GLXDrawableFactory.createProxySurfaceImpl 0x"+Long.toHexString(windowHandle)+": visualID 0x"+Integer.toHexString(xvisualID));
    }
    final X11GLXGraphicsConfiguration cfg = X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsRequested, capsRequested, chooser, screen, xvisualID);
    if(DEBUG) {
        System.err.println("X11GLXDrawableFactory.createProxySurfaceImpl 0x"+Long.toHexString(windowHandle)+": "+cfg);
    }
    return new WrappedSurface(cfg, windowHandle, upstream, true);
  }

  @Override
  protected final GLContext createExternalGLContextImpl() {
    return X11ExternalGLXContext.create(this, null);
  }

  @Override
  public final boolean canCreateExternalGLDrawable(final AbstractGraphicsDevice device) {
    return canCreateGLPbuffer(device, null /* GLProfile not used for query on X11 */);
  }

  @Override
  protected final GLDrawable createExternalGLDrawableImpl() {
    return X11ExternalGLXDrawable.create(this, null);
  }

  //----------------------------------------------------------------------
  // Gamma-related functionality
  //

  private boolean gotGammaRampLength;
  private int gammaRampLength;
  @Override
  protected final synchronized int getGammaRampLength(final NativeSurface surface) {
    if (gotGammaRampLength) {
      return gammaRampLength;
    }
    final long display = surface.getDisplayHandle();
    if(0 == display) {
        return 0;
    }
    final int screenIdx = surface.getScreenIndex();

    final int[] size = new int[1];
    final boolean res = X11Lib.XF86VidModeGetGammaRampSize(display, screenIdx, size, 0);
    if (!res) {
      return 0;
    }
    gotGammaRampLength = true;
    gammaRampLength = size[0];
    System.err.println("XXX: Gamma ramp size: "+gammaRampLength);
    return gammaRampLength;
  }

  @Override
  protected final boolean setGammaRamp(final NativeSurface surface, final float[] ramp) {
    final long display = surface.getDisplayHandle();
    if(0 == display) {
        return false;
    }
    final int screenIdx = surface.getScreenIndex();

    final int len = ramp.length;
    final short[] rampData = new short[len];
    for (int i = 0; i < len; i++) {
      rampData[i] = (short) (ramp[i] * 65535);
    }

    final boolean res = X11Lib.XF86VidModeSetGammaRamp(display, screenIdx,
                                              rampData.length,
                                              rampData, 0,
                                              rampData, 0,
                                              rampData, 0);
    if( DEBUG ) {
        dumpRamp("SET__", rampData.length, rampData, rampData, rampData);
    }
    return res;
  }

  private static void dumpRamp(final String prefix, final int size, final ShortBuffer r, final ShortBuffer g, final ShortBuffer b) {
      for(int i=0; i<size; i++) {
          if( 0 == i % 4 ) {
              System.err.printf("%n%4d/%4d %s: ", i, size, prefix);
          }
          System.err.printf(" [%04X %04X %04X], ",  r.get(i), g.get(i), b.get(i));
      }
      System.err.println();
  }
  private static void dumpRamp(final String prefix, final int size, final short[] r, final short[] g, final short[] b) {
      for(int i=0; i<size; i++) {
          if( 0 == i % 4 ) {
              System.err.printf("%n%4d/%4d %s: ", i, size, prefix);
          }
          System.err.printf(" [%04X %04X %04X], ",  r[i], g[i], b[i]);
      }
      System.err.println();
  }

  @Override
  protected final Buffer getGammaRamp(final NativeSurface surface) {
    final long display = surface.getDisplayHandle();
    if(0 == display) {
        return null;
    }
    final int screenIdx = surface.getScreenIndex();

    final int size = getGammaRampLength(surface);

    final ShortBuffer rampData = Buffers.newDirectShortBuffer(3 * size);
    final ShortBuffer redRampData   = Buffers.slice(rampData, 0 * size, size);
    final ShortBuffer greenRampData = Buffers.slice(rampData, 1 * size, size);
    final ShortBuffer blueRampData  = Buffers.slice(rampData, 2 * size, size);

    final boolean res = X11Lib.XF86VidModeGetGammaRamp(display, screenIdx,
                                              size,
                                              redRampData,
                                              greenRampData,
                                              blueRampData);
    if (!res) {
      return null;
    }
    if( DEBUG ) {
        dumpRamp("GET__", size, redRampData, greenRampData, blueRampData);
    }
    return rampData;
  }

  @Override
  protected final void resetGammaRamp(final NativeSurface surface, final Buffer originalGammaRamp) {
    if (originalGammaRamp == null) {
        return; // getGammaRamp failed originally
    }
    final long display = surface.getDisplayHandle();
    if(0 == display) {
        return;
    }
    final int screenIdx = surface.getScreenIndex();

    resetGammaRamp(display, screenIdx, originalGammaRamp);
  }

  @Override
  protected final void resetGammaRamp(final DeviceScreenID deviceScreenID, final Buffer originalGammaRamp) {
    if (originalGammaRamp == null) {
        return; // getGammaRamp failed originally
    }
    final long display = X11Util.openDisplay(deviceScreenID.deviceConnection);
    if( 0 == display ) {
        return;
    }
    try {
        resetGammaRamp(display, deviceScreenID.screenIdx, originalGammaRamp);
    } finally {
        X11Util.closeDisplay(display);
    }
  }

  private static final void resetGammaRamp(final long display, final int screenIdx, final Buffer originalGammaRamp) {
    final ShortBuffer rampData = (ShortBuffer) originalGammaRamp;
    final int capacity = rampData.capacity();
    if ((capacity % 3) != 0) {
      throw new IllegalArgumentException("Must not be the original gamma ramp");
    }
    final int size = capacity / 3;

    final ShortBuffer redRampData   = Buffers.slice(rampData, 0 * size, size);
    final ShortBuffer greenRampData = Buffers.slice(rampData, 1 * size, size);
    final ShortBuffer blueRampData  = Buffers.slice(rampData, 2 * size, size);
    if( DEBUG ) {
        dumpRamp("RESET", size, redRampData, greenRampData, blueRampData);
    }

    X11Lib.XF86VidModeSetGammaRamp(display, screenIdx,
                                   size,
                                   redRampData,
                                   greenRampData,
                                   blueRampData);
  }

}
