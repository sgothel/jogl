/*
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl.util.awt;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;


/**
 * Wrapper for an OpenGL texture that can be drawn into.
 *
 * <p><i>TextureBackingStore</i> provides the ability to draw into a
 * grayscale texture using Java 2D.  To increase performance, the
 * backing store maintains a local copy as a {@link BufferedImage}.
 * Changes are applied to the image first and then pushed to the
 * texture all at once.
 *
 * <p>After creating a backing store, a client simply needs to grab its
 * {@link Graphics2D} and use its AWT or Java 2D drawing methods.  Then
 * the area that was drawn to should be noted with the {@link
 * #mark(int, int, int, int)} method.  After everything is drawn,
 * activate the texture using {@link #bind(GL, int)} and call {@link
 * #update(GL)} to actually push the dirty regions to the texture.  If
 * further changes need to made, consider using {@link #clear(int, int,
 * int, int)} to erase old data.
 *
 * <p>Note that since texturing hasn't changed much, BackingStore is
 * compatible with GL2 or GL3.  For that reason, it only requests
 * simple GL objects.
 */
final class TextureBackingStore {

    // Size in X direction
    private final int width;

    // Size in Y direction
    private final int height;

    // Local copy of texture
    private final BufferedImage image;

    // Java2D utility for drawing into image
    private final Graphics2D g2d;

    // Raw image data for pushing to texture
    private final ByteBuffer pixels;

    // True for quality texturing
    private final boolean mipmap;

    // OpenGL texture on video card
    private Texture2D texture;

    // Area in image not pushed to texture
    private Rectangle dirtyRegion;

    // True to interpolate samples
    private boolean smooth;

    // True if interpolation has changed
    private boolean smoothChanged;

    /**
     * Creates a texture backing store.
     *
     * @param width Width of backing store
     * @param height Height of backing store
     * @param font Style of text
     * @param antialias <tt>true</tt> to render smooth edges
     * @param subpixel <tt>true</tt> to use subpixel accuracy
     * @param smooth <tt>true</tt> to interpolate samples
     * @param mipmap <tt>true</tt> for quality texturing
     * @throws AssertionError if width is negative
     * @throws AssertionError if height is negative
     * @throws AssertionError if font is <tt>null</tt>
     */
    TextureBackingStore(final int width,
                        final int height,
                        final Font font,
                        final boolean antialias,
                        final boolean subpixel,
                        final boolean smooth,
                        final boolean mipmap) {

        assert (width >= 0);
        assert (height >= 0);
        assert (font != null);

        this.width = width;
        this.height = height;
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        this.g2d = createGraphics(image, font, antialias, subpixel);
        this.pixels = getPixels(image);
        this.mipmap = mipmap;
        this.texture = null;
        this.dirtyRegion = null;
        this.smooth = smooth;
        this.smoothChanged = false;
    }

    /**
     * Binds underlying OpenGL texture on a texture unit.
     *
     * @param gl Current OpenGL context
     * @param unit OpenGL enumeration for a texture unit (i.e. <tt>GL_TEXTURE0</tt>)
     * @throws AssertionError if context is <tt>null</tt>
     * @throws AssertionError if unit is less than <tt>GL_TEXTURE0</tt>
     */
    void bind(final GL gl, final int unit) {

        assert (gl != null);
        assert (unit >= GL.GL_TEXTURE0);

        ensureTexture(gl);
        texture.bind(gl, unit);
    }

    /**
     * Clears out an area in the backing store.
     *
     * @param x Position of area's left edge
     * @param y Position of area's top edge
     * @param width Width of area
     * @param height Height of area
     * @throws AssertionError if x or y is negative
     * @throws AssertionError if width or height is negative
     */
    void clear(final int x, final int y, final int width, final int height) {

        assert ((x >= 0) || (y >= 0));
        assert ((width >= 0) || (height >= 0));

        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(x, y, width, height);
        g2d.setComposite(AlphaComposite.Src);
    }

    /**
     * Releases resources used by the backing store.
     *
     * @param gl Current OpenGL context
     * @throws AssertionError if context is <tt>null</tt>
     */
    void dispose(final GL gl) {

        assert (gl != null);

        // Dispose of image
        if (image != null) {
            image.flush();
        }

        // Dispose of texture
        if (texture != null) {
            texture.dispose(gl);
        }
    }

