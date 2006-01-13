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

package com.sun.opengl.util;

import java.nio.*;
import java.util.*;

/** Utility routines for dealing with direct buffers. */

public class BufferUtil {
  public static final int SIZEOF_BYTE = 1;
  public static final int SIZEOF_SHORT = 2;
  public static final int SIZEOF_INT = 4;
  public static final int SIZEOF_FLOAT = 4;
  public static final int SIZEOF_LONG = 8;
  public static final int SIZEOF_DOUBLE = 8;

  private BufferUtil() {}

  //----------------------------------------------------------------------
  // Allocation routines
  //

  /** Allocates a new direct ByteBuffer with the specified number of
      elements. The returned buffer will have its byte order set to
      the host platform's native byte order. */
  public static ByteBuffer newByteBuffer(int numElements) {
    ByteBuffer bb = ByteBuffer.allocateDirect(numElements);
    bb.order(ByteOrder.nativeOrder());
    return bb;
  }

  /** Allocates a new direct DoubleBuffer with the specified number of
      elements. The returned buffer will have its byte order set to
      the host platform's native byte order. */
  public static DoubleBuffer newDoubleBuffer(int numElements) {
    ByteBuffer bb = newByteBuffer(numElements * SIZEOF_DOUBLE);
    return bb.asDoubleBuffer();
  }

  /** Allocates a new direct FloatBuffer with the specified number of
      elements. The returned buffer will have its byte order set to
      the host platform's native byte order. */
  public static FloatBuffer newFloatBuffer(int numElements) {
    ByteBuffer bb = newByteBuffer(numElements * SIZEOF_FLOAT);
    return bb.asFloatBuffer();
  }

  /** Allocates a new direct IntBuffer with the specified number of
      elements. The returned buffer will have its byte order set to
      the host platform's native byte order. */
  public static IntBuffer newIntBuffer(int numElements) {
    ByteBuffer bb = newByteBuffer(numElements * SIZEOF_INT);
    return bb.asIntBuffer();
  }

  /** Allocates a new direct LongBuffer with the specified number of
      elements. The returned buffer will have its byte order set to
      the host platform's native byte order. */
  public static LongBuffer newLongBuffer(int numElements) {
    ByteBuffer bb = newByteBuffer(numElements * SIZEOF_LONG);
    return bb.asLongBuffer();
  }

  /** Allocates a new direct ShortBuffer with the specified number of
      elements. The returned buffer will have its byte order set to
      the host platform's native byte order. */
  public static ShortBuffer newShortBuffer(int numElements) {
    ByteBuffer bb = newByteBuffer(numElements * SIZEOF_SHORT);
    return bb.asShortBuffer();
  }

  //----------------------------------------------------------------------
  // Copy routines (type-to-type)
  //

  /** Copies the <i>remaining</i> elements (as defined by
      <code>limit() - position()</code>) in the passed ByteBuffer into
      a newly-allocated direct ByteBuffer. The returned buffer will
      have its byte order set to the host platform's native byte
      order. The position of the newly-allocated buffer will be zero,
      and the position of the passed buffer is unchanged (though its
      mark is changed). */
  public static ByteBuffer copyByteBuffer(ByteBuffer orig) {
    ByteBuffer dest = newByteBuffer(orig.remaining());
    orig.mark();
    dest.put(orig);
    orig.reset();
    dest.rewind();
    return dest;
  }

  /** Copies the <i>remaining</i> elements (as defined by
      <code>limit() - position()</code>) in the passed DoubleBuffer
      into a newly-allocated direct DoubleBuffer. The returned buffer
      will have its byte order set to the host platform's native byte
      order. The position of the newly-allocated buffer will be zero,
      and the position of the passed buffer is unchanged (though its
      mark is changed). */
  public static DoubleBuffer copyDoubleBuffer(DoubleBuffer orig) {
    return copyDoubleBufferAsByteBuffer(orig).asDoubleBuffer();
  }

  /** Copies the <i>remaining</i> elements (as defined by
      <code>limit() - position()</code>) in the passed FloatBuffer
      into a newly-allocated direct FloatBuffer. The returned buffer
      will have its byte order set to the host platform's native byte
      order. The position of the newly-allocated buffer will be zero,
      and the position of the passed buffer is unchanged (though its
      mark is changed). */
  public static FloatBuffer copyFloatBuffer(FloatBuffer orig) {
    return copyFloatBufferAsByteBuffer(orig).asFloatBuffer();
  }

  /** Copies the <i>remaining</i> elements (as defined by
      <code>limit() - position()</code>) in the passed IntBuffer
      into a newly-allocated direct IntBuffer. The returned buffer
      will have its byte order set to the host platform's native byte
      order. The position of the newly-allocated buffer will be zero,
      and the position of the passed buffer is unchanged (though its
      mark is changed). */
  public static IntBuffer copyIntBuffer(IntBuffer orig) {
    return copyIntBufferAsByteBuffer(orig).asIntBuffer();
  }

