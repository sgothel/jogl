/**
 * Copyright 2025 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.demos.gl4es31;

import com.jogamp.common.util.VersionUtil;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL3ES3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.caps.NonFSAAGLCapsChooser;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.glsl.ShaderUtil;

/**
 * JOGL Compute ShaderCode demo using OpenGL 4.3 or ES 3.1 core profile features.
 *
 * The compute shader fills tuples of vertices + color attributes in a VBO buffer,
 * passed directly to the vertex-shader w/o leaving the GPU.
 */
public class ComputeShader01GL4ES31 implements GLEventListener  {
    private ShaderState stComp, stGfx;
    private PMVMatrix pmvMatrix;
    private GLUniformData pmvMatrixUniform, uRadius;
    private boolean usesPMVMatrix;
    private int iVBO;
    private int iLocVertex = -1, iLocColor = -1;

    private static final int FloatByteSize = GLBuffers.sizeOfGLType(GL.GL_FLOAT);
    private static final int CompPerElem = 3;
    private static final int BytesPerElem = CompPerElem * FloatByteSize;
    private static final int ByteStride = 2 * CompPerElem * FloatByteSize;
    private static final String shaderBasename = "compute01_xxx";

    public ComputeShader01GL4ES31() {
    }

    private static final int GROUP_SIZE_HEIGHT = 8, GROUP_SIZE_WIDTH = 8;
    private static final int NUM_VERTS_H = 16, NUM_VERTS_V = 16;
    // private static final int LOCAL_SIZE_X = 8, LOCAL_SIZE_y = 8; // in compute shader

    @Override
    public void init(final GLAutoDrawable drawable) {
        {
            final GL gl = drawable.getGL();
            System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
            System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
            System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
            System.err.println("GL GLSL: "+gl.hasGLSL()+", has-compiler-func: "+gl.isFunctionAvailable("glCompileShader")+", version "+(gl.hasGLSL() ? gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION) : "none"));
            System.err.println("GL Profile: "+gl.getGLProfile());
            System.err.println("GL Renderer Quirks:" + gl.getContext().getRendererQuirks().toString());
            System.err.println("GL:" + gl + ", " + gl.getContext().getGLVersion());
            if( !ShaderUtil.isComputeShaderSupported(gl) ) {
                throw new RuntimeException("GL object not >= 4.3 or ES >= 3.1, i.e. no compute shader support.: "+gl);
            }
        }
        final GL3 gl = drawable.getGL().getGL3();

