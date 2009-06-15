/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.sun.opengl.impl.windows.wgl;

import java.util.*;
import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.gluegen.runtime.NativeLibrary;

public class WindowsWGLGraphicsConfiguration extends DefaultGraphicsConfiguration implements Cloneable {
    // Keep this under the same debug flag as the drawable factory for convenience
    protected static final boolean DEBUG = com.sun.opengl.impl.Debug.debug("GraphicsConfiguration");
    
    protected static final int MAX_PFORMATS = 256;
    protected static final int MAX_ATTRIBS  = 256;

    private PIXELFORMATDESCRIPTOR pixelfmt;
    private int pixelfmtID;
    private boolean isChosen = false;
    private GLCapabilitiesChooser chooser;

    public WindowsWGLGraphicsConfiguration(AbstractGraphicsScreen screen, GLCapabilities capsChosen, GLCapabilities capsRequested,
                                           PIXELFORMATDESCRIPTOR pixelfmt, int pixelfmtID, GLCapabilitiesChooser chooser) {
        super(screen, capsChosen, capsRequested);
        this.chooser=chooser;
        this.pixelfmt = pixelfmt;
        this.pixelfmtID = pixelfmtID;
    }

    public Object clone() {
        return super.clone();
    }

    protected void updateGraphicsConfiguration(GLDrawableFactory factory, NativeWindow nativeWindow, boolean useOffScreen) {
        WindowsWGLGraphicsConfigurationFactory.updateGraphicsConfiguration(chooser, factory, nativeWindow, useOffScreen);
    }
    protected void setCapsPFD(GLCapabilities caps, PIXELFORMATDESCRIPTOR pfd, int pfdID) {
        // FIXME: setScreen ( .. )
        this.pixelfmt = pfd;
        this.pixelfmtID = pfdID;
        setChosenCapabilities(caps);
        isChosen=true;
    }

    public boolean getCapabilitiesChosen() {
        return isChosen;
    }

    public PIXELFORMATDESCRIPTOR getPixelFormat()   { return pixelfmt; }
    public int getPixelFormatID() { return pixelfmtID; }

