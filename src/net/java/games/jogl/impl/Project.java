/*
** License Applicability. Except to the extent portions of this file are
** made subject to an alternative license as permitted in the SGI Free
** Software License B, Version 1.1 (the "License"), the contents of this
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
** $Date$ $Revision$
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
package net.java.games.jogl.impl;

import java.nio.*;

import net.java.games.jogl.*;
import net.java.games.jogl.util.*;

/**
 * Project.java
 * <p/>
 * <p/>
 * Created 11-jan-2004
 * 
 * @author Erik Duijs
 * @author Kenneth Russell
 */
class Project {
  private static final double[] IDENTITY_MATRIX =
    new double[] {
      1.0, 0.0, 0.0, 0.0,
      0.0, 1.0, 0.0, 0.0,
      0.0, 0.0, 1.0, 0.0,
      0.0, 0.0, 0.0, 1.0 };

  private final DoubleBuffer matrix = BufferUtils.newDoubleBuffer(16);
  private final double[] finalMatrix = new double[16];

  private final double[][] tempMatrix = new double[4][4];
  private final double[] in = new double[4];
  private final double[] out = new double[4];

  private final double[] forward = new double[3];
  private final double[] side = new double[3];
  private final double[] up = new double[3];

  /**
   * Make matrix an identity matrix
   */
  private void __gluMakeIdentityd(DoubleBuffer m) {
    int oldPos = m.position();
    m.put(IDENTITY_MATRIX);
    m.position(oldPos);
  }

