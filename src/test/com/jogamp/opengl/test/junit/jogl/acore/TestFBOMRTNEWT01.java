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
package com.jogamp.opengl.test.junit.jogl.acore;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NEWTGLContext;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.io.IOException;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFBOMRTNEWT01 extends UITestCase {
    static long durationPerTest = 10*40*2; // ms

    @Test
    public void test01() throws InterruptedException {
        final int step = 4;
        final int width = 800;
        final int height = 600;
        // preset ..
        if(!GLProfile.isAvailable(GLProfile.GL2GL3)) {
            System.err.println("Test requires GL2/GL3 profile.");
            return;
        }
        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createWindow(
                new GLCapabilities(GLProfile.getGL2GL3()), width/step, height/step, true);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL2GL3 gl = winctx.context.getGL().getGL2GL3();
        // gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", null, gl, null) ).getGL2GL3();
        System.err.println(winctx.context);

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        // test code ..
        final ShaderState st = new ShaderState();
        // st.setVerbose(true);

        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, RedSquareES2.class, "shader",
                "shader/bin", "fbo-mrt-1", true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, RedSquareES2.class, "shader",
                "shader/bin", "fbo-mrt-1", true);
        vp0.defaultShaderCustomization(gl, true, true);
        fp0.defaultShaderCustomization(gl, true, true);
        final ShaderProgram sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);
        Assert.assertTrue(0 != sp0.program());
        Assert.assertTrue(!sp0.inUse());
        Assert.assertTrue(!sp0.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        st.attachShaderProgram(gl, sp0, false);

        final ShaderCode vp1 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, RedSquareES2.class, "shader",
                "shader/bin", "fbo-mrt-2", true);
        final ShaderCode fp1 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, RedSquareES2.class, "shader",
                "shader/bin", "fbo-mrt-2", true);
        vp1.defaultShaderCustomization(gl, true, true);
        fp1.defaultShaderCustomization(gl, true, true);
        final ShaderProgram sp1 = new ShaderProgram();
        sp1.add(gl, vp1, System.err);
        sp1.add(gl, fp1, System.err);
        Assert.assertTrue(0 != sp1.program());
        Assert.assertTrue(!sp1.inUse());
        Assert.assertTrue(!sp1.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        st.attachShaderProgram(gl, sp1, true);

        final PMVMatrix pmvMatrix = new PMVMatrix();
        final GLUniformData pmvMatrixUniform = new GLUniformData("gcu_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        final GLArrayDataServer vertices0 = GLArrayDataServer.createGLSL("gca_Vertices", 3, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        // st.bindAttribLocation(gl, 0, vertices0);
        vertices0.putf(0); vertices0.putf(1);  vertices0.putf(0);
        vertices0.putf(1);  vertices0.putf(1);  vertices0.putf(0);
        vertices0.putf(0); vertices0.putf(0); vertices0.putf(0);
        vertices0.putf(1);  vertices0.putf(0); vertices0.putf(0);
        vertices0.seal(gl, true);
        st.ownAttribute(vertices0, true);
        vertices0.enableBuffer(gl, false);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        final GLArrayDataServer colors0 = GLArrayDataServer.createGLSL("gca_Colors", 4, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        // st.bindAttribLocation(gl, 1, colors0);
        colors0.putf(1); colors0.putf(0);  colors0.putf(1); colors0.putf(1);
        colors0.putf(0);  colors0.putf(0);  colors0.putf(1); colors0.putf(1);
        colors0.putf(0); colors0.putf(0); colors0.putf(0); colors0.putf(1);
        colors0.putf(0);  colors0.putf(1); colors0.putf(1); colors0.putf(1);
        colors0.seal(gl, true);
        st.ownAttribute(colors0, true);
        colors0.enableBuffer(gl, false);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        final GLUniformData texUnit0 = new GLUniformData("gcs_TexUnit0", 0);
        st.ownUniform(texUnit0);
        st.uniform(gl, texUnit0);
        final GLUniformData texUnit1 = new GLUniformData("gcs_TexUnit1", 1);
        st.ownUniform(texUnit1);
        st.uniform(gl, texUnit1);

        final GLArrayDataServer texCoords0 = GLArrayDataServer.createGLSL("gca_TexCoords", 2, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        // st.bindAttribLocation(gl, 2, texCoords0);
        texCoords0.putf(0f); texCoords0.putf(1f);
        texCoords0.putf(1f);  texCoords0.putf(1f);
        texCoords0.putf(0f); texCoords0.putf(0f);
        texCoords0.putf(1f);  texCoords0.putf(0f);
        texCoords0.seal(gl, true);
        st.ownAttribute(texCoords0, true);
        texCoords0.enableBuffer(gl, false);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        final int texA0Point = 0; // attachment point for texA0
        final int texA1Point = 1; // attachment point for texA1

        // FBO w/ 2 texture2D color buffers
        final FBObject fbo_mrt = new FBObject();
        fbo_mrt.init(gl, drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), 0);
        final TextureAttachment texA0 = fbo_mrt.attachTexture2D(gl, texA0Point, true, GL.GL_NEAREST, GL.GL_NEAREST, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
        final TextureAttachment texA1;
        if(fbo_mrt.getMaxColorAttachments() > 1) {
            texA1 = fbo_mrt.attachTexture2D(gl, texA1Point, true, GL.GL_NEAREST, GL.GL_NEAREST, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
        } else {
            texA1 = null;
            System.err.println("FBO supports only one attachment, no MRT available!");
        }
        fbo_mrt.attachRenderbuffer(gl, Type.DEPTH, FBObject.CHOSEN_BITS);
        Assert.assertTrue( fbo_mrt.isStatusValid() ) ;
        fbo_mrt.unbind(gl);

        // misc GL setup
        gl.glClearColor(1, 1, 1, 1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // reshape
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glOrthof(0f, 1f, 0f, 1f, -10f, 10f);
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        st.uniform(gl, pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        final int[] two_buffers = new int[] { GL.GL_COLOR_ATTACHMENT0+texA0Point, GL.GL_COLOR_ATTACHMENT0+texA1Point };
        final int[] bck_buffers = new int[] { GL2GL3.GL_BACK_LEFT };

        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        int step_i = 0;
        final int[] last_snap_size = new int[] { 0, 0 };

        for(int i=0; i<durationPerTest; i+=50) {
            // pass 1 - MRT: Red -> buffer0, Green -> buffer1
            st.attachShaderProgram(gl, sp0, true);
            vertices0.enableBuffer(gl, true);
            colors0.enableBuffer(gl, true);

            fbo_mrt.bind(gl);
            gl.glDrawBuffers(2, two_buffers, 0);
            gl.glViewport(0, 0, fbo_mrt.getWidth(), fbo_mrt.getHeight());

            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
            fbo_mrt.unbind(gl);
            vertices0.enableBuffer(gl, false);
            colors0.enableBuffer(gl, false);

            // pass 2 - mix buffer0, buffer1 and blue
            // rg = buffer0.rg + buffer1.rg, b = Blue - length(rg);
            st.attachShaderProgram(gl, sp1, true);
            vertices0.enableBuffer(gl, true);
            colors0.enableBuffer(gl, true);
            texCoords0.enableBuffer(gl, true);
            gl.glDrawBuffers(1, bck_buffers, 0);

            gl.glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

            gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit0.intValue());
            fbo_mrt.use(gl, texA0);
            if(null != texA1) {
                gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit1.intValue());
                fbo_mrt.use(gl, texA1);
            }
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
            fbo_mrt.unuse(gl);
            vertices0.enableBuffer(gl, false);
            colors0.enableBuffer(gl, false);
            texCoords0.enableBuffer(gl, false);

            {
                final NativeSurface ns = gl.getContext().getGLReadDrawable().getNativeSurface();
                if(last_snap_size[0] != ns.getSurfaceWidth() && last_snap_size[1] != ns.getSurfaceHeight()) {
                    gl.glFinish(); // sync .. no swap buffers yet!
                    snapshot(step_i, null, gl, screenshot, TextureIO.PNG, null); // overwrite ok
                    last_snap_size[0] = ns.getSurfaceWidth();
                    last_snap_size[1] = ns.getSurfaceHeight();
                }
            }

            drawable.swapBuffers();
            Thread.sleep(50);
            final int j = (int) ( i / (durationPerTest/step) ) + 1;
            if(j>step_i) {
                final int w = width/step * j;
                final int h = height/step * j;
                System.err.println("resize: "+step_i+" -> "+j+" - "+w+"x"+h);
                fbo_mrt.reset(gl, w, h, 0);
                winctx.window.setSize(w, h);
                step_i = j;
            }
        }

        NEWTGLContext.destroyWindow(winctx);
    }

    public static void main(final String args[]) throws IOException {
        System.err.println("main - start");
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            }
        }
        final String tstname = TestFBOMRTNEWT01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
        System.err.println("main - end");
    }
}

