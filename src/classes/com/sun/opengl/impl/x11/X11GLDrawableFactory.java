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
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.impl.x11;

import java.nio.*;
import java.security.*;
import java.util.*;
import javax.media.opengl.*;
import com.sun.gluegen.runtime.*;
import com.sun.opengl.impl.*;

public class X11GLDrawableFactory extends GLDrawableFactoryImpl {
  private static final boolean DEBUG = Debug.debug("X11GLDrawableFactory");

  // ATI's proprietary drivers apparently send GLX tokens even for
  // direct contexts, so we need to disable the context optimizations
  // in this case
  private static boolean isVendorATI;

  // Map for rediscovering the GLCapabilities associated with a
  // particular screen and visualID after the fact
  private static Map visualToGLCapsMap = Collections.synchronizedMap(new HashMap());
  
  static class ScreenAndVisualIDKey {
    private int screen;
    private long visualID;

    ScreenAndVisualIDKey(int screen,
                         long visualID) {
      this.screen = screen;
      this.visualID = visualID;
    }

    public int hashCode() {
      return (int) (screen + 13 * visualID);
    }

    public boolean equals(Object obj) {
      if ((obj == null) || (!(obj instanceof ScreenAndVisualIDKey))) {
        return false;
      }

      ScreenAndVisualIDKey key = (ScreenAndVisualIDKey) obj;
      return (screen == key.screen &&
              visualID == key.visualID);
    }

    int  screen()   { return screen; }
    long visualID() { return visualID; }
  }

  static {
    // See DRIHack.java for an explanation of why this is necessary
    DRIHack.begin();

    com.sun.opengl.impl.NativeLibLoader.loadCore();

    DRIHack.end();
  }

  public X11GLDrawableFactory(String profile) {
    super(profile);
    // Must initialize GLX support eagerly in case a pbuffer is the
    // first thing instantiated
    ProcAddressHelper.resetProcAddressTable(GLX.getGLXProcAddressTable(), this);
  }

  private static final int MAX_ATTRIBS = 128;

  public AbstractGraphicsConfiguration chooseGraphicsConfiguration(GLCapabilities capabilities,
                                                                   GLCapabilitiesChooser chooser,
                                                                   AbstractGraphicsDevice absDevice) {
    return null;
  }

