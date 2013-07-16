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

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import junit.framework.Assert;

import org.junit.Test;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;

/**
 * Test Vertex Array Object (VAO) Usage.
 * <p>
 * testGL3() tests VAO alone, i.e. w/o VBO enable/disabling.
 * </p>
 * <p>
 * testGL3bc() tests VAO and VBO while alternating between both methods.
 * </p>
 */
public class TestBug692GL3VAO extends UITestCase {
    static long duration = 500; // ms
    
    static class GL3VAODemo implements GLEventListener {
        /** Different modes of displaying the geometry */
        public enum Mode {
            /** Traditional one without using VAO */
            NON_VAO {
                @Override
                void display(GL3VAODemo t, GL3 gl) {
                    t.displayNonVAO(gl);
                }
            },
            
            /** Using VAOs throws [incorrectly as of JOGL 2.0rc11] a GLException */
            VAO_NORMAL {
                @Override
                void display(GL3VAODemo t, GL3 gl) {
                    t.displayVAONormal(gl);
                }
            };
            
            abstract void display(GL3VAODemo t, GL3 gl);
        }
        
        private final Mode[] allModes;
        private Mode currentMode;
        private int currentModeIdx;        
        
        public GL3VAODemo(Mode[] modes) {
            allModes = modes;
            currentMode = allModes[0];
            currentModeIdx = 0;
        }
        
        private final static float[] vertexData = new float[]{
             0.0f,  0.75f, 0.0f,  1,0,0,
            -0.5f, -0.75f, 0.0f,  0,1,0,
             0.9f, -0.75f, 0.0f,  0,0,1
        };
        
        private int ibo = -1;
        private int vbo = -1;
        private int vertID = -1;
        private int fragID = -1;
        private int progID = -1;
        
        private int vaoNormal  = -1;
        
        private static int createShader(final GL3 gl, int type,
                final String[] srcLines){
            int shaderID = gl.glCreateShader(type);
            assert shaderID > 0;
            int[] lengths  = new int[srcLines.length];
            for (int i = 0; i < srcLines.length; i++) {
                lengths[i] = srcLines[i].length();
            }
            gl.glShaderSource(shaderID, srcLines.length, srcLines, lengths, 0);
            gl.glCompileShader(shaderID);
            return shaderID;
        }
        
        private void initBuffers(GL3 gl) {
            // IDs for 2 buffers
            int[] buffArray = new int[2];
            gl.glGenBuffers(buffArray.length, buffArray, 0);
            vbo = buffArray[0];
            assert vbo > 0;
            
            // Bind buffer and upload data
            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vbo);
            FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(vertexData);
            assert buffer.remaining() == vertexData.length;
            gl.glBufferData(GL3.GL_ARRAY_BUFFER, vertexData.length * Buffers.SIZEOF_FLOAT,
                    buffer, GL3.GL_STATIC_DRAW);
            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
            
