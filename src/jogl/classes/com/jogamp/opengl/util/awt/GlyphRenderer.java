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
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.awt.QuadPipeline.Quad;
import com.jogamp.opengl.util.texture.TextureCoords;

import java.util.ArrayList;
import java.util.List;


/**
 * Utility for drawing glyphs.
 */
interface GlyphRenderer {

    /**
     * Registers an {@link EventListener} with this {@link GlyphRenderer}.
     *
     * @param listener Listener to register
     * @throws NullPointerException if listener is null
     */
    void addListener(/*@Nonnull*/ EventListener listener);

    /**
     * Starts a render cycle with this {@link GlyphRenderer}.
     *
     * @param gl Current OpenGL context
     * @param ortho True if using orthographic projection
     * @param width Width of current OpenGL viewport
     * @param height Height of current OpenGL viewport
     * @param disableDepthTest True if should ignore depth values
     * @throws NullPointerException if context is null
     * @throws IllegalArgumentException if width or height is negative
     * @throws GLException if context is unexpected version
     */
    void beginRendering(/*@Nonnull*/ GL gl,
                        boolean ortho,
                        /*@Nonnegative*/ int width,
                        /*@Nonnegative*/ int height,
                        boolean disableDepthTest);

    /**
     * Frees resources used by this {@link GlyphRenderer}.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is null
     * @throws GLException if context is unexpected version
     */
    void dispose(/*@Nonnull*/ GL gl);

    /**
     * Draws a glyph with this {@link GlyphRenderer}.
     *
     * @param gl Current OpenGL context
     * @param glyph Visual representation of a character
     * @param x Position to draw on X axis, which may be negative
     * @param y Position to draw on Y axis, which may be negative
     * @param z Position to draw on Z axis, which may be negative
     * @param scale Relative size of glyph, which may be negative
     * @param coords Texture coordinates of glyph
     * @return Distance to next character, which may be negative
     * @throws NullPointerException if context, glyph, or texture coordinate is null
     * @throws GLException if context is unexpected version
     */
    /*@CheckForSigned*/
    float drawGlyph(/*@Nonnull*/ GL gl,
                    /*@Nonnull*/ Glyph glyph,
                    /*@CheckForSigned*/ float x,
                    /*@CheckForSigned*/ float y,
                    /*@CheckForSigned*/ float z,
                    /*@CheckForSigned*/ float scale,
                    /*@Nonnull*/ TextureCoords coords);

    /**
     * Finishes a render cycle with this {@link GlyphRenderer}.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is null
     * @throws GLException if context is unexpected version
     */
    void endRendering(/*@Nonnull*/ GL gl);

    /**
     * Forces all stored text to be rendered.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     * @throws IllegalStateException if not in a render cycle
     */
    void flush(/*@Nonnull*/ GL gl);

    /**
     * Checks if this {@link GlyphRenderer} is using vertex arrays.
     *
     * @return True if this renderer is using vertex arrays
     */
    boolean getUseVertexArrays();

    /**
     * Changes the color used to draw the text.
     *
     * @param r Red component of color
     * @param g Green component of color
     * @param b Blue component of color
     * @param a Alpha component of color
     */
    void setColor(float r, float g, float b, float a);

    /**
     * Changes the transformation matrix for drawing in 3D.
     *
     * @param gl Current OpenGL context
     * @param value Matrix as float array
     * @param transpose True if array is in in row-major order
     * @throws IndexOutOfBoundsException if value's length is less than sixteen
     * @throws IllegalStateException if in orthographic mode
     */
    void setTransform(/*@Nonnull*/ float[] value, boolean transpose);

    /**
     * Changes whether vertex arrays are in use.
     *
     * @param useVertexArrays <tt>true</tt> to use vertex arrays
     */
    void setUseVertexArrays(boolean useVertexArrays);

    /**
     * <i>Observer</i> of a {@link GlyphRenderer}.
     */
    interface EventListener {

