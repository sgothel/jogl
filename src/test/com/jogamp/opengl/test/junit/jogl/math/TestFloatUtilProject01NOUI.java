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

import java.util.Arrays;

import com.jogamp.common.nio.Buffers;
import com.jogamp.junit.util.JunitTracer;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;

import jogamp.opengl.gl2.ProjectDouble;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFloatUtilProject01NOUI extends JunitTracer {

    static final float epsilon = 0.00001f;

    // Simple 10 x 10 view port
    static final int[] viewport = new int[] { 0,0,10,10};

    /**
     * PMVMatrix FloatUtil.mapObjToWinCoords() w/ separate P + Mv, against FloatUtil.mapObjToWinCoords() w/ PMV
     */
    @Test
    public void test01PMVMatrixToFloatUtil1() {
        final float[] vec4Tmp1 = new float[4];
        final float[] vec4Tmp2 = new float[4];

        final float[] winA00 = new float[4];
        final float[] winA01 = new float[4];
        final float[] winA10 = new float[4];
        final float[] winA11 = new float[4];
        final float[] winB00 = new float[4];
        final float[] winB01 = new float[4];
        final float[] winB10 = new float[4];
        final float[] winB11 = new float[4];

        final PMVMatrix m = new PMVMatrix();
        final float[] mat4PMv = new float[16];
        m.multPMvMatrixf(mat4PMv, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4PMv", "%10.5f", mat4PMv, 0, 4, 4, false /* rowMajorOrder */));

        m.gluProject(1f, 0f, 0f, viewport, 0, winA00, 0);
        System.err.println("A.0.0 - Project 1,0 -->" + Arrays.toString(winA00));
        FloatUtil.mapObjToWinCoords(1f, 0f, 0f, mat4PMv, viewport, 0, winB00, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.0.0 - Project 1,0 -->" + Arrays.toString(winB00));

        m.gluProject(0f, 0f, 0f, viewport, 0, winA01, 0);
        System.err.println("A.0.1 - Project 0,0 -->" + Arrays.toString(winA01));
        FloatUtil.mapObjToWinCoords(0f, 0f, 0f, mat4PMv, viewport, 0, winB01, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.0.1 - Project 0,0 -->" + Arrays.toString(winB01));

        m.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        m.glOrthof(0, 10, 0, 10, 1, -1);
        System.err.println("MATRIX - Ortho 0,0,10,10 - Locate the origin in the bottom left and scale");
        System.err.println(m);
        m.multPMvMatrixf(mat4PMv, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4PMv", "%10.5f", mat4PMv, 0, 4, 4, false /* rowMajorOrder */));

        m.gluProject(1f, 0f, 0f, viewport, 0, winA10, 0);
        System.err.println("A.1.0 - Project 1,0 -->" +Arrays.toString(winA10));
        FloatUtil.mapObjToWinCoords(1f, 0f, 0f, mat4PMv, viewport, 0, winB10, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.1.0 - Project 1,0 -->" +Arrays.toString(winB10));

        m.gluProject(0f, 0f, 0f, viewport, 0, winA11, 0);
        System.err.println("A.1.1 - Project 0,0 -->" +Arrays.toString(winA11));
        FloatUtil.mapObjToWinCoords(0f, 0f, 0f, mat4PMv, viewport, 0, winB11, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.1.1 - Project 0,0 -->" +Arrays.toString(winB11));

        Assert.assertArrayEquals("A/B 0.0 Project 1,0 failure", winB00, winA00, epsilon);
        Assert.assertArrayEquals("A/B 0.1 Project 0,0 failure", winB01, winA01, epsilon);
        Assert.assertArrayEquals("A/B 1.0 Project 1,0 failure", winB10, winA10, epsilon);
        Assert.assertArrayEquals("A/B 1.1 Project 0,0 failure", winB11, winA11, epsilon);
    }

    /**
     * Actually both using same FloatUtil.mapObjToWinCoords() w/ separate P + Mv
     */
    @Test
    public void test01PMVMatrixToFloatUtil2() {
        final float[] vec4Tmp1 = new float[4];
        final float[] vec4Tmp2 = new float[4];

        final float[] winA00 = new float[4];
        final float[] winA01 = new float[4];
        final float[] winA10 = new float[4];
        final float[] winA11 = new float[4];
        final float[] winB00 = new float[4];
        final float[] winB01 = new float[4];
        final float[] winB10 = new float[4];
        final float[] winB11 = new float[4];

        final PMVMatrix m = new PMVMatrix();
        final float[] mat4Mv = new float[16];
        final float[] mat4P = new float[16];

        m.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, mat4Mv, 0);
        m.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, mat4P, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4Mv", "%10.5f", mat4Mv, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "mat4P ", "%10.5f", mat4P,  0, 4, 4, false /* rowMajorOrder */));

        m.gluProject(1f, 0f, 0f, viewport, 0, winA00, 0);
        System.err.println("A.0.0 - Project 1,0 -->" + Arrays.toString(winA00));
        FloatUtil.mapObjToWinCoords(1f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winB00, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.0.0 - Project 1,0 -->" + Arrays.toString(winB00));

        m.gluProject(0f, 0f, 0f, viewport, 0, winA01, 0);
        System.err.println("A.0.1 - Project 0,0 -->" + Arrays.toString(winA01));
        FloatUtil.mapObjToWinCoords(0f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winB01, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.0.1 - Project 0,0 -->" + Arrays.toString(winB01));

        m.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        m.glOrthof(0, 10, 0, 10, 1, -1);
        System.err.println("MATRIX - Ortho 0,0,10,10 - Locate the origin in the bottom left and scale");
        System.err.println(m);
        m.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, mat4Mv, 0);
        m.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, mat4P, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4Mv", "%10.5f", mat4Mv, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "mat4P ", "%10.5f", mat4P,  0, 4, 4, false /* rowMajorOrder */));

        m.gluProject(1f, 0f, 0f, viewport, 0, winA10, 0);
        System.err.println("A.1.0 - Project 1,0 -->" +Arrays.toString(winA10));
        FloatUtil.mapObjToWinCoords(1f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winB10, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.1.0 - Project 1,0 -->" +Arrays.toString(winB10));

        m.gluProject(0f, 0f, 0f, viewport, 0, winA11, 0);
        System.err.println("A.1.1 - Project 0,0 -->" +Arrays.toString(winA11));
        FloatUtil.mapObjToWinCoords(0f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winB11, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.1.1 - Project 0,0 -->" +Arrays.toString(winB11));

        Assert.assertArrayEquals("A/B 0.0 Project 1,0 failure", winB00, winA00, epsilon);
        Assert.assertArrayEquals("A/B 0.1 Project 0,0 failure", winB01, winA01, epsilon);
        Assert.assertArrayEquals("A/B 1.0 Project 1,0 failure", winB10, winA10, epsilon);
        Assert.assertArrayEquals("A/B 1.1 Project 0,0 failure", winB11, winA11, epsilon);
    }

    /**
     * GLU ProjectFloat w/ same FloatUtil.mapObjToWinCoords() w/ separate P + Mv
     */
    @Test
    public void test03GLUToFloatUtil2() {
        final float[] vec4Tmp1 = new float[4];
        final float[] vec4Tmp2 = new float[4];

        final float[] winA00 = new float[4];
        final float[] winA01 = new float[4];
        final float[] winA10 = new float[4];
        final float[] winA11 = new float[4];
        final float[] winB00 = new float[4];
        final float[] winB01 = new float[4];
        final float[] winB10 = new float[4];
        final float[] winB11 = new float[4];

        final PMVMatrix m = new PMVMatrix();
        final float[] mat4Mv = new float[16];
        final float[] mat4P = new float[16];
        final GLU glu = new GLU();

        m.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, mat4Mv, 0);
        m.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, mat4P, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4Mv", "%10.5f", mat4Mv, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "mat4P ", "%10.5f", mat4P,  0, 4, 4, false /* rowMajorOrder */));

        glu.gluProject(1f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winA00, 0);
        System.err.println("A.0.0 - Project 1,0 -->" + Arrays.toString(winA00));
        FloatUtil.mapObjToWinCoords(1f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winB00, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.0.0 - Project 1,0 -->" + Arrays.toString(winB00));

        glu.gluProject(0f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winA01, 0);
        System.err.println("A.0.1 - Project 0,0 -->" + Arrays.toString(winA01));
        FloatUtil.mapObjToWinCoords(0f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winB01, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.0.1 - Project 0,0 -->" + Arrays.toString(winB01));

        m.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        m.glOrthof(0, 10, 0, 10, 1, -1);
        System.err.println("MATRIX - Ortho 0,0,10,10 - Locate the origin in the bottom left and scale");
        System.err.println(m);
        m.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, mat4Mv, 0);
        m.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, mat4P, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4Mv", "%10.5f", mat4Mv, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "mat4P ", "%10.5f", mat4P,  0, 4, 4, false /* rowMajorOrder */));

        glu.gluProject(1f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winA10, 0);
        System.err.println("A.1.0 - Project 1,0 -->" +Arrays.toString(winA10));
        FloatUtil.mapObjToWinCoords(1f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winB10, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.1.0 - Project 1,0 -->" +Arrays.toString(winB10));

        glu.gluProject(0f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winA11, 0);
        System.err.println("A.1.1 - Project 0,0 -->" +Arrays.toString(winA11));
        FloatUtil.mapObjToWinCoords(0f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winB11, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.1.1 - Project 0,0 -->" +Arrays.toString(winB11));

        Assert.assertArrayEquals("A/B 0.0 Project 1,0 failure", winB00, winA00, epsilon);
        Assert.assertArrayEquals("A/B 0.1 Project 0,0 failure", winB01, winA01, epsilon);
        Assert.assertArrayEquals("A/B 1.0 Project 1,0 failure", winB10, winA10, epsilon);
        Assert.assertArrayEquals("A/B 1.1 Project 0,0 failure", winB11, winA11, epsilon);
    }

    /**
     * GLU ProjectDouble against FloatUtil.mapObjToWinCoords() w/ separate P + Mv
     */
    @Test
    public void test04GLUDoubleToFloatUtil2() {
        final float[] vec4Tmp1 = new float[4];
        final float[] vec4Tmp2 = new float[4];

        final double[] winA00 = new double[4];
        final double[] winA01 = new double[4];
        final double[] winA10 = new double[4];
        final double[] winA11 = new double[4];
        final float[] winB00 = new float[4];
        final float[] winB01 = new float[4];
        final float[] winB10 = new float[4];
        final float[] winB11 = new float[4];

        final PMVMatrix m = new PMVMatrix();
        final float[] mat4Mv = new float[16];
        final float[] mat4P = new float[16];
        final ProjectDouble glu = new ProjectDouble();

        m.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, mat4Mv, 0);
        m.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, mat4P, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4Mv", "%10.5f", mat4Mv, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "mat4P ", "%10.5f", mat4P,  0, 4, 4, false /* rowMajorOrder */));
        double[] d_mat4Mv = Buffers.getDoubleArray(mat4Mv, 0, null, 0, -1);
        double[] d_mat4P = Buffers.getDoubleArray(mat4P, 0, null, 0, -1);

        glu.gluProject(1f, 0f, 0f, d_mat4Mv, 0, d_mat4P, 0, viewport, 0, winA00, 0);
        System.err.println("A.0.0 - Project 1,0 -->" + Arrays.toString(winA00));
        FloatUtil.mapObjToWinCoords(1f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winB00, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.0.0 - Project 1,0 -->" + Arrays.toString(winB00));

        glu.gluProject(0f, 0f, 0f, d_mat4Mv, 0, d_mat4P, 0, viewport, 0, winA01, 0);
        System.err.println("A.0.1 - Project 0,0 -->" + Arrays.toString(winA01));
        FloatUtil.mapObjToWinCoords(0f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winB01, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.0.1 - Project 0,0 -->" + Arrays.toString(winB01));

        m.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        m.glOrthof(0, 10, 0, 10, 1, -1);
        System.err.println("MATRIX - Ortho 0,0,10,10 - Locate the origin in the bottom left and scale");
        System.err.println(m);
        m.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, mat4Mv, 0);
        m.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, mat4P, 0);
        System.err.println(FloatUtil.matrixToString(null, "mat4Mv", "%10.5f", mat4Mv, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println(FloatUtil.matrixToString(null, "mat4P ", "%10.5f", mat4P,  0, 4, 4, false /* rowMajorOrder */));
        d_mat4Mv = Buffers.getDoubleArray(mat4Mv, 0, null, 0, -1);
        d_mat4P = Buffers.getDoubleArray(mat4P, 0, null, 0, -1);

        glu.gluProject(1f, 0f, 0f, d_mat4Mv, 0, d_mat4P, 0, viewport, 0, winA10, 0);
        System.err.println("A.1.0 - Project 1,0 -->" +Arrays.toString(winA10));
        FloatUtil.mapObjToWinCoords(1f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winB10, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.1.0 - Project 1,0 -->" +Arrays.toString(winB10));

        glu.gluProject(0f, 0f, 0f, d_mat4Mv, 0, d_mat4P, 0, viewport, 0, winA11, 0);
        System.err.println("A.1.1 - Project 0,0 -->" +Arrays.toString(winA11));
        FloatUtil.mapObjToWinCoords(0f, 0f, 0f, mat4Mv, 0, mat4P, 0, viewport, 0, winB11, 0, vec4Tmp1, vec4Tmp2);
        System.err.println("B.1.1 - Project 0,0 -->" +Arrays.toString(winB11));

        double[] d_winBxx = Buffers.getDoubleArray(winB00, 0, null, 0, -1);
        Assert.assertArrayEquals("A/B 0.0 Project 1,0 failure", d_winBxx, winA00, epsilon);
        d_winBxx = Buffers.getDoubleArray(winB01, 0, null, 0, -1);
        Assert.assertArrayEquals("A/B 0.1 Project 0,0 failure", d_winBxx, winA01, epsilon);
        d_winBxx = Buffers.getDoubleArray(winB10, 0, null, 0, -1);
        Assert.assertArrayEquals("A/B 1.0 Project 1,0 failure", d_winBxx, winA10, epsilon);
        d_winBxx = Buffers.getDoubleArray(winB11, 0, null, 0, -1);
        Assert.assertArrayEquals("A/B 1.1 Project 0,0 failure", d_winBxx, winA11, epsilon);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestFloatUtilProject01NOUI.class.getName());
    }
}
