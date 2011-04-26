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

import javax.media.opengl.FPSCounter;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import java.awt.Frame;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * Duplicates bug 459, where a vertex shader won't compile when 8 bits of stencil are requested.
 * This bug is Windows-only; it works on Mac OS X and CentOS.
 */
public class TestShaderCompilationBug459AWT extends UITestCase {
    static GLProfile glp;
    static int width, height;
    static long duration = 500; // ms
    /** Exception in shader code sets this, since it won't bubble up through AWT. */
    GLException glexception;

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton(true);
        glp = GLProfile.getDefault();
        Assert.assertNotNull(glp);
        width  = 512;
        height = 512;
    }

    @AfterClass
    public static void releaseClass() {
    }

    @Test
    public void compileShader() throws InterruptedException {
        GLProfile glp = GLProfile.get("GL2GL3");         
        GLCapabilities caps = new GLCapabilities(glp);   
        // commenting out this line makes it work
        caps.setStencilBits(8);

        // commenting in this line also makes it work
        //caps.setSampleBuffers(true); 

        Frame frame = new Frame("Bug 459 shader compilation test");
        Assert.assertNotNull(frame);

        GLCanvas glCanvas = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas);
        frame.add(glCanvas);
        frame.setSize(512, 512);

        glCanvas.addGLEventListener(new GLEventListener() {
            /* @Override */
            public void init(GLAutoDrawable drawable) {
                String code = "void main(void){gl_Position = vec4(0,0,0,1);}";         

                GL2GL3 gl = drawable.getGL().getGL2GL3();         
                int id = gl.glCreateShader(GL2GL3.GL_VERTEX_SHADER); 

                try {
                    gl.glShaderSource(id, 1, new String[] { code }, (int[])null, 0); 
                    gl.glCompileShader(id); 
    
                    int[] compiled = new int[1]; 
                    gl.glGetShaderiv(id, GL2GL3.GL_COMPILE_STATUS, compiled, 0); 
                    if (compiled[0] == GL2GL3.GL_FALSE) { 
                        int[] logLength = new int[1]; 
                        gl.glGetShaderiv(id, GL2GL3.GL_INFO_LOG_LENGTH, logLength, 0); 
                        
                        byte[] log = new byte[logLength[0]]; 
                        gl.glGetShaderInfoLog(id, logLength[0], (int[])null, 0, log, 0); 
                        
                        System.err.println("Error compiling the shader: " + new String(log)); 
                            
                        gl.glDeleteShader(id); 
                    } 
                    else {
                        System.out.println("Shader compiled: id=" + id); 
                    }
                }
                catch( GLException e ) {
                    glexception = e;
                }
            }

            /* @Override */
            public void dispose(GLAutoDrawable drawable) {
            }

            /* @Override */
            public void display(GLAutoDrawable drawable) {
            }

            /* @Override */
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            }
        });

        Animator animator = new Animator(glCanvas);
        frame.setVisible(true);
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
        frame.setVisible(false);
        Assert.assertEquals(false, frame.isVisible());
        frame.remove(glCanvas);
        frame.dispose();
        frame=null;
        glCanvas=null;
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestShaderCompilationBug459AWT.class.getName());
    }
}
