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

import com.jogamp.math.FloatUtil;

/**
 * GraphUI CSS property Margin, scaled space between or around elements and not included in the element's size.
 * <p>
 * The CSS margin properties are used to create space around elements, outside of any defined borders.
 * </p>
 * <p>
 * Center alignment is defined via {@link Alignment} and {@link Margin} ignored with only center {@link Alignment} w/o {@link Alignment.Bit#Fill} scale.
 * </p>
 */
public class Margin {
    /** Zero margin constant. */
    public static final Margin None = new Margin();

    /** Scaled top value */
    public final float top;
    /** Scaled right value */
    public final float right;
    /** Scaled bottom value */
    public final float bottom;
    /** Scaled left value */
    public final float left;

    private Margin() {
        this(0f);
    }

    /**
     * Ctor
     * @param top scaled top value
     * @param right scaled right value
     * @param bottom scaled bottom value
     * @param left scaled left value
     */
    public Margin(final float top, final float right, final float bottom, final float left) {
        this.top = top;
        this.bottom = bottom;
        this.right = right;
        this.left = left;
    }

    /**
     * Ctor
     * @param top scaled top value
     * @param rl scaled right and left value
     * @param bottom scaled bottom value
     */
    public Margin(final float top, final float rl, final float bottom) {
        this.top = top;
        this.bottom = bottom;
        this.right = rl;
        this.left = rl;
    }

    /**
     * Ctor
     * @param tb scaled top and bottom value
     * @param rl scaled right and left value
     */
    public Margin(final float tb, final float rl) {
        this.top = tb;
        this.bottom = tb;
        this.right = rl;
        this.left = rl;
    }

    /**
     * Ctor
     * @param trbl scaled top, right, bottom and left value
     */
    public Margin(final float trbl) {
        this.top = trbl;
        this.bottom = trbl;
        this.right = trbl;
        this.left = trbl;
    }

    /** Return scaled width of horizontal values top + right. Zero if {@link #isCenteredHoriz()}. */
    public float width() { return left + right; }

    /** Return scaled height of vertical values bottom + top. Zero if {@link #isCenteredVert()}. */
    public float height() { return bottom + top; }

    public boolean zeroWidth() { return FloatUtil.isZero( width() ); };

    public boolean zeroHeight() { return FloatUtil.isZero( height() ); };

    public boolean zeroSize() { return zeroWidth() && zeroHeight(); }

    @Override
    public String toString() { return "Margin[t "+top+", r "+right+", b "+bottom+", l "+left+"]"; }
}
