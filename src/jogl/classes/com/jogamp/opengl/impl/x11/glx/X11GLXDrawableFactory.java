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

package com.jogamp.opengl.impl.x11.glx;

import java.nio.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import javax.media.opengl.*;

import com.jogamp.opengl.impl.*;
import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.util.*;
import com.jogamp.nativewindow.impl.ProxySurface;
import com.jogamp.nativewindow.impl.x11.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class X11GLXDrawableFactory extends GLDrawableFactoryImpl {
  
  private static final DesktopGLDynamicLookupHelper x11GLXDynamicLookupHelper;
  static final VersionNumber versionOneThree = new VersionNumber(1, 3, 0);

  static {
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

  public GLDynamicLookupHelper getGLDynamicLookupHelper(int profile) {
      return x11GLXDynamicLookupHelper;
  }

  public X11GLXDrawableFactory() {
    super();
    // Register our GraphicsConfigurationFactory implementations
    // The act of constructing them causes them to be registered
    new X11GLXGraphicsConfigurationFactory();
    if(GLProfile.isAWTAvailable()) {
        try {
          ReflectionUtil.createInstance("com.jogamp.opengl.impl.x11.glx.awt.X11AWTGLXGraphicsConfigurationFactory",
                                        null, getClass().getClassLoader());
        } catch (JogampRuntimeException jre) { /* n/a .. */ }
    }

    defaultDevice = new X11GraphicsDevice(X11Util.getNullDisplayName(), AbstractGraphicsDevice.DEFAULT_UNIT);

    // Init shared resources via own thread
    // Will be released via ShutdownHook
    sharedResourcesRunner = new SharedResourcesRunner();    
    sharedResourcesThread = new Thread(sharedResourcesRunner, Thread.currentThread().getName()+"-SharedResourcesRunner");
    sharedResourcesThread.setDaemon(true); // Allow JVM to exit, even if this one is running
    sharedResourcesThread.start();
  }

  class SharedResourcesRunner implements Runnable {
      boolean ready = false;
      boolean released = false;
      boolean shouldRelease = false;
      String initConnection = null;
      SharedResource result = null;

      public final void initializeAndWait(String connection) {
          // wait until thread becomes ready to init new device,
          // pass the device and release the sync
          String threadName = Thread.currentThread().getName();
          if (DEBUG) {
              System.err.println(threadName+ " initializeAndWait START: "+connection);
          }
          synchronized (this) {
              while (!ready) {
                  try {
                      this.wait();
                  } catch (InterruptedException ex) { }
              }
              if (DEBUG) {
                  System.err.println(threadName+ " initializeAndWait set command: "+connection);
              }
              initConnection = connection;
              this.notifyAll();

              // wait until thread has initialized the device
              while (!ready || null != initConnection) {
                  try {
                      this.wait();
                  } catch (InterruptedException ex) { }
              }
              if (DEBUG) {
                  System.err.println(threadName+ " initializeAndWait done: "+connection);
              }
          }
          // done
      }

      public final void releaseAndWait() {
          synchronized (this) {
              shouldRelease = true;
              this.notifyAll();

              while (!released) {
                  try {
                      this.wait();
                  } catch (InterruptedException ex) {
                  }
              }
          }
      }

      public final void run() {
          String threadName = Thread.currentThread().getName();

          synchronized (this) {
              if (DEBUG) {
                  System.err.println(threadName+ " STARTED -> ready");
              }

              while (!shouldRelease) {
                  try {
                      // wait for stop or init
                      ready = true;
                      this.wait();
                  } catch (InterruptedException ex) { }
                  ready = false;

                  if(!shouldRelease && null!=initConnection) {
                      if (DEBUG) {
                          System.err.println(threadName+ " create Shared for: "+initConnection);
                      }
                      SharedResource sr = createSharedResource(initConnection);
                      if(null!=sr) {
                        synchronized(sharedMap) {
                            sharedMap.put(initConnection, sr);
                        }
                      }
                      if (DEBUG) {
                          String msg = "Info: (" + threadName + ") initializedSharedResource for device connection: "+initConnection+" -> ready";
                          System.err.println(msg);
                          Throwable t = new Throwable(msg);
                          t.printStackTrace();
                      }
                  }
                  initConnection = null;
                  notifyAll();
              }

              if (DEBUG) {
                  System.err.println(threadName+ " release START");
              }

              releaseSharedResources();

              if (DEBUG) {
                  System.err.println(threadName+ " release END");
              }

              released = true;
              ready = false;
              notifyAll();
          }
      }
          
      private final SharedResource createSharedResource(String connection) {
          X11GraphicsDevice sharedDevice = new X11GraphicsDevice(X11Util.createDisplay(connection), AbstractGraphicsDevice.DEFAULT_UNIT);
          sharedDevice.setCloseDisplay(true);
          sharedDevice.lock();
          try {
              String glXVendorName = GLXUtil.getVendorName(sharedDevice.getHandle());
              X11GraphicsScreen sharedScreen = new X11GraphicsScreen(sharedDevice, 0);
              X11DummyGLXDrawable sharedDrawable = X11DummyGLXDrawable.create(sharedScreen, X11GLXDrawableFactory.this,
                                                                              GLProfile.getDefault(sharedDevice));
              if (null == sharedScreen || null == sharedDrawable) {
                  throw new GLException("Couldn't init shared screen(" + sharedScreen + ")/drawable(" + sharedDrawable + ")");
              }
              X11GLXContext sharedContext;
              VersionNumber glXVersion;
              try {
                  X11GLXContext ctx = (X11GLXContext) sharedDrawable.createContext(null);
                  ctx.setSynchronized(true);
                  ctx.makeCurrent();
                  {
                      int[] major = new int[1];
                      int[] minor = new int[1];
                      GLXUtil.getGLXVersion(sharedDevice.getHandle(), major, minor);
                      glXVersion = new VersionNumber(major[0], minor[0], 0);
                  }
                  ctx.release();
                  sharedContext = ctx;
              } catch (Throwable t) {
                  throw new GLException("X11GLXDrawableFactory - Could not initialize shared resources", t);
              }
              if (null == sharedContext) {
                  throw new GLException("X11GLXDrawableFactory - Shared Context is null");
              }
              if (DEBUG) {
                  System.err.println("!!! SharedDevice:  "+sharedDevice);
                  System.err.println("!!! SharedScreen:  "+sharedScreen);
                  System.err.println("!!! SharedContext: "+sharedContext);
                  System.err.println("!!! GLX Vendor:    "+glXVendorName);
                  System.err.println("!!! GLX Version:   "+glXVersion + 
                                     " >= 1.3: " + ( glXVersion.compareTo(versionOneThree) >= 0 ) );
              }
              return new SharedResource(sharedDevice, sharedScreen, sharedDrawable, sharedContext, glXVersion, glXVendorName);
          } finally {
              sharedDevice.unlock();
          }
      }

      private final void releaseSharedResources() {
          Collection/*<SharedResource>*/ sharedResources = sharedMap.values();
          for(Iterator iter=sharedResources.iterator(); iter.hasNext(); ) {
              SharedResource sr = (SharedResource) iter.next();

              if (DEBUG) {
                  System.err.println("!!! Shutdown Shared:");
                  System.err.println("!!!   Device  : "+sr.device);
                  System.err.println("!!!   Screen  : "+sr.screen);
                  System.err.println("!!!   Drawable: "+sr.drawable);
                  System.err.println("!!!   CTX     : "+sr.context);
              }

              if (null != sr.context) {
                  // may cause JVM SIGSEGV: sharedContext.destroy();
                  sr.context = null;
              }

              if (null != sr.drawable) {
                  // may cause JVM SIGSEGV: sharedDrawable.destroy();
                  sr.drawable = null;
              }

              if (null != sr.screen) {
                  sr.screen = null;
              }

              if (null != sr.device) {
                  sr.device.close();
                  sr.device=null;
              }
          }
          sharedMap.clear();
      }
  }
  Thread sharedResourcesThread = null;
  SharedResourcesRunner sharedResourcesRunner=null;

  static class SharedResource {
      X11GraphicsDevice device;
      X11GraphicsScreen screen;
      X11DummyGLXDrawable drawable;
      X11GLXContext context;
      String glXVendorName;
      boolean isGLXVendorATI;
      boolean isGLXVendorNVIDIA;
      VersionNumber glXVersion;

      SharedResource(X11GraphicsDevice dev, X11GraphicsScreen scrn, 
                     X11DummyGLXDrawable draw, X11GLXContext ctx,
                     VersionNumber glXVer, String glXVendor) {
          device = dev;
          screen = scrn;
          drawable = draw;
          context = ctx;
          glXVersion = glXVer;
          glXVendorName = glXVendor;
          isGLXVendorATI = GLXUtil.isVendorATI(glXVendorName);
          isGLXVendorNVIDIA = GLXUtil.isVendorNVIDIA(glXVendorName);
      }
      X11GraphicsDevice getDevice() { return device; }
      X11GraphicsScreen getScreen() { return screen; }
      X11GLXContext getContext() { return context; }
      String getGLXVendorName() { return glXVendorName; }
      boolean isGLXVendorATI() { return isGLXVendorATI; }
      boolean isGLXVendorNVIDIA() { return isGLXVendorNVIDIA; }
      VersionNumber getGLXVersion() { return glXVersion; }
  }
  HashMap/*<connection, SharedResource>*/ sharedMap = new HashMap();
  X11GraphicsDevice defaultDevice;

  public final AbstractGraphicsDevice getDefaultDevice() {
      return defaultDevice;
  }

  public final boolean getIsDeviceCompatible(AbstractGraphicsDevice device) {
      if(device instanceof X11GraphicsDevice) {
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

  private SharedResource getOrCreateShared(AbstractGraphicsDevice device) {
    String connection = device.getConnection();
    SharedResource sr;
    synchronized(sharedMap) {
      sr = (SharedResource) sharedMap.get(connection);
    }

    if(null==sr && !getDeviceTried(connection)) {
        addDeviceTried(connection);
        if (DEBUG) {
          System.err.println("getOrCreateShared() "+connection+": trying");
        }
        sharedResourcesRunner.initializeAndWait(connection);
        synchronized(sharedMap) {
            sr = (SharedResource) sharedMap.get(connection);
        }
        if(DEBUG) {
            Throwable t = new Throwable("getOrCreateSharedl() "+connection+": done");
            t.printStackTrace();
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

  protected final long getOrCreateSharedDpy(AbstractGraphicsDevice device) {
    SharedResource sr = getOrCreateShared(device);
    if(null!=sr) {
        return sr.getDevice().getHandle();
    }
    return 0;
  }

  protected final VersionNumber getGLXVersion(AbstractGraphicsDevice device) {
    SharedResource sr = getOrCreateShared(device);
    if(null!=sr) {
      return sr.getGLXVersion();
    }
    return null;
  }

  protected final String getGLXVendorName(AbstractGraphicsDevice device) {
    SharedResource sr = getOrCreateShared(device);
    if(null!=sr) {
      return sr.getGLXVendorName();
    }
    return GLXUtil.getVendorName(device.getHandle());
  }

  protected final boolean isGLXVendorATI(AbstractGraphicsDevice device) {
    SharedResource sr = getOrCreateShared(device);
    if(null!=sr) {
      return sr.isGLXVendorATI();
    }
    return GLXUtil.isVendorATI(device.getHandle());
  }

  protected final boolean isGLXVendorNVIDIA(AbstractGraphicsDevice device) {
    SharedResource sr = getOrCreateShared(device);
    if(null!=sr) {
      return sr.isGLXVendorNVIDIA();
    }
    return GLXUtil.isVendorNVIDIA(device.getHandle());
  }

  protected final void shutdownInstance() {
    sharedResourcesRunner.releaseAndWait();

    // Don't really close pending Display connections,
    // since this may trigger a JVM exception
    X11Util.shutdown( false, DEBUG );
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
    AbstractGraphicsConfiguration config = target.getGraphicsConfiguration().getNativeGraphicsConfiguration();
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
    SharedResource sr = getOrCreateShared(device);
    if( null!=sr && sr.isGLXVendorATI() && null == GLContext.getCurrent() ) {
        synchronized(sr.context) {
            sr.context.makeCurrent();
            try {
                pbufferDrawable = new X11PbufferGLXDrawable(this, target);
            } finally {
                sr.context.release();
            }
        }
    } else {
        pbufferDrawable = new X11PbufferGLXDrawable(this, target);
    }
    return pbufferDrawable;
  }

  public final boolean glXVersionGreaterEqualOneThree(AbstractGraphicsDevice device) {
      VersionNumber glXVersion = getGLXVersion(device);
      return ( null != glXVersion ) ? glXVersion.compareTo(versionOneThree) >= 0 : false ;
  }

  public final boolean canCreateGLPbuffer(AbstractGraphicsDevice device) {
      if(null == device) {
        SharedResource sr = getOrCreateShared(defaultDevice);
        if(null!=sr) {
            device = sr.getDevice();
        }
      }
      return glXVersionGreaterEqualOneThree(device);
  }

  protected final NativeSurface createOffscreenSurfaceImpl(AbstractGraphicsDevice device,
                                                           GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested,
                                                           GLCapabilitiesChooser chooser,
                                                           int width, int height) {
    X11GraphicsScreen screen = null;
    SharedResource sr = getOrCreateShared(device);
    if(null!=sr) {
        screen = sr.getScreen();
    }
    if(null==screen) {
        return null;
    }

    ProxySurface ns = new ProxySurface(
               X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, screen) );
    if(ns != null) {
        ns.setSize(width, height);
    }
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
    boolean res = X11Util.XF86VidModeGetGammaRampSize(display,
                                                      X11Util.DefaultScreen(display),
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

    boolean res = X11Util.XF86VidModeSetGammaRamp(display,
                                              X11Util.DefaultScreen(display),
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

    boolean res = X11Util.XF86VidModeGetGammaRamp(display,
                                              X11Util.DefaultScreen(display),
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

    X11Util.XF86VidModeSetGammaRamp(display,
                                X11Util.DefaultScreen(display),
                                size,
                                redRampData,
                                greenRampData,
                                blueRampData);
  }
}