    public static boolean GLCapabilities2AttribList(GLCapabilities caps,
                                                  int[] iattributes,
                                                  WGLExt wglExt,
                                                  boolean pbuffer,
                                                  int[] floatMode) throws GLException {
        if (!wglExt.isExtensionAvailable("WGL_ARB_pixel_format")) {
          return false;
        }

        int niattribs = 0;

        iattributes[niattribs++] = WGLExt.WGL_SUPPORT_OPENGL_ARB;
        iattributes[niattribs++] = GL.GL_TRUE;
        if (pbuffer) {
          iattributes[niattribs++] = WGLExt.WGL_DRAW_TO_PBUFFER_ARB;
          iattributes[niattribs++] = GL.GL_TRUE;
        } else {
          iattributes[niattribs++] = WGLExt.WGL_DRAW_TO_WINDOW_ARB;
          iattributes[niattribs++] = GL.GL_TRUE;
        }

        iattributes[niattribs++] = WGLExt.WGL_DOUBLE_BUFFER_ARB;
        if (caps.getDoubleBuffered()) {
          iattributes[niattribs++] = GL.GL_TRUE;
        } else {
          iattributes[niattribs++] = GL.GL_FALSE;
        }

        iattributes[niattribs++] = WGLExt.WGL_STEREO_ARB;
        if (caps.getStereo()) {
          iattributes[niattribs++] = GL.GL_TRUE;
        } else {
          iattributes[niattribs++] = GL.GL_FALSE;
        }
        
        iattributes[niattribs++] = WGLExt.WGL_DEPTH_BITS_ARB;
        iattributes[niattribs++] = caps.getDepthBits();
        iattributes[niattribs++] = WGLExt.WGL_RED_BITS_ARB;
        iattributes[niattribs++] = caps.getRedBits();
        iattributes[niattribs++] = WGLExt.WGL_GREEN_BITS_ARB;
        iattributes[niattribs++] = caps.getGreenBits();
        iattributes[niattribs++] = WGLExt.WGL_BLUE_BITS_ARB;
        iattributes[niattribs++] = caps.getBlueBits();
        iattributes[niattribs++] = WGLExt.WGL_ALPHA_BITS_ARB;
        iattributes[niattribs++] = caps.getAlphaBits();
        iattributes[niattribs++] = WGLExt.WGL_STENCIL_BITS_ARB;
        iattributes[niattribs++] = caps.getStencilBits();
        if (caps.getAccumRedBits()   > 0 ||
            caps.getAccumGreenBits() > 0 ||
            caps.getAccumBlueBits()  > 0 ||
            caps.getAccumAlphaBits() > 0) {
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_BITS_ARB;
          iattributes[niattribs++] = (caps.getAccumRedBits() +
                                      caps.getAccumGreenBits() +
                                      caps.getAccumBlueBits() +
                                      caps.getAccumAlphaBits());
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_RED_BITS_ARB;
          iattributes[niattribs++] = caps.getAccumRedBits();
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_GREEN_BITS_ARB;
          iattributes[niattribs++] = caps.getAccumGreenBits();
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_BLUE_BITS_ARB;
          iattributes[niattribs++] = caps.getAccumBlueBits();
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_ALPHA_BITS_ARB;
          iattributes[niattribs++] = caps.getAccumAlphaBits();
        }

        if (wglExt.isExtensionAvailable("WGL_ARB_multisample")) {
          if (caps.getSampleBuffers()) {
            iattributes[niattribs++] = WGLExt.WGL_SAMPLE_BUFFERS_ARB;
            iattributes[niattribs++] = GL.GL_TRUE;
            iattributes[niattribs++] = WGLExt.WGL_SAMPLES_ARB;
            iattributes[niattribs++] = caps.getNumSamples();
          }
        }

        boolean rtt      = caps.getPbufferRenderToTexture();
        boolean rect     = caps.getPbufferRenderToTextureRectangle();
        boolean useFloat = caps.getPbufferFloatingPointBuffers();
        boolean ati      = false;
        if (pbuffer) {
          // Check some invariants and set up some state
          if (rect && !rtt) {
            throw new GLException("Render-to-texture-rectangle requires render-to-texture to be specified");
          }

          if (rect) {
            if (!wglExt.isExtensionAvailable("GL_NV_texture_rectangle")) {
              throw new GLException("Render-to-texture-rectangle requires GL_NV_texture_rectangle extension");
            }
          }

          if (useFloat) {
            if (!wglExt.isExtensionAvailable("WGL_ATI_pixel_format_float") &&
                !wglExt.isExtensionAvailable("WGL_NV_float_buffer")) {
              throw new GLException("Floating-point pbuffers not supported by this hardware");
            }

            // Prefer NVidia extension over ATI
            if (wglExt.isExtensionAvailable("WGL_NV_float_buffer")) {
              ati = false;
              floatMode[0] = GLPbuffer.NV_FLOAT;
            } else {
              ati = true;
              floatMode[0] = GLPbuffer.ATI_FLOAT;
            }
            if (DEBUG) {
              System.err.println("Using " + (ati ? "ATI" : "NVidia") + " floating-point extension");
            }
          }

          // See whether we need to change the pixel type to support ATI's
          // floating-point pbuffers
          if (useFloat && ati) {
            if (rtt) {
              throw new GLException("Render-to-floating-point-texture not supported on ATI hardware");
            } else {
              iattributes[niattribs++] = WGLExt.WGL_PIXEL_TYPE_ARB;
              iattributes[niattribs++] = WGLExt.WGL_TYPE_RGBA_FLOAT_ARB;
            }
          } else {
            if (!rtt) {
              // Currently we don't support non-truecolor visuals in the
              // GLCapabilities, so we don't offer the option of making
              // color-index pbuffers.
              iattributes[niattribs++] = WGLExt.WGL_PIXEL_TYPE_ARB;
              iattributes[niattribs++] = WGLExt.WGL_TYPE_RGBA_ARB;
            }
          }

          if (useFloat && !ati) {
            iattributes[niattribs++] = WGLExt.WGL_FLOAT_COMPONENTS_NV;
            iattributes[niattribs++] = GL.GL_TRUE;
          }

          if (rtt) {
            if (useFloat) {
              assert(!ati);
              if (!rect) {
                throw new GLException("Render-to-floating-point-texture only supported on NVidia hardware with render-to-texture-rectangle");
              }
              iattributes[niattribs++] = WGLExt.WGL_BIND_TO_TEXTURE_RECTANGLE_FLOAT_RGB_NV;
              iattributes[niattribs++] = GL.GL_TRUE;
            } else {
              iattributes[niattribs++] = rect ? WGLExt.WGL_BIND_TO_TEXTURE_RECTANGLE_RGB_NV : WGLExt.WGL_BIND_TO_TEXTURE_RGB_ARB;
              iattributes[niattribs++] = GL.GL_TRUE;
            }
          }
        } else {
          iattributes[niattribs++] = WGLExt.WGL_PIXEL_TYPE_ARB;
          iattributes[niattribs++] = WGLExt.WGL_TYPE_RGBA_ARB;
        }
        iattributes[niattribs++] = 0;

        return true;
    }

