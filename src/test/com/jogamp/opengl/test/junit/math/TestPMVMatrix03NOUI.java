package com.jogamp.opengl.test.junit.math;

import java.util.Arrays;

import com.jogamp.junit.util.JunitTracer;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Matrix4f;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec3f;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import jogamp.opengl.ProjectFloat;

import com.jogamp.opengl.util.PMVMatrix;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPMVMatrix03NOUI extends JunitTracer {

    static final float epsilon = 0.00001f;

    // Simple 10 x 10 view port
    static final Recti viewport = new Recti( 0,0,10,10 );
    static final int[] viewport_i4 = new int[] { 0, 0, 10, 10 };

    @Test
    public void test01() {
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

        m.mapObjToWin(new Vec3f(1f, 0f, 0f), viewport, winA00);
        System.err.println("A.0.0 - Project 1,0 -->" + winA00);
        Matrix4f.mapObjToWin(new Vec3f(1f, 0f, 0f), mat4PMv, viewport, winB00); // single PMv
        System.err.println("B.0.0 - Project 1,0 -->" + winB00);

        m.mapObjToWin(new Vec3f(0f, 0f, 0f), viewport, winA01);
        System.err.println("A.0.1 - Project 0,0 -->" + winA01);
        Matrix4f.mapObjToWin(new Vec3f(0f, 0f, 0f), mat4PMv, viewport, winB01); // single PMv
        System.err.println("B.0.1 - Project 0,0 -->" + winB01);

        m.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        m.glOrthof(0, 10, 0, 10, 1, -1);
        System.err.println("MATRIX - Ortho 0,0,10,10 - Locate the origin in the bottom left and scale");
        System.err.println(m);
        m.getMulPMv(mat4PMv);
        System.err.println(mat4PMv.toString(null, "mat4PMv", "%10.5f"));

        m.mapObjToWin(new Vec3f(1f, 0f, 0f), viewport, winA10);
        System.err.println("A.1.0 - Project 1,0 -->" +winA10);
        Matrix4f.mapObjToWin(new Vec3f(1f, 0f, 0f), mat4PMv, viewport, winB10); // single PMv
        System.err.println("B.1.0 - Project 1,0 -->" +winB10);

        m.mapObjToWin(new Vec3f(0f, 0f, 0f), viewport, winA11);
        System.err.println("A.1.1 - Project 0,0 -->" +winA11);
        Matrix4f.mapObjToWin(new Vec3f(0f, 0f, 0f), mat4PMv, viewport, winB11); // single PMv
        System.err.println("B.1.1 - Project 0,0 -->" +winB11);

        Assert.assertEquals("A/B 0.0 Project 1,0 failure", winB00, winA00);
        Assert.assertEquals("A/B 0.1 Project 0,0 failure", winB01, winA01);
        Assert.assertEquals("A/B 1.0 Project 1,0 failure", winB10, winA10);
        Assert.assertEquals("A/B 1.1 Project 0,0 failure", winB11, winA11);

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

        projectFloat.gluProject(1f, 0f, 0f, modelMatrixC, 0, projMatrixC, 0, viewport_i4, 0, winC00, 0);
        System.err.println("C.0.0 - Project 1,0 -->" +Arrays.toString(winC00));

        projectFloat.gluProject(0f, 0f, 0f, modelMatrixC, 0, projMatrixC, 0, viewport_i4, 0, winC01, 0);
        System.err.println("C.0.1 - Project 0,0 -->" +Arrays.toString(winC01));

        glOrthof(projMatrixC, 0, 10, 0, 10, 1, -1);
        System.err.println("FloatUtil - Ortho 0,0,10,10 - Locate the origin in the bottom left and scale");
        System.err.println("Projection");
        System.err.println(FloatUtil.matrixToString(null, null, "%10.5f", projMatrixC, 0, 4, 4, false /* rowMajorOrder */));
        System.err.println("Modelview");
        System.err.println(FloatUtil.matrixToString(null, null, "%10.5f", modelMatrixC, 0, 4, 4, false /* rowMajorOrder */));

        projectFloat.gluProject(1f, 0f, 0f, modelMatrixC, 0, projMatrixC, 0, viewport_i4, 0, winC10, 0);
        System.err.println("C.1.0 - Project 1,0 -->" +Arrays.toString(winC10));

        projectFloat.gluProject(0f, 0f, 0f, modelMatrixC, 0, projMatrixC, 0, viewport_i4, 0, winC11, 0);
        System.err.println("B.1.1 - Project 0,0 -->" +Arrays.toString(winC11));

        Assert.assertEquals("A/C 0.0 Project 1,0 failure", new Vec3f(winC00), winA00);
        Assert.assertEquals("A/C 0.1 Project 0,0 failure", new Vec3f(winC01), winA01);
        Assert.assertEquals("A/C 1.0 Project 1,0 failure", new Vec3f(winC10), winA10);
        Assert.assertEquals("A/C 1.1 Project 0,0 failure", new Vec3f(winC11), winA11);

        Assert.assertEquals("A 0.0 Project 1,0 failure X", 10.0, winA00.x(), epsilon);
        Assert.assertEquals("A 0.0 Project 1,0 failure Y",  5.0, winA00.y(), epsilon);
        Assert.assertEquals("A.0.1 Project 0,0 failure X",  5.0, winA01.x(), epsilon);
        Assert.assertEquals("A.0.1 Project 0,0 failure Y",  5.0, winA01.y(), epsilon);
        Assert.assertEquals("A 1.0 Project 1,0 failure X",  1.0, winA10.x(), epsilon);
        Assert.assertEquals("A 1.0 Project 1,0 failure Y",  0.0, winA10.y(), epsilon);
        Assert.assertEquals("A.1.1 Project 0,0 failure X",  0.0, winA11.x(), epsilon);
        Assert.assertEquals("A.1.1 Project 0,0 failure Y",  0.0, winA11.y(), epsilon);
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