        /**
         * Responds to an event from a glyph renderer.
         *
         * @param type Type of event
         * @throws NullPointerException if event type is <tt>null</tt>
         */
        public void onGlyphRendererEvent(EventType type);
    }

    /**
     * Type of event fired from the renderer.
     */
    static enum EventType {

        /**
         * Renderer is automatically flushing queued glyphs, e.g., when it's full or color changes.
         */
        AUTOMATIC_FLUSH;
    }
}


/**
 * Skeletal implementation of {@link GlyphRenderer}.
 */
abstract class AbstractGlyphRenderer implements GlyphRenderer, QuadPipeline.EventListener {

    // Default color
    private static float DEFAULT_RED = 1.0f;
    private static float DEFAULT_GREEN = 1.0f;
    private static float DEFAULT_BLUE = 1.0f;
    private static float DEFAULT_ALPHA = 1.0f;

    /**
     * Listeners to send events to.
     */
    /*@Nonnull*/
    private final List<EventListener> listeners = new ArrayList<EventListener>();

    /**
     * Quad to send to pipeline.
     */
    /*@Nonnull*/
    private final Quad quad = new Quad();

    /**
     * Buffer of quads.
     */
    /*@CheckForNull*/
    private QuadPipeline pipeline = null;

    /**
     * Whether pipeline needs to be flushed.
     */
    private boolean pipelineDirty = true;

    /**
     * True if between begin and end calls.
     */
    private boolean inRenderCycle = false;

    /**
     * True if orthographic.
     */
    private boolean orthoMode = false;

    /**
     * Red component of color.
     */
    private float r = DEFAULT_RED;

    /**
     * Green component of color.
     */
    private float g = DEFAULT_GREEN;

    /**
     * Blue component of color.
     */
    private float b = DEFAULT_BLUE;

    /**
     * Alpha component of color.
     */
    private float a = DEFAULT_ALPHA;

    /**
     * True if color needs to be updated.
     */
    private boolean colorDirty = true;

    /**
     * Transformation matrix for 3D mode.
     */
    /*@Nonnull*/
    private final float[] transform = new float[16];

    /**
     * Whether transformation matrix is in row-major order instead of column-major.
     */
    private boolean transposed = false;

    // TODO: Should `transformDirty` start out as true?
    /**
     * Whether transformation matrix needs to be updated.
     */
    private boolean transformDirty = false;

    /**
     * Constructs an {@link AbstractGlyphRenderer}.
     */
    AbstractGlyphRenderer() {
        // empty
    }

    @Override
    public final void addListener(/*@Nonnull*/ final EventListener listener) {
        listeners.add(listener);
    }

    @Override
    public final void beginRendering(/*@Nonnull*/ final GL gl,
                                     final boolean ortho,
                                     /*@Nonnegative*/ final int width,
                                     /*@Nonnegative*/ final int height,
                                     final boolean disableDepthTest) {

        // Perform hook
        doBeginRendering(gl, ortho, width, height, disableDepthTest);

        // Store text renderer state
        inRenderCycle = true;
        orthoMode = ortho;

        // Make sure the pipeline is made
        if (pipelineDirty) {
            setPipeline(gl, doCreateQuadPipeline(gl));
        }

        // Pass to quad renderer
        pipeline.beginRendering(gl);

        // Make sure color is correct
        if (colorDirty) {
            doSetColor(gl, r, g, b, a);
            colorDirty = false;
        }

        // Make sure transform is correct
        if (transformDirty) {
            doSetTransform3d(gl, transform, transposed);
            transformDirty = false;
        }
    }

    /*@Nonnull*/
    private static <T> T checkNotNull(/*@Nullable*/ final T obj,
                                      /*@CheckForNull*/ final String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
        return obj;
    }

