/**
 * Copyright 2009-2023 JogAmp Community. All rights reserved.
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
package com.jogamp.math.util;

import java.nio.Buffer;
import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Matrix4f;
import com.jogamp.math.Quaternion;
import com.jogamp.math.Ray;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.Frustum;

/**
 * PMVMatrix4f implements the basic computer graphics {@link Matrix4f} pack using
 * projection (P), modelview (Mv) and texture (T) {@link Matrix4f} operations.
 * <p>
 * Unlike {@link com.jogamp.opengl.util.PMVMatrix PMVMatrix}, this class doesn't implement
 * {@link com.jogamp.opengl.fixedfunc.GLMatrixFunc GLMatrixFunc} and is OpenGL agnostic.
 * </p>
 * <p>
 * This is the second implementation of `PMVMatrix4f` using
 * direct {@link Matrix4f}, {@link Vec4f} and {@link Vec3f} math operations instead of `float[]`
 * via {@link com.jogamp.math.FloatUtil FloatUtil}.
 * </p>
 * <p>
 * PMVMatrix4f provides the {@link #getMvi() inverse modelview matrix (Mvi)} and
 * {@link #getMvit() inverse transposed modelview matrix (Mvit)}.
 * {@link Frustum} is also provided by {@link #getFrustum()}.
 *
 * To keep these derived values synchronized after mutable Mv operations like {@link #rotateMv(Quaternion)}
 * users have to call {@link #update()} before using Mvi and Mvit.
 * </p>
 * <p>
 * All matrices are provided in column-major order,
 * as specified in the OpenGL fixed function pipeline, i.e. compatibility profile.
 * See {@link Matrix4f}.
 * </p>
 * <p>
 * PMVMatrix4f can supplement {@link com.jogamp.opengl.GL2ES2 GL2ES2} applications w/ the
 * lack of the described matrix functionality.
 * </p>
 * <a name="storageDetails"><h5>Matrix storage details</h5></a>
 * <p>
 * The {@link SyncBuffer} abstraction is provided, e.g. {@link #getSyncPMvMvi()},
 * to synchronize the respective {@link Matrix4f matrices} with the `float[]` backing store.
 * The latter is represents the data to {@link com.jogamp.opengl.GLUniformData} via its {@link FloatBuffer}s, see {@link SyncBuffer#getBuffer()},
 * and is pushed to the GPU eventually.
 * </p>
 * <p>
 * {@link SyncBuffer}'s {@link SyncAction} is called by {@link com.jogamp.opengl.GLUniformData#getBuffer()},
 * i.e. before the data is pushed to the GPU.
 * </p>
 * <p>
 * The provided {@link SyncAction} ensures that the {@link Matrix4f matrices data}
 * gets copied into the `float[]` backing store.
 * </p>
 * <p>
 * PMVMatrix4f provides two specializations of {@link SyncBuffer}, {@link SyncMatrix4f} for single {@link Matrix4f} mappings
 * and {@link SyncMatrices4f} for multiple {@link Matrix4f} mappings.
 * </p>
 * <p>
 * They can be feed directly to instantiate a {@link com.jogamp.opengl.GLUniformData} object via e.g. {@link com.jogamp.opengl.GLUniformData#GLUniformData(String, int, int, SyncBuffer)}.
 * </p>
 * <p>
 * All {@link Matrix4f matrix} {@link SyncBuffer}'s backing store are backed up by a common primitive float-array for performance considerations
 * and are a {@link Buffers#slice2Float(float[], int, int) sliced} representation of it.
 * </p>
 * <p>
 * <b>{@link Matrix4f} {@link SyncBuffer}'s Backing-Store Notes:</b>
 * <ul>
 *   <li>The {@link Matrix4f matrix} {@link SyncBuffer}'s backing store is a {@link Buffers#slice2Float(float[], int, int) sliced part } of a host matrix and it's start position has been {@link FloatBuffer#mark() marked}.</li>
 *   <li>Use {@link FloatBuffer#reset() reset()} to rewind it to it's start position after relative operations, like {@link FloatBuffer#get() get()}.</li>
 *   <li>If using absolute operations like {@link FloatBuffer#get(int) get(int)}, use it's {@link FloatBuffer#reset() reset} {@link FloatBuffer#position() position} as it's offset.</li>
 * </ul>
 * </p>
 */
public class PMVMatrix4f {

    /** Bit value stating a modified {@link #getP() projection matrix (P)}, since last {@link #update()} call. */
    public static final int MODIFIED_PROJECTION = 1 << 0;
    /** Bit value stating a modified {@link #getMv() modelview matrix (Mv)}, since last {@link #update()} call. */
    public static final int MODIFIED_MODELVIEW = 1 << 1;
    /** Bit value stating a modified {@link #getT() texture matrix (T)}, since last {@link #update()} call. */
    public static final int MODIFIED_TEXTURE = 1 << 2;
    /** Bit value stating all is modified */
    public static final int MODIFIED_ALL = MODIFIED_PROJECTION | MODIFIED_MODELVIEW | MODIFIED_TEXTURE;
    /** Bit value for {@link #getMvi() inverse modelview matrix (Mvi)}, updated via {@link #update()}. */
    public static final int INVERSE_MODELVIEW = 1 << 1;
    /** Bit value for {@link #getMvit() inverse transposed modelview matrix (Mvit)}, updated via {@link #update()}. */
    public static final int INVERSE_TRANSPOSED_MODELVIEW = 1 << 2;
    /** Bit value for {@link #getFrustum() frustum} and updated by {@link #getFrustum()}. */
    public static final int FRUSTUM = 1 << 3;
    /** Bit value for {@link #getPMv() pre-multiplied P * Mv}, updated by {@link #getPMv()}. */
    public static final int PREMUL_PMV = 1 << 4;
    /** Bit value for {@link #getPMvi() pre-multiplied invert(P * Mv)}, updated by {@link #getPMvi()}. */
    public static final int PREMUL_PMVI = 1 << 5;
    /** Manual bits not covered by {@link #update()} but {@link #getFrustum()}, {@link #FRUSTUM}, {@link #getPMv()}, {@link #PREMUL_PMV}, {@link #getPMvi()}, {@link #PREMUL_PMVI}, etc. */
    public static final int MANUAL_BITS = FRUSTUM | PREMUL_PMV | PREMUL_PMVI;

    /**
     * Creates an instance of PMVMatrix4f.
     * <p>
     * This constructor only sets up an instance w/o additional {@link #INVERSE_MODELVIEW} or {@link #INVERSE_TRANSPOSED_MODELVIEW}.
     * </p>
     * <p>
     * Implementation uses non-direct non-NIO Buffers with guaranteed backing array,
     * which are synchronized to the actual Matrix4f instances.
     * This allows faster access in Java computation.
     * </p>
     * @see #PMVMatrix4f(int)
     */
    public PMVMatrix4f() {
        this(0);
    }

