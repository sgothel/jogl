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
public class Outline<T extends Vertex> implements Comparable<Outline<T>>{
	
	private ArrayList<T> vertices = new ArrayList<T>(3);
	private boolean closed = false;
	private AABBox box = new AABBox();
	
	/**Create an outline defined by control vertices.
	 * An outline can contain off Curve vertices which define curved
	 * regions in the outline.
	 */
	public Outline(){
		
	}
	
	/** Add a vertex to the outline. The vertex is added at the 
	 * end of the outline loop/strip.
	 * @param vertex Vertex to be added
	 */
	public final void addVertex(T vertex) {
		vertices.add(vertex);
		box.resize(vertex.getX(), vertex.getY(), vertex.getZ());
	}
	
	public final void addVertex(Vertex.Factory<? extends Vertex> factory, float x, float y, boolean onCurve) {
		addVertex(factory, x, y, 0f, onCurve);
	}
	
	@SuppressWarnings("unchecked")
	public final void addVertex(Vertex.Factory<? extends Vertex> factory, float x, float y, float z, boolean onCurve) {
		Vertex v = factory.create(x, y, z);
		v.setOnCurve(onCurve);
		addVertex((T)v);
	}
	
	@SuppressWarnings("unchecked")
	public final void addVertex(Vertex.Factory<? extends Vertex> factory, float[] coordsBuffer, int offset, int length, boolean onCurve) {
		Vertex v = factory.create(coordsBuffer, offset, length);
		v.setOnCurve(onCurve);
		addVertex((T)v);
	}
	
	public T getVertex(int index){
		return vertices.get(index);
	}
	
	public boolean isEmpty(){
		return (vertices.size() == 0);
	}
	public T getLastVertex(){
		if(isEmpty()){
			return null;
		}
		return vertices.get(vertices.size()-1);
	}
	
	public ArrayList<T> getVertices() {
		return vertices;
	}
	public void setVertices(ArrayList<T> vertices) {
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
			T first = vertices.get(0);
			T last = getLastVertex();
			if(!VectorUtil.checkEquality(first.getCoord(), last.getCoord())){
				@SuppressWarnings("unchecked")
				T v = (T) first.clone();
				vertices.add(v);
			}
		}
	}
	
	/** Compare two outlines with Bounding Box area
	 * as criteria. 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Outline<T> outline) {
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
