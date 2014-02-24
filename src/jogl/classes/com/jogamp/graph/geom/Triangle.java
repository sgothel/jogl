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
package com.jogamp.graph.geom;

import com.jogamp.graph.curve.Region;

import jogamp.graph.geom.plane.AffineTransform;

public class Triangle {
    private final Vertex[] vertices = new Vertex[3];
    private final boolean[] boundaryEdges = new boolean[3];
    private boolean[] boundaryVertices = null;
    private int id;

    public Triangle(Vertex v1, Vertex v2, Vertex v3) {
        id = Integer.MAX_VALUE;
        vertices[0] = v1;
        vertices[1] = v2;
        vertices[2] = v3;
    }

    public Triangle(Triangle src) {
        id = src.id;
        vertices[0] = src.vertices[0].clone();
        vertices[1] = src.vertices[1].clone();
        vertices[2] = src.vertices[2].clone();
        System.arraycopy(src.boundaryEdges, 0, boundaryEdges, 0, 3);
        boundaryVertices = src.boundaryVertices;
    }

    private Triangle(final int id, final boolean[] boundaryEdges, final boolean[] boundaryVertices){
        this.id = id;
        System.arraycopy(boundaryEdges, 0, this.boundaryEdges, 0, 3);
        this.boundaryVertices = boundaryVertices;
        /**
        if( null != boundaryVertices ) {
            this.boundaryVertices = new boolean[3];
            System.arraycopy(boundaryVertices, 0, this.boundaryVertices, 0, 3);
        } */
    }

    /**
     * Returns a transformed a clone of this instance using the given AffineTransform.
     */
    public Triangle transform(AffineTransform t) {
        final Triangle tri = new Triangle(id, boundaryEdges, boundaryVertices);
        tri.vertices[0] = t.transform(vertices[0], null);
        tri.vertices[1] = t.transform(vertices[1], null);
        tri.vertices[2] = t.transform(vertices[2], null);
        return tri;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void addVertexIndicesOffset(int offset) {
        if( 0 < offset ) {
            final int i0 = vertices[0].getId();
            if( Integer.MAX_VALUE-offset > i0 ) { // Integer.MAX_VALUE != i0 // FIXME: renderer uses SHORT!
                if(Region.DEBUG_INSTANCE) {
                    System.err.println("Triangle.addVertexIndicesOffset: "+i0+" + "+offset+" -> "+(i0+offset));
                }
                vertices[0].setId(i0+offset);
                vertices[1].setId(vertices[1].getId()+offset);
                vertices[2].setId(vertices[2].getId()+offset);
            }
        }
    }

    /** Returns array of 3 vertices, denominating the triangle. */
    public Vertex[] getVertices() {
        return vertices;
    }

    public boolean isEdgesBoundary() {
        return boundaryEdges[0] || boundaryEdges[1] || boundaryEdges[2];
    }

    public boolean isVerticesBoundary() {
        return boundaryVertices[0] || boundaryVertices[1] || boundaryVertices[2];
    }

    public boolean[] getEdgeBoundary() {
        return boundaryEdges;
    }

    public boolean[] getVerticesBoundary() {
        return boundaryVertices;
    }

    public void setVerticesBoundary(boolean[] boundaryVertices) {
        this.boundaryVertices = boundaryVertices;
    }

    @Override
    public String toString() {
        return "Tri ID: " + id + "\n" +  vertices[0]  + "\n" +  vertices[1] + "\n" +  vertices[2];
    }
}
