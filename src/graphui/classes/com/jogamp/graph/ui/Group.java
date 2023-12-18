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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;

import jogamp.graph.ui.TreeTool;

/**
 * Group of {@link Shape}s, optionally utilizing a {@link Group.Layout}.
 * @see Scene
 * @see Shape
 * @see Group.Layout
 */
public class Group extends Shape implements Container {
    /** Layout for the GraphUI {@link Group}, called @ {@link Shape#validate(GL2ES2)} or {@link Shape#validate(GLProfile)}.  */
    public static interface Layout {
        /** Prepare given {@link Shape} before {@link Shape#validate(GL2ES2) validation}, e.g. {@link Shape#setPaddding(Padding)}. */
        void preValidate(final Shape s);

        /**
         * Performing the layout of {@link Group#getShapes()}, called @ {@link Shape#validate(GL2ES2)} or {@link Shape#validate(GLProfile)}.
         * <p>
         * According to the implemented layout, method
         * - may scale the {@Link Shape}s
         * - may move the {@Link Shape}s
         * - may reuse the given {@link PMVMatrix4f} `pmv`
         * - must update the given {@link AABBox} `box`
         * </p>
         * @param g the {@link Group} to layout
         * @param box the bounding box of {@link Group} to be updated by this method.
         * @param pmv a {@link PMVMatrix4f} which can be reused.
         */
        void layout(final Group g, final AABBox box, final PMVMatrix4f pmv);
    }

    private final List<Shape> shapes = new CopyOnWriteArrayList<Shape>();
    private Layout layouter;
    private Rectangle border = null;

    /**
     * Create a group of {@link Shape}s w/o {@link Group.Layout}.
     * <p>
     * Default is non-interactive, see {@link #setInteractive(boolean)}.
     * </p>
     */
    public Group() {
        this(null);
    }

    /**
     * Create a group of {@link Shape}s w/ given {@link Group.Layout}.
     * <p>
     * Default is non-interactive, see {@link #setInteractive(boolean)}.
     * </p>
     * @param l optional {@link Layout}, maybe {@code null}
     */
    public Group(final Layout l) {
        super();
        this.layouter = l;
        this.setInteractive(false);
    }

    @Override
    public final boolean isGroup() { return true; }

    /** Return current {@link Group.Layout}. */
    public Layout getLayout() { return layouter; }

    /** Set {@link Group.Layout}. */
    public Group setLayout(final Layout l) { layouter = l; return this; }

    @Override
    public int getShapeCount() { return shapes.size(); }

    @Override
    public List<Shape> getShapes() { return shapes; }

    @Override
    public void addShape(final Shape s) {
        shapes.add(s);
        s.setParent(this);
        markShapeDirty();
    }

    /** Removes given shape, keeps it alive. */
    @Override
    public Shape removeShape(final Shape s) {
        if( shapes.remove(s) ) {
            s.setParent(null);
            markShapeDirty();
            return s;
        } else {
            return null;
        }
    }

    /**
     * Atomic replacement of the given {@link Shape} {@code remove} with {@link Shape} {@code replacement}.
     * @param remove the shape to be replaced
     * @param replacement the replacement shape to be inserted at same position
     * @return true if shape {@code remove} is contained and replaced by {@code replacement}, otherwise false.
     */
    public boolean replaceShape(final Shape remove, final Shape replacement) {
        final int idx = shapes.indexOf(remove);
        if( 0 > idx ) {
            return false;
        }
        if( null == shapes.remove(idx) ) {
            return false;
        }
        remove.setParent(null);
        shapes.add(idx, replacement);
        replacement.setParent(this);
        markShapeDirty();
        return true;
    }

    @Override
    public Shape removeShape(final int idx) {
        final Shape r = shapes.remove(idx);
        if( null != r ) {
            r.setParent(null);
            markShapeDirty();
        }
        return r;
    }

