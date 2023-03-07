/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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

import java.io.PrintStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLUniformData;

import jogamp.graph.curve.opengl.shader.AttributeNames;
import jogamp.graph.curve.opengl.shader.UniformNames;
import jogamp.opengl.Debug;

import com.jogamp.common.util.PropertyAccess;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.GLArrayDataClient;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureSequence;

public class VBORegion2PVBAAES2  extends GLRegion {
    private static final boolean DEBUG_FBO_1 = false;
    private static final boolean DEBUG_FBO_2 = false;

    /**
     * Boundary triggering FBO resize if
     * <pre>
     *      fbo[Width|Height] - targetFbo[Width|Height] > RESIZE_BOUNDARY.
     * </pre>
     * <p>
     * Increasing the FBO will add RESIZE_BOUNDARY/2.
     * </p>
     * <p>
     * Reducing FBO resize to gain performance.
     * </p>
     * <p>
     * Defaults to disabled since:
     *   - not working properly
     *   - FBO texture rendered > than desired size
     *   - FBO resize itself should be fast enough ?!
     * </p>
     */
    private static final int RESIZE_BOUNDARY;

    static {
        Debug.initSingleton();
        final String key = "jogl.debug.graph.curve.vbaa.resizeLowerBoundary";
        RESIZE_BOUNDARY = Math.max(0, PropertyAccess.getIntProperty(key, true, 0));
        if( RESIZE_BOUNDARY > 0 ) {
            System.err.println("key: "+RESIZE_BOUNDARY);
        }
    }


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
    private final GLUniformData gcu_FboTexSize;
    private final float[] pmvMatrix02 = new float[2*16]; // P + Mv
    private final GLUniformData gcu_PMVMatrix02;
    private ShaderProgram spPass2 = null;

