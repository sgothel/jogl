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
import com.jogamp.newt.event.GestureHandler.GestureEvent;
import com.jogamp.newt.event.GestureHandler.GestureListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.newt.event.PinchToZoomGesture;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.Recti;
import com.jogamp.opengl.math.Vec2f;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * Generic UI Shape, potentially using a Graph via {@link GraphShape} or other means of representing content.
 * <p>
 * A shape includes the following build-in user-interactions
 * - drag shape w/ 1-pointer click, see {@link #setDraggable(boolean)}
 * - resize shape w/ 1-pointer click and drag in 1/4th bottom-left and bottom-right corner, see {@link #setResizable(boolean)}.
 * </p>
 * <p>
 * A shape is expected to have its 0/0 origin in its bottom-left corner, otherwise the drag-zoom sticky-edge will not work as expected.
 * </p>
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 * <p>
 * GraphUI is intended to become an immediate- and retained-mode API.
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
         * @param pmv the {@link PMVMatrix} setup from the {@link Scene} down to the {@link Shape}
         * @return true to signal operation complete and to stop traversal, otherwise false
         */
        boolean visit(Shape s, final PMVMatrix pmv);
    }

    /**
     * General {@link Shape} listener action
     */
    public static interface Listener {
        void run(final Shape shape);
    }
    protected static final boolean DEBUG_DRAW = false;
    private static final boolean DEBUG = false;

    private static final int DIRTY_SHAPE    = 1 << 0 ;
    private static final int DIRTY_STATE    = 1 << 1 ;

    protected final AABBox box;

    private final Vec3f position = new Vec3f();
    private final Quaternion rotation = new Quaternion();
    private final Vec3f rotPivot = new Vec3f();
    private final Vec3f scale = new Vec3f(1f, 1f, 1f);

    private volatile int dirty = DIRTY_SHAPE | DIRTY_STATE;
    private final Object dirtySync = new Object();

    /** Default base-color w/o color channel, will be modulated w/ pressed- and toggle color */
    protected final float[] rgbaColor         = {0.75f, 0.75f, 0.75f, 1.0f};
    /** Default pressed color-factor w/o color channel, modulated base-color. 0.75 * 1.2 = 0.9 */
    protected final float[] pressedRGBAModulate = {1.2f, 1.2f, 1.2f, 0.7f};
    /** Default toggle color-factor w/o color channel, modulated base-color.  0.75 * 1.13 ~ 0.85 */
    protected final float[] toggleOnRGBAModulate = {1.13f, 1.13f, 1.13f, 1.0f};
    /** Default toggle color-factor w/o color channel, modulated base-color.  0.75 * 0.86 ~ 0.65 */
    protected final float[] toggleOffRGBAModulate = {0.86f, 0.86f, 0.86f, 1.0f};

    private int name = -1;

    private boolean down = false;
    private boolean toggle = false;
    private boolean toggleable = false;
    private boolean draggable = true;
    private boolean resizable = true;
    private boolean interactive = true;
    private boolean enabled = true;
    private float dbgbox_thickness = 0f; // fractional thickness of bounds, 0f for no debug box
    private ArrayList<MouseGestureListener> mouseListeners = new ArrayList<MouseGestureListener>();

    private Listener onMoveListener = null;
    private Listener onToggleListener = null;

    public Shape() {
        this.box = new AABBox();
    }

    /** Set a symbolic name for this shape for identification. Default is -1 for noname. */
    public final void setName(final int name) { this.name = name; }
    /** Return the optional symbolic name for this shape. */
    public final int getName() { return this.name; }

    /** Returns true if this shape is enabled and hence visible, otherwise false. */
    public final boolean isEnabled() { return enabled; }
    /** Enable or disable this shape, i.e. its visibility. */
    public final void setEnabled(final boolean v) { enabled = v; }

    /**
     * Sets the {@link #getBounds()} fractional thickness of the debug box ranging [0..1], zero for no debug box (default).
     * @param v fractional thickness of {@link #getBounds()} ranging [0..1], zero for no debug box
     */
    public final void setDebugBox(final float v) {
        dbgbox_thickness = Math.min(1f, Math.max(0f, v));
    }
    /** Returns true if a debug box has been enabled via {@link #setDebugBox(float)}. */
    public final boolean hasDebugBox() { return !FloatUtil.isZero(dbgbox_thickness); }
    /** Returns the fractional thickness of the debug box ranging [0..1], see {@link #setDebugBox(float)}. */
    public final float getDebugBox() { return dbgbox_thickness; }

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
            rotPivot.set(0f, 0f, 0f);
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
        rotPivot.set(0f, 0f, 0f);
        scale.set(1f, 1f, 1f);
        box.reset();
        markShapeDirty();
    }

    public final void onMove(final Listener l) { onMoveListener = l; }
    public final void onToggle(final Listener l) { onToggleListener = l; }

    /** Move to scaled position. Position ends up in PMVMatrix unmodified. */
    public final void moveTo(final float tx, final float ty, final float tz) {
        position.set(tx, ty, tz);
        if( null != onMoveListener ) {
            onMoveListener.run(this);
        }
    }

    /** Move to scaled position. Position ends up in PMVMatrix unmodified. */
    public final void moveTo(final Vec3f t) {
        position.set(t);
        if( null != onMoveListener ) {
            onMoveListener.run(this);
        }
    }

    /** Move about scaled distance. Position ends up in PMVMatrix unmodified. */
    public final void move(final float dtx, final float dty, final float dtz) {
        position.add(dtx, dty, dtz);
        if( null != onMoveListener ) {
            onMoveListener.run(this);
        }
    }

    /** Move about scaled distance. Position ends up in PMVMatrix unmodified. */
    public final void move(final Vec3f dt) {
        position.add(dt);
        if( null != onMoveListener ) {
            onMoveListener.run(this);
        }
    }

    /** Returns position, i.e. scaled translation as set via {@link #moveTo(float, float, float) or {@link #move(float, float, float)}}. */
    public final Vec3f getPosition() { return position; }

    /** Returns {@link Quaternion} for rotation. */
    public final Quaternion getRotation() { return rotation; }
    /** Return unscaled rotation origin, aka pivot. */
    public final Vec3f getRotationPivot() { return rotPivot; }
    /** Set unscaled rotation origin, aka pivot. Usually the {@link #getBounds()} center and should be set while {@link #validateImpl(GLProfile, GL2ES2)}. */
    public final void setRotationPivot(final float px, final float py, final float pz) {
        rotPivot.set(px, py, pz);
    }
    /**
     * Set unscaled rotation origin, aka pivot. Usually the {@link #getBounds()} center and should be set while {@link #validateImpl(GLProfile, GL2ES2)}.
     * @param pivot rotation origin
     */
    public final void setRotationPivot(final Vec3f pivot) {
        rotPivot.set(pivot);
    }

    /**
     * Set scale factor to given scale.
     * @see #scale(float, float, float)
     * @see #getScale()
     */
    public final void setScale(final float sx, final float sy, final float sz) {
        scale.set(sx, sy, sz);
    }
    /**
     * Multiply current scale factor by given scale.
     * @see #setScale(float, float, float)
     * @see #getScale()
     */
    public final void scale(final float sx, final float sy, final float sz) {
        scale.scale(sx, sy, sz);
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

    protected final boolean isShapeDirty() {
        return 0 != ( dirty & DIRTY_SHAPE ) ;
    }
    protected final boolean isStateDirty() {
        return 0 != ( dirty & DIRTY_STATE ) ;
    }

    /**
     * Returns the unscaled bounding {@link AABBox} for this shape, borrowing internal instance.
     *
     * The returned {@link AABBox} will only cover this unscaled shape
     * after an initial call to {@link #draw(GL2ES2, RegionRenderer, int[]) draw(..)}
     * or {@link #validate(GL2ES2)}.
     *
     * @see #getBounds(GLProfile)
     */
    public final AABBox getBounds() { return box; }

    /**
     * Returns the scaled width of the bounding {@link AABBox} for this shape.
     *
     * The returned width will only cover the scaled shape
     * after an initial call to {@link #draw(GL2ES2, RegionRenderer, int[]) draw(..)}
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
     * The returned height will only cover the scaled shape
     * after an initial call to {@link #draw(GL2ES2, RegionRenderer, int[]) draw(..)}
     * or {@link #validate(GL2ES2)}.
     *
     * @see #getBounds()
     */
    public final float getScaledHeight() {
        return box.getHeight() * getScale().y();
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

    private final float[] rgba_tmp = { 0, 0, 0, 1 };

    /**
     * Renders the shape.
     * <p>
     * {@link #setTransform(PMVMatrix)} is expected to be completed beforehand.
     * </p>
     * @param gl the current GL object
     * @param renderer {@link RegionRenderer} which might be used for Graph Curve Rendering, also source of {@link RegionRenderer#getMatrix()} and {@link RegionRenderer#getViewport()}.
     * @param sampleCount sample count if used by Graph renderModes
     */
    public void draw(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount) {
        final boolean isPressed = isPressed(), isToggleOn = isToggleOn();
        final float[] rgba;
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
                rgba = rgbaColor;
            }
        } else {
            rgba = rgba_tmp;
            if( isPressed ) {
                rgba[0] = rgbaColor[0]*pressedRGBAModulate[0];
                rgba[1] = rgbaColor[1]*pressedRGBAModulate[1];
                rgba[2] = rgbaColor[2]*pressedRGBAModulate[2];
                rgba[3] = rgbaColor[3]*pressedRGBAModulate[3];
            } else if( isToggleable() ) {
                if( isToggleOn ) {
                    rgba[0] = rgbaColor[0]*toggleOnRGBAModulate[0];
                    rgba[1] = rgbaColor[1]*toggleOnRGBAModulate[1];
                    rgba[2] = rgbaColor[2]*toggleOnRGBAModulate[2];
                    rgba[3] = rgbaColor[3]*toggleOnRGBAModulate[3];
                } else {
                    rgba[0] = rgbaColor[0]*toggleOffRGBAModulate[0];
                    rgba[1] = rgbaColor[1]*toggleOffRGBAModulate[1];
                    rgba[2] = rgbaColor[2]*toggleOffRGBAModulate[2];
                    rgba[3] = rgbaColor[3]*toggleOffRGBAModulate[3];
                }
            } else {
                rgba[0] = rgbaColor[0];
                rgba[1] = rgbaColor[1];
                rgba[2] = rgbaColor[2];
                rgba[3] = rgbaColor[3];
            }
        }
        synchronized ( dirtySync ) {
            validate(gl);
            drawImpl0(gl, renderer, sampleCount, rgba);
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
    public final void validate(final GL2ES2 gl) {
        synchronized ( dirtySync ) {
            if( isShapeDirty() ) {
                box.reset();
            }
            validateImpl(gl.getGLProfile(), gl);
            dirty = 0;
        }
    }

    /**
     * Validates the shape's underlying {@link GLRegion} w/o a current {@link GL2ES2} object
     * <p>
     * If the region is dirty a new region is created
     * and the old one gets pushed to a dirty-list to get disposed when a GL context is available.
     * </p>
     * @see #validate(GL2ES2)
     */
    public final void validate(final GLProfile glp) {
        synchronized ( dirtySync ) {
            if( isShapeDirty() ) {
                box.reset();
            }
            validateImpl(glp, null);
            dirty = 0;
        }
    }

    /**
     * Setup the pre-selected {@link GLMatrixFunc#GL_MODELVIEW} {@link PMVMatrix} for this object.
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
    public void setTransform(final PMVMatrix pmv) {
        final boolean hasScale = !scale.isEqual(Vec3f.ONE);
        final boolean hasRotate = !rotation.isIdentity();
        final boolean hasRotPivot = !rotPivot.isZero();
        final Vec3f ctr = box.getCenter();
        final boolean sameScaleRotatePivot = hasScale && hasRotate && ( !hasRotPivot || rotPivot.isEqual(ctr) );

        pmv.glTranslatef(position.x(), position.y(), position.z()); // translate, scaled

        if( sameScaleRotatePivot ) {
            // Scale shape from its center position and rotate around its center
            pmv.glTranslatef(ctr.x()*scale.x(), ctr.y()*scale.y(), ctr.z()*scale.z()); // add-back center, scaled
            pmv.glRotate(rotation);
            pmv.glScalef(scale.x(), scale.y(), scale.z());
            pmv.glTranslatef(-ctr.x(), -ctr.y(), -ctr.z()); // move to center
        } else if( hasRotate || hasScale ) {
            if( hasRotate ) {
                if( hasRotPivot ) {
                    // Rotate shape around its scaled pivot
                    pmv.glTranslatef(rotPivot.x()*scale.x(), rotPivot.y()*scale.y(), rotPivot.z()*scale.z()); // pivot back from rot-pivot, scaled
                    pmv.glRotate(rotation);
                    pmv.glTranslatef(-rotPivot.x()*scale.x(), -rotPivot.y()*scale.y(), -rotPivot.z()*scale.z()); // pivot to rot-pivot, scaled
                } else {
                    // Rotate shape around its scaled center
                    pmv.glTranslatef(ctr.x()*scale.x(), ctr.y()*scale.y(), ctr.z()*scale.z()); // pivot back from center-pivot, scaled
                    pmv.glRotate(rotation);
                    pmv.glTranslatef(-ctr.x()*scale.x(), -ctr.y()*scale.y(), -ctr.z()*scale.z()); // pivot to center-pivot, scaled
                }
            }
            if( hasScale ) {
                // Scale shape from its center position
                pmv.glTranslatef(ctr.x()*scale.x(), ctr.y()*scale.y(), ctr.z()*scale.z()); // add-back center, scaled
                pmv.glScalef(scale.x(), scale.y(), scale.z());
                pmv.glTranslatef(-ctr.x(), -ctr.y(), -ctr.z()); // move to center
            }
        }
        // TODO: Add alignment features.
    }

    /**
     * Retrieve surface (view) size of this shape.
     * <p>
     * The given {@link PMVMatrix} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * including this shape's {@link #setTransform(PMVMatrix)}.
     * </p>
     * @param pmv well formed {@link PMVMatrix}, e.g. could have been setup via {@link Scene#setupMatrix(PMVMatrix) setupMatrix(..)} and {@link #setTransform(PMVMatrix)}.
     * @param viewport the int[4] viewport
     * @param surfaceSize int[2] target surface size
     * @return given int[2] {@code surfaceSize} for successful gluProject(..) operation, otherwise {@code null}
     * @see #getSurfaceSize(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, PMVMatrix, int[])
     * @see #getSurfaceSize(Scene, PMVMatrix, int[])
     */
    public int[/*2*/] getSurfaceSize(final PMVMatrix pmv, final Recti viewport, final int[/*2*/] surfaceSize) {
        // System.err.println("Shape::getSurfaceSize.VP "+viewport[0]+"/"+viewport[1]+" "+viewport[2]+"x"+viewport[3]);
        final Vec3f winCoordHigh = new Vec3f();
        final Vec3f winCoordLow = new Vec3f();
        final Vec3f high = box.getHigh();
        final Vec3f low = box.getLow();

        final Matrix4f matPMv = pmv.getPMvMat();
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
     * Retrieve surface (view) size of this shape.
     * <p>
     * The given {@link PMVMatrix} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) setup} properly for this shape
     * including this shape's {@link #setTransform(PMVMatrix)}.
     * </p>
     * @param pmvMatrixSetup {@link Scene.PMVMatrixSetup} to {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) setup} given {@link PMVMatrix} {@code pmv}.
     * @param viewport used viewport for {@link PMVMatrix#gluProject(float, float, float, int[], float[])}
     * @param pmv a new {@link PMVMatrix} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) be setup},
     *            {@link #setTransform(PMVMatrix) shape-transformed} and can be reused by the caller.
     * @param surfaceSize int[2] target surface size
     * @return given int[2] {@code surfaceSize} for successful gluProject(..) operation, otherwise {@code null}
     * @see #getSurfaceSize(PMVMatrix, Recti, int[])
     * @see #getSurfaceSize(Scene, PMVMatrix, int[])
     */
    public int[/*2*/] getSurfaceSize(final Scene.PMVMatrixSetup pmvMatrixSetup, final Recti viewport, final PMVMatrix pmv, final int[/*2*/] surfaceSize) {
        pmvMatrixSetup.set(pmv, viewport);
        setTransform(pmv);
        return getSurfaceSize(pmv, viewport, surfaceSize);
    }

    /**
     * Retrieve surface (view) size of this shape.
     * <p>
     * The given {@link PMVMatrix} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) setup} properly for this shape
     * including this shape's {@link #setTransform(PMVMatrix)}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param pmv a new {@link PMVMatrix} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) be setup},
     *            {@link #setTransform(PMVMatrix) shape-transformed} and can be reused by the caller.
     * @param surfaceSize int[2] target surface size
     * @return given int[2] {@code surfaceSize} for successful gluProject(..) operation, otherwise {@code null}
     * @see #getSurfaceSize(PMVMatrix, Recti, int[])
     * @see #getSurfaceSize(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, PMVMatrix, int[])
     */
    public int[/*2*/] getSurfaceSize(final Scene scene, final PMVMatrix pmv, final int[/*2*/] surfaceSize) {
        return getSurfaceSize(scene.getPMVMatrixSetup(), scene.getViewport(), pmv, surfaceSize);
    }

    /**
     * Retrieve pixel per scaled shape-coordinate unit, i.e. [px]/[obj].
     * <p>
     * The given {@link PMVMatrix} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) setup} properly for this shape
     * including this shape's {@link #setTransform(PMVMatrix)}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param pmv a new {@link PMVMatrix} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) be setup},
     *            {@link #setTransform(PMVMatrix) shape-transformed} and can be reused by the caller.
     * @param pixPerShape float[2] pixel per scaled shape-coordinate unit result storage
     * @return given float[2] {@code pixPerShape} for successful gluProject(..) operation, otherwise {@code null}
     * @see #getPixelPerShapeUnit(int[], float[])
     * @see #getSurfaceSize(Scene, PMVMatrix, int[])
     * @see #getScaledWidth()
     * @see #getScaledHeight()
     */
    public float[] getPixelPerShapeUnit(final Scene scene, final PMVMatrix pmv, final float[] pixPerShape) {
        final int[] shapeSizePx = new int[2];
        if( null != getSurfaceSize(scene, new PMVMatrix(), shapeSizePx) ) {
            return getPixelPerShapeUnit(shapeSizePx, pixPerShape);
        } else {
            return null;
        }
    }

    /**
     * Retrieve pixel per scaled shape-coordinate unit, i.e. [px]/[obj].
     * @param shapeSizePx int[2] shape size in pixel as retrieved via e.g. {@link #getSurfaceSize(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, PMVMatrix, int[])}
     * @param pixPerShape float[2] pixel scaled per shape-coordinate unit result storage
     * @return given float[2] {@code pixPerShape}
     * @see #getPixelPerShapeUnit(Scene, PMVMatrix, float[])
     * @see #getSurfaceSize(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, PMVMatrix, int[])
     * @see #getScaledWidth()
     * @see #getScaledHeight()
     */
    public float[] getPixelPerShapeUnit(final int[] shapeSizePx, final float[] pixPerShape) {
        pixPerShape[0] = shapeSizePx[0] / getScaledWidth();
        pixPerShape[0] = shapeSizePx[1] / getScaledHeight();
        return pixPerShape;
    }

    /**
     * Map given object coordinate relative to this shape to window coordinates.
     * <p>
     * The given {@link PMVMatrix} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * including this shape's {@link #setTransform(PMVMatrix)}.
     * </p>
     * @param pmv well formed {@link PMVMatrix}, e.g. could have been setup via {@link Scene#setupMatrix(PMVMatrix) setupMatrix(..)} and {@link #setTransform(PMVMatrix)}.
     * @param viewport the viewport
     * @param objPos object position relative to this shape's center
     * @param glWinPos int[2] target window position of objPos relative to this shape
     * @return given int[2] {@code glWinPos} for successful gluProject(..) operation, otherwise {@code null}
     * @see #shapeToWinCoord(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, float[], PMVMatrix, int[])
     * @see #shapeToWinCoord(Scene, float[], PMVMatrix, int[])
     */
    public int[/*2*/] shapeToWinCoord(final PMVMatrix pmv, final Recti viewport, final Vec3f objPos, final int[/*2*/] glWinPos) {
        // System.err.println("Shape::objToWinCoordgetSurfaceSize.VP "+viewport[0]+"/"+viewport[1]+" "+viewport[2]+"x"+viewport[3]);
        final Vec3f winCoord = new Vec3f();

        if( pmv.gluProject(objPos, viewport, winCoord) ) {
            glWinPos[0] = (int)(winCoord.x());
            glWinPos[1] = (int)(winCoord.y());
            return glWinPos;
        }
        return null;
    }

    /**
     * Map given object coordinate relative to this shape to window coordinates.
     * <p>
     * The given {@link PMVMatrix} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) setup} properly for this shape
     * including this shape's {@link #setTransform(PMVMatrix)}.
     * </p>
     * @param pmvMatrixSetup {@link Scene.PMVMatrixSetup} to {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) setup} given {@link PMVMatrix} {@code pmv}.
     * @param viewport used viewport for {@link PMVMatrix#gluProject(Vec3f, Recti, Vec3f)}
     * @param objPos object position relative to this shape's center
     * @param pmv a new {@link PMVMatrix} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) be setup},
     *            {@link #setTransform(PMVMatrix) shape-transformed} and can be reused by the caller.
     * @param glWinPos int[2] target window position of objPos relative to this shape
     * @return given int[2] {@code glWinPos} for successful gluProject(..) operation, otherwise {@code null}
     * @see #shapeToWinCoord(PMVMatrix, Recti, float[], int[])
     * @see #shapeToWinCoord(Scene, float[], PMVMatrix, int[])
     */
    public int[/*2*/] shapeToWinCoord(final Scene.PMVMatrixSetup pmvMatrixSetup, final Recti viewport, final Vec3f objPos, final PMVMatrix pmv, final int[/*2*/] glWinPos) {
        pmvMatrixSetup.set(pmv, viewport);
        setTransform(pmv);
        return this.shapeToWinCoord(pmv, viewport, objPos, glWinPos);
    }

    /**
     * Map given object coordinate relative to this shape to window coordinates.
     * <p>
     * The given {@link PMVMatrix} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) setup} properly for this shape
     * including this shape's {@link #setTransform(PMVMatrix)}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param objPos object position relative to this shape's center
     * @param pmv a new {@link PMVMatrix} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) be setup},
     *            {@link #setTransform(PMVMatrix) shape-transformed} and can be reused by the caller.
     * @param glWinPos int[2] target window position of objPos relative to this shape
     * @return given int[2] {@code glWinPos} for successful gluProject(..) operation, otherwise {@code null}
     * @see #shapeToWinCoord(PMVMatrix, Recti, float[], int[])
     * @see #shapeToWinCoord(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, float[], PMVMatrix, int[])
     */
    public int[/*2*/] shapeToWinCoord(final Scene scene, final Vec3f objPos, final PMVMatrix pmv, final int[/*2*/] glWinPos) {
        return this.shapeToWinCoord(scene.getPMVMatrixSetup(), scene.getViewport(), objPos, pmv, glWinPos);
    }

    /**
     * Map given gl-window-coordinates to object coordinates relative to this shape and its z-coordinate.
     * <p>
     * The given {@link PMVMatrix} has to be setup properly for this object,
     * i.e. its {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW} for the surrounding scene
     * including this shape's {@link #setTransform(PMVMatrix)}.
     * </p>
     * @param pmv well formed {@link PMVMatrix}, e.g. could have been setup via {@link Scene#setupMatrix(PMVMatrix) setupMatrix(..)} and {@link #setTransform(PMVMatrix)}.
     * @param viewport the Rect4i viewport
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param objPos target object position of glWinX/glWinY relative to this shape
     * @return given {@code objPos} for successful gluProject(..) and gluUnProject(..) operation, otherwise {@code null}
     * @see #winToShapeCoord(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, int, int, PMVMatrix, float[])
     * @see #winToShapeCoord(Scene, int, int, PMVMatrix, float[])
     */
    public Vec3f winToShapeCoord(final PMVMatrix pmv, final Recti viewport, final int glWinX, final int glWinY, final Vec3f objPos) {
        final Vec3f ctr = box.getCenter();

        if( Matrix4f.mapObjToWin(ctr, pmv.getPMvMat(), viewport, objPos) ) {
            final float winZ = objPos.z();
            if( Matrix4f.mapWinToObj(glWinX, glWinY, winZ, pmv.getPMviMat(), viewport, objPos) ) {
                return objPos;
            }
        }
        return null;
    }

    /**
     * Map given gl-window-coordinates to object coordinates relative to this shape and its z-coordinate.
     * <p>
     * The given {@link PMVMatrix} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) setup} properly for this shape
     * including this shape's {@link #setTransform(PMVMatrix)}.
     * </p>
     * @param pmvMatrixSetup {@link Scene.PMVMatrixSetup} to {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) setup} given {@link PMVMatrix} {@code pmv}.
     * @param viewport used viewport for {@link PMVMatrix#gluUnProject(float, float, float, Recti, Vec3f)}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param pmv a new {@link PMVMatrix} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) be setup},
     *            {@link #setTransform(PMVMatrix) shape-transformed} and can be reused by the caller.
     * @param objPos target object position of glWinX/glWinY relative to this shape
     * @return given {@code objPos} for successful gluProject(..) and gluUnProject(..) operation, otherwise {@code null}
     * @see #winToShapeCoord(PMVMatrix, Recti, int, int, float[])
     * @see #winToShapeCoord(Scene, int, int, PMVMatrix, float[])
     */
    public Vec3f winToShapeCoord(final Scene.PMVMatrixSetup pmvMatrixSetup, final Recti viewport, final int glWinX, final int glWinY, final PMVMatrix pmv, final Vec3f objPos) {
        pmvMatrixSetup.set(pmv, viewport);
        setTransform(pmv);
        return this.winToShapeCoord(pmv, viewport, glWinX, glWinY, objPos);
    }

    /**
     * Map given gl-window-coordinates to object coordinates relative to this shape and its z-coordinate.
     * <p>
     * The given {@link PMVMatrix} will be {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) setup} properly for this shape
     * including this shape's {@link #setTransform(PMVMatrix)}.
     * </p>
     * @param scene {@link Scene} to retrieve {@link Scene.PMVMatrixSetup} and the viewport.
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param pmv a new {@link PMVMatrix} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix, Recti) be setup},
     *            {@link #setTransform(PMVMatrix) shape-transformed} and can be reused by the caller.
     * @param objPos target object position of glWinX/glWinY relative to this shape
     * @return given {@code objPos} for successful gluProject(..) and gluUnProject(..) operation, otherwise {@code null}
     * @see #winToShapeCoord(PMVMatrix, Recti, int, int, float[])
     * @see #winToShapeCoord(com.jogamp.graph.ui.Scene.PMVMatrixSetup, Recti, int, int, PMVMatrix, float[])
     */
    public Vec3f winToShapeCoord(final Scene scene, final int glWinX, final int glWinY, final PMVMatrix pmv, final Vec3f objPos) {
        return this.winToShapeCoord(scene.getPMVMatrixSetup(), scene.getViewport(), glWinX, glWinY, pmv, objPos);
    }

    public float[] getColor() {
        return rgbaColor;
    }

    /**
     * Set base color.
     * <p>
     * Default base-color w/o color channel, will be modulated w/ pressed- and toggle color
     * </p>
     */
    public final void setColor(final float r, final float g, final float b, final float a) {
        this.rgbaColor[0] = r;
        this.rgbaColor[1] = g;
        this.rgbaColor[2] = b;
        this.rgbaColor[3] = a;
    }

    /**
     * Set pressed color.
     * <p>
     * Default pressed color-factor w/o color channel, modulated base-color. 0.75 * 1.2 = 0.9
     * </p>
     */
    public final void setPressedColorMod(final float r, final float g, final float b, final float a) {
        this.pressedRGBAModulate[0] = r;
        this.pressedRGBAModulate[1] = g;
        this.pressedRGBAModulate[2] = b;
        this.pressedRGBAModulate[3] = a;
    }

    /**
     * Set toggle-on color.
     * <p>
     * Default toggle-on color-factor w/o color channel, modulated base-color.  0.75 * 1.13 ~ 0.85
     * </p>
     */
    public final void setToggleOnColorMod(final float r, final float g, final float b, final float a) {
        this.toggleOnRGBAModulate[0] = r;
        this.toggleOnRGBAModulate[1] = g;
        this.toggleOnRGBAModulate[2] = b;
        this.toggleOnRGBAModulate[3] = a;
    }

    /**
     * Set toggle-off color.
     * <p>
     * Default toggle-off color-factor w/o color channel, modulated base-color.  0.75 * 0.86 ~ 0.65
     * </p>
     */
    public final void setToggleOffColorMod(final float r, final float g, final float b, final float a) {
        this.toggleOffRGBAModulate[0] = r;
        this.toggleOffRGBAModulate[1] = g;
        this.toggleOffRGBAModulate[2] = b;
        this.toggleOffRGBAModulate[3] = a;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName()+"["+getSubString()+"]";
    }

    public String getSubString() {
        final String pivotS;
        if( !rotPivot.isZero() ) {
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
        return "enabled "+enabled+", toggle[able "+toggleable+", state "+toggle+
               "], able[iactive "+isInteractive()+", resize "+isResizable()+", move "+this.isDraggable()+
               "], ["+position+"], "+pivotS+scaleS+rotateS+
                "box "+box;
    }

    //
    // Input
    //

    public void setPressed(final boolean b) {
        this.down  = b;
        markStateDirty();
    }
    public boolean isPressed() {
        return this.down;
    }

    /**
     *
     * @param toggleable
     * @see #isInteractive()
     */
    public void setToggleable(final boolean toggleable) {
        this.toggleable = toggleable;
    }

    /**
     * Returns true if this shape is toggable,
     * i.e. rendered w/ {@link #setToggleOnColorMod(float, float, float, float)} or {@link #setToggleOffColorMod(float, float, float, float)}.
     * @see #isInteractive()
     */
    public boolean isToggleable() {
        return toggleable;
    }
    public void setToggle(final boolean v) {
        toggle = v;
        markStateDirty();
    }
    public void toggle() {
        if( isToggleable() ) {
            toggle = !toggle;
            onToggleListener.run(this);
            markStateDirty();
        }
    }
    public boolean isToggleOn() { return toggle; }

    /**
     * Set whether this shape is interactive,
     * i.e. any user interaction like
     * - {@link #isToggleable()}
     * - {@link #isDraggable()}
     * - {@link #isResizable()}
     * but excluding programmatic changes.
     * @param v new value for {@link #isInteractive()}
     */
    public void setInteractive(final boolean v) { interactive = v; }
    /**
     * Returns if this shape allows user interaction, see {@link #setInteractive(boolean)}
     * @see #setInteractive(boolean)
     */
    public boolean isInteractive() { return interactive; }

    /**
     * Set whether this shape is draggable,
     * i.e. translated by 1-pointer-click and drag.
     * <p>
     * Default draggable is true.
     * </p>
     * @see #isInteractive()
     */
    public void setDraggable(final boolean draggable) {
        this.draggable = draggable;
    }
    /**
     * Returns if this shape is draggable, a user interaction.
     * @see #isInteractive()
     */
    public boolean isDraggable() {
        return draggable;
    }

    /**
     * Set whether this shape is resizable,
     * i.e. zoomed by 1-pointer-click and drag in 1/4th bottom-left and bottom-right corner.
     * <p>
     * Default resizable is true.
     * </p>
     * @see #isInteractive()
     */
    public void setResizable(final boolean resizable) {
        this.resizable = resizable;
    }
    /**
     * Returns if this shape is resiable, a user interaction.
     * @see #isInteractive()
     */
    public boolean isResizable() {
        return resizable;
    }

    public final void addMouseListener(final MouseGestureListener l) {
        if(l == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        final ArrayList<MouseGestureListener> clonedListeners = (ArrayList<MouseGestureListener>) mouseListeners.clone();
        clonedListeners.add(l);
        mouseListeners = clonedListeners;
    }

    public final void removeMouseListener(final MouseGestureListener l) {
        if (l == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        final ArrayList<MouseGestureListener> clonedListeners = (ArrayList<MouseGestureListener>) mouseListeners.clone();
        clonedListeners.remove(l);
        mouseListeners = clonedListeners;
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

    private boolean dragFirst = false;
    private final Vec2f objDraggedFirst = new Vec2f(); // b/c its relative to Shape and we stick to it
    private final int[] winDraggedLast = { 0, 0 }; // b/c its absolute window pos
    private boolean inMove = false;
    private int inResize = 0; // 1 br, 2 bl
    private static final float resize_sxy_min = 1f/200f; // 1/2% - TODO: Maybe customizable?
    private static final float resize_section = 1f/5f; // resize action in a corner

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
                    break;
                case MouseEvent.EVENT_MOUSE_PRESSED:
                    dragFirst = true;
                    setPressed(true);
                    break;
                case MouseEvent.EVENT_MOUSE_RELEASED:
                    // Release active shape: last pointer has been lifted!
                    setPressed(false);
                    inMove = false;
                    inResize = 0;
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
                if(dragFirst) {
                    objDraggedFirst.set(objPos);
                    winDraggedLast[0] = glWinX;
                    winDraggedLast[1] = glWinY;
                    dragFirst=false;

                    final float ix = x_flip ? box.getWidth()  - objPos.x() : objPos.x();
                    final float iy = y_flip ? box.getHeight() - objPos.y() : objPos.y();
                    final float minx_br = box.getMaxX() - box.getWidth() * resize_section;
                    final float miny_br = box.getMinY();
                    final float maxx_br = box.getMaxX();
                    final float maxy_br = box.getMinY() + box.getHeight() * resize_section;
                    if( minx_br <= ix && ix <= maxx_br &&
                        miny_br <= iy && iy <= maxy_br ) {
                        if( interactive && resizable ) {
                            inResize = 1; // bottom-right
                        }
                    } else {
                        final float minx_bl = box.getMinX();
                        final float miny_bl = box.getMinY();
                        final float maxx_bl = box.getMinX() + box.getWidth() * resize_section;
                        final float maxy_bl = box.getMinY() + box.getHeight() * resize_section;
                        if( minx_bl <= ix && ix <= maxx_bl &&
                            miny_bl <= iy && iy <= maxy_bl ) {
                            if( interactive && resizable ) {
                                inResize = 2; // bottom-left
                            }
                        } else {
                            inMove = interactive && draggable;
                        }
                    }
                    if( DEBUG ) {
                        System.err.printf("DragFirst: drag %b, resize %d, obj[%s], flip[x %b, y %b]%n",
                                inMove, inResize, objPos, x_flip, y_flip);
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
                    if( 0 != inResize ) {
                        final float bw = box.getWidth();
                        final float bh = box.getHeight();
                        final float sx;
                        if( 1 == inResize ) {
                            sx = scale.x() + sdx/bw; // bottom-right
                        } else {
                            sx = scale.x() - sdx/bw; // bottom-left
                        }
                        final float sy = scale.y() - sdy/bh;
                        if( resize_sxy_min <= sx && resize_sxy_min <= sy ) { // avoid scale flip
                            if( DEBUG ) {
                                System.err.printf("DragZoom: resize %d, win[%4d, %4d], , flip[x %b, y %b], obj[%s], dxy +[%s], sdxy +[%.4f, %.4f], scale [%s] -> [%.4f, %.4f]%n",
                                        inResize, glWinX, glWinY, x_flip, y_flip, objPos,
                                        shapeEvent.objDrag, sdx, sdy,
                                        scale, sx, sy);
                            }
                            if( 1 == inResize ) {
                                move(   0, sdy, 0f); // bottom-right, sticky left- and top-edge
                            } else {
                                move( sdx, sdy, 0f); // bottom-left, sticky right- and top-edge
                            }
                            setScale(sx, sy, scale.z());
                        }
                        return; // FIXME: pass through event? Issue zoom event?
                    } else if( inMove ) {
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
     * @param pmv well formed PMVMatrix for this shape
     * @param viewport the viewport
     * @param objPos object position of mouse event relative to this shape
     */
    /* pp */ final void dispatchGestureEvent(final GestureEvent e, final int glWinX, final int glWinY, final PMVMatrix pmv, final Recti viewport, final Vec3f objPos) {
        if( interactive && resizable && e instanceof PinchToZoomGesture.ZoomEvent ) {
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
                System.err.printf("DragZoom: resize %b, win %4d/%4d, obj %s, %s + %.3f/%.3f -> %.3f/%.3f%n",
                        inResize, glWinX, glWinY, objPos, position, dx, dy, sx, sy);
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

    protected abstract void drawImpl0(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount, float[] rgba);

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
            final float s1Z = s1.getBounds().getMinZ()+s1.getPosition().z();
            final float s2Z = s2.getBounds().getMinZ()+s2.getPosition().z();
            if( FloatUtil.isEqual(s1Z, s2Z, FloatUtil.EPSILON) ) {
                return 0;
            } else if( s1Z < s2Z ){
                return -1;
            } else {
                return 1;
            }
        } };

    //
    //
    //
}
