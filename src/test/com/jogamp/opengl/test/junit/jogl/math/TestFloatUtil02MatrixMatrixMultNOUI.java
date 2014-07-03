/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.math.FloatUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFloatUtil02MatrixMatrixMultNOUI {

    final float[] m1 = new float[]{    1,    3,    4,    0,
                                       6,    7,    8,    5,
                                      98,    7,    6,    9,
                                      54,    3,    2,    5 };

    final float[] m2 = new float[]{    1,    6,   98,   54,
                                       3,    7,    7,    3,
                                       4,    8,    6,    2,
                                       0,    5,    9,    5 };

    final float[] m1xm2_RM = // m2xm1_CM
                       new float[]{   26,   59,  143,   71,
                                      59,  174,  730,  386,
                                     143,  730, 9770, 5370,
                                      71,  386, 5370, 2954 };

    final float[] m2xm1_RM = // m1xm2_CM
                       new float[]{12557,  893,  748, 1182,
                                     893,  116,  116,  113,
                                     748,  116,  120,  104,
                                    1182,  113,  104,  131 };

    public static final void multMatrixf_RM(final float[] a, final int a_off, final float[] b, final int b_off, final float[] d, final int d_off) {
     for (int i = 0; i < 4; i++) {
        final float ai0=a[a_off+i*4+0],  ai1=a[a_off+i*4+1],  ai2=a[a_off+i*4+2],  ai3=a[a_off+i*4+3];
        d[d_off+i*4+0] = ai0 * b[b_off+0*4+0] + ai1 * b[b_off+1*4+0] + ai2 * b[b_off+2*4+0] + ai3 * b[b_off+3*4+0] ;
        d[d_off+i*4+1] = ai0 * b[b_off+0*4+1] + ai1 * b[b_off+1*4+1] + ai2 * b[b_off+2*4+1] + ai3 * b[b_off+3*4+1] ;
        d[d_off+i*4+2] = ai0 * b[b_off+0*4+2] + ai1 * b[b_off+1*4+2] + ai2 * b[b_off+2*4+2] + ai3 * b[b_off+3*4+2] ;
        d[d_off+i*4+3] = ai0 * b[b_off+0*4+3] + ai1 * b[b_off+1*4+3] + ai2 * b[b_off+2*4+3] + ai3 * b[b_off+3*4+3] ;
     }
    }

    @Test
    public void testCM_m1xm2(){

        final float[] r = new float[16];

        FloatUtil.multMatrix(m1, 0, m2, 0, r, 0);

        Assert.assertArrayEquals(m2xm1_RM, r, 0f);
    }

    @Test
    public void testCM_m2xm1(){

        final float[] r = new float[16];

        FloatUtil.multMatrix(m2, 0, m1, 0, r, 0);

        Assert.assertArrayEquals(m1xm2_RM, r, 0f);
    }

    @Test
    public void testRM_m1xm2(){

        final float[] r = new float[16];

        multMatrixf_RM(m1, 0, m2, 0, r, 0);

        Assert.assertArrayEquals(m1xm2_RM, r, 0f);
    }

    @Test
    public void testRM_m2xm1(){

        final float[] r = new float[16];

        multMatrixf_RM(m2, 0, m1, 0, r, 0);

        Assert.assertArrayEquals(m2xm1_RM, r, 0f);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestFloatUtil02MatrixMatrixMultNOUI.class.getName());
    }
}
