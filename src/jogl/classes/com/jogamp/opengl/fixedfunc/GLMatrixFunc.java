/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.jogamp.opengl.fixedfunc;

import java.nio.*;

import com.jogamp.opengl.GL;

/**
 * Subset of OpenGL fixed function pipeline's matrix operations.
 */
public interface GLMatrixFunc {

    public static final int GL_MATRIX_MODE = 0x0BA0;
    /** Matrix mode modelview */
    public static final int GL_MODELVIEW = 0x1700;
    /** Matrix mode projection */
    public static final int GL_PROJECTION = 0x1701;
    // public static final int GL_TEXTURE = 0x1702; // Use GL.GL_TEXTURE due to ambiguous GL usage
    /** Matrix access name for modelview */
    public static final int GL_MODELVIEW_MATRIX = 0x0BA6;
    /** Matrix access name for projection */
    public static final int GL_PROJECTION_MATRIX = 0x0BA7;
    /** Matrix access name for texture */
    public static final int GL_TEXTURE_MATRIX = 0x0BA8;

    /**
     * Copy the named matrix into the given storage.
     * @param pname {@link #GL_MODELVIEW_MATRIX}, {@link #GL_PROJECTION_MATRIX} or {@link #GL_TEXTURE_MATRIX}
     * @param params the FloatBuffer's position remains unchanged,
     *        which is the same behavior than the native JOGL GL impl
     */
    public void glGetFloatv(int pname, java.nio.FloatBuffer params);

    /**
     * Copy the named matrix to the given storage at offset.
     * @param pname {@link #GL_MODELVIEW_MATRIX}, {@link #GL_PROJECTION_MATRIX} or {@link #GL_TEXTURE_MATRIX}
     * @param params storage
     * @param params_offset storage offset
     */
    public void glGetFloatv(int pname, float[] params, int params_offset);

    /**
     * glGetIntegerv
     * @param pname {@link #GL_MATRIX_MODE} to receive the current matrix mode
     * @param params the FloatBuffer's position remains unchanged
     *        which is the same behavior than the native JOGL GL impl
     */
    public void glGetIntegerv(int pname, IntBuffer params);
    public void glGetIntegerv(int pname, int[] params, int params_offset);

    /**
     * Sets the current matrix mode.
     * @param mode {@link #GL_MODELVIEW}, {@link #GL_PROJECTION} or {@link GL#GL_TEXTURE GL_TEXTURE}.
     */
    public void glMatrixMode(int mode) ;

    /**
     * Push the current matrix to it's stack, while preserving it's values.
     * <p>
     * There exist one stack per matrix mode, i.e. {@link #GL_MODELVIEW}, {@link #GL_PROJECTION} and {@link GL#GL_TEXTURE GL_TEXTURE}.
     * </p>
     */
    public void glPushMatrix();

    /**
     * Pop the current matrix from it's stack.
     * @see #glPushMatrix()
     */
    public void glPopMatrix();

    /**
     * Load the current matrix with the identity matrix
     */
    public void glLoadIdentity() ;

    /**
     * Load the current matrix w/ the provided one.
     * @param params the FloatBuffer's position remains unchanged,
     *        which is the same behavior than the native JOGL GL impl
     */
    public void glLoadMatrixf(java.nio.FloatBuffer m) ;
    /**
     * Load the current matrix w/ the provided one.
     */
    public void glLoadMatrixf(float[] m, int m_offset);

    /**
     * Multiply the current matrix: [c] = [c] x [m]
     * @param m the FloatBuffer's position remains unchanged,
     *        which is the same behavior than the native JOGL GL impl
     */
    public void glMultMatrixf(java.nio.FloatBuffer m) ;
    /**
     * Multiply the current matrix: [c] = [c] x [m]
     */
    public void glMultMatrixf(float[] m, int m_offset);

    /**
     * Translate the current matrix.
     */
    public void glTranslatef(float x, float y, float z) ;

    /**
     * Rotate the current matrix.
     */
    public void glRotatef(float angle, float x, float y, float z);

    /**
     * Scale the current matrix.
     */
    public void glScalef(float x, float y, float z) ;

    /**
     * {@link #glMultMatrixf(FloatBuffer) Multiply} the current matrix with the orthogonal matrix.
     */
    public void glOrthof(float left, float right, float bottom, float top, float zNear, float zFar) ;

    /**
     * {@link #glMultMatrixf(FloatBuffer) Multiply} the current matrix with the frustum matrix.
     */
    public void glFrustumf(float left, float right, float bottom, float top, float zNear, float zFar);

}

