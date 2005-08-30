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
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.utils;

import java.nio.*;
import java.util.*;

/** Utility routines for dealing with direct buffers. */

public class BufferUtils {
  public static final int SIZEOF_DOUBLE = 8;
  public static final int SIZEOF_FLOAT = 4;
  public static final int SIZEOF_INT = 4;
  public static final int SIZEOF_LONG = 8;
  public static final int SIZEOF_SHORT = 2;

  public static DoubleBuffer newDoubleBuffer(int numElements) {
    ByteBuffer bb = newByteBuffer(numElements * SIZEOF_DOUBLE);
    return bb.asDoubleBuffer();
  }

  public static FloatBuffer newFloatBuffer(int numElements) {
    ByteBuffer bb = newByteBuffer(numElements * SIZEOF_FLOAT);
    return bb.asFloatBuffer();
  }

  public static IntBuffer newIntBuffer(int numElements) {
    ByteBuffer bb = newByteBuffer(numElements * SIZEOF_INT);
    return bb.asIntBuffer();
  }

  public static ByteBuffer newByteBuffer(int numElements) {
    ByteBuffer bb = ByteBuffer.allocateDirect(numElements);
    bb.order(ByteOrder.nativeOrder());
    return bb;
  }

  public static DoubleBuffer copyDoubleBuffer(DoubleBuffer orig) {
    DoubleBuffer dest = newDoubleBuffer(orig.capacity());
    orig.rewind();
    dest.put(orig);
    return dest;
  }

  public static FloatBuffer copyFloatBuffer(FloatBuffer orig) {
    FloatBuffer dest = newFloatBuffer(orig.capacity());
    orig.rewind();
    dest.put(orig);
    return dest;
  }

  public static IntBuffer copyIntBuffer(IntBuffer orig) {
    IntBuffer dest = newIntBuffer(orig.capacity());
    orig.rewind();
    dest.put(orig);
    return dest;
  }

  public static ByteBuffer copyByteBuffer(ByteBuffer orig) {
    ByteBuffer dest = newByteBuffer(orig.capacity());
    orig.rewind();
    dest.put(orig);
    return dest;
  }

  private static Map bufferOffsetCache = Collections.synchronizedMap(new HashMap());

  /** Creates an "offset buffer" for use with the
      ARB_vertex_buffer_object extension. The resulting Buffers are
      suitable for use with routines such as glVertexPointer <em>when
      used in conjunction with that extension</em>. They have no
      capacity and are not suitable for passing to OpenGL routines
      that do not support buffer offsets, or to non-OpenGL
      routines. */
  public static ByteBuffer bufferOffset(int offset) {
    Integer key = new Integer(offset);
    ByteBuffer buf = (ByteBuffer) bufferOffsetCache.get(key);
    if (buf == null) {
      buf = bufferOffset0(offset);
      bufferOffsetCache.put(key, buf);
    }
    return buf;
  }

  private static native ByteBuffer bufferOffset0(int offset);
}
