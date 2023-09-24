/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.jogamp.common.os.Clock;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.Font.Glyph;
import com.jogamp.graph.ui.Group.Layout;
import com.jogamp.graph.ui.shapes.GlyphShape;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Quaternion;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.plane.AffineTransform;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

/**
 * Group of animated {@link Shape}s including other static {@link Shape}s, optionally utilizing a {@link Group.Layout}.
 * @see Scene
 * @see Shape
 * @see Group.Layout
 */
public class AnimGroup extends Group {
    private static final boolean DEBUG = false;
    /** Epsilon of position, 5000 x {@link FloatUtil#EPSILON} */
    public static final float POS_EPS = FloatUtil.EPSILON * 5000; // ~= 0.0005960
    /** Epsilon of rotation [radian], 0.5 degrees or 0.008726646 radians */
    public static final float ROT_EPS = FloatUtil.adegToRad(0.5f); // 1 adeg ~= 0.01745 rad

    private volatile long tstart_us = 0;
    private volatile long tlast_us = 0;
    private volatile long tpause_us = 0;
    private volatile boolean tickOnDraw = true;
    private volatile boolean tickPaused = false;
    private long frame_count = 0;

    /** Animation {@link Shapes} data covering one {@link Shape} of {@link Set}. */
    public static final class ShapeData {
        /** Indicator whether the {@link Shapes} is animating or not. */
        public boolean active;
        /** {@link Shapes} scaled start position */
        public final Vec3f startPos;
        /** {@link Shapes} scaled target position */
        public final Vec3f targetPos;
        /** The {@link Shapes} */
        public final Shape shape;
        /** Optional user attachment per {@link Shape} to be used within {@link LerpFunc}. */
        public Object user;

        /** New instance with set {@link Shape} using its scaled {@link Shape#getPosition()} for {@link #startPos} and {@link #targetPos}. */
        public ShapeData(final Shape s) {
            active = true;
            startPos = new Vec3f( s.getPosition() );
            targetPos = startPos.copy();
            shape = s;
            user = null;
        }
    }

    /** Animation-Set covering its {@link ShapeData} elements, {@link LerpFunc} and animation parameter. */
    public static final class Set {
        /** Pixel per millimeter */
        public final float pixPerMM;
        /** Pixel per shape unit */
        public final Vec2f pixPerShapeUnit;
        /** Reference {@link Shape} giving reference size */
        public final Shape refShape;

        /** Translation acceleration in [m]/[s*s] */
        public final float accel;
        /** Translation acceleration in [shapeUnit]/[s*s] */
        public final float accel_obj;
        /** Start translation velocity in [m]/[s] */
        public final float start_velocity;
        /** Start translation velocity in [shapeUnit]/[s] */
        public final float start_velocity_obj;
        /** Current translation velocity in [m]/[s] */
        public float velocity;
        /** Current translation velocity in [shapeUnit]/[s] */
        public float velocity_obj;

        /** Angular acceleration in [radians]/[s*s] */
        public final float ang_accel;
        /** Start angular velocity in [radians]/[s] */
        public final float start_ang_velo;
        /** Current angular velocity in [radians]/[s] */
        public float ang_velo;

        /** {@link LerpFunc} function */
        public final LerpFunc lerp;

        /** All {@link Shape}s wrapped within {@link ShapeData}. */
        public final List<ShapeData> allShapes;

        /** Unscaled bounds of {@link #allShapes} at their original position, size and rotation. */
        public final AABBox sourceBounds;

        private Set(final float pixPerMM, final float[/*2*/] pixPerShapeUnit, final Shape refShape,
                        final float accel, final float velocity,
                        final float ang_accel, final float ang_velo,
                        final List<ShapeData> allShapes, final AABBox sourceBounds,
                        final LerpFunc lerp) {
            this.pixPerMM = pixPerMM;
            this.pixPerShapeUnit = new Vec2f( pixPerShapeUnit );
            this.refShape = refShape;
            this.accel = accel;
            this.start_velocity = velocity;
            this.velocity = velocity;
            {
                final float accel_px = accel * 1e3f * pixPerMM; // [px]/[s*s]
                this.accel_obj = accel_px / this.pixPerShapeUnit.x(); // [shapeUnit]/[s*s]

                final float velocity_px = velocity * 1e3f * pixPerMM; // [px]/[s]
                this.start_velocity_obj = velocity_px / this.pixPerShapeUnit.x(); // [shapeUnit]/[s]
                this.velocity_obj = this.start_velocity_obj;
            }
            this.ang_accel = ang_accel;
            this.start_ang_velo = ang_velo;
            this.ang_velo = ang_velo;
            this.lerp = lerp;
            this.allShapes = allShapes;
            this.sourceBounds = sourceBounds;
        }