    /**
     * Marks an area of the backing store to be updated.
     *
     * <p>The next time the backing store is updated, the area will be
     * pushed to the texture.
     *
     * @param x Position of area's left edge
     * @param y Position of area's top edge
     * @param width Width of area
     * @param height Height of area
     * @throws AssertionError if x or y is negative
     * @throws AssertionError if width or height is negative
     */
    void mark(final int x, final int y, final int width, final int height) {

        assert ((x >= 0) || (y >= 0));
        assert ((width >= 0) || (height >= 0));

        final Rectangle region = new Rectangle(x, y, width, height);
        if (dirtyRegion == null) {
            dirtyRegion = region;
        } else {
            dirtyRegion.add(region);
        }
    }

    /**
     * Uploads any recently drawn data to the texture.
     *
     * @param gl Current OpenGL context
     * @throws AssertionError if context is <tt>null</tt>
     */
    void update(final GL gl) {

        assert (gl != null);

        // Make sure texture is created
        ensureTexture(gl);

        // Check smoothing
        if (smoothChanged) {
            texture.setFiltering(gl, smooth);
            smoothChanged = false;
        }

        // Check texture
        if (dirtyRegion != null) {
            texture.update(gl, pixels, dirtyRegion);
            dirtyRegion = null;
        }
    }

    //------------------------------------------------------------------
    // Helpers
    //

    /**
     * Creates a graphics for a backing store.
     *
     * @param image Backing store's local copy of data (assumed non-null)
     * @param font Style of text (assumed non-null)
     * @param antialias <tt>true</tt> to smooth edges
     * @param subpixel <tt>true</tt> to use subpixel accuracy
     * @return Graphics2D for rendering into <tt>image</tt>
     */
    private static Graphics2D createGraphics(final BufferedImage image,
                                             final Font font,
                                             final boolean antialias,
                                             final boolean subpixel) {

        final Graphics2D g2d = image.createGraphics();

        g2d.setComposite(AlphaComposite.Src);
        g2d.setColor(Color.WHITE);
        g2d.setFont(font);
        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                antialias ?
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON :
                        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2d.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                subpixel ?
                        RenderingHints.VALUE_FRACTIONALMETRICS_ON :
                        RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        return g2d;
    }

    /**
     * Makes sure the texture has been created.
     *
     * @param gl Current OpenGL context (assumed non-null)
     */
    private void ensureTexture(final GL gl) {
        if (texture == null) {
            texture = new GrayTexture2D(gl, width, height, smooth, mipmap);
        }
    }

    /**
     * Retrieves the underlying pixels of a buffered image.
     *
     * @param image Image with underlying pixel buffer (assumed non-null)
     * @return Pixel data of the image as a byte buffer
     * @throws IllegalStateException if image is not stored as bytes
     */
    private static ByteBuffer getPixels(final BufferedImage image) {

        final DataBuffer db = image.getRaster().getDataBuffer();
        final byte[] arr;

        if (db instanceof DataBufferByte) {
            arr = ((DataBufferByte) db).getData();
        } else {
            throw new IllegalStateException("Unexpected format in image.");
        }
        return ByteBuffer.wrap(arr);
    }

    //------------------------------------------------------------------
    // Getters and setters
    //

    /**
     * Returns Java2D Graphics2D object for drawing into the store.
     */
    final Graphics2D getGraphics() {
        return g2d;
    }

    /**
     * Returns height of the underlying image and texture.
     */
    final int getHeight() {
        return height;
    }

    /**
     * Returns local copy of texture.
     */
    final BufferedImage getImage() {
        return image;
    }

    /**
     * Returns <tt>true</tt> if texture is interpolating samples.
     */
    final boolean getUseSmoothing() {
        return smooth;
    }

    /**
     * Changes whether the texture should interpolate samples.
     */
    final void setUseSmoothing(final boolean useSmoothing) {
        smoothChanged = (this.smooth != useSmoothing);
        this.smooth = useSmoothing;
    }

    /**
     * Returns width of the underlying image and texture.
     */
    final int getWidth() {
        return width;
    }

    //---------------------------------------------------------------
    // Nested classes
    //

    /**
     * Observer of texture backing store events.
     */
    static interface EventListener {

        /**
         * Responds to an event from a texture backing store.
         *
         * @param type Type of event
         * @throws NullPointerException if event type is <tt>null</tt>
         */
        public void onBackingStoreEvent(EventType type);
    }

    /**
     * Type of event fired from the backing store.
     */
    static enum EventType {

        /**
         * Backing store being resized.
         */
        REALLOCATE,

        /**
         * Backing store could not be resized.
         */
        FAILURE
    };
}


/**
 * OpenGL texture.
 */
abstract class AbstractTexture {

