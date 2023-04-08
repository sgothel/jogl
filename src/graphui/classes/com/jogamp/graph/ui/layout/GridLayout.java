/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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

import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.Group.Layout;

public class GridLayout implements Group.Layout {
    private final int columns;
    private final float padX, padY;

    /**
     *
     * @param columns [1..inf)
     * @param padX
     * @param padY
     */
    public GridLayout(final int columns, final float padX, final float padY) {
        this.columns = Math.max(1, columns);
        this.padX = padX;
        this.padY = padY;
    }

    @Override
    public void layout(final Group g) {
        int col = 0;
        float x=0;
        float y=0;
        for(final Shape s : g.getShapes()) {
            s.moveTo(x, y, 0);
            if( col + 1 == columns ) {
                col = 0;
                // row++;
                x  = 0;
                y -= padY;
            } else {
                col++;
                x += padX;
            }
        }
    }
}

