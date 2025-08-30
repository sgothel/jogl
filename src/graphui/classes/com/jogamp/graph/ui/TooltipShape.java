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
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureSequence;

import jogamp.graph.ui.TreeTool;

/** A HUD {@link Shape} {@link Tooltip} for client {@link Shape}, see {@link Shape#setTooltip(Tooltip)}. */
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
     * @see TooltipShape#createTip(Scene, AABBox)
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
    private final Shape clientShape;
    private final Vec2f scale;
    private final float borderThickness;
    private final Padding padding;
    private final DestroyCallback dtorCallback;

    /**
     * Ctor of {@link TooltipShape}.
     * <p>
     * The tip {@link Shape} including the user provided {@code clientShape} will be destroyed via {@link #destroyTip(GL2ES2, RegionRenderer, Shape)},
     * since no {@link DestroyCallback} is being provided via {@link TooltipShape#TooltipShape(Vec2f, long, Shape, DestroyCallback)}.
     * </p>
     * @param scale HUD tip scale for the tip shape
     * @param delayMS delay until HUD tip is visible after timer start (mouse moved)
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param clientShape user/client {@link Shape} to be presented in the HUD tip
     */
    public TooltipShape(final Vec2f scale, final long delayMS, final int renderModes, final Shape clientShape) {
        this(null, null, 0, null, scale, delayMS, renderModes, clientShape, null);
    }

    /**
     * Ctor of {@link TooltipShape}.
     * <p>
     * The tip {@link Shape} will be destroyed via provided {@link DestroyCallback} {@code dtor} if not {@code null},
     * otherwise the default {@link Tooltip#destroyTip(GL2ES2, RegionRenderer, Shape)} gets called.
     * </p>
     * <p>
     * In case {@link DestroyCallback} {@code dtor} is being used, the user {@code clientShape}
     * is removed from internal layout shapes before they get destroyed and the single {@code clientShape}
     * gets passed to {@link DestroyCallback#destroy(TooltipShape, GL2ES2, RegionRenderer, Shape)}.
     * </p>
     * <p>
     * In case user provided {@code clientShape} is reused within a DAG,
     * the provided implementation shall do nothing, i.e. use {@link TooltipShape#NoOpDtor}.
     * </p>
     * @param backColor optional background color
     * @param borderColor optional border color
     * @param borderThickness border thickness
     * @param padding optional padding for the given {@code clientShape} for the internal wrapper group
     * @param scale scale for the HUD tip
     * @param delayMS delay until HUD tip is visible after timer start (mouse moved)
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param clientShape user/client {@link Shape} to be presented in the HUD tip
     * @param dtor optional {@link DestroyCallback}
     */
    public TooltipShape(final Vec4f backColor, final Vec4f borderColor, final float borderThickness,
                        final Padding padding, final Vec2f scale,
                        final long delayMS, final int renderModes, final Shape clientShape, final DestroyCallback dtor) {
        super(backColor, borderColor, delayMS, renderModes);
        this.clientShape = clientShape;
        this.scale = scale;
        this.borderThickness = borderThickness;
        this.padding = padding;
        this.dtorCallback = dtor;
    }

    public Shape getClientShape() { return this.clientShape; }

    @Override
    public Shape createTip(final Scene scene, final AABBox toolMvBounds) {
        final float zEps = scene.getZEpsilon(16);

        final float w = toolMvBounds.getWidth()*scale.x();
        final float h = toolMvBounds.getHeight()*scale.y();

        // tipWrapper ensures user 'clientShape' won't get mutated (scale, move) for DAG
        final Group tipWrapper = new Group("TTS.wrapper", null, null, clientShape);
        if( null != padding ) {
            tipWrapper.setPaddding(padding);
        }
        final Group tipGroup = new Group(new BoxLayout(w, h, Alignment.FillCenter));
        tipGroup.addShape(new Rectangle(renderModes, 1*w/h, 1, 0).setColor(backColor)
                          .setBorder(borderThickness).setBorderColor(frontColor)
                          .setName("TTS.frame").move(0, 0, -zEps));
        tipGroup.setName("TTS.group");
        tipGroup.addShape(tipWrapper);
        tipGroup.setInteractive(false);

        final Vec2f pos = getTipMvPosition(scene, toolMvBounds, w, h);
        tipGroup.moveTo(pos.x(), pos.y(), 100*zEps);
        return tipGroup;
    }

    /**
     * Removed the user provided client {@link Shape} from the {@link #createTip(Scene, AABBox) created} HUD {@code tipGroup},
     * i.e. {@link TooltipShape}'s layout {@link Group}.
     * <p>
     * This allows the user to release its own passed tip back, e.g. before destruction.
     * </p>
     * @param tip created tip {@link Shape} via {@link #createTip(Scene, AABBox)}
     * @return the user provided client {@link Shape}
     * @see #createTip(Scene, AABBox)
     */
    public Shape removeTip(final Shape tip) {
        final Shape cs = clientShape;
        if( null != cs ) {
            final Group tipGroup = (Group)tip;
            final Group tipWrapper = (Group)tipGroup.getShapeByIdx(1);
            if( null == tipWrapper.removeShape(cs) ) {
                System.err.println("TooltipShape.destroyTip: Warning: ClientShape "+cs.getName()+" not contained in "+tipWrapper.getName()+"; Internal Group: ");
                TreeTool.forAll(tipGroup, (final Shape s) -> {
                    System.err.println("- "+s.getName());
                    return false;
                });
            }
        }
        return cs;
    }

    @Override
    public void destroyTip(final GL2ES2 gl, final RegionRenderer renderer, final Shape tip) {
        if( null != dtorCallback ) {
            final Shape cs = removeTip(tip);
            tip.destroy(gl, renderer);
            dtorCallback.destroy(this, gl, renderer, cs);
        } else {
            super.destroyTip(gl, renderer, tip);
        }
    }
}
