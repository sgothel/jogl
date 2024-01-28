/**
 * Copyright 2023-2024 JogAmp Community. All rights reserved.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Matrix4f;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.Cube;
import com.jogamp.math.geom.Frustum;
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
    private Shape[] drawShapeArray = new Shape[0]; // reduce memory re-alloc @ display
    private final List<Shape> renderedShapesB0 = new ArrayList<Shape>();
    private final List<Shape> renderedShapesB1 = new ArrayList<Shape>();
    private volatile List<Shape> renderedShapes = renderedShapesB1;
    /** Enforced fixed size. In case z-axis is NaN, its 3D z-axis will be adjusted. */
    private final Vec3f fixedSize = new Vec3f();
    private Layout layouter;
    private Rectangle border = null;

    private boolean relayoutOnDirtyShapes = true;
    private boolean widgetMode = false;
    private boolean clipOnBounds = false;
    private Frustum clipFrustum = null;

    /**
     * Create a group of {@link Shape}s w/o {@link Group.Layout}.
     * <p>
     * Default is non-interactive, see {@link #setInteractive(boolean)}.
     * </p>
     */
    public Group() {
        this(null, null, null, null);
    }

    /**
     * Create a group of {@link Shape}s w/ given {@link Group.Layout}.
     * <p>
     * Default is non-interactive, see {@link #setInteractive(boolean)}.
     * </p>
     * @param l optional {@link Layout}, maybe {@code null}
     */
    public Group(final Layout l) {
        this(null, l, null, null);
    }

    /**
     * Create a group of {@link Shape}s w/ given {@link Group.Layout} and {@link Shape}.
     * <p>
     * Default is non-interactive, see {@link #setInteractive(boolean)}.
     * </p>
     * @param name optional name for {@link #setName(String)}
     * @param l optional {@link Layout}, maybe {@code null}
     * @param fixedSize optional fixed size for {@link #setFixedSize(Vec2f)}
     * @param s optional {@link Shape} for {@link #addShape(Shape)}
     */
    public Group(final String name, final Layout l, final Vec2f fixedSize, final Shape s) {
        super();
        if( null != name ) {
            this.setName(name);
        }
        this.layouter = l;
        this.setInteractive(false);
        if( null != fixedSize ) {
            this.setFixedSize(fixedSize);
        }
        if( null != s ) {
            addShape(s);
        }
    }

    @Override
    public final boolean isGroup() { return true; }

    /** Return current {@link Group.Layout}. */
    public Layout getLayout() { return layouter; }

    /** Set {@link Group.Layout}. */
    public Group setLayout(final Layout l) { layouter = l; return this; }

    /** Enforce size of this group for all given 3 dimensions {@link #getBounds()} without adjusting 3D z-axis like {@link #setFixedSize(Vec2f)}. */
    public Group setFixedSize(final Vec3f v) { fixedSize.set(v); return this; }
    /**
     * Enforce size of this group to given 2 dimensions,
     * adjusting the 3D z-axis {@link #getBounds()} giving room for potential clipping via {@link #setClipOnBounds(boolean)} or {@link #setClipMvFrustum(Frustum)}.
     * @see #setFixedSize(Vec3f)
     */
    public Group setFixedSize(final Vec2f v) { fixedSize.set(v.x(), v.y(), Float.NaN); return this; }
    /** Returns borrowed fixed size instance, see {@link #setFixedSize(Vec3f)} and {@link #setFixedSize(Vec2f)}. */
    public Vec3f getFixedSize() { return fixedSize; }
    /** Returns given {@link Vec2f} instance set with 2 dimensions, see {@link #setFixedSize(Vec2f)}. */
    public Vec2f getFixedSize(final Vec2f out) { out.set(fixedSize.x(),  fixedSize.y()); return out; }

    /**
     * Enable Modelview (Mv) {@link Frustum} clipping on {@link #getBounds()} for this group and its shapes as follows
     * <ul>
     *   <li>Discard {@link Shape} {@link #draw(GL2ES2, RegionRenderer) rendering} if not intersecting {@code clip-box}.</li>
     *   <li>Otherwise perform pixel-accurate clipping inside the shader to {@code clip-box}.</li>
     * </ul>
     * <p>
     * {@link #setClipMvFrustum(Frustum)} takes precedence over {@link #setClipOnBounds(boolean)}.
     * </p>
     * <p>
     * With clipping enabled, the 3D z-axis {@link #getBounds()} depth
     * will be slightly increased for functional {@link Frustum} operation.
     * </p>
     * @param v boolean to toggle clipping
     * @return this instance for chaining
     * @see #setClipMvFrustum(Frustum)
     * @see #setFixedSize(Vec2f)
     * @see #setFixedSize(Vec3f)
     */
    public Group setClipOnBounds(final boolean v) { clipOnBounds = v; return this; }
    /** Returns {@link #setClipOnBounds(boolean)} value */
    public boolean getClipOnBounds() { return clipOnBounds; }

    /**
     * Enable Modelview (Mv) {@link Frustum} clipping on explicit given pre-multiplied w/ Mv-matrix {@code clip-box}
     * for this group and its shapes as follows
     * <ul>
     *   <li>Discard {@link Shape} {@link #draw(GL2ES2, RegionRenderer) rendering} if not intersecting {@code clip-box}.</li>
     *   <li>Otherwise perform pixel-accurate clipping inside the shader to {@code clip-box}.</li>
     * </ul>
     * <p>
     * {@link #setClipMvFrustum(Frustum)} takes precedence over {@link #setClipOnBounds(boolean)}.
     * </p>
     * <p>
     * With clipping enabled, the 3D z-axis {@link #getBounds()} depth
     * will be slightly increased for functional {@link Frustum} operation.
     * </p>
     * @param v {@link Frustum} pre-multiplied w/ Mv-matrix
     * @return this instance for chaining
     * @see #setClipOnBounds(boolean)
     * @see #setFixedSize(Vec2f)
     * @see #setFixedSize(Vec3f)
     */
    public Group setClipMvFrustum(final Frustum v) { clipFrustum = v; return this; }
    /** Returns {@link #setClipMvFrustum(Frustum)} value */
    public Frustum getClipMvFrustum() { return clipFrustum; }

    @Override
    public int getShapeCount() { return shapes.size(); }

    @Override
    public List<Shape> getShapes() { return shapes; }

    @Override
    public List<Shape> getRenderedShapes() { return renderedShapes; }

    @Override
    public void addShape(final Shape s) {
        shapes.add(s);
        s.setParent(this);
        markShapeDirty();
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
    public Shape removeShape(final Shape s) {
        if( shapes.remove(s) ) {
            s.setParent(null);
            markShapeDirty();
            return s;
        } else {
            return null;
        }
    }

    @Override
    public void removeShapes(final Collection<? extends Shape> shapes) {
        for(final Shape s : shapes) {
            removeShape(s);
        }
    }

    @Override
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
    @Override
    public void removeShapes(final GL2ES2 gl, final RegionRenderer renderer, final Collection<? extends Shape> shapes) {
        for(final Shape s : shapes) {
            removeShape(gl, renderer, s);
        }
    }

    @Override
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
    protected void clearImpl0(final GL2ES2 gl, final RegionRenderer renderer) {
        for(final Shape s : shapes) {
            // s.clearImpl0(gl, renderer);
            s.clear(gl, renderer);
        }
        shapes.clear();
        drawShapeArray = new Shape[0];
        renderedShapesB0.clear();
        renderedShapesB1.clear();
    }

    @Override
    protected void destroyImpl0(final GL2ES2 gl, final RegionRenderer renderer) {
        for(final Shape s : shapes) {
            // s.destroyImpl0(gl, renderer);
            s.destroy(gl, renderer);
        }
        shapes.clear();
        drawShapeArray = new Shape[0];
        renderedShapesB0.clear();
        renderedShapesB1.clear();
        if( null != border ) {
            border.destroy(gl, renderer);
            border = null;
        }
    }

    private boolean doFrustumCulling = false;

    @Override
    public final void setPMvCullingEnabled(final boolean v) { doFrustumCulling = v; }

    @Override
    public final boolean isPMvCullingEnabled() { return doFrustumCulling; }

    @Override
    public final boolean isCullingEnabled() { return doFrustumCulling || clipOnBounds || null != clipFrustum; }

    @Override
    public final boolean isOutside(final PMVMatrix4f pmv, final Shape shape) {
        final AABBox shapeBox = shape.getBounds();
        final boolean useClipFrustum = null != clipFrustum;
        if( useClipFrustum || clipOnBounds ) {
            final Frustum frustumMv = useClipFrustum ? clipFrustum : tempC00.set( box ).transform( pmv.getMv() ).updateFrustumPlanes(tempF00);
            pmv.pushMv();
            shape.applyMatToMv(pmv);
            final boolean res;
            if( doFrustumCulling && pmv.getFrustum().isOutside( shapeBox ) ) {
                res = true;
            } else {
                final Cube shapeMv = tempC01.set( shapeBox ).transform( pmv.getMv() );
                res = frustumMv.isOutside( shapeMv );
            }
            pmv.popMv();
            return res;
        } else if( doFrustumCulling ){
            pmv.pushMv();
            shape.applyMatToMv(pmv);
            final boolean res = pmv.getFrustum().isOutside( shapeBox );
            pmv.popMv();
            return res;
        } else {
            return false;
        }
    }
    @Override
    public boolean isOutside2(final Matrix4f mvCont, final Shape shape, final PMVMatrix4f pmvShape) {
        final AABBox shapeBox = shape.getBounds();
        final boolean useClipFrustum = null != clipFrustum;
        if( useClipFrustum || clipOnBounds ) {
            final Frustum frustumMv = useClipFrustum ? clipFrustum : tempC00.set( box ).transform( mvCont ).updateFrustumPlanes(tempF00);
            final boolean res;
            if( doFrustumCulling && pmvShape.getFrustum().isOutside( shapeBox ) ) {
                res = true;
            } else {
                final Cube shapeMv = tempC01.set( shapeBox ).transform( pmvShape.getMv() );
                res = frustumMv.isOutside( shapeMv );
            }
            return res;
        } else if( doFrustumCulling ){
            return pmvShape.getFrustum().isOutside( shapeBox );
        } else {
            return false;
        }
    }

    @Override
    protected void drawImpl0(final GL2ES2 gl, final RegionRenderer renderer, final Vec4f rgba) {
        final PMVMatrix4f pmv = renderer.getMatrix();
        final int shapeCount = shapes.size();
        Arrays.fill(drawShapeArray, null); // flush old refs
        final Shape[] shapeArray = shapes.toArray(drawShapeArray); // local-backup
        drawShapeArray = shapeArray; // keep backup
        Arrays.sort(shapeArray, 0, shapeCount, Shape.ZAscendingComparator);

        final List<Shape> iShapes = renderedShapes == renderedShapesB0 ? renderedShapesB1 : renderedShapesB0;
        iShapes.clear();

        final boolean useClipFrustum = null != clipFrustum;
        if( useClipFrustum || clipOnBounds ) {
            final Frustum origClipFrustum = renderer.getClipFrustum();

            final Frustum frustumMv = useClipFrustum ? clipFrustum : tempC00.set( box ).transform( pmv.getMv() ).updateFrustumPlanes(tempF00);
            renderer.setClipFrustum( frustumMv );

            for(int i=0; i<shapeCount; i++) {
                final Shape shape = shapeArray[i];
                if( shape.isVisible() ) {
                    pmv.pushMv();
                    shape.applyMatToMv(pmv);

                    final AABBox shapeBox = shape.getBounds();
                    final Cube shapeMv = tempC01.set( shapeBox ).transform( pmv.getMv() );

                    if( ( !frustumMv.isOutside( shapeMv ) ) &&
                        ( !doFrustumCulling || !pmv.getFrustum().isOutside( shapeBox ) ) )
                    {
                        shape.draw(gl, renderer);
                        iShapes.add(shape);
                    }
                    pmv.popMv();
                }
            }
            renderer.setClipFrustum(origClipFrustum);
        } else {
            for(int i=0; i<shapeCount; i++) {
                final Shape shape = shapeArray[i];
                if( shape.isVisible() ) {
                    pmv.pushMv();
                    shape.applyMatToMv(pmv);
                    if( !doFrustumCulling || !pmv.getFrustum().isOutside( shape.getBounds() ) ) {
                        shape.draw(gl, renderer);
                        iShapes.add(shape);
                    }
                    pmv.popMv();
                }
            }
        }
        renderedShapes = iShapes;
        if( null != border && border.isVisible() ) {
            border.draw(gl, renderer);
        }
    }
    private final Frustum tempF00 = new Frustum(); // OK, synchronized
    private final Cube tempC00 = new Cube(); // OK, synchronized
    private final Cube tempC01 = new Cube(); // OK, synchronized

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected final void drawToSelectImpl0(final GL2ES2 gl, final RegionRenderer renderer) {
        final PMVMatrix4f pmv = renderer.getMatrix();
        final Object[] shapesS = shapes.toArray();
        Arrays.sort(shapesS, (Comparator)Shape.ZAscendingComparator);

        final int shapeCount = shapesS.length;
        for(int i=0; i<shapeCount; i++) {
            final Shape shape = (Shape) shapesS[i];
            if( shape.isVisible() ) {
                pmv.pushMv();
                shape.applyMatToMv(pmv);

                if( !doFrustumCulling || !pmv.getFrustum().isOutside( shape.getBounds() ) ) {
                    shape.drawToSelect(gl, renderer);
                }
                pmv.popMv();
            }
        }
        if( null != border && border.isVisible() ) {
            border.drawToSelect(gl, renderer);
        }
    }

    /**
     * Set relayout on dirty shapes mode, defaults to true.
     * <p>
     * If relayouting on dirty shape mode is enabler (default),
     * {@link #isShapeDirty()} traverses through all shapes updating all dirty states of all its groups
     * provoking a relayout if required.
     * </p>
     */
    public void setRelayoutOnDirtyShapes(final boolean v) { relayoutOnDirtyShapes = v; }
    public boolean getRelayoutOnDirtyShapes() { return relayoutOnDirtyShapes; }

    /**
     * Toggles widget behavior for this group and all its elements, default is disabled.
     * <p>
     * Enabled widget behavior for a group causes
     * <ul>
     *   <li>the whole group to be shown on top on (mouse over) activation of one of its elements</li>
     *   <li>this group's {@link #addActivationListener(Listener)} to handle all it's elements activation events</li>
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
            s.addActivationListener(activationListener);
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

    /**
     * {@inheritDoc}
     * <p>
     * If re-layouting on dirty shape mode is enabled (default), see {@link #setRelayoutOnDirtyShapes(boolean)},
     * this method traverses through all shapes updating all dirty states of all its groups
     * provoking a re-layout if required.
     * </p>
     */
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
    protected void validateImpl(final GL2ES2 gl, final GLProfile glp) {
        if( isShapeDirty() ) {
            final boolean needsRMs = hasBorder() && null == border;
            GraphShape firstGS = null;

            // box has been reset
            final PMVMatrix4f pmv = new PMVMatrix4f();
            if( null != layouter ) {
                if( 0 == shapes.size() ) {
                    box.resize(0, 0, 0);
                } else {
                    for(final Shape s : shapes) {
                        if( needsRMs && null == firstGS && s instanceof GraphShape ) {
                            firstGS = (GraphShape)s;
                        }
                        layouter.preValidate(s);
                        s.validate(gl, glp);
                    }
                    layouter.layout(this, box, pmv);
                }
            } else if( 0 == shapes.size() ) {
                box.resize(0, 0, 0);
            } else {
                final AABBox tsbox = new AABBox();
                for(final Shape s : shapes) {
                    if( needsRMs && null == firstGS && s instanceof GraphShape ) {
                        firstGS = (GraphShape)s;
                    }
                    s.validate(gl, glp);
                    pmv.pushMv();
                    s.applyMatToMv(pmv);
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
            final boolean useFixedSize = !FloatUtil.isZero(fixedSize.x()) && !FloatUtil.isZero(fixedSize.y());
            final boolean useClipping = null != clipFrustum || clipOnBounds;
            if( useFixedSize || useClipping ) {
                // final AABBox old = new AABBox(box);
                final boolean adjustZ = useClipping || ( useFixedSize && Float.isNaN(fixedSize.z()) );
                final Vec3f lo = box.getLow();
                if( adjustZ ) {
                    final float oldDepth = box.getDepth();
                    final Vec3f hi;
                    final float zAdjustment = 10f*Scene.DEFAULT_ACTIVE_ZOFFSET_SCALE*Scene.DEFAULT_Z16_EPSILON;
                    lo.add(                0,             0,         -(1f*zAdjustment));
                    if( useFixedSize ) {
                        hi = new Vec3f(lo);
                        hi.add(fixedSize.x(), fixedSize.y(), oldDepth+(2f*zAdjustment));
                    } else {
                        hi = box.getHigh();
                        hi.add(        0,             0,     oldDepth+(1f*zAdjustment));
                    }
                    box.setSize(lo, hi);
                } else if( useFixedSize ) {
                    final Vec3f hi = useFixedSize ? new Vec3f(lo) : box.getHigh();

                    hi.add(fixedSize.x(), fixedSize.y(), fixedSize.z());
                    box.setSize(lo, hi);
                }
                // System.err.println("- was "+old);
                // System.err.println("- has "+box);
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
        return TreeTool.contains(this, s);
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
        return TreeTool.getShapeByID(this, id);
    }
    @Override
    public Shape getShapeByName(final String name) {
        return TreeTool.getShapeByName(this, name);
    }

    @Override
    public AABBox getBounds(final PMVMatrix4f pmv, final Shape shape) {
        pmv.reset();
        applyMatToMv(pmv);
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
        return TreeTool.forOne(this, pmv, shape, action);
    }

    @Override
    public boolean forAll(final Visitor1 v) {
        return TreeTool.forAll(this, v);
    }

    @Override
    public boolean forAll(final PMVMatrix4f pmv, final Visitor2 v) {
        return TreeTool.forAll(this, pmv, v);
    }

    @Override
    public boolean forSortedAll(final Comparator<Shape> sortComp, final PMVMatrix4f pmv, final Visitor2 v) {
        return TreeTool.forSortedAll(this, sortComp, pmv, v);
    }

    @Override
    public boolean forAllRendered(final Comparator<Shape> sortComp, final PMVMatrix4f pmv, final Visitor2 v) {
        return TreeTool.forAllRendered(this, pmv, v);
    }
}

