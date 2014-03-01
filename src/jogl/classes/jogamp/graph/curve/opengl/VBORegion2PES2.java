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
package jogamp.graph.curve.opengl;

import java.nio.FloatBuffer;

import javax.media.opengl.GL2ES2;
// FIXME: Subsume GL2GL3.GL_DRAW_FRAMEBUFFER -> GL2ES2.GL_DRAW_FRAMEBUFFER !
import javax.media.opengl.GL;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import jogamp.graph.curve.opengl.shader.AttributeNames;
import jogamp.graph.curve.opengl.shader.UniformNames;

import com.jogamp.common.nio.Buffers;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

public class VBORegion2PES2  extends GLRegion {
    private static final boolean DEBUG_FBO_1 = false;
    private static final boolean DEBUG_FBO_2 = false;
    private GLArrayDataServer verticeTxtAttr;
    private GLArrayDataServer texCoordTxtAttr;
    private GLArrayDataServer indicesTxtBuffer;
    private GLArrayDataServer verticeFboAttr;
    private GLArrayDataServer texCoordFboAttr;
    private GLArrayDataServer indicesFbo;

    private FBObject fbo;
    private TextureAttachment texA;
    private final PMVMatrix fboPMVMatrix;
    GLUniformData mgl_fboPMVMatrix;

    private int fboWidth = 0;
    private int fboHeight = 0;
    GLUniformData mgl_ActiveTexture;
    GLUniformData mgl_TextureSize; // if GLSL < 1.30

    final int[] maxTexSize = new int[] { -1 } ;

    public VBORegion2PES2(final int renderModes, final int textureUnit) {
        super(renderModes);
        final int initialElementCount = 256;
        fboPMVMatrix = new PMVMatrix();
        mgl_fboPMVMatrix = new GLUniformData(UniformNames.gcu_PMVMatrix, 4, 4, fboPMVMatrix.glGetPMvMatrixf());
        mgl_ActiveTexture = new GLUniformData(UniformNames.gcu_TextureUnit, textureUnit);

        indicesTxtBuffer = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, initialElementCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
        verticeTxtAttr = GLArrayDataServer.createGLSL(AttributeNames.VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                                                      false, initialElementCount, GL.GL_STATIC_DRAW);
        texCoordTxtAttr = GLArrayDataServer.createGLSL(AttributeNames.TEXCOORD_ATTR_NAME, 2, GL2ES2.GL_FLOAT,
                                                       false, initialElementCount, GL.GL_STATIC_DRAW);
    }

