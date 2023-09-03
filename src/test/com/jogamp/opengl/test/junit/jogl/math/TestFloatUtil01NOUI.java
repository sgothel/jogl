/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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



import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.junit.util.JunitTracer;
import com.jogamp.opengl.math.FloatUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFloatUtil01NOUI extends JunitTracer {
    static final float MACH_EPSILON = FloatUtil.getMachineEpsilon();

    static boolean deltaMachEpsLEQEpsilon;
    static boolean deltaFixedEpsLEQEpsilon;

    @BeforeClass
    public static void test00Epsilon() {
        System.err.println();
        System.err.println("Machine Epsilon: "+MACH_EPSILON);
        System.err.println("Fixed   Epsilon: "+FloatUtil.EPSILON+", diff "+Math.abs(MACH_EPSILON-FloatUtil.EPSILON));
        System.err.println("Float MIN:       "+Float.MIN_VALUE);

        final float deltaMachEpsMin = Math.abs(MACH_EPSILON-Float.MIN_VALUE);
        deltaMachEpsLEQEpsilon = FloatUtil.compare(deltaMachEpsMin, MACH_EPSILON) <= 0;

        final float deltaFixedEpsMin = Math.abs(MACH_EPSILON-Float.MIN_VALUE);
        deltaFixedEpsLEQEpsilon = FloatUtil.compare(deltaFixedEpsMin, MACH_EPSILON) <= 0;

        System.err.println("deltaMachEpsMin "+deltaMachEpsMin+", deltaMachEpsLEQEpsilon "+deltaMachEpsLEQEpsilon);
        System.err.println("deltaFixedEpsMin "+deltaFixedEpsMin+", deltaFixedEpsLEQEpsilon "+deltaFixedEpsLEQEpsilon);
    }

    private void dumpTestWE(final int tstNum, final int expWE, final float a, final float b, final float EPSILON) {
        final float delta = a-b;
        final boolean equalWE =  FloatUtil.isEqual(a, b, EPSILON);
        final int compWE = FloatUtil.compare(a, b, EPSILON);
        final String msgWE = ( expWE != compWE ) ? "**** mismatch ****" : " OK";
        System.err.println("Print.WE."+tstNum+": a: "+a+", b: "+b+" -> d "+delta+", exp "+expWE+", equal "+equalWE+", comp "+compWE+" - "+msgWE+", epsilon "+EPSILON);
    }
    private void dumpTestNE(final int tstNum, final int exp, final float a, final float b) {
        final float delta = a-b;
        final boolean equal =  FloatUtil.isEqualRaw(a, b);
        final int comp = FloatUtil.compare(a, b);
        final String msg = ( exp != comp ) ? "**** mismatch ****" : " OK";
        System.err.println("Print.NE."+tstNum+": a: "+a+", b: "+b+" -> d "+delta+", exp "+exp+", equal "+equal+", comp "+comp+" - "+msg);
    }

    @Test
    public void test01aZeroWithFixedEpsilon() {
        testZeroWithEpsilon(10, FloatUtil.EPSILON);
    }
    @Test
    public void test01bZeroWithMachEpsilon() {
        testZeroWithEpsilon(100, MACH_EPSILON);
    }
    private void testZeroWithEpsilon(int i, final float EPSILON) {
        System.err.println();
        testZeroWithEpsilon(i++, true, 0f, EPSILON);
        testZeroWithEpsilon(i++, true, 0f-EPSILON/2f, EPSILON);
        testZeroWithEpsilon(i++, true, 0f+EPSILON/2f, EPSILON);
        testZeroWithEpsilon(i++, true, 0f-Float.MIN_VALUE, EPSILON);
        testZeroWithEpsilon(i++, true, 0f+Float.MIN_VALUE, EPSILON);
        testZeroWithEpsilon(i++, true, -0f, EPSILON);
        testZeroWithEpsilon(i++, true, +0f, EPSILON);

        testZeroWithEpsilon(i++, false, 0f+EPSILON+Float.MIN_VALUE, EPSILON);
        testZeroWithEpsilon(i++, false, 0f-EPSILON-Float.MIN_VALUE, EPSILON);

        // Unpredicted .. accuracy beyond epsilon, or deltaMachEpsLEQEpsilon or deltaFixedEpsLEQEpsilon;
        dumpTestWE(i++, 1, 0f, 0f+EPSILON-Float.MIN_VALUE, EPSILON);
        dumpTestWE(i++, 1, 0f, 0f-EPSILON+Float.MIN_VALUE, EPSILON);
    }
    private void testZeroWithEpsilon(final int tstNum, final boolean exp, final float a, final float EPSILON) {
        final boolean zero =  FloatUtil.isZero(a, EPSILON);
        final float delta = a-0f;
        System.err.println("Zero."+tstNum+": a: "+a+", -> d "+delta+", exp "+exp+", zero "+zero+", epsilon "+EPSILON);
        Assert.assertEquals("Zero failed a: "+a+" within "+EPSILON, exp, zero);
    }

    @Test
    public void test02EqualsNoEpsilon() {
        int i=0;
        System.err.println();
        testEqualsNoEpsilon(i++, true, 0f, 0f);

        testEqualsNoEpsilon(i++, true, Float.MAX_VALUE, Float.MAX_VALUE);
        testEqualsNoEpsilon(i++, true, Float.MIN_VALUE, Float.MIN_VALUE);
        testEqualsNoEpsilon(i++, true, Float.MIN_NORMAL, Float.MIN_NORMAL);
        testEqualsNoEpsilon(i++, true, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        testEqualsNoEpsilon(i++, true, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        testEqualsNoEpsilon(i++, true, Float.NaN, Float.NaN);
        testEqualsNoEpsilon(i++, false,  -0f, 0f);
        testEqualsNoEpsilon(i++, false, 0f, -0f);

        // Unpredicted .. accuracy beyond epsilon, or deltaMachEpsLEQEpsilon or deltaFixedEpsLEQEpsilon;
        dumpTestNE(i++, 0, 1f, 1f-MACH_EPSILON/2f);
        dumpTestNE(i++, 0, 0f, 0f-MACH_EPSILON/2f);
        dumpTestNE(i++, 0, 1f, 1f+MACH_EPSILON/2f);
        dumpTestNE(i++, 0, 0f, 0f+MACH_EPSILON/2f);
        dumpTestNE(i++, 0, 1f, 1f-Float.MIN_VALUE);
        dumpTestNE(i++, 0, 0f, 0f-Float.MIN_VALUE);
        dumpTestNE(i++, 0, 1f, 1f+Float.MIN_VALUE);
        dumpTestNE(i++, 0, 0f, 0f+Float.MIN_VALUE);
    }
    private void testEqualsNoEpsilon(final int tstNum, final boolean exp, final float a, final float b) {
        final boolean equal =  FloatUtil.isEqualRaw(a, b);
        final int comp = FloatUtil.compare(a, b);
        final float delta = a-b;
        System.err.println("Equal.NE."+tstNum+": a: "+a+", b: "+b+" -> d "+delta+", exp "+exp+", equal "+equal+", comp "+comp);
        Assert.assertEquals("Compare failed a: "+a+", b: "+b, exp, 0==comp);
        Assert.assertEquals("Equal failed a: "+a+", b: "+b, exp, equal);
    }

    @Test
    public void test03aEqualsWithFixedEpsilon() {
        testEqualsWithEpsilon(10, FloatUtil.EPSILON);
    }
    @Test
    public void test03bEqualsWithMachEpsilon() {
        testEqualsWithEpsilon(50, MACH_EPSILON);
    }
    private void testEqualsWithEpsilon(int i, final float EPSILON) {
        System.err.println();
        testEqualsWithEpsilon(i++, true, 0f, 0f, EPSILON);
        testEqualsWithEpsilon(i++, true, 1f, 1f-EPSILON/2f, EPSILON);
        testEqualsWithEpsilon(i++, true, 1f, 1f+EPSILON/2f, EPSILON);
        testEqualsWithEpsilon(i++, true, 1f, 1f-Float.MIN_VALUE, EPSILON);
        testEqualsWithEpsilon(i++, true, 1f, 1f+Float.MIN_VALUE, EPSILON);
        testEqualsWithEpsilon(i++, true, Float.MAX_VALUE, Float.MAX_VALUE, EPSILON);
        testEqualsWithEpsilon(i++, true, Float.MIN_VALUE, Float.MIN_VALUE, EPSILON);
        testEqualsWithEpsilon(i++, true, Float.MIN_NORMAL, Float.MIN_NORMAL, EPSILON);
        testEqualsWithEpsilon(i++, true, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, EPSILON);
        testEqualsWithEpsilon(i++, true, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, EPSILON);
        testEqualsWithEpsilon(i++, true, Float.NaN, Float.NaN, EPSILON);
        testEqualsWithEpsilon(i++, true, -0f, 0f, EPSILON);
        testEqualsWithEpsilon(i++, true, 0f, -0f, EPSILON);

        testEqualsWithEpsilon(i++, false, 1f, 1f+EPSILON+Float.MIN_VALUE, EPSILON);
        testEqualsWithEpsilon(i++, false, 1f, 1f-EPSILON-Float.MIN_VALUE, EPSILON);

        // Unpredicted .. accuracy beyond epsilon, or deltaMachEpsLEQEpsilon or deltaFixedEpsLEQEpsilon;
        dumpTestWE(i++, 1, 1f, 1f+EPSILON-Float.MIN_VALUE, EPSILON);
        dumpTestWE(i++, 1, 1f, 1f-EPSILON+Float.MIN_VALUE, EPSILON);
    }
    private void testEqualsWithEpsilon(final int tstNum, final boolean exp, final float a, final float b, final float EPSILON) {
        final boolean equal =  FloatUtil.isEqual(a, b, EPSILON);
        final int comp = FloatUtil.compare(a, b, EPSILON);
        final float delta = a-b;
        System.err.println("Equal.WE."+tstNum+": a: "+a+", b: "+b+" -> d "+delta+", exp "+exp+", equal "+equal+", comp "+comp);
        Assert.assertEquals("Compare failed a: "+a+", b: "+b+" within "+EPSILON, exp, 0==comp);
        Assert.assertEquals("Equal failed a: "+a+", b: "+b+" within "+EPSILON, exp, equal);
    }

    @Test
    public void test04CompareNoEpsilon() {
        int i=0;
        System.err.println();
        testCompareNoEpsilon(i++,  0, 0f, 0f);
        testCompareNoEpsilon(i++,  0, Float.MAX_VALUE, Float.MAX_VALUE);
        testCompareNoEpsilon(i++,  0, Float.MIN_VALUE, Float.MIN_VALUE);
        testCompareNoEpsilon(i++,  0, Float.MIN_NORMAL, Float.MIN_NORMAL);
        testCompareNoEpsilon(i++,  0, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        testCompareNoEpsilon(i++,  0, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        testCompareNoEpsilon(i++,  0, Float.NaN, Float.NaN);

        testCompareNoEpsilon(i++,  1,  1f,  0f);
        testCompareNoEpsilon(i++, -1,  0f,  1f);
        testCompareNoEpsilon(i++,  1,  0f, -1f);
        testCompareNoEpsilon(i++, -1, -1f,  0f);

        testCompareNoEpsilon(i++,  1, Float.MAX_VALUE, Float.MIN_VALUE);
        testCompareNoEpsilon(i++, -1, Float.MIN_VALUE, Float.MAX_VALUE);
        testCompareNoEpsilon(i++,  1, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
        testCompareNoEpsilon(i++, -1, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

        testCompareNoEpsilon(i++, -1,        0f, Float.NaN);
        testCompareNoEpsilon(i++,  1, Float.NaN, 0f);

        testCompareNoEpsilon(i++, -1,  -0f, 0f);
        testCompareNoEpsilon(i++,  1, 0f, -0f);

        // Unpredicted .. accuracy beyond epsilon, or deltaMachEpsLEQEpsilon or deltaFixedEpsLEQEpsilon;
        dumpTestNE(i++, 0, 1f, 1f-MACH_EPSILON/2f);
        dumpTestNE(i++, 0, 0f, 0f-MACH_EPSILON/2f);
        dumpTestNE(i++, 0, 1f, 1f+MACH_EPSILON/2f);
        dumpTestNE(i++, 0, 0f, 0f+MACH_EPSILON/2f);
        dumpTestNE(i++, 0, 1f, 1f-Float.MIN_VALUE);
        dumpTestNE(i++, 0, 0f, 0f-Float.MIN_VALUE);
        dumpTestNE(i++, 0, 1f, 1f+Float.MIN_VALUE);
        dumpTestNE(i++, 0, 0f, 0f+Float.MIN_VALUE);
    }
    private void testCompareNoEpsilon(final int tstNum, final int exp, final float a, final float b) {
        final boolean equal =  FloatUtil.isEqualRaw(a, b);
        final int comp = FloatUtil.compare(a, b);
        final float delta = a-b;
        System.err.println("Comp.NE."+tstNum+": a: "+a+", b: "+b+" -> d "+delta+", equal "+equal+", comp: exp "+exp+" has "+comp);
        Assert.assertEquals("Compare failed a: "+a+", b: "+b, exp, comp);
    }

    @Test
    public void test05aCompareWithFixedEpsilon() {
        test05CompareWithEpsilon(10, FloatUtil.EPSILON);
    }
    @Test
    public void test05bCompareWithMachEpsilon() {
        test05CompareWithEpsilon(50, MACH_EPSILON);
    }
    private void test05CompareWithEpsilon(int i, final float epsilon) {
        System.err.println();
        testCompareWithEpsilon(i++, 0, 0f, 0f, epsilon);
        testCompareWithEpsilon(i++, 0, 1f, 1f-epsilon/2f, epsilon);
        testCompareWithEpsilon(i++, 0, 1f, 1f+epsilon/2f, epsilon);
        testCompareWithEpsilon(i++, 0, 1f, 1f-Float.MIN_VALUE, epsilon);
        testCompareWithEpsilon(i++, 0, 1f, 1f+Float.MIN_VALUE, epsilon);
        testCompareWithEpsilon(i++, 0, Float.MAX_VALUE, Float.MAX_VALUE, epsilon);
        testCompareWithEpsilon(i++, 0, Float.MIN_VALUE, Float.MIN_VALUE, epsilon);
        testCompareWithEpsilon(i++, 0, Float.MIN_NORMAL, Float.MIN_NORMAL, epsilon);
        testCompareWithEpsilon(i++, 0, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, epsilon);
        testCompareWithEpsilon(i++, 0, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, epsilon);
        testCompareWithEpsilon(i++, 0, Float.NaN, Float.NaN, epsilon);

        testCompareWithEpsilon(i++,  1,  1f,  0f, epsilon);
        testCompareWithEpsilon(i++, -1,  0f,  1f, epsilon);
        testCompareWithEpsilon(i++,  1,  0f, -1f, epsilon);
        testCompareWithEpsilon(i++, -1, -1f,  0f, epsilon);

        testCompareWithEpsilon(i++,  1, Float.MAX_VALUE, Float.MIN_VALUE, epsilon);
        testCompareWithEpsilon(i++, -1, Float.MIN_VALUE, Float.MAX_VALUE, epsilon);
        testCompareWithEpsilon(i++,  1, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, epsilon);
        testCompareWithEpsilon(i++, -1, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, epsilon);

        testCompareWithEpsilon(i++, -1, 0f,Float.NaN, epsilon);
        testCompareWithEpsilon(i++,  1, Float.NaN, 0f, epsilon);

        testCompareWithEpsilon(i++,  0, -0f, 0f, epsilon);
        testCompareWithEpsilon(i++,  0, 0f, -0f, epsilon);
    }
    private void testCompareWithEpsilon(final int tstNum, final int exp, final float a, final float b, final float epsilon) {
        final boolean equal =  FloatUtil.isEqual(a, b, epsilon);
        final int comp = FloatUtil.compare(a, b, epsilon);
        final float delta = a-b;
        System.err.println("Comp.WE."+tstNum+": a: "+a+", b: "+b+" -> d "+delta+", equal "+equal+", comp: exp "+exp+" has "+comp);
        Assert.assertEquals("Compare failed a: "+a+", b: "+b+" within "+epsilon, exp, comp);
    }


    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestFloatUtil01NOUI.class.getName());
    }
}
