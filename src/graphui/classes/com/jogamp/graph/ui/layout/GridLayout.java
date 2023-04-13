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

import com.jogamp.graph.ui.GraphShape;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Shape;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * GraphUI Grid {@link Group.Layout}.
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
    private final Padding padding;
    private int row_count, col_count;


    /**
     * Default layout order of {@link Group#getShapes()}} is {@link Order#COLUMN}.
     * @param column_limit [1..inf)
     * @param cellWidth
     * @param cellHeight
     */
    public GridLayout(final int column_limit, final float cellWidth, final float cellHeight) {
        this(Math.max(1, column_limit), -1, cellWidth, cellHeight, new Padding());
    }

    /**
     * Default layout order of {@link Group#getShapes()}} is {@link Order#COLUMN}.
     * @param column_limit [1..inf)
     * @param cellWidth
     * @param cellHeight
     * @param padding
     */
    public GridLayout(final int column_limit, final float cellWidth, final float cellHeight, final Padding padding) {
        this(Math.max(1, column_limit), -1, cellWidth, cellHeight, padding);
    }

    /**
     * Default layout order of {@link Group#getShapes()}} is {@link Order#ROW}.
     * @param cellWidth
     * @param cellHeight
     * @param padding
     * @param row_limit [1..inf)
     */
    public GridLayout(final float cellWidth, final float cellHeight, final Padding padding, final int row_limit) {
        this(-1, Math.max(1, row_limit), cellWidth, cellHeight, padding);
    }

    private GridLayout(final int column_limit, final int row_limit, final float cellWidth, final float cellHeight, final Padding padding) {
        this.order = 0 < column_limit ? Order.COLUMN : Order.ROW;
        this.col_limit = column_limit;
        this.row_limit = row_limit;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.padding = padding;;
        row_count = 0;
        col_count = 0;
    }

    public Order getOrder() { return order; }
    public int getColumnCount() { return col_count; }
    public int getRowCount() { return row_count; }

    @Override
    public void layout(final Group g, final AABBox box, final PMVMatrix pmv) {
        final List<Shape> shapes = g.getShapes();
        if( Order.COLUMN == order ) {
            row_count = (int) Math.ceil( (double)shapes.size() / (double)col_limit );
            col_count = col_limit;
        } else { // Order.ROW_MAJOR == order
            row_count = row_limit;
            col_count = (int) Math.ceil( (double)shapes.size() / (double)row_limit );
        }
        int col_i = 0, row_i = 0;
        final AABBox sbox = new AABBox();
        for(final Shape s : shapes) {
            // measure size
            pmv.glPushMatrix();
            s.setTransform(pmv);
            s.getBounds().transformMv(pmv, sbox);
            pmv.glPopMatrix();

            // adjust size and position (centered)
            final float x =  ( (             col_i     ) * ( cellWidth  + padding.width()  ) ) + padding.left;
            final float y =  ( ( row_count - row_i - 1 ) * ( cellHeight + padding.height() ) ) + padding.bottom;
            final float sx = cellWidth / sbox.getWidth();
            final float sy = cellHeight/ sbox.getHeight();
            final float sxy = sx < sy ? sx : sy;
            final float dxh = sbox.getWidth()  * ( sx - sxy ) * 0.5f;
            final float dyh = sbox.getHeight() * ( sy - sxy ) * 0.5f;
            s.moveTo( x + dxh, y + dyh, 0f );       // center the scaled artifact
            s.move( sbox.getLow().mul(-1f*sxy) );   // remove the bottom-left delta
            s.scale( sxy, sxy, 1f);
            box.resize( x + cellWidth + padding.right, y + cellHeight + padding.top,    0);
            box.resize( x             - padding.left,  y -              padding.bottom, 0);
            // System.err.println("["+row_i+"]["+col_i+"]: "+x+" / "+y+", sxy "+sxy+", d[xy]h "+dxh+" x "+dyh+", "+sbox);

            // position for next cell
            if( Order.COLUMN == order ) {
                if( col_i + 1 == col_limit ) {
                    col_i = 0;
                    row_i++;
                } else {
                    col_i++;
                }
            } else { // Order.ROW_MAJOR == order
                if( row_i + 1 == row_limit ) {
                    row_i = 0;
                    col_i++;
                } else {
                    row_i++;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Grid["+row_count+"x"+col_count+", "+order+", cell["+cellWidth+" x "+cellHeight+"], "+padding+"]";
    }
}