        /**
         * Adds given {@link Shape} to this {@link Set} and its {@link AnimGroup} wrapping it in {@link ShapeData}.
         * <p>
         * Also issues {@link ShapeSetup#setup(Set, int, ShapeData)}.
         * </p>
         * @return newly created {@link ShapeData}
         */
        public ShapeData addShape(final AnimGroup g, final Shape s, final ShapeSetup op) {
            final ShapeData sd = new ShapeData(s);
            final int idx = this.allShapes.size();
            this.allShapes.add( sd );
            this.sourceBounds.resize(sd.shape.getBounds());
            op.setup(this, idx, sd);
            g.addShape(sd.shape);
            return sd;
        }

        /**
         * Removes given {@link ShapeData} from this {@link Set} and its {@link AnimGroup}.
         * <p>
         * Also destroys the {@link ShapeData}, including its {@link ShapeData} and their {@link Shape}.
         * </p>
         */
        public void removeShape(final AnimGroup g, final GL2ES2 gl, final RegionRenderer renderer, final ShapeData sd) {
            g.removeShape(gl, renderer, sd.shape);
            sd.active = false;
            allShapes.remove(sd);
        }

        /**
         * Removes all {@link ShapeData} from this {@link Set} and its {@link AnimGroup}.
         * <p>
         * Also destroys the {@link ShapeData}, including its {@link ShapeData} and their {@link Shape}.
         * </p>
         */
        public void removeShapes(final AnimGroup g, final GL2ES2 gl, final RegionRenderer renderer) {
            for(final ShapeData sd : allShapes) {
                g.removeShape(gl, renderer, sd.shape);
                sd.active = false;
            }
            allShapes.clear();
        }

        /** Removes this {@link Set} from its {@link AnimGroup} and destroys it, including its {@link ShapeData} and their {@link Shape}. */
        private void remove(final AnimGroup g, final GL2ES2 gl, final RegionRenderer renderer) {
            removeShapes(g, gl, renderer);
            refShape.destroy(gl, renderer);
        }

        public void setAnimationActive(final boolean v) {
            for(final ShapeData sd : allShapes) {
                sd.active = v;
            }
        }
        public boolean isAnimationActive() {
            for(final ShapeData sd : allShapes) {
                if( sd.active ) { return true; }
            }
            return false;
        }
    }
    private final List<Set> animSets = new ArrayList<Set>();

    /**
     * Create a group of animated {@link Shape}s including other static {@link Shape}s w/ given {@link Group.Layout}.
     * <p>
     * Default is non-interactive, see {@link #setInteractive(boolean)}.
     * </p>
     * @param l optional {@link Layout}, maybe {@code null}
     */
    public AnimGroup(final Layout l) {
        super(l);
    }

    /** Return the {@link Set} at given index or {@code null} if n/a. */
    public Set getAnimSet(final int idx) {
        if( idx < animSets.size() ) {
            return animSets.get(idx);
        }
        return null;
    }

    /** Removes all {@link Set}s and destroys them, including all {@link ShapeData} and their {@link Shape}s. */
    public final void removeAllAnimSets(final GL2ES2 gl, final RegionRenderer renderer) {
        for(final Set as : animSets) {
            as.remove(this, gl, renderer);
        }
        animSets.clear();
    }

    /** Removes the given {@link Set} and destroys it, including its {@link ShapeData} and {@link Shape}. */
    public final void removeAnimSet(final GL2ES2 gl, final RegionRenderer renderer, final Set as) {
        if( null != as ) {
            as.remove(this, gl, renderer);
            animSets.remove(as);
        }
    }

    /** Removes the given {@link Set}s and destroys them, including their {@link ShapeData} and {@link Shape}. */
    public final void removeAnimSets(final GL2ES2 gl, final RegionRenderer renderer, final List<Set> asList) {
        for(final Set as : asList) {
            if( null != as ) {
                as.remove(this, gl, renderer);
                animSets.remove(as);
            }
        }
    }

