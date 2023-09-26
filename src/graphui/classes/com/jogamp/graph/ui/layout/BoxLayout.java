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
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;

/**
 * GraphUI Stack {@link Group.Layout}.
 * <p>
 * A stack of {@link Shape}s
 * <ul>
 *   <li>Optionally centered {@link Alignment.Bit#CenterHoriz horizontally}, {@link Alignment.Bit#CenterVert vertically} or {@link Alignment#Center both}.</li>
 *   <li>Optionally scaled to cell-size if given and {@link Alignment#Fill}</li>
 *   <li>{@link Padding} is applied to each {@Shape} via {@link Shape#setPaddding(Padding)} if passed in constructor and is scaled if {@link Alignment.Bit#Fill}</li>
 *   <li>{@link Margin} is applied unscaled if used and ignored with only center {@link Alignment} w/o {@link Alignment.Bit#Fill} scale</li>
 *   <li>Not implemented {@link Alignment}: {@link Alignment.Bit#Top Top}, {@link Alignment.Bit#Right Right}, {@link Alignment.Bit#Bottom Bottom}, {@link Alignment.Bit#Left Left}</li>
 * </ul>
 * </p>
 */
public class BoxLayout implements Group.Layout {
    private final Vec2f cellSize;
    private final Alignment alignment;
    private final Margin margin; // unscaled
    private final Padding padding; // scaled

    private static final boolean TRACE_LAYOUT = false;


    /**
     */
    public BoxLayout() {
        this(0f, 0f, new Alignment(), new Margin(), null);
    }

    /**
     *
     * @param padding {@link Padding} applied to each {@Shape} via {@link Shape#setPaddding(Padding)} and is scaled if {@link Alignment.Bit#Fill}
     */
    public BoxLayout(final Padding padding) {
        this(0f, 0f, new Alignment(), new Margin(), padding);
    }

    public BoxLayout(final float cellWidth, final float cellHeight) {
        this(cellWidth, cellHeight, new Alignment(), new Margin(), null);
    }

    /**
     *
     * @param cellWidth
     * @param cellHeight
     * @param alignment
     */
    public BoxLayout(final float cellWidth, final float cellHeight, final Alignment alignment) {
        this(cellWidth, cellHeight, alignment, new Margin(), null);
    }

    /**
     *
     * @param cellWidth
     * @param cellHeight
     * @param margin {@link Margin} is applied unscaled and ignored with only center {@link Alignment} w/o {@link Alignment.Bit#Fill} scale
     */
    public BoxLayout(final float cellWidth, final float cellHeight, final Margin margin) {
        this(cellWidth, cellHeight, new Alignment(), margin, null);
    }

    /**
     *
     * @param cellWidth
     * @param cellHeight
     * @param padding {@link Padding} applied to each {@Shape} via {@link Shape#setPaddding(Padding)} and is scaled if {@link Alignment.Bit#Fill}
     */
    public BoxLayout(final float cellWidth, final float cellHeight, final Padding padding) {
        this(cellWidth, cellHeight, new Alignment(), new Margin(), padding);
    }

    /**
     *
     * @param cellWidth
     * @param cellHeight
     * @param margin {@link Margin} is applied unscaled and ignored with only center {@link Alignment} w/o {@link Alignment.Bit#Fill} scale
     * @param padding {@link Padding} applied to each {@Shape} via {@link Shape#setPaddding(Padding)} and is scaled if {@link Alignment.Bit#Fill}
     */
    public BoxLayout(final float cellWidth, final float cellHeight, final Margin margin, final Padding padding) {
        this(cellWidth, cellHeight, new Alignment(), margin, padding);
    }

    /**
     *
     * @param cellWidth
     * @param cellHeight
     * @param margin {@link Margin} is applied unscaled
     */
    public BoxLayout(final float cellWidth, final float cellHeight, final Alignment alignment, final Margin margin) {
        this(cellWidth, cellHeight, alignment, margin, null);
    }

    /**
     *
     * @param cellWidth
     * @param cellHeight
     * @param alignment
     * @param padding {@link Padding} applied to each {@Shape} via {@link Shape#setPaddding(Padding)} and is scaled if {@link Alignment.Bit#Fill}
     */
    public BoxLayout(final float cellWidth, final float cellHeight, final Alignment alignment, final Padding padding) {
        this(cellWidth, cellHeight, alignment, new Margin(), padding);
    }

