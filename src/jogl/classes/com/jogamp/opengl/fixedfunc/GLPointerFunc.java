/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.jogamp.opengl.fixedfunc;

import com.jogamp.opengl.*;

public interface GLPointerFunc {
  public static final int GL_VERTEX_ARRAY = 0x8074;
  public static final int GL_NORMAL_ARRAY = 0x8075;
  public static final int GL_COLOR_ARRAY = 0x8076;
  public static final int GL_TEXTURE_COORD_ARRAY = 0x8078;

  public void glEnableClientState(int arrayName);
  public void glDisableClientState(int arrayName);

  public void glVertexPointer(GLArrayData array);
  public void glVertexPointer(int size, int type, int stride, java.nio.Buffer pointer);
  public void glVertexPointer(int size, int type, int stride, long pointer_buffer_offset);

  public void glColorPointer(GLArrayData array);
  public void glColorPointer(int size, int type, int stride, java.nio.Buffer pointer);
  public void glColorPointer(int size, int type, int stride, long pointer_buffer_offset);
  public void glColor4f(float red, float green, float blue, float alpha);

  public void glNormalPointer(GLArrayData array);
  public void glNormalPointer(int type, int stride, java.nio.Buffer pointer);
  public void glNormalPointer(int type, int stride, long pointer_buffer_offset);

  public void glTexCoordPointer(GLArrayData array);
  public void glTexCoordPointer(int size, int type, int stride, java.nio.Buffer pointer);
  public void glTexCoordPointer(int size, int type, int stride, long pointer_buffer_offset);

}

