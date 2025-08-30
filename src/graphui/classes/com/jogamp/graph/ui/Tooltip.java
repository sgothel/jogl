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
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureSequence;

/** A HUD tooltip for {@link Shape}, see {@link Shape#setTooltip(Tooltip)}. */
public abstract class Tooltip {

    /** Default tooltip delay is {@value}ms */
    public static final long DEFAULT_DELAY = 1000;

    /** Delay in ms, zero implies no time based alarm */
    private final long delayMS;
    /** Alarm t1, time to show tooltip, i.e. t0 + delayMS, if delayMS > 0 */
    private volatile long alarmT1;
    /** Toggle for forced tooltip display */
    private volatile boolean forced;
    /** Shape 'tool' owning this tooltip. */
    private Shape tool;
    /** Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}. */
    protected final int renderModes;
    protected final Vec4f backColor = new Vec4f(1, 1, 1, 0.9f);
    protected final Vec4f frontColor = new Vec4f(0.2f, 0.2f, 0.2f, 1);

    @Override
    public String toString() {
        return "Tooltip[d "+delayMS+", next "+alarmT1+", forced "+forced+"]";
    }
    /**
     *
     * @param backColor optional HUD tip background color, if null a slightly transparent white background is used
     * @param frontColor optional HUD tip front color, if null an opaque almost-black is used
     * @param delayMS delay until HUD tip is visible after timer start (mouse moved), zero implies no time based alarm
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     */
    protected Tooltip(final Vec4f backColor, final Vec4f frontColor, final long delayMS, final int renderModes) {
        this.delayMS = delayMS;
        this.alarmT1 = 0;
        this.forced = false;
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

    /** Returns {@link Shape} 'tool' owning this tooltip, set after {@link Shape#setTooltip(Tooltip)}. */
    public final Shape getTool() {
        return tool;
    }

    /**
     * Stops the timer if not enforced via {@link #now()} or {@code clearForced} is true.
     * @param clearForced if true, also clears enforced flag set by {@link #now()}
     * @return true if timer has been stopped, otherwise false
     */
    public final boolean stop(final boolean clearForced) {
        if( clearForced ) {
            this.alarmT1 = 0;
            this.forced = false;
            return true;
        } else if( !this.forced ) {
            this.alarmT1 = 0;
            return true;
        } else {
            return false;
        }
    }

    /** Starts the timer. */
    public final void start() {
        if( !this.forced && delayMS > 0 ) {
            this.alarmT1 = Clock.currentMillis() + delayMS;
        }
    }

    /** Enforce tooltip display with next {@link #tick()}. */
    public final void now() {
        this.forced = true;
        this.alarmT1 = Clock.currentMillis() - 1;
    }

    /** Returns true if display is enforced via {@link #now()}. */
    public final boolean forced() {
        return forced;
    }

    /**
     * Send tick to this tooltip
     * @return true if {@link #start() started} timer has been reached or is enforced via {@link #now()} to {@link #createTip(PMVMatrix4f)}, otherwise false
     */
    public final boolean tick() {
        if( 0 == alarmT1 ) {
            return false;
        }
        if( Clock.currentMillis() < alarmT1 ) {
            return false;
        }
        this.alarmT1 = 0;
        this.forced = false;
        return true;
    }

    /**
     * Little helper for {@link #createTip(Scene, AABBox)} returning the Mv {@link AABBox} of the tool within {@link Scene} Mv space.
     * <p>
     * Method uses {@link #getTool()}
     * <pre>
     *   return getTool().getBounds().transform(pmv.getMv(), new AABBox());
     * </pre>
     * </p>
     */
    public AABBox getToolMvBounds(final PMVMatrix4f pmv) {
        return getTool().getBounds().transform(pmv.getMv(), new AABBox());
    }
    /** Little helper for {@link #createTip(Scene, AABBox)} returning the Mv position of the tip within {@link Scene} Mv space. */
    public Vec2f getTipMvPosition(final Scene scene, final PMVMatrix4f pmv, final float tipWidth, final float tipHeight) {
        return getTipMvPosition(scene, getToolMvBounds(pmv), tipWidth, tipHeight);
    }
    /** Little helper for {@link #createTip(Scene, AABBox)} returning the Mv position of the tip @ center within {@link Scene} Mv space. */
    public Vec2f getTipMvPosition(final Scene scene, final AABBox toolMvBounds, final float tipWidth, final float tipHeight) {
        final AABBox sceneAABox = scene.getBounds();
        final Vec2f pos = new Vec2f();
        if( toolMvBounds.getCenter().x() - tipWidth/2 < sceneAABox.getLow().x() ) {
            pos.setX( sceneAABox.getLow().x() );
        } else if( toolMvBounds.getCenter().x() + tipWidth/2 > sceneAABox.getHigh().x() ) {
            pos.setX( sceneAABox.getHigh().x() - tipWidth);
        } else {
            pos.setX( toolMvBounds.getCenter().x()-tipWidth/2 );
        }
        if( toolMvBounds.getCenter().y() + tipHeight <= sceneAABox.getHigh().y()  ) {
            pos.setY( toolMvBounds.getCenter().y() );
        } else if( toolMvBounds.getHigh().y() >= tipHeight ) {
            pos.setY( toolMvBounds.getHigh().y() - tipHeight );
        } else {
            pos.setY( sceneAABox.getHigh().y() - tipHeight );
        }
        return pos;
    }
    /** Little helper for {@link #createTip(Scene, AABBox)} returning the Mv position of the tip @ center within {@link Scene} Mv space. */
    public Vec2f getTipMvPosition(final Scene scene, final Vec3f toolMvPos, final float tipWidth, final float tipHeight) {
        final AABBox sceneAABox = scene.getBounds();
        final Vec2f pos = new Vec2f();
        if( toolMvPos.x() - tipWidth/2 < sceneAABox.getLow().x() ) {
            pos.setX( sceneAABox.getLow().x() );
        } else if( toolMvPos.x() + tipWidth/2 > sceneAABox.getHigh().x() ) {
            pos.setX( sceneAABox.getHigh().x() - tipWidth);
        } else {
            pos.setX( toolMvPos.x()-tipWidth/2 );
        }
        if( toolMvPos.y() + tipHeight <= sceneAABox.getHigh().y()  ) {
            pos.setY( toolMvPos.y() );
        } else {
            pos.setY( sceneAABox.getHigh().y() - tipHeight );
        }
        return pos;
    }

    /**
     * Create a new HUD tip shape, usually called by {@link Scene}
     * @param scene the {@link Scene} caller for which this HUD tip shape is created
     * @param toolMvBounds {@link AABBox} of the {@link #getTool()} in model-view (Mv) space of the given {@link Scene}
     * @return newly created HUD tip shape
     * @see #destroyTip(GL2ES2, RegionRenderer, Shape)
     */
    public abstract Shape createTip(final Scene scene, AABBox toolMvBounds);

    /**
     * Destroy a {@link #createTip(Scene, AABBox) created} HUD tip.
     * <p>
     * Called after {@link Scene#removeShape(Shape)}, allowing implementation to perform certain
     * resource cleanup tasks. Even keeping the {@link Shape} tip alive is possible.
     * </p>
     * <p>
     * This default implementation simply calls {@link Shape#destroy(GL2ES2, RegionRenderer)}.
     * </p>
     * @param gl current {@link GL2ES2}
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param tip created tip {@link Shape} via {@link #createTip(Scene, AABBox)}
     * @see #createTip(Scene, AABBox)
     */
    public void destroyTip(final GL2ES2 gl, final RegionRenderer renderer, final Shape tip) {
        tip.destroy(gl, renderer);
    }

}