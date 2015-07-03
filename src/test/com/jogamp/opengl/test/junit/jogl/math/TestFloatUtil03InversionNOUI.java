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
import com.jogamp.opengl.math.FloatUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFloatUtil03InversionNOUI {

    @Test
    public void test01Ident(){
        final float[] res1 = new float[16];
        final float[] res2 = new float[16];
        final float[] temp = new float[16];

        final float[] identity = new float[] { 1, 0, 0, 0,
                                               0, 1, 0, 0,
                                               0, 0, 1, 0,
                                               0, 0, 0, 1 };

        FloatUtil.invertMatrix(identity, 0, res1, 0);
        System.err.println(FloatUtil.matrixToString(null, "inv-1: ", "%10.7f", res1, 0, 4, 4, false /* rowMajorOrder */));
        invertMatrix(identity, 0, res2, 0, temp);
        System.err.println(FloatUtil.matrixToString(null, "inv-2: ", "%10.7f", res2, 0, 4, 4, false /* rowMajorOrder */));

        Assert.assertArrayEquals("I1/I2 failure", res1, res2, FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I2 failure", identity, res2, FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I1 failure", identity, res1, FloatUtil.INV_DEVIANCE);
    }

    private void testImpl(final float[] matrix) {
        final float[] inv1_0 = new float[16];
        final float[] inv1_1 = new float[16];
        final float[] inv1_2 = new float[16];
        final float[] inv2_0 = new float[16];
        final float[] inv2_1 = new float[16];
        final float[] inv2_2 = new float[16];
        final float[] temp = new float[16];

        System.err.println(FloatUtil.matrixToString(null, "orig  : ", "%10.7f", matrix, 0, 4, 4, false /* rowMajorOrder */));
        invertMatrix(matrix, 0, inv1_0, 0, temp);
        invertMatrix(inv1_0, 0, inv2_0, 0, temp);
        System.err.println(FloatUtil.matrixToString(null, "inv1_0: ", "%10.7f", inv1_0, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "inv2_0: ", "%10.7f", inv2_0, 0, 4, 4, false /* rowMajorOrder */));
        FloatUtil.invertMatrix(matrix, 0, inv1_1, 0);
        FloatUtil.invertMatrix(inv1_1, 0, inv2_1, 0);
        System.err.println(FloatUtil.matrixToString(null, "inv1_1: ", "%10.7f", inv1_1, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "inv2_1: ", "%10.7f", inv2_1, 0, 4, 4, false /* rowMajorOrder */));
        FloatUtil.invertMatrix(matrix, inv1_2);
        FloatUtil.invertMatrix(inv1_2, inv2_2);
        System.err.println(FloatUtil.matrixToString(null, "inv1_2: ", "%10.7f", inv1_2, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "inv2_2: ", "%10.7f", inv2_2, 0, 4, 4, false /* rowMajorOrder */));

        Assert.assertArrayEquals("I1_1/I1_2 failure", inv1_1, inv1_2, FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I2_1/I2_2 failure", inv2_1, inv2_2, FloatUtil.INV_DEVIANCE);

        Assert.assertArrayEquals("I1_0/I1_1 failure", inv1_0, inv1_2, FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I2_0/I2_1 failure", inv2_0, inv2_2, FloatUtil.INV_DEVIANCE);

        Assert.assertArrayEquals("I1 failure", matrix, inv2_0, FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I2 failure", matrix, inv2_2, FloatUtil.INV_DEVIANCE);
        Assert.assertArrayEquals("I2 failure", matrix, inv2_1, FloatUtil.INV_DEVIANCE);
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
        final float[] mv = new float[] { 1, 0,    0, 0,
                                   0, 1,    0, 0,
                                   0, 0,    1, 0,
                                   0, 0, -200, 1 } ;
        testImpl(mv);
    }

    @Test
    public void test04(){
        final float[] p = new float[] { 2.3464675f, 0,          0,        0,
                                  0,          2.4142134f, 0,        0,
                                  0,          0,         -1.0002f, -1,
                                  0,          0,        -20.002f,   0 };

        testImpl(p);
    }

    @Test
    public void test05Perf(){
        final float[] p1 = new float[] { 2.3464675f, 0,          0,        0,
                                   0,          2.4142134f, 0,        0,
                                   0,          0,         -1.0002f, -1,
                                   0,          0,        -20.002f,   0 };

        final float[] p2 = new float[]{   26,   59,  143,   71,
                                    59,  174,  730,  386,
                                   143,  730, 9770, 5370,
                                    71,  386, 5370, 2954 };

        final float[] res1 = new float[16];
        final float[] res2 = new float[16];
        final float[] temp = new float[16];

        final int loops = 1000000;
        long tI0 = 0;
        long tI1 = 0;
        long tI2 = 0;

        // warm-up
        for(int i=0; i<10; i++) {
            invertMatrix(p1, 0, res2, 0, temp);
            FloatUtil.invertMatrix(p1, 0, res1, 0);
            FloatUtil.invertMatrix(p1, res1);

            invertMatrix(p2, 0, res2, 0, temp);
            FloatUtil.invertMatrix(p2, 0, res1, 0);
            FloatUtil.invertMatrix(p2, res1);
        }


        for(int i=0; i<loops; i++) {

            final long t_0 = Platform.currentTimeMillis();

            invertMatrix(p1, 0, res2, 0, temp);
            final long t_1 = Platform.currentTimeMillis();
            tI0 += t_1 - t_0;

            FloatUtil.invertMatrix(p1, 0, res1, 0);
            final long t_2 = Platform.currentTimeMillis();
            tI1 += t_2 - t_1;

            FloatUtil.invertMatrix(p1, res1);
            final long t_3 = Platform.currentTimeMillis();
            tI2 += t_3 - t_2;

            invertMatrix(p2, 0, res2, 0, temp);
            final long t_4 = Platform.currentTimeMillis();
            tI0 += t_4 - t_3;

            FloatUtil.invertMatrix(p2, 0, res1, 0);
            final long t_5 = Platform.currentTimeMillis();
            tI1 += t_5 - t_4;

            FloatUtil.invertMatrix(p2, res2);
            final long t_6 = Platform.currentTimeMillis();
            tI2 += t_6 - t_5;
        }
        System.err.printf("Summary loops %6d: I1 %6d ms total, %f ms/inv%n", loops, tI0, (double)tI0/loops);
        System.err.printf("Summary loops %6d: I2 %6d ms total, %f ms/inv%n", loops, tI1, (double)tI1/loops);
        System.err.printf("Summary loops %6d: I3 %6d ms total, %f ms/inv%n", loops, tI2, (double)tI2/loops);

    }

    public static float[] invertMatrix(final float[] msrc, final int msrc_offset, final float[] mres, final int mres_offset, final float[/*4*4*/] temp) {
        int i, j, k, swap;
        float t;
        for (i = 0; i < 4; i++) {
            final int i4 = i*4;
            for (j = 0; j < 4; j++) {
                temp[i4+j] = msrc[i4+j+msrc_offset];
            }
        }
        FloatUtil.makeIdentity(mres, mres_offset);

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

                    t = mres[i4+k+mres_offset];
                    mres[i4+k+mres_offset] = mres[swap4+k+mres_offset];
                    mres[swap4+k+mres_offset] = t;
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
                mres[i4+k+mres_offset] /= t;
            }
            for (j = 0; j < 4; j++) {
                if (j != i) {
                    final int j4 = j*4;
                    t = temp[j4+i];
                    for (k = 0; k < 4; k++) {
                        temp[j4+k] -= temp[i4+k] * t;
                        mres[j4+k+mres_offset] -= mres[i4+k+mres_offset]*t;
                    }
                }
            }
        }
        return mres;
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestFloatUtil03InversionNOUI.class.getName());
    }
}
