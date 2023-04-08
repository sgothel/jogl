/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.junit.util.JunitTracer;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Vec3f;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas De Bodt
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPMVMatrix02NOUI extends JunitTracer {

  private PMVMatrix fMat;

  @Before
  public void setUp() throws Exception {
    fMat = new PMVMatrix();
  }

  @Test
  public void testLookAtNegZIsNoOp() throws Exception {
    fMat.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
    // Look towards -z
    fMat.gluLookAt(
        new Vec3f(0, 0, 0),
        new Vec3f(0, 0, -1),
        new Vec3f(0, 1, 0)
    );
    final Matrix4f mvMatrix = fMat.getMvMat();
    assertEquals(
        /**
         * The 3 rows of the matrix (= the 3 columns of the array/buffer) should be: side, up, -forward.
         */
        new Matrix4f( new float[] {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
        }),
        mvMatrix
    );
  }
  @Test
  public void testLookAtPosY() throws Exception {
    fMat.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
    // Look towards +y
    fMat.gluLookAt(
        new Vec3f(0, 0, 0),
        new Vec3f(0, 1, 0),
        new Vec3f(0, 0, 1)
    );
    final Matrix4f mvMatrix = fMat.getMvMat();
    assertEquals(
        /**
         * The 3 rows of the matrix (= the 3 columns of the array/buffer) should be: side, up, -forward.
         */
        new Matrix4f(new float[] {
            1, 0, 0, 0,
            0, 0, -1, 0,
            0, 1, 0, 0,
            0, 0, 0, 1
        }),
        mvMatrix
    );
  }

  public static void main(final String args[]) {
      org.junit.runner.JUnitCore.main(TestPMVMatrix02NOUI.class.getName());
  }
}
