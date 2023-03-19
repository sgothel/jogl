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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;

import jogamp.graph.curve.opengl.shader.AttributeNames;
import jogamp.graph.curve.opengl.shader.UniformNames;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureSequence;

public final class VBORegionSPES2 extends GLRegion {
    private final RenderState.ProgramLocal rsLocal;

    private int curVerticesCap = 0;
    private int curIndicesCap = 0;
    private GLArrayDataServer gca_VerticesAttr = null;
    private GLArrayDataServer gca_CurveParamsAttr = null;
    private GLArrayDataServer gca_ColorsAttr;
    private GLArrayDataServer indicesBuffer = null;
    private final GLUniformData gcu_ColorTexUnit;
    private final float[] colorTexBBox; // x0, y0, x1, y1
    private final GLUniformData gcu_ColorTexBBox;
    private ShaderProgram spPass1 = null;

    public VBORegionSPES2(final GLProfile glp, final int renderModes, final TextureSequence colorTexSeq,
                          final int initialVerticesCount, final int initialIndicesCount)
    {
        super(glp, renderModes, colorTexSeq);

        rsLocal = new RenderState.ProgramLocal();

        initBuffer(initialVerticesCount, initialIndicesCount);

        if( hasColorTexture() ) {
            gcu_ColorTexUnit = new GLUniformData(UniformNames.gcu_ColorTexUnit, colorTexSeq.getTextureUnit());
            colorTexBBox = new float[4];
            gcu_ColorTexBBox = new GLUniformData(UniformNames.gcu_ColorTexBBox, 4, FloatBuffer.wrap(colorTexBBox));
        } else {
            gcu_ColorTexUnit = null;
            colorTexBBox = null;
            gcu_ColorTexBBox = null;
        }
    }

