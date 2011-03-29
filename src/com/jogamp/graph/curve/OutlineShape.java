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
package com.jogamp.graph.curve;

import java.util.ArrayList;
import java.util.Collections;

import com.jogamp.graph.geom.Outline;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.math.VectorUtil;

import com.jogamp.graph.curve.tess.CDTriangulator2D;

/** A Generic shape objects which is defined by a list of Outlines.
 * This Shape can be transformed to Triangulations.
 * The list of triangles generated are render-able by a Region object.
 * The triangulation produced by this Shape will define the 
 * closed region defined by the outlines.
 * 
 * One or more OutlineShape Object can be associated to a region
 * this is left high-level representation of the Objects. For
 * possible Optimizations.
 * 
 * @see Region
 */
public class OutlineShape {
	public static final int QUADRATIC_NURBS = 10;
	private final Vertex.Factory<? extends Vertex> pointFactory;
	private ArrayList<Outline<Vertex>> outlines = new ArrayList<Outline<Vertex>>(3);
	
	/** Create a new Outline based Shape
	 */
	public OutlineShape(Vertex.Factory<? extends Vertex> factory) {
		pointFactory = factory;
		outlines.add(new Outline<Vertex>());
	}
	
	public final Vertex.Factory<? extends Vertex> pointFactory() { return pointFactory; }
	
	/** Add a new empty outline 
	 * to the shape, this new outline will
	 * be placed at the end of the outline list.
	 */
	public void addEmptyOutline(){
		outlines.add(new Outline<Vertex>());
	}
	
	/** Adds an outline to the OutlineShape object
	 * if last outline of the shape is empty, it will replace
	 * that last Outline with the new one. If outline is empty,
	 * it will do nothing.
	 * @param outline an Outline object
	 */
	public void addOutline(Outline<Vertex> outline){
		if(outline.isEmpty()){
			return;
		}
		if(getLastOutline().isEmpty()){
			outlines.remove(getLastOutline());
		}
		outlines.add(outline);
	}
	
	/** Adds a vertex to the last open outline in the
	 *  shape
	 * @param point 
	 */
	public final void addVertex(Vertex point){
		getLastOutline().addVertex(point);
	}
	
	public final void addVertex(float x, float y, boolean onCurve) {
		getLastOutline().addVertex(pointFactory, x, y, onCurve);
	}
	
	public final void addVertex(float x, float y, float z, boolean onCurve) {
		getLastOutline().addVertex(pointFactory, x, y, z, onCurve);
	}
	
	public final void addVertex(float[] coordsBuffer, int offset, int length, boolean onCurve) {
		getLastOutline().addVertex(pointFactory, coordsBuffer, offset, length, onCurve);
	}	
	
	/** Closes the last outline in the shape
	 * if last vertex is not equal to first vertex.
	 * A new temp vertex is added at the end which 
	 * is equal to the first.
	 */
	public void closeLastOutline(){
		getLastOutline().setClosed(true);
	}
	
	/** Get the last added outline to the list
	 * of outlines that define the shape
	 * @return the last outline
	 */
	public final Outline<Vertex> getLastOutline(){
		return outlines.get(outlines.size()-1);
	}
	/** Make sure that the outlines represent
	 * the specified destinationType, if not
	 * transform outlines to destinationType.
	 * @param destinationType The curve type needed
	 */
	public void transformOutlines(int destinationType){
		if(destinationType == QUADRATIC_NURBS){
			transformOutlinesQuadratic();
		}
	}
	
	private void transformOutlinesQuadratic(){
		ArrayList<Outline<Vertex>> newOutlines = new ArrayList<Outline<Vertex>>(3);

		/**loop over the outlines and make sure no
		 * adj off-curve vertices
		 */
		for(Outline<Vertex> outline:outlines){
			Outline<Vertex> newOutline = new Outline<Vertex>();

			ArrayList<Vertex> vertices = outline.getVertices();
			int size =vertices.size()-1;
			for(int i=0;i<size;i++){
				Vertex currentVertex = vertices.get(i);
				Vertex nextVertex = vertices.get((i+1)%size);
				if(!(currentVertex.isOnCurve()) && !(nextVertex.isOnCurve())) {
					newOutline.addVertex(currentVertex);
					
					float[] newCoords = VectorUtil.mid(currentVertex.getCoord(), nextVertex.getCoord());
					newOutline.addVertex(pointFactory, newCoords, 0, 3, true);
				}
				else {
					newOutline.addVertex(currentVertex);
				}
			}
			newOutlines.add(newOutline);
		}
		outlines = newOutlines;
	}
	
	private void generateVertexIds(){
		int maxVertexId = 0;
		for(Outline<Vertex> outline:outlines){
			ArrayList<Vertex> vertices = outline.getVertices();
			for(Vertex vert:vertices){
				vert.setId(maxVertexId);
				maxVertexId++;
			}
		}
	}
	
	/** @return the list of vertices associated with the 
	 * {@code Outline} list of this object
	 */
	public ArrayList<Vertex> getVertices(){
		ArrayList<Vertex> vertices = new ArrayList<Vertex>();
		for(Outline<Vertex> polyline:outlines){
			vertices.addAll(polyline.getVertices());
		}
		return vertices;
	}
	

	/** Triangluate the graph object
	 * @param sharpness sharpness of the curved regions default = 0.5
	 */
	public ArrayList<Triangle<Vertex>> triangulate(float sharpness){
		if(outlines.size() == 0){
			return null;
		}
		sortOutlines();
		generateVertexIds();
		
		CDTriangulator2D<Vertex> triangulator2d = new CDTriangulator2D<Vertex>(sharpness);
		
		for(int index = 0; index< outlines.size();index++){
			Outline<Vertex> outline = outlines.get(index);
			triangulator2d.addCurve(outline);
		}
		
		ArrayList<Triangle<Vertex>> triangles = triangulator2d.generateTriangulation();
		triangulator2d.reset();
		
		return triangles;
	}
	
	/** Sort the outlines from large
	 *  to small depending on the AABox
	 */
	private void sortOutlines() {
		Collections.sort(outlines);
		Collections.reverse(outlines);
	}
}
