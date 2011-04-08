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
public class Outline implements Comparable<Outline> {
    
    private ArrayList<Vertex> vertices = new ArrayList<Vertex>(3);
    private boolean closed = false;
    private AABBox box = new AABBox();
    
    /**Create an outline defined by control vertices.
     * An outline can contain off Curve vertices which define curved
     * regions in the outline.
     */
    public Outline(){
        
    }
    
    /** Add a vertex to the outline. The {@link Vertex} is added at the 
     * end of the outline loop/strip.
     * @param vertex Vertex to be added
     */
    public final void addVertex(Vertex vertex) {
        vertices.add(vertex);
        box.resize(vertex.getX(), vertex.getY(), vertex.getZ());
    }
    
    /**  Add a {@link Vertex} by specifying its 2D attributes to the outline. 
     * The {@link Vertex} is added at the 
     * end of the outline loop/strip. 
     * @param factory a {@link Factory} to get the required Vertex impl
     * @param x the x coordinate
     * @param y the y coordinate
     * @param onCurve flag if this vertex is on the final curve or defines a curved region
     * of the shape around this vertex.
     */
    public final void addVertex(Vertex.Factory<? extends Vertex> factory, float x, float y, boolean onCurve) {
        addVertex(factory, x, y, 0f, onCurve);
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
     */
    public final void addVertex(Vertex.Factory<? extends Vertex> factory, float x, float y, float z, boolean onCurve) {
        Vertex v = factory.create(x, y, z);
        v.setOnCurve(onCurve);
        addVertex(v);
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
     */
    public final void addVertex(Vertex.Factory<? extends Vertex> factory, float[] coordsBuffer, int offset, int length, boolean onCurve) {
        Vertex v = factory.create(coordsBuffer, offset, length);
        v.setOnCurve(onCurve);
        addVertex(v);
    }
    
    public Vertex getVertex(int index){
        return vertices.get(index);
    }
    
    public boolean isEmpty(){
        return (vertices.size() == 0);
    }
    public Vertex getLastVertex(){
        if(isEmpty()){
            return null;
        }
        return vertices.get(vertices.size()-1);
    }
    
    public ArrayList<Vertex> getVertices() {
        return vertices;
    }
    public void setVertices(ArrayList<Vertex> vertices) {
        this.vertices = vertices;
    }
    public AABBox getBox() {
        return box;
    }
    public boolean isClosed() {
        return closed;
    }
    
    /** define if this outline is closed or not.
     * if set to closed, checks if the last vertex is 
     * equal to the first vertex. If not Equal adds a
     * vertex at the end to the list.
     * @param closed
     */
    public void setClosed(boolean closed) {
        this.closed = closed;
        if(closed){
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
    public int compareTo(Outline outline) {
        float size = box.getSize();
        float newSize = outline.getBox().getSize();
        if(size < newSize){
            return -1;
        }
        else if(size > newSize){
            return 1;
        }
        return 0;
    }
}