    /**
     * {@link ShapeData} setup function for animation using its enclosing {@link Set} and other data points
     * <p>
     * At minimum, {@link ShapeData}'s {@link ShapeData#startPos} and {@link ShapeData#targetPos} shall be adjusted.
     * </p>
     */
    public static interface ShapeSetup {
        /**
         * Setting up the {@link ShapeData} for animation using its enclosing {@link Set} and other data points
         * @param as {@link Set} of the animation
         * @param idx {@link ShapeData} index within the {@link Set#allShapes}
         * @param sd the {@link ShapeData} matching {@code idx} containing the {@link Shape} to apply this operation
         */
        public void setup(final Set as, final int idx, final ShapeData sd);
    }

    /**
     * Linear interpolation (LERP) function to evaluate the next animated frame for each {@link ShapeData} of a {@link Set}.
     * @see AnimGroup.TargetLerp
     */
    public static interface LerpFunc {
        /**
         * Evaluate next LERP step for the given {@link ShapeData} within the animation {@link Set}.
         * @param frame_cnt frame count for the given {@link ShapeData}
         * @param as {@link Set} of the animation
         * @param idx {@link ShapeData} index within the {@link Set#allShapes}
         * @param sd the {@link ShapeData} matching {@code idx} containing the {@link Shape} to apply this operation
         * @param at_s time delta to animation start, i.e. animation duration [s]
         * @param dt_s time delta to last call [s]
         * @return true if target animation shall continue, false otherwise
         */
        public boolean eval(long frame_cnt, Set as, final int idx, ShapeData sd, float at_s, float dt_s);
    }

    /**
     * Add a new {@link Set} with an empty {@link ShapeData} container.
     * <p>
     * The given {@link PMVMatrix4f} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * only, without a shape's {@link #setTransformMv(PMVMatrix4f)}. See {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti)}.
     * </p>
     * @param pixPerMM monitor pixel per millimeter for accurate animation
     * @param glp used {@link GLProfile}
     * @param pmv well formed {@link PMVMatrix4f}, e.g. could have been setup via {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti)}.
     * @param viewport the int[4] viewport
     * @param accel translation acceleration in [m]/[s*s]
     * @param velocity translation velocity in [m]/[s]
     * @param ang_accel angular acceleration in [radians]/[s*s], usable for rotation etc
     * @param ang_velo angular velocity in [radians]/[s], usable for rotation etc
     * @param lerp {@link LerpFunc} function, see {@link AnimGroup.TargetLerp}
     * @param refShape reference {@link Shape} giving reference size, see {@link #refShape}
     * @param op {@link ShapeData} setup function for {@link ShapeData#startPos} and {@link ShapeData#targetPos}
     * @return a new {@link Set} instance
     */
    public Set addAnimSet(final float pixPerMM,
                          final GLProfile glp, final PMVMatrix4f pmv, final Recti viewport,
                          final float accel, final float velocity,
                          final float ang_accel, final float ang_velo,
                          final LerpFunc lerp, final Shape refShape)
    {
        final Set as;
        refShape.validate(glp);
        pmv.pushMv();
        {
            refShape.setTransformMv(pmv);
            as = new Set(pixPerMM, refShape.getPixelPerShapeUnit(pmv, viewport, new float[2]), refShape,
                              accel, velocity, ang_accel, ang_velo,
                              new ArrayList<ShapeData>(), new AABBox(), lerp);
        }
        pmv.popMv();
        animSets.add(as);
        return as;
    }

