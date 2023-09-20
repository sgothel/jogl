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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.math.FloatUtil;
import com.jogamp.math.Matrix4f;
import com.jogamp.math.Quaternion;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec3f;
import com.jogamp.math.geom.Frustum;
import com.jogamp.math.util.PMVMatrix4f;

/**
 * PMVMatrix implements a subset of the fixed function pipeline {@link GLMatrixFunc}
 * using {@link PMVMatrix4f}.
 * <p>
 * PMVMatrix provides the {@link #getMvi() inverse modelview matrix (Mvi)} and
 * {@link #getMvit() inverse transposed modelview matrix (Mvit)}.
 * {@link Frustum} is also provided by {@link #getFrustum()}.
 *
 * To keep these derived values synchronized after mutable Mv operations like {@link #glRotatef(float, float, float, float) glRotatef(..)}
 * in {@link #glMatrixMode(int) glMatrixMode}({@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW}),
 * users have to call {@link #update()} before using Mvi and Mvit.
 * </p>
 * <p>
 * PMVMatrix can supplement {@link com.jogamp.opengl.GL2ES2 GL2ES2} applications w/ the
 * lack of the described matrix functionality.
 * </p>
 */
public final class PMVMatrix extends PMVMatrix4f implements GLMatrixFunc {

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
        super(derivedMatrices);
    }


    /**
     * {@inheritDoc}
     * <p>
     * Leaves {@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW} the active matrix mode.
     * </p>
     */
    @Override
    public void reset() {
        super.reset();
        matrixMode = GL_MODELVIEW;
    }

    //
    // GLMatrixFunc implementation
    //

    /** Returns the current matrix-mode, one of {@link GLMatrixFunc#GL_MODELVIEW GL_MODELVIEW}, {@link GLMatrixFunc#GL_PROJECTION GL_PROJECTION} or {@link GL#GL_TEXTURE GL_TEXTURE}. */
    public final int glGetMatrixMode() {
        return matrixMode;
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
            loadMv(values, offset);
        } else if(matrixMode==GL_PROJECTION) {
            loadP(values, offset);
        } else if(matrixMode==GL.GL_TEXTURE) {
            loadT(values, offset);
        }
    }

    @Override
    public final void glLoadMatrixf(final java.nio.FloatBuffer m) {
        if(matrixMode==GL_MODELVIEW) {
            loadMv(m);
        } else if(matrixMode==GL_PROJECTION) {
            loadP(m);
        } else if(matrixMode==GL.GL_TEXTURE) {
            loadT(m);
        }
    }

    /**
     * Load the current matrix with the values of the given {@link Matrix4f}.
     * <p>
     * Extension to {@link GLMatrixFunc}.
     * </p>
     */
    public final void glLoadMatrixf(final Matrix4f m) {
        if(matrixMode==GL_MODELVIEW) {
            loadMv(m);
        } else if(matrixMode==GL_PROJECTION) {
            loadP(m);
        } else if(matrixMode==GL.GL_TEXTURE) {
            loadT(m);
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
            loadMv(quat);
        } else if(matrixMode==GL_PROJECTION) {
            loadP(quat);
        } else if(matrixMode==GL.GL_TEXTURE) {
            loadT(quat);
        }
    }

    @Override
    public final void glLoadIdentity() {
        if(matrixMode==GL_MODELVIEW) {
            loadMvIdentity();
        } else if(matrixMode==GL_PROJECTION) {
            loadPIdentity();
        } else if(matrixMode==GL.GL_TEXTURE) {
            loadTIdentity();
        }
    }

    @Override
    public final void glMultMatrixf(final FloatBuffer m) {
        final int spos = m.position();
        if(matrixMode==GL_MODELVIEW) {
            mulMv( mat4Tmp1.load( m ) );
        } else if(matrixMode==GL_PROJECTION) {
            mulP( mat4Tmp1.load( m ) );
        } else if(matrixMode==GL.GL_TEXTURE) {
            mulT( mat4Tmp1.load( m ) );
        }
        m.position(spos);
    }

    @Override
    public final void glMultMatrixf(final float[] m, final int m_offset) {
        if(matrixMode==GL_MODELVIEW) {
            mulMv( mat4Tmp1.load( m, m_offset ) );
        } else if(matrixMode==GL_PROJECTION) {
            mulP( mat4Tmp1.load( m, m_offset ) );
        } else if(matrixMode==GL.GL_TEXTURE) {
            mulT( mat4Tmp1.load( m, m_offset ) );
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
            mulMv(m);
        } else if(matrixMode==GL_PROJECTION) {
            mulP(m);
        } else if(matrixMode==GL.GL_TEXTURE) {
            mulT(m);
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
    public final void glPopMatrix() {
        if(matrixMode==GL_MODELVIEW) {
            popMv();
        } else if(matrixMode==GL_PROJECTION) {
            popP();
        } else if(matrixMode==GL.GL_TEXTURE) {
            popT();
        }
    }

    @Override
    public final void glPushMatrix() {
        if(matrixMode==GL_MODELVIEW) {
            pushMv();
        } else if(matrixMode==GL_PROJECTION) {
            pushP();
        } else if(matrixMode==GL.GL_TEXTURE) {
            pushT();
        }
    }

    @Override
    public final void glOrthof(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) {
        glMultMatrixf( mat4Tmp1.setToOrtho(left, right, bottom, top, zNear, zFar) );
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code zNear <= 0} or {@code zFar <= zNear}
     *                                  or {@code left == right}, or {@code bottom == top}.
     * @see Matrix4f#setToFrustum(float, float, float, float, float, float)
     */
    @Override
    public final void glFrustumf(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) throws IllegalArgumentException {
        glMultMatrixf( mat4Tmp1.setToFrustum(left, right, bottom, top, zNear, zFar) );
    }

    //
    // Extra functionality
    //

    /**
     * {@link #glMultMatrixf(FloatBuffer) Multiply} the {@link #glGetMatrixMode() current matrix} with the perspective/frustum matrix.
     *
     * @param fovy_rad fov angle in radians
     * @param aspect aspect ratio width / height
     * @param zNear
     * @param zFar
     * @throws IllegalArgumentException if {@code zNear <= 0} or {@code zFar <= zNear}
     * @see Matrix4f#setToPerspective(float, float, float, float)
     */
    public final void gluPerspective(final float fovy_rad, final float aspect, final float zNear, final float zFar) throws IllegalArgumentException {
         glMultMatrixf( mat4Tmp1.setToPerspective(fovy_rad, aspect, zNear, zFar) );
    }

    /**
     * {@link #glMultMatrixf(FloatBuffer) Multiply} the {@link #glGetMatrixMode() current matrix}
     * with the eye, object and orientation, i.e. {@link Matrix4f#setToLookAt(Vec3f, Vec3f, Vec3f, Matrix4f)}.
     */
    public final void gluLookAt(final Vec3f eye, final Vec3f center, final Vec3f up) {
        glMultMatrixf( mat4Tmp1.setToLookAt(eye, center, up, getTmp2Mat()) );
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
    public final void gluPickMatrix(final float x, final float y, final float deltaX, final float deltaY, final Recti viewport) {
        if( null != mat4Tmp1.setToPick(x, y, deltaX, deltaY, viewport, getTmp2Mat()) ) {
            glMultMatrixf( mat4Tmp1 );
        }
    }

    //
    // private
    //

    private int matrixMode = GL_MODELVIEW;
}
