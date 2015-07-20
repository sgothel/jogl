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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GLException;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.opengl.math.FloatUtil;

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

  private final float[] mat4Tmp1 = new float[16];
  private final float[] mat4Tmp2 = new float[16];
  private final float[] mat4Tmp3 = new float[16];

  public ProjectFloat() {
  }

  /**
   * Method gluOrtho2D.
   *
   * @param left
   * @param right
   * @param bottom
   * @param top
   */
  public void gluOrtho2D(final GLMatrixFunc gl, final float left, final float right, final float bottom, final float top) {
    gl.glOrthof(left, right, bottom, top, -1, 1);
  }

  /**
   * Method gluPerspective.
   *
   * @param fovy_deg fov angle in degrees
   * @param aspect
   * @param zNear
   * @param zFar
   * @throws GLException if {@code zNear <= 0} or {@code zFar <= zNear}
   * @see FloatUtil#makePerspective(float[], int, boolean, float, float, float, float)
   */
  public void gluPerspective(final GLMatrixFunc gl, final float fovy_deg, final float aspect, final float zNear, final float zFar) throws GLException {
    gl.glMultMatrixf(FloatUtil.makePerspective(mat4Tmp1, 0, true, fovy_deg * FloatUtil.PI / 180.0f, aspect, zNear, zFar), 0);
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
  public void gluLookAt(final GLMatrixFunc gl,
                        final float eyex, final float eyey, final float eyez,
                        final float centerx, final float centery, final float centerz,
                        final float upx, final float upy, final float upz) {
    mat4Tmp2[0+0] = eyex;
    mat4Tmp2[1+0] = eyey;
    mat4Tmp2[2+0] = eyez;
    mat4Tmp2[0+4] = centerx;
    mat4Tmp2[1+4] = centery;
    mat4Tmp2[2+4] = centerz;
    mat4Tmp2[0+8] = upx;
    mat4Tmp2[1+8] = upy;
    mat4Tmp2[2+8] = upz;
    gl.glMultMatrixf(
            FloatUtil.makeLookAt(mat4Tmp1, 0, mat4Tmp2 /* eye */, 0, mat4Tmp2 /* center */, 4, mat4Tmp2 /* up */, 8, mat4Tmp3), 0);
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
   * @param viewport 4 component viewport vector
   * @param viewport_offset
   * @param win_pos 3 component window coordinate, the result
   * @param win_pos_offset
   * @return true if successful, otherwise false (z is 1)
   */
  public boolean gluProject(final float objx, final float objy, final float objz,
                            final float[] modelMatrix, final int modelMatrix_offset,
                            final float[] projMatrix, final int projMatrix_offset,
                            final int[] viewport, final int viewport_offset,
                            final float[] win_pos, final int win_pos_offset ) {
    return FloatUtil.mapObjToWinCoords(objx, objy, objz,
                      modelMatrix, modelMatrix_offset,
                      projMatrix, projMatrix_offset,
                      viewport, viewport_offset,
                      win_pos, win_pos_offset,
                      mat4Tmp1, mat4Tmp2);
  }

  /**
   * Map object coordinates to window coordinates.
   */
  @SuppressWarnings("deprecation")
  public boolean gluProject(final float objx, final float objy, final float objz,
                            final FloatBuffer modelMatrix,
                            final FloatBuffer projMatrix,
                            final int[] viewport, final int viewport_offset,
                            final float[] win_pos, final int win_pos_offset ) {
    final float[] in = this.mat4Tmp1;
    final float[] out = this.mat4Tmp2;

    in[0] = objx;
    in[1] = objy;
    in[2] = objz;
    in[3] = 1.0f;

    FloatUtil.multMatrixVec(modelMatrix, in, out);
    FloatUtil.multMatrixVec(projMatrix, out, in);

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
  @SuppressWarnings("deprecation")
  public boolean gluProject(final float objx, final float objy, final float objz,
                            final FloatBuffer modelMatrix,
                            final FloatBuffer projMatrix,
                            final IntBuffer viewport,
                            final FloatBuffer win_pos) {

    final float[] in = this.mat4Tmp1;
    final float[] out = this.mat4Tmp2;

    in[0] = objx;
    in[1] = objy;
    in[2] = objz;
    in[3] = 1.0f;

    FloatUtil.multMatrixVec(modelMatrix, in, out);
    FloatUtil.multMatrixVec(projMatrix, out, in);

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
   * <p>
   * Traditional <code>gluUnProject</code> implementation.
   * </p>
   *
   * @param winx
   * @param winy
   * @param winz
   * @param viewport 4 component viewport vector
   * @param viewport_offset
   * @param obj_pos 3 component object coordinate, the result
   * @param obj_pos_offset
   * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
   */
  public boolean gluUnProject(final float winx, final float winy, final float winz,
                              final float[] modelMatrix, final int modelMatrix_offset,
                              final float[] projMatrix, final int projMatrix_offset,
                              final int[] viewport, final int viewport_offset,
                              final float[] obj_pos, final int obj_pos_offset) {
    return FloatUtil.mapWinToObjCoords(winx, winy, winz,
                                   modelMatrix, modelMatrix_offset,
                                   projMatrix, projMatrix_offset,
                                   viewport, viewport_offset,
                                   obj_pos, obj_pos_offset,
                                   mat4Tmp1, mat4Tmp2);
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
  @SuppressWarnings("deprecation")
  public boolean gluUnProject(final float winx, final float winy, final float winz,
                              final FloatBuffer modelMatrix,
                              final FloatBuffer projMatrix,
                              final int[] viewport, final int viewport_offset,
                              final float[] obj_pos, final int obj_pos_offset) {
    // mat4Tmp1 = P x M
    FloatUtil.multMatrix(projMatrix, modelMatrix, mat4Tmp1);

    // mat4Tmp1 = Inv(P x M)
    if ( null == FloatUtil.invertMatrix(mat4Tmp1, mat4Tmp1) ) {
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
    // object raw coords = Inv(P x M) *  winPos  -> mat4Tmp2
    FloatUtil.multMatrixVec(mat4Tmp1, 0, mat4Tmp2, 0, mat4Tmp2, raw_off);

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
  @SuppressWarnings("deprecation")
  public boolean gluUnProject(final float winx, final float winy, final float winz,
                              final FloatBuffer modelMatrix,
                              final FloatBuffer projMatrix,
                              final IntBuffer viewport,
                              final FloatBuffer obj_pos) {
    final int vPos = viewport.position();
    final int oPos = obj_pos.position();

    // mat4Tmp1 = P x M
    FloatUtil.multMatrix(projMatrix, modelMatrix, mat4Tmp1);

    // mat4Tmp1 = Inv(P x M)
    if ( null == FloatUtil.invertMatrix(mat4Tmp1, mat4Tmp1) ) {
      return false;
    }

    mat4Tmp2[0] = winx;
    mat4Tmp2[1] = winy;
    mat4Tmp2[2] = winz;
    mat4Tmp2[3] = 1.0f;

    // Map x and y from window coordinates
    mat4Tmp2[0] = (mat4Tmp2[0] - viewport.get(0+vPos)) / viewport.get(2+vPos);
    mat4Tmp2[1] = (mat4Tmp2[1] - viewport.get(1+vPos)) / viewport.get(3+vPos);

    // Map to range -1 to 1
    mat4Tmp2[0] = mat4Tmp2[0] * 2 - 1;
    mat4Tmp2[1] = mat4Tmp2[1] * 2 - 1;
    mat4Tmp2[2] = mat4Tmp2[2] * 2 - 1;

    final int raw_off = 4;
    // object raw coords = Inv(P x M) *  winPos  -> mat4Tmp2
    FloatUtil.multMatrixVec(mat4Tmp1, 0, mat4Tmp2, 0, mat4Tmp2, raw_off);

    if (mat4Tmp2[3+raw_off] == 0.0) {
      return false;
    }

    mat4Tmp2[3+raw_off] = 1.0f / mat4Tmp2[3+raw_off];

    obj_pos.put(0+oPos, mat4Tmp2[0+raw_off] * mat4Tmp2[3+raw_off]);
    obj_pos.put(1+oPos, mat4Tmp2[1+raw_off] * mat4Tmp2[3+raw_off]);
    obj_pos.put(2+oPos, mat4Tmp2[2+raw_off] * mat4Tmp2[3+raw_off]);

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
   * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
   */
   public boolean gluUnProject4(final float winx, final float winy, final float winz, final float clipw,
                                final float[] modelMatrix, final int modelMatrix_offset,
                                final float[] projMatrix, final int projMatrix_offset,
                                final int[] viewport, final int viewport_offset,
                                final float near, final float far,
                                final float[] obj_pos, final int obj_pos_offset ) {
    return FloatUtil.mapWinToObjCoords(winx, winy, winz, clipw,
                                       modelMatrix, modelMatrix_offset,
                                       projMatrix, projMatrix_offset,
                                       viewport, viewport_offset,
                                       near, far,
                                       obj_pos, obj_pos_offset,
                                       mat4Tmp1, mat4Tmp2);
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
  @SuppressWarnings("deprecation")
  public boolean gluUnProject4(final float winx, final float winy, final float winz, final float clipw,
                               final FloatBuffer modelMatrix, final FloatBuffer projMatrix,
                               final IntBuffer viewport,
                               final float near, final float far,
                               final FloatBuffer obj_pos) {
    FloatUtil.multMatrix(projMatrix, modelMatrix, mat4Tmp1);

    if ( null == FloatUtil.invertMatrix(mat4Tmp1, mat4Tmp1) ) {
      return false;
    }

    mat4Tmp2[0] = winx;
    mat4Tmp2[1] = winy;
    mat4Tmp2[2] = winz;
    mat4Tmp2[3] = clipw;

    // Map x and y from window coordinates
    final int vPos = viewport.position();
    mat4Tmp2[0] = (mat4Tmp2[0] - viewport.get(0+vPos)) / viewport.get(2+vPos);
    mat4Tmp2[1] = (mat4Tmp2[1] - viewport.get(1+vPos)) / viewport.get(3+vPos);
    mat4Tmp2[2] = (mat4Tmp2[2] - near) / (far - near);

    // Map to range -1 to 1
    mat4Tmp2[0] = mat4Tmp2[0] * 2 - 1;
    mat4Tmp2[1] = mat4Tmp2[1] * 2 - 1;
    mat4Tmp2[2] = mat4Tmp2[2] * 2 - 1;

    final int raw_off = 4;
    FloatUtil.multMatrixVec(mat4Tmp1, 0, mat4Tmp2, 0, mat4Tmp2, raw_off);

    if (mat4Tmp2[3+raw_off] == 0.0f) {
      return false;
    }

    final int oPos = obj_pos.position();
    obj_pos.put(0+oPos, mat4Tmp2[0+raw_off]);
    obj_pos.put(1+oPos, mat4Tmp2[1+raw_off]);
    obj_pos.put(2+oPos, mat4Tmp2[2+raw_off]);
    obj_pos.put(3+oPos, mat4Tmp2[3+raw_off]);
    return true;
  }


  /**
   * Make given matrix the <i>pick</i> matrix based on given parameters.
   * <p>
   * Traditional <code>gluPickMatrix</code> implementation.
   * </p>
   * @param x
   * @param y
   * @param deltaX
   * @param deltaY
   * @param viewport 4 component viewport vector
   * @param viewport_offset
   */
  public void gluPickMatrix(final GLMatrixFunc gl,
                            final float x, final float y,
                            final float deltaX, final float deltaY,
                            final IntBuffer viewport) {
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
   * Make given matrix the <i>pick</i> matrix based on given parameters.
   * <p>
   * Traditional <code>gluPickMatrix</code> implementation.
   * </p>
   * @param x
   * @param y
   * @param deltaX
   * @param deltaY
   * @param viewport 4 component viewport vector
   * @param viewport_offset
   */
  public void gluPickMatrix(final GLMatrixFunc gl,
                            final float x, final float y,
                            final float deltaX, final float deltaY,
                            final int[] viewport, final int viewport_offset) {
    if( null != FloatUtil.makePick(mat4Tmp1, 0, x, y, deltaX, deltaY, viewport, viewport_offset, mat4Tmp2) ) {
        gl.glMultMatrixf(mat4Tmp1, 0);
    }
  }

}