    private static void checkState(final boolean condition,
                                   /*@CheckForNull*/ final String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Requests that the pipeline be replaced on the next call to {@link #beginRendering}.
     */
    protected final void dirtyPipeline() {
        pipelineDirty = true;
    }

    @Override
    public final void dispose(/*@Nonnull*/ final GL gl) {
        doDispose(gl);
        listeners.clear();
        pipeline.dispose(gl);
    }

    /**
     * Actually starts a render cycle.
     *
     * @param gl Current OpenGL context
     * @param ortho True if using orthographic projection
     * @param width Width of current OpenGL viewport
     * @param height Height of current OpenGL viewport
     * @param disableDepthTest True if should ignore depth values
     * @throws NullPointerException if context is null
     * @throws IllegalArgumentException if width or height is negative
     * @throws GLException if context is unexpected version
     */
    protected abstract void doBeginRendering(/*@Nonnull*/ final GL gl,
                                             final boolean ortho,
                                             /*@Nonnegative*/ final int width,
                                             /*@Nonnegative*/ final int height,
                                             final boolean disableDepthTest);

    /**
     * Actually creates the quad pipeline for rendering quads.
     *
     * @param gl Current OpenGL context
     * @return Quad pipeline to render quads with
     * @throws NullPointerException if context is null
     */
    protected abstract QuadPipeline doCreateQuadPipeline(/*@Nonnull*/ final GL gl);

    /**
     * Actually frees resources used by the renderer.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is null
     * @throws GLException if context is unexpected version
     */
    protected abstract void doDispose(/*@Nonnull*/ final GL gl);

    /**
     * Actually finishes a render cycle.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is null
     * @throws GLException if context is unexpected version
     */
    protected abstract void doEndRendering(/*@Nonnull*/ final GL gl);

    /**
     * Actually changes the color when user calls {@link #setColor}.
     *
     * @param gl Current OpenGL context
     * @param r Red component of color
     * @param g Green component of color
     * @param b Blue component of color
     * @param a Alpha component of color
     * @throws NullPointerException if context is null
     * @throws GLException if context is unexpected version
     */
    protected abstract void doSetColor(/*@Nonnull*/ final GL gl,
                                       float r,
                                       float g,
                                       float b,
                                       float a);

    /**
     * Actually changes the MVP matrix when using an arbitrary projection.
     *
     * @param gl Current OpenGL context
     * @param value Matrix as float array
     * @param transpose True if in row-major order
     * @throws NullPointerException if context is null
     * @throws GLException if context is unexpected version
     * @throws IndexOutOfBoundsException if length of value is less than sixteen
     */
    protected abstract void doSetTransform3d(/*@Nonnull*/ GL gl,
                                             /*@Nonnull*/ float[] value,
                                             boolean transpose);

    /**
     * Actually changes the MVP matrix when using orthographic projection.
     *
     * @param gl Current OpenGL context
     * @param width Width of viewport
     * @param height Height of viewport
     * @throws NullPointerException if context is null
     * @throws GLException if context is unexpected version
     * @throws IllegalArgumentException if width or height is negative
     */
    protected abstract void doSetTransformOrtho(/*@Nonnull*/ GL gl,
                                                /*@Nonnegative*/ int width,
                                                /*@Nonnegative*/ int height);

    @Override
    public final float drawGlyph(/*@Nonnull*/ final GL gl,
                                 /*@Nonnull*/ final Glyph glyph,
                                 /*@CheckForSigned*/ final float x,
                                 /*@CheckForSigned*/ final float y,
                                 /*@CheckForSigned*/ final float z,
                                 /*@CheckForSigned*/ final float scale,
                                 /*@Nonnull*/ final TextureCoords coords) {

        // Compute position and size
        quad.xl = x + (scale * glyph.kerning);
        quad.xr = quad.xl + (scale * glyph.width);
        quad.yb = y - (scale * glyph.descent);
        quad.yt = quad.yb + (scale * glyph.height);
        quad.z = z;
        quad.sl = coords.left();
        quad.sr = coords.right();
        quad.tb = coords.bottom();
        quad.tt = coords.top();

        // Draw quad
        pipeline.addQuad(gl, quad);

        // Return distance to next character
        return glyph.advance;
    }

    @Override
    public final void endRendering(/*@Nonnull*/ final GL gl) {

        // Store text renderer state
        inRenderCycle = false;

        // Pass to quad renderer
        pipeline.endRendering(gl);

        // Perform hook
        doEndRendering(gl);
    }

    /**
     * Fires an event to all observers.
     *
     * @param type Kind of event
     * @throws NullPointerException if type is null
     */
    protected final void fireEvent(/*@Nonnull*/ final EventType type) {
        checkNotNull(type, "Event type cannot be null");
        for (final EventListener listener : listeners) {
            assert listener != null : "addListener rejects null";
            listener.onGlyphRendererEvent(type);
        }
    }

    @Override
    public final void flush(/*@Nonnull*/ final GL gl) {

        checkState(inRenderCycle, "Must be in render cycle");

        pipeline.flush(gl);
        gl.glFlush();
    }

    /**
     * Determines if a color is the same one that is stored.
     *
     * @param r Red component of color
     * @param g Green component of color
     * @param b Blue component of color
     * @param a Alpha component of color
     * @return True if each component matches
     */
    final boolean hasColor(final float r, final float g, final float b, final float a) {
        return (this.r == r) && (this.g == g) && (this.b == b) && (this.a == a);
    }

    // TODO: Rename to `isOrthographic`?
    /**
     * Checks if this {@link GlyphRenderer} using an orthographic projection.
     *
     * @return True if this renderer is using an orthographic projection
     */
    final boolean isOrthoMode() {
        return orthoMode;
    }

    @Override
    public final void onQuadPipelineEvent(/*@Nonnull*/ final QuadPipeline.EventType type) {
        checkNotNull(type, "Event type cannot be null");
        if (type == QuadPipeline.EventType.AUTOMATIC_FLUSH) {
            fireEvent(EventType.AUTOMATIC_FLUSH);
        }
    }

    @Override
    public final void setColor(final float r, final float g, final float b, final float a) {

        // Check if already has the color
        if (hasColor(r, g, b, a)) {
            return;
        }

        // Render any outstanding quads first
        if (!pipeline.isEmpty()) {
            fireEvent(EventType.AUTOMATIC_FLUSH);
            final GL gl = GLContext.getCurrentGL();
            flush(gl);
        }

        // Store the color
        this.r = r;
        this.g = g;
        this.b = g;
        this.a = a;

        // Change the color
        if (inRenderCycle) {
            final GL gl = GLContext.getCurrentGL();
            doSetColor(gl, r, g, b, a);
        } else {
            colorDirty = true;
        }
    }

    /**
     * Changes the quad pipeline.
     *
     * @param gl Current OpenGL context
     * @param pipeline Quad pipeline to change to
     * @throws NullPointerException if context or pipeline is null
     */
    private final void setPipeline(/*@Nonnull*/ final GL gl,
                                   /*@Nonnull*/ final QuadPipeline pipeline) {

        assert gl != null;
        assert pipeline != null;

        final QuadPipeline oldPipeline = this.pipeline;
        final QuadPipeline newPipeline = pipeline;

        // Remove the old pipeline
        if (oldPipeline != null) {
            oldPipeline.removeListener(this);
            oldPipeline.dispose(gl);
            this.pipeline = null;
        }

        // Store the new pipeline
        newPipeline.addListener(this);
        this.pipeline = newPipeline;
        pipelineDirty = false;
    }

    @Override
    public final void setTransform(/*@Nonnull*/ final float[] value, final boolean transpose) {

        // Check if in wrong mode
        if (orthoMode) {
            throw new IllegalStateException("Must be in 3D mode!");
        }

        // Render any outstanding quads first
        if (!pipeline.isEmpty()) {
            fireEvent(EventType.AUTOMATIC_FLUSH);
            final GL gl = GLContext.getCurrentGL();
            flush(gl);
        }

        // Store the transform
        System.arraycopy(value, 0, this.transform, 0, value.length);
        this.transposed = transpose;

        // Change the transform
        if (inRenderCycle) {
            final GL gl = GLContext.getCurrentGL();
            doSetTransform3d(gl, value, transpose);
        } else {
            transformDirty = true;
        }
    }
}


// TODO: Rename to `GlyphRenderers`?
/**
 * Utility for creating {@link GlyphRenderer} instances.
 */
/*@ThreadSafe*/
final class GlyphRendererFactory {

    /**
     * Prevents instantiation.
     */
    private GlyphRendererFactory() {
        // pass
    }

    /**
     * Creates a {@link GlyphRenderer} based on the current OpenGL context.
     *
     * @param gl Current OpenGL context
     * @return New glyph renderer for the given context, not null
     * @throws NullPointerException if context is null
     * @throws UnsupportedOperationException if GL is unsupported
     */
    /*@Nonnull*/
    static GlyphRenderer createGlyphRenderer(/*@Nonnull*/ final GL gl) {

        final GLProfile profile = gl.getGLProfile();

        if (profile.isGL3()) {
            return new GlyphRendererGL3(gl.getGL3());
        } else if (profile.isGL2()) {
            return new GlyphRendererGL2();
        } else {
            throw new UnsupportedOperationException("Profile currently unsupported");
        }
    }
}


/**
 * {@link GlyphRenderer} for use with OpenGL 2.
 */
/*@NotThreadSafe*/
final class GlyphRendererGL2 extends AbstractGlyphRenderer {

    /**
     * True if using vertex arrays.
     */
    private boolean useVertexArrays = true;

    /**
     * Constructs a {@link GlyphRendererGL2}.
     */
    GlyphRendererGL2() {
        // empty
    }

    @Override
    protected void doBeginRendering(/*@Nonnull*/ final GL gl,
                                    final boolean ortho,
                                    /*@Nonnull*/ final int width,
                                    /*@Nonnull*/ final int height,
                                    final boolean disableDepthTest) {

        final GL2 gl2 = gl.getGL2();

        // Change general settings
        gl2.glPushAttrib(getAttribMask(ortho));
        gl2.glDisable(GL2.GL_LIGHTING);
        gl2.glEnable(GL2.GL_BLEND);
        gl2.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl2.glEnable(GL2.GL_TEXTURE_2D);
        gl2.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);

        // Set up transformations
        if (ortho) {
            if (disableDepthTest) {
                gl2.glDisable(GL2.GL_DEPTH_TEST);
            }
            gl2.glDisable(GL2.GL_CULL_FACE);
            gl2.glMatrixMode(GL2.GL_PROJECTION);
            gl2.glPushMatrix();
            gl2.glLoadIdentity();
            gl2.glOrtho(0, width, 0, height, -1, +1);
            gl2.glMatrixMode(GL2.GL_MODELVIEW);
            gl2.glPushMatrix();
            gl2.glLoadIdentity();
            gl2.glMatrixMode(GL2.GL_TEXTURE);
            gl2.glPushMatrix();
            gl2.glLoadIdentity();
        }
    }

    /*@Nonnull*/
    protected QuadPipeline doCreateQuadPipeline(/*@Nonnull*/ final GL gl) {

        final GL2 gl2 = gl.getGL2();

        if (useVertexArrays) {
            if (gl2.isExtensionAvailable(GLExtensions.VERSION_1_5)) {
                return new QuadPipelineGL15(gl2);
            } else if (gl2.isExtensionAvailable("GL_VERSION_1_1")) {
                return new QuadPipelineGL11();
            } else {
                return new QuadPipelineGL10();
            }
        } else {
            return new QuadPipelineGL10();
        }
    }

    protected void doDispose(/*@Nonnull*/ final GL gl) {
        // empty
    }

    @Override
    protected void doEndRendering(/*@Nonnull*/ final GL gl) {

        final GL2 gl2 = gl.getGL2();

        // Reset transformations
        if (isOrthoMode()) {
            gl2.glMatrixMode(GL2.GL_PROJECTION);
            gl2.glPopMatrix();
            gl2.glMatrixMode(GL2.GL_MODELVIEW);
            gl2.glPopMatrix();
            gl2.glMatrixMode(GL2.GL_TEXTURE);
            gl2.glPopMatrix();
        }

        // Reset general settings
        gl2.glPopAttrib();
    }

    @Override
    protected void doSetColor(/*@Nonnull*/ final GL gl,
                              final float r,
                              final float g,
                              final float b,
                              final float a) {

        final GL2 gl2 = gl.getGL2();

        gl2.glColor4f(r, g, b, a);
    }

    @Override
    protected void doSetTransform3d(/*@Nonnull*/ final GL gl,
                                    /*@Nonnull*/ final float[] value,
                                    final boolean transpose) {
        // FIXME: Could implement this...
        throw new UnsupportedOperationException("Use standard GL instead");
    }

    @Override
    protected void doSetTransformOrtho(/*@Nonnull*/ final GL gl,
                                       /*@Nonnegative*/ final int width,
                                       /*@Nonnegative*/ final int height) {

        final GL2 gl2 = gl.getGL2();

        gl2.glMatrixMode(GL2.GL_PROJECTION);
        gl2.glPushMatrix();
        gl2.glLoadIdentity();
        gl2.glOrtho(0, width, 0, height, -1, +1);
        gl2.glMatrixMode(GL2.GL_MODELVIEW);
        gl2.glPushMatrix();
        gl2.glLoadIdentity();
    }

    /**
     * Returns attribute bits for {@code glPushAttrib} calls.
     *
     * @param ortho True if using orthographic projection
     * @return Attribute bits for {@code glPushAttrib} calls
     */
    private static int getAttribMask(final boolean ortho) {
        return GL2.GL_ENABLE_BIT |
               GL2.GL_TEXTURE_BIT |
               GL2.GL_COLOR_BUFFER_BIT |
               (ortho ? (GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_TRANSFORM_BIT) : 0);
    }

    @Override
    public boolean getUseVertexArrays() {
        return useVertexArrays;
    }

    @Override
    public void setUseVertexArrays(final boolean useVertexArrays) {
        if (useVertexArrays != this.useVertexArrays) {
            dirtyPipeline();
            this.useVertexArrays = useVertexArrays;
        }
    }
}


/**
 * Utility for drawing glyphs with OpenGL 3.
 */
/*@NotThreadSafe*/
final class GlyphRendererGL3 extends AbstractGlyphRenderer {

