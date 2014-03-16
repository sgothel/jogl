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

import jogamp.graph.geom.plane.AffineTransform;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.OutlineShapeXForm;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.geom.AABBox;

public abstract class UIShape {
    public static final boolean DRAW_DEBUG_BOX = false;

    private final Factory<? extends Vertex> vertexFactory;

    protected final ArrayList<OutlineShapeXForm> shapes;

    protected static final int DIRTY_SHAPE     = 1 << 0 ;
    protected static final int DIRTY_REGION    = 1 << 2 ;
    protected int dirty = DIRTY_SHAPE | DIRTY_REGION;

    protected final AABBox box;
    protected final float[] translate = new float[] { 0f, 0f, 0f };
    protected final Quaternion rotation = new Quaternion();
    protected final float[] scale = new float[] { 1f, 1f, 1f };

    protected final float[] shapeTranslate2D = new float[] { 0f, 0f };
    protected final float[] shapeScale2D = new float[] { 1f, 1f };
    private GLRegion region = null;

    protected final float[] color         = {0.6f, 0.6f, 0.6f};
    protected final float[] selectedColor = {0.8f, 0.8f, 0.8f};

    private boolean down = false;
    private boolean toggle =false;
    private boolean toggleable = false;
    private boolean enabled = true;
    private ArrayList<MouseListener> mouseListeners = new ArrayList<MouseListener>();

    public UIShape(Factory<? extends Vertex> factory) {
        this.vertexFactory = factory;
        this.shapes = new ArrayList<OutlineShapeXForm>();
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
        shapes.clear();
        translate[0] = 0f;
        translate[1] = 0f;
        translate[2] = 0f;
        rotation.setIdentity();
        scale[0] = 1f;
        scale[1] = 1f;
        scale[2] = 1f;
        shapeTranslate2D[0] = 0f;
        shapeTranslate2D[1] = 0f;
        shapeScale2D[0] = 1f;
        shapeScale2D[1] = 1f;
        box.reset();
        dirty = DIRTY_SHAPE | DIRTY_REGION;
    }

    /**
     * Destroys all data
     * @param gl
     * @param renderer
     */
    public void destroy(GL2ES2 gl, RegionRenderer renderer) {
        destroyImpl(gl, renderer);
        shapes.clear();
        translate[0] = 0f;
        translate[1] = 0f;
        translate[2] = 0f;
        rotation.setIdentity();
        scale[0] = 1f;
        scale[1] = 1f;
        scale[2] = 1f;
        shapeTranslate2D[0] = 0f;
        shapeTranslate2D[1] = 0f;
        shapeScale2D[0] = 1f;
        shapeScale2D[1] = 1f;
        box.reset();
        dirty = DIRTY_SHAPE | DIRTY_REGION;
    }

    public final void setTranslate(float tx, float ty, float tz) {
        translate[0] = tx;
        translate[1] = ty;
        translate[2] = tz;
    }
    public final void translate(float tx, float ty, float tz) {
        translate[0] += tx;
        translate[1] += ty;
        translate[2] += tz;
    }
    public final float[] getTranslate() { return translate; }
    public final Quaternion getRotation() { return rotation; }
    public final void setScale(float sx, float sy, float sz) {
        scale[0] = sx;
        scale[1] = sy;
        scale[2] = sz;
    }
    public final void scale(float sx, float sy, float sz) {
        scale[0] *= sx;
        scale[1] *= sy;
        scale[2] *= sz;
    }
    public final float[] getScale() { return scale; }

    public final void translateShape(float tx, float ty) {
        shapeTranslate2D[0] += tx;
        shapeTranslate2D[1] += ty;
    }
    public final void scaleShape(float sx, float sy) {
        shapeScale2D[0] *= sx;
        shapeScale2D[1] *= sy;
    }

    public final boolean isShapeDirty() {
        return 0 != ( dirty & DIRTY_SHAPE ) ;
    }

    public final boolean isRegionDirty() {
        return 0 != ( dirty & DIRTY_REGION ) ;
    }

    public ArrayList<OutlineShapeXForm> getShapes() { return shapes; }

