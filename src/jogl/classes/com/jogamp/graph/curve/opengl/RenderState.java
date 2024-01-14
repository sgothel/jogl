/**
 * Copyright 2011-2023 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLUniformData;

import jogamp.common.os.PlatformPropsImpl;
import jogamp.graph.curve.opengl.shader.UniformNames;

import com.jogamp.graph.curve.Region;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.util.GLArrayDataWrapper;
import com.jogamp.opengl.util.glsl.ShaderProgram;

/**
 * The RenderState is owned by {@link RegionRenderer}.
 *
 * It holds rendering state data like {@link PMVMatrix4f}, viewport,
 * but also the current {@link #getColorStatic(float[]) static color}.
 */
public class RenderState {
    private static final String thisKey = "jogamp.graph.curve.RenderState" ;

    /**
     * Bitfield hint, {@link #isHintMaskSet(int) if set}
     * stating <i>enabled</i> {@link GL#GL_BLEND}, otherwise <i>disabled</i>.
     * <p>
     * Shall be set via {@link #setHintMask(int)} and cleared via {@link #clearHintMask(int)}.
     * </p>
     * <p>
     * If set, {@link GLRegion#draw(GL2ES2, RegionRenderer, int, int[]) GLRegion's draw-method}
     * will set the proper {@link GL#glBlendFuncSeparate(int, int, int, int) blend-function}
     * and the clear-color to <i>transparent-black</i> in case of {@link Region#isTwoPass(int) multipass} FBO rendering.
     * </p>
     * <p>
     * Shall be set by custom code, e.g. via {@link RegionRenderer}'s
     * enable and disable {@link RegionRenderer.GLCallback} as done in
     * {@link RegionRenderer#defaultBlendEnable} and {@link RegionRenderer#defaultBlendDisable}.
     * </p>
     */
    public static final int BITHINT_BLENDING_ENABLED = 1 << 0 ;

    /**
     * Bitfield hint, {@link #isHintMaskSet(int) if set}
     * stating globally <i>enabled</i> {@link GL#GL_DEPTH_TEST}, otherwise <i>disabled</i>.
     * <p>
     * Shall be set via {@link #setHintMask(int)} and cleared via {@link #clearHintMask(int)}.
     * </p>
     * <p>
     * {@link GLRegion#draw(GL2ES2, RegionRenderer, int, int[]) GLRegion's draw-method}
     * may toggle depth test, and reset it's state according to this hint.
     * </p>
     * <p>
     * Shall be set by custom code, e.g. after {@link RenderState} or {@link RegionRenderer} construction.
     * </p>
     */
    public static final int BITHINT_GLOBAL_DEPTH_TEST_ENABLED = 1 << 1 ;

    public static final RenderState getRenderState(final GL2ES2 gl) {
        return (RenderState) gl.getContext().getAttachedObject(thisKey);
    }

    private final int id;
    private final PMVMatrix4f pmvMatrix;
    private final float[] weight;
    private final FloatBuffer weightBuffer;
    private final float[] colorStatic;
    private final FloatBuffer colorStaticBuffer;
    /** Optional clipping {@link AABBox}, which shall be pre-multiplied with the Mv-matrix. Null if unused. */
    private AABBox clipBBox;
    private int hintBitfield;
    private ShaderProgram sp;

    private static synchronized int getNextID() {
        return nextID++;
    }
    private static int nextID = 1;

    /**
     * Representation of {@link RenderState} data per {@link ShaderProgram}
     * as {@link GLUniformData}.
     * <p>
     * FIXME: Utilize 'ARB_Uniform_Buffer_Object' where available!
     * </p>
     */
    public static class ProgramLocal {
        public final GLUniformData gcu_PMVMatrix01;
        public final GLUniformData gcu_Weight;
        public final GLUniformData gcu_ColorStatic;
        private int rsId = -1;

        public ProgramLocal() {
            gcu_PMVMatrix01 = GLUniformData.creatEmptyMatrix(UniformNames.gcu_PMVMatrix01, 4, 4);
            gcu_Weight = GLUniformData.creatEmptyVector(UniformNames.gcu_Weight, 1);
            gcu_ColorStatic = GLUniformData.creatEmptyVector(UniformNames.gcu_ColorStatic, 4);
        }

