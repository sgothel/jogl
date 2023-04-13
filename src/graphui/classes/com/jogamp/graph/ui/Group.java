/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.PMVMatrix;

import jogamp.graph.ui.TreeTool;

/**
 * Group of UI {@link Shape}s, optionally utilizing a {@link Group.Layout}.
 * @see Scene
 * @see Shape
 * @see Group.Layout
 */
public class Group extends Shape implements Container {
    /** Layout for the GraphUI {@link Group}, called @ {@link Shape#validate(GL2ES2)} or {@link Shape#validate(GLProfile)}.  */
    public static interface Layout {
        /**
         * Performing the layout of {@link Group#getShapes()}, called @ {@link Shape#validate(GL2ES2)} or {@link Shape#validate(GLProfile)}.
         * <p>
         * According to the implemented layout, method
         * - may scale the {@Link Shape}s
         * - may move the {@Link Shape}s
         * - may reuse the given {@link PMVMatrix} `pmv`
         * - must update the given {@link AABBox} `box`
         * </p>
         * @param g the {@link Group} to layout
         * @param box the bounding box of {@link Group} to be updated by this method.
         * @param pmv a {@link PMVMatrix} which can be reused.
         */
        void layout(final Group g, final AABBox box, final PMVMatrix pmv);
    }

    private final List<Shape> shapes = new ArrayList<Shape>();
    private Layout layouter;

    /**
     * Create a Graph based {@link GLRegion} UI {@link Shape}.
     * <p>
     * Default is non-interactive, see {@link #setInteractive(boolean)}.
     * </p>
     */
    public Group() {
        this(null);
    }

    /**
     * Create a Graph based {@link GLRegion} UI {@link Shape} w/ given {@link Group.Layour}.
     * <p>
     * Default is non-interactive, see {@link #setInteractive(boolean)}.
     * </p>
     */
    public Group(final Layout l) {
        super();
        this.layouter = l;
        this.setInteractive(false);
    }

    /** Return current {@link Group.Layout}. */
    public Layout getLayout() { return layouter; }

    /** Set {@link Group.Layout}. */
    public Group setLayout(final Layout l) { layouter = l; return this; }

    @Override
    public List<Shape> getShapes() {
        return shapes;
    }
    @Override
    public void addShape(final Shape s) {
        shapes.add(s);
    }

    /** Removes given shape, keeps it alive. */
    @Override
    public void removeShape(final Shape s) {
        shapes.remove(s);
    }

    /** Removes all given shapes and destroys them. */
    public void removeShape(final GL2ES2 gl, final RegionRenderer renderer, final Shape s) {
        s.setDebugBox(0f);
        shapes.remove(s);
        s.destroy(gl, renderer);
    }

    @Override
    public void addShapes(final Collection<? extends Shape> shapes) {
        for(final Shape s : shapes) {
            addShape(s);
        }
    }
    /** Removes all given shapes, keeps them alive. */
    @Override
    public void removeShapes(final Collection<? extends Shape> shapes) {
        for(final Shape s : shapes) {
            removeShape(s);
        }
    }
    /** Removes all given shapes and destroys them. */
    public void removeShapes(final GL2ES2 gl, final RegionRenderer renderer, final Collection<? extends Shape> shapes) {
        for(final Shape s : shapes) {
            removeShape(gl, renderer, s);
        }
    }

    @Override
    public void removeAllShapes() {
        shapes.clear();
    }

    /** Removes all given shapes and destroys them. */
    public void removeAllShapes(final GL2ES2 gl, final RegionRenderer renderer) {
        final int count = shapes.size();
        for(int i=count-1; i>=0; --i) {
            removeShape(gl, renderer, shapes.get(i));
        }
    }

    @Override
    public boolean hasColorChannel() {
        return false; // FIXME
    }

    @Override
    protected final void clearImpl0(final GL2ES2 gl, final RegionRenderer renderer) {
        for(final Shape s : shapes) {
            // s.clearImpl0(gl, renderer);;
            s.clear(gl, renderer);;
        }
    }

    @Override
    protected final void destroyImpl0(final GL2ES2 gl, final RegionRenderer renderer) {
        for(final Shape s : shapes) {
            // s.destroyImpl0(gl, renderer);
            s.destroy(gl, renderer);;
        }
    }

    private boolean doFrustumCulling = false;

    @Override
    public final void setFrustumCullingEnabled(final boolean v) { doFrustumCulling = v; }

    @Override
    public final boolean isFrustumCullingEnabled() { return doFrustumCulling; }

    @Override
    protected final void drawImpl0(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount, final float[] rgba) {
        final PMVMatrix pmv = renderer.getMatrix();
        final int shapeCount = shapes.size();
        for(int i=0; i<shapeCount; i++) {
            final Shape shape = shapes.get(i);
            if( shape.isEnabled() ) {
                pmv.glPushMatrix();
                shape.setTransform(pmv);

                if( !doFrustumCulling || !pmv.getFrustum().isAABBoxOutside( shape.getBounds() ) ) {
                    if( null == rgba ) {
                        shape.drawToSelect(gl, renderer, sampleCount);
                    } else {
                        shape.draw(gl, renderer, sampleCount);
                    }
                }
                pmv.glPopMatrix();
            }
        }
    }

    @Override
    protected void validateImpl(final GLProfile glp, final GL2ES2 gl) {
        if( isShapeDirty() ) {
            final PMVMatrix pmv = new PMVMatrix();
            if( null != layouter ) {
                for(final Shape s : shapes) {
                    if( null != gl ) {
                        s.validate(gl);
                    } else {
                        s.validate(glp);
                    }
                }
                layouter.layout(this, box, pmv);
            } else {
                final AABBox tsbox = new AABBox();
                for(final Shape s : shapes) {
                    if( null != gl ) {
                        s.validate(gl);
                    } else {
                        s.validate(glp);
                    }
                    pmv.glPushMatrix();
                    s.setTransform(pmv);
                    s.getBounds().transformMv(pmv, tsbox);
                    pmv.glPopMatrix();
                    box.resize(tsbox);
                }
            }
        }
    }

    @Override
    public boolean contains(final Shape s) {
        if( shapes.contains(s) ) {
            return true;
        }
        for(final Shape shape : shapes) {
            if( shape instanceof Container ) {
                if( ((Container)shape).contains(s) ) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AABBox getBounds(final PMVMatrix pmv, final Shape shape) {
        pmv.reset();
        setTransform(pmv);
        final AABBox res = new AABBox();
        if( null == shape ) {
            return res;
        }
        forOne(pmv, shape, () -> {
            shape.getBounds().transformMv(pmv, res);
        });
        return res;
    }

    @Override
    public String getSubString() {
        return super.getSubString()+", shapes "+shapes.size();
    }

    @Override
    public boolean forOne(final PMVMatrix pmv, final Shape shape, final Runnable action) {
        return TreeTool.forOne(shapes, pmv, shape, action);
    }

    @Override
    public boolean forAll(final Visitor1 v) {
        return TreeTool.forAll(shapes, v);
    }

    @Override
    public boolean forAll(final PMVMatrix pmv, final Visitor2 v) {
        return TreeTool.forAll(shapes, pmv, v);
    }

    @Override
    public boolean forSortedAll(final Comparator<Shape> sortComp, final PMVMatrix pmv, final Visitor2 v) {
        return TreeTool.forSortedAll(sortComp, shapes, pmv, v);
    }
}

