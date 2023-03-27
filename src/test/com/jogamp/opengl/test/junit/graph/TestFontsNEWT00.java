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
package com.jogamp.opengl.test.junit.graph;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontScale;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.test.junit.util.UITestCase;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFontsNEWT00 extends UITestCase {
    static boolean mainRun = false;

    static int atoi(final String a) {
        try {
            return Integer.parseInt(a);
        } catch (final Exception ex) { throw new RuntimeException(ex); }
    }

    public static void main(final String args[]) throws IOException {
        mainRun = true;
        final String tstname = TestFontsNEWT00.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

    @Test
    public void test00() throws InterruptedException, IOException {
        testFontImpl(FontSet01.getSet01());
    }
    void testFontImpl(final Font[] fonts) throws InterruptedException, IOException {
        final float fontSize = 10;
        final float dpi = 96;
        for(int i=0; i<fonts.length; i++) {
            final Font font = fonts[i];
            System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.err.println(font.getAllNames(null, "\n"));
            System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            final float pixelSize = FontScale.toPixels(fontSize, dpi);
            System.err.println(font.getFullFamilyName()+": "+fontSize+"p, "+dpi+"dpi -> "+pixelSize+"px:");
            System.err.println(font.fullString());
            testFontGlyph01(font, 'X', pixelSize);
            testFontGlyph01(font, 'j', pixelSize);
            testFontGlyph01(font, ' ', pixelSize);
            testFontGlyph02(font, 'X', 'X');
            testFontGlyph02(font, 't', '.');
            testFontGlyph02(font, 'f', 'f');
        }
    }
    void testFontGlyph01(final Font font, final char c, final float pixelSize) {
        final int glyphID = font.getGlyphID(c);
        final int s0 = font.getAdvanceWidthFU(glyphID);
        final Font.Glyph glyph = font.getGlyph(glyphID);
        final int s1 = glyph.getAdvanceFU();

        final int unitsPerEM = font.getMetrics().getUnitsPerEM();

        final float s0_em = font.getAdvanceWidth(glyphID);
        final float s1_em = glyph.getAdvance();

        final float s0_px = s0_em * pixelSize;
        final float s1_px = s1_em * pixelSize;

        System.err.println("    Char '"+c+"', id "+glyphID+", font-px "+pixelSize+", unitsPerEM "+unitsPerEM+":");
        System.err.println("      "+glyph);
        System.err.println("      Advance");
        System.err.println("        funits "+s0+", "+s1);
        System.err.println("            em "+s0_em+", "+s1_em);
        System.err.println("            px "+s0_px+", "+s1_px);
        System.err.println("      AABBox");
        System.err.println("        funits "+glyph.getBoundsFU());
        System.err.println("            em "+glyph.getBounds(new AABBox(), new float[3]));

        Assert.assertEquals(s0, s1);

        Assert.assertEquals((float)s0/(float)unitsPerEM, s0_em, FloatUtil.EPSILON);
        Assert.assertEquals((float)s1/(float)unitsPerEM, s1_em, FloatUtil.EPSILON);
        Assert.assertEquals(s0_em, s1_em, FloatUtil.EPSILON);

        Assert.assertEquals(s0_em*pixelSize, s0_px, FloatUtil.EPSILON);
        Assert.assertEquals(s1_em*pixelSize, s1_px, FloatUtil.EPSILON);
        Assert.assertEquals(s0_px, s1_px, FloatUtil.EPSILON);
    }
    void testFontGlyph02(final Font font, final char left, final char right) {
        final int glyphid_left = font.getGlyphID(left);
        final int glyphid_right = font.getGlyphID(right);
        final Font.Glyph glyph_left = font.getGlyph(glyphid_left);

        final int k_val = glyph_left.getKerningFU(glyphid_right);

        System.err.println("    Font "+font.getFullFamilyName());
        System.err.println("    Char left['"+left+"', id "+glyphid_left+", kpairs "+glyph_left.getKerningPairCount()+
                                "], right['"+right+"', id "+glyphid_right+"], kerning "+k_val);
        System.err.println("      "+glyph_left);
        // System.err.println("      "+glyph_left.fullString());
    }
}
