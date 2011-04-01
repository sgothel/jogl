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
 * this is left as a high-level representation of the Objects. For
 * optimizations, flexibility requirements for future features.
 * 
 * <br><br>
 * Example to creating an Outline Shape:
 * <pre>
  	addVertex(...)
  	addVertex(...)
  	addVertex(...)
  	addEnptyOutline()
  	addVertex(...)
  	addVertex(...)
  	addVertex(...)
 * </pre>
 * 
 * The above will create two outlines each with three vertices. By adding these two outlines to 
 * the OutlineShape, we are stating that the combination of the two outlines represent the shape.
 * <br>
 * 
 * To specify that the shape is curved at a region, the on-curve flag should be set to false 
 * for the vertex that is in the middle of the curved region (if the curved region is defined by 3
 * vertices (quadratic curve).
 * <br>
 * In case the curved region is defined by 4 or more vertices the middle vertices should both have 
 * the on-curve flag set to false.
 * 
 * <br>Example: <br>
 * <pre>
  	addVertex(0,0, true);
  	addVertex(0,1, false);
  	addVertex(1,1, false);
  	addVertex(1,0, true);
 * </pre>
 * 
 * The above snippet defines a cubic nurbs curve where (0,1 and 1,1) 
 * do not belong to the final rendered shape.
 *  
 * <i>Implementation Notes:</i><br>
 * <ul>
 *		<li> The first vertex of any outline belonging to the shape should be on-curve</li>
 *		<li> Intersections between off-curved parts of the outline is not handled</li>
 * </ul>
 * 
 * @see Outline
 * @see Region
 */
public class OutlineShape {

	public static final int QUADRATIC_NURBS = 10;
	private final Vertex.Factory<? extends Vertex> vertexFactory;

	/** The list of {@link Outline}s that are part of this 
	 *  outline shape.
	 */
	private ArrayList<Outline> outlines = new ArrayList<Outline>(3);

	/** Create a new Outline based Shape
	 */
	public OutlineShape(Vertex.Factory<? extends Vertex> factory) {
		vertexFactory = factory;
		outlines.add(new Outline());
	}

	/** Returns the associated vertex factory of this outline shape
	 * @return Vertex.Factory object
	 */
	public final Vertex.Factory<? extends Vertex> vertexFactory() { return vertexFactory; }

	/** Add a new empty {@link Outline} 
	 * to the shape, this new outline will
	 * be placed at the end of the outline list.
	 * 
	 * After a call to this function all new vertices added
	 * will belong to the new outline
	 */
	public void addEmptyOutline(){
		outlines.add(new Outline());
	}

	/** Adds an {@link Outline} to the OutlineShape object
	 * if last outline of the shape is empty, it will replace
	 * that last Outline with the new one. If outline is empty,
	 * it will do nothing.
	 * @param outline an Outline object
	 */
	public void addOutline(Outline outline){
		if(outline.isEmpty()){
			return;
		}
		if(getLastOutline().isEmpty()){
			outlines.remove(getLastOutline());
		}
		outlines.add(outline);
	}

	/** Adds a vertex to the last open outline in the
	 *  shape. 
	 * @param v the vertex to be added to the OutlineShape
	 */
	public final void addVertex(Vertex v){
		getLastOutline().addVertex(v);
	}

	/** Add a 2D {@link Vertex} to the last outline by defining the coordniate attribute
	 * of the vertex. The 2D vertex will be represented as Z=0.
	 * 
	 * @param x the x coordinate
	 * @param y the y coordniate
	 * @param onCurve flag if this vertex is on the final curve or defines a curved region
	 * of the shape around this vertex.
	 */
	public final void addVertex(float x, float y, boolean onCurve) {
		getLastOutline().addVertex(vertexFactory, x, y, onCurve);
	}

	/** Add a 3D {@link Vertex} to the last outline by defining the coordniate attribute
	 * of the vertex.
	 * @param x the x coordinate
	 * @param y the y coordniate
	 * @param z the z coordniate
	 * @param onCurve flag if this vertex is on the final curve or defines a curved region
	 * of the shape around this vertex.
	 */
	public final void addVertex(float x, float y, float z, boolean onCurve) {
		getLastOutline().addVertex(vertexFactory, x, y, z, onCurve);
	}

	/** Add a vertex to the last outline by passing a float array and specifying the 
	 * offset and length in which. The attributes of the vertex are located. 
	 * The attributes should be continuous (stride = 0).
	 * Attributes which value are not set (when length less than 3) 
	 * are set implicitly to zero.
	 * @param coordsBuffer the coordinate array where the vertex attributes are to be picked from
	 * @param offset the offset in the buffer to the x coordinate
	 * @param length the number of attributes to pick from the buffer (maximum 3)
	 * @param onCurve flag if this vertex is on the final curve or defines a curved region
	 * of the shape around this vertex.
	 */
	public final void addVertex(float[] coordsBuffer, int offset, int length, boolean onCurve) {
		getLastOutline().addVertex(vertexFactory, coordsBuffer, offset, length, onCurve);
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
	public final Outline getLastOutline(){
		return outlines.get(outlines.size()-1);
	}
	/** Make sure that the outlines represent
	 * the specified destinationType, if not
	 * transform outlines to destination type.
	 * @param destinationType The curve type needed
	 */
	public void transformOutlines(int destinationType){
		if(destinationType == QUADRATIC_NURBS){
			transformOutlinesQuadratic();
		}
	}

	private void transformOutlinesQuadratic(){
		ArrayList<Outline> newOutlines = new ArrayList<Outline>(3);

		/**loop over the outlines and make sure no
		 * adj off-curve vertices
		 */
		for(Outline outline:outlines){
			Outline newOutline = new Outline();

			ArrayList<Vertex> vertices = outline.getVertices();
			int size =vertices.size()-1;
			for(int i=0;i<size;i++){
				Vertex currentVertex = vertices.get(i);
				Vertex nextVertex = vertices.get((i+1)%size);
				if(!(currentVertex.isOnCurve()) && !(nextVertex.isOnCurve())) {
					newOutline.addVertex(currentVertex);

					float[] newCoords = VectorUtil.mid(currentVertex.getCoord(), nextVertex.getCoord());
					newOutline.addVertex(vertexFactory, newCoords, 0, 3, true);
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
		for(Outline outline:outlines){
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
		for(Outline polyline:outlines){
			vertices.addAll(polyline.getVertices());
		}
		return vertices;
	}

	/** Triangulate the outline shape generating a list of triangles
	 * @return an arraylist of triangles representing the filled region
	 * which is produced by the combination of the outlines 
	 */
	public ArrayList<Triangle> triangulate(){
		return triangulate(0.5f);
	}

	/**Triangulate the {@link OutlineShape} generating a list of triangles
	 * @param sharpness defines the curvature strength around the off-curve vertices.
	 * defaults to 0.5f
	 * @return an arraylist of triangles representing the filled region
	 * which is produced by the combination of the outlines
	 */
	public ArrayList<Triangle> triangulate(float sharpness){
		if(outlines.size() == 0){
			return null;
		}
		sortOutlines();
		generateVertexIds();
		
		CDTriangulator2D triangulator2d = new CDTriangulator2D(sharpness);
		for(int index = 0; index< outlines.size();index++){
			Outline outline = outlines.get(index);
			triangulator2d.addCurve(outline);
		}
		
		ArrayList<Triangle> triangles = triangulator2d.generateTriangulation();
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
