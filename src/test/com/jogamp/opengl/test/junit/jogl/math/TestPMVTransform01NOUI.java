/**
 * Copyright 2014-2023 JogAmp Community. All rights reserved.
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

import com.jogamp.junit.util.JunitTracer;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPMVTransform01NOUI extends JunitTracer {

    static final float epsilon = 0.00001f;

    @Test
    public void test01() {
        final PMVMatrix pmv = new PMVMatrix();
        System.err.println(FloatUtil.matrixToString(null, "Ident     ", "%7.5f", pmv.glGetMatrixf(), 0, 4, 4, true /* rowMajorOrder */));

        final float[] t = { 1f, 2f, 3f };
        final float[] s = { 2f, 2f, 2f };

        pmv.glTranslatef(t[0], t[1], t[2]);
        System.err.println(FloatUtil.matrixToString(null, "Translate ", "%7.5f", pmv.glGetMatrixf(), 0, 4, 4, true/* rowMajorOrder */));
        pmv.glScalef(s[0], s[1], s[2]);
        System.err.println(FloatUtil.matrixToString(null, "Scale     ", "%7.5f", pmv.glGetMatrixf(), 0, 4, 4, true /* rowMajorOrder */));

        final float[] exp = new float[] {
                2.00000f, 0.00000f, 0.00000f, 0.00000f,
                0.00000f, 2.00000f, 0.00000f, 0.00000f,
                0.00000f, 0.00000f, 2.00000f, 0.00000f,
                1.00000f, 2.00000f, 3.00000f, 1.00000f,
        };
        final float[] has = new float[16];
        pmv.multPMvMatrixf(has, 0);
        Assert.assertArrayEquals(exp, has, epsilon);
    }

    @Test
    public void test02() {
        final PMVMatrix pmv = new PMVMatrix();
        System.err.println(FloatUtil.matrixToString(null, "Ident     ", "%7.5f", pmv.glGetMatrixf(), 0, 4, 4, true /* rowMajorOrder */));

        final float[] t = { 1f, 2f, 3f };
        final float[] s = { 2f, 2f, 2f };

        pmv.glScalef(s[0], s[1], s[2]);
        System.err.println(FloatUtil.matrixToString(null, "Scale     ", "%7.5f", pmv.glGetMatrixf(), 0, 4, 4, true /* rowMajorOrder */));
        pmv.glTranslatef(t[0], t[1], t[2]);
        System.err.println(FloatUtil.matrixToString(null, "Translate ", "%7.5f", pmv.glGetMatrixf(), 0, 4, 4, true/* rowMajorOrder */));

        final float[] exp = new float[] {
                2.00000f, 0.00000f, 0.00000f, 0.00000f,
                0.00000f, 2.00000f, 0.00000f, 0.00000f,
                0.00000f, 0.00000f, 2.00000f, 0.00000f,
                2.00000f, 4.00000f, 6.00000f, 1.00000f,
        };
        final float[] has = new float[16];
        pmv.multPMvMatrixf(has, 0);
        Assert.assertArrayEquals(exp, has, epsilon);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestPMVTransform01NOUI.class.getName());
    }
}
