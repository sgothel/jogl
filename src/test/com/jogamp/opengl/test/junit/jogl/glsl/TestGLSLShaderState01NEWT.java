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

import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NEWTGLContext;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.io.IOException;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Testing different vertex attribute (VA) data sets on one shader
 * and shader state in general.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLSLShaderState01NEWT extends UITestCase {
    static long durationPerTest = 10; // ms
    static boolean firstUIActionOnProcess = false;

    static final int vertices0_loc = 0; // FIXME: AMD needs this to be location 0 ? hu ?
    static final int colors0_loc = 1;

    @Test
    public void test00NoShaderState_Validation() throws InterruptedException {
        // preset ..
        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createWindow(
                new GLCapabilities(GLProfile.getGL2ES2()), 480, 480, true);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL2ES2 gl = winctx.context.getGL().getGL2ES2();
        System.err.println(winctx.context);

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // test code ..
        final ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, RedSquareES2.class,
                "shader", "shader/bin", "RedSquareShader", true);
        final ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, RedSquareES2.class,
                "shader", "shader/bin", "RedSquareShader", true);
        rsVp.defaultShaderCustomization(gl, true, true);
        rsFp.defaultShaderCustomization(gl, true, true);

        final ShaderProgram sp = new ShaderProgram();
        Assert.assertTrue(0 == sp.program());

        sp.add(gl, rsVp, System.err);
        sp.add(gl, rsFp, System.err);

        Assert.assertTrue(0 != sp.program());
        Assert.assertTrue(!sp.inUse());
        Assert.assertTrue(!sp.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        Assert.assertTrue( sp.link(gl, System.err) );
        sp.useProgram(gl, true);

        // Allocate Vertex Array0
        final GLArrayDataServer vertices0 = GLSLMiscHelper.createVertices(gl, null, sp.program(), vertices0_loc, GLSLMiscHelper.vertices0);
        System.err.println("vertices0: " + vertices0);
        vertices0.enableBuffer(gl, false);
        Assert.assertEquals(vertices0_loc, vertices0.getLocation());

        // Allocate Color Array0
        final GLArrayDataServer colors0 = GLSLMiscHelper.createColors(gl, null, sp.program(), colors0_loc, GLSLMiscHelper.colors0);
        System.err.println("colors0: " + colors0);
        colors0.enableBuffer(gl, false);
        Assert.assertEquals(colors0_loc, colors0.getLocation());

        Assert.assertTrue(sp.link(gl, System.err));
        Assert.assertTrue(sp.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        Assert.assertEquals(vertices0_loc, vertices0.getLocation());
        GLSLMiscHelper.validateGLArrayDataServerState(gl, null, vertices0);

        Assert.assertEquals(colors0_loc, colors0.getLocation());
        GLSLMiscHelper.validateGLArrayDataServerState(gl, null, colors0);

        sp.useProgram(gl, true);
        Assert.assertTrue(sp.inUse());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // setup mgl_PMVMatrix
        final PMVMatrix pmvMatrix = new PMVMatrix();
        final GLUniformData pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        pmvMatrixUniform.setLocation(gl, sp.program());
        gl.glUniform(pmvMatrixUniform);

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // Allocate Vertex Array1
        final GLArrayDataServer vertices1 = GLSLMiscHelper.createVertices(gl, null, sp.program(), -1, GLSLMiscHelper.vertices1);
        System.err.println("vertices1: " + vertices1);
        vertices1.enableBuffer(gl, false);
        GLSLMiscHelper.validateGLArrayDataServerState(gl, null, vertices1);

        // Allocate Color Array1
        final GLArrayDataServer colors1 = GLSLMiscHelper.createColors(gl, null, sp.program(), -1, GLSLMiscHelper.colors1);
        System.err.println("colors1: " + colors1);
        colors1.enableBuffer(gl, false);
        GLSLMiscHelper.validateGLArrayDataServerState(gl, null, colors1);

        // misc GL setup
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // reshape
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0F, (float) drawable.getSurfaceWidth() / (float) drawable.getSurfaceHeight(), 1.0F, 100.0F);
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        gl.glUniform(pmvMatrixUniform);

        gl.glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // display #1 vertices0 / colors0 (post-disable)
        GLSLMiscHelper.displayVCArrays(drawable, gl, null, true, vertices0, colors0, true, 1, durationPerTest);

        // display #2 #1 vertices1 / colors1 (post-disable)
        GLSLMiscHelper.displayVCArrays(drawable, gl, null, true, vertices1, colors1, true, 2, durationPerTest);

        // display #3 vertices0 / colors0 (post-disable)
        GLSLMiscHelper.displayVCArrays(drawable, gl, null, true, vertices0, colors0, true, 3, durationPerTest);

        // cleanup
        sp.useProgram(gl, false);
        sp.destroy(gl);
        vertices1.destroy(gl);
        colors0.destroy(gl);
        colors1.destroy(gl);

        NEWTGLContext.destroyWindow(winctx);
    }

    @Test
    public void test01ShaderState_Validation() throws InterruptedException {
        // preset ..
        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createWindow(
                new GLCapabilities(GLProfile.getGL2ES2()), 480, 480, true);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL2ES2 gl = winctx.context.getGL().getGL2ES2();
        System.err.println(winctx.context);

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // test code ..
        final ShaderState st = new ShaderState();

        final ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, RedSquareES2.class, "shader",
                "shader/bin", "RedSquareShader", true);
        final ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, RedSquareES2.class, "shader",
                "shader/bin", "RedSquareShader", true);
        rsVp.defaultShaderCustomization(gl, true, true);
        rsFp.defaultShaderCustomization(gl, true, true);

        final ShaderProgram sp = new ShaderProgram();
        Assert.assertTrue(0 == sp.program());

        sp.add(gl, rsVp, System.err);
        sp.add(gl, rsFp, System.err);

        Assert.assertTrue(0 != sp.program());
        Assert.assertTrue(!sp.inUse());
        Assert.assertTrue(!sp.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        st.attachShaderProgram(gl, sp, false);
        Assert.assertTrue(!sp.inUse());
        Assert.assertTrue(!sp.linked());

        // Allocate Vertex Array0
        final GLArrayDataServer vertices0 = GLSLMiscHelper.createVertices(gl, st, 0, vertices0_loc, GLSLMiscHelper.vertices0);
        System.err.println("vertices0: " + vertices0);
        vertices0.enableBuffer(gl, false);
        Assert.assertEquals(vertices0_loc, vertices0.getLocation());

        // Allocate Color Array0
        final GLArrayDataServer colors0 = GLSLMiscHelper.createColors(gl, st, 0, colors0_loc, GLSLMiscHelper.colors0);
        System.err.println("colors0: " + colors0);
        colors0.enableBuffer(gl, false);
        Assert.assertEquals(colors0_loc, colors0.getLocation());

        Assert.assertTrue(sp.link(gl, System.err));
        Assert.assertTrue(sp.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        Assert.assertEquals(vertices0_loc, vertices0.getLocation());
        GLSLMiscHelper.validateGLArrayDataServerState(gl, st, vertices0);

        Assert.assertEquals(colors0_loc, colors0.getLocation());
        GLSLMiscHelper.validateGLArrayDataServerState(gl, st, colors0);

        st.useProgram(gl, true);
        Assert.assertTrue(sp.inUse());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // setup mgl_PMVMatrix
        final PMVMatrix pmvMatrix = new PMVMatrix();
        final GLUniformData pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        st.ownUniform(pmvMatrixUniform);

        st.uniform(gl, pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertEquals(pmvMatrixUniform, st.getUniform("mgl_PMVMatrix"));

        // Allocate Vertex Array1
        final GLArrayDataServer vertices1 = GLSLMiscHelper.createVertices(gl, st, 0, -1, GLSLMiscHelper.vertices1);
        System.err.println("vertices1: " + vertices1);
        vertices1.enableBuffer(gl, false);
        GLSLMiscHelper.validateGLArrayDataServerState(gl, st, vertices1);

        // Allocate Color Array1
        final GLArrayDataServer colors1 = GLSLMiscHelper.createColors(gl, st, 0, -1, GLSLMiscHelper.colors1);
        System.err.println("colors1: " + colors1);
        colors1.enableBuffer(gl, false);
        GLSLMiscHelper.validateGLArrayDataServerState(gl, st, colors1);

        // misc GL setup
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // reshape
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0F, (float) drawable.getSurfaceWidth() / (float) drawable.getSurfaceHeight(), 1.0F, 100.0F);
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        st.uniform(gl, pmvMatrixUniform);
        gl.glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // display #1 vertices0 / colors0 (post-disable)
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices0, colors0, true, 1, durationPerTest);

        // display #2 #1 vertices1 / colors1 (post-disable)
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices1, colors1, true, 2, durationPerTest);

        // display #3 vertices0 / colors0 (post-disable)
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices0, colors0, true, 3, durationPerTest);

        // cleanup
        st.destroy(gl);

        NEWTGLContext.destroyWindow(winctx);
    }

    @Test(timeout=240000)
    public void test02ShaderState_PerformanceSingleKeepEnabled() throws InterruptedException {
        testShaderState_PerformanceSingleImpl(false);
    }
    @Test(timeout=240000)
    public void test03ShaderState_PerformanceSingleToggleEnable() throws InterruptedException {
        testShaderState_PerformanceSingleImpl(true);
    }

    private void testShaderState_PerformanceSingleImpl(final boolean toggleEnable) throws InterruptedException {
        // preset ..
        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createWindow(
                new GLCapabilities(GLProfile.getGL2ES2()), 480, 480, false);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL2ES2 gl = winctx.context.getGL().getGL2ES2();
        System.err.println(winctx.context);
        gl.setSwapInterval(0);

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // test code ..
        final ShaderState st = new ShaderState();

        final ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, RedSquareES2.class, "shader",
                "shader/bin", "RedSquareShader", true);
        final ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, RedSquareES2.class, "shader",
                "shader/bin", "RedSquareShader", true);
        rsVp.defaultShaderCustomization(gl, true, true);
        rsFp.defaultShaderCustomization(gl, true, true);

        final ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);

        sp.init(gl);
        Assert.assertTrue(sp.link(gl, System.err));

        st.attachShaderProgram(gl, sp, true);

        // setup mgl_PMVMatrix
        final PMVMatrix pmvMatrix = new PMVMatrix();
        final GLUniformData pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);

        // Allocate Vertex Array0
        final GLArrayDataServer vertices0 = GLSLMiscHelper.createVertices(gl, st, 0, -1, GLSLMiscHelper.vertices0);
        vertices0.enableBuffer(gl, toggleEnable ? false : true);

        // Allocate Color Array0
        final GLArrayDataServer colors0 = GLSLMiscHelper.createColors(gl, st, 0, -1, GLSLMiscHelper.colors0);
        colors0.enableBuffer(gl, toggleEnable ? false : true);

        // misc GL setup
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL.GL_DEPTH_TEST);

        // reshape
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0F, (float) drawable.getSurfaceWidth() / (float) drawable.getSurfaceHeight(), 1.0F, 100.0F);
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        st.uniform(gl, pmvMatrixUniform);
        gl.glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

        gl.setSwapInterval(0);

        // validation ..
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, toggleEnable, vertices0, colors0, toggleEnable, 1, 0);

        // warmup ..
        for(int frames=0; frames<GLSLMiscHelper.frames_warmup; frames++) {
            GLSLMiscHelper.displayVCArraysNoChecks(drawable, gl, toggleEnable, vertices0, colors0, toggleEnable);
        }

        // measure ..
        final long t0 = System.currentTimeMillis();
        int frames;

        for(frames=0; frames<GLSLMiscHelper.frames_perftest; frames++) {
            GLSLMiscHelper.displayVCArraysNoChecks(drawable, gl, toggleEnable, vertices0, colors0, toggleEnable);
        }
        final long t1 = System.currentTimeMillis();
        final long dt = t1 - t0;
        final double fps = ( frames * 1000.0 ) / dt;
        final String fpsS = String.valueOf(fps);
        final int fpsSp = fpsS.indexOf('.');
        System.err.println("testShaderState00PerformanceSingle toggleEnable "+toggleEnable+": "+dt/1000.0 +"s: "+ frames + "f, " + fpsS.substring(0, fpsSp+2) + " fps, "+dt/frames+" ms/f");

        // cleanup
        st.destroy(gl);

        NEWTGLContext.destroyWindow(winctx);
    }

    @Test(timeout=240000)
    public void test04ShaderState_PerformanceDouble() throws InterruptedException {
        // preset ..
        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createWindow(
                new GLCapabilities(GLProfile.getGL2ES2()), 480, 480, false);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL2ES2 gl = winctx.context.getGL().getGL2ES2();
        System.err.println(winctx.context);
        gl.setSwapInterval(0);

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // test code ..
        final ShaderState st = new ShaderState();

        final ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, RedSquareES2.class, "shader",
                "shader/bin", "RedSquareShader", true);
        final ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, RedSquareES2.class, "shader",
                "shader/bin", "RedSquareShader", true);
        rsVp.defaultShaderCustomization(gl, true, true);
        rsFp.defaultShaderCustomization(gl, true, true);

        final ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);

        sp.init(gl);
        Assert.assertTrue(sp.link(gl, System.err));

        st.attachShaderProgram(gl, sp, true);

        // setup mgl_PMVMatrix
        final PMVMatrix pmvMatrix = new PMVMatrix();
        final GLUniformData pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);

        // Allocate Vertex Array0
        final GLArrayDataServer vertices0 = GLSLMiscHelper.createVertices(gl, st, 0, -1, GLSLMiscHelper.vertices0);
        vertices0.enableBuffer(gl, false);

        // Allocate Vertex Array1
        final GLArrayDataServer vertices1 = GLSLMiscHelper.createVertices(gl, st, 0, -1, GLSLMiscHelper.vertices1);
        vertices1.enableBuffer(gl, false);

        // Allocate Color Array0
        final GLArrayDataServer colors0 = GLSLMiscHelper.createColors(gl, st, 0, -1, GLSLMiscHelper.colors0);
        colors0.enableBuffer(gl, false);

        // Allocate Color Array1
        final GLArrayDataServer colors1 = GLSLMiscHelper.createColors(gl, st, 0, -1, GLSLMiscHelper.colors1);
        colors1.enableBuffer(gl, false);

        // misc GL setup
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL.GL_DEPTH_TEST);

        // reshape
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0F, (float) drawable.getSurfaceWidth() / (float) drawable.getSurfaceHeight(), 1.0F, 100.0F);
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        st.uniform(gl, pmvMatrixUniform);
        gl.glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

        gl.setSwapInterval(0);

        // validation ..
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices0, colors0, true, 1, 0);
        GLSLMiscHelper.displayVCArrays(drawable, gl, st, true, vertices1, colors1, true, 2, 0);

        // warmup ..
        for(int frames=0; frames<GLSLMiscHelper.frames_warmup; frames+=2) {
            GLSLMiscHelper.displayVCArraysNoChecks(drawable, gl, true, vertices0, colors0, true);
            GLSLMiscHelper.displayVCArraysNoChecks(drawable, gl, true, vertices1, colors1, true);
        }

        // measure ..
        final long t0 = System.currentTimeMillis();
        int frames;

        for(frames=0; frames<GLSLMiscHelper.frames_perftest; frames+=2) {
            GLSLMiscHelper.displayVCArraysNoChecks(drawable, gl, true, vertices0, colors0, true);
            GLSLMiscHelper.displayVCArraysNoChecks(drawable, gl, true, vertices1, colors1, true);
        }
        final long t1 = System.currentTimeMillis();
        final long dt = t1 - t0;
        final double fps = ( frames * 1000.0 ) / dt;
        final String fpsS = String.valueOf(fps);
        final int fpsSp = fpsS.indexOf('.');
        System.err.println("testShaderState01PerformanceDouble: "+dt/1000.0 +"s: "+ frames + "f, " + fpsS.substring(0, fpsSp+2) + " fps, "+dt/frames+" ms/f");

        // cleanup
        st.destroy(gl);

        NEWTGLContext.destroyWindow(winctx);
    }

    public static void main(final String args[]) throws IOException {
        System.err.println("main - start");
        boolean wait = false;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            } else if(args[i].equals("-wait")) {
                wait = true;
            } else if(args[i].equals("-firstUIAction")) {
                firstUIActionOnProcess = true;
            }
        }
        if(wait) {
            while(-1 == System.in.read()) ;
            final TestGLSLShaderState01NEWT tst = new TestGLSLShaderState01NEWT();
            try {
                tst.test04ShaderState_PerformanceDouble();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else {
            final String tstname = TestGLSLShaderState01NEWT.class.getName();
            org.junit.runner.JUnitCore.main(tstname);
        }
        System.err.println("main - end");
    }
}