    /**
     * Removes given shape and destroy it, if contained.
     * @param gl GL2ES2 context
     * @param renderer
     * @param s the shape to be removed
     * @return true if given Shape is removed and destroyed
     */
    public boolean removeShape(final GL2ES2 gl, final RegionRenderer renderer, final Shape s) {
        if( shapes.remove(s) ) {
            s.setParent(null);
            markShapeDirty();
            s.destroy(gl, renderer);
            return true;
        } else {
            return false;
        }
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
        final int count = shapes.size();
        for(int i=count-1; i>=0; --i) {
            removeShape(i);
        }
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
        if( null != border ) {
            border.destroy(gl, renderer);
            border = null;
        }
    }

    private boolean doFrustumCulling = false;

    @Override
    public final void setFrustumCullingEnabled(final boolean v) { doFrustumCulling = v; }

    @Override
    public final boolean isFrustumCullingEnabled() { return doFrustumCulling; }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected final void drawImpl0(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount, final Vec4f rgba) {
        final PMVMatrix4f pmv = renderer.getMatrix();
        final Object[] shapesS = shapes.toArray();
        Arrays.sort(shapesS, (Comparator)Shape.ZAscendingComparator);

        final int shapeCount = shapesS.length;
        for(int i=0; i<shapeCount; i++) {
            final Shape shape = (Shape) shapesS[i];
            if( shape.isVisible() ) {
                pmv.pushMv();
                shape.setTransformMv(pmv);

                if( !doFrustumCulling || !pmv.getFrustum().isAABBoxOutside( shape.getBounds() ) ) {
                    if( null == rgba ) {
                        shape.drawToSelect(gl, renderer, sampleCount);
                    } else {
                        shape.draw(gl, renderer, sampleCount);
                    }
                }
                pmv.popMv();
            }
        }
        if( null != border ) {
            if( null == rgba ) {
                border.drawToSelect(gl, renderer, sampleCount);
            } else {
                border.draw(gl, renderer, sampleCount);
            }
        }
    }

    private boolean relayoutOnDirtyShapes = true;
    public void setRelayoutOnDirtyShapes(final boolean v) { relayoutOnDirtyShapes = v; }
    public boolean getRelayoutOnDirtyShapes() { return relayoutOnDirtyShapes; }

    private boolean widgetMode = false;
    /**
     * Toggles widget behavior for this group and all its elements, default is disabled.
     * <p>
     * Enabled widget behavior for a group causes
     * <ul>
     *   <li>the whole group to be shown on top on (mouse over) activation of one of its elements</li>
     *   <li>this group's {@link #onActivation(Listener)} to handle all it's elements activation events</li>
     *   <li>{@link #isActive()} of this group and its sub-groups to return true if one of its elements is active</li>
     * </ul>
     * </p>
     * <p>
     * This method modifies all elements of this group for enabled or disabled widget behavior.
     * </p>
     * @param v enable or disable
     * @return this group for chaining
     */
    public final Group setWidgetMode(final boolean v) {
        widgetMode = v;
        if( v ) {
            enableUniActivationImpl(true, forwardActivation);
        } else {
            enableUniActivationImpl(false, null);
        }
        return this;
    }
    protected final void enableUniActivationImpl(final boolean v, final Listener activationListener) {
        for(final Shape s : shapes ) {
            if( s.isGroup() ) {
                final Group sg = (Group)s;
                sg.setWidgetMode(v);
            }
            s.onActivation(activationListener);
        }
    }

    /** Returns whether {@link #setWidgetMode(boolean)} is enabled or disabled. */
    public final boolean getWidgetMode() { return widgetMode; }

    @Override
    public boolean isActive() {
        return super.isActive() || ( widgetMode && forAll((final Shape gs) -> { return gs.isActive(); } ) );
    }

    @Override
    public float getAdjustedZ() {
        final float[] v = { getAdjustedZImpl() };
        if( widgetMode && !super.isActive() ) {
            forAll((final Shape gs) -> {
                if( gs.isActive() ) {
                    v[0] = gs.getAdjustedZImpl();
                    return true;
                } else {
                    return false;
                }
            } );
        }
        return v[0];
    }