    /**
     *
     * @param cellWidth
     * @param cellHeight
     * @param alignment
     * @param margin {@link Margin} is applied unscaled and ignored with only center {@link Alignment} w/o {@link Alignment.Bit#Fill} scale
     * @param padding {@link Padding} applied to each {@Shape} via {@link Shape#setPaddding(Padding)} and is scaled if {@link Alignment.Bit#Fill}
     */
    public BoxLayout(final float cellWidth, final float cellHeight, final Alignment alignment, final Margin margin, final Padding padding) {
        this.cellSize = new Vec2f(Math.max(0f, cellWidth), Math.max(0f, cellHeight));
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
    public void layout(final Group g, final AABBox box, final PMVMatrix4f pmv) {
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
            pmv.pushMv();
            s.setTransformMv(pmv);
            s.getBounds().transform(pmv.getMv(), sbox);
            pmv.popMv();

            final int x = 0, y = 0;
            if( TRACE_LAYOUT ) {
                System.err.println("bl("+i+").0: sbox "+sbox+", s "+s);
            }

            // IF isScaled: Uniform scale w/ lowest axis scale and center position on lower-scale axis
            final float shapeWidthU  = sbox.getWidth();
            final float shapeHeightU = sbox.getHeight();
            final float sxy;
            float dxh = 0, dyh = 0;
            if( isScaled ) {
                // scaling to cell size
                final float cellWidth = hasCellWidth ? cellSize.x() - margin.width() : shapeWidthU;
                final float cellHeight = hasCellHeight ? cellSize.y() - margin.height() : shapeHeightU;
                final float sx = cellWidth / shapeWidthU;
                final float sy = cellHeight/ shapeHeightU;
                sxy = sx < sy ? sx : sy;

                if( isCenteredHoriz ) {
                    dxh += shapeWidthU  * ( sx - sxy ) * 0.5f; // horiz-center (adjustment for scale-axis w/o margin)
                }
                if( isCenteredVert ) {
                    dyh += shapeHeightU * ( sy - sxy ) * 0.5f; // vert-center (adjustment for scale-axis w/o margin)
                }
                dyh += margin.bottom; // always consider unscaled margin when scaling
                dxh += margin.left;   // ditto
                if( TRACE_LAYOUT ) {
                    System.err.println("bl("+i+").s: "+sx+" x "+sy+" -> "+sxy+": +"+dxh+" / "+dyh+", U: s "+shapeWidthU+" x "+shapeHeightU+", sz "+cellWidth+" x "+cellHeight);
                }
            } else {
                sxy = 1;
            }
            final float shapeWidthS = sxy * shapeWidthU;
            final float shapeHeightS = sxy * shapeHeightU;
            final float cellWidthS = hasCellWidth ? cellSize.x() : shapeWidthS;
            final float cellHeightS = hasCellHeight ? cellSize.y() : shapeHeightS;

            if( !isScaled ) {
                // Center w/o scale and ignoring margin (not scaled)
                if( isCenteredHoriz ) {
                    dxh += 0.5f * ( cellWidthS - shapeWidthS ); // horiz-center
                } else {
                    dxh += margin.left;
                }
                if( isCenteredVert ) {
                    dyh += 0.5f * ( cellHeightS - shapeHeightS ); // vert-center
                } else {
                    dyh += margin.bottom;
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

                // Remove the negative or positive delta on centered axis.
                // Only remove negative offset of non-centered axis (i.e. underline)
                final Vec3f diffBL = new Vec3f(s.getBounds().getLow());
                diffBL.setZ(0);
                if( isCenteredHoriz || isCenteredVert ) {
                    if( !isCenteredVert && diffBL.y() > 0 ) {
                        diffBL.setY(0); // only adjust negative if !center-vert
                    } else if( !isCenteredHoriz && diffBL.x() > 0 ) {
                        diffBL.setX(0); // only adjust negative if !center-horiz
                    }
                    diffBL.scale(s.getScale()).scale(-1f);
                } else {
                    diffBL.min(new Vec3f()).scale(s.getScale()).scale(-1f);
                }
                s.move( diffBL.scale(sxy) );
                if( TRACE_LAYOUT ) {
                    System.err.println("bl("+i+").bl: sbox0 "+s.getBounds()+", diffBL_ "+diffBL);
                }

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