        public final int getRenderStateId() { return rsId; }

        /**
         * <p>
         * Since {@link RenderState} data is being used in multiple
         * {@link ShaderProgram}s the data must always be written.
         * </p>
         * @param gl
         * @param updateLocation
         * @param renderModes
         * @param setPMVMat01 TODO
         * @param throwOnError TODO
         * @return true if no error occurred, i.e. all locations found, otherwise false.
         */
        public final boolean update(final GL2ES2 gl, final RenderState rs, final boolean updateLocation, final int renderModes,
                                    final boolean setPMVMat01, final boolean pass1, final boolean throwOnError) {
            if( rs.id() != rsId ) {
                // Assignment of Renderstate buffers to uniforms (no copy, direct reference)
                gcu_PMVMatrix01.setData(rs.pmvMatrix.getSyncPMv());
                gcu_Weight.setData(rs.weightBuffer);
                gcu_ColorStatic.setData(rs.colorStaticBuffer);
                rsId = rs.id();
            }
            boolean res = true;
            if( null != rs.sp && rs.sp.inUse() ) {
                if( setPMVMat01 ) {
                    final boolean r0 = rs.updateUniformDataLoc(gl, updateLocation, true, gcu_PMVMatrix01, throwOnError);
                    res = res && r0;
                }
                if( pass1 ) {
                    if( Region.hasVariableWeight( renderModes ) ) {
                        final boolean r0 = rs.updateUniformDataLoc(gl, updateLocation, true, gcu_Weight, throwOnError);
                        res = res && r0;
                    }
                    {
                        final boolean r0 = rs.updateUniformDataLoc(gl, updateLocation, true, gcu_ColorStatic, throwOnError);
                        res = res && r0;
                    }
                }
            }
            return res;
        }

        public StringBuilder toString(StringBuilder sb, final boolean alsoUnlocated) {
            if(null==sb) {
                sb = new StringBuilder();
            }
            sb.append("ProgramLocal[rsID ").append(rsId).append(PlatformPropsImpl.NEWLINE);
            // pmvMatrix.toString(sb, "%.2f");
            sb.append(gcu_PMVMatrix01).append(", ").append(PlatformPropsImpl.NEWLINE);
            sb.append(gcu_ColorStatic).append(", ");
            sb.append(gcu_Weight).append("]");
            return sb;
        }

        @Override
        public String toString() {
            return toString(null, false).toString();
        }
    }

    /**
     * Create a RenderState, a composition of RegionRenderer
     * @param sharedPMVMatrix optional shared PMVMatrix4f, if null using a local instance
     */
    /* pp */ RenderState(final PMVMatrix4f sharedPMVMatrix) {
        this.id = getNextID();
        this.pmvMatrix = null != sharedPMVMatrix ? sharedPMVMatrix : new PMVMatrix4f();
        this.weight = new float[1];
        this.weightBuffer = FloatBuffer.wrap(weight);
        this.colorStatic = new float[] { 1, 1, 1, 1 };
        this.colorStaticBuffer = FloatBuffer.wrap(colorStatic);
        this.clipBBox = null;
        this.hintBitfield = 0;
        this.sp = null;
    }

    public final int id() { return id; }

    /** Return the current {@link ShaderProgram} */
    public final ShaderProgram getShaderProgram() { return sp; }

    /** Return whether the current {@link ShaderProgram} is {@link ShaderProgram#inUse() in use}. */
    public final boolean isShaderProgramInUse() { return null != sp ? sp.inUse() : false; }

    /**
     * Sets the current {@link ShaderProgram} and enables it.
     *
     * If the given {@link ShaderProgram} is not {@link #getShaderProgram() the current}, method returns true, otherwise false.
     *
     * @param gl
     * @param spNext the next current {@link ShaderProgram} to be set and enabled
     * @return true if a new shader program is being used and hence external uniform-data and -location,
     *         as well as the attribute-location must be updated, otherwise false.
     */
    public final boolean setShaderProgram(final GL2ES2 gl, final ShaderProgram spNext) {
        if( spNext.equals(this.sp) ) {
            spNext.useProgram(gl, true);
            return false;
        }
        if( null != this.sp ) {
            this.sp.notifyNotInUse();
        }
        this.sp = spNext;
        spNext.useProgram(gl, true);
        return true;
    }

