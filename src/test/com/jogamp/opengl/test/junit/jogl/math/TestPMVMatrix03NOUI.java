package com.jogamp.opengl.test.junit.jogl.math;

import java.util.Arrays;

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
        float[] winA00 = new float[4];
        float[] winA01 = new float[4];
        float[] winA10 = new float[4];
        float[] winA11 = new float[4];
        PMVMatrix m = new PMVMatrix();
                
        m.gluProject(1f, 0f, 0f, viewport, 0, winA00, 0);
        System.out.println("A.0.0 - Project 1,0 -->" + Arrays.toString(winA00));
        
        m.gluProject(0f, 0f, 0f, viewport, 0, winA01, 0);
        System.out.println("A.0.1 - Project 0,0 -->" + Arrays.toString(winA01));
        
        m.glMatrixMode(PMVMatrix.GL_PROJECTION);
        m.glOrthof(0, 10, 0, 10, 1, -1);
        System.out.println("MATRIX - Ortho 0,0,10,10 - Locate the origin in the bottom left and scale");
        System.out.println(m);
        float[] projMatrixA = new float[16];
        float[] modelMatrixA = new float[16];
        m.glGetFloatv(PMVMatrix.GL_PROJECTION, projMatrixA, 0);
        m.glGetFloatv(PMVMatrix.GL_MODELVIEW, modelMatrixA, 0);
        
        m.gluProject(1f, 0f, 0f, viewport, 0, winA10, 0);
        System.out.println("A.1.0 - Project 1,0 -->" +Arrays.toString(winA10));
        
        m.gluProject(0f, 0f, 0f, viewport, 0, winA11, 0);
        System.out.println("A.1.1 - Project 0,0 -->" +Arrays.toString(winA11));
        
        
        ////////////////////
        /////////////////////
        
        float[] winB00 = new float[4];
        float[] winB01 = new float[4];
        float[] winB10 = new float[4];
        float[] winB11 = new float[4];
        float[] projMatrixB = new float[16];
        float[] modelMatrixB = new float[16];
        FloatUtil.makeIdentityf(projMatrixB, 0);
        FloatUtil.makeIdentityf(modelMatrixB, 0);        
        final ProjectFloat projectFloat = new ProjectFloat(true);
        
        projectFloat.gluProject(1f, 0f, 0f, modelMatrixB, 0, projMatrixB, 0, viewport, 0, winB00, 0);
        System.out.println("B.0.0 - Project 1,0 -->" +Arrays.toString(winB00));
        
        projectFloat.gluProject(0f, 0f, 0f, modelMatrixB, 0, projMatrixB, 0, viewport, 0, winB01, 0);
        System.out.println("B.0.1 - Project 0,0 -->" +Arrays.toString(winB01));
                
        glOrthof(projMatrixB, 0, 10, 0, 10, 1, -1);
        System.out.println("FloatUtil - Ortho 0,0,10,10 - Locate the origin in the bottom left and scale");
        System.out.println("Projection");
        System.err.println(FloatUtil.matrixToString(null, null, "%10.5f", projMatrixB, 0, 4, 4, false /* rowMajorOrder */));
        System.out.println("Modelview");
        System.err.println(FloatUtil.matrixToString(null, null, "%10.5f", modelMatrixB, 0, 4, 4, false /* rowMajorOrder */));
                
        projectFloat.gluProject(1f, 0f, 0f, modelMatrixB, 0, projMatrixB, 0, viewport, 0, winB10, 0);
        System.out.println("B.1.0 - Project 1,0 -->" +Arrays.toString(winB10));
        
        projectFloat.gluProject(0f, 0f, 0f, modelMatrixB, 0, projMatrixB, 0, viewport, 0, winB11, 0);
        System.out.println("B.1.1 - Project 0,0 -->" +Arrays.toString(winB11));
        
        Assert.assertArrayEquals("A/B 0.0 Project 1,0 failure", winB00, winA00, epsilon);
        Assert.assertArrayEquals("A/B 0.1 Project 0,0 failure", winB01, winA01, epsilon);
        Assert.assertArrayEquals("A/B 1.0 Project 1,0 failure", winB10, winA10, epsilon);
        Assert.assertArrayEquals("A/B 1.1 Project 0,0 failure", winB11, winA11, epsilon);
        
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

        float[] matrixOrtho = new float[16];
        FloatUtil.makeIdentityf(matrixOrtho, 0);
        matrixOrtho[0+4*0] =  2.0f/dx;
        matrixOrtho[1+4*1] =  2.0f/dy;
        matrixOrtho[2+4*2] = -2.0f/dz;
        matrixOrtho[0+4*3] = tx;
        matrixOrtho[1+4*3] = ty;
        matrixOrtho[2+4*3] = tz;

        FloatUtil.multMatrixf(m, 0, matrixOrtho, 0);
    }
    
    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestPMVMatrix03NOUI.class.getName());
    }
}
