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
     * Adds a listener that will be notified of events.
     *
     * @param listener Object that wants to be notified of events
     * @throws AssertionError if listener is <tt>null</tt>
     */
    void addListener(EventListener listener);

    /**
     * Starts a render cycle.
     *
     * @param gl Current OpenGL context
     * @param ortho <tt>true</tt> if using orthographic projection
     * @param width Width of current OpenGL viewport
     * @param height Height of current OpenGL viewport
     * @param disableDepthTest <tt>true</tt> if should ignore depth values
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     * @throws AssertionError if width or height is negative
     */
    void beginRendering(GL gl, boolean ortho, int width, int height, boolean disableDepthTest);

    /**
     * Frees resources used by the renderer.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     */
    void dispose(GL gl);

    /**
     * Draws a glyph.
     *
     * @param gl Current OpenGL context
     * @param glyph Visual representation of a character
     * @param x Position to draw on X axis
     * @param y Position to draw on Y axis
     * @param z Position to draw on Z axis
     * @param scale Relative size of glyph
     * @param coords Texture coordinates of glyph
     * @return Distance to next character
     * @throws NullPointerException if context, glyph, or texture coordinate is <tt>null</tt>
     * @throws GLException if context is unexpected version
     */
    float drawGlyph(GL gl, Glyph glyph, float x, float y, float z, float scale, TextureCoords coords);

    /**
     * Finishes a render cycle.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     */
    void endRendering(GL gl);

    /**
     * Forces all stored text to be rendered.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     * @throws IllegalStateException if not in a render cycle
     */
    void flush(GL gl);

    //-----------------------------------------------------------------
    // Getters and setters
    //

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
     * @param transpose <tt>true</tt> if in row-major order
     * @throws IndexOutOfBoundsException if value's length is less than sixteen
     * @throws IllegalStateException if in orthographic mode
     */
    void setTransform(float[] value, boolean transpose);

    /**
     * Returns <tt>true</tt> if vertex arrays are in use.
     */
    boolean getUseVertexArrays();

    /**
     * Changes whether vertex arrays are in use.
     *
     * @param useVertexArrays <tt>true</tt> to use vertex arrays
     */
    void setUseVertexArrays(boolean useVertexArrays);

    //-----------------------------------------------------------------
    // Nested classes
    //

    /**
     * <i>Observer</i> of glyph renderer events.
     */
    static interface EventListener {

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
         * Renderer is automatically flushing queued glyphs, e.g. when it's full or color is changed.
         */
        AUTOMATIC_FLUSH
    };
}


/**
 * Abstract utility for drawing glyphs.
 */
abstract class AbstractGlyphRenderer implements GlyphRenderer, QuadPipeline.EventListener {

    // Default color
    private static float DEFAULT_RED = 1.0f;
    private static float DEFAULT_GREEN = 1.0f;
    private static float DEFAULT_BLUE = 1.0f;
    private static float DEFAULT_ALPHA = 1.0f;

    // Listeners to send events to
    private final List<EventListener> listeners;

    // Quad to send to pipeline
    private final Quad quad;

    // Buffer of quads
    private QuadPipeline pipeline;
    private boolean pipelineDirty;

    // True if between begin and end calls
    private boolean inRenderCycle;

    // True if orthographic
    private boolean orthoMode;

    // Components of color, and whether it needs to be updated
    private float r;
    private float g;
    private float b;
    private float a;
    private boolean colorDirty;

    // Transformation matrix for 3D mode, and whether it needs to be updated
    private final float[] transform;
    private boolean transposed;
    private boolean transformDirty;

    /**
     * Constructs an abstract glyph renderer.
     */
    AbstractGlyphRenderer() {
        this.listeners = new ArrayList<EventListener>();
        this.quad = new Quad();
        this.pipeline = null;
        this.pipelineDirty = true;
        this.inRenderCycle = false;
        this.orthoMode = false;
        this.r = DEFAULT_RED;
        this.g = DEFAULT_GREEN;
        this.b = DEFAULT_BLUE;
        this.a = DEFAULT_ALPHA;
        this.colorDirty = true;
        this.transform = new float[16];
        this.transposed = false;
        this.transformDirty = false;
    }

