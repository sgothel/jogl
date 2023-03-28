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
package com.jogamp.opengl.test.junit.graph;

import com.jogamp.graph.font.FontScale;
import com.jogamp.junit.util.JunitTracer;

import com.jogamp.opengl.math.FloatUtil;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFontScale01NOUI extends JunitTracer {

    @Test
    public void test01() {
        Assert.assertEquals(10f/72f, FontScale.ptToInch(10), FloatUtil.EPSILON); // pt -> inch
        Assert.assertEquals(10f/72f * 25.4f, FontScale.ptToMM(10), FloatUtil.EPSILON); // pt -> mm
        Assert.assertEquals(0.138888f, FontScale.ptToInch(10), 0.000001f); // pt -> inch
        Assert.assertEquals(3.527778f, FontScale.ptToMM(10), 0.000001f); // pt -> mm

        Assert.assertEquals(5f, FontScale.ppiToPPMM(new float[] { 127f, 127f })[0], FloatUtil.EPSILON); // dpi -> pixel/mm
        Assert.assertEquals(127f, FontScale.ppmmToPPI(new float[] { 5f, 5f })[0], FloatUtil.EPSILON); // pixel/mm -> dpi

        System.err.println("10pt @ 128 dpi -> pixels "+FontScale.toPixels(10 /* pt */, 127f /* dpi */));
        System.err.println("10pt @  5 ppmm -> pixels "+FontScale.toPixels2(10 /* pt */,   5f /* ppmm */));
        Assert.assertEquals(17.638889f, FontScale.toPixels(10 /* pt */, 127f /* dpi */), 0.000001f);
        Assert.assertEquals(17.638889f, FontScale.toPixels2(10 /* pt */,   5f /* ppmm */), 0.000001f);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestFontScale01NOUI.class.getName());
    }
}