    /**
     * Add a new {@link Set} with {@link ShapeData} for each {@link GlyphShape}, moving towards its target position
     * using a generic displacement via {@link ShapeSetup} to determine each {@link ShapeData}'s starting position.
     * <p>
     * The given {@link PMVMatrix4f} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * only, without a shape's {@link #setTransformMv(PMVMatrix4f)}. See {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti)}.
     * </p>
     * @param pixPerMM monitor pixel per millimeter for accurate animation
     * @param glp used {@link GLProfile}
     * @param pmv well formed {@link PMVMatrix4f}, e.g. could have been setup via {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti)}.
     * @param viewport the int[4] viewport
     * @param renderModes used {@link GLRegion#create(GLProfile, int, com.jogamp.opengl.util.texture.TextureSequence) region render-modes}
     * @param font {@link Font} to be used for resulting {@link GlyphShape}s
     * @param refChar reference character to calculate the reference {@link GlyphShape}
     * @param text the text for resulting {@link GlyphShape}s
     * @param fontScale font scale factor for resulting {@link GlyphShape}s
     * @param accel translation acceleration in [m]/[s*s]
     * @param velocity translation velocity in [m]/[s]
     * @param ang_accel angular acceleration in [radians]/[s*s], usable for rotation etc
     * @param ang_velo angular velocity in [radians]/[s], usable for rotation etc
     * @param lerp {@link LerpFunc} function, see {@link AnimGroup.TargetLerp}
     * @param op {@link ShapeData} setup function for {@link ShapeData#startPos} and {@link ShapeData#targetPos}
     * @return newly created and added {@link Set}
     */
    public final Set addGlyphSet(final float pixPerMM,
                                 final GLProfile glp, final PMVMatrix4f pmv, final Recti viewport, final int renderModes,
                                 final Font font, final char refChar, final CharSequence text, final float fontScale,
                                 final float accel, final float velocity, final float ang_accel, final float ang_velo,
                                 final LerpFunc lerp, final ShapeSetup op)
    {
        final Set as;
        {
            final List<ShapeData> allShapes = new ArrayList<ShapeData>();
            final AABBox sourceBounds = processString(allShapes, renderModes, font, fontScale, text);
            final GlyphShape refShape = new GlyphShape(renderModes, font, refChar, 0, 0);
            refShape.setScale(fontScale, fontScale, 1f);
            refShape.validate(glp);
            pmv.pushMv();
            {
                refShape.setTransformMv(pmv);
                as = new Set(pixPerMM, refShape.getPixelPerShapeUnit(pmv, viewport, new float[2]), refShape,
                                 accel, velocity, ang_accel, ang_velo, allShapes, sourceBounds, lerp);
            }
            pmv.popMv();
        }
        animSets.add(as);

        for (int idx = 0; idx < as.allShapes.size(); ++idx) {
            final ShapeData sd = as.allShapes.get(idx);
            op.setup(as, idx, sd);
            super.addShape(sd.shape);
        }
        if( DEBUG ) {
            System.err.println("addAnimShapes: AnimSet.sourceBounds = "+as.sourceBounds);
        }
        resetAnimation();
        return as;
    }
    private static final AABBox processString(final List<ShapeData> res, final int renderModes,
                                              final Font font, final float fontScale, final CharSequence text)
    {
        final Font.GlyphVisitor fgv = new Font.GlyphVisitor() {
            @Override
            public void visit(final Glyph glyph, final AffineTransform t) {
                if( !glyph.isNonContour() ) {
                    final GlyphShape gs = new GlyphShape(renderModes, glyph, t.getTranslateX(), t.getTranslateY());
                    gs.setScale(fontScale, fontScale, 1f);
                    gs.moveTo(gs.getOrigPos().x()*fontScale, gs.getOrigPos().y()*fontScale, gs.getOrigPos().z());
                    res.add( new ShapeData( gs ) );
                }
            }
        };
        return font.processString(fgv, null, text, new AffineTransform(), new AffineTransform());
    }

    /**
     * Add a new {@link Set} with {@link ShapeData} for each {@link GlyphShape}, moving towards its target position
     * using a fixed displacement function, defining each {@link ShapeData}'s starting position.
     * <p>
     * The start-position is randomly chosen within given {@link AABBox} glyphBox.
     * </p>
     * <p>
     * The given {@link PMVMatrix4f} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * only, without a shape's {@link #setTransformMv(PMVMatrix4f)}. See {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti)}.
     * </p>
     * @param pixPerMM monitor pixel per millimeter for accurate animation
     * @param glp used {@link GLProfile}
     * @param pmv well formed {@link PMVMatrix4f}, e.g. could have been setup via {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti)}.
     * @param viewport the int[4] viewport
     * @param renderModes used {@link GLRegion#create(GLProfile, int, com.jogamp.opengl.util.texture.TextureSequence) region render-modes}
     * @param font {@link Font} to be used for resulting {@link GlyphShape}s
     * @param text the text for resulting {@link GlyphShape}s
     * @param fontScale font scale factor for resulting {@link GlyphShape}s
     * @param fgCol foreground color for resulting {@link GlyphShape}s
     * @param accel translation acceleration in [m]/[s*s]
     * @param velocity translation velocity in [m]/[s]
     * @param ang_accel angular acceleration in [radians]/[s*s], usable for rotation etc
     * @param ang_velo angular velocity in [radians]/[s], usable for rotation etc
     * @param animBox {@link AABBox} denoting the maximum extend of {@link ShapeData}s start-position, also used for their x-offset
     * @param z_only Pass true for z-only distance
     * @param random the random float generator
     * @param lerp {@link LerpFunc} function, see {@link AnimGroup.TargetLerp}
     * @return newly created and added {@link Set}
     */
    public final Set addGlyphSetRandom01(final float pixPerMM,
                                             final GLProfile glp, final PMVMatrix4f pmv, final Recti viewport, final int renderModes,
                                             final Font font, final CharSequence text, final float fontScale, final Vec4f fgCol,
                                             final float accel, final float velocity, final float ang_accel, final float ang_velo,
                                             final AABBox animBox, final boolean z_only, final Random random, final LerpFunc lerp)
    {
        return addGlyphSet(pixPerMM, glp, pmv, viewport, renderModes, font, 'X', text, fontScale,
                       accel, velocity, ang_accel, ang_velo, lerp, (final Set as, final int idx, final ShapeData sd) -> {
            sd.shape.setColor(fgCol);

            // shift targetPost to glyphBox.getMinX()
            sd.targetPos.add(animBox.getMinX(), 0f, 0f);

            final Vec3f target = sd.targetPos;

            sd.startPos.set( z_only ? target.x() : animBox.getMinX() + random.nextFloat() * animBox.getWidth(),
                             z_only ? target.y() : animBox.getMinY() + random.nextFloat() * animBox.getHeight(),
                             0f + random.nextFloat() * animBox.getHeight() * 1f);
            sd.shape.moveTo(sd.startPos);
       } );
    }

