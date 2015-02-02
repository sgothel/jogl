/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLES2;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.common.os.Platform;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureSequence;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

public class TextureSequenceCubeES2 implements GLEventListener {
    public TextureSequenceCubeES2 (final TextureSequence texSource, final boolean innerCube, final float zoom0, final float rotx, final float roty) {
        this.texSeq = texSource;
        this.innerCube = innerCube;
        this.zoom      = zoom0;
        this.view_rotx = rotx;
        this.view_roty = roty;
    }

    private TextureSequence texSeq;
    public ShaderState st;
    public PMVMatrix pmvMatrix;
    private GLUniformData pmvMatrixUniform;
    // private TextureCoords[] textureCoords = null;
    private float nearPlaneNormalized;
    // private float zoom0=-5.0f, zoom=zoom0;
    // private float view_rotx = 20.0f, view_roty = 30.0f, view_rotz = 0.0f;
    public float zoom=-2.3f;
    private float view_rotx = 0.0f, view_roty = 0.0f;
    private final float view_rotz = 0.0f;
    int[] vboNames = new int[4];
    boolean innerCube;

    private final MouseListener mouseAction = new MouseAdapter() {
        int lx = 0;
        int ly = 0;
        boolean first = false;

        public void mousePressed(final MouseEvent e) {
            first = true;
        }
        public void mouseMoved(final MouseEvent e) {
            first = false;
        }
        public void mouseDragged(final MouseEvent e) {
            int width, height;
            final Object source = e.getSource();
            Window window = null;
            if(source instanceof Window) {
                window = (Window) source;
                width=window.getSurfaceWidth();
                height=window.getSurfaceHeight();
            } else if (source instanceof GLAutoDrawable) {
                final GLAutoDrawable glad = (GLAutoDrawable) source;
                width = glad.getSurfaceWidth();
                height = glad.getSurfaceHeight();
            } else if (GLProfile.isAWTAvailable() && source instanceof java.awt.Component) {
                final java.awt.Component comp = (java.awt.Component) source;
                width=comp.getWidth(); // FIXME HiDPI: May need to convert window units -> pixel units!
                height=comp.getHeight();
            } else {
                throw new RuntimeException("Event source neither Window nor Component: "+source);
            }
            if(e.getPointerCount()==2) {
                // 2 pointers zoom ..
                if(first) {
                    lx = Math.abs(e.getY(0)-e.getY(1));
                    first=false;
                    return;
                }
                final int nv = Math.abs(e.getY(0)-e.getY(1));
                final int dy = nv - lx;

                {
                    final float o = zoom;
                    final float d = 40f*Math.signum(dy)/height;
                    zoom += d;
                    System.err.println("zoom.d: "+o+" + "+d+" -> "+zoom);
                }

                lx = nv;
            } else {
                // 1 pointer rotate
                if(first) {
                    lx = e.getX();
                    ly = e.getY();
                    first=false;
                    return;
                }
                final int nx = e.getX();
                final int ny = e.getY();
                view_roty += 360f * ( (float)( nx - lx ) / (float)width );
                view_rotx += 360f * ( (float)( ny - ly ) / (float)height );
                lx = nx;
                ly = ny;
            }
        }
        public void mouseWheelMoved(final MouseEvent e) {
            // System.err.println("XXX "+e);
            if( !e.isShiftDown() ) {
                final float o = zoom;
                final float d = e.getRotation()[1]/10f; // vertical: wheel
                zoom += d;
                System.err.println("zoom.w: "+o+" + "+d+" -> "+zoom);
            }
        }
    };

    static final String shaderBasename = "texsequence_xxx";
    static final String myTextureLookupName = "myTexture2D";

