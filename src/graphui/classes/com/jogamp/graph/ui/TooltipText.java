/**
 * Copyright 2023-2024 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.ui;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.ui.shapes.Button;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.plane.AffineTransform;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLProfile;

/** A HUD text {@link Tooltip} for {@link Shape}, see {@link Shape#setToolTip(Tooltip)}. */
public class TooltipText extends Tooltip {
    /** Text of this tooltip */
    private final CharSequence tipText;
    /** Font of this tooltip */
    private final Font tipFont;
    private final float scaleY;

    /**
     * Ctor of {@link TooltipText}.
     * @param tipText HUD tip text
     * @param tipFont HUD tip font
     * @param backColor HUD tip background color
     * @param labelColor HUD tip label color
     * @param scaleY HUD tip vertical scale against tool height
     * @param delayMS delay until HUD tip is visible after timer start (mouse moved)
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     */
    public TooltipText(final CharSequence tipText, final Font tipFont, final Vec4f backColor, final Vec4f labelColor,
                       final float scaleY, final long delayMS, final int renderModes)
    {
        super(backColor, labelColor, delayMS, renderModes);
        this.tipText = tipText;
        this.tipFont = tipFont;
        this.scaleY = scaleY;
    }
    /**
     * Ctor of {@link TooltipText} using {@link Tooltip#DEFAULT_DELAY}, {@link Region#VBAA_RENDERING_BIT}
     * and a slightly transparent yellow background with an almost black opaque text color.
     * @param tipText HUD tip text
     * @param tipFont HUD tip font
     * @param scaleY HUD tip vertical scale against tool height
     * @param tool the tool shape for this tip
     */
    public TooltipText(final CharSequence tipText, final Font tipFont, final float scaleY) {
        this(tipText, tipFont, null, null, scaleY, Tooltip.DEFAULT_DELAY, Region.VBAA_RENDERING_BIT);
    }

    @Override
    public Shape createTip(final GLAutoDrawable gl, final Scene scene, final PMVMatrix4f pmv, final AABBox toolMvBounds) {
        final float zEps = scene.getZEpsilon(16);

        // Precompute text-box size .. guessing pixelSize
        final AABBox sceneAABox = scene.getBounds();
        final AffineTransform tempT1 = new AffineTransform();
        final AffineTransform tempT2 = new AffineTransform();
        final AABBox tipBox_em = tipFont.getGlyphBounds(tipText, tempT1, tempT2);

        float h = toolMvBounds.getHeight() * scaleY;
        float w = tipBox_em.getWidth() * h / tipBox_em.getHeight();
        if( w > sceneAABox.getWidth() * 0.9f) {
            w = sceneAABox.getWidth() * 0.9f;
            h = tipBox_em.getHeight() * w / tipBox_em.getWidth();
        } else if( h > sceneAABox.getHeight() * 0.9f) {
            h = sceneAABox.getHeight() * 0.9f;
            w = tipBox_em.getWidth() * h / tipBox_em.getHeight();
        }
        final Vec2f pos = getTipMvPosition(scene, toolMvBounds, w, h);

        final Button ntip = (Button) new Button(renderModes, tipFont, tipText, w, h, zEps)
                .setPerp()
                .moveTo(pos.x(), pos.y(), 100*zEps)
                .setColor(backColor)
                // .setBorder(0.05f).setBorderColor(0, 0, 0, 1)
                .setInteractive(false);
        ntip.setLabelColor(frontColor);
        ntip.setSpacing(0.10f, 0.10f);
        return ntip;
    }
}