    // ID of internal OpenGL texture
    protected final int handle;

    // GL_TEXTURE2D, etc
    protected final int type;

    // True for quality texturing
    protected final boolean mipmap;

    /**
     * Creates a texture.
     *
     * @param gl Current OpenGL context
     * @param type Type of texture
     * @param mipmap <tt>true</tt> for quality texturing
     * @throws AssertionError if context is <tt>null</tt>
     * @throws AssertionError if type is invalid
     */
    AbstractTexture(final GL gl, final int type, final boolean mipmap) {

        assert (gl != null);
        assert (isValidTextureType(type));

        this.handle = generate(gl);
        this.type = type;
        this.mipmap = mipmap;
    }

    /**
     * Binds underlying OpenGL texture on a texture unit.
     *
     * @param gl Current OpenGL context
     * @param unit OpenGL enumeration for a texture unit, i.e. <tt>GL_TEXTURE0</tt>
     * @throws AssertionError if context is <tt>null</tt>
     * @throws AssertionError if unit is invalid
     */
    void bind(final GL gl, final int unit) {

        assert (gl != null);
        assert (isValidTextureUnit(unit));

        gl.glActiveTexture(unit);
        gl.glBindTexture(type, handle);
    }

    /**
     * Destroys the texture.
     *
     * @param gl Current OpenGL context
     * @throws AssertionError if context is <tt>null</tt>
     */
    void dispose(final GL gl) {

        assert (gl != null);

        final int[] handles = new int[] { handle };
        gl.glDeleteTextures(1, handles, 0);
    }

    /**
     * Updates filter parameters for the texture.
     *
     * @param gl Current OpenGL context
     * @param smooth <tt>true</tt> to interpolate samples
     * @throws AssertionError if context is <tt>null</tt>
     */
    void setFiltering(final GL gl, final boolean smooth) {

        assert (gl != null);

        final int mag;
        final int min;
        if (smooth) {
            mag = GL.GL_LINEAR;
            min = mipmap ? GL.GL_LINEAR_MIPMAP_NEAREST : GL.GL_LINEAR;
        } else {
            mag = GL.GL_NEAREST;
            min = mipmap ? GL.GL_NEAREST_MIPMAP_NEAREST : GL.GL_NEAREST;
        }
        setParameter(gl, GL.GL_TEXTURE_MAG_FILTER, mag);
        setParameter(gl, GL.GL_TEXTURE_MIN_FILTER, min);
    }

    //--------------------------------------------------
    // Helpers
    //

    /**
     * Generates an OpenGL texture object.
     *
     * @param gl Current OpenGL context (assumed non-null)
     * @return Handle to the OpenGL texture
     * @throws NullPointerException if context is <tt>null</tt>
     */
    private static int generate(final GL gl) {
        final int[] handles = new int[1];
        gl.glGenTextures(1, handles, 0);
        return handles[0];
    }

    /**
     * Checks if an integer is a valid OpenGL enumeration for a texture type.
     *
     * @param type Integer to check
     * @return <tt>true</tt> if type is valid
     */
    private static boolean isValidTextureType(final int type) {
        switch (type) {
        case GL3.GL_TEXTURE_1D:
        case GL3.GL_TEXTURE_2D:
        case GL3.GL_TEXTURE_3D:
            return true;
        default:
            return false;
        }
    }

    /**
     * Checks if an integer is a valid OpenGL enumeration for a texture unit.
     *
     * @param unit Integer to check
     * @return <tt>true</tt> if unit is valid
     */
    private static boolean isValidTextureUnit(final int unit) {
        return (unit >= GL.GL_TEXTURE0) && (unit <= GL.GL_TEXTURE31);
    }

    /**
     * Changes a texture parameter for a 2D texture.
     *
     * @param gl Current OpenGL context (assumed non-null)
     * @param name Name of the parameter (assumed valid)
     * @param value Value of the parameter (assumed valid)
     */
    private void setParameter(final GL gl, final int name, final int value) {
        gl.glTexParameteri(type, name, value);
    }
}


/**
 * Two-dimensional OpenGL texture.
 */
abstract class Texture2D extends AbstractTexture {

    // Size on X axis
    protected final int width;

    // Size on Y axis
    protected final int height;

