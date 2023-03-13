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
package com.jogamp.graph.ui.gl;

import java.util.ArrayList;

import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.newt.event.GestureHandler.GestureEvent;
import com.jogamp.newt.event.GestureHandler.GestureListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.newt.event.PinchToZoomGesture;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseEvent.PointerClass;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * GraphUI Shape
 * <p>
 * A shape includes the following build-in user-interactions
 * - drag shape w/ 1-pointer click, see {@link #setDraggable(boolean)}
 * - resize shape w/ 1-pointer click and drag in 1/4th bottom-left and bottom-right corner, see {@link #setResizable(boolean)}.
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
    public static final boolean DRAW_DEBUG_BOX = false;
    private static final boolean DEBUG = false;

    protected static final int DIRTY_SHAPE    = 1 << 0 ;
    protected static final int DIRTY_STATE    = 1 << 1 ;

    private final Factory<? extends Vertex> vertexFactory;
    private final int renderModes;
    protected final AABBox box;

    protected final AffineTransform tempT1 = new AffineTransform();
    protected final AffineTransform tempT2 = new AffineTransform();
    protected final AffineTransform tempT3 = new AffineTransform();
    protected final AffineTransform tempT4 = new AffineTransform();

    protected final float[] position = new float[] { 0f, 0f, 0f };
    protected final Quaternion rotation = new Quaternion();
    protected final float[] rotOrigin = new float[] { 0f, 0f, 0f };
    protected final float[] scale = new float[] { 1f, 1f, 1f };

    protected GLRegion region = null;
    protected int regionQuality = Region.MAX_QUALITY;

    protected int dirty = DIRTY_SHAPE | DIRTY_STATE;
    protected float shapesSharpness = OutlineShape.DEFAULT_SHARPNESS;

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
    private boolean enabled = true;
    private ArrayList<MouseGestureListener> mouseListeners = new ArrayList<MouseGestureListener>();

    public Shape(final Factory<? extends Vertex> factory, final int renderModes) {
        this.vertexFactory = factory;
        this.renderModes = renderModes;
        this.box = new AABBox();
    }

    /** Set a symbolic name for this shape for identification. Default is -1 for noname. */
    public void setName(final int name) { this.name = name; }
    /** Return the optional symbolic name for this shape. */
    public int getName() { return this.name; }

    public final Vertex.Factory<? extends Vertex> getVertexFactory() { return vertexFactory; }

    /** Returns true if this shape is enabled and hence visible, otherwise false. */
    public boolean isEnabled() { return enabled; }
    /** Enable or disable this shape, i.e. its visibility. */
    public void setEnabled(final boolean v) { enabled = v; }

    /**
     * Clears all data and reset all states as if this instance was newly created
     * @param gl TODO
     * @param renderer TODO
     */
    public void clear(final GL2ES2 gl, final RegionRenderer renderer) {
        clearImpl(gl, renderer);
        position[0] = 0f;
        position[1] = 0f;
        position[2] = 0f;
        rotation.setIdentity();
        rotOrigin[0] = 0f;
        rotOrigin[1] = 0f;
        rotOrigin[2] = 0f;
        scale[0] = 1f;
        scale[1] = 1f;
        scale[2] = 1f;
        box.reset();
        markShapeDirty();
    }

    /**
     * Destroys all data
     * @param gl
     * @param renderer
     */
    public void destroy(final GL2ES2 gl, final RegionRenderer renderer) {
        destroyImpl(gl, renderer);
        position[0] = 0f;
        position[1] = 0f;
        position[2] = 0f;
        rotation.setIdentity();
        rotOrigin[0] = 0f;
        rotOrigin[1] = 0f;
        rotOrigin[2] = 0f;
        scale[0] = 1f;
        scale[1] = 1f;
        scale[2] = 1f;
        box.reset();
        markShapeDirty();
    }

    public void setPosition(final float tx, final float ty, final float tz) {
        position[0] = tx;
        position[1] = ty;
        position[2] = tz;
        // System.err.println("UIShape.setTranslate: "+tx+"/"+ty+"/"+tz+": "+toString());
    }
    public void move(final float tx, final float ty, final float tz) {
        position[0] += tx;
        position[1] += ty;
        position[2] += tz;
        // System.err.println("UIShape.translate: "+tx+"/"+ty+"/"+tz+": "+toString());
    }
    public final float[] getPosition() { return position; }
    public final Quaternion getRotation() { return rotation; }
    public final float[] getRotationOrigin() { return rotOrigin; }
    public void setRotationOrigin(final float rx, final float ry, final float rz) {
        rotOrigin[0] = rx;
        rotOrigin[1] = ry;
        rotOrigin[2] = rz;
    }
    public void setScale(final float sx, final float sy, final float sz) {
        scale[0] = sx;
        scale[1] = sy;
        scale[2] = sz;
    }
    public void scale(final float sx, final float sy, final float sz) {
        scale[0] *= sx;
        scale[1] *= sy;
        scale[2] *= sz;
    }
    public final float[] getScale() { return scale; }

    public final void markShapeDirty() {
        dirty |= DIRTY_SHAPE;
    }
    public final boolean isShapeDirty() {
        return 0 != ( dirty & DIRTY_SHAPE ) ;
    }
    public final void markStateDirty() {
        dirty |= DIRTY_STATE;
    }
    public final boolean isStateDirty() {
        return 0 != ( dirty & DIRTY_STATE ) ;
    }

    public final AABBox getBounds() { return box; }

    public final int getRenderModes() { return renderModes; }

    public GLRegion getRegion(final GL2ES2 gl, final RegionRenderer renderer) {
        validate(gl, renderer);
        return region;
    }

    /**
     * Renders {@link OutlineShape} using local {@link GLRegion} which might be cached or updated.
     * <p>
     * No matrix operations (translate, scale, ..) are performed.
     * </p>
     * @param gl
     * @param renderer
     * @param sampleCount
     */
    public void drawShape(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount) {
        final float r, g, b, a;
        final boolean isPressed = isPressed(), isToggleOn = isToggleOn();
        final boolean modBaseColor = !Region.hasColorChannel( renderModes ) && !Region.hasColorTexture( renderModes );
        if( modBaseColor ) {
            if( isPressed ) {
                r = rgbaColor[0]*pressedRGBAModulate[0];
                g = rgbaColor[1]*pressedRGBAModulate[1];
                b = rgbaColor[2]*pressedRGBAModulate[2];
                a = rgbaColor[3]*pressedRGBAModulate[3];
            } else if( isToggleable() ) {
                if( isToggleOn ) {
                    r = rgbaColor[0]*toggleOnRGBAModulate[0];
                    g = rgbaColor[1]*toggleOnRGBAModulate[1];
                    b = rgbaColor[2]*toggleOnRGBAModulate[2];
                    a = rgbaColor[3]*toggleOnRGBAModulate[3];
                } else {
                    r = rgbaColor[0]*toggleOffRGBAModulate[0];
                    g = rgbaColor[1]*toggleOffRGBAModulate[1];
                    b = rgbaColor[2]*toggleOffRGBAModulate[2];
                    a = rgbaColor[3]*toggleOffRGBAModulate[3];
                }
            } else {
                r = rgbaColor[0];
                g = rgbaColor[1];
                b = rgbaColor[2];
                a = rgbaColor[3];
            }
        } else {
            if( isPressed ) {
                r = pressedRGBAModulate[0];
                g = pressedRGBAModulate[1];
                b = pressedRGBAModulate[2];
                a = pressedRGBAModulate[3];
            } else if( isToggleable() ) {
                if( isToggleOn ) {
                    r = toggleOnRGBAModulate[0];
                    g = toggleOnRGBAModulate[1];
                    b = toggleOnRGBAModulate[2];
                    a = toggleOnRGBAModulate[3];
                } else {
                    r = toggleOffRGBAModulate[0];
                    g = toggleOffRGBAModulate[1];
                    b = toggleOffRGBAModulate[2];
                    a = toggleOffRGBAModulate[3];
                }
            } else {
                r = rgbaColor[0];
                g = rgbaColor[1];
                b = rgbaColor[2];
                a = rgbaColor[3];
            }
        }
        renderer.getRenderState().setColorStatic(r, g, b, a);
        getRegion(gl, renderer).draw(gl, renderer, sampleCount);
    }

    protected GLRegion createGLRegion(final GLProfile glp) {
        return GLRegion.create(glp, renderModes, null);
    }

    /**
     * Validates the shape's underlying {@link GLRegion}.
     *
     * @param gl
     * @param renderer
     */
    public final void validate(final GL2ES2 gl, final RegionRenderer renderer) {
        if( isShapeDirty() || null == region ) {
            box.reset();
            if( null == region ) {
                region = createGLRegion(gl.getGLProfile());
            } else {
                region.clear(gl);
            }
            addShapeToRegion(gl, renderer);
            if( DRAW_DEBUG_BOX ) {
                region.clear(gl);
                final OutlineShape shape = new OutlineShape(renderer.getRenderState().getVertexFactory());
                shape.setSharpness(shapesSharpness);
                shape.setIsQuadraticNurbs();
                region.addOutlineShape(shape, null, rgbaColor);
            }
            region.setQuality(regionQuality);
            dirty &= ~(DIRTY_SHAPE|DIRTY_STATE);
        } else if( isStateDirty() ) {
            region.markStateDirty();
            dirty &= ~DIRTY_STATE;
        }
    }

    /**
     * Setup the pre-selected {@link GLMatrixFunc#GL_MODELVIEW} {@link PMVMatrix} for this object.
     * @param pmv the matrix
     */
    public void setTransform(final PMVMatrix pmv) {
        final float[] uiTranslate = getPosition();
        pmv.glTranslatef(uiTranslate[0], uiTranslate[1], uiTranslate[2]);

        final Quaternion quat = getRotation();
        final boolean rotate = !quat.isIdentity();
        final float[] uiScale = getScale();
        final boolean scale = !VectorUtil.isVec3Equal(uiScale, 0, VectorUtil.VEC3_ONE, 0, FloatUtil.EPSILON);
        if( rotate || scale ) {
            final float[] rotOrigin = getRotationOrigin();
            final boolean pivot = !VectorUtil.isVec3Zero(rotOrigin, 0, FloatUtil.EPSILON);
            if( pivot ) {
                pmv.glTranslatef(rotOrigin[0], rotOrigin[1], rotOrigin[2]);
            }
            if( scale ) {
                pmv.glScalef(uiScale[0], uiScale[1], uiScale[2]);
            }
            if( rotate ) {
                pmv.glRotate(quat);
            }
            if( pivot ) {
                pmv.glTranslatef(-rotOrigin[0], -rotOrigin[1], -rotOrigin[2]);
            }
        }
    }

    /**
     * Retrieve window surface size of this shape
     * <p>
     * The {@link RegionRenderer#getMatrix()} has to be setup properly for this object,
     * i.e. reshape for {@link GLMatrixFunc#GL_PROJECTION} and {@link #setTransform(PMVMatrix)} for {@link GLMatrixFunc#GL_MODELVIEW}.
     * </p>
     * @param renderer source of viewport and {@link PMVMatrix}
     * @param surfaceSize target surface size
     * @return true for successful gluProject(..) operation, otherwise false
     */
    public boolean getSurfaceSize(final RegionRenderer renderer, final int[/*2*/] surfaceSize) {
        boolean res = false;
        final int[/*4*/] viewport = renderer.getViewport(new int[4]);
        // System.err.println("UIShape::getSurfaceSize.VP "+viewport[0]+"/"+viewport[1]+" "+viewport[2]+"x"+viewport[3]);
        final float[] winCoordHigh = new float[3];
        final float[] winCoordLow = new float[3];
        final float[] high = getBounds().getHigh();
        final float[] low = getBounds().getLow();
        final PMVMatrix pmv = renderer.getMatrix();

        if( pmv.gluProject(high[0], high[1], high[2], viewport, 0, winCoordHigh, 0) ) {
            // System.err.printf("UIShape::surfaceSize.H: shape %d: obj [%f, %f, %f] -> win [%f, %f, %f]%n", getName(), high[0], high[1], high[2], winCoordHigh[0], winCoordHigh[1], winCoordHigh[2]);
            if( pmv.gluProject(low[0], low[1], low[2], viewport, 0, winCoordLow, 0) ) {
                // System.err.printf("UIShape::surfaceSize.L: shape %d: obj [%f, %f, %f] -> win [%f, %f, %f]%n", getName(), low[0], low[1], low[2], winCoordLow[0], winCoordLow[1], winCoordLow[2]);
                surfaceSize[0] = (int)(winCoordHigh[0] - winCoordLow[0]);
                surfaceSize[1] = (int)(winCoordHigh[1] - winCoordLow[1]);
                // System.err.printf("UIShape::surfaceSize.S: shape %d: %f x %f -> %d x %d%n", getName(), winCoordHigh[0] - winCoordLow[0], winCoordHigh[1] - winCoordLow[1], surfaceSize[0], surfaceSize[1]);
                res = true;
            }
        }
        return res;
    }

    /**
     * Map given object coordinate relative to this shape to window coordinates
     * <p>
     * The {@link RegionRenderer#getMatrix()} has to be setup properly for this object,
     * i.e. reshape for {@link GLMatrixFunc#GL_PROJECTION} and {@link #setTransform(PMVMatrix)} for {@link GLMatrixFunc#GL_MODELVIEW}.
     * </p>
     * @param renderer source of viewport and {@link PMVMatrix}
     * @param objPos object position relative to this shape's center
     * @param glWinPos target window position of objPos relative to this shape
     * @return true for successful gluProject(..) operation, otherwise false
     */
    public boolean objToWinCoord(final RegionRenderer renderer, final float[/*3*/] objPos, final int[/*2*/] glWinPos) {
        boolean res = false;
        final int[/*4*/] viewport = renderer.getViewport(new int[4]);
        // System.err.println("UIShape::objToWinCoordgetSurfaceSize.VP "+viewport[0]+"/"+viewport[1]+" "+viewport[2]+"x"+viewport[3]);
        final float[] winCoord = new float[3];
        final PMVMatrix pmv = renderer.getMatrix();

        if( pmv.gluProject(objPos[0], objPos[1], objPos[2], viewport, 0, winCoord, 0) ) {
            // System.err.printf("UIShape::objToWinCoord.0: shape %d: obj [%f, %f, %f] -> win [%f, %f, %f]%n", getName(), objPos[0], objPos[1], objPos[2], winCoord[0], winCoord[1], winCoord[2]);
            glWinPos[0] = (int)(winCoord[0]);
            glWinPos[1] = (int)(winCoord[1]);
            // System.err.printf("UIShape::objToWinCoord.X: shape %d: %f / %f -> %d / %d%n", getName(), winCoord[0], winCoord[1], glWinPos[0], glWinPos[1]);
            res = true;
        }
        return res;
    }

    /**
     * Map given gl-window-coordinates to object coordinates relative to this shape and its z-coordinate.
     * <p>
     * The {@link RegionRenderer#getMatrix()} has to be setup properly for this object,
     * i.e. reshape for {@link GLMatrixFunc#GL_PROJECTION} and {@link #setTransform(PMVMatrix)} for {@link GLMatrixFunc#GL_MODELVIEW}.
     * </p>
     * @param renderer source of viewport and {@link PMVMatrix}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param objPos target object position of glWinX/glWinY relative to this shape
     * @return @return true for successful gluProject(..) and gluUnProject(..) operations, otherwise false
     */
    public boolean winToObjCoord(final RegionRenderer renderer, final int glWinX, final int glWinY, final float[/*3*/] objPos) {
        boolean res = false;
        final float[] ctr = getBounds().getCenter();
        final int[] viewport = renderer.getViewport(new int[4]);
        final float[] tmp = new float[3];
        final PMVMatrix pmv = renderer.getMatrix();

        if( pmv.gluProject(ctr[0], ctr[1], ctr[2], viewport, 0, tmp, 0) ) {
            // System.err.printf("UIShape::winToObjCoord.0: shape %d: obj [%f, %f, %f] -> win [%f, %f, %f]%n", getName(), ctr[0], ctr[1], ctr[2], tmp[0], tmp[1], tmp[2]);
            if( pmv.gluUnProject(glWinX, glWinY, tmp[2], viewport, 0, objPos, 0) ) {
                // System.err.printf("UIShape::winToObjCoord.1: shape %d: win [%d, %d, %f] -> obj [%f, %f, %f]%n", getName(), glWinX, glWinY, tmp[2], objPos[0], objPos[1], objPos[2]);
                res = true;
            }
        }
        return res;
    }

    public float[] getColor() {
        return rgbaColor;
    }

    public final int getQuality() { return regionQuality; }
    public final void setQuality(final int q) {
        this.regionQuality = q;
        if( null != region ) {
            region.setQuality(q);
        }
    }
    public final void setSharpness(final float sharpness) {
        this.shapesSharpness = sharpness;
        markShapeDirty();
    }
    public final float getSharpness() {
        return shapesSharpness;
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
        return "enabled "+enabled+", toggle[able "+toggleable+", state "+toggle+"], "+position[0]+" / "+position[1]+", box "+box;
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

    public void setToggleable(final boolean toggleable) {
        this.toggleable = toggleable;
    }
    /**
     * Returns true if this shape is toggable,
     * i.e. rendered w/ {@link #setToggleOnColorMod(float, float, float, float)} or {@link #setToggleOffColorMod(float, float, float, float)}.
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
        }
        markStateDirty();
    }
    public boolean isToggleOn() { return toggle; }

    /**
     * Set whether this shape is draggable,
     * i.e. translated by 1-pointer-click and drag.
     * <p>
     * Default draggable is true.
     * </p>
     */
    public void setDraggable(final boolean draggable) {
        this.draggable = draggable;
    }
    public boolean isDraggable() {
        return draggable;
    }

    /**
     * Set whether this shape is resizable,
     * i.e. zoomed by 1-pointer-click and drag in 1/4th bottom-left and bottom-right corner.
     * <p>
     * Default resizable is true.
     * </p>
     */
    public void setResizable(final boolean resizable) {
        this.resizable = resizable;
    }
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
     * The latter is normalized to lower-left zero origin, allowing easier usage.
     */
    public static class EventInfo {
        /** The associated {@link Shape} for this event */
        public final Shape shape;
        /** The relative object coordinate of glWinX/glWinY to the associated {@link Shape}. */
        public final float[] objPos;
        /** The GL window coordinates, origin bottom-left */
        public final int[] winPos;
        /** The drag delta of the relative object coordinate of glWinX/glWinY to the associated {@link Shape}. */
        public final float[] objDrag = { 0f, 0f };
        /** The drag delta of GL window coordinates, origin bottom-left */
        public final int[] winDrag = { 0, 0 };

        /**
         * Ctor
         * @param glWinX in GL window coordinates, origin bottom-left
         * @param glWinY in GL window coordinates, origin bottom-left
         * @param shape associated shape
         * @param objPos relative object coordinate of glWinX/glWinY to the associated shape.
         */
        EventInfo(final int glWinX, final int glWinY, final Shape shape, final float[] objPos) {
            this.winPos = new int[] { glWinX, glWinY };
            this.shape = shape;
            this.objPos = objPos;
        }

        @Override
        public String toString() {
            return "EventDetails[winPos ["+winPos[0]+", "+winPos[1]+"], objPos ["+objPos[0]+", "+objPos[1]+", "+objPos[2]+"], "+shape+"]";
        }
    }

    private boolean dragFirst = false;
    private final float[] objDraggedFirst = { 0f, 0f }; // b/c its relative to Shape and we stick to it
    private final int[] winDraggedLast = { 0, 0 }; // b/c its absolute window pos
    private boolean inDrag = false;
    private int inResize = 0; // 1 br, 2 bl
    private static final float resize_sxy_min = 0.33f;
    private static final float resize_sxy_max = 20f;
    private static final float resize_section = 1f/5f; // resize action in a corner

    /**
     * Dispatch given NEWT mouse event to this shape
     * @param e original Newt {@link MouseEvent}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param objPos object position of mouse event relative to this shape
     */
    /* pp */ final void dispatchMouseEvent(final MouseEvent e, final int glWinX, final int glWinY, final float[] objPos) {
        final Shape.EventInfo shapeEvent = new EventInfo(glWinX, glWinY, this, objPos);

        final short eventType = e.getEventType();
        if( 1 == e.getPointerCount() ) {
            switch( eventType ) {
                case MouseEvent.EVENT_MOUSE_CLICKED:
                    toggle();
                    break;
                case MouseEvent.EVENT_MOUSE_PRESSED:
                    setPressed(true);
                    break;
                case MouseEvent.EVENT_MOUSE_RELEASED:
                    setPressed(false);
                    break;
            }
        }
        switch( eventType ) {
            case MouseEvent.EVENT_MOUSE_PRESSED:
                dragFirst = 1 == e.getPointerCount();
                break;
            case MouseEvent.EVENT_MOUSE_RELEASED:
                dragFirst = false;
                inDrag = false;
                inResize = 0;
                break;
            case MouseEvent.EVENT_MOUSE_DRAGGED: {
                // 1 pointer drag and potential drag-resize
                if(dragFirst) {
                    objDraggedFirst[0] = objPos[0];
                    objDraggedFirst[1] = objPos[1];
                    winDraggedLast[0] = glWinX;
                    winDraggedLast[1] = glWinY;
                    dragFirst=false;

                    final float ix = objPos[0]; // - position[0];
                    final float iy = objPos[1]; // - position[1];
                    final float minx_br = box.getMaxX() - box.getWidth() * resize_section;
                    final float miny_br = box.getMinY();
                    final float maxx_br = box.getMaxX();
                    final float maxy_br = box.getMinY() + box.getHeight() * resize_section;
                    if( minx_br <= ix && ix <= maxx_br &&
                        miny_br <= iy && iy <= maxy_br ) {
                        inResize = 1; // bottom-right
                    } else {
                        final float minx_bl = box.getMinX();
                        final float miny_bl = box.getMinY();
                        final float maxx_bl = box.getMinX() + box.getWidth() * resize_section;
                        final float maxy_bl = box.getMinY() + box.getHeight() * resize_section;
                        if( minx_bl <= ix && ix <= maxx_bl &&
                            miny_bl <= iy && iy <= maxy_bl ) {
                            inResize = 2; // bottom-left
                        } else {
                            inDrag = true;
                        }
                    }
                    if( DEBUG ) {
                        System.err.printf("Drag: drag %b, resize %b, obj %.3f/%.3f, %.3f/%.3f + %.3f/%.3f, %s%n",
                                inDrag, inResize,
                                ix, iy,
                                objPos[0], objPos[1],
                                shapeEvent.objDrag[0], shapeEvent.objDrag[1], box.toString());
                    }
                    return;
                }
                shapeEvent.objDrag[0] = objPos[0] - objDraggedFirst[0];
                shapeEvent.objDrag[1] = objPos[1] - objDraggedFirst[1];
                shapeEvent.winDrag[0] = glWinX - winDraggedLast[0];
                shapeEvent.winDrag[1] = glWinY - winDraggedLast[1];
                winDraggedLast[0] = glWinX;
                winDraggedLast[1] = glWinY;
                if( 1 == e.getPointerCount() ) {
                    if( 0 != inResize && resizable ) {
                        final float dx = shapeEvent.objDrag[0]/2f;
                        final float dy = shapeEvent.objDrag[1]/2f;
                        final float sx;
                        if( 1 == inResize ) {
                            sx = scale[0] + (  2f*dx/box.getWidth() ); // bottom-right
                        } else {
                            sx = scale[0] + ( -2f*dx/box.getWidth() ); // bottom-left
                        }
                        final float sy = scale[1] + ( -2f*dy/box.getHeight() );
                        if( resize_sxy_min <= sx && sx <= resize_sxy_max && resize_sxy_min <= sy && sy <= resize_sxy_max ) {
                            if( DEBUG ) {
                                System.err.printf("DragZoom: resize %b, obj %4d/%4d, %.3f/%.3f/%.3f %.3f/%.3f/%.3f + %.3f/%.3f -> %.3f/%.3f%n",
                                        inResize, glWinX, glWinY, objPos[0], objPos[1], objPos[2], position[0], position[1], position[2],
                                        dx, dy, sx, sy);
                            }
                            move(dx, dy, 0f);
                            setScale(sx, sy, scale[2]);
                        }
                        return; // FIXME: pass through event? Issue zoom event?
                    } else if( inDrag && draggable ) {
                        if( DEBUG ) {
                            System.err.printf("Drag: obj %.3f/%.3f + %.3f/%.3f%n",
                                    objPos[0], objPos[1], shapeEvent.objDrag[0], shapeEvent.objDrag[1]);
                        }
                        move(shapeEvent.objDrag[0], shapeEvent.objDrag[1], 0f);
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
     * @param renderer TODO
     * @param e original Newt {@link GestureEvent}
     * @param glWinX x-position in OpenGL model space
     * @param glWinY y-position in OpenGL model space
     */
    /* pp */ final void dispatchGestureEvent(final RegionRenderer renderer, final GestureEvent e, final int glWinX, final int glWinY, final float[] objPos) {
        if( resizable && e instanceof PinchToZoomGesture.ZoomEvent ) {
            final PinchToZoomGesture.ZoomEvent ze = (PinchToZoomGesture.ZoomEvent) e;
            final float pixels = ze.getDelta() * ze.getScale(); //
            final float[] objPos2 = { 0f, 0f, 0f };
            final int winX2 = glWinX + Math.round(pixels);
            final boolean ok;
            {
                final PMVMatrix pmv = renderer.getMatrix();
                pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
                pmv.glPushMatrix();
                setTransform(pmv);
                ok = winToObjCoord(null, winX2, glWinY, objPos2);
                pmv.glPopMatrix();
            }
            final float dx = objPos2[0];
            final float dy = objPos2[1];
            final float sx = scale[0] + ( dx/box.getWidth() ); // bottom-right
            final float sy = scale[1] + ( dy/box.getHeight() );
            if( DEBUG ) {
                System.err.printf("DragZoom: resize %b, obj %4d/%4d, %.3f/%.3f/%.3f %.3f/%.3f/%.3f + %.3f/%.3f -> %.3f/%.3f%n",
                        inResize, glWinX, glWinY, objPos[0], objPos[1], objPos[2], position[0], position[1], position[2],
                        dx, dy, sx, sy);
            }
            if( resize_sxy_min <= sx && sx <= resize_sxy_max && resize_sxy_min <= sy && sy <= resize_sxy_max ) {
                if( DEBUG ) {
                    System.err.printf("PinchZoom: pixels %f, obj %4d/%4d, %.3f/%.3f/%.3f %.3f/%.3f/%.3f + %.3f/%.3f -> %.3f/%.3f%n",
                            pixels, glWinX, glWinY, objPos[0], objPos[1], objPos[2], position[0], position[1], position[2],
                            dx, dy, sx, sy);
                }
                // move(dx, dy, 0f);
                setScale(sx, sy, scale[2]);
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

    protected abstract void clearImpl(GL2ES2 gl, RegionRenderer renderer);
    protected abstract void destroyImpl(GL2ES2 gl, RegionRenderer renderer);
    protected abstract void addShapeToRegion(GL2ES2 gl, RegionRenderer renderer);

    //
    //
    //

    protected OutlineShape createDebugOutline(final OutlineShape shape, final AABBox box) {
        final float d = 0.025f;
        final float tw = box.getWidth() + d*2f;
        final float th = box.getHeight() + d*2f;

        final float minX = box.getMinX() - d;
        final float minY = box.getMinY() - d;
        final float z = 0; // box.getMinZ() + 0.025f;

        // CCW!
        shape.moveTo(minX, minY, z);
        shape.lineTo(minX+tw, minY, z);
        shape.lineTo(minX+tw, minY + th, z);
        shape.lineTo(minX,    minY + th, z);
        shape.closePath();

        // shape.addVertex(minX,    minY,      z, true);
        // shape.addVertex(minX+tw, minY,      z, true);
        // shape.addVertex(minX+tw, minY + th, z, true);
        // shape.addVertex(minX,    minY + th, z, true);
        // shape.closeLastOutline(true);

        return shape;
    }

}
