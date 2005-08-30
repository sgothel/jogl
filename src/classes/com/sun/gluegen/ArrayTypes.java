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

package com.sun.gluegen;

import java.nio.*;

/**
 * Convenience class containing the Class objects corresponding to arrays of
 * various types (e.g., {@link #booleanArrayClass} is the Class of Java type
 * "boolean[]").
 */
public class ArrayTypes {
  /** Class for Java type boolean[] */
  public static final Class booleanArrayClass;
  /** Class for Java type byte[] */
  public static final Class byteArrayClass;
  /** Class for Java type char[] */
  public static final Class charArrayClass;
  /** Class for Java type short[] */
  public static final Class shortArrayClass;
  /** Class for Java type int[] */
  public static final Class intArrayClass;
  /** Class for Java type long[] */
  public static final Class longArrayClass;
  /** Class for Java type float[] */
  public static final Class floatArrayClass;
  /** Class for Java type double[] */
  public static final Class doubleArrayClass;
  /** Class for Java type String[] */
  public static final Class stringArrayClass;

  // Classes for two-dimensional arrays.
  //
  // GlueGen converts C types like int** into Java arrays of direct
  // buffers of the appropriate type (like IntBuffer[]). If the tool
  // supported conversions like byte[][] -> char**, it would
  // effectively be necessary to copy all of the data from the Java
  // heap to the C heap during each call. The reason for this is that
  // if we wanted to use GetPrimitiveArrayCritical to lock down the
  // storage for each individual array element, we would need to fetch
  // each element of the two-dimensional Java array into temporary
  // storage before making the first GetPrimitiveArrayCritical call,
  // since one can not call GetObjectArrayElement inside a Get /
  // ReleasePrimitiveArrayCritical pair. This means that we would need
  // two top-level pieces of temporary storage for the two-dimensional
  // array as well as two loops to set up the contents, which would be
  // too complicated.
  //
  // The one concession we make is converting String[] -> char**. The
  // JVM takes care of the C heap allocation for GetStringUTFChars and
  // ReleaseStringUTFChars, and this conversion is important for
  // certain OpenGL operations.

  /** Class for Java type Buffer[] */
  public static final Class bufferArrayClass;
  /** Class for Java type ByteBuffer[] */
  public static final Class byteBufferArrayClass;
  /** Class for Java type ShortBuffer[] */
  public static final Class shortBufferArrayClass;
  /** Class for Java type IntBuffer[] */
  public static final Class intBufferArrayClass;
  /** Class for Java type LongBuffer[] */
  public static final Class longBufferArrayClass;
  /** Class for Java type FloatBuffer[] */
  public static final Class floatBufferArrayClass;
  /** Class for Java type DoubleBuffer[] */
  public static final Class doubleBufferArrayClass;

  static {
    booleanArrayClass = new boolean[0].getClass();
    byteArrayClass    = new byte   [0].getClass();
    charArrayClass    = new char   [0].getClass();
    shortArrayClass   = new short  [0].getClass();
    intArrayClass     = new int    [0].getClass();
    longArrayClass    = new long   [0].getClass();
    floatArrayClass   = new float  [0].getClass();
    doubleArrayClass  = new double [0].getClass();
    stringArrayClass  = new String [0].getClass();

    bufferArrayClass       = new Buffer      [0].getClass();
    byteBufferArrayClass   = new ByteBuffer  [0].getClass();
    shortBufferArrayClass  = new ShortBuffer [0].getClass();
    intBufferArrayClass    = new IntBuffer   [0].getClass();
    longBufferArrayClass   = new LongBuffer  [0].getClass();
    floatBufferArrayClass  = new FloatBuffer [0].getClass();
    doubleBufferArrayClass = new DoubleBuffer[0].getClass();
  }
}
