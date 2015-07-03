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

package com.jogamp.opengl.test.junit.jogl.math;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.math.Binary16;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class TestBinary16NOUI extends UITestCase  /* due to hardship on machine, we want to run this test exclusively! */
{
  static int stepping = 1;
  static boolean verbose = false;

  /**
   * Exponents in the range [-15, 16] are encoded and decoded correctly.
   */

  @SuppressWarnings("static-method") @Test public void testExponentIdentity()
  {
    System.out.println("-- Exponent identities");
    for (int e = -15; e <= 16; ++e) {
      final char p = Binary16.packSetExponentUnbiasedUnchecked(e);
      final int u = Binary16.unpackGetExponentUnbiased(p);
      if( verbose ) {
          System.out.println("e: " + e +", p: "+Integer.toHexString(p)+", u: "+u);
      }
      Assert.assertEquals(e, u);
    }
  }

  /**
   * Infinities are infinite.
   */

  @SuppressWarnings("static-method") @Test public void testInfinite()
  {
    Assert.assertTrue(Binary16.isInfinite(Binary16.POSITIVE_INFINITY));
    Assert.assertTrue(Binary16.isInfinite(Binary16.NEGATIVE_INFINITY));
    Assert.assertFalse(Binary16.isInfinite(Binary16.exampleNaN()));

    for (int i = 0; i <= 65535; i+=stepping) {
      Assert.assertFalse(Binary16.isInfinite(Binary16.packDouble(i)));
    }
  }

  /**
   * The unencoded exponent of infinity is 16.
   */

  @SuppressWarnings("static-method") @Test public void testInfinityExponent()
  {
    Assert.assertEquals(
      16,
      Binary16.unpackGetExponentUnbiased(Binary16.POSITIVE_INFINITY));
  }

  /**
   * The unencoded exponent of infinity is 16.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testInfinityNegativeExponent()
  {
    Assert.assertEquals(
      16,
      Binary16.unpackGetExponentUnbiased(Binary16.NEGATIVE_INFINITY));
  }

  /**
   * The sign of negative infinity is 1.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testInfinityNegativeSign()
  {
    Assert
      .assertEquals(1, Binary16.unpackGetSign(Binary16.NEGATIVE_INFINITY));
  }

  /**
   * The significand of infinity is 0.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testInfinityNegativeSignificand()
  {
    Assert.assertEquals(
      0,
      Binary16.unpackGetSignificand(Binary16.NEGATIVE_INFINITY));
  }

  /**
   * The sign of positive infinity is 0.
   */

  @SuppressWarnings("static-method") @Test public void testInfinitySign()
  {
    Assert
      .assertEquals(0, Binary16.unpackGetSign(Binary16.POSITIVE_INFINITY));
  }

  /**
   * The significand of infinity is 0.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testInfinitySignificand()
  {
    Assert.assertEquals(
      0,
      Binary16.unpackGetSignificand(Binary16.POSITIVE_INFINITY));
  }

  /**
   * NaN is NaN.
   */

  @SuppressWarnings("static-method") @Test public void testNaN()
  {
    final int n =
      Binary16.packSetExponentUnbiasedUnchecked(16)
        | Binary16.packSetSignificandUnchecked(1);
    final char c = (char) n;
    Assert.assertEquals(16, Binary16.unpackGetExponentUnbiased(c));
    Assert.assertEquals(1, Binary16.unpackGetSignificand(c));
    Assert.assertEquals(
      16,
      Binary16.unpackGetExponentUnbiased(Binary16.exampleNaN()));
    Assert.assertEquals(
      1,
      Binary16.unpackGetSignificand(Binary16.exampleNaN()));
    Assert.assertTrue(Binary16.isNaN(c));
    Assert.assertTrue(Binary16.isNaN(Binary16.exampleNaN()));
  }

  /**
   * Packing NaN results in NaN.
   */

  @SuppressWarnings("static-method") @Test public void testPackDoubleNaN()
  {
    final double k = Double.NaN;
    final char r = Binary16.packDouble(k);
    Assert.assertTrue(Binary16.isNaN(r));
  }

  /**
   * Packing negative infinity results in negative infinity.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testPackDoubleNegativeInfinity()
  {
    Assert.assertTrue(Binary16.NEGATIVE_INFINITY == Binary16
      .packDouble(Double.NEGATIVE_INFINITY));
  }

  /**
   * Packing negative zero results in negative zero.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testPackDoubleNegativeZero()
  {
    Assert.assertTrue(Binary16.NEGATIVE_ZERO == Binary16.packDouble(-0.0));
  }

  /**
   * Packing positive infinity results in positive infinity.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testPackDoublePositiveInfinity()
  {
    Assert.assertTrue(Binary16.POSITIVE_INFINITY == Binary16
      .packDouble(Double.POSITIVE_INFINITY));
  }

  /**
   * Packing positive zero results in positive zero.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testPackDoublePositiveZero()
  {
    Assert.assertTrue(Binary16.POSITIVE_ZERO == Binary16.packDouble(0.0));
  }

  /**
   * Integers in the range [0, 65520] should be representable.
   */

  @SuppressWarnings({ "static-method", "boxing" }) @Test public
    void
    testPackDoubleUnpackFloat()
  {
    for (int i = 0; i <= 65536; i+=stepping) {
      final double in = i;
      final char packed = Binary16.packDouble(in);
      final float r = Binary16.unpackFloat(packed);
      if( verbose ) {
          System.out.println(String.format(
            "packed: 0x%04x 0b%s in: %f unpacked: %f",
            (int) packed,
            Binary16.toRawBinaryString(packed),
            in,
            r));
      }

      if (i <= 2048) {
        Assert.assertEquals(in, r, 0.0);
      }
      if ((i > 2048) && (i <= 4096)) {
        Assert.assertTrue((r % 2) == 0);
      }
      if ((i > 4096) && (i <= 8192)) {
        Assert.assertTrue((r % 4) == 0);
      }
      if ((i > 8192) && (i <= 16384)) {
        Assert.assertTrue((r % 8) == 0);
      }
      if ((i > 16384) && (i <= 32768)) {
        Assert.assertTrue((r % 16) == 0);
      }
      if ((i > 32768) && (i < 65536)) {
        Assert.assertTrue((r % 32) == 0);
      }
      if (i == 65536) {
        Assert.assertTrue(Double.isInfinite(r));
      }
    }
  }

  /**
   * Integers in the range [0, 65520] should be representable.
   */

  @SuppressWarnings({ "static-method", "boxing" }) @Test public
    void
    testPackFloatDoubleEquivalent()
  {
    for (int i = 0; i <= 65536; i+=stepping) {
      final float f_in = i;
      final double d_in = i;
      final char pf = Binary16.packFloat(f_in);
      final char pd = Binary16.packDouble(d_in);

      if( verbose ) {
          System.out.println("i: " + i);
          System.out.println(String.format(
            "pack_f: 0x%04x 0b%s",
            (int) pf,
            Binary16.toRawBinaryString(pf)));
          System.out.println(String.format(
            "pack_d: 0x%04x 0b%s",
            (int) pd,
            Binary16.toRawBinaryString(pd)));
      }

      Assert.assertEquals(pf, pd);
    }
  }

  /**
   * Packing NaN results in NaN.
   */

  @SuppressWarnings("static-method") @Test public void testPackFloatNaN()
  {
    final float k = Float.NaN;
    final char r = Binary16.packFloat(k);
    Assert.assertTrue(Binary16.isNaN(r));
  }

  /**
   * Packing negative infinity results in negative infinity.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testPackFloatNegativeInfinity()
  {
    Assert.assertTrue(Binary16.NEGATIVE_INFINITY == Binary16
      .packFloat(Float.NEGATIVE_INFINITY));
  }

  /**
   * Packing negative zero results in negative zero.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testPackFloatNegativeZero()
  {
    Assert.assertTrue(Binary16.NEGATIVE_ZERO == Binary16.packFloat(-0.0f));
  }

  /**
   * Packing positive infinity results in positive infinity.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testPackFloatPositiveInfinity()
  {
    Assert.assertTrue(Binary16.POSITIVE_INFINITY == Binary16
      .packFloat(Float.POSITIVE_INFINITY));
  }

  /**
   * Packing positive zero results in positive zero.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testPackFloatPositiveZero()
  {
    Assert.assertTrue(Binary16.POSITIVE_ZERO == Binary16.packFloat(0.0f));
  }

  /**
   * Integers in the range [0, 65520] should be representable.
   */

  @SuppressWarnings({ "static-method", "boxing" }) @Test public
    void
    testPackFloatUnpackDouble()
  {
    for (int i = 0; i <= 65536; i+=stepping) {
      final float in = i;
      final char packed = Binary16.packFloat(in);
      final double r = Binary16.unpackDouble(packed);
      if( verbose ) {
          System.out.println(String.format(
            "packed: 0x%04x 0b%s in: %f unpacked: %f",
            (int) packed,
            Binary16.toRawBinaryString(packed),
            in,
            r));
      }

      if (i <= 2048) {
        Assert.assertEquals(in, r, 0.0);
      }
      if ((i > 2048) && (i <= 4096)) {
        Assert.assertTrue((r % 2) == 0);
      }
      if ((i > 4096) && (i <= 8192)) {
        Assert.assertTrue((r % 4) == 0);
      }
      if ((i > 8192) && (i <= 16384)) {
        Assert.assertTrue((r % 8) == 0);
      }
      if ((i > 16384) && (i <= 32768)) {
        Assert.assertTrue((r % 16) == 0);
      }
      if ((i > 32768) && (i < 65536)) {
        Assert.assertTrue((r % 32) == 0);
      }
      if (i == 65536) {
        Assert.assertTrue(Double.isInfinite(r));
      }
    }
  }

  /**
   * Integers in the range [0, 65520] should be representable.
   */

  @SuppressWarnings({ "static-method", "boxing" }) @Test public
    void
    testPackUnpackDouble()
  {
    for (int i = 0; i <= 65536; i+=stepping) {
      final double in = i;
      final char packed = Binary16.packDouble(in);
      final double r = Binary16.unpackDouble(packed);
      if( verbose ) {
          System.out.println(String.format(
            "packed: 0x%04x 0b%s in: %f unpacked: %f",
            (int) packed,
            Binary16.toRawBinaryString(packed),
            in,
            r));
      }

      if (i <= 2048) {
        Assert.assertEquals(in, r, 0.0);
      }
      if ((i > 2048) && (i <= 4096)) {
        Assert.assertTrue((r % 2) == 0);
      }
      if ((i > 4096) && (i <= 8192)) {
        Assert.assertTrue((r % 4) == 0);
      }
      if ((i > 8192) && (i <= 16384)) {
        Assert.assertTrue((r % 8) == 0);
      }
      if ((i > 16384) && (i <= 32768)) {
        Assert.assertTrue((r % 16) == 0);
      }
      if ((i > 32768) && (i < 65536)) {
        Assert.assertTrue((r % 32) == 0);
      }
      if (i == 65536) {
        Assert.assertTrue(Double.isInfinite(r));
      }
    }
  }

  /**
   * Integers in the range [0, 65520] should be representable.
   */

  @SuppressWarnings({ "static-method", "boxing" }) @Test public
    void
    testPackUnpackFloat()
  {
    for (int i = 0; i <= 65536; i+=stepping) {
      final float in = i;
      final char packed = Binary16.packFloat(in);
      final float r = Binary16.unpackFloat(packed);
      if( verbose ) {
          System.out.println(String.format(
            "packed: 0x%04x 0b%s in: %f unpacked: %f",
            (int) packed,
            Binary16.toRawBinaryString(packed),
            in,
            r));
      }
      if (i <= 2048) {
        Assert.assertEquals(in, r, 0.0);
      }
      if ((i > 2048) && (i <= 4096)) {
        Assert.assertTrue((r % 2) == 0);
      }
      if ((i > 4096) && (i <= 8192)) {
        Assert.assertTrue((r % 4) == 0);
      }
      if ((i > 8192) && (i <= 16384)) {
        Assert.assertTrue((r % 8) == 0);
      }
      if ((i > 16384) && (i <= 32768)) {
        Assert.assertTrue((r % 16) == 0);
      }
      if ((i > 32768) && (i < 65536)) {
        Assert.assertTrue((r % 32) == 0);
      }
      if (i == 65536) {
        Assert.assertTrue(Float.isInfinite(r));
      }
    }
  }

  /**
   * Signs in the range [0, 1] are encoded and decoded correctly.
   */

  @SuppressWarnings("static-method") @Test public void testSignIdentity()
  {
    System.out.println("-- Sign identities");
    for (int e = 0; e <= 1; ++e) {
      final char p = Binary16.packSetSignUnchecked(e);
      final int u = Binary16.unpackGetSign(p);
      if( verbose ) {
          System.out.println("e: " + e +", p: "+Integer.toHexString(p)+", u: "+u);
      }
      Assert.assertEquals(e, u);
    }
  }

  /**
   * Significands in the range [0, 1023] are encoded and decoded correctly.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testSignificandIdentity()
  {
    System.out.println("-- Significand identities");
    for (int e = 0; e <= 1023; ++e) {
      final char p = Binary16.packSetSignificandUnchecked(e);
      final int u = Binary16.unpackGetSignificand(p);
      if( verbose ) {
          System.out.println("e: " + e +", p: "+Integer.toHexString(p)+", u: "+u);
      }
      Assert.assertEquals(e, u);
    }
  }

  /**
   * Unpacking NaN results in NaN.
   */

  @SuppressWarnings("static-method") @Test public void testUnpackDoubleNaN()
  {
    final double k = Binary16.unpackDouble(Binary16.exampleNaN());
    Assert.assertTrue(Double.isNaN(k));
  }

  /**
   * Unpacking negative infinity results in negative infinity.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testUnpackDoubleNegativeInfinity()
  {
    Assert.assertTrue(Double.NEGATIVE_INFINITY == Binary16
      .unpackDouble(Binary16.NEGATIVE_INFINITY));
  }

  /**
   * Unpacking negative zero results in negative zero.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testUnpackDoubleNegativeZero()
  {
    Assert.assertTrue(-0.0 == Binary16.unpackDouble(Binary16.NEGATIVE_ZERO));
  }

  /**
   * Unpacking 1.0 results in 1.0.
   */

  @SuppressWarnings({ "static-method", "boxing" }) @Test public
    void
    testUnpackDoubleOne()
  {
    final char one = 0x3C00;
    final double r = Binary16.unpackDouble(one);
    System.out.println(String.format("0x%04x -> %f", (int) one, r));
    Assert.assertEquals(r, 1.0, 0.0);
  }

  /**
   * Unpacking -1.0 results in -1.0.
   */

  @SuppressWarnings({ "static-method", "boxing" }) @Test public
    void
    testUnpackDoubleOneNegative()
  {
    final char one = 0xBC00;
    final double r = Binary16.unpackDouble(one);
    System.out.println(String.format("0x%04x -> %f", (int) one, r));
    Assert.assertEquals(r, -1.0, 0.0);
  }

  /**
   * Unpacking positive infinity results in positive infinity.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testUnpackDoublePositiveInfinity()
  {
    Assert.assertTrue(Double.POSITIVE_INFINITY == Binary16
      .unpackDouble(Binary16.POSITIVE_INFINITY));
  }

  /**
   * Unpacking positive zero results in positive zero.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testUnpackDoublePositiveZero()
  {
    Assert.assertTrue(0.0 == Binary16.unpackDouble(Binary16.POSITIVE_ZERO));
  }

  /**
   * Unpacking 2.0 results in 2.0.
   */

  @SuppressWarnings({ "static-method", "boxing" }) @Test public
    void
    testUnpackDoubleTwo()
  {
    final char one = 0x4000;
    final double r = Binary16.unpackDouble(one);
    System.out.println(String.format("%04x -> %f", (int) one, r));
    Assert.assertEquals(r, 2.0, 0.0);
  }

  /**
   * Unpacking -2.0 results in -2.0.
   */

  @SuppressWarnings({ "static-method", "boxing" }) @Test public
    void
    testUnpackDoubleTwoNegative()
  {
    final char one = 0xC000;
    final double r = Binary16.unpackDouble(one);
    System.out.println(String.format("%04x -> %f", (int) one, r));
    Assert.assertEquals(r, -2.0, 0.0);
  }

  /**
   * Unpacking NaN results in NaN.
   */

  @SuppressWarnings("static-method") @Test public void testUnpackFloatNaN()
  {
    final float k = Binary16.unpackFloat(Binary16.exampleNaN());
    Assert.assertTrue(Float.isNaN(k));
  }

  /**
   * Unpacking negative infinity results in negative infinity.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testUnpackFloatNegativeInfinity()
  {
    Assert.assertTrue(Float.NEGATIVE_INFINITY == Binary16
      .unpackFloat(Binary16.NEGATIVE_INFINITY));
  }

  /**
   * Unpacking negative zero results in negative zero.
   */

  @SuppressWarnings("static-method") @Test public
    void
    testUnpackFloatNegativeZero()
  {
    Assert.assertTrue(-0.0 == Binary16.unpackFloat(Binary16.NEGATIVE_ZERO));
  }

  /**
   * Unpacking 1.0 results in 1.0.
   */

  @SuppressWarnings({ "static-method", "boxing" }) @Test public
    void
    testUnpackFloatOne()
  {
    final char one = 0x3C00;
    final float r = Binary16.unpackFloat(one);
    System.out.println(String.format("0x%04x -> %f", (int) one, r));
    Assert.assertEquals(r, 1.0, 0.0);
  }

  /**
   * Unpacking -1.0 results in -1.0.
   */

  @SuppressWarnings({ "static-method", "boxing" }) @Test public
    void
    testUnpackFloatOneNegative()
  {
    final char one = 0xBC00;
    final float r = Binary16.unpackFloat(one);
    System.out.println(String.format("0x%04x -> %f", (int) one, r));
    Assert.assertEquals(r, -1.0, 0.0);
  }

  public static void main(final String args[]) {
      for(int i=0; i<args.length; i++) {
        if(args[i].equals("-stepping")) {
            stepping = MiscUtils.atoi(args[++i], stepping);
        } else if(args[i].equals("-verbose")) {
            verbose = true;
        }
      }
      org.junit.runner.JUnitCore.main(TestBinary16NOUI.class.getName());
  }

}
