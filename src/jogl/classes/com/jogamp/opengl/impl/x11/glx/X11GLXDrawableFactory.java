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

public class X11GLXDrawableFactory extends GLDrawableFactoryImpl {
  
  private static final DesktopGLDynamicLookupHelper x11GLXDynamicLookupHelper;

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
    try {
      ReflectionUtil.createInstance("com.jogamp.opengl.impl.x11.glx.awt.X11AWTGLXGraphicsConfigurationFactory", 
                                    null, getClass().getClassLoader());
    } catch (JogampRuntimeException jre) { /* n/a .. */ }

    // init shared resources ..
    sharedResourcesRunner = new SharedResourcesRunner();
    sharedResourcesThread = new Thread(sharedResourcesRunner, Thread.currentThread().getName()+"-SharedResourcesRunner");
    sharedResourcesThread.start();
    sharedResourcesRunner.waitUntilInitialized();

    if (DEBUG) {
      System.err.println("!!! Vendor: "+vendorName+", ATI: "+isVendorATI+", NV: "+isVendorNVIDIA);
      System.err.println("!!! SharedScreen: "+sharedScreen);
      System.err.println("!!! SharedContext: "+sharedContext);
    }
  }

  class SharedResourcesRunner implements Runnable {
      boolean initialized = false;
      boolean released = false;
      boolean shouldRelease = false;

      public void waitUntilInitialized() {
          synchronized (this) {
              while (!this.initialized) {
                  try {
                      this.wait();
                  } catch (InterruptedException ex) {
                  }
              }
          }
      }

      public void releaseAndWait() {
          synchronized (this) {
              this.shouldRelease = true;
              this.notifyAll();

              while (!this.released) {
                  try {
                      this.wait();
                  } catch (InterruptedException ex) {
                  }
              }
          }
      }

      public void run() {
          String threadName = Thread.currentThread().getName();
          synchronized (this) {
              if (DEBUG) {
                  System.err.println(threadName+ " initializing START");
              }
              long tlsDisplay = X11Util.createDisplay(null);
              X11Util.lockDefaultToolkit(tlsDisplay); // OK
              try {
                  X11GraphicsDevice sharedDevice = new X11GraphicsDevice(tlsDisplay);
                  vendorName = GLXUtil.getVendorName(sharedDevice.getHandle());
                  isVendorATI = GLXUtil.isVendorATI(vendorName);
                  isVendorNVIDIA = GLXUtil.isVendorNVIDIA(vendorName);
                  sharedScreen = new X11GraphicsScreen(sharedDevice, 0);
                  sharedDrawable = new X11DummyGLXDrawable(sharedScreen, X11GLXDrawableFactory.this, GLProfile.getDefault());
                  /* if(isVendorATI() && GLProfile.isAWTAvailable()) {
                    X11Util.markDisplayUncloseable(tlsDisplay); // failure to close with ATI and AWT usage
                  } */
                  if (null == sharedScreen || null == sharedDrawable) {
                      throw new GLException("Couldn't init shared screen(" + sharedScreen + ")/drawable(" + sharedDrawable + ")");
                  }
                  // We have to keep this within this thread,
                  // since we have a 'chicken-and-egg' problem otherwise on the <init> lock of this thread.
                  try {
                      X11GLXContext ctx = (X11GLXContext) sharedDrawable.createContext(null);
                      ctx.makeCurrent();
                      ctx.release();
                      sharedContext = ctx;
                  } catch (Throwable t) {
                      throw new GLException("X11GLXDrawableFactory - Could not initialize shared resources", t);
                  }
                  if (null == sharedContext) {
                      throw new GLException("X11GLXDrawableFactory - Shared Context is null");
                  }
              } finally {
                  X11Util.unlockDefaultToolkit(tlsDisplay); // OK
              }

              if (DEBUG) {
                  System.err.println(threadName+ " initializing DONE");
              }
              initialized = true;
              notifyAll();
          }

          if (DEBUG) {
              System.err.println(threadName+ " release WAIT");
          }

          synchronized(this) {
              while (!this.shouldRelease) {
                  try {
                      this.wait();
                  } catch (InterruptedException ex) {
                  }
              }
              if (DEBUG) {
                  System.err.println(threadName+ " release START");
              }
              if (null != sharedContext) {
                  // may cause JVM SIGSEGV: sharedContext.destroy();
                  sharedContext = null;
              }

              if (null != sharedDrawable) {
                  // may cause JVM SIGSEGV: sharedDrawable.destroy();
                  sharedDrawable = null;
              }
              if (null != sharedScreen) {
                  // may cause JVM SIGSEGV:
                  X11Util.closeDisplay(sharedScreen.getDevice().getHandle());
                  sharedScreen = null;
              }

              if (DEBUG) {
                  System.err.println(threadName+ " release END");
              }
              released = true;
              notifyAll();
          }
      }
  }

  Thread sharedResourcesThread = null;
  SharedResourcesRunner sharedResourcesRunner=null;
  private X11GraphicsScreen sharedScreen=null;
  private X11DummyGLXDrawable sharedDrawable=null;
  private X11GLXContext sharedContext=null;
  private String vendorName;
  private boolean isVendorATI;
  private boolean isVendorNVIDIA;

  protected String getVendorName() { return vendorName; }
  protected boolean isVendorATI() { return isVendorATI; }
  protected boolean isVendorNVIDIA() { return isVendorNVIDIA; }

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
          System.err.println("!!!          Screen  : "+sharedScreen);
    }

    sharedResourcesRunner.releaseAndWait();

    X11Util.shutdown( true, DEBUG );
  }

  protected GLDrawableImpl createOnscreenDrawableImpl(NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new X11OnscreenGLXDrawable(this, target);
  }

  protected GLDrawableImpl createOffscreenDrawableImpl(NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new X11OffscreenGLXDrawable(this, target);
  }

  public boolean canCreateGLPbuffer(AbstractGraphicsDevice device) { 
      return glxVersionGreaterEqualThan(device, 1, 3); 
  }

  private boolean glxVersionsQueried = false;
  private int     glxVersionMajor=0, glxVersionMinor=0;
  protected final boolean glxVersionGreaterEqualThan(AbstractGraphicsDevice device, int majorReq, int minorReq) {
    if (!glxVersionsQueried) {
        if(null == device) {
            device = (X11GraphicsDevice) sharedScreen.getDevice();
        }
        if(null == device) {
            throw new GLException("FIXME: No AbstractGraphicsDevice (passed or shared-device");
        }
        device.lock(); // OK
        try {
            long display = device.getHandle();
            int[] major = new int[1];
            int[] minor = new int[1];

            GLXUtil.getGLXVersion(display, major, minor);
            if (DEBUG) {
              System.err.println("!!! GLX version: major " + major[0] +
                                 ", minor " + minor[0]);
            }

            glxVersionMajor = major[0];
            glxVersionMinor = minor[0];
            glxVersionsQueried = true;
        } finally {
            device.unlock(); // OK
        }
    }
    return ( glxVersionMajor > majorReq ) || ( glxVersionMajor == majorReq && glxVersionMinor >= minorReq ) ;
  }

  protected GLDrawableImpl createGLPbufferDrawableImpl(final NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }

    GLDrawableImpl pbufferDrawable;

    /** 
     * Due to the ATI Bug https://bugzilla.mozilla.org/show_bug.cgi?id=486277,
     * we need to have a context current on the same Display to create a PBuffer.
     * The dummy context shall also use the same Display,
     * since switching Display in this regard is another ATI bug.
     */
    if( isVendorATI() && null == GLContext.getCurrent() ) {
        synchronized(sharedContext) {
            sharedContext.makeCurrent();
            try {
                pbufferDrawable = new X11PbufferGLXDrawable(this, target);
            } finally {
                sharedContext.release();
            }
        }
    } else {
        pbufferDrawable = new X11PbufferGLXDrawable(this, target);
    }
    return pbufferDrawable;
  }


  protected NativeSurface createOffscreenSurfaceImpl(GLCapabilities capabilities, GLCapabilitiesChooser chooser, int width, int height) {
    ProxySurface ns = new ProxySurface(X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capabilities, chooser, sharedScreen));
    if(ns != null) {
        ns.setSize(width, height);
    }
    return ns;
  }

  protected GLContext createExternalGLContextImpl() {
    return X11ExternalGLXContext.create(this, null);
  }

  public boolean canCreateExternalGLDrawable(AbstractGraphicsDevice device) {
    return canCreateGLPbuffer(device);
  }

  protected GLDrawable createExternalGLDrawableImpl() {
    return X11ExternalGLXDrawable.create(this, null);
  }

  public boolean canCreateContextOnJava2DSurface(AbstractGraphicsDevice device) {
    return false;
  }

  public GLContext createContextOnJava2DSurface(Object graphics, GLContext shareWith)
    throws GLException {
    throw new GLException("Unimplemented on this platform");
  }

  //----------------------------------------------------------------------
  // Gamma-related functionality
  //

  private boolean gotGammaRampLength;
  private int gammaRampLength;
  protected synchronized int getGammaRampLength() {
    if (gotGammaRampLength) {
      return gammaRampLength;
    }

    long display = sharedScreen.getDevice().getHandle();

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

  protected boolean setGammaRamp(float[] ramp) {
    int len = ramp.length;
    short[] rampData = new short[len];
    for (int i = 0; i < len; i++) {
      rampData[i] = (short) (ramp[i] * 65535);
    }

    long display = sharedScreen.getDevice().getHandle();
    boolean res = X11Util.XF86VidModeSetGammaRamp(display,
                                              X11Util.DefaultScreen(display),
                                              rampData.length,
                                              rampData, 0,
                                              rampData, 0,
                                              rampData, 0);
    return res;
  }

  protected Buffer getGammaRamp() {
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
    long display = sharedScreen.getDevice().getHandle();
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

  protected void resetGammaRamp(Buffer originalGammaRamp) {
    if (originalGammaRamp == null)
      return; // getGammaRamp failed originally
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
    long display = sharedScreen.getDevice().getHandle();
    X11Util.XF86VidModeSetGammaRamp(display,
                                X11Util.DefaultScreen(display),
                                size,
                                redRampData,
                                greenRampData,
                                blueRampData);
  }
}
