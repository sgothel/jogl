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

import javax.media.nativewindow.NativeWindowException;
import javax.media.opengl.GL2ES2;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.math.geom.AABBox;

public abstract class UIShape {
    public static final boolean DRAW_DEBUG_BOX = false;

    private final Factory<? extends Vertex> vertexFactory;

    protected static final int DIRTY_SHAPE     = 1 << 0 ;
    protected int dirty = DIRTY_SHAPE;
    protected float shapesSharpness = OutlineShape.DEFAULT_SHARPNESS;

    protected final AABBox box;
    protected final float[] translate = new float[] { 0f, 0f, 0f };
    protected final Quaternion rotation = new Quaternion();
    protected final float[] rotOrigin = new float[] { 0f, 0f, 0f };
    protected final float[] scale = new float[] { 1f, 1f, 1f };

    protected GLRegion region = null;
    protected int regionQuality = 99;

    protected final float[] rgbaColor         = {0.6f, 0.6f, 0.6f, 1.0f};
    protected final float[] selectedRGBAModulate = {1.4f, 1.4f, 1.4f, 1.0f};

    private boolean down = false;
    private boolean toggle =false;
    private boolean toggleable = false;
    private boolean enabled = true;
    private ArrayList<MouseListener> mouseListeners = new ArrayList<MouseListener>();

    public UIShape(Factory<? extends Vertex> factory) {
        this.vertexFactory = factory;
        this.box = new AABBox();
    }

    public final Vertex.Factory<? extends Vertex> getVertexFactory() { return vertexFactory; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }

    /**
     * Clears all data and reset all states as if this instance was newly created
     * @param gl TODO
     * @param renderer TODO\
     */
    public void clear(GL2ES2 gl, RegionRenderer renderer) {
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
        dirty = DIRTY_SHAPE;
    }

    /**
     * Destroys all data
     * @param gl
     * @param renderer
     */
    public void destroy(GL2ES2 gl, RegionRenderer renderer) {
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
        dirty = DIRTY_SHAPE;
    }

    public void setTranslate(float tx, float ty, float tz) {
        translate[0] = tx;
        translate[1] = ty;
        translate[2] = tz;
        // System.err.println("UIShape.setTranslate: "+tx+"/"+ty+"/"+tz+": "+toString());
    }
    public void translate(float tx, float ty, float tz) {
        translate[0] += tx;
        translate[1] += ty;
        translate[2] += tz;
        // System.err.println("UIShape.translate: "+tx+"/"+ty+"/"+tz+": "+toString());
    }
    public final float[] getTranslate() { return translate; }
    public final Quaternion getRotation() { return rotation; }
    public final float[] getRotationOrigin() { return rotOrigin; }
    public void setRotationOrigin(float rx, float ry, float rz) {
        rotOrigin[0] = rx;
        rotOrigin[1] = ry;
        rotOrigin[2] = rz;
    }
    public void setScale(float sx, float sy, float sz) {
        scale[0] = sx;
        scale[1] = sy;
        scale[2] = sz;
    }
    public void scale(float sx, float sy, float sz) {
        scale[0] *= sx;
        scale[1] *= sy;
        scale[2] *= sz;
    }
    public final float[] getScale() { return scale; }

    public final void markDirty() {
        dirty = DIRTY_SHAPE;
    }
    public final boolean isShapeDirty() {
        return 0 != ( dirty & DIRTY_SHAPE ) ;
    }

    public final AABBox getBounds() { return box; }

    public GLRegion getRegion(GL2ES2 gl, RegionRenderer renderer) {
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
    public void drawShape(GL2ES2 gl, RegionRenderer renderer, int[] sampleCount) {
        final float r, g, b, a;
        final boolean isSelect;
        if( isPressed() || toggle ) {
            isSelect = true;
            r = rgbaColor[0]*selectedRGBAModulate[0];
            g = rgbaColor[1]*selectedRGBAModulate[1];
            b = rgbaColor[2]*selectedRGBAModulate[2];
            a = rgbaColor[3]*selectedRGBAModulate[3];
        } else {
            isSelect = false;
            r = rgbaColor[0];
            g = rgbaColor[1];
            b = rgbaColor[2];
            a = rgbaColor[3];
        }

        if( renderer.getRenderState().isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED) ) {
            gl.glClearColor(r, g, b, 0.0f);
        }
        final RenderState rs = renderer.getRenderState();

        if( Region.hasColorChannel( renderer.getRenderModes() ) ) {
            if( isSelect ) {
                rs.setColorStatic(selectedRGBAModulate[0], selectedRGBAModulate[1], selectedRGBAModulate[2], selectedRGBAModulate[3]);
            } else {
                rs.setColorStatic(1.0f, 1.0f, 1.0f, 1.0f);
            }
        } else {
            rs.setColorStatic(r, g, b, a);
        }
        getRegion(gl, renderer).draw(gl, renderer, sampleCount);
    }