    /**
     * Source code of vertex shader.
     */
    /*@Nonnull*/
    private static final String VERT_SOURCE =
        "#version 130\n" +
        "uniform mat4 MVPMatrix;\n" +
        "in vec4 MCVertex;\n" +
        "in vec2 TexCoord0;\n" +
        "out vec2 Coord0;\n" +
        "out vec4 gl_Position;\n" +
        "void main() {\n" +
        "   gl_Position = MVPMatrix * MCVertex;\n" +
        "   Coord0 = TexCoord0;\n" +
        "}\n";

    /**
     * Source code of fragment shader.
     */
    /*@Nonnull*/
    private static final String FRAG_SOURCE =
        "#version 130\n" +
        "uniform sampler2D Texture;\n" +
        "uniform vec4 Color=vec4(1,1,1,1);\n" +
        "in vec2 Coord0;\n" +
        "out vec4 FragColor;\n" +
        "void main() {\n" +
        "   float sample;\n" +
        "   sample = texture(Texture,Coord0).r;\n" +
        "   FragColor = Color * sample;\n" +
        "}\n";

    /**
     * True if blending needs to be reset.
     */
    private boolean restoreBlending;

    /**
     * True if depth test needs to be reset.
     */
    private boolean restoreDepthTest;

    /**
     * Shader program.
     */
    /*@Nonnegative*/
    private final int program;

