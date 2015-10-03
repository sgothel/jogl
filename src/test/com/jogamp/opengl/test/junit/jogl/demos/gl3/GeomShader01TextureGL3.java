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
package com.jogamp.opengl.test.junit.jogl.demos.gl3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.glsl.ShaderUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * JOGL Geometry ShaderCode test case using OpenGL 3.2 core profile features only.
 * <p>
 * Demonstrates <code>pass through</code> and <code>XYZ flipping</code>
 * geometry shader.
 * </p>
 * <p>
 * If the <code>XYZ flipping</code> geometry shader functions properly,
 * the texture will be flipped horizontally and vertically.
 * </p>
 *
 * @author Chuck Ritola December 2012
 * @author Sven Gothel (GL3 core, pass-though, core geometry shader)
 */
public class GeomShader01TextureGL3 implements GLEventListener  {
    private final int geomShader;
    private Texture texture;
    private ShaderState st;
    private PMVMatrix pmvMatrix;
    private GLUniformData pmvMatrixUniform;
    private GLArrayDataServer interleavedVBO;

    static final String shaderBasename = "texture01_xxx";
    static final String[] geomShaderBaseNames = new String[] { "passthrough01_xxx", "flipXYZ01_xxx" };

    public GeomShader01TextureGL3(final int geomShader) {
        this.geomShader = geomShader;
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        {
            final GL gl = drawable.getGL();
            System.err.println("Init - START - useGeomShader "+geomShader+" -> "+geomShaderBaseNames[geomShader]);
            System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
            System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
            System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
            System.err.println("GL GLSL: "+gl.hasGLSL()+", has-compiler-func: "+gl.isFunctionAvailable("glCompileShader")+", version "+(gl.hasGLSL() ? gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION) : "none"));
            System.err.println("GL Profile: "+gl.getGLProfile());
            System.err.println("GL Renderer Quirks:" + gl.getContext().getRendererQuirks().toString());
            System.err.println("GL:" + gl + ", " + gl.getContext().getGLVersion());
            if( !gl.isGL3() ) {
                throw new RuntimeException("GL object not a GL3 core compatible profile: "+gl);
            }
            if( !ShaderUtil.isGeometryShaderSupported(gl) ) {
                throw new RuntimeException("GL object not >= 3.2, i.e. no geometry shader support.: "+gl);
            }
        }
        final GL3 gl = drawable.getGL().getGL3();

