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
package jogamp.graph.curve.tess;

import java.util.ArrayList;


import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.math.VectorUtil;

public class Loop <T extends Vertex> {
	private HEdge<T> root = null;
	private AABBox box = new AABBox();
	private GraphOutline<T> initialOutline = null;

	public Loop(GraphOutline<T> polyline, int direction){
		initialOutline = polyline;
		this.root = initFromPolyline(initialOutline, direction);
	}

	public HEdge<T> getHEdge(){
		return root;
	}

	public Triangle<T> cut(boolean delaunay){
		if(isSimplex()){
			@SuppressWarnings("unchecked")
			Triangle<T> t = new Triangle<T>(root.getGraphPoint().getPoint(), root.getNext().getGraphPoint().getPoint(), 
					root.getNext().getNext().getGraphPoint().getPoint());
			t.setVerticesBoundary(checkVerticesBoundary(root));
			return t;
		}
		HEdge<T> prev = root.getPrev();
		HEdge<T> next1 = root.getNext();

		HEdge<T> next2 =findClosestValidNeighbor(next1.getNext(), delaunay);
		if(next2 == null){
			root = root.getNext();
			return null;
		}

		GraphVertex<T> v1 = root.getGraphPoint();
		GraphVertex<T> v2 = next1.getGraphPoint();
		GraphVertex<T> v3 = next2.getGraphPoint();

		HEdge<T> v3Edge = new HEdge<T>(v3, HEdge.INNER);

		HEdge.connect(v3Edge, root);
		HEdge.connect(next1, v3Edge);

		HEdge<T> v3EdgeSib = v3Edge.getSibling();
		if(v3EdgeSib == null){
			v3EdgeSib = new HEdge<T>(v3Edge.getNext().getGraphPoint(), HEdge.INNER);
			HEdge.makeSiblings(v3Edge, v3EdgeSib);
		}

		HEdge.connect(prev, v3EdgeSib);
		HEdge.connect(v3EdgeSib, next2);

		Triangle<T> t = createTriangle(v1.getPoint(), v2.getPoint(), v3.getPoint(), root);
		this.root = next2;
		return t;
	}

	public boolean isSimplex(){
		return (root.getNext().getNext().getNext() == root);
	}

	/**Create a connected list of half edges (loop)
	 * from the boundary profile
	 * @param direction requested winding of edges (CCW or CW)
	 */
	private HEdge<T> initFromPolyline(GraphOutline<T> outline, int direction){
		ArrayList<GraphVertex<T>> vertices = outline.getGraphPoint();

		if(vertices.size()<3) {
			throw new IllegalArgumentException("outline's vertices < 3: " + vertices.size());
		}
		boolean isCCW = VectorUtil.ccw(vertices.get(0).getPoint(), vertices.get(1).getPoint(),
				vertices.get(2).getPoint());
		boolean invert = isCCW && (direction == VectorUtil.CW);

		HEdge<T> firstEdge = null;
		HEdge<T> lastEdge = null;
		int index =0;
		int max = vertices.size();

		int edgeType =  HEdge.BOUNDARY;
		if(invert){
			index = vertices.size() -1;
			max = -1;
			edgeType = HEdge.HOLE;
		}

		while(index != max){
			GraphVertex<T> v1 = vertices.get(index);
			box.resize(v1.getX(), v1.getY(), v1.getZ());

			HEdge<T> edge = new HEdge<T>(v1, edgeType);

			v1.addEdge(edge);
			if(lastEdge != null){
				lastEdge.setNext(edge);
				edge.setPrev(lastEdge);
			}
			else{
				firstEdge = edge;
			}

			if(!invert){
				if(index == vertices.size()-1){
					edge.setNext(firstEdge);
					firstEdge.setPrev(edge);
				}
			}
			else if (index == 0){
				edge.setNext(firstEdge);
				firstEdge.setPrev(edge);
			}

			lastEdge = edge;

			if(!invert){
				index++;
			}
			else{
				index--;
			}
		}
		return firstEdge;
	}

