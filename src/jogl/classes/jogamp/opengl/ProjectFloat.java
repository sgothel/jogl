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
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */
package jogamp.opengl;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;

/**
 * ProjectFloat.java
 * <p>
 * Created 11-jan-2004
 * </p>
 *
 * @author Erik Duijs
 * @author Kenneth Russell
 * @author Sven Gothel
 */
public class ProjectFloat {
  public static final int getRequiredFloatBufferSize() { return 1*16; }

  // Note that we have cloned parts of the implementation in order to
  // support incoming Buffers. The reason for this is to avoid loading
  // non-direct buffer subclasses unnecessarily, because doing so can
  // cause performance decreases on direct buffer operations, at least
  // on the current HotSpot JVM. It would be nicer (and make the code
  // simpler) to simply have the array-based entry points delegate to
  // the versions taking Buffers by wrapping the arrays.

  // Array-based implementation
  private final float[] matrix = new float[16];
  private final float[][] tempInvertMatrix = new float[4][4];

  private final float[] in = new float[4];
  private final float[] out = new float[4];

  // Buffer-based implementation
  private FloatBuffer matrixBuf; // 4x4

  private final float[] forward = new float[3];  // 3
  private final float[] side    = new float[3];  // 3
  private final float[] up      = new float[3];  // 3

  public ProjectFloat() {
      this(true);
  }

  public ProjectFloat(boolean useBackingArray) {
      this(useBackingArray ? null : Buffers.newDirectByteBuffer(getRequiredFloatBufferSize() * Buffers.SIZEOF_FLOAT),
           useBackingArray ? new float[getRequiredFloatBufferSize()] : null,
           0);
  }

  /**
   * @param floatBuffer source buffer, may be ByteBuffer (recommended) or FloatBuffer or <code>null</code>.
   *                    If used, shall be &ge; {@link #getRequiredFloatBufferSize()} + floatOffset.
   *                    Buffer's position is ignored and floatPos is being used.
   * @param floatArray source float array or <code>null</code>.
   *                   If used, size shall be &ge; {@link #getRequiredFloatBufferSize()} + floatOffset.
   * @param floatOffset Offset for either of the given sources (buffer or array)
   */
  public ProjectFloat(Buffer floatBuffer, float[] floatArray, int floatOffset) {
    matrixBuf = Buffers.slice2Float(floatBuffer, floatArray, floatOffset, 16);
  }

  public void destroy() {
    matrixBuf = null;
  }

