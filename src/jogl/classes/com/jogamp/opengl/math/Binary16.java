/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.math;

/**
 * <p>
 * Functions to convert values to/from the <code>binary16</code> format
 * specified in <code>IEEE 754 2008</code>.
 * </p>
 */

public final class Binary16
{
  /**
   * The encoded form of negative infinity <code>-∞</code>.
   */

  public static final char NEGATIVE_INFINITY;

  /**
   * The encoded form of positive infinity <code>∞</code>.
   */

  public static final char POSITIVE_INFINITY;

  /**
   * The encoded form of positive zero <code>0</code>.
   */

  public static final char POSITIVE_ZERO;

  /**
   * The encoded form of negative zero <code>-0</code>.
   */

  public static final char NEGATIVE_ZERO;

  /**
   * The <i>bias</i> value used to offset the encoded exponent. A given
   * exponent <code>e</code> is encoded as <code>{@link #BIAS} + e</code>.
   */

  public static final int  BIAS;

  static {
    NEGATIVE_INFINITY = 0xFC00;
    POSITIVE_INFINITY = 0x7C00;
    POSITIVE_ZERO = 0x0000;
    NEGATIVE_ZERO = 0x8000;
    BIAS = 15;
  }

  private static final int MASK_SIGN;
  private static final int MASK_EXPONENT;
  private static final int MASK_SIGNIFICAND;

  static {
    MASK_SIGN = 0x8000;
    MASK_EXPONENT = 0x7C00;
    MASK_SIGNIFICAND = 0x03FF;
  }

  /**
   * One possible not-a-number value.
   */

  public static char exampleNaN()
  {
    final int n =
      Binary16.packSetExponentUnbiasedUnchecked(16)
        | Binary16.packSetSignificandUnchecked(1);
    final char c = (char) n;
    return c;
  }

  /**
   * Return <code>true</code> if the given packed <code>binary16</code> value
   * is infinite.
   */

