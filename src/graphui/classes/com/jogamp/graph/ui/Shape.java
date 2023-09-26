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
import java.util.Comparator;

import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Matrix4f;
import com.jogamp.math.Quaternion;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.newt.event.GestureHandler.GestureEvent;
import com.jogamp.newt.event.GestureHandler.GestureListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.newt.event.PinchToZoomGesture;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

/**
 * Generic Shape, potentially using a Graph via {@link GraphShape} or other means of representing content.
 * <p>
 * A shape includes the following build-in user-interactions
 * - drag shape w/ 1-pointer click, see {@link #setDraggable(boolean)}
 * - resize shape w/ 1-pointer click and drag in 1/4th bottom-left and bottom-right corner, see {@link #setResizable(boolean)}.
 * </p>
 * <p>
 * A shape is expected to have its 0/0 origin in its bottom-left corner, otherwise the drag-zoom sticky-edge will not work as expected.
 * </p>
 * <p>
 * A shape's {@link #getBounds()} includes its optional {@link #getPadding()} and optional {@link #getBorderThickness()}.
 * </p>
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 * <p>
 * GraphUI is intended to become an immediate- and retained-mode API.
 * </p>
 * <p>
 * Default colors (toggle-off is full color):
 * - non-toggle: 0.6 * color, static -> 0.6
 * - pressed: 0.8 * color, static -> 0.5
 * - toggle-off: 1.0 * color, static -> 0.6
 * - toggle-on: 0.8 * color
 * </p>
 * @see Scene
 */
public abstract class Shape {
    /**
     * General {@link Shape} visitor
     */
    public static interface Visitor1 {
        /**
         * Visitor method
         * @param s the {@link Shape} to process
         * @return true to signal operation complete and to stop traversal, otherwise false
         */
        boolean visit(Shape s);
    }

    /**
     * General {@link Shape} visitor
     */
    public static interface Visitor2 {
        /**
         * Visitor method
         * @param s the {@link Shape} to process
         * @param pmv the {@link PMVMatrix4f} setup from the {@link Scene} down to the {@link Shape}
         * @return true to signal operation complete and to stop traversal, otherwise false
         */
        boolean visit(Shape s, final PMVMatrix4f pmv);
    }

    /**
     * General {@link Shape} listener action
     */
    public static interface Listener {
        void run(final Shape shape);
    }
    /**
     * {@link Shape} listener action returning a boolean value
     */
    public static interface ListenerBool {
        boolean run(final Shape shape);
    }

    protected static final boolean DEBUG_DRAW = false;
    private static final boolean DEBUG = false;

    private static final int DIRTY_SHAPE    = 1 << 0 ;
    private static final int DIRTY_STATE    = 1 << 1 ;

    protected final AABBox box;

    private final Vec3f position = new Vec3f();
    private final Quaternion rotation = new Quaternion();
    private Vec3f rotPivot = null;
    private final Vec3f scale = new Vec3f(1f, 1f, 1f);

    private volatile int dirty = DIRTY_SHAPE | DIRTY_STATE;
    private final Object dirtySync = new Object();

    /** Default base-color w/o color channel, will be modulated w/ pressed- and toggle color */
    protected final Vec4f rgbaColor             = new Vec4f(0.60f, 0.60f, 0.60f, 1.0f);
    /** Default pressed color-factor (darker and slightly transparent), modulated base-color. ~0.65 (due to alpha) */
    protected final Vec4f pressedRGBAModulate   = new Vec4f(0.70f, 0.70f, 0.70f, 0.8f);
    /** Default toggle color-factor (darkest), modulated base-color.  0.60 * 0.83 ~= 0.50 */
    protected final Vec4f toggleOnRGBAModulate  = new Vec4f(0.83f, 0.83f, 0.83f, 1.0f);
    /** Default toggle color-factor, modulated base-color.  0.60 * 1.00 ~= 0.60 */
    protected final Vec4f toggleOffRGBAModulate = new Vec4f(1.00f, 1.00f, 1.00f, 1.0f);

    private final Vec4f rgba_tmp = new Vec4f(0, 0, 0, 1);
    private final Vec4f cWhite = new Vec4f(1, 1, 1, 1);

    private int name = -1;

    private static final int IO_ENABLED            = 1 << 0;
    private static final int IO_INTERACTIVE        = 1 << 1;
    private static final int IO_TOGGLEABLE         = 1 << 2;
    private static final int IO_DRAGGABLE          = 1 << 3;
    private static final int IO_RESIZABLE          = 1 << 4;
    private static final int IO_RESIZE_FIXED_RATIO = 1 << 5;
    private static final int IO_ACTIVE             = 1 << 6;
    private static final int IO_DOWN               = 1 << 7;
    private static final int IO_TOGGLE             = 1 << 8;
    private static final int IO_DRAG_FIRST         = 1 << 9;
    private static final int IO_IN_MOVE            = 1 << 10;
    private static final int IO_IN_RESIZE_BR       = 1 << 11;
    private static final int IO_IN_RESIZE_BL       = 1 << 12;
    private volatile int ioState = IO_DRAGGABLE | IO_RESIZABLE | IO_INTERACTIVE | IO_ENABLED;
    private final boolean isIO(final int mask) { return mask == ( ioState & mask ); }
    private final void setIO(final int mask, final boolean v) { if( v ) { ioState |= mask; } else { ioState &= ~mask; }; }

    private float borderThickness = 0f;
    private Padding padding = null;
    private final Vec4f borderColor = new Vec4f(0.0f, 0.0f, 0.0f, 1.0f);
    private ArrayList<MouseGestureListener> mouseListeners = new ArrayList<MouseGestureListener>();

    private ListenerBool onInitListener = null;
    private Listener onMoveListener = null;
    private Listener onToggleListener = null;
    private Listener onActivationListener = null;
    private Listener onClickedListener = null;

    private final Vec2f objDraggedFirst = new Vec2f(); // b/c its relative to Shape and we stick to it
    private final int[] winDraggedLast = { 0, 0 }; // b/c its absolute window pos
    private static final float resize_sxy_min = 1f/200f; // 1/2% - TODO: Maybe customizable?
    private static final float resize_section = 1f/5f; // resize action in a corner

    /**
     * Create a generic UI {@link Shape}
     */
    protected Shape() {
        this.box = new AABBox();
    }

    /** Set a symbolic name for this shape for identification. Default is -1 for noname. */
    public final Shape setName(final int name) { this.name = name; return this; }
    /** Return the optional symbolic name for this shape. */
    public final int getName() { return this.name; }

    /** Returns true if this shape is enabled and hence visible, otherwise false. */
    public final boolean isEnabled() { return isIO(IO_ENABLED); }
    /** Enable or disable this shape, i.e. its visibility. */
    public final Shape setEnabled(final boolean v) { setIO(IO_ENABLED, v); return this; }

    /**
     * Sets the padding for this shape, which is included in {@link #getBounds()B} and also includes the border. Default is zero.
     *
     * Method issues {@link #markShapeDirty()}.
     *
     * @param padding distance of shape to the border, i.e. padding
     * @return this shape for chaining
     * @see #getPadding()
     * @see #hasPadding()
     */
    public final Shape setPaddding(final Padding padding) {
        this.padding = padding;
        markShapeDirty();
        return this;
    }

    /**
     * Returns {@link Padding} of this shape, which is included in {@link #getBounds()B} and also includes the border. Default is zero.
     * @see #setPaddding(Padding)
     * @see #hasPadding()
     */
    public Padding getPadding() { return padding; }

    /** Returns true if {@link #setPaddding(Padding)} added a non {@link Padding#zeroSumSize()} spacing to this shape. */
    public boolean hasPadding() { return null != padding && !padding.zeroSumSize(); }

    /**
     * Sets the thickness of the border, which is included in {@link #getBounds()} and is outside of {@link #getPadding()}. Default is zero for no border.
     *
     * Method issues {@link #markShapeDirty()}.
     *
     * @param thickness border thickness, zero for no border
     * @return this shape for chaining
     * @see #setBorderColor(Vec4f)
     */
    public final Shape setBorder(final float thickness) {
        borderThickness = Math.max(0f, thickness);
        markShapeDirty();
        return this;
    }
    /** Returns true if a border has been enabled via {@link #setBorder(float, Padding)}. */
    public final boolean hasBorder() { return !FloatUtil.isZero(borderThickness); }

