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

package com.sun.opengl.util.texture;

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
 * <p><a name="nonpow2"><b>Non-power-of-two restrictions</b></a>
 * <br> When creating an OpenGL texture object, the Texture class will
 * attempt to leverage the <a
 * href="http://www.opengl.org/registry/specs/ARB/texture_non_power_of_two.txt">GL_ARB_texture_non_power_of_two</a>
 * and <a
 * href="http://www.opengl.org/registry/specs/ARB/texture_rectangle.txt">GL_ARB_texture_rectangle</a>
 * extensions (in that order) whenever possible.  If neither extension
 * is available, the Texture class will simply upload a non-pow2-sized
 * image into a standard pow2-sized texture (without any special
 * scaling).  Since the choice of extension (or whether one is used at
 * all) depends on the user's machine configuration, developers are
 * recommended to use {@link #getImageTexCoords} and {@link
 * #getSubImageTexCoords}, as those methods will calculate the
 * appropriate texture coordinates for the situation.
 *
 * <p>One caveat in this approach is that certain texture wrap modes
 * (e.g.  <code>GL_REPEAT</code>) are not legal when the GL_ARB_texture_rectangle
 * extension is in use.  Another issue to be aware of is that in the
 * default pow2 scenario, if the original image does not have pow2
 * dimensions, then wrapping may not work as one might expect since
 * the image does not extend to the edges of the pow2 texture.  If
 * texture wrapping is important, it is recommended to use only
 * pow2-sized images with the Texture class.
 *
 * <p><a name="perftips"><b>Performance Tips</b></a>
 * <br> For best performance, try to avoid calling {@link #enable} /
 * {@link #bind} / {@link #disable} any more than necessary. For
 * example, applications using many Texture objects in the same scene
 * may want to reduce the number of calls to both {@link #enable} and
 * {@link #disable}. To do this it is necessary to call {@link
 * #getTarget} to make sure the OpenGL texture target is the same for
 * all of the Texture objects in use; non-power-of-two textures using
 * the GL_ARB_texture_rectangle extension use a different target than
 * power-of-two textures using the GL_TEXTURE_2D target. Note that
 * when switching between textures it is necessary to call {@link
 * #bind}, but when drawing many triangles all using the same texture,
 * for best performance only one call to {@link #bind} should be made.
 *
 * <p><a name="premult"><b>Alpha premultiplication and blending</b></a>
 * <br> The mathematically correct way to perform blending in OpenGL
 * (with the SrcOver "source over destination" mode, or any other
 * Porter-Duff rule) is to use "premultiplied color components", which
 * means the R/G/ B color components have already been multiplied by
 * the alpha value.  To make things easier for developers, the Texture
 * class will automatically convert non-premultiplied image data into
 * premultiplied data when storing it into an OpenGL texture.  As a
 * result, it is important to use the correct blending function; for
 * example, the SrcOver rule is expressed as:
<pre>
    gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
</pre>
 * Also, when using a texture function like <code>GL_MODULATE</code> where
 * the current color plays a role, it is important to remember to make
 * sure that the color is specified in a premultiplied form, for
 * example:
<pre>
    float a = ...;
    float r = r * a;
    float g = g * a;
    float b = b * a;
    gl.glColor4f(r, g, b, a);
</pre> 
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
  /** The original aspect ratio of the image, before any rescaling
      that might have occurred due to using the GLU mipmap routines. */
  private float aspectRatio;
  /** Indicates whether the TextureData requires a vertical flip of
      the texture coords. */
  private boolean mustFlipVertically;
  /** Indicates whether we're using automatic mipmap generation
      support (GL_GENERATE_MIPMAP). */
  private boolean usingAutoMipmapGeneration;

  /** The texture coordinates corresponding to the entire image. */
  private TextureCoords coords;

  /** An estimate of the amount of texture memory this texture consumes. */
  private int estimatedMemorySize;

  private static final boolean DEBUG = Debug.debug("Texture");
  private static final boolean VERBOSE = Debug.verbose();

  // For testing alternate code paths on more capable hardware
  private static final boolean disableNPOT    = Debug.isPropertyDefined("jogl.texture.nonpot");
  private static final boolean disableTexRect = Debug.isPropertyDefined("jogl.texture.notexrect");

  // For now make Texture constructor package-private to limit the
  // number of public APIs we commit to
  Texture(TextureData data) throws GLException {
    GL gl = GLU.getCurrentGL();
    texID = createTextureID(gl); 

    updateImage(data);
  }

  // Constructor for use when creating e.g. cube maps, where there is
  // no initial texture data
  Texture(int target) throws GLException {
    GL gl = GLU.getCurrentGL();
    texID = createTextureID(gl); 
    this.target = target;
  }

  /**
   * Enables this texture's target (e.g., GL_TEXTURE_2D) in the
   * current GL context's state. This method is a shorthand equivalent
   * of the following OpenGL code:
<pre>
    gl.glEnable(texture.getTarget());
</pre>
   *
   * See the <a href="#perftips">performance tips</a> above for hints
   * on how to maximize performance when using many Texture objects.
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void enable() throws GLException {
    GLU.getCurrentGL().glEnable(target);
  }

  /**
   * Disables this texture's target (e.g., GL_TEXTURE_2D) in the
   * current GL context's state. This method is a shorthand equivalent
   * of the following OpenGL code:
<pre>
    gl.glDisable(texture.getTarget());
</pre>
   *
   * See the <a href="#perftips">performance tips</a> above for hints
   * on how to maximize performance when using many Texture objects.
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void disable() throws GLException {
    GLU.getCurrentGL().glDisable(target); 
  }

  /**
   * Binds this texture to the current GL context. This method is a
   * shorthand equivalent of the following OpenGL code:
<pre>
    gl.glBindTexture(texture.getTarget(), texture.getTextureObject());
</pre>
   *
   * See the <a href="#perftips">performance tips</a> above for hints
   * on how to maximize performance when using many Texture objects.
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void bind() throws GLException {
    GLU.getCurrentGL().glBindTexture(target, texID); 
  }

  /**
   * Disposes the native resources used by this texture object.
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void dispose() throws GLException {
    GLU.getCurrentGL().glDeleteTextures(1, new int[] {texID}, 0);
    texID = 0;
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
   * Returns the width of the allocated OpenGL texture in pixels.
   * Note that the texture width will be greater than or equal to the
   * width of the image contained within.
   *
   * @return the width of the texture
   */
  public int getWidth() {
    return texWidth;
  }
    
  /**
   * Returns the height of the allocated OpenGL texture in pixels.
   * Note that the texture height will be greater than or equal to the
   * height of the image contained within.
   *
   * @return the height of the texture
   */
  public int getHeight() {
    return texHeight;
  }   
    
  /** 
   * Returns the width of the image contained within this texture.
   * Note that for non-power-of-two textures in particular this may
   * not be equal to the result of {@link #getWidth}. It is
   * recommended that applications call {@link #getImageTexCoords} and
   * {@link #getSubImageTexCoords} rather than using this API
   * directly.
   *
   * @return the width of the image
   */
  public int getImageWidth() {
    return imgWidth;
  }

  /**
   * Returns the height of the image contained within this texture.
   * Note that for non-power-of-two textures in particular this may
   * not be equal to the result of {@link #getHeight}. It is
   * recommended that applications call {@link #getImageTexCoords} and
   * {@link #getSubImageTexCoords} rather than using this API
   * directly.
   *
   * @return the height of the image
   */
  public int getImageHeight() {
    return imgHeight;
  }

  /**
   * Returns the original aspect ratio of the image, defined as (image
   * width) / (image height), before any scaling that might have
   * occurred as a result of using the GLU mipmap routines.
   */
  public float getAspectRatio() {
    return aspectRatio;
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
        float yMax = (float) imgHeight / (float) texHeight;
        return new TextureCoords(tx1, yMax - ty1, tx2, yMax - ty2);
      } else {
        return new TextureCoords(tx1, ty1, tx2, ty2);
      }
    }
  }

  /**
   * Updates the entire content area of this texture using the data in
   * the given image.
   * 
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void updateImage(TextureData data) throws GLException {
    updateImage(data, 0);
  }

  /**
   * Indicates whether this texture's texture coordinates must be
   * flipped vertically in order to properly display the texture. This
   * is handled automatically by {@link #getImageTexCoords
   * getImageTexCoords} and {@link #getSubImageTexCoords
   * getSubImageTexCoords}, but applications may generate or otherwise
   * produce texture coordinates which must be corrected.
   */
  public boolean getMustFlipVertically() {
    return mustFlipVertically;
  }

  /**
   * Updates the content area of the specified target of this texture
   * using the data in the given image. In general this is intended
   * for construction of cube maps.
   * 
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void updateImage(TextureData data, int target) throws GLException {
    GL gl = GLU.getCurrentGL();

    imgWidth = data.getWidth();
    imgHeight = data.getHeight();
    aspectRatio = (float) imgWidth / (float) imgHeight;
    mustFlipVertically = data.getMustFlipVertically();

    int texTarget = 0;
    int texParamTarget = this.target;

    // See whether we have automatic mipmap generation support
    boolean haveAutoMipmapGeneration =
        (gl.isExtensionAvailable("GL_VERSION_1_4") ||
         gl.isExtensionAvailable("GL_SGIS_generate_mipmap"));

    // Indicate to the TextureData what functionality is available
    data.setHaveEXTABGR(gl.isExtensionAvailable("GL_EXT_abgr"));
    data.setHaveGL12(gl.isExtensionAvailable("GL_VERSION_1_2"));

    // Note that automatic mipmap generation doesn't work for
    // GL_ARB_texture_rectangle
    if ((!isPowerOfTwo(imgWidth) || !isPowerOfTwo(imgHeight)) &&
        !haveNPOT(gl)) {
      haveAutoMipmapGeneration = false;
    }

    if (data.getMipmap() && !haveAutoMipmapGeneration) {
      // GLU always scales the texture's dimensions to be powers of
      // two. It also doesn't really matter exactly what the texture
      // width and height are because the texture coords are always
      // between 0.0 and 1.0.
      imgWidth = nextPowerOfTwo(imgWidth);
      imgHeight = nextPowerOfTwo(imgHeight);
      texWidth = imgWidth;
      texHeight = imgHeight;
      texTarget = GL.GL_TEXTURE_2D;
    } else if ((isPowerOfTwo(imgWidth) && isPowerOfTwo(imgHeight)) ||
               haveNPOT(gl)) {
      if (DEBUG) {
        if (isPowerOfTwo(imgWidth) && isPowerOfTwo(imgHeight)) {
          System.err.println("Power-of-two texture");
        } else {
          System.err.println("Using GL_ARB_texture_non_power_of_two");
        }
      }

      texWidth = imgWidth;
      texHeight = imgHeight;
      texTarget = GL.GL_TEXTURE_2D;
    } else if (haveTexRect(gl)) {
      if (DEBUG) {
        System.err.println("Using GL_ARB_texture_rectangle");
      }

      texWidth = imgWidth;
      texHeight = imgHeight;
      texTarget = GL.GL_TEXTURE_RECTANGLE_ARB;
    } else {
      if (DEBUG) {
        System.err.println("Expanding texture to power-of-two dimensions");
      }

      if (data.getBorder() != 0) {
        throw new RuntimeException("Scaling up a non-power-of-two texture which has a border won't work");
      }
      texWidth = nextPowerOfTwo(imgWidth);
      texHeight = nextPowerOfTwo(imgHeight);
      texTarget = GL.GL_TEXTURE_2D;
    }

    texParamTarget = texTarget;
    setImageSize(imgWidth, imgHeight, texTarget);

    if (target != 0) {
      // Allow user to override auto detection and skip bind step (for
      // cubemap construction)
      texTarget = target;
      if (this.target == 0) {
        throw new GLException("Override of target failed; no target specified yet");
      }
      texParamTarget = this.target;
      gl.glBindTexture(texParamTarget, texID);
    } else {
      gl.glBindTexture(texTarget, texID);
    }

    if (data.getMipmap() && !haveAutoMipmapGeneration) {
      int[] align = new int[1];
      gl.glGetIntegerv(GL.GL_UNPACK_ALIGNMENT, align, 0); // save alignment
      gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, data.getAlignment());

      if (data.isDataCompressed()) {
        throw new GLException("May not request mipmap generation for compressed textures");
      }

      try {
        GLU glu = new GLU();
        glu.gluBuild2DMipmaps(texTarget, data.getInternalFormat(),
                              data.getWidth(), data.getHeight(),
                              data.getPixelFormat(), data.getPixelType(), data.getBuffer());
      } finally {
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, align[0]); // restore alignment
      }
    } else {
      checkCompressedTextureExtensions(data);
      Buffer[] mipmapData = data.getMipmapData();
      if (mipmapData != null) {
        int width = texWidth;
        int height = texHeight;
        for (int i = 0; i < mipmapData.length; i++) {
          if (data.isDataCompressed()) {
            // Need to use glCompressedTexImage2D directly to allocate and fill this image
            gl.glCompressedTexImage2D(texTarget, i, data.getInternalFormat(),
                                      width, height, data.getBorder(),
                                      mipmapData[i].remaining(), mipmapData[i]);
          } else {
            // Allocate texture image at this level
            gl.glTexImage2D(texTarget, i, data.getInternalFormat(),
                            width, height, data.getBorder(),
                            data.getPixelFormat(), data.getPixelType(), null);
            updateSubImageImpl(data, texTarget, i, 0, 0, 0, 0, data.getWidth(), data.getHeight());
          }

          width /= 2;
          height /= 2;
        }
      } else {
        if (data.isDataCompressed()) {
          // Need to use glCompressedTexImage2D directly to allocate and fill this image
          gl.glCompressedTexImage2D(texTarget, 0, data.getInternalFormat(),
                                    texWidth, texHeight, data.getBorder(),
                                    data.getBuffer().capacity(), data.getBuffer());
        } else {
          if (data.getMipmap() && haveAutoMipmapGeneration) {
            // For now, only use hardware mipmapping for uncompressed 2D
            // textures where the user hasn't explicitly specified
            // mipmap data; don't know about interactions between
            // GL_GENERATE_MIPMAP and glCompressedTexImage2D
            gl.glTexParameteri(texParamTarget, GL.GL_GENERATE_MIPMAP, GL.GL_TRUE);
            usingAutoMipmapGeneration = true;
          }

          gl.glTexImage2D(texTarget, 0, data.getInternalFormat(),
                          texWidth, texHeight, data.getBorder(),
                          data.getPixelFormat(), data.getPixelType(), null);
          updateSubImageImpl(data, texTarget, 0, 0, 0, 0, 0, data.getWidth(), data.getHeight());
        }
      }
    }

    int minFilter = (data.getMipmap() ? GL.GL_LINEAR_MIPMAP_LINEAR : GL.GL_LINEAR);
    int magFilter = GL.GL_LINEAR;
    int wrapMode = (gl.isExtensionAvailable("GL_VERSION_1_2") ? GL.GL_CLAMP_TO_EDGE : GL.GL_CLAMP);

    // REMIND: figure out what to do for GL_TEXTURE_RECTANGLE_ARB
    if (texTarget != GL.GL_TEXTURE_RECTANGLE_ARB) {
      gl.glTexParameteri(texParamTarget, GL.GL_TEXTURE_MIN_FILTER, minFilter);
      gl.glTexParameteri(texParamTarget, GL.GL_TEXTURE_MAG_FILTER, magFilter);
      gl.glTexParameteri(texParamTarget, GL.GL_TEXTURE_WRAP_S, wrapMode);
      gl.glTexParameteri(texParamTarget, GL.GL_TEXTURE_WRAP_T, wrapMode);
      if (this.target == GL.GL_TEXTURE_CUBE_MAP) {
        gl.glTexParameteri(texParamTarget, GL.GL_TEXTURE_WRAP_R, wrapMode);
      }
    }

    // Don't overwrite target if we're loading e.g. faces of a cube
    // map
    if ((this.target == 0) ||
        (this.target == GL.GL_TEXTURE_2D) ||
        (this.target == GL.GL_TEXTURE_RECTANGLE_ARB)) {
      this.target = texTarget;
    }

    // This estimate will be wrong for cube maps
    estimatedMemorySize = data.getEstimatedMemorySize();
  }

  /**
   * Updates a subregion of the content area of this texture using the
   * given data. If automatic mipmap generation is in use (see {@link
   * #isUsingAutoMipmapGeneration isUsingAutoMipmapGeneration}),
   * updates to the base (level 0) mipmap will cause the lower-level
   * mipmaps to be regenerated, and updates to other mipmap levels
   * will be ignored. Otherwise, if automatic mipmap generation is not
   * in use, only updates the specified mipmap level and does not
   * re-generate mipmaps if they were originally produced or loaded.
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
    if (usingAutoMipmapGeneration && mipmapLevel != 0) {
      // When we're using mipmap generation via GL_GENERATE_MIPMAP, we
      // don't need to update other mipmap levels
      return;
    }
    bind();
    updateSubImageImpl(data, target, mipmapLevel, x, y, 0, 0, data.getWidth(), data.getHeight());
  }

  /**
   * Updates a subregion of the content area of this texture using the
   * specified sub-region of the given data.  If automatic mipmap
   * generation is in use (see {@link #isUsingAutoMipmapGeneration
   * isUsingAutoMipmapGeneration}), updates to the base (level 0)
   * mipmap will cause the lower-level mipmaps to be regenerated, and
   * updates to other mipmap levels will be ignored. Otherwise, if
   * automatic mipmap generation is not in use, only updates the
   * specified mipmap level and does not re-generate mipmaps if they
   * were originally produced or loaded. This method is only supported
   * for uncompressed TextureData sources.
   *
   * @param data the image data to be uploaded to this texture
   * @param mipmapLevel the mipmap level of the texture to set. If
   * this is non-zero and the TextureData contains mipmap data, the
   * appropriate mipmap level will be selected.
   * @param dstx the x offset (in pixels) relative to the lower-left corner
   * of this texture where the update will be applied
   * @param dsty the y offset (in pixels) relative to the lower-left corner
   * of this texture where the update will be applied
   * @param srcx the x offset (in pixels) relative to the lower-left corner
   * of the supplied TextureData from which to fetch the update rectangle
   * @param srcy the y offset (in pixels) relative to the lower-left corner
   * of the supplied TextureData from which to fetch the update rectangle
   * @param width the width (in pixels) of the rectangle to be updated
   * @param height the height (in pixels) of the rectangle to be updated
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void updateSubImage(TextureData data, int mipmapLevel,
                             int dstx, int dsty,
                             int srcx, int srcy,
                             int width, int height) throws GLException {
    if (data.isDataCompressed()) {
      throw new GLException("updateSubImage specifying a sub-rectangle is not supported for compressed TextureData");
    }
    if (usingAutoMipmapGeneration && mipmapLevel != 0) {
      // When we're using mipmap generation via GL_GENERATE_MIPMAP, we
      // don't need to update other mipmap levels
      return;
    }
    bind();
    updateSubImageImpl(data, target, mipmapLevel, dstx, dsty, srcx, srcy, width, height);
  }

  /**
   * Sets the OpenGL floating-point texture parameter for the
   * texture's target. This gives control over parameters such as
   * GL_TEXTURE_MAX_ANISOTROPY_EXT. Causes this texture to be bound to
   * the current texture state.
   * 
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void setTexParameterf(int parameterName,
                               float value) {
    bind();
    GL gl = GLU.getCurrentGL();
    gl.glTexParameterf(target, parameterName, value);
  }

  /**
   * Sets the OpenGL multi-floating-point texture parameter for the
   * texture's target. Causes this texture to be bound to the current
   * texture state.
   * 
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void setTexParameterfv(int parameterName,
                                FloatBuffer params) {
    bind();
    GL gl = GLU.getCurrentGL();
    gl.glTexParameterfv(target, parameterName, params);
  }

  /**
   * Sets the OpenGL multi-floating-point texture parameter for the
   * texture's target. Causes this texture to be bound to the current
   * texture state.
   * 
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void setTexParameterfv(int parameterName,
                                float[] params, int params_offset) {
    bind();
    GL gl = GLU.getCurrentGL();
    gl.glTexParameterfv(target, parameterName, params, params_offset);
  }

  /**
   * Sets the OpenGL integer texture parameter for the texture's
   * target. This gives control over parameters such as
   * GL_TEXTURE_WRAP_S and GL_TEXTURE_WRAP_T, which by default are set
   * to GL_CLAMP_TO_EDGE if OpenGL 1.2 is supported on the current
   * platform and GL_CLAMP if not. Causes this texture to be bound to
   * the current texture state.
   * 
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void setTexParameteri(int parameterName,
                               int value) {
    bind();
    GL gl = GLU.getCurrentGL();
    gl.glTexParameteri(target, parameterName, value);
  }

  /**
   * Sets the OpenGL multi-integer texture parameter for the texture's
   * target. Causes this texture to be bound to the current texture
   * state.
   * 
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void setTexParameteriv(int parameterName,
                                IntBuffer params) {
    bind();
    GL gl = GLU.getCurrentGL();
    gl.glTexParameteriv(target, parameterName, params);
  }

  /**
   * Sets the OpenGL multi-integer texture parameter for the texture's
   * target. Causes this texture to be bound to the current texture
   * state.
   * 
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void setTexParameteriv(int parameterName,
                                int[] params, int params_offset) {
    bind();
    GL gl = GLU.getCurrentGL();
    gl.glTexParameteriv(target, parameterName, params, params_offset);
  }

  /**
   * Returns the underlying OpenGL texture object for this texture.
   * Most applications will not need to access this, since it is
   * handled automatically by the bind() and dispose() APIs.
   */
  public int getTextureObject() {
    return texID;
  }

  /** Returns an estimate of the amount of texture memory in bytes
      this Texture consumes. It should only be treated as an estimate;
      most applications should not need to query this but instead let
      the OpenGL implementation page textures in and out as
      necessary. */
  public int getEstimatedMemorySize() {
    return estimatedMemorySize;
  }

  /** Indicates whether this Texture is using automatic mipmap
      generation (via the OpenGL texture parameter
      GL_GENERATE_MIPMAP). This will automatically be used when
      mipmapping is requested via the TextureData and either OpenGL
      1.4 or the GL_SGIS_generate_mipmap extension is available. If
      so, updates to the base image (mipmap level 0) will
      automatically propagate down to the lower mipmap levels. Manual
      updates of the mipmap data at these lower levels will be
      ignored. */
  public boolean isUsingAutoMipmapGeneration() {
    return usingAutoMipmapGeneration;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

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
  private void setImageSize(int width, int height, int target) {
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

  private void updateSubImageImpl(TextureData data, int newTarget, int mipmapLevel,
                                  int dstx, int dsty,
                                  int srcx, int srcy, int width, int height) throws GLException {
    GL gl = GLU.getCurrentGL();
    data.setHaveEXTABGR(gl.isExtensionAvailable("GL_EXT_abgr"));
    data.setHaveGL12(gl.isExtensionAvailable("GL_VERSION_1_2"));

    Buffer buffer = data.getBuffer();
    if (buffer == null && data.getMipmapData() == null) {
      // Assume user just wanted to get the Texture object allocated
      return;
    }

    int rowlen = data.getRowLength();
    int dataWidth = data.getWidth();
    int dataHeight = data.getHeight();
    if (data.getMipmapData() != null) {
      // Compute the width, height and row length at the specified mipmap level
      // Note we do not support specification of the row length for
      // mipmapped textures at this point
      for (int i = 0; i < mipmapLevel; i++) {
        width /= 2;
        height /= 2;

        dataWidth /= 2;
        dataHeight /= 2;
      }
      rowlen = 0;
      buffer = data.getMipmapData()[mipmapLevel];
    }

    // Clip incoming rectangles to what is available both on this
    // texture and in the incoming TextureData
    if (srcx < 0) {
      width += srcx;
      srcx = 0;
    }
    if (srcy < 0) {
      height += srcy;
      srcy = 0;
    }
    // NOTE: not sure whether the following two are the correct thing to do
    if (dstx < 0) {
      width += dstx;
      dstx = 0;
    }
    if (dsty < 0) {
      height += dsty;
      dsty = 0;
    }

    if (srcx + width > dataWidth) {
      width = dataWidth - srcx;
    }
    if (srcy + height > dataHeight) {
      height = dataHeight - srcy;
    }
    if (dstx + width > texWidth) {
      width = texWidth - dstx;
    }
    if (dsty + height > texHeight) {
      height = texHeight - dsty;
    }

    checkCompressedTextureExtensions(data);

    if (data.isDataCompressed()) {
      gl.glCompressedTexSubImage2D(newTarget, mipmapLevel,
                                   dstx, dsty, width, height,
                                   data.getInternalFormat(),
                                   buffer.remaining(), buffer);
    } else {
      int[] align = new int[1];
      int[] rowLength = new int[1];
      int[] skipRows = new int[1];
      int[] skipPixels = new int[1];
      gl.glGetIntegerv(GL.GL_UNPACK_ALIGNMENT,   align,      0); // save alignment
      gl.glGetIntegerv(GL.GL_UNPACK_ROW_LENGTH,  rowLength,  0); // save row length
      gl.glGetIntegerv(GL.GL_UNPACK_SKIP_ROWS,   skipRows,   0); // save skipped rows
      gl.glGetIntegerv(GL.GL_UNPACK_SKIP_PIXELS, skipPixels, 0); // save skipped pixels
      gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, data.getAlignment());
      if (DEBUG && VERBOSE) {
        System.out.println("Row length  = " + rowlen);
        System.out.println("skip pixels = " + srcx);
        System.out.println("skip rows   = " + srcy);
        System.out.println("dstx        = " + dstx);
        System.out.println("dsty        = " + dsty);
        System.out.println("width       = " + width);
        System.out.println("height      = " + height);
      }
      gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, rowlen);
      gl.glPixelStorei(GL.GL_UNPACK_SKIP_ROWS, srcy);
      gl.glPixelStorei(GL.GL_UNPACK_SKIP_PIXELS, srcx);

      gl.glTexSubImage2D(newTarget, mipmapLevel,
                         dstx, dsty, width, height,
                         data.getPixelFormat(), data.getPixelType(),
                         buffer);
      gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT,   align[0]);      // restore alignment
      gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH,  rowLength[0]);  // restore row length
      gl.glPixelStorei(GL.GL_UNPACK_SKIP_ROWS,   skipRows[0]);   // restore skipped rows
      gl.glPixelStorei(GL.GL_UNPACK_SKIP_PIXELS, skipPixels[0]); // restore skipped pixels
    }
  }

  private void checkCompressedTextureExtensions(TextureData data) {
    GL gl = GLU.getCurrentGL();
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

  // Helper routines for disabling certain codepaths
  private static boolean haveNPOT(GL gl) {
    return (!disableNPOT &&
            gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two"));
  }

  private static boolean haveTexRect(GL gl) {
    return (!disableTexRect &&
            TextureIO.isTexRectEnabled() &&
            gl.isExtensionAvailable("GL_ARB_texture_rectangle"));
  }
}