    /**
     * Creates a 2D texture.
     *
     * @param gl Current OpenGL context
     * @param width Size of texture on X axis
     * @param height Size of texture on Y axis
     * @param smooth <tt>true</tt> to interpolate samples
     * @param mipmap <tt>true</tt> for high quality
     * @throws AssertionError if context is <tt>null</tt>
     * @throws AssertionError if width or height is negative
     */
    Texture2D(final GL gl, final int width, final int height, final boolean smooth, final boolean mipmap) {

        super(gl, GL.GL_TEXTURE_2D, mipmap);

        assert (gl != null);
        assert (width >= 0);
        assert (height >= 0);

        // Copy parameters
        this.width = width;
        this.height = height;

        // Set up
        bind(gl, GL.GL_TEXTURE0);
        allocate(gl);
        setFiltering(gl, smooth);
    }

    /**
     * Updates the texture.
     *
     * <p>Copies any areas marked with {@link #mark(int, int, int,
     * int)} from the local image to the OpenGL texture.  Only those
     * areas will be modified.
     *
     * @param gl Current OpenGL context
     * @param pixels Data of entire image
     * @param area Region to update
     * @throws AssertionError if context is <tt>null</tt>
     * @throws AssertionError if pixels is <tt>null</tt>
     * @throws AssertionError if area is <tt>null</tt>
     */
    void update(final GL gl, final ByteBuffer pixels, final Rectangle area) {

        assert (gl != null);
        assert (pixels != null);
        assert (area != null);

        final int parameters[] = new int[4];

        // Store unpack parameters
        gl.glGetIntegerv(GL.GL_UNPACK_ALIGNMENT, parameters, 0);
        gl.glGetIntegerv(GL2.GL_UNPACK_SKIP_ROWS, parameters, 1);
        gl.glGetIntegerv(GL2.GL_UNPACK_SKIP_PIXELS, parameters, 2);
        gl.glGetIntegerv(GL2.GL_UNPACK_ROW_LENGTH, parameters, 3);

        // Change unpack parameters
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, area.y);
        gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, area.x);
        gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, width);

        // Update the texture
        gl.glTexSubImage2D(
                GL.GL_TEXTURE_2D,     // target
                0,                    // mipmap level
                area.x,               // x offset
                area.y,               // y offset
                area.width,           // width
                area.height,          // height
                getFormat(gl),        // format
                GL.GL_UNSIGNED_BYTE,  // type
                pixels);              // pixels

        // Reset unpack parameters
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, parameters[0]);
        gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, parameters[1]);
        gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, parameters[2]);
        gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, parameters[3]);

        // Generate mipmaps
        if (mipmap) {
            gl.glGenerateMipmap(GL.GL_TEXTURE_2D);
        }
    }

    //--------------------------------------------------
    // Helpers
    //

    /**
     * Allocates a 2D texture for use with a backing store.
     *
     * @param gl Current OpenGL context (assumed non-null)
     * @param width Width of texture
     * @param height Height of texture
     */
    private void allocate(final GL gl) {
        gl.glTexImage2D(
                GL.GL_TEXTURE_2D,          // target
                0,                         // level
                getInternalFormat(gl),     // internal format
                width,                     // width
                height,                    // height
                0,                         // border
                GL.GL_RGB,                 // format (unused)
                GL.GL_UNSIGNED_BYTE,       // type (unused)
                null);                     // pixels
    }

    /**
     * Determines the proper texture format for an OpenGL context.
     *
     * @param gl Current OpenGL context (assumed non-null)
     * @return Texture format enumeration for OpenGL context
     */
    protected abstract int getFormat(GL gl);

    /**
     * Determines the proper internal texture format for an OpenGL context.
     *
     * @param gl Current OpenGL context (assumed non-null)
     * @return Internal texture format enumeration for OpenGL context
     */
    protected abstract int getInternalFormat(GL gl);
}


/**
 * Two-dimensional, grayscale OpenGL texture.
 */
class GrayTexture2D extends Texture2D {

    /**
     * Creates a two-dimensional, grayscale texture.
     *
     * @param gl Current OpenGL context
     * @param width Size of texture on X axis
     * @param height Size of texture on Y axis
     * @param smooth <tt>true</tt> to interpolate samples
     * @param mipmap <tt>true</tt> for high quality
     * @throws NullPointerException if context is <tt>null</tt>
     */
    GrayTexture2D(final GL gl, final int width, final int height, final boolean smooth, final boolean mipmap) {
        super(gl, width, height, smooth, mipmap);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getFormat(final GL gl) {
        return gl.getGLProfile().isGL2() ? GL2.GL_LUMINANCE : GL3.GL_RED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getInternalFormat(final GL gl) {
        return gl.getGLProfile().isGL2() ? GL2.GL_INTENSITY : GL3.GL_RED;
    }
}
