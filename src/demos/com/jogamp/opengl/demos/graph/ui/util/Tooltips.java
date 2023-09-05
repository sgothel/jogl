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
package com.jogamp.opengl.demos.graph.ui.util;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.shapes.Button;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.texture.TextureSequence;

public class Tooltips {
    /**
     * Shows the {@link Label#getText()} within a given proportion of {@link Scene#getBounds()} height within a rectangular {@link Button} tool-tip on click.
     * <p>
     * This {@link Shape.MouseGestureAdapter} must be {@link Shape#addMouseListener(com.jogamp.graph.ui.Shape.MouseGestureListener) added}
     * to a {@link Label} to be functional.
     * </p>
     */
    public static class ZoomLabelOnClickListener extends Shape.MouseGestureAdapter {
        private final Scene scene;
        private final int renderModes;
        private final float sceneHeightScale;
        private Button buttonLabel = null;

        /**
         * Ctor of {@link ZoomLabelOnClickListener}.
         * @param scene the {@link Scene} to be attached to while pressed
         * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
         * @param sceneHeightScale proportion of {@link Scene#getBounds()} height to cover with this tool-tip
         */
        public ZoomLabelOnClickListener(final Scene scene, final int renderModes, final float sceneHeightScale) {
            this.scene = scene;
            this.renderModes = renderModes;
            this.sceneHeightScale = sceneHeightScale;
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            final AABBox sceneDim = scene.getBounds();
            final float zEps = scene.getZEpsilon(16);

            final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
            if( !( shapeEvent.shape instanceof Label ) ) {
                return;
            }
            final Label label = (Label)shapeEvent.shape;
            final AABBox textDim = label.getBounds();
            final float l_sxy = sceneHeightScale * sceneDim.getHeight() / textDim.getHeight(); // text width independent
            buttonLabel = (Button) new Button(renderModes, label.getFont(), label.getText(), textDim.getWidth(), textDim.getHeight(), zEps)
                    .setPerp().scale(l_sxy, l_sxy, 1).move(0, 0, 10*zEps)
                    .setColor(0.97f, 0.97f, 0.97f, 0.92f).setBorder(0.05f).setBorderColor(0, 0, 0, 1)
                    .setInteractive(false);
            buttonLabel.setLabelColor(0, 0, 0);
            buttonLabel.setSpacing(Button.DEFAULT_SPACING_X, Button.DEFAULT_SPACING_X);
            final Shape s = buttonLabel;
            scene.invoke(false, (final GLAutoDrawable drawable) -> {
                s.validate(drawable.getGL().getGL2ES2());
                s.move(-s.getScaledWidth()/2f, -s.getScaledHeight()/2f, 0);
                // System.err.println("Add "+s);
                scene.addShape(s);
                return true;
            });
        }
        @Override
        public void mouseReleased(final MouseEvent e) {
            if( null != buttonLabel ) {
                final Shape s = buttonLabel;
                buttonLabel = null;
                scene.invoke(false, (final GLAutoDrawable drawable) -> {
                    // System.err.println("Remove "+s);
                    scene.removeShape(drawable.getGL().getGL2ES2(), s);
                    return true;
                });
            }
        }
    };

}
