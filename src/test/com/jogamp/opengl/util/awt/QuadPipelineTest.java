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

import static org.junit.Assert.*;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.awt.QuadPipeline.Quad;

import javax.swing.JFrame;

import org.junit.Test;


/**
 * Test for {@link QuadPipeline}.
 */
public class QuadPipelineTest {

    // Amount of time to wait before closing the window
    private static final int WAIT_TIME = 1000;

    // Shader code for OpenGL 3 tests
    private static final String VERT_SOURCE;
    private static final String FRAG_SOURCE;

    // Shader program for OpenGL 3 test
    private int program;

    // Pipeline to render with
    private QuadPipeline pipeline;

    // Quad to render
    private Quad quad;

    // Utility for making canvases
    private final GLCanvasFactory canvasFactory = new GLCanvasFactory();

    /**
     * Initializes static fields.
     */
    static {
        VERT_SOURCE =
            "#version 130\n" +
            "uniform mat4 MVPMatrix=mat4(1);\n" +
            "in vec4 MCVertex;\n" +
            "out vec4 gl_Position;\n" +
            "void main() {\n" +
            "   gl_Position = MVPMatrix * MCVertex;\n" +
            "}\n";
        FRAG_SOURCE =
            "#version 130\n" +
            "uniform vec4 Color=vec4(1,1,1,1);\n" +
            "out vec4 FragColor;\n" +
            "void main() {\n" +
            "   FragColor = Color;\n" +
            "}\n";
    }

    /**
     * Test case for {@link QuadPipelineGL10}.
     *
     * <p><em>Performs the following:</em>
     * <ul>
     *   <li>Creates a pipeline
     *   <li>Adds a unit square to the pipeline
     *   <li>Flushes the pipeline
     * </ul>
     *
     * <p><em>Results:</em>
     * <ul>
     *   <li>White square is drawn on a cyan background
     * </ul>
     */
    @Test
    public void testQuadPipelineGL10() throws Exception {

        final JFrame frame = new JFrame("testQuadPipelineGL10");
        final GLCanvas canvas = canvasFactory.createGLCanvas("GL2");

        frame.add(canvas);
        canvas.addGLEventListener(new GL2EventAdapter() {

            @Override
            public void doInit(final GL2 gl) {
                pipeline = new QuadPipelineGL10(gl);
                quad = createQuad();
            }

            @Override
            public void doDisplay(final GL2 gl) {

                // View
                gl.glViewport(0, 0, 512, 512);
                gl.glClearColor(0, 1, 1, 1);
                gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

                // Create geometry
                pipeline.beginRendering(gl);
                pipeline.addQuad(gl, quad);
                pipeline.endRendering(gl);
            }
        });
        TestRunner.run(frame, WAIT_TIME);
    }

    /**
     * Test case for {@link QuadPipelineGL11}.
     *
     * <p><em>Performs the following:</em>
     * <ul>
     *   <li>Creates a pipeline
     *   <li>Adds a unit square to the pipeline
     *   <li>Flushes the pipeline
     * </ul>
     *
     * <p><em>Results:</em>
     * <ul>
     *   <li>White square is drawn on a cyan background
     * </ul>
     */
    @Test
    public void testQuadPipelineGL11() throws Exception {

        final JFrame frame = new JFrame("testQuadPipelineGL11");
        final GLCanvas canvas = canvasFactory.createGLCanvas("GL2");

        frame.add(canvas);
        canvas.addGLEventListener(new DebugGL2EventAdapter() {

            @Override
            public void doInit(final GL2 gl) {
                pipeline = new QuadPipelineGL11(gl);
                quad = createQuad();
            }

            @Override
            public void doDisplay(final GL2 gl) {

                // View
                gl.glViewport(0, 0, 512, 512);
                gl.glClearColor(0, 1, 1, 1);
                gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

                // Create geometry
                pipeline.beginRendering(gl);
                pipeline.addQuad(gl, quad);
                pipeline.endRendering(gl);
            }
        });
        TestRunner.run(frame, WAIT_TIME);
    }

    /**
     * Test case for {@link QuadPipelineGL15}.
     *
     * <p><em>Performs the following:</em>
     * <ul>
     *   <li>Creates a pipeline
     *   <li>Adds a unit square to the pipeline
     *   <li>Flushes the pipeline
     * </ul>
     *
     * <p><em>Results:</em>
     * <ul>
     *   <li>White square is drawn on a cyan background
     * </ul>
     */
    @Test
    public void testQuadPipelineGL15() throws Exception {

        final JFrame frame = new JFrame("testQuadPipelineGL15");
        final GLCanvas canvas = canvasFactory.createGLCanvas("GL2");

        frame.add(canvas);
        canvas.addGLEventListener(new DebugGL2EventAdapter() {

            @Override
            public void doInit(final GL2 gl) {
                pipeline = new QuadPipelineGL15(gl);
                quad = createQuad();
            }

            @Override
            public void doDisplay(final GL2 gl) {

                // View
                gl.glViewport(0, 0, 512, 512);
                gl.glClearColor(0, 1, 1, 1);
                gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

                // Create geometry
                pipeline.beginRendering(gl);
                pipeline.addQuad(gl, quad);
                pipeline.endRendering(gl);
            }
        });
        TestRunner.run(frame, WAIT_TIME);
    }

    /**
     * Test case for {@link QuadPipelineGL30}.
     *
     * <p><em>Performs the following:</em>
     * <ul>
     *   <li>Loads very basic vertex and fragment shaders
     *   <li>Sets the MVP matrix to equivalent of gluOrtho2D(0, 512, 0, 512)
     *   <li>Creates a new QuadPipeline
     *   <li>Clears the screen to CYAN
     *   <li>Adds a unit square to the QuadPipeline
     *   <li>Flushes the QuadPipeline
     * </ul>
     *
     * <p><em>Results:</em>
     * <ul>
     *   <li>White quad is drawn on a cyan background
     * </ul>
     */
    @Test
    public void testQuadPipelineGL30() throws Exception {

        final JFrame frame = new JFrame("testQuadPipelineGL30");
        final GLCanvas canvas = canvasFactory.createGLCanvas("GL3");

        frame.add(canvas);
        canvas.addGLEventListener(new DebugGL3EventAdapter() {

            @Override
            public void doInit(final GL3 gl) {
                program = createProgram(gl);
                pipeline = new QuadPipelineGL30(gl, program);
                quad = createQuad();
            }

            @Override
            public void doDisplay(final GL3 gl) {

                // View
                gl.glViewport(0, 0, 512, 512);
                gl.glClearColor(0, 1, 1, 1);
                gl.glClear(GL3.GL_COLOR_BUFFER_BIT);

                // Create geometry
                gl.glUseProgram(program);
                pipeline.beginRendering(gl);
                pipeline.addQuad(gl, quad);
                pipeline.endRendering(gl);
            }
        });
        TestRunner.run(frame, WAIT_TIME);
    }

    //-----------------------------------------------------------------
    // Helpers
    //

    /**
     * Returns a shader program for use with pipeline.
     */
    private static int createProgram(final GL2ES2 gl) {
        int program = -1;
        try {
            program = ShaderLoader.loadProgram(gl, VERT_SOURCE, FRAG_SOURCE);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return program;
    }

    /**
     * Returns a quad structure for use with pipeline.
     */
    private static Quad createQuad() {
        final Quad q = new Quad();
        q.xl = -0.5f;
        q.xr = +0.5f;
        q.yb = -0.5f;
        q.yt = +0.5f;
        q.z = 0;
        q.sl = 0;
        q.sr = 1;
        q.tb = 0;
        q.tt = 1;
        return q;
    }
}
