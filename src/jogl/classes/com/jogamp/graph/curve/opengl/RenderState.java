/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.curve.opengl;

import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLUniformData;

import jogamp.graph.curve.opengl.shader.UniformNames;

import com.jogamp.common.os.Platform;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderProgram;

public class RenderState {
    private static final String thisKey = "jogamp.graph.curve.RenderState" ;

    /**
     * Bitfield hint, {@link #isHintMaskSet(int) if set}
     * stating <i>enabled</i> {@link GL#GL_BLEND}, otherwise <i>disabled</i>.
     * <p>
     * Shall be set via {@link #setHintMask(int)} and cleared via {@link #clearHintMask(int)}.
     * </p>
     * <p>
     * Due to alpha blending and multipass rendering, e.g. {@link Region#VBAA_RENDERING_BIT},
     * the clear-color shall be set to the {@link #getColorStaticUniform() foreground color} and <i>zero alpha</i>,
     * otherwise blending will amplify the scene's clear-color.
     * </p>
     * <p>
     * Shall be called by custom code, e.g. via {@link RegionRenderer}'s
     * enable and disable {@link RegionRenderer.GLCallback} as done in
     * {@link RegionRenderer#defaultBlendEnable} and {@link RegionRenderer#defaultBlendDisable}.
     * </p>
     */
    public static final int BITHINT_BLENDING_ENABLED = 1 << 0 ;

    public static RenderState createRenderState(Vertex.Factory<? extends Vertex> pointFactory) {
        return new RenderState(pointFactory, null);
    }

    public static RenderState createRenderState(Vertex.Factory<? extends Vertex> pointFactory, PMVMatrix pmvMatrix) {
        return new RenderState(pointFactory, pmvMatrix);
    }

    public static final RenderState getRenderState(GL2ES2 gl) {
        return (RenderState) gl.getContext().getAttachedObject(thisKey);
    }

    private final Vertex.Factory<? extends Vertex> vertexFactory;
    private final PMVMatrix pmvMatrix;
    private final GLUniformData gcu_PMVMatrix01;
    private final GLUniformData gcu_Weight;
    private final GLUniformData gcu_ColorStatic;
    private boolean gcu_PMVMatrix01_dirty = true;
    private boolean gcu_Weight_dirty = true;
    private boolean gcu_ColorStatic_dirty = true;
    private ShaderProgram sp;
    private int hintBitfield;

    protected RenderState(Vertex.Factory<? extends Vertex> vertexFactory, PMVMatrix pmvMatrix) {
        this.sp = null;
        this.vertexFactory = vertexFactory;
        this.pmvMatrix = null != pmvMatrix ? pmvMatrix : new PMVMatrix();
        this.gcu_PMVMatrix01 = new GLUniformData(UniformNames.gcu_PMVMatrix01, 4, 4, this.pmvMatrix.glGetPMvMatrixf());
        this.gcu_Weight = new GLUniformData(UniformNames.gcu_Weight, 1.0f);
        this.gcu_ColorStatic = new GLUniformData(UniformNames.gcu_ColorStatic, 4, FloatBuffer.allocate(4));
        this.hintBitfield = 0;
    }

    public final ShaderProgram getShaderProgram() { return sp; }
    public final boolean isShaderProgramInUse() { return null != sp ? sp.inUse() : false; }

    /**
     * Set a {@link ShaderProgram} and enable it. If the given {@link ShaderProgram} is new,
     * method returns true, otherwise false.
     * @param gl
     * @param sp
     * @return true if a new shader program is being used and hence external uniform-data and -location,
     *         as well as the attribute-location must be updated, otherwise false.
     */
    public final boolean setShaderProgram(final GL2ES2 gl, final ShaderProgram sp) {
        if( sp.equals(this.sp) ) {
            sp.useProgram(gl, true);
            return false;
        }
        this.sp = sp;
        sp.useProgram(gl, true);
        return true;
    }

    public final Vertex.Factory<? extends Vertex> getVertexFactory() { return vertexFactory; }

    public final PMVMatrix getMatrix() { return pmvMatrix; }
    public final PMVMatrix getMatrixMutable() {
        gcu_PMVMatrix01_dirty = true;
        return pmvMatrix;
    }
    public final GLUniformData getMatrixUniform() { return gcu_PMVMatrix01; }
    public final void setMatrixDirty() { gcu_PMVMatrix01_dirty = true; }
    public final boolean isMatrixDirty() { return gcu_PMVMatrix01_dirty;}

    public final void updateMatrix(GL2ES2 gl) {
        if( gcu_PMVMatrix01_dirty && sp.inUse() ) {
            gl.glUniform( gcu_PMVMatrix01 );
            gcu_PMVMatrix01_dirty = false;
        }
    }

    public static boolean isWeightValid(float v) {
        return 0.0f <= v && v <= 1.9f ;
    }
    public final float getWeight() { return gcu_Weight.floatValue(); }
    public final void setWeight(float v) {
        if( !isWeightValid(v) ) {
             throw new IllegalArgumentException("Weight out of range");
        }
        gcu_Weight_dirty = true;
        gcu_Weight.setData(v);
    }