  /** Copies the <i>remaining</i> elements (as defined by
      <code>limit() - position()</code>) in the passed LongBuffer
      into a newly-allocated direct LongBuffer. The returned buffer
      will have its byte order set to the host platform's native byte
      order. The position of the newly-allocated buffer will be zero,
      and the position of the passed buffer is unchanged (though its
      mark is changed). */
  public static LongBuffer copyLongBuffer(LongBuffer orig) {
    return copyLongBufferAsByteBuffer(orig).asLongBuffer();
  }

  /** Copies the <i>remaining</i> elements (as defined by
      <code>limit() - position()</code>) in the passed ShortBuffer
      into a newly-allocated direct ShortBuffer. The returned buffer
      will have its byte order set to the host platform's native byte
      order. The position of the newly-allocated buffer will be zero,
      and the position of the passed buffer is unchanged (though its
      mark is changed). */
  public static ShortBuffer copyShortBuffer(ShortBuffer orig) {
    return copyShortBufferAsByteBuffer(orig).asShortBuffer();
  }

  //----------------------------------------------------------------------
  // Copy routines (type-to-ByteBuffer)
  //

  /** Copies the <i>remaining</i> elements (as defined by
      <code>limit() - position()</code>) in the passed DoubleBuffer
      into a newly-allocated direct ByteBuffer. The returned buffer
      will have its byte order set to the host platform's native byte
      order. The position of the newly-allocated buffer will be zero,
      and the position of the passed buffer is unchanged (though its
      mark is changed). */
  public static ByteBuffer copyDoubleBufferAsByteBuffer(DoubleBuffer orig) {
    ByteBuffer dest = newByteBuffer(orig.remaining() * SIZEOF_DOUBLE);
    orig.mark();
    dest.asDoubleBuffer().put(orig);
    orig.reset();
    dest.rewind();
    return dest;
  }

  /** Copies the <i>remaining</i> elements (as defined by
      <code>limit() - position()</code>) in the passed FloatBuffer
      into a newly-allocated direct ByteBuffer. The returned buffer
      will have its byte order set to the host platform's native byte
      order. The position of the newly-allocated buffer will be zero,
      and the position of the passed buffer is unchanged (though its
      mark is changed). */
  public static ByteBuffer copyFloatBufferAsByteBuffer(FloatBuffer orig) {
    ByteBuffer dest = newByteBuffer(orig.remaining() * SIZEOF_FLOAT);
    orig.mark();
    dest.asFloatBuffer().put(orig);
    orig.reset();
    dest.rewind();
    return dest;
  }

  /** Copies the <i>remaining</i> elements (as defined by
      <code>limit() - position()</code>) in the passed IntBuffer into
      a newly-allocated direct ByteBuffer. The returned buffer will
      have its byte order set to the host platform's native byte
      order. The position of the newly-allocated buffer will be zero,
      and the position of the passed buffer is unchanged (though its
      mark is changed). */
  public static ByteBuffer copyIntBufferAsByteBuffer(IntBuffer orig) {
    ByteBuffer dest = newByteBuffer(orig.remaining() * SIZEOF_INT);
    orig.mark();
    dest.asIntBuffer().put(orig);
    orig.reset();
    dest.rewind();
    return dest;
  }

  /** Copies the <i>remaining</i> elements (as defined by
      <code>limit() - position()</code>) in the passed LongBuffer into
      a newly-allocated direct ByteBuffer. The returned buffer will
      have its byte order set to the host platform's native byte
      order. The position of the newly-allocated buffer will be zero,
      and the position of the passed buffer is unchanged (though its
      mark is changed). */
  public static ByteBuffer copyLongBufferAsByteBuffer(LongBuffer orig) {
    ByteBuffer dest = newByteBuffer(orig.remaining() * SIZEOF_LONG);
    orig.mark();
    dest.asLongBuffer().put(orig);
    orig.reset();
    dest.rewind();
    return dest;
  }

  /** Copies the <i>remaining</i> elements (as defined by
      <code>limit() - position()</code>) in the passed ShortBuffer
      into a newly-allocated direct ByteBuffer. The returned buffer
      will have its byte order set to the host platform's native byte
      order. The position of the newly-allocated buffer will be zero,
      and the position of the passed buffer is unchanged (though its
      mark is changed). */
  public static ByteBuffer copyShortBufferAsByteBuffer(ShortBuffer orig) {
    ByteBuffer dest = newByteBuffer(orig.remaining() * SIZEOF_SHORT);
    orig.mark();
    dest.asShortBuffer().put(orig);
    orig.reset();
    dest.rewind();
    return dest;
  }
}
