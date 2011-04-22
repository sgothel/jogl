/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
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
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.jogamp.opengl.util.awt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.gl2.*;
import com.jogamp.opengl.util.texture.*;
import com.jogamp.opengl.util.texture.awt.*;

/** Provides the ability to render into an OpenGL {@link
    com.jogamp.opengl.util.texture.Texture Texture} using the Java 2D
    APIs. This renderer class uses an internal Java 2D image (of
    unspecified type) for its backing store and flushes portions of
    that image to an OpenGL texture on demand. The resulting OpenGL
    texture can then be mapped on to a polygon for display. */

public class TextureRenderer {
  // For now, we supply only a BufferedImage back-end for this
  // renderer. In theory we could use the Java 2D/JOGL bridge to fully
  // accelerate the rendering paths, but there are restrictions on
  // what work can be done where; for example, Graphics2D-related work
  // must not be done on the Queue Flusher Thread, but JOGL's
  // OpenGL-related work must be. This implies that the user's code
  // would need to be split up into multiple callbacks run from the
  // appropriate threads, which would be somewhat unfortunate.

  // Whether we have an alpha channel in the (RGB/A) backing store
  private boolean alpha;

  // Whether we're using only a GL_INTENSITY backing store
  private boolean intensity;

  // Whether we're attempting to use automatic mipmap generation support
  private boolean mipmap;

  // Whether smoothing is enabled for the OpenGL texture (switching
  // between GL_LINEAR and GL_NEAREST filtering)
  private boolean smoothing = true;
  private boolean smoothingChanged;

  // The backing store itself
  private BufferedImage image;

  private Texture texture;
  private AWTTextureData textureData;
  private boolean mustReallocateTexture;
  private Rectangle dirtyRegion;

  private GLUgl2 glu = new GLUgl2();

  // Current color
  private float r = 1.0f;
  private float g = 1.0f;
  private float b = 1.0f;
  private float a = 1.0f;

  /** Creates a new renderer with backing store of the specified width
      and height. If <CODE>alpha</CODE> is true, allocates an alpha
      channel in the backing store image. No mipmap support is
      requested.

      @param width the width of the texture to render into
      @param height the height of the texture to render into
      @param alpha whether to allocate an alpha channel for the texture
  */
  public TextureRenderer(int width, int height, boolean alpha) {
    this(width, height, alpha, false);
  }

  /** Creates a new renderer with backing store of the specified width
      and height. If <CODE>alpha</CODE> is true, allocates an alpha channel in the
      backing store image. If <CODE>mipmap</CODE> is true, attempts to use OpenGL's
      automatic mipmap generation for better smoothing when rendering
      the TextureRenderer's contents at a distance.

      @param width the width of the texture to render into
      @param height the height of the texture to render into
      @param alpha whether to allocate an alpha channel for the texture
      @param mipmap whether to attempt use of automatic mipmap generation
  */
  public TextureRenderer(int width, int height, boolean alpha, boolean mipmap) {
    this(width, height, alpha, false, mipmap);
  }

  // Internal constructor to avoid confusion since alpha only makes
  // sense when intensity is not set
  private TextureRenderer(int width, int height, boolean alpha, boolean intensity, boolean mipmap) {
    this.alpha = alpha;
    this.intensity = intensity;
    this.mipmap = mipmap;
    init(width, height);
  }

  /** Creates a new renderer with a special kind of backing store
      which acts only as an alpha channel. No mipmap support is
      requested. Internally, this associates a GL_INTENSITY OpenGL
      texture with the backing store. */
  public static TextureRenderer createAlphaOnlyRenderer(int width, int height) {
    return createAlphaOnlyRenderer(width, height, false);
  }

  /** Creates a new renderer with a special kind of backing store
      which acts only as an alpha channel. If <CODE>mipmap</CODE> is
      true, attempts to use OpenGL's automatic mipmap generation for
      better smoothing when rendering the TextureRenderer's contents
      at a distance. Internally, this associates a GL_INTENSITY OpenGL
      texture with the backing store. */
  public static TextureRenderer createAlphaOnlyRenderer(int width, int height, boolean mipmap) {
    return new TextureRenderer(width, height, false, true, mipmap);
  }

  /** Returns the width of the backing store of this renderer.

      @return the width of the backing store of this renderer
  */
  public int getWidth() {
    return image.getWidth();
  }

  /** Returns the height of the backing store of this renderer.

      @return the height of the backing store of this renderer
  */
  public int getHeight() {
    return image.getHeight();
  }

