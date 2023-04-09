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
package com.jogamp.opengl.util;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.GLUniformData;

import jogamp.common.os.PlatformPropsImpl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.Ray;
import com.jogamp.opengl.math.Recti;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.math.Vec4f;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.math.geom.Frustum;

/**
 * PMVMatrix implements a subset of the fixed function pipeline
 * regarding the projection (P), modelview (Mv) and texture (T) matrix operations,
 * which is specified in {@link GLMatrixFunc}.
 * <p>
 * This is the second implementation of `PMVMatrix` using
 * direct {@link Matrix4f}, {@link Vec4f} and {@link Vec3f} math operations instead of `float[]`
 * via {@link com.jogamp.opengl.math.FloatUtil FloatUtil}.
 * </p>
 * <p>
 * PMVMatrix provides the {@link #getMviMat() inverse modelview matrix (Mvi)} and
 * {@link #getMvitMat() inverse transposed modelview matrix (Mvit)}.
 * {@link Frustum} is also provided by {@link #getFrustum()}.
 *
 * To keep these derived values synchronized after mutable Mv operations like {@link #glRotatef(float, float, float, float) glRotatef(..)}
 * in {@link #glMatrixMode(int) glMatrixMode}({@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW}),
 * users have to call {@link #update()} before using Mvi and Mvit.
 * </p>
 * <p>
 * All matrices are provided in column-major order,
 * as specified in the OpenGL fixed function pipeline, i.e. compatibility profile.
 * See {@link Matrix4f}.
 * </p>
 * <p>
 * PMVMatrix can supplement {@link GL2ES2} applications w/ the
 * lack of the described matrix functionality.
 * </p>
 * <a name="storageDetails"><h5>Matrix storage details</h5></a>
 * <p>
 * The {@link SyncBuffer} abstraction is provided, e.g. {@link #getSyncPMvMviMat()},
 * to synchronize the respective {@link Matrix4f matrices} with the `float[]` backing store.
 * The latter is represents the data to {@link GLUniformData} via its {@link FloatBuffer}s, see {@link SyncBuffer#getBuffer()},
 * and is pushed to the GPU eventually.
 *
 * {@link SyncBuffer}'s {@link SyncAction} is called by {@link GLUniformData#getBuffer()},
 * i.e. before the data is pushed to the GPU.
 *
 * The provided {@link SyncAction} ensures that the {@link Matrix4f matrices data}
 * gets copied into the `float[]` backing store.
 *
 * PMVMatrix provides two specializations of {@link SyncBuffer}, {@link SyncMatrix4f} for single {@link Matrix4f} mappings
 * and {@link SyncMatrices4f} for multiple {@link Matrix4f} mappings.
 *
 * They can be feed directly to instantiate a {@link GLUniformData} object via e.g. {@link GLUniformData#GLUniformData(String, int, int, SyncBuffer)}.
 * </p>
 * <p>
 * All matrices are backed up by a common primitive float-array for performance considerations
 * and are a {@link Buffers#slice2Float(float[], int, int) sliced} representation of it.
 * </p>
 * <p>
 * <b>Note:</b>
 * <ul>
 *   <li>The matrix is a {@link Buffers#slice2Float(float[], int, int) sliced part } of a host matrix and it's start position has been {@link FloatBuffer#mark() marked}.</li>
 *   <li>Use {@link FloatBuffer#reset() reset()} to rewind it to it's start position after relative operations, like {@link FloatBuffer#get() get()}.</li>
 *   <li>If using absolute operations like {@link FloatBuffer#get(int) get(int)}, use it's {@link FloatBuffer#reset() reset} {@link FloatBuffer#position() position} as it's offset.</li>
 * </ul>
 * </p>
 */
public final class PMVMatrix implements GLMatrixFunc {

    /** Bit value stating a modified {@link #getPMat() projection matrix (P)}, since last {@link #update()} call. */
    public static final int MODIFIED_PROJECTION                 = 1 << 0;
    /** Bit value stating a modified {@link #getMvMat() modelview matrix (Mv)}, since last {@link #update()} call. */
    public static final int MODIFIED_MODELVIEW                  = 1 << 1;
    /** Bit value stating a modified {@link #getTMat() texture matrix (T)}, since last {@link #update()} call. */
    public static final int MODIFIED_TEXTURE                    = 1 << 2;
    /** Bit value stating all is modified */
    public static final int MODIFIED_ALL                        = MODIFIED_PROJECTION | MODIFIED_MODELVIEW | MODIFIED_TEXTURE ;

    /** Bit value for {@link #getMviMat() inverse modelview matrix (Mvi)}, updated via {@link #update()}. */
    public static final int INVERSE_MODELVIEW             = 1 << 1;
    /** Bit value for {@link #getMvitMat() inverse transposed modelview matrix (Mvit)}, updated via {@link #update()}. */
    public static final int INVERSE_TRANSPOSED_MODELVIEW  = 1 << 2;
    /** Bit value for {@link #getFrustum() frustum} and updated by {@link #getFrustum()}. */
    public static final int FRUSTUM                       = 1 << 3;
    /** Bit value for {@link #getPMvMat() pre-multiplied P * Mv}, updated by {@link #getPMvMat()}. */
    public static final int PREMUL_PMV                   = 1 << 4;
    /** Bit value for {@link #getPMviMat() pre-multiplied invert(P * Mv)}, updated by {@link #getPMviMat()}. */
    public static final int PREMUL_PMVI                  = 1 << 5;
    /** Manual bits not covered by {@link #update()} but {@link #getFrustum()}, {@link #FRUSTUM}, {@link #getPMvMat()}, {@link #PREMUL_PMV}, {@link #getPMviMat()}, {@link #PREMUL_PMVI}, etc. */
    public static final int MANUAL_BITS                   = FRUSTUM | PREMUL_PMV | PREMUL_PMVI;