    /** Returns the border thickness, see {@link #setBorder(float, Padding)}. */
    public final float getBorderThickness() { return borderThickness; }

    /** Perform given {@link Runnable} action synchronized */
    public final void runSynced(final Runnable action) {
        synchronized ( dirtySync ) {
            action.run();
        }
    }

    /**
     * Clears all data and reset all states as if this instance was newly created
     * @param gl TODO
     * @param renderer TODO
     */
    public final void clear(final GL2ES2 gl, final RegionRenderer renderer) {
        synchronized ( dirtySync ) {
            clearImpl0(gl, renderer);
            position.set(0f, 0f, 0f);
            rotation.setIdentity();
            rotPivot = null;
            scale.set(1f, 1f, 1f);
            box.reset();
            markShapeDirty();
        }
    }

    /**
     * Destroys all data
     * @param gl
     * @param renderer
     */
    public final void destroy(final GL2ES2 gl, final RegionRenderer renderer) {
        destroyImpl0(gl, renderer);
        position.set(0f, 0f, 0f);
        rotation.setIdentity();
        rotPivot = null;
        scale.set(1f, 1f, 1f);
        box.reset();
        markShapeDirty();
    }

    /**
     * Set a user one-shot initializer callback.
     * <p>
     * {@link ListenerBool#run(Shape)} will be called
     * after each {@link #draw(GL2ES2, RegionRenderer, int[])}
     * until it returns true, signaling user initialization is completed.
     * </p>
     * @param l callback, which shall return true signaling user initialization is done
     */
    public final void onInit(final ListenerBool l) { onInitListener = l; }
    /**
     * Set user callback to be notified when shape is {@link #move(Vec3f)}'ed.
     */
    public final void onMove(final Listener l) { onMoveListener = l; }
    /**
     * Set user callback to be notified when shape {@link #toggle()}'ed.
     * <p>
     * This is usually the case when clicked, see {@link #onClicked(Listener)}.
     * </p>
     * <p>
     * Use {@link #isToggleOn()} to retrieve the state.
     * </p>
     */
    public final void onToggle(final Listener l) { onToggleListener = l; }
    /**
     * Set user callback to be notified when shape is activated (pointer-over and/or click) or de-activated (pointer left).
     * <p>
     * Use {@link #isActive()} to retrieve the state.
     * </p>
     */
    public final void onActivation(final Listener l) { onActivationListener = l; }
    /**
     * Set user callback to be notified when shape is clicked.
     * <p>
     * Usually shape is {@link #toggle()}'ed when clicked, see {@link #onToggle(Listener)}.
     * However, in case shape is not {@link #isToggleable()} this is the last resort.
     * </p>
     */
    public final void onClicked(final Listener l) { onClickedListener = l; }

    /** Move to scaled position. Position ends up in PMVMatrix4f unmodified. */
    public final Shape moveTo(final float tx, final float ty, final float tz) {
        position.set(tx, ty, tz);
        if( null != onMoveListener ) {
            onMoveListener.run(this);
        }
        return this;
    }

    /** Move to scaled position. Position ends up in PMVMatrix4f unmodified. */
    public final Shape moveTo(final Vec3f t) {
        position.set(t);
        if( null != onMoveListener ) {
            onMoveListener.run(this);
        }
        return this;
    }

    /** Move about scaled distance. Position ends up in PMVMatrix4f unmodified. */
    public final Shape move(final float dtx, final float dty, final float dtz) {
        position.add(dtx, dty, dtz);
        if( null != onMoveListener ) {
            onMoveListener.run(this);
        }
        return this;
    }

    /** Move about scaled distance. Position ends up in PMVMatrix4f unmodified. */
    public final Shape move(final Vec3f dt) {
        position.add(dt);
        if( null != onMoveListener ) {
            onMoveListener.run(this);
        }
        return this;
    }

    /** Returns position, i.e. scaled translation as set via {@link #moveTo(float, float, float) or {@link #move(float, float, float)}}. */
    public final Vec3f getPosition() { return position; }

    /** Returns {@link Quaternion} for rotation. */
    public final Quaternion getRotation() { return rotation; }
    /** Return unscaled rotation origin, aka pivot. Null if not set via {@link #getRotationPivot()}. */
    public final Vec3f getRotationPivot() { return rotPivot; }
    /**
     * Set unscaled rotation origin, aka pivot. Usually the {@link #getBounds()} center and should be set while {@link #validateImpl(GLProfile, GL2ES2)}.
     * @return this shape for chaining
     */
    public final Shape setRotationPivot(final float px, final float py, final float pz) {
        rotPivot = new Vec3f(px, py, pz);
        return this;
    }
    /**
     * Set unscaled rotation origin, aka pivot. Usually the {@link #getBounds()} center and should be set while {@link #validateImpl(GLProfile, GL2ES2)}.
     * @param pivot rotation origin
     * @return this shape for chaining
     */
    public final Shape setRotationPivot(final Vec3f pivot) {
        rotPivot = new Vec3f(pivot);
        return this;
    }

    /**
     * Set scale factor to given scale.
     * @see #scale(Vec3f)
     * @see #getScale()
     */
    public final Shape setScale(final Vec3f s) {
        scale.set(s);
        return this;
    }
    /**
     * Set scale factor to given scale.
     * @see #scale(float, float, float)
     * @see #getScale()
     */
    public final Shape setScale(final float sx, final float sy, final float sz) {
        scale.set(sx, sy, sz);
        return this;
    }
    /**
     * Multiply current scale factor by given scale.
     * @see #setScale(Vec3f)
     * @see #getScale()
     */
    public final Shape scale(final Vec3f s) {
        scale.scale(s);
        return this;
    }
    /**
     * Multiply current scale factor by given scale.
     * @see #setScale(float, float, float)
     * @see #getScale()
     */
    public final Shape scale(final float sx, final float sy, final float sz) {
        scale.scale(sx, sy, sz);
        return this;
    }
    /**
     * Returns scale factors.
     * @see #setScale(float, float, float)
     * @see #scale(float, float, float)
     */
    public final Vec3f getScale() { return scale; }

    /**
     * Marks the shape dirty, causing next {@link #draw(GL2ES2, RegionRenderer, int[]) draw()}
     * to recreate the Graph shape and reset the region.
     */
    public final void markShapeDirty() {
        synchronized ( dirtySync ) {
            dirty |= DIRTY_SHAPE;
        }
    }

    /**
     * Marks the rendering state dirty, causing next {@link #draw(GL2ES2, RegionRenderer, int[]) draw()}
     * to notify the Graph region to reselect shader and repaint potentially used FBOs.
     */
    public final void markStateDirty() {
        synchronized ( dirtySync ) {
            dirty |= DIRTY_STATE;
        }
    }

    protected boolean isShapeDirty() {
        return 0 != ( dirty & DIRTY_SHAPE ) ;
    }
    protected final boolean isStateDirty() {
        return 0 != ( dirty & DIRTY_STATE ) ;
    }

    protected final String getDirtyString() {
        if( isShapeDirty() && isShapeDirty() ) {
            return "dirty[shape, state]";
        } else if( isShapeDirty() ) {
            return "dirty[shape]";
        } else if( isStateDirty() ) {
            return "dirty[state]";
        } else {
            return "clean";
        }
    }

    /**
     * Returns the unscaled bounding {@link AABBox} for this shape, borrowing internal instance.
     *
     * The returned {@link AABBox} will cover the unscaled shape
     * as well as its optional {@link #getPadding()} and optional {@link #getBorderThickness()}.
     *
     * The returned {@link AABBox} is only valid after an initial call to {@link #draw(GL2ES2, RegionRenderer, int[]) draw(..)}
     * or {@link #validate(GL2ES2)}.
     *
     * @see #getBounds(GLProfile)
     */
    public final AABBox getBounds() { return box; }

