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

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLUniformData;

import jogamp.graph.curve.opengl.shader.AttributeNames;
import jogamp.graph.curve.opengl.shader.UniformNames;

import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureSequence;

public class VBORegion2PMSAAES2  extends GLRegion {
    private static final boolean DEBUG_FBO_1 = false;
    private static final boolean DEBUG_FBO_2 = false;

    private final RenderState.ProgramLocal rsLocal;

    // Pass-1:
    private GLArrayDataServer gca_VerticesAttr;
    private GLArrayDataServer gca_CurveParamsAttr;
    private GLArrayDataServer gca_ColorsAttr;
    private GLArrayDataServer indicesBuffer;
    private final GLUniformData gcu_ColorTexUnit;
    private final float[] colorTexBBox; // x0, y0, x1, y1
    private final GLUniformData gcu_ColorTexBBox;
    private ShaderProgram spPass1 = null;

    // Pass-2:
    private GLArrayDataServer gca_FboVerticesAttr;
    private GLArrayDataServer gca_FboTexCoordsAttr;
    private GLArrayDataServer indicesFbo;
    private final GLUniformData gcu_FboTexUnit;
    private final float[] pmvMatrix02 = new float[2*16]; // P + Mv
    private final GLUniformData gcu_PMVMatrix02;
    private ShaderProgram spPass2 = null;

    private FBObject fbo;

    private int fboWidth = 0;
    private int fboHeight = 0;
    private boolean fboDirty = true;

    final int[] maxTexSize = new int[] { -1 } ;

    /**
     * <p>
     * Since multiple {@link Region}s may share one
     * {@link ShaderProgram}, the uniform data must always be updated.
     * </p>
     *
     * @param gl
     * @param renderer
     * @param renderModes
     * @param pass1
     * @param quality
     * @param sampleCount
     */
    public void useShaderProgram(final GL2ES2 gl, final RegionRenderer renderer, final int renderModes, final boolean pass1, final int quality, final int sampleCount) {
        final RenderState rs = renderer.getRenderState();
        final boolean updateLocGlobal = renderer.useShaderProgram(gl, renderModes, pass1, quality, sampleCount, colorTexSeq);
        final ShaderProgram sp = renderer.getRenderState().getShaderProgram();
        final boolean updateLocLocal;
        if( pass1 ) {
            updateLocLocal = !sp.equals(spPass1);
            spPass1 = sp;
            if( DEBUG ) {
                System.err.println("XXX changedSP.p1 updateLocation loc "+updateLocLocal+" / glob "+updateLocGlobal);
            }
            if( updateLocLocal ) {
                rs.updateAttributeLoc(gl, true, gca_VerticesAttr, true);
                rs.updateAttributeLoc(gl, true, gca_CurveParamsAttr, true);
                if( null != gca_ColorsAttr ) {
                    rs.updateAttributeLoc(gl, true, gca_ColorsAttr, true);
                }
            }
            rsLocal.update(gl, rs, updateLocLocal, renderModes, true, true);
            rs.updateUniformLoc(gl, updateLocLocal, gcu_PMVMatrix02, true);
            if( null != gcu_ColorTexUnit ) {
                rs.updateUniformLoc(gl, updateLocLocal, gcu_ColorTexUnit, true);
                rs.updateUniformLoc(gl, updateLocLocal, gcu_ColorTexBBox, true);
            }
        } else {
            updateLocLocal = !sp.equals(spPass2);
            spPass2 = sp;
            if( DEBUG ) {
                System.err.println("XXX changedSP.p2 updateLocation loc "+updateLocLocal+" / glob "+updateLocGlobal);
            }
            if( updateLocLocal ) {
                rs.updateAttributeLoc(gl, true, gca_FboVerticesAttr, true);
                rs.updateAttributeLoc(gl, true, gca_FboTexCoordsAttr, true);
            }
            rsLocal.update(gl, rs, updateLocLocal, renderModes, false, true);
            rs.updateUniformDataLoc(gl, updateLocLocal, false /* updateData */, gcu_FboTexUnit, true); // FIXME always update if changing tex-unit
        }
    }

