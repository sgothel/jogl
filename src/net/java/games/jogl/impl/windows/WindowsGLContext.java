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

package net.java.games.jogl.impl.windows;

import java.awt.Component;
import java.util.*;
import net.java.games.gluegen.opengl.*; // for PROCADDRESS_VAR_PREFIX
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public abstract class WindowsGLContext extends GLContext {
  private static JAWT jawt;
  protected long hglrc;
  protected long hdc;
  private boolean wglGetExtensionsStringEXTInitialized;
  private boolean wglGetExtensionsStringEXTAvailable;
  private static final Map/*<String, String>*/ functionNameMap;
  private static final Map/*<String, String>*/ extensionNameMap;

  static {
    functionNameMap = new HashMap();
    functionNameMap.put("glAllocateMemoryNV", "wglAllocateMemoryNV");
    functionNameMap.put("glFreeMemoryNV", "wglFreeMemoryNV");

    extensionNameMap = new HashMap();
    extensionNameMap.put("GL_ARB_pbuffer", "WGL_ARB_pbuffer");
    extensionNameMap.put("GL_ARB_pixel_format", "WGL_ARB_pixel_format");
  }

  public WindowsGLContext(Component component, GLCapabilities capabilities, GLCapabilitiesChooser chooser) {
    super(component, capabilities, chooser);
  }
  
  protected GL createGL()
  {
    return new WindowsGLImpl(this);
  }
  
  protected String mapToRealGLFunctionName(String glFunctionName) {
    String lookup = (String) functionNameMap.get(glFunctionName);
    if (lookup != null) {
      return lookup;
    }
    return glFunctionName;
  }

  protected String mapToRealGLExtensionName(String glExtensionName) {
    String lookup = (String) extensionNameMap.get(glExtensionName);
    if (lookup != null) {
      return lookup;
    }
    return glExtensionName;
  }

  protected abstract boolean isOffscreen();
  
  public abstract int getOffscreenContextBufferedImageType();

  public abstract int getOffscreenContextReadBuffer();

  public abstract boolean offscreenImageNeedsVerticalFlip();

  /**
   * Creates and initializes an appropriate OpenGl context. Should only be
   * called by {@link makeCurrent(Runnable)}.
   */
  protected abstract void create();
  
  protected synchronized boolean makeCurrent(Runnable initAction) throws GLException {
    boolean created = false;
    if (hglrc == 0) {
      create();
      if (DEBUG) {
        System.err.println("!!! Created GL context for " + getClass().getName());
      }
      created = true;
    }

    if (!WGL.wglMakeCurrent(hdc, hglrc)) {
      throw new GLException("Error making context current");
    }

    if (created) {
      resetGLFunctionAvailability();
      initAction.run();
    }
    return true;
  }

  protected synchronized void free() throws GLException {
    if (!WGL.wglMakeCurrent(0, 0)) {
      throw new GLException("Error freeing OpenGL context");
    }
  }

  protected abstract void swapBuffers() throws GLException;


  protected void resetGLFunctionAvailability() {
    super.resetGLFunctionAvailability();
    resetGLProcAddressTable();
  }
  
  protected void resetGLProcAddressTable() {    
    
    if (DEBUG) {
      System.err.println("!!! Initializing OpenGL extension address table");
    }

    net.java.games.jogl.impl.ProcAddressTable table = getGLProcAddressTable();
    
    // if GL is no longer an interface, we'll have to re-implement the code
    // below so it only iterates through gl methods (a non-interface might
    // have constructors, custom methods, etc). For now we assume all methods
    // will be gl methods.
    GL gl = getGL();

    Class tableClass = table.getClass();
    
    java.lang.reflect.Field[] fields = tableClass.getDeclaredFields();
    
    for (int i = 0; i < fields.length; ++i) {
      String addressFieldName = fields[i].getName();
      if (!addressFieldName.startsWith(GLEmitter.PROCADDRESS_VAR_PREFIX))
      {
        // not a proc address variable
        continue;
      }
      int startOfMethodName = GLEmitter.PROCADDRESS_VAR_PREFIX.length();
      String glFuncName = addressFieldName.substring(startOfMethodName);
      try
      {
        java.lang.reflect.Field addressField = tableClass.getDeclaredField(addressFieldName);
        assert(addressField.getType() == Long.TYPE);
        // get the current value of the proc address variable in the table object
        long oldProcAddress = addressField.getLong(table); 
        long newProcAddress = WGL.wglGetProcAddress(glFuncName);
        /*
        System.err.println(
          "!!!   Address=" + (newProcAddress == 0 
                        ? "<NULL>    "
                        : ("0x" +
                           Long.toHexString(newProcAddress))) +
          "\tGL func: " + glFuncName);
        */
        // set the current value of the proc address variable in the table object
        addressField.setLong(gl, newProcAddress); 
      } catch (Exception e) {
        throw new GLException(
          "Cannot get GL proc address for method \"" +
          glFuncName + "\": Couldn't get value of field \"" + addressFieldName +
          "\" in class " + tableClass.getName(), e);
      }
    }

  }
  
  public net.java.games.jogl.impl.ProcAddressTable getGLProcAddressTable() {
    if (glProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
      glProcAddressTable =
        new net.java.games.jogl.impl.ProcAddressTable();
    }          
    return glProcAddressTable;
  }
  
  public String getPlatformExtensionsString() {
    if (!wglGetExtensionsStringEXTInitialized) {
      wglGetExtensionsStringEXTAvailable = (WGL.wglGetProcAddress("wglGetExtensionsStringEXT") != 0);
      wglGetExtensionsStringEXTInitialized = true;
    }
    if (wglGetExtensionsStringEXTAvailable) {
      return gl.wglGetExtensionsStringEXT();
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

  // Table that holds the addresses of the native C-language entry points for
  // OpenGL functions.
  private net.java.games.jogl.impl.ProcAddressTable glProcAddressTable;

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
  
  // Helper routine for the overridden create() to call
  protected void choosePixelFormatAndCreateContext(boolean onscreen) {
    PIXELFORMATDESCRIPTOR pfd = null;
    int pixelFormat = 0;
    if (chooser == null) {
      // Note: this code path isn't taken any more now that the
      // DefaultGLCapabilitiesChooser is present. However, it is being
      // left in place for debugging purposes.
      pfd = glCapabilities2PFD(capabilities, onscreen);
      pixelFormat = WGL.ChoosePixelFormat(hdc, pfd);
      if (pixelFormat == 0) {
        throw new GLException("Unable to choose appropriate pixel format");
      }
      if (DEBUG) {
        System.err.println("Chosen pixel format from ChoosePixelFormat:");
	PIXELFORMATDESCRIPTOR tmpPFD = new PIXELFORMATDESCRIPTOR();
	WGL.DescribePixelFormat(hdc, pixelFormat, tmpPFD.size(), tmpPFD);
        System.err.println(pfd2GLCapabilities(tmpPFD));
      }
    } else {
      int numFormats = WGL.DescribePixelFormat(hdc, 1, 0, null);
      if (numFormats == 0) {
        throw new GLException("Unable to enumerate pixel formats of window for GLCapabilitiesChooser");
      }
      GLCapabilities[] availableCaps = new GLCapabilities[numFormats];
      pfd = new PIXELFORMATDESCRIPTOR();
      for (int i = 0; i < numFormats; i++) {
        if (WGL.DescribePixelFormat(hdc, 1 + i, pfd.size(), pfd) == 0) {
          throw new GLException("Error describing pixel format " + (1 + i) + " of device context");
        }
        availableCaps[i] = pfd2GLCapabilities(pfd);
      }
      // Supply information to chooser
      pixelFormat = chooser.chooseCapabilities(capabilities, availableCaps);
      if ((pixelFormat < 0) || (pixelFormat >= numFormats)) {
        throw new GLException("Invalid result " + pixelFormat +
                              " from GLCapabilitiesChooser (should be between 0 and " +
                              (numFormats - 1) + ")");
      }
      if (DEBUG) {
        System.err.println("Chosen pixel format (" + pixelFormat + "):");
        System.err.println(availableCaps[pixelFormat]);
      }
      pixelFormat += 1; // one-base the index
      if (WGL.DescribePixelFormat(hdc, pixelFormat, pfd.size(), pfd) == 0) {
        throw new GLException("Error re-describing the chosen pixel format");
      }
    }
    if (!WGL.SetPixelFormat(hdc, pixelFormat, pfd)) {
      throw new GLException("Unable to set pixel format");
    }
    hglrc = WGL.wglCreateContext(hdc);
    if (hglrc == 0) {
      throw new GLException("Unable to create OpenGL context");
    }
  }

  static PIXELFORMATDESCRIPTOR glCapabilities2PFD(GLCapabilities caps, boolean onscreen) {
    int colorDepth = (caps.getRedBits() +
                      caps.getGreenBits() +
                      caps.getBlueBits());
    if (colorDepth < 15) {
      throw new GLException("Bit depths < 15 (i.e., non-true-color) not supported");
    }
    PIXELFORMATDESCRIPTOR pfd = new PIXELFORMATDESCRIPTOR();
    pfd.nSize((short) pfd.size());
    pfd.nVersion((short) 1);
    int pfdFlags = (WGL.PFD_SUPPORT_OPENGL |
                    WGL.PFD_GENERIC_ACCELERATED);
    if (caps.getDoubleBuffered()) {
      pfdFlags |= WGL.PFD_DOUBLEBUFFER;
      if (onscreen) {
        pfdFlags |= WGL.PFD_SWAP_EXCHANGE;
      }
    }
    if (onscreen) {
      pfdFlags |= WGL.PFD_DRAW_TO_WINDOW;
    } else {
      pfdFlags |= WGL.PFD_DRAW_TO_BITMAP;
    }
    pfd.dwFlags(pfdFlags);
    pfd.iPixelType((byte) WGL.PFD_TYPE_RGBA);
    pfd.cColorBits((byte) colorDepth);
    pfd.cRedBits  ((byte) caps.getRedBits());
    pfd.cGreenBits((byte) caps.getGreenBits());
    pfd.cBlueBits ((byte) caps.getBlueBits());
    pfd.cDepthBits((byte) caps.getDepthBits());
    pfd.iLayerType((byte) WGL.PFD_MAIN_PLANE);
    return pfd;
  }

  static GLCapabilities pfd2GLCapabilities(PIXELFORMATDESCRIPTOR pfd) {
    if ((pfd.dwFlags() & WGL.PFD_SUPPORT_OPENGL) == 0) {
      return null;
    }
    GLCapabilities res = new GLCapabilities();
    res.setRedBits       (pfd.cRedBits());
    res.setGreenBits     (pfd.cGreenBits());
    res.setBlueBits      (pfd.cBlueBits());
    res.setAlphaBits     (pfd.cAlphaBits());
    res.setAccumRedBits  (pfd.cAccumRedBits());
    res.setAccumGreenBits(pfd.cAccumGreenBits());
    res.setAccumBlueBits (pfd.cAccumBlueBits());
    res.setAccumAlphaBits(pfd.cAccumAlphaBits());
    res.setDepthBits     (pfd.cDepthBits());
    res.setStencilBits   (pfd.cStencilBits());
    res.setDoubleBuffered((pfd.dwFlags() & WGL.PFD_DOUBLEBUFFER) != 0);
    res.setStereo        ((pfd.dwFlags() & WGL.PFD_STEREO) != 0);
    res.setHardwareAccelerated(((pfd.dwFlags() & WGL.PFD_GENERIC_FORMAT) == 0) ||
			       ((pfd.dwFlags() & WGL.PFD_GENERIC_ACCELERATED) != 0));
    return res;
  }
}