    /**
     * Returns the scaled width of the bounding {@link AABBox} for this shape.
     *
     * The returned width will cover the scaled shape
     * as well as its optional scaled {@link #getPadding()} and optional scaled {@link #getBorderThickness()}.
     *
     * The returned width is only valid after an initial call to {@link #draw(GL2ES2, RegionRenderer, int[]) draw(..)}
     * or {@link #validate(GL2ES2)}.
     *
     * @see #getBounds()
     */
    public final float getScaledWidth() {
        return box.getWidth() * getScale().x();
    }

    /**
     * Returns the scaled height of the bounding {@link AABBox} for this shape.
     *
     * The returned height will cover the scaled shape
     * as well as its optional scaled {@link #getPadding()} and optional scaled {@link #getBorderThickness()}.
     *
     * The returned height is only valid after an initial call to {@link #draw(GL2ES2, RegionRenderer, int[]) draw(..)}
     * or {@link #validate(GL2ES2)}.
     *
     * @see #getBounds()
     */
    public final float getScaledHeight() {
        return box.getHeight() * getScale().y();
    }

    public final float getScaledDepth() {
        return box.getDepth() * getScale().z();
    }
    private final float getScaledMinZ() {
        return box.getMinZ() * getScale().z();
    }

    /**
     * Returns the unscaled bounding {@link AABBox} for this shape.
     *
     * This variant differs from {@link #getBounds()} as it
     * returns a valid {@link AABBox} even before {@link #draw(GL2ES2, RegionRenderer, int[]) draw(..)}
     * and having an OpenGL instance available.
     *
     * @see #getBounds()
     */
    public final AABBox getBounds(final GLProfile glp) {
        validate(glp);
        return box;
    }

    /** Experimental selection draw command used by {@link Scene}. */
    public void drawToSelect(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount) {
        synchronized ( dirtySync ) {
            validate(gl);
            drawImpl0(gl, renderer, sampleCount, null);
        }
    }

    /**
     * Renders the shape.
     * <p>
     * {@link #setTransformMv(PMVMatrix4f)} is expected to be completed beforehand.
     * </p>
     * @param gl the current GL object
     * @param renderer {@link RegionRenderer} which might be used for Graph Curve Rendering, also source of {@link RegionRenderer#getMatrix()} and {@link RegionRenderer#getViewport()}.
     * @param sampleCount sample count if used by Graph renderModes
     */
    public void draw(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount) {
        final boolean isPressed = isPressed(), isToggleOn = isToggleOn();
        final Vec4f rgba;
        if( hasColorChannel() ) {
            if( isPressed ) {
                rgba = pressedRGBAModulate;
            } else if( isToggleable() ) {
                if( isToggleOn ) {
                    rgba = toggleOnRGBAModulate;
                } else {
                    rgba = toggleOffRGBAModulate;
                }
            } else {
                rgba = cWhite;
            }
        } else {
            rgba = rgba_tmp;
            if( isPressed ) {
                rgba.mul(rgbaColor, pressedRGBAModulate);
            } else if( isToggleable() ) {
                if( isToggleOn ) {
                    rgba.mul(rgbaColor, toggleOnRGBAModulate);
                } else {
                    rgba.mul(rgbaColor, toggleOffRGBAModulate);
                }
            } else {
                rgba.set(rgbaColor);
            }
        }
        synchronized ( dirtySync ) {
            validate(gl);
            drawImpl0(gl, renderer, sampleCount, rgba);
        }
        if( null != onInitListener ) {
            if( onInitListener.run(this) ) {
                onInitListener = null;
            }
        }
    }

    /**
     * Validates the shape's underlying {@link GLRegion}.
     * <p>
     * If the region is dirty, it gets {@link GLRegion#clear(GL2ES2) cleared} and is reused.
     * </p>
     * @param gl current {@link GL2ES2} object
     * @see #validate(GLProfile)
     */
    public final Shape validate(final GL2ES2 gl) {
        synchronized ( dirtySync ) {
            if( isShapeDirty() ) {
                box.reset();
            }
            validateImpl(gl.getGLProfile(), gl);
            dirty = 0;
        }
        return this;
    }

    /**
     * Validates the shape's underlying {@link GLRegion} w/o a current {@link GL2ES2} object
     * <p>
     * If the region is dirty a new region is created
     * and the old one gets pushed to a dirty-list to get disposed when a GL context is available.
     * </p>
     * @see #validate(GL2ES2)
     */
    public final Shape validate(final GLProfile glp) {
        synchronized ( dirtySync ) {
            if( isShapeDirty() ) {
                box.reset();
            }
            validateImpl(glp, null);
            dirty = 0;
        }
        return this;
    }

    /**
     * Setup the {@link PMVMatrix4f#getMv() modelview matrix} of the given {@link PMVMatrix4f} for this object.
     * - Scale shape from its center position
     * - Rotate shape around optional scaled pivot, see {@link #setRotationPivot(float[])}), otherwise rotate around its scaled center (default)
     * <p>
     * Shape's origin should be bottom-left @ 0/0 to have build-in drag-zoom work properly.
     * </p>
     * @param pmv the matrix
     * @see #setRotationPivot(float[])
     * @see #getRotation()
     * @see #moveTo(float, float, float)
     * @see #setScale(float, float, float)
     */
    public void setTransformMv(final PMVMatrix4f pmv) {
        final boolean hasScale = !scale.isEqual(Vec3f.ONE);
        final boolean hasRotate = !rotation.isIdentity();
        final boolean hasRotPivot = null != rotPivot;
        final Vec3f ctr = box.getCenter();
        final boolean sameScaleRotatePivot = hasScale && hasRotate && ( !hasRotPivot || rotPivot.isEqual(ctr) );

        pmv.translateMv(position.x(), position.y(), position.z()); // translate, scaled

        if( sameScaleRotatePivot ) {
            // Scale shape from its center position and rotate around its center
            pmv.translateMv(ctr.x()*scale.x(), ctr.y()*scale.y(), ctr.z()*scale.z()); // add-back center, scaled
            pmv.rotateMv(rotation);
            pmv.scaleMv(scale.x(), scale.y(), scale.z());
            pmv.translateMv(-ctr.x(), -ctr.y(), -ctr.z()); // move to center
        } else if( hasRotate || hasScale ) {
            if( hasRotate ) {
                if( hasRotPivot ) {
                    // Rotate shape around its scaled pivot
                    pmv.translateMv(rotPivot.x()*scale.x(), rotPivot.y()*scale.y(), rotPivot.z()*scale.z()); // pivot back from rot-pivot, scaled
                    pmv.rotateMv(rotation);
                    pmv.translateMv(-rotPivot.x()*scale.x(), -rotPivot.y()*scale.y(), -rotPivot.z()*scale.z()); // pivot to rot-pivot, scaled
                } else {
                    // Rotate shape around its scaled center
                    pmv.translateMv(ctr.x()*scale.x(), ctr.y()*scale.y(), ctr.z()*scale.z()); // pivot back from center-pivot, scaled
                    pmv.rotateMv(rotation);
                    pmv.translateMv(-ctr.x()*scale.x(), -ctr.y()*scale.y(), -ctr.z()*scale.z()); // pivot to center-pivot, scaled
                }
            }
            if( hasScale ) {
                // Scale shape from its center position
                pmv.translateMv(ctr.x()*scale.x(), ctr.y()*scale.y(), ctr.z()*scale.z()); // add-back center, scaled
                pmv.scaleMv(scale.x(), scale.y(), scale.z());
                pmv.translateMv(-ctr.x(), -ctr.y(), -ctr.z()); // move to center
            }
        }
    }

    /**
     * {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) Setup} the given {@link PMVMatrix4f}
     * and apply this shape's {@link #setTransformMv(PMVMatrix4f) transformation}.
     * </p>
     * @param pmvMatrixSetup {@link Scene.PMVMatrixSetup} to {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} given {@link PMVMatrix4f} {@code pmv}.
     * @param viewport used viewport for {@link PMVMatrix4f#mapObjToWin(Vec3f, Recti, Vec3f)}
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #setTransformMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @return the given {@link PMVMatrix4f} for chaining
     * @see Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti)
     * @see #setTransformMv(PMVMatrix4f)
     * @see #setPMVMatrix(Scene, PMVMatrix4f)
     */
    public PMVMatrix4f setPMVMatrix(final Scene.PMVMatrixSetup pmvMatrixSetup, final Recti viewport, final PMVMatrix4f pmv) {
        pmvMatrixSetup.set(pmv, viewport);
        setTransformMv(pmv);
        return pmv;
    }

