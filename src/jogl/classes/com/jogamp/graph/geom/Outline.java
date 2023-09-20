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
package com.jogamp.graph.geom;

import java.util.ArrayList;

import com.jogamp.math.FloatUtil;
import com.jogamp.math.VectorUtil;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.plane.AffineTransform;
import com.jogamp.math.geom.plane.Winding;
import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;



/** Define a single continuous stroke by control vertices.
 *  The vertices define the shape of the region defined by this
 *  outline. The Outline can contain a list of off-curve and on-curve
 *  vertices which define curved regions.
 *
 *  Note: An outline should be closed to be rendered as a region.
 *
 *  @see OutlineShape
 *  @see Region
 */
public class Outline implements Comparable<Outline> {

    private ArrayList<Vertex> vertices;
    private boolean closed;
    private final AABBox bbox;
    private boolean dirtyBBox;
    private Winding winding;
    private boolean dirtyWinding;

    /**Create an outline defined by control vertices.
     * An outline can contain off Curve vertices which define curved
     * regions in the outline.
     */
    public Outline() {
        vertices = new ArrayList<Vertex>(3);
        closed = false;
        bbox = new AABBox();
        dirtyBBox = false;
        winding = Winding.CCW;
        dirtyWinding = false;
    }

    /**
     * Copy ctor
     */
    public Outline(final Outline src) {
        final int count = src.vertices.size();
        vertices = new ArrayList<Vertex>(count);
        winding = src.getWinding();
        dirtyWinding = false;
        for(int i=0; i<count; i++) {
            vertices.add( src.vertices.get(i).clone() );
        }
        closed = src.closed;
        bbox = new AABBox(src.bbox);
        dirtyBBox = src.dirtyBBox;
    }

    /**
     * Copy ctor w/ enforced Winding
     * <p>
     * If the enforced {@link Winding} doesn't match the source Outline, the vertices reversed copied into this new instance.
     * </p>
     * @param src the source Outline
     * @param enforce {@link Winding} to be enforced on this copy
     */
    public Outline(final Outline src, final Winding enforce) {
        final int count = src.vertices.size();
        vertices = new ArrayList<Vertex>(count);
        final Winding had_winding = src.getWinding();;
        winding = had_winding;
        dirtyWinding = false;
        if( enforce != had_winding ) {
            for(int i=count-1; i>=0; --i) {
                vertices.add( src.vertices.get(i).clone() );
            }
            winding = enforce;
        } else {
            for(int i=0; i<count; ++i) {
                vertices.add( src.vertices.get(i).clone() );
            }
        }
        closed = src.closed;
        bbox = new AABBox(src.bbox);
        dirtyBBox = src.dirtyBBox;
    }

    /**
     * Sets {@link Winding} to this outline
     * <p>
     * If the enforced {@link Winding} doesn't match this Outline, the vertices are reversed.
     * </p>
     * @param enforce to be enforced {@link Winding}
     */
    public final void setWinding(final Winding enforce) {
        final Winding had_winding = getWinding();
        if( enforce != had_winding ) {
            final int count = vertices.size();
            final ArrayList<Vertex> ccw = new ArrayList<Vertex>(count);
            for(int i=count-1; i>=0; --i) {
                ccw.add(vertices.get(i));
            }
            vertices = ccw;
            winding = enforce;
        }
    }

    /**
     * Compute the winding of the {@link #getLastOutline()} using the {@link #area(ArrayList)} function over all of its vertices.
     * @return {@link Winding#CCW} or {@link Winding#CW}
     */
    public final Winding getWinding() {
        if( !dirtyWinding ) {
            return winding;
        }
        final int count = getVertexCount();
        if( 3 > count ) {
            winding = Winding.CCW;
        } else {
            winding = VectorUtil.getWinding( getVertices() );
        }
        dirtyWinding = false;
        return winding;
    }

    public final int getVertexCount() {
        return vertices.size();
    }

    /**
     * Appends a vertex to the outline loop/strip.
     * @param vertex Vertex to be added
     * @throws NullPointerException if the  {@link Vertex} element is null
     */
    public final void addVertex(final Vertex vertex) throws NullPointerException {
        addVertex(vertices.size(), vertex);
    }

    /**
     * Insert the {@link Vertex} element at the given {@code position} to the outline loop/strip.
     * @param position of the added Vertex
     * @param vertex Vertex object to be added
     * @throws NullPointerException if the  {@link Vertex} element is null
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position > getVertexNumber())
     */
    public final void addVertex(final int position, final Vertex vertex) throws NullPointerException, IndexOutOfBoundsException {
        if (null == vertex) {
            throw new NullPointerException("vertex is null");
        }
        vertices.add(position, vertex);
        if(!dirtyBBox) {
            bbox.resize(vertex.getCoord());
        }
        dirtyWinding = true;
    }

