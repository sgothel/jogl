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

import com.jogamp.common.os.Clock;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;

/** A HUD tooltip for {@link Shape}, see {@link Shape#setToolTip(Tooltip)}. */
public abstract class Tooltip {

    /** Default tooltip delay is {@value}ms */
    public static final long DEFAULT_DELAY = 1000;

    private final long delayMS;
    /** Delay t1, time to show tooltip, i.e. t0 + delayMS */
    private volatile long delayT1;
    /** Shape 'tool' owning this tooltip. */
    private Shape tool;
    protected final int renderModes;
    protected final Vec4f backColor = new Vec4f(1, 1, 0, 1);
    protected final Vec4f frontColor = new Vec4f(0.1f, 0.1f, 0.1f, 1);

    /**
     *
     * @param backColor optional HUD tip background color
     * @param frontColor optional HUD tip front color
     * @param delayMS delay until HUD tip is visible after timer start (mouse moved)
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     */
    protected Tooltip(final Vec4f backColor, final Vec4f frontColor, final long delayMS, final int renderModes) {
        this.delayMS = delayMS;
        this.delayT1 = 0;
        this.tool = null;
        this.renderModes = renderModes;
        if( null != backColor ) {
            this.backColor.set(backColor);
        }
        if( null != frontColor ) {
            this.frontColor.set(frontColor);
        }
    }
    /* pp */ final void setTool(final Shape tool) { this.tool = tool; }

    /** Returns {@link Shape} 'tool' owning this tooltip, set after {@link Shape#setToolTip(Tooltip)}. */
    public final Shape getTool() {
        return tool;
    }

    /** Stops the timer. */
    public final void stop() {
        this.delayT1 = 0;
    }

    /** Starts the timer. */
    public final void start() {
        this.delayT1 = Clock.currentMillis() + delayMS;
    }

    /**
     * Send tick to this tooltip
     * @return true if timer has been reached to {@link #createTip(PMVMatrix4f)}, otherwise false
     */
    public final boolean tick() {
        if( 0 == delayT1 ) {
            return false;
        }
        if( Clock.currentMillis() < delayT1 ) {
            return false;
        }
        this.delayT1 = 0;
        return true;
    }

    /** Little helper for {@link #createTip(GLAutoDrawable, Scene, PMVMatrix4f, AABBox)} returning the Mv {@link AABBox} of the tool within {@link Scene} Mv space. */
    public AABBox getToolMvBounds(final PMVMatrix4f pmv) {
        return getTool().getBounds().transform(pmv.getMv(), new AABBox());
    }
    /** Little helper for {@link #createTip(GLAutoDrawable, Scene, PMVMatrix4f, AABBox)} returning the Mv position of the tip within {@link Scene} Mv space. */
    public Vec2f getTipMvPosition(final Scene scene, final PMVMatrix4f pmv, final float tipWidth, final float tipHeight) {
        return getTipMvPosition(scene, getToolMvBounds(pmv), tipWidth, tipHeight);
    }
    /** Little helper for {@link #createTip(GLAutoDrawable, Scene, PMVMatrix4f, AABBox)} returning the Mv position of the tip within {@link Scene} Mv space. */
    public Vec2f getTipMvPosition(final Scene scene, final AABBox toolMvBounds, final float tipWidth, final float tipHeight) {
        final AABBox sceneAABox = scene.getBounds();
        final Vec2f pos = new Vec2f();
        if( toolMvBounds.getCenter().x() - tipWidth/2 >= sceneAABox.getLow().x() ) {
            pos.setX( toolMvBounds.getCenter().x()-tipWidth/2 );
        } else {
            pos.setX( sceneAABox.getLow().x() );
        }
        if( toolMvBounds.getHigh().y() + tipHeight <= sceneAABox.getHigh().y()  ) {
            pos.setY( toolMvBounds.getHigh().y() );
        } else if( toolMvBounds.getHigh().y() >= tipHeight ) {
            pos.setY( toolMvBounds.getHigh().y() - tipHeight );
        } else {
            pos.setY( sceneAABox.getHigh().y() - tipHeight );
        }
        return pos;
    }

    /**
     * Create a new HUD tip shape, usually called by {@link Scene}
     * @param drawable current {@link GLAutoDrawable}
     * @param scene the {@link Scene} caller for which this HUD tip shape is created
     * @param pmv {@link PMVMatrix4f}, which shall be properly initialized, e.g. via {@link Scene#setupMatrix(PMVMatrix4f)}
     * @param toolMvBounds TODO
     * @return newly created HUD tip shape
     * @see #destroyTip(GL2ES2, RegionRenderer, Shape)
     */
    public abstract Shape createTip(final GLAutoDrawable drawable, final Scene scene, final PMVMatrix4f pmv, AABBox toolMvBounds);

    /**
     * Destroy a {@link #createTip(GLAutoDrawable, Scene, PMVMatrix4f, AABBox) created} HUD tip.
     * <p>
     * Called after {@link Scene#removeShape(Shape)}, allowing implementation to perform certain
     * resource cleanup tasks. Even keeping the {@link Shape} tip alive is possible.
     * </p>
     * <p>
     * This default implementation simply calls {@link Shape#destroy(GL2ES2, RegionRenderer)}.
     * </p>
     * @param gl
     * @param renderer
     * @param tip
     * @see #createTip(GLAutoDrawable, Scene, PMVMatrix4f, AABBox)
     */
    public void destroyTip(final GL2ES2 gl, final RegionRenderer renderer, final Shape tip) {
        tip.destroy(gl, renderer);
    }

}