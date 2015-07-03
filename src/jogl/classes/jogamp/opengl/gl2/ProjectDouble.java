/*
** License Applicability. Except to the extent portions of this file are
** made subject to an alternative license as permitted in the SGI Free
** Software License B, Version 2.0 (the "License"), the contents of this
** file are subject only to the provisions of the License. You may not use
** this file except in compliance with the License. You may obtain a copy
** of the License at Silicon Graphics, Inc., attn: Legal Services, 1600
** Amphitheatre Parkway, Mountain View, CA 94043-1351, or at:
**
** http://oss.sgi.com/projects/FreeB
**
** Note that, as provided in the License, the Software is distributed on an
** "AS IS" basis, with ALL EXPRESS AND IMPLIED WARRANTIES AND CONDITIONS
** DISCLAIMED, INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTIES AND
** CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A
** PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
**
** NOTE:  The Original Code (as defined below) has been licensed to Sun
** Microsystems, Inc. ("Sun") under the SGI Free Software License B
** (Version 1.1), shown above ("SGI License").   Pursuant to Section
** 3.2(3) of the SGI License, Sun is distributing the Covered Code to
** you under an alternative license ("Alternative License").  This
** Alternative License includes all of the provisions of the SGI License
** except that Section 2.2 and 11 are omitted.  Any differences between
** the Alternative License and the SGI License are offered solely by Sun
** and not by SGI.
**
** Original Code. The Original Code is: OpenGL Sample Implementation,
** Version 1.2.1, released January 26, 2000, developed by Silicon Graphics,
** Inc. The Original Code is Copyright (c) 1991-2000 Silicon Graphics, Inc.
** Copyright in any portions created by third parties is as indicated
** elsewhere herein. All Rights Reserved.
**
** Additional Notice Provisions: The application programming interfaces
** established by SGI in conjunction with the Original Code are The
** OpenGL(R) Graphics System: A Specification (Version 1.2.1), released
** April 1, 1999; The OpenGL(R) Graphics System Utility Library (Version
** 1.3), released November 4, 1998; and OpenGL(R) Graphics with the X
** Window System(R) (Version 1.3), released October 19, 1998. This software
** was created using the OpenGL(R) version 1.2.1 Sample Implementation
** published by SGI, but has not been independently verified as being
** compliant with the OpenGL(R) version 1.2.1 Specification.
**
** $Date: 2009-03-13 22:20:29 -0700 (Fri, 13 Mar 2009) $ $Revision: 1867 $
** $Header$
*/

/*
 * Copyright (c) 2002-2004 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */
package jogamp.opengl.gl2;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL2;

import com.jogamp.common.nio.Buffers;

/**
 * Project.java
 * <p/>
 * <p/>
 * Created 11-jan-2004
 *
 * @author Erik Duijs
 * @author Kenneth Russell
 */
public class ProjectDouble {
  private static final double[] IDENTITY_MATRIX =
    new double[] {
      1.0, 0.0, 0.0, 0.0,
      0.0, 1.0, 0.0, 0.0,
      0.0, 0.0, 1.0, 0.0,
      0.0, 0.0, 0.0, 1.0 };

  // Note that we have cloned parts of the implementation in order to
  // support incoming Buffers. The reason for this is to avoid loading
  // non-direct buffer subclasses unnecessarily, because doing so can
  // cause performance decreases on direct buffer operations, at least
  // on the current HotSpot JVM. It would be nicer (and make the code
  // simpler) to simply have the array-based entry points delegate to
  // the versions taking Buffers by wrapping the arrays.

  // Array-based implementation
  private final double[] matrix = new double[16];

  private final double[][] tempMatrix = new double[4][4];
  private final double[] in = new double[4];
  private final double[] out = new double[4];

  // Buffer-based implementation
  private final DoubleBuffer matrixBuf;

  private final DoubleBuffer tempMatrixBuf;
  private final DoubleBuffer inBuf;
  private final DoubleBuffer outBuf;

  private final DoubleBuffer forwardBuf;
  private final DoubleBuffer sideBuf;
  private final DoubleBuffer upBuf;