  /** Returns the size of the backing store of this renderer in a
      newly-allocated {@link java.awt.Dimension Dimension} object.

      @return the size of the backing store of this renderer
  */
  public Dimension getSize() {
    return getSize(null);
  }

  /** Returns the size of the backing store of this renderer. Uses the
      {@link java.awt.Dimension Dimension} object if one is supplied,
      or allocates a new one if null is passed.

      @param d a {@link java.awt.Dimension Dimension} object in which
        to store the results, or null to allocate a new one

      @return the size of the backing store of this renderer
  */
  public Dimension getSize(Dimension d) {
    if (d == null)
      d = new Dimension();
    d.setSize(image.getWidth(), image.getHeight());
    return d;
  }

  /** Sets the size of the backing store of this renderer. This may
      cause the OpenGL texture object associated with this renderer to
      be invalidated; it is not recommended to cache this texture
      object outside this class but to instead call {@link #getTexture
      getTexture} when it is needed.

      @param width the new width of the backing store of this renderer
      @param height the new height of the backing store of this renderer
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void setSize(int width, int height) throws GLException {
    init(width, height);
  }

  /** Sets the size of the backing store of this renderer. This may
      cause the OpenGL texture object associated with this renderer to
      be invalidated.

      @param d the new size of the backing store of this renderer
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void setSize(Dimension d) throws GLException {
    setSize(d.width, d.height);
  }

  /** Sets whether smoothing is enabled for the OpenGL texture; if so,
      uses GL_LINEAR interpolation for the minification and
      magnification filters. Defaults to true. Changes to this setting
      will not take effect until the next call to {@link
      #beginOrthoRendering beginOrthoRendering}.

      @param smoothing whether smoothing is enabled for the OpenGL texture
  */
  public void setSmoothing(boolean smoothing) {
    this.smoothing = smoothing;
    smoothingChanged = true;
  }

  /** Returns whether smoothing is enabled for the OpenGL texture; see
      {@link #setSmoothing setSmoothing}. Defaults to true.

      @return whether smoothing is enabled for the OpenGL texture
  */
  public boolean getSmoothing() {
    return smoothing;
  }

  /** Creates a {@link java.awt.Graphics2D Graphics2D} instance for
      rendering to the backing store of this renderer. The returned
      object should be disposed of using the normal {@link
      java.awt.Graphics#dispose() Graphics.dispose()} method once it
      is no longer being used.

      @return a new {@link java.awt.Graphics2D Graphics2D} object for
        rendering into the backing store of this renderer
  */
  public Graphics2D createGraphics() {
    return image.createGraphics();
  }

  /** Returns the underlying Java 2D {@link java.awt.Image Image}
      being rendered into. */
  public Image getImage() {
    return image;
  }

  /** Marks the given region of the TextureRenderer as dirty. This
      region, and any previously set dirty regions, will be
      automatically synchronized with the underlying Texture during
      the next {@link #getTexture getTexture} operation, at which
      point the dirty region will be cleared. It is not necessary for
      an OpenGL context to be current when this method is called.

      @param x the x coordinate (in Java 2D coordinates -- relative to
        upper left) of the region to update
      @param y the y coordinate (in Java 2D coordinates -- relative to
        upper left) of the region to update
      @param width the width of the region to update
      @param height the height of the region to update
  */
  public void markDirty(int x, int y, int width, int height) {
    Rectangle curRegion = new Rectangle(x, y, width, height);
    if (dirtyRegion == null) {
      dirtyRegion = curRegion;
    } else {
      dirtyRegion.add(curRegion);
    }
  }

