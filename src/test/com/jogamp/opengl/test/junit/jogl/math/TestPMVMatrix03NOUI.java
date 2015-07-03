package com.jogamp.opengl.test.junit.jogl.math;

import java.util.Arrays;

import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import jogamp.opengl.ProjectFloat;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPMVMatrix03NOUI {

    static final float epsilon = 0.00001f;

    // Simple 10 x 10 view port
    static final int[] viewport = new int[] { 0,0,10,10};

    @Test
    public void test01() {
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

        ////////////////////
        /////////////////////

        final float[] winC00 = new float[4];
        final float[] winC01 = new float[4];
        final float[] winC10 = new float[4];
        final float[] winC11 = new float[4];
        final float[] projMatrixC = new float[16];
        final float[] modelMatrixC = new float[16];
        FloatUtil.makeIdentity(projMatrixC);
        FloatUtil.makeIdentity(modelMatrixC);
        final ProjectFloat projectFloat = new ProjectFloat();

        projectFloat.gluProject(1f, 0f, 0f, modelMatrixC, 0, projMatrixC, 0, viewport, 0, winC00, 0);
        System.err.println("C.0.0 - Project 1,0 -->" +Arrays.toString(winC00));

        projectFloat.gluProject(0f, 0f, 0f, modelMatrixC, 0, projMatrixC, 0, viewport, 0, winC01, 0);
        System.err.println("C.0.1 - Project 0,0 -->" +Arrays.toString(winC01));

        glOrthof(projMatrixC, 0, 10, 0, 10, 1, -1);
        System.err.println("FloatUtil - Ortho 0,0,10,10 - Locate the origin in the bottom left and scale");
        System.err.println("Projection");
        System.err.println(FloatUtil.matrixToString(null, null, "%10.5f", projMatrixC, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println("Modelview");
        System.err.println(FloatUtil.matrixToString(null, null, "%10.5f", modelMatrixC, 0, 4, 4, false /* rowMajorOrder */));

        projectFloat.gluProject(1f, 0f, 0f, modelMatrixC, 0, projMatrixC, 0, viewport, 0, winC10, 0);
        System.err.println("C.1.0 - Project 1,0 -->" +Arrays.toString(winC10));

        projectFloat.gluProject(0f, 0f, 0f, modelMatrixC, 0, projMatrixC, 0, viewport, 0, winC11, 0);
        System.err.println("B.1.1 - Project 0,0 -->" +Arrays.toString(winC11));

        Assert.assertArrayEquals("A/C 0.0 Project 1,0 failure", winC00, winA00, epsilon);
        Assert.assertArrayEquals("A/C 0.1 Project 0,0 failure", winC01, winA01, epsilon);
        Assert.assertArrayEquals("A/C 1.0 Project 1,0 failure", winC10, winA10, epsilon);
        Assert.assertArrayEquals("A/C 1.1 Project 0,0 failure", winC11, winA11, epsilon);

        Assert.assertEquals("A 0.0 Project 1,0 failure X", 10.0, winA00[0], epsilon);
        Assert.assertEquals("A 0.0 Project 1,0 failure Y",  5.0, winA00[1], epsilon);
        Assert.assertEquals("A.0.1 Project 0,0 failure X",  5.0, winA01[0], epsilon);
        Assert.assertEquals("A.0.1 Project 0,0 failure Y",  5.0, winA01[1], epsilon);
        Assert.assertEquals("A 1.0 Project 1,0 failure X",  1.0, winA10[0], epsilon);
        Assert.assertEquals("A 1.0 Project 1,0 failure Y",  0.0, winA10[1], epsilon);
        Assert.assertEquals("A.1.1 Project 0,0 failure X",  0.0, winA11[0], epsilon);
        Assert.assertEquals("A.1.1 Project 0,0 failure Y",  0.0, winA11[1], epsilon);
    }

    public final void glOrthof(final float[] m, final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) {
        // Ortho matrix:
        //  2/dx  0     0    tx
        //  0     2/dy  0    ty
        //  0     0     2/dz tz
        //  0     0     0    1
        final float dx=right-left;
        final float dy=top-bottom;
        final float dz=zFar-zNear;
        final float tx=-1.0f*(right+left)/dx;
        final float ty=-1.0f*(top+bottom)/dy;
        final float tz=-1.0f*(zFar+zNear)/dz;

        final float[] matrixOrtho = new float[16];
        FloatUtil.makeIdentity(matrixOrtho);
        matrixOrtho[0+4*0] =  2.0f/dx;
        matrixOrtho[1+4*1] =  2.0f/dy;
        matrixOrtho[2+4*2] = -2.0f/dz;
        matrixOrtho[0+4*3] = tx;
        matrixOrtho[1+4*3] = ty;
        matrixOrtho[2+4*3] = tz;

        FloatUtil.multMatrix(m, 0, matrixOrtho, 0);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestPMVMatrix03NOUI.class.getName());
    }
}
