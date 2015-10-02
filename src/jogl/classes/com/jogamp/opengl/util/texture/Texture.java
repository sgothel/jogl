/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.jogamp.opengl.util.texture;

import java.nio.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.*;
import com.jogamp.nativewindow.NativeWindowFactory;

import jogamp.opengl.*;

import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.util.texture.spi.*;

/**
 * Represents an OpenGL texture object. Contains convenience routines
 * for enabling/disabling OpenGL texture state, binding this texture,
 * and computing texture coordinates for both the entire image as well
 * as a sub-image.
 *
 * <a name="textureCallOrder"><h5>Order of Texture Commands</h5></a>
 * <p>
 * Due to many confusions w/ texture usage, following list described the order
 * and semantics of texture unit selection, binding and enabling.
 * <ul>
 *   <li><i>Optional:</i> Set active textureUnit via <code>gl.glActiveTexture(GL.GL_TEXTURE0 + textureUnit)</code>, <code>0</code> is default.</li>
 *   <li>Bind <code>textureId</code> -> active <code>textureUnit</code>'s <code>textureTarget</code> via <code>gl.glBindTexture(textureTarget, textureId)</code></li>
 *   <li><i>Compatible Context Only:</i> Enable active <code>textureUnit</code>'s <code>textureTarget</code> via <code>glEnable(textureTarget)</code>.
 *   <li><i>Optional:</i> Fiddle with the texture parameters and/or environment settings.</li>
 *   <li>GLSL: Use <code>textureUnit</code> in your shader program, enable shader program.</li>
 *   <li>Issue draw commands</li>
 * </ul>
 * </p>
 *
 * <p><a name="nonpow2"><b>Non-power-of-two restrictions</b></a>
 * <br> When creating an OpenGL texture object, the Texture class will
 * attempt to use <i>non-power-of-two textures</i> (NPOT) if available, see {@link GL#isNPOTTextureAvailable()}.
 * Further more,
 * <a href="http://www.opengl.org/registry/specs/ARB/texture_rectangle.txt">GL_ARB_texture_rectangle</a>
 * (RECT) will be attempted on OSX w/ ATI drivers.
 * If NPOT is not available or RECT not chosen, the Texture class will simply upload a non-pow2-sized
 * image into a standard pow2-sized texture (without any special
 * scaling).
 * Since the choice of extension (or whether one is used at
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
 * User may also utilize multiple texture units,
 * see <a href="#textureCallOrder"> order of texture commands above</a>.
 *
 * <p><a name="premult"><b>Alpha premultiplication and blending</b></a>
 * <p>
 * <i>Disclaimer: Consider performing alpha premultiplication in shader code, if really desired! Otherwise use RGBA.</i><br/>
 * </p>
 * <p>
 * The Texture class does not convert RGBA image data into
 * premultiplied data when storing it into an OpenGL texture.
 * </p>
 * <p>
 * The mathematically correct way to perform blending in OpenGL
 * with the SrcOver "source over destination" mode, or any other
 * Porter-Duff rule, is to use <i>premultiplied color components</i>,
 * which means the R/G/ B color components must have been multiplied by
 * the alpha value.  If using <i>premultiplied color components</i>
 * it is important to use the correct blending function; for
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
 * For reference, here is a list of the Porter-Duff compositing rules
 * and the associated OpenGL blend functions (source and destination
 * factors) to use in the face of premultiplied alpha:
 *
<CENTER>
<TABLE WIDTH="75%">
<TR> <TD> Rule     <TD> Source                  <TD> Dest
<TR> <TD> Clear    <TD> GL_ZERO                 <TD> GL_ZERO
<TR> <TD> Src      <TD> GL_ONE                  <TD> GL_ZERO
<TR> <TD> SrcOver  <TD> GL_ONE                  <TD> GL_ONE_MINUS_SRC_ALPHA
<TR> <TD> DstOver  <TD> GL_ONE_MINUS_DST_ALPHA  <TD> GL_ONE
<TR> <TD> SrcIn    <TD> GL_DST_ALPHA            <TD> GL_ZERO
<TR> <TD> DstIn    <TD> GL_ZERO                 <TD> GL_SRC_ALPHA
<TR> <TD> SrcOut   <TD> GL_ONE_MINUS_DST_ALPHA  <TD> GL_ZERO
<TR> <TD> DstOut   <TD> GL_ZERO                 <TD> GL_ONE_MINUS_SRC_ALPHA
<TR> <TD> Dst      <TD> GL_ZERO                 <TD> GL_ONE
<TR> <TD> SrcAtop  <TD> GL_DST_ALPHA            <TD> GL_ONE_MINUS_SRC_ALPHA
<TR> <TD> DstAtop  <TD> GL_ONE_MINUS_DST_ALPHA  <TD> GL_SRC_ALPHA
<TR> <TD> AlphaXor <TD> GL_ONE_MINUS_DST_ALPHA  <TD> GL_ONE_MINUS_SRC_ALPHA
</TABLE>
</CENTER>
 * @author Chris Campbell, Kenneth Russell, et.al.
 */
