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
package com.jogamp.opengl.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.common.nio.Buffers;

/**
 * ProjectFloat.java
 * <p/>
 * <p/>
 * Created 11-jan-2004
 * 
 * @author Erik Duijs
 * @author Kenneth Russell
 * @author Sven Gothel
 */
public class ProjectFloat {
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
  public static void makeIdentityf(float[] m, int offset) {
    for (int i = 0; i < 16; i++) {
      m[i+offset] = IDENTITY_MATRIX[i];
    }
  }

  /**
   * Make matrix an identity matrix
   */
  public static void makeIdentityf(FloatBuffer m) {
    int oldPos = m.position();
    m.put(IDENTITY_MATRIX);
    m.position(oldPos);
  }

  /**
   * Make matrix an zero matrix
   */
  public static void makeZero(float[] m, int offset) {
    for (int i = 0; i < 16; i++) {
      m[i+offset] = 0;
    }
  }

  /**
   * Make matrix an zero matrix
   */
  public static void makeZero(FloatBuffer m) {
    int oldPos = m.position();
    m.put(ZERO_MATRIX);
    m.position(oldPos);
  }
  
  /**
   * @param a
   * @param b
   * @param d result a*b
   */
  public static final void multMatrixf(final float[] a, int a_off, final float[] b, int b_off, float[] d, int d_off) {
     for (int i = 0; i < 4; i++) {
        final float ai0=a[a_off+i+0*4],  ai1=a[a_off+i+1*4],  ai2=a[a_off+i+2*4],  ai3=a[a_off+i+3*4];
        d[d_off+i+0*4] = ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] ;
        d[d_off+i+1*4] = ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] ;
        d[d_off+i+2*4] = ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] ;
        d[d_off+i+3*4] = ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] ;
     }
  }

  /**
   * @param a
   * @param b
   * @param d result a*b
   */
  public static final void multMatrixf(final float[] a, int a_off, final float[] b, int b_off, FloatBuffer d) {
     final int dP = d.position();
     for (int i = 0; i < 4; i++) {
        final float ai0=a[a_off+i+0*4],  ai1=a[a_off+i+1*4],  ai2=a[a_off+i+2*4],  ai3=a[a_off+i+3*4];
        d.put(dP+i+0*4 , ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] );
        d.put(dP+i+1*4 , ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] );
        d.put(dP+i+2*4 , ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] );
        d.put(dP+i+3*4 , ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] );
     }
  }

  /**
   * @param a
   * @param b
   * @param d result a*b
   */
  public static final void multMatrixf(final FloatBuffer a, final float[] b, int b_off, FloatBuffer d) {
     final int aP = a.position(); 
     final int dP = d.position();
     for (int i = 0; i < 4; i++) {
        final float ai0=a.get(aP+i+0*4),  ai1=a.get(aP+i+1*4),  ai2=a.get(aP+i+2*4),  ai3=a.get(aP+i+3*4);
        d.put(dP+i+0*4 , ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] );
        d.put(dP+i+1*4 , ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] );
        d.put(dP+i+2*4 , ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] );
        d.put(dP+i+3*4 , ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] );
     }
  }

  /**
   * @param a
   * @param b
   * @param d result a*b
   */
  public static final void multMatrixf(final FloatBuffer a, final FloatBuffer b, FloatBuffer d) {
     final int aP = a.position(); 
     final int bP = b.position();
     final int dP = d.position();
     for (int i = 0; i < 4; i++) {
        final float ai0=a.get(aP+i+0*4),  ai1=a.get(aP+i+1*4),  ai2=a.get(aP+i+2*4),  ai3=a.get(aP+i+3*4);
        d.put(dP+i+0*4 , ai0 * b.get(bP+0+0*4) + ai1 * b.get(bP+1+0*4) + ai2 * b.get(bP+2+0*4) + ai3 * b.get(bP+3+0*4) );
        d.put(dP+i+1*4 , ai0 * b.get(bP+0+1*4) + ai1 * b.get(bP+1+1*4) + ai2 * b.get(bP+2+1*4) + ai3 * b.get(bP+3+1*4) );
        d.put(dP+i+2*4 , ai0 * b.get(bP+0+2*4) + ai1 * b.get(bP+1+2*4) + ai2 * b.get(bP+2+2*4) + ai3 * b.get(bP+3+2*4) );
        d.put(dP+i+3*4 , ai0 * b.get(bP+0+3*4) + ai1 * b.get(bP+1+3*4) + ai2 * b.get(bP+2+3*4) + ai3 * b.get(bP+3+3*4) );
     }
  }
  
  
  /**
   * Normalize vector
   *
   * @param v makes len(v)==1
   */
  public static void normalize(float[] v) {
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
  public static void normalize(FloatBuffer v) {
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
   * Calculate cross-product
   *
   * @param v1
   * @param v2
   * @param result v1 X v2
   */
  public static void cross(float[] v1, float[] v2, float[] result) {
    result[0] = v1[1] * v2[2] - v1[2] * v2[1];
    result[1] = v1[2] * v2[0] - v1[0] * v2[2];
    result[2] = v1[0] * v2[1] - v1[1] * v2[0];
  }

  /**
   * Calculate cross-product
   *
   * @param v1
   * @param v2
   * @param result v1 X v2
   */
  public static void cross(FloatBuffer v1, FloatBuffer v2, FloatBuffer result) {
    final int v1Pos = v1.position();
    final int v2Pos = v2.position();
    final int rPos  = result.position();

    result.put(0+rPos, v1.get(1+v1Pos) * v2.get(2+v2Pos) - v1.get(2+v1Pos) * v2.get(1+v2Pos));
    result.put(1+rPos, v1.get(2+v1Pos) * v2.get(0+v2Pos) - v1.get(0+v1Pos) * v2.get(2+v2Pos));
    result.put(2+rPos, v1.get(0+v1Pos) * v2.get(1+v2Pos) - v1.get(1+v1Pos) * v2.get(0+v2Pos));
  }

  /**
   * Method __gluMultMatrixVecf
   * 
   * @param m_in
   * @param m_in_off
   * @param v_in
   * @param v_out m_in * v_in
   */
  public static void multMatrixVecf(float[] m_in, int m_in_off, float[] v_in, int v_in_off, float[] v_out) {
    for (int i = 0; i < 4; i++) {
      v_out[i] =
        v_in[0+v_in_off] * m_in[0*4+i+m_in_off] +
        v_in[1+v_in_off] * m_in[1*4+i+m_in_off] +
        v_in[2+v_in_off] * m_in[2*4+i+m_in_off] +
        v_in[3+v_in_off] * m_in[3*4+i+m_in_off];
    }
  }

  /**
   * Method __gluMultMatrixVecf
   * 
   * @param m_in
   * @param m_in_off
   * @param v_in
   * @param v_out m_in * v_in
   */
  public static void multMatrixVecf(float[] m_in, float[] v_in, float[] v_out) {
    for (int i = 0; i < 4; i++) {
      v_out[i] =
        v_in[0] * m_in[0*4+i] +
        v_in[1] * m_in[1*4+i] +
        v_in[2] * m_in[2*4+i] +
        v_in[3] * m_in[3*4+i];
    }
  }
  
  /**
   * Method __gluMultMatrixVecf
   * 
   * @param m_in
   * @param v_in
   * @param v_out m_in * v_in
   */
  public static void multMatrixVecf(FloatBuffer m_in, FloatBuffer v_in, FloatBuffer v_out) {
    int inPos = v_in.position();
    int outPos = v_out.position();
    int matrixPos = m_in.position();
    for (int i = 0; i < 4; i++) {
      v_out.put(i + outPos,
              v_in.get(0+inPos) * m_in.get(0*4+i+matrixPos) +
              v_in.get(1+inPos) * m_in.get(1*4+i+matrixPos) +
              v_in.get(2+inPos) * m_in.get(2*4+i+matrixPos) +
              v_in.get(3+inPos) * m_in.get(3*4+i+matrixPos));
    }
  }

  /**
   * Slices a ByteBuffer or a primitive float array to a FloatBuffer at the given position with the given size
   * in float-space.
   * <p> 
   * Using a ByteBuffer as the source guarantees 
   * keeping the source native order programmatically.  
   * This works around <a href="http://code.google.com/p/android/issues/detail?id=16434">Honeycomb / Android 3.0 Issue 16434</a>. 
   * This bug is resolved at least in Android 3.2.
   * </p>
   * 
   * @param buf source Buffer, maybe ByteBuffer (recommended) or FloatBuffer or <code>null</code>. 
   *            Buffer's position is ignored and floatPos is being used.
   * @param backing source float array or <code>null</code>
   * @param floatPos {@link Buffers#SIZEOF_FLOAT} position
   * @param floatSize {@link Buffers#SIZEOF_FLOAT} size 
   * @return FloatBuffer w/ native byte order as given ByteBuffer
   */
  public static FloatBuffer slice2Float(Buffer buf, float[] backing, int floatPos, int floatSize) {
        if(buf instanceof ByteBuffer) {
            ByteBuffer bb = (ByteBuffer) buf;
            bb.position( floatPos * Buffers.SIZEOF_FLOAT );
            bb.limit( (floatPos + floatSize) * Buffers.SIZEOF_FLOAT );
            FloatBuffer fb = bb.slice().order(bb.order()).asFloatBuffer(); // slice and duplicate may change byte order
            fb.mark();
            return fb;
        } else if(null != backing) {
            FloatBuffer fb  = FloatBuffer.wrap(backing, floatPos, floatSize);
            fb.mark();
            return fb;
        } else if(buf instanceof FloatBuffer) {
            FloatBuffer fb = (FloatBuffer) buf;
            fb.position( floatPos );
            fb.limit( floatPos + floatSize );
            FloatBuffer fb0 = fb.slice(); // slice and duplicate may change byte order
            fb0.mark();
            return fb0;
        } else {
            throw new InternalError("XXX");
        }
  }
  
  public static final int getRequiredFloatBufferSize() { return 2*16+2*4+3*3; }
  
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
  private FloatBuffer matrixBuf;
  private FloatBuffer tempInvertMatrixBuf;

  private FloatBuffer inBuf;
  private FloatBuffer outBuf;

  private FloatBuffer forwardBuf;
  private FloatBuffer sideBuf;
  private FloatBuffer upBuf;

  public ProjectFloat() {
      this(false);
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
    int floatPos = floatOffset;
    int floatSize = 16;
    matrixBuf = slice2Float(floatBuffer, floatArray, floatPos, floatSize);
    floatPos += floatSize;
    tempInvertMatrixBuf = slice2Float(floatBuffer, floatArray, floatPos, floatSize);
    floatPos += floatSize;
    floatSize = 4;
    inBuf = slice2Float(floatBuffer, floatArray, floatPos, floatSize);
    floatPos += floatSize;
    outBuf = slice2Float(floatBuffer, floatArray, floatPos, floatSize);
    floatPos += floatSize;
    floatSize = 3;
    forwardBuf = slice2Float(floatBuffer, floatArray, floatPos, floatSize);
    floatPos += floatSize;
    sideBuf = slice2Float(floatBuffer, floatArray, floatPos, floatSize);
    floatPos += floatSize;
    upBuf = slice2Float(floatBuffer, floatArray, floatPos, floatSize);
  }

  public void destroy() {
    matrixBuf = null;
    tempInvertMatrixBuf = null;
    inBuf = null;
    outBuf = null;
    forwardBuf = null;
    sideBuf = null;
    upBuf = null;
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
    float[][] temp = tempInvertMatrix;

    for (i = 0; i < 4; i++) {
      for (j = 0; j < 4; j++) {
        temp[i][j] = src[i*4+j+srcOffset];
      }
    }
    makeIdentityf(inverse, inverseOffset);

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

    int srcPos = src.position();
    int invPos = inverse.position();

    FloatBuffer temp = tempInvertMatrixBuf;

    for (i = 0; i < 4; i++) {
      for (j = 0; j < 4; j++) {
        temp.put(i*4+j, src.get(i*4+j + srcPos));
      }
    }
    makeIdentityf(inverse);

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
    float sine, cotangent, deltaZ;
    float radians = fovy / 2 * (float) Math.PI / 180;

    deltaZ = zFar - zNear;
    sine = (float) Math.sin(radians);

    if ((deltaZ == 0) || (sine == 0) || (aspect == 0)) {
      return;
    }

    cotangent = (float) Math.cos(radians) / sine;

    makeIdentityf(matrixBuf);

    matrixBuf.put(0 * 4 + 0, cotangent / aspect);
    matrixBuf.put(1 * 4 + 1, cotangent);
    matrixBuf.put(2 * 4 + 2, - (zFar + zNear) / deltaZ);
    matrixBuf.put(2 * 4 + 3, -1);
    matrixBuf.put(3 * 4 + 2, -2 * zNear * zFar / deltaZ);
    matrixBuf.put(3 * 4 + 3, 0);

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
    FloatBuffer forward = this.forwardBuf;
    FloatBuffer side = this.sideBuf;
    FloatBuffer up = this.upBuf;

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

    makeIdentityf(matrixBuf);
    matrixBuf.put(0 * 4 + 0, side.get(0));
    matrixBuf.put(1 * 4 + 0, side.get(1));
    matrixBuf.put(2 * 4 + 0, side.get(2));

    matrixBuf.put(0 * 4 + 1, up.get(0));
    matrixBuf.put(1 * 4 + 1, up.get(1));
    matrixBuf.put(2 * 4 + 1, up.get(2));

    matrixBuf.put(0 * 4 + 2, -forward.get(0));
    matrixBuf.put(1 * 4 + 2, -forward.get(1));
    matrixBuf.put(2 * 4 + 2, -forward.get(2));

    gl.glMultMatrixf(matrixBuf);
    gl.glTranslatef(-eyex, -eyey, -eyez);
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
  public boolean gluProject(float objx, float objy, float objz,
                            float[] modelMatrix, int modelMatrix_offset,
                            float[] projMatrix, int projMatrix_offset,
                            int[] viewport, int viewport_offset,
                            float[] win_pos, int win_pos_offset ) {

    float[] in = this.in;
    float[] out = this.out;

    in[0] = objx;
    in[1] = objy;
    in[2] = objz;
    in[3] = 1.0f;

    multMatrixVecf(modelMatrix, modelMatrix_offset, in, 0, out);
    multMatrixVecf(projMatrix, projMatrix_offset, out, 0, in);

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

  public boolean gluProject(float objx, float objy, float objz,
                            FloatBuffer modelMatrix,
                            FloatBuffer projMatrix,
                            int[] viewport, int viewport_offset,
                            float[] win_pos, int win_pos_offset ) {

    FloatBuffer in = this.inBuf;
    FloatBuffer out = this.outBuf;

    in.put(0, objx);
    in.put(1, objy);
    in.put(2, objz);
    in.put(3, 1.0f);

    multMatrixVecf(modelMatrix, in, out);
    multMatrixVecf(projMatrix, out, in);

    if (in.get(3) == 0.0f) {
      return false;
    }

    in.put(3, (1.0f / in.get(3)) * 0.5f);

    // Map x, y and z to range 0-1
    in.put(0, in.get(0) * in.get(3) + 0.5f);
    in.put(1, in.get(1) * in.get(3) + 0.5f);
    in.put(2, in.get(2) * in.get(3) + 0.5f);

    // Map x,y to viewport
    win_pos[0+win_pos_offset] = in.get(0) * viewport[2+viewport_offset] + viewport[0+viewport_offset];
    win_pos[1+win_pos_offset] = in.get(1) * viewport[3+viewport_offset] + viewport[1+viewport_offset];
    win_pos[2+win_pos_offset] = in.get(2);

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
  public boolean gluProject(float objx, float objy, float objz,
                            FloatBuffer modelMatrix,
                            FloatBuffer projMatrix,
                            IntBuffer viewport,
                            FloatBuffer win_pos) {

    FloatBuffer in = this.inBuf;
    FloatBuffer out = this.outBuf;

    in.put(0, objx);
    in.put(1, objy);
    in.put(2, objz);
    in.put(3, 1.0f);

    multMatrixVecf(modelMatrix, in, out);
    multMatrixVecf(projMatrix, out, in);

    if (in.get(3) == 0.0f) {
      return false;
    }

    in.put(3, (1.0f / in.get(3)) * 0.5f);

    // Map x, y and z to range 0-1
    in.put(0, in.get(0) * in.get(3) + 0.5f);
    in.put(1, in.get(1) * in.get(3) + 0.5f);
    in.put(2, in.get(2) * in.get(3) + 0.5f);

    // Map x,y to viewport
    int vPos = viewport.position();
    int wPos = win_pos.position();
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
  public boolean gluUnProject(float winx, float winy, float winz,
                              float[] modelMatrix, int modelMatrix_offset,
                              float[] projMatrix, int projMatrix_offset,
                              int[] viewport, int viewport_offset,
                              float[] obj_pos, int obj_pos_offset) {
    float[] in = this.in;
    float[] out = this.out;

    multMatrixf(modelMatrix, modelMatrix_offset, projMatrix, projMatrix_offset, matrix, 0);

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

    multMatrixVecf(matrix, in, out);

    if (out[3] == 0.0) {
      return false;
    }

    out[3] = 1.0f / out[3];

    obj_pos[0+obj_pos_offset] = out[0] * out[3];
    obj_pos[1+obj_pos_offset] = out[1] * out[3];
    obj_pos[2+obj_pos_offset] = out[2] * out[3];

    return true;
  }


  public boolean gluUnProject(float winx, float winy, float winz,
                              FloatBuffer modelMatrix, 
                              FloatBuffer projMatrix,
                              int[] viewport, int viewport_offset,
                              float[] obj_pos, int obj_pos_offset) {
    FloatBuffer in = this.inBuf;
    FloatBuffer out = this.outBuf;

    multMatrixf(modelMatrix, projMatrix, matrixBuf);

    if (!gluInvertMatrixf(matrixBuf, matrixBuf)) {
      return false;
    }

    in.put(0, winx);
    in.put(1, winy);
    in.put(2, winz);
    in.put(3, 1.0f);

    // Map x and y from window coordinates
    in.put(0, (in.get(0) - viewport[0+viewport_offset]) / viewport[2+viewport_offset]);
    in.put(1, (in.get(1) - viewport[1+viewport_offset]) / viewport[3+viewport_offset]);
    
    // Map to range -1 to 1
    in.put(0, in.get(0) * 2 - 1);
    in.put(1, in.get(1) * 2 - 1);
    in.put(2, in.get(2) * 2 - 1);

    multMatrixVecf(matrixBuf, in, out);

    if (out.get(3) == 0.0f) {
      return false;
    }

    out.put(3, 1.0f / out.get(3));

    obj_pos[0+obj_pos_offset] = out.get(0) * out.get(3);
    obj_pos[1+obj_pos_offset] = out.get(1) * out.get(3);
    obj_pos[2+obj_pos_offset] = out.get(2) * out.get(3);

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
  public boolean gluUnProject(float winx, float winy, float winz,
                              FloatBuffer modelMatrix, 
                              FloatBuffer projMatrix,
                              IntBuffer viewport,
                              FloatBuffer obj_pos) {
    FloatBuffer in = this.inBuf;
    FloatBuffer out = this.outBuf;

    multMatrixf(modelMatrix, projMatrix, matrixBuf);

    if (!gluInvertMatrixf(matrixBuf, matrixBuf)) {
      return false;
    }

    in.put(0, winx);
    in.put(1, winy);
    in.put(2, winz);
    in.put(3, 1.0f);

    // Map x and y from window coordinates
    int vPos = viewport.position();
    int oPos = obj_pos.position();
    in.put(0, (in.get(0) - viewport.get(0+vPos)) / viewport.get(2+vPos));
    in.put(1, (in.get(1) - viewport.get(1+vPos)) / viewport.get(3+vPos));

    // Map to range -1 to 1
    in.put(0, in.get(0) * 2 - 1);
    in.put(1, in.get(1) * 2 - 1);
    in.put(2, in.get(2) * 2 - 1);

    multMatrixVecf(matrixBuf, in, out);

    if (out.get(3) == 0.0f) {
      return false;
    }

    out.put(3, 1.0f / out.get(3));

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
    float[] in = this.in;
    float[] out = this.out;

    multMatrixf(modelMatrix, modelMatrix_offset, projMatrix, projMatrix_offset, matrix, 0);

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

    multMatrixVecf(matrix, in, out);

    if (out[3] == 0.0f)
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
    FloatBuffer in = this.inBuf;
    FloatBuffer out = this.outBuf;

    multMatrixf(modelMatrix, projMatrix, matrixBuf);

    if (!gluInvertMatrixf(matrixBuf, matrixBuf))
      return false;

    in.put(0, winx);
    in.put(1, winy);
    in.put(2, winz);
    in.put(3, clipw);

    // Map x and y from window coordinates
    int vPos = viewport.position();
    in.put(0, (in.get(0) - viewport.get(0+vPos)) / viewport.get(2+vPos));
    in.put(1, (in.get(1) - viewport.get(1+vPos)) / viewport.get(3+vPos));
    in.put(2, (in.get(2) - near) / (far - near));

    // Map to range -1 to 1
    in.put(0, in.get(0) * 2 - 1);
    in.put(1, in.get(1) * 2 - 1);
    in.put(2, in.get(2) * 2 - 1);

    multMatrixVecf(matrixBuf, in, out);

    if (out.get(3) == 0.0f)
      return false;

    int oPos = obj_pos.position();
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
    int vPos = viewport.position();
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
