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
import java.util.List;

import com.jogamp.graph.curve.tess.Triangulator;
import com.jogamp.graph.geom.Outline;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.math.VectorUtil;

import jogamp.opengl.Debug;

/**
 * Constrained Delaunay Triangulation
 * implementation of a list of Outlines that define a set of
 * Closed Regions with optional n holes.
 */
public class CDTriangulator2D implements Triangulator {

    protected static final boolean DEBUG = Debug.debug("graph.curve.Triangulation");

    private static final boolean TEST_LINE_AA = Debug.debug("graph.curve.triangulation.LINE_AA");
    private static final boolean TEST_MARK_LINE = Debug.debug("graph.curve.triangulation.MARK_AA");
    private static final boolean TEST_ENABLED = TEST_LINE_AA || TEST_MARK_LINE;

    private final ArrayList<Loop> loops = new ArrayList<Loop>();

    private int addedVerticeCount;
    private int maxTriID;


    /** Constructor for a new Delaunay triangulator
     */
    public CDTriangulator2D() {
        reset();
    }

    @Override
    public final void reset() {
        maxTriID = 0;
        addedVerticeCount = 0;
        loops.clear();
    }

    @Override
    public final int getAddedVerticeCount() {
        return addedVerticeCount;
    }


    @Override
    public final void addCurve(final List<Triangle> sink, final Outline polyline, final float sharpness) {
        Loop loop = null;

        if(!loops.isEmpty()) {
            loop = getContainerLoop(polyline);
        }

        if(loop == null) {
            final GraphOutline outline = new GraphOutline(polyline);
            final GraphOutline innerPoly = extractBoundaryTriangles(sink, outline, false, sharpness);
            // vertices.addAll(polyline.getVertices());
            loop = new Loop(innerPoly, VectorUtil.Winding.CCW);
            loops.add(loop);
        } else {
            final GraphOutline outline = new GraphOutline(polyline);
            final GraphOutline innerPoly = extractBoundaryTriangles(sink, outline, true, sharpness);
            // vertices.addAll(innerPoly.getVertices());
            loop.addConstraintCurve(innerPoly);
        }
    }

    @Override
    public final void generate(final List<Triangle> sink) {
        final int loopsSize = loops.size();
        for(int i=0;i<loopsSize;i++) {
            final Loop loop = loops.get(i);
            int numTries = 0;
            int size = loop.computeLoopSize();
            while(!loop.isSimplex()){
                final Triangle tri;
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
                    sink.add(tri);
                    if(DEBUG){
                        System.err.println("CDTri.gen["+i+"].0: "+tri);
                    }
                }
                if(numTries > size*2){
                    if(DEBUG){
                        System.err.println("CDTri.gen["+i+"].X: Triangulation not complete!");
                    }
                    break;
                }
            }
            final Triangle tri = loop.cut(true);
            if(tri != null) {
                sink.add(tri);
                if(DEBUG){
                    System.err.println("CDTri.gen["+i+"].1: "+tri);
                }
            }
        }
        if( TEST_ENABLED ) {
            final float[] tempV2 = new float[2];
            final CDTriangulator2DExpAddOn addOn = new CDTriangulator2DExpAddOn();
            final int sinkSize = sink.size();
            if( TEST_MARK_LINE ) {
                for(int i=0; i<sinkSize; i++) {
                    final Triangle t0 = sink.get(i);
                    addOn.markLineInTriangle(t0, tempV2);
                }
            } else if ( TEST_LINE_AA ){
                for(int i=0; i<sinkSize-1; i+=2) {
                    final Triangle t0 = sink.get(i);
                    final Triangle t1 = sink.get(i+1);
                    /* final float[] rect =  */ addOn.processLineAA(i, t0, t1, tempV2);
                }
            }
        }
    }

    private GraphOutline extractBoundaryTriangles(final List<Triangle> sink, final GraphOutline outline, final boolean hole, final float sharpness) {
        final GraphOutline innerOutline = new GraphOutline();
        final ArrayList<GraphVertex> outVertices = outline.getGraphPoint();
        final int size = outVertices.size();
        for(int i=0; i < size; i++) {
            final GraphVertex gv1 = outVertices.get(i);               // currentVertex
            final GraphVertex gv0 = outVertices.get((i+size-1)%size); // -1
            final GraphVertex gv2 = outVertices.get((i+1)%size);      // +1

            if( !gv1.getPoint().isOnCurve() ) {
                final Vertex v0 = gv0.getPoint().clone();
                final Vertex v2 = gv2.getPoint().clone();
                final Vertex v1 = gv1.getPoint().clone();
                addedVerticeCount += 3;
                final boolean[] boundaryVertices = { true, true, true };

                gv0.setBoundaryContained(true);
                gv1.setBoundaryContained(true);
                gv2.setBoundaryContained(true);

                final Triangle t;
                final boolean holeLike;
                if(VectorUtil.ccw(v0,v1,v2)) {
                    holeLike = false;
                    t = new Triangle(v0, v1, v2, boundaryVertices);
                } else {
                    holeLike = true;
                    t = new Triangle(v2, v1, v0, boundaryVertices);
                }
                t.setId(maxTriID++);
                sink.add(t);
                if(DEBUG){
                    System.err.println(t);
                }
                if( hole || holeLike ) {
                    v0.setTexCoord(0.0f,           -0.1f, 0f);
                    v2.setTexCoord(1.0f,           -0.1f, 0f);
                    v1.setTexCoord(0.5f, -sharpness-0.1f, 0f);
                    innerOutline.addVertex(gv1);
                } else {
                    v0.setTexCoord(0.0f,            0.1f, 0f);
                    v2.setTexCoord(1.0f,            0.1f, 0f);
                    v1.setTexCoord(0.5f,  sharpness+0.1f, 0f);
                }
                if(DEBUG) {
                    System.err.println("CDTri.ebt["+i+"].0: hole "+(hole || holeLike)+" "+gv1+", "+t);
                }
            } else {
                if( !gv2.getPoint().isOnCurve() || !gv0.getPoint().isOnCurve() ) {
                    gv1.setBoundaryContained(true);
                }
                innerOutline.addVertex(gv1);
                if(DEBUG) {
                    System.err.println("CDTri.ebt["+i+"].1: "+gv1);
                }
            }
        }
        return innerOutline;
    }

    private Loop getContainerLoop(final Outline polyline) {
        final ArrayList<Vertex> vertices = polyline.getVertices();
        for(int i=0; i < loops.size(); i++) {
            final Loop loop = loops.get(i);
            for(int j=0; j < vertices.size(); j++) {
                if( loop.checkInside( vertices.get(j) ) ) {
                    return loop;
                }
            }
        }
        return null;
    }
}