    /** Replaces the {@link Vertex} element at the given {@code position}.
     * <p>Sets the bounding box dirty, hence a next call to {@link #getBounds()} will validate it.</p>
     *
     * @param position of the replaced Vertex
     * @param vertex replacement Vertex object
     * @throws NullPointerException if the  {@link Outline} element is null
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position >= getVertexNumber())
     */
    public final void setVertex(final int position, final Vertex vertex) throws NullPointerException, IndexOutOfBoundsException {
        if (null == vertex) {
            throw new NullPointerException("vertex is null");
        }
        vertices.set(position, vertex);
        dirtyBBox = true;
        dirtyWinding = true;
    }

    public final Vertex getVertex(final int index){
        return vertices.get(index);
    }

    public int getVertexIndex(final Vertex vertex){
        return vertices.indexOf(vertex);
    }

    /** Removes the {@link Vertex} element at the given {@code position}.
     * <p>Sets the bounding box dirty, hence a next call to {@link #getBounds()} will validate it.</p>
     *
     * @param position of the to be removed Vertex
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position >= getVertexNumber())
     */
    public final Vertex removeVertex(final int position) throws IndexOutOfBoundsException {
        dirtyBBox = true;
        dirtyWinding = true;
        return vertices.remove(position);
    }

    public final boolean isEmpty(){
        return (vertices.size() == 0);
    }

    public final Vertex getLastVertex(){
        if(isEmpty()){
            return null;
        }
        return vertices.get(vertices.size()-1);
    }

    public final ArrayList<Vertex> getVertices() {
        return vertices;
    }

    /**
     * Use the given outline loop/strip.
     * <p>Validates the bounding box.</p>
     *
     * @param vertices the new outline loop/strip
     */
    public final void setVertices(final ArrayList<Vertex> vertices) {
        this.vertices = vertices;
        validateBoundingBox();
    }

    public final boolean isClosed() {
        return closed;
    }

    /**
     * Ensure this outline is closed.
     * <p>
     * Checks whether the last vertex equals to the first.
     * If not equal, it either appends a clone of the first vertex
     * or prepends a clone of the last vertex, depending on <code>closeTail</code>.
     * </p>
     * @param closeTail if true, a clone of the first vertex will be appended,
     *                  otherwise a clone of the last vertex will be prepended.
     * @return true if closing performed, otherwise false for NOP
     */
    public final boolean setClosed(final boolean closeTail) {
        this.closed = true;
        if( !isEmpty() ) {
            final Vertex first = vertices.get(0);
            final Vertex last = getLastVertex();
            if( !first.getCoord().isEqual( last.getCoord() ) ) {
                if( closeTail ) {
                    vertices.add(first.clone());
                } else {
                    vertices.add(0, last.clone());
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Return a transformed instance with all vertices are copied and transformed.
     */
    public final Outline transform(final AffineTransform t) {
        final Outline newOutline = new Outline();
        final int vsize = vertices.size();
        for(int i=0; i<vsize; i++) {
            final Vertex v = vertices.get(i);
            newOutline.addVertex(t.transform(v, new Vertex()));
        }
        newOutline.closed = this.closed;
        return newOutline;
    }

    private final void validateBoundingBox() {
        dirtyBBox = false;
        bbox.reset();
        for (int i=0; i<vertices.size(); i++) {
            bbox.resize(vertices.get(i).getCoord());
        }
    }

    public final AABBox getBounds() {
        if (dirtyBBox) {
            validateBoundingBox();
        }
        return bbox;
    }

    /**
     * Compare two outline's Bounding Box size.
     * @see AABBox#getSize()
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public final int compareTo(final Outline other) {
        final float thisSize = getBounds().getSize();
        final float otherSize = other.getBounds().getSize();
        if( FloatUtil.isEqual2(thisSize, otherSize) ) {
            return 0;
        } else if(thisSize < otherSize){
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * @param obj the Object to compare this Outline with
     * @return true if {@code obj} is an Outline, not null, equals bounds and equal vertices in the same order
     */
    @Override
    public boolean equals(final Object obj) {
        if( obj == this) {
            return true;
        }
        if( null == obj || !(obj instanceof Outline) ) {
            return false;
        }
        final Outline o = (Outline) obj;
        if(getVertexCount() != o.getVertexCount()) {
            return false;
        }
        if( !getBounds().equals( o.getBounds() ) ) {
            return false;
        }
        for (int i=getVertexCount()-1; i>=0; i--) {
            if( ! getVertex(i).equals( o.getVertex(i) ) ) {
                return false;
            }
        }
        return true;
    }
    @Override
    public final int hashCode() {
        throw new InternalError("hashCode not designed");
    }
    @Override
    public String toString() {
        // Avoid calling this.hashCode() !
        return getClass().getName() + "@" + Integer.toHexString(super.hashCode());
    }
}
