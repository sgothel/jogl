/**
 * Copyright 2012-2023 JogAmp Community. All rights reserved.
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

import com.jogamp.common.os.Platform;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec3f;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLES2;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureSequence;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

/**
 * Simple planar movie player w/ orthogonal 1:1 projection.
 */
public class TextureSequenceES2 implements GLEventListener {
    public static final int EFFECT_NORMAL                  =    0;
    public static final int EFFECT_GRADIENT_BOTTOM2TOP     = 1<<1;
    public static final int EFFECT_TRANSPARENT             = 1<<3;

    private TextureSequence texSeq;
    private final boolean texSeqShared;
    private ShaderState st;
    private PMVMatrix pmvMatrix;
    private GLUniformData pmvMatrixUniform;

    private float zrot = 0f;
    private final boolean  orthoProjection;
    private float nearPlaneNormalized;
    private float zoom;

    private int effects = EFFECT_NORMAL;
    private float alpha = 1.0f;

    private boolean useOriginalScale = false;
    private float[] verts = null;
    private GLArrayDataServer interleavedVBO;

    public TextureSequenceES2(final TextureSequence texSource, final boolean texSeqShared, final boolean orthoProjection, final float zoom0) throws IllegalStateException {
        this.texSeq = texSource;
        this.texSeqShared = texSeqShared;
        this.orthoProjection = orthoProjection;
        this.zoom = zoom0;
    }

    public void setZoom(final float zoom) { this.zoom = zoom; }
    public float getZoom() { return zoom; }
    public void setZRotation(final float zrot) { this.zrot = zrot; }
    public boolean hasEffect(final int e) { return 0 != ( effects & e ) ; }
    public void setEffects(final int e) { effects = e; };
    public void setTransparency(final float alpha) {
        this.effects |= EFFECT_TRANSPARENT;
        this.alpha = alpha;
    }
    public void setUseOriginalScale(final boolean v) {
        useOriginalScale = v;
    }

    private static final String shaderBasename = "texsequence_xxx";
    private static final String myTextureLookupName = "myTexture2D";

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

        final String texLookupFuncName = texSeq.setTextureLookupFunctionName(myTextureLookupName);
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

    @Override
    public void init(final GLAutoDrawable drawable) {
        if(null == texSeq) {
            throw new InternalError("texSeq null");
        }
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        final TextureFrame frame = texSeq.getLastTexture();
        if( null == frame ) {
            return;
        }
        final Texture tex = frame.getTexture();

        initShader(gl);

        // Push the 1st uniform down the path
        st.useProgram(gl, true);

        final Recti viewPort = new Recti(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        pmvMatrix = new PMVMatrix();
        reshapePMV(viewPort.width(), viewPort.height());
        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.getSyncPMv());
        if(!st.uniform(gl, pmvMatrixUniform)) {
            throw new GLException("Error setting PMVMatrix in shader: "+st);
        }
        if(!st.uniform(gl, new GLUniformData("mgl_ActiveTexture", texSeq.getTextureUnit()))) {
            throw new GLException("Error setting mgl_ActiveTexture in shader: "+st);
        }

        final float dWidth = drawable.getSurfaceWidth();
        final float dHeight = drawable.getSurfaceHeight();
        final float mWidth = tex.getImageWidth();
        final float mHeight = tex.getImageHeight();
        final float mAspect = mWidth/mHeight;
        System.err.println("XXX0: mov aspect: "+mAspect);
        float xs, ys;
        if(orthoProjection) {
            if(useOriginalScale && mWidth < dWidth && mHeight < dHeight) {
                xs   = mWidth/2f;                ys   = xs / mAspect;
            } else {
                xs   = dWidth/2f;                ys   = xs / mAspect; // w>h
            }
        } else {
            if(useOriginalScale && mWidth < dWidth && mHeight < dHeight) {
                xs   = mAspect * ( mWidth / dWidth ) ; ys   =  xs / mAspect ;
            } else {
                xs   = mAspect; ys   = 1f; // b>h
            }
        }
        verts = new float[] { -1f*xs, -1f*ys, 0f, // LB
                               1f*xs,  1f*ys, 0f  // RT
                            };
        {
            System.err.println("XXX0: pixel  LB: "+verts[0]+", "+verts[1]+", "+verts[2]);
            System.err.println("XXX0: pixel  RT: "+verts[3]+", "+verts[4]+", "+verts[5]);
            final Vec3f winLB = new Vec3f();
            final Vec3f winRT = new Vec3f();
            pmvMatrix.mapObjToWin(new Vec3f(verts[0], verts[1], verts[2]), viewPort, winLB);
            pmvMatrix.mapObjToWin(new Vec3f(verts[3], verts[4], verts[5]), viewPort, winRT);
            System.err.println("XXX0: win   LB: "+winLB);
            System.err.println("XXX0: win   RT: "+winRT);
        }

        interleavedVBO = GLArrayDataServer.createGLSLInterleaved(3+4+2, GL.GL_FLOAT, false, 3*4, GL.GL_STATIC_DRAW);
        {
            interleavedVBO.addGLSLSubArray("mgl_Vertex",        3, GL.GL_ARRAY_BUFFER);
            interleavedVBO.addGLSLSubArray("mgl_Color",         4, GL.GL_ARRAY_BUFFER);
            interleavedVBO.addGLSLSubArray("mgl_MultiTexCoord", 2, GL.GL_ARRAY_BUFFER);
        }
        updateInterleavedVBO(gl, tex);

        st.ownAttribute(interleavedVBO, true);
        gl.glClearColor(0.3f, 0.3f, 0.3f, 0.3f);

        gl.glEnable(GL.GL_DEPTH_TEST);

        st.useProgram(gl, false);

        // Let's show the completed shader state ..
        System.out.println("iVBO: "+interleavedVBO);
        System.out.println(st);
    }

