/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.graph.demos.ui;

import com.jogamp.opengl.GL2ES2;

import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.opengl.util.texture.ImageSequence;

/**
 * GPU based resolution independent {@link ImageSequence} Button impl
 */
public class ImageSeqButton extends TextureSeqButton {

    public ImageSeqButton(final Factory<? extends Vertex> factory, final int renderModes,
                         final float width, final float height, final ImageSequence texSeq) {
        super(factory, renderModes, width, height, texSeq);
        setColor(0.95f, 0.95f, 0.95f, 1.0f);
        setPressedColorMod(1f, 1f, 1f, 0.9f);
        setToggleOffColorMod(0.8f, 0.8f, 0.8f, 1.0f);
        setToggleOnColorMod(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public final void setCurrentIdx(final int idx) {
        ((ImageSequence)texSeq).setCurrentIdx(idx);
        markStateDirty();
    }

    @Override
    public void drawShape(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount) {
        super.drawShape(gl, renderer, sampleCount);
        if( !((ImageSequence)texSeq).getManualStepping() ) {
            markStateDirty(); // keep on going
        }
    };
}
