/**
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
package com.jogamp.opengl.math;

import java.nio.FloatBuffer;

import jogamp.opengl.Debug;

import com.jogamp.common.os.Platform;

/**
 * Basic Float math utility functions.
 * <p>
 * Implementation assumes linear matrix layout in column-major order
 * matching OpenGL's implementation, illustration:
 * <pre>
  Row-Major                    Column-Major (OpenGL):

        | 0  1  2  3  |            | 0  4  8  12 |
        |             |            |             |
        | 4  5  6  7  |            | 1  5  9  13 |
    M = |             |        M = |             |
        | 8  9  10 11 |            | 2  6  10 14 |
        |             |            |             |
        | 12 13 14 15 |            | 3  7  11 15 |
 * </pre>
 * </p>
 * <p>
 * See <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html">Matrix-FAQ</a>
 * </p>
 * <p>
 * Derived from ProjectFloat.java - Created 11-jan-2004
 * </p>
 *
 * @author Erik Duijs, Kenneth Russell, et al.
 */
public class FloatUtil {
  public static final boolean DEBUG = Debug.debug("Math");

  private static final float[] IDENTITY_MATRIX =
    new float[] {
      1.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 1.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 1.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 1.0f };

  private static final float[] ZERO_MATRIX =
    new float[] {
      0.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 0.0f };

  /**
   * Make matrix an identity matrix
   */
  public static final void makeIdentityf(float[] m, int offset) {
    for (int i = 0; i < 16; i++) {
      m[i+offset] = IDENTITY_MATRIX[i];
    }
  }

  /**
   * Make matrix an identity matrix
   */
  public static final void makeIdentityf(FloatBuffer m) {
    final int oldPos = m.position();
    m.put(IDENTITY_MATRIX);
    m.position(oldPos);
  }

  /**
   * Make matrix an zero matrix
   */
  public static final void makeZero(float[] m, int offset) {
    for (int i = 0; i < 16; i++) {
      m[i+offset] = 0;
    }
  }

  /**
   * Make matrix an zero matrix
   */
  public static final void makeZero(FloatBuffer m) {
    final int oldPos = m.position();
    m.put(ZERO_MATRIX);
    m.position(oldPos);
  }

  /**
   * Make a rotation matrix from the given axis and angle in radians.
   * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q38">Matrix-FAQ Q38</a>
   */
  public static final void makeRotationAxis(final float angrad, float x, float y, float z, final float[] mat, final int mat_offset, final float[] tmpVec3f) {
        final float c = cos(angrad);
        final float ic= 1.0f - c;
        final float s = sin(angrad);

        tmpVec3f[0]=x; tmpVec3f[1]=y; tmpVec3f[2]=z;
        VectorUtil.normalize(tmpVec3f);
        x = tmpVec3f[0]; y = tmpVec3f[1]; z = tmpVec3f[2];

        // Rotation matrix (Row Order):
        //      xx(1-c)+c  xy(1-c)+zs xz(1-c)-ys 0
        //      xy(1-c)-zs yy(1-c)+c  yz(1-c)+xs 0
        //      xz(1-c)+ys yz(1-c)-xs zz(1-c)+c  0
        //      0          0          0          1
        final float xy = x*y;
        final float xz = x*z;
        final float xs = x*s;
        final float ys = y*s;
        final float yz = y*z;
        final float zs = z*s;
        mat[0+0*4+mat_offset] = x*x*ic+c;
        mat[1+0*4+mat_offset] = xy*ic+zs;
        mat[2+0*4+mat_offset] = xz*ic-ys;

        mat[0+1*4+mat_offset] = xy*ic-zs;
        mat[1+1*4+mat_offset] = y*y*ic+c;
        mat[2+1*4+mat_offset] = yz*ic+xs;

        mat[0+2*4+mat_offset] = xz*ic+ys;
        mat[1+2*4+mat_offset] = yz*ic-xs;
        mat[2+2*4+mat_offset] = z*z*ic+c;
  }