  public ProjectDouble() {
    // Use direct buffers to avoid loading indirect buffer
    // implementations for applications trying to avoid doing so.
    // Slice up one big buffer because some NIO implementations
    // allocate a huge amount of memory to back even the smallest of
    // buffers.
    final DoubleBuffer locbuf = Buffers.newDirectDoubleBuffer(128);
    int pos = 0;
    int sz = 16;
    matrixBuf = slice(locbuf, pos, sz);
    pos += sz;
    tempMatrixBuf = slice(locbuf, pos, sz);
    pos += sz;
    sz = 4;
    inBuf = slice(locbuf, pos, sz);
    pos += sz;
    outBuf = slice(locbuf, pos, sz);
    pos += sz;
    sz = 3;
    forwardBuf = slice(locbuf, pos, sz);
    pos += sz;
    sideBuf = slice(locbuf, pos, sz);
    pos += sz;
    upBuf = slice(locbuf, pos, sz);
  }

  private static DoubleBuffer slice(final DoubleBuffer buf, final int pos, final int len) {
    buf.position(pos);
    buf.limit(pos + len);
    return buf.slice();
  }

  /**
   * Make matrix an identity matrix
   */
  private void __gluMakeIdentityd(final DoubleBuffer m) {
    final int oldPos = m.position();
    m.put(IDENTITY_MATRIX);
    m.position(oldPos);
  }

  /**
   * Make matrix an identity matrix
   */
  private void __gluMakeIdentityd(final double[] m) {
    for (int i = 0; i < 16; i++) {
      m[i] = IDENTITY_MATRIX[i];
    }
  }

  /**
   * Method __gluMultMatrixVecd
   *
   * @param matrix
   * @param in
   * @param out
   */
  private void __gluMultMatrixVecd(final double[] matrix, final int matrix_offset, final double[] in, final double[] out) {
    for (int i = 0; i < 4; i++) {
      out[i] =
        in[0] * matrix[0*4+i+matrix_offset] +
        in[1] * matrix[1*4+i+matrix_offset] +
        in[2] * matrix[2*4+i+matrix_offset] +
        in[3] * matrix[3*4+i+matrix_offset];
    }
  }

  /**
   * Method __gluMultMatrixVecd
   *
   * @param matrix
   * @param in
   * @param out
   */
  private void __gluMultMatrixVecd(final DoubleBuffer matrix, final DoubleBuffer in, final DoubleBuffer out) {
    final int inPos = in.position();
    final int outPos = out.position();
    final int matrixPos = matrix.position();
    for (int i = 0; i < 4; i++) {
      out.put(i + outPos,
              in.get(0+inPos) * matrix.get(0*4+i+matrixPos) +
              in.get(1+inPos) * matrix.get(1*4+i+matrixPos) +
              in.get(2+inPos) * matrix.get(2*4+i+matrixPos) +
              in.get(3+inPos) * matrix.get(3*4+i+matrixPos));
    }
  }

  /**
   * @param src
   * @param inverse
   *
   * @return
   */
  private boolean __gluInvertMatrixd(final double[] src, final double[] inverse) {
    int i, j, k, swap;
    double t;
    final double[][] temp = tempMatrix;

    for (i = 0; i < 4; i++) {
      for (j = 0; j < 4; j++) {
        temp[i][j] = src[i*4+j];
      }
    }
    __gluMakeIdentityd(inverse);

    for (i = 0; i < 4; i++) {
      //
      // Look for largest element in column
      //
      swap = i;
      for (j = i + 1; j < 4; j++) {
        if (Math.abs(temp[j][i]) > Math.abs(temp[i][i])) {
          swap = j;
        }
      }

      if (swap != i) {
        //
        // Swap rows.
        //
        for (k = 0; k < 4; k++) {
          t = temp[i][k];
          temp[i][k] = temp[swap][k];
          temp[swap][k] = t;

          t = inverse[i*4+k];
          inverse[i*4+k] = inverse[swap*4+k];
          inverse[swap*4+k] = t;
        }
      }

      if (temp[i][i] == 0) {
        //
        // No non-zero pivot. The matrix is singular, which shouldn't
        // happen. This means the user gave us a bad matrix.
        //
        return false;
      }

      t = temp[i][i];
      for (k = 0; k < 4; k++) {
        temp[i][k] /= t;
        inverse[i*4+k] /= t;
      }
      for (j = 0; j < 4; j++) {
        if (j != i) {
          t = temp[j][i];
          for (k = 0; k < 4; k++) {
            temp[j][k] -= temp[i][k] * t;
            inverse[j*4+k] -= inverse[i*4+k]*t;
          }
        }
      }
    }
    return true;
  }

