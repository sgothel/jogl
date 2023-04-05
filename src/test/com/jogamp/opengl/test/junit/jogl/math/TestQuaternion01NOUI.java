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

import com.jogamp.junit.util.JunitTracer;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.math.Vec4f;
import com.jogamp.opengl.math.VectorUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestQuaternion01NOUI extends JunitTracer {
    static final boolean DEBUG = false;

    static final Quaternion QUAT_IDENT = new Quaternion(0f, 0f, 0f, 1f);

    static final Vec3f ZERO       = new Vec3f (  0f,  0f,  0f );
    static final Vec3f ONE        = new Vec3f (  1f,  1f,  1f );
    static final Vec3f NEG_ONE    = new Vec3f ( -1f, -1f, -1f );
    static final Vec3f UNIT_X     = new Vec3f (  1f,  0f,  0f );
    static final Vec3f UNIT_Y     = new Vec3f (  0f,  1f,  0f );
    static final Vec3f UNIT_Z     = new Vec3f (  0f,  0f,  1f );
    static final Vec3f NEG_UNIT_X = new Vec3f ( -1f,  0f,  0f );
    static final Vec3f NEG_UNIT_Y = new Vec3f (  0f, -1f,  0f );
    static final Vec3f NEG_UNIT_Z = new Vec3f (  0f,  0f, -1f );

    static final Vec4f NEG_ONE_v4 = new Vec4f ( -1f, -1f, -1f, 0f );
    static final Vec4f ONE_v4     = new Vec4f (  1f,  1f,  1f, 0f );

    static final float MACH_EPSILON = FloatUtil.EPSILON;

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
        final Vec3f ZERO = new Vec3f(0, 0, 0);
        final Vec3f rotVec0 = quat.rotateVector(ZERO, new Vec3f());
        Assert.assertEquals(ZERO, rotVec0);
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
        final Vec3f tmpV3f = new Vec3f();
        final Quaternion quat1 = new Quaternion().setFromAngleAxis(FloatUtil.HALF_PI, new Vec3f ( 2, 0, 0 ), tmpV3f );
        final Quaternion quat2 = new Quaternion().setFromAngleNormalAxis(FloatUtil.HALF_PI, new Vec3f ( 1, 0, 0 ) );

        Assert.assertEquals(quat2, quat1);
        // System.err.println("M "+quat2.magnitude()+", 1-M "+(1f-quat2.magnitude())+", Eps "+FloatUtil.EPSILON);
        Assert.assertEquals(0f, 1 - quat2.magnitude(), FloatUtil.EPSILON);
        Assert.assertTrue(1 - quat1.magnitude() <= FloatUtil.EPSILON);

        final Vec3f vecOut1 = new Vec3f();
        final Vec3f vecOut2 = new Vec3f();
        quat1.rotateVector(Vec3f.ONE, vecOut1);
        quat2.rotateVector(Vec3f.ONE, vecOut2);
        Assert.assertEquals(vecOut1, vecOut2);
        Assert.assertEquals(0f, Math.abs( vecOut1.dist(vecOut2) ), FloatUtil.EPSILON );

        quat1.rotateVector(Vec3f.UNIT_Z, vecOut1);
        Assert.assertEquals(0f, Math.abs( Vec3f.UNIT_Y_NEG.dist(vecOut1) ), FloatUtil.EPSILON );

        quat2.setFromAngleAxis(FloatUtil.HALF_PI, ZERO, tmpV3f);
        Assert.assertEquals(QUAT_IDENT, quat2);

        float angle = quat1.toAngleAxis(vecOut1);
        quat2.setFromAngleAxis(angle, vecOut1, tmpV3f);
        Assert.assertEquals(quat1, quat2);

        quat1.set(0, 0, 0, 0);
        angle = quat1.toAngleAxis(vecOut1);
        Assert.assertTrue(0.0f == angle);
        Assert.assertEquals(UNIT_X, vecOut1);
    }

    @Test
    public void test11FromVectorToVector() {
        final Vec3f tmp0V3f = new Vec3f();
        final Vec3f tmp1V3f = new Vec3f();
        final Vec3f vecOut = new Vec3f();
        final Quaternion quat = new Quaternion();
        quat.setFromVectors(UNIT_Z, UNIT_X, tmp0V3f, tmp1V3f);

        final Quaternion quat2 = new Quaternion();
        quat2.setFromNormalVectors(UNIT_Z, UNIT_X, tmp0V3f);
        Assert.assertEquals(quat, quat2);

        quat2.setFromAngleAxis(FloatUtil.HALF_PI, UNIT_Y, tmp0V3f);
        Assert.assertEquals(quat2, quat);

        quat.setFromVectors(UNIT_Z, NEG_UNIT_Z, tmp0V3f, tmp1V3f);
        quat.rotateVector(UNIT_Z, vecOut);
        // System.err.println("vecOut: "+Arrays.toString(vecOut));
        Assert.assertEquals( 0f, Math.abs( NEG_UNIT_Z.dist(vecOut) ), Quaternion.ALLOWED_DEVIANCE );

        quat.setFromVectors(UNIT_X, NEG_UNIT_X, tmp0V3f, tmp1V3f);
        quat.rotateVector(UNIT_X, vecOut);
        Assert.assertEquals( 0f, Math.abs( NEG_UNIT_X.dist(vecOut) ), Quaternion.ALLOWED_DEVIANCE );

        quat.setFromVectors(UNIT_Y, NEG_UNIT_Y, tmp0V3f, tmp1V3f);
        quat.rotateVector(UNIT_Y, vecOut);
        Assert.assertEquals( 0f, Math.abs( NEG_UNIT_Y.dist(vecOut) ), Quaternion.ALLOWED_DEVIANCE );

        quat.setFromVectors(ONE, NEG_ONE, tmp0V3f, tmp1V3f);
        quat.rotateVector(ONE, vecOut);
        Assert.assertEquals( 0f, Math.abs( NEG_ONE.dist(vecOut) ), Quaternion.ALLOWED_DEVIANCE );

        quat.setFromVectors(ZERO, ZERO, tmp0V3f, tmp1V3f);
        Assert.assertEquals(QUAT_IDENT, quat);
    }

    @Test
    public void test12FromAndToEulerAngles() {
        // Y.Z.X -> X.Y.Z
        final Quaternion quat = new Quaternion();
        final Vec3f angles0Exp = new Vec3f( 0f, FloatUtil.HALF_PI, 0f );
        quat.setFromEuler(angles0Exp);
        Assert.assertEquals(1.0f, quat.magnitude(), FloatUtil.EPSILON);

        final Vec3f angles0Has = quat.toEuler(new Vec3f());
        // System.err.println("exp0 "+Arrays.toString(angles0Exp));
        // System.err.println("has0 "+Arrays.toString(angles0Has));
        Assert.assertEquals(angles0Exp, angles0Has);

        final Quaternion quat2 = new Quaternion();
        quat2.setFromEuler(angles0Has);
        Assert.assertEquals(quat, quat2);

        ///

        final Vec3f angles1Exp = new Vec3f(0f, 0f, -FloatUtil.HALF_PI);
        quat.setFromEuler(angles1Exp);
        Assert.assertEquals(1.0f, quat.magnitude(), FloatUtil.EPSILON);

        final Vec3f angles1Has = quat.toEuler(new Vec3f());
        // System.err.println("exp1 "+Arrays.toString(angles1Exp));
        // System.err.println("has1 "+Arrays.toString(angles1Has));
        Assert.assertEquals(angles1Exp, angles1Has);

        quat2.setFromEuler(angles1Has);
        Assert.assertEquals(quat, quat2);

        ///

        final Vec3f angles2Exp = new Vec3f(FloatUtil.HALF_PI, 0f, 0f);
        quat.setFromEuler(angles2Exp);
        Assert.assertEquals(1.0f, quat.magnitude(), FloatUtil.EPSILON);

        final Vec3f angles2Has = quat.toEuler(new Vec3f());
        // System.err.println("exp2 "+Arrays.toString(angles2Exp));
        // System.err.println("has2 "+Arrays.toString(angles2Has));
        Assert.assertEquals(angles2Exp, angles2Has);

        quat2.setFromEuler(angles2Has);
        Assert.assertEquals(quat, quat2);
    }

    @Test
    public void test13FromEulerAnglesAndRotateVector() {
        final Quaternion quat = new Quaternion();
        quat.setFromEuler(0, FloatUtil.HALF_PI, 0); // 90 degrees y-axis
        Assert.assertEquals(1.0f, quat.magnitude(), FloatUtil.EPSILON);

        final Vec3f v2 = quat.rotateVector(UNIT_X, new Vec3f());
        Assert.assertEquals(0f, Math.abs( NEG_UNIT_Z.dist(v2)), FloatUtil.EPSILON);

        quat.setFromEuler(0, 0, -FloatUtil.HALF_PI);
        Assert.assertEquals(1.0f, quat.magnitude(), FloatUtil.EPSILON);
        quat.rotateVector(UNIT_X, v2);
        Assert.assertEquals(0f, Math.abs( NEG_UNIT_Y.dist(v2)), FloatUtil.EPSILON);

        quat.setFromEuler(FloatUtil.HALF_PI, 0, 0);
        Assert.assertEquals(1.0f, quat.magnitude(), FloatUtil.EPSILON);
        quat.rotateVector(UNIT_Y, v2);
        Assert.assertEquals(0f, Math.abs( UNIT_Z.dist(v2)), FloatUtil.EPSILON);
    }

    @Test
    public void test14Matrix() {
        final Vec3f vecHas = new Vec3f();
        final Vec3f vecOut3 = new Vec3f();
        final Vec4f vecOut4 = new Vec4f();
        final Matrix4f mat1 = new Matrix4f();;
        final Matrix4f mat2 = new Matrix4f();
        final Quaternion quat = new Quaternion();

        //
        // IDENTITY CHECK
        //
        mat1.loadIdentity();
        quat.set(0, 0, 0, 0);
        quat.toMatrix(mat2);
        Assert.assertEquals(mat1, mat2);

        //
        // 90 degrees rotation on X
        //

        float a = FloatUtil.HALF_PI;
        final float[] mat1_0 = new float[] { // Column Order
            1,  0,                 0,                0, //
            0,  FloatUtil.cos(a),  FloatUtil.sin(a), 0, //
            0, -FloatUtil.sin(a),  FloatUtil.cos(a), 0,
            0,  0,                 0,                1  };
        mat1.load( mat1_0 );
        {
            // Matrix4f load() <-> toFloats()
            final float[] mat2_0 = new float[16];
            mat1.get(mat2_0);
            Assert.assertArrayEquals(mat1_0, mat2_0, FloatUtil.EPSILON);
        }
        {
            // Validate Matrix via Euler rotation on Quaternion!
            quat.setFromEuler(a, 0f, 0f);
            {
                // quat.toMatrix(float[])
                final float[] mat2_0 = new float[16];
                quat.toMatrix(mat2_0, 0);
                Assert.assertArrayEquals(mat1_0, mat2_0, FloatUtil.EPSILON);
            }
            {
                // quat.toMatrix(float[]) and Matrix4f.load()
                final float[] mat2_0 = new float[16];
                quat.toMatrix(mat2_0, 0);
                Assert.assertArrayEquals(mat1_0, mat2_0, FloatUtil.EPSILON);
                mat2.load(mat2_0);
                Assert.assertEquals(mat1, mat2);
            }
            {
                // Quaternion.toMatrix(Matrix4f)
                quat.toMatrix(mat2);
                // System.err.println(FloatUtil.matrixToString(null, "quat-rot", "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
                Assert.assertEquals(mat1, mat2);
            }
            quat.rotateVector(UNIT_Y, vecHas);
            // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
            // System.err.println("has0 "+Arrays.toString(vecHas));
            Assert.assertEquals( 0f, Math.abs( UNIT_Z.dist(vecHas) ), Quaternion.ALLOWED_DEVIANCE );
        }
        mat1.getRotation(quat);
        quat.setFromMatrix(mat1);
        quat.rotateVector(UNIT_Y, vecHas);
        // System.err.println("exp0 "+Arrays.toString(UNIT_Z));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( UNIT_Z.dist(vecHas) ), Quaternion.ALLOWED_DEVIANCE );

        quat.toMatrix(mat2);
        // System.err.println(FloatUtil.matrixToString(null, null, "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
        Assert.assertEquals(mat1, mat2);

        quat.rotateVector(NEG_ONE, vecHas);
        {
            // 1st use float[] math
            final float[] vecHas_0 = new float[3];
            vecHas.get(vecHas_0);
            final float[] mat2_0 = new float[16];
            quat.toMatrix(mat2_0, 0);
            final float[] NEG_ONE_0 = new float[3];
            NEG_ONE.get(NEG_ONE_0);
            final float[] vecOut3_0 = new float[3];
            FloatUtil.multMatrixVec3(mat2_0, NEG_ONE_0, vecOut3_0);
            Assert.assertEquals( 0f, Math.abs( VectorUtil.distVec3(vecHas_0, vecOut3_0) ), Quaternion.ALLOWED_DEVIANCE );
            Assert.assertArrayEquals(vecHas_0, vecOut3_0, FloatUtil.EPSILON);

            // 2nd use Vec3f math
            mat2.mulVec3f(NEG_ONE, vecOut3);
            Assert.assertEquals( 0f, Math.abs( vecHas.dist(vecOut3) ), Quaternion.ALLOWED_DEVIANCE );
            Assert.assertEquals(vecHas, vecOut3);

            // 3rd compare both
            final float[] vecOut3_1 = new float[3];
            vecOut3.get(vecOut3_1);
            Assert.assertArrayEquals(vecOut3_0, vecOut3_1, FloatUtil.EPSILON);
        }
        {
            // 1st use float[] math
            final float[] vecHas_0 = new float[4];
            vecHas.get(vecHas_0); // w is 0
            final float[] mat2_0 = new float[16];
            quat.toMatrix(mat2_0, 0);
            final float[] NEG_ONE_v4_0 = new float[4];
            NEG_ONE_v4.get(NEG_ONE_v4_0);
            final float[] vecOut4_0 = new float[4];
            FloatUtil.multMatrixVec(mat2_0, NEG_ONE_v4_0, vecOut4_0);
            Assert.assertEquals( 0f, Math.abs( VectorUtil.distVec3(vecHas_0, vecOut4_0) ), Quaternion.ALLOWED_DEVIANCE );
            Assert.assertArrayEquals(vecHas_0, vecOut4_0, FloatUtil.EPSILON);

            // 2nd use Vec4f math
            mat2.mulVec4f(NEG_ONE_v4, vecOut4);
            vecOut3.set(vecOut4);
            Assert.assertEquals( 0f, Math.abs( vecHas.dist(vecOut3) ), Quaternion.ALLOWED_DEVIANCE );
            Assert.assertEquals(vecHas, vecOut3);

            // 3rd compare both
            final float[] vecOut4_1 = new float[4];
            vecOut4.get(vecOut4_1);
            Assert.assertArrayEquals(vecOut4_0, vecOut4_1, FloatUtil.EPSILON);
        }

        //
        // 180 degrees rotation on X
        //
        a = FloatUtil.PI;
        mat1.load( new float[] { // Column Order
                1,  0,                 0,                0, //
                0,   FloatUtil.cos(a), FloatUtil.sin(a), 0, //
                0,  -FloatUtil.sin(a), FloatUtil.cos(a), 0,
                0,  0,                 0,                1 } );
        {
            // Validate Matrix via Euler rotation on Quaternion!
            quat.setFromEuler(a, 0f, 0f);
            quat.toMatrix(mat2);
            // System.err.println(FloatUtil.matrixToString(null, "quat-rot", "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
            Assert.assertEquals(mat1, mat2);
            quat.rotateVector(UNIT_Y, vecHas);
            // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
            // System.err.println("has0 "+Arrays.toString(vecHas));
            Assert.assertEquals( 0f, Math.abs( NEG_UNIT_Y.dist(vecHas) ), Quaternion.ALLOWED_DEVIANCE );
        }
        quat.setFromMatrix(mat1);
        quat.rotateVector(UNIT_Y, vecHas);
        // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_Y));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( NEG_UNIT_Y.dist(vecHas) ), Quaternion.ALLOWED_DEVIANCE );

        quat.toMatrix(mat2);
        // System.err.println(FloatUtil.matrixToString(null, null, "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
        Assert.assertEquals(mat1, mat2);

        quat.rotateVector(ONE, vecHas);
        mat2.mulVec4f(ONE_v4, vecOut4);
        vecOut3.set(vecOut4);
        Assert.assertEquals( 0f, Math.abs( vecHas.dist(vecOut3) ), Quaternion.ALLOWED_DEVIANCE );

        //
        // 180 degrees rotation on Y
        //
        a = FloatUtil.PI;
        mat1.load( new float[] { // Column Order
                 FloatUtil.cos(a), 0,  -FloatUtil.sin(a), 0, //
                 0,                1,   0,                0, //
                 FloatUtil.sin(a), 0,   FloatUtil.cos(a), 0,
                 0,                0,   0,                1 } );
        {
            // Validate Matrix via Euler rotation on Quaternion!
            quat.setFromEuler(0f, a, 0f);
            quat.toMatrix(mat2);
            // System.err.println(FloatUtil.matrixToString(null, "quat-rot", "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
            Assert.assertEquals(mat1, mat2);
            quat.rotateVector(UNIT_X, vecHas);
            // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
            // System.err.println("has0 "+Arrays.toString(vecHas));
            Assert.assertEquals( 0f, Math.abs( NEG_UNIT_X.dist(vecHas) ), Quaternion.ALLOWED_DEVIANCE );
        }
        quat.setFromMatrix(mat1);
        quat.rotateVector(UNIT_X, vecHas);
        // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( NEG_UNIT_X.dist(vecHas) ), Quaternion.ALLOWED_DEVIANCE );

        quat.toMatrix(mat2);
        // System.err.println(FloatUtil.matrixToString(null, "matr-rot", "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
        Assert.assertEquals(mat1, mat2);

        quat.rotateVector(NEG_ONE, vecHas);
        mat2.mulVec4f(NEG_ONE_v4, vecOut4);
        vecOut3.set(vecOut4);
        Assert.assertEquals( 0f, Math.abs( vecHas.dist(vecOut3) ), Quaternion.ALLOWED_DEVIANCE );

        //
        // 180 degrees rotation on Z
        //
        a = FloatUtil.PI;
        mat1.load( new float[] { // Column Order
                  FloatUtil.cos(a), FloatUtil.sin(a), 0, 0, //
                 -FloatUtil.sin(a), FloatUtil.cos(a), 0, 0,
                  0,                0,                1, 0,
                  0,                0,                0, 1 } );
        {
            // Validate Matrix via Euler rotation on Quaternion!
            quat.setFromEuler(0f, 0f, a);
            quat.toMatrix(mat2);
            // System.err.println(FloatUtil.matrixToString(null, "quat-rot", "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
            Assert.assertEquals(mat1, mat2);
            quat.rotateVector(UNIT_X, vecHas);
            // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
            // System.err.println("has0 "+Arrays.toString(vecHas));
            Assert.assertEquals( 0f, Math.abs( NEG_UNIT_X.dist(vecHas) ), Quaternion.ALLOWED_DEVIANCE );
        }
        quat.setFromMatrix(mat1);
        quat.rotateVector(UNIT_X, vecHas);
        // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( NEG_UNIT_X.dist(vecHas) ), Quaternion.ALLOWED_DEVIANCE );

        quat.toMatrix(mat2);
        // System.err.println(FloatUtil.matrixToString(null, "matr-rot", "%10.5f", mat1, 0, mat2, 0, 4, 4, false).toString());
        Assert.assertEquals(mat1, mat2);

        quat.rotateVector(ONE, vecHas);
        mat2.mulVec4f(ONE_v4, vecOut4);
        vecOut3.set(vecOut4);
        Assert.assertEquals( 0f, Math.abs( vecHas.dist(vecOut3) ), Quaternion.ALLOWED_DEVIANCE );

        //
        // Test Matrix-Columns
        //

        a = FloatUtil.QUARTER_PI;
        final Vec3f vecExp0 = new Vec3f( FloatUtil.cos(a), FloatUtil.sin(a), 0);
        final Vec3f vecExp1 = new Vec3f(-FloatUtil.sin(a), FloatUtil.cos(a), 0);
        final Vec3f vecExp2 = new Vec3f( 0,                0,                1);
        final Vec3f vecCol = new Vec3f();
        mat1.load( new float[] { // Column Order
                  FloatUtil.cos(a), FloatUtil.sin(a), 0, 0, //
                 -FloatUtil.sin(a), FloatUtil.cos(a), 0, 0,
                  0,                0,                1, 0,
                  0,                0,                0, 1 } );
        mat1.getColumn(0, vecCol);
        // System.err.println("exp0 "+Arrays.toString(vecExp));
        // System.err.println("has0 "+Arrays.toString(vecCol))
        Assert.assertEquals(vecExp0, vecCol);
        Assert.assertEquals(0f, Math.abs( vecExp0.dist(vecCol)), FloatUtil.EPSILON);

        mat1.getColumn(1, vecCol);
        Assert.assertEquals(vecExp1, vecCol);
        Assert.assertEquals(0f, Math.abs( vecExp1.dist(vecCol)), FloatUtil.EPSILON);

        mat1.getColumn(2, vecCol);
        Assert.assertEquals(vecExp2, vecCol);
        Assert.assertEquals(0f, Math.abs( vecExp2.dist(vecCol)), FloatUtil.EPSILON);
    }

    @Test
    public void test15aAxesAndMatrix() {
        final Vec3f eulerExp = new Vec3f ( 0f, FloatUtil.HALF_PI, 0f ); // 45 degr on X, 90 degr on Y
        final Matrix4f matExp1 = new Matrix4f();
        matExp1.setToRotationEuler(eulerExp.x(), eulerExp.y(), eulerExp.z());
        {
            final float[] matExp0 = new float[4*4];
            FloatUtil.makeRotationEuler(matExp0, 0, eulerExp.x(), eulerExp.y(), eulerExp.z());
            final Matrix4f matExp0b = new Matrix4f();
            matExp0b.load(matExp0);
            Assert.assertEquals(matExp0b, matExp1);
        }

        final Matrix4f matHas = new Matrix4f();;
        final Quaternion quat1 = new Quaternion();
        quat1.setFromEuler(eulerExp);
        quat1.toMatrix(matHas);
        // System.err.println(FloatUtil.matrixToString(null, "exp-has", "%10.5f", matExp, 0, matHas, 0, 4, 4, false).toString());
        Assert.assertEquals(matExp1, matHas);

        final Vec3f eulerHas = new Vec3f();
        final Quaternion quat2 = new Quaternion();
        quat2.setFromMatrix(matExp1);
        quat2.toEuler(eulerHas);
        // System.err.println("exp-euler "+Arrays.toString(eulerExp));
        // System.err.println("has-euler "+Arrays.toString(eulerHas));
        Assert.assertEquals(eulerExp, eulerHas);

        Assert.assertEquals(quat2, quat1);

        final Vec3f angles = new Vec3f();
        quat2.toEuler(angles);
        quat1.setFromEuler(angles);
        Assert.assertEquals(quat2, quat1);
    }

    @Test
    public void test15bAxesAndMatrix() {
        final Vec3f eulerExp = new Vec3f(FloatUtil.HALF_PI, 0f, 0f);
        final Matrix4f matExp = new Matrix4f();
        matExp.setToRotationEuler(eulerExp.x(), eulerExp.y(), eulerExp.z()); // 45 degr on X, 90 degr on Y (?)
        {
            final float[] matExp_b0 = new float[4*4];
            FloatUtil.makeRotationEuler(matExp_b0, 0, eulerExp.x(), eulerExp.y(), eulerExp.z());
            final Matrix4f matExp_b = new Matrix4f();
            matExp_b.load(matExp_b0);
            Assert.assertEquals(matExp_b, matExp);

            final float[] matExp_b1 = new float[16];
            matExp.get(matExp_b1);
            Assert.assertArrayEquals(matExp_b0, matExp_b1, FloatUtil.EPSILON);
        }

        final Matrix4f matHas = new Matrix4f();
        final Quaternion quat1 = new Quaternion();
        quat1.setFromEuler(eulerExp);
        quat1.toMatrix(matHas);
        // System.err.println(FloatUtil.matrixToString(null, "exp-has", "%10.5f", matExp, 0, matHas, 0, 4, 4, false).toString());
        Assert.assertEquals(matExp, matHas);

        final Vec3f eulerHas = new Vec3f();
        final Quaternion quat2 = new Quaternion();
        quat2.setFromMatrix(matExp);
        quat2.toEuler(eulerHas);
        // System.err.println("exp-euler "+Arrays.toString(eulerExp));
        // System.err.println("has-euler "+Arrays.toString(eulerHas));
        Assert.assertEquals(eulerExp, eulerHas);

        Assert.assertEquals(quat2, quat1);

        final Vec3f angles = new Vec3f();
        quat2.toEuler(angles);
        quat1.setFromEuler(angles);
        Assert.assertEquals(quat2, quat1);
    }

    @Test
    public void test15cAxesAndMatrix() {
        final Vec3f eulerExp1 = new Vec3f(FloatUtil.QUARTER_PI, FloatUtil.HALF_PI, 0f); // 45 degr on X, 90 degr on Y
        final float[] eulerExp0 = new float[3];
        eulerExp1.get(eulerExp0);

        final Matrix4f matExp = new Matrix4f();
        matExp.setToRotationEuler(eulerExp1.x(), eulerExp1.y(), eulerExp1.z());
        {
            final float[] matExp_b0 = new float[4*4];
            FloatUtil.makeRotationEuler(matExp_b0, 0, eulerExp1.x(), eulerExp1.y(), eulerExp1.z());
            final Matrix4f matExp_b = new Matrix4f();
            matExp_b.load(matExp_b0);
            Assert.assertEquals(matExp_b, matExp);

            final float[] matExp_b1 = new float[16];
            matExp.get(matExp_b1);
            Assert.assertArrayEquals(matExp_b0, matExp_b1, FloatUtil.EPSILON);

            matExp.get(matExp_b0);
            final Quaternion quat2 = new Quaternion();
            quat2.setFromMatrix(matExp);
            quat2.toMatrix(matExp_b1, 0);
            Assert.assertArrayEquals(matExp_b0, matExp_b1, FloatUtil.EPSILON);
            quat2.toMatrix(matExp_b);
            Assert.assertEquals(matExp, matExp_b);
        }

        final Matrix4f matHas = new Matrix4f();
        final Quaternion quat1 = new Quaternion();
        quat1.setFromEuler(eulerExp1);
        quat1.toMatrix(matHas);
        // System.err.println(FloatUtil.matrixToString(null, "exp-has", "%10.5f", matExp, 0, matHas, 0, 4, 4, false).toString());
        Assert.assertEquals(matExp, matHas);

        final Vec3f eulerHas1 = new Vec3f();
        final Quaternion quat2 = new Quaternion();
        quat2.setFromMatrix(matExp);
        quat2.toEuler(eulerHas1); // Vec3f
        if( DEBUG ) {
            System.err.println("PI");
            System.err.printf("  double %20.20f%n", Math.PI);
            System.err.printf("   float %20.20f%n", FloatUtil.PI);
            System.err.printf("    diff %20.20f%n", (Math.PI - FloatUtil.PI));
            System.err.println("PI/2");
            System.err.printf("  double %20.20f%n", Math.PI/2f);
            System.err.printf("   float %20.20f%n", FloatUtil.HALF_PI);
            System.err.printf("    diff %20.20f%n", (Math.PI/2f - FloatUtil.HALF_PI));

            System.err.println("exp-euler "+eulerExp1);
            System.err.println("has-euler1 "+eulerHas1);
            System.err.println("dif-euler1 "+eulerExp1.minus(eulerHas1));
        }
        {
            final float[] eulerHas0 = new float[3];
            eulerHas1.get(eulerHas0);
            Assert.assertArrayEquals(eulerExp0, eulerHas0, FloatUtil.EPSILON);
        }
        Assert.assertTrue(eulerExp1+" != "+eulerHas1, eulerExp1.isEqual(eulerHas1, Quaternion.ALLOWED_DEVIANCE));
        // Assert.assertEquals(eulerExp1, eulerHas1); // `diff < EPSILON` criteria hits, while `Assert.assertArrayEquals(..)` uses `diff <= EPSILON`

        Assert.assertEquals(quat2, quat1);

        final Vec3f angles = new Vec3f();
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

        final Vec3f vecOut = new Vec3f();
        quat2.rotateVector(UNIT_Z, vecOut);
        Assert.assertTrue( Math.abs( UNIT_X.dist(vecOut)) <= Quaternion.ALLOWED_DEVIANCE);

        quat2.setFromAngleNormalAxis(FloatUtil.HALF_PI, UNIT_Y); // 90 degr on Y
        quat1.mult(quat1); // q1 = q1 * q1 -> 2 * 45 degr ->  90 degr on Y
        quat1.mult(quat2); // q1 = q1 * q2 -> 2 * 90 degr -> 180 degr on Y
        quat1.rotateVector(UNIT_Z, vecOut);
        Assert.assertTrue( Math.abs( NEG_UNIT_Z.dist(vecOut)) <= Quaternion.ALLOWED_DEVIANCE);

        quat2.setFromEuler(0f, FloatUtil.HALF_PI, 0f);
        quat1.mult(quat2); // q1 = q1 * q2 = q1 * rotMat(0, 90degr, 0)
        quat1.rotateVector(UNIT_Z, vecOut);
        Assert.assertTrue( Math.abs( NEG_UNIT_X.dist(vecOut)) <= Quaternion.ALLOWED_DEVIANCE);
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


        final Vec3f vecExp = new Vec3f();
        final Vec3f vecRot = new Vec3f();
        final Quaternion quat = new Quaternion();

        // Try a new way with new angles...
        quat.setFromEuler(FloatUtil.HALF_PI, FloatUtil.QUARTER_PI, FloatUtil.PI);
        vecRot.set(1f, 1f, 1f);
        quat.rotateVector(vecRot, vecRot);

        // expected
        vecExp.set(1f, 1f, 1f);
        final Quaternion worker = new Quaternion();
        // put together matrix, then apply to vector, so YZX
        worker.rotateByAngleY(FloatUtil.QUARTER_PI).rotateByAngleZ(FloatUtil.PI).rotateByAngleX(FloatUtil.HALF_PI);
        quat.rotateVector(vecExp, vecExp);
        Assert.assertEquals(0f, vecExp.dist(vecRot), FloatUtil.EPSILON);
        Assert.assertEquals(vecExp, vecRot);

        // test axis rotation methods against general purpose
        // X AXIS
        vecExp.set(1f, 1f, 1f);
        vecRot.set(1f, 1f, 1f);
        worker.setIdentity().rotateByAngleX(FloatUtil.QUARTER_PI).rotateVector(vecExp, vecExp);
        worker.setIdentity().rotateByAngleNormalAxis(FloatUtil.QUARTER_PI, 1f, 0f, 0f).rotateVector(vecRot, vecRot);
        // System.err.println("exp0 "+Arrays.toString(vecExp)+", len "+VectorUtil.length(vecExp));
        // System.err.println("has0 "+Arrays.toString(vecRot)+", len "+VectorUtil.length(vecRot));
        Assert.assertEquals(0f, vecExp.dist(vecRot), FloatUtil.EPSILON);
        Assert.assertEquals(vecExp, vecRot);

        // Y AXIS
        vecExp.set(1f, 1f, 1f);
        vecRot.set(1f, 1f, 1f);
        worker.setIdentity().rotateByAngleY(FloatUtil.QUARTER_PI).rotateVector(vecExp, vecExp);
        worker.setIdentity().rotateByAngleNormalAxis(FloatUtil.QUARTER_PI, 0f, 1f, 0f).rotateVector(vecRot, vecRot);
        // System.err.println("exp0 "+Arrays.toString(vecExp));
        // System.err.println("has0 "+Arrays.toString(vecRot));
        Assert.assertEquals(0f, vecExp.dist(vecRot), FloatUtil.EPSILON);
        Assert.assertEquals(vecExp, vecRot);

        // Z AXIS
        vecExp.set(1f, 1f, 1f);
        vecRot.set(1f, 1f, 1f);
        worker.setIdentity().rotateByAngleZ(FloatUtil.QUARTER_PI).rotateVector(vecExp, vecExp);
        worker.setIdentity().rotateByAngleNormalAxis(FloatUtil.QUARTER_PI, 0f, 0f, 1f).rotateVector(vecRot, vecRot);
        // System.err.println("exp0 "+Arrays.toString(vecExp));
        // System.err.println("has0 "+Arrays.toString(vecRot));
        Assert.assertEquals(0f, vecExp.dist(vecRot), FloatUtil.EPSILON);
        Assert.assertEquals(vecExp, vecRot);

        quat.set(worker);
        worker.rotateByAngleNormalAxis(0f, 0f, 0f, 0f);
        Assert.assertEquals(quat, worker);
    }

    @Test
    public void test24Axes() {
        final Quaternion quat0 = new Quaternion().rotateByAngleX(FloatUtil.QUARTER_PI).rotateByAngleY(FloatUtil.HALF_PI);
        final Matrix4f rotMat = new Matrix4f();
        quat0.toMatrix(rotMat);
        final Vec3f xAxis = new Vec3f();
        final Vec3f yAxis = new Vec3f();
        final Vec3f zAxis = new Vec3f();
        rotMat.getColumn(0, xAxis);
        rotMat.getColumn(1, yAxis);
        rotMat.getColumn(2, zAxis);

        final Quaternion quat1 = new Quaternion().setFromAxes(xAxis, yAxis, zAxis);
        Assert.assertEquals(quat0, quat1);
        final Quaternion quat2 = new Quaternion().setFromMatrix(rotMat);
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

        final Vec3f vecExp = new Vec3f( FloatUtil.sin(FloatUtil.QUARTER_PI), 0f, FloatUtil.sin(FloatUtil.QUARTER_PI) );
        final Vec3f vecHas = new Vec3f();
        final Quaternion quatS = new Quaternion();
        // System.err.println("Slerp #01: 1/2 * 90 degrees Y");
        quatS.setSlerp(quat1, quat2, 0.5f);
        quatS.rotateVector(UNIT_Z, vecHas);
        // System.err.println("exp0 "+Arrays.toString(vecExp));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( vecExp.dist(vecHas)), Quaternion.ALLOWED_DEVIANCE);
        if( !vecExp.equals(vecHas) ) {
            System.err.println("Deviance: "+vecExp+", "+vecHas+": "+vecExp.minus(vecHas)+", dist "+vecExp.dist(vecHas));
        }
        // Assert.assertEquals(vecExp, vecHas);

        // delta == 100%
        quat2.setIdentity().rotateByAngleZ(FloatUtil.PI); // angle: 180 degrees, axis Z
        // System.err.println("Slerp #02: 1 * 180 degrees Z");
        quatS.setSlerp(quat1, quat2, 1.0f);
        quatS.rotateVector(UNIT_X, vecHas);
        // System.err.println("exp0 "+Arrays.toString(NEG_UNIT_X));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( NEG_UNIT_X.dist(vecHas)), Quaternion.ALLOWED_DEVIANCE);
        Assert.assertEquals(NEG_UNIT_X, vecHas);

        quat2.setIdentity().rotateByAngleZ(FloatUtil.PI); // angle: 180 degrees, axis Z
        // System.err.println("Slerp #03: 1/2 * 180 degrees Z");
        quatS.setSlerp(quat1, quat2, 0.5f);
        quatS.rotateVector(UNIT_X, vecHas);
        // System.err.println("exp0 "+Arrays.toString(UNIT_Y));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( UNIT_Y.dist(vecHas)), Quaternion.ALLOWED_DEVIANCE);
        if( !UNIT_Y.equals(vecHas) ) {
            System.err.println("Deviance: "+UNIT_Y+", "+vecHas+": "+UNIT_Y.minus(vecHas)+", dist "+UNIT_Y.dist(vecHas));
        }
        // Assert.assertEquals(UNIT_Y, vecHas);

        // delta == 0%
        quat2.setIdentity().rotateByAngleZ(FloatUtil.PI); // angle: 180 degrees, axis Z
        // System.err.println("Slerp #04: 0 * 180 degrees Z");
        quatS.setSlerp(quat1, quat2, 0.0f);
        quatS.rotateVector(UNIT_X, vecHas);
        // System.err.println("exp0 "+Arrays.toString(UNIT_X));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( UNIT_X.dist(vecHas)), Quaternion.ALLOWED_DEVIANCE);
        Assert.assertEquals(UNIT_X, vecHas);

        // a==b
        quat2.setIdentity();
        // System.err.println("Slerp #05: 1/4 * 0 degrees");
        quatS.setSlerp(quat1, quat2, 0.25f); // 1/4 of identity .. NOP
        quatS.rotateVector(UNIT_X, vecHas);
        // System.err.println("exp0 "+Arrays.toString(UNIT_X));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( UNIT_X.dist(vecHas)), Quaternion.ALLOWED_DEVIANCE);
        Assert.assertEquals(UNIT_X, vecHas);

        // negative dot product
        vecExp.set(0f, -FloatUtil.sin(FloatUtil.QUARTER_PI), FloatUtil.sin(FloatUtil.QUARTER_PI));
        quat1.setIdentity().rotateByAngleX( -2f * FloatUtil.HALF_PI); // angle: -180 degrees, axis X
        quat2.setIdentity().rotateByAngleX(       FloatUtil.HALF_PI); // angle:   90 degrees, axis X
        // System.err.println("Slerp #06: 1/2 * 270 degrees");
        quatS.setSlerp(quat1, quat2, 0.5f);
        quatS.rotateVector(UNIT_Y, vecHas);
        // System.err.println("exp0 "+Arrays.toString(vecExp));
        // System.err.println("has0 "+Arrays.toString(vecHas));
        Assert.assertEquals( 0f, Math.abs( vecExp.dist(vecHas) ), Quaternion.ALLOWED_DEVIANCE);
        if( !vecExp.equals(vecHas) ) {
            System.err.println("Deviance: "+vecExp+", "+vecHas+": "+vecExp.minus(vecHas)+", dist "+vecExp.dist(vecHas));
        }
        // Assert.assertEquals(vecExp, vecHas);
    }

    @Test
    public void test26LookAt() {
        final Vec3f direction = new Vec3f();
        final Vec3f xAxis = new Vec3f();
        final Vec3f yAxis = new Vec3f();
        final Vec3f zAxis = new Vec3f();
        final Vec3f vecHas = new Vec3f();

        if( DEBUG ) System.err.println("LookAt #01");
        direction.set(NEG_UNIT_X);
        final Quaternion quat = new Quaternion().setLookAt(direction, UNIT_Y, xAxis, yAxis, zAxis);
        Assert.assertEquals(0f, direction.dist( quat.rotateVector(UNIT_Z, vecHas) ), Quaternion.ALLOWED_DEVIANCE);
        Assert.assertEquals(direction, vecHas);

        if( DEBUG ) System.err.println("LookAt #02");
        direction.set(ONE).normalize();
        quat.setLookAt(direction, UNIT_Y, xAxis, yAxis, zAxis);
        if( DEBUG )System.err.println("quat0 "+quat);
        quat.rotateVector(UNIT_Z, vecHas);
        if( DEBUG ) {
            System.err.println("xAxis "+xAxis+", len "+xAxis.length());
            System.err.println("yAxis "+yAxis+", len "+yAxis.length());
            System.err.println("zAxis "+zAxis+", len "+zAxis.length());
            System.err.println("exp0 "+direction+", len "+direction.length());
            System.err.println("has0 "+vecHas+", len "+vecHas.length());
        }
        // Assert.assertEquals(0f, VectorUtil.distance(direction, quat.rotateVector(vecHas, 0, UNIT_Z, 0)), Quaternion.ALLOWED_DEVIANCE);
        Assert.assertEquals(0f, direction.dist(vecHas), Quaternion.ALLOWED_DEVIANCE);
        Assert.assertEquals(direction, vecHas);

        if( DEBUG )System.err.println("LookAt #03");
        direction.set(-1f, 2f, -1f).normalize();
        quat.setLookAt(direction, UNIT_Y, xAxis, yAxis, zAxis);
        if( DEBUG )System.err.println("quat0 "+quat);
        quat.rotateVector(UNIT_Z, vecHas);
        if( DEBUG ) {
            System.err.println("xAxis "+xAxis+", len "+xAxis.length());
            System.err.println("yAxis "+yAxis+", len "+yAxis.length());
            System.err.println("zAxis "+zAxis+", len "+zAxis.length());
            System.err.println("exp0 "+direction+", len "+direction.length());
            System.err.println("has0 "+vecHas+", len "+vecHas.length());
        }
        // Assert.assertEquals(0f, VectorUtil.distance(direction, quat.rotateVector(vecHas, 0, UNIT_Z, 0)), Quaternion.ALLOWED_DEVIANCE);
        Assert.assertEquals(0f, direction.dist(vecHas), Quaternion.ALLOWED_DEVIANCE);
        if( !direction.equals(vecHas) ) {
            System.err.println("Deviance: "+direction+", "+vecHas+": "+direction.minus(vecHas)+", dist "+direction.dist(vecHas));
        }
        // Assert.assertEquals(direction, vecHas);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestQuaternion01NOUI.class.getName());
    }
}