  /** Returns the underlying OpenGL Texture object associated with
      this renderer, synchronizing any dirty regions of the
      TextureRenderer with the underlying OpenGL texture.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public Texture getTexture() throws GLException {
    if (dirtyRegion != null) {
      sync(dirtyRegion.x, dirtyRegion.y, dirtyRegion.width, dirtyRegion.height);
      dirtyRegion = null;
    }

    ensureTexture();
    return texture;
  }

  /** Disposes all resources associated with this renderer. It is not
      valid to use this renderer after calling this method.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void dispose() throws GLException {
    if (texture != null) {
      texture.destroy(GLContext.getCurrentGL());
      texture = null;
    }
    if (image != null) {
      image.flush();
      image = null;
    }
  }

  /** Convenience method which assists in rendering portions of the
      OpenGL texture to the screen, if the application intends to draw
      them as a flat overlay on to the screen. Pushes OpenGL state
      bits (GL_ENABLE_BIT, GL_DEPTH_BUFFER_BIT and GL_TRANSFORM_BIT);
      disables the depth test, back-face culling, and lighting;
      enables the texture in this renderer; and sets up the viewing
      matrices for orthographic rendering where the coordinates go
      from (0, 0) at the lower left to (width, height) at the upper
      right. Equivalent to beginOrthoRendering(width, height, true).
      {@link #endOrthoRendering} must be used in conjunction with this
      method to restore all OpenGL states.

      @param width the width of the current on-screen OpenGL drawable
      @param height the height of the current on-screen OpenGL drawable

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void beginOrthoRendering(int width, int height) throws GLException {
    beginOrthoRendering(width, height, true);
  }

  /** Convenience method which assists in rendering portions of the
      OpenGL texture to the screen, if the application intends to draw
      them as a flat overlay on to the screen. Pushes OpenGL state
      bits (GL_ENABLE_BIT, GL_DEPTH_BUFFER_BIT and GL_TRANSFORM_BIT);
      disables the depth test (if the "disableDepthTest" argument is
      true), back-face culling, and lighting; enables the texture in
      this renderer; and sets up the viewing matrices for orthographic
      rendering where the coordinates go from (0, 0) at the lower left
      to (width, height) at the upper right. {@link
      #endOrthoRendering} must be used in conjunction with this method
      to restore all OpenGL states.

      @param width the width of the current on-screen OpenGL drawable
      @param height the height of the current on-screen OpenGL drawable
      @param disableDepthTest whether the depth test should be disabled

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void beginOrthoRendering(int width, int height, boolean disableDepthTest) throws GLException {
    beginRendering(true, width, height, disableDepthTest);
  }

  /** Convenience method which assists in rendering portions of the
      OpenGL texture to the screen as 2D quads in 3D space. Pushes
      OpenGL state (GL_ENABLE_BIT); disables lighting; and enables the
      texture in this renderer. Unlike {@link #beginOrthoRendering
      beginOrthoRendering}, does not modify the depth test, back-face
      culling, lighting, or the modelview or projection matrices. The
      user is responsible for setting up the view matrices for correct
      results of {@link #draw3DRect draw3DRect}. {@link
      #end3DRendering} must be used in conjunction with this method to
      restore all OpenGL states.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void begin3DRendering() throws GLException {
    beginRendering(false, 0, 0, false);
  }

  /** Changes the color of the polygons, and therefore the drawn
      images, this TextureRenderer produces. Use of this method is
      optional. The TextureRenderer uses the GL_MODULATE texture
      environment mode, which causes the portions of the rendered
      texture to be multiplied by the color of the rendered
      polygons. The polygon color can be varied to achieve effects
      like tinting of the overall output or fading in and out by
      changing the alpha of the color. <P>

      Each component ranges from 0.0f - 1.0f. The alpha component, if
      used, does not need to be premultiplied into the color channels
      as described in the documentation for {@link
      com.jogamp.opengl.util.texture.Texture Texture}, although
      premultiplied colors are used internally. The default color is
      opaque white.

      @param r the red component of the new color
      @param g the green component of the new color
      @param b the blue component of the new color
      @param a the alpha component of the new color, 0.0f = completely
        transparent, 1.0f = completely opaque
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void setColor(float r, float g, float b, float a) throws GLException {
    GL2 gl = GLContext.getCurrentGL().getGL2();
    this.r = r * a;
    this.g = g * a;
    this.b = b * a;
    this.a = a;

    gl.glColor4f(this.r, this.g, this.b, this.a);
  }  

  private float[] compArray;
  /** Changes the current color of this TextureRenderer to the
      supplied one. The default color is opaque white. See {@link
      #setColor(float,float,float,float) setColor} for more details.

      @param color the new color to use for rendering
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void setColor(Color color) throws GLException {
    // Get color's RGBA components as floats in the range [0,1].
    if (compArray == null) {
      compArray = new float[4];
    }
    color.getRGBComponents(compArray);
    setColor(compArray[0], compArray[1], compArray[2], compArray[3]);
  }

  /** Draws an orthographically projected rectangle containing all of
      the underlying texture to the specified location on the
      screen. All (x, y) coordinates are specified relative to the
      lower left corner of either the texture image or the current
      OpenGL drawable. This method is equivalent to
      <code>drawOrthoRect(screenx, screeny, 0, 0, getWidth(),
      getHeight());</code>.

      @param screenx the on-screen x coordinate at which to draw the rectangle
      @param screeny the on-screen y coordinate (relative to lower left) at
        which to draw the rectangle
      
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void drawOrthoRect(int screenx, int screeny) throws GLException {
    drawOrthoRect(screenx, screeny, 0, 0, getWidth(), getHeight());
  }

  /** Draws an orthographically projected rectangle of the underlying
      texture to the specified location on the screen. All (x, y)
      coordinates are specified relative to the lower left corner of
      either the texture image or the current OpenGL drawable.

      @param screenx the on-screen x coordinate at which to draw the rectangle
      @param screeny the on-screen y coordinate (relative to lower left) at
        which to draw the rectangle
      @param texturex the x coordinate of the pixel in the texture of
        the lower left portion of the rectangle to draw
      @param texturey the y coordinate of the pixel in the texture
        (relative to lower left) of the lower left portion of the
        rectangle to draw
      @param width the width of the rectangle to draw
      @param height the height of the rectangle to draw
      
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void drawOrthoRect(int screenx, int screeny,
                            int texturex, int texturey,
                            int width, int height) throws GLException {
    draw3DRect(screenx, screeny, 0, texturex, texturey, width, height, 1);
  }

  /** Draws a rectangle of the underlying texture to the specified 3D
      location. In the current coordinate system, the lower left
      corner of the rectangle is placed at (x, y, z), and the upper
      right corner is placed at (x + width * scaleFactor, y + height *
      scaleFactor, z). The lower left corner of the sub-rectangle of
      the texture is (texturex, texturey) and the upper right corner
      is (texturex + width, texturey + height). For back-face culling
      purposes, the rectangle is drawn with counterclockwise
      orientation of the vertices when viewed from the front.

      @param x the x coordinate at which to draw the rectangle
      @param y the y coordinate at which to draw the rectangle
      @param z the z coordinate at which to draw the rectangle
      @param texturex the x coordinate of the pixel in the texture of
        the lower left portion of the rectangle to draw
      @param texturey the y coordinate of the pixel in the texture
        (relative to lower left) of the lower left portion of the
        rectangle to draw
      @param width the width in texels of the rectangle to draw
      @param height the height in texels of the rectangle to draw
      @param scaleFactor the scale factor to apply (multiplicatively)
        to the size of the drawn rectangle
      
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void draw3DRect(float x, float y, float z,
                         int texturex, int texturey,
                         int width, int height,
                         float scaleFactor) throws GLException {
    GL2 gl = GLContext.getCurrentGL().getGL2();
    Texture texture = getTexture();
    TextureCoords coords = texture.getSubImageTexCoords(texturex, texturey,
                                                        texturex + width,
                                                        texturey + height);
    gl.glBegin(GL2.GL_QUADS);
    gl.glTexCoord2f(coords.left(), coords.bottom());
    gl.glVertex3f(x, y, z);
    gl.glTexCoord2f(coords.right(), coords.bottom());
    gl.glVertex3f(x + width * scaleFactor, y, z);
    gl.glTexCoord2f(coords.right(), coords.top());
    gl.glVertex3f(x + width * scaleFactor, y + height * scaleFactor, z);
    gl.glTexCoord2f(coords.left(), coords.top());
    gl.glVertex3f(x, y + height * scaleFactor, z);
    gl.glEnd();
  }

  /** Convenience method which assists in rendering portions of the
      OpenGL texture to the screen, if the application intends to draw
      them as a flat overlay on to the screen. Must be used if {@link
      #beginOrthoRendering} is used to set up the rendering stage for
      this overlay. 

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void endOrthoRendering() throws GLException {
    endRendering(true);
  }

  /** Convenience method which assists in rendering portions of the
      OpenGL texture to the screen as 2D quads in 3D space. Must be
      used if {@link #begin3DRendering} is used to set up the
      rendering stage for this overlay.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void end3DRendering() throws GLException {
    endRendering(false);
  }

  /** Indicates whether automatic mipmap generation is in use for this
      TextureRenderer. The result of this method may change from true
      to false if it is discovered during allocation of the
      TextureRenderer's backing store that automatic mipmap generation
      is not supported at the OpenGL level. */
  public boolean isUsingAutoMipmapGeneration() {
    return mipmap;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void beginRendering(boolean ortho, int width, int height, boolean disableDepthTestForOrtho) {
    GL2 gl = GLContext.getCurrentGL().getGL2();
    int attribBits = 
      GL2.GL_ENABLE_BIT | GL2.GL_TEXTURE_BIT | GL2.GL_COLOR_BUFFER_BIT |
      (ortho ? (GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_TRANSFORM_BIT) : 0);
    gl.glPushAttrib(attribBits);
    gl.glDisable(GL2.GL_LIGHTING);
    if (ortho) {
      if (disableDepthTestForOrtho) {
        gl.glDisable(GL2.GL_DEPTH_TEST);
      }
      gl.glDisable(GL2.GL_CULL_FACE);
      gl.glMatrixMode(GL2.GL_PROJECTION);
      gl.glPushMatrix();
      gl.glLoadIdentity();
      glu.gluOrtho2D(0, width, 0, height);
      gl.glMatrixMode(GL2.GL_MODELVIEW);
      gl.glPushMatrix();
      gl.glLoadIdentity();
      gl.glMatrixMode(GL2.GL_TEXTURE);
      gl.glPushMatrix();
      gl.glLoadIdentity();
    }
    gl.glEnable(GL2.GL_BLEND);
    gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE_MINUS_SRC_ALPHA);
    Texture texture = getTexture();
    texture.enable(gl);
    texture.bind(gl);
    gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
    // Change polygon color to last saved
    gl.glColor4f(r, g, b, a);
    if (smoothingChanged) {
      smoothingChanged = false;
      if (smoothing) {
        texture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        if (mipmap) {
          texture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
        } else {
          texture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        }
      } else {
        texture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        texture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
      }
    }
  }