    /**
     * {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) Setup} the given {@link PMVMatrix4f}
     * and apply this shape's {@link #setTransformMv(PMVMatrix4f) transformation}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #setTransformMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @return the given {@link PMVMatrix4f} for chaining
     * @see Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti)
     * @see #setTransformMv(PMVMatrix4f)
     * @see #setPMVMatrix(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, PMVMatrix4f)
     */
    public PMVMatrix4f setPMVMatrix(final Scene scene, final PMVMatrix4f pmv) {
        return setPMVMatrix(scene.getPMVMatrixSetup(), scene.getViewport(), pmv);
    }

    /**
     * Retrieve surface (view) port of this shape, i.e. lower x/y position and size.
     * <p>
     * The given {@link PMVMatrix4f} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * including this shape's {@link #setTransformMv(PMVMatrix4f)}. See {@link #setPMVMatrix(Scene, PMVMatrix4f)}.
     * </p>
     * @param pmv well formed {@link PMVMatrix4f}, e.g. could have been setup via {@link Shape#setPMVMatrix(Scene, PMVMatrix4f)}.
     * @param viewport the int[4] viewport
     * @param surfacePort Recti target surface port
     * @return given Recti {@code surfacePort} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     */
    public Recti getSurfacePort(final PMVMatrix4f pmv, final Recti viewport, final Recti surfacePort) {
        final Vec3f winCoordHigh = new Vec3f();
        final Vec3f winCoordLow = new Vec3f();
        final Vec3f high = box.getHigh();
        final Vec3f low = box.getLow();

        final Matrix4f matPMv = pmv.getPMv();
        if( Matrix4f.mapObjToWin(high, matPMv, viewport, winCoordHigh) ) {
            if( Matrix4f.mapObjToWin(low, matPMv, viewport, winCoordLow) ) {
                surfacePort.setX( (int)Math.abs( winCoordLow.x() ) );
                surfacePort.setY( (int)Math.abs( winCoordLow.y() ) );
                surfacePort.setWidth( (int)Math.abs( winCoordHigh.x() - winCoordLow.x() ) );
                surfacePort.setHeight( (int)Math.abs( winCoordHigh.y() - winCoordLow.y() ) );
                return surfacePort;
            }
        }
        return null;
    }

    /**
     * Retrieve surface (view) size in pixels of this shape.
     * <p>
     * The given {@link PMVMatrix4f} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * including this shape's {@link #setTransformMv(PMVMatrix4f)}. See {@link #setPMVMatrix(Scene, PMVMatrix4f)}.
     * </p>
     * @param pmv well formed {@link PMVMatrix4f}, e.g. could have been setup via {@link Shape#setPMVMatrix(Scene, PMVMatrix4f)}.
     * @param viewport the int[4] viewport
     * @param surfaceSize int[2] target surface size
     * @return given int[2] {@code surfaceSize} in pixels for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #getSurfaceSize(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, PMVMatrix4f, int[])
     * @see #getSurfaceSize(Scene, PMVMatrix4f, int[])
     */
    public int[/*2*/] getSurfaceSize(final PMVMatrix4f pmv, final Recti viewport, final int[/*2*/] surfaceSize) {
        // System.err.println("Shape::getSurfaceSize.VP "+viewport[0]+"/"+viewport[1]+" "+viewport[2]+"x"+viewport[3]);
        final Vec3f winCoordHigh = new Vec3f();
        final Vec3f winCoordLow = new Vec3f();
        final Vec3f high = box.getHigh();
        final Vec3f low = box.getLow();

        final Matrix4f matPMv = pmv.getPMv();
        if( Matrix4f.mapObjToWin(high, matPMv, viewport, winCoordHigh) ) {
            if( Matrix4f.mapObjToWin(low, matPMv, viewport, winCoordLow) ) {
                surfaceSize[0] = (int)Math.abs(winCoordHigh.x() - winCoordLow.x());
                surfaceSize[1] = (int)Math.abs(winCoordHigh.y() - winCoordLow.y());
                return surfaceSize;
            }
        }
        return null;
    }

    /**
     * Retrieve surface (view) size in pixels of this shape.
     * <p>
     * The given {@link PMVMatrix4f} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} properly for this shape
     * including this shape's {@link #setTransformMv(PMVMatrix4f)}.
     * </p>
     * @param pmvMatrixSetup {@link Scene.PMVMatrixSetup} to {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} given {@link PMVMatrix4f} {@code pmv}.
     * @param viewport used viewport for {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)}
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #setTransformMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @param surfaceSize int[2] target surface size
     * @return given int[2] {@code surfaceSize} in pixels for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #getSurfaceSize(PMVMatrix4f, Recti, int[])
     * @see #getSurfaceSize(Scene, PMVMatrix4f, int[])
     */
    public int[/*2*/] getSurfaceSize(final Scene.PMVMatrixSetup pmvMatrixSetup, final Recti viewport, final PMVMatrix4f pmv, final int[/*2*/] surfaceSize) {
        return getSurfaceSize(setPMVMatrix(pmvMatrixSetup, viewport, pmv), viewport, surfaceSize);
    }

    /**
     * Retrieve surface (view) size in pixels of this shape.
     * <p>
     * The given {@link PMVMatrix4f} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} properly for this shape
     * including this shape's {@link #setTransformMv(PMVMatrix4f)}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #setTransformMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @param surfaceSize int[2] target surface size
     * @return given int[2] {@code surfaceSize} in pixels for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #getSurfaceSize(PMVMatrix4f, Recti, int[])
     * @see #getSurfaceSize(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, PMVMatrix4f, int[])
     */
    public int[/*2*/] getSurfaceSize(final Scene scene, final PMVMatrix4f pmv, final int[/*2*/] surfaceSize) {
        return getSurfaceSize(scene.getPMVMatrixSetup(), scene.getViewport(), pmv, surfaceSize);
    }

    /**
     * Retrieve pixel per scaled shape-coordinate unit, i.e. [px]/[obj].
     * @param shapeSizePx int[2] shape size in pixel as retrieved via e.g. {@link #getSurfaceSize(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, PMVMatrix4f, int[])}
     * @param pixPerShape float[2] pixel scaled per shape-coordinate unit result storage
     * @return given float[2] {@code pixPerShape}
     * @see #getPixelPerShapeUnit(Scene, PMVMatrix4f, float[])
     * @see #getSurfaceSize(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, PMVMatrix4f, int[])
     * @see #getScaledWidth()
     * @see #getScaledHeight()
     */
    public float[] getPixelPerShapeUnit(final int[] shapeSizePx, final float[] pixPerShape) {
        pixPerShape[0] = shapeSizePx[0] / getScaledWidth();
        pixPerShape[0] = shapeSizePx[1] / getScaledHeight();
        return pixPerShape;
    }

    /**
     * Retrieve pixel per scaled shape-coordinate unit, i.e. [px]/[obj].
     * <p>
     * The given {@link PMVMatrix4f} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * including this shape's {@link #setTransformMv(PMVMatrix4f)}. See {@link #setPMVMatrix(Scene, PMVMatrix4f)}.
     * </p>
     * @param pmv well formed {@link PMVMatrix4f}, e.g. could have been setup via {@link Shape#setPMVMatrix(Scene, PMVMatrix4f)}.
     * @param viewport the int[4] viewport
     * @param pixPerShape float[2] pixel per scaled shape-coordinate unit result storage
     * @return given float[2] {@code pixPerShape} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #getPixelPerShapeUnit(int[], float[])
     * @see #getSurfaceSize(Scene, PMVMatrix4f, int[])
     * @see #getScaledWidth()
     * @see #getScaledHeight()
     */
    public float[] getPixelPerShapeUnit(final PMVMatrix4f pmv, final Recti viewport, final float[] pixPerShape) {
        final int[] shapeSizePx = new int[2];
        if( null != getSurfaceSize(pmv, viewport, shapeSizePx) ) {
            return getPixelPerShapeUnit(shapeSizePx, pixPerShape);
        } else {
            return null;
        }
    }

