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
import java.util.Arrays;
import java.util.Collection;

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
public class Outline
    extends ArrayList<Vertex>
    implements Comparable<Outline>
{
    

    private boolean closed, dirty;

    private AABBox box = new AABBox();
    
    /**Create an outline defined by control vertices.
     * An outline can contain off Curve vertices which define curved
     * regions in the outline.
     */
    public Outline(){
        super();
    }
    
    /** Add a vertex to the outline. The {@link Vertex} is added at the 
     * end of the outline loop/strip.
     * @param vertex Vertex to be added
     * @return Whether this collection has been modified by the add
     */
    @Override
    public final boolean add(Vertex vertex) {

        if (vertex.isOnCurve())
            this.box.resize(vertex.getX(), vertex.getY(), vertex.getZ());

        return super.add(vertex);
    }
    /**
     * Insert
     */
    @Override
    public final void add(int index, Vertex vertex) {

        if (vertex.isOnCurve())
            this.box.resize(vertex.getX(), vertex.getY(), vertex.getZ());

        super.add(index,vertex);
    }
    @Override
    public boolean addAll(Collection<? extends Vertex> c) {
        boolean mod = false;
        for (Vertex v : c){

            mod = (this.add(v) || mod);
        }
        return mod;
    }
    @Override
    public boolean addAll(int index, Collection<? extends Vertex> c) {
        boolean mod = false;
        for (Vertex v : c){

            this.add(index++,v);
            mod = true;
        }
        return mod;
    }
    /**
     * Replace: makes dirty, needs subsequent clean.
     * @see #clean()
     */
    @Override
    public Vertex set(int index, Vertex v){

        this.dirty = true;

        return super.set(index,v);
    }
    /**
     * Remove: makes dirty, needs subsequent clean.
     * @see #clean()
     */
    @Override
    public Vertex remove(int index){

        this.dirty = true;

        return super.remove(index);
    }
    /**
     * Remove: makes dirty, needs subsequent clean.
     * @see #clean()
     */
    @Override
    public boolean remove(Object vertex){

        this.dirty = true;

        return super.remove(vertex);
    }
    /**
     * Clear, clean and open (not closed path)
     */
    @Override
    public void clear(){
        this.dirty = false;
        this.closed = false;
        this.box.reset();
        super.clear();
    }
    /**
     * Is dirty?  Needs clean.
     * @return Recorded need for clean (calls to remove or replace operators)
     */
    public boolean isDirty(){
        return this.dirty;
    }
    public boolean isNotEmpty(){
        return (0 < this.size());
    }
    /**
     * Clean when dirty
     */
    public Outline clean(){
        return this.clean(false);
    }
    /**
     * After remove or replace, reset and resize the bounding box from
     * this collection.
     * 
     * @param override Whether to override the recorded need for clean
     * and do the clean anyway.
     * 
     * @see #isDirty()
     */
    public Outline clean(boolean override){

        if (this.dirty || override){

            this.dirty = false;

            this.box.reset();

            for (Vertex v: this){

                box.resize(v.getX(), v.getY(), v.getZ());
            }
        }
        return this;
    }

    /**  Add a {@link Vertex} by specifying its 2D attributes to the outline. 
     * The {@link Vertex} is added at the 
     * end of the outline loop/strip. 
     * @param factory a {@link Factory} to get the required Vertex impl
     * @param x the x coordinate
     * @param y the y coordinate
     * @param onCurve flag if this vertex is on the final curve or defines a curved region
     * of the shape around this vertex.
     * @return Whether this collection has been modified by the add
     */
    public final boolean add(Vertex.Factory<? extends Vertex> factory, float x, float y, boolean onCurve) {
        return this.add(factory, x, y, 0f, onCurve);
    }
    
    /** Add a {@link Vertex} by specifying its 3D attributes to the outline. 
     * The {@link Vertex} is added at the 
     * end of the outline loop/strip. 
     * @param factory  a {@link Factory} to get the required Vertex impl
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param onCurve flag if this vertex is on the final curve or defines a curved region
     * of the shape around this vertex.
     * @return Whether this collection has been modified by the add
     */
    public final boolean add(Vertex.Factory<? extends Vertex> factory, float x, float y, float z, boolean onCurve) {
        Vertex v = factory.create(x, y, z);
        v.setOnCurve(onCurve);
        return this.add(v);
    }
    
    /** Add a vertex to the outline by passing a float array and specifying the 
     * offset and length in which. The attributes of the vertex are located. 
     * The attributes should be continuous (stride = 0).
     * Attributes which value are not set (when length less than 3) 
     * are set implicitly to zero.
     * @param factory  a {@link Factory} to get the required Vertex impl
     * @param coordsBuffer the coordinate array where the vertex attributes are to be picked from
     * @param offset the offset in the buffer to the x coordinate
     * @param length the number of attributes to pick from the buffer (maximum 3)
     * @param onCurve flag if this vertex is on the final curve or defines a curved region
     * of the shape around this vertex.
     * @return Whether this collection has been modified by the add
     */
    public final boolean add(Vertex.Factory<? extends Vertex> factory, float[] coordsBuffer, int offset, int length, boolean onCurve) {
        Vertex v = factory.create(coordsBuffer, offset, length);
        v.setOnCurve(onCurve);
        return this.add(v);
    }
    
    public Vertex getLastVertex(){
        if (isEmpty())
            return null;
        else
            return this.get(this.size()-1);
    }
    
    public ArrayList<Vertex> getVertices() {
        return this;
    }
    public void setVertices(ArrayList<Vertex> vertices) {
        this.clear();

        this.addAll(vertices);
    }
    public AABBox getBounds() {
        return box;
    }
    public boolean isClosed() {
        return closed;
    }
    
    /** Define outline is closed or not.
     * if set to closed, checks if the last vertex is 
     * equal to the first vertex. If not Equal adds a
     * vertex at the end to the list.
     * @param closed
     */
    public void setClosed(boolean closed) {
        this.closed = closed;
        if (closed){
            Vertex first = this.get(0);
            Vertex last = getLastVertex();
            if(!VectorUtil.checkEquality(first.getCoord(), last.getCoord())){
                Vertex v = first.clone();
                this.add(v);
            }
        }
    }
    public boolean equals(Object that){
        if (that instanceof Outline)
            return this.equals( (Outline)that);
        else
            return false;
    }
    public boolean equals(Outline that){
        if (this == that)
            return true;
        else if (null == that)
            return false;
        else if (this.size() == that.size() && 0 == this.compareTo(that)){

            final int count = this.size();
            if (0 < count){
                Vertex[] thisAry = this.toArray(new Vertex[count]);
                Vertex[] thatAry = that.toArray(new Vertex[count]);
                Arrays.sort(thisAry);
                Arrays.sort(thatAry);
                for (int cc = 0; cc < count; cc++){

                    if (!thisAry[cc].equals(thatAry[cc]))
                        return false;
                }
            }
            return true;
        }
        else
            return false;
    }
    /** Compare two outlines with Bounding Box area
     * as criteria. 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Outline outline) {
        float size = box.getSize();
        float newSize = outline.getBounds().getSize();
        if(size < newSize){
            return -1;
        }
        else if(size > newSize){
            return 1;
        }
        return 0;
    }
    /**
     * @return Shallow clone not vertices
     */
    public Outline clone(){
        Outline clone = (Outline)super.clone();
        clone.box = clone.box.clone();
        return clone;
    }
}
