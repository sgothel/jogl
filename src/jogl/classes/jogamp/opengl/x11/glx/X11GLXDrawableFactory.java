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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;
import javax.media.nativewindow.VisualIDHolder;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

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

import com.jogamp.common.util.VersionNumber;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;
import com.jogamp.opengl.GLRendererQuirks;

public class X11GLXDrawableFactory extends GLDrawableFactoryImpl {

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

        sharedMap = new HashMap<String, SharedResourceRunner.Resource>();

        // Init shared resources off thread
        // Will be released via ShutdownHook
        sharedResourceRunner = new SharedResourceRunner(new SharedResourceImplementation());
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
    if(null != sharedMap) {
        sharedMap.clear();
        sharedMap = null;
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
  public final GLDynamicLookupHelper getGLDynamicLookupHelper(final int profile) {
      return x11GLXDynamicLookupHelper;
  }

  private X11GraphicsDevice defaultDevice;
  private SharedResourceRunner sharedResourceRunner;
  private HashMap<String /* connection */, SharedResourceRunner.Resource> sharedMap;

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
                     final VersionNumber glXServerVer, final String glXServerVendor, final boolean glXServerMultisampleAvail) {
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

      final String getGLXVendorName() { return glXServerVendorName; }
      final boolean isGLXVendorATI() { return isGLXServerVendorATI; }
      final boolean isGLXVendorNVIDIA() { return isGLXServerVendorNVIDIA; }
      final VersionNumber getGLXVersion() { return glXServerVersion; }
      final boolean isGLXVersionGreaterEqualOneOne() { return glXServerVersionOneOneCapable; }
      final boolean isGLXVersionGreaterEqualOneThree() { return glXServerVersionOneThreeCapable; }
      final boolean isGLXMultisampleAvailable() { return glXMultisampleAvailable; }
  }

  class SharedResourceImplementation implements SharedResourceRunner.Implementation {
        @Override
        public void clear() {
            sharedMap.clear();
        }
        @Override
        public SharedResourceRunner.Resource mapPut(final String connection, final SharedResourceRunner.Resource resource) {
            return sharedMap.put(connection, resource);
        }
        @Override
        public SharedResourceRunner.Resource mapGet(final String connection) {
            return sharedMap.get(connection);
        }
        @Override
        public Collection<SharedResourceRunner.Resource> mapValues() {
            return sharedMap.values();
        }

