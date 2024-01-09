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

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;

import jogamp.graph.curve.opengl.shader.UniformNames;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.math.geom.AABBox;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureSequence;

public final class VBORegionSPES2 extends GLRegion {
    private final RenderState.ProgramLocal rsLocal;

    // Pass-1:
    private final GLUniformData gcu_ColorTexUnit;
    private final float[] colorTexBBox; // minX/minY, maxX/maxY, texW/texH
    private final GLUniformData gcu_ColorTexBBox; // vec2 gcu_ColorTexBBox[3] -> boxMin[2], boxMax[2] and texSize[2]
    private final float[] clipBBox; // minX/minY/minZ, maxX/maxY/maxZ
    private final GLUniformData gcu_ClipBBox; // uniform vec3  gcu_ClipBBox[2]; // box-min[3], box-max[3]
    private ShaderProgram spPass1 = null;

    public VBORegionSPES2(final GLProfile glp, final int renderModes, final TextureSequence colorTexSeq,
                          final int initialVerticesCount, final int initialIndicesCount)
    {
        super(glp, renderModes, colorTexSeq);

        rsLocal = new RenderState.ProgramLocal();

        initBuffer(initialVerticesCount, initialIndicesCount);

        if( hasColorTexture() ) {
            gcu_ColorTexUnit = new GLUniformData(UniformNames.gcu_ColorTexUnit, colorTexSeq.getTextureUnit());
            colorTexBBox = new float[6];
            gcu_ColorTexBBox = new GLUniformData(UniformNames.gcu_ColorTexBBox, 2, FloatBuffer.wrap(colorTexBBox));
        } else {
            gcu_ColorTexUnit = null;
            colorTexBBox = null;
            gcu_ColorTexBBox = null;
        }
        clipBBox = new float[6];
        gcu_ClipBBox = new GLUniformData(UniformNames.gcu_ClipBBox, 3, FloatBuffer.wrap(clipBBox));
    }

    @Override
    public void setTextureUnit(final int pass2TexUnit) {
        // nop
    }

    @Override
    protected final void clearImpl(final GL2ES2 gl) { }

    @Override
    protected void updateImpl(final GL2ES2 gl, final RegionRenderer renderer, final int curRenderModes) {
        // final boolean hasColorChannel = Region.hasColorChannel( curRenderModes );
        final boolean hasColorTexture = Region.hasColorTexture( curRenderModes );

        // seal buffers
        vpc_ileave.seal(gl, true);
        vpc_ileave.enableBuffer(gl, false);
        if( hasColorTexture && null != gcu_ColorTexUnit && colorTexSeq.isTextureAvailable() ) {
            TextureSequence.setTexCoordBBox(colorTexSeq.getLastTexture().getTexture(), box, isColorTextureLetterbox(), colorTexBBox, true);
        }
        indicesBuffer.seal(gl, true);
        indicesBuffer.enableBuffer(gl, false);
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegionSPES2 idx "+indicesBuffer);
            System.err.println("VBORegionSPES2 vpc "+vpc_ileave);
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
        final boolean hasAABBoxClipping = null != rs.getClipBBox();

        final boolean updateLocGlobal = renderer.useShaderProgram(gl, curRenderModes, true, quality, 0, colorTexSeq);
        final ShaderProgram sp = renderer.getRenderState().getShaderProgram();
        final boolean updateLocLocal = !sp.equals(spPass1);
        spPass1 = sp;
        if( DEBUG ) {
            if( DEBUG_ALL_EVENT || updateLocLocal || updateLocGlobal ) {
                System.err.println("XXX changedSP.p1 updateLocation loc "+updateLocLocal+" / glob "+updateLocGlobal);
            }
        }
        if( updateLocLocal ) {
            rs.updateAttributeLoc(gl, true, gca_VerticesAttr, throwOnError);
            rs.updateAttributeLoc(gl, true, gca_CurveParamsAttr, throwOnError);
            if( hasColorChannel && null != gca_ColorsAttr ) {
                rs.updateAttributeLoc(gl, true, gca_ColorsAttr, throwOnError);
            }
            if( hasAABBoxClipping ) {
                rs.updateUniformLoc(gl, true, gcu_ClipBBox, throwOnError);
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
        // final boolean hasColorChannel = Region.hasColorChannel( curRenderModes );
        final boolean hasColorTexture = Region.hasColorTexture( curRenderModes );

        useShaderProgram(gl, renderer, curRenderModes, getQuality());
        {
            final AABBox cb = renderer.getClipBBox();
            if( null != cb ) {
                clipBBox[0] = cb.getMinX(); clipBBox[1] = cb.getMinY(); clipBBox[2] = cb.getMinZ();
                clipBBox[3] = cb.getMaxX(); clipBBox[4] = cb.getMaxY(); clipBBox[5] = cb.getMaxZ();
                gl.glUniform(gcu_ClipBBox); // Always update, since program maybe used by multiple regions
            }
        }

        if( 0 >= indicesBuffer.getElemCount() ) {
            if(DEBUG_INSTANCE) {
                System.err.printf("VBORegionSPES2.drawImpl: Empty%n");
            }
            return; // empty!
        }
        vpc_ileave.enableBuffer(gl, true);
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
        vpc_ileave.enableBuffer(gl, false);
    }

    @Override
    protected void destroyImpl(final GL2ES2 gl) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegionSPES2 Destroy: " + this);
        }
        spPass1 = null; // owned by RegionRenderer
    }
}
