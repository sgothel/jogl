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
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.math.Recti;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.caps.NonFSAAGLCapsChooser;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * FBO demo using a texture attachment for color- and combined depth + stencil-buffer.
 * - The color-buffer texture is visualized.
 * - The depth-buffer texture portion is visualized non-linear and linear.
 * - The stencil-buffer texture portion is visualized.
 *
 * The FBO programming is done `manually` w/o FBObject.
 *
 * This demo required GL4 (core).
 *
 * We split viewport in 4 parts
 * - bottom-left: original
 * - bottom-right: depth-buffer
 * - top-right:    stencil-buffer
 */
public class GearsFBO02 implements GLEventListener {
    private final GearsES2 demo;
    private final GLEventListener demo2;
    private final int swapInterval;

    private final ShaderState st;
    private final PMVMatrix pmvMatrix;

    private ShaderProgram sp0;
    private GLUniformData pmvMatrixUniform;
    private GLArrayDataServer interleavedVBO;
    private final GLUniformData texUnit0, texUnit1;
    private GLUniformData texType;

    public GearsFBO02(final int demo2Type, final int swapInterval) {
        this.demo = new GearsES2(0);
        switch(demo2Type) {
            case 1: demo2 = new RedSquareES2(0); break;
            case 2: demo2 = new LandscapeES2(0); break;
            default: demo2 = null; break;
        }
        this.swapInterval = swapInterval;
        texUnit0 = new GLUniformData("mgl_Texture0", 0);
        texUnit1 = new GLUniformData("mgl_Texture1", 1);

        st = new ShaderState();
        // st.setVerbose(true);
        pmvMatrix = new PMVMatrix();
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        System.err.println("Init: Chosen Caps: "+drawable.getChosenGLCapabilities());
        final GL4 gl = drawable.getGL().getGL4();

        demo.init(drawable);
        if (null != demo2) {
            demo2.init(drawable);
            demo.setScale(2.0f);
            demo.setRotX(0.0f);
            demo.setRotY(0.0f);
        }

        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, GearsFBO02.class, "shader",
                "shader/bin", "texture01_xxx", true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, GearsFBO02.class, "shader",
                "shader/bin", "texture01_customtex", true);
        vp0.defaultShaderCustomization(gl, true, true);
        fp0.defaultShaderCustomization(gl, true, true);

        sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);
        st.attachShaderProgram(gl, sp0, true);

        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.getSyncPMv());
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);

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
        st.ownAttribute(interleavedVBO, true);

        texType = new GLUniformData("mgl_TexType", 1);
        st.ownUniform(texType);
        st.uniform(gl, texType);
        st.ownUniform(texUnit0);
        st.uniform(gl, texUnit0);
        st.ownUniform(texUnit1);
        st.uniform(gl, texUnit1);

        st.useProgram(gl, false);

        viewport.set(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        initFBO(gl, viewport.width()/2, viewport.height()/2);

        gl.glEnable(GL.GL_DEPTH_TEST);
    }

    Recti viewport = new Recti();
    private int fboID, fboWidth, fboHeight;
    private int colorTexID, depthStencilTexID, stencilViewID; // via textures
    private int depthStencilRenderID; // via renderbuffer
    private final boolean useDepthStencilTexture = true;
    private final int colorAttachmentPoint = 0;

    private void initTexParam(final GL2ES2 gl) {
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
    }
    private void initFBO(final GL4 gl, final int width, final int height) {
        // Generate a Framebuffer
        final int tmp[] = { 0, 0 };

        colorTexID=0;
        depthStencilTexID=0;
        stencilViewID=0;
        depthStencilRenderID=0;
        fboWidth = 0;
        fboHeight = 0;

        gl.glGenTextures(1, tmp, 0);
        colorTexID = tmp[0];
        if (0 == colorTexID) {
            throw new GLException("XXX");
        }

        if( useDepthStencilTexture ) {
            gl.glGenTextures(2, tmp, 0);
            depthStencilTexID = tmp[0];
            stencilViewID = tmp[1];
            if (0 == depthStencilTexID || 0 == stencilViewID) {
                throw new GLException("XXX");
            }
        } else {
            gl.glGenRenderbuffers(1, tmp, 0);
            depthStencilRenderID = tmp[0];
            if (0 == depthStencilRenderID) {
                throw new GLException("XXX");
            }
        }

        gl.glGenFramebuffers(1, tmp, 0);
        fboID = tmp[0];
        if (0 == fboID) {
            throw new GLException("XXX");
        }
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboID);

        // color tex
        {
            final boolean alpha=true;
            final int iFormat, dataFormat, dataType;
            if(gl.isGLES3()) {
                iFormat = alpha ? GL.GL_RGBA8 : GL.GL_RGB8;
                dataFormat = alpha ? GL.GL_RGBA : GL.GL_RGB;
                dataType = GL.GL_UNSIGNED_BYTE;
            } else if(gl.isGLES()) {
                iFormat = alpha ? GL.GL_RGBA : GL.GL_RGB;
                dataFormat = alpha ? GL.GL_RGBA : GL.GL_RGB;
                dataType = GL.GL_UNSIGNED_BYTE;
            } else {
                iFormat = alpha ? GL.GL_RGBA8 : GL.GL_RGB8;
                // textureInternalFormat = alpha ? GL.GL_RGBA : GL.GL_RGB;
                // textureInternalFormat = alpha ? 4 : 3;
                dataFormat = alpha ? GL.GL_BGRA : GL.GL_RGB;
                dataType = alpha ? GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV : GL.GL_UNSIGNED_BYTE;
            }
            gl.glBindTexture(GL.GL_TEXTURE_2D, colorTexID);
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, iFormat, width, height, 0, dataFormat, dataType, null);
            initTexParam(gl);
            // gl.glFramebufferTexture(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, colorTexID, 0);
            gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0 + colorAttachmentPoint, GL.GL_TEXTURE_2D, colorTexID, 0);
        }

        if( useDepthStencilTexture ) {
            gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit0.intValue());
            gl.glBindTexture(GL.GL_TEXTURE_2D, depthStencilTexID);
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_DEPTH24_STENCIL8, width, height, 0, GL.GL_DEPTH_STENCIL, GL.GL_UNSIGNED_INT_24_8, null);
            gl.glTexStorage2D(GL.GL_TEXTURE_2D, 1, GL.GL_DEPTH24_STENCIL8, width, height);
            // conform to GL_TEXTURE_IMMUTABLE_FORMAT
            gl.glTextureView(stencilViewID, GL.GL_TEXTURE_2D, depthStencilTexID, GL.GL_DEPTH24_STENCIL8, 0, 1, 0, 1);
            // float zBits = texture2D(tex.uv).r; //access the 24 first bit
            initTexParam(gl);
            gl.glTexParameteri (GL.GL_TEXTURE_2D, GL2ES3.GL_DEPTH_STENCIL_TEXTURE_MODE, GL2ES2.GL_DEPTH_COMPONENT);

            gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit1.intValue());
            gl.glBindTexture(GL.GL_TEXTURE_2D, stencilViewID);
            initTexParam(gl);
            gl.glTexParameteri (GL.GL_TEXTURE_2D, GL2ES3.GL_DEPTH_STENCIL_TEXTURE_MODE, GL2ES2.GL_STENCIL_INDEX);

            // gl.glFramebufferTexture(GL.GL_FRAMEBUFFER, GL2ES3.GL_DEPTH_STENCIL_ATTACHMENT, depthStencilTexID, 0);
            gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL2ES3.GL_DEPTH_STENCIL_ATTACHMENT, GL.GL_TEXTURE_2D, depthStencilTexID, 0);
        } else {
            gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, depthStencilRenderID);
            gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL.GL_DEPTH24_STENCIL8, width, height);
            gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL2ES3.GL_DEPTH_STENCIL_ATTACHMENT, GL.GL_RENDERBUFFER, depthStencilRenderID);
        }
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        final int fboStatus = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
        if( GL.GL_FRAMEBUFFER_COMPLETE != fboStatus ) {
            throw new GLException("FBO not complete, but 0x"+Integer.toHexString(fboStatus));
        }
        fboWidth = width;
        fboHeight = height;
    }
    private void disposeFBO(final GL gl) {
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboID);
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0 + colorAttachmentPoint, GL.GL_TEXTURE_2D, 0, 0);
        if( useDepthStencilTexture ) {
            gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT,  GL.GL_TEXTURE_2D, 0, 0);
        } else {
            gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, 0);
        }
        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_STENCIL_ATTACHMENT, GL.GL_RENDERBUFFER, 0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
        {
            final int tmp[] = { colorTexID };
            gl.glDeleteTextures(1, tmp, 0);
        }
        if( useDepthStencilTexture ) {
            final int tmp[] = { depthStencilTexID, stencilViewID };
            gl.glDeleteTextures(2, tmp, 0);
        } else {
            final int tmp[] = { depthStencilRenderID };
            gl.glDeleteRenderbuffers(1, tmp, 0);
        }
        colorTexID=0;
        depthStencilTexID=0;
        stencilViewID=0;
        depthStencilRenderID=0;
        fboWidth = 0;
        fboHeight = 0;

        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        {
            final int tmp[] = { fboID };
            gl.glDeleteFramebuffers(1, tmp, 0);
            fboID = 0;
        }
    }
    private void resetFBOs(final GL4 gl, final int width, final int height) {
        if( fboWidth != width || fboHeight != height ) {
            disposeFBO(gl);
            initFBO(gl, width, height);
        }
    }
    private void resetStencil(final GL2ES2 gl) {
        // default
        gl.glStencilFunc(GL.GL_ALWAYS, 0, 0xFF); // default: ALWAYS, 0, 1s
        gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP); // default: KEEP, KEEP, KEEP
        gl.glStencilMask(0xFF); // default: 1s
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        demo.dispose(drawable);
        disposeFBO(gl);
        st.destroy(gl);

        sp0 = null;
        pmvMatrixUniform = null;
        interleavedVBO = null;
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }

        {
            // demo writes to stencil (gears)
            gl.glViewport(viewport.x(), viewport.y(), fboWidth, fboHeight);
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboID);

            gl.glEnable(GL.GL_STENCIL_TEST);
            gl.glStencilFunc(GL.GL_ALWAYS, 1, 0xFF); // default: ALWAYS, 0, 1s
            gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_REPLACE); // default: KEEP, KEEP, KEEP
            gl.glStencilMask(0xFF); // default: 1s

            demo.setClearBuffers(true);
            demo.setClearStencilBuffer(true);
            demo.display(drawable);
            resetStencil(gl);
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
            gl.glFinish();
        }

        {
            // only draw on non '1' stencil (demo or demo2)
            gl.glViewport(viewport.x(), viewport.y(), fboWidth, fboHeight);
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboID);

            gl.glStencilFunc(GL.GL_NOTEQUAL, 0, 0xFF); // default: ALWAYS, 0, 1s
            gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_REPLACE); // default: KEEP, KEEP, KEEP
            gl.glStencilMask(0x00); // no stencil write
            if( null != demo2 ) {
                demo2.display(drawable);
            } else {
                demo.setClearBuffers(false);
                demo.setClearStencilBuffer(false);
                demo.setGearsColor(-1, 1.0f, 0.8431f, 0.0f, 0.7f);
                demo.setScale(0.5f);
                final float panX = -1.4f, panY = -1.0f, panZ = 2.0f;
                demo.addPanning(panX, panY, panZ);
                demo.display(drawable);
                demo.setScale(1.0f);
                demo.addPanning(-panX, -panY, -panZ);
                demo.resetGearsColor();
            }
            resetStencil(gl);
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
            gl.glFinish();
        }
        st.useProgram(gl, true);
        {
            // FBO color-buffer
            gl.glViewport(viewport.x(), viewport.y(), fboWidth, fboHeight);
            texType.setData(0);
            st.uniform(gl, texType);

            gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit0.intValue());
            {
                gl.glBindTexture(GL.GL_TEXTURE_2D, colorTexID);
                if( !gl.isGLcore() ) {
                    gl.glEnable(GL.GL_TEXTURE_2D);
                }
            }
            interleavedVBO.enableBuffer(gl, true);

            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

            interleavedVBO.enableBuffer(gl, false);

            gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

            gl.glViewport(viewport.x(), viewport.y(), viewport.width(), viewport.height());
        }
        {
            // FBO depth-buffer
            gl.glViewport(viewport.x()+fboWidth, viewport.y(), fboWidth, fboHeight);

            texType.setData(1);
            st.uniform(gl, texType);

            gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit0.intValue());
            {
                gl.glBindTexture(GL.GL_TEXTURE_2D, depthStencilTexID);
                if( !gl.isGLcore() ) {
                    gl.glEnable(GL.GL_TEXTURE_2D);
                }
            }
            interleavedVBO.enableBuffer(gl, true);

            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

            interleavedVBO.enableBuffer(gl, false);

            gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

            gl.glViewport(viewport.x(), viewport.y(), viewport.width(), viewport.height());
        }
        {
            // FBO stencil-buffer
            gl.glViewport(viewport.x()+fboWidth, viewport.y()+fboHeight, fboWidth, fboHeight);

            texType.setData(2);
            st.uniform(gl, texType);

            gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit1.intValue());
            {
                gl.glBindTexture(GL.GL_TEXTURE_2D, stencilViewID);
                if( !gl.isGLcore() ) {
                    gl.glEnable(GL.GL_TEXTURE_2D);
                }
            }
            interleavedVBO.enableBuffer(gl, true);

            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

            interleavedVBO.enableBuffer(gl, false);

            gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

            gl.glViewport(viewport.x(), viewport.y(), viewport.width(), viewport.height());
        }
        st.useProgram(gl, false);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL4 gl = drawable.getGL().getGL4();

        viewport.set(x, y, width, height);

        System.err.println("**** Reshape.Reset: "+width+"x"+height);
        resetFBOs(gl, width/2, height/2);

        gl.glViewport(viewport.x(), viewport.y(), fboWidth, fboHeight);
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboID);
        demo.reshape(drawable, x, y, fboWidth, fboHeight);
        if (null != demo2) {
            demo2.reshape(drawable, x, y, fboWidth, fboHeight);
        }
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        gl.glViewport(viewport.x(), viewport.y(), viewport.width(), viewport.height());

        gl.setSwapInterval(swapInterval);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 10.0f);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();

        st.useProgram(gl, true);
        st.uniform(gl, pmvMatrixUniform);
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
        options.setGLProfile(GLProfile.GL4);
        int demo2Type = 2;

        for (int i = 0; i < args.length; ++i) {
            if(args[i].equals("-demo2") && i+1 < args.length) {
                ++i;
                demo2Type = MiscUtils.atoi(args[i], demo2Type);
            }
        }

        System.err.println(VersionUtil.getPlatformInfo());
        // System.err.println(JoglVersion.getAllAvailableCapabilitiesInfo(dpy.getGraphicsDevice(), null).toString());
        System.err.println(options);

        final GLCapabilities reqCaps = options.getGLCaps();
        System.out.println("Requested: " + reqCaps);
        System.out.println("Demo2 Type: " + demo2Type);

        final GLWindow window = GLWindow.create(reqCaps);
        if( 0 == options.sceneMSAASamples ) {
            window.setCapabilitiesChooser(new NonFSAAGLCapsChooser(false));
        }
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(GearsFBO02.class.getSimpleName());

        window.addGLEventListener(new GearsFBO02(demo2Type, 1));
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
