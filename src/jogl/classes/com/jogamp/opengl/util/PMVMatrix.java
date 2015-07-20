/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2011 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 */

package com.jogamp.opengl.util;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import jogamp.common.os.PlatformPropsImpl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.FloatStack;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.Ray;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.math.geom.Frustum;

/**
 * PMVMatrix implements a subset of the fixed function pipeline
 * regarding the projection (P), modelview (Mv) matrix operation
 * which is specified in {@link GLMatrixFunc}.
 * <p>
 * Further more, PMVMatrix provides the {@link #glGetMviMatrixf() inverse modelview matrix (Mvi)} and
 * {@link #glGetMvitMatrixf() inverse transposed modelview matrix (Mvit)}.
 * {@link Frustum} is also provided by {@link #glGetFrustum()}.
 * To keep these derived values synchronized after mutable Mv operations like {@link #glRotatef(float, float, float, float) glRotatef(..)}
 * in {@link #glMatrixMode(int) glMatrixMode}({@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW}),
 * users have to call {@link #update()} before using Mvi and Mvit.
 * </p>
 * <p>
 * All matrices are provided in column-major order,
 * as specified in the OpenGL fixed function pipeline, i.e. compatibility profile.
 * See {@link FloatUtil}.
 * </p>
 * <p>
 * PMVMatrix can supplement {@link GL2ES2} applications w/ the
 * lack of the described matrix functionality.
 * </p>
 * <a name="storageDetails"><h5>Matrix storage details</h5></a>
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

    /** Bit value stating a modified {@link #glGetPMatrixf() projection matrix (P)}, since last {@link #update()} call. */
    public static final int MODIFIED_PROJECTION                 = 1 << 0;
    /** Bit value stating a modified {@link #glGetMvMatrixf() modelview matrix (Mv)}, since last {@link #update()} call. */
    public static final int MODIFIED_MODELVIEW                  = 1 << 1;
    /** Bit value stating a modified {@link #glGetTMatrixf() texture matrix (T)}, since last {@link #update()} call. */
    public static final int MODIFIED_TEXTURE                    = 1 << 2;
    /** Bit value stating all is modified */
    public static final int MODIFIED_ALL                        = MODIFIED_PROJECTION | MODIFIED_MODELVIEW | MODIFIED_TEXTURE ;

    /** Bit value stating a dirty {@link #glGetMviMatrixf() inverse modelview matrix (Mvi)}. */
    public static final int DIRTY_INVERSE_MODELVIEW             = 1 << 0;
    /** Bit value stating a dirty {@link #glGetMvitMatrixf() inverse transposed modelview matrix (Mvit)}. */
    public static final int DIRTY_INVERSE_TRANSPOSED_MODELVIEW  = 1 << 1;
    /** Bit value stating a dirty {@link #glGetFrustum() frustum}. */
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
     * @param sb optional passed StringBuilder instance to be used
     * @param f the format string of one floating point, i.e. "%10.5f", see {@link java.util.Formatter}
     * @param a 4x4 matrix in column major order (OpenGL)
     * @return matrix string representation
     */
    @SuppressWarnings("deprecation")
    public static StringBuilder matrixToString(final StringBuilder sb, final String f, final FloatBuffer a) {
        return FloatUtil.matrixToString(sb, null, f, a, 0, 4, 4, false);
    }

    /**
     * @param sb optional passed StringBuilder instance to be used
     * @param f the format string of one floating point, i.e. "%10.5f", see {@link java.util.Formatter}
     * @param a 4x4 matrix in column major order (OpenGL)
     * @param b 4x4 matrix in column major order (OpenGL)
     * @return side by side representation
     */
    @SuppressWarnings("deprecation")
    public static StringBuilder matrixToString(final StringBuilder sb, final String f, final FloatBuffer a, final FloatBuffer b) {
        return FloatUtil.matrixToString(sb, null, f, a, 0, b, 0, 4, 4, false);
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
          matrixArray = new float[5*16];

          mP_offset   = 0*16;
          mMv_offset  = 1*16;
          mTex_offset = 4*16;

          matrixPMvMvit = Buffers.slice2Float(matrixArray,  0*16, 4*16);  // P + Mv + Mvi + Mvit
          matrixPMvMvi  = Buffers.slice2Float(matrixArray,  0*16, 3*16);  // P + Mv + Mvi
          matrixPMv     = Buffers.slice2Float(matrixArray,  0*16, 2*16);  // P + Mv
          matrixP       = Buffers.slice2Float(matrixArray,  0*16, 1*16);  // P
          matrixMv      = Buffers.slice2Float(matrixArray,  1*16, 1*16);  //     Mv
          matrixMvi     = Buffers.slice2Float(matrixArray,  2*16, 1*16);  //          Mvi
          matrixMvit    = Buffers.slice2Float(matrixArray,  3*16, 1*16);  //                Mvit
          matrixTex     = Buffers.slice2Float(matrixArray,  4*16, 1*16);  //                       T

          mat4Tmp1      = new float[16];
          mat4Tmp2      = new float[16];
          mat4Tmp3      = new float[16];
          matrixTxSx    = new float[16];
          FloatUtil.makeIdentity(matrixTxSx);

          // Start w/ zero size to save memory
          matrixTStack = new FloatStack( 0,  2*16); // growSize: GL-min size (2)
          matrixPStack = new FloatStack( 0,  2*16); // growSize: GL-min size (2)
          matrixMvStack= new FloatStack( 0, 16*16); // growSize: half GL-min size (32)

          reset();

          frustum = null;
    }

    /**
     * Issues {@link #glLoadIdentity()} on all matrices,
     * i.e. {@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW}, {@link GLMatrixFunc#GL_PROJECTION GL_PROJECTION} or {@link GL#GL_TEXTURE GL_TEXTURE}
     * and resets all internal states.
     */
    public final void reset() {
        FloatUtil.makeIdentity(matrixArray, mMv_offset);
        FloatUtil.makeIdentity(matrixArray, mP_offset);
        FloatUtil.makeIdentity(matrixArray, mTex_offset);

        modifiedBits = MODIFIED_ALL;
        dirtyBits = DIRTY_ALL;
        requestMask = 0;
        matrixMode = GL_MODELVIEW;
    }

    /** Returns the current matrix-mode, one of {@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW}, {@link GLMatrixFunc#GL_PROJECTION GL_PROJECTION} or {@link GL#GL_TEXTURE GL_TEXTURE}. */
    public final int  glGetMatrixMode() {
        return matrixMode;
    }

    /**
     * Returns the {@link GLMatrixFunc#GL_TEXTURE_MATRIX texture matrix} (T).
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final FloatBuffer glGetTMatrixf() {
        return matrixTex;
    }

    /**
     * Returns the {@link GLMatrixFunc#GL_PROJECTION_MATRIX projection matrix} (P).
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final FloatBuffer glGetPMatrixf() {
        return matrixP;
    }

    /**
     * Returns the {@link GLMatrixFunc#GL_MODELVIEW_MATRIX modelview matrix} (Mv).
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final FloatBuffer glGetMvMatrixf() {
        return matrixMv;
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
    public final FloatBuffer glGetMviMatrixf() {
        requestMask |= DIRTY_INVERSE_MODELVIEW ;
        updateImpl(false);
        return matrixMvi;
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
    public final FloatBuffer glGetMvitMatrixf() {
        requestMask |= DIRTY_INVERSE_TRANSPOSED_MODELVIEW ;
        updateImpl(false);
        return matrixMvit;
    }

    /**
     * Returns 2 matrices within one FloatBuffer: {@link #glGetPMatrixf() P} and {@link #glGetMvMatrixf() Mv}.
     * <p>
     * See <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     */
    public final FloatBuffer glGetPMvMatrixf() {
        return matrixPMv;
    }

    /**
     * Returns 3 matrices within one FloatBuffer: {@link #glGetPMatrixf() P}, {@link #glGetMvMatrixf() Mv} and {@link #glGetMviMatrixf() Mvi}.
     * <p>
     * Method enables the Mvi matrix update, and performs it's update w/o clearing the modified bits.
     * </p>
     * <p>
     * See {@link #update()} and <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @see #update()
     * @see #clearAllUpdateRequests()
     */
    public final FloatBuffer glGetPMvMviMatrixf() {
        requestMask |= DIRTY_INVERSE_MODELVIEW ;
        updateImpl(false);
        return matrixPMvMvi;
    }

    /**
     * Returns 4 matrices within one FloatBuffer: {@link #glGetPMatrixf() P}, {@link #glGetMvMatrixf() Mv}, {@link #glGetMviMatrixf() Mvi} and {@link #glGetMvitMatrixf() Mvit}.
     * <p>
     * Method enables the Mvi and Mvit matrix update, and performs it's update w/o clearing the modified bits.
     * </p>
     * <p>
     * See {@link #update()} and <a href="#storageDetails"> matrix storage details</a>.
     * </p>
     * @see #update()
     * @see #clearAllUpdateRequests()
     */
    public final FloatBuffer glGetPMvMvitMatrixf() {
        requestMask |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW ;
        updateImpl(false);
        return matrixPMvMvit;
    }

    /** Returns the frustum, derived from projection * modelview */
    public final Frustum glGetFrustum() {
        requestMask |= DIRTY_FRUSTUM;
        updateImpl(false);
        return frustum;
    }

    /*
     * @return the matrix of the current matrix-mode
     */
    public final FloatBuffer glGetMatrixf() {
        return glGetMatrixf(matrixMode);
    }

    /**
     * @param matrixName Either a matrix-get-name, i.e.
     *                   {@link GLMatrixFunc#GL_MODELVIEW_MATRIX GL_MODELVIEW_MATRIX}, {@link GLMatrixFunc#GL_PROJECTION_MATRIX GL_PROJECTION_MATRIX} or {@link GLMatrixFunc#GL_TEXTURE_MATRIX GL_TEXTURE_MATRIX},
     *                   or a matrix-mode-name, i.e.
     *                   {@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW}, {@link GLMatrixFunc#GL_PROJECTION GL_PROJECTION} or {@link GL#GL_TEXTURE GL_TEXTURE}
     * @return the named matrix, not a copy!
     */
    public final FloatBuffer glGetMatrixf(final int matrixName) {
        switch(matrixName) {
            case GL_MODELVIEW_MATRIX:
            case GL_MODELVIEW:
                return matrixMv;
            case GL_PROJECTION_MATRIX:
            case GL_PROJECTION:
                return matrixP;
            case GL_TEXTURE_MATRIX:
            case GL.GL_TEXTURE:
                return matrixTex;
            default:
              throw new GLException("unsupported matrixName: "+matrixName);
        }
    }


    /**
     * Multiplies the {@link #glGetPMatrixf() P} and {@link #glGetMvMatrixf() Mv} matrix, i.e.
     * <pre>
     *    mat4PMv = P x Mv
     * </pre>
     * @param mat4PMv 4x4 matrix storage for result
     * @param mat4PMv_offset
     * @return given matrix for chaining
     */
    public final float[] multPMvMatrixf(final float[/*16*/] mat4PMv, final int mat4PMv_offset) {
        FloatUtil.multMatrix(matrixArray, mP_offset, matrixArray, mMv_offset, mat4PMv, mat4PMv_offset);
        return mat4PMv;
    }

    /**
     * Multiplies the {@link #glGetMvMatrixf() Mv} and {@link #glGetPMatrixf() P} matrix, i.e.
     * <pre>
     *    mat4MvP = Mv x P
     * </pre>
     * @param mat4MvP 4x4 matrix storage for result
     * @param mat4MvP_offset
     * @return given matrix for chaining
     */
    public final float[] multMvPMatrixf(final float[/*16*/] mat4MvP, final int mat4MvP_offset) {
        FloatUtil.multMatrix(matrixArray, mMv_offset, matrixArray, mP_offset, mat4MvP, mat4MvP_offset);
        return mat4MvP;
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
            final FloatBuffer matrix = glGetMatrixf(matrixGetName);
            params.put(matrix); // matrix -> params
            matrix.reset();
        }
        params.position(pos);
    }

    @Override
    public final void glGetFloatv(final int matrixGetName, final float[] params, final int params_offset) {
        if(matrixGetName==GL_MATRIX_MODE) {
            params[params_offset]=matrixMode;
        } else {
            final FloatBuffer matrix = glGetMatrixf(matrixGetName);
            matrix.get(params, params_offset, 16); // matrix -> params
            matrix.reset();
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
            matrixMv.put(values, offset, 16);
            matrixMv.reset();
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matrixP.put(values, offset, 16);
            matrixP.reset();
            dirtyBits |= DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matrixTex.put(values, offset, 16);
            matrixTex.reset();
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    @Override
    public final void glLoadMatrixf(final java.nio.FloatBuffer m) {
        final int spos = m.position();
        if(matrixMode==GL_MODELVIEW) {
            matrixMv.put(m);
            matrixMv.reset();
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matrixP.put(m);
            matrixP.reset();
            dirtyBits |= DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matrixTex.put(m);
            matrixTex.reset();
            modifiedBits |= MODIFIED_TEXTURE;
        }
        m.position(spos);
    }

    /**
     * Load the current matrix with the values of the given {@link Quaternion}'s rotation {@link Quaternion#toMatrix(float[], int) matrix representation}.
     */
    public final void glLoadMatrix(final Quaternion quat) {
        if(matrixMode==GL_MODELVIEW) {
            quat.toMatrix(matrixArray, mMv_offset);
            matrixMv.reset();
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            quat.toMatrix(matrixArray, mP_offset);
            matrixP.reset();
            dirtyBits |= DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            quat.toMatrix(matrixArray, mTex_offset);
            matrixTex.reset();
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    @Override
    public final void glPopMatrix() {
        final FloatStack stack;
        if(matrixMode==GL_MODELVIEW) {
            stack = matrixMvStack;
        } else if(matrixMode==GL_PROJECTION) {
            stack = matrixPStack;
        } else if(matrixMode==GL.GL_TEXTURE) {
            stack = matrixTStack;
        } else {
            throw new InternalError("XXX: mode "+matrixMode);
        }
        stack.position(stack.position() - 16);
        glLoadMatrixf(stack.buffer(), stack.position());
    }

    @Override
    public final void glPushMatrix() {
        if(matrixMode==GL_MODELVIEW) {
            matrixMvStack.putOnTop(matrixMv, 16);
            matrixMv.reset();
        } else if(matrixMode==GL_PROJECTION) {
            matrixPStack.putOnTop(matrixP, 16);
            matrixP.reset();
        } else if(matrixMode==GL.GL_TEXTURE) {
            matrixTStack.putOnTop(matrixTex, 16);
            matrixTex.reset();
        }
    }

    @Override
    public final void glLoadIdentity() {
        if(matrixMode==GL_MODELVIEW) {
            FloatUtil.makeIdentity(matrixArray, mMv_offset);
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            FloatUtil.makeIdentity(matrixArray, mP_offset);
            dirtyBits |= DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            FloatUtil.makeIdentity(matrixArray, mTex_offset);
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void glMultMatrixf(final FloatBuffer m) {
        if(matrixMode==GL_MODELVIEW) {
            FloatUtil.multMatrix(matrixMv, m);
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            FloatUtil.multMatrix(matrixP, m);
            dirtyBits |= DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            FloatUtil.multMatrix(matrixTex, m);
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    @Override
    public final void glMultMatrixf(final float[] m, final int m_offset) {
        if(matrixMode==GL_MODELVIEW) {
            FloatUtil.multMatrix(matrixArray, mMv_offset, m, m_offset);
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            FloatUtil.multMatrix(matrixArray, mP_offset, m, m_offset);
            dirtyBits |= DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            FloatUtil.multMatrix(matrixArray, mTex_offset, m, m_offset);
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    @Override
    public final void glTranslatef(final float x, final float y, final float z) {
        glMultMatrixf(FloatUtil.makeTranslation(matrixTxSx, false, x, y, z), 0);
    }

    @Override
    public final void glScalef(final float x, final float y, final float z) {
        glMultMatrixf(FloatUtil.makeScale(matrixTxSx, false, x, y, z), 0);
    }

    @Override
    public final void glRotatef(final float ang_deg, final float x, final float y, final float z) {
        glMultMatrixf(FloatUtil.makeRotationAxis(mat4Tmp1, 0, ang_deg * FloatUtil.PI / 180.0f, x, y, z, mat4Tmp2), 0);
    }

    /**
     * Rotate the current matrix with the given {@link Quaternion}'s rotation {@link Quaternion#toMatrix(float[], int) matrix representation}.
     */
    public final void glRotate(final Quaternion quat) {
        glMultMatrixf(quat.toMatrix(mat4Tmp1, 0), 0);
    }

    @Override
    public final void glOrthof(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) {
        glMultMatrixf( FloatUtil.makeOrtho(mat4Tmp1, 0, true, left, right, bottom, top, zNear, zFar), 0 );
    }

    /**
     * {@inheritDoc}
     *
     * @throws GLException if {@code zNear <= 0} or {@code zFar <= zNear}
     *                     or {@code left == right}, or {@code bottom == top}.
     * @see FloatUtil#makeFrustum(float[], int, boolean, float, float, float, float, float, float)
     */
    @Override
    public final void glFrustumf(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) throws GLException {
        glMultMatrixf( FloatUtil.makeFrustum(mat4Tmp1, 0, true, left, right, bottom, top, zNear, zFar), 0 );
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
     * @see FloatUtil#makePerspective(float[], int, boolean, float, float, float, float)
     */
    public final void gluPerspective(final float fovy_deg, final float aspect, final float zNear, final float zFar) throws GLException {
      glMultMatrixf( FloatUtil.makePerspective(mat4Tmp1, 0, true, fovy_deg * FloatUtil.PI / 180.0f, aspect, zNear, zFar), 0 );
    }

    /**
     * {@link #glMultMatrixf(FloatBuffer) Multiply} and {@link #glTranslatef(float, float, float) translate} the {@link #glGetMatrixMode() current matrix}
     * with the eye, object and orientation.
     */
    public final void gluLookAt(final float eyex, final float eyey, final float eyez,
                                final float centerx, final float centery, final float centerz,
                                final float upx, final float upy, final float upz) {
        mat4Tmp2[0+0] = eyex;
        mat4Tmp2[1+0] = eyey;
        mat4Tmp2[2+0] = eyez;
        mat4Tmp2[0+4] = centerx;
        mat4Tmp2[1+4] = centery;
        mat4Tmp2[2+4] = centerz;
        mat4Tmp2[0+8] = upx;
        mat4Tmp2[1+8] = upy;
        mat4Tmp2[2+8] = upz;
        glMultMatrixf(
                FloatUtil.makeLookAt(mat4Tmp1, 0, mat4Tmp2 /* eye */, 0, mat4Tmp2 /* center */, 4, mat4Tmp2 /* up */, 8, mat4Tmp3), 0);
    }

    /**
     * Map object coordinates to window coordinates.
     * <p>
     * Traditional <code>gluProject</code> implementation.
     * </p>
     *
     * @param objx
     * @param objy
     * @param objz
     * @param viewport 4 component viewport vector
     * @param viewport_offset
     * @param win_pos 3 component window coordinate, the result
     * @param win_pos_offset
     * @return true if successful, otherwise false (z is 1)
     */
    public final boolean gluProject(final float objx, final float objy, final float objz,
                                    final int[] viewport, final int viewport_offset,
                                    final float[] win_pos, final int win_pos_offset ) {
        return FloatUtil.mapObjToWinCoords(objx, objy, objz,
                          matrixArray, mMv_offset,
                          matrixArray, mP_offset,
                          viewport, viewport_offset,
                          win_pos, win_pos_offset,
                          mat4Tmp1, mat4Tmp2);
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
     * @param viewport 4 component viewport vector
     * @param viewport_offset
     * @param obj_pos 3 component object coordinate, the result
     * @param obj_pos_offset
     * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
     */
    public final boolean gluUnProject(final float winx, final float winy, final float winz,
                                      final int[] viewport, final int viewport_offset,
                                      final float[] obj_pos, final int obj_pos_offset) {
        return FloatUtil.mapWinToObjCoords(winx, winy, winz,
                                           matrixArray, mMv_offset,
                                           matrixArray, mP_offset,
                                           viewport, viewport_offset,
                                           obj_pos, obj_pos_offset,
                                           mat4Tmp1, mat4Tmp2);
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
     * @param modelMatrix 4x4 modelview matrix
     * @param modelMatrix_offset
     * @param projMatrix 4x4 projection matrix
     * @param projMatrix_offset
     * @param viewport 4 component viewport vector
     * @param viewport_offset
     * @param near
     * @param far
     * @param obj_pos 4 component object coordinate, the result
     * @param obj_pos_offset
     * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
     */
    public boolean gluUnProject4(final float winx, final float winy, final float winz, final float clipw,
                                 final int[] viewport, final int viewport_offset,
                                 final float near, final float far,
                                 final float[] obj_pos, final int obj_pos_offset ) {
        return FloatUtil.mapWinToObjCoords(winx, winy, winz, clipw,
                matrixArray, mMv_offset,
                matrixArray, mP_offset,
                viewport, viewport_offset,
                near, far,
                obj_pos, obj_pos_offset,
                mat4Tmp1, mat4Tmp2);
    }

    /**
     * Make given matrix the <i>pick</i> matrix based on given parameters.
     * <p>
     * Traditional <code>gluPickMatrix</code> implementation.
     * </p>
     * <p>
     * See {@link FloatUtil#makePick(float[], int, float, float, float, float, int[], int, float[]) FloatUtil.makePick(..)} for details.
     * </p>
     * @param x the center x-component of a picking region in window coordinates
     * @param y the center y-component of a picking region in window coordinates
     * @param deltaX the width of the picking region in window coordinates.
     * @param deltaY the height of the picking region in window coordinates.
     * @param viewport 4 component viewport vector
     * @param viewport_offset
     */
    public final void gluPickMatrix(final float x, final float y,
                                    final float deltaX, final float deltaY,
                                    final int[] viewport, final int viewport_offset) {
        if( null != FloatUtil.makePick(mat4Tmp1, 0, x, y, deltaX, deltaY, viewport, viewport_offset, mat4Tmp2) ) {
            glMultMatrixf(mat4Tmp1, 0);
        }
    }

    /**
     * Map two window coordinates w/ shared X/Y and distinctive Z
     * to a {@link Ray}. The resulting {@link Ray} maybe used for <i>picking</i>
     * using a {@link AABBox#getRayIntersection(Ray, float[]) bounding box}.
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
     * @param viewport_offset
     * @param ray storage for the resulting {@link Ray}
     * @return true if successful, otherwise false (failed to invert matrix, or becomes z is infinity)
     */
    public final boolean gluUnProjectRay(final float winx, final float winy, final float winz0, final float winz1,
                                         final int[] viewport, final int viewport_offset,
                                         final Ray ray) {
        return FloatUtil.mapWinToRay(winx, winy, winz0, winz1,
                matrixArray, mMv_offset,
                matrixArray, mP_offset,
                viewport, viewport_offset,
                ray,
                mat4Tmp1, mat4Tmp2, mat4Tmp3);
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
        matrixToString(sb, f, matrixP);
        sb.append(", Modelview").append(PlatformPropsImpl.NEWLINE);
        matrixToString(sb, f, matrixMv);
        sb.append(", Texture").append(PlatformPropsImpl.NEWLINE);
        matrixToString(sb, f, matrixTex);
        if( 0 != ( requestMask & DIRTY_INVERSE_MODELVIEW ) ) {
            sb.append(", Inverse Modelview").append(PlatformPropsImpl.NEWLINE);
            matrixToString(sb, f, matrixMvi);
        }
        if( 0 != ( requestMask & DIRTY_INVERSE_TRANSPOSED_MODELVIEW ) ) {
            sb.append(", Inverse Transposed Modelview").append(PlatformPropsImpl.NEWLINE);
            matrixToString(sb, f, matrixMvit);
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
     * has been requested by one of the {@link #glGetMviMatrixf() Mvi get}, {@link #glGetMvitMatrixf() Mvit get}
     * or {@link #glGetFrustum() Frustum get} methods.
     * </p>
     *
     * @deprecated Function is exposed for debugging purposes only.
     * @see #DIRTY_INVERSE_MODELVIEW
     * @see #DIRTY_INVERSE_TRANSPOSED_MODELVIEW
     * @see #DIRTY_FRUSTUM
     * @see #glGetMviMatrixf()
     * @see #glGetMvitMatrixf()
     * @see #glGetPMvMviMatrixf()
     * @see #glGetPMvMvitMatrixf()
     * @see #glGetFrustum()
     */
    public final int getDirtyBits() {
        return dirtyBits;
    }

    /**
     * Returns the request bit mask, which uses bit values equal to the dirty mask.
     * <p>
     * The request bit mask is set by one of the {@link #glGetMviMatrixf() Mvi get}, {@link #glGetMvitMatrixf() Mvit get}
     * or {@link #glGetFrustum() Frustum get} methods.
     * </p>
     *
     * @deprecated Function is exposed for debugging purposes only.
     * @see #clearAllUpdateRequests()
     * @see #DIRTY_INVERSE_MODELVIEW
     * @see #DIRTY_INVERSE_TRANSPOSED_MODELVIEW
     * @see #DIRTY_FRUSTUM
     * @see #glGetMviMatrixf()
     * @see #glGetMvitMatrixf()
     * @see #glGetPMvMviMatrixf()
     * @see #glGetPMvMvitMatrixf()
     * @see #glGetFrustum()
     */
    public final int getRequestMask() {
        return requestMask;
    }


    /**
     * Clears all {@link #update()} requests of the Mvi and Mvit matrix and Frustum
     * after it has been enabled by one of the {@link #glGetMviMatrixf() Mvi get}, {@link #glGetMvitMatrixf() Mvit get}
     * or {@link #glGetFrustum() Frustum get} methods.
     * <p>
     * Allows user to disable subsequent Mvi, Mvit and {@link Frustum} updates if no more required.
     * </p>
     *
     * @see #glGetMviMatrixf()
     * @see #glGetMvitMatrixf()
     * @see #glGetPMvMviMatrixf()
     * @see #glGetPMvMvitMatrixf()
     * @see #glGetFrustum()
     * @see #getRequestMask()
     */
    public final void clearAllUpdateRequests() {
        requestMask &= ~DIRTY_ALL;
    }

    /**
     * Update the derived {@link #glGetMviMatrixf() inverse modelview (Mvi)},
     * {@link #glGetMvitMatrixf() inverse transposed modelview (Mvit)} matrices and {@link Frustum}
     * <b>if</b> they are dirty <b>and</b> they were requested
     * by one of the {@link #glGetMviMatrixf() Mvi get}, {@link #glGetMvitMatrixf() Mvit get}
     * or {@link #glGetFrustum() Frustum get} methods.
     * <p>
     * The Mvi and Mvit matrices and {@link Frustum} are considered dirty, if their corresponding
     * {@link #glGetMvMatrixf() Mv matrix} has been modified since their last update.
     * </p>
     * <p>
     * Method should be called manually in case mutable operations has been called
     * and caller operates on already fetched references, i.e. not calling
     * {@link #glGetMviMatrixf() Mvi get}, {@link #glGetMvitMatrixf() Mvit get}
     * or {@link #glGetFrustum() Frustum get} etc anymore.
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
     * @see #glGetMviMatrixf()
     * @see #glGetMvitMatrixf()
     * @see #glGetPMvMviMatrixf()
     * @see #glGetPMvMvitMatrixf()
     * @see #glGetFrustum()
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
            FloatUtil.multMatrix(matrixArray, mP_offset, matrixArray, mMv_offset, mat4Tmp1, 0);
            // FloatUtil.multMatrix(matrixP, matrixMv, mat4Tmp1, 0);
            frustum.updateByPMV(mat4Tmp1, 0);
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
        final float[] _matrixMvi = matrixMvi.array();
        final int _matrixMviOffset = matrixMvi.position();
        boolean res = false;
        if( 0 != ( dirtyBits & DIRTY_INVERSE_MODELVIEW ) ) { // only if dirt; always requested at this point, see update()
            if( null == FloatUtil.invertMatrix(matrixArray, mMv_offset, _matrixMvi, _matrixMviOffset) ) {
                throw new GLException(msgCantComputeInverse);
            }
            dirtyBits &= ~DIRTY_INVERSE_MODELVIEW;
            res = true;
        }
        if( 0 != ( requestMask & ( dirtyBits & DIRTY_INVERSE_TRANSPOSED_MODELVIEW ) ) ) { // only if requested & dirty
            FloatUtil.transposeMatrix(_matrixMvi, _matrixMviOffset, matrixMvit.array(), matrixMvit.position());
            dirtyBits &= ~DIRTY_INVERSE_TRANSPOSED_MODELVIEW;
            res = true;
        }
        return res;
    }

    private final float[] matrixArray;
    private final int mP_offset, mMv_offset, mTex_offset;
    private final FloatBuffer matrixPMvMvit, matrixPMvMvi, matrixPMv, matrixP, matrixTex, matrixMv, matrixMvi, matrixMvit;
    private final float[] matrixTxSx;
    private final float[] mat4Tmp1, mat4Tmp2, mat4Tmp3;
    private final FloatStack matrixTStack, matrixPStack, matrixMvStack;
    private int matrixMode = GL_MODELVIEW;
    private int modifiedBits = MODIFIED_ALL;
    private int dirtyBits = DIRTY_ALL; // contains the dirty bits, i.e. hinting for update operation
    private int requestMask = 0; // may contain the requested dirty bits: DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW
    private Frustum frustum;
}
