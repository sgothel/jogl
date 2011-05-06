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
    protected boolean onCurve;
    private float[] texCoord = new float[2];
    
    static final Factory factory = new Factory();
    
    public static Factory factory() { return factory; } 
    
    public static class Factory implements Vertex.Factory<SVertex> {
        public SVertex create() {
            return new SVertex();
        }

        public SVertex create(float x, float y, float z, boolean onCurve) {
            return new SVertex(x, y, z, onCurve);
        }

        public SVertex create(float[] coordsBuffer, int offset, int length, boolean onCurve) {
            return new SVertex(coordsBuffer, offset, length, onCurve);
        }        
    }
    
    public SVertex() {
    }

    public SVertex(float x, float y, float z, boolean onCurve) {
        setCoord(x, y, z);
        setOnCurve(onCurve);
    }
    
    public SVertex(float[] coordsBuffer, int offset, int length, boolean onCurve) {
        setCoord(coordsBuffer, offset, length);
        setOnCurve(onCurve);
    }
        
    public SVertex(float[] coordsBuffer, int offset, int length, 
                   float[] texCoordsBuffer, int offsetTC, int lengthTC, boolean onCurve) {
        setCoord(coordsBuffer, offset, length);
        setTexCoord(texCoordsBuffer, offsetTC, lengthTC);
        setOnCurve(onCurve);
    }
    
    public final void setCoord(float x, float y, float z) {
        this.coord[0] = x;
        this.coord[1] = y;
        this.coord[2] = z;
    }

    public final void setCoord(float[] coordsBuffer, int offset, int length) {
        System.arraycopy(coordsBuffer, offset, coord, 0, length);
    }
        
    public final float[] getCoord() {
        return coord;
    }

    public final void setX(float x) {
        this.coord[0] = x;
    }

    public final void setY(float y) {
        this.coord[1] = y;
    }

    public final void setZ(float z) {
        this.coord[2] = z;
    }

    public final float getX() {
        return this.coord[0];
    }

    public final float getY() {
        return this.coord[1];
    }

    public final float getZ() {
        return this.coord[2];
    }

    public final boolean isOnCurve() {
        return onCurve;
    }

    public final void setOnCurve(boolean onCurve) {
        this.onCurve = onCurve;
    }

    public final int getId(){
        return id;
    }
    
    public final void setId(int id){
        this.id = id;
    }
    
    public boolean equals(Object obj) {
        if( obj == this) {
            return true;
        }
        if( null == obj || !(obj instanceof Vertex) ) {
            return false;
        }
        final Vertex v = (Vertex) obj;
        return this == v || 
               isOnCurve() == v.isOnCurve() && 
               VectorUtil.checkEqualityVec2(getTexCoord(), v.getTexCoord()) &&
               VectorUtil.checkEquality(getCoord(), v.getCoord()) ;
    }
    
    public final float[] getTexCoord() {
        return texCoord;
    }

    public final void setTexCoord(float s, float t) {
        this.texCoord[0] = s;
        this.texCoord[1] = t;
    }

    public final void setTexCoord(float[] texCoordsBuffer, int offset, int length) {
        System.arraycopy(texCoordsBuffer, offset, texCoord, 0, length);
    }
        
    /**
     * @return deep clone of this Vertex, but keeping the id blank
     */
    public SVertex clone(){
        return new SVertex(this.coord, 0, 3, this.texCoord, 0, 2, this.onCurve);
    }
    
    public String toString() {
        return "[ID: " + id + ", onCurve: " + onCurve + 
               ": p " + coord[0] + ", " + coord[1] + ", " + coord[2] +
               ", t " + texCoord[0] + ", " + texCoord[1] + "]";
    }
}