  private void endRendering(boolean ortho) {
    GL2 gl = GLContext.getCurrentGL().getGL2();
    Texture texture = getTexture();
    texture.disable(gl);
    if (ortho) {
      gl.glMatrixMode(GL2.GL_PROJECTION);
      gl.glPopMatrix();
      gl.glMatrixMode(GL2.GL_MODELVIEW);
      gl.glPopMatrix();
      gl.glMatrixMode(GL2.GL_TEXTURE);
      gl.glPopMatrix();
    }
    gl.glPopAttrib();
  }

  private void init(int width, int height) {
    GL2 gl = GLContext.getCurrentGL().getGL2();
    // Discard previous BufferedImage if any
    if (image != null) {
      image.flush();
      image = null;
    }

    // Infer the internal format if not an intensity texture
    int internalFormat = (intensity ? GL2.GL_INTENSITY : 0);
    int imageType = 
      (intensity ? BufferedImage.TYPE_BYTE_GRAY :
       (alpha ?  BufferedImage.TYPE_INT_ARGB_PRE : BufferedImage.TYPE_INT_RGB));
    image = new BufferedImage(width, height, imageType);
    // Always realllocate the TextureData associated with this
    // BufferedImage; it's just a reference to the contents but we
    // need it in order to update sub-regions of the underlying
    // texture
    textureData = new AWTTextureData(gl.getGLProfile(), internalFormat, 0, mipmap, image);
    // For now, always reallocate the underlying OpenGL texture when
    // the backing store size changes
    mustReallocateTexture = true;
  }

