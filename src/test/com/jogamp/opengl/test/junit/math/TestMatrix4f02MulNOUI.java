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

package com.jogamp.opengl.test.junit.math;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.JunitTracer;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Matrix4f;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestMatrix4f02MulNOUI extends JunitTracer {

    final float[] m1_0 = new float[]{    1,    3,    4,    0,
                                         6,    7,    8,    5,
                                        98,    7,    6,    9,
                                        54,    3,    2,    5 };
    final Matrix4f m1 = new Matrix4f(m1_0);
    final Matrix4fb n1 = new Matrix4fb(m1_0);


    final float[] m2_0 = new float[]{    1,    6,   98,   54,
                                         3,    7,    7,    3,
                                         4,    8,    6,    2,
                                         0,    5,    9,    5 };
    final Matrix4f m2 = new Matrix4f(m2_0);
    final Matrix4fb n2 = new Matrix4fb(m2_0);

    final float[] m2xm1_0 =
                       new float[]{   26,   59,  143,   71,
                                      59,  174,  730,  386,
                                     143,  730, 9770, 5370,
                                      71,  386, 5370, 2954 };
    final Matrix4f m2xm1 = new Matrix4f(m2xm1_0);
    final Matrix4fb n2xn1 = new Matrix4fb(m2xm1_0);

    final float[] m1xm2_0 =
                       new float[]{12557,  893,  748, 1182,
                                     893,  116,  116,  113,
                                     748,  116,  120,  104,
                                    1182,  113,  104,  131 };
    final Matrix4f m1xm2 = new Matrix4f(m1xm2_0);
    final Matrix4fb n1xn2 = new Matrix4fb(m1xm2_0);

    @Test
    public void test01_mul(){
        {
            final float[] r_0 = new float[16];
            FloatUtil.multMatrix(m1_0, 0, m2_0, 0, r_0, 0);
            Assert.assertArrayEquals(m1xm2_0, r_0, 0f);

            Assert.assertEquals(m1xm2, new Matrix4f(m1).mul(m2));
            Assert.assertEquals(m1xm2, new Matrix4f().mul(m1, m2));

            Assert.assertEquals(n1xn2, new Matrix4fb(n1).mul(n2));
            Assert.assertEquals(n1xn2, new Matrix4fb().mul(n1, n2));
        }
        {
            final float[] r_0 = new float[16];
            FloatUtil.multMatrix(m2_0, 0, m1_0, 0, r_0, 0);
            Assert.assertArrayEquals(m2xm1_0, r_0, 0f);

            Assert.assertEquals(m2xm1, new Matrix4f(m2).mul(m1));
            Assert.assertEquals(m2xm1, new Matrix4f().mul(m2, m1));

            Assert.assertEquals(n2xn1, new Matrix4fb(n2).mul(n1));
            Assert.assertEquals(n2xn1, new Matrix4fb().mul(n2, n1));
        }
    }

    @Test
    public void test05Perf01(){
        final float[] res = new float[16];

        final Matrix4f res_m = new Matrix4f();
        final Matrix4fb res_n = new Matrix4fb();

        final int warmups = 1000;
        final int loops = 10*1000*1000;
        long tI1 = 0;
        long tI2 = 0;
        long tI4a = 0;
        long tI4b = 0;
        long tI5a = 0;
        long tI5b = 0;

        // warm-up
        for(int i=0; i<warmups; i++) {
            FloatUtil.multMatrix(m1_0, 0, m2_0, 0, res, 0);
            FloatUtil.multMatrix(m2_0, 0, m1_0, 0, res, 0);
        }
        long t_0 = Platform.currentTimeMillis();
        for(int i=0; i<loops; i++) {
            FloatUtil.multMatrix(m1_0, 0, m2_0, 0, res, 0);
            FloatUtil.multMatrix(m2_0, 0, m1_0, 0, res, 0);
        }
        tI1 = Platform.currentTimeMillis() - t_0;

        // warm-up
        for(int i=0; i<warmups; i++) {
            FloatUtil.multMatrix(m1_0, m2_0, res);
            FloatUtil.multMatrix(m2_0, m1_0, res);
        }
        t_0 = Platform.currentTimeMillis();
        for(int i=0; i<loops; i++) {
            FloatUtil.multMatrix(m1_0, m2_0, res);
            FloatUtil.multMatrix(m2_0, m1_0, res);
        }
        tI2 = Platform.currentTimeMillis() - t_0;

        //
        // Matrix4f
        //

        // warm-up
        for(int i=0; i<warmups; i++) {
            res_m.mul(m1, m2);
            res_m.mul(m2, m1);
        }
        t_0 = Platform.currentTimeMillis();
        for(int i=0; i<loops; i++) {
            res_m.mul(m1, m2);
            res_m.mul(m2, m1);
        }
        tI4a = Platform.currentTimeMillis() - t_0;

        res_m.load(m1);

        // warm-up
        for(int i=0; i<warmups; i++) {
            res_m.mul(m2);
            res_m.mul(m1);
        }
        t_0 = Platform.currentTimeMillis();
        for(int i=0; i<loops; i++) {
            res_m.mul(m2);
            res_m.mul(m1);
        }
        tI4b = Platform.currentTimeMillis() - t_0;

        //
        // Matrix4fb
        //

        // warm-up
        for(int i=0; i<warmups; i++) {
            res_n.mul(n1, n2);
            res_n.mul(n2, n1);
        }
        t_0 = Platform.currentTimeMillis();
        for(int i=0; i<loops; i++) {
            res_n.mul(n1, n2);
            res_n.mul(n2, n1);
        }
        tI5a = Platform.currentTimeMillis() - t_0;

        res_n.load(n1);

        // warm-up
        for(int i=0; i<warmups; i++) {
            res_n.mul(n2);
            res_n.mul(n1);
        }
        t_0 = Platform.currentTimeMillis();
        for(int i=0; i<loops; i++) {
            res_n.mul(n2);
            res_n.mul(n1);
        }
        tI5b = Platform.currentTimeMillis() - t_0;

        System.err.printf("Summary loops %6d: I1  %6d ms total, %f us/mul%n", loops, tI1, tI1*1e3/loops);
        System.err.printf("Summary loops %6d: I2  %6d ms total, %f us/mul, I2  / I1 %f%%%n", loops, tI2, tI2*1e3/2.0/loops, (double)tI2/(double)tI1*100.0);
        System.err.printf("Summary loops %6d: I4a %6d ms total, %f us/mul, I4a / I2 %f%%, I4a / I4b %f%%%n", loops, tI4a, tI4a*1e3/2.0/loops, (double)tI4a/(double)tI2*100.0, (double)tI4a/(double)tI4b*100.0);
        System.err.printf("Summary loops %6d: I4b %6d ms total, %f us/mul, I4b / I2 %f%%, I4b / I4a %f%%%n", loops, tI4b, tI4b*1e3/2.0/loops, (double)tI4b/(double)tI2*100.0, (double)tI4b/(double)tI4a*100.0);
        System.err.printf("Summary loops %6d: I5a %6d ms total, %f us/mul, I5a / I2 %f%%, I5a / I5b %f%%%n", loops, tI5a, tI5a*1e3/2.0/loops, (double)tI5a/(double)tI2*100.0, (double)tI5a/(double)tI5b*100.0);
        System.err.printf("Summary loops %6d: I5b %6d ms total, %f us/mul, I5b / I2 %f%%, I5b / I5a %f%%%n", loops, tI5b, tI5b*1e3/2.0/loops, (double)tI5b/(double)tI2*100.0, (double)tI5b/(double)tI5a*100.0);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestMatrix4f02MulNOUI.class.getName());
    }
}
