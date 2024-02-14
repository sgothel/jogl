/**
 * Copyright 2010-2024 JogAmp Community. All rights reserved.
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
import java.util.List;

import com.jogamp.graph.geom.Vertex;
import com.jogamp.math.DoubleUtil;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.VectorUtil;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.plane.Winding;
import com.jogamp.graph.geom.Triangle;

public class Loop {
    private final AABBox box = new AABBox();
    private final GraphOutline initialOutline;
    private final boolean complexShape;
    private HEdge root;
    private final List<GraphOutline> outlines = new ArrayList<GraphOutline>();

    private Loop(final GraphOutline polyline, final int edgeType, final boolean complexShape){
        this.initialOutline = polyline;
        this.complexShape = complexShape;
        this.root = initFromPolyline(initialOutline, edgeType);
    }

    public static Loop createBoundary(final GraphOutline polyline, final boolean isConvex) {
        final Loop res = new Loop(polyline, HEdge.BOUNDARY, isConvex);
        if( null == res.root ) {
            return null;
        }
        return res;
    }

    public HEdge getHEdge(){
        return root;
    }

    public boolean isSimplex(){
        return (root.getNext().getNext().getNext() == root);
    }

    /**
     * Create a connected list of half edges (loop)
     * from the boundary profile
     * @param edgeType either {@link HEdge#BOUNDARY} requiring {@link Winding#CCW} or {@link HEdge#HOLE} using {@link Winding#CW} or even {@link Winding#CCW}
     */
    private HEdge initFromPolyline(final GraphOutline outline, final int edgeType) {
        outlines.add(outline);
        final ArrayList<GraphVertex> vertices = outline.getGraphPoint();

        if(vertices.size()<3) {
            System.err.println( "Graph: Loop.initFromPolyline: GraphOutline's vertices < 3: " + vertices.size() );
            if( GraphOutline.DEBUG ) {
                Thread.dumpStack();
            }
            return null;
        }
        final Winding edgeWinding = HEdge.BOUNDARY == edgeType ? Winding.CCW : Winding.CW;
        final Winding winding = CDTriangulator2D.FixedWindingRule ?  edgeWinding : outline.getOutline().getWinding();

        if( HEdge.BOUNDARY == edgeType && Winding.CCW != winding ) {
            // XXXX
            System.err.println("Loop.init.xx.01: BOUNDARY req CCW but has "+winding);
            // outline.getOutline().print(System.err);
            Thread.dumpStack();
        }
        HEdge firstEdge = null;
        HEdge lastEdge = null;

        if( winding == edgeWinding || HEdge.BOUNDARY == edgeType ) {
            // Correct Winding or skipped CW -> CCW (no inversion possible here, too late)
            final int max = vertices.size() - 1;
            for(int index = 0; index <= max; ++index) {
                final GraphVertex v1 = vertices.get(index);
                box.resize(v1.x(), v1.y(), v1.z());

                final HEdge edge = new HEdge(v1, edgeType);

                v1.addEdge(edge);
                if(lastEdge != null) {
                    lastEdge.setNext(edge);
                    edge.setPrev(lastEdge);
                } else {
                    firstEdge = edge;
                }
                if(index == max ) {
                    edge.setNext(firstEdge);
                    firstEdge.setPrev(edge);
                }
                lastEdge = edge;
            }
        } else { // if( winding == Winding.CW ) {
            // CCW <-> CW
            for(int index = vertices.size() - 1; index >= 0; --index) {
                final GraphVertex v1 = vertices.get(index);
                box.resize(v1.x(), v1.y(), v1.z());

                final HEdge edge = new HEdge(v1, edgeType);

                v1.addEdge(edge);
                if(lastEdge != null) {
                    lastEdge.setNext(edge);
                    edge.setPrev(lastEdge);
                } else {
                    firstEdge = edge;
                }

                if (index == 0) {
                    edge.setNext(firstEdge);
                    firstEdge.setPrev(edge);
                }
                lastEdge = edge;
            }
        }
        return firstEdge;
    }

    public void addConstraintCurveHole(final GraphOutline polyline) {
        //        GraphOutline outline = new GraphOutline(polyline);
        /**needed to generate vertex references.*/
        if( null == initFromPolyline(polyline, HEdge.HOLE) ) {
            return;
        }
        final GraphVertex v3 = locateClosestVertex(polyline);
        if( null == v3 ) {
            System.err.println( "Graph: Loop.locateClosestVertex returns null; root valid? "+(null!=root));
            if( GraphOutline.DEBUG ) {
                Thread.dumpStack();
            }
            return;
        }
        final HEdge v3Edge = v3.findBoundEdge();
        final HEdge v3EdgeP = v3Edge.getPrev();
        final HEdge crossEdge = new HEdge(root.getGraphPoint(), HEdge.INNER);

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

    /**
     * Locates the vertex and update the loops root
     * to have (root + vertex) as closest pair
     * @param polyline the control polyline to search for closestvertices in CW
     * @return the vertex that is closest to the newly set root Hedge.
     */
    private GraphVertex locateClosestVertex(final GraphOutline polyline) {
        float minDistance = Float.MAX_VALUE;
        final ArrayList<GraphVertex> initVertices = initialOutline.getGraphPoint();
        if( initVertices.size() < 2 ) {
            return null;
        }
        final ArrayList<GraphVertex> vertices = polyline.getGraphPoint();
        HEdge closestE = null;
        GraphVertex closestV = null;

        final int initSz = initVertices.size();
        GraphVertex v0 = initVertices.get(0);
        for(int i=1; i< initSz; i++){
            final GraphVertex v1 = initVertices.get(i);
            for(int pos=0; pos<vertices.size(); pos++) {
                final GraphVertex cand = vertices.get(pos);
                final float distance = v0.getCoord().dist( cand.getCoord() );
                if(distance < minDistance){
                    boolean inside = false;
                    for (final GraphVertex vert:vertices){
                        if( !( vert == v0 || vert == v1 || vert == cand) ) {
                            inside = VectorUtil.isInCircle(v0.getPoint(), v1.getPoint(), cand.getPoint(), vert.getPoint());
                            if(inside){
                                break;
                            }
                        }
                    }
                    if(!inside){
                        closestV = cand;
                        minDistance = distance;
                        closestE = v0.findBoundEdge();
                    }
                }
            }
            v0 = v1;
        }
        if(closestE != null){
            root = closestE;
        }
        return closestV;
    }

    /**
    public static void printPerf(final PrintStream out) {
        out.printf("Graph.Intersection: cut[count %,d, td %,f ms], isect[count %,d, td %,f ms], isec/cut[count %f, td %f]%n",
                   perf_cut_count, perf_cut_td_ns/1000000.0, perf_isect_count, perf_isect_td_ns/1000000.0,
                   (double)perf_isect_count/(double)perf_cut_count, (double)perf_isect_td_ns/(double)perf_cut_td_ns);
    }
    private static long perf_isect_td_ns = 0;
    private static long perf_isect_count = 0;
    private static long perf_cut_td_ns = 0;
    private static long perf_cut_count = 0;
    */
    public final Triangle cut(final boolean delaunay){
        if( !CDTriangulator2D.DEBUG ) {
            // final long t0 = Clock.currentNanos();
            return cut0(delaunay);
            // perf_cut_td_ns += Clock.currentNanos() - t0;
            // perf_cut_count++;
        } else {
            return cutDbg(delaunay);
        }
    }
    private final Triangle cut0(final boolean delaunay){
        final HEdge next1 = root.getNext();
        if(isSimplex()){
            final Vertex rootPoint = root.getGraphPoint().getPoint();
            final Vertex nextPoint = next1.getGraphPoint().getPoint();
            final Vertex candPoint = next1.getNext().getGraphPoint().getPoint();
            if( complexShape && intersectsOutline(rootPoint, nextPoint, candPoint) ) {
                return null;
            }
            return new Triangle(rootPoint, nextPoint, candPoint, checkVerticesBoundary(root));
        }
        final HEdge prev = root.getPrev();

        final HEdge next2 = isValidNeighbor(next1.getNext(), delaunay);
        if(next2 == null){
            root = root.getNext();
            return null;
        }

        final GraphVertex v1 = root.getGraphPoint();
        final GraphVertex v2 = next1.getGraphPoint();
        final GraphVertex v3 = next2.getGraphPoint();

        final HEdge v3Edge = new HEdge(v3, HEdge.INNER);

        HEdge.connect(v3Edge, root);
        HEdge.connect(next1, v3Edge);

        HEdge v3EdgeSib = v3Edge.getSibling();
        if(v3EdgeSib == null){
            v3EdgeSib = new HEdge(v3Edge.getNext().getGraphPoint(), HEdge.INNER);
            HEdge.makeSiblings(v3Edge, v3EdgeSib);
        }

        HEdge.connect(prev, v3EdgeSib);
        HEdge.connect(v3EdgeSib, next2);

        final Triangle t = createTriangle(v1.getPoint(), v2.getPoint(), v3.getPoint(), root);
        this.root = next2;
        return t;
    }
    private final Triangle cutDbg(final boolean delaunay){
        final HEdge next1 = root.getNext();
        if(isSimplex()){
            final Vertex rootPoint = root.getGraphPoint().getPoint();
            final Vertex nextPoint = next1.getGraphPoint().getPoint();
            final Vertex candPoint = next1.getNext().getGraphPoint().getPoint();
            if( complexShape && intersectsOutlineDbg(rootPoint, nextPoint, candPoint) ) {
                System.err.printf("Loop.cut.X0: last-simplex intersects%n");
                return null;
            }
            final Triangle tri = new Triangle(rootPoint, nextPoint, candPoint, checkVerticesBoundary(root));
            System.err.printf("Loop.cut.X1: last-simplex %s%n", tri);
            return tri;
        }
        final HEdge prev = root.getPrev();

        final HEdge next2 = isValidNeighborDbg(next1.getNext(), delaunay);
        if(next2 == null){
            root = root.getNext();
            System.err.printf("Loop.cut.0: null-cut %s%n", next1.getNext().getGraphPoint());
            return null;
        }

        final GraphVertex v1 = root.getGraphPoint();
        final GraphVertex v2 = next1.getGraphPoint();
        final GraphVertex v3 = next2.getGraphPoint();

        final HEdge v3Edge = new HEdge(v3, HEdge.INNER);

        HEdge.connect(v3Edge, root);
        HEdge.connect(next1, v3Edge);

        HEdge v3EdgeSib = v3Edge.getSibling();
        if(v3EdgeSib == null){
            v3EdgeSib = new HEdge(v3Edge.getNext().getGraphPoint(), HEdge.INNER);
            HEdge.makeSiblings(v3Edge, v3EdgeSib);
        }

        HEdge.connect(prev, v3EdgeSib);
        HEdge.connect(v3EdgeSib, next2);

        final Triangle t = createTriangle(v1.getPoint(), v2.getPoint(), v3.getPoint(), root);
        this.root = next2;
        System.err.printf("Loop.cut.1: new-cut %s -> %s%n", next2.getGraphPoint(), t);
        return t;
    }

    private boolean intersectsOutline(final Vertex a1, final Vertex a2, final Vertex b) {
        // final long t0 = Clock.currentNanos();
        for(final GraphOutline outline : outlines) {
            if( intersectsOutline(outline, a1, a2, b) ) {
                // perf_isect_td_ns += Clock.currentNanos() - t0;
                // perf_isect_count++;
                return true;
            }
        }
        // perf_isect_td_ns += Clock.currentNanos() - t0;
        // perf_isect_count++;
        return false;
    }
    private boolean intersectsOutline(final GraphOutline outline, final Vertex a1, final Vertex a2, final Vertex b) {
        final ArrayList<GraphVertex> vertices = outline.getGraphPoint();
        final int sz = vertices.size();
        if( sz < 2 ) {
            return false;
        }
        Vertex v0 = vertices.get(0).getPoint();
        for(int i=1; i< sz; i++){
            final Vertex v1 = vertices.get(i).getPoint();
            if( !( v0 == b || v1 == b ) ) {
                if( !( v0 == a1 || v1 == a1 ) &&
                    VectorUtil.testSeg2SegIntersection(a1, b, v0, v1) ) {
                    return true;
                }
                if( !( v0 == a2 || v1 == a2 ) &&
                    VectorUtil.testSeg2SegIntersection(a2, b, v0, v1) ) {
                    return true;
                }
            }
            v0 = v1;
        }
        return false;
    }
    private boolean intersectsOutlineDbg(final Vertex a1, final Vertex a2, final Vertex b) {
        for(final GraphOutline outline : outlines) {
            if( intersectsOutlineDbg(outline, a1, a2, b) ) {
                return true;
            }
        }
        return false;
    }
    private boolean intersectsOutlineDbg(final GraphOutline outline, final Vertex a1, final Vertex a2, final Vertex b) {
        final ArrayList<GraphVertex> vertices = outline.getGraphPoint();
        final int sz = vertices.size();
        if( sz < 2 ) {
            return false;
        }
        Vertex v0 = vertices.get(0).getPoint();
        for(int i=1; i< sz; i++){
            final Vertex v1 = vertices.get(i).getPoint();
            if( !( v0 == b || v1 == b ) ) {
                if( !( v0 == a1 || v1 == a1 ) &&
                    VectorUtil.testSeg2SegIntersection(a1, b, v0, v1) ) {
                    System.err.printf("Loop.intersection.b-a1.1: %d/%d %s to%n-a1 %s, with%n-v0 %s%n-v1 %s%n", i, sz-1, b, a1, v0, v1);
                    return true;
                }
                if( !( v0 == a2 || v1 == a2 ) &&
                    VectorUtil.testSeg2SegIntersection(a2, b, v0, v1) ) {
                    System.err.printf("Loop.intersection.b-a2.1: %d/%d %s to%n-a2 %s, with%n-v0 %s%n-v1 %s%n", i, sz-1, b, a2, v0, v1);
                    return true;
                }
            }
            v0 = v1;
        }
        return false;
    }

    private HEdge isValidNeighbor(final HEdge candEdge, final boolean delaunay) {
        final HEdge next = root.getNext();
        final Vertex rootPoint = root.getGraphPoint().getPoint();
        final Vertex nextPoint = next.getGraphPoint().getPoint();
        final Vertex candPoint = candEdge.getGraphPoint().getPoint();
        if( !VectorUtil.isCCW( rootPoint, nextPoint, candPoint) ) {
            return null;
        }
        if( complexShape && intersectsOutline(rootPoint, nextPoint, candPoint) ) {
            return null;
        }
        if( !delaunay ) {
            return candEdge;
        }
        HEdge e = candEdge.getNext();
        while (e != candEdge){
            final GraphVertex egp = e.getGraphPoint();
            if(egp != root.getGraphPoint() &&
               egp != next.getGraphPoint() &&
               egp.getPoint() != candPoint )
            {
                if( VectorUtil.isInCircle(rootPoint, nextPoint, candPoint, egp.getPoint()) ) {
                    return null;
                }
            }
            e = e.getNext();
        }
        return candEdge;
    }
    private HEdge isValidNeighborDbg(final HEdge candEdge, final boolean delaunay) {
        final HEdge next = root.getNext();
        final Vertex rootPoint = root.getGraphPoint().getPoint();
        final Vertex nextPoint = next.getGraphPoint().getPoint();
        final Vertex candPoint = candEdge.getGraphPoint().getPoint();
        if( !VectorUtil.isCCW( rootPoint, nextPoint, candPoint) ) {
            System.err.printf("Loop.isInCircle.X: !CCW %s, of%n- %s%n- %s%n- %s%n",
                    candPoint, rootPoint, nextPoint, candPoint);
            return null;
        }
        if( complexShape && intersectsOutlineDbg(rootPoint, nextPoint, candPoint) ) {
            return null;
        }
        if( !delaunay ) {
            return candEdge;
        }
        HEdge e = candEdge.getNext();
        while (e != candEdge){
            final GraphVertex egp = e.getGraphPoint();
            if(egp != root.getGraphPoint() &&
               egp != next.getGraphPoint() &&
               egp.getPoint() != candPoint )
            {
                final double v = VectorUtil.inCircleVal(rootPoint, nextPoint, candPoint, egp.getPoint());
                if( v > DoubleUtil.EPSILON ) {
                    System.err.printf("Loop.isInCircle.1: %30.30f: %s, of%n- %s%n- %s%n- %s%n",
                            v, candPoint, rootPoint, nextPoint, egp.getPoint());
                    return null;
                }
                System.err.printf("Loop.isInCircle.0: %30.30f: %s, of%n- %s%n- %s%n- %s%n",
                        v, candPoint, root.getGraphPoint().getPoint(), next.getGraphPoint().getPoint(), egp.getPoint());
            }
            e = e.getNext();
        }
        System.err.printf("Loop.isInCircle.0: %s%n", candPoint);
        return candEdge;
    }

    /** Create a triangle from the param vertices only if
     * the triangle is valid. IE not outside region.
     * @param v1 vertex 1
     * @param v2 vertex 2
     * @param v3 vertex 3
     * @param root and edge of this triangle
     * @return the triangle iff it satisfies, null otherwise
     */
    private Triangle createTriangle(final Vertex v1, final Vertex v2, final Vertex v3, final HEdge rootT){
        return new Triangle(v1, v2, v3, checkVerticesBoundary(rootT));
    }

    private boolean[] checkVerticesBoundary(final HEdge rootT) {
        final boolean[] boundary = new boolean[3];
        if(rootT.getGraphPoint().isBoundaryContained()){
                boundary[0] = true;
        }
        if(rootT.getNext().getGraphPoint().isBoundaryContained()){
                boundary[1] = true;
        }
        if(rootT.getNext().getNext().getGraphPoint().isBoundaryContained()){
                boundary[2] = true;
        }
        return boundary;
    }

    public boolean checkInside(final Vertex v) {
        if(!box.contains(v.x(), v.y(), v.z())){
            return false;
        }

        boolean inside = false;
        HEdge current = root;
        HEdge next = root.getNext();
        do {
            final Vertex v2 = current.getGraphPoint().getPoint();
            final Vertex v1 = next.getGraphPoint().getPoint();

            if ( ((v1.y() > v.y()) != (v2.y() > v.y())) &&
                  (v.x() < (v2.x() - v1.x()) * (v.y() - v1.y()) / (v2.y() - v1.y()) + v1.x()) ){
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
