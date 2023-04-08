/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
import java.util.Locale;

import com.jogamp.opengl.GLException;

import jogamp.opengl.Debug;

import com.jogamp.common.os.Platform;

/**
 * Basic Float math utility functions.
 * <p>
 * Implementation assumes linear matrix layout in column-major order
 * matching OpenGL's implementation, illustration:
 * <pre>
    Row-Major                       Column-Major (OpenGL):

        |  0   1   2  tx |
        |                |
        |  4   5   6  ty |
    M = |                |
        |  8   9  10  tz |
        |                |
        | 12  13  14  15 |

           R   C                      R   C
         m[0*4+3] = tx;             m[0+4*3] = tx;
         m[1*4+3] = ty;             m[1+4*3] = ty;
         m[2*4+3] = tz;             m[2+4*3] = tz;

          RC (std subscript order)   RC (std subscript order)
         m03 = tx;                  m03 = tx;
         m13 = ty;                  m13 = ty;
         m23 = tz;                  m23 = tz;

 * </pre>
 * </p>
 * <p>
 * <ul>
 *   <li><a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html">Matrix-FAQ</a></li>
 *   <li><a href="https://en.wikipedia.org/wiki/Matrix_%28mathematics%29">Wikipedia-Matrix</a></li>
 *   <li><a href="http://www.euclideanspace.com/maths/algebra/matrix/index.htm">euclideanspace.com-Matrix</a></li>
 * </ul>
 * </p>
 * <p>
 * Implementation utilizes unrolling of small vertices and matrices wherever possible
 * while trying to access memory in a linear fashion for performance reasons, see:
 * <ul>
 *   <li><a href="https://lessthanoptimal.github.io/Java-Matrix-Benchmark/">java-matrix-benchmark</a></li>
 *   <li><a href="https://github.com/lessthanoptimal/ejml">EJML Efficient Java Matrix Library</a></li>
 * </ul>
 * </p>
 */
public final class FloatUtil {
  public static final boolean DEBUG = Debug.debug("Math");

  //
  // Matrix Ops
  // Only a subset will remain here, try using Matrix4f and perhaps PMVMatrix, SyncMatrix4f16 or SyncMatrices4f16
  //

  /**
   * Make matrix an identity matrix
   * @param m 4x4 matrix in column-major order (also result)
   * @return given matrix for chaining
   */
  public static float[] makeIdentity(final float[] m) {
      m[0+4*0] = 1f;
      m[1+4*0] = 0f;
      m[2+4*0] = 0f;
      m[3+4*0] = 0f;

      m[0+4*1] = 0f;
      m[1+4*1] = 1f;
      m[2+4*1] = 0f;
      m[3+4*1] = 0f;

      m[0+4*2] = 0f;
      m[1+4*2] = 0f;
      m[2+4*2] = 1f;
      m[3+4*2] = 0f;

      m[0+4*3] = 0f;
      m[1+4*3] = 0f;
      m[2+4*3] = 0f;
      m[3+4*3] = 1f;
      return m;
  }

  /**
   * Make a translation matrix in column-major order from the given axis deltas
   * <pre>
      Translation matrix (Column Order):
      1 0 0 0
      0 1 0 0
      0 0 1 0
      x y z 1
   * </pre>
   * <p>
   * All matrix fields are only set if <code>initM</code> is <code>true</code>.
   * </p>
   * @param m 4x4 matrix in column-major order (also result)
   * @param initM if true, given matrix will be initialized w/ identity matrix,
   *              otherwise only the diagonal and last-row is set.
   *              The latter can be utilized to share a once {@link #makeIdentity(float[], int) identity set} matrix
   *              for {@link #makeScale(float[], int, boolean, float, float, float) scaling}
   *              and {@link #makeTranslation(float[], int, boolean, float, float, float) translation},
   *              while leaving the other fields untouched for performance reasons.
   * @return given matrix for chaining
   */
  public static float[] makeTranslation(final float[] m, final boolean initM, final float tx, final float ty, final float tz) {
      if( initM ) {
          makeIdentity(m);
      } else {
          m[0+4*0] = 1;
          m[1+4*1] = 1;
          m[2+4*2] = 1;
          m[3+4*3] = 1;
      }
      m[0+4*3] = tx;
      m[1+4*3] = ty;
      m[2+4*3] = tz;
      return m;
  }

  /**
   * Make a scale matrix in column-major order from the given axis factors
   * <pre>
      Scale matrix (Any Order):
      x 0 0 0
      0 y 0 0
      0 0 z 0
      0 0 0 1
   * </pre>
   * <p>
   * All matrix fields are only set if <code>initM</code> is <code>true</code>.
   * </p>
   * @param m 4x4 matrix in column-major order (also result)
   * @param initM if true, given matrix will be initialized w/ identity matrix,
   *              otherwise only the diagonal and last-row is set.
   *              The latter can be utilized to share a once {@link #makeIdentity(float[], int) identity set} matrix
   *              for {@link #makeScale(float[], int, boolean, float, float, float) scaling}
   *              and {@link #makeTranslation(float[], int, boolean, float, float, float) translation},
   *              while leaving the other fields untouched for performance reasons.
   * @return given matrix for chaining
   */
  public static float[] makeScale(final float[] m, final boolean initM, final float sx, final float sy, final float sz) {
      if( initM ) {
          makeIdentity(m);
      } else {
          m[0+4*3] = 0;
          m[1+4*3] = 0;
          m[2+4*3] = 0;
          m[3+4*3] = 1;
      }
      m[0+4*0] = sx;
      m[1+4*1] = sy;
      m[2+4*2] = sz;
      return m;
  }

  /**
   * Make given matrix the frustum matrix based on given parameters.
   * <pre>
      Frustum matrix (Column Order):
      2*zNear/dx   0          0   0
      0            2*zNear/dy 0   0
      A            B          C  -1
      0            0          D   0
   * </pre>
   * <p>
   * All matrix fields are only set if <code>initM</code> is <code>true</code>.
   * </p>
   *
   * @param m 4x4 matrix in column-major order (also result)
   * @param m_offset offset in given array <i>m</i>, i.e. start of the 4x4 matrix
   * @param initM if true, given matrix will be initialized w/ identity matrix,
   *              otherwise only the frustum fields are set.
   * @param left
   * @param right
   * @param bottom
   * @param top
   * @param zNear
   * @param zFar
   * @return given matrix for chaining
   * @throws GLException if {@code zNear <= 0} or {@code zFar <= zNear}
   *                     or {@code left == right}, or {@code bottom == top}.
   */
  public static float[] makeFrustum(final float[] m, final int m_offset, final boolean initM,
                                    final float left, final float right,
                                    final float bottom, final float top,
                                    final float zNear, final float zFar) throws GLException {
      if( zNear <= 0.0f || zFar <= zNear ) {
          throw new GLException("Requirements zNear > 0 and zFar > zNear, but zNear "+zNear+", zFar "+zFar);
      }
      if( left == right || top == bottom) {
          throw new GLException("GL_INVALID_VALUE: top,bottom and left,right must not be equal");
      }
      if( initM ) {
          // m[m_offset+0+4*0] = 1f;
          m[m_offset+1+4*0] = 0f;
          m[m_offset+2+4*0] = 0f;
          m[m_offset+3+4*0] = 0f;

          m[m_offset+0+4*1] = 0f;
          // m[m_offset+1+4*1] = 1f;
          m[m_offset+2+4*1] = 0f;
          m[m_offset+3+4*1] = 0f;

          // m[m_offset+0+4*2] = 0f;
          // m[m_offset+1+4*2] = 0f;
          // m[m_offset+2+4*2] = 1f;
          // m[m_offset+3+4*2] = 0f;

          m[m_offset+0+4*3] = 0f;
          m[m_offset+1+4*3] = 0f;
          // m[m_offset+2+4*3] = 0f;
          // m[m_offset+3+4*3] = 1f;
      }
      final float zNear2 = 2.0f*zNear;
      final float dx=right-left;
      final float dy=top-bottom;
      final float dz=zFar-zNear;
      final float A=(right+left)/dx;
      final float B=(top+bottom)/dy;
      final float C=-1.0f*(zFar+zNear)/dz;
      final float D=-2.0f*(zFar*zNear)/dz;

      m[m_offset+0+4*0] = zNear2/dx;

      m[m_offset+1+4*1] = zNear2/dy;

      m[m_offset+0+4*2] = A;
      m[m_offset+1+4*2] = B;
      m[m_offset+2+4*2] = C;
      m[m_offset+3+4*2] = -1.0f;

      m[m_offset+2+4*3] = D;
      m[m_offset+3+4*3] = 0f;

      return m;
  }