    /**
     * Creates an instance of PMVMatrix4f.
     * <p>
     * Additional derived matrices can be requested via `derivedMatrices`, i.e.
     * - {@link #INVERSE_MODELVIEW}
     * - {@link #INVERSE_TRANSPOSED_MODELVIEW}
     * </p>
     * <p>
     * Implementation uses non-direct non-NIO Buffers with guaranteed backing array,
     * which are synchronized to the actual Matrix4f instances.
     * This allows faster access in Java computation.
     * </p>
     * @param derivedMatrices additional matrices can be requested by passing bits {@link #INVERSE_MODELVIEW} and {@link #INVERSE_TRANSPOSED_MODELVIEW}.
     * @see #getReqBits()
     * @see #isReqDirty()
     * @see #getDirtyBits()
     * @see #update()
     */
    public PMVMatrix4f(final int derivedMatrices) {
        // I    Identity
        // T    Texture
        // P    Projection
        // Mv   ModelView
        // Mvi  Modelview-Inverse
        // Mvit Modelview-Inverse-Transpose
        {
            int mask = 0;
            if( 0 != ( derivedMatrices & ( INVERSE_MODELVIEW | INVERSE_TRANSPOSED_MODELVIEW ) ) ) {
                mask |= INVERSE_MODELVIEW;
            }
            if( 0 != ( derivedMatrices & INVERSE_TRANSPOSED_MODELVIEW ) ) {
                mask |= INVERSE_TRANSPOSED_MODELVIEW;
            }
            requestBits = mask;
        }

        // actual underlying Matrix4f count
        int mcount     = 3;

        // actual underlying Matrix4f data
        matP           = new Matrix4f();
        matMv          = new Matrix4f();
        matTex         = new Matrix4f();

        if( 0 != ( requestBits & INVERSE_MODELVIEW ) ) {
            matMvi         = new Matrix4f();
            mMvi_offset    = 2*16;
            ++mcount;
        } else {
            matMvi         = null;
            mMvi_offset    = -1;
        }
        if( 0 != ( requestBits & INVERSE_TRANSPOSED_MODELVIEW ) ) {
            matMvit        = new Matrix4f();
            mMvit_offset   = 3*16;
            ++mcount;
        } else {
            matMvit        = null;
            mMvit_offset   = -1;
        }
        mTex_offset        = (mcount-1)*16; // last one

        // float back buffer for GPU, Matrix4f -> matrixStore via SyncedBuffer
        matrixStore    = new float[mcount*16];

        // FloatBuffer for single Matrix4f back-buffer
        final FloatBuffer bufP                 = Buffers.slice2Float(matrixStore,  mP_offset,    1*16);  // P
        syncP       = new SyncBuffer0(matP,    bufP);   // mP_offset

        final FloatBuffer bufMv                = Buffers.slice2Float(matrixStore,  mMv_offset,   1*16);  // Mv
        syncMv      = new SyncBuffer1(matMv,   bufMv,   mMv_offset);

        final FloatBuffer bufP_Mv              = Buffers.slice2Float(matrixStore,  mP_offset,    2*16);  // P + Mv
        syncP_Mv    = new SyncBufferN(new Matrix4f[]  { matP, matMv }, bufP_Mv, mP_offset);

        final FloatBuffer bufTex               = Buffers.slice2Float(matrixStore,  mTex_offset,  1*16);  // T
        syncT       = new SyncBuffer1(matTex,  bufTex,  mTex_offset);

        if( null != matMvi ) {
            final FloatBuffer bufMvi           = Buffers.slice2Float(matrixStore,  mMvi_offset,  1*16);  // Mvi
            final FloatBuffer bufP_Mv_Mvi      = Buffers.slice2Float(matrixStore,  mP_offset,    3*16);  // P + Mv + Mvi
            syncMvi      = new SyncBuffer1U(matMvi,  bufMvi,  mMvi_offset);
            syncP_Mv_Mvi = new SyncBufferNU(new Matrix4f[] { matP, matMv, matMvi }, bufP_Mv_Mvi, mP_offset);
        } else {
            syncMvi     = null;
            syncP_Mv_Mvi = null;
        }
        if( null != matMvit ) {
            final FloatBuffer bufMvit          = Buffers.slice2Float(matrixStore,  mMvit_offset, 1*16);  //          Mvit
            final FloatBuffer bufP_Mv_Mvi_Mvit = Buffers.slice2Float(matrixStore,  mP_offset,    4*16);  // P + Mv + Mvi + Mvit
            syncMvit          = new SyncBuffer1U(matMvit, bufMvit, mMvit_offset);
            syncP_Mv_Mvi_Mvit = new SyncBufferNU(new Matrix4f[] { matP, matMv, matMvi, matMvit }, bufP_Mv_Mvi_Mvit, mP_offset);
        } else {
            syncMvit = null;
            syncP_Mv_Mvi_Mvit = null;
        }

        mat4Tmp1 = new Matrix4f();

        mat4Tmp2 = null; // on demand
        matPMv = null; // on demand
        matPMvi = null; // on demand
        matPMviOK = false;
        frustum = null; // on demand

        reset();
    }

    /**
     * Issues {@link Matrix4f#loadIdentity()} on all matrices and resets all internal states.
     */
    public void reset() {
        matP.loadIdentity();
        matMv.loadIdentity();
        matTex.loadIdentity();

        modifiedBits = MODIFIED_ALL;
        dirtyBits = requestBits | MANUAL_BITS;
    }

    //
    // Temporary storage access for efficiency
    //


    /**
     * Return the second temporary Matrix4f exposed to be reused for efficiency.
     * <p>
     * Temporary storage is only used by this class within single method calls,
     * hence has no side-effects.
     * </p>
     */
    protected final Matrix4f getTmp2Mat() {
        if( null == mat4Tmp2 ) {
            mat4Tmp2 = new Matrix4f();
        }
        return mat4Tmp2;
    }

    //
    // Regular Matrix4f access as well as their SyncedBuffer counterpart SyncedMatrix and SyncedMatrices
    //

    /**
     * Returns the {@link GLMatrixFunc#GL_TEXTURE_MATRIX texture matrix} (T).
     * <p>
     * Consider using {@link #setTextureDirty()} if modifying the returned {@link Matrix4f}.
     * </p>
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final Matrix4f getT() {
        return matTex;
    }

    /**
     * Returns the {@link SyncMatrix} of {@link GLMatrixFunc#GL_TEXTURE_MATRIX texture matrix} (T).
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final SyncMatrix4f getSyncT() {
        return syncT;
    }

    /**
     * Returns the {@link GLMatrixFunc#GL_PROJECTION_MATRIX projection matrix} (P).
     * <p>
     * Consider using {@link #setProjectionDirty()} if modifying the returned {@link Matrix4f}.
     * </p>
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final Matrix4f getP() {
        return matP;
    }

    /**
     * Returns the {@link SyncMatrix} of {@link GLMatrixFunc#GL_PROJECTION_MATRIX projection matrix} (P).
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final SyncMatrix4f getSyncP() {
        return syncP;
    }

    /**
     * Returns the {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mv).
     * <p>
     * Consider using {@link #setModelviewDirty()} if modifying the returned {@link Matrix4f}.
     * </p>
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final Matrix4f getMv() {
        return matMv;
    }

    /**
     * Returns the {@link SyncMatrix} of {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mv).
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final SyncMatrix4f getSyncMv() {
        return syncMv;
    }

    /**
     * Returns {@link SyncMatrices4f} of 2 matrices within one FloatBuffer: {@link #getP() P} and {@link #getMv() Mv}.
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final SyncMatrices4f getSyncPMv() {
        return syncP_Mv;
    }

    /**
     * Returns the inverse {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mvi) if requested.
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @throws IllegalArgumentException if {@link #INVERSE_MODELVIEW} has not been requested in ctor {@link #PMVMatrix4f(int)}.
     */
    public final Matrix4f getMvi() {
        if( 0 == ( INVERSE_MODELVIEW & requestBits ) ) {
            throw new IllegalArgumentException("Not requested in ctor");
        }
        updateImpl(false);
        return matMvi;
    }

