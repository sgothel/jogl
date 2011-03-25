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

import jogamp.graph.math.VectorFloatUtil;

import com.jogamp.graph.geom.Outline;
import com.jogamp.graph.geom.Line;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Point;
import com.jogamp.graph.geom.PointTex;

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
	private final Point.Factory<? extends PointTex> pointFactory;
	private ArrayList<Outline<PointTex>> outlines = new ArrayList<Outline<PointTex>>(3);
	
	/** Create a new Outline based Shape
	 */
	public OutlineShape(Point.Factory<? extends PointTex> factory) {
		pointFactory = factory;
		outlines.add(new Outline<PointTex>());
	}
	
	public final Point.Factory<? extends PointTex> pointFactory() { return pointFactory; }
	
	/** Add a new empty outline 
	 * to the shape, this new outline will
	 * be placed at the end of the outline list.
	 */
	public void addEmptyOutline(){
		outlines.add(new Outline<PointTex>());
	}
	
	/** Adds an outline to the OutlineShape object
	 * if last outline of the shape is empty, it will replace
	 * that last Outline with the new one. If outline is empty,
	 * it will do nothing.
	 * @param outline an Outline object
	 */
	public void addOutline(Outline<PointTex> outline){
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
	public final void addVertex(PointTex point){
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
	public final Outline<PointTex> getLastOutline(){
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
		ArrayList<Outline<PointTex>> newOutlines = new ArrayList<Outline<PointTex>>(3);

		/**loop over the outlines and make sure no
		 * adj off-curve vertices
		 */
		for(Outline<PointTex> outline:outlines){
			Outline<PointTex> newOutline = new Outline<PointTex>();

			ArrayList<PointTex> vertices = outline.getVertices();
			int size =vertices.size()-1;
			for(int i=0;i<size;i++){
				PointTex currentVertex = vertices.get(i);
				PointTex nextVertex = vertices.get((i+1)%size);
				if(!(currentVertex.isOnCurve()) && !(nextVertex.isOnCurve())) {
					newOutline.addVertex(currentVertex);
					
					float[] newCoords = VectorFloatUtil.mid(currentVertex.getCoord(), nextVertex.getCoord());
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
		for(Outline<PointTex> outline:outlines){
			ArrayList<PointTex> vertices = outline.getVertices();
			for(PointTex vert:vertices){
				vert.setId(maxVertexId);
				maxVertexId++;
			}
		}
	}
	
	/** @return the list of vertices associated with the 
	 * {@code Outline} list of this object
	 */
	public ArrayList<PointTex> getVertices(){
		ArrayList<PointTex> vertices = new ArrayList<PointTex>();
		for(Outline<PointTex> polyline:outlines){
			vertices.addAll(polyline.getVertices());
		}
		return vertices;
	}
	

	/** Generates the lines the define the noncurved
	 * parts of this graph
	 * @return arraylist of lines
	 */
	public ArrayList<Line<PointTex>> getLines(){
		ArrayList<Line<PointTex>> lines = new ArrayList<Line<PointTex>>();
		for(Outline<PointTex> outline:outlines){
			ArrayList<PointTex> outVertices = outline.getVertices();
			int size = outVertices.size();
			for(int i=0; i < size; i++) {
				PointTex currentVertex = outVertices.get(i);
				if(currentVertex.isOnCurve()) {
					PointTex v2 = outVertices.get((i+1)%size);
					if(v2.isOnCurve()){
						lines.add(new Line<PointTex>(currentVertex, v2));
					}
				}
			}
		}
		return lines;
	}
	
	/** Triangluate the graph object
	 * @param sharpness sharpness of the curved regions default = 0.5
	 */
	public ArrayList<Triangle<PointTex>> triangulate(float sharpness){
		if(outlines.size() == 0){
			return null;
		}
		sortOutlines();
		generateVertexIds();
		
		CDTriangulator2D<PointTex> triangulator2d = new CDTriangulator2D<PointTex>(sharpness);
		
		for(int index = 0; index< outlines.size();index++){
			Outline<PointTex> outline = outlines.get(index);
			triangulator2d.addCurve(outline);
		}
		
		ArrayList<Triangle<PointTex>> triangles = triangulator2d.generateTriangulation();
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
