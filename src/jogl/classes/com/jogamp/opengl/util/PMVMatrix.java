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

    /** Bit value stating a dirty {@link #getMviMat() inverse modelview matrix (Mvi)}. */
    public static final int DIRTY_INVERSE_MODELVIEW             = 1 << 0;
    /** Bit value stating a dirty {@link #getMvitMat() inverse transposed modelview matrix (Mvit)}. */
    public static final int DIRTY_INVERSE_TRANSPOSED_MODELVIEW  = 1 << 1;
    /** Bit value stating a dirty {@link #getFrustum() frustum}. */
    public static final int DIRTY_FRUSTUM                       = 1 << 2;
    /** Bit value stating all is dirty */
    public static final int DIRTY_ALL                           = DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM;

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
     * Implementation uses non-direct non-NIO Buffers with guaranteed backing array,
     * which allows faster access in Java computation.
     * </p>
     */
    public PMVMatrix() {
          // I    Identity
          // T    Texture
          // P    Projection
          // Mv   ModelView
          // Mvi  Modelview-Inverse
          // Mvit Modelview-Inverse-Transpose

          // actual underlying Matrix4f data
          matP           = new Matrix4f();
          matMv          = new Matrix4f();
          matMvi         = new Matrix4f();
          matMvit        = new Matrix4f();
          matTex         = new Matrix4f();

          // float back buffer for GPU, Matrix4f -> matrixStore via SyncedBuffer
          matrixStore    = new float[5*16];

          // FloatBuffer for single Matrix4f back-buffer
          matrixP        = Buffers.slice2Float(matrixStore,  mP_offset,    1*16);  // P
          matrixMv       = Buffers.slice2Float(matrixStore,  mMv_offset,   1*16);  //     Mv
          matrixMvi      = Buffers.slice2Float(matrixStore,  mMvi_offset,  1*16);  //          Mvi
          matrixMvit     = Buffers.slice2Float(matrixStore,  mMvit_offset, 1*16);  //                Mvit
          matrixTex      = Buffers.slice2Float(matrixStore,  mTex_offset,  1*16);  //                       T

          // FloatBuffer for aggregated multiple Matrix4f back-buffer
          matrixPMvMvit  = Buffers.slice2Float(matrixStore,  mP_offset,    4*16);  // P + Mv + Mvi + Mvit
          matrixPMvMvi   = Buffers.slice2Float(matrixStore,  mP_offset,    3*16);  // P + Mv + Mvi
          matrixPMv      = Buffers.slice2Float(matrixStore,  mP_offset,    2*16);  // P + Mv

          matPSync       = new SyncBuffer0(matP,    matrixP);   // mP_offset
          matMvSync      = new SyncBuffer1(matMv,   matrixMv,   mMv_offset);
          matMviSync     = new SyncBuffer1(matMvi,  matrixMvi,  mMvi_offset);
          matMvitSync    = new SyncBuffer1(matMvit, matrixMvit, mMvit_offset);
          matTexSync     = new SyncBuffer1(matTex,  matrixTex,  mTex_offset);

          matPMvMvitSync = new SyncBufferN(new Matrix4f[] { matP, matMv, matMvi, matMvit }, matrixPMvMvit, mP_offset);
          matPMvMviSync  = new SyncBufferN(new Matrix4f[] { matP, matMv, matMvi },          matrixPMvMvi,  mP_offset);
          matPMvSync     = new SyncBufferN(new Matrix4f[] { matP, matMv },                  matrixPMv,     mP_offset);

          mat4Tmp1       = new Matrix4f();
          mat4Tmp2       = new Matrix4f();
          vec3Tmp1       = new Vec3f();
          vec4Tmp1       = new Vec4f();

          reset();

          frustum = null;
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
        dirtyBits = DIRTY_ALL;
        requestMask = 0;
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
     * Return the first temporary Matrix4f exposed to be reused for efficiency.
     * <p>
     * Temporary storage is only used by this class within single method calls,
     * hence has no side-effects.
     * </p>
     */
    public final Matrix4f getTmp1Mat() { return mat4Tmp1; }

    /**
     * Return the second temporary Matrix4f exposed to be reused for efficiency.
     * <p>
     * Temporary storage is only used by this class within single method calls,
     * hence has no side-effects.
     * </p>
     */
    public final Matrix4f getTmp2Mat() { return mat4Tmp2; }

    /**
     * Return the first temporary Vec3f exposed to be reused for efficiency.
     * <p>
     * Temporary storage is only used by this class within single method calls,
     * hence has no side-effects.
     * </p>
     */
    public final Vec3f getTmp1Vec3f() { return vec3Tmp1; }

    /**
     * Return the first temporary Vec4f exposed to be reused for efficiency.
     * <p>
     * Temporary storage is only used by this class within single method calls,
     * hence has no side-effects.
     * </p>
     */
    public final Vec4f getTmp1Vec4f() { return vec4Tmp1; }

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
        return matTexSync;
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
        return matPSync;
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
        return matMvSync;
    }

    /**
     * Returns the inverse {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mvi).
     * <p>
     * Method enables the Mvi matrix update, and performs it's update w/o clearing the modified bits.
     * </p>
     * <p>
     * See {@link #update()} and <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @see #update()
     * @see #clearAllUpdateRequests()
     */
    public final Matrix4f getMviMat() {
        requestMask |= DIRTY_INVERSE_MODELVIEW ;
        updateImpl(false);
        return matMvi;
    }

    /**
     * Returns the {@link SyncMatrix} of inverse {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mvi).
     * <p>
     * Method enables the Mvi matrix update, and performs it's update w/o clearing the modified bits.
     * </p>
     * <p>
     * See {@link #update()} and <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @see #update()
     * @see #clearAllUpdateRequests()
     */
    public final SyncMatrix4f getSyncMviMat() {
        requestMask |= DIRTY_INVERSE_MODELVIEW ;
        updateImpl(false);
        return matMviSync;
    }

    /**
     * Returns the inverse transposed {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mvit).
     * <p>
     * Method enables the Mvit matrix update, and performs it's update w/o clearing the modified bits.
     * </p>
     * <p>
     * See {@link #update()} and <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @see #update()
     * @see #clearAllUpdateRequests()
     */
    public final Matrix4f getMvitMat() {
        requestMask |= DIRTY_INVERSE_TRANSPOSED_MODELVIEW ;
        updateImpl(false);
        return matMvit;
    }

    /**
     * Returns the {@link SyncMatrix} of inverse transposed {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mvit).
     * <p>
     * Method enables the Mvit matrix update, and performs it's update w/o clearing the modified bits.
     * </p>
     * <p>
     * See {@link #update()} and <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @see #update()
     * @see #clearAllUpdateRequests()
     */
    public final SyncMatrix4f getSyncMvitMat() {
        requestMask |= DIRTY_INVERSE_TRANSPOSED_MODELVIEW ;
        updateImpl(false);
        return matMvitSync;
    }

    /**
     * Returns {@link SyncMatrices4f} of 2 matrices within one FloatBuffer: {@link #getPMat() P} and {@link #getMvMat() Mv}.
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final SyncMatrices4f getSyncPMvMat() {
        return matPMvSync;
    }

    /**
     * Returns {@link SyncMatrices4f} of 3 matrices within one FloatBuffer: {@link #getPMat() P}, {@link #getMvMat() Mv} and {@link #getMviMat() Mvi}.
     * <p>
     * Method enables the Mvi matrix update, and performs it's update w/o clearing the modified bits.
     * </p>
     * <p>
     * See {@link #update()} and <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @see #update()
     * @see #clearAllUpdateRequests()
     */
    public final SyncMatrices4f getSyncPMvMviMat() {
        requestMask |= DIRTY_INVERSE_MODELVIEW ;
        updateImpl(false);
        return matPMvMviSync;
    }

    /**
     * Returns {@link SyncMatrices4f} of 4 matrices within one FloatBuffer: {@link #getPMat() P}, {@link #getMvMat() Mv}, {@link #getMviMat() Mvi} and {@link #getMvitMat() Mvit}.
     * <p>
     * Method enables the Mvi and Mvit matrix update, and performs it's update w/o clearing the modified bits.
     * </p>
     * <p>
     * See {@link #update()} and <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @see #update()
     * @see #clearAllUpdateRequests()
     */
    public final SyncMatrices4f getSyncPMvMvitMat() {
        requestMask |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW ;
        updateImpl(false);
        return matPMvMvitSync;
    }

    /** Returns the frustum, derived from projection * modelview */
    public final Frustum getFrustum() {
        requestMask |= DIRTY_FRUSTUM;
        updateImpl(false);
        return frustum;
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
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.load(values, offset);
            dirtyBits |= DIRTY_FRUSTUM ;
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
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.load(m);
            dirtyBits |= DIRTY_FRUSTUM ;
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
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.load(m);
            dirtyBits |= DIRTY_FRUSTUM ;
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
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.setToRotation(quat);
            dirtyBits |= DIRTY_FRUSTUM ;
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
        } else if(matrixMode==GL_PROJECTION) {
            matP.pop();
        } else if(matrixMode==GL.GL_TEXTURE) {
            matTex.pop();
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
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.loadIdentity();
            dirtyBits |= DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matTex.loadIdentity();
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void glMultMatrixf(final FloatBuffer m) {
        final int spos = m.position();
        if(matrixMode==GL_MODELVIEW) {
            matMv.mul( mat4Tmp1.load( m ) );
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.mul( mat4Tmp1.load( m ) );
            dirtyBits |= DIRTY_FRUSTUM ;
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
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.mul( mat4Tmp1.load( m, m_offset ) );
            dirtyBits |= DIRTY_FRUSTUM ;
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
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matP.mul( m );
            dirtyBits |= DIRTY_FRUSTUM ;
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
        glMultMatrixf( mat4Tmp1.setToLookAt(eye, center, up, mat4Tmp2) );
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
        if( Matrix4f.mapWinToObj(winx, winy, winz, matMv, matP, viewport, objPos, mat4Tmp1) ) {
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
        if( Matrix4f.mapWinToObj4(winx, winy, winz, clipw, matMv, matP, viewport, near, far, objPos, mat4Tmp1) ) {
            return true;
        } else {
            return false;
        }
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
        if( null != mat4Tmp1.setToPick(x, y, deltaX, deltaY, viewport, mat4Tmp2) ) {
            glMultMatrixf( mat4Tmp1 );
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
        return Matrix4f.mapWinToRay(winx, winy, winz0, winz1, matMv, matP, viewport, ray, mat4Tmp1, mat4Tmp2);
    }

    public StringBuilder toString(StringBuilder sb, final String f) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        final boolean mviDirty  = 0 != (DIRTY_INVERSE_MODELVIEW & dirtyBits);
        final boolean mvitDirty = 0 != (DIRTY_INVERSE_TRANSPOSED_MODELVIEW & dirtyBits);
        final boolean frustumDirty = 0 != (DIRTY_FRUSTUM & dirtyBits);
        final boolean mviReq = 0 != (DIRTY_INVERSE_MODELVIEW & requestMask);
        final boolean mvitReq = 0 != (DIRTY_INVERSE_TRANSPOSED_MODELVIEW & requestMask);
        final boolean frustumReq = 0 != (DIRTY_FRUSTUM & requestMask);
        final boolean modP = 0 != ( MODIFIED_PROJECTION & modifiedBits );
        final boolean modMv = 0 != ( MODIFIED_MODELVIEW & modifiedBits );
        final boolean modT = 0 != ( MODIFIED_TEXTURE & modifiedBits );

        sb.append("PMVMatrix[modified[P ").append(modP).append(", Mv ").append(modMv).append(", T ").append(modT);
        sb.append("], dirty/req[Mvi ").append(mviDirty).append("/").append(mviReq).append(", Mvit ").append(mvitDirty).append("/").append(mvitReq).append(", Frustum ").append(frustumDirty).append("/").append(frustumReq).append("]").append(PlatformPropsImpl.NEWLINE);
        sb.append(", Projection").append(PlatformPropsImpl.NEWLINE);
        matP.toString(sb, null, f);
        sb.append(", Modelview").append(PlatformPropsImpl.NEWLINE);
        matMv.toString(sb, null, f);
        sb.append(", Texture").append(PlatformPropsImpl.NEWLINE);
        matTex.toString(sb, null, f);
        if( 0 != ( requestMask & DIRTY_INVERSE_MODELVIEW ) ) {
            sb.append(", Inverse Modelview").append(PlatformPropsImpl.NEWLINE);
            matMvi.toString(sb, null, f);
        }
        if( 0 != ( requestMask & DIRTY_INVERSE_TRANSPOSED_MODELVIEW ) ) {
            sb.append(", Inverse Transposed Modelview").append(PlatformPropsImpl.NEWLINE);
            matMvit.toString(sb, null, f);
        }
        sb.append("]");
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
     */
    public final int getModifiedBits(final boolean clear) {
        final int r = modifiedBits;
        if(clear) {
            modifiedBits = 0;
        }
        return r;
    }

    /**
     * Returns the dirty bits due to mutable operations.
     * <p>
     * A dirty bit is set , if the corresponding matrix had been modified by a mutable operation
     * since last {@link #update()} call. The latter clears the dirty state only if the dirty matrix (Mvi or Mvit) or {@link Frustum}
     * has been requested by one of the {@link #getMviMat() Mvi get}, {@link #getMvitMat() Mvit get}
     * or {@link #getFrustum() Frustum get} methods.
     * </p>
     *
     * @see #DIRTY_INVERSE_MODELVIEW
     * @see #DIRTY_INVERSE_TRANSPOSED_MODELVIEW
     * @see #DIRTY_FRUSTUM
     * @see #getMviMat()
     * @see #getMvitMat()
     * @see #glGetPMvMviMatrixf()
     * @see #glGetPMvMvitMatrixf()
     * @see #getFrustum()
     */
    public final int getDirtyBits() {
        return dirtyBits;
    }

    /**
     * Returns the request bit mask, which uses bit values equal to the dirty mask.
     * <p>
     * The request bit mask is set by one of the {@link #getMviMat() Mvi get}, {@link #getMvitMat() Mvit get}
     * or {@link #getFrustum() Frustum get} methods.
     * </p>
     *
     * @see #clearAllUpdateRequests()
     * @see #DIRTY_INVERSE_MODELVIEW
     * @see #DIRTY_INVERSE_TRANSPOSED_MODELVIEW
     * @see #DIRTY_FRUSTUM
     * @see #getMviMat()
     * @see #getMvitMat()
     * @see #glGetPMvMviMatrixf()
     * @see #glGetPMvMvitMatrixf()
     * @see #getFrustum()
     */
    public final int getRequestMask() {
        return requestMask;
    }


    /**
     * Clears all {@link #update()} requests of the Mvi and Mvit matrix and Frustum
     * after it has been enabled by one of the {@link #getMviMat() Mvi get}, {@link #getMvitMat() Mvit get}
     * or {@link #getFrustum() Frustum get} methods.
     * <p>
     * Allows user to disable subsequent Mvi, Mvit and {@link Frustum} updates if no more required.
     * </p>
     *
     * @see #getMviMat()
     * @see #getMvitMat()
     * @see #glGetPMvMviMatrixf()
     * @see #glGetPMvMvitMatrixf()
     * @see #getFrustum()
     * @see #getRequestMask()
     */
    public final void clearAllUpdateRequests() {
        requestMask &= ~DIRTY_ALL;
    }

    /**
     * Update the derived {@link #getMviMat() inverse modelview (Mvi)},
     * {@link #getMvitMat() inverse transposed modelview (Mvit)} matrices and {@link Frustum}
     * <b>if</b> they are dirty <b>and</b> they were requested
     * by one of the {@link #getMviMat() Mvi get}, {@link #getMvitMat() Mvit get}
     * or {@link #getFrustum() Frustum get} methods.
     * <p>
     * The Mvi and Mvit matrices and {@link Frustum} are considered dirty, if their corresponding
     * {@link #getMvMat() Mv matrix} has been modified since their last update.
     * </p>
     * <p>
     * Method should be called manually in case mutable operations has been called
     * and caller operates on already fetched references, i.e. not calling
     * {@link #getMviMat() Mvi get}, {@link #getMvitMat() Mvit get}
     * or {@link #getFrustum() Frustum get} etc anymore.
     * </p>
     * <p>
     * This method clears the modified bits like {@link #getModifiedBits(boolean) getModifiedBits(true)},
     * which are set by any mutable operation. The modified bits have no impact
     * on this method, but the return value.
     * </p>
     *
     * @return true if any matrix has been modified since last update call or
     *         if the derived matrices Mvi and Mvit or {@link Frustum} were updated, otherwise false.
     *         In other words, method returns true if any matrix used by the caller must be updated,
     *         e.g. uniforms in a shader program.
     *
     * @see #getModifiedBits(boolean)
     * @see #MODIFIED_PROJECTION
     * @see #MODIFIED_MODELVIEW
     * @see #MODIFIED_TEXTURE
     * @see #DIRTY_INVERSE_MODELVIEW
     * @see #DIRTY_INVERSE_TRANSPOSED_MODELVIEW
     * @see #DIRTY_FRUSTUM
     * @see #getMviMat()
     * @see #getMvitMat()
     * @see #glGetPMvMviMatrixf()
     * @see #glGetPMvMvitMatrixf()
     * @see #getFrustum()
     * @see #clearAllUpdateRequests()
     */
    public final boolean update() {
        return updateImpl(true);
    }
    private final boolean updateImpl(final boolean clearModBits) {
        boolean mod = 0 != modifiedBits;
        if(clearModBits) {
            modifiedBits = 0;
        }

        if( 0 != ( dirtyBits & ( DIRTY_FRUSTUM & requestMask ) ) ) {
            if( null == frustum ) {
                frustum = new Frustum();
            }
            mat4Tmp1.mul(matP, matMv);
            frustum.updateFrustumPlanes(mat4Tmp1);
            dirtyBits &= ~DIRTY_FRUSTUM;
            mod = true;
        }

        if( 0 == ( dirtyBits & requestMask ) ) {
            return mod; // nothing more requested which may have been dirty
        }

        return setMviMvit() || mod;
    }

    //
    // private
    //
    private static final String msgCantComputeInverse = "Invalid source Mv matrix, can't compute inverse";

    private final boolean setMviMvit() {
        boolean res = false;
        if( 0 != ( dirtyBits & DIRTY_INVERSE_MODELVIEW ) ) { // only if dirt; always requested at this point, see update()
            if( !matMvi.invert(matMv) ) {
                throw new GLException(msgCantComputeInverse);
            }
            dirtyBits &= ~DIRTY_INVERSE_MODELVIEW;
            res = true;
        }
        if( 0 != ( requestMask & ( dirtyBits & DIRTY_INVERSE_TRANSPOSED_MODELVIEW ) ) ) { // only if requested & dirty
            matMvit.transpose(matMvi);
            dirtyBits &= ~DIRTY_INVERSE_TRANSPOSED_MODELVIEW;
            res = true;
        }
        return res;
    }

    private final Matrix4f matP;
    private final Matrix4f matMv;
    private final Matrix4f matMvi;
    private final Matrix4f matMvit;
    private final Matrix4f matTex;

    private static final int mP_offset      = 0*16;
    private static final int mMv_offset     = 1*16;
    private static final int mMvi_offset    = 2*16;
    private static final int mMvit_offset   = 3*16;
    private static final int mTex_offset    = 4*16;

    private final float[] matrixStore;
    private final FloatBuffer matrixP, matrixMv, matrixMvi, matrixMvit, matrixTex;
    private final FloatBuffer matrixPMvMvit, matrixPMvMvi, matrixPMv;

    private final SyncMatrix4f matPSync, matMvSync, matMviSync, matMvitSync, matTexSync;
    private final SyncMatrices4f matPMvMvitSync, matPMvMviSync, matPMvSync;

    private final Matrix4f mat4Tmp1, mat4Tmp2;
    private final Vec3f vec3Tmp1;
    private final Vec4f vec4Tmp1;

    private int matrixMode = GL_MODELVIEW;
    private int modifiedBits = MODIFIED_ALL;
    private int dirtyBits = DIRTY_ALL; // contains the dirty bits, i.e. hinting for update operation
    private int requestMask = 0; // may contain the requested dirty bits: DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW
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
    private final class SyncBufferN implements SyncMatrices4f {
        private final Matrix4f[] mats;
        private final FloatBuffer fbuf;
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
            this.mats = ms;
            this.fbuf = fbuf;
            this.offset = offset;
        }

        @Override
        public SyncAction getAction() { return action; }

        @Override
        public Buffer getBuffer() { return fbuf; }

        @Override
        public SyncBuffer sync() { getAction().sync(); return this; }

        @Override
        public Buffer getSyncBuffer() { getAction().sync(); return fbuf; }

        @Override
        public Matrix4f[] getMatrices() { return mats; }

        @Override
        public FloatBuffer getSyncFloats() { getAction().sync(); return fbuf; }
    }
}