  private void __gluMakeIdentityd(double[] m) {
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
  private void __gluMultMatrixVecd(double[] matrix, int matrix_offset, double[] in, double[] out) {
    for (int i = 0; i < 4; i++) {
      out[i] =
        in[0] * matrix[0*4+i+matrix_offset] +
        in[1] * matrix[1*4+i+matrix_offset] +
        in[2] * matrix[2*4+i+matrix_offset] +
        in[3] * matrix[3*4+i+matrix_offset];
    }
  }

  /**
   * @param src
   * @param inverse
   * 
   * @return
   */
  private boolean __gluInvertMatrixd(double[] src, double[] inverse) {
    int i, j, k, swap;
    double t;
    double[][] temp = tempMatrix;

    for (i = 0; i < 4; i++) {
      for (j = 0; j < 4; j++) {
        temp[i][j] = src[i*4+j];
      }
    }
    __gluMakeIdentityd(inverse);

    for (i = 0; i < 4; i++) {
      /*
       * * Look for largest element in column
       */
      swap = i;
      for (j = i + 1; j < 4; j++) {
        if (Math.abs(temp[j][i]) > Math.abs(temp[i][i])) {
          swap = j;
        }
      }

      if (swap != i) {
        /*
         * * Swap rows.
         */
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
        /*
         * No non-zero pivot. The matrix is singular, which shouldn't
         * happen. This means the user gave us a bad matrix.
         */
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
   * @param a
   * @param b
   * @param r
   */
  private void __gluMultMatricesd(double[] a, int a_offset, double[] b, int b_offset, double[] r) {
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
   * Normalize vector
   *
   * @param v
   */
  private static void normalize(double[] v) {
    double r;

    r = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    if ( r == 0.0 )
      return;

    r = 1.0 / r;

    v[0] *= r;
    v[1] *= r;
    v[2] *= r;

    return;
  }

  /**
   * Calculate cross-product
   *
   * @param v1
   * @param v2
   * @param result
   */
  private static void cross(double[] v1, double[] v2, double[] result) {
    result[0] = v1[1] * v2[2] - v1[2] * v2[1];
    result[1] = v1[2] * v2[0] - v1[0] * v2[2];
    result[2] = v1[0] * v2[1] - v1[1] * v2[0];
  }

  /**
   * Method gluOrtho2D.
   * 
   * @param left
   * @param right
   * @param bottom
   * @param top
   */
  public void gluOrtho2D(GL gl, double left, double right, double bottom, double top) {
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
  public void gluPerspective(GL gl, double fovy, double aspect, double zNear, double zFar) {
    double sine, cotangent, deltaZ;
    double radians = fovy / 2 * Math.PI / 180;

    deltaZ = zFar - zNear;
    sine = Math.sin(radians);

    if ((deltaZ == 0) || (sine == 0) || (aspect == 0)) {
      return;
    }

    cotangent = Math.cos(radians) / sine;

    __gluMakeIdentityd(matrix);

    matrix.put(0 * 4 + 0, cotangent / aspect);
    matrix.put(1 * 4 + 1, cotangent);
    matrix.put(2 * 4 + 2, - (zFar + zNear) / deltaZ);
    matrix.put(2 * 4 + 3, -1);
    matrix.put(3 * 4 + 2, -2 * zNear * zFar / deltaZ);
    matrix.put(3 * 4 + 3, 0);

    gl.glMultMatrixd(matrix);
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
  public void gluLookAt(GL gl,
                        double eyex,
                        double eyey,
                        double eyez,
                        double centerx,
                        double centery,
                        double centerz,
                        double upx,
                        double upy,
                        double upz) {
    double[] forward = this.forward;
    double[] side = this.side;
    double[] up = this.up;

    forward[0] = centerx - eyex;
    forward[1] = centery - eyey;
    forward[2] = centerz - eyez;

    up[0] = upx;
    up[1] = upy;
    up[2] = upz;

    normalize(forward);

    /* Side = forward x up */
    cross(forward, up, side);
    normalize(side);

    /* Recompute up as: up = side x forward */
    cross(side, forward, up);

    __gluMakeIdentityd(matrix);
    matrix.put(0 * 4 + 0, side[0]);
    matrix.put(1 * 4 + 0, side[1]);
    matrix.put(2 * 4 + 0, side[2]);

    matrix.put(0 * 4 + 1, up[0]);
    matrix.put(1 * 4 + 1, up[1]);
    matrix.put(2 * 4 + 1, up[2]);

    matrix.put(0 * 4 + 2, -forward[0]);
    matrix.put(1 * 4 + 2, -forward[1]);
    matrix.put(2 * 4 + 2, -forward[2]);

    gl.glMultMatrixd(matrix);
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
  public boolean gluProject(double objx,
                            double objy,
                            double objz,
                            double[] modelMatrix,
                            int modelMatrix_offset,
                            double[] projMatrix,
                            int projMatrix_offset,
                            int[] viewport,
                            int viewport_offset,
                            double[] win_pos,
                            int win_pos_offset ) {

    double[] in = this.in;
    double[] out = this.out;

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
  public boolean gluUnProject(double winx,
                              double winy,
                              double winz,
                              double[] modelMatrix,
                              int modelMatrix_offset,
                              double[] projMatrix,
                              int projMatrix_offset,
                              int[] viewport,
                              int viewport_offset,
                              double[] obj_pos,
                              int obj_pos_offset) {
    double[] in = this.in;
    double[] out = this.out;

    __gluMultMatricesd(modelMatrix, modelMatrix_offset, projMatrix, projMatrix_offset, finalMatrix);

    if (!__gluInvertMatrixd(finalMatrix, finalMatrix))
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

    __gluMultMatrixVecd(finalMatrix, 0, in, out);

    if (out[3] == 0.0)
      return false;

    out[3] = 1.0 / out[3];

    obj_pos[0+obj_pos_offset] = out[0] * out[3];
    obj_pos[1+obj_pos_offset] = out[1] * out[3];
    obj_pos[2+obj_pos_offset] = out[2] * out[3];

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
  public boolean gluUnProject4(double winx,
                               double winy,
                               double winz,
                               double clipw,
                               double[] modelMatrix,
                               int modelMatrix_offset,
                               double[] projMatrix,
                               int projMatrix_offset,
                               int[] viewport,
                               int viewport_offset,
                               double near,
                               double far,
                               double[] obj_pos,
                               int obj_pos_offset ) {
    double[] in = this.in;
    double[] out = this.out;

    __gluMultMatricesd(modelMatrix, modelMatrix_offset, projMatrix, projMatrix_offset, finalMatrix);

    if (!__gluInvertMatrixd(finalMatrix, finalMatrix))
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

    __gluMultMatrixVecd(finalMatrix, 0, in, out);

    if (out[3] == 0.0)
      return false;

    obj_pos[0+obj_pos_offset] = out[0];
    obj_pos[1+obj_pos_offset] = out[1];
    obj_pos[2+obj_pos_offset] = out[2];
    obj_pos[3+obj_pos_offset] = out[3];
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
  public void gluPickMatrix(GL gl,
                            double x,
                            double y,
                            double deltaX,
                            double deltaY,
                            IntBuffer viewport) {
    if (deltaX <= 0 || deltaY <= 0) {
      return;
    }

    /* Translate and scale the picked region to the entire window */
    gl.glTranslated((viewport.get(2) - 2 * (x - viewport.get(0))) / deltaX,
                    (viewport.get(3) - 2 * (y - viewport.get(1))) / deltaY,
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
  public void gluPickMatrix(GL gl,
                            double x,
                            double y,
                            double deltaX,
                            double deltaY,
                            int[] viewport,
                            int viewport_offset) {
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
