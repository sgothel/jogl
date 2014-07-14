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

import com.jogamp.graph.geom.Vertex;

public class GraphVertex {
    private Vertex point;
    private ArrayList<HEdge> edges = null;
    private boolean boundaryContained = false;

    public GraphVertex(final Vertex point) {
        this.point = point;
    }

    public Vertex getPoint() {
        return point;
    }

    public float getX(){
        return point.getX();
    }

    public float getY(){
        return point.getY();
    }

    public float getZ(){
        return point.getZ();
    }
    public float[] getCoord() {
        return point.getCoord();
    }

    public void setPoint(final Vertex point) {
        this.point = point;
    }

    public ArrayList<HEdge> getEdges() {
        return edges;
    }

    public void setEdges(final ArrayList<HEdge> edges) {
        this.edges = edges;
    }

    public void addEdge(final HEdge edge){
        if(edges == null){
            edges = new ArrayList<HEdge>();
        }
        edges.add(edge);
    }
    public void removeEdge(final HEdge edge){
        if(edges == null)
            return;
        edges.remove(edge);
        if(edges.size() == 0){
            edges = null;
        }
    }
    public HEdge findNextEdge(final GraphVertex nextVert){
        for(int i=0; i<edges.size(); i++) {
            final HEdge e = edges.get(i);
            if(e.getNext().getGraphPoint() == nextVert){
                return e;
            }
        }
        return null;
    }
    public HEdge findBoundEdge(){
        for(int i=0; i<edges.size(); i++) {
            final HEdge e = edges.get(i);
            if((e.getType() == HEdge.BOUNDARY) || (e.getType() == HEdge.HOLE)){
                return e;
            }
        }
        return null;
    }
    public HEdge findPrevEdge(final GraphVertex prevVert){
        for(int i=0; i<edges.size(); i++) {
            final HEdge e = edges.get(i);
            if(e.getPrev().getGraphPoint() == prevVert){
                return e;
            }
        }
        return null;
    }

    public boolean isBoundaryContained() {
        return boundaryContained;
    }

    public void setBoundaryContained(final boolean boundaryContained) {
        this.boundaryContained = boundaryContained;
    }

    public String toString() {
        return "GraphVertex[contained "+boundaryContained+", "+point+"]";
    }
}
