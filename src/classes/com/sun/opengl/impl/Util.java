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

package com.sun.opengl.impl;

import java.nio.IntBuffer;
import javax.media.opengl.*;

/**
 * Util.java
 * <p/>
 * <p/>
 * Created 7-jan-2004
 *
 * @author Erik Duijs
 */
class Util {

  /**
   * temp int[] of one for getting an int from some GL functions
   */
  private int[] scratch = new int[1];

  /**
   * Return ceiling of integer division
   *
   * @param a
   * @param b
   *
   * @return int
   */
  protected static int ceil(int a, int b) {
    return (a % b == 0 ? a / b : a / b + 1);
  }

  /**
   * Method compPerPix.
   *
   * @param format
   *
   * @return int
   */
  protected static int compPerPix(int format) {
    /* Determine number of components per pixel */
    switch ( format ) {
    case GL.GL_COLOR_INDEX:
    case GL.GL_STENCIL_INDEX:
    case GL.GL_DEPTH_COMPONENT:
    case GL.GL_RED:
    case GL.GL_GREEN:
    case GL.GL_BLUE:
    case GL.GL_ALPHA:
    case GL.GL_LUMINANCE:
      return 1;
    case GL.GL_LUMINANCE_ALPHA:
      return 2;
    case GL.GL_RGB:
    case GL.GL_BGR:
      return 3;
    case GL.GL_RGBA:
    case GL.GL_BGRA:
      return 4;
    default :
      return -1;
    }
  }

  /**
   * Method nearestPower.
   * <p/>
   * Compute the nearest power of 2 number.  This algorithm is a little strange, but it works quite well.
   *
   * @param value
   *
   * @return int
   */
  protected static int nearestPower(int value) {
    int i;

    i = 1;

    /* Error! */
    if ( value == 0 )
      return -1;

    for ( ; ; ) {
      if ( value == 1 ) {
        return i;
      } else if ( value == 3 ) {
        return i << 2;
      }
      value >>= 1;
      i <<= 1;
    }
  }

  /**
   * Method bytesPerPixel.
   *
   * @param format
   * @param type
   *
   * @return int
   */
  protected static int bytesPerPixel(int format, int type) {
    int n, m;

    switch ( format ) {
    case GL.GL_COLOR_INDEX:
    case GL.GL_STENCIL_INDEX:
    case GL.GL_DEPTH_COMPONENT:
    case GL.GL_RED:
    case GL.GL_GREEN:
    case GL.GL_BLUE:
    case GL.GL_ALPHA:
    case GL.GL_LUMINANCE:
      n = 1;
      break;
    case GL.GL_LUMINANCE_ALPHA:
      n = 2;
      break;
    case GL.GL_RGB:
    case GL.GL_BGR:
      n = 3;
      break;
    case GL.GL_RGBA:
    case GL.GL_BGRA:
      n = 4;
      break;
    default :
      n = 0;
    }

    switch ( type ) {
    case GL.GL_UNSIGNED_BYTE:
      m = 1;
      break;
    case GL.GL_BYTE:
      m = 1;
      break;
    case GL.GL_BITMAP:
      m = 1;
      break;
    case GL.GL_UNSIGNED_SHORT:
      m = 2;
      break;
    case GL.GL_SHORT:
      m = 2;
      break;
    case GL.GL_UNSIGNED_INT:
      m = 4;
      break;
    case GL.GL_INT:
      m = 4;
      break;
    case GL.GL_FLOAT:
      m = 4;
      break;
    default :
      m = 0;
    }

    return n * m;
  }

  /**
   * Convenience method for returning an int, rather than getting it out of a buffer yourself.
   *
   * @param what
   *
   * @return int
   */
  protected int glGetIntegerv(GL gl, int what) {
    gl.glGetIntegerv(what, scratch, 0);
    return scratch[0];
  }
}
