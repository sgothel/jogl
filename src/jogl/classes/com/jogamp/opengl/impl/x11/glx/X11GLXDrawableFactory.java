/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

import com.jogamp.common.os.DynamicLookupHelper;
import java.nio.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import javax.media.opengl.*;

import com.jogamp.opengl.impl.*;
import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.util.*;
import com.jogamp.nativewindow.impl.NullWindow;
import com.jogamp.nativewindow.impl.x11.*;

public class X11GLXDrawableFactory extends GLDrawableFactoryImpl implements DynamicLookupHelper {
  
  static {
    X11Util.initSingleton(); // ensure it's loaded and setup
  }

  public X11GLXDrawableFactory() {
    super();
    // Must initialize GLX support eagerly in case a pbuffer is the
    // first thing instantiated
    GLX.getGLXProcAddressTable().reset(this);
    // Register our GraphicsConfigurationFactory implementations
    // The act of constructing them causes them to be registered
    new X11GLXGraphicsConfigurationFactory();
    try {
      ReflectionUtil.createInstance("com.jogamp.opengl.impl.x11.glx.awt.X11AWTGLXGraphicsConfigurationFactory",
                                  new Object[] {});
    } catch (JogampRuntimeException jre) { /* n/a .. */ }

    shareableResourceThread = new ShareableResourceThread(GLProfile.getDefault(), GLProfile.isAWTJOGLAvailable());
    shareableResourceThread.start();
    while (!shareableResourceThread.isInitialized()) {
        synchronized(shareableResourceThread) {
            try {
                shareableResourceThread.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    if(null==sharedScreen || null==sharedDrawable) {
        throw new GLException("Couldn't init shared screen("+sharedScreen+")/drawable("+sharedDrawable+")");
    }
    // We have to keep this within this thread,
    // since we have a 'chicken-and-egg' problem otherwise on the <init> lock of this thread.
    X11Util.XLockDisplay(sharedScreen.getDevice().getHandle());
    try{
        X11GLXContext ctx  = (X11GLXContext) sharedDrawable.createContext(null);
        ctx.makeCurrent();
        ctx.release();
        sharedContext = ctx;
    } catch (Throwable t) {
        throw new GLException("X11GLXDrawableFactory - Could not initialize shared resources", t);
    } finally {
        X11Util.XUnlockDisplay(sharedScreen.getDevice().getHandle());
    }
    if(null==sharedContext) {
        throw new GLException("X11GLXDrawableFactory - Shared Context is null");
    }
    if (DEBUG) {
      System.err.println("!!! Vendor: "+vendorName+", ATI: "+isVendorATI+", NV: "+isVendorNVIDIA);
      System.err.println("!!! SharedScreen: "+sharedScreen);
      System.err.println("!!! SharedContext: "+sharedContext);
    }
  }

  ShareableResourceThread shareableResourceThread;

  class ShareableResourceThread extends Thread {
        volatile boolean shutdown  = false;
        volatile boolean initialized = false;
        GLProfile glp;
        boolean mayUseAWT;

        final void shutdown() { shutdown = true; }
        final boolean isInitialized() { return initialized; }

        public ShareableResourceThread(GLProfile glp, boolean mayUseAWT) {
            super("ShareableResourceThread-"+Thread.currentThread().getName());
            this.glp = glp;
            this.mayUseAWT = mayUseAWT;
        }

        public void run() {
            synchronized(this) {
                long tlsDisplay = X11Util.createThreadLocalDisplay(null);
                X11GraphicsDevice sharedDevice = new X11GraphicsDevice(tlsDisplay);
                vendorName = GLXUtil.getVendorName(sharedDevice.getHandle());
                isVendorATI = GLXUtil.isVendorATI(vendorName);
                isVendorNVIDIA = GLXUtil.isVendorNVIDIA(vendorName);
                sharedScreen = new X11GraphicsScreen(sharedDevice, 0);
                X11Util.XLockDisplay(sharedScreen.getDevice().getHandle());
                try{
                    sharedDrawable = new X11DummyGLXDrawable(sharedScreen, X11GLXDrawableFactory.this, glp);
                } finally {
                    X11Util.XUnlockDisplay(sharedScreen.getDevice().getHandle());
                }
                if(isVendorATI() && mayUseAWT) {
                    X11Util.markThreadLocalDisplayUncloseable(tlsDisplay); // failure to close with ATI and AWT usage
                }
                initialized = true;
                this.notifyAll();

                while (!shutdown) {
                    synchronized(this) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if(null!=sharedDrawable) {
                    sharedDrawable.destroy();
                    sharedDrawable=null;
                }
                if(null!=sharedScreen) {
                     X11Util.closeThreadLocalDisplay(null);
                     sharedScreen = null;
                     sharedDevice=null;
                }
                // don't close pending XDisplay, since they might be a different thread as the opener
                X11Util.shutdown( false, DEBUG );

                initialized = false;
                this.notifyAll();
            }
        }
  }

  private X11GraphicsScreen sharedScreen;
  private String vendorName;
  private boolean isVendorATI;
  private boolean isVendorNVIDIA;

  public String getVendorName() { return vendorName; }
  public boolean isVendorATI() { return isVendorATI; }
  public boolean isVendorNVIDIA() { return isVendorNVIDIA; }

  private X11DummyGLXDrawable sharedDrawable=null;
  private X11GLXContext sharedContext=null;

  protected final GLDrawableImpl getSharedDrawable() {
    return sharedDrawable; 
  }

  protected final GLContextImpl getSharedContext() {
    return sharedContext; 
  }

  protected void shutdown() {
    if (DEBUG) {
          System.err.println("!!! Shutdown Shared:");
          System.err.println("!!!          CTX     : "+sharedContext);
          System.err.println("!!!          Drawable: "+sharedDrawable);
          System.err.println("!!!          Screen  : "+sharedScreen);
    }
    if(null!=sharedContext) {
        sharedContext.destroy(); // implies release, if current
        sharedContext=null;
    }
    synchronized(shareableResourceThread) {
        if (shareableResourceThread.isInitialized()) {
            shareableResourceThread.shutdown();
            shareableResourceThread.notifyAll();
            while (shareableResourceThread.isInitialized()) {
                try {
                    shareableResourceThread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    shareableResourceThread = null;
  }

  public GLDrawableImpl createOnscreenDrawable(NativeWindow target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new X11OnscreenGLXDrawable(this, target);
  }

  protected GLDrawableImpl createOffscreenDrawable(NativeWindow target) {
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
  public boolean glxVersionGreaterEqualThan(AbstractGraphicsDevice device, int majorReq, int minorReq) { 
    if (!glxVersionsQueried) {
        if(null == device) {
            device = (X11GraphicsDevice) sharedScreen.getDevice();
        }
        if(null == device) {
            throw new GLException("FIXME: No AbstractGraphicsDevice (passed or shared-device");
        }
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
    }
    return ( glxVersionMajor > majorReq ) || ( glxVersionMajor == majorReq && glxVersionMinor >= minorReq ) ;
  }

  protected GLDrawableImpl createGLPbufferDrawableImpl(final NativeWindow target) {
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
    boolean usedSharedContext=false;
    if( isVendorATI() && null == GLContext.getCurrent() ) {
        sharedContext.makeCurrent();
        usedSharedContext=true;
    }
    try {
        pbufferDrawable = new X11PbufferGLXDrawable(this, target);
    } finally {
        if(usedSharedContext) {
            sharedContext.release();
        }
    }
    return pbufferDrawable;
  }


  protected NativeWindow createOffscreenWindow(GLCapabilities capabilities, GLCapabilitiesChooser chooser, int width, int height) {
    NullWindow nw = null;
    X11Util.XLockDisplay(sharedScreen.getDevice().getHandle());
    try{
        nw = new NullWindow(X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capabilities, chooser, sharedScreen));
    }finally{
        X11Util.XUnlockDisplay(sharedScreen.getDevice().getHandle());
    }
    if(nw != null) {
        nw.setSize(width, height);
    }
    return nw;
  }

  public GLContext createExternalGLContext() {
    return X11ExternalGLXContext.create(this, null);
  }

  public boolean canCreateExternalGLDrawable(AbstractGraphicsDevice device) {
    return canCreateGLPbuffer(device);
  }

  public GLDrawable createExternalGLDrawable() {
    return X11ExternalGLXDrawable.create(this, null);
  }

  public void loadGLULibrary() {
    X11Lib.dlopen("/usr/lib/libGLU.so");
  }

  public long dynamicLookupFunction(String glFuncName) {
    long res = 0;
    res = GLX.glXGetProcAddressARB(glFuncName);
    if (res == 0) {
      // GLU routines aren't known to the OpenGL function lookup
      res = X11Lib.dlsym(glFuncName);
    }
    return res;
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

    X11Util.XLockDisplay(display);
    try {
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
    } finally {
        X11Util.XUnlockDisplay(display);
    }
  }

  protected boolean setGammaRamp(float[] ramp) {
    int len = ramp.length;
    short[] rampData = new short[len];
    for (int i = 0; i < len; i++) {
      rampData[i] = (short) (ramp[i] * 65535);
    }

    long display = sharedScreen.getDevice().getHandle();
    X11Util.XLockDisplay(display);
    try {
        boolean res = X11Lib.XF86VidModeSetGammaRamp(display,
                                                  X11Lib.DefaultScreen(display),
                                                  rampData.length,
                                                  rampData, 0,
                                                  rampData, 0,
                                                  rampData, 0);
        return res;
    } finally {
        X11Util.XUnlockDisplay(display);
    }
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
    X11Util.XLockDisplay(display);
    try {
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
    } finally {
        X11Util.XUnlockDisplay(display);
    }
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
    X11Util.XLockDisplay(display);
    try {
        X11Lib.XF86VidModeSetGammaRamp(display,
                                    X11Lib.DefaultScreen(display),
                                    size,
                                    redRampData,
                                    greenRampData,
                                    blueRampData);
    } finally {
        X11Util.XUnlockDisplay(display);
    }
  }
}
