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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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
import java.util.*;
import net.java.games.gluegen.runtime.*; // for PROCADDRESS_VAR_PREFIX
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public abstract class X11GLContext extends GLContext {
  private static JAWT jawt;
  protected long display;
  protected long drawable;
  protected long context;
  private boolean glXQueryExtensionsStringInitialized;
  private boolean glXQueryExtensionsStringAvailable;
  private static final Map/*<String, String>*/ functionNameMap;
  private boolean isGLX13;
  // Table that holds the addresses of the native C-language entry points for
  // OpenGL functions.
  private GLProcAddressTable glProcAddressTable;
  private static boolean haveResetGLXProcAddressTable;

  static {
    functionNameMap = new HashMap();
    functionNameMap.put("glAllocateMemoryNV", "glXAllocateMemoryNV");
    functionNameMap.put("glFreeMemoryNV", "glXFreeMemoryNV");
  }

  public X11GLContext(Component component,
                      GLCapabilities capabilities,
                      GLCapabilitiesChooser chooser,
                      GLContext shareWith) {
    super(component, capabilities, chooser, shareWith);
  }
  
  protected GL createGL()
  {
    return new X11GLImpl(this);
  }
  
  protected String mapToRealGLFunctionName(String glFunctionName) {
    String lookup = (String) functionNameMap.get(glFunctionName);
    if (lookup != null) {
      return lookup;
    }
    return glFunctionName;
  }

  protected String mapToRealGLExtensionName(String glExtensionName) {
    return glExtensionName;
  }

  protected abstract boolean isOffscreen();
  
  public abstract int getOffscreenContextBufferedImageType();

  public int getOffscreenContextWidth() {
    throw new GLException("Should not call this");
  }
  
  public int getOffscreenContextHeight() {
    throw new GLException("Should not call this");
  }
  
  public int getOffscreenContextPixelDataType() {
    throw new GLException("Should not call this");
  }

  public abstract int getOffscreenContextReadBuffer();

  public abstract boolean offscreenImageNeedsVerticalFlip();

  public synchronized void setRenderingThread(Thread currentThreadOrNull, Runnable initAction) {
    this.willSetRenderingThread = false;
    // FIXME: the JAWT on X11 grabs the AWT lock while the
    // DrawingSurface is locked, which means that no other events can
    // be processed. Currently we handle this by preventing the
    // effects of setRenderingThread. We should figure out a better
    // solution that is reasonably robust. Must file a bug to be fixed
    // in the 1.5 JAWT.
  }

  /**
   * Creates and initializes an appropriate OpenGl context. Should only be
   * called by {@link makeCurrent(Runnable)}.
   */
  protected abstract void create();
  
  public boolean isExtensionAvailable(String glExtensionName) {
    if (glExtensionName.equals("GL_ARB_pbuffer") ||
        glExtensionName.equals("GL_ARB_pixel_format")) {
      return isGLX13;
    }
    return super.isExtensionAvailable(glExtensionName);
  }

  protected synchronized boolean makeCurrent(Runnable initAction) throws GLException {
    boolean created = false;
    if (context == 0) {
      create();
      if (DEBUG) {
        System.err.println("!!! Created GL context for " + getClass().getName());
      }
      created = true;
    }
    if (drawable == 0) {
      throw new GLException("Unable to make context current; drawable was null");
    }

    // FIXME: this cast to int would be wrong on 64-bit platforms
    // where the argument type to glXMakeCurrent would change (should
    // probably make GLXDrawable, and maybe XID, Opaque as long)
    if (!GLX.glXMakeCurrent(display, (int) drawable, context)) {
      throw new GLException("Error making context current");
    }

    if (created) {
      resetGLFunctionAvailability();
      initAction.run();
    }
    return true;
  }

  protected synchronized void free() throws GLException {
    if (!GLX.glXMakeCurrent(display, 0, 0)) {
      throw new GLException("Error freeing OpenGL context");
    }
  }

  public abstract void swapBuffers() throws GLException;

  protected long dynamicLookupFunction(String glFuncName) {
    long res = GLX.glXGetProcAddressARB(glFuncName);
    if (res == 0) {
      // GLU routines aren't known to the OpenGL function lookup
      res = GLX.dlsym(glFuncName);
    }
    return res;
  }

  public boolean isCreated() {
    return (context != 0);
  }

  protected void resetGLFunctionAvailability() {
    super.resetGLFunctionAvailability();
    if (DEBUG) {
      System.err.println("!!! Initializing OpenGL extension address table");
    }
    resetProcAddressTable(getGLProcAddressTable());

    if (!haveResetGLXProcAddressTable) {
      resetProcAddressTable(GLX.getGLXProcAddressTable());
    }

    // Figure out whether we are running GLX version 1.3 or above and
    // therefore have pbuffer support
    if (display == 0) {
      throw new GLException("Expected non-null DISPLAY for querying GLX version");
    }
    int[] major = new int[1];
    int[] minor = new int[1];
    if (!GLX.glXQueryVersion(display, major, minor)) {
      throw new GLException("glXQueryVersion failed");
    }
    isGLX13 = ((major[0] > 1) || (minor[0] > 2));
  }
  
  public GLProcAddressTable getGLProcAddressTable() {
    if (glProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
      glProcAddressTable = new GLProcAddressTable();
    }          
    return glProcAddressTable;
  }
  
  public synchronized String getPlatformExtensionsString() {
    if (display == 0) {
      throw new GLException("Context not current");
    }
    if (!glXQueryExtensionsStringInitialized) {
      glXQueryExtensionsStringAvailable = (GLX.glXGetProcAddressARB("glXQueryExtensionsString") != 0);
      glXQueryExtensionsStringInitialized = true;
    }
    if (glXQueryExtensionsStringAvailable) {
      return GLX.glXQueryExtensionsString(display, GLX.DefaultScreen(display));
    } else {
      return "";
    }
  }

  protected boolean isFunctionAvailable(String glFunctionName)
  {
    boolean available = super.isFunctionAvailable(glFunctionName);
    
    // Sanity check for implementations that use proc addresses for run-time
    // linking: if the function IS available, then make sure there's a proc
    // address for it if it's an extension or not part of the OpenGL 1.1 core
    // (post GL 1.1 functions are run-time linked on windows).
    assert(!available ||
           (getGLProcAddressTable().getAddressFor(mapToRealGLFunctionName(glFunctionName)) != 0 ||
            FunctionAvailabilityCache.isPartOfGLCore("1.1", mapToRealGLFunctionName(glFunctionName)))
           );

    return available;
  }
  
  //----------------------------------------------------------------------
  // Internals only below this point
  //

  protected JAWT getJAWT() {
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

  protected XVisualInfo chooseVisual() {
    int screen = 0; // FIXME: provide way to specify this?
    XVisualInfo vis = null;
    if (chooser == null) {
      // Note: this code path isn't taken any more now that the
      // DefaultGLCapabilitiesChooser is present. However, it is being
      // left in place for debugging purposes.
      int[] attribs = glCapabilities2AttribList(capabilities);
      vis = GLX.glXChooseVisual(display, screen, attribs);
      if (vis == null) {
        throw new GLException("Unable to find matching visual");
      }
      if (DEBUG) {
        System.err.println("Chosen visual from glXChooseVisual:");
        System.err.println(xvi2GLCapabilities(vis));
      }
    } else {
      int[] count = new int[1];
      XVisualInfo template = new XVisualInfo();
      template.screen(screen);
      XVisualInfo[] infos = GLX.XGetVisualInfo(display, GLX.VisualScreenMask, template, count);
      if (infos == null) {
        throw new GLException("Error while enumerating available XVisualInfos");
      }
      GLCapabilities[] caps = new GLCapabilities[infos.length];
      for (int i = 0; i < infos.length; i++) {
        caps[i] = xvi2GLCapabilities(infos[i]);
      }
      int chosen = chooser.chooseCapabilities(capabilities, caps);
      if (chosen < 0 || chosen >= caps.length) {
        throw new GLException("GLCapabilitiesChooser specified invalid index (expected 0.." + (caps.length - 1) + ")");
      }
      if (DEBUG) {
        System.err.println("Chosen visual (" + chosen + "):");
        System.err.println(caps[chosen]);
      }
      vis = infos[chosen];
      if (vis == null) {
        throw new GLException("GLCapabilitiesChooser chose an invalid visual");
      }
    }
    return vis;
  }

  protected long createContext(XVisualInfo vis, boolean onscreen) {
    X11GLContext other = (X11GLContext) GLContextShareSet.getShareContext(this);
    long share = 0;
    if (other != null) {
      share = other.getContext();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
    }
    long res = GLX.glXCreateContext(display, vis, share, onscreen);
    if (res != 0) {
      GLContextShareSet.contextCreated(this);
    }
    return res;
  }

  // Helper routine for the overridden create() to call
  protected void chooseVisualAndCreateContext(boolean onscreen) {
    XVisualInfo vis = chooseVisual();
    context = createContext(vis, onscreen);
    if (context == 0) {
      throw new GLException("Unable to create OpenGL context");
    }
  }

  protected long getContext() {
    return context;
  }

  protected int[] glCapabilities2AttribList(GLCapabilities caps) {
    int colorDepth = (caps.getRedBits() +
                      caps.getGreenBits() +
                      caps.getBlueBits());
    if (colorDepth < 15) {
      throw new GLException("Bit depths < 15 (i.e., non-true-color) not supported");
    }
    int[] res = new int[22];
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
    res[idx++] = 0;
    return res;
  }

  protected GLCapabilities xvi2GLCapabilities(XVisualInfo info) {
    int[] tmp = new int[1];
    int val = glXGetConfig(info, GLX.GLX_USE_GL, tmp);
    if (val == 0) {
      // Visual does not support OpenGL
      return null;
    }
    val = glXGetConfig(info, GLX.GLX_RGBA, tmp);
    if (val == 0) {
      // Visual does not support RGBA
      return null;
    }
    GLCapabilities res = new GLCapabilities();
    res.setDoubleBuffered(glXGetConfig(info, GLX.GLX_DOUBLEBUFFER,     tmp) != 0);
    res.setStereo        (glXGetConfig(info, GLX.GLX_STEREO,           tmp) != 0);
    // Note: use of hardware acceleration is determined by
    // glXCreateContext, not by the XVisualInfo. Optimistically claim
    // that all GLCapabilities have the capability to be hardware
    // accelerated.
    res.setHardwareAccelerated(true);
    res.setDepthBits     (glXGetConfig(info, GLX.GLX_DEPTH_SIZE,       tmp));
    res.setStencilBits   (glXGetConfig(info, GLX.GLX_STENCIL_SIZE,     tmp));
    res.setRedBits       (glXGetConfig(info, GLX.GLX_RED_SIZE,         tmp));
    res.setGreenBits     (glXGetConfig(info, GLX.GLX_GREEN_SIZE,       tmp));
    res.setBlueBits      (glXGetConfig(info, GLX.GLX_BLUE_SIZE,        tmp));
    res.setAlphaBits     (glXGetConfig(info, GLX.GLX_ALPHA_SIZE,       tmp));
    res.setAccumRedBits  (glXGetConfig(info, GLX.GLX_ACCUM_RED_SIZE,   tmp));
    res.setAccumGreenBits(glXGetConfig(info, GLX.GLX_ACCUM_GREEN_SIZE, tmp));
    res.setAccumBlueBits (glXGetConfig(info, GLX.GLX_ACCUM_BLUE_SIZE,  tmp));
    res.setAccumAlphaBits(glXGetConfig(info, GLX.GLX_ACCUM_ALPHA_SIZE, tmp));
    return res;
  }

  protected String glXGetConfigErrorCode(int err) {
    switch (err) {
      case GLX.GLX_NO_EXTENSION:  return "GLX_NO_EXTENSION";
      case GLX.GLX_BAD_SCREEN:    return "GLX_BAD_SCREEN";
      case GLX.GLX_BAD_ATTRIBUTE: return "GLX_BAD_ATTRIBUTE";
      case GLX.GLX_BAD_VISUAL:    return "GLX_BAD_VISUAL";
      default:                return "Unknown error code " + err;
    }
  }

  protected int glXGetConfig(XVisualInfo info, int attrib, int[] tmp) {
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
