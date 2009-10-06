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
    private boolean choosenByWGLPixelFormat=false;

    public WindowsWGLGraphicsConfiguration(AbstractGraphicsScreen screen, GLCapabilities capsChosen, GLCapabilities capsRequested,
                                           PIXELFORMATDESCRIPTOR pixelfmt, int pixelfmtID, GLCapabilitiesChooser chooser) {
        super(screen, capsChosen, capsRequested);
        this.chooser=chooser;
        this.pixelfmt = pixelfmt;
        this.pixelfmtID = pixelfmtID;
    }

    public static WindowsWGLGraphicsConfiguration create(long hdc, int pfdID, 
                                                         GLProfile glp, AbstractGraphicsScreen screen, boolean onscreen, boolean usePBuffer)
    {
        if(pfdID<=0) {
            throw new GLException("Invalid pixelformat id "+pfdID);
        }
        if(null==glp) {
          glp = GLProfile.getDefault();
        }
        PIXELFORMATDESCRIPTOR pfd = createPixelFormatDescriptor();
        if (WGL.DescribePixelFormat(hdc, pfdID, pfd.size(), pfd) == 0) {
            throw new GLException("Unable to describe pixel format " + pfdID);
        }

        GLCapabilities caps = PFD2GLCapabilities(glp, pfd, onscreen, usePBuffer);
        if(null==caps) {
            throw new GLException("Couldn't choose Capabilities by: HDC 0x"+Long.toHexString(hdc)+", pfdID "+pfdID);
        }
        WindowsWGLGraphicsConfiguration cfg = new WindowsWGLGraphicsConfiguration(screen, caps, caps, pfd, pfdID, new DefaultGLCapabilitiesChooser());
        cfg.setCapsPFD(caps, pfd, pfdID, false);

        return cfg;
    }

    public Object clone() {
        return super.clone();
    }

    /** Update config - before having a valid context */
    protected void updateGraphicsConfiguration(GLDrawableFactory factory, NativeWindow nativeWindow) {
        WindowsWGLGraphicsConfigurationFactory.updateGraphicsConfiguration(chooser, factory, nativeWindow);
    }

    /** Update config - after having a valid and current context */
    protected void updateCapabilitiesByWGL(GLContextImpl context) {
        if(choosenByWGLPixelFormat) return; // already done ..

        GLCapabilities capabilities = (GLCapabilities) getRequestedCapabilities();
        boolean onscreen = capabilities.isOnscreen();
        boolean usePBuffer = capabilities.isPBuffer();
        GLProfile glp = capabilities.getGLProfile();

        WGLExt wglExt = (WGLExt) context.getPlatformGLExtensions();
        GLDrawable drawable = context.getGLDrawable();
        NativeWindow nativeWindow = drawable.getNativeWindow();
        long hdc = nativeWindow.getSurfaceHandle();

        GLCapabilities[] caps = HDC2GLCapabilities(wglExt, hdc, getPixelFormatID(), glp, true, onscreen, usePBuffer);
        if(null!=caps && null!=caps[0]) {
            setCapsPFD(caps[0], getPixelFormat(), getPixelFormatID(), true);
        }
    }

    protected void setCapsPFD(GLCapabilities caps, PIXELFORMATDESCRIPTOR pfd, int pfdID, boolean choosenByWGLPixelFormat) {
        this.pixelfmt = pfd;
        this.pixelfmtID = pfdID;
        setChosenCapabilities(caps);
        this.isChosen=true;
        this.choosenByWGLPixelFormat=choosenByWGLPixelFormat;
        if (DEBUG) {
            System.err.println("*** setCapsPFD: WGL-Choosen "+choosenByWGLPixelFormat+", pfdID "+pfdID+", "+caps);
        }
    }

    public boolean getCapabilitiesChosen() {
        return isChosen;
    }

    public PIXELFORMATDESCRIPTOR getPixelFormat()   { return pixelfmt; }
    public int getPixelFormatID() { return pixelfmtID; }
    public boolean isChoosenByWGL() { return choosenByWGLPixelFormat; }

    private static int haveWGLChoosePixelFormatARB = -1;
    private static int haveWGLARBMultisample = -1;

    public static GLCapabilities[] HDC2GLCapabilities(WGLExt wglExt, long hdc, int pfdIDOnly,
                                                      GLProfile glp, boolean relaxed, boolean onscreen, boolean usePBuffer) {
    
        if(haveWGLChoosePixelFormatARB<0) {
            haveWGLChoosePixelFormatARB = wglExt.isExtensionAvailable("WGL_ARB_pixel_format")?1:0;
        }
        if(haveWGLARBMultisample<0) {
            haveWGLARBMultisample = wglExt.isExtensionAvailable("WGL_ARB_multisample")?1:0;
        }
        if (0==haveWGLChoosePixelFormatARB) {
            return null;
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
        GLCapabilities[] availableCaps = null;
        int numFormats = 0;
        int niattribs = 0;
        int[] iattributes = new int  [2*MAX_ATTRIBS];
        int[] iresults    = new int  [2*MAX_ATTRIBS];

        iattributes[0] = WGLExt.WGL_NUMBER_PIXEL_FORMATS_ARB;
        if (wglExt.wglGetPixelFormatAttribivARB(hdc, 0, 0, 1, iattributes, 0, iresults, 0)) {
          numFormats = iresults[0];

          if (DEBUG) {
            System.err.println("wglGetPixelFormatAttribivARB reported WGL_NUMBER_PIXEL_FORMATS = " + numFormats);
          }

          if(pfdIDOnly>0 && pfdIDOnly>numFormats) {
            throw new GLException("Invalid pixelformat ID " + pfdIDOnly + " (should be between 1 and " + numFormats + ")");
          }

          // Should we be filtering out the pixel formats which aren't
          // applicable, as we are doing here?
          // We don't have enough information in the GLCapabilities to
          // represent those that aren't...
          iattributes[niattribs++] = WGLExt.WGL_DRAW_TO_WINDOW_ARB;
          iattributes[niattribs++] = WGLExt.WGL_ACCELERATION_ARB;
          iattributes[niattribs++] = WGLExt.WGL_SUPPORT_OPENGL_ARB;
          iattributes[niattribs++] = WGLExt.WGL_DEPTH_BITS_ARB;
          iattributes[niattribs++] = WGLExt.WGL_STENCIL_BITS_ARB;
          iattributes[niattribs++] = WGLExt.WGL_DOUBLE_BUFFER_ARB;
          iattributes[niattribs++] = WGLExt.WGL_STEREO_ARB;
          iattributes[niattribs++] = WGLExt.WGL_PIXEL_TYPE_ARB;
          iattributes[niattribs++] = WGLExt.WGL_RED_BITS_ARB;
          iattributes[niattribs++] = WGLExt.WGL_GREEN_BITS_ARB;
          iattributes[niattribs++] = WGLExt.WGL_BLUE_BITS_ARB;
          iattributes[niattribs++] = WGLExt.WGL_ALPHA_BITS_ARB;
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_RED_BITS_ARB;
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_GREEN_BITS_ARB;
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_BLUE_BITS_ARB;
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_ALPHA_BITS_ARB;
          if (1==haveWGLARBMultisample) {
            iattributes[niattribs++] = WGLExt.WGL_SAMPLE_BUFFERS_ARB;
            iattributes[niattribs++] = WGLExt.WGL_SAMPLES_ARB;
          }

          if(pfdIDOnly>0) {
              availableCaps = new GLCapabilities[1];
              if (!wglExt.wglGetPixelFormatAttribivARB(hdc, pfdIDOnly, 0, niattribs, iattributes, 0, iresults, 0)) {
                  throw new GLException("Error getting pixel format attributes for pixel format " + pfdIDOnly + " of device context");
              }
              availableCaps[0] = AttribList2GLCapabilities(glp, iattributes, niattribs, iresults, 
                                                           relaxed, onscreen, usePBuffer);
          } else {
              availableCaps = new GLCapabilities[numFormats];
              for (int i = 0; i < numFormats; i++) {
                if (!wglExt.wglGetPixelFormatAttribivARB(hdc, i+1, 0, niattribs, iattributes, 0, iresults, 0)) {
                  throw new GLException("Error getting pixel format attributes for pixel format " + (i + 1) + " of device context");
                }
                availableCaps[i] = AttribList2GLCapabilities(glp, iattributes, niattribs, iresults, 
                                                             relaxed, onscreen, usePBuffer);
              }
          }
        } else {
          long lastErr = WGL.GetLastError();
          // Intel Extreme graphics fails with a zero error code
          if (lastErr != 0) {
            throw new GLException("Unable to enumerate pixel formats of window using wglGetPixelFormatAttribivARB: error code " + WGL.GetLastError());
          }
        }
        return availableCaps;
    }

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

    public static final int WINDOW_BIT  = 1 << 0 ;
    public static final int BITMAP_BIT  = 1 << 1 ;
    public static final int PBUFFER_BIT = 1 << 2 ;

    public static int WGLConfig2DrawableTypeBits(int[] iattribs,
                                                 int niattribs,
                                                 int[] iresults) {
        int val = 0;

        for (int i = 0; i < niattribs; i++) {
          int attr = iattribs[i];
          switch (attr) {
            case WGLExt.WGL_DRAW_TO_WINDOW_ARB:
                if(iresults[i] == GL.GL_TRUE) val |= WINDOW_BIT;
                break;
            case WGLExt.WGL_DRAW_TO_BITMAP_ARB:
                if(iresults[i] == GL.GL_TRUE) val |= BITMAP_BIT;
                break;
            case WGLExt.WGL_DRAW_TO_PBUFFER_ARB:
                if(iresults[i] == GL.GL_TRUE) val |= PBUFFER_BIT;
                break;
            }
        }
        return val;
    }

    public static boolean WGLConfigDrawableTypeVerify(int val, boolean onscreen, boolean usePBuffer) {
        boolean res;

        if ( onscreen ) {
            res = ( 0 != (val & WINDOW_BIT) ) ;
        } else {
            res = ( 0 != (val & BITMAP_BIT) ) || usePBuffer ;
        }
        if ( usePBuffer ) {
            res = res && ( 0 != (val & PBUFFER_BIT) ) ;
        }

        return res;
    }
    public static GLCapabilities AttribList2GLCapabilities(GLProfile glp, int[] iattribs,
                                                         int niattribs,
                                                         int[] iresults,
                                                         boolean relaxed, boolean onscreen, boolean usePBuffer) {
        GLCapabilities res = new GLCapabilities(glp);
        int drawableTypeBits = WGLConfig2DrawableTypeBits(iattribs, niattribs, iresults);
        if(WGLConfigDrawableTypeVerify(drawableTypeBits, onscreen, usePBuffer)) {
            res.setOnscreen(onscreen);
            res.setPBuffer(usePBuffer);
        } else if(relaxed) {
            res.setOnscreen( 0 != (drawableTypeBits & WINDOW_BIT) );
            res.setPBuffer ( 0 != (drawableTypeBits & PBUFFER_BIT) );
        } else {
            throw new GLException("WGL DrawableType does not match !!!");
        }

        for (int i = 0; i < niattribs; i++) {
          int attr = iattribs[i];
          switch (attr) {
            case WGLExt.WGL_DRAW_TO_WINDOW_ARB:
            case WGLExt.WGL_DRAW_TO_BITMAP_ARB:
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

    public static GLCapabilities PFD2GLCapabilities(GLProfile glp, PIXELFORMATDESCRIPTOR pfd, boolean onscreen, boolean usePBuffer) {
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
        res.setHardwareAccelerated( ((pfd.dwFlags() & WGL.PFD_GENERIC_FORMAT) == 0) ||
                                    ((pfd.dwFlags() & WGL.PFD_GENERIC_ACCELERATED) != 0) );
        res.setOnscreen      ( onscreen && ((pfd.dwFlags() & WGL.PFD_DRAW_TO_WINDOW) != 0) );
        res.setPBuffer       ( usePBuffer );
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

  public static PIXELFORMATDESCRIPTOR GLCapabilities2PFD(GLCapabilities caps) {
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
    if (caps.isOnscreen()) {
      pfdFlags |= WGL.PFD_DRAW_TO_WINDOW;
    } else {
      pfdFlags |= WGL.PFD_DRAW_TO_BITMAP;
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
    return "WindowsWGLGraphicsConfiguration["+getScreen()+", pfdID " + pixelfmtID + ", wglChoosen "+choosenByWGLPixelFormat+
                                            ",\n\trequested " + getRequestedCapabilities() +
                                            ",\n\tchosen    " + getChosenCapabilities() +
                                            "]";
  }
}

