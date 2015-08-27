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
package com.jogamp.opengl.test.junit.jogl.demos.es2;

import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLUniformData;

/**
 * LandscapeES2
 */
public class LandscapeES2 implements GLEventListener {
    private int swapInterval = 0;
    private boolean verbose = true;

    static public final int TARGET_FPS    = 120;
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
    private final int fint = 1;

    public LandscapeES2(final int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public LandscapeES2() {
        this.swapInterval = 1;
    }

    public void setVerbose(final boolean v) { verbose = v; }

    public void init(final GLAutoDrawable drawable) {
        System.err.println(Thread.currentThread()+" LandscapeES2.init ...");
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

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

        resolution = new float[] { drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), 0};
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

    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        System.err.println(Thread.currentThread()+" LandscapeES2.reshape "+x+"/"+y+" "+width+"x"+height+", swapInterval "+swapInterval+", drawable 0x"+Long.toHexString(drawable.getHandle()));

        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.setSwapInterval(swapInterval); // in case switching the drawable (impl. may bound attribute there)

        shaderState.useProgram(gl, true);

        resolution[0] = drawable.getSurfaceWidth();
        resolution[1] = drawable.getSurfaceHeight();
        shaderState.uniform(gl, resolutionUni);

        shaderState.useProgram(gl, false);
    }

    public void dispose(final GLAutoDrawable drawable) {
        System.err.println(Thread.currentThread()+" LandscapeES2.dispose ... ");
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        shaderState.useProgram(gl, false);
        shaderState.destroy(gl);
        shaderState = null;

        System.err.println(Thread.currentThread()+" LandscapeES2.dispose FIN");
    }

    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        // Shader fills complete framebuffer regardless of DEPTH, no Clear required.
        // gl.glClearColor(0.5f, 0.1f, 0.1f, 1);
        // gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        shaderState.useProgram(gl, true);

        timeUni.setData((System.currentTimeMillis() - millisOffset) / 1000.0f);
        shaderState.uniform(gl, timeUni);
        vertices.enableBuffer(gl, true);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        vertices.enableBuffer(gl, false);

        shaderState.useProgram(gl, false);

        // Compute current framerate and printout.
        frameCount++;
        fcount += 1;
        final int m = (int) (System.currentTimeMillis() - millisOffset);
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

    public void setConfinedFixedCenter(final boolean v) {
        confinedFixedCenter = v;
    }
}
