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

public class Triangle {
	private int id = Integer.MAX_VALUE;
	final private Vertex[] vertices;
	private boolean[] boundaryEdges = new boolean[3];
	private boolean[] boundaryVertices = null;

	public Triangle(Vertex ... v123){
		vertices = v123;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Vertex[] getVertices() {
		return vertices;
	}
	
	public boolean isEdgesBoundary() {
		return boundaryEdges[0] || boundaryEdges[1] || boundaryEdges[2];
	}
	
	public boolean isVerticesBoundary() {
		return boundaryVertices[0] || boundaryVertices[1] || boundaryVertices[2];
	}

	public void setEdgesBoundary(boolean[] boundary) {
		this.boundaryEdges = boundary;
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
	
	public String toString() {
		return "Tri ID: " + id + "\n" +  vertices[0]  + "\n" +  vertices[1] + "\n" +  vertices[2];
	}
}