  /**
   * Make given matrix the perspective {@link #makeFrustum(float[], int, boolean, float, float, float, float, float, float) frustum}
   * matrix based on given parameters.
   * <p>
   * All matrix fields are only set if <code>initM</code> is <code>true</code>.
   * </p>
   *
   * @param m 4x4 matrix in column-major order (also result)
   * @param m_offset offset in given array <i>m</i>, i.e. start of the 4x4 matrix
   * @param initM if true, given matrix will be initialized w/ identity matrix,
   *              otherwise only the frustum fields are set.
   * @param fovy_rad angle in radians
   * @param aspect aspect ratio width / height
   * @param zNear
   * @param zFar
   * @return given matrix for chaining
   * @throws GLException if {@code zNear <= 0} or {@code zFar <= zNear}
   * @see #makeFrustum(float[], int, boolean, float, float, float, float, float, float)
   */
  public static float[] makePerspective(final float[] m, final int m_off, final boolean initM,
                                        final float fovy_rad, final float aspect, final float zNear, final float zFar) throws GLException {
      final float top    =  tan(fovy_rad/2f) * zNear; // use tangent of half-fov !
      final float bottom =  -1.0f * top;    //          -1f * fovhvTan.top * zNear
      final float left   = aspect * bottom; // aspect * -1f * fovhvTan.top * zNear
      final float right  = aspect * top;    // aspect * fovhvTan.top * zNear
      return makeFrustum(m, m_off, initM, left, right, bottom, top, zNear, zFar);
  }

  /**
   * Make given matrix the <i>look-at</i> matrix based on given parameters.
   * <p>
   * Consist out of two matrix multiplications:
   * <pre>
   *   <b>R</b> = <b>L</b> x <b>T</b>,
   *   with <b>L</b> for <i>look-at</i> matrix and
   *        <b>T</b> for eye translation.
   *
   *   Result <b>R</b> can be utilized for <i>modelview</i> multiplication, i.e.
   *          <b>M</b> = <b>M</b> x <b>R</b>,
   *          with <b>M</b> being the <i>modelview</i> matrix.
   * </pre>
   * </p>
   * <p>
   * All matrix fields are set.
   * </p>
   * @param m 4x4 matrix in column-major order, result only
   * @param m_offset offset in given array <i>m</i>, i.e. start of the 4x4 matrix
   * @param eye 3 component eye vector
   * @param eye_offset
   * @param center 3 component center vector
   * @param center_offset
   * @param up 3 component up vector
   * @param up_offset
   * @param mat4Tmp temp float[16] storage
   * @return given matrix <code>m</code> for chaining
   */
  public static float[] makeLookAt(final float[] m, final int m_offset,
                                   final float[] eye, final int eye_offset,
                                   final float[] center, final int center_offset,
                                   final float[] up, final int up_offset,
                                   final float[] mat4Tmp) {
      final int forward_off = 0;
      final int side_off = 3;
      final int up2_off = 6;

      // forward!
      mat4Tmp[0] = center[0+center_offset] - eye[0+eye_offset];
      mat4Tmp[1] = center[1+center_offset] - eye[1+eye_offset];
      mat4Tmp[2] = center[2+center_offset] - eye[2+eye_offset];

      VectorUtil.normalizeVec3(mat4Tmp); // normalize forward

      /* Side = forward x up */
      VectorUtil.crossVec3(mat4Tmp, side_off, mat4Tmp, forward_off, up, up_offset);
      VectorUtil.normalizeVec3(mat4Tmp, side_off); // normalize side

      /* Recompute up as: up = side x forward */
      VectorUtil.crossVec3(mat4Tmp, up2_off, mat4Tmp, side_off, mat4Tmp, forward_off);

      m[m_offset + 0 * 4 + 0] = mat4Tmp[0+side_off]; // side
      m[m_offset + 0 * 4 + 1] = mat4Tmp[0+up2_off];  // up2
      m[m_offset + 0 * 4 + 2] = -mat4Tmp[0];         // forward
      m[m_offset + 0 * 4 + 3] = 0;

      m[m_offset + 1 * 4 + 0] = mat4Tmp[1+side_off]; // side
      m[m_offset + 1 * 4 + 1] = mat4Tmp[1+up2_off];  // up2
      m[m_offset + 1 * 4 + 2] = -mat4Tmp[1];         // forward
      m[m_offset + 1 * 4 + 3] = 0;

      m[m_offset + 2 * 4 + 0] = mat4Tmp[2+side_off]; // side
      m[m_offset + 2 * 4 + 1] = mat4Tmp[2+up2_off];  // up2
      m[m_offset + 2 * 4 + 2] = -mat4Tmp[2];         // forward
      m[m_offset + 2 * 4 + 3] = 0;

      m[m_offset + 3 * 4 + 0] = 0;
      m[m_offset + 3 * 4 + 1] = 0;
      m[m_offset + 3 * 4 + 2] = 0;
      m[m_offset + 3 * 4 + 3] = 1;

      makeTranslation(mat4Tmp, true, -eye[0+eye_offset], -eye[1+eye_offset], -eye[2+eye_offset]);
      multMatrix(m, m_offset, mat4Tmp, 0);

      return m;
  }