  /**
   * Make a concatenated rotation matrix in column-major order from the given Euler rotation angles in radians.
   * <p>
   * The rotations are applied in the given order:
   * <ul>
   *  <li>y - heading</li>
   *  <li>z - attitude</li>
   *  <li>x - bank</li>
   * </ul>
   * </p>
   * @param bankX the Euler pitch angle in radians. (rotation about the X axis)
   * @param headingY the Euler yaw angle in radians. (rotation about the Y axis)
   * @param attitudeZ the Euler roll angle in radians. (rotation about the Z axis)
   * <p>
   * Implementation does not use Quaternion and hence is exposed to
   * <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q34">Gimbal-Lock</a>
   * </p>
   * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q36">Matrix-FAQ Q36</a>
   * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToMatrix/index.htm">euclideanspace.com-eulerToMatrix</a>
   */
  public static final void makeRotationEuler(final float bankX, final float headingY, final float attitudeZ, final float[] mat, final int mat_offset) {
      // Assuming the angles are in radians.
      final float ch = cos(headingY);
      final float sh = sin(headingY);
      final float ca = cos(attitudeZ);
      final float sa = sin(attitudeZ);
      final float cb = cos(bankX);
      final float sb = sin(bankX);

      mat[0+0*4+mat_offset] =  ch*ca;
      mat[0+1*4+mat_offset] =  sh*sb    - ch*sa*cb;
      mat[0+2*4+mat_offset] =  ch*sa*sb + sh*cb;
      mat[1+0*4+mat_offset] =  sa;
      mat[1+1*4+mat_offset] =  ca*cb;
      mat[1+2*4+mat_offset] = -ca*sb;
      mat[2+0*4+mat_offset] = -sh*ca;
      mat[2+1*4+mat_offset] =  sh*sa*cb + ch*sb;
      mat[2+2*4+mat_offset] = -sh*sa*sb + ch*cb;

      mat[3+0*4+mat_offset] =  0;
      mat[3+1*4+mat_offset] =  0;
      mat[3+2*4+mat_offset] =  0;

      mat[0+3*4+mat_offset] =  0;
      mat[1+3*4+mat_offset] =  0;
      mat[2+3*4+mat_offset] =  0;
      mat[3+3*4+mat_offset] =  1;
  }

