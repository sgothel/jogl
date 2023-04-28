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
import com.jogamp.graph.ui.GraphShape;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;

/**
 * GPU based resolution independent test object
 * - FreeSans, '0'
 * - TTF Shape for Glyph 19
 */
public class Glyph04FreeSans_0 extends GraphShape {

    public Glyph04FreeSans_0(final int renderModes) {
        super(renderModes);
    }

    @Override
    protected void addShapeToRegion(final GLProfile glp, final GL2ES2 gl) {
        final OutlineShape shape = new OutlineShape();

        // Start TTF Shape for Glyph 19
        // 000: B0a: move-to p0
        // Shape.MoveTo:
        shape.closeLastOutline(false);
        shape.addEmptyOutline();
        shape.addVertex(0, 0.043000f, 0.343000f, true);
        // 000: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.043000f, 0.432000f, false);
        shape.addVertex(0, 0.058000f, 0.500000f, true);
        // 002: B5: quad-to pMh-p0-p1h ***** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.073000f, 0.568000f, false);
        shape.addVertex(0, 0.096000f, 0.606000f, true);
        // 003: B5: quad-to pMh-p0-p1h ***** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.119000f, 0.645000f, false);
        shape.addVertex(0, 0.151000f, 0.669000f, true);
        // 004: B5: quad-to pMh-p0-p1h ***** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.183000f, 0.693000f, false);
        shape.addVertex(0, 0.212000f, 0.701000f, true);
        // 005: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.242000f, 0.709000f, false);
        shape.addVertex(0, 0.275000f, 0.709000f, true);
        // 006: B2: quad-to p0-p1-p2
        // Shape.QuadTo:
        shape.addVertex(0, 0.507000f, 0.709000f, false);
        shape.addVertex(0, 0.507000f, 0.337000f, true);
        // 008: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.507000f, 0.162000f, false);
        shape.addVertex(0, 0.448000f, 0.070000f, true);
        // 010: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.388000f, -0.023000f, false);
        shape.addVertex(0, 0.275000f, -0.023000f, true);
        // 011: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.161000f, -0.023000f, false);
        shape.addVertex(0, 0.102000f, 0.070000f, true);
        // 013: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.043000f, 0.164000f, false);
        shape.addVertex(0, 0.043000f, 0.343000f, true);
        System.err.println("Glyph04FreeSans_0.shape01a.winding_area: "+shape.getWindingOfLastOutline());
        shape.closeLastOutline(false);

        // 021: B0b: move-to pM
        // Shape.MoveTo:
        shape.closeLastOutline(false);
        shape.addEmptyOutline();
        shape.addVertex(0, 0.417000f, 0.345000f, true);
        // 021: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.417000f, 0.631000f, false);
        shape.addVertex(0, 0.275000f, 0.631000f, true);
        // 015: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.133000f, 0.631000f, false);
        shape.addVertex(0, 0.133000f, 0.342000f, true);
        // 016: B2: quad-to p0-p1-p2
        // Shape.QuadTo:
        shape.addVertex(0, 0.133000f, 0.050000f, false);
        shape.addVertex(0, 0.273000f, 0.050000f, true);
        // 018: B4: quad-to p0-p1-p2h **** MID
        // Shape.QuadTo:
        shape.addVertex(0, 0.347000f, 0.050000f, false);
        shape.addVertex(0, 0.382000f, 0.122000f, true);
        // 020: B6: quad-to pMh-p0-p1
        // Shape.QuadTo:
        shape.addVertex(0, 0.417000f, 0.194000f, false);
        shape.addVertex(0, 0.417000f, 0.345000f, true);
        System.err.println("Glyph04FreeSans_0.shape02a.winding_area: "+shape.getWindingOfLastOutline());
        shape.closeLastOutline(false);

        // End Shape for Glyph 19

        shape.setIsQuadraticNurbs();
        shape.setSharpness(oshapeSharpness);

        resetGLRegion(glp, gl, null, shape);
        region.addOutlineShape(shape, null, rgbaColor);
        box.resize(shape.getBounds());
        setRotationPivot( box.getCenter() );
    }

    @Override
    public String getSubString() {
        return super.getSubString();
    }
}
