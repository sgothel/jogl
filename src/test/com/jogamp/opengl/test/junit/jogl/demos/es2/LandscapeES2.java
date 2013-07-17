/**
 * Copyright (C) 2013 JogAmp Community. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * BRIAN PAUL BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.jogamp.opengl.test.junit.jogl.demos.es2;

import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLUniformData;

/**
 * LandscapeES2
 */
public class LandscapeES2 implements GLEventListener {
    private int swapInterval = 0;
    private boolean verbose = true;

    static public int TARGET_FPS    = 120;
    private long millisOffset;
    private int frameCount;
    private float frameRate;
    private ShaderCode vertShader;
    private ShaderCode fragShader;
    private ShaderProgram shaderProg;
    private ShaderState shaderState;
    private float[] resolution; 
    private GLUniformData resolutionUni;
    private GLUniformData timeUni;  
    private GLArrayDataServer vertices;   
      
    private int fcount = 0, lastm = 0;  
    private int fint = 1;
      
    public LandscapeES2(int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public LandscapeES2() {
        this.swapInterval = 1;
    }

    public void setVerbose(boolean v) { verbose = v; }
    
    public void init(GLAutoDrawable drawable) {
        System.err.println(Thread.currentThread()+" LandscapeES2.init ...");
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        if(verbose) {
            System.err.println("LandscapeES2 init on "+Thread.currentThread());
            System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
            System.err.println("INIT GL IS: " + gl.getClass().getName());
            System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
            System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
            System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
            System.err.println("GL GLSL: "+gl.hasGLSL()+", has-compiler-func: "+gl.isFunctionAvailable("glCompileShader")+", version "+(gl.hasGLSL() ? gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION) : "none")+", "+gl.getContext().getGLSLVersionNumber());
            System.err.println("GL FBO: basic "+ gl.hasBasicFBOSupport()+", full "+gl.hasFullFBOSupport());
            System.err.println("GL Profile: "+gl.getGLProfile());
            System.err.println("GL Renderer Quirks:" + gl.getContext().getRendererQuirks().toString());
            System.err.println("GL:" + gl + ", " + gl.getContext().getGLVersion());
        }

        vertShader = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(), "shader", "shader/bin", "landscape", true);
        fragShader = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(), "shader", "shader/bin", "landscape", true);
        vertShader.defaultShaderCustomization(gl, true, true);
        fragShader.defaultShaderCustomization(gl, true, true);
        shaderProg = new ShaderProgram();
        shaderProg.add(gl, vertShader, System.err);
        shaderProg.add(gl, fragShader, System.err); 
        
        shaderState = new ShaderState();
        shaderState.attachShaderProgram(gl, shaderProg, true);
        
        resolution = new float[] { drawable.getWidth(), drawable.getHeight(), 0};
        resolutionUni = new GLUniformData("iResolution", 3, FloatBuffer.wrap(resolution));
        shaderState.ownUniform(resolutionUni);
        shaderState.uniform(gl, resolutionUni);
        
        timeUni = new GLUniformData("iGlobalTime", 0.0f);
        shaderState.ownUniform(timeUni);
            
        vertices = GLArrayDataServer.createGLSL("inVertex", 2, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        vertices.putf(-1.0f); vertices.putf(-1.0f);
        vertices.putf(+1.0f); vertices.putf(-1.0f);
        vertices.putf(-1.0f); vertices.putf(+1.0f);
        vertices.putf(+1.0f); vertices.putf(+1.0f);
        vertices.seal(gl, true);
        shaderState.ownAttribute(vertices, true);
        shaderState.useProgram(gl, false);

        millisOffset = System.currentTimeMillis();
        
        System.err.println(Thread.currentThread()+" LandscapeES2.init FIN");
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        System.err.println(Thread.currentThread()+" LandscapeES2.reshape "+x+"/"+y+" "+width+"x"+height+", swapInterval "+swapInterval+", drawable 0x"+Long.toHexString(drawable.getHandle()));
        
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        if(-1 != swapInterval) {
            gl.setSwapInterval(swapInterval); // in case switching the drawable (impl. may bound attribute there)
        }
        
        shaderState.useProgram(gl, true);
        
        resolution[0] = drawable.getWidth();
        resolution[1] = drawable.getHeight();
        shaderState.uniform(gl, resolutionUni);
                
        shaderState.useProgram(gl, false);
    }

    public void dispose(GLAutoDrawable drawable) {
        System.err.println(Thread.currentThread()+" LandscapeES2.dispose ... ");
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        shaderState.useProgram(gl, false);
        shaderState.destroy(gl);
        shaderState = null;
        
        System.err.println(Thread.currentThread()+" LandscapeES2.dispose FIN");
    }

    public void display(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        // Shader fills complete framebuffer regardless of DEPTH, no Clear required.
        // gl.glClearColor(0.5f, 0.1f, 0.1f, 1);
        // gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        
        shaderState.useProgram(gl, true);    
        
        timeUni.setData((System.currentTimeMillis() - millisOffset) / 1000.0f);
        shaderState.uniform(gl, timeUni);
        vertices.enableBuffer(gl, true);
        gl.glDrawArrays(GL2ES2.GL_TRIANGLE_STRIP, 0, 4);
        vertices.enableBuffer(gl, false);
        
        shaderState.useProgram(gl, false);
        
        // Compute current framerate and printout.
        frameCount++;      
        fcount += 1;
        int m = (int) (System.currentTimeMillis() - millisOffset);
        if (m - lastm > 1000 * fint) {
          frameRate = (float)(fcount) / fint;
          fcount = 0;
          lastm = m;
        }         
        if (frameCount % TARGET_FPS == 0) {
          System.out.println("FrameCount: " + frameCount + " - " + "FrameRate: " + frameRate);
        }    
    }
    
    boolean confinedFixedCenter = false;
    
    public void setConfinedFixedCenter(boolean v) {
        confinedFixedCenter = v;
    }    
}