    /**
     * Add a new {@link Set} with {@link ShapeData} for each {@link GlyphShape}, implementing<br/>
     * horizontal continuous scrolling while repeating the given {@code text}.
     * <p>
     * The given {@link PMVMatrix4f} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * only, without a shape's {@link #setTransformMv(PMVMatrix4f)}. See {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti)}.
     * </p>
     * @param pixPerMM monitor pixel per millimeter for accurate animation
     * @param glp used {@link GLProfile}
     * @param pmv well formed {@link PMVMatrix4f}, e.g. could have been setup via {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti)}.
     * @param viewport the int[4] viewport
     * @param renderModes used {@link GLRegion#create(GLProfile, int, com.jogamp.opengl.util.texture.TextureSequence) region render-modes}
     * @param font {@link Font} to be used for resulting {@link GlyphShape}s
     * @param text the text for resulting {@link GlyphShape}s
     * @param fontScale font scale factor for resulting {@link GlyphShape}s
     * @param fgCol foreground color for resulting {@link GlyphShape}s
     * @param velocity translation velocity in [m]/[s]
     * @param animBox {@link AABBox} denoting the maximum extend of {@link ShapeData}s start-position, also used for their x-offset
     * @return newly created and added {@link Set}
     */
    public final Set addGlyphSetHorizScroll01(final float pixPerMM,
                                              final GLProfile glp, final PMVMatrix4f pmv, final Recti viewport, final int renderModes,
                                              final Font font, final CharSequence text, final float fontScale, final Vec4f fgCol,
                                              final float velocity, final AABBox animBox, final float y_offset)
    {
        return addGlyphSet(pixPerMM, glp, pmv, viewport,
                renderModes, font, 'X', text, fontScale,
                0f /* accel */, velocity, 0f /* ang_accel */, 0f /* 1-rotation/s */,
                new AnimGroup.ScrollLerp(animBox),
                (final AnimGroup.Set as, final int idx, final AnimGroup.ShapeData sd) -> {
                    sd.shape.setColor(fgCol);

                    sd.targetPos.set(animBox.getMinX(), y_offset, 0);

                    sd.startPos.set( sd.startPos.x() + animBox.getMaxX(), sd.targetPos.y(), sd.targetPos.z());

                    sd.shape.moveTo( sd.startPos );
                } );
    }

    /** Sets whether {@link #tick()} shall be automatic issued on {@link #draw(GL2ES2, RegionRenderer, int[])}, default is {@code true}. */
    public final void setTickOnDraw(final boolean v) { tickOnDraw = v; }
    public final boolean getTickOnDraw() { return tickOnDraw; }

    /**
     * Sets whether {@link #tick()} shall be paused, default is {@code false}.
     * <p>
     * Unpausing {@link #tick()} will also forward animation start-time about paused duration,
     * as well as set last-tick timestamp to now. This prevents animation artifacts and resumes where left off.
     * </p>
     */
    public final void setTickPaused(final boolean v) {
        if( tickPaused == v ) {
            return;
        }
        if( v ) {
            tickPaused = true;
            tpause_us = Clock.currentNanos() / 1000; // [us]
        } else {
            final long tnow_us = Clock.currentNanos() / 1000; // [us]
            final long dtP_us = tnow_us - tpause_us;
            tstart_us += dtP_us;
            tlast_us += dtP_us;
            tickPaused = false;
        }
    }
    public final boolean getTickPaused() { return tickPaused; }