public class Texture {
    /** The GL target type for this texture. */
    private int target;
    /** The image GL target type for this texture, or its sub-components if cubemap. */
    private int imageTarget;
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

    @Override
    public String toString() {
        final String targetS = target == imageTarget ? Integer.toHexString(target) : Integer.toHexString(target) + " - image "+Integer.toHexString(imageTarget);
        return "Texture[target "+targetS+", name "+texID+", "+
                imgWidth+"/"+texWidth+" x "+imgHeight+"/"+texHeight+", y-flip "+mustFlipVertically+
                ", "+estimatedMemorySize+" bytes]";
    }

    /** An estimate of the amount of texture memory this texture consumes. */
    private int estimatedMemorySize;

    private static final boolean DEBUG = Debug.debug("Texture");
    private static final boolean VERBOSE = Debug.verbose();

    // For testing alternate code paths on more capable hardware
    private static final boolean disableNPOT    = Debug.isPropertyDefined("jogl.texture.nonpot", true);
    private static final boolean disableTexRect = Debug.isPropertyDefined("jogl.texture.notexrect", true);

    public Texture(final GL gl, final TextureData data) throws GLException {
        this.texID = 0;
        this.target = 0;
        this.imageTarget = 0;
        updateImage(gl, data);
    }

    /**
     * Constructor for use when creating e.g. cube maps, where there is
     * no initial texture data
     * @param target the OpenGL texture target, eg GL.GL_TEXTURE_2D,
     *               GL2.GL_TEXTURE_RECTANGLE
     */
    public Texture(final int target) {
        this.texID = 0;
        this.target = target;
        this.imageTarget = target;
    }

    /**
     * Constructor to wrap an OpenGL texture ID from an external library and allows
     * some of the base methods from the Texture class, such as
     * binding and querying of texture coordinates, to be used with
     * it. Attempts to update such textures' contents will yield
     * undefined results.
     *
     * @param textureID the OpenGL texture object to wrap
     * @param target the OpenGL texture target, eg GL.GL_TEXTURE_2D,
     *               GL2.GL_TEXTURE_RECTANGLE
     * @param texWidth the width of the texture in pixels
     * @param texHeight the height of the texture in pixels
     * @param imgWidth the width of the image within the texture in
     *          pixels (if the content is a sub-rectangle in the upper
     *          left corner); otherwise, pass in texWidth
     * @param imgHeight the height of the image within the texture in
     *          pixels (if the content is a sub-rectangle in the upper
     *          left corner); otherwise, pass in texHeight
     * @param mustFlipVertically indicates whether the texture
     *                           coordinates must be flipped vertically
     *                           in order to properly display the
     *                           texture
     */
    public Texture(final int textureID, final int target,
                   final int texWidth, final int texHeight,
                   final int imgWidth, final int imgHeight,
                   final boolean mustFlipVertically) {
        this.texID = textureID;
        this.target = target;
        this.imageTarget = target;
        this.mustFlipVertically = mustFlipVertically;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.aspectRatio = (float) imgWidth / (float) imgHeight;
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
        this.updateTexCoords();
    }