    private FBObject fbo;
    private TextureAttachment texA;

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
            rs.updateUniformLoc(gl, updateLocLocal, gcu_FboTexSize, sampleCount > 1); // maybe optimized away for sampleCount <= 1
        }
    }

    // private static final float growthFactor = 1.2f; // avg +5% size but 15% more overhead (34% total)
    private static final float growthFactor = GLArrayDataClient.DEFAULT_GROWTH_FACTOR; // avg +20% size, but 15% less CPU overhead compared to 1.2 (19% total)

    public VBORegion2PVBAAES2(final GLProfile glp, final int renderModes, final TextureSequence colorTexSeq, final int pass2TexUnit,
                              final int initialVerticesCount, final int initialIndicesCount)
    {
        super(glp, renderModes, colorTexSeq);

        rsLocal = new RenderState.ProgramLocal();

        // Pass 1:
        growBufferSize(initialVerticesCount, initialIndicesCount);

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
        gcu_FboTexSize = new GLUniformData(UniformNames.gcu_FboTexSize, 2, FloatBuffer.wrap(new float[2]));

        indicesFbo = GLArrayDataServer.createData(3, GL.GL_UNSIGNED_SHORT, 2, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
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
    public void growBufferSize(final int verticeCount, final int indexCount) {
        if(null == indicesBuffer ) {
            indicesBuffer = GLArrayDataServer.createData(3, glIdxType(), indexCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
            indicesBuffer.setGrowthFactor(growthFactor);
        } else {
            indicesBuffer.growIfNeeded(indexCount * indicesBuffer.getCompsPerElem());
        }
        if( null == gca_VerticesAttr ) {
            gca_VerticesAttr = GLArrayDataServer.createGLSL(AttributeNames.VERTEX_ATTR_NAME, 3, GL.GL_FLOAT,
                                                            false, verticeCount, GL.GL_STATIC_DRAW);
            gca_VerticesAttr.setGrowthFactor(growthFactor);
        } else {
            gca_VerticesAttr.growIfNeeded(verticeCount * gca_VerticesAttr.getCompsPerElem());
        }
        if( null == gca_CurveParamsAttr ) {
            gca_CurveParamsAttr = GLArrayDataServer.createGLSL(AttributeNames.CURVEPARAMS_ATTR_NAME, 3, GL.GL_FLOAT,
                                                               false, verticeCount, GL.GL_STATIC_DRAW);
            gca_CurveParamsAttr.setGrowthFactor(growthFactor);
        } else {
            gca_CurveParamsAttr.growIfNeeded(verticeCount * gca_CurveParamsAttr.getCompsPerElem());
        }

        if( null == gca_ColorsAttr ) {
            if( hasColorChannel() ) {
                gca_ColorsAttr = GLArrayDataServer.createGLSL(AttributeNames.COLOR_ATTR_NAME, 4, GL.GL_FLOAT,
                                                              false, verticeCount, GL.GL_STATIC_DRAW);
                gca_ColorsAttr.setGrowthFactor(growthFactor);
            }
        } else {
            gca_ColorsAttr.growIfNeeded(verticeCount * gca_ColorsAttr.getCompsPerElem());
        }
    }

    @Override
    protected final void clearImpl(final GL2ES2 gl) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegion2PES2 Clear: " + this);
            // Thread.dumpStack();
        }
        if( null != indicesBuffer ) {
            indicesBuffer.clear(gl);
        }
        if( null != gca_VerticesAttr ) {
            gca_VerticesAttr.clear(gl);
        }
        if( null != gca_CurveParamsAttr ) {
            gca_CurveParamsAttr.clear(gl);
        }
        if( null != gca_ColorsAttr ) {
            gca_ColorsAttr.clear(gl);;
        }
        fboDirty = true;
    }

    @Override
    public void printBufferStats(final PrintStream out) {
        final int[] size= { 0 }, capacity= { 0 };
        out.println("VBORegion2PVBAAES2: idx32 "+usesI32Idx());
        printAndCount(out, "  indices ", indicesBuffer, size, capacity);
        out.println();
        printAndCount(out, "  vertices ", gca_VerticesAttr, size, capacity);
        out.println();
        printAndCount(out, "  params ", gca_CurveParamsAttr, size, capacity);
        out.println();
        printAndCount(out, "  color ", gca_ColorsAttr, size, capacity);
        final float filled = (float)size[0]/(float)capacity[0];
        out.println();
                out.printf("  total [bytes %,d / %,d], filled %.1f%%, left %.1f%%]%n",
                        size[0], capacity[0], filled*100f, (1f-filled)*100f);
    }

    @Override
    protected final void pushVertex(final float[] coords, final float[] texParams, final float[] rgba) {
        // NIO array[3] is much slows than group/single
        // gca_VerticesAttr.putf(coords, 0, 3);
        // gca_CurveParamsAttr.putf(texParams, 0, 3);
        // gca_VerticesAttr.put3f(coords[0], coords[1], coords[2]);
        put3f((FloatBuffer)gca_VerticesAttr.getBuffer(), coords[0], coords[1], coords[2]);
        // gca_CurveParamsAttr.put3f(texParams[0], texParams[1], texParams[2]);
        put3f((FloatBuffer)gca_CurveParamsAttr.getBuffer(), texParams[0], texParams[1], texParams[2]);
        if( null != gca_ColorsAttr ) {
            if( null != rgba ) {
                // gca_ColorsAttr.putf(rgba, 0, 4);
                // gca_ColorsAttr.put4f(rgba[0], rgba[1], rgba[2], rgba[3]);
                put4f((FloatBuffer)gca_ColorsAttr.getBuffer(), rgba[0], rgba[1], rgba[2], rgba[3]);
            } else {
                throw new IllegalArgumentException("Null color given for COLOR_CHANNEL rendering mode");
            }
        }
    }

    @Override
    protected final void pushVertices(final float[] coords1, final float[] coords2, final float[] coords3,
                                      final float[] texParams1, final float[] texParams2, final float[] texParams3, final float[] rgba) {
        // gca_VerticesAttr.put3f(coords1[0], coords1[1], coords1[2]);
        // gca_VerticesAttr.put3f(coords2[0], coords2[1], coords2[2]);
        // gca_VerticesAttr.put3f(coords3[0], coords3[1], coords3[2]);
        put3f((FloatBuffer)gca_VerticesAttr.getBuffer(), coords1[0], coords1[1], coords1[2]);
        put3f((FloatBuffer)gca_VerticesAttr.getBuffer(), coords2[0], coords2[1], coords2[2]);
        put3f((FloatBuffer)gca_VerticesAttr.getBuffer(), coords3[0], coords3[1], coords3[2]);
        // gca_CurveParamsAttr.put3f(texParams1[0], texParams1[1], texParams1[2]);
        // gca_CurveParamsAttr.put3f(texParams2[0], texParams2[1], texParams2[2]);
        // gca_CurveParamsAttr.put3f(texParams3[0], texParams3[1], texParams3[2]);
        put3f((FloatBuffer)gca_CurveParamsAttr.getBuffer(), texParams1[0], texParams1[1], texParams1[2]);
        put3f((FloatBuffer)gca_CurveParamsAttr.getBuffer(), texParams2[0], texParams2[1], texParams2[2]);
        put3f((FloatBuffer)gca_CurveParamsAttr.getBuffer(), texParams3[0], texParams3[1], texParams3[2]);
        if( null != gca_ColorsAttr ) {
            if( null != rgba ) {
                final float r=rgba[0], g=rgba[1], b=rgba[2], a=rgba[3];
                // gca_ColorsAttr.put4f(r, g, b, a);
                // gca_ColorsAttr.put4f(r, g, b, a);
                // gca_ColorsAttr.put4f(r, g, b, a);
                put4f((FloatBuffer)gca_ColorsAttr.getBuffer(), r, g, b, a);
                put4f((FloatBuffer)gca_ColorsAttr.getBuffer(), r, g, b, a);
                put4f((FloatBuffer)gca_ColorsAttr.getBuffer(), r, g, b, a);
            } else {
                throw new IllegalArgumentException("Null color given for COLOR_CHANNEL rendering mode");
            }
        }
    }

    @Override
    protected final void pushIndex(final int idx) {
        if( usesI32Idx() ) {
            indicesBuffer.puti(idx);
        } else {
            indicesBuffer.puts((short)idx);
        }
    }

    @Override
    protected final void pushIndices(final int idx1, final int idx2, final int idx3) {
        if( usesI32Idx() ) {
            // indicesBuffer.put3i(idx1, idx2, idx3);
            put3i((IntBuffer)indicesBuffer.getBuffer(), idx1, idx2, idx3);
        } else {
            // indicesBuffer.put3s((short)idx1, (short)idx2, (short)idx3);
            put3s((ShortBuffer)indicesBuffer.getBuffer(), (short)idx1, (short)idx2, (short)idx3);
        }
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
        if( 0 >= indicesBuffer.getElemCount() ) {
            if(DEBUG_INSTANCE) {
                System.err.printf("VBORegion2PVBAAES2.drawImpl: Empty%n");
            }
            return; // empty!
        }
        if( Float.isInfinite(box.getWidth()) || Float.isInfinite(box.getHeight()) ) {
            if(DEBUG_INSTANCE) {
                System.err.printf("VBORegion2PVBAAES2.drawImpl: Inf %s%n", box);
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

                targetFboWidth = (targetWinWidth+2*border)*sampleCount[0];
                targetFboHeight = (targetWinHeight+2*border)*sampleCount[0];

                if( DEBUG_FBO_2 ) {
                    final float ratioWinWidth, ratioWinHeight;
                    ratioWinWidth = winWidth/targetWinWidth;
                    ratioWinHeight = winHeight/targetWinHeight;
                    final float renderFboWidth, renderFboHeight;
                    renderFboWidth = (winWidth+2*border)*sampleCount[0];
                    renderFboHeight = (winHeight+2*border)*sampleCount[0];
                    final float ratioFboWidth, ratioFboHeight;
                    ratioFboWidth = renderFboWidth/targetFboWidth;
                    ratioFboHeight = renderFboHeight/targetFboHeight;
                    final float diffFboWidth, diffFboHeight;
                    diffFboWidth = targetFboWidth-renderFboWidth;
                    diffFboHeight = targetFboHeight-renderFboHeight;

                    System.err.printf("XXX.MinMax obj %s%n", box.toString());
                    System.err.printf("XXX.MinMax obj d[%.3f, %.3f], r[%f, %f], b[%f, %f]%n",
                            diffObjWidth, diffObjHeight, ratioObjWinWidth, ratioObjWinWidth, diffObjBorderWidth, diffObjBorderHeight);
                    System.err.printf("XXX.MinMax win %s%n", drawWinBox.toString());
                    System.err.printf("XXX.MinMax view[%d, %d] -> win[%.3f, %.3f], i[%d x %d], d[%.3f, %.3f], r[%f, %f]: FBO f[%.3f, %.3f], i[%d x %d], d[%.3f, %.3f], r[%f, %f], samples %d%n",
                            drawView[2], drawView[3],
                            winWidth, winHeight, targetWinWidth, targetWinHeight, diffWinWidth,
                            diffWinHeight, ratioWinWidth, ratioWinHeight,
                            renderFboWidth, renderFboHeight, targetFboWidth, targetFboHeight,
                            diffFboWidth, diffFboHeight, ratioFboWidth, ratioFboHeight,
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
            if( hasDelta || fboDirty || isShapeDirty() || null == fbo ) {
                final int maxLength = Math.max(targetFboWidth, targetFboHeight);
                if( maxLength > maxTexSize[0] ) {
                    if( targetFboWidth > targetFboHeight ) {
                        sampleCount[0] = (int)Math.floor(maxTexSize[0] / (winWidth+2*border));
                    } else {
                        sampleCount[0] = (int)Math.floor(maxTexSize[0] / (winHeight+2*border));
                    }
                    final float renderFboWidth, renderFboHeight;
                    renderFboWidth = (winWidth+2*border)*sampleCount[0];
                    renderFboHeight = (winWidth+2*border)*sampleCount[0];
                    targetFboWidth = (int)Math.ceil(renderFboWidth);
                    targetFboHeight = (int)Math.ceil(renderFboHeight);
                    if( DEBUG_FBO_1 ) {
                        System.err.printf("XXX.Rescale (MAX): win[%.3f, %.3f]: FBO f[%.3f, %.3f], i[%d x %d], msaa %d%n",
                                winWidth, winHeight,
                                renderFboWidth, renderFboHeight, targetFboWidth, targetFboHeight, sampleCount[0]);
                    }
                    if( sampleCount[0] <= 0 ) {
                        // Last way out!
                        renderRegion(gl);
                        return;
                    }
                }

                final int newFboWidth, newFboHeight, resizeCase;
                if( 0 >= RESIZE_BOUNDARY ) {
                    // Resize w/o optimization
                    newFboWidth = targetFboWidth;
                    newFboHeight = targetFboHeight;
                    resizeCase = 0;
                } else {
                    if( 0 >= fboWidth || 0 >= fboHeight || null == fbo ) {
                        // Case: New FBO
                        newFboWidth = targetFboWidth;
                        newFboHeight = targetFboHeight;
                        resizeCase = 1;
                    } else if( targetFboWidth > fboWidth || targetFboHeight > fboHeight ) {
                        // Case: Inscrease FBO Size, add boundary/2 if avail
                        newFboWidth = ( targetFboWidth + RESIZE_BOUNDARY/2 < maxTexSize[0] ) ? targetFboWidth + RESIZE_BOUNDARY/2 : targetFboWidth;
                        newFboHeight = ( targetFboHeight+ RESIZE_BOUNDARY/2 < maxTexSize[0] ) ? targetFboHeight + RESIZE_BOUNDARY/2 : targetFboHeight;
                        resizeCase = 2;
                    } else if( targetFboWidth < fboWidth && targetFboHeight < fboHeight &&
                               fboWidth - targetFboWidth < RESIZE_BOUNDARY &&
                               fboHeight - targetFboHeight < RESIZE_BOUNDARY ) {
                        // Case: Decreased FBO Size Request within boundary
                        newFboWidth = fboWidth;
                        newFboHeight = fboHeight;
                        resizeCase = 3;
                    } else {
                        // Case: Decreased-Size-Beyond-Boundary or No-Resize
                        newFboWidth = targetFboWidth;
                        newFboHeight = targetFboHeight;
                        resizeCase = 4;
                    }
                }
                final int dResizeWidth = newFboWidth - targetFboWidth;
                final int dResizeHeight = newFboHeight - targetFboHeight;
                final float diffObjResizeWidth = dResizeWidth*ratioObjWinWidth;
                final float diffObjResizeHeight = dResizeHeight*ratioObjWinHeight;
                if( DEBUG_FBO_1 ) {
                    System.err.printf("XXX.resizeFBO: case %d, has %dx%d > target %dx%d, resize: i[%d x %d], f[%.3f x %.3f] -> %dx%d%n",
                            resizeCase, fboWidth, fboHeight, targetFboWidth, targetFboHeight,
                            dResizeWidth, dResizeHeight, diffObjResizeWidth, diffObjResizeHeight,
                            newFboWidth, newFboHeight);
                }

                final float minX = box.getMinX()-diffObjBorderWidth;
                final float minY = box.getMinY()-diffObjBorderHeight;
                final float maxX = box.getMaxX()+diffObjBorderWidth+diffObjWidth+diffObjResizeWidth;
                final float maxY = box.getMaxY()+diffObjBorderHeight+diffObjHeight+diffObjResizeHeight;
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
                renderRegion2FBO(gl, rs, targetFboWidth, targetFboHeight, newFboWidth, newFboHeight, vpWidth, vpHeight, sampleCount[0]);
            } else if( isStateDirty() ) {
                useShaderProgram(gl, renderer, getRenderModes(), true, getQuality(), sampleCount[0]);
                renderRegion2FBO(gl, rs, targetFboWidth, targetFboHeight, fboWidth, fboHeight, vpWidth, vpHeight, sampleCount[0]);
            }
            useShaderProgram(gl, renderer, getRenderModes(), false, getQuality(), sampleCount[0]);
            renderFBO(gl, rs, targetFboWidth, targetFboHeight, vpWidth, vpHeight, sampleCount[0]);
        }
    }

    private void renderFBO(final GL2ES2 gl, final RenderState rs, final int targetFboWidth, final int targetFboHeight,
                           final int vpWidth, final int vpHeight, final int sampleCount) {
        gl.glViewport(0, 0, vpWidth, vpHeight);

        if( rs.isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED) ) {
            // RGB is already multiplied w/ alpha via renderRegion2FBO(..)
            gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
        }

        gl.glUniform(gcu_FboTexSize);

        gl.glActiveTexture(GL.GL_TEXTURE0 + gcu_FboTexUnit.intValue());

        fbo.use(gl, texA);
        gca_FboVerticesAttr.enableBuffer(gl, true);
        gca_FboTexCoordsAttr.enableBuffer(gl, true);
        indicesFbo.bindBuffer(gl, true); // keeps VBO binding

        gl.glDrawElements(GL.GL_TRIANGLES, indicesFbo.getElemCount() * indicesFbo.getCompsPerElem(), GL.GL_UNSIGNED_SHORT, 0);

        indicesFbo.bindBuffer(gl, false);
        gca_FboTexCoordsAttr.enableBuffer(gl, false);
        gca_FboVerticesAttr.enableBuffer(gl, false);
        fbo.unuse(gl);

        // setback: gl.glActiveTexture(currentActiveTextureEngine[0]);
    }

    private void renderRegion2FBO(final GL2ES2 gl, final RenderState rs,
                                  final int targetFboWidth, final int targetFboHeight, final int newFboWidth, final int newFboHeight,
                                  final int vpWidth, final int vpHeight, final int sampleCount) {
        if( 0 >= targetFboWidth || 0 >= targetFboHeight ) {
            throw new IllegalArgumentException("fboSize must be greater than 0: "+targetFboWidth+"x"+targetFboHeight);
        }

        final boolean blendingEnabled = rs.isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED);

        if(null == fbo) {
            fboWidth  = newFboWidth;
            fboHeight  = newFboHeight;
            final FloatBuffer fboTexSize = (FloatBuffer) gcu_FboTexSize.getBuffer();
            {
                fboTexSize.put(0, fboWidth);
                fboTexSize.put(1, fboHeight);
            }
            fbo = new FBObject();
            fbo.init(gl, fboWidth, fboHeight, 0);
            // Shall not use bilinear (GL_LINEAR), due to own VBAA. Result is smooth w/o it now!
            // FIXME: FXAA requires bilinear filtering!
            // texA = fbo.attachTexture2D(gl, 0, true, GL.GL_LINEAR, GL.GL_LINEAR, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
            texA = fbo.attachTexture2D(gl, 0, true, GL.GL_NEAREST, GL.GL_NEAREST, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
            if( !blendingEnabled ) {
                // no depth-buffer w/ blending
                fbo.attachRenderbuffer(gl, Attachment.Type.DEPTH, FBObject.DEFAULT_BITS);
            }
            if( DEBUG_FBO_1 ) {
                System.err.printf("XXX.createFBO: %dx%d%n%s%n", fboWidth, fboHeight, fbo.toString());
            }
        } else if( newFboWidth != fboWidth || newFboHeight != fboHeight ) {
            fbo.reset(gl, newFboWidth, newFboHeight, 0);
            fbo.bind(gl);
            if( DEBUG_FBO_1 ) {
                System.err.printf("XXX.resetFBO: %dx%d -> %dx%d, target %dx%d%n", fboWidth, fboHeight, newFboWidth, newFboHeight, targetFboWidth, targetFboHeight);
            }
            fboWidth  = newFboWidth;
            fboHeight  = newFboHeight;
            final FloatBuffer fboTexSize = (FloatBuffer) gcu_FboTexSize.getBuffer();
            {
                fboTexSize.put(0, fboWidth);
                fboTexSize.put(1, fboHeight);
            }
        } else {
            fbo.bind(gl);
        }

        //render texture
        gl.glViewport(0, 0, fboWidth, fboHeight);
        if( blendingEnabled ) {
            gl.glClearColor(0f, 0f, 0f, 0.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT); // no depth-buffer w/ blending
            // For already pre-multiplied alpha values, use:
            // gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);

            // Multiply RGB w/ Alpha, preserve alpha for renderFBO(..)
            gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
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
            gl.glDrawElements(GL.GL_TRIANGLES, indicesBuffer.getElemCount() * indicesBuffer.getCompsPerElem(), glIdxType(), 0);
            tex.disable(gl); // nop on core
        } else {
            gl.glDrawElements(GL.GL_TRIANGLES, indicesBuffer.getElemCount() * indicesBuffer.getCompsPerElem(), glIdxType(), 0);
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
            // Thread.dumpStack();
        }
        if(null != fbo) {
            fbo.destroy(gl);
            fbo = null;
            texA = null;
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