    /**
     * Returns the {@link SyncMatrix} of inverse {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mvi) if requested.
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @throws IllegalArgumentException if {@link #INVERSE_MODELVIEW} has not been requested in ctor {@link #PMVMatrix4f(int)}.
     */
    public final SyncMatrix4f getSyncMvi() {
        if( 0 == ( INVERSE_MODELVIEW & requestBits ) ) {
            throw new IllegalArgumentException("Not requested in ctor");
        }
        return syncMvi;
    }

    /**
     * Returns the inverse transposed {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mvit) if requested.
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @throws IllegalArgumentException if {@link #INVERSE_TRANSPOSED_MODELVIEW} has not been requested in ctor {@link #PMVMatrix4f(int)}.
     */
    public final Matrix4f getMvit() {
        if( 0 == ( INVERSE_TRANSPOSED_MODELVIEW & requestBits ) ) {
            throw new IllegalArgumentException("Not requested in ctor");
        }
        updateImpl(false);
        return matMvit;
    }

    /**
     * Returns the {@link SyncMatrix} of inverse transposed {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mvit) if requested.
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @throws IllegalArgumentException if {@link #INVERSE_TRANSPOSED_MODELVIEW} has not been requested in ctor {@link #PMVMatrix4f(int)}.
     */
    public final SyncMatrix4f getSyncMvit() {
        if( 0 == ( INVERSE_TRANSPOSED_MODELVIEW & requestBits ) ) {
            throw new IllegalArgumentException("Not requested in ctor");
        }
        return syncMvit;
    }

    /**
     * Returns {@link SyncMatrices4f} of 3 matrices within one FloatBuffer: {@link #getP() P}, {@link #getMv() Mv} and {@link #getMvi() Mvi} if requested.
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @throws IllegalArgumentException if {@link #INVERSE_MODELVIEW} has not been requested in ctor {@link #PMVMatrix4f(int)}.
     */
    public final SyncMatrices4f getSyncPMvMvi() {
        if( 0 == ( INVERSE_MODELVIEW & requestBits ) ) {
            throw new IllegalArgumentException("Not requested in ctor");
        }
        return syncP_Mv_Mvi;
    }

    /**
     * Returns {@link SyncMatrices4f} of 4 matrices within one FloatBuffer: {@link #getP() P}, {@link #getMv() Mv}, {@link #getMvi() Mvi} and {@link #getMvit() Mvit} if requested.
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @throws IllegalArgumentException if {@link #INVERSE_TRANSPOSED_MODELVIEW} has not been requested in ctor {@link #PMVMatrix4f(int)}.
     */
    public final SyncMatrices4f getSyncPMvMviMvit() {
        if( 0 == ( INVERSE_TRANSPOSED_MODELVIEW & requestBits ) ) {
            throw new IllegalArgumentException("Not requested in ctor");
        }
        return syncP_Mv_Mvi_Mvit;
    }

    //
    // Basic Matrix4f, Vec3f and Vec4f operations similar to GLMatrixFunc
    //

    /**
     * Returns multiplication result of {@link #getP() P} and {@link #getMv() Mv} matrix, i.e.
     * <pre>
     *    result = P x Mv
     * </pre>
     * @param result 4x4 matrix storage for result
     * @return given result matrix for chaining
     */
    public final Matrix4f getMulPMv(final Matrix4f result) {
        return result.mul(matP, matMv);
    }

    /**
     * Returns multiplication result of {@link #getMv() Mv} and {@link #getP() P} matrix, i.e.
     * <pre>
     *    result = Mv x P
     * </pre>
     * @param result 4x4 matrix storage for result
     * @return given result matrix for chaining
     */
    public final Matrix4f getMulMvP(final Matrix4f result) {
        return result.mul(matMv, matP);
    }

    /**
     * v_out = Mv * v_in
     * @param v_in input vector, can be v_out for in-place transformation
     * @param v_out output vector
     * @returns v_out for chaining
     */
    public final Vec4f mulWithMv(final Vec4f v_in, final Vec4f v_out) {
        return matMv.mulVec4f(v_in, v_out);
    }

    /**
     * v_inout = Mv * v_inout
     * @param v_inout input and output vector, i.e. in-place transformation
     * @returns v_inout for chaining
     */
    public final Vec4f mulWithMv(final Vec4f v_inout) {
        return matMv.mulVec4f(v_inout);
    }

    /**
     * v_out = Mv * v_in
     *
     * Affine 3f-vector transformation by 4x4 matrix, see {@link Matrix4f#mulVec3f(Vec3f, Vec3f)}.
     *
     * @param v_in input vector, can be v_out for in-place transformation
     * @param v_out output vector
     * @returns v_out for chaining
     */
    public final Vec3f mulWithMv(final Vec3f v_in, final Vec3f v_out) {
        return matMv.mulVec3f(v_in, v_out);
    }

    //
    // GLMatrixFunc alike functionality
    //

    /**
     * Load the {@link #getMv() modelview matrix} with the provided values.
     */
    public final PMVMatrix4f loadMv(final float[] values, final int offset) {
        matMv.load(values, offset);
        setModelviewDirty();
        return this;
    }
    /**
     * Load the {@link #getMv() modelview matrix} with the provided values.
     */
    public final PMVMatrix4f loadMv(final java.nio.FloatBuffer m) {
        final int spos = m.position();
        matMv.load(m);
        setModelviewDirty();
        m.position(spos);
        return this;
    }
    /**
     * Load the {@link #getMv() modelview matrix} with the values of the given {@link Matrix4f}.
     */
    public final PMVMatrix4f loadMv(final Matrix4f m) {
        matMv.load(m);
        setModelviewDirty();
        return this;
    }
    /**
     * Load the {@link #getMv() modelview matrix} with the values of the given {@link Quaternion}'s rotation {@link Matrix4f#setToRotation(Quaternion) matrix representation}.
     */
    public final PMVMatrix4f loadMv(final Quaternion quat) {
        matMv.setToRotation(quat);
        setModelviewDirty();
        return this;
    }

