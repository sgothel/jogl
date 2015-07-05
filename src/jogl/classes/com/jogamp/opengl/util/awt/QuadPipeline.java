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
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.GLProfile;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 * Utility for drawing a stream of quads.
 */
interface QuadPipeline {

    /**
     * Registers an {@link EventListener} with this {@link QuadPipeline}.
     *
     * @param listener Listener to register
     * @throws NullPointerException if listener is null
     */
    void addListener(/*@Nonnull*/ EventListener listener);

    /**
     * Adds a quad to this {@link QuadPipeline}.
     *
     * @param gl Current OpenGL context
     * @param quad Quad to add to pipeline
     * @throws NullPointerException if context or quad is null
     * @throws GLException if context is unexpected version
     */
    void addQuad(/*@Nonnull*/ GL gl, /*@Nonnull*/ Quad quad);

    /**
     * Starts a render cycle with this {@link QuadPipeline}.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is null
     * @throws GLException if context is unexpected version
     */
    void beginRendering(/*@Nonnull*/ GL gl);

    /**
     * Frees resources used by this {@link QuadPipeline}.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is null
     * @throws GLException if context is unexpected version
     */
    void dispose(/*@Nonnull*/ GL gl);

    /**
     * Finishes a render cycle with this {@link QuadPipeline}.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is null
     * @throws GLException if context is unexpected version
     */
    void endRendering(/*@Nonnull*/ GL gl);

    /**
     * Draws all vertices in this {@link QuadPipeline}.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is null
     * @throws GLException if context is unexpected version
     */
    void flush(/*@Nonnull*/ GL gl);

    // TODO: Rename to `size`?
    /**
     * Returns number of quads in this {@link QuadPipeline}.
     *
     * @return Number of quads in this pipeline, not negative
     */
    /*@Nonnegative*/
    int getSize();

    /**
     * Checks if there aren't any quads in this {@link QuadPipeline}.
     *
     * @return True if there aren't any quads in this pipeline
     */
    boolean isEmpty();

    /**
     * Deregisters an {@link EventListener} from this {@link QuadPipeline}.
     *
     * @param listener Listener to deregister, ignored if null or unregistered
     */
    void removeListener(/*@CheckForNull*/ EventListener listener);

    /**
     * <i>Observer</i> of a {@link QuadPipeline}.
     */
    interface EventListener {

        /**
         * Responds to an event from a {@link QuadPipeline}.
         *
         * @param type Type of event
         * @throws NullPointerException if event type is null
         */
        void onQuadPipelineEvent(/*@Nonnull*/ EventType type);
    }

    /**
     * Kind of event.
     */
    enum EventType {

        /**
         * Pipeline is automatically flushing all queued quads, e.g., when it's full.
         */
        AUTOMATIC_FLUSH;
    }

    /**
     * Structure for points and coordinates.
     */
    /*@NotThreadSafe*/
    static final class Quad {

        /**
         * Position of left side.
         */
        public float xl;

        /**
         * Position of right side.
         */
        public float xr;

        /**
         * Position of bottom side.
         */
        public float yb;

        /**
         * Position of top side.
         */
        public float yt;

        /**
         * Depth.
         */
        public float z;

        /**
         * Left texture coordinate.
         */
        public float sl;

        /**
         * Right texture coordinate.
         */
        public float sr;

        /**
         * Bottom texture coordinate.
         */
        public float tb;

        /**
         * Top texture coordinate.
         */
        public float tt;
    }
}


/**
 * Skeletal implementation of {@link QuadPipeline}.
 */
abstract class AbstractQuadPipeline implements QuadPipeline {

    /**
     * Number of bytes in one float.
     */
    /*@Nonnegative*/
    static final int SIZEOF_FLOAT = 4;

    /**
     * Number of bytes in one int.
     */
    /*@Nonnegative*/
    static final int SIZEOF_INT = 4;

    /**
     * Maximum number of quads in the buffer.
     */
    /*@Nonnegative*/
    static final int QUADS_PER_BUFFER = 100;

    /**
     * Number of components in a point attribute.
     */
    /*@Nonnegative*/
    static final int FLOATS_PER_POINT = 3;