  public static boolean isInfinite(
    final char k)
  {
    if (Binary16.unpackGetExponentUnbiased(k) == 16) {
      if (Binary16.unpackGetSignificand(k) == 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return <code>true</code> if the given packed <code>binary16</code> value
   * is not a number (<code>NaN</code>).
   */

  public static boolean isNaN(
    final char k)
  {
    final int e = Binary16.unpackGetExponentUnbiased(k);
    final int s = Binary16.unpackGetSignificand(k);
    return (e == 16) && (s > 0);
  }

  /**
   * <p>
   * Convert a double precision floating point value to a packed
   * <code>binary16</code> value.
   * </p>
   * <p>
   * For the following specific cases, the function returns:
   * </p>
   * <ul>
   * <li><code>NaN</code> iff <code>isNaN(k)</code></li>
   * <li>{@link #POSITIVE_INFINITY} iff
   * <code>k == {@link Double#POSITIVE_INFINITY}</code></li>
   * <li>{@link #NEGATIVE_INFINITY} iff
   * <code>k == {@link Double#NEGATIVE_INFINITY}</code></li>
   * <li>{@link #NEGATIVE_ZERO} iff <code>k == -0.0</code></li>
   * <li>{@link #POSITIVE_ZERO} iff <code>k == 0.0</code></li>
   * </ul>
   * <p>
   * Otherwise, the <code>binary16</code> value that most closely represents
   * <code>k</code> is returned. This may obviously be an infinite value as
   * the interval of double precision values is far larger than that of the
   * <code>binary16</code> type.
   * </p>
   *
   * @see #unpackDouble(char)
   */

  public static char packDouble(
    final double k)
  {
    if (Double.isNaN(k)) {
      return Binary16.exampleNaN();
    }
    if (k == Double.POSITIVE_INFINITY) {
      return Binary16.POSITIVE_INFINITY;
    }
    if (k == Double.NEGATIVE_INFINITY) {
      return Binary16.NEGATIVE_INFINITY;
    }
    if (Double.doubleToLongBits(k) == Binary64.NEGATIVE_ZERO_BITS) {
      return Binary16.NEGATIVE_ZERO;
    }
    if (k == 0.0) {
      return Binary16.POSITIVE_ZERO;
    }

    final long de = Binary64.unpackGetExponentUnbiased(k);
    final long ds = Binary64.unpackGetSign(k);
    final long dn = Binary64.unpackGetSignificand(k);
    final char rsr = Binary16.packSetSignUnchecked((int) ds);

    /**
     * Extract the 5 least-significant bits of the exponent.
     */

    final int rem = (int) (de & 0x001F);
    final char rer = Binary16.packSetExponentUnbiasedUnchecked(rem);

    /**
     * Extract the 10 most-significant bits of the significand.
     */

    final long rnm = dn & 0xFFC0000000000L;
    final long rns = rnm >> 42;
    final char rnr = Binary16.packSetSignificandUnchecked((int) rns);

    /**
     * Combine the results.
     */

    return (char) (rsr | rer | rnr);
  }

  /**
   * <p>
   * Convert a single precision floating point value to a packed
   * <code>binary16</code> value.
   * </p>
   * <p>
   * For the following specific cases, the function returns:
   * </p>
   * <ul>
   * <li><code>NaN</code> iff <code>isNaN(k)</code></li>
   * <li>{@link #POSITIVE_INFINITY} iff
   * <code>k == {@link Float#POSITIVE_INFINITY}</code></li>
   * <li>{@link #NEGATIVE_INFINITY} iff
   * <code>k == {@link Float#NEGATIVE_INFINITY}</code></li>
   * <li>{@link #NEGATIVE_ZERO} iff <code>k == -0.0</code></li>
   * <li>{@link #POSITIVE_ZERO} iff <code>k == 0.0</code></li>
   * </ul>
   * <p>
   * Otherwise, the <code>binary16</code> value that most closely represents
   * <code>k</code> is returned. This may obviously be an infinite value as
   * the interval of single precision values is far larger than that of the
   * <code>binary16</code> type.
   * </p>
   *
   * @see #unpackFloat(char)
   */

  public static char packFloat(
    final float k)
  {
    if (Float.isNaN(k)) {
      return Binary16.exampleNaN();
    }
    if (k == Float.POSITIVE_INFINITY) {
      return Binary16.POSITIVE_INFINITY;
    }
    if (k == Float.NEGATIVE_INFINITY) {
      return Binary16.NEGATIVE_INFINITY;
    }
    if (Float.floatToIntBits(k) == Binary32.NEGATIVE_ZERO_BITS) {
      return Binary16.NEGATIVE_ZERO;
    }
    if (k == 0.0) {
      return Binary16.POSITIVE_ZERO;
    }

    final long de = Binary32.unpackGetExponentUnbiased(k);
    final long ds = Binary32.unpackGetSign(k);
    final long dn = Binary32.unpackGetSignificand(k);
    final char rsr = Binary16.packSetSignUnchecked((int) ds);

    /**
     * Extract the 5 least-significant bits of the exponent.
     */

    final int rem = (int) (de & 0x001F);
    final char rer = Binary16.packSetExponentUnbiasedUnchecked(rem);

    /**
     * Extract the 10 most-significant bits of the significand.
     */

    final long rnm = dn & 0x7FE000L;
    final long rns = rnm >> 13;
    final char rnr = Binary16.packSetSignificandUnchecked((int) rns);

    /**
     * Combine the results.
     */

    return (char) (rsr | rer | rnr);
  }

  /**
   * <p>
   * Encode the unbiased exponent <code>e</code>. Values should be in the
   * range <code>[-15, 16]</code> - values outside of this range will be
   * truncated.
   * </p>
   *
   * @see #unpackGetExponentUnbiased(char)
   */

  public static char packSetExponentUnbiasedUnchecked(
    final int e)
  {
    final int eb = e + Binary16.BIAS;
    final int es = eb << 10;
    final int em = es & Binary16.MASK_EXPONENT;
    return (char) em;
  }

  /**
   * <p>
   * Encode the significand <code>s</code>. Values should be in the range
   * <code>[0, 1023]</code>. Values outside of this range will be truncated.
   * </p>
   *
   * @see #unpackGetSignificand(char)
   */

  public static char packSetSignificandUnchecked(
    final int s)
  {
    final int sm = s & Binary16.MASK_SIGNIFICAND;
    return (char) sm;
  }

  /**
   * <p>
   * Encode the sign bit <code>s</code>. Values should be in the range
   * <code>[0, 1]</code>, with <code>0</code> ironically denoting a positive
   * value. Values outside of this range will be truncated.
   * </p>
   *
   * @see #unpackGetSign(char)
   */

  public static char packSetSignUnchecked(
    final int s)
  {
    final int ss = s << 15;
    final int sm = ss & Binary16.MASK_SIGN;
    return (char) sm;
  }

  /**
   * Show the given raw packed <code>binary16</code> value as a string of
   * binary digits.
   */

  public static String toRawBinaryString(
    final char k)
  {
    final StringBuilder b = new StringBuilder();
    int z = k;
    for (int i = 0; i < 16; ++i) {
      if ((z & 1) == 1) {
        b.insert(0, "1");
      } else {
        b.insert(0, "0");
      }
      z >>= 1;
    }
    return b.toString();
  }

  /**
   * <p>
   * Convert a packed <code>binary16</code> value <code>k</code> to a
   * double-precision floating point value.
   * </p>
   * <p>
   * The function returns:
   * </p>
   * <ul>
   * <li><code>NaN</code> iff <code>isNaN(k)</code></li>
   * <li>{@link Double#POSITIVE_INFINITY} iff
   * <code>k == {@link #POSITIVE_INFINITY}</code></li>
   * <li>{@link Double#NEGATIVE_INFINITY} iff
   * <code>k == {@link #NEGATIVE_INFINITY}</code></li>
   * <li><code>-0.0</code> iff <code>k == {@link #NEGATIVE_ZERO}</code></li>
   * <li><code>0.0</code> iff <code>k == {@link #POSITIVE_ZERO}</code></li>
   * <li><code>(-1.0 * n) * (2 ^ e) * 1.s</code>, for the decoded sign
   * <code>n</code> of <code>k</code>, the decoded exponent <code>e</code> of
   * <code>k</code>, and the decoded significand <code>s</code> of
   * <code>k</code>.</li>
   * </ul>
   *
   * @see #packDouble(double)
   */

  public static double unpackDouble(
    final char k)
  {
    if (Binary16.isNaN(k)) {
      return Double.NaN;
    }
    if (k == Binary16.POSITIVE_INFINITY) {
      return Double.POSITIVE_INFINITY;
    }
    if (k == Binary16.NEGATIVE_INFINITY) {
      return Double.NEGATIVE_INFINITY;
    }
    if (k == Binary16.NEGATIVE_ZERO) {
      return -0.0;
    }
    if (k == Binary16.POSITIVE_ZERO) {
      return 0.0;
    }

    final long e = Binary16.unpackGetExponentUnbiased(k);
    final long s = Binary16.unpackGetSign(k);
    final long n = Binary16.unpackGetSignificand(k);

    /**
     * Shift the sign bit to the position at which it will appear in the
     * resulting value.
     */

    final long rsr = s << 63;

    /**
     * 1. Bias the exponent.
     *
     * 2. Shift the result left to the position at which it will appear in the
     * resulting value.
     */

    final long reb = (e + Binary64.BIAS);
    final long rer = reb << 52;

    /**
     * Shift the significand left to the position at which it will appear in
     * the resulting value.
     */

    final long rnr = n << 42;
    return Double.longBitsToDouble(rsr | rer | rnr);
  }

  /**
   * <p>
   * Convert a packed <code>binary16</code> value <code>k</code> to a
   * single-precision floating point value.
   * </p>
   * <p>
   * The function returns:
   * </p>
   * <ul>
   * <li><code>NaN</code> iff <code>isNaN(k)</code></li>
   * <li>{@link Float#POSITIVE_INFINITY} iff
   * <code>k == {@link #POSITIVE_INFINITY}</code></li>
   * <li>{@link Float#NEGATIVE_INFINITY} iff
   * <code>k == {@link #NEGATIVE_INFINITY}</code></li>
   * <li><code>-0.0</code> iff <code>k == {@link #NEGATIVE_ZERO}</code></li>
   * <li><code>0.0</code> iff <code>k == {@link #POSITIVE_ZERO}</code></li>
   * <li><code>(-1.0 * n) * (2 ^ e) * 1.s</code>, for the decoded sign
   * <code>n</code> of <code>k</code>, the decoded exponent <code>e</code> of
   * <code>k</code>, and the decoded significand <code>s</code> of
   * <code>k</code>.</li>
   * </ul>
   *
   * @see #packFloat(float)
   */

  public static float unpackFloat(
    final char k)
  {
    if (Binary16.isNaN(k)) {
      return Float.NaN;
    }
    if (k == Binary16.POSITIVE_INFINITY) {
      return Float.POSITIVE_INFINITY;
    }
    if (k == Binary16.NEGATIVE_INFINITY) {
      return Float.NEGATIVE_INFINITY;
    }
    if (k == Binary16.NEGATIVE_ZERO) {
      return -0.0f;
    }
    if (k == Binary16.POSITIVE_ZERO) {
      return 0.0f;
    }

    final int e = Binary16.unpackGetExponentUnbiased(k);
    final int s = Binary16.unpackGetSign(k);
    final int n = Binary16.unpackGetSignificand(k);

    /**
     * Shift the sign bit to the position at which it will appear in the
     * resulting value.
     */

    final int rsr = s << 31;

    /**
     * 1. Bias the exponent.
     *
     * 2. Shift the result left to the position at which it will appear in the
     * resulting value.
     */

    final int reb = (e + Binary32.BIAS);
    final int rer = reb << 23;

    /**
     * Shift the significand left to the position at which it will appear in
     * the resulting value.
     */

    final int rnr = n << 13;
    return Float.intBitsToFloat(rsr | rer | rnr);
  }

  /**
   * <p>
   * Extract and unbias the exponent of the given packed <code>binary16</code>
   * value.
   * </p>
   * <p>
   * The exponent is encoded <i>biased</i> as a number in the range
   * <code>[0, 31]</code>, with <code>0</code> indicating that the number is
   * <i>subnormal</i> and <code>[1, 30]</code> denoting the actual exponent
   * plus {@link #BIAS}. Infinite and <code>NaN</code> values always have an
   * exponent of <code>31</code>.
   * </p>
   * <p>
   * This function will therefore return:
   * </p>
   * <ul>
   * <li>
   * <code>0 - {@link #BIAS} = -15</code> iff the input is a <i>subnormal</i>
   * number.</li>
   * <li>An integer in the range
   * <code>[1 - {@link #BIAS}, 30 - {@link #BIAS}] = [-14, 15]</code> iff the
   * input is a <i>normal</i> number.</li>
   * <li>
   * <code>16</code> iff the input is {@link #POSITIVE_INFINITY},
   * {@link #NEGATIVE_INFINITY}, or <code>NaN</code>.</li>
   * </ul>
   *
   * @see #packSetExponentUnbiasedUnchecked(int)
   */

  public static int unpackGetExponentUnbiased(
    final char k)
  {
    final int em = k & Binary16.MASK_EXPONENT;
    final int es = em >> 10;
    return es - Binary16.BIAS;
  }

  /**
   * Retrieve the sign bit of the given packed <code>binary16</code> value, as
   * an integer in the range <code>[0, 1]</code>.
   *
   * @see Binary16#packSetSignUnchecked(int)
   */

  public static int unpackGetSign(
    final char k)
  {
    return (k & Binary16.MASK_SIGN) >> 15;
  }

  /**
   * <p>
   * Return the significand of the given packed <code>binary16</code> value as
   * an integer in the range <code>[0, 1023]</code>.
   * </p>
   *
   * @see Binary16#packSetSignificandUnchecked(int)
   */

  public static int unpackGetSignificand(
    final char k)
  {
    return k & Binary16.MASK_SIGNIFICAND;
  }

  private Binary16()
  {
    throw new AssertionError("Unreachable code, report this bug!");
  }
}