  /**
   * Make given matrix the <i>pick</i> matrix based on given parameters.
   * <p>
   * Traditional <code>gluPickMatrix</code> implementation.
   * </p>
   * <p>
   * Consist out of two matrix multiplications:
   * <pre>
   *   <b>R</b> = <b>T</b> x <b>S</b>,
   *   with <b>T</b> for viewport translation matrix and
   *        <b>S</b> for viewport scale matrix.
   *
   *   Result <b>R</b> can be utilized for <i>projection</i> multiplication, i.e.
   *          <b>P</b> = <b>P</b> x <b>R</b>,
   *          with <b>P</b> being the <i>projection</i> matrix.
   * </pre>
   * </p>
   * <p>
   * To effectively use the generated pick matrix for picking,
   * call {@link #makePick(float[], int, float, float, float, float, int[], int, float[]) makePick}
   * and multiply a {@link #makePerspective(float[], int, boolean, float, float, float, float) custom perspective matrix}
   * by this pick matrix. Then you may load the result onto the perspective matrix stack.
   * </p>
   * <p>
   * All matrix fields are set.
   * </p>
   * @param m 4x4 matrix in column-major order, result only
   * @param m_offset offset in given array <i>m</i>, i.e. start of the 4x4 matrix
   * @param x the center x-component of a picking region in window coordinates
   * @param y the center y-component of a picking region in window coordinates
   * @param deltaX the width of the picking region in window coordinates.
   * @param deltaY the height of the picking region in window coordinates.
   * @param viewport 4 component viewport vector
   * @param viewport_offset
   * @param mat4Tmp temp float[16] storage
   * @return given matrix <code>m</code> for chaining or <code>null</code> if either delta value is <= zero.
   */
  public static float[] makePick(final float[] m,
                                 final float x, final float y,
                                 final float deltaX, final float deltaY,
                                 final int[] viewport, final int viewport_offset,
                                 final float[] mat4Tmp) {
      if (deltaX <= 0 || deltaY <= 0) {
          return null;
      }

      /* Translate and scale the picked region to the entire window */
      makeTranslation(m, true,
              (viewport[2+viewport_offset] - 2 * (x - viewport[0+viewport_offset])) / deltaX,
              (viewport[3+viewport_offset] - 2 * (y - viewport[1+viewport_offset])) / deltaY,
              0);
      makeScale(mat4Tmp, true,
              viewport[2+viewport_offset] / deltaX, viewport[3+viewport_offset] / deltaY, 1.0f);
      multMatrix(m, mat4Tmp);
      return m;
  }

  /**
   * Transpose the given matrix.
   *
   * @param msrc 4x4 matrix in column-major order, the source
   * @param mres 4x4 matrix in column-major order, the result
   * @return given result matrix <i>mres</i> for chaining
   */
  public static float[] transposeMatrix(final float[] msrc, final float[] mres) {
      mres[0] = msrc[0*4];
      mres[1] = msrc[1*4];
      mres[2] = msrc[2*4];
      mres[3] = msrc[3*4];

      final int i4_1 = 1*4;
      mres[0+i4_1] = msrc[1+0*4];
      mres[1+i4_1] = msrc[1+1*4];
      mres[2+i4_1] = msrc[1+2*4];
      mres[3+i4_1] = msrc[1+3*4];

      final int i4_2 = 2*4;
      mres[0+i4_2] = msrc[2+0*4];
      mres[1+i4_2] = msrc[2+1*4];
      mres[2+i4_2] = msrc[2+2*4];
      mres[3+i4_2] = msrc[2+3*4];

      final int i4_3 = 3*4;
      mres[0+i4_3] = msrc[3+0*4];
      mres[1+i4_3] = msrc[3+1*4];
      mres[2+i4_3] = msrc[3+2*4];
      mres[3+i4_3] = msrc[3+3*4];

      return mres;
  }

