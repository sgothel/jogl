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

package com.sun.opengl.impl.windows;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public class WindowsPbufferGLDrawable extends WindowsGLDrawable {
  private int  initWidth;
  private int  initHeight;

  private WGLExt cachedWGLExt; // cached WGLExt instance from parent GLCanvas,
                               // needed to destroy pbuffer
  private long buffer; // pbuffer handle
  private int  width;
  private int  height;

  private int floatMode;

  public WindowsPbufferGLDrawable(GLCapabilities capabilities,
                                  int initialWidth,
                                  int initialHeight,
                                  WindowsGLDrawable dummyDrawable,
                                  WGLExt wglExt) {
    super(capabilities, null);
    this.initWidth  = initialWidth;
    this.initHeight = initialHeight;
    if (initWidth <= 0 || initHeight <= 0) {
      throw new GLException("Initial width and height of pbuffer must be positive (were (" +
			    initWidth + ", " + initHeight + "))");
    }

    if (DEBUG) {
      System.out.println("Pbuffer caps on init: " + capabilities +
                         (capabilities.getPbufferRenderToTexture() ? " [rtt]" : "") +
                         (capabilities.getPbufferRenderToTextureRectangle() ? " [rect]" : "") +
                         (capabilities.getPbufferFloatingPointBuffers() ? " [float]" : ""));
    }

    createPbuffer(dummyDrawable.getHDC(), wglExt);
  }

  public GLContext createContext(GLContext shareWith) {
    return new WindowsPbufferGLContext(this, shareWith);
  }

  public void destroy() {
    if (hdc != 0) {
      // Must release DC and pbuffer
      // NOTE that since the context is not current, glGetError() can
      // not be called here, so we skip the use of any composable
      // pipelines (see WindowsOnscreenGLContext.makeCurrentImpl)
      WGLExt wglExt = cachedWGLExt;
      if (wglExt.wglReleasePbufferDCARB(buffer, hdc) == 0) {
        throw new GLException("Error releasing pbuffer device context: error code " + WGL.GetLastError());
      }
      hdc = 0;
      if (!wglExt.wglDestroyPbufferARB(buffer)) {
        throw new GLException("Error destroying pbuffer: error code " + WGL.GetLastError());
      }
      buffer = 0;
      setChosenGLCapabilities(null);
    }
  }

  public void setSize(int width, int height) {
    // FIXME
    throw new GLException("Not yet implemented");
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public GLCapabilities getCapabilities() {
    return capabilities;
  }

  public long getPbuffer() {
    return buffer;
  }

  public int getFloatingPointMode() {
    return floatMode;
  }

  public void swapBuffers() throws GLException {
    // FIXME: this doesn't make sense any more because we don't have
    // access to our OpenGL context here
    /*
    // FIXME: do we need to do anything if the pbuffer is double-buffered?
    // For now, just grab the pixels for the render-to-texture support.
    if (rtt && !hasRTT) {
      if (DEBUG) {
        System.err.println("Copying pbuffer data to GL_TEXTURE_2D state");
      }

      GL gl = getGL();
      gl.glCopyTexSubImage2D(textureTarget, 0, 0, 0, 0, 0, width, height);
    }
    */
  }

  private void createPbuffer(long parentHdc, WGLExt wglExt) {
    int[]   iattributes = new int  [2*MAX_ATTRIBS];
    float[] fattributes = new float[1];
    int[]   floatModeTmp = new int[1];
    int     niattribs   = 0;

    if (DEBUG) {
      System.out.println("Pbuffer parentHdc = " + toHexString(parentHdc));
      System.out.println("Pbuffer caps: " + capabilities +
                         (capabilities.getPbufferRenderToTexture() ? " [rtt]" : "") +
                         (capabilities.getPbufferRenderToTextureRectangle() ? " [rect]" : "") +
                         (capabilities.getPbufferFloatingPointBuffers() ? " [float]" : ""));
    }

    if (!glCapabilities2iattributes(capabilities,
                                    iattributes,
                                    wglExt,
                                    true,
                                    floatModeTmp)) {
      throw new GLException("Pbuffer-related extensions not supported");
    }

    floatMode = floatModeTmp[0];
    boolean rtt      = capabilities.getPbufferRenderToTexture();
    boolean rect     = capabilities.getPbufferRenderToTextureRectangle();
    boolean useFloat = capabilities.getPbufferFloatingPointBuffers();
    boolean ati      = false;

    if (useFloat) {
      ati = (floatMode == GLPbuffer.ATI_FLOAT);
    }

    int[] pformats = new int[MAX_PFORMATS];
    int   nformats;
    int[] nformatsTmp = new int[1];
    if (!wglExt.wglChoosePixelFormatARB(parentHdc,
                                        iattributes, 0,
                                        fattributes, 0,
                                        MAX_PFORMATS,
                                        pformats, 0,
                                        nformatsTmp, 0)) {
      throw new GLException("pbuffer creation error: wglChoosePixelFormatARB() failed");
    }
    nformats = nformatsTmp[0];
    if (nformats <= 0) {
      throw new GLException("pbuffer creation error: Couldn't find a suitable pixel format");
    }

    boolean haveMultisample = wglExt.isExtensionAvailable("WGL_ARB_multisample");

    if (DEBUG) {
      System.err.println("" + nformats + " suitable pixel formats found");
      // query pixel format
      iattributes[0] = WGLExt.WGL_RED_BITS_ARB;
      iattributes[1] = WGLExt.WGL_GREEN_BITS_ARB;
      iattributes[2] = WGLExt.WGL_BLUE_BITS_ARB;
      iattributes[3] = WGLExt.WGL_ALPHA_BITS_ARB;
      iattributes[4] = WGLExt.WGL_DEPTH_BITS_ARB;
      iattributes[5] = (useFloat ? (ati ? WGLExt.WGL_PIXEL_TYPE_ARB : WGLExt.WGL_FLOAT_COMPONENTS_NV) : WGLExt.WGL_RED_BITS_ARB);
      iattributes[6] = (haveMultisample ? WGLExt.WGL_SAMPLE_BUFFERS_ARB : WGLExt.WGL_RED_BITS_ARB);
      iattributes[7] = (haveMultisample ? WGLExt.WGL_SAMPLES_ARB : WGLExt.WGL_RED_BITS_ARB);
      iattributes[8] = WGLExt.WGL_DRAW_TO_PBUFFER_ARB;
      int[] ivalues = new int[9];
      for (int i = 0; i < nformats; i++) {
        if (!wglExt.wglGetPixelFormatAttribivARB(parentHdc, pformats[i], 0, 9, iattributes, 0, ivalues, 0)) {
          throw new GLException("Error while querying pixel format " + pformats[i] +
                                "'s (index " + i + "'s) capabilities for debugging");
        }
        System.err.print("pixel format " + pformats[i] + " (index " + i + "): ");
        System.err.print( "r: " + ivalues[0]);
        System.err.print(" g: " + ivalues[1]);
        System.err.print(" b: " + ivalues[2]);
        System.err.print(" a: " + ivalues[3]);
        System.err.print(" depth: " + ivalues[4]);
        if (haveMultisample) {
          System.err.print(" multisample: " + ivalues[6]);
        }
        System.err.print(" samples: " + ivalues[7]);
        if (useFloat) {
          if (ati) {
            if (ivalues[5] == WGLExt.WGL_TYPE_RGBA_FLOAT_ATI) {
              System.err.print(" [ati float]");
            } else if (ivalues[5] != WGLExt.WGL_TYPE_RGBA_ARB) {
              System.err.print(" [unknown pixel type " + ivalues[5] + "]");
            }
          } else {
            if (ivalues[5] != 0) {
              System.err.print(" [float]");
            }
          }
        }

        if (ivalues[8] != 0) {
          System.err.print(" [pbuffer]");
        }
        System.err.println();
      }
    }

    long tmpBuffer = 0;
    int whichFormat = -1;
    // Loop is a workaround for bugs in NVidia's recent drivers
    for (whichFormat = 0; whichFormat < nformats; whichFormat++) {
      int format = pformats[whichFormat];

      // Create the p-buffer.
      niattribs = 0;

      if (rtt) {
        iattributes[niattribs++]   = WGLExt.WGL_TEXTURE_FORMAT_ARB;
        if (useFloat) {
          iattributes[niattribs++] = WGLExt.WGL_TEXTURE_FLOAT_RGB_NV;
        } else {
          iattributes[niattribs++] = WGLExt.WGL_TEXTURE_RGBA_ARB;
        }

        iattributes[niattribs++] = WGLExt.WGL_TEXTURE_TARGET_ARB;
        iattributes[niattribs++] = rect ? WGLExt.WGL_TEXTURE_RECTANGLE_NV : WGLExt.WGL_TEXTURE_2D_ARB;

        iattributes[niattribs++] = WGLExt.WGL_MIPMAP_TEXTURE_ARB;
        iattributes[niattribs++] = GL.GL_FALSE;

        iattributes[niattribs++] = WGLExt.WGL_PBUFFER_LARGEST_ARB;
        iattributes[niattribs++] = GL.GL_FALSE;
      }

      iattributes[niattribs++] = 0;

      tmpBuffer = wglExt.wglCreatePbufferARB(parentHdc, format, initWidth, initHeight, iattributes, 0);
      if (tmpBuffer != 0) {
        // Done
        break;
      }
    }

    if (tmpBuffer == 0) {
      throw new GLException("pbuffer creation error: wglCreatePbufferARB() failed: tried " + nformats +
                            " pixel formats, last error was: " + wglGetLastError());
    }

    // Get the device context.
    long tmpHdc = wglExt.wglGetPbufferDCARB(tmpBuffer);
    if (tmpHdc == 0) {
      throw new GLException("pbuffer creation error: wglGetPbufferDCARB() failed");
    }

    // Set up instance variables
    buffer = tmpBuffer;
    hdc    = tmpHdc;
    cachedWGLExt = wglExt;

    // Re-query chosen pixel format
    {
      niattribs = 0;
      iattributes[niattribs++] = WGLExt.WGL_ACCELERATION_ARB;
      iattributes[niattribs++] = WGLExt.WGL_RED_BITS_ARB;
      iattributes[niattribs++] = WGLExt.WGL_GREEN_BITS_ARB;
      iattributes[niattribs++] = WGLExt.WGL_BLUE_BITS_ARB;
      iattributes[niattribs++] = WGLExt.WGL_ALPHA_BITS_ARB;
      iattributes[niattribs++] = WGLExt.WGL_DEPTH_BITS_ARB;
      iattributes[niattribs++] = WGLExt.WGL_STENCIL_BITS_ARB;
      iattributes[niattribs++] = WGLExt.WGL_DOUBLE_BUFFER_ARB;
      iattributes[niattribs++] = WGLExt.WGL_STEREO_ARB;
      iattributes[niattribs++] = WGLExt.WGL_ACCUM_RED_BITS_ARB;
      iattributes[niattribs++] = WGLExt.WGL_ACCUM_GREEN_BITS_ARB;
      iattributes[niattribs++] = WGLExt.WGL_ACCUM_BLUE_BITS_ARB;
      iattributes[niattribs++] = WGLExt.WGL_ACCUM_ALPHA_BITS_ARB;
      iattributes[niattribs++] = (useFloat ? (ati ? WGLExt.WGL_PIXEL_TYPE_ARB : WGLExt.WGL_FLOAT_COMPONENTS_NV) : WGLExt.WGL_RED_BITS_ARB);
      iattributes[niattribs++] = (haveMultisample ? WGLExt.WGL_SAMPLE_BUFFERS_ARB : WGLExt.WGL_RED_BITS_ARB);
      iattributes[niattribs++] = (haveMultisample ? WGLExt.WGL_SAMPLES_ARB : WGLExt.WGL_RED_BITS_ARB);
      iattributes[niattribs++] = WGLExt.WGL_DRAW_TO_PBUFFER_ARB;
      int[] ivalues = new int[niattribs];
      // FIXME: usually prefer to throw exceptions, but failure here is not critical
      if (wglExt.wglGetPixelFormatAttribivARB(parentHdc, pformats[whichFormat], 0, niattribs, iattributes, 0, ivalues, 0)) {
        setChosenGLCapabilities(iattributes2GLCapabilities(iattributes, niattribs, ivalues, false));
      }
    }

    // Determine the actual width and height we were able to create.
    int[] tmp = new int[1];
    wglExt.wglQueryPbufferARB( buffer, WGLExt.WGL_PBUFFER_WIDTH_ARB,  tmp, 0 );
    width = tmp[0];
    wglExt.wglQueryPbufferARB( buffer, WGLExt.WGL_PBUFFER_HEIGHT_ARB, tmp, 0 );
    height = tmp[0];

    if (DEBUG) {
      System.err.println("Created pbuffer " + width + " x " + height);
    }
  }

  private static String wglGetLastError() {
    return WindowsGLDrawableFactory.wglGetLastError();
  }
}