    /**
     * Load the {@link #getP() projection matrix} with the provided values.
     */
    public final PMVMatrix4f loadP(final float[] values, final int offset) {
        matP.load(values, offset);
        setProjectionDirty();
        return this;
    }
    /**
     * Load the {@link #getP() projection matrix} with the provided values.
     */
    public final PMVMatrix4f loadP(final java.nio.FloatBuffer m) {
        final int spos = m.position();
        matP.load(m);
        setProjectionDirty();
        m.position(spos);
        return this;
    }
    /**
     * Load the {@link #getP() projection matrix} with the values of the given {@link Matrix4f}.
     */
    public final PMVMatrix4f loadP(final Matrix4f m) {
        matP.load(m);
        setProjectionDirty();
        return this;
    }
    /**
     * Load the {@link #getP() projection matrix} with the values of the given {@link Quaternion}'s rotation {@link Matrix4f#setToRotation(Quaternion) matrix representation}.
     */
    public final PMVMatrix4f loadP(final Quaternion quat) {
        matP.setToRotation(quat);
        setProjectionDirty();
        return this;
    }

    /**
     * Load the {@link #getT() texture matrix} with the provided values.
     */
    public final PMVMatrix4f loadT(final float[] values, final int offset) {
        matTex.load(values, offset);
        setTextureDirty();
        return this;
    }
    /**
     * Load the {@link #getT() texture matrix} with the provided values.
     */
    public final PMVMatrix4f loadT(final java.nio.FloatBuffer m) {
        final int spos = m.position();
        matTex.load(m);
        setTextureDirty();
        m.position(spos);
        return this;
    }
    /**
     * Load the {@link #getT() texture matrix} with the values of the given {@link Matrix4f}.
     */
    public final PMVMatrix4f loadT(final Matrix4f m) {
        matTex.load(m);
        setTextureDirty();
        return this;
    }
    /**
     * Load the {@link #getT() texture matrix} with the values of the given {@link Quaternion}'s rotation {@link Matrix4f#setToRotation(Quaternion) matrix representation}.
     */
    public final PMVMatrix4f loadT(final Quaternion quat) {
        matTex.setToRotation(quat);
        setTextureDirty();
        return this;
    }

    /**
     * Load the {@link #getMv() modelview matrix} with the values of the given {@link Matrix4f}.
     */
    public final PMVMatrix4f loadMvIdentity() {
        matMv.loadIdentity();
        setModelviewDirty();
        return this;
    }

    /**
     * Load the {@link #getP() projection matrix} with the values of the given {@link Matrix4f}.
     */
    public final PMVMatrix4f loadPIdentity() {
        matP.loadIdentity();
        setProjectionDirty();
        return this;
    }

    /**
     * Load the {@link #getT() texture matrix} with the values of the given {@link Matrix4f}.
     */
    public final PMVMatrix4f loadTIdentity() {
        matTex.loadIdentity();
        setTextureDirty();
        return this;
    }

    /**
     * Multiply the {@link #getMv() modelview matrix}: [c] = [c] x [m]
     * @param m the right hand Matrix4f
     * @return this instance of chaining
     */
    public final PMVMatrix4f mulMv(final Matrix4f m) {
        matMv.mul( m );
        setModelviewDirty();
        return this;
    }

    /**
     * Multiply the {@link #getP() projection matrix}: [c] = [c] x [m]
     * @param m the right hand Matrix4f
     * @return this instance of chaining
     */
    public final PMVMatrix4f mulP(final Matrix4f m) {
        matP.mul( m );
        setProjectionDirty();
        return this;
    }

    /**
     * Multiply the {@link #getT() texture matrix}: [c] = [c] x [m]
     * @param m the right hand Matrix4f
     * @return this instance of chaining
     */
    public final PMVMatrix4f mulT(final Matrix4f m) {
        matTex.mul( m );
        setTextureDirty();
        return this;
    }

    /**
     * Translate the {@link #getMv() modelview matrix}.
     * @param x
     * @param y
     * @param z
     * @return this instance of chaining
     */
    public final PMVMatrix4f translateMv(final float x, final float y, final float z) {
        return mulMv( mat4Tmp1.setToTranslation(x, y, z) );
    }
    /**
     * Translate the {@link #getMv() modelview matrix}.
     * @param t translation vec3
     * @return this instance of chaining
     */
    public final PMVMatrix4f translateMv(final Vec3f t) {
        return mulMv( mat4Tmp1.setToTranslation(t) );
    }

    /**
     * Translate the {@link #getP() projection matrix}.
     * @param x
     * @param y
     * @param z
     * @return this instance of chaining
     */
    public final PMVMatrix4f translateP(final float x, final float y, final float z) {
        return mulP( mat4Tmp1.setToTranslation(x, y, z) );
    }
    /**
     * Translate the {@link #getP() projection matrix}.
     * @param t translation vec3
     * @return this instance of chaining
     */
    public final PMVMatrix4f translateP(final Vec3f t) {
        return mulP( mat4Tmp1.setToTranslation(t) );
    }

    /**
     * Scale the {@link #getMv() modelview matrix}.
     * @param x
     * @param y
     * @param z
     * @return this instance of chaining
     */
    public final PMVMatrix4f scaleMv(final float x, final float y, final float z) {
        return mulMv( mat4Tmp1.setToScale(x, y, z) );
    }
    /**
     * Scale the {@link #getMv() modelview matrix}.
     * @param s scale vec4f
     * @return this instance of chaining
     */
    public final PMVMatrix4f scaleMv(final Vec3f s) {
        return mulMv( mat4Tmp1.setToScale(s) );
    }

    /**
     * Scale the {@link #getP() projection matrix}.
     * @param x
     * @param y
     * @param z
     * @return this instance of chaining
     */
    public final PMVMatrix4f scaleP(final float x, final float y, final float z) {
        return mulP( mat4Tmp1.setToScale(x, y, z) );
    }
    /**
     * Scale the {@link #getP() projection matrix}.
     * @param s scale vec4f
     * @return this instance of chaining
     */
    public final PMVMatrix4f scaleP(final Vec3f s) {
        return mulP( mat4Tmp1.setToScale(s) );
    }

    /**
     * Rotate the {@link #getMv() modelview matrix} by the given axis and angle in radians.
     * <p>
     * Consider using {@link #rotateMv(Quaternion)}
     * </p>
     * @param ang_rad angle in radians
     * @param axis rotation axis
     * @return this instance of chaining
     * @see #rotateMv(Quaternion)
     */
    public final PMVMatrix4f rotateMv(final float ang_rad, final float x, final float y, final float z) {
        return mulMv( mat4Tmp1.setToRotationAxis(ang_rad, x, y, z) );
    }
    /**
     * Rotate the {@link #getMv() modelview matrix} by the given axis and angle in radians.
     * <p>
     * Consider using {@link #rotateMv(Quaternion)}
     * </p>
     * @param ang_rad angle in radians
     * @param axis rotation axis
     * @return this instance of chaining
     * @see #rotateMv(Quaternion)
     */
    public final PMVMatrix4f rotateMv(final float ang_rad, final Vec3f axis) {
        return mulMv( mat4Tmp1.setToRotationAxis(ang_rad, axis) );
    }
    /**
     * Rotate the {@link #getMv() modelview matrix} with the given {@link Quaternion}'s rotation {@link Matrix4f#setToRotation(Quaternion) matrix representation}.
     * @param quat the {@link Quaternion}
     * @return this instance of chaining
     */
    public final PMVMatrix4f rotateMv(final Quaternion quat) {
        return mulMv( mat4Tmp1.setToRotation(quat) );
    }

