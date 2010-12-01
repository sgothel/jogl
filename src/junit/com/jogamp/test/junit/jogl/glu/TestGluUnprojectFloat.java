package com.jogamp.test.junit.jogl.glu;

import javax.media.opengl.glu.GLU;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Julien Gouesse
 */
public class TestGluUnprojectFloat {

    
    @Test
    public void test(){
        final GLU glu = new GLU();
        final int[] pickedPoint = new int[]{400,300};
        final float pickedPointDepth = 0;
        final float[] sceneModelViewValues = new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        final float[] projectionValues = new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        final int[] viewport = new int[]{0,0,800,600};
        final float[] objCoords = new float[]{Float.NaN,Float.NaN,Float.NaN};
        glu.gluUnProject(pickedPoint[0], pickedPoint[1], pickedPointDepth, sceneModelViewValues, 0, projectionValues, 0, viewport, 0, objCoords, 0);
        Assert.assertTrue(!Double.isNaN(objCoords[0])&&!Double.isNaN(objCoords[1])&&!Double.isNaN(objCoords[2]));      
    }
    
    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestGluUnprojectFloat.class.getName());
    }
}
