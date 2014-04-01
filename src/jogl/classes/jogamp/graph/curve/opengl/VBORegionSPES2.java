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

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;

import jogamp.graph.curve.opengl.shader.AttributeNames;

import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderProgram;

public class VBORegionSPES2 extends GLRegion {
    private GLArrayDataServer gca_VerticesAttr = null;
    private GLArrayDataServer gca_CurveParamsAttr = null;
    private GLArrayDataServer gca_ColorsAttr;
    private GLArrayDataServer indicesBuffer = null;

    public VBORegionSPES2(final int renderModes) {
        super(renderModes);
        final int initialElementCount = 256;
        indicesBuffer = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, initialElementCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);

        gca_VerticesAttr = GLArrayDataServer.createGLSL(AttributeNames.VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                                                        false, initialElementCount, GL.GL_STATIC_DRAW);

        gca_CurveParamsAttr = GLArrayDataServer.createGLSL(AttributeNames.CURVEPARAMS_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                                                           false, initialElementCount, GL.GL_STATIC_DRAW);

        if( hasColorChannel() ) {
            gca_ColorsAttr = GLArrayDataServer.createGLSL(AttributeNames.COLOR_ATTR_NAME, 4, GL2ES2.GL_FLOAT,
                                                          false, initialElementCount, GL.GL_STATIC_DRAW);
        } else {
            gca_ColorsAttr = null;
        }
    }

    @Override
    protected final void clearImpl(final GL2ES2 gl, final RegionRenderer renderer) {
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
    protected final void pushVertex(float[] coords, float[] texParams, float[] rgba) {
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
    protected final void pushIndex(int idx) {
        indicesBuffer.puts((short)idx);
    }

    @Override
    protected void updateImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        // seal buffers
        indicesBuffer.seal(gl, true);
        indicesBuffer.enableBuffer(gl, false);
        gca_VerticesAttr.seal(gl, true);
        gca_VerticesAttr.enableBuffer(gl, false);
        gca_CurveParamsAttr.seal(gl, true);
        gca_CurveParamsAttr.enableBuffer(gl, false);
        if( null != gca_ColorsAttr ) {
            gca_ColorsAttr.seal(gl, true);
            gca_ColorsAttr.enableBuffer(gl, false);
        }
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegionSPES2 idx "+indicesBuffer);
            System.err.println("VBORegionSPES2 ver "+gca_VerticesAttr);
            System.err.println("VBORegionSPES2 tex "+gca_CurveParamsAttr);
        }
    }

    private ShaderProgram spPass1 = null;

    public void useShaderProgram(final GL2ES2 gl, final RegionRenderer renderer, final int renderModes, final int quality) {
        final RenderState rs = renderer.getRenderState();
        final boolean updateLocation0 = renderer.useShaderProgram(gl, renderModes, true, quality, 0);
        final ShaderProgram sp = renderer.getRenderState().getShaderProgram();
        final boolean updateLocation = !sp.equals(spPass1);
        spPass1 = sp;

        // update attribute-location and uniform data and location
        rs.update(gl, updateLocation, renderModes, true);
        rs.updateAttributeLoc(gl, updateLocation, gca_VerticesAttr);
        rs.updateAttributeLoc(gl, updateLocation, gca_CurveParamsAttr);
        if( null != gca_ColorsAttr ) {
            rs.updateAttributeLoc(gl, updateLocation, gca_ColorsAttr);
        }
        System.err.println("XXX changedSP "+updateLocation+", "+rs);
        System.err.println("XXX gca_VerticesAttr "+gca_VerticesAttr);
        System.err.println("XXX gca_CurveParamsAttr "+gca_CurveParamsAttr);
        System.err.println("XXX gca_ColorsAttr "+gca_ColorsAttr);
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
        indicesBuffer.bindBuffer(gl, true); // keeps VBO binding

        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesBuffer.getElementCount() * indicesBuffer.getComponentCount(), GL2ES2.GL_UNSIGNED_SHORT, 0);

        indicesBuffer.bindBuffer(gl, false);
        gca_CurveParamsAttr.enableBuffer(gl, false);
        gca_VerticesAttr.enableBuffer(gl, false);
    }

    @Override
    protected void destroyImpl(final GL2ES2 gl, final RegionRenderer renderer) {
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
    }
}
