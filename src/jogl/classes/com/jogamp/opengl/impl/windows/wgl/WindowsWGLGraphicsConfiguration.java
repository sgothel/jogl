/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.impl.windows.wgl;

import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.DefaultGraphicsConfiguration;
import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;

import com.jogamp.nativewindow.impl.windows.GDI;
import com.jogamp.nativewindow.impl.windows.PIXELFORMATDESCRIPTOR;
import com.jogamp.opengl.impl.GLContextImpl;
import javax.media.opengl.GLCapabilitiesImmutable;

public class WindowsWGLGraphicsConfiguration extends DefaultGraphicsConfiguration implements Cloneable {
    // Keep this under the same debug flag as the drawable factory for convenience
    protected static final boolean DEBUG = com.jogamp.opengl.impl.Debug.debug("GraphicsConfiguration");
    
    protected static final int MAX_PFORMATS = 256;
    protected static final int MAX_ATTRIBS  = 256;

    private PIXELFORMATDESCRIPTOR pixelfmt;
    private int pixelfmtID;
    private boolean isChosen = false;
    private GLCapabilitiesChooser chooser;
    private boolean choosenByWGLPixelFormat=false;

    WindowsWGLGraphicsConfiguration(AbstractGraphicsScreen screen, 
                                    GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested,
                                    PIXELFORMATDESCRIPTOR pixelfmt, int pixelfmtID, GLCapabilitiesChooser chooser) {
        super(screen, capsChosen, capsRequested);
        this.chooser=chooser;
        this.pixelfmt = pixelfmt;
        this.pixelfmtID = pixelfmtID;
    }