  /**
   * @param src
   * @param inverse
   *
   * @return
   */
  private boolean __gluInvertMatrixd(final DoubleBuffer src, final DoubleBuffer inverse) {
    int i, j, k, swap;
    double t;

    final int srcPos = src.position();
    final int invPos = inverse.position();

    final DoubleBuffer temp = tempMatrixBuf;

    for (i = 0; i < 4; i++) {
      for (j = 0; j < 4; j++) {
        temp.put(i*4+j, src.get(i*4+j + srcPos));
      }
    }
    __gluMakeIdentityd(inverse);

    for (i = 0; i < 4; i++) {
      //
      // Look for largest element in column
      //
      swap = i;
      for (j = i + 1; j < 4; j++) {
        if (Math.abs(temp.get(j*4+i)) > Math.abs(temp.get(i*4+i))) {
          swap = j;
        }
      }

      if (swap != i) {
        //
        // Swap rows.
        //
        for (k = 0; k < 4; k++) {
          t = temp.get(i*4+k);
          temp.put(i*4+k, temp.get(swap*4+k));
          temp.put(swap*4+k, t);

          t = inverse.get(i*4+k + invPos);
          inverse.put(i*4+k + invPos, inverse.get(swap*4+k + invPos));
          inverse.put(swap*4+k + invPos, t);
        }
      }

      if (temp.get(i*4+i) == 0) {
        //
        // No non-zero pivot. The matrix is singular, which shouldn't
        // happen. This means the user gave us a bad matrix.
        //
        return false;
      }

      t = temp.get(i*4+i);
      for (k = 0; k < 4; k++) {
        temp.put(i*4+k, temp.get(i*4+k) / t);
        inverse.put(i*4+k + invPos, inverse.get(i*4+k + invPos) / t);
      }
      for (j = 0; j < 4; j++) {
        if (j != i) {
          t = temp.get(j*4+i);
          for (k = 0; k < 4; k++) {
            temp.put(j*4+k, temp.get(j*4+k) - temp.get(i*4+k) * t);
            inverse.put(j*4+k + invPos, inverse.get(j*4+k + invPos) - inverse.get(i*4+k + invPos) * t);
          }
        }
      }
    }
    return true;
  }


