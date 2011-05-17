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

import com.jogamp.opengl.util.FBObject;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquare0;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NEWTGLContext;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.io.IOException;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLUniformData;

import org.junit.Assert;
import org.junit.Test;

public class TestFBOMRTNEWT01 extends UITestCase {
    static long durationPerTest = 10; // ms

    @Test
    public void test01() throws InterruptedException {
        // preset ..
        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createWindow(GLProfile.getGL2ES2(), 640, 480, true);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL _gl = winctx.context.getGL();
        Assert.assertTrue(_gl.isGL2GL3());
        final GL2GL3 gl = _gl.getGL2GL3();
        System.err.println(winctx.context);

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        // test code ..        
        final ShaderState st = new ShaderState();
        // st.setVerbose(true);
        
        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "fbo-mrt-1");
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "fbo-mrt-1");
        final ShaderProgram sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);       
        Assert.assertTrue(0<=sp0.program()); 
        Assert.assertTrue(!sp0.inUse());
        Assert.assertTrue(!sp0.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());        
        st.attachShaderProgram(gl, sp0);
        
        final ShaderCode vp1 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "fbo-mrt-2");
        final ShaderCode fp1 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "fbo-mrt-2");
        final ShaderProgram sp1 = new ShaderProgram();
        sp1.add(gl, vp1, System.err);
        sp1.add(gl, fp1, System.err);       
        Assert.assertTrue(0<=sp1.program()); 
        Assert.assertTrue(!sp1.inUse());
        Assert.assertTrue(!sp1.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());        
        st.attachShaderProgram(gl, sp1);
        st.useProgram(gl, true);
                        
        final PMVMatrix pmvMatrix = new PMVMatrix();
        final GLUniformData pmvMatrixUniform = new GLUniformData("gcu_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        st.ownUniform(pmvMatrixUniform);       
        st.uniform(gl, pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        final GLArrayDataServer vertices0 = GLArrayDataServer.createGLSL(st, "gca_Vertices", 3, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        // st.bindAttribLocation(gl, 0, vertices0);
        vertices0.putf(0); vertices0.putf(1);  vertices0.putf(0);
        vertices0.putf(1);  vertices0.putf(1);  vertices0.putf(0);
        vertices0.putf(0); vertices0.putf(0); vertices0.putf(0);
        vertices0.putf(1);  vertices0.putf(0); vertices0.putf(0);
        vertices0.seal(gl, true);
        st.ownAttribute(vertices0, true);
        vertices0.enableBuffer(gl, false);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        final GLArrayDataServer colors0 = GLArrayDataServer.createGLSL(st, "gca_Colors", 4, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
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
                
        final GLArrayDataServer texCoords0 = GLArrayDataServer.createGLSL(st, "gca_TexCoords", 2, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        // st.bindAttribLocation(gl, 2, texCoords0);
        texCoords0.putf(0f); texCoords0.putf(1f);
        texCoords0.putf(1f);  texCoords0.putf(1f);
        texCoords0.putf(0f); texCoords0.putf(0f);
        texCoords0.putf(1f);  texCoords0.putf(0f);        
        texCoords0.seal(gl, true);
        st.ownAttribute(texCoords0, true);
        texCoords0.enableBuffer(gl, false);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // FBO w/ 2 texture2D color buffers
        final FBObject fbo_mrt = new FBObject(drawable.getWidth(), drawable.getHeight());
        fbo_mrt.init(gl);
        Assert.assertTrue( 0 == fbo_mrt.attachTexture2D(gl, texUnit0.intValue(), GL.GL_NEAREST, GL.GL_NEAREST, GL2ES2.GL_CLAMP_TO_EDGE, GL2ES2.GL_CLAMP_TO_EDGE) );
        Assert.assertTrue( 1 == fbo_mrt.attachTexture2D(gl, texUnit1.intValue(), GL.GL_NEAREST, GL.GL_NEAREST, GL2ES2.GL_CLAMP_TO_EDGE, GL2ES2.GL_CLAMP_TO_EDGE) );
        Assert.assertTrue( fbo_mrt.attachDepthBuffer(gl, GL.GL_DEPTH_COMPONENT16) );
        Assert.assertTrue( fbo_mrt.isStatusValid() ) ;
        fbo_mrt.unbind(gl);
        
        // misc GL setup
        gl.glClearColor(1, 1, 1, 1);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // reshape
        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glOrthof(0f, 1f, 0f, 1f, -10f, 10f);
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        st.uniform(gl, pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        final int[] two_buffers = new int[] { GL.GL_COLOR_ATTACHMENT0, GL.GL_COLOR_ATTACHMENT0+1 };
        final int[] bck_buffers = new int[] { GL2GL3.GL_BACK_LEFT };
        
        for(int i=0; i<durationPerTest; i+=50) {
            // pass 1 - MRT: Red -> buffer0, Green -> buffer1
            st.attachShaderProgram(gl, sp0);           
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
            st.attachShaderProgram(gl, sp1);
            vertices0.enableBuffer(gl, true);
            colors0.enableBuffer(gl, true);
            texCoords0.enableBuffer(gl, true);
            gl.glDrawBuffers(1, bck_buffers, 0);
            
            gl.glViewport(0, 0, drawable.getWidth(), drawable.getHeight());
            fbo_mrt.use(gl, 0);
            fbo_mrt.use(gl, 1);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
            fbo_mrt.unuse(gl);
            vertices0.enableBuffer(gl, false);
            colors0.enableBuffer(gl, false);
            texCoords0.enableBuffer(gl, false);
            
            drawable.swapBuffers();
            Thread.sleep(50);
        }
        
        NEWTGLContext.destroyWindow(winctx);
    }
    
    public static void main(String args[]) throws IOException {
        System.err.println("main - start");
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            }
        }
        String tstname = TestFBOMRTNEWT01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
        System.err.println("main - end");
    }    
}

