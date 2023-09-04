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
 * GraphUI CSS property Padding, space belonging to the element and included in the element's size.
 *
 * The CSS padding properties are used to generate space around an element's content, inside of any defined borders.
 */
public class Padding {
    /** Top value */
    public final float top;
    /** Right value */
    public final float right;
    /** Bottom value */
    public final float bottom;
    /** Left value */
    public final float left;

    /**
     * Ctor w/ zero values
     */
    public Padding() {
        this(0f);
    }

    /**
     * Ctor
     * @param top top value
     * @param right right value
     * @param bottom bottom value
     * @param left left value
     */
    public Padding(final float top, final float right, final float bottom, final float left) {
        this.top = top; this.right = right; this.bottom = bottom; this.left = left;
    }

    /**
     * Ctor
     * @param top top value
     * @param rl right and left value
     * @param bottom bottom value
     */
    public Padding(final float top, final float rl, final float bottom) {
        this.top = top; this.right = rl; this.bottom = bottom; this.left = rl;
    }

    /**
     * Ctor
     * @param tb top and bottom value
     * @param rl right and left value
     */
    public Padding(final float tb, final float rl) {
        this.top = tb; this.right = rl; this.bottom = tb; this.left = rl;
    }

    /**
     * Ctor
     * @param trbl top, right, bottom and left value
     */
    public Padding(final float trbl) {
        this.top = trbl; this.right = trbl; this.bottom = trbl; this.left = trbl;
    }

    /** Return width of horizontal values top + right. */
    public float width() { return left + right; }

    /** Return height of vertical values bottom + top. */
    public float height() { return bottom + top; }

    public boolean zeroSumWidth() { return FloatUtil.isZero( width() ); };

    public boolean zeroSumHeight() { return FloatUtil.isZero( height() ); };

    public boolean zeroSumSize() { return zeroSumWidth() && zeroSumHeight(); }

    @Override
    public String toString() { return "Padding[t "+top+", r "+right+", b "+bottom+", l "+left+"]"; }
}