    public static GLCapabilities AttribList2GLCapabilities(GLProfile glp, int[] iattribs,
                                                         int niattribs,
                                                         int[] iresults,
                                                         boolean requireRenderToWindow) {
        GLCapabilities res = new GLCapabilities(glp);
        for (int i = 0; i < niattribs; i++) {
          int attr = iattribs[i];
          switch (attr) {
            case WGLExt.WGL_DRAW_TO_WINDOW_ARB:
              if (requireRenderToWindow && iresults[i] != GL.GL_TRUE) {
                return null;
              }
              break;

            case WGLExt.WGL_DRAW_TO_PBUFFER_ARB:
              break;

            case WGLExt.WGL_ACCELERATION_ARB:
              res.setHardwareAccelerated(iresults[i] == WGLExt.WGL_FULL_ACCELERATION_ARB);
              break;

            case WGLExt.WGL_SUPPORT_OPENGL_ARB:
              if (iresults[i] != GL.GL_TRUE) {
                return null;
              }
              break;

            case WGLExt.WGL_DEPTH_BITS_ARB:
              res.setDepthBits(iresults[i]);
              break;

            case WGLExt.WGL_STENCIL_BITS_ARB:
              res.setStencilBits(iresults[i]);
              break;

            case WGLExt.WGL_DOUBLE_BUFFER_ARB:
              res.setDoubleBuffered(iresults[i] == GL.GL_TRUE);
              break;

            case WGLExt.WGL_STEREO_ARB:
              res.setStereo(iresults[i] == GL.GL_TRUE);
              break;

            case WGLExt.WGL_PIXEL_TYPE_ARB:
              // Fail softly with unknown results here
              if (iresults[i] == WGLExt.WGL_TYPE_RGBA_ARB||
                  iresults[i] == WGLExt.WGL_TYPE_RGBA_FLOAT_ARB) {
                res.setPbufferFloatingPointBuffers(true);
              }
              break;

            case WGLExt.WGL_FLOAT_COMPONENTS_NV:
              if (iresults[i] != 0) {
                res.setPbufferFloatingPointBuffers(true);
              }
              break;

            case WGLExt.WGL_RED_BITS_ARB:
              res.setRedBits(iresults[i]);
              break;
              
            case WGLExt.WGL_GREEN_BITS_ARB:
              res.setGreenBits(iresults[i]);
              break;

            case WGLExt.WGL_BLUE_BITS_ARB:
              res.setBlueBits(iresults[i]);
              break;

            case WGLExt.WGL_ALPHA_BITS_ARB:
              res.setAlphaBits(iresults[i]);
              break;

            case WGLExt.WGL_ACCUM_RED_BITS_ARB:
              res.setAccumRedBits(iresults[i]);
              break;

            case WGLExt.WGL_ACCUM_GREEN_BITS_ARB:
              res.setAccumGreenBits(iresults[i]);
              break;

            case WGLExt.WGL_ACCUM_BLUE_BITS_ARB:
              res.setAccumBlueBits(iresults[i]);
              break;

            case WGLExt.WGL_ACCUM_ALPHA_BITS_ARB:
              res.setAccumAlphaBits(iresults[i]);
              break;

            case WGLExt.WGL_SAMPLE_BUFFERS_ARB:
              res.setSampleBuffers(iresults[i] != 0);
              break;

            case WGLExt.WGL_SAMPLES_ARB:
              res.setNumSamples(iresults[i]);
              break;

            default:
              throw new GLException("Unknown pixel format attribute " + iattribs[i]);
          }
        }
        return res;
    }

  // PIXELFORMAT