  /**
   * @param a
   * @param b
   * @param r
   */
  private void __gluMultMatricesd(final double[] a, final int a_offset, final double[] b, final int b_offset, final double[] r) {
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        r[i*4+j] =
          a[i*4+0+a_offset]*b[0*4+j+b_offset] +
          a[i*4+1+a_offset]*b[1*4+j+b_offset] +
          a[i*4+2+a_offset]*b[2*4+j+b_offset] +
          a[i*4+3+a_offset]*b[3*4+j+b_offset];
      }
    }
  }


  /**
   * @param a
   * @param b
   * @param r
   */
  private void __gluMultMatricesd(final DoubleBuffer a, final DoubleBuffer b, final DoubleBuffer r) {
    final int aPos = a.position();
    final int bPos = b.position();
    final int rPos = r.position();

    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        r.put(i*4+j + rPos,
          a.get(i*4+0+aPos)*b.get(0*4+j+bPos) +
          a.get(i*4+1+aPos)*b.get(1*4+j+bPos) +
          a.get(i*4+2+aPos)*b.get(2*4+j+bPos) +
          a.get(i*4+3+aPos)*b.get(3*4+j+bPos));
      }
    }
  }

  /**
   * Normalize vector
   *
   * @param v
   */
  private static void normalize(final DoubleBuffer v) {
    double r;

    final int vPos = v.position();

    r = Math.sqrt(v.get(0+vPos) * v.get(0+vPos) +
                  v.get(1+vPos) * v.get(1+vPos) +
                  v.get(2+vPos) * v.get(2+vPos));
    if ( r == 0.0 )
      return;

    r = 1.0 / r;

    v.put(0+vPos, v.get(0+vPos) * r);
    v.put(1+vPos, v.get(1+vPos) * r);
    v.put(2+vPos, v.get(2+vPos) * r);

    return;
  }


  /**
   * Calculate cross-product
   *
   * @param v1
   * @param v2
   * @param result
   */
  private static void cross(final DoubleBuffer v1, final DoubleBuffer v2, final DoubleBuffer result) {
    final int v1Pos = v1.position();
    final int v2Pos = v2.position();
    final int rPos  = result.position();

    result.put(0+rPos, v1.get(1+v1Pos) * v2.get(2+v2Pos) - v1.get(2+v1Pos) * v2.get(1+v2Pos));
    result.put(1+rPos, v1.get(2+v1Pos) * v2.get(0+v2Pos) - v1.get(0+v1Pos) * v2.get(2+v2Pos));
    result.put(2+rPos, v1.get(0+v1Pos) * v2.get(1+v2Pos) - v1.get(1+v1Pos) * v2.get(0+v2Pos));
  }

  /**
   * Method gluOrtho2D.
   *
   * @param left
   * @param right
   * @param bottom
   * @param top
   */
  public void gluOrtho2D(final GL2 gl, final double left, final double right, final double bottom, final double top) {
    gl.glOrtho(left, right, bottom, top, -1, 1);
  }

  /**
   * Method gluPerspective.
   *
   * @param fovy
   * @param aspect
   * @param zNear
   * @param zFar
   */
  public void gluPerspective(final GL2 gl, final double fovy, final double aspect, final double zNear, final double zFar) {
    double sine, cotangent, deltaZ;
    final double radians = fovy / 2 * Math.PI / 180;

    deltaZ = zFar - zNear;
    sine = Math.sin(radians);

    if ((deltaZ == 0) || (sine == 0) || (aspect == 0)) {
      return;
    }

    cotangent = Math.cos(radians) / sine;

    __gluMakeIdentityd(matrixBuf);

    matrixBuf.put(0 * 4 + 0, cotangent / aspect);
    matrixBuf.put(1 * 4 + 1, cotangent);
    matrixBuf.put(2 * 4 + 2, - (zFar + zNear) / deltaZ);
    matrixBuf.put(2 * 4 + 3, -1);
    matrixBuf.put(3 * 4 + 2, -2 * zNear * zFar / deltaZ);
    matrixBuf.put(3 * 4 + 3, 0);

    gl.glMultMatrixd(matrixBuf);
  }

  /**
   * Method gluLookAt
   *
   * @param eyex
   * @param eyey
   * @param eyez
   * @param centerx
   * @param centery
   * @param centerz
   * @param upx
   * @param upy
   * @param upz
   */
  public void gluLookAt(final GL2 gl,
                        final double eyex,
                        final double eyey,
                        final double eyez,
                        final double centerx,
                        final double centery,
                        final double centerz,
                        final double upx,
                        final double upy,
                        final double upz) {
    final DoubleBuffer forward = this.forwardBuf;
    final DoubleBuffer side = this.sideBuf;
    final DoubleBuffer up = this.upBuf;

    forward.put(0, centerx - eyex);
    forward.put(1, centery - eyey);
    forward.put(2, centerz - eyez);

    up.put(0, upx);
    up.put(1, upy);
    up.put(2, upz);

    normalize(forward);

    /* Side = forward x up */
    cross(forward, up, side);
    normalize(side);

    /* Recompute up as: up = side x forward */
    cross(side, forward, up);

    __gluMakeIdentityd(matrixBuf);
    matrixBuf.put(0 * 4 + 0, side.get(0));
    matrixBuf.put(1 * 4 + 0, side.get(1));
    matrixBuf.put(2 * 4 + 0, side.get(2));

    matrixBuf.put(0 * 4 + 1, up.get(0));
    matrixBuf.put(1 * 4 + 1, up.get(1));
    matrixBuf.put(2 * 4 + 1, up.get(2));

    matrixBuf.put(0 * 4 + 2, -forward.get(0));
    matrixBuf.put(1 * 4 + 2, -forward.get(1));
    matrixBuf.put(2 * 4 + 2, -forward.get(2));

    gl.glMultMatrixd(matrixBuf);
    gl.glTranslated(-eyex, -eyey, -eyez);
  }

  /**
   * Method gluProject
   *
   * @param objx
   * @param objy
   * @param objz
   * @param modelMatrix
   * @param projMatrix
   * @param viewport
   * @param win_pos
   *
   * @return
   */
  public boolean gluProject(final double objx,
                            final double objy,
                            final double objz,
                            final double[] modelMatrix,
                            final int modelMatrix_offset,
                            final double[] projMatrix,
                            final int projMatrix_offset,
                            final int[] viewport,
                            final int viewport_offset,
                            final double[] win_pos,
                            final int win_pos_offset ) {

    final double[] in = this.in;
    final double[] out = this.out;

    in[0] = objx;
    in[1] = objy;
    in[2] = objz;
    in[3] = 1.0;

    __gluMultMatrixVecd(modelMatrix, modelMatrix_offset, in, out);
    __gluMultMatrixVecd(projMatrix, projMatrix_offset, out, in);

    if (in[3] == 0.0)
      return false;

    in[3] = (1.0 / in[3]) * 0.5;

    // Map x, y and z to range 0-1
    in[0] = in[0] * in[3] + 0.5f;
    in[1] = in[1] * in[3] + 0.5f;
    in[2] = in[2] * in[3] + 0.5f;

    // Map x,y to viewport
    win_pos[0+win_pos_offset] = in[0] * viewport[2+viewport_offset] + viewport[0+viewport_offset];
    win_pos[1+win_pos_offset] = in[1] * viewport[3+viewport_offset] + viewport[1+viewport_offset];
    win_pos[2+win_pos_offset] = in[2];

    return true;
  }

  /**
   * Method gluProject
   *
   * @param objx
   * @param objy
   * @param objz
   * @param modelMatrix
   * @param projMatrix
   * @param viewport
   * @param win_pos
   *
   * @return
   */
  public boolean gluProject(final double objx,
                            final double objy,
                            final double objz,
                            final DoubleBuffer modelMatrix,
                            final DoubleBuffer projMatrix,
                            final IntBuffer viewport,
                            final DoubleBuffer win_pos) {

    final DoubleBuffer in = this.inBuf;
    final DoubleBuffer out = this.outBuf;

    in.put(0, objx);
    in.put(1, objy);
    in.put(2, objz);
    in.put(3, 1.0);

    __gluMultMatrixVecd(modelMatrix, in, out);
    __gluMultMatrixVecd(projMatrix, out, in);

    if (in.get(3) == 0.0)
      return false;

    in.put(3, (1.0 / in.get(3)) * 0.5);

    // Map x, y and z to range 0-1
    in.put(0, in.get(0) * in.get(3) + 0.5f);
    in.put(1, in.get(1) * in.get(3) + 0.5f);
    in.put(2, in.get(2) * in.get(3) + 0.5f);

    // Map x,y to viewport
    final int vPos = viewport.position();
    final int wPos = win_pos.position();
    win_pos.put(0+wPos, in.get(0) * viewport.get(2+vPos) + viewport.get(0+vPos));
    win_pos.put(1+wPos, in.get(1) * viewport.get(3+vPos) + viewport.get(1+vPos));
    win_pos.put(2+wPos, in.get(2));

    return true;
  }


  /**
   * Method gluUnproject
   *
   * @param winx
   * @param winy
   * @param winz
   * @param modelMatrix
   * @param projMatrix
   * @param viewport
   * @param obj_pos
   *
   * @return
   */
  public boolean gluUnProject(final double winx,
                              final double winy,
                              final double winz,
                              final double[] modelMatrix,
                              final int modelMatrix_offset,
                              final double[] projMatrix,
                              final int projMatrix_offset,
                              final int[] viewport,
                              final int viewport_offset,
                              final double[] obj_pos,
                              final int obj_pos_offset) {
    final double[] in = this.in;
    final double[] out = this.out;

    __gluMultMatricesd(modelMatrix, modelMatrix_offset, projMatrix, projMatrix_offset, matrix);

    if (!__gluInvertMatrixd(matrix, matrix))
      return false;

    in[0] = winx;
    in[1] = winy;
    in[2] = winz;
    in[3] = 1.0;

    // Map x and y from window coordinates
    in[0] = (in[0] - viewport[0+viewport_offset]) / viewport[2+viewport_offset];
    in[1] = (in[1] - viewport[1+viewport_offset]) / viewport[3+viewport_offset];

    // Map to range -1 to 1
    in[0] = in[0] * 2 - 1;
    in[1] = in[1] * 2 - 1;
    in[2] = in[2] * 2 - 1;

    __gluMultMatrixVecd(matrix, 0, in, out);

    if (out[3] == 0.0)
      return false;

    out[3] = 1.0 / out[3];

    obj_pos[0+obj_pos_offset] = out[0] * out[3];
    obj_pos[1+obj_pos_offset] = out[1] * out[3];
    obj_pos[2+obj_pos_offset] = out[2] * out[3];

    return true;
  }


  /**
   * Method gluUnproject
   *
   * @param winx
   * @param winy
   * @param winz
   * @param modelMatrix
   * @param projMatrix
   * @param viewport
   * @param obj_pos
   *
   * @return
   */
  public boolean gluUnProject(final double winx,
                              final double winy,
                              final double winz,
                              final DoubleBuffer modelMatrix,
                              final DoubleBuffer projMatrix,
                              final IntBuffer viewport,
                              final DoubleBuffer obj_pos) {
    final DoubleBuffer in = this.inBuf;
    final DoubleBuffer out = this.outBuf;

    __gluMultMatricesd(modelMatrix, projMatrix, matrixBuf);

    if (!__gluInvertMatrixd(matrixBuf, matrixBuf))
      return false;

    in.put(0, winx);
    in.put(1, winy);
    in.put(2, winz);
    in.put(3, 1.0);

    // Map x and y from window coordinates
    final int vPos = viewport.position();
    final int oPos = obj_pos.position();
    in.put(0, (in.get(0) - viewport.get(0+vPos)) / viewport.get(2+vPos));
    in.put(1, (in.get(1) - viewport.get(1+vPos)) / viewport.get(3+vPos));

    // Map to range -1 to 1
    in.put(0, in.get(0) * 2 - 1);
    in.put(1, in.get(1) * 2 - 1);
    in.put(2, in.get(2) * 2 - 1);

    __gluMultMatrixVecd(matrixBuf, in, out);

    if (out.get(3) == 0.0)
      return false;

    out.put(3, 1.0 / out.get(3));

    obj_pos.put(0+oPos, out.get(0) * out.get(3));
    obj_pos.put(1+oPos, out.get(1) * out.get(3));
    obj_pos.put(2+oPos, out.get(2) * out.get(3));

    return true;
  }


  /**
   * Method gluUnproject4
   *
   * @param winx
   * @param winy
   * @param winz
   * @param clipw
   * @param modelMatrix
   * @param projMatrix
   * @param viewport
   * @param near
   * @param far
   * @param obj_pos
   *
   * @return
   */
  public boolean gluUnProject4(final double winx,
                               final double winy,
                               final double winz,
                               final double clipw,
                               final double[] modelMatrix,
                               final int modelMatrix_offset,
                               final double[] projMatrix,
                               final int projMatrix_offset,
                               final int[] viewport,
                               final int viewport_offset,
                               final double near,
                               final double far,
                               final double[] obj_pos,
                               final int obj_pos_offset ) {
    final double[] in = this.in;
    final double[] out = this.out;

    __gluMultMatricesd(modelMatrix, modelMatrix_offset, projMatrix, projMatrix_offset, matrix);

    if (!__gluInvertMatrixd(matrix, matrix))
      return false;

    in[0] = winx;
    in[1] = winy;
    in[2] = winz;
    in[3] = clipw;

    // Map x and y from window coordinates
    in[0] = (in[0] - viewport[0+viewport_offset]) / viewport[2+viewport_offset];
    in[1] = (in[1] - viewport[1+viewport_offset]) / viewport[3+viewport_offset];
    in[2] = (in[2] - near) / (far - near);

    // Map to range -1 to 1
    in[0] = in[0] * 2 - 1;
    in[1] = in[1] * 2 - 1;
    in[2] = in[2] * 2 - 1;

    __gluMultMatrixVecd(matrix, 0, in, out);

    if (out[3] == 0.0)
      return false;

    obj_pos[0+obj_pos_offset] = out[0];
    obj_pos[1+obj_pos_offset] = out[1];
    obj_pos[2+obj_pos_offset] = out[2];
    obj_pos[3+obj_pos_offset] = out[3];
    return true;
  }

  /**
   * Method gluUnproject4
   *
   * @param winx
   * @param winy
   * @param winz
   * @param clipw
   * @param modelMatrix
   * @param projMatrix
   * @param viewport
   * @param near
   * @param far
   * @param obj_pos
   *
   * @return
   */
  public boolean gluUnProject4(final double winx,
                               final double winy,
                               final double winz,
                               final double clipw,
                               final DoubleBuffer modelMatrix,
                               final DoubleBuffer projMatrix,
                               final IntBuffer viewport,
                               final double near,
                               final double far,
                               final DoubleBuffer obj_pos) {
    final DoubleBuffer in = this.inBuf;
    final DoubleBuffer out = this.outBuf;

    __gluMultMatricesd(modelMatrix, projMatrix, matrixBuf);

    if (!__gluInvertMatrixd(matrixBuf, matrixBuf))
      return false;

    in.put(0, winx);
    in.put(1, winy);
    in.put(2, winz);
    in.put(3, clipw);

    // Map x and y from window coordinates
    final int vPos = viewport.position();
    in.put(0, (in.get(0) - viewport.get(0+vPos)) / viewport.get(2+vPos));
    in.put(1, (in.get(1) - viewport.get(1+vPos)) / viewport.get(3+vPos));
    in.put(2, (in.get(2) - near) / (far - near));

    // Map to range -1 to 1
    in.put(0, in.get(0) * 2 - 1);
    in.put(1, in.get(1) * 2 - 1);
    in.put(2, in.get(2) * 2 - 1);

    __gluMultMatrixVecd(matrixBuf, in, out);

    if (out.get(3) == 0.0)
      return false;

    final int oPos = obj_pos.position();
    obj_pos.put(0+oPos, out.get(0));
    obj_pos.put(1+oPos, out.get(1));
    obj_pos.put(2+oPos, out.get(2));
    obj_pos.put(3+oPos, out.get(3));
    return true;
  }


  /**
   * Method gluPickMatrix
   *
   * @param x
   * @param y
   * @param deltaX
   * @param deltaY
   * @param viewport
   */
  public void gluPickMatrix(final GL2 gl,
                            final double x,
                            final double y,
                            final double deltaX,
                            final double deltaY,
                            final IntBuffer viewport) {
    if (deltaX <= 0 || deltaY <= 0) {
      return;
    }

    /* Translate and scale the picked region to the entire window */
    final int vPos = viewport.position();
    gl.glTranslated((viewport.get(2+vPos) - 2 * (x - viewport.get(0+vPos))) / deltaX,
                    (viewport.get(3+vPos) - 2 * (y - viewport.get(1+vPos))) / deltaY,
                    0);
    gl.glScaled(viewport.get(2) / deltaX, viewport.get(3) / deltaY, 1.0);
  }

  /**
   * Method gluPickMatrix
   *
   * @param x
   * @param y
   * @param deltaX
   * @param deltaY
   * @param viewport
   * @param viewport_offset
   */
  public void gluPickMatrix(final GL2 gl,
                            final double x,
                            final double y,
                            final double deltaX,
                            final double deltaY,
                            final int[] viewport,
                            final int viewport_offset) {
    if (deltaX <= 0 || deltaY <= 0) {
      return;
    }

    /* Translate and scale the picked region to the entire window */
    gl.glTranslated((viewport[2+viewport_offset] - 2 * (x - viewport[0+viewport_offset])) / deltaX,
                    (viewport[3+viewport_offset] - 2 * (y - viewport[1+viewport_offset])) / deltaY,
                    0);
    gl.glScaled(viewport[2+viewport_offset] / deltaX, viewport[3+viewport_offset] / deltaY, 1.0);
  }
}
