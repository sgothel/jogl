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
package com.jogamp.opengl.test.junit.graph.demos.ui;

import com.jogamp.opengl.GL2ES2;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

/**
 * GPU based resolution independent test object
 */
public class TestObject02 extends UIShape {

    public TestObject02(final Factory<? extends Vertex> factory, final int renderModes) {
        super(factory, renderModes);
    }

    @Override
    protected void clearImpl(final GL2ES2 gl, final RegionRenderer renderer) {
    }

    @Override
    protected void destroyImpl(final GL2ES2 gl, final RegionRenderer renderer) {
    }

    @SuppressWarnings("unused")
    @Override
    protected void addShapeToRegion(final GL2ES2 gl, final RegionRenderer renderer) {
        final OutlineShape shape = new OutlineShape(renderer.getRenderState().getVertexFactory());

        // lower case 'æ'

        // Start TTF Shape for Glyph 193
        if( true ) {
            // Original Inner e-shape: Winding.CCW
            // Moved into OutlineShape reverse -> Winding.CW -> OK
            //
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();
            shape.addVertex(0, 0.728000f, 0.300000f, true);
            // 000: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.726000f, 0.381000f, false);
            shape.addVertex(0, 0.690000f, 0.426000f, true);
            // 002: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.654000f, 0.471000f, false);
            shape.addVertex(0, 0.588000f, 0.471000f, true);
            // 003: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.553000f, 0.471000f, false);
            shape.addVertex(0, 0.526000f, 0.457000f, true);
            // 005: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.498000f, 0.443000f, false);
            shape.addVertex(0, 0.478000f, 0.420000f, true);
            // 006: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.457000f, 0.396000f, false);
            shape.addVertex(0, 0.446000f, 0.365000f, true);
            // 007: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.434000f, 0.334000f, false);
            shape.addVertex(0, 0.432000f, 0.300000f, true);
            // 008: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.728000f, 0.300000f, true);
            System.err.println("TestObject02.shape01a.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);
        } else {
            // Inner e-shape: Winding.CCW
            // Moved into OutlineShape same-order -> Winding.CCW -> ??
            //
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();
            shape.addVertex(0.728000f, 0.300000f, true);
            // 000: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.726000f, 0.381000f, false);
            shape.addVertex(0.690000f, 0.426000f, true);
            // 002: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.654000f, 0.471000f, false);
            shape.addVertex(0.588000f, 0.471000f, true);
            // 003: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.553000f, 0.471000f, false);
            shape.addVertex(0.526000f, 0.457000f, true);
            // 005: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.498000f, 0.443000f, false);
            shape.addVertex(0.478000f, 0.420000f, true);
            // 006: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.457000f, 0.396000f, false);
            shape.addVertex(0.446000f, 0.365000f, true);
            // 007: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.434000f, 0.334000f, false);
            shape.addVertex(0.432000f, 0.300000f, true);
            // 008: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0.728000f, 0.300000f, true);
            System.err.println("TestObject02.shape01b.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);
        }

        if( true ) {
            // Original Outer shape: Winding.CW
            // Moved into OutlineShape reverse -> Winding.CCW -> OK
            //
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();
            shape.addVertex(0, 0.252000f, -0.011000f, true);
            // 009: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.208000f, -0.011000f, false);
            shape.addVertex(0, 0.171000f, -0.002000f, true);
            // 011: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.133000f, 0.007000f, false);
            shape.addVertex(0, 0.106000f, 0.026000f, true);
            // 012: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.079000f, 0.046000f, false);
            shape.addVertex(0, 0.064000f, 0.076000f, true);
            // 013: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.048000f, 0.107000f, false);
            shape.addVertex(0, 0.048000f, 0.151000f, true);
            // 014: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.048000f, 0.193000f, false);
            shape.addVertex(0, 0.064000f, 0.223000f, true);
            // 016: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.080000f, 0.253000f, false);
            shape.addVertex(0, 0.109000f, 0.272000f, true);
            // 017: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.138000f, 0.292000f, false);
            shape.addVertex(0, 0.178000f, 0.301000f, true);
            // 018: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.218000f, 0.310000f, false);
            shape.addVertex(0, 0.265000f, 0.310000f, true);
            // 019: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.279000f, 0.310000f, false);
            shape.addVertex(0, 0.294000f, 0.309000f, true);
            // 021: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.310000f, 0.307000f, false);
            shape.addVertex(0, 0.324000f, 0.305000f, true);
            // 022: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.339000f, 0.302000f, false);
            shape.addVertex(0, 0.349000f, 0.300000f, true);
            // 023: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.360000f, 0.297000f, false);
            shape.addVertex(0, 0.364000f, 0.295000f, true);
            // 024: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.364000f, 0.327000f, true);
            // 025: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.364000f, 0.354000f, false);
            shape.addVertex(0, 0.360000f, 0.379000f, true);
            // 027: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.356000f, 0.405000f, false);
            shape.addVertex(0, 0.343000f, 0.425000f, true);
            // 028: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.329000f, 0.446000f, false);
            shape.addVertex(0, 0.305000f, 0.458000f, true);
            // 029: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.280000f, 0.471000f, false);
            shape.addVertex(0, 0.240000f, 0.471000f, true);
            // 030: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.186000f, 0.471000f, false);
            shape.addVertex(0, 0.156000f, 0.464000f, true);
            // 032: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.126000f, 0.456000f, false);
            shape.addVertex(0, 0.113000f, 0.451000f, true);
            // 033: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.105000f, 0.507000f, true);
            // 034: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.122000f, 0.515000f, false);
            shape.addVertex(0, 0.158000f, 0.522000f, true);
            // 036: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.194000f, 0.529000f, false);
            shape.addVertex(0, 0.243000f, 0.529000f, true);
            // 037: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.314000f, 0.529000f, false);
            shape.addVertex(0, 0.354000f, 0.503000f, true);
            // 039: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.395000f, 0.476000f, false);
            shape.addVertex(0, 0.412000f, 0.431000f, true);
            // 040: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.445000f, 0.480000f, false);
            shape.addVertex(0, 0.491000f, 0.504000f, true);
            // 042: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.537000f, 0.529000f, false);
            shape.addVertex(0, 0.587000f, 0.529000f, true);
            // 043: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.682000f, 0.529000f, false);
            shape.addVertex(0, 0.738000f, 0.467000f, true);
            // 045: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.795000f, 0.405000f, false);
            shape.addVertex(0, 0.795000f, 0.276000f, true);
            // 046: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.795000f, 0.268000f, false);
            shape.addVertex(0, 0.795000f, 0.260000f, true);
            // 048: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.794000f, 0.252000f, false);
            shape.addVertex(0, 0.793000f, 0.245000f, true);
            // 049: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.430000f, 0.245000f, true);
            // 050: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.433000f, 0.150000f, false);
            shape.addVertex(0, 0.477000f, 0.099000f, true);
            // 052: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.521000f, 0.048000f, false);
            shape.addVertex(0, 0.617000f, 0.048000f, true);
            // 053: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.670000f, 0.048000f, false);
            shape.addVertex(0, 0.701000f, 0.058000f, true);
            // 055: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.732000f, 0.068000f, false);
            shape.addVertex(0, 0.746000f, 0.075000f, true);
            // 056: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0, 0.758000f, 0.019000f, true);
            // 057: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.744000f, 0.011000f, false);
            shape.addVertex(0, 0.706000f, 0.000000f, true);
            // 059: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.667000f, -0.011000f, false);
            shape.addVertex(0, 0.615000f, -0.011000f, true);
            // 060: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.558000f, -0.011000f, false);
            shape.addVertex(0, 0.514000f, 0.003000f, true);
            // 062: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.470000f, 0.017000f, false);
            shape.addVertex(0, 0.437000f, 0.049000f, true);
            // 063: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.426000f, 0.040000f, false);
            shape.addVertex(0, 0.410000f, 0.030000f, true);
            // 065: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.393000f, 0.019000f, false);
            shape.addVertex(0, 0.370000f, 0.010000f, true);
            // 066: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.347000f, 0.001000f, false);
            shape.addVertex(0, 0.318000f, -0.005000f, true);
            // 067: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.289000f, -0.011000f, false);
            shape.addVertex(0, 0.252000f, -0.011000f, true);
            System.err.println("TestObject02.shape02a.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);
        } else {
            // Outer shape: Winding.CW
            // Moved into OutlineShape same-order -> Winding.CW -> OK now
            //
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();
            shape.addVertex(0.252000f, -0.011000f, true);
            // 009: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.208000f, -0.011000f, false);
            shape.addVertex(0.171000f, -0.002000f, true);
            // 011: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.133000f, 0.007000f, false);
            shape.addVertex(0.106000f, 0.026000f, true);
            // 012: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.079000f, 0.046000f, false);
            shape.addVertex(0.064000f, 0.076000f, true);
            // 013: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.048000f, 0.107000f, false);
            shape.addVertex(0.048000f, 0.151000f, true);
            // 014: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.048000f, 0.193000f, false);
            shape.addVertex(0.064000f, 0.223000f, true);
            // 016: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.080000f, 0.253000f, false);
            shape.addVertex(0.109000f, 0.272000f, true);
            // 017: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.138000f, 0.292000f, false);
            shape.addVertex(0.178000f, 0.301000f, true);
            // 018: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.218000f, 0.310000f, false);
            shape.addVertex(0.265000f, 0.310000f, true);
            // 019: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.279000f, 0.310000f, false);
            shape.addVertex(0.294000f, 0.309000f, true);
            // 021: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.310000f, 0.307000f, false);
            shape.addVertex(0.324000f, 0.305000f, true);
            // 022: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.339000f, 0.302000f, false);
            shape.addVertex(0.349000f, 0.300000f, true);
            // 023: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.360000f, 0.297000f, false);
            shape.addVertex(0.364000f, 0.295000f, true);
            // 024: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0.364000f, 0.327000f, true);
            // 025: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.364000f, 0.354000f, false);
            shape.addVertex(0.360000f, 0.379000f, true);
            // 027: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.356000f, 0.405000f, false);
            shape.addVertex(0.343000f, 0.425000f, true);
            // 028: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.329000f, 0.446000f, false);
            shape.addVertex(0.305000f, 0.458000f, true);
            // 029: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.280000f, 0.471000f, false);
            shape.addVertex(0.240000f, 0.471000f, true);
            // 030: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.186000f, 0.471000f, false);
            shape.addVertex(0.156000f, 0.464000f, true);
            // 032: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.126000f, 0.456000f, false);
            shape.addVertex(0.113000f, 0.451000f, true);
            // 033: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0.105000f, 0.507000f, true);
            // 034: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.122000f, 0.515000f, false);
            shape.addVertex(0.158000f, 0.522000f, true);
            // 036: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.194000f, 0.529000f, false);
            shape.addVertex(0.243000f, 0.529000f, true);
            // 037: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.314000f, 0.529000f, false);
            shape.addVertex(0.354000f, 0.503000f, true);
            // 039: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.395000f, 0.476000f, false);
            shape.addVertex(0.412000f, 0.431000f, true);
            // 040: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.445000f, 0.480000f, false);
            shape.addVertex(0.491000f, 0.504000f, true);
            // 042: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.537000f, 0.529000f, false);
            shape.addVertex(0.587000f, 0.529000f, true);
            // 043: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.682000f, 0.529000f, false);
            shape.addVertex(0.738000f, 0.467000f, true);
            // 045: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.795000f, 0.405000f, false);
            shape.addVertex(0.795000f, 0.276000f, true);
            // 046: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.795000f, 0.268000f, false);
            shape.addVertex(0.795000f, 0.260000f, true);
            // 048: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.794000f, 0.252000f, false);
            shape.addVertex(0.793000f, 0.245000f, true);
            // 049: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0.430000f, 0.245000f, true);
            // 050: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.433000f, 0.150000f, false);
            shape.addVertex(0.477000f, 0.099000f, true);
            // 052: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.521000f, 0.048000f, false);
            shape.addVertex(0.617000f, 0.048000f, true);
            // 053: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.670000f, 0.048000f, false);
            shape.addVertex(0.701000f, 0.058000f, true);
            // 055: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.732000f, 0.068000f, false);
            shape.addVertex(0.746000f, 0.075000f, true);
            // 056: B1: line-to p0-p1
            // Shape.LineTo:
            shape.addVertex(0.758000f, 0.019000f, true);
            // 057: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.744000f, 0.011000f, false);
            shape.addVertex(0.706000f, 0.000000f, true);
            // 059: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.667000f, -0.011000f, false);
            shape.addVertex(0.615000f, -0.011000f, true);
            // 060: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.558000f, -0.011000f, false);
            shape.addVertex(0.514000f, 0.003000f, true);
            // 062: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.470000f, 0.017000f, false);
            shape.addVertex(0.437000f, 0.049000f, true);
            // 063: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.426000f, 0.040000f, false);
            shape.addVertex(0.410000f, 0.030000f, true);
            // 065: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.393000f, 0.019000f, false);
            shape.addVertex(0.370000f, 0.010000f, true);
            // 066: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.347000f, 0.001000f, false);
            shape.addVertex(0.318000f, -0.005000f, true);
            // 067: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.289000f, -0.011000f, false);
            shape.addVertex(0.252000f, -0.011000f, true);
            System.err.println("TestObject02.shape02b.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);
        }

        if( true ) {
            // Original Inner a-shape: Winding.CCW
            // Moved into OutlineShape reverse -> Winding.CW -> OK now
            //
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();
            shape.addVertex(0, 0.365000f, 0.238000f, true);
            // 068: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.354000f, 0.243000f, false);
            shape.addVertex(0, 0.330000f, 0.248000f, true);
            // 070: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.305000f, 0.254000f, false);
            shape.addVertex(0, 0.263000f, 0.254000f, true);
            // 071: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.239000f, 0.254000f, false);
            shape.addVertex(0, 0.213000f, 0.251000f, true);
            // 073: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.187000f, 0.247000f, false);
            shape.addVertex(0, 0.165000f, 0.236000f, true);
            // 074: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.143000f, 0.224000f, false);
            shape.addVertex(0, 0.129000f, 0.204000f, true);
            // 075: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.115000f, 0.184000f, false);
            shape.addVertex(0, 0.115000f, 0.151000f, true);
            // 076: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.115000f, 0.122000f, false);
            shape.addVertex(0, 0.125000f, 0.102000f, true);
            // 078: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.135000f, 0.082000f, false);
            shape.addVertex(0, 0.153000f, 0.070000f, true);
            // 079: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.172000f, 0.058000f, false);
            shape.addVertex(0, 0.197000f, 0.053000f, true);
            // 080: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.222000f, 0.047000f, false);
            shape.addVertex(0, 0.252000f, 0.047000f, true);
            // 081: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.314000f, 0.047000f, false);
            shape.addVertex(0, 0.350000f, 0.063000f, true);
            // 083: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.386000f, 0.080000f, false);
            shape.addVertex(0, 0.400000f, 0.093000f, true);
            // 084: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0, 0.384000f, 0.119000f, false);
            shape.addVertex(0, 0.375000f, 0.154000f, true);
            // 086: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0, 0.366000f, 0.190000f, false);
            shape.addVertex(0, 0.365000f, 0.238000f, true);
            System.err.println("TestObject02.shape03a.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);
        } else {
            // Inner a-shape: Winding.CCW
            // Moved into OutlineShape same-order -> Winding.CCW -> OK
            //
            // Shape.MoveTo:
            shape.closeLastOutline(false);
            shape.addEmptyOutline();
            shape.addVertex(0.365000f, 0.238000f, true);
            // 068: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.354000f, 0.243000f, false);
            shape.addVertex(0.330000f, 0.248000f, true);
            // 070: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.305000f, 0.254000f, false);
            shape.addVertex(0.263000f, 0.254000f, true);
            // 071: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.239000f, 0.254000f, false);
            shape.addVertex(0.213000f, 0.251000f, true);
            // 073: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.187000f, 0.247000f, false);
            shape.addVertex(0.165000f, 0.236000f, true);
            // 074: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.143000f, 0.224000f, false);
            shape.addVertex(0.129000f, 0.204000f, true);
            // 075: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.115000f, 0.184000f, false);
            shape.addVertex(0.115000f, 0.151000f, true);
            // 076: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.115000f, 0.122000f, false);
            shape.addVertex(0.125000f, 0.102000f, true);
            // 078: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.135000f, 0.082000f, false);
            shape.addVertex(0.153000f, 0.070000f, true);
            // 079: B5: quad-to pMh-p0-p1h ***** MID
            // Shape.QuadTo:
            shape.addVertex(0.172000f, 0.058000f, false);
            shape.addVertex(0.197000f, 0.053000f, true);
            // 080: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.222000f, 0.047000f, false);
            shape.addVertex(0.252000f, 0.047000f, true);
            // 081: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.314000f, 0.047000f, false);
            shape.addVertex(0.350000f, 0.063000f, true);
            // 083: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.386000f, 0.080000f, false);
            shape.addVertex(0.400000f, 0.093000f, true);
            // 084: B4: quad-to p0-p1-p2h **** MID
            // Shape.QuadTo:
            shape.addVertex(0.384000f, 0.119000f, false);
            shape.addVertex(0.375000f, 0.154000f, true);
            // 086: B6: quad-to pMh-p0-p1
            // Shape.QuadTo:
            shape.addVertex(0.366000f, 0.190000f, false);
            shape.addVertex(0.365000f, 0.238000f, true);
            System.err.println("TestObject02.shape03b.winding_area: "+shape.getWindingOfLastOutline());
            shape.closeLastOutline(false);
        }
        // End Shape for Glyph 193

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