        final ShaderProgram sp;
        {
            final ShaderCode vs, gs, fs;
            vs = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(),
                                   "shader", "shader/bin", shaderBasename, true);
            gs = ShaderCode.create(gl, GL3.GL_GEOMETRY_SHADER, this.getClass(),
                                   "shader", "shader/bin", geomShaderBaseNames[geomShader], true);
            fs = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(),
                                   "shader", "shader/bin", shaderBasename, true);
            vs.defaultShaderCustomization(gl, true, true);
            gs.defaultShaderCustomization(gl, true, true);
            fs.defaultShaderCustomization(gl, true, true);

            sp = new ShaderProgram();
            sp.add(gl, vs, System.err);
            sp.add(gl, gs, System.err);
            sp.add(gl, fs, System.err);
            if(!sp.link(gl, System.err)) {
                throw new GLException("Couldn't link program: "+sp);
            }
        }

        st=new ShaderState();
        st.attachShaderProgram(gl, sp, true);

        // setup mgl_PMVMatrix
        pmvMatrix = new PMVMatrix();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf()); // P, Mv
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);

        st.ownUniform(pmvMatrixUniform);
        if(!st.uniform(gl, pmvMatrixUniform)) {
            throw new GLException("Error setting PMVMatrix in shader: "+st);
        }
        if(!st.uniform(gl, new GLUniformData("mgl_ActiveTexture", 0))) {
            throw new GLException("Error setting mgl_ActiveTexture in shader: "+st);
        }

        try {
            texture = createTestTexture(gl);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        if(null == texture) {
            throw new RuntimeException("Could not load test texture");
        }

        // Tri order:
        //   TL, BL, BR
        //   TL, TR, BR
        {
            int i=0;
            final TextureCoords tc = texture.getImageTexCoords();
            s_triTexCoords[i++] = tc.left();  s_triTexCoords[i++] = tc.top();
            s_triTexCoords[i++] = tc.left();  s_triTexCoords[i++] = tc.bottom();
            s_triTexCoords[i++] = tc.right(); s_triTexCoords[i++] = tc.bottom();
            s_triTexCoords[i++] = tc.left();  s_triTexCoords[i++] = tc.top();
            s_triTexCoords[i++] = tc.right(); s_triTexCoords[i++] = tc.top();
            s_triTexCoords[i++] = tc.right(); s_triTexCoords[i++] = tc.bottom();
        }

        interleavedVBO = GLArrayDataServer.createGLSLInterleaved(2+4+2, GL.GL_FLOAT, false, 3*6, GL.GL_STATIC_DRAW);
        {
            interleavedVBO.addGLSLSubArray("mgl_Vertex",        2, GL.GL_ARRAY_BUFFER);
            interleavedVBO.addGLSLSubArray("mgl_Color",         4, GL.GL_ARRAY_BUFFER);
            interleavedVBO.addGLSLSubArray("mgl_MultiTexCoord", 2, GL.GL_ARRAY_BUFFER);

            final FloatBuffer ib = (FloatBuffer)interleavedVBO.getBuffer();

            for(int i=0; i<6; i++) {
                ib.put(s_triVertices,  i*2, 2);
                ib.put(s_triColors,    i*4, 4);
                ib.put(s_triTexCoords, i*2, 2);
            }
        }
        interleavedVBO.seal(gl, true);
        interleavedVBO.enableBuffer(gl, false);
        st.ownAttribute(interleavedVBO, true);

        gl.glClearColor(0f, 0f, 0f, 0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        st.useProgram(gl, false);
    }

    private Texture createTestTexture(final GL3 gl) throws IOException  {
        final URLConnection urlConn = IOUtil.getResource("../../util/texture/test-ntscN_3-01-160x90.png", this.getClass().getClassLoader(), this.getClass());
        if(null == urlConn) { return null; }
        final InputStream istream = urlConn.getInputStream();
        if(null == istream) { return null; }
        final TextureData texData = TextureIO.newTextureData(gl.getGLProfile(), istream, false /* mipmap */, TextureIO.PNG);
        final Texture res = TextureIO.newTexture(gl, texData);
        texData.destroy();
        return res;
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();
        if(null!=texture) {
            texture.disable(gl);
            texture.destroy(gl);
        }

        if(null != st) {
            pmvMatrixUniform = null;
            pmvMatrix=null;
            st.destroy(gl);
            st=null;
        }
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL3 gl = drawable.getGL().getGL3();

        gl.setSwapInterval(1);

        // Clear background to white
        gl.glClearColor(1.0f, 1.0f, 1.0f, 0.4f);

        if(null != st) {
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmvMatrix.glLoadIdentity();
            pmvMatrix.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 10.0f);

            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmvMatrix.glLoadIdentity();

            st.useProgram(gl, true);
            st.uniform(gl, pmvMatrixUniform);
            st.useProgram(gl, false);
        }
    }

    @Override
    public void display(final GLAutoDrawable drawable)  {
        final GL3 gl = drawable.getGL().getGL3();

        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

        if(null != st) {
            //Draw the image as a pseudo-quad using two triangles
            st.useProgram(gl, true);
            interleavedVBO.enableBuffer(gl, true);
            gl.glActiveTexture(GL.GL_TEXTURE0);
            texture.enable(gl);
            texture.bind(gl);

            gl.glDrawArrays(GL.GL_TRIANGLES, 0, 6);

            texture.disable(gl);
            interleavedVBO.enableBuffer(gl, false);
            st.useProgram(gl, false);
        }
    }//end display()

    private static final float[] s_triVertices = {
           -1f,  1f, // TL
           -1f, -1f, // BL
            1f, -1f, // BR
           -1f,  1f, // TL
            1f,  1f, // TR
            1f, -1f  // BR
    };
    private static final float[] s_triColors = {
            1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f
    };
    private static final float[] s_triTexCoords = {
            0f, 1f, // TL
            0f, 0f, // BL
            1f, 0f, // BR
            0f, 1f, // TL
            1f, 1f, // TR
            1f, 0f  // BR
    };

}//end Test
