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

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GLException;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import jogamp.opengl.ProjectFloat;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.FloatStack;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.Ray;
import com.jogamp.opengl.math.VectorUtil;
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
 * All matrices use a common FloatBuffer storage
 * and are a {@link Buffers#slice2Float(Buffer, float[], int, int) sliced} representation of it.
 * The common FloatBuffer and hence all matrices may use NIO direct storage or a {@link #usesBackingArray() backing float array},
 * depending how the instance if {@link #PMVMatrix(boolean) being constructed}.
 * </p>
 * <p>
 * <b>Note:</b>
 * <ul>
 *   <li>The matrix is a {@link Buffers#slice2Float(Buffer, float[], int, int) sliced part } of a host matrix and it's start position has been {@link FloatBuffer#mark() marked}.</li>
 *   <li>Use {@link FloatBuffer#reset() reset()} to rewind it to it's start position after relative operations, like {@link FloatBuffer#get() get()}.</li>
 *   <li>If using absolute operations like {@link FloatBuffer#get(int) get(int)}, use it's {@link FloatBuffer#reset() reset} {@link FloatBuffer#position() position} as it's offset.</li>
 * </ul>
 * </p>
 */
public class PMVMatrix implements GLMatrixFunc {

    /** Bit value stating a modified {@link #glGetPMatrixf() projection matrix (P)}, since last {@link #update()} call. */
    public static final int MODIFIED_PROJECTION                    = 1 << 0;
    /** Bit value stating a modified {@link #glGetMvMatrixf() modelview matrix (Mv)}, since last {@link #update()} call. */
    public static final int MODIFIED_MODELVIEW                     = 1 << 1;
    /** Bit value stating a modified {@link #glGetTMatrixf() texture matrix (T)}, since last {@link #update()} call. */
    public static final int MODIFIED_TEXTURE                       = 1 << 2;
    /** Bit value stating all is modified */
    public static final int MODIFIED_ALL                           = MODIFIED_PROJECTION | MODIFIED_MODELVIEW | MODIFIED_TEXTURE ;

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
    public static StringBuilder matrixToString(StringBuilder sb, String f, FloatBuffer a) {
        return FloatUtil.matrixToString(sb, null, f, a, 0, 4, 4, false);
    }

    /**
     * @param sb optional passed StringBuilder instance to be used
     * @param f the format string of one floating point, i.e. "%10.5f", see {@link java.util.Formatter}
     * @param a 4x4 matrix in column major order (OpenGL)
     * @param b 4x4 matrix in column major order (OpenGL)
     * @return side by side representation
     */
    public static StringBuilder matrixToString(StringBuilder sb, String f, FloatBuffer a, FloatBuffer b) {
        return FloatUtil.matrixToString(sb, null, f, a, 0, b, 0, 4, 4, false);
    }

    /**
     * Creates an instance of PMVMatrix {@link #PMVMatrix(boolean) PMVMatrix(boolean useBackingArray)},
     * with <code>useBackingArray = true</code>.
     */
    public PMVMatrix() {
        this(true);
    }

    /**
     * Creates an instance of PMVMatrix.
     *
     * @param useBackingArray <code>true</code> for non direct NIO Buffers with guaranteed backing array,
     *                        which allows faster access in Java computation.
     *                        <p><code>false</code> for direct NIO buffers w/o a guaranteed backing array.
     *                        In most Java implementations, direct NIO buffers have no backing array
     *                        and hence the Java computation will be throttled down by direct IO get/put
     *                        operations.</p>
     *                        <p>Depending on the application, ie. whether the Java computation or
     *                        JNI invocation and hence native data transfer part is heavier,
     *                        this flag shall be set to <code>true</code> or <code>false</code></p>.
     */
    public PMVMatrix(boolean useBackingArray) {
          this.usesBackingArray = useBackingArray;

          // I    Identity
          // T    Texture
          // P    Projection
          // Mv   ModelView
          // Mvi  Modelview-Inverse
          // Mvit Modelview-Inverse-Transpose
          if(useBackingArray) {
              matrixBufferArray = new float[ 6*16 + ProjectFloat.getRequiredFloatBufferSize() ];
              matrixBuffer = null;
          } else {
              matrixBufferArray = null;
              matrixBuffer = Buffers.newDirectByteBuffer( ( 6*16 + ProjectFloat.getRequiredFloatBufferSize() ) * Buffers.SIZEOF_FLOAT );
              matrixBuffer.mark();
          }

          matrixIdent   = Buffers.slice2Float(matrixBuffer, matrixBufferArray,  0*16, 1*16);  //  I
          matrixTex     = Buffers.slice2Float(matrixBuffer, matrixBufferArray,  1*16, 1*16);  //      T
          matrixPMvMvit = Buffers.slice2Float(matrixBuffer, matrixBufferArray,  2*16, 4*16);  //          P  + Mv + Mvi + Mvit
          matrixPMvMvi  = Buffers.slice2Float(matrixBuffer, matrixBufferArray,  2*16, 3*16);  //          P  + Mv + Mvi
          matrixPMv     = Buffers.slice2Float(matrixBuffer, matrixBufferArray,  2*16, 2*16);  //          P  + Mv
          matrixP       = Buffers.slice2Float(matrixBuffer, matrixBufferArray,  2*16, 1*16);  //          P
          matrixMv      = Buffers.slice2Float(matrixBuffer, matrixBufferArray,  3*16, 1*16);  //               Mv
          matrixMvi     = Buffers.slice2Float(matrixBuffer, matrixBufferArray,  4*16, 1*16);  //                    Mvi
          matrixMvit    = Buffers.slice2Float(matrixBuffer, matrixBufferArray,  5*16, 1*16);  //                          Mvit

          projectFloat  = new ProjectFloat(matrixBuffer, matrixBufferArray, 6*16);

          if(null != matrixBuffer) {
              matrixBuffer.reset();
          }
          FloatUtil.makeIdentityf(matrixIdent);

          tmpVec3f      = new float[3];
          tmpMatrix     = new float[16];
          matrixRot     = new float[16];
          matrixMult    = new float[16];
          matrixTrans   = new float[16];
          matrixScale   = new float[16];
          matrixOrtho   = new float[16];
          matrixFrustum = new float[16];
          FloatUtil.makeIdentityf(matrixTrans, 0);
          FloatUtil.makeIdentityf(matrixRot, 0);
          FloatUtil.makeIdentityf(matrixScale, 0);
          FloatUtil.makeIdentityf(matrixOrtho, 0);
          FloatUtil.makeZero(matrixFrustum, 0);

          // Start w/ zero size to save memory
          matrixTStack = new FloatStack( 0,  2*16); // growSize: GL-min size (2)
          matrixPStack = new FloatStack( 0,  2*16); // growSize: GL-min size (2)
          matrixMvStack= new FloatStack( 0, 16*16); // growSize: half GL-min size (32)

          // default values and mode
          glMatrixMode(GL_PROJECTION);
          glLoadIdentity();
          glMatrixMode(GL_MODELVIEW);
          glLoadIdentity();
          glMatrixMode(GL.GL_TEXTURE);
          glLoadIdentity();
          modifiedBits = MODIFIED_ALL;
          dirtyBits = DIRTY_ALL;
          requestMask = 0;
          matrixMode = GL_MODELVIEW;

          frustum = null;
    }

    /** @see #PMVMatrix(boolean) */
    public final boolean usesBackingArray() { return usesBackingArray; }

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
    public Frustum glGetFrustum() {
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
     * @return the named matrix
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
        int pos = params.position();
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
    public final void glGetFloatv(final int matrixGetName, float[] params, final int params_offset) {
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
        int pos = params.position();
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
        int spos = m.position();
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
            quat.toMatrix(matrixMv);
            matrixMv.reset();
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            quat.toMatrix(matrixP);
            matrixP.reset();
            dirtyBits |= DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            quat.toMatrix(matrixTex);
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
            matrixMv.put(matrixIdent);
            matrixMv.reset();
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            matrixP.put(matrixIdent);
            matrixP.reset();
            dirtyBits |= DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matrixTex.put(matrixIdent);
            matrixTex.reset();
            modifiedBits |= MODIFIED_TEXTURE;
        }
        matrixIdent.reset();
    }

    @Override
    public final void glMultMatrixf(final FloatBuffer m) {
        if(matrixMode==GL_MODELVIEW) {
            FloatUtil.multMatrixf(matrixMv, m);
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            FloatUtil.multMatrixf(matrixP, m);
            dirtyBits |= DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            FloatUtil.multMatrixf(matrixTex, m);
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    @Override
    public final void glMultMatrixf(final float[] m, final int m_offset) {
        if(matrixMode==GL_MODELVIEW) {
            FloatUtil.multMatrixf(matrixMv, m, m_offset);
            dirtyBits |= DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW | DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_MODELVIEW;
        } else if(matrixMode==GL_PROJECTION) {
            FloatUtil.multMatrixf(matrixP, m, m_offset);
            dirtyBits |= DIRTY_FRUSTUM ;
            modifiedBits |= MODIFIED_PROJECTION;
        } else if(matrixMode==GL.GL_TEXTURE) {
            FloatUtil.multMatrixf(matrixTex, m, m_offset);
            modifiedBits |= MODIFIED_TEXTURE;
        }
    }

    @Override
    public final void glTranslatef(final float x, final float y, final float z) {
        // Translation matrix (Column Order):
        //  1 0 0 0
        //  0 1 0 0
        //  0 0 1 0
        //  x y z 1
        matrixTrans[0+4*3] = x;
        matrixTrans[1+4*3] = y;
        matrixTrans[2+4*3] = z;
        glMultMatrixf(matrixTrans, 0);
    }

    @Override
    public final void glRotatef(final float angdeg, final float x, final float y, final float z) {
        final float angrad = angdeg   * FloatUtil.PI / 180.0f;
        FloatUtil.makeRotationAxis(angrad, x, y, z, matrixRot, 0, tmpVec3f);
        glMultMatrixf(matrixRot, 0);
    }

    /**
     * Rotate the current matrix with the given {@link Quaternion}'s rotation {@link Quaternion#toMatrix(float[], int) matrix representation}.
     */
    public final void glRotate(final Quaternion quat) {
        quat.toMatrix(tmpMatrix, 0);
        glMultMatrixf(tmpMatrix, 0);
    }

    @Override
    public final void glScalef(final float x, final float y, final float z) {
        // Scale matrix (Any Order):
        //  x 0 0 0
        //  0 y 0 0
        //  0 0 z 0
        //  0 0 0 1
        matrixScale[0+4*0] = x;
        matrixScale[1+4*1] = y;
        matrixScale[2+4*2] = z;

        glMultMatrixf(matrixScale, 0);
    }

    @Override
    public final void glOrthof(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) {
        // Ortho matrix (Column Order):
        //  2/dx  0     0    0
        //  0     2/dy  0    0
        //  0     0     2/dz 0
        //  tx    ty    tz   1
        final float dx=right-left;
        final float dy=top-bottom;
        final float dz=zFar-zNear;
        final float tx=-1.0f*(right+left)/dx;
        final float ty=-1.0f*(top+bottom)/dy;
        final float tz=-1.0f*(zFar+zNear)/dz;

        matrixOrtho[0+4*0] =  2.0f/dx;
        matrixOrtho[1+4*1] =  2.0f/dy;
        matrixOrtho[2+4*2] = -2.0f/dz;
        matrixOrtho[0+4*3] = tx;
        matrixOrtho[1+4*3] = ty;
        matrixOrtho[2+4*3] = tz;

        glMultMatrixf(matrixOrtho, 0);
    }

    @Override
    public final void glFrustumf(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) {
        if(zNear<=0.0f||zFar<0.0f) {
            throw new GLException("GL_INVALID_VALUE: zNear and zFar must be positive, and zNear>0");
        }
        if(left==right || top==bottom) {
            throw new GLException("GL_INVALID_VALUE: top,bottom and left,right must not be equal");
        }
        // Frustum matrix (Column Order):
        //  2*zNear/dx   0          0   0
        //  0            2*zNear/dy 0   0
        //  A            B          C  -1
        //  0            0          D   0
        final float zNear2 = 2.0f*zNear;
        final float dx=right-left;
        final float dy=top-bottom;
        final float dz=zFar-zNear;
        final float A=(right+left)/dx;
        final float B=(top+bottom)/dy;
        final float C=-1.0f*(zFar+zNear)/dz;
        final float D=-2.0f*(zFar*zNear)/dz;

        matrixFrustum[0+4*0] = zNear2/dx;
        matrixFrustum[1+4*1] = zNear2/dy;
        matrixFrustum[2+4*2] = C;

        matrixFrustum[0+4*2] = A;
        matrixFrustum[1+4*2] = B;

        matrixFrustum[2+4*3] = D;
        matrixFrustum[3+4*2] = -1.0f;

        glMultMatrixf(matrixFrustum, 0);
    }

    //
    // Extra functionality
    //

    /**
     * {@link #glMultMatrixf(FloatBuffer) Multiply} the {@link #glGetMatrixMode() current matrix} with the perspective/frustum matrix.
     */
    public final void gluPerspective(final float fovy, final float aspect, final float zNear, final float zFar) {
      float top=(float)Math.tan(fovy*((float)Math.PI)/360.0f)*zNear;
      float bottom=-1.0f*top;
      float left=aspect*bottom;
      float right=aspect*top;
      glFrustumf(left, right, bottom, top, zNear, zFar);
    }

    /**
     * {@link #glMultMatrixf(FloatBuffer) Multiply} and {@link #glTranslatef(float, float, float) translate} the {@link #glGetMatrixMode() current matrix}
     * with the eye, object and orientation.
     */
    public final void gluLookAt(final float eyex, final float eyey, final float eyez,
                                final float centerx, final float centery, final float centerz,
                                final float upx, final float upy, final float upz) {
        projectFloat.gluLookAt(this, eyex, eyey, eyez, centerx, centery, centerz, upx, upy, upz);
    }

    /**
     * Map object coordinates to window coordinates.
     *
     * @param objx
     * @param objy
     * @param objz
     * @param viewport
     * @param viewport_offset
     * @param win_pos
     * @param win_pos_offset
     * @return
     */
    public final boolean gluProject(final float objx, final float objy, final float objz,
                                    final int[] viewport, final int viewport_offset,
                                    final float[] win_pos, final int win_pos_offset ) {
        if(usesBackingArray) {
            return projectFloat.gluProject(objx, objy, objz,
                                           matrixMv.array(), matrixMv.position(),
                                           matrixP.array(), matrixP.position(),
                                           viewport, viewport_offset,
                                           win_pos, win_pos_offset);
        } else {
            return projectFloat.gluProject(objx, objy, objz,
                                           matrixMv,
                                           matrixP,
                                           viewport, viewport_offset,
                                           win_pos, win_pos_offset);
        }
    }

    /**
     * Map window coordinates to object coordinates.
     *
     * @param winx
     * @param winy
     * @param winz
     * @param viewport
     * @param viewport_offset
     * @param obj_pos
     * @param obj_pos_offset
     * @return true if successful, otherwise false (failed to invert matrix, or becomes z is infinity)
     */
    public final boolean gluUnProject(final float winx, final float winy, final float winz,
                                      final int[] viewport, final int viewport_offset,
                                      final float[] obj_pos, final int obj_pos_offset) {
        if(usesBackingArray) {
            return projectFloat.gluUnProject(winx, winy, winz,
                                             matrixMv.array(), matrixMv.position(),
                                             matrixP.array(), matrixP.position(),
                                             viewport, viewport_offset,
                                             obj_pos, obj_pos_offset);
        } else {
            return projectFloat.gluUnProject(winx, winy, winz,
                                             matrixMv,
                                             matrixP,
                                             viewport, viewport_offset,
                                             obj_pos, obj_pos_offset);
        }
    }

    public final void gluPickMatrix(final float x, final float y,
                                    final float deltaX, final float deltaY,
                                    final int[] viewport, final int viewport_offset) {
        projectFloat.gluPickMatrix(this, x, y, deltaX, deltaY, viewport, viewport_offset);
    }

    /**
     * Map two window coordinates w/ shared X/Y and distinctive Z
     * to a {@link Ray}. The resulting {@link Ray} maybe used for <i>picking</i>
     * using a {@link AABBox#intersectsRay(Ray, float[]) bounding box}.
     * <p>
     * Notes for picking <i>winz0</i> and <i>winz1</i>:
     * </p>
     * <p>
     * <a href="http://www.sjbaker.org/steve/omniv/love_your_z_buffer.html">Love Your Z-Buffer</a>
     * <pre>
     *  delta = z * z / ( zNear * (1&lt;&lt;N) - z )
     *
     *  Where:
     *    N     = number of bits of Z precision
     *    zNear = distance from eye to near clip plane
     *    z     = distance from the eye to the object
     *    delta = the smallest resolvable Z separation at this range.
     * </pre>
     * Another equation to determine winZ for 'orthoDist' > 0
     * <pre>
     *  winZ = (1f/zNear-1f/orthoDist)/(1f/zNear-1f/zFar);
     * </pre>
     * </p>
     *
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
        if( gluUnProject(winx, winy, winz0, viewport, viewport_offset, ray.orig, 0) &&
            gluUnProject(winx, winy, winz1, viewport, viewport_offset, ray.dir, 0) ) {
            VectorUtil.normalizeVec3( VectorUtil.subVec3(ray.dir, ray.dir, ray.orig) );
            return true;
        } else {
            return false;
        }
    }

    public StringBuilder toString(StringBuilder sb, String f) {
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

        sb.append("PMVMatrix[backingArray ").append(this.usesBackingArray());
        sb.append(", modified[P ").append(modP).append(", Mv ").append(modMv).append(", T ").append(modT);
        sb.append("], dirty/req[Mvi ").append(mviDirty).append("/").append(mviReq).append(", Mvit ").append(mvitDirty).append("/").append(mvitReq).append(", Frustum ").append(frustumDirty).append("/").append(frustumReq);
        sb.append("], Projection").append(Platform.NEWLINE);
        matrixToString(sb, f, matrixP);
        sb.append(", Modelview").append(Platform.NEWLINE);
        matrixToString(sb, f, matrixMv);
        sb.append(", Texture").append(Platform.NEWLINE);
        matrixToString(sb, f, matrixTex);
        if( 0 != ( requestMask & DIRTY_INVERSE_MODELVIEW ) ) {
            sb.append(", Inverse Modelview").append(Platform.NEWLINE);
            matrixToString(sb, f, matrixMvi);
        }
        if( 0 != ( requestMask & DIRTY_INVERSE_TRANSPOSED_MODELVIEW ) ) {
            sb.append(", Inverse Transposed Modelview").append(Platform.NEWLINE);
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
    public final int getModifiedBits(boolean clear) {
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
    private final boolean updateImpl(boolean clearModBits) {
        boolean mod = 0 != modifiedBits;
        if(clearModBits) {
            modifiedBits = 0;
        }

        if( 0 != ( dirtyBits & ( DIRTY_FRUSTUM & requestMask ) ) ) {
            if( null == frustum ) {
                frustum = new Frustum();
            }
            FloatUtil.multMatrixf(matrixP, matrixMv, tmpMatrix, 0);
            frustum.updateByPMV(tmpMatrix, 0);
            dirtyBits &= ~DIRTY_FRUSTUM;
            mod = true;
        }

        if( 0 == ( dirtyBits & requestMask ) ) {
            return mod; // nothing more requested which may have been dirty
        }

        if(nioBackupArraySupported>=0) {
            try {
                nioBackupArraySupported = 1;
                return setMviMvitNIOBackupArray() || mod;
            } catch(UnsupportedOperationException uoe) {
                nioBackupArraySupported = -1;
            }
        }
        return setMviMvitNIODirectAccess() || mod;
    }

    //
    // private
    //
    private int nioBackupArraySupported = 0; // -1 not supported, 0 - TBD, 1 - supported
    private final String msgCantComputeInverse = "Invalid source Mv matrix, can't compute inverse";

    private final boolean setMviMvitNIOBackupArray() {
        final float[] _matrixMvi = matrixMvi.array();
        final int _matrixMviOffset = matrixMvi.position();
        boolean res = false;
        if( 0 != ( dirtyBits & DIRTY_INVERSE_MODELVIEW ) ) { // only if dirt; always requested at this point, see update()
            if(!projectFloat.gluInvertMatrixf(matrixMv.array(), matrixMv.position(), _matrixMvi, _matrixMviOffset)) {
                throw new GLException(msgCantComputeInverse);
            }
            dirtyBits &= ~DIRTY_INVERSE_MODELVIEW;
            res = true;
        }
        if( 0 != ( requestMask & ( dirtyBits & DIRTY_INVERSE_TRANSPOSED_MODELVIEW ) ) ) { // only if requested & dirty
            // transpose matrix
            final float[] _matrixMvit = matrixMvit.array();
            final int _matrixMvitOffset = matrixMvit.position();
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    _matrixMvit[_matrixMvitOffset+j+i*4] = _matrixMvi[_matrixMviOffset+i+j*4];
                }
            }
            dirtyBits &= ~DIRTY_INVERSE_TRANSPOSED_MODELVIEW;
            res = true;
        }
        return res;
    }

    private final boolean setMviMvitNIODirectAccess() {
        boolean res = false;
        if( 0 != ( dirtyBits & DIRTY_INVERSE_MODELVIEW ) ) { // only if dirt; always requested at this point, see update()
            if(!projectFloat.gluInvertMatrixf(matrixMv, matrixMvi)) {
                throw new GLException(msgCantComputeInverse);
            }
            dirtyBits &= ~DIRTY_INVERSE_MODELVIEW;
            res = true;
        }
        if( 0 != ( requestMask & ( dirtyBits & DIRTY_INVERSE_TRANSPOSED_MODELVIEW ) ) ) { // only if requested & dirty
            // transpose matrix
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    matrixMvit.put(j+i*4, matrixMvi.get(i+j*4));
                }
            }
            dirtyBits &= ~DIRTY_INVERSE_TRANSPOSED_MODELVIEW;
            res = true;
        }
        return res;
    }

    protected final boolean usesBackingArray;
    protected final float[] matrixBufferArray;
    protected final Buffer matrixBuffer;
    protected final FloatBuffer matrixIdent, matrixPMvMvit, matrixPMvMvi, matrixPMv, matrixP, matrixTex, matrixMv, matrixMvi, matrixMvit;
    protected final float[] matrixMult, matrixTrans, matrixRot, matrixScale, matrixOrtho, matrixFrustum, tmpVec3f;
    protected final float[] tmpMatrix;
    protected final FloatStack matrixTStack, matrixPStack, matrixMvStack;
    protected final ProjectFloat projectFloat;
    protected int matrixMode = GL_MODELVIEW;
    protected int modifiedBits = MODIFIED_ALL;
    protected int dirtyBits = DIRTY_ALL; // contains the dirty bits, i.e. hinting for update operation
    protected int requestMask = 0; // may contain the requested dirty bits: DIRTY_INVERSE_MODELVIEW | DIRTY_INVERSE_TRANSPOSED_MODELVIEW
    protected Frustum frustum;
}