    /**
     * Uniform for modelview projection.
     */
    /*@Nonnull*/
    private final Mat4Uniform transform;

    /**
     * Uniform for color of glyphs.
     */
    /*@Nonnull*/
    private final Vec4Uniform color;

    /**
     * Width of last orthographic render.
     */
    /*@Nonnegative*/
    private int lastWidth = 0;

    /**
     * Height of last orthographic render
     */
    /*@Nonnegative*/
    private int lastHeight = 0;

    /**
     * Constructs a {@link GlyphRendererGL3}.
     *
     * @param gl3 Current OpenGL context
     * @throws NullPointerException if context is null
     */
    GlyphRendererGL3(/*@Nonnull*/ final GL3 gl3) {
        this.program = ShaderLoader.loadProgram(gl3, VERT_SOURCE, FRAG_SOURCE);
        this.transform = new Mat4Uniform(gl3, program, "MVPMatrix");
        this.color = new Vec4Uniform(gl3, program, "Color");
    }

    @Override
    protected void doBeginRendering(/*@Nonnull*/ final GL gl,
                                    final boolean ortho,
                                    /*@Nonnegative*/ final int width,
                                    /*@Nonnegative*/ final int height,
                                    final boolean disableDepthTest) {

        final GL3 gl3 = gl.getGL3();

        // Activate program
        gl3.glUseProgram(program);

        // Check blending and depth test
        restoreBlending = false;
        if (!gl3.glIsEnabled(GL.GL_BLEND)) {
            gl3.glEnable(GL.GL_BLEND);
            gl3.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
            restoreBlending = true;
        }
        restoreDepthTest = false;
        if (disableDepthTest && gl3.glIsEnabled(GL.GL_DEPTH_TEST)) {
            gl3.glDisable(GL.GL_DEPTH_TEST);
            restoreDepthTest = true;
        }

        // Check transform
        if (ortho) {
            doSetTransformOrtho(gl, width, height);
        }
    }

