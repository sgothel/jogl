/**
 * Copyright 2014-2023 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.ui.shapes;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.ui.GraphShape;
import com.jogamp.math.Vec4f;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * An abstract GraphUI {@link TextureSequence} {@link BaseButton} {@link GraphShape}.
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 * <p>
 * This button is rendered with a round oval shape.
 * To render it rectangular, {@link #setCorner(float)} to zero.
 * </p>
 */
public abstract class TexSeqButton extends BaseButton {
    protected final TextureSequence texSeq;

    public TexSeqButton(final int renderModes, final float width,
                        final float height, final TextureSequence texSeq) {
        super(renderModes | Region.COLORTEXTURE_RENDERING_BIT, width, height);
        this.texSeq = texSeq;
    }

    public final TextureSequence getTextureSequence() { return this.texSeq; }

    /**
     * See {@link TextureSequence#setARatioAdjustment(boolean)}.
     * @return this instance for chaining
     */
    public TexSeqButton setARatioAdjustment(final boolean v) {
        if( useARatioAdjustment() != v ) {
            texSeq.setARatioAdjustment(v);
            markShapeDirty();
        }
        return this;
    }
    /**
     * See {@link TextureSequence#useARatioAdjustment()}.
     */
    public boolean useARatioAdjustment() { return texSeq.useARatioAdjustment(); }

    /**
     * See {@link TextureSequence#setARatioLetterbox(boolean, Vec4f)}.
     * @param v new value for {@link #useARatioLetterbox()}
     * @param backColor optional background color for added letter-box space, defaults to transparent zero
     * @return this instance for chaining
     */
    public TexSeqButton setARatioLetterbox(final boolean v, final Vec4f backColor) {
        if( useARatioLetterbox() != v ) {
            texSeq.setARatioLetterbox(v, backColor);
            markShapeDirty();
        }
        return this;
    }
    /**
     * See {@link TextureSequence#useARatioLetterbox()}.
     */
    public boolean useARatioLetterbox() { return texSeq.useARatioLetterbox(); }

    /** See {@link TextureSequence#getARatioLetterboxBackColor()}. */
    public Vec4f getARatioLetterboxBackColor() { return texSeq.getARatioLetterboxBackColor(); }

    @Override
    protected void addShapeToRegion(final GLProfile glp, final GL2ES2 gl) {
        final OutlineShape shape = createBaseShape(0f);
        resetGLRegion(glp, gl, texSeq, shape);
        region.addOutlineShape(shape, null, rgbaColor);
        box.resize(shape.getBounds());
        setRotationPivot( box.getCenter() );
    }
}