    @Override
    protected final void clearImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        indicesTxtBuffer.seal(gl, false);
        indicesTxtBuffer.rewind();
        verticeTxtAttr.seal(gl, false);
        verticeTxtAttr.rewind();
        texCoordTxtAttr.seal(gl, false);
        texCoordTxtAttr.rewind();
    }

    @Override
    protected final void pushVertex(float[] coords, float[] texParams) {
        verticeTxtAttr.putf(coords[0]);
        verticeTxtAttr.putf(coords[1]);
        verticeTxtAttr.putf(coords[2]);

        texCoordTxtAttr.putf(texParams[0]);
        texCoordTxtAttr.putf(texParams[1]);
    }

    @Override
    protected final void pushIndex(int idx) {
        indicesTxtBuffer.puts((short)idx);
    }

    @Override
    protected void update(final GL2ES2 gl, final RegionRenderer renderer) {
        if(null == indicesFbo) {
            final ShaderState st = renderer.getShaderState();

            indicesFbo = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, 2, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
            indicesFbo.puts((short) 0); indicesFbo.puts((short) 1); indicesFbo.puts((short) 3);
            indicesFbo.puts((short) 1); indicesFbo.puts((short) 2); indicesFbo.puts((short) 3);
            indicesFbo.seal(true);

            texCoordFboAttr = GLArrayDataServer.createGLSL(AttributeNames.TEXCOORD_ATTR_NAME, 2, GL2ES2.GL_FLOAT,
                                                           false, 4, GL.GL_STATIC_DRAW);
            st.ownAttribute(texCoordFboAttr, true);
            texCoordFboAttr.putf(5); texCoordFboAttr.putf(5);
            texCoordFboAttr.putf(5); texCoordFboAttr.putf(6);
            texCoordFboAttr.putf(6); texCoordFboAttr.putf(6);
            texCoordFboAttr.putf(6); texCoordFboAttr.putf(5);
            texCoordFboAttr.seal(true);

            verticeFboAttr = GLArrayDataServer.createGLSL(AttributeNames.VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                                                          false, 4, GL.GL_STATIC_DRAW);
            st.ownAttribute(verticeFboAttr, true);

            st.ownAttribute(verticeTxtAttr, true);
            st.ownAttribute(texCoordTxtAttr, true);

            if(Region.DEBUG_INSTANCE) {
                System.err.println("VBORegion2PES2 Create: " + this);
            }
        }
        // seal buffers
        indicesTxtBuffer.seal(gl, true);
        indicesTxtBuffer.enableBuffer(gl, false);
        texCoordTxtAttr.seal(gl, true);
        texCoordTxtAttr.enableBuffer(gl, false);
        verticeTxtAttr.seal(gl, true);
        verticeTxtAttr.enableBuffer(gl, false);

        // update all bbox related data
        verticeFboAttr.seal(gl, false);
        verticeFboAttr.rewind();
        verticeFboAttr.putf(box.getLow()[0]);  verticeFboAttr.putf(box.getLow()[1]);  verticeFboAttr.putf(box.getLow()[2]);
        verticeFboAttr.putf(box.getLow()[0]);  verticeFboAttr.putf(box.getHigh()[1]); verticeFboAttr.putf(box.getLow()[2]);
        verticeFboAttr.putf(box.getHigh()[0]); verticeFboAttr.putf(box.getHigh()[1]); verticeFboAttr.putf(box.getLow()[2]);
        verticeFboAttr.putf(box.getHigh()[0]); verticeFboAttr.putf(box.getLow()[1]);  verticeFboAttr.putf(box.getLow()[2]);
        verticeFboAttr.seal(gl, true);
        verticeFboAttr.enableBuffer(gl, false);

        fboPMVMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        fboPMVMatrix.glLoadIdentity();
        fboPMVMatrix.glOrthof(box.getLow()[0], box.getHigh()[0], box.getLow()[1], box.getHigh()[1], -1, 1);

        // push data 2 GPU ..
        indicesFbo.seal(gl, true);
        indicesFbo.enableBuffer(gl, false);

        // trigger renderRegion2FBO !
        fboHeight = 0;
        fboWidth = 0;
        // the buffers were disabled, since due to real/fbo switching and other vbo usage
    }

    @Override
    protected void drawImpl(final GL2ES2 gl, final RegionRenderer renderer, final int[/*1*/] sampleCount) {
        final int width = renderer.getWidth();
        final int height = renderer.getHeight();
        if(width <=0 || height <= 0 || null==sampleCount || sampleCount[0] <= 0){
            renderRegion(gl);
        } else {
            if(0 > maxTexSize[0]) {
                gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, maxTexSize, 0);
            }
            final RenderState rs = renderer.getRenderState();
            final float winWidth, winHeight;
            int targetFboWidth, targetFboHeight;
            {
                // Calculate perspective pixel width/height for FBO,
                // considering the sampleCount.
                final int[] view = new int[] { 0, 0, width, height };
                final float objZ = 0f;

                final float[] winPosSzMin = new float[3];
                final float[] winPosSzMax = new float[3];
                final PMVMatrix pmv = renderer.getMatrix();
                pmv.gluProject(box.getMinX(), box.getMinY(), objZ, view, 0, winPosSzMin, 0);
                pmv.gluProject(box.getMaxX(), box.getMaxY(), objZ, view, 0, winPosSzMax, 0);
                winWidth = Math.abs(winPosSzMax[0] - winPosSzMin[0]);
                winHeight = Math.abs(winPosSzMax[1] - winPosSzMin[1]);
                targetFboWidth = Math.round(winWidth*sampleCount[0]);
                targetFboHeight= Math.round(winHeight*sampleCount[0]);
                if( DEBUG_FBO_2 ) {
                    System.err.printf("XXX.MinMax1 min [%.1f, %.1f -> %.3f, %.3f, %.3f]"+
                                      ", max [%.1f, %.1f -> %.3f, %.3f, %.3f], view[%d, %d] -> [%f x %f]%n",
                            box.getMinX(), box.getMinY(), winPosSzMin[0], winPosSzMin[1], winPosSzMin[2],
                            box.getMaxX(), box.getMaxY(), winPosSzMax[0], winPosSzMax[1], winPosSzMax[2],
                            view[2], view[3], winWidth, winHeight);
                    System.err.printf("XXX.MinMax1 [%f x %f] * %d = %d x %d%n", winWidth, winHeight, sampleCount[0], targetFboWidth, targetFboHeight);
                }
            }
            final int deltaFboWidth = Math.abs(targetFboWidth-fboWidth);
            final int deltaFboHeight = Math.abs(targetFboHeight-fboHeight);
            final int maxDeltaFbo, maxLengthFbo;
            if( deltaFboWidth >= deltaFboHeight ) {
                maxDeltaFbo = deltaFboWidth;
                maxLengthFbo = fboWidth > 0 ? fboWidth : 1;
            } else {
                maxDeltaFbo = deltaFboHeight;
                maxLengthFbo = fboHeight > 0 ? fboHeight : 1;
            }
            final float pctFboDelta = (float)maxDeltaFbo / (float)maxLengthFbo;
            if( DEBUG_FBO_2 ) {
                System.err.printf("XXX.maxDelta: %d / %d = %.3f%n", maxDeltaFbo, maxLengthFbo, pctFboDelta);
            }
            if( pctFboDelta > 0.1f ) { // more than 10% !
                if( DEBUG_FBO_1 ) {
                    System.err.printf("XXX.maxDelta: %d / %d = %.3f%n", maxDeltaFbo, maxLengthFbo, pctFboDelta);
                    System.err.printf("XXX.Scale %d * [%f x %f]: %dx%d%n",
                            sampleCount[0], winWidth, winHeight, targetFboWidth, targetFboHeight);
                }
                final int maxLength = Math.max(targetFboWidth, targetFboHeight);
                if( maxLength > maxTexSize[0] ) {
                    if( targetFboWidth > targetFboHeight ) {
                        sampleCount[0] = (int)Math.floor(maxTexSize[0] / winWidth);
                    } else {
                        sampleCount[0] = (int)Math.floor(maxTexSize[0] / winHeight);
                    }
                    targetFboWidth = Math.round(winWidth*sampleCount[0]);
                    targetFboHeight= Math.round(winHeight*sampleCount[0]);
                    if( DEBUG_FBO_1 ) {
                        System.err.printf("XXX.Rescale (MAX): %d * [%f x %f]: %dx%d%n",
                                sampleCount[0], winWidth, winHeight, targetFboWidth, targetFboHeight);
                    }
                }
                renderRegion2FBO(gl, rs, targetFboWidth, targetFboHeight);
            }
            // System.out.println("Scale: " + matrix.glGetMatrixf().get(1+4*3) +" " + matrix.glGetMatrixf().get(2+4*3));
            renderFBO(gl, rs, width, height);
        }
    }

    private void renderFBO(final GL2ES2 gl, final RenderState rs, final int width, final int hight) {
        final ShaderState st = rs.getShaderState();

        gl.glViewport(0, 0, width, hight);
        st.uniform(gl, mgl_ActiveTexture);
        gl.glActiveTexture(GL.GL_TEXTURE0 + mgl_ActiveTexture.intValue());
        setTexSize(gl, st);

        fbo.use(gl, texA);
        verticeFboAttr.enableBuffer(gl, true);
        texCoordFboAttr.enableBuffer(gl, true);
        indicesFbo.bindBuffer(gl, true); // keeps VBO binding

        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesFbo.getElementCount() * indicesFbo.getComponentCount(), GL2ES2.GL_UNSIGNED_SHORT, 0);

        indicesFbo.bindBuffer(gl, false);
        texCoordFboAttr.enableBuffer(gl, false);
        verticeFboAttr.enableBuffer(gl, false);
        fbo.unuse(gl);

        // setback: gl.glActiveTexture(currentActiveTextureEngine[0]);
    }

    private void renderRegion2FBO(final GL2ES2 gl, final RenderState rs, final int targetFboWidth, final int targetFboHeight) {
        final ShaderState st = rs.getShaderState();

        if( 0 >= targetFboWidth || 0 >= targetFboHeight ) {
            throw new IllegalArgumentException("fboSize must be greater than 0: "+targetFboWidth+"x"+targetFboHeight);
        }

        if(null == fbo) {
            fboWidth  = targetFboWidth;
            fboHeight  = targetFboHeight;
            fbo = new FBObject();
            fbo.reset(gl, fboWidth, fboHeight);
            // FIXME: shall not use bilinear, due to own AA ? However, w/o bilinear result is not smooth
            texA = fbo.attachTexture2D(gl, 0, true, GL2ES2.GL_LINEAR, GL2ES2.GL_LINEAR, GL2ES2.GL_CLAMP_TO_EDGE, GL2ES2.GL_CLAMP_TO_EDGE);
            // texA = fbo.attachTexture2D(gl, 0, GL2ES2.GL_NEAREST, GL2ES2.GL_NEAREST, GL2ES2.GL_CLAMP_TO_EDGE, GL2ES2.GL_CLAMP_TO_EDGE);
            fbo.attachRenderbuffer(gl, Attachment.Type.DEPTH, 24);
            if( DEBUG_FBO_1 ) {
                System.err.printf("XXX.createFBO: %dx%d%n%s%n", fboWidth, fboHeight, fbo.toString());
            }
        } else if( targetFboWidth != fboWidth || targetFboHeight != fboHeight ) {
            fbo.reset(gl, targetFboWidth, targetFboHeight);
            fbo.bind(gl);
            if( DEBUG_FBO_1 ) {
                System.err.printf("XXX.resetFBO: %dx%d -> %dx%d%n%s%n", fboWidth, fboHeight, targetFboWidth, targetFboHeight, fbo );
            }
            fboWidth  = targetFboWidth;
            fboHeight  = targetFboHeight;
        } else {
            fbo.bind(gl);
        }

        //render texture
        gl.glViewport(0, 0, fboWidth, fboHeight);
        st.uniform(gl, mgl_fboPMVMatrix); // use orthogonal matrix

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT | GL2ES2.GL_DEPTH_BUFFER_BIT);
        renderRegion(gl);
        fbo.unbind(gl);

        st.uniform(gl, rs.getPMVMatrix()); // switch back to real PMV matrix
    }
    private void setTexSize(final GL2ES2 gl, final ShaderState st) {
        // if( !gl.isGL3() ) {
            // GLSL < 1.30
            if(null == mgl_TextureSize) {
                mgl_TextureSize = new GLUniformData(UniformNames.gcu_TextureSize, 2, Buffers.newDirectFloatBuffer(2));
            }
            final FloatBuffer texSize = (FloatBuffer) mgl_TextureSize.getBuffer();
            texSize.put(0, fboWidth);
            texSize.put(1, fboHeight);
            st.uniform(gl, mgl_TextureSize);
        //}
    }

    private void renderRegion(final GL2ES2 gl) {
        verticeTxtAttr.enableBuffer(gl, true);
        texCoordTxtAttr.enableBuffer(gl, true);
        indicesTxtBuffer.bindBuffer(gl, true); // keeps VBO binding

        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesTxtBuffer.getElementCount() * indicesTxtBuffer.getComponentCount(), GL2ES2.GL_UNSIGNED_SHORT, 0);

        indicesTxtBuffer.bindBuffer(gl, false);
        texCoordTxtAttr.enableBuffer(gl, false);
        verticeTxtAttr.enableBuffer(gl, false);
    }

    @Override
    protected void destroyImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegion2PES2 Destroy: " + this);
        }
        final ShaderState st = renderer.getShaderState();
        if(null != fbo) {
            fbo.destroy(gl);
            fbo = null;
            texA = null;
        }
        if(null != verticeTxtAttr) {
            st.ownAttribute(verticeTxtAttr, false);
            verticeTxtAttr.destroy(gl);
            verticeTxtAttr = null;
        }
        if(null != texCoordTxtAttr) {
            st.ownAttribute(texCoordTxtAttr, false);
            texCoordTxtAttr.destroy(gl);
            texCoordTxtAttr = null;
        }
        if(null != indicesTxtBuffer) {
            indicesTxtBuffer.destroy(gl);
            indicesTxtBuffer = null;
        }
        if(null != verticeFboAttr) {
            st.ownAttribute(verticeFboAttr, false);
            verticeFboAttr.destroy(gl);
            verticeFboAttr = null;
        }
        if(null != texCoordFboAttr) {
            st.ownAttribute(texCoordFboAttr, false);
            texCoordFboAttr.destroy(gl);
            texCoordFboAttr = null;
        }
        if(null != indicesFbo) {
            indicesFbo.destroy(gl);
            indicesFbo = null;
        }
    }
}
