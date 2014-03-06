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

import javax.media.opengl.GL2ES2;

import jogamp.graph.geom.plane.AffineTransform;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.opengl.math.geom.AABBox;

public abstract class UIShape {
    private final Factory<? extends Vertex> vertexFactory;

    public class TransformedShape {
        public final OutlineShape shape;
        public final AffineTransform t;

        public TransformedShape(final OutlineShape shape, final AffineTransform t) {
            this.shape = shape;
            this.t = t;
        }
    }
    protected final ArrayList<TransformedShape> shapes;

    protected static final int DIRTY_SHAPE     = 1 << 0 ;
    protected static final int DIRTY_POSITION  = 1 << 1 ;
    protected static final int DIRTY_REGION    = 1 << 2 ;
    protected int dirty = DIRTY_SHAPE | DIRTY_POSITION | DIRTY_REGION;

    protected final AABBox box;
    protected final AffineTransform transform;
    private GLRegion region = null;

    protected final float[] color         = {0.6f, 0.6f, 0.6f};
    protected final float[] selectedColor = {0.8f, 0.8f, 0.8f};

    private boolean down = false;
    private boolean toggle =false;
    private boolean toggleable = false;

    public UIShape(Factory<? extends Vertex> factory) {
        this.vertexFactory = factory;
        this.shapes = new ArrayList<TransformedShape>();
        this.box = new AABBox();
        this.transform = new AffineTransform(factory);
    }

    public final Vertex.Factory<? extends Vertex> getVertexFactory() { return vertexFactory; }

    /**
     * Clears all data and reset all states as if this instance was newly created
     * @param gl TODO
     * @param renderer TODO\
     */
    public void clear(GL2ES2 gl, RegionRenderer renderer) {
        clearImpl(gl, renderer);
        shapes.clear();
        transform.setToIdentity();
        box.reset();
        dirty = DIRTY_SHAPE | DIRTY_POSITION | DIRTY_REGION;
    }

    /**
     * Destroys all data
     * @param gl
     * @param renderer
     */
    public void destroy(GL2ES2 gl, RegionRenderer renderer) {
        destroyImpl(gl, renderer);
        shapes.clear();
        transform.setToIdentity();
        box.reset();
        dirty = DIRTY_SHAPE | DIRTY_POSITION | DIRTY_REGION;
    }

    public final void translate(float tx, float ty) {
        transform.translate(tx, ty);
        dirty |= DIRTY_POSITION;
    }

    public final void scale(float sx, float sy, float sz) {
        transform.scale(sx, sy);
    }

    public final AffineTransform getTransform() {
        if( !isShapeDirty() ) {
            validatePosition();
        }
        return transform;
    }

    public final boolean isShapeDirty() {
        return 0 != ( dirty & DIRTY_SHAPE ) ;
    }

    public final boolean isPositionDirty() {
        return 0 != ( dirty & DIRTY_POSITION ) ;
    }

    public final boolean isRegionDirty() {
        return 0 != ( dirty & DIRTY_REGION ) ;
    }

    public ArrayList<TransformedShape> getShapes() { return shapes; }

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
            System.err.println("XXX.UIShape: updated: "+region);
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
     * @param select
     */
    public void drawShape(GL2ES2 gl, RegionRenderer renderer, int[] sampleCount, boolean select) {
        float[] _color = color;
        if( isPressed() || toggle ){
            _color = selectedColor;
        }
        if(!select){
            /**
            if( useBlending ) {
                // NOTE_ALPHA_BLENDING:
                // Due to alpha blending and VBAA, we need a background in text color
                // otherwise blending will amplify 'white'!
                gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            } */
            renderer.setColorStatic(gl, _color[0], _color[1], _color[2]);
        }
        getRegion(gl, renderer).draw(gl, renderer, sampleCount);
    }

    public final boolean validate(GL2ES2 gl, RegionRenderer renderer) {
        if( !validateShape(gl, renderer) ) {
            return validatePosition();
        }
        return true;
    }
    private final boolean validateShape(GL2ES2 gl, RegionRenderer renderer) {
        if( isShapeDirty() ) {
            shapes.clear();
            box.reset();
            createShape(gl, renderer);
            dirty &= ~DIRTY_SHAPE;
            dirty |= DIRTY_REGION;
            validatePosition();
            return true;
        }
        return false;
    }
    private final boolean validatePosition () {
        if( isPositionDirty() && !isShapeDirty() ) {
            // Subtract the bbox minx/miny from position, i.e. the shape's offset.
            final AABBox box = getBounds();
            final float minX = box.getMinX();
            final float minY = box.getMinY();
            System.err.println("XXX.UIShape: Position pre: " + transform.getTranslateX() + " " + transform.getTranslateY() + ", sbox "+box);
            translate(-minX, -minY);
            System.err.println("XXX.UIShape: Position post: " + transform.getTranslateX() + " " + transform.getTranslateY() + ", sbox "+box);
            dirty &= ~DIRTY_POSITION;
            return true;
        }
        return false;
    }

    private final void addToRegion(Region region) {
        final int shapeCount = shapes.size();
        for(int i=0; i<shapeCount; i++) {
            final TransformedShape tshape = shapes.get(i);
            region.addOutlineShape(tshape.shape, tshape.t);
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

    public void onClick() { }
    public void onPressed() { }
    public void onRelease() { }

    //
    //
    //

    protected abstract void clearImpl(GL2ES2 gl, RegionRenderer renderer);
    protected abstract void destroyImpl(GL2ES2 gl, RegionRenderer renderer);
    protected abstract void createShape(GL2ES2 gl, RegionRenderer renderer);

    //
    //
    //

}
