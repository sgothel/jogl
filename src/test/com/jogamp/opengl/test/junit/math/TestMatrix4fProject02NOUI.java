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

import com.jogamp.junit.util.JunitTracer;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Matrix4f;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestMatrix4fProject02NOUI extends JunitTracer {

    static final float epsilon = 0.00001f;

    @Test
    public void test01() {
        final Vec3f winHas = new Vec3f();
        final Vec2f winExp = new Vec2f( 297, 360 );

        final Recti viewport = new Recti(0, 0, 1280, 720);

        final Matrix4f mat4Mv = new Matrix4f(new float[] {
                0.40000000596046450000f,    0.00000000000000000000f,    0.00000000000000000000f,    0.00000000000000000000f,
                0.00000000000000000000f,    0.40000000596046450000f,    0.00000000000000000000f,    0.00000000000000000000f,
                0.00000000000000000000f,    0.00000000000000000000f,    1.00000000000000000000f,    0.00000000000000000000f,
               -0.09278385341167450000f,   -0.00471283448860049250f,   -0.20000000298023224000f,    1.00000000000000000000f });

        final Matrix4f mat4P = new Matrix4f(new float[] {
                1.35799503326416020000f,    0.00000000000000000000f,    0.00000000000000000000f,    0.00000000000000000000f,
                0.00000000000000000000f,    2.41421341896057130000f,    0.00000000000000000000f,    0.00000000000000000000f,
                0.00000000000000000000f,    0.00000000000000000000f,   -1.00002861022949220000f,   -1.00000000000000000000f,
                0.00000000000000000000f,    0.00000000000000000000f,   -0.20000286400318146000f,    0.00000000000000000000f });

        final Vec3f objPos = new Vec3f(0.02945519052445888500f,    0.01178207620978355400f,   -0.00499999988824129100f);

        System.err.println("pMv");
        System.err.println(mat4Mv.toString(null, "", "%25.20ff"));
        System.err.println("pP");
        System.err.println(mat4P.toString(null, "", "%25.20ff"));

        Matrix4f.mapObjToWin(objPos, mat4Mv, mat4P, viewport, winHas);
        System.err.println("B.0.0 - Project 1,0 -->" + winHas);

        Assert.assertEquals("A/B 0.0 Project 1,0 failure.x", winExp.x(), Math.round(winHas.x()), epsilon);
        Assert.assertEquals("A/B 0.0 Project 1,0 failure.y", winExp.y(), Math.round(winHas.y()), epsilon);
    }

    @Test
    public void test02() {
        final Vec3f winHas = new Vec3f();
        final Vec2f winExp = new Vec2f( 136, 360 );

        final Recti viewport = new Recti(0, 0, 1280, 720);

        // m30 (row 3, column 0) differs from test01
        final Matrix4f mat4Mv = new Matrix4f(new float[] {
                0.40000000596046450000f,    0.00000000000000000000f,    0.00000000000000000000f,    0.00000000000000000000f,
                0.00000000000000000000f,    0.40000000596046450000f,    0.00000000000000000000f,    0.00000000000000000000f,
                0.00000000000000000000f,    0.00000000000000000000f,    1.00000000000000000000f,    0.00000000000000000000f,
               -0.13065303862094880000f,   -0.00471283448860049250f,   -0.20000000298023224000f,    1.00000000000000000000f });

        final Matrix4f mat4P = new Matrix4f(new float[] {
                1.35799503326416020000f,    0.00000000000000000000f,    0.00000000000000000000f,    0.00000000000000000000f,
                0.00000000000000000000f,    2.41421341896057130000f,    0.00000000000000000000f,    0.00000000000000000000f,
                0.00000000000000000000f,    0.00000000000000000000f,   -1.00002861022949220000f,   -1.00000000000000000000f,
                0.00000000000000000000f,    0.00000000000000000000f,   -0.20000286400318146000f,    0.00000000000000000000f });

        final Vec3f objPos = new Vec3f(0.02945519052445888500f,    0.01178207620978355400f,   -0.00499999988824129100f);

        System.err.println("pMv");
        System.err.println(mat4Mv.toString(null, "", "%25.20ff"));
        System.err.println("pP");
        System.err.println(mat4P.toString(null, "", "%25.20ff"));

        Matrix4f.mapObjToWin(objPos, mat4Mv, mat4P, viewport, winHas);
        System.err.println("B.0.0 - Project 1,0 -->" + winHas);

        Assert.assertEquals("A/B 0.0 Project 1,0 failure.x", winExp.x(), Math.round(winHas.x()), epsilon);
        Assert.assertEquals("A/B 0.0 Project 1,0 failure.y", winExp.y(), Math.round(winHas.y()), epsilon);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestMatrix4fProject02NOUI.class.getName());
    }
}
