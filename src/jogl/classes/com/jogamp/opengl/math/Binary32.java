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
 * Functions for interrogating <code>binary32</code> (float) values.
 */

public final class Binary32
{
  static final int NEGATIVE_ZERO_BITS;
  static final int MASK_SIGN;
  static final int MASK_EXPONENT;
  static final int MASK_SIGNIFICAND;
  static final int BIAS;

  static {
    NEGATIVE_ZERO_BITS = 0x80000000;
    MASK_SIGN = 0x80000000;
    MASK_EXPONENT = 0x7ff00000;
    MASK_SIGNIFICAND = 0x7fffff;
    BIAS = 127;
  }

  /**
   * <p>
   * Extract and unbias the exponent of the given packed <code>float</code>
   * value.
   * </p>
   * <p>
   * The exponent is encoded <i>biased</i> as a number in the range
   * <code>[0, 255]</code>, with <code>0</code> indicating that the number is
   * <i>subnormal</i> and <code>[1, 254]</code> denoting the actual exponent
   * plus {@link #BIAS}. Infinite and <code>NaN</code> values always have a
   * biased exponent of <code>255</code>.
   * </p>
   * <p>
   * This function will therefore return:
   * </p>
   * <ul>
   * <li>
   * <code>0 - {@link #BIAS} = -127</code> iff the input is a <i>subnormal</i>
   * number.</li>
   * <li>An integer in the range
   * <code>[1 - {@link #BIAS}, 254 - {@link #BIAS}] = [-126, 127]</code> iff
   * the input is a <i>normal</i> number.</li>
   * <li>
   * <code>255 - {@link #BIAS} = 128</code> iff the input is
   * {@link #POSITIVE_INFINITY}, {@link #NEGATIVE_INFINITY}, or
   * <code>NaN</code>.</li>
   * </ul>
   *
   * @see #packSetExponentUnbiasedUnchecked(int)
   */

  public static int unpackGetExponentUnbiased(
    final float d)
  {
    final int b = Float.floatToRawIntBits(d);
    final int em = b & Binary32.MASK_EXPONENT;
    final int es = em >> 23;
    return es - Binary32.BIAS;
  }

  /**
   * <p>
   * Return the sign of the given float value.
   * </p>
   */

  public static int unpackGetSign(
    final float d)
  {
    final int b = Float.floatToRawIntBits(d);
    return ((b & Binary32.MASK_SIGN) >> 31) & 1;
  }

  /**
   * <p>
   * Return the significand of the given float value.
   * </p>
   */

  public static int unpackGetSignificand(
    final float d)
  {
    final int b = Float.floatToRawIntBits(d);
    return b & Binary32.MASK_SIGNIFICAND;
  }
}
