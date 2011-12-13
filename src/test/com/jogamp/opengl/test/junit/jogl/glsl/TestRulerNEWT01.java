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

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.ScreenMode;
import com.jogamp.newt.util.MonitorMode;
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
import java.nio.FloatBuffer;

import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLUniformData;

import org.junit.Assert;
import org.junit.Test;
import org.junit.BeforeClass;

public class TestRulerNEWT01 extends UITestCase {
    static long durationPerTest = 10; // ms

    @Test
    public void test01() throws InterruptedException {
        long t0 = System.nanoTime();
        GLProfile.initSingleton();
        long t1 = System.nanoTime();
        // preset ..
        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createOnscreenWindow(GLProfile.getGL2ES2(), 640, 480, true);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL2ES2 gl = winctx.context.getGL().getGL2ES2();
        System.err.println(winctx.context);

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        // test code ..        
        final ShaderState st = new ShaderState();
        
        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, RedSquareES2.class,
                "shader", "shader/bin", "default");
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, RedSquareES2.class,
                "shader", "shader/bin", "ruler");

        final ShaderProgram sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);       
        Assert.assertTrue(0<=sp0.program()); 
        Assert.assertTrue(!sp0.inUse());
        Assert.assertTrue(!sp0.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        st.attachShaderProgram(gl, sp0);
        st.useProgram(gl, true);
        
        final PMVMatrix pmvMatrix = new PMVMatrix();
        final GLUniformData pmvMatrixUniform = new GLUniformData("gcu_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        st.ownUniform(pmvMatrixUniform);       
        st.uniform(gl, pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
         
        final GLUniformData rulerColor= new GLUniformData("gcu_RulerColor", 3, Buffers.newDirectFloatBuffer(3));
        final FloatBuffer rulerColorV = (FloatBuffer) rulerColor.getBuffer();
        rulerColorV.put(0, 0.5f);
        rulerColorV.put(1, 0.5f);
        rulerColorV.put(2, 0.5f);
        st.ownUniform(rulerColor);       
        st.uniform(gl, rulerColor);        
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        Assert.assertNotNull(winctx);
        Assert.assertNotNull(winctx.window);
        Assert.assertNotNull(winctx.window.getScreen());
        ScreenMode sm = winctx.window.getScreen().getCurrentScreenMode();
        Assert.assertNotNull(sm);
        System.err.println(sm);
        final MonitorMode mmode = sm.getMonitorMode();
        final DimensionImmutable sdim = mmode.getScreenSizeMM();
        final DimensionImmutable spix = mmode.getSurfaceSize().getResolution();   
        final GLUniformData rulerPixFreq = new GLUniformData("gcu_RulerPixFreq", 2, Buffers.newDirectFloatBuffer(2));
        final FloatBuffer rulerPixFreqV = (FloatBuffer) rulerPixFreq.getBuffer();
        rulerPixFreqV.put(0, (float)spix.getWidth() / (float)sdim.getWidth() * 10.0f);
        rulerPixFreqV.put(1, (float)spix.getHeight() / (float)sdim.getHeight() * 10.0f);
        st.ownUniform(rulerPixFreq);
        st.uniform(gl, rulerPixFreq);        
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        System.err.println("Screen dim "+sdim);
        System.err.println("Screen siz "+spix);
        System.err.println("Screen pixel/cm "+rulerPixFreqV.get(0)+", "+rulerPixFreqV.get(1));

        final GLArrayDataServer vertices0 = GLArrayDataServer.createGLSL("gca_Vertices", 3, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        vertices0.putf(0); vertices0.putf(1);  vertices0.putf(0);
        vertices0.putf(1);  vertices0.putf(1);  vertices0.putf(0);
        vertices0.putf(0); vertices0.putf(0); vertices0.putf(0);
        vertices0.putf(1);  vertices0.putf(0); vertices0.putf(0);
        vertices0.seal(gl, true);
        st.ownAttribute(vertices0, true);
        
        // misc GL setup
        gl.glClearColor(1, 1, 1, 1);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // reshape
        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glOrthof(0f, 1f, 0f, 1f, -10f, 10f);
        // pmvMatrix.gluPerspective(45.0F, (float) drawable.getWidth() / (float) drawable.getHeight(), 1.0F, 100.0F);
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        // pmvMatrix.glTranslatef(0, 0, -6);
        // pmvMatrix.glRotatef(45f, 1f, 0f, 0f);
        st.uniform(gl, pmvMatrixUniform);
        gl.glViewport(0, 0, drawable.getWidth(), drawable.getHeight());        
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        for(int i=0; i<10; i++) {
            vertices0.enableBuffer(gl, true);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);        
            vertices0.enableBuffer(gl, false);
            drawable.swapBuffers();
            Thread.sleep(durationPerTest/10);
        }
        
        long t2 = System.nanoTime();
        
        NEWTGLContext.destroyWindow(winctx);
        
        long t3 = System.nanoTime();
        
        System.err.println("t1-t0: "+ (t1-t0)/1e6 +"ms"); 
        System.err.println("t3-t0: "+ (t3-t0)/1e6 +"ms"); 
        System.err.println("t3-t2: "+ (t3-t2)/1e6 +"ms"); 
    }
    
    public static void main(String args[]) throws IOException {
        System.err.println("main - start");
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            }
        }
        String tstname = TestRulerNEWT01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
        System.err.println("main - end");
    }    
}