    public static GLCapabilities PFD2GLCapabilities(GLProfile glp, PIXELFORMATDESCRIPTOR pfd) {
        if ((pfd.dwFlags() & WGL.PFD_SUPPORT_OPENGL) == 0) {
          return null;
        }
        GLCapabilities res = new GLCapabilities(glp);
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
        /* FIXME: Missing ??
        if (GLXUtil.isMultisampleAvailable()) {
          res.setSampleBuffers(glXGetFBConfig(display, fbcfg, GLX.GLX_SAMPLE_BUFFERS, tmp, 0) != 0);
          res.setNumSamples   (glXGetFBConfig(display, fbcfg, GLX.GLX_SAMPLES,        tmp, 0));
        }
        res.setBackgroundOpaque(glXGetFBConfig(display, fbcfg, GLX.GLX_TRANSPARENT_TYPE, tmp, 0) != GLX.GLX_NONE);
        try { 
            res.setPbufferFloatingPointBuffers(glXGetFBConfig(display, fbcfg, GLXExt.GLX_FLOAT_COMPONENTS_NV, tmp, 0) != GL.GL_FALSE);
        } catch (Exception e) {}
        */
        return res;
  }

  public static PIXELFORMATDESCRIPTOR GLCapabilities2PFD(GLCapabilities caps, boolean offscreen) {
    int colorDepth = (caps.getRedBits() +
                      caps.getGreenBits() +
                      caps.getBlueBits());
    if (colorDepth < 15) {
      throw new GLException("Bit depths < 15 (i.e., non-true-color) not supported");
    }
    PIXELFORMATDESCRIPTOR pfd = createPixelFormatDescriptor();
    int pfdFlags = (WGL.PFD_SUPPORT_OPENGL |
                    WGL.PFD_GENERIC_ACCELERATED);
    if (caps.getDoubleBuffered()) {
      pfdFlags |= WGL.PFD_DOUBLEBUFFER;
    }
    if (offscreen) {
      pfdFlags |= WGL.PFD_DRAW_TO_BITMAP;
    } else {
      pfdFlags |= WGL.PFD_DRAW_TO_WINDOW;
    }
    if (caps.getStereo()) {
      pfdFlags |= WGL.PFD_STEREO;
    }
    pfd.dwFlags(pfdFlags);
    pfd.iPixelType((byte) WGL.PFD_TYPE_RGBA);
    pfd.cColorBits((byte) colorDepth);
    pfd.cRedBits  ((byte) caps.getRedBits());
    pfd.cGreenBits((byte) caps.getGreenBits());
    pfd.cBlueBits ((byte) caps.getBlueBits());
    pfd.cAlphaBits((byte) caps.getAlphaBits());
    int accumDepth = (caps.getAccumRedBits() +
                      caps.getAccumGreenBits() +
                      caps.getAccumBlueBits());
    pfd.cAccumBits     ((byte) accumDepth);
    pfd.cAccumRedBits  ((byte) caps.getAccumRedBits());
    pfd.cAccumGreenBits((byte) caps.getAccumGreenBits());
    pfd.cAccumBlueBits ((byte) caps.getAccumBlueBits());
    pfd.cAccumAlphaBits((byte) caps.getAccumAlphaBits());
    pfd.cDepthBits((byte) caps.getDepthBits());
    pfd.cStencilBits((byte) caps.getStencilBits());
    pfd.iLayerType((byte) WGL.PFD_MAIN_PLANE);

    /* FIXME: Missing: 
      caps.getSampleBuffers()
      caps.getNumSamples   ()
    }
    caps.getBackgroundOpaque()
    try { 
        caps.getPbufferFloatingPointBuffers()
    } catch (Exception e) {}
    */
    return pfd;
  }

  public static PIXELFORMATDESCRIPTOR createPixelFormatDescriptor() {
    PIXELFORMATDESCRIPTOR pfd = PIXELFORMATDESCRIPTOR.create();
    pfd.nSize((short) pfd.size());
    pfd.nVersion((short) 1);
    return pfd;
  }

  public String toString() {
    return "WindowsWGLGraphicsConfiguration["+getScreen()+", pfdID " + pixelfmtID + 
                                            ",\n\trequested " + getRequestedCapabilities() +
                                            ",\n\tchosen    " + getChosenCapabilities() +
                                            "]";
  }
}

