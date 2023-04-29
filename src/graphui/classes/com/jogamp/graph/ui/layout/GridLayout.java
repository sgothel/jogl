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
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * GraphUI Grid {@link Group.Layout}.
 * <p>
 * A grid of {@link Shape}s
 * - Optional cell-size with {@link Alignment#Fill} and or {@link Alignment#Center}
 * - Without cell-size behaves like a grid bag using individual shape sizes including padding
 * - Can be filled in {@link Order#COLUMN} or {@link Order#ROW} major-order.
 * - ..
 * </p>
 */
public class GridLayout implements Group.Layout {
    /** Layout order for {@link Group#getShapes()}} after population. */
    public static enum Order {
        /** COLUMN layout order of {@link Group#getShapes()}} is left to right and top to bottom. */
        COLUMN,
        /** ROW layout order of {@link Group#getShapes()}} is top to bottom and left to right. */
        ROW
    }
    private final Order order;
    private final int col_limit;
    private final int row_limit;
    private final float cellWidth, cellHeight;
    private final Alignment alignment;
    private final Gap gap;
    private int row_count, col_count;

    private static final boolean TRACE_LAYOUT = false;

    /**
     * Default layout order of {@link Group#getShapes()}} is {@link Order#COLUMN}.
     * @param column_limit [1..inf)
     * @param cellWidth
     * @param cellHeight
     * @param alignment TODO
     */
    public GridLayout(final int column_limit, final float cellWidth, final float cellHeight, final Alignment alignment) {
        this(alignment, Math.max(1, column_limit), -1, cellWidth, cellHeight, new Gap());
    }

    /**
     * Default layout order of {@link Group#getShapes()}} is {@link Order#COLUMN}.
     * @param column_limit [1..inf)
     * @param cellWidth
     * @param cellHeight
     * @param alignment TODO
     * @param gap
     */
    public GridLayout(final int column_limit, final float cellWidth, final float cellHeight, final Alignment alignment, final Gap gap) {
        this(alignment, Math.max(1, column_limit), -1, cellWidth, cellHeight, gap);
    }

    /**
     * Default layout order of {@link Group#getShapes()}} is {@link Order#ROW}.
     * @param cellWidth
     * @param cellHeight
     * @param alignment TODO
     * @param gap
     * @param row_limit [1..inf)
     */
    public GridLayout(final float cellWidth, final float cellHeight, final Alignment alignment, final Gap gap, final int row_limit) {
        this(alignment, -1, Math.max(1, row_limit), cellWidth, cellHeight, gap);
    }

    private GridLayout(final Alignment alignment, final int column_limit, final int row_limit, final float cellWidth, final float cellHeight, final Gap gap) {
        this.order = 0 < column_limit ? Order.COLUMN : Order.ROW;
        this.col_limit = column_limit;
        this.row_limit = row_limit;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.alignment = alignment;
        this.gap = gap;
        row_count = 0;
        col_count = 0;
    }

    public Order getOrder() { return order; }
    public int getColumnCount() { return col_count; }
    public int getRowCount() { return row_count; }
    public Gap getGap() { return gap; }

    @Override
    public void layout(final Group g, final AABBox box, final PMVMatrix pmv) {
        final Vec3f zeroVec3 = new Vec3f();
        final boolean hasCellWidth = !FloatUtil.isZero(cellWidth);
        final boolean hasCellHeight = !FloatUtil.isZero(cellHeight);
        final boolean isCenteredHoriz = hasCellWidth && alignment.isSet(Alignment.Bit.Center);
        final boolean isCenteredVert = hasCellHeight && alignment.isSet(Alignment.Bit.Center);
        final boolean isScaled = alignment.isSet(Alignment.Bit.Fill) && ( hasCellWidth || hasCellHeight );
        final List<Shape> shapes = g.getShapes();
        if( Order.COLUMN == order ) {
            row_count = (int) Math.ceil( (double)shapes.size() / (double)col_limit );
            col_count = col_limit;
        } else { // Order.ROW_MAJOR == order
            row_count = row_limit;
            col_count = (int) Math.ceil( (double)shapes.size() / (double)row_limit );
        }
        if( TRACE_LAYOUT ) {
            System.err.println("gl.00: "+order+", "+col_count+" x "+row_count+", a "+alignment+", shapes "+shapes.size()+", "+gap);
        }
        int col_i = 0, row_i = 0;
        float x=0, y=0;
        float totalWidth=-Float.MAX_VALUE, totalHeight=-Float.MAX_VALUE;
        final AABBox[] sboxes = new AABBox[shapes.size()];
        final float[] y_pos = new float[col_count * row_count]; // y_bottom = totalHeight - y_pos[..]

        // Pass-1: Determine totalHeight, while collect sbox and y_pos
        for(int i=0; i < shapes.size(); ++i) {
            final Shape s = shapes.get(i);
            // measure size
            pmv.glPushMatrix();
            s.setTransform(pmv);
            {
                final AABBox sbox0 = s.getBounds();
                sboxes[i] = sbox0.transformMv(pmv, new AABBox());
            }
            pmv.glPopMatrix();
            final AABBox sbox = sboxes[i];

            final float sxy;
            if( isScaled ) {
                // scaling to cell size
                final float shapeWidthU = sbox.getWidth();
                final float shapeHeightU = sbox.getHeight();
                final float cellWidthU = hasCellWidth ? cellWidth : shapeWidthU;
                final float cellHeightU = hasCellHeight ? cellHeight : shapeHeightU;
                final float sx = cellWidthU / shapeWidthU;
                final float sy = cellHeightU/ shapeHeightU;
                sxy = sx < sy ? sx : sy;
            } else {
                sxy = 1;
            }
            final float shapeWidthS = sxy*sbox.getWidth();
            final float shapeHeightS = sxy*sbox.getHeight();
            final float cellWidthS = hasCellWidth ? cellWidth : shapeWidthS;
            final float cellHeightS = hasCellHeight ? cellHeight : shapeHeightS;

            // bottom y_pos, top to bottom, to be subtracted from totalHeight
            final float y0 = y + cellHeightS;
            final float x1 = x + cellWidthS;
            totalHeight = Math.max(totalHeight, y0);
            totalWidth = Math.max(totalWidth, x1);
            y_pos[col_count * row_i + col_i] = y0;
            if( TRACE_LAYOUT ) {
                System.err.println("gl.00: y("+i+")["+col_i+"]["+row_i+"]: "+y0+", ["+cellWidthS+" x "+cellHeightS+"]");
            }

            // position for next cell
            if( i + 1 < shapes.size() ) {
                if( Order.COLUMN == order ) {
                    if( col_i + 1 == col_count ) {
                        col_i = 0;
                        row_i++;
                        x = 0;
                        y += cellHeightS + gap.height();
                    } else {
                        col_i++;
                        x += cellWidthS  + gap.width();
                    }
                } else { // Order.ROW_MAJOR == order
                    if( row_i + 1 == row_count ) {
                        row_i = 0;
                        col_i++;
                        y = 0;
                        x += cellWidthS  + gap.width();
                    } else {
                        row_i++;
                        y += cellHeightS + gap.height();
                    }
                }
            }
        }
        if( TRACE_LAYOUT ) {
            System.err.println("gl[__].00: Total "+totalWidth+" / "+totalHeight);
        }

        // Pass-2: Layout
        row_i = 0; col_i = 0;
        x = 0; y = 0;
        for(int i=0; i < shapes.size(); ++i) {
            final Shape s = shapes.get(i);
            final AABBox sbox = sboxes[i];
            final float zPos = sbox.getCenter().z();
            final Vec3f diffBL = new Vec3f();

            {
                final AABBox sbox0 = s.getBounds();
                if( !diffBL.set( sbox0.getLow().x(), sbox0.getLow().y(), 0).min( zeroVec3 ).isZero() ) {
                    // pmv.mulMvMatVec3f(diffBL).scale(-1f, -1f, 0f);
                    final Vec3f ss = s.getScale();
                    diffBL.scale(-1f*ss.x(), -1f*ss.y(), 0f);
                }
            }

            if( TRACE_LAYOUT ) {
                System.err.println("gl("+i+")["+col_i+"]["+row_i+"].0: "+s);
                System.err.println("gl("+i+")["+col_i+"]["+row_i+"].0: sbox "+sbox+", diffBL "+diffBL);
            }

            // IF isScaled: Uniform scale w/ lowest axis scale and center position on lower-scale axis
            final float sxy;
            float dxh = 0, dyh = 0;
            if( isScaled ) {
                // scaling to cell size
                final float shapeWidthU = sbox.getWidth();
                final float shapeHeightU = sbox.getHeight();
                final float cellWidth2 = hasCellWidth ? cellWidth : shapeWidthU;
                final float cellHeight2 = hasCellHeight ? cellHeight : shapeHeightU;
                final float sx = cellWidth2 / shapeWidthU;
                final float sy = cellHeight2/ shapeHeightU;
                sxy = sx < sy ? sx : sy;
                dxh += shapeWidthU  * ( sx - sxy ) * 0.5f; // adjustment for scale-axis
                dyh += shapeHeightU * ( sy - sxy ) * 0.5f; // ditto
                if( TRACE_LAYOUT ) {
                    System.err.println("gl("+i+")["+col_i+"]["+row_i+"].s: "+sx+" x "+sy+" -> "+sxy+": +"+dxh+" / "+dyh+", U: s "+shapeWidthU+" x "+shapeHeightU+", sz "+cellWidth2+" x "+cellHeight2);
                }
            } else {
                sxy = 1;
            }
            final float shapeWidthS = sxy*sbox.getWidth();
            final float shapeHeightS = sxy*sbox.getHeight();
            final float cellWidthS = hasCellWidth ? cellWidth : shapeWidthS;
            final float cellHeightS = hasCellHeight ? cellHeight : shapeHeightS;

            y = totalHeight - y_pos[col_count * row_i + col_i];

            if( isCenteredHoriz ) {
                dxh += 0.5f * ( cellWidthS - shapeWidthS ); // actual horiz-centered
            }
            if( isCenteredVert ) {
                dyh += 0.5f * ( cellHeightS - shapeHeightS ); // actual vert-centered
            }
            if( TRACE_LAYOUT ) {
                System.err.println("gl("+i+")["+col_i+"]["+row_i+"].m: "+x+" / "+y+" + "+dxh+" / "+dyh+", S: s "+shapeWidthS+" x "+shapeHeightS+", sz "+cellWidthS+" x "+cellHeightS);
            }
            {
                // New shape position, relative to previous position
                final float aX = x + dxh;
                final float aY = y + dyh;
                s.moveTo( aX, aY, 0f );
                s.move( diffBL.scale(sxy) ); // remove the bottom-left delta

                // resize bounds including padding, excluding margin
                box.resize(  x,               y,               zPos);
                box.resize( aX + cellWidthS, aY + cellHeightS, zPos);
            }
            s.scale( sxy, sxy, 1f);

            if( TRACE_LAYOUT ) {
                System.err.println("gl("+i+")["+col_i+"]["+row_i+"].x: "+x+" / "+y+" + "+dxh+" / "+dyh+" -> "+s.getPosition()+", p3 "+shapeWidthS+" x "+shapeHeightS+", sz3 "+cellWidthS+" x "+cellHeightS+", box "+box.getWidth()+" x "+box.getHeight());
                System.err.println("gl("+i+")["+col_i+"]["+row_i+"].x: "+s);
            }

            if( i + 1 < shapes.size() ) {
                // position for next cell
                if( Order.COLUMN == order ) {
                    if( col_i + 1 == col_count ) {
                        col_i = 0;
                        row_i++;
                        x = 0;
                    } else {
                        col_i++;
                        x += cellWidthS  + gap.width();
                    }
                } else { // Order.ROW_MAJOR == order
                    if( row_i + 1 == row_count ) {
                        row_i = 0;
                        col_i++;
                        y = 0;
                        x += cellWidthS  + gap.width();
                    } else {
                        row_i++;
                    }
                }
            }
        }
        if( TRACE_LAYOUT ) {
            System.err.println("gl.xx: "+box);
        }
    }

    @Override
    public String toString() {
        return "Grid["+col_count+"x"+row_count+", "+order+", cell["+cellWidth+" x "+cellHeight+", a "+alignment+"], "+gap+"]";
    }
}

