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
package com.jogamp.opengl.demos.es2;

import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.caps.NonFSAAGLCapsChooser;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

public class RedSquareES2 implements GLEventListener {
    private ShaderState st;
    private PMVMatrix pmvMatrix;
    private GLUniformData pmvMatrixUniform;
    private GLArrayDataServer vertices ;
    private GLArrayDataServer colors ;
    private long t0;
    private int swapInterval = 0;
    private float aspect = 1.0f;
    private boolean doRotate = true;
    private boolean verbose = true;
    private boolean clearBuffers = true;

    public RedSquareES2(final int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public RedSquareES2() {
        this.swapInterval = 1;
    }

    public void setAspect(final float aspect) { this.aspect = aspect; }
    public void setDoRotation(final boolean rotate) { this.doRotate = rotate; }
    public void setVerbose(final boolean v) { verbose = v; }
    public void setClearBuffers(final boolean v) { clearBuffers = v; }

    @Override
    public void init(final GLAutoDrawable glad) {
        if(verbose) {
            System.err.println(Thread.currentThread()+" RedSquareES2.init");
        }
        final GL2ES2 gl = glad.getGL().getGL2ES2();

        if(verbose) {
            System.err.println("RedSquareES2 init on "+Thread.currentThread());
            System.err.println("Chosen GLCapabilities: " + glad.getChosenGLCapabilities());
            System.err.println("INIT GL IS: " + gl.getClass().getName());
            System.err.println(JoglVersion.getGLStrings(gl, null, false).toString());
        }
        if( !gl.hasGLSL() ) {
            System.err.println("No GLSL available, no rendering.");
            return;
        }
        st = new ShaderState();
        st.setVerbose(true);
        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(), "shader",
                "shader/bin", "RedSquareShader", true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(), "shader",
                "shader/bin", "RedSquareShader", true);
        vp0.defaultShaderCustomization(gl, true, true);
        fp0.defaultShaderCustomization(gl, true, true);
        final ShaderProgram sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);
        st.attachShaderProgram(gl, sp0, true);

        // setup mgl_PMVMatrix
        pmvMatrix = new PMVMatrix();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.getSyncPMv()); // P, Mv
        st.manage(pmvMatrixUniform, true);
        st.send(gl, pmvMatrixUniform);

        // Allocate Vertex Array
        vertices = GLArrayDataServer.createGLSL("mgl_Vertex", 3, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        vertices.putf(-2); vertices.putf( 2); vertices.putf( 0);
        vertices.putf( 2); vertices.putf( 2); vertices.putf( 0);
        vertices.putf(-2); vertices.putf(-2); vertices.putf( 0);
        vertices.putf( 2); vertices.putf(-2); vertices.putf( 0);
        vertices.seal(gl, true);
        st.manage(vertices, true);
        vertices.enableBuffer(gl, false);

        // Allocate Color Array
        colors= GLArrayDataServer.createGLSL("mgl_Color", 4, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        colors.putf(1); colors.putf(0); colors.putf(0); colors.putf(1);
        colors.putf(0); colors.putf(0); colors.putf(1); colors.putf(1);
        colors.putf(1); colors.putf(0); colors.putf(0); colors.putf(1);
        colors.putf(1); colors.putf(0); colors.putf(0); colors.putf(1);
        colors.seal(gl, true);
        st.manage(colors, true);
        colors.enableBuffer(gl, false);

        // OpenGL Render Settings
        gl.glEnable(GL.GL_DEPTH_TEST);
        st.useProgram(gl, false);

        t0 = System.currentTimeMillis();
        if(verbose) {
            System.err.println(Thread.currentThread()+" RedSquareES2.init FIN");
        }
    }

    @Override
    public void display(final GLAutoDrawable glad) {
        final long t1 = System.currentTimeMillis();

        final GL2ES2 gl = glad.getGL().getGL2ES2();
        if( clearBuffers ) {
            gl.glClearColor(0, 0, 0, 0);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }
        if( !gl.hasGLSL() ) {
            return;
        }
        st.useProgram(gl, true);
        // One rotation every four seconds
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        if(doRotate) {
            final float ang = ((t1 - t0) * 360.0F) / 4000.0F;
            pmvMatrix.glRotatef(ang, 0, 0, 1);
            pmvMatrix.glRotatef(ang, 0, 1, 0);
        }
        st.send(gl, pmvMatrixUniform);

        // Draw a square
        vertices.enableBuffer(gl, true);
        colors.enableBuffer(gl, true);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        vertices.enableBuffer(gl, false);
        colors.enableBuffer(gl, false);
        st.useProgram(gl, false);
    }

    @Override
    public void reshape(final GLAutoDrawable glad, final int x, final int y, final int width, final int height) {
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        if(verbose) {
            System.err.println(Thread.currentThread()+" RedSquareES2.reshape "+x+"/"+y+" "+width+"x"+height+", swapInterval "+swapInterval+
                               ", drawable 0x"+Long.toHexString(gl.getContext().getGLDrawable().getHandle()));
        }
        gl.setSwapInterval(swapInterval);
        if( !gl.hasGLSL() ) {
            return;
        }

        st.useProgram(gl, true);
        // Set location in front of camera
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();

        // compute projection parameters 'normal' perspective
        final float fovy=45f;
        final float aspect2 = ( (float) width / (float) height ) / aspect;
        final float zNear=1f;
        final float zFar=100f;

        // compute projection parameters frustum
        final float t=(float)Math.tan(fovy*((float)Math.PI)/360.0f)*zNear;
        final float b=-1.0f*t;
        final float l=aspect2*b;
        final float r=aspect2*t;

        pmvMatrix.glFrustumf(l, r, b, t, zNear, zFar);
        //pmvMatrix.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);
        st.send(gl, pmvMatrixUniform);
        st.useProgram(gl, false);

        System.err.println(Thread.currentThread()+" RedSquareES2.reshape FIN");
    }

    @Override
    public void dispose(final GLAutoDrawable glad) {
        if(verbose) {
            System.err.println(Thread.currentThread()+" RedSquareES2.dispose");
        }
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        if( !gl.hasGLSL() ) {
            return;
        }
        st.destroy(gl);
        st = null;
        pmvMatrix = null;
        if(verbose) {
            System.err.println(Thread.currentThread()+" RedSquareES2.dispose FIN");
        }
    }

    public static void main(final String[] args) {
        final CommandlineOptions options = new CommandlineOptions(1280, 720, 0);

        System.err.println(options);
        System.err.println(VersionUtil.getPlatformInfo());
        // System.err.println(JoglVersion.getAllAvailableCapabilitiesInfo(dpy.getGraphicsDevice(), null).toString());

        final GLCapabilities reqCaps = options.getGLCaps();
        System.out.println("Requested: " + reqCaps);

        final GLWindow window = GLWindow.create(reqCaps);
        if( 0 == options.sceneMSAASamples ) {
            window.setCapabilitiesChooser(new NonFSAAGLCapsChooser(false));
        }
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(RedSquareES2.class.getSimpleName());

        window.addGLEventListener(new RedSquareES2(1));

        final Animator animator = new Animator(0 /* w/o AWT */);
        animator.setUpdateFPSFrames(5*60, null);
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
