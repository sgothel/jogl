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
import com.jogamp.graph.geom.plane.Winding;
import com.jogamp.graph.ui.gl.Shape;

/**
 * GPU based resolution independent test object
 * - Ubuntu-Light, lower case 'o'
 * - TTF Shape for Glyph 82
 */
public class Glyph01UbuntuLight_o extends Shape {

    public Glyph01UbuntuLight_o(final Factory<? extends Vertex> factory, final int renderModes) {
        super(factory, renderModes);
    }

    @SuppressWarnings("unused")
    @Override
    protected void addShapeToRegion() {
        final OutlineShape shape = new OutlineShape(vertexFactory);

        // Ubuntu-Light, lower case 'o'
        // Start TTF Shape for Glyph 82
        if( false ) {
            // Original Outer shape: Winding.CW
            // Moved into OutlineShape reverse -> Winding.CCW -> OK
            //
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();
            shape.addVertex(0, 0.527000f, 0.258000f, true);
            // 000: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.527000f, 0.197000f, false);
            shape.addVertex(0, 0.510000f, 0.147000f, true);
            // 002: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.492000f, 0.097000f, false);
            shape.addVertex(0, 0.461000f, 0.062000f, true);
            // 003: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.429000f, 0.027000f, false);
            shape.addVertex(0, 0.386000f, 0.008000f, true);
            // 004: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.343000f, -0.012000f, false);
            shape.addVertex(0, 0.291000f, -0.012000f, true);
            // 005: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.239000f, -0.012000f, false);
            shape.addVertex(0, 0.196000f, 0.007000f, true);
            // 007: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.153000f, 0.027000f, false);
            shape.addVertex(0, 0.122000f, 0.062000f, true);
            // 008: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.090000f, 0.097000f, false);
            shape.addVertex(0, 0.073000f, 0.147000f, true);
            // 009: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.055000f, 0.197000f, false);
            shape.addVertex(0, 0.055000f, 0.258000f, true);
            // 010: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.055000f, 0.319000f, false);
            shape.addVertex(0, 0.072000f, 0.369000f, true);
            // 012: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.090000f, 0.419000f, false);
            shape.addVertex(0, 0.121000f, 0.454000f, true);
            // 013: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.153000f, 0.490000f, false);
            shape.addVertex(0, 0.196000f, 0.509000f, true);
            // 014: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.239000f, 0.529000f, false);
            shape.addVertex(0, 0.291000f, 0.529000f, true);
            // 015: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.343000f, 0.529000f, false);
            shape.addVertex(0, 0.386000f, 0.510000f, true);
            // 017: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.429000f, 0.490000f, false);
            shape.addVertex(0, 0.460000f, 0.455000f, true);
            // 018: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.492000f, 0.419000f, false);
            shape.addVertex(0, 0.509000f, 0.369000f, true);
            // 019: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.527000f, 0.319000f, false);
            shape.addVertex(0, 0.527000f, 0.258000f, true);
            System.err.println("Glyph01UbuntuLight_o.shape01a.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);
        } else  {
            // Outer shape: Winding.CW
            // Moved into OutlineShape same-order -> Winding.CW -> ERROR (so we fix it in the end, see below)
            //
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();
            shape.addVertex(0.527000f, 0.258000f, true);
            // 000: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.527000f, 0.197000f, false);
            shape.addVertex(0.510000f, 0.147000f, true);
            // 002: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.492000f, 0.097000f, false);
            shape.addVertex(0.461000f, 0.062000f, true);
            // 003: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.429000f, 0.027000f, false);
            shape.addVertex(0.386000f, 0.008000f, true);
            // 004: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.343000f, -0.012000f, false);
            shape.addVertex(0.291000f, -0.012000f, true);
            // 005: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.239000f, -0.012000f, false);
            shape.addVertex(0.196000f, 0.007000f, true);
            // 007: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.153000f, 0.027000f, false);
            shape.addVertex(0.122000f, 0.062000f, true);
            // 008: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.090000f, 0.097000f, false);
            shape.addVertex(0.073000f, 0.147000f, true);
            // 009: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.055000f, 0.197000f, false);
            shape.addVertex(0.055000f, 0.258000f, true);
            // 010: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.055000f, 0.319000f, false);
            shape.addVertex(0.072000f, 0.369000f, true);
            // 012: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.090000f, 0.419000f, false);
            shape.addVertex(0.121000f, 0.454000f, true);
            // 013: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.153000f, 0.490000f, false);
            shape.addVertex(0.196000f, 0.509000f, true);
            // 014: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.239000f, 0.529000f, false);
            shape.addVertex(0.291000f, 0.529000f, true);
            // 015: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.343000f, 0.529000f, false);
            shape.addVertex(0.386000f, 0.510000f, true);
            // 017: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.429000f, 0.490000f, false);
            shape.addVertex(0.460000f, 0.455000f, true);
            // 018: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.492000f, 0.419000f, false);
            shape.addVertex(0.509000f, 0.369000f, true);
            // 019: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.527000f, 0.319000f, false);
            shape.addVertex(0.527000f, 0.258000f, true);
            System.err.println("Glyph01UbuntuLight_o.shape01b.1.winding_area: "+shape.getWindingOfLastOutline());
            shape.setWindingOfLastOutline(Winding.CCW);
            System.err.println("Glyph01UbuntuLight_o.shape01b.2.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);
        }

        if( true ) {
            // Original Inner shape: Winding.CCW
            // Moved into OutlineShape reverse -> Winding.CW -> OK
            //
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();
            shape.addVertex(0, 0.458000f, 0.258000f, true);
            // 020: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.458000f, 0.355000f, false);
            shape.addVertex(0, 0.413000f, 0.412000f, true);
            // 022: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.368000f, 0.470000f, false);
            shape.addVertex(0, 0.291000f, 0.470000f, true);
            // 023: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.214000f, 0.470000f, false);
            shape.addVertex(0, 0.169000f, 0.413000f, true);
            // 025: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.124000f, 0.355000f, false);
            shape.addVertex(0, 0.124000f, 0.258000f, true);
            // 026: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.124000f, 0.161000f, false);
            shape.addVertex(0, 0.169000f, 0.104000f, true);
            // 028: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.214000f, 0.047000f, false);
            shape.addVertex(0, 0.291000f, 0.047000f, true);
            // 029: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.368000f, 0.047000f, false);
            shape.addVertex(0, 0.413000f, 0.104000f, true);
            // 031: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.458000f, 0.161000f, false);
            shape.addVertex(0, 0.458000f, 0.258000f, true);
            System.err.println("Glyph01UbuntuLight_o.shape02a.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);
        } else {
            // Inner shape: Winding.CCW
            // Moved into OutlineShape same-order -> Winding.CCW -> OK
            //
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();

            shape.addVertex(0.458000f, 0.258000f, true);
            // 020: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.458000f, 0.355000f, false);
            shape.addVertex(0.413000f, 0.412000f, true);
            // 022: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.368000f, 0.470000f, false);
            shape.addVertex(0.291000f, 0.470000f, true);
            // 023: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.214000f, 0.470000f, false);
            shape.addVertex(0.169000f, 0.413000f, true);
            // 025: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.124000f, 0.355000f, false);
            shape.addVertex(0.124000f, 0.258000f, true);
            // 026: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.124000f, 0.161000f, false);
            shape.addVertex(0.169000f, 0.104000f, true);
            // 028: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.214000f, 0.047000f, false);
            shape.addVertex(0.291000f, 0.047000f, true);
            // 029: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.368000f, 0.047000f, false);
            shape.addVertex(0.413000f, 0.104000f, true);
            // 031: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.458000f, 0.161000f, false);
            shape.addVertex(0.458000f, 0.258000f, true);

            System.err.println("Glyph01UbuntuLight_o.shape02b.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);
        }
        // End Shape for Glyph 82

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
