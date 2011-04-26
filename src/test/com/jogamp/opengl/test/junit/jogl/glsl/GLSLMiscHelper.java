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

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderState;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;

import org.junit.Assert;

public class GLSLMiscHelper {
    public static final int frames_perftest =  10000; // frames
    public static final int frames_warmup   =    500; // frames
    
    public static class WindowContext {        
        public final Window window;
        public final GLContext context;
        
        public WindowContext(Window w, GLContext c) {
            window = w;
            context = c;
        }
    }       
    
    public static WindowContext createWindow(GLProfile glp, boolean debugGL) {        
        GLCapabilities caps = new GLCapabilities(glp);
        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);

        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);

        Window window = NewtFactory.createWindow(screen, caps);
        Assert.assertNotNull(window);
        window.setSize(480, 480);
        window.setVisible(true);

        GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
        GLDrawable drawable = factory.createGLDrawable(window);
        Assert.assertNotNull(drawable);
        
        drawable.setRealized(true);
        
        GLContext context = drawable.createContext(null);
        Assert.assertNotNull(context);
        
        context.enableGLDebugMessage(debugGL);
        
        int res = context.makeCurrent();
        Assert.assertTrue(GLContext.CONTEXT_CURRENT_NEW==res || GLContext.CONTEXT_CURRENT==res);
        
        return new WindowContext(window, context);
    }

    public static void destroyWindow(WindowContext winctx) {
        GLDrawable drawable = winctx.context.getGLDrawable();
        
        Assert.assertNotNull(winctx.context);
        winctx.context.destroy();

        Assert.assertNotNull(drawable);
        drawable.setRealized(false);

        Assert.assertNotNull(winctx.window);
        winctx.window.destroy();
    }
        
    public static void validateGLArrayDataServerState(GL2ES2 gl, ShaderState st, GLArrayDataServer data) {
        int[] qi = new int[1];
        if(null != st) {            
            Assert.assertEquals(data, st.getAttribute(data.getName()));            
            if(st.shaderProgram().linked()) {
                Assert.assertEquals(data.getLocation(), st.getAttribLocation(data.getName()));
                Assert.assertEquals(data.getLocation(), st.getAttribLocation(gl, data));
                Assert.assertEquals(data.getLocation(), st.getAttribLocation(gl, data.getName()));
                Assert.assertEquals(data.getLocation(), gl.glGetAttribLocation(st.shaderProgram().program(), data.getName()));                
            }
        }
        gl.glGetVertexAttribiv(data.getLocation(), GL2ES2.GL_VERTEX_ATTRIB_ARRAY_ENABLED, qi, 0);
        Assert.assertEquals(data.enabled()?GL.GL_TRUE:GL.GL_FALSE, qi[0]);
        gl.glGetVertexAttribiv(data.getLocation(), GL2ES2.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING, qi, 0);
        Assert.assertEquals(data.getVBOName(), qi[0]);  
        Assert.assertEquals(data.getByteSize(), gl.glGetBufferSize(data.getVBOName()));        
    }

    public static void pause(long ms) throws InterruptedException {
        long t0 = System.currentTimeMillis();
        while( System.currentTimeMillis() - t0 < ms) {
            Thread.sleep(ms);
        }        
    }
    
    public static void displayVCArrays(GLDrawable drawable, GL2ES2 gl, ShaderState st, boolean preEnable, GLArrayDataServer vertices, GLArrayDataServer colors, boolean postDisable, int num, long postDelay) throws InterruptedException {
        System.err.println("screen #"+num);
        if(preEnable) {
            vertices.enableBuffer(gl, true);
            // invalid - Assert.assertEquals(vertices.getVBOName(), gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER));
            colors.enableBuffer(gl, true);
            // invalid - Assert.assertEquals(colors.getVBOName(), gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER));
            //
            // Above assertions are invalid, since GLSLArrayHandler will not bind the VBO to target
            // if the VBO is already bound to the attribute itself.
            // validateGLArrayDataServerState(..) does check proper VBO to attribute binding.
        }
        Assert.assertTrue(vertices.enabled());
        Assert.assertTrue(colors.enabled());
        
        validateGLArrayDataServerState(gl, st, vertices);
        validateGLArrayDataServerState(gl, st, colors);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);        
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        if(postDisable) {
            vertices.enableBuffer(gl, false);
            colors.enableBuffer(gl, false);
            Assert.assertTrue(!vertices.enabled());
            Assert.assertTrue(!colors.enabled());
        }
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        drawable.swapBuffers();
        if(postDelay>0) { pause(postDelay); }        
    }
    
    public static void displayVCArraysNoChecks(GLDrawable drawable, GL2ES2 gl, boolean preEnable, GLArrayDataServer vertices, GLArrayDataServer colors, boolean postDisable) throws InterruptedException {
        if(preEnable) {
            vertices.enableBuffer(gl, true);
            colors.enableBuffer(gl, true);
        }
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);        
        if(postDisable) {
            vertices.enableBuffer(gl, false);
            colors.enableBuffer(gl, false);
        }
        drawable.swapBuffers();
    }
    
    public static GLArrayDataServer createRSVertices0(GL2ES2 gl, ShaderState st, int location) {        
        // Allocate Vertex Array0
        GLArrayDataServer vertices0 = GLArrayDataServer.createGLSL(st, "mgl_Vertex", 3, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        if(0<=location) {
            st.bindAttribLocation(gl, location, vertices0);
        }
        Assert.assertTrue(vertices0.isVBO());
        Assert.assertTrue(vertices0.isVertexAttribute());
        Assert.assertTrue(!vertices0.isVBOWritten());
        Assert.assertTrue(!vertices0.sealed());
        vertices0.putf(-2); vertices0.putf(2);  vertices0.putf(0);
        vertices0.putf(2);  vertices0.putf(2);  vertices0.putf(0);
        vertices0.putf(-2); vertices0.putf(-2); vertices0.putf(0);
        vertices0.putf(2);  vertices0.putf(-2); vertices0.putf(0);
        vertices0.seal(gl, true);
        Assert.assertTrue(vertices0.isVBOWritten());
        Assert.assertTrue(vertices0.sealed());
        Assert.assertEquals(4, vertices0.getElementNumber());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());        
        Assert.assertEquals(vertices0.getVBOName(), gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER));
        validateGLArrayDataServerState(gl, st, vertices0);
        return vertices0;
    }
        
    public static GLArrayDataServer createRSVertices1(GL2ES2 gl, ShaderState st) {        
        GLArrayDataServer vertices1 = GLArrayDataServer.createGLSL(st, "mgl_Vertex", 3, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW); 
        Assert.assertTrue(vertices1.isVBO());
        Assert.assertTrue(vertices1.isVertexAttribute());
        Assert.assertTrue(!vertices1.isVBOWritten());
        Assert.assertTrue(!vertices1.sealed());
        vertices1.putf(-2); vertices1.putf(1);  vertices1.putf(0);
        vertices1.putf(2);  vertices1.putf(1);  vertices1.putf(0);
        vertices1.putf(-2); vertices1.putf(-1); vertices1.putf(0);
        vertices1.putf(2);  vertices1.putf(-1); vertices1.putf(0);
        vertices1.seal(gl, true);
        Assert.assertTrue(vertices1.isVBOWritten());
        Assert.assertTrue(vertices1.sealed());
        Assert.assertEquals(4, vertices1.getElementNumber());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertEquals(vertices1.getVBOName(), gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER));
        validateGLArrayDataServerState(gl, st, vertices1);
        return vertices1;
    }
        
    public static GLArrayDataServer createRSColors0(GL2ES2 gl, ShaderState st, int location) {        
        GLArrayDataServer colors0 = GLArrayDataServer.createGLSL(st, "mgl_Color", 4, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        if(0<=location) {
            st.bindAttribLocation(gl, location, colors0);
        }        
        colors0.putf(1); colors0.putf(0); colors0.putf(0); colors0.putf(1);
        colors0.putf(0); colors0.putf(0); colors0.putf(1); colors0.putf(1);
        colors0.putf(1); colors0.putf(0); colors0.putf(0); colors0.putf(1);
        colors0.putf(1); colors0.putf(0); colors0.putf(0); colors0.putf(1);
        colors0.seal(gl, true);
        Assert.assertTrue(colors0.isVBO());
        Assert.assertTrue(colors0.isVertexAttribute());
        Assert.assertTrue(colors0.isVBOWritten());
        Assert.assertTrue(colors0.sealed());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertEquals(colors0.getVBOName(), gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER));
        validateGLArrayDataServerState(gl, st, colors0);
        return colors0;
    }
    
    public static GLArrayDataServer createRSColors1(GL2ES2 gl, ShaderState st) {        
        // Allocate Color Array1
        GLArrayDataServer colors1 = GLArrayDataServer.createGLSL(st, "mgl_Color", 4, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        colors1.putf(1); colors1.putf(0); colors1.putf(1); colors1.putf(1);
        colors1.putf(0); colors1.putf(1); colors1.putf(0); colors1.putf(1);
        colors1.putf(1); colors1.putf(0); colors1.putf(1); colors1.putf(1);
        colors1.putf(1); colors1.putf(0); colors1.putf(1); colors1.putf(1);
        colors1.seal(gl, true);
        Assert.assertTrue(colors1.isVBO());
        Assert.assertTrue(colors1.isVertexAttribute());
        Assert.assertTrue(colors1.isVBOWritten());
        Assert.assertTrue(colors1.sealed());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertEquals(colors1.getVBOName(), gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER));
        validateGLArrayDataServerState(gl, st, colors1);
        return colors1;        
    }    
}
