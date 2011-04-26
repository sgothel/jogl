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

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquare0;
import com.jogamp.opengl.test.junit.jogl.glsl.GLSLMiscHelper.WindowContext;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.io.IOException;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLUniformData;

import org.junit.Assert;
import org.junit.Test;

/**
 * Testing different vertex attribute (VA) data sets on one shader
 * and shader state in general.
 */
public class TestGLSLShaderState02NEWT extends UITestCase {
    static long durationPerTest = 10; // ms

    static final int vertices0_loc = 0; // FIXME: AMD needs this to be location 0 ? hu ?
    static final int colors0_loc = 5;
    
    @Test
    public void testShaderState01ValidationSP1Linked() throws InterruptedException {
        testShaderState01Validation(true);
    }
    @Test
    public void testShaderState01ValidationSP1Unlinked() throws InterruptedException {
        testShaderState01Validation(false);
    }
    
    private void testShaderState01Validation(boolean linkSP1) throws InterruptedException {
        // preset ..
        GLSLMiscHelper.WindowContext winctx = GLSLMiscHelper.createWindow(GLProfile.getGL2ES2(), true);
        GLDrawable drawable = winctx.context.getGLDrawable();
        GL2ES2 gl = winctx.context.getGL().getGL2ES2();
        
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        // test code ..        
        ShaderState st = new ShaderState();
        
        ShaderCode rsVp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "RedSquareShader");
        ShaderCode rsFp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "RedSquareShader");
        ShaderCode rsFp1 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "RedSquareShader2");

        ShaderProgram sp1 = new ShaderProgram();
        sp1.add(rsVp0);
        sp1.add(rsFp1);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertTrue(0>sp1.program());        
        sp1.init(gl);
        Assert.assertTrue(0<=sp1.program()); 
        Assert.assertTrue(!sp1.inUse());
        Assert.assertTrue(!sp1.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        if(linkSP1) {
            Assert.assertTrue(sp1.link(gl, System.err));
            Assert.assertTrue(sp1.linked());
            Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        }
        
        ShaderProgram sp0 = new ShaderProgram();        
        sp0.add(rsVp0);
        sp0.add(rsFp0);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        Assert.assertTrue(0>sp0.program());        
        sp0.init(gl);
        Assert.assertTrue(0<=sp0.program()); 
        Assert.assertTrue(!sp0.inUse());
        Assert.assertTrue(!sp0.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());        
                
        st.attachShaderProgram(gl, sp0);
        
        // Allocate Vertex Array0
        GLArrayDataServer vertices0 = GLSLMiscHelper.createRSVertices0(gl, st, vertices0_loc);
        st.ownAttribute(vertices0, true);
        System.err.println("vertices0: " + vertices0);        
        vertices0.enableBuffer(gl, false);
        Assert.assertEquals(vertices0_loc, vertices0.getLocation());
        
        // Allocate Color Array0
        GLArrayDataServer colors0 = GLSLMiscHelper.createRSColors0(gl, st, colors0_loc);
        st.ownAttribute(colors0, true);
        System.err.println("colors0: " + colors0);
        colors0.enableBuffer(gl, false);
        Assert.assertEquals(colors0_loc, colors0.getLocation());
                               
        Assert.assertTrue(sp0.link(gl, System.err));
        Assert.assertTrue(sp0.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        Assert.assertEquals(vertices0_loc, vertices0.getLocation());
        Assert.assertEquals(vertices0_loc, st.getAttribLocation(gl, vertices0.getName()));
        Assert.assertEquals(vertices0_loc, gl.glGetAttribLocation(st.shaderProgram().program(), vertices0.getName()));
        
        Assert.assertEquals(colors0_loc, colors0.getLocation());
        Assert.assertEquals(colors0_loc, st.getAttribLocation(gl, colors0.getName()));
        Assert.assertEquals(colors0_loc, gl.glGetAttribLocation(st.shaderProgram().program(), colors0.getName()));
        
        Assert.assertEquals(null, ShaderState.getShaderState(gl));
        st.useProgram(gl, true);
        Assert.assertTrue(sp0.inUse());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertEquals(st, ShaderState.getShaderState(gl));        
        
        // setup gcu_PMVMatrix
        PMVMatrix pmvMatrix = new PMVMatrix();
        GLUniformData pmvMatrixUniform = new GLUniformData("gcu_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        st.ownUniform(pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        st.uniform(gl, pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertEquals(pmvMatrixUniform, st.getUniform("gcu_PMVMatrix"));
        
        // Allocate Vertex Array1
        GLArrayDataServer vertices1 = GLSLMiscHelper.createRSVertices1(gl, st);
        st.ownAttribute(vertices1, true);
        System.err.println("vertices1: " + vertices1);
        vertices1.enableBuffer(gl, false);
        
        // Allocate Color Array1
        GLArrayDataServer colors1 = GLSLMiscHelper.createRSColors1(gl, st);
        st.ownAttribute(colors1, true);
        System.err.println("colors1: " + colors1);
        colors1.enableBuffer(gl, false);
        
        // misc GL setup
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // reshape
        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0F, (float) drawable.getWidth() / (float) drawable.getHeight(), 1.0F, 100.0F);
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        st.uniform(gl, pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        // display #1 vertices0 / colors0 (post-disable)
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices0, colors0, true, 1, durationPerTest);

        // display #2 #1 vertices1 / colors1 (post-disable)
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices1, colors1, true, 2, durationPerTest);
        
        // display #3 vertices0 / colors0 (post-disable)
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices0, colors0, true, 3, durationPerTest);

        // SP1
        st.attachShaderProgram(gl, sp1);
        
        if(!linkSP1) {
            // all attribute locations shall be same now, due to impl. glBindAttributeLocation
            Assert.assertEquals(vertices0_loc, vertices0.getLocation());
            Assert.assertEquals(vertices0_loc, st.getAttribLocation(gl, vertices0.getName()));
            Assert.assertEquals(vertices0_loc, gl.glGetAttribLocation(st.shaderProgram().program(), vertices0.getName()));
            
            Assert.assertEquals(colors0_loc, colors0.getLocation());
            Assert.assertEquals(colors0_loc, st.getAttribLocation(gl, colors0.getName()));
            Assert.assertEquals(colors0_loc, gl.glGetAttribLocation(st.shaderProgram().program(), colors0.getName()));
        }
        
        // display #1 vertices0 / colors0 (post-disable)
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices0, colors0, true, 10, durationPerTest);

        // display #2 #1 vertices1 / colors1 (post-disable)
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices1, colors1, true, 20, durationPerTest);
        
        // display #3 vertices0 / colors0 (post-disable)
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices0, colors0, true, 30, durationPerTest);
        
        // cleanup
        st.destroy(gl);
        
        GLSLMiscHelper.destroyWindow(winctx);
    }

    @Test
    public void testShaderState01PerformanceDouble() throws InterruptedException {
        // preset ..
        GLSLMiscHelper.WindowContext winctx = GLSLMiscHelper.createWindow(GLProfile.getGL2ES2(), false);
        GLDrawable drawable = winctx.context.getGLDrawable();
        GL2ES2 gl = winctx.context.getGL().getGL2ES2();
        
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        // test code ..        
        ShaderState st = new ShaderState();
        
        ShaderCode rsVp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "RedSquareShader");
        ShaderCode rsFp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "RedSquareShader");
        ShaderCode rsFp1 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "RedSquareShader2");

        ShaderProgram sp1 = new ShaderProgram();
        sp1.add(rsVp0);
        sp1.add(rsFp1);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertTrue(0>sp1.program());        
        sp1.init(gl);
        Assert.assertTrue(sp1.link(gl, System.err));
        
        ShaderProgram sp0 = new ShaderProgram();
        sp0.add(rsVp0);
        sp0.add(rsFp0);
        
        sp0.init(gl);
        Assert.assertTrue(sp0.link(gl, System.err));
        
        st.attachShaderProgram(gl, sp0);        
        st.useProgram(gl, true);
        
        // setup gcu_PMVMatrix
        PMVMatrix pmvMatrix = new PMVMatrix();
        GLUniformData pmvMatrixUniform = new GLUniformData("gcu_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);
        
        // Allocate Vertex Array0
        GLArrayDataServer vertices0 = GLSLMiscHelper.createRSVertices0(gl, st, -1);
        st.ownAttribute(vertices0, true);
        vertices0.enableBuffer(gl, false);
        
        // Allocate Vertex Array1
        GLArrayDataServer vertices1 = GLSLMiscHelper.createRSVertices1(gl, st);
        st.ownAttribute(vertices1, true);
        vertices1.enableBuffer(gl, false);
        
        // Allocate Color Array0
        GLArrayDataServer colors0 = GLSLMiscHelper.createRSColors0(gl, st, -1);
        st.ownAttribute(colors0, true);
        colors0.enableBuffer(gl, false);
        
        // Allocate Color Array1
        GLArrayDataServer colors1 = GLSLMiscHelper.createRSColors1(gl, st);
        st.ownAttribute(colors1, true);
        colors1.enableBuffer(gl, false);
        
        // misc GL setup
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);

        // reshape
        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0F, (float) drawable.getWidth() / (float) drawable.getHeight(), 1.0F, 100.0F);
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        st.uniform(gl, pmvMatrixUniform);

        // validation ..
        st.attachShaderProgram(gl, sp0);
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices0, colors0, true, 1, 0);
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices1, colors1, true, 2, 0);
        st.attachShaderProgram(gl, sp1);
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices0, colors0, true, 1, 0);
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices1, colors1, true, 2, 0);
        
        long t0 = System.currentTimeMillis();
        int frames;
        // warmup ..
        for(frames=0; frames<GLSLMiscHelper.frames_warmup; frames+=2) {
            // SP0
            st.attachShaderProgram(gl, sp0);
            GLSLMiscHelper.displayVCArraysNoChecks(drawable, gl, true, vertices0, colors0, true);
            // SP1
            st.attachShaderProgram(gl, sp1);
            GLSLMiscHelper.displayVCArraysNoChecks(drawable, gl, true, vertices1, colors1, true);
        }        
        // measure ..
        for(frames=0; frames<GLSLMiscHelper.frames_perftest; frames+=4) {
            // SP0
            st.attachShaderProgram(gl, sp0);
            GLSLMiscHelper.displayVCArraysNoChecks(drawable, gl, true, vertices0, colors0, true);
            GLSLMiscHelper.displayVCArraysNoChecks(drawable, gl, true, vertices1, colors1, true);            
            // SP1
            st.attachShaderProgram(gl, sp1);
            GLSLMiscHelper.displayVCArraysNoChecks(drawable, gl, true, vertices0, colors0, true);
            GLSLMiscHelper.displayVCArraysNoChecks(drawable, gl, true, vertices1, colors1, true);
        }
        
        final long t1 = System.currentTimeMillis();
        final long dt = t1 - t0;        
        final double fps = ( frames * 1000.0 ) / (double) dt;
        final String fpsS = String.valueOf(fps);        
        final int fpsSp = fpsS.indexOf('.');        
        System.err.println("testShaderState01PerformanceDouble: "+dt/1000.0 +"s: "+ frames + "f, " + fpsS.substring(0, fpsSp+2) + " fps, "+dt/frames+" ms/f");        
        
        // cleanup
        st.destroy(gl);
        GLSLMiscHelper.destroyWindow(winctx);
    }
    
    public static void main(String args[]) throws IOException {
        System.err.println("main - start");
        boolean wait = false;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            }
            if(args[i].equals("-wait")) {
                wait = true;
            }
        }
        if(wait) {
            while(-1 == System.in.read()) ;
            TestGLSLShaderState02NEWT tst = new TestGLSLShaderState02NEWT();
            try {
                tst.testShaderState01PerformanceDouble();
            } catch (Exception e) {
                e.printStackTrace();
            }            
        } else {
            String tstname = TestGLSLShaderState02NEWT.class.getName();
            org.junit.runner.JUnitCore.main(tstname);
            System.err.println("main - end");
        }
    }    
}
