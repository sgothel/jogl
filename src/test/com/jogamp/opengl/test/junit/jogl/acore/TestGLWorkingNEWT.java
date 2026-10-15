/**
 * Copyright 2014-2026 JogAmp Community. All rights reserved.
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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.JoglVersion;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.DynamicLibraryBundle;
import com.jogamp.common.os.NativeLibrary;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.GLPixelBuffer;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.TextureIO;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLWorkingNEWT extends UITestCase {

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton();
    }

    @Test
    public void test00Onscreen() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(null);
        final GLWindow glad= GLWindow.create(caps);
        glad.setSize(256, 256);
        glad.setVisible(true);
        test(glad);
    }

    @Test
    public void test10Offscreen() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(null);
        final GLDrawableFactory glFactory = GLDrawableFactory.getFactory(caps.getGLProfile());
        final GLAutoDrawable glad = glFactory.createOffscreenAutoDrawable(null, caps, null, 256, 256);
        test(glad);
    }

    protected void test(final GLAutoDrawable glad) {
        final OneSquareES2 sq = new OneSquareES2();
        sq.makeSnap();
        glad.addGLEventListener(sq);
        glad.display();
        testBuffer((ByteBuffer)sq.getPixelBuffer().buffer, glad.getSurfaceWidth(), glad.getSurfaceHeight(), 3);
        glad.destroy();
    }
    private static int getRGB(final ByteBuffer bb, final int o) {
        return ( bb.get(o+0) & 0x000000ff ) << 16 |
               ( bb.get(o+1) & 0x000000ff ) << 8 |
               ( bb.get(o+2) & 0x000000ff );
    }

    static final int red   = 0x00ff0000; // below

    private static void testBuffer(final ByteBuffer bb, final int width, final int height, final int comp) {
        // Default origin 0/0 is lower left corner, so is the memory layout

        // x=0, y=0: RGB -> _RGB [high-byte .. low-byte]
        final int below = getRGB(bb, 0);
        System.err.println("below: 0x"+Integer.toHexString(below));

        // x=0, y=height-1: RGB -> _RGB [high-byte .. low-byte]
        final int above= getRGB(bb, ( height - 1 ) * ( width * comp ));
        System.err.println("above: 0x"+Integer.toHexString(above));

        if (above == red && below == red) {
            System.out.println("Image OK");
        } else {
            Assert.assertTrue("Image Error", false);
        }
    }

    private class OneSquareES2 implements GLEventListener {
        private ShaderState st;
        private PMVMatrix pmvMatrix;
        private GLUniformData pmvMatrixUniform;
        private GLUniformData gcuColor;
        private GLArrayDataServer vertices ;
        private final GLReadBufferUtil glReadBufferUtil;
        private int sn = 0;
        private boolean snap = false;

        public OneSquareES2() {
            glReadBufferUtil = new GLReadBufferUtil(false, false);
        }

        public GLPixelBuffer getPixelBuffer() { return glReadBufferUtil.getPixelBuffer(); }

        public void makeSnap() { snap = true; }

        @Override
        public void init(final GLAutoDrawable glad) {
            final GL2ES2 gl = glad.getGL().getGL2ES2();
            {
                System.err.println("init "+Thread.currentThread());
                System.err.println("Chosen GLCapabilities: " + glad.getChosenGLCapabilities());
                System.err.println("INIT GL IS: " + gl.getClass().getName());
                System.err.println(JoglVersion.getGLStrings(gl, null, false).toString());
                final DynamicLibraryBundle dlb = gl.getContext().getDynamicLibraryBundle();
                int i=0;
                for(final NativeLibrary l : dlb.getToolLibraries()) {
                    System.err.printf("- %02d: %s%n", i++, l.toString());
                }
            }
            if( !gl.hasGLSL() ) {
                System.err.println("No GLSL available, no rendering.");
                return;
            }
            st = new ShaderState();
            final ShaderCode vp0 = new ShaderCode(GL2ES2.GL_VERTEX_SHADER, new StringBuilder(vp));
            final ShaderCode fp0 = new ShaderCode(GL2ES2.GL_FRAGMENT_SHADER, new StringBuilder(fp));
            vp0.defaultShaderCustomization(gl, true, true);
            fp0.defaultShaderCustomization(gl, true, true);
            final ShaderProgram sp0 = new ShaderProgram();
            sp0.add(gl, vp0, System.err);
            sp0.add(gl, fp0, System.err);
            st.attachShaderProgram(gl, sp0, true);

            pmvMatrix = new PMVMatrix();
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmvMatrix.glLoadIdentity();
            pmvMatrix.glOrthof(0f, 1f, 0f, 1f, -10f, 10f);
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmvMatrix.glLoadIdentity();
            pmvMatrixUniform = new GLUniformData("gcu_PMVMatrix", 4, 4, pmvMatrix.getSyncPMv()); // P, Mv
            st.manage(pmvMatrixUniform, true);
            st.send(gl, pmvMatrixUniform);

            gcuColor = new GLUniformData("gcu_Color", 4, Buffers.newDirectFloatBuffer( new float[] { 1.0f, 0.0f, 0.0f, 1.0f } ));
            st.manage(gcuColor, true);
            st.send(gl, gcuColor);

            // Allocate Vertex Array
            vertices = GLArrayDataServer.createGLSL("gca_Vertices", 3, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
            vertices.putf(0); vertices.putf(1); vertices.putf( 0);
            vertices.putf(1); vertices.putf(1); vertices.putf( 0);
            vertices.putf(0); vertices.putf(0); vertices.putf( 0);
            vertices.putf(1); vertices.putf(0); vertices.putf( 0);
            vertices.seal(gl, true);
            st.manage(vertices, true);
            vertices.enableBuffer(gl, false);

            st.useProgram(gl, false);
        }

        @Override
        public void display(final GLAutoDrawable glad) {
            final GL2ES2 gl = glad.getGL().getGL2ES2();
            gl.glClearColor(0, 0, 0, 0);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            if( !gl.hasGLSL() ) {
                return;
            }
            st.useProgram(gl, true);

            vertices.enableBuffer(gl, true);
            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
            vertices.enableBuffer(gl, false);
            st.useProgram(gl, false);
            if( snap ) {
                snapshot(glad);
                snap = false;
            } else {
                readPixels(glad);
            }
        }

        @Override
        public void reshape(final GLAutoDrawable glad, final int x, final int y, final int width, final int height) {
        }

        @Override
        public void dispose(final GLAutoDrawable glad) {
            final GL2ES2 gl = glad.getGL().getGL2ES2();
            if( !gl.hasGLSL() ) {
                return;
            }
            st.destroy(gl);
            st = null;
            pmvMatrix = null;
            gcuColor = null;
        }

        private void snapshot(final GLAutoDrawable drawable) {
            final GL gl = drawable.getGL();
            final String filenameJGL = getSnapshotFilename(sn++, "jgl-usr",
                                                           drawable.getChosenGLCapabilities(), drawable.getSurfaceWidth(), drawable.getSurfaceHeight(),
                                                           glReadBufferUtil.hasAlpha(), TextureIO.PNG, null);
            gl.glFinish(); // just make sure rendering finished ..
            final boolean mustFlipVertically = !drawable.isGLOriented();
            if(glReadBufferUtil.readPixels(gl, mustFlipVertically)) {
                glReadBufferUtil.write(new File(filenameJGL));
            }
        }

        private boolean readPixels(final GLAutoDrawable drawable) {
            final GL gl = drawable.getGL();
            gl.glFinish(); // just make sure rendering finished ..
            final boolean mustFlipVertically = !drawable.isGLOriented();
            return glReadBufferUtil.readPixels(gl, mustFlipVertically);
        }

        private static final String vp =
                  "#if __VERSION__ >= 130\n"
                + "  #define attribute in\n"
                + "  #define varying out\n"
                + "#endif\n"
                + "\n"
                + "uniform mat4    gcu_PMVMatrix[2]; // P, Mv\n"
                + "attribute vec4  gca_Vertices;\n"
                + "\n"
                + "void main(void)\n"
                + "{\n"
                + "  gl_Position = gcu_PMVMatrix[0] * gcu_PMVMatrix[1] * gca_Vertices;\n"
                + "}\n"
                + "";
        private static final String fp =
                  "#if __VERSION__ >= 130\n"
                + "  #define varying in\n"
                + "  out vec4 mgl_FragColor;\n"
                + "#else\n"
                + "  #define mgl_FragColor gl_FragColor   \n"
                + "#endif\n"
                + "\n"
                + "uniform vec4 gcu_Color;\n"
                + "\n"
                + "void main (void)\n"
                + "{\n"
                + "    mgl_FragColor = gcu_Color;\n"
                + "}\n"
                + "";
    }

    public static void main(final String[] args) {
        org.junit.runner.JUnitCore.main(TestGLWorkingNEWT.class.getName());
    }

}