    /**
     * @param matrixModeName One of {@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW}, {@link GLMatrixFunc#GL_PROJECTION GL_PROJECTION} or {@link GL#GL_TEXTURE GL_TEXTURE}
     * @return true if the given matrix-mode name is valid, otherwise false.
     */
    public static final boolean isMatrixModeName(final int matrixModeName) {
        switch(matrixModeName) {
            case GL_MODELVIEW_MATRIX:
            case GL_PROJECTION_MATRIX:
            case GL_TEXTURE_MATRIX:
                return true;
        }
        return false;
    }

    /**
     * @param matrixModeName One of {@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW}, {@link GLMatrixFunc#GL_PROJECTION GL_PROJECTION} or {@link GL#GL_TEXTURE GL_TEXTURE}
     * @return The corresponding matrix-get name, one of {@link GLMatrixFunc#GL_MODELVIEW_MATRIX GL_MODELVIEW_MATRIX}, {@link GLMatrixFunc#GL_PROJECTION_MATRIX GL_PROJECTION_MATRIX} or {@link GLMatrixFunc#GL_TEXTURE_MATRIX GL_TEXTURE_MATRIX}
     */
    public static final int matrixModeName2MatrixGetName(final int matrixModeName) {
        switch(matrixModeName) {
            case GL_MODELVIEW:
                return GL_MODELVIEW_MATRIX;
            case GL_PROJECTION:
                return GL_PROJECTION_MATRIX;
            case GL.GL_TEXTURE:
                return GL_TEXTURE_MATRIX;
            default:
              throw new GLException("unsupported matrixName: "+matrixModeName);
        }
    }

    /**
     * @param matrixGetName One of {@link GLMatrixFunc#GL_MODELVIEW_MATRIX GL_MODELVIEW_MATRIX}, {@link GLMatrixFunc#GL_PROJECTION_MATRIX GL_PROJECTION_MATRIX} or {@link GLMatrixFunc#GL_TEXTURE_MATRIX GL_TEXTURE_MATRIX}
     * @return true if the given matrix-get name is valid, otherwise false.
     */
    public static final boolean isMatrixGetName(final int matrixGetName) {
        switch(matrixGetName) {
            case GL_MATRIX_MODE:
            case GL_MODELVIEW_MATRIX:
            case GL_PROJECTION_MATRIX:
            case GL_TEXTURE_MATRIX:
                return true;
        }
        return false;
    }

    /**
     * @param matrixGetName One of  {@link GLMatrixFunc#GL_MODELVIEW_MATRIX GL_MODELVIEW_MATRIX}, {@link GLMatrixFunc#GL_PROJECTION_MATRIX GL_PROJECTION_MATRIX} or {@link GLMatrixFunc#GL_TEXTURE_MATRIX GL_TEXTURE_MATRIX}
     * @return The corresponding matrix-mode name, one of {@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW}, {@link GLMatrixFunc#GL_PROJECTION GL_PROJECTION} or {@link GL#GL_TEXTURE GL_TEXTURE}
     */
    public static final int matrixGetName2MatrixModeName(final int matrixGetName) {
        switch(matrixGetName) {
            case GL_MODELVIEW_MATRIX:
                return GL_MODELVIEW;
            case GL_PROJECTION_MATRIX:
                return GL_PROJECTION;
            case GL_TEXTURE_MATRIX:
                return GL.GL_TEXTURE;
            default:
              throw new GLException("unsupported matrixGetName: "+matrixGetName);
        }
    }

    /**
     * Creates an instance of PMVMatrix.
     * <p>
     * This constructor only sets up an instance w/o additional {@link #INVERSE_MODELVIEW} or {@link #INVERSE_TRANSPOSED_MODELVIEW}.
     * </p>
     * <p>
     * Implementation uses non-direct non-NIO Buffers with guaranteed backing array,
     * which are synchronized to the actual Matrix4f instances.
     * This allows faster access in Java computation.
     * </p>
     * @see #PMVMatrix(int)
     */
    public PMVMatrix() {
        this(0);
    }