        @Override
        public boolean isDeviceSupported(final String connection) {
            final boolean res;
            final X11GraphicsDevice x11Device = new X11GraphicsDevice(X11Util.openDisplay(connection), AbstractGraphicsDevice.DEFAULT_UNIT, true /* owner */);
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
        public SharedResourceRunner.Resource createSharedResource(final String connection) {
            final X11GraphicsDevice sharedDevice = new X11GraphicsDevice(X11Util.openDisplay(connection), AbstractGraphicsDevice.DEFAULT_UNIT, true /* owner */);
            sharedDevice.lock();
            try {
                final X11GraphicsScreen sharedScreen = new X11GraphicsScreen(sharedDevice, sharedDevice.getDefaultScreen());

                GLXUtil.initGLXClientDataSingleton(sharedDevice);
                final String glXServerVendorName = GLX.glXQueryServerString(sharedDevice.getHandle(), 0, GLX.GLX_VENDOR);
                final boolean glXServerMultisampleAvailable = GLXUtil.isMultisampleAvailable(GLX.glXQueryServerString(sharedDevice.getHandle(), 0, GLX.GLX_EXTENSIONS));

                final GLProfile glp = GLProfile.get(sharedDevice, GLProfile.GL_PROFILE_LIST_MIN_DESKTOP, false);
                if (null == glp) {
                    throw new GLException("Couldn't get default GLProfile for device: "+sharedDevice);
                }

                final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
                final GLDrawableImpl sharedDrawable = createOnscreenDrawableImpl(createDummySurfaceImpl(sharedDevice, false, caps, caps, null, 64, 64));
                sharedDrawable.setRealized(true);
                final X11GLCapabilities chosenCaps =  (X11GLCapabilities) sharedDrawable.getChosenGLCapabilities();
                final boolean glxForcedOneOne = !chosenCaps.hasFBConfig();
                final VersionNumber glXServerVersion;
                if( glxForcedOneOne ) {
                    glXServerVersion = versionOneOne;
                } else {
                    glXServerVersion = GLXUtil.getGLXServerVersionNumber(sharedDevice);
                }
                final GLContextImpl sharedContext = (GLContextImpl) sharedDrawable.createContext(null);
                if (null == sharedContext) {
                    throw new GLException("Couldn't create shared context for drawable: "+sharedDrawable);
                }

                boolean madeCurrent = false;
                sharedContext.makeCurrent();
                try {
                    madeCurrent = sharedContext.isCurrent();
                } finally {
                    sharedContext.release();
                }
                if( sharedContext.hasRendererQuirk( GLRendererQuirks.DontCloseX11Display ) ) {
                    X11Util.markAllDisplaysUnclosable();
                }
                if (DEBUG) {
                    System.err.println("SharedDevice:  " + sharedDevice);
                    System.err.println("SharedScreen:  " + sharedScreen);
                    System.err.println("SharedContext: " + sharedContext + ", madeCurrent " + madeCurrent);
                    System.err.println("GLX Server Vendor:      " + glXServerVendorName);
                    System.err.println("GLX Server Version:     " + glXServerVersion + ", forced "+glxForcedOneOne);
                    System.err.println("GLX Server Multisample: " + glXServerMultisampleAvailable);
                    System.err.println("GLX Client Vendor:      " + GLXUtil.getClientVendorName());
                    System.err.println("GLX Client Version:     " + GLXUtil.getClientVersionNumber());
                    System.err.println("GLX Client Multisample: " + GLXUtil.isClientMultisampleAvailable());
                }
                return new SharedResource(sharedDevice, sharedScreen, sharedDrawable, sharedContext,
                                          glXServerVersion, glXServerVendorName,
                                          glXServerMultisampleAvailable && GLXUtil.isClientMultisampleAvailable());
            } catch (final Throwable t) {
                throw new GLException("X11GLXDrawableFactory - Could not initialize shared resources for "+connection, t);
            } finally {
                sharedDevice.unlock();
            }
        }

        @Override
        public void releaseSharedResource(final SharedResourceRunner.Resource shared) {
            final SharedResource sr = (SharedResource) shared;
            if (DEBUG) {
                System.err.println("Shutdown Shared:");
                System.err.println("Device  : " + sr.device);
                System.err.println("Screen  : " + sr.screen);
                System.err.println("Drawable: " + sr.drawable);
                System.err.println("CTX     : " + sr.context);
                Thread.dumpStack();
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
  protected final synchronized int getGammaRampLength() {
    if (gotGammaRampLength) {
      return gammaRampLength;
    }

    final long display = getOrCreateSharedDpy(defaultDevice);
    if(0 == display) {
        return 0;
    }

    final int[] size = new int[1];
    final boolean res = X11Lib.XF86VidModeGetGammaRampSize(display,
                                                      X11Lib.DefaultScreen(display),
                                                      size, 0);
    if (!res) {
      return 0;
    }
    gotGammaRampLength = true;
    gammaRampLength = size[0];
    return gammaRampLength;
  }

  @Override
  protected final boolean setGammaRamp(final float[] ramp) {
    final long display = getOrCreateSharedDpy(defaultDevice);
    if(0 == display) {
        return false;
    }

    final int len = ramp.length;
    final short[] rampData = new short[len];
    for (int i = 0; i < len; i++) {
      rampData[i] = (short) (ramp[i] * 65535);
    }

    final boolean res = X11Lib.XF86VidModeSetGammaRamp(display,
                                              X11Lib.DefaultScreen(display),
                                              rampData.length,
                                              rampData, 0,
                                              rampData, 0,
                                              rampData, 0);
    return res;
  }

  @Override
  protected final Buffer getGammaRamp() {
    final long display = getOrCreateSharedDpy(defaultDevice);
    if(0 == display) {
        return null;
    }

    final int size = getGammaRampLength();
    final ShortBuffer rampData = ShortBuffer.wrap(new short[3 * size]);
    rampData.position(0);
    rampData.limit(size);
    final ShortBuffer redRampData = rampData.slice();
    rampData.position(size);
    rampData.limit(2 * size);
    final ShortBuffer greenRampData = rampData.slice();
    rampData.position(2 * size);
    rampData.limit(3 * size);
    final ShortBuffer blueRampData = rampData.slice();

    final boolean res = X11Lib.XF86VidModeGetGammaRamp(display,
                                              X11Lib.DefaultScreen(display),
                                              size,
                                              redRampData,
                                              greenRampData,
                                              blueRampData);
    if (!res) {
      return null;
    }
    return rampData;
  }

  @Override
  protected final void resetGammaRamp(final Buffer originalGammaRamp) {
    if (originalGammaRamp == null) {
        return; // getGammaRamp failed originally
    }
    final long display = getOrCreateSharedDpy(defaultDevice);
    if(0 == display) {
        return;
    }

    final ShortBuffer rampData = (ShortBuffer) originalGammaRamp;
    final int capacity = rampData.capacity();
    if ((capacity % 3) != 0) {
      throw new IllegalArgumentException("Must not be the original gamma ramp");
    }
    final int size = capacity / 3;
    rampData.position(0);
    rampData.limit(size);
    final ShortBuffer redRampData = rampData.slice();
    rampData.position(size);
    rampData.limit(2 * size);
    final ShortBuffer greenRampData = rampData.slice();
    rampData.position(2 * size);
    rampData.limit(3 * size);
    final ShortBuffer blueRampData = rampData.slice();

    X11Lib.XF86VidModeSetGammaRamp(display,
                                X11Lib.DefaultScreen(display),
                                size,
                                redRampData,
                                greenRampData,
                                blueRampData);
  }
}
