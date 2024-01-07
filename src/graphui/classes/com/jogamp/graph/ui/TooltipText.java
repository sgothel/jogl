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
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.plane.AffineTransform;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.GLProfile;

/** A HUD text {@link Tooltip} for {@link Shape}, see {@link Shape#setToolTip(CharSequence, com.jogamp.graph.font.Font, float, Scene)}. */
public class TooltipText extends Tooltip {
    /** Text of this tooltip */
    final private CharSequence tipText;
    /** Font of this tooltip */
    final private Font tipFont;
    final private float scaleY;
    final private int renderModes;
    /**
     * Ctor of {@link TooltipText}.
     * @param scene the {@link Scene} to be attached to while pressed
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     */
    /* pp */ TooltipText(final CharSequence tipText, final Font tipFont, final float scaleY, final Shape tool, final long delayMS, final Scene scene, final int renderModes) {
        super(tool, delayMS, scene);
        this.tipText = tipText;
        this.tipFont = tipFont;
        this.scaleY = scaleY;
        this.renderModes = renderModes;
    }

    @Override
    public GraphShape createTip(final PMVMatrix4f pmv) {
        this.delayT1 = 0;
        final float zEps = scene.getZEpsilon(16);

        // Precompute text-box size .. guessing pixelSize
        final AffineTransform tempT1 = new AffineTransform();
        final AffineTransform tempT2 = new AffineTransform();
        final AABBox tipBox_em = tipFont.getGlyphBounds(tipText, tempT1, tempT2);

        // final AABBox toolAABox = scene.getBounds(new PMVMatrix4f(), tool);
        final AABBox toolAABox = tool.getBounds().transform(pmv.getMv(), new AABBox());

        final float h = toolAABox.getHeight() * scaleY;
        final float w = tipBox_em.getWidth() / tipBox_em.getHeight() * h;

        final AABBox sceneAABox = scene.getBounds();
        final float xpos, ypos;
        if( toolAABox.getCenter().x()-w/2 < sceneAABox.getLow().x() ) {
            xpos = sceneAABox.getLow().x();
        } else {
            xpos = toolAABox.getCenter().x()-w/2;
        }
        if( toolAABox.getHigh().y() > sceneAABox.getHigh().y() - h ) {
            ypos = sceneAABox.getHigh().y() - h;
        } else {
            ypos = toolAABox.getHigh().y();
        }
        final Button ntip = (Button) new Button(renderModes, tipFont, tipText, w, h, zEps)
                .setPerp()
                .moveTo(xpos, ypos, 100*zEps)
                .setColor(1, 1, 0, 0.80f)
                // .setBorder(0.05f).setBorderColor(0, 0, 0, 1)
                .setInteractive(false);
        ntip.setLabelColor(0, 0, 0);
        ntip.setSpacing(0.10f, 0.10f);
        scene.addShape(ntip);
        return ntip;
    }
}