    protected void updateInterleavedVBO(final GL gl, final Texture tex) {
        final float ss = 1f, ts = 1f; // scale tex-coord
        final boolean wasEnabled = interleavedVBO.enabled();
        interleavedVBO.seal(gl, false);
        interleavedVBO.rewind();
        {
            final FloatBuffer ib = (FloatBuffer)interleavedVBO.getBuffer();
            final TextureCoords tc = tex.getImageTexCoords();
            System.err.println("XXX0: "+tc);
            System.err.println("XXX0: tex aspect: "+tex.getAspectRatio());
            System.err.println("XXX0: tex y-flip: "+tex.getMustFlipVertically());

             // left-bottom
            ib.put(verts[0]);  ib.put(verts[1]);  ib.put(verts[2]);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                ib.put( 0);    ib.put( 0);     ib.put( 0);    ib.put(alpha);
            } else {
                ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            }
            ib.put( tc.left()   *ss);  ib.put( tc.bottom() *ts);

             // right-bottom
            ib.put(verts[3]);  ib.put(verts[1]);  ib.put(verts[2]);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                ib.put( 0);    ib.put( 0);     ib.put( 0);    ib.put(alpha);
            } else {
                ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            }
            ib.put( tc.right()  *ss);  ib.put( tc.bottom() *ts);

             // left-top
            ib.put(verts[0]);  ib.put(verts[4]);  ib.put(verts[2]);
            ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            ib.put( tc.left()   *ss);  ib.put( tc.top()    *ts);

             // right-top
            ib.put(verts[3]);  ib.put(verts[4]);  ib.put(verts[2]);
            ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            ib.put( tc.right()  *ss);  ib.put( tc.top()    *ts);
        }
        interleavedVBO.seal(gl, true);
        if( !wasEnabled ) {
            interleavedVBO.enableBuffer(gl, false);
        }
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        if(null != st) {
            reshapePMV(width, height);
            st.useProgram(gl, true);
            st.uniform(gl, pmvMatrixUniform);
            st.useProgram(gl, false);
        }

        System.out.println("pR "+texSeq);
    }

    private final float zNear = 1f;
    private final float zFar = 10f;

    private void reshapePMV(final int width, final int height) {
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        if(orthoProjection) {
            final float fw = width / 2f;
            final float fh = height/ 2f;
            pmvMatrix.glOrthof(-fw, fw, -fh, fh, -1.0f, 1.0f);
            nearPlaneNormalized = 0f;
        } else {
            pmvMatrix.gluPerspective(FloatUtil.QUARTER_PI, (float)width / (float)height, zNear, zFar);
            nearPlaneNormalized = 1f/(10f-1f);
        }
        System.err.println("XXX0: Perspective nearPlaneNormalized: "+nearPlaneNormalized);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, zoom);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        if(null == texSeq) { return; }
        disposeImpl(drawable, true);
    }
    private void disposeImpl(final GLAutoDrawable drawable, final boolean disposeTexSeq) {
        if( disposeTexSeq ) {
            texSeq = null;
        }
        pmvMatrixUniform = null;
        pmvMatrix=null;
        if(null != st) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            st.destroy(gl);
            st=null;
        }
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        if(null == texSeq) { return; }
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        if(null == st) {
            return;
        }

        st.useProgram(gl, true);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, zoom);
        if(zrot > 0f) {
            pmvMatrix.glRotatef(zrot, 0, 0, 1);
        } else {
            zrot = 0;
        }
        st.uniform(gl, pmvMatrixUniform);
        interleavedVBO.enableBuffer(gl, true);
        Texture tex = null;
        if(null!=texSeq) {
            final TextureSequence.TextureFrame texFrame;
            if( texSeqShared ) {
                texFrame = texSeq.getLastTexture();
            } else {
                texFrame = texSeq.getNextTexture(gl);
            }
            if(null != texFrame) {
                tex = texFrame.getTexture();
                gl.glActiveTexture(GL.GL_TEXTURE0+texSeq.getTextureUnit());
                tex.enable(gl);
                tex.bind(gl);
            }
        }
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        if(null != tex) {
            tex.disable(gl);
        }
        interleavedVBO.enableBuffer(gl, false);
        st.useProgram(gl, false);
    }
}