    @Override
    protected boolean isShapeDirty() {
        if( relayoutOnDirtyShapes ) {
            // Deep dirty state update:
            // - Ensure all group member's dirty state is updated
            // - Allowing all group member's validate to function
            for(final Shape s : shapes) {
                if( s.isShapeDirty() ) {
                    markShapeDirty();
                }
            }
        }
        return super.isShapeDirty();
    }

    @Override
    protected void validateImpl(final GLProfile glp, final GL2ES2 gl) {
        if( isShapeDirty() ) {
            final boolean needsRMs = hasBorder() && null == border;
            GraphShape firstGS = null;

            // box has been reset
            final PMVMatrix4f pmv = new PMVMatrix4f();
            if( null != layouter ) {
                for(final Shape s : shapes) {
                    if( needsRMs && null == firstGS && s instanceof GraphShape ) {
                        firstGS = (GraphShape)s;
                    }
                    layouter.preValidate(s);
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
                    if( needsRMs && null == firstGS && s instanceof GraphShape ) {
                        firstGS = (GraphShape)s;
                    }
                    if( null != gl ) {
                        s.validate(gl);
                    } else {
                        s.validate(glp);
                    }
                    pmv.pushMv();
                    s.setTransformMv(pmv);
                    s.getBounds().transform(pmv.getMv(), tsbox);
                    pmv.popMv();
                    box.resize(tsbox);
                }
            }
            if( hasPadding() ) {
                final Padding p = getPadding();
                final Vec3f l = box.getLow();
                final Vec3f h = box.getHigh();
                box.resize(l.x() - p.left, l.y() - p.bottom, l.z());
                box.resize(h.x() + p.right, h.y() + p.top, l.z());
                setRotationPivot( box.getCenter() );
            }
            if( hasBorder() ) {
                if( null == border ) {
                    final int firstRMs = null != firstGS ? firstGS.getRenderModes() : 0;
                    final int myRMs = Region.isVBAA(firstRMs) ? Region.VBAA_RENDERING_BIT : 0;
                    border = new Rectangle(myRMs, box, getBorderThickness());
                } else {
                    border.setVisible(true);
                    border.setBounds(box, getBorderThickness());
                }
                border.setColor(getBorderColor());
            } else if( null != border ) {
                border.setVisible(false);
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
    public Shape getShapeByIdx(final int id) {
        if( 0 > id ) {
            return null;
        }
        return shapes.get(id);
    }
    @Override
    public Shape getShapeByID(final int id) {
        for(final Shape b : shapes) {
            if(b.getID() == id ) {
                return b;
            }
        }
        return null;
    }
    @Override
    public Shape getShapeByName(final String name) {
        for(final Shape b : shapes) {
            if( b.getName().equals(name) ) {
                return b;
            }
        }
        return null;
    }

    @Override
    public AABBox getBounds(final PMVMatrix4f pmv, final Shape shape) {
        pmv.reset();
        setTransformMv(pmv);
        final AABBox res = new AABBox();
        if( null == shape ) {
            return res;
        }
        forOne(pmv, shape, () -> {
            shape.getBounds().transform(pmv.getMv(), res);
        });
        return res;
    }

    @Override
    public String getSubString() {
        return super.getSubString()+", shapes "+shapes.size();
    }

    @Override
    public boolean forOne(final PMVMatrix4f pmv, final Shape shape, final Runnable action) {
        return TreeTool.forOne(shapes, pmv, shape, action);
    }

    @Override
    public boolean forAll(final Visitor1 v) {
        return TreeTool.forAll(shapes, v);
    }

    @Override
    public boolean forAll(final PMVMatrix4f pmv, final Visitor2 v) {
        return TreeTool.forAll(shapes, pmv, v);
    }

    @Override
    public boolean forSortedAll(final Comparator<Shape> sortComp, final PMVMatrix4f pmv, final Visitor2 v) {
        return TreeTool.forSortedAll(sortComp, shapes, pmv, v);
    }
}