    /**
     * Creates an instance of PMVMatrix.
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
    public PMVMatrix(final int derivedMatrices) {
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
        bufP        = Buffers.slice2Float(matrixStore,  mP_offset,    1*16);  // P
        syncP       = new SyncBuffer0(matP,    bufP);   // mP_offset

        bufMv       = Buffers.slice2Float(matrixStore,  mMv_offset,   1*16);  // Mv
        syncMv      = new SyncBuffer1(matMv,   bufMv,   mMv_offset);

        bufP_Mv     = Buffers.slice2Float(matrixStore,  mP_offset,    2*16);  // P + Mv
        syncP_Mv    = new SyncBufferN(new Matrix4f[]  { matP, matMv }, bufP_Mv, mP_offset);

        bufTex      = Buffers.slice2Float(matrixStore,  mTex_offset,  1*16);  // T
        syncT       = new SyncBuffer1(matTex,  bufTex,  mTex_offset);

        if( null != matMvi ) {
            bufMvi       = Buffers.slice2Float(matrixStore,  mMvi_offset,  1*16);  // Mvi
            bufP_Mv_Mvi  = Buffers.slice2Float(matrixStore,  mP_offset,    3*16);  // P + Mv + Mvi
            syncMvi      = new SyncBuffer1U(matMvi,  bufMvi,  mMvi_offset);
            syncP_Mv_Mvi = new SyncBufferNU(new Matrix4f[] { matP, matMv, matMvi }, bufP_Mv_Mvi, mP_offset);
        } else {
            bufMvi  = null;
            bufP_Mv_Mvi = null;
            syncMvi     = null;
            syncP_Mv_Mvi = null;
        }
        if( null != matMvit ) {
            bufMvit           = Buffers.slice2Float(matrixStore,  mMvit_offset, 1*16);  //          Mvit
            bufP_Mv_Mvi_Mvit  = Buffers.slice2Float(matrixStore,  mP_offset,    4*16);  // P + Mv + Mvi + Mvit
            syncMvit          = new SyncBuffer1U(matMvit, bufMvit, mMvit_offset);
            syncP_Mv_Mvi_Mvit = new SyncBufferNU(new Matrix4f[] { matP, matMv, matMvi, matMvit }, bufP_Mv_Mvi_Mvit, mP_offset);
        } else {
            bufMvit = null;
            bufP_Mv_Mvi_Mvit = null;
            syncMvit = null;
            syncP_Mv_Mvi_Mvit = null;
        }

        mat4Tmp1       = new Matrix4f();
        vec3Tmp1       = new Vec3f();
        vec4Tmp1       = new Vec4f();

        mat4Tmp2 = null; // on demand
        matPMv = null; // on demand
        matPMvi = null; // on demand
        matPMviOK = false;
        frustum = null; // on demand

        reset();
    }

    /**
     * Issues {@link #glLoadIdentity()} on all matrices,
     * i.e. {@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW}, {@link GLMatrixFunc#GL_PROJECTION GL_PROJECTION} or {@link GL#GL_TEXTURE GL_TEXTURE}
     * and resets all internal states.
     *
     * Leaves {@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW} the active matrix mode.
     */
    public final void reset() {
        matP.loadIdentity();
        matMv.loadIdentity();
        matTex.loadIdentity();

        modifiedBits = MODIFIED_ALL;
        dirtyBits = requestBits | MANUAL_BITS;
        matrixMode = GL_MODELVIEW;
    }

