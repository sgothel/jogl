/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.jogl.glsl;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import java.awt.Frame;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Duplicates bug 459, where a vertex shader won't compile when 8 bits of stencil are requested.
 * This bug is Windows-only; it works on Mac OS X and CentOS.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestShaderCompilationBug459AWT extends UITestCase {
    static int width, height;
    static long duration = 500; // ms
    /** Exception in shader code sets this, since it won't bubble up through AWT. */
    GLException glexception;

    @BeforeClass
    public static void initClass() {
        width  = 512;
        height = 512;
    }

    @AfterClass
    public static void releaseClass() {
    }

    @Test
    public void compileShader() throws InterruptedException {
        final GLProfile glp = GLProfile.get(GLProfile.GL2GL3);
        final GLCapabilities caps = new GLCapabilities(glp);
        // commenting out this line makes it work
        caps.setStencilBits(8);

        // commenting in this line also makes it work
        //caps.setSampleBuffers(true);

        final Frame frame = new Frame("Bug 459 shader compilation test");
        Assert.assertNotNull(frame);

        final GLCanvas glCanvas = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas);
        frame.add(glCanvas);

        glCanvas.addGLEventListener(new GLEventListener() {
            /* @Override */
            public void init(final GLAutoDrawable drawable) {
                final String code = "void main(void){gl_Position = vec4(0,0,0,1);}";

                final GL2GL3 gl = drawable.getGL().getGL2GL3();
                final int id = gl.glCreateShader(GL2ES2.GL_VERTEX_SHADER);

                try {
                    gl.glShaderSource(id, 1, new String[] { code }, (int[])null, 0);
                    gl.glCompileShader(id);

                    final int[] compiled = new int[1];
                    gl.glGetShaderiv(id, GL2ES2.GL_COMPILE_STATUS, compiled, 0);
                    if (compiled[0] == GL.GL_FALSE) {
                        final int[] logLength = new int[1];
                        gl.glGetShaderiv(id, GL2ES2.GL_INFO_LOG_LENGTH, logLength, 0);

                        final byte[] log = new byte[logLength[0]];
                        gl.glGetShaderInfoLog(id, logLength[0], (int[])null, 0, log, 0);

                        System.err.println("Error compiling the shader: " + new String(log));

                        gl.glDeleteShader(id);
                    }
                    else {
                        System.out.println("Shader compiled: id=" + id);
                    }
                }
                catch( final GLException e ) {
                    glexception = e;
                }
            }

            /* @Override */
            public void dispose(final GLAutoDrawable drawable) {
            }

            /* @Override */
            public void display(final GLAutoDrawable drawable) {
            }

            /* @Override */
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
            }
        });

        final Animator animator = new Animator(glCanvas);
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setSize(512, 512);
                    frame.setVisible(true);
                } } );
        } catch(final Exception ex) {
            throw new RuntimeException(ex);
        }
        animator.setUpdateFPSFrames(1, null);
        animator.start();

        while(animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        Assert.assertTrue( glexception != null ? glexception.getMessage() : "", glexception == null );
        Assert.assertNotNull(frame);
        Assert.assertNotNull(glCanvas);
        Assert.assertNotNull(animator);

        animator.stop();
        Assert.assertEquals(false, animator.isAnimating());
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    frame.remove(glCanvas);
                    frame.dispose();
                }});
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestShaderCompilationBug459AWT.class.getName());
    }
}
