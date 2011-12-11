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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.x11.X11GraphicsDevice;
import javax.media.nativewindow.x11.X11GraphicsScreen;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLProfile.ShutdownType;

import jogamp.nativewindow.WrappedSurface;
import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.X11Util;
import jogamp.opengl.DesktopGLDynamicLookupHelper;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.SharedResourceRunner;

import com.jogamp.common.util.VersionNumber;

public class X11GLXDrawableFactory extends GLDrawableFactoryImpl {
  
  public static final VersionNumber versionOneZero = new VersionNumber(1, 0, 0);
  public static final VersionNumber versionOneOne = new VersionNumber(1, 1, 0);
  public static final VersionNumber versionOneTwo = new VersionNumber(1, 2, 0);
  public static final VersionNumber versionOneThree = new VersionNumber(1, 3, 0);
  public static final VersionNumber versionOneFour = new VersionNumber(1, 4, 0);

  private static DesktopGLDynamicLookupHelper x11GLXDynamicLookupHelper = null;
  
  public X11GLXDrawableFactory() {
    super();

    synchronized(X11GLXDrawableFactory.class) {
        if(null==x11GLXDynamicLookupHelper) {
            DesktopGLDynamicLookupHelper tmp = null;
            try {
                tmp = new DesktopGLDynamicLookupHelper(new X11GLXDynamicLibraryBundleInfo());
            } catch (GLException gle) {
                if(DEBUG) {
                    gle.printStackTrace();
                }
            }
            x11GLXDynamicLookupHelper = tmp;
            if(null!=x11GLXDynamicLookupHelper) {
                GLX.getGLXProcAddressTable().reset(x11GLXDynamicLookupHelper);
            }
        }
    }
    
    if(null!=x11GLXDynamicLookupHelper) {        
        // Register our GraphicsConfigurationFactory implementations
        // The act of constructing them causes them to be registered
        X11GLXGraphicsConfigurationFactory.registerFactory();
        
        defaultDevice = new X11GraphicsDevice(X11Util.getNullDisplayName(), AbstractGraphicsDevice.DEFAULT_UNIT);
        sharedMap = new HashMap<String, SharedResourceRunner.Resource>();
        
        // Init shared resources off thread
        // Will be released via ShutdownHook
        sharedResourceRunner = new SharedResourceRunner(new SharedResourceImplementation());
        sharedResourceRunner.start();
    }    
  }

  protected final void destroy(ShutdownType shutdownType) {
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
    if(ShutdownType.COMPLETE == shutdownType && null != x11GLXDynamicLookupHelper) {
        x11GLXDynamicLookupHelper.destroy();
        x11GLXDynamicLookupHelper = null;
    } */

    // Don't really close pending Display connections,
    // since this may trigger a JVM exception
    X11Util.shutdown( false, DEBUG );
  }

  public GLDynamicLookupHelper getGLDynamicLookupHelper(int profile) {
      return x11GLXDynamicLookupHelper;
  }

  private X11GraphicsDevice defaultDevice;
  private SharedResourceRunner sharedResourceRunner;
  private HashMap<String /* connection */, SharedResourceRunner.Resource> sharedMap;  

  static class SharedResource implements SharedResourceRunner.Resource {
      X11GraphicsDevice device;
      X11GraphicsScreen screen;
      X11DummyGLXDrawable drawable;
      X11GLXContext context;
      String glXServerVendorName;
      boolean isGLXServerVendorATI;
      boolean isGLXServerVendorNVIDIA;
      VersionNumber glXServerVersion;
      boolean glXServerVersionOneOneCapable;
      boolean glXServerVersionOneThreeCapable;
      boolean glXMultisampleAvailable;

