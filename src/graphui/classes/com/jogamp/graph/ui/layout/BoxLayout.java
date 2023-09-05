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
import com.jogamp.opengl.math.Vec2f;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * GraphUI Stack {@link Group.Layout}.
 * <p>
 * A stack of {@link Shape}s
 * <ul>
 *   <li>Optionally centered {@link Alignment.Bit#CenterHoriz horizontally}, {@link Alignment.Bit#CenterVert vertically} or {@link Alignment#Center both}.</li>
 *   <li>Optionally scaled to cell-size if given and {@link Alignment#Fill}</li>
 *   <li>Padding is applied to each {@Shape} via {@link Shape#setPaddding(Padding)} if passed in constructor</li>
 *   <li>Margin is ignored on dimension with center {@link Alignment}</li>
 *   <li>Not implemented {@link Alignment}: {@link Alignment.Bit#Top Top}, {@link Alignment.Bit#Right Right}, {@link Alignment.Bit#Bottom Bottom}, {@link Alignment.Bit#Left Left}</li>
 * </ul>
 * </p>
 */
public class BoxLayout implements Group.Layout {
    private final Vec2f cellSize;
    private final Alignment alignment;
    private final Margin margin;
    private final Padding padding;

    private static final boolean TRACE_LAYOUT = false;


    /**
     */
    public BoxLayout() {
        this(0f, 0f, new Alignment(), new Margin(), null);
    }

    /**
     *
     * @param padding {@link Padding} applied to each {@Shape} via {@link Shape#setPaddding(Padding)}
     */
    public BoxLayout(final Padding padding) {
        this(0f, 0f, new Alignment(), new Margin(), padding);
    }

    public BoxLayout(final float width, final float height) {
        this(width, height, new Alignment(), new Margin(), null);
    }

    /**
     *
     * @param width
     * @param height
     * @param alignment
     */
    public BoxLayout(final float width, final float height, final Alignment alignment) {
        this(width, height, alignment, new Margin(), null);
    }

    /**
     *
     * @param width
     * @param height
     * @param margin
     */
    public BoxLayout(final float width, final float height, final Margin margin) {
        this(width, height, new Alignment(), margin, null);
    }

    /**
     *
     * @param width
     * @param height
     * @param padding {@link Padding} applied to each {@Shape} via {@link Shape#setPaddding(Padding)}
     */
    public BoxLayout(final float width, final float height, final Padding padding) {
        this(width, height, new Alignment(), new Margin(), padding);
    }

    /**
     *
     * @param width
     * @param height
     * @param margin
     * @param padding {@link Padding} applied to each {@Shape} via {@link Shape#setPaddding(Padding)}
     */
    public BoxLayout(final float width, final float height, final Margin margin, final Padding padding) {
        this(width, height, new Alignment(), margin, padding);
    }

    /**
     *
     * @param width
     * @param height
     * @param margin
     */
    public BoxLayout(final float width, final float height, final Alignment alignment, final Margin margin) {
        this(width, height, alignment, margin, null);
    }

    /**
     *
     * @param width
     * @param height
     * @param alignment
     * @param margin
     * @param padding {@link Padding} applied to each {@Shape} via {@link Shape#setPaddding(Padding)}
     */
    public BoxLayout(final float width, final float height, final Alignment alignment, final Margin margin, final Padding padding) {
        this.cellSize = new Vec2f(Math.max(0f, width), Math.max(0f, height));
        this.alignment = alignment;
        this.margin = margin;
        this.padding = padding;
    }

    /** Returns the preset cell size */
    public Vec2f getCellSize() { return cellSize; }
    /** Returns given {@link Alignment}. */
    public Alignment getAlignment() { return alignment; }
    /** Returns given {@link Margin}. */
    public Margin getMargin() { return margin; }
    /** Returns given {@link Padding}, may be {@code null} if not given via constructor. */
    public Padding getPadding() { return padding; }

    @Override
    public void preValidate(final Shape s) {
        if( null != padding ) {
            s.setPaddding(padding);
        }
    }

    @Override
    public void layout(final Group g, final AABBox box, final PMVMatrix pmv) {
        final Vec3f zeroVec3 = new Vec3f();
        final boolean hasCellWidth = !FloatUtil.isZero(cellSize.x());
        final boolean hasCellHeight = !FloatUtil.isZero(cellSize.y());
        final boolean isCenteredHoriz = hasCellWidth && alignment.isSet(Alignment.Bit.CenterHoriz);
        final boolean isCenteredVert = hasCellHeight && alignment.isSet(Alignment.Bit.CenterVert);
        final boolean isScaled = alignment.isSet(Alignment.Bit.Fill) && ( hasCellWidth || hasCellHeight );

        final List<Shape> shapes = g.getShapes();
        final AABBox sbox = new AABBox();
        for(int i=0; i < shapes.size(); ++i) {
            final Shape s = shapes.get(i);

            // measure size
            pmv.glPushMatrix();
            s.setTransform(pmv);
            s.getBounds().transformMv(pmv, sbox);
            pmv.glPopMatrix();

            final int x = 0, y = 0;
            if( TRACE_LAYOUT ) {
                System.err.println("bl("+i+").0: sbox "+sbox+", s "+s);
            }

            // IF isScaled: Uniform scale w/ lowest axis scale and center position on lower-scale axis
            final float shapeWidthU     = sbox.getWidth() + margin.width();
            final float shapeWidthU_LH  = ( sbox.getWidth()  * 0.5f ) + margin.left;   // left-half
            final float shapeHeightU    = sbox.getHeight() + margin.height();
            final float shapeHeightU_BH = ( sbox.getHeight() * 0.5f ) + margin.bottom; // bottom-half
            final float sxy;
            float dxh = 0, dyh = 0;
            if( isScaled ) {
                // scaling to cell size, implies center (horiz + vert)
                final float cellWidth2 = hasCellWidth ? cellSize.x() : shapeWidthU;
                final float cellHeight2 = hasCellHeight ? cellSize.y() : shapeHeightU;
                final float sx = cellWidth2 / shapeWidthU;
                final float sy = cellHeight2/ shapeHeightU;
                sxy = sx < sy ? sx : sy;
                dxh += sxy * margin.left   + shapeWidthU_LH  * ( sx - sxy ); // adjustment for scale-axis
                dyh += sxy * margin.bottom + shapeHeightU_BH * ( sy - sxy ); // ditto
                if( TRACE_LAYOUT ) {
                    System.err.println("bl("+i+").s: "+sx+" x "+sy+" -> "+sxy+": +"+dxh+" / "+dyh+", U: s "+shapeWidthU+" x "+shapeHeightU+", sz "+cellWidth2+" x "+cellHeight2);
                }
            } else {
                sxy = 1;
            }
            final float shapeWidthS = sxy * shapeWidthU;
            final float shapeHeightS = sxy * shapeHeightU;
            final float cellWidthS = hasCellWidth ? cellSize.x() : shapeWidthS;
            final float cellHeightS = hasCellHeight ? cellSize.y() : shapeHeightS;

            if( !isScaled ) {
                // Use half delta of net-size to center, margin is ignored when centering
                final float shapeWidthS_H = sxy * sbox.getWidth() * 0.5f;
                final float shapeHeightS_H = sxy * sbox.getHeight() * 0.5f;
                final float cellWidthS_H = hasCellWidth ? cellSize.x() * 0.5f : shapeWidthS_H;
                final float cellHeightS_H = hasCellHeight ? cellSize.y() * 0.5f : shapeHeightS_H;
                if( isCenteredHoriz ) {
                    dxh += cellWidthS_H - shapeWidthS_H; // horiz-center
                } else {
                    dxh += sxy * margin.left;
                }
                if( isCenteredVert ) {
                    dyh += cellHeightS_H - shapeHeightS_H; // vert-center
                } else {
                    dyh += sxy * margin.bottom;
                }
            }
            if( TRACE_LAYOUT ) {
                System.err.println("bl("+i+").m: "+x+" / "+y+" + "+dxh+" / "+dyh+", sxy "+sxy+", S: s "+shapeWidthS+" x "+shapeHeightS+", sz "+cellWidthS+" x "+cellHeightS);
            }
            // Position and scale shape
            {
                // New shape position, relative to previous position
                final float aX = x + dxh;
                final float aY = y + dyh;
                s.moveTo( aX, aY, s.getPosition().z() );

                // remove the bottom-left delta
                final Vec3f diffBL = new Vec3f();
                final AABBox sbox0 = s.getBounds();
                if( !diffBL.set( sbox0.getLow().x(), sbox0.getLow().y(), 0).min( zeroVec3 ).isZero() ) {
                    // pmv.mulMvMatVec3f(diffBL).scale(-1f, -1f, 0f);
                    final Vec3f ss = s.getScale();
                    diffBL.scale(-1f*ss.x(), -1f*ss.y(), 0f);
                }
                if( TRACE_LAYOUT ) {
                    System.err.println("bl("+i+").bl: sbox0 "+sbox0+", diffBL "+diffBL);
                }
                s.move( diffBL.scale(sxy) );

                // resize bounds
                box.resize(  x,               y,               sbox.getMinZ());
                box.resize(  x + cellWidthS,  y + cellHeightS, sbox.getMaxZ());
            }
            s.scale( sxy, sxy, 1f);

            if( TRACE_LAYOUT ) {
                System.err.println("bl("+i+").x: "+dxh+" / "+dyh+" -> "+s.getPosition()+", p3 "+shapeWidthS+" x "+shapeHeightS+", sz3 "+cellWidthS+" x "+cellHeightS+", box "+box.getWidth()+" x "+box.getHeight());
                System.err.println("bl("+i+").x: "+s);
                System.err.println("bl("+i+").x: "+box);
            }
        }
    }

    @Override
    public String toString() {
        final String p_s = ( null == padding || padding.zeroSumSize() ) ? "" : ", "+padding.toString();
        final String m_s = margin.zeroSumSize() ? "" : ", "+margin.toString();
        return "Box[cell "+cellSize+", a "+alignment+m_s+p_s+"]";
    }
}

