/**
 * Copyright 2010-2024 JogAmp Community. All rights reserved.
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
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
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
     * {@link Shape} move listener
     */
    public static interface MoveListener {
        /**
         * Move callback
         * @param s the moved shape
         * @param origin original position
         * @param dest new position
         */
        void run(Shape s, Vec3f origin, Vec3f dest);
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

    /**
     * Forward {@link KeyListener}, to be attached to a key event source forwarded to the receiver set at constructor.
     * <p>
     * This given receiver {@link Shape} must be {@link #setInteractive(boolean)} to have the events forwarded.
     * </p>
     * @see Shape#receiveKeyEvents(Shape)
     */
    public static class ForwardKeyListener implements KeyListener {
        public final Shape receiver;
        /**
         * {@link ForwardKeyListener} Constructor
         * @param receiver the {@link KeyListener} receiver
         */
        public ForwardKeyListener(final Shape receiver) {
            this.receiver = receiver;
        }

        private void dispatch(final KeyEvent e) {
            if( receiver.isInteractive() ) {
                receiver.dispatchKeyEvent(e);
            }
        }
        @Override
        public void keyPressed(final KeyEvent e) { dispatch(e); }
        @Override
        public void keyReleased(final KeyEvent e) { dispatch(e); }
    }

    /**
     * Forward {@link MouseGestureListener}, to be attached to a mouse event source forwarded to the receiver set at constructor.
     * <p>
     * This given receiver {@link Shape} must be {@link #setInteractive(boolean)} to have the events forwarded.
     * </p>
     * @see Shape#receiveMouseEvents(Shape)
     */
    public static class ForwardMouseListener implements MouseGestureListener {
        public final Shape receiver;
        /**
         * {@link ForwardMouseListener} Constructor
         * @param receiver the {@link MouseGestureListener} receiver
         */
        public ForwardMouseListener(final Shape receiver) {
            this.receiver = receiver;
        }
        private void dispatch(final MouseEvent e) {
            if( receiver.isInteractive() ) {
                receiver.dispatchMouseEvent(e);
            }
        }
        @Override
        public void mouseClicked(final MouseEvent e) { dispatch(e); }
        @Override
        public void mouseEntered(final MouseEvent e) { dispatch(e); }
        @Override
        public void mouseExited(final MouseEvent e) { dispatch(e); }
        @Override
        public void mousePressed(final MouseEvent e) { dispatch(e); }
        @Override
        public void mouseReleased(final MouseEvent e) { dispatch(e); }
        @Override
        public void mouseMoved(final MouseEvent e) { dispatch(e); }
        @Override
        public void mouseDragged(final MouseEvent e) { dispatch(e); }
        @Override
        public void mouseWheelMoved(final MouseEvent e) { dispatch(e); }
        @Override
        public void gestureDetected(final GestureEvent e) {
            if( receiver.isInteractive() ) {
                receiver.dispatchGestureEvent(e);
            }
        }
    };

    protected static final boolean DEBUG_DRAW = false;
    private static final boolean DEBUG = false;

    private static final int DIRTY_SHAPE    = 1 << 0 ;
    private static final int DIRTY_STATE    = 1 << 1 ;

    private volatile Group parent = null;
    protected final AABBox box = new AABBox();

    private final Vec3f position = new Vec3f();
    private float zOffset = 0;
    private final Quaternion rotation = new Quaternion();
    private Vec3f rotPivot = null;
    private final Vec3f scale = new Vec3f(1f, 1f, 1f);
    private final Matrix4f iMat = new Matrix4f();
    private final Matrix4f tmpMat = new Matrix4f();
    private boolean iMatIdent = true;

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

    private int id = -1;
    private String name = "noname";

    private static final int IO_VISIBLE            = 1 << 0;
    private static final int IO_INTERACTIVE        = 1 << 1;
    private static final int IO_ACTIVABLE          = 1 << 2;
    private static final int IO_TOGGLEABLE         = 1 << 3;
    private static final int IO_DRAGGABLE          = 1 << 4;
    private static final int IO_RESIZABLE          = 1 << 5;
    private static final int IO_RESIZE_FIXED_RATIO = 1 << 6;
    private static final int IO_ACTIVE             = 1 << 7;
    private static final int IO_DOWN               = 1 << 8;
    private static final int IO_TOGGLE             = 1 << 9;
    private static final int IO_DRAG_FIRST         = 1 << 10;
    private static final int IO_IN_MOVE            = 1 << 11;
    private static final int IO_IN_RESIZE_BR       = 1 << 12;
    private static final int IO_IN_RESIZE_BL       = 1 << 13;
    private volatile int ioState = IO_DRAGGABLE | IO_RESIZABLE | IO_INTERACTIVE | IO_ACTIVABLE | IO_VISIBLE;
    private final boolean isIO(final int mask) { return mask == ( ioState & mask ); }
    private final Shape setIO(final int mask, final boolean v) { if( v ) { ioState |= mask; } else { ioState &= ~mask; } return this; }

    private float borderThickness = 0f;
    private Padding padding = null;
    private final Vec4f borderColor = new Vec4f(0.0f, 0.0f, 0.0f, 1.0f);
    private ArrayList<MouseGestureListener> mouseListeners = new ArrayList<MouseGestureListener>();
    private ArrayList<KeyListener> keyListeners = new ArrayList<KeyListener>();

    private ListenerBool onInitListener = null;
    private MoveListener onMoveListener = null;
    private Listener onToggleListener = null;
    private ArrayList<Listener> activationListeners = new ArrayList<Listener>();
    private Listener onClickedListener = null;

    private final Vec2f objDraggedFirst = new Vec2f(); // b/c its relative to Shape and we stick to it
    private final int[] winDraggedLast = { 0, 0 }; // b/c its absolute window pos
    private static final float resize_sxy_min = 1f/200f; // 1/2% - TODO: Maybe customizable?
    private static final float resize_section = 1f/5f; // resize action in a corner

    private volatile Tooltip tooltip = null;

    /**
     * Create a generic UI {@link Shape}
     */
    protected Shape() { }

    protected void setParent(final Group c) { parent = c; }

    /**
     * Returns the last parent container {@link Group} this shape has been added to or {@code null}.
     * <p>
     * Since a shape can be added to multiple container (DAG),
     * usability of this information depends on usage.
     * </p>
     */
    public Group getParent() { return parent; }

    /** Set a symbolic ID for this shape for identification. Default is -1 for noname. */
    public final Shape setID(final int id) { this.id = id; return this; }
    /** Return the optional symbolic ID for this shape. */
    public final int getID() { return this.id; }

    /** Set a symbolic name for this shape for identification. Default is `noname`. */
    public Shape setName(final String name) { this.name = name; return this; }
    /** Return the optional symbolic name for this shape, defaults to `noname`. */
    public final String getName() { return this.name; }

    /** Returns true if this shape denotes a {@link Group}, otherwise false. */
    public boolean isGroup() { return false; }

    /**
     * Returns true if this shape is set {@link #setVisible(boolean) visible} by the user, otherwise false. Defaults to true.
     * <p>
     * Note that invisible shapes are not considered for picking/activation.
     * </p>
     * @see #isInteractive()
     */
    public final boolean isVisible() { return isIO(IO_VISIBLE); }
    /**
     * Enable (default) or disable this shape's visibility.
     * <p>
     * Note that invisible shapes are not considered for picking/activation.
     * </p>
     * <p>
     * This visibility flag is toggled by the user only.
     * </p>
     */
    public final Shape setVisible(final boolean v) { return setIO(IO_VISIBLE, v); }

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

    /** Returns true if {@link #setPaddding(Padding)} added a non {@link Padding#zeroSize()} spacing to this shape. */
    public boolean hasPadding() { return null != padding && !padding.zeroSize(); }

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
     * @param gl current {@link GL2ES2} instance used to release GPU resources
     * @param renderer {@link RegionRenderer} used to release GPU resources
     */
    public final void clear(final GL2ES2 gl, final RegionRenderer renderer) {
        synchronized ( dirtySync ) {
            stopToolTip();
            clearImpl0(gl, renderer);
            resetState();
        }
    }
    private final void resetState() {
        position.set(0f, 0f, 0f);
        rotation.setIdentity();
        rotPivot = null;
        scale.set(1f, 1f, 1f);
        iMat.loadIdentity();
        iMatIdent = true;
        box.reset();
        mouseListeners.clear();
        keyListeners.clear();
        onInitListener = null;
        onMoveListener = null;
        onToggleListener = null;
        activationListeners.clear();
        onClickedListener = null;
        markShapeDirty();
    }

    /**
     * Destroys all data
     * @param gl current {@link GL2ES2} instance used to release GPU resources
     * @param renderer {@link RegionRenderer} used to release GPU resources
     */
    public final void destroy(final GL2ES2 gl, final RegionRenderer renderer) {
        removeToolTip();
        destroyImpl0(gl, renderer);
        resetState();
    }

    /**
     * Set a user one-shot initializer callback.
     * <p>
     * {@link ListenerBool#run(Shape)} will be called
     * after each {@link #draw(GL2ES2, RegionRenderer)}
     * until it returns true, signaling user initialization is completed.
     * </p>
     * @param l callback, which shall return true signaling user initialization is done
     */
    public final void onInit(final ListenerBool l) { onInitListener = l; }
    /**
     * Set user callback to be notified when shape is {@link #move(Vec3f)}'ed.
     */
    public final void onMove(final MoveListener l) { onMoveListener = l; }
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
     * Add user callback to be notified when shape is activated (pointer-over and/or click) or de-activated (pointer left).
     * <p>
     * Use {@link #isActive()} to retrieve the state.
     * </p>
     */
    public final Shape addActivationListener(final Listener l) {
        if(l == null) {
            return this;
        }
        @SuppressWarnings("unchecked")
        final ArrayList<Listener> clonedListeners = (ArrayList<Listener>) activationListeners.clone();
        clonedListeners.add(l);
        activationListeners = clonedListeners;
        return this;
    }
    public final Shape removeActivationListener(final Listener l) {
        if (l == null) {
            return this;
        }
        @SuppressWarnings("unchecked")
        final ArrayList<Listener> clonedListeners = (ArrayList<Listener>) activationListeners.clone();
        clonedListeners.remove(l);
        activationListeners = clonedListeners;
        return this;
    }
    /**
     * Dispatch activation event event to this shape
     * @return true to signal operation complete and to stop traversal, otherwise false
     */
    private final void dispatchActivationEvent(final Shape s) {
        final int sz = activationListeners.size();
        for(int i = 0; i < sz; i++ ) {
            activationListeners.get(i).run(s);
        }
    }

    /**
     * Set user callback to be notified when shape is clicked.
     * <p>
     * Usually shape is {@link #toggle()}'ed when clicked, see {@link #onToggle(Listener)}.
     * However, in case shape is not {@link #isToggleable()} this is the last resort.
     * </p>
     */
    public final void onClicked(final Listener l) { onClickedListener = l; }

    /** Move to scaled position. Position ends up in PMVMatrix4f unmodified. No {@link MoveListener} notification will occur. */
    public final Shape moveTo(final float tx, final float ty, final float tz) {
        position.set(tx, ty, tz);
        updateMat();
        return this;
    }

    /** Move to scaled position. Position ends up in PMVMatrix4f unmodified. No {@link MoveListener} notification will occur. */
    public final Shape moveTo(final Vec3f t) {
        position.set(t);
        updateMat();
        return this;
    }

    /** Move about scaled distance. Position ends up in PMVMatrix4f unmodified. No {@link MoveListener} notification will occur. */
    public final Shape move(final float dtx, final float dty, final float dtz) {
        position.add(dtx, dty, dtz);
        updateMat();
        return this;
    }

    /** Move about scaled distance. Position ends up in PMVMatrix4f unmodified. No {@link MoveListener} notification will occur. */
    public final Shape move(final Vec3f dt) {
        position.add(dt);
        updateMat();
        return this;
    }

    private final Shape moveNotify(final float dtx, final float dty, final float dtz) {
        forwardMove(position.copy(), position.add(dtx, dty, dtz));
        return this;
    }

    private final void forwardMove(final Vec3f origin, final Vec3f dest) {
        if( !origin.isEqual(dest) ) {
            updateMat();
            if( null != onMoveListener ) {
                onMoveListener.run(this, origin, dest);
            }
        }
    }

    /**
     * Returns position {@link Vec3f} reference, i.e. scaled translation as set via {@link #moveTo(float, float, float) or {@link #move(float, float, float)}}.
     * @see #updateMat()
     */
    public final Vec3f getPosition() { return position; }

    /**
     * Returns {@link Quaternion} for rotation.
     * @see #updateMat()
     */
    public final Quaternion getRotation() { return rotation; }

    /**
     * Sets the rotation {@link Quaternion}.
     * @return this shape for chaining
     */
    public final Shape setRotation(final Quaternion q) {
        rotation.set(q);
        updateMat();
        return this;
    }

    /**
     * Return unscaled rotation origin {@link Vec3f} reference, aka pivot. Null if not set via {@link #setRotationPivot(float, float, float)}.
     * @see #updateMat()
     */
    public final Vec3f getRotationPivot() { return rotPivot; }

    /**
     * Set unscaled rotation origin, aka pivot. Usually the {@link #getBounds()} center and should be set while {@link #validateImpl(GL2ES2, GLProfile)}.
     * @return this shape for chaining
     */
    public final Shape setRotationPivot(final float px, final float py, final float pz) {
        rotPivot = new Vec3f(px, py, pz);
        updateMat();
        return this;
    }
    /**
     * Set unscaled rotation origin, aka pivot. Usually the {@link #getBounds()} center and should be set while {@link #validateImpl(GL2ES2, GLProfile)}.
     * @param pivot rotation origin
     * @return this shape for chaining
     */
    public final Shape setRotationPivot(final Vec3f pivot) {
        rotPivot = new Vec3f(pivot);
        updateMat();
        return this;
    }

    /**
     * Set scale factor to given scale.
     * @see #scale(Vec3f)
     * @see #getScale()
     */
    public final Shape setScale(final Vec3f s) {
        scale.set(s);
        updateMat();
        return this;
    }
    /**
     * Set scale factor to given scale.
     * @see #scale(float, float, float)
     * @see #getScale()
     */
    public final Shape setScale(final float sx, final float sy, final float sz) {
        scale.set(sx, sy, sz);
        updateMat();
        return this;
    }
    /**
     * Multiply current scale factor by given scale.
     * @see #setScale(Vec3f)
     * @see #getScale()
     */
    public final Shape scale(final Vec3f s) {
        scale.scale(s);
        updateMat();
        return this;
    }
    /**
     * Multiply current scale factor by given scale.
     * @see #setScale(float, float, float)
     * @see #getScale()
     */
    public final Shape scale(final float sx, final float sy, final float sz) {
        scale.scale(sx, sy, sz);
        updateMat();
        return this;
    }
    /**
     * Returns scale {@link Vec3f} reference.
     * @see #setScale(float, float, float)
     * @see #scale(float, float, float)
     * @see #updateMat()
     */
    public final Vec3f getScale() { return scale; }

    /**
     * Marks the shape dirty, causing next {@link #draw(GL2ES2, RegionRenderer) draw()}
     * to recreate the Graph shape and reset the region.
     */
    public final void markShapeDirty() {
        synchronized ( dirtySync ) {
            dirty |= DIRTY_SHAPE;
        }
    }

    /**
     * Marks the rendering state dirty, causing next {@link #draw(GL2ES2, RegionRenderer) draw()}
     * to notify the Graph region to reselect shader and repaint potentially used FBOs.
     */
    public final void markStateDirty() {
        synchronized ( dirtySync ) {
            dirty |= DIRTY_STATE;
        }
    }

    /** Returns the shape's dirty state, see {@link #markShapeDirty()}. */
    protected boolean isShapeDirty() {
        return 0 != ( dirty & DIRTY_SHAPE ) ;
    }
    /** Returns the rendering dirty state, see {@link #markStateDirty()}. */
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
     * The returned {@link AABBox} is only valid after an initial call to {@link #draw(GL2ES2, RegionRenderer) draw(..)}
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
     * The returned width is only valid after an initial call to {@link #draw(GL2ES2, RegionRenderer) draw(..)}
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
     * The returned height is only valid after an initial call to {@link #draw(GL2ES2, RegionRenderer) draw(..)}
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

    /**
     * Returns the unscaled bounding {@link AABBox} for this shape.
     *
     * This variant differs from {@link #getBounds()} as it
     * returns a valid {@link AABBox} even before {@link #draw(GL2ES2, RegionRenderer) draw(..)}
     * and having an OpenGL instance available.
     *
     * @see #getBounds()
     */
    public final AABBox getBounds(final GLProfile glp) {
        validate(glp);
        return box;
    }

    /** Experimental selection draw command used by {@link Scene}. */
    public void drawToSelect(final GL2ES2 gl, final RegionRenderer renderer) {
        synchronized ( dirtySync ) {
            validate(gl);
            drawToSelectImpl0(gl, renderer);
        }
    }

    /**
     * Renders the shape.
     * <p>
     * {@link #applyMatToMv(PMVMatrix4f)} is expected to be completed beforehand.
     * </p>
     * @param gl the current GL object
     * @param renderer {@link RegionRenderer} which might be used for Graph Curve Rendering, also source of {@link RegionRenderer#getMatrix()} and {@link RegionRenderer#getViewport()}.
     */
    public void draw(final GL2ES2 gl, final RegionRenderer renderer) {
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
            drawImpl0(gl, renderer, rgba);
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
            validateImpl(gl, gl.getGLProfile());
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
            validateImpl(null, glp);
            dirty = 0;
        }
        return this;
    }

    /**
     * Validate the shape via {@link #validate(GL2ES2)} if {@code gl} is not null,
     * otherwise uses {@link #validate(GLProfile)}.
     * @see #validate(GL2ES2)
     * @see #validate(GLProfile)
     */
    public final Shape validate(final GL2ES2 gl, final GLProfile glp) {
        if( null != gl ) {
            return validate(gl);
        } else {
            return validate(glp);
        }
    }

    /**
     * Applies the internal {@link Matrix4f} to the given {@link PMVMatrix4f#getMv() modelview matrix},
     * i.e. {@code pmv.mulMv( getMat() )}.
     * <p>
     * In case {@link #isMatIdentity()} is {@code true}, implementation is a no-operation.
     * </p>
     * @param pmv the matrix
     * @see #isMatIdentity()
     * @see #updateMat()
     * @see #getMat()
     * @see PMVMatrix4f#mulMv(Matrix4f)
     */
    public final void applyMatToMv(final PMVMatrix4f pmv) {
        if( !iMatIdent ) {
            pmv.mulMv(iMat);
        }
    }

    /**
     * Returns the internal {@link Matrix4f} reference, see {@link #updateMat()}.
     * <p>
     * Using this method renders {@link #isMatIdentity()} {@code false},
     * since its content is mutable. Use {@link #getMat(Matrix4f) instead if suitable.
     * </p>
     * @see #getMat(Matrix4f)
     * @see #isMatIdentity()
     * @see #applyMatToMv(PMVMatrix4f)
     * @see #updateMat()
     */
    public final Matrix4f getMat() { iMatIdent = false; return iMat; }

    /**
     * Returns a copy of the internal {@link Matrix4f} to {@code out} see {@link #updateMat()}.
     * @see #getMat()
     * @see #isMatIdentity()
     * @see #applyMatToMv(PMVMatrix4f)
     * @see #updateMat()
     */
    public final Matrix4f getMat(final Matrix4f out) { out.load(iMat); return out; }

    /**
     * Returns true if {@link #getMat()} has not been mutated, i.e. contains identity.
     * @see #getMat()
     * @see #updateMat()
     */
    public final boolean isMatIdentity() { return iMatIdent; }

    /**
     * Updates the internal {@link Matrix4f} with local position, rotation and scale.
     * <ul>
     * <li>Scale shape from its center position</li>
     * <li>Rotate shape around optional scaled pivot, see {@link #setRotationPivot(float[])}), otherwise rotate around its scaled center (default)</li>
     * </ul>
     * <p>
     * Shape's origin should be bottom-left @ 0/0 to have build-in drag-zoom work properly.
     * </p>
     * <p>
     * Usually only used internally after modifying position, scale or rotation.
     * </p>
     * <p>
     * However, after modifying borrowed values via {@link #getPosition()}, {@link #getScale()}, {@link #getRotation()} or {@link #getRotationPivot()}
     * without any other change thereafter, e.g. {@link #move(Vec3f)}, this method must be called!
     * </p>
     * @see #isMatIdentity()
     * @see #getMat()
     * @see #getPosition()
     * @see #getScale()
     * @see #getRotation()
     * @see #getRotationPivot()
     * @see #applyMatToMv(PMVMatrix4f)
     */
    public final void updateMat() {
        final boolean hasScale = !scale.isEqual(Vec3f.ONE);
        final boolean hasRotate = !rotation.isIdentity();
        final boolean hasRotPivot = null != rotPivot;
        final Vec3f ctr = box.getCenter();
        final boolean sameScaleRotatePivot = hasScale && hasRotate && ( !hasRotPivot || rotPivot.isEqual(ctr) );

        iMatIdent = false;

        iMat.setToTranslation(position); // identify + translate, scaled

        if( sameScaleRotatePivot ) {
            // Scale shape from its center position and rotate around its center
            iMat.translate(ctr.x()*scale.x(), ctr.y()*scale.y(), ctr.z()*scale.z(), tmpMat); // add-back center, scaled
            iMat.rotate(rotation, tmpMat);
            iMat.scale(scale.x(), scale.y(), scale.z(), tmpMat);
            iMat.translate(-ctr.x(), -ctr.y(), -ctr.z(), tmpMat); // move to center
        } else if( hasRotate || hasScale ) {
            if( hasRotate ) {
                if( hasRotPivot ) {
                    // Rotate shape around its scaled pivot
                    iMat.translate(rotPivot.x()*scale.x(), rotPivot.y()*scale.y(), rotPivot.z()*scale.z(), tmpMat); // pivot back from rot-pivot, scaled
                    iMat.rotate(rotation, tmpMat);
                    iMat.translate(-rotPivot.x()*scale.x(), -rotPivot.y()*scale.y(), -rotPivot.z()*scale.z(), tmpMat); // pivot to rot-pivot, scaled
                } else {
                    // Rotate shape around its scaled center
                    iMat.translate(ctr.x()*scale.x(), ctr.y()*scale.y(), ctr.z()*scale.z(), tmpMat); // pivot back from center-pivot, scaled
                    iMat.rotate(rotation, tmpMat);
                    iMat.translate(-ctr.x()*scale.x(), -ctr.y()*scale.y(), -ctr.z()*scale.z(), tmpMat); // pivot to center-pivot, scaled
                }
            }
            if( hasScale ) {
                // Scale shape from its center position
                iMat.translate(ctr.x()*scale.x(), ctr.y()*scale.y(), ctr.z()*scale.z(), tmpMat); // add-back center, scaled
                iMat.scale(scale.x(), scale.y(), scale.z(), tmpMat);
                iMat.translate(-ctr.x(), -ctr.y(), -ctr.z(), tmpMat); // move to center
            }
        }
    }

    /**
     * {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) Setup} the given {@link PMVMatrix4f}
     * and apply this shape's {@link #applyMatToMv(PMVMatrix4f) transformation}.
     * </p>
     * @param pmvMatrixSetup {@link Scene.PMVMatrixSetup} to {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} given {@link PMVMatrix4f} {@code pmv}.
     * @param viewport used viewport for {@link PMVMatrix4f#mapObjToWin(Vec3f, Recti, Vec3f)}
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #applyMatToMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @return the given {@link PMVMatrix4f} for chaining
     * @see Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti)
     * @see #applyMatToMv(PMVMatrix4f)
     * @see #setPMVMatrix(Scene, PMVMatrix4f)
     */
    public final PMVMatrix4f setPMVMatrix(final Scene.PMVMatrixSetup pmvMatrixSetup, final Recti viewport, final PMVMatrix4f pmv) {
        pmvMatrixSetup.set(pmv, viewport);
        applyMatToMv(pmv);
        return pmv;
    }

    /**
     * {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) Setup} the given {@link PMVMatrix4f}
     * and apply this shape's {@link #applyMatToMv(PMVMatrix4f) transformation}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #applyMatToMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @return the given {@link PMVMatrix4f} for chaining
     * @see Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti)
     * @see #applyMatToMv(PMVMatrix4f)
     * @see #setPMVMatrix(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, PMVMatrix4f)
     */
    public final PMVMatrix4f setPMVMatrix(final Scene scene, final PMVMatrix4f pmv) {
        return setPMVMatrix(scene.getPMVMatrixSetup(), scene.getViewport(), pmv);
    }

    /**
     * Retrieve surface (view) port of this shape, i.e. lower x/y position and size.
     * <p>
     * The given {@link PMVMatrix4f} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * including this shape's {@link #applyMatToMv(PMVMatrix4f)}. See {@link #setPMVMatrix(Scene, PMVMatrix4f)}.
     * </p>
     * @param pmv well formed {@link PMVMatrix4f}, e.g. could have been setup via {@link Shape#setPMVMatrix(Scene, PMVMatrix4f)}.
     * @param viewport the int[4] viewport
     * @param surfacePort Recti target surface port
     * @return given Recti {@code surfacePort} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     */
    public final Recti getSurfacePort(final PMVMatrix4f pmv, final Recti viewport, final Recti surfacePort) {
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
     * including this shape's {@link #applyMatToMv(PMVMatrix4f)}. See {@link #setPMVMatrix(Scene, PMVMatrix4f)}.
     * </p>
     * @param pmv well formed {@link PMVMatrix4f}, e.g. could have been setup via {@link Shape#setPMVMatrix(Scene, PMVMatrix4f)}.
     * @param viewport the int[4] viewport
     * @param surfaceSize int[2] target surface size
     * @return given int[2] {@code surfaceSize} in pixels for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #getSurfaceSize(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, PMVMatrix4f, int[])
     * @see #getSurfaceSize(Scene, PMVMatrix4f, int[])
     */
    public final int[/*2*/] getSurfaceSize(final PMVMatrix4f pmv, final Recti viewport, final int[/*2*/] surfaceSize) {
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
     * including this shape's {@link #applyMatToMv(PMVMatrix4f)}.
     * </p>
     * @param pmvMatrixSetup {@link Scene.PMVMatrixSetup} to {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} given {@link PMVMatrix4f} {@code pmv}.
     * @param viewport used viewport for {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)}
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #applyMatToMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @param surfaceSize int[2] target surface size
     * @return given int[2] {@code surfaceSize} in pixels for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #getSurfaceSize(PMVMatrix4f, Recti, int[])
     * @see #getSurfaceSize(Scene, PMVMatrix4f, int[])
     */
    public final int[/*2*/] getSurfaceSize(final Scene.PMVMatrixSetup pmvMatrixSetup, final Recti viewport, final PMVMatrix4f pmv, final int[/*2*/] surfaceSize) {
        return getSurfaceSize(setPMVMatrix(pmvMatrixSetup, viewport, pmv), viewport, surfaceSize);
    }

    /**
     * Retrieve surface (view) size in pixels of this shape.
     * <p>
     * The given {@link PMVMatrix4f} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} properly for this shape
     * including this shape's {@link #applyMatToMv(PMVMatrix4f)}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #applyMatToMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @param surfaceSize int[2] target surface size
     * @return given int[2] {@code surfaceSize} in pixels for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #getSurfaceSize(PMVMatrix4f, Recti, int[])
     * @see #getSurfaceSize(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, PMVMatrix4f, int[])
     */
    public final int[/*2*/] getSurfaceSize(final Scene scene, final PMVMatrix4f pmv, final int[/*2*/] surfaceSize) {
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
    public final float[] getPixelPerShapeUnit(final int[] shapeSizePx, final float[] pixPerShape) {
        pixPerShape[0] = shapeSizePx[0] / getScaledWidth();
        pixPerShape[0] = shapeSizePx[1] / getScaledHeight();
        return pixPerShape;
    }

    /**
     * Retrieve pixel per scaled shape-coordinate unit, i.e. [px]/[obj].
     * <p>
     * The given {@link PMVMatrix4f} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * including this shape's {@link #applyMatToMv(PMVMatrix4f)}. See {@link #setPMVMatrix(Scene, PMVMatrix4f)}.
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
    public final float[] getPixelPerShapeUnit(final PMVMatrix4f pmv, final Recti viewport, final float[] pixPerShape) {
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
     * including this shape's {@link #applyMatToMv(PMVMatrix4f)}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #applyMatToMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @param pixPerShape float[2] pixel per scaled shape-coordinate unit result storage
     * @return given float[2] {@code pixPerShape} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #getPixelPerShapeUnit(int[], float[])
     * @see #getSurfaceSize(Scene, PMVMatrix4f, int[])
     * @see #getScaledWidth()
     * @see #getScaledHeight()
     */
    public final float[] getPixelPerShapeUnit(final Scene scene, final PMVMatrix4f pmv, final float[] pixPerShape) {
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
     * including this shape's {@link #applyMatToMv(PMVMatrix4f)}. See {@link #setPMVMatrix(Scene, PMVMatrix4f)}.
     * </p>
     * @param pmv well formed {@link PMVMatrix4f}, e.g. could have been setup via {@link Shape#setPMVMatrix(Scene, PMVMatrix4f)}.
     * @param viewport the viewport
     * @param objPos object position relative to this shape's center
     * @param glWinPos int[2] target window position of objPos relative to this shape
     * @return given int[2] {@code glWinPos} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #shapeToWinCoord(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, float[], PMVMatrix4f, int[])
     * @see #shapeToWinCoord(Scene, float[], PMVMatrix4f, int[])
     */
    public final int[/*2*/] shapeToWinCoord(final PMVMatrix4f pmv, final Recti viewport, final Vec3f objPos, final int[/*2*/] glWinPos) {
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
     * including this shape's {@link #applyMatToMv(PMVMatrix4f)}.
     * </p>
     * @param pmvMatrixSetup {@link Scene.PMVMatrixSetup} to {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} given {@link PMVMatrix4f} {@code pmv}.
     * @param viewport used viewport for {@link PMVMatrix4f#mapObjToWin(Vec3f, Recti, Vec3f)}
     * @param objPos object position relative to this shape's center
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #applyMatToMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @param glWinPos int[2] target window position of objPos relative to this shape
     * @return given int[2] {@code glWinPos} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #shapeToWinCoord(PMVMatrix4f, Recti, float[], int[])
     * @see #shapeToWinCoord(Scene, float[], PMVMatrix4f, int[])
     */
    public final int[/*2*/] shapeToWinCoord(final Scene.PMVMatrixSetup pmvMatrixSetup, final Recti viewport, final Vec3f objPos, final PMVMatrix4f pmv, final int[/*2*/] glWinPos) {
        return this.shapeToWinCoord(setPMVMatrix(pmvMatrixSetup, viewport, pmv), viewport, objPos, glWinPos);
    }

    /**
     * Map given object coordinate relative to this shape to window coordinates.
     * <p>
     * The given {@link PMVMatrix4f} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} properly for this shape
     * including this shape's {@link #applyMatToMv(PMVMatrix4f)}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param objPos object position relative to this shape's center
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #applyMatToMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @param glWinPos int[2] target window position of objPos relative to this shape
     * @return given int[2] {@code glWinPos} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)} operation, otherwise {@code null}
     * @see #shapeToWinCoord(PMVMatrix4f, Recti, float[], int[])
     * @see #shapeToWinCoord(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, float[], PMVMatrix4f, int[])
     */
    public final int[/*2*/] shapeToWinCoord(final Scene scene, final Vec3f objPos, final PMVMatrix4f pmv, final int[/*2*/] glWinPos) {
        return this.shapeToWinCoord(scene.getPMVMatrixSetup(), scene.getViewport(), objPos, pmv, glWinPos);
    }

    /**
     * Map given gl-window-coordinates to object coordinates relative to this shape and its z-coordinate.
     * <p>
     * The given {@link PMVMatrix4f} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * including this shape's {@link #applyMatToMv(PMVMatrix4f)}. See {@link #setPMVMatrix(Scene, PMVMatrix4f)}.
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
    public final Vec3f winToShapeCoord(final PMVMatrix4f pmv, final Recti viewport, final int glWinX, final int glWinY, final Vec3f objPos) {
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
     * including this shape's {@link #applyMatToMv(PMVMatrix4f)}.
     * </p>
     * @param pmvMatrixSetup {@link Scene.PMVMatrixSetup} to {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} given {@link PMVMatrix4f} {@code pmv}.
     * @param viewport used viewport for {@link PMVMatrix4f#mapWinToObj(float, float, float, Recti, Vec3f)}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #applyMatToMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
     * @param objPos target object position of glWinX/glWinY relative to this shape
     * @return given {@code objPos} for successful {@link Matrix4f#mapObjToWin(Vec3f, Matrix4f, Recti, Vec3f) gluProject(..)}
     *         and {@link Matrix4f#mapWinToObj(float, float, float, float, Matrix4f, Recti, Vec3f, Vec3f) gluUnProject(..)}
     *         operation, otherwise {@code null}
     * @see #winToShapeCoord(PMVMatrix4f, Recti, int, int, float[])
     * @see #winToShapeCoord(Scene, int, int, PMVMatrix4f, float[])
     */
    public final Vec3f winToShapeCoord(final Scene.PMVMatrixSetup pmvMatrixSetup, final Recti viewport, final int glWinX, final int glWinY, final PMVMatrix4f pmv, final Vec3f objPos) {
        return this.winToShapeCoord(setPMVMatrix(pmvMatrixSetup, viewport, pmv), viewport, glWinX, glWinY, objPos);
    }

    /**
     * Map given gl-window-coordinates to object coordinates relative to this shape and its z-coordinate.
     * <p>
     * The given {@link PMVMatrix4f} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) setup} properly for this shape
     * including this shape's {@link #applyMatToMv(PMVMatrix4f)}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link #applyMatToMv(PMVMatrix4f) shape-transformed} and can be reused by the caller.
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
    public Shape setColor(final float r, final float g, final float b, final float a) {
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
    public Shape setColor(final Vec4f c) {
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
    public Shape setPressedColorMod(final float r, final float g, final float b, final float a) {
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
        markShapeDirty();
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
        markShapeDirty();
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
        final String idS = -1 != id ? ", id "+id : "";
        final String nameS = "noname" != name ? ", '"+name+"'" : "";
        return getDirtyString()+idS+nameS+", visible "+isIO(IO_VISIBLE)+activeS+", toggle "+isIO(IO_TOGGLE)+
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
     * Set this shape toggleable, default is off.
     * @param toggleable
     * @see #isInteractive()
     */
    public final Shape setToggleable(final boolean toggleable) { return setIO(IO_TOGGLEABLE, toggleable); }

    /**
     * Returns true if this shape is toggable,
     * i.e. rendered w/ {@link #setToggleOnColorMod(float, float, float, float)} or {@link #setToggleOffColorMod(float, float, float, float)}.
     * @see #isInteractive()
     */
    public boolean isToggleable() { return isIO(IO_TOGGLEABLE); }

    /**
     * Set this shape's toggle state, default is off.
     * @param v
     * @return
     */
    public final Shape setToggle(final boolean v) {
        setIO(IO_TOGGLE, v);
        toggleNotify(v);
        if( null != onToggleListener ) {
            onToggleListener.run(this);
        }
        markStateDirty();
        return this;
    }
    public final Shape toggle() {
        if( isToggleable() ) {
            setIO(IO_TOGGLE, !isToggleOn());
            toggleNotify(isToggleOn());
            if( null != onToggleListener ) {
                onToggleListener.run(this);
            }
            markStateDirty();
        }
        return this;
    }
    protected void toggleNotify(final boolean on) {}

    /** Returns true this shape's toggle state. */
    public final boolean isToggleOn() { return isIO(IO_TOGGLE); }

    protected final boolean setActive(final boolean v, final float zOffset) {
        if( isActivable() ) {
            this.zOffset = zOffset;
            setIO(IO_ACTIVE, v);
            if( !v ) {
                releaseInteraction();
                final Tooltip tt = tooltip;
                if( null != tt ) {
                    tt.stop(false);
                }
            }
            if( DEBUG ) {
                System.err.println("XXX "+(v?"  Active":"DeActive")+" "+this);
            }
            dispatchActivationEvent(this);
            return true;
        } else {
            return false;
        }
    }
    /** Returns true of this shape is active */
    public boolean isActive() { return isIO(IO_ACTIVE); }

    protected final Listener forwardActivation = new Listener() {
        @Override
        public void run(final Shape shape) {
            dispatchActivationEvent(shape);
        }
    };

    public float getAdjustedZ() {
        return getAdjustedZImpl();
    }
    protected final float getAdjustedZImpl() {
        return position.z() * getScale().z() + zOffset;
    }

    /**
     * Set's a new {@link Tooltip} for this shape.
     * <p>
     * The {@link Shape} must be set {@link #setInteractive(boolean) interactive}
     * to receive the mouse-over signal, i.e. being picked.
     * </p>
     */
    public Tooltip setToolTip(final Tooltip newTooltip) {
        final Tooltip oldTT = this.tooltip;
        this.tooltip = null;
        if( null != oldTT ) {
            oldTT.stop(true);
        }
        newTooltip.setTool(this);
        this.tooltip = newTooltip;
        return newTooltip;
    }
    public void removeToolTip() {
        final Tooltip tt = tooltip;
        tooltip = null;
        if( null != tt ) {
            tt.stop(true);
            tt.setTool(null);
        }
    }
    private void stopToolTip() {
        final Tooltip tt = tooltip;
        if( null != tt ) {
            tt.stop(true);
        }
    }
    /* pp */ Tooltip startToolTip(final boolean lookupParents) {
        Tooltip tt = tooltip;
        if( null != tt ) {
            tt.start();
            return tt;
        } else if( lookupParents ) {
            Shape p = getParent();
            while( null != p ) {
                tt = p.startToolTip(false);
                if( null != tt ) {
                    return tt;
                } else {
                    p = p.getParent();
                }
            }
        }
        return null;
    }
    public Tooltip getTooltip() { return tooltip; }

    /**
     * Set whether this shape is interactive in general,
     * i.e. any user interaction like
     * - {@link #isToggleable()}
     * - {@link #isDraggable()}
     * - {@link #isResizable()}
     * but excluding programmatic changes.
     * @param v new value for {@link #isInteractive()}
     * @see #isInteractive()
     * @see #isVisible()
     * @see #setDraggable(boolean)
     * @see #setResizable(boolean)
     * @see #setDragAndResizeable(boolean)
     */
    public final Shape setInteractive(final boolean v) { return setIO(IO_INTERACTIVE, v); }
    /**
     * Returns if this shape allows user interaction in general, see {@link #setInteractive(boolean)}
     * @see #setInteractive(boolean)
     * @see #isVisible()
     */
    public final boolean isInteractive() { return isIO(IO_INTERACTIVE); }

    /**
     * Set whether this shape is allowed to be activated, i.e become {@link #isActive()}.
     * <p>
     * A non activable shape still allows a shape to be dragged or resized,
     * it just can't gain the main focus.
     * </p>
     */
    public final Shape setActivable(final boolean v) { return setIO(IO_ACTIVABLE, v); }

    /** Returns if this shape is allowed to be activated, i.e become {@link #isActive()}. */
    public final boolean isActivable() { return isIO(IO_ACTIVABLE); }

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
    public final Shape setDraggable(final boolean draggable) { return setIO(IO_DRAGGABLE, draggable); }
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
    public final Shape setResizable(final boolean resizable) { return setIO(IO_RESIZABLE, resizable); }

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
    public final Shape setFixedARatioResize(final boolean v) { return setIO(IO_RESIZE_FIXED_RATIO, v); }

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
     * Forward {@link MouseGestureListener} events to this {@link Shape} from {@code source} using a {@link ForwardMouseListener}.
     * <p>
     * This source {@link Shape} must be {@link #setInteractive(boolean)} to receive and forward the events.
     * </p>
     * <p>
     * This receiver {@link Shape} must be {@link #setInteractive(boolean)} to have the events forwarded.
     * </p>
     * @see #receiveKeyEvents(Shape)
     */
    public void receiveMouseEvents(final Shape source) {
        source.addMouseListener(new Shape.ForwardMouseListener(this));
    }

    public final Shape addKeyListener(final KeyListener l) {
        if(l == null) {
            return this;
        }
        @SuppressWarnings("unchecked")
        final ArrayList<KeyListener> clonedListeners = (ArrayList<KeyListener>) keyListeners.clone();
        clonedListeners.add(l);
        keyListeners = clonedListeners;
        return this;
    }
    public final Shape removeKeyListener(final KeyListener l) {
        if (l == null) {
            return this;
        }
        @SuppressWarnings("unchecked")
        final ArrayList<KeyListener> clonedListeners = (ArrayList<KeyListener>) keyListeners.clone();
        clonedListeners.remove(l);
        keyListeners = clonedListeners;
        return this;
    }
    /**
     * Forward {@link KeyListener} events to this {@link Shape} from {@code source} using a {@link ForwardKeyListener}.
     * <p>
     * This source {@link Shape} must be {@link #setInteractive(boolean)} to receive and forward the events.
     * </p>
     * <p>
     * This receiver {@link Shape} must be {@link #setInteractive(boolean)} to have the events forwarded.
     * </p>
     * @see #receiveMouseEvents(Shape)
     */
    public void receiveKeyEvents(final Shape source) {
        source.addKeyListener(new Shape.ForwardKeyListener(this));
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

    private final void releaseInteraction() {
        setPressed(false);
        setIO(IO_IN_MOVE, false);
        setIO(IO_IN_RESIZE_BR, false);
        setIO(IO_IN_RESIZE_BL, false);
    }

    /**
     * Dispatch given NEWT mouse event to this shape
     * @param e original Newt {@link MouseEvent}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param objPos object position of mouse event relative to this shape
     * @return true to signal operation complete and to stop traversal, otherwise false
     */
    /* pp */ final boolean dispatchMouseEvent(final MouseEvent e, final int glWinX, final int glWinY, final Vec3f objPos) {
        /**
         * Checked at caller!
        if( !isInteractive() ) {
            return false;
        } */
        final boolean resizableOrDraggable = isResizable() || isDraggable();
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
                    if( resizableOrDraggable ) {
                        setIO(IO_DRAG_FIRST, true);
                    }
                    setPressed(true);
                    break;
                case MouseEvent.EVENT_MOUSE_RELEASED:
                    // Release active shape: last pointer has been lifted!
                    releaseInteraction();
                    break;
            }
        }
        if( resizableOrDraggable && MouseEvent.EVENT_MOUSE_DRAGGED == eventType ) {
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
                    if( isResizable() ) {
                        setIO(IO_IN_RESIZE_BR, true);
                    }
                } else {
                    final float minx_bl = box.getMinX();
                    final float miny_bl = box.getMinY();
                    final float maxx_bl = box.getMinX() + box.getWidth() * resize_section;
                    final float maxy_bl = box.getMinY() + box.getHeight() * resize_section;
                    if( minx_bl <= ix && ix <= maxx_bl &&
                        miny_bl <= iy && iy <= maxy_bl ) {
                        if( isResizable() ) {
                            setIO(IO_IN_RESIZE_BL, true);
                        }
                    } else {
                        setIO(IO_IN_MOVE, isDraggable());
                    }
                }
                if( DEBUG ) {
                    System.err.printf("DragFirst: drag %b, resize[br %b, bl %b], obj[%s], flip[x %b, y %b]%n",
                            isIO(IO_IN_MOVE), isIO(IO_IN_RESIZE_BR), isIO(IO_IN_RESIZE_BL), objPos, x_flip, y_flip);
                    System.err.printf("DragFirst: %s%n", this);
                }
                return true; // end signal traversal at 1st drag
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
                            moveNotify(   0, sdy2, 0f); // bottom-right, sticky left- and top-edge
                        } else {
                            moveNotify( sdx, sdy2, 0f); // bottom-left, sticky right- and top-edge
                        }
                        setScale(sx, sy, scale.z());
                    }
                    return true; // end signal traversal with completed drag
                } else if( isIO(IO_IN_MOVE) ) {
                    if( DEBUG ) {
                        System.err.printf("DragMove: win[%4d, %4d] +[%2d, %2d], , flip[x %b, y %b], obj[%s] +[%s], rot %s%n",
                                glWinX, glWinY, shapeEvent.winDrag[0], shapeEvent.winDrag[1],
                                x_flip, y_flip, objPos, shapeEvent.objDrag, euler);
                    }
                    moveNotify( sdx, sdy, 0f);
                    return true; // end signal traversal with completed move
                }
            }
        } // resizableOrDraggable && EVENT_MOUSE_DRAGGED
        e.setAttachment(shapeEvent);

        return dispatchMouseEvent(e);
    }

    /**
     * Dispatch given NEWT mouse event to this shape
     * @param e original Newt {@link MouseEvent}
     * @return true to signal operation complete and to stop traversal, otherwise false
     */
    /* pp */ final boolean dispatchMouseEvent(final MouseEvent e) {
        final short eventType = e.getEventType();
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
        return e.isConsumed(); // end signal traversal if consumed
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
                // moveNotify(dx, dy, 0f);
                setScale(sx, sy, scale.z());
            }
            return; // FIXME: pass through event? Issue zoom event?
        }
        final Shape.EventInfo shapeEvent = new EventInfo(glWinX, glWinY, this, objPos);
        e.setAttachment(shapeEvent);

        dispatchGestureEvent(e);
    }

    /**
     * Dispatch given NEWT mouse event to this shape
     * @param e original Newt {@link MouseEvent}
     * @return true to signal operation complete and to stop traversal, otherwise false
     */
    /* pp */ final boolean dispatchGestureEvent(final GestureEvent e) {
        for(int i = 0; !e.isConsumed() && i < mouseListeners.size(); i++ ) {
            mouseListeners.get(i).gestureDetected(e);
        }
        return e.isConsumed(); // end signal traversal if consumed
    }

    /**
     * Dispatch given NEWT key event to this shape
     * @param e original Newt {@link KeyEvent}
     * @return true to signal operation complete and to stop traversal, otherwise false
     */
    /* pp */ final boolean dispatchKeyEvent(final KeyEvent e) {
        /**
         * Checked at caller!
        if( !isInteractive() ) {
            return false;
        } */
        final short eventType = e.getEventType();
        for(int i = 0; !e.isConsumed() && i < keyListeners.size(); i++ ) {
            final KeyListener l = keyListeners.get(i);
            switch( eventType ) {
                case KeyEvent.EVENT_KEY_PRESSED:
                    l.keyPressed(e);
                    break;
                case KeyEvent.EVENT_KEY_RELEASED:
                    l.keyReleased(e);
                    break;
                default:
                    throw new NativeWindowException("Unexpected key event type " + e.getEventType());
            }
        }
        return e.isConsumed(); // end signal traversal if consumed
    }

    //
    //
    //

    protected abstract void validateImpl(final GL2ES2 gl, final GLProfile glp);

    /**
     * Actual draw implementation, called by {@link #draw(GL2ES2, RegionRenderer)}
     * @param gl
     * @param renderer
     * @param rgba
     */
    protected abstract void drawImpl0(final GL2ES2 gl, final RegionRenderer renderer, final Vec4f rgba);

    /**
     * Actual draw implementation, called by {@link #drawToSelect(GL2ES2, RegionRenderer)}
     * @param gl
     * @param renderer
     */
    protected abstract void drawToSelectImpl0(final GL2ES2 gl, final RegionRenderer renderer);

    /** Custom {@link #clear(GL2ES2, RegionRenderer)} task, called 1st. */
    protected abstract void clearImpl0(final GL2ES2 gl, final RegionRenderer renderer);

    /** Custom {@link #destroy(GL2ES2, RegionRenderer)} task, called 1st. */
    protected abstract void destroyImpl0(final GL2ES2 gl, final RegionRenderer renderer);

    /**
     * Returns true if implementation uses an extra color channel or texture
     * which will be modulated with the passed rgba color {@link #drawImpl0(GL2ES2, RegionRenderer, float[])}.
     *
     * Otherwise the base color will be modulated and passed to {@link #drawImpl0(GL2ES2, RegionRenderer, float[])}.
     */
    public abstract boolean hasColorChannel();

    @SuppressWarnings("unused")
    private static int compareAsc0(final float a, final float b) {
        if( FloatUtil.isEqual2(a, b) ) {
            return 0;
        } else if( a < b ){
            return -1;
        } else {
            return 1;
        }
    }
    private static int compareAsc1(final float a, final float b) {
        if (a < b) {
            return -1; // Neither is NaN, a is smaller
        }
        if (a > b) {
            return 1;  // Neither is NaN, a is larger
        }
        return 0;
    }
    @SuppressWarnings("unused")
    private static int compareDesc0(final float a, final float b) {
        if( FloatUtil.isEqual2(a, b) ) {
            return 0;
        } else if( a < b ){
            return 1;
        } else {
            return -1;
        }
    }
    private static int compareDesc1(final float a, final float b) {
        if (a < b) {
            return 1; // Neither is NaN, a is smaller
        }
        if (a > b) {
            return -1;  // Neither is NaN, a is larger
        }
        return 0;
    }

    public static Comparator<Shape> ZAscendingComparator = new Comparator<Shape>() {
        @Override
        public int compare(final Shape s1, final Shape s2) {
            return compareAsc1( s1.getAdjustedZ(), s2.getAdjustedZ() );
        } };

    public static Comparator<Shape> ZDescendingComparator = new Comparator<Shape>() {
        @Override
        public int compare(final Shape s1, final Shape s2) {
            return compareDesc1( s2.getAdjustedZ(), s1.getAdjustedZ() );
        } };

    //
    //
    //
}
