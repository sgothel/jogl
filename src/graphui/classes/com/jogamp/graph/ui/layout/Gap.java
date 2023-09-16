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
 * GraphUI CSS property Gap, spacing between (grid) cells not belonging to the element.
 * <p>
 * The CSS gap property defines the size of the gap between the rows and columns in a grid layout.
 * </p>
 */
public class Gap {
    /** Row gap value, vertical spacing. */
    public final float row;
    /** Column gap value, horizontal spacing. */
    public final float column;

    /**
     * Ctor w/ zero values
     */
    public Gap() {
        row = 0f; column = 0f;
    }

    /**
     * Ctor
     * @param row vertical row value
     * @param column horizontal column value
     */
    public Gap(final float row, final float column) {
        this.row = row; this.column = column;
    }

    /**
     * Ctor
     * @param rc vertical row and horizontal column value
     */
    public Gap(final float rc) {
        this.row = rc; this.column = rc;
    }

    /** Return width of horizontal value, i.e. 1 * column. */
    public float width() { return column; }

    /** Return height of vertical value, i.e. 1 * row. */
    public float height() { return row; }

    public boolean zeroSumWidth() { return FloatUtil.isZero( width() ); };

    public boolean zeroSumHeight() { return FloatUtil.isZero( height() ); };

    public boolean zeroSumSize() { return zeroSumWidth() && zeroSumHeight(); }

    @Override
    public String toString() { return "Gap[r "+row+", c "+column+"]"; }
}
