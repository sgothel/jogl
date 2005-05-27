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

package net.java.games.jogl.impl.x11;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class X11GLContextFactory extends GLContextFactory {
  static {
    NativeLibLoader.load();
  }

  private static final int MAX_ATTRIBS = 128;

  public GraphicsConfiguration chooseGraphicsConfiguration(GLCapabilities capabilities,
                                                           GLCapabilitiesChooser chooser,
                                                           GraphicsDevice device) {
    int screen = X11SunJDKReflection.graphicsDeviceGetScreen(device);
    // Until we have a rock-solid visual selection algorithm written
    // in pure Java, we're going to provide the underlying window
    // system's selection to the chooser as a hint

    int[] attribs = glCapabilities2AttribList(capabilities, isMultisampleAvailable());
    XVisualInfo[] infos = null;
    GLCapabilities[] caps = null;
    int recommendedIndex = -1;
    lockAWT();
    try {
      long display = getDisplayConnection();
      XVisualInfo recommendedVis = GLX.glXChooseVisual(display, screen, attribs);
      int[] count = new int[1];
      XVisualInfo template = new XVisualInfo();
      template.screen(screen);
      infos = GLX.XGetVisualInfo(display, GLX.VisualScreenMask, template, count);
      if (infos == null) {
        throw new GLException("Error while enumerating available XVisualInfos");
      }
      caps = new GLCapabilities[infos.length];
      for (int i = 0; i < infos.length; i++) {
        caps[i] = xvi2GLCapabilities(display, infos[i]);
        // Attempt to find the visual chosen by glXChooseVisual
        if (recommendedVis != null && recommendedVis.visualid() == infos[i].visualid()) {
          recommendedIndex = i;
        }
      }
    } finally {
      unlockAWT();
    }
    int chosen = chooser.chooseCapabilities(capabilities, caps, recommendedIndex);
    if (chosen < 0 || chosen >= caps.length) {
      throw new GLException("GLCapabilitiesChooser specified invalid index (expected 0.." + (caps.length - 1) + ")");
    }
    XVisualInfo vis = infos[chosen];
    if (vis == null) {
      throw new GLException("GLCapabilitiesChooser chose an invalid visual");
    }
    // FIXME: need to look at glue code and see type of this field
    long visualID = vis.visualid();
    // FIXME: the storage for the infos array, as well as that for the
    // recommended visual, is leaked; should free them here with XFree()

    // Now figure out which GraphicsConfiguration corresponds to this
    // visual by matching the visual ID
    GraphicsConfiguration[] configs = device.getConfigurations();
    for (int i = 0; i < configs.length; i++) {
      GraphicsConfiguration config = configs[i];
      if (config != null) {
        if (X11SunJDKReflection.graphicsConfigurationGetVisualID(config) == visualID) {
          return config;
        }
      }
    }

    // Either we weren't able to reflectively introspect on the
    // X11GraphicsConfig or something went wrong in the steps above;
    // we're going to return null without signaling an error condition
    // in this case (although we should distinguish between the two
    // and possibly report more of an error in the latter case)
    return null;
  }

  public GLContext createGLContext(Component component,
                                   GLCapabilities capabilities,
                                   GLCapabilitiesChooser chooser,
                                   GLContext shareWith) {
    if (component != null) {
      return new X11OnscreenGLContext(component, capabilities, chooser, shareWith);
    } else {
      return new X11OffscreenGLContext(capabilities, chooser, shareWith);
    }
  }

  public static GLCapabilities xvi2GLCapabilities(long display, XVisualInfo info) {
    int[] tmp = new int[1];
    int val = glXGetConfig(display, info, GLX.GLX_USE_GL, tmp);
    if (val == 0) {
      // Visual does not support OpenGL
      return null;
    }
    val = glXGetConfig(display, info, GLX.GLX_RGBA, tmp);
    if (val == 0) {
      // Visual does not support RGBA
      return null;
    }
    GLCapabilities res = new GLCapabilities();
    res.setDoubleBuffered(glXGetConfig(display, info, GLX.GLX_DOUBLEBUFFER,     tmp) != 0);
    res.setStereo        (glXGetConfig(display, info, GLX.GLX_STEREO,           tmp) != 0);
    // Note: use of hardware acceleration is determined by
    // glXCreateContext, not by the XVisualInfo. Optimistically claim
    // that all GLCapabilities have the capability to be hardware
    // accelerated.
    res.setHardwareAccelerated(true);
    res.setDepthBits     (glXGetConfig(display, info, GLX.GLX_DEPTH_SIZE,       tmp));
    res.setStencilBits   (glXGetConfig(display, info, GLX.GLX_STENCIL_SIZE,     tmp));
    res.setRedBits       (glXGetConfig(display, info, GLX.GLX_RED_SIZE,         tmp));
    res.setGreenBits     (glXGetConfig(display, info, GLX.GLX_GREEN_SIZE,       tmp));
    res.setBlueBits      (glXGetConfig(display, info, GLX.GLX_BLUE_SIZE,        tmp));
    res.setAlphaBits     (glXGetConfig(display, info, GLX.GLX_ALPHA_SIZE,       tmp));
    res.setAccumRedBits  (glXGetConfig(display, info, GLX.GLX_ACCUM_RED_SIZE,   tmp));
    res.setAccumGreenBits(glXGetConfig(display, info, GLX.GLX_ACCUM_GREEN_SIZE, tmp));
    res.setAccumBlueBits (glXGetConfig(display, info, GLX.GLX_ACCUM_BLUE_SIZE,  tmp));
    res.setAccumAlphaBits(glXGetConfig(display, info, GLX.GLX_ACCUM_ALPHA_SIZE, tmp));
    if (isMultisampleAvailable()) {
      res.setSampleBuffers(glXGetConfig(display, info, GLX.GLX_SAMPLE_BUFFERS_ARB, tmp) != 0);
      res.setNumSamples   (glXGetConfig(display, info, GLX.GLX_SAMPLES_ARB,        tmp));
    }
    return res;
  }

  public static int[] glCapabilities2AttribList(GLCapabilities caps,
                                                boolean isMultisampleAvailable) {
    int colorDepth = (caps.getRedBits() +
                      caps.getGreenBits() +
                      caps.getBlueBits());
    if (colorDepth < 15) {
      throw new GLException("Bit depths < 15 (i.e., non-true-color) not supported");
    }
    int[] res = new int[MAX_ATTRIBS];
    int idx = 0;
    res[idx++] = GLX.GLX_RGBA;
    if (caps.getDoubleBuffered()) {
      res[idx++] = GLX.GLX_DOUBLEBUFFER;
    }
    if (caps.getStereo()) {
      res[idx++] = GLX.GLX_STEREO;
    }
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
    res[idx++] = GLX.GLX_STENCIL_SIZE;
    res[idx++] = caps.getStencilBits();
    res[idx++] = GLX.GLX_ACCUM_RED_SIZE;
    res[idx++] = caps.getAccumRedBits();
    res[idx++] = GLX.GLX_ACCUM_GREEN_SIZE;
    res[idx++] = caps.getAccumGreenBits();
    res[idx++] = GLX.GLX_ACCUM_BLUE_SIZE;
    res[idx++] = caps.getAccumBlueBits();
    if (isMultisampleAvailable && caps.getSampleBuffers()) {
      res[idx++] = GL.GLX_SAMPLE_BUFFERS_ARB;
      res[idx++] = GL.GL_TRUE;
      res[idx++] = GL.GLX_SAMPLES_ARB;
      res[idx++] = caps.getNumSamples();
    }
    res[idx++] = 0;
    return res;
  }

  // JAWT access
  private static JAWT jawt;
  public static JAWT getJAWT() {
    if (jawt == null) {
      JAWT j = new JAWT();
      j.version(JAWTFactory.JAWT_VERSION_1_4);
      if (!JAWTFactory.JAWT_GetAWT(j)) {
        throw new RuntimeException("Unable to initialize JAWT");
      }
      jawt = j;
    }
    return jawt;
  }

  public static void lockAWT() {
    getJAWT().Lock();
  }

  public static void unlockAWT() {
    getJAWT().Unlock();
  }

  // Display connection for use by visual selection algorithm and by all offscreen surfaces
  private static long staticDisplay;
  public static long getDisplayConnection() {
    if (staticDisplay == 0) {
      lockAWT();
      try {
        staticDisplay = GLX.XOpenDisplay(null);
      } finally {
        unlockAWT();
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

  public static int glXGetConfig(long display, XVisualInfo info, int attrib, int[] tmp) {
    if (display == 0) {
      throw new GLException("No display connection");
    }
    int res = GLX.glXGetConfig(display, info, attrib, tmp);
    if (res != 0) {
      throw new GLException("glXGetConfig failed: error code " + glXGetConfigErrorCode(res));
    }
    return tmp[0];
  }
}