    /**
     * Retrieve pixel per scaled shape-coordinate unit, i.e. [px]/[obj].
     * <p>
     * The given {@link PMVMatrix4f} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} properly for this shape
     * including this shape's {@link #setTransformMv(PMVMatrix4f)}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #setTransformMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @param pixPerShape float[2] pixel per scaled shape-coordinate unit result storage
     * @return given float[2] {@code pixPerShape} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #getPixelPerShapeUnit(int[], float[])
     * @see #getSurfaceSize(Scene, PMVMatrix4f, int[])
     * @see #getScaledWidth()
     * @see #getScaledHeight()
     */
    public float[] getPixelPerShapeUnit(final Scene scene, final PMVMatrix4f pmv, final float[] pixPerShape) {
        final int[] shapeSizePx = new int[2];
        if( null != getSurfaceSize(scene, pmv, shapeSizePx) ) {
            return getPixelPerShapeUnit(shapeSizePx, pixPerShape);
        } else {
            return null;
        }
    }

    /**
     * Map given object coordinate relative to this shape to window coordinates.
     * <p>
     * The given {@link PMVMatrix4f} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * including this shape's {@link #setTransformMv(PMVMatrix4f)}. See {@link #setPMVMatrix(Scene, PMVMatrix4f)}.
     * </p>
     * @param pmv well formed {@link PMVMatrix4f}, e.g. could have been setup via {@link Shape#setPMVMatrix(Scene, PMVMatrix4f)}.
     * @param viewport the viewport
     * @param objPos object position relative to this shape's center
     * @param glWinPos int[2] target window position of objPos relative to this shape
     * @return given int[2] {@code glWinPos} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #shapeToWinCoord(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, float[], PMVMatrix4f, int[])
     * @see #shapeToWinCoord(Scene, float[], PMVMatrix4f, int[])
     */
    public int[/*2*/] shapeToWinCoord(final PMVMatrix4f pmv, final Recti viewport, final Vec3f objPos, final int[/*2*/] glWinPos) {
        // System.err.println("Shape::objToWinCoordgetSurfaceSize.VP "+viewport[0]+"/"+viewport[1]+" "+viewport[2]+"x"+viewport[3]);
        final Vec3f winCoord = new Vec3f();

        if( pmv.mapObjToWin(objPos, viewport, winCoord) ) {
            glWinPos[0] = (int)(winCoord.x());
            glWinPos[1] = (int)(winCoord.y());
            return glWinPos;
        }
        return null;
    }

    /**
     * Map given object coordinate relative to this shape to window coordinates.
     * <p>
     * The given {@link PMVMatrix4f} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} properly for this shape
     * including this shape's {@link #setTransformMv(PMVMatrix4f)}.
     * </p>
     * @param pmvMatrixSetup {@link Scene.PMVMatrixSetup} to {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} given {@link PMVMatrix4f} {@code pmv}.
     * @param viewport used viewport for {@link PMVMatrix4f#mapObjToWin(Vec3f, Recti, Vec3f)}
     * @param objPos object position relative to this shape's center
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #setTransformMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @param glWinPos int[2] target window position of objPos relative to this shape
     * @return given int[2] {@code glWinPos} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #shapeToWinCoord(PMVMatrix4f, Recti, float[], int[])
     * @see #shapeToWinCoord(Scene, float[], PMVMatrix4f, int[])
     */
    public int[/*2*/] shapeToWinCoord(final Scene.PMVMatrixSetup pmvMatrixSetup, final Recti viewport, final Vec3f objPos, final PMVMatrix4f pmv, final int[/*2*/] glWinPos) {
        return this.shapeToWinCoord(setPMVMatrix(pmvMatrixSetup, viewport, pmv), viewport, objPos, glWinPos);
    }

    /**
     * Map given object coordinate relative to this shape to window coordinates.
     * <p>
     * The given {@link PMVMatrix4f} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} properly for this shape
     * including this shape's {@link #setTransformMv(PMVMatrix4f)}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param objPos object position relative to this shape's center
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #setTransformMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @param glWinPos int[2] target window position of objPos relative to this shape
     * @return given int[2] {@code glWinPos} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #shapeToWinCoord(PMVMatrix4f, Recti, float[], int[])
     * @see #shapeToWinCoord(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, float[], PMVMatrix4f, int[])
     */
    public int[/*2*/] shapeToWinCoord(final Scene scene, final Vec3f objPos, final PMVMatrix4f pmv, final int[/*2*/] glWinPos) {
        return this.shapeToWinCoord(scene.getPMVMatrixSetup(), scene.getViewport(), objPos, pmv, glWinPos);
    }

    /**
     * Map given gl-window-coordinates to object coordinates relative to this shape and its z-coordinate.
     * <p>
     * The given {@link PMVMatrix4f} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * including this shape's {@link #setTransformMv(PMVMatrix4f)}. See {@link #setPMVMatrix(Scene, PMVMatrix4f)}.
     * </p>
     * @param pmv well formed {@link PMVMatrix4f}, e.g. could have been setup via {@link Shape#setPMVMatrix(Scene, PMVMatrix4f)}.
     * @param viewport the Rect4i viewport
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param objPos target object position of glWinX/glWinY relative to this shape
     * @return given {@code objPos} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)}
     *         and {@link Matrix4f#mapWinToObj(float, float, float, float, Matrix4f, Recti, Vec3f, Vec3f) gluUnProject(..)}
     *         operation, otherwise {@code null}
     * @see #winToShapeCoord(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, int, int, PMVMatrix4f, float[])
     * @see #winToShapeCoord(Scene, int, int, PMVMatrix4f, float[])
     */
    public Vec3f winToShapeCoord(final PMVMatrix4f pmv, final Recti viewport, final int glWinX, final int glWinY, final Vec3f objPos) {
        final Vec3f ctr = box.getCenter();

        if( Matrix4f.mapObjToWin(ctr, pmv.getPMv(), viewport, objPos) ) {
            final float winZ = objPos.z();
            if( Matrix4f.mapWinToObj(glWinX, glWinY, winZ, pmv.getPMvi(), viewport, objPos) ) {
                return objPos;
            }
        }
        return null;
    }

    /**
     * Map given gl-window-coordinates to object coordinates relative to this shape and its z-coordinate.
     * <p>
     * The given {@link PMVMatrix4f} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} properly for this shape
     * including this shape's {@link #setTransformMv(PMVMatrix4f)}.
     * </p>
     * @param pmvMatrixSetup {@link Scene.PMVMatrixSetup} to {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} given {@link PMVMatrix4f} {@code pmv}.
     * @param viewport used viewport for {@link PMVMatrix4f#mapWinToObj(float, float, float, Recti, Vec3f)}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #setTransformMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @param objPos target object position of glWinX/glWinY relative to this shape
     * @return given {@code objPos} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)}
     *         and {@link Matrix4f#mapWinToObj(float, float, float, float, Matrix4f, Recti, Vec3f, Vec3f) gluUnProject(..)}
     *         operation, otherwise {@code null}
     * @see #winToShapeCoord(PMVMatrix4f, Recti, int, int, float[])
     * @see #winToShapeCoord(Scene, int, int, PMVMatrix4f, float[])
     */
    public Vec3f winToShapeCoord(final Scene.PMVMatrixSetup pmvMatrixSetup, final Recti viewport, final int glWinX, final int glWinY, final PMVMatrix4f pmv, final Vec3f objPos) {
        return this.winToShapeCoord(setPMVMatrix(pmvMatrixSetup, viewport, pmv), viewport, glWinX, glWinY, objPos);
    }

    /**
     * Map given gl-window-coordinates to object coordinates relative to this shape and its z-coordinate.
     * <p>
     * The given {@link PMVMatrix4f} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} properly for this shape
     * including this shape's {@link #setTransformMv(PMVMatrix4f)}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #setTransformMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @param objPos target object position of glWinX/glWinY relative to this shape
     * @return given {@code objPos} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)}
     *         and {@link Matrix4f#mapWinToObj(float, float, float, float, Matrix4f, Recti, Vec3f, Vec3f) gluUnProject(..)}
     *         operation, otherwise {@code null}
     * @see #winToShapeCoord(PMVMatrix4f, Recti, int, int, float[])
     * @see #winToShapeCoord(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, int, int, PMVMatrix4f, float[])
     */
    public final Vec3f winToShapeCoord(final Scene scene, final int glWinX, final int glWinY, final PMVMatrix4f pmv, final Vec3f objPos) {
        return this.winToShapeCoord(scene.getPMVMatrixSetup(), scene.getViewport(), glWinX, glWinY, pmv, objPos);
    }