        {
            final int[] tmp = new int[1];
            gl.glGenBuffers(1, tmp, 0);
            iVBO = tmp[0];
            if(0 == iVBO) {
                throw new GLException("Couldn't create VBO");
            }
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, iVBO);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, NUM_VERTS_H * NUM_VERTS_V * ByteStride, null, GL.GL_STATIC_DRAW);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            System.err.println("iVBO: "+iVBO+": "+NUM_VERTS_H+"x"+NUM_VERTS_V);
        }
        {
            final ShaderCode cs = ShaderCode.create(gl, GL3ES3.GL_COMPUTE_SHADER, this.getClass(),
                                                    "shader", "shader/bin", shaderBasename, true);
            cs.defaultShaderCustomization(gl, true, true);

            final ShaderProgram sp = new ShaderProgram();
            sp.add(gl, cs, System.err);
            if(!sp.link(gl, System.err)) {
                throw new GLException("Couldn't link program: "+sp);
            }
            stComp=new ShaderState();
            stComp.attachShaderProgram(gl, sp, true);
            uRadius = new GLUniformData("radius", 1.0f);
            stComp.ownUniform(uRadius);
            if(!stComp.uniform(gl, uRadius)) {
                throw new GLException("Error setting radius in shader: "+uRadius);
            }
            System.err.println("uRadius: "+uRadius);
            stComp.useProgram(gl, false);
        }
        {
            usesPMVMatrix = true;
            final ShaderCode vs, fs;
            vs = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(),
                                   "shader", "shader/bin", shaderBasename, true);
            fs = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(),
                                   "shader", "shader/bin", shaderBasename, true);
            vs.defaultShaderCustomization(gl, true, true);
            fs.defaultShaderCustomization(gl, true, true);

            final ShaderProgram sp = new ShaderProgram();
            sp.add(gl, vs, System.err);
            sp.add(gl, fs, System.err);
            if(!sp.link(gl, System.err)) {
                throw new GLException("Couldn't link program: "+sp);
            }
            stGfx=new ShaderState();
            stGfx.attachShaderProgram(gl, sp, true);

            // setup mgl_PMVMatrix
            pmvMatrix = new PMVMatrix();
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmvMatrix.glLoadIdentity();
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmvMatrix.glLoadIdentity();
            if( usesPMVMatrix ) {
                pmvMatrixUniform = new GLUniformData("gcu_PMVMatrix", 4, 4, pmvMatrix.getSyncPMv()); // P, Mv
                stGfx.ownUniform(pmvMatrixUniform);
                if(!stGfx.uniform(gl, pmvMatrixUniform)) {
                    throw new GLException("Error setting PMVMatrix in shader: "+stGfx);
                }
            }
            iLocVertex = gl.glGetAttribLocation(sp.program(), "gca_Vertex");
            if( iLocVertex < 0 ) {
                throw new GLException("Couldn't find gca_Vertex: "+sp);
            }
            iLocColor = gl.glGetAttribLocation(sp.program(), "gca_Color");
            if( iLocColor < 0 ) {
                throw new GLException("Couldn't find gca_Color: "+sp);
            }
            System.err.println("iLocVertex: "+iLocVertex);
            System.err.println("iLocColor: "+iLocColor);

            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, iVBO);
            gl.glEnableVertexAttribArray(iLocVertex);
            gl.glEnableVertexAttribArray(iLocColor);
            gl.glVertexAttribPointer(iLocVertex, CompPerElem, GL.GL_FLOAT, false, ByteStride, 0);
            gl.glVertexAttribPointer(iLocColor,  CompPerElem, GL.GL_FLOAT, false, ByteStride, BytesPerElem);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

            stGfx.useProgram(gl, false);
        }
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();
        if(null != stComp) {
            stComp.destroy(gl);
            stComp=null;
        }
        if(null != stGfx) {
            pmvMatrixUniform = null;
            pmvMatrix=null;
            stGfx.destroy(gl);
            stGfx=null;
        }
        if(iVBO !=0 ) {
            final int[] tmp = new int[] { iVBO } ;
            gl.glDeleteBuffers(1, tmp, 0);
            iVBO = 0;
        }
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL3 gl = drawable.getGL().getGL3();

        gl.setSwapInterval(1);

        if(null != stGfx) {
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmvMatrix.glLoadIdentity();
            pmvMatrix.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 10.0f);

            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmvMatrix.glLoadIdentity();

            stGfx.useProgram(gl, true);
            if( usesPMVMatrix ) {
                stGfx.uniform(gl, pmvMatrixUniform);
            }
            stGfx.useProgram(gl, false);
        }
    }

    float radius_dir = -1.0f;

    @Override
    public void display(final GLAutoDrawable drawable)  {
        final GL3 gl = drawable.getGL().getGL3();

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

        if(null != stComp) {
            stComp.useProgram(gl, true);

            // Update the radius uniform
            float r = uRadius.floatValue();
            if( r > 1.0f) {
                radius_dir = -1.0f;
            } else if( r < 0.01f ){
                radius_dir = 1.0f;
            }
            r += radius_dir * 0.004f;
            uRadius.setData(r);
            stComp.uniform(gl, uRadius);

            // Bind the VBO onto SSBO, which is going to filled in witin the compute shader.
            // gIndexBufferBinding is equal to 0 (same as the compute shader binding)
            final int gIndexBufferBinding = 0;
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, gIndexBufferBinding, iVBO);

            // Submit job for the compute shader execution.
            // As the result the function is called with the following parameters:
            // glDispatchCompute(2, 2, 1)
            // 4 x [8 x 8] which results with the number of 256 threads.
            // 8 x 8 is hardcoded in the compute shader
            gl.glDispatchCompute((NUM_VERTS_H % GROUP_SIZE_WIDTH + NUM_VERTS_H) / GROUP_SIZE_WIDTH,
                                 (NUM_VERTS_V % GROUP_SIZE_HEIGHT + NUM_VERTS_V) / GROUP_SIZE_HEIGHT,
                                 1);

            // Unbind the SSBO buffer.
            // gIndexBufferBinding is equal to 0 (same as the compute shader binding)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, gIndexBufferBinding, 0);
            stComp.useProgram(gl, false);
        }
        if(null != stGfx) {
            stGfx.useProgram(gl, true);

            // Call this function before we submit a draw call, which uses dependency
            // buffer, to the GPU
            gl.glMemoryBarrier(GL2ES3.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);

            // Bind VBO
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, iVBO);

            gl.glEnableVertexAttribArray(iLocVertex);
            gl.glEnableVertexAttribArray(iLocColor);

            // Draw points from VBO
            gl.glDrawArrays(GL.GL_LINE_STRIP, 0, NUM_VERTS_H*NUM_VERTS_V);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

            stGfx.useProgram(gl, false);
        }
    }//end display()

    public static void main(final String[] args) {
        final CommandlineOptions options = new CommandlineOptions(1280, 720, 0);

        System.err.println(options);
        System.err.println(VersionUtil.getPlatformInfo());

        final GLCapabilities reqCaps = options.getGLCaps();
        System.out.println("Requested: " + reqCaps);

        final GLWindow window = GLWindow.create(reqCaps);
        if( 0 == options.sceneMSAASamples ) {
            window.setCapabilitiesChooser(new NonFSAAGLCapsChooser(false));
        }
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(ComputeShader01GL4ES31.class.getSimpleName());

        window.addGLEventListener(new ComputeShader01GL4ES31());

        final Animator animator = new Animator(0 /* w/o AWT */);
        animator.setUpdateFPSFrames(5*60, System.err);
        animator.add(window);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(final WindowEvent e) {
                animator.stop();
            }
        });

        window.setVisible(true);
        animator.start();
    }
}
