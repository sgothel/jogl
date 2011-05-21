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

public class Loop {
    private HEdge root = null;
    private AABBox box = new AABBox();
    private GraphOutline initialOutline = null;

    public Loop(GraphOutline polyline, VectorUtil.Winding winding){
        initialOutline = polyline;
        this.root = initFromPolyline(initialOutline, winding);
    }

    public HEdge getHEdge(){
        return root;
    }

    public Triangle cut(boolean delaunay){
        if(isSimplex()){
            Triangle t = new Triangle(root.getGraphPoint().getPoint(), root.getNext().getGraphPoint().getPoint(), 
                    root.getNext().getNext().getGraphPoint().getPoint());
            t.setVerticesBoundary(checkVerticesBoundary(root));
            return t;
        }
        HEdge prev = root.getPrev();
        HEdge next1 = root.getNext();

        HEdge next2 = findClosestValidNeighbor(next1.getNext(), delaunay);
        if(next2 == null){
            root = root.getNext();
            return null;
        }

        GraphVertex v1 = root.getGraphPoint();
        GraphVertex v2 = next1.getGraphPoint();
        GraphVertex v3 = next2.getGraphPoint();

        HEdge v3Edge = new HEdge(v3, HEdge.INNER);

        HEdge.connect(v3Edge, root);
        HEdge.connect(next1, v3Edge);

        HEdge v3EdgeSib = v3Edge.getSibling();
        if(v3EdgeSib == null){
            v3EdgeSib = new HEdge(v3Edge.getNext().getGraphPoint(), HEdge.INNER);
            HEdge.makeSiblings(v3Edge, v3EdgeSib);
        }

        HEdge.connect(prev, v3EdgeSib);
        HEdge.connect(v3EdgeSib, next2);

        Triangle t = createTriangle(v1.getPoint(), v2.getPoint(), v3.getPoint(), root);
        this.root = next2;
        return t;
    }

    public boolean isSimplex(){
        return (root.getNext().getNext().getNext() == root);
    }

    /**Create a connected list of half edges (loop)
     * from the boundary profile
     * @param reqWinding requested winding of edges (CCW or CW)
     */
    private HEdge initFromPolyline(GraphOutline outline, VectorUtil.Winding reqWinding){
        ArrayList<GraphVertex> vertices = outline.getGraphPoint();

        if(vertices.size()<3) {
            throw new IllegalArgumentException("outline's vertices < 3: " + vertices.size());
        }
        final VectorUtil.Winding hasWinding = VectorUtil.getWinding(
                                 vertices.get(0).getPoint(), 
                                 vertices.get(1).getPoint(),
                                 vertices.get(2).getPoint());
        //FIXME: handle case when vertices come inverted - Rami
        // skips inversion CW -> CCW
        final boolean invert =  hasWinding != reqWinding &&
                                reqWinding == VectorUtil.Winding.CW;
       
        final int max;
        final int edgeType = reqWinding == VectorUtil.Winding.CCW ? HEdge.BOUNDARY : HEdge.HOLE ;
        int index;
        HEdge firstEdge = null;
        HEdge lastEdge = null;
        
        if(!invert) {
            max = vertices.size();
            index = 0;
        } else {
            max = -1;
            index = vertices.size() -1;
        }

        while(index != max){
            GraphVertex v1 = vertices.get(index);
            box.resize(v1.getX(), v1.getY(), v1.getZ());

            HEdge edge = new HEdge(v1, edgeType);

            v1.addEdge(edge);
            if(lastEdge != null) {
                lastEdge.setNext(edge);
                edge.setPrev(lastEdge);
            } else {
                firstEdge = edge;
            }

            if(!invert) {
                if(index == vertices.size()-1) {
                    edge.setNext(firstEdge);
                    firstEdge.setPrev(edge);
                }
                index++;
            } else {
                if (index == 0) {
                    edge.setNext(firstEdge);
                    firstEdge.setPrev(edge);
                }
                index--;
            }
            lastEdge = edge;
        }
        return firstEdge;
    }

    public void addConstraintCurve(GraphOutline polyline) {
        //        GraphOutline outline = new GraphOutline(polyline);
        /**needed to generate vertex references.*/
        initFromPolyline(polyline, VectorUtil.Winding.CW); 

        GraphVertex v3 = locateClosestVertex(polyline);
        HEdge v3Edge = v3.findBoundEdge();
        HEdge v3EdgeP = v3Edge.getPrev();
        HEdge crossEdge = new HEdge(root.getGraphPoint(), HEdge.INNER);

        HEdge.connect(root.getPrev(), crossEdge);
        HEdge.connect(crossEdge, v3Edge);

        HEdge crossEdgeSib = crossEdge.getSibling();
        if(crossEdgeSib == null) {
            crossEdgeSib = new HEdge(crossEdge.getNext().getGraphPoint(), HEdge.INNER);
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
    private GraphVertex locateClosestVertex(GraphOutline polyline) {
        HEdge closestE = null;
        GraphVertex closestV = null;

        float minDistance = Float.MAX_VALUE;
        boolean inValid = false;
        ArrayList<GraphVertex> initVertices = initialOutline.getGraphPoint();
        ArrayList<GraphVertex> vertices = polyline.getGraphPoint();

        for(int i=0; i< initVertices.size()-1; i++){
            GraphVertex v = initVertices.get(i);
            GraphVertex nextV = initVertices.get(i+1);
            for(int pos=0; pos<vertices.size(); pos++) {
                GraphVertex cand = vertices.get(pos);
                float distance = VectorUtil.computeLength(v.getCoord(), cand.getCoord());
                if(distance < minDistance){
                    for (GraphVertex vert:vertices){
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

    private HEdge findClosestValidNeighbor(HEdge edge, boolean delaunay) {
        HEdge next = root.getNext();

        if(!VectorUtil.ccw(root.getGraphPoint().getPoint(), next.getGraphPoint().getPoint(),
                edge.getGraphPoint().getPoint())){
            return null;
        }

        HEdge candEdge = edge;
        boolean inValid = false;

        if(delaunay){
            Vertex cand = candEdge.getGraphPoint().getPoint();
            HEdge e = candEdge.getNext();
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
    private Triangle createTriangle(Vertex v1, Vertex v2, Vertex v3, HEdge rootT){
        Triangle t = new Triangle(v1, v2, v3);
        t.setVerticesBoundary(checkVerticesBoundary(rootT));
        return t;
    }

    private boolean[] checkVerticesBoundary(HEdge rootT) {
        boolean[] boundary = new boolean[3];
        HEdge e1 = rootT;
        HEdge e2 = rootT.getNext();
        HEdge e3 = rootT.getNext().getNext();

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

    public boolean checkInside(Vertex v) {
        if(!box.contains(v.getX(), v.getY(), v.getZ())){
            return false;
        }

        boolean inside = false;
        HEdge current = root;
        HEdge next = root.getNext();
        do {
            Vertex v2 = current.getGraphPoint().getPoint();
            Vertex v1 = next.getGraphPoint().getPoint();

            if ( ((v1.getY() > v.getY()) != (v2.getY() > v.getY())) &&
                  (v.getX() < (v2.getX() - v1.getX()) * (v.getY() - v1.getY()) / (v2.getY() - v1.getY()) + v1.getX()) ){
                inside = !inside;
            }
            
            current = next;
            next = current.getNext();
            
        } while(current != root);
        
        return inside;
    }
    
    public int computeLoopSize(){
        int size = 0;
        HEdge e = root;
        do{
            size++;
            e = e.getNext();
        }while(e != root);
        return size;
    }
}