  /**
   * Returns the determinant of the given matrix
   * @param m 4x4 matrix in column-major order, the source
   * @return the matrix determinant
   */
  public static float matrixDeterminant(final float[] m) {
        float a11 = m[ 1+4*1 ];
        float a21 = m[ 2+4*1 ];
        float a31 = m[ 3+4*1 ];
        float a12 = m[ 1+4*2 ];
        float a22 = m[ 2+4*2 ];
        float a32 = m[ 3+4*2 ];
        float a13 = m[ 1+4*3 ];
        float a23 = m[ 2+4*3 ];
        float a33 = m[ 3+4*3 ];

        float ret = 0;
        ret += m[     0 ] * ( + a11*(a22*a33 - a23*a32) - a12*(a21*a33 - a23*a31) + a13*(a21*a32 - a22*a31));
        a11  = m[ 1+4*0 ];
        a21  = m[ 2+4*0 ];
        a31  = m[ 3+4*0 ];
        ret -= m[ 0+4*1 ] * ( + a11*(a22*a33 - a23*a32) - a12*(a21*a33 - a23*a31) + a13*(a21*a32 - a22*a31));
        a12  = m[ 1+4*1 ];
        a22  = m[ 2+4*1 ];
        a32  = m[ 3+4*1 ];
        ret += m[ 0+4*2 ] * ( + a11*(a22*a33 - a23*a32) - a12*(a21*a33 - a23*a31) + a13*(a21*a32 - a22*a31));
        a13  = m[ 1+4*2 ];
        a23  = m[ 2+4*2 ];
        a33  = m[ 3+4*2 ];
        ret -= m[ 0+4*3 ] * ( + a11*(a22*a33 - a23*a32) - a12*(a21*a33 - a23*a31) + a13*(a21*a32 - a22*a31));
        return ret;
   }
  /**
   * Invert the given matrix.
   * <p>
   * Returns <code>null</code> if inversion is not possible,
   * e.g. matrix is singular due to a bad matrix.
   * </p>
   *
   * @param msrc 4x4 matrix in column-major order, the source
   * @param mres 4x4 matrix in column-major order, the result - may be <code>msrc</code> (in-place)
   * @return given result matrix <i>mres</i> for chaining if successful, otherwise <code>null</code>. See above.
   */
  public static float[] invertMatrix(final float[] msrc, final float[] mres) {
      final float scale;
      {
          float max = Math.abs(msrc[0]);

          for( int i = 1; i < 16; i++ ) {
              final float a = Math.abs(msrc[i]);
              if( a > max ) max = a;
          }
          if( 0 == max ) {
              return null;
          }
          scale = 1.0f/max;
      }

      final float a11 = msrc[0+4*0]*scale;
      final float a21 = msrc[1+4*0]*scale;
      final float a31 = msrc[2+4*0]*scale;
      final float a41 = msrc[3+4*0]*scale;
      final float a12 = msrc[0+4*1]*scale;
      final float a22 = msrc[1+4*1]*scale;
      final float a32 = msrc[2+4*1]*scale;
      final float a42 = msrc[3+4*1]*scale;
      final float a13 = msrc[0+4*2]*scale;
      final float a23 = msrc[1+4*2]*scale;
      final float a33 = msrc[2+4*2]*scale;
      final float a43 = msrc[3+4*2]*scale;
      final float a14 = msrc[0+4*3]*scale;
      final float a24 = msrc[1+4*3]*scale;
      final float a34 = msrc[2+4*3]*scale;
      final float a44 = msrc[3+4*3]*scale;

      final float m11 = + a22*(a33*a44 - a34*a43) - a23*(a32*a44 - a34*a42) + a24*(a32*a43 - a33*a42);
      final float m12 = -( + a21*(a33*a44 - a34*a43) - a23*(a31*a44 - a34*a41) + a24*(a31*a43 - a33*a41));
      final float m13 = + a21*(a32*a44 - a34*a42) - a22*(a31*a44 - a34*a41) + a24*(a31*a42 - a32*a41);
      final float m14 = -( + a21*(a32*a43 - a33*a42) - a22*(a31*a43 - a33*a41) + a23*(a31*a42 - a32*a41));
      final float m21 = -( + a12*(a33*a44 - a34*a43) - a13*(a32*a44 - a34*a42) + a14*(a32*a43 - a33*a42));
      final float m22 = + a11*(a33*a44 - a34*a43) - a13*(a31*a44 - a34*a41) + a14*(a31*a43 - a33*a41);
      final float m23 = -( + a11*(a32*a44 - a34*a42) - a12*(a31*a44 - a34*a41) + a14*(a31*a42 - a32*a41));
      final float m24 = + a11*(a32*a43 - a33*a42) - a12*(a31*a43 - a33*a41) + a13*(a31*a42 - a32*a41);
      final float m31 = + a12*(a23*a44 - a24*a43) - a13*(a22*a44 - a24*a42) + a14*(a22*a43 - a23*a42);
      final float m32 = -( + a11*(a23*a44 - a24*a43) - a13*(a21*a44 - a24*a41) + a14*(a21*a43 - a23*a41));
      final float m33 = + a11*(a22*a44 - a24*a42) - a12*(a21*a44 - a24*a41) + a14*(a21*a42 - a22*a41);
      final float m34 = -( + a11*(a22*a43 - a23*a42) - a12*(a21*a43 - a23*a41) + a13*(a21*a42 - a22*a41));
      final float m41 = -( + a12*(a23*a34 - a24*a33) - a13*(a22*a34 - a24*a32) + a14*(a22*a33 - a23*a32));
      final float m42 = + a11*(a23*a34 - a24*a33) - a13*(a21*a34 - a24*a31) + a14*(a21*a33 - a23*a31);
      final float m43 = -( + a11*(a22*a34 - a24*a32) - a12*(a21*a34 - a24*a31) + a14*(a21*a32 - a22*a31));
      final float m44 = + a11*(a22*a33 - a23*a32) - a12*(a21*a33 - a23*a31) + a13*(a21*a32 - a22*a31);

      final float det = (a11*m11 + a12*m12 + a13*m13 + a14*m14)/scale;
      if( 0 == det ) {
          return null;
      }
      final float invdet = 1.0f / det;

      mres[0+4*0] = m11 * invdet;
      mres[1+4*0] = m12 * invdet;
      mres[2+4*0] = m13 * invdet;
      mres[3+4*0] = m14 * invdet;
      mres[0+4*1] = m21 * invdet;
      mres[1+4*1] = m22 * invdet;
      mres[2+4*1] = m23 * invdet;
      mres[3+4*1] = m24 * invdet;
      mres[0+4*2] = m31 * invdet;
      mres[1+4*2] = m32 * invdet;
      mres[2+4*2] = m33 * invdet;
      mres[3+4*2] = m34 * invdet;
      mres[0+4*3] = m41 * invdet;
      mres[1+4*3] = m42 * invdet;
      mres[2+4*3] = m43 * invdet;
      mres[3+4*3] = m44 * invdet;
      return mres;
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
   * @param modelMatrix 4x4 modelview matrix
   * @param modelMatrix_offset
   * @param projMatrix 4x4 projection matrix
   * @param projMatrix_offset
   * @param viewport 4 component viewport vector
   * @param viewport_offset
   * @param win_pos 3 component window coordinate, the result
   * @param win_pos_offset
   * @param vec4Tmp1 4 component vector for temp storage
   * @param vec4Tmp2 4 component vector for temp storage
   * @return true if successful, otherwise false (z is 1)
   */
  public static boolean mapObjToWin(final float objx, final float objy, final float objz,
                                    final float[] modelMatrix, final int modelMatrix_offset,
                                    final float[] projMatrix, final int projMatrix_offset,
                                    final int[] viewport, final int viewport_offset,
                                    final float[] win_pos, final int win_pos_offset,
                                    final float[/*4*/] vec4Tmp1, final float[/*4*/] vec4Tmp2) {
      vec4Tmp1[0] = objx;
      vec4Tmp1[1] = objy;
      vec4Tmp1[2] = objz;
      vec4Tmp1[3] = 1.0f;

      // vec4Tmp2 = Mv * o
      // vec4Tmp1 = P  * vec4Tmp2
      // vec4Tmp1 = P * ( Mv * o )
      // vec4Tmp1 = P * Mv * o
      multMatrixVec(modelMatrix, modelMatrix_offset, vec4Tmp1, 0, vec4Tmp2, 0);
      multMatrixVec(projMatrix, projMatrix_offset, vec4Tmp2, 0, vec4Tmp1, 0);

      if (vec4Tmp1[3] == 0.0f) {
          return false;
      }

      vec4Tmp1[3] = (1.0f / vec4Tmp1[3]) * 0.5f;

      // Map x, y and z to range 0-1
      vec4Tmp1[0] = vec4Tmp1[0] * vec4Tmp1[3] + 0.5f;
      vec4Tmp1[1] = vec4Tmp1[1] * vec4Tmp1[3] + 0.5f;
      vec4Tmp1[2] = vec4Tmp1[2] * vec4Tmp1[3] + 0.5f;

      // Map x,y to viewport
      win_pos[0+win_pos_offset] = vec4Tmp1[0] * viewport[2+viewport_offset] + viewport[0+viewport_offset];
      win_pos[1+win_pos_offset] = vec4Tmp1[1] * viewport[3+viewport_offset] + viewport[1+viewport_offset];
      win_pos[2+win_pos_offset] = vec4Tmp1[2];

      return true;
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
   * @param modelMatrix 4x4 modelview matrix
   * @param modelMatrix_offset
   * @param projMatrix 4x4 projection matrix
   * @param projMatrix_offset
   * @param viewport 4 component viewport vector
   * @param viewport_offset
   * @param obj_pos 3 component object coordinate, the result
   * @param obj_pos_offset
   * @param mat4Tmp1 16 component matrix for temp storage
   * @param mat4Tmp2 16 component matrix for temp storage
   * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
   */
  public static boolean mapWinToObj(final float winx, final float winy, final float winz,
                                    final float[] modelMatrix, final int modelMatrix_offset,
                                    final float[] projMatrix, final int projMatrix_offset,
                                    final int[] viewport, final int viewport_offset,
                                    final float[] obj_pos, final int obj_pos_offset,
                                    final float[/*16*/] mat4Tmp1, final float[/*16*/] mat4Tmp2) {
    // mat4Tmp1 = P x Mv
    multMatrix(projMatrix, projMatrix_offset, modelMatrix, modelMatrix_offset, mat4Tmp1, 0);

    // mat4Tmp1 = Inv(P x Mv)
    if ( null == invertMatrix(mat4Tmp1, mat4Tmp1) ) {
      return false;
    }
    mat4Tmp2[0] = winx;
    mat4Tmp2[1] = winy;
    mat4Tmp2[2] = winz;
    mat4Tmp2[3] = 1.0f;

    // Map x and y from window coordinates
    mat4Tmp2[0] = (mat4Tmp2[0] - viewport[0+viewport_offset]) / viewport[2+viewport_offset];
    mat4Tmp2[1] = (mat4Tmp2[1] - viewport[1+viewport_offset]) / viewport[3+viewport_offset];

    // Map to range -1 to 1
    mat4Tmp2[0] = mat4Tmp2[0] * 2 - 1;
    mat4Tmp2[1] = mat4Tmp2[1] * 2 - 1;
    mat4Tmp2[2] = mat4Tmp2[2] * 2 - 1;

    final int raw_off = 4;
    // object raw coords = Inv(P x Mv) *  winPos  -> mat4Tmp2
    multMatrixVec(mat4Tmp1, 0, mat4Tmp2, 0, mat4Tmp2, raw_off);

    if (mat4Tmp2[3+raw_off] == 0.0) {
      return false;
    }

    mat4Tmp2[3+raw_off] = 1.0f / mat4Tmp2[3+raw_off];

    obj_pos[0+obj_pos_offset] = mat4Tmp2[0+raw_off] * mat4Tmp2[3+raw_off];
    obj_pos[1+obj_pos_offset] = mat4Tmp2[1+raw_off] * mat4Tmp2[3+raw_off];
    obj_pos[2+obj_pos_offset] = mat4Tmp2[2+raw_off] * mat4Tmp2[3+raw_off];

    return true;
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
   * @param mat4Tmp1 16 component matrix for temp storage
   * @param mat4Tmp2 16 component matrix for temp storage
   * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
   */
  public static boolean mapWinToObj4(final float winx, final float winy, final float winz, final float clipw,
                                     final float[] modelMatrix, final int modelMatrix_offset,
                                     final float[] projMatrix, final int projMatrix_offset,
                                     final int[] viewport, final int viewport_offset,
                                     final float near, final float far,
                                     final float[] obj_pos, final int obj_pos_offset,
                                     final float[/*16*/] mat4Tmp1, final float[/*16*/] mat4Tmp2) {
    // mat4Tmp1 = P x Mv
    multMatrix(projMatrix, projMatrix_offset, modelMatrix, modelMatrix_offset, mat4Tmp1, 0);

    // mat4Tmp1 = Inv(P x Mv)
    if ( null == invertMatrix(mat4Tmp1, mat4Tmp1) ) {
      return false;
    }

    mat4Tmp2[0] = winx;
    mat4Tmp2[1] = winy;
    mat4Tmp2[2] = winz;
    mat4Tmp2[3] = clipw;

    // Map x and y from window coordinates
    mat4Tmp2[0] = (mat4Tmp2[0] - viewport[0+viewport_offset]) / viewport[2+viewport_offset];
    mat4Tmp2[1] = (mat4Tmp2[1] - viewport[1+viewport_offset]) / viewport[3+viewport_offset];
    mat4Tmp2[2] = (mat4Tmp2[2] - near) / (far - near);

    // Map to range -1 to 1
    mat4Tmp2[0] = mat4Tmp2[0] * 2 - 1;
    mat4Tmp2[1] = mat4Tmp2[1] * 2 - 1;
    mat4Tmp2[2] = mat4Tmp2[2] * 2 - 1;

    final int raw_off = 4;
    // object raw coords = Inv(P x Mv) *  winPos  -> mat4Tmp2
    multMatrixVec(mat4Tmp1, 0, mat4Tmp2, 0, mat4Tmp2, raw_off);

    if (mat4Tmp2[3+raw_off] == 0.0) {
      return false;
    }

    obj_pos[0+obj_pos_offset] = mat4Tmp2[0+raw_off];
    obj_pos[1+obj_pos_offset] = mat4Tmp2[1+raw_off];
    obj_pos[2+obj_pos_offset] = mat4Tmp2[2+raw_off];
    obj_pos[3+obj_pos_offset] = mat4Tmp2[3+raw_off];

    return true;
  }

  /**
   * Multiply matrix: [d] = [a] x [b]
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   */
  public static void multMatrix(final float[] a, final int a_off, final float[] b, final int b_off, final float[] d, final int d_off) {
      final float b00 = b[b_off+0+0*4];
      final float b10 = b[b_off+1+0*4];
      final float b20 = b[b_off+2+0*4];
      final float b30 = b[b_off+3+0*4];
      final float b01 = b[b_off+0+1*4];
      final float b11 = b[b_off+1+1*4];
      final float b21 = b[b_off+2+1*4];
      final float b31 = b[b_off+3+1*4];
      final float b02 = b[b_off+0+2*4];
      final float b12 = b[b_off+1+2*4];
      final float b22 = b[b_off+2+2*4];
      final float b32 = b[b_off+3+2*4];
      final float b03 = b[b_off+0+3*4];
      final float b13 = b[b_off+1+3*4];
      final float b23 = b[b_off+2+3*4];
      final float b33 = b[b_off+3+3*4];

      float ai0=a[a_off+  0*4]; // row-0 of a
      float ai1=a[a_off+  1*4];
      float ai2=a[a_off+  2*4];
      float ai3=a[a_off+  3*4];
      d[d_off+ 0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      d[d_off+ 1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      d[d_off+ 2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      d[d_off+ 3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      ai0=a[a_off+1+0*4]; // row-1 of a
      ai1=a[a_off+1+1*4];
      ai2=a[a_off+1+2*4];
      ai3=a[a_off+1+3*4];
      d[d_off+1+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      d[d_off+1+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      d[d_off+1+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      d[d_off+1+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      ai0=a[a_off+2+0*4]; // row-2 of a
      ai1=a[a_off+2+1*4];
      ai2=a[a_off+2+2*4];
      ai3=a[a_off+2+3*4];
      d[d_off+2+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      d[d_off+2+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      d[d_off+2+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      d[d_off+2+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      ai0=a[a_off+3+0*4]; // row-3 of a
      ai1=a[a_off+3+1*4];
      ai2=a[a_off+3+2*4];
      ai3=a[a_off+3+3*4];
      d[d_off+3+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      d[d_off+3+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      d[d_off+3+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      d[d_off+3+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;
  }

  /**
   * Multiply matrix: [d] = [a] x [b]
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   * @return given result matrix <i>d</i> for chaining
   */
  public static float[] multMatrix(final float[] a, final float[] b, final float[] d) {
      final float b00 = b[0+0*4];
      final float b10 = b[1+0*4];
      final float b20 = b[2+0*4];
      final float b30 = b[3+0*4];
      final float b01 = b[0+1*4];
      final float b11 = b[1+1*4];
      final float b21 = b[2+1*4];
      final float b31 = b[3+1*4];
      final float b02 = b[0+2*4];
      final float b12 = b[1+2*4];
      final float b22 = b[2+2*4];
      final float b32 = b[3+2*4];
      final float b03 = b[0+3*4];
      final float b13 = b[1+3*4];
      final float b23 = b[2+3*4];
      final float b33 = b[3+3*4];

      float ai0=a[  0*4]; // row-0 of a
      float ai1=a[  1*4];
      float ai2=a[  2*4];
      float ai3=a[  3*4];
      d[ 0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      d[ 1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      d[ 2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      d[ 3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      ai0=a[1+0*4]; // row-1 of a
      ai1=a[1+1*4];
      ai2=a[1+2*4];
      ai3=a[1+3*4];
      d[1+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      d[1+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      d[1+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      d[1+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      ai0=a[2+0*4]; // row-2 of a
      ai1=a[2+1*4];
      ai2=a[2+2*4];
      ai3=a[2+3*4];
      d[2+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      d[2+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      d[2+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      d[2+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      ai0=a[3+0*4]; // row-3 of a
      ai1=a[3+1*4];
      ai2=a[3+2*4];
      ai3=a[3+3*4];
      d[3+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      d[3+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      d[3+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      d[3+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      return d;
  }

  /**
   * Multiply matrix: [a] = [a] x [b]
   * @param a 4x4 matrix in column-major order (also result)
   * @param b 4x4 matrix in column-major order
   */
  public static void multMatrix(final float[] a, final int a_off, final float[] b, final int b_off) {
      final float b00 = b[b_off+0+0*4];
      final float b10 = b[b_off+1+0*4];
      final float b20 = b[b_off+2+0*4];
      final float b30 = b[b_off+3+0*4];
      final float b01 = b[b_off+0+1*4];
      final float b11 = b[b_off+1+1*4];
      final float b21 = b[b_off+2+1*4];
      final float b31 = b[b_off+3+1*4];
      final float b02 = b[b_off+0+2*4];
      final float b12 = b[b_off+1+2*4];
      final float b22 = b[b_off+2+2*4];
      final float b32 = b[b_off+3+2*4];
      final float b03 = b[b_off+0+3*4];
      final float b13 = b[b_off+1+3*4];
      final float b23 = b[b_off+2+3*4];
      final float b33 = b[b_off+3+3*4];

      float ai0=a[a_off+  0*4]; // row-0 of a
      float ai1=a[a_off+  1*4];
      float ai2=a[a_off+  2*4];
      float ai3=a[a_off+  3*4];
      a[a_off+ 0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      a[a_off+ 1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      a[a_off+ 2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      a[a_off+ 3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      ai0=a[a_off+1+0*4]; // row-1 of a
      ai1=a[a_off+1+1*4];
      ai2=a[a_off+1+2*4];
      ai3=a[a_off+1+3*4];
      a[a_off+1+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      a[a_off+1+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      a[a_off+1+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      a[a_off+1+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      ai0=a[a_off+2+0*4]; // row-2 of a
      ai1=a[a_off+2+1*4];
      ai2=a[a_off+2+2*4];
      ai3=a[a_off+2+3*4];
      a[a_off+2+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      a[a_off+2+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      a[a_off+2+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      a[a_off+2+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      ai0=a[a_off+3+0*4]; // row-3 of a
      ai1=a[a_off+3+1*4];
      ai2=a[a_off+3+2*4];
      ai3=a[a_off+3+3*4];
      a[a_off+3+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      a[a_off+3+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      a[a_off+3+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      a[a_off+3+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;
  }

  /**
   * Multiply matrix: [a] = [a] x [b]
   * @param a 4x4 matrix in column-major order (also result)
   * @param b 4x4 matrix in column-major order
   * @return given result matrix <i>a</i> for chaining
   */
  public static float[] multMatrix(final float[] a, final float[] b) {
      final float b00 = b[0+0*4];
      final float b10 = b[1+0*4];
      final float b20 = b[2+0*4];
      final float b30 = b[3+0*4];
      final float b01 = b[0+1*4];
      final float b11 = b[1+1*4];
      final float b21 = b[2+1*4];
      final float b31 = b[3+1*4];
      final float b02 = b[0+2*4];
      final float b12 = b[1+2*4];
      final float b22 = b[2+2*4];
      final float b32 = b[3+2*4];
      final float b03 = b[0+3*4];
      final float b13 = b[1+3*4];
      final float b23 = b[2+3*4];
      final float b33 = b[3+3*4];

      float ai0=a[  0*4]; // row-0 of a
      float ai1=a[  1*4];
      float ai2=a[  2*4];
      float ai3=a[  3*4];
      a[ 0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      a[ 1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      a[ 2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      a[ 3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      ai0=a[1+0*4]; // row-1 of a
      ai1=a[1+1*4];
      ai2=a[1+2*4];
      ai3=a[1+3*4];
      a[1+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      a[1+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      a[1+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      a[1+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      ai0=a[2+0*4]; // row-2 of a
      ai1=a[2+1*4];
      ai2=a[2+2*4];
      ai3=a[2+3*4];
      a[2+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      a[2+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      a[2+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      a[2+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      ai0=a[3+0*4]; // row-3 of a
      ai1=a[3+1*4];
      ai2=a[3+2*4];
      ai3=a[3+3*4];
      a[3+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
      a[3+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
      a[3+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
      a[3+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

      return a;
  }

  /**
   * Multiply matrix: [d] = [a] x [b]
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   */
  public static void multMatrix(final FloatBuffer a, final FloatBuffer b, final float[] d) {
     final int a_off = a.position();
     final int b_off = b.position();
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final int a_off_i = a_off+i;
        final float ai0=a.get(a_off_i+0*4),  ai1=a.get(a_off_i+1*4),  ai2=a.get(a_off_i+2*4),  ai3=a.get(a_off_i+3*4); // row-i of a
        d[i+0*4] = ai0 * b.get(b_off+0+0*4) + ai1 * b.get(b_off+1+0*4) + ai2 * b.get(b_off+2+0*4) + ai3 * b.get(b_off+3+0*4) ;
        d[i+1*4] = ai0 * b.get(b_off+0+1*4) + ai1 * b.get(b_off+1+1*4) + ai2 * b.get(b_off+2+1*4) + ai3 * b.get(b_off+3+1*4) ;
        d[i+2*4] = ai0 * b.get(b_off+0+2*4) + ai1 * b.get(b_off+1+2*4) + ai2 * b.get(b_off+2+2*4) + ai3 * b.get(b_off+3+2*4) ;
        d[i+3*4] = ai0 * b.get(b_off+0+3*4) + ai1 * b.get(b_off+1+3*4) + ai2 * b.get(b_off+2+3*4) + ai3 * b.get(b_off+3+3*4) ;
     }
  }

  /**
   * Multiply matrix: [a] = [a] x [b]
   * @param a 4x4 matrix in column-major order (also result)
   * @param b 4x4 matrix in column-major order
   */
  public static void multMatrix(final FloatBuffer a, final FloatBuffer b) {
     final int a_off = a.position();
     final int b_off = b.position();
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final int a_off_i = a_off+i;
        final float ai0=a.get(a_off_i+0*4),  ai1=a.get(a_off_i+1*4),  ai2=a.get(a_off_i+2*4),  ai3=a.get(a_off_i+3*4); // row-i of a
        a.put(a_off_i+0*4 , ai0 * b.get(b_off+0+0*4) + ai1 * b.get(b_off+1+0*4) + ai2 * b.get(b_off+2+0*4) + ai3 * b.get(b_off+3+0*4) );
        a.put(a_off_i+1*4 , ai0 * b.get(b_off+0+1*4) + ai1 * b.get(b_off+1+1*4) + ai2 * b.get(b_off+2+1*4) + ai3 * b.get(b_off+3+1*4) );
        a.put(a_off_i+2*4 , ai0 * b.get(b_off+0+2*4) + ai1 * b.get(b_off+1+2*4) + ai2 * b.get(b_off+2+2*4) + ai3 * b.get(b_off+3+2*4) );
        a.put(a_off_i+3*4 , ai0 * b.get(b_off+0+3*4) + ai1 * b.get(b_off+1+3*4) + ai2 * b.get(b_off+2+3*4) + ai3 * b.get(b_off+3+3*4) );
     }
  }

  /**
   * @param m_in 4x4 matrix in column-major order
   * @param m_in_off
   * @param v_in 4-component column-vector
   * @param v_out m_in * v_in
   */
  public static void multMatrixVec(final float[] m_in, final int m_in_off,
                                   final float[] v_in, final int v_in_off,
                                   final float[] v_out, final int v_out_off) {
      // (one matrix row in column-major order) X (column vector)
      v_out[0 + v_out_off] = v_in[0+v_in_off] * m_in[0*4+m_in_off  ]  +  v_in[1+v_in_off] * m_in[1*4+m_in_off  ] +
                             v_in[2+v_in_off] * m_in[2*4+m_in_off  ]  +  v_in[3+v_in_off] * m_in[3*4+m_in_off  ];

      final int m_in_off_1 = 1+m_in_off;
      v_out[1 + v_out_off] = v_in[0+v_in_off] * m_in[0*4+m_in_off_1]  +  v_in[1+v_in_off] * m_in[1*4+m_in_off_1] +
                             v_in[2+v_in_off] * m_in[2*4+m_in_off_1]  +  v_in[3+v_in_off] * m_in[3*4+m_in_off_1];

      final int m_in_off_2 = 2+m_in_off;
      v_out[2 + v_out_off] = v_in[0+v_in_off] * m_in[0*4+m_in_off_2]  +  v_in[1+v_in_off] * m_in[1*4+m_in_off_2] +
                             v_in[2+v_in_off] * m_in[2*4+m_in_off_2]  +  v_in[3+v_in_off] * m_in[3*4+m_in_off_2];

      final int m_in_off_3 = 3+m_in_off;
      v_out[3 + v_out_off] = v_in[0+v_in_off] * m_in[0*4+m_in_off_3]  +  v_in[1+v_in_off] * m_in[1*4+m_in_off_3] +
                             v_in[2+v_in_off] * m_in[2*4+m_in_off_3]  +  v_in[3+v_in_off] * m_in[3*4+m_in_off_3];
  }

  /**
   * @param m_in 4x4 matrix in column-major order
   * @param m_in_off
   * @param v_in 4-component column-vector
   * @param v_out m_in * v_in
   */
  public static void multMatrixVec(final float[] m_in, final int m_in_off,
                                   final float[] v_in, final float[] v_out) {
      // (one matrix row in column-major order) X (column vector)
      v_out[0] = v_in[0] * m_in[0*4+m_in_off  ]  +  v_in[1] * m_in[1*4+m_in_off  ] +
                 v_in[2] * m_in[2*4+m_in_off  ]  +  v_in[3] * m_in[3*4+m_in_off  ];

      final int m_in_off_1 = 1+m_in_off;
      v_out[1] = v_in[0] * m_in[0*4+m_in_off_1]  +  v_in[1] * m_in[1*4+m_in_off_1] +
                 v_in[2] * m_in[2*4+m_in_off_1]  +  v_in[3] * m_in[3*4+m_in_off_1];

      final int m_in_off_2 = 2+m_in_off;
      v_out[2] = v_in[0] * m_in[0*4+m_in_off_2]  +  v_in[1] * m_in[1*4+m_in_off_2] +
                 v_in[2] * m_in[2*4+m_in_off_2]  +  v_in[3] * m_in[3*4+m_in_off_2];

      final int m_in_off_3 = 3+m_in_off;
      v_out[3] = v_in[0] * m_in[0*4+m_in_off_3]  +  v_in[1] * m_in[1*4+m_in_off_3] +
                 v_in[2] * m_in[2*4+m_in_off_3]  +  v_in[3] * m_in[3*4+m_in_off_3];
  }

  /**
   * @param m_in 4x4 matrix in column-major order
   * @param m_in_off
   * @param v_in 4-component column-vector
   * @param v_out m_in * v_in
   * @return given result vector <i>v_out</i> for chaining
   */
  public static float[] multMatrixVec(final float[] m_in, final float[] v_in, final float[] v_out) {
      // (one matrix row in column-major order) X (column vector)
      v_out[0] = v_in[0] * m_in[0*4  ]  +  v_in[1] * m_in[1*4  ] +
                 v_in[2] * m_in[2*4  ]  +  v_in[3] * m_in[3*4  ];

      v_out[1] = v_in[0] * m_in[0*4+1]  +  v_in[1] * m_in[1*4+1] +
                 v_in[2] * m_in[2*4+1]  +  v_in[3] * m_in[3*4+1];

      v_out[2] = v_in[0] * m_in[0*4+2]  +  v_in[1] * m_in[1*4+2] +
                 v_in[2] * m_in[2*4+2]  +  v_in[3] * m_in[3*4+2];

      v_out[3] = v_in[0] * m_in[0*4+3]  +  v_in[1] * m_in[1*4+3] +
                 v_in[2] * m_in[2*4+3]  +  v_in[3] * m_in[3*4+3];

      return v_out;
  }

  /**
   * @param m_in 4x4 matrix in column-major order
   * @param v_in 4-component column-vector
   * @param v_out m_in * v_in
   */
  public static void multMatrixVec(final FloatBuffer m_in, final float[] v_in, final float[] v_out) {
    final int m_in_off = m_in.position();
    for (int i = 0; i < 4; i++) {
      // (one matrix row in column-major order) X (column vector)
      final int i_m_in_off = i+m_in_off;
      v_out[i] =
        v_in[0] * m_in.get(0*4+i_m_in_off) +
        v_in[1] * m_in.get(1*4+i_m_in_off) +
        v_in[2] * m_in.get(2*4+i_m_in_off) +
        v_in[3] * m_in.get(3*4+i_m_in_off);
    }
  }

  /**
   * Affine 3f-vector transformation by 4x4 matrix
   *
   * 4x4 matrix multiplication with 3-component vector,
   * using {@code 1} for for {@code v_in[3]} and dropping {@code v_out[3]},
   * which shall be {@code 1}.
   *
   * @param m_in 4x4 matrix in column-major order
   * @param m_in_off
   * @param v_in 3-component column-vector
   * @param v_out m_in * v_in, 3-component column-vector
   * @return given result vector <i>v_out</i> for chaining
   */
  public static float[] multMatrixVec3(final float[] m_in, final float[] v_in, final float[] v_out) {
      // (one matrix row in column-major order) X (column vector)
      v_out[0] = v_in[0] * m_in[0*4  ]  +  v_in[1] * m_in[1*4  ] +
                 v_in[2] * m_in[2*4  ]  +       1f * m_in[3*4  ];

      v_out[1] = v_in[0] * m_in[0*4+1]  +  v_in[1] * m_in[1*4+1] +
                 v_in[2] * m_in[2*4+1]  +       1f * m_in[3*4+1];

      v_out[2] = v_in[0] * m_in[0*4+2]  +  v_in[1] * m_in[1*4+2] +
                 v_in[2] * m_in[2*4+2]  +       1f * m_in[3*4+2];

      return v_out;
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
  public static StringBuilder matrixRowToString(StringBuilder sb, final String f,
                                                final FloatBuffer a, final int aOffset,
                                                final int rows, final int columns, final boolean rowMajorOrder, final int row) {
      if(null == sb) {
          sb = new StringBuilder();
      }
      final int a0 = aOffset + a.position();
      if(rowMajorOrder) {
          for(int c=0; c<columns; c++) {
              sb.append( String.format((Locale)null, f+", ", a.get( a0 + row*columns + c ) ) );
          }
      } else {
          for(int r=0; r<columns; r++) {
              sb.append( String.format((Locale)null, f+", ", a.get( a0 + row + r*rows ) ) );
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
  public static StringBuilder matrixRowToString(StringBuilder sb, final String f,
                                                final float[] a, final int aOffset, final int rows, final int columns, final boolean rowMajorOrder, final int row) {
      if(null == sb) {
          sb = new StringBuilder();
      }
      if(rowMajorOrder) {
          for(int c=0; c<columns; c++) {
              sb.append( String.format((Locale)null, f+", ", a[ aOffset + row*columns + c ] ) );
          }
      } else {
          for(int r=0; r<columns; r++) {
              sb.append( String.format((Locale)null, f+", ", a[ aOffset + row + r*rows ] ) );
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
  public static StringBuilder matrixToString(StringBuilder sb, final String rowPrefix, final String f,
                                             final FloatBuffer a, final int aOffset, final int rows, final int columns, final boolean rowMajorOrder) {
      if(null == sb) {
          sb = new StringBuilder();
      }
      final String prefix = ( null == rowPrefix ) ? "" : rowPrefix;
      sb.append(prefix).append("{ ");
      for(int i=0; i<rows; i++) {
          if( 0 < i ) {
              sb.append(prefix).append("  ");
          }
          matrixRowToString(sb, f, a, aOffset, rows, columns, rowMajorOrder, i);
          sb.append(Platform.getNewline());
      }
      sb.append(prefix).append("}").append(Platform.getNewline());
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
  public static StringBuilder matrixToString(StringBuilder sb, final String rowPrefix, final String f,
                                             final float[] a, final int aOffset, final int rows, final int columns, final boolean rowMajorOrder) {
      if(null == sb) {
          sb = new StringBuilder();
      }
      final String prefix = ( null == rowPrefix ) ? "" : rowPrefix;
      sb.append(prefix).append("{ ");
      for(int i=0; i<rows; i++) {
          if( 0 < i ) {
              sb.append(prefix).append("  ");
          }
          matrixRowToString(sb, f, a, aOffset, rows, columns, rowMajorOrder, i);
          sb.append(Platform.getNewline());
      }
      sb.append(prefix).append("}").append(Platform.getNewline());
      return sb;
  }

  //
  // Scalar Ops
  //

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

  /** Converts arc-degree to radians */
  public static float adegToRad(final float arc_degree) {
      return arc_degree * PI / 180.0f;
  }

  /** Converts radians to arc-degree */
  public static float radToADeg(final float rad) {
      return rad * 180.0f / PI;
  }

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
   * @see #isEqual(float, float, float)
   * @see #isZero(float, float)
   */
  public static final float EPSILON = 1.1920929E-7f; // Float.MIN_VALUE == 1.4e-45f ; double EPSILON 2.220446049250313E-16d

  /**
   * Inversion Epsilon, used with equals method to determine if two inverted matrices are close enough to be considered equal.
   * <p>
   * Using {@value}, which is ~100 times {@link FloatUtil#EPSILON}.
   * </p>
   */
  public static final float INV_DEVIANCE = 1.0E-5f; // FloatUtil.EPSILON == 1.1920929E-7f; double ALLOWED_DEVIANCE: 1.0E-8f

  /**
   * Return true if both values are equal w/o regarding an epsilon.
   * <p>
   * Implementation considers following corner cases:
   * <ul>
   *    <li>NaN == NaN</li>
   *    <li>+Inf == +Inf</li>
   *    <li>-Inf == -Inf</li>
   * </ul>
   * </p>
   * @see #isEqual(float, float, float)
   */
  public static boolean isEqual(final float a, final float b) {
      // Values are equal (Inf, Nan .. )
      return Float.floatToIntBits(a) == Float.floatToIntBits(b);
  }

  /**
   * Return true if both values are equal, i.e. their absolute delta < <code>epsilon</code>.
   * <p>
   * Implementation considers following corner cases:
   * <ul>
   *    <li>NaN == NaN</li>
   *    <li>+Inf == +Inf</li>
   *    <li>-Inf == -Inf</li>
   * </ul>
   * </p>
   * @see #EPSILON
   */
  public static boolean isEqual(final float a, final float b, final float epsilon) {
      if ( Math.abs(a - b) < epsilon ) {
          return true;
      } else {
          // Values are equal (Inf, Nan .. )
          return Float.floatToIntBits(a) == Float.floatToIntBits(b);
      }
  }

  /**
   * Return true if both values are equal w/o regarding an epsilon.
   * <p>
   * Implementation considers following corner cases:
   * <ul>
   *    <li>NaN == NaN</li>
   *    <li>+Inf == +Inf</li>
   *    <li>-Inf == -Inf</li>
   *    <li>NaN > 0</li>
   *    <li>+Inf > -Inf</li>
   * </ul>
   * </p>
   * @see #compare(float, float, float)
   */
  public static int compare(final float a, final float b) {
      if (a < b) {
          return -1; // Neither is NaN, a is smaller
      }
      if (a > b) {
          return 1;  // Neither is NaN, a is larger
      }
      final int aBits = Float.floatToIntBits(a);
      final int bBits = Float.floatToIntBits(b);
      if( aBits == bBits ) {
          return 0;  // Values are equal (Inf, Nan .. )
      } else if( aBits < bBits ) {
          return -1; // (-0.0,  0.0) or (!NaN,  NaN)
      } else {
          return 1;  // ( 0.0, -0.0) or ( NaN, !NaN)
      }
  }

  /**
   * Return true if both values are equal, i.e. their absolute delta < <code>epsilon</code>.
   * <p>
   * Implementation considers following corner cases:
   * <ul>
   *    <li>NaN == NaN</li>
   *    <li>+Inf == +Inf</li>
   *    <li>-Inf == -Inf</li>
   *    <li>NaN > 0</li>
   *    <li>+Inf > -Inf</li>
   * </ul>
   * </p>
   * @see #EPSILON
   */
  public static int compare(final float a, final float b, final float epsilon) {
      if ( Math.abs(a - b) < epsilon ) {
          return 0;
      } else {
          return compare(a, b);
      }
  }

  /**
   * Return true if value is zero, i.e. it's absolute value < <code>epsilon</code>.
   * @see #EPSILON
   */
  public static boolean isZero(final float a, final float epsilon) {
      return Math.abs(a) < epsilon;
  }

  /**
   * Return true if value is zero, i.e. it's absolute value < {@link #EPSILON}.
   * @see #EPSILON
   */
  public static boolean isZero(final float a) {
      return Math.abs(a) < FloatUtil.EPSILON;
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

  /**
   * Returns resolution of Z buffer of given parameter,
   * see <a href="http://www.sjbaker.org/steve/omniv/love_your_z_buffer.html">Love Your Z-Buffer</a>.
   * <pre>
   *  return z * z / ( zNear * (1&lt;&lt;zBits) - z )
   * </pre>
   * @param zBits number of bits of Z precision, i.e. z-buffer depth
   * @param z distance from the eye to the object
   * @param zNear distance from eye to near clip plane
   * @return smallest resolvable Z separation at this range.
   */
  public static float getZBufferEpsilon(final int zBits, final float z, final float zNear) {
      return z * z / ( zNear * ( 1 << zBits ) - z );
  }

  /**
   * Returns Z buffer value of given parameter,
   * see <a href="http://www.sjbaker.org/steve/omniv/love_your_z_buffer.html">Love Your Z-Buffer</a>.
   * <pre>
   *  float a = zFar / ( zFar - zNear )
   *  float b = zFar * zNear / ( zNear - zFar )
   *  return (int) ( (1&lt;&lt;zBits) * ( a + b / z ) )
   * </pre>
   * @param zBits number of bits of Z precision, i.e. z-buffer depth
   * @param z distance from the eye to the object
   * @param zNear distance from eye to near clip plane
   * @param zFar distance from eye to far clip plane
   * @return z buffer value
   */
  public static int getZBufferValue(final int zBits, final float z, final float zNear, final float zFar) {
      final float a = zFar / ( zFar - zNear );
      final float b = zFar * zNear / ( zNear - zFar );
      return (int) ( (1<<zBits) * ( a + b / z ) );
  }

  /**
   * Returns orthogonal distance
   * (1f/zNear-1f/orthoZ) / (1f/zNear-1f/zFar);
   */
  public static float getOrthoWinZ(final float orthoZ, final float zNear, final float zFar) {
      return (1f/zNear-1f/orthoZ) / (1f/zNear-1f/zFar);
  }

}