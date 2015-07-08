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
package jogamp.opengl.util.awt;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;


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
/*@VisibleForTesting*/
/*@NotThreadSafe*/
public final class QuadPipelineGL30 extends AbstractQuadPipeline {

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
    /*@VisibleForTesting*/
    public QuadPipelineGL30(/*@Nonnull*/ final GL3 gl, /*@Nonnegative*/ final int shaderProgram) {

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