    @Override
    protected QuadPipeline doCreateQuadPipeline(/*@Nonnull*/ final GL gl) {
        final GL3 gl3 = gl.getGL3();
        return new QuadPipelineGL30(gl3, program);
    }

    protected void doDispose(/*@Nonnull*/ final GL gl) {

        final GL3 gl3 = gl.getGL3();

        gl3.glUseProgram(0);
        gl3.glDeleteProgram(program);
    }

    @Override
    protected void doEndRendering(/*@Nonnull*/ final GL gl) {

        final GL3 gl3 = gl.getGL3();

        // Deactivate program
        gl3.glUseProgram(0);

        // Check blending and depth test
        if (restoreBlending) {
            gl3.glDisable(GL.GL_BLEND);
        }
        if (restoreDepthTest) {
            gl3.glEnable(GL.GL_DEPTH_TEST);
        }
    }

    @Override
    protected void doSetColor(/*@Nonnull*/ final GL gl,
                              final float r,
                              final float g,
                              final float b,
                              final float a) {

        final GL3 gl3 = gl.getGL3();

        color.value[0] = r;
        color.value[1] = g;
        color.value[2] = b;
        color.value[3] = a;
        color.update(gl3);
    }

    @Override
    protected void doSetTransform3d(/*@Nonnull*/ final GL gl,
                                    /*@Nonnull*/ final float[] value,
                                    final boolean transpose) {

        final GL3 gl3 = gl.getGL3();

        gl3.glUniformMatrix4fv(transform.location, 1, transpose, value, 0);
        transform.dirty = true;
    }