  /**
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   */
  public static final void multMatrixf(final float[] a, int a_off, final float[] b, int b_off, float[] d, int d_off) {
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final float ai0=a[a_off+i+0*4],  ai1=a[a_off+i+1*4],  ai2=a[a_off+i+2*4],  ai3=a[a_off+i+3*4]; // row-i of a
        d[d_off+i+0*4] = ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] ;
        d[d_off+i+1*4] = ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] ;
        d[d_off+i+2*4] = ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] ;
        d[d_off+i+3*4] = ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] ;
     }
  }

  /**
   * @param a 4x4 matrix in column-major order (also result)
   * @param b 4x4 matrix in column-major order
   */
  public static final void multMatrixf(final float[] a, int a_off, final float[] b, int b_off) {
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final int a_off_i = a_off+i;
        final float ai0=a[a_off_i+0*4],  ai1=a[a_off_i+1*4],  ai2=a[a_off_i+2*4],  ai3=a[a_off_i+3*4]; // row-i of a
        a[a_off_i+0*4] = ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] ;
        a[a_off_i+1*4] = ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] ;
        a[a_off_i+2*4] = ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] ;
        a[a_off_i+3*4] = ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] ;
     }
  }

  /**
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   */
  public static final void multMatrixf(final float[] a, int a_off, final float[] b, int b_off, FloatBuffer d) {
     final int dP = d.position();
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final float ai0=a[a_off+i+0*4],  ai1=a[a_off+i+1*4],  ai2=a[a_off+i+2*4],  ai3=a[a_off+i+3*4]; // row-i of a
        d.put(dP+i+0*4 , ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] );
        d.put(dP+i+1*4 , ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] );
        d.put(dP+i+2*4 , ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] );
        d.put(dP+i+3*4 , ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] );
     }
  }

  /**
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   */
  public static final void multMatrixf(final FloatBuffer a, final float[] b, int b_off, FloatBuffer d) {
     final int aP = a.position();
     final int dP = d.position();
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final float ai0=a.get(aP+i+0*4),  ai1=a.get(aP+i+1*4),  ai2=a.get(aP+i+2*4),  ai3=a.get(aP+i+3*4); // row-i of a
        d.put(dP+i+0*4 , ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] );
        d.put(dP+i+1*4 , ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] );
        d.put(dP+i+2*4 , ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] );
        d.put(dP+i+3*4 , ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] );
     }
  }

  /**
   * @param a 4x4 matrix in column-major order (also result)
   * @param b 4x4 matrix in column-major order
   */
  public static final void multMatrixf(final FloatBuffer a, final float[] b, int b_off) {
     final int aP = a.position();
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final int aP_i = aP+i;
        final float ai0=a.get(aP_i+0*4),  ai1=a.get(aP_i+1*4),  ai2=a.get(aP_i+2*4),  ai3=a.get(aP_i+3*4); // row-i of a
        a.put(aP_i+0*4 , ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] );
        a.put(aP_i+1*4 , ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] );
        a.put(aP_i+2*4 , ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] );
        a.put(aP_i+3*4 , ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] );
     }
  }

  /**
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   */
  public static final void multMatrixf(final FloatBuffer a, final FloatBuffer b, FloatBuffer d) {
     final int aP = a.position();
     final int bP = b.position();
     final int dP = d.position();
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final float ai0=a.get(aP+i+0*4),  ai1=a.get(aP+i+1*4),  ai2=a.get(aP+i+2*4),  ai3=a.get(aP+i+3*4); // row-i of a
        d.put(dP+i+0*4 , ai0 * b.get(bP+0+0*4) + ai1 * b.get(bP+1+0*4) + ai2 * b.get(bP+2+0*4) + ai3 * b.get(bP+3+0*4) );
        d.put(dP+i+1*4 , ai0 * b.get(bP+0+1*4) + ai1 * b.get(bP+1+1*4) + ai2 * b.get(bP+2+1*4) + ai3 * b.get(bP+3+1*4) );
        d.put(dP+i+2*4 , ai0 * b.get(bP+0+2*4) + ai1 * b.get(bP+1+2*4) + ai2 * b.get(bP+2+2*4) + ai3 * b.get(bP+3+2*4) );
        d.put(dP+i+3*4 , ai0 * b.get(bP+0+3*4) + ai1 * b.get(bP+1+3*4) + ai2 * b.get(bP+2+3*4) + ai3 * b.get(bP+3+3*4) );
     }
  }

  /**
   * @param a 4x4 matrix in column-major order (also result)
   * @param b 4x4 matrix in column-major order
   */
  public static final void multMatrixf(final FloatBuffer a, final FloatBuffer b) {
     final int aP = a.position();
     final int bP = b.position();
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final int aP_i = aP+i;
        final float ai0=a.get(aP_i+0*4),  ai1=a.get(aP_i+1*4),  ai2=a.get(aP_i+2*4),  ai3=a.get(aP_i+3*4); // row-i of a
        a.put(aP_i+0*4 , ai0 * b.get(bP+0+0*4) + ai1 * b.get(bP+1+0*4) + ai2 * b.get(bP+2+0*4) + ai3 * b.get(bP+3+0*4) );
        a.put(aP_i+1*4 , ai0 * b.get(bP+0+1*4) + ai1 * b.get(bP+1+1*4) + ai2 * b.get(bP+2+1*4) + ai3 * b.get(bP+3+1*4) );
        a.put(aP_i+2*4 , ai0 * b.get(bP+0+2*4) + ai1 * b.get(bP+1+2*4) + ai2 * b.get(bP+2+2*4) + ai3 * b.get(bP+3+2*4) );
        a.put(aP_i+3*4 , ai0 * b.get(bP+0+3*4) + ai1 * b.get(bP+1+3*4) + ai2 * b.get(bP+2+3*4) + ai3 * b.get(bP+3+3*4) );
     }
  }

  /**
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   */
  public static final void multMatrixf(final FloatBuffer a, final FloatBuffer b, float[] d, int d_off) {
     final int aP = a.position();
     final int bP = b.position();
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final float ai0=a.get(aP+i+0*4),  ai1=a.get(aP+i+1*4),  ai2=a.get(aP+i+2*4),  ai3=a.get(aP+i+3*4); // row-i of a
        d[d_off+i+0*4] = ai0 * b.get(bP+0+0*4) + ai1 * b.get(bP+1+0*4) + ai2 * b.get(bP+2+0*4) + ai3 * b.get(bP+3+0*4) ;
        d[d_off+i+1*4] = ai0 * b.get(bP+0+1*4) + ai1 * b.get(bP+1+1*4) + ai2 * b.get(bP+2+1*4) + ai3 * b.get(bP+3+1*4) ;
        d[d_off+i+2*4] = ai0 * b.get(bP+0+2*4) + ai1 * b.get(bP+1+2*4) + ai2 * b.get(bP+2+2*4) + ai3 * b.get(bP+3+2*4) ;
        d[d_off+i+3*4] = ai0 * b.get(bP+0+3*4) + ai1 * b.get(bP+1+3*4) + ai2 * b.get(bP+2+3*4) + ai3 * b.get(bP+3+3*4) ;
     }
  }

  /**
   * @param m_in 4x4 matrix in column-major order
   * @param m_in_off
   * @param v_in 4-component column-vector
   * @param v_out m_in * v_in
   */
  public static final void multMatrixVecf(float[] m_in, int m_in_off, float[] v_in, int v_in_off, float[] v_out, int v_out_off) {
    for (int i = 0; i < 4; i++) {
      // (one matrix row in column-major order) X (column vector)
      v_out[i + v_out_off] =
        v_in[0+v_in_off] * m_in[0*4+i+m_in_off] +
        v_in[1+v_in_off] * m_in[1*4+i+m_in_off] +
        v_in[2+v_in_off] * m_in[2*4+i+m_in_off] +
        v_in[3+v_in_off] * m_in[3*4+i+m_in_off];
    }
  }

  /**
   * @param m_in 4x4 matrix in column-major order
   * @param m_in_off
   * @param v_in 4-component column-vector
   * @param v_out m_in * v_in
   */
  public static final void multMatrixVecf(float[] m_in, float[] v_in, float[] v_out) {
    for (int i = 0; i < 4; i++) {
      // (one matrix row in column-major order) X (column vector)
      v_out[i] =
        v_in[0] * m_in[0*4+i] +
        v_in[1] * m_in[1*4+i] +
        v_in[2] * m_in[2*4+i] +
        v_in[3] * m_in[3*4+i];
    }
  }

  /**
   * @param m_in 4x4 matrix in column-major order
   * @param v_in 4-component column-vector
   * @param v_out m_in * v_in
   */
  public static final void multMatrixVecf(FloatBuffer m_in, float[] v_in, int v_in_off, float[] v_out, int v_out_off) {
    final int matrixPos = m_in.position();
    for (int i = 0; i < 4; i++) {
      // (one matrix row in column-major order) X (column vector)
      v_out[i+v_out_off] =
        v_in[0+v_in_off] * m_in.get(0*4+i+matrixPos) +
        v_in[1+v_in_off] * m_in.get(1*4+i+matrixPos) +
        v_in[2+v_in_off] * m_in.get(2*4+i+matrixPos) +
        v_in[3+v_in_off] * m_in.get(3*4+i+matrixPos);
    }
  }

  /**
   * @param m_in 4x4 matrix in column-major order
   * @param v_in 4-component column-vector
   * @param v_out m_in * v_in
   */
  public static final void multMatrixVecf(FloatBuffer m_in, float[] v_in, float[] v_out) {
    final int matrixPos = m_in.position();
    for (int i = 0; i < 4; i++) {
      // (one matrix row in column-major order) X (column vector)
      v_out[i] =
        v_in[0] * m_in.get(0*4+i+matrixPos) +
        v_in[1] * m_in.get(1*4+i+matrixPos) +
        v_in[2] * m_in.get(2*4+i+matrixPos) +
        v_in[3] * m_in.get(3*4+i+matrixPos);
    }
  }

  /**
   * @param m_in 4x4 matrix in column-major order
   * @param v_in 4-component column-vector
   * @param v_out m_in * v_in
   */
  public static final void multMatrixVecf(FloatBuffer m_in, FloatBuffer v_in, FloatBuffer v_out) {
    final int inPos = v_in.position();
    final int outPos = v_out.position();
    final int matrixPos = m_in.position();
    for (int i = 0; i < 4; i++) {
      // (one matrix row in column-major order) X (column vector)
      v_out.put(i + outPos,
              v_in.get(0+inPos) * m_in.get(0*4+i+matrixPos) +
              v_in.get(1+inPos) * m_in.get(1*4+i+matrixPos) +
              v_in.get(2+inPos) * m_in.get(2*4+i+matrixPos) +
              v_in.get(3+inPos) * m_in.get(3*4+i+matrixPos));
    }
  }

  /**
   * Copy the named column of the given column-major matrix to v_out.
   * <p>
   * v_out may be 3 or 4 components long, hence the 4th row may not be stored.
   * </p>
   * @param m_in input column-major matrix
   * @param m_in_off offset to input matrix
   * @param column named column to copy
   * @param v_out the column-vector storage, at least 3 components long
   * @param v_out_off offset to storage
   */
  public static final void copyMatrixColumn(final float[] m_in, final int m_in_off, final int column, final float[] v_out, final int v_out_off) {
      v_out[0+v_out_off]=m_in[0+column*4+m_in_off];
      v_out[1+v_out_off]=m_in[1+column*4+m_in_off];
      v_out[2+v_out_off]=m_in[2+column*4+m_in_off];
      if( v_out.length > 3+v_out_off ) {
          v_out[3+v_out_off]=m_in[3+column*4+m_in_off];
      }
  }

  /**
   * Copy the named row of the given column-major matrix to v_out.
   * <p>
   * v_out may be 3 or 4 components long, hence the 4th column may not be stored.
   * </p>
   * @param m_in input column-major matrix
   * @param m_in_off offset to input matrix
   * @param row named row to copy
   * @param v_out the row-vector storage, at least 3 components long
   * @param v_out_off offset to storage
   */
  public static final void copyMatrixRow(final float[] m_in, final int m_in_off, final int row, final float[] v_out, final int v_out_off) {
      v_out[0+v_out_off]=m_in[row+0*4+m_in_off];
      v_out[1+v_out_off]=m_in[row+1*4+m_in_off];
      v_out[2+v_out_off]=m_in[row+2*4+m_in_off];
      if( v_out.length > 3+v_out_off ) {
          v_out[3+v_out_off]=m_in[row+3*4+m_in_off];
      }
  }

  /**
   * @param sb optional passed StringBuilder instance to be used
   * @param f the format string of one floating point, i.e. "%10.5f", see {@link java.util.Formatter}
   * @param a mxn matrix (rows x columns)
   * @param aOffset offset to <code>a</code>'s current position
   * @param rows
   * @param columns
   * @param rowMajorOrder if true floats are layed out in row-major-order, otherwise column-major-order (OpenGL)
   * @param row row number to print
   * @return matrix row string representation
   */
  public static StringBuilder matrixRowToString(StringBuilder sb, String f, FloatBuffer a, int aOffset, int rows, int columns, boolean rowMajorOrder, int row) {
      if(null == sb) {
          sb = new StringBuilder();
      }
      final int a0 = aOffset + a.position();
      if(rowMajorOrder) {
          for(int c=0; c<columns; c++) {
              sb.append( String.format( f+" ", a.get( a0 + row*columns + c ) ) );
          }
      } else {
          for(int r=0; r<columns; r++) {
              sb.append( String.format( f+" ", a.get( a0 + row + r*rows ) ) );
          }
      }
      return sb;
  }

  /**
   * @param sb optional passed StringBuilder instance to be used
   * @param f the format string of one floating point, i.e. "%10.5f", see {@link java.util.Formatter}
   * @param a mxn matrix (rows x columns)
   * @param aOffset offset to <code>a</code>'s current position
   * @param rows
   * @param columns
   * @param rowMajorOrder if true floats are layed out in row-major-order, otherwise column-major-order (OpenGL)
   * @param row row number to print
   * @return matrix row string representation
   */
  public static StringBuilder matrixRowToString(StringBuilder sb, String f, float[] a, int aOffset, int rows, int columns, boolean rowMajorOrder, int row) {
      if(null == sb) {
          sb = new StringBuilder();
      }
      if(rowMajorOrder) {
          for(int c=0; c<columns; c++) {
              sb.append( String.format( f+" ", a[ aOffset + row*columns + c ] ) );
          }
      } else {
          for(int r=0; r<columns; r++) {
              sb.append( String.format( f+" ", a[ aOffset + row + r*rows ] ) );
          }
      }
      return sb;
  }

  /**
   * @param sb optional passed StringBuilder instance to be used
   * @param rowPrefix optional prefix for each row
   * @param f the format string of one floating point, i.e. "%10.5f", see {@link java.util.Formatter}
   * @param a mxn matrix (rows x columns)
   * @param aOffset offset to <code>a</code>'s current position
   * @param rows
   * @param columns
   * @param rowMajorOrder if true floats are layed out in row-major-order, otherwise column-major-order (OpenGL)
   * @return matrix string representation
   */
  public static StringBuilder matrixToString(StringBuilder sb, String rowPrefix, String f, FloatBuffer a, int aOffset, int rows, int columns, boolean rowMajorOrder) {
      if(null == sb) {
          sb = new StringBuilder();
      }
      final String prefix = ( null == rowPrefix ) ? "" : rowPrefix;
      for(int i=0; i<rows; i++) {
          sb.append(prefix).append("[ ");
          matrixRowToString(sb, f, a, aOffset, rows, columns, rowMajorOrder, i);
          sb.append("]").append(Platform.getNewline());
      }
      return sb;
  }

  /**
   * @param sb optional passed StringBuilder instance to be used
   * @param rowPrefix optional prefix for each row
   * @param f the format string of one floating point, i.e. "%10.5f", see {@link java.util.Formatter}
   * @param a mxn matrix (rows x columns)
   * @param aOffset offset to <code>a</code>'s current position
   * @param rows
   * @param columns
   * @param rowMajorOrder if true floats are layed out in row-major-order, otherwise column-major-order (OpenGL)
   * @return matrix string representation
   */
  public static StringBuilder matrixToString(StringBuilder sb, String rowPrefix, String f, float[] a, int aOffset, int rows, int columns, boolean rowMajorOrder) {
      if(null == sb) {
          sb = new StringBuilder();
      }
      final String prefix = ( null == rowPrefix ) ? "" : rowPrefix;
      for(int i=0; i<rows; i++) {
          sb.append(prefix).append("[ ");
          matrixRowToString(sb, f, a, aOffset, rows, columns, rowMajorOrder, i);
          sb.append("]").append(Platform.getNewline());
      }
      return sb;
  }

  /**
   * @param sb optional passed StringBuilder instance to be used
   * @param rowPrefix optional prefix for each row
   * @param f the format string of one floating point, i.e. "%10.5f", see {@link java.util.Formatter}
   * @param a 4x4 matrix in column major order (OpenGL)
   * @param aOffset offset to <code>a</code>'s current position
   * @param b 4x4 matrix in column major order (OpenGL)
   * @param bOffset offset to <code>a</code>'s current position
   * @param rows
   * @param columns
   * @param rowMajorOrder if true floats are layed out in row-major-order, otherwise column-major-order (OpenGL)
   * @return side by side representation
   */
  public static StringBuilder matrixToString(StringBuilder sb, String rowPrefix, String f, FloatBuffer a, int aOffset, FloatBuffer b, int bOffset, int rows, int columns, boolean rowMajorOrder) {
      if(null == sb) {
          sb = new StringBuilder();
      }
      final String prefix = ( null == rowPrefix ) ? "" : rowPrefix;
      for(int i=0; i<rows; i++) {
          sb.append(prefix).append("[ ");
          matrixRowToString(sb, f, a, aOffset, rows, columns, rowMajorOrder, i);
          sb.append("=?= ");
          matrixRowToString(sb, f, b, bOffset, rows, columns, rowMajorOrder, i);
          sb.append("]").append(Platform.getNewline());
      }
      return sb;
  }

  /**
   * @param sb optional passed StringBuilder instance to be used
   * @param rowPrefix optional prefix for each row
   * @param f the format string of one floating point, i.e. "%10.5f", see {@link java.util.Formatter}
   * @param a 4x4 matrix in column major order (OpenGL)
   * @param aOffset offset to <code>a</code>'s current position
   * @param b 4x4 matrix in column major order (OpenGL)
   * @param bOffset offset to <code>a</code>'s current position
   * @param rows
   * @param columns
   * @param rowMajorOrder if true floats are layed out in row-major-order, otherwise column-major-order (OpenGL)
   * @return side by side representation
   */
  public static StringBuilder matrixToString(StringBuilder sb, String rowPrefix, String f, float[] a, int aOffset, float[] b, int bOffset, int rows, int columns, boolean rowMajorOrder) {
      if(null == sb) {
          sb = new StringBuilder();
      }
      final String prefix = ( null == rowPrefix ) ? "" : rowPrefix;
      for(int i=0; i<rows; i++) {
          sb.append(prefix).append("[ ");
          matrixRowToString(sb, f, a, aOffset, rows, columns, rowMajorOrder, i);
          sb.append("=?= ");
          matrixRowToString(sb, f, b, bOffset, rows, columns, rowMajorOrder, i);
          sb.append("]").append(Platform.getNewline());
      }
      return sb;
  }

  @SuppressWarnings("unused")
  private static void calculateMachineEpsilonFloat() {
      final long t0;
      if( DEBUG_EPSILON ) {
          t0 = Platform.currentTimeMillis();
      }
      float machEps = 1.0f;
      int i=0;
      do {
          machEps /= 2.0f;
          i++;
      } while (1.0f + (machEps / 2.0f) != 1.0f);
      machEpsilon = machEps;
      if( DEBUG_EPSILON ) {
          final long t1 = Platform.currentTimeMillis();
          System.err.println("MachineEpsilon: "+machEpsilon+", in "+i+" iterations within "+(t1-t0)+" ms");
      }
  }
  private static volatile boolean machEpsilonAvail = false;
  private static float machEpsilon = 0f;
  private static final boolean DEBUG_EPSILON = false;

  /**
   * Return computed machine Epsilon value.
   * <p>
   * The machine Epsilon value is computed once.
   * </p>
   * <p>
   * On a reference machine the result was {@link #EPSILON} in 23 iterations.
   * </p>
   * @see #EPSILON
   */
  public static float getMachineEpsilon() {
      if( !machEpsilonAvail ) {
          synchronized(FloatUtil.class) {
              if( !machEpsilonAvail ) {
                  machEpsilonAvail = true;
                  calculateMachineEpsilonFloat();
              }
          }
      }
      return machEpsilon;
  }

  public static final float E = 2.7182818284590452354f;

  /** The value PI, i.e. 180 degrees in radians. */
  public static final float PI = 3.14159265358979323846f;

  /** The value 2PI, i.e. 360 degrees in radians. */
  public static final float TWO_PI = 2f * PI;

  /** The value PI/2, i.e. 90 degrees in radians. */
  public static final float HALF_PI = PI / 2f;

  /** The value PI/4, i.e. 45 degrees in radians. */
  public static final float QUARTER_PI = PI / 4f;

  /** The value PI^2. */
  public final static float SQUARED_PI = PI * PI;

  /**
   * Epsilon for floating point {@value}, as once computed via {@link #getMachineEpsilon()} on an AMD-64 CPU.
   * <p>
   * Definition of machine epsilon guarantees that:
   * <pre>
   *        1.0f + EPSILON != 1.0f
   * </pre>
   * In other words: <i>machEps</i> is the maximum relative error of the chosen rounding procedure.
   * </p>
   * <p>
   * A number can be considered zero if it is in the range (or in the set):
   * <pre>
   *    <b>MaybeZeroSet</b> e ]-<i>machEps</i> .. <i>machEps</i>[  <i>(exclusive)</i>
   * </pre>
   * While comparing floating point values, <i>machEps</i> allows to clip the relative error:
   * <pre>
   *    boolean isZero    = afloat < EPSILON;
   *    boolean isNotZero = afloat >= EPSILON;
   *
   *    boolean isEqual    = abs(bfloat - afloat) < EPSILON;
   *    boolean isNotEqual = abs(bfloat - afloat) >= EPSILON;
   * </pre>
   * </p>
   * @see #equals(float, float, float)
   * @see #isZero(float, float)
   */
  public static final float EPSILON = 1.1920929E-7f; // Float.MIN_VALUE == 1.4e-45f ; double EPSILON 2.220446049250313E-16d

  /**
   * Return true if both values are equal, i.e. their absolute delta < <code>epsilon</code>.
   * @see #EPSILON
   */
  public static boolean equals(final float a, final float b, final float epsilon) {
      return Math.abs(a - b) < epsilon;
  }

  /**
   * Return true if value is zero, i.e. it's absolute value < <code>epsilon</code>.
   * @see #EPSILON
   */
  public static boolean isZero(final float a, final float epsilon) {
      return Math.abs(a) < epsilon;
  }

  public static float abs(final float a) { return java.lang.Math.abs(a);  }

  public static float pow(final float a, final float b) { return (float) java.lang.Math.pow(a, b);  }

  public static float sin(final float a) { return (float) java.lang.Math.sin(a);  }

  public static float asin(final float a) { return (float) java.lang.Math.asin(a);  }

  public static float cos(final float a) { return (float) java.lang.Math.cos(a);  }

  public static float acos(final float a) { return (float) java.lang.Math.acos(a);  }

  public static float tan(final float a) { return (float) java.lang.Math.tan(a); }

  public static float atan(final float a) { return (float) java.lang.Math.atan(a); }

  public static float atan2(final float y, final float x) { return (float) java.lang.Math.atan2(y, x); }

  public static float sqrt(final float a) { return (float) java.lang.Math.sqrt(a);  }

}