    public VBORegion2PMSAAES2(final int renderModes, final TextureSequence colorTexSeq, final int pass2TexUnit) {
        super(renderModes, colorTexSeq);

        rsLocal = new RenderState.ProgramLocal();

        final int initialElementCount = 256;

        // Pass 1:
        indicesBuffer = GLArrayDataServer.createData(3, GL.GL_SHORT, initialElementCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);

        gca_VerticesAttr = GLArrayDataServer.createGLSL(AttributeNames.VERTEX_ATTR_NAME, 3, GL.GL_FLOAT,
                                                      false, initialElementCount, GL.GL_STATIC_DRAW);

        gca_CurveParamsAttr = GLArrayDataServer.createGLSL(AttributeNames.CURVEPARAMS_ATTR_NAME, 3, GL.GL_FLOAT,
                                                       false, initialElementCount, GL.GL_STATIC_DRAW);

        if( hasColorChannel() ) {
            gca_ColorsAttr = GLArrayDataServer.createGLSL(AttributeNames.COLOR_ATTR_NAME, 4, GL.GL_FLOAT,
                                                          false, initialElementCount, GL.GL_STATIC_DRAW);
        } else {
            gca_ColorsAttr = null;
        }
        if( hasColorTexture() ) {
            gcu_ColorTexUnit = new GLUniformData(UniformNames.gcu_ColorTexUnit, colorTexSeq.getTextureUnit());
            colorTexBBox = new float[4];
            gcu_ColorTexBBox = new GLUniformData(UniformNames.gcu_ColorTexBBox, 4, FloatBuffer.wrap(colorTexBBox));
        } else {
            gcu_ColorTexUnit = null;
            colorTexBBox = null;
            gcu_ColorTexBBox = null;
        }

        FloatUtil.makeIdentity(pmvMatrix02, 0);
        FloatUtil.makeIdentity(pmvMatrix02, 16);
        gcu_PMVMatrix02 = new GLUniformData(UniformNames.gcu_PMVMatrix02, 4, 4, FloatBuffer.wrap(pmvMatrix02));

        // Pass 2:
        gcu_FboTexUnit = new GLUniformData(UniformNames.gcu_FboTexUnit, pass2TexUnit);

        indicesFbo = GLArrayDataServer.createData(3, GL.GL_SHORT, 2, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
        indicesFbo.puts((short) 0); indicesFbo.puts((short) 1); indicesFbo.puts((short) 3);
        indicesFbo.puts((short) 1); indicesFbo.puts((short) 2); indicesFbo.puts((short) 3);
        indicesFbo.seal(true);

        gca_FboTexCoordsAttr = GLArrayDataServer.createGLSL(AttributeNames.FBO_TEXCOORDS_ATTR_NAME, 2, GL.GL_FLOAT,
                                                           false, 4, GL.GL_STATIC_DRAW);
        gca_FboTexCoordsAttr.putf(0); gca_FboTexCoordsAttr.putf(0);
        gca_FboTexCoordsAttr.putf(0); gca_FboTexCoordsAttr.putf(1);
        gca_FboTexCoordsAttr.putf(1); gca_FboTexCoordsAttr.putf(1);
        gca_FboTexCoordsAttr.putf(1); gca_FboTexCoordsAttr.putf(0);
        gca_FboTexCoordsAttr.seal(true);

        gca_FboVerticesAttr = GLArrayDataServer.createGLSL(AttributeNames.FBO_VERTEX_ATTR_NAME, 3, GL.GL_FLOAT,
                                                           false, 4, GL.GL_STATIC_DRAW);
    }

    @Override
    protected final void clearImpl(final GL2ES2 gl) {
        if( null != indicesBuffer ) {
            indicesBuffer.seal(gl, false);
            indicesBuffer.rewind();
        }
        if( null != gca_VerticesAttr ) {
            gca_VerticesAttr.seal(gl, false);
            gca_VerticesAttr.rewind();
        }
        if( null != gca_CurveParamsAttr ) {
            gca_CurveParamsAttr.seal(gl, false);
            gca_CurveParamsAttr.rewind();
        }
        if( null != gca_ColorsAttr ) {
            gca_ColorsAttr.seal(gl, false);
            gca_ColorsAttr.rewind();
        }
        fboDirty = true;
    }

    @Override
    protected final void pushVertex(final float[] coords, final float[] texParams, final float[] rgba) {
        gca_VerticesAttr.putf(coords[0]);
        gca_VerticesAttr.putf(coords[1]);
        gca_VerticesAttr.putf(coords[2]);

        gca_CurveParamsAttr.putf(texParams[0]);
        gca_CurveParamsAttr.putf(texParams[1]);
        gca_CurveParamsAttr.putf(texParams[2]);

        if( null != gca_ColorsAttr ) {
            if( null != rgba ) {
                gca_ColorsAttr.putf(rgba[0]);
                gca_ColorsAttr.putf(rgba[1]);
                gca_ColorsAttr.putf(rgba[2]);
                gca_ColorsAttr.putf(rgba[3]);
            } else {
                throw new IllegalArgumentException("Null color given for COLOR_CHANNEL rendering mode");
            }
        }
    }

    @Override
    protected final void pushIndex(final int idx) {
        indicesBuffer.puts((short)idx);
    }

    @Override
    protected void updateImpl(final GL2ES2 gl) {
        // seal buffers
        indicesBuffer.seal(gl, true);
        indicesBuffer.enableBuffer(gl, false);
        gca_CurveParamsAttr.seal(gl, true);
        gca_CurveParamsAttr.enableBuffer(gl, false);
        gca_VerticesAttr.seal(gl, true);
        gca_VerticesAttr.enableBuffer(gl, false);
        if( null != gca_ColorsAttr ) {
            gca_ColorsAttr.seal(gl, true);
            gca_ColorsAttr.enableBuffer(gl, false);
        }
        if( null != gcu_ColorTexUnit && colorTexSeq.isTextureAvailable() ) {
            final TextureSequence.TextureFrame frame = colorTexSeq.getLastTexture();
            final Texture tex = frame.getTexture();
            final TextureCoords tc = tex.getImageTexCoords();
            final float tcSx = 1f / ( tc.right() - tc.left() );
            colorTexBBox[0] = box.getMinX() * tcSx;
            colorTexBBox[2] = box.getMaxX() * tcSx;
            if( tex.getMustFlipVertically() ) {
                final float tcSy = 1f / ( tc.bottom() - tc.top() );
                colorTexBBox[1] = box.getMaxY() * tcSy;
                colorTexBBox[3] = box.getMinY() * tcSy;
            } else {
                final float tcSy = 1f / ( tc.top() - tc.bottom() );
                colorTexBBox[1] = box.getMinY() * tcSy;
                colorTexBBox[3] = box.getMaxY() * tcSy;
            }
        }
        gca_FboVerticesAttr.seal(gl, false);
        {
            final FloatBuffer fb = (FloatBuffer)gca_FboVerticesAttr.getBuffer();
            fb.put( 2, box.getMinZ());
            fb.put( 5, box.getMinZ());
            fb.put( 8, box.getMinZ());
            fb.put(11, box.getMinZ());
        }
        // Pending gca_FboVerticesAttr-seal and fboPMVMatrix-setup, follow fboDirty

        // push data 2 GPU ..
        indicesFbo.seal(gl, true);
        indicesFbo.enableBuffer(gl, false);

        fboDirty = true;
        // the buffers were disabled, since due to real/fbo switching and other vbo usage
    }

    private final AABBox drawWinBox = new AABBox();
    private final int[] drawView = new int[] { 0, 0, 0, 0 };
    private final float[] drawVec4Tmp0 = new float[4];
    private final float[] drawVec4Tmp1 = new float[4];
    private final float[] drawVec4Tmp2 = new float[4];
    private final float[] drawMat4PMv = new float[16];

    private static final int border = 2; // surrounding border, i.e. width += 2*border, height +=2*border

    @Override
    protected void drawImpl(final GL2ES2 gl, final RegionRenderer renderer, final int[/*1*/] sampleCount) {
        if( 0 >= indicesBuffer.getElementCount() ) {
            if(DEBUG_INSTANCE) {
                System.err.printf("VBORegion2PMSAAES2.drawImpl: Empty%n");
            }
            return; // empty!
        }
        if( Float.isInfinite(box.getWidth()) || Float.isInfinite(box.getHeight()) ) {
            if(DEBUG_INSTANCE) {
                System.err.printf("VBORegion2PMSAAES2.drawImpl: Inf %s%n", box);
            }
            return; // inf
        }
        final int vpWidth = renderer.getWidth();
        final int vpHeight = renderer.getHeight();
        if(vpWidth <=0 || vpHeight <= 0 || null==sampleCount || sampleCount[0] <= 0){
            renderRegion(gl);
        } else {
            if(0 > maxTexSize[0]) {
                gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, maxTexSize, 0);
            }
            final RenderState rs = renderer.getRenderState();
            final float winWidth, winHeight;

            final float ratioObjWinWidth, ratioObjWinHeight;
            final float diffObjWidth, diffObjHeight;
            final float diffObjBorderWidth, diffObjBorderHeight;
            int targetFboWidth, targetFboHeight;
            {
                final float diffWinWidth, diffWinHeight;
                final int targetWinWidth, targetWinHeight;

                // Calculate perspective pixel width/height for FBO,
                // considering the sampleCount.
                drawView[2] = vpWidth;
                drawView[3] = vpHeight;

                renderer.getMatrix().multPMvMatrixf(drawMat4PMv, 0);
                box.mapToWindow(drawWinBox, drawMat4PMv, drawView, true /* useCenterZ */,
                                drawVec4Tmp0, drawVec4Tmp1, drawVec4Tmp2);

                winWidth = drawWinBox.getWidth();
                winHeight = drawWinBox.getHeight();
                targetWinWidth = (int)Math.ceil(winWidth);
                targetWinHeight = (int)Math.ceil(winHeight);
                diffWinWidth = targetWinWidth-winWidth;
                diffWinHeight = targetWinHeight-winHeight;

                ratioObjWinWidth = box.getWidth() / winWidth;
                ratioObjWinHeight= box.getHeight() / winHeight;
                diffObjWidth = diffWinWidth * ratioObjWinWidth;
                diffObjHeight = diffWinHeight * ratioObjWinHeight;
                diffObjBorderWidth = border * ratioObjWinWidth;
                diffObjBorderHeight = border * ratioObjWinHeight;

                targetFboWidth = targetWinWidth+2*border;
                targetFboHeight = targetWinHeight+2*border;

                if( DEBUG_FBO_2 ) {
                    final float ratioWinWidth, ratioWinHeight;
                    ratioWinWidth = winWidth/targetWinWidth;
                    ratioWinHeight = winHeight/targetWinHeight;

                    System.err.printf("XXX.MinMax obj %s%n", box.toString());
                    System.err.printf("XXX.MinMax obj d[%.3f, %.3f], r[%f, %f], b[%f, %f]%n",
                            diffObjWidth, diffObjHeight, ratioObjWinWidth, ratioObjWinWidth, diffObjBorderWidth, diffObjBorderHeight);
                    System.err.printf("XXX.MinMax win %s%n", drawWinBox.toString());
                    System.err.printf("XXX.MinMax view[%d, %d] -> win[%.3f, %.3f], i[%d x %d], d[%.3f, %.3f], r[%f, %f]: FBO i[%d x %d], samples %d%n",
                            drawView[2], drawView[3],
                            winWidth, winHeight, targetWinWidth, targetWinHeight, diffWinWidth,
                            diffWinHeight, ratioWinWidth, ratioWinHeight,
                            targetFboWidth, targetFboHeight,
                            sampleCount[0]);
                }
            }
            if( 0 >= targetFboWidth || 0 >= targetFboHeight ) {
                // Nothing ..
                return;
            }
            final int deltaFboWidth = Math.abs(targetFboWidth-fboWidth);
            final int deltaFboHeight = Math.abs(targetFboHeight-fboHeight);
            final boolean hasDelta = 0!=deltaFboWidth || 0!=deltaFboHeight;
            if( DEBUG_FBO_2 ) {
                System.err.printf("XXX.maxDelta: hasDelta %b: %d / %d,  %.3f, %.3f%n",
                        hasDelta, deltaFboWidth, deltaFboHeight, (float)deltaFboWidth/fboWidth, (float)deltaFboHeight/fboHeight);
                System.err.printf("XXX.Scale %d * [%f x %f]: %d x %d%n",
                        sampleCount[0], winWidth, winHeight, targetFboWidth, targetFboHeight);
            }
            if( hasDelta || fboDirty || isShapeDirty() || null == fbo || fbo.getNumSamples() != sampleCount[0] ) {
                // FIXME: rescale
                final float minX = box.getMinX()-diffObjBorderWidth;
                final float minY = box.getMinY()-diffObjBorderHeight;
                final float maxX = box.getMaxX()+diffObjBorderWidth+diffObjWidth;
                final float maxY = box.getMaxY()+diffObjBorderHeight+diffObjHeight;
                gca_FboVerticesAttr.seal(false);
                {
                    final FloatBuffer fb = (FloatBuffer)gca_FboVerticesAttr.getBuffer();
                    fb.put(0, minX); fb.put( 1, minY);
                    fb.put(3, minX); fb.put( 4, maxY);
                    fb.put(6, maxX); fb.put( 7, maxY);
                    fb.put(9, maxX); fb.put(10, minY);
                    fb.position(12);
                }
                gca_FboVerticesAttr.seal(true);
                FloatUtil.makeOrtho(pmvMatrix02, 0, true, minX, maxX, minY, maxY, -1, 1);
                useShaderProgram(gl, renderer, getRenderModes(), true, getQuality(), sampleCount[0]);
                renderRegion2FBO(gl, rs, targetFboWidth, targetFboHeight, vpWidth, vpHeight, sampleCount);
            } else if( isStateDirty() ) {
                useShaderProgram(gl, renderer, getRenderModes(), true, getQuality(), sampleCount[0]);
                renderRegion2FBO(gl, rs, targetFboWidth, targetFboHeight, vpWidth, vpHeight, sampleCount);
            }
            useShaderProgram(gl, renderer, getRenderModes(), false, getQuality(), sampleCount[0]);
            renderFBO(gl, rs, vpWidth, vpHeight, sampleCount[0]);
        }
    }

