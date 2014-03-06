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
 * matching OpenGL's implementation, translation matrix example:
 * <pre>
   Row-Major Order:
     1 0 0 x
     0 1 0 y
     0 0 1 z
     0 0 0 1
 * </pre>
 * <pre>
   Column-Major Order:
     1 0 0 0
     0 1 0 0
     0 0 1 0
     x y z 1
 * </pre>
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
   * Normalize vector
   *
   * @param v makes len(v)==1
   */
  public static final void normalize(float[] v) {
    float r = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);

    if ( r == 0.0 || r == 1.0) {
      return;
    }

    r = 1.0f / r;

    v[0] *= r;
    v[1] *= r;
    v[2] *= r;
  }

  /**
   * Normalize vector
   *
   * @param v makes len(v)==1
   */
  public static final void normalize(FloatBuffer v) {
    final int vPos = v.position();

    float r = (float) Math.sqrt(v.get(0+vPos) * v.get(0+vPos) +
                                v.get(1+vPos) * v.get(1+vPos) +
                                v.get(2+vPos) * v.get(2+vPos));

    if ( r == 0.0 || r == 1.0) {
      return;
    }

    r = 1.0f / r;

    v.put(0+vPos, v.get(0+vPos) * r);
    v.put(1+vPos, v.get(1+vPos) * r);
    v.put(2+vPos, v.get(2+vPos) * r);
  }


  /**
   * Calculate cross-product of 2 vector
   *
   * @param v1 3-component vector
   * @param v2 3-component vector
   * @param result v1 X v2
   */
  public static final void cross(float[] v1, float[] v2, float[] result) {
    result[0] = v1[1] * v2[2] - v1[2] * v2[1];
    result[1] = v1[2] * v2[0] - v1[0] * v2[2];
    result[2] = v1[0] * v2[1] - v1[1] * v2[0];
  }

  /**
   * Calculate cross-product of 2 vector
   *
   * @param v1 3-component vector
   * @param v2 3-component vector
   * @param result v1 X v2
   */
  public static final void cross(FloatBuffer v1, FloatBuffer v2, FloatBuffer result) {
    final int v1Pos = v1.position();
    final int v2Pos = v2.position();
    final int rPos  = result.position();

    result.put(0+rPos, v1.get(1+v1Pos) * v2.get(2+v2Pos) - v1.get(2+v1Pos) * v2.get(1+v2Pos));
    result.put(1+rPos, v1.get(2+v1Pos) * v2.get(0+v2Pos) - v1.get(0+v1Pos) * v2.get(2+v2Pos));
    result.put(2+rPos, v1.get(0+v1Pos) * v2.get(1+v2Pos) - v1.get(1+v1Pos) * v2.get(0+v2Pos));
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

  public static final float E = 2.7182818284590452354f;

  public static final float PI = 3.14159265358979323846f;

  public static float abs(float a) { return java.lang.Math.abs(a);  }

  public static float pow(float a, float b) { return (float) java.lang.Math.pow(a, b);  }

  public static float sin(float a) { return (float) java.lang.Math.sin(a);  }

  public static float cos(float a) { return (float) java.lang.Math.cos(a);  }

  public static float acos(float a) { return (float) java.lang.Math.acos(a);  }

  public static float sqrt(float a) { return (float) java.lang.Math.sqrt(a);  }

}