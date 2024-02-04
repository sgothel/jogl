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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.jogamp.graph.ui.Shape.Visitor2;
import com.jogamp.math.Matrix4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.ui.Shape.Visitor1;

/**
 * Container interface of UI {@link Shape}s
 * @see Scene
 * @see Shape
 */
public interface Container {

    /** Returns number of {@link Shape}s, see {@link #getShapes()}. */
    int getShapeCount();

    /** Returns {@link #addShape(Shape) added} {@link Shape}s. */
    List<Shape> getShapes();

    /**
     * Returns {@link #addShape(Shape) added shapes} which are rendered and sorted by z-axis in ascending order toward z-near.
     * <p>
     * The rendered shapes are {@link Shape#isVisible() visible} and not deemed outside of this container due to {@link #isCullingEnabled() culling}.
     * </p>
     * <p>
     * Only rendered shapes are considered for picking/activation.
     * </p>
     * <p>
     * The returned list is data-race free, i.e. won't be mutated by the rendering thread
     * as it gets completely replace at each rendering loop using a local volatile reference.<br/>
     * Only when disposing the container, the list gets cleared, hence {@Link List#size()} shall be used in the loop.
     * </p>
     * @see #addShape(Shape)
     * @see #isCullingEnabled()
     * @see Shape#isVisible()
     * @see #isOutside(PMVMatrix4f, Shape)
     */
    List<Shape> getRenderedShapes();

    /** Adds a {@link Shape}. */
    void addShape(Shape s);

    /**
     * Removes given shape, w/o {@link Shape#destroy(GL2ES2, RegionRenderer)}.
     * @return the removed shape or null if not contained
     */
    Shape removeShape(final Shape s);

    /** Removes all given shapes, w/o {@link Shape#destroy(GL2ES2, RegionRenderer)}. */
    void removeShapes(Collection<? extends Shape> shapes);

    /**
     * Removes given shape with {@link Shape#destroy(GL2ES2, RegionRenderer)}, if contained.
     * @param gl GL2ES2 context
     * @param renderer
     * @param s the shape to be removed
     * @return true if given Shape is removed and destroyed
     */
    boolean removeShape(final GL2ES2 gl, final RegionRenderer renderer, final Shape s);

    void addShapes(Collection<? extends Shape> shapes);

    /** Removes all given shapes with {@link Shape#destroy(GL2ES2, RegionRenderer)}. */
    void removeShapes(final GL2ES2 gl, final RegionRenderer renderer, final Collection<? extends Shape> shapes);

    /** Removes all contained shapes with {@link Shape#destroy(GL2ES2, RegionRenderer)}. */
    void removeAllShapes(final GL2ES2 gl, final RegionRenderer renderer);

    boolean contains(Shape s);

    Shape getShapeByIdx(final int id);
    Shape getShapeByID(final int id);
    Shape getShapeByName(final String name);

    /** Returns {@link AABBox} dimension of given {@link Shape} from this container's perspective, i.e. world-bounds if performing from the {@link Scene}. */
    AABBox getBounds(final PMVMatrix4f pmv, Shape shape);

    /** Enable or disable {@link PMVMatrix4f#getFrustum() Project-Modelview (PMv) frustum} culling per {@link Shape} for this container. Default is disabled. */
    void setPMvCullingEnabled(final boolean v);

    /** Return whether {@link #setPMvCullingEnabled(boolean) Project-Modelview (PMv) frustum culling} is enabled for this container. */
    boolean isPMvCullingEnabled();

    /**
     * Return whether {@link #setPMvCullingEnabled(boolean) Project-Modelview (PMv) frustum culling}
     * or {@link Group#setClipMvFrustum(com.jogamp.math.geom.Frustum) Group's Modelview (Mv) frustum clipping}
     * is enabled for this container. Default is disabled.
     */
    boolean isCullingEnabled();

    /**
     * Returns whether the given {@link Shape} is completely outside of this container.
     * <p>
     * Note: If method returns false, the box may only be partially inside, i.e. intersects with this container
     * </p>
     * @param pmv current {@link PMVMatrix4f} of this container
     * @param shape the {@link Shape} to test
     * @see #isOutside2(Matrix4f, Shape, PMVMatrix4f)
     * @see Shape#isOutside()
     */
    public boolean isOutside(final PMVMatrix4f pmv, final Shape shape);

    /**
     * Returns whether the given {@link Shape} is completely outside of this container.
     * <p>
     * Note: If method returns false, the box may only be partially inside, i.e. intersects with this container
     * </p>
     * @param mvCont copy of the model-view {@link Matrix4f) of this container
     * @param shape the {@link Shape} to test
     * @param pmvShape current {@link PMVMatrix4f} of the shape to test
     * @see #isOutside(PMVMatrix4f, Shape)
     * @see Shape#isOutside()
     */
    public boolean isOutside2(final Matrix4f mvCont, final Shape shape, final PMVMatrix4f pmvShape);
}
