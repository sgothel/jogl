/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.acore;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLBuffers;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCPUSourcingAPINEWT extends UITestCase {
    static long duration = 500; // ms

    static class Demo implements GLEventListener {
        private final static float[] vertexColorData = new float[]{
             0.0f,  0.75f, 0.0f,  1,0,0,
            -0.5f, -0.75f, 0.0f,  0,1,0,
             0.9f, -0.75f, 0.0f,  0,0,1
        };
        private final FloatBuffer vertexColorDataBuffer = GLBuffers.newDirectFloatBuffer(vertexColorData);

        private final short[] indices = new short[]{0, 1, 2};
        private final ShortBuffer indicesBuffer = GLBuffers.newDirectShortBuffer(indices);


        private int vertID = -1;
        private int fragID = -1;
        private int progID = -1;

        private static int createShader(final GL2ES2 gl, final int type,
                final String[] srcLines){
            final int shaderID = gl.glCreateShader(type);
            assert shaderID > 0;
            final int[] lengths  = new int[srcLines.length];
            for (int i = 0; i < srcLines.length; i++) {
                lengths[i] = srcLines[i].length();
            }
            gl.glShaderSource(shaderID, srcLines.length, srcLines, lengths, 0);
            gl.glCompileShader(shaderID);
            return shaderID;
        }

        private void initShaders(final GL2ES2 gl) {
            final String[] vertSrc = new String[]{
                "#version 150\n",
                "in vec4 vPosition;\n",
                "in vec4 vColor;\n",
                "out vec4 pColor;\n",
                "void main() {\n",
                "    pColor       = vColor;\n",
                "    gl_Position = vPosition;\n",
                "}\n"
            };
            vertID = createShader(gl, GL2ES2.GL_VERTEX_SHADER, vertSrc);

            final String[] fragSrc = new String[]{
                "#version 150\n",
                "in vec4 pColor;\n",
                "void main() {\n",
                "    gl_FragColor = pColor;\n",
                "}\n"
            };
            fragID = createShader(gl, GL2ES2.GL_FRAGMENT_SHADER, fragSrc);

            // We're done with the compiler
            gl.glReleaseShaderCompiler();

            progID = gl.glCreateProgram();
            assert progID > 0;
            gl.glAttachShader(progID, vertID);
            gl.glAttachShader(progID, fragID);
            gl.glLinkProgram(progID);
            gl.glValidateProgram(progID);
        }

        @Override
        public void init(final GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDisable(GL.GL_CULL_FACE);
            initShaders(gl);

            gl.setSwapInterval(1);
        }

        @Override
        public void dispose(final GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            gl.glDetachShader(progID, fragID);
            gl.glDetachShader(progID, vertID);
            gl.glDeleteProgram(progID);
            gl.glDeleteShader(fragID);
            gl.glDeleteShader(vertID);
        }

        private void displayCPUSourcing(final GL2 gl) {
            final int posLoc    = gl.glGetAttribLocation(progID, "vPosition");
            final int colorLoc = gl.glGetAttribLocation(progID, "vColor");
            gl.glEnableVertexAttribArray(posLoc);
            gl.glEnableVertexAttribArray(colorLoc);

            final int stride = 6 * Buffers.SIZEOF_FLOAT;
            // final int cOff   = 3 * Buffers.SIZEOF_FLOAT;
            gl.glVertexAttribPointer(posLoc,  3, GL.GL_FLOAT, false, stride, vertexColorDataBuffer);
            vertexColorDataBuffer.position(3); // move to cOff
            gl.glVertexAttribPointer(colorLoc,3, GL.GL_FLOAT, false, stride, vertexColorDataBuffer);
            vertexColorDataBuffer.position(0); // rewind cOff

            gl.glDrawElements(GL.GL_TRIANGLES, 3, GL.GL_UNSIGNED_SHORT, indicesBuffer);

            gl.glDisableVertexAttribArray(posLoc);
            gl.glDisableVertexAttribArray(colorLoc);
        }

        @Override
        public void display(final GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            gl.glClearColor(0x44, 0x44, 0x44, 0);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            gl.glUseProgram(progID);

            // Hard casting is of course invalid!
            // But we need to 'fake' compatibility mode to trigger CPU-sourcing w/ GL3 core
            displayCPUSourcing((GL2) gl);

            gl.glUseProgram(0);
        }

        @Override
        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int w, final int h) {
        }
    }

    private void testImpl(final GLProfile profile) throws InterruptedException {
        final GLCapabilities capabilities = new GLCapabilities(profile);
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
        final GLOffscreenAutoDrawable glad = factory.createOffscreenAutoDrawable(null, capabilities, null, 512, 512);

        final Demo vaoTest = new Demo();
        glad.addGLEventListener(vaoTest);
        glad.display();

        glad.destroy();
    }

    @Test
    public void test01GL2CPUSource() throws GLException, InterruptedException {
        if( ! GLProfile.isAvailable(GLProfile.GL2) ) {
            System.err.println("GL2 n/a");
            return;
        }
        testImpl(GLProfile.get(GLProfile.GL2));
    }

    @Test
    public void test02GL3CPUSource() throws GLException, InterruptedException {
        final GLProfile glp = GLProfile.getMaxProgrammableCore(true);
        if( !glp.isGL3ES3() && !glp.isGL2ES2() ) {
            System.err.println("No GL core profile available, got "+glp);
            return;
        }
        GLException exp = null;
        try {
            testImpl(glp);
        } catch(final GLException gle) {
            exp = gle;
            System.err.println("Expected Exception: "+exp.getMessage());
        }
        Assert.assertNotNull("Excpected GLException missing due to CPU Sourcing w/ GL3 core context", exp);
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestCPUSourcingAPINEWT.class.getName());
    }
}