    /**
     * Rotate the {@link #getP() projection matrix} by the given axis and angle in radians.
     * <p>
     * Consider using {@link #rotateP(Quaternion)}
     * </p>
     * @param ang_rad angle in radians
     * @param axis rotation axis
     * @return this instance of chaining
     * @see #rotateP(Quaternion)
     */
    public final PMVMatrix4f rotateP(final float ang_rad, final float x, final float y, final float z) {
        return mulP( mat4Tmp1.setToRotationAxis(ang_rad, x, y, z) );
    }
    /**
     * Rotate the {@link #getP() projection matrix} by the given axis and angle in radians.
     * <p>
     * Consider using {@link #rotateP(Quaternion)}
     * </p>
     * @param ang_rad angle in radians
     * @param axis rotation axis
     * @return this instance of chaining
     * @see #rotateP(Quaternion)
     */
    public final PMVMatrix4f rotateP(final float ang_rad, final Vec3f axis) {
        return mulP( mat4Tmp1.setToRotationAxis(ang_rad, axis) );
    }
    /**
     * Rotate the {@link #getP() projection matrix} with the given {@link Quaternion}'s rotation {@link Matrix4f#setToRotation(Quaternion) matrix representation}.
     * @param quat the {@link Quaternion}
     * @return this instance of chaining
     */
    public final PMVMatrix4f rotateP(final Quaternion quat) {
        return mulP( mat4Tmp1.setToRotation(quat) );
    }

    /** Pop the {@link #getMv() modelview matrix} from its stack. */
    public final PMVMatrix4f popMv() {
        matMv.pop();
        setModelviewDirty();
        return this;
    }
    /** Pop the {@link #getP() projection matrix} from its stack. */
    public final PMVMatrix4f popP() {
        matP.pop();
        setProjectionDirty();
        return this;
    }
    /** Pop the {@link #getT() texture matrix} from its stack. */
    public final PMVMatrix4f popT() {
        matTex.pop();
        setTextureDirty();
        return this;
    }
    /** Push the {@link #getMv() modelview matrix} to its stack, while preserving its values. */
    public final PMVMatrix4f pushMv() {
        matMv.push();
        return this;
    }
    /** Push the {@link #getP() projection matrix} to its stack, while preserving its values. */
    public final PMVMatrix4f pushP() {
        matP.push();
        return this;
    }
    /** Push the {@link #getT() texture matrix} to its stack, while preserving its values. */
    public final PMVMatrix4f pushT() {
        matTex.push();
        return this;
    }

    /**
     * {@link #mulP(Matrix4f) Multiply} the {@link #getP() projection matrix} with the orthogonal matrix.
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param zNear
     * @param zFar
     * @see Matrix4f#setToOrtho(float, float, float, float, float, float)
     */
    public final void orthoP(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) {
        mulP( mat4Tmp1.setToOrtho(left, right, bottom, top, zNear, zFar) );
    }

    /**
     * {@link #mulP(Matrix4f) Multiply} the {@link #getP() projection matrix} with the frustum matrix.
     *
     * @throws IllegalArgumentException if {@code zNear <= 0} or {@code zFar <= zNear}
     *                          or {@code left == right}, or {@code bottom == top}.
     * @see Matrix4f#setToFrustum(float, float, float, float, float, float)
     */
    public final void frustumP(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) throws IllegalArgumentException {
        mulP( mat4Tmp1.setToFrustum(left, right, bottom, top, zNear, zFar) );
    }

    //
    // Extra functionality
    //

    /**
     * Set the the {@link #getP() projection matrix} to the perspective/frustum matrix.
     *
     * @param fovy_rad fov angle in radians
     * @param aspect aspect ratio width / height
     * @param zNear
     * @param zFar
     * @throws IllegalArgumentException if {@code zNear <= 0} or {@code zFar <= zNear}
     * @see Matrix4f#setToPerspective(float, float, float, float)
     */
    public final PMVMatrix4f setToPerspective(final float fovy_rad, final float aspect, final float zNear, final float zFar) throws IllegalArgumentException {
        matP.setToPerspective(fovy_rad, aspect, zNear, zFar);
        setProjectionDirty();
        return this;
    }

    /**
     * {@link #mulP(Matrix4f) Multiply} the {@link #getP() projection matrix} with the perspective/frustum matrix.
     *
     * @param fovy_rad fov angle in radians
     * @param aspect aspect ratio width / height
     * @param zNear
     * @param zFar
     * @throws IllegalArgumentException if {@code zNear <= 0} or {@code zFar <= zNear}
     * @see Matrix4f#setToPerspective(float, float, float, float)
     */
    public final PMVMatrix4f perspectiveP(final float fovy_rad, final float aspect, final float zNear, final float zFar) throws IllegalArgumentException {
        mulP( mat4Tmp1.setToPerspective(fovy_rad, aspect, zNear, zFar) );
        return this;
    }

    /**
     * Set the {@link #getMv() modelview matrix}
     * to the eye, object and orientation (camera), i.e. {@link Matrix4f#setToLookAt(Vec3f, Vec3f, Vec3f, Matrix4f)}.
     */
    public final PMVMatrix4f setToLookAtMv(final Vec3f eye, final Vec3f center, final Vec3f up) {
        matMv.setToLookAt(eye, center, up, getTmp2Mat());
        setModelviewDirty();
        return this;
    }

    /**
     * {@link #mulMv(Matrix4f) Multiply} the {@link #getMv() modelview matrix}
     * with the eye, object and orientation (camera), i.e. {@link Matrix4f#setToLookAt(Vec3f, Vec3f, Vec3f, Matrix4f)}.
     */
    public final PMVMatrix4f lookAtMv(final Vec3f eye, final Vec3f center, final Vec3f up) {
        mulMv( mat4Tmp1.setToLookAt(eye, center, up, getTmp2Mat()) );
        return this;
    }

    /**
     * Map object coordinates to window coordinates.
     * <p>
     * Traditional <code>gluProject</code> implementation.
     * </p>
     *
     * @param objPos 3 component object coordinate
     * @param viewport Rect4i viewport
     * @param winPos 3 component window coordinate, the result
     * @return true if successful, otherwise false (z is 1)
     */
    public final boolean mapObjToWin(final Vec3f objPos, final Recti viewport, final Vec3f winPos) {
        return Matrix4f.mapObjToWin(objPos, matMv, matP, viewport, winPos);
    }

