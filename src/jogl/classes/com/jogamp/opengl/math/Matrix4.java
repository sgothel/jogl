/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.math;

import com.jogamp.opengl.GLException;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.opengl.util.PMVMatrix;

/**
 * Simple float array-backed float 4x4 matrix
 * exposing {@link FloatUtil} matrix functionality in an object oriented manner.
 * <p>
 * Unlike {@link PMVMatrix}, this class only represents one single matrix
 * without a complete {@link GLMatrixFunc} implementation,
 * allowing this class to be more lightweight.
 * </p>
 * <p>
 * Implementation is not mature - WIP and subject to change.
 * </p>
 */
public class Matrix4 {

    public Matrix4() {
          matrix     = new float[16];
          matrixTxSx = new float[16];
          mat4Tmp1   = new float[16];
          vec4Tmp1   = new float[4];
          FloatUtil.makeIdentity(matrixTxSx);
          loadIdentity();
    }

    public final float[] getMatrix() {
        return matrix;
    }

    public final void loadIdentity() {
        FloatUtil.makeIdentity(matrix);
    }

    /**
     * Multiply matrix: [this] = [this] x [m]
     * @param m 4x4 matrix in column-major order
     */
    public final void multMatrix(final float[] m, final int m_offset) {
        FloatUtil.multMatrix(matrix, 0, m, m_offset);
    }

    /**
     * Multiply matrix: [this] = [this] x [m]
     * @param m 4x4 matrix in column-major order
     */
    public final void multMatrix(final float[] m) {
        FloatUtil.multMatrix(matrix, m);
    }

    /**
     * Multiply matrix: [this] = [this] x [m]
     * @param m 4x4 matrix in column-major order
     */
    public final void multMatrix(final Matrix4 m) {
        FloatUtil.multMatrix(matrix, m.getMatrix());
    }

    /**
     * @param v_in 4-component column-vector
     * @param v_out this * v_in
     */
    public final void multVec(final float[] v_in, final float[] v_out) {
        FloatUtil.multMatrixVec(matrix, v_in, v_out);
    }

    /**
     * @param v_in 4-component column-vector
     * @param v_out this * v_in
     */
    public final void multVec(final float[] v_in, final int v_in_offset, final float[] v_out, final int v_out_offset) {
        FloatUtil.multMatrixVec(matrix, 0, v_in, v_in_offset, v_out, v_out_offset);
    }

    public final void translate(final float x, final float y, final float z) {
        multMatrix(FloatUtil.makeTranslation(matrixTxSx, false, x, y, z));
    }

    public final void scale(final float x, final float y, final float z) {
        multMatrix(FloatUtil.makeScale(matrixTxSx, false, x, y, z));
    }

    public final void rotate(final float angrad, final float x, final float y, final float z) {
        multMatrix(FloatUtil.makeRotationAxis(mat4Tmp1, 0, angrad, x, y, z, vec4Tmp1));
    }

    /**
     * Rotate the current matrix with the given {@link Quaternion}'s rotation {@link Quaternion#toMatrix(float[], int) matrix representation}.
     */
    public final void rotate(final Quaternion quat) {
        multMatrix(quat.toMatrix(mat4Tmp1, 0));
    }

    public final void transpose() {
        System.arraycopy(matrix, 0, mat4Tmp1, 0, 16);
        FloatUtil.transposeMatrix(mat4Tmp1, matrix);
    }

    public final float determinant() {
        return FloatUtil.matrixDeterminant(matrix);
    }

    public final boolean invert() {
        return null != FloatUtil.invertMatrix(matrix, matrix);
    }

    public final void makeOrtho(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) {
        multMatrix( FloatUtil.makeOrtho(mat4Tmp1, 0, true, left, right, bottom, top, zNear, zFar) );
    }

    /**
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param zNear
     * @param zFar
     * @throws GLException if {@code zNear <= 0} or {@code zFar <= zNear}
     *                     or {@code left == right}, or {@code bottom == top}.
     * @see FloatUtil#makeFrustum(float[], int, boolean, float, float, float, float, float, float)
     */
    public final void makeFrustum(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) throws GLException {
        multMatrix( FloatUtil.makeFrustum(mat4Tmp1, 0, true, left, right, bottom, top, zNear, zFar) );
    }

    /**
     * @param fovy_rad
     * @param aspect
     * @param zNear
     * @param zFar
     * @throws GLException if {@code zNear <= 0} or {@code zFar <= zNear}
     * @see FloatUtil#makePerspective(float[], int, boolean, float, float, float, float)
     */
    public final void makePerspective(final float fovy_rad, final float aspect, final float zNear, final float zFar) throws GLException {
        multMatrix( FloatUtil.makePerspective(mat4Tmp1, 0, true, fovy_rad, aspect, zNear, zFar) );
    }

    private final float[] matrix, matrixTxSx;
    private final float[] mat4Tmp1, vec4Tmp1;
}
