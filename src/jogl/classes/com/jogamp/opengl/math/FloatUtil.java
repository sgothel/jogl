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

import javax.media.opengl.GLException;

import jogamp.opengl.Debug;

import com.jogamp.common.os.Platform;
import com.jogamp.opengl.math.geom.AABBox;

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
public final class FloatUtil {
  public static final boolean DEBUG = Debug.debug("Math");

  //
  // Matrix Ops
  //

  /**
   * Make matrix an identity matrix
   * @param m 4x4 matrix in column-major order (also result)
   * @param m_offset offset in given array <i>m</i>, i.e. start of the 4x4 matrix
   * @return given matrix for chaining
   */
  public static float[] makeIdentity(final float[] m, final int m_offset) {
      m[m_offset+0+4*0] = 1f;
      m[m_offset+1+4*0] = 0f;
      m[m_offset+2+4*0] = 0f;
      m[m_offset+3+4*0] = 0f;

      m[m_offset+0+4*1] = 0f;
      m[m_offset+1+4*1] = 1f;
      m[m_offset+2+4*1] = 0f;
      m[m_offset+3+4*1] = 0f;

      m[m_offset+0+4*2] = 0f;
      m[m_offset+1+4*2] = 0f;
      m[m_offset+2+4*2] = 1f;
      m[m_offset+3+4*2] = 0f;

      m[m_offset+0+4*3] = 0f;
      m[m_offset+1+4*3] = 0f;
      m[m_offset+2+4*3] = 0f;
      m[m_offset+3+4*3] = 1f;
      return m;
  }

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
   * Make matrix an identity matrix
   * @param m 4x4 matrix in column-major order (also result)
   * @return given matrix for chaining
   */
  public static FloatBuffer makeIdentity(final FloatBuffer m) {
      final int m_offset = m.position();
      m.put(m_offset+0+4*0, 1f);
      m.put(m_offset+1+4*0, 0f);
      m.put(m_offset+2+4*0, 0f);
      m.put(m_offset+3+4*0, 0f);

      m.put(m_offset+0+4*1, 0f);
      m.put(m_offset+1+4*1, 1f);
      m.put(m_offset+2+4*1, 0f);
      m.put(m_offset+3+4*1, 0f);

      m.put(m_offset+0+4*2, 0f);
      m.put(m_offset+1+4*2, 0f);
      m.put(m_offset+2+4*2, 1f);
      m.put(m_offset+3+4*2, 0f);

      m.put(m_offset+0+4*3, 0f);
      m.put(m_offset+1+4*3, 0f);
      m.put(m_offset+2+4*3, 0f);
      m.put(m_offset+3+4*3, 1f);
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
   * @param m_offset offset in given array <i>m</i>, i.e. start of the 4x4 matrix
   * @param initM if true, given matrix will be initialized w/ identity matrix,
   *              otherwise only the diagonal and last-row is set.
   *              The latter can be utilized to share a once {@link #makeIdentity(float[], int) identity set} matrix
   *              for {@link #makeScale(float[], int, boolean, float, float, float) scaling}
   *              and {@link #makeTranslation(float[], int, boolean, float, float, float) translation},
   *              while leaving the other fields untouched for performance reasons.
   * @return given matrix for chaining
   */
  public static float[] makeTranslation(final float[] m, final int m_offset, final boolean initM, final float tx, final float ty, final float tz) {
      if( initM ) {
          makeIdentity(m, m_offset);
      } else {
          m[m_offset+0+4*0] = 1;
          m[m_offset+1+4*1] = 1;
          m[m_offset+2+4*2] = 1;
          m[m_offset+3+4*3] = 1;
      }
      m[m_offset+0+4*3] = tx;
      m[m_offset+1+4*3] = ty;
      m[m_offset+2+4*3] = tz;
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
   * @param m_offset offset in given array <i>m</i>, i.e. start of the 4x4 matrix
   * @param initM if true, given matrix will be initialized w/ identity matrix,
   *              otherwise only the diagonal and last-row is set.
   *              The latter can be utilized to share a once {@link #makeIdentity(float[], int) identity set} matrix
   *              for {@link #makeScale(float[], int, boolean, float, float, float) scaling}
   *              and {@link #makeTranslation(float[], int, boolean, float, float, float) translation},
   *              while leaving the other fields untouched for performance reasons.
   * @return given matrix for chaining
   */
  public static float[] makeScale(final float[] m, final int m_offset, final boolean initM, final float sx, final float sy, final float sz) {
      if( initM ) {
          makeIdentity(m, m_offset);
      } else {
          m[m_offset+0+4*3] = 0;
          m[m_offset+1+4*3] = 0;
          m[m_offset+2+4*3] = 0;
          m[m_offset+3+4*3] = 1;
      }
      m[m_offset+0+4*0] = sx;
      m[m_offset+1+4*1] = sy;
      m[m_offset+2+4*2] = sz;
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
   * Make a rotation matrix from the given axis and angle in radians.
   * <pre>
        Rotation matrix (Column Order):
        xx(1-c)+c  xy(1-c)+zs xz(1-c)-ys 0
        xy(1-c)-zs yy(1-c)+c  yz(1-c)+xs 0
        xz(1-c)+ys yz(1-c)-xs zz(1-c)+c  0
        0          0          0          1
   * </pre>
   * <p>
   * All matrix fields are set.
   * </p>
   * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q38">Matrix-FAQ Q38</a>
   * @param m 4x4 matrix in column-major order (also result)
   * @param m_offset offset in given array <i>m</i>, i.e. start of the 4x4 matrix
   * @return given matrix for chaining
   */
  public static float[] makeRotationAxis(final float[] m, final int m_offset, final float angrad, float x, float y, float z, final float[] tmpVec3f) {
        final float c = cos(angrad);
        final float ic= 1.0f - c;
        final float s = sin(angrad);

        tmpVec3f[0]=x; tmpVec3f[1]=y; tmpVec3f[2]=z;
        VectorUtil.normalizeVec3(tmpVec3f);
        x = tmpVec3f[0]; y = tmpVec3f[1]; z = tmpVec3f[2];

        final float xy = x*y;
        final float xz = x*z;
        final float xs = x*s;
        final float ys = y*s;
        final float yz = y*z;
        final float zs = z*s;
        m[0+0*4+m_offset] = x*x*ic+c;
        m[1+0*4+m_offset] = xy*ic+zs;
        m[2+0*4+m_offset] = xz*ic-ys;
        m[3+0*4+m_offset] = 0;

        m[0+1*4+m_offset] = xy*ic-zs;
        m[1+1*4+m_offset] = y*y*ic+c;
        m[2+1*4+m_offset] = yz*ic+xs;
        m[3+1*4+m_offset] = 0;

        m[0+2*4+m_offset] = xz*ic+ys;
        m[1+2*4+m_offset] = yz*ic-xs;
        m[2+2*4+m_offset] = z*z*ic+c;
        m[3+2*4+m_offset] = 0;

        m[0+3*4+m_offset]  = 0f;
        m[1+3*4+m_offset]  = 0f;
        m[2+3*4+m_offset]  = 0f;
        m[3+3*4+m_offset]  = 1f;

        return m;
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
   * <p>
   * All matrix fields are set.
   * </p>
   * @param m 4x4 matrix in column-major order (also result)
   * @param m_offset offset in given array <i>m</i>, i.e. start of the 4x4 matrix
   * @param bankX the Euler pitch angle in radians. (rotation about the X axis)
   * @param headingY the Euler yaw angle in radians. (rotation about the Y axis)
   * @param attitudeZ the Euler roll angle in radians. (rotation about the Z axis)
   * @return given matrix for chaining
   * <p>
   * Implementation does not use Quaternion and hence is exposed to
   * <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q34">Gimbal-Lock</a>
   * </p>
   * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q36">Matrix-FAQ Q36</a>
   * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToMatrix/index.htm">euclideanspace.com-eulerToMatrix</a>
   */
  public static float[] makeRotationEuler(final float[] m, final int m_offset, final float bankX, final float headingY, final float attitudeZ) {
      // Assuming the angles are in radians.
      final float ch = cos(headingY);
      final float sh = sin(headingY);
      final float ca = cos(attitudeZ);
      final float sa = sin(attitudeZ);
      final float cb = cos(bankX);
      final float sb = sin(bankX);

      m[0+0*4+m_offset] =  ch*ca;
      m[0+1*4+m_offset] =  sh*sb    - ch*sa*cb;
      m[0+2*4+m_offset] =  ch*sa*sb + sh*cb;
      m[0+3*4+m_offset] =  0;

      m[1+0*4+m_offset] =  sa;
      m[1+1*4+m_offset] =  ca*cb;
      m[1+2*4+m_offset] = -ca*sb;
      m[1+3*4+m_offset] =  0;

      m[2+0*4+m_offset] = -sh*ca;
      m[2+1*4+m_offset] =  sh*sa*cb + ch*sb;
      m[2+2*4+m_offset] = -sh*sa*sb + ch*cb;
      m[2+3*4+m_offset] =  0;

      m[3+0*4+m_offset] =  0;
      m[3+1*4+m_offset] =  0;
      m[3+2*4+m_offset] =  0;
      m[3+3*4+m_offset] =  1;

      return m;
  }

  /**
   * Make given matrix the orthogonal matrix based on given parameters.
   * <pre>
      Ortho matrix (Column Order):
      2/dx  0     0    0
      0     2/dy  0    0
      0     0     2/dz 0
      tx    ty    tz   1
   * </pre>
   * <p>
   * All matrix fields are only set if <code>initM</code> is <code>true</code>.
   * </p>
   * @param m 4x4 matrix in column-major order (also result)
   * @param m_offset offset in given array <i>m</i>, i.e. start of the 4x4 matrix
   * @param initM if true, given matrix will be initialized w/ identity matrix,
   *              otherwise only the orthogonal fields are set.
   * @param left
   * @param right
   * @param bottom
   * @param top
   * @param zNear
   * @param zFar
   * @return given matrix for chaining
   */
  public static float[] makeOrtho(final float[] m, final int m_offset, final boolean initM,
                                  final float left, final float right,
                                  final float bottom, final float top,
                                  final float zNear, final float zFar) {
      if( initM ) {
          // m[m_offset+0+4*0] = 1f;
          m[m_offset+1+4*0] = 0f;
          m[m_offset+2+4*0] = 0f;
          m[m_offset+3+4*0] = 0f;

          m[m_offset+0+4*1] = 0f;
          // m[m_offset+1+4*1] = 1f;
          m[m_offset+2+4*1] = 0f;
          m[m_offset+3+4*1] = 0f;

          m[m_offset+0+4*2] = 0f;
          m[m_offset+1+4*2] = 0f;
          // m[m_offset+2+4*2] = 1f;
          m[m_offset+3+4*2] = 0f;

          // m[m_offset+0+4*3] = 0f;
          // m[m_offset+1+4*3] = 0f;
          // m[m_offset+2+4*3] = 0f;
          // m[m_offset+3+4*3] = 1f;
      }
      final float dx=right-left;
      final float dy=top-bottom;
      final float dz=zFar-zNear;
      final float tx=-1.0f*(right+left)/dx;
      final float ty=-1.0f*(top+bottom)/dy;
      final float tz=-1.0f*(zFar+zNear)/dz;

      m[m_offset+0+4*0] =  2.0f/dx;

      m[m_offset+1+4*1] =  2.0f/dy;

      m[m_offset+2+4*2] = -2.0f/dz;

      m[m_offset+0+4*3] = tx;
      m[m_offset+1+4*3] = ty;
      m[m_offset+2+4*3] = tz;
      m[m_offset+3+4*3] = 1f;

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
   */
  public static float[] makeFrustum(final float[] m, final int m_offset, final boolean initM,
                                    final float left, final float right,
                                    final float bottom, final float top,
                                    final float zNear, final float zFar) {
      if(zNear<=0.0f||zFar<0.0f) {
          throw new GLException("GL_INVALID_VALUE: zNear and zFar must be positive, and zNear>0");
      }
      if(left==right || top==bottom) {
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
   * Make given matrix the perspective matrix based on given parameters.
   * <p>
   * All matrix fields are only set if <code>initM</code> is <code>true</code>.
   * </p>
   *
   * @param m 4x4 matrix in column-major order (also result)
   * @param m_offset offset in given array <i>m</i>, i.e. start of the 4x4 matrix
   * @param initM if true, given matrix will be initialized w/ identity matrix,
   *              otherwise only the non-zero fields are set.
   * @param fovy angle in radians
   * @param aspect
   * @param zNear
   * @param zFar
   * @return given matrix for chaining
   */
  public static float[] makePerspective(final float[] m, final int m_off, final boolean initM,
                                        final float fovy, final float aspect, final float zNear, final float zFar) {
      float top=(float)Math.tan(fovy)*zNear;
      float bottom=-1.0f*top;
      float left=aspect*bottom;
      float right=aspect*top;
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
      m[m_offset + 1 * 4 + 0] = mat4Tmp[1+side_off]; // side
      m[m_offset + 2 * 4 + 0] = mat4Tmp[2+side_off]; // side
      m[m_offset + 3 * 4 + 0] = 0;

      m[m_offset + 0 * 4 + 1] = mat4Tmp[0+up2_off]; // up2
      m[m_offset + 1 * 4 + 1] = mat4Tmp[1+up2_off]; // up2
      m[m_offset + 2 * 4 + 1] = mat4Tmp[2+up2_off]; // up2
      m[m_offset + 3 * 4 + 1] = 0;

      m[m_offset + 0 * 4 + 2] = -mat4Tmp[0]; // forward
      m[m_offset + 1 * 4 + 2] = -mat4Tmp[1]; // forward
      m[m_offset + 2 * 4 + 2] = -mat4Tmp[2]; // forward
      m[m_offset + 3 * 4 + 2] = 0;

      m[m_offset + 0 * 4 + 3] = 0;
      m[m_offset + 1 * 4 + 3] = 0;
      m[m_offset + 2 * 4 + 3] = 0;
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
   * All matrix fields are set.
   * </p>
   * @param m 4x4 matrix in column-major order, result only
   * @param m_offset offset in given array <i>m</i>, i.e. start of the 4x4 matrix
   * @param x
   * @param y
   * @param deltaX
   * @param deltaY
   * @param viewport 4 component viewport vector
   * @param viewport_offset
   * @param mat4Tmp temp float[16] storage
   * @return given matrix <code>m</code> for chaining or <code>null</code> if either delta value is <= zero.
   */
  public static float[] makePick(final float[] m, final int m_offset,
                                 final float x, final float y,
                                 final float deltaX, final float deltaY,
                                 final int[] viewport, final int viewport_offset,
                                 final float[] mat4Tmp) {
      if (deltaX <= 0 || deltaY <= 0) {
          return null;
      }

      /* Translate and scale the picked region to the entire window */
      makeTranslation(m, m_offset, true,
              (viewport[2+viewport_offset] - 2 * (x - viewport[0+viewport_offset])) / deltaX,
              (viewport[3+viewport_offset] - 2 * (y - viewport[1+viewport_offset])) / deltaY,
              0);
      makeScale(mat4Tmp, true,
              viewport[2+viewport_offset] / deltaX, viewport[3+viewport_offset] / deltaY, 1.0f);
      multMatrix(m, m_offset, mat4Tmp, 0);
      return m;
  }

  /**
   * Transpose the given matrix.
   *
   * @param msrc 4x4 matrix in column-major order, the source
   * @param msrc_offset offset in given array <i>msrc</i>, i.e. start of the 4x4 matrix
   * @param mres 4x4 matrix in column-major order, the result - may be <code>msrc</code> (in-place)
   * @param mres_offset offset in given array <i>mres</i>, i.e. start of the 4x4 matrix - may be <code>msrc_offset</code> (in-place)
   * @return given result matrix <i>mres</i> for chaining
   */
  public static float[] transposeMatrix(final float[] msrc, final int msrc_offset, final float[] mres, final int mres_offset) {
      for (int i = 0; i < 4; i++) {
          final int i4 = i*4;
          for (int j = 0; j < 4; j++) {
              mres[mres_offset+j+i4] = msrc[msrc_offset+i+j*4];
          }
      }
      return mres;
  }

  /**
   * Transpose the given matrix in place.
   *
   * @param m 4x4 matrix in column-major order, the source
   * @param m_offset offset in given array <i>msrc</i>, i.e. start of the 4x4 matrix
   * @param temp temporary 4*4 float storage
   * @return given result matrix <i>m</i> for chaining
   */
  public static float[] transposeMatrix(final float[] m, final int m_offset, final float[/*4*4*/] temp) {
      int i, j;
      for (i = 0; i < 4; i++) {
          final int i4 = i*4;
          for (j = 0; j < 4; j++) {
              temp[i4+j] = m[i4+j+m_offset];
          }
      }
      for (i = 0; i < 4; i++) {
          final int i4 = i*4;
          for (j = 0; j < 4; j++) {
              m[m_offset+j+i4] = temp[i+j*4];
          }
      }
      return m;
  }

  /**
   * Transpose the given matrix.
   *
   * @param msrc 4x4 matrix in column-major order, the source
   * @param mres 4x4 matrix in column-major order, the result - may be <code>msrc</code> (in-place)
   * @return given result matrix <i>mres</i> for chaining
   */
  public static FloatBuffer transposeMatrix(final FloatBuffer msrc, final FloatBuffer mres) {
        final int msrc_offset = msrc.position();
        final int mres_offset = mres.position();
        for (int i = 0; i < 4; i++) {
            final int i4 = i*4;
            for (int j = 0; j < 4; j++) {
                mres.put(mres_offset+j+i4, msrc.get(msrc_offset+i+j*4));
            }
        }
        return mres;
  }

  /**
   * Transpose the given matrix in place.
   *
   * @param m 4x4 matrix in column-major order, the source
   * @param temp temporary 4*4 float storage
   * @return given matrix <i>m</i> for chaining
   */
  public static FloatBuffer transposeMatrix(FloatBuffer m, final float[/*4*4*/] temp) {
      final int m_offset = m.position();
      int i, j;
      for (i = 0; i < 4; i++) {
          final int i4 = i*4;
          for (j = 0; j < 4; j++) {
              temp[i4+j] = m.get(i4+j+m_offset);
          }
      }
      for (i = 0; i < 4; i++) {
          final int i4 = i*4;
          for (j = 0; j < 4; j++) {
              m.put(m_offset+j+i4, temp[i+j*4]);
          }
      }
      return m;
  }

  /**
   * Invert the given matrix.
   * <p>
   * Returns <code>null</code> if inversion is not possible,
   * e.g. matrix is singular due to a bad matrix.
   * </p>
   *
   * @param msrc 4x4 matrix in column-major order, the source
   * @param msrc_offset offset in given array <i>msrc</i>, i.e. start of the 4x4 matrix
   * @param mres 4x4 matrix in column-major order, the result - may be <code>msrc</code> (in-place)
   * @param mres_offset offset in given array <i>mres</i>, i.e. start of the 4x4 matrix - may be <code>msrc_offset</code> (in-place)
   * @param temp temporary 4*4 float storage
   * @return given result matrix <i>mres</i> for chaining if successful, otherwise <code>null</code>. See above.
   */
  public static float[] invertMatrix(final float[] msrc, final int msrc_offset, final float[] mres, final int mres_offset, final float[/*4*4*/] temp) {
      int i, j, k, swap;
      float t;
      for (i = 0; i < 4; i++) {
          final int i4 = i*4;
          for (j = 0; j < 4; j++) {
              temp[i4+j] = msrc[i4+j+msrc_offset];
          }
      }
      makeIdentity(mres, mres_offset);

      for (i = 0; i < 4; i++) {
          final int i4 = i*4;

          //
          // Look for largest element in column
          //
          swap = i;
          for (j = i + 1; j < 4; j++) {
              if (Math.abs(temp[j*4+i]) > Math.abs(temp[i4+i])) {
                  swap = j;
              }
          }

          if (swap != i) {
              final int swap4 = swap*4;
              //
              // Swap rows.
              //
              for (k = 0; k < 4; k++) {
                  t = temp[i4+k];
                  temp[i4+k] = temp[swap4+k];
                  temp[swap4+k] = t;

                  t = mres[i4+k+mres_offset];
                  mres[i4+k+mres_offset] = mres[swap4+k+mres_offset];
                  mres[swap4+k+mres_offset] = t;
              }
          }

          if (temp[i4+i] == 0) {
              //
              // No non-zero pivot. The matrix is singular, which shouldn't
              // happen. This means the user gave us a bad matrix.
              //
              return null;
          }

          t = temp[i4+i];
          for (k = 0; k < 4; k++) {
              temp[i4+k] /= t;
              mres[i4+k+mres_offset] /= t;
          }
          for (j = 0; j < 4; j++) {
              if (j != i) {
                  final int j4 = j*4;
                  t = temp[j4+i];
                  for (k = 0; k < 4; k++) {
                      temp[j4+k] -= temp[i4+k] * t;
                      mres[j4+k+mres_offset] -= mres[i4+k+mres_offset]*t;
                  }
              }
          }
      }
      return mres;
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
   * @param temp temporary 4*4 float storage
   * @return given result matrix <i>mres</i> for chaining if successful, otherwise <code>null</code>. See above.
   */
  public static FloatBuffer invertMatrix(final FloatBuffer msrc, final FloatBuffer mres, final float[/*4*4*/] temp) {
    int i, j, k, swap;
    float t;

    final int msrc_offset = msrc.position();
    final int mres_offset = mres.position();

    for (i = 0; i < 4; i++) {
      final int i4 = i*4;
      for (j = 0; j < 4; j++) {
        temp[i4+j] = msrc.get(i4+j + msrc_offset);
      }
    }
    makeIdentity(mres);

    for (i = 0; i < 4; i++) {
      final int i4 = i*4;

      //
      // Look for largest element in column
      //
      swap = i;
      for (j = i + 1; j < 4; j++) {
        if (Math.abs(temp[j*4+i]) > Math.abs(temp[i4+i])) {
          swap = j;
        }
      }

      if (swap != i) {
        final int swap4 = swap*4;
        //
        // Swap rows.
        //
        for (k = 0; k < 4; k++) {
          t = temp[i4+k];
          temp[i4+k] = temp[swap4+k];
          temp[swap4+k] = t;

          t = mres.get(i4+k + mres_offset);
          mres.put(i4+k + mres_offset, mres.get(swap4+k + mres_offset));
          mres.put(swap4+k + mres_offset, t);
        }
      }

      if (temp[i4+i] == 0) {
        //
        // No non-zero pivot. The matrix is singular, which shouldn't
        // happen. This means the user gave us a bad matrix.
        //
        return null;
      }

      t = temp[i4+i];
      for (k = 0; k < 4; k++) {
        temp[i4+k] /= t;
        final int z = i4+k + mres_offset;
        mres.put(z, mres.get(z) / t);
      }
      for (j = 0; j < 4; j++) {
        if (j != i) {
          final int j4 = j*4;
          t = temp[j4+i];
          for (k = 0; k < 4; k++) {
            temp[j4+k] -= temp[i4+k] * t;
            final int z = j4+k + mres_offset;
            mres.put(z, mres.get(z) - mres.get(i4+k + mres_offset) * t);
          }
        }
      }
    }
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
  public static boolean mapObjToWinCoords(final float objx, final float objy, final float objz,
                                          final float[] modelMatrix, final int modelMatrix_offset,
                                          final float[] projMatrix, final int projMatrix_offset,
                                          final int[] viewport, final int viewport_offset,
                                          final float[] win_pos, int win_pos_offset,
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
   * Map object coordinates to window coordinates.
   * <p>
   * Traditional <code>gluProject</code> implementation.
   * </p>
   *
   * @param objx
   * @param objy
   * @param objz
   * @param mat4PMv [projection] x [modelview] matrix, i.e. P x Mv
   * @param viewport 4 component viewport vector
   * @param viewport_offset
   * @param win_pos 3 component window coordinate, the result
   * @param win_pos_offset
   * @param vec4Tmp1 4 component vector for temp storage
   * @param vec4Tmp2 4 component vector for temp storage
   * @return true if successful, otherwise false (z is 1)
   */
  public static boolean mapObjToWinCoords(final float objx, final float objy, final float objz,
                                          final float[/*16*/] mat4PMv,
                                          final int[] viewport, final int viewport_offset,
                                          final float[] win_pos, int win_pos_offset,
                                          final float[/*4*/] vec4Tmp1, final float[/*4*/] vec4Tmp2) {
      vec4Tmp2[0] = objx;
      vec4Tmp2[1] = objy;
      vec4Tmp2[2] = objz;
      vec4Tmp2[3] = 1.0f;

      // vec4Tmp1 = P * Mv * o
      multMatrixVec(mat4PMv, vec4Tmp2, vec4Tmp1);

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
  public static boolean mapWinToObjCoords(final float winx, final float winy, final float winz,
                                          final float[] modelMatrix, final int modelMatrix_offset,
                                          final float[] projMatrix, final int projMatrix_offset,
                                          final int[] viewport, final int viewport_offset,
                                          final float[] obj_pos, final int obj_pos_offset,
                                          final float[/*16*/] mat4Tmp1, final float[/*16*/] mat4Tmp2) {
    // mat4Tmp1 = P x Mv
    multMatrix(projMatrix, projMatrix_offset, modelMatrix, modelMatrix_offset, mat4Tmp1, 0);

    // mat4Tmp1 = Inv(P x Mv)
    if ( null == invertMatrix(mat4Tmp1, 0, mat4Tmp1, 0, mat4Tmp2) ) {
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
   * Traditional <code>gluUnProject</code> implementation.
   * </p>
   *
   * @param winx
   * @param winy
   * @param winz
   * @param mat4PMvI inverse [projection] x [modelview] matrix, i.e. Inv(P x Mv)
   * @param viewport 4 component viewport vector
   * @param viewport_offset
   * @param obj_pos 3 component object coordinate, the result
   * @param obj_pos_offset
   * @param vec4Tmp1 4 component vector for temp storage
   * @param vec4Tmp2 4 component vector for temp storage
   * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
   */
  public static boolean mapWinToObjCoords(final float winx, final float winy, final float winz,
                                          final float[/*16*/] mat4PMvI,
                                          final int[] viewport, final int viewport_offset,
                                          final float[] obj_pos, final int obj_pos_offset,
                                          final float[/*4*/] vec4Tmp1, final float[/*4*/] vec4Tmp2) {
    vec4Tmp1[0] = winx;
    vec4Tmp1[1] = winy;
    vec4Tmp1[2] = winz;
    vec4Tmp1[3] = 1.0f;

    // Map x and y from window coordinates
    vec4Tmp1[0] = (vec4Tmp1[0] - viewport[0+viewport_offset]) / viewport[2+viewport_offset];
    vec4Tmp1[1] = (vec4Tmp1[1] - viewport[1+viewport_offset]) / viewport[3+viewport_offset];

    // Map to range -1 to 1
    vec4Tmp1[0] = vec4Tmp1[0] * 2 - 1;
    vec4Tmp1[1] = vec4Tmp1[1] * 2 - 1;
    vec4Tmp1[2] = vec4Tmp1[2] * 2 - 1;

    // object raw coords = Inv(P x Mv) *  winPos  -> mat4Tmp2
    multMatrixVec(mat4PMvI, vec4Tmp1, vec4Tmp2);

    if (vec4Tmp2[3] == 0.0) {
      return false;
    }

    vec4Tmp2[3] = 1.0f / vec4Tmp2[3];

    obj_pos[0+obj_pos_offset] = vec4Tmp2[0] * vec4Tmp2[3];
    obj_pos[1+obj_pos_offset] = vec4Tmp2[1] * vec4Tmp2[3];
    obj_pos[2+obj_pos_offset] = vec4Tmp2[2] * vec4Tmp2[3];

    return true;
  }

  /**
   * Map two window coordinates to two object coordinates,
   * distinguished by their z component.
   *
   * @param winx
   * @param winy
   * @param winz1
   * @param winz2
   * @param mat4PMvI inverse [projection] x [modelview] matrix, i.e. Inv(P x Mv)
   * @param viewport 4 component viewport vector
   * @param viewport_offset
   * @param obj1_pos 3 component object coordinate, the result for winz1
   * @param obj1_pos_offset
   * @param obj2_pos 3 component object coordinate, the result for winz2
   * @param obj2_pos_offset
   * @param vec4Tmp1 4 component vector for temp storage
   * @param vec4Tmp2 4 component vector for temp storage
   * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
   */
  public static boolean mapWinToObjCoords(final float winx, final float winy, final float winz1, final float winz2,
                                          final float[/*16*/] mat4PMvI,
                                          final int[] viewport, final int viewport_offset,
                                          final float[] obj1_pos, final int obj1_pos_offset,
                                          final float[] obj2_pos, final int obj2_pos_offset,
                                          final float[/*4*/] vec4Tmp1, final float[/*4*/] vec4Tmp2) {
    vec4Tmp1[0] = winx;
    vec4Tmp1[1] = winy;
    vec4Tmp1[3] = 1.0f;

    // Map x and y from window coordinates
    vec4Tmp1[0] = (vec4Tmp1[0] - viewport[0+viewport_offset]) / viewport[2+viewport_offset];
    vec4Tmp1[1] = (vec4Tmp1[1] - viewport[1+viewport_offset]) / viewport[3+viewport_offset];

    // Map to range -1 to 1
    vec4Tmp1[0] = vec4Tmp1[0] * 2 - 1;
    vec4Tmp1[1] = vec4Tmp1[1] * 2 - 1;

    //
    // winz1
    //
    vec4Tmp1[2] = winz1;
    vec4Tmp1[2] = vec4Tmp1[2] * 2 - 1;

    // object raw coords = Inv(P x Mv) *  winPos  -> mat4Tmp2
    multMatrixVec(mat4PMvI, vec4Tmp1, vec4Tmp2);

    if (vec4Tmp2[3] == 0.0) {
      return false;
    }

    vec4Tmp2[3] = 1.0f / vec4Tmp2[3];

    obj1_pos[0+obj1_pos_offset] = vec4Tmp2[0] * vec4Tmp2[3];
    obj1_pos[1+obj1_pos_offset] = vec4Tmp2[1] * vec4Tmp2[3];
    obj1_pos[2+obj1_pos_offset] = vec4Tmp2[2] * vec4Tmp2[3];

    //
    // winz2
    //
    vec4Tmp1[2] = winz2;
    vec4Tmp1[2] = vec4Tmp1[2] * 2 - 1;

    // object raw coords = Inv(P x Mv) *  winPos  -> mat4Tmp2
    multMatrixVec(mat4PMvI, vec4Tmp1, vec4Tmp2);

    if (vec4Tmp2[3] == 0.0) {
      return false;
    }

    vec4Tmp2[3] = 1.0f / vec4Tmp2[3];

    obj2_pos[0+obj2_pos_offset] = vec4Tmp2[0] * vec4Tmp2[3];
    obj2_pos[1+obj2_pos_offset] = vec4Tmp2[1] * vec4Tmp2[3];
    obj2_pos[2+obj2_pos_offset] = vec4Tmp2[2] * vec4Tmp2[3];

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
  public static boolean mapWinToObjCoords(final float winx, final float winy, final float winz, final float clipw,
                                          float[] modelMatrix, int modelMatrix_offset,
                                          float[] projMatrix, int projMatrix_offset,
                                          int[] viewport, int viewport_offset,
                                          float near, float far,
                                          float[] obj_pos, int obj_pos_offset,
                                          final float[/*16*/] mat4Tmp1, final float[/*16*/] mat4Tmp2) {
    // mat4Tmp1 = P x Mv
    multMatrix(projMatrix, projMatrix_offset, modelMatrix, modelMatrix_offset, mat4Tmp1, 0);

    // mat4Tmp1 = Inv(P x Mv)
    if ( null == invertMatrix(mat4Tmp1, 0, mat4Tmp1, 0, mat4Tmp2) ) {
      return false;
    }

    mat4Tmp2[0] = winx;
    mat4Tmp2[1] = winy;
    mat4Tmp2[2] = winz;
    mat4Tmp2[3] = 1.0f;

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

    mat4Tmp2[3+raw_off] = 1.0f / mat4Tmp2[3+raw_off];

    obj_pos[0+obj_pos_offset] = mat4Tmp2[0+raw_off];
    obj_pos[1+obj_pos_offset] = mat4Tmp2[1+raw_off];
    obj_pos[2+obj_pos_offset] = mat4Tmp2[2+raw_off];
    obj_pos[3+obj_pos_offset] = mat4Tmp2[3+raw_off];

    return true;
  }


  /**
   * Map two window coordinates w/ shared X/Y and distinctive Z
   * to a {@link Ray}. The resulting {@link Ray} maybe used for <i>picking</i>
   * using a {@link AABBox#getRayIntersection(Ray, float[]) bounding box}.
   * <p>
   * Notes for picking <i>winz0</i> and <i>winz1</i>:
   * <ul>
   *   <li>see {@link #getZBufferEpsilon(int, float, float)}</li>
   *   <li>see {@link #getZBufferValue(int, float, float, float)}</li>
   *   <li>see {@link #getOrthoWinZ(float, float, float)}</li>
   * </ul>
   * </p>
   * @param winx
   * @param winy
   * @param winz0
   * @param winz1
   * @param modelMatrix 4x4 modelview matrix
   * @param modelMatrix_offset
   * @param projMatrix 4x4 projection matrix
   * @param projMatrix_offset
   * @param viewport 4 component viewport vector
   * @param viewport_offset
   * @param ray storage for the resulting {@link Ray}
   * @param mat4Tmp1 16 component matrix for temp storage
   * @param mat4Tmp2 16 component matrix for temp storage
   * @param vec4Tmp2 4 component vector for temp storage
   * @return true if successful, otherwise false (failed to invert matrix, or becomes z is infinity)
   */
  public static boolean mapWinToRay(final float winx, final float winy, final float winz0, final float winz1,
                                    final float[] modelMatrix, final int modelMatrix_offset,
                                    final float[] projMatrix, final int projMatrix_offset,
                                    final int[] viewport, final int viewport_offset,
                                    final Ray ray,
                                    final float[/*16*/] mat4Tmp1, final float[/*16*/] mat4Tmp2, final float[/*4*/] vec4Tmp2) {
      // mat4Tmp1 = P x Mv
      multMatrix(projMatrix, projMatrix_offset, modelMatrix, modelMatrix_offset, mat4Tmp1, 0);

      // mat4Tmp1 = Inv(P x Mv)
      if ( null == invertMatrix(mat4Tmp1, 0, mat4Tmp1, 0, mat4Tmp2) ) {
          return false;
      }
      if( mapWinToObjCoords(winx, winy, winz0, winz1, mat4Tmp1,
                            viewport, viewport_offset,
                            ray.orig, 0, ray.dir, 0,
                            mat4Tmp2, vec4Tmp2) ) {
          VectorUtil.normalizeVec3( VectorUtil.subVec3(ray.dir, ray.dir, ray.orig) );
          return true;
      } else {
          return false;
      }
  }

  /**
   * Multiply matrix: [d] = [a] x [b]
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   */
  public static void multMatrix(final float[] a, final int a_off, final float[] b, final int b_off, float[] d, final int d_off) {
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final int a_off_i = a_off+i;
        final int d_off_i = d_off+i;
        final float ai0=a[a_off_i+0*4],  ai1=a[a_off_i+1*4],  ai2=a[a_off_i+2*4],  ai3=a[a_off_i+3*4]; // row-i of a
        d[d_off_i+0*4] = ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] ;
        d[d_off_i+1*4] = ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] ;
        d[d_off_i+2*4] = ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] ;
        d[d_off_i+3*4] = ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] ;
     }
  }

  /**
   * Multiply matrix: [d] = [a] x [b]
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   */
  public static void multMatrix(final float[] a, final float[] b, float[] d) {
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final float ai0=a[i+0*4],  ai1=a[i+1*4],  ai2=a[i+2*4],  ai3=a[i+3*4]; // row-i of a
        d[i+0*4] = ai0 * b[0+0*4] + ai1 * b[1+0*4] + ai2 * b[2+0*4] + ai3 * b[3+0*4] ;
        d[i+1*4] = ai0 * b[0+1*4] + ai1 * b[1+1*4] + ai2 * b[2+1*4] + ai3 * b[3+1*4] ;
        d[i+2*4] = ai0 * b[0+2*4] + ai1 * b[1+2*4] + ai2 * b[2+2*4] + ai3 * b[3+2*4] ;
        d[i+3*4] = ai0 * b[0+3*4] + ai1 * b[1+3*4] + ai2 * b[2+3*4] + ai3 * b[3+3*4] ;
     }
  }

  /**
   * Multiply matrix: [a] = [a] x [b]
   * @param a 4x4 matrix in column-major order (also result)
   * @param b 4x4 matrix in column-major order
   */
  public static void multMatrix(final float[] a, final int a_off, final float[] b, final int b_off) {
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
   * Multiply matrix: [a] = [a] x [b]
   * @param a 4x4 matrix in column-major order (also result)
   * @param b 4x4 matrix in column-major order
   */
  public static void multMatrix(final float[] a, final float[] b) {
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final float ai0=a[i+0*4],  ai1=a[i+1*4],  ai2=a[i+2*4],  ai3=a[i+3*4]; // row-i of a
        a[i+0*4] = ai0 * b[0+0*4] + ai1 * b[1+0*4] + ai2 * b[2+0*4] + ai3 * b[3+0*4] ;
        a[i+1*4] = ai0 * b[0+1*4] + ai1 * b[1+1*4] + ai2 * b[2+1*4] + ai3 * b[3+1*4] ;
        a[i+2*4] = ai0 * b[0+2*4] + ai1 * b[1+2*4] + ai2 * b[2+2*4] + ai3 * b[3+2*4] ;
        a[i+3*4] = ai0 * b[0+3*4] + ai1 * b[1+3*4] + ai2 * b[2+3*4] + ai3 * b[3+3*4] ;
     }
  }

  /**
   * Multiply matrix: [d] = [a] x [b]
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   */
  public static void multMatrix(final float[] a, final int a_off, final float[] b, final int b_off, final FloatBuffer d) {
     final int d_off = d.position();
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final int a_off_i = a_off+i;
        final int d_off_i = d_off+i;
        final float ai0=a[a_off_i+0*4],  ai1=a[a_off_i+1*4],  ai2=a[a_off_i+2*4],  ai3=a[a_off_i+3*4]; // row-i of a
        d.put(d_off_i+0*4 , ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] );
        d.put(d_off_i+1*4 , ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] );
        d.put(d_off_i+2*4 , ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] );
        d.put(d_off_i+3*4 , ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] );
     }
  }

  /**
   * Multiply matrix: [d] = [a] x [b]
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   */
  public static void multMatrix(final FloatBuffer a, final float[] b, final int b_off, final FloatBuffer d) {
     final int a_off = a.position();
     final int d_off = d.position();
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final int a_off_i = a_off+i;
        final int d_off_i = d_off+i;
        final float ai0=a.get(a_off_i+0*4),  ai1=a.get(a_off_i+1*4),  ai2=a.get(a_off_i+2*4),  ai3=a.get(a_off_i+3*4); // row-i of a
        d.put(d_off_i+0*4 , ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] );
        d.put(d_off_i+1*4 , ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] );
        d.put(d_off_i+2*4 , ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] );
        d.put(d_off_i+3*4 , ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] );
     }
  }

  /**
   * Multiply matrix: [a] = [a] x [b]
   * @param a 4x4 matrix in column-major order (also result)
   * @param b 4x4 matrix in column-major order
   */
  public static void multMatrix(final FloatBuffer a, final float[] b, final int b_off) {
     final int a_off = a.position();
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final int a_off_i = a_off+i;
        final float ai0=a.get(a_off_i+0*4),  ai1=a.get(a_off_i+1*4),  ai2=a.get(a_off_i+2*4),  ai3=a.get(a_off_i+3*4); // row-i of a
        a.put(a_off_i+0*4 , ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] );
        a.put(a_off_i+1*4 , ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] );
        a.put(a_off_i+2*4 , ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] );
        a.put(a_off_i+3*4 , ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] );
     }
  }