    private void initBuffer(final int verticeCount, final int indexCount) {
        indicesBuffer = GLArrayDataServer.createData(3, glIdxType(), indexCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
        indicesBuffer.setGrowthFactor(growthFactor);
        curIndicesCap = indicesBuffer.getElemCapacity();

        gca_VerticesAttr = GLArrayDataServer.createGLSL(AttributeNames.VERTEX_ATTR_NAME, 3, GL.GL_FLOAT,
                false, verticeCount, GL.GL_STATIC_DRAW);
        gca_VerticesAttr.setGrowthFactor(growthFactor);
        gca_CurveParamsAttr = GLArrayDataServer.createGLSL(AttributeNames.CURVEPARAMS_ATTR_NAME, 3, GL.GL_FLOAT,
                false, verticeCount, GL.GL_STATIC_DRAW);
        gca_CurveParamsAttr.setGrowthFactor(growthFactor);
        if( hasColorChannel() ) {
            gca_ColorsAttr = GLArrayDataServer.createGLSL(AttributeNames.COLOR_ATTR_NAME, 4, GL.GL_FLOAT,
                    false, verticeCount, GL.GL_STATIC_DRAW);
            gca_ColorsAttr.setGrowthFactor(growthFactor);
        }
        curVerticesCap = gca_VerticesAttr.getElemCapacity();
    }

    @Override
    public void growBuffer(final int verticesCount, final int indicesCount) {
        if( curIndicesCap < indicesBuffer.elemPosition() + indicesCount ) {
            indicesBuffer.growIfNeeded(indicesCount * indicesBuffer.getCompsPerElem());
            curIndicesCap = indicesBuffer.getElemCapacity();
        }
        if( curVerticesCap < gca_VerticesAttr.elemPosition() + verticesCount ) {
            gca_VerticesAttr.growIfNeeded(verticesCount * gca_VerticesAttr.getCompsPerElem());
            gca_CurveParamsAttr.growIfNeeded(verticesCount * gca_CurveParamsAttr.getCompsPerElem());
            if( null != gca_ColorsAttr ) {
                gca_ColorsAttr.growIfNeeded(verticesCount * gca_ColorsAttr.getCompsPerElem());
            }
            curVerticesCap = gca_VerticesAttr.getElemCapacity();
        }
    }

    @Override
    public void setBufferCapacity(final int verticesCount, final int indicesCount) {
        if( curIndicesCap < indicesCount ) {
            indicesBuffer.reserve(indicesCount);
            curIndicesCap = indicesBuffer.getElemCapacity();
        }
        if( curVerticesCap < verticesCount ) {
            gca_VerticesAttr.reserve(verticesCount);
            gca_CurveParamsAttr.reserve(verticesCount);
            if( null != gca_ColorsAttr ) {
                gca_ColorsAttr.reserve(verticesCount);
            }
            curVerticesCap = gca_VerticesAttr.getElemCapacity();
        }
    }

    @Override
    protected final void clearImpl(final GL2ES2 gl) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegionSPES2 Clear: " + this);
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
            gca_ColorsAttr.clear(gl);
        }
    }

    @Override
    public void printBufferStats(final PrintStream out) {
        final int[] size= { 0 }, capacity= { 0 };
        out.println("VBORegionSPES2: idx32 "+usesI32Idx());
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
        put3f((FloatBuffer)gca_VerticesAttr.getBuffer(), coords[0], coords[1], coords[2]);
        put3f((FloatBuffer)gca_CurveParamsAttr.getBuffer(), texParams[0], texParams[1], texParams[2]);
        if( null != gca_ColorsAttr ) {
            if( null != rgba ) {
                put4f((FloatBuffer)gca_ColorsAttr.getBuffer(), rgba[0], rgba[1], rgba[2], rgba[3]);
            } else {
                throw new IllegalArgumentException("Null color given for COLOR_CHANNEL rendering mode");
            }
        }
    }

    @Override
    protected final void pushVertices(final float[] coords1, final float[] coords2, final float[] coords3,
                                      final float[] texParams1, final float[] texParams2, final float[] texParams3, final float[] rgba) {
        put3f((FloatBuffer)gca_VerticesAttr.getBuffer(), coords1[0], coords1[1], coords1[2]);
        put3f((FloatBuffer)gca_VerticesAttr.getBuffer(), coords2[0], coords2[1], coords2[2]);
        put3f((FloatBuffer)gca_VerticesAttr.getBuffer(), coords3[0], coords3[1], coords3[2]);
        put3f((FloatBuffer)gca_CurveParamsAttr.getBuffer(), texParams1[0], texParams1[1], texParams1[2]);
        put3f((FloatBuffer)gca_CurveParamsAttr.getBuffer(), texParams2[0], texParams2[1], texParams2[2]);
        put3f((FloatBuffer)gca_CurveParamsAttr.getBuffer(), texParams3[0], texParams3[1], texParams3[2]);
        if( null != gca_ColorsAttr ) {
            if( null != rgba ) {
                final float r=rgba[0], g=rgba[1], b=rgba[2], a=rgba[3];
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
            put3i((IntBuffer)indicesBuffer.getBuffer(), idx1, idx2, idx3);
        } else {
            put3s((ShortBuffer)indicesBuffer.getBuffer(), (short)idx1, (short)idx2, (short)idx3);
        }
    }

    @Override
    protected void updateImpl(final GL2ES2 gl, final int curRenderModes) {
        final boolean hasColorChannel = Region.hasColorChannel( curRenderModes );
        final boolean hasColorTexture = Region.hasColorTexture( curRenderModes );

        // seal buffers
        gca_VerticesAttr.seal(gl, true);
        gca_VerticesAttr.enableBuffer(gl, false);
        gca_CurveParamsAttr.seal(gl, true);
        gca_CurveParamsAttr.enableBuffer(gl, false);
        if( hasColorChannel && null != gca_ColorsAttr ) {
            gca_ColorsAttr.seal(gl, true);
            gca_ColorsAttr.enableBuffer(gl, false);
        }
        if( hasColorTexture && null != gcu_ColorTexUnit && colorTexSeq.isTextureAvailable() ) {
            final TextureSequence.TextureFrame frame = colorTexSeq.getLastTexture();
            final Texture tex = frame.getTexture();
            final TextureCoords tc = tex.getImageTexCoords();
            final float tcSx = 1f / ( tc.right() - tc.left() );
            colorTexBBox[0] = box.getMinX() * tcSx;
            colorTexBBox[2] = box.getMaxX() * tcSx;
            final float tcSy;
            if( tex.getMustFlipVertically() ) {
                tcSy = 1f / ( tc.bottom() - tc.top() );
                colorTexBBox[1] = box.getMaxY() * tcSy;
                colorTexBBox[3] = box.getMinY() * tcSy;
            } else {
                tcSy = 1f / ( tc.top() - tc.bottom() );
                colorTexBBox[1] = box.getMinY() * tcSy;
                colorTexBBox[3] = box.getMaxY() * tcSy;
            }
        }
        indicesBuffer.seal(gl, true);
        indicesBuffer.enableBuffer(gl, false);
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegionSPES2 idx "+indicesBuffer);
            System.err.println("VBORegionSPES2 ver "+gca_VerticesAttr);
            System.err.println("VBORegionSPES2 tex "+gca_CurveParamsAttr);
        }
    }

    private static final boolean throwOnError = false; // FIXME
    /**
     * <p>
     * Since multiple {@link Region}s may share one
     * {@link ShaderProgram} managed and owned by {@link RegionRendered}, the uniform data must always be updated.
     * </p>
     *
     * @param gl
     * @param renderer
     * @param curRenderModes
     * @param quality
     */
    public void useShaderProgram(final GL2ES2 gl, final RegionRenderer renderer, final int curRenderModes, final int quality) {
        final boolean hasColorChannel = Region.hasColorChannel( curRenderModes );
        final boolean hasColorTexture = Region.hasColorTexture( curRenderModes ) && null != colorTexSeq;

        final RenderState rs = renderer.getRenderState();
        final boolean updateLocGlobal = renderer.useShaderProgram(gl, curRenderModes, true, quality, 0, colorTexSeq);
        final ShaderProgram sp = renderer.getRenderState().getShaderProgram();
        final boolean updateLocLocal = !sp.equals(spPass1);
        spPass1 = sp;
        if( DEBUG ) {
            System.err.println("XXX changedSP.p1 updateLocation loc "+updateLocLocal+" / glob "+updateLocGlobal);
        }
        if( updateLocLocal ) {
            rs.updateAttributeLoc(gl, true, gca_VerticesAttr, throwOnError);
            rs.updateAttributeLoc(gl, true, gca_CurveParamsAttr, throwOnError);
            if( hasColorChannel && null != gca_ColorsAttr ) {
                rs.updateAttributeLoc(gl, true, gca_ColorsAttr, throwOnError);
            }
        }
        rsLocal.update(gl, rs, updateLocLocal, curRenderModes, true, throwOnError);
        if( hasColorTexture && null != gcu_ColorTexUnit ) {
            rs.updateUniformLoc(gl, updateLocLocal, gcu_ColorTexUnit, throwOnError);
            rs.updateUniformLoc(gl, updateLocLocal, gcu_ColorTexBBox, throwOnError);
        }
    }


    @Override
    protected void drawImpl(final GL2ES2 gl, final RegionRenderer renderer, final int curRenderModes, final int[/*1*/] sampleCount) {
        final boolean hasColorChannel = Region.hasColorChannel( curRenderModes );
        final boolean hasColorTexture = Region.hasColorTexture( curRenderModes );

        useShaderProgram(gl, renderer, curRenderModes, getQuality());

        if( 0 >= indicesBuffer.getElemCount() ) {
            if(DEBUG_INSTANCE) {
                System.err.printf("VBORegionSPES2.drawImpl: Empty%n");
            }
            return; // empty!
        }
        gca_VerticesAttr.enableBuffer(gl, true);
        gca_CurveParamsAttr.enableBuffer(gl, true);
        if( hasColorChannel && null != gca_ColorsAttr ) {
            gca_ColorsAttr.enableBuffer(gl, true);
        }
        indicesBuffer.bindBuffer(gl, true); // keeps VBO binding

        if( renderer.getRenderState().isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED) ) {
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        }

        if( hasColorTexture && null != gcu_ColorTexUnit && colorTexSeq.isTextureAvailable() ) {
            final TextureSequence.TextureFrame frame = colorTexSeq.getNextTexture(gl);
            gl.glActiveTexture(GL.GL_TEXTURE0 + colorTexSeq.getTextureUnit());
            final Texture tex = frame.getTexture();
            tex.bind(gl);
            tex.enable(gl); // nop on core
            gcu_ColorTexUnit.setData(colorTexSeq.getTextureUnit());
            gl.glUniform(gcu_ColorTexUnit); // Always update, since program maybe used by multiple regions
            gl.glUniform(gcu_ColorTexBBox); // Always update, since program maybe used by multiple regions
            gl.glDrawElements(GL.GL_TRIANGLES, indicesBuffer.getElemCount() * indicesBuffer.getCompsPerElem(), glIdxType(), 0);
            // gl.glDrawElements(GL.GL_LINE_STRIP, indicesBuffer.getElementCount() * indicesBuffer.getComponentCount(), gl_idx_type, 0);
            tex.disable(gl); // nop on core
        } else {
            gl.glDrawElements(GL.GL_TRIANGLES, indicesBuffer.getElemCount() * indicesBuffer.getCompsPerElem(), glIdxType(), 0);
            // gl.glDrawElements(GL.GL_LINE_STRIP, indicesBuffer.getElementCount() * indicesBuffer.getComponentCount(), gl_idx_type, 0);
        }

        indicesBuffer.bindBuffer(gl, false);
        if( hasColorChannel && null != gca_ColorsAttr ) {
            gca_ColorsAttr.enableBuffer(gl, false);
        }
        gca_CurveParamsAttr.enableBuffer(gl, false);
        gca_VerticesAttr.enableBuffer(gl, false);
    }

    @Override
    protected void destroyImpl(final GL2ES2 gl) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegionSPES2 Destroy: " + this);
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
        spPass1 = null; // owned by RegionRenderer
    }
}
