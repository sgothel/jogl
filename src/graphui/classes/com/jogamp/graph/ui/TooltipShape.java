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
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.BoxLayout;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureSequence;

/** A HUD {@link Shape} {@link Tooltip} for {@link Shape}, see {@link Shape#setToolTip(Tooltip)}. */
public class TooltipShape extends Tooltip {
    /**
     * Optional HUD tip {@link #destroy(TooltipShape, GL2ES2, RegionRenderer, Shape) destroy callback}
     * for the user provided {@link Shape}, see {@link Tooltip#destroyTip(GL2ES2, RegionRenderer, Shape)}.
     * <p>
     * In case no callback is being set via {@link TooltipShape#TooltipShape(Vec2f, long, Shape, DestroyCallback)}
     * {@link Tooltip#destroyTip(GL2ES2, RegionRenderer, Shape)} destroys the shape.
     * Otherwise this callback gets invoked.
     * </p>
     * <p>
     * In case user provided {@code tip} is reused within a DAG,
     * the provided implementation shall do nothing, i.e. use {@link TooltipShape#NoOpDtor}.
     * </p>
     * @see TooltipShape#TooltipShape(Vec2f, long, Shape, DestroyCallback)
     * @see TooltipShape#createTip(GLAutoDrawable, Scene, PMVMatrix4f, AABBox)
     */
    public static interface DestroyCallback {
        /**
         * The custom destroy method of {@link DestroyCallback}
         * @param tts the {#link TooltipShape} instance
         * @param gl current {@link GL2ES2}
         * @param renderer used {@link RegionRenderer}
         * @param tip the user provided {@link Shape} as passed via {@link TooltipShape#TooltipShape(Vec4f, Vec4f, float, Vec2f, long, int, Shape, DestroyCallback)}.
         */
        public void destroy(TooltipShape tts, final GL2ES2 gl, final RegionRenderer renderer, final Shape tip);
    }
    /** No operation {@link DestroyCallback}, e.g. for a user provide {@code tip} {@link Shape}, reused within a DAG. */
    public static DestroyCallback NoOpDtor = new DestroyCallback() {
        @Override
        public void destroy(final TooltipShape tts, final GL2ES2 gl, final RegionRenderer renderer, final Shape tip) { }
    };

    /** Shape of this tooltip */
    private volatile Shape tip;
    private final Vec2f scale;
    private final float borderThickness;
    private final Padding padding;
    private final DestroyCallback dtorCallback;

    /**
     * Ctor of {@link TooltipShape}.
     * <p>
     * The {@link Shape} is destroyed via {@link #destroyTip(GL2ES2, RegionRenderer, Shape)},
     * since no {@link DestroyCallback} is being provided via {@link TooltipShape#TooltipShape(Vec2f, long, Shape, DestroyCallback)}.
     * </p>
     * @param scale HUD tip scale for the tip shape
     * @param delayMS delay until HUD tip is visible after timer start (mouse moved)
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param tip HUD tip shape
     */
    public TooltipShape(final Vec2f scale, final long delayMS, final int renderModes, final Shape tip) {
        this(null, null, 0, null, scale, delayMS, renderModes, tip, null);
    }

    /**
     * Ctor of {@link TooltipShape}.
     * <p>
     * The {@link Shape} is destroyed via provided {@link DestroyCallback} {@code dtor} if not {@code null},
     * otherwise the default {@link Tooltip#destroyTip(GL2ES2, RegionRenderer, Shape)} gets called.
     * </p>
     * <p>
     * In case {@link DestroyCallback} {@code dtor} is being used, the user {@code tip}
     * is removed from internal layout shapes before they get destroyed and the single {@code tip}
     * gets passed to {@link DestroyCallback#destroy(TooltipShape, GL2ES2, RegionRenderer, Shape)}.
     * </p>
     * <p>
     * In case user provided {@code tip} is reused within a DAG,
     * the provided implementation shall do nothing, i.e. use {@link TooltipShape#NoOpDtor}.
     * </p>
     * @param backColor optional background color
     * @param borderColor optional border color
     * @param borderThickness border thickness
     * @param padding optional padding for the given {@code tip} for the internal wrapper group
     * @param scale HUD tip scale for the tip shape
     * @param delayMS delay until HUD tip is visible after timer start (mouse moved)
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param tip HUD tip shape
     * @param dtor optional {@link DestroyCallback}
     */
    public TooltipShape(final Vec4f backColor, final Vec4f borderColor, final float borderThickness,
                        final Padding padding, final Vec2f scale,
                        final long delayMS, final int renderModes, final Shape tip, final DestroyCallback dtor) {
        super(backColor, borderColor, delayMS, renderModes);
        this.tip = tip;
        this.scale = scale;
        this.borderThickness = borderThickness;
        this.padding = padding;
        this.dtorCallback = dtor;
    }

    public void setTip(final Shape tip) {
        this.tip = tip;
    }

    @Override
    public Shape createTip(final GLAutoDrawable drawable, final Scene scene, final PMVMatrix4f pmv, final AABBox toolMvBounds) {
        final float zEps = scene.getZEpsilon(16);

        final float w = toolMvBounds.getWidth()*scale.x();
        final float h = toolMvBounds.getHeight()*scale.y();

        // tipWrapper ensures user 'tip' shape won't get mutated (scale, move) for DAG
        final Group tipWrapper = new Group("TTSWrapper", null, null, tip);
        if( null != padding ) {
            tipWrapper.setPaddding(padding);
        }
        final Group tipGroup = new Group(new BoxLayout(w, h, Alignment.FillCenter));
        tipGroup.addShape(new Rectangle(renderModes, 1*w/h, 1, 0).setColor(backColor).setBorder(borderThickness).setBorderColor(frontColor).move(0, 0, -zEps));
        tipGroup.setName("TTSGroup");
        tipGroup.addShape(tipWrapper);
        tipGroup.setInteractive(false);

        final Vec2f pos = getTipMvPosition(scene, toolMvBounds, w, h);
        tipGroup.moveTo(pos.x(), pos.y(), 100*zEps);
        return tipGroup;
    }
    @Override
    public void destroyTip(final GL2ES2 gl, final RegionRenderer renderer, final Shape tipGroup_) {
        if( null != dtorCallback ) {
            // Remove user tip from our layout group first and dtor our group
            // This allows the user to receive its own passed tip
            final Group tipGroup = (Group)tipGroup_;
            final Group tipWrapper = (Group)tipGroup.getShapeByIdx(1);
            if( null == tipWrapper.removeShape(tip) ) {
                System.err.println("TooltipShape.destroyTip: Warning: Tip "+tip.getName()+" not contained in "+tipWrapper.getName()+"; Internal Group: ");
                tipGroup.forAll((final Shape s) -> {
                    System.err.println("- "+s.getName());
                    return false;
                });
            }
            tipGroup.destroy(gl, renderer);
            dtorCallback.destroy(this, gl, renderer, tip);
        } else {
            super.destroyTip(gl, renderer, tipGroup_);
        }
    }
}
