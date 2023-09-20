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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.jogamp.graph.ui.Shape.Visitor2;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.graph.ui.Shape.Visitor1;

/**
 * Container interface of UI {@link Shape}s
 * @see Scene
 * @see Shape
 */
public interface Container {

    /** Returns number of {@link Shape}s, see {@link #getShapes()}. */
    int getShapeCount();

    /** Returns added {@link Shape}s, see {@link #addShape(Shape)}. */
    List<Shape> getShapes();

    /** Adds a {@link Shape}. */
    void addShape(Shape s);

    /**
     * Removes given shape, w/o {@link Shape#destroy(com.jogamp.opengl.GL2ES2, com.jogamp.graph.curve.opengl.RegionRenderer) destroying} them.
     * @return the removed shape or null if not contained
     */
    Shape removeShape(Shape s);

    /**
     * Removes shape at given index, w/o {@link Shape#destroy(com.jogamp.opengl.GL2ES2, com.jogamp.graph.curve.opengl.RegionRenderer) destroying} them.
     * @return the removed shape
     * @throws IndexOutOfBoundsException if index is out of bounds, i.e. (index < 0 || index >= size())
     */
    Shape removeShape(final int idx);

    void addShapes(Collection<? extends Shape> shapes);

    /** Removes all given shapes, w/o {@link Shape#destroy(com.jogamp.opengl.GL2ES2, com.jogamp.graph.curve.opengl.RegionRenderer) destroying} them. */
    void removeShapes(Collection<? extends Shape> shapes);

    /** Removes all contained shapes, w/o {@link Shape#destroy(com.jogamp.opengl.GL2ES2, com.jogamp.graph.curve.opengl.RegionRenderer) destroying} them. */
    void removeAllShapes();

    boolean contains(Shape s);

    AABBox getBounds(final PMVMatrix4f pmv, Shape shape);

    /** Enable or disable {@link PMVMatrix4f#getFrustum()} culling per {@link Shape}. Default is disabled. */
    void setFrustumCullingEnabled(final boolean v);

    /** Return whether {@link #setFrustumCullingEnabled(boolean) frustum culling} is enabled. */
    boolean isFrustumCullingEnabled();

    /**
     * Traverses through the graph up until {@code shape} and apply {@code action} on it.
     * @param pmv
     * @param shape
     * @param action
     * @return true to signal operation complete, i.e. {@code shape} found, otherwise false
     */
    boolean forOne(final PMVMatrix4f pmv, final Shape shape, final Runnable action);

    /**
     * Traverses through the graph and apply {@link Visitor1#visit(Shape)} for each, stop if it returns true.
     * @param v
     * @return true to signal operation complete and to stop traversal, i.e. {@link Visitor1#visit(Shape)} returned true, otherwise false
     */
    boolean forAll(Visitor1 v);

    /**
     * Traverses through the graph and apply {@link Visitor2#visit(Shape, PMVMatrix4f)} for each, stop if it returns true.
     * @param pmv
     * @param v
     * @return true to signal operation complete and to stop traversal, i.e. {@link Visitor2#visit(Shape, PMVMatrix4f)} returned true, otherwise false
     */
    boolean forAll(final PMVMatrix4f pmv, Visitor2 v);

    /**
     * Traverses through the graph and apply {@link Visitor#visit(Shape, PMVMatrix4f)} for each, stop if it returns true.
     *
     * Each {@link Container} level is sorted using {@code sortComp}
     * @param sortComp
     * @param pmv
     * @param v
     * @return true to signal operation complete and to stop traversal, i.e. {@link Visitor2#visit(Shape, PMVMatrix4f)} returned true, otherwise false
     */
    boolean forSortedAll(final Comparator<Shape> sortComp, final PMVMatrix4f pmv, final Visitor2 v);
}