    public final AABBox getBounds() { return box; }

    public GLRegion getRegion(GL2ES2 gl, RegionRenderer renderer) {
        validate(gl, renderer);
        if( isRegionDirty() ) {
            if( null == region ) {
                region = GLRegion.create(renderer.getRenderModes());
            } else {
                region.clear(gl, renderer);
            }
            addToRegion(region);
            dirty &= ~DIRTY_REGION;
            // System.err.println("XXX.UIShape: updated: "+region);
        }
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
        final float[] _color;
        if( isPressed() || toggle ){
            _color = selectedColor;
        } else {
            _color = color;

        }
        if( renderer.getRenderState().isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED) ) {
            gl.glClearColor(_color[0], _color[1], _color[2], 0.0f);
        }
        renderer.setColorStatic(gl, _color[0], _color[1], _color[2]);

        getRegion(gl, renderer).draw(gl, renderer, sampleCount);
    }

    public final boolean validate(GL2ES2 gl, RegionRenderer renderer) {
        if( isShapeDirty() ) {
            shapes.clear();
            box.reset();
            createShape(gl, renderer);
            if( DRAW_DEBUG_BOX ) {
                shapes.clear();
                final OutlineShape shape = new OutlineShape(renderer.getRenderState().getVertexFactory());
                shapes.add(new OutlineShapeXForm(createDebugOutline(shape, box), null));
            }
            dirty &= ~DIRTY_SHAPE;
            dirty |= DIRTY_REGION;
            return true;
        }
        return false;
    }

    private final void addToRegion(Region region) {
        final boolean hasLocTrans = 0f != shapeTranslate2D[0] || 0f != shapeTranslate2D[1];
        final boolean hasLocScale = 1f != shapeScale2D[0] || 1f != shapeScale2D[1];
        final AffineTransform t;
        if( hasLocScale || hasLocTrans ) {
            // System.err.printf("UIShape.addToRegion: locTranslate %f x %f, locScale %f x %f%n",
            //                                        shapeTranslate[0], shapeTranslate[1], shapeScale[0], shapeScale[1]);
            t = new AffineTransform();
            if( hasLocTrans ) {
                t.translate(shapeTranslate2D[0], shapeTranslate2D[1]);
            }
            if( hasLocScale ) {
                t.scale(shapeScale2D[0], shapeScale2D[1]);
            }
        } else {
            t = null;
        }
        final int shapeCount = shapes.size();
        for(int i=0; i<shapeCount; i++) {
            final OutlineShapeXForm tshape = shapes.get(i);
            final AffineTransform t2;
            if( null != tshape.t ) {
                if( null != t ) {
                    t2 = new AffineTransform(t).concatenate(tshape.t);
                } else {
                    t2 = tshape.t;
                }
            } else {
                t2 = t;
            }
            region.addOutlineShape(tshape.shape, t2);
        }
    }

    public float[] getColor() {
        return color;
    }

    public void setColor(float r, float g, float b) {
        this.color[0] = r;
        this.color[1] = g;
        this.color[2] = b;
    }
    public void setSelectedColor(float r, float g, float b){
        this.selectedColor[0] = r;
        this.selectedColor[1] = g;
        this.selectedColor[2] = b;
    }

    public String toString() {
        return getClass().getSimpleName()+"[enabled "+enabled+", box "+box+"]";
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

    public final void dispatchMouseEvent(MouseEvent e) {
        e.setAttachment(this);
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
    protected abstract void createShape(GL2ES2 gl, RegionRenderer renderer);

    //
    //
    //

    protected OutlineShape createDebugOutline(OutlineShape shape, AABBox box) {
        final float tw = box.getWidth();
        final float th = box.getHeight();

        final float minX = box.getMinX();
        final float minY = box.getMinY();
        final float z = box.getMinZ() + 0.025f;

        shape.addVertex(minX,    minY,      z, true);
        shape.addVertex(minX+tw, minY,      z, true);
        shape.addVertex(minX+tw, minY + th, z, true);
        shape.addVertex(minX,    minY + th, z, true);
        shape.closeLastOutline(true);

        return shape;
    }

}
