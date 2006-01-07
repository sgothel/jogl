/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.sun.opengl.utils;

import java.nio.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.impl.*;

/**
 * Represents an OpenGL texture object. Contains convenience routines
 * for enabling/disabling OpenGL texture state, binding this texture,
 * and computing texture coordinates for both the entire image as well
 * as a sub-image. 
 * 
 * <br> REMIND: document GL_TEXTURE_2D/GL_TEXTURE_RECTANGLE_ARB issues...
 * <br> REMIND: translucent images will have premultiplied comps by default...
 *
 * @author Chris Campbell
 * @author Kenneth Russell
 */
public class Texture {
  /** The GL target type. */
  private int target;
  /** The GL texture ID. */
  private int texID;
  /** The width of the texture. */
  private int texWidth;
  /** The height of the texture. */
  private int texHeight;
  /** The width of the image. */
  private int imgWidth;
  /** The height of the image. */
  private int imgHeight;
  /** Indicates whether the TextureData requires a vertical flip of
      the texture coords. */
  private boolean mustFlipVertically;

  /** The texture coordinates corresponding to the entire image. */
  private TextureCoords coords;

  private static final boolean DEBUG = Debug.debug("Texture");

  // For now make Texture constructor package-private to limit the
  // number of public APIs we commit to
  Texture(TextureData data) throws GLException {
    GL gl = getCurrentGL();

    imgWidth = data.getWidth();
    imgHeight = data.getHeight();
    mustFlipVertically = data.getMustFlipVertically();

    if (data.getMipmap()) {
      // GLU always targets GL_TEXTURE_2D and scales the texture to be
      // a power of two. It also doesn't really matter exactly what
      // the texture width and height are because the texture coords
      // are always between 0.0 and 1.0.
      imgWidth = nextPowerOfTwo(imgWidth);
      imgHeight = nextPowerOfTwo(imgHeight);
      texWidth = imgWidth;
      texHeight = imgHeight;
      target = GL.GL_TEXTURE_2D;
    } else if ((isPowerOfTwo(imgWidth) && isPowerOfTwo(imgHeight)) ||
               gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two")) {
      if (DEBUG) {
        if (isPowerOfTwo(imgWidth) && isPowerOfTwo(imgHeight)) {
          System.err.println("Power-of-two texture");
        } else {
          System.err.println("Using GL_ARB_texture_non_power_of_two");
        }
      }

      texWidth = imgWidth;
      texHeight = imgHeight;
      target = GL.GL_TEXTURE_2D;
    } else if (gl.isExtensionAvailable("GL_ARB_texture_rectangle")) {
      if (DEBUG) {
        System.err.println("Using GL_ARB_texture_rectangle");
      }

      texWidth = imgWidth;
      texHeight = imgHeight;
      target = GL.GL_TEXTURE_RECTANGLE_ARB;
    } else {
      if (DEBUG) {
        System.err.println("Expanding texture to power-of-two dimensions");
      }

      if (data.getBorder() != 0) {
        throw new RuntimeException("Scaling up a non-power-of-two texture which has a border won't work");
      }
      texWidth = nextPowerOfTwo(imgWidth);
      texHeight = nextPowerOfTwo(imgHeight);
      target = GL.GL_TEXTURE_2D;
    }

    // REMIND: let the user specify these, optionally
    int minFilter = GL.GL_LINEAR;
    int magFilter = GL.GL_LINEAR;
    int wrapMode = GL.GL_CLAMP_TO_EDGE;

    texID = createTextureID(gl); 
    setImageSize(imgWidth, imgHeight);
    gl.glBindTexture(target, texID);
    // REMIND: figure out what to do for GL_TEXTURE_RECTANGLE_ARB
    if (target == GL.GL_TEXTURE_2D) {
      gl.glTexParameteri(target, GL.GL_TEXTURE_MIN_FILTER, minFilter);
      gl.glTexParameteri(target, GL.GL_TEXTURE_MAG_FILTER, magFilter);
      gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_S, wrapMode);
      gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_T, wrapMode);
    }

    if (data.getMipmap()) {
      GLU glu = new GLU();
      glu.gluBuild2DMipmaps(target, data.getInternalFormat(),
                            data.getWidth(), data.getHeight(),
                            data.getPixelFormat(), data.getPixelType(), data.getBuffer());
    } else {
      gl.glTexImage2D(target, 0, data.getInternalFormat(),
                      texWidth, texHeight, data.getBorder(),
                      data.getPixelFormat(), data.getPixelType(), null);
      Buffer[] mipmapData = data.getMipmapData();
      if (mipmapData != null) {
        for (int i = 0; i < mipmapData.length; i++) {
          updateSubImage(data, i, 0, 0);
        }
      } else {
        updateSubImage(data, 0, 0, 0);
      }
    }
  }

  /**
   * Enables this texture's target (e.g., GL_TEXTURE_2D) in the
   * current GL context's state.
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void enable() throws GLException {
    getCurrentGL().glEnable(target); 
  }

  /**
   * Disables this texture's target (e.g., GL_TEXTURE_2D) in the
   * current GL context's state.
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void disable() throws GLException {
    getCurrentGL().glDisable(target); 
  }

  /**
   * Binds this texture to the current GL context.
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void bind() throws GLException {
    getCurrentGL().glBindTexture(target, texID); 
  }

  /**
   * Disposes the native resources used by this texture object.
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void dispose() throws GLException {
    getCurrentGL().glDeleteTextures(1, new int[] {texID}, 0);
  }

  /**
   * Returns the OpenGL "target" of this texture.
   *
   * @return the OpenGL target of this texture
   * @see javax.media.opengl.GL#GL_TEXTURE_2D
   * @see javax.media.opengl.GL#GL_TEXTURE_RECTANGLE_ARB
   */
  public int getTarget() {
    return target;
  }

  /**
   * Returns the width of the texture.  Note that the texture width will
   * be greater than or equal to the width of the image contained within.
   *
   * @return the width of the texture
   */
  public int getWidth() {
    return texWidth;
  }
    
  /**
   * Returns the height of the texture.  Note that the texture height will
   * be greater than or equal to the height of the image contained within.
   *
   * @return the height of the texture
   */
  public int getHeight() {
    return texHeight;
  }   
    
  /** 
   * Returns the width of the image contained within this texture.
   *
   * @return the width of the image
   */
  public int getImageWidth() {
    return imgWidth;
  }

  /**
   * Returns the height of the image contained within this texture.
   *
   * @return the height of the image
   */
  public int getImageHeight() {
    return imgHeight;
  }

  /**
   * Returns the set of texture coordinates corresponding to the
   * entire image. If the TextureData indicated that the texture
   * coordinates must be flipped vertically, the returned
   * TextureCoords will take that into account.
   * 
   * @return the texture coordinates corresponding to the entire image
   */
  public TextureCoords getImageTexCoords() {
    return coords;
  }

  /**
   * Returns the set of texture coordinates corresponding to the
   * specified sub-image. The (x1, y1) and (x2, y2) points are
   * specified in terms of pixels starting from the lower-left of the
   * image. (x1, y1) should specify the lower-left corner of the
   * sub-image and (x2, y2) the upper-right corner of the sub-image.
   * If the TextureData indicated that the texture coordinates must be
   * flipped vertically, the returned TextureCoords will take that
   * into account; this should not be handled by the end user in the
   * specification of the y1 and y2 coordinates.
   * 
   * @return the texture coordinates corresponding to the specified sub-image
   */
  public TextureCoords getSubImageTexCoords(int x1, int y1, int x2, int y2) {
    if (target == GL.GL_TEXTURE_RECTANGLE_ARB) {
      if (mustFlipVertically) {
        return new TextureCoords(x1, texHeight - y1, x2, texHeight - y2);
      } else {
        return new TextureCoords(x1, y1, x2, y2);
      }
    } else {
      float tx1 = (float)x1 / (float)texWidth;
      float ty1 = (float)y1 / (float)texHeight;
      float tx2 = (float)x2 / (float)texWidth;
      float ty2 = (float)y2 / (float)texHeight;
      if (mustFlipVertically) {
        return new TextureCoords(tx1, 1.0f - ty1, tx2, 1.0f - ty2);
      } else {
        return new TextureCoords(tx1, ty1, tx2, ty2);
      }
    }
  }


  /**
   * Updates a subregion of the content area of this texture using the
   * data in the given image. Only updates the specified mipmap level
   * and does not re-generate mipmaps if they were originally produced
   * or loaded.
   *
   * @param data the image data to be uploaded to this texture
   * @param mipmapLevel the mipmap level of the texture to set. If
   * this is non-zero and the TextureData contains mipmap data, the
   * appropriate mipmap level will be selected.
   * @param x the x offset (in pixels) relative to the lower-left corner
   * of this texture
   * @param y the y offset (in pixels) relative to the lower-left corner
   * of this texture
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void updateSubImage(TextureData data, int mipmapLevel, int x, int y) throws GLException {
    GL gl = getCurrentGL();
    bind();
    int width = data.getWidth();
    int height = data.getHeight();
    Buffer buffer = data.getBuffer();
    if (data.getMipmapData() != null) {
      // Compute the width and height at the specified mipmap level
      for (int i = 0; i < mipmapLevel; i++) {
        width /= 2;
        height /= 2;
      }
      buffer = data.getMipmapData()[mipmapLevel];
    }

    if (data.isDataCompressed()) {
      switch (data.getInternalFormat()) {
        case GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT:
        case GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
        case GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
        case GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
          if (!gl.isExtensionAvailable("GL_EXT_texture_compression_s3tc") &&
              !gl.isExtensionAvailable("GL_NV_texture_compression_vtc")) {
            throw new GLException("DXTn compressed textures not supported by this graphics card");
          }
          break;
        default:
          // FIXME: should test availability of more texture
          // compression extensions here
          break;
      }

      gl.glCompressedTexSubImage2D(target, mipmapLevel,
                                   x, y, width, height,
                                   data.getInternalFormat(),
                                   buffer.remaining(), buffer);
    } else {
      int[] align = new int[1];
      gl.glGetIntegerv(GL.GL_UNPACK_ALIGNMENT, align, 0); // save alignment
      gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, data.getAlignment());

      gl.glTexSubImage2D(target, mipmapLevel,
                         x, y, width, height,
                         data.getPixelFormat(), data.getPixelType(),
                         buffer);
      gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, align[0]); // restore align
    }
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  /**
   * Returns the current GL object. Throws GLException if no OpenGL
   * context was current.
   */
  private static GL getCurrentGL() throws GLException {
    GLContext context = GLContext.getCurrent();
    if (context == null) {
      throw new GLException("No OpenGL context current on current thread");
    }
    return context.getGL();
  }

  /**
   * Returns true if the given value is a power of two.
   *
   * @return true if the given value is a power of two, false otherwise
   */
  private static boolean isPowerOfTwo(int val) {
    return ((val & (val - 1)) == 0);
  }

  /**
   * Returns the nearest power of two that is larger than the given value.
   * If the given value is already a power of two, this method will simply
   * return that value.
   *
   * @param val the value
   * @return the next power of two
   */
  private static int nextPowerOfTwo(int val) {
    int ret = 1;
    while (ret < val) {
      ret <<= 1;
    }
    return ret;
  }

  /**
   * Updates the actual image dimensions; usually only called from
   * <code>updateImage</code>.
   */
  private void setImageSize(int width, int height) {
    imgWidth = width;
    imgHeight = height;
    if (target == GL.GL_TEXTURE_RECTANGLE_ARB) {
      if (mustFlipVertically) {
        coords = new TextureCoords(0, imgHeight, imgWidth, 0);
      } else {
        coords = new TextureCoords(0, 0, imgWidth, imgHeight);
      }
    } else {
      if (mustFlipVertically) {
        coords = new TextureCoords(0, (float) imgHeight / (float) texHeight,
                                   (float) imgWidth / (float) texWidth, 0);
      } else {
        coords = new TextureCoords(0, 0,
                                   (float) imgWidth / (float) texWidth,
                                   (float) imgHeight / (float) texHeight);
      }
    }
  }

  /**
   * Creates a new texture ID.
   *
   * @param gl the GL object associated with the current OpenGL context
   * @return a new texture ID
   */
  private static int createTextureID(GL gl) {
    int[] tmp = new int[1];
    gl.glGenTextures(1, tmp, 0);
    return tmp[0];
  }
}