    /** Borrow the current {@link PMVMatrix4f}. */
    public final PMVMatrix4f getMatrix() { return pmvMatrix; }

    public static boolean isWeightValid(final float v) {
        return 0.0f <= v && v <= 1.9f ;
    }
    public final float getWeight() { return weight[0]; }
    public final void setWeight(final float v) {
        if( !isWeightValid(v) ) {
             throw new IllegalArgumentException("Weight out of range");
        }
        weight[0] = v;
    }

    public final Vec4f getColorStatic(final Vec4f rgbaColor) {
        return rgbaColor.set(colorStatic);
    }
    public final void setColorStatic(final Vec4f rgbaColor){
        colorStatic[0] = rgbaColor.x();
        colorStatic[1] = rgbaColor.y();
        colorStatic[2] = rgbaColor.z();
        colorStatic[3] = rgbaColor.w();
    }
    public final void setColorStatic(final float r, final float g, final float b, final float a){
        colorStatic[0] = r;
        colorStatic[1] = g;
        colorStatic[2] = b;
        colorStatic[3] = a;
    }

    /** Set the optional clipping {@link AABBox}, which shall be pre-multiplied with the Mv-matrix or null to disable. */
    public final void setClipBBox(final AABBox clipBBox) { this.clipBBox = clipBBox; }
    /** Returns the optional Mv-premultiplied clipping {@link AABBox} or null if unused. */
    public final AABBox getClipBBox() { return this.clipBBox; }

    /**
     *
     * @param gl
     * @param updateLocation
     * @param data
     * @param throwOnError TODO
     * @return true if no error occured, i.e. all locations found, otherwise false.
     */
    public final boolean updateUniformLoc(final GL2ES2 gl, final boolean updateLocation, final GLUniformData data, final boolean throwOnError) {
        if( updateLocation || 0 > data.getLocation() ) {
            final boolean ok = 0 <= data.setLocation(gl, sp.program());
            if( throwOnError && !ok ) {
                sp.dumpSource(System.err);
                throw new GLException("Could not locate "+data.getName()+" in "+sp+", "+data);
            }
            return ok;
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
     * @param throwOnError TODO
     * @return true if no error occured, i.e. all locations found, otherwise false.
     */
    public final boolean updateUniformDataLoc(final GL2ES2 gl, boolean updateLocation, boolean updateData, final GLUniformData data, final boolean throwOnError) {
        updateLocation = updateLocation || 0 > data.getLocation();
        if( updateLocation ) {
            updateData = 0 <= data.setLocation(gl, sp.program());
            if( throwOnError && !updateData ) {
                sp.dumpSource(System.err);
                throw new GLException("Could not locate "+data.getName()+" in "+sp+", "+data);
            }
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
     * @param throwOnError TODO
     * @return true if no error occured, i.e. all locations found, otherwise false.
     */
    public final boolean updateAttributeLoc(final GL2ES2 gl, final boolean updateLocation, final GLArrayDataWrapper data, final boolean throwOnError) {
        if( updateLocation || 0 > data.getLocation() ) {
            final boolean ok = 0 <= data.setLocation(gl, sp.program());
            if( throwOnError && !ok ) {
                sp.dumpSource(System.err);
                throw new GLException("Could not locate "+data.getName()+" in "+sp+", "+data);
            }
            return ok;
        } else {
            return true;
        }
    }


    public final boolean isHintMaskSet(final int mask) {
        return mask == ( hintBitfield & mask );
    }
    public final void setHintMask(final int mask) {
        hintBitfield |= mask;
    }
    public final void clearHintMask(final int mask) {
        hintBitfield &= ~mask;
    }

    /**
     * Only nullifies {@link ShaderProgram} reference owned by {@link RegionRenderer}.
     */
    /* pp */ void destroy() {
        sp = null; // owned by RegionRenderer
    }

    public final RenderState attachTo(final GL2ES2 gl) {
        return (RenderState) gl.getContext().attachObject(thisKey, this);
    }

    public final boolean detachFrom(final GL2ES2 gl) {
        final RenderState _rs = (RenderState) gl.getContext().getAttachedObject(thisKey);
        if(_rs == this) {
            gl.getContext().detachObject(thisKey);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "RenderState["+sp+"]";
    }
}
