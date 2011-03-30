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
package com.jogamp.graph.geom.opengl;


import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.math.VectorUtil;

/** A Simple Vertex Implementation. Where the coordinates, and other attributes are
 * float based, and the coordinates and texture coordinates are saved in two float arrays.
 *
 */
public class SVertex implements Vertex {
	private int id = Integer.MAX_VALUE;
	protected float[] coord = new float[3];
	protected boolean onCurve = true;
	private float[] texCoord = new float[2];
	
	static final Factory factory = new Factory();
	
	public static Factory factory() { return factory; } 
	
	public static class Factory implements Vertex.Factory<SVertex> {
		@Override
		public SVertex create() {
			return new SVertex();
		}

		@Override
		public SVertex create(float x, float y) {
			return new SVertex(x, y);
		}

		@Override
		public SVertex create(float x, float y, float z) {
			return new SVertex(x, y, z);
		}

		@Override
		public SVertex create(float[] coordsBuffer, int offset, int length) {
			return new SVertex(coordsBuffer, offset, length);
		}		
	}
	
	public SVertex() {
	}

	public SVertex(float x, float y) {
		setCoord(x, y);
	}
	public SVertex(float x, float y, float z) {
		setCoord(x, y, z);
	}	
	public SVertex(float[] coordsBuffer, int offset, int length) {
		setCoord(coordsBuffer, offset, length);
	}
		
	public void setCoord(float x, float y) {
		this.coord[0] = x;
		this.coord[1] = y;
		this.coord[2] = 0f;
	}

	public void setCoord(float x, float y, float z) {
		this.coord[0] = x;
		this.coord[1] = y;
		this.coord[2] = z;
	}

	public void setCoord(float[] coordsBuffer, int offset, int length) {
		if(length > coordsBuffer.length - offset) {
			throw new IndexOutOfBoundsException("coordsBuffer too small: "+coordsBuffer.length+" - "+offset+" < "+length);
		}
		if(length > 3) {
			throw new IndexOutOfBoundsException("length too big: "+length+" > "+3);
		}		
		int i=0;
		while(i<length) {
			this.coord[i++] = coordsBuffer[offset++];
		}
	}
		
	public float[] getCoord() {
		return coord;
	}

	public void setX(float x) {
		this.coord[0] = x;
	}

	public void setY(float y) {
		this.coord[1] = y;
	}

	public void setZ(float z) {
		this.coord[2] = z;
	}

	public float getX() {
		return this.coord[0];
	}

	public float getY() {
		return this.coord[1];
	}

	public float getZ() {
		return this.coord[2];
	}

	public boolean isOnCurve() {
		return onCurve;
	}

	public void setOnCurve(boolean onCurve) {
		this.onCurve = onCurve;
	}

	public int getId(){
		return id;
	}
	
	public void setId(int id){
		this.id = id;
	}
	
	public int compareTo(Vertex p) {
		if(VectorUtil.checkEquality(coord, p.getCoord())) {
			return 0;
		}
		return -1;
	}
	
	public float[] getTexCoord() {
		return texCoord;
	}

	public void setTexCoord(float s, float t) {
		this.texCoord[0] = s;
		this.texCoord[1] = t;
	}
	
	public SVertex clone(){
		SVertex v = new SVertex(this.coord, 0, 3);
		v.setOnCurve(this.onCurve);
		return v;
	}
	
	public String toString() {
		return "[ID: " + id + " X: " + coord[0]
		        + " Y: " + coord[1] + " Z: " + coord[2] + "]";
	}
}
