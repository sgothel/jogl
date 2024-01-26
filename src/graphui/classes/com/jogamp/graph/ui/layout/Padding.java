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
 * GraphUI CSS property Padding, unscaled space belonging to the element and included in the element's size.
 * <p>
 * The CSS padding properties are used to generate space around an element's content, inside of any defined borders.
 * </p>
 */
public class Padding {
    /** Zero padding constant. */
    public static final Padding None = new Padding();

    /** Top value (unscaled) */
    public final float top;
    /** Right value (unscaled) */
    public final float right;
    /** Bottom value (unscaled) */
    public final float bottom;
    /** Left value (unscaled) */
    public final float left;

    private Padding() {
        this(0f);
    }

    /**
     * Ctor
     * @param top unscaled top value
     * @param right unscaled right value
     * @param bottom unscaled bottom value
     * @param left unscaled left value
     */
    public Padding(final float top, final float right, final float bottom, final float left) {
        this.top = top; this.right = right; this.bottom = bottom; this.left = left;
    }

    /**
     * Ctor
     * @param top unscaled top value
     * @param rl unscaled right and left value
     * @param bottom unscaled bottom value
     */
    public Padding(final float top, final float rl, final float bottom) {
        this.top = top; this.right = rl; this.bottom = bottom; this.left = rl;
    }

    /**
     * Ctor
     * @param tb unscaled top and bottom value
     * @param rl unscaled right and left value
     */
    public Padding(final float tb, final float rl) {
        this.top = tb; this.right = rl; this.bottom = tb; this.left = rl;
    }

    /**
     * Ctor
     * @param trbl unscaled top, right, bottom and left value
     */
    public Padding(final float trbl) {
        this.top = trbl; this.right = trbl; this.bottom = trbl; this.left = trbl;
    }

    /** Return unscaled width of horizontal values top + right. */
    public float width() { return left + right; }

    /** Return unscaled height of vertical values bottom + top. */
    public float height() { return bottom + top; }

    public boolean zeroWidth() { return FloatUtil.isZero( width() ); };

    public boolean zeroHeight() { return FloatUtil.isZero( height() ); };

    public boolean zeroSize() { return zeroWidth() && zeroHeight(); }

    @Override
    public String toString() { return "Padding[t "+top+", r "+right+", b "+bottom+", l "+left+"]"; }
}
