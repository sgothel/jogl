/**
 * Copyright 2012-2023 JogAmp Community. All rights reserved.
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

import com.jogamp.junit.util.JunitTracer;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Matrix4f;
import com.jogamp.math.Vec3f;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestMatrix4f01NOUI extends JunitTracer {

    final float[] mI_0 = new float[]{    1,    0,    0,    0,
                                         0,    1,    0,    0,
                                         0,    0,    1,    0,
                                         0,    0,    0,    1 };
    final Matrix4f mI = new Matrix4f(mI_0);

    final float[] m1_0 = new float[]{    1,    3,    4,    0,
                                         6,    7,    8,    5,
                                        98,    7,    6,    9,
                                        54,    3,    2,    5 };
    final Matrix4f m1 = new Matrix4f(m1_0);

    final float[] m1T_0 = new float[]{   1,    6,   98,   54,
                                         3,    7,    7,    3,
                                         4,    8,    6,    2,
                                         0,    5,    9,    5 };
    final Matrix4f m1T = new Matrix4f(m1T_0);

    final float[] m2_0 = new float[]{    1,    6,   98,   54,
                                         3,    7,    7,    3,
                                         4,    8,    6,    2,
                                         0,    5,    9,    5 };
    final Matrix4f m2 = new Matrix4f(m2_0);

    final float[] m2xm1_0 =
                       new float[]{   26,   59,  143,   71,
                                      59,  174,  730,  386,
                                     143,  730, 9770, 5370,
                                      71,  386, 5370, 2954 };
    final Matrix4f m2xm1 = new Matrix4f(m2xm1_0);

    final float[] m1xm2_0 =
                       new float[]{12557,  893,  748, 1182,
                                     893,  116,  116,  113,
                                     748,  116,  120,  104,
                                    1182,  113,  104,  131 };
    final Matrix4f m1xm2 = new Matrix4f(m1xm2_0);

    @Test
    public void test00_load_get() {
        {
            final Matrix4f m = new Matrix4f();
            Assert.assertEquals(mI, m);
        }
        {
            final float[] f16 = new float[16];
            m1.get(f16);
            Assert.assertArrayEquals(m1_0, f16, FloatUtil.EPSILON);
            final Matrix4f m = new Matrix4f();
            m.load(f16);
            Assert.assertEquals(m1, m);
        }
    }

    @Test
    public void test01_mul(){
        {
            final float[] r_0 = new float[16];
            FloatUtil.multMatrix(m1_0, 0, m2_0, 0, r_0, 0);
            Assert.assertArrayEquals(m1xm2_0, r_0, 0f);

            Assert.assertEquals(m1xm2, new Matrix4f(m1).mul(m2));
            Assert.assertEquals(m1xm2, new Matrix4f().mul(m1, m2));
        }
        {
            final float[] r_0 = new float[16];
            FloatUtil.multMatrix(m2_0, 0, m1_0, 0, r_0, 0);
            Assert.assertArrayEquals(m2xm1_0, r_0, 0f);

            Assert.assertEquals(m2xm1, new Matrix4f(m2).mul(m1));
            Assert.assertEquals(m2xm1, new Matrix4f().mul(m2, m1));
        }
    }

    @Test
    public void test02_transpose() {
        Assert.assertEquals(m1T, new Matrix4f(m1).transpose());
        Assert.assertEquals(m1T, new Matrix4f().transpose(m1));
    }

    @Test
    public void test80LookAtNegZIsNoOp() throws Exception {
        final Matrix4f tmp = new Matrix4f();
        final Matrix4f m = new Matrix4f();
        // Look towards -z
        m.setToLookAt(
                new Vec3f(0, 0, 0),  // eye
                new Vec3f(0, 0, -1), // center
                new Vec3f(0, 1, 0),  // up
                tmp);

        /**
         * The 3 rows of the matrix (= the 3 columns of the array/buffer) should be: side, up, -forward.
         */
        final Matrix4f exp = new Matrix4f(
                new float[] {
                        1, 0, 0, 0,
                        0, 1, 0, 0,
                        0, 0, 1, 0,
                        0, 0, 0, 1
                } );

        Assert.assertEquals(exp, m);
    }

    @Test
    public void test81LookAtPosY() throws Exception {
        final Matrix4f tmp = new Matrix4f();
        final Matrix4f m = new Matrix4f();
        // Look towards -z
        m.setToLookAt(
                new Vec3f(0, 0, 0),  // eye
                new Vec3f(0, 1, 0),  // center
                new Vec3f(0, 0, 1),  // up
                tmp);

        /**
         * The 3 rows of the matrix (= the 3 columns of the array/buffer) should be: side, up, -forward.
         */
        final Matrix4f exp = new Matrix4f(
                new float[] {
                        1, 0, 0, 0,
                        0, 0, -1, 0,
                        0, 1, 0, 0,
                        0, 0, 0, 1
                } );

        Assert.assertEquals(exp, m);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestMatrix4f01NOUI.class.getName());
    }
}
