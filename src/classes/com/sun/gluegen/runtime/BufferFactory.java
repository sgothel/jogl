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

package com.sun.gluegen.runtime;

import java.nio.*;

public class BufferFactory {
  public static final int SIZEOF_BYTE = 1;
  public static final int SIZEOF_SHORT = 2;
  public static final int SIZEOF_INT = 4;
  public static final int SIZEOF_FLOAT = 4;
  public static final int SIZEOF_LONG = 8;
  public static final int SIZEOF_DOUBLE = 8;

  public static ByteBuffer newDirectByteBuffer(int size) {
    ByteBuffer buf = ByteBuffer.allocateDirect(size);
    buf.order(ByteOrder.nativeOrder());
    return buf;
  }

  /** Helper routine to tell whether a buffer is direct or not. Null
      pointers are considered direct. isDirect() should really be
      public in Buffer and not replicated in all subclasses. */
  public static boolean isDirect(Buffer buf) {
    if (buf == null) {
      return true;
    }
    if (buf instanceof ByteBuffer) {
      return ((ByteBuffer) buf).isDirect();
    } else if (buf instanceof FloatBuffer) {
      return ((FloatBuffer) buf).isDirect();
    } else if (buf instanceof DoubleBuffer) {
      return ((DoubleBuffer) buf).isDirect();
    } else if (buf instanceof CharBuffer) {
      return ((CharBuffer) buf).isDirect();
    } else if (buf instanceof ShortBuffer) {
      return ((ShortBuffer) buf).isDirect();
    } else if (buf instanceof IntBuffer) {
      return ((IntBuffer) buf).isDirect();
    } else if (buf instanceof LongBuffer) {
      return ((LongBuffer) buf).isDirect();
    }
    throw new RuntimeException("Unknown buffer type " + buf.getClass().getName());
  }


  /** Helper routine to get the Buffer byte offset by taking into
      account the Buffer position and the underlying type.  This is
      the total offset for Direct Buffers.  */

  public static int getDirectBufferByteOffset(Buffer buf) {
    if(buf == null) {
      return 0;
    }
    if(buf instanceof ByteBuffer) {
      return (buf.position());
    } else if (buf instanceof FloatBuffer) {
      return (buf.position() * SIZEOF_FLOAT);
    } else if (buf instanceof IntBuffer) {
      return (buf.position() * SIZEOF_INT);
    } else if (buf instanceof ShortBuffer) {
      return (buf.position() * SIZEOF_SHORT);
    } else if (buf instanceof DoubleBuffer) {
      return (buf.position() * SIZEOF_DOUBLE);
    } else if (buf instanceof LongBuffer) {
      return (buf.position() * SIZEOF_LONG);
    } 

    throw new RuntimeException("Disallowed array backing store type in buffer "
                               + buf.getClass().getName());
  }


  /** Helper routine to return the array backing store reference from
      a Buffer object.  */

   public static Object getArray(Buffer buf) {
     if (buf == null) {
       return null;
     }
     if(buf instanceof ByteBuffer) {
       return ((ByteBuffer) buf).array();
     } else if (buf instanceof FloatBuffer) {
       return ((FloatBuffer) buf).array();
     } else if (buf instanceof IntBuffer) {
       return ((IntBuffer) buf).array();
     } else if (buf instanceof ShortBuffer) { 
       return ((ShortBuffer) buf).array();
     } else if (buf instanceof DoubleBuffer) {
       return ((DoubleBuffer) buf).array();
     } else if (buf instanceof LongBuffer) {
       return ((LongBuffer) buf).array();
     }

     throw new RuntimeException("Disallowed array backing store type in buffer "
                                + buf.getClass().getName());
   } 


  /** Helper routine to get the full byte offset from the beginning of
      the array that is the storage for the indirect Buffer
      object.  The array offset also includes the position offset 
      within the buffer, in addition to any array offset. */

  public static int getIndirectBufferByteOffset(Buffer buf) {
    if(buf == null) {
      return 0;
    }
    int pos = buf.position();
    if(buf instanceof ByteBuffer) {
      return (((ByteBuffer)buf).arrayOffset() + pos);
    } else if(buf instanceof FloatBuffer) {
      return (SIZEOF_FLOAT*(((FloatBuffer)buf).arrayOffset() + pos));
    } else if(buf instanceof IntBuffer) {
      return (SIZEOF_INT*(((IntBuffer)buf).arrayOffset() + pos));
    } else if(buf instanceof ShortBuffer) {
      return (SIZEOF_SHORT*(((ShortBuffer)buf).arrayOffset() + pos));
    } else if(buf instanceof DoubleBuffer) {
      return (SIZEOF_DOUBLE*(((DoubleBuffer)buf).arrayOffset() + pos));
    } else if(buf instanceof LongBuffer) {
      return (SIZEOF_LONG*(((LongBuffer)buf).arrayOffset() + pos));
    } 

    throw new RuntimeException("Unknown buffer type " + buf.getClass().getName());
  }

  public static void rangeCheck(byte[] array, int offset, int minElementsRemaining) {
    if (array.length < offset + minElementsRemaining) {
      throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
    }
  }

  public static void rangeCheck(char[] array, int offset, int minElementsRemaining) {
    if (array.length < offset + minElementsRemaining) {
      throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
    }
  }

  public static void rangeCheck(short[] array, int offset, int minElementsRemaining) {
    if (array.length < offset + minElementsRemaining) {
      throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
    }
  }

  public static void rangeCheck(int[] array, int offset, int minElementsRemaining) {
    if (array.length < offset + minElementsRemaining) {
      throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
    }
  }

  public static void rangeCheck(long[] array, int offset, int minElementsRemaining) {
    if (array.length < offset + minElementsRemaining) {
      throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
    }
  }

  public static void rangeCheck(float[] array, int offset, int minElementsRemaining) {
    if (array.length < offset + minElementsRemaining) {
      throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
    }
  }

  public static void rangeCheck(double[] array, int offset, int minElementsRemaining) {
    if (array.length < offset + minElementsRemaining) {
      throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
    }
  }

  public static void rangeCheck(Buffer buffer, int minElementsRemaining) {
    if (buffer == null) {
      return;
    }

    if (buffer.remaining() < minElementsRemaining) {
      throw new IndexOutOfBoundsException("Required " + minElementsRemaining + " remaining elements in buffer, only had " + buffer.remaining());
    }
  }

  public static void rangeCheckBytes(Buffer buffer, int minBytesRemaining) {
    if (buffer == null) {
      return;
    }

    int elementsRemaining = buffer.remaining();
    int bytesRemaining = 0;
    if (buffer instanceof ByteBuffer) {
      bytesRemaining = elementsRemaining;
    } else if (buffer instanceof FloatBuffer) {
      bytesRemaining = elementsRemaining * SIZEOF_FLOAT;
    } else if (buffer instanceof IntBuffer) {
      bytesRemaining = elementsRemaining * SIZEOF_INT;
    } else if (buffer instanceof ShortBuffer) {
      bytesRemaining = elementsRemaining * SIZEOF_SHORT;
    } else if (buffer instanceof DoubleBuffer) {
      bytesRemaining = elementsRemaining * SIZEOF_DOUBLE;
    } else if (buffer instanceof LongBuffer) {
      bytesRemaining = elementsRemaining * SIZEOF_LONG;
    }
    if (bytesRemaining < minBytesRemaining) {
      throw new IndexOutOfBoundsException("Required " + minBytesRemaining + " remaining bytes in buffer, only had " + bytesRemaining);
    }
  }
}
