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
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.util.*;
import net.java.games.gluegen.runtime.*; // for PROCADDRESS_VAR_PREFIX
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
  // Table that holds the addresses of the native C-language entry points for
  // OpenGL functions.
  private GLProcAddressTable glProcAddressTable;
  // Handle to GLU32.dll
  private long hglu32;

  private static final int MAX_PFORMATS = 256;
  private static final int MAX_ATTRIBS  = 256;

  static {
    functionNameMap = new HashMap();
    functionNameMap.put("glAllocateMemoryNV", "wglAllocateMemoryNV");
    functionNameMap.put("glFreeMemoryNV", "wglFreeMemoryNV");

    extensionNameMap = new HashMap();
    extensionNameMap.put("GL_ARB_pbuffer", "WGL_ARB_pbuffer");
    extensionNameMap.put("GL_ARB_pixel_format", "WGL_ARB_pixel_format");
  }

  public WindowsGLContext(Component component,
                          GLCapabilities capabilities,
                          GLCapabilitiesChooser chooser,
                          GLContext shareWith) {
    super(component, capabilities, chooser, shareWith);
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

  /**
   * Creates and initializes an appropriate OpenGL context. Should only be
   * called by {@link #makeCurrent(Runnable)}.
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
      throw new GLException("Error making context current: " + WGL.GetLastError());
    }

    if (created) {
      resetGLFunctionAvailability();
      // Windows can set up sharing of display lists after creation time
      WindowsGLContext other = (WindowsGLContext) GLContextShareSet.getShareContext(this);
      if (other != null) {
        long hglrc2 = other.getHGLRC();
        if (hglrc2 == 0) {
          throw new GLException("GLContextShareSet returned an invalid OpenGL context");
        }
        if (!WGL.wglShareLists(hglrc2, hglrc)) {
          throw new GLException("wglShareLists(0x" + Long.toHexString(hglrc2) +
                                ", 0x" + Long.toHexString(hglrc) + ") failed");
        }
      }
      GLContextShareSet.contextCreated(this);      

      initAction.run();
    }
    return true;
  }

  protected synchronized void free() throws GLException {
    if (!WGL.wglMakeCurrent(0, 0)) {
      throw new GLException("Error freeing OpenGL context: " + WGL.GetLastError());
    }
  }

  protected void destroyImpl() throws GLException {
    if (hglrc != 0) {
      if (!WGL.wglDeleteContext(hglrc)) {
        throw new GLException("Unable to delete OpenGL context");
      }
      if (DEBUG) {
        System.err.println("!!! Destroyed OpenGL context " + hglrc);
      }
      hglrc = 0;
    }
  }

  public abstract void swapBuffers() throws GLException;

  protected long dynamicLookupFunction(String glFuncName) {
    long res = WGL.wglGetProcAddress(glFuncName);
    if (res == 0) {
      // GLU routines aren't known to the OpenGL function lookup
      if (hglu32 == 0) {
        hglu32 = WGL.LoadLibraryA("GLU32");
        if (hglu32 == 0) {
          throw new GLException("Error loading GLU32.DLL");
        }
      }
      res = WGL.GetProcAddress(hglu32, glFuncName);
    }
    return res;
  }

  public boolean isCreated() {
    return (hglrc != 0);
  }

  protected void resetGLFunctionAvailability() {
    super.resetGLFunctionAvailability();
    if (DEBUG) {
      System.err.println("!!! Initializing OpenGL extension address table");
    }
    resetProcAddressTable(getGLProcAddressTable());
  }
  
  public GLProcAddressTable getGLProcAddressTable() {
    if (glProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
      glProcAddressTable = new GLProcAddressTable();
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
    if (onscreen) {
      GLCapabilities[] availableCaps = null;
      int numFormats = 0;
      pfd = newPixelFormatDescriptor();
      GraphicsConfiguration config = component.getGraphicsConfiguration();
      GraphicsDevice device = config.getDevice();
      // Produce a recommended pixel format selection for the GLCapabilitiesChooser.
      // Try to use wglChoosePixelFormatARB if we have it available
      GL dummyGL = WindowsGLContextFactory.getDummyGL(device);
      int recommendedPixelFormat = -1;
      boolean haveWGLChoosePixelFormatARB = false;
      boolean haveWGLARBMultisample = false;
      if (dummyGL != null) {
        String availableWGLExtensions = WindowsGLContextFactory.getDummyGLExtensions(device);
        if (availableWGLExtensions.indexOf("WGL_ARB_pixel_format") >= 0) {
          haveWGLChoosePixelFormatARB = true;
          if (availableWGLExtensions.indexOf("WGL_ARB_multisample") >= 0) {
            haveWGLARBMultisample = true;
          }
        }
      }
      Rectangle rect = config.getBounds();
      long dc = 0;
      long rc = 0;
      boolean freeWGLC = false;
      if( dummyGL != null ) {
        dc = WindowsGLContextFactory.getDummyGLContext( device ).hdc;
        rc = WindowsGLContextFactory.getDummyGLContext( device ).hglrc;
        if( !WGL.wglMakeCurrent( dc, rc ) ) {
          System.err.println("Error Making WGLC Current: " + WGL.GetLastError() );
        } else {
          freeWGLC = true;
        }
      }
      if (dummyGL != null && haveWGLChoosePixelFormatARB) {
        int[]   iattributes = new int  [2 * MAX_ATTRIBS];
        int[]   iresults    = new int  [2 * MAX_ATTRIBS];
        float[] fattributes = new float[2 * MAX_ATTRIBS];
        int niattribs = 0;
        int nfattribs = 0;
        iattributes[niattribs++] = GL.WGL_SUPPORT_OPENGL_ARB;
        iattributes[niattribs++] = GL.GL_TRUE;
        iattributes[niattribs++] = GL.WGL_DRAW_TO_WINDOW_ARB;
        iattributes[niattribs++] = GL.GL_TRUE;
        iattributes[niattribs++] = GL.WGL_PIXEL_TYPE_ARB;
        iattributes[niattribs++] = GL.WGL_TYPE_RGBA_ARB;
        iattributes[niattribs++] = GL.WGL_DOUBLE_BUFFER_ARB;
        if (capabilities.getDoubleBuffered()) {
          iattributes[niattribs++] = GL.GL_TRUE;
        } else {
          iattributes[niattribs++] = GL.GL_FALSE;
        }
        iattributes[niattribs++] = GL.WGL_STEREO_ARB;
        if (capabilities.getStereo()) {
          iattributes[niattribs++] = GL.GL_TRUE;
        } else {
          iattributes[niattribs++] = GL.GL_FALSE;
        }
        iattributes[niattribs++] = GL.WGL_DEPTH_BITS_ARB;
        iattributes[niattribs++] = capabilities.getDepthBits();
        iattributes[niattribs++] = GL.WGL_RED_BITS_ARB;
        iattributes[niattribs++] = capabilities.getRedBits();
        iattributes[niattribs++] = GL.WGL_GREEN_BITS_ARB;
        iattributes[niattribs++] = capabilities.getGreenBits();
        iattributes[niattribs++] = GL.WGL_BLUE_BITS_ARB;
        iattributes[niattribs++] = capabilities.getBlueBits();
        iattributes[niattribs++] = GL.WGL_ALPHA_BITS_ARB;
        iattributes[niattribs++] = capabilities.getAlphaBits();
        iattributes[niattribs++] = GL.WGL_STENCIL_BITS_ARB;
        iattributes[niattribs++] = capabilities.getStencilBits();
        if (capabilities.getAccumRedBits()   > 0 ||
            capabilities.getAccumGreenBits() > 0 ||
            capabilities.getAccumBlueBits()  > 0 ||
            capabilities.getAccumAlphaBits()  > 0) {
          iattributes[niattribs++] = GL.WGL_ACCUM_BITS_ARB;
          iattributes[niattribs++] = (capabilities.getAccumRedBits() +
                                      capabilities.getAccumGreenBits() +
                                      capabilities.getAccumBlueBits() +
                                      capabilities.getAccumAlphaBits());
          iattributes[niattribs++] = GL.WGL_ACCUM_RED_BITS_ARB;
          iattributes[niattribs++] = capabilities.getAccumRedBits();
          iattributes[niattribs++] = GL.WGL_ACCUM_GREEN_BITS_ARB;
          iattributes[niattribs++] = capabilities.getAccumGreenBits();
          iattributes[niattribs++] = GL.WGL_ACCUM_BLUE_BITS_ARB;
          iattributes[niattribs++] = capabilities.getAccumBlueBits();
          iattributes[niattribs++] = GL.WGL_ACCUM_ALPHA_BITS_ARB;
          iattributes[niattribs++] = capabilities.getAccumAlphaBits();
        }
        if (haveWGLARBMultisample) {
          if (capabilities.getSampleBuffers()) {
            iattributes[niattribs++] = GL.WGL_SAMPLE_BUFFERS_ARB;
            iattributes[niattribs++] = GL.GL_TRUE;
            iattributes[niattribs++] = GL.WGL_SAMPLES_ARB;
            iattributes[niattribs++] = capabilities.getNumSamples();
          }
        }
          
        int[] pformats = new int[MAX_PFORMATS];
        int[] numFormatsTmp = new int[1];
        if (dummyGL.wglChoosePixelFormatARB(hdc,
                                            iattributes,
                                            fattributes,
                                            MAX_PFORMATS,
                                            pformats, 
                                            numFormatsTmp)) {
          numFormats = numFormatsTmp[0];
          if (numFormats > 0) {
            // Remove one-basing of pixel format (added on later)
            recommendedPixelFormat = pformats[0] - 1;
            if (DEBUG) {
              System.err.println("Used wglChoosePixelFormatARB to recommend pixel format " + recommendedPixelFormat);
            }
          }
        } else {
          if (DEBUG) {
            System.err.println("wglChoosePixelFormatARB failed: " + WGL.GetLastError() );
            Thread.dumpStack();
          }
        }
        if (DEBUG) {
          if (recommendedPixelFormat < 0) {
            System.err.print("wglChoosePixelFormatARB didn't recommend a pixel format");
            if (capabilities.getSampleBuffers()) {
              System.err.print(" for multisampled GLCapabilities");
            }
            System.err.println();
          }
        }

        // Produce a list of GLCapabilities to give to the
        // GLCapabilitiesChooser.
        // Use wglGetPixelFormatAttribivARB instead of
        // DescribePixelFormat to get higher-precision information
        // about the pixel format (should make the GLCapabilities
        // more precise as well...i.e., remove the
        // "HardwareAccelerated" bit, which is basically
        // meaningless, and put in whether it can render to a
        // window, to a pbuffer, or to a pixmap)
        niattribs = 0;
        iattributes[0] = GL.WGL_NUMBER_PIXEL_FORMATS_ARB;
        if (!dummyGL.wglGetPixelFormatAttribivARB(hdc, 0, 0, 1, iattributes, iresults)) {
          throw new GLException("Unable to enumerate pixel formats of window using wglGetPixelFormatAttribivARB: " + WGL.GetLastError());
        }
        numFormats = iresults[0];
        // Should we be filtering out the pixel formats which aren't
        // applicable, as we are doing here?
        // We don't have enough information in the GLCapabilities to
        // represent those that aren't...
        iattributes[niattribs++] = GL.WGL_DRAW_TO_WINDOW_ARB;
        iattributes[niattribs++] = GL.WGL_ACCELERATION_ARB;
        iattributes[niattribs++] = GL.WGL_SUPPORT_OPENGL_ARB;
        iattributes[niattribs++] = GL.WGL_DEPTH_BITS_ARB;
        iattributes[niattribs++] = GL.WGL_STENCIL_BITS_ARB;
        iattributes[niattribs++] = GL.WGL_DOUBLE_BUFFER_ARB;
        iattributes[niattribs++] = GL.WGL_STEREO_ARB;
        iattributes[niattribs++] = GL.WGL_PIXEL_TYPE_ARB;
        iattributes[niattribs++] = GL.WGL_RED_BITS_ARB;
        iattributes[niattribs++] = GL.WGL_GREEN_BITS_ARB;
        iattributes[niattribs++] = GL.WGL_BLUE_BITS_ARB;
        iattributes[niattribs++] = GL.WGL_ALPHA_BITS_ARB;
        iattributes[niattribs++] = GL.WGL_ACCUM_RED_BITS_ARB;
        iattributes[niattribs++] = GL.WGL_ACCUM_GREEN_BITS_ARB;
        iattributes[niattribs++] = GL.WGL_ACCUM_BLUE_BITS_ARB;
        iattributes[niattribs++] = GL.WGL_ACCUM_ALPHA_BITS_ARB;
        if (haveWGLARBMultisample) {
          iattributes[niattribs++] = GL.WGL_SAMPLE_BUFFERS_ARB;
          iattributes[niattribs++] = GL.WGL_SAMPLES_ARB;
        }

        availableCaps = new GLCapabilities[numFormats];
        for (int i = 0; i < numFormats; i++) {
          if (!dummyGL.wglGetPixelFormatAttribivARB(hdc, i+1, 0, niattribs, iattributes, iresults)) {
            throw new GLException("Error getting pixel format attributes for pixel format " + (i + 1) + " of device context");
          }
          availableCaps[i] = iattributes2GLCapabilities(iattributes, iresults, niattribs, true);
        }
        if( freeWGLC ) {
          WGL.wglMakeCurrent( 0, 0 );
        }
      } else {
        if (DEBUG) {
          System.err.println("Using ChoosePixelFormat because no wglChoosePixelFormatARB: dummyGL = " + dummyGL);
        }
        pfd = glCapabilities2PFD(capabilities, onscreen);
        // Remove one-basing of pixel format (added on later)
        recommendedPixelFormat = WGL.ChoosePixelFormat(hdc, pfd) - 1;

        numFormats = WGL.DescribePixelFormat(hdc, 1, 0, null);
        if (numFormats == 0) {
          throw new GLException("Unable to enumerate pixel formats of window for GLCapabilitiesChooser");
        }
        availableCaps = new GLCapabilities[numFormats];
        for (int i = 0; i < numFormats; i++) {
          if (WGL.DescribePixelFormat(hdc, 1 + i, pfd.size(), pfd) == 0) {
            throw new GLException("Error describing pixel format " + (1 + i) + " of device context");
          }
          availableCaps[i] = pfd2GLCapabilities(pfd);
        }
      }
      // Supply information to chooser
      pixelFormat = chooser.chooseCapabilities(capabilities, availableCaps, recommendedPixelFormat);
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
        throw new GLException("Error re-describing the chosen pixel format: " + WGL.GetLastError());
      }
    } else {
      // For now, use ChoosePixelFormat for offscreen surfaces until
      // we figure out how to properly choose an offscreen-
      // compatible pixel format
      pfd = glCapabilities2PFD(capabilities, onscreen);
      pixelFormat = WGL.ChoosePixelFormat(hdc, pfd);
    }
    if (!WGL.SetPixelFormat(hdc, pixelFormat, pfd)) {
      throw new GLException("Unable to set pixel format");
    }
    hglrc = WGL.wglCreateContext(hdc);
    if (DEBUG) {
      System.err.println("!!! Created OpenGL context " + hglrc);
    }
    if (hglrc == 0) {
      throw new GLException("Unable to create OpenGL context");
    }
  }

  protected long getHGLRC() {
    return hglrc;
  }

  static PIXELFORMATDESCRIPTOR glCapabilities2PFD(GLCapabilities caps, boolean onscreen) {
    int colorDepth = (caps.getRedBits() +
                      caps.getGreenBits() +
                      caps.getBlueBits());
    if (colorDepth < 15) {
      throw new GLException("Bit depths < 15 (i.e., non-true-color) not supported");
    }
    PIXELFORMATDESCRIPTOR pfd = newPixelFormatDescriptor();
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

  static PIXELFORMATDESCRIPTOR newPixelFormatDescriptor() {
    PIXELFORMATDESCRIPTOR pfd = new PIXELFORMATDESCRIPTOR();
    pfd.nSize((short) pfd.size());
    pfd.nVersion((short) 1);
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

  static GLCapabilities iattributes2GLCapabilities(int[] iattribs,
                                                   int[] iresults,
                                                   int   niattribs,
                                                   boolean requireRenderToWindow) {
    GLCapabilities res = new GLCapabilities();
    for (int i = 0; i < niattribs; i++) {
      switch (iattribs[i]) {
        case GL.WGL_DRAW_TO_WINDOW_ARB:
          if (iresults[i] != GL.GL_TRUE)
            return null;
          break;

        case GL.WGL_ACCELERATION_ARB:
          res.setHardwareAccelerated(iresults[i] == GL.WGL_FULL_ACCELERATION_ARB);
          break;

        case GL.WGL_SUPPORT_OPENGL_ARB:
          if (iresults[i] != GL.GL_TRUE)
            return null;
          break;

        case GL.WGL_DEPTH_BITS_ARB:
          res.setDepthBits(iresults[i]);
          break;

        case GL.WGL_STENCIL_BITS_ARB:
          res.setStencilBits(iresults[i]);
          break;

        case GL.WGL_DOUBLE_BUFFER_ARB:
          res.setDoubleBuffered(iresults[i] == GL.GL_TRUE);
          break;

        case GL.WGL_STEREO_ARB:
          res.setStereo(iresults[i] == GL.GL_TRUE);
          break;

        case GL.WGL_PIXEL_TYPE_ARB:
          if (iresults[i] != GL.WGL_TYPE_RGBA_ARB)
            return null;
          break;

        case GL.WGL_RED_BITS_ARB:
          res.setRedBits(iresults[i]);
          break;
          
        case GL.WGL_GREEN_BITS_ARB:
          res.setGreenBits(iresults[i]);
          break;

        case GL.WGL_BLUE_BITS_ARB:
          res.setBlueBits(iresults[i]);
          break;

        case GL.WGL_ALPHA_BITS_ARB:
          res.setAlphaBits(iresults[i]);
          break;

        case GL.WGL_ACCUM_RED_BITS_ARB:
          res.setAccumRedBits(iresults[i]);
          break;

        case GL.WGL_ACCUM_GREEN_BITS_ARB:
          res.setAccumGreenBits(iresults[i]);
          break;

        case GL.WGL_ACCUM_BLUE_BITS_ARB:
          res.setAccumBlueBits(iresults[i]);
          break;

        case GL.WGL_ACCUM_ALPHA_BITS_ARB:
          res.setAccumAlphaBits(iresults[i]);
          break;

        case GL.WGL_SAMPLE_BUFFERS_ARB:
          res.setSampleBuffers(iresults[i] == GL.GL_TRUE);
          break;

        case GL.WGL_SAMPLES_ARB:
          res.setNumSamples(iresults[i]);
          break;

        default:
          throw new GLException("Unknown pixel format attribute " + iattribs[i]);
      }
    }
    return res;
  }
}