    @Override
    public void draw(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount) {
        if( tickOnDraw && !tickPaused) {
            tickImpl();
        }
        super.draw(gl, renderer, sampleCount);
    }

    public final void resetAnimation() {
        tstart_us = Clock.currentNanos() / 1000; // [us]
        tlast_us = tstart_us;
        frame_count = 0;
    }

    public final void restartAnimation() {
        super.runSynced( () -> {
            for(final Set as : animSets) {
                as.setAnimationActive(true);
            } } );
        resetAnimation();
    }

    public void stopAnimation() {
        super.runSynced( () -> {
            for(final Set as : animSets) {
                as.setAnimationActive(false);
            } } );
    }

    public final boolean isAnimationActive() {
        for(final Set as : animSets) {
            if( as.isAnimationActive() ) { return true; }
        }
        return false;
    }

    /**
     * Issues an animation tick, usually done at {@link #draw(GL2ES2, RegionRenderer, int[])}.
     * @see #setTickOnDraw(boolean)
     * @see #setTickPaused(boolean)
     */
    public final void tick() {
        if( !tickPaused ) {
            super.runSynced( () -> { tickImpl(); } );
        }
    }
    private final void tickImpl() {
        final long tnow_us = Clock.currentNanos() / 1000;
        final float at_s = (tnow_us - tstart_us) / 1e6f;
        final float dt_s = (tnow_us - tlast_us) / 1e6f;
        tlast_us = tnow_us;
        for(final Set as : animSets) {
            if( as.isAnimationActive() ) {
                if( !FloatUtil.isZero( as.accel ) ) {
                    as.velocity += as.accel * dt_s; // [shapeUnit]/[s]
                    as.velocity_obj += as.accel_obj * dt_s; // [shapeUnit]/[s]
                }
                if( !FloatUtil.isZero( as.ang_accel ) ) {
                    as.ang_velo += as.ang_accel * dt_s; // [radians]/[s]
                }
                for (int idx = 0; idx < as.allShapes.size(); ++idx) {
                    final ShapeData sd = as.allShapes.get(idx);
                    if( !as.lerp.eval(frame_count, as, idx, sd, at_s, dt_s) ) {
                        sd.active = false;
                    }
                }
            }
        }
        ++frame_count;
    }