      SharedResource(X11GraphicsDevice dev, X11GraphicsScreen scrn,
                     X11DummyGLXDrawable draw, X11GLXContext ctx,
                     VersionNumber glXServerVer, String glXServerVendor, boolean glXServerMultisampleAvail) {
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
      final public AbstractGraphicsDevice getDevice() { return device; }
      final public AbstractGraphicsScreen getScreen() { return screen; }
      final public GLDrawableImpl getDrawable() { return drawable; }
      final public GLContextImpl getContext() { return context; }

      final String getGLXVendorName() { return glXServerVendorName; }
      final boolean isGLXVendorATI() { return isGLXServerVendorATI; }
      final boolean isGLXVendorNVIDIA() { return isGLXServerVendorNVIDIA; }
      final VersionNumber getGLXVersion() { return glXServerVersion; }
      final boolean isGLXVersionGreaterEqualOneOne() { return glXServerVersionOneOneCapable; }
      final boolean isGLXVersionGreaterEqualOneThree() { return glXServerVersionOneThreeCapable; }
      final boolean isGLXMultisampleAvailable() { return glXMultisampleAvailable; }
  }

  class SharedResourceImplementation implements SharedResourceRunner.Implementation {
        public void clear() {
            synchronized(sharedMap) {
                sharedMap.clear();
            }
        }
        public SharedResourceRunner.Resource mapPut(String connection, SharedResourceRunner.Resource resource) {
            synchronized(sharedMap) {
                return sharedMap.put(connection, resource);
            }
        }
        public SharedResourceRunner.Resource mapGet(String connection) {
            synchronized(sharedMap) {
                return sharedMap.get(connection);
            }
        }
        public Collection<SharedResourceRunner.Resource> mapValues() {
            synchronized(sharedMap) {
                return sharedMap.values();
            }
        }

