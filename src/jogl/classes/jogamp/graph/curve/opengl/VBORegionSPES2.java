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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
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

public class VBORegionSPES2 extends GLRegion {
    private final RenderState.ProgramLocal rsLocal;

    private GLArrayDataServer gca_VerticesAttr = null;
    private GLArrayDataServer gca_CurveParamsAttr = null;
    private GLArrayDataServer gca_ColorsAttr;
    private GLArrayDataServer indicesBuffer = null;
    private final GLUniformData gcu_ColorTexUnit;
    private final float[] colorTexBBox; // x0, y0, x1, y1
    private final GLUniformData gcu_ColorTexBBox;
    private ShaderProgram spPass1 = null;

    public VBORegionSPES2(final int renderModes, final TextureSequence colorTexSeq) {
        super(renderModes, colorTexSeq);

        rsLocal = new RenderState.ProgramLocal();

        final int initialElementCount = 256;
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
    }

    @Override
    protected final void clearImpl(final GL2ES2 gl) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegionSPES2 Clear: " + this);
        }
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
        gca_VerticesAttr.seal(gl, true);
        gca_VerticesAttr.enableBuffer(gl, false);
        gca_CurveParamsAttr.seal(gl, true);
        gca_CurveParamsAttr.enableBuffer(gl, false);
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
     * {@link ShaderProgram}, the uniform data must always be updated.
     * </p>
     *
     * @param gl
     * @param renderer
     * @param renderModes
     * @param quality
     */
    public void useShaderProgram(final GL2ES2 gl, final RegionRenderer renderer, final int renderModes, final int quality) {
        final RenderState rs = renderer.getRenderState();
        final boolean updateLocGlobal = renderer.useShaderProgram(gl, renderModes, true, quality, 0, colorTexSeq);
        final ShaderProgram sp = renderer.getRenderState().getShaderProgram();
        final boolean updateLocLocal = !sp.equals(spPass1);
        spPass1 = sp;
        if( DEBUG ) {
            System.err.println("XXX changedSP.p1 updateLocation loc "+updateLocLocal+" / glob "+updateLocGlobal);
        }
        if( updateLocLocal ) {
            rs.updateAttributeLoc(gl, true, gca_VerticesAttr, throwOnError);
            rs.updateAttributeLoc(gl, true, gca_CurveParamsAttr, throwOnError);
            if( null != gca_ColorsAttr ) {
                rs.updateAttributeLoc(gl, true, gca_ColorsAttr, throwOnError);
            }
        }
        rsLocal.update(gl, rs, updateLocLocal, renderModes, true, throwOnError);
        if( null != gcu_ColorTexUnit ) {
            rs.updateUniformLoc(gl, updateLocLocal, gcu_ColorTexUnit, throwOnError);
            rs.updateUniformLoc(gl, updateLocLocal, gcu_ColorTexBBox, throwOnError);
        }
    }


    @Override
    protected void drawImpl(final GL2ES2 gl, final RegionRenderer renderer, final int[/*1*/] sampleCount) {
        final int renderModes = getRenderModes();
        useShaderProgram(gl, renderer, renderModes, getQuality());

        if( 0 >= indicesBuffer.getElementCount() ) {
            if(DEBUG_INSTANCE) {
                System.err.printf("VBORegionSPES2.drawImpl: Empty%n");
            }
            return; // empty!
        }
        gca_VerticesAttr.enableBuffer(gl, true);
        gca_CurveParamsAttr.enableBuffer(gl, true);
        if( null != gca_ColorsAttr ) {
            gca_ColorsAttr.enableBuffer(gl, true);
        }
        indicesBuffer.bindBuffer(gl, true); // keeps VBO binding

        if( renderer.getRenderState().isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED) ) {
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        }

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
        spPass1 = null;
    }
}
