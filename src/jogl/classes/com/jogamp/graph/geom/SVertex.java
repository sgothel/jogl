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

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;

/** A Simple Vertex Implementation. Where the coordinates, and other attributes are
 * float based, and the coordinates and texture coordinates are saved in two float arrays.
 *
 */
public class SVertex implements Vertex {
    private int id;
    protected boolean onCurve;
    protected final float[] coord = new float[3];
    private final float[] texCoord = new float[3];

    static final Factory factory = new Factory();

    public static Factory factory() { return factory; }

    public static class Factory implements Vertex.Factory<SVertex> {
        @Override
        public SVertex create() {
            return new SVertex();
        }

        public SVertex create(final Vertex src) {
            return new SVertex(src);
        }

        @Override
        public SVertex create(final int id, final boolean onCurve, final float[] texCoordsBuffer) {
            return new SVertex(id, onCurve, texCoordsBuffer);
        }

        @Override
        public SVertex create(final float x, final float y, final float z, final boolean onCurve) {
            return new SVertex(x, y, z, onCurve);
        }

        @Override
        public SVertex create(final float[] coordsBuffer, final int offset, final int length, final boolean onCurve) {
            return new SVertex(coordsBuffer, offset, length, onCurve);
        }
    }

    public SVertex() {
        this.id = Integer.MAX_VALUE;
    }

    public SVertex(final Vertex src) {
        this.id = Integer.MAX_VALUE;
        System.arraycopy(src.getCoord(), 0, coord, 0, 3);
        System.arraycopy(src.getTexCoord(), 0, texCoord, 0, 3);
        setOnCurve(src.isOnCurve());
    }

    public SVertex(final int id, final boolean onCurve, final float[] texCoordsBuffer) {
        this.id = id;
        this.onCurve = onCurve;
        System.arraycopy(texCoordsBuffer, 0, texCoord, 0, 3);
    }

    public SVertex(final float x, final float y, final float z, final boolean onCurve) {
        this.id = Integer.MAX_VALUE;
        setCoord(x, y, z);
        setOnCurve(onCurve);
    }

    public SVertex(final float[] coordsBuffer, final int offset, final int length, final boolean onCurve) {
        this.id = Integer.MAX_VALUE;
        setCoord(coordsBuffer, offset, length);
        setOnCurve(onCurve);
    }

    @Override
    public final void setCoord(final float x, final float y, final float z) {
        coord[0] = x;
        coord[1] = y;
        coord[2] = z;
    }

    @Override
    public final void setCoord(final float[] coordsBuffer, final int offset, final int length) {
        System.arraycopy(coordsBuffer, offset, coord, 0, length);
    }

    @Override
    public int getCoordCount() {
        return 3;
    }

    @Override
    public final float[] getCoord() {
        return coord;
    }

    @Override
    public final void setX(final float x) {
        this.coord[0] = x;
    }

    @Override
    public final void setY(final float y) {
        this.coord[1] = y;
    }

    @Override
    public final void setZ(final float z) {
        this.coord[2] = z;
    }

    @Override
    public final float getX() {
        return this.coord[0];
    }

    @Override
    public final float getY() {
        return this.coord[1];
    }

    @Override
    public final float getZ() {
        return this.coord[2];
    }

    @Override
    public final boolean isOnCurve() {
        return onCurve;
    }

    @Override
    public final void setOnCurve(final boolean onCurve) {
        this.onCurve = onCurve;
    }

    @Override
    public final int getId(){
        return id;
    }

    @Override
    public final void setId(final int id){
        this.id = id;
    }

    @Override
    public boolean equals(final Object obj) {
        if( obj == this) {
            return true;
        }
        if( null == obj || !(obj instanceof Vertex) ) {
            return false;
        }
        final Vertex v = (Vertex) obj;
        return this == v ||
               isOnCurve() == v.isOnCurve() &&
               VectorUtil.isVec3Equal(getTexCoord(), 0, v.getTexCoord(), 0, FloatUtil.EPSILON) &&
               VectorUtil.isVec3Equal(getCoord(), 0, v.getCoord(), 0, FloatUtil.EPSILON) ;
    }
    @Override
    public final int hashCode() {
        throw new InternalError("hashCode not designed");
    }

    @Override
    public final float[] getTexCoord() {
        return texCoord;
    }

    @Override
    public final void setTexCoord(final float s, final float t, final float p) {
        texCoord[0] = s;
        texCoord[1] = t;
        texCoord[2] = p;
    }

    @Override
    public final void setTexCoord(final float[] texCoordsBuffer, final int offset, final int length) {
        System.arraycopy(texCoordsBuffer, offset, texCoord, 0, length);
    }

    /**
     * @return deep clone of this Vertex elements
     */
    @Override
    public SVertex clone(){
        return new SVertex(this); // OK to not call super.clone(), using own copy-ctor
    }

    @Override
    public String toString() {
        return "[ID: " + id + ", onCurve: " + onCurve +
               ": p " + coord[0] + ", " + coord[1] + ", " + coord[2] +
               ", t " + texCoord[0] + ", " + texCoord[1] + ", " + texCoord[2] + "]";
    }
}