        public SharedResourceRunner.Resource createSharedResource(String connection) {
            X11GraphicsDevice sharedDevice = 
                    new X11GraphicsDevice(X11Util.openDisplay(connection), AbstractGraphicsDevice.DEFAULT_UNIT, 
                                          true); // own non-shared display connection, no locking
                    // new X11GraphicsDevice(X11Util.openDisplay(connection), AbstractGraphicsDevice.DEFAULT_UNIT, 
                    //                       NativeWindowFactory.getNullToolkitLock(), true); // own non-shared display connection, no locking
            sharedDevice.lock();
            try {
                GLXUtil.initGLXClientDataSingleton(sharedDevice);
                final String glXServerVendorName = GLX.glXQueryServerString(sharedDevice.getHandle(), 0, GLX.GLX_VENDOR);
                final VersionNumber glXServerVersion = GLXUtil.getGLXServerVersionNumber(sharedDevice.getHandle());
                final boolean glXServerMultisampleAvailable = GLXUtil.isMultisampleAvailable(GLX.glXQueryServerString(sharedDevice.getHandle(), 0, GLX.GLX_EXTENSIONS));                
                if(X11Util.ATI_HAS_XCLOSEDISPLAY_BUG && GLXUtil.isVendorATI(glXServerVendorName)) {
                    X11Util.setMarkAllDisplaysUnclosable(true);
                    X11Util.markDisplayUncloseable(sharedDevice.getHandle());
                }
                X11GraphicsScreen sharedScreen = new X11GraphicsScreen(sharedDevice, 0);

                GLProfile glp = GLProfile.get(sharedDevice, GLProfile.GL_PROFILE_LIST_MIN_DESKTOP);
                if (null == glp) {
                    throw new GLException("Couldn't get default GLProfile for device: "+sharedDevice);
                }
                X11DummyGLXDrawable sharedDrawable = X11DummyGLXDrawable.create(sharedScreen, X11GLXDrawableFactory.this, glp);
                if (null == sharedDrawable) {
                    throw new GLException("Couldn't create shared drawable for screen: "+sharedScreen+", "+glp);
                }
                X11GLXContext sharedContext = (X11GLXContext) sharedDrawable.createContext(null);
                if (null == sharedContext) {
                    throw new GLException("Couldn't create shared context for drawable: "+sharedDrawable);
                }
                sharedContext.setSynchronized(true);
                boolean madeCurrent = false;
                sharedContext.makeCurrent();
                try {
                    madeCurrent = sharedContext.isCurrent();
                } finally {
                    sharedContext.release();
                }
                if (DEBUG) {
                    System.err.println("!!! SharedDevice:  " + sharedDevice);
                    System.err.println("!!! SharedScreen:  " + sharedScreen);
                    System.err.println("!!! SharedContext: " + sharedContext + ", madeCurrent " + madeCurrent);
                    System.err.println("!!! GLX Server Vendor:      " + glXServerVendorName);
                    System.err.println("!!! GLX Server Version:     " + glXServerVersion);
                    System.err.println("!!! GLX Server Multisample: " + glXServerMultisampleAvailable);
                    System.err.println("!!! GLX Client Vendor:      " + GLXUtil.getClientVendorName());
                    System.err.println("!!! GLX Client Version:     " + GLXUtil.getClientVersionNumber());
                    System.err.println("!!! GLX Client Multisample: " + GLXUtil.isClientMultisampleAvailable());
                }
                return new SharedResource(sharedDevice, sharedScreen, sharedDrawable, sharedContext, 
                                          glXServerVersion, glXServerVendorName, 
                                          glXServerMultisampleAvailable && GLXUtil.isClientMultisampleAvailable());
            } catch (Throwable t) {
                throw new GLException("X11GLXDrawableFactory - Could not initialize shared resources for "+connection, t);
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
                Thread.dumpStack();
            }

            if (null != sr.context) {
                // may cause JVM SIGSEGV:
                sr.context.makeCurrent();
                sr.context.destroy();
                sr.context = null;
            }

            if (null != sr.drawable) {
                // may cause JVM SIGSEGV:
                sr.drawable.destroy();
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

  public final AbstractGraphicsDevice getDefaultDevice() {
      return defaultDevice;
  }

  public final boolean getIsDeviceCompatible(AbstractGraphicsDevice device) {
      if(device instanceof X11GraphicsDevice) {
          return true;
      }
      return false;
  }

  protected final Thread getSharedResourceThread() {
    return sharedResourceRunner.start();
  }
    
  protected final boolean createSharedResource(AbstractGraphicsDevice device) {
    try {
        SharedResourceRunner.Resource sr = sharedResourceRunner.getOrCreateShared(device);
        if(null!=sr) {
          return null != sr.getContext();
        }
    } catch (GLException gle) {
        if(DEBUG) {
            System.err.println("Catched Exception while X11GLX Shared Resource initialization");
            gle.printStackTrace();
        }
    }
    return false;
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

  protected final long getOrCreateSharedDpy(AbstractGraphicsDevice device) {
    SharedResourceRunner.Resource sr = sharedResourceRunner.getOrCreateShared(device);
    if(null!=sr) {
        return sr.getDevice().getHandle();
    }
    return 0;
  }

  SharedResource getOrCreateSharedResource(AbstractGraphicsDevice device) {
    return (SharedResource) sharedResourceRunner.getOrCreateShared(device);
  }

  protected List<GLCapabilitiesImmutable> getAvailableCapabilitiesImpl(AbstractGraphicsDevice device) {
    return X11GLXGraphicsConfigurationFactory.getAvailableCapabilities(this, device);
  }

  protected final GLDrawableImpl createOnscreenDrawableImpl(NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new X11OnscreenGLXDrawable(this, target);
  }

  protected final GLDrawableImpl createOffscreenDrawableImpl(NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    AbstractGraphicsConfiguration config = target.getGraphicsConfiguration();
    GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    if(!caps.isPBuffer()) {
        return new X11PixmapGLXDrawable(this, target);
    }

    // PBuffer GLDrawable Creation
    GLDrawableImpl pbufferDrawable;
    AbstractGraphicsDevice device = config.getScreen().getDevice();

    /**
     * Due to the ATI Bug https://bugzilla.mozilla.org/show_bug.cgi?id=486277,
     * we need to have a context current on the same Display to create a PBuffer.
     * The dummy context shall also use the same Display,
     * since switching Display in this regard is another ATI bug.
     */
    SharedResource sr = (SharedResource) sharedResourceRunner.getOrCreateShared(device);
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

  public final boolean isGLXMultisampleAvailable(AbstractGraphicsDevice device) {
    if(null != device) {  
        SharedResource sr = (SharedResource) sharedResourceRunner.getOrCreateShared(device);
        if(null!=sr) {
          return sr.isGLXMultisampleAvailable();
        }
    }
    return false;
  }

  public final VersionNumber getGLXVersionNumber(AbstractGraphicsDevice device) {
    if(null != device) {  
        SharedResource sr = (SharedResource) sharedResourceRunner.getOrCreateShared(device);
        if(null!=sr) {
          return sr.getGLXVersion();
        }
        if( device instanceof X11GraphicsDevice ) {
          return GLXUtil.getGLXServerVersionNumber(device.getHandle());
        }
    }
    return null;
  }
  
  public final boolean isGLXVersionGreaterEqualOneOne(AbstractGraphicsDevice device) {
    if(null != device) {  
        SharedResource sr = (SharedResource) sharedResourceRunner.getOrCreateShared(device);
        if(null!=sr) {
          return sr.isGLXVersionGreaterEqualOneOne();
        }
        if( device instanceof X11GraphicsDevice ) {
          final VersionNumber glXServerVersion = GLXUtil.getGLXServerVersionNumber(device.getHandle());
          return glXServerVersion.compareTo(versionOneOne) >= 0;
        }
    }
    return false;
  }
  
  public final boolean isGLXVersionGreaterEqualOneThree(AbstractGraphicsDevice device) {
    if(null != device) {  
        SharedResource sr = (SharedResource) sharedResourceRunner.getOrCreateShared(device);
        if(null!=sr) {
          return sr.isGLXVersionGreaterEqualOneThree();
        }
        if( device instanceof X11GraphicsDevice ) {
          final VersionNumber glXServerVersion = GLXUtil.getGLXServerVersionNumber(device.getHandle());
          return glXServerVersion.compareTo(versionOneThree) >= 0;
        }
    }
    return false;
  }

  public final boolean canCreateGLPbuffer(AbstractGraphicsDevice device) {
      if(null == device) {
        SharedResourceRunner.Resource sr = sharedResourceRunner.getOrCreateShared(defaultDevice);
        if(null!=sr) {
            device = sr.getDevice();
        }
      }
      return isGLXVersionGreaterEqualOneThree(device);
  }

  protected final NativeSurface createOffscreenSurfaceImpl(AbstractGraphicsDevice deviceReq,
                                                           GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested,
                                                           GLCapabilitiesChooser chooser,
                                                           int width, int height) {
    if(null == deviceReq) {
        throw new InternalError("deviceReq is null");
    }
    final SharedResourceRunner.Resource sr = sharedResourceRunner.getOrCreateShared(deviceReq);
    if(null==sr) {
        throw new InternalError("No SharedResource for: "+deviceReq);
    }
    final X11GraphicsScreen sharedScreen = (X11GraphicsScreen) sr.getScreen();
    final AbstractGraphicsDevice sharedDevice = sharedScreen.getDevice(); // should be same ..

    // create screen/device pair - Null X11 locking, due to private non-shared Display handle
    final X11GraphicsDevice device = new X11GraphicsDevice(X11Util.openDisplay(sharedDevice.getConnection()), AbstractGraphicsDevice.DEFAULT_UNIT, NativeWindowFactory.getNullToolkitLock(), true);
    final X11GraphicsScreen screen = new X11GraphicsScreen(device, sharedScreen.getIndex()); 
    
    WrappedSurface ns = new WrappedSurface(
               X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, screen) );
    if(ns != null) {
        ns.surfaceSizeChanged(width, height);
    }
    return ns;
  }

  protected final ProxySurface createProxySurfaceImpl(AbstractGraphicsDevice adevice, long windowHandle, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser) {
    // FIXME device/windowHandle -> screen ?!
    X11GraphicsDevice device = (X11GraphicsDevice) adevice;
    X11GraphicsScreen screen = new X11GraphicsScreen(device, 0);
    X11GLXGraphicsConfiguration cfg = X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsRequested, capsRequested, chooser, screen);
    WrappedSurface ns = new WrappedSurface(cfg, windowHandle);
    return ns;
  }
 
  protected final GLContext createExternalGLContextImpl() {
    return X11ExternalGLXContext.create(this, null);
  }

  public final boolean canCreateExternalGLDrawable(AbstractGraphicsDevice device) {
    return canCreateGLPbuffer(device);
  }

  protected final GLDrawable createExternalGLDrawableImpl() {
    return X11ExternalGLXDrawable.create(this, null);
  }

  public final boolean canCreateContextOnJava2DSurface(AbstractGraphicsDevice device) {
    return false;
  }

  public final GLContext createContextOnJava2DSurface(Object graphics, GLContext shareWith)
    throws GLException {
    throw new GLException("Unimplemented on this platform");
  }

  //----------------------------------------------------------------------
  // Gamma-related functionality
  //

  private boolean gotGammaRampLength;
  private int gammaRampLength;
  protected final synchronized int getGammaRampLength() {
    if (gotGammaRampLength) {
      return gammaRampLength;
    }

    long display = getOrCreateSharedDpy(defaultDevice);
    if(0 == display) {
        return 0;
    }

    int[] size = new int[1];
    boolean res = X11Lib.XF86VidModeGetGammaRampSize(display,
                                                      X11Lib.DefaultScreen(display),
                                                      size, 0);
    if (!res) {
      return 0;
    }
    gotGammaRampLength = true;
    gammaRampLength = size[0];
    return gammaRampLength;
  }

  protected final boolean setGammaRamp(float[] ramp) {
    long display = getOrCreateSharedDpy(defaultDevice);
    if(0 == display) {
        return false;
    }

    int len = ramp.length;
    short[] rampData = new short[len];
    for (int i = 0; i < len; i++) {
      rampData[i] = (short) (ramp[i] * 65535);
    }

    boolean res = X11Lib.XF86VidModeSetGammaRamp(display,
                                              X11Lib.DefaultScreen(display),
                                              rampData.length,
                                              rampData, 0,
                                              rampData, 0,
                                              rampData, 0);
    return res;
  }

  protected final Buffer getGammaRamp() {
    long display = getOrCreateSharedDpy(defaultDevice);
    if(0 == display) {
        return null;
    }

    int size = getGammaRampLength();
    ShortBuffer rampData = ShortBuffer.wrap(new short[3 * size]);
    rampData.position(0);
    rampData.limit(size);
    ShortBuffer redRampData = rampData.slice();
    rampData.position(size);
    rampData.limit(2 * size);
    ShortBuffer greenRampData = rampData.slice();
    rampData.position(2 * size);
    rampData.limit(3 * size);
    ShortBuffer blueRampData = rampData.slice();

    boolean res = X11Lib.XF86VidModeGetGammaRamp(display,
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

  protected final void resetGammaRamp(Buffer originalGammaRamp) {
    if (originalGammaRamp == null) {
        return; // getGammaRamp failed originally
    }
    long display = getOrCreateSharedDpy(defaultDevice);
    if(0 == display) {
        return;
    }

    ShortBuffer rampData = (ShortBuffer) originalGammaRamp;
    int capacity = rampData.capacity();
    if ((capacity % 3) != 0) {
      throw new IllegalArgumentException("Must not be the original gamma ramp");
    }
    int size = capacity / 3;
    rampData.position(0);
    rampData.limit(size);
    ShortBuffer redRampData = rampData.slice();
    rampData.position(size);
    rampData.limit(2 * size);
    ShortBuffer greenRampData = rampData.slice();
    rampData.position(2 * size);
    rampData.limit(3 * size);
    ShortBuffer blueRampData = rampData.slice();

    X11Lib.XF86VidModeSetGammaRamp(display,
                                X11Lib.DefaultScreen(display),
                                size,
                                redRampData,
                                greenRampData,
                                blueRampData);
  }
}