    public final Vec4f getColor() { return rgbaColor; }

    /**
     * Set base color.
     * <p>
     * Base color w/o color channel, will be modulated w/ pressed- and toggle color
     * </p>
     * <p>
     * Default RGBA value is 0.60f, 0.60f, 0.60f, 1.0f
     * </p>
     */
    public final Shape setColor(final float r, final float g, final float b, final float a) {
        this.rgbaColor.set(r, g, b, a);
        return this;
    }

    /**
     * Set base color.
     * <p>
     * Default base-color w/o color channel, will be modulated w/ pressed- and toggle color
     * </p>
     * <p>
     * Default RGBA value is 0.60f, 0.60f, 0.60f, 1.0f
     * </p>
     */
    public final Shape setColor(final Vec4f c) {
        this.rgbaColor.set(c);
        return this;
    }

    /**
     * Set pressed color.
     * <p>
     * Default pressed color-factor w/o color channel, modulated base-color. ~0.65 (due to alpha)
     * </p>
     * <p>
     * Default RGBA value is 0.70f, 0.70f, 0.70f, 0.8f
     * </p>
     */
    public final Shape setPressedColorMod(final float r, final float g, final float b, final float a) {
        this.pressedRGBAModulate.set(r, g, b, a);
        return this;
    }

    /**
     * Set toggle-on color.
     * <p>
     * Default toggle-on color-factor w/o color channel, modulated base-color.  0.60 * 0.83 ~= 0.50
     * </p>
     * <p>
     * Default RGBA value is 0.83f, 0.83f, 0.83f, 1.0f
     * </p>
     */
    public final Shape setToggleOnColorMod(final float r, final float g, final float b, final float a) {
        this.toggleOnRGBAModulate.set(r, g, b, a);
        return this;
    }

    /**
     * Set toggle-off color.
     * <p>
     * Default toggle-off color-factor w/o color channel, modulated base-color.  0.60 * 1.00 ~= 0.60
     * </p>
     * <p>
     * Default RGBA value is 1.00f, 1.00f, 1.00f, 1.0f
     * </p>
     */
    public final Shape setToggleOffColorMod(final float r, final float g, final float b, final float a) {
        this.toggleOffRGBAModulate.set(r, g, b, a);
        return this;
    }

    public final Vec4f getBorderColor() { return borderColor; }

    /**
     * Set border color.
     * <p>
     * Default RGBA value is 0.00f, 0.00f, 0.00f, 1.0f
     * </p>
     * @see #setBorder(float)
     */
    public final Shape setBorderColor(final float r, final float g, final float b, final float a) {
        this.borderColor.set(r, g, b, a);
        return this;
    }

    /**
     * Set border color.
     * <p>
     * Default RGBA value is 0.00f, 0.00f, 0.00f, 1.0f
     * </p>
     * @see #setBorder(float)
     */
    public final Shape setBorderColor(final Vec4f c) {
        this.borderColor.set(c);
        return this;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName()+"["+getSubString()+"]";
    }

    public String getSubString() {
        final String pivotS;
        if( null != rotPivot ) {
            pivotS = "pivot["+rotPivot+"], ";
        } else {
            pivotS = "";
        }
        final String scaleS;
        if( !scale.isEqual( Vec3f.ONE ) ) {
            scaleS = "scale["+scale+"], ";
        } else {
            scaleS = "scale 1, ";
        }
        final String rotateS;
        if( !rotation.isIdentity() ) {
            final Vec3f euler = rotation.toEuler(new Vec3f());
            rotateS = "rot["+euler+"], ";
        } else {
            rotateS = "";
        }
        final String activeS = isIO(IO_ACTIVE) ? ", active" : "";
        final String ps = hasPadding() ? padding.toString()+", " : "";
        final String bs = hasBorder() ? "border[l "+getBorderThickness()+", c "+getBorderColor()+"], " : "";
        return getDirtyString()+", id "+name+", enabled "+isIO(IO_ENABLED)+activeS+", toggle "+isIO(IO_TOGGLE)+
               ", able[toggle "+isIO(IO_TOGGLEABLE)+", iactive "+isInteractive()+", resize "+isResizable()+", move "+this.isDraggable()+
               "], pos["+position+"], "+pivotS+scaleS+rotateS+
                ps+bs+"box"+box;
    }

    //
    // Input
    //

    public final Shape setPressed(final boolean b) {
        setIO(IO_DOWN, b);
        markStateDirty();
        return this;
    }
    public final boolean isPressed() { return isIO(IO_DOWN); }

    /**
     *
     * @param toggleable
     * @see #isInteractive()
     */
    public final Shape setToggleable(final boolean toggleable) { setIO(IO_TOGGLEABLE, toggleable); return this; }

    /**
     * Returns true if this shape is toggable,
     * i.e. rendered w/ {@link #setToggleOnColorMod(float, float, float, float)} or {@link #setToggleOffColorMod(float, float, float, float)}.
     * @see #isInteractive()
     */
    public boolean isToggleable() { return isIO(IO_TOGGLEABLE); }

    public final Shape setToggle(final boolean v) {
        setIO(IO_TOGGLE, v);
        markStateDirty();
        return this;
    }
    public final Shape toggle() {
        if( isToggleable() ) {
            setIO(IO_TOGGLE, !isToggleOn());
            if( null != onToggleListener ) {
                onToggleListener.run(this);
            }
            markStateDirty();
        }
        return this;
    }
    /** Returns true this shape's toggle state. */
    public final boolean isToggleOn() { return isIO(IO_TOGGLE); }

    protected final void setActive(final boolean v) {
        setIO(IO_ACTIVE, v);
        if( null != onActivationListener ) {
            onActivationListener.run(this);
        }
    }
    /** Returns true of this shape is active */
    public final boolean isActive() { return isIO(IO_ACTIVE); }

    /**
     * Set whether this shape is interactive,
     * i.e. any user interaction like
     * - {@link #isToggleable()}
     * - {@link #isDraggable()}
     * - {@link #isResizable()}
     * but excluding programmatic changes.
     * @param v new value for {@link #isInteractive()}
     * @see #isInteractive()
     * @see #setDraggable(boolean)
     * @see #setResizable(boolean)
     * @see #setDragAndResizeable(boolean)
     */
    public final Shape setInteractive(final boolean v) { setIO(IO_INTERACTIVE, v); return this; }
    /**
     * Returns if this shape allows user interaction, see {@link #setInteractive(boolean)}
     * @see #setInteractive(boolean)
     */
    public final boolean isInteractive() { return isIO(IO_INTERACTIVE); }

    /**
     * Set whether this shape is draggable,
     * i.e. translated by 1-pointer-click and drag.
     * <p>
     * Default draggable is true.
     * </p>
     * @see #isDraggable()
     * @see #setInteractive(boolean)
     * @see #setResizable(boolean)
     * @see #setDragAndResizeable(boolean)
     */
    public final Shape setDraggable(final boolean draggable) { setIO(IO_DRAGGABLE, draggable); return this; }
    /**
     * Returns if this shape is draggable, a user interaction.
     * @see #setDraggable(boolean)
     */
    public final boolean isDraggable() { return isIO(IO_DRAGGABLE); }

    /**
     * Set whether this shape is resizable,
     * i.e. zoomed by 1-pointer-click and drag in 1/4th bottom-left and bottom-right corner.
     * <p>
     * Default resizable is true.
     * </p>
     * @see #isResizable()
     * @see #setInteractive(boolean)
     * @see #setDraggable(boolean)
     * @see #setDragAndResizeable(boolean)
     */
    public final Shape setResizable(final boolean resizable) { setIO(IO_RESIZABLE, resizable); return this; }

    /**
     * Returns if this shape is resiable, a user interaction.
     * @see #setResizable(boolean)
     */
    public final boolean isResizable() { return isIO(IO_RESIZABLE); }

