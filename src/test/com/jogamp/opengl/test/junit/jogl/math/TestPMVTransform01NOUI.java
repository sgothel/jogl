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

import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.test.junit.util.MiscUtils;
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
        System.err.println(pmv.getCurrentMat().toString(null, "Ident     ", "%7.5f"));

        final Vec3f t = new Vec3f(1f, 2f, 3f);
        final Vec3f s = new Vec3f(2f, 2f, 2f);

        pmv.glTranslatef(t);
        System.err.println(pmv.getCurrentMat().toString(null, "Translate ", "%7.5f"));
        pmv.glScalef(s);
        System.err.println(pmv.getCurrentMat().toString(null, "Scale     ", "%7.5f"));

        final Matrix4f exp = new Matrix4f(new float[] {
                2.00000f, 0.00000f, 0.00000f, 0.00000f,
                0.00000f, 2.00000f, 0.00000f, 0.00000f,
                0.00000f, 0.00000f, 2.00000f, 0.00000f,
                1.00000f, 2.00000f, 3.00000f, 1.00000f,
        });
        final Matrix4f has = new Matrix4f();
        pmv.mulPMvMat(has);
        MiscUtils.assertMatrix4fEquals(exp, has, epsilon);
        Assert.assertEquals(exp, has);
    }

    @Test
    public void test02() {
        final PMVMatrix pmv = new PMVMatrix();
        System.err.println(pmv.getCurrentMat().toString(null, "Ident     ", "%7.5f"));

        final Vec3f t = new Vec3f(1f, 2f, 3f);
        final Vec3f s = new Vec3f(2f, 2f, 2f);

        pmv.glScalef(s);
        System.err.println(pmv.getCurrentMat().toString(null, "Scale     ", "%7.5f"));
        pmv.glTranslatef(t);
        System.err.println(pmv.getCurrentMat().toString(null, "Translate ", "%7.5f"));

        final Matrix4f exp = new Matrix4f(new float[] {
                2.00000f, 0.00000f, 0.00000f, 0.00000f,
                0.00000f, 2.00000f, 0.00000f, 0.00000f,
                0.00000f, 0.00000f, 2.00000f, 0.00000f,
                2.00000f, 4.00000f, 6.00000f, 1.00000f,
        });
        final Matrix4f has = new Matrix4f();
        pmv.mulPMvMat(has);
        MiscUtils.assertMatrix4fEquals(exp, has, epsilon);
        Assert.assertEquals(exp, has);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestPMVTransform01NOUI.class.getName());
    }
}