    private void initShader(final GL2ES2 gl) {
        // Create & Compile the shader objects
        final ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(),
                                            "shader", "shader/bin", shaderBasename, true);
        final ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(),
                                            "shader", "shader/bin", shaderBasename, true);

        boolean preludeGLSLVersion = true;
        if( GLES2.GL_TEXTURE_EXTERNAL_OES == texSeq.getTextureTarget() ) {
            if( !gl.isExtensionAvailable(GLExtensions.OES_EGL_image_external) ) {
                throw new GLException(GLExtensions.OES_EGL_image_external+" requested but not available");
            }
            if( Platform.OSType.ANDROID == Platform.getOSType() && gl.isGLES3() ) {
                // Bug on Nexus 10, ES3 - Android 4.3, where
                // GL_OES_EGL_image_external extension directive leads to a failure _with_ '#version 300 es' !
                //   P0003: Extension 'GL_OES_EGL_image_external' not supported
                preludeGLSLVersion = false;
            }
        }
        rsVp.defaultShaderCustomization(gl, preludeGLSLVersion, true);

        int rsFpPos = preludeGLSLVersion ? rsFp.addGLSLVersion(gl) : 0;
        rsFpPos = rsFp.insertShaderSource(0, rsFpPos, texSeq.getRequiredExtensionsShaderStub());
        rsFp.addDefaultShaderPrecision(gl, rsFpPos);

        final String texLookupFuncName = texSeq.getTextureLookupFunctionName(myTextureLookupName);
        rsFp.replaceInShaderSource(myTextureLookupName, texLookupFuncName);

        // Inject TextureSequence shader details
        final StringBuilder sFpIns = new StringBuilder();
        sFpIns.append("uniform ").append(texSeq.getTextureSampler2DType()).append(" mgl_ActiveTexture;\n");
        sFpIns.append(texSeq.getTextureLookupFragmentShaderImpl());
        rsFp.insertShaderSource(0, "TEXTURE-SEQUENCE-CODE-BEGIN", 0, sFpIns);

        // Create & Link the shader program
        final ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);
        if(!sp.link(gl, System.err)) {
            throw new GLException("Couldn't link program: "+sp);
        }

        // Let's manage all our states using ShaderState.
        st = new ShaderState();
        st.attachShaderProgram(gl, sp, false);
    }

    GLArrayDataServer interleavedVBO, cubeIndicesVBO;

    public void init(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        System.err.println(JoglVersion.getGLInfo(gl, null));
        final TextureFrame frame = texSeq.getLastTexture();
        if( null == frame ) {
            return;
        }
        final Texture tex= frame.getTexture();

        initShader(gl);

        // Push the 1st uniform down the path
        st.useProgram(gl, true);

        pmvMatrix = new PMVMatrix();
        reshapePMV(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf()); // P, Mv
        if(!st.uniform(gl, pmvMatrixUniform)) {
            throw new GLException("Error setting PMVMatrix in shader: "+st);
        }
        if(!st.uniform(gl, new GLUniformData("mgl_ActiveTexture", texSeq.getTextureUnit()))) {
            throw new GLException("Error setting mgl_ActiveTexture in shader: "+st);
        }


        // calculate centered tex coords w/ aspect ratio
        final float[] fixedCubeTexCoords = new float[s_cubeTexCoords.length];
        {
            final float aspect = tex.getAspectRatio();
            final TextureCoords tc = tex.getImageTexCoords();
            System.err.println("XXX0: aspect: "+aspect);
            System.err.println("XXX0: y-flip: "+tex.getMustFlipVertically());
            System.err.println("XXX0: "+tc);
            final float tc_x1 = Math.max(tc.left(), tc.right());
            final float tc_y1 = Math.max(tc.bottom(), tc.top());
            final float ss=1f, ts=aspect; // scale tex-coord
            final float dy = ( 1f - aspect ) / 2f ;
            for(int i=0; i<s_cubeTexCoords.length; i+=2) {
                final float tx = s_cubeTexCoords[i+0];
                final float ty = s_cubeTexCoords[i+1];
                if(tx!=0) {
                    fixedCubeTexCoords[i+0] = tc_x1 * ss;
                }
                if(ty==0 && !tex.getMustFlipVertically() || ty!=0 && tex.getMustFlipVertically()) {
                    fixedCubeTexCoords[i+1] = 0f         + dy;
                } else {
                    fixedCubeTexCoords[i+1] = tc_y1 * ts + dy;
                }
            }
        }

        interleavedVBO = GLArrayDataServer.createGLSLInterleaved(3+4+2, GL.GL_FLOAT, false, 3*6*4, GL.GL_STATIC_DRAW);
        {
            interleavedVBO.addGLSLSubArray("mgl_Vertex",        3, GL.GL_ARRAY_BUFFER);
            interleavedVBO.addGLSLSubArray("mgl_Color",         4, GL.GL_ARRAY_BUFFER);
            //interleavedVBO.addGLSLSubArray("mgl_Normal",        3, GL.GL_ARRAY_BUFFER);
            interleavedVBO.addGLSLSubArray("mgl_MultiTexCoord", 2, GL.GL_ARRAY_BUFFER);

            final FloatBuffer ib = (FloatBuffer)interleavedVBO.getBuffer();

            for(int i=0; i<6*4; i++) {
                ib.put(s_cubeVertices,  i*3, 3);
                ib.put(s_cubeColors,    i*4, 4);
                //ib.put(s_cubeNormals,   i*3, 3);
                ib.put(fixedCubeTexCoords, i*2, 2);
            }
        }
        interleavedVBO.seal(gl, true);
        interleavedVBO.enableBuffer(gl, false);
        st.ownAttribute(interleavedVBO, true);

        cubeIndicesVBO = GLArrayDataServer.createData(6, GL.GL_UNSIGNED_SHORT, 6, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
        for(int i=0; i<6*6; i++) {
            cubeIndicesVBO.puts(s_cubeIndices[i]);
        }
        cubeIndicesVBO.seal(gl, true);
        cubeIndicesVBO.enableBuffer(gl, false);
        st.ownAttribute(cubeIndicesVBO, true);


        gl.glEnable(GL.GL_DEPTH_TEST);

        st.useProgram(gl, false);

        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {
            final Window window = (Window) upstreamWidget;
            window.addMouseListener(mouseAction);
        } else if (GLProfile.isAWTAvailable() && upstreamWidget instanceof java.awt.Component) {
            final java.awt.Component comp = (java.awt.Component) upstreamWidget;
            new com.jogamp.newt.event.awt.AWTMouseAdapter(mouseAction, drawable).addTo(comp);
        }

        // Let's show the completed shader state ..
        System.out.println("iVBO: "+interleavedVBO);
        System.out.println(st);
    }

    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glViewport(0, 0, width, height);

        if(!innerCube) {
            // lights on
        } else {
            // lights off
        }
        // gl.glEnable(GL.GL_CULL_FACE);
        // gl.glDisable(GL.GL_DITHER);

        if(null != st) {
            reshapePMV(width, height);
            st.useProgram(gl, true);
            st.uniform(gl, pmvMatrixUniform);
            st.useProgram(gl, false);
        }
    }


    private void reshapePMV(final int width, final int height) {
        if(null != pmvMatrix) {
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmvMatrix.glLoadIdentity();
            if(!innerCube) {
                pmvMatrix.gluPerspective(45.0f, (float)width / (float)height, 1f, 10.0f);
                nearPlaneNormalized = 1f/(100f-1f);
            } else {
                pmvMatrix.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 10.0f);
                nearPlaneNormalized = 0f;
            }
            System.err.println("XXX0: Perspective nearPlaneNormalized: "+nearPlaneNormalized);

            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmvMatrix.glLoadIdentity();
            pmvMatrix.glTranslatef(0, 0, zoom);
        }
    }


    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        texSeq = null;
        pmvMatrixUniform = null;
        pmvMatrix=null;
        if( null != st ) {
            st.destroy(gl);
            st=null;
        }
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        if(innerCube) {
            // Clear background to white
            gl.glClearColor(1.0f, 1.0f, 1.0f, 0.4f);
        } else {
            // Clear background to blue
            gl.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
        }
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        if( null == st ) {
            return;
        }

        st.useProgram(gl, true);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, zoom);
        pmvMatrix.glRotatef(view_rotx, 1.0f, 0.0f, 0.0f);
        pmvMatrix.glRotatef(view_roty, 0.0f, 1.0f, 0.0f);
        pmvMatrix.glRotatef(view_rotz, 0.0f, 0.0f, 1.0f);
        st.uniform(gl, pmvMatrixUniform);
        interleavedVBO.enableBuffer(gl, true);
        Texture tex = null;
        if(null!=texSeq) {
            final TextureSequence.TextureFrame texFrame = texSeq.getNextTexture(gl);
            if(null != texFrame) {
                tex = texFrame.getTexture();
                gl.glActiveTexture(GL.GL_TEXTURE0+texSeq.getTextureUnit());
                tex.enable(gl);
                tex.bind(gl);
            }
        }
        cubeIndicesVBO.bindBuffer(gl, true); // keeps VBO binding
        gl.glDrawElements(GL.GL_TRIANGLES, cubeIndicesVBO.getElementCount() * cubeIndicesVBO.getComponentCount(), GL.GL_UNSIGNED_SHORT, 0);
        cubeIndicesVBO.bindBuffer(gl, false);

        if(null != tex) {
            tex.disable(gl);
        }
        interleavedVBO.enableBuffer(gl, false);
        st.useProgram(gl, false);
    }

    static final float[] light_position = { -50.f, 50.f, 50.f, 0.f };
    static final float[] light_ambient = { 0.125f, 0.125f, 0.125f, 1.f };
    static final float[] light_diffuse = { 1.0f, 1.0f, 1.0f, 1.f };
    static final float[] material_spec = { 1.0f, 1.0f, 1.0f, 0.f };
    static final float[] zero_vec4 = { 0.0f, 0.0f, 0.0f, 0.f };

    private static final float[] s_cubeVertices = /* f b t b r l */
        {
            -1f,  1f,  1f,    1f, -1f,  1f,    1f,  1f,  1f,   -1f, -1f,  1f,

            -1f,  1f, -1f,    1f, -1f, -1f,    1f,  1f, -1f,   -1f, -1f, -1f,

            -1f, -1f,  1f,    1f, -1f, -1f,    1f, -1f,  1f,   -1f, -1f, -1f,

            -1f,  1f,  1f,    1f,  1f, -1f,    1f,  1f,  1f,   -1f,  1f, -1f,

             1f, -1f,  1f,    1f,  1f, -1f,    1f,  1f,  1f,    1f, -1f, -1f,

            -1f, -1f,  1f,   -1f,  1f, -1f,   -1f,  1f,  1f,   -1f, -1f, -1f
        };

    private static final float[] s_cubeTexCoords =
        { // LT       RB        RT       LB
            0f, 1f,   1f, 0f,   1f, 1f,   0f, 0f,

            0f, 1f,   1f, 0f,   1f, 1f,   0f, 0f,

            0f, 1f,   1f, 0f,   1f, 1f,   0f, 0f,

            0f, 1f,   1f, 0f,   1f, 1f,   0f, 0f,

            0f, 0f,   1f, 1f,   0f, 1f,   1f, 0f,

            0f, 0f,   1f, 1f,   0f, 1f,   1f, 0f,
        };

    private static final float[] s_cubeColors =
        {
            1f, 1f, 1f, 1f,     1f, 1f, 1f, 1f,            1f, 1f, 1f, 1f,     1f, 1f, 1f, 1f,

            40f/255f, 80f/255f, 160f/255f, 255f/255f,      40f/255f, 80f/255f, 160f/255f, 255f/255f,
            40f/255f, 80f/255f, 160f/255f, 255f/255f,      40f/255f, 80f/255f, 160f/255f, 255f/255f,

            40f/255f, 80f/255f, 160f/255f, 255f/255f,      40f/255f, 80f/255f, 160f/255f, 255f/255f,
            40f/255f, 80f/255f, 160f/255f, 255f/255f,      40f/255f, 80f/255f, 160f/255f, 255f/255f,

            128f/255f, 128f/255f, 128f/255f, 255f/255f,   128f/255f, 128f/255f, 128f/255f, 255f/255f,
            128f/255f, 128f/255f, 128f/255f, 255f/255f,   128f/255f, 128f/255f, 128f/255f, 255f/255f,

            255f/255f, 110f/255f, 10f/255f, 255f/255f,    255f/255f, 110f/255f, 10f/255f, 255f/255f,
            255f/255f, 110f/255f, 10f/255f, 255f/255f,    255f/255f, 110f/255f, 10f/255f, 255f/255f,

            255f/255f, 70f/255f, 60f/255f, 255f/255f,     255f/255f, 70f/255f, 60f/255f, 255f/255f,
            255f/255f, 70f/255f, 60f/255f, 255f/255f,     255f/255f, 70f/255f, 60f/255f, 255f/255f
        };

    /*
    private static final float[] s_cubeNormals =
        {
             0f,  0f,  1f,    0f,  0f,  1f,    0f,  0f,  1f,    0f,  0f,  1f,

             0f,  0f, -1f,    0f,  0f, -1f,    0f,  0f, -1f,    0f,  0f, -1f,

             0f, -1f,  0f,    0f, -1f,  0f,    0f, -1f,  0f,    0f, -1f,  0f,

             0f,  1f,  0f,    0f,  1f,  0f,    0f,  1f,  0f,    0f,  1f,  0f,

             1f,  0f,  0f,    1f,  0f,  0f,    1f,  0f,  0f,    1f,  0f,  0f,

            -1f,  0f,  0f,   -1f,  0f,  0f,   -1f,  0f,  0f,   -1f,  0f,  0f
        };*/
    private static final short[] s_cubeIndices =
        {
             0,  3,  1,  2,  0,  1, /* front  */
             6,  5,  4,  5,  7,  4, /* back   */
             8, 11,  9, 10,  8,  9, /* top    */
            15, 12, 13, 12, 14, 13, /* bottom */
            16, 19, 17, 18, 16, 17, /* right  */
            23, 20, 21, 20, 22, 21  /* left   */
        };
}