    /**
     * Returns if aspect-ratio shall be kept at resize, if {@link #isResizable()}.
     * @see #setFixedARatioResize(boolean)
     */
    public final boolean isFixedARatioResize() { return isIO(IO_RESIZE_FIXED_RATIO); }

    /**
     * Sets whether aspect-ratio shall be kept at resize, if {@link #isResizable()}.
     * @see #isResizable()
     * @see #isFixedARatioResize()
     */
    public final Shape setFixedARatioResize(final boolean v) { setIO(IO_RESIZE_FIXED_RATIO, v); return this; }

    /**
     * Set whether this shape is draggable and resizable.
     * <p>
     * Default draggable and resizable is true.
     * </p>
     * @see #isDraggable()
     * @see #isResizable()
     * @see #setInteractive(boolean)
     * @see #setDraggable(boolean)
     * @see #setResizable(boolean)
     */
    public final Shape setDragAndResizeable(final boolean v) {
        setDraggable(v);
        setResizable(v);
        return this;
    }

    public final Shape addMouseListener(final MouseGestureListener l) {
        if(l == null) {
            return this;
        }
        @SuppressWarnings("unchecked")
        final ArrayList<MouseGestureListener> clonedListeners = (ArrayList<MouseGestureListener>) mouseListeners.clone();
        clonedListeners.add(l);
        mouseListeners = clonedListeners;
        return this;
    }

    public final Shape removeMouseListener(final MouseGestureListener l) {
        if (l == null) {
            return this;
        }
        @SuppressWarnings("unchecked")
        final ArrayList<MouseGestureListener> clonedListeners = (ArrayList<MouseGestureListener>) mouseListeners.clone();
        clonedListeners.remove(l);
        mouseListeners = clonedListeners;
        return this;
    }

    /**
     * Combining {@link MouseListener} and {@link GestureListener}
     */
    public static interface MouseGestureListener extends MouseListener, GestureListener {
    }

    /**
     * Convenient adapter combining dummy implementation for {@link MouseListener} and {@link GestureListener}
     */
    public static abstract class MouseGestureAdapter extends MouseAdapter implements MouseGestureListener {
        @Override
        public void gestureDetected(final GestureEvent gh) {
        }
    }

    /**
     * {@link Shape} event info for propagated {@link NEWTEvent}s
     * containing reference of {@link #shape the intended shape} as well as
     * the {@link #objPos rotated relative position} to this shape.
     * The latter is normalized to bottom-left zero origin, allowing easier usage.
     */
    public static class EventInfo {
        /** The associated {@link Shape} for this event */
        public final Shape shape;
        /** The relative object coordinate of glWinX/glWinY to the associated {@link Shape}. */
        public final Vec3f objPos;
        /** The GL window coordinates, origin bottom-left */
        public final int[] winPos;
        /** The drag delta of the relative object coordinate of glWinX/glWinY to the associated {@link Shape}. */
        public final Vec2f objDrag = new Vec2f();
        /** The drag delta of GL window coordinates, origin bottom-left */
        public final int[] winDrag = { 0, 0 };

        /**
         * Ctor
         * @param glWinX in GL window coordinates, origin bottom-left
         * @param glWinY in GL window coordinates, origin bottom-left
         * @param shape associated shape
         * @param objPos relative object coordinate of glWinX/glWinY to the associated shape.
         */
        EventInfo(final int glWinX, final int glWinY, final Shape shape, final Vec3f objPos) {
            this.winPos = new int[] { glWinX, glWinY };
            this.shape = shape;
            this.objPos = objPos;
        }

        @Override
        public String toString() {
            return "EventInfo[winPos ["+winPos[0]+", "+winPos[1]+"], objPos ["+objPos+"], "+shape+"]";
        }
    }

    /**
     * Dispatch given NEWT mouse event to this shape
     * @param e original Newt {@link MouseEvent}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param objPos object position of mouse event relative to this shape
     */
    /* pp */ final void dispatchMouseEvent(final MouseEvent e, final int glWinX, final int glWinY, final Vec3f objPos) {
        final Shape.EventInfo shapeEvent = new EventInfo(glWinX, glWinY, this, objPos);

        final short eventType = e.getEventType();
        if( 1 == e.getPointerCount() ) {
            switch( eventType ) {
                case MouseEvent.EVENT_MOUSE_CLICKED:
                    toggle();
                    if( null != onClickedListener ) {
                        onClickedListener.run(this);
                    }
                    break;
                case MouseEvent.EVENT_MOUSE_PRESSED:
                    setIO(IO_DRAG_FIRST, true);
                    setPressed(true);
                    break;
                case MouseEvent.EVENT_MOUSE_RELEASED:
                    // Release active shape: last pointer has been lifted!
                    setPressed(false);
                    setIO(IO_IN_MOVE, false);
                    setIO(IO_IN_RESIZE_BR, false);
                    setIO(IO_IN_RESIZE_BL, false);
                    break;
            }
        }
        switch( eventType ) {
            case MouseEvent.EVENT_MOUSE_DRAGGED: {
                // adjust for rotation
                final Vec3f euler = rotation.toEuler(new Vec3f());
                final boolean x_flip, y_flip;
                {
                    final float x_rot = Math.abs(euler.x());
                    final float y_rot = Math.abs(euler.y());
                    x_flip = 1f*FloatUtil.HALF_PI <= y_rot && y_rot <= 3f*FloatUtil.HALF_PI;
                    y_flip = 1f*FloatUtil.HALF_PI <= x_rot && x_rot <= 3f*FloatUtil.HALF_PI;
                }
                // 1 pointer drag and potential drag-resize
                if( isIO(IO_DRAG_FIRST) ) {
                    objDraggedFirst.set(objPos);
                    winDraggedLast[0] = glWinX;
                    winDraggedLast[1] = glWinY;
                    setIO(IO_DRAG_FIRST, false);

                    final float ix = x_flip ? box.getWidth()  - objPos.x() : objPos.x();
                    final float iy = y_flip ? box.getHeight() - objPos.y() : objPos.y();
                    final float minx_br = box.getMaxX() - box.getWidth() * resize_section;
                    final float miny_br = box.getMinY();
                    final float maxx_br = box.getMaxX();
                    final float maxy_br = box.getMinY() + box.getHeight() * resize_section;
                    if( minx_br <= ix && ix <= maxx_br &&
                        miny_br <= iy && iy <= maxy_br ) {
                        if( isInteractive() && isResizable() ) {
                            setIO(IO_IN_RESIZE_BR, true);
                        }
                    } else {
                        final float minx_bl = box.getMinX();
                        final float miny_bl = box.getMinY();
                        final float maxx_bl = box.getMinX() + box.getWidth() * resize_section;
                        final float maxy_bl = box.getMinY() + box.getHeight() * resize_section;
                        if( minx_bl <= ix && ix <= maxx_bl &&
                            miny_bl <= iy && iy <= maxy_bl ) {
                            if( isInteractive() && isResizable() ) {
                                setIO(IO_IN_RESIZE_BL, true);
                            }
                        } else {
                            setIO(IO_IN_MOVE, isInteractive() && isDraggable());
                        }
                    }
                    if( DEBUG ) {
                        System.err.printf("DragFirst: drag %b, resize[br %b, bl %b], obj[%s], flip[x %b, y %b]%n",
                                isIO(IO_IN_MOVE), isIO(IO_IN_RESIZE_BR), isIO(IO_IN_RESIZE_BL), objPos, x_flip, y_flip);
                        System.err.printf("DragFirst: %s%n", this);
                    }
                    return;
                }
                shapeEvent.objDrag.set( objPos.x() - objDraggedFirst.x(),
                                        objPos.y() - objDraggedFirst.y() );
                shapeEvent.objDrag.scale(x_flip ? -1f : 1f, y_flip ? -1f : 1f);

                shapeEvent.winDrag[0] = glWinX - winDraggedLast[0];
                shapeEvent.winDrag[1] = glWinY - winDraggedLast[1];
                winDraggedLast[0] = glWinX;
                winDraggedLast[1] = glWinY;
                if( 1 == e.getPointerCount() ) {
                    final float sdx = shapeEvent.objDrag.x() * scale.x(); // apply scale, since operation
                    final float sdy = shapeEvent.objDrag.y() * scale.y(); // is from a scaled-model-viewpoint
                    if( isIO(IO_IN_RESIZE_BR) || isIO(IO_IN_RESIZE_BL) ) {
                        final float bw = box.getWidth();
                        final float bh = box.getHeight();
                        final float sdy2, sx, sy;
                        if( isIO(IO_IN_RESIZE_BR) ) {
                            sx = scale.x() + sdx/bw; // bottom-right
                        } else {
                            sx = scale.x() - sdx/bw; // bottom-left
                        }
                        if( isFixedARatioResize() ) {
                            sy = sx;
                            sdy2  = bh * ( scale.y() - sy );
                        } else {
                            sdy2 = sdy;
                            sy = scale.y() - sdy2/bh;
                        }
                        if( resize_sxy_min <= sx && resize_sxy_min <= sy ) { // avoid scale flip
                            if( DEBUG ) {
                                System.err.printf("DragZoom: resize[br %b, bl %b], win[%4d, %4d], , flip[x %b, y %b], obj[%s], dxy +[%s], sdxy +[%.4f, %.4f], sdxy2 +[%.4f, %.4f], scale [%s] -> [%.4f, %.4f]%n",
                                        isIO(IO_IN_RESIZE_BR), isIO(IO_IN_RESIZE_BL), glWinX, glWinY, x_flip, y_flip, objPos,
                                        shapeEvent.objDrag, sdx, sdy, sdx, sdy2,
                                        scale, sx, sy);
                            }
                            if( isIO(IO_IN_RESIZE_BR) ) {
                                move(   0, sdy2, 0f); // bottom-right, sticky left- and top-edge
                            } else {
                                move( sdx, sdy2, 0f); // bottom-left, sticky right- and top-edge
                            }
                            setScale(sx, sy, scale.z());
                        }
                        return; // FIXME: pass through event? Issue zoom event?
                    } else if( isIO(IO_IN_MOVE) ) {
                        if( DEBUG ) {
                            System.err.printf("DragMove: win[%4d, %4d] +[%2d, %2d], , flip[x %b, y %b], obj[%s] +[%s], rot %s%n",
                                    glWinX, glWinY, shapeEvent.winDrag[0], shapeEvent.winDrag[1],
                                    x_flip, y_flip, objPos, shapeEvent.objDrag, euler);
                        }
                        move( sdx, sdy, 0f);
                        // FIXME: Pass through event? Issue move event?
                    }
                }
            }
            break;
        }
        e.setAttachment(shapeEvent);

        for(int i = 0; !e.isConsumed() && i < mouseListeners.size(); i++ ) {
            final MouseGestureListener l = mouseListeners.get(i);
            switch( eventType ) {
                case MouseEvent.EVENT_MOUSE_CLICKED:
                    l.mouseClicked(e);
                    break;
                case MouseEvent.EVENT_MOUSE_ENTERED:
                    l.mouseEntered(e);
                    break;
                case MouseEvent.EVENT_MOUSE_EXITED:
                    l.mouseExited(e);
                    break;
                case MouseEvent.EVENT_MOUSE_PRESSED:
                    l.mousePressed(e);
                    break;
                case MouseEvent.EVENT_MOUSE_RELEASED:
                    l.mouseReleased(e);
                    break;
                case MouseEvent.EVENT_MOUSE_MOVED:
                    l.mouseMoved(e);
                    break;
                case MouseEvent.EVENT_MOUSE_DRAGGED:
                    l.mouseDragged(e);
                    break;
                case MouseEvent.EVENT_MOUSE_WHEEL_MOVED:
                    l.mouseWheelMoved(e);
                    break;
                default:
                    throw new NativeWindowException("Unexpected mouse event type " + e.getEventType());
            }
        }
    }

