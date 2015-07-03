/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.graph.demos.ui;

import java.util.ArrayList;

import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.opengl.GL2ES2;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.newt.event.GestureHandler.GestureEvent;
import com.jogamp.newt.event.GestureHandler.GestureListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.geom.AABBox;

public abstract class UIShape {
    public static final boolean DRAW_DEBUG_BOX = false;

    protected static final int DIRTY_SHAPE    = 1 << 0 ;
    protected static final int DIRTY_STATE    = 1 << 1 ;

    private final Factory<? extends Vertex> vertexFactory;
    private final int renderModes;
    protected final AABBox box;

    protected final float[] translate = new float[] { 0f, 0f, 0f };
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
    private boolean enabled = true;
    private ArrayList<MouseGestureListener> mouseListeners = new ArrayList<MouseGestureListener>();

    public UIShape(final Factory<? extends Vertex> factory, final int renderModes) {
        this.vertexFactory = factory;
        this.renderModes = renderModes;
        this.box = new AABBox();
    }

    public void setName(final int name) { this.name = name; }
    public int getName() { return this.name; }

    public final Vertex.Factory<? extends Vertex> getVertexFactory() { return vertexFactory; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(final boolean v) { enabled = v; }

    /**
     * Clears all data and reset all states as if this instance was newly created
     * @param gl TODO
     * @param renderer TODO\
     */
    public void clear(final GL2ES2 gl, final RegionRenderer renderer) {
        clearImpl(gl, renderer);
        translate[0] = 0f;
        translate[1] = 0f;
        translate[2] = 0f;
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
        translate[0] = 0f;
        translate[1] = 0f;
        translate[2] = 0f;
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

    public void setTranslate(final float tx, final float ty, final float tz) {
        translate[0] = tx;
        translate[1] = ty;
        translate[2] = tz;
        // System.err.println("UIShape.setTranslate: "+tx+"/"+ty+"/"+tz+": "+toString());
    }
    public void translate(final float tx, final float ty, final float tz) {
        translate[0] += tx;
        translate[1] += ty;
        translate[2] += tz;
        // System.err.println("UIShape.translate: "+tx+"/"+ty+"/"+tz+": "+toString());
    }
    public final float[] getTranslate() { return translate; }
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

    protected GLRegion createGLRegion() {
        return GLRegion.create(renderModes, null);
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
                region = createGLRegion();
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

    public final void setColor(final float r, final float g, final float b, final float a) {
        this.rgbaColor[0] = r;
        this.rgbaColor[1] = g;
        this.rgbaColor[2] = b;
        this.rgbaColor[3] = a;
    }
    public final void setPressedColorMod(final float r, final float g, final float b, final float a) {
        this.pressedRGBAModulate[0] = r;
        this.pressedRGBAModulate[1] = g;
        this.pressedRGBAModulate[2] = b;
        this.pressedRGBAModulate[3] = a;
    }
    public final void setToggleOnColorMod(final float r, final float g, final float b, final float a) {
        this.toggleOnRGBAModulate[0] = r;
        this.toggleOnRGBAModulate[1] = g;
        this.toggleOnRGBAModulate[2] = b;
        this.toggleOnRGBAModulate[3] = a;
    }
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
        return "enabled "+enabled+", toggle[able "+toggleable+", state "+toggle+"], "+translate[0]+" / "+translate[1]+", box "+box;
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
     * {@link UIShape} event details for propagated {@link NEWTEvent}s
     * containing reference of {@link #shape the intended shape} as well as
     * the {@link #objPos rotated relative position} and {@link #rotBounds bounding box}.
     * The latter fields are also normalized to lower-left zero origin, allowing easier usage.
     */
    public static class PointerEventInfo {
        /** The intended {@link UIShape} instance for this event */
        public final UIShape shape;
        /** The relative pointer position inside the intended {@link UIShape}. */
        public final float[] objPos;
        /** window x-position in OpenGL model space */
        public final int glWinX;
        /** window y-position in OpenGL model space */
        public final int glWinY;

        PointerEventInfo(final int glWinX, final int glWinY, final UIShape shape, final float[] objPos) {
            this.glWinX = glWinX;
            this.glWinY = glWinY;
            this.shape = shape;
            this.objPos = objPos;
        }

        public String toString() {
            return "EventDetails[winPos ["+glWinX+", "+glWinY+"], objPos ["+objPos[0]+", "+objPos[1]+", "+objPos[2]+"], "+shape+"]";
        }
    }

    /**
     * @param e original Newt {@link GestureEvent}
     * @param glWinX x-position in OpenGL model space
     * @param glWinY y-position in OpenGL model space
     */
    public final void dispatchGestureEvent(final GestureEvent e, final int glWinX, final int glWinY, final float[] objPos) {
        e.setAttachment(new PointerEventInfo(glWinX, glWinY, this, objPos));
        for(int i = 0; !e.isConsumed() && i < mouseListeners.size(); i++ ) {
            mouseListeners.get(i).gestureDetected(e);
        }
    }

    /**
     *
     * @param e original Newt {@link MouseEvent}
     * @param glX x-position in OpenGL model space
     * @param glY y-position in OpenGL model space
     */
    public final void dispatchMouseEvent(final MouseEvent e, final int glWinX, final int glWinY, final float[] objPos) {
        e.setAttachment(new PointerEventInfo(glWinX, glWinY, this, objPos));

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
        final float tw = box.getWidth();
        final float th = box.getHeight();

        final float minX = box.getMinX();
        final float minY = box.getMinY();
        final float z = box.getMinZ() + 0.025f;

        // CCW!
        shape.addVertex(minX,    minY,      z, true);
        shape.addVertex(minX+tw, minY,      z, true);
        shape.addVertex(minX+tw, minY + th, z, true);
        shape.addVertex(minX,    minY + th, z, true);
        shape.closeLastOutline(true);

        return shape;
    }

}
