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

import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.BoxLayout;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;

/** A HUD {@link Shape} {@link Tooltip} for {@link Shape}, see {@link Shape#setToolTip(Tooltip)}. */
public class TooltipShape extends Tooltip {
    /**
     * Optional HUD tip destroy callback for the user provided {@link Shape}, see {@link #Tooltip#destroyTip(GL2ES2, RegionRenderer, Shape)}.
     * <p>
     * In case no callback is being set via {@link TooltipShape#TooltipShape(Vec2f, long, Shape, DestroyCallback)}
     * {@link #Tooltip#destroyTip(GL2ES2, RegionRenderer, Shape)} destroys the shape.
     * Otherwise the callback gets invoked.
     * </p>
     * @param gl
     * @param renderer
     * @param tip the user provided {@link Shape} as passed via {@link TooltipShape#TooltipShape(Vec4f, Vec4f, float, Vec2f, long, int, Shape, DestroyCallback)}.
     * @see TooltipShape#TooltipShape(Vec2f, long, Shape, DestroyCallback)
     * @see TooltipShape#createTip(GLAutoDrawable, Scene, PMVMatrix4f, AABBox)
     */
    public static interface DestroyCallback {
        public void destroyTip(final GL2ES2 gl, final RegionRenderer renderer, final Shape tip);
    }

    /** Text of this tooltip */
    private final Shape tip;
    private final Vec2f scale;
    private final float borderThickness;
    private final DestroyCallback dtorCallback;

    /**
     * Ctor of {@link TooltipShape}.
     * <p>
     * The {@link Shape} is destroyed via {@link #destroyTip(GL2ES2, RegionRenderer, Shape)},
     * since no {@link DestroyCallback} is being provided via {@link TooltipShape#TooltipShape(Vec2f, long, Shape, DestroyCallback)}.
     * </p>
     * @param scale HUD tip scale for the tip shape
     * @param delayMS delay until HUD tip is visible after timer start (mouse moved)
     * @param tip HUD tip shape
     */
    public TooltipShape(final Vec2f scale, final long delayMS, final int renderModes, final Shape tip) {
        this(null, null, 0, scale, delayMS, renderModes, tip, null);
    }

    /**
     * Ctor of {@link TooltipShape}.
     * <p>
     * The {@link Shape} is not destroyed via {@link #destroyTip(GL2ES2, RegionRenderer, Shape)},
     * if {@link DestroyCallback} {@code dtor} is not {@code null}, otherwise it is destroyed.
     * </p>
     * @param scale HUD tip scale for the tip shape
     * @param delayMS delay until HUD tip is visible after timer start (mouse moved)
     * @param tip HUD tip shape
     * @param dtor
     */
    public TooltipShape(final Vec4f backColor, final Vec4f borderColor, final float borderThickness,
                        final Vec2f scale, final long delayMS, final int renderModes, final Shape tip, final DestroyCallback dtor) {
        super(backColor, borderColor, delayMS, renderModes);
        this.tip = tip;
        this.scale = scale;
        this.borderThickness = borderThickness;
        this.dtorCallback = dtor;
    }

    @Override
    public Shape createTip(final GLAutoDrawable drawable, final Scene scene, final PMVMatrix4f pmv, final AABBox toolMvBounds) {
        final float zEps = scene.getZEpsilon(16);

        final float w = toolMvBounds.getWidth()*scale.x();
        final float h = toolMvBounds.getHeight()*scale.y();

        final Group g = new Group(new BoxLayout(w, h, Alignment.FillCenter));
        g.addShape(new Rectangle(renderModes, 1*w/h, 1, 0).setColor(backColor).setBorder(borderThickness).setBorderColor(frontColor).move(0, 0, -zEps));
        g.setName("TooltipShapeGroup");
        g.addShape(tip);
        g.setInteractive(false);

        final Vec2f pos = getTipMvPosition(scene, toolMvBounds, w, h);
        g.moveTo(pos.x(), pos.y(), 100*zEps);
        return g;
    }
    @Override
    public void destroyTip(final GL2ES2 gl, final RegionRenderer renderer, final Shape tipGroup) {
        if( null != dtorCallback ) {
            // Remove user tip from our layout group first and dtor our group
            // This allows the user to receive its own passed tip
            ((Group)tipGroup).removeShape(tip);
            tipGroup.destroy(gl, renderer);
            dtorCallback.destroyTip(gl, renderer, tip);
        } else {
            super.destroyTip(gl, renderer, tipGroup);
        }
    }
}