    /**
     * {@inheritDoc}
     *
     * @throws AssertionError {@inheritDoc}
     */
    @Override
    public final void addListener(final EventListener listener) {
        assert (listener != null);
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@inheritDoc}
     * @throws GLException {@inheritDoc}
     * @throws AssertionError {@inheritDoc}
     */
    @Override
    public final void beginRendering(final GL gl,
                                     final boolean ortho,
                                     final int width, final int height,
                                     final boolean disableDepthTest) {

        assert (width >= 0);
        assert (height >= 0);

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

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public final void dispose(final GL gl) {
        doDispose(gl);
        listeners.clear();
        pipeline.dispose(gl);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public final float drawGlyph(final GL gl,
                                 final Glyph glyph,
                                 final float x, final float y, final float z,
                                 final float scale, final TextureCoords coords) {

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

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public final void endRendering(final GL gl) {

        // Store text renderer state
        inRenderCycle = false;

        // Pass to quad renderer
        pipeline.endRendering(gl);

        // Perform hook
        doEndRendering(gl);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     * @throws IllegalStateException {@inheritDoc}
     */
    @Override
    public final void flush(final GL gl) {

        if (!inRenderCycle) {
            throw new IllegalStateException("Not in render cycle!");
        }

        pipeline.flush(gl);
        gl.glFlush();
    }

    /**
     * {@inheritDoc}
     *
     * @throws AssertionError {@inheritDoc}
     */
    @Override
    public final void onQuadPipelineEvent(final QuadPipeline.EventType type) {
        assert (type != null);
        if (type == QuadPipeline.EventType.AUTOMATIC_FLUSH) {
            fireEvent(EventType.AUTOMATIC_FLUSH);
        }
    }

    //------------------------------------------------------------------
    // Getters and setters
    //

    /**
     * Determines if a color is the same one that is stored.
     *
     * @param r Red component of color
     * @param g Green component of color
     * @param b Blue component of color
     * @param a Alpha component of color
     * @return <tt>true</tt> if each component matches
     */
    final boolean hasColor(final float r, final float g, final float b, final float a) {
        return (this.r == r) && (this.g == g) && (this.b == b) && (this.a == a);
    }

    /**
     * {@inheritDoc}
     */
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
     * Returns <tt>true</tt> if using orthographic projection.
     */
    final boolean isOrthoMode() {
        return orthoMode;
    }

    /**
     * Requests that the pipeline be replaced on the next call to <i>beginRendering</i>.
     */
    protected final void dirtyPipeline() {
        pipelineDirty = true;
    }

    /**
     * Changes the quad pipeline.
     *
     * @param gl Current OpenGL context
     * @param pipeline Quad pipeline to change to
     * @throws NullPointerException if context or pipeline is <tt>null</tt>
     */
    private final void setPipeline(final GL gl, final QuadPipeline pipeline) {

        assert (gl != null);
        assert (pipeline != null);

        // Remove the old pipeline
        if (this.pipeline != null) {
            this.pipeline.removeListener(this);
            this.pipeline.dispose(gl);
            this.pipeline = null;
        }

        // Store the new pipeline
        this.pipeline = pipeline;
        this.pipeline.addListener(this);
        this.pipelineDirty = false;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public final void setTransform(final float[] value, final boolean transpose) {

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

    //------------------------------------------------------------------
    // Helpers
    //

    /**
     * Fires an event to all observers.
     *
     * @param type Kind of event
     * @throws AssertionError if type is <tt>null</tt>
     */
    protected final void fireEvent(final EventType type) {
        assert (type != null);
        for (final EventListener listener : listeners) {
            listener.onGlyphRendererEvent(type);
        }
    }

    //-----------------------------------------------------------------
    // Hooks
    //

    /**
     * Actually starts a render cycle.
     *
     * @param gl Current OpenGL context
     * @param ortho <tt>true</tt> if using orthographic projection
     * @param width Width of current OpenGL viewport
     * @param height Height of current OpenGL viewport
     * @param disableDepthTest <tt>true</tt> if should ignore depth values
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     * @throws AssertionError if width or height is negative
     */
    protected abstract void doBeginRendering(final GL gl,
                                             final boolean ortho,
                                             final int width, final int height,
                                             final boolean disableDepthTest);

    /**
     * Actually creates the quad pipeline for rendering quads.
     *
     * @param gl Current OpenGL context
     * @return Quad pipeline to render quads with
     * @throws NullPointerException if context is <tt>null</tt>
     */
    protected abstract QuadPipeline doCreateQuadPipeline(final GL gl);

    /**
     * Actually frees resources used by the renderer.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     */
    protected abstract void doDispose(final GL gl);

    /**
     * Actually finishes a render cycle.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     */
    protected abstract void doEndRendering(final GL gl);

    /**
     * Actually changes color when user calls {@link #setColor}.
     *
     * @param gl Current OpenGL context
     * @param r Red component of color
     * @param g Green component of color
     * @param b Blue component of color
     * @param a Alpha component of color
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     */
    protected abstract void doSetColor(final GL gl, float r, float g, float b, float a);

    /**
     * Actually changes MVP matrix when user calls {@link #setMVPMatrix}.
     *
     * @param gl Current OpenGL context
     * @param value Matrix as float array
     * @param transpose <tt>True</tt> if in row-major order
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     * @throws IndexOutOfBoundsException if length of value is less than sixteen
     */
    protected abstract void doSetTransform3d(GL gl, float[] value, boolean transpose);

    /**
     * Actually changes MVP matrix when using orthographic projection.
     *
     * @param gl Current OpenGL context
     * @param width Width of viewport
     * @param height Height of viewport
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     */
    protected abstract void doSetTransformOrtho(GL gl, int width, int height);
}


/**
 * Utility for creating glyph renderers.
 */
class GlyphRendererFactory {

    /**
     * Creates a glyph renderer based on the current OpenGL context.
     *
     * @param gl Current OpenGL context
     * @return {@link GlyphRendererGL2} or {@link GlyphRendererGL3}
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws UnsupportedOperationException if GL is unsupported
     */
    static GlyphRenderer createGlyphRenderer(final GL gl) {

        final GLProfile profile = gl.getGLProfile();

        if (profile.isGL3()) {
            final GL3 gl3 = gl.getGL3();
            return new GlyphRendererGL3(gl3);
        } else if (profile.isGL2()) {
            final GL2 gl2 = gl.getGL2();
            return new GlyphRendererGL2(gl2);
        } else {
            throw new UnsupportedOperationException("Profile currently unsupported!");
        }
    }

    /**
     * Prevents instantiation.
     */
    private GlyphRendererFactory() {
        // pass
    }
}


/**
 * Utility for drawing glyphs with OpenGL 2.
 */
final class GlyphRendererGL2 extends AbstractGlyphRenderer {

    // True if using vertex arrays
    private boolean useVertexArrays;

    /**
     * Constructs a glyph renderer for OpenGL 2.
     *
     * @param gl2 Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     */
    GlyphRendererGL2(final GL2 gl2) {
        useVertexArrays = true;
    }

    //-----------------------------------------------------------------
    // Hooks
    //

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    protected void doBeginRendering(final GL gl,
                                    final boolean ortho,
                                    final int width, final int height,
                                    final boolean disableDepthTest) {

        // Get an OpenGL 2 context
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

    /**
     * Creates a quad pipeline for the current OpenGL context.
     *
     * @param gl Current OpenGL context
     * @return Correct quad pipeline for version of OpenGL in use
     * @throws NullPointerException if context is <tt>null</tt>
     */
    protected QuadPipeline doCreateQuadPipeline(final GL gl) {
        final GL2 gl2 = gl.getGL2();
        if (useVertexArrays) {
            if (gl2.isExtensionAvailable(GLExtensions.VERSION_1_5)) {
                return new QuadPipelineGL15(gl2);
            } else if (gl2.isExtensionAvailable("GL_VERSION_1_1")) {
                return new QuadPipelineGL11(gl2);
            } else {
                return new QuadPipelineGL10(gl2);
            }
        } else {
            return new QuadPipelineGL10(gl2);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    protected void doDispose(final GL gl) {
        // pass
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    protected void doEndRendering(final GL gl) {

        // Get an OpenGL 2 context
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

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    protected void doSetColor(final GL gl, final float r, final float g, final float b, final float a) {
        final GL2 gl2 = gl.getGL2();
        gl2.glColor4f(r, g, b, a);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    protected void doSetTransform3d(final GL gl, final float[] value, final boolean transpose) {
        // FIXME: Could implement this...
        throw new UnsupportedOperationException("Use standard GL instead.");
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    protected void doSetTransformOrtho(final GL gl, final int width, final int height) {

        final GL2 gl2 = gl.getGL2();

        gl2.glMatrixMode(GL2.GL_PROJECTION);
        gl2.glPushMatrix();
        gl2.glLoadIdentity();
        gl2.glOrtho(0, width, 0, height, -1, +1);
        gl2.glMatrixMode(GL2.GL_MODELVIEW);
        gl2.glPushMatrix();
        gl2.glLoadIdentity();
    }

    //-----------------------------------------------------------------
    // Helpers
    //

    /**
     * Returns attribute bits for <i>glPushAttrib</i> calls.
     *
     * @param ortho <tt>true</tt> if using orthographic projection
     */
    private static int getAttribMask(final boolean ortho) {
        return GL2.GL_ENABLE_BIT |
               GL2.GL_TEXTURE_BIT |
               GL2.GL_COLOR_BUFFER_BIT |
               (ortho ? (GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_TRANSFORM_BIT) : 0);
    }

    //-----------------------------------------------------------------
    // Getters and setters
    //

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getUseVertexArrays() {
        return useVertexArrays;
    }

    /**
     * {@inheritDoc}
     */
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
final class GlyphRendererGL3 extends AbstractGlyphRenderer {

    // Source code of fragment shader source
    private static String FRAG_SOURCE;

    // Source code of vertex shader source
    private static String VERT_SOURCE;

    // True if blending needs to be reset
    private boolean restoreBlending;

    // True if depth test needs to be reset
    private boolean restoreDepthTest;

    // Shader program
    private final int program;

    // Width of last orthographic render
    private int lastWidth;

    // Height of last orthographic render
    private int lastHeight;

    // Uniform for modelview projection
    private final UniformMatrix transform;

    // Uniform for color of glyphs
    private final UniformVector color;

    /**
     * Initializes static fields.
     */
    static {
        VERT_SOURCE =
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
        FRAG_SOURCE =
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
    }

    /**
     * Constructs a glyph renderer for OpenGL 3.
     */
    GlyphRendererGL3(final GL3 gl3) {
        this.program = ShaderLoader.loadProgram(gl3, VERT_SOURCE, FRAG_SOURCE);
        this.transform = new UniformMatrix(gl3, program, "MVPMatrix");
        this.color = new UniformVector(gl3, program, "Color");
    }

    //-----------------------------------------------------------------
    // Hooks
    //

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    protected void doBeginRendering(final GL gl,
                                    final boolean ortho,
                                    final int width, final int height,
                                    final boolean disableDepthTest) {

        // Get an OpenGL 3 profile
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
    protected QuadPipeline doCreateQuadPipeline(final GL gl) {
        final GL3 gl3 = gl.getGL3();
        return new QuadPipelineGL30(gl3, program);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    protected void doDispose(final GL gl) {

        // Get an OpenGL 3 context
        final GL3 gl3 = gl.getGL3();

        gl3.glUseProgram(0);
        gl3.glDeleteProgram(program);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    protected void doEndRendering(final GL gl) {

        // Get an OpenGL 3 context
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

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    protected void doSetColor(final GL gl, final float r, final float g, final float b, final float a) {
        final GL3 gl3 = gl.getGL3();
        color.value[0] = r;
        color.value[1] = g;
        color.value[2] = b;
        color.value[3] = a;
        color.update(gl3);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    protected void doSetTransform3d(final GL gl, final float[] value, final boolean transpose) {
        final GL3 gl3 = gl.getGL3();
        gl3.glUniformMatrix4fv(transform.location, 1, transpose, value, 0);
        transform.dirty = true;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    protected void doSetTransformOrtho(final GL gl, final int width, final int height) {

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

    //------------------------------------------------------------------
    // Getters
    //

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getUseVertexArrays() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUseVertexArrays(final boolean useVertexArrays) {
        // pass
    }
}


/**
 * Utility for computing projections.
 */
class Projection {

    /**
     * Computes an orthographic projection matrix.
     *
     * @param v Computed matrix values, in row-major order
     * @param width Width of current OpenGL viewport
     * @param height Height of current OpenGL viewport
     */
    static void orthographic(final float[] v, final int width, final int height) {

        // Zero out
        for (int i=0; i<16; ++i) {
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
 * Uniform base class.
 */
abstract class Uniform {

    // Index of uniform in shader
    final int location;

    // True if local value should be pushed
    boolean dirty;

    /**
     * Constructs a uniform.
     *
     * @param gl2gl3 Current OpenGL context
     * @param program OpenGL handle to shader program
     * @param name Name of the uniform in shader source code
     * @throws NullPointerException if context is <tt>null</tt>
     */
    Uniform(final GL2GL3 gl2gl3, final int program, final String name) {
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
    abstract void update(GL2GL3 gl);
}


/**
 * Uniform for a <tt>mat4</tt>.
 */
final class UniformMatrix extends Uniform {

    // Local copy of matrix values
    final float[] value;

    // True if stored in row-major order
    boolean transpose;

    /**
     * Constructs a uniform matrix.
     *
     * @param gl2gl3 Current OpenGL context
     * @param program OpenGL handle to shader program
     * @param name Name of the uniform in shader source code
     * @throws NullPointerException if context is <tt>null</tt>
     */
    UniformMatrix(final GL2GL3 gl2gl3, final int program, final String name) {
        super(gl2gl3, program, name);
        value = new float[16];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void update(final GL2GL3 gl) {
        gl.glUniformMatrix4fv(location, 1, transpose, value, 0);
    }
}


/**
 * Uniform vec4.
 */
final class UniformVector extends Uniform {

    // Local copy of vector values
    float[] value;

    /**
     * Constructs a uniform vector.
     *
     * @param gl2gl3 Current OpenGL context
     * @param program OpenGL handle to shader program
     * @param name Name of the uniform in shader source code
     * @throws NullPointerException if context is <tt>null</tt>
     */
    UniformVector(final GL2GL3 gl2gl3,final int program, final String name) {
        super(gl2gl3, program, name);
        value = new float[4];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void update(final GL2GL3 gl) {
        gl.glUniform4fv(location, 1, value, 0);
    }
}
