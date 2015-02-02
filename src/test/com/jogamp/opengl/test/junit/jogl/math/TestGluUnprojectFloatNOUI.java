/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.glu.GLU;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGluUnprojectFloatNOUI {

    @Test
    public void testNaN(){
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

    @Test
    public void test01(){
        final float[] mv = new float[] { 1, 0, 0, 0,
                                   0, 1, 0, 0,
                                   0, 0, 1, 0,
                                   0, 0, 0, 1 };

        final float[] p = new float[] { 2.3464675f, 0,          0,        0,
                                  0,          2.4142134f, 0,        0,
                                  0,          0,         -1.0002f, -1,
                                  0,          0,        -20.002f,   0 };

        final int[] v = new int[] { 0, 0, 1000, 1000 };

        final float[] s = new float[] { 250, 250, 0.5f };

        final float[] expected = new float[] { -4.2612f, -4.1417f, -19.9980f };
        final float[] result = new float[] { 0, 0, 0 };

        final GLU glu = new GLU();
        glu.gluUnProject(s[0], s[1], s[2], mv, 0, p, 0, v, 0, result, 0);

        Assert.assertArrayEquals(expected, result, 0.0001f);
    }

    @Test
    public void test02(){
        final float[] mv = new float[] { 1, 0,    0, 0,
                                   0, 1,    0, 0,
                                   0, 0,    1, 0,
                                   0, 0, -200, 1 } ;

        final float[] p = new float[] { 2.3464675f, 0,          0,        0,
                                  0,          2.4142134f, 0,        0,
                                  0,          0,         -1.0002f, -1,
                                  0,          0,        -20.002f,   0 };

        final int[] v = new int[] { 0, 0, 1000, 1000 };

        final float[] s = new float[] { 250, 250, 0.5f };
        final float[] expected = new float[] { -4.2612f, -4.1417f, 180.002f };
        final float[] result = new float[] { 0, 0, 0 };

        final GLU glu = new GLU();
        glu.gluUnProject(s[0], s[1], s[2], mv, 0, p, 0, v, 0, result, 0);

        Assert.assertArrayEquals(expected, result, 0.0001f);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestGluUnprojectFloatNOUI.class.getName());
    }
}