    public final float[] getColorStatic(float[] rgbaColor) {
        FloatBuffer fb = (FloatBuffer) gcu_ColorStatic.getBuffer();
        rgbaColor[0] = fb.get(0);
        rgbaColor[1] = fb.get(1);
        rgbaColor[2] = fb.get(2);
        rgbaColor[3] = fb.get(3);
        return rgbaColor;
    }
    public final void setColorStatic(float r, float g, float b, float a){
        final FloatBuffer fb = (FloatBuffer) gcu_ColorStatic.getBuffer();
        fb.put(0, r);
        fb.put(1, g);
        fb.put(2, b);
        fb.put(3, a);
        gcu_ColorStatic_dirty = true;
    }


    /**
     *
     * @param gl
     * @param updateLocation
     * @param renderModes
     * @return true if no error occurred, i.e. all locations found, otherwise false.
     */
    public final boolean update(GL2ES2 gl, final boolean updateLocation, final int renderModes, final boolean pass1) {
        boolean res = true;
        if( null != sp && sp.inUse() ) {
            if( ( !Region.isTwoPass(renderModes) || !pass1 ) && ( gcu_PMVMatrix01_dirty || updateLocation ) ) {
                final boolean r0 = updateUniformDataLoc(gl, updateLocation, gcu_PMVMatrix01_dirty, gcu_PMVMatrix01);
                System.err.println("XXX gcu_PMVMatrix01.update: "+r0);
                res = res && r0;
                gcu_PMVMatrix01_dirty = !r0;
            }
            if( pass1 ) {
                if( Region.hasVariableWeight( renderModes ) && ( gcu_Weight_dirty || updateLocation ) ) {
                    final boolean r0 = updateUniformDataLoc(gl, updateLocation, gcu_Weight_dirty, gcu_Weight);
                    System.err.println("XXX gcu_Weight.update: "+r0);
                    res = res && r0;
                    gcu_Weight_dirty = !r0;
                }
                if( gcu_ColorStatic_dirty || updateLocation )  {
                    final boolean r0 = updateUniformDataLoc(gl, updateLocation, gcu_ColorStatic_dirty, gcu_ColorStatic);
                    System.err.println("XXX gcu_ColorStatic.update: "+r0);
                    res = res && r0;
                    gcu_ColorStatic_dirty = false;
                }
            }
        }
        return res;
    }

    /**
     *
     * @param gl
     * @param updateLocation
     * @param data
     * @return true if no error occured, i.e. all locations found, otherwise false.
     */
    public final boolean updateUniformLoc(final GL2ES2 gl, final boolean updateLocation, final GLUniformData data) {
        if( updateLocation || 0 > data.getLocation() ) {
            return 0 <= data.setLocation(gl, sp.program());
        } else {
            return true;
        }
    }

    /**
     *
     * @param gl
     * @param updateLocation
     * @param updateData TODO
     * @param data
     * @return true if no error occured, i.e. all locations found, otherwise false.
     */
    public final boolean updateUniformDataLoc(final GL2ES2 gl, boolean updateLocation, boolean updateData, final GLUniformData data) {
        updateLocation = updateLocation || 0 > data.getLocation();
        if( updateLocation ) {
            updateData = 0 <= data.setLocation(gl, sp.program());
        }
        if( updateData ){
            gl.glUniform(data);
            return true;
        } else {
            return !updateLocation;
        }
    }

    /**
     * @param gl
     * @param data
     * @return true if no error occured, i.e. all locations found, otherwise false.
     */
    public final boolean updateAttributeLoc(final GL2ES2 gl, final boolean updateLocation, final GLArrayDataServer data) {
        if( updateLocation || 0 > data.getLocation() ) {
            return 0 <= data.setLocation(gl, sp.program());
        } else {
            return true;
        }
    }


    public final boolean isHintMaskSet(int mask) {
        return mask == ( hintBitfield & mask );
    }
    public final void setHintMask(int mask) {
        hintBitfield |= mask;
    }
    public final void clearHintMask(int mask) {
        hintBitfield &= ~mask;
    }

    public void destroy(GL2ES2 gl) {
        if( null != sp ) {
            sp.destroy(gl);
            sp = null;
        }
    }

    public final RenderState attachTo(GL2ES2 gl) {
        return (RenderState) gl.getContext().attachObject(thisKey, this);
    }

    public final boolean detachFrom(GL2ES2 gl) {
        RenderState _rs = (RenderState) gl.getContext().getAttachedObject(thisKey);
        if(_rs == this) {
            gl.getContext().detachObject(thisKey);
            return true;
        }
        return false;
    }

    public StringBuilder toString(StringBuilder sb, boolean alsoUnlocated) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        sb.append("RenderState[").append(sp).append(Platform.NEWLINE);
        // pmvMatrix.toString(sb, "%.2f");
        sb.append(", dirty[pmv "+gcu_PMVMatrix01_dirty+", color "+gcu_ColorStatic_dirty+", weight "+gcu_Weight_dirty+"], ").append(Platform.NEWLINE);
        sb.append(gcu_PMVMatrix01).append(", ").append(Platform.NEWLINE);
        sb.append(gcu_ColorStatic).append(", ");
        sb.append(gcu_Weight).append("]");
        return sb;
    }

    @Override
    public String toString() {
        return toString(null, false).toString();
    }
}
