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

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLProfile;

import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.List;


/**
 * Utility for drawing a stream of quads.
 */
interface QuadPipeline {

    /**
     * Adds an observer that will be notified of events.
     *
     * @param listener Listener to add
     * @throws AssertionError if listener is <tt>null</tt>
     */
    void addListener(EventListener listener);

    /**
     * Adds a quad to the pipeline.
     *
     * @param gl Current OpenGL context
     * @param quad Quad to add to pipeline
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     * @throws AssertionError if quad is <tt>null</tt>
     */
    void addQuad(GL gl, Quad quad);

    /**
     * Starts a render cycle.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     */
    void beginRendering(GL gl);

    /**
     * Frees resources used by the pipeline.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     */
    void dispose(GL gl);

    /**
     * Finishes a render cycle.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     */
    void endRendering(GL gl);

    /**
     * Draws all vertices in the pipeline.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     */
    void flush(GL gl);

    /**
     * Deregisters an observer that was previously registered.
     *
     * @param listener Listener to deregister
     * @throws AssertionError if listener is <tt>null</tt>
     */
    void removeListener(EventListener listener);

    //-----------------------------------------------------------------
    // Getters
    //

    /**
     * Returns number of quads in the pipeline.
     */
    int getSize();

    /**
     * Returns <tt>true</tt> if there is no data in pipeline.
     */
    boolean isEmpty();

    //-----------------------------------------------------------------
    // Nested classes
    //

    /**
     * <i>Observer</i> of quad pipeline.
     */
    static interface EventListener {

        /**
         * Responds to an event from a quad pipeline.
         *
         * @param type Type of event
         * @throws AssertionError if event type is <tt>null</tt>
         */
        void onQuadPipelineEvent(EventType type);
    }

    /**
     * Kind of event.
     */
    static enum EventType {

        /**
         * Pipeline is automatically flushing all queued quads, e.g. when it's full.
         */
        AUTOMATIC_FLUSH
    }

    /**
     * Structure for points and coordinates.
     */
    static class Quad {

        /** Position of left side */
        public float xl;

        /** Position of right side */
        public float xr;

        /** Position of bottom */
        public float yb;

        /** Position of top */
        public float yt;

        /** Depth */
        public float z;

        /** Left texture coordinate */
        public float sl;

        /** Right texture coordinate */
        public float sr;

        /** Bottom texture coordinate */
        public float tb;

        /** Top texture coordinate */
        public float tt;
    }
}


/**
 * Skeletal implementation of {@link QuadPipeline}.
 */
abstract class AbstractQuadPipeline implements QuadPipeline {

    // Number of bytes in one float
    static final int SIZEOF_FLOAT = 4;

    // Number of bytes in one int
    static final int SIZEOF_INT = 4;

    // Maximum number of quads in the buffer
    static final int QUADS_PER_BUFFER = 100;

    // Number of components in a point attribute
    static final int FLOATS_PER_POINT = 3;

    // Number of components in a texture coordinate attribute
    static final int FLOATS_PER_COORD = 2;

    // Total components in vertex
    static final int FLOATS_PER_VERT = FLOATS_PER_POINT + FLOATS_PER_COORD;

    // Size of a point attribute in bytes
    static final int BYTES_PER_POINT = FLOATS_PER_POINT * SIZEOF_FLOAT;

    // Size of a texture coordinate attribute in bytes
    static final int BYTES_PER_COORD = FLOATS_PER_COORD * SIZEOF_FLOAT;

    // Total size of a vertex in bytes
    static final int BYTES_PER_VERT = BYTES_PER_POINT + BYTES_PER_COORD;

    // Number of bytes before first point attribute in buffer
    static final int POINT_OFFSET = 0;

    // Number of bytes before first texture coordinate in buffer
    static final int COORD_OFFSET = BYTES_PER_POINT;

    // Number of bytes between successive values for the same attribute
    static final int STRIDE = BYTES_PER_POINT + BYTES_PER_COORD;

    // Maximum buffer size in floats
    final int FLOATS_PER_BUFFER;

    // Maximum buffer size in bytes
    final int BYTES_PER_BUFFER;

    // Number of vertices per primitive
    final int VERTS_PER_PRIM;

