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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

package net.java.games.gluegen;

/**
 * Convenience class containing the Class objects corresponding to arrays of
 * various types (e.g., {@link #booleanArrayClass} is the Class of Java type
 * "boolean[]").
 */
public class ArrayTypes {
  /** Class for Java type string[] */
  public static final Class stringArrayClass;
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

  /** Class for Java type string[][] */
  public static final Class stringArrayArrayClass;
  /** Class for Java type boolean[][] */
  public static final Class booleanArrayArrayClass;
  /** Class for Java type byte[][] */
  public static final Class byteArrayArrayClass;
  /** Class for Java type char[][] */
  public static final Class charArrayArrayClass;
  /** Class for Java type short[][] */
  public static final Class shortArrayArrayClass;
  /** Class for Java type int[][] */
  public static final Class intArrayArrayClass;
  /** Class for Java type long[][] */
  public static final Class longArrayArrayClass;
  /** Class for Java type float[][] */
  public static final Class floatArrayArrayClass;
  /** Class for Java type double[][] */
  public static final Class doubleArrayArrayClass;

  static {
    stringArrayClass  = new String [0].getClass();
    booleanArrayClass = new boolean[0].getClass();
    byteArrayClass    = new byte   [0].getClass();
    charArrayClass    = new char   [0].getClass();
    shortArrayClass   = new short  [0].getClass();
    intArrayClass     = new int    [0].getClass();
    longArrayClass    = new long   [0].getClass();
    floatArrayClass   = new float  [0].getClass();
    doubleArrayClass  = new double [0].getClass();

    stringArrayArrayClass  = new String [0][0].getClass();
    booleanArrayArrayClass = new boolean[0][0].getClass();
    byteArrayArrayClass    = new byte   [0][0].getClass();
    charArrayArrayClass    = new char   [0][0].getClass();
    shortArrayArrayClass   = new short  [0][0].getClass();
    intArrayArrayClass     = new int    [0][0].getClass();
    longArrayArrayClass    = new long   [0][0].getClass();
    floatArrayArrayClass   = new float  [0][0].getClass();
    doubleArrayArrayClass  = new double [0][0].getClass();
  }
}
