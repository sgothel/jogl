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

import com.jogamp.graph.geom.Outline;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.math.VectorUtil;

import jogamp.opengl.Debug;

/** Constrained Delaunay Triangulation 
 * implementation of a list of Outlines that define a set of
 * Closed Regions with optional n holes.
 * 
 */
public class CDTriangulator2D {

    protected static final boolean DEBUG = Debug.debug("Triangulation");
    
    private float sharpness = 0.5f;
    private ArrayList<Loop> loops;
    private ArrayList<Vertex> vertices;
    
    private ArrayList<Triangle> triangles;
    private int maxTriID = 0;

    
    /** Constructor for a new Delaunay triangulator
     */
    public CDTriangulator2D() {
        reset();
    }
    
    /** Reset the triangulation to initial state
     *  Clearing cached data
     */
    public void reset() {
        maxTriID = 0;
        vertices = new ArrayList<Vertex>();
        triangles = new ArrayList<Triangle>(3);
        loops = new ArrayList<Loop>();
    }
    
    /** Add a curve to the list of profiles provided
     * @param polyline a bounding {@link Outline}
     */
    public void addCurve(Outline polyline){
        Loop loop = null;
        
        // FIXME: multiple in/out and CW/CCW tests (as follows) ??
        
        if(!loops.isEmpty()) {
            // FIXME: #1 in/out test 
            loop = getContainerLoop(polyline);
        }
        
        if(loop == null) {
            // Claim:  CCW (out)
            GraphOutline outline = new GraphOutline(polyline);
            // FIXME: #2/#3 extract..(CCW) and new Loop(CCW).. does CW/CCW tests
            GraphOutline innerPoly = extractBoundaryTriangles(outline, false);
            vertices.addAll(polyline.getVertices());
            loop = new Loop(innerPoly, VectorUtil.Winding.CCW);
            loops.add(loop);
        } else {
            // Claim: CW (in)
            GraphOutline outline = new GraphOutline(polyline);
            // FIXME: #3/#4 extract..(CW) and addContraint..(CW) does CW/CCW tests
            GraphOutline innerPoly = extractBoundaryTriangles(outline, true);
            vertices.addAll(innerPoly.getVertices());
            loop.addConstraintCurve(innerPoly);
        }
    }
    
    /** Generate the triangulation of the provided 
     *  List of {@link Outline}s
     */
    public ArrayList<Triangle> generateTriangulation(){    
        for(int i=0;i<loops.size();i++) {
            Loop loop = loops.get(i);
            int numTries = 0;
            int size = loop.computeLoopSize();
            while(!loop.isSimplex()){
                Triangle tri = null;
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
            Triangle tri = loop.cut(true);
            if(tri != null)
                triangles.add(tri);
        }
        return triangles;
    }

    private GraphOutline extractBoundaryTriangles(GraphOutline outline, boolean hole) {
        GraphOutline innerOutline = new GraphOutline();
        ArrayList<GraphVertex> outVertices = outline.getGraphPoint();
        int size = outVertices.size();
        for(int i=0; i < size; i++) {
            GraphVertex currentVertex = outVertices.get(i);
            GraphVertex gv0 = outVertices.get((i+size-1)%size);
            GraphVertex gv2 = outVertices.get((i+1)%size);
            GraphVertex gv1 = currentVertex;
            
            if(!currentVertex.getPoint().isOnCurve()) {
                Vertex v0 = gv0.getPoint().clone();
                Vertex v2 = gv2.getPoint().clone();
                Vertex v1 = gv1.getPoint().clone();
                
                gv0.setBoundaryContained(true);
                gv1.setBoundaryContained(true);
                gv2.setBoundaryContained(true);
                
                final Triangle t;
                final boolean holeLike;
                if(VectorUtil.ccw(v0,v1,v2)) {
                    holeLike = false;
                    t = new Triangle(v0, v1, v2);
                } else {
                    holeLike = true;
                    t = new Triangle(v2, v1, v0);
                }
                t.setId(maxTriID++);
                triangles.add(t);
                if(DEBUG){
                    System.err.println(t);
                }
                if( hole || holeLike ) {
                    v0.setTexCoord(0, -0.1f);
                    v2.setTexCoord(1, -0.1f);
                    v1.setTexCoord(0.5f, -1*sharpness -0.1f);
                    innerOutline.addVertex(currentVertex);
                } else {
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
    
    private Loop getContainerLoop(Outline polyline){
        ArrayList<Vertex> vertices = polyline.getVertices();
        // FIXME: remove implicit iterator
        for(Vertex vert: vertices){
            for (Loop loop:loops){
                if(loop.checkInside(vert)){
                    return loop;
                }
            }
        }
        return null;
    }
}
