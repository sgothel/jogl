/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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

import com.jogamp.junit.util.JunitTracer;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.Vec3f;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestVec3f01NOUI extends JunitTracer {
    static final boolean DEBUG = false;

    static final Quaternion QUAT_IDENT = new Quaternion(0f, 0f, 0f, 1f);

    static final Vec3f ZERO       = new Vec3f();
    static final Vec3f ONE        = Vec3f.ONE;
    static final Vec3f NEG_ONE    = new Vec3f ( -1f, -1f, -1f );
    static final Vec3f UNIT_X     = Vec3f.UNIT_X;
    static final Vec3f UNIT_X_NEG = Vec3f.UNIT_X_NEG;
    static final Vec3f UNIT_Y     = Vec3f.UNIT_Y;
    static final Vec3f UNIT_Z     = Vec3f.UNIT_Z;

    static final float MACH_EPSILON = FloatUtil.EPSILON;

    //
    // Basic
    //

    @Test
    public void test01Normalize() {
        final Vec3f v0 = UNIT_X;
        final Vec3f v1 = new Vec3f(1, 2, 3);
        Assert.assertEquals(1f, v0.length(), MACH_EPSILON);
        Assert.assertEquals(1f, v1.normalize().length(), MACH_EPSILON);
    }

    @Test
    public void test02Angle() {
        // test 0 deg
        {
            System.err.println("Test 0-deg, UNIT_X vecs");
            final Vec3f v0 = UNIT_X;
            final Vec3f v1 = UNIT_X;
            System.err.println("v0 "+v0);
            System.err.println("v1 "+v1);
            final float a0_v0_v1 = v0.angle(v1);
            System.err.println("a0(v0, v1) = "+a0_v0_v1+" rad, "+FloatUtil.radToADeg(a0_v0_v1)+" deg, via dot, acos");
            Assert.assertEquals(0f, a0_v0_v1, MACH_EPSILON);
        }
        // test 0 deg
        {
            System.err.println("Test 0-deg, free vecs");
            final Vec3f v0 = new Vec3f(0.14f, 0.07f, 0f);
            final Vec3f v1 = new Vec3f(0.33f, 0.07f, 0f);
            final Vec3f v0_1 = v1.minus(v0);
            System.err.println("v0 "+v0);
            System.err.println("v1 "+v1);
            System.err.println("v0_1 "+v0_1);

            final float a0_x_v0_1 = UNIT_X.angle(v0_1);
            System.err.println("a0(X, v0_1) = "+a0_x_v0_1+" rad, "+FloatUtil.radToADeg(a0_x_v0_1)+" deg, via dot, acos");
            Assert.assertEquals(0f, a0_x_v0_1, MACH_EPSILON);
        }
        // test 180 deg
        {
            System.err.println("Test 180-deg, free vecs");
            final Vec3f v0 = new Vec3f(0.33f, 0.07f, 0f);
            final Vec3f v1 = new Vec3f(0.14f, 0.07f, 0f);
            final Vec3f v0_1 = v1.minus(v0);
            System.err.println("v0 "+v0);
            System.err.println("v1 "+v1);
            System.err.println("v0_1 "+v0_1);

            final float a0_x_v0_1 = UNIT_X.angle(v0_1);
            System.err.println("a0(X, v0_1) = "+a0_x_v0_1+" rad, "+FloatUtil.radToADeg(a0_x_v0_1)+" deg, via dot, acos");
            Assert.assertEquals(FloatUtil.PI, a0_x_v0_1, MACH_EPSILON);
        }
        // test 90 deg
        {
            System.err.println("Test 90-deg, UNIT_X, UNIT_Y vecs");
            final Vec3f v0 = UNIT_X;
            final Vec3f v1 = UNIT_Y;
            System.err.println("v0 "+v0);
            System.err.println("v1 "+v1);
            final float a0_v0_v1 = v0.angle(v1);
            System.err.println("a0(v0, v1) = "+a0_v0_v1+" rad, "+FloatUtil.radToADeg(a0_v0_v1)+" deg, via dot, acos");
            Assert.assertEquals(FloatUtil.HALF_PI, a0_v0_v1, MACH_EPSILON);
        }
        // test 180 deg
        {
            System.err.println("Test 180-deg, UNIT_X, UNIT_X_NEG vecs");
            final Vec3f v0 = UNIT_X;
            final Vec3f v1 = UNIT_X_NEG;
            System.err.println("v0 "+v0);
            System.err.println("v1 "+v1);
            final float a0_v0_v1 = v0.angle(v1);
            System.err.println("a0(v0, v1) = "+a0_v0_v1+" rad, "+FloatUtil.radToADeg(a0_v0_v1)+" deg, via dot, acos");
            Assert.assertEquals(FloatUtil.PI, a0_v0_v1, MACH_EPSILON);
        }
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestVec3f01NOUI.class.getName());
    }
}