    /**
     * Map window coordinates to object coordinates.
     * <p>
     * Traditional <code>gluUnProject</code> implementation.
     * </p>
     *
     * @param winx
     * @param winy
     * @param winz
     * @param viewport Rect4i viewport
     * @param objPos 3 component object coordinate, the result
     * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
     */
    public final boolean mapWinToObj(final float winx, final float winy, final float winz,
                                     final Recti viewport, final Vec3f objPos) {
        if( Matrix4f.mapWinToObj(winx, winy, winz, getPMvi(), viewport, objPos) ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Map window coordinates to object coordinates.
     * <p>
     * Traditional <code>gluUnProject4</code> implementation.
     * </p>
     *
     * @param winx
     * @param winy
     * @param winz
     * @param clipw
     * @param viewport Rect4i viewport
     * @param near
     * @param far
     * @param objPos 4 component object coordinate, the result
     * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
     */
    public boolean mapWinToObj4(final float winx, final float winy, final float winz, final float clipw,
                                final Recti viewport, final float near, final float far, final Vec4f objPos) {
        if( Matrix4f.mapWinToObj4(winx, winy, winz, clipw, getPMvi(), viewport, near, far, objPos) ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Map two window coordinates w/ shared X/Y and distinctive Z
     * to a {@link Ray}. The resulting {@link Ray} maybe used for <i>picking</i>
     * using a {@link AABBox#getRayIntersection(Vec3f, Ray, float, boolean) bounding box}.
     * <p>
     * Notes for picking <i>winz0</i> and <i>winz1</i>:
     * <ul>
     *   <li>see {@link FloatUtil#getZBufferEpsilon(int, float, float)}</li>
     *   <li>see {@link FloatUtil#getZBufferValue(int, float, float, float)}</li>
     *   <li>see {@link FloatUtil#getOrthoWinZ(float, float, float)}</li>
     * </ul>
     * </p>
     * @param winx
     * @param winy
     * @param winz0
     * @param winz1
     * @param viewport
     * @param ray storage for the resulting {@link Ray}
     * @return true if successful, otherwise false (failed to invert matrix, or becomes z is infinity)
     */
    public final boolean mapWinToRay(final float winx, final float winy, final float winz0, final float winz1,
                                     final Recti viewport, final Ray ray) {
        return Matrix4f.mapWinToRay(winx, winy, winz0, winz1, getPMvi(), viewport, ray);
    }

    public StringBuilder toString(StringBuilder sb, final String f) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        final boolean pmvDirty  = 0 != (PREMUL_PMV & dirtyBits);
        final boolean pmvUsed = null != matPMv;

        final boolean pmviDirty  = 0 != (PREMUL_PMVI & dirtyBits);
        final boolean pmviUsed = null != matPMvi;

        final boolean frustumDirty = 0 != (FRUSTUM & dirtyBits);
        final boolean frustumUsed = null != frustum;

        final boolean mviDirty  = 0 != (INVERSE_MODELVIEW & dirtyBits);
        final boolean mviReq = 0 != (INVERSE_MODELVIEW & requestBits);

        final boolean mvitDirty = 0 != (INVERSE_TRANSPOSED_MODELVIEW & dirtyBits);
        final boolean mvitReq = 0 != (INVERSE_TRANSPOSED_MODELVIEW & requestBits);

        final boolean modP = 0 != ( MODIFIED_PROJECTION & modifiedBits );
        final boolean modMv = 0 != ( MODIFIED_MODELVIEW & modifiedBits );
        final boolean modT = 0 != ( MODIFIED_TEXTURE & modifiedBits );
        int count = 3; // P, Mv, T

        sb.append("PMVMatrix4f[modified[P ").append(modP).append(", Mv ").append(modMv).append(", T ").append(modT);
        sb.append("], dirty/used[PMv ").append(pmvDirty).append("/").append(pmvUsed).append(", Pmvi ").append(pmviDirty).append("/").append(pmviUsed).append(", Frustum ").append(frustumDirty).append("/").append(frustumUsed);
        sb.append("], dirty/req[Mvi ").append(mviDirty).append("/").append(mviReq).append(", Mvit ").append(mvitDirty).append("/").append(mvitReq).append("]").append(System.lineSeparator());
        sb.append(", Projection").append(System.lineSeparator());
        matP.toString(sb, null, f);
        sb.append(", Modelview").append(System.lineSeparator());
        matMv.toString(sb, null, f);
        sb.append(", Texture").append(System.lineSeparator());
        matTex.toString(sb, null, f);
        if( null != matPMv ) {
            sb.append(", P * Mv").append(System.lineSeparator());
            matPMv.toString(sb, null, f);
            ++count;
        }
        if( null != matPMvi ) {
            sb.append(", P * Mv").append(System.lineSeparator());
            matPMvi.toString(sb, null, f);
            ++count;
        }
        if( mviReq ) {
            sb.append(", Inverse Modelview").append(System.lineSeparator());
            matMvi.toString(sb, null, f);
            ++count;
        }
        if( mvitReq ) {
            sb.append(", Inverse Transposed Modelview").append(System.lineSeparator());
            matMvit.toString(sb, null, f);
            ++count;
        }
        int tmpCount = 1;
        if( null != mat4Tmp2 ) {
            ++tmpCount;
        }
        sb.append(", matrices "+count+" + "+tmpCount+" temp = "+(count+tmpCount)+"]");
        return sb;
    }

    @Override
    public String toString() {
        return toString(null, "%10.5f").toString();
    }

    /**
     * Returns the modified bits due to mutable operations..
     * <p>
     * A modified bit is set, if the corresponding matrix had been modified by a mutable operation
     * since last {@link #update()} or {@link #getModifiedBits(boolean) getModifiedBits(true)} call.
     * </p>
     * @param clear if true, clears the modified bits, otherwise leaves them untouched.
     *
     * @see #MODIFIED_PROJECTION
     * @see #MODIFIED_MODELVIEW
     * @see #MODIFIED_TEXTURE
     * @see #getDirtyBits()
     * @see #isReqDirty()
     */
    public final int getModifiedBits(final boolean clear) {
        final int r = modifiedBits;
        if(clear) {
            modifiedBits = 0;
        }
        return r;
    }

    /**
     * Returns the dirty bits due to mutable operations,
     * i.e.
     * - {@link #INVERSE_MODELVIEW} (if requested)
     * - {@link #INVERSE_TRANSPOSED_MODELVIEW} (if requested)
     * - {@link #FRUSTUM} (always, cleared via {@link #getFrustum()}
     * <p>
     * A dirty bit is set, if the corresponding matrix had been modified by a mutable operation
     * since last {@link #update()} call and requested in the constructor {@link #PMVMatrix4f(int)}.
     * </p>
     * <p>
     * {@link #update()} clears the dirty state for the matrices and {@link #getFrustum()} for {@link #FRUSTUM}.
     * </p>
     *
     * @see #isReqDirty()
     * @see #INVERSE_MODELVIEW
     * @see #INVERSE_TRANSPOSED_MODELVIEW
     * @see #FRUSTUM
     * @see #PMVMatrix4f(int)
     * @see #getMvi()
     * @see #getMvit()
     * @see #getSyncPMvMvi()
     * @see #getSyncPMvMviMvit()
     * @see #getFrustum()
     */
    public final int getDirtyBits() {
        return dirtyBits;
    }

    /**
     * Returns true if the one of the {@link #getReqBits() requested bits} are are set dirty due to mutable operations,
     * i.e. at least one of
     * - {@link #INVERSE_MODELVIEW}
     * - {@link #INVERSE_TRANSPOSED_MODELVIEW}
     * <p>
     * A dirty bit is set, if the corresponding matrix had been modified by a mutable operation
     * since last {@link #update()} call and requested in the constructor {@link #PMVMatrix4f(int)}.
     * </p>
     * <p>
     * {@link #update()} clears the dirty state for the matrices and {@link #getFrustum()} for {@link #FRUSTUM}.
     * </p>
     *
     * @see #INVERSE_MODELVIEW
     * @see #INVERSE_TRANSPOSED_MODELVIEW
     * @see #PMVMatrix4f(int)
     * @see #getMvi()
     * @see #getMvit()
     * @see #getSyncPMvMvi()
     * @see #getSyncPMvMviMvit()
     */
    public final boolean isReqDirty() {
        return 0 != ( requestBits & dirtyBits );
    }

    /**
     * Sets the {@link #getMv() Modelview (Mv)} matrix dirty and modified,
     * i.e. adds {@link #getReqBits() requested bits} and {@link #MANUAL_BITS} to {@link #getDirtyBits() dirty bits}.
     * @see #isReqDirty()
     */
    public final void setModelviewDirty() {
        dirtyBits |= requestBits | MANUAL_BITS ;
        modifiedBits |= MODIFIED_MODELVIEW;
    }

    /**
     * Sets the {@link #getP() Projection (P)} matrix dirty and modified,
     * i.e. adds {@link #MANUAL_BITS} to {@link #getDirtyBits() dirty bits}.
     */
    public final void setProjectionDirty() {
        dirtyBits |= MANUAL_BITS ;
        modifiedBits |= MODIFIED_PROJECTION;
    }

    /**
     * Sets the {@link #getT() Texture (T)} matrix modified.
     */
    public final void setTextureDirty() {
        modifiedBits |= MODIFIED_TEXTURE;
    }

    /**
     * Returns the request bit mask, which uses bit values equal to the dirty mask
     * and may contain
     * - {@link #INVERSE_MODELVIEW}
     * - {@link #INVERSE_TRANSPOSED_MODELVIEW}
     * <p>
     * The request bit mask is set by in the constructor {@link #PMVMatrix4f(int)}.
     * </p>
     *
     * @see #INVERSE_MODELVIEW
     * @see #INVERSE_TRANSPOSED_MODELVIEW
     * @see #PMVMatrix4f(int)
     * @see #getMvi()
     * @see #getMvit()
     * @see #getSyncPMvMvi()
     * @see #getSyncPMvMviMvit()
     * @see #getFrustum()
     */
    public final int getReqBits() {
        return requestBits;
    }

    /**
     * Returns the pre-multiplied projection x modelview, P x Mv.
     * <p>
     * This {@link Matrix4f} instance should be re-fetched via this method and not locally stored
     * to have it updated from a potential modification of underlying projection and/or modelview matrix.
     * {@link #update()} has no effect on this {@link Matrix4f}.
     * </p>
     * <p>
     * This pre-multipled P x Mv is considered dirty, if its corresponding
     * {@link #getP() P matrix} or {@link #getMv() Mv matrix} has been modified since its last update.
     * </p>
     * @see #update()
     */
    public final Matrix4f getPMv() {
        if( 0 != ( dirtyBits & PREMUL_PMV ) ) {
            if( null == matPMv ) {
                matPMv = new Matrix4f();
            }
            matPMv.mul(matP, matMv);
            dirtyBits &= ~PREMUL_PMV;
        }
        return matPMv;
    }

    /**
     * Returns the pre-multiplied inverse projection x modelview,
     * if {@link Matrix4f#invert(Matrix4f)} succeeded, otherwise `null`.
     * <p>
     * This {@link Matrix4f} instance should be re-fetched via this method and not locally stored
     * to have it updated from a potential modification of underlying projection and/or modelview matrix.
     * {@link #update()} has no effect on this {@link Matrix4f}.
     * </p>
     * <p>
     * This pre-multipled invert(P x Mv) is considered dirty, if its corresponding
     * {@link #getP() P matrix} or {@link #getMv() Mv matrix} has been modified since its last update.
     * </p>
     * @see #update()
     */
    public final Matrix4f getPMvi() {
        if( 0 != ( dirtyBits & PREMUL_PMVI ) ) {
            if( null == matPMvi ) {
                matPMvi = new Matrix4f();
            }
            final Matrix4f mPMv = getPMv();
            matPMviOK = matPMvi.invert(mPMv);
            dirtyBits &= ~PREMUL_PMVI;
        }
        return matPMviOK ? matPMvi : null;
    }

    /**
     * Returns the frustum, derived from projection x modelview.
     * <p>
     * This {@link Frustum} instance should be re-fetched via this method and not locally stored
     * to have it updated from a potential modification of underlying projection and/or modelview matrix.
     * {@link #update()} has no effect on this {@link Frustum}.
     * </p>
     * <p>
     * The {@link Frustum} is considered dirty, if its corresponding
     * {@link #getP() P matrix} or {@link #getMv() Mv matrix} has been modified since its last update.
     * </p>
     * @see #update()
     */
    public final Frustum getFrustum() {
        if( 0 != ( dirtyBits & FRUSTUM ) ) {
            if( null == frustum ) {
                frustum = new Frustum();
            }
            frustum.setFromMat(getPMv());
            dirtyBits &= ~FRUSTUM;
        }
        return frustum;
    }

    /**
     * Update the derived {@link #getMvi() inverse modelview (Mvi)},
     * {@link #getMvit() inverse transposed modelview (Mvit)} matrices
     * <b>if</b> they {@link #isReqDirty() are dirty} <b>and</b>
     * requested via the constructor {@link #PMVMatrix4f(int)}.<br/>
     * Hence updates the following dirty bits.
     * - {@link #INVERSE_MODELVIEW}
     * - {@link #INVERSE_TRANSPOSED_MODELVIEW}
     * <p>
     * The {@link Frustum} is updated only via {@link #getFrustum()} separately.
     * </p>
     * <p>
     * The Mvi and Mvit matrices are considered dirty, if their corresponding
     * {@link #getMv() Mv matrix} has been modified since their last update.
     * </p>
     * <p>
     * Method is automatically called by {@link SyncMatrix4f} and {@link SyncMatrices4f}
     * instances {@link SyncAction} as retrieved by e.g. {@link #getSyncMvit()}.
     * This ensures an automatic update cycle if used with {@link com.jogamp.opengl.GLUniformData}.
     * </p>
     * <p>
     * Method may be called manually in case mutable operations has been called
     * and caller operates on already fetched references, i.e. not calling
     * {@link #getMvi()}, {@link #getMvit()} anymore.
     * </p>
     * <p>
     * Method clears the modified bits like {@link #getModifiedBits(boolean) getModifiedBits(true)},
     * which are set by any mutable operation. The modified bits have no impact
     * on this method, but the return value.
     * </p>
     *
     * @return true if any matrix has been modified since last update call or
     *         if the derived matrices Mvi and Mvit were updated, otherwise false.
     *         In other words, method returns true if any matrix used by the caller must be updated,
     *         e.g. uniforms in a shader program.
     *
     * @see #getModifiedBits(boolean)
     * @see #isReqDirty()
     * @see #INVERSE_MODELVIEW
     * @see #INVERSE_TRANSPOSED_MODELVIEW
     * @see #PMVMatrix4f(int)
     * @see #getMvi()
     * @see #getMvit()
     * @see #getSyncPMvMvi()
     * @see #getSyncPMvMviMvit()
     */
    public final boolean update() {
        return updateImpl(true);
    }

    //
    // private
    //

    private final boolean updateImpl(final boolean clearModBits) {
        boolean mod = 0 != modifiedBits;
        if( clearModBits ) {
            modifiedBits = 0;
        }
        if( 0 != ( requestBits & ( ( dirtyBits & ( INVERSE_MODELVIEW | INVERSE_TRANSPOSED_MODELVIEW ) ) ) ) ) { // only if dirt requested & dirty
            if( !matMvi.invert(matMv) ) {
                throw new RuntimeException("Invalid source Mv matrix, can't compute inverse");
            }
            dirtyBits &= ~INVERSE_MODELVIEW;
            mod = true;
        }
        if( 0 != ( requestBits & ( dirtyBits & INVERSE_TRANSPOSED_MODELVIEW ) ) ) { // only if requested & dirty
            matMvit.transpose(matMvi);
            dirtyBits &= ~INVERSE_TRANSPOSED_MODELVIEW;
            mod = true;
        }
        return mod;
    }

    protected final Matrix4f matP;
    protected final Matrix4f matMv;
    protected final Matrix4f matTex;

    private final Matrix4f matMvi;
    private final Matrix4f matMvit;

    private static final int mP_offset      = 0*16;
    private static final int mMv_offset     = 1*16;
    private final int mMvi_offset;
    private final int mMvit_offset;
    private final int mTex_offset;

    private final float[] matrixStore;

    private final SyncMatrix4f syncP, syncMv, syncT;
    private final SyncMatrix4f syncMvi, syncMvit;
    private final SyncMatrices4f syncP_Mv, syncP_Mv_Mvi, syncP_Mv_Mvi_Mvit;

    protected final Matrix4f mat4Tmp1;
    private Matrix4f mat4Tmp2;

    private int modifiedBits = MODIFIED_ALL;
    private int dirtyBits = 0; // contains the dirty bits, i.e. hinting for update operation
    private final int requestBits; // may contain the requested bits: INVERSE_MODELVIEW | INVERSE_TRANSPOSED_MODELVIEW
    private Matrix4f matPMv;
    private Matrix4f matPMvi;
    private boolean matPMviOK;
    private Frustum frustum;

    private abstract class PMVSyncBuffer implements SyncMatrix4f {
        protected final Matrix4f mat;
        private final FloatBuffer fbuf;

        public PMVSyncBuffer(final Matrix4f m, final FloatBuffer fbuf) {
            this.mat = m;
            this.fbuf = fbuf;
        }

        @Override
        public final Buffer getBuffer() { return fbuf; }

        @Override
        public final SyncBuffer sync() { getAction().sync(); return this; }

        @Override
        public final Buffer getSyncBuffer() { getAction().sync(); return fbuf; }

        @Override
        public final Matrix4f getMatrix() { return mat; }

        @Override
        public final FloatBuffer getSyncFloats() { getAction().sync(); return fbuf; }
    }
    private final class SyncBuffer0 extends PMVSyncBuffer {
        private final SyncAction action = new SyncAction() {
            @Override
            public void sync() { mat.get(matrixStore); }
        };

        public SyncBuffer0(final Matrix4f m, final FloatBuffer fbuf) { super(m, fbuf); }

        @Override
        public SyncAction getAction() { return action; }

    }
    private final class SyncBuffer1 extends PMVSyncBuffer {
        private final int offset;
        private final SyncAction action = new SyncAction() {
            @Override
            public void sync() { mat.get(matrixStore, offset); }
        };

        public SyncBuffer1(final Matrix4f m, final FloatBuffer fbuf, final int offset) {
            super(m, fbuf);
            this.offset = offset;
        }

        @Override
        public SyncAction getAction() { return action; }
    }
    private final class SyncBuffer1U extends PMVSyncBuffer {
        private final int offset;
        private final SyncAction action = new SyncAction() {
            @Override
            public void sync() {
                updateImpl(true);
                mat.get(matrixStore, offset);
            }
        };

        public SyncBuffer1U(final Matrix4f m, final FloatBuffer fbuf, final int offset) {
            super(m, fbuf);
            this.offset = offset;
        }

        @Override
        public SyncAction getAction() { return action; }
    }

    private abstract class PMVSyncBufferN implements SyncMatrices4f {
        protected final Matrix4f[] mats;
        private final FloatBuffer fbuf;

        public PMVSyncBufferN(final Matrix4f[] ms, final FloatBuffer fbuf) {
            this.mats = ms;
            this.fbuf = fbuf;
        }

        @Override
        public final Buffer getBuffer() { return fbuf; }

        @Override
        public final SyncBuffer sync() { getAction().sync(); return this; }

        @Override
        public final Buffer getSyncBuffer() { getAction().sync(); return fbuf; }

        @Override
        public Matrix4f[] getMatrices() { return mats; }

        @Override
        public final FloatBuffer getSyncFloats() { getAction().sync(); return fbuf; }
    }
    private final class SyncBufferN extends PMVSyncBufferN {
        private final int offset;
        private final SyncAction action = new SyncAction() {
            @Override
            public void sync() {
                int ioff = offset;
                for(int i=0; i<mats.length; ++i, ioff+=16) {
                    mats[i].get(matrixStore, ioff);
                }
            }
        };

        public SyncBufferN(final Matrix4f[] ms, final FloatBuffer fbuf, final int offset) {
            super(ms, fbuf);
            this.offset = offset;
        }

        @Override
        public SyncAction getAction() { return action; }
    }
    private final class SyncBufferNU extends PMVSyncBufferN {
        private final int offset;
        private final SyncAction action = new SyncAction() {
            @Override
            public void sync() {
                updateImpl(true);
                int ioff = offset;
                for(int i=0; i<mats.length; ++i, ioff+=16) {
                    mats[i].get(matrixStore, ioff);
                }
            }
        };

        public SyncBufferNU(final Matrix4f[] ms, final FloatBuffer fbuf, final int offset) {
            super(ms, fbuf);
            this.offset = offset;
        }

        @Override
        public SyncAction getAction() { return action; }
    }
}