    private void renderFBO(final GL2ES2 gl, final RenderState rs, final int width, final int height, final int sampleCount) {
        gl.glViewport(0, 0, width, height);

        if( rs.isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED | RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED) ) {
            // RGB is already multiplied w/ alpha via renderRegion2FBO(..)
            gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
            gl.glEnable(GL.GL_DEPTH_TEST);

        } else if( rs.isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED) ) {
            // RGB is already multiplied w/ alpha via renderRegion2FBO(..)
            gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
        }

        gl.glActiveTexture(GL.GL_TEXTURE0 + gcu_FboTexUnit.intValue());

        fbo.use(gl, fbo.getSamplingSink().getTextureAttachment());
        gca_FboVerticesAttr.enableBuffer(gl, true);
        gca_FboTexCoordsAttr.enableBuffer(gl, true);
        indicesFbo.bindBuffer(gl, true); // keeps VBO binding

        gl.glDrawElements(GL.GL_TRIANGLES, indicesFbo.getElementCount() * indicesFbo.getComponentCount(), GL.GL_UNSIGNED_SHORT, 0);

        indicesFbo.bindBuffer(gl, false);
        gca_FboTexCoordsAttr.enableBuffer(gl, false);
        gca_FboVerticesAttr.enableBuffer(gl, false);
        fbo.unuse(gl);

        // setback: gl.glActiveTexture(currentActiveTextureEngine[0]);
    }

    private void renderRegion2FBO(final GL2ES2 gl, final RenderState rs, final int targetFboWidth, final int targetFboHeight,
                                  final int vpWidth, final int vpHeight, final int[] sampleCount) {
        if( 0 >= targetFboWidth || 0 >= targetFboHeight ) {
            throw new IllegalArgumentException("fboSize must be greater than 0: "+targetFboWidth+"x"+targetFboHeight);
        }

        final boolean blendingEnabled = rs.isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED);

        if(null == fbo) {
            fboWidth  = targetFboWidth;
            fboHeight  = targetFboHeight;
            fbo = new FBObject();
            fbo.init(gl, fboWidth, fboHeight, sampleCount[0]);
            sampleCount[0] = fbo.getNumSamples();
            fbo.attachColorbuffer(gl, 0, true);
            if( !blendingEnabled ) {
                // no depth-buffer w/ blending
                fbo.attachRenderbuffer(gl, Attachment.Type.DEPTH, FBObject.DEFAULT_BITS);
            }
            final FBObject ssink = new FBObject();
            {
                ssink.init(gl, fboWidth, fboHeight, 0);
                // FIXME: shall not use bilinear (GL_LINEAR), due to MSAA ???
                // ssink.attachTexture2D(gl, 0, true, GL2ES2.GL_LINEAR, GL2ES2.GL_LINEAR, GL2ES2.GL_CLAMP_TO_EDGE, GL2ES2.GL_CLAMP_TO_EDGE);
                ssink.attachTexture2D(gl, 0, true, GL.GL_NEAREST, GL.GL_NEAREST, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
                if( !blendingEnabled ) {
                    // no depth-buffer w/ blending
                    ssink.attachRenderbuffer(gl, Attachment.Type.DEPTH, FBObject.DEFAULT_BITS);
                }
            }
            fbo.setSamplingSink(ssink);
            fbo.resetSamplingSink(gl); // validate
            if( DEBUG_FBO_1 ) {
                System.err.printf("XXX.createFBO: blending %b, %dx%d%n%s%n", blendingEnabled, fboWidth, fboHeight, fbo.toString());
            }
        } else if( targetFboWidth != fboWidth || targetFboHeight != fboHeight || fbo.getNumSamples() != sampleCount[0] ) {
            fbo.reset(gl, targetFboWidth, targetFboHeight, sampleCount[0]);
            sampleCount[0] = fbo.getNumSamples();
            if( DEBUG_FBO_1 ) {
                System.err.printf("XXX.resetFBO: %dx%d -> %dx%d%n%s%n", fboWidth, fboHeight, targetFboWidth, targetFboHeight, fbo );
            }
            fboWidth  = targetFboWidth;
            fboHeight  = targetFboHeight;
        }
        fbo.bind(gl);

        //render texture
        gl.glViewport(0, 0, fboWidth, fboHeight);
        if( blendingEnabled ) {
            gl.glClearColor(0f, 0f, 0f, 0.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT); // no depth-buffer w/ blending
            // For already pre-multiplied alpha values, use:
            // gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);

            // Multiply RGB w/ Alpha, preserve alpha for renderFBO(..)
            gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);

            if( rs.isHintMaskSet(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED) ) {
                gl.glDisable(GL.GL_DEPTH_TEST);
            }
        } else {
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }

        renderRegion(gl);

        fbo.unbind(gl);
        fboDirty = false;
    }

    private void renderRegion(final GL2ES2 gl) {
        gl.glUniform(gcu_PMVMatrix02);
        gca_VerticesAttr.enableBuffer(gl, true);
        gca_CurveParamsAttr.enableBuffer(gl, true);
        if( null != gca_ColorsAttr ) {
            gca_ColorsAttr.enableBuffer(gl, true);
        }
        indicesBuffer.bindBuffer(gl, true); // keeps VBO binding
        if( null != gcu_ColorTexUnit && colorTexSeq.isTextureAvailable() ) {
            final TextureSequence.TextureFrame frame = colorTexSeq.getNextTexture(gl);
            gl.glActiveTexture(GL.GL_TEXTURE0 + colorTexSeq.getTextureUnit());
            final Texture tex = frame.getTexture();
            tex.bind(gl);
            tex.enable(gl); // nop on core
            gcu_ColorTexUnit.setData(colorTexSeq.getTextureUnit());
            gl.glUniform(gcu_ColorTexUnit); // Always update, since program maybe used by multiple regions
            gl.glUniform(gcu_ColorTexBBox); // Always update, since program maybe used by multiple regions
            gl.glDrawElements(GL.GL_TRIANGLES, indicesBuffer.getElementCount() * indicesBuffer.getComponentCount(), GL.GL_UNSIGNED_SHORT, 0);
            tex.disable(gl); // nop on core
        } else {
            gl.glDrawElements(GL.GL_TRIANGLES, indicesBuffer.getElementCount() * indicesBuffer.getComponentCount(), GL.GL_UNSIGNED_SHORT, 0);
        }

        indicesBuffer.bindBuffer(gl, false);
        if( null != gca_ColorsAttr ) {
            gca_ColorsAttr.enableBuffer(gl, false);
        }
        gca_CurveParamsAttr.enableBuffer(gl, false);
        gca_VerticesAttr.enableBuffer(gl, false);
    }

    @Override
    protected void destroyImpl(final GL2ES2 gl) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegion2PES2 Destroy: " + this);
        }
        if(null != fbo) {
            fbo.destroy(gl);
            fbo = null;
        }
        if(null != gca_VerticesAttr) {
            gca_VerticesAttr.destroy(gl);
            gca_VerticesAttr = null;
        }
        if(null != gca_CurveParamsAttr) {
            gca_CurveParamsAttr.destroy(gl);
            gca_CurveParamsAttr = null;
        }
        if(null != gca_ColorsAttr) {
            gca_ColorsAttr.destroy(gl);
            gca_ColorsAttr = null;
        }
        if(null != indicesBuffer) {
            indicesBuffer.destroy(gl);
            indicesBuffer = null;
        }
        if(null != gca_FboVerticesAttr) {
            gca_FboVerticesAttr.destroy(gl);
            gca_FboVerticesAttr = null;
        }
        if(null != gca_FboTexCoordsAttr) {
            gca_FboTexCoordsAttr.destroy(gl);
            gca_FboTexCoordsAttr = null;
        }
        if(null != indicesFbo) {
            indicesFbo.destroy(gl);
            indicesFbo = null;
        }
        spPass1 = null;
        spPass2 = null;
    }
}