  /**
   * Multiply matrix: [d] = [a] x [b]
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   */
  public static void multMatrix(final FloatBuffer a, final FloatBuffer b, final FloatBuffer d) {
     final int a_off = a.position();
     final int b_off = b.position();
     final int d_off = d.position();
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final int a_off_i = a_off+i;
        final int d_off_i = d_off+i;
        final float ai0=a.get(a_off_i+0*4),  ai1=a.get(a_off_i+1*4),  ai2=a.get(a_off_i+2*4),  ai3=a.get(a_off_i+3*4); // row-i of a
        d.put(d_off_i+0*4 , ai0 * b.get(b_off+0+0*4) + ai1 * b.get(b_off+1+0*4) + ai2 * b.get(b_off+2+0*4) + ai3 * b.get(b_off+3+0*4) );
        d.put(d_off_i+1*4 , ai0 * b.get(b_off+0+1*4) + ai1 * b.get(b_off+1+1*4) + ai2 * b.get(b_off+2+1*4) + ai3 * b.get(b_off+3+1*4) );
        d.put(d_off_i+2*4 , ai0 * b.get(b_off+0+2*4) + ai1 * b.get(b_off+1+2*4) + ai2 * b.get(b_off+2+2*4) + ai3 * b.get(b_off+3+2*4) );
        d.put(d_off_i+3*4 , ai0 * b.get(b_off+0+3*4) + ai1 * b.get(b_off+1+3*4) + ai2 * b.get(b_off+2+3*4) + ai3 * b.get(b_off+3+3*4) );
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
   * Multiply matrix: [d] = [a] x [b]
   * @param a 4x4 matrix in column-major order
   * @param b 4x4 matrix in column-major order
   * @param d result a*b in column-major order
   */
  public static void multMatrix(final FloatBuffer a, final FloatBuffer b, final float[] d, final int d_off) {
     final int a_off = a.position();
     final int b_off = b.position();
     for (int i = 0; i < 4; i++) {
        // one row in column-major order
        final int a_off_i = a_off+i;
        final int d_off_i = d_off+i;
        final float ai0=a.get(a_off_i+0*4),  ai1=a.get(a_off_i+1*4),  ai2=a.get(a_off_i+2*4),  ai3=a.get(a_off_i+3*4); // row-i of a
        d[d_off_i+0*4] = ai0 * b.get(b_off+0+0*4) + ai1 * b.get(b_off+1+0*4) + ai2 * b.get(b_off+2+0*4) + ai3 * b.get(b_off+3+0*4) ;
        d[d_off_i+1*4] = ai0 * b.get(b_off+0+1*4) + ai1 * b.get(b_off+1+1*4) + ai2 * b.get(b_off+2+1*4) + ai3 * b.get(b_off+3+1*4) ;
        d[d_off_i+2*4] = ai0 * b.get(b_off+0+2*4) + ai1 * b.get(b_off+1+2*4) + ai2 * b.get(b_off+2+2*4) + ai3 * b.get(b_off+3+2*4) ;
        d[d_off_i+3*4] = ai0 * b.get(b_off+0+3*4) + ai1 * b.get(b_off+1+3*4) + ai2 * b.get(b_off+2+3*4) + ai3 * b.get(b_off+3+3*4) ;
     }
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
   * @param m_in 4x4 matrix in column-major order
   * @param m_in_off
   * @param v_in 4-component column-vector
   * @param v_out m_in * v_in
   */
  public static void multMatrixVec(final float[] m_in, final int m_in_off,
                                   final float[] v_in, final int v_in_off,
                                   final float[] v_out, final int v_out_off) {
    for (int i = 0; i < 4; i++) {
      // (one matrix row in column-major order) X (column vector)
      final int i_m_in_off = i+m_in_off;
      v_out[i + v_out_off] =
        v_in[0+v_in_off] * m_in[0*4+i_m_in_off] +
        v_in[1+v_in_off] * m_in[1*4+i_m_in_off] +
        v_in[2+v_in_off] * m_in[2*4+i_m_in_off] +
        v_in[3+v_in_off] * m_in[3*4+i_m_in_off];
    }
  }

  /**
   * @param m_in 4x4 matrix in column-major order
   * @param m_in_off
   * @param v_in 4-component column-vector
   * @param v_out m_in * v_in
   */
  public static void multMatrixVec(final float[] m_in, final float[] v_in, final float[] v_out) {
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
  public static void multMatrixVec(final FloatBuffer m_in, final float[] v_in, final int v_in_off, final float[] v_out, final int v_out_off) {
    final int m_in_off = m_in.position();
    for (int i = 0; i < 4; i++) {
      // (one matrix row in column-major order) X (column vector)
      final int i_m_in_off = i+m_in_off;
      v_out[i+v_out_off] =
        v_in[0+v_in_off] * m_in.get(0*4+i_m_in_off) +
        v_in[1+v_in_off] * m_in.get(1*4+i_m_in_off) +
        v_in[2+v_in_off] * m_in.get(2*4+i_m_in_off) +
        v_in[3+v_in_off] * m_in.get(3*4+i_m_in_off);
    }
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
   * @param m_in 4x4 matrix in column-major order
   * @param v_in 4-component column-vector
   * @param v_out m_in * v_in
   */
  public static void multMatrixVec(final FloatBuffer m_in, final FloatBuffer v_in, final FloatBuffer v_out) {
    final int v_in_off = v_in.position();
    final int v_out_off = v_out.position();
    final int m_in_off = m_in.position();
    for (int i = 0; i < 4; i++) {
      // (one matrix row in column-major order) X (column vector)
      final int i_m_in_off = i+m_in_off;
      v_out.put(i + v_out_off,
              v_in.get(0+v_in_off) * m_in.get(0*4+i_m_in_off) +
              v_in.get(1+v_in_off) * m_in.get(1*4+i_m_in_off) +
              v_in.get(2+v_in_off) * m_in.get(2*4+i_m_in_off) +
              v_in.get(3+v_in_off) * m_in.get(3*4+i_m_in_off));
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
  public static void copyMatrixColumn(final float[] m_in, final int m_in_off, final int column, final float[] v_out, final int v_out_off) {
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
  public static void copyMatrixRow(final float[] m_in, final int m_in_off, final int row, final float[] v_out, final int v_out_off) {
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
  public static StringBuilder matrixRowToString(StringBuilder sb, final String f,
                                                final FloatBuffer a, final int aOffset,
                                                final int rows, final int columns, final boolean rowMajorOrder, final int row) {
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
  public static StringBuilder matrixRowToString(StringBuilder sb, final String f,
                                                final float[] a, final int aOffset, final int rows, final int columns, final boolean rowMajorOrder, final int row) {
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
  public static StringBuilder matrixToString(StringBuilder sb, final String rowPrefix, final String f,
                                             final FloatBuffer a, final int aOffset, final int rows, final int columns, final boolean rowMajorOrder) {
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
  public static StringBuilder matrixToString(StringBuilder sb, final String rowPrefix, final String f,
                                             final float[] a, final int aOffset, final int rows, final int columns, final boolean rowMajorOrder) {
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
  public static StringBuilder matrixToString(StringBuilder sb, final String rowPrefix, final String f,
                                             final FloatBuffer a, final int aOffset, final FloatBuffer b, final int bOffset,
                                             final int rows, final int columns, final boolean rowMajorOrder) {
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
  public static StringBuilder matrixToString(StringBuilder sb, final String rowPrefix, final String f,
                                             final float[] a, final int aOffset, final float[] b, final int bOffset,
                                             final int rows, final int columns, final boolean rowMajorOrder) {
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
   * (1f/zNear-1f/orthoDist)/(1f/zNear-1f/zFar);
   */
  public static float getOrthoWinZ(final float orthoZ, final float zNear, final float zFar) {
      return (1f/zNear-1f/orthoZ)/(1f/zNear-1f/zFar);
  }

}