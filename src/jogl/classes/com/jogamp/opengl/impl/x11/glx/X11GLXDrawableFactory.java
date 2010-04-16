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
import com.jogamp.gluegen.runtime.opengl.*;
import com.jogamp.opengl.impl.*;
import com.jogamp.nativewindow.impl.NullWindow;
import com.jogamp.nativewindow.impl.NWReflection;
import com.jogamp.nativewindow.impl.x11.*;

public class X11GLXDrawableFactory extends GLDrawableFactoryImpl implements DynamicLookupHelper {
  
  public X11GLXDrawableFactory() {
    super();
    // Must initialize GLX support eagerly in case a pbuffer is the
    // first thing instantiated
    GLProcAddressHelper.resetProcAddressTable(GLX.getGLXProcAddressTable(), this);
    // Register our GraphicsConfigurationFactory implementations
    // The act of constructing them causes them to be registered
    new X11GLXGraphicsConfigurationFactory();
    try {
      NWReflection.createInstance("com.jogamp.opengl.impl.x11.glx.awt.X11AWTGLXGraphicsConfigurationFactory",
                                  new Object[] {});
    } catch (Throwable t) { }

    X11GraphicsDevice sharedDevice = new X11GraphicsDevice(X11Util.createThreadLocalDisplay(null));
    vendorName = GLXUtil.getVendorName(sharedDevice.getHandle());
    isVendorATI = GLXUtil.isVendorATI(vendorName);
    isVendorNVIDIA = GLXUtil.isVendorNVIDIA(vendorName);
    if( isVendorATI() ) {
        X11Util.markGlobalDisplayUndeletable(sharedDevice.getHandle()); // ATI hack ..
    }
    sharedScreen = new X11GraphicsScreen(sharedDevice, 0);
    if (DEBUG) {
      System.err.println("!!! Vendor: "+vendorName+", ATI: "+isVendorATI+", NV: "+isVendorNVIDIA);
      System.err.println("!!! SharedScreen: "+sharedScreen);
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

  // package private ..
  final X11GLXContext getSharedContext() {
    validate();
    return sharedContext; 
  }

  private void initShared() {
    if(null==sharedDrawable) {
        X11Lib.XLockDisplay(sharedScreen.getDevice().getHandle());
        sharedDrawable = new X11DummyGLXDrawable(sharedScreen, this, null);
        X11GLXContext _sharedContext  = (X11GLXContext) sharedDrawable.createContext(null);
        {
            _sharedContext.makeCurrent();
            _sharedContext.release();
        }
        sharedContext = _sharedContext;
        X11Lib.XUnlockDisplay(sharedScreen.getDevice().getHandle());
        if (DEBUG) {
          System.err.println("!!! SharedContext: "+sharedContext);
        }
        if(null==sharedContext) {
            throw new GLException("Couldn't init shared resources");
        }
    }
  }

  public void shutdown() {
     super.shutdown();
     if (DEBUG) {
          System.err.println("!!! Shutdown Shared:");
          System.err.println("!!!          CTX     : "+sharedContext);
          System.err.println("!!!          Drawable: "+sharedDrawable);
          System.err.println("!!!          Screen  : "+sharedScreen);
          Exception e = new Exception("Debug");
          e.printStackTrace();
     }
     if(null!=sharedContext) {
        sharedContext.destroy(); // implies release, if current
     }
     if(null!=sharedDrawable) {
        sharedDrawable.destroy();
     }
     if(null!=sharedScreen) {
         X11GraphicsDevice sharedDevice = (X11GraphicsDevice) sharedScreen.getDevice();
         if(null!=sharedDevice) {
             X11Util.closeThreadLocalDisplay(null);
         }
         sharedScreen = null;
     }
     X11Util.shutdown( !isVendorATI(), DEBUG );
  }

  public GLDrawableImpl createOnscreenDrawable(NativeWindow target) {
    validate();
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    initShared();
    if( isVendorATI() ) {
        X11Util.markGlobalDisplayUndeletable(target.getDisplayHandle()); // ATI hack ..
    }
    return new X11OnscreenGLXDrawable(this, target);
  }

  protected GLDrawableImpl createOffscreenDrawable(NativeWindow target) {
    validate();
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    initShared();
    if( isVendorATI() ) {
        X11Util.markGlobalDisplayUndeletable(target.getDisplayHandle()); // ATI hack ..
    }
    initShared();
    return new X11OffscreenGLXDrawable(this, target);
  }

  public boolean canCreateGLPbuffer(AbstractGraphicsDevice device) { 
      validate();
      return glxVersionGreaterEqualThan(device, 1, 3); 
  }

  private boolean glxVersionsQueried = false;
  private int     glxVersionMajor=0, glxVersionMinor=0;
  public boolean glxVersionGreaterEqualThan(AbstractGraphicsDevice device, int majorReq, int minorReq) { 
    validate();
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
    validate();
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    initShared();

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
    if( isVendorATI() ) {
        X11Util.markGlobalDisplayUndeletable(target.getDisplayHandle()); // ATI hack ..
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
    validate();
    initShared();
    X11Lib.XLockDisplay(sharedScreen.getDevice().getHandle());
    NullWindow nw = new NullWindow(X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capabilities, chooser, sharedScreen));
    X11Lib.XUnlockDisplay(sharedScreen.getDevice().getHandle());
    nw.setSize(width, height);
    return nw;
  }

  public GLContext createExternalGLContext() {
    validate();
    initShared();
    return X11ExternalGLXContext.create(this, null);
  }

  public boolean canCreateExternalGLDrawable(AbstractGraphicsDevice device) {
    validate();
    initShared();
    return canCreateGLPbuffer(device);
  }

  public GLDrawable createExternalGLDrawable() {
    validate();
    initShared();
    return X11ExternalGLXDrawable.create(this, null);
  }

  public void loadGLULibrary() {
    validate();
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
    validate();
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

    X11Lib.XLockDisplay(display);
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
        X11Lib.XUnlockDisplay(display);
    }
  }

  protected boolean setGammaRamp(float[] ramp) {
    int len = ramp.length;
    short[] rampData = new short[len];
    for (int i = 0; i < len; i++) {
      rampData[i] = (short) (ramp[i] * 65535);
    }

    long display = sharedScreen.getDevice().getHandle();
    X11Lib.XLockDisplay(display);
    try {
        boolean res = X11Lib.XF86VidModeSetGammaRamp(display,
                                                  X11Lib.DefaultScreen(display),
                                                  rampData.length,
                                                  rampData, 0,
                                                  rampData, 0,
                                                  rampData, 0);
        return res;
    } finally {
        X11Lib.XUnlockDisplay(display);
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
    X11Lib.XLockDisplay(display);
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
        X11Lib.XUnlockDisplay(display);
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
    X11Lib.XLockDisplay(display);
    try {
        X11Lib.XF86VidModeSetGammaRamp(display,
                                    X11Lib.DefaultScreen(display),
                                    size,
                                    redRampData,
                                    greenRampData,
                                    blueRampData);
    } finally {
        X11Lib.XUnlockDisplay(display);
    }
  }
}