    /**
     * Number of components in a texture coordinate attribute
     */
    /*@Nonnegative*/
    static final int FLOATS_PER_COORD = 2;

    /**
     * Total components in vertex.
     */
    /*@Nonnegative*/
    static final int FLOATS_PER_VERT = FLOATS_PER_POINT + FLOATS_PER_COORD;

    /**
     * Size of a point attribute in bytes.
     */
    /*@Nonnegative*/
    static final int BYTES_PER_POINT = FLOATS_PER_POINT * SIZEOF_FLOAT;

    /**
     * Size of a texture coordinate attribute in bytes.
     */
    /*@Nonnegative*/
    static final int BYTES_PER_COORD = FLOATS_PER_COORD * SIZEOF_FLOAT;

    /**
     * Total size of a vertex in bytes.
     */
    /*@Nonnegative*/
    static final int BYTES_PER_VERT = BYTES_PER_POINT + BYTES_PER_COORD;

    /**
     * Number of bytes before first point attribute in buffer.
     */
    /*@Nonnegative*/
    static final int POINT_OFFSET = 0;

    /**
     * Number of bytes before first texture coordinate in buffer.
     */
    /*@Nonnegative*/
    static final int COORD_OFFSET = BYTES_PER_POINT;

    /**
     * Number of bytes between successive values for the same attribute.
     */
    /*@Nonnegative*/
    static final int STRIDE = BYTES_PER_POINT + BYTES_PER_COORD;

    /**
     * Maximum buffer size in floats.
     */
    /*@Nonnegative*/
    final int FLOATS_PER_BUFFER;

    /**
     * Maximum buffer size in bytes.
     */
    /*@Nonnegative*/
    final int BYTES_PER_BUFFER;

    /**
     * Number of vertices per primitive.
     */
    /*@Nonnegative*/
    final int VERTS_PER_PRIM;

    /**
     * Maximum buffer size in primitives.
     */
    /*@Nonnegative*/
    final int PRIMS_PER_BUFFER;

    /**
     * Maximum buffer size in vertices.
     */
    /*@Nonnegative*/
    final int VERTS_PER_BUFFER;

    /**
     * Size of a quad in vertices.
     */
    /*@Nonnegative*/
    final int VERTS_PER_QUAD;

    /**
     * Size of a quad in bytes.
     */
    /*@Nonnegative*/
    final int BYTES_PER_QUAD;

    /**
     * Size of a quad in primitives.
     */
    /*@Nonnegative*/
    final int PRIMS_PER_QUAD;

    /**
     * Observers of events.
     */
    /*@Nonnull*/
    private final List<EventListener> listeners = new ArrayList<EventListener>();

    /**
     * Buffer of vertices.
     */
    /*@Nonnull*/
    private final FloatBuffer data;

    /**
     * Number of outstanding quads in the buffer.
     */
    /*@Nonnegative*/
    private int size = 0;

    /**
     * Constructs an abstract quad pipeline.
     *
     * @param vertsPerPrim Number of vertices per primitive
     * @param primsPerQuad Number of primitives per quad
     * @throws IllegalArgumentException if vertices or primitives is less than one
     */
    AbstractQuadPipeline(/*@Nonnegative*/ final int vertsPerPrim,
                         /*@Nonnegative*/ final int primsPerQuad) {

        checkArgument(vertsPerPrim > 0, "Vertices is less than one");
        checkArgument(primsPerQuad > 0, "Vertices is less than one");

        VERTS_PER_PRIM = vertsPerPrim;
        PRIMS_PER_QUAD = primsPerQuad;
        PRIMS_PER_BUFFER = primsPerQuad * QUADS_PER_BUFFER;
        VERTS_PER_QUAD = vertsPerPrim * primsPerQuad;
        VERTS_PER_BUFFER = PRIMS_PER_BUFFER * VERTS_PER_PRIM;
        FLOATS_PER_BUFFER = FLOATS_PER_VERT * VERTS_PER_BUFFER;
        BYTES_PER_BUFFER = BYTES_PER_VERT * VERTS_PER_BUFFER;
        BYTES_PER_QUAD = BYTES_PER_VERT * VERTS_PER_QUAD;

        this.data = Buffers.newDirectFloatBuffer(FLOATS_PER_BUFFER);
    }

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

