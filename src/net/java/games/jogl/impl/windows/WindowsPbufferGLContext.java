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

import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class WindowsPbufferGLContext extends WindowsGLContext {
  private static final boolean DEBUG = false;

  private int  initWidth;
  private int  initHeight;

  private long buffer; // pbuffer handle
  private int  width;
  private int  height;

  // FIXME: kept around because we create the OpenGL context lazily to
  // better integrate with the WindowsGLContext framework
  private long parentHglrc;

  private static final int MAX_PFORMATS = 256;
  private static final int MAX_ATTRIBS  = 256;

  // State for render-to-texture and render-to-texture-rectangle support
  private boolean created;
  private boolean rtt;       // render-to-texture?
  private boolean rect;      // render-to-texture-rectangle?
  private int textureTarget; // e.g. GL_TEXTURE_2D, GL_TEXTURE_RECTANGLE_NV
  private int texture;       // actual texture object

  public WindowsPbufferGLContext(GLCapabilities capabilities, int initialWidth, int initialHeight) {
    super(null, capabilities, null);
    this.initWidth  = initialWidth;
    this.initHeight = initialHeight;
    if (initWidth <= 0 || initHeight <= 0) {
      throw new GLException("Initial width and height of pbuffer must be positive (were (" +
			    initWidth + ", " + initHeight + "))");
    }
  }

  public boolean canCreatePbufferContext() {
    return false;
  }

  public GLContext createPbufferContext(GLCapabilities capabilities,
                                        int initialWidth,
                                        int initialHeight) {
    throw new GLException("Not supported");
  }

  public void bindPbufferToTexture() {
    if (!rtt) {
      throw new GLException("Shouldn't try to bind a pbuffer to a texture if render-to-texture hasn't been " +
                            "specified in its GLCapabilities");
    }
    GL gl = getGL();
    gl.glBindTexture(textureTarget, texture);
    // Note: this test was on the rtt variable in NVidia's code but I
    // think it doesn't make sense written that way
    if (rect) {
      if (!gl.wglBindTexImageARB(buffer, GL.WGL_FRONT_LEFT_ARB)) {
        throw new GLException("Binding of pbuffer to texture failed: " + wglGetLastError());
      }
    }
    // Note that if the render-to-texture-rectangle extension is not
    // specified, we perform a glCopyTexImage2D in swapBuffers().
  }

  public void releasePbufferFromTexture() {
    if (!rtt) {
      throw new GLException("Shouldn't try to bind a pbuffer to a texture if render-to-texture hasn't been " +
                            "specified in its GLCapabilities");
    }
    if (rect) {
      GL gl = getGL();
      if (!gl.wglReleaseTexImageARB(buffer, GL.WGL_FRONT_LEFT_ARB)) {
        throw new GLException("Releasing of pbuffer from texture failed: " + wglGetLastError());
      }
    }
  }

  public void createPbuffer(long parentHdc, long parentHglrc) {
    GL gl = getGL();
    
    int[]   iattributes = new int  [2*MAX_ATTRIBS];
    float[] fattributes = new float[2*MAX_ATTRIBS];
    int nfattribs = 0;
    int niattribs = 0;

    rtt              = capabilities.getOffscreenRenderToTexture();
    rect             = capabilities.getOffscreenRenderToTextureRectangle();
    boolean useFloat = capabilities.getOffscreenFloatingPointBuffers();
    
    // Since we are trying to create a pbuffer, the pixel format we
    // request (and subsequently use) must be "p-buffer capable".
    iattributes[niattribs++] = GL.WGL_DRAW_TO_PBUFFER_ARB;
    iattributes[niattribs++] = GL.GL_TRUE;

    if (!rtt) {
      // Currently we don't support non-truecolor visuals in the
      // GLCapabilities, so we don't offer the option of making
      // color-index pbuffers.
      iattributes[niattribs++] = GL.WGL_PIXEL_TYPE_ARB;
      iattributes[niattribs++] = GL.WGL_TYPE_RGBA_ARB;
    }

    iattributes[niattribs++] = GL.WGL_DOUBLE_BUFFER_ARB;
    if (capabilities.getDoubleBuffered()) {
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
    if (capabilities.getStencilBits() > 0) {
      iattributes[niattribs++] = GL.GL_TRUE;
    } else {
      iattributes[niattribs++] = GL.GL_FALSE;
    }

    if (capabilities.getAccumRedBits()   > 0 ||
        capabilities.getAccumGreenBits() > 0 ||
        capabilities.getAccumBlueBits()  > 0) {
      iattributes[niattribs++] = GL.WGL_ACCUM_BITS_ARB;
      iattributes[niattribs++] = GL.GL_TRUE;
    }

    if (useFloat) {
      iattributes[niattribs++] = GL.WGL_FLOAT_COMPONENTS_NV;
      iattributes[niattribs++] = GL.GL_TRUE;
    }

    if (rtt) {
      if (useFloat) {
        iattributes[niattribs++] = GL.WGL_BIND_TO_TEXTURE_RECTANGLE_FLOAT_RGB_NV;
        iattributes[niattribs++] = GL.GL_TRUE;
      } else {
        iattributes[niattribs++] = rect ? GL.WGL_BIND_TO_TEXTURE_RECTANGLE_RGB_NV : GL.WGL_BIND_TO_TEXTURE_RGB_ARB;
        iattributes[niattribs++] = GL.GL_TRUE;
      }
    }

    iattributes[niattribs++] = GL.WGL_SUPPORT_OPENGL_ARB;
    iattributes[niattribs++] = GL.GL_TRUE;
    
    int[] pformats = new int[MAX_PFORMATS];
    int   nformats;
    int[] nformatsTmp = new int[1];
    if (!gl.wglChoosePixelFormatARB(parentHdc,
                                    iattributes,
                                    fattributes,
                                    MAX_PFORMATS,
                                    pformats, 
                                    nformatsTmp)) {
      throw new GLException("pbuffer creation error: wglChoosePixelFormatARB() failed");
    }
    nformats = nformatsTmp[0];
    if (nformats <= 0) {
      throw new GLException("pbuffer creation error: Couldn't find a suitable pixel format");
    }

    if (DEBUG) {
      System.err.println("" + nformats + " suitable pixel formats found");
      // query pixel format
      iattributes[0] = GL.WGL_RED_BITS_ARB;
      iattributes[1] = GL.WGL_GREEN_BITS_ARB;
      iattributes[2] = GL.WGL_BLUE_BITS_ARB;
      iattributes[3] = GL.WGL_ALPHA_BITS_ARB;
      iattributes[4] = GL.WGL_DEPTH_BITS_ARB;
      iattributes[5] = GL.WGL_FLOAT_COMPONENTS_NV;
      iattributes[6] = GL.WGL_SAMPLE_BUFFERS_EXT;
      iattributes[7] = GL.WGL_SAMPLES_EXT;
      int[] ivalues = new int[8];
      for (int i = 0; i < nformats; i++) {
        if (!gl.wglGetPixelFormatAttribivARB(parentHdc, pformats[i], 0, 8, iattributes, ivalues)) {
          throw new GLException("Error while querying pixel format " + pformats[i] +
                                "'s (index " + i + "'s) capabilities for debugging");
        }
        System.err.print("pixel format " + pformats[i] + " (index " + i + "): ");
        System.err.print( "r: " + ivalues[0]);
        System.err.print(" g: " + ivalues[1]);
        System.err.print(" b: " + ivalues[2]);
        System.err.print(" a: " + ivalues[3]);
        System.err.print(" depth: " + ivalues[4]);
        System.err.print(" multisample: " + ivalues[6]);
        System.err.print(" samples: " + ivalues[7]);
        if (ivalues[5] != 0) {
          System.err.print(" [float]");
        }
        System.err.println();
      }
    }

    int format = pformats[0];

    // Create the p-buffer.
    niattribs = 0;

    if (rtt) {
      iattributes[niattribs++]   = GL.WGL_TEXTURE_FORMAT_ARB;
      if (useFloat) {
        iattributes[niattribs++] = GL.WGL_TEXTURE_FLOAT_RGB_NV;
      } else {
        iattributes[niattribs++] = GL.WGL_TEXTURE_RGBA_ARB;
      }

      iattributes[niattribs++] = GL.WGL_TEXTURE_TARGET_ARB;
      iattributes[niattribs++] = rect ? GL.WGL_TEXTURE_RECTANGLE_NV : GL.WGL_TEXTURE_2D_ARB;

      iattributes[niattribs++] = GL.WGL_MIPMAP_TEXTURE_ARB;
      iattributes[niattribs++] = GL.GL_FALSE;

      iattributes[niattribs++] = GL.WGL_PBUFFER_LARGEST_ARB;
      iattributes[niattribs++] = GL.GL_FALSE;
    }

    iattributes[niattribs++] = 0;

    long tmpBuffer = gl.wglCreatePbufferARB(parentHdc, format, initWidth, initHeight, iattributes);
    if (tmpBuffer == 0) {
      throw new GLException("pbuffer creation error: wglCreatePbufferARB() failed: " + wglGetLastError());
    }

    // Get the device context.
    long tmpHdc = gl.wglGetPbufferDCARB(tmpBuffer);
    if (tmpHdc == 0) {
      throw new GLException("pbuffer creation error: wglGetPbufferDCARB() failed");
    }

    this.parentHglrc = parentHglrc;

    // Set up instance variables
    buffer = tmpBuffer;
    hdc    = tmpHdc;

    // Determine the actual width and height we were able to create.
    int[] tmp = new int[1];
    gl.wglQueryPbufferARB( buffer, GL.WGL_PBUFFER_WIDTH_ARB,  tmp );
    width = tmp[0];
    gl.wglQueryPbufferARB( buffer, GL.WGL_PBUFFER_HEIGHT_ARB, tmp );
    height = tmp[0];

    if (DEBUG) {
      System.err.println("Created pbuffer " + width + " x " + height);
    }
  }

  protected synchronized boolean makeCurrent(Runnable initAction) throws GLException {
    created = false;

    if (buffer == 0) {
      // pbuffer not instantiated yet
      return false;
    }

    boolean res = super.makeCurrent(initAction);
    if (created) {
      // Initialize render-to-texture support if requested
      rtt  = capabilities.getOffscreenRenderToTexture();
      rect = capabilities.getOffscreenRenderToTextureRectangle();

      if (rtt) {
        if (DEBUG) {
          System.err.println("Initializing render-to-texture support");
        }

        GL gl = getGL();
        if (rect && !gl.isExtensionAvailable("GL_NV_texture_rectangle")) {
          System.err.println("WindowsPbufferGLContext: WARNING: GL_NV_texture_rectangle extension not " +
                             "supported; skipping requested render_to_texture_rectangle support for pbuffer");
          rect = false;
        }
        if (rect) {
          if (DEBUG) {
            System.err.println("  Using render-to-texture-rectangle");
          }
          textureTarget = GL.GL_TEXTURE_RECTANGLE_NV;
        } else {
          if (DEBUG) {
            System.err.println("  Using vanilla render-to-texture");
          }
          textureTarget = GL.GL_TEXTURE_2D;
        }
        int[] tmp = new int[1];
        gl.glGenTextures(1, tmp);
        texture = tmp[0];
        gl.glBindTexture(textureTarget, texture);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        gl.glCopyTexImage2D(textureTarget, 0, GL.GL_RGB, 0, 0, width, height, 0);
      }
    }
    return res;
  }

  public void handleModeSwitch(long parentHdc, long parentHglrc) {
    throw new GLException("Not yet implemented");
  }

  protected boolean isOffscreen() {
    // FIXME: currently the only caller of this won't cause proper
    // resizing of the pbuffer anyway.
    return false;
  }

  public int getOffscreenContextBufferedImageType() {
    throw new GLException("Should not call this");
  }

  public int getOffscreenContextReadBuffer() {
    throw new GLException("Should not call this");
  }

  public boolean offscreenImageNeedsVerticalFlip() {
    throw new GLException("Should not call this");
  }

  protected void create() {
    created = true;
    // Create a gl context for the p-buffer.
    hglrc = WGL.wglCreateContext(hdc);
    if (hglrc == 0) {
      throw new GLException("pbuffer creation error: wglCreateContext() failed");
    }

    // FIXME: provide option to not share display lists with subordinate pbuffer?
    if (!WGL.wglShareLists(parentHglrc, hglrc)) {
      throw new GLException("pbuffer: wglShareLists() failed");
    }
  }

  protected void swapBuffers() throws GLException {
    // FIXME: do we need to do anything if the pbuffer is double-buffered?
    // For now, just grab the pixels for the render-to-texture support.
    if (rtt && !rect) {
      if (DEBUG) {
        System.err.println("Copying pbuffer data to GL_TEXTURE_2D state");
      }

      GL gl = getGL();
      gl.glCopyTexImage2D(textureTarget, 0, GL.GL_RGB, 0, 0, width, height, 0);
    }
  }

  private String wglGetLastError() {
    int err = WGL.GetLastError();
    String detail = null;
    switch (err) {
      case WGL.ERROR_INVALID_PIXEL_FORMAT: detail = "ERROR_INVALID_PIXEL_FORMAT";       break;
      case WGL.ERROR_NO_SYSTEM_RESOURCES:  detail = "ERROR_NO_SYSTEM_RESOURCES";        break;
      case WGL.ERROR_INVALID_DATA:         detail = "ERROR_INVALID_DATA";               break;
      case WGL.ERROR_PROC_NOT_FOUND:       detail = "ERROR_PROC_NOT_FOUND";             break;
      case WGL.ERROR_INVALID_WINDOW_HANDLE:detail = "ERROR_INVALID_WINDOW_HANDLE";      break;
      default:                             detail = "(Unknown error code " + err + ")"; break;
    }
    return detail;
  }
}