    /**
     * @param e original Newt {@link GestureEvent}
     * @param glWinX x-position in OpenGL model space
     * @param glWinY y-position in OpenGL model space
     * @param pmv well formed PMVMatrix4f for this shape
     * @param viewport the viewport
     * @param objPos object position of mouse event relative to this shape
     */
    /* pp */ final void dispatchGestureEvent(final GestureEvent e, final int glWinX, final int glWinY, final PMVMatrix4f pmv, final Recti viewport, final Vec3f objPos) {
        if( isInteractive() && isResizable() && e instanceof PinchToZoomGesture.ZoomEvent ) {
            final PinchToZoomGesture.ZoomEvent ze = (PinchToZoomGesture.ZoomEvent) e;
            final float pixels = ze.getDelta() * ze.getScale(); //
            final int winX2 = glWinX + Math.round(pixels);
            final Vec3f objPos2 = winToShapeCoord(pmv, viewport, winX2, glWinY, new Vec3f());
            if( null == objPos2 ) {
                return;
            }
            final float dx = objPos2.x();
            final float dy = objPos2.y();
            final float sx = scale.x() + ( dx/box.getWidth() ); // bottom-right
            final float sy = scale.y() + ( dy/box.getHeight() );
            if( DEBUG ) {
                System.err.printf("DragZoom: resize[br %b, bl %b], win %4d/%4d, obj %s, %s + %.3f/%.3f -> %.3f/%.3f%n",
                        isIO(IO_IN_RESIZE_BR), isIO(IO_IN_RESIZE_BL), glWinX, glWinY, objPos, position, dx, dy, sx, sy);
            }
            if( resize_sxy_min <= sx && resize_sxy_min <= sy ) { // avoid scale flip
                if( DEBUG ) {
                    System.err.printf("PinchZoom: pixels %f, win %4d/%4d, obj %s, %s + %.3f/%.3f -> %.3f/%.3f%n",
                            pixels, glWinX, glWinY, objPos, position, dx, dy, sx, sy);
                }
                // move(dx, dy, 0f);
                setScale(sx, sy, scale.z());
            }
            return; // FIXME: pass through event? Issue zoom event?
        }
        final Shape.EventInfo shapeEvent = new EventInfo(glWinX, glWinY, this, objPos);
        e.setAttachment(shapeEvent);

        for(int i = 0; !e.isConsumed() && i < mouseListeners.size(); i++ ) {
            mouseListeners.get(i).gestureDetected(e);
        }
    }

    //
    //
    //

    protected abstract void validateImpl(final GLProfile glp, final GL2ES2 gl);

    /**
     * Actual draw implementation
     * @param gl
     * @param renderer
     * @param sampleCount
     * @param rgba if null, caller is {@link #drawToSelect(GL2ES2, RegionRenderer, int[])}, otherwise regular {@#link #draw(GL2ES2, RegionRenderer, int[])}
     */
    protected abstract void drawImpl0(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount, Vec4f rgba);

    protected abstract void clearImpl0(final GL2ES2 gl, final RegionRenderer renderer);

    protected abstract void destroyImpl0(final GL2ES2 gl, final RegionRenderer renderer);

    /**
     * Returns true if implementation uses an extra color channel or texture
     * which will be modulated with the passed rgba color {@link #drawImpl0(GL2ES2, RegionRenderer, int[], float[])}.
     *
     * Otherwise the base color will be modulated and passed to {@link #drawImpl0(GL2ES2, RegionRenderer, int[], float[])}.
     */
    public abstract boolean hasColorChannel();

    public static Comparator<Shape> ZAscendingComparator = new Comparator<Shape>() {
        @Override
        public int compare(final Shape s1, final Shape s2) {
            final float s1Z = s1.getScaledMinZ()+s1.getPosition().z();
            final float s2Z = s2.getScaledMinZ()+s2.getPosition().z();
            if( FloatUtil.isEqual2(s1Z, s2Z) ) {
                return 0;
            } else if( s1Z < s2Z ){
                return -1;
            } else {
                return 1;
            }
        } };

    public static Comparator<Shape> ZDescendingComparator = new Comparator<Shape>() {
        @Override
        public int compare(final Shape s1, final Shape s2) {
            final float s1Z = s1.getScaledMinZ()+s1.getPosition().z();
            final float s2Z = s2.getScaledMinZ()+s2.getPosition().z();
            if( FloatUtil.isEqual2(s1Z, s2Z) ) {
                return 0;
            } else if( s1Z < s2Z ){
                return 1;
            } else {
                return -1;
            }
        } };

    //
    //
    //
}
