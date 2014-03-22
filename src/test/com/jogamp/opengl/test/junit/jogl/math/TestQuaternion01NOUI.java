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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestQuaternion01NOUI {
    static final boolean DEBUG = false;

    static final Quaternion QUAT_IDENT = new Quaternion(0f, 0f, 0f, 1f);

    static final float[] ZERO       = new float[] {  0f,  0f,  0f };
    static final float[] ONE        = new float[] {  1f,  1f,  1f };
    static final float[] NEG_ONE    = new float[] { -1f, -1f, -1f };
    static final float[] UNIT_X     = new float[] {  1f,  0f,  0f };
    static final float[] UNIT_Y     = new float[] {  0f,  1f,  0f };
    static final float[] UNIT_Z     = new float[] {  0f,  0f,  1f };
    static final float[] NEG_UNIT_X = new float[] { -1f,  0f,  0f };
    static final float[] NEG_UNIT_Y = new float[] {  0f, -1f,  0f };
    static final float[] NEG_UNIT_Z = new float[] {  0f,  0f, -1f };

    static final float[] NEG_ONE_v4 = new float[] { -1f, -1f, -1f, 0f };
    static final float[] ONE_v4     = new float[] {  1f,  1f,  1f, 0f };

    static final float MACH_EPSILON = FloatUtil.getMachineEpsilon();

    //
    // Basic
    //

    @Test
    public void test01Normalize() {
        final Quaternion quat = new Quaternion(0, 1, 2, 3);
        final Quaternion quat2 = new Quaternion(quat).normalize();
        // Assert.assertTrue(Math.abs(1 - quat2.magnitude()) <= MACH_EPSILON);
        Assert.assertEquals(0f, Math.abs(1 - quat2.magnitude()), MACH_EPSILON);
    }

    @Test
    public void test02RotateZeroVector() {
        final Quaternion quat = new Quaternion();
        final float[] rotVec0 = quat.rotateVector(new float[3], 0, ZERO, 0);
        Assert.assertArrayEquals(ZERO, rotVec0, FloatUtil.EPSILON);
    }

    @Test
    public void test03InvertAndConj() {
        // inversion check
        {
            final Quaternion quat0 = new Quaternion(0, 1, 2, 3);
            final Quaternion quat0Inv = new Quaternion(quat0).invert();
            Assert.assertEquals(quat0, quat0Inv.invert());
        }
        // conjugate check
        {
            final Quaternion quat0 = new Quaternion(-1f, -2f, -3f, 4f);
            final Quaternion quat0Conj = new Quaternion( 1f,  2f,  3f, 4f).conjugate();
            Assert.assertEquals(quat0, quat0Conj);
        }
    }

    @Test
    public void test04Dot() {
        final Quaternion quat = new Quaternion(7f, 2f, 5f, -1f);
        Assert.assertTrue(35.0f == quat.dot(3f, 1f, 2f, -2f));
        Assert.assertTrue(-11.0f == quat.dot(new Quaternion(-1f, 1f, -1f, 1f)));
    }


    //
    // Conversion
    //

    @Test
    public void test10AngleAxis() {
        final float[] tmpV3f = new float[3];
        final Quaternion quat1 = new Quaternion().setFromAngleAxis(FloatUtil.HALF_PI, new float[] { 2, 0, 0 }, tmpV3f );
        final Quaternion quat2 = new Quaternion().setFromAngleNormalAxis(FloatUtil.HALF_PI, new float[] { 1, 0, 0 } );

        Assert.assertEquals(quat2, quat1);
        // System.err.println("M "+quat2.magnitude()+", 1-M "+(1f-quat2.magnitude())+", Eps "+FloatUtil.EPSILON);
        Assert.assertEquals(0f, 1 - quat2.magnitude(), FloatUtil.EPSILON);
        Assert.assertTrue(1 - quat1.magnitude() <= FloatUtil.EPSILON);

        final float[] vecOut1 = new float[3];
        final float[] vecOut2 = new float[3];
        quat1.rotateVector(vecOut1, 0, ONE, 0);
        quat2.rotateVector(vecOut2, 0, ONE, 0);
        Assert.assertArrayEquals(vecOut1, vecOut2, FloatUtil.EPSILON);
        Assert.assertEquals(0f, Math.abs( VectorUtil.vec3Distance(vecOut1, vecOut2) ), FloatUtil.EPSILON );

        quat1.rotateVector(vecOut1, 0, UNIT_Z, 0);
        Assert.assertEquals(0f, Math.abs( VectorUtil.vec3Distance(NEG_UNIT_Y, vecOut1) ), FloatUtil.EPSILON );

        quat2.setFromAngleAxis(FloatUtil.HALF_PI, ZERO, tmpV3f);
        Assert.assertEquals(QUAT_IDENT, quat2);

        float angle = quat1.toAngleAxis(vecOut1);
        quat2.setFromAngleAxis(angle, vecOut1, tmpV3f);
        Assert.assertEquals(quat1, quat2);

        quat1.set(0, 0, 0, 0);
        angle = quat1.toAngleAxis(vecOut1);
        Assert.assertTrue(0.0f == angle);
        Assert.assertArrayEquals(UNIT_X, vecOut1, FloatUtil.EPSILON);
    }

    @Test
    public void test11FromVectorToVector() {
        final float[] tmp0V3f = new float[3];
        final float[] tmp1V3f = new float[3];
        final float[] vecOut = new float[3];
        final Quaternion quat = new Quaternion();
        quat.setFromVectors(UNIT_Z, UNIT_X, tmp0V3f, tmp1V3f);

        final Quaternion quat2 = new Quaternion();
        quat2.setFromNormalVectors(UNIT_Z, UNIT_X, tmp0V3f);
        Assert.assertEquals(quat, quat2);

        quat2.setFromAngleAxis(FloatUtil.HALF_PI, UNIT_Y, tmp0V3f);
        Assert.assertEquals(quat2, quat);

        quat.setFromVectors(UNIT_Z, NEG_UNIT_Z, tmp0V3f, tmp1V3f);
        quat.rotateVector(vecOut, 0, UNIT_Z, 0);
        // System.err.println("vecOut: "+Arrays.toString(vecOut));
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(NEG_UNIT_Z, vecOut) ), Quaternion.ALLOWED_DEVIANCE );

        quat.setFromVectors(UNIT_X, NEG_UNIT_X, tmp0V3f, tmp1V3f);
        quat.rotateVector(vecOut, 0, UNIT_X, 0);
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(NEG_UNIT_X, vecOut) ), Quaternion.ALLOWED_DEVIANCE );

        quat.setFromVectors(UNIT_Y, NEG_UNIT_Y, tmp0V3f, tmp1V3f);
        quat.rotateVector(vecOut, 0, UNIT_Y, 0);
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(NEG_UNIT_Y, vecOut) ), Quaternion.ALLOWED_DEVIANCE );

        quat.setFromVectors(ONE, NEG_ONE, tmp0V3f, tmp1V3f);
        quat.rotateVector(vecOut, 0, ONE, 0);
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(NEG_ONE, vecOut) ), Quaternion.ALLOWED_DEVIANCE );

        quat.setFromVectors(ZERO, ZERO, tmp0V3f, tmp1V3f);
        Assert.assertEquals(QUAT_IDENT, quat);
    }

    @Test
    public void test12FromAndToEulerAngles() {
        // Y.Z.X -> X.Y.Z
        final Quaternion quat = new Quaternion();
        final float[] angles0Exp = new float[] { 0f, FloatUtil.HALF_PI, 0f};
        quat.setFromEuler(angles0Exp);
        Assert.assertEquals(1.0f, quat.magnitude(), FloatUtil.EPSILON);

        final float[] angles0Has = quat.toEuler(new float[3]);
        // System.err.println("exp0 "+Arrays.toString(angles0Exp));
        // System.err.println("has0 "+Arrays.toString(angles0Has));
        Assert.assertArrayEquals(angles0Exp, angles0Has, FloatUtil.EPSILON);

        final Quaternion quat2 = new Quaternion();
        quat2.setFromEuler(angles0Has);
        Assert.assertEquals(quat, quat2);

        ///

        final float[] angles1Exp = new float[] { 0f, 0f, -FloatUtil.HALF_PI };
        quat.setFromEuler(angles1Exp);
        Assert.assertEquals(1.0f, quat.magnitude(), FloatUtil.EPSILON);

        final float[] angles1Has = quat.toEuler(new float[3]);
        // System.err.println("exp1 "+Arrays.toString(angles1Exp));
        // System.err.println("has1 "+Arrays.toString(angles1Has));
        Assert.assertArrayEquals(angles1Exp, angles1Has, FloatUtil.EPSILON);

        quat2.setFromEuler(angles1Has);
        Assert.assertEquals(quat, quat2);

        ///

        final float[] angles2Exp = new float[] { FloatUtil.HALF_PI, 0f, 0f };
        quat.setFromEuler(angles2Exp);
        Assert.assertEquals(1.0f, quat.magnitude(), FloatUtil.EPSILON);

        final float[] angles2Has = quat.toEuler(new float[3]);
        // System.err.println("exp2 "+Arrays.toString(angles2Exp));
        // System.err.println("has2 "+Arrays.toString(angles2Has));
        Assert.assertArrayEquals(angles2Exp, angles2Has, FloatUtil.EPSILON);

        quat2.setFromEuler(angles2Has);
        Assert.assertEquals(quat, quat2);
    }

    @Test
    public void test13FromEulerAnglesAndRotateVector() {
        final Quaternion quat = new Quaternion();
        quat.setFromEuler(0, FloatUtil.HALF_PI, 0); // 90 degrees y-axis
        Assert.assertEquals(1.0f, quat.magnitude(), FloatUtil.EPSILON);

        final float[] v2 = quat.rotateVector(new float[3], 0, UNIT_X, 0);
        Assert.assertEquals(0f, Math.abs(VectorUtil.vec3Distance(NEG_UNIT_Z, v2)), FloatUtil.EPSILON);

        quat.setFromEuler(0, 0, -FloatUtil.HALF_PI);
        Assert.assertEquals(1.0f, quat.magnitude(), FloatUtil.EPSILON);
        quat.rotateVector(v2, 0, UNIT_X, 0);
        Assert.assertEquals(0f, Math.abs(VectorUtil.vec3Distance(NEG_UNIT_Y, v2)), FloatUtil.EPSILON);

        quat.setFromEuler(FloatUtil.HALF_PI, 0, 0);
        Assert.assertEquals(1.0f, quat.magnitude(), FloatUtil.EPSILON);
        quat.rotateVector(v2, 0, UNIT_Y, 0);
        Assert.assertEquals(0f, Math.abs(VectorUtil.vec3Distance(UNIT_Z, v2)), FloatUtil.EPSILON);
    }

    @Test
    public void test14Matrix() {
        final float[] vecHas = new float[3];
        final float[] vecOut2 = new float[4];
        float[] mat1 = new float[4*4];
        float[] mat2 = new float[4*4];
        final Quaternion quat = new Quaternion();

        //
        // IDENTITY CHECK
        //
        FloatUtil.makeIdentityf(mat1, 0);
        quat.set(0, 0, 0, 0);
        quat.toMatrix(mat2, 0);
        Assert.assertArrayEquals(mat1, mat2, FloatUtil.EPSILON);

        //
        // 90 degrees rotation on X
        //

        float a = FloatUtil.HALF_PI;
        mat1 = new float[] { // Column Order
                1,  0,                 0,                0, //
                0,  FloatUtil.cos(a),  FloatUtil.sin(a), 0, //
                0, -FloatUtil.sin(a),  FloatUtil.cos(a), 0,
                0,  0,                 0,                1  };
        {
            // Validate Matrix via Euler rotation on Quaternion!
            quat.setFromEuler(a, 0f, 0f);
            quat.toMatrix(mat2, 0);
            // System.err.println(FloatUtil.matrixToString(null, "quat-rot", "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
            Assert.assertArrayEquals(mat1, mat2, FloatUtil.EPSILON);
            quat.rotateVector(vecHas, 0, UNIT_Y, 0);
            // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
            // System.err.println("has0 "+Arrays.toString(vecHas));
            Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(UNIT_Z, vecHas) ), Quaternion.ALLOWED_DEVIANCE );
        }
        quat.setFromMatrix(mat1, 0);
        quat.rotateVector(vecHas, 0, UNIT_Y, 0);
        // System.err.println("exp0 "+Arrays.toString(UNIT_Z));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(UNIT_Z, vecHas) ), Quaternion.ALLOWED_DEVIANCE );

        quat.toMatrix(mat2, 0);
        // System.err.println(FloatUtil.matrixToString(null, null, "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
        Assert.assertArrayEquals(mat1, mat2, FloatUtil.EPSILON);

        quat.rotateVector(vecHas, 0, NEG_ONE, 0);
        FloatUtil.multMatrixVecf(mat2, NEG_ONE_v4, vecOut2);
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(vecHas, vecOut2) ), Quaternion.ALLOWED_DEVIANCE );

        //
        // 180 degrees rotation on X
        //
        a = FloatUtil.PI;
        mat1 = new float[] { // Column Order
                1,  0,                 0,                0, //
                0,   FloatUtil.cos(a), FloatUtil.sin(a), 0, //
                0,  -FloatUtil.sin(a), FloatUtil.cos(a), 0,
                0,  0,                 0,                1 };
        {
            // Validate Matrix via Euler rotation on Quaternion!
            quat.setFromEuler(a, 0f, 0f);
            quat.toMatrix(mat2, 0);
            // System.err.println(FloatUtil.matrixToString(null, "quat-rot", "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
            Assert.assertArrayEquals(mat1, mat2, FloatUtil.EPSILON);
            quat.rotateVector(vecHas, 0, UNIT_Y, 0);
            // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
            // System.err.println("has0 "+Arrays.toString(vecHas));
            Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(NEG_UNIT_Y, vecHas) ), Quaternion.ALLOWED_DEVIANCE );
        }
        quat.setFromMatrix(mat1, 0);
        quat.rotateVector(vecHas, 0, UNIT_Y, 0);
        // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_Y));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(NEG_UNIT_Y, vecHas) ), Quaternion.ALLOWED_DEVIANCE );

        quat.toMatrix(mat2, 0);
        // System.err.println(FloatUtil.matrixToString(null, null, "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
        Assert.assertArrayEquals(mat1, mat2, FloatUtil.EPSILON);

        quat.rotateVector(vecHas, 0, ONE, 0);
        FloatUtil.multMatrixVecf(mat2, ONE_v4, vecOut2);
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(vecHas, vecOut2) ), Quaternion.ALLOWED_DEVIANCE );

        //
        // 180 degrees rotation on Y
        //
        a = FloatUtil.PI;
        mat1 = new float[] { // Column Order
                 FloatUtil.cos(a), 0,  -FloatUtil.sin(a), 0, //
                 0,                1,   0,                0, //
                 FloatUtil.sin(a), 0,   FloatUtil.cos(a), 0,
                 0,                0,   0,                1 };
        {
            // Validate Matrix via Euler rotation on Quaternion!
            quat.setFromEuler(0f, a, 0f);
            quat.toMatrix(mat2, 0);
            // System.err.println(FloatUtil.matrixToString(null, "quat-rot", "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
            Assert.assertArrayEquals(mat1, mat2, FloatUtil.EPSILON);
            quat.rotateVector(vecHas, 0, UNIT_X, 0);
            // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
            // System.err.println("has0 "+Arrays.toString(vecHas));
            Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(NEG_UNIT_X, vecHas) ), Quaternion.ALLOWED_DEVIANCE );
        }
        quat.setFromMatrix(mat1, 0);
        quat.rotateVector(vecHas, 0, UNIT_X, 0);
        // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(NEG_UNIT_X, vecHas) ), Quaternion.ALLOWED_DEVIANCE );

        quat.toMatrix(mat2, 0);
        // System.err.println(FloatUtil.matrixToString(null, "matr-rot", "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
        Assert.assertArrayEquals(mat1, mat2, FloatUtil.EPSILON);

        quat.rotateVector(vecHas, 0, NEG_ONE, 0);
        FloatUtil.multMatrixVecf(mat2, NEG_ONE_v4, vecOut2);
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(vecHas, vecOut2) ), Quaternion.ALLOWED_DEVIANCE );

        //
        // 180 degrees rotation on Z
        //
        a = FloatUtil.PI;
        mat1 = new float[] { // Column Order
                  FloatUtil.cos(a), FloatUtil.sin(a), 0, 0, //
                 -FloatUtil.sin(a), FloatUtil.cos(a), 0, 0,
                  0,                0,                1, 0,
                  0,                0,                0, 1 };
        {
            // Validate Matrix via Euler rotation on Quaternion!
            quat.setFromEuler(0f, 0f, a);
            quat.toMatrix(mat2, 0);
            // System.err.println(FloatUtil.matrixToString(null, "quat-rot", "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
            Assert.assertArrayEquals(mat1, mat2, FloatUtil.EPSILON);
            quat.rotateVector(vecHas, 0, UNIT_X, 0);
            // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
            // System.err.println("has0 "+Arrays.toString(vecHas));
            Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(NEG_UNIT_X, vecHas) ), Quaternion.ALLOWED_DEVIANCE );
        }
        quat.setFromMatrix(mat1, 0);
        quat.rotateVector(vecHas, 0, UNIT_X, 0);
        // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(NEG_UNIT_X, vecHas) ), Quaternion.ALLOWED_DEVIANCE );

        quat.toMatrix(mat2, 0);
        // System.err.println(FloatUtil.matrixToString(null, "matr-rot", "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
        Assert.assertArrayEquals(mat1, mat2, FloatUtil.EPSILON);

        quat.rotateVector(vecHas, 0, ONE, 0);
        FloatUtil.multMatrixVecf(mat2, ONE_v4, vecOut2);
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(vecHas, vecOut2) ), Quaternion.ALLOWED_DEVIANCE );

        //
        // Test Matrix-Columns
        //

        a = FloatUtil.QUARTER_PI;
        float[] vecExp = new float[3];
        float[] vecCol = new float[3];
        mat1 = new float[] { // Column Order
                  FloatUtil.cos(a), FloatUtil.sin(a), 0, 0, //
                 -FloatUtil.sin(a), FloatUtil.cos(a), 0, 0,
                  0,                0,                1, 0,
                  0,                0,                0, 1 };
        quat.setFromMatrix(mat1, 0);
        FloatUtil.copyMatrixColumn(mat1, 0, 0, vecExp, 0);
        quat.copyMatrixColumn(0, vecCol, 0);
        // System.err.println("exp0 "+Arrays.toString(vecExp));
        // System.err.println("has0 "+Arrays.toString(vecCol));
        Assert.assertEquals(0f, Math.abs( VectorUtil.vec3Distance(vecExp, vecCol)), FloatUtil.EPSILON);

        FloatUtil.copyMatrixColumn(mat1, 0, 1, vecExp, 0);
        quat.copyMatrixColumn(1, vecCol, 0);
        // System.err.println("exp1 "+Arrays.toString(vecExp));
        // System.err.println("has1 "+Arrays.toString(vecCol));
        Assert.assertEquals(0f, Math.abs( VectorUtil.vec3Distance(vecExp, vecCol)), FloatUtil.EPSILON);

        FloatUtil.copyMatrixColumn(mat1, 0, 2, vecExp, 0);
        quat.copyMatrixColumn(2, vecCol, 0);
        // System.err.println("exp2 "+Arrays.toString(vecExp));
        // System.err.println("has2 "+Arrays.toString(vecCol));
        Assert.assertEquals(0f, Math.abs( VectorUtil.vec3Distance(vecExp, vecCol)), FloatUtil.EPSILON);

        quat.set(0f, 0f, 0f, 0f);
        Assert.assertArrayEquals(UNIT_X, quat.copyMatrixColumn(0, vecCol, 0), FloatUtil.EPSILON);

    }

    @Test
    public void test15aAxesAndMatrix() {
        final float[] eulerExp = new float[] { 0f, FloatUtil.HALF_PI, 0f };
        final float[] matExp = new float[4*4];
        FloatUtil.makeRotationEuler(eulerExp[0], eulerExp[1], eulerExp[2], matExp, 0); // 45 degr on X, 90 degr on Y

        final float[] matHas = new float[4*4];
        final Quaternion quat1 = new Quaternion();
        quat1.setFromEuler(eulerExp);
        quat1.toMatrix(matHas, 0);
        // System.err.println(FloatUtil.matrixToString(null, "exp-has", "%10.5f", matExp, 0, matHas, 0, 4, 4, false).toString());
        Assert.assertArrayEquals(matExp, matHas, FloatUtil.EPSILON);

        final float[] eulerHas = new float[3];
        final Quaternion quat2 = new Quaternion();
        quat2.setFromMatrix(matExp, 0);
        quat2.toEuler(eulerHas);
        // System.err.println("exp-euler "+Arrays.toString(eulerExp));
        // System.err.println("has-euler "+Arrays.toString(eulerHas));
        Assert.assertArrayEquals(eulerExp, eulerHas, FloatUtil.EPSILON);

        Assert.assertEquals(quat2, quat1);

        final float[] angles = new float[3];
        quat2.toEuler(angles);
        quat1.setFromEuler(angles);
        Assert.assertEquals(quat2, quat1);
    }

    @Test
    public void test15bAxesAndMatrix() {
        final float[] eulerExp = new float[] { FloatUtil.HALF_PI, 0f, 0f };
        final float[] matExp = new float[4*4];
        FloatUtil.makeRotationEuler(eulerExp[0], eulerExp[1], eulerExp[2], matExp, 0); // 45 degr on X, 90 degr on Y

        final float[] matHas = new float[4*4];
        final Quaternion quat1 = new Quaternion();
        quat1.setFromEuler(eulerExp);
        quat1.toMatrix(matHas, 0);
        // System.err.println(FloatUtil.matrixToString(null, "exp-has", "%10.5f", matExp, 0, matHas, 0, 4, 4, false).toString());
        Assert.assertArrayEquals(matExp, matHas, FloatUtil.EPSILON);

        final float[] eulerHas = new float[3];
        final Quaternion quat2 = new Quaternion();
        quat2.setFromMatrix(matExp, 0);
        quat2.toEuler(eulerHas);
        // System.err.println("exp-euler "+Arrays.toString(eulerExp));
        // System.err.println("has-euler "+Arrays.toString(eulerHas));
        Assert.assertArrayEquals(eulerExp, eulerHas, FloatUtil.EPSILON);

        Assert.assertEquals(quat2, quat1);

        final float[] angles = new float[3];
        quat2.toEuler(angles);
        quat1.setFromEuler(angles);
        Assert.assertEquals(quat2, quat1);
    }

    @Test
    public void test15cAxesAndMatrix() {
        final float[] eulerExp = new float[] { FloatUtil.QUARTER_PI, FloatUtil.HALF_PI, 0f };
        final float[] matExp = new float[4*4];
        FloatUtil.makeRotationEuler(eulerExp[0], eulerExp[1], eulerExp[2], matExp, 0); // 45 degr on X, 90 degr on Y

        final float[] matHas = new float[4*4];
        final Quaternion quat1 = new Quaternion();
        quat1.setFromEuler(eulerExp);
        quat1.toMatrix(matHas, 0);
        // System.err.println(FloatUtil.matrixToString(null, "exp-has", "%10.5f", matExp, 0, matHas, 0, 4, 4, false).toString());
        Assert.assertArrayEquals(matExp, matHas, FloatUtil.EPSILON);

        final float[] eulerHas = new float[3];
        final Quaternion quat2 = new Quaternion();
        quat2.setFromMatrix(matExp, 0);
        quat2.toEuler(eulerHas);
        // System.err.println("exp-euler "+Arrays.toString(eulerExp));
        // System.err.println("has-euler "+Arrays.toString(eulerHas));
        Assert.assertArrayEquals(eulerExp, eulerHas, FloatUtil.EPSILON);

        Assert.assertEquals(quat2, quat1);

        final float[] angles = new float[3];
        quat2.toEuler(angles);
        quat1.setFromEuler(angles);
        Assert.assertEquals(quat2, quat1);
    }

    //
    // Functions
    //

    @Test
    public void test20AddSubtract() {
        final Quaternion quatExp1 = new Quaternion(1, 2, 3, 4);
        final Quaternion quat1    = new Quaternion(0, 1, 2, 3);
        final Quaternion quat2    = new Quaternion(1, 1, 1, 1);

        final Quaternion quatHas  = new Quaternion();
        quatHas.set(quat1);
        quatHas.add(quat2); // q3 = q1 + q2
        Assert.assertEquals(quatExp1, quatHas);

        quat1.set(0, 1, 2, 3);
        quat2.set(1, 1, 1, 1);
        quatHas.set(quat1);
        quatHas.subtract(quat2); // q3 = q1 - q2
        Assert.assertEquals(new Quaternion(-1, 0, 1, 2), quatHas);
    }

    @Test
    public void test21Multiply() {
        final Quaternion quat1 = new Quaternion(0.5f, 1f, 2f, 3f);
        final Quaternion quat2 = new Quaternion();

        quat2.set(quat1);
        quat2.scale(2f); // q2 = q1 * 2f
        Assert.assertEquals(new Quaternion(1, 2, 4, 6), quat2);

        quat2.set(quat1);
        quat2.scale(4f); // q2 = q1 * 4f
        Assert.assertEquals(new Quaternion(2, 4, 8, 12), quat2);

        //
        // mul and cmp rotated vector
        //
        quat1.setFromAngleNormalAxis(FloatUtil.QUARTER_PI, UNIT_Y); // 45 degr on Y
        quat2.set(quat1);
        quat2.mult(quat1); // q2 = q1 * q1 -> 2 * 45 degr -> 90 degr on Y

        final float[] vecOut = new float[3];
        quat2.rotateVector(vecOut, 0, UNIT_Z, 0);
        Assert.assertTrue( Math.abs( VectorUtil.vec3Distance(UNIT_X, vecOut)) <= Quaternion.ALLOWED_DEVIANCE);

        quat2.setFromAngleNormalAxis(FloatUtil.HALF_PI, UNIT_Y); // 90 degr on Y
        quat1.mult(quat1); // q1 = q1 * q1 -> 2 * 45 degr ->  90 degr on Y
        quat1.mult(quat2); // q1 = q1 * q2 -> 2 * 90 degr -> 180 degr on Y
        quat1.rotateVector(vecOut, 0, UNIT_Z, 0);
        Assert.assertTrue( Math.abs( VectorUtil.vec3Distance(NEG_UNIT_Z, vecOut)) <= Quaternion.ALLOWED_DEVIANCE);

        quat2.setFromEuler(0f, FloatUtil.HALF_PI, 0f);
        quat1.mult(quat2); // q1 = q1 * q2 = q1 * rotMat(0, 90degr, 0)
        quat1.rotateVector(vecOut, 0, UNIT_Z, 0);
        Assert.assertTrue( Math.abs( VectorUtil.vec3Distance(NEG_UNIT_X, vecOut)) <= Quaternion.ALLOWED_DEVIANCE);
    }

    @Test
    public void test22InvertMultNormalAndConj() {
        final Quaternion quat0 = new Quaternion(0, 1, 2, 3);
        final Quaternion quat1 = new Quaternion(quat0);
        final Quaternion quat2 = new Quaternion(quat0);
        quat1.invert();    // q1 = invert(q0)
        quat2.mult(quat1); // q2 = q0 * q1 = q0 * invert(q0)
        Assert.assertEquals(QUAT_IDENT, quat2);
        quat1.invert();
        Assert.assertEquals(quat0, quat1);

        // normalized version
        quat0.setFromAngleNormalAxis(FloatUtil.QUARTER_PI, UNIT_Y);
        quat1.set(quat0);
        quat1.invert(); // q1 = invert(q0)
        quat2.set(quat0);
        quat2.mult(quat1); // q2 = q0 * q1 = q0 * invert(q0)
        Assert.assertEquals(QUAT_IDENT, quat2);
        quat1.invert();
        Assert.assertEquals(quat0, quat1);

        // conjugate check
        quat0.set(-1f, -2f, -3f, 4f);
        quat1.set( 1f,  2f,  3f, 4f);
        quat2.set(quat1);
        quat2.conjugate();
        Assert.assertEquals(quat0, quat2);
    }

    @Test
    public void test23RotationOrder() {
        {
            final Quaternion quat1 = new Quaternion().setFromEuler( -2f*FloatUtil.HALF_PI, 0f, 0f); // -180 degr X
            final Quaternion quat2 = new Quaternion().rotateByAngleX( -2f * FloatUtil.HALF_PI); // angle: -180 degrees, axis X
            Assert.assertEquals(quat1, quat2);
        }
        {
            final Quaternion quat1 = new Quaternion().setFromEuler(    FloatUtil.HALF_PI, 0f, 0f); //   90 degr X
            final Quaternion quat2 = new Quaternion().rotateByAngleX(       FloatUtil.HALF_PI); // angle:   90 degrees, axis X
            Assert.assertEquals(quat1, quat2);
        }
        {
            final Quaternion quat1 = new Quaternion().setFromEuler( FloatUtil.HALF_PI, FloatUtil.QUARTER_PI, 0f);
            final Quaternion quat2 = new Quaternion().rotateByAngleY(FloatUtil.QUARTER_PI).rotateByAngleX(FloatUtil.HALF_PI);
            Assert.assertEquals(quat1, quat2);
        }
        {
            final Quaternion quat1 = new Quaternion().setFromEuler( FloatUtil.PI, FloatUtil.QUARTER_PI, FloatUtil.HALF_PI);
            final Quaternion quat2 = new Quaternion().rotateByAngleY(FloatUtil.QUARTER_PI).rotateByAngleZ(FloatUtil.HALF_PI).rotateByAngleX(FloatUtil.PI);
            Assert.assertEquals(quat1, quat2);
        }


        float[] vecExp = new float[3];
        float[] vecRot = new float[3];
        final Quaternion quat = new Quaternion();

        // Try a new way with new angles...
        quat.setFromEuler(FloatUtil.HALF_PI, FloatUtil.QUARTER_PI, FloatUtil.PI);
        vecRot = new float[] { 1f, 1f, 1f };
        quat.rotateVector(vecRot, 0, vecRot, 0);

        // expected
        vecExp = new float[] { 1f, 1f, 1f };
        final Quaternion worker = new Quaternion();
        // put together matrix, then apply to vector, so YZX
        worker.rotateByAngleY(FloatUtil.QUARTER_PI).rotateByAngleZ(FloatUtil.PI).rotateByAngleX(FloatUtil.HALF_PI);
        quat.rotateVector(vecExp, 0, vecExp, 0);
        Assert.assertEquals(0f, VectorUtil.vec3Distance(vecExp, vecRot), FloatUtil.EPSILON);

        // test axis rotation methods against general purpose
        // X AXIS
        vecExp = new float[] { 1f, 1f, 1f };
        vecRot = new float[] { 1f, 1f, 1f };
        worker.setIdentity().rotateByAngleX(FloatUtil.QUARTER_PI).rotateVector(vecExp, 0, vecExp, 0);
        worker.setIdentity().rotateByAngleNormalAxis(FloatUtil.QUARTER_PI, 1f, 0f, 0f).rotateVector(vecRot, 0, vecRot, 0);
        // System.err.println("exp0 "+Arrays.toString(vecExp)+", len "+VectorUtil.length(vecExp));
        // System.err.println("has0 "+Arrays.toString(vecRot)+", len "+VectorUtil.length(vecRot));
        Assert.assertEquals(0f, VectorUtil.vec3Distance(vecExp, vecRot), FloatUtil.EPSILON);

        // Y AXIS
        vecExp = new float[] { 1f, 1f, 1f };
        vecRot = new float[] { 1f, 1f, 1f };
        worker.setIdentity().rotateByAngleY(FloatUtil.QUARTER_PI).rotateVector(vecExp, 0, vecExp, 0);
        worker.setIdentity().rotateByAngleNormalAxis(FloatUtil.QUARTER_PI, 0f, 1f, 0f).rotateVector(vecRot, 0, vecRot, 0);
        // System.err.println("exp0 "+Arrays.toString(vecExp));
        // System.err.println("has0 "+Arrays.toString(vecRot));
        Assert.assertEquals(0f, VectorUtil.vec3Distance(vecExp, vecRot), FloatUtil.EPSILON);

        // Z AXIS
        vecExp = new float[] { 1f, 1f, 1f };
        vecRot = new float[] { 1f, 1f, 1f };
        worker.setIdentity().rotateByAngleZ(FloatUtil.QUARTER_PI).rotateVector(vecExp, 0, vecExp, 0);
        worker.setIdentity().rotateByAngleNormalAxis(FloatUtil.QUARTER_PI, 0f, 0f, 1f).rotateVector(vecRot, 0, vecRot, 0);
        // System.err.println("exp0 "+Arrays.toString(vecExp));
        // System.err.println("has0 "+Arrays.toString(vecRot));
        Assert.assertEquals(0f, VectorUtil.vec3Distance(vecExp, vecRot), FloatUtil.EPSILON);

        quat.set(worker);
        worker.rotateByAngleNormalAxis(0f, 0f, 0f, 0f);
        Assert.assertEquals(quat, worker);
    }

    @Test
    public void test24Axes() {
        final Quaternion quat0 = new Quaternion().rotateByAngleX(FloatUtil.QUARTER_PI).rotateByAngleY(FloatUtil.HALF_PI);
        final float[] rotMat = new float[4*4];
        quat0.toMatrix(rotMat, 0);
        final float[] xAxis = new float[3];
        final float[] yAxis = new float[3];
        final float[] zAxis = new float[3];
        FloatUtil.copyMatrixColumn(rotMat, 0, 0, xAxis, 0);
        FloatUtil.copyMatrixColumn(rotMat, 0, 1, yAxis, 0);
        FloatUtil.copyMatrixColumn(rotMat, 0, 2, zAxis, 0);

        final Quaternion quat1 = new Quaternion().setFromAxes(xAxis, yAxis, zAxis);
        Assert.assertEquals(quat0, quat1);
        final Quaternion quat2 = new Quaternion().setFromMatrix(rotMat, 0);
        Assert.assertEquals(quat2, quat1);

        quat1.toAxes(xAxis, yAxis, zAxis, rotMat);
        quat2.setFromAxes(xAxis, yAxis, zAxis);
        Assert.assertEquals(quat0, quat2);
        Assert.assertEquals(quat1, quat2);
    }

    @Test
    public void test25Slerp() {
        final Quaternion quat1 = new Quaternion();     // angle: 0 degrees
        final Quaternion quat2 = new Quaternion().rotateByAngleY(FloatUtil.HALF_PI); // angle: 90 degrees, axis Y

        float[] vecExp = new float[] { FloatUtil.sin(FloatUtil.QUARTER_PI), 0f, FloatUtil.sin(FloatUtil.QUARTER_PI) };
        final float[] vecHas = new float[3];
        final Quaternion quatS = new Quaternion();
        // System.err.println("Slerp #01: 1/2 * 90 degrees Y");
        quatS.setSlerp(quat1, quat2, 0.5f);
        quatS.rotateVector(vecHas, 0, UNIT_Z, 0);
        // System.err.println("exp0 "+Arrays.toString(vecExp));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(vecExp, vecHas)), Quaternion.ALLOWED_DEVIANCE);

        // delta == 100%
        quat2.setIdentity().rotateByAngleZ(FloatUtil.PI); // angle: 180 degrees, axis Z
        // System.err.println("Slerp #02: 1 * 180 degrees Z");
        quatS.setSlerp(quat1, quat2, 1.0f);
        quatS.rotateVector(vecHas, 0, UNIT_X, 0);
        // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(NEG_UNIT_X, vecHas)), Quaternion.ALLOWED_DEVIANCE);

        quat2.setIdentity().rotateByAngleZ(FloatUtil.PI); // angle: 180 degrees, axis Z
        // System.err.println("Slerp #03: 1/2 * 180 degrees Z");
        quatS.setSlerp(quat1, quat2, 0.5f);
        quatS.rotateVector(vecHas, 0, UNIT_X, 0);
        // System.err.println("exp0 "+Arrays.toString(UNIT_Y));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(UNIT_Y, vecHas)), Quaternion.ALLOWED_DEVIANCE);

        // delta == 0%
        quat2.setIdentity().rotateByAngleZ(FloatUtil.PI); // angle: 180 degrees, axis Z
        // System.err.println("Slerp #04: 0 * 180 degrees Z");
        quatS.setSlerp(quat1, quat2, 0.0f);
        quatS.rotateVector(vecHas, 0, UNIT_X, 0);
        // System.err.println("exp0 "+Arrays.toString(UNIT_X));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(UNIT_X, vecHas)), Quaternion.ALLOWED_DEVIANCE);

        // a==b
        quat2.setIdentity();
        // System.err.println("Slerp #05: 1/4 * 0 degrees");
        quatS.setSlerp(quat1, quat2, 0.25f); // 1/4 of identity .. NOP
        quatS.rotateVector(vecHas, 0, UNIT_X, 0);
        // System.err.println("exp0 "+Arrays.toString(UNIT_X));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(UNIT_X, vecHas)), Quaternion.ALLOWED_DEVIANCE);

        // negative dot product
        vecExp = new float[] { 0f, -FloatUtil.sin(FloatUtil.QUARTER_PI), FloatUtil.sin(FloatUtil.QUARTER_PI) };
        quat1.setIdentity().rotateByAngleX( -2f * FloatUtil.HALF_PI); // angle: -180 degrees, axis X
        quat2.setIdentity().rotateByAngleX(       FloatUtil.HALF_PI); // angle:   90 degrees, axis X
        // System.err.println("Slerp #06: 1/2 * 270 degrees");
        quatS.setSlerp(quat1, quat2, 0.5f);
        quatS.rotateVector(vecHas, 0, UNIT_Y, 0);
        // System.err.println("exp0 "+Arrays.toString(vecExp));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( VectorUtil.vec3Distance(vecExp, vecHas)), Quaternion.ALLOWED_DEVIANCE);


    }

    @Test
    public void test26LookAt() {
        final float[] direction = new float[3];
        final float[] xAxis = new float[3];
        final float[] yAxis = new float[3];
        final float[] zAxis = new float[3];
        final float[] vecHas = new float[3];

        if( DEBUG ) System.err.println("LookAt #01");
        VectorUtil.copyVec3(direction, 0, NEG_UNIT_X, 0);
        final Quaternion quat = new Quaternion().setLookAt(direction, UNIT_Y, xAxis, yAxis, zAxis);
        Assert.assertEquals(0f, VectorUtil.vec3Distance(direction, quat.rotateVector(vecHas, 0, UNIT_Z, 0)), Quaternion.ALLOWED_DEVIANCE);

        if( DEBUG ) System.err.println("LookAt #02");
        VectorUtil.normalizeVec3(VectorUtil.copyVec3(direction, 0, ONE, 0));
        quat.setLookAt(direction, UNIT_Y, xAxis, yAxis, zAxis);
        if( DEBUG )System.err.println("quat0 "+quat);
        quat.rotateVector(vecHas, 0, UNIT_Z, 0);
        if( DEBUG ) {
            System.err.println("xAxis "+Arrays.toString(xAxis)+", len "+VectorUtil.vec3Norm(xAxis));
            System.err.println("yAxis "+Arrays.toString(yAxis)+", len "+VectorUtil.vec3Norm(yAxis));
            System.err.println("zAxis "+Arrays.toString(zAxis)+", len "+VectorUtil.vec3Norm(zAxis));
            System.err.println("exp0 "+Arrays.toString(direction)+", len "+VectorUtil.vec3Norm(direction));
            System.err.println("has0 "+Arrays.toString(vecHas)+", len "+VectorUtil.vec3Norm(vecHas));
        }
        // Assert.assertEquals(0f, VectorUtil.distance(direction, quat.rotateVector(vecHas, 0, UNIT_Z, 0)), Quaternion.ALLOWED_DEVIANCE);
        Assert.assertEquals(0f, VectorUtil.vec3Distance(direction, vecHas), Quaternion.ALLOWED_DEVIANCE);

        if( DEBUG )System.err.println("LookAt #03");
        VectorUtil.normalizeVec3(VectorUtil.copyVec3(direction, 0, new float[] { -1f, 2f, -1f }, 0));
        quat.setLookAt(direction, UNIT_Y, xAxis, yAxis, zAxis);
        if( DEBUG )System.err.println("quat0 "+quat);
        quat.rotateVector(vecHas, 0, UNIT_Z, 0);
        if( DEBUG ) {
            System.err.println("xAxis "+Arrays.toString(xAxis)+", len "+VectorUtil.vec3Norm(xAxis));
            System.err.println("yAxis "+Arrays.toString(yAxis)+", len "+VectorUtil.vec3Norm(yAxis));
            System.err.println("zAxis "+Arrays.toString(zAxis)+", len "+VectorUtil.vec3Norm(zAxis));
            System.err.println("exp0 "+Arrays.toString(direction)+", len "+VectorUtil.vec3Norm(direction));
            System.err.println("has0 "+Arrays.toString(vecHas)+", len "+VectorUtil.vec3Norm(vecHas));
        }
        // Assert.assertEquals(0f, VectorUtil.distance(direction, quat.rotateVector(vecHas, 0, UNIT_Z, 0)), Quaternion.ALLOWED_DEVIANCE);
        Assert.assertEquals(0f, VectorUtil.vec3Distance(direction, vecHas), Quaternion.ALLOWED_DEVIANCE);
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestQuaternion01NOUI.class.getName());
    }
}
