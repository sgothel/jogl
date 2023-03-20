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

import com.jogamp.opengl.GL2ES2;
import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.graph.ui.gl.Shape;

/**
 * GPU based resolution independent test object
 * - FreeSans, '0'
 * - TTF Shape for Glyph 19
 */
public class Glyph05FreeSerifBoldItalic_ae extends Shape {

    public Glyph05FreeSerifBoldItalic_ae(final Factory<? extends Vertex> factory, final int renderModes) {
        super(factory, renderModes);
    }

    @Override
    protected void addShapeToRegion() {
        final OutlineShape shape = new OutlineShape(vertexFactory);

        // Start TTF Shape for Glyph 168
        // 000: B0a: move-to p0
        // Shape.MoveTo:
        shape.closeLastOutline(false);
        shape.addEmptyOutline();
        shape.addVertex(0, 0.450000f, -0.013000f, true);
        // 000: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.386000f, -0.013000f, false);
        shape.addVertex(0, 0.353000f, 0.018000f, true);
        // 002: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.319000f, 0.049000f, false);
        shape.addVertex(0, 0.307000f, 0.118000f, true);
        // 003: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.265000f, 0.049000f, false);
        shape.addVertex(0, 0.225000f, 0.019000f, true);
        // 005: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.184000f, -0.012000f, false);
        shape.addVertex(0, 0.134000f, -0.012000f, true);
        // 006: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.085000f, -0.012000f, false);
        shape.addVertex(0, 0.053000f, 0.021000f, true);
        // 008: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.020000f, 0.055000f, false);
        shape.addVertex(0, 0.020000f, 0.106000f, true);
        // 009: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.020000f, 0.185000f, false);
        shape.addVertex(0, 0.062000f, 0.269000f, true);
        // 011: B5: quad-to pMh-p0-p1h ***** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.105000f, 0.353000f, false);
        shape.addVertex(0, 0.170000f, 0.407000f, true);
        // 012: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.235000f, 0.462000f, false);
        shape.addVertex(0, 0.296000f, 0.462000f, true);
        // 013: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.328000f, 0.462000f, false);
        shape.addVertex(0, 0.346000f, 0.448000f, true);
        // 015: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.364000f, 0.433000f, false);
        shape.addVertex(0, 0.377000f, 0.396000f, true);
        // 016: B1: line-to p0-p1
        // Shape.LineTo:
        shape.addVertex(0, 0.395000f, 0.454000f, true);
        // 017: B1: line-to p0-p1
        // Shape.LineTo:
        shape.addVertex(0, 0.498000f, 0.459000f, true);
        // 018: B1: line-to p0-p1
        // Shape.LineTo:
        shape.addVertex(0, 0.478000f, 0.394000f, true);
        // 019: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.510000f, 0.431000f, false);
        shape.addVertex(0, 0.535000f, 0.445000f, true);
        // 021: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.561000f, 0.459000f, false);
        shape.addVertex(0, 0.598000f, 0.459000f, true);
        // 022: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.645000f, 0.459000f, false);
        shape.addVertex(0, 0.671000f, 0.436000f, true);
        // 024: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.698000f, 0.413000f, false);
        shape.addVertex(0, 0.698000f, 0.372000f, true);
        // 025: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.698000f, 0.310000f, false);
        shape.addVertex(0, 0.639000f, 0.263000f, true);
        // 027: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.579000f, 0.215000f, false);
        shape.addVertex(0, 0.470000f, 0.190000f, true);
        // 028: B1: line-to p0-p1
        // Shape.LineTo:
        shape.addVertex(0, 0.431000f, 0.181000f, true);
        // 029: B2: quad-to p0-p1-p2
        // Shape.QuadTo:
        shape.addVertex(0, 0.426000f, 0.156000f, false);
        shape.addVertex(0, 0.426000f, 0.134000f, true);
        // 031: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.426000f, 0.096000f, false);
        shape.addVertex(0, 0.444000f, 0.073000f, true);
        // 033: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.462000f, 0.050000f, false);
        shape.addVertex(0, 0.493000f, 0.050000f, true);
        // 034: B2: quad-to p0-p1-p2
        // Shape.QuadTo:
        shape.addVertex(0, 0.565000f, 0.050000f, false);
        shape.addVertex(0, 0.616000f, 0.139000f, true);
        // 036: B1: line-to p0-p1
        // Shape.LineTo:
        shape.addVertex(0, 0.644000f, 0.122000f, true);
        // 037: B2: quad-to p0-p1-p2
        // Shape.QuadTo:
        shape.addVertex(0, 0.578000f, -0.013000f, false);
        shape.addVertex(0, 0.450000f, -0.013000f, true);
        System.err.println("Glyph05FreeSerifBoldItalic_ae.shape01a.winding_area: "+shape.getWindingOfLastOutline());
        shape.closeLastOutline(false);

        // 039: B0a: move-to p0
        // Shape.MoveTo:
        shape.closeLastOutline(false);
        shape.addEmptyOutline();
        shape.addVertex(0, 0.194000f, 0.058000f, true);
        // 039: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.238000f, 0.058000f, false);
        shape.addVertex(0, 0.278000f, 0.122000f, true);
        // 041: B5: quad-to pMh-p0-p1h ***** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.319000f, 0.187000f, false);
        shape.addVertex(0, 0.338000f, 0.256000f, true);
        // 042: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.358000f, 0.326000f, false);
        shape.addVertex(0, 0.358000f, 0.363000f, true);
        // 043: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.358000f, 0.387000f, false);
        shape.addVertex(0, 0.345000f, 0.403000f, true);
        // 045: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.331000f, 0.419000f, false);
        shape.addVertex(0, 0.310000f, 0.419000f, true);
        // 046: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.267000f, 0.419000f, false);
        shape.addVertex(0, 0.227000f, 0.356000f, true);
        // 048: B5: quad-to pMh-p0-p1h ***** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.187000f, 0.293000f, false);
        shape.addVertex(0, 0.167000f, 0.225000f, true);
        // 049: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.146000f, 0.156000f, false);
        shape.addVertex(0, 0.146000f, 0.119000f, true);
        // 050: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.146000f, 0.092000f, false);
        shape.addVertex(0, 0.159000f, 0.075000f, true);
        // 052: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.172000f, 0.058000f, false);
        shape.addVertex(0, 0.194000f, 0.058000f, true);
        System.err.println("Glyph05FreeSerifBoldItalic_ae.shape02a.winding_area: "+shape.getWindingOfLastOutline());
        shape.closeLastOutline(false);

        if( true ) {
            // GlyphShape<168>: offset 0 of 8/61 points
            //  pM[060] P[443/231, on true, end true]
            //  p0[053] P[438/214, on true, end false]
            //  p1[054] P[498/223, on false, end false]
            //  p2[055] P[608/320, on false, end false]
            // 053: B0a: move-to p0
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();
            shape.addVertex(0, 0.438000f, 0.214000f, true);
            // 053: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.498000f, 0.223000f, false);
            shape.addVertex(0, 0.553000f, 0.271000f, true);
            // GlyphShape<168>: offset 2 of 8/61 points
            //  pM[054] P[498/223, on false, end false]
            //  p0[055] P[608/320, on false, end false]
            //  p1[056] P[608/388, on true, end false]
            //  p2[057] P[608/429, on false, end false]
            // 055: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.608000f, 0.320000f, false);
            shape.addVertex(0, 0.608000f, 0.388000f, true);
            // GlyphShape<168>: offset 3 of 8/61 points
            //  pM[055] P[608/320, on false, end false]
            //  p0[056] P[608/388, on true, end false]
            //  p1[057] P[608/429, on false, end false]
            //  p2[058] P[575/429, on true, end false]
            // 056: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.608000f, 0.429000f, false);
            shape.addVertex(0, 0.575000f, 0.429000f, true);
            // GlyphShape<168>: offset 5 of 8/61 points
            //  pM[057] P[608/429, on false, end false]
            //  p0[058] P[575/429, on true, end false]
            //  p1[059] P[502/429, on false, end false]
            //  p2[060] P[443/231, on true, end true]
            // 058: B2: quad-to p0-p1-p2
            // Shape.QuadTo:
            shape.addVertex(0, 0.502000f, 0.429000f, false);
            shape.addVertex(0, 0.443000f, 0.231000f, true);
            // GlyphShape<168>: offset 7 of 8/61 points
            //  pM[059] P[502/429, on false, end false]
            //  p0[060] P[443/231, on true, end true]
            //  p1[053] P[438/214, on true, end false]
            //  p2[054] P[498/223, on false, end false]
            // 060: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.438000f, 0.214000f, true);
            System.err.println("Glyph05FreeSerifBoldItalic_ae.shape03a.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);
        }

        // End Shape for Glyph 168

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