    /**
     * Default target {@link LerpFunc}, approaching {@link ShapeData}'s target position inclusive angular rotation around given normalized axis.
     * <p>
     * Implementation uses the current shape position and time delta since last call,
     * hence allows rugged utilization even if shapes are dragged around.
     * </p>
     */
    public static class TargetLerp implements LerpFunc {
        final Vec3f rotAxis;
        /**
         * New target {@link LerpFunc} instance
         * @param rotAxis normalized axis vector for {@link Quaternion#rotateByAngleNormalAxis(float, Vec3f)}
         */
        public TargetLerp(final Vec3f rotAxis) {
            this.rotAxis = rotAxis;
        }
        @Override
        public boolean eval(final long frame_cnt, final Set as, final int idx, final ShapeData sd, final float at_s, final float dt_s) {
            final float dxy = as.velocity_obj * dt_s; // [shapeUnit]
            final float rot_step = as.ang_velo * dt_s; // [radians]
            final float shapeScale = sd.shape.getScale().y();
            final Vec3f pos = sd.shape.getPosition().copy();
            final Vec3f p_t = sd.targetPos.minus(pos);
            final float p_t_diff = p_t.length();
            final Quaternion q = sd.shape.getRotation();
            final Vec3f euler = q.toEuler(new Vec3f());
            final float rotAng = euler.length();
            final float rotAngDiff = Math.min(Math.abs(rotAng), FloatUtil.TWO_PI - Math.abs(rotAng));
            final boolean pos_ok = p_t_diff <= AnimGroup.POS_EPS;
            final boolean pos_near = pos_ok || p_t_diff <= sd.shape.getBounds().getSize() * shapeScale * 2f;
            final boolean rot_ok = pos_near && ( rot_step < AnimGroup.ROT_EPS || rotAngDiff <= AnimGroup.ROT_EPS || rotAngDiff <= rot_step * 2f );
            if ( pos_ok && rot_ok ) {
                // arrived
                if( DEBUG ) {
                    if( 0 == idx ) {
                        System.err.println("F: dt "+(dt_s*1000f)+" ms, p_t[OK "+pos_ok+", near "+pos_near+", diff: "+p_t_diff+", dxy "+dxy+"], rot[OK "+rot_ok+", radY "+rotAng+" ("+FloatUtil.radToADeg(rotAng)+"), diff "+rotAngDiff+" ("+FloatUtil.radToADeg(rotAngDiff)+"), ang_velo "+as.ang_velo+", step "+rot_step+" ("+FloatUtil.radToADeg(rot_step)+")]");
                    }
                }
                sd.shape.moveTo(sd.targetPos);
                q.setIdentity();
                sd.shape.setInteractive(false);
                return false;
            }
            if( !pos_ok ) {
                if( DEBUG ) {
                    if( 0 == idx ) {
                        System.err.println("P: dt "+(dt_s*1000f)+" ms, p_t[OK "+pos_ok+", near "+pos_near+", diff: "+p_t_diff+", dxy "+dxy+"], rot[OK "+rot_ok+", radY "+rotAng+" ("+FloatUtil.radToADeg(rotAng)+"), diff "+rotAngDiff+" ("+FloatUtil.radToADeg(rotAngDiff)+"), ang_velo "+as.ang_velo+", step "+rot_step+" ("+FloatUtil.radToADeg(rot_step)+")]");
                    }
                }
                if( p_t_diff <= dxy || p_t_diff <= AnimGroup.POS_EPS ) {
                    sd.shape.moveTo(sd.targetPos);
                } else {
                    pos.add( p_t.normalize().scale( dxy ) );
                    sd.shape.moveTo(pos);
                }
                if( !rot_ok ) {
                    if( pos_near ) {
                        q.rotateByAngleNormalAxis( rot_step * 2f, rotAxis );
                    } else {
                        q.rotateByAngleNormalAxis( rot_step, rotAxis );
                    }
                }
            } else {
                if( DEBUG ) {
                    if( 0 == idx ) {
                        System.err.println("p: dt "+(dt_s*1000f)+" ms, p_t[OK "+pos_ok+", near "+pos_near+", diff: "+p_t_diff+", dxy "+dxy+"], rot[OK "+rot_ok+", radY "+rotAng+" ("+FloatUtil.radToADeg(rotAng)+"), diff "+rotAngDiff+" ("+FloatUtil.radToADeg(rotAngDiff)+"), ang_velo "+as.ang_velo+", step "+rot_step+" ("+FloatUtil.radToADeg(rot_step)+")]");
                    }
                }
                if( rot_ok || rotAngDiff <= rot_step * 3f ) {
                    q.setIdentity();
                } else {
                    q.rotateByAngleNormalAxis( rot_step * 3f, rotAxis );
                }
            }
            return true;
        }
    };