    /**
     * Enables this texture's target (e.g., GL_TEXTURE_2D) in the
     * given GL context's state. This method is a shorthand equivalent
     * of the following OpenGL code:
     * <pre>
     *   gl.glEnable(texture.getTarget());
     * </pre>
     * <p>
     * Call is ignored if the {@link GL} object's context
     * is using a core profile, see {@link GL#isGLcore()},
     * or if {@link #getTarget()} is {@link GLES2#GL_TEXTURE_EXTERNAL_OES}.
     * </p>
     * <p>
     * See the <a href="#perftips">performance tips</a> above for hints
     * on how to maximize performance when using many Texture objects.
     * </p>
     * @param gl the current GL object
     *
     * @throws GLException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void enable(final GL gl) throws GLException {
        if( !gl.isGLcore() && GLES2.GL_TEXTURE_EXTERNAL_OES != target) {
            gl.glEnable(target);
        }
    }

    /**
     * Disables this texture's target (e.g., GL_TEXTURE_2D) in the
     * given GL state. This method is a shorthand equivalent
     * of the following OpenGL code:
     * <pre>
     *   gl.glDisable(texture.getTarget());
     * </pre>
     * <p>
     * Call is ignored if the {@link GL} object's context
     * is using a core profile, see {@link GL#isGLcore()},
     * or if {@link #getTarget()} is {@link GLES2#GL_TEXTURE_EXTERNAL_OES}.
     * </p>
     * <p>
     * See the <a href="#perftips">performance tips</a> above for hints
     * on how to maximize performance when using many Texture objects.
     * </p>
     * @param gl the current GL object
     *
     * @throws GLException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void disable(final GL gl) throws GLException {
        if( !gl.isGLcore() && GLES2.GL_TEXTURE_EXTERNAL_OES != target ) {
            gl.glDisable(target);
        }
    }

    /**
     * Binds this texture to the given GL context. This method is a
     * shorthand equivalent of the following OpenGL code:
     <pre>
     gl.glBindTexture(texture.getTarget(), texture.getTextureObject());
     </pre>
     *
     * See the <a href="#perftips">performance tips</a> above for hints
     * on how to maximize performance when using many Texture objects.
     *
     * @param gl the current GL context
     * @throws GLException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void bind(final GL gl) throws GLException {
        validateTexID(gl, true);
        gl.glBindTexture(target, texID);
    }

    /**
     * Destroys the native resources used by this texture object.
     *
     * @throws GLException if any OpenGL-related errors occurred
     */
    public void destroy(final GL gl) throws GLException {
        if(0!=texID) {
            gl.glDeleteTextures(1, new int[] {texID}, 0);
            texID = 0;
        }
    }

    /**
     * Returns the OpenGL "target" of this texture.
     * @see com.jogamp.opengl.GL#GL_TEXTURE_2D
     * @see com.jogamp.opengl.GL2#GL_TEXTURE_RECTANGLE_ARB
     */
    public int getTarget() {
        return target;
    }