    @Override
    public final void addListener(/*@Nonnull*/ final EventListener listener) {
        checkNotNull(listener, "Listener cannot be null");
        listeners.add(listener);
    }

    @Override
    public final void addQuad(/*@Nonnull*/ final GL gl, /*@Nonnull*/ final Quad quad) {

        checkNotNull(gl, "Context cannot be null");
        checkNotNull(quad, "Quad cannot be null");

        doAddQuad(quad);
        if (++size >= QUADS_PER_BUFFER) {
            fireEvent(EventType.AUTOMATIC_FLUSH);
            flush(gl);
        }
    }

    @Override
    public void beginRendering(/*@Nonnull*/ final GL gl) {
        // empty
    }

    private static void checkArgument(final boolean condition,
                                      /*@CheckForNull*/ final String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
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
     * @param gl Current OpenGL context
     * @param size Size in bytes of buffer
     * @return OpenGL handle to vertex buffer object
     * @throws NullPointerException if context is null
     * @throws IllegalArgumentException if size is negative
     */
    /*@Nonnegative*/
    protected static int createVertexBufferObject(/*@Nonnull*/ final GL2GL3 gl,
                                                  /*@Nonnegative*/ final int size) {

        checkNotNull(gl, "Context cannot be null");
        checkArgument(size >= 0, "Size cannot be negative");

        // Generate
        final int[] handles = new int[1];
        gl.glGenBuffers(1, handles, 0);
        final int vbo = handles[0];

        // Allocate
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(
                GL2GL3.GL_ARRAY_BUFFER, // target
                size,                   // size
                null,                   // data
                GL2GL3.GL_STREAM_DRAW); // usage
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

        return vbo;
    }

    @Override
    public void dispose(/*@Nonnull*/ final GL gl) {
        listeners.clear();
    }

    /**
     * Actually adds vertices from a quad to the buffer.
     *
     * @param quad Quad to add
     * @throws NullPointerException if quad is null
     */
    protected void doAddQuad(/*@Nonnull*/ final Quad quad) {
        addPoint(quad.xr, quad.yt, quad.z);
        addCoord(quad.sr, quad.tt);
        addPoint(quad.xl, quad.yt, quad.z);
        addCoord(quad.sl, quad.tt);
        addPoint(quad.xl, quad.yb, quad.z);
        addCoord(quad.sl, quad.tb);
        addPoint(quad.xr, quad.yb, quad.z);
        addCoord(quad.sr, quad.tb);
    }

    /**
     * Actually draws everything in the pipeline.
     *
     * @param gl Current OpenGL context
     * @throws NullPointerException if context is null
     * @throws GLException if context is unexpected version
     */
    protected abstract void doFlush(/*@Nonnull*/ final GL gl);

    @Override
    public void endRendering(/*@Nonnull*/ final GL gl) {
        flush(gl);
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

    @Override
    public final void flush(/*@Nonnull*/ final GL gl) {
        if (size > 0) {
            doFlush(gl);
        }
    }

    /**
     * Returns NIO buffer backing the pipeline.
     */
    /*@Nonnull*/
    protected final FloatBuffer getData() {
        return data;
    }

    /**
     * Returns next float in the pipeline.
     */
    /*@CheckForSigned*/
    protected final float getFloat() {
        return data.get();
    }

    /*@Nonnegative*/
    @Override
    public final int getSize() {
        return size;
    }

    @Override
    public final boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns size of vertices in the pipeline in bytes.
     */
    /*@Nonnegative*/
    public final int getSizeInBytes() {
        return size * BYTES_PER_QUAD;
    }

    /**
     * Returns number of primitives in the pipeline.
     */
    /*@Nonnegative*/
    public final int getSizeInPrimitives() {
        return size * PRIMS_PER_QUAD;
    }

    /**
     * Returns number of vertices in the pipeline.
     */
    /*@Nonnegative*/
    public final int getSizeInVertices() {
        return size * VERTS_PER_QUAD;
    }

    /**
     * Changes the buffer's position.
     *
     * @param index Location in buffer to move to
     */
    protected final void position(/*@Nonnegative*/ final int index) {
        data.position(index);
    }

    @Override
    public final void removeListener(/*@CheckForNull*/ final EventListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Rewinds the data buffer.
     */
    protected final void rewind() {
        data.rewind();
    }
}


/**
 * Utility for creating quad pipelines.
 */
/*ThreadSafe*/
final class QuadPipelineFactory {

    /**
     * Prevents instantiation.
     */
    private QuadPipelineFactory() {
        // pass
    }

    private static void checkArgument(final boolean condition,
                                      /*@CheckForNull*/ final String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
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

    /**
     * Creates a quad pipeline based on the current OpenGL context.
     *
     * @param gl Current OpenGL context
     * @param program Shader program to use, or zero to use default
     * @return Correct quad pipeline for the version of OpenGL in use, not null
     * @throws NullPointerException if context is null
     * @throws IllegalArgumentException if shader program is negative
     * @throws UnsupportedOperationException if GL is unsupported
     */
    /*@Nonnull*/
    QuadPipeline createQuadPipeline(/*@Nonnull*/ final GL gl, /*@Nonnegative*/ final int program) {

        checkNotNull(gl, "Context cannot be null");
        checkArgument(program >= 0, "Program cannot be negative");

        final GLProfile profile = gl.getGLProfile();

        if (profile.isGL3()) {
            final GL3 gl3 = gl.getGL3();
            return new QuadPipelineGL30(gl3, program);
        } else if (profile.isGL2()) {
            final GL2 gl2 = gl.getGL2();
            if (gl2.isExtensionAvailable(GLExtensions.VERSION_1_5)) {
                return new QuadPipelineGL15(gl2);
            } else if (gl2.isExtensionAvailable("GL_VERSION_1_1")) {
                return new QuadPipelineGL11();
            } else {
                return new QuadPipelineGL10();
            }
        } else {
            throw new UnsupportedOperationException("Profile currently unsupported");
        }
    }
}


/**
 * {@link QuadPipeline} for use with OpenGL 1.0.
 */
/*@NotThreadSafe*/
final class QuadPipelineGL10 extends AbstractQuadPipeline {

    /**
     * Number of vertices per primitive.
     */
    /*@Nonnegative*/
    private static final int VERTS_PER_PRIM = 4;

    /**
     * Number of primitives per quad.
     */
    /*@Nonnegative*/
    private static final int PRIMS_PER_QUAD = 1;

    /**
     * Constructs a {@link QuadPipelineGL10}.
     */
    QuadPipelineGL10() {
        super(VERTS_PER_PRIM, PRIMS_PER_QUAD);
    }

    @Override
    protected void doFlush(/*@Nonnull*/ final GL gl) {

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
 * {@link QuadPipeline} for use with OpenGL 1.1.
 */
/*@NotThreadSafe*/
final class QuadPipelineGL11 extends AbstractQuadPipeline {

    /**
     * Number of vertices per primitive.
     */
    /*@Nonnegative*/
    private static final int VERTS_PER_PRIM = 4;

    /**
     * Number of primitives per quad.
     */
    /*@Nonnegative*/
    private static final int PRIMS_PER_QUAD = 1;

    /**
     * Vertex array for points.
     */
    /*@Nonnull*/
    private final FloatBuffer pointsArray;

    /**
     * Vertex array for texture coordinates.
     */
    /*@Nonnull*/
    private final FloatBuffer coordsArray;

    /**
     * Constructs a {@link QuadPipelineGL11}.
     */
    QuadPipelineGL11() {

        super(VERTS_PER_PRIM, PRIMS_PER_QUAD);

        pointsArray = createFloatBufferView(getData(), POINT_OFFSET);
        coordsArray = createFloatBufferView(getData(), COORD_OFFSET);
    }

    @Override
    public void beginRendering(/*@Nonnull*/ final GL gl) {

        super.beginRendering(gl);

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

    private static void checkArgument(final boolean condition,
                                      /*@CheckForNull*/ final String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
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

    /**
     * Makes a view of a float buffer at a certain position.
     *
     * @param fb Original float buffer
     * @param position Index to start view at
     * @return Resulting float buffer
     * @throws NullPointerException if float buffer is null
     * @throws IllegalArgumentException if position is negative
     */
    /*@Nonnull*/
    private static FloatBuffer createFloatBufferView(/*@Nonnull*/ final FloatBuffer fb,
                                                     /*@Nonnegative*/ final int position) {

        checkNotNull(fb, "Buffer cannot be null");
        checkArgument(position >= 0, "Possition cannot be negative");

        // Store original position
        final int original = fb.position();

        // Make a view at desired position
        fb.position(position);
        final FloatBuffer view = fb.asReadOnlyBuffer();

        // Reset buffer to original position
        fb.position(original);

        return view;
    }

    @Override
    protected void doFlush(/*@Nonnull*/ final GL gl) {

        final GL2 gl2 = gl.getGL2();

        gl2.glDrawArrays(
                GL2.GL_QUADS,         // mode
                0,                    // first
                getSizeInVertices()); // count
        clear();
    }

    @Override
    public void endRendering(/*@Nonnull*/ final GL gl) {

        super.endRendering(gl);

        final GL2 gl2 = gl.getGL2();

        // Pop state
        gl2.glPopClientAttrib();
    }
}


/**
 * {@link QuadPipeline} for use with OpenGL 1.5.
 */
/*@NotThreadSafe*/
final class QuadPipelineGL15 extends AbstractQuadPipeline {

    /**
     * Number of vertices per primitive.
     */
    /*@Nonnegative*/
    private static final int VERTS_PER_PRIM = 4;

    /**
     * Number of primitives per quad.
     */
    /*@Nonnegative*/
    private static final int PRIMS_PER_QUAD = 1;

    /**
     * OpenGL handle to vertex buffer.
     */
    /*@Nonnegative*/
    private final int vbo;

    /**
     * Constructs a {@link QuadPipelineGL15}.
     *
     * @param gl2 Current OpenGL context
     * @throws NullPointerException if context is null
     */
    QuadPipelineGL15(/*@Nonnull*/ final GL2 gl2) {

        super(VERTS_PER_PRIM, PRIMS_PER_QUAD);

        this.vbo = createVertexBufferObject(gl2, BYTES_PER_BUFFER);
    }

    @Override
    public void beginRendering(/*@Nonnull*/ final GL gl) {

        super.beginRendering(gl);

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

    @Override
    public void dispose(/*@Nonnull*/ final GL gl) {

        super.dispose(gl);

        final GL2 gl2 = gl.getGL2();

        // Delete the vertex buffer object
        final int[] handles = new int[] { vbo };
        gl2.glDeleteBuffers(1, handles, 0);
    }

    @Override
    protected void doFlush(/*@Nonnull*/ final GL gl) {

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

    @Override
    public void endRendering(/*@Nonnull*/ final GL gl) {

        super.endRendering(gl);

        final GL2 gl2 = gl.getGL2();

        // Restore state
        gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        gl2.glPopClientAttrib();
    }
}


/**
 * {@link QuadPipeline} for use with OpenGL 3.
 *
 * <p>
 * {@code QuadPipelineGL30} draws quads using OpenGL 3 features.  It uses a Vertex Buffer Object to
 * store vertices in graphics memory and a Vertex Array Object to quickly switch which vertex
 * attributes are enabled.
 *
 * <p>
 * Since {@code GL_QUAD} has been deprecated in OpenGL 3, this implementation uses two triangles to
 * represent one quad.  An alternative implementation using one {@code GL_FAN} per quad was also
 * tested, but proved slower in most cases.  Apparently the penalty imposed by the extra work
 * required by the driver outweighed the benefit of transferring less vertices.
 */
/*@NotThreadSafe*/
final class QuadPipelineGL30 extends AbstractQuadPipeline {

    /**
     * Name of point attribute in shader program.
     */
    /*@Nonnull*/
    private static final String POINT_ATTRIB_NAME = "MCVertex";

    /**
     * Name of texture coordinate attribute in shader program.
     */
    /*@Nonnull*/
    private static final String COORD_ATTRIB_NAME = "TexCoord0";

    /**
     * Number of vertices per primitive.
     */
    /*@Nonnegative*/
    private static final int VERTS_PER_PRIM = 3;

    /**
     * Number of primitives per quad.
     */
    /*@Nonnegative*/
    private static final int PRIMS_PER_QUAD = 2;

    /**
     * Vertex Buffer Object with vertex data.
     */
    /*@Nonnegative*/
    private final int vbo;

    /**
     * Vertex Array Object with vertex attribute state.
     */
    /*@Nonnegative*/
    private final int vao;

    /**
     * Constructs a {@link QuadPipelineGL30}.
     *
     * @param gl Current OpenGL context
     * @param shaderProgram Shader program to render quads with
     * @throws NullPointerException if context is null
     * @throws IllegalArgumentException if shader program is less than one
     */
    QuadPipelineGL30(/*@Nonnull*/ final GL3 gl, /*@Nonnegative*/ final int shaderProgram) {

        super(VERTS_PER_PRIM, PRIMS_PER_QUAD);

        checkArgument(shaderProgram > 0, "Shader program cannot be less than one");

        this.vbo = createVertexBufferObject(gl, BYTES_PER_BUFFER);
        this.vao = createVertexArrayObject(gl, shaderProgram, vbo);
    }

    @Override
    public void beginRendering(/*@Nonnull*/ final GL gl) {

        super.beginRendering(gl);

        final GL3 gl3 = gl.getGL3();

        // Bind the VBO and VAO
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vbo);
        gl3.glBindVertexArray(vao);
    }

    private static void checkArgument(final boolean condition,
                                      /*@CheckForNull*/ final String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Creates a vertex array object for use with the pipeline.
     *
     * @param gl3 Current OpenGL context
     * @param program OpenGL handle to the shader program
     * @param vbo OpenGL handle to VBO holding vertices
     * @return OpenGL handle to resulting VAO
     * @throws NullPointerException if context is null
     * @throws IllegalArgumentException if program or VBO handle is less than one
     * @throws IllegalStateException if could not find attribute locations in program
     */
    /*@Nonnegative*/
    private static int createVertexArrayObject(/*@Nonnull*/ final GL3 gl3,
                                               /*@Nonnegative*/ final int program,
                                               /*@Nonnegative*/ final int vbo) {

        checkArgument(program > 0, "Shader Program cannot be less than one");
        checkArgument(vbo > 0, "Vertex Buffer Object cannot be less than one");

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

    @Override
    public void dispose(/*@Nonnull*/ final GL gl) {

        super.dispose(gl);

        final GL3 gl3 = gl.getGL3();

        // Delete VBO and VAO
        final int[] handles = new int[1];
        handles[0] = vbo;
        gl3.glDeleteBuffers(1, handles, 0);
        handles[0] = vao;
        gl3.glDeleteVertexArrays(1, handles, 0);
    }

    @Override
    protected void doAddQuad(/*@Nonnull*/ final Quad quad) {

        // Add upper-left triangle
        addPoint(quad.xr, quad.yt, quad.z);
        addCoord(quad.sr, quad.tt);
        addPoint(quad.xl, quad.yt, quad.z);
        addCoord(quad.sl, quad.tt);
        addPoint(quad.xl, quad.yb, quad.z);
        addCoord(quad.sl, quad.tb);

        // Add lower-right triangle
        addPoint(quad.xr, quad.yt, quad.z);
        addCoord(quad.sr, quad.tt);
        addPoint(quad.xl, quad.yb, quad.z);
        addCoord(quad.sl, quad.tb);
        addPoint(quad.xr, quad.yb, quad.z);
        addCoord(quad.sr, quad.tb);
    }

    @Override
    protected void doFlush(/*@Nonnull*/ final GL gl) {

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

    @Override
    public void endRendering(/*@Nonnull*/ final GL gl) {

        super.endRendering(gl);

        final GL3 gl3 = gl.getGL3();

        // Unbind the VBO and VAO
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
        gl3.glBindVertexArray(0);
    }
}