  public GLDrawable createGLDrawable(NativeWindow target,
                                     GLCapabilities capabilities,
                                     GLCapabilitiesChooser chooser) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    X11OnscreenGLDrawable drawable = new X11OnscreenGLDrawable(getProfile(), target);
    int visualID = target.getVisualID();
    int screen = target.getScreenIndex(); 
    drawable.setChosenGLCapabilities((GLCapabilities) visualToGLCapsMap.get(new ScreenAndVisualIDKey(screen, visualID)));
    return drawable;
  }

  public GLDrawableImpl createOffscreenDrawable(GLCapabilities capabilities,
                                                GLCapabilitiesChooser chooser) {
    return new X11OffscreenGLDrawable(capabilities, chooser);
  }

  private boolean pbufferSupportInitialized = false;
  private boolean canCreateGLPbuffer = false;
  public boolean canCreateGLPbuffer() {
    if (!pbufferSupportInitialized) {
      Runnable r = new Runnable() {
          public void run() {
            long display = getDisplayConnection();
            lockToolkit();
            try {
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
            } finally {
              unlockToolkit();
            }
          }
        };
      maybeDoSingleThreadedWorkaround(r);
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
    Runnable r = new Runnable() {
        public void run() {
          X11PbufferGLDrawable pbufferDrawable = new X11PbufferGLDrawable(capabilities,
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
    return new X11ExternalGLContext();
  }

  public boolean canCreateExternalGLDrawable() {
    return canCreateGLPbuffer();
  }

  public GLDrawable createExternalGLDrawable() {
    return new X11ExternalGLDrawable();
  }

  public void loadGLULibrary() {
    GLX.dlopen("/usr/lib/libGLU.so");
  }

  public long dynamicLookupFunction(String glFuncName) {
    long res = 0;
    res = GLX.glXGetProcAddressARB(glFuncName);
    if (res == 0) {
      // GLU routines aren't known to the OpenGL function lookup
      res = GLX.dlsym(glFuncName);
    }
    return res;
  }

  public static GLCapabilities xvi2GLCapabilities(long display, XVisualInfo info) {
    int[] tmp = new int[1];
    int val = glXGetConfig(display, info, GLX.GLX_USE_GL, tmp, 0);
    if (val == 0) {
      // Visual does not support OpenGL
      return null;
    }
    val = glXGetConfig(display, info, GLX.GLX_RGBA, tmp, 0);
    if (val == 0) {
      // Visual does not support RGBA
      return null;
    }
    GLCapabilities res = new GLCapabilities();
    res.setDoubleBuffered(glXGetConfig(display, info, GLX.GLX_DOUBLEBUFFER,     tmp, 0) != 0);
    res.setStereo        (glXGetConfig(display, info, GLX.GLX_STEREO,           tmp, 0) != 0);
    // Note: use of hardware acceleration is determined by
    // glXCreateContext, not by the XVisualInfo. Optimistically claim
    // that all GLCapabilities have the capability to be hardware
    // accelerated.
    res.setHardwareAccelerated(true);
    res.setDepthBits     (glXGetConfig(display, info, GLX.GLX_DEPTH_SIZE,       tmp, 0));
    res.setStencilBits   (glXGetConfig(display, info, GLX.GLX_STENCIL_SIZE,     tmp, 0));
    res.setRedBits       (glXGetConfig(display, info, GLX.GLX_RED_SIZE,         tmp, 0));
    res.setGreenBits     (glXGetConfig(display, info, GLX.GLX_GREEN_SIZE,       tmp, 0));
    res.setBlueBits      (glXGetConfig(display, info, GLX.GLX_BLUE_SIZE,        tmp, 0));
    res.setAlphaBits     (glXGetConfig(display, info, GLX.GLX_ALPHA_SIZE,       tmp, 0));
    res.setAccumRedBits  (glXGetConfig(display, info, GLX.GLX_ACCUM_RED_SIZE,   tmp, 0));
    res.setAccumGreenBits(glXGetConfig(display, info, GLX.GLX_ACCUM_GREEN_SIZE, tmp, 0));
    res.setAccumBlueBits (glXGetConfig(display, info, GLX.GLX_ACCUM_BLUE_SIZE,  tmp, 0));
    res.setAccumAlphaBits(glXGetConfig(display, info, GLX.GLX_ACCUM_ALPHA_SIZE, tmp, 0));
    if (isMultisampleAvailable()) {
      res.setSampleBuffers(glXGetConfig(display, info, GLX.GLX_SAMPLE_BUFFERS_ARB, tmp, 0) != 0);
      res.setNumSamples   (glXGetConfig(display, info, GLX.GLX_SAMPLES_ARB,        tmp, 0));
    }
    return res;
  }

  public static int[] glCapabilities2AttribList(GLCapabilities caps,
                                                boolean isMultisampleAvailable,
                                                boolean pbuffer,
                                                long display,
                                                int screen) {
    int colorDepth = (caps.getRedBits() +
                      caps.getGreenBits() +
                      caps.getBlueBits());
    if (colorDepth < 15) {
      throw new GLException("Bit depths < 15 (i.e., non-true-color) not supported");
    }
    int[] res = new int[MAX_ATTRIBS];
    int idx = 0;
    if (pbuffer) {
      res[idx++] = GLXExt.GLX_DRAWABLE_TYPE;
      res[idx++] = GLXExt.GLX_PBUFFER_BIT;

      res[idx++] = GLXExt.GLX_RENDER_TYPE;
      res[idx++] = GLXExt.GLX_RGBA_BIT;
    } else {
      res[idx++] = GLX.GLX_RGBA;
    }
    if (caps.getDoubleBuffered()) {
      res[idx++] = GLX.GLX_DOUBLEBUFFER;
      if (pbuffer) {
        res[idx++] = GL.GL_TRUE;
      }
    } else {
      if (pbuffer) {
        res[idx++] = GLX.GLX_DOUBLEBUFFER;
        res[idx++] = GL.GL_FALSE;
      }
    }
    if (caps.getStereo()) {
      res[idx++] = GLX.GLX_STEREO;
      if (pbuffer) {
        res[idx++] = GL.GL_TRUE;
      }
    }
    // NOTE: don't set (GLX_STEREO, GL_FALSE) in "else" branch for
    // pbuffer case to work around Mesa bug

    res[idx++] = GLX.GLX_RED_SIZE;
    res[idx++] = caps.getRedBits();
    res[idx++] = GLX.GLX_GREEN_SIZE;
    res[idx++] = caps.getGreenBits();
    res[idx++] = GLX.GLX_BLUE_SIZE;
    res[idx++] = caps.getBlueBits();
    res[idx++] = GLX.GLX_ALPHA_SIZE;
    res[idx++] = caps.getAlphaBits();
    res[idx++] = GLX.GLX_DEPTH_SIZE;
    res[idx++] = caps.getDepthBits();
    if (caps.getStencilBits() > 0) {
      res[idx++] = GLX.GLX_STENCIL_SIZE;
      res[idx++] = caps.getStencilBits();
    }
    if (caps.getAccumRedBits()   > 0 ||
        caps.getAccumGreenBits() > 0 ||
        caps.getAccumBlueBits()  > 0 ||
        caps.getAccumAlphaBits() > 0) {
      res[idx++] = GLX.GLX_ACCUM_RED_SIZE;
      res[idx++] = caps.getAccumRedBits();
      res[idx++] = GLX.GLX_ACCUM_GREEN_SIZE;
      res[idx++] = caps.getAccumGreenBits();
      res[idx++] = GLX.GLX_ACCUM_BLUE_SIZE;
      res[idx++] = caps.getAccumBlueBits();
      res[idx++] = GLX.GLX_ACCUM_ALPHA_SIZE;
      res[idx++] = caps.getAccumAlphaBits();
    }
    if (isMultisampleAvailable && caps.getSampleBuffers()) {
      res[idx++] = GLXExt.GLX_SAMPLE_BUFFERS_ARB;
      res[idx++] = GL.GL_TRUE;
      res[idx++] = GLXExt.GLX_SAMPLES_ARB;
      res[idx++] = caps.getNumSamples();
    }
    if (pbuffer) {
      if (caps.getPbufferFloatingPointBuffers()) {
        String glXExtensions = GLX.glXQueryExtensionsString(display, screen);
        if (glXExtensions == null ||
            glXExtensions.indexOf("GLX_NV_float_buffer") < 0) {
          throw new GLException("Floating-point pbuffers on X11 currently require NVidia hardware");
        }
        res[idx++] = GLX.GLX_FLOAT_COMPONENTS_NV;
        res[idx++] = GL.GL_TRUE;
      }
    }
    res[idx++] = 0;
    return res;
  }

  public static GLCapabilities attribList2GLCapabilities(int[] iattribs,
                                                         int niattribs,
                                                         int[] ivalues,
                                                         boolean pbuffer) {
    GLCapabilities caps = new GLCapabilities();

    for (int i = 0; i < niattribs; i++) {
      int attr = iattribs[i];
      switch (attr) {
        case GLX.GLX_DOUBLEBUFFER:
          caps.setDoubleBuffered(ivalues[i] != GL.GL_FALSE);
          break;

        case GLX.GLX_STEREO:
          caps.setStereo(ivalues[i] != GL.GL_FALSE);
          break;

        case GLX.GLX_RED_SIZE:
          caps.setRedBits(ivalues[i]);
          break;

        case GLX.GLX_GREEN_SIZE:
          caps.setGreenBits(ivalues[i]);
          break;

        case GLX.GLX_BLUE_SIZE:
          caps.setBlueBits(ivalues[i]);
          break;

        case GLX.GLX_ALPHA_SIZE:
          caps.setAlphaBits(ivalues[i]);
          break;

        case GLX.GLX_DEPTH_SIZE:
          caps.setDepthBits(ivalues[i]);
          break;

        case GLX.GLX_STENCIL_SIZE:
          caps.setStencilBits(ivalues[i]);
          break;

        case GLX.GLX_ACCUM_RED_SIZE:
          caps.setAccumRedBits(ivalues[i]);
          break;

        case GLX.GLX_ACCUM_GREEN_SIZE:
          caps.setAccumGreenBits(ivalues[i]);
          break;

        case GLX.GLX_ACCUM_BLUE_SIZE:
          caps.setAccumBlueBits(ivalues[i]);
          break;

        case GLX.GLX_ACCUM_ALPHA_SIZE:
          caps.setAccumAlphaBits(ivalues[i]);
          break;

        case GLXExt.GLX_SAMPLE_BUFFERS_ARB:
          caps.setSampleBuffers(ivalues[i] != GL.GL_FALSE);
          break;

        case GLXExt.GLX_SAMPLES_ARB:
          caps.setNumSamples(ivalues[i]);
          break;

        case GLX.GLX_FLOAT_COMPONENTS_NV:
          caps.setPbufferFloatingPointBuffers(ivalues[i] != GL.GL_FALSE);
          break;

        default:
          break;
      }
    }

    return caps;
  }

  public void lockToolkit() {
  }

  public void unlockToolkit() {
  }

  public void lockAWTForJava2D() {
    lockToolkit();
  }
  public void unlockAWTForJava2D() {
    unlockToolkit();
  }

  // Display connection for use by visual selection algorithm and by all offscreen surfaces
  private static long staticDisplay;
  public static long getDisplayConnection() {
    if (staticDisplay == 0) {
      getX11Factory().lockToolkit();
      try {
        staticDisplay = GLX.XOpenDisplay(null);
        if (DEBUG && (staticDisplay != 0)) {
          long display = staticDisplay;
          int screen = 0; // FIXME
          System.err.println("!!! GLX server vendor : " +
                             GLX.glXQueryServerString(display, screen, GLX.GLX_VENDOR));
          System.err.println("!!! GLX server version: " +
                             GLX.glXQueryServerString(display, screen, GLX.GLX_VERSION));
          System.err.println("!!! GLX client vendor : " +
                             GLX.glXGetClientString(display, GLX.GLX_VENDOR));
          System.err.println("!!! GLX client version: " +
                             GLX.glXGetClientString(display, GLX.GLX_VERSION));
        }

        if (staticDisplay != 0) {
          String vendor = GLX.glXGetClientString(staticDisplay, GLX.GLX_VENDOR);
          if (vendor != null && vendor.startsWith("ATI")) {
            isVendorATI = true;
          }
        }
      } finally {
        getX11Factory().unlockToolkit();
      }
      if (staticDisplay == 0) {
        throw new GLException("Unable to open default display, needed for visual selection and offscreen surface handling");
      }
    }
    return staticDisplay;
  }

  private static boolean checkedMultisample;
  private static boolean multisampleAvailable;
  public static boolean isMultisampleAvailable() {
    if (!checkedMultisample) {
      long display = getDisplayConnection();
      String exts = GLX.glXGetClientString(display, GLX.GLX_EXTENSIONS);
      if (exts != null) {
        multisampleAvailable = (exts.indexOf("GLX_ARB_multisample") >= 0);
      }
      checkedMultisample = true;
    }
    return multisampleAvailable;
  }

  private static String glXGetConfigErrorCode(int err) {
    switch (err) {
      case GLX.GLX_NO_EXTENSION:  return "GLX_NO_EXTENSION";
      case GLX.GLX_BAD_SCREEN:    return "GLX_BAD_SCREEN";
      case GLX.GLX_BAD_ATTRIBUTE: return "GLX_BAD_ATTRIBUTE";
      case GLX.GLX_BAD_VISUAL:    return "GLX_BAD_VISUAL";
      default:                return "Unknown error code " + err;
    }
  }

  public static int glXGetConfig(long display, XVisualInfo info, int attrib, int[] tmp, int tmp_offset) {
    if (display == 0) {
      throw new GLException("No display connection");
    }
    int res = GLX.glXGetConfig(display, info, attrib, tmp, tmp_offset);
    if (res != 0) {
      throw new GLException("glXGetConfig failed: error code " + glXGetConfigErrorCode(res));
    }
    return tmp[tmp_offset];
  }

  public static X11GLDrawableFactory getX11Factory() {
    return (X11GLDrawableFactory) getFactory();
  }

  /** Workaround for apparent issue with ATI's proprietary drivers
      where direct contexts still send GLX tokens for GL calls */
  public static boolean isVendorATI() {
    return isVendorATI;
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

  public GLContext createContextOnJava2DSurface(Graphics g, GLContext shareWith)
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
    lockToolkit();
    long display = getDisplayConnection();
    boolean res = GLX.XF86VidModeGetGammaRampSize(display,
                                                  GLX.DefaultScreen(display),
                                                  size, 0);
    unlockToolkit();
    if (!res)
      return 0;
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

    lockToolkit();
    long display = getDisplayConnection();
    boolean res = GLX.XF86VidModeSetGammaRamp(display,
                                              GLX.DefaultScreen(display),
                                              rampData.length,
                                              rampData, 0,
                                              rampData, 0,
                                              rampData, 0);
    unlockToolkit();
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
    lockToolkit();
    long display = getDisplayConnection();
    boolean res = GLX.XF86VidModeGetGammaRamp(display,
                                              GLX.DefaultScreen(display),
                                              size,
                                              redRampData,
                                              greenRampData,
                                              blueRampData);
    unlockToolkit();
    if (!res)
      return null;
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
    lockToolkit();
    long display = getDisplayConnection();
    GLX.XF86VidModeSetGammaRamp(display,
                                GLX.DefaultScreen(display),
                                size,
                                redRampData,
                                greenRampData,
                                blueRampData);
    unlockToolkit();
  }
}
