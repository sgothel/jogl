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
package com.jogamp.graph.ui.layout;

import java.util.List;

import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Shape;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * GraphUI Stack {@link Group.Layout}.
 * <p>
 * A stack of {@link Shape}s
 * - size kept unscaled
 * - position depends on {@Link Padding} and {@link Margin}
 * - *cell* size can be set
 * </p>
 */
public class BoxLayout implements Group.Layout {
    private final float cellWidth, cellHeight;
    private final Margin margin;
    private final Padding padding;
    private final float borderThickness;

    public BoxLayout(final Padding padding) {
        this(0f, 0f, new Margin(), padding);
    }
    public BoxLayout(final float width, final float height, final Margin margin) {
        this(width, height, margin, new Padding());
    }
    public BoxLayout(final float width, final float height, final Margin margin, final Padding padding) {
        this.cellWidth = Math.max(0f, width);
        this.cellHeight = Math.max(0f, height);
        this.margin = margin;
        this.padding = padding;
        this.borderThickness = 0f;
    }

    public Padding getPadding() { return padding; }
    public Margin getMargin() { return margin; }

    @Override
    public void layout(final Group g, final AABBox box, final PMVMatrix pmv) {
        final boolean hasCellWidth = !FloatUtil.isZero(cellWidth);
        final boolean hasCellHeight = !FloatUtil.isZero(cellHeight);
        final List<Shape> shapes = g.getShapes();
        final AABBox sbox = new AABBox();
        for(int i=0; i < shapes.size(); ++i) {
            final Shape s = shapes.get(i);
            pmv.glPushMatrix();
            s.setTransform(pmv);
            s.getBounds().transformMv(pmv, sbox);
            pmv.glPopMatrix();

            // adjust size and position (centered)
            final float paddedWidth = sbox.getWidth() + padding.width();
            final float paddedHeight = sbox.getHeight() + padding.height();
            final float marginedWidth = paddedWidth + margin.width();
            final float marginedHeight = paddedHeight + margin.height();
            final float cellWidth2 = hasCellWidth ? cellWidth : marginedWidth;
            final float cellHeight2 = hasCellHeight ? cellHeight : marginedHeight;
            final float x, y;
            if( margin.isCenteredHoriz() || hasCellWidth && sbox.getWidth() + padding.width() + margin.width() > cellWidth2 ) {
                x = 0;
            } else {
                x = margin.left + padding.left;
            }
            if( margin.isCenteredVert() || hasCellHeight && sbox.getHeight() + padding.height() + margin.height() > cellHeight2 ) {
                y = 0;
            } else {
                y = margin.bottom + padding.bottom;
            }
            float dxh = 0, dyh = 0;
            if( margin.isCenteredHoriz() ) {
                dxh += 0.5f * ( cellWidth2 - paddedWidth ); // actual horiz-centered
            }
            if( margin.isCenteredVert() ) {
                dyh += 0.5f * ( cellHeight2 - paddedHeight ); // actual vert-centered
            }
            System.err.println("["+i+"].m: "+x+" / "+y+" + "+dxh+" / "+dyh+", p "+paddedWidth+" x "+paddedHeight+", sz "+cellWidth2+" x "+cellHeight2+", box "+box.getWidth()+" x "+box.getHeight());
            s.moveTo( x + dxh, y + dyh, 0f ); // center the scaled artifact
            s.move( sbox.getLow().mul(-1f) ); // remove the bottom-left delta
            box.resize( x + cellWidth2 + padding.right, y + cellHeight2 + padding.top,    0);
            box.resize( x              - padding.left,  y -               padding.bottom, 0);
            System.err.println("["+i+"].x: "+x+" / "+y+" + "+dxh+" / "+dyh+" -> "+s.getPosition()+", p "+paddedWidth+" x "+paddedHeight+", sz "+cellWidth2+" x "+cellHeight2+", box "+box.getWidth()+" x "+box.getHeight());
        }
    }

    @Override
    public String toString() {
        return "Box[cell["+cellWidth+" x "+cellHeight+"], "+margin+", "+padding+"]";
    }
}

