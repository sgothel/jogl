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

package com.jogamp.graph.curve.tess;

import java.util.ArrayList;

import jogamp.graph.curve.tess.GraphOutline;
import jogamp.graph.curve.tess.GraphVertex;
import jogamp.graph.curve.tess.Loop;
import jogamp.graph.math.VectorFloatUtil;

import com.jogamp.graph.geom.Outline;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;
import jogamp.opengl.Debug;

/** Constrained Delaunay Triangulation 
 * implementation of a list of Outlines that define a set of
 * Closed Regions with optional n holes.
 * 
 */
public class CDTriangulator2D <T extends Vertex> {

	protected static final boolean DEBUG = Debug.debug("Triangulation");
	
	private float sharpness = 0.5f;
	private ArrayList<Loop<T>> loops;
	private ArrayList<T> vertices;
	
	private ArrayList<Triangle<T>> triangles;
	private int maxTriID = 0;

	
	public CDTriangulator2D() {
		this(0.5f);
	}
	
	/** Constructor for a new Delaunay triangulator
	 * @param curveSharpness the curvature around
	 *  the off-curve vertices
	 */
	public CDTriangulator2D(float curveSharpness) {
		this.sharpness = curveSharpness;
		reset();
	}
	
	/** Reset the triangulation to initial state
	 *  Clearing cached data
	 */
	public void reset() {
		maxTriID = 0;
		vertices = new ArrayList<T>();
		triangles = new ArrayList<Triangle<T>>(3);
		loops = new ArrayList<Loop<T>>();
	}
	
	/** Add a curve to the list of profiles provided
	 * @param polyline a bounding Outline
	 */
	public void addCurve(Outline<T> polyline){
		Loop<T> loop = null;
		
		if(!loops.isEmpty()){
			loop = getContainerLoop(polyline);
		}
		
		if(loop == null) {
			GraphOutline<T> outline = new GraphOutline<T>(polyline);
			GraphOutline<T> innerPoly = extractBoundaryTriangles(outline, false);
			vertices.addAll(polyline.getVertices());
			loop = new Loop<T>(innerPoly, VectorFloatUtil.CCW);
			loops.add(loop);
		}
		else {
			GraphOutline<T> outline = new GraphOutline<T>(polyline);
			GraphOutline<T> innerPoly = extractBoundaryTriangles(outline, true);
			vertices.addAll(innerPoly.getPoints());
			loop.addConstraintCurve(innerPoly);
		}
	}
	
	/** Generate the triangulation of the provided 
	 *  List of Outlines
	 */
	public ArrayList<Triangle<T>> generateTriangulation(){	
		for(int i=0;i<loops.size();i++) {
			Loop<T> loop = loops.get(i);
			int numTries = 0;
			int size = loop.computeLoopSize();
			while(!loop.isSimplex()){
				Triangle<T> tri = null;
				if(numTries > size){
					tri = loop.cut(false);
				}
				else{
					tri = loop.cut(true);
				}
				numTries++;

				if(tri != null) {
					numTries = 0;
					size--;
					tri.setId(maxTriID++);
					triangles.add(tri);
					if(DEBUG){
						System.err.println(tri);
					}
				}
				if(numTries > size*2){
					if(DEBUG){
						System.err.println("Triangulation not complete!");
					}
					break;
				}
			}
			Triangle<T> tri = loop.cut(true);
			if(tri != null)
				triangles.add(tri);
		}
		return triangles;
	}

	@SuppressWarnings("unchecked")	
	private GraphOutline<T> extractBoundaryTriangles(GraphOutline<T> outline, boolean hole){
		GraphOutline<T> innerOutline = new GraphOutline<T>();
		ArrayList<GraphVertex<T>> outVertices = outline.getGraphPoint();
		int size = outVertices.size();
		for(int i=0; i < size; i++) {
			GraphVertex<T> currentVertex = outVertices.get(i);
			GraphVertex<T> gv0 = outVertices.get((i+size-1)%size);
			GraphVertex<T> gv2 = outVertices.get((i+1)%size);
			GraphVertex<T> gv1 = currentVertex;
			
			if(!currentVertex.getPoint().isOnCurve()) {
				T v0 = (T) gv0.getPoint().clone();
				T v2 = (T) gv2.getPoint().clone();
				T v1 = (T) gv1.getPoint().clone();
				
				gv0.setBoundaryContained(true);
				gv1.setBoundaryContained(true);
				gv2.setBoundaryContained(true);
				
				Triangle<T> t= null;
				boolean holeLike = false;
				if(VectorFloatUtil.ccw(v0,v1,v2)){
					t = new Triangle<T>(v0, v1, v2);
				}
				else {
					holeLike = true;
					t = new Triangle<T>(v2, v1, v0);
				}
				t.setId(maxTriID++);
				triangles.add(t);
				
				if(hole || holeLike) {
					v0.setTexCoord(0, -0.1f);
					v2.setTexCoord(1, -0.1f);
					v1.setTexCoord(0.5f, -1*sharpness -0.1f);
					innerOutline.addVertex(currentVertex);
				}
				else {
					v0.setTexCoord(0, 0.1f);
					v2.setTexCoord(1, 0.1f);
					v1.setTexCoord(0.5f, sharpness+0.1f);
				}
			}
			else {
				if(!gv2.getPoint().isOnCurve() || !gv0.getPoint().isOnCurve()){
					currentVertex.setBoundaryContained(true);
				}
				innerOutline.addVertex(currentVertex);
			}
		}
		return innerOutline;
	}
	
	private Loop<T> getContainerLoop(Outline<T> polyline){
		T v = polyline.getVertex(0);
		
		for (Loop<T> loop:loops){
			if(loop.checkInside(v)){
				return loop;
			}
		}
		return null;
	}
}