    /**
     * Returns the image OpenGL "target" of this texture, or its sub-components if cubemap.
     * @see com.jogamp.opengl.GL#GL_TEXTURE_2D
     * @see com.jogamp.opengl.GL2#GL_TEXTURE_RECTANGLE_ARB
     */
    public int getImageTarget() {
        return imageTarget;
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
    public TextureCoords getSubImageTexCoords(final int x1, final int y1, final int x2, final int y2) {
        if (GL2.GL_TEXTURE_RECTANGLE_ARB == imageTarget) {
            if (mustFlipVertically) {
                return new TextureCoords(x1, texHeight - y1, x2, texHeight - y2);
            } else {
                return new TextureCoords(x1, y1, x2, y2);
            }
        } else {
            final float tx1 = (float)x1 / (float)texWidth;
            final float ty1 = (float)y1 / (float)texHeight;
            final float tx2 = (float)x2 / (float)texWidth;
            final float ty2 = (float)y2 / (float)texHeight;
            if (mustFlipVertically) {
                final float yMax = (float) imgHeight / (float) texHeight;
                return new TextureCoords(tx1, yMax - ty1, tx2, yMax - ty2);
            } else {
                return new TextureCoords(tx1, ty1, tx2, ty2);
            }
        }
    }

    /**
     * Updates the entire content area incl. {@link TextureCoords}
     * of this texture using the data in the given image.
     *
     * @throws GLException if any OpenGL-related errors occurred
     */
    public void updateImage(final GL gl, final TextureData data) throws GLException {
        updateImage(gl, data, 0);
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
     * Change whether the TextureData requires a vertical flip of
     * the texture coords.
     * <p>
     * No-op if no change, otherwise generates new {@link TextureCoords}.
     * </p>
     */
    public void setMustFlipVertically(final boolean v) {
        if( v != mustFlipVertically ) {
            mustFlipVertically = v;
            updateTexCoords();
        }
    }

    /**
     * Updates the content area incl. {@link TextureCoords} of the specified target of this texture
     * using the data in the given image. In general this is intended
     * for construction of cube maps.
     *
     * @throws GLException if any OpenGL-related errors occurred
     */
    public void updateImage(final GL gl, final TextureData data, final int targetOverride) throws GLException {
        validateTexID(gl, true);

        imgWidth = data.getWidth();
        imgHeight = data.getHeight();
        aspectRatio = (float) imgWidth / (float) imgHeight;
        mustFlipVertically = data.getMustFlipVertically();

        int texTarget = 0;
        int texParamTarget = this.target;

        // See whether we have automatic mipmap generation support
        boolean haveAutoMipmapGeneration =
            (gl.isExtensionAvailable(GLExtensions.VERSION_1_4) ||
             gl.isExtensionAvailable(GLExtensions.SGIS_generate_mipmap));

        // Indicate to the TextureData what functionality is available
        data.setHaveEXTABGR(gl.isExtensionAvailable(GLExtensions.EXT_abgr));
        data.setHaveGL12(gl.isExtensionAvailable(GLExtensions.VERSION_1_2));

        // Indicates whether both width and height are power of two
        final boolean isPOT = isPowerOfTwo(imgWidth) && isPowerOfTwo(imgHeight);

        // Note that automatic mipmap generation doesn't work for
        // GL_ARB_texture_rectangle
        if (!isPOT && !haveNPOT(gl)) {
            haveAutoMipmapGeneration = false;
        }

        boolean expandingCompressedTexture = false;
        boolean done = false;
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
            done = true;
        }

        if (!done && preferTexRect(gl) && !isPOT &&
            haveTexRect(gl) && !data.isDataCompressed() && !gl.isGL3() && !gl.isGLES()) {
            // GL_ARB_texture_rectangle does not work for compressed textures
            if (DEBUG) {
                System.err.println("Using GL_ARB_texture_rectangle preferentially on this hardware");
            }

            texWidth = imgWidth;
            texHeight = imgHeight;
            texTarget = GL2.GL_TEXTURE_RECTANGLE_ARB;
            done = true;
        }

        if (!done && (isPOT || haveNPOT(gl))) {
            if (DEBUG) {
                if (isPOT) {
                    System.err.println("Power-of-two texture");
                } else {
                    System.err.println("Using GL_ARB_texture_non_power_of_two");
                }
            }

            texWidth = imgWidth;
            texHeight = imgHeight;
            texTarget = GL.GL_TEXTURE_2D;
            done = true;
        }

        if (!done && haveTexRect(gl) && !data.isDataCompressed() && !gl.isGL3() && !gl.isGLES()) {
            // GL_ARB_texture_rectangle does not work for compressed textures
            if (DEBUG) {
                System.err.println("Using GL_ARB_texture_rectangle");
            }

            texWidth = imgWidth;
            texHeight = imgHeight;
            texTarget = GL2.GL_TEXTURE_RECTANGLE_ARB;
            done = true;
        }

        if (!done) {
            // If we receive non-power-of-two compressed texture data and
            // don't have true hardware support for compressed textures, we
            // can fake this support by producing an empty "compressed"
            // texture image, using glCompressedTexImage2D with that to
            // allocate the texture, and glCompressedTexSubImage2D with the
            // incoming data.
            if (data.isDataCompressed()) {
                if (data.getMipmapData() != null) {

                    // We don't currently support expanding of compressed,
                    // mipmapped non-power-of-two textures to the nearest power
                    // of two; the obvious port of the non-mipmapped code didn't
                    // work
                    throw new GLException("Mipmapped non-power-of-two compressed textures only supported on OpenGL 2.0 hardware (GL_ARB_texture_non_power_of_two)");
                }

                expandingCompressedTexture = true;
            }

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
        imageTarget = texTarget;
        updateTexCoords();

        if (targetOverride != 0) {
            // Allow user to override auto detection and skip bind step (for
            // cubemap construction)
            if (this.target == 0) {
                throw new GLException("Override of target failed; no target specified yet");
            }
            texTarget = targetOverride;
            texParamTarget = this.target;
            gl.glBindTexture(texParamTarget, texID);
        } else {
            gl.glBindTexture(texTarget, texID);
        }

        if (data.getMipmap() && !haveAutoMipmapGeneration) {
            final int[] align = new int[1];
            gl.glGetIntegerv(GL.GL_UNPACK_ALIGNMENT, align, 0); // save alignment
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, data.getAlignment());

            if (data.isDataCompressed()) {
                throw new GLException("May not request mipmap generation for compressed textures");
            }

            try {
                // FIXME: may need check for GLUnsupportedException
                final GLU glu = GLU.createGLU(gl);
                glu.gluBuild2DMipmaps(texTarget, data.getInternalFormat(),
                                      data.getWidth(), data.getHeight(),
                                      data.getPixelFormat(), data.getPixelType(), data.getBuffer());
            } finally {
                gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, align[0]); // restore alignment
            }
        } else {
            checkCompressedTextureExtensions(gl, data);
            final Buffer[] mipmapData = data.getMipmapData();
            if (mipmapData != null) {
                int width = texWidth;
                int height = texHeight;
                for (int i = 0; i < mipmapData.length; i++) {
                    if (data.isDataCompressed()) {
                        // Need to use glCompressedTexImage2D directly to allocate and fill this image
                        // Avoid spurious memory allocation when possible
                        gl.glCompressedTexImage2D(texTarget, i, data.getInternalFormat(),
                                                  width, height, data.getBorder(),
                                                  mipmapData[i].remaining(), mipmapData[i]);
                    } else {
                        // Allocate texture image at this level
                        gl.glTexImage2D(texTarget, i, data.getInternalFormat(),
                                        width, height, data.getBorder(),
                                        data.getPixelFormat(), data.getPixelType(), null);
                        updateSubImageImpl(gl, data, texTarget, i, 0, 0, 0, 0, data.getWidth(), data.getHeight());
                    }

                    width = Math.max(width / 2, 1);
                    height = Math.max(height / 2, 1);
                }
            } else {
                if (data.isDataCompressed()) {
                    if (!expandingCompressedTexture) {
                        // Need to use glCompressedTexImage2D directly to allocate and fill this image
                        // Avoid spurious memory allocation when possible
                        gl.glCompressedTexImage2D(texTarget, 0, data.getInternalFormat(),
                                                  texWidth, texHeight, data.getBorder(),
                                                  data.getBuffer().capacity(), data.getBuffer());
                    } else {
                        final ByteBuffer buf = DDSImage.allocateBlankBuffer(texWidth,
                                                                      texHeight,
                                                                      data.getInternalFormat());
                        gl.glCompressedTexImage2D(texTarget, 0, data.getInternalFormat(),
                                                  texWidth, texHeight, data.getBorder(),
                                                  buf.capacity(), buf);
                        updateSubImageImpl(gl, data, texTarget, 0, 0, 0, 0, 0, data.getWidth(), data.getHeight());
                    }
                } else {
                    if (data.getMipmap() && haveAutoMipmapGeneration && gl.isGL2ES1()) {
                        // For now, only use hardware mipmapping for uncompressed 2D
                        // textures where the user hasn't explicitly specified
                        // mipmap data; don't know about interactions between
                        // GL_GENERATE_MIPMAP and glCompressedTexImage2D
                        gl.glTexParameteri(texParamTarget, GL2ES1.GL_GENERATE_MIPMAP, GL.GL_TRUE);
                        usingAutoMipmapGeneration = true;
                    }

                    gl.glTexImage2D(texTarget, 0, data.getInternalFormat(),
                                    texWidth, texHeight, data.getBorder(),
                                    data.getPixelFormat(), data.getPixelType(), null);
                    updateSubImageImpl(gl, data, texTarget, 0, 0, 0, 0, 0, data.getWidth(), data.getHeight());
                }
            }
        }

        final int minFilter = (data.getMipmap() ? GL.GL_LINEAR_MIPMAP_LINEAR : GL.GL_LINEAR);
        final int magFilter = GL.GL_LINEAR;
        final int wrapMode = (gl.isExtensionAvailable(GLExtensions.VERSION_1_2) || !gl.isGL2()) ? GL.GL_CLAMP_TO_EDGE : GL2.GL_CLAMP;

        // REMIND: figure out what to do for GL_TEXTURE_RECTANGLE_ARB
        if (texTarget != GL2.GL_TEXTURE_RECTANGLE_ARB) {
            gl.glTexParameteri(texParamTarget, GL.GL_TEXTURE_MIN_FILTER, minFilter);
            gl.glTexParameteri(texParamTarget, GL.GL_TEXTURE_MAG_FILTER, magFilter);
            gl.glTexParameteri(texParamTarget, GL.GL_TEXTURE_WRAP_S, wrapMode);
            gl.glTexParameteri(texParamTarget, GL.GL_TEXTURE_WRAP_T, wrapMode);
            if (this.target == GL.GL_TEXTURE_CUBE_MAP) {
                gl.glTexParameteri(texParamTarget, GL2ES2.GL_TEXTURE_WRAP_R, wrapMode);
            }
        }

        // Don't overwrite target if we're loading e.g. faces of a cube
        // map
        if ((this.target == 0) ||
            (this.target == GL.GL_TEXTURE_2D) ||
            (this.target == GL2.GL_TEXTURE_RECTANGLE_ARB)) {
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
     * @throws GLException if any OpenGL-related errors occurred
     */
    public void updateSubImage(final GL gl, final TextureData data, final int mipmapLevel, final int x, final int y) throws GLException {
        if (usingAutoMipmapGeneration && mipmapLevel != 0) {
            // When we're using mipmap generation via GL_GENERATE_MIPMAP, we
            // don't need to update other mipmap levels
            return;
        }
        bind(gl);
        updateSubImageImpl(gl, data, target, mipmapLevel, x, y, 0, 0, data.getWidth(), data.getHeight());
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
    public void updateSubImage(final GL gl, final TextureData data, final int mipmapLevel,
                               final int dstx, final int dsty,
                               final int srcx, final int srcy,
                               final int width, final int height) throws GLException {
        if (data.isDataCompressed()) {
            throw new GLException("updateSubImage specifying a sub-rectangle is not supported for compressed TextureData");
        }
        if (usingAutoMipmapGeneration && mipmapLevel != 0) {
            // When we're using mipmap generation via GL_GENERATE_MIPMAP, we
            // don't need to update other mipmap levels
            return;
        }
        bind(gl);
        updateSubImageImpl(gl, data, target, mipmapLevel, dstx, dsty, srcx, srcy, width, height);
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
    public void setTexParameterf(final GL gl, final int parameterName,
                                 final float value) {
        bind(gl);
        gl.glTexParameterf(target, parameterName, value);
    }

    /**
     * Sets the OpenGL multi-floating-point texture parameter for the
     * texture's target. Causes this texture to be bound to the current
     * texture state.
     *
     * @throws GLException if any OpenGL-related errors occurred
     */
    public void setTexParameterfv(final GL gl, final int parameterName,
                                  final FloatBuffer params) {
        bind(gl);
        gl.glTexParameterfv(target, parameterName, params);
    }

    /**
     * Sets the OpenGL multi-floating-point texture parameter for the
     * texture's target. Causes this texture to be bound to the current
     * texture state.
     *
     * @throws GLException if any OpenGL-related errors occurred
     */
    public void setTexParameterfv(final GL gl, final int parameterName,
                                  final float[] params, final int params_offset) {
        bind(gl);
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
     * @throws GLException if any OpenGL-related errors occurred
     */
    public void setTexParameteri(final GL gl, final int parameterName,
                                 final int value) {
        bind(gl);
        gl.glTexParameteri(target, parameterName, value);
    }

    /**
     * Sets the OpenGL multi-integer texture parameter for the texture's
     * target. Causes this texture to be bound to the current texture
     * state.
     *
     * @throws GLException if any OpenGL-related errors occurred
     */
    public void setTexParameteriv(final GL gl, final int parameterName,
                                  final IntBuffer params) {
        bind(gl);
        gl.glTexParameteriv(target, parameterName, params);
    }

    /**
     * Sets the OpenGL multi-integer texture parameter for the texture's
     * target. Causes this texture to be bound to the current texture
     * state.
     *
     * @throws GLException if any OpenGL-related errors occurred
     */
    public void setTexParameteriv(final GL gl, final int parameterName,
                                  final int[] params, final int params_offset) {
        bind(gl);
        gl.glTexParameteriv(target, parameterName, params, params_offset);
    }

    /**
     * Returns the underlying OpenGL texture object for this texture
     * and generates it if not done yet.
     * <p>
     * Most applications will not need to access this, since it is
     * handled automatically by the bind(GL) and destroy(GL) APIs.
     * </p>
     * @param gl required to be valid and current in case the texture object has not been generated yet,
     *           otherwise it may be <code>null</code>.
     * @see #getTextureObject()
     */
    public int getTextureObject(final GL gl) {
        validateTexID(gl, false);
        return texID;
    }

    /**
     * Returns the underlying OpenGL texture object for this texture,
     * maybe <code>0</code> if not yet generated.
     * <p>
     * Most applications will not need to access this, since it is
     * handled automatically by the bind(GL) and destroy(GL) APIs.
     * </p>
     * @see #getTextureObject(GL)
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
    private static boolean isPowerOfTwo(final int val) {
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
    private static int nextPowerOfTwo(final int val) {
        int ret = 1;
        while (ret < val) {
            ret <<= 1;
        }
        return ret;
    }

    private void updateTexCoords() {
        if ( GL2.GL_TEXTURE_RECTANGLE_ARB == imageTarget ) {
            if (mustFlipVertically) {
                coords = new TextureCoords(0, imgHeight, imgWidth, 0);
            } else {
                coords = new TextureCoords(0, 0, imgWidth, imgHeight);
            }
        } else {
            if (mustFlipVertically) {
                coords = new TextureCoords(0,                                      // l
                                           (float) imgHeight / (float) texHeight,  // b
                                           (float) imgWidth / (float) texWidth,    // r
                                           0                                       // t
                                          );
            } else {
                coords = new TextureCoords(0,                                      // l
                                           0,                                      // b
                                           (float) imgWidth / (float) texWidth,    // r
                                           (float) imgHeight / (float) texHeight   // t
                                          );
            }
        }
    }

    private void updateSubImageImpl(final GL gl, final TextureData data, final int newTarget, final int mipmapLevel,
                                    int dstx, int dsty,
                                    int srcx, int srcy, int width, int height) throws GLException {
        data.setHaveEXTABGR(gl.isExtensionAvailable(GLExtensions.EXT_abgr));
        data.setHaveGL12(gl.isExtensionAvailable(GLExtensions.VERSION_1_2));

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
                width = Math.max(width / 2, 1);
                height = Math.max(height / 2, 1);

                dataWidth = Math.max(dataWidth / 2, 1);
                dataHeight = Math.max(dataHeight / 2, 1);
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

        checkCompressedTextureExtensions(gl, data);

        if (data.isDataCompressed()) {
            gl.glCompressedTexSubImage2D(newTarget, mipmapLevel,
                                         dstx, dsty, width, height,
                                         data.getInternalFormat(),
                                         buffer.remaining(), buffer);
        } else {
            final int[] align = { 0 };
            final int[] rowLength = { 0 };
            final int[] skipRows = { 0 };
            final int[] skipPixels = { 0 };
            gl.glGetIntegerv(GL.GL_UNPACK_ALIGNMENT,   align,      0); // save alignment
            if(gl.isGL2GL3()) {
                gl.glGetIntegerv(GL2ES2.GL_UNPACK_ROW_LENGTH,  rowLength,  0); // save row length
                gl.glGetIntegerv(GL2ES2.GL_UNPACK_SKIP_ROWS,   skipRows,   0); // save skipped rows
                gl.glGetIntegerv(GL2ES2.GL_UNPACK_SKIP_PIXELS, skipPixels, 0); // save skipped pixels
            }
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
            if(gl.isGL2GL3()) {
                gl.glPixelStorei(GL2ES2.GL_UNPACK_ROW_LENGTH, rowlen);
                gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_ROWS, srcy);
                gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_PIXELS, srcx);
            } else {
                if ( rowlen!=0 && rowlen!=width &&
                     srcy!=0 && srcx!=0 ) {
                    throw new GLException("rowlen and/or x/y offset only available for GL2");
                }
            }

            gl.glTexSubImage2D(newTarget, mipmapLevel,
                               dstx, dsty, width, height,
                               data.getPixelFormat(), data.getPixelType(),
                               buffer);
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT,   align[0]);      // restore alignment
            if(gl.isGL2GL3()) {
                gl.glPixelStorei(GL2ES2.GL_UNPACK_ROW_LENGTH,  rowLength[0]);  // restore row length
                gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_ROWS,   skipRows[0]);   // restore skipped rows
                gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_PIXELS, skipPixels[0]); // restore skipped pixels
            }
        }
    }

    private void checkCompressedTextureExtensions(final GL gl, final TextureData data) {
        if (data.isDataCompressed()) {
            switch (data.getInternalFormat()) {
            case GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT:
            case GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
            case GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
            case GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
                if (!gl.isExtensionAvailable(GLExtensions.EXT_texture_compression_s3tc) &&
                    !gl.isExtensionAvailable(GLExtensions.NV_texture_compression_vtc)) {
                    throw new GLException("DXTn compressed textures not supported by this graphics card");
                }
                break;
            default:
                // FI1027GXME: should test availability of more texture
                // compression extensions here
                break;
            }
        }
    }

    private boolean validateTexID(final GL gl, final boolean throwException) {
        if( 0 == texID ) {
            if( null != gl ) {
                final int[] tmp = new int[1];
                gl.glGenTextures(1, tmp, 0);
                texID = tmp[0];
                if ( 0 == texID && throwException ) {
                    throw new GLException("Create texture ID invalid: texID "+texID+", glerr 0x"+Integer.toHexString(gl.glGetError()));
                }
            } else if ( throwException ) {
                throw new GLException("No GL context given, can't create texture ID");
            }
        }
        return 0 != texID;
    }

    // Helper routines for disabling certain codepaths
    private static boolean haveNPOT(final GL gl) {
        return !disableNPOT && gl.isNPOTTextureAvailable();
    }

    private static boolean haveTexRect(final GL gl) {
        return (!disableTexRect &&
                TextureIO.isTexRectEnabled() &&
                gl.isExtensionAvailable(GLExtensions.ARB_texture_rectangle));
    }

    private static boolean preferTexRect(final GL gl) {
        // Prefer GL_ARB_texture_rectangle on ATI hardware on Mac OS X
        // due to software fallbacks

        if (NativeWindowFactory.TYPE_MACOSX == NativeWindowFactory.getNativeWindowType(false)) {
            final String vendor = gl.glGetString(GL.GL_VENDOR);
            if (vendor != null && vendor.startsWith("ATI")) {
                return true;
            }
        }

        return false;
    }
}
