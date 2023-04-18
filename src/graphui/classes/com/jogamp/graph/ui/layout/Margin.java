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

import com.jogamp.opengl.math.FloatUtil;

/**
 * GraphUI CSS property Margin, space between or around elements.
 *
 * The CSS margin properties are used to create space around elements, outside of any defined borders.
 *
 * {@link Margin#CENTER} is mapped to `zero` while earmarking {@link #isCenteredHoriz()} and {@link #isCenteredVert()}.
 * The container must be sized via its layout horizontally and/or vertically matching the centered axis, similar to CSS.
 */
public class Margin {
    /** Auto margin value to horizontally and/or vertically center an element within its sized-layout container, value if {@link Float#NaN}. */
    public static final float CENTER = Float.NaN;

    /** Top value */
    public final float top;
    /** Right value */
    public final float right;
    /** Bottom value */
    public final float bottom;
    /** Left value */
    public final float left;

    private final int bits;
    static private final int CENTER_HORIZ = 1 << 0;
    static private final int CENTER_VERT  = 1 << 1;
    static private int getBits(final float top, final float right, final float bottom, final float left) {
        int b = 0;
        if( FloatUtil.isEqual(CENTER, left) && FloatUtil.isEqual(CENTER, right) ) {
            b |= CENTER_HORIZ;
        }
        if( FloatUtil.isEqual(CENTER, top) && FloatUtil.isEqual(CENTER, bottom) ) {
            b |= CENTER_VERT;
        }
        return b;
    }

    /**
     * Ctor w/ zero values
     */
    public Margin() {
        top = 0f; right = 0f; bottom = 0f; left = 0f; bits = 0;
    }

    /**
     * Ctor
     * @param top top value
     * @param right right value
     * @param bottom bottom value
     * @param left left value
     */
    public Margin(final float top, final float right, final float bottom, final float left) {
        this.bits = getBits(top, right, bottom, left);
        if( isCenteredVert() ) {
            this.top = 0;
            this.bottom = 0;
        } else {
            this.top = top;
            this.bottom = bottom;
        }
        if( isCenteredHoriz() ) {
            this.right = 0;
            this.left = 0;
        } else {
            this.right = right;
            this.left = left;
        }
    }

    /**
     * Ctor
     * @param top top value
     * @param rl right and left value, use {@link #CENTER} to horizontally center the element in its container
     * @param bottom bottom value
     */
    public Margin(final float top, final float rl, final float bottom) {
        this.bits = getBits(top, rl, bottom, rl);
        if( isCenteredVert() ) {
            this.top = 0;
            this.bottom = 0;
        } else {
            this.top = top;
            this.bottom = bottom;
        }
        if( isCenteredHoriz() ) {
            this.right = 0;
            this.left = 0;
        } else {
            this.right = rl;
            this.left = rl;
        }
    }

    /**
     * Ctor
     * @param tb top and bottom value, use {@link #CENTER} to vertically center the element in its container
     * @param rl right and left value, use {@link #CENTER} to horizontally center the element in its container
     */
    public Margin(final float tb, final float rl) {
        this.bits = getBits(tb, rl, tb, rl);
        if( isCenteredVert() ) {
            this.top = 0;
            this.bottom = 0;
        } else {
            this.top = tb;
            this.bottom = tb;
        }
        if( isCenteredHoriz() ) {
            this.right = 0;
            this.left = 0;
        } else {
            this.right = rl;
            this.left = rl;
        }
    }

    /**
     * Ctor
     * @param trbl top, right, bottom and left value, use {@link #CENTER} to horizontally and vertically center the element in its container.
     */
    public Margin(final float trbl) {
        this.bits = getBits(trbl, trbl, trbl, trbl);
        if( isCenteredVert() ) {
            this.top = 0;
            this.bottom = 0;
        } else {
            this.top = trbl;
            this.bottom = trbl;
        }
        if( isCenteredHoriz() ) {
            this.right = 0;
            this.left = 0;
        } else {
            this.right = trbl;
            this.left = trbl;
        }
    }

    /** Returns true if {@link #left} and {@link #right} is {@link #CENTER}. */
    public boolean isCenteredHoriz() {
        return 0 != ( CENTER_HORIZ & bits );
    }

    /** Returns true if {@link #top} and {@link #bottom} is {@link #CENTER}. */
    public boolean isCenteredVert() {
        return 0 != ( CENTER_VERT & bits );
    }

    /** Returns true if {@link #isCenteredHoriz()} and {@link #isCenteredVert()} is true, i.e. for horizontal and vertical center. */
    public boolean isCentered() {
        return 0 != ( ( CENTER_VERT | CENTER_HORIZ ) & bits );
    }

    /** Return width of horizontal values top + right. Zero if {@link #isCenteredHoriz()}. */
    public float width() { return left + right; }

    /** Return height of vertical values bottom + top. Zero if {@link #isCenteredVert()}. */
    public float height() { return bottom + top; }

    public boolean zeroSumWidth() { return FloatUtil.isZero( width() ); };

    public boolean zeroSumHeight() { return FloatUtil.isZero( height() ); };

    public boolean zeroSumSize() { return zeroSumWidth() && zeroSumHeight(); }

    @Override
    public String toString() { return "Margin[t "+top+", r "+right+", b "+bottom+", l "+left+", ctr[h "+isCenteredHoriz()+", v "+isCenteredVert()+"]]"; }
}
