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

package com.sun.opengl.impl.macosx.cgl;

import javax.media.opengl.*;
import javax.media.nativewindow.*;
import com.sun.opengl.impl.*;

public class MacOSXPbufferCGLDrawable extends MacOSXCGLDrawable {
  private static final boolean DEBUG = Debug.debug("MacOSXPbufferCGLDrawable");
  
  // State for render-to-texture and render-to-texture-rectangle support
  private int textureTarget; // e.g. GL_TEXTURE_2D, GL_TEXTURE_RECTANGLE_NV
  private int texture;       // actual texture object

  // NSOpenGLPbuffer (for normal mode)
  // CGLPbufferObj (for CGL_MODE situation, i.e., when Java2D/JOGL bridge is active)
  // Note that we can not store this in the NativeWindow because the
  // semantic is that contains an NSView
  protected long pBuffer;

  public MacOSXPbufferCGLDrawable(GLDrawableFactory factory, NativeWindow target) {
    super(factory, target, true);

    if (DEBUG) {
        System.out.println("Pbuffer config: " + getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration());
    }

    initOpenGLImpl();
    createPbuffer();

    if (DEBUG) {
        System.err.println("Created pbuffer " + this);
    }
  }

  protected void setRealizedImpl() {
    if(realized) {
        createPbuffer();
    } else {
        destroy();
    }
  }

  public GLContext createContext(GLContext shareWith) {
    return new MacOSXPbufferCGLContext(this, shareWith);
  }

  public void destroy() {
    if (this.pBuffer != 0) {
      NativeWindow nw = getNativeWindow();
      impl.destroy(pBuffer);
      this.pBuffer = 0;
      ((SurfaceChangeable)nw).setSurfaceHandle(0);
      if (DEBUG) {
        System.err.println("Destroyed pbuffer: " + pBuffer);
      }
    }
  }

  public long getPbuffer() {
    return pBuffer;
  }
  
  protected void swapBuffersImpl() {
    if(DEBUG) {
        System.err.println("unhandled swapBuffersImpl() called for: "+this);
    }
  }

  private void createPbuffer() {
    NativeWindow nw = getNativeWindow();
    DefaultGraphicsConfiguration config = (DefaultGraphicsConfiguration) nw.getGraphicsConfiguration().getNativeGraphicsConfiguration();
    GLCapabilities capabilities = (GLCapabilities)config.getChosenCapabilities();
    GLProfile glProfile = capabilities.getGLProfile();
    int renderTarget;
    if (glProfile.isGL2() && capabilities.getPbufferRenderToTextureRectangle()) {
      renderTarget = GL2.GL_TEXTURE_RECTANGLE;
    } else {
      int w = getNextPowerOf2(getWidth());
      int h = getNextPowerOf2(getHeight());
      ((SurfaceChangeable)nw).setSize(w, h);
      renderTarget = GL.GL_TEXTURE_2D;
    }

    int internalFormat = GL.GL_RGBA;
    if (capabilities.getPbufferFloatingPointBuffers()) {
      // FIXME: want to check availability of GL_APPLE_float_pixels
      // extension, but need valid OpenGL context in order to do so --
      // in worst case would need to create dummy window / GLCanvas
      // (undesirable) -- could maybe also do this with pbuffers
      /*
        if (!gl.isExtensionAvailable("GL_APPLE_float_pixels")) {
        throw new GLException("Floating-point support (GL_APPLE_float_pixels) not available");
        }
      */
      if(glProfile.isGL2()) {
        switch (capabilities.getRedBits()) {
        case 16: internalFormat = GL2.GL_RGBA_FLOAT16_APPLE; break;
        case 32: internalFormat = GL2.GL_RGBA_FLOAT32_APPLE; break;
        default: throw new GLException("Invalid floating-point bit depth (only 16 and 32 supported)");
        }
      } else {
        internalFormat = GL.GL_RGBA;
      }
    }
            
    pBuffer = impl.create(renderTarget, internalFormat, getWidth(), getHeight());
    if (pBuffer == 0) {
      throw new GLException("pbuffer creation error: CGL.createPBuffer() failed");
    }

    ((SurfaceChangeable)nw).setSurfaceHandle(pBuffer);

  }

  private int getNextPowerOf2(int number) {
    if (((number-1) & number) == 0) {
      //ex: 8 -> 0b1000; 8-1=7 -> 0b0111; 0b1000&0b0111 == 0
      return number;
    }
    int power = 0;
    while (number > 0) {
      number = number>>1;
      power++;
    }
    return (1<<power);
  }

  //---------------------------------------------------------------------------
  // OpenGL "mode switching" functionality
  //
  private boolean haveSetOpenGLMode = false;
  // FIXME: should consider switching the default mode based on
  // whether the Java2D/JOGL bridge is active -- need to ask ourselves
  // whether it's more likely that we will share with a GLCanvas or a
  // GLJPanel when the bridge is turned on
  private int     openGLMode = NSOPENGL_MODE;
  // Implementation object (either NSOpenGL-based or CGL-based)
  protected Impl impl;

  public void setOpenGLMode(int mode) {
    if (mode == openGLMode) {
      return;
    }
    if (haveSetOpenGLMode) {
      throw new GLException("Can't switch between using NSOpenGLPixelBuffer and CGLPBufferObj more than once");
    }
    destroy();
    openGLMode = mode;
    haveSetOpenGLMode = true;
    if (DEBUG) {
      System.err.println("Switching PBuffer drawable mode to " +
                         ((mode == MacOSXCGLDrawable.NSOPENGL_MODE) ? "NSOPENGL_MODE" : "CGL_MODE"));
    }
    initOpenGLImpl();
    createPbuffer();
  }

  public int getOpenGLMode() {
    return openGLMode;
  }

  private void initOpenGLImpl() {
    switch (openGLMode) {
      case NSOPENGL_MODE:
        impl = new NSOpenGLImpl();
        break;
      case CGL_MODE:
        impl = new CGLImpl();
        break;
      default:
        throw new InternalError("Illegal implementation mode " + openGLMode);
    }
  }

  // Abstract interface for implementation of this drawable (either
  // NSOpenGL-based or CGL-based)
  interface Impl {
    public long create(int renderTarget, int internalFormat, int width, int height);
    public void destroy(long pbuffer);
  }

  // NSOpenGLPixelBuffer implementation
  class NSOpenGLImpl implements Impl {
    public long create(int renderTarget, int internalFormat, int width, int height) {
      return CGL.createPBuffer(renderTarget, internalFormat, width, height);
    }

    public void destroy(long pbuffer) {
      CGL.destroyPBuffer(pbuffer);
    }
  }

  // CGL implementation
  class CGLImpl implements Impl {
    public long create(int renderTarget, int internalFormat, int width, int height) {
      long[] pbuffer = new long[1];
      int res = CGL.CGLCreatePBuffer(width, height, renderTarget, internalFormat, 0, pbuffer, 0);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error creating CGL-based pbuffer: error code " + res);
      }
      return pbuffer[0];
    }

    public void destroy(long pbuffer) {
      int res = CGL.CGLDestroyPBuffer(pbuffer);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error destroying CGL-based pbuffer: error code " + res);
      }
    }
  }
}
