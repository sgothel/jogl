/**
 * Copyright 2011-2025 JogAmp Community. All rights reserved.
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

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.caps.NonFSAAGLCapsChooser;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * FBO demo using a texture attachment for color-buffer using FBObject
 * - The color-buffer texture is visualized.
 */
public class GearsFBO00 implements GLEventListener {
    private final GLEventListener demo;
    private final int swapInterval;
    private int numSamples;

    private final ShaderState st;
    private final PMVMatrix pmvMatrix;

    private final FBObject fbo0;

    private TextureAttachment fbo0Tex;

    private ShaderProgram sp0;
    private GLUniformData pmvMatrixUniform;
    private GLArrayDataServer interleavedVBO;
    private final GLUniformData texUnit0;

    public GearsFBO00(final int swapInterval) {
        this.demo = new GearsES2(0);
        this.swapInterval = swapInterval;

        st = new ShaderState();
        // st.setVerbose(true);
        pmvMatrix = new PMVMatrix();
        texUnit0 = new GLUniformData("mgl_Texture0", 0);

        fbo0 = new FBObject();

        numSamples = 0;
    }

    public void setMSAA(final int numSamples) {
        this.numSamples=numSamples;
    }
    public int getMSAA() { return numSamples; }

    @Override
    public void init(final GLAutoDrawable drawable) {
        System.err.println("Init: Chosen Caps: "+drawable.getChosenGLCapabilities());
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        demo.init(drawable);

        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, GearsFBO00.class, "shader",
                "shader/bin", "texture01_xxx", true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, GearsFBO00.class, "shader",
                "shader/bin", "texture01_xxx", true);
        vp0.defaultShaderCustomization(gl, true, true);
        fp0.defaultShaderCustomization(gl, true, true);

        sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);
        st.attachShaderProgram(gl, sp0, true);

        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.getSyncPMv());
        st.manage(pmvMatrixUniform, true);
        st.send(gl, pmvMatrixUniform);

        interleavedVBO = GLArrayDataServer.createGLSLInterleaved(3+4+2, GL.GL_FLOAT, false, 3*4, GL.GL_STATIC_DRAW);
        {
            interleavedVBO.addGLSLSubArray("mgl_Vertex",        3, GL.GL_ARRAY_BUFFER);
            interleavedVBO.addGLSLSubArray("mgl_Color",         4, GL.GL_ARRAY_BUFFER);
            //interleavedVBO.addGLSLSubArray("mgl_Normal",        3, GL.GL_ARRAY_BUFFER);
            interleavedVBO.addGLSLSubArray("mgl_MultiTexCoord", 2, GL.GL_ARRAY_BUFFER);

            final FloatBuffer ib = (FloatBuffer)interleavedVBO.getBuffer();

            for(int i=0; i<4; i++) {
                ib.put(s_quadVertices,  i*3, 3);
                ib.put(s_quadColors,    i*4, 4);
                //ib.put(s_cubeNormals,   i*3, 3);
                ib.put(s_quadTexCoords, i*2, 2);
            }
        }
        interleavedVBO.seal(gl, true);
        interleavedVBO.enableBuffer(gl, false);
        st.manage(interleavedVBO, true);

        st.manage(texUnit0, true);
        st.send(gl, texUnit0);

        st.useProgram(gl, false);

        initFBOs(gl, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

        gl.glEnable(GL.GL_DEPTH_TEST);
    }

    private void initFBOs(final GL gl, final int width, final int height) {
        fbo0.init(gl, width, height, numSamples);
        numSamples = fbo0.getNumSamples();

        if(numSamples>0) {
            fbo0.attachColorbuffer(gl, 0, true);
            fbo0.resetSamplingSink(gl);
            fbo0Tex = fbo0.getSamplingSink().getTextureAttachment();
        } else {
            fbo0Tex = fbo0.attachTexture2D(gl, 0, true);
        }
        numSamples=fbo0.getNumSamples();
        fbo0.attachRenderbuffer(gl, Type.DEPTH, FBObject.CHOSEN_BITS);
        fbo0.unbind(gl);
    }

    private void resetFBOs(final GL gl, final int width, final int height) {
        fbo0.reset(gl, width, height, numSamples);
        numSamples = fbo0.getNumSamples();
        if(numSamples>0) {
            fbo0Tex = fbo0.getSamplingSink().getTextureAttachment();
        } else {
            fbo0Tex = fbo0.getColorbuffer(0).getTextureAttachment();
        }
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        demo.dispose(drawable);
        fbo0.destroy(gl);
        st.destroy(gl);

        fbo0Tex = null;
        sp0 = null;
        pmvMatrixUniform = null;
        interleavedVBO = null;
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        if( fbo0.getNumSamples() != numSamples ) {
            System.err.println("**** NumSamples: "+fbo0.getNumSamples()+" -> "+numSamples);
            resetFBOs(gl, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        }

        if(0 < numSamples) {
            gl.glEnable(GL.GL_MULTISAMPLE);
        }

        fbo0.bind(gl);
        demo.display(drawable);
        fbo0.unbind(gl);

        st.useProgram(gl, true);
        {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }

        {
            // FBO color-buffer
            gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit0.intValue());
            {
                fbo0.use(gl, fbo0Tex);
                if( !gl.isGLcore() ) {
                    gl.glEnable(GL.GL_TEXTURE_2D);
                }
            }
            interleavedVBO.enableBuffer(gl, true);

            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

            interleavedVBO.enableBuffer(gl, false);

            fbo0.unuse(gl);
        }

        st.useProgram(gl, false);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        System.err.println("**** Reshape.Reset: "+width+"x"+height);
        resetFBOs(gl, width, height);

        fbo0.bind(gl);
        demo.reshape(drawable, x, y, width, height);
        fbo0.unbind(gl);

        gl.setSwapInterval(swapInterval);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 10.0f);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();

        st.useProgram(gl, true);
        st.send(gl, pmvMatrixUniform);
        st.useProgram(gl, false);

    }

    private static final float[] s_quadVertices = {
      -1f, -1f, 0f, // LB
       1f, -1f, 0f, // RB
      -1f,  1f, 0f, // LT
       1f,  1f, 0f  // RT
    };
    private static final float[] s_quadColors = {
      1f, 1f, 1f, 1f,
      1f, 1f, 1f, 1f,
      1f, 1f, 1f, 1f,
      1f, 1f, 1f, 1f };
    private static final float[] s_quadTexCoords = {
            0f, 0f, // LB
            1f, 0f, // RB
            0f, 1f, // LT
            1f, 1f  // RT
    };

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
        window.setTitle(GearsFBO00.class.getSimpleName());

        window.addGLEventListener(new GearsFBO00(1));
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