    static WindowsWGLGraphicsConfiguration create(long hdc, int pfdID, 
                                                  GLProfile glp, AbstractGraphicsScreen screen, boolean onscreen, boolean usePBuffer)
    {
        if(pfdID<=0) {
            throw new GLException("Invalid pixelformat id "+pfdID);
        }
        if(null==glp) {
          glp = GLProfile.getDefault(screen.getDevice());
        }
        PIXELFORMATDESCRIPTOR pfd = createPixelFormatDescriptor();
        if (GDI.DescribePixelFormat(hdc, pfdID, pfd.size(), pfd) == 0) {
            throw new GLException("Unable to describe pixel format " + pfdID);
        }

        GLCapabilitiesImmutable caps = PFD2GLCapabilities(glp, pfd, onscreen, usePBuffer);
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

    final void updateGraphicsConfiguration(GLDrawableFactory factory, NativeSurface ns) {
        WindowsWGLGraphicsConfigurationFactory.updateGraphicsConfiguration(chooser, factory, ns);
    }

    void setCapsPFD(GLCapabilitiesImmutable caps, PIXELFORMATDESCRIPTOR pfd, int pfdID, boolean choosenByWGLPixelFormat) {
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

    static int fillAttribsForGeneralWGLARBQuery(boolean haveWGLARBMultisample, int[] iattributes) {
        int niattribs = 0;
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
        if (haveWGLARBMultisample) {
            iattributes[niattribs++] = WGLExt.WGL_SAMPLE_BUFFERS_ARB;
            iattributes[niattribs++] = WGLExt.WGL_SAMPLES_ARB;
        }
        return niattribs;
    }

    static boolean wglARBPFIDValid(WindowsWGLContext sharedCtx, long hdc, int pfdID) {
        int[] in = new int[1];
        int[] out = new int[1];
        in[0] = WGLExt.WGL_COLOR_BITS_ARB;
        if (!sharedCtx.getWGLExt().wglGetPixelFormatAttribivARB(hdc, pfdID, 0, 1, in, 0, out, 0)) {
            // Some GPU's falsely fails with a zero error code (success)
            return GDI.GetLastError() == GDI.ERROR_SUCCESS ;
        }
        return true;
    }

    static GLCapabilitiesImmutable wglARBPFID2GLCapabilities(WindowsWGLContext sharedCtx, long hdc, int pfdID,
                                                                GLProfile glp, boolean onscreen, boolean usePBuffer) {
        boolean haveWGLChoosePixelFormatARB = sharedCtx.isExtensionAvailable("WGL_ARB_pixel_format");
        if (!haveWGLChoosePixelFormatARB) {
            return null;
        }
        boolean haveWGLARBMultisample = sharedCtx.isExtensionAvailable("WGL_ARB_multisample");

        int[] iattributes = new int  [2*MAX_ATTRIBS];
        int[] iresults    = new int  [2*MAX_ATTRIBS];

        iattributes[0] = WGLExt.WGL_NUMBER_PIXEL_FORMATS_ARB;
        if (sharedCtx.getWGLExt().wglGetPixelFormatAttribivARB(hdc, 0, 0, 1, iattributes, 0, iresults, 0)) {
            if(iresults[0] > 0 ) {
                int niattribs = fillAttribsForGeneralWGLARBQuery(haveWGLARBMultisample, iattributes);
                if (!sharedCtx.getWGLExt().wglGetPixelFormatAttribivARB(hdc, pfdID, 0, niattribs, iattributes, 0, iresults, 0)) {
                  throw new GLException("wglARBPFID2GLCapabilities: Error getting pixel format attributes for pixel format " + pfdID + " of device context");
                }
                return AttribList2GLCapabilities(glp, iattributes, niattribs, iresults,
                                                 onscreen, usePBuffer);
            }
        }
        long lastErr = GDI.GetLastError();
        // Intel Extreme graphics fails with a zero error code
        if (lastErr != 0) {
            throw new GLException("wglARBPFID2GLCapabilities: Unable to enumerate pixel formats of window using wglGetPixelFormatAttribivARB: error code " + GDI.GetLastError());
        }
        return null;
    }

    static int wglChoosePixelFormatARB(long hdc, WindowsWGLContext sharedContext,
                                       GLCapabilitiesImmutable capabilities,
                                       int[] iattributes, int accelerationMode, float[] fattributes,
                                       int[] pformats)
    {
      int numFormats = -1;

      if(WindowsWGLGraphicsConfiguration.GLCapabilities2AttribList(capabilities,
                                                                   iattributes,
                                                                   sharedContext,
                                                                   accelerationMode,
                                                                   false,
                                                                   null)) {
        int[] numFormatsTmp = new int[1];
        if (sharedContext.getWGLExt().wglChoosePixelFormatARB(hdc,
                                           iattributes, 0,
                                           fattributes, 0,
                                           WindowsWGLGraphicsConfiguration.MAX_PFORMATS,
                                           pformats, 0,
                                           numFormatsTmp, 0)) {
          numFormats = numFormatsTmp[0];
          if (DEBUG) {
            System.err.println("wglChoosePixelFormatARB1: NumFormats (wglChoosePixelFormatARB) accelMode 0x" +
                    Integer.toHexString(accelerationMode) + ": " + numFormats + " / " + WindowsWGLGraphicsConfiguration.MAX_PFORMATS);
          }
        } else {
          if (DEBUG) {
            System.err.println("wglChoosePixelFormatARB1: wglChoosePixelFormatARB failed: " + GDI.GetLastError() );
            Thread.dumpStack();
          }
        }
      } else {
          if (DEBUG) {
            System.err.println("wglChoosePixelFormatARB1: GLCapabilities2AttribList failed: " + GDI.GetLastError() );
            Thread.dumpStack();
          }
      }
      return numFormats;
    }

    static GLCapabilitiesImmutable[] wglARBPFIDs2GLCapabilities(WindowsWGLContext sharedCtx, long hdc, int[] pfdIDs, int numFormats,
                                                                GLProfile glp, boolean onscreen, boolean usePBuffer) {
        boolean haveWGLChoosePixelFormatARB = sharedCtx.isExtensionAvailable("WGL_ARB_pixel_format");
        if (!haveWGLChoosePixelFormatARB) {
            return null;
        }
        boolean haveWGLARBMultisample = sharedCtx.isExtensionAvailable("WGL_ARB_multisample");

        GLCapabilitiesImmutable[] caps = new GLCapabilitiesImmutable[numFormats];

        int[] iattributes = new int  [2*MAX_ATTRIBS];
        int[] iresults    = new int  [2*MAX_ATTRIBS];
        int niattribs = fillAttribsForGeneralWGLARBQuery(haveWGLARBMultisample, iattributes);

        for(int i = 0; i<numFormats; i++) {
            if ( pfdIDs[i] >= 1 &&
                 sharedCtx.getWGLExt().wglGetPixelFormatAttribivARB(hdc, pfdIDs[i], 0, niattribs, iattributes, 0, iresults, 0) ) {
                caps[i] = AttribList2GLCapabilities(glp, iattributes, niattribs, iresults, onscreen, usePBuffer);
            } else {
                if (DEBUG) {
                    System.err.println("wglARBPFIDs2GLCapabilities: Cannot get pixel format attributes for pixel format " +
                                       i + "/" + numFormats + ": " + pfdIDs[i]);
                }
                caps[i] = null;
            }
        }
        return caps;
    }

    /**
     *
     * @param sharedCtx
     * @param hdc
     * @param glp
     * @param onscreen
     * @param usePBuffer
     * @param pfIDs stores the PIXELFORMAT ID for the GLCapabilitiesImmutable[]
     * @return the resulting GLCapabilitiesImmutable[]
     */
    static GLCapabilitiesImmutable[] wglARBAllPFIDs2GLCapabilities(WindowsWGLContext sharedCtx, long hdc,
                                                                   GLProfile glp, boolean onscreen, boolean usePBuffer, int[] pfIDs) {
        boolean haveWGLChoosePixelFormatARB = sharedCtx.isExtensionAvailable("WGL_ARB_pixel_format");
        if (!haveWGLChoosePixelFormatARB) {
            return null;
        }
        boolean haveWGLARBMultisample = sharedCtx.isExtensionAvailable("WGL_ARB_multisample");

        // Produce a list of GLCapabilities to give to the
        // GLCapabilitiesChooser.
        // Use wglGetPixelFormatAttribivARB instead of
        // DescribePixelFormat to get higher-precision information
        // about the pixel format (should make the GLCapabilities
        // more precise as well...i.e., remove the
        // "HardwareAccelerated" bit, which is basically
        // meaningless, and put in whether it can render to a
        // window, to a pbuffer, or to a pixmap)
        GLCapabilitiesImmutable[] availableCaps = null;
        int numFormats = 0;
        int niattribs = 0;
        int[] iattributes = new int[2 * MAX_ATTRIBS];
        int[] iresults = new int[2 * MAX_ATTRIBS];

        WGLExt wglExt = sharedCtx.getWGLExt();
        iattributes[0] = WGLExt.WGL_NUMBER_PIXEL_FORMATS_ARB;
        if (wglExt.wglGetPixelFormatAttribivARB(hdc, 0, 0, 1, iattributes, 0, iresults, 0)) {
            numFormats = iresults[0];
            if (DEBUG) {
                System.err.println("wglARBAllPFIDs2GLCapabilities: wglGetPixelFormatAttribivARB reported WGL_NUMBER_PIXEL_FORMATS = " + numFormats + ", pfIDs sz "+pfIDs.length);
            }
            if (numFormats > pfIDs.length) {
                numFormats = pfIDs.length;
            }

            niattribs = fillAttribsForGeneralWGLARBQuery(haveWGLARBMultisample, iattributes);

            availableCaps = new GLCapabilitiesImmutable[numFormats];
            for (int i = 0; i < numFormats; i++) {
                pfIDs[i] = i + 1;
                if (!wglExt.wglGetPixelFormatAttribivARB(hdc, pfIDs[i], 0, niattribs, iattributes, 0, iresults, 0)) {
                    throw new GLException("wglARBAllPFIDs2GLCapabilities: Error getting pixel format attributes for pixel format " + pfIDs[i]);
                }
                availableCaps[i] = AttribList2GLCapabilities(glp, iattributes, niattribs, iresults, onscreen, usePBuffer);
            }
        } else {
            long lastErr = GDI.GetLastError();
            // Some GPU's falsely fails with a zero error code (success)
            if (lastErr != GDI.ERROR_SUCCESS) {
                throw new GLException("wglARBAllPFIDs2GLCapabilities: Unable to enumerate pixel formats of window using wglGetPixelFormatAttribivARB: error code " + lastErr);
            }
        }
        return availableCaps;
    }

    static boolean GLCapabilities2AttribList(GLCapabilitiesImmutable caps,
                                             int[] iattributes,
                                             GLContextImpl sharedCtx,
                                             int accellerationValue,
                                             boolean pbuffer,
                                             int[] floatMode) throws GLException {
        boolean haveWGLChoosePixelFormatARB = sharedCtx.isExtensionAvailable("WGL_ARB_pixel_format");
        boolean haveWGLARBMultisample = sharedCtx.isExtensionAvailable("WGL_ARB_multisample");
        if(DEBUG) {
            System.err.println("HDC2GLCapabilities: ARB_pixel_format: "+haveWGLChoosePixelFormatARB);
            System.err.println("HDC2GLCapabilities: ARB_multisample : "+haveWGLARBMultisample);
        }

        if (!haveWGLChoosePixelFormatARB) {
          return false;
        }

        int niattribs = 0;

        iattributes[niattribs++] = WGLExt.WGL_SUPPORT_OPENGL_ARB;
        iattributes[niattribs++] = GL.GL_TRUE;
        if(accellerationValue>0) {
            iattributes[niattribs++] = WGLExt.WGL_ACCELERATION_ARB;
            iattributes[niattribs++] = accellerationValue;
        }
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

        if (caps.getSampleBuffers() && haveWGLARBMultisample) {
            iattributes[niattribs++] = WGLExt.WGL_SAMPLE_BUFFERS_ARB;
            iattributes[niattribs++] = GL.GL_TRUE;
            iattributes[niattribs++] = WGLExt.WGL_SAMPLES_ARB;
            iattributes[niattribs++] = caps.getNumSamples();
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
            if (!sharedCtx.isExtensionAvailable("GL_NV_texture_rectangle")) {
              throw new GLException("Render-to-texture-rectangle requires GL_NV_texture_rectangle extension");
            }
          }

          if (useFloat) {
            if (!sharedCtx.isExtensionAvailable("WGL_ATI_pixel_format_float") &&
                !sharedCtx.isExtensionAvailable("WGL_NV_float_buffer")) {
              throw new GLException("Floating-point pbuffers not supported by this hardware");
            }

            // Prefer NVidia extension over ATI
            if (sharedCtx.isExtensionAvailable("WGL_NV_float_buffer")) {
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

    static int WGLConfig2DrawableTypeBits(int[] iattribs,
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

    static boolean WGLConfigDrawableTypeVerify(int val, boolean onscreen, boolean usePBuffer) {
        boolean res;

        if ( onscreen ) {
            res = ( 0 != (val & WINDOW_BIT) ) ;
        } else {
            if ( usePBuffer ) {
                res = ( 0 != (val & PBUFFER_BIT) ) ;
            } else {
                res = ( 0 != (val & BITMAP_BIT) ) ;
            }
        }

        return res;
    }

    static GLCapabilitiesImmutable AttribList2GLCapabilities(
                                                 GLProfile glp, int[] iattribs,
                                                 int niattribs,
                                                 int[] iresults,
                                                 boolean onscreen, boolean usePBuffer) {
        GLCapabilities res = new GLCapabilities(glp);
        int drawableTypeBits = WGLConfig2DrawableTypeBits(iattribs, niattribs, iresults);
        if(WGLConfigDrawableTypeVerify(drawableTypeBits, onscreen, usePBuffer)) {
            res.setOnscreen(onscreen);
            res.setPBuffer(usePBuffer);
        } else {
            if(DEBUG) {
              System.err.println("WGL DrawableType does not match: req(onscrn "+onscreen+", pbuffer "+usePBuffer+"), got(onscreen "+( 0 != (drawableTypeBits & WINDOW_BIT) )+", pbuffer "+( 0 != (drawableTypeBits & PBUFFER_BIT) )+", pixmap "+( 0 != (drawableTypeBits & BITMAP_BIT))+")");
            }
            return null;
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

    static GLCapabilitiesImmutable PFD2GLCapabilities(GLProfile glp, PIXELFORMATDESCRIPTOR pfd, boolean onscreen, boolean usePBuffer) {
        if ((pfd.getDwFlags() & GDI.PFD_SUPPORT_OPENGL) == 0) {
          return null;
        }
        GLCapabilities res = new GLCapabilities(glp);
        res.setRedBits       (pfd.getCRedBits());
        res.setGreenBits     (pfd.getCGreenBits());
        res.setBlueBits      (pfd.getCBlueBits());
        res.setAlphaBits     (pfd.getCAlphaBits());
        res.setAccumRedBits  (pfd.getCAccumRedBits());
        res.setAccumGreenBits(pfd.getCAccumGreenBits());
        res.setAccumBlueBits (pfd.getCAccumBlueBits());
        res.setAccumAlphaBits(pfd.getCAccumAlphaBits());
        res.setDepthBits     (pfd.getCDepthBits());
        res.setStencilBits   (pfd.getCStencilBits());
        res.setDoubleBuffered((pfd.getDwFlags() & GDI.PFD_DOUBLEBUFFER) != 0);
        res.setStereo        ((pfd.getDwFlags() & GDI.PFD_STEREO) != 0);
        res.setHardwareAccelerated( (pfd.getDwFlags() & GDI.PFD_GENERIC_FORMAT) == 0 ||
                                    (pfd.getDwFlags() & GDI.PFD_GENERIC_ACCELERATED) != 0  );
        res.setOnscreen      ( onscreen && ((pfd.getDwFlags() & GDI.PFD_DRAW_TO_WINDOW) != 0) );
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

  static PIXELFORMATDESCRIPTOR GLCapabilities2PFD(GLCapabilitiesImmutable caps) {
    int colorDepth = (caps.getRedBits() +
                      caps.getGreenBits() +
                      caps.getBlueBits());
    if (colorDepth < 15) {
      throw new GLException("Bit depths < 15 (i.e., non-true-color) not supported");
    }
    PIXELFORMATDESCRIPTOR pfd = createPixelFormatDescriptor();
    int pfdFlags = (GDI.PFD_SUPPORT_OPENGL |
                    GDI.PFD_GENERIC_ACCELERATED);
    if (caps.getDoubleBuffered()) {
      pfdFlags |= GDI.PFD_DOUBLEBUFFER;
    }
    if (caps.isOnscreen()) {
      pfdFlags |= GDI.PFD_DRAW_TO_WINDOW;
    } else {
      pfdFlags |= GDI.PFD_DRAW_TO_BITMAP;
    }
    if (caps.getStereo()) {
      pfdFlags |= GDI.PFD_STEREO;
    }
    pfd.setDwFlags(pfdFlags);
    pfd.setIPixelType((byte) GDI.PFD_TYPE_RGBA);
    pfd.setCColorBits((byte) colorDepth);
    pfd.setCRedBits  ((byte) caps.getRedBits());
    pfd.setCGreenBits((byte) caps.getGreenBits());
    pfd.setCBlueBits ((byte) caps.getBlueBits());
    pfd.setCAlphaBits((byte) caps.getAlphaBits());
    int accumDepth = (caps.getAccumRedBits() +
                      caps.getAccumGreenBits() +
                      caps.getAccumBlueBits());
    pfd.setCAccumBits     ((byte) accumDepth);
    pfd.setCAccumRedBits  ((byte) caps.getAccumRedBits());
    pfd.setCAccumGreenBits((byte) caps.getAccumGreenBits());
    pfd.setCAccumBlueBits ((byte) caps.getAccumBlueBits());
    pfd.setCAccumAlphaBits((byte) caps.getAccumAlphaBits());
    pfd.setCDepthBits((byte) caps.getDepthBits());
    pfd.setCStencilBits((byte) caps.getStencilBits());
    pfd.setILayerType((byte) GDI.PFD_MAIN_PLANE);

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

  static PIXELFORMATDESCRIPTOR createPixelFormatDescriptor() {
    PIXELFORMATDESCRIPTOR pfd = PIXELFORMATDESCRIPTOR.create();
    pfd.setNSize((short) pfd.size());
    pfd.setNVersion((short) 1);
    return pfd;
  }

  public String toString() {
    return "WindowsWGLGraphicsConfiguration["+getScreen()+", pfdID " + pixelfmtID + ", wglChoosen "+choosenByWGLPixelFormat+
                                            ",\n\trequested " + getRequestedCapabilities() +
                                            ",\n\tchosen    " + getChosenCapabilities() +
                                            "]";
  }
}