    /**
     * Scrolling {@link LerpFunc}, approaching {@link ShapeData}'s target position over and over.
     * <p>
     * Implementation uses the current shape position and time delta since last call,
     * hence allows rugged utilization even if shapes are dragged around.
     * </p>
     */
    public static class ScrollLerp implements LerpFunc {
        final AABBox clip;
        /**
         * New scroller {@link LerpFunc} instance
         * @param clip clipping box for each shape
         */
        public ScrollLerp(final AABBox clip) {
            this.clip = clip;
        }
        @Override
        public boolean eval(final long frame_cnt, final Set as, final int idx, final ShapeData sd, final float at_s, final float dt_s) {
            final float dxy = as.velocity_obj * dt_s; // [shapeUnit]
            final Vec3f pos = sd.shape.getPosition().copy();
            final Vec3f p_t = sd.targetPos.minus(pos);
            final float p_t_diff = p_t.length();
            final boolean pos_ok = p_t_diff <= dxy || p_t_diff <= AnimGroup.POS_EPS;
            if ( pos_ok ) {
                // arrived -> restart
                if( 0 == idx ) {
                    as.velocity = as.start_velocity;
                    as.velocity_obj = as.start_velocity_obj;
                    as.ang_velo = as.start_ang_velo;
                    final ShapeData sd_last = as.allShapes.get(as.allShapes.size()-1);
                    final Vec3f v_thisstart_lastpos = sd_last.shape.getPosition().minus( sd.startPos );
                    final float angle_thisstart_lastpos = Vec3f.UNIT_X.angle(v_thisstart_lastpos);
                    if( angle_thisstart_lastpos >= FloatUtil.HALF_PI ) {
                        // start position of this is 'right of' current position of last: short shape-string case
                        pos.set( sd.startPos );
                    } else {
                        // start position of this is 'left of' current position of last: long shape-string case
                        pos.set( sd_last.shape.getPosition() ).add( Vec3f.UNIT_X.mul( sd_last.shape.getScaledWidth() * 2f ) );
                    }
                    // System.err.println("Scroll-0: idx "+idx+", this "+sd.shape.getPosition()+", lst "+sd_last.shape.getPosition()+", angle "+angle_thisstart_lastpos+" rad ("+FloatUtil.radToADeg(angle_thisstart_lastpos)+" deg) -> "+pos);
                } else {
                    final ShapeData sd_pre = as.allShapes.get(idx-1);
                    final Vec3f diff_start_pre_this = sd.startPos.minus( sd_pre.startPos );
                    pos.set( sd_pre.shape.getPosition() ).add( diff_start_pre_this );
                    // System.err.println("Scroll-n: idx "+idx+", this "+sd.shape.getPosition()+", pre "+sd_pre.shape.getPosition()+" -> "+pos);
                }
            } else {
                pos.add( p_t.normalize().scale( dxy ) );
            }
            if( clip.intersects2DRegion(pos.x(), pos.y(), sd.shape.getScaledWidth(), sd.shape.getScaledHeight()) ) {
                sd.shape.setEnabled(true);
            } else {
                sd.shape.setEnabled(false);
            }
            sd.shape.moveTo(pos);
            return true;
        }
    };

    /**
     * Sine target {@link LerpFunc}, approaching {@link ShapeData}'s target position utilizing the angular value for sine amplitude
     * towards the given normalized direction vector.
     * <p>
     * The sine amplitude is flattened towards target.
     * </p>
     * <p>
     * Implementation uses the current shape position and relative time duration since last call to interpolate,
     * hence allows rugged utilization even if shapes are dragged around.
     * </p>
     */
    public static class SineLerp implements LerpFunc {
        final Vec3f sineDir;
        final float sineScale;
        final float shapeStep;

        /**
         * New sine {@link LerpFunc} instance
         * @param sineDir normalized vector for sine amplitude direction
         * @param sineScale sine scale factor to amplify effect
         * @param shapeStep shape index {@code idx} factor for {@code dt_s}, amplifying angular distance between each shape. Golden ratio {@code 1.618f} reveals dynamic characteristics.
         */
        public SineLerp(final Vec3f sineDir, final float sineScale, final float shapeStep) {
            this.sineDir = sineDir;
            this.sineScale = sineScale;
            this.shapeStep = shapeStep;
        }
        @Override
        public boolean eval(final long frame_cnt, final Set as, final int idx, final ShapeData sd, final float at_s, final float dt_s) {
            final float dxy = as.velocity_obj * dt_s; // [shapeUnit]
            final float angle = as.ang_velo * ( at_s + idx * shapeStep * dt_s ); // [radians]
            final Vec3f pos = sd.shape.getPosition().copy();
            if( 0 == frame_cnt ) {
                sd.user = null;
            } else if( null != sd.user ) {
                final Vec3f lastSineVal = (Vec3f)sd.user;
                pos.sub(lastSineVal);
            }
            final Vec3f p_t = sd.targetPos.minus(pos);
            final float p_t_diff = p_t.length();
            final boolean pos_ok = p_t_diff <= dxy || p_t_diff <= AnimGroup.POS_EPS;

            if ( pos_ok ) {
                // arrived
                sd.shape.moveTo(sd.targetPos);
                sd.shape.setInteractive(false);
                return false;
            } else {
                final float shapeScale = sd.shape.getScale().y();
                final float p_t_norm;
                {
                    final Vec3f s_t = sd.targetPos.minus(sd.startPos);
                    p_t_norm = p_t_diff / s_t.length(); // [1 -> 0] from start to target
                }
                final float sineAmp = FloatUtil.sin(angle)*p_t_norm*shapeScale*sineScale;
                final Vec3f sineVec = sineDir.copy().scale( sineAmp );
                sd.user = sineVec;
                sd.shape.moveTo( pos.add( p_t.normalize().scale( dxy ) ).add( sineVec ) );
            }
            return true;
        }
        static final boolean methodB = true;
    };
}
