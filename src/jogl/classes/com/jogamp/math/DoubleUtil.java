/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
package com.jogamp.math;

/**
 * Basic Double math utility functions.
 */
public final class DoubleUtil {
  //
  // Scalar Ops
  //

  @SuppressWarnings("unused")
  private static void calculateMachineEpsilonDouble() {
      final long t0;
      double machEps = 1.0;
      int i=0;
      do {
          machEps /= 2.0;
          i++;
      } while (1.0 + (machEps / 2.0) != 1.0);
      machEpsilon = machEps;
  }
  private static volatile boolean machEpsilonAvail = false;
  private static double machEpsilon = 0f;

  /**
   * Return computed machine Epsilon value.
   * <p>
   * The machine Epsilon value is computed once.
   * </p>
   * @see #EPSILON
   */
  public static double getMachineEpsilon() {
      if( !machEpsilonAvail ) {
          synchronized(DoubleUtil.class) {
              if( !machEpsilonAvail ) {
                  machEpsilonAvail = true;
                  calculateMachineEpsilonDouble();
              }
          }
      }
      return machEpsilon;
  }

  public static final double E = 2.7182818284590452354;

  /** The value PI, i.e. 180 degrees in radians. */
  public static final double PI = 3.14159265358979323846;

  /** The value 2PI, i.e. 360 degrees in radians. */
  public static final double TWO_PI = 2.0 * PI;

  /** The value PI/2, i.e. 90 degrees in radians. */
  public static final double HALF_PI = PI / 2.0;

  /** The value PI/4, i.e. 45 degrees in radians. */
  public static final double QUARTER_PI = PI / 4.0;

  /** The value PI^2. */
  public final static double SQUARED_PI = PI * PI;

  /** Converts arc-degree to radians */
  public static double adegToRad(final double arc_degree) {
      return arc_degree * PI / 180.0;
  }

  /** Converts radians to arc-degree */
  public static double radToADeg(final double rad) {
      return rad * 180.0 / PI;
  }

  /**
   * Epsilon for floating point {@value}, as once computed via {@link #getMachineEpsilon()} on an AMD-64 CPU.
   * <p>
   * Definition of machine epsilon guarantees that:
   * <pre>
   *        1.0 + EPSILON != 1.0
   * </pre>
   * In other words: <i>machEps</i> is the maximum relative error of the chosen rounding procedure.
   * </p>
   * <p>
   * A number can be considered zero if it is in the range (or in the set):
   * <pre>
   *    <b>MaybeZeroSet</b> e ]-<i>machEps</i> .. <i>machEps</i>[  <i>(exclusive)</i>
   * </pre>
   * While comparing floating point values, <i>machEps</i> allows to clip the relative error:
   * <pre>
   *    boolean isZero    = afloat < EPSILON;
   *    boolean isNotZero = afloat >= EPSILON;
   *
   *    boolean isEqual    = abs(bfloat - afloat) < EPSILON;
   *    boolean isNotEqual = abs(bfloat - afloat) >= EPSILON;
   * </pre>
   * </p>
   * @see #isEqual(float, float, float)
   * @see #isZero(float, float)
   */
  public static final double EPSILON = 2.220446049250313E-16;

  /**
   * Inversion Epsilon, used with equals method to determine if two inverted matrices are close enough to be considered equal.
   * <p>
   * Using {@value}, which is ~100 times {@link DoubleUtil#EPSILON}.
   * </p>
   */
  public static final double INV_DEVIANCE = 1.0E-8f; // EPSILON == 1.1920929E-7f; double ALLOWED_DEVIANCE: 1.0E-8f

  /**
   * Return true if both values are equal w/o regarding an epsilon.
   * <p>
   * Implementation considers following corner cases:
   * <ul>
   *    <li>NaN == NaN</li>
   *    <li>+Inf == +Inf</li>
   *    <li>-Inf == -Inf</li>
   * </ul>
   * </p>
   * @see #isEqual(float, float, float)
   */
  public static boolean isEqualRaw(final double a, final double b) {
      // Values are equal (Inf, Nan .. )
      return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
  }