    // Maximum buffer size in primitives
    final int PRIMS_PER_BUFFER;

    // Maximum buffer size in vertices
    final int VERTS_PER_BUFFER;

    // Size of a quad in vertices
    final int VERTS_PER_QUAD;

    // Size of a quad in bytes
    final int BYTES_PER_QUAD;

    // Size of a quad in primitives
    final int PRIMS_PER_QUAD;

    // Observers of events
    private final List<EventListener> listeners;

    // Buffer of vertices
    private final FloatBuffer data;

    // Number of outstanding quads in the buffer
    private int size;

    /**
     * Constructs an abstract quad pipeline.
     *
     * @param vertsPerPrim Number of vertices per primitive
     * @param primsPerQuad Number of primitives per quad
     * @throws AssertionError if vertices per primitive or primitives per quad is less than one
     */
    AbstractQuadPipeline(final int vertsPerPrim, final int primsPerQuad) {

        assert (vertsPerPrim > 0);
        assert (primsPerQuad > 0);

        VERTS_PER_PRIM = vertsPerPrim;
        PRIMS_PER_QUAD = primsPerQuad;
        PRIMS_PER_BUFFER = primsPerQuad * QUADS_PER_BUFFER;
        VERTS_PER_QUAD = vertsPerPrim * primsPerQuad;
        VERTS_PER_BUFFER = PRIMS_PER_BUFFER * VERTS_PER_PRIM;
        FLOATS_PER_BUFFER = FLOATS_PER_VERT * VERTS_PER_BUFFER;
        BYTES_PER_BUFFER = BYTES_PER_VERT * VERTS_PER_BUFFER;
        BYTES_PER_QUAD = BYTES_PER_VERT * VERTS_PER_QUAD;

        this.listeners = new LinkedList<EventListener>();
        this.data = Buffers.newDirectFloatBuffer(FLOATS_PER_BUFFER);
        this.size = 0;
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
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     * @throws AssertionError {@inheritDoc}
     */
    @Override
    public final void addQuad(final GL gl, final Quad quad) {
        assert (quad != null);
        doAddQuad(quad);
        if (++size >= QUADS_PER_BUFFER) {
            fireEvent(EventType.AUTOMATIC_FLUSH);
            flush(gl);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public void beginRendering(final GL gl) {
        // pass
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    public void dispose(final GL gl) {
        listeners.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public void endRendering(final GL gl) {
        flush(gl);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public final void flush(final GL gl) {
        if (size > 0) {
            doFlush(gl);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws AssertionError {@inheritDoc}
     */
    @Override
    public final void removeListener(final EventListener listener) {
        assert (listener != null);
        listeners.remove(listener);
    }

    //-----------------------------------------------------------------
    // Hooks
    //

    /**
     * Actually adds vertices from a quad to the buffer.
     *
     * @param q Quad to add
     * @throws AssertionError if quad is <tt>null</tt>
     */
    protected void doAddQuad(final Quad q) {
        assert (q != null);
        addPoint(q.xr, q.yt, q.z);
        addCoord(q.sr, q.tt);
        addPoint(q.xl, q.yt, q.z);
        addCoord(q.sl, q.tt);
        addPoint(q.xl, q.yb, q.z);
        addCoord(q.sl, q.tb);
        addPoint(q.xr, q.yb, q.z);
        addCoord(q.sr, q.tb);
    }

    /**
     * Actually draws everything in the pipeline.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws GLException if context is unexpected version
     */
    protected abstract void doFlush(final GL gl);

    //------------------------------------------------------------------
    // Helpers
    //

    /**
     * Adds a texture coordinate to the pipeline.
     *
     * @param s Texture coordinate for X axis
     * @param t Texture coordinate for Y axis
     */
    protected final void addCoord(final float s, final float t) {
        data.put(s).put(t);
    }

    /**
     * Adds a point to the pipeline.
     *
     * @param x Position on X axis
     * @param y Position on Y axis
     * @param z Position on Z axis
     */
    protected final void addPoint(final float x, final float y, final float z) {
        data.put(x).put(y).put(z);
    }

    /**
     * Rewinds the buffer and resets the number of outstanding quads.
     */
    protected final void clear() {
        data.rewind();
        size = 0;
    }

    /**
     * Creates a vertex buffer object for use with a pipeline.
     *
     * @param gl2gl3 Current OpenGL context
     * @param size Size in bytes of buffer
     * @return OpenGL handle to vertex buffer object
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws AssertionError if size is negative
     */
    protected static int createVertexBufferObject(final GL2GL3 gl2gl3, final int size) {

        assert (size >= 0);

        // Generate
        final int[] handles = new int[1];
        gl2gl3.glGenBuffers(1, handles, 0);
        final int vbo = handles[0];

        // Allocate
        gl2gl3.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vbo);
        gl2gl3.glBufferData(
                GL2GL3.GL_ARRAY_BUFFER, // target
                size,                   // size
                null,                   // data
                GL2GL3.GL_STREAM_DRAW); // usage
        gl2gl3.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

        return vbo;
    }

    /**
     * Fires an event to all observers.
     *
     * @param type Type of event to send to observers
     * @throws AssertionError if type is <tt>null</tt>
     */
    protected final void fireEvent(final EventType type) {
        assert (type != null);
        for (final EventListener listener : listeners) {
            listener.onQuadPipelineEvent(type);
        }
    }

    /**
     * Changes the data's position.
     *
     * @param index Location in buffer to move to
     * @throws AssertionError if index is negative
     */
    protected final void position(final int index) {
        assert (index >= 0);
        data.position(index);
    }

    /**
     * Rewinds the data buffer.
     */
    protected final void rewind() {
        data.rewind();
    }

    //-----------------------------------------------------------------
    // Getters and setters
    //

    /**
     * Returns NIO buffer backing the pipeline.
     */
    protected final FloatBuffer getData() {
        return data;
    }

    /**
     * Returns next float in the pipeline.
     */
    protected final float getFloat() {
        return data.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getSize() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isEmpty() {
        return (size == 0);
    }

    /**
     * Returns size of vertices in the pipeline in bytes.
     */
    public final int getSizeInBytes() {
        return size * BYTES_PER_QUAD;
    }

    /**
     * Returns number of primitives in the pipeline.
     */
    public final int getSizeInPrimitives() {
        return size * PRIMS_PER_QUAD;
    }

    /**
     * Returns number of vertices in the pipeline.
     */
    public final int getSizeInVertices() {
        return size * VERTS_PER_QUAD;
    }
}


/**
 * Utility for creating quad pipelines.
 */
final class QuadPipelineFactory {

    /**
     * Creates a quad pipeline based on the current OpenGL context.
     *
     * @param gl Current OpenGL context
     * @param program Shader program to use, or <tt>0</tt> to use default
     * @return Correct quad pipeline for the version of OpenGL in use
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws UnsupportedOperationException if GL is unsupported
     * @throws AssertionError if shader program is negative
     */
    QuadPipeline createQuadPipeline(final GL gl, final int program) {

        assert (program >= 0);

        final GLProfile profile = gl.getGLProfile();

        if (profile.isGL3()) {
            final GL3 gl3 = gl.getGL3();
            return new QuadPipelineGL30(gl3, program);
        } else if (profile.isGL2()) {
            final GL2 gl2 = gl.getGL2();
            if (gl2.isExtensionAvailable("GL_VERSION_1_5")) {
                return new QuadPipelineGL15(gl2);
            } else if (gl2.isExtensionAvailable("GL_VERSION_1_1")) {
                return new QuadPipelineGL11(gl2);
            } else {
                return new QuadPipelineGL10(gl2);
            }
        } else {
            throw new UnsupportedOperationException("Profile currently unsupported!");
        }
    }

    /**
     * Prevents instantiation.
     */
    private QuadPipelineFactory() {
        // pass
    }
}


/**
 * Utility for drawing quads with <i>OpenGL 1.0</i>.
 */
final class QuadPipelineGL10 extends AbstractQuadPipeline {

    // Number of vertices per primitive
    private static final int VERTS_PER_PRIM = 4;

    // Number of primitives per quad
    private static final int PRIMS_PER_QUAD = 1;

    /**
     * Constructs a quad pipeline for OpenGL 1.0.
     *
     * @param gl2 Current OpenGL context
     */
    QuadPipelineGL10(final GL2 gl2) {
        super(VERTS_PER_PRIM, PRIMS_PER_QUAD);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    protected void doFlush(final GL gl) {

        // Get an OpenGL context
        final GL2 gl2 = gl.getGL2();

        gl2.glBegin(GL2.GL_QUADS);
        try {
            rewind();
            final int size = getSize();
            for (int q = 0; q < size; ++q) {
                for (int v = 0; v < VERTS_PER_QUAD; ++v) {
                    gl2.glVertex3f(getFloat(), getFloat(), getFloat());
                    gl2.glTexCoord2f(getFloat(), getFloat());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            gl2.glEnd();
            clear();
        }
    }
}


/**
 * Utility for drawing quads for <i>OpenGL 1.1</i>.
 */
final class QuadPipelineGL11 extends AbstractQuadPipeline {

    // Number of vertices per primitive
    private static final int VERTS_PER_PRIM = 4;

    // Number of primitives per quad
    private static final int PRIMS_PER_QUAD = 1;

    // Vertex array for points
    private final FloatBuffer pointsArray;

    // Vertex array for texture coordinates
    private final FloatBuffer coordsArray;

    /**
     * Constructs a quad pipeline for OpenGL 1.1.
     *
     * @param gl2 Current OpenGL context
     */
    QuadPipelineGL11(final GL2 gl2) {

        super(VERTS_PER_PRIM, PRIMS_PER_QUAD);

        pointsArray = createFloatBufferView(getData(), POINT_OFFSET);
        coordsArray = createFloatBufferView(getData(), COORD_OFFSET);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public void beginRendering(final GL gl) {

        super.beginRendering(gl);

        // Get an OpenGL 2 context
        final GL2 gl2 = gl.getGL2();

        // Push state
        gl2.glPushClientAttrib((int) GL2.GL_ALL_CLIENT_ATTRIB_BITS);

        // Points
        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl2.glVertexPointer(
                FLOATS_PER_POINT,   // size
                GL2.GL_FLOAT,       // type
                STRIDE,             // stride
                pointsArray);       // pointer

        // Coordinates
        gl2.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
        gl2.glTexCoordPointer(
                FLOATS_PER_COORD,   // size
                GL2.GL_FLOAT,       // type
                STRIDE,             // stride
                coordsArray);       // pointer
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    protected void doFlush(final GL gl) {

        // Get an OpenGL 2 context
        final GL2 gl2 = gl.getGL2();

        gl2.glDrawArrays(
                GL2.GL_QUADS,         // mode
                0,                    // first
                getSizeInVertices()); // count
        clear();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public void endRendering(final GL gl) {

        super.endRendering(gl);

        // Get an OpenGL 2 context
        final GL2 gl2 = gl.getGL2();

        // Pop state
        gl2.glPopClientAttrib();
    }

    //-----------------------------------------------------------------
    // Helpers
    //

    /**
     * Makes a view of a float buffer at a certain position.
     *
     * @param fb Original float buffer
     * @param position Index to start view at
     * @return Resulting float buffer
     * @throws AssertionError if float buffer is <tt>null</tt>, or position is negative
     */
    private static FloatBuffer createFloatBufferView(final FloatBuffer fb, final int position) {

        assert (fb != null);
        assert (position >= 0);

        // Store original position
        final int original = fb.position();

        // Make a view at desired position
        fb.position(position);
        final FloatBuffer view = fb.asReadOnlyBuffer();

        // Reset buffer to original position
        fb.position(original);

        return view;
    }
}


/**
 * Utility for drawing quads for <i>OpenGL 1.5</i>.
 */
final class QuadPipelineGL15 extends AbstractQuadPipeline {

    // Number of vertices per primitive
    private static final int VERTS_PER_PRIM = 4;

    // Number of primitives per quad
    private static final int PRIMS_PER_QUAD = 1;

    // OpenGL handle to vertex buffer
    private final int vbo;

    /**
     * Constructs a quad pipeline for OpenGL 1.5.
     *
     * @param gl2 Current OpenGL context
     * @throws NullPointerException if context is <tt>null</tt>
     */
    QuadPipelineGL15(final GL2 gl2) {

        super(VERTS_PER_PRIM, PRIMS_PER_QUAD);

        this.vbo = createVertexBufferObject(gl2, BYTES_PER_BUFFER);
    }

    /**
     * Starts a render cycle.
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public void beginRendering(final GL gl) {

        super.beginRendering(gl);

        // Get an OpenGL 2 context
        final GL2 gl2 = gl.getGL2();

        // Change state
        gl2.glPushClientAttrib((int) GL2.GL_ALL_CLIENT_ATTRIB_BITS);
        gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo);

        // Points
        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl2.glVertexPointer(
                FLOATS_PER_POINT,   // size
                GL2.GL_FLOAT,       // type
                STRIDE,             // stride
                POINT_OFFSET);      // offset

        // Coordinates
        gl2.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
        gl2.glTexCoordPointer(
                FLOATS_PER_COORD,   // size
                GL2.GL_FLOAT,       // type
                STRIDE,             // stride
                COORD_OFFSET);      // offset
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public void dispose(final GL gl) {

        super.dispose(gl);

        // Get an OpenGL 2 context
        final GL2 gl2 = gl.getGL2();

        // Delete the vertex buffer object
        final int[] handles = new int[] { vbo };
        gl2.glDeleteBuffers(1, handles, 0);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    protected void doFlush(final GL gl) {

        // Get an OpenGL 2 context
        final GL2 gl2 = gl.getGL2();

        // Upload data
        rewind();
        gl2.glBufferSubData(
                GL2.GL_ARRAY_BUFFER, // target
                0,                   // offset
                getSizeInBytes(),    // size
                getData());          // data

        // Draw
        gl2.glDrawArrays(
                GL2.GL_QUADS,         // mode
                0,                    // first
                getSizeInVertices()); // count
        clear();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public void endRendering(final GL gl) {

        super.endRendering(gl);

        // Get an OpenGL 2 context
        final GL2 gl2 = gl.getGL2();

        // Restore state
        gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        gl2.glPopClientAttrib();
    }
}


/**
 * Utility for drawing quads in <i>OpenGL 3</i>.
 *
 * <p><i>QuadPipelineGL30</i> draws quads using OpenGL 3 features.  It
 * uses a Vertex Buffer Object to store vertices in graphics memory and
 * a Vertex Array Object to quickly switch which vertex attributes are
 * enabled.
 *
 * <p>Since <tt>GL_QUAD</tt> has been deprecated in OpenGL 3, this
 * implementation uses two triangles to represent one quad.  An
 * alternative implementation using one <tt>GL_FAN</tt> per quad was
 * also tested, but proved slower in most cases.  Apparently the
 * penalty imposed by the extra work required by the driver outweighed
 * the benefit of transferring less vertices.
 */
final class QuadPipelineGL30 extends AbstractQuadPipeline {

    // Name of point attribute in shader program
    private static final String POINT_ATTRIB_NAME = "MCVertex";

    // Name of texture coordinate attribute in shader program
    private static final String COORD_ATTRIB_NAME = "TexCoord0";

    // Number of vertices per primitive
    private static final int VERTS_PER_PRIM = 3;

    // Number of primitives per quad
    private static final int PRIMS_PER_QUAD = 2;

    // Vertex Buffer Object with vertex data
    private final int vbo;

    // Vertex Array Object with vertex attribute state
    private final int vao;

    /**
     * Constructs a quad pipeline for OpenGL 3.
     *
     * @param gl3 Current OpenGL context
     * @param shaderProgram Shader program to render quads with
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws AssertionError if shader program is less than one
     */
    QuadPipelineGL30(final GL3 gl3, final int shaderProgram) {

        super(VERTS_PER_PRIM, PRIMS_PER_QUAD);

        assert (shaderProgram > 0);

        this.vbo = createVertexBufferObject(gl3, BYTES_PER_BUFFER);
        this.vao = createVertexArrayObject(gl3, shaderProgram, vbo);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public void beginRendering(final GL gl) {

        super.beginRendering(gl);

        // Get an OpenGL 3 context
        final GL3 gl3 = gl.getGL3();

        // Bind the VBO and VAO
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vbo);
        gl3.glBindVertexArray(vao);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public void dispose(final GL gl) {

        super.dispose(gl);

        // Get an OpenGL 3 context
        final GL3 gl3 = gl.getGL3();

        // Delete VBO and VAO
        final int[] handles = new int[1];
        handles[0] = vbo;
        gl3.glDeleteBuffers(1, handles, 0);
        handles[0] = vao;
        gl3.glDeleteVertexArrays(1, handles, 0);
    }

    /**
     * {@inheritDoc}
     *
     * @throws AssertionError {@inheritDoc}
     */
    @Override
    protected void doAddQuad(final Quad q) {

        assert (q != null);

        // Add upper-left triangle
        addPoint(q.xr, q.yt, q.z);
        addCoord(q.sr, q.tt);
        addPoint(q.xl, q.yt, q.z);
        addCoord(q.sl, q.tt);
        addPoint(q.xl, q.yb, q.z);
        addCoord(q.sl, q.tb);

        // Add lower-right triangle
        addPoint(q.xr, q.yt, q.z);
        addCoord(q.sr, q.tt);
        addPoint(q.xl, q.yb, q.z);
        addCoord(q.sl, q.tb);
        addPoint(q.xr, q.yb, q.z);
        addCoord(q.sr, q.tb);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    protected void doFlush(final GL gl) {

        // Get an OpenGL 3 context
        final GL3 gl3 = gl.getGL3();

        // Upload data
        rewind();
        gl3.glBufferSubData(
                GL3.GL_ARRAY_BUFFER, // target
                0,                   // offset
                getSizeInBytes(),    // size
                getData());          // data

        // Draw
        gl3.glDrawArrays(
                GL3.GL_TRIANGLES,     // mode
                0,                    // first
                getSizeInVertices()); // count
        clear();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws GLException {@inheritDoc}
     */
    @Override
    public void endRendering(final GL gl) {

        super.endRendering(gl);

        // Get an OpenGL 3 context
        final GL3 gl3 = gl.getGL3();

        // Unbind the VBO and VAO
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
        gl3.glBindVertexArray(0);
    }

    //------------------------------------------------------------------
    // Helpers
    //

    /**
     * Creates a vertex array object for use with the pipeline.
     *
     * @param gl3 Current OpenGL context
     * @param program OpenGL handle to the shader program
     * @param vbo OpenGL handle to VBO holding vertices
     * @return OpenGL handle to resulting VAO
     * @throws NullPointerException if context is <tt>null</tt>
     * @throws AssertionError if program or VBO handle is less than one
     * @throws IllegalStateException if could not find attribute locations in program
     */
    private static int createVertexArrayObject(final GL3 gl3, final int program, final int vbo) {

        assert (program > 0);
        assert (vbo > 0);

        // Generate
        final int[] handles = new int[1];
        gl3.glGenVertexArrays(1, handles, 0);
        final int vao = handles[0];

        // Bind
        gl3.glBindVertexArray(vao);
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vbo);

        // Points
        final int pointLoc = gl3.glGetAttribLocation(program, POINT_ATTRIB_NAME);
        if (pointLoc == -1) {
            throw new IllegalStateException("Could not find point attribute location!");
        } else {
            gl3.glEnableVertexAttribArray(pointLoc);
            gl3.glVertexAttribPointer(
                    pointLoc,            // location
                    FLOATS_PER_POINT,    // number of components
                    GL3.GL_FLOAT,        // type
                    false,               // normalized
                    STRIDE,              // stride
                    POINT_OFFSET);       // offset
        }

        // Coords
        final int coordLoc = gl3.glGetAttribLocation(program, COORD_ATTRIB_NAME);
        if (coordLoc != -1) {
            gl3.glEnableVertexAttribArray(coordLoc);
            gl3.glVertexAttribPointer(
                    coordLoc,            // location
                    FLOATS_PER_COORD,    // number of components
                    GL3.GL_FLOAT,        // type
                    false,               // normalized
                    STRIDE,              // stride
                    COORD_OFFSET);       // offset
        }

        // Unbind
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
        gl3.glBindVertexArray(0);

        return vao;
    }
}
