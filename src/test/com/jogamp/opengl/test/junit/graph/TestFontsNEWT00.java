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
import com.jogamp.graph.font.Font.Glyph;
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
            final float pixelSize = font.getPixelSize(fontSize, dpi);
            System.err.println(font.getFullFamilyName(null).toString()+": "+fontSize+"p, "+dpi+"dpi -> "+pixelSize+"px:");
            testFontGlyphAdvancedSize(font, ' ', Glyph.ID_SPACE, fontSize, dpi, pixelSize);
            testFontGlyphAdvancedSize(font, 'X', 'X', fontSize, dpi, pixelSize);
        }
    }
    void testFontGlyphAdvancedSize(final Font font, final char c, final int glyphID,
                                   final float fontSize, final float dpi, final float pixelSize) {
        final float glyphScale = font.getGlyph(c).getScale(pixelSize);
        final float fontScale = font.getMetrics().getScale(pixelSize);

        // return this.metrics.getAdvance(pixelSize, useFrationalMetrics);
        // this.metrics.getAdvance(pixelSize, useFrationalMetrics)
        // this.advance * this.font.getMetrics().getScale(pixelSize)
        // font.getHmtxTable().getAdvanceWidth(glyphID) * this.font.getMetrics().getScale(pixelSize)
        final float spaceAdvanceSizeOfGlyph = font.getGlyph(c).getAdvance(pixelSize, true);

        // font.getHmtxTable().getAdvanceWidth(glyphID) * metrics.getScale(pixelSize);
        // font.getHmtxTable().getAdvanceWidth(glyphID) * pixelSize * unitsPerEM_Inv;
        final float spaceAdvanceWidth = font.getAdvanceWidth(glyphID, pixelSize);
        System.err.println("    Char '"+c+"', "+glyphID+":");
        System.err.println("        glyphScale "+glyphScale);
        System.err.println("        glyphSize  "+spaceAdvanceSizeOfGlyph);
        System.err.println("        fontScale  "+fontScale);
        System.err.println("        fontWidth  "+spaceAdvanceWidth);
    }
}
