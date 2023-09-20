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
package com.jogamp.opengl.test.junit.math;

import java.util.Arrays;

import com.jogamp.common.nio.Buffers;
import com.jogamp.junit.util.JunitTracer;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Matrix4f;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec3f;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;

import jogamp.opengl.gl2.ProjectDouble;

import com.jogamp.opengl.util.PMVMatrix;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestMatrix4fProject01NOUI extends JunitTracer {

    static final float epsilon = 0.00001f;

    // Simple 10 x 10 view port
    static final Recti viewport = new Recti(0,0,10,10);
    static final int[] viewport_i4 = new int[] { 0, 0, 10, 10 };

    /**
     * PMVMatrix w/ separate P + Mv vs Matrix4f.mapObjToWin() w/ single PMv
     *
     * Both using same Matrix4f.mapObjToWin(..).
     */
    @Test
    public void test01PMVMatrixToMatrix4f() {
        final Vec3f winA00 = new Vec3f();
        final Vec3f winA01 = new Vec3f();
        final Vec3f winA10 = new Vec3f();
        final Vec3f winA11 = new Vec3f();
        final Vec3f winB00 = new Vec3f();
        final Vec3f winB01 = new Vec3f();
        final Vec3f winB10 = new Vec3f();
        final Vec3f winB11 = new Vec3f();

        final PMVMatrix m = new PMVMatrix();
        final Matrix4f mat4PMv = new Matrix4f();
        m.getMulPMv(mat4PMv);
        System.err.println(mat4PMv.toString(null, "mat4PMv", "%10.5f"));

        m.mapObjToWin(new Vec3f(1f, 0f, 0f), viewport, winA00); // separate P + Mv
        System.err.println("A.0.0 - Project 1,0 -->" + winA00);
        Matrix4f.mapObjToWin(new Vec3f(1f, 0f, 0f), mat4PMv, viewport, winB00); // single PMv
        System.err.println("B.0.0 - Project 1,0 -->" + winB00);

        m.mapObjToWin(new Vec3f(0f, 0f, 0f), viewport, winA01);
        System.err.println("A.0.1 - Project 0,0 -->" + winA01);
        Matrix4f.mapObjToWin(new Vec3f(0f, 0f, 0f), mat4PMv, viewport, winB01);
        System.err.println("B.0.1 - Project 0,0 -->" + winB01);

        m.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        m.glOrthof(0, 10, 0, 10, 1, -1);
        System.err.println("MATRIX - Ortho 0,0,10,10 - Locate the origin in the bottom left and scale");
        System.err.println(m);
        m.getMulPMv(mat4PMv);
        System.err.println(mat4PMv.toString(null, "mat4PMv", "%10.5f"));

        m.mapObjToWin(new Vec3f(1f, 0f, 0f), viewport, winA10);
        System.err.println("A.1.0 - Project 1,0 -->" +winA10);
        Matrix4f.mapObjToWin(new Vec3f(1f, 0f, 0f), mat4PMv, viewport, winB10);
        System.err.println("B.1.0 - Project 1,0 -->" +winB10);

        m.mapObjToWin(new Vec3f(0f, 0f, 0f), viewport, winA11);
        System.err.println("A.1.1 - Project 0,0 -->" +winA11);
        Matrix4f.mapObjToWin(new Vec3f(0f, 0f, 0f), mat4PMv, viewport, winB11);
        System.err.println("B.1.1 - Project 0,0 -->" +winB11);

        Assert.assertEquals("A/B 0.0 Project 1,0 failure", winB00, winA00);
        Assert.assertEquals("A/B 0.1 Project 0,0 failure", winB01, winA01);
        Assert.assertEquals("A/B 1.0 Project 1,0 failure", winB10, winA10);
        Assert.assertEquals("A/B 1.1 Project 0,0 failure", winB11, winA11);
    }

    /**
     * PMVMatrix vs Matrix4f.mapObjToWin(), both w/ separate P + Mv
     *
     * Both using same Matrix4f.mapObjToWin().
     */
    @Test
    public void test01PMVMatrixToMatrix4f2() {
        final Vec3f winA00 = new Vec3f();
        final Vec3f winA01 = new Vec3f();
        final Vec3f winA10 = new Vec3f();
        final Vec3f winA11 = new Vec3f();
        final Vec3f winB00 = new Vec3f();
        final Vec3f winB01 = new Vec3f();
        final Vec3f winB10 = new Vec3f();
        final Vec3f winB11 = new Vec3f();

        final PMVMatrix m = new PMVMatrix();
        final Matrix4f mat4Mv = new Matrix4f();
        final Matrix4f mat4P = new Matrix4f();
        final float[] mat4Mv_f16 = new float[16];
        final float[] mat4P_f16 = new float[16];

        m.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, mat4Mv_f16, 0);
        m.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, mat4P_f16, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4Mv", "%10.5f", mat4Mv_f16, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "mat4P ", "%10.5f", mat4P_f16,  0, 4, 4, false /* rowMajorOrder */));
        mat4Mv.load( m.getMat(GLMatrixFunc.GL_MODELVIEW_MATRIX) );
        mat4P.load( m.getMat(GLMatrixFunc.GL_PROJECTION_MATRIX) );
        Assert.assertEquals(new Matrix4f(mat4Mv_f16), mat4Mv);
        Assert.assertEquals(new Matrix4f(mat4P_f16), mat4P);
        Assert.assertEquals(mat4Mv, m.getMv());
        Assert.assertEquals(mat4P, m.getP());

        m.mapObjToWin(new Vec3f(1f, 0f, 0f), viewport, winA00);
        System.err.println("A.0.0 - Project 1,0 -->" + winA00);
        Matrix4f.mapObjToWin(new Vec3f(1f, 0f, 0f), mat4Mv, mat4P, viewport, winB00);
        System.err.println("B.0.0 - Project 1,0 -->" + winB00);

        m.mapObjToWin(new Vec3f(0f, 0f, 0f), viewport, winA01);
        System.err.println("A.0.1 - Project 0,0 -->" + winA01);
        Matrix4f.mapObjToWin(new Vec3f(0f, 0f, 0f), mat4Mv, mat4P, viewport, winB01);
        System.err.println("B.0.1 - Project 0,0 -->" + winB01);

        m.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        m.glOrthof(0, 10, 0, 10, 1, -1);
        System.err.println("MATRIX - Ortho 0,0,10,10 - Locate the origin in the bottom left and scale");
        System.err.println(m);
        m.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, mat4Mv_f16, 0);
        m.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, mat4P_f16, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4Mv", "%10.5f", mat4Mv_f16, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "mat4P ", "%10.5f", mat4P_f16,  0, 4, 4, false /* rowMajorOrder */));
        mat4Mv.load( m.getMat(GLMatrixFunc.GL_MODELVIEW_MATRIX) );
        mat4P.load( m.getMat(GLMatrixFunc.GL_PROJECTION_MATRIX) );
        Assert.assertEquals(new Matrix4f(mat4Mv_f16), mat4Mv);
        Assert.assertEquals(new Matrix4f(mat4P_f16), mat4P);
        Assert.assertEquals(mat4Mv, m.getMv());
        Assert.assertEquals(mat4P, m.getP());

        m.mapObjToWin(new Vec3f(1f, 0f, 0f), viewport, winA10);
        System.err.println("A.1.0 - Project 1,0 -->" +winA10);
        Matrix4f.mapObjToWin(new Vec3f(1f, 0f, 0f), mat4Mv, mat4P, viewport, winB10);
        System.err.println("B.1.0 - Project 1,0 -->" +winB10);

        m.mapObjToWin(new Vec3f(0f, 0f, 0f), viewport, winA11);
        System.err.println("A.1.1 - Project 0,0 -->" +winA11);
        Matrix4f.mapObjToWin(new Vec3f(0f, 0f, 0f), mat4Mv, mat4P, viewport, winB11);
        System.err.println("B.1.1 - Project 0,0 -->" +winB11);

        Assert.assertEquals("A/B 0.0 Project 1,0 failure", winB00, winA00);
        Assert.assertEquals("A/B 0.1 Project 0,0 failure", winB01, winA01);
        Assert.assertEquals("A/B 1.0 Project 1,0 failure", winB10, winA10);
        Assert.assertEquals("A/B 1.1 Project 0,0 failure", winB11, winA11);
    }

    /**
     * GLU ProjectFloat vs Matrix4f.mapObjToWin(), both w/ separate P + Mv
     */
    @Test
    public void test03GLUToMatrix4f2() {
        final float[] winA00 = new float[4];
        final float[] winA01 = new float[4];
        final float[] winA10 = new float[4];
        final float[] winA11 = new float[4];
        final Vec3f winB00 = new Vec3f();
        final Vec3f winB01 = new Vec3f();
        final Vec3f winB10 = new Vec3f();
        final Vec3f winB11 = new Vec3f();

        final PMVMatrix m = new PMVMatrix();
        final Matrix4f mat4Mv = new Matrix4f();
        final Matrix4f mat4P = new Matrix4f();
        final float[] mat4Mv_f16 = new float[16];
        final float[] mat4P_f16 = new float[16];
        final GLU glu = new GLU();

        m.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, mat4Mv_f16, 0);
        m.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, mat4P_f16, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4Mv", "%10.5f", mat4Mv_f16, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "mat4P ", "%10.5f", mat4P_f16,  0, 4, 4, false /* rowMajorOrder */));
        mat4Mv.load( m.getMv() );
        mat4P.load( m.getP() );

        glu.gluProject(1f, 0f, 0f, mat4Mv_f16, 0, mat4P_f16, 0, viewport_i4, 0, winA00, 0);
        System.err.println("A.0.0 - Project 1,0 -->" + winA00);
        Matrix4f.mapObjToWin(new Vec3f(1f, 0f, 0f), mat4Mv, mat4P, viewport, winB00);
        System.err.println("B.0.0 - Project 1,0 -->" + winB00);

        glu.gluProject(0f, 0f, 0f, mat4Mv_f16, 0, mat4P_f16, 0, viewport_i4, 0, winA01, 0);
        System.err.println("A.0.1 - Project 0,0 -->" + winA01);
        Matrix4f.mapObjToWin(new Vec3f(0f, 0f, 0f), mat4Mv, mat4P, viewport, winB01);
        System.err.println("B.0.1 - Project 0,0 -->" + winB01);

        m.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        m.glOrthof(0, 10, 0, 10, 1, -1);
        System.err.println("MATRIX - Ortho 0,0,10,10 - Locate the origin in the bottom left and scale");
        System.err.println(m);
        m.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, mat4Mv_f16, 0);
        m.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, mat4P_f16, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4Mv", "%10.5f", mat4Mv_f16, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "mat4P ", "%10.5f", mat4P_f16,  0, 4, 4, false /* rowMajorOrder */));
        mat4Mv.load( m.getMv() );
        mat4P.load( m.getP() );

        glu.gluProject(1f, 0f, 0f, mat4Mv_f16, 0, mat4P_f16, 0, viewport_i4, 0, winA10, 0);
        System.err.println("A.1.0 - Project 1,0 -->" +winA10);
        Matrix4f.mapObjToWin(new Vec3f(1f, 0f, 0f), mat4Mv, mat4P, viewport, winB10);
        System.err.println("B.1.0 - Project 1,0 -->" +winB10);

        glu.gluProject(0f, 0f, 0f, mat4Mv_f16, 0, mat4P_f16, 0, viewport_i4, 0, winA11, 0);
        System.err.println("A.1.1 - Project 0,0 -->" +winA11);
        Matrix4f.mapObjToWin(new Vec3f(0f, 0f, 0f), mat4Mv, mat4P, viewport, winB11);
        System.err.println("B.1.1 - Project 0,0 -->" +winB11);

        Assert.assertEquals("A/B 0.0 Project 1,0 failure", winB00, new Vec3f(winA00));
        Assert.assertEquals("A/B 0.1 Project 0,0 failure", winB01, new Vec3f(winA01));
        Assert.assertEquals("A/B 1.0 Project 1,0 failure", winB10, new Vec3f(winA10));
        Assert.assertEquals("A/B 1.1 Project 0,0 failure", winB11, new Vec3f(winA11));
    }

    /**
     * GLU ProjectDouble vs Matrix4f.mapObjToWin(), both w/ separate P + Mv
     */
    @Test
    public void test04GLUDoubleToMatrix4f2() {
        final double[] winA00 = new double[3];
        final double[] winA01 = new double[3];
        final double[] winA10 = new double[3];
        final double[] winA11 = new double[3];
        final Vec3f winB00 = new Vec3f();
        final Vec3f winB01 = new Vec3f();
        final Vec3f winB10 = new Vec3f();
        final Vec3f winB11 = new Vec3f();

        final PMVMatrix m = new PMVMatrix();
        final Matrix4f mat4Mv = new Matrix4f();
        final Matrix4f mat4P = new Matrix4f();
        final float[] mat4Mv_f16 = new float[16];
        final float[] mat4P_f16 = new float[16];
        final ProjectDouble glu = new ProjectDouble();

        m.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, mat4Mv_f16, 0);
        m.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, mat4P_f16, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4Mv", "%10.5f", mat4Mv_f16, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "mat4P ", "%10.5f", mat4P_f16,  0, 4, 4, false /* rowMajorOrder */));
        mat4Mv.load( m.getMv() );
        mat4P.load( m.getP() );
        double[] mat4Mv_d16 = Buffers.getDoubleArray(mat4Mv_f16, 0, null, 0, -1);
        double[] mat4P_d16 = Buffers.getDoubleArray(mat4P_f16, 0, null, 0, -1);

        glu.gluProject(1f, 0f, 0f, mat4Mv_d16, 0, mat4P_d16, 0, viewport_i4, 0, winA00, 0);
        System.err.println("A.0.0 - Project 1,0 -->" + Arrays.toString(winA00));
        Matrix4f.mapObjToWin(new Vec3f(1f, 0f, 0f), mat4Mv, mat4P, viewport, winB00);
        System.err.println("B.0.0 - Project 1,0 -->" + winB00);

        glu.gluProject(0f, 0f, 0f, mat4Mv_d16, 0, mat4P_d16, 0, viewport_i4, 0, winA01, 0);
        System.err.println("A.0.1 - Project 0,0 -->" + Arrays.toString(winA01));
        Matrix4f.mapObjToWin(new Vec3f(0f, 0f, 0f), mat4Mv, mat4P, viewport, winB01);
        System.err.println("B.0.1 - Project 0,0 -->" + winB01);

        m.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        m.glOrthof(0, 10, 0, 10, 1, -1);
        System.err.println("MATRIX - Ortho 0,0,10,10 - Locate the origin in the bottom left and scale");
        System.err.println(m);
        m.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, mat4Mv_f16, 0);
        m.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, mat4P_f16, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4Mv", "%10.5f", mat4Mv_f16, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "mat4P ", "%10.5f", mat4P_f16,  0, 4, 4, false /* rowMajorOrder */));
        mat4Mv.load( m.getMv() );
        mat4P.load( m.getP() );
        mat4Mv_d16 = Buffers.getDoubleArray(mat4Mv_f16, 0, null, 0, -1);
        mat4P_d16 = Buffers.getDoubleArray(mat4P_f16, 0, null, 0, -1);

        glu.gluProject(1f, 0f, 0f, mat4Mv_d16, 0, mat4P_d16, 0, viewport_i4, 0, winA10, 0);
        System.err.println("A.1.0 - Project 1,0 -->" +Arrays.toString(winA10));
        Matrix4f.mapObjToWin(new Vec3f(1f, 0f, 0f), mat4Mv, mat4P, viewport, winB10);
        System.err.println("B.1.0 - Project 1,0 -->" +winB10);

        glu.gluProject(0f, 0f, 0f, mat4Mv_d16, 0, mat4P_d16, 0, viewport_i4, 0, winA11, 0);
        System.err.println("A.1.1 - Project 0,0 -->" +Arrays.toString(winA11));
        Matrix4f.mapObjToWin(new Vec3f(0f, 0f, 0f), mat4Mv, mat4P, viewport, winB11);
        System.err.println("B.1.1 - Project 0,0 -->" +winB11);

        final float[] tmp = new float[3];
        double[] d_winBxx = Buffers.getDoubleArray(winB00.get(tmp), 0, null, 0, -1);
        Assert.assertArrayEquals("A/B 0.0 Project 1,0 failure", d_winBxx, winA00, epsilon);
        d_winBxx = Buffers.getDoubleArray(winB01.get(tmp), 0, null, 0, -1);
        Assert.assertArrayEquals("A/B 0.1 Project 0,0 failure", d_winBxx, winA01, epsilon);
        d_winBxx = Buffers.getDoubleArray(winB10.get(tmp), 0, null, 0, -1);
        Assert.assertArrayEquals("A/B 1.0 Project 1,0 failure", d_winBxx, winA10, epsilon);
        d_winBxx = Buffers.getDoubleArray(winB11.get(tmp), 0, null, 0, -1);
        Assert.assertArrayEquals("A/B 1.1 Project 0,0 failure", d_winBxx, winA11, epsilon);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestMatrix4fProject01NOUI.class.getName());
    }
}
