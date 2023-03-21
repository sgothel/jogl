/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.demos.graph.ui.testshapes;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.ui.gl.Shape;

/**
 * GPU based resolution independent test object
 * - FreeMono-Regular, capital case 'M'
 * - TTF Shape for Glyph 48
 */
public class Glyph03FreeMonoRegular_M extends Shape {

    public Glyph03FreeMonoRegular_M(final int renderModes) {
        super(renderModes);
    }

    @SuppressWarnings("unused")
    @Override
    protected void addShapeToRegion() {
        final OutlineShape shape = new OutlineShape(vertexFactory);

        if( false ) {
            // Start TTF Shape for Glyph 48
            // GlyphShape<48>: offset 0 of 45/45 points
            //  pM[044] P[483/522, on true, end true]
            //  p0[000] P[326/169, on true, end false]
            //  p1[001] P[280/169, on true, end false]
            //  p2[002] P[121/522, on true, end false]
            // 000: B0a: move-to p0
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();
            shape.addVertex(0, 0.326000f, 0.169000f, true);
            // 000: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.280000f, 0.169000f, true);
            // GlyphShape<48>: offset 1 of 45/45 points
            //  pM[000] P[326/169, on true, end false]
            //  p0[001] P[280/169, on true, end false]
            //  p1[002] P[121/522, on true, end false]
            //  p2[003] P[113/522, on true, end false]
            // 001: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.121000f, 0.522000f, true);
            // GlyphShape<48>: offset 2 of 45/45 points
            //  pM[001] P[280/169, on true, end false]
            //  p0[002] P[121/522, on true, end false]
            //  p1[003] P[113/522, on true, end false]
            //  p2[004] P[113/41, on true, end false]
            // 002: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.113000f, 0.522000f, true);
            // GlyphShape<48>: offset 3 of 45/45 points
            //  pM[002] P[121/522, on true, end false]
            //  p0[003] P[113/522, on true, end false]
            //  p1[004] P[113/41, on true, end false]
            //  p2[005] P[187/41, on true, end false]
            // 003: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.113000f, 0.041000f, true);
            // GlyphShape<48>: offset 4 of 45/45 points
            //  pM[003] P[113/522, on true, end false]
            //  p0[004] P[113/41, on true, end false]
            //  p1[005] P[187/41, on true, end false]
            //  p2[006] P[215/41, on false, end false]
            // 004: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.187000f, 0.041000f, true);
            // GlyphShape<48>: offset 5 of 45/45 points
            //  pM[004] P[113/41, on true, end false]
            //  p0[005] P[187/41, on true, end false]
            //  p1[006] P[215/41, on false, end false]
            //  p2[007] P[215/21, on true, end false]
            // 005: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.215000f, 0.041000f, false);
            shape.addVertex(0, 0.215000f, 0.021000f, true);
            // GlyphShape<48>: offset 7 of 45/45 points
            //  pM[006] P[215/41, on false, end false]
            //  p0[007] P[215/21, on true, end false]
            //  p1[008] P[215/0, on false, end false]
            //  p2[009] P[187/0, on true, end false]
            // 007: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.215000f, 0.000000f, false);
            shape.addVertex(0, 0.187000f, 0.000000f, true);
            // GlyphShape<48>: offset 9 of 45/45 points
            //  pM[008] P[215/0, on false, end false]
            //  p0[009] P[187/0, on true, end false]
            //  p1[010] P[38/0, on true, end false]
            //  p2[011] P[11/0, on false, end false]
            // 009: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.038000f, 0.000000f, true);
            // GlyphShape<48>: offset 10 of 45/45 points
            //  pM[009] P[187/0, on true, end false]
            //  p0[010] P[38/0, on true, end false]
            //  p1[011] P[11/0, on false, end false]
            //  p2[012] P[11/21, on true, end false]
            // 010: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.011000f, 0.000000f, false);
            shape.addVertex(0, 0.011000f, 0.021000f, true);
            // GlyphShape<48>: offset 12 of 45/45 points
            //  pM[011] P[11/0, on false, end false]
            //  p0[012] P[11/21, on true, end false]
            //  p1[013] P[11/41, on false, end false]
            //  p2[014] P[38/41, on true, end false]
            // 012: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.011000f, 0.041000f, false);
            shape.addVertex(0, 0.038000f, 0.041000f, true);
            // GlyphShape<48>: offset 14 of 45/45 points
            //  pM[013] P[11/41, on false, end false]
            //  p0[014] P[38/41, on true, end false]
            //  p1[015] P[72/41, on true, end false]
            //  p2[016] P[72/522, on true, end false]
            // 014: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.072000f, 0.041000f, true);
            // GlyphShape<48>: offset 15 of 45/45 points
            //  pM[014] P[38/41, on true, end false]
            //  p0[015] P[72/41, on true, end false]
            //  p1[016] P[72/522, on true, end false]
            //  p2[017] P[47/522, on true, end false]
            // 015: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.072000f, 0.522000f, true);
            // GlyphShape<48>: offset 16 of 45/45 points
            //  pM[015] P[72/41, on true, end false]
            //  p0[016] P[72/522, on true, end false]
            //  p1[017] P[47/522, on true, end false]
            //  p2[018] P[20/522, on false, end false]
            // 016: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.047000f, 0.522000f, true);
            // GlyphShape<48>: offset 17 of 45/45 points
            //  pM[016] P[72/522, on true, end false]
            //  p0[017] P[47/522, on true, end false]
            //  p1[018] P[20/522, on false, end false]
            //  p2[019] P[20/543, on true, end false]
            // 017: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.020000f, 0.522000f, false);
            shape.addVertex(0, 0.020000f, 0.543000f, true);
            // GlyphShape<48>: offset 19 of 45/45 points
            //  pM[018] P[20/522, on false, end false]
            //  p0[019] P[20/543, on true, end false]
            //  p1[020] P[20/563, on false, end false]
            //  p2[021] P[47/563, on true, end false]
            // 019: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.020000f, 0.563000f, false);
            shape.addVertex(0, 0.047000f, 0.563000f, true);
            // GlyphShape<48>: offset 21 of 45/45 points
            //  pM[020] P[20/563, on false, end false]
            //  p0[021] P[47/563, on true, end false]
            //  p1[022] P[146/563, on true, end false]
            //  p2[023] P[303/215, on true, end false]
            // 021: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.146000f, 0.563000f, true);
            // GlyphShape<48>: offset 22 of 45/45 points
            //  pM[021] P[47/563, on true, end false]
            //  p0[022] P[146/563, on true, end false]
            //  p1[023] P[303/215, on true, end false]
            //  p2[024] P[457/563, on true, end false]
            // 022: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.303000f, 0.215000f, true);
            // GlyphShape<48>: offset 23 of 45/45 points
            //  pM[022] P[146/563, on true, end false]
            //  p0[023] P[303/215, on true, end false]
            //  p1[024] P[457/563, on true, end false]
            //  p2[025] P[557/563, on true, end false]
            // 023: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.457000f, 0.563000f, true);
            // GlyphShape<48>: offset 24 of 45/45 points
            //  pM[023] P[303/215, on true, end false]
            //  p0[024] P[457/563, on true, end false]
            //  p1[025] P[557/563, on true, end false]
            //  p2[026] P[584/563, on false, end false]
            // 024: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.557000f, 0.563000f, true);
            // GlyphShape<48>: offset 25 of 45/45 points
            //  pM[024] P[457/563, on true, end false]
            //  p0[025] P[557/563, on true, end false]
            //  p1[026] P[584/563, on false, end false]
            //  p2[027] P[584/543, on true, end false]
            // 025: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.584000f, 0.563000f, false);
            shape.addVertex(0, 0.584000f, 0.543000f, true);
            // GlyphShape<48>: offset 27 of 45/45 points
            //  pM[026] P[584/563, on false, end false]
            //  p0[027] P[584/543, on true, end false]
            //  p1[028] P[584/522, on false, end false]
            //  p2[029] P[557/522, on true, end false]
            // 027: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.584000f, 0.522000f, false);
            shape.addVertex(0, 0.557000f, 0.522000f, true);
            // GlyphShape<48>: offset 29 of 45/45 points
            //  pM[028] P[584/522, on false, end false]
            //  p0[029] P[557/522, on true, end false]
            //  p1[030] P[532/522, on true, end false]
            //  p2[031] P[532/41, on true, end false]
            // 029: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.532000f, 0.522000f, true);
            // GlyphShape<48>: offset 30 of 45/45 points
            //  pM[029] P[557/522, on true, end false]
            //  p0[030] P[532/522, on true, end false]
            //  p1[031] P[532/41, on true, end false]
            //  p2[032] P[566/41, on true, end false]
            // 030: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.532000f, 0.041000f, true);
            // GlyphShape<48>: offset 31 of 45/45 points
            //  pM[030] P[532/522, on true, end false]
            //  p0[031] P[532/41, on true, end false]
            //  p1[032] P[566/41, on true, end false]
            //  p2[033] P[593/41, on false, end false]
            // 031: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.566000f, 0.041000f, true);
            // GlyphShape<48>: offset 32 of 45/45 points
            //  pM[031] P[532/41, on true, end false]
            //  p0[032] P[566/41, on true, end false]
            //  p1[033] P[593/41, on false, end false]
            //  p2[034] P[593/21, on true, end false]
            // 032: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.593000f, 0.041000f, false);
            shape.addVertex(0, 0.593000f, 0.021000f, true);
            // GlyphShape<48>: offset 34 of 45/45 points
            //  pM[033] P[593/41, on false, end false]
            //  p0[034] P[593/21, on true, end false]
            //  p1[035] P[593/0, on false, end false]
            //  p2[036] P[566/0, on true, end false]
            // 034: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.593000f, 0.000000f, false);
            shape.addVertex(0, 0.566000f, 0.000000f, true);
            // GlyphShape<48>: offset 36 of 45/45 points
            //  pM[035] P[593/0, on false, end false]
            //  p0[036] P[566/0, on true, end false]
            //  p1[037] P[417/0, on true, end false]
            //  p2[038] P[390/0, on false, end false]
            // 036: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.417000f, 0.000000f, true);
            // GlyphShape<48>: offset 37 of 45/45 points
            //  pM[036] P[566/0, on true, end false]
            //  p0[037] P[417/0, on true, end false]
            //  p1[038] P[390/0, on false, end false]
            //  p2[039] P[390/21, on true, end false]
            // 037: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.390000f, 0.000000f, false);
            shape.addVertex(0, 0.390000f, 0.021000f, true);
            // GlyphShape<48>: offset 39 of 45/45 points
            //  pM[038] P[390/0, on false, end false]
            //  p0[039] P[390/21, on true, end false]
            //  p1[040] P[390/41, on false, end false]
            //  p2[041] P[417/41, on true, end false]
            // 039: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.390000f, 0.041000f, false);
            shape.addVertex(0, 0.417000f, 0.041000f, true);
            // GlyphShape<48>: offset 41 of 45/45 points
            //  pM[040] P[390/41, on false, end false]
            //  p0[041] P[417/41, on true, end false]
            //  p1[042] P[491/41, on true, end false]
            //  p2[043] P[491/522, on true, end false]
            // 041: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.491000f, 0.041000f, true);
            // GlyphShape<48>: offset 42 of 45/45 points
            //  pM[041] P[417/41, on true, end false]
            //  p0[042] P[491/41, on true, end false]
            //  p1[043] P[491/522, on true, end false]
            //  p2[044] P[483/522, on true, end true]
            // 042: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.491000f, 0.522000f, true);
            // GlyphShape<48>: offset 43 of 45/45 points
            //  pM[042] P[491/41, on true, end false]
            //  p0[043] P[491/522, on true, end false]
            //  p1[044] P[483/522, on true, end true]
            //  p2[000] P[326/169, on true, end false]
            // 043: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.483000f, 0.522000f, true);
            // GlyphShape<48>: offset 44 of 45/45 points
            //  pM[043] P[491/522, on true, end false]
            //  p0[044] P[483/522, on true, end true]
            //  p1[000] P[326/169, on true, end false]
            //  p2[001] P[280/169, on true, end false]
            // 044: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.326000f, 0.169000f, true);
            System.err.println("Glyph03FreeMonoRegular_M.shape01a.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);

            // End Shape for Glyph 48
        } else if( false ) {
            // Start TTF Shape for Glyph 48
            // GlyphShape<48>: offset 0 of 45/45 points
            //  pM[044] P[483/522, on true, end true]
            //  p0[000] P[326/169, on true, end false]
            //  p1[001] P[280/169, on true, end false]
            //  p2[002] P[121/522, on true, end false]
            // 000: B0a: move-to p0
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();
            shape.addVertex(0, 0.326000f, 0.169000f, true);
            // 000: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.280000f, 0.169000f, true);
            // GlyphShape<48>: offset 1 of 45/45 points
            //  pM[000] P[326/169, on true, end false]
            //  p0[001] P[280/169, on true, end false]
            //  p1[002] P[121/522, on true, end false]
            //  p2[003] P[113/522, on true, end false]
            // 001: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.121000f, 0.522000f, true);
            // GlyphShape<48>: offset 2 of 45/45 points
            //  pM[001] P[280/169, on true, end false]
            //  p0[002] P[121/522, on true, end false]
            //  p1[003] P[113/522, on true, end false]
            //  p2[004] P[113/41, on true, end false]
            // 002: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.113000f, 0.522000f, true);
            // GlyphShape<48>: offset 3 of 45/45 points
            //  pM[002] P[121/522, on true, end false]
            //  p0[003] P[113/522, on true, end false]
            //  p1[004] P[113/41, on true, end false]
            //  p2[005] P[187/41, on true, end false]
            // 003: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.113000f, 0.041000f, true);
            // GlyphShape<48>: offset 4 of 45/45 points
            //  pM[003] P[113/522, on true, end false]
            //  p0[004] P[113/41, on true, end false]
            //  p1[005] P[187/41, on true, end false]
            //  p2[006] P[215/41, on false, end false]
            // 004: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.187000f, 0.041000f, true);
            // GlyphShape<48>: offset 5 of 45/45 points
            //  pM[004] P[113/41, on true, end false]
            //  p0[005] P[187/41, on true, end false]
            //  p1[006] P[215/41, on false, end false]
            //  p2[007] P[215/21, on true, end false]
            // 005: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.215000f, 0.041000f, true); // curve -> line
            shape.addVertex(0, 0.215000f, 0.021000f, true);
            // GlyphShape<48>: offset 7 of 45/45 points
            //  pM[006] P[215/41, on false, end false]
            //  p0[007] P[215/21, on true, end false]
            //  p1[008] P[215/0, on false, end false]
            //  p2[009] P[187/0, on true, end false]
            // 007: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.215000f, 0.000000f, true); // curve -> line
            shape.addVertex(0, 0.187000f, 0.000000f, true);
            // GlyphShape<48>: offset 9 of 45/45 points
            //  pM[008] P[215/0, on false, end false]
            //  p0[009] P[187/0, on true, end false]
            //  p1[010] P[38/0, on true, end false]
            //  p2[011] P[11/0, on false, end false]
            // 009: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.038000f, 0.000000f, true);
            // GlyphShape<48>: offset 10 of 45/45 points
            //  pM[009] P[187/0, on true, end false]
            //  p0[010] P[38/0, on true, end false]
            //  p1[011] P[11/0, on false, end false]
            //  p2[012] P[11/21, on true, end false]
            // 010: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.011000f, 0.000000f, true); // curve -> line
            shape.addVertex(0, 0.011000f, 0.021000f, true);
            // GlyphShape<48>: offset 12 of 45/45 points
            //  pM[011] P[11/0, on false, end false]
            //  p0[012] P[11/21, on true, end false]
            //  p1[013] P[11/41, on false, end false]
            //  p2[014] P[38/41, on true, end false]
            // 012: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.011000f, 0.041000f, true); // curve -> line
            shape.addVertex(0, 0.038000f, 0.041000f, true);
            // GlyphShape<48>: offset 14 of 45/45 points
            //  pM[013] P[11/41, on false, end false]
            //  p0[014] P[38/41, on true, end false]
            //  p1[015] P[72/41, on true, end false]
            //  p2[016] P[72/522, on true, end false]
            // 014: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.072000f, 0.041000f, true);
            // GlyphShape<48>: offset 15 of 45/45 points
            //  pM[014] P[38/41, on true, end false]
            //  p0[015] P[72/41, on true, end false]
            //  p1[016] P[72/522, on true, end false]
            //  p2[017] P[47/522, on true, end false]
            // 015: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.072000f, 0.522000f, true);
            // GlyphShape<48>: offset 16 of 45/45 points
            //  pM[015] P[72/41, on true, end false]
            //  p0[016] P[72/522, on true, end false]
            //  p1[017] P[47/522, on true, end false]
            //  p2[018] P[20/522, on false, end false]
            // 016: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.047000f, 0.522000f, true);
            // GlyphShape<48>: offset 17 of 45/45 points
            //  pM[016] P[72/522, on true, end false]
            //  p0[017] P[47/522, on true, end false]
            //  p1[018] P[20/522, on false, end false]
            //  p2[019] P[20/543, on true, end false]
            // 017: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.020000f, 0.522000f, true); // curve -> line
            shape.addVertex(0, 0.020000f, 0.543000f, true);
            // GlyphShape<48>: offset 19 of 45/45 points
            //  pM[018] P[20/522, on false, end false]
            //  p0[019] P[20/543, on true, end false]
            //  p1[020] P[20/563, on false, end false]
            //  p2[021] P[47/563, on true, end false]
            // 019: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.020000f, 0.563000f, true); // curve -> line
            shape.addVertex(0, 0.047000f, 0.563000f, true);
            // GlyphShape<48>: offset 21 of 45/45 points
            //  pM[020] P[20/563, on false, end false]
            //  p0[021] P[47/563, on true, end false]
            //  p1[022] P[146/563, on true, end false]
            //  p2[023] P[303/215, on true, end false]
            // 021: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.146000f, 0.563000f, true);
            // GlyphShape<48>: offset 22 of 45/45 points
            //  pM[021] P[47/563, on true, end false]
            //  p0[022] P[146/563, on true, end false]
            //  p1[023] P[303/215, on true, end false]
            //  p2[024] P[457/563, on true, end false]
            // 022: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.303000f, 0.215000f, true);
            // GlyphShape<48>: offset 23 of 45/45 points
            //  pM[022] P[146/563, on true, end false]
            //  p0[023] P[303/215, on true, end false]
            //  p1[024] P[457/563, on true, end false]
            //  p2[025] P[557/563, on true, end false]
            // 023: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.457000f, 0.563000f, true);
            // GlyphShape<48>: offset 24 of 45/45 points
            //  pM[023] P[303/215, on true, end false]
            //  p0[024] P[457/563, on true, end false]
            //  p1[025] P[557/563, on true, end false]
            //  p2[026] P[584/563, on false, end false]
            // 024: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.557000f, 0.563000f, true);
            // GlyphShape<48>: offset 25 of 45/45 points
            //  pM[024] P[457/563, on true, end false]
            //  p0[025] P[557/563, on true, end false]
            //  p1[026] P[584/563, on false, end false]
            //  p2[027] P[584/543, on true, end false]
            // 025: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.584000f, 0.563000f, true); // curve -> line
            shape.addVertex(0, 0.584000f, 0.543000f, true);
            // GlyphShape<48>: offset 27 of 45/45 points
            //  pM[026] P[584/563, on false, end false]
            //  p0[027] P[584/543, on true, end false]
            //  p1[028] P[584/522, on false, end false]
            //  p2[029] P[557/522, on true, end false]
            // 027: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.584000f, 0.522000f, true); // curve -> line
            shape.addVertex(0, 0.557000f, 0.522000f, true);
            // GlyphShape<48>: offset 29 of 45/45 points
            //  pM[028] P[584/522, on false, end false]
            //  p0[029] P[557/522, on true, end false]
            //  p1[030] P[532/522, on true, end false]
            //  p2[031] P[532/41, on true, end false]
            // 029: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.532000f, 0.522000f, true);
            // GlyphShape<48>: offset 30 of 45/45 points
            //  pM[029] P[557/522, on true, end false]
            //  p0[030] P[532/522, on true, end false]
            //  p1[031] P[532/41, on true, end false]
            //  p2[032] P[566/41, on true, end false]
            // 030: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.532000f, 0.041000f, true);
            // GlyphShape<48>: offset 31 of 45/45 points
            //  pM[030] P[532/522, on true, end false]
            //  p0[031] P[532/41, on true, end false]
            //  p1[032] P[566/41, on true, end false]
            //  p2[033] P[593/41, on false, end false]
            // 031: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.566000f, 0.041000f, true);
            // GlyphShape<48>: offset 32 of 45/45 points
            //  pM[031] P[532/41, on true, end false]
            //  p0[032] P[566/41, on true, end false]
            //  p1[033] P[593/41, on false, end false]
            //  p2[034] P[593/21, on true, end false]
            // 032: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.593000f, 0.041000f, true); // curve -> line
            shape.addVertex(0, 0.593000f, 0.021000f, true);
            // GlyphShape<48>: offset 34 of 45/45 points
            //  pM[033] P[593/41, on false, end false]
            //  p0[034] P[593/21, on true, end false]
            //  p1[035] P[593/0, on false, end false]
            //  p2[036] P[566/0, on true, end false]
            // 034: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.593000f, 0.000000f, true); // curve -> line
            shape.addVertex(0, 0.566000f, 0.000000f, true);
            // GlyphShape<48>: offset 36 of 45/45 points
            //  pM[035] P[593/0, on false, end false]
            //  p0[036] P[566/0, on true, end false]
            //  p1[037] P[417/0, on true, end false]
            //  p2[038] P[390/0, on false, end false]
            // 036: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.417000f, 0.000000f, true);
            // GlyphShape<48>: offset 37 of 45/45 points
            //  pM[036] P[566/0, on true, end false]
            //  p0[037] P[417/0, on true, end false]
            //  p1[038] P[390/0, on false, end false]
            //  p2[039] P[390/21, on true, end false]
            // 037: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.390000f, 0.000000f, true); // curve -> line
            shape.addVertex(0, 0.390000f, 0.021000f, true);
            // GlyphShape<48>: offset 39 of 45/45 points
            //  pM[038] P[390/0, on false, end false]
            //  p0[039] P[390/21, on true, end false]
            //  p1[040] P[390/41, on false, end false]
            //  p2[041] P[417/41, on true, end false]
            // 039: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.390000f, 0.041000f, true); // curve -> line
            shape.addVertex(0, 0.417000f, 0.041000f, true);
            // GlyphShape<48>: offset 41 of 45/45 points
            //  pM[040] P[390/41, on false, end false]
            //  p0[041] P[417/41, on true, end false]
            //  p1[042] P[491/41, on true, end false]
            //  p2[043] P[491/522, on true, end false]
            // 041: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.491000f, 0.041000f, true);
            // GlyphShape<48>: offset 42 of 45/45 points
            //  pM[041] P[417/41, on true, end false]
            //  p0[042] P[491/41, on true, end false]
            //  p1[043] P[491/522, on true, end false]
            //  p2[044] P[483/522, on true, end true]
            // 042: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.491000f, 0.522000f, true);
            // GlyphShape<48>: offset 43 of 45/45 points
            //  pM[042] P[491/41, on true, end false]
            //  p0[043] P[491/522, on true, end false]
            //  p1[044] P[483/522, on true, end true]
            //  p2[000] P[326/169, on true, end false]
            // 043: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.483000f, 0.522000f, true);
            // GlyphShape<48>: offset 44 of 45/45 points
            //  pM[043] P[491/522, on true, end false]
            //  p0[044] P[483/522, on true, end true]
            //  p1[000] P[326/169, on true, end false]
            //  p2[001] P[280/169, on true, end false]
            // 044: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.326000f, 0.169000f, true);
            System.err.println("Glyph03FreeMonoRegular_M.shape01a.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);
        } else {
            final boolean with_left_leg = true; // ERROR
            final boolean with_right_leg = false; // OK

            // Start TTF Shape for Glyph 48
            // GlyphShape<48>: offset 0 of 45/45 points
            //  pM[044] P[483/522, on true, end true]
            //  p0[000] P[326/169, on true, end false]
            //  p1[001] P[280/169, on true, end false]
            //  p2[002] P[121/522, on true, end false]
            // 000: B0a: move-to p0
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();
            shape.addVertex(0, 0.326000f, 0.169000f, true);
            // 000: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.280000f, 0.169000f, true);
            // GlyphShape<48>: offset 1 of 45/45 points
            //  pM[000] P[326/169, on true, end false]
            //  p0[001] P[280/169, on true, end false]
            //  p1[002] P[121/522, on true, end false]
            //  p2[003] P[113/522, on true, end false]
            // 001: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.121000f, 0.522000f, true); // ID 11

            // GlyphShape<48>: offset 2 of 45/45 points
            //  pM[001] P[280/169, on true, end false]
            //  p0[002] P[121/522, on true, end false]
            //  p1[003] P[113/522, on true, end false]
            //  p2[004] P[113/41, on true, end false]
            // 002: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.113000f, 0.522000f, true);

            if( with_left_leg ) {
                // GlyphShape<48>: offset 3 of 45/45 points
                //  pM[002] P[121/522, on true, end false]
                //  p0[003] P[113/522, on true, end false]
                //  p1[004] P[113/41, on true, end false]
                //  p2[005] P[187/41, on true, end false]
                // 003: B1: line-to p0-p1
                // Shape.LineTo:
                // shape.addVertex(0, 0.113000f, 0.041000f, true);

                shape.addVertex(0, 0.113000f, 0.000000f, true);
                shape.addVertex(0, 0.072000f, 0.000000f, true);

                // GlyphShape<48>: offset 14 of 45/45 points
                //  pM[013] P[11/41, on false, end false]
                //  p0[014] P[38/41, on true, end false]
                //  p1[015] P[72/41, on true, end false]
                //  p2[016] P[72/522, on true, end false]
                // 014: B1: line-to p0-p1
                // Shape.LineTo:
                // shape.addVertex(0, 0.072000f, 0.041000f, true);

                // GlyphShape<48>: offset 15 of 45/45 points
                //  pM[014] P[38/41, on true, end false]
                //  p0[015] P[72/41, on true, end false]
                //  p1[016] P[72/522, on true, end false]
                //  p2[017] P[47/522, on true, end false]
                // 015: B1: line-to p0-p1
                // Shape.LineTo:
                // shape.addVertex(0, 0.072000f, 0.522000f, true);

                shape.addVertex(0, 0.072000f, 0.563000f, true); // ID 7
            } else {
                shape.addVertex(0, 0.113000f, 0.563000f, true);
            }

            // GlyphShape<48>: offset 21 of 45/45 points
            //  pM[020] P[20/563, on false, end false]
            //  p0[021] P[47/563, on true, end false]
            //  p1[022] P[146/563, on true, end false]
            //  p2[023] P[303/215, on true, end false]
            // 021: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.146000f, 0.563000f, true);
            // GlyphShape<48>: offset 22 of 45/45 points
            //  pM[021] P[47/563, on true, end false]
            //  p0[022] P[146/563, on true, end false]
            //  p1[023] P[303/215, on true, end false]
            //  p2[024] P[457/563, on true, end false]
            // 022: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.303000f, 0.215000f, true);
            // GlyphShape<48>: offset 23 of 45/45 points
            //  pM[022] P[146/563, on true, end false]
            //  p0[023] P[303/215, on true, end false]
            //  p1[024] P[457/563, on true, end false]
            //  p2[025] P[557/563, on true, end false]
            // 023: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.457000f, 0.563000f, true); // ID 4

            if( with_right_leg ) {
                shape.addVertex(0, 0.532000f, 0.563000f, true);

                // GlyphShape<48>: offset 29 of 45/45 points
                //  pM[028] P[584/522, on false, end false]
                //  p0[029] P[557/522, on true, end false]
                //  p1[030] P[532/522, on true, end false]
                //  p2[031] P[532/41, on true, end false]
                // 029: B1: line-to p0-p1
                // Shape.LineTo:
                shape.addVertex(0, 0.532000f, 0.522000f, true);
                // GlyphShape<48>: offset 30 of 45/45 points
                //  pM[029] P[557/522, on true, end false]
                //  p0[030] P[532/522, on true, end false]
                //  p1[031] P[532/41, on true, end false]
                //  p2[032] P[566/41, on true, end false]
                // 030: B1: line-to p0-p1
                // Shape.LineTo:
                // shape.addVertex(0, 0.532000f, 0.041000f, true);

                shape.addVertex(0, 0.532000f, 0.000000f, true);
                shape.addVertex(0, 0.491000f, 0.000000f, true);
            } else {
                shape.addVertex(0, 0.491000f, 0.563000f, true); // ID 3
            }

            // GlyphShape<48>: offset 41 of 45/45 points
            //  pM[040] P[390/41, on false, end false]
            //  p0[041] P[417/41, on true, end false]
            //  p1[042] P[491/41, on true, end false]
            //  p2[043] P[491/522, on true, end false]
            // 041: B1: line-to p0-p1
            // Shape.LineTo:
            // shape.addVertex(0, 0.491000f, 0.041000f, true);

            // GlyphShape<48>: offset 42 of 45/45 points
            //  pM[041] P[417/41, on true, end false]
            //  p0[042] P[491/41, on true, end false]
            //  p1[043] P[491/522, on true, end false]
            //  p2[044] P[483/522, on true, end true]
            // 042: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.491000f, 0.522000f, true); // ID 2
            // GlyphShape<48>: offset 43 of 45/45 points
            //  pM[042] P[491/41, on true, end false]
            //  p0[043] P[491/522, on true, end false]
            //  p1[044] P[483/522, on true, end true]
            //  p2[000] P[326/169, on true, end false]
            // 043: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.483000f, 0.522000f, true); // ID 1
            // GlyphShape<48>: offset 44 of 45/45 points
            //  pM[043] P[491/522, on true, end false]
            //  p0[044] P[483/522, on true, end true]
            //  p1[000] P[326/169, on true, end false]
            //  p2[001] P[280/169, on true, end false]
            // 044: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.326000f, 0.169000f, true);
            System.err.println("Glyph03FreeMonoRegular_M.shape01a.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);
        }

        shape.setIsQuadraticNurbs();
        shape.setSharpness(shapesSharpness);
        region.addOutlineShape(shape, null, rgbaColor);

        box.resize(shape.getBounds());
    }

    @Override
    public String getSubString() {
        return super.getSubString();
    }
}
