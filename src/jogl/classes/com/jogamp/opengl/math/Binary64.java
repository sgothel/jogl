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
 * Functions for interrogating <code>binary64</code> (double) values.
 */

public final class Binary64
{
  static final long NEGATIVE_ZERO_BITS;
  static final long MASK_SIGN;
  static final long MASK_EXPONENT;
  static final long MASK_SIGNIFICAND;
  static final long BIAS;

  static {
    NEGATIVE_ZERO_BITS = 0x8000000000000000L;
    MASK_SIGN = 0x8000000000000000L;
    MASK_EXPONENT = 0x7ff0000000000000L;
    MASK_SIGNIFICAND = 0x000fffffffffffffL;
    BIAS = 1023;
  }

  /**
   * <p>
   * Extract and unbias the exponent of the given packed <code>double</code>
   * value.
   * </p>
   * <p>
   * The exponent is encoded <i>biased</i> as a number in the range
   * <code>[0, 2047]</code>, with <code>0</code> indicating that the number is
   * <i>subnormal</i> and <code>[1, 2046]</code> denoting the actual exponent
   * plus {@link #BIAS}. Infinite and <code>NaN</code> values always have a
   * biased exponent of <code>2047</code>.
   * </p>
   * <p>
   * This function will therefore return:
   * </p>
   * <ul>
   * <li>
   * <code>0 - {@link #BIAS} = -1023</code> iff the input is a
   * <i>subnormal</i> number.</li>
   * <li>An integer in the range
   * <code>[1 - {@link #BIAS}, 2046 - {@link #BIAS}] = [-1022, 1023]</code>
   * iff the input is a <i>normal</i> number.</li>
   * <li>
   * <code>2047 - {@link #BIAS} = 1024</code> iff the input is
   * {@link #POSITIVE_INFINITY}, {@link #NEGATIVE_INFINITY}, or
   * <code>NaN</code>.</li>
   * </ul>
   *
   * @see #packSetExponentUnbiasedUnchecked(int)
   */

  public static long unpackGetExponentUnbiased(
    final double d)
  {
    final long b = Double.doubleToRawLongBits(d);
    final long em = b & Binary64.MASK_EXPONENT;
    final long es = em >> 52;
    return es - Binary64.BIAS;
  }

  /**
   * <p>
   * Return the significand of the given double value.
   * </p>
   */

  public static long unpackGetSignificand(
    final double d)
  {
    final long b = Double.doubleToRawLongBits(d);
    return b & Binary64.MASK_SIGNIFICAND;
  }

  /**
   * <p>
   * Return the sign of the given double value.
   * </p>
   */

  public static long unpackGetSign(
    final double d)
  {
    final long b = Double.doubleToRawLongBits(d);
    return ((b & Binary64.MASK_SIGN) >> 63) & 1;
  }
}