  /**
   * @param src
   * @param srcOffset
   * @param inverse
   * @param inverseOffset
   * @return
   */
  public boolean gluInvertMatrixf(float[] src, int srcOffset, float[] inverse, int inverseOffset) {
    int i, j, k, swap;
    float t;
    final float[][] temp = tempInvertMatrix;

    for (i = 0; i < 4; i++) {
      for (j = 0; j < 4; j++) {
        temp[i][j] = src[i*4+j+srcOffset];
      }
    }
    FloatUtil.makeIdentityf(inverse, inverseOffset);

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

          t = inverse[i*4+k+inverseOffset];
          inverse[i*4+k+inverseOffset] = inverse[swap*4+k+inverseOffset];
          inverse[swap*4+k+inverseOffset] = t;
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
        inverse[i*4+k+inverseOffset] /= t;
      }
      for (j = 0; j < 4; j++) {
        if (j != i) {
          t = temp[j][i];
          for (k = 0; k < 4; k++) {
            temp[j][k] -= temp[i][k] * t;
            inverse[j*4+k+inverseOffset] -= inverse[i*4+k+inverseOffset]*t;
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
  public boolean gluInvertMatrixf(FloatBuffer src, FloatBuffer inverse) {
    int i, j, k, swap;
    float t;

    final int srcPos = src.position();
    final int invPos = inverse.position();

    final float[][] temp = tempInvertMatrix;

    for (i = 0; i < 4; i++) {
      for (j = 0; j < 4; j++) {
        temp[i][j] = src.get(i*4+j + srcPos);
      }
    }
    FloatUtil.makeIdentityf(inverse);

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

          t = inverse.get(i*4+k + invPos);
          inverse.put(i*4+k + invPos, inverse.get(swap*4+k + invPos));
          inverse.put(swap*4+k + invPos, t);
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
        final int z = i*4+k + invPos;
        inverse.put(z, inverse.get(z) / t);
      }
      for (j = 0; j < 4; j++) {
        if (j != i) {
          t = temp[j][i];
          for (k = 0; k < 4; k++) {
            temp[j][k] -= temp[i][k] * t;
            final int z = j*4+k + invPos;
            inverse.put(z, inverse.get(z) - inverse.get(i*4+k + invPos) * t);
          }
        }
      }
    }
    return true;
  }


  /**
   * Method gluOrtho2D.
   *
   * @param left
   * @param right
   * @param bottom
   * @param top
   */
  public void gluOrtho2D(GLMatrixFunc gl, float left, float right, float bottom, float top) {
    gl.glOrthof(left, right, bottom, top, -1, 1);
  }

  /**
   * Method gluPerspective.
   *
   * @param fovy
   * @param aspect
   * @param zNear
   * @param zFar
   */
  public void gluPerspective(GLMatrixFunc gl, float fovy, float aspect, float zNear, float zFar) {
    final float radians = fovy / 2 * (float) Math.PI / 180;
    float sine, cotangent, deltaZ;

    deltaZ = zFar - zNear;
    sine = (float) Math.sin(radians);

    if ((deltaZ == 0) || (sine == 0) || (aspect == 0)) {
      return;
    }

    cotangent = (float) Math.cos(radians) / sine;

    FloatUtil.makeIdentityf(matrixBuf);
    final int mPos = matrixBuf.position();
    matrixBuf.put(0 * 4 + 0 + mPos, cotangent / aspect);
    matrixBuf.put(1 * 4 + 1 + mPos, cotangent);
    matrixBuf.put(2 * 4 + 2 + mPos, - (zFar + zNear) / deltaZ);
    matrixBuf.put(2 * 4 + 3 + mPos, -1);
    matrixBuf.put(3 * 4 + 2 + mPos, -2 * zNear * zFar / deltaZ);
    matrixBuf.put(3 * 4 + 3 + mPos, 0);

    gl.glMultMatrixf(matrixBuf);
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
  public void gluLookAt(GLMatrixFunc gl,
                        float eyex, float eyey, float eyez,
                        float centerx, float centery, float centerz,
                        float upx, float upy, float upz) {
    final float[] forward = this.forward;
    final float[] side = this.side;
    final float[] up = this.up;

    forward[0] = centerx - eyex;
    forward[1] = centery - eyey;
    forward[2] = centerz - eyez;

    up[0] = upx;
    up[1] = upy;
    up[2] = upz;

    VectorUtil.normalizeVec3(forward);

    /* Side = forward x up */
    VectorUtil.crossVec3(side, forward, up);
    VectorUtil.normalizeVec3(side);

    /* Recompute up as: up = side x forward */
    VectorUtil.crossVec3(up, side, forward);

    FloatUtil.makeIdentityf(matrixBuf);
    final int mPos = matrixBuf.position();
    matrixBuf.put(0 * 4 + 0 + mPos, side[0]);
    matrixBuf.put(1 * 4 + 0 + mPos, side[1]);
    matrixBuf.put(2 * 4 + 0 + mPos, side[2]);

    matrixBuf.put(0 * 4 + 1 + mPos, up[0]);
    matrixBuf.put(1 * 4 + 1 + mPos, up[1]);
    matrixBuf.put(2 * 4 + 1 + mPos, up[2]);

    matrixBuf.put(0 * 4 + 2 + mPos, -forward[0]);
    matrixBuf.put(1 * 4 + 2 + mPos, -forward[1]);
    matrixBuf.put(2 * 4 + 2 + mPos, -forward[2]);

    gl.glMultMatrixf(matrixBuf);
    gl.glTranslatef(-eyex, -eyey, -eyez);
  }

  /**
   * Map object coordinates to window coordinates.
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
  public boolean gluProject(float objx, float objy, float objz,
                            float[] modelMatrix, int modelMatrix_offset,
                            float[] projMatrix, int projMatrix_offset,
                            int[] viewport, int viewport_offset,
                            float[] win_pos, int win_pos_offset ) {

    final float[] in = this.in;
    final float[] out = this.out;

    in[0] = objx;
    in[1] = objy;
    in[2] = objz;
    in[3] = 1.0f;

    FloatUtil.multMatrixVecf(modelMatrix, modelMatrix_offset, in, 0, out, 0);
    FloatUtil.multMatrixVecf(projMatrix, projMatrix_offset, out, 0, in, 0);

    if (in[3] == 0.0f) {
      return false;
    }

    in[3] = (1.0f / in[3]) * 0.5f;

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
   * Map object coordinates to window coordinates.
   */
  public boolean gluProject(float objx, float objy, float objz,
                            FloatBuffer modelMatrix,
                            FloatBuffer projMatrix,
                            int[] viewport, int viewport_offset,
                            float[] win_pos, int win_pos_offset ) {

    final float[] in = this.in;
    final float[] out = this.out;

    in[0] = objx;
    in[1] = objy;
    in[2] = objz;
    in[3] = 1.0f;

    FloatUtil.multMatrixVecf(modelMatrix, in, out);
    FloatUtil.multMatrixVecf(projMatrix, out, in);

    if (in[3] == 0.0f) {
      return false;
    }

    in[3] = (1.0f / in[3]) * 0.5f;

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
   * Map object coordinates to window coordinates.
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
  public boolean gluProject(float objx, float objy, float objz,
                            FloatBuffer modelMatrix,
                            FloatBuffer projMatrix,
                            IntBuffer viewport,
                            FloatBuffer win_pos) {

    final float[] in = this.in;
    final float[] out = this.out;

    in[0] = objx;
    in[1] = objy;
    in[2] = objz;
    in[3] = 1.0f;

    FloatUtil.multMatrixVecf(modelMatrix, in, out);
    FloatUtil.multMatrixVecf(projMatrix, out, in);

    if (in[3] == 0.0f) {
      return false;
    }

    in[3] = (1.0f / in[3]) * 0.5f;

    // Map x, y and z to range 0-1
    in[0] = in[0] * in[3] + 0.5f;
    in[1] = in[1] * in[3] + 0.5f;
    in[2] = in[2] * in[3] + 0.5f;

    // Map x,y to viewport
    final int vPos = viewport.position();
    final int wPos = win_pos.position();
    win_pos.put(0+wPos, in[0] * viewport.get(2+vPos) + viewport.get(0+vPos));
    win_pos.put(1+wPos, in[1] * viewport.get(3+vPos) + viewport.get(1+vPos));
    win_pos.put(2+wPos, in[2]);

    return true;
  }


  /**
   * Map window coordinates to object coordinates.
   *
   * @param winx
   * @param winy
   * @param winz
   * @param modelMatrix
   * @param projMatrix
   * @param viewport
   * @param obj_pos
   *
   * @return true if successful, otherwise false (failed to invert matrix, or becomes z is infinity)
   */
  public boolean gluUnProject(float winx, float winy, float winz,
                              float[] modelMatrix, int modelMatrix_offset,
                              float[] projMatrix, int projMatrix_offset,
                              int[] viewport, int viewport_offset,
                              float[] obj_pos, int obj_pos_offset) {
    final float[] in = this.in;
    final float[] out = this.out;

    FloatUtil.multMatrixf(projMatrix, projMatrix_offset, modelMatrix, modelMatrix_offset, matrix, 0);

    if (!gluInvertMatrixf(matrix, 0, matrix, 0)) {
      return false;
    }

    in[0] = winx;
    in[1] = winy;
    in[2] = winz;
    in[3] = 1.0f;

    // Map x and y from window coordinates
    in[0] = (in[0] - viewport[0+viewport_offset]) / viewport[2+viewport_offset];
    in[1] = (in[1] - viewport[1+viewport_offset]) / viewport[3+viewport_offset];

    // Map to range -1 to 1
    in[0] = in[0] * 2 - 1;
    in[1] = in[1] * 2 - 1;
    in[2] = in[2] * 2 - 1;

    FloatUtil.multMatrixVecf(matrix, in, out);

    if (out[3] == 0.0) {
      return false;
    }

    out[3] = 1.0f / out[3];

    obj_pos[0+obj_pos_offset] = out[0] * out[3];
    obj_pos[1+obj_pos_offset] = out[1] * out[3];
    obj_pos[2+obj_pos_offset] = out[2] * out[3];

    return true;
  }


  /**
   * Map window coordinates to object coordinates.
   *
   * @param winx
   * @param winy
   * @param winz
   * @param modelMatrix
   * @param projMatrix
   * @param viewport
   * @param viewport_offset
   * @param obj_pos
   * @param obj_pos_offset
   * @return true if successful, otherwise false (failed to invert matrix, or becomes z is infinity)
   */
  public boolean gluUnProject(float winx, float winy, float winz,
                              FloatBuffer modelMatrix,
                              FloatBuffer projMatrix,
                              int[] viewport, int viewport_offset,
                              float[] obj_pos, int obj_pos_offset) {
    final float[] in = this.in;
    final float[] out = this.out;

    FloatUtil.multMatrixf(projMatrix, modelMatrix, matrixBuf);

    if (!gluInvertMatrixf(matrixBuf, matrixBuf)) {
      return false;
    }

    in[0] = winx;
    in[1] = winy;
    in[2] = winz;
    in[3] = 1.0f;

    // Map x and y from window coordinates
    in[0] = (in[0] - viewport[0+viewport_offset]) / viewport[2+viewport_offset];
    in[1] = (in[1] - viewport[1+viewport_offset]) / viewport[3+viewport_offset];

    // Map to range -1 to 1
    in[0] = in[0] * 2 - 1;
    in[1] = in[1] * 2 - 1;
    in[2] = in[2] * 2 - 1;

    FloatUtil.multMatrixVecf(matrixBuf, in, out);

    if (out[3] == 0.0) {
      return false;
    }

    out[3] = 1.0f / out[3];

    obj_pos[0+obj_pos_offset] = out[0] * out[3];
    obj_pos[1+obj_pos_offset] = out[1] * out[3];
    obj_pos[2+obj_pos_offset] = out[2] * out[3];

    return true;
  }

  /**
   * Map window coordinates to object coordinates.
   *
   * @param winx
   * @param winy
   * @param winz
   * @param modelMatrix
   * @param projMatrix
   * @param viewport
   * @param obj_pos
   *
   * @return true if successful, otherwise false (failed to invert matrix, or becomes z is infinity)
   */
  public boolean gluUnProject(float winx, float winy, float winz,
                              FloatBuffer modelMatrix,
                              FloatBuffer projMatrix,
                              IntBuffer viewport,
                              FloatBuffer obj_pos) {
    final float[] in = this.in;
    final float[] out = this.out;

    FloatUtil.multMatrixf(projMatrix, modelMatrix, matrixBuf);

    if (!gluInvertMatrixf(matrixBuf, matrixBuf)) {
      return false;
    }

    in[0] = winx;
    in[1] = winy;
    in[2] = winz;
    in[3] = 1.0f;

    // Map x and y from window coordinates
    final int vPos = viewport.position();
    final int oPos = obj_pos.position();
    in[0] = (in[0] - viewport.get(0+vPos)) / viewport.get(2+vPos);
    in[1] = (in[1] - viewport.get(1+vPos)) / viewport.get(3+vPos);

    // Map to range -1 to 1
    in[0] = in[0] * 2 - 1;
    in[1] = in[1] * 2 - 1;
    in[2] = in[2] * 2 - 1;

    FloatUtil.multMatrixVecf(matrixBuf, in, out);

    if (out[3] == 0.0) {
      return false;
    }

    out[3] = 1.0f / out[3];

    obj_pos.put(0+oPos, out[0] * out[3]);
    obj_pos.put(1+oPos, out[1] * out[3]);
    obj_pos.put(2+oPos, out[2] * out[3]);

    return true;
  }


  /**
   * Map window coordinates to object coordinates.
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
   * @return true if successful, otherwise false (failed to invert matrix, or becomes z is infinity)
   */
  public boolean gluUnProject4(float winx,
                               float winy,
                               float winz,
                               float clipw,
                               float[] modelMatrix,
                               int modelMatrix_offset,
                               float[] projMatrix,
                               int projMatrix_offset,
                               int[] viewport,
                               int viewport_offset,
                               float near,
                               float far,
                               float[] obj_pos,
                               int obj_pos_offset ) {
    final float[] in = this.in;
    final float[] out = this.out;

    FloatUtil.multMatrixf(projMatrix, projMatrix_offset, modelMatrix, modelMatrix_offset, matrix, 0);

    if (!gluInvertMatrixf(matrix, 0, matrix, 0))
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

    FloatUtil.multMatrixVecf(matrix, in, out);

    if (out[3] == 0.0f) {
      return false;
    }

    obj_pos[0+obj_pos_offset] = out[0];
    obj_pos[1+obj_pos_offset] = out[1];
    obj_pos[2+obj_pos_offset] = out[2];
    obj_pos[3+obj_pos_offset] = out[3];
    return true;
  }

  /**
   * Map window coordinates to object coordinates.
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
   * @return true if successful, otherwise false (failed to invert matrix, or becomes z is infinity)
   */
  public boolean gluUnProject4(float winx,
                               float winy,
                               float winz,
                               float clipw,
                               FloatBuffer modelMatrix,
                               FloatBuffer projMatrix,
                               IntBuffer viewport,
                               float near,
                               float far,
                               FloatBuffer obj_pos) {
    final float[] in = this.in;
    final float[] out = this.out;

    FloatUtil.multMatrixf(projMatrix, modelMatrix, matrixBuf);

    if (!gluInvertMatrixf(matrixBuf, matrixBuf))
      return false;

    in[0] = winx;
    in[1] = winy;
    in[2] = winz;
    in[3] = clipw;

    // Map x and y from window coordinates
    final int vPos = viewport.position();
    in[0] = (in[0] - viewport.get(0+vPos)) / viewport.get(2+vPos);
    in[1] = (in[1] - viewport.get(1+vPos)) / viewport.get(3+vPos);
    in[2] = (in[2] - near) / (far - near);

    // Map to range -1 to 1
    in[0] = in[0] * 2 - 1;
    in[1] = in[1] * 2 - 1;
    in[2] = in[2] * 2 - 1;

    FloatUtil.multMatrixVecf(matrixBuf, in, out);

    if (out[3] == 0.0f) {
      return false;
    }

    final int oPos = obj_pos.position();
    obj_pos.put(0+oPos, out[0]);
    obj_pos.put(1+oPos, out[1]);
    obj_pos.put(2+oPos, out[2]);
    obj_pos.put(3+oPos, out[3]);
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
  public void gluPickMatrix(GLMatrixFunc gl,
                            float x,
                            float y,
                            float deltaX,
                            float deltaY,
                            IntBuffer viewport) {
    if (deltaX <= 0 || deltaY <= 0) {
      return;
    }

    /* Translate and scale the picked region to the entire window */
    final int vPos = viewport.position();
    gl.glTranslatef((viewport.get(2+vPos) - 2 * (x - viewport.get(0+vPos))) / deltaX,
                    (viewport.get(3+vPos) - 2 * (y - viewport.get(1+vPos))) / deltaY,
                    0);
    gl.glScalef(viewport.get(2) / deltaX, viewport.get(3) / deltaY, 1.0f);
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
  public void gluPickMatrix(GLMatrixFunc gl,
                            float x,
                            float y,
                            float deltaX,
                            float deltaY,
                            int[] viewport,
                            int viewport_offset) {
    if (deltaX <= 0 || deltaY <= 0) {
      return;
    }

    /* Translate and scale the picked region to the entire window */
    gl.glTranslatef((viewport[2+viewport_offset] - 2 * (x - viewport[0+viewport_offset])) / deltaX,
                    (viewport[3+viewport_offset] - 2 * (y - viewport[1+viewport_offset])) / deltaY,
                    0);
    gl.glScalef(viewport[2+viewport_offset] / deltaX, viewport[3+viewport_offset] / deltaY, 1.0f);
  }

}