  /**
   * Returns true if both values are equal, i.e. their absolute delta < {@code epsilon} if 0 != {@code epsilon},
   * otherwise == {@code 0}.
   * <p>
   * {@code epsilon} is allowed to be {@code 0}.
   * </p>
   * <p>
   * Implementation considers following corner cases:
   * <ul>
   *    <li>NaN == NaN</li>
   *    <li>+Inf == +Inf</li>
   *    <li>-Inf == -Inf</li>
   * </ul>
   * </p>
   * @see #EPSILON
   */
  public static boolean isEqual(final double a, final double b, final double epsilon) {
      if( 0 == epsilon && Math.abs(a - b) == 0 ||
          0 != epsilon && Math.abs(a - b) < epsilon ) {
          return true;
      } else {
          // Values are equal (Inf, Nan .. )
          return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
      }
  }

  /**
   * Returns true if both values are equal, i.e. their absolute delta < {@link #EPSILON}.
   * <p>
   * Implementation considers following corner cases:
   * <ul>
   *    <li>NaN == NaN</li>
   *    <li>+Inf == +Inf</li>
   *    <li>-Inf == -Inf</li>
   * </ul>
   * </p>
   * @see #EPSILON
   */
  public static boolean isEqual(final double a, final double b) {
      if ( Math.abs(a - b) < EPSILON ) {
          return true;
      } else {
          // Values are equal (Inf, Nan .. )
          return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
      }
  }

  /**
   * Returns true if both values are equal, i.e. their absolute delta < {@link #EPSILON}.
   * <p>
   * Implementation does not consider corner cases like {@link #isEqual(float, float, float)}.
   * </p>
   * @see #EPSILON
   */
  public static boolean isEqual2(final double a, final double b) {
      return Math.abs(a - b) < EPSILON;
  }

  /**
   * Returns true if both values are equal w/o regarding an epsilon.
   * <p>
   * Implementation considers following corner cases:
   * <ul>
   *    <li>NaN == NaN</li>
   *    <li>+Inf == +Inf</li>
   *    <li>-Inf == -Inf</li>
   *    <li>NaN > 0</li>
   *    <li>+Inf > -Inf</li>
   * </ul>
   * </p>
   * @see #compare(float, float, float)
   */
  public static int compare(final double a, final double b) {
      if (a < b) {
          return -1; // Neither is NaN, a is smaller
      }
      if (a > b) {
          return 1;  // Neither is NaN, a is larger
      }
      final long aBits = Double.doubleToLongBits(a);
      final long bBits = Double.doubleToLongBits(b);
      if( aBits == bBits ) {
          return 0;  // Values are equal (Inf, Nan .. )
      } else if( aBits < bBits ) {
          return -1; // (-0.0,  0.0) or (!NaN,  NaN)
      } else {
          return 1;  // ( 0.0, -0.0) or ( NaN, !NaN)
      }
  }

  /**
   * Returns {@code -1}, {@code 0} or {@code 1} if {@code a} is less, equal or greater than {@code b},
   * taking {@code epsilon} into account for equality.
   * <p>
   * {@code epsilon} is allowed to be {@code 0}.
   * </p>
   * <p>
   * Implementation considers following corner cases:
   * <ul>
   *    <li>NaN == NaN</li>
   *    <li>+Inf == +Inf</li>
   *    <li>-Inf == -Inf</li>
   *    <li>NaN > 0</li>
   *    <li>+Inf > -Inf</li>
   * </ul>
   * </p>
   * @see #EPSILON
   */
  public static int compare(final double a, final double b, final double epsilon) {
      if( 0 == epsilon && Math.abs(a - b) == 0 ||
          0 != epsilon && Math.abs(a - b) < epsilon ) {
          return 0;
      } else {
          return compare(a, b);
      }
  }

  /**
   * Returns true if value is zero, i.e. it's absolute value < {@code epsilon} if 0 != {@code epsilon},
   * otherwise {@code 0 == a}.
   * <p>
   * {@code epsilon} is allowed to be {@code 0}.
   * </p>
   * <pre>
   *    return 0 == epsilon && 0 == a || 0 != epsilon && Math.abs(a) < epsilon
   * </pre>
   * @param a value to test
   * @param epsilon optional positive epsilon value, maybe {@code 0}
   * @see #EPSILON
   */
  public static boolean isZero(final double a, final double epsilon) {
      return 0 == epsilon && a == 0 ||
             0 != epsilon && Math.abs(a) < epsilon;
  }

  /**
   * Returns true if value is zero, i.e. it's absolute value < {@link #EPSILON}.
   * @see #EPSILON
   */
  public static boolean isZero(final double a) {
      return Math.abs(a) < EPSILON;
  }

}