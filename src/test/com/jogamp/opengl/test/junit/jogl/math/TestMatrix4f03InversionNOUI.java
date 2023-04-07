/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.JunitTracer;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Matrix4f;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestMatrix4f03InversionNOUI extends JunitTracer {

    @Test
    public void test01Ident(){
        final float[] res1 = new float[16];
        final float[] res2 = new float[16];
        final float[] temp = new float[16];

        final float[] identity = new float[] { 1, 0, 0, 0,
                                               0, 1, 0, 0,
                                               0, 0, 1, 0,
                                               0, 0, 0, 1 };

        FloatUtil.invertMatrix(identity, res1);
        // System.err.println(FloatUtil.matrixToString(null, "inv-1: ", "%10.7f", res1, 0, 4, 4, false /* rowMajorOrder */));
        invertMatrix(identity, res2, temp);
        // System.err.println(FloatUtil.matrixToString(null, "inv-2: ", "%10.7f", res2, 0, 4, 4, false /* rowMajorOrder */));

        Assert.assertArrayEquals("I1/I2 failure", res1, res2, FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I2 failure", identity, res2, FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I1 failure", identity, res1, FloatUtil.INV_DEVIANCE);

        final Matrix4f res3 = new Matrix4f(identity);
        Assert.assertTrue( res3.invert() );
        // System.err.println(res3.toString(null, "inv-4: ", "%10.7f"));
        Assert.assertEquals(new Matrix4f(res1), res3);
        Assert.assertEquals(new Matrix4f(), res3);

        final Matrix4fb res4 = new Matrix4fb(identity);
        Assert.assertTrue( res4.invert() );
        // System.err.println(res4.toString(null, "inv-5: ", "%10.7f"));
        Assert.assertEquals(new Matrix4fb(res1), res4);
        Assert.assertEquals(new Matrix4fb(), res4);
    }

    private void testImpl(final float[] matrix) {
        final float[] inv1_0 = new float[16];
        final float[] inv1_1 = new float[16];
        final float[] inv1_2 = new float[16];
        final float[] inv2_0 = new float[16];
        final float[] inv2_1 = new float[16];
        final float[] inv2_2 = new float[16];
        final float[] temp = new float[16];

        // System.err.println(FloatUtil.matrixToString(null, "orig  : ", "%10.7f", matrix, 0, 4, 4, false /* rowMajorOrder */));
        invertMatrix(matrix, inv1_0, temp);
        invertMatrix(inv1_0, inv2_0, temp);
        // System.err.println(FloatUtil.matrixToString(null, "inv1_0: ", "%10.7f", inv1_0, 0, 4, 4, false /* rowMajorOrder */));
        // System.err.println(FloatUtil.matrixToString(null, "inv2_0: ", "%10.7f", inv2_0, 0, 4, 4, false /* rowMajorOrder */));
        FloatUtil.invertMatrix(matrix, inv1_1);
        FloatUtil.invertMatrix(inv1_1, inv2_1);
        // System.err.println(FloatUtil.matrixToString(null, "inv1_1: ", "%10.7f", inv1_1, 0, 4, 4, false /* rowMajorOrder */));
        // System.err.println(FloatUtil.matrixToString(null, "inv2_1: ", "%10.7f", inv2_1, 0, 4, 4, false /* rowMajorOrder */));
        FloatUtil.invertMatrix(matrix, inv1_2);
        FloatUtil.invertMatrix(inv1_2, inv2_2);
        // System.err.println(FloatUtil.matrixToString(null, "inv1_2: ", "%10.7f", inv1_2, 0, 4, 4, false /* rowMajorOrder */));
        // System.err.println(FloatUtil.matrixToString(null, "inv2_2: ", "%10.7f", inv2_2, 0, 4, 4, false /* rowMajorOrder */));

        Assert.assertArrayEquals("I1_1/I1_2 failure", inv1_1, inv1_2, FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I2_1/I2_2 failure", inv2_1, inv2_2, FloatUtil.INV_DEVIANCE);

        Assert.assertArrayEquals("I1_0/I1_1 failure", inv1_0, inv1_2, FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I2_0/I2_1 failure", inv2_0, inv2_2, FloatUtil.INV_DEVIANCE);

        Assert.assertArrayEquals("I1 failure", matrix, inv2_0, FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I2 failure", matrix, inv2_2, FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I2 failure", matrix, inv2_1, FloatUtil.INV_DEVIANCE);

        //
        // Matrix4f
        //

        final Matrix4f matrix_m = new Matrix4f(matrix);
        final Matrix4f inv1_4a = new Matrix4f(matrix_m);
        Assert.assertTrue( inv1_4a.invert() );
        final Matrix4f inv2_4a = new Matrix4f(inv1_4a);
        Assert.assertTrue( inv2_4a.invert() );
        // System.err.println(inv1_4a.toString(null, "inv1_4a: ", "%10.7f"));
        // System.err.println(inv2_4a.toString(null, "inv2_4a: ", "%10.7f"));

        // Assert.assertEquals(new Matrix4f(inv1_2), inv1_4a);
        // Assert.assertEquals(new Matrix4f(inv2_2), inv2_4a);
        Assert.assertArrayEquals("I5 failure", inv1_2, inv1_4a.get(temp), FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I5 failure", inv2_2, inv2_4a.get(temp), FloatUtil.INV_DEVIANCE);
        Assert.assertTrue("I4 failure: "+matrix_m+" != "+inv2_4a, matrix_m.isEqual(inv2_4a, FloatUtil.INV_DEVIANCE));

        final Matrix4f inv1_4b = new Matrix4f();
        Assert.assertTrue( inv1_4b.invert(matrix_m) );
        final Matrix4f inv2_4b = new Matrix4f();
        Assert.assertTrue( inv2_4b.invert(inv1_4b) );
        // System.err.println(inv1_4b.toString(null, "inv1_4b: ", "%10.7f"));
        // System.err.println(inv2_4b.toString(null, "inv2_4b: ", "%10.7f"));

        // Assert.assertEquals(new Matrix4f(inv1_2), inv1_4b);
        // Assert.assertEquals(new Matrix4f(inv2_2), inv2_4b);
        Assert.assertArrayEquals("I5 failure", inv1_2, inv1_4b.get(temp), FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I5 failure", inv2_2, inv2_4b.get(temp), FloatUtil.INV_DEVIANCE);
        Assert.assertTrue("I4 failure: "+matrix_m+" != "+inv2_4b, matrix_m.isEqual(inv2_4b, FloatUtil.INV_DEVIANCE));

        //
        // Matrix4fb
        //

        final Matrix4fb matrix_n = new Matrix4fb(matrix);
        final Matrix4fb inv1_5a = new Matrix4fb(matrix_n);
        Assert.assertTrue( inv1_5a.invert() );
        final Matrix4fb inv2_5a = new Matrix4fb(inv1_5a);
        Assert.assertTrue( inv2_5a.invert() );
        // System.err.println(inv1_5a.toString(null, "inv1_5a: ", "%10.7f"));
        // System.err.println(inv2_5a.toString(null, "inv2_5a: ", "%10.7f"));

        // Assert.assertEquals(new Matrix4fb(inv1_2), inv1_5a);
        // Assert.assertEquals(new Matrix4fb(inv2_2), inv2_5a);
        Assert.assertArrayEquals("I5 failure", inv1_2, inv1_5a.get(temp), FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I5 failure", inv2_2, inv2_5a.get(temp), FloatUtil.INV_DEVIANCE);
        Assert.assertTrue("I5 failure: "+matrix_n+" != "+inv2_5a, matrix_n.isEqual(inv2_5a, FloatUtil.INV_DEVIANCE));

        final Matrix4fb inv1_5b = new Matrix4fb();
        Assert.assertTrue( inv1_5b.invert(matrix_n) );
        final Matrix4fb inv2_5b = new Matrix4fb();
        Assert.assertTrue( inv2_5b.invert(inv1_5b) );
        // System.err.println(inv1_5b.toString(null, "inv1_5b: ", "%10.7f"));
        // System.err.println(inv2_5b.toString(null, "inv2_5b: ", "%10.7f"));

        // Assert.assertEquals(new Matrix4fb(inv1_2), inv1_5b);
        // Assert.assertEquals(new Matrix4fb(inv2_2), inv2_5b);
        Assert.assertArrayEquals("I5 failure", inv1_2, inv1_5b.get(temp), FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I5 failure", inv2_2, inv2_5b.get(temp), FloatUtil.INV_DEVIANCE);
        Assert.assertTrue("I5 failure: "+matrix_n+" != "+inv2_5b, matrix_n.isEqual(inv2_5b, FloatUtil.INV_DEVIANCE));
    }

    @Test
    public void test02(){
        final float[] p = new float[] { 2.3464675f, 0,          0,        0,
                                  0,          2.4142134f, 0,        0,
                                  0,          0,         -1.0002f, -1,
                                  0,          0,        -20.002f,   0 };
        testImpl(p);
    }

    @Test
    public void test03(){
        final float[] mv = new float[] {
                                   1, 0,    0, 0,
                                   0, 1,    0, 0,
                                   0, 0,    1, 0,
                                   0, 0, -200, 1 } ;
        testImpl(mv);
    }

    @Test
    public void test04(){
        final float[] p = new float[] {
                                  2.3464675f, 0,          0,        0,
                                  0,          2.4142134f, 0,        0,
                                  0,          0,         -1.0002f, -1,
                                  0,          0,        -20.002f,   0 };

        testImpl(p);
    }

    @Test
    public void test05Perf01(){
        final float[] p1 = new float[] {
                                   2.3464675f, 0,          0,        0,
                                   0,          2.4142134f, 0,        0,
                                   0,          0,         -1.0002f, -1,
                                   0,          0,        -20.002f,   0 };
        final Matrix4f p1_m = new Matrix4f(p1);
        final Matrix4fb p1_n = new Matrix4fb(p1);

        final float[] p2 = new float[]{
                                    26,   59,  143,   71,
                                    59,  174,  730,  386,
                                   143,  730, 9770, 5370,
                                    71,  386, 5370, 2954 };
        final Matrix4f p2_m = new Matrix4f(p2);
        final Matrix4fb p2_n = new Matrix4fb(p2);

        final float[] res = new float[16];
        final float[] temp = new float[16];

        final Matrix4f res_m = new Matrix4f();
        final Matrix4fb res_n = new Matrix4fb();

        final int warmups = 1000;
        final int loops = 10*1000*1000;
        long tI0 = 0;
        long tI2 = 0;
        long tI4a = 0;
        long tI4b = 0;
        long tI5a = 0;
        long tI5b = 0;

        // warm-up
        for(int i=0; i<warmups; i++) {
            invertMatrix(p1, res, temp);
        }
        long t_0 = Platform.currentTimeMillis();
        for(int i=0; i<loops; i++) {
            // I0: p1 -> res
            invertMatrix(p1, res, temp);

            // I0: p2 -> res
            invertMatrix(p2, res, temp);
        }
        tI0 = Platform.currentTimeMillis() - t_0;

        // warm-up
        for(int i=0; i<warmups; i++) {
            FloatUtil.invertMatrix(p1, res);
            FloatUtil.invertMatrix(p2, res);
        }
        t_0 = Platform.currentTimeMillis();
        for(int i=0; i<loops; i++) {
            // I2: p1 -> res
            FloatUtil.invertMatrix(p1, res);

            // I2: p2 -> res
            FloatUtil.invertMatrix(p2, res);
        }
        tI2 = Platform.currentTimeMillis() - t_0;

        //
        // Matrix4f
        //

        // warm-up
        for(int i=0; i<warmups; i++) {
            res_m.invert(p1_m);
            res_m.invert(p2_m);
        }
        t_0 = Platform.currentTimeMillis();
        for(int i=0; i<loops; i++) {
            res_m.invert(p1_m);
            res_m.invert(p2_m);
        }
        tI4a = Platform.currentTimeMillis() - t_0;

        if( false ) {
            // warm-up
            for(int i=0; i<warmups; i++) {
                res_m.load(p1_m).invert();
                res_m.load(p2_m).invert();
            }
            t_0 = Platform.currentTimeMillis();
            for(int i=0; i<loops; i++) {
                res_m.load(p1_m).invert();
                res_m.load(p2_m).invert();
            }
            tI4b = Platform.currentTimeMillis() - t_0;
        } else {
            res_m.load(p1_m);

            // warm-up
            for(int i=0; i<warmups; i++) {
                res_m.invert();
                res_m.invert();
            }
            t_0 = Platform.currentTimeMillis();
            for(int i=0; i<loops; i++) {
                res_m.invert();
                res_m.invert();
            }
            tI4b = Platform.currentTimeMillis() - t_0;
        }

        //
        // Matrix4fb
        //

        // warm-up
        for(int i=0; i<warmups; i++) {
            res_n.invert(p1_n);
            res_n.invert(p2_n);
        }
        t_0 = Platform.currentTimeMillis();
        for(int i=0; i<loops; i++) {
            res_n.invert(p1_n);
            res_n.invert(p2_n);
        }
        tI5a = Platform.currentTimeMillis() - t_0;

        if( false ) {
            // warm-up
            for(int i=0; i<warmups; i++) {
                res_n.load(p1_n).invert();
                res_n.load(p2_n).invert();
            }
            t_0 = Platform.currentTimeMillis();
            for(int i=0; i<loops; i++) {
                res_n.load(p1_n).invert();
                res_n.load(p2_n).invert();
            }
            tI5b = Platform.currentTimeMillis() - t_0;
        } else {
            res_n.load(p1_n);

            // warm-up
            for(int i=0; i<warmups; i++) {
                res_n.invert();
                res_n.invert();
            }
            t_0 = Platform.currentTimeMillis();
            for(int i=0; i<loops; i++) {
                res_n.invert();
                res_n.invert();
            }
            tI5b = Platform.currentTimeMillis() - t_0;
        }

        System.err.printf("Summary loops %6d: I0  %6d ms total, %f us/inv%n", loops, tI0, tI0*1e3/loops);
        System.err.printf("Summary loops %6d: I2  %6d ms total, %f us/inv, I2  / I0 %f%%%n", loops, tI2, tI2*1e3/2.0/loops, tI2/(double)tI0*100.0);
        System.err.printf("Summary loops %6d: I4a %6d ms total, %f us/inv, I4a / I2 %f%%%n", loops, tI4a, tI4a*1e3/2.0/loops, (double)tI4a/(double)tI2*100.0);
        System.err.printf("Summary loops %6d: I4b %6d ms total, %f us/inv, I4b / I2 %f%%%n", loops, tI4b, tI4b*1e3/2.0/loops, (double)tI4b/(double)tI2*100.0);
        System.err.printf("Summary loops %6d: I5a %6d ms total, %f us/inv, I5a / I2 %f%%%n", loops, tI5a, tI5a*1e3/2.0/loops, (double)tI5a/(double)tI2*100.0);
        System.err.printf("Summary loops %6d: I5b %6d ms total, %f us/inv, I5b / I2 %f%%%n", loops, tI5b, tI5b*1e3/2.0/loops, (double)tI5b/(double)tI2*100.0);
    }

    public static float[] invertMatrix(final float[] msrc, final float[] mres, final float[/*4*4*/] temp) {
        int i, j, k, swap;
        float t;
        for (i = 0; i < 4; i++) {
            final int i4 = i*4;
            for (j = 0; j < 4; j++) {
                temp[i4+j] = msrc[i4+j];
            }
        }
        FloatUtil.makeIdentity(mres);

        for (i = 0; i < 4; i++) {
            final int i4 = i*4;

            //
            // Look for largest element in column
            //
            swap = i;
            for (j = i + 1; j < 4; j++) {
                if (Math.abs(temp[j*4+i]) > Math.abs(temp[i4+i])) {
                    swap = j;
                }
            }

            if (swap != i) {
                final int swap4 = swap*4;
                //
                // Swap rows.
                //
                for (k = 0; k < 4; k++) {
                    t = temp[i4+k];
                    temp[i4+k] = temp[swap4+k];
                    temp[swap4+k] = t;

                    t = mres[i4+k];
                    mres[i4+k] = mres[swap4+k];
                    mres[swap4+k] = t;
                }
            }

            if (temp[i4+i] == 0) {
                //
                // No non-zero pivot. The matrix is singular, which shouldn't
                // happen. This means the user gave us a bad matrix.
                //
                return null;
            }

            t = temp[i4+i];
            for (k = 0; k < 4; k++) {
                temp[i4+k] /= t;
                mres[i4+k] /= t;
            }
            for (j = 0; j < 4; j++) {
                if (j != i) {
                    final int j4 = j*4;
                    t = temp[j4+i];
                    for (k = 0; k < 4; k++) {
                        temp[j4+k] -= temp[i4+k] * t;
                        mres[j4+k] -= mres[i4+k]*t;
                    }
                }
            }
        }
        return mres;
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestMatrix4f03InversionNOUI.class.getName());
    }
}