    @Override
    protected void doSetTransformOrtho(/*@Nonnull*/ final GL gl,
                                       /*@Nonnegative*/ final int width,
                                       /*@Nonnegative*/ final int height) {

        final GL3 gl3 = gl.getGL3();

        // Recompute if width and height changed
        if (width != lastWidth || height != lastHeight) {
            Projection.orthographic(transform.value, width, height);
            transform.transpose = true;
            transform.dirty = true;
            lastWidth = width;
            lastHeight = height;
        }

        // Upload if made dirty anywhere
        if (transform.dirty) {
            transform.update(gl3);
            transform.dirty = false;
        }
    }

    @Override
    public boolean getUseVertexArrays() {
        return true;
    }

    @Override
    public void setUseVertexArrays(final boolean useVertexArrays) {
        // empty
    }
}


/**
 * Utility for computing projections.
 */
/*@NotThreadSafe*/
final class Projection {

    /**
     * Prevents instantiation.
     */
    private Projection() {
        // empty
    }

    /**
     * Computes an orthographic projection matrix.
     *
     * @param v Computed matrix values, in row-major order
     * @param width Width of current OpenGL viewport
     * @param height Height of current OpenGL viewport
     */
    static void orthographic(/*@Nonnull*/ final float[] v,
                             /*@Nonnegative*/ final int width,
                             /*@Nonnegative*/ final int height) {

        // Zero out
        for (int i = 0; i < 16; ++i) {
            v[i] = 0;
        }

        // Translate to origin
        v[3] = -1;
        v[7] = -1;

        // Scale to unit cube
        v[0] = 2f / width;
        v[5] = 2f / height;
        v[10] = -1;
        v[15] = 1;
    }
}


/**
 * Uniform variable in a shader.
 */
abstract class Uniform {