	public void addConstraintCurve(GraphOutline<T> polyline) {
		//		GraphOutline outline = new GraphOutline(polyline);
		/**needed to generate vertex references.*/
		initFromPolyline(polyline, VectorUtil.CW); 

		GraphVertex<T> v3 = locateClosestVertex(polyline);
		HEdge<T> v3Edge = v3.findBoundEdge();
		HEdge<T> v3EdgeP = v3Edge.getPrev();
		HEdge<T> crossEdge = new HEdge<T>(root.getGraphPoint(), HEdge.INNER);

		HEdge.connect(root.getPrev(), crossEdge);
		HEdge.connect(crossEdge, v3Edge);

		HEdge<T> crossEdgeSib = crossEdge.getSibling();
		if(crossEdgeSib == null) {
			crossEdgeSib = new HEdge<T>(crossEdge.getNext().getGraphPoint(), HEdge.INNER);
			HEdge.makeSiblings(crossEdge, crossEdgeSib);
		}

		HEdge.connect(v3EdgeP, crossEdgeSib);
		HEdge.connect(crossEdgeSib, root);
	}

	/** Locates the vertex and update the loops root 
	 * to have (root + vertex) as closest pair 
	 * @param polyline the control polyline 
	 * to search for closestvertices
	 * @return the vertex that is closest to the newly set root Hedge.
	 */
	private GraphVertex<T> locateClosestVertex(GraphOutline<T> polyline) {
		HEdge<T> closestE = null;
		GraphVertex<T> closestV = null;

		float minDistance = Float.MAX_VALUE;
		boolean inValid = false;
		ArrayList<GraphVertex<T>> initVertices = initialOutline.getGraphPoint();
		ArrayList<GraphVertex<T>> vertices = polyline.getGraphPoint();

		for(int i=0; i< initVertices.size()-1; i++){
			GraphVertex<T> v = initVertices.get(i);
			GraphVertex<T> nextV = initVertices.get(i+1);
			for(GraphVertex<T> cand:vertices){
				float distance = VectorUtil.computeLength(v.getCoord(), cand.getCoord());
				if(distance < minDistance){
					for (GraphVertex<T> vert:vertices){
						if(vert == v || vert == nextV || vert == cand)
							continue;
						inValid = VectorUtil.inCircle(v.getPoint(), nextV.getPoint(), 
								cand.getPoint(), vert.getPoint());
						if(inValid){
							break;
						}
					}
					if(!inValid){
						closestV = cand;
						minDistance = distance;
						closestE = v.findBoundEdge();
					}
				}

			}
		}

		if(closestE != null){
			root = closestE;
		}

		return closestV;
	}

	private HEdge<T> findClosestValidNeighbor(HEdge<T> edge, boolean delaunay) {
		HEdge<T> next = root.getNext();

		if(!VectorUtil.ccw(root.getGraphPoint().getPoint(), next.getGraphPoint().getPoint(),
				edge.getGraphPoint().getPoint())){
			return null;
		}

		HEdge<T> candEdge = edge;
		boolean inValid = false;

		if(delaunay){
			T cand = candEdge.getGraphPoint().getPoint();
			HEdge<T> e = candEdge.getNext();
			while (e != candEdge){
				if(e.getGraphPoint() == root.getGraphPoint() 
						|| e.getGraphPoint() == next.getGraphPoint() 
						|| e.getGraphPoint().getPoint() == cand){
					e = e.getNext();
					continue;
				}
				inValid = VectorUtil.inCircle(root.getGraphPoint().getPoint(), next.getGraphPoint().getPoint(),
						cand, e.getGraphPoint().getPoint());
				if(inValid){
					break;
				}
				e = e.getNext();
			}
		}
		if(!inValid){
			return candEdge;
		}
		return null;
	}

