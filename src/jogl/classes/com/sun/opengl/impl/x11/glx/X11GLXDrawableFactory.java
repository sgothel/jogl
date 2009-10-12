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

package com.sun.opengl.impl.x11.glx;

import java.nio.*;
import java.security.*;
import java.util.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import javax.media.opengl.*;
import com.sun.gluegen.runtime.*;
import com.sun.gluegen.runtime.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.opengl.impl.x11.glx.*;
import com.sun.nativewindow.impl.NullWindow;
import com.sun.nativewindow.impl.NWReflection;
import com.sun.nativewindow.impl.x11.*;

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
      NWReflection.createInstance("com.sun.opengl.impl.x11.glx.awt.X11AWTGLXGraphicsConfigurationFactory",
                                  new Object[] {});
    } catch (Throwable t) { }
  }

  public GLDrawableImpl createOnscreenDrawable(NativeWindow target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new X11OnscreenGLXDrawable(this, target);
  }

  protected GLDrawableImpl createOffscreenDrawable(NativeWindow target) {
    return new X11OffscreenGLXDrawable(this, target);
  }

  private boolean pbufferSupportInitialized = false;
  private boolean canCreateGLPbuffer = false;
  public boolean canCreateGLPbuffer() {
    if (!pbufferSupportInitialized) {
        long display = X11Util.getThreadLocalDefaultDisplay();
        int[] major = new int[1];
        int[] minor = new int[1];
        int screen = 0; // FIXME: provide way to specify this?

        if (!GLX.glXQueryVersion(display, major, 0, minor, 0)) {
          throw new GLException("glXQueryVersion failed");
        }
        if (DEBUG) {
          System.err.println("!!! GLX version: major " + major[0] +
                             ", minor " + minor[0]);
        }

        // Work around bugs in ATI's Linux drivers where they report they
        // only implement GLX version 1.2 on the server side
        if (major[0] == 1 && minor[0] == 2) {
          String str = GLX.glXGetClientString(display, GLX.GLX_VERSION);
          if (str != null && str.startsWith("1.") &&
             (str.charAt(2) >= '3')) {
            canCreateGLPbuffer = true;
          }
        } else {
          canCreateGLPbuffer = ((major[0] > 1) || (minor[0] > 2));
        }

        pbufferSupportInitialized = true;        
    }
    return canCreateGLPbuffer;
  }

  protected GLDrawableImpl createGLPbufferDrawableImpl(final NativeWindow target) {
    /** 
     * FIXME: Think about this ..
     * should not be necessary ? ..
    final List returnList = new ArrayList();
    final GLDrawableFactory factory = this;
    Runnable r = new Runnable() {
        public void run() {
          returnList.add(new X11PbufferGLXDrawable(factory, target));
        }
      };
    maybeDoSingleThreadedWorkaround(r);
    return (GLDrawableImpl) returnList.get(0);
    */
    return new X11PbufferGLXDrawable(this, target);
  }


  protected NativeWindow createOffscreenWindow(GLCapabilities capabilities, GLCapabilitiesChooser chooser, int width, int height) {
    AbstractGraphicsScreen screen = X11GraphicsScreen.createDefault();
    NullWindow nw = new NullWindow(X11GLXGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capabilities, chooser, screen));
    nw.setSize(width, height);
    return nw;
  }

  public GLContext createExternalGLContext() {
    return X11ExternalGLXContext.create(this, null);
  }

  public boolean canCreateExternalGLDrawable() {
    return canCreateGLPbuffer();
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

  public boolean canCreateContextOnJava2DSurface() {
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

    long display = X11Util.getThreadLocalDefaultDisplay();
    try {
        X11Lib.XLockDisplay(display);
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

    long display = X11Util.getThreadLocalDefaultDisplay();
    try {
        X11Lib.XLockDisplay(display);
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
    long display = X11Util.getThreadLocalDefaultDisplay();
    try {
        X11Lib.XLockDisplay(display);
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
    long display = X11Util.getThreadLocalDefaultDisplay();
    try {
        X11Lib.XLockDisplay(display);
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