            // Buffer with the 3 indices required for one triangle
            ibo = buffArray[1];
            assert ibo > 0;
            final short[] indices = new short[]{0, 1, 2};
            ShortBuffer shortBuffer = GLBuffers.newDirectShortBuffer(indices);
            assert shortBuffer.remaining() == indices.length;
            gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, ibo);
            gl.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER,indices.length*Buffers.SIZEOF_SHORT,
                    shortBuffer, GL3.GL_STATIC_DRAW);
            gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
        private void initShaders(GL3 gl) {
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
            vertID = createShader(gl, GL3.GL_VERTEX_SHADER, vertSrc);
    
            final String[] fragSrc = new String[]{
                "#version 150\n",
                "in vec4 pColor;\n",
                "void main() {\n",
                "    gl_FragColor = pColor;\n",
                "}\n"
            };
            fragID = createShader(gl, GL3.GL_FRAGMENT_SHADER, fragSrc);
    
            // We're done with the compiler
            gl.glReleaseShaderCompiler();
    
            progID = gl.glCreateProgram();
            assert progID > 0;
            gl.glAttachShader(progID, vertID);
            gl.glAttachShader(progID, fragID);
            gl.glLinkProgram(progID);
            gl.glValidateProgram(progID);
        }
        
        private int initVAO(GL3 gl) {
            int[] buff = new int[1];
            gl.glGenVertexArrays(1, buff, 0);
            int vao = buff[0];
            Assert.assertTrue("Invalid VAO: "+vao, vao > 0);
            
            
            gl.glUseProgram(progID);
            final int posLoc = gl.glGetAttribLocation(progID, "vPosition");
            final int colorLoc = gl.glGetAttribLocation(progID, "vColor");
            gl.glUseProgram(0);
            
            gl.glBindVertexArray(vao);
            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vbo);
            gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, ibo);
            
            gl.glEnableVertexAttribArray(posLoc);
            gl.glEnableVertexAttribArray(colorLoc);
            
            final int stride = 6 * Buffers.SIZEOF_FLOAT;
            final int cOff   = 3 * Buffers.SIZEOF_FLOAT;
            gl.glVertexAttribPointer(posLoc,  3, GL3.GL_FLOAT, false, stride, 0L);
            gl.glVertexAttribPointer(colorLoc,3, GL3.GL_FLOAT, false, stride, cOff);
    
            gl.glBindVertexArray(0);
            return vao;
        }
    
        @Override
        public void init(GLAutoDrawable drawable) {
            drawable.setGL(new DebugGL3(drawable.getGL().getGL3()));
            
            final GL3 gl = drawable.getGL().getGL3();
            gl.glEnable(GL3.GL_DEPTH_TEST);
            gl.glDisable(GL3.GL_CULL_FACE);
            initBuffers(gl);
            initShaders(gl);
            
            vaoNormal  = initVAO(gl);
            
            gl.setSwapInterval(1);
        }
    
        @Override
        public void dispose(GLAutoDrawable drawable) {
            final GL3 gl = drawable.getGL().getGL3();
            gl.glDeleteBuffers(2, new int[]{vbo, ibo}, 0);
            gl.glDetachShader(progID, fragID);
            gl.glDetachShader(progID, vertID);
            gl.glDeleteProgram(progID);
            gl.glDeleteShader(fragID);
            gl.glDeleteShader(vertID);
        }
        
        private void displayNonVAO(final GL3 gl) {
           final int posLoc    = gl.glGetAttribLocation(progID, "vPosition");
            final int colorLoc = gl.glGetAttribLocation(progID, "vColor");
            gl.glEnableVertexAttribArray(posLoc);
            gl.glEnableVertexAttribArray(colorLoc);
    
            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vbo);
            final int stride = 6 * Buffers.SIZEOF_FLOAT;
            final int cOff   = 3 * Buffers.SIZEOF_FLOAT;
            gl.glVertexAttribPointer(posLoc,  3, GL3.GL_FLOAT, false, stride, 0L);
            gl.glVertexAttribPointer(colorLoc,3, GL3.GL_FLOAT, false, stride, cOff);
            gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, ibo);
            gl.glDrawElements(GL3.GL_TRIANGLES, 3, GL3.GL_UNSIGNED_SHORT, 0L);
    
            gl.glDisableVertexAttribArray(posLoc);
            gl.glDisableVertexAttribArray(colorLoc);
            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
            gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
        
        private void displayVAONormal(final GL3 gl) {
            try {
                gl.glBindVertexArray(vaoNormal);
                gl.glDrawElements(GL3.GL_TRIANGLES, 3, GL3.GL_UNSIGNED_SHORT, 0L);
                gl.glBindVertexArray(0);
            } catch (GLException ex) {
                Logger.getLogger(TestBug692GL3VAO.class.getName()).log(Level.SEVERE,null,ex);
            }
        }
        
        @Override
        public void display(GLAutoDrawable drawable) {
            final GL3 gl = drawable.getGL().getGL3();
            float color = ((float) currentMode.ordinal() + 1) / (Mode.values().length + 2);
            gl.glClearColor(color, color, color, 0);
            gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
            gl.glUseProgram(progID);
            final Mode newMode;
            {
                currentModeIdx = ( currentModeIdx + 1 ) % allModes.length;  
                newMode = allModes[ currentModeIdx ];
            }
            if (newMode != currentMode) {
                currentMode = newMode;
                System.out.println("Display mode: " + currentMode);
            }
            currentMode.display(this, gl);
            gl.glUseProgram(0);
        }
    
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        }        
    }
    
    private void testImpl(GLProfile profile, GL3VAODemo.Mode[] modes) throws InterruptedException {
        final GLCapabilities capabilities = new GLCapabilities(profile);
        final GLWindow glWindow = GLWindow.create(capabilities);
        glWindow.setSize(512, 512);
        
        Animator anim = new Animator(glWindow);
        
        QuitAdapter quitAdapter = new QuitAdapter();
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);
        
        final GL3VAODemo vaoTest = new GL3VAODemo(modes);
        glWindow.addGLEventListener(vaoTest);
        glWindow.setVisible(true);
        anim.start();
        
        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!quitAdapter.shouldQuit() && t1-t0<duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }
        
        anim.stop();
        glWindow.destroy();
    }
    
    //@Test
    public void testGL3() throws GLException, InterruptedException {
        if( ! GLProfile.isAvailable(GLProfile.GL3) ) {
            System.err.println("GL3 n/a");
            return;
        }
        GL3VAODemo.Mode[] modes = new GL3VAODemo.Mode[] { GL3VAODemo.Mode.VAO_NORMAL }; 
        testImpl(GLProfile.get(GLProfile.GL3), modes);        
    }
    
    @Test
    public void testGL3bc() throws GLException, InterruptedException {
        if( ! GLProfile.isAvailable(GLProfile.GL3bc) ) {
            System.err.println("GL3bc n/a");
            return;
        }
        GL3VAODemo.Mode[] modes = new GL3VAODemo.Mode[] { GL3VAODemo.Mode.VAO_NORMAL, GL3VAODemo.Mode.NON_VAO }; 
        testImpl(GLProfile.get(GLProfile.GL3bc), modes);        
    }
    
    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = MiscUtils.atoi(args[++i], (int)duration);
            }
        }
        String tstname = TestBug692GL3VAO.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }    
    
}
