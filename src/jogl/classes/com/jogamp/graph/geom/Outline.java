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
package com.jogamp.graph.geom;

import java.util.ArrayList;

import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.math.VectorUtil;



/** Define a single continuous stroke by control vertices.
 *  The vertices define the shape of the region defined by this 
 *  outline. The Outline can contain a list of off-curve and on-curve
 *  vertices which define curved regions.
 *  
 *  Note: An outline should be closed to be rendered as a region.
 *  
 *  @see OutlineShape, Region
 */
public class Outline implements Cloneable, Comparable<Outline> {

    private ArrayList<Vertex> vertices = new ArrayList<Vertex>(3);
    private boolean closed = false;
    private AABBox bbox = new AABBox();
    private boolean dirtyBBox = false;

    /**Create an outline defined by control vertices.
     * An outline can contain off Curve vertices which define curved
     * regions in the outline.
     */
    public Outline() {        
    }

    public final int getVertexCount() {
        return vertices.size();
    }

    /** Appends a vertex to the outline loop/strip.
     * @param vertex Vertex to be added
     * @throws NullPointerException if the  {@link Vertex} element is null 
     */
    public final void addVertex(Vertex vertex) throws NullPointerException {
        addVertex(vertices.size(), vertex);
    }

    /** Insert the {@link Vertex} element at the given {@code position} to the outline loop/strip.
     * @param position of the added Vertex
     * @param vertex Vertex object to be added
     * @throws NullPointerException if the  {@link Vertex} element is null 
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position > getVertexNumber())
     */
    public final void addVertex(int position, Vertex vertex) throws NullPointerException, IndexOutOfBoundsException {
        if (null == vertex) {
            throw new NullPointerException("vertex is null");
        }
        vertices.add(position, vertex);
        if(!dirtyBBox) {
            bbox.resize(vertex.getX(), vertex.getY(), vertex.getZ());
        }
    }

    /** Replaces the {@link Vertex} element at the given {@code position}.
     * <p>Sets the bounding box dirty, hence a next call to {@link #getBounds()} will validate it.</p>
     * 
     * @param position of the replaced Vertex
     * @param vertex replacement Vertex object 
     * @throws NullPointerException if the  {@link Outline} element is null 
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position >= getVertexNumber())
     */
    public final void setVertex(int position, Vertex vertex) throws NullPointerException, IndexOutOfBoundsException {
        if (null == vertex) {
            throw new NullPointerException("vertex is null");
        }
        vertices.set(position, vertex);
        dirtyBBox = true;
    }

    public final Vertex getVertex(int index){
        return vertices.get(index);
    }

    public int getVertexIndex(Vertex vertex){
        return vertices.indexOf(vertex);
    }

    /** Removes the {@link Vertex} element at the given {@code position}.
     * <p>Sets the bounding box dirty, hence a next call to {@link #getBounds()} will validate it.</p>
     * 
     * @param position of the to be removed Vertex
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position >= getVertexNumber())
     */
    public final Vertex removeVertex(int position) throws IndexOutOfBoundsException {
        dirtyBBox = true;        
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
    public final void setVertices(ArrayList<Vertex> vertices) {
        this.vertices = vertices;
        validateBoundingBox();
    }

    public final boolean isClosed() {
        return closed;
    }

    /** define if this outline is closed or not.
     * if set to closed, checks if the last vertex is 
     * equal to the first vertex. If not Equal adds a
     * vertex at the end to the list.
     * @param closed
     */
    public final void setClosed(boolean closed) {
        this.closed = closed;
        if( closed && !isEmpty() ) {
            Vertex first = vertices.get(0);
            Vertex last = getLastVertex();
            if(!VectorUtil.checkEquality(first.getCoord(), last.getCoord())){
                Vertex v = first.clone();
                vertices.add(v);
            }
        }
    }

    /** Compare two outlines with Bounding Box area
     * as criteria. 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public final int compareTo(Outline outline) {
        float size = getBounds().getSize();
        float newSize = outline.getBounds().getSize();
        if(size < newSize){
            return -1;
        }
        else if(size > newSize){
            return 1;
        }
        return 0;
    }

    private final void validateBoundingBox() {
        dirtyBBox = false;
        bbox.reset();
        for (int i=0; i<vertices.size(); i++) {
            bbox.resize(vertices.get(i).getCoord(), 0);
        }
    }

    public final AABBox getBounds() {
        if (dirtyBBox) {
            validateBoundingBox();
        }
        return bbox;
    }    

    /**
     * @param obj the Object to compare this Outline with
     * @return true if {@code obj} is an Outline, not null, equals bounds and equal vertices in the same order 
     */
    public boolean equals(Object obj) {
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

    /**
     * @return deep clone of this Outline
     */
    public Outline clone() {
        Outline o;
        try {
            o = (Outline) super.clone();
        } catch (CloneNotSupportedException e) { throw new InternalError(); }
        o.bbox = bbox.clone();
        o.vertices = new ArrayList<Vertex>(vertices.size());
        for(int i=0; i<vertices.size(); i++) {
            o.vertices.add(vertices.get(i).clone());
        }
        return o;
    }       
}