  /** Synchronizes the specified region of the backing store down to
      the underlying OpenGL texture. If {@link #markDirty markDirty}
      is used instead to indicate the regions that are out of sync,
      this method does not need to be called.

      @param x the x coordinate (in Java 2D coordinates -- relative to
        upper left) of the region to update
      @param y the y coordinate (in Java 2D coordinates -- relative to
        upper left) of the region to update
      @param width the width of the region to update
      @param height the height of the region to update

      @throws GLException If an OpenGL context is not current when this method is called
  */
  private void sync(int x, int y, int width, int height) throws GLException {
    // Force allocation if necessary
    boolean canSkipUpdate = ensureTexture();

    if (!canSkipUpdate) {
      // Update specified region.
      // NOTE that because BufferedImage-based TextureDatas now don't
      // do anything to their contents, the coordinate systems for
      // OpenGL and Java 2D actually line up correctly for
      // updateSubImage calls, so we don't need to do any argument
      // conversion here (i.e., flipping the Y coordinate).
      texture.updateSubImage(GLContext.getCurrentGL(), textureData, 0, x, y, x, y, width, height);
    }
  }

  // Returns true if the texture was newly allocated, false if not
  private boolean ensureTexture() {
    GL gl = GLContext.getCurrentGL();
    if (mustReallocateTexture) {
      if (texture != null) {
        texture.destroy(gl);
        texture = null;
      }
      mustReallocateTexture = false;
    }

    if (texture == null) {
      texture = TextureIO.newTexture(textureData);
      if (mipmap && !texture.isUsingAutoMipmapGeneration()) {
        // Only try this once
        texture.destroy(gl);
        mipmap = false;
        textureData.setMipmap(false);
        texture = TextureIO.newTexture(textureData);
      }

      if (!smoothing) {
        // The TextureIO classes default to GL_LINEAR filtering
        texture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        texture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
      }
      return true;
    }

    return false;
  }
}