    /** Returns the current matrix-mode, one of {@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW}, {@link GLMatrixFunc#GL_PROJECTION GL_PROJECTION} or {@link GL#GL_TEXTURE GL_TEXTURE}. */
    public final int  glGetMatrixMode() {
        return matrixMode;
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
    private final Matrix4f getTmp2Mat() {
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
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final Matrix4f getTMat() {
        return matTex;
    }

    /**
     * Returns the {@link SyncMatrix} of {@link GLMatrixFunc#GL_TEXTURE_MATRIX texture matrix} (T).
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final SyncMatrix4f getSyncTMat() {
        return syncT;
    }

    /**
     * Returns the {@link GLMatrixFunc#GL_PROJECTION_MATRIX projection matrix} (P).
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final Matrix4f getPMat() {
        return matP;
    }

    /**
     * Returns the {@link SyncMatrix} of {@link GLMatrixFunc#GL_PROJECTION_MATRIX projection matrix} (P).
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final SyncMatrix4f getSyncPMat() {
        return syncP;
    }

    /**
     * Returns the {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mv).
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final Matrix4f getMvMat() {
        return matMv;
    }

    /**
     * Returns the {@link SyncMatrix} of {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mv).
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final SyncMatrix4f getSyncMvMat() {
        return syncMv;
    }

    /**
     * Returns {@link SyncMatrices4f} of 2 matrices within one FloatBuffer: {@link #getPMat() P} and {@link #getMvMat() Mv}.
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final SyncMatrices4f getSyncPMvMat() {
        return syncP_Mv;
    }

    /**
     * Returns the inverse {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mvi) if requested.
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @throws IllegalArgumentException if {@link #INVERSE_MODELVIEW} has not been requested in ctor {@link #PMVMatrix(int)}.
     */
    public final Matrix4f getMviMat() {
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
     * @throws IllegalArgumentException if {@link #INVERSE_MODELVIEW} has not been requested in ctor {@link #PMVMatrix(int)}.
     */
    public final SyncMatrix4f getSyncMviMat() {
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
     * @throws IllegalArgumentException if {@link #INVERSE_TRANSPOSED_MODELVIEW} has not been requested in ctor {@link #PMVMatrix(int)}.
     */
    public final Matrix4f getMvitMat() {
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
     * @throws IllegalArgumentException if {@link #INVERSE_TRANSPOSED_MODELVIEW} has not been requested in ctor {@link #PMVMatrix(int)}.
     */
    public final SyncMatrix4f getSyncMvitMat() {
        if( 0 == ( INVERSE_TRANSPOSED_MODELVIEW & requestBits ) ) {
            throw new IllegalArgumentException("Not requested in ctor");
        }
        return syncMvit;
    }

    /**
     * Returns {@link SyncMatrices4f} of 3 matrices within one FloatBuffer: {@link #getPMat() P}, {@link #getMvMat() Mv} and {@link #getMviMat() Mvi} if requested.
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @throws IllegalArgumentException if {@link #INVERSE_MODELVIEW} has not been requested in ctor {@link #PMVMatrix(int)}.
     */
    public final SyncMatrices4f getSyncPMvMviMat() {
        if( 0 == ( INVERSE_MODELVIEW & requestBits ) ) {
            throw new IllegalArgumentException("Not requested in ctor");
        }
        return syncP_Mv_Mvi;
    }

    /**
     * Returns {@link SyncMatrices4f} of 4 matrices within one FloatBuffer: {@link #getPMat() P}, {@link #getMvMat() Mv}, {@link #getMviMat() Mvi} and {@link #getMvitMat() Mvit} if requested.
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @throws IllegalArgumentException if {@link #INVERSE_TRANSPOSED_MODELVIEW} has not been requested in ctor {@link #PMVMatrix(int)}.
     */
    public final SyncMatrices4f getSyncPMvMviMvitMat() {
        if( 0 == ( INVERSE_TRANSPOSED_MODELVIEW & requestBits ) ) {
            throw new IllegalArgumentException("Not requested in ctor");
        }
        return syncP_Mv_Mvi_Mvit;
    }

    /**
     * @return the matrix of the current matrix-mode
     */
    public final Matrix4f getCurrentMat() {
        return getMat(matrixMode);
    }

    /**
     * @param matrixName Either a matrix-get-name, i.e.
     *                   {@link GLMatrixFunc#GL_MODELVIEW_MATRIX GL_MODELVIEW_MATRIX}, {@link GLMatrixFunc#GL_PROJECTION_MATRIX GL_PROJECTION_MATRIX} or {@link GLMatrixFunc#GL_TEXTURE_MATRIX GL_TEXTURE_MATRIX},
     *                   or a matrix-mode-name, i.e.
     *                   {@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW}, {@link GLMatrixFunc#GL_PROJECTION GL_PROJECTION} or {@link GL#GL_TEXTURE GL_TEXTURE}
     * @return the named matrix, not a copy!
     */
    public final Matrix4f getMat(final int matrixName) {
        switch(matrixName) {
            case GL_MODELVIEW_MATRIX:
            case GL_MODELVIEW:
                return matMv;
            case GL_PROJECTION_MATRIX:
            case GL_PROJECTION:
                return matP;
            case GL_TEXTURE_MATRIX:
            case GL.GL_TEXTURE:
                return matTex;
            default:
              throw new GLException("unsupported matrixName: "+matrixName);
        }
    }

    //
    // Basic Matrix4f, Vec3f and Vec4f operations similar to GLMatrixFunc
    //

    /**
     * Multiplies the {@link #getPMat() P} and {@link #getMvMat() Mv} matrix, i.e.
     * <pre>
     *    result = P x Mv
     * </pre>
     * @param result 4x4 matrix storage for result
     * @return given result matrix for chaining
     */
    public final Matrix4f mulPMvMat(final Matrix4f result) {
        return result.mul(matP, matMv);
    }

    /**
     * Multiplies the {@link #getMvMat() Mv} and {@link #getPMat() P} matrix, i.e.
     * <pre>
     *    result = Mv x P
     * </pre>
     * @param result 4x4 matrix storage for result
     * @return given result matrix for chaining
     */
    public final Matrix4f mulMvPMat(final Matrix4f result) {
        return result.mul(matMv, matP);
    }

    /**
     * v_out = Mv * v_in
     * @param v_in input vector
     * @param v_out output vector
     * @return given result vector for chaining
     */
    public final Vec4f mulMvMatVec4f(final Vec4f v_in, final Vec4f v_out) {
        return matMv.mulVec4f(v_in, v_out);
    }

    /**
     * v_out = Mv * v_in
     *
     * Affine 3f-vector transformation by 4x4 matrix, see {@link Matrix4f#mulVec3f(Vec3f, Vec3f)}.
     *
     * @param v_in input vector
     * @param v_out output vector
     * @return given result vector for chaining
     */
    public final Vec3f mulMvMatVec3f(final Vec3f v_in, final Vec3f v_out) {
        return matMv.mulVec3f(v_in, v_out);
    }

    /**
     * v_out = P * v_in
     * @param v_in input vector
     * @param v_out output vector
     * @return given result vector for chaining
     */
    public final Vec4f mulPMatVec4f(final Vec4f v_in, final Vec4f v_out) {
        return matP.mulVec4f(v_in, v_out);
    }

    /**
     * v_out = P * v_in
     *
     * Affine 3f-vector transformation by 4x4 matrix, see {@link Matrix4f#mulVec3f(Vec3f, Vec3f)}.
     *
     * @param v_in float[3] input vector
     * @param v_out float[3] output vector
     */
    public final Vec3f mulPMatVec3f(final Vec3f v_in, final Vec3f v_out) {
        return matP.mulVec3f(v_in, v_out);
    }

    /**
     * v_out = P * Mv * v_in
     * @param v_in float[4] input vector
     * @param v_out float[4] output vector
     */
    public final Vec4f mulPMvMatVec4f(final Vec4f v_in, final Vec4f v_out) {
        return matP.mulVec4f( matMv.mulVec4f( v_in, vec4Tmp1 ), v_out );
    }

    /**
     * v_out = P * Mv * v_in
     *
     * Affine 3f-vector transformation by 4x4 matrix, see {@link Matrix4f#mulVec3f(Vec3f, Vec3f)}.
     *
     * @param v_in float[3] input vector
     * @param v_out float[3] output vector
     */
    public final Vec3f mulPMvMatVec3f(final Vec3f v_in, final Vec3f v_out) {
        return matP.mulVec3f( matMv.mulVec3f( v_in, vec3Tmp1 ), v_out );
    }

    //
    // GLMatrixFunc implementation
    //

    @Override
    public final void glMatrixMode(final int matrixName) {
        switch(matrixName) {
            case GL_MODELVIEW:
            case GL_PROJECTION:
            case GL.GL_TEXTURE:
                break;
            default:
              throw new GLException("unsupported matrixName: "+matrixName);
        }
        matrixMode = matrixName;
    }

    @Override
    public final void glGetFloatv(final int matrixGetName, final FloatBuffer params) {
        final int pos = params.position();
        if(matrixGetName==GL_MATRIX_MODE) {
            params.put(matrixMode);
        } else {
            getMat(matrixGetName).get(params); // matrix -> params
        }
        params.position(pos);
    }

    @Override
    public final void glGetFloatv(final int matrixGetName, final float[] params, final int params_offset) {
        if(matrixGetName==GL_MATRIX_MODE) {
            params[params_offset]=matrixMode;
        } else {
            getMat(matrixGetName).get(params, params_offset); // matrix -> params
        }
    }

    @Override
    public final void glGetIntegerv(final int pname, final IntBuffer params) {
        final int pos = params.position();
        if(pname==GL_MATRIX_MODE) {
            params.put(matrixMode);
        } else {
            throw new GLException("unsupported pname: "+pname);
        }
        params.position(pos);
    }

    @Override
    public final void glGetIntegerv(final int pname, final int[] params, final int params_offset) {
        if(pname==GL_MATRIX_MODE) {
            params[params_offset]=matrixMode;
        } else {
            throw new GLException("unsupported pname: "+pname);
        }
    }

    @Override
    public final void glLoadMatrixf(final float[] values, final int offset) {
        if(matrixMode==GL_MODELVIEW) {
            matMv.load(values, offset);
            dirtyBits |= requestBits | MANUAL_BITS ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.load(values, offset);
            dirtyBits |= MANUAL_BITS ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matTex.load(values, offset);
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    @Override
    public final void glLoadMatrixf(final java.nio.FloatBuffer m) {
        final int spos = m.position();
        if(matrixMode==GL_MODELVIEW) {
            matMv.load(m);
            dirtyBits |= requestBits | MANUAL_BITS ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.load(m);
            dirtyBits |= MANUAL_BITS ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matTex.load(m);
            modifiedBits |= MODIFIED_TEXTURE;
        }
        m.position(spos);
    }

    /**
     * Load the current matrix with the values of the given {@link Matrix4f}.
     * <p>
     * Extension to {@link GLMatrixFunc}.
     * </p>
     */
    public final void glLoadMatrixf(final Matrix4f m) {
        if(matrixMode==GL_MODELVIEW) {
            matMv.load(m);
            dirtyBits |= requestBits | MANUAL_BITS ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.load(m);
            dirtyBits |= MANUAL_BITS ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matTex.load(m);
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    /**
     * Load the current matrix with the values of the given {@link Quaternion}'s rotation {@link Matrix4f#setToRotation(Quaternion) matrix representation}.
     * <p>
     * Extension to {@link GLMatrixFunc}.
     * </p>
     */
    public final void glLoadMatrix(final Quaternion quat) {
        if(matrixMode==GL_MODELVIEW) {
            matMv.setToRotation(quat);
            dirtyBits |= requestBits | MANUAL_BITS ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.setToRotation(quat);
            dirtyBits |= MANUAL_BITS ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matTex.setToRotation(quat);
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    @Override
    public final void glPopMatrix() {
        if(matrixMode==GL_MODELVIEW) {
            matMv.pop();
            dirtyBits |= requestBits | MANUAL_BITS ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.pop();
            dirtyBits |= MANUAL_BITS ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matTex.pop();
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    @Override
    public final void glPushMatrix() {
        if(matrixMode==GL_MODELVIEW) {
            matMv.push();
        } else if(matrixMode==GL_PROJECTION) {
            matP.push();
        } else if(matrixMode==GL.GL_TEXTURE) {
            matTex.push();
        }
    }

    @Override
    public final void glLoadIdentity() {
        if(matrixMode==GL_MODELVIEW) {
            matMv.loadIdentity();
            dirtyBits |= requestBits | MANUAL_BITS ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.loadIdentity();
            dirtyBits |= MANUAL_BITS ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matTex.loadIdentity();
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    @Override
    public final void glMultMatrixf(final FloatBuffer m) {
        final int spos = m.position();
        if(matrixMode==GL_MODELVIEW) {
            matMv.mul( mat4Tmp1.load( m ) );
            dirtyBits |= requestBits | MANUAL_BITS ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.mul( mat4Tmp1.load( m ) );
            dirtyBits |= MANUAL_BITS ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matTex.mul( mat4Tmp1.load( m ) );
            modifiedBits |= MODIFIED_TEXTURE;
        }
        m.position(spos);
    }

    @Override
    public final void glMultMatrixf(final float[] m, final int m_offset) {
        if(matrixMode==GL_MODELVIEW) {
            matMv.mul( mat4Tmp1.load( m, m_offset ) );
            dirtyBits |= requestBits | MANUAL_BITS ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.mul( mat4Tmp1.load( m, m_offset ) );
            dirtyBits |= MANUAL_BITS ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matTex.mul( mat4Tmp1.load( m, m_offset ) );
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    /**
     * Multiply the current matrix: [c] = [c] x [m]
     * <p>
     * Extension to {@link GLMatrixFunc}.
     * </p>
     * @param m the right hand Matrix4f
     * @return this instance of chaining
     */
    public final PMVMatrix glMultMatrixf(final Matrix4f m) {
        if(matrixMode==GL_MODELVIEW) {
            matMv.mul( m );
            dirtyBits |= requestBits | MANUAL_BITS ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.mul( m );
            dirtyBits |= MANUAL_BITS ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matTex.mul( m );
            modifiedBits |= MODIFIED_TEXTURE;
        }
        return this;
    }

    @Override
    public final void glTranslatef(final float x, final float y, final float z) {
        glMultMatrixf( mat4Tmp1.setToTranslation(x, y, z) );
    }

    /**
     * Translate the current matrix.
     * <p>
     * Extension to {@link GLMatrixFunc}.
     * </p>
     * @param t translation vec3
     * @return this instance of chaining
     */
    public final PMVMatrix glTranslatef(final Vec3f t) {
        return glMultMatrixf( mat4Tmp1.setToTranslation(t) );
    }

    @Override
    public final void glScalef(final float x, final float y, final float z) {
        glMultMatrixf( mat4Tmp1.setToScale(x, y, z) );
    }

    /**
     * Scale the current matrix.
     * <p>
     * Extension to {@link GLMatrixFunc}.
     * </p>
     * @param s scale vec4f
     * @return this instance of chaining
     */
    public final PMVMatrix glScalef(final Vec3f s) {
        return glMultMatrixf( mat4Tmp1.setToScale(s) );
    }

    @Override
    public final void glRotatef(final float ang_deg, final float x, final float y, final float z) {
        glMultMatrixf( mat4Tmp1.setToRotationAxis(FloatUtil.adegToRad(ang_deg), x, y, z) );
    }

    /**
     * Rotate the current matrix by the given axis and angle in radians.
     * <p>
     * Consider using {@link #glRotate(Quaternion)}
     * </p>
     * <p>
     * Extension to {@link GLMatrixFunc}.
     * </p>
     * @param ang_rad angle in radians
     * @param axis rotation axis
     * @return this instance of chaining
     * @see #glRotate(Quaternion)
     */
    public final PMVMatrix glRotatef(final float ang_rad, final Vec3f axis) {
        return glMultMatrixf( mat4Tmp1.setToRotationAxis(ang_rad, axis) );
    }

    /**
     * Rotate the current matrix with the given {@link Quaternion}'s rotation {@link Matrix4f#setToRotation(Quaternion) matrix representation}.
     * <p>
     * Extension to {@link GLMatrixFunc}.
     * </p>
     * @param quat the {@link Quaternion}
     * @return this instance of chaining
     */
    public final PMVMatrix glRotate(final Quaternion quat) {
        return glMultMatrixf( mat4Tmp1.setToRotation(quat) );
    }

    @Override
    public final void glOrthof(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) {
        glMultMatrixf( mat4Tmp1.setToOrtho(left, right, bottom, top, zNear, zFar) );
    }

    /**
     * {@inheritDoc}
     *
     * @throws GLException if {@code zNear <= 0} or {@code zFar <= zNear}
     *                     or {@code left == right}, or {@code bottom == top}.
     * @see Matrix4f#setToFrustum(float, float, float, float, float, float)
     */
    @Override
    public final void glFrustumf(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) throws GLException {
        glMultMatrixf( mat4Tmp1.setToFrustum(left, right, bottom, top, zNear, zFar) );
    }

    //
    // Extra functionality
    //

    /**
     * {@link #glMultMatrixf(FloatBuffer) Multiply} the {@link #glGetMatrixMode() current matrix} with the perspective/frustum matrix.
     *
     * @param fovy_deg fov angle in degrees
     * @param aspect aspect ratio width / height
     * @param zNear
     * @param zFar
     * @throws GLException if {@code zNear <= 0} or {@code zFar <= zNear}
     * @see Matrix4f#setToPerspective(float, float, float, float)
     */
    public final void gluPerspective(final float fovy_deg, final float aspect, final float zNear, final float zFar) throws GLException {
         glMultMatrixf( mat4Tmp1.setToPerspective(FloatUtil.adegToRad(fovy_deg), aspect, zNear, zFar) );
    }

    /**
     * {@link #glMultMatrixf(FloatBuffer) Multiply} and {@link #glTranslatef(float, float, float) translate} the {@link #glGetMatrixMode() current matrix}
     * with the eye, object and orientation.
     */
    public final void gluLookAt(final Vec3f eye, final Vec3f center, final Vec3f up) {
        glMultMatrixf( mat4Tmp1.setToLookAt(eye, center, up, getTmp2Mat()) );
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
    public final boolean gluProject(final Vec3f objPos, final Recti viewport, final Vec3f winPos ) {
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
    public final boolean gluUnProject(final float winx, final float winy, final float winz,
                                      final Recti viewport, final Vec3f objPos) {
        if( Matrix4f.mapWinToObj(winx, winy, winz, getPMviMat(), viewport, objPos) ) {
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
    public boolean gluUnProject4(final float winx, final float winy, final float winz, final float clipw,
                                 final Recti viewport,
                                 final float near, final float far,
                                 final Vec4f objPos) {
        if( Matrix4f.mapWinToObj4(winx, winy, winz, clipw, getPMviMat(), viewport, near, far, objPos) ) {
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
    public final boolean gluUnProjectRay(final float winx, final float winy, final float winz0, final float winz1,
                                         final Recti viewport, final Ray ray) {
        return Matrix4f.mapWinToRay(winx, winy, winz0, winz1, getPMviMat(), viewport, ray);
    }

    /**
     * Make given matrix the <i>pick</i> matrix based on given parameters.
     * <p>
     * Traditional <code>gluPickMatrix</code> implementation.
     * </p>
     * <p>
     * See {@link Matrix4f#setToPick(float, float, float, float, Recti, int, Matrix4f) for details.
     * </p>
     * @param x the center x-component of a picking region in window coordinates
     * @param y the center y-component of a picking region in window coordinates
     * @param deltaX the width of the picking region in window coordinates.
     * @param deltaY the height of the picking region in window coordinates.
     * @param viewport Rect4i viewport vector
     */
    public final void gluPickMatrix(final float x, final float y,
                                    final float deltaX, final float deltaY, final Recti viewport) {
        if( null != mat4Tmp1.setToPick(x, y, deltaX, deltaY, viewport, getTmp2Mat()) ) {
            glMultMatrixf( mat4Tmp1 );
        }
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

        sb.append("PMVMatrix[modified[P ").append(modP).append(", Mv ").append(modMv).append(", T ").append(modT);
        sb.append("], dirty/used[PMv ").append(pmvDirty).append("/").append(pmvUsed).append(", Pmvi ").append(pmviDirty).append("/").append(pmviUsed).append(", Frustum ").append(frustumDirty).append("/").append(frustumUsed);
        sb.append("], dirty/req[Mvi ").append(mviDirty).append("/").append(mviReq).append(", Mvit ").append(mvitDirty).append("/").append(mvitReq).append("]").append(PlatformPropsImpl.NEWLINE);
        sb.append(", Projection").append(PlatformPropsImpl.NEWLINE);
        matP.toString(sb, null, f);
        sb.append(", Modelview").append(PlatformPropsImpl.NEWLINE);
        matMv.toString(sb, null, f);
        sb.append(", Texture").append(PlatformPropsImpl.NEWLINE);
        matTex.toString(sb, null, f);
        if( null != matPMv ) {
            sb.append(", P * Mv").append(PlatformPropsImpl.NEWLINE);
            matPMv.toString(sb, null, f);
            ++count;
        }
        if( null != matPMvi ) {
            sb.append(", P * Mv").append(PlatformPropsImpl.NEWLINE);
            matPMvi.toString(sb, null, f);
            ++count;
        }
        if( mviReq ) {
            sb.append(", Inverse Modelview").append(PlatformPropsImpl.NEWLINE);
            matMvi.toString(sb, null, f);
            ++count;
        }
        if( mvitReq ) {
            sb.append(", Inverse Transposed Modelview").append(PlatformPropsImpl.NEWLINE);
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
     * since last {@link #update()} call and requested in the constructor {@link #PMVMatrix(int)}.
     * </p>
     * <p>
     * {@link #update()} clears the dirty state for the matrices and {@link #getFrustum()} for {@link #FRUSTUM}.
     * </p>
     *
     * @see #isReqDirty()
     * @see #INVERSE_MODELVIEW
     * @see #INVERSE_TRANSPOSED_MODELVIEW
     * @see #FRUSTUM
     * @see #PMVMatrix(int)
     * @see #getMviMat()
     * @see #getMvitMat()
     * @see #getSyncPMvMviMat()
     * @see #getSyncPMvMviMvitMat()
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
     * since last {@link #update()} call and requested in the constructor {@link #PMVMatrix(int)}.
     * </p>
     * <p>
     * {@link #update()} clears the dirty state for the matrices and {@link #getFrustum()} for {@link #FRUSTUM}.
     * </p>
     *
     * @see #INVERSE_MODELVIEW
     * @see #INVERSE_TRANSPOSED_MODELVIEW
     * @see #PMVMatrix(int)
     * @see #getMviMat()
     * @see #getMvitMat()
     * @see #getSyncPMvMviMat()
     * @see #getSyncPMvMviMvitMat()
     */
    public final boolean isReqDirty() {
        return 0 != ( requestBits & dirtyBits );
    }

    /**
     * Returns the request bit mask, which uses bit values equal to the dirty mask
     * and may contain
     * - {@link #INVERSE_MODELVIEW}
     * - {@link #INVERSE_TRANSPOSED_MODELVIEW}
     * <p>
     * The request bit mask is set by in the constructor {@link #PMVMatrix(int)}.
     * </p>
     *
     * @see #INVERSE_MODELVIEW
     * @see #INVERSE_TRANSPOSED_MODELVIEW
     * @see #PMVMatrix(int)
     * @see #getMviMat()
     * @see #getMvitMat()
     * @see #getSyncPMvMviMat()
     * @see #getSyncPMvMviMvitMat()
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
     * {@link #getPMat() P matrix} or {@link #getMvMat() Mv matrix} has been modified since its last update.
     * </p>
     * @see #update()
     */
    public final Matrix4f getPMvMat() {
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
     * {@link #getPMat() P matrix} or {@link #getMvMat() Mv matrix} has been modified since its last update.
     * </p>
     * @see #update()
     */
    public final Matrix4f getPMviMat() {
        if( 0 != ( dirtyBits & PREMUL_PMVI ) ) {
            if( null == matPMvi ) {
                matPMvi = new Matrix4f();
            }
            final Matrix4f mPMv = getPMvMat();
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
     * {@link #getPMat() P matrix} or {@link #getMvMat() Mv matrix} has been modified since its last update.
     * </p>
     * @see #update()
     */
    public final Frustum getFrustum() {
        if( 0 != ( dirtyBits & FRUSTUM ) ) {
            if( null == frustum ) {
                frustum = new Frustum();
            }
            final Matrix4f mPMv = getPMvMat();
            frustum.updateFrustumPlanes(mPMv);
            dirtyBits &= ~FRUSTUM;
        }
        return frustum;
    }

    /**
     * Update the derived {@link #getMviMat() inverse modelview (Mvi)},
     * {@link #getMvitMat() inverse transposed modelview (Mvit)} matrices
     * <b>if</b> they {@link #isReqDirty() are dirty} <b>and</b>
     * requested via the constructor {@link #PMVMatrix(int)}.<br/>
     * Hence updates the following dirty bits.
     * - {@link #INVERSE_MODELVIEW}
     * - {@link #INVERSE_TRANSPOSED_MODELVIEW}
     * <p>
     * The {@link Frustum} is updated only via {@link #getFrustum()} separately.
     * </p>
     * <p>
     * The Mvi and Mvit matrices are considered dirty, if their corresponding
     * {@link #getMvMat() Mv matrix} has been modified since their last update.
     * </p>
     * <p>
     * Method is automatically called by {@link SyncMatrix4f} and {@link SyncMatrices4f}
     * instances {@link SyncAction} as retrieved by e.g. {@link #getSyncMvitMat()}.
     * This ensures an automatic update cycle if used with {@link GLUniformData}.
     * </p>
     * <p>
     * Method may be called manually in case mutable operations has been called
     * and caller operates on already fetched references, i.e. not calling
     * {@link #getMviMat()}, {@link #getMvitMat()} anymore.
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
     * @see #PMVMatrix(int)
     * @see #getMviMat()
     * @see #getMvitMat()
     * @see #getSyncPMvMviMat()
     * @see #getSyncPMvMviMvitMat()
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
                throw new GLException(msgCantComputeInverse);
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
    private static final String msgCantComputeInverse = "Invalid source Mv matrix, can't compute inverse";

    private final Matrix4f matP;
    private final Matrix4f matMv;
    private final Matrix4f matTex;

    private final Matrix4f matMvi;
    private final Matrix4f matMvit;

    private static final int mP_offset      = 0*16;
    private static final int mMv_offset     = 1*16;
    private final int mMvi_offset;
    private final int mMvit_offset;
    private final int mTex_offset;

    private final float[] matrixStore;

    private final FloatBuffer bufP, bufMv, bufTex;
    private final FloatBuffer bufMvi, bufMvit;
    private final FloatBuffer bufP_Mv, bufP_Mv_Mvi, bufP_Mv_Mvi_Mvit;

    private final SyncMatrix4f syncP, syncMv, syncT;
    private final SyncMatrix4f syncMvi, syncMvit;
    private final SyncMatrices4f syncP_Mv, syncP_Mv_Mvi, syncP_Mv_Mvi_Mvit;

    private final Matrix4f mat4Tmp1;
    private final Vec3f vec3Tmp1;
    private final Vec4f vec4Tmp1;

    private int matrixMode = GL_MODELVIEW;
    private int modifiedBits = MODIFIED_ALL;
    private int dirtyBits = 0; // contains the dirty bits, i.e. hinting for update operation
    private final int requestBits; // may contain the requested bits: INVERSE_MODELVIEW | INVERSE_TRANSPOSED_MODELVIEW
    private Matrix4f mat4Tmp2;
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