	/** Create a triangle from the param vertices only if
	 * the triangle is valid. IE not outside region.
	 * @param v1 vertex 1
	 * @param v2 vertex 2
	 * @param v3 vertex 3
	 * @param root and edge of this triangle
	 * @return the triangle iff it satisfies, null otherwise
	 */
	private Triangle<T> createTriangle(T v1, T v2, T v3, HEdge<T> rootT){
		@SuppressWarnings("unchecked")
		Triangle<T> t = new Triangle<T>(v1, v2, v3);
		t.setVerticesBoundary(checkVerticesBoundary(rootT));
		return t;
	}

	private boolean[] checkVerticesBoundary(HEdge<T> rootT) {
		boolean[] boundary = new boolean[3];
		HEdge<T> e1 = rootT;
		HEdge<T> e2 = rootT.getNext();
		HEdge<T> e3 = rootT.getNext().getNext();

		if(e1.getGraphPoint().isBoundaryContained()){
				boundary[0] = true;
		}
		if(e2.getGraphPoint().isBoundaryContained()){
				boundary[1] = true;
		}
		if(e3.getGraphPoint().isBoundaryContained()){
				boundary[2] = true;
		}
		return boundary;
	}


	/** Check if vertex inside the Loop
	 * @param vertex the Vertex
	 * @return true if the vertex is inside, false otherwise
	 */
	public boolean checkInside(T vertex) {
		if(!box.contains(vertex.getX(), vertex.getY(), vertex.getZ())){
			return false;
		}

		float[] center = box.getCenter();

		int hits = 0;
		HEdge<T> current = root;
		HEdge<T> next = root.getNext();
		while(next!= root){
			if(current.getType() == HEdge.INNER || next.getType() == HEdge.INNER){
				current = next;
				next = current.getNext();
				continue;
			}

			T vert1 = current.getGraphPoint().getPoint();
			T vert2 = next.getGraphPoint().getPoint();

			/** The ray is P0+s*D0, where P0 is the ray origin, D0 is a direction vector and s >= 0. 
			 * The segment is P1+t*D1, where P1 and P1+D1 are the endpoints, and 0 <= t <= 1. 
			 * perp(x,y) = (y,-x).
			 * if Dot(perp(D1),D0) is not zero,
			 * s = Dot(perp(D1),P1-P0)/Dot(perp(D1),D0)
			 * t = Dot(perp(D0),P1-P0)/Dot(perp(D1),D0)
			 */

			float[] d0 = new float[]{center[0] - vertex.getX(), center[1]-vertex.getY(),
					center[2]-vertex.getZ()};
			float[] d1 = {vert2.getX() - vert1.getX(), vert2.getY() - vert1.getY(),
					vert2.getZ() - vert1.getZ()};

			float[] prepD1 = {d1[1],-1*d1[0], d1[2]};
			float[] prepD0 = {d0[1],-1*d0[0], d0[2]};

			float[] p0p1 = new float[]{vert1.getX() - vertex.getX(), vert1.getY() - vertex.getY(),
					vert1.getZ() - vertex.getZ()};

			float dotD1D0 = VectorUtil.dot(prepD1, d0);
			if(dotD1D0 == 0){ 
				/** ray parallel to segment */
				current = next;
				next = current.getNext();
				continue;
			}

			float s = VectorUtil.dot(prepD1,p0p1)/dotD1D0;
			float t = VectorUtil.dot(prepD0,p0p1)/dotD1D0;

			if(s >= 0 && t >= 0 && t<= 1){
				hits++;
			}
			current = next;
			next = current.getNext();
		}

		if(hits % 2 != 0){ 
			/** check if hit count is even */
			return true;
		}
		return false;
	}

	public int computeLoopSize(){
		int size = 0;
		HEdge<T> e = root;
		do{
			size++;
			e = e.getNext();
		}while(e != root);
		return size;
	}
}
