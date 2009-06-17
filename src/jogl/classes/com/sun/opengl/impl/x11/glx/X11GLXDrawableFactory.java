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
import com.sun.nativewindow.impl.jawt.x11.*;

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

  public GLDrawable createGLDrawable(NativeWindow target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    target = NativeWindowFactory.getNativeWindow(target, null);
    return new X11OnscreenGLXDrawable(this, target);
  }

  public GLDrawableImpl createOffscreenDrawable(GLCapabilities capabilities,
                                                GLCapabilitiesChooser chooser,
                                                int width,
                                                int height) {
    AbstractGraphicsScreen screen = X11GraphicsScreen.createDefault();
    return new X11OffscreenGLXDrawable(this, screen, capabilities, chooser, width, height);
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

  public GLPbuffer createGLPbuffer(final GLCapabilities capabilities,
                                   final GLCapabilitiesChooser chooser,
                                   final int initialWidth,
                                   final int initialHeight,
                                   final GLContext shareWith) {
    if (!canCreateGLPbuffer()) {
      throw new GLException("Pbuffer support not available with current graphics card");
    }
    final List returnList = new ArrayList();
    final GLDrawableFactory factory = this;
    Runnable r = new Runnable() {
        public void run() {
          AbstractGraphicsScreen screen = X11GraphicsScreen.createDefault();
          X11PbufferGLXDrawable pbufferDrawable = new X11PbufferGLXDrawable(factory, screen, capabilities, chooser,
                                                                            initialWidth,
                                                                            initialHeight);
          GLPbufferImpl pbuffer = new GLPbufferImpl(pbufferDrawable, shareWith);
          returnList.add(pbuffer);
        }
      };
    maybeDoSingleThreadedWorkaround(r);
    return (GLPbuffer) returnList.get(0);
  }

  public GLContext createExternalGLContext() {
    AbstractGraphicsScreen screen = X11GraphicsScreen.createDefault();
    return new X11ExternalGLXContext(screen);
  }

  public boolean canCreateExternalGLDrawable() {
    return canCreateGLPbuffer();
  }

  public GLDrawable createExternalGLDrawable() {
    AbstractGraphicsScreen screen = X11GraphicsScreen.createDefault();
    return X11ExternalGLXDrawable.create(this, screen);
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

  private void maybeDoSingleThreadedWorkaround(Runnable action) {
    if (Threading.isSingleThreaded() &&
        !Threading.isOpenGLThread()) {
      Threading.invokeOnOpenGLThread(action);
    } else {
      action.run();
    }
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

    int[] size = new int[1];
    long display = X11Util.getThreadLocalDefaultDisplay();
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

  protected boolean setGammaRamp(float[] ramp) {
    int len = ramp.length;
    short[] rampData = new short[len];
    for (int i = 0; i < len; i++) {
      rampData[i] = (short) (ramp[i] * 65535);
    }

    long display = X11Util.getThreadLocalDefaultDisplay();
    boolean res = X11Lib.XF86VidModeSetGammaRamp(display,
                                              X11Lib.DefaultScreen(display),
                                              rampData.length,
                                              rampData, 0,
                                              rampData, 0,
                                              rampData, 0);
    return res;
  }

  protected Buffer getGammaRamp() {
    int size = getGammaRampLength();
    ShortBuffer rampData = ShortBuffer.allocate(3 * size);
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
    X11Lib.XF86VidModeSetGammaRamp(display,
                                X11Lib.DefaultScreen(display),
                                size,
                                redRampData,
                                greenRampData,
                                blueRampData);
  }
}
