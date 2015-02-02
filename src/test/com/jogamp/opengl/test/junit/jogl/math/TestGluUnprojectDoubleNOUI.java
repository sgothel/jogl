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

/**
 * @author Julien Gouesse
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGluUnprojectDoubleNOUI {

    @Test
    public void test(){
        final GLU glu = new GLU();
        final int[] pickedPoint = new int[]{400,300};
        final double pickedPointDepth = 0;
        final double[] sceneModelViewValues = new double[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        final double[] projectionValues = new double[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        final int[] viewport = new int[]{0,0,800,600};
        final double[] objCoords = new double[]{Double.NaN,Double.NaN,Double.NaN};
        glu.gluUnProject(pickedPoint[0], pickedPoint[1], pickedPointDepth, sceneModelViewValues, 0, projectionValues, 0, viewport, 0, objCoords, 0);
        Assert.assertTrue(!Double.isNaN(objCoords[0])&&!Double.isNaN(objCoords[1])&&!Double.isNaN(objCoords[2]));
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestGluUnprojectDoubleNOUI.class.getName());
    }
}
