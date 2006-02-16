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

package com.sun.opengl.impl.macosx;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public class MacOSXPbufferGLDrawable extends MacOSXGLDrawable {
  private static final boolean DEBUG = Debug.debug("MacOSXPbufferGLContext");
  
  protected int  initWidth;
  protected int  initHeight;

  protected long pBuffer;
  
  protected int  width;
  protected int  height;

  // State for render-to-texture and render-to-texture-rectangle support
  private int textureTarget; // e.g. GL_TEXTURE_2D, GL_TEXTURE_RECTANGLE_NV
  private int texture;       // actual texture object

  public MacOSXPbufferGLDrawable(GLCapabilities capabilities, int initialWidth, int initialHeight) {
    super(capabilities, null);
    this.initWidth  = initialWidth;
    this.initHeight = initialHeight;

    createPbuffer();
  }

  public GLContext createContext(GLContext shareWith) {
    return new MacOSXPbufferGLContext(this, shareWith);
  }

  public void destroy() {
    if (this.pBuffer != 0) {
      CGL.destroyPBuffer(0, pBuffer);
      this.pBuffer = 0;
    
      if (DEBUG) {
        System.err.println("Destroyed pbuffer " + width + " x " + height);
      }
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
    return pBuffer;
  }
  
  public void swapBuffers() throws GLException {
    // FIXME: do we need to do anything if the pbuffer is double-buffered?
  }

  protected void createPbuffer() {
    int renderTarget;
    if (capabilities.getPbufferRenderToTextureRectangle()) {
      width = initWidth;
      height = initHeight;
      renderTarget = GL.GL_TEXTURE_RECTANGLE_EXT;
    } else {
      width = getNextPowerOf2(initWidth);
      height = getNextPowerOf2(initHeight);
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
      switch (capabilities.getRedBits()) {
        case 16: internalFormat = GL.GL_RGBA_FLOAT16_APPLE; break;
        case 32: internalFormat = GL.GL_RGBA_FLOAT32_APPLE; break;
        default: throw new GLException("Invalid floating-point bit depth (only 16 and 32 supported)");
      }
    }
		
    pBuffer = CGL.createPBuffer(renderTarget, internalFormat, width, height);
    if (pBuffer == 0) {
      throw new GLException("pbuffer creation error: CGL.createPBuffer() failed");
    }
	
    if (DEBUG) {
      System.err.println("Created pbuffer 0x" + toHexString(pBuffer) + ", " + width + " x " + height + " for " + this);
    }
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
}