    /**
     * Index of uniform in shader.
     */
    /*@Nonnegative*/
    final int location;

    /**
     * True if local value should be pushed.
     */
    boolean dirty;

    /**
     * Constructs a uniform.
     *
     * @param gl2gl3 Current OpenGL context
     * @param program OpenGL handle to shader program
     * @param name Name of the uniform in shader source code
     * @throws NullPointerException if context is null
     */
    Uniform(/*@Nonnull*/ final GL2GL3 gl2gl3,
            /*@Nonnegative*/ final int program,
            /*@Nonnull*/ final String name) {
        location = gl2gl3.glGetUniformLocation(program, name);
        if (location == -1) {
            throw new RuntimeException("Could not find uniform in program.");
        }
    }

    /**
     * Pushes the local value to the shader program.
     *
     * @param gl Current OpenGL context
     */
    abstract void update(/*@Nonnull*/ GL2GL3 gl);
}


/**
 * Uniform for a {@code mat4}.
 */
/*@NotThreadSafe*/
final class Mat4Uniform extends Uniform {

    /**
     * Local copy of matrix values.
     */
    final float[] value = new float[16];

    /**
     * True if matrix is stored in row-major order.
     */
    boolean transpose;

    /**
     * Constructs a {@link UniformMatrix}.
     *
     * @param gl Current OpenGL context
     * @param program OpenGL handle to shader program
     * @param name Name of the uniform in shader source code
     * @throws NullPointerException if context is null
     */
    Mat4Uniform(/*@Nonnull*/ final GL2GL3 gl,
                /*@Nonnegative*/ final int program,
                /*@Nonnull*/ final String name) {
        super(gl2gl3, program, name);
    }

    @Override
    void update(/*@Nonnull*/ final GL2GL3 gl) {
        gl.glUniformMatrix4fv(location, 1, transpose, value, 0);
    }
}


/**
 * Uniform for a {@code vec4}.
 */
/*@NotThreadSafe*/
final class Vec4Uniform extends Uniform {

    /**
     * Local copy of vector values.
     */
    /*@Nonnull*/
    final float[] value = new float[4];

    /**
     * Constructs a uniform vector.
     *
     * @param gl2gl3 Current OpenGL context
     * @param program OpenGL handle to shader program
     * @param name Name of the uniform in shader source code
     * @throws NullPointerException if context is null
     */
    Vec4Uniform(/*@Nonnull*/ final GL2GL3 gl,
                /*@Nonnegative*/ final int program,
                /*@Nonnull*/ final String name) {
        super(gl, program, name);
    }

    @Override
    void update(/*@Nonnull*/ final GL2GL3 gl) {
        gl.glUniform4fv(location, 1, value, 0);
    }
}