    public final boolean validate(GL2ES2 gl, RegionRenderer renderer) {
        if( isShapeDirty() ) {
            box.reset();
            if( null == region ) {
                region = GLRegion.create(renderer.getRenderModes());
            } else {
                region.clear(gl, renderer);
            }
            addShapeToRegion(gl, renderer);
            if( DRAW_DEBUG_BOX ) {
                region.clear(gl, renderer);
                final OutlineShape shape = new OutlineShape(renderer.getRenderState().getVertexFactory());
                shape.setSharpness(shapesSharpness);
                shape.setIsQuadraticNurbs();
                region.addOutlineShape(shape, null, rgbaColor);
            }
            region.setQuality(regionQuality);
            dirty &= ~DIRTY_SHAPE;
            return true;
        } else {
            return false;
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
    public final void setSharpness(float sharpness) {
        this.shapesSharpness = sharpness;
        dirty = DIRTY_SHAPE;
    }
    public final float getSharpness() {
        return shapesSharpness;
    }

    public final void setColor(float r, float g, float b, float a) {
        this.rgbaColor[0] = r;
        this.rgbaColor[1] = g;
        this.rgbaColor[2] = b;
        this.rgbaColor[3] = a;
    }
    public final void setSelectedColorMod(float r, float g, float b, float a){
        this.selectedRGBAModulate[0] = r;
        this.selectedRGBAModulate[1] = g;
        this.selectedRGBAModulate[2] = b;
        this.selectedRGBAModulate[3] = a;
    }

    public String toString() {
        return getClass().getSimpleName()+"[enabled "+enabled+", "+translate[0]+" / "+translate[1]+", box "+box+"]";
    }

    //
    // Input
    //

    public void setPressed(boolean b) {
        this.down  = b;
        if(isToggleable() && b) {
            toggle = !toggle;
        }
    }

    public boolean isPressed() {
        return this.down;
    }

    public boolean isToggleable() {
        return toggleable;
    }

    public void setToggleable(boolean toggleable) {
        this.toggleable = toggleable;
    }

    public final void addMouseListener(MouseListener l) {
        if(l == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        ArrayList<MouseListener> clonedListeners = (ArrayList<MouseListener>) mouseListeners.clone();
        clonedListeners.add(l);
        mouseListeners = clonedListeners;
    }

    public final void removeMouseListener(MouseListener l) {
        if (l == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        ArrayList<MouseListener> clonedListeners = (ArrayList<MouseListener>) mouseListeners.clone();
        clonedListeners.remove(l);
        mouseListeners = clonedListeners;
    }

    /**
     * {@link UIShape} event details for propagated {@link NEWTEvent}s
     * containing reference of {@link #shape the intended shape} as well as
     * the {@link #rotPosition rotated relative position} and {@link #rotBounds bounding box}.
     * The latter fields are also normalized to lower-left zero origin, allowing easier usage.
     */
    public static class EventDetails {
        /** The intended {@link UIShape} instance for this event */
        public final UIShape shape;
        /** The {@link AABBox} of the intended {@link UIShape}, rotated about {@link UIShape#getRotation()} and normalized to lower-left zero origin.*/
        public final AABBox rotBounds;
        /** The relative mouse pointer position inside the intended {@link UIShape}, rotated about {@link UIShape#getRotation()} and normalized to lower-left zero origin. */
        public final float[] rotPosition;

        EventDetails(final UIShape shape, final AABBox rotatedBounds, final float[] rotatedRelPos) {
            this.shape = shape;
            this.rotBounds = rotatedBounds;
            this.rotPosition = rotatedRelPos;
        }

        public String toString() {
            return "EventDetails[pos "+rotPosition[0]+", "+rotPosition[1]+", "+rotPosition[2]+
                                 ", "+rotBounds+", "+shape+"]";
        }
    }

    /**
     *
     * @param e original Newt {@link MouseEvent}
     * @param glX x-position in OpenGL model space
     * @param glY x-position in OpenGL model space
     */
    public final void dispatchMouseEvent(final MouseEvent e, final int glX, final int glY) {
        // rotate bounding box and normalize to 0/0
        final Quaternion rot = getRotation();
        final float[] bLow = new float[3];
        VectorUtil.copyVec3(bLow, 0, getBounds().getLow(), 0);
        VectorUtil.scaleVec3(bLow, bLow, -1f);
        final AABBox rbox = new AABBox(getBounds());
        rbox.translate(bLow);
        rbox.rotate(rot);

        // get unrotated relative position within shape, rotate and normalize to 0/0
        final float[] relPos = new float[] { glX, glY, 0f };
        VectorUtil.subVec3(relPos, relPos, getTranslate());
        VectorUtil.addVec3(relPos, relPos, bLow);
        rot.rotateVector(relPos, 0, relPos, 0);

        // set as attachment
        e.setAttachment(new EventDetails(this, rbox, relPos));

        for(int i = 0; !e.isConsumed() && i < mouseListeners.size(); i++ ) {
            final MouseListener l = mouseListeners.get(i);
            switch(e.getEventType()) {
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
                    markDirty();
                    l.mousePressed(e);
                    break;
                case MouseEvent.EVENT_MOUSE_RELEASED:
                    markDirty();
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

    protected OutlineShape createDebugOutline(OutlineShape shape, AABBox box) {